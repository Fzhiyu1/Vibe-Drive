package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 时段枚举
 * 根据当前时间（小时）判断时段，影响氛围基调
 */
public enum TimeOfDay {
    DAWN("dawn", "黎明", 5, 6),
    MORNING("morning", "早晨", 7, 9),
    NOON("noon", "正午", 10, 13),
    AFTERNOON("afternoon", "下午", 14, 17),
    EVENING("evening", "傍晚", 18, 19),
    NIGHT("night", "夜晚", 20, 22),
    MIDNIGHT("midnight", "深夜", 23, 4);

    private final String value;
    private final String displayName;
    private final int startHour;
    private final int endHour;

    TimeOfDay(String value, String displayName, int startHour, int endHour) {
        this.value = value;
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    @JsonCreator
    public static TimeOfDay fromValue(String value) {
        for (TimeOfDay tod : values()) {
            if (tod.value.equalsIgnoreCase(value)) {
                return tod;
            }
        }
        throw new IllegalArgumentException("Unknown TimeOfDay: " + value);
    }

    /**
     * 根据小时数判断时段
     */
    public static TimeOfDay fromHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23, got: " + hour);
        }

        for (TimeOfDay timeOfDay : values()) {
            if (timeOfDay == MIDNIGHT) {
                if (hour >= timeOfDay.startHour || hour <= timeOfDay.endHour) {
                    return timeOfDay;
                }
            } else {
                if (hour >= timeOfDay.startHour && hour <= timeOfDay.endHour) {
                    return timeOfDay;
                }
            }
        }
        throw new IllegalStateException("Unable to determine time of day for hour: " + hour);
    }

    /**
     * 判断是否为深夜时段（需要更柔和的氛围）
     */
    public boolean isLateNight() {
        return this == NIGHT || this == MIDNIGHT;
    }

    /**
     * 判断是否为日间时段
     */
    public boolean isDaytime() {
        return this == MORNING || this == NOON || this == AFTERNOON;
    }
}
