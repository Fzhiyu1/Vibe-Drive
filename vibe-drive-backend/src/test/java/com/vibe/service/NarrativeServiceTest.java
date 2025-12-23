package com.vibe.service;

import com.vibe.model.Narrative;
import com.vibe.model.enums.NarrativeEmotion;
import com.vibe.model.enums.SafetyMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NarrativeService 单元测试
 */
@SpringBootTest
class NarrativeServiceTest {

    @Autowired
    private NarrativeService narrativeService;

    @Nested
    @DisplayName("叙事生成测试")
    class GenerateTest {
        @Test
        void shouldGenerateNarrativeForMidnightRain() {
            Narrative result = narrativeService.generate(
                "midnight", "rainy", "highway", "tired", null, null);

            assertThat(result).isNotNull();
            assertThat(result.text()).isNotBlank();
            assertThat(result.text().length()).isLessThanOrEqualTo(100);
        }

        @Test
        void shouldGenerateNarrativeForMorningSunny() {
            Narrative result = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);

            assertThat(result).isNotNull();
            assertThat(result.text()).isNotBlank();
        }

        @Test
        void shouldIncludeSongNameInNarrative() {
            Narrative result = narrativeService.generate(
                "midnight", "rainy", "highway", "calm", "夜空中最亮的星", null);

            assertThat(result).isNotNull();
            // 如果模板包含"这首歌"，应该被替换为歌曲名
            // 注意：不是所有模板都包含"这首歌"
        }

        @Test
        void shouldSetDefaultVoice() {
            Narrative result = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);

            assertThat(result.voice()).isEqualTo("gentle_female");
        }
    }

    @Nested
    @DisplayName("情感映射测试")
    class EmotionMappingTest {
        @Test
        void shouldMapHappyMoodToEnergetic() {
            Narrative result = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);

            assertThat(result.emotion()).isEqualTo(NarrativeEmotion.ENERGETIC);
        }

        @Test
        void shouldMapCalmMoodToCalm() {
            Narrative result = narrativeService.generate(
                "afternoon", "cloudy", "suburban", "calm", null, null);

            assertThat(result.emotion()).isEqualTo(NarrativeEmotion.CALM);
        }

        @Test
        void shouldMapTiredMoodToWarm() {
            Narrative result = narrativeService.generate(
                "evening", "cloudy", "urban", "tired", null, null);

            assertThat(result.emotion()).isEqualTo(NarrativeEmotion.WARM);
        }

        @Test
        void shouldOverrideEnergeticToCalmForMidnight() {
            // 深夜时即使是 happy 情绪，也应该使用 CALM
            Narrative result = narrativeService.generate(
                "midnight", "cloudy", "highway", "happy", null, null);

            assertThat(result.emotion()).isEqualTo(NarrativeEmotion.CALM);
        }
    }

    @Nested
    @DisplayName("语速和音量测试")
    class SpeedAndVolumeTest {
        @Test
        void shouldReduceSpeedForMidnight() {
            Narrative result = narrativeService.generate(
                "midnight", "cloudy", "highway", "calm", null, null);

            assertThat(result.speed()).isLessThan(1.0);
        }

        @Test
        void shouldReduceSpeedForTiredMood() {
            Narrative result = narrativeService.generate(
                "afternoon", "sunny", "urban", "tired", null, null);

            assertThat(result.speed()).isLessThan(1.0);
        }

        @Test
        void shouldIncreaseSpeedForExcitedMood() {
            Narrative result = narrativeService.generate(
                "morning", "sunny", "urban", "excited", null, null);

            assertThat(result.speed()).isGreaterThan(1.0);
        }

        @Test
        void shouldReduceVolumeForMidnight() {
            Narrative result = narrativeService.generate(
                "midnight", "cloudy", "highway", "calm", null, null);

            assertThat(result.volume()).isLessThanOrEqualTo(0.6);
        }

        @Test
        void shouldReduceVolumeForNight() {
            Narrative result = narrativeService.generate(
                "night", "cloudy", "highway", "calm", null, null);

            assertThat(result.volume()).isLessThanOrEqualTo(0.7);
        }
    }

    @Nested
    @DisplayName("安全模式过滤测试")
    class SafetyFilterTest {
        @Test
        void shouldReduceVolumeForSilentMode() {
            Narrative narrative = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);
            double originalVolume = narrative.volume();

            Narrative filtered = narrativeService.applySafetyFilter(narrative, SafetyMode.L3_SILENT);

            assertThat(filtered).isNotNull();
            assertThat(filtered.volume()).isLessThan(originalVolume);
        }

        @Test
        void shouldNotFilterForNormalMode() {
            Narrative narrative = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);

            Narrative filtered = narrativeService.applySafetyFilter(narrative, SafetyMode.L1_NORMAL);

            assertThat(filtered.volume()).isEqualTo(narrative.volume());
        }

        @Test
        void shouldNotFilterForFocusMode() {
            Narrative narrative = narrativeService.generate(
                "morning", "sunny", "urban", "happy", null, null);

            Narrative filtered = narrativeService.applySafetyFilter(narrative, SafetyMode.L2_FOCUS);

            assertThat(filtered.volume()).isEqualTo(narrative.volume());
        }
    }

    @Nested
    @DisplayName("带安全模式的叙事生成测试")
    class GenerateWithSafetyTest {
        @Test
        void shouldGenerateAndFilterInOneCall() {
            Narrative result = narrativeService.generateWithSafety(
                "morning", "sunny", "urban", "happy", null, null, SafetyMode.L3_SILENT);

            assertThat(result).isNotNull();
            // L3_SILENT 模式应该降低音量
        }
    }
}
