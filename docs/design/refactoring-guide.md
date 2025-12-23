# Vibe Drive 重构指南

基于 LangChain4j 最新 API 的架构重构指南与变更思想

---

## 1. 变更思想 (Why)

### 1.1 核心理念：从"手工作坊"到"工业化生产"

#### 问题诊断

**旧设计的本质问题**：
```
我们在重复造轮子，维护大量"胶水代码"，而这些功能 LangChain4j 已经提供了成熟的解决方案。
```

具体表现：
1. **过度工程化**：自定义了太多抽象层（VibeTool、ToolRegistry、AgentBridge）
2. **重复劳动**：手动实现了框架已经提供的功能（JSON 解析、Tool 调用、会话管理）
3. **维护负担重**：1250+ 行自定义代码需要持续维护和测试
4. **学习曲线陡峭**：新成员需要理解自研框架 + LangChain4j 两套体系

#### 变更哲学

**遵循"框架优先"原则**：
```
Rule 1: 框架能做的，不要自己做
Rule 2: 如果框架不能做，考虑是否真的需要
Rule 3: 确实需要时，先提 Issue 看框架是否计划支持
```

**拥抱"声明式编程"**：
```
从命令式（How）→ 声明式（What）
- 旧：告诉系统"如何"调用 Tool、"如何"解析 JSON
- 新：告诉系统"需要什么"结果，框架自动处理细节
```

**追求"最小惊讶原则"**：
```
代码应该符合框架的惯用法（idiomatic），而不是自创一套规范。
```

---

### 1.2 三大设计原则

#### 原则 1：减少抽象层次

**反模式**：
```java
// ❌ 过度抽象
interface VibeTool { ... }
  ↓ 继承
interface MusicTool extends VibeTool { ... }
  ↓ 实现
class MusicToolImpl implements MusicTool { ... }
  ↓ 注册
toolRegistry.register(musicToolImpl);
```

**最佳实践**：
```java
// ✅ 扁平化
@Component
class MusicTool {
    @Tool("推荐音乐")
    MusicRecommendation recommend(...) { ... }
}
```

**为什么**：
- 减少间接层次，代码更直观
- 减少维护点（3 个文件 → 1 个文件）
- 符合 Spring 和 LangChain4j 的惯用法

---

#### 原则 2：数据与行为分离

**反模式**：
```java
// ❌ 混合关注点
class AgentOutputParser {
    // 数据定义
    record AgentOutput(...) {}

    // 解析逻辑
    AgentOutput parse(String json) { ... }

    // JSON 提取逻辑
    String extractJson(String markdown) { ... }
}
```

**最佳实践**：
```java
// ✅ 数据用 Record + @Description
@Description("...")
record AmbiencePlan(...) {}

// ✅ 行为委托给框架
interface VibeAgent {
    AmbiencePlan analyze(String env);  // 框架自动解析
}
```

**为什么**：
- 数据模型更纯粹，可复用性更高
- 解析逻辑由框架统一处理，避免不一致
- 类型安全，编译时检查

---

#### 原则 3：配置外部化

**反模式**：
```java
// ❌ 硬编码在代码中
class PromptTemplates {
    public static final String SYSTEM_PROMPT = """
        你是 Vibe Drive 的氛围编排智能体...
        """;
}
```

**最佳实践**：
```
✅ 外部资源文件
src/main/resources/prompts/vibe-system.txt
```

```java
// ✅ 引用外部文件
@SystemMessage(fromResource = "prompts/vibe-system.txt")
```

**为什么**：
- Prompt 调整无需重新编译
- 支持 A/B 测试和版本控制
- 团队协作更友好（Prompt 工程师可以独立工作）

---

## 2. 变更对比 (Before vs After)

### 2.1 Tool 层

#### Before（旧设计）

```java
// 1. 定义接口
public interface VibeTool {
    String getName();
    String getDescription();
    boolean isAvailable();
}

// 2. 定义具体 Tool 接口
public interface MusicTool extends VibeTool {
    @Tool(name = "recommend_music")
    MusicRecommendation recommend(String mood, ...);
}

// 3. 实现类
@Component
public class MusicToolImpl implements MusicTool {
    @Override
    public String getName() { return "MusicTool"; }

    @Override
    public String getDescription() { return "..."; }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public MusicRecommendation recommend(String mood, ...) {
        // 实现
    }
}

// 4. 注册
@Component
public class ToolRegistry {
    private Map<String, VibeTool> tools = new ConcurrentHashMap<>();

    public void register(VibeTool tool) {
        tools.put(tool.getName(), tool);
    }
}
```

**问题**：
- 3 层抽象（接口 → 接口 → 实现）
- 需要手动注册
- 大量样板代码（getName、getDescription、isAvailable）

---

#### After（新设计）

```java
// 一个类搞定
@Component
public class MusicTool {

    private final MusicService musicService;

    @Tool("根据用户情绪、时段和乘客数量推荐合适的音乐")
    public MusicRecommendation recommendMusic(
        @P("目标情绪") String mood,
        @P("时段") String timeOfDay,
        @P("乘客数量") int passengerCount,
        @P("偏好流派，可选") String genre
    ) {
        return musicService.recommend(mood, timeOfDay, passengerCount, genre);
    }
}

// 配置中自动注册
@Bean
public VibeAgent vibeAgent(..., MusicTool musicTool, ...) {
    return AiServices.builder(VibeAgent.class)
        .tools(musicTool, ...)  // 直接传入实例
        .build();
}
```

**改进**：
- ✅ 1 层（直接实现）
- ✅ 自动注册（Spring DI + AiServices）
- ✅ 零样板代码

**代码行数**：120 行 → 30 行（减少 75%）

---

### 2.2 Agent 实现

#### Before（旧设计）

```java
public interface VibeAgent {
    AmbiencePlan onEnvironmentChange(Environment env);
}

@Service
public class VibeAgentImpl implements VibeAgent {

    private final Monitor monitor;
    private final Analyzer analyzer;
    private final Planner planner;
    private final Executor executor;
    private final ToolRegistry toolRegistry;

    @Override
    public AmbiencePlan onEnvironmentChange(Environment env) {
        // 1. Monitor：检测变化
        if (!monitor.shouldRespond(env)) {
            return null;
        }

        // 2. Analyze：调用 LLM 理解语义
        String prompt = buildPrompt(env);
        String llmResponse = chatModel.generate(prompt);

        // 3. Plan：解析 LLM 输出
        AgentOutput output = parseOutput(llmResponse);

        // 4. Execute：调用 Tools
        List<ToolResult> toolResults = new ArrayList<>();
        for (ToolCall call : output.toolCalls()) {
            VibeTool tool = toolRegistry.getTool(call.toolName());
            ToolResult result = tool.execute(call.args());
            toolResults.add(result);
        }

        // 5. 整合结果
        return buildAmbiencePlan(output, toolResults);
    }

    private String buildPrompt(Environment env) {
        return PromptTemplates.buildSystemPrompt(...) +
               PromptTemplates.buildUserPrompt(env);
    }

    private AgentOutput parseOutput(String response) {
        String json = extractJson(response);
        return objectMapper.readValue(json, AgentOutput.class);
    }
}
```

**问题**：
- 500+ 行实现代码
- 手动管理 MAPE-K 闭环
- 手动 Prompt 拼接和 JSON 解析
- 手动 Tool 调用和结果整合

---

#### After（新设计）

```java
public interface VibeAgent {

    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据，并编排合适的氛围方案：
        {{environment}}
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson
    );
}

// 配置
@Bean
public VibeAgent vibeAgent(ChatLanguageModel model, ...) {
    return AiServices.builder(VibeAgent.class)
        .chatLanguageModel(model)
        .tools(musicTool, lightTool, narrativeTool)
        .chatMemoryProvider(memoryId ->
            MessageWindowChatMemory.withMaxMessages(20))
        .build();
}

// 使用
@Service
public class VibeService {
    public AnalyzeResponse analyze(Environment env) {
        Result<AmbiencePlan> result = vibeAgent.analyzeEnvironment(
            "user-123",
            objectMapper.writeValueAsString(env)
        );

        return AnalyzeResponse.success(
            result.content(),              // AmbiencePlan
            TokenUsageInfo.from(result.tokenUsage()),
            result.toolExecutions().stream()
                .map(ToolExecutionInfo::from).toList(),
            processingTime
        );
    }
}
```

**改进**：
- ✅ 声明式接口，零实现代码
- ✅ 框架自动处理 MAPE-K 闭环
- ✅ 自动 Prompt 渲染和 JSON 解析
- ✅ 自动 Tool Calling 和结果整合
- ✅ 内置元数据收集（Token、执行时间）

**代码行数**：500 行 → 30 行（减少 94%）

---

### 2.3 Prompt 管理

#### Before（旧设计）

```java
public class PromptTemplates {
    public static final String ROLE = "你是 Vibe Drive...";
    public static final String SAFETY_RULES = "## 安全模式...";

    public static String buildSystemPrompt(List<VibeTool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROLE).append("\n\n");
        sb.append(SAFETY_RULES).append("\n\n");
        sb.append(buildToolsSection(tools));
        return sb.toString();
    }

    public static String buildUserPrompt(Environment env) {
        return String.format("""
            请分析以下环境：
            %s
            """, toJson(env));
    }
}
```

**问题**：
- Prompt 内容硬编码在 Java 代码中
- 修改 Prompt 需要重新编译
- 难以进行 A/B 测试

---

#### After（新设计）

**文件结构**：
```
src/main/resources/prompts/
├── vibe-system.txt       - System Prompt
├── vibe-system-v2.txt    - A/B 测试版本
└── vibe-safety-rules.txt - 安全规则模块
```

**代码**：
```java
public interface VibeAgent {
    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        ## 当前环境
        {{environment}}

        {{#if preferences}}
        ## 用户偏好
        {{preferences}}
        {{/if}}
        """)
    AmbiencePlan analyze(
        @V("environment") String env,
        @V("preferences") String prefs
    );
}
```

**改进**：
- ✅ Prompt 独立于代码
- ✅ 热更新（无需重启）
- ✅ 版本控制和 A/B 测试
- ✅ 支持 Mustache 模板语法

---

### 2.4 结构化输出

#### Before（旧设计）

```java
@Component
public class AgentOutputParser {

    public AgentOutput parse(String llmResponse) {
        try {
            String json = extractJson(llmResponse);
            return objectMapper.readValue(json, AgentOutput.class);
        } catch (Exception e) {
            throw new AgentOutputParseException("...", e);
        }
    }

    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        return response.trim();
    }
}

// 数据模型（无注解）
public record AmbiencePlan(
    String id,
    MusicRecommendation music,
    LightSetting light,
    Narrative narrative,
    SafetyMode safetyMode,
    String reasoning
) {}
```

**问题**：
- 手动提取 JSON（容易出错）
- LLM 不知道字段含义（容易输出错误格式）
- 错误处理繁琐

---

#### After（新设计）

```java
// 数据模型（添加 @Description）
@Description("氛围编排方案，包含音乐、灯光和叙事的完整配置")
public record AmbiencePlan(
    @Description("方案唯一标识符")
    String id,

    @Description("推荐的音乐列表和相关元数据")
    MusicRecommendation music,

    @Description("氛围灯设置，L2专注模式下禁用动态效果，L3静默模式下为null")
    LightSetting light,

    @Description("TTS播报的叙事文本及语音参数")
    Narrative narrative,

    @Description("当前安全模式：L1_NORMAL/L2_FOCUS/L3_SILENT")
    SafetyMode safetyMode,

    @Description("Agent的推理过程，说明为何做出此氛围选择")
    String reasoning
) {}

// AI Service 直接返回 Record
public interface VibeAgent {
    AmbiencePlan analyze(String env);  // 自动解析！
}

// 配置 strictJsonSchema（可选但推荐）
@Bean
public ChatLanguageModel chatModel() {
    return OpenAiChatModel.builder()
        .strictJsonSchema(true)  // 严格 JSON Schema
        .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
        .build();
}
```

**改进**：
- ✅ 零解析代码
- ✅ LLM 理解字段含义（通过 @Description）
- ✅ 类型安全 + 编译时检查
- ✅ 严格模式确保格式正确

---

### 2.5 会话管理

#### Before（旧设计）

```java
public interface ConversationStateManager {
    ConversationState fork(String branchId);
    MergeResult merge(String branchId, MergeStrategy strategy);
    ConversationState current();
}

@Service
public class ConversationStateManagerImpl implements ConversationStateManager {
    private final Map<String, ConversationState> states = new ConcurrentHashMap<>();

    @Override
    public ConversationState fork(String branchId) {
        ConversationState current = getCurrentState();
        ConversationState branch = new ConversationState();
        branch.setMessages(new ArrayList<>(current.getMessages()));
        states.put(branchId, branch);
        return branch;
    }

    @Override
    public MergeResult merge(String branchId, MergeStrategy strategy) {
        ConversationState branch = states.get(branchId);
        ConversationState main = getCurrentState();

        // 检测冲突
        if (hasConflict(branch, main)) {
            return MergeResult.conflict(...);
        }

        // 合并消息
        main.getMessages().addAll(branch.getMessagesSince(...));
        states.remove(branchId);
        return MergeResult.success(...);
    }
}
```

**问题**：
- 300+ 行自研代码
- 复杂的冲突检测和合并逻辑
- 难以持久化

---

#### After（新设计）

```java
// AI Service 自动隔离会话
public interface VibeAgent {
    AmbiencePlan analyze(
        @MemoryId String sessionId,  // 自动隔离！
        @V("environment") String env
    );
}

// 配置持久化（可选）
@Bean
public ChatMemoryStore chatMemoryStore(RedisTemplate redisTemplate) {
    return new RedisChatMemoryStore(redisTemplate);
}

@Bean
public VibeAgent vibeAgent(..., ChatMemoryStore store) {
    return AiServices.builder(VibeAgent.class)
        .chatMemoryProvider(memoryId ->
            MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(store)  // 持久化
                .build())
        .build();
}

// 使用
AmbiencePlan plan1 = vibeAgent.analyze("user-123", env);  // 会话1
AmbiencePlan plan2 = vibeAgent.analyze("user-456", env);  // 会话2（隔离）
```

**改进**：
- ✅ 零自研代码
- ✅ 自动会话隔离（@MemoryId）
- ✅ 内置持久化支持（Redis/数据库）
- ✅ 无需处理冲突（简化为单向）

**代码行数**：300 行 → 0 行（减少 100%）

---

## 3. 变更指南 (How)

### 3.1 实施步骤

#### Phase 1: 数据模型增强（低风险，高收益）

**目标**：为所有 Record 添加 `@Description` 注解

**步骤**：
1. 在 `pom.xml` 中确认依赖：
   ```xml
   <dependency>
       <groupId>dev.langchain4j</groupId>
       <artifactId>langchain4j</artifactId>
       <version>1.0.0-alpha1</version>
   </dependency>
   ```

2. 为每个 Record 添加注解：
   ```java
   // Before
   public record AmbiencePlan(String id, ...) {}

   // After
   @Description("氛围编排方案")
   public record AmbiencePlan(
       @Description("方案唯一标识符") String id,
       ...
   ) {}
   ```

3. 编译验证：
   ```bash
   mvn clean compile
   ```

**验证**：
- [ ] 所有 Record 都有类级别 @Description
- [ ] 所有字段都有 @Description
- [ ] 编译无错误

**预计时间**：1-2 小时

---

#### Phase 2: Tool 层重构（中风险，高收益）

**目标**：移除 VibeTool 接口和 ToolRegistry

**步骤**：
1. 重构 MusicTool：
   ```java
   // 删除 MusicTool 接口
   // 删除 MusicToolImpl 类

   // 创建新的 MusicTool
   @Component
   public class MusicTool {
       private final MusicService musicService;

       @Tool("推荐音乐的详细描述")
       public MusicRecommendation recommendMusic(
           @P("情绪") String mood,
           @P("时段") String timeOfDay,
           @P("乘客数") int passengerCount
       ) {
           return musicService.recommend(mood, timeOfDay, passengerCount);
       }
   }
   ```

2. 同样重构 LightTool 和 NarrativeTool

3. 删除 `ToolRegistry.java` 和 `VibeTool.java`

4. 更新 `VibeAgentConfig`：
   ```java
   @Bean
   public VibeAgent vibeAgent(
           ChatLanguageModel model,
           MusicTool musicTool,
           LightTool lightTool,
           NarrativeTool narrativeTool) {
       return AiServices.builder(VibeAgent.class)
           .chatLanguageModel(model)
           .tools(musicTool, lightTool, narrativeTool)  // 直接注册
           .build();
   }
   ```

**验证**：
- [ ] 删除了 VibeTool 接口
- [ ] 删除了 ToolRegistry 类
- [ ] 所有 Tool 都是 @Component + @Tool 方法
- [ ] 编译无错误
- [ ] 单元测试通过

**预计时间**：2-3 小时

---

#### Phase 3: Prompt 外部化（低风险，中收益）

**目标**：将 Prompt 移到资源文件

**步骤**：
1. 创建目录结构：
   ```bash
   mkdir -p src/main/resources/prompts
   ```

2. 创建 `vibe-system.txt`：
   ```
   你是 Vibe Drive 的氛围编排智能体...

   ## 你的角色
   ...

   ## 安全模式规则
   ...
   ```

3. 更新 AI Service：
   ```java
   public interface VibeAgent {
       @SystemMessage(fromResource = "prompts/vibe-system.txt")
       @UserMessage("...")
       AmbiencePlan analyze(...);
   }
   ```

4. 删除 `PromptTemplates.java`

**验证**：
- [ ] Prompt 文件存在于 resources/prompts/
- [ ] @SystemMessage 正确引用文件
- [ ] 运行时加载成功
- [ ] 删除了 PromptTemplates 类

**预计时间**：1-2 小时

---

#### Phase 4: Agent 接口重构（高风险，高收益）

**目标**：使用 LangChain4j AI Services

**步骤**：
1. 定义新的 AI Service 接口：
   ```java
   public interface VibeAgent {
       @SystemMessage(fromResource = "prompts/vibe-system.txt")
       @UserMessage("""
           请分析以下环境数据：
           {{environment}}
           """)
       Result<AmbiencePlan> analyzeEnvironment(
           @MemoryId String sessionId,
           @V("environment") String environmentJson
       );
   }
   ```

2. 配置 ChatLanguageModel：
   ```java
   @Bean
   public ChatLanguageModel chatModel() {
       return OpenAiChatModel.builder()
           .apiKey(apiKey)
           .modelName("gpt-4o")
           .strictJsonSchema(true)
           .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
           .build();
   }
   ```

3. 构建 AI Service：
   ```java
   @Bean
   public VibeAgent vibeAgent(ChatLanguageModel model, ...) {
       return AiServices.builder(VibeAgent.class)
           .chatLanguageModel(model)
           .tools(musicTool, lightTool, narrativeTool)
           .chatMemoryProvider(memoryId ->
               MessageWindowChatMemory.withMaxMessages(20))
           .build();
   }
   ```

4. 更新 Service 层使用方式：
   ```java
   @Service
   public class VibeService {
       public AnalyzeResponse analyze(Environment env) {
           Result<AmbiencePlan> result = vibeAgent.analyzeEnvironment(
               "session-" + env.userId(),
               objectMapper.writeValueAsString(env)
           );

           return AnalyzeResponse.success(
               result.content(),
               TokenUsageInfo.from(result.tokenUsage()),
               ...
           );
       }
   }
   ```

5. 删除旧的实现：
   - `VibeAgentImpl.java`
   - `AgentOutputParser.java`
   - `ConversationStateManager.java` 及相关类

**验证**：
- [ ] AI Service 接口定义正确
- [ ] 配置正确，能成功构建 Agent
- [ ] 删除了所有手动实现类
- [ ] 集成测试通过
- [ ] Token 使用量正确记录

**预计时间**：4-6 小时

---

#### Phase 5: 元数据增强（低风险，中收益）

**目标**：添加执行元数据支持

**步骤**：
1. 添加元数据模型：
   ```java
   public record TokenUsageInfo(...) {}
   public record ToolExecutionInfo(...) {}
   ```

2. 增强 API 响应：
   ```java
   public record AnalyzeResponse(
       boolean success,
       AmbiencePlan plan,
       TokenUsageInfo tokenUsage,      // 新增
       List<ToolExecutionInfo> toolExecutions,  // 新增
       String error,
       long processingTimeMs
   ) {}
   ```

3. 从 Result<T> 提取元数据：
   ```java
   Result<AmbiencePlan> result = vibeAgent.analyze(...);
   TokenUsageInfo tokenUsage = TokenUsageInfo.from(result.tokenUsage());
   List<ToolExecutionInfo> toolExecs = result.toolExecutions()
       .stream().map(ToolExecutionInfo::from).toList();
   ```

4. 添加日志记录：
   ```java
   logger.info("Token usage: {}, Cost: ${}",
       tokenUsage.totalTokenCount(),
       tokenUsage.estimateCost());
   ```

**验证**：
- [ ] 元数据模型定义完整
- [ ] API 响应包含元数据
- [ ] Token 使用量正确记录
- [ ] 成本估算准确

**预计时间**：2-3 小时

---

### 3.2 迁移检查清单

#### 代码清理

- [ ] 删除 `VibeTool.java` 接口
- [ ] 删除 `ToolRegistry.java` 类
- [ ] 删除 `VibeAgentImpl.java` 实现类
- [ ] 删除 `AgentOutputParser.java` 解析器
- [ ] 删除 `PromptTemplates.java` 模板类
- [ ] 删除 `ConversationStateManager.java` 及相关
- [ ] 删除 `AgentBridge.java` 及相关

#### 新增文件

- [ ] 创建 `src/main/resources/prompts/vibe-system.txt`
- [ ] 创建 `TokenUsageInfo.java`
- [ ] 创建 `ToolExecutionInfo.java`
- [ ] 更新所有 Record 添加 `@Description`

#### 配置更新

- [ ] 升级 LangChain4j 到 1.x
- [ ] 配置 `ChatLanguageModel` 使用 `strictJsonSchema`
- [ ] 配置 `ChatMemoryProvider`
- [ ] 配置 OpenAI API Key

#### 测试验证

- [ ] 单元测试：Tool 层
- [ ] 单元测试：数据模型
- [ ] 集成测试：AI Service
- [ ] 集成测试：完整流程
- [ ] 性能测试：Token 使用量
- [ ] 手动测试：各种环境场景

---

### 3.3 风险控制

#### 高风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Agent 接口变更导致功能失效 | 高 | 1. 保留旧实现并行运行<br>2. 充分的集成测试<br>3. 灰度发布 |
| Tool 调用格式变化 | 中 | 1. Mock 测试验证<br>2. 添加 Tool 执行日志 |
| Prompt 加载失败 | 中 | 1. 添加资源文件检查<br>2. Fallback 机制 |

#### 回滚计划

如果重构后出现问题，按以下顺序回滚：

1. **Phase 5（元数据）**：移除元数据字段，API 响应保持兼容
2. **Phase 4（Agent）**：恢复旧的 VibeAgentImpl 实现
3. **Phase 3（Prompt）**：恢复 PromptTemplates 类
4. **Phase 2（Tool）**：恢复 VibeTool 接口和 ToolRegistry
5. **Phase 1（数据模型）**：移除 @Description 注解（无影响）

**回滚触发条件**：
- 集成测试失败率 > 20%
- 生产环境错误率 > 5%
- Token 使用量异常增长 > 50%

---

## 4. 最佳实践

### 4.1 Tool 设计最佳实践

#### ✅ 推荐

```java
@Component
public class MusicTool {

    @Tool("""
        根据用户情绪、时段和乘客数量推荐合适的音乐。
        - 独自驾驶时推荐个人化音乐
        - 多人乘坐时推荐大众化音乐
        - 深夜时避免激烈音乐
        """)
    public MusicRecommendation recommendMusic(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/.../midnight") String timeOfDay,
        @P("乘客数量: 1-7") int passengerCount,
        @P("偏好流派，可选: pop/rock/jazz/classical") String genre
    ) {
        // 实现
    }
}
```

**要点**：
- 详细的 @Tool 描述（LLM 看得到）
- 清晰的参数说明（@P 中列出可选值）
- 参数类型明确（String、int、enum）

#### ❌ 避免

```java
// ❌ 描述不清晰
@Tool("推荐音乐")
public MusicRecommendation recommend(String a, String b, int c) {}

// ❌ 参数无说明
@Tool("...")
public MusicRecommendation recommend(@P("mood") String mood) {}

// ❌ 使用 Map 作为参数（类型不安全）
@Tool("...")
public MusicRecommendation recommend(Map<String, Object> params) {}
```

---

### 4.2 Prompt 设计最佳实践

#### ✅ 推荐

**结构清晰**：
```
你是 Vibe Drive 的氛围编排智能体。

## 你的角色
...

## 环境数据说明
...

## 安全模式规则
...

## 可用工具
...

## 推理规则
...

## 重要提醒
...
```

**使用模板变量**：
```
{{#if preferences}}
## 用户偏好
{{preferences}}
{{/if}}
```

**版本控制**：
```
prompts/
├── vibe-system.txt       (v1.0)
├── vibe-system-v1.1.txt  (实验版本)
└── vibe-system-backup.txt (备份)
```

#### ❌ 避免

```
// ❌ 全部塞在一个段落
你是 Vibe Drive 的氛围编排智能体，负责根据车载环境数据为驾驶者创造沉浸式体验，你需要理解环境的情感语义，编排合适的音乐灯光和叙事文本...

// ❌ 硬编码变量
请分析用户 123 的环境数据...

// ❌ 没有结构化输出要求
请生成一个氛围方案。
```

---

### 4.3 数据模型最佳实践

#### ✅ 推荐

```java
@Description("氛围编排方案，包含音乐、灯光和叙事的完整配置")
public record AmbiencePlan(
    @Description("方案唯一标识符，格式：plan-{timestamp}-{random}")
    String id,

    @Description("推荐的音乐列表和相关元数据，包含 1-10 首歌曲")
    MusicRecommendation music,

    @Description("氛围灯设置，L2专注模式下禁用动态效果，L3静默模式下为null")
    LightSetting light,

    @Description("TTS播报的叙事文本及语音参数，文本不超过50字")
    Narrative narrative,

    @Description("当前安全模式：L1_NORMAL（车速<60）/L2_FOCUS（60-100）/L3_SILENT（>=100）")
    SafetyMode safetyMode,

    @Description("Agent的推理过程，说明为何做出此氛围选择，帮助用户理解决策依据")
    String reasoning
) {}
```

**要点**：
- 类级别描述概述整体
- 字段级别描述具体含义
- 描述中包含约束条件（范围、格式）
- 描述中说明特殊情况（如 null 的含义）

#### ❌ 避免

```java
// ❌ 无描述
public record AmbiencePlan(String id, ...) {}

// ❌ 描述过于简单
@Description("方案")
public record AmbiencePlan(
    @Description("ID") String id,
    ...
) {}

// ❌ 描述不准确
@Description("音乐") MusicRecommendation music  // 应该说明是"列表"
```

---

### 4.4 会话管理最佳实践

#### ✅ 推荐

```java
// 使用业务 ID 作为 sessionId
String sessionId = "user-" + userId + "-vehicle-" + vehicleId;
Result<AmbiencePlan> result = vibeAgent.analyze(sessionId, env);

// 持久化到 Redis
@Bean
public ChatMemoryStore chatMemoryStore(RedisTemplate<String, List<ChatMessage>> redis) {
    return new RedisChatMemoryStore(redis);
}

// 配置合理的窗口大小
.chatMemoryProvider(memoryId ->
    MessageWindowChatMemory.builder()
        .id(memoryId)
        .maxMessages(20)  // 保留最近 20 条
        .chatMemoryStore(store)
        .build())
```

#### ❌ 避免

```java
// ❌ 所有用户共享一个会话
vibeAgent.analyze("global-session", env);

// ❌ 不持久化（重启丢失）
.chatMemoryProvider(memoryId ->
    MessageWindowChatMemory.withMaxMessages(20))  // 仅内存

// ❌ 窗口过大（浪费 Token）
.maxMessages(1000)

// ❌ 窗口过小（丢失上下文）
.maxMessages(2)
```

---

## 5. FAQ

### Q1: 为什么不保留 VibeTool 接口？

**A**: 三个原因：
1. **多余的抽象**：LangChain4j 已经有 `@Tool` 注解，不需要额外接口
2. **增加维护成本**：每个 Tool 需要 3 个文件（接口 + 接口2 + 实现）
3. **不符合惯用法**：Spring 和 LangChain4j 生态都倾向于"类 + 注解"方式

### Q2: 如果需要 Mock Tool 怎么办？

**A**: 使用 Spring Profile：

```java
@Component
@Profile("prod")
public class MusicTool {
    @Tool("...")
    public MusicRecommendation recommend(...) {
        return musicService.recommend(...);  // 真实实现
    }
}

@Component
@Profile("mock")
public class MockMusicTool {
    @Tool("...")
    public MusicRecommendation recommend(...) {
        return new MusicRecommendation(...);  // Mock 数据
    }
}
```

### Q3: Prompt 文件修改后需要重启吗？

**A**:
- **开发环境**：不需要，Spring Boot DevTools 自动重新加载
- **生产环境**：需要重启，或使用配置中心（如 Nacos）实现热更新

### Q4: @Description 注解对性能有影响吗？

**A**: 几乎没有：
- 注解在编译时处理
- 运行时使用反射读取（缓存后影响忽略不计）
- JSON Schema 生成一次后缓存复用

### Q5: 如果 LLM 输出格式不正确怎么办？

**A**: 三层保障：
1. **@Description** 指导 LLM 输出正确格式
2. **strictJsonSchema** 严格模式确保格式
3. **重试机制**：LangChain4j 内置重试（如果格式错误）

### Q6: 会话管理是否支持多轮对话？

**A**: 支持：
```java
// 第一轮
vibeAgent.analyze("session-123", "深夜雨天高速");

// 第二轮（有上下文）
vibeAgent.analyze("session-123", "现在车速降到60了");
// LLM 会记住之前是"深夜雨天高速"场景
```

### Q7: Token 使用量如何优化？

**A**: 几个建议：
1. **控制会话窗口**：`maxMessages(20)` 而非 100
2. **使用更小的模型**：gpt-4o-mini 而非 gpt-4o（成本降低 90%）
3. **缓存 System Prompt**：启用 Prompt Caching（某些模型支持）
4. **减少 Few-shot 示例**：仅保留关键场景

### Q8: 如何调试 Agent 的决策过程？

**A**: 三种方式：
1. **查看 reasoning 字段**：LLM 自己解释为什么这样决策
2. **Tool 执行日志**：
   ```java
   for (ToolExecution exec : result.toolExecutions()) {
       logger.debug("Tool: {}, Args: {}, Result: {}",
           exec.toolName(), exec.arguments(), exec.result());
   }
   ```
3. **启用 LangChain4j 调试日志**：
   ```yaml
   logging.level.dev.langchain4j: DEBUG
   ```

---

## 6. 参考资料

### 官方文档

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [AI Services 教程](https://docs.langchain4j.dev/tutorials/ai-services)
- [Tools 教程](https://docs.langchain4j.dev/tutorials/tools)
- [Structured Outputs](https://docs.langchain4j.dev/tutorials/structured-outputs)

### 示例项目

- [LangChain4j Examples](https://github.com/langchain4j/langchain4j-examples)
- [Spring Boot + LangChain4j](https://github.com/langchain4j/langchain4j-spring)

### 相关技术

- [Spring Boot 3 文档](https://spring.io/projects/spring-boot)
- [Java 21 新特性](https://openjdk.org/projects/jdk/21/)
- [OpenAI API 文档](https://platform.openai.com/docs/)

---

## 7. 总结

### 7.1 核心价值

这次重构的核心价值在于：

1. **简化架构**：从 1250 行降到 180 行（-85%）
2. **提升可维护性**：声明式 > 命令式
3. **遵循最佳实践**：框架优先，避免重复造轮子
4. **增强可观测性**：Token 监控、性能分析

### 7.2 长期收益

- **降低学习曲线**：新成员只需学习 LangChain4j，无需理解自研框架
- **减少 Bug**：框架经过充分测试，自研代码容易出错
- **快速迭代**：Prompt 调整、Tool 添加都更快
- **技术债务减少**：框架升级自动获得新特性

### 7.3 后续演进

基于新架构，可以轻松扩展：

- **多模型支持**：切换到 Claude、Gemini 只需改配置
- **RAG 集成**：添加知识库检索
- **Multi-Agent**：使用 SupervisorAgent 构建主从架构
- **流式输出**：支持实时响应

---

**变更指南版本**: v1.0
**最后更新**: 2025-12-23
**适用版本**: LangChain4j 1.x + Spring Boot 3.x
