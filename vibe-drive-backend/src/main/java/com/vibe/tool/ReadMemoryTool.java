package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.memory.MemoryStore;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 读取记忆工具
 */
@Component
public class ReadMemoryTool {

    private final MemoryStore memoryStore;

    public ReadMemoryTool(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Tool("读取当前记忆内容")
    public String readMemory() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法读取：会话未初始化";
        }

        return memoryStore.getMemory(sessionId);
    }
}
