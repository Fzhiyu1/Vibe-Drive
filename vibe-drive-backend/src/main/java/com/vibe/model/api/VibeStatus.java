package com.vibe.model.api;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import com.vibe.model.enums.SafetyMode;

import java.time.Instant;

/**
 * Vibe 状态响应
 */
public record VibeStatus(
    String sessionId,
    boolean agentRunning,
    SafetyMode currentSafetyMode,
    AmbiencePlan currentPlan,
    Environment lastEnvironment,
    Instant lastUpdateTime
) {
    /**
     * 创建初始状态
     */
    public static VibeStatus initial(String sessionId) {
        return new VibeStatus(
            sessionId,
            false,
            SafetyMode.L1_NORMAL,
            null,
            null,
            Instant.now()
        );
    }

    /**
     * 创建处理中状态
     */
    public static VibeStatus processing(String sessionId, SafetyMode safetyMode,
                                        AmbiencePlan plan, Environment environment) {
        return new VibeStatus(
            sessionId,
            true,
            safetyMode,
            plan,
            environment,
            Instant.now()
        );
    }

    /**
     * 创建已完成状态
     */
    public static VibeStatus completed(String sessionId, SafetyMode safetyMode,
                                       AmbiencePlan plan, Environment environment) {
        return new VibeStatus(
            sessionId,
            false,
            safetyMode,
            plan,
            environment,
            Instant.now()
        );
    }

    /**
     * 兼容旧命名：运行中状态（等价于 processing）
     */
    public static VibeStatus running(String sessionId, SafetyMode safetyMode,
                                     AmbiencePlan plan, Environment environment) {
        return processing(sessionId, safetyMode, plan, environment);
    }
}
