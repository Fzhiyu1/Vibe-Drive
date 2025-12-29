package com.vibe.memory;

import com.vibe.model.event.MemoryUpdatedEvent;
import com.vibe.sse.SseEventPublisher;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 记忆总结服务
 * 异步执行记忆压缩
 * 使用 DeepSeek 模型（更稳定经济）
 */
@Component
public class MemorySummarizer {

    private static final Logger log = LoggerFactory.getLogger(MemorySummarizer.class);

    private final ChatModel model;
    private final MemoryStore memoryStore;
    private final SseEventPublisher sseEventPublisher;

    public MemorySummarizer(
            @Qualifier("summarizerModel") ChatModel model,
            MemoryStore memoryStore,
            SseEventPublisher sseEventPublisher) {
        this.model = model;
        this.memoryStore = memoryStore;
        this.sseEventPublisher = sseEventPublisher;
    }

    /**
     * 异步触发记忆总结
     */
    @Async
    public void triggerAsync(String sessionId) {
        try {
            log.info("[MemorySummarizer] 开始总结记忆: {}", sessionId);

            String memory = memoryStore.getMemory(sessionId);
            String summarized = summarize(memory);
            memoryStore.setMemory(sessionId, summarized);

            // 推送 SSE 事件通知前端
            MemoryUpdatedEvent event = new MemoryUpdatedEvent(sessionId, summarized);
            sseEventPublisher.publish(sessionId, MemoryUpdatedEvent.EVENT_TYPE, event);

            log.info("[MemorySummarizer] 记忆总结完成并已推送: {}", sessionId);
        } catch (Exception e) {
            log.error("[MemorySummarizer] 记忆总结失败: {}", sessionId, e);
        }
    }

    private String summarize(String memory) {
        String prompt = """
            请总结以下记忆内容，输出新的 MEMORY.md 格式：

            规则：
            1. 将「最近」区的重要信息整合到「摘要」区
            2. 合并重复或相似的信息
            3. 处理遗忘请求（如果用户说"忘掉X"，则X和遗忘请求互相抵消）
            4. 保留用户偏好和重要信息
            5. 清空「最近」区

            当前记忆：
            """ + memory + """

            请直接输出新的记忆内容，格式如下：
            # 记忆

            ## 摘要（长期记忆）
            [整合后的内容]

            ## 最近（短期记忆）
            暂无
            """;

        return model.chat(prompt);
    }
}
