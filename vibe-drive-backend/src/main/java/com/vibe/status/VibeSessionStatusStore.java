package com.vibe.status;

import com.vibe.model.api.VibeStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话状态存储
 * 用于在多个 Controller 之间共享 sessionId -> VibeStatus 的映射
 */
@Component
public class VibeSessionStatusStore {

    private final Map<String, VibeStatus> statuses = new ConcurrentHashMap<>();

    public VibeStatus getOrInitial(String sessionId) {
        return statuses.getOrDefault(sessionId, VibeStatus.initial(sessionId));
    }

    public VibeStatus put(String sessionId, VibeStatus status) {
        statuses.put(sessionId, status);
        return status;
    }
}
