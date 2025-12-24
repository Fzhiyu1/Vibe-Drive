<script setup lang="ts">
import { ref } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'
import AppLayout from '@/components/layout/AppLayout.vue'
import EnvironmentPanel from '@/components/environment/EnvironmentPanel.vue'
import ThreeVisualizer from '@/components/ambience/ThreeVisualizer.vue'
import MusicPlayer from '@/components/music/MusicPlayer.vue'
import NarrativeDisplay from '@/components/narrative/NarrativeDisplay.vue'
import ScentDisplay from '@/components/scent/ScentDisplay.vue'
import MassageDisplay from '@/components/massage/MassageDisplay.vue'
import ThinkingChain from '@/components/agent/ThinkingChain.vue'
import ScenarioModal from '@/components/environment/ScenarioModal.vue'
import type { Environment } from '@/types/api'

const store = useVibeStore()
const showModal = ref(false)

function openModal() {
  showModal.value = true
}

function handleSelect(env: Environment) {
  store.setEnvironment(env)
  store.analyzeStream()
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
          @click="openModal"
        >
          开始演示
        </button>
        <button
          v-else
          class="demo-btn stop"
          disabled
        >
          编排中...
        </button>
      </div>
    </template>

    <template #ambience>
      <ThreeVisualizer />
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
</template>

<style scoped>
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
</style>
