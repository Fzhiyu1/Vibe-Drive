package com.vibe.model;

import java.util.List;

/**
 * 歌单
 *
 * @param songs        歌曲列表（已获取播放URL）
 * @param currentIndex 当前播放索引
 */
public record Playlist(
    List<PlayResult> songs,
    int currentIndex
) {
    public Playlist(List<PlayResult> songs) {
        this(songs, 0);
    }

    public PlayResult currentSong() {
        if (songs == null || songs.isEmpty()) return null;
        return songs.get(Math.min(currentIndex, songs.size() - 1));
    }

    public boolean hasNext() {
        return songs != null && currentIndex < songs.size() - 1;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    public int size() {
        return songs != null ? songs.size() : 0;
    }
}
