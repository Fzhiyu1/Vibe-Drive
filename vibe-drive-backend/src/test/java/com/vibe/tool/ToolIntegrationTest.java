package com.vibe.tool;

import com.vibe.model.LightSetting;
import com.vibe.model.MusicRecommendation;
import com.vibe.model.Narrative;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tool 层集成测试
 */
@SpringBootTest
class ToolIntegrationTest {

    @Autowired
    private MusicTool musicTool;

    @Autowired
    private LightTool lightTool;

    @Autowired
    private NarrativeTool narrativeTool;

    @Test
    @DisplayName("MusicTool 应该正确推荐音乐")
    void musicToolShouldRecommendMusic() {
        MusicRecommendation result = musicTool.recommendMusic(
            "calm", "evening", 1, "jazz");

        assertThat(result).isNotNull();
        assertThat(result.songs()).isNotEmpty();
        assertThat(result.mood()).isEqualTo("calm");
    }

    @Test
    @DisplayName("LightTool 应该正确设置灯光")
    void lightToolShouldSetLight() {
        LightSetting result = lightTool.setLight("happy", "morning", "sunny");

        assertThat(result).isNotNull();
        assertThat(result.color()).isNotNull();
        assertThat(result.brightness()).isBetween(0, 100);
    }

    @Test
    @DisplayName("NarrativeTool 应该正确生成叙事")
    void narrativeToolShouldGenerateNarrative() {
        Narrative result = narrativeTool.generateNarrative(
            "midnight", "rainy", "highway", "tired", "夜空中最亮的星", "comfort");

        assertThat(result).isNotNull();
        assertThat(result.text()).isNotBlank();
        assertThat(result.voice()).isNotBlank();
    }

    @Test
    @DisplayName("所有 Tool 应该能协同工作")
    void allToolsShouldWorkTogether() {
        // 模拟一个完整的氛围编排场景
        String mood = "calm";
        String timeOfDay = "evening";
        String weather = "rainy";
        String gpsTag = "coastal";
        int passengerCount = 2;

        // 1. 推荐音乐
        MusicRecommendation music = musicTool.recommendMusic(
            mood, timeOfDay, passengerCount, null);
        assertThat(music.songs()).isNotEmpty();

        // 2. 设置灯光
        LightSetting light = lightTool.setLight(mood, timeOfDay, weather);
        assertThat(light).isNotNull();

        // 3. 生成叙事（使用推荐的第一首歌）
        String currentSong = music.songs().get(0).title();
        Narrative narrative = narrativeTool.generateNarrative(
            timeOfDay, weather, gpsTag, mood, currentSong, null);
        assertThat(narrative.text()).isNotBlank();
    }
}
