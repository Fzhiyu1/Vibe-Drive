package com.vibe.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import com.vibe.model.enums.SafetyMode;
import com.vibe.orchestration.callback.VibeStreamCallback;
import com.vibe.orchestration.dto.VibeDialogRequest;
import com.vibe.orchestration.dto.VibeMessage;
import com.vibe.orchestration.service.VibeDialogService;
import com.vibe.sse.SseEventPublisher;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * 氛围任务管理器
 * 管理异步氛围编排任务，每个会话只能有一个任务运行
 */
@Component
public class VibeTaskManager {

    private static final Logger log = LoggerFactory.getLogger(VibeTaskManager.class);

    private final VibeDialogService vibeDialogService;
    private final SseEventPublisher sseEventPublisher;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    // 当前运行的任务（每个 sessionId 只能有一个）
    private final Map<String, VibeTask> currentTasks = new ConcurrentHashMap<>();

    // 完成/失败消息队列（按 sessionId 隔离）
    private final Map<String, Queue<VibeMessage>> messageQueues = new ConcurrentHashMap<>();

    public VibeTaskManager(
            VibeDialogService vibeDialogService,
            SseEventPublisher sseEventPublisher,
            ObjectMapper objectMapper) {
        this.vibeDialogService = vibeDialogService;
        this.sseEventPublisher = sseEventPublisher;
        this.objectMapper = objectMapper;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "vibe-task-");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动新任务（自动终止旧任务）
     * @return 任务 ID
     */
    public String startTask(String sessionId, Environment env) {
        // 1. 终止旧任务
        cancelTask(sessionId);

        // 2. 创建新任务
        String taskId = generateTaskId();
        CompletableFuture<Void> future = new CompletableFuture<>();
        VibeTask task = new VibeTask(taskId, sessionId, future, Instant.now());

        currentTasks.put(sessionId, task);
        log.info("启动氛围任务: taskId={}, sessionId={}", taskId, sessionId);

        // 3. 异步执行
        executor.submit(() -> executeTask(task, env));

        return taskId;
    }

    /**
     * 终止当前任务
     */
    public void cancelTask(String sessionId) {
        VibeTask task = currentTasks.remove(sessionId);
        if (task != null) {
            log.info("终止氛围任务: taskId={}, sessionId={}", task.taskId(), sessionId);
            task.future().cancel(true);

            // 推送取消事件
            publishEvent(sessionId, "vibe_cancelled", Map.of("taskId", task.taskId()));

            // 消息入队
            enqueueMessage(sessionId, new VibeMessage.Cancelled(task.taskId(), Instant.now()));
        }
    }

    /**
     * 获取队列中的消息（并清空）
     */
    public List<VibeMessage> pollMessages(String sessionId) {
        Queue<VibeMessage> queue = messageQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }

        List<VibeMessage> messages = new ArrayList<>();
        VibeMessage msg;
        while ((msg = queue.poll()) != null) {
            messages.add(msg);
        }
        return messages;
    }

    /**
     * 检查是否有任务在运行
     */
    public boolean isRunning(String sessionId) {
        return currentTasks.containsKey(sessionId);
    }

    /**
     * 执行任务
     */
    private void executeTask(VibeTask task, Environment env) {
        String sessionId = task.sessionId();
        String taskId = task.taskId();

        try {
            VibeDialogRequest request = VibeDialogRequest.of(sessionId, env);

            vibeDialogService.executeDialog(request, new VibeStreamCallback() {
                @Override
                public void onTextDelta(String text) {
                    // 可选：推送思考过程
                }

                @Override
                public void onToolStart(String toolName, Object toolInput) {
                    publishEvent(sessionId, "vibe_tool_start", Map.of(
                        "taskId", taskId,
                        "toolName", toolName,
                        "input", toJsonSafe(toolInput)
                    ));
                }

                @Override
                public void onToolComplete(String toolName, String result) {
                    publishEvent(sessionId, "vibe_tool_end", Map.of(
                        "taskId", taskId,
                        "toolName", toolName,
                        "result", result != null ? result : ""
                    ));
                }

                @Override
                public void onToolError(String toolName, Throwable error) {
                    publishEvent(sessionId, "vibe_tool_error", Map.of(
                        "taskId", taskId,
                        "toolName", toolName,
                        "error", error != null ? error.getMessage() : "Unknown error"
                    ));
                }

                @Override
                public void onComplete(AmbiencePlan plan, ChatResponse response) {
                    // 检查任务是否仍然有效（可能已被取消）
                    if (!isCurrentTask(sessionId, taskId)) {
                        log.info("任务已被取消，忽略完成回调: taskId={}", taskId);
                        return;
                    }

                    log.info("氛围任务完成: taskId={}, sessionId={}", taskId, sessionId);

                    // 推送完成事件
                    publishEvent(sessionId, "vibe_complete", Map.of(
                        "taskId", taskId,
                        "plan", plan != null ? plan : Map.of()
                    ));

                    // 消息入队
                    enqueueMessage(sessionId, new VibeMessage.Success(taskId, plan, Instant.now()));

                    // 清理任务
                    currentTasks.remove(sessionId, task);
                    task.future().complete(null);
                }

                @Override
                public void onError(Throwable error) {
                    // 检查任务是否仍然有效
                    if (!isCurrentTask(sessionId, taskId)) {
                        log.info("任务已被取消，忽略错误回调: taskId={}", taskId);
                        return;
                    }

                    String errorMsg = error != null ? error.getMessage() : "Unknown error";
                    log.error("氛围任务失败: taskId={}, sessionId=, error={}", taskId, sessionId, errorMsg);

                    // 推送错误事件
                    publishEvent(sessionId, "vibe_error", Map.of(
                        "taskId", taskId,
                        "error", errorMsg
                    ));

                    // 消息入队
                    enqueueMessage(sessionId, new VibeMessage.Failed(taskId, errorMsg, Instant.now()));

                    // 清理任务
                    currentTasks.remove(sessionId, task);
                    task.future().completeExceptionally(error);
                }

                @Override
                public void onSafetyModeApplied(SafetyMode mode) {
                    publishEvent(sessionId, "vibe_safety_mode", Map.of(
                        "taskId", taskId,
                        "mode", mode.name()
                    ));
                }
            });

        } catch (Exception e) {
            log.error("执行氛围任务异常: taskId={}, sessionId={}", taskId, sessionId, e);

            publishEvent(sessionId, "vibe_error", Map.of(
                "taskId", taskId,
                "error", e.getMessage()
            ));

            enqueueMessage(sessionId, new VibeMessage.Failed(taskId, e.getMessage(), Instant.now()));
            currentTasks.remove(sessionId, task);
            task.future().completeExceptionally(e);
        }
    }

    /**
     * 检查是否是当前任务
     */
    private boolean isCurrentTask(String sessionId, String taskId) {
        VibeTask current = currentTasks.get(sessionId);
        return current != null && current.taskId().equals(taskId);
    }

    /**
     * 消息入队
     */
    private void enqueueMessage(String sessionId, VibeMessage message) {
        messageQueues
            .computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>())
            .offer(message);
    }

    /**
     * 发布 SSE 事件
     */
    private void publishEvent(String sessionId, String eventType, Object data) {
        try {
            sseEventPublisher.publish(sessionId, eventType, data);
        } catch (Exception e) {
            log.warn("发布 SSE 事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
        }
    }

    /**
     * 安全转换为 JSON
     */
    private Object toJsonSafe(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 生成任务 ID
     */
    private String generateTaskId() {
        return "vibe-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 氛围任务记录
     */
    private record VibeTask(
        String taskId,
        String sessionId,
        CompletableFuture<Void> future,
        Instant startTime
    ) {}
}
