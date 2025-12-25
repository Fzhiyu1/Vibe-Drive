package com.vibe.orchestration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.agent.PromptAssembler;
import com.vibe.agent.VibeAgent;
import com.vibe.agent.VibeAgentFactory;
import com.vibe.model.AmbiencePlan;
import com.vibe.model.LightSetting;
import com.vibe.model.MassageSetting;
import com.vibe.model.MusicRecommendation;
import com.vibe.model.Narrative;
import com.vibe.model.PlayResult;
import com.vibe.model.ScentSetting;
import com.vibe.model.ToolExecutionInfo;
import com.vibe.model.enums.SafetyMode;
import com.vibe.orchestration.callback.VibeStreamCallback;
import com.vibe.orchestration.dto.VibeDialogRequest;
import com.vibe.orchestration.dto.VibeDialogResult;
import com.vibe.orchestration.dto.VibeLoopState;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Vibe 对话编排服务
 * 实现递归调用逻辑，参考 IC-Coder 的 IccoderDialogService
 */
@Service
public class VibeDialogService {

    private static final Logger log = LoggerFactory.getLogger(VibeDialogService.class);

    private final VibeAgentFactory agentFactory;
    private final PromptAssembler promptAssembler;
    private final SafetyModeFilter safetyModeFilter;
    private final ObjectMapper objectMapper;

    @Value("${vibe.dialog.max-recursion-depth:5}")
    private int maxRecursionDepth;

    public VibeDialogService(
            VibeAgentFactory agentFactory,
            PromptAssembler promptAssembler,
            SafetyModeFilter safetyModeFilter,
            ObjectMapper objectMapper) {
        this.agentFactory = agentFactory;
        this.promptAssembler = promptAssembler;
        this.safetyModeFilter = safetyModeFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行对话（流式，公共入口）
     */
    public void executeDialog(VibeDialogRequest request, VibeStreamCallback callback) {
        // 1. 计算安全模式
        SafetyMode safetyMode = SafetyMode.fromSpeed(request.environment().speed());
        log.info("开始对话: sessionId={}, safetyMode={}", request.sessionId(), safetyMode);

        VibeLoopState state = VibeLoopState.newTurn(request.sessionId(), safetyMode);
        callback.onStateUpdate(state);
        callback.onSafetyModeApplied(safetyMode);

        // 2. L3 静默模式前置过滤
        if (safetyMode == SafetyMode.L3_SILENT) {
            log.info("L3 静默模式，跳过主动推荐: sessionId={}", request.sessionId());
            callback.onComplete(AmbiencePlan.silent(), null);
            return;
        }

        VibeToolResults toolResults = new VibeToolResults(objectMapper);

        // 3. 开始递归
        try {
            executeTurn(request, callback, state, toolResults);
        } catch (Exception e) {
            log.error("对话执行异常: sessionId={}", request.sessionId(), e);
            callback.onError(e);
        }
    }

    /**
     * 执行对话（异步，返回 Future）
     */
    public CompletableFuture<VibeDialogResult> executeDialogAsync(VibeDialogRequest request) {
        CompletableFuture<VibeDialogResult> future = new CompletableFuture<>();
        AtomicReference<VibeLoopState> stateRef = new AtomicReference<>();
        Map<String, ToolInFlight> inFlightTools = new ConcurrentHashMap<>();
        List<ToolExecutionInfo> toolExecutions = new CopyOnWriteArrayList<>();

        executeDialog(request, new VibeStreamCallback() {
            @Override
            public void onTextDelta(String text) {
                // no-op
            }

            @Override
            public void onToolStart(String toolName, Object toolInput) {
                String arguments = toJsonString(toolInput);
                inFlightTools.put(toolName, new ToolInFlight(arguments, System.nanoTime()));
            }

            @Override
            public void onToolComplete(String toolName, String result) {
                ToolInFlight inFlight = inFlightTools.remove(toolName);
                long durationMs = inFlight != null ? nanosToMillis(System.nanoTime() - inFlight.startNanos()) : 0;
                toolExecutions.add(ToolExecutionInfo.success(toolName, inFlight != null ? inFlight.arguments() : null, result, durationMs));
            }

            @Override
            public void onToolError(String toolName, Throwable error) {
                ToolInFlight inFlight = inFlightTools.remove(toolName);
                long durationMs = inFlight != null ? nanosToMillis(System.nanoTime() - inFlight.startNanos()) : 0;
                String message = error != null ? error.getMessage() : "Unknown tool error";
                toolExecutions.add(ToolExecutionInfo.error(toolName, inFlight != null ? inFlight.arguments() : null, message, durationMs));
            }

            @Override
            public void onStateUpdate(VibeLoopState state) {
                stateRef.set(state);
            }

            @Override
            public void onComplete(AmbiencePlan plan, ChatResponse response) {
                VibeLoopState finalState = stateRef.get();
                if (plan == null) {
                    future.complete(VibeDialogResult.error("No plan generated", finalState, toolExecutions));
                } else {
                    future.complete(VibeDialogResult.success(plan, finalState, toolExecutions));
                }
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }

            @Override
            public void onSafetyModeApplied(SafetyMode mode) {
                // no-op
            }
        });

        return future;
    }

    /**
     * 执行对话轮次（递归核心）
     */
    private void executeTurn(
            VibeDialogRequest request,
            VibeStreamCallback callback,
            VibeLoopState state,
            VibeToolResults toolResults) {

        int depth = state.depth();
        log.info("执行对话轮次: sessionId={}, depth={}, elapsed={}ms",
                request.sessionId(), depth, state.getElapsedMillis());
        callback.onDepthUpdate(depth);
        callback.onStateUpdate(state);

        // 1. 递归深度检查
        if (depth >= maxRecursionDepth) {
            log.warn("超过最大递归深度: sessionId={}, depth={}", request.sessionId(), depth);
            callback.onWarning("达到最大递归深度（" + maxRecursionDepth + "），对话结束");
            AmbiencePlan plan = buildAmbiencePlan(null, state.safetyMode(), toolResults);
            AmbiencePlan filteredPlan = safetyModeFilter.apply(plan, state.safetyMode());
            callback.onComplete(filteredPlan, null);
            return;
        }

        // 2. 创建 Agent 并构建 Prompt
        VibeAgent agent = agentFactory.createAgent();
        String prompt = depth == 0
                ? promptAssembler.assembleUserPrompt(request.environment(), request.userPreferences())
                : "请基于已经获得的工具结果，输出最终的氛围推荐理由（简短），不要再调用任何工具。";

        // 3. 调用 Agent
        TokenStream tokenStream = agent.analyze(prompt, request.sessionId());

        // 4. 流式响应处理
        AtomicBoolean hasToolCall = new AtomicBoolean(false);
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
        AtomicReference<VibeLoopState> turnStateRef = new AtomicReference<>(state);
        CompletableFuture<Void> turnFuture = new CompletableFuture<>();

        tokenStream
                .onPartialResponse(callback::onTextDelta)
                .beforeToolExecution(before -> {
                    hasToolCall.set(true);
                    turnStateRef.updateAndGet(VibeLoopState::incrementToolCallCount);
                    callback.onStateUpdate(turnStateRef.get());
                    callback.onToolStart(
                            before.request().name(),
                            before.request().arguments()
                    );
                })
                .onToolExecuted(execution -> {
                    toolResults.updateFromToolExecution(execution.request().name(), execution.result());
                    callback.onToolComplete(
                            execution.request().name(),
                            execution.result()
                    );
                })
                .onCompleteResponse(response -> {
                    responseRef.set(response);
                    turnFuture.complete(null);
                })
                .onError(error -> {
                    turnFuture.completeExceptionally(error);
                })
                .start();

        // 5. 等待完成并处理递归
        turnFuture.whenComplete((v, error) -> {
            if (error != null) {
                log.error("对话轮次错误: sessionId={}, depth={}", request.sessionId(), depth, error);
                callback.onError(error);
                return;
            }

            // 6. 递归判断：当工具被调用且本轮未产出最终文本时，继续下一轮
            ChatResponse response = responseRef.get();
            boolean hasFinalText = response != null
                    && response.aiMessage() != null
                    && response.aiMessage().text() != null
                    && !response.aiMessage().text().isBlank();

            if (hasToolCall.get() && !hasFinalText) {
                // 有工具调用，递归继续
                log.info("检测到工具调用，递归继续: sessionId={}, nextDepth={}",
                        request.sessionId(), depth + 1);

                VibeLoopState nextState = turnStateRef.get().incrementDepth();
                executeTurn(request, callback, nextState, toolResults);
            } else {
                // 无工具调用，对话结束
                log.info("对话完成: sessionId={}, totalDepth={}, elapsed={}ms",
                        request.sessionId(), depth, state.getElapsedMillis());

                // 应用安全模式过滤
                AmbiencePlan plan = buildAmbiencePlan(response, state.safetyMode(), toolResults);
                AmbiencePlan filteredPlan = safetyModeFilter.apply(plan, state.safetyMode());

                if (filteredPlan != null && !filteredPlan.isComplete()) {
                    callback.onWarning("生成的方案不完整（可能缺少音乐/灯光/叙事），可调整 Prompt 或工具策略");
                }
                callback.onComplete(filteredPlan, response);
            }
        });
    }

    /**
     * 从响应构建 AmbiencePlan
     */
    private AmbiencePlan buildAmbiencePlan(ChatResponse response, SafetyMode safetyMode, VibeToolResults toolResults) {
        String reasoning = null;
        if (response != null && response.aiMessage() != null) {
            reasoning = response.aiMessage().text();
        }
        return AmbiencePlan.builder()
                .safetyMode(safetyMode)
                .reasoning(reasoning)
                .music(toolResults.music())
                .playResult(toolResults.playResult())
                .light(toolResults.light())
                .narrative(toolResults.narrative())
                .scent(toolResults.scent())
                .massage(toolResults.massage())
                .build();
    }

    private static final class VibeToolResults {
        private static final Logger log = LoggerFactory.getLogger(VibeToolResults.class);

        private final ObjectMapper objectMapper;
        private final AtomicReference<MusicRecommendation> music = new AtomicReference<>();
        private final AtomicReference<PlayResult> playResult = new AtomicReference<>();
        private final AtomicReference<LightSetting> light = new AtomicReference<>();
        private final AtomicReference<Narrative> narrative = new AtomicReference<>();
        private final AtomicReference<ScentSetting> scent = new AtomicReference<>();
        private final AtomicReference<MassageSetting> massage = new AtomicReference<>();

        private VibeToolResults(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        private void updateFromToolExecution(String toolName, String resultJson) {
            if (resultJson == null || resultJson.isBlank()) {
                return;
            }
            try {
                switch (toolName) {
                    case "recommendMusic" -> music.set(objectMapper.readValue(resultJson, MusicRecommendation.class));
                    case "playMusic" -> playResult.set(objectMapper.readValue(resultJson, PlayResult.class));
                    case "setLight" -> light.set(objectMapper.readValue(resultJson, LightSetting.class));
                    case "generateNarrative" -> narrative.set(objectMapper.readValue(resultJson, Narrative.class));
                    case "setScent" -> scent.set(objectMapper.readValue(resultJson, ScentSetting.class));
                    case "setMassage" -> massage.set(objectMapper.readValue(resultJson, MassageSetting.class));
                    case "searchMusic" -> log.debug("searchMusic result received (not stored)");
                    default -> log.debug("Skip tool result parsing: toolName={}", toolName);
                }
            } catch (Exception e) {
                log.warn("Failed to parse tool result: toolName={}, error={}", toolName, e.getMessage());
            }
        }

        private MusicRecommendation music() {
            return music.get();
        }

        private PlayResult playResult() {
            return playResult.get();
        }

        private LightSetting light() {
            return light.get();
        }

        private Narrative narrative() {
            return narrative.get();
        }

        private ScentSetting scent() {
            return scent.get();
        }

        private MassageSetting massage() {
            return massage.get();
        }
    }

    private record ToolInFlight(String arguments, long startNanos) {}

    private String toJsonString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }
}
