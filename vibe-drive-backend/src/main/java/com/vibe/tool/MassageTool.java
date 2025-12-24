package com.vibe.tool;

import com.vibe.model.MassageSetting;
import com.vibe.model.enums.MassageMode;
import com.vibe.model.enums.MassageZone;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 按摩座椅控制工具
 * 根据驾驶员状态和场景设置座椅按摩
 */
@Component
public class MassageTool {

    @Tool("""
        设置座椅按摩功能。根据驾驶员疲劳程度和驾驶场景选择按摩模式。
        - 长途驾驶/疲劳时推荐relax放松模式
        - 需要提神时推荐energize活力模式
        - 日常使用推荐comfort舒适模式
        - 运动后推荐sport运动模式深度按摩
        - 高速行驶时应降低强度或关闭
        - 强度1-10，建议日常3-5
        """)
    public MassageSetting setMassage(
        @P("按摩模式: relax/energize/comfort/sport/off") String mode,
        @P("按摩区域，逗号分隔: back/lumbar/shoulder/thigh/all") String zones,
        @P("强度 1-10，0表示关闭") int intensity
    ) {
        MassageMode massageMode = MassageMode.fromValue(mode);
        List<MassageZone> zoneList = parseZones(zones);
        return new MassageSetting(massageMode, zoneList, intensity);
    }

    private List<MassageZone> parseZones(String zones) {
        if (zones == null || zones.isBlank()) {
            return List.of(MassageZone.ALL);
        }
        return Arrays.stream(zones.split(","))
            .map(String::trim)
            .map(MassageZone::fromValue)
            .toList();
    }
}
