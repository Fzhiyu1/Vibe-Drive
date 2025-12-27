package com.vibe.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.context.SessionContext;
import com.vibe.model.Playlist;
import com.vibe.model.PlayResult;
import com.vibe.service.MusicService;
import com.vibe.service.PlaylistService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 播放列表控制工具
 */
@Component
public class PlaylistTool {

    private static final Logger log = LoggerFactory.getLogger(PlaylistTool.class);
    private final PlaylistService playlistService;
    private final MusicService musicService;
    private final ObjectMapper objectMapper;

    public PlaylistTool(PlaylistService playlistService, MusicService musicService, ObjectMapper objectMapper) {
        this.playlistService = playlistService;
        this.musicService = musicService;
        this.objectMapper = objectMapper;
    }

    @Tool("获取当前播放列表，返回所有歌曲信息和当前播放索引")
    public String getPlaylist() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        Playlist playlist = playlistService.getPlaylist(sessionId);
        if (playlist == null || playlist.songs() == null || playlist.songs().isEmpty()) {
            return "当前播放列表为空";
        }

        PlaylistService.PlaybackState state = playlistService.getPlaybackState(sessionId);
        StringBuilder sb = new StringBuilder();
        sb.append("当前播放列表（共").append(playlist.songs().size()).append("首）：\n");

        List<PlayResult> songs = playlist.songs();
        for (int i = 0; i < songs.size(); i++) {
            PlayResult song = songs.get(i);
            String marker = (i == state.currentIndex()) ? "▶ " : "  ";
            sb.append(marker).append(i + 1).append(". ")
              .append(song.name()).append(" - ").append(song.artist()).append("\n");
        }

        sb.append("\n当前状态：").append(state.isPlaying() ? "播放中" : "已暂停");
        return sb.toString();
    }

    @Tool("暂停音乐播放")
    public String pauseMusic() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        playlistService.pause(sessionId);
        log.info("暂停音乐: sessionId={}", sessionId);
        return toJson(new PlaybackAction("pause", null, null));
    }

    @Tool("继续播放音乐")
    public String resumeMusic() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        playlistService.resume(sessionId);
        log.info("继续播放: sessionId={}", sessionId);
        return toJson(new PlaybackAction("resume", null, null));
    }

    @Tool("播放播放列表中指定位置的歌曲")
    public String playAt(@P("歌曲位置，从1开始") int position) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        int index = position - 1; // 转换为0-based索引
        PlayResult song = playlistService.playAt(sessionId, index);
        if (song == null) {
            return "无效的位置：" + position;
        }

        log.info("播放指定歌曲: sessionId={}, position={}, song={}", sessionId, position, song.name());
        return toJson(new PlaybackAction("playAt", index, song));
    }

    @Tool("从播放列表中删除指定位置的歌曲")
    public String removeFromPlaylist(@P("要删除的歌曲位置，从1开始") int position) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        Playlist playlist = playlistService.getPlaylist(sessionId);
        if (playlist == null || playlist.songs() == null) {
            return "播放列表为空";
        }

        int index = position - 1;
        if (index < 0 || index >= playlist.songs().size()) {
            return "无效的位置：" + position;
        }

        String songName = playlist.songs().get(index).name();
        boolean removed = playlistService.removeAt(sessionId, index);
        if (removed) {
            log.info("删除歌曲: sessionId={}, position={}, song={}", sessionId, position, songName);
            return toJson(new PlaybackAction("remove", index, null));
        }
        return "删除失败";
    }

    @Tool("清空播放列表")
    public String clearPlaylist() {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        playlistService.clear(sessionId);
        log.info("清空播放列表: sessionId={}", sessionId);
        return toJson(new PlaybackAction("clear", null, null));
    }

    @Tool("添加歌曲到播放列表末尾")
    public String addToPlaylist(@P("歌曲ID") String songId) {
        String sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            return "会话未初始化";
        }

        PlayResult song = musicService.play(songId);
        if (song == null || song.url() == null) {
            return "无法获取歌曲: " + songId;
        }

        playlistService.addSong(sessionId, song);
        log.info("添加歌曲到播放列表: sessionId={}, song={}", sessionId, song.name());
        return toJson(new PlaybackAction("add", null, song));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record PlaybackAction(String action, Integer index, PlayResult song) {}
}
