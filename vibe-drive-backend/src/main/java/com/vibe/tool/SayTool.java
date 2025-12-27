package com.vibe.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 语音输出工具
 * 只有调用此工具的内容才会被转为语音播放
 */
@Component
public class SayTool {

    @Tool("对用户说话，会转为语音播放。只在需要回复用户时调用。")
    public String say(@P("要说的内容") String text) {
        return text;
    }
}
