package com.vibe.orchestration.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vibe 任务状态（供主智能体查询）
 */
public record VibeTaskStatus(
    boolean running,
    String taskId,
    Instant startTime,
    List<ToolExecution> toolHistory,
    String currentTool
) {
    /**
     * 获取已完成的工具数量
     */
    public long completedToolCount() {
        return toolHistory.stream()
            .filter(t -> !t.isRunning())
            .count();
    }

    /**
     * 获取成功的工具数量
     */
    public long successToolCount() {
        return toolHistory.stream()
            .filter(t -> !t.isRunning() && t.success())
            .count();
    }
}
