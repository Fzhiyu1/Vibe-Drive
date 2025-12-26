package com.vibe.tool;

import com.vibe.model.Narrative;
import com.vibe.model.enums.NarrativeEmotion;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 叙事工具
 * 接收 AI 生成的叙事文本，包装成 TTS 播报对象
 */
@Component
public class NarrativeTool {

    private static final String DEFAULT_VOICE = "gentle_female";

    @Tool("""
        设置 TTS 播报文本。由你根据当前环境和歌曲创作叙事文本。

        创作要求：
        1. 文本简短温馨，15-30字为佳，不超过50字
        2. 结合窗外风景（天气、时段、位置）与音乐意境
        3. 可以引用歌词意境，但不要直接复制歌词
        4. 语气符合氛围：深夜轻柔、早晨清新、雨天温馨
        5. 避免说教，像朋友轻声细语

        示例：
        - "夜色温柔，《晴天》的旋律陪你穿过这片星空"
        - "雨滴敲窗，就让这首歌温暖归途"
        - "晨光正好，新的一天从这首歌开始"
        """)
    public Narrative generateNarrative(
        @P("叙事文本，由你创作") String text,
        @P("情感基调: calm(平静)/warm(温暖)/energetic(活力)") String emotion
    ) {
        NarrativeEmotion narrativeEmotion = parseEmotion(emotion);
        double speed = calculateSpeed(narrativeEmotion);
        double volume = calculateVolume(narrativeEmotion);

        return new Narrative(text, DEFAULT_VOICE, speed, volume, narrativeEmotion);
    }

    private NarrativeEmotion parseEmotion(String emotion) {
        if (emotion == null) return NarrativeEmotion.CALM;
        return switch (emotion.toLowerCase()) {
            case "warm" -> NarrativeEmotion.WARM;
            case "energetic" -> NarrativeEmotion.ENERGETIC;
            default -> NarrativeEmotion.CALM;
        };
    }

    private double calculateSpeed(NarrativeEmotion emotion) {
        return switch (emotion) {
            case CALM -> 0.9;
            case WARM, ROMANTIC -> 0.95;
            case ENERGETIC, ADVENTUROUS -> 1.05;
        };
    }

    private double calculateVolume(NarrativeEmotion emotion) {
        return switch (emotion) {
            case CALM -> 0.7;
            case WARM, ROMANTIC -> 0.75;
            case ENERGETIC, ADVENTUROUS -> 0.8;
        };
    }
}
