package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.model.Environment;
import com.vibe.model.enums.*;
import com.vibe.orchestration.VibeTaskManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 调用氛围智能体工具（异步版本）
 * 启动异步氛围编排任务，立即返回任务 ID
 */
@Component
public class CallVibeAgentTool {

    private static final Logger log = LoggerFactory.getLogger(CallVibeAgentTool.class);

    private final VibeTaskManager vibeTaskManager;

    public CallVibeAgentTool(VibeTaskManager vibeTaskManager) {
        this.vibeTaskManager = vibeTaskManager;
    }

    @Tool("""
        异步启动氛围智能体进行自动编排。立即返回任务 ID，编排结果通过 SSE 实时推送。

        参数说明：
        - gpsTag: HIGHWAY/TUNNEL/BRIDGE/URBAN/SUBURBAN/MOUNTAIN/COASTAL/PARKING
        - weather: SUNNY/CLOUDY/RAINY/SNOWY/FOGGY
        - speed: 0-200 km/h
        - userMood: HAPPY/CALM/TIRED/STRESSED/EXCITED
        - timeOfDay: DAWN/MORNING/NOON/AFTERNOON/EVENING/NIGHT/MIDNIGHT
        - passengerCount: 1-7
        - routeType: HIGHWAY/URBAN/MOUNTAIN/COASTAL/TUNNEL
        """)
    public String callVibeAgent(
        @P("地理标签") String gpsTag,
        @P("天气") String weather,
        @P("车速 km/h") int speed,
        @P("用户心情") String userMood,
        @P("时段") String timeOfDay,
        @P("乘客数量") int passengerCount,
        @P("路线类型") String routeType
    ) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "无法调用氛围智能体：会话未初始化";
        }

        try {
            Environment env = buildEnvironment(gpsTag, weather, speed, userMood,
                                               timeOfDay, passengerCount, routeType);

            log.info("异步启动氛围智能体: sessionId={}, env={}", sessionId, env);

            // 启动异步任务（自动终止旧任务）
            String taskId = vibeTaskManager.startTask(sessionId, env);

            // 立即返回
            return "已开始编排，任务ID: " + taskId;
        } catch (Exception e) {
            log.error("启动氛围智能体失败: sessionId={}", sessionId, e);
            return "启动氛围智能体失败: " + e.getMessage();
        }
    }

    private Environment buildEnvironment(String gpsTag, String weather, int speed,
                                          String userMood, String timeOfDay,
                                          int passengerCount, String routeType) {
        return Environment.builder()
            .gpsTag(parseEnum(GpsTag.class, gpsTag, GpsTag.URBAN))
            .weather(parseEnum(Weather.class, weather, Weather.SUNNY))
            .speed(Math.max(0, Math.min(200, speed)))
            .userMood(parseEnum(UserMood.class, userMood, UserMood.CALM))
            .timeOfDay(parseEnum(TimeOfDay.class, timeOfDay, TimeOfDay.MORNING))
            .passengerCount(Math.max(1, Math.min(7, passengerCount)))
            .routeType(parseEnum(RouteType.class, routeType, RouteType.URBAN))
            .build();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
