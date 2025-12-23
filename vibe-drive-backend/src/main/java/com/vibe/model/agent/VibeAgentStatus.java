package com.vibe.model.agent;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import com.vibe.model.enums.SafetyMode;

import java.time.Duration;
import java.time.Instant;

/**
 * Vibe Agent 状态
 * 包含 Agent 的运行状态和当前氛围信息
 */
public record VibeAgentStatus(
    boolean running,
    SafetyMode currentSafetyMode,
    AmbiencePlan currentPlan,
    Environment lastEnvironment,
    Instant lastUpdateTime,
    int totalPlansGenerated,
    Instant startTime
) {
    /**
     * 判断 Agent 是否空闲（运行中但无当前方案）
     */
    public boolean isIdle() {
        return running && currentPlan == null;
    }

    /**
     * 判断 Agent 是否正在处理
     */
    public boolean isProcessing() {
        return running && currentPlan != null;
    }

    /**
     * 获取运行时长
     */
    public Duration getUptime() {
        if (!running || startTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, Instant.now());
    }

    /**
     * 获取格式化的运行时长
     */
    public String getUptimeFormatted() {
        Duration uptime = getUptime();
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", minutes);
    }

    /**
     * 创建初始状态（未运行）
     */
    public static VibeAgentStatus initial() {
        return new VibeAgentStatus(
            false,
            SafetyMode.L1_NORMAL,
            null,
            null,
            null,
            0,
            null
        );
    }

    /**
     * 创建运行中状态
     */
    public static VibeAgentStatus running(SafetyMode safetyMode) {
        return new VibeAgentStatus(
            true,
            safetyMode,
            null,
            null,
            Instant.now(),
            0,
            Instant.now()
        );
    }

    /**
     * 更新当前方案
     */
    public VibeAgentStatus withPlan(AmbiencePlan plan) {
        return new VibeAgentStatus(
            running,
            currentSafetyMode,
            plan,
            lastEnvironment,
            Instant.now(),
            totalPlansGenerated + 1,
            startTime
        );
    }

    /**
     * 更新环境数据
     */
    public VibeAgentStatus withEnvironment(Environment environment) {
        return new VibeAgentStatus(
            running,
            environment.getSafetyMode(),
            currentPlan,
            environment,
            Instant.now(),
            totalPlansGenerated,
            startTime
        );
    }

    /**
     * 停止 Agent
     */
    public VibeAgentStatus stopped() {
        return new VibeAgentStatus(
            false,
            currentSafetyMode,
            null,
            lastEnvironment,
            Instant.now(),
            totalPlansGenerated,
            null
        );
    }
}
