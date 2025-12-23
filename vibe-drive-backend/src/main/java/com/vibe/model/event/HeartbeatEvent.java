package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;

import java.time.Instant;

/**
 * 心跳事件
 * 用于保持 SSE 连接活跃
 */
@Description("心跳事件，用于保持SSE连接活跃")
public record HeartbeatEvent(
    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public HeartbeatEvent() {
        this(Instant.now());
    }

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public HeartbeatEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "heartbeat";

    /**
     * 默认心跳间隔（秒）
     */
    public static final int DEFAULT_INTERVAL_SECONDS = 30;
}
