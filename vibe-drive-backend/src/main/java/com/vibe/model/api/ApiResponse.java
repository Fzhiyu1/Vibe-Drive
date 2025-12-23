package com.vibe.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * 统一 API 响应包装
 * 所有 REST API 响应都使用此格式
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    Instant timestamp
) {
    /**
     * 紧凑构造器：设置默认时间戳
     */
    public ApiResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    /**
     * 创建错误响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message), Instant.now());
    }

    /**
     * 创建错误响应（带详情）
     */
    public static <T> ApiResponse<T> error(String code, String message, String details) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message, details), Instant.now());
    }

    /**
     * 错误信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorInfo(
        String code,
        String message,
        String details
    ) {
        public ErrorInfo(String code, String message) {
            this(code, message, null);
        }
    }

    /**
     * 常用错误码
     */
    public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERROR_INVALID_ENVIRONMENT = "INVALID_ENVIRONMENT";
    public static final String ERROR_AGENT_NOT_RUNNING = "AGENT_NOT_RUNNING";
    public static final String ERROR_AGENT_BUSY = "AGENT_BUSY";
    public static final String ERROR_LLM_ERROR = "LLM_ERROR";
    public static final String ERROR_LLM_TIMEOUT = "LLM_TIMEOUT";
    public static final String ERROR_INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * 便捷方法：创建无效请求错误
     */
    public static <T> ApiResponse<T> invalidRequest(String message) {
        return error(ERROR_INVALID_REQUEST, message);
    }

    /**
     * 便捷方法：创建无效环境数据错误
     */
    public static <T> ApiResponse<T> invalidEnvironment(String message) {
        return error(ERROR_INVALID_ENVIRONMENT, message);
    }

    /**
     * 便捷方法：创建 Agent 未运行错误
     */
    public static <T> ApiResponse<T> agentNotRunning() {
        return error(ERROR_AGENT_NOT_RUNNING, "Vibe Agent 未运行");
    }

    /**
     * 便捷方法：创建 LLM 错误
     */
    public static <T> ApiResponse<T> llmError(String message) {
        return error(ERROR_LLM_ERROR, message);
    }

    /**
     * 便捷方法：创建内部错误
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return error(ERROR_INTERNAL_ERROR, message);
    }
}
