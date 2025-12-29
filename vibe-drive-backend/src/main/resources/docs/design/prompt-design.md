# Vibe Drive Agent Prompt 设计

## 1. 概述

本文档定义 Vibe Agent 的 Prompt 设计，包括 System Prompt、Few-shot 示例和输出格式规范。

**重要变更（基于 LangChain4j 最佳实践）**：
- ✅ 使用 `@SystemMessage(fromResource)` 从资源文件加载 Prompt
- ✅ 使用 `SystemMessageProvider` 支持动态 Prompt
- ✅ 使用 `@UserMessage` 模板变量
- ✅ 使用 `@Description` 注解实现结构化输出（无需手动解析 JSON）

---

## 2. System Prompt 设计

### 2.1 方式 1：使用资源文件（推荐）

**资源文件位置**：
```
src/main/resources/prompts/
├── vibe-system.txt           - 主 System Prompt
└── vibe-safety-rules.txt     - 安全规则（可选）
```

**AI Service 定义**：
```java
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.V;

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

        请根据安全模式规则，输出氛围编排方案。
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferencesJson
    );
}
```

### 2.2 方式 2：使用 SystemMessageProvider（动态生成）

```java
import dev.langchain4j.service.AiServices;

@Configuration
public class VibeAgentConfig {

    @Bean
    public VibeAgent vibeAgent(
            ChatLanguageModel chatModel,
            MusicTool musicTool,
            LightTool lightTool,
            NarrativeTool narrativeTool) {

        return AiServices.builder(VibeAgent.class)
            .chatLanguageModel(chatModel)
            .tools(musicTool, lightTool, narrativeTool)
            .systemMessageProvider(memoryId -> {
                // 根据 memoryId 加载用户偏好
                UserPreferences prefs = loadUserPreferences(memoryId);
                return buildDynamicSystemPrompt(prefs);
            })
            .build();
    }

    private String buildDynamicSystemPrompt(UserPreferences prefs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(loadPromptTemplate("prompts/vibe-system.txt"));

        // 动态添加用户特定的偏好
        if (prefs.preferredGenres() != null) {
            prompt.append("\n\n## 用户音乐偏好\n");
            prompt.append("用户偏好的音乐流派: ");
            prompt.append(String.join(", ", prefs.preferredGenres()));
        }

        return prompt.toString();
    }
}
```

### 2.3 System Prompt 内容（vibe-system.txt）

```txt
你是 Vibe Drive 的氛围编排智能体，负责根据车载环境数据为驾驶者创造沉浸式的"时空叙事"体验。

## 你的角色

你是一位懂得感知情绪、理解场景的车载氛围设计师。你的任务是：
1. 理解当前驾驶环境的"情感语义"（不是简单的关键字匹配）
2. 编排合适的音乐、灯光和叙事文本
3. 确保驾驶安全始终是第一优先级

## 环境数据说明

你会收到以下环境数据：
- gpsTag: 地理标签（highway/tunnel/bridge/urban/suburban/mountain/coastal/parking）
- weather: 天气（sunny/cloudy/rainy/snowy/foggy）
- speed: 车速（km/h）
- userMood: 用户情绪（happy/calm/tired/stressed/excited）
- timeOfDay: 时段（dawn/morning/noon/afternoon/evening/night/midnight）
- passengerCount: 乘客数量（1-7）
- routeType: 路线类型（highway/urban/mountain/coastal/tunnel）

## 安全模式规则（必须严格遵守）

根据车速自动进入不同安全模式：

### L1 正常模式（车速 < 60 km/h）
- 全功能开放
- 可以主动推荐氛围变化
- 可以使用动态灯光效果

### L2 专注模式（60 ≤ 车速 < 100 km/h）
- 禁用动态灯光效果，只能使用静态灯光
- 降低推荐频率
- 叙事文本保持简短

### L3 静默模式（车速 ≥ 100 km/h）
- 禁用所有主动推荐
- 禁用灯光变化
- TTS 音量降低 30%
- 只响应用户主动指令

## 可用工具（由系统注入）

你可以按需调用以下能力（无需手写 toolCalls，LangChain4j 会自动完成工具选择、调用与结果注入）：

1. **音乐推荐**：根据 mood/timeOfDay/passengerCount/genre 等信息返回 1-10 首歌曲
2. **灯光设置**：根据 mood/timeOfDay/weather 生成灯光配置（L2 禁用动态效果；L3 禁用）
3. **叙事生成**：根据环境生成 ≤50 字的 TTS 文本（L3 音量自动降低 30%）

## 推理规则

1. **语义理解优先**：
   - "深夜 + 雨天 + 疲劳" → 用户需要放松和陪伴，而非刺激
   - "早晨 + 晴天 + 多人" → 家庭出游氛围，需要欢快共享的音乐
   - "傍晚 + 海滨 + 平静" → 浪漫惬意的氛围

2. **乘客数量影响**：
   - 1人：可以推荐个性化、小众的音乐
   - 2人：考虑浪漫或对话友好的氛围
   - 3人以上：选择大众化、欢快的音乐，避免争议

3. **时段影响**：
   - 深夜/凌晨：避免激烈音乐，注重安全提醒
   - 早晨：可以稍微活力，帮助清醒
   - 傍晚：适合放松，过渡到休息状态

4. **天气影响**：
   - 雨天：适合舒缓、有意境的音乐
   - 晴天：可以更明快
   - 雾天：注重安全，减少干扰

## 输出格式

你必须输出一个 `AmbiencePlan` JSON 对象（不要输出 toolCalls；工具调用由系统自动完成），示例：

```json
{
  "id": "plan-20251223-001",
  "music": {
    "songs": [
      { "id": "song_001", "title": "夜空中最亮的星", "artist": "逃跑计划" }
    ],
    "mood": "calm",
    "genre": "jazz",
    "bpmRange": { "min": 60, "max": 90 }
  },
  "light": {
    "color": { "hex": "#FFE4B5", "temperature": 2700 },
    "brightness": 30,
    "mode": "static",
    "transitionDuration": 1000
  },
  "narrative": {
    "text": "夜深了，慢一点，音乐会陪你安全到家。",
    "voice": "default",
    "speed": 1.0,
    "volume": 0.8,
    "emotion": "gentle"
  },
  "safetyMode": "L2_FOCUS",
  "reasoning": "简短决策说明（不要逐步输出思维链）",
  "createdAt": "2025-12-23T23:30:00Z"
}
```

约束：
- L2：`light.mode` 必须为 `static`
- L3：`light` 必须为 `null`，叙事尽量简短

## 重要提醒

1. 安全第一：高速行驶时严格遵守安全模式限制
2. 简洁温馨：叙事文本不超过 50 字，语气温暖不啰嗦
3. 情感共鸣：理解场景背后的情感需求，而非机械匹配
4. 避免打扰：不要过于频繁地推荐变化，给用户空间
```

### 2.4 旧方式：手动拼接 Prompt（已废弃）

**⚠️ 以下方式已不推荐，建议使用 @SystemMessage(fromResource) 或 SystemMessageProvider**

```java
/**
 * Prompt 模板管理（旧方式，已废弃）
 * ❌ 不推荐：手动拼接 Prompt 字符串
 */
@Deprecated
public class PromptTemplates {

    // 角色定义
    public static final String ROLE = """
        你是 Vibe Drive 的氛围编排智能体，负责根据车载环境数据为驾驶者创造沉浸式的"时空叙事"体验。
        """;

    // 安全规则
    public static final String SAFETY_RULES = """
        ## 安全模式规则（必须严格遵守）

        ### L1 正常模式（车速 < 60 km/h）
        - 全功能开放

        ### L2 专注模式（60 ≤ 车速 < 100 km/h）
        - 禁用动态灯光效果
        - 叙事文本保持简短

        ### L3 静默模式（车速 ≥ 100 km/h）
        - 禁用所有主动推荐
        - 禁用灯光变化
        - TTS 音量降低 30%
        """;

    // 工具描述
    public static final String TOOLS_DESC = """
        ## 可用工具

        1. **recommend_music**: 推荐音乐
        2. **set_light**: 设置氛围灯
        3. **generate_narrative**: 生成叙事文本
        """;

    // 组装完整 Prompt（已废弃）
    @Deprecated
    public static String buildSystemPrompt(List<VibeTool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROLE).append("\n\n");
        sb.append(SAFETY_RULES).append("\n\n");
        sb.append(buildToolsSection(tools)).append("\n\n");
        sb.append(REASONING_RULES).append("\n\n");
        sb.append(OUTPUT_FORMAT);
        return sb.toString();
    }
}
```

**为什么废弃**：
- ❌ 代码中硬编码 Prompt 内容，不易维护
- ❌ 无法独立进行 Prompt A/B 测试
- ❌ 不符合 LangChain4j 最佳实践

**推荐替代方案**：使用 Section 2.1 或 2.2 的方式

---

## 3. Few-shot 示例

### 3.1 示例 1：深夜雨天高速

**输入**

```json
{
  "gpsTag": "highway",
  "weather": "rainy",
  "speed": 85,
  "userMood": "tired",
  "timeOfDay": "midnight",
  "passengerCount": 1,
  "routeType": "highway"
}
```

**期望输出**

```json
{
  "id": "plan_20251223_001",
  "music": {
    "songs": [
      { "id": "song_001", "title": "Take Five", "artist": "Dave Brubeck" },
      { "id": "song_002", "title": "Blue in Green", "artist": "Miles Davis" }
    ],
    "mood": "calm",
    "genre": "jazz",
    "bpmRange": { "min": 60, "max": 90 }
  },
  "light": {
    "color": { "hex": "#FFE4B5", "temperature": 2700 },
    "brightness": 20,
    "mode": "static",
    "transitionDuration": 1000
  },
  "narrative": {
    "text": "雨夜高速，放慢一点，听点舒缓的爵士，让音乐陪你安全到家。",
    "voice": "default",
    "speed": 0.9,
    "volume": 0.7,
    "emotion": "gentle"
  },
  "safetyMode": "L2_FOCUS",
  "reasoning": "深夜+雨天+疲劳更适合舒缓陪伴；L2 模式下仅使用静态灯光，避免干扰。",
  "createdAt": "2025-12-23T23:30:00Z"
}
```

### 3.2 示例 2：周末家庭出游

**输入**

```json
{
  "gpsTag": "suburban",
  "weather": "sunny",
  "speed": 50,
  "userMood": "happy",
  "timeOfDay": "morning",
  "passengerCount": 4,
  "routeType": "urban"
}
```

**期望输出**

```json
{
  "id": "plan_20251223_002",
  "music": {
    "songs": [
      { "id": "song_101", "title": "晴天", "artist": "周杰伦" },
      { "id": "song_102", "title": "小幸运", "artist": "田馥甄" }
    ],
    "mood": "happy",
    "genre": "pop",
    "bpmRange": { "min": 100, "max": 140 }
  },
  "light": {
    "color": { "hex": "#FFD54F", "temperature": 4000 },
    "brightness": 60,
    "mode": "gradient",
    "transitionDuration": 2000
  },
  "narrative": {
    "text": "周末早晨阳光正好，来点轻快的旋律，祝你们一路好心情。",
    "voice": "default",
    "speed": 1.0,
    "volume": 0.8,
    "emotion": "energetic"
  },
  "safetyMode": "L1_NORMAL",
  "reasoning": "多人出行更适合大众化、明快的氛围；L1 模式可使用渐变灯光增强活力感。",
  "createdAt": "2025-12-23T23:35:00Z"
}
```

### 3.3 示例 3：高速静默模式

**输入**

```json
{
  "gpsTag": "highway",
  "weather": "sunny",
  "speed": 120,
  "userMood": "calm",
  "timeOfDay": "afternoon",
  "passengerCount": 2,
  "routeType": "highway"
}
```

**期望输出**

```json
{
  "id": "plan_20251223_003",
  "music": {
    "songs": [
      { "id": "song_201", "title": "Weightless", "artist": "Marconi Union" }
    ],
    "mood": "calm",
    "genre": "ambient",
    "bpmRange": { "min": 50, "max": 80 }
  },
  "light": null,
  "narrative": {
    "text": "已进入静默模式，我会减少提示，祝你一路平安。",
    "voice": "default",
    "speed": 1.0,
    "volume": 0.6,
    "emotion": "neutral"
  },
  "safetyMode": "L3_SILENT",
  "reasoning": "车速≥100 进入 L3：禁用灯光变化，叙事保持最简短，避免打扰。",
  "createdAt": "2025-12-23T23:40:00Z"
}
```

---

## 4. User Prompt 模板

### 4.1 使用 @UserMessage 注解（推荐）

**方式 1：简单模板变量**

```java
public interface VibeAgent {

    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据，并编排合适的氛围方案：

        ## 当前环境
        ```json
        {{environment}}
        ```

        ## 用户偏好（如有）
        {{preferences}}

        请根据环境数据和安全模式规则，输出氛围编排方案。
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferencesJson
    );
}
```

**方式 2：使用 Mustache 条件语法**

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

        {{#if currentSong}}
        ## 当前播放
        正在播放：{{currentSong}}
        {{/if}}

        请根据环境数据和安全模式规则，输出氛围编排方案。
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferences,
        @V("currentSong") String currentSong
    );
}
```

**优势**：
- ✅ 清晰的参数绑定（`@V` 注解）
- ✅ 支持条件渲染（`{{#if}}`）
- ✅ 类型安全，编译时检查
- ✅ IDE 自动补全支持

### 4.2 旧方式：手动构建 User Prompt（已废弃）

**⚠️ 以下方式已不推荐**

```java
/**
 * User Prompt 构建器（已废弃）
 */
@Deprecated
public class UserPromptBuilder {

    public String build(Environment env, Map<String, Object> preferences) {
        StringBuilder sb = new StringBuilder();

        sb.append("请分析以下车载环境数据，并编排合适的氛围方案：\n\n");

        sb.append("## 当前环境\n");
        sb.append("```json\n");
        sb.append(toJson(env));
        sb.append("\n```\n\n");

        if (preferences != null && !preferences.isEmpty()) {
            sb.append("## 用户偏好\n");
            sb.append(formatPreferences(preferences));
            sb.append("\n\n");
        }

        sb.append("请根据环境数据和安全模式规则，输出氛围编排方案。");

        return sb.toString();
    }
}
```

**为什么废弃**：
- ❌ 手动拼接字符串，容易出错
- ❌ 无法利用 LangChain4j 的模板引擎
- ❌ 代码冗长，可读性差

---

## 5. 结构化输出与自动解析

### 5.1 使用 @Description 注解（推荐）

**重要变更**：使用 `@Description` 注解后，LangChain4j 自动处理 JSON 解析，无需手动解析器。

**Step 1：定义数据模型（使用 @Description 注解）**

```java
import dev.langchain4j.model.output.structured.Description;

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

    @Description("当前安全模式：L1_NORMAL（正常）/L2_FOCUS（专注）/L3_SILENT（静默）")
    SafetyMode safetyMode,

    @Description("Agent的推理过程，说明为何做出此氛围选择")
    String reasoning,

    @Description("方案创建时间")
    Instant createdAt
) {}
```

**Step 2：AI Service 直接返回 Record，自动解析**

```java
public interface VibeAgent {

    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据，并编排合适的氛围方案：

        ## 当前环境
        ```json
        {{environment}}
        ```

        请根据环境数据和安全模式规则，输出氛围编排方案。
        """)
    AmbiencePlan analyzeEnvironment(
        @V("environment") String environmentJson
    );  // ✅ 直接返回 Record，自动解析
}
```

**Step 3：配置 strictJsonSchema（可选，但推荐）**

```java
@Configuration
public class AiServiceConfig {

    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o")
            .strictJsonSchema(true)  // ✅ 严格 JSON Schema
            .supportedCapabilities(Set.of(RESPONSE_FORMAT_JSON_SCHEMA))
            .build();
    }

    @Bean
    public VibeAgent vibeAgent(ChatLanguageModel chatModel) {
        return AiServices.builder(VibeAgent.class)
            .chatLanguageModel(chatModel)
            .tools(musicTool, lightTool, narrativeTool)
            .build();
    }
}
```

**工作原理**：
1. LangChain4j 根据 `@Description` 注解自动生成 JSON Schema
2. JSON Schema 传递给 LLM，指导输出格式
3. LLM 返回的 JSON 自动反序列化为 Record
4. 无需手动编写解析器

**优势**：
- ✅ 类型安全：编译时检查
- ✅ 自动解析：无需手动提取 JSON
- ✅ LLM 友好：`@Description` 帮助 LLM 理解字段含义
- ✅ 严格模式：`strictJsonSchema` 确保输出格式正确

### 5.2 输出 JSON Schema（自动生成）

使用 `@Description` 注解后，LangChain4j 自动生成以下 Schema：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AmbiencePlan",
  "description": "氛围编排方案，包含音乐、灯光和叙事的完整配置",
  "type": "object",
  "required": ["id", "music", "safetyMode", "reasoning"],
  "properties": {
    "id": {
      "type": "string",
      "description": "方案唯一标识符"
    },
    "music": {
      "type": "object",
      "description": "推荐的音乐列表和相关元数据"
    },
    "light": {
      "type": "object",
      "description": "氛围灯设置，L2专注模式下禁用动态效果，L3静默模式下为null"
    },
    "narrative": {
      "type": "object",
      "description": "TTS播报的叙事文本及语音参数"
    },
    "safetyMode": {
      "type": "string",
      "enum": ["L1_NORMAL", "L2_FOCUS", "L3_SILENT"],
      "description": "当前安全模式"
    },
    "reasoning": {
      "type": "string",
      "description": "Agent的推理过程，说明为何做出此氛围选择"
    }
  }
}
```

### 5.3 旧方式：手动解析 JSON（已废弃）

**⚠️ 以下方式已不推荐**

```java
/**
 * Agent 输出解析器（已废弃）
 * ❌ 不推荐：手动提取和解析 JSON
 */
@Deprecated
@Component
public class AgentOutputParser {

    private final ObjectMapper objectMapper;

    public AgentOutput parse(String llmResponse) {
        try {
            // 提取 JSON 部分
            String json = extractJson(llmResponse);
            return objectMapper.readValue(json, AgentOutput.class);
        } catch (Exception e) {
            throw new AgentOutputParseException("Failed to parse agent output", e);
        }
    }

    private String extractJson(String response) {
        // 处理 markdown 代码块
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        // 直接尝试解析
        return response.trim();
    }
}

public record AgentOutput(
    String reasoning,
    SafetyMode safetyMode,
    List<ToolCall> toolCalls
) {}

public record ToolCall(
    String tool,
    Map<String, Object> args
) {}
```

**为什么废弃**：
- ❌ 手动提取 JSON，容易出错
- ❌ 无法利用 LangChain4j 的自动解析
- ❌ 需要额外的错误处理代码
- ❌ 不支持严格 JSON Schema 验证

**推荐替代方案**：使用 Section 5.1 的方式，直接返回 Record

---

## 6. Prompt 优化策略

### 6.1 减少幻觉

```
## 重要约束

1. 只使用提供的工具，不要编造不存在的工具
2. 工具参数必须符合定义的类型和取值范围
3. 如果环境数据不完整，使用合理的默认值而非编造
4. 不确定时，选择更保守的方案
```

### 6.2 提高一致性

```
## 输出检查清单

在输出前，请确认：
- [ ] safetyMode 与车速匹配
- [ ] L2 模式下 `light.mode` 为 `static`（禁用动态效果）
- [ ] L3 模式下 `light` 为 `null`，叙事尽量简短（系统会降低音量 30%）
- [ ] reasoning 为“决策摘要”，不要逐步输出思维链
- [ ] 输出字段与数据模型一致（由 strictJsonSchema 约束）
- [ ] 工具调用是否合理可从 `Result.toolExecutions()` 追溯
```

### 6.3 处理边界情况

```
## 特殊情况处理

1. **车速为 0（停车）**：
   - 可以使用全部功能
   - 适合更丰富的氛围体验

2. **情绪为 stressed + 高速**：
   - 安全优先，不要增加刺激
   - 选择舒缓音乐帮助放松

3. **深夜 + 疲劳**：
   - 避免过于舒缓导致困倦
   - 可以适当提神但不刺激

4. **多人 + 不同偏好**：
   - 选择大众化、无争议的音乐
   - 避免小众或极端风格
```

---

## 7. LangChain4j 集成

### 7.1 AI Service 定义

```java
/**
 * Vibe Agent AI Service
 * - Tool Calling + Structured Outputs
 * - 使用 @MemoryId 隔离会话
 */
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

### 7.2 配置

```java
@Configuration
public class AiServiceConfig {

    @Bean
    public VibeAgent vibeAgent(
            ChatLanguageModel chatModel,
            MusicTool musicTool,
            LightTool lightTool,
            NarrativeTool narrativeTool) {

        return AiServices.builder(VibeAgent.class)
            .chatLanguageModel(chatModel)
            .tools(musicTool, lightTool, narrativeTool)
            .chatMemoryProvider(memoryId ->
                MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(10)
                    .build())
            .build();
    }
}
```

---

## 8. Prompt 版本管理

### 8.1 版本记录

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.0 | 2025-12-23 | 初始版本 |

### 8.2 A/B 测试支持

```java
/**
 * Prompt 版本管理
 */
@Component
public class PromptVersionManager {

    private final Map<String, String> promptVersions = new ConcurrentHashMap<>();

    public void registerVersion(String version, String prompt) {
        promptVersions.put(version, prompt);
    }

    public String getPrompt(String version) {
        return promptVersions.getOrDefault(version, promptVersions.get("default"));
    }

    // A/B 测试：随机选择版本
    public String getPromptForABTest(List<String> versions) {
        int index = ThreadLocalRandom.current().nextInt(versions.size());
        return getPrompt(versions.get(index));
    }
}
```

---

## 9. 评估指标

### 9.1 Prompt 质量评估

| 指标 | 说明 | 目标 |
|------|------|------|
| 安全合规率 | 输出符合安全模式规则的比例 | > 99% |
| 格式正确率 | 输出 JSON 格式正确的比例 | > 95% |
| 工具调用正确率 | 工具参数有效的比例 | > 95% |
| 语义相关性 | 推理与环境匹配的程度 | 人工评估 |

### 9.2 评估脚本

```java
/**
 * Prompt 评估器
 */
public class PromptEvaluator {

    public EvaluationResult evaluate(Environment env, Result<AmbiencePlan> result) {
        List<String> issues = new ArrayList<>();

        AmbiencePlan plan = result.content();

        // 检查安全模式
        SafetyMode expected = SafetyMode.fromSpeed(env.speed());
        if (plan.safetyMode() != expected) {
            issues.add("Safety mode mismatch: expected " + expected + ", got " + plan.safetyMode());
        }

        // 检查 L3 模式：禁用灯光变化
        if (expected == SafetyMode.L3_SILENT && plan.light() != null) {
            issues.add("L3 mode should disable light changes");
        }

        // 检查 L2 模式：禁用动态灯光
        if (expected == SafetyMode.L2_FOCUS && plan.light() != null
                && plan.light().mode() != LightMode.STATIC) {
            issues.add("L2 mode should only use static light");
        }

        // 可选：从 Result.toolExecutions() 校验是否触发了不允许的工具
        boolean hasLightToolExecution = result.toolExecutions().stream()
            .anyMatch(exec -> exec.toolName().toLowerCase().contains("light"));
        if (expected == SafetyMode.L3_SILENT && hasLightToolExecution) {
            issues.add("L3 mode should avoid light tool executions");
        }

        return new EvaluationResult(issues.isEmpty(), issues);
    }
}
```
