<script setup lang="ts">
import { ref } from 'vue'
import { vibeApi } from '@/services/api'
import type { ScenarioType } from '@/types/api'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  select: [env: unknown]
}>()

const description = ref('')
const loading = ref(false)
const error = ref('')

const presets = [
  { type: 'LATE_NIGHT_RETURN' as ScenarioType, label: '深夜归途', icon: '🌙' },
  { type: 'WEEKEND_FAMILY_TRIP' as ScenarioType, label: '周末出游', icon: '👨‍👩‍👧' },
  { type: 'MORNING_COMMUTE' as ScenarioType, label: '早高峰通勤', icon: '🚗' },
]

async function generateByAI() {
  if (!description.value.trim()) {
    error.value = '请输入场景描述'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const env = await vibeApi.generateEnvironment(description.value)
    emit('select', env)
    emit('close')
  } catch (e) {
    error.value = '生成失败，请重试'
  } finally {
    loading.value = false
  }
}

async function selectPreset(type: ScenarioType) {
  loading.value = true
  error.value = ''
  try {
    const env = await vibeApi.getScenario(type)
    emit('select', env)
    emit('close')
  } catch (e) {
    error.value = '获取场景失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="modal">
        <div class="modal-header">
          <h3>选择驾驶场景</h3>
          <button class="close-btn" @click="emit('close')">×</button>
        </div>

        <div class="modal-body">
          <!-- AI 生成 -->
          <div class="ai-section">
            <label>描述你的驾驶场景</label>
            <textarea
              v-model="description"
              placeholder="例如：深夜在高速公路上疲惫地开车回家..."
              rows="3"
            />
            <button
              class="ai-btn"
              :disabled="loading"
              @click="generateByAI"
            >
              {{ loading ? '生成中...' : 'AI 生成环境' }}
            </button>
          </div>

          <div class="divider">
            <span>或选择预设场景</span>
          </div>

          <!-- 预设场景 -->
          <div class="presets">
            <button
              v-for="preset in presets"
              :key="preset.type"
              class="preset-btn"
              :disabled="loading"
              @click="selectPreset(preset.type)"
            >
              <span class="icon">{{ preset.icon }}</span>
              <span class="label">{{ preset.label }}</span>
            </button>
          </div>

          <div v-if="error" class="error">{{ error }}</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: var(--bg-primary);
  border-radius: 12px;
  width: 90%;
  max-width: 480px;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.125rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-muted);
}

.modal-body {
  padding: 1.5rem;
}

.ai-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ai-section label {
  font-weight: 500;
  color: var(--text-primary);
}

.ai-section textarea {
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  resize: none;
  font-family: inherit;
}

.ai-btn {
  padding: 0.75rem;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.ai-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.divider {
  display: flex;
  align-items: center;
  margin: 1.5rem 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider span {
  padding: 0 1rem;
}

.presets {
  display: flex;
  gap: 0.75rem;
}

.preset-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-btn:hover:not(:disabled) {
  border-color: var(--accent);
  background: var(--bg-tertiary);
}

.preset-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.preset-btn .icon {
  font-size: 1.5rem;
}

.preset-btn .label {
  font-size: 0.875rem;
  color: var(--text-primary);
}

.error {
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(255, 59, 48, 0.1);
  color: var(--accent-danger);
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>
