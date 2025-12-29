package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.memory.MemoryStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 更新记忆工具
 */
@Component
public class UpdateMemoryTool {

    private final MemoryStore memoryStore;

    public UpdateMemoryTool(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Tool("更新记忆中的特定内容，使用字符串匹配替换")
    public String updateMemory(
            @P("要替换的原文本") String oldText,
            @P("替换后的新文本") String newText
    ) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法更新：会话未初始化";
        }

        String memory = memoryStore.getMemory(sessionId);

        if (!memory.contains(oldText)) {
            return "未找到匹配的文本: " + oldText;
        }

        memory = memory.replace(oldText, newText);
        memoryStore.setMemory(sessionId, memory);

        return "已更新记忆";
    }
}
