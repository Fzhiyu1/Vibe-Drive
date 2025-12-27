package com.vibe.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 项目介绍工具
 * 获取 Vibe Drive 项目的介绍文档
 */
@Component
public class GetProjectIntroTool {

    private static final String INTRO_PATH = "docs/project-intro.md";

    @Tool("获取 Vibe Drive 项目介绍，包括功能、技术架构等信息")
    public String getProjectIntro() {
        try {
            ClassPathResource resource = new ClassPathResource(INTRO_PATH);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "无法读取项目介绍文档: " + e.getMessage();
        }
    }
}
