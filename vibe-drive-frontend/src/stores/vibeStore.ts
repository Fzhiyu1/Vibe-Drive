import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import type {
  Environment,
  AmbiencePlan,
  ThinkingStep,
  SafetyMode,
  PageWindowState,
  PageWindowType,
} from '@/types/api'
import { vibeApi, masterApi } from '@/services/api'
import { useAnalyzeStream } from '@/composables/useSSE'
import { useVibeEvents } from '@/composables/useVibeEvents'
import { useTTS } from '@/composables/useTTS'
import { useMemoryStore } from './memoryStore'

const SESSION_ID_KEY = 'vibe-drive-session-id'

function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

function getOrCreateSessionId(): string {
  const stored = localStorage.getItem(SESSION_ID_KEY)
  if (stored) {
    return stored
  }
  const newId = generateSessionId()
  localStorage.setItem(SESSION_ID_KEY, newId)
  return newId
}

export const useVibeStore = defineStore('vibe', () => {
  // ============ 状态 ============
  const sessionId = ref<string>(getOrCreateSessionId())
  const environment = ref<Environment | null>(null)
  const plan = ref<AmbiencePlan | null>(null)
  // 分离运行状态：主智能体和Vibe智能体独立
  const masterRunning = ref(false)
  const vibeRunning = ref(false)
  // 分离思维链：主智能体和Vibe智能体独立显示
  const masterChain = ref<ThinkingStep[]>([])
  const vibeChain = ref<ThinkingStep[]>([])
  const error = ref<string | null>(null)

  // UI 状态
  const theme = ref<'light' | 'dark'>('light')
  const demoMode = ref(false)
  const chainExpanded = ref(false)
  const drivingSimulationActive = ref(false)

  // 弹窗状态
  const pageWindows = ref<PageWindowState[]>([])
  let nextWindowId = 1
  let topZIndex = 1000

  // ============ 音频管理 ============
  const audio = new Audio()
  const isPlaying = ref(false)
  const audioVolume = ref(0.5)  // 默认音量50%
  audio.volume = 0.5
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

  // ============ 氛围事件监听 ============
  const vibeEvents = useVibeEvents()

  // 连接氛围事件
  function connectVibeEvents() {
    vibeEvents.connect(sessionId.value, {
      onToolStart: (taskId, toolName, input) => {
        vibeRunning.value = true
        addThinkingStep({
          type: 'tool_start',
          content: `调用 ${toolName}`,
          agent: 'vibe',
          toolName,
          toolInput: input
        })
      },
      onToolEnd: (taskId, toolName, result) => {
        addThinkingStep({
          type: 'tool_end',
          content: `${toolName} 完成`,
          agent: 'vibe',
          toolName,
          toolOutput: result
        })
        // 立即应用工具结果
        applyToolResult(toolName, result)
      },
      onComplete: (taskId, completedPlan) => {
        vibeRunning.value = false
        addThinkingStep({
          type: 'complete',
          content: '氛围编排完成',
          agent: 'vibe'
        })
      },
      onError: (taskId, errorMsg) => {
        vibeRunning.value = false
        addThinkingStep({
          type: 'error',
          content: errorMsg,
          agent: 'vibe'
        })
      },
      onCancelled: (taskId) => {
        vibeRunning.value = false
        addThinkingStep({
          type: 'error',
          content: '氛围编排已取消',
          agent: 'vibe'
        })
      },
      onEnvironmentUpdate: (gpsTag, weather, speed) => {
        console.log('[vibeStore] 收到环境更新:', { gpsTag, weather, speed })
        // 更新环境状态
        if (environment.value) {
          environment.value = {
            ...environment.value,
            gpsTag: gpsTag as any,
            weather: weather as any,
            speed
          }
        } else {
          environment.value = {
            gpsTag: gpsTag as any,
            weather: weather as any,
            speed,
            userMood: 'CALM',
            timeOfDay: 'AFTERNOON',
            passengerCount: 1,
            routeType: 'URBAN'
          } as any
        }
      },
      onMemoryUpdated: (sid, content) => {
        console.log('[vibeStore] 收到记忆更新:', sid)
        // 更新前端记忆
        memoryStore.memory = content
      }
    })
  }

  // 初始连接
  connectVibeEvents()

  // 初始化：清除聊天记忆（避免损坏的对话历史）+ 同步用户记忆
  const memoryStore = useMemoryStore()

  // 清除聊天记忆（对话历史）
  fetch(`/api/memory/chat?sessionId=${sessionId.value}`, { method: 'DELETE' })
    .then(() => console.log('[vibeStore] 聊天记忆已清除'))
    .catch(e => console.warn('[vibeStore] 清除聊天记忆失败:', e))

  // 同步用户记忆（偏好）
  memoryStore.syncToBackend(sessionId.value)

  // sessionId 变化时重新连接
  watch(sessionId, () => {
    vibeEvents.disconnect()
    connectVibeEvents()
    // 重新同步记忆
    memoryStore.syncToBackend(sessionId.value)
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
    // 同步到后端
    if (environment.value) {
      vibeApi.syncEnvironment(sessionId.value, environment.value).catch(e => {
        console.warn('[vibeStore] 同步环境失败:', e)
      })
    }
  }

  function clearThinkingChain() {
    masterChain.value = []
    vibeChain.value = []
  }

  function addThinkingStep(step: Omit<ThinkingStep, 'timestamp'>) {
    const newStep = {
      ...step,
      timestamp: Date.now(),
    }
    // 根据 agent 分发到不同的思维链
    if (step.agent === 'vibe') {
      vibeChain.value.push(newStep)
    } else {
      masterChain.value.push(newStep)
    }
  }

  // ============ 弹窗管理方法 ============
  function openPageWindow(title: string, content: string, type: PageWindowType = 'document') {
    const id = `page-${nextWindowId++}`
    const offset = (pageWindows.value.length % 5) * 30
    pageWindows.value.push({
      id,
      title,
      content,
      type,
      position: { x: 100 + offset, y: 100 + offset },
      size: { width: 600, height: 400 },
      zIndex: ++topZIndex,
      minimized: false
    })
  }

  function closePageWindow(id: string) {
    const index = pageWindows.value.findIndex(w => w.id === id)
    if (index !== -1) {
      pageWindows.value.splice(index, 1)
    }
  }

  function focusPageWindow(id: string) {
    const window = pageWindows.value.find(w => w.id === id)
    if (window) {
      window.zIndex = ++topZIndex
    }
  }

  function updateWindowPosition(id: string, position: { x: number; y: number }) {
    const window = pageWindows.value.find(w => w.id === id)
    if (window) {
      window.position = position
    }
  }

  function updateWindowSize(id: string, size: { width: number; height: number }) {
    const window = pageWindows.value.find(w => w.id === id)
    if (window) {
      window.size = size
    }
  }

  function toggleWindowMinimize(id: string) {
    const window = pageWindows.value.find(w => w.id === id)
    if (window) {
      window.minimized = !window.minimized
    }
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
          // 自动播放新歌单第一首
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
        case 'callVibeAgent':
          // 氛围智能体返回完整的 AmbiencePlan
          console.log('[vibeStore] 应用氛围智能体结果:', result)
          if (result.playlist) {
            plan.value = { ...plan.value, playlist: result.playlist }
            currentPlaylistIndex.value = 0
            if (result.playlist.songs?.length > 0 && result.playlist.songs[0].url) {
              playMusic(result.playlist.songs[0].url)
            }
          }
          if (result.playResult) {
            plan.value = { ...plan.value, playResult: result.playResult }
            if (result.playResult.url) {
              playMusic(result.playResult.url)
            }
          }
          if (result.light) {
            plan.value = { ...plan.value, light: result.light }
          }
          if (result.scent) {
            plan.value = { ...plan.value, scent: result.scent }
          }
          if (result.massage) {
            plan.value = { ...plan.value, massage: result.massage }
          }
          if (result.narrative) {
            plan.value = { ...plan.value, narrative: result.narrative }
            if (result.narrative.text) {
              speakTTS(result.narrative.text, { volume: result.narrative.volume ?? 0.8 })
            }
          }
          break
        // 播放列表控制工具
        case 'pauseMusic':
          if (result.action === 'pause') {
            audio.pause()
            isPlaying.value = false
            console.log('[vibeStore] 暂停音乐')
          }
          break
        case 'resumeMusic':
          if (result.action === 'resume') {
            audio.play()
            isPlaying.value = true
            console.log('[vibeStore] 继续播放')
          }
          break
        case 'playAt':
          if (result.action === 'playAt' && result.song?.url) {
            currentPlaylistIndex.value = result.index
            playMusic(result.song.url)
            console.log('[vibeStore] 播放指定歌曲:', result.song.name)
          }
          break
        case 'removeFromPlaylist':
          if (result.action === 'remove' && plan.value?.playlist?.songs) {
            const newSongs = [...plan.value.playlist.songs]
            newSongs.splice(result.index, 1)
            plan.value = { ...plan.value, playlist: { ...plan.value.playlist, songs: newSongs } }
            console.log('[vibeStore] 删除歌曲:', result.index)
          }
          break
        case 'clearPlaylist':
          if (result.action === 'clear') {
            audio.pause()
            audio.src = ''
            isPlaying.value = false
            if (plan.value) {
              plan.value = { ...plan.value, playlist: undefined }
            }
            console.log('[vibeStore] 清空播放列表')
          }
          break
        case 'addToPlaylist':
          if (result.action === 'add' && result.song) {
            if (!plan.value) {
              plan.value = {} as AmbiencePlan
            }
            const currentSongs = plan.value.playlist?.songs || []
            plan.value = {
              ...plan.value,
              playlist: { songs: [...currentSongs, result.song], currentIndex: plan.value.playlist?.currentIndex || 0 }
            }
            console.log('[vibeStore] 添加歌曲到播放列表:', result.song.name)
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
    vibeRunning.value = true
    clearThinkingChain()

    const stream = useAnalyzeStream()

    await stream.connect(sessionId.value, environment.value, {
      onToken: (text) => {
        // 打字机效果：累积到最后一个 thinking 步骤
        const lastStep = vibeChain.value[vibeChain.value.length - 1]
        if (lastStep && lastStep.type === 'thinking') {
          lastStep.content += text
        } else {
          addThinkingStep({ type: 'thinking', content: text, agent: 'vibe' })
        }
      },
      onToolStart: (toolName, input) => {
        addThinkingStep({
          type: 'tool_start',
          content: `调用 ${toolName}`,
          agent: 'vibe',
          toolName,
          toolInput: input,
        })
      },
      onToolEnd: (toolName, result) => {
        addThinkingStep({
          type: 'tool_end',
          content: `${toolName} 完成`,
          agent: 'vibe',
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

        vibeRunning.value = false
      },
      onError: (code, message) => {
        error.value = `${code}: ${message}`
        addThinkingStep({ type: 'error', content: message })
        vibeRunning.value = false
      },
    }, { debug: true })
  }

  async function analyze() {
    if (!environment.value) {
      error.value = '请先设置环境数据'
      return
    }

    error.value = null
    vibeRunning.value = true

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
      vibeRunning.value = false
    }
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }

  function resetSession() {
    const newId = generateSessionId()
    localStorage.setItem(SESSION_ID_KEY, newId)
    sessionId.value = newId
    environment.value = null
    plan.value = null
    masterChain.value = []
    vibeChain.value = []
    error.value = null
  }

  // 发送用户消息（集成到思维链）
  async function sendMessage(text: string) {
    if (!text.trim() || masterRunning.value) return

    masterRunning.value = true

    // 添加用户输入步骤
    addThinkingStep({
      type: 'user_input',
      content: text,
      agent: 'master'
    })

    await masterApi.chatStream(sessionId.value, text, {
      onToken: () => {
        // 主智能体的 thinking（暂不处理）
      },
      onToolStart: (toolName, input) => {
        // 解析 input（可能是 JSON 字符串）
        let parsedInput = input
        if (typeof input === 'string') {
          try {
            parsedInput = JSON.parse(input)
          } catch {
            // 保持原样
          }
        }

        // say 工具：立即触发 TTS，不等 tool_end
        if (toolName === 'say' && (parsedInput as { text?: string })?.text) {
          speakTTS((parsedInput as { text: string }).text, { volume: 0.8 })
        }

        // showPage 工具：立即弹出窗口
        if (toolName === 'showPage') {
          const { title, content, type } = parsedInput as {
            title: string
            content: string
            type: PageWindowType
          }
          openPageWindow(title, content, type || 'document')
        }

        if (toolName === 'callVibeAgent') {
          addThinkingStep({
            type: 'agent_call',
            content: '调用氛围智能体',
            agent: 'master',
            toolName,
            toolInput: parsedInput
          })
        } else {
          addThinkingStep({
            type: 'tool_start',
            content: `调用 ${toolName}`,
            agent: 'master',
            toolName,
            toolInput: parsedInput
          })
        }
      },
      onToolEnd: (toolName, result) => {
        if (toolName === 'say') {
          // say 工具 → ai_response（TTS 已在 tool_start 触发，这里不再调用）
          addThinkingStep({
            type: 'ai_response',
            content: result,
            agent: 'master',
            toolName
          })
        } else {
          addThinkingStep({
            type: 'tool_end',
            content: `${toolName} 完成`,
            agent: 'master',
            toolName,
            toolOutput: result
          })
          // 应用工具结果（跳过非 JSON 工具）
          if (toolName !== 'getProjectIntro' && toolName !== 'getEnvironment' && toolName !== 'setEnvironment') {
            applyToolResult(toolName, result)
          }
          // 记忆工具：同步后端记忆到前端
          if (toolName === 'saveMemory' || toolName === 'updateMemory') {
            memoryStore.fetchFromBackend(sessionId.value)
          }
        }
      },
      onComplete: () => {
        addThinkingStep({ type: 'complete', content: '完成', agent: 'master' })
        masterRunning.value = false
      },
      onError: (code, message) => {
        addThinkingStep({ type: 'error', content: message, agent: 'master' })
        masterRunning.value = false
      }
    })
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
    audio.volume = audioVolume.value
    audio.muted = false
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

  function setVolume(volume: number) {
    audioVolume.value = Math.max(0, Math.min(1, volume))
    audio.volume = audioVolume.value
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
    masterRunning,
    vibeRunning,
    masterChain,
    vibeChain,
    error,
    theme,
    demoMode,
    chainExpanded,
    // 弹窗状态
    pageWindows,
    // 音频状态
    isPlaying,
    audioProgress,
    audioVolume,
    currentPlaylistIndex,
    // 计算属性
    safetyMode,
    hasActivePlan,
    // Actions
    setEnvironment,
    clearThinkingChain,
    addThinkingStep,
    analyzeStream,
    analyze,
    toggleTheme,
    startDrivingSimulation: () => { drivingSimulationActive.value = true },
    stopDrivingSimulation: () => { drivingSimulationActive.value = false },
    drivingSimulationActive,
    resetSession,
    sendMessage,
    // 音频控制
    unlockAudio,
    playMusic,
    toggleAudio,
    setVolume,
    // 歌单控制
    playNext,
    playPrevious,
    playSongAt,
    // TTS 控制
    ttsSpeaking,
    speakTTS,
    stopTTS,
    // 弹窗控制
    openPageWindow,
    closePageWindow,
    focusPageWindow,
    updateWindowPosition,
    updateWindowSize,
    toggleWindowMinimize,
  }
})
