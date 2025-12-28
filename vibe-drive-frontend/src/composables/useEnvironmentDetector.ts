import { watch, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useVibeStore } from '@/stores/vibeStore'
import { vibeApi } from '@/services/api'
import type { Environment } from '@/types/api'

/**
 * 变化事件类型
 */
interface ChangeEvent {
  type: 'city' | 'time' | 'speed' | 'weather' | 'heartRate' | 'stress' | 'fatigue'
  oldValue: unknown
  newValue: unknown
  description: string
}

/**
 * 检测配置
 */
const CONFIG = {
  // 合并窗口时间（毫秒）
  mergeWindow: 3000,
  // 速度变化阈值
  speedThreshold: 30,
  // 心率变化阈值
  heartRateThreshold: 20,
  // 压力/疲劳变化阈值
  biometricsThreshold: 0.3,
  // 持续时间要求（毫秒）
  sustainDuration: 10000,
}

/**
 * 环境变化检测器
 */
export function useEnvironmentDetector() {
  const store = useVibeStore()
  const { environment, sessionId, plan, currentPlaylistIndex } = storeToRefs(store)

  // 上一次的环境快照
  const lastEnv = ref<Partial<Environment> | null>(null)

  // 待合并的变化
  const pendingChanges = ref<ChangeEvent[]>([])

  // 合并窗口定时器
  let mergeTimer: ReturnType<typeof setTimeout> | null = null

  // 是否启用检测
  const enabled = ref(false)

  /**
   * 检测即时变化（城市、时间、天气）
   */
  function detectInstantChanges(oldEnv: Partial<Environment>, newEnv: Environment): ChangeEvent[] {
    const changes: ChangeEvent[] = []

    // 城市变化
    if (oldEnv.location?.cityName !== newEnv.location?.cityName && newEnv.location?.cityName) {
      changes.push({
        type: 'city',
        oldValue: oldEnv.location?.cityName,
        newValue: newEnv.location?.cityName,
        description: `进入${newEnv.location.cityName}`,
      })
    }

    // 时间段变化
    if (oldEnv.timeOfDay !== newEnv.timeOfDay) {
      changes.push({
        type: 'time',
        oldValue: oldEnv.timeOfDay,
        newValue: newEnv.timeOfDay,
        description: `时间变为${newEnv.timeOfDay}`,
      })
    }

    // 天气变化
    if (oldEnv.weather !== newEnv.weather) {
      changes.push({
        type: 'weather',
        oldValue: oldEnv.weather,
        newValue: newEnv.weather,
        description: `天气变为${newEnv.weather}`,
      })
    }

    return changes
  }

  /**
   * 添加变化到待合并队列
   */
  function addChanges(changes: ChangeEvent[]) {
    if (changes.length === 0) return

    pendingChanges.value.push(...changes)
    console.log('[EnvironmentDetector] 检测到变化:', changes.map(c => c.description))

    // 重置合并窗口定时器
    if (mergeTimer) {
      clearTimeout(mergeTimer)
    }
    mergeTimer = setTimeout(flushChanges, CONFIG.mergeWindow)
  }

  /**
   * 触发 Agent
   */
  async function flushChanges() {
    if (pendingChanges.value.length === 0) return
    if (!environment.value || !sessionId.value) return

    const changes = [...pendingChanges.value]
    pendingChanges.value = []

    const descriptions = changes.map(c => c.description).join('，')
    console.log('[EnvironmentDetector] 触发氛围编排:', descriptions)

    // 在思维链中显示检测到的变化
    store.addThinkingStep({
      type: 'env_change',
      content: `检测到环境变化：${descriptions}`,
      agent: 'detector'
    })

    try {
      // 传递当前氛围状态和变化描述，支持增量调整
      await vibeApi.triggerVibeAgent(
        sessionId.value,
        environment.value,
        plan.value,
        descriptions,
        currentPlaylistIndex.value
      )
    } catch (e) {
      console.error('[EnvironmentDetector] 触发失败:', e)
    }
  }

  /**
   * 启动检测
   */
  function start() {
    enabled.value = true
    lastEnv.value = environment.value ? { ...environment.value } : null
    console.log('[EnvironmentDetector] 检测已启动')
  }

  /**
   * 停止检测
   */
  function stop() {
    enabled.value = false
    if (mergeTimer) {
      clearTimeout(mergeTimer)
      mergeTimer = null
    }
    pendingChanges.value = []
    console.log('[EnvironmentDetector] 检测已停止')
  }

  // 监听环境变化
  watch(environment, (newEnv) => {
    if (!enabled.value || !newEnv || !lastEnv.value) {
      lastEnv.value = newEnv ? { ...newEnv } : null
      return
    }

    const changes = detectInstantChanges(lastEnv.value, newEnv)
    addChanges(changes)

    // 更新快照
    lastEnv.value = { ...newEnv }
  }, { deep: true })

  return {
    enabled,
    pendingChanges,
    start,
    stop,
  }
}
