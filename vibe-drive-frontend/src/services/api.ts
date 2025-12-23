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
}
