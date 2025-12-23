package com.vibe.model.enums;

/**
 * 叙事情感枚举
 * 定义 TTS 播报时的情感基调
 */
public enum NarrativeEmotion {
    /**
     * 温暖 - 适合日常通勤、家庭出行
     */
    WARM("温暖"),

    /**
     * 活力 - 适合愉快心情、郊游场景
     */
    ENERGETIC("活力"),

    /**
     * 浪漫 - 适合海滨、傍晚、情侣出行
     */
    ROMANTIC("浪漫"),

    /**
     * 冒险 - 适合山路、探索路线
     */
    ADVENTUROUS("冒险"),

    /**
     * 平静 - 适合疲劳、深夜、雨天
     */
    CALM("平静");

    private final String displayName;

    NarrativeEmotion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
