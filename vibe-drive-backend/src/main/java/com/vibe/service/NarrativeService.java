package com.vibe.service;

import com.vibe.model.Narrative;
import com.vibe.model.enums.NarrativeEmotion;
import com.vibe.model.enums.SafetyMode;
import com.vibe.support.NarrativeTemplates;
import org.springframework.stereotype.Service;

/**
 * 叙事生成服务
 * 负责根据环境条件生成 TTS 播报文本
 */
@Service
public class NarrativeService {

    /**
     * 默认语音
     */
    private static final String DEFAULT_VOICE = "gentle_female";

    /**
     * 生成叙事文本
     *
     * @param timeOfDay   时段
     * @param weather     天气
     * @param gpsTag      位置标签
     * @param userMood    用户情绪
     * @param currentSong 当前歌曲（可选）
     * @param theme       叙事主题（可选）
     * @return 叙事结果
     */
    public Narrative generate(String timeOfDay, String weather, String gpsTag,
                              String userMood, String currentSong, String theme) {
        // 生成文本
        String text = NarrativeTemplates.generate(timeOfDay, weather, gpsTag, currentSong);

        // 如果有特定主题，可能需要调整文本（未来可扩展 LLM 生成）
        if (theme != null && !theme.isBlank()) {
            text = adjustTextForTheme(text, theme);
        }

        // 确定情感
        NarrativeEmotion emotion = mapEmotion(userMood, timeOfDay);

        // 确定语速和音量
        double speed = calculateSpeed(timeOfDay, userMood);
        double volume = calculateVolume(timeOfDay, userMood);

        return new Narrative(text, DEFAULT_VOICE, speed, volume, emotion);
    }

    /**
     * 生成叙事文本（带安全模式过滤）
     *
     * @param timeOfDay   时段
     * @param weather     天气
     * @param gpsTag      位置标签
     * @param userMood    用户情绪
     * @param currentSong 当前歌曲（可选）
     * @param theme       叙事主题（可选）
     * @param safetyMode  安全模式
     * @return 叙事结果
     */
    public Narrative generateWithSafety(String timeOfDay, String weather, String gpsTag,
                                        String userMood, String currentSong, String theme,
                                        SafetyMode safetyMode) {
        Narrative narrative = generate(timeOfDay, weather, gpsTag, userMood, currentSong, theme);
        return applySafetyFilter(narrative, safetyMode);
    }

    /**
     * 应用安全模式过滤
     *
     * @param narrative  原始叙事
     * @param safetyMode 安全模式
     * @return 过滤后的叙事
     */
    public Narrative applySafetyFilter(Narrative narrative, SafetyMode safetyMode) {
        if (narrative == null) {
            return null;
        }

        return switch (safetyMode) {
            case L3_SILENT -> narrative.withReducedVolume();  // 静默模式降低音量
            case L2_FOCUS, L1_NORMAL -> narrative;  // 其他模式不过滤
        };
    }

    /**
     * 根据主题调整文本
     */
    private String adjustTextForTheme(String text, String theme) {
        // 简单实现：未来可扩展为 LLM 生成
        return switch (theme) {
            case "comfort" -> text;  // 舒适主题保持原样
            case "energy" -> text.replace("慢慢", "").replace("轻柔", "活力");
            case "romance" -> text;  // 浪漫主题保持原样
            case "adventure" -> text.replace("安全", "精彩");
            default -> text;
        };
    }

    /**
     * 映射用户情绪到叙事情感
     */
    private NarrativeEmotion mapEmotion(String userMood, String timeOfDay) {
        // 优先根据用户情绪
        NarrativeEmotion emotion = switch (userMood) {
            case "happy", "excited" -> NarrativeEmotion.ENERGETIC;
            case "calm" -> NarrativeEmotion.CALM;
            case "tired" -> NarrativeEmotion.WARM;      // 疲劳时使用温暖情感
            case "stressed" -> NarrativeEmotion.WARM;   // 压力时使用温暖情感
            default -> NarrativeEmotion.CALM;           // 默认使用平静情感
        };

        // 深夜时段强制使用平静情感
        if ("midnight".equals(timeOfDay) || "night".equals(timeOfDay)) {
            if (emotion == NarrativeEmotion.ENERGETIC) {
                emotion = NarrativeEmotion.CALM;
            }
        }

        return emotion;
    }

    /**
     * 计算语速
     */
    private double calculateSpeed(String timeOfDay, String userMood) {
        // 深夜或疲劳时降低语速
        if ("midnight".equals(timeOfDay) || "night".equals(timeOfDay) || "tired".equals(userMood)) {
            return 0.85;
        }
        // 兴奋时稍微加快
        if ("excited".equals(userMood)) {
            return 1.1;
        }
        return 1.0;
    }

    /**
     * 计算音量
     */
    private double calculateVolume(String timeOfDay, String userMood) {
        // 深夜降低音量
        if ("midnight".equals(timeOfDay)) {
            return 0.6;
        }
        if ("night".equals(timeOfDay)) {
            return 0.7;
        }
        // 疲劳时降低音量
        if ("tired".equals(userMood)) {
            return 0.7;
        }
        return 0.8;
    }
}
