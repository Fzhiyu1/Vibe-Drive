package com.vibe.model.enums;

/**
 * 地理标签枚举
 * 表示车辆当前所处的地理位置类型
 */
public enum GpsTag {
    HIGHWAY("高速公路"),
    TUNNEL("隧道"),
    BRIDGE("桥梁"),
    URBAN("城区"),
    SUBURBAN("郊区"),
    MOUNTAIN("山区"),
    COASTAL("海滨"),
    PARKING("停车场");

    private final String displayName;

    GpsTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
