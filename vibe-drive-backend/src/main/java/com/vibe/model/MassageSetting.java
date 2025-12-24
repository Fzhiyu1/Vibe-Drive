package com.vibe.model;

import com.vibe.model.enums.MassageMode;
import com.vibe.model.enums.MassageZone;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 按摩设置
 * 包含按摩模式、区域和强度
 */
@Description("按摩设置，包含按摩模式、区域和强度")
public record MassageSetting(
    @NotNull(message = "Massage mode cannot be null")
    @Description("按摩模式：RELAX/ENERGIZE/COMFORT/SPORT/OFF")
    MassageMode mode,

    @Description("按摩区域列表：BACK/LUMBAR/SHOULDER/THIGH/ALL")
    List<MassageZone> zones,

    @Min(value = 0, message = "Intensity must be at least 0")
    @Max(value = 10, message = "Intensity must be at most 10")
    @Description("按摩强度，范围 0-10，0 表示关闭")
    int intensity
) {
    /**
     * 紧凑构造器：校验和默认值
     */
    public MassageSetting {
        if (intensity < 0 || intensity > 10) {
            throw new IllegalArgumentException("Intensity must be between 0 and 10");
        }
        if (zones == null || zones.isEmpty()) {
            zones = List.of(MassageZone.ALL);
        } else {
            zones = List.copyOf(zones);
        }
    }

    /**
     * 判断是否关闭
     */
    public boolean isOff() {
        return mode == MassageMode.OFF || intensity == 0;
    }

    /**
     * 创建关闭状态
     */
    public static MassageSetting off() {
        return new MassageSetting(MassageMode.OFF, List.of(), 0);
    }

    /**
     * 创建默认设置
     */
    public static MassageSetting defaultSetting(MassageMode mode) {
        return new MassageSetting(mode, List.of(MassageZone.ALL), 5);
    }

    /**
     * 调整强度
     */
    public MassageSetting withIntensity(int newIntensity) {
        return new MassageSetting(mode, zones, newIntensity);
    }

    /**
     * 为高速模式过滤（强制使用安全模式）
     */
    public MassageSetting forHighSpeed() {
        if (!mode.isSafeForHighSpeed()) {
            return new MassageSetting(MassageMode.COMFORT, zones, Math.min(intensity, 3));
        }
        return this;
    }
}
