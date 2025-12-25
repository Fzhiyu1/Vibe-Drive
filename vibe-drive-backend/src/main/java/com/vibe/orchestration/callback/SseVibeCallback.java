package com.vibe.orchestration.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.AmbiencePlan;
import com.vibe.model.ToolExecutionInfo;
import com.vibe.model.api.AnalyzeResponse;
import com.vibe.model.event.ErrorEvent;
import com.vibe.model.event.TokenEvent;
import com.vibe.model.event.ToolEndEvent;
import com.vibe.model.event.ToolStartEvent;
import com.vibe.model.enums.SafetyMode;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 流式回调实现
 * 将 Agent 编排事件通过 SSE 推送到前端
 */
public class SseVibeCallback implements VibeStreamCallback {

    private static final Logger log = LoggerFactory.getLogger(SseVibeCallback.class);

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private final String sessionId;
    private final boolean debugEvents;
    private final long dialogStartNanos = System.nanoTime();
    private final Map<String, ToolInFlight> inFlightTools = new ConcurrentHashMap<>();
    private final List<ToolExecutionInfo> toolExecutions = new CopyOnWriteArrayList<>();

    public SseVibeCallback(SseEmitter emitter, ObjectMapper objectMapper, String sessionId) {
        this(emitter, objectMapper, sessionId, false);
    }

    public SseVibeCallback(SseEmitter emitter, ObjectMapper objectMapper, String sessionId, boolean debugEvents) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
        this.sessionId = sessionId;
        this.debugEvents = debugEvents;
    }

    @Override
    public void onTextDelta(String text) {
        if (!debugEvents) {
            return;
        }
        sendEvent(TokenEvent.EVENT_TYPE, new TokenEvent(text));
    }

    @Override
    public void onToolStart(String toolName, Object toolInput) {
        String arguments = toJsonString(toolInput);
        inFlightTools.put(toolName, new ToolInFlight(arguments, System.nanoTime()));
        if (!debugEvents) {
            return;
        }
        sendEvent(ToolStartEvent.EVENT_TYPE, new ToolStartEvent(toolName, arguments));
    }

    @Override
    public void onToolComplete(String toolName, String result) {
        ToolInFlight inFlight = inFlightTools.remove(toolName);
        long durationMs = inFlight != null ? nanosToMillis(System.nanoTime() - inFlight.startNanos()) : 0;
        toolExecutions.add(ToolExecutionInfo.success(toolName, inFlight != null ? inFlight.arguments() : null, result, durationMs));
        if (!debugEvents) {
            return;
        }
        sendEvent(ToolEndEvent.EVENT_TYPE, ToolEndEvent.success(toolName, result, durationMs));
    }

    @Override
    public void onToolError(String toolName, Throwable error) {
        ToolInFlight inFlight = inFlightTools.remove(toolName);
        long durationMs = inFlight != null ? nanosToMillis(System.nanoTime() - inFlight.startNanos()) : 0;
        String message = error != null ? error.getMessage() : "Unknown tool error";
        toolExecutions.add(ToolExecutionInfo.error(toolName, inFlight != null ? inFlight.arguments() : null, message, durationMs));
        if (!debugEvents) {
            return;
        }
        sendEvent(ToolEndEvent.EVENT_TYPE, ToolEndEvent.error(toolName, durationMs, message));
    }

    @Override
    public void onComplete(AmbiencePlan plan, ChatResponse response) {
        long processingTimeMs = nanosToMillis(System.nanoTime() - dialogStartNanos);
        AnalyzeResponse payload = plan != null
            ? AnalyzeResponse.applied(plan, null, toolExecutions, processingTimeMs)
            : AnalyzeResponse.noAction("No plan generated", null, processingTimeMs);
        sendEvent("complete", payload);
        completeEmitter();
    }

    @Override
    public void onError(Throwable error) {
        log.error("Dialog error: sessionId={}", sessionId, error);
        String message = error != null ? error.getMessage() : "Unknown error";
        sendEvent(ErrorEvent.EVENT_TYPE, ErrorEvent.llmError(message));
        completeEmitter();
    }

    @Override
    public void onSafetyModeApplied(SafetyMode mode) {
        // analyze/stream SSE 事件模型不包含安全模式事件；安全模式变化由 /api/vibe/events 推送
    }

    @Override
    public void onDepthUpdate(int depth) {
        // no-op
    }

    @Override
    public void onWarning(String message) {
        log.warn("Dialog warning: sessionId={}, message={}", sessionId, message);
    }

    private void sendEvent(String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(json));
        } catch (Exception e) {
            log.warn("Failed to send SSE event: {}", eventName, e);
        }
    }

    private void completeEmitter() {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("Failed to complete SSE emitter", e);
        }
    }

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

    private record ToolInFlight(String arguments, long startNanos) {}
}
