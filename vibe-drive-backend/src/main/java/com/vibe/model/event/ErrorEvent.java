package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;

import java.time.Instant;

/**
 * 错误事件
 */
@Description("错误事件")
public record ErrorEvent(
    @Description("错误码")
    String code,

    @Description("错误信息")
    String message,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public ErrorEvent(String code, String message) {
        this(code, message, Instant.now());
    }

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public ErrorEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 常用错误码
     */
    public static final String CODE_LLM_ERROR = "LLM_ERROR";
    public static final String CODE_LLM_TIMEOUT = "LLM_TIMEOUT";
    public static final String CODE_TOOL_ERROR = "TOOL_ERROR";
    public static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * 创建 LLM 错误事件
     */
    public static ErrorEvent llmError(String message) {
        return new ErrorEvent(CODE_LLM_ERROR, message);
    }

    /**
     * 创建 LLM 超时事件
     */
    public static ErrorEvent llmTimeout() {
        return new ErrorEvent(CODE_LLM_TIMEOUT, "LLM 服务超时");
    }

    /**
     * 创建工具错误事件
     */
    public static ErrorEvent toolError(String message) {
        return new ErrorEvent(CODE_TOOL_ERROR, message);
    }

    /**
     * 创建内部错误事件
     */
    public static ErrorEvent internalError(String message) {
        return new ErrorEvent(CODE_INTERNAL_ERROR, message);
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "error";
}
