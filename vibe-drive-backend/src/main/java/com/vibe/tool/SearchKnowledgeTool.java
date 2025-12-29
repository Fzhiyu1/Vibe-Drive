package com.vibe.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.rag.KnowledgeStore;
import com.vibe.rag.SearchResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 知识检索工具
 * 让主智能体能够搜索项目文档
 */
@Component
public class SearchKnowledgeTool {

    private final KnowledgeStore knowledgeStore;
    private final ObjectMapper objectMapper;

    public SearchKnowledgeTool(KnowledgeStore knowledgeStore, ObjectMapper objectMapper) {
        this.knowledgeStore = knowledgeStore;
        this.objectMapper = objectMapper;
    }

    @Tool("搜索项目知识库，根据问题检索相关文档。返回最相关的文档片段及相似度分数。")
    public String searchKnowledge(
        @P("搜索查询，描述你想了解的内容") String query
    ) {
        List<SearchResult> results = knowledgeStore.search(query, 3);

        if (results.isEmpty()) {
            return "未找到相关文档";
        }

        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            // 降级为简单格式
            StringBuilder sb = new StringBuilder();
            for (SearchResult r : results) {
                sb.append(r.toDisplayString()).append("\n---\n");
            }
            return sb.toString();
        }
    }
}
