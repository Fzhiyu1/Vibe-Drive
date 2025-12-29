<script setup lang="ts">
import { ref, watch } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'
import { useEnvironmentDetector } from '@/composables/useEnvironmentDetector'
import { useWelcome } from '@/composables/useWelcome'
import AppLayout from '@/components/layout/AppLayout.vue'
import EnvironmentPanel from '@/components/environment/EnvironmentPanel.vue'
import ThreeVisualizer from '@/components/ambience/ThreeVisualizer.vue'
import MusicPlayer from '@/components/music/MusicPlayer.vue'
import NarrativeDisplay from '@/components/narrative/NarrativeDisplay.vue'
import ScentDisplay from '@/components/scent/ScentDisplay.vue'
import MassageDisplay from '@/components/massage/MassageDisplay.vue'
import ThinkingChain from '@/components/agent/ThinkingChain.vue'
import ScenarioModal from '@/components/environment/ScenarioModal.vue'
import VoiceInput from '@/components/voice/VoiceInput.vue'
import MapPanel from '@/components/map/MapPanel.vue'
import WelcomeModal from '@/components/common/WelcomeModal.vue'
import type { Environment } from '@/types/api'

const store = useVibeStore()
const showModal = ref(false)

// 新手引导
const { showWelcome, acceptWelcome } = useWelcome()

// 环境变化检测器
const detector = useEnvironmentDetector()

// 监听行驶模拟状态，自动启停检测器
watch(() => store.drivingSimulationActive, (active) => {
  if (active) {
    detector.start()
  } else {
    detector.stop()
  }
})

function openModal() {
  showModal.value = true
}

function startDemo() {
  store.unlockAudio()
  if (store.environment) {
    // 已有环境数据，直接开始
    store.analyzeStream()
  } else {
    // 没有环境数据，弹出选择弹窗
    showModal.value = true
  }
}

function handleSelect(env: Environment) {
  store.unlockAudio()
  store.setEnvironment(env)
  store.analyzeStream()
}

function handleWelcomeAccept() {
  acceptWelcome()
  store.unlockAudio()
}
</script>

<template>
  <AppLayout>
    <template #environment>
      <EnvironmentPanel />

      <!-- 演示控制 -->
      <div class="demo-controls">
        <button
          v-if="!store.agentRunning"
          class="demo-btn"
          @click="startDemo"
        >
          {{ store.environment ? '开始编排' : '选择场景' }}
        </button>
        <button
          v-else
          class="demo-btn stop"
          disabled
        >
          编排中...
        </button>

        <!-- 语音输入 -->
        <VoiceInput class="voice-input-wrapper" />
      </div>
    </template>

    <template #ambience>
      <ThreeVisualizer />
      <!-- 可拖拽地图面板 -->
      <MapPanel class="map-draggable" />
    </template>

    <template #music>
      <MusicPlayer />
    </template>

    <template #narrative>
      <NarrativeDisplay />
    </template>

    <template #scent>
      <ScentDisplay />
    </template>

    <template #massage>
      <MassageDisplay />
    </template>

    <template #thinking>
      <ThinkingChain />
    </template>
  </AppLayout>

  <!-- 场景选择弹窗 -->
  <ScenarioModal
    :visible="showModal"
    @close="showModal = false"
    @select="handleSelect"
  />

  <!-- 新手引导弹窗 -->
  <WelcomeModal
    :visible="showWelcome"
    @accept="handleWelcomeAccept"
  />
</template>

<style scoped>
.map-draggable {
  position: absolute;
  top: 20px;
  left: 20px;
  width: 300px;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  cursor: move;
}

.demo-controls {
  margin-top: auto;
  padding-top: 1rem;
}

.demo-btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.demo-btn:hover {
  opacity: 0.9;
}

.demo-btn.stop {
  background: var(--accent-danger);
}

.voice-input-wrapper {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
