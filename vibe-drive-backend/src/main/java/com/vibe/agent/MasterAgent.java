package com.vibe.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 主智能体接口
 * 用户交互入口，支持对话理解、任务分发和直接控制
 */
public interface MasterAgent {

    @UserMessage("{{prompt}}")
    TokenStream chat(@V("prompt") String prompt, @MemoryId String sessionId);
}
