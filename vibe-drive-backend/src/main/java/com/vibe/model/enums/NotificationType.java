package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知类型枚举
 * 用于 Vibe Agent 向主智能体或前端发送的通知分类
 */
public enum NotificationType {
    AMBIENCE_CHANGED("ambience_changed", "氛围已切换"),
    SAFETY_MODE_CHANGED("safety_mode_changed", "安全模式变化"),
    AGENT_STARTED("agent_started", "Agent 启动"),
    AGENT_STOPPED("agent_stopped", "Agent 停止");

    private final String value;
    private final String displayName;

    NotificationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static NotificationType fromValue(String value) {
        for (NotificationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationType: " + value);
    }

    /**
     * 判断是否为状态变化类通知
     */
    public boolean isStateChange() {
        return this == AMBIENCE_CHANGED || this == SAFETY_MODE_CHANGED;
    }

    /**
     * 判断是否为 Agent 生命周期通知
     */
    public boolean isLifecycleEvent() {
        return this == AGENT_STARTED || this == AGENT_STOPPED;
    }
}
