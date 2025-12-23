package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;

/**
 * 工具执行详情
 * 用于性能分析和调试
 */
@Description("工具执行详情，用于性能分析和调试")
public record ToolExecutionInfo(
    @Description("工具名称")
    String toolName,

    @Description("执行参数（JSON格式）")
    String arguments,

    @Description("执行结果（JSON格式）")
    String result,

    @Description("执行耗时（毫秒）")
    Long durationMs,

    @Description("是否执行成功")
    boolean success,

    @Description("错误信息，仅在失败时存在")
    String error
) {
    /**
     * 紧凑构造器：校验参数
     */
    public ToolExecutionInfo {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
    }

    /**
     * 创建成功的执行记录
     */
    public static ToolExecutionInfo success(String toolName, String arguments, String result, long durationMs) {
        return new ToolExecutionInfo(toolName, arguments, result, durationMs, true, null);
    }

    /**
     * 创建失败的执行记录
     */
    public static ToolExecutionInfo error(String toolName, String arguments, String error) {
        return new ToolExecutionInfo(toolName, arguments, null, null, false, error);
    }

    /**
     * 创建失败的执行记录（带耗时）
     */
    public static ToolExecutionInfo error(String toolName, String arguments, String error, long durationMs) {
        return new ToolExecutionInfo(toolName, arguments, null, durationMs, false, error);
    }

    /**
     * 判断执行是否超时（超过 5 秒）
     */
    public boolean isSlowExecution() {
        return durationMs != null && durationMs > 5000;
    }

    /**
     * 获取格式化的耗时
     */
    public String durationFormatted() {
        if (durationMs == null) {
            return "N/A";
        }
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        return String.format("%.2fs", durationMs / 1000.0);
    }
}
