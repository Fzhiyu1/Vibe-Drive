package com.vibe.controller;

import com.vibe.memory.MemoryStore;
import com.vibe.memory.MemorySummarizer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 记忆 API 控制器
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    private final MemoryStore memoryStore;
    private final MemorySummarizer summarizer;
    private final ChatMemoryStore chatMemoryStore;

    public MemoryController(MemoryStore memoryStore, MemorySummarizer summarizer, ChatMemoryStore chatMemoryStore) {
        this.memoryStore = memoryStore;
        this.summarizer = summarizer;
        this.chatMemoryStore = chatMemoryStore;
    }

    /**
     * 同步记忆（前端 → 后端）
     */
    @PostMapping("/sync")
    public SyncResponse syncMemory(
            @RequestParam String sessionId,
            @RequestBody MemorySyncRequest request
    ) {
        int contentLength = request.content() != null ? request.content().length() : 0;
        log.info("[MemoryController] 同步记忆: sessionId={}, 内容长度={}", sessionId, contentLength);

        memoryStore.setMemory(sessionId, request.content());

        // 检查是否需要总结
        if (memoryStore.needsSummarize(sessionId)) {
            log.info("[MemoryController] 记忆超过阈值(2000)，触发异步总结: sessionId={}", sessionId);
            summarizer.triggerAsync(sessionId);
            return new SyncResponse(true, "同步成功，已触发记忆总结");
        }

        return new SyncResponse(true, "同步成功");
    }

    /**
     * 获取记忆
     */
    @GetMapping
    public MemoryResponse getMemory(@RequestParam String sessionId) {
        return new MemoryResponse(memoryStore.getMemory(sessionId));
    }

    /**
     * 清除聊天记忆（修复损坏的对话历史）
     */
    @DeleteMapping("/chat")
    public SyncResponse clearChatMemory(@RequestParam String sessionId) {
        log.info("[MemoryController] 清除聊天记忆: sessionId={}", sessionId);
        chatMemoryStore.deleteMessages(sessionId);
        return new SyncResponse(true, "聊天记忆已清除");
    }

    public record MemorySyncRequest(String content) {}
    public record MemoryResponse(String content) {}
    public record SyncResponse(boolean success, String message) {}
}
