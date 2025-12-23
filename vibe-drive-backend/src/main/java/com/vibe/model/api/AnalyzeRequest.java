package com.vibe.model.api;

import com.vibe.model.Environment;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 分析请求
 * 用于向 Vibe Agent 提交环境数据进行氛围分析
 */
@Description("分析请求")
public record AnalyzeRequest(
    @NotBlank(message = "Session ID cannot be empty")
    @Description("会话ID（对应 LangChain4j @MemoryId），用于隔离多用户/多车辆上下文")
    String sessionId,

    @NotNull(message = "Environment cannot be null")
    @Valid
    @Description("环境数据")
    Environment environment,

    @Description("用户偏好（可选），如 musicGenre、narrativeEnabled 等")
    Map<String, Object> preferences,

    @Description("是否异步处理（默认 false）")
    boolean async
) {
    /**
     * 紧凑构造器：校验参数
     */
    public AnalyzeRequest {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be empty");
        }
        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }
        // 确保 preferences 不可变
        if (preferences != null) {
            preferences = Map.copyOf(preferences);
        }
    }

    /**
     * 简化构造：仅提供必要参数
     */
    public static AnalyzeRequest of(String sessionId, Environment environment) {
        return new AnalyzeRequest(sessionId, environment, null, false);
    }

    /**
     * 简化构造：带偏好
     */
    public static AnalyzeRequest of(String sessionId, Environment environment, Map<String, Object> preferences) {
        return new AnalyzeRequest(sessionId, environment, preferences, false);
    }

    /**
     * 判断是否有用户偏好
     */
    public boolean hasPreferences() {
        return preferences != null && !preferences.isEmpty();
    }

    /**
     * 获取偏好值
     */
    @SuppressWarnings("unchecked")
    public <T> T getPreference(String key, T defaultValue) {
        if (preferences == null) {
            return defaultValue;
        }
        Object value = preferences.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
}
