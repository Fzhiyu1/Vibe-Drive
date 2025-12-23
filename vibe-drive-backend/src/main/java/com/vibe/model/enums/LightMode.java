package com.vibe.model.enums;

/**
 * 灯光模式枚举
 * 定义氛围灯的动态效果类型
 */
public enum LightMode {
    /**
     * 静态模式 - 固定颜色和亮度
     */
    STATIC("静态"),

    /**
     * 呼吸模式 - 亮度缓慢渐变（适合舒缓场景）
     */
    BREATHING("呼吸"),

    /**
     * 渐变模式 - 颜色缓慢过渡（适合风景路线）
     */
    GRADIENT("渐变"),

    /**
     * 脉冲模式 - 节奏性闪烁（适合动感音乐，仅 L1 模式）
     */
    PULSE("脉冲");

    private final String displayName;

    LightMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判断是否为动态效果（需要 L1 正常模式）
     */
    public boolean isDynamic() {
        return this == GRADIENT || this == PULSE;
    }

    /**
     * 判断是否适合舒缓场景
     */
    public boolean isSoothing() {
        return this == STATIC || this == BREATHING;
    }
}
