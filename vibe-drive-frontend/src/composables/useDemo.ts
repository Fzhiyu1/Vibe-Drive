import { ref } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'
import { vibeApi } from '@/services/api'
import type { ScenarioType } from '@/types/api'

const scenarios: ScenarioType[] = [
  'LATE_NIGHT_RETURN',
  'WEEKEND_FAMILY_TRIP',
  'MORNING_COMMUTE',
]

const scenarioLabels: Record<ScenarioType, string> = {
  LATE_NIGHT_RETURN: '深夜归途',
  WEEKEND_FAMILY_TRIP: '周末家庭出游',
  MORNING_COMMUTE: '通勤早高峰',
  RANDOM: '随机场景',
}

export function useDemo() {
  const store = useVibeStore()
  const isRunning = ref(false)
  const currentIndex = ref(0)
  let stopFlag = false

  async function start() {
    if (isRunning.value) return

    isRunning.value = true
    store.demoMode = true
    stopFlag = false
    currentIndex.value = 0

    for (let i = 0; i < scenarios.length && !stopFlag; i++) {
      currentIndex.value = i
      const scenario = scenarios[i]
      if (scenario) {
        await runScenario(scenario)
      }
      if (!stopFlag) {
        await sleep(3000) // 场景间隔
      }
    }

    isRunning.value = false
    store.demoMode = false
  }

  async function runScenario(type: ScenarioType) {
    try {
      // 获取模拟环境
      const env = await vibeApi.getScenario(type)
      store.setEnvironment(env)

      // 触发流式分析
      await store.analyzeStream()
    } catch (e) {
      console.error('Demo scenario failed:', e)
    }
  }

  function stop() {
    stopFlag = true
    isRunning.value = false
    store.demoMode = false
  }

  function next() {
    currentIndex.value = (currentIndex.value + 1) % scenarios.length
  }

  return {
    isRunning,
    currentIndex,
    scenarios,
    scenarioLabels,
    start,
    stop,
    next,
  }
}

function sleep(ms: number) {
  return new Promise((r) => setTimeout(r, ms))
}
