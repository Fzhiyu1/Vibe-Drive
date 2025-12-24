<script setup lang="ts">
import { computed } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

const modeLabels: Record<string, string> = {
  RELAX: '放松',
  ENERGIZE: '活力',
  COMFORT: '舒适',
  SPORT: '运动',
  OFF: '关闭',
}

const zoneLabels: Record<string, string> = {
  BACK: '背部',
  LUMBAR: '腰部',
  SHOULDER: '肩部',
  THIGH: '大腿',
  ALL: '全身',
}

const massage = computed(() => store.plan?.massage)
const hasMassage = computed(() => massage.value && massage.value.mode !== 'OFF' && massage.value.mode?.toUpperCase() !== 'OFF')

// 转换为大写键
const massageMode = computed(() => massage.value?.mode?.toUpperCase() || '')

const intensityPercent = computed(() => {
  if (!massage.value) return 0
  return massage.value.intensity * 10
})

const zonesText = computed(() => {
  if (!massage.value?.zones) return ''
  return massage.value.zones.map(z => zoneLabels[z.toUpperCase()] || z).join('、')
})
</script>

<template>
  <div class="massage-display">
    <h3 class="panel-title">座椅按摩</h3>

    <div v-if="hasMassage" class="massage-content">
      <div class="massage-mode">
        <span class="mode-badge" :class="massageMode.toLowerCase()">
          {{ modeLabels[massageMode] }}
        </span>
      </div>

      <div class="massage-info">
        <div class="info-row">
          <span class="label">区域</span>
          <span class="value">{{ zonesText }}</span>
        </div>

        <div class="info-row">
          <span class="label">强度</span>
          <div class="intensity-bar">
            <div class="intensity-fill" :style="{ width: intensityPercent + '%' }" />
          </div>
          <span class="value">{{ massage!.intensity }}/10</span>
        </div>
      </div>
    </div>

    <div v-else class="no-massage">
      <span class="placeholder-text">暂无按摩设置</span>
    </div>
  </div>
</template>

<style scoped>
.massage-display {
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

.massage-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.massage-mode {
  display: flex;
  align-items: center;
}

.mode-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
  color: white;
}

.mode-badge.relax {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.mode-badge.energize {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.mode-badge.comfort {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.mode-badge.sport {
  background: linear-gradient(135deg, #fa709a, #fee140);
}

.massage-info {
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
  background: linear-gradient(90deg, var(--accent), var(--accent-warning));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.no-massage {
  padding: 1rem;
  text-align: center;
}

.placeholder-text {
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
