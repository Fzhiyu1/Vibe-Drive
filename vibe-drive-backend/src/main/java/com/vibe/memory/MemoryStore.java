package com.vibe.memory;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记忆存储服务
 * 管理会话级别的用户记忆
 */
@Component
public class MemoryStore {

    private final Map<String, String> memories = new ConcurrentHashMap<>();
    private static final int SUMMARIZE_THRESHOLD = 2000;

    private static final String DEFAULT_MEMORY = """
        # 记忆

        ## 摘要（长期记忆）
        暂无

        ## 最近（短期记忆）
        暂无
        """;

    /**
     * 获取会话记忆
     */
    public String getMemory(String sessionId) {
        return memories.getOrDefault(sessionId, DEFAULT_MEMORY);
    }

    /**
     * 设置会话记忆
     */
    public void setMemory(String sessionId, String content) {
        memories.put(sessionId, content);
    }

    /**
     * 追加内容到「最近」区
     */
    public void appendToRecent(String sessionId, String entry) {
        String memory = getMemory(sessionId);
        String marker = "## 最近（短期记忆）";
        int idx = memory.indexOf(marker);

        if (idx != -1) {
            int insertPos = idx + marker.length();
            String before = memory.substring(0, insertPos);
            String after = memory.substring(insertPos);
            memory = before + "\n" + entry + after;
        } else {
            memory = memory + "\n\n## 最近（短期记忆）\n" + entry;
        }

        setMemory(sessionId, memory);
    }

    /**
     * 检查是否需要总结
     */
    public boolean needsSummarize(String sessionId) {
        String memory = getMemory(sessionId);
        return memory.length() > SUMMARIZE_THRESHOLD;
    }

    /**
     * 清除会话记忆
     */
    public void clearMemory(String sessionId) {
        memories.remove(sessionId);
    }
}
