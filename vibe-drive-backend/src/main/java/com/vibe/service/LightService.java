package com.vibe.service;

import com.vibe.model.LightColor;
import com.vibe.model.LightSetting;
import com.vibe.model.ZoneSetting;
import com.vibe.model.enums.LightMode;
import com.vibe.model.enums.SafetyMode;
import com.vibe.support.LightPresets;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 灯光控制服务
 * 负责根据环境条件计算灯光设置，并应用安全模式过滤
 */
@Service
public class LightService {

    /**
     * 默认过渡时长（毫秒）
     */
    private static final int DEFAULT_TRANSITION_MS = 1500;

    /**
     * 计算灯光设置
     *
     * @param mood      目标情绪
     * @param timeOfDay 时段
     * @param weather   天气
     * @return 灯光设置
     */
    public LightSetting calculateSetting(String mood, String timeOfDay, String weather) {
        LightColor color = LightPresets.getColorForMood(mood);
        int brightness = LightPresets.getBrightnessForTime(timeOfDay);
        LightMode mode = LightPresets.getModeForWeather(weather);

        // 根据情绪微调亮度
        brightness = adjustBrightnessForMood(brightness, mood);

        return new LightSetting(
            color,
            brightness,
            mode,
            DEFAULT_TRANSITION_MS,
            createDefaultZones(color, brightness)
        );
    }

    /**
     * 计算灯光设置（带安全模式过滤）
     *
     * @param mood       目标情绪
     * @param timeOfDay  时段
     * @param weather    天气
     * @param safetyMode 安全模式
     * @return 灯光设置（可能为 null，表示禁用）
     */
    public LightSetting calculateSettingWithSafety(String mood, String timeOfDay, String weather, SafetyMode safetyMode) {
        LightSetting setting = calculateSetting(mood, timeOfDay, weather);
        return applySafetyFilter(setting, safetyMode);
    }

    /**
     * 应用安全模式过滤
     *
     * @param setting    原始灯光设置
     * @param safetyMode 安全模式
     * @return 过滤后的灯光设置
     */
    public LightSetting applySafetyFilter(LightSetting setting, SafetyMode safetyMode) {
        if (setting == null) {
            return null;
        }

        return switch (safetyMode) {
            case L3_SILENT -> null;  // 静默模式禁用灯光
            case L2_FOCUS -> setting.forFocusMode();  // 专注模式仅静态
            case L1_NORMAL -> setting;  // 正常模式不过滤
        };
    }

    /**
     * 根据情绪微调亮度
     */
    private int adjustBrightnessForMood(int baseBrightness, String mood) {
        return switch (mood) {
            case "tired", "stressed" -> Math.max(20, baseBrightness - 10);  // 降低亮度
            case "excited" -> Math.min(100, baseBrightness + 10);  // 提高亮度
            default -> baseBrightness;
        };
    }

    /**
     * 创建默认分区设置
     */
    private List<ZoneSetting> createDefaultZones(LightColor baseColor, int brightness) {
        return List.of(
            new ZoneSetting("dashboard", baseColor.hex(), brightness),
            new ZoneSetting("door", baseColor.hex(), Math.max(10, brightness - 10)),
            new ZoneSetting("footwell", baseColor.hex(), Math.max(10, brightness - 20))
        );
    }
}
