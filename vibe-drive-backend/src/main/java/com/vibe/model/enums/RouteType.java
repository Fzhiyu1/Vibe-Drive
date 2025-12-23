package com.vibe.model.enums;

/**
 * 路线类型枚举
 * 补充 GpsTag，描述当前驾驶路线的整体特征
 */
public enum RouteType {
    HIGHWAY("高速路线"),
    URBAN("城市路线"),
    MOUNTAIN("山路"),
    COASTAL("海滨路线"),
    TUNNEL("隧道路线");

    private final String displayName;

    RouteType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
