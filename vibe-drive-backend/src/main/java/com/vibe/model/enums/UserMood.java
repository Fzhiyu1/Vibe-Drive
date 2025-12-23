package com.vibe.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户情绪枚举
 * 作为氛围编排的核心输入之一
 */
public enum UserMood {
    HAPPY("happy", "愉快"),
    CALM("calm", "平静"),
    TIRED("tired", "疲劳"),
    STRESSED("stressed", "压力"),
    EXCITED("excited", "兴奋");

    private final String value;
    private final String displayName;

    UserMood(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static UserMood fromValue(String value) {
        for (UserMood mood : values()) {
            if (mood.value.equalsIgnoreCase(value)) {
                return mood;
            }
        }
        throw new IllegalArgumentException("Unknown UserMood: " + value);
    }

    /**
     * 判断是否需要舒缓氛围
     */
    public boolean needsSoothingAmbience() {
        return this == TIRED || this == STRESSED;
    }

    /**
     * 判断是否适合活力氛围
     */
    public boolean needsEnergeticAmbience() {
        return this == HAPPY || this == EXCITED;
    }
}
