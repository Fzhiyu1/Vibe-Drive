package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 路线类型枚举
 * 补充 GpsTag，描述当前驾驶路线的整体特征
 */
public enum RouteType {
    HIGHWAY("highway", "高速路线"),
    URBAN("urban", "城市路线"),
    MOUNTAIN("mountain", "山路"),
    COASTAL("coastal", "海滨路线"),
    TUNNEL("tunnel", "隧道路线");

    private final String value;
    private final String displayName;

    RouteType(String value, String displayName) {
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
    public static RouteType fromValue(String value) {
        for (RouteType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RouteType: " + value);
    }

    /**
     * 判断是否为风景优美路线（适合叙事）
     */
    public boolean isScenicRoute() {
        return this == MOUNTAIN || this == COASTAL;
    }

    /**
     * 判断是否为高速路线（安全优先）
     */
    public boolean isHighSpeedRoute() {
        return this == HIGHWAY;
    }
}
