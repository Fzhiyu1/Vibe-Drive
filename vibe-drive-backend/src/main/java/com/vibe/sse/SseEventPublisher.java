package com.vibe.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.event.AgentStatusChangedEvent;
import com.vibe.model.event.AmbienceChangedEvent;
import com.vibe.model.event.EnvironmentUpdateEvent;
import com.vibe.model.event.HeartbeatEvent;
import com.vibe.model.event.SafetyModeChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * SSE 事件发布器
 * 管理所有 SSE 连接，支持按会话和主题发布事件
 */
@Component
public class SseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseEventPublisher.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Set<EmitterInfo>> sessionEmitters = new ConcurrentHashMap<>();

    public SseEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册 SSE 连接
     */
    public void register(String sessionId, SseEmitter emitter, Set<String> topics) {
        sessionEmitters
            .computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>())
            .add(new EmitterInfo(emitter, normalizeTopics(topics)));
        log.debug("注册 SSE 连接: sessionId={}, topics={}", sessionId, topics);
    }

    /**
     * 注销 SSE 连接
     */
    public void unregister(String sessionId, SseEmitter emitter) {
        Set<EmitterInfo> emitters = sessionEmitters.get(sessionId);
        if (emitters != null) {
            emitters.removeIf(info -> info.emitter() == emitter);
            if (emitters.isEmpty()) {
                sessionEmitters.remove(sessionId);
            }
        }
        log.debug("注销 SSE 连接: sessionId={}", sessionId);
    }

    /**
     * 发布事件到指定会话
     */
    public void publish(String sessionId, String eventType, Object data) {
        Set<EmitterInfo> emitters = sessionEmitters.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (EmitterInfo info : emitters) {
            if (info.shouldReceive(eventType)) {
                sendEvent(sessionId, info.emitter(), eventType, data);
            }
        }
    }

    /**
     * 发布事件到所有会话
     */
    public void publishToAll(String eventType, Object data) {
        for (Map.Entry<String, Set<EmitterInfo>> entry : sessionEmitters.entrySet()) {
            for (EmitterInfo info : entry.getValue()) {
                if (info.shouldReceive(eventType)) {
                    sendEvent(entry.getKey(), info.emitter(), eventType, data);
                }
            }
        }
    }

    /**
     * 心跳定时任务（每30秒）
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (sessionEmitters.isEmpty()) {
            return;
        }

        HeartbeatEvent heartbeat = new HeartbeatEvent();
        publishToAll(HeartbeatEvent.EVENT_TYPE, heartbeat);
        log.trace("发送心跳: connections={}", countConnections());
    }

    /**
     * 获取当前连接数
     */
    public int countConnections() {
        return sessionEmitters.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    private void sendEvent(String sessionId, SseEmitter emitter, String eventType, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(json));
        } catch (Exception e) {
            log.warn("发送 SSE 事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
            unregister(sessionId, emitter);
            try {
                emitter.completeWithError(e);
            } catch (Exception completeError) {
                log.debug("Failed to complete SSE emitter after send error", completeError);
            }
        }
    }

    private static Set<String> normalizeTopics(Set<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return Set.of();
        }
        return topics.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String topicForEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case AmbienceChangedEvent.EVENT_TYPE -> "ambience";
            case SafetyModeChangedEvent.EVENT_TYPE -> "safety";
            case AgentStatusChangedEvent.EVENT_TYPE -> "status";
            case EnvironmentUpdateEvent.EVENT_TYPE -> "environment";
            default -> null;
        };
    }

    /**
     * Emitter 信息（包含订阅的主题）
     */
    private record EmitterInfo(SseEmitter emitter, Set<String> topics) {
        boolean shouldReceive(String eventType) {
            // 如果没有指定主题，接收所有事件
            if (topics == null || topics.isEmpty()) {
                return true;
            }
            // 心跳事件始终发送
            if (HeartbeatEvent.EVENT_TYPE.equals(eventType)) {
                return true;
            }
            String normalizedEventType = eventType != null ? eventType.toLowerCase() : "";

            // 允许两种订阅方式：
            // 1) 直接订阅事件类型（ambience_changed 等）
            // 2) 订阅主题分类（ambience/safety/status/environment）
            if (topics.contains(normalizedEventType)) {
                return true;
            }
            String topic = topicForEventType(normalizedEventType);
            return topic != null && topics.contains(topic);
        }
    }
}
