package com.vibe.model.api;

import com.vibe.model.AmbiencePlan;
import com.vibe.model.Environment;
import jakarta.validation.constraints.NotNull;

/**
 * 触发氛围编排请求
 */
public record TriggerRequest(
    @NotNull Environment environment,
    AmbiencePlan currentPlan,
    String changeDescription,
    Integer currentPlaylistIndex
) {
    /**
     * 是否为增量调整请求
     */
    public boolean isAdjustment() {
        return changeDescription != null && !changeDescription.isBlank();
    }
}
