package com.vibe.model.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 枚举 JSON 序列化测试
 * 验证所有枚举使用小写 JSON 值（@JsonValue/@JsonCreator）
 */
@SpringBootTest
class EnumSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GpsTag 枚举测试")
    class GpsTagTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(GpsTag.HIGHWAY);
            assertThat(json).isEqualTo("\"highway\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            GpsTag tag = objectMapper.readValue("\"highway\"", GpsTag.class);
            assertThat(tag).isEqualTo(GpsTag.HIGHWAY);
        }

        @Test
        void shouldDeserializeCaseInsensitive() throws Exception {
            GpsTag tag = objectMapper.readValue("\"HIGHWAY\"", GpsTag.class);
            assertThat(tag).isEqualTo(GpsTag.HIGHWAY);
        }

        @Test
        void shouldThrowOnUnknownValue() {
            assertThatThrownBy(() -> objectMapper.readValue("\"unknown\"", GpsTag.class))
                .hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Weather 枚举测试")
    class WeatherTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(Weather.RAINY);
            assertThat(json).isEqualTo("\"rainy\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            Weather weather = objectMapper.readValue("\"rainy\"", Weather.class);
            assertThat(weather).isEqualTo(Weather.RAINY);
        }

        @Test
        void shouldIdentifySevereWeather() {
            assertThat(Weather.RAINY.isSevereWeather()).isTrue();
            assertThat(Weather.SNOWY.isSevereWeather()).isTrue();
            assertThat(Weather.FOGGY.isSevereWeather()).isTrue();
            assertThat(Weather.SUNNY.isSevereWeather()).isFalse();
            assertThat(Weather.CLOUDY.isSevereWeather()).isFalse();
        }
    }

    @Nested
    @DisplayName("UserMood 枚举测试")
    class UserMoodTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(UserMood.HAPPY);
            assertThat(json).isEqualTo("\"happy\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            UserMood mood = objectMapper.readValue("\"happy\"", UserMood.class);
            assertThat(mood).isEqualTo(UserMood.HAPPY);
        }

        @Test
        void shouldIdentifyMoodRequirements() {
            assertThat(UserMood.TIRED.needsSoothingAmbience()).isTrue();
            assertThat(UserMood.STRESSED.needsSoothingAmbience()).isTrue();
            assertThat(UserMood.HAPPY.needsEnergeticAmbience()).isTrue();
            assertThat(UserMood.EXCITED.needsEnergeticAmbience()).isTrue();
        }
    }

    @Nested
    @DisplayName("TimeOfDay 枚举测试")
    class TimeOfDayTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(TimeOfDay.MORNING);
            assertThat(json).isEqualTo("\"morning\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            TimeOfDay time = objectMapper.readValue("\"morning\"", TimeOfDay.class);
            assertThat(time).isEqualTo(TimeOfDay.MORNING);
        }

        @Test
        void shouldIdentifyLateNight() {
            assertThat(TimeOfDay.NIGHT.isLateNight()).isTrue();
            assertThat(TimeOfDay.MIDNIGHT.isLateNight()).isTrue();
            assertThat(TimeOfDay.MORNING.isLateNight()).isFalse();
        }
    }

    @Nested
    @DisplayName("RouteType 枚举测试")
    class RouteTypeTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(RouteType.HIGHWAY);
            assertThat(json).isEqualTo("\"highway\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            RouteType route = objectMapper.readValue("\"highway\"", RouteType.class);
            assertThat(route).isEqualTo(RouteType.HIGHWAY);
        }
    }

    @Nested
    @DisplayName("SafetyMode 枚举测试")
    class SafetyModeTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(SafetyMode.L1_NORMAL);
            assertThat(json).isEqualTo("\"L1_NORMAL\"");
        }

        @Test
        void shouldDeserializeFromValue() throws Exception {
            SafetyMode mode = objectMapper.readValue("\"L1_NORMAL\"", SafetyMode.class);
            assertThat(mode).isEqualTo(SafetyMode.L1_NORMAL);
        }

        @Test
        void shouldDetermineFromSpeed() {
            // L1_NORMAL: speed < 60
            assertThat(SafetyMode.fromSpeed(0)).isEqualTo(SafetyMode.L1_NORMAL);
            assertThat(SafetyMode.fromSpeed(50)).isEqualTo(SafetyMode.L1_NORMAL);
            assertThat(SafetyMode.fromSpeed(59)).isEqualTo(SafetyMode.L1_NORMAL);
            // L2_FOCUS: 60 <= speed < 100
            assertThat(SafetyMode.fromSpeed(60)).isEqualTo(SafetyMode.L2_FOCUS);
            assertThat(SafetyMode.fromSpeed(80)).isEqualTo(SafetyMode.L2_FOCUS);
            assertThat(SafetyMode.fromSpeed(99)).isEqualTo(SafetyMode.L2_FOCUS);
            // L3_SILENT: speed >= 100
            assertThat(SafetyMode.fromSpeed(100)).isEqualTo(SafetyMode.L3_SILENT);
            assertThat(SafetyMode.fromSpeed(120)).isEqualTo(SafetyMode.L3_SILENT);
            assertThat(SafetyMode.fromSpeed(150)).isEqualTo(SafetyMode.L3_SILENT);
        }
    }

    @Nested
    @DisplayName("LightMode 枚举测试")
    class LightModeTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(LightMode.BREATHING);
            assertThat(json).isEqualTo("\"breathing\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            LightMode mode = objectMapper.readValue("\"breathing\"", LightMode.class);
            assertThat(mode).isEqualTo(LightMode.BREATHING);
        }

        @Test
        void shouldIdentifyDynamicModes() {
            // 只有 GRADIENT 和 PULSE 是动态效果
            assertThat(LightMode.STATIC.isDynamic()).isFalse();
            assertThat(LightMode.BREATHING.isDynamic()).isFalse();
            assertThat(LightMode.GRADIENT.isDynamic()).isTrue();
            assertThat(LightMode.PULSE.isDynamic()).isTrue();
        }
    }

    @Nested
    @DisplayName("NarrativeEmotion 枚举测试")
    class NarrativeEmotionTest {
        @Test
        void shouldSerializeToLowercase() throws Exception {
            String json = objectMapper.writeValueAsString(NarrativeEmotion.WARM);
            assertThat(json).isEqualTo("\"warm\"");
        }

        @Test
        void shouldDeserializeFromLowercase() throws Exception {
            NarrativeEmotion emotion = objectMapper.readValue("\"warm\"", NarrativeEmotion.class);
            assertThat(emotion).isEqualTo(NarrativeEmotion.WARM);
        }
    }

    @Nested
    @DisplayName("AnalyzeAction 枚举测试")
    class AnalyzeActionTest {
        @Test
        void shouldSerializeToUppercase() throws Exception {
            String json = objectMapper.writeValueAsString(AnalyzeAction.APPLY);
            assertThat(json).isEqualTo("\"APPLY\"");
        }

        @Test
        void shouldDeserializeFromValue() throws Exception {
            AnalyzeAction action = objectMapper.readValue("\"APPLY\"", AnalyzeAction.class);
            assertThat(action).isEqualTo(AnalyzeAction.APPLY);
        }

        @Test
        void shouldIdentifyUiUpdateRequirement() {
            assertThat(AnalyzeAction.APPLY.requiresUiUpdate()).isTrue();
            assertThat(AnalyzeAction.NO_ACTION.requiresUiUpdate()).isFalse();
        }
    }

    @Nested
    @DisplayName("NotificationType 枚举测试")
    class NotificationTypeTest {
        @Test
        void shouldSerializeToSnakeCase() throws Exception {
            String json = objectMapper.writeValueAsString(NotificationType.AMBIENCE_CHANGED);
            assertThat(json).isEqualTo("\"ambience_changed\"");
        }

        @Test
        void shouldDeserializeFromSnakeCase() throws Exception {
            NotificationType type = objectMapper.readValue("\"ambience_changed\"", NotificationType.class);
            assertThat(type).isEqualTo(NotificationType.AMBIENCE_CHANGED);
        }

        @Test
        void shouldIdentifyStateChangeTypes() {
            assertThat(NotificationType.AMBIENCE_CHANGED.isStateChange()).isTrue();
            assertThat(NotificationType.SAFETY_MODE_CHANGED.isStateChange()).isTrue();
            assertThat(NotificationType.AGENT_STARTED.isStateChange()).isFalse();
        }

        @Test
        void shouldIdentifyLifecycleTypes() {
            assertThat(NotificationType.AGENT_STARTED.isLifecycleEvent()).isTrue();
            assertThat(NotificationType.AGENT_STOPPED.isLifecycleEvent()).isTrue();
            assertThat(NotificationType.AMBIENCE_CHANGED.isLifecycleEvent()).isFalse();
        }
    }
}
