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
| POST | `/vibe/analyze` | 分析环境，返回氛围方案 |
| GET | `/vibe/status` | 获取 Vibe Agent 状态 |
| POST | `/vibe/control` | 控制 Vibe Agent |
| POST | `/vibe/feedback` | 提交用户反馈 |
| GET | `/vibe/history` | 获取氛围历史 |
| WebSocket | `/ws/vibe` | 实时氛围推送 |

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
    "planId": "plan_20251223_001",
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
| planId | string | 方案唯一标识 |
| music | Object | 音乐推荐 |
| light | Object | 灯光设置（L3 模式下为 null） |
| narrative | Object | 叙事文本 |
| safetyMode | string | 当前安全模式 |
| reasoning | string | Agent 推理过程 |
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
      "planId": "plan_20251223_001",
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

---

## 4. WebSocket API

### 4.1 连接

```
ws://localhost:8080/ws/vibe
```

### 4.2 消息格式

**服务端推送 - 氛围变化**

```json
{
  "type": "AMBIENCE_CHANGED",
  "data": {
    "planId": "plan_20251223_002",
    "music": { ... },
    "light": { ... },
    "narrative": { ... },
    "safetyMode": "L1_NORMAL",
    "trigger": "environment_change"
  },
  "timestamp": "2025-12-23T10:35:00Z"
}
```

**服务端推送 - 安全模式变化**

```json
{
  "type": "SAFETY_MODE_CHANGED",
  "data": {
    "previousMode": "L1_NORMAL",
    "currentMode": "L2_FOCUS",
    "speed": 75
  },
  "timestamp": "2025-12-23T10:35:00Z"
}
```

**服务端推送 - Agent 状态变化**

```json
{
  "type": "AGENT_STATUS_CHANGED",
  "data": {
    "running": true,
    "event": "started"
  },
  "timestamp": "2025-12-23T10:35:00Z"
}
```

**客户端发送 - 订阅**

```json
{
  "type": "SUBSCRIBE",
  "topics": ["ambience", "safety", "status"]
}
```

### 4.3 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `AMBIENCE_CHANGED` | Server → Client | 氛围方案变化 |
| `SAFETY_MODE_CHANGED` | Server → Client | 安全模式变化 |
| `AGENT_STATUS_CHANGED` | Server → Client | Agent 状态变化 |
| `ENVIRONMENT_UPDATE` | Server → Client | 环境数据更新 |
| `SUBSCRIBE` | Client → Server | 订阅主题 |
| `UNSUBSCRIBE` | Client → Server | 取消订阅 |
| `PING` | Client → Server | 心跳 |
| `PONG` | Server → Client | 心跳响应 |

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
    private final AmbienceService ambienceService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AmbiencePlan>> analyze(
            @Valid @RequestBody AnalyzeRequest request) {

        long startTime = System.currentTimeMillis();

        AmbiencePlan plan = vibeAgent.onEnvironmentChange(request.environment());

        long processingTime = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(ApiResponse.success(plan));
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
