package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 驾驶员生理数据
 * 包含心率、压力、疲劳等健康指标
 */
@Description("驾驶员生理数据，包含心率、压力、疲劳等健康指标")
public record DriverBiometrics(
    @Min(value = 40, message = "Heart rate too low")
    @Max(value = 200, message = "Heart rate too high")
    @Description("心率，单位 bpm，正常范围 60-100")
    int heartRate,

    @Min(value = 0, message = "Stress level must be at least 0")
    @Max(value = 1, message = "Stress level must be at most 1")
    @Description("压力指数，范围 0-1，0.7 以上为高压力")
    double stressLevel,

    @Min(value = 0, message = "Fatigue level must be at least 0")
    @Max(value = 1, message = "Fatigue level must be at most 1")
    @Description("疲劳指数，范围 0-1，0.6 以上需要休息")
    double fatigueLevel,

    @Min(value = 35, message = "Body temperature too low")
    @Max(value = 42, message = "Body temperature too high")
    @Description("体温，单位摄氏度，正常范围 36-37.5")
    double bodyTemperature
) {
    /**
     * 判断是否高压力状态
     */
    public boolean isHighStress() {
        return stressLevel >= 0.7;
    }

    /**
     * 判断是否疲劳状态
     */
    public boolean isFatigued() {
        return fatigueLevel >= 0.6;
    }

    /**
     * 判断心率是否正常
     */
    public boolean isHeartRateNormal() {
        return heartRate >= 60 && heartRate <= 100;
    }

    /**
     * 判断是否需要休息提醒
     */
    public boolean needsRestReminder() {
        return isFatigued() || fatigueLevel >= 0.5 && isHighStress();
    }

    /**
     * 判断是否需要舒缓氛围
     */
    public boolean needsSoothingAmbience() {
        return isHighStress() || heartRate > 100;
    }

    /**
     * 创建正常状态
     */
    public static DriverBiometrics normal() {
        return new DriverBiometrics(75, 0.3, 0.2, 36.5);
    }
}
