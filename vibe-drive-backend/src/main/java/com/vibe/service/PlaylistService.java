package com.vibe.service;

import com.vibe.model.Playlist;
import com.vibe.model.PlayResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 播放列表服务
 * 管理每个会话的播放列表状态
 */
@Service
public class PlaylistService {

    // 每个会话的播放列表
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();
    // 每个会话的播放状态
    private final Map<String, PlaybackState> playbackStates = new ConcurrentHashMap<>();

    /**
     * 获取播放列表
     */
    public Playlist getPlaylist(String sessionId) {
        return playlists.get(sessionId);
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(String sessionId, Playlist playlist) {
        playlists.put(sessionId, playlist);
        playbackStates.put(sessionId, new PlaybackState(0, true));
    }

    /**
     * 获取播放状态
     */
    public PlaybackState getPlaybackState(String sessionId) {
        return playbackStates.getOrDefault(sessionId, new PlaybackState(0, false));
    }

    /**
     * 暂停播放
     */
    public void pause(String sessionId) {
        PlaybackState state = playbackStates.get(sessionId);
        if (state != null) {
            playbackStates.put(sessionId, new PlaybackState(state.currentIndex(), false));
        }
    }

    /**
     * 继续播放
     */
    public void resume(String sessionId) {
        PlaybackState state = playbackStates.get(sessionId);
        if (state != null) {
            playbackStates.put(sessionId, new PlaybackState(state.currentIndex(), true));
        }
    }

    /**
     * 播放指定索引
     */
    public PlayResult playAt(String sessionId, int index) {
        Playlist playlist = playlists.get(sessionId);
        if (playlist == null || playlist.songs() == null) {
            return null;
        }
        if (index < 0 || index >= playlist.songs().size()) {
            return null;
        }
        playbackStates.put(sessionId, new PlaybackState(index, true));
        return playlist.songs().get(index);
    }

    /**
     * 从播放列表删除
     */
    public boolean removeAt(String sessionId, int index) {
        Playlist playlist = playlists.get(sessionId);
        if (playlist == null || playlist.songs() == null) {
            return false;
        }
        if (index < 0 || index >= playlist.songs().size()) {
            return false;
        }
        List<PlayResult> newSongs = new ArrayList<>(playlist.songs());
        newSongs.remove(index);
        playlists.put(sessionId, new Playlist(newSongs, playlist.currentIndex()));
        return true;
    }

    /**
     * 清空播放列表
     */
    public void clear(String sessionId) {
        playlists.remove(sessionId);
        playbackStates.remove(sessionId);
    }

    /**
     * 添加歌曲到播放列表末尾
     */
    public void addSong(String sessionId, PlayResult song) {
        Playlist playlist = playlists.get(sessionId);
        if (playlist == null) {
            // 创建新播放列表
            playlists.put(sessionId, new Playlist(new ArrayList<>(List.of(song))));
            playbackStates.put(sessionId, new PlaybackState(0, true));
        } else {
            // 添加到现有播放列表
            List<PlayResult> newSongs = new ArrayList<>(playlist.songs());
            newSongs.add(song);
            playlists.put(sessionId, new Playlist(newSongs, playlist.currentIndex()));
        }
    }

    /**
     * 播放状态
     */
    public record PlaybackState(int currentIndex, boolean isPlaying) {}
}
