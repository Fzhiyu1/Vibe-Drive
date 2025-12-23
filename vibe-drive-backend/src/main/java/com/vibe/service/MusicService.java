package com.vibe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.BpmRange;
import com.vibe.model.MusicRecommendation;
import com.vibe.model.Song;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 音乐推荐服务
 * 负责加载曲库、根据条件筛选和推荐音乐
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    /**
     * 情绪 → BPM 范围映射
     */
    private static final Map<String, BpmRange> MOOD_BPM_MAP = Map.of(
        "happy", new BpmRange(100, 140),
        "calm", new BpmRange(60, 90),
        "tired", new BpmRange(50, 80),
        "stressed", new BpmRange(60, 100),
        "excited", new BpmRange(120, 180)
    );

    /**
     * 时段 → 推荐流派映射
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

    private final ObjectMapper objectMapper;
    private List<Song> songLibrary;

    public MusicService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
     * 推荐音乐
     *
     * @param mood           目标情绪
     * @param timeOfDay      时段
     * @param passengerCount 乘客数量
     * @param genre          偏好流派（可选）
     * @return 音乐推荐结果
     */
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
}
