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
            你是车载环境数据生成器。你的唯一任务是调用 generateEnvironment 工具。

            ## 重要规则
            - 你必须且只能调用 generateEnvironment 工具
            - 禁止返回任何文字说明或解释
            - 禁止询问用户更多信息
            - 如果描述不完整，自行推断合理的默认值

            ## 参数推断规则
            时间推断：
            - "深夜/凌晨" → midnight, "夜晚" → night
            - "早上/早晨" → morning, "中午" → noon
            - "下午" → afternoon, "傍晚" → evening
            - 默认 → morning

            地点推断：
            - "高速/高速公路" → highway, "隧道" → tunnel
            - "城市/市区" → urban, "郊区" → suburban
            - "山路/山区" → mountain, "海边/沿海" → coastal
            - 默认 → urban

            天气推断：
            - "雨/下雨" → rainy, "雪" → snowy
            - "雾" → foggy, "阴天" → cloudy
            - 默认 → sunny

            情绪推断：
            - "疲惫/累" → tired, "压力/焦虑" → stressed
            - "开心/愉快" → happy, "兴奋" → excited
            - 默认 → calm

            速度推断：
            - 高速公路 → 90-110, 城市道路 → 40-60
            - 堵车 → 5-20, 停车 → 0
            - 默认 → 60

            乘客推断：
            - "独自/一个人" → 1, "家人/家庭" → 3-4
            - "朋友" → 2-3
            - 默认 → 1

            生理数据推断：
            - 疲劳场景：心率 65-75, 压力 0.4-0.6, 疲劳 0.6-0.8
            - 压力场景：心率 85-100, 压力 0.7-0.9, 疲劳 0.3-0.5
            - 放松场景：心率 70-80, 压力 0.2-0.4, 疲劳 0.1-0.3

            立即调用 generateEnvironment 工具！
            """;
    }
}
