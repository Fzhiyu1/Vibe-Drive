<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useVibeStore } from '@/stores/vibeStore'
import { useThreeScene } from '@/composables/three/useThreeScene'
import { useCarInterior } from '@/composables/three/useCarInterior'
import { useTimeOfDay } from '@/composables/three/useTimeOfDay'
import { useAmbienceLight } from '@/composables/three/useAmbienceLight'
import { useScentParticles } from '@/composables/three/useScentParticles'
import { useCarScreen } from '@/composables/three/useCarScreen'

const store = useVibeStore()
const { environment, plan } = storeToRefs(store)

const containerRef = ref<HTMLElement | null>(null)

const { context, isReady, addToAnimationLoop } = useThreeScene({
  container: containerRef,
  enableControls: false
})

watch(isReady, (ready) => {
  if (!ready || !context.value) return

  const { scene } = context.value

  // 构建车内几何体
  const interior = useCarInterior(scene)

  // 时间光照系统
  const timeOfDay = useTimeOfDay(context.value, {
    headLights: interior.headLights,
    headLightBulbs: interior.headLightBulbs
  })

  // 氛围灯系统
  const ambienceLight = useAmbienceLight(scene, interior)

  // 香氛粒子系统
  const scentParticles = useScentParticles(scene)

  // 车机屏幕
  const carScreen = useCarScreen(interior)

  // 注册动画回调
  addToAnimationLoop((delta) => {
    ambienceLight.tick(delta)
    scentParticles.tick(delta)
    carScreen.tick(delta)
  })

  // 监听环境变化
  watch(() => environment.value?.timeOfDay, (tod) => {
    console.log('[ThreeVisualizer] timeOfDay changed:', tod)
    if (tod) timeOfDay.updateTime(tod)
  }, { immediate: true })

  // 监听灯光变化
  watch(() => plan.value?.light, (light) => {
    console.log('[ThreeVisualizer] light changed:', light)
    if (light) ambienceLight.updateLight(light)
  }, { immediate: true })

  // 监听香氛变化
  watch(() => plan.value?.scent, (scent) => {
    console.log('[ThreeVisualizer] scent changed:', scent)
    scentParticles.updateScent(scent ?? null)
  }, { immediate: true })

  // 监听音乐变化（优先使用 playResult）
  watch(() => plan.value?.playResult, (playResult) => {
    if (playResult) {
      // 转换 PlayResult 为 Song 格式
      carScreen.updateSong({
        id: playResult.id,
        title: playResult.name,
        artist: playResult.artist,
        duration: playResult.duration,
        coverUrl: playResult.coverUrl
      })
    } else {
      // 兼容旧的 music.songs
      const song = plan.value?.music?.songs?.[0]
      carScreen.updateSong(song ?? null)
    }
  }, { immediate: true })

  // 监听播放进度同步到车机屏幕
  watch(() => store.audioProgress, (progress) => {
    carScreen.updateProgress(progress / 100)  // audioProgress 是 0-100，需要转为 0-1
  })
})
</script>

<template>
  <div ref="containerRef" class="three-visualizer" />
</template>

<style scoped>
.three-visualizer {
  width: 100%;
  height: 100%;
  min-height: 400px;
  background: #000;
}
</style>
