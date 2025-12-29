package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.memory.MemoryStore;
import com.vibe.memory.MemorySummarizer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 保存记忆工具
 */
@Component
public class SaveMemoryTool {

    private final MemoryStore memoryStore;
    private final MemorySummarizer summarizer;

    public SaveMemoryTool(MemoryStore memoryStore, MemorySummarizer summarizer) {
        this.memoryStore = memoryStore;
        this.summarizer = summarizer;
    }

    @Tool("保存重要信息到记忆，会追加到「最近」区。用于记录用户偏好、重要对话等。")
    public String saveMemory(@P("要记录的内容") String content) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法保存：会话未初始化";
        }

        String timestamp = LocalDate.now().toString();
        String entry = "- " + timestamp + ": " + content;

        memoryStore.appendToRecent(sessionId, entry);

        // 检查是否需要总结
        if (memoryStore.needsSummarize(sessionId)) {
            summarizer.triggerAsync(sessionId);
        }

        return "已保存到记忆";
    }
}
