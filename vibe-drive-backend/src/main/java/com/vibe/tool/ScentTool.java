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
        设置车内香氛系统。根据场景和用户状态选择合适的香氛类型和强度。
        - 疲劳/压力大时推荐薰衣草(lavender)放松
        - 需要提神时推荐薄荷(peppermint)或柑橘(citrus)
        - 海边/山路推荐海洋(ocean)或森林(forest)
        - 温馨场景推荐香草(vanilla)
        - 强度1-10，建议日常使用3-5，特殊场景可调高
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
