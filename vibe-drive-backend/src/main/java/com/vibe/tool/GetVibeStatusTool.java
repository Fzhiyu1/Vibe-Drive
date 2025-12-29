package com.vibe.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.context.SessionContext;
import com.vibe.orchestration.VibeTaskManager;
import com.vibe.orchestration.dto.VibeTaskStatus;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 获取 Vibe 智能体状态工具
 * 让主智能体能够感知 Vibe 智能体的执行状态
 */
@Component
public class GetVibeStatusTool {

    private final VibeTaskManager vibeTaskManager;
    private final ObjectMapper objectMapper;

    public GetVibeStatusTool(VibeTaskManager vibeTaskManager, ObjectMapper objectMapper) {
        this.vibeTaskManager = vibeTaskManager;
        this.objectMapper = objectMapper;
    }

    @Tool("查询当前 Vibe 智能体的状态，包括是否在运行、已执行的工具列表、当前正在执行的工具")
    public String getVibeStatus() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法获取状态：会话未初始化";
        }

        VibeTaskStatus status = vibeTaskManager.getTaskStatus(sessionId);

        try {
            return objectMapper.writeValueAsString(status);
        } catch (JsonProcessingException e) {
            return "状态序列化失败: " + e.getMessage();
        }
    }
}
