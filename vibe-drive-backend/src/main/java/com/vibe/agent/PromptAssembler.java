package com.vibe.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Prompt 组装器
 * 负责加载和组装 System Prompt 和 User Prompt
 */
@Component
public class PromptAssembler {

    private final ObjectMapper objectMapper;

    @Value("${vibe.prompt.system-file:prompts/vibe-system.txt}")
    private String systemPromptFile;

    private String systemPrompt;

    public PromptAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadSystemPrompt();
    }

    /**
     * 加载 System Prompt
     */
    private void loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(systemPromptFile);
            systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 使用默认 Prompt
            systemPrompt = getDefaultSystemPrompt();
        }
    }

    /**
     * 获取 System Prompt
     */
    public String assembleSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 组装 User Prompt（包含环境数据）
     */
    public String assembleUserPrompt(Environment environment, String userPreferences) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下车载环境数据，并编排合适的氛围方案：\n\n");
        sb.append("## 当前环境\n```json\n");

        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(environment));
        } catch (Exception e) {
            sb.append("{}");
        }

        sb.append("\n```\n");

        if (userPreferences != null && !userPreferences.isBlank()) {
            sb.append("\n## 用户偏好\n");
            sb.append(userPreferences);
            sb.append("\n");
        }

        sb.append("\n请根据环境数据和安全模式规则，调用合适的工具生成氛围编排方案。");

        return sb.toString();
    }

    /**
     * 默认 System Prompt
     */
    private String getDefaultSystemPrompt() {
        return """
            你是 Vibe Drive 氛围编排智能体，负责根据车载环境数据编排合适的氛围方案。

            ## 可用工具
            - recommendMusic: 根据情绪、时段、乘客数推荐音乐
            - setLight: 根据情绪、时段、天气设置氛围灯
            - generateNarrative: 生成 TTS 播报文本

            ## 安全模式规则
            - L1 正常模式 (speed < 60): 全功能
            - L2 专注模式 (60-100): 禁用灯光动效
            - L3 静默模式 (≥100): 不主动推荐

            ## 输出要求
            根据环境数据调用合适的工具，生成氛围编排方案。
            """;
    }
}
