package com.vibe.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 展示页面工具
 * 打开弹窗，返回 pageId，后续用 appendPage 追加内容
 */
@Component
public class ShowPageTool {

    @Tool("""
        打开一个内容弹窗，返回 pageId。

        【重要】这个工具只负责打开弹窗，不传递内容。
        打开后，使用 appendPage 工具分段追加内容。

        内容类型：
        - document: 文档/说明（支持 Markdown）
        - code: 代码展示（支持语法高亮）
        - table: 表格数据
        """)
    public String showPage(
            @P("弹窗标题") String title,
            @P("内容类型: document/code/table") String type
    ) {
        String pageId = UUID.randomUUID().toString().substring(0, 8);
        return pageId;
    }
}
