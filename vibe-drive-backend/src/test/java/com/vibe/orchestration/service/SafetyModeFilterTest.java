package com.vibe.orchestration.service;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.LightSetting;
import com.vibe.model.enums.LightMode;
import com.vibe.model.enums.SafetyMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SafetyModeFilter 单元测试
 */
@DisplayName("SafetyModeFilter 测试")
class SafetyModeFilterTest {

    private SafetyModeFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SafetyModeFilter();
    }

    @Test
    @DisplayName("L1 正常模式：返回原方案")
    void shouldReturnOriginalPlanForL1Normal() {
        AmbiencePlan plan = createTestPlan(SafetyMode.L1_NORMAL);

        AmbiencePlan result = filter.apply(plan, SafetyMode.L1_NORMAL);

        assertThat(result).isSameAs(plan);
    }

    @Test
    @DisplayName("L2 专注模式：灯光切换为静态模式")
    void shouldApplyFocusModeForL2() {
        LightSetting light = LightSetting.breathing(null, 50);
        AmbiencePlan plan = AmbiencePlan.builder()
                .safetyMode(SafetyMode.L2_FOCUS)
                .light(light)
                .build();

        AmbiencePlan result = filter.apply(plan, SafetyMode.L2_FOCUS);

        assertThat(result.light()).isNotNull();
        assertThat(result.light().mode()).isEqualTo(LightMode.STATIC);
    }

    @Test
    @DisplayName("L3 静默模式：返回静默方案")
    void shouldReturnSilentPlanForL3() {
        AmbiencePlan plan = createTestPlan(SafetyMode.L3_SILENT);

        AmbiencePlan result = filter.apply(plan, SafetyMode.L3_SILENT);

        assertThat(result.safetyMode()).isEqualTo(SafetyMode.L3_SILENT);
        assertThat(result.music()).isNull();
        assertThat(result.light()).isNull();
    }

    @Test
    @DisplayName("null 方案返回 null")
    void shouldReturnNullForNullPlan() {
        AmbiencePlan result = filter.apply(null, SafetyMode.L1_NORMAL);

        assertThat(result).isNull();
    }

    private AmbiencePlan createTestPlan(SafetyMode mode) {
        return AmbiencePlan.builder()
                .safetyMode(mode)
                .reasoning("Test plan")
                .build();
    }
}
