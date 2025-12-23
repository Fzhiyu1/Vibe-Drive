package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 天气状况枚举
 * 影响灯光氛围和音乐选择
 */
public enum Weather {
    SUNNY("sunny", "晴天"),
    CLOUDY("cloudy", "多云"),
    RAINY("rainy", "雨天"),
    SNOWY("snowy", "雪天"),
    FOGGY("foggy", "雾天");

    private final String value;
    private final String displayName;

    Weather(String value, String displayName) {
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
    public static Weather fromValue(String value) {
        for (Weather weather : values()) {
            if (weather.value.equalsIgnoreCase(value)) {
                return weather;
            }
        }
        throw new IllegalArgumentException("Unknown Weather: " + value);
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
