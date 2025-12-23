<script setup lang="ts">
import { useVibeStore } from '@/stores/vibeStore'
import MoodSelector from './MoodSelector.vue'

const store = useVibeStore()

const gpsLabels: Record<string, string> = {
  HIGHWAY: '高速公路',
  TUNNEL: '隧道',
  BRIDGE: '桥梁',
  URBAN: '城区',
  SUBURBAN: '郊区',
  MOUNTAIN: '山区',
  COASTAL: '海滨',
  PARKING: '停车场',
}

const weatherLabels: Record<string, string> = {
  SUNNY: '晴天',
  CLOUDY: '多云',
  RAINY: '雨天',
  SNOWY: '雪天',
  FOGGY: '雾天',
}

const timeLabels: Record<string, string> = {
  DAWN: '黎明',
  MORNING: '上午',
  NOON: '中午',
  AFTERNOON: '下午',
  EVENING: '傍晚',
  NIGHT: '夜晚',
  MIDNIGHT: '深夜',
}

const safetyLabels: Record<string, string> = {
  L1_NORMAL: '正常',
  L2_FOCUS: '专注',
  L3_SILENT: '静默',
}
</script>

<template>
  <div class="environment-panel">
    <h2 class="panel-title">环境信息</h2>

    <div v-if="store.environment" class="info-list">
      <div class="info-item">
        <span class="label">位置</span>
        <span class="value">{{ gpsLabels[store.environment.gpsTag] }}</span>
      </div>
      <div class="info-item">
        <span class="label">天气</span>
        <span class="value">{{ weatherLabels[store.environment.weather] }}</span>
      </div>
      <div class="info-item">
        <span class="label">车速</span>
        <span class="value">{{ store.environment.speed }} km/h</span>
      </div>
      <div class="info-item">
        <span class="label">时段</span>
        <span class="value">{{ timeLabels[store.environment.timeOfDay] }}</span>
      </div>
      <div class="info-item">
        <span class="label">乘客</span>
        <span class="value">{{ store.environment.passengerCount }} 人</span>
      </div>
    </div>

    <div v-else class="no-data">
      暂无环境数据
    </div>

    <div class="divider" />

    <div class="safety-mode">
      <span class="label">安全模式</span>
      <span class="badge" :class="store.safetyMode">
        {{ safetyLabels[store.safetyMode] }}
      </span>
    </div>

    <div class="divider" />

    <MoodSelector />
  </div>
</template>

<style scoped>
.environment-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.panel-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.value {
  color: var(--text-primary);
  font-weight: 500;
}

.no-data {
  color: var(--text-muted);
  text-align: center;
  padding: 1rem;
}

.divider {
  height: 1px;
  background-color: var(--divider-color);
  margin: 0.5rem 0;
}

.safety-mode {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge.L1_NORMAL {
  background-color: var(--accent-success);
  color: white;
}

.badge.L2_FOCUS {
  background-color: var(--accent-warning);
  color: white;
}

.badge.L3_SILENT {
  background-color: var(--accent-danger);
  color: white;
}
</style>
