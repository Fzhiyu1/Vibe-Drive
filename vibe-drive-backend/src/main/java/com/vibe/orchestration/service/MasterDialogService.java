package com.vibe.orchestration.service;

import com.vibe.agent.MasterAgent;
import com.vibe.agent.MasterAgentFactory;
import com.vibe.context.SessionContext;
import com.vibe.orchestration.VibeTaskManager;
import com.vibe.orchestration.callback.MasterStreamCallback;
import com.vibe.orchestration.dto.VibeMessage;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主智能体对话服务
 * 处理用户与主智能体的对话
 */
@Service
public class MasterDialogService {

    private static final Logger log = LoggerFactory.getLogger(MasterDialogService.class);

    private final MasterAgentFactory agentFactory;
    private final VibeTaskManager vibeTaskManager;

    public MasterDialogService(MasterAgentFactory agentFactory, VibeTaskManager vibeTaskManager) {
        this.agentFactory = agentFactory;
        this.vibeTaskManager = vibeTaskManager;
    }

    /**
     * 执行对话（流式）
     * LangChain4j 会自动处理工具调用循环，无需手动递归
     */
    public void executeChat(String sessionId, String userMessage, MasterStreamCallback callback) {
        log.info("开始主智能体对话: sessionId={}, message={}", sessionId, userMessage);

        // 1. 检查消息队列（氛围任务完成/失败通知）
        List<VibeMessage> vibeMessages = vibeTaskManager.pollMessages(sessionId);

        // 2. 构建增强的用户消息
        String enhancedMessage = buildEnhancedMessage(userMessage, vibeMessages);

        // 设置会话上下文（在回调中清除）
        SessionContext.setSessionId(sessionId);

        try {
            MasterAgent agent = agentFactory.createAgent();
            TokenStream tokenStream = agent.chat(enhancedMessage, sessionId);

            tokenStream
                .onPartialResponse(callback::onTextDelta)
                .beforeToolExecution(before -> {
                    // 重新设置 SessionContext，因为工具可能在不同线程执行
                    SessionContext.setSessionId(sessionId);
                    callback.onToolStart(
                        before.request().name(),
                        before.request().arguments()
                    );
                })
                .onToolExecuted(execution -> {
                    callback.onToolComplete(
                        execution.request().name(),
                        execution.result()
                    );
                })
                .onCompleteResponse(response -> {
                    log.info("对话完成: sessionId={}", sessionId);
                    SessionContext.clear();
                    callback.onComplete();
                })
                .onError(error -> {
                    log.error("对话错误: sessionId={}", sessionId, error);
                    SessionContext.clear();
                    callback.onError(error);
                })
                .start();

        } catch (Exception e) {
            log.error("主智能体对话异常: sessionId={}", sessionId, e);
            SessionContext.clear();
            callback.onError(e);
        }
    }

    /**
     * 构建增强的用户消息（注入氛围任务完成通知）
     */
    private String buildEnhancedMessage(String userMessage, List<VibeMessage> vibeMessages) {
        if (vibeMessages == null || vibeMessages.isEmpty()) {
            return userMessage;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[系统通知] ");

        for (VibeMessage msg : vibeMessages) {
            switch (msg) {
                case VibeMessage.Success s -> sb.append("氛围编排已完成。");
                case VibeMessage.Failed f -> sb.append("氛围编排失败: ").append(f.error()).append("。");
                case VibeMessage.Cancelled c -> sb.append("氛围编排已取消。");
            }
        }

        sb.append("\n\n[用户消息] ").append(userMessage);

        log.info("注入氛围消息: count={}", vibeMessages.size());
        return sb.toString();
    }
}
