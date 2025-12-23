# Vibe Drive API 接口规范

## 1. 概述

本文档定义 Vibe Drive 后端 API 的接口规范，供前端和外部系统调用。

### 1.1 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080/api` |
| 协议 | HTTP/HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |

### 1.2 API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/vibe/analyze` | 分析环境，返回氛围方案（同步） |
| GET | `/vibe/analyze/stream` | 流式分析环境（SSE） |
| GET | `/vibe/status` | 获取 Vibe Agent 状态 |
| GET | `/vibe/events` | 订阅实时事件（SSE） |
| POST | `/vibe/control` | 控制 Vibe Agent |
| POST | `/vibe/feedback` | 提交用户反馈 |
| GET | `/vibe/history` | 获取氛围历史 |

---

## 2. 通用规范

### 2.1 请求头

```http
Content-Type: application/json
Accept: application/json
X-Request-Id: <uuid>  # 可选，用于请求追踪
```

### 2.2 响应格式

**成功响应**

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

**错误响应**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_ENVIRONMENT",
    "message": "环境数据校验失败：speed 超出范围"
  },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

### 2.3 错误码定义

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| `INVALID_REQUEST` | 400 | 请求参数无效 |
| `INVALID_ENVIRONMENT` | 400 | 环境数据校验失败 |
| `AGENT_NOT_RUNNING` | 503 | Vibe Agent 未运行 |
| `AGENT_BUSY` | 503 | Vibe Agent 正在处理中 |
| `LLM_ERROR` | 502 | LLM 服务调用失败 |
| `LLM_TIMEOUT` | 504 | LLM 服务超时 |
| `INTERNAL_ERROR` | 500 | 内部服务错误 |

---

## 3. 核心 API

### 3.1 分析环境 - POST /vibe/analyze

分析环境数据，返回氛围编排方案。

**请求**

```http
POST /api/vibe/analyze
Content-Type: application/json
```

```json
{
  "sessionId": "user-123-vehicle-001",
  "environment": {
    "gpsTag": "highway",
    "weather": "rainy",
    "speed": 80,
    "userMood": "tired",
    "timeOfDay": "midnight",
    "passengerCount": 1,
    "routeType": "highway"
  },
  "preferences": {
    "musicGenre": "jazz",
    "narrativeEnabled": true
  },
  "async": false
}
```

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话 ID（对应 LangChain4j `@MemoryId`，用于隔离多用户/多车辆上下文） |
| environment | Object | 是 | 环境数据 |
| preferences | Object | 否 | 用户偏好 |
| async | boolean | 否 | 是否异步处理，默认 false |

**响应**

```json
{
  "success": true,
  "data": {
    "action": "APPLY",
    "message": null,
    "plan": {
      "id": "plan_20251223_001",
      "music": {
        "songs": [
          {
            "id": "song_001",
            "title": "夜空中最亮的星",
            "artist": "逃跑计划",
            "album": "世界",
            "duration": 252,
            "bpm": 76,
            "genre": "rock",
            "coverUrl": "https://example.com/cover.jpg"
          }
        ],
        "mood": "calm",
        "genre": "rock"
      },
      "light": {
        "color": {
          "hex": "#FFE4B5",
          "temperature": 2700
        },
        "brightness": 30,
        "mode": "breathing",
        "transitionDuration": 2000
      },
      "narrative": {
        "text": "夜深了，窗外的雨声和这首歌很配，让音乐陪你安全到家。",
        "voice": "gentle_female",
        "speed": 0.9,
        "volume": 0.7,
        "emotion": "gentle"
      },
      "safetyMode": "L2_FOCUS",
      "reasoning": "检测到深夜+雨天+疲劳状态，用户需要放松舒缓的氛围...",
      "createdAt": "2025-12-23T23:30:00Z"
    },
    "tokenUsage": {
      "inputTokenCount": 1234,
      "outputTokenCount": 567,
      "totalTokenCount": 1801
    },
    "toolExecutions": [
      {
        "toolName": "recommendMusic",
        "arguments": "{\"mood\":\"calm\",\"timeOfDay\":\"midnight\",\"passengerCount\":1,\"genre\":\"jazz\"}",
        "durationMs": 120,
        "success": true
      },
      {
        "toolName": "generateNarrative",
        "arguments": "{\"theme\":\"comfort\"}",
        "durationMs": 80,
        "success": true
      }
    ],
    "processingTimeMs": 1850
  },
  "timestamp": "2025-12-23T23:30:00Z"
}
```

**响应字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| action | string | 动作：`APPLY`（应用新方案）/`NO_ACTION`（本次不更新，例如 L3 静默模式） |
| message | string | 可选提示信息（通常在 `NO_ACTION` 时返回原因） |
| plan | Object | 生成的氛围方案（`AmbiencePlan`，`NO_ACTION` 时为 null） |
| tokenUsage | Object | Token 使用统计（成本/性能监控） |
| toolExecutions | Array | Tool 执行详情（调试/性能分析） |
| processingTimeMs | number | 处理耗时（毫秒） |

---

### 3.2 获取状态 - GET /vibe/status

获取 Vibe Agent 当前状态。

**请求**

```http
GET /api/vibe/status
```

**响应**

```json
{
  "success": true,
  "data": {
    "running": true,
    "safetyMode": "L1_NORMAL",
    "currentPlan": {
      "id": "plan_20251223_001",
      "music": { ... },
      "light": { ... },
      "narrative": { ... }
    },
    "lastEnvironment": {
      "gpsTag": "urban",
      "weather": "sunny",
      "speed": 45,
      "userMood": "calm",
      "timeOfDay": "morning",
      "passengerCount": 1,
      "routeType": "urban"
    },
    "lastUpdateTime": "2025-12-23T10:25:00Z",
    "stats": {
      "totalPlansGenerated": 42,
      "avgProcessingTimeMs": 1650,
      "uptime": "2h 30m"
    }
  },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

---

### 3.3 控制 Agent - POST /vibe/control

控制 Vibe Agent 的运行状态。

**请求**

```http
POST /api/vibe/control
Content-Type: application/json
```

```json
{
  "command": "start"
}
```

**支持的命令**

| 命令 | 说明 |
|------|------|
| `start` | 启动 Agent |
| `stop` | 停止 Agent |
| `refresh` | 强制刷新氛围 |
| `setSafetyMode` | 设置安全模式（需要 `mode` 参数） |

**设置安全模式示例**

```json
{
  "command": "setSafetyMode",
  "params": {
    "mode": "L2_FOCUS"
  }
}
```

**响应**

```json
{
  "success": true,
  "data": {
    "command": "start",
    "executed": true,
    "newStatus": "running"
  },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

---

### 3.4 提交反馈 - POST /vibe/feedback

提交用户对氛围方案的反馈。

**请求**

```http
POST /api/vibe/feedback
Content-Type: application/json
```

```json
{
  "planId": "plan_20251223_001",
  "rating": 4,
  "feedback": {
    "musicLiked": true,
    "lightLiked": true,
    "narrativeLiked": false,
    "comment": "音乐很棒，但叙事有点啰嗦"
  }
}
```

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | string | 是 | 方案 ID |
| rating | number | 是 | 评分 1-5 |
| feedback | Object | 否 | 详细反馈 |

**响应**

```json
{
  "success": true,
  "data": {
    "feedbackId": "fb_001",
    "received": true
  },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

---

### 3.5 获取历史 - GET /vibe/history

获取氛围方案历史记录。

**请求**

```http
GET /api/vibe/history?limit=10&offset=0
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | number | 否 | 返回数量，默认 10，最大 50 |
| offset | number | 否 | 偏移量，默认 0 |
| startTime | string | 否 | 开始时间（ISO 8601） |
| endTime | string | 否 | 结束时间（ISO 8601） |

**响应**

```json
{
  "success": true,
  "data": {
    "total": 42,
    "items": [
      {
        "planId": "plan_20251223_001",
        "environment": { ... },
        "plan": { ... },
        "createdAt": "2025-12-23T10:25:00Z"
      }
    ]
  },
  "timestamp": "2025-12-23T10:30:00Z"
}
```

### 3.6 流式分析环境 - GET /vibe/analyze/stream

使用 Server-Sent Events (SSE) 流式返回分析过程和结果。适用于需要实时展示 LLM 输出的场景。

**请求**

```http
GET /api/vibe/analyze/stream?sessionId=user-123-vehicle-001&environment={...}
Accept: text/event-stream
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话 ID |
| environment | string | 是 | URL 编码的环境 JSON |
| preferences | string | 否 | URL 编码的用户偏好 JSON |
| debug | boolean | 否 | 是否开启调试事件（`token/tool_start/tool_end`），默认 false |

**SSE 事件流示例**

```
# （debug=true 时才会出现 token/tool_* 事件）
event: token
data: {"content": "正在分析"}

event: token
data: {"content": "深夜雨天场景"}

event: tool_start
data: {"toolName": "recommendMusic", "arguments": "{\"mood\":\"calm\"}", "timestamp": "2025-12-23T23:30:00Z"}

event: tool_end
data: {"toolName": "recommendMusic", "durationMs": 120, "success": true, "timestamp": "2025-12-23T23:30:00Z"}

event: tool_start
data: {"toolName": "generateNarrative", "arguments": "{\"theme\":\"comfort\"}", "timestamp": "2025-12-23T23:30:01Z"}

event: tool_end
data: {"toolName": "generateNarrative", "durationMs": 80, "success": true, "timestamp": "2025-12-23T23:30:01Z"}

event: complete
data: {"action":"APPLY","message":null,"plan":{...},"tokenUsage":{"inputTokenCount":1234,"outputTokenCount":567,"totalTokenCount":1801},"toolExecutions":[...],"processingTimeMs":1850}

```

**事件类型**

| 事件类型 | 说明 | data 结构 |
|----------|------|-----------|
| `token` | LLM 输出的 token（`debug=true`） | `{"content": "..."}` |
| `tool_start` | Tool 开始执行（`debug=true`） | `{"toolName": "...", "arguments": "...", "timestamp": "..."}` |
| `tool_end` | Tool 执行完成（`debug=true`） | `{"toolName": "...", "durationMs": N, "success": bool, "timestamp": "..."}` |
| `complete` | 分析完成 | 完整的 `AnalyzeResponse`（与 `POST /vibe/analyze` 的 `data` 一致） |
| `error` | 发生错误 | `{"code": "...", "message": "..."}` |

**错误事件示例**

```
event: error
data: {"code": "LLM_TIMEOUT", "message": "LLM 服务超时"}

```

**前端使用示例（JavaScript）**

> `EventSource` 无法自定义请求头；如需 `Authorization: Bearer ...`，建议使用 fetch-based SSE 客户端（如 `@microsoft/fetch-event-source`），或改用 Cookie 鉴权。

```javascript
import { fetchEventSource } from '@microsoft/fetch-event-source';

const params = new URLSearchParams({
  sessionId: 'user-123-vehicle-001',
  environment: JSON.stringify(environmentData),
  debug: 'false'
});

await fetchEventSource(`/api/vibe/analyze/stream?${params}`, {
  headers: { Authorization: `Bearer ${token}` },
  onmessage(ev) {
    if (ev.event === 'token') {
      const { content } = JSON.parse(ev.data);
      appendToOutput(content);
    }
    if (ev.event === 'complete') {
      const response = JSON.parse(ev.data);
      updateUI(response.plan);
    }
  },
  onerror(err) {
    console.error(err);
    throw err;
  }
});
```

---

## 4. SSE 实时事件 API

### 4.1 订阅实时事件 - GET /vibe/events

使用 Server-Sent Events (SSE) 订阅系统实时事件，替代 WebSocket 实现服务端推送。

**请求**

```http
GET /api/vibe/events?sessionId=user-123-vehicle-001&topics=ambience,safety,status
Accept: text/event-stream
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话 ID |
| topics | string | 否 | 订阅主题，逗号分隔，默认全部 |

**支持的主题**

| 主题 | 说明 |
|------|------|
| `ambience` | 氛围方案变化 |
| `safety` | 安全模式变化 |
| `status` | Agent 状态变化 |
| `environment` | 环境数据更新 |

### 4.2 事件格式

**氛围变化事件**

```
event: ambience_changed
data: {"planId": "plan_20251223_002", "music": {...}, "light": {...}, "narrative": {...}, "safetyMode": "L1_NORMAL", "trigger": "environment_change", "timestamp": "2025-12-23T10:35:00Z"}

```

**安全模式变化事件**

```
event: safety_mode_changed
data: {"previousMode": "L1_NORMAL", "currentMode": "L2_FOCUS", "speed": 75, "timestamp": "2025-12-23T10:35:00Z"}

```

**Agent 状态变化事件**

```
event: agent_status_changed
data: {"running": true, "event": "started", "timestamp": "2025-12-23T10:35:00Z"}

```

**环境数据更新事件**

```
event: environment_update
data: {"gpsTag": "highway", "weather": "rainy", "speed": 80, "timestamp": "2025-12-23T10:35:00Z"}

```

**心跳事件（每 30 秒）**

```
event: heartbeat
data: {"timestamp": "2025-12-23T10:35:00Z"}

```

### 4.3 事件类型汇总

| 事件类型 | 说明 |
|----------|------|
| `ambience_changed` | 氛围方案变化 |
| `safety_mode_changed` | 安全模式变化 |
| `agent_status_changed` | Agent 状态变化 |
| `environment_update` | 环境数据更新 |
| `heartbeat` | 心跳保活 |

### 4.4 前端使用示例

```javascript
import { fetchEventSource } from '@microsoft/fetch-event-source';

const params = new URLSearchParams({
  sessionId: 'user-123-vehicle-001',
  topics: 'ambience,safety'
});

const controller = new AbortController();

fetchEventSource(`/api/vibe/events?${params}`, {
  headers: { Authorization: `Bearer ${token}` },
  signal: controller.signal,
  onmessage(ev) {
    if (ev.event === 'ambience_changed') {
      const data = JSON.parse(ev.data);
      updateAmbienceUI(data);
    }
    if (ev.event === 'safety_mode_changed') {
      const data = JSON.parse(ev.data);
      updateSafetyModeUI(data);
    }
    if (ev.event === 'heartbeat') {
      console.log('Connection alive');
    }
  },
  onerror(err) {
    console.warn('SSE connection error', err);
  }
});

// 页面卸载时关闭连接
window.addEventListener('beforeunload', () => controller.abort());
```

**（无鉴权或 Cookie 鉴权场景）**也可直接使用浏览器内置 `EventSource`：

```javascript
const eventSource = new EventSource(`/api/vibe/events?${params}`);

eventSource.addEventListener('ambience_changed', (e) => {
  const data = JSON.parse(e.data);
  updateAmbienceUI(data);
});

eventSource.addEventListener('safety_mode_changed', (e) => {
  const data = JSON.parse(e.data);
  updateSafetyModeUI(data);
});

eventSource.addEventListener('heartbeat', (e) => {
  console.log('Connection alive');
});

// SSE 自动重连，但可以监听错误
eventSource.onerror = (e) => {
  console.warn('SSE connection error, will auto-reconnect');
};

// 页面卸载时关闭连接
window.addEventListener('beforeunload', () => {
  eventSource.close();
});
```

### 4.5 SSE vs WebSocket 对比

| 特性 | SSE | WebSocket |
|------|-----|-----------|
| 通信方向 | 单向（服务端→客户端） | 双向 |
| 协议 | HTTP | 独立协议 |
| 重连 | 浏览器自动重连 | 需手动实现 |
| HTTP/2 | 原生支持多路复用 | 需额外处理 |
| 复杂度 | 低 | 高 |
| LangChain4j | TokenStream 天然适配 | 需手动转换 |

**为何选择 SSE**：
- Vibe Drive 的实时通信是单向的（服务端推送氛围变化）
- 客户端的操作（如提交环境数据）通过 REST API 完成
- SSE 与 LangChain4j TokenStream 天然适配
- 更简单的实现和更好的 HTTP/2 兼容性

---

## 5. Mock 数据 API

用于前端开发和演示的 Mock API。

### 5.1 生成随机环境 - GET /mock/environment

```http
GET /api/mock/environment
```

**响应**

```json
{
  "success": true,
  "data": {
    "gpsTag": "highway",
    "weather": "sunny",
    "speed": 85,
    "userMood": "calm",
    "timeOfDay": "afternoon",
    "passengerCount": 2,
    "routeType": "highway",
    "timestamp": "2025-12-23T14:30:00Z"
  }
}
```

### 5.2 模拟场景 - POST /mock/scenario

```http
POST /api/mock/scenario
Content-Type: application/json
```

```json
{
  "scenario": "midnight_rain_highway"
}
```

**预设场景**

| 场景 ID | 说明 |
|---------|------|
| `midnight_rain_highway` | 深夜雨天高速 |
| `morning_sunny_commute` | 早晨晴天通勤 |
| `weekend_family_trip` | 周末家庭出游 |
| `evening_coastal_drive` | 傍晚海滨兜风 |
| `stressed_traffic_jam` | 压力堵车场景 |

---

## 6. Java Controller 实现参考

```java
@RestController
@RequestMapping("/api/vibe")
@RequiredArgsConstructor
public class VibeController {

    private final VibeAgent vibeAgent;
    private final ObjectMapper objectMapper;
    private final AmbienceService ambienceService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AnalyzeResponse>> analyze(
            @Valid @RequestBody AnalyzeRequest request) {

        long startTime = System.currentTimeMillis();

        Result<AmbiencePlan> result = vibeAgent.analyzeEnvironment(
            request.sessionId(),
            toJson(request.environment()),
            request.preferences() == null ? null : toJson(request.preferences())
        );

        long processingTime = System.currentTimeMillis() - startTime;

        AnalyzeResponse response = AnalyzeResponse.applied(
            result.content(),
            TokenUsageInfo.from(result.tokenUsage()),
            result.toolExecutions().stream().map(ToolExecutionInfo::from).toList(),
            processingTime
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<VibeAgentStatus>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(vibeAgent.getStatus()));
    }

    @PostMapping("/control")
    public ResponseEntity<ApiResponse<ControlResult>> control(
            @Valid @RequestBody ControlRequest request) {

        ControlResult result = switch (request.command()) {
            case "start" -> {
                vibeAgent.start();
                yield new ControlResult("start", true, "running");
            }
            case "stop" -> {
                vibeAgent.stop();
                yield new ControlResult("stop", true, "stopped");
            }
            case "refresh" -> {
                vibeAgent.forceRefresh();
                yield new ControlResult("refresh", true, "refreshed");
            }
            default -> throw new IllegalArgumentException("Unknown command");
        };

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<FeedbackResult>> feedback(
            @Valid @RequestBody FeedbackRequest request) {

        String feedbackId = ambienceService.saveFeedback(request);
        return ResponseEntity.ok(ApiResponse.success(
            new FeedbackResult(feedbackId, true)
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResult<AmbienceHistory>>> getHistory(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        PagedResult<AmbienceHistory> history = ambienceService.getHistory(limit, offset);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
```

### 6.2 SSE 流式 Controller 实现参考

```java
@RestController
@RequestMapping("/api/vibe")
@RequiredArgsConstructor
public class VibeStreamController {

    private final VibeAgent vibeAgent;
    private final ObjectMapper objectMapper;
    private final AmbienceEventPublisher ambienceEventPublisher;
    private final SafetyModeEventPublisher safetyModeEventPublisher;

    /**
     * 流式分析环境（SSE）
     * 使用 LangChain4j TokenStream 实现流式输出
     */
    @GetMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeStream(
            @RequestParam String sessionId,
            @RequestParam String environment,
            @RequestParam(required = false) String preferences,
            @RequestParam(defaultValue = "false") boolean debug) {

        return Flux.create(sink -> {
            long startTime = System.currentTimeMillis();

            // 获取 TokenStream（LangChain4j 流式输出）
            TokenStream tokenStream = vibeAgent.analyzeEnvironmentStreaming(
                sessionId, environment, preferences
            );

            // 调试事件：逐 token 输出（默认关闭，避免 UI 过于“吵”）
            if (debug) {
                tokenStream.onNext(token -> sink.next(ServerSentEvent.<String>builder()
                    .event("token")
                    .data(toJson(new TokenEvent(token)))
                    .build()));
            }

            // 处理完成事件
            tokenStream.onComplete(response -> {
                long processingTime = System.currentTimeMillis() - startTime;

                AnalyzeResponse analyzeResponse = AnalyzeResponse.applied(
                    response.content(),
                    TokenUsageInfo.from(response.tokenUsage()),
                    response.toolExecutions().stream().map(ToolExecutionInfo::from).toList(),
                    processingTime
                );

                sink.next(ServerSentEvent.<String>builder()
                    .event("complete")
                    .data(toJson(analyzeResponse))
                    .build());

                sink.complete();
            });

            // 处理错误事件
            tokenStream.onError(error -> {
                sink.next(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(toJson(new ErrorEvent("LLM_ERROR", error.getMessage())))
                    .build());
                sink.complete();
            });

            // 启动流
            tokenStream.start();
        });
    }

    /**
     * 订阅实时事件（SSE）
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribeEvents(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "ambience,safety,status,environment") String topics) {

        Set<String> topicSet = Set.of(topics.split(","));

        return Flux.merge(
            // 氛围变化事件
            topicSet.contains("ambience") ?
                ambienceEventPublisher.subscribe(sessionId)
                    .map(event -> ServerSentEvent.<String>builder()
                        .event("ambience_changed")
                        .data(toJson(event))
                        .build()) :
                Flux.empty(),

            // 安全模式变化事件
            topicSet.contains("safety") ?
                safetyModeEventPublisher.subscribe(sessionId)
                    .map(event -> ServerSentEvent.<String>builder()
                        .event("safety_mode_changed")
                        .data(toJson(event))
                        .build()) :
                Flux.empty(),

            // 心跳事件（每 30 秒）
            Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<String>builder()
                    .event("heartbeat")
                    .data(toJson(new HeartbeatEvent(Instant.now())))
                    .build())
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### 6.3 VibeAgent 流式接口定义

```java
public interface VibeAgent {

    // 方式1：同步返回 + 元数据（非流式）
    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("请分析以下车载环境数据：{{environment}}")
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferences
    );

    // 方式2：流式输出（SSE）
    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("请分析以下车载环境数据：{{environment}}")
    TokenStream analyzeEnvironmentStreaming(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferences
    );
}
```

### 6.4 SSE 事件发布器

```java
/**
 * SSE 事件发布器接口
 */
public interface SseEventPublisher<T> {
    Flux<T> subscribe(String sessionId);
    void publish(String sessionId, T event);
    void broadcast(T event);
}

/**
 * 氛围变化事件发布器
 */
@Component
public class AmbienceEventPublisher implements SseEventPublisher<AmbienceChangedEvent> {

    private final Map<String, Sinks.Many<AmbienceChangedEvent>> sinks = new ConcurrentHashMap<>();

    @Override
    public Flux<AmbienceChangedEvent> subscribe(String sessionId) {
        return sinks.computeIfAbsent(sessionId,
            id -> Sinks.many().multicast().onBackpressureBuffer()
        ).asFlux();
    }

    @Override
    public void publish(String sessionId, AmbienceChangedEvent event) {
        Sinks.Many<AmbienceChangedEvent> sink = sinks.get(sessionId);
        if (sink != null) {
            sink.tryEmitNext(event);
        }
    }

    @Override
    public void broadcast(AmbienceChangedEvent event) {
        sinks.values().forEach(sink -> sink.tryEmitNext(event));
    }
}
```

---

## 7. OpenAPI 文档

API 文档通过 SpringDoc 自动生成：

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vibeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Vibe Drive API")
                .description("车载智能氛围编排系统 API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Vibe Drive Team")
                    .email("vibe@example.com")))
            .addTagsItem(new Tag()
                .name("vibe")
                .description("氛围编排相关接口"));
    }
}
```
