package com.vibe.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Vibe Agent 配置类
 * 支持 OpenAI 和 Anthropic 两种模型提供商，带自动降级
 */
@Configuration
public class VibeAgentConfig {

    private static final Logger log = LoggerFactory.getLogger(VibeAgentConfig.class);

    @Value("${langchain4j.provider:openai}")
    private String provider;

    @Value("${langchain4j.fallback.cooldown-minutes:5}")
    private int cooldownMinutes;

    // OpenAI 配置
    @Value("${langchain4j.open-ai.chat-model.api-key:demo}")
    private String openaiApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String openaiModelName;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private double openaiTemperature;

    // Anthropic 配置
    @Value("${langchain4j.anthropic.chat-model.api-key:demo}")
    private String anthropicApiKey;

    @Value("${langchain4j.anthropic.chat-model.base-url:https://api.anthropic.com}")
    private String anthropicBaseUrl;

    @Value("${langchain4j.anthropic.chat-model.model-name:claude-haiku-4-5-20251001}")
    private String anthropicModelName;

    @Value("${langchain4j.anthropic.chat-model.temperature:0.7}")
    private double anthropicTemperature;

    /**
     * 流式聊天模型（带降级支持）
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        log.info("初始化模型提供商: {}, 冷却时间: {}分钟", provider, cooldownMinutes);

        // 创建 Anthropic 模型
        StreamingChatModel anthropicModel = AnthropicStreamingChatModel.builder()
                .apiKey(anthropicApiKey)
                .baseUrl(anthropicBaseUrl)
                .modelName(anthropicModelName)
                .temperature(anthropicTemperature)
                .logRequests(true)
                .logResponses(true)
                .build();

        // 创建 OpenAI/DeepSeek 模型
        StreamingChatModel openaiModel = OpenAiStreamingChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .modelName(openaiModelName)
                .temperature(openaiTemperature)
                .logRequests(true)
                .logResponses(true)
                .build();

        // 根据配置决定主模型和降级模型
        if ("anthropic".equalsIgnoreCase(provider)) {
            log.info("主模型: {} (Anthropic), 降级模型: {} (OpenAI)", anthropicModelName, openaiModelName);
            return new FallbackStreamingChatModel(
                    anthropicModel,
                    openaiModel,
                    "Anthropic/" + anthropicModelName,
                    "OpenAI/" + openaiModelName,
                    Duration.ofMinutes(cooldownMinutes)
            );
        } else {
            log.info("主模型: {} (OpenAI), 降级模型: {} (Anthropic)", openaiModelName, anthropicModelName);
            return new FallbackStreamingChatModel(
                    openaiModel,
                    anthropicModel,
                    "OpenAI/" + openaiModelName,
                    "Anthropic/" + anthropicModelName,
                    Duration.ofMinutes(cooldownMinutes)
            );
        }
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
        if ("anthropic".equalsIgnoreCase(provider)) {
            return AnthropicChatModel.builder()
                    .apiKey(anthropicApiKey)
                    .baseUrl(anthropicBaseUrl)
                    .modelName(anthropicModelName)
                    .temperature(anthropicTemperature)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            return OpenAiChatModel.builder()
                    .apiKey(openaiApiKey)
                    .baseUrl(openaiBaseUrl)
                    .modelName(openaiModelName)
                    .temperature(openaiTemperature)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
    }
}
