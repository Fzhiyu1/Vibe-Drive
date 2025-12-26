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
        设置座椅按摩功能。

        按摩模式及特点：
        - relax: 放松模式，轻柔舒缓
        - energize: 活力模式，振奋精神
        - comfort: 舒适模式，均衡适中
        - sport: 运动模式，深度按摩
        - off: 关闭按摩

        按摩区域：
        - back: 背部
        - lumbar: 腰部
        - shoulder: 肩部
        - thigh: 大腿
        - all: 全部区域

        参数说明：
        - 强度 1-10，根据需要调节
        - 高速行驶时建议降低强度

        自由发挥：
        - 根据场景自主选择，不要形成固定映射
        - 可以组合不同区域
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
