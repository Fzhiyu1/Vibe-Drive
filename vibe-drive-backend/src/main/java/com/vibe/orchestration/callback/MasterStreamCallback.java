package com.vibe.orchestration.callback;

/**
 * 主智能体流式回调接口
 */
public interface MasterStreamCallback {

    /**
     * 文本增量
     */
    void onTextDelta(String text);

    /**
     * 工具开始执行
     */
    void onToolStart(String toolName, Object toolInput);

    /**
     * 工具执行完成
     */
    void onToolComplete(String toolName, String result);

    /**
     * 对话完成
     */
    void onComplete();

    /**
     * 错误
     */
    void onError(Throwable error);
}
