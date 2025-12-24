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

            ## 可用工具（必须全部调用）
            1. recommendMusic: 根据情绪、时段、乘客数推荐音乐
            2. setLight: 根据情绪、时段、天气设置氛围灯
            3. setScent: 设置车内香氛（薰衣草放松、薄荷提神、海洋清新等）
            4. setMassage: 设置座椅按摩（放松/活力/舒适模式）
            5. generateNarrative: 生成 TTS 播报文本

            ## 重要：你必须调用以上全部5个工具来生成完整的氛围方案！

            ## 安全模式规则
            - L1 正常模式 (speed < 60): 全功能
            - L2 专注模式 (60-100): 禁用灯光动效，按摩降低强度
            - L3 静默模式 (≥100): 不主动推荐

            ## 工具使用建议
            - 疲劳驾驶：薄荷香氛提神 + 放松按摩缓解疲劳
            - 压力大：薰衣草香氛 + 舒适按摩
            - 长途驾驶：森林/海洋香氛 + 腰部按摩
            - 深夜驾驶：香氛强度降低，按摩轻柔

            ## 输出要求
            必须依次调用所有5个工具，生成完整的氛围编排方案。
            """;
    }
}
