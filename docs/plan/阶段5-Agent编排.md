# 阶段 5: Agent 编排

## 状态

🟢 已完成

## 目标

使用 LangChain4j AI Services 配置 Vibe Agent，实现环境感知 → 推理 → 工具调用的完整闭环。

**重要变更**：参考 IC-Coder 项目的递归编排架构，实现了简化版本的编排层。

## 前置依赖

- [x] 阶段 2: 设计文档（含重构）
- [x] 阶段 3: 数据模型实现（需要 Environment 和 AmbiencePlan）
- [x] 阶段 4: Tool 层实现（需要 MusicTool、LightTool、NarrativeTool）
- [x] 参考：IC-Coder 项目的递归编排架构

## 架构设计

### 参考 IC-Coder 架构

```
IccoderDialogService (递归核心)
├── executeTurn() 递归调用
├── TokenStream 流式响应
├── hasToolCall 检测 → 递归继续
└── 终止条件：无工具调用 或 达到最大深度
```

### Vibe Drive 简化点

| IC-Coder 功能 | Vibe Drive 处理 |
|--------------|----------------|
| 客户端工具 | ❌ 移除（所有工具在服务端）|
| 用户交互工具 | ❌ 移除（驾驶中不适合）|
| 上下文压缩 | ❌ 暂不实现（Vibe 对话较短）|
| 知识图谱 | ❌ 移除 |
| 安全模式过滤 | ✅ 新增（L1/L2/L3）|

### 设计决策

- **VibeLoopState**：使用 Record（不可变），每次状态变化返回新实例
- **上下文压缩**：暂不实现，Vibe 对话通常较短（1-3轮递归）
- **LangChain4j 版本**：升级到 1.9.1（与 IC-Coder 一致）

---

## 任务清单

### Phase 1: 编排层 DTO ✅

- [x] 创建 `VibeLoopState.java` - 循环状态（不可变 Record）
- [x] 创建 `VibeDialogRequest.java` - 对话请求
- [x] 创建 `VibeDialogResult.java` - 对话结果

### Phase 2: 流式回调接口 ✅

- [x] 创建 `VibeStreamCallback.java` - 回调接口
- [x] 创建 `SseVibeCallback.java` - SSE 实现

### Phase 3: Agent 工厂 ✅

- [x] 创建 `VibeAgent.java` - AI Service 接口
- [x] 创建 `VibeAgentFactory.java` - Agent 工厂
- [x] 创建 `PromptAssembler.java` - Prompt 组装器

### Phase 4: 编排服务核心 ✅

- [x] 创建 `VibeDialogService.java` - 递归编排核心
  - 递归调用 `executeTurn()`
  - TokenStream 流式响应处理
  - 工具调用检测与递归继续
  - 安全模式前置过滤

### Phase 5: 安全模式过滤 ✅

- [x] 创建 `SafetyModeFilter.java` - 后置过滤器
  - L1 正常模式：返回原方案
  - L2 专注模式：灯光切换为静态
  - L3 静默模式：返回静默方案

### Phase 6: Prompt 资源文件 ✅

- [x] 创建 `prompts/vibe-system.txt` - System Prompt
  - Agent 角色定义
  - 环境数据字段说明
  - 安全模式规则
  - 推理规则
  - 输出要求

### Phase 7: 配置类 ✅

- [x] 创建 `VibeAgentConfig.java` - Agent 配置
  - StreamingChatModel Bean
  - ChatMemoryStore Bean
- [x] 升级 LangChain4j 版本到 1.9.1

### Phase 8: 单元测试 ✅

- [x] 创建 `VibeLoopStateTest.java` - 5 个测试
- [x] 创建 `SafetyModeFilterTest.java` - 4 个测试

---

## 相关文件

```
src/main/java/com/vibe/
├── orchestration/                   # 编排层（新增）
│   ├── dto/
│   │   ├── VibeLoopState.java      # 循环状态
│   │   ├── VibeDialogRequest.java  # 对话请求
│   │   └── VibeDialogResult.java   # 对话结果
│   ├── callback/
│   │   ├── VibeStreamCallback.java # 回调接口
│   │   └── SseVibeCallback.java    # SSE 实现
│   └── service/
│       ├── VibeDialogService.java  # 递归编排核心
│       └── SafetyModeFilter.java   # 安全模式过滤
├── agent/
│   ├── VibeAgent.java              # AI Service 接口
│   ├── VibeAgentFactory.java       # Agent 工厂
│   └── PromptAssembler.java        # Prompt 组装器
└── config/
    └── VibeAgentConfig.java        # Agent 配置

src/main/resources/prompts/
└── vibe-system.txt                 # System Prompt

src/test/java/com/vibe/orchestration/
├── dto/
│   └── VibeLoopStateTest.java
└── service/
    └── SafetyModeFilterTest.java
```

## 完成标准

- [x] 编排层 DTO 创建完成
- [x] 流式回调接口实现
- [x] Agent 工厂可正确创建 Agent
- [x] 递归编排逻辑正确（工具调用 → 递归，无调用 → 结束）
- [x] 安全模式过滤正确生效
- [x] 单元测试通过（9 个新测试）
- [x] LangChain4j 升级到 1.9.1
- [ ] 代码已提交到 Git

## 技术要点

### 递归编排流程

```
executeDialog()
    │
    ├─ 计算安全模式
    ├─ L3 静默模式前置过滤
    ├─ 初始化 VibeLoopState
    └─ executeTurn(depth=0)
         │
         ├─ 深度检查
         ├─ 创建 Agent
         ├─ 调用 TokenStream
         │
         ├─ 流式处理：
         │  ├─ onPartialResponse → callback.onTextDelta()
         │  ├─ beforeToolExecution → hasToolCall = true
         │  └─ onCompleteResponse → 保存响应
         │
         └─ 递归判断：
            ├─ hasToolCall && 无最终文本 → executeTurn(depth+1)
            └─ 其他情况 → 组装 AmbiencePlan + 安全过滤 → onComplete()
```

### LangChain4j 1.9.1 API 变更

| 旧 API | 新 API |
|--------|--------|
| `StreamingChatLanguageModel` | `StreamingChatModel` |
| `.streamingChatLanguageModel()` | `.streamingChatModel()` |

## 问题与笔记

### 2025-12-23: 编排与 SSE 对齐

- 递归继续条件改为 `hasToolCall && !hasFinalText`，避免工具调用后已产生最终文本仍继续递归
- `SseVibeCallback` 事件类型对齐数据模型：`token` / `tool_start` / `tool_end` / `complete` / `error`
- `complete` 事件 payload 复用 `AnalyzeResponse`，与同步接口保持同一份 DTO
- `AmbiencePlan` 由 Tool 返回结果解析组装（移除 `buildAmbiencePlan()` 的 TODO）
- Agent 配置读取 `application.yml` 的 `langchain4j.open-ai.chat-model.*`，避免重复配置键

### 版本兼容性

- LangChain4j 从 1.0.0-alpha1 升级到 1.9.1
- 移除了 `langchain4j-spring-boot-starter` 依赖（1.9.1 版本不存在）
- API 类名变更：`StreamingChatLanguageModel` → `StreamingChatModel`

### 后续优化

1. 集成测试（需要 OpenAI API Key）
2. 上下文压缩（如果对话变长）
3. Token 使用量监控
4. 多轮对话测试
