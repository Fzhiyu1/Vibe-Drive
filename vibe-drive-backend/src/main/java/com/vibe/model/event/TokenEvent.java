package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;

import java.time.Instant;

/**
 * Token 输出事件
 * LLM 输出的 token 事件，用于流式展示
 */
@Description("LLM输出的token事件")
public record TokenEvent(
    @Description("token内容")
    String content,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public TokenEvent(String content) {
        this(content, Instant.now());
    }

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public TokenEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "token";
}
