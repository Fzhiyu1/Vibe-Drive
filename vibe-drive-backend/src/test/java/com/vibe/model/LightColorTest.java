package com.vibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LightColor 模型测试
 */
@SpringBootTest
class LightColorTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            LightColor color = new LightColor("#FFE4B5", 2700);
            String json = objectMapper.writeValueAsString(color);

            assertThat(json).contains("\"hex\":\"#FFE4B5\"");
            assertThat(json).contains("\"temperature\":2700");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = "{\"hex\":\"#FFE4B5\",\"temperature\":2700}";
            LightColor color = objectMapper.readValue(json, LightColor.class);

            assertThat(color.hex()).isEqualTo("#FFE4B5");
            assertThat(color.temperature()).isEqualTo(2700);
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectInvalidHexFormat() {
            assertThatThrownBy(() -> new LightColor("invalid", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid hex color format");
        }

        @Test
        void shouldRejectTemperatureTooLow() {
            assertThatThrownBy(() -> new LightColor(null, 2000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature must be between 2700 and 6500");
        }

        @Test
        void shouldRejectTemperatureTooHigh() {
            assertThatThrownBy(() -> new LightColor(null, 7000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature must be between 2700 and 6500");
        }

        @Test
        void shouldAcceptValidHex() {
            LightColor color = new LightColor("#AABBCC", null);
            assertThat(color.hex()).isEqualTo("#AABBCC");
        }

        @Test
        void shouldAcceptLowercaseHex() {
            LightColor color = new LightColor("#aabbcc", null);
            assertThat(color.hex()).isEqualTo("#aabbcc");
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTest {
        @Test
        void shouldCreateWarmWhite() {
            LightColor warmWhite = LightColor.warmWhite();
            assertThat(warmWhite.hex()).isEqualTo("#FFE4B5");
            assertThat(warmWhite.temperature()).isEqualTo(2700);
        }

        @Test
        void shouldCreateCoolWhite() {
            LightColor coolWhite = LightColor.coolWhite();
            assertThat(coolWhite.hex()).isEqualTo("#F0F8FF");
            assertThat(coolWhite.temperature()).isEqualTo(6500);
        }

        @Test
        void shouldCreateAmber() {
            LightColor amber = LightColor.amber();
            assertThat(amber.hex()).isEqualTo("#FFBF00");
            assertThat(amber.temperature()).isEqualTo(2700);
        }

        @Test
        void shouldCreateOceanBlue() {
            LightColor oceanBlue = LightColor.oceanBlue();
            assertThat(oceanBlue.hex()).isEqualTo("#006994");
            assertThat(oceanBlue.temperature()).isEqualTo(5000);
        }

        @Test
        void shouldCreateSunsetOrange() {
            LightColor sunsetOrange = LightColor.sunsetOrange();
            assertThat(sunsetOrange.hex()).isEqualTo("#FF4500");
            assertThat(sunsetOrange.temperature()).isEqualTo(3000);
        }

        @Test
        void shouldCreateFromHex() {
            LightColor color = LightColor.fromHex("#123456");
            assertThat(color.hex()).isEqualTo("#123456");
            assertThat(color.temperature()).isNull();
        }

        @Test
        void shouldCreateFromTemperature() {
            LightColor color = LightColor.fromTemperature(4000);
            assertThat(color.hex()).isNull();
            assertThat(color.temperature()).isEqualTo(4000);
        }
    }
}
