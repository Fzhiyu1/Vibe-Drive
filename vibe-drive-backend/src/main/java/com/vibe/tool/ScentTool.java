package com.vibe.tool;

import com.vibe.model.ScentSetting;
import com.vibe.model.enums.ScentType;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 香氛控制工具
 * 根据场景和用户状态设置车内香氛
 */
@Component
public class ScentTool {

    @Tool("""
        设置车内香氛系统。

        香氛类型及特点：
        - lavender: 薰衣草，舒缓放松
        - peppermint: 薄荷，清新提神
        - ocean: 海洋，清爽自然
        - forest: 森林，木质清新
        - citrus: 柑橘，活力清新
        - vanilla: 香草，温暖甜美
        - none: 关闭香氛

        参数说明：
        - 强度 1-10，根据需要调节
        - 持续时间 0 表示持续释放

        自由发挥：
        - 根据场景自主选择，不要形成固定映射
        - 同一场景可以尝试不同香氛
        """)
    public ScentSetting setScent(
        @P("香氛类型: lavender/peppermint/ocean/forest/citrus/vanilla/none") String type,
        @P("强度 1-10，0表示关闭") int intensity,
        @P("持续时间(分钟)，0表示持续释放") int durationMinutes
    ) {
        ScentType scentType = ScentType.fromValue(type);
        return new ScentSetting(scentType, intensity, durationMinutes);
    }
}
