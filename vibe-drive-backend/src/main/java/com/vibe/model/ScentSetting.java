package com.vibe.model;

import com.vibe.model.enums.ScentType;
import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 香氛设置
 * 包含香氛类型、强度和持续时间
 */
@Description("香氛设置，包含香氛类型、强度和持续时间")
public record ScentSetting(
    @NotNull(message = "Scent type cannot be null")
    @Description("香氛类型：LAVENDER/PEPPERMINT/OCEAN/FOREST/CITRUS/VANILLA/NONE")
    ScentType type,

    @Min(value = 0, message = "Intensity must be at least 0")
    @Max(value = 10, message = "Intensity must be at most 10")
    @Description("香氛强度，范围 0-10，0 表示关闭")
    int intensity,

    @Min(value = 0, message = "Duration must be non-negative")
    @Description("持续时间，单位分钟，0 表示持续释放")
    int durationMinutes
) {
    /**
     * 默认持续时间（分钟）
     */
    public static final int DEFAULT_DURATION = 30;

    /**
     * 紧凑构造器：校验和默认值
     */
    public ScentSetting {
        if (intensity < 0 || intensity > 10) {
            throw new IllegalArgumentException("Intensity must be between 0 and 10");
        }
        if (durationMinutes < 0) {
            durationMinutes = DEFAULT_DURATION;
        }
    }

    /**
     * 判断是否关闭
     */
    public boolean isOff() {
        return type == ScentType.NONE || intensity == 0;
    }

    /**
     * 创建关闭状态
     */
    public static ScentSetting off() {
        return new ScentSetting(ScentType.NONE, 0, 0);
    }

    /**
     * 创建默认设置
     */
    public static ScentSetting defaultSetting(ScentType type) {
        return new ScentSetting(type, 5, DEFAULT_DURATION);
    }

    /**
     * 调整强度
     */
    public ScentSetting withIntensity(int newIntensity) {
        return new ScentSetting(type, newIntensity, durationMinutes);
    }
}
