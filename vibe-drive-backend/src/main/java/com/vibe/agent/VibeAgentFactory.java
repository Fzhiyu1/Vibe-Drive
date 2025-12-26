package com.vibe.agent;

import com.vibe.tool.LightTool;
import com.vibe.tool.MassageTool;
import com.vibe.tool.MusicSeedTool;
import com.vibe.tool.MusicTool;
import com.vibe.tool.NarrativeTool;
import com.vibe.tool.ScentTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Vibe Agent 工厂
 * 负责创建配置好的 VibeAgent 实例
 */
@Component
public class VibeAgentFactory {

    private final StreamingChatModel streamingModel;
    private final ChatMemoryStore memoryStore;
    private final MusicTool musicTool;
    private final MusicSeedTool musicSeedTool;
    private final LightTool lightTool;
    private final NarrativeTool narrativeTool;
    private final ScentTool scentTool;
    private final MassageTool massageTool;
    private final PromptAssembler promptAssembler;

    @Value("${vibe.agent.max-messages:20}")
    private int maxMessages;

    public VibeAgentFactory(
            StreamingChatModel streamingModel,
            ChatMemoryStore memoryStore,
            MusicTool musicTool,
            MusicSeedTool musicSeedTool,
            LightTool lightTool,
            NarrativeTool narrativeTool,
            ScentTool scentTool,
            MassageTool massageTool,
            PromptAssembler promptAssembler) {
        this.streamingModel = streamingModel;
        this.memoryStore = memoryStore;
        this.musicTool = musicTool;
        this.musicSeedTool = musicSeedTool;
        this.lightTool = lightTool;
        this.narrativeTool = narrativeTool;
        this.scentTool = scentTool;
        this.massageTool = massageTool;
        this.promptAssembler = promptAssembler;
    }

    /**
     * 创建 VibeAgent 实例
     */
    public VibeAgent createAgent() {
        ChatMemoryProvider memoryProvider = memoryId ->
            MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(memoryStore)
                .build();

        return AiServices.builder(VibeAgent.class)
            .streamingChatModel(streamingModel)
            .chatMemoryProvider(memoryProvider)
            .systemMessageProvider(id -> promptAssembler.assembleSystemPrompt())
            .tools(musicTool, musicSeedTool, lightTool, narrativeTool, scentTool, massageTool)
            .build();
    }
}
