package com.vibe.model.api;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.TokenUsageInfo;
import com.vibe.model.ToolExecutionInfo;
import com.vibe.model.enums.AnalyzeAction;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

/**
 * 分析响应
 * 包含动作决策、氛围方案和执行元数据
 */
@Description("分析响应，包含动作决策、氛围方案和执行元数据")
public record AnalyzeResponse(
    @Description("动作：APPLY（应用新方案）/NO_ACTION（本次不更新）")
    AnalyzeAction action,

    @Description("可选提示信息（通常在 NO_ACTION 时返回原因）")
    String message,

    @Description("生成的氛围方案")
    AmbiencePlan plan,

    @Description("Token 使用统计，用于成本监控")
    TokenUsageInfo tokenUsage,

    @Description("工具执行详情，用于性能分析")
    List<ToolExecutionInfo> toolExecutions,

    @Description("处理耗时（毫秒）")
    long processingTimeMs
) {
    /**
     * 紧凑构造器：确保列表不可变
     */
    public AnalyzeResponse {
        if (toolExecutions != null) {
            toolExecutions = List.copyOf(toolExecutions);
        } else {
            toolExecutions = List.of();
        }
    }

    /**
     * 创建应用新方案的响应
     */
    public static AnalyzeResponse applied(
            AmbiencePlan plan,
            TokenUsageInfo tokenUsage,
            List<ToolExecutionInfo> toolExecutions,
            long processingTimeMs) {
        return new AnalyzeResponse(
            AnalyzeAction.APPLY,
            null,
            plan,
            tokenUsage,
            toolExecutions,
            processingTimeMs
        );
    }

    /**
     * 创建不执行操作的响应（无 Token 使用记录）
     */
    public static AnalyzeResponse noAction(String message) {
        return new AnalyzeResponse(
            AnalyzeAction.NO_ACTION,
            message,
            null,
            null,
            List.of(),
            0
        );
    }

    /**
     * 创建不执行操作的响应（带 Token 使用记录）
     * 用于 LLM 判断后决定不更新的场景
     */
    public static AnalyzeResponse noAction(String message, TokenUsageInfo tokenUsage, long processingTimeMs) {
        return new AnalyzeResponse(
            AnalyzeAction.NO_ACTION,
            message,
            null,
            tokenUsage,
            List.of(),
            processingTimeMs
        );
    }

    /**
     * 判断是否需要更新 UI
     */
    public boolean requiresUiUpdate() {
        return action != null && action.requiresUiUpdate();
    }

    /**
     * 判断是否有 Token 使用记录
     */
    public boolean hasTokenUsage() {
        return tokenUsage != null;
    }

    /**
     * 判断是否有工具执行记录
     */
    public boolean hasToolExecutions() {
        return toolExecutions != null && !toolExecutions.isEmpty();
    }

    /**
     * 获取工具执行数量
     */
    public int toolExecutionCount() {
        return toolExecutions != null ? toolExecutions.size() : 0;
    }
}
