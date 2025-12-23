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
 * BpmRange 模型测试
 */
@SpringBootTest
class BpmRangeTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("JSON 序列化测试")
    class JsonSerializationTest {
        @Test
        void shouldSerializeToJson() throws Exception {
            BpmRange range = new BpmRange(80, 120);
            String json = objectMapper.writeValueAsString(range);

            assertThat(json).contains("\"min\":80");
            assertThat(json).contains("\"max\":120");
        }

        @Test
        void shouldDeserializeFromJson() throws Exception {
            String json = "{\"min\":80,\"max\":120}";
            BpmRange range = objectMapper.readValue(json, BpmRange.class);

            assertThat(range.min()).isEqualTo(80);
            assertThat(range.max()).isEqualTo(120);
        }
    }

    @Nested
    @DisplayName("紧凑构造器校验测试")
    class ValidationTest {
        @Test
        void shouldRejectNegativeMin() {
            assertThatThrownBy(() -> new BpmRange(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min BPM must be non-negative");
        }

        @Test
        void shouldRejectMaxLessThanMin() {
            assertThatThrownBy(() -> new BpmRange(100, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max BPM must be greater than or equal to min BPM");
        }

        @Test
        void shouldAcceptEqualMinMax() {
            BpmRange range = new BpmRange(100, 100);
            assertThat(range.min()).isEqualTo(100);
            assertThat(range.max()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTest {
        @Test
        void shouldCreateSlowRange() {
            BpmRange slow = BpmRange.slow();
            assertThat(slow.min()).isEqualTo(60);
            assertThat(slow.max()).isEqualTo(80);
        }

        @Test
        void shouldCreateModerateRange() {
            BpmRange moderate = BpmRange.moderate();
            assertThat(moderate.min()).isEqualTo(80);
            assertThat(moderate.max()).isEqualTo(120);
        }

        @Test
        void shouldCreateFastRange() {
            BpmRange fast = BpmRange.fast();
            assertThat(fast.min()).isEqualTo(120);
            assertThat(fast.max()).isEqualTo(160);
        }
    }

    @Nested
    @DisplayName("业务方法测试")
    class BusinessMethodTest {
        @Test
        void shouldCheckContains() {
            BpmRange range = new BpmRange(80, 120);

            assertThat(range.contains(80)).isTrue();
            assertThat(range.contains(100)).isTrue();
            assertThat(range.contains(120)).isTrue();
            assertThat(range.contains(79)).isFalse();
            assertThat(range.contains(121)).isFalse();
        }
    }
}
