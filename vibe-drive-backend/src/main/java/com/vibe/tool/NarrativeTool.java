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
        设置 TTS 播报文本。根据当前环境创作叙事文本。

        创作流程：
        1. 先在心中构思3个不同风格的候选文案
        2. 根据下方标准评估，选出最佳的一个
        3. 只输出最终选中的文案

        风格标准：
        - 简短：10-20字，不啰嗦
        - 口语化：像朋友说话
        - 不说教：不提醒、不建议、不评价状态
        - 认同而非安慰：接纳当下
        - 转换视角：把负面变成另一种体验
        - 诗意对比：善用比喻，不直白
        - 用氛围呼应状态，而非直说"你很累"

        禁止：
        - 提及具体歌名
        - 描述设备功能（如"香氛提神"）
        - 超过25字
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
