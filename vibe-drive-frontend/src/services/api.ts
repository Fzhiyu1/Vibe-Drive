import type {
  AnalyzeRequest,
  AnalyzeResponse,
  ApiResponse,
  Environment,
  FeedbackRequest,
  ScenarioType,
  VibeStatus,
} from '@/types/api'

const API_BASE = '/api/vibe'
const MASTER_API_BASE = '/api/master'

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })
  return response.json()
}

export const vibeApi = {
  /**
   * 同步分析环境
   */
  async analyze(req: AnalyzeRequest): Promise<ApiResponse<AnalyzeResponse>> {
    return request<AnalyzeResponse>('/analyze', {
      method: 'POST',
      body: JSON.stringify(req),
    })
  },

  /**
   * 获取会话状态
   */
  async getStatus(sessionId: string): Promise<ApiResponse<VibeStatus>> {
    return request<VibeStatus>(`/status?sessionId=${encodeURIComponent(sessionId)}`)
  },

  /**
   * 提交用户反馈
   */
  async feedback(req: FeedbackRequest): Promise<ApiResponse<void>> {
    return request<void>('/feedback', {
      method: 'POST',
      body: JSON.stringify(req),
    })
  },

  /**
   * 获取模拟场景
   */
  async getScenario(type: ScenarioType): Promise<Environment> {
    const response = await fetch(
      `${API_BASE}/simulator/scenario?type=${encodeURIComponent(type)}`
    )
    return response.json()
  },

  /**
   * AI 生成环境
   */
  async generateEnvironment(description: string): Promise<Environment> {
    const response = await fetch(`${API_BASE}/environment/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ description }),
    })
    return response.json()
  },
}

// ============ 主智能体 API ============

export interface MasterChatCallbacks {
  onToken?: (text: string) => void
  onToolStart?: (toolName: string, input: unknown) => void
  onToolEnd?: (toolName: string, result: string) => void
  onComplete?: () => void
  onError?: (code: string, message: string) => void
}

export const masterApi = {
  /**
   * 流式对话
   */
  async chatStream(
    sessionId: string,
    message: string,
    callbacks: MasterChatCallbacks
  ): Promise<void> {
    const url = `${MASTER_API_BASE}/chat/stream?sessionId=${encodeURIComponent(sessionId)}`

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message }),
    })

    if (!response.ok) {
      callbacks.onError?.('HTTP_ERROR', `HTTP ${response.status}`)
      return
    }

    const reader = response.body?.getReader()
    if (!reader) {
      callbacks.onError?.('NO_BODY', 'No response body')
      return
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          const eventType = line.slice(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (!data) continue

          try {
            const parsed = JSON.parse(data)
            // 根据事件类型处理
            if (parsed.text !== undefined) {
              callbacks.onToken?.(parsed.text)
            } else if (parsed.toolName && parsed.result !== undefined) {
              callbacks.onToolEnd?.(parsed.toolName, parsed.result)
            } else if (parsed.toolName && parsed.input !== undefined) {
              callbacks.onToolStart?.(parsed.toolName, parsed.input)
            } else if (parsed.code) {
              callbacks.onError?.(parsed.code, parsed.message || 'Unknown error')
            }
          } catch {
            // 忽略解析错误
          }
        }
      }
    }

    callbacks.onComplete?.()
  },
}
