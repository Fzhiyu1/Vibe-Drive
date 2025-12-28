package com.vibe.agent;

import com.vibe.tool.EnvironmentGeneratorTool;
import com.vibe.tool.GeocodingTool;
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
    private final GeocodingTool geocodingTool;

    public EnvironmentAgentFactory(
            ChatModel chatModel,
            EnvironmentGeneratorTool environmentGeneratorTool,
            GeocodingTool geocodingTool) {
        this.chatModel = chatModel;
        this.environmentGeneratorTool = environmentGeneratorTool;
        this.geocodingTool = geocodingTool;
    }

    public EnvironmentAgent createAgent() {
        return AiServices.builder(EnvironmentAgent.class)
                .chatModel(chatModel)
                .systemMessageProvider(id -> getSystemPrompt())
                .tools(geocodingTool, environmentGeneratorTool)
                .build();
    }

    private String getSystemPrompt() {
        return """
            你是车载环境数据生成器。

            ## 工作流程（必须按顺序执行）
            1. 如果用户提到了具体地点（城市、景点、道路），先调用 searchLocation 获取坐标
            2. 然后调用 generateEnvironment 生成环境数据，使用获取到的坐标

            ## 重要规则
            - 禁止返回任何文字说明或解释
            - 禁止询问用户更多信息
            - 如果描述不完整，自行推断合理的默认值
            - 如果 searchLocation 失败，使用默认坐标（上海 31.23, 121.47）

            ## 参数推断规则
            时间：深夜→midnight, 夜晚→night, 早上→morning, 中午→noon, 下午→afternoon, 傍晚→evening
            地点：高速→highway, 隧道→tunnel, 城市→urban, 郊区→suburban, 山区→mountain, 海边→coastal
            天气：雨→rainy, 雪→snowy, 雾→foggy, 阴→cloudy, 默认→sunny
            情绪：疲惫→tired, 压力→stressed, 开心→happy, 兴奋→excited, 默认→calm
            速度：高速90-110, 城市40-60, 堵车5-20
            乘客：独自→1, 家人→3-4, 朋友→2-3

            立即执行！
            """;
    }
}
