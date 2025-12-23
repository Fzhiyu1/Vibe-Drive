package com.vibe.simulator;

/**
 * 场景类型
 */
public enum ScenarioType {
    LATE_NIGHT_RETURN("深夜归途"),
    WEEKEND_FAMILY_TRIP("周末家庭出游"),
    MORNING_COMMUTE("通勤早高峰"),
    RANDOM("随机场景");

    private final String description;

    ScenarioType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
