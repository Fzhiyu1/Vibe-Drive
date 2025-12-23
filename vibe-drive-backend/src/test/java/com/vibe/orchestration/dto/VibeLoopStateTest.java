package com.vibe.orchestration.dto;

import com.vibe.model.enums.SafetyMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VibeLoopState 单元测试
 */
@DisplayName("VibeLoopState 测试")
class VibeLoopStateTest {

    @Test
    @DisplayName("创建新轮次状态")
    void shouldCreateNewTurn() {
        VibeLoopState state = VibeLoopState.newTurn("session-1", SafetyMode.L1_NORMAL);

        assertThat(state.turnId()).isNotNull();
        assertThat(state.sessionId()).isEqualTo("session-1");
        assertThat(state.depth()).isEqualTo(0);
        assertThat(state.toolCallCount()).isEqualTo(0);
        assertThat(state.startTime()).isNotNull();
        assertThat(state.safetyMode()).isEqualTo(SafetyMode.L1_NORMAL);
    }

    @Test
    @DisplayName("增加递归深度返回新实例")
    void shouldIncrementDepthImmutably() {
        VibeLoopState original = VibeLoopState.newTurn("session-1", SafetyMode.L1_NORMAL);
        VibeLoopState incremented = original.incrementDepth();

        // 原实例不变
        assertThat(original.depth()).isEqualTo(0);
        // 新实例深度+1
        assertThat(incremented.depth()).isEqualTo(1);
        // 其他字段保持不变
        assertThat(incremented.turnId()).isEqualTo(original.turnId());
        assertThat(incremented.sessionId()).isEqualTo(original.sessionId());
    }

    @Test
    @DisplayName("增加工具调用计数返回新实例")
    void shouldIncrementToolCallCountImmutably() {
        VibeLoopState original = VibeLoopState.newTurn("session-1", SafetyMode.L2_FOCUS);
        VibeLoopState incremented = original.incrementToolCallCount();

        assertThat(original.toolCallCount()).isEqualTo(0);
        assertThat(incremented.toolCallCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取已运行时间")
    void shouldCalculateElapsedTime() throws InterruptedException {
        VibeLoopState state = VibeLoopState.newTurn("session-1", SafetyMode.L1_NORMAL);
        Thread.sleep(10);

        long elapsed = state.getElapsedMillis();
        assertThat(elapsed).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("链式调用多次增加")
    void shouldSupportChainedIncrements() {
        VibeLoopState state = VibeLoopState.newTurn("session-1", SafetyMode.L1_NORMAL)
                .incrementDepth()
                .incrementToolCallCount()
                .incrementDepth()
                .incrementToolCallCount();

        assertThat(state.depth()).isEqualTo(2);
        assertThat(state.toolCallCount()).isEqualTo(2);
    }
}
