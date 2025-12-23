package com.vibe.model.event;

import com.vibe.model.enums.SafetyMode;
import dev.langchain4j.model.output.structured.Description;

import java.time.Instant;

/**
 * 安全模式变化事件
 */
@Description("安全模式变化事件")
public record SafetyModeChangedEvent(
    @Description("之前的安全模式")
    SafetyMode previousMode,

    @Description("当前安全模式")
    SafetyMode currentMode,

    @Description("当前车速")
    double speed,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public SafetyModeChangedEvent(SafetyMode previousMode, SafetyMode currentMode, double speed) {
        this(previousMode, currentMode, speed, Instant.now());
    }

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public SafetyModeChangedEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 判断是否升级到更严格的模式
     */
    public boolean isUpgrade() {
        if (previousMode == null || currentMode == null) {
            return false;
        }
        return currentMode.ordinal() > previousMode.ordinal();
    }

    /**
     * 判断是否降级到更宽松的模式
     */
    public boolean isDowngrade() {
        if (previousMode == null || currentMode == null) {
            return false;
        }
        return currentMode.ordinal() < previousMode.ordinal();
    }

    /**
     * 获取变化描述
     */
    public String getChangeDescription() {
        if (previousMode == null || currentMode == null) {
            return "安全模式已变化";
        }
        return String.format("安全模式从 %s 变为 %s（车速 %.0f km/h）",
            previousMode.getDisplayName(),
            currentMode.getDisplayName(),
            speed);
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "safety_mode_changed";
}
