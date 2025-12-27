package com.vibe.tool;

import com.vibe.context.SessionContext;
import com.vibe.model.Environment;
import com.vibe.model.api.VibeStatus;
import com.vibe.model.enums.*;
import com.vibe.model.event.EnvironmentUpdateEvent;
import com.vibe.sse.SseEventPublisher;
import com.vibe.status.VibeSessionStatusStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 设置环境工具
 * AI 根据用户描述生成环境数据
 */
@Component
public class SetEnvironmentTool {

    private final VibeSessionStatusStore statusStore;
    private final SseEventPublisher sseEventPublisher;

    public SetEnvironmentTool(VibeSessionStatusStore statusStore,
                              SseEventPublisher sseEventPublisher) {
        this.statusStore = statusStore;
        this.sseEventPublisher = sseEventPublisher;
    }

    @Tool("""
        设置模拟环境数据。根据用户描述生成合适的环境参数。

        参数说明：
        - gpsTag: HIGHWAY/TUNNEL/BRIDGE/URBAN/SUBURBAN/MOUNTAIN/COASTAL/PARKING
        - weather: SUNNY/CLOUDY/RAINY/SNOWY/FOGGY
        - speed: 0-200 km/h
        - userMood: HAPPY/CALM/TIRED/STRESSED/EXCITED
        - timeOfDay: DAWN/MORNING/NOON/AFTERNOON/EVENING/NIGHT/MIDNIGHT
        - passengerCount: 1-7
        - routeType: HIGHWAY/URBAN/MOUNTAIN/COASTAL/TUNNEL
        """)
    public String setEnvironment(
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
            return "无法设置环境：会话未初始化";
        }

        try {
            Environment env = Environment.builder()
                .gpsTag(parseEnum(GpsTag.class, gpsTag, GpsTag.URBAN))
                .weather(parseEnum(Weather.class, weather, Weather.SUNNY))
                .speed(Math.max(0, Math.min(200, speed)))
                .userMood(parseEnum(UserMood.class, userMood, UserMood.CALM))
                .timeOfDay(parseEnum(TimeOfDay.class, timeOfDay, TimeOfDay.MORNING))
                .passengerCount(Math.max(1, Math.min(7, passengerCount)))
                .routeType(parseEnum(RouteType.class, routeType, RouteType.URBAN))
                .build();

            // 更新状态存储
            VibeStatus currentStatus = statusStore.getOrInitial(sessionId);
            VibeStatus newStatus = VibeStatus.completed(
                sessionId,
                env.getSafetyMode(),
                currentStatus.currentPlan(),
                env
            );
            statusStore.put(sessionId, newStatus);

            // 推送环境变更事件
            sseEventPublisher.publish(sessionId, EnvironmentUpdateEvent.EVENT_TYPE,
                new EnvironmentUpdateEvent(env.gpsTag(), env.weather(), env.speed()));

            return "环境已设置：" + formatEnvironment(env);
        } catch (Exception e) {
            return "设置环境失败: " + e.getMessage();
        }
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

    private String formatEnvironment(Environment env) {
        return String.format(
            "%s, %s, %d km/h, %s, %s, %d人, %s路线",
            env.gpsTag(), env.weather(), (int) env.speed(),
            env.userMood(), env.timeOfDay(), env.passengerCount(), env.routeType()
        );
    }
}
