package com.vibe.model;

import com.vibe.model.enums.SafetyMode;
import dev.langchain4j.model.output.structured.Description;

import java.time.Instant;
import java.util.UUID;

/**
 * 氛围编排方案
 * 包含音乐、灯光和叙事的完整配置
 */
@Description("氛围编排方案，包含音乐、灯光和叙事的完整配置")
public record AmbiencePlan(
    @Description("方案唯一标识符")
    String id,

    @Description("推荐的音乐列表和相关元数据")
    MusicRecommendation music,

    @Description("氛围灯设置，L2专注模式下禁用动态效果，L3静默模式下为null")
    LightSetting light,

    @Description("TTS播报的叙事文本及语音参数")
    Narrative narrative,

    @Description("当前安全模式：L1_NORMAL（正常）/L2_FOCUS（专注）/L3_SILENT（静默）")
    SafetyMode safetyMode,

    @Description("Agent的推理过程，说明为何做出此氛围选择")
    String reasoning,

    @Description("方案创建时间")
    Instant createdAt
) {
    /**
     * 紧凑构造器：校验和默认值
     */
    public AmbiencePlan {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * 根据安全模式过滤输出
     * - L3_SILENT: 灯光设为 null，叙事音量降低 30%
     * - L2_FOCUS: 灯光设为 null（禁用视觉效果）
     * - L1_NORMAL: 返回原方案
     */
    public AmbiencePlan applyingSafetyFilter() {
        return switch (safetyMode) {
            case L3_SILENT -> new AmbiencePlan(
                id,
                music,
                null,  // 禁用灯光
                narrative != null ? narrative.withReducedVolume() : null,
                safetyMode,
                reasoning,
                createdAt
            );
            case L2_FOCUS -> new AmbiencePlan(
                id,
                music,
                null,  // 禁用灯光
                narrative,
                safetyMode,
                reasoning,
                createdAt
            );
            case L1_NORMAL -> this;
        };
    }

    /**
     * 判断是否有音乐推荐
     */
    public boolean hasMusic() {
        return music != null && !music.songs().isEmpty();
    }

    /**
     * 判断是否有灯光设置
     */
    public boolean hasLight() {
        return light != null;
    }

    /**
     * 判断是否有叙事文本
     */
    public boolean hasNarrative() {
        return narrative != null && narrative.text() != null && !narrative.text().isBlank();
    }

    /**
     * 判断是否为完整方案（包含所有三个组件）
     */
    public boolean isComplete() {
        return hasMusic() && hasLight() && hasNarrative();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * AmbiencePlan Builder
     */
    public static class Builder {
        private String id;
        private MusicRecommendation music;
        private LightSetting light;
        private Narrative narrative;
        private SafetyMode safetyMode = SafetyMode.L1_NORMAL;
        private String reasoning;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder music(MusicRecommendation music) {
            this.music = music;
            return this;
        }

        public Builder light(LightSetting light) {
            this.light = light;
            return this;
        }

        public Builder narrative(Narrative narrative) {
            this.narrative = narrative;
            return this;
        }

        public Builder safetyMode(SafetyMode safetyMode) {
            this.safetyMode = safetyMode;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AmbiencePlan build() {
            return new AmbiencePlan(id, music, light, narrative, safetyMode, reasoning, createdAt);
        }
    }
}
