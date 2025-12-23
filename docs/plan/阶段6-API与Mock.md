# 阶段 6: API 与 Mock

## 状态

⚪ 待开始

## 目标

实现后端 REST API 和 SSE 流式接口，创建环境数据模拟器，支持前端实时获取氛围方案。

## 前置依赖

- [x] 阶段 5: Agent 编排（需要 Agent 可正常工作）

## 任务清单

### REST API

- [ ] POST `/api/vibe/analyze` - 分析环境，返回氛围方案（同步）
  - 输入：Environment JSON
  - 输出：AmbiencePlan JSON + TokenUsage + ToolExecutions
- [ ] GET `/api/vibe/status` - 获取当前氛围状态
- [ ] POST `/api/vibe/feedback` - 用户反馈（喜欢/不喜欢）
- [ ] 实现统一响应格式（ApiResponse）
- [ ] 实现全局异常处理

### SSE 流式 API

- [ ] GET `/api/vibe/analyze/stream` - 流式分析环境（SSE）
  - 事件类型：token, tool_start, tool_end, complete, error
  - 使用 LangChain4j TokenStream 实现
- [ ] GET `/api/vibe/events` - 实时事件订阅（SSE）
  - 事件类型：ambience_changed, safety_mode_changed, agent_status_changed, environment_update, heartbeat
  - 支持主题订阅：`?topics=ambience,safety,status,environment`
- [ ] 实现 SseEventPublisher 事件发布器接口
- [ ] 实现 AmbienceEventPublisher 氛围变化事件发布器
- [ ] 实现心跳机制（每 30 秒）

### Mock 环境数据生成器

- [ ] 创建 EnvironmentSimulator 类
- [ ] 实现场景模板
  - 深夜归途场景
  - 周末家庭出游场景
  - 通勤早高峰场景
  - 自定义随机场景
- [ ] 实现环境渐变逻辑（模拟真实驾驶）
- [ ] 支持定时推送环境变化

### API 文档

- [ ] 集成 Swagger/OpenAPI
- [ ] 编写 API 使用说明

### 测试

- [ ] 编写 Controller 单元测试
- [ ] 编写 API 集成测试
- [ ] 使用 Postman 测试 API
- [ ] 测试 SSE 流式输出

## 相关文件

```
src/main/java/com/vibe/controller/
├── VibeController.java           # REST API
└── VibeStreamController.java     # SSE 流式 API

src/main/java/com/vibe/sse/
├── SseEventPublisher.java        # SSE 事件发布器接口
├── AmbienceEventPublisher.java   # 氛围变化事件发布器
└── SafetyModeEventPublisher.java # 安全模式变化事件发布器

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
└── ScenarioTemplate.java
```

## 完成标准

- [ ] REST API 可正常调用并返回正确结果
- [ ] SSE 流式 API 可正常推送事件
- [ ] Mock 数据生成器可模拟多种场景
- [ ] API 文档完整
- [ ] 代码已提交到 Git

## 问题与笔记

（开发过程中遇到的问题和解决方案记录在这里）
