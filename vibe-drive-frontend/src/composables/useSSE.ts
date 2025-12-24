import { ref } from 'vue'
import type { AmbiencePlan, Environment } from '@/types/api'

const API_BASE = '/api/vibe'

export interface StreamHandlers {
  onToken?: (text: string) => void
  onToolStart?: (toolName: string, input: unknown) => void
  onToolEnd?: (toolName: string, result: string, durationMs: number) => void
  onComplete?: (plan: AmbiencePlan, processingTimeMs: number) => void
  onError?: (code: string, message: string) => void
}

/**
 * 流式分析 SSE 连接
 * POST /api/vibe/analyze/stream
 */
export function useAnalyzeStream() {
  const isConnected = ref(false)
  let abortController: AbortController | null = null

  async function connect(
    sessionId: string,
    environment: Environment,
    handlers: StreamHandlers,
    options?: { preferences?: string; debug?: boolean }
  ) {
    if (isConnected.value) {
      disconnect()
    }

    abortController = new AbortController()
    isConnected.value = true

    const params = new URLSearchParams({ sessionId })
    if (options?.preferences) params.set('preferences', options.preferences)
    if (options?.debug) params.set('debug', 'true')

    try {
      const response = await fetch(
        `${API_BASE}/analyze/stream?${params.toString()}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(environment),
          signal: abortController.signal,
        }
      )

      if (!response.ok || !response.body) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      let currentEventType = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            try {
              const data = JSON.parse(line.slice(5).trim())
              handleEventWithType(currentEventType, data, handlers)
            } catch (e) {
              console.warn('Failed to parse SSE data:', e)
            }
            currentEventType = ''
          }
        }
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        handlers.onError?.('STREAM_ERROR', (error as Error).message)
      }
    } finally {
      isConnected.value = false
    }
  }

  function handleEventWithType(eventType: string, data: Record<string, unknown>, handlers: StreamHandlers) {
    switch (eventType) {
      case 'token':
        handlers.onToken?.(data.content as string)
        break
      case 'tool_start':
        handlers.onToolStart?.(data.toolName as string, data.input)
        break
      case 'tool_end':
        handlers.onToolEnd?.(
          data.toolName as string,
          data.result as string,
          data.durationMs as number
        )
        break
      case 'complete':
        // AnalyzeResponse 格式: { action, plan, toolExecutions, processingTimeMs }
        handlers.onComplete?.(
          data.plan as AmbiencePlan,
          data.processingTimeMs as number
        )
        break
      case 'error':
        handlers.onError?.(data.code as string, data.message as string)
        break
      default:
        console.log('Unknown SSE event:', eventType, data)
    }
  }

  function disconnect() {
    abortController?.abort()
    abortController = null
    isConnected.value = false
  }

  return { connect, disconnect, isConnected }
}
