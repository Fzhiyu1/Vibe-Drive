package com.vibe.tool;

import com.vibe.model.MusicRecommendation;
import com.vibe.model.PlayResult;
import com.vibe.model.SearchResult;
import com.vibe.service.MusicService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 音乐工具
 * 支持搜索和播放音乐（调用 Go 微服务）
 */
@Component
public class MusicTool {

    private final MusicService musicService;

    public MusicTool(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * 推荐音乐（旧方法，使用本地 Mock 数据）
     * 已移除 @Tool 注解，AI 不再调用此方法
     *
     * @deprecated 请使用 {@link #searchMusic(String)} 和 {@link #playMusic(String)} 方法
     */
    @Deprecated
    public MusicRecommendation recommendMusic(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("乘客数量: 1-7") int passengerCount,
        @P("偏好流派，可选: pop/rock/jazz/classical/folk/electronic/ambient") String genre
    ) {
        return musicService.recommend(mood, timeOfDay, passengerCount, genre);
    }

    // ==================== 新方法：调用 Go 微服务 ====================

    /**
     * 搜索音乐
     */
    @Tool("""
        搜索音乐。返回候选列表供选择。
        根据播放量、歌手、时长等信息选择合适的歌曲。
        - fee=0 或 fee=8 可以播放
        - fee=1 需要 VIP，暂不支持
        - 时长太短（<60秒）可能不是正经音乐
        """)
    public SearchResult searchMusic(
        @P("搜索关键词") String keyword
    ) {
        return musicService.search(keyword);
    }

    /**
     * 播放指定歌曲
     */
    @Tool("播放指定歌曲。传入歌曲 ID，返回播放信息。")
    public PlayResult playMusic(
        @P("歌曲 ID") String id
    ) {
        return musicService.play(id);
    }
}
