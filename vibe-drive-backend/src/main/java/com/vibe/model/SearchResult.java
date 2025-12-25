package com.vibe.model;

import java.util.List;

/**
 * 音乐搜索结果
 *
 * @param songs 歌曲候选列表
 * @param total 总数
 */
public record SearchResult(
    List<SongCandidate> songs,
    int total
) {
    /**
     * 获取可播放的歌曲数量
     */
    public long playableCount() {
        return songs.stream().filter(SongCandidate::isPlayable).count();
    }
}
