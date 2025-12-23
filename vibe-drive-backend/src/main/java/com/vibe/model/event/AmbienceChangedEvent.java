package com.vibe.model.event;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.LightSetting;
import com.vibe.model.MusicRecommendation;
import com.vibe.model.Narrative;
import com.vibe.model.enums.SafetyMode;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;

import java.time.Instant;

/**
 * 氛围方案变化事件
 */
@Description("氛围方案变化事件")
public record AmbienceChangedEvent(
    @Description("方案ID")
    String planId,

    @Valid
    @Description("音乐推荐")
    MusicRecommendation music,

    @Valid
    @Description("灯光设置")
    LightSetting light,

    @Valid
    @Description("叙事文本")
    Narrative narrative,

    @Description("当前安全模式")
    SafetyMode safetyMode,

    @Description("触发原因：environment_change/user_request/scheduled")
    String trigger,

    @Description("事件时间戳")
    Instant timestamp
) {
    /**
     * 触发原因常量
     */
    public static final String TRIGGER_ENVIRONMENT_CHANGE = "environment_change";
    public static final String TRIGGER_USER_REQUEST = "user_request";
    public static final String TRIGGER_SCHEDULED = "scheduled";

    /**
     * 从 AmbiencePlan 创建事件
     */
    public AmbienceChangedEvent(AmbiencePlan plan, String trigger) {
        this(
            plan.id(),
            plan.music(),
            plan.light(),
            plan.narrative(),
            plan.safetyMode(),
            trigger,
            Instant.now()
        );
    }

    /**
     * 紧凑构造器：设置默认时间戳
     */
    public AmbienceChangedEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 创建环境变化触发的事件
     */
    public static AmbienceChangedEvent fromEnvironmentChange(AmbiencePlan plan) {
        return new AmbienceChangedEvent(plan, TRIGGER_ENVIRONMENT_CHANGE);
    }

    /**
     * 创建用户请求触发的事件
     */
    public static AmbienceChangedEvent fromUserRequest(AmbiencePlan plan) {
        return new AmbienceChangedEvent(plan, TRIGGER_USER_REQUEST);
    }

    /**
     * 创建定时触发的事件
     */
    public static AmbienceChangedEvent fromScheduled(AmbiencePlan plan) {
        return new AmbienceChangedEvent(plan, TRIGGER_SCHEDULED);
    }

    /**
     * SSE 事件类型名称
     */
    public static final String EVENT_TYPE = "ambience_changed";
}
