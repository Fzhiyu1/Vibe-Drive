package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;

/**
 * 分区灯光设置
 * 用于车内不同区域的独立灯光控制
 */
@Description("分区灯光设置，用于车内不同区域的独立灯光控制")
public record ZoneSetting(
    @Description("区域名称，如 dashboard/door/roof/footwell")
    String zone,

    @Description("该区域的颜色，十六进制格式 #RRGGBB")
    String color,

    @Description("该区域的亮度，范围 0-100")
    int brightness
) {
    /**
     * 紧凑构造器：校验参数
     */
    public ZoneSetting {
        if (zone == null || zone.isBlank()) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Invalid color format, expected #RRGGBB");
        }
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        }
    }

    /**
     * 常用区域名称常量
     */
    public static final String ZONE_DASHBOARD = "dashboard";
    public static final String ZONE_DOOR = "door";
    public static final String ZONE_ROOF = "roof";
    public static final String ZONE_FOOTWELL = "footwell";
    public static final String ZONE_CENTER_CONSOLE = "center_console";

    /**
     * 创建仪表盘区域设置
     */
    public static ZoneSetting dashboard(String color, int brightness) {
        return new ZoneSetting(ZONE_DASHBOARD, color, brightness);
    }

    /**
     * 创建车门区域设置
     */
    public static ZoneSetting door(String color, int brightness) {
        return new ZoneSetting(ZONE_DOOR, color, brightness);
    }

    /**
     * 创建车顶区域设置
     */
    public static ZoneSetting roof(String color, int brightness) {
        return new ZoneSetting(ZONE_ROOF, color, brightness);
    }
}
