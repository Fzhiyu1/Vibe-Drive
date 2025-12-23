package com.vibe.model;

import com.vibe.model.enums.NarrativeEmotion;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 叙事文本
 * 用于 TTS 语音播报，将环境与音乐进行时空编织
 */
@Description("叙事文本，用于TTS语音播报，将环境与音乐进行时空编织")
public record Narrative(
    @NotBlank(message = "Narrative text cannot be empty")
    @Size(max = 500, message = "Narrative text too long, max 500 characters")
    @Description("播报文本内容，应简短温馨，不超过500字")
    String text,

    @Description("语音角色ID，默认为 default")
    String voice,

    @DecimalMin(value = "0.5", message = "Speed must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Speed must be at most 2.0")
    @Description("语速，范围 0.5-2.0，1.0为正常速度")
    double speed,

    @DecimalMin(value = "0.0", message = "Volume must be at least 0")
    @DecimalMax(value = "1.0", message = "Volume must be at most 1.0")
    @Description("音量，范围 0-1，L3静默模式下会自动降低30%")
    double volume,

    @Description("情感色彩：NEUTRAL（中性）/WARM（温暖）/ENERGETIC（活力）/CALM（平静）/GENTLE（轻柔）")
    NarrativeEmotion emotion
) {
    /**
     * 默认值常量
     */
    public static final String DEFAULT_VOICE = "default";
    public static final double DEFAULT_SPEED = 1.0;
    public static final double DEFAULT_VOLUME = 0.8;
    public static final double VOLUME_REDUCTION_FACTOR = 0.7;

    /**
     * 紧凑构造器：校验和默认值
     */
    public Narrative {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Narrative text cannot be empty");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Narrative text too long, max 500 characters");
        }
        if (voice == null || voice.isBlank()) {
            voice = DEFAULT_VOICE;
        }
        if (speed <= 0 || speed > 2.0) {
            speed = DEFAULT_SPEED;
        }
        if (volume <= 0 || volume > 1.0) {
            volume = DEFAULT_VOLUME;
        }
        if (emotion == null) {
            emotion = NarrativeEmotion.CALM;
        }
    }

    /**
     * 降低音量（用于 L3 静默模式）
     * 返回音量降低 30% 的新实例
     */
    public Narrative withReducedVolume() {
        return new Narrative(text, voice, speed, volume * VOLUME_REDUCTION_FACTOR, emotion);
    }

    /**
     * 使用指定情感创建新实例
     */
    public Narrative withEmotion(NarrativeEmotion newEmotion) {
        return new Narrative(text, voice, speed, volume, newEmotion);
    }

    /**
     * 使用指定语速创建新实例
     */
    public Narrative withSpeed(double newSpeed) {
        return new Narrative(text, voice, newSpeed, volume, emotion);
    }

    /**
     * 简化构造：仅提供文本
     */
    public static Narrative of(String text) {
        return new Narrative(text, DEFAULT_VOICE, DEFAULT_SPEED, DEFAULT_VOLUME, NarrativeEmotion.CALM);
    }

    /**
     * 简化构造：文本 + 情感
     */
    public static Narrative of(String text, NarrativeEmotion emotion) {
        return new Narrative(text, DEFAULT_VOICE, DEFAULT_SPEED, DEFAULT_VOLUME, emotion);
    }
}
