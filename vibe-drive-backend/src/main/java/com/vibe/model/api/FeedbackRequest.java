package com.vibe.model.api;

import com.vibe.model.enums.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户反馈请求
 */
public record FeedbackRequest(
    @NotBlank(message = "Session ID cannot be empty")
    String sessionId,

    @NotBlank(message = "Plan ID cannot be empty")
    String planId,

    @NotNull(message = "Feedback type cannot be null")
    FeedbackType type,

    String comment
) {
    /**
     * 创建点赞反馈
     */
    public static FeedbackRequest like(String sessionId, String planId) {
        return new FeedbackRequest(sessionId, planId, FeedbackType.LIKE, null);
    }

    /**
     * 创建点踩反馈
     */
    public static FeedbackRequest dislike(String sessionId, String planId, String comment) {
        return new FeedbackRequest(sessionId, planId, FeedbackType.DISLIKE, comment);
    }

    /**
     * 创建跳过反馈
     */
    public static FeedbackRequest skip(String sessionId, String planId) {
        return new FeedbackRequest(sessionId, planId, FeedbackType.SKIP, null);
    }
}
