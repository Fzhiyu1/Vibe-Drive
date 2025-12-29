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

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

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
     * 加载文档并建立索引（支持 JAR 包内的 classpath 资源）
     */
    private void loadDocuments() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pattern = "classpath*:" + docsPath + "/**/*.md";

        log.info("扫描文档: {}", pattern);
        Resource[] resources = resolver.getResources(pattern);

        if (resources.length == 0) {
            log.warn("未找到文档，pattern: {}", pattern);
            return;
        }

        int count = 0;
        for (Resource resource : resources) {
            try (InputStream is = resource.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String filename = resource.getFilename();

                TextSegment segment = TextSegment.from(content);
                segment.metadata().put("source", filename != null ? filename : "unknown");

                Embedding embedding = embeddingModel.embed(segment).content();
                store.add(embedding, segment);
                count++;
                log.debug("已加载文档: {}", filename);
            } catch (Exception e) {
                log.warn("加载文档失败: {}", resource.getFilename(), e);
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
