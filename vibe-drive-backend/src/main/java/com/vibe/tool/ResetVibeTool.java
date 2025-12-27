package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.orchestration.VibeTaskManager;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 重置氛围工具
 * 终止当前正在运行的氛围编排任务
 */
@Component
public class ResetVibeTool {

    private static final Logger log = LoggerFactory.getLogger(ResetVibeTool.class);

    private final VibeTaskManager vibeTaskManager;

    public ResetVibeTool(VibeTaskManager vibeTaskManager) {
        this.vibeTaskManager = vibeTaskManager;
    }

    @Tool("终止当前氛围编排任务。当用户说'算了'、'不要了'、'取消'等时调用此工具。")
    public String resetVibe() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法终止氛围任务：会话未初始化";
        }

        log.info("终止氛围任务: sessionId={}", sessionId);

        if (vibeTaskManager.isRunning(sessionId)) {
            vibeTaskManager.cancelTask(sessionId);
            return "已终止氛围编排";
        } else {
            return "当前没有正在运行的氛围任务";
        }
    }
}
