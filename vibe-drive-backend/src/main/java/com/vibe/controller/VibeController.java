package com.vibe.controller;

import com.vibe.agent.EnvironmentAgent;
import com.vibe.agent.EnvironmentAgentFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.Environment;
import com.vibe.model.api.*;
import com.vibe.model.event.AgentStatusChangedEvent;
import com.vibe.simulator.EnvironmentSimulator;
import com.vibe.simulator.ScenarioType;
import com.vibe.model.event.AmbienceChangedEvent;
import com.vibe.model.event.SafetyModeChangedEvent;
import com.vibe.model.enums.SafetyMode;
import com.vibe.orchestration.dto.VibeDialogRequest;
import com.vibe.orchestration.dto.VibeDialogResult;
import com.vibe.orchestration.service.VibeDialogService;
import com.vibe.sse.SseEventPublisher;
import com.vibe.status.VibeSessionStatusStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vibe REST API Controller
 */
@RestController
@RequestMapping("/api/vibe")
@Tag(name = "Vibe API", description = "氛围编排 REST API")
public class VibeController {

    private static final Logger log = LoggerFactory.getLogger(VibeController.class);
    private static final long ANALYZE_TIMEOUT_SECONDS = 60;

    private final VibeDialogService dialogService;
    private final VibeSessionStatusStore statusStore;
    private final SseEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final EnvironmentSimulator environmentSimulator;
    private final EnvironmentAgent environmentAgent;

    public VibeController(
            VibeDialogService dialogService,
            VibeSessionStatusStore statusStore,
            SseEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            EnvironmentSimulator environmentSimulator,
            EnvironmentAgentFactory environmentAgentFactory) {
        this.dialogService = dialogService;
        this.statusStore = statusStore;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.environmentSimulator = environmentSimulator;
        this.environmentAgent = environmentAgentFactory.createAgent();
    }

    @PostMapping("/analyze")
    @Operation(summary = "分析环境", description = "同步分析环境数据，返回氛围方案")
    public ApiResponse<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        log.info("收到分析请求: sessionId={}", request.sessionId());

        try {
            // 转换为内部请求格式
            String preferences = null;
            if (request.hasPreferences()) {
                try {
                    preferences = objectMapper.writeValueAsString(request.preferences());
                } catch (Exception e) {
                    preferences = request.preferences().toString();
                }
            }
            VibeDialogRequest dialogRequest = VibeDialogRequest.of(
                request.sessionId(),
                request.environment(),
                preferences
            );

            SafetyMode safetyMode = SafetyMode.fromSpeed(request.environment().speed());
            VibeStatus previousStatus = statusStore.getOrInitial(request.sessionId());
            if (previousStatus.currentSafetyMode() != safetyMode) {
                eventPublisher.publish(
                    request.sessionId(),
                    SafetyModeChangedEvent.EVENT_TYPE,
                    new SafetyModeChangedEvent(previousStatus.currentSafetyMode(), safetyMode, request.environment().speed())
                );
            }

            statusStore.put(request.sessionId(), VibeStatus.processing(
                request.sessionId(),
                safetyMode,
                previousStatus.currentPlan(),
                request.environment()
            ));
            eventPublisher.publish(request.sessionId(), AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.started());

            // 执行分析
            long startTime = System.currentTimeMillis();
            var future = dialogService.executeDialogAsync(dialogRequest);
            VibeDialogResult result = future.get(ANALYZE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            long processingTime = System.currentTimeMillis() - startTime;

            if (result.success()) {
                // 更新会话状态（已完成）
                statusStore.put(request.sessionId(), VibeStatus.completed(
                    request.sessionId(),
                    safetyMode,
                    result.plan(),
                    request.environment()
                ));
                if (result.plan() != null) {
                    eventPublisher.publish(request.sessionId(), AmbienceChangedEvent.EVENT_TYPE, AmbienceChangedEvent.fromUserRequest(result.plan()));
                }
                eventPublisher.publish(request.sessionId(), AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.stopped());

                AnalyzeResponse response = AnalyzeResponse.applied(
                    result.plan(),
                    null,
                    result.toolExecutions(),
                    processingTime
                );
                return ApiResponse.success(response);
            } else {
                statusStore.put(request.sessionId(), VibeStatus.completed(
                    request.sessionId(),
                    safetyMode,
                    previousStatus.currentPlan(),
                    request.environment()
                ));
                eventPublisher.publish(request.sessionId(), AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.stopped());
                AnalyzeResponse response = AnalyzeResponse.noAction(
                    result.errorMessage(),
                    null,
                    processingTime
                );
                return ApiResponse.success(response);
            }

        } catch (TimeoutException e) {
            log.error("分析超时: sessionId={}", request.sessionId());
            statusStore.put(request.sessionId(), VibeStatus.completed(
                request.sessionId(),
                SafetyMode.fromSpeed(request.environment().speed()),
                statusStore.getOrInitial(request.sessionId()).currentPlan(),
                request.environment()
            ));
            eventPublisher.publish(request.sessionId(), AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.error("LLM timeout"));
            return ApiResponse.error(ApiResponse.ERROR_LLM_TIMEOUT, "分析超时，请稍后重试");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String message = cause != null ? cause.getMessage() : e.getMessage();
            log.error("分析执行错误: sessionId={}", request.sessionId(), cause != null ? cause : e);
            statusStore.put(request.sessionId(), VibeStatus.completed(
                request.sessionId(),
                SafetyMode.fromSpeed(request.environment().speed()),
                statusStore.getOrInitial(request.sessionId()).currentPlan(),
                request.environment()
            ));
            eventPublisher.publish(
                request.sessionId(),
                AgentStatusChangedEvent.EVENT_TYPE,
                AgentStatusChangedEvent.error(message != null ? message : "Execution error")
            );
            return ApiResponse.llmError(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分析被中断: sessionId={}", request.sessionId());
            statusStore.put(request.sessionId(), VibeStatus.completed(
                request.sessionId(),
                SafetyMode.fromSpeed(request.environment().speed()),
                statusStore.getOrInitial(request.sessionId()).currentPlan(),
                request.environment()
            ));
            eventPublisher.publish(request.sessionId(), AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.error("Interrupted"));
            return ApiResponse.internalError("分析被中断");
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取状态", description = "获取当前会话的氛围状态")
    public ApiResponse<VibeStatus> getStatus(@RequestParam String sessionId) {
        log.debug("获取状态: sessionId={}", sessionId);

        VibeStatus status = statusStore.getOrInitial(sessionId);
        return ApiResponse.success(status);
    }

    @PostMapping("/feedback")
    @Operation(summary = "提交反馈", description = "提交用户对氛围方案的反馈")
    public ApiResponse<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("收到反馈: sessionId={}, planId={}, type={}",
            request.sessionId(), request.planId(), request.type());

        // TODO: 存储反馈用于后续优化
        // 目前仅记录日志

        return ApiResponse.success(null);
    }

    @GetMapping("/simulator/scenario")
    @Operation(summary = "获取模拟场景", description = "根据场景类型生成模拟环境数据")
    public Environment getScenario(@RequestParam ScenarioType type) {
        log.info("获取模拟场景: type={}", type);
        return environmentSimulator.generateScenario(type);
    }

    @PostMapping("/environment/generate")
    @Operation(summary = "AI生成环境", description = "根据自然语言描述生成环境数据")
    public Environment generateEnvironment(@RequestBody GenerateEnvRequest request) {
        log.info("AI生成环境: description={}", request.description());
        return environmentAgent.generate(request.description());
    }

    @PostMapping("/environment/sync")
    @Operation(summary = "同步环境", description = "将前端环境数据同步到后端会话存储")
    public ApiResponse<Void> syncEnvironment(
            @RequestParam String sessionId,
            @RequestBody Environment environment) {
        log.info("同步环境: sessionId={}", sessionId);

        SafetyMode safetyMode = SafetyMode.fromSpeed(environment.speed());
        VibeStatus currentStatus = statusStore.getOrInitial(sessionId);

        statusStore.put(sessionId, VibeStatus.completed(
            sessionId,
            safetyMode,
            currentStatus.currentPlan(),
            environment
        ));

        return ApiResponse.success(null);
    }
}
