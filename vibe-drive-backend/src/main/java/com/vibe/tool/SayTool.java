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
        // 返回简单确认，避免 AI 把返回值当作需要继续处理的内容
        return "已播放";
    }
}
