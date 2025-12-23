package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 安全模式枚举
 * 根据车速自动判断，限制氛围编排的激进程度
 */
public enum SafetyMode {
    /**
     * L1 正常模式（车速 < 60 km/h）
     */
    L1_NORMAL("L1_NORMAL", "正常模式", 0, 60),

    /**
     * L2 专注模式（车速 60-100 km/h）
     */
    L2_FOCUS("L2_FOCUS", "专注模式", 60, 100),

    /**
     * L3 静默模式（车速 >= 100 km/h）
     */
    L3_SILENT("L3_SILENT", "静默模式", 100, 200);

    private final String value;
    private final String displayName;
    private final int minSpeed;
    private final int maxSpeed;

    SafetyMode(String value, String displayName, int minSpeed, int maxSpeed) {
        this.value = value;
        this.displayName = displayName;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinSpeed() {
        return minSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    @JsonCreator
    public static SafetyMode fromValue(String value) {
        for (SafetyMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown SafetyMode: " + value);
    }

    /**
     * 根据车速判断安全模式
     */
    public static SafetyMode fromSpeed(double speed) {
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Speed must be between 0 and 200 km/h, got: " + speed);
        }
        if (speed < 60) {
            return L1_NORMAL;
        } else if (speed < 100) {
            return L2_FOCUS;
        } else {
            return L3_SILENT;
        }
    }

    /**
     * 判断是否允许动态灯光效果
     */
    public boolean allowsDynamicLighting() {
        return this == L1_NORMAL;
    }

    /**
     * 判断是否允许主动推荐
     */
    public boolean allowsProactiveRecommendation() {
        return this == L1_NORMAL || this == L2_FOCUS;
    }

    /**
     * 获取 TTS 音量系数
     */
    public double getTtsVolumeMultiplier() {
        return switch (this) {
            case L1_NORMAL, L2_FOCUS -> 1.0;
            case L3_SILENT -> 0.7;
        };
    }
}
