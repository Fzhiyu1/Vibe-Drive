package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 灯光模式枚举
 * 定义氛围灯的动态效果类型
 */
public enum LightMode {
    STATIC("static", "静态"),
    BREATHING("breathing", "呼吸"),
    GRADIENT("gradient", "渐变"),
    PULSE("pulse", "脉冲");

    private final String value;
    private final String displayName;

    LightMode(String value, String displayName) {
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
    public static LightMode fromValue(String value) {
        for (LightMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown LightMode: " + value);
    }

    /**
     * 判断是否为动态效果（需要 L1 正常模式）
     */
    public boolean isDynamic() {
        return this == GRADIENT || this == PULSE;
    }

    /**
     * 判断是否适合舒缓场景
     */
    public boolean isSoothing() {
        return this == STATIC || this == BREATHING;
    }
}
