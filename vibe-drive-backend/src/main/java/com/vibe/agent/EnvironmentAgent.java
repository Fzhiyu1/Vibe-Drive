package com.vibe.agent;

import com.vibe.model.Environment;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 环境生成智能体接口
 * 根据用户的自然语言描述生成车载环境数据
 */
public interface EnvironmentAgent {

    @UserMessage("{{prompt}}")
    Environment generate(@V("prompt") String prompt);
}
