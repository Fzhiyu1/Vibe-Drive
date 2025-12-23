package com.vibe.model.enums;

/**
 * 天气状况枚举
 * 影响灯光氛围和音乐选择
 */
public enum Weather {
    SUNNY("晴天"),
    CLOUDY("多云"),
    RAINY("雨天"),
    SNOWY("雪天"),
    FOGGY("雾天");

    private final String displayName;

    Weather(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判断是否为恶劣天气（需要更谨慎的驾驶）
     */
    public boolean isSevereWeather() {
        return this == RAINY || this == SNOWY || this == FOGGY;
    }

    /**
     * 判断是否适合明亮氛围
     */
    public boolean isBrightAmbience() {
        return this == SUNNY || this == CLOUDY;
    }
}
