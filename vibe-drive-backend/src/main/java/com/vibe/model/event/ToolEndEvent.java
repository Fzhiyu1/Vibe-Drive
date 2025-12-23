package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Tool 执行完成事件
 */
@Description("Tool执行完成事件")
public record ToolEndEvent(
    @NotBlank(message = "Tool name cannot be empty")
    @Description("工具名称")
    String toolName,

    @Min(value = 0, message = "Duration must be non-negative")
    @Description("执行耗时（毫秒）")
    long durationMs,

    @Description("是否执行成功")
    boolean success,

    @Description("错误信息，仅在失败时存在")
    String error,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：成功执行
     */
    public ToolEndEvent(String toolName, long durationMs, boolean success) {
        this(toolName, durationMs, success, null, Instant.now());
    }

    /**
     * 紧凑构造器：校验和默认值
     */
    public ToolEndEvent {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 创建成功事件
     */
    public static ToolEndEvent success(String toolName, long durationMs) {
        return new ToolEndEvent(toolName, durationMs, true, null, Instant.now());
    }

    /**
     * 创建失败事件
     */
    public static ToolEndEvent error(String toolName, String error) {
        return new ToolEndEvent(toolName, 0, false, error, Instant.now());
    }

    /**
     * 创建失败事件（带耗时）
     */
    public static ToolEndEvent error(String toolName, long durationMs, String error) {
        return new ToolEndEvent(toolName, durationMs, false, error, Instant.now());
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "tool_end";
}
