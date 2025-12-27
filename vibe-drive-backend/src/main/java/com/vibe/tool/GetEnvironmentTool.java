package com.vibe.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.context.SessionContext;
import com.vibe.model.Environment;
import com.vibe.model.api.VibeStatus;
import com.vibe.status.VibeSessionStatusStore;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 获取环境工具
 * 获取当前会话的环境数据
 */
@Component
public class GetEnvironmentTool {

    private final VibeSessionStatusStore statusStore;
    private final ObjectMapper objectMapper;

    public GetEnvironmentTool(VibeSessionStatusStore statusStore, ObjectMapper objectMapper) {
        this.statusStore = statusStore;
        this.objectMapper = objectMapper;
    }

    @Tool("获取当前环境数据，包括GPS位置、天气、速度、用户心情、时间等信息")
    public String getEnvironment() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法获取环境数据：会话未初始化";
        }

        VibeStatus status = statusStore.getOrInitial(sessionId);
        Environment env = status.lastEnvironment();

        if (env == null) {
            return "当前没有环境数据，请先设置环境";
        }

        try {
            return objectMapper.writeValueAsString(env);
        } catch (JsonProcessingException e) {
            return "环境数据序列化失败: " + e.getMessage();
        }
    }
}
