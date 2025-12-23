package com.vibe.orchestration.service;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.enums.SafetyMode;
import org.springframework.stereotype.Component;

/**
 * 安全模式过滤器
 * 根据安全模式对 AmbiencePlan 进行后置过滤
 */
@Component
public class SafetyModeFilter {

    /**
     * 应用安全模式过滤
     *
     * @param plan 原始氛围方案
     * @param mode 安全模式
     * @return 过滤后的氛围方案
     */
    public AmbiencePlan apply(AmbiencePlan plan, SafetyMode mode) {
        if (plan == null) {
            return null;
        }

        return switch (mode) {
            case L1_NORMAL -> plan;
            case L2_FOCUS -> applyFocusMode(plan);
            case L3_SILENT -> AmbiencePlan.silent();
        };
    }

    /**
     * L2 专注模式：禁用灯光动效
     */
    private AmbiencePlan applyFocusMode(AmbiencePlan plan) {
        return new AmbiencePlan(
            plan.id(),
            plan.music(),
            plan.light() != null ? plan.light().forFocusMode() : null,
            plan.narrative(),
            plan.safetyMode(),
            plan.reasoning(),
            plan.createdAt()
        );
    }
}
