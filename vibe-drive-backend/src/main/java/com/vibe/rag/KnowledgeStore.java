package com.vibe.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * 知识库服务
 * 使用 RAG 技术实现项目文档的语义检索
 */
@Service
public class KnowledgeStore {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeStore.class);

    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> store;

    @Value("${vibe.knowledge.docs-path:docs}")
    private String docsPath;

    @Value("${vibe.knowledge.max-results:3}")
    private int defaultMaxResults;

    public KnowledgeStore() {
        this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        this.store = new InMemoryEmbeddingStore<>();
    }

    @PostConstruct
    public void init() {
        log.info("初始化知识库，加载文档目录: {}", docsPath);
        try {
            loadDocuments();
        } catch (Exception e) {
            log.error("加载知识库文档失败", e);
        }
    }

    /**
     * 加载文档并建立索引
     */
    private void loadDocuments() throws IOException {
        Path basePath = Paths.get(docsPath);
        if (!Files.exists(basePath)) {
            log.warn("文档目录不存在: {}", basePath.toAbsolutePath());
            return;
        }

        int count = 0;
        try (Stream<Path> paths = Files.walk(basePath)) {
            List<Path> mdFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".md"))
                .filter(p -> !p.toString().contains("node_modules"))
                .toList();

            for (Path file : mdFiles) {
                try {
                    String content = Files.readString(file);
                    String relativePath = basePath.relativize(file).toString();

                    // 创建文本段，包含元数据
                    TextSegment segment = TextSegment.from(content);
                    segment.metadata().put("source", relativePath);

                    // 向量化并存储
                    Embedding embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);
                    count++;
                } catch (Exception e) {
                    log.warn("加载文档失败: {}", file, e);
                }
            }
        }
        log.info("知识库初始化完成，共加载 {} 个文档", count);
    }

    /**
     * 语义检索
     */
    public List<SearchResult> search(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .build();
        EmbeddingSearchResult<TextSegment> result = store.search(request);

        return result.matches().stream()
            .map(match -> new SearchResult(
                match.embedded().metadata().getString("source"),
                match.score(),
                truncateContent(match.embedded().text(), 500)
            ))
            .toList();
    }

    /**
     * 使用默认数量检索
     */
    public List<SearchResult> search(String query) {
        return search(query, defaultMaxResults);
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 获取已索引的文档数量
     */
    public int getDocumentCount() {
        // InMemoryEmbeddingStore 没有直接的 size 方法，用搜索空字符串的方式估算
        return search("", 1000).size();
    }
}
