package com.vibe.model.enums;

/**
 * 用户情绪枚举
 * 作为氛围编排的核心输入之一
 */
public enum UserMood {
    HAPPY("愉快"),
    CALM("平静"),
    TIRED("疲劳"),
    STRESSED("压力"),
    EXCITED("兴奋");

    private final String displayName;

    UserMood(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
