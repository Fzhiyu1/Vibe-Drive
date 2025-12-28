package com.vibe.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 支持降级的流式聊天模型
 * 当主模型失败时自动切换到降级模型，冷却期后尝试恢复
 */
public class FallbackStreamingChatModel implements StreamingChatModel {

    private static final Logger log = LoggerFactory.getLogger(FallbackStreamingChatModel.class);

    private final StreamingChatModel primaryModel;
    private final StreamingChatModel fallbackModel;
    private final String primaryName;
    private final String fallbackName;
    private final Duration cooldownDuration;

    // 记录主模型失败时间
    private final AtomicReference<Instant> primaryFailedAt = new AtomicReference<>(null);

    public FallbackStreamingChatModel(
            StreamingChatModel primaryModel,
            StreamingChatModel fallbackModel,
            String primaryName,
            String fallbackName,
            Duration cooldownDuration) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.primaryName = primaryName;
        this.fallbackName = fallbackName;
        this.cooldownDuration = cooldownDuration;
    }

    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        StreamingChatModel activeModel = selectModel();
        String activeName = activeModel == primaryModel ? primaryName : fallbackName;

        log.info("使用模型: {}", activeName);

        activeModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                handler.onPartialResponse(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 主模型成功，清除失败记录
                if (activeModel == primaryModel) {
                    primaryFailedAt.set(null);
                }
                handler.onCompleteResponse(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                if (activeModel == primaryModel) {
                    log.warn("主模型 {} 失败，切换到降级模型 {}: {}",
                            primaryName, fallbackName, error.getMessage());
                    primaryFailedAt.set(Instant.now());

                    // 使用降级模型重试
                    fallbackModel.chat(chatRequest, handler);
                } else {
                    // 降级模型也失败了
                    log.error("降级模型 {} 也失败: {}", fallbackName, error.getMessage());
                    handler.onError(error);
                }
            }
        });
    }

    /**
     * 选择当前应该使用的模型
     */
    private StreamingChatModel selectModel() {
        Instant failedAt = primaryFailedAt.get();

        if (failedAt == null) {
            return primaryModel;
        }

        // 检查冷却时间是否已过
        if (Instant.now().isAfter(failedAt.plus(cooldownDuration))) {
            log.info("冷却时间已过，尝试恢复主模型 {}", primaryName);
            return primaryModel;
        }

        long remainingSeconds = Duration.between(Instant.now(), failedAt.plus(cooldownDuration)).getSeconds();
        log.debug("主模型 {} 仍在冷却中，剩余 {}秒，使用降级模型 {}",
                primaryName, remainingSeconds, fallbackName);
        return fallbackModel;
    }

    /**
     * 获取当前活跃模型名称
     */
    public String getActiveModelName() {
        return selectModel() == primaryModel ? primaryName : fallbackName;
    }

    /**
     * 手动重置，立即尝试主模型
     */
    public void resetToPrimary() {
        primaryFailedAt.set(null);
        log.info("手动重置，恢复主模型 {}", primaryName);
    }
}
