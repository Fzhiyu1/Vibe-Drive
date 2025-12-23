package com.vibe.tool;

import com.vibe.model.LightSetting;
import com.vibe.service.LightService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 灯光控制工具
 * 根据情绪、时段、天气设置氛围灯
 */
@Component
public class LightTool {

    private final LightService lightService;

    public LightTool(LightService lightService) {
        this.lightService = lightService;
    }

    /**
     * 设置灯光
     *
     * @param mood      目标情绪
     * @param timeOfDay 时段
     * @param weather   天气
     * @return 灯光设置
     */
    @Tool("""
        根据情绪、时段和天气设置车内氛围灯。
        - 深夜/疲劳时使用暖色调低亮度
        - 晴天/开心时可使用明亮活力的颜色
        - 雨天/压力时使用柔和舒缓的颜色
        - 高速行驶时（L2/L3模式）禁用动态效果
        """)
    public LightSetting setLight(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("天气: sunny/cloudy/rainy/snowy/foggy") String weather
    ) {
        return lightService.calculateSetting(mood, timeOfDay, weather);
    }
}
