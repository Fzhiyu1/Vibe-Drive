package com.vibe.orchestration.callback;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.enums.SafetyMode;
import com.vibe.orchestration.dto.VibeLoopState;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * 流式回调接口
 * 用于处理 Agent 编排过程中的各种事件
 */
public interface VibeStreamCallback {

    /**
     * 文本增量
     * @param text 增量文本
     */
    void onTextDelta(String text);

    /**
     * 工具开始执行
     * @param toolName 工具名称
     * @param toolInput 工具输入参数
     */
    void onToolStart(String toolName, Object toolInput);

    /**
     * 工具执行完成
     * @param toolName 工具名称
     * @param result 执行结果
     */
    void onToolComplete(String toolName, String result);

    /**
     * 工具执行错误
     * @param toolName 工具名称
     * @param error 错误信息
     */
    void onToolError(String toolName, Throwable error);

    /**
     * 对话完成
     * @param plan 氛围方案
     * @param response 完整响应
     */
    void onComplete(AmbiencePlan plan, ChatResponse response);

    /**
     * 错误
     * @param error 错误信息
     */
    void onError(Throwable error);

    /**
     * 安全模式已应用
     * @param mode 安全模式
     */
    void onSafetyModeApplied(SafetyMode mode);

    /**
     * 循环状态更新（可用于异步结果收集/调试）
     * @param state 当前循环状态
     */
    default void onStateUpdate(VibeLoopState state) {}

    /**
     * 递归深度更新
     * @param depth 当前深度
     */
    default void onDepthUpdate(int depth) {}

    /**
     * 警告消息
     * @param message 警告内容
     */
    default void onWarning(String message) {}
}
