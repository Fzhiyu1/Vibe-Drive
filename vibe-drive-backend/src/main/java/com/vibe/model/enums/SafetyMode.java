package com.vibe.model.enums;

/**
 * 安全模式枚举
 * 根据车速自动判断，限制氛围编排的激进程度
 */
public enum SafetyMode {
    /**
     * L1 正常模式（车速 < 60 km/h）
     * - 全功能开放
     * - 支持语音交互、视觉动效、主动推荐
     */
    L1_NORMAL("正常模式", 0, 60),

    /**
     * L2 专注模式（车速 60-100 km/h）
     * - 禁用视觉动态效果（灯光仅静态/呼吸）
     * - 降低主动推荐频率
     * - 语音交互正常
     */
    L2_FOCUS("专注模式", 60, 100),

    /**
     * L3 静默模式（车速 >= 100 km/h）
     * - 禁用所有主动推荐
     * - TTS 音量降低 30%
     * - 禁用视觉动效
     * - 仅响应明确指令
     */
    L3_SILENT("静默模式", 100, 200);

    private final String displayName;
    private final int minSpeed;  // 最小车速（含）
    private final int maxSpeed;  // 最大车速（不含）

    SafetyMode(String displayName, int minSpeed, int maxSpeed) {
        this.displayName = displayName;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinSpeed() {
        return minSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * 根据车速判断安全模式
     * @param speed 车速（km/h）
     * @return 对应的安全模式
     * @throws IllegalArgumentException 如果车速为负数或超过200
     */
    public static SafetyMode fromSpeed(double speed) {
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Speed must be between 0 and 200 km/h, got: " + speed);
        }

        if (speed < 60) {
            return L1_NORMAL;
        } else if (speed < 100) {
            return L2_FOCUS;
        } else {
            return L3_SILENT;
        }
    }

    /**
     * 判断是否允许动态灯光效果
     */
    public boolean allowsDynamicLighting() {
        return this == L1_NORMAL;
    }

    /**
     * 判断是否允许主动推荐
     */
    public boolean allowsProactiveRecommendation() {
        return this == L1_NORMAL || this == L2_FOCUS;
    }

    /**
     * 获取 TTS 音量系数
     */
    public double getTtsVolumeMultiplier() {
        return switch (this) {
            case L1_NORMAL, L2_FOCUS -> 1.0;
            case L3_SILENT -> 0.7;  // 降低 30%
        };
    }
}
