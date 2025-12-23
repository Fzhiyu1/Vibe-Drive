package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Tool 开始执行事件
 */
@Description("Tool开始执行事件")
public record ToolStartEvent(
    @NotBlank(message = "Tool name cannot be empty")
    @Description("工具名称")
    String toolName,

    @Description("执行参数（JSON格式）")
    String arguments,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public ToolStartEvent(String toolName, String arguments) {
        this(toolName, arguments, Instant.now());
    }

    /**
     * 紧凑构造器：校验和默认值
     */
    public ToolStartEvent {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "tool_start";
}
