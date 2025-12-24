<script setup lang="ts">
import { computed } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

const scentLabels: Record<string, string> = {
  LAVENDER: '薰衣草',
  PEPPERMINT: '薄荷',
  OCEAN: '海洋',
  FOREST: '森林',
  CITRUS: '柑橘',
  VANILLA: '香草',
  NONE: '无',
}

const scentIcons: Record<string, string> = {
  LAVENDER: '💜',
  PEPPERMINT: '🌿',
  OCEAN: '🌊',
  FOREST: '🌲',
  CITRUS: '🍊',
  VANILLA: '🍦',
  NONE: '⭕',
}

const scent = computed(() => store.plan?.scent)
const hasScent = computed(() => scent.value && scent.value.type !== 'NONE' && scent.value.type?.toUpperCase() !== 'NONE')

// 转换为大写键
const scentType = computed(() => scent.value?.type?.toUpperCase() || '')

const intensityPercent = computed(() => {
  if (!scent.value) return 0
  return scent.value.intensity * 10
})
</script>

<template>
  <div class="scent-display">
    <h3 class="panel-title">香氛系统</h3>

    <div v-if="hasScent" class="scent-content">
      <div class="scent-type">
        <span class="scent-icon">{{ scentIcons[scentType] }}</span>
        <span class="scent-name">{{ scentLabels[scentType] }}</span>
      </div>

      <div class="scent-info">
        <div class="info-row">
          <span class="label">强度</span>
          <div class="intensity-bar">
            <div class="intensity-fill" :style="{ width: intensityPercent + '%' }" />
          </div>
          <span class="value">{{ scent!.intensity }}/10</span>
        </div>

        <div class="info-row">
          <span class="label">时长</span>
          <span class="value">{{ scent!.durationMinutes }} 分钟</span>
        </div>
      </div>
    </div>

    <div v-else class="no-scent">
      <span class="placeholder-text">暂无香氛设置</span>
    </div>
  </div>
</template>

<style scoped>
.scent-display {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.panel-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.scent-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.scent-type {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.scent-icon {
  font-size: 1.5rem;
}

.scent-name {
  font-size: 1.125rem;
  font-weight: 500;
  color: var(--text-primary);
}

.scent-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.label {
  color: var(--text-secondary);
  font-size: 0.875rem;
  min-width: 2.5rem;
}

.value {
  color: var(--text-primary);
  font-size: 0.875rem;
  font-weight: 500;
}

.intensity-bar {
  flex: 1;
  height: 6px;
  background: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.intensity-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-success), var(--accent));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.no-scent {
  padding: 1rem;
  text-align: center;
}

.placeholder-text {
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
