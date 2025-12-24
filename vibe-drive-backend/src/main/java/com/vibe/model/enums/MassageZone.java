package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 按摩区域枚举
 * 定义座椅按摩系统支持的按摩区域
 */
public enum MassageZone {
    BACK("back", "背部"),
    LUMBAR("lumbar", "腰部"),
    SHOULDER("shoulder", "肩部"),
    THIGH("thigh", "大腿"),
    ALL("all", "全部");

    private final String value;
    private final String displayName;

    MassageZone(String value, String displayName) {
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
    public static MassageZone fromValue(String value) {
        for (MassageZone zone : values()) {
            if (zone.value.equalsIgnoreCase(value)) {
                return zone;
            }
        }
        throw new IllegalArgumentException("Unknown MassageZone: " + value);
    }
}
