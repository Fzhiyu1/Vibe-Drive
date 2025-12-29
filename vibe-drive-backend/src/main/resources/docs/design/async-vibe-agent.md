# 异步氛围智能体设计

## 1. 概述

本文档描述主智能体与氛围智能体之间的异步调用机制。

### 1.1 设计目标

- **即时响应**：用户发出指令后立即得到反馈
- **后台执行**：氛围编排在后台异步执行
- **实时推送**：工具执行过程实时推送给前端
- **结果通知**：编排完成后通知主智能体

### 1.2 当前问题

```
用户: "帮我放松一下"
    ↓
主智能体: say("好的") → callVibeAgent(同步等待 60-120s) → 完成
                              ↑
                         用户等待很久才听到回复
```

### 1.3 目标架构

```
用户: "帮我放松一下"
    ↓
主智能体: say("好的，正在编排") → callVibeAgent(异步) → 立即返回
    ↓                                    ↓
用户立即听到回复                    氛围智能体后台执行
                                         ↓
                                   工具调用实时推送
                                         ↓
                                   完成后通知主智能体
```

---

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    主智能体                              │
│                                                         │
│  工具：                                                  │
│  - say()           对用户说话                            │
│  - callVibeAgent() 异步启动氛围智能体                    │
│  - resetVibe()     终止当前氛围任务                      │
│  - getEnvironment()                                     │
│  - ...                                                  │
└─────────────────────────────────────────────────────────┘
         │                              ↑
         │ 异步启动                     │ 完成/失败消息
         ↓                              │
┌─────────────────────────────────────────────────────────┐
│                 氛围任务管理器 (VibeTaskManager)         │
│                                                         │
│  状态：                                                  │
│  - currentTask: 当前运行的氛围任务（只能有一个）          │
│  - messageQueue: 完成/失败消息队列                       │
│                                                         │
│  方法：                                                  │
│  - startTask()    启动新任务（自动终止旧任务）           │
│  - cancelTask()   终止当前任务                          │
│  - pollMessages() 获取队列中的消息                       │
└─────────────────────────────────────────────────────────┘
         │                              │
         │ 执行                         │ 实时推送
         ↓                              ↓
┌───────────────────────┐    ┌─────────────────────────────┐
│     氛围智能体         │    │    SseEventPublisher        │
│                       │───→│                             │
│  - setLight()         │    │  publish(sessionId, event)  │
│  - setScent()         │    │                             │
│  - batchPlayMusic()   │    └──────────────┬──────────────┘
│  - generateNarrative()│                   │
└───────────────────────┘                   ↓
                                    ┌───────────────┐
                                    │     前端      │
                                    │  实时接收应用  │
                                    └───────────────┘
```

---

## 3. 消息队列机制

### 3.1 消息类型

```java
public sealed interface VibeMessage {
    record Success(String taskId, AmbiencePlan plan, Instant timestamp) implements VibeMessage {}
    record Failed(String taskId, String error, Instant timestamp) implements VibeMessage {}
    record Cancelled(String taskId, Instant timestamp) implements VibeMessage {}
}
```

### 3.2 主智能体处理流程

```
1. 收到用户消息
2. 检查消息队列（pollMessages）
3. 将队列消息 + 用户消息一起给 LLM 处理
4. LLM 生成回复，可能调用工具
5. 返回结果
```

### 3.3 消息注入示例

```
用户消息: "今天天气怎么样？"
队列消息: [VibeMessage.Success(plan=...)]

注入后的 Prompt:
───────────────────
[系统] 氛围编排已完成，结果：音乐-爵士乐，灯光-暖黄色，香氛-薰衣草
[用户] 今天天气怎么样？

LLM 回复:
───────────────────
"今天天气晴朗，对了，氛围已经为您调整好了~"
```

---

## 4. 实时推送机制

氛围智能体执行过程中，工具调用**实时推送**给前端。

### 4.1 执行流程

```
氛围智能体执行：
─────────────────
1. setLight()         → 推送 vibe_tool_end → 前端立即应用灯光
2. setScent()         → 推送 vibe_tool_end → 前端立即应用香氛
3. batchPlayMusic()   → 推送 vibe_tool_end → 前端立即播放音乐
4. generateNarrative()→ 推送 vibe_tool_end → 前端立即播报 TTS
5. 全部完成           → 推送 vibe_complete → 消息入队
```

### 4.2 SSE 事件类型

| 事件类型 | 说明 | 数据 |
|---------|------|------|
| `vibe_tool_start` | 工具开始执行 | `{toolName, input}` |
| `vibe_tool_end` | 工具执行完成 | `{toolName, result}` |
| `vibe_complete` | 编排全部完成 | `{taskId, plan}` |
| `vibe_error` | 编排失败 | `{taskId, error}` |

---

## 5. 并发场景处理

### 5.1 场景一：用户没有新对话

```
主智能体: say("正在编排") → 静默
                              ↓
                        氛围智能体完成，消息入队
                              ↓
                        （等待用户下次对话时处理）
```

### 5.2 场景二：用户发起新对话

```
主智能体: say("正在编排") → 静默
                              ↓
用户: "现在什么天气？"
                              ↓
主智能体: 检查队列 + 处理用户问题
         "今天晴天，对了氛围编排好了"
```

### 5.3 场景三：用户换风格

**规则**：只能有一个氛围任务运行，新任务自动终止旧任务。

```
用户: "帮我放松" → 启动氛围 A
用户: "换个风格" → 终止 A，启动氛围 B
```

### 5.4 场景四：用户取消

```
用户: "帮我放松" → 启动氛围
用户: "算了不要了"
         ↓
主智能体: resetVibe() → 终止当前氛围任务
```

---

## 6. 核心组件设计

### 6.1 VibeTaskManager

```java
@Component
public class VibeTaskManager {
    private volatile VibeTask currentTask;
    private final Queue<VibeMessage> messageQueue = new ConcurrentLinkedQueue<>();

    // 启动新任务（自动终止旧任务）
    public String startTask(String sessionId, Environment env);

    // 终止当前任务
    public void cancelTask(String sessionId);

    // 获取队列中的消息
    public List<VibeMessage> pollMessages(String sessionId);

    // 检查是否有任务在运行
    public boolean isRunning(String sessionId);
}
```

### 6.2 CallVibeAgentTool（改造）

```java
@Tool("异步启动氛围智能体")
public String callVibeAgent(...) {
    // 1. 启动异步任务
    String taskId = vibeTaskManager.startTask(sessionId, env);

    // 2. 立即返回
    return "已开始编排，任务ID: " + taskId;
}
```

### 6.3 ResetVibeTool（新增）

```java
@Tool("终止当前氛围任务")
public String resetVibe() {
    vibeTaskManager.cancelTask(sessionId);
    return "已终止氛围编排";
}
```

---

## 7. 前端处理

### 7.1 监听 SSE 事件

前端需要监听氛围智能体的实时推送事件：

```typescript
// 监听氛围智能体事件
sseSource.addEventListener('vibe_tool_end', (event) => {
  const { toolName, result } = JSON.parse(event.data)
  applyToolResult(toolName, result)  // 立即应用
})

sseSource.addEventListener('vibe_complete', (event) => {
  const { taskId, plan } = JSON.parse(event.data)
  // 编排完成，可选：显示通知
})
```

### 7.2 思维链显示

氛围智能体的工具调用也显示在思维链中：

```
[MASTER] 好的，正在编排
[VIBE] setLight 开始
[VIBE] setLight 完成 → 灯光已应用
[VIBE] batchPlayMusic 完成 → 音乐已播放
[VIBE] generateNarrative 完成 → TTS 已播报
[VIBE] 编排完成
```

---

## 8. 实现步骤

### 8.1 后端

1. 创建 `VibeTaskManager` 组件
2. 创建 `VibeMessage` 消息类型
3. 改造 `CallVibeAgentTool` 为异步
4. 新增 `ResetVibeTool`
5. 修改 `MasterDialogService` 支持消息队列注入

### 8.2 前端

1. 监听 `vibe_*` SSE 事件
2. 思维链支持显示氛围智能体步骤
3. 实时应用工具结果
