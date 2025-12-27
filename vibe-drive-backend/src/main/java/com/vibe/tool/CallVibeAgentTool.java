package com.vibe.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.context.SessionContext;
import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import com.vibe.model.enums.*;
import com.vibe.orchestration.dto.VibeDialogRequest;
import com.vibe.orchestration.dto.VibeDialogResult;
import com.vibe.orchestration.service.VibeDialogService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 调用氛围智能体工具
 * 根据环境数据调用氛围智能体进行自动编排
 */
@Component
public class CallVibeAgentTool {

    private static final Logger log = LoggerFactory.getLogger(CallVibeAgentTool.class);
    private static final int TIMEOUT_SECONDS = 120;

    private final VibeDialogService vibeDialogService;
    private final ObjectMapper objectMapper;

    public CallVibeAgentTool(VibeDialogService vibeDialogService, ObjectMapper objectMapper) {
        this.vibeDialogService = vibeDialogService;
        this.objectMapper = objectMapper;
    }

    @Tool("""
        调用氛围智能体进行自动编排。根据环境数据自动推荐音乐、灯光、香氛、按摩等。

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

            log.info("调用氛围智能体: sessionId={}, env={}", sessionId, env);

            VibeDialogRequest request = VibeDialogRequest.of(sessionId, env);
            VibeDialogResult result = vibeDialogService.executeDialogAsync(request)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (result.success()) {
                return formatResult(result.plan());
            } else {
                return "氛围编排失败: " + result.errorMessage();
            }
        } catch (Exception e) {
            log.error("调用氛围智能体失败: sessionId={}", sessionId, e);
            return "调用氛围智能体失败: " + e.getMessage();
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

    private String formatResult(AmbiencePlan plan) {
        if (plan == null) {
            return "{\"error\": \"氛围编排完成，但未生成方案\"}";
        }

        try {
            return objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            log.error("序列化 AmbiencePlan 失败", e);
            return "{\"error\": \"序列化失败: " + e.getMessage() + "\"}";
        }
    }
}
