package com.vibe.model.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.Environment;
import com.vibe.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AnalyzeRequest 模型测试
 */
@SpringBootTest
class AnalyzeRequestTest {

    @Autowired
    private ObjectMapper objectMapper;

    private Environment createTestEnvironment() {
        return Environment.builder()
            .gpsTag(GpsTag.HIGHWAY)
            .weather(Weather.SUNNY)
            .speed(100)
            .userMood(UserMood.CALM)
            .timeOfDay(TimeOfDay.MORNING)
            .passengerCount(1)
            .routeType(RouteType.HIGHWAY)
            .build();
    }

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            AnalyzeRequest request = AnalyzeRequest.of("session-1", createTestEnvironment());
            String json = objectMapper.writeValueAsString(request);

            assertThat(json).contains("\"sessionId\":\"session-1\"");
            assertThat(json).contains("\"environment\":");
            assertThat(json).contains("\"async\":false");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = """
                {
                    "sessionId": "session-1",
                    "environment": {
                        "gpsTag": "highway",
                        "weather": "sunny",
                        "speed": 100,
                        "userMood": "calm",
                        "timeOfDay": "morning",
                        "passengerCount": 1,
                        "routeType": "highway"
                    },
                    "async": false
                }
                """;
            AnalyzeRequest request = objectMapper.readValue(json, AnalyzeRequest.class);

            assertThat(request.sessionId()).isEqualTo("session-1");
            assertThat(request.environment().gpsTag()).isEqualTo(GpsTag.HIGHWAY);
            assertThat(request.async()).isFalse();
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectEmptySessionId() {
            assertThatThrownBy(() -> new AnalyzeRequest("", createTestEnvironment(), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session ID cannot be empty");
        }

        @Test
        void shouldRejectNullSessionId() {
            assertThatThrownBy(() -> new AnalyzeRequest(null, createTestEnvironment(), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session ID cannot be empty");
        }

        @Test
        void shouldRejectNullEnvironment() {
            assertThatThrownBy(() -> new AnalyzeRequest("session-1", null, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Environment cannot be null");
        }

        @Test
        void shouldMakePreferencesImmutable() {
            Map<String, Object> prefs = new java.util.HashMap<>();
            prefs.put("key", "value");
            AnalyzeRequest request = new AnalyzeRequest("session-1", createTestEnvironment(), prefs, false);

            assertThatThrownBy(() -> request.preferences().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTest {
        @Test
        void shouldCreateWithMinimalParams() {
            AnalyzeRequest request = AnalyzeRequest.of("session-1", createTestEnvironment());

            assertThat(request.sessionId()).isEqualTo("session-1");
            assertThat(request.environment()).isNotNull();
            assertThat(request.preferences()).isNull();
            assertThat(request.async()).isFalse();
        }

        @Test
        void shouldCreateWithPreferences() {
            Map<String, Object> prefs = Map.of("musicGenre", "jazz");
            AnalyzeRequest request = AnalyzeRequest.of("session-1", createTestEnvironment(), prefs);

            assertThat(request.preferences()).containsEntry("musicGenre", "jazz");
        }
    }

    @Nested
    @DisplayName("业务方法测试")
    class BusinessMethodTest {
        @Test
        void shouldCheckHasPreferences() {
            AnalyzeRequest withPrefs = AnalyzeRequest.of("s1", createTestEnvironment(), Map.of("key", "value"));
            AnalyzeRequest withoutPrefs = AnalyzeRequest.of("s2", createTestEnvironment());
            AnalyzeRequest withEmptyPrefs = new AnalyzeRequest("s3", createTestEnvironment(), Map.of(), false);

            assertThat(withPrefs.hasPreferences()).isTrue();
            assertThat(withoutPrefs.hasPreferences()).isFalse();
            assertThat(withEmptyPrefs.hasPreferences()).isFalse();
        }

        @Test
        void shouldGetPreference() {
            AnalyzeRequest request = AnalyzeRequest.of("s1", createTestEnvironment(), Map.of("genre", "jazz"));

            assertThat(request.<String>getPreference("genre", "default")).isEqualTo("jazz");
            assertThat(request.<String>getPreference("missing", "default")).isEqualTo("default");
        }
    }
}
