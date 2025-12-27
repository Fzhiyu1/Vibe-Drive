import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type {
  Environment,
  AmbiencePlan,
  ThinkingStep,
  SafetyMode,
  ChatMessage,
} from '@/types/api'
import { vibeApi, masterApi } from '@/services/api'
import { useAnalyzeStream } from '@/composables/useSSE'
import { useTTS } from '@/composables/useTTS'

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

  // 对话状态
  const chatMessages = ref<ChatMessage[]>([])
  const chatPanelOpen = ref(false)
  const chatRunning = ref(false)

  // ============ 音频管理 ============
  const audio = new Audio()
  const isPlaying = ref(false)
  const audioProgress = ref(0)
  const currentPlaylistIndex = ref(0)

  // TTS 语音播报
  const { isSpeaking: ttsSpeaking, speak: speakTTS, stop: stopTTS } = useTTS()

  // 音频事件监听
  audio.addEventListener('timeupdate', () => {
    if (audio.duration > 0) {
      audioProgress.value = (audio.currentTime / audio.duration) * 100
    }
  })
  audio.addEventListener('ended', () => {
    isPlaying.value = false
    audioProgress.value = 0
    // 自动播放下一首
    const playlist = plan.value?.playlist
    if (playlist && currentPlaylistIndex.value < playlist.songs.length - 1) {
      playNext()
    }
  })

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

  // 立即应用工具结果
  function applyToolResult(toolName: string, resultJson: string) {
    try {
      const result = JSON.parse(resultJson)

      // 初始化 plan 如果不存在
      if (!plan.value) {
        plan.value = {} as AmbiencePlan
      }

      switch (toolName) {
        case 'setLight':
          plan.value = { ...plan.value, light: result }
          console.log('[vibeStore] 立即应用灯光:', result)
          break
        case 'setScent':
          plan.value = { ...plan.value, scent: result }
          console.log('[vibeStore] 立即应用香氛:', result)
          break
        case 'setMassage':
          plan.value = { ...plan.value, massage: result }
          console.log('[vibeStore] 立即应用按摩:', result)
          break
        case 'batchPlayMusic':
          plan.value = { ...plan.value, playlist: result }
          currentPlaylistIndex.value = 0
          // 自动播放第一首
          if (result.songs?.length > 0 && result.songs[0].url) {
            playMusic(result.songs[0].url)
          }
          console.log('[vibeStore] 立即应用歌单:', result)
          break
        case 'playMusic':
          plan.value = { ...plan.value, playResult: result }
          if (result.url) {
            playMusic(result.url)
          }
          console.log('[vibeStore] 立即播放音乐:', result)
          break
        case 'generateNarrative':
          plan.value = { ...plan.value, narrative: result }
          console.log('[vibeStore] 立即应用叙事:', result)
          // 自动播放 TTS
          if (result.text) {
            speakTTS(result.text, { volume: result.volume ?? 0.8 })
          }
          break
        default:
          console.log('[vibeStore] 未处理的工具结果:', toolName)
      }
    } catch (e) {
      console.warn('[vibeStore] 解析工具结果失败:', toolName, e)
    }
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
        // 立即应用工具结果
        applyToolResult(toolName, result)
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

  // 发送用户消息（语音输入）
  async function sendMessage(text: string) {
    if (!text.trim() || chatRunning.value) return

    // 添加用户消息
    const userMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: Date.now(),
    }
    chatMessages.value.push(userMsg)

    // 创建 AI 消息占位
    const aiMsg: ChatMessage = {
      id: `msg-${Date.now()}-ai`,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      toolCalls: [],
    }
    chatMessages.value.push(aiMsg)

    chatRunning.value = true

    await masterApi.chatStream(sessionId.value, text, {
      onToken: (token) => {
        aiMsg.content += token
      },
      onToolStart: (toolName, input) => {
        aiMsg.toolCalls?.push({ toolName, input })
      },
      onToolEnd: (toolName, result) => {
        const call = aiMsg.toolCalls?.find(c => c.toolName === toolName)
        if (call) call.output = result
        // 处理 say 工具 - 触发 TTS
        if (toolName === 'say' && result) {
          speakTTS(result, { volume: 0.8 })
        }
        // 应用其他工具结果
        applyToolResult(toolName, result)
      },
      onComplete: () => {
        chatRunning.value = false
      },
      onError: (code, message) => {
        aiMsg.content += `\n[错误: ${message}]`
        chatRunning.value = false
      },
    })
  }

  function toggleChatPanel() {
    chatPanelOpen.value = !chatPanelOpen.value
  }

  function clearChatMessages() {
    chatMessages.value = []
  }

  // ============ 音频控制方法 ============
  function unlockAudio() {
    // 播放静音音频来激活
    audio.src = 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA='
    audio.play().then(() => audio.pause()).catch(() => {})
  }

  function playMusic(url: string) {
    console.log('[vibeStore] playMusic called:', url)
    console.log('[vibeStore] audio.volume:', audio.volume, 'audio.muted:', audio.muted)
    audio.src = url
    audio.volume = 1  // 确保音量最大
    audio.muted = false  // 确保不静音
    audio.play().then(() => {
      console.log('[vibeStore] audio.play() success, duration:', audio.duration)
      isPlaying.value = true
    }).catch((err) => {
      console.error('[vibeStore] audio.play() failed:', err)
    })
  }

  function toggleAudio() {
    if (isPlaying.value) {
      audio.pause()
    } else {
      audio.play()
    }
    isPlaying.value = !isPlaying.value
  }

  // ============ 歌单控制方法 ============
  function playNext() {
    const playlist = plan.value?.playlist
    if (playlist && currentPlaylistIndex.value < playlist.songs.length - 1) {
      currentPlaylistIndex.value++
      const song = playlist.songs[currentPlaylistIndex.value]
      if (song?.url) {
        playMusic(song.url)
      }
    }
  }

  function playPrevious() {
    const playlist = plan.value?.playlist
    if (playlist && currentPlaylistIndex.value > 0) {
      currentPlaylistIndex.value--
      const song = playlist.songs[currentPlaylistIndex.value]
      if (song?.url) {
        playMusic(song.url)
      }
    }
  }

  function playSongAt(index: number) {
    const playlist = plan.value?.playlist
    if (playlist && index >= 0 && index < playlist.songs.length) {
      currentPlaylistIndex.value = index
      const song = playlist.songs[index]
      if (song?.url) {
        playMusic(song.url)
      }
    }
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
    // 音频状态
    isPlaying,
    audioProgress,
    currentPlaylistIndex,
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
    sendMessage,
    // 音频控制
    unlockAudio,
    playMusic,
    toggleAudio,
    // 歌单控制
    playNext,
    playPrevious,
    playSongAt,
    // TTS 控制
    ttsSpeaking,
    speakTTS,
    stopTTS,
    // 对话控制
    chatMessages,
    chatPanelOpen,
    chatRunning,
    toggleChatPanel,
    clearChatMessages,
  }
})
