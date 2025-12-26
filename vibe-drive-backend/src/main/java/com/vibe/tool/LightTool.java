package com.vibe.tool;

import com.vibe.model.LightColor;
import com.vibe.model.LightSetting;
import com.vibe.model.ZoneSetting;
import com.vibe.model.enums.LightMode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 灯光控制工具
 * AI 直接指定灯光参数
 */
@Component
public class LightTool {

    private static final int DEFAULT_TRANSITION_MS = 1500;

    @Tool("""
        设置车内氛围灯。

        技术约束：
        1. 使用高饱和度、高明度的颜色，避免暗沉
        2. 主色和副色应形成和谐对比（可以是相近色、互补色等）
        3. speed > 0 才有流光效果

        参数范围：
        - brightness: 0-100（夜间建议降低）
        - speed: 0.0-5.0（越大越快）
        - sharpness: 1.0-10.0（越大边缘越锐利）

        模式说明：
        - STATIC: 静态
        - BREATHING: 呼吸效果
        - GRADIENT: 流光效果
        - PULSE: 脉冲效果

        自由发挥：
        - 根据场景氛围自主选择颜色，不要形成固定映射
        - 同一场景可以有多种颜色方案，发挥创意
        """)
    public LightSetting setLight(
        @P("主色 HEX，如 #87CEEB") String color,
        @P("副色 HEX，用于流光效果，如 #ADD8E6") String colorB,
        @P("亮度 0-100") int brightness,
        @P("模式: STATIC/BREATHING/GRADIENT/PULSE") String mode,
        @P("流光速度 0.0-5.0，0为静止") Double speed,
        @P("流光锐度 1.0-10.0，越大边缘越锐利") Double sharpness
    ) {
        LightColor primaryColor = new LightColor(color, null);
        LightColor secondaryColor = colorB != null ? new LightColor(colorB, null) : null;
        LightMode lightMode = parseLightMode(mode);

        // 限制参数范围
        brightness = Math.max(0, Math.min(100, brightness));
        if (speed != null) speed = Math.max(0.0, Math.min(5.0, speed));
        if (sharpness != null) sharpness = Math.max(1.0, Math.min(10.0, sharpness));

        return new LightSetting(
            primaryColor,
            secondaryColor,
            brightness,
            lightMode,
            speed != null ? speed.floatValue() : null,
            sharpness != null ? sharpness.floatValue() : null,
            DEFAULT_TRANSITION_MS,
            createDefaultZones(color, brightness)
        );
    }

    private LightMode parseLightMode(String mode) {
        if (mode == null) return LightMode.STATIC;
        try {
            return LightMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LightMode.STATIC;
        }
    }

    private List<ZoneSetting> createDefaultZones(String color, int brightness) {
        return List.of(
            new ZoneSetting("dashboard", color, brightness),
            new ZoneSetting("door", color, Math.max(10, brightness - 10)),
            new ZoneSetting("footwell", color, Math.max(10, brightness - 20))
        );
    }
}
