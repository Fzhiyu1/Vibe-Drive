package com.vibe.orchestration.dto;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;

/**
 * 对话请求
 */
public record VibeDialogRequest(
    String sessionId,
    String taskId,
    Environment environment,
    String vibeDescription,
    String userPreferences,
    String message,
    AmbiencePlan currentPlan,
    String changeDescription,
    Integer currentPlaylistIndex
) {
    /**
     * 创建初始请求
     */
    public static VibeDialogRequest of(String sessionId, Environment environment) {
        return new VibeDialogRequest(sessionId, null, environment, null, null, null, null, null, null);
    }

    /**
     * 创建带 taskId 的请求
     */
    public static VibeDialogRequest of(String sessionId, String taskId, Environment environment) {
        return new VibeDialogRequest(sessionId, taskId, environment, null, null, null, null, null, null);
    }

    /**
     * 创建带 taskId 和氛围描述的请求
     */
    public static VibeDialogRequest of(String sessionId, String taskId, Environment environment, String vibeDescription) {
        return new VibeDialogRequest(sessionId, taskId, environment, vibeDescription, null, null, null, null, null);
    }

    /**
     * 创建带偏好的请求
     */
    public static VibeDialogRequest of(String sessionId, Environment environment, String userPreferences) {
        return new VibeDialogRequest(sessionId, null, environment, null, userPreferences, null, null, null, null);
    }

    /**
     * 创建增量调整请求
     */
    public static VibeDialogRequest forAdjustment(
            String sessionId,
            String taskId,
            Environment environment,
            AmbiencePlan currentPlan,
            String changeDescription,
            Integer currentPlaylistIndex) {
        return new VibeDialogRequest(sessionId, taskId, environment, null, null, null, currentPlan, changeDescription, currentPlaylistIndex);
    }

    /**
     * 是否为增量调整请求
     */
    public boolean isAdjustment() {
        return currentPlan != null && changeDescription != null && !changeDescription.isBlank();
    }

    /**
     * 是否有氛围描述
     */
    public boolean hasVibeDescription() {
        return vibeDescription != null && !vibeDescription.isBlank();
    }

    /**
     * 创建递归继续请求
     */
    public VibeDialogRequest withContinueMessage() {
        return new VibeDialogRequest(sessionId, taskId, environment, vibeDescription, userPreferences, "请继续执行任务", currentPlan, changeDescription, currentPlaylistIndex);
    }
}
