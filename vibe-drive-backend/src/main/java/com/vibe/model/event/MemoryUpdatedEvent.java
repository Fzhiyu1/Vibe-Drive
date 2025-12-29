package com.vibe.model.event;

/**
 * 记忆更新事件
 * 当记忆总结完成后推送给前端
 */
public record MemoryUpdatedEvent(
    String sessionId,
    String content,
    long timestamp
) {
    public static final String EVENT_TYPE = "memory_updated";

    public MemoryUpdatedEvent(String sessionId, String content) {
        this(sessionId, content, System.currentTimeMillis());
    }
}
