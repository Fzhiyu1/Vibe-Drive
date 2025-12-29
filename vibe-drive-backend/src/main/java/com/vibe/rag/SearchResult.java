package com.vibe.rag;

/**
 * RAG 检索结果
 */
public record SearchResult(
    String source,      // 文档来源路径
    double score,       // 相似度分数 (0-1)
    String content      // 文档内容片段
) {
    /**
     * 格式化为可读字符串
     */
    public String toDisplayString() {
        return String.format("[%.2f] %s\n%s", score, source, content);
    }
}
