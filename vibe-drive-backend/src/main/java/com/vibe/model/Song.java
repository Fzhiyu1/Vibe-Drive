package com.vibe.model;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 歌曲信息
 * 包含歌曲的基本元数据
 */
@Description("歌曲信息")
public record Song(
    @NotBlank(message = "Song id cannot be empty")
    @Description("歌曲唯一标识")
    String id,

    @NotBlank(message = "Song title cannot be empty")
    @Description("歌曲名称")
    String title,

    @NotBlank(message = "Artist cannot be empty")
    @Description("艺术家/演唱者")
    String artist,

    @Description("专辑名称")
    String album,

    @Min(value = 0, message = "Duration must be non-negative")
    @Description("时长，单位秒")
    int duration,

    @Min(value = 0, message = "BPM must be non-negative")
    @Description("节拍速度 BPM（Beats Per Minute）")
    int bpm,

    @Description("音乐流派：pop/rock/jazz/classical/electronic/ambient 等")
    String genre,

    @Description("封面图片URL")
    String coverUrl
) {
    /**
     * 紧凑构造器：校验参数
     */
    public Song {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Song id cannot be empty");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Song title cannot be empty");
        }
        if (artist == null || artist.isBlank()) {
            throw new IllegalArgumentException("Artist cannot be empty");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
        if (bpm < 0) {
            throw new IllegalArgumentException("BPM must be non-negative");
        }
    }

    /**
     * 获取格式化的时长字符串（mm:ss）
     */
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 判断是否为慢节奏歌曲（BPM < 80）
     */
    public boolean isSlowTempo() {
        return bpm > 0 && bpm < 80;
    }

    /**
     * 判断是否为快节奏歌曲（BPM > 120）
     */
    public boolean isFastTempo() {
        return bpm > 120;
    }
}
