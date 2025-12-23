<script setup lang="ts">
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

function toggleExpand() {
  store.chainExpanded = !store.chainExpanded
}

function getStepIcon(type: string) {
  switch (type) {
    case 'thinking': return '🤔'
    case 'tool_start': return '🔧'
    case 'tool_end': return '✅'
    case 'complete': return '🎉'
    case 'error': return '❌'
    default: return '•'
  }
}
</script>

<template>
  <div class="thinking-chain">
    <div class="header" @click="toggleExpand">
      <span class="title">Agent 思维链</span>
      <span v-if="store.agentRunning" class="status running">运行中...</span>
      <span class="toggle">{{ store.chainExpanded ? '▼' : '▲' }}</span>
    </div>

    <div v-if="store.chainExpanded" class="chain-content">
      <div
        v-for="(step, index) in store.thinkingChain"
        :key="index"
        class="step"
        :class="step.type"
      >
        <span class="icon">{{ getStepIcon(step.type) }}</span>
        <span class="content">{{ step.content }}</span>
      </div>

      <div v-if="store.thinkingChain.length === 0" class="empty">
        暂无思维链数据
      </div>
    </div>
  </div>
</template>

<style scoped>
.thinking-chain {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem;
  cursor: pointer;
  user-select: none;
}

.title {
  font-weight: 600;
  color: var(--text-primary);
}

.status {
  margin-left: 0.5rem;
  font-size: 0.75rem;
  color: var(--accent);
}

.toggle {
  margin-left: auto;
  color: var(--text-muted);
}

.chain-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 1rem 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.step {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background: var(--bg-secondary);
  border-radius: 4px;
  font-size: 0.8rem;
}

.step.error {
  background: rgba(255, 59, 48, 0.1);
  color: var(--accent-danger);
}

.icon {
  font-size: 0.9rem;
}

.empty {
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
