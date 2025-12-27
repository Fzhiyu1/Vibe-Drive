package com.vibe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.orchestration.callback.MasterStreamCallback;
import com.vibe.orchestration.service.MasterDialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 主智能体 API Controller
 */
@RestController
@RequestMapping("/api/master")
@Tag(name = "Master Agent API", description = "主智能体对话 API")
public class MasterController {

    private static final Logger log = LoggerFactory.getLogger(MasterController.class);
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    private final MasterDialogService dialogService;
    private final ObjectMapper objectMapper;

    public MasterController(MasterDialogService dialogService, ObjectMapper objectMapper) {
        this.dialogService = dialogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "与主智能体进行流式对话")
    public SseEmitter chatStream(
            @RequestParam String sessionId,
            @RequestBody ChatRequest request) {

        log.info("开始主智能体对话: sessionId={}, message={}", sessionId, request.message());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> log.debug("SSE 完成: sessionId={}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE 超时: sessionId={}", sessionId));
        emitter.onError(e -> log.error("SSE 错误: sessionId={}", sessionId, e));

        MasterStreamCallback callback = createCallback(emitter, sessionId);
        dialogService.executeChat(sessionId, request.message(), callback);

        return emitter;
    }

    private MasterStreamCallback createCallback(SseEmitter emitter, String sessionId) {
        return new MasterStreamCallback() {
            @Override
            public void onTextDelta(String text) {
                sendEvent(emitter, "token", Map.of("text", text));
            }

            @Override
            public void onToolStart(String toolName, Object toolInput) {
                sendEvent(emitter, "tool_start", Map.of(
                    "toolName", toolName,
                    "input", toolInput != null ? toolInput : ""
                ));
            }

            @Override
            public void onToolComplete(String toolName, String result) {
                sendEvent(emitter, "tool_end", Map.of(
                    "toolName", toolName,
                    "result", result != null ? result : ""
                ));
            }

            @Override
            public void onComplete() {
                sendEvent(emitter, "complete", Map.of());
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                String message = error != null ? error.getMessage() : "Unknown error";
                sendEvent(emitter, "error", Map.of(
                    "code", "MASTER_ERROR",
                    "message", message
                ));
                emitter.completeWithError(error);
            }
        };
    }

    private void sendEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(json));
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: eventType={}", eventType, e);
        }
    }

    public record ChatRequest(String message) {}
}
