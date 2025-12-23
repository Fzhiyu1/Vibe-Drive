package com.vibe.model.event;

import com.vibe.model.enums.GpsTag;
import com.vibe.model.enums.Weather;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * 环境数据更新事件
 * 通常来自模拟器或车端采集
 */
@Description("环境数据更新事件（通常来自模拟器或车端采集）")
public record EnvironmentUpdateEvent(
    @Description("位置标签")
    GpsTag gpsTag,

    @Description("天气")
    Weather weather,

    @Min(value = 0, message = "Speed must be at least 0")
    @Max(value = 200, message = "Speed must be at most 200 km/h")
    @Description("车速（km/h）")
    double speed,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 简化构造：自动设置时间戳
     */
    public EnvironmentUpdateEvent(GpsTag gpsTag, Weather weather, double speed) {
        this(gpsTag, weather, speed, Instant.now());
    }

    /**
     * 紧凑构造器：校验和默认值
     */
    public EnvironmentUpdateEvent {
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Speed must be between 0 and 200 km/h");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 获取简要描述
     */
    public String getSummary() {
        return String.format("%s, %s, %.0f km/h",
            gpsTag != null ? gpsTag.getDisplayName() : "未知位置",
            weather != null ? weather.getDisplayName() : "未知天气",
            speed);
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "environment_update";
}
