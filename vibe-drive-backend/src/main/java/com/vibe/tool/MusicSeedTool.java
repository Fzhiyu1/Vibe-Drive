package com.vibe.tool;

import com.vibe.service.MusicVariationService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 音乐种子工具
 * 提供随机音乐灵感种子，增加选歌多样性
 */
@Component
public class MusicSeedTool {

    private final MusicVariationService variationService;

    public MusicSeedTool(MusicVariationService variationService) {
        this.variationService = variationService;
    }

    @Tool("""
        获取音乐灵感种子。当用户没有指定音乐风格时，必须先调用此工具。

        返回 5 个随机种子（如"爵士抒情"、"英文流行"）。

        重要：你必须严格基于这些种子选歌！
        - 每个种子必须选 2-3 首具体歌名
        - 不要忽略种子，不要自由发挥
        - 搜索关键词必须是具体歌名，不是描述词
        """)
    public List<String> getMusicSeeds() {
        return variationService.generateSeeds(5);
    }
}
