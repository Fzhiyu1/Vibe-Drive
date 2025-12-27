package com.vibe.context;

/**
 * 会话上下文
 * 使用 ThreadLocal 存储当前会话 ID，供 Tool 访问
 */
public class SessionContext {

    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    public static String getSessionId() {
        return currentSessionId.get();
    }

    public static void clear() {
        currentSessionId.remove();
    }
}
