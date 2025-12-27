package com.vibe.orchestration.dto;

import com.vibe.model.AmbiencePlan;

import java.time.Instant;

/**
 * 氛围任务消息类型
 * 用于异步任务完成后通知主智能体
 */
public sealed interface VibeMessage {

    /**
     * 氛围编排成功
     */
    record Success(
        String taskId,
        AmbiencePlan plan,
        Instant timestamp
    ) implements VibeMessage {}

    /**
     * 氛围编排失败
     */
    record Failed(
        String taskId,
        String error,
        Instant timestamp
    ) implements VibeMessage {}

    /**
     * 氛围编排被取消
     */
    record Cancelled(
        String taskId,
        Instant timestamp
    ) implements VibeMessage {}
}
