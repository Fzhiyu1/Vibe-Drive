package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 叙事情感枚举
 * 定义 TTS 播报时的情感基调
 */
public enum NarrativeEmotion {
    WARM("warm", "温暖"),
    ENERGETIC("energetic", "活力"),
    ROMANTIC("romantic", "浪漫"),
    ADVENTUROUS("adventurous", "冒险"),
    CALM("calm", "平静");

    private final String value;
    private final String displayName;

    NarrativeEmotion(String value, String displayName) {
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
    public static NarrativeEmotion fromValue(String value) {
        for (NarrativeEmotion emotion : values()) {
            if (emotion.value.equalsIgnoreCase(value)) {
                return emotion;
            }
        }
        throw new IllegalArgumentException("Unknown NarrativeEmotion: " + value);
    }

    /**
     * 判断是否为舒缓情感（适合疲劳/压力状态）
     */
    public boolean isSoothing() {
        return this == WARM || this == CALM;
    }

    /**
     * 判断是否为激励情感（适合活力场景）
     */
    public boolean isUplifting() {
        return this == ENERGETIC || this == ADVENTUROUS;
    }
}
