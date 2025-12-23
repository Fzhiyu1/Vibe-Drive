package com.vibe.model.agent;

import com.vibe.model.enums.SafetyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Vibe 命令
 * 使用 Sealed Interface 实现类型安全的命令模式
 */
public sealed interface VibeCommand permits
    VibeCommand.Start,
    VibeCommand.Stop,
    VibeCommand.SetSafetyMode,
    VibeCommand.ForceRefresh,
    VibeCommand.SetPreference {

    /**
     * 启动 Agent 命令
     */
    record Start() implements VibeCommand {
        @Override
        public String description() {
            return "启动 Vibe Agent";
        }
    }

    /**
     * 停止 Agent 命令
     */
    record Stop() implements VibeCommand {
        @Override
        public String description() {
            return "停止 Vibe Agent";
        }
    }

    /**
     * 设置安全模式命令
     */
    record SetSafetyMode(
        @NotNull(message = "Safety mode cannot be null")
        SafetyMode mode
    ) implements VibeCommand {
        public SetSafetyMode {
            if (mode == null) {
                throw new IllegalArgumentException("Safety mode cannot be null");
            }
        }

        @Override
        public String description() {
            return "设置安全模式为 " + mode.getDisplayName();
        }
    }

    /**
     * 强制刷新氛围命令
     */
    record ForceRefresh() implements VibeCommand {
        @Override
        public String description() {
            return "强制刷新氛围";
        }
    }

    /**
     * 设置偏好命令
     */
    record SetPreference(
        @NotBlank(message = "Preference key cannot be empty")
        String key,
        Object value
    ) implements VibeCommand {
        public SetPreference {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Preference key cannot be empty");
            }
        }

        @Override
        public String description() {
            return "设置偏好 " + key + " = " + value;
        }
    }

    /**
     * 获取命令描述
     */
    String description();

    /**
     * 判断是否为生命周期命令
     */
    default boolean isLifecycleCommand() {
        return this instanceof Start || this instanceof Stop;
    }

    /**
     * 判断是否为配置命令
     */
    default boolean isConfigCommand() {
        return this instanceof SetSafetyMode || this instanceof SetPreference;
    }
}
