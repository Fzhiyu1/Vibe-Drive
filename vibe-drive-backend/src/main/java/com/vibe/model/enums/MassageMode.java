package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 按摩模式枚举
 * 定义座椅按摩系统支持的按摩模式
 */
public enum MassageMode {
    RELAX("relax", "放松模式", "轻柔舒缓，适合长途驾驶"),
    ENERGIZE("energize", "活力模式", "节奏明快，提神醒脑"),
    COMFORT("comfort", "舒适模式", "均衡按摩，日常使用"),
    SPORT("sport", "运动模式", "深度按摩，缓解肌肉疲劳"),
    OFF("off", "关闭", "停止按摩");

    private final String value;
    private final String displayName;
    private final String description;

    MassageMode(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static MassageMode fromValue(String value) {
        for (MassageMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown MassageMode: " + value);
    }

    /**
     * 判断是否为放松类模式
     */
    public boolean isRelaxing() {
        return this == RELAX || this == COMFORT;
    }

    /**
     * 判断是否为活力类模式
     */
    public boolean isEnergizing() {
        return this == ENERGIZE || this == SPORT;
    }

    /**
     * 判断是否适合高速驾驶
     */
    public boolean isSafeForHighSpeed() {
        return this == OFF || this == COMFORT;
    }
}
