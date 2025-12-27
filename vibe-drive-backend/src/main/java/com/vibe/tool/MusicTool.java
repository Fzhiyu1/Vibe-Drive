package com.vibe.tool;

import com.vibe.model.BatchSearchResult;
import com.vibe.model.MusicRecommendation;
import com.vibe.model.PlayResult;
import com.vibe.model.Playlist;
import com.vibe.model.SearchResult;
import com.vibe.service.MusicService;
import com.vibe.service.PlaylistService;
import com.vibe.context.SessionContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 音乐工具
 * 支持搜索和播放音乐（调用 Go 微服务）
 */
@Component
public class MusicTool {

    private final MusicService musicService;
    private final PlaylistService playlistService;

    public MusicTool(MusicService musicService, PlaylistService playlistService) {
        this.musicService = musicService;
        this.playlistService = playlistService;
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

    // ==================== 批量方法 ====================

    /**
     * 批量搜索音乐
     */
    @Tool("""
        批量搜索音乐。传入 5-15 个搜索关键词，并行搜索返回候选列表。

        关键词规则：
        1. 必须是具体歌名，禁止用描述词
           - ❌ 错误: "夜间驾驶", "晨间音乐", "雾中上海"
           - ✅ 正确: "夜空中最亮的星", "十年", "海阔天空"
        2. 如果调用了 getMusicSeeds，必须严格基于种子选歌
           - 种子"粤语怀旧" → 搜 "千千阙歌", "偏偏喜欢你"
           - 种子"日语爵士" → 搜 "Fly Me to the Moon", "坂本龙一"
        3. 每个种子选 2-3 首具体歌名

        返回每个关键词的前5首候选，供后续选择。
        """)
    public BatchSearchResult batchSearchMusic(
        @P("搜索关键词列表，5-15个，歌名优先") List<String> keywords
    ) {
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("关键词列表不能为空");
        }
        if (keywords.size() > 15) {
            keywords = keywords.subList(0, 15);
        }
        return musicService.batchSearch(keywords);
    }

    /**
     * 批量播放音乐（获取歌单）
     */
    @Tool("""
        批量获取歌曲播放信息，生成歌单。

        从 batchSearchMusic 返回的候选中，每个关键词选择 1 首最佳歌曲：
        1. 优先选择 fee=0 或 fee=8 的免费歌曲
        2. 时长 >= 60秒
        3. 优先选择原唱版本

        返回歌单（包含播放URL），前端可顺序播放。
        """)
    public Playlist batchPlayMusic(
        @P("选中的歌曲ID列表") List<String> ids
    ) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("歌曲ID列表不能为空");
        }
        if (ids.size() > 15) {
            ids = ids.subList(0, 15);
        }
        Playlist playlist = new Playlist(musicService.batchPlay(ids));

        // 同步到 PlaylistService
        String sessionId = SessionContext.getSessionId();
        if (sessionId != null) {
            playlistService.setPlaylist(sessionId, playlist);
        }

        return playlist;
    }
}