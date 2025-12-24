// ============ 枚举类型 ============

export type GpsTag = 'HIGHWAY' | 'TUNNEL' | 'BRIDGE' | 'URBAN' | 'SUBURBAN' | 'MOUNTAIN' | 'COASTAL' | 'PARKING'
export type Weather = 'SUNNY' | 'CLOUDY' | 'RAINY' | 'SNOWY' | 'FOGGY'
export type UserMood = 'HAPPY' | 'CALM' | 'TIRED' | 'STRESSED' | 'EXCITED'
export type TimeOfDay = 'DAWN' | 'MORNING' | 'NOON' | 'AFTERNOON' | 'EVENING' | 'NIGHT' | 'MIDNIGHT'
export type RouteType = 'HIGHWAY' | 'URBAN' | 'MOUNTAIN' | 'COASTAL' | 'TUNNEL'
export type SafetyMode = 'L1_NORMAL' | 'L2_FOCUS' | 'L3_SILENT'
export type LightMode = 'STATIC' | 'BREATHING' | 'GRADIENT' | 'PULSE'
export type FeedbackType = 'LIKE' | 'DISLIKE' | 'SKIP'
export type NarrativeEmotion = 'NEUTRAL' | 'WARM' | 'ENERGETIC' | 'CALM' | 'GENTLE'
export type AnalyzeAction = 'APPLY' | 'NO_ACTION'
export type ScenarioType = 'LATE_NIGHT_RETURN' | 'WEEKEND_FAMILY_TRIP' | 'MORNING_COMMUTE' | 'RANDOM'

// 新增枚举类型
export type ScentType = 'LAVENDER' | 'PEPPERMINT' | 'OCEAN' | 'FOREST' | 'CITRUS' | 'VANILLA' | 'NONE'
export type MassageMode = 'RELAX' | 'ENERGIZE' | 'COMFORT' | 'SPORT' | 'OFF'
export type MassageZone = 'BACK' | 'LUMBAR' | 'SHOULDER' | 'THIGH' | 'ALL'

// ============ 核心数据结构 ============

// 新增数据结构
export interface PoiInfo {
  name: string
  category: string
  distanceMeters: number
}

export interface LocationInfo {
  latitude: number
  longitude: number
  cityName?: string
  districtName?: string
  roadName?: string
  nearbyPois?: PoiInfo[]
}

export interface DriverBiometrics {
  heartRate: number
  stressLevel: number
  fatigueLevel: number
  bodyTemperature: number
}

export interface ScentSetting {
  type: ScentType
  intensity: number
  durationMinutes: number
}

export interface MassageSetting {
  mode: MassageMode
  zones: MassageZone[]
  intensity: number
}

export interface Environment {
  gpsTag: GpsTag
  weather: Weather
  speed: number
  userMood: UserMood
  timeOfDay: TimeOfDay
  passengerCount: number
  routeType: RouteType
  biometrics?: DriverBiometrics
  location?: LocationInfo
  timestamp?: string
}

export interface Song {
  id: string
  title: string
  artist: string
  album?: string
  coverUrl?: string
  duration: number // 秒
  previewUrl?: string
}

export interface BpmRange {
  min: number
  max: number
}

export interface MusicRecommendation {
  songs: Song[]
  mood?: string
  genre?: string
  bpmRange?: BpmRange
}

export interface LightColor {
  hex: string
  temperature?: number // 色温 K
}

export interface ZoneSetting {
  zone: string
  color: LightColor
  brightness: number
}

export interface LightSetting {
  color?: LightColor
  brightness: number
  mode: LightMode
  transitionDuration: number
  zones?: ZoneSetting[]
}

export interface Narrative {
  text: string
  voice: string
  speed: number
  volume: number
  emotion: NarrativeEmotion
}

export interface AmbiencePlan {
  id: string
  music?: MusicRecommendation
  light?: LightSetting
  narrative?: Narrative
  scent?: ScentSetting
  massage?: MassageSetting
  safetyMode: SafetyMode
  reasoning?: string
  createdAt: string
}

// ============ API 请求/响应 ============

export interface AnalyzeRequest {
  sessionId: string
  environment: Environment
  preferences?: Record<string, unknown>
  async?: boolean
}

export interface TokenUsageInfo {
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

export interface ToolExecutionInfo {
  toolName: string
  input: unknown
  output: unknown
  durationMs: number
}

export interface AnalyzeResponse {
  action: AnalyzeAction
  message?: string
  plan?: AmbiencePlan
  tokenUsage?: TokenUsageInfo
  toolExecutions: ToolExecutionInfo[]
  processingTimeMs: number
}

export interface VibeStatus {
  sessionId: string
  agentRunning: boolean
  currentSafetyMode: SafetyMode
  currentPlan?: AmbiencePlan
  lastEnvironment?: Environment
  lastUpdateTime: string
}

export interface FeedbackRequest {
  sessionId: string
  planId: string
  type: FeedbackType
  comment?: string
}

export interface ErrorInfo {
  code: string
  message: string
  details?: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: ErrorInfo
  timestamp: string
}

// ============ SSE 事件类型 ============

export interface TokenEvent {
  text: string
}

export interface ToolStartEvent {
  toolName: string
  input: unknown
}

export interface ToolEndEvent {
  toolName: string
  result: string
  durationMs: number
}

export interface CompleteEvent {
  plan: AmbiencePlan
  processingTimeMs: number
}

export interface ErrorEvent {
  code: string
  message: string
}

// ============ UI 辅助类型 ============

export interface ThinkingStep {
  type: 'thinking' | 'tool_start' | 'tool_end' | 'complete' | 'error'
  timestamp: number
  content: string
  toolName?: string
  toolInput?: unknown
  toolOutput?: string
}
