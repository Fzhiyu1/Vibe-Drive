package com.vibe.agent;

import com.vibe.tool.EnvironmentGeneratorTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

/**
 * 环境生成智能体工厂
 */
@Component
public class EnvironmentAgentFactory {

    private final ChatModel chatModel;
    private final EnvironmentGeneratorTool environmentGeneratorTool;

    public EnvironmentAgentFactory(
            ChatModel chatModel,
            EnvironmentGeneratorTool environmentGeneratorTool) {
        this.chatModel = chatModel;
        this.environmentGeneratorTool = environmentGeneratorTool;
    }

    public EnvironmentAgent createAgent() {
        return AiServices.builder(EnvironmentAgent.class)
                .chatModel(chatModel)
                .systemMessageProvider(id -> getSystemPrompt())
                .tools(environmentGeneratorTool)
                .build();
    }

    private String getSystemPrompt() {
        return """
            你是一个车载环境数据生成专家。根据用户描述的驾驶场景，生成合理的车载环境数据。

            ## 任务
            分析用户描述，调用 generateEnvironment 工具生成完整的环境数据。

            ## 推理规则
            1. 从描述中提取：时间、地点、天气、情绪、乘客等信息
            2. 根据语义推断缺失信息：
               - "深夜" → timeOfDay: night/midnight, 疲劳较高
               - "高速" → gpsTag: highway, speed: 80-120
               - "堵车" → speed: 0-30, 压力较高
               - "家人出游" → passengerCount: 3-5, mood: happy
               - "雨天" → weather: rainy
               - "疲惫" → fatigueLevel: 0.6-0.9
            3. 生理数据与场景一致：
               - 疲劳：心率偏低(60-75)，疲劳水平高
               - 压力：心率偏高(85-110)，压力水平高
               - 放松：心率正常(70-85)，压力/疲劳低

            ## 输出要求
            必须调用 generateEnvironment 工具，填写所有参数。
            """;
    }
}
