import { ref, onUnmounted } from 'vue'

const API_BASE = '/api/vibe'

export interface VibeEventHandlers {
  onToolStart?: (taskId: string, toolName: string, input: unknown) => void
  onToolEnd?: (taskId: string, toolName: string, result: string) => void
  onComplete?: (taskId: string, plan: unknown) => void
  onError?: (taskId: string, error: string) => void
  onCancelled?: (taskId: string) => void
}

/**
 * 订阅氛围智能体事件
 * GET /api/vibe/events
 */
export function useVibeEvents() {
  const isConnected = ref(false)
  let eventSource: EventSource | null = null

  function connect(sessionId: string, handlers: VibeEventHandlers) {
    if (eventSource) {
      disconnect()
    }

    const url = `${API_BASE}/events?sessionId=${encodeURIComponent(sessionId)}`
    eventSource = new EventSource(url)
    isConnected.value = true

    eventSource.onopen = () => {
      console.log('[useVibeEvents] 连接已建立')
    }

    eventSource.onerror = (e) => {
      console.warn('[useVibeEvents] 连接错误:', e)
      isConnected.value = false
    }

    // 监听氛围工具开始事件
    eventSource.addEventListener('vibe_tool_start', (event) => {
      try {
        const data = JSON.parse(event.data)
        handlers.onToolStart?.(data.taskId, data.toolName, data.input)
      } catch (e) {
        console.warn('[useVibeEvents] 解析 vibe_tool_start 失败:', e)
      }
    })

    // 监听氛围工具完成事件
    eventSource.addEventListener('vibe_tool_end', (event) => {
      try {
        const data = JSON.parse(event.data)
        handlers.onToolEnd?.(data.taskId, data.toolName, data.result)
      } catch (e) {
        console.warn('[useVibeEvents] 解析 vibe_tool_end 失败:', e)
      }
    })

    // 监听氛围编排完成事件
    eventSource.addEventListener('vibe_complete', (event) => {
      try {
        const data = JSON.parse(event.data)
        handlers.onComplete?.(data.taskId, data.plan)
      } catch (e) {
        console.warn('[useVibeEvents] 解析 vibe_complete 失败:', e)
      }
    })

    // 监听氛围编排错误事件
    eventSource.addEventListener('vibe_error', (event) => {
      try {
        const data = JSON.parse(event.data)
        handlers.onError?.(data.taskId, data.error)
      } catch (e) {
        console.warn('[useVibeEvents] 解析 vibe_error 失败:', e)
      }
    })

    // 监听氛围编排取消事件
    eventSource.addEventListener('vibe_cancelled', (event) => {
      try {
        const data = JSON.parse(event.data)
        handlers.onCancelled?.(data.taskId)
      } catch (e) {
        console.warn('[useVibeEvents] 解析 vibe_cancelled 失败:', e)
      }
    })

    // 心跳事件（保持连接）
    eventSource.addEventListener('heartbeat', () => {
      // 忽略心跳
    })
  }

  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
      isConnected.value = false
      console.log('[useVibeEvents] 连接已断开')
    }
  }

  // 组件卸载时自动断开
  onUnmounted(() => {
    disconnect()
  })

  return { connect, disconnect, isConnected }
}
