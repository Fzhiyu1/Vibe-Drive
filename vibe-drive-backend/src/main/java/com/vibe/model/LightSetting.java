package com.vibe.model;

import com.vibe.model.enums.LightMode;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

/**
 * 氛围灯设置
 * 包含颜色、亮度和动态效果
 */
@Description("氛围灯设置，包含颜色、亮度和动态效果")
public record LightSetting(
    @Description("灯光颜色和色温")
    LightColor color,

    @Description("亮度，范围 0-100")
    int brightness,

    @Description("灯光模式：STATIC（静态）/BREATHING（呼吸）/GRADIENT（渐变）/PULSE（脉冲）")
    LightMode mode,

    @Description("颜色过渡时长，单位毫秒")
    int transitionDuration,

    @Description("分区设置，可选，用于多区域灯光控制")
    List<ZoneSetting> zones
) {
    /**
     * 默认过渡时长（毫秒）
     */
    public static final int DEFAULT_TRANSITION_DURATION = 1000;

    /**
     * 紧凑构造器：校验和默认值
     */
    public LightSetting {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        }
        if (transitionDuration <= 0) {
            transitionDuration = DEFAULT_TRANSITION_DURATION;
        }
        if (zones != null) {
            zones = List.copyOf(zones);
        }
    }

    /**
     * 判断是否为动态效果
     */
    public boolean isDynamic() {
        return mode != null && mode.isDynamic();
    }

    /**
     * 判断是否有分区设置
     */
    public boolean hasZones() {
        return zones != null && !zones.isEmpty();
    }

    /**
     * 创建静态灯光设置
     */
    public static LightSetting staticLight(LightColor color, int brightness) {
        return new LightSetting(color, brightness, LightMode.STATIC, DEFAULT_TRANSITION_DURATION, null);
    }

    /**
     * 创建呼吸灯效果
     */
    public static LightSetting breathing(LightColor color, int brightness) {
        return new LightSetting(color, brightness, LightMode.BREATHING, 2000, null);
    }

    /**
     * 创建渐变效果
     */
    public static LightSetting gradient(LightColor color, int brightness) {
        return new LightSetting(color, brightness, LightMode.GRADIENT, 3000, null);
    }

    /**
     * 为 L2 专注模式过滤（禁用动态效果）
     */
    public LightSetting forFocusMode() {
        if (isDynamic()) {
            return new LightSetting(color, brightness, LightMode.STATIC, transitionDuration, zones);
        }
        return this;
    }

    /**
     * 调整亮度
     */
    public LightSetting withBrightness(int newBrightness) {
        return new LightSetting(color, newBrightness, mode, transitionDuration, zones);
    }
}
