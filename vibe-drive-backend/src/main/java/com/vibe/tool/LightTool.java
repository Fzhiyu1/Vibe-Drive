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
        设置车内氛围灯。必须设置所有参数！

        重要规则：
        1. 必须使用高饱和度、高明度的颜色！禁止使用暗色
        2. 必须设置 colorB（副色），与主色形成对比
        3. 必须设置 speed > 0 实现流光效果

        推荐颜色组合（主色 + 副色）：
        - 放松/夜晚：#FF8C00 + #FFD700（橙金渐变）
        - 平静/专注：#00BFFF + #00CED1（蓝青渐变）
        - 活力/开心：#FF69B4 + #FFD700（粉金渐变）
        - 自然/森林：#00FF7F + #7CFC00（绿色渐变）
        - 神秘/夜空：#9370DB + #BA55D3（紫色渐变）

        参数建议：
        - brightness: 60-80（夜间40-60）
        - speed: 1.0-2.0（放松），2.0-3.0（活力），3.0-5.0（动感）
        - sharpness: 1.0-3.0（柔和），3.0-6.0（清晰），6.0-10.0（锐利）

        模式说明：
        - BREATHING: 呼吸效果，speed 建议 1.0-2.0
        - GRADIENT: 流光效果，speed 建议 2.0-4.0
        - PULSE: 脉冲效果，speed 建议 3.0-5.0
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
