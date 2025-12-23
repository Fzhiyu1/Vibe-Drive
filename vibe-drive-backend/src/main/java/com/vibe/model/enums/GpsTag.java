package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 地理标签枚举
 * 表示车辆当前所处的地理位置类型
 */
public enum GpsTag {
    HIGHWAY("highway", "高速公路"),
    TUNNEL("tunnel", "隧道"),
    BRIDGE("bridge", "桥梁"),
    URBAN("urban", "城区"),
    SUBURBAN("suburban", "郊区"),
    MOUNTAIN("mountain", "山区"),
    COASTAL("coastal", "海滨"),
    PARKING("parking", "停车场");

    private final String value;
    private final String displayName;

    GpsTag(String value, String displayName) {
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
    public static GpsTag fromValue(String value) {
        for (GpsTag tag : values()) {
            if (tag.value.equalsIgnoreCase(value)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Unknown GpsTag: " + value);
    }

    /**
     * 判断是否为高速场景（需要更高的安全约束）
     */
    public boolean isHighSpeedScenario() {
        return this == HIGHWAY || this == BRIDGE;
    }

    /**
     * 判断是否为封闭环境（影响叙事内容）
     */
    public boolean isEnclosedEnvironment() {
        return this == TUNNEL || this == PARKING;
    }
}
