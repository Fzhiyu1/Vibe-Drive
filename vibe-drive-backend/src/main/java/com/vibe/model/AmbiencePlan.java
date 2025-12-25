package com.vibe.model;

import com.vibe.model.enums.SafetyMode;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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

    @Valid
    @Description("推荐的音乐列表和相关元数据（旧方法）")
    MusicRecommendation music,

    @Valid
    @Description("当前播放的音乐信息（新方法）")
    PlayResult playResult,

    @Valid
    @Description("氛围灯设置，L2专注模式下禁用动态效果，L3静默模式下为null")
    LightSetting light,

    @Valid
    @Description("TTS播报的叙事文本及语音参数")
    Narrative narrative,

    @Valid
    @Description("香氛设置，包含香氛类型、强度和持续时间")
    ScentSetting scent,

    @Valid
    @Description("按摩设置，包含按摩模式、区域和强度")
    MassageSetting massage,

    @NotNull(message = "Safety mode cannot be null")
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
     * - L2_FOCUS: 灯光强制静态模式（禁用动态效果）
     * - L1_NORMAL: 返回原方案
     */
    public AmbiencePlan applyingSafetyFilter() {
        return switch (safetyMode) {
            case L3_SILENT -> new AmbiencePlan(
                id,
                music,
                playResult,
                null,  // 禁用灯光
                narrative != null ? narrative.withReducedVolume() : null,
                scent != null ? scent.withIntensity(Math.min(scent.intensity(), 3)) : null,
                massage != null ? MassageSetting.off() : null,
                safetyMode,
                reasoning,
                createdAt
            );
            case L2_FOCUS -> new AmbiencePlan(
                id,
                music,
                playResult,
                light != null ? light.forFocusMode() : null,  // 禁用动态效果
                narrative,
                scent,
                massage != null ? massage.forHighSpeed() : null,
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
     * 创建静默模式方案（L3_SILENT 时使用）
     */
    public static AmbiencePlan silent() {
        return new AmbiencePlan(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SafetyMode.L3_SILENT,
            "高速行驶中，静默模式，不主动推荐",
            null
        );
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
        private PlayResult playResult;
        private LightSetting light;
        private Narrative narrative;
        private ScentSetting scent;
        private MassageSetting massage;
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

        public Builder playResult(PlayResult playResult) {
            this.playResult = playResult;
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

        public Builder scent(ScentSetting scent) {
            this.scent = scent;
            return this;
        }

        public Builder massage(MassageSetting massage) {
            this.massage = massage;
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
            return new AmbiencePlan(id, music, playResult, light, narrative, scent, massage, safetyMode, reasoning, createdAt);
        }
    }
}
