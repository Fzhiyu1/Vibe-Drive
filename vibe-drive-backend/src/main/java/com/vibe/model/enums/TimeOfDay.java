package com.vibe.model.enums;

/**
 * 时段枚举
 * 根据当前时间（小时）判断时段，影响氛围基调
 */
public enum TimeOfDay {
    DAWN("黎明", 5, 6),         // 5:00-6:59
    MORNING("早晨", 7, 9),      // 7:00-9:59
    NOON("正午", 10, 13),       // 10:00-13:59
    AFTERNOON("下午", 14, 17),  // 14:00-17:59
    EVENING("傍晚", 18, 19),    // 18:00-19:59
    NIGHT("夜晚", 20, 22),      // 20:00-22:59
    MIDNIGHT("深夜", 23, 4);    // 23:00-4:59

    private final String displayName;
    private final int startHour;
    private final int endHour;

    TimeOfDay(String displayName, int startHour, int endHour) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
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

    /**
     * 根据小时数判断时段
     * @param hour 小时数��0-23）
     * @return 对应的时段
     * @throws IllegalArgumentException 如果小时数不在 0-23 范围内
     */
    public static TimeOfDay fromHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23, got: " + hour);
        }

        for (TimeOfDay timeOfDay : values()) {
            if (timeOfDay == MIDNIGHT) {
                // 深夜跨越0点：23:00-4:59
                if (hour >= timeOfDay.startHour || hour <= timeOfDay.endHour) {
                    return timeOfDay;
                }
            } else {
                // 其他时段：startHour <= hour <= endHour
                if (hour >= timeOfDay.startHour && hour <= timeOfDay.endHour) {
                    return timeOfDay;
                }
            }
        }

        // 理论上不会到达这里
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
