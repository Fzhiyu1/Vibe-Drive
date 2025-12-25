package com.vibe.model;

/**
 * 播放结果
 *
 * @param id       歌曲 ID
 * @param name     歌曲名称
 * @param artist   歌手名称
 * @param url      播放地址
 * @param duration 时长（秒）
 * @param coverUrl 封面图片 URL
 */
public record PlayResult(
    String id,
    String name,
    String artist,
    String url,
    int duration,
    String coverUrl
) {
    /**
     * 是否有有效的播放地址
     */
    public boolean hasValidUrl() {
        return url != null && !url.isBlank();
    }
}
