package com.vibe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import com.vibe.model.event.AgentStatusChangedEvent;
import com.vibe.model.event.AmbienceChangedEvent;
import com.vibe.model.event.SafetyModeChangedEvent;
import com.vibe.model.enums.SafetyMode;
import com.vibe.model.api.VibeStatus;
import com.vibe.orchestration.callback.SseVibeCallback;
import com.vibe.orchestration.callback.VibeStreamCallback;
import com.vibe.orchestration.dto.VibeDialogRequest;
import com.vibe.orchestration.service.VibeDialogService;
import com.vibe.sse.SseEventPublisher;
import com.vibe.status.VibeSessionStatusStore;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

/**
 * Vibe SSE 流式 API Controller
 */
@RestController
@RequestMapping("/api/vibe")
@Tag(name = "Vibe SSE API", description = "氛围编排流式 API")
public class VibeStreamController {

    private static final Logger log = LoggerFactory.getLogger(VibeStreamController.class);
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

    private final VibeDialogService dialogService;
    private final SseEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final VibeSessionStatusStore statusStore;

    public VibeStreamController(
            VibeDialogService dialogService,
            SseEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            VibeSessionStatusStore statusStore) {
        this.dialogService = dialogService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.statusStore = statusStore;
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式分析", description = "流式分析环境数据，通过 SSE 推送事件")
    public SseEmitter analyzeStream(
            @RequestParam String sessionId,
            @RequestBody Environment environment,
            @RequestParam(required = false) String preferences,
            @RequestParam(defaultValue = "false") boolean debug) {

        log.info("开始流式分析: sessionId={}", sessionId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 设置回调
        emitter.onCompletion(() -> log.debug("SSE 完成: sessionId={}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE 超时: sessionId={}", sessionId));
        emitter.onError(e -> log.error("SSE 错误: sessionId={}", sessionId, e));

        SafetyMode safetyMode = SafetyMode.fromSpeed(environment.speed());
        VibeStatus previousStatus = statusStore.getOrInitial(sessionId);
        if (previousStatus.currentSafetyMode() != safetyMode) {
            eventPublisher.publish(
                sessionId,
                SafetyModeChangedEvent.EVENT_TYPE,
                new SafetyModeChangedEvent(previousStatus.currentSafetyMode(), safetyMode, environment.speed())
            );
        }

        // 更新会话状态：处理中
        statusStore.put(sessionId, VibeStatus.processing(sessionId, safetyMode, previousStatus.currentPlan(), environment));

        // 通知 agent 启动
        eventPublisher.publish(sessionId, AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.started());

        // 创建回调并执行
        SseVibeCallback callback = new SseVibeCallback(emitter, objectMapper, sessionId, debug);
        VibeDialogRequest request = VibeDialogRequest.of(sessionId, environment, preferences);

        VibeStreamCallback compositeCallback = new VibeStreamCallback() {
            @Override
            public void onTextDelta(String text) {
                callback.onTextDelta(text);
            }

            @Override
            public void onToolStart(String toolName, Object toolInput) {
                callback.onToolStart(toolName, toolInput);
            }

            @Override
            public void onToolComplete(String toolName, String result) {
                callback.onToolComplete(toolName, result);
            }

            @Override
            public void onToolError(String toolName, Throwable error) {
                callback.onToolError(toolName, error);
            }

            @Override
            public void onComplete(AmbiencePlan plan, ChatResponse response) {
                callback.onComplete(plan, response);
                if (plan != null) {
                    statusStore.put(sessionId, VibeStatus.completed(sessionId, plan.safetyMode(), plan, environment));
                    eventPublisher.publish(sessionId, AmbienceChangedEvent.EVENT_TYPE, AmbienceChangedEvent.fromUserRequest(plan));
                } else {
                    statusStore.put(sessionId, VibeStatus.completed(sessionId, safetyMode, previousStatus.currentPlan(), environment));
                }
                eventPublisher.publish(sessionId, AgentStatusChangedEvent.EVENT_TYPE, AgentStatusChangedEvent.stopped());
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
                statusStore.put(sessionId, VibeStatus.completed(sessionId, safetyMode, previousStatus.currentPlan(), environment));
                eventPublisher.publish(
                    sessionId,
                    AgentStatusChangedEvent.EVENT_TYPE,
                    AgentStatusChangedEvent.error(error != null ? error.getMessage() : "Unknown error")
                );
            }

            @Override
            public void onSafetyModeApplied(SafetyMode mode) {
                callback.onSafetyModeApplied(mode);
            }
        };

        dialogService.executeDialog(request, compositeCallback);

        return emitter;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅事件", description = "订阅实时事件（氛围变化、安全模式变化等）")
    public SseEmitter subscribeEvents(
            @RequestParam String sessionId,
            @RequestParam(required = false) Set<String> topics) {

        log.info("订阅事件: sessionId={}, topics={}", sessionId, topics);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 注册到事件发布器
        eventPublisher.register(sessionId, emitter, topics);

        // 设置回调
        emitter.onCompletion(() -> {
            log.debug("事件订阅完成: sessionId={}", sessionId);
            eventPublisher.unregister(sessionId, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("事件订阅超时: sessionId={}", sessionId);
            eventPublisher.unregister(sessionId, emitter);
        });
        emitter.onError(e -> {
            log.error("事件订阅错误: sessionId={}", sessionId, e);
            eventPublisher.unregister(sessionId, emitter);
        });

        return emitter;
    }
}
