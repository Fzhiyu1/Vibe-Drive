package com.vibe.orchestration.dto;

import java.time.Instant;

/**
 * 工具执行记录
 */
public record ToolExecution(
    String toolName,
    Instant startTime,
    Instant endTime,    // null 表示正在执行
    boolean success
) {
    /**
     * 创建开始执行记录
     */
    public static ToolExecution started(String toolName) {
        return new ToolExecution(toolName, Instant.now(), null, false);
    }

    /**
     * 标记为完成
     */
    public ToolExecution completed(boolean success) {
        return new ToolExecution(toolName, startTime, Instant.now(), success);
    }

    /**
     * 是否正在执行
     */
    public boolean isRunning() {
        return endTime == null;
    }
}
