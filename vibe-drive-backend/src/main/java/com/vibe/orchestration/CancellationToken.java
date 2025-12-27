package com.vibe.orchestration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 取消令牌
 * 用于在异步任务中传递取消状态
 */
public class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 请求取消
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 如果已取消，抛出异常
     */
    public void throwIfCancelled() throws CancelledException {
        if (cancelled.get()) {
            throw new CancelledException("Task was cancelled");
        }
    }

    /**
     * 取消异常
     */
    public static class CancelledException extends RuntimeException {
        public CancelledException(String message) {
            super(message);
        }
    }
}
