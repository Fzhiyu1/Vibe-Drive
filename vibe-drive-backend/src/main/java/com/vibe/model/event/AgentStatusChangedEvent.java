package com.vibe.model.event;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Agent 状态变化事件
 */
@Description("Agent状态变化事件")
public record AgentStatusChangedEvent(
    @Description("Agent是否运行中")
    boolean running,

    @NotBlank(message = "Event type cannot be empty")
    @Description("状态变化事件：started/stopped/error")
    String event,

    @Description("错误信息，仅在 event=error 时存在")
    String error,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 事件类型常量
     */
    public static final String EVENT_STARTED = "started";
    public static final String EVENT_STOPPED = "stopped";
    public static final String EVENT_ERROR = "error";

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public AgentStatusChangedEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 创建启动事件
     */
    public static AgentStatusChangedEvent started() {
        return new AgentStatusChangedEvent(true, EVENT_STARTED, null, Instant.now());
    }

    /**
     * 创建停止事件
     */
    public static AgentStatusChangedEvent stopped() {
        return new AgentStatusChangedEvent(false, EVENT_STOPPED, null, Instant.now());
    }

    /**
     * 创建错误事件
     */
    public static AgentStatusChangedEvent error(String error) {
        return new AgentStatusChangedEvent(false, EVENT_ERROR, error, Instant.now());
    }

    /**
     * 判断是否为启动事件
     */
    public boolean isStarted() {
        return EVENT_STARTED.equals(event);
    }

    /**
     * 判断是否为停止事件
     */
    public boolean isStopped() {
        return EVENT_STOPPED.equals(event);
    }

    /**
     * 判断是否为错误事件
     */
    public boolean isError() {
        return EVENT_ERROR.equals(event);
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "agent_status_changed";
}
