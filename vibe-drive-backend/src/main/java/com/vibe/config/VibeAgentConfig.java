package com.vibe.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vibe Agent 配置类
 */
@Configuration
public class VibeAgentConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key:demo}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private double temperature;

    /**
     * 流式聊天模型
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 聊天记忆存储（开发阶段使用内存存储）
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * 非流式聊天模型（用于环境生成等同步调用）
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
