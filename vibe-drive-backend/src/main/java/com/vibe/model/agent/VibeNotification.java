package com.vibe.model.agent;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.enums.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Vibe 通知
 * 用于 Vibe Agent 向主智能体或前端发送的通知
 */
public record VibeNotification(
    String id,
    @NotNull(message = "Notification type cannot be null")
    NotificationType type,
    @Valid
    AmbiencePlan plan,
    String summary,
    Instant timestamp
) {
    /**
     * 紧凑构造器：设置默认值
     */
    public VibeNotification {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 创建氛围变化通知
     */
    public static VibeNotification ambienceChanged(AmbiencePlan plan, String summary) {
        return new VibeNotification(
            null,
            NotificationType.AMBIENCE_CHANGED,
            plan,
            summary,
            null
        );
    }

    /**
     * 创建安全模式变化通知
     */
    public static VibeNotification safetyModeChanged(String summary) {
        return new VibeNotification(
            null,
            NotificationType.SAFETY_MODE_CHANGED,
            null,
            summary,
            null
        );
    }

    /**
     * 创建 Agent 启动通知
     */
    public static VibeNotification agentStarted() {
        return new VibeNotification(
            null,
            NotificationType.AGENT_STARTED,
            null,
            "Vibe Agent 已启动",
            null
        );
    }

    /**
     * 创建 Agent 停止通知
     */
    public static VibeNotification agentStopped() {
        return new VibeNotification(
            null,
            NotificationType.AGENT_STOPPED,
            null,
            "Vibe Agent 已停止",
            null
        );
    }

    /**
     * 判断是否为状态变化通知
     */
    public boolean isStateChange() {
        return type != null && type.isStateChange();
    }

    /**
     * 判断是否为生命周期通知
     */
    public boolean isLifecycleEvent() {
        return type != null && type.isLifecycleEvent();
    }

    /**
     * 判断是否包含氛围方案
     */
    public boolean hasPlan() {
        return plan != null;
    }
}
