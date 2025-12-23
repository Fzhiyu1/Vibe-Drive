package com.vibe.support;

import com.vibe.model.LightColor;
import com.vibe.model.enums.LightMode;

import java.util.Map;

/**
 * 灯光预设配置
 * 定义情绪、时段、天气与灯光设置的映射关系
 */
public final class LightPresets {

    private LightPresets() {
        // 工具类，禁止实例化
    }

    /**
     * 情绪 → 颜色映射
     */
    public static final Map<String, LightColor> MOOD_COLORS = Map.of(
        "happy", new LightColor("#FFD700", 4000),      // 金色，活力
        "calm", new LightColor("#87CEEB", 5000),       // 天蓝，平静
        "tired", new LightColor("#FFE4B5", 2700),      // 暖白，舒适
        "stressed", new LightColor("#98FB98", 4500),   // 淡绿，放松
        "excited", new LightColor("#FF69B4", 4000)     // 粉色，活力
    );

    /**
     * 时段 → 亮度映射 (0-100)
     */
    public static final Map<String, Integer> TIME_BRIGHTNESS = Map.of(
        "dawn", 40,
        "morning", 60,
        "noon", 70,
        "afternoon", 60,
        "evening", 50,
        "night", 30,
        "midnight", 20
    );

    /**
     * 天气 → 灯光模式映射
     */
    public static final Map<String, LightMode> WEATHER_MODE = Map.of(
        "sunny", LightMode.STATIC,
        "cloudy", LightMode.STATIC,
        "rainy", LightMode.BREATHING,
        "snowy", LightMode.GRADIENT,
        "foggy", LightMode.STATIC
    );

    /**
     * 获取情绪对应的颜色，如果不存在则返回默认暖白色
     */
    public static LightColor getColorForMood(String mood) {
        return MOOD_COLORS.getOrDefault(mood, LightColor.warmWhite());
    }

    /**
     * 获取时段对应的亮度，如果不存在则返回默认50%
     */
    public static int getBrightnessForTime(String timeOfDay) {
        return TIME_BRIGHTNESS.getOrDefault(timeOfDay, 50);
    }

    /**
     * 获取天气对应的灯光模式，如果不存在则返回静态模式
     */
    public static LightMode getModeForWeather(String weather) {
        return WEATHER_MODE.getOrDefault(weather, LightMode.STATIC);
    }
}
