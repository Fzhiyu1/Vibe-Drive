package com.vibe.model.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.model.enums.GpsTag;
import com.vibe.model.enums.SafetyMode;
import com.vibe.model.enums.Weather;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SSE 事件模型序列化测试
 */
@SpringBootTest
class EventSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("TokenEvent 测试")
    class TokenEventTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            TokenEvent event = new TokenEvent("Hello");
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"content\":\"Hello\"");
            assertThat(json).contains("\"timestamp\":");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = "{\"content\":\"Hello\",\"timestamp\":\"2025-01-01T00:00:00Z\"}";
            TokenEvent event = objectMapper.readValue(json, TokenEvent.class);

            assertThat(event.content()).isEqualTo("Hello");
            assertThat(event.timestamp()).isNotNull();
        }

        @Test
        void shouldSetDefaultTimestamp() {
            TokenEvent event = new TokenEvent("Hello");
            assertThat(event.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ToolStartEvent 测试")
    class ToolStartEventTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            ToolStartEvent event = new ToolStartEvent("MusicTool", "{\"genre\":\"jazz\"}");
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"toolName\":\"MusicTool\"");
            assertThat(json).contains("\"arguments\":");
        }

        @Test
        void shouldRejectEmptyToolName() {
            assertThatThrownBy(() -> new ToolStartEvent("", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool name cannot be empty");
        }
    }

    @Nested
    @DisplayName("ToolEndEvent 测试")
    class ToolEndEventTest {
        @Test
        void shouldCreateSuccessEvent() {
            ToolEndEvent event = ToolEndEvent.success("MusicTool", "result", 100);

            assertThat(event.toolName()).isEqualTo("MusicTool");
            assertThat(event.durationMs()).isEqualTo(100);
            assertThat(event.success()).isTrue();
            assertThat(event.error()).isNull();
        }

        @Test
        void shouldCreateErrorEvent() {
            ToolEndEvent event = ToolEndEvent.error("MusicTool", "Connection failed");

            assertThat(event.toolName()).isEqualTo("MusicTool");
            assertThat(event.success()).isFalse();
            assertThat(event.error()).isEqualTo("Connection failed");
        }

        @Test
        void shouldRejectEmptyToolName() {
            assertThatThrownBy(() -> new ToolEndEvent("", "result", 100, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool name cannot be empty");
        }
    }

    @Nested
    @DisplayName("ErrorEvent 测试")
    class ErrorEventTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            ErrorEvent event = new ErrorEvent("LLM_ERROR", "Connection timeout");
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"code\":\"LLM_ERROR\"");
            assertThat(json).contains("\"message\":\"Connection timeout\"");
        }

        @Test
        void shouldCreateLlmError() {
            ErrorEvent event = ErrorEvent.llmError("API error");
            assertThat(event.code()).isEqualTo("LLM_ERROR");
            assertThat(event.message()).isEqualTo("API error");
        }

        @Test
        void shouldCreateLlmTimeout() {
            ErrorEvent event = ErrorEvent.llmTimeout();
            assertThat(event.code()).isEqualTo("LLM_TIMEOUT");
        }

        @Test
        void shouldCreateToolError() {
            ErrorEvent event = ErrorEvent.toolError("Tool failed");
            assertThat(event.code()).isEqualTo("TOOL_ERROR");
        }

        @Test
        void shouldCreateInternalError() {
            ErrorEvent event = ErrorEvent.internalError("Unexpected error");
            assertThat(event.code()).isEqualTo("INTERNAL_ERROR");
        }
    }

    @Nested
    @DisplayName("HeartbeatEvent 测试")
    class HeartbeatEventTest {
        @Test
        void shouldCreateWithDefaultTimestamp() {
            HeartbeatEvent event = new HeartbeatEvent();
            assertThat(event.timestamp()).isNotNull();
        }

        @Test
        void shouldSerializeToJson() throws Exception {
            HeartbeatEvent event = new HeartbeatEvent();
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"timestamp\":");
        }
    }

    @Nested
    @DisplayName("SafetyModeChangedEvent 测试")
    class SafetyModeChangedEventTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            SafetyModeChangedEvent event = new SafetyModeChangedEvent(
                SafetyMode.L1_NORMAL, SafetyMode.L2_FOCUS, 80);
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"previousMode\":\"L1_NORMAL\"");
            assertThat(json).contains("\"currentMode\":\"L2_FOCUS\"");
            assertThat(json).contains("\"speed\":80");
        }

        @Test
        void shouldIdentifyUpgrade() {
            SafetyModeChangedEvent upgrade = new SafetyModeChangedEvent(
                SafetyMode.L1_NORMAL, SafetyMode.L2_FOCUS, 80);
            SafetyModeChangedEvent downgrade = new SafetyModeChangedEvent(
                SafetyMode.L2_FOCUS, SafetyMode.L1_NORMAL, 50);

            assertThat(upgrade.isUpgrade()).isTrue();
            assertThat(upgrade.isDowngrade()).isFalse();
            assertThat(downgrade.isUpgrade()).isFalse();
            assertThat(downgrade.isDowngrade()).isTrue();
        }

        @Test
        void shouldGetChangeDescription() {
            SafetyModeChangedEvent event = new SafetyModeChangedEvent(
                SafetyMode.L1_NORMAL, SafetyMode.L2_FOCUS, 80);
            String description = event.getChangeDescription();

            // 使用 displayName（中文）而不是枚举名
            assertThat(description).contains("正常模式");
            assertThat(description).contains("专注模式");
            assertThat(description).contains("80");
        }
    }

    @Nested
    @DisplayName("EnvironmentUpdateEvent 测试")
    class EnvironmentUpdateEventTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            EnvironmentUpdateEvent event = new EnvironmentUpdateEvent(
                GpsTag.HIGHWAY, Weather.SUNNY, 100);
            String json = objectMapper.writeValueAsString(event);

            assertThat(json).contains("\"gpsTag\":\"highway\"");
            assertThat(json).contains("\"weather\":\"sunny\"");
            assertThat(json).contains("\"speed\":100");
        }

        @Test
        void shouldRejectInvalidSpeed() {
            assertThatThrownBy(() -> new EnvironmentUpdateEvent(GpsTag.HIGHWAY, Weather.SUNNY, 250))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Speed must be between 0 and 200");
        }

        @Test
        void shouldGetSummary() {
            EnvironmentUpdateEvent event = new EnvironmentUpdateEvent(
                GpsTag.HIGHWAY, Weather.SUNNY, 100);
            String summary = event.getSummary();

            assertThat(summary).isNotBlank();
        }
    }

    @Nested
    @DisplayName("AgentStatusChangedEvent 测试")
    class AgentStatusChangedEventTest {
        @Test
        void shouldCreateStartedEvent() {
            AgentStatusChangedEvent event = AgentStatusChangedEvent.started();

            assertThat(event.running()).isTrue();
            assertThat(event.event()).isEqualTo("started");
            assertThat(event.isStarted()).isTrue();
        }

        @Test
        void shouldCreateStoppedEvent() {
            AgentStatusChangedEvent event = AgentStatusChangedEvent.stopped();

            assertThat(event.running()).isFalse();
            assertThat(event.event()).isEqualTo("stopped");
            assertThat(event.isStopped()).isTrue();
        }

        @Test
        void shouldCreateErrorEvent() {
            AgentStatusChangedEvent event = AgentStatusChangedEvent.error("Connection lost");

            assertThat(event.running()).isFalse();
            assertThat(event.event()).isEqualTo("error");
            assertThat(event.error()).isEqualTo("Connection lost");
            assertThat(event.isError()).isTrue();
        }
    }
}
