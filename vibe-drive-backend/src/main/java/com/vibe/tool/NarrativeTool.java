package com.vibe.tool;

import com.vibe.model.Narrative;
import com.vibe.service.NarrativeService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 叙事生成工具
 * 生成 TTS 播报文本，将环境与音乐进行"时空编织"
 */
@Component
public class NarrativeTool {

    private final NarrativeService narrativeService;

    public NarrativeTool(NarrativeService narrativeService) {
        this.narrativeService = narrativeService;
    }

    /**
     * 生成叙事文本
     *
     * @param timeOfDay   时段
     * @param weather     天气
     * @param gpsTag      位置标签
     * @param userMood    用户情绪
     * @param currentSong 当前歌曲（可选）
     * @param theme       叙事主题（可选）
     * @return 叙事文本
     */
    @Tool("""
        生成 TTS 播报文本，将窗外风景与音乐进行"时空编织"。
        - 文本应简短温馨，不超过 50 字
        - 结合当前环境（天气、时段、位置）
        - 如有正在播放的歌曲，可以关联歌曲意境
        - 语气应符合当前氛围（深夜轻柔，早晨活力）
        """)
    public Narrative generateNarrative(
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("天气: sunny/cloudy/rainy/snowy/foggy") String weather,
        @P("位置标签: highway/urban/suburban/rural/coastal/mountain") String gpsTag,
        @P("用户情绪: happy/calm/tired/stressed/excited") String userMood,
        @P("当前歌曲名称，可选") String currentSong,
        @P("叙事主题，可选: comfort/energy/romance/adventure") String theme
    ) {
        return narrativeService.generate(timeOfDay, weather, gpsTag, userMood, currentSong, theme);
    }
}
