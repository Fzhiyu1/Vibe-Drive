# 阶段 6: API 与 Mock

## 状态

🟢 已完成

## 目标

实现后端 REST API 和 SSE 流式接口，创建环境数据模拟器，支持前端实时获取氛围方案。

## 前置依赖

- [x] 阶段 5: Agent 编排（需要 Agent 可正常工作）

## 任务清单

### REST API

- [x] POST `/api/vibe/analyze` - 分析环境，返回氛围方案（同步）
  - 输入：AnalyzeRequest（包含 Environment）
  - 输出：AnalyzeResponse（plan + toolExecutions；tokenUsage 当前为 null）
- [x] GET `/api/vibe/status?sessionId=...` - 获取当前会话氛围状态
- [x] POST `/api/vibe/feedback` - 用户反馈（like/dislike/skip）
- [x] 实现统一响应格式（ApiResponse）
- [x] 实现全局异常处理（GlobalExceptionHandler）

### SSE 流式 API

- [x] POST `/api/vibe/analyze/stream` - 流式分析环境（SSE）
  - 事件类型：complete, error（debug=true 时包含 token/tool_start/tool_end）
  - 使用 LangChain4j TokenStream 实现
- [x] GET `/api/vibe/events` - 实时事件订阅（SSE）
  - 事件类型：ambience_changed, safety_mode_changed, agent_status_changed, environment_update, heartbeat
  - 支持主题订阅：`?topics=ambience,safety,status,environment`（也支持直接传事件名）
- [x] 实现 SseEventPublisher（连接管理 + 事件发布 + 心跳）
- [x] 在 Controller 中发布 ambience/safety/status 事件（供 /events 订阅）

### Mock 环境数据生成器

- [x] 创建 EnvironmentSimulator 类
- [x] 实现场景模板：深夜归途 / 周末家庭出游 / 通勤早高峰 / 随机场景
- [x] 实现环境渐变逻辑（evolve）
- [ ] 定时推送环境变化（可选，后续）

### API 文档

- [x] 集成 Swagger/OpenAPI（springdoc）
- [x] 编写 API 使用说明（docs/design/api-spec.md）

### 测试

- [x] 现有单元测试通过（模型/服务/编排）
- [ ] Controller / SSE 集成测试（可选，后续）

## 相关文件

```
src/main/java/com/vibe/controller/
├── VibeController.java           # REST API
└── VibeStreamController.java     # SSE 流式 API
    └── GlobalExceptionHandler.java # 全局异常处理

src/main/java/com/vibe/sse/
└── SseEventPublisher.java        # SSE 事件发布器（连接管理 + 心跳）

src/main/java/com/vibe/status/
└── VibeSessionStatusStore.java   # 会话状态缓存

src/main/java/com/vibe/model/event/
├── TokenEvent.java               # Token 输出事件
├── ToolStartEvent.java           # Tool 开始执行事件
├── ToolEndEvent.java             # Tool 执行完成事件
├── AnalyzeResponse.java          # complete 最终结果（复用 API 响应 DTO）
├── ErrorEvent.java               # 错误事件
├── AmbienceChangedEvent.java     # 氛围变化事件
├── SafetyModeChangedEvent.java   # 安全模式变化事件
├── AgentStatusChangedEvent.java  # Agent 状态变化事件
└── HeartbeatEvent.java           # 心跳事件

src/main/java/com/vibe/simulator/
├── EnvironmentSimulator.java
└── ScenarioType.java
```

## 完成标准

- [x] REST API 可正常调用并返回正确结果
- [x] SSE 流式 API 可正常推送事件
- [x] Mock 数据生成器可模拟多种场景
- [x] API 文档完整
- [ ] Git 提交（按需，由用户触发）

## 问题与笔记

- `/api/vibe/analyze/stream` 使用 POST：避免环境 JSON 过长（URL 限制），也方便配合 fetch-based SSE（可带 Authorization Header）。
- `EventSource` 不能自定义请求头：如必须使用 EventSource，需要改成 GET + query（或 Cookie 鉴权）。
