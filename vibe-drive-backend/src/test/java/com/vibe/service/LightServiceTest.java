package com.vibe.service;

import com.vibe.model.LightSetting;
import com.vibe.model.enums.LightMode;
import com.vibe.model.enums.SafetyMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LightService 单元测试
 */
@SpringBootTest
class LightServiceTest {

    @Autowired
    private LightService lightService;

    @Nested
    @DisplayName("灯光设置计算测试")
    class CalculateSettingTest {
        @Test
        void shouldCalculateSettingForHappyMood() {
            LightSetting result = lightService.calculateSetting("happy", "morning", "sunny");

            assertThat(result).isNotNull();
            assertThat(result.color()).isNotNull();
            assertThat(result.brightness()).isBetween(0, 100);
            assertThat(result.mode()).isEqualTo(LightMode.STATIC);
        }

        @Test
        void shouldCalculateSettingForTiredMood() {
            LightSetting result = lightService.calculateSetting("tired", "night", "cloudy");

            assertThat(result).isNotNull();
            // 疲劳时亮度应该较低
            assertThat(result.brightness()).isLessThanOrEqualTo(30);
        }

        @Test
        void shouldUseBreathingModeForRainyWeather() {
            LightSetting result = lightService.calculateSetting("calm", "afternoon", "rainy");

            assertThat(result).isNotNull();
            assertThat(result.mode()).isEqualTo(LightMode.BREATHING);
        }

        @Test
        void shouldUseGradientModeForSnowyWeather() {
            LightSetting result = lightService.calculateSetting("calm", "afternoon", "snowy");

            assertThat(result).isNotNull();
            assertThat(result.mode()).isEqualTo(LightMode.GRADIENT);
        }

        @Test
        void shouldAdjustBrightnessForTimeOfDay() {
            LightSetting morning = lightService.calculateSetting("calm", "morning", "sunny");
            LightSetting midnight = lightService.calculateSetting("calm", "midnight", "sunny");

            // 早晨亮度应该比深夜高
            assertThat(morning.brightness()).isGreaterThan(midnight.brightness());
        }

        @Test
        void shouldCreateDefaultZones() {
            LightSetting result = lightService.calculateSetting("happy", "noon", "sunny");

            assertThat(result.hasZones()).isTrue();
            assertThat(result.zones()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("安全模式过滤测试")
    class SafetyFilterTest {
        @Test
        void shouldReturnNullForSilentMode() {
            LightSetting setting = lightService.calculateSetting("happy", "morning", "sunny");
            LightSetting filtered = lightService.applySafetyFilter(setting, SafetyMode.L3_SILENT);

            assertThat(filtered).isNull();
        }

        @Test
        void shouldDisableDynamicEffectsForFocusMode() {
            LightSetting setting = lightService.calculateSetting("calm", "afternoon", "rainy");
            // 雨天默认是 BREATHING 模式
            assertThat(setting.mode()).isEqualTo(LightMode.BREATHING);

            LightSetting filtered = lightService.applySafetyFilter(setting, SafetyMode.L2_FOCUS);

            assertThat(filtered).isNotNull();
            assertThat(filtered.mode()).isEqualTo(LightMode.STATIC);
        }

        @Test
        void shouldNotFilterForNormalMode() {
            LightSetting setting = lightService.calculateSetting("calm", "afternoon", "rainy");
            LightSetting filtered = lightService.applySafetyFilter(setting, SafetyMode.L1_NORMAL);

            assertThat(filtered).isNotNull();
            assertThat(filtered.mode()).isEqualTo(setting.mode());
        }

        @Test
        void shouldHandleNullSetting() {
            LightSetting filtered = lightService.applySafetyFilter(null, SafetyMode.L1_NORMAL);

            assertThat(filtered).isNull();
        }
    }

    @Nested
    @DisplayName("带安全模式的灯光设置测试")
    class CalculateWithSafetyTest {
        @Test
        void shouldCalculateAndFilterInOneCall() {
            LightSetting result = lightService.calculateSettingWithSafety(
                "calm", "afternoon", "rainy", SafetyMode.L2_FOCUS);

            assertThat(result).isNotNull();
            assertThat(result.mode()).isEqualTo(LightMode.STATIC);
        }
    }
}
