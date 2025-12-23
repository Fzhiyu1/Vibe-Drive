package com.vibe.orchestration.dto;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.ToolExecutionInfo;

import java.util.List;

/**
 * 对话结果
 */
public record VibeDialogResult(
    boolean success,
    AmbiencePlan plan,
    VibeLoopState loopState,
    String errorMessage,
    List<ToolExecutionInfo> toolExecutions
) {
    public VibeDialogResult {
        if (toolExecutions != null) {
            toolExecutions = List.copyOf(toolExecutions);
        } else {
            toolExecutions = List.of();
        }
    }

    /**
     * 创建成功结果
     */
    public static VibeDialogResult success(AmbiencePlan plan, VibeLoopState state, List<ToolExecutionInfo> toolExecutions) {
        return new VibeDialogResult(true, plan, state, null, toolExecutions);
    }

    /**
     * 创建错误结果
     */
    public static VibeDialogResult error(String message, VibeLoopState state, List<ToolExecutionInfo> toolExecutions) {
        return new VibeDialogResult(false, null, state, message, toolExecutions);
    }

    /**
     * 创建静默模式结果
     */
    public static VibeDialogResult silent(VibeLoopState state) {
        return new VibeDialogResult(true, AmbiencePlan.silent(), state, null, List.of());
    }
}
