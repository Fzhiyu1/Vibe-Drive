# 主智能体设计文档

## 1. 概述

主智能体（Master Agent）是 Vibe Drive 的用户交互入口，负责对话理解、任务分发和直接控制。

### 1.1 定位

- **演示用途**：用于展示系统完整能力
- **双重身份**：既是车机助手，又了解项目本身
- **全能控制**：拥有所有工具，可直接干预

### 1.2 与氛围智能体的关系

```
┌─────────────────────────────────────┐
│         主智能体 (Master Agent)      │
│  - 对话理解                          │
│  - 意图识别                          │
│  - 直接控制                          │
└──────────────┬──────────────────────┘
               │ 异步调用
               ▼
┌─────────────────────────────────────┐
│         氛围智能体 (Vibe Agent)      │
│  - 自动氛围编排                      │
│  - 环境感知                          │
└─────────────────────────────────────┘
```

---

## 2. 工具设计

### 2.1 工具清单

```
主智能体工具：
├── 继承氛围智能体工具（可直接干预）
│   ├── MusicTool (搜索/播放)
│   ├── MusicSeedTool (音乐种子)
│   ├── LightTool (灯光控制)
│   ├── ScentTool (香氛控制)
│   └── MassageTool (按摩控制)
│
├── 语音交互工具
│   └── SayTool (语音输出)
│
└── 管理工具
    ├── CallVibeAgentTool (调用氛围智能体)
    ├── GetEnvironmentTool (获取环境)
    ├── SetEnvironmentTool (模拟环境)
    └── GetProjectIntroTool (项目介绍)
```

### 2.2 SayTool（语音输出）

**问题**：ReAct 模式下，AI 输出很多中间内容，如果都转语音会很吵。

**解决方案**：只有调用 `say` 工具时才转语音。

```java
@Component
public class SayTool {

    @Tool("对用户说话，会转为语音播放。只在需要回复用户时调用。")
    public String say(@P("要说的内容") String text) {
        return text; // 前端收到后调用 TTS
    }
}
```

**System Prompt 要求**：
```
重要：你必须使用 say 工具来回复用户，不要直接输出文本。
只有 say 工具的内容会被转为语音播放。
```

### 2.3 CallVibeAgentTool（调用氛围智能体）

**异步调用**：不阻塞主智能体，结果进入消息队列。

```java
@Component
public class CallVibeAgentTool {

    @Tool("异步调用氛围智能体进行自动编排。结果会通过消息队列返回。")
    public String callVibeAgent(
        @P("环境数据，由AI根据用户描述生成") Environment environment
    ) {
        // 异步执行
        vibeAgentExecutor.submitAsync(environment);
        return "已开始氛围编排，请稍候...";
    }
}
```

**流程**：
```
1. 主智能体调用 callVibeAgent(env)
2. 异步提交任务，立即返回
3. 氛围智能体后台执行
4. 完成后结果进入消息队列
5. 主智能体空闲时处理结果
```

---

## 3. 能力边界

| 能力 | 可以 | 不可以 |
|------|------|--------|
| 氛围控制 | ✅ 调用工具 | |
| 闲聊 | ✅ 聊天 | |
| 技术架构 | ✅ 解释项目 | |
| 文本操作 | | ❌ 写文档/代码 |
| 文件操作 | | ❌ 读写文件 |
| 外部请求 | | ❌ 联网/API |

**原则**：只能"说"和"控制工具"，不能"写"。

---

## 4. 对话交互

### 4.1 语音输入

```
用户按住按钮 → 说话 → 松开 → Whisper 转文本 → 发给主智能体
```

- Whisper Docker 服务：端口 9000
- 模型：medium

### 4.2 用户打断处理

**问题**：ReAct 执行时间长，用户可能在中间说话。

**解决方案**：Spring AOP 拦截工具调用

```java
@Aspect
@Component
public class ToolInterceptor {

    @Autowired
    private MessageQueue messageQueue;

    @Around("@annotation(dev.langchain4j.agent.tool.Tool)")
    public Object checkBeforeToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        if (messageQueue.hasUserMessage()) {
            throw new UserInterruptException("用户有新消息");
        }
        return joinPoint.proceed();
    }
}
```

**完整处理流程**：
```
1. 用户消息进入队列
2. AOP 检测到新消息，抛出 UserInterruptException
3. 上层捕获异常，保存当前任务状态
4. 从队列取出用户消息，优先处理
5. 用户消息处理完成后，恢复之前的任务
```

---

## 5. 演示场景

| 场景 | 用户说 | 主智能体行为 |
|------|--------|-------------|
| 编排环境 | "模拟夜间高速" | 调用 setEnvironment |
| 编排氛围 | "帮我放松一下" | 调用 callVibeAgent |
| 直接控制 | "灯光调暗" | 调用 LightTool |
| 项目介绍 | "这个项目怎么实现的" | 调用 getProjectIntro |

---

## 6. 待实现

- [ ] MasterAgentFactory
- [ ] SayTool
- [ ] CallVibeAgentTool
- [ ] GetEnvironmentTool / SetEnvironmentTool
- [ ] GetProjectIntroTool
- [ ] ToolInterceptor (AOP)
- [ ] MessageQueue
- [ ] 前端对话界面
