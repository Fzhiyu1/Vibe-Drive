package com.vibe.orchestration.dto;

import com.vibe.model.enums.SafetyMode;

import java.time.Instant;
import java.util.UUID;

/**
 * 循环状态（不可变）
 * 跟踪递归编排的状态信息
 */
public record VibeLoopState(
    String turnId,
    String sessionId,
    int depth,
    int toolCallCount,
    Instant startTime,
    SafetyMode safetyMode
) {
    /**
     * 创建新轮次状态
     */
    public static VibeLoopState newTurn(String sessionId, SafetyMode safetyMode) {
        return new VibeLoopState(
            UUID.randomUUID().toString(),
            sessionId,
            0,
            0,
            Instant.now(),
            safetyMode
        );
    }

    /**
     * 增加递归深度（返回新实例）
     */
    public VibeLoopState incrementDepth() {
        return new VibeLoopState(
            turnId,
            sessionId,
            depth + 1,
            toolCallCount,
            startTime,
            safetyMode
        );
    }

    /**
     * 增加工具调用计数（返回新实例）
     */
    public VibeLoopState incrementToolCallCount() {
        return new VibeLoopState(
            turnId,
            sessionId,
            depth,
            toolCallCount + 1,
            startTime,
            safetyMode
        );
    }

    /**
     * 获取已运行时间（毫秒）
     */
    public long getElapsedMillis() {
        return Instant.now().toEpochMilli() - startTime.toEpochMilli();
    }
}
