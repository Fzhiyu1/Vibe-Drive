<script setup lang="ts">
import { computed } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

const lightColor = computed(() => {
  return store.plan?.light?.color?.hex || '#007AFF'
})

const brightness = computed(() => {
  return (store.plan?.light?.brightness || 50) / 100
})

const lightMode = computed(() => {
  return store.plan?.light?.mode || 'STATIC'
})

const animationClass = computed(() => {
  switch (lightMode.value) {
    case 'BREATHING':
      return 'breathing'
    case 'GRADIENT':
      return 'gradient'
    case 'PULSE':
      return 'pulse'
    default:
      return ''
  }
})
</script>

<template>
  <div class="ambience-visualizer">
    <div
      v-if="store.plan"
      class="ambience-orb"
      :class="animationClass"
      :style="{
        '--light-color': lightColor,
        '--brightness': brightness,
      }"
    >
      <div class="orb-inner" />
      <div class="orb-glow" />
    </div>

    <div v-else class="placeholder">
      <span class="placeholder-text">等待氛围方案...</span>
    </div>
  </div>
</template>

<style scoped>
.ambience-visualizer {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at center, #1a1a2e 0%, #0f0f1a 100%);
  position: relative;
  overflow: hidden;
}

.ambience-orb {
  position: relative;
  width: 200px;
  height: 200px;
}

.orb-inner {
  position: absolute;
  inset: 20%;
  border-radius: 50%;
  background: var(--light-color);
  opacity: calc(var(--brightness) * 0.8 + 0.2);
  box-shadow: 0 0 60px var(--light-color);
}

.orb-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    var(--light-color) 0%,
    transparent 70%
  );
  opacity: calc(var(--brightness) * 0.5);
  filter: blur(20px);
}

/* 呼吸动画 */
.breathing .orb-inner,
.breathing .orb-glow {
  animation: breathe 3s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    transform: scale(1);
    opacity: calc(var(--brightness) * 0.6 + 0.2);
  }
  50% {
    transform: scale(1.1);
    opacity: calc(var(--brightness) * 0.9 + 0.1);
  }
}

/* 渐变动画 */
.gradient .orb-inner {
  animation: gradient-shift 4s linear infinite;
}

@keyframes gradient-shift {
  0% {
    filter: hue-rotate(0deg);
  }
  100% {
    filter: hue-rotate(360deg);
  }
}

/* 脉冲动画 */
.pulse .orb-glow {
  animation: pulse 1.5s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: calc(var(--brightness) * 0.5);
  }
  50% {
    transform: scale(1.3);
    opacity: calc(var(--brightness) * 0.2);
  }
  100% {
    transform: scale(1);
    opacity: calc(var(--brightness) * 0.5);
  }
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  color: var(--text-muted);
  font-size: 1rem;
}
</style>
