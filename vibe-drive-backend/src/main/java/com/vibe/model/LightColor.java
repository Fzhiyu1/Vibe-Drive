package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;

/**
 * 灯光颜色配置
 * 支持十六进制颜色和色温两种表示方式
 */
@Description("灯光颜色配置")
public record LightColor(
    @Description("十六进制颜色代码，格式 #RRGGBB")
    String hex,

    @Description("色温，单位开尔文（K），范围 2700-6500")
    Integer temperature
) {
    /**
     * 紧凑构造器：校验参数
     */
    public LightColor {
        if (hex != null && !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color format, expected #RRGGBB");
        }
        if (temperature != null && (temperature < 2700 || temperature > 6500)) {
            throw new IllegalArgumentException("Temperature must be between 2700 and 6500 K");
        }
    }

    /**
     * 暖白色（适合放松场景）
     */
    public static LightColor warmWhite() {
        return new LightColor("#FFE4B5", 2700);
    }

    /**
     * 冷白色（适合专注场景）
     */
    public static LightColor coolWhite() {
        return new LightColor("#F0F8FF", 6500);
    }

    /**
     * 琥珀色（适合深夜场景）
     */
    public static LightColor amber() {
        return new LightColor("#FFBF00", 2700);
    }

    /**
     * 海蓝色（适合海滨场景）
     */
    public static LightColor oceanBlue() {
        return new LightColor("#006994", 5000);
    }

    /**
     * 日落橙（适合傍晚场景）
     */
    public static LightColor sunsetOrange() {
        return new LightColor("#FF4500", 3000);
    }

    /**
     * 仅使用十六进制颜色创建
     */
    public static LightColor fromHex(String hex) {
        return new LightColor(hex, null);
    }

    /**
     * 仅使用色温创建
     */
    public static LightColor fromTemperature(int temperature) {
        return new LightColor(null, temperature);
    }
}
