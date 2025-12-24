package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 香氛类型枚举
 * 定义车载香氛系统支持的香氛种类
 */
public enum ScentType {
    LAVENDER("lavender", "薰衣草", "放松助眠"),
    PEPPERMINT("peppermint", "薄荷", "提神醒脑"),
    OCEAN("ocean", "海洋", "清新自然"),
    FOREST("forest", "森林", "自然舒适"),
    CITRUS("citrus", "柑橘", "活力清爽"),
    VANILLA("vanilla", "香草", "温馨甜蜜"),
    NONE("none", "无", "关闭香氛");

    private final String value;
    private final String displayName;
    private final String effect;

    ScentType(String value, String displayName, String effect) {
        this.value = value;
        this.displayName = displayName;
        this.effect = effect;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEffect() {
        return effect;
    }

    @JsonCreator
    public static ScentType fromValue(String value) {
        for (ScentType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ScentType: " + value);
    }

    /**
     * 判断是否为放松类香氛
     */
    public boolean isRelaxing() {
        return this == LAVENDER || this == VANILLA || this == FOREST;
    }

    /**
     * 判断是否为提神类香氛
     */
    public boolean isEnergizing() {
        return this == PEPPERMINT || this == CITRUS;
    }

    /**
     * 判断是否为清新类香氛
     */
    public boolean isRefreshing() {
        return this == OCEAN || this == FOREST || this == CITRUS;
    }
}
