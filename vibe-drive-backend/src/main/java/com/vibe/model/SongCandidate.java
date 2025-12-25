package com.vibe.model;

/**
 * 搜索结果中的歌曲候选项
 *
 * @param id       歌曲 ID
 * @param name     歌曲名称
 * @param artist   歌手名称
 * @param duration 时长（秒）
 * @param plays    播放量
 * @param fee      费用类型：0=免费, 1=VIP, 8=低音质免费
 * @param coverUrl 封面图片 URL
 */
public record SongCandidate(
    String id,
    String name,
    String artist,
    int duration,
    long plays,
    int fee,
    String coverUrl
) {
    /**
     * 是否可以播放（fee=0 或 fee=8）
     */
    public boolean isPlayable() {
        return fee == 0 || fee == 8;
    }

    /**
     * 是否是正经音乐（时长 >= 60秒）
     */
    public boolean isValidDuration() {
        return duration >= 60;
    }
}
