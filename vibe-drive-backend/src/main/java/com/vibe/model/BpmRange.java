package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;

/**
 * BPM（节拍速度）范围
 * 用于音乐推荐时指定节拍速度的范围
 */
@Description("BPM（节拍速度）范围")
public record BpmRange(
    @Min(value = 0, message = "Min BPM must be non-negative")
    @Description("最小BPM值")
    int min,

    @Min(value = 0, message = "Max BPM must be non-negative")
    @Description("最大BPM值")
    int max
) {
    /**
     * 紧凑构造器：校验参数
     */
    public BpmRange {
        if (min < 0) {
            throw new IllegalArgumentException("Min BPM must be non-negative");
        }
        if (max < min) {
            throw new IllegalArgumentException("Max BPM must be greater than or equal to min BPM");
        }
    }

    /**
     * 创建常见的 BPM 范围
     */
    public static BpmRange slow() {
        return new BpmRange(60, 80);
    }

    public static BpmRange moderate() {
        return new BpmRange(80, 120);
    }

    public static BpmRange fast() {
        return new BpmRange(120, 160);
    }

    /**
     * 判断给定 BPM 是否在范围内
     */
    public boolean contains(int bpm) {
        return bpm >= min && bpm <= max;
    }
}
