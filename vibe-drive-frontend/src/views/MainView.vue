<script setup lang="ts">
import { useVibeStore } from '@/stores/vibeStore'
import { useDemo } from '@/composables/useDemo'
import AppLayout from '@/components/layout/AppLayout.vue'
import EnvironmentPanel from '@/components/environment/EnvironmentPanel.vue'
import AmbienceVisualizer from '@/components/ambience/AmbienceVisualizer.vue'
import MusicPlayer from '@/components/music/MusicPlayer.vue'
import NarrativeDisplay from '@/components/narrative/NarrativeDisplay.vue'
import ThinkingChain from '@/components/agent/ThinkingChain.vue'

const store = useVibeStore()
const demo = useDemo()
</script>

<template>
  <AppLayout>
    <template #environment>
      <EnvironmentPanel />

      <!-- 演示控制 -->
      <div class="demo-controls">
        <button
          v-if="!demo.isRunning.value"
          class="demo-btn"
          @click="demo.start"
        >
          开始演示
        </button>
        <button
          v-else
          class="demo-btn stop"
          @click="demo.stop"
        >
          停止演示
        </button>
      </div>
    </template>

    <template #ambience>
      <AmbienceVisualizer />
    </template>

    <template #music>
      <MusicPlayer />
    </template>

    <template #narrative>
      <NarrativeDisplay />
    </template>

    <template #thinking>
      <ThinkingChain />
    </template>
  </AppLayout>
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
