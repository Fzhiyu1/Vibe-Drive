package com.vibe.tool;

import com.vibe.model.MusicRecommendation;
import com.vibe.service.MusicService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 音乐推荐工具
 * 根据情绪、时段、乘客数量推荐合适的音乐
 */
@Component
public class MusicTool {

    private final MusicService musicService;

    public MusicTool(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * 推荐音乐
     *
     * @param mood           目标情绪
     * @param timeOfDay      时段
     * @param passengerCount 乘客数量
     * @param genre          偏好流派（可选）
     * @return 音乐推荐结果
     */
    @Tool("""
        根据用户情绪、时段和乘客数量推荐合适的音乐。
        - 独自驾驶时推荐个人化音乐
        - 多人乘坐时推荐大众化、欢快的音乐
        - 疲劳时推荐舒缓音乐
        - 深夜时避免过于激烈的音乐
        """)
    public MusicRecommendation recommendMusic(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("乘客数量: 1-7") int passengerCount,
        @P("偏好流派，可选: pop/rock/jazz/classical/folk/electronic/ambient") String genre
    ) {
        return musicService.recommend(mood, timeOfDay, passengerCount, genre);
    }
}
