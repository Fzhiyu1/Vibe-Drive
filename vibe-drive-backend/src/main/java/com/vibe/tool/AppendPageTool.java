package com.vibe.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 追加页面内容工具
 * 配合 showPage 使用，流式追加内容到弹窗
 */
@Component
public class AppendPageTool {

    @Tool("""
        向已打开的弹窗追加内容。

        【使用方式】
        1. 先用 showPage 打开弹窗，获取 pageId
        2. 多次调用 appendPage 分段追加内容
        3. 每次追加一小段（1-3 段落），实现流式效果
        """)
    public String appendPage(
            @P("showPage 返回的页面ID") String pageId,
            @P("要追加的内容片段，支持 Markdown") String content
    ) {
        return "ok";
    }
}
