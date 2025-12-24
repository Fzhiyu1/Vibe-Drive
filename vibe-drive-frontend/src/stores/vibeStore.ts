import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type {
  Environment,
  AmbiencePlan,
  ThinkingStep,
  SafetyMode,
} from '@/types/api'
import { vibeApi } from '@/services/api'
import { useAnalyzeStream } from '@/composables/useSSE'

function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export const useVibeStore = defineStore('vibe', () => {
  // ============ 状态 ============
  const sessionId = ref<string>(generateSessionId())
  const environment = ref<Environment | null>(null)
  const plan = ref<AmbiencePlan | null>(null)
  const agentRunning = ref(false)
  const thinkingChain = ref<ThinkingStep[]>([])
  const error = ref<string | null>(null)

  // UI 状态
  const theme = ref<'light' | 'dark'>('light')
  const demoMode = ref(false)
  const chainExpanded = ref(false)

  // ============ 计算属性 ============
  // 安全模式阈值与后端一致：L1 < 60, L2 60-100, L3 >= 100
  const safetyMode = computed<SafetyMode>(() => {
    if (!environment.value) return 'L1_NORMAL'
    const speed = environment.value.speed
    if (speed >= 100) return 'L3_SILENT'
    if (speed >= 60) return 'L2_FOCUS'
    return 'L1_NORMAL'
  })

  const hasActivePlan = computed(() => plan.value !== null)

  // ============ Actions ============
  function setEnvironment(env: Partial<Environment>) {
    if (environment.value) {
      environment.value = { ...environment.value, ...env }
    } else {
      environment.value = env as Environment
    }
  }

  function clearThinkingChain() {
    thinkingChain.value = []
  }

  function addThinkingStep(step: Omit<ThinkingStep, 'timestamp'>) {
    thinkingChain.value.push({
      ...step,
      timestamp: Date.now(),
    })
  }

  async function analyzeStream() {
    if (!environment.value) {
      error.value = '请先设置环境数据'
      return
    }

    error.value = null
    agentRunning.value = true
    clearThinkingChain()

    const stream = useAnalyzeStream()

    await stream.connect(sessionId.value, environment.value, {
      onToken: (text) => {
        // 打字机效果：累积到最后一个 thinking 步骤
        const lastStep = thinkingChain.value[thinkingChain.value.length - 1]
        if (lastStep && lastStep.type === 'thinking') {
          lastStep.content += text
        } else {
          addThinkingStep({ type: 'thinking', content: text })
        }
      },
      onToolStart: (toolName, input) => {
        addThinkingStep({
          type: 'tool_start',
          content: `调用 ${toolName}`,
          toolName,
          toolInput: input,
        })
      },
      onToolEnd: (toolName, result) => {
        addThinkingStep({
          type: 'tool_end',
          content: `${toolName} 完成`,
          toolName,
          toolOutput: result,
        })
      },
      onComplete: (newPlan) => {
        plan.value = newPlan

        // 检测 L3 静默模式（空方案）
        const isEmptyPlan = !newPlan?.music && !newPlan?.light && !newPlan?.narrative && !newPlan?.scent
        if (isEmptyPlan && safetyMode.value === 'L3_SILENT') {
          console.warn('[vibeStore] L3 静默模式：车速 >= 100 km/h，为保障驾驶安全，已跳过氛围推荐')
          addThinkingStep({
            type: 'complete',
            content: '⚠️ L3 静默模式：车速过高，为保障驾驶安全，已跳过氛围推荐'
          })
        } else {
          addThinkingStep({ type: 'complete', content: '分析完成' })
        }

        agentRunning.value = false
      },
      onError: (code, message) => {
        error.value = `${code}: ${message}`
        addThinkingStep({ type: 'error', content: message })
        agentRunning.value = false
      },
    }, { debug: true })
  }

  async function analyze() {
    if (!environment.value) {
      error.value = '请先设置环境数据'
      return
    }

    error.value = null
    agentRunning.value = true

    try {
      const response = await vibeApi.analyze({
        sessionId: sessionId.value,
        environment: environment.value,
      })

      if (response.success && response.data) {
        if (response.data.plan) {
          plan.value = response.data.plan
        }
      } else {
        error.value = response.error?.message || '分析失败'
      }
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      agentRunning.value = false
    }
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }

  function resetSession() {
    sessionId.value = generateSessionId()
    environment.value = null
    plan.value = null
    thinkingChain.value = []
    error.value = null
  }

  return {
    // 状态
    sessionId,
    environment,
    plan,
    agentRunning,
    thinkingChain,
    error,
    theme,
    demoMode,
    chainExpanded,
    // 计算属性
    safetyMode,
    hasActivePlan,
    // Actions
    setEnvironment,
    clearThinkingChain,
    analyzeStream,
    analyze,
    toggleTheme,
    resetSession,
  }
})
