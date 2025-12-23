package com.vibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.enums.NarrativeEmotion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Narrative 模型测试
 */
@SpringBootTest
class NarrativeTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            Narrative narrative = new Narrative("测试文本", "default", 1.0, 0.8, NarrativeEmotion.WARM);
            String json = objectMapper.writeValueAsString(narrative);

            assertThat(json).contains("\"text\":\"测试文本\"");
            assertThat(json).contains("\"voice\":\"default\"");
            assertThat(json).contains("\"emotion\":\"warm\"");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = """
                {
                    "text": "测试文本",
                    "voice": "default",
                    "speed": 1.0,
                    "volume": 0.8,
                    "emotion": "warm"
                }
                """;
            Narrative narrative = objectMapper.readValue(json, Narrative.class);

            assertThat(narrative.text()).isEqualTo("测试文本");
            assertThat(narrative.emotion()).isEqualTo(NarrativeEmotion.WARM);
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectEmptyText() {
            assertThatThrownBy(() -> new Narrative("", null, 1.0, 0.8, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Narrative text cannot be empty");
        }

        @Test
        void shouldRejectTextTooLong() {
            String longText = "a".repeat(501);
            assertThatThrownBy(() -> new Narrative(longText, null, 1.0, 0.8, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Narrative text too long");
        }

        @Test
        void shouldSetDefaultVoice() {
            Narrative narrative = new Narrative("测试", null, 1.0, 0.8, null);
            assertThat(narrative.voice()).isEqualTo("default");
        }

        @Test
        void shouldSetDefaultSpeed() {
            Narrative narrative = new Narrative("测试", null, 0, 0.8, null);
            assertThat(narrative.speed()).isEqualTo(1.0);
        }

        @Test
        void shouldSetDefaultVolume() {
            Narrative narrative = new Narrative("测试", null, 1.0, 0, null);
            assertThat(narrative.volume()).isEqualTo(0.8);
        }

        @Test
        void shouldSetDefaultEmotion() {
            Narrative narrative = new Narrative("测试", null, 1.0, 0.8, null);
            assertThat(narrative.emotion()).isEqualTo(NarrativeEmotion.CALM);
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTest {
        @Test
        void shouldCreateWithTextOnly() {
            Narrative narrative = Narrative.of("测试文本");
            assertThat(narrative.text()).isEqualTo("测试文本");
            assertThat(narrative.voice()).isEqualTo("default");
            assertThat(narrative.speed()).isEqualTo(1.0);
            assertThat(narrative.volume()).isEqualTo(0.8);
            assertThat(narrative.emotion()).isEqualTo(NarrativeEmotion.CALM);
        }

        @Test
        void shouldCreateWithTextAndEmotion() {
            Narrative narrative = Narrative.of("测试文本", NarrativeEmotion.WARM);
            assertThat(narrative.text()).isEqualTo("测试文本");
            assertThat(narrative.emotion()).isEqualTo(NarrativeEmotion.WARM);
        }
    }

    @Nested
    @DisplayName("业务方法测试")
    class BusinessMethodTest {
        @Test
        void shouldReduceVolume() {
            Narrative original = new Narrative("测试", "default", 1.0, 1.0, NarrativeEmotion.CALM);
            Narrative reduced = original.withReducedVolume();

            assertThat(reduced.volume()).isEqualTo(0.7); // 1.0 * 0.7
            assertThat(reduced.text()).isEqualTo(original.text());
        }

        @Test
        void shouldChangeEmotion() {
            Narrative original = Narrative.of("测试", NarrativeEmotion.CALM);
            Narrative changed = original.withEmotion(NarrativeEmotion.WARM);

            assertThat(changed.emotion()).isEqualTo(NarrativeEmotion.WARM);
            assertThat(changed.text()).isEqualTo(original.text());
        }

        @Test
        void shouldChangeSpeed() {
            Narrative original = Narrative.of("测试");
            Narrative changed = original.withSpeed(1.5);

            assertThat(changed.speed()).isEqualTo(1.5);
            assertThat(changed.text()).isEqualTo(original.text());
        }
    }
}
