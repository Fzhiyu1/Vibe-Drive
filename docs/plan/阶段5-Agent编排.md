# 阶段 5: Agent 编排

## 状态

⚪ 待开始

## 目标

使用 LangChain4j AI Services 配置 Vibe Agent，实现环境感知 → 推理 → 工具调用的完整闭环。

**重要**：基于重构后的设计，使用声明式 AI Service 接口，框架自动处理 MAPE-K 闭环。

## 前置依赖

- [x] 阶段 2: 设计文档（含重构）
- [ ] 阶段 3: 数据模型实现（需要 Environment 和 AmbiencePlan）
- [ ] 阶段 4: Tool 层实现（需要 MusicTool、LightTool、NarrativeTool）
- [x] 参考：`docs/design/refactoring-guide.md` Phase 3, 4

## 任务清单

### Phase 1: Prompt 资源文件（2 小时）

- [ ] 创建目录 `src/main/resources/prompts/`
- [ ] 创建 `vibe-system.txt` System Prompt 文件
  - Agent 角色定义：氛围编排智能体
  - 环境数据说明（字段含义）
  - 安全模式规则（L1/L2/L3）
  - 可用工具说明（自动生成，但可描述使用场景）
  - 推理规则（语义理解优先、乘客数量影响等）
  - 输出格式要求（JSON Schema）
  - 重要提醒（安全第一、简洁温馨）
- [ ] 验证文件编码为 UTF-8
- [ ] 提交到 Git

**参考**：`docs/design/prompt-design.md` Section 2.3

---

### Phase 2: AI Service 接口定义（1 小时）

- [ ] 创建 `VibeAgent.java` 接口
  ```java
  public interface VibeAgent {
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
  }
  ```
- [ ] 添加快速分析方法（无会话历史）
  ```java
  AmbiencePlan analyzeEnvironmentQuick(
      @V("environment") String environmentJson
  );
  ```

**参考**：`docs/design/architecture.md` Section 6.3

---

### Phase 3: Agent 配置（2-3 小时）

- [ ] 创建 `VibeAgentConfig.java` 配置类
- [ ] 配置 ChatLanguageModel Bean：
  ```java
  @Bean
  public ChatLanguageModel chatModel(@Value("${openai.api.key}") String apiKey) {
      return OpenAiChatModel.builder()
          .apiKey(apiKey)
          .modelName("gpt-4o")
          .strictJsonSchema(true)
          .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
          .temperature(0.7)
          .build();
  }
  ```
- [ ] 配置 ChatMemoryStore Bean（开发用内存，生产用 Redis）：
  ```java
  @Bean
  public ChatMemoryStore chatMemoryStore() {
      return new InMemoryChatMemoryStore();
      // 生产：return new RedisChatMemoryStore(redisTemplate);
  }
  ```
- [ ] 构建 VibeAgent Bean：
  ```java
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
                  .chatMemoryStore(chatMemoryStore)
                  .build())
          .build();
  }
  ```
- [ ] 配置 `application.yml`：
  ```yaml
  openai:
    api:
      key: ${OPENAI_API_KEY}

  logging:
    level:
      dev.langchain4j: DEBUG  # 调试时启用
  ```

**参考**：`docs/design/architecture.md` Section 6.3

---

### Phase 4: Service 层集成（1-2 小时）

- [ ] 创建 `VibeService.java`
- [ ] 实现 analyze 方法：
  ```java
  @Service
  public class VibeService {
      private final VibeAgent vibeAgent;
      private final ObjectMapper objectMapper;

      public AnalyzeResponse analyze(Environment environment) {
          long startTime = System.currentTimeMillis();

          // 调用 Agent
          Result<AmbiencePlan> result = vibeAgent.analyzeEnvironment(
              generateSessionId(environment),
              objectMapper.writeValueAsString(environment),
              null  // preferences 暂时为 null
          );

          // 提取元数据
          AmbiencePlan plan = result.content();
          TokenUsage tokenUsage = result.tokenUsage();
          List<ToolExecution> toolExecutions = result.toolExecutions();

          // 记录日志
          logExecutionMetadata(tokenUsage, toolExecutions);

          // 构建响应
          long processingTime = System.currentTimeMillis() - startTime;
          return AnalyzeResponse.success(
              plan,
              TokenUsageInfo.from(tokenUsage),
              toolExecutions.stream()
                  .map(ToolExecutionInfo::from).toList(),
              processingTime
          );
      }
  }
  ```
- [ ] 实现会话 ID 生成逻辑
- [ ] 实现日志记录（Token 使用、Tool 执行时间）

---

### Phase 5: ~~MAPE-K 闭环实现~~（已简化）

- [x] ~~Monitor：接收环境数据~~ → **框架自动处理**
- [x] ~~Analyze：LLM 分析环境语义~~ → **框架自动处理**
- [x] ~~Plan：决定调用哪些工具~~ → **框架自动处理**
- [x] ~~Execute：执行工具调用~~ → **框架自动处理**
- [x] ~~Knowledge：维护上下文记忆~~ → **ChatMemoryProvider 自动管理**

**说明**：使用 AI Services 后，MAPE-K 闭环由 LangChain4j 框架自动处理，无需手动实现。

---

### Phase 6: 安全模式实现（2 小时）

- [ ] 在 Service 层实现前置过滤：
  ```java
  public AnalyzeResponse analyze(Environment environment) {
      SafetyMode safetyMode = SafetyMode.fromSpeed(environment.speed());

      // L3 静默模式：不主动推荐
      if (safetyMode == SafetyMode.L3_SILENT && !isUserInitiated()) {
          return AnalyzeResponse.noAction("高速行驶中，静默模式");
      }

      // 正常调用 Agent
      Result<AmbiencePlan> result = vibeAgent.analyze(...);

      // 后置过滤（L2/L3 禁用灯光动效）
      AmbiencePlan filteredPlan = applySafetyFilter(result.content(), safetyMode);

      return AnalyzeResponse.success(filteredPlan, ...);
  }
  ```
- [ ] 实现后置过滤逻辑（禁用灯光、降低音量）
- [ ] 在 Prompt 中强调安全规则

---

### Phase 7: 测试与调试（3-4 小时）

- [ ] 编写集成测试：
  ```java
  @SpringBootTest
  class VibeAgentIntegrationTest {
      @Test
      void testMidnightRainyHighway() {
          Environment env = new Environment(
              GpsTag.HIGHWAY,
              Weather.RAINY,
              85.0,
              UserMood.TIRED,
              TimeOfDay.MIDNIGHT,
              1,
              RouteType.HIGHWAY,
              Instant.now()
          );

          Result<AmbiencePlan> result = vibeAgent.analyze(...);

          assertThat(result.content().safetyMode()).isEqualTo(SafetyMode.L2_FOCUS);
          assertThat(result.content().music()).isNotNull();
          assertThat(result.content().reasoning()).contains("疲劳");
      }
  }
  ```
- [ ] 测试 Few-shot 示例场景（深夜雨天、家庭出游、高速静默）
- [ ] 测试安全模式切换
- [ ] 调试 Prompt，优化输出质量
- [ ] 测试 Token 使用量（确保不超预算）
- [ ] 测试多轮对话（会话记忆）

---

## 相关文件

```
src/main/java/com/vibe/agent/
├── VibeAgent.java              # AI Service 接口（声明式）
└── VibeAgentConfig.java        # Agent 配置

src/main/java/com/vibe/service/
└── VibeService.java            # Service 层（集成 Agent）

src/main/resources/prompts/
└── vibe-system.txt             # System Prompt

src/test/java/com/vibe/agent/
└── VibeAgentIntegrationTest.java  # 集成测试

application.yml                 # 配置文件
```

## 完成标准

- [ ] VibeAgent 接口定义完成
- [ ] Prompt 资源文件创建完成
- [ ] Agent 配置正确，可成功构建
- [ ] Service 层集成完成
- [ ] 安全模式正确生效
- [ ] 集成测试通过（3+ 场景）
- [ ] Token 使用量在合理范围内
- [ ] 代码已提交到 Git

## 问题与笔记

### 重构要点

1. **声明式接口**：无需实现类，框架自动处理
2. **自动 Tool Calling**：框架根据 Prompt 和 @Tool 描述自动选择和调用工具
3. **自动 JSON 解析**：框架根据 @Description 自动解析为 AmbiencePlan
4. **内置会话管理**：使用 @MemoryId 自动隔离不同用户/车辆的会话
5. **执行元数据**：使用 Result<T> 获取 Token 使用量和 Tool 执行详情

### 开发建议

1. **先 Mock 后真实**：使用 MockMusicTool 等测试 Agent 流程
2. **Prompt 迭代**：先简单 Prompt，根据输出逐步优化
3. **日志调试**：启用 `dev.langchain4j: DEBUG` 查看完整调用链
4. **成本控制**：使用 gpt-4o-mini 开发，gpt-4o 生产

### 参考文档

- `docs/design/architecture.md` Section 6.3
- `docs/design/prompt-design.md`
- `docs/design/refactoring-guide.md` Phase 3, 4
