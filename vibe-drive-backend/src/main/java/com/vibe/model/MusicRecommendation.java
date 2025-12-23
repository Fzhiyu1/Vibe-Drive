package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 音乐推荐结果
 * 包含歌曲列表和元数据
 */
@Description("音乐推荐结果，包含歌曲列表和元数据")
public record MusicRecommendation(
    @NotEmpty(message = "Songs list cannot be empty")
    @Size(max = 10, message = "Songs list cannot exceed 10 items")
    @Description("推荐的歌曲列表，1-10首")
    List<@Valid Song> songs,

    @Description("目标情绪标签")
    String mood,

    @Description("音乐流派")
    String genre,

    @Valid
    @Description("推荐歌曲的BPM（节拍速度）范围")
    BpmRange bpmRange
) {
    /**
     * 紧凑构造器：校验参数
     */
    public MusicRecommendation {
        if (songs == null || songs.isEmpty()) {
            throw new IllegalArgumentException("Songs list cannot be empty");
        }
        if (songs.size() > 10) {
            throw new IllegalArgumentException("Songs list cannot exceed 10 items");
        }
        // 确保列表不可变
        songs = List.copyOf(songs);
    }

    /**
     * 获取歌曲数量
     */
    public int songCount() {
        return songs.size();
    }

    /**
     * 获取总时长（秒）
     */
    public int totalDuration() {
        return songs.stream()
            .mapToInt(Song::duration)
            .sum();
    }

    /**
     * 获取格式化的总时长
     */
    public String totalDurationFormatted() {
        int total = totalDuration();
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 获取第一首歌曲
     */
    public Song firstSong() {
        return songs.getFirst();
    }

    /**
     * 判断是否为单曲推荐
     */
    public boolean isSingleSong() {
        return songs.size() == 1;
    }
}
