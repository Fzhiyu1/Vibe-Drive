<script setup lang="ts">
import { ref } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'
import { vibeApi } from '@/services/api'
import type { ScenarioType } from '@/types/api'
import MoodSelector from './MoodSelector.vue'

const store = useVibeStore()

// 环境生成状态
const isGenerating = ref(false)
const aiPrompt = ref('')

// 预设场景
const scenarios: { type: ScenarioType; label: string }[] = [
  { type: 'LATE_NIGHT_RETURN', label: '深夜归途' },
  { type: 'WEEKEND_FAMILY_TRIP', label: '周末出游' },
  { type: 'MORNING_COMMUTE', label: '通勤早高峰' },
]

// 加载预设场景
async function loadScenario(type: ScenarioType) {
  if (isGenerating.value) return
  isGenerating.value = true
  try {
    const env = await vibeApi.getScenario(type)
    store.setEnvironment(env)
  } catch (e) {
    console.error('加载场景失败:', e)
  } finally {
    isGenerating.value = false
  }
}

// AI 生成环境
async function generateEnvironment() {
  if (!aiPrompt.value.trim() || isGenerating.value) return
  isGenerating.value = true
  try {
    const env = await vibeApi.generateEnvironment(aiPrompt.value)
    store.setEnvironment(env)
    aiPrompt.value = ''
  } catch (e) {
    console.error('生成环境失败:', e)
  } finally {
    isGenerating.value = false
  }
}

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

// 辅助函数
const formatPercent = (value: number) => `${Math.round(value * 100)}%`

// 将值转换为大写以匹配标签键
const toUpperKey = (value: string | undefined) => value?.toUpperCase() || ''

const getStressClass = (level: number) => {
  if (level >= 0.7) return 'status-danger'
  if (level >= 0.5) return 'status-warning'
  return 'status-normal'
}

const getFatigueClass = (level: number) => {
  if (level >= 0.6) return 'status-danger'
  if (level >= 0.4) return 'status-warning'
  return 'status-normal'
}
</script>

<template>
  <div class="environment-panel">
    <!-- 环境生成区域 -->
    <div class="generate-section">
      <h3 class="section-title">生成环境</h3>

      <!-- 预设场景 -->
      <div class="scenario-buttons">
        <button
          v-for="s in scenarios"
          :key="s.type"
          class="scenario-btn"
          :disabled="isGenerating"
          @click="loadScenario(s.type)"
        >
          {{ s.label }}
        </button>
      </div>

      <!-- AI 生成 -->
      <div class="ai-generate">
        <input
          v-model="aiPrompt"
          placeholder="描述场景，如：雨夜高速..."
          :disabled="isGenerating"
          @keydown.enter="generateEnvironment"
        />
        <button
          class="generate-btn"
          :disabled="!aiPrompt.trim() || isGenerating"
          @click="generateEnvironment"
        >
          {{ isGenerating ? '...' : '生成' }}
        </button>
      </div>
    </div>

    <div class="divider" />

    <h2 class="panel-title">环境信息</h2>

    <div v-if="store.environment" class="info-list">
      <div class="info-item">
        <span class="label">位置</span>
        <span class="value">{{ gpsLabels[toUpperKey(store.environment.gpsTag)] }}</span>
      </div>
      <div class="info-item">
        <span class="label">天气</span>
        <span class="value">{{ weatherLabels[toUpperKey(store.environment.weather)] }}</span>
      </div>
      <div class="info-item">
        <span class="label">车速</span>
        <span class="value">{{ store.environment.speed }} km/h</span>
      </div>
      <div class="info-item">
        <span class="label">时段</span>
        <span class="value">{{ timeLabels[toUpperKey(store.environment.timeOfDay)] }}</span>
      </div>
      <div class="info-item">
        <span class="label">乘客</span>
        <span class="value">{{ store.environment.passengerCount }} 人</span>
      </div>

      <!-- 位置信息 -->
      <template v-if="store.environment.location">
        <div class="info-item">
          <span class="label">城市</span>
          <span class="value">{{ store.environment.location.cityName }}</span>
        </div>
        <div class="info-item">
          <span class="label">道路</span>
          <span class="value">{{ store.environment.location.roadName }}</span>
        </div>
      </template>

      <!-- 生理数据 -->
      <template v-if="store.environment.biometrics">
        <div class="divider" />
        <div class="section-title">驾驶员状态</div>
        <div class="info-item">
          <span class="label">心率</span>
          <span class="value">{{ store.environment.biometrics.heartRate }} bpm</span>
        </div>
        <div class="info-item">
          <span class="label">压力</span>
          <span class="value" :class="getStressClass(store.environment.biometrics.stressLevel)">
            {{ formatPercent(store.environment.biometrics.stressLevel) }}
          </span>
        </div>
        <div class="info-item">
          <span class="label">疲劳</span>
          <span class="value" :class="getFatigueClass(store.environment.biometrics.fatigueLevel)">
            {{ formatPercent(store.environment.biometrics.fatigueLevel) }}
          </span>
        </div>
      </template>
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

.section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0.25rem 0;
}

.status-normal {
  color: var(--accent-success);
}

.status-warning {
  color: var(--accent-warning);
}

.status-danger {
  color: var(--accent-danger);
}

/* 环境生成区域 */
.generate-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.scenario-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.scenario-btn {
  padding: 0.375rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.scenario-btn:hover:not(:disabled) {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}

.scenario-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-generate {
  display: flex;
  gap: 0.5rem;
}

.ai-generate input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.8rem;
}

.ai-generate input:focus {
  outline: none;
  border-color: var(--accent);
}

.generate-btn {
  padding: 0.5rem 0.75rem;
  border: none;
  border-radius: 6px;
  background: var(--accent);
  color: white;
  font-size: 0.8rem;
  cursor: pointer;
}

.generate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
