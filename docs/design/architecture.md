# Vibe Drive 系统架构设计

## 1. 架构概述

Vibe Drive 采用 **双智能体异步架构**，基于 MAPE-K 闭环模式实现车载氛围的智能编排。

### 1.1 核心设计理念

- **主从分离**：主智能体负责用户交互，Vibe Agent 专注氛围编排
- **异步非阻塞**：两个智能体异步运行，互不阻塞
- **内置会话隔离**：使用 `@MemoryId` + `ChatMemoryProvider` 管理会话记忆（弃用自研 Fork-Merge）

### 1.2 系统全景图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Vibe Drive 系统                               │
│                                                                         │
│  ┌─────────────────────────┐         ┌─────────────────────────────┐   │
│  │     Master Agent        │         │      Vibe Drive Agent       │   │
│  │     (主智能体)           │         │      (氛围子智能体)          │   │
│  │     [TODO: 后续实现]     │         │      [本次重点]              │   │
│  │                         │         │                             │   │
│  │  ┌───────────────────┐  │  控制   │  ┌───────────────────────┐  │   │
│  │  │ 用户交互处理       │  │ ──────→ │  │ Monitor (环境感知)    │  │   │
│  │  │ - 语音指令        │  │         │  │ - 接收环境 JSON       │  │   │
│  │  │ - 导航/电话       │  │ ←────── │  │ - 变化检测            │  │   │
│  │  │ - 通用问答        │  │  状态   │  └──────────┬────────────┘  │   │
│  │  └───────────────────┘  │         │             │               │   │
│  │                         │         │             ↓               │   │
│  │  ┌───────────────────┐  │         │  ┌───────────────────────┐  │   │
│  │  │ Vibe 控制         │  │         │  │ Analyze + Plan (LLM)  │  │   │
│  │  │ - 启动/停止       │  │         │  │ - 语义理解            │  │   │
│  │  │ - 模式切换        │  │         │  │ - 意图识别            │  │   │
│  │  │ - 状态查询        │  │         │  │ - 方案规划            │  │   │
│  │  └───────────────────┘  │         │  └──────────┬────────────┘  │   │
│  └────────────┬────────────┘         │             │               │   │
│               │                      │             ↓               │   │
│               │                      │  ┌───────────────────────┐  │   │
│               │                      │  │ Execute (Tool 调用)   │  │   │
│               │                      │  │ - MusicTool           │  │   │
│               │                      │  │ - LightTool           │  │   │
│               │                      │  │ - NarrativeTool       │  │   │
│               │                      │  └───────────────────────┘  │   │
│               │                      └─────────────┬───────────────┘   │
│               │                                    │                   │
│               └──────────────┬─────────────────────┘                   │
│                              ↓                                         │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                    Shared Layer (共享层)                          │ │
│  │                                                                   │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │ │
│  │  │ Conversation    │  │ Agent Bridge    │  │ Shared State    │   │ │
│  │  │ State Manager   │  │ (通信桥接)       │  │ (共享状态)       │   │ │
│  │  │ (会话状态管理)   │  │                 │  │                 │   │ │
│  │  │ - fork()        │  │ - notify()      │  │ - ambience      │   │ │
│  │  │ - merge()       │  │ - command()     │  │ - environment   │   │ │
│  │  │ - snapshot()    │  │ - listen()      │  │ - agentStatus   │   │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘   │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

注：上图中的 `Conversation State Manager / Fork-Merge` 为旧设想，已弃用；会话记忆由 LangChain4j 的 `@MemoryId` + `ChatMemoryProvider` 管理。

---

## 2. MAPE-K 闭环架构

Vibe Agent 内部采用 MAPE-K 闭环模式：

```
                    ┌─────────────────────────────────────┐
                    │         Knowledge (知识库)          │
                    │  - 用户偏好                         │
                    │  - 场景规则                         │
                    │  - 历史氛围记录                     │
                    └──────────────────┬──────────────────┘
                                       │
           ┌───────────────────────────┼───────────────────────────┐
           │                           │                           │
           ↓                           ↓                           ↓
    ┌─────────────┐            ┌─────────────┐            ┌─────────────┐
    │   Monitor   │            │   Analyze   │            │    Plan     │
    │   (监控)    │ ─────────→ │   (分析)    │ ─────────→ │   (规划)    │
    │             │            │             │            │             │
    │ 接收环境数据 │            │ LLM 语义理解 │            │ 生成氛围方案 │
    │ 检测变化    │            │ 意图识别    │            │ Tool 选择   │
    └─────────────┘            └─────────────┘            └──────┬──────┘
           ↑                                                     │
           │                                                     ↓
           │                                              ┌─────────────┐
           │                                              │   Execute   │
           │                                              │   (执行)    │
           └──────────────── 反馈循环 ────────────────────│             │
                                                          │ 调用 Tools  │
                                                          │ 输出方案    │
                                                          └─────────────┘
```

### 2.1 各阶段职责

| 阶段 | 职责 | 实现方式 |
|------|------|----------|
| **Monitor** | 接收环境 JSON，检测显著变化 | Java 代码，规则判断 |
| **Analyze** | 语义理解，识别用户意图 | LLM 推理 |
| **Plan** | 生成氛围方案，选择 Tools | LLM 推理（与 Analyze 合并） |
| **Execute** | 调用 Tools，输出最终方案 | Tool Calling |
| **Knowledge** | 提供上下文：用户偏好、历史记录 | 内存/数据库 |

### 2.2 Analyze + Plan 合并

为降低延迟，Analyze 和 Plan 合并为一次 LLM 调用：

```
输入：Environment JSON + Knowledge Context
  ↓
LLM 推理（ReAct 模式）
  ↓
输出：AmbiencePlan（工具调用由框架自动完成）
```

---

## 3. 双智能体通信机制

### 3.1 通信原则

| 方向 | 方式 | 协议 | 说明 |
|------|------|------|------|
| 用户 → 主智能体 | 同步阻塞 | REST | 用户体验优先，立即响应 |
| 主智能体 → Vibe Agent | 异步命令 | REST | 不阻塞用户交互 |
| Vibe Agent → 前端 | 流式推送 | **SSE** | LLM 输出实时展示（TokenStream） |
| 系统 → 前端 | 事件推送 | **SSE** | 状态变化实时通知 |

### 3.2 通讯协议选择：SSE vs WebSocket

**为何选择 SSE 而非 WebSocket**：

| 考量因素 | SSE | WebSocket | 选择理由 |
|----------|-----|-----------|----------|
| **通信模式** | 单向（服务端→客户端） | 双向 | Vibe Drive 只需服务端推送 |
| **LangChain4j 适配** | TokenStream 天然适配 | 需要手动转换 | 减少代码量 |
| **HTTP/2 兼容** | 原生支持多路复用 | 需要额外处理 | 更好的性能 |
| **重连机制** | 浏览器自动重连 | 需手动实现 | 更可靠 |
| **实现复杂度** | 低（基于 HTTP） | 高（独立协议） | 更易维护 |
| **防火墙穿透** | 无障碍（HTTP） | 可能被阻止 | 更好的兼容性 |

**通信架构图**：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Vibe Drive 通信架构                            │
│                                                                         │
│  ┌─────────────┐                              ┌─────────────────────┐   │
│  │   前端 UI   │                              │    Vibe Agent       │   │
│  │             │                              │                     │   │
│  │  ┌───────┐  │  POST /api/vibe/analyze      │  ┌───────────────┐  │   │
│  │  │ 提交  │──┼──────────────────────────────┼─→│ Result<T>     │  │   │
│  │  │ 环境  │  │  (同步 REST)                 │  │ 非流式响应    │  │   │
│  │  └───────┘  │                              │  └───────────────┘  │   │
│  │             │                              │                     │   │
│  │  ┌───────┐  │  GET /api/vibe/analyze/stream│  ┌───────────────┐  │   │
│  │  │ 流式  │←─┼──────────────────────────────┼──│ TokenStream   │  │   │
│  │  │ 展示  │  │  (SSE 流式推送)              │  │ 流式输出      │  │   │
│  │  └───────┘  │                              │  └───────────────┘  │   │
│  │             │                              └─────────────────────┘   │
│  │  ┌───────┐  │  GET /api/vibe/events                                  │
│  │  │ 状态  │←─┼────────────────────────────────────────────────────────│
│  │  │ 监听  │  │  (SSE 事件订阅)                                        │
│  │  └───────┘  │                                                        │
│  └─────────────┘                                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 会话隔离与异步通知（推荐：@MemoryId + ChatMemoryProvider）

LangChain4j 已内置会话隔离与窗口记忆能力，Vibe Drive 不再自研 Fork-Merge：

- **会话隔离键**：以 `sessionId`（建议：`user-{userId}-vehicle-{vehicleId}`）作为 `@MemoryId`，天然隔离多用户/多车辆上下文
- **记忆管理**：使用 `ChatMemoryProvider`（如 `MessageWindowChatMemory`）控制窗口大小，按需接入 `ChatMemoryStore` 做持久化
- **异步通知**：Vibe Agent 的输出作为事件/通知写入主智能体状态（例如追加一条带标签的 SystemMessage 或更新共享状态），避免复制/合并会话历史

### 3.3 并发与冲突策略（简化）

- **用户交互优先**：主智能体正在与用户交互时，Vibe 通知进入队列；交互结束后再追加到会话记忆并播报/展示
- **去重与节流**：相同场景短时间内只保留最新一次氛围建议，避免“打扰式推荐”
- **无需 Merge**：不做分支会话复制与合并，只做“事件追加”和“状态更新”

---

## 4. 安全模式架构

基于车速的三级安全模式：

```
┌─────────────────────────────────────────────────────────────┐
│                     Safety Mode Controller                  │
│                                                             │
│   Speed < 60 km/h     60-100 km/h      ≥ 100 km/h          │
│        │                  │                │                │
│        ↓                  ↓                ↓                │
│   ┌─────────┐       ┌─────────┐       ┌─────────┐          │
│   │   L1    │       │   L2    │       │   L3    │          │
│   │ 正常模式 │       │ 专注模式 │       │ 静默模式 │          │
│   └────┬────┘       └────┬────┘       └────┬────┘          │
│        │                 │                 │                │
│   全功能开放         禁用视觉动效       禁用主动推荐         │
│   - 语音交互 ✓       - 语音交互 ✓       - 仅响应指令        │
│   - 视觉动效 ✓       - 视觉动效 ✗       - TTS 音量 -30%     │
│   - 主动推荐 ✓       - 推荐频率 ↓       - 视觉动效 ✗        │
└─────────────────────────────────────────────────────────────┘
```

### 4.1 安全模式判断位置

采用 **前置过滤 + Agent 约束** 双重保障：

```
环境数据 → [前置过滤器] → Vibe Agent → [后置校验] → 输出
               │                            │
               │ 高速时直接拦截              │ 二次确认安全合规
               │ 主动推荐请求                │
               ↓                            ↓
          快速返回                      过滤违规内容
```

---

## 5. 模块职责边界

### 5.1 模块划分

```
com.vibe/
├── agent/                    # 智能体层
│   ├── master/              # 主智能体 [TODO]
│   │   ├── MasterAgent.java
│   │   └── MockMasterAgent.java
│   └── vibe/                # Vibe 子智能体 [重点]
│       ├── VibeAgent.java          # AI Service 接口
│       ├── VibeAgentConfig.java    # AiServices + Memory 配置
│       └── VibeAgentStatus.java
│
├── tool/                     # Tool 层
│   ├── MusicTool.java
│   ├── LightTool.java
│   └── NarrativeTool.java
│
├── model/                    # 数据模型
│   ├── Environment.java
│   ├── AmbiencePlan.java
│   ├── MusicRecommendation.java
│   └── LightSetting.java
│
├── service/                  # 业务服务层
│   ├── VibeService.java
│   └── SafetyModePolicy.java
│
├── controller/               # API 层
│   └── VibeController.java
│
└── config/                   # 配置
    └── VibeConfig.java
```

### 5.2 依赖关系

```
┌─────────────┐
│ Controller  │ ← HTTP 请求
└──────┬──────┘
       │
       ↓
┌──────────────┐
│   Service    │
└──────┬───────┘
       │
       ↓
┌──────────────┐     ┌──────────────┐
│    Agent     │ ←─→ │    Bridge    │
└──────┬───────┘     └──────────────┘
       │
       ↓
┌──────────────┐
│    Tool      │
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    Model     │
└──────────────┘
```

---

## 6. 核心接口定义

### 6.1 会话状态管理

不再自研 `ConversationStateManager` / Fork-Merge，会话隔离与记忆由 LangChain4j 提供：

- Vibe Agent：`@MemoryId` + `ChatMemoryProvider`（见 6.3 配置示例）
- 需要持久化：接入 `ChatMemoryStore`（内存/Redis/数据库）

### 6.2 主智能体接口（预留）

```java
public interface MasterAgent {

    /**
     * 处理用户请求
     */
    Response handleUserRequest(String input);

    /**
     * 接收 Vibe Agent 通知
     */
    void onVibeNotification(VibeNotification notification);

    /**
     * 是否正在与用户交互
     */
    boolean isInteracting();

    /**
     * 控制 Vibe Agent
     */
    void controlVibeAgent(VibeCommand command);
}
```

### 6.3 Vibe Agent 接口（基于 LangChain4j AI Services）

**重要变更**：使用 LangChain4j AI Services 声明式接口，替代手动实现。

**新设计（推荐）**：

```java
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.MemoryId;

/**
 * Vibe Agent AI Service
 * 使用 LangChain4j 声明式接口，自动处理 Prompt、Tool Calling 和结构化输出
 */
public interface VibeAgent {

    /**
     * 分析环境数据，生成氛围方案
     *
     * @param sessionId 会话 ID，用于隔离不同用户/车辆的会话
     * @param environmentJson 当前环境的 JSON 字符串
     * @param preferences 用户偏好（可选）
     * @return Result 包含 AmbiencePlan 和执行元数据（Token 使用量、Tool 执行详情）
     */
    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据，并编排合适的氛围方案：

        ## 当前环境
        ```json
        {{environment}}
        ```

        {{#if preferences}}
        ## 用户偏好
        {{preferences}}
        {{/if}}

        请根据环境数据和安全模式规则，输出氛围编排方案。
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferences
    );

    /**
     * 快速分析（不保留会话历史）
     */
    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据：
        {{environment}}
        """)
    AmbiencePlan analyzeEnvironmentQuick(
        @V("environment") String environmentJson
    );
}
```

**配置与构建**：

```java
@Configuration
public class VibeAgentConfig {

    @Bean
    public ChatLanguageModel chatModel(@Value("${openai.api.key}") String apiKey) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o")
            .strictJsonSchema(true)  // 严格 JSON Schema
            .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
            .temperature(0.7)
            .build();
    }

    @Bean
    public VibeAgent vibeAgent(
            ChatLanguageModel chatModel,
            MusicTool musicTool,
            LightTool lightTool,
            NarrativeTool narrativeTool,
            ChatMemoryStore chatMemoryStore) {

        return AiServices.builder(VibeAgent.class)
            .chatLanguageModel(chatModel)
            .tools(musicTool, lightTool, narrativeTool)
            .chatMemoryProvider(memoryId ->
                MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20)
                    .chatMemoryStore(chatMemoryStore)  // 可选：持久化
                    .build())
            .build();
    }

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        // 使用内存存储（开发阶段）
        return new InMemoryChatMemoryStore();

        // 生产环境可以使用 Redis
        // return new RedisChatMemoryStore(redisTemplate);
    }
}
```

**优势**：
- ✅ **声明式接口**：无需手动实现 Agent 逻辑
- ✅ **自动 Tool Calling**：LangChain4j 自动处理工具调用
- ✅ **内置会话管理**：使用 `@MemoryId` 自动隔离会话
- ✅ **结构化输出**：自动解析为 `AmbiencePlan` Record
- ✅ **执行元数据**：使用 `Result<T>` 获取 Token 使用量和性能数据

---

**旧设计（已不推荐）**：

```java
/**
 * Vibe Agent 接口（旧设计，已不推荐）
 * ❌ 需要手动实现 MAPE-K 闭环和 Tool 调用逻辑
 */
@Deprecated
public interface VibeAgentOld {

    /**
     * 启动 Agent
     */
    void start();

    /**
     * 停止 Agent
     */
    void stop();

    /**
     * 处理环境变化，返回氛围方案
     */
    AmbiencePlan onEnvironmentChange(Environment env);

    /**
     * 获取当前状态
     */
    VibeAgentStatus getStatus();

    /**
     * 设置安全模式
     */
    void setSafetyMode(SafetyMode mode);
}
```

**为什么废弃**：
- ❌ 需要手动实现 `VibeAgentImpl` 类，管理 MAPE-K 闭环
- ❌ 需要手动调用 LLM 和 Tools
- ❌ 需要手动解析 JSON 输出
- ❌ 缺少内置的会话管理和元数据收集

**推荐替代方案**：使用上述基于 LangChain4j AI Services 的新设计

### 6.4 智能体通信桥接

```java
public interface AgentBridge {

    /**
     * Vibe Agent → Master Agent（异步通知）
     */
    void notifyMaster(VibeNotification notification);

    /**
     * Master Agent → Vibe Agent（命令）
     */
    void sendCommand(VibeCommand command);

    /**
     * 注册事件监听器
     */
    void registerListener(AgentEventListener listener);

    /**
     * 获取共享状态
     */
    SharedState getSharedState();
}
```

### 6.5 SSE 事件发布器

用于向订阅的客户端推送实时事件，替代 WebSocket 实现服务端推送。

```java
/**
 * SSE 事件发布器接口
 * 用于向订阅的客户端推送实时事件
 */
public interface SseEventPublisher<T> {

    /**
     * 订阅事件流
     * @param sessionId 会话 ID
     * @return 事件流（Reactor Flux）
     */
    Flux<T> subscribe(String sessionId);

    /**
     * 发布事件到指定会话
     * @param sessionId 会话 ID
     * @param event 事件数据
     */
    void publish(String sessionId, T event);

    /**
     * 广播事件到所有订阅者
     * @param event 事件数据
     */
    void broadcast(T event);
}

/**
 * 氛围变化事件发布器实现
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

    /**
     * 清理断开连接的订阅者
     */
    public void cleanup(String sessionId) {
        Sinks.Many<AmbienceChangedEvent> sink = sinks.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
```

---

## 7. 数据流图

### 7.1 环境数据 → 氛围方案

```
┌──────────────┐
│ Environment  │  (GPS_Tag, Weather, Speed, Mood, ...)
│    JSON      │
└──────┬───────┘
       │
       ↓
┌──────────────┐
│   Monitor    │  检测变化：是否需要响应？
└──────┬───────┘
       │ 需要响应
       ↓
┌──────────────┐
│   Analyze    │  LLM 语义理解
│   + Plan     │  "深夜+雨天+疲劳" → "需要放松舒缓"
└──────┬───────┘
       │
       ↓
┌──────────────┐
│   Execute    │  Tool Calling
└──────┬───────┘
       │
       ├────────────────┬────────────────┐
       ↓                ↓                ↓
┌────────────┐  ┌────────────┐  ┌────────────┐
│ MusicTool  │  │ LightTool  │  │ Narrative  │
│            │  │            │  │   Tool     │
│ 返回歌曲列表│  │ 返回灯光设置│  │ 返回 TTS 文本│
└─────┬──────┘  └─────┬──────┘  └─────┬──────┘
      │               │               │
      └───────────────┼───────────────┘
                      ↓
              ┌──────────────┐
              │ AmbiencePlan │  最终氛围方案
              │              │
              │ - songs[]    │
              │ - light{}    │
              │ - narrative  │
              └──────────────┘
```

---

## 8. 技术选型确认

| 组件 | 技术选型 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 3.x + **WebFlux** | Java 21，支持响应式 SSE |
| AI 框架 | LangChain4j 1.x | AI Services + Tool Calling + **TokenStream** |
| 会话管理 | ✅ **LangChain4j ChatMemoryProvider** | 使用 `@MemoryId` 自动隔离会话 |
| ~~会话管理~~ | ~~自研 Fork-Merge~~ | ~~已废弃，使用内置方案~~ |
| 结构化输出 | ✅ **@Description 注解** | 自动生成 JSON Schema |
| Prompt 管理 | ✅ **@SystemMessage(fromResource)** | 从资源文件加载 |
| 执行元数据 | ✅ **Result<T>** | 非流式：Token 使用量 + Tool 执行详情 |
| 流式输出 | ✅ **TokenStream + SSE** | 流式：逐 token 推送 + 事件通知 |
| 实时推送 | ✅ **SSE (Server-Sent Events)** | 替代 WebSocket，更简单可靠 |
| ~~实时推送~~ | ~~WebSocket~~ | ~~已废弃，使用 SSE~~ |
| 异步处理 | Virtual Threads + Reactor | Java 21 特性 + Spring WebFlux |
| 前端框架 | React + TypeScript | - |
| 3D 渲染 | Three.js | 氛围灯动效 |
| API 文档 | SpringDoc OpenAPI | Swagger UI |

---

## 9. 基于 LangChain4j 最新 API 的架构简化

### 9.1 核心变更总结

| 领域 | 旧设计 | 新设计 | 收益 |
|------|--------|--------|------|
| **Tool 接口** | 自定义 `VibeTool` 接口 + `ToolRegistry` | 直接使用 `@Tool` 注解的类 | 减少 ~200 行代码 |
| **Agent 实现** | 手动实现 `VibeAgentImpl` + MAPE-K | 声明式 `interface VibeAgent` | 减少 ~500 行代码 |
| **Prompt 管理** | 手动拼接字符串 | `@SystemMessage(fromResource)` | 易于维护和 A/B 测试 |
| **输出解析** | 手动 JSON 解析器 | `@Description` 自动解析 | 类型安全 + 减少错误 |
| **会话管理** | 自研 Fork-Merge | `@MemoryId` + `ChatMemoryProvider` | 简化实现 + 支持持久化 |
| **元数据收集** | 无 | `Result<T>` | Token 监控 + 性能分析 |

### 9.2 代码量对比

**旧架构（预估）**：
- `VibeTool.java` + `ToolRegistry.java`: ~200 行
- `VibeAgentImpl.java`: ~500 行
- `PromptTemplates.java`: ~150 行
- `AgentOutputParser.java`: ~100 行
- `ConversationStateManager.java` + `Fork-Merge`: ~300 行
- **总计**: ~1250 行自定义代码

**新架构（实际）**：
- `VibeAgent.java` (AI Service 接口): ~30 行
- `VibeAgentConfig.java` (配置): ~50 行
- `MusicTool.java` / `LightTool.java` / `NarrativeTool.java`: ~100 行（精简）
- **总计**: ~180 行代码

**代码减少**: ~85% ✅

### 9.3 架构对比图

**旧架构（复杂）**：
```
┌──────────────────────────────────────────────────────────────┐
│                       自定义层（需要维护）                    │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │  ToolRegistry  │  │VibeAgentImpl   │  │ PromptBuilder  │ │
│  │  (手动管理)    │  │ (手动实现MAPE) │  │ (字符串拼接)   │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ OutputParser   │  │ Fork-Merge     │  │ AgentBridge    │ │
│  │ (手动解析JSON) │  │ (会话管理)     │  │ (通信机制)     │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
└──────────────────────────────────────────────────────────────┘
           ↓ 调用
┌──────────────────────────────────────────────────────────────┐
│                      LangChain4j 框架                         │
└──────────────────────────────────────────────────────────────┘
```

**新架构（简化）**：
```
┌──────────────────────────────────────────────────────────────┐
│                   应用层（声明式）                            │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ VibeAgent      │  │ MusicTool      │  │ data-models    │ │
│  │ (AI Service)   │  │ (@Tool)        │  │ (@Description) │ │
│  │ - @SystemMsg   │  │ - @Tool方法    │  │ - Record类     │ │
│  │ - @UserMsg     │  │                │  │                │ │
│  └────────────────┘  └────────────────┘  └────────────────┘ │
└──────────────────────────────────────────────────────────────┘
           ↓ 自动处理
┌──────────────────────────────────────────────────────────────┐
│                   LangChain4j 框架（全自动）                  │
│  ✅ 自动 Tool Calling   ✅ 自动 JSON 解析                      │
│  ✅ 自动会话管理        ✅ 自动元数据收集                      │
│  ✅ 自动 Prompt 渲染    ✅ 自动重试和错误处理                  │
└──────────────────────────────────────────────────────────────┘
```

### 9.4 开发工作流简化

**旧流程**：
1. 定义 Tool 接口 → 实现 Tool → 注册到 ToolRegistry
2. 实现 VibeAgentImpl → 管理 MAPE-K 闭环 → 手动调用 Tools
3. 手动拼接 Prompt → 调用 LLM → 手动解析 JSON
4. 实现 Fork-Merge → 管理会话分支 → 处理冲突

**新流程**：
1. 定义 Tool 类，添加 `@Tool` 方法 ✅
2. 定义 AI Service 接口，添加 `@SystemMessage` 和 `@UserMessage` ✅
3. 配置 `AiServices.builder()` 注册 Tools ✅
4. 调用 `vibeAgent.analyzeEnvironment(sessionId, data)` - **完成！** ✅

### 9.5 维护性对比

| 维护任务 | 旧方式 | 新方式 |
|---------|--------|--------|
| 添加新 Tool | 定义接口 + 实现类 + 注册 | 添加 `@Tool` 方法 |
| 修改 Prompt | 修改 Java 字符串常量 | 编辑资源文件 `.txt` |
| 调整输出格式 | 修改解析器 + Schema | 修改 Record + `@Description` |
| A/B 测试 Prompt | 代码分支 | 切换资源文件 |
| 会话隔离 | 手动管理 `sessionId` | 自动使用 `@MemoryId` |
| 监控 Token 使用 | 手动记录 | `result.tokenUsage()` |

---

## 10. 待确认事项

- [ ] ~~自研 Fork-Merge 持久化策略~~ → ✅ 已简化为使用 `@MemoryId`
- [ ] 主智能体的具体实现方案（后续阶段，可使用 SupervisorAgent）
- [ ] ChatMemoryStore 的存储方案（内存 vs Redis，已有内置实现）
- [x] ~~Tool 层设计~~ → ✅ 已简化为直接使用 `@Tool` 注解
- [x] ~~输出解析器~~ → ✅ 已简化为使用 `@Description` 自动解析
- [ ] 多用户/多车辆的隔离策略：使用 `@MemoryId` 已解决 ✅
