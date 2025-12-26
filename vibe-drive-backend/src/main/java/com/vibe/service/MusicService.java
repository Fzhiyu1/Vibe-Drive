package com.vibe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音乐推荐服务
 * 负责加载曲库、根据条件筛选和推荐音乐
 * 支持调用 Go 微服务（网易云 API）
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);
    private static final int SEARCH_LIMIT = 20;
    private static final int BATCH_SEARCH_LIMIT = 5;
    private static final ExecutorService searchExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${music-api.url:http://localhost:8081}")
    private String musicApiUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private List<Song> songLibrary;

    public MusicService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 情绪 → BPM 范围映射（旧方法使用）
     */
    private static final Map<String, BpmRange> MOOD_BPM_MAP = Map.of(
        "happy", new BpmRange(100, 140),
        "calm", new BpmRange(60, 90),
        "tired", new BpmRange(50, 80),
        "stressed", new BpmRange(60, 100),
        "excited", new BpmRange(120, 180)
    );

    /**
     * 时段 → 推荐流派映射（旧方法使用）
     */
    private static final Map<String, List<String>> TIME_GENRE_MAP = Map.of(
        "midnight", List.of("jazz", "classical", "ambient"),
        "night", List.of("jazz", "folk", "ambient"),
        "dawn", List.of("classical", "folk", "ambient"),
        "morning", List.of("pop", "folk", "rock"),
        "noon", List.of("pop", "rock", "electronic"),
        "afternoon", List.of("pop", "rock", "jazz"),
        "evening", List.of("jazz", "folk", "pop")
    );

    @PostConstruct
    public void init() {
        loadSongLibrary();
    }

    /**
     * 加载曲库数据
     */
    private void loadSongLibrary() {
        try {
            ClassPathResource resource = new ClassPathResource("mock-data/mock-songs.json");
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode songsNode = root.get("songs");
                if (songsNode == null || songsNode.isNull() || !songsNode.isArray()) {
                    log.warn("Invalid song library format: 'songs' field is missing or not an array");
                    songLibrary = List.of();
                    return;
                }
                songLibrary = objectMapper.convertValue(songsNode, new TypeReference<List<Song>>() {});
                log.info("Loaded {} songs from mock library", songLibrary.size());
            }
        } catch (Exception e) {
            log.error("Failed to load song library: {}", e.getMessage(), e);
            songLibrary = List.of();
        }
    }

    /**
     * 推荐音乐（旧方法，使用本地 Mock 数据）
     *
     * @deprecated 请使用 {@link #search(String)} 和 {@link #play(String)} 方法
     */
    @Deprecated
    public MusicRecommendation recommend(String mood, String timeOfDay, int passengerCount, String genre) {
        BpmRange targetBpm = MOOD_BPM_MAP.getOrDefault(mood, new BpmRange(60, 120));
        String effectiveGenre = determineGenre(genre, timeOfDay, passengerCount);

        // 优先使用 mood 标签 + BPM + 流派筛选
        List<Song> filtered = songLibrary.stream()
            .filter(song -> song.matchesMood(mood))
            .filter(song -> matchesBpm(song, targetBpm))
            .filter(song -> matchesGenre(song, effectiveGenre))
            .limit(5)
            .toList();

        // 如果筛选结果太少，放宽流派条件
        if (filtered.size() < 3) {
            filtered = songLibrary.stream()
                .filter(song -> song.matchesMood(mood))
                .filter(song -> matchesBpm(song, targetBpm))
                .limit(5)
                .toList();
        }

        // 如果还是太少，只用 mood 标签筛选
        if (filtered.size() < 3) {
            filtered = songLibrary.stream()
                .filter(song -> song.matchesMood(mood))
                .limit(5)
                .toList();
        }

        // 如果还是太少，返回默认推荐
        if (filtered.isEmpty()) {
            filtered = songLibrary.stream().limit(5).toList();
        }

        return new MusicRecommendation(filtered, mood, effectiveGenre, targetBpm);
    }

    /**
     * 确定有效流派
     */
    private String determineGenre(String preferredGenre, String timeOfDay, int passengerCount) {
        // 如果用户指定了流派，优先使用
        if (preferredGenre != null && !preferredGenre.isBlank()) {
            return preferredGenre;
        }

        // 多人乘坐时选择更大众化的音乐
        if (passengerCount >= 3) {
            return "pop";
        }

        // 根据时段推荐流派
        List<String> timeGenres = TIME_GENRE_MAP.get(timeOfDay);
        if (timeGenres != null && !timeGenres.isEmpty()) {
            return timeGenres.get(0);
        }

        return "pop";
    }

    /**
     * 检查歌曲 BPM 是否在目标范围内
     */
    private boolean matchesBpm(Song song, BpmRange range) {
        int bpm = song.bpm();
        return bpm >= range.min() && bpm <= range.max();
    }

    /**
     * 检查歌曲流派是否匹配
     */
    private boolean matchesGenre(Song song, String genre) {
        if (genre == null || genre.isBlank() || "mixed".equals(genre)) {
            return true;
        }
        return genre.equalsIgnoreCase(song.genre());
    }

    /**
     * 获取曲库大小（用于测试）
     */
    public int getLibrarySize() {
        return songLibrary != null ? songLibrary.size() : 0;
    }

    // ==================== 新方法：调用 Go 微服务 ====================

    /**
     * 搜索音乐
     *
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    public SearchResult search(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String urlStr = musicApiUrl + "/api/music/search?keyword=" + encodedKeyword + "&limit=" + SEARCH_LIMIT;

        log.info("Searching music: keyword={}, url={}", keyword, urlStr);

        try {
            // 使用 URI 对象避免 RestTemplate 二次编码
            java.net.URI uri = java.net.URI.create(urlStr);
            String response = restTemplate.getForObject(uri, String.class);
            return parseSearchResponse(response);
        } catch (Exception e) {
            log.error("Failed to search music: {}", e.getMessage());
            throw new RuntimeException("音乐搜索服务不可用: " + e.getMessage(), e);
        }
    }

    /**
     * 播放音乐
     *
     * @param id 歌曲 ID
     * @return 播放结果
     */
    public PlayResult play(String id) {
        log.info("Getting play info: id={}", id);

        try {
            // 获取播放 URL
            String urlResponse = restTemplate.getForObject(
                musicApiUrl + "/api/music/url?id=" + id, String.class);
            String playUrl = parseUrlResponse(urlResponse);

            // 获取歌曲详情
            String detailResponse = restTemplate.getForObject(
                musicApiUrl + "/api/music/detail?id=" + id, String.class);

            return parseDetailResponse(detailResponse, playUrl);
        } catch (Exception e) {
            log.error("Failed to get play info: {}", e.getMessage());
            throw new RuntimeException("获取播放信息失败: " + e.getMessage(), e);
        }
    }

    // ==================== 批量方法 ====================

    /**
     * 批量搜索音乐（并行）
     *
     * @param keywords 搜索关键词列表（3-5个）
     * @return 批量搜索结果
     */
    public BatchSearchResult batchSearch(List<String> keywords) {
        log.info("Batch searching music: keywords={}", keywords);

        List<CompletableFuture<Map.Entry<String, List<SongCandidate>>>> futures =
            keywords.stream()
                .map(keyword -> CompletableFuture.supplyAsync(() -> {
                    try {
                        SearchResult result = searchWithLimit(keyword, BATCH_SEARCH_LIMIT);
                        return Map.entry(keyword, result.songs());
                    } catch (Exception e) {
                        log.warn("Search failed for keyword: {}", keyword, e);
                        return Map.entry(keyword, List.<SongCandidate>of());
                    }
                }, searchExecutor))
                .toList();

        Map<String, List<SongCandidate>> results = new HashMap<>();
        for (var future : futures) {
            var entry = future.join();
            results.put(entry.getKey(), entry.getValue());
        }

        int total = results.values().stream().mapToInt(List::size).sum();
        log.info("Batch search completed: {} keywords, {} total candidates",
                 keywords.size(), total);

        return new BatchSearchResult(results, total);
    }

    /**
     * 带限制的搜索
     */
    private SearchResult searchWithLimit(String keyword, int limit) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String urlStr = musicApiUrl + "/api/music/search?keyword=" + encodedKeyword + "&limit=" + limit;

        try {
            java.net.URI uri = java.net.URI.create(urlStr);
            String response = restTemplate.getForObject(uri, String.class);
            return parseSearchResponse(response);
        } catch (Exception e) {
            log.error("Failed to search music: {}", e.getMessage());
            throw new RuntimeException("音乐搜索服务不可用: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取播放信息（并行）
     *
     * @param ids 歌曲ID列表（3-5个）
     * @return 播放结果列表
     */
    public List<PlayResult> batchPlay(List<String> ids) {
        log.info("Batch getting play info: ids={}", ids);

        List<CompletableFuture<PlayResult>> futures = ids.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> {
                try {
                    return play(id);
                } catch (Exception e) {
                    log.warn("Failed to get play info for id: {}", id, e);
                    return null;
                }
            }, searchExecutor))
            .toList();

        List<PlayResult> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .filter(PlayResult::hasValidUrl)
            .toList();

        log.info("Batch play completed: {} requested, {} valid", ids.size(), results.size());
        return results;
    }

    /**
     * 解析搜索响应
     */
    private SearchResult parseSearchResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.get("result");
            if (result == null) {
                return new SearchResult(List.of(), 0);
            }

            int total = result.has("songCount") ? result.get("songCount").asInt() : 0;
            JsonNode songsNode = result.get("songs");
            if (songsNode == null || !songsNode.isArray()) {
                return new SearchResult(List.of(), total);
            }

            List<SongCandidate> songs = new ArrayList<>();
            for (JsonNode songNode : songsNode) {
                songs.add(parseSongCandidate(songNode));
            }

            return new SearchResult(songs, total);
        } catch (Exception e) {
            log.error("Failed to parse search response: {}", e.getMessage());
            throw new RuntimeException("解析搜索结果失败", e);
        }
    }

    /**
     * 解析单个歌曲候选
     */
    private SongCandidate parseSongCandidate(JsonNode node) {
        String id = node.get("id").asText();
        String name = node.get("name").asText();

        // 获取歌手名（网易云 API 使用 "ar" 字段）
        String artist = "";
        JsonNode artists = node.get("ar");
        if (artists != null && artists.isArray() && !artists.isEmpty()) {
            artist = artists.get(0).get("name").asText();
        }

        // 时长（网易云 API 使用 "dt" 字段，毫秒转秒）
        int duration = node.has("dt") ? node.get("dt").asInt() / 1000 : 0;

        // 播放量（网易云 API 使用 "pop" 字段）
        long plays = node.has("pop") ? node.get("pop").asLong() : 0;

        // 费用类型
        int fee = node.has("fee") ? node.get("fee").asInt() : 0;

        // 封面 URL（网易云 API 使用 "al" 字段）
        String coverUrl = "";
        JsonNode album = node.get("al");
        if (album != null && album.has("picUrl")) {
            coverUrl = album.get("picUrl").asText();
        }

        return new SongCandidate(id, name, artist, duration, plays, fee, coverUrl);
    }

    /**
     * 解析 URL 响应
     */
    private String parseUrlResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                if (first.has("url") && !first.get("url").isNull()) {
                    return first.get("url").asText();
                }
            }
            return "";
        } catch (Exception e) {
            log.error("Failed to parse URL response: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析详情响应
     */
    private PlayResult parseDetailResponse(String response, String playUrl) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode songs = root.get("songs");
            if (songs == null || !songs.isArray() || songs.isEmpty()) {
                throw new RuntimeException("歌曲详情不存在");
            }

            JsonNode song = songs.get(0);
            String id = song.get("id").asText();
            String name = song.get("name").asText();

            // 歌手名
            String artist = "";
            JsonNode ar = song.get("ar");
            if (ar != null && ar.isArray() && !ar.isEmpty()) {
                artist = ar.get(0).get("name").asText();
            }

            // 时长（毫秒转秒）
            int duration = song.has("dt") ? song.get("dt").asInt() / 1000 : 0;

            // 封面 URL
            String coverUrl = "";
            JsonNode al = song.get("al");
            if (al != null && al.has("picUrl")) {
                coverUrl = al.get("picUrl").asText();
            }

            return new PlayResult(id, name, artist, playUrl, duration, coverUrl);
        } catch (Exception e) {
            log.error("Failed to parse detail response: {}", e.getMessage());
            throw new RuntimeException("解析歌曲详情失败", e);
        }
    }
}
