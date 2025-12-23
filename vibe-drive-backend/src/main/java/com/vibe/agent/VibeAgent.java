package com.vibe.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Vibe Agent AI Service 接口
 * 使用 LangChain4j 声明式接口，自动处理 Tool Calling 和流式输出
 */
public interface VibeAgent {

    /**
     * 分析环境数据，生成氛围方案（流式）
     *
     * @param prompt 用户提示（包含环境数据）
     * @param sessionId 会话 ID，用于隔离不同用户/车辆的会话
     * @return TokenStream 流式响应
     */
    @UserMessage("{{prompt}}")
    TokenStream analyze(
        @V("prompt") String prompt,
        @MemoryId String sessionId
    );
}
