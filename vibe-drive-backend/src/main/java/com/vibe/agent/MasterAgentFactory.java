package com.vibe.agent;

import com.vibe.tool.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 主智能体工厂
 * 负责创建配置好的 MasterAgent 实例
 */
@Component
public class MasterAgentFactory {

    private final StreamingChatModel streamingModel;
    private final ChatMemoryStore memoryStore;

    // 继承自氛围智能体的工具
    private final MusicTool musicTool;
    private final MusicSeedTool musicSeedTool;
    private final LightTool lightTool;
    private final ScentTool scentTool;
    private final MassageTool massageTool;

    // 主智能体专属工具
    private final SayTool sayTool;
    private final GetEnvironmentTool getEnvironmentTool;
    private final SetEnvironmentTool setEnvironmentTool;
    private final GetProjectIntroTool getProjectIntroTool;
    private final CallVibeAgentTool callVibeAgentTool;
    private final ResetVibeTool resetVibeTool;

    @Value("${vibe.master.max-messages:30}")
    private int maxMessages;

    private static final String SYSTEM_PROMPT_PATH = "prompts/master-system.txt";

    public MasterAgentFactory(
            StreamingChatModel streamingModel,
            ChatMemoryStore memoryStore,
            MusicTool musicTool,
            MusicSeedTool musicSeedTool,
            LightTool lightTool,
            ScentTool scentTool,
            MassageTool massageTool,
            SayTool sayTool,
            GetEnvironmentTool getEnvironmentTool,
            SetEnvironmentTool setEnvironmentTool,
            GetProjectIntroTool getProjectIntroTool,
            CallVibeAgentTool callVibeAgentTool,
            ResetVibeTool resetVibeTool) {
        this.streamingModel = streamingModel;
        this.memoryStore = memoryStore;
        this.musicTool = musicTool;
        this.musicSeedTool = musicSeedTool;
        this.lightTool = lightTool;
        this.scentTool = scentTool;
        this.massageTool = massageTool;
        this.sayTool = sayTool;
        this.getEnvironmentTool = getEnvironmentTool;
        this.setEnvironmentTool = setEnvironmentTool;
        this.getProjectIntroTool = getProjectIntroTool;
        this.callVibeAgentTool = callVibeAgentTool;
        this.resetVibeTool = resetVibeTool;
    }

    /**
     * 创建 MasterAgent 实例
     */
    public MasterAgent createAgent() {
        ChatMemoryProvider memoryProvider = memoryId ->
            MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(memoryStore)
                .build();

        return AiServices.builder(MasterAgent.class)
            .streamingChatModel(streamingModel)
            .chatMemoryProvider(memoryProvider)
            .systemMessageProvider(id -> loadSystemPrompt())
            .tools(
                // 继承自氛围智能体
                musicTool, musicSeedTool, lightTool, scentTool, massageTool,
                // 主智能体专属
                sayTool, getEnvironmentTool, setEnvironmentTool,
                getProjectIntroTool, callVibeAgentTool, resetVibeTool
            )
            .build();
    }

    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_PATH);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("无法加载主智能体 System Prompt: " + SYSTEM_PROMPT_PATH, e);
        }
    }
}
