<script setup lang="ts">
import { computed, ref } from 'vue'
import { TresCanvas } from '@tresjs/core'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

// 从 LightSetting 获取颜色
const lightColor = computed(() => {
  return store.plan?.light?.color?.hex || '#007AFF'
})

const brightness = computed(() => {
  return (store.plan?.light?.brightness || 50) / 100
})

const lightMode = computed(() => {
  return store.plan?.light?.mode || 'STATIC'
})

// 呼吸动画
const breatheScale = ref(1)
let animationFrame: number | null = null

function animate() {
  if (lightMode.value === 'BREATHING') {
    const time = Date.now() / 1000
    breatheScale.value = 1 + Math.sin(time * 2) * 0.1
  } else {
    breatheScale.value = 1
  }
  animationFrame = requestAnimationFrame(animate)
}

animate()
</script>

<template>
  <div class="ambience-visualizer">
    <TresCanvas clear-color="#000000">
      <!-- 环境光 -->
      <TresAmbientLight :intensity="0.2" />

      <!-- 主氛围灯 -->
      <TresPointLight
        :position="[0, 2, 0]"
        :color="lightColor"
        :intensity="brightness * 2"
      />

      <!-- 氛围球体 -->
      <TresMesh :scale="breatheScale">
        <TresSphereGeometry :args="[1, 32, 32]" />
        <TresMeshStandardMaterial
          :color="lightColor"
          :emissive="lightColor"
          :emissive-intensity="brightness * 0.5"
          :transparent="true"
          :opacity="0.6"
        />
      </TresMesh>

      <!-- 相机 -->
      <TresPerspectiveCamera :position="[0, 0, 5]" />
    </TresCanvas>

    <!-- 无方案时的占位 -->
    <div v-if="!store.plan" class="placeholder">
      <span class="placeholder-text">等待氛围方案...</span>
    </div>
  </div>
</template>

<style scoped>
.ambience-visualizer {
  width: 100%;
  height: 100%;
  position: relative;
}

.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
}

.placeholder-text {
  color: var(--text-muted);
  font-size: 1rem;
}
</style>
