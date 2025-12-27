package com.vibe.orchestration.dto;

import com.vibe.model.Environment;

/**
 * 对话请求
 */
public record VibeDialogRequest(
    String sessionId,
    String taskId,
    Environment environment,
    String userPreferences,
    String message
) {
    /**
     * 创建初始请求
     */
    public static VibeDialogRequest of(String sessionId, Environment environment) {
        return new VibeDialogRequest(sessionId, null, environment, null, null);
    }

    /**
     * 创建带 taskId 的请求
     */
    public static VibeDialogRequest of(String sessionId, String taskId, Environment environment) {
        return new VibeDialogRequest(sessionId, taskId, environment, null, null);
    }

    /**
     * 创建带偏好的请求
     */
    public static VibeDialogRequest of(String sessionId, Environment environment, String userPreferences) {
        return new VibeDialogRequest(sessionId, null, environment, userPreferences, null);
    }

    /**
     * 创建递归继续请求
     */
    public VibeDialogRequest withContinueMessage() {
        return new VibeDialogRequest(sessionId, taskId, environment, userPreferences, "请继续执行任务");
    }
}
