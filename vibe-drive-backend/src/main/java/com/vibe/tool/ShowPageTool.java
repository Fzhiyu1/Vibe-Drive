package com.vibe.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 展示页面工具
 * 仅在用户明确要求时使用
 */
@Component
public class ShowPageTool {

    @Tool("""
        展示详细内容弹窗。

        【使用原则】
        仅在用户明确表达想要查看详细内容时使用。
        用户需要主动请求查看，而非你主动展示。

        内容类型：
        - document: 文档/说明（支持 Markdown）
        - code: 代码展示（支持语法高亮）
        - table: 表格数据
        """)
    public String showPage(
            @P("弹窗标题") String title,
            @P("内容，支持 Markdown 格式") String content,
            @P("内容类型: document/code/table") String type
    ) {
        // 返回简单确认，实际展示由前端处理
        return "已展示";
    }
}
