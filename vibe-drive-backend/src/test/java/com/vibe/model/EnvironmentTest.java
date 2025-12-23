package com.vibe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Environment 模型测试
 */
@SpringBootTest
class EnvironmentTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            Environment env = Environment.builder()
                .gpsTag(GpsTag.HIGHWAY)
                .weather(Weather.SUNNY)
                .speed(100)
                .userMood(UserMood.CALM)
                .timeOfDay(TimeOfDay.MORNING)
                .passengerCount(2)
                .routeType(RouteType.HIGHWAY)
                .build();

            String json = objectMapper.writeValueAsString(env);

            // 验证枚举使用小写
            assertThat(json).contains("\"gpsTag\":\"highway\"");
            assertThat(json).contains("\"weather\":\"sunny\"");
            assertThat(json).contains("\"userMood\":\"calm\"");
            assertThat(json).contains("\"speed\":100");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = """
                {
                    "gpsTag": "highway",
                    "weather": "sunny",
                    "speed": 100,
                    "userMood": "calm",
                    "timeOfDay": "morning",
                    "passengerCount": 2,
                    "routeType": "highway"
                }
                """;

            Environment env = objectMapper.readValue(json, Environment.class);

            assertThat(env.gpsTag()).isEqualTo(GpsTag.HIGHWAY);
            assertThat(env.weather()).isEqualTo(Weather.SUNNY);
            assertThat(env.speed()).isEqualTo(100);
            assertThat(env.passengerCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectNegativeSpeed() {
            assertThatThrownBy(() -> Environment.builder().speed(-1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Speed must be between 0 and 200");
        }

        @Test
        void shouldRejectSpeedOver200() {
            assertThatThrownBy(() -> Environment.builder().speed(250).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Speed must be between 0 and 200");
        }

        @Test
        void shouldRejectPassengerCountLessThan1() {
            assertThatThrownBy(() -> Environment.builder().passengerCount(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passenger count must be between 1 and 7");
        }

        @Test
        void shouldRejectPassengerCountOver7() {
            assertThatThrownBy(() -> Environment.builder().passengerCount(8).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passenger count must be between 1 and 7");
        }

        @Test
        void shouldSetDefaultTimestamp() {
            Environment env = Environment.builder().build();
            assertThat(env.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("业务方法测试")
    class BusinessMethodTest {
        @Test
        void shouldCalculateSafetyMode() {
            Environment parked = Environment.builder().speed(0).build();
            Environment cruising = Environment.builder().speed(50).build();
            Environment focus = Environment.builder().speed(80).build();
            Environment highSpeed = Environment.builder().speed(120).build();

            assertThat(parked.getSafetyMode()).isEqualTo(SafetyMode.L1_NORMAL);
            assertThat(cruising.getSafetyMode()).isEqualTo(SafetyMode.L1_NORMAL);
            assertThat(focus.getSafetyMode()).isEqualTo(SafetyMode.L2_FOCUS);
            assertThat(highSpeed.getSafetyMode()).isEqualTo(SafetyMode.L3_SILENT);
        }

        @Test
        void shouldIdentifyHighSpeedScenario() {
            Environment highSpeed = Environment.builder().speed(100).build();
            Environment highway = Environment.builder().gpsTag(GpsTag.HIGHWAY).speed(60).build();
            Environment urban = Environment.builder().gpsTag(GpsTag.URBAN).speed(60).build();

            assertThat(highSpeed.isHighSpeedScenario()).isTrue();
            assertThat(highway.isHighSpeedScenario()).isTrue();
            assertThat(urban.isHighSpeedScenario()).isFalse();
        }

        @Test
        void shouldIdentifySevereWeather() {
            Environment rainy = Environment.builder().weather(Weather.RAINY).build();
            Environment sunny = Environment.builder().weather(Weather.SUNNY).build();

            assertThat(rainy.isSevereWeather()).isTrue();
            assertThat(sunny.isSevereWeather()).isFalse();
        }

        @Test
        void shouldIdentifyLateNight() {
            Environment night = Environment.builder().timeOfDay(TimeOfDay.NIGHT).build();
            Environment morning = Environment.builder().timeOfDay(TimeOfDay.MORNING).build();

            assertThat(night.isLateNight()).isTrue();
            assertThat(morning.isLateNight()).isFalse();
        }

        @Test
        void shouldIdentifySoloDriving() {
            Environment solo = Environment.builder().passengerCount(1).build();
            Environment withPassengers = Environment.builder().passengerCount(3).build();

            assertThat(solo.isSoloDriving()).isTrue();
            assertThat(withPassengers.isSoloDriving()).isFalse();
        }
    }
}
