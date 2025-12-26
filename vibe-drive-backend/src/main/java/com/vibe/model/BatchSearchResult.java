package com.vibe.model;

import java.util.List;
import java.util.Map;

/**
 * 批量搜索结果
 *
 * @param results         关键词 -> 搜索结果的映射
 * @param totalCandidates 总候选数量
 */
public record BatchSearchResult(
    Map<String, List<SongCandidate>> results,
    int totalCandidates
) {
    /**
     * 获取所有候选歌曲（扁平化）
     */
    public List<SongCandidate> allCandidates() {
        return results.values().stream()
            .flatMap(List::stream)
            .toList();
    }

    /**
     * 获取可播放的候选歌曲
     */
    public List<SongCandidate> playableCandidates() {
        return allCandidates().stream()
            .filter(SongCandidate::isPlayable)
            .filter(SongCandidate::isValidDuration)
            .toList();
    }
}
