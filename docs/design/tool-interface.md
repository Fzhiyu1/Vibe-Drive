# Vibe Drive Tool 接口设计

## 1. 概述

Tool 是 Vibe Agent 调用的原子服务，负责执行具体的氛围编排操作。本文档定义各 Tool 的接口规范。

**重要变更（基于 LangChain4j 最佳实践）**：
- ❌ 移除了 `VibeTool` 基础接口（不必要的抽象）
- ❌ 移除了 `ToolRegistry` 类（LangChain4j 已内置）
- ✅ 直接使用 `@Tool` 注解的普通类
- ✅ 通过 `AiServices.builder().tools(...)` 注册

### 1.1 Tool 设计原则

- **单一职责**：每个 Tool 只做一件事
- **无状态**：Tool 本身不维护状态，状态由 Agent 管理
- **可 Mock**：所有 Tool 支持 Mock 实现，便于测试和演示
- **LLM 友好**：使用 `@Tool` 和 `@P` 注解提供清晰描述

### 1.2 Tool 列表

| Tool | 职责 | 输入 | 输出 |
|------|------|------|------|
| MusicTool | 音乐推荐 | 情绪、时段、乘客数 | 歌曲列表 |
| LightTool | 灯光控制 | 情绪、时段、天气 | 灯光设置 |
| NarrativeTool | 叙事生成 | 环境、当前歌曲 | TTS 文本 |

---

## 2. Tool 基础设计（基于 LangChain4j 最佳实践）

### 2.1 无需基础接口

**旧设计（已废弃）**：
```java
// ❌ 不再需要 VibeTool 接口
public interface VibeTool {
    String getName();
    String getDescription();
    boolean isAvailable();
}
```

**新设计**：
```java
// ✅ 直接使用 @Tool 注解的普通类
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

@Component
public class MusicTool {

    @Tool("根据用户情绪、时段和乘客数量推荐合适的音乐")
    public MusicRecommendation recommendMusic(
        @P("目标情绪") String mood,
        @P("时段") String timeOfDay,
        @P("乘客数量") int passengerCount,
        @P("偏好流派，可选") String genre
    ) {
        // 实现逻辑
    }
}
```

### 2.2 Tool 注册方式

**旧方式（已废弃）**：
```java
// ❌ 不再需要 ToolRegistry
ToolRegistry registry = new ToolRegistry();
registry.register(musicTool);
registry.register(lightTool);
```

**新方式**：
```java
// ✅ 直接通过 AiServices.builder() 注册
VibeAgent agent = AiServices.builder(VibeAgent.class)
    .chatModel(model)
    .tools(new MusicTool(), new LightTool(), new NarrativeTool())
    .build();
```

---

## 3. MusicTool - 音乐推荐工具

### 3.1 类定义（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.stereotype.Component;

/**
 * 音乐推荐工具
 * 根据情绪、时段、乘客数量推荐合适的音乐
 */
@Component
public class MusicTool {

    private final MusicService musicService;

    public MusicTool(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * 推荐音乐
     *
     * @param mood      目标情绪 (happy/calm/tired/stressed/excited)
     * @param timeOfDay 时段 (dawn/morning/noon/afternoon/evening/night/midnight)
     * @param passengerCount 乘客数量 (1-7)
     * @param genre     偏好流派（可选）
     * @return 音乐推荐结果
     */
    @Tool("""
        根据用户情绪、时段和乘客数量推荐合适的音乐。
        - 独自驾驶时推荐个人化音乐
        - 多人乘坐时推荐大众化、欢快的音乐
        - 疲劳时推荐舒缓音乐
        - 深夜时避免过于激烈的音乐
        """)
    public MusicRecommendation recommendMusic(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("乘客数量: 1-7") int passengerCount,
        @P("偏好流派，可选: pop/rock/jazz/classical") String genre
    ) {
        return musicService.recommend(mood, timeOfDay, passengerCount, genre);
    }
}
```

### 3.2 输入参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| mood | String | 是 | 目标情绪：happy/calm/tired/stressed/excited |
| timeOfDay | String | 是 | 时段：dawn/morning/noon/afternoon/evening/night/midnight |
| passengerCount | int | 是 | 乘客数量：1-7 |
| genre | String | 否 | 偏好流派：pop/rock/jazz/classical/electronic/folk |

### 3.3 输出格式

```json
{
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
  "genre": "rock",
  "bpmRange": {
    "min": 60,
    "max": 90
  }
}
```

### 3.4 推荐逻辑（参考）

```java
/**
 * 音乐推荐逻辑
 */
public class MusicRecommendationLogic {

    // 情绪 → BPM 范围映射
    private static final Map<String, BpmRange> MOOD_BPM_MAP = Map.of(
        "happy", new BpmRange(100, 140),
        "calm", new BpmRange(60, 90),
        "tired", new BpmRange(50, 80),
        "stressed", new BpmRange(60, 100),
        "excited", new BpmRange(120, 160)
    );

    // 时段 → 推荐流派映射
    private static final Map<String, List<String>> TIME_GENRE_MAP = Map.of(
        "midnight", List.of("jazz", "classical", "ambient"),
        "morning", List.of("pop", "folk", "acoustic"),
        "noon", List.of("pop", "rock"),
        "evening", List.of("jazz", "r&b", "soul")
    );

    // 乘客数量 → 风格调整
    public String adjustForPassengers(int count, String baseGenre) {
        if (count >= 3) {
            // 多人时选择更大众化的音乐
            return "pop";
        }
        return baseGenre;
    }
}
```

### 3.5 Mock 实现（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockMusicTool {

    private static final List<Song> MOCK_SONGS = List.of(
        new Song("1", "夜空中最亮的星", "逃跑计划", "世界", 252, 76, "rock", null),
        new Song("2", "平凡之路", "朴树", "猎户星座", 295, 82, "folk", null),
        new Song("3", "晴天", "周杰伦", "叶惠美", 269, 120, "pop", null),
        new Song("4", "Take Five", "Dave Brubeck", "Time Out", 324, 88, "jazz", null),
        new Song("5", "Clair de Lune", "Debussy", "Suite bergamasque", 300, 60, "classical", null)
    );

    @Tool("""
        根据用户情绪、时段和乘客数量推荐合适的音乐。
        - 独自驾驶时推荐个人化音乐
        - 多人乘坐时推荐大众化、欢快的音乐
        - 疲劳时推荐舒缓音乐
        - 深夜时避免过于激烈的音乐
        """)
    public MusicRecommendation recommendMusic(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("乘客数量: 1-7") int passengerCount,
        @P("偏好流派，可选: pop/rock/jazz/classical") String genre
    ) {
        // 简单的 Mock 逻辑：根据情绪筛选
        List<Song> filtered = MOCK_SONGS.stream()
            .filter(s -> matchesMood(s, mood))
            .limit(5)
            .toList();

        return new MusicRecommendation(
            filtered.isEmpty() ? MOCK_SONGS.subList(0, 3) : filtered,
            mood,
            genre != null ? genre : "mixed",
            new BpmRange(60, 120)
        );
    }

    private boolean matchesMood(Song song, String mood) {
        return switch (mood) {
            case "calm", "tired" -> song.bpm() < 90;
            case "happy", "excited" -> song.bpm() > 100;
            default -> true;
        };
    }
}
```

---

## 4. LightTool - 灯光控制工具

### 4.1 类定义（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.stereotype.Component;

/**
 * 灯光控制工具
 * 根据情绪、时段、天气设置氛围灯
 */
@Component
public class LightTool {

    private final LightService lightService;

    public LightTool(LightService lightService) {
        this.lightService = lightService;
    }

    /**
     * 设置灯光
     *
     * @param mood      目标情绪
     * @param timeOfDay 时段
     * @param weather   天气
     * @return 灯光设置
     */
    @Tool("""
        根据情绪、时段和天气设置车内氛围灯。
        - 深夜/疲劳时使用暖色调低亮度
        - 晴天/开心时可使用明亮活力的颜色
        - 雨天/压力时使用柔和舒缓的颜色
        - 高速行驶时（L2/L3模式）禁用动态效果
        """)
    public LightSetting setLight(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("天气: sunny/cloudy/rainy/snowy/foggy") String weather
    ) {
        return lightService.calculateSetting(mood, timeOfDay, weather);
    }
}
```

### 4.2 输入参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| mood | String | 是 | 目标情绪 |
| timeOfDay | String | 是 | 时段 |
| weather | String | 是 | 天气：sunny/cloudy/rainy/snowy/foggy |

### 4.3 输出格式

```json
{
  "color": {
    "hex": "#FFE4B5",
    "temperature": 2700
  },
  "brightness": 30,
  "mode": "breathing",
  "transitionDuration": 2000,
  "zones": [
    { "zone": "dashboard", "color": "#FFE4B5", "brightness": 30 },
    { "zone": "door", "color": "#FFD700", "brightness": 20 }
  ]
}
```

### 4.4 灯光预设

```java
/**
 * 灯光预设配置
 */
public class LightPresets {

    // 情绪 → 颜色映射
    public static final Map<String, LightColor> MOOD_COLORS = Map.of(
        "happy", new LightColor("#FFD700", 4000),      // 金色，活力
        "calm", new LightColor("#87CEEB", 5000),       // 天蓝，平静
        "tired", new LightColor("#FFE4B5", 2700),      // 暖白，舒适
        "stressed", new LightColor("#98FB98", 4500),   // 淡绿，放松
        "excited", new LightColor("#FF69B4", 4000)     // 粉色，活力
    );

    // 时段 → 亮度映射
    public static final Map<String, Integer> TIME_BRIGHTNESS = Map.of(
        "dawn", 40,
        "morning", 60,
        "noon", 70,
        "afternoon", 60,
        "evening", 50,
        "night", 30,
        "midnight", 20
    );

    // 天气 → 模式映射
    public static final Map<String, LightMode> WEATHER_MODE = Map.of(
        "sunny", LightMode.STATIC,
        "cloudy", LightMode.STATIC,
        "rainy", LightMode.BREATHING,
        "snowy", LightMode.GRADIENT,
        "foggy", LightMode.STATIC
    );
}
```

### 4.5 Mock 实现（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockLightTool {

    @Tool("""
        根据情绪、时段和天气设置车内氛围灯。
        - 深夜/疲劳时使用暖色调低亮度
        - 晴天/开心时可使用明亮活力的颜色
        - 雨天/压力时使用柔和舒缓的颜色
        - 高速行驶时（L2/L3模式）禁用动态效果
        """)
    public LightSetting setLight(
        @P("目标情绪: happy/calm/tired/stressed/excited") String mood,
        @P("时段: dawn/morning/noon/afternoon/evening/night/midnight") String timeOfDay,
        @P("天气: sunny/cloudy/rainy/snowy/foggy") String weather
    ) {
        LightColor color = LightPresets.MOOD_COLORS.getOrDefault(mood, LightColor.warmWhite());
        int brightness = LightPresets.TIME_BRIGHTNESS.getOrDefault(timeOfDay, 50);
        LightMode mode = LightPresets.WEATHER_MODE.getOrDefault(weather, LightMode.STATIC);

        return new LightSetting(
            color,
            brightness,
            mode,
            1500,
            List.of()  // 简化：不分区
        );
    }
}
```

---

## 5. NarrativeTool - 叙事生成工具

### 5.1 类定义（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.stereotype.Component;

/**
 * 叙事生成工具
 * 生成 TTS 播报文本，将环境与音乐进行"时空编织"
 */
@Component
public class NarrativeTool {

    private final NarrativeService narrativeService;
    private final ObjectMapper objectMapper;

    public NarrativeTool(NarrativeService narrativeService, ObjectMapper objectMapper) {
        this.narrativeService = narrativeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成叙事文本
     *
     * @param environmentJson 当前环境 JSON
     * @param currentSong 当前播放的歌曲（可选）
     * @param theme       叙事主题（可选）
     * @return 叙事文本
     */
    @Tool("""
        生成 TTS 播报文本，将窗外风景与音乐进行"时空编织"。
        - 文本应简短温馨，不超过 50 字
        - 结合当前环境（天气、时段、位置）
        - 如有正在播放的歌曲，可以关联歌曲意境
        - 语气应符合当前氛围（深夜轻柔，早晨活力）
        """)
    public Narrative generateNarrative(
        @P("当前环境的JSON字符串") String environmentJson,
        @P("当前歌曲名称，可选") String currentSong,
        @P("叙事主题，可选: comfort/energy/romance/adventure") String theme
    ) throws JsonProcessingException {
        Environment env = objectMapper.readValue(environmentJson, Environment.class);
        return narrativeService.generate(env, currentSong, theme);
    }
}
```

### 5.2 输入参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| environmentJson | String | 是 | 当前环境的 JSON 字符串 |
| currentSong | String | 否 | 当前播放的歌曲名称 |
| theme | String | 否 | 叙事主题：comfort/energy/romance/adventure |

### 5.3 输出格式

```json
{
  "text": "夜深了，窗外的雨声和这首歌很配，让音乐陪你安全到家。",
  "voice": "gentle_female",
  "speed": 0.9,
  "volume": 0.7,
  "emotion": "gentle"
}
```

### 5.4 叙事模板

```java
/**
 * 叙事模板库
 */
public class NarrativeTemplates {

    // 深夜 + 雨天
    public static final List<String> MIDNIGHT_RAIN = List.of(
        "夜深了，窗外的雨声和这首歌很配，让音乐陪你安全到家。",
        "雨夜的路上，愿这首歌温暖你的归途。",
        "深夜的雨，洗去一天的疲惫，慢慢开，安全到家。"
    );

    // 早晨 + 晴天
    public static final List<String> MORNING_SUNNY = List.of(
        "阳光正好，新的一天从这首歌开始。",
        "早安，今天也是元气满满的一天！",
        "清晨的阳光和音乐，是最好的出发仪式。"
    );

    // 傍晚 + 海滨
    public static final List<String> EVENING_COASTAL = List.of(
        "夕阳西下，海风轻拂，享受这片刻的宁静。",
        "海边的傍晚，让音乐和海浪一起陪伴你。"
    );

    // 通用模板
    public static String generate(Environment env, String song) {
        String template = selectTemplate(env);
        if (song != null && !song.isBlank()) {
            template = template.replace("这首歌", "《" + song + "》");
        }
        return template;
    }

    private static String selectTemplate(Environment env) {
        // 根据环境选择模板
        if (env.timeOfDay() == TimeOfDay.MIDNIGHT && env.weather() == Weather.RAINY) {
            return randomFrom(MIDNIGHT_RAIN);
        }
        if (env.timeOfDay() == TimeOfDay.MORNING && env.weather() == Weather.SUNNY) {
            return randomFrom(MORNING_SUNNY);
        }
        // ... 更多匹配
        return "享受旅途，让音乐陪伴你。";
    }
}
```

### 5.5 Mock 实现（新设计）

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockNarrativeTool {

    private final ObjectMapper objectMapper;

    public MockNarrativeTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool("""
        生成 TTS 播报文本，将窗外风景与音乐进行"时空编织"。
        - 文本应简短温馨，不超过 50 字
        - 结合当前环境（天气、时段、位置）
        - 如有正在播放的歌曲，可以关联歌曲意境
        - 语气应符合当前氛围（深夜轻柔，早晨活力）
        """)
    public Narrative generateNarrative(
        @P("当前环境的JSON字符串") String environmentJson,
        @P("当前歌曲名称，可选") String currentSong,
        @P("叙事主题，可选: comfort/energy/romance/adventure") String theme
    ) {
        try {
            Environment env = objectMapper.readValue(environmentJson, Environment.class);
            String text = NarrativeTemplates.generate(env, currentSong);
            NarrativeEmotion emotion = mapEmotion(env.userMood());

            return new Narrative(
                text,
                "default",
                env.timeOfDay() == TimeOfDay.MIDNIGHT ? 0.85 : 1.0,
                env.timeOfDay() == TimeOfDay.MIDNIGHT ? 0.6 : 0.8,
                emotion
            );
        } catch (Exception e) {
            return new Narrative(
                "享受旅途，让音乐陪伴你。",
                "default", 1.0, 0.8,
                NarrativeEmotion.NEUTRAL
            );
        }
    }

    private NarrativeEmotion mapEmotion(UserMood mood) {
        return switch (mood) {
            case HAPPY, EXCITED -> NarrativeEmotion.ENERGETIC;
            case CALM -> NarrativeEmotion.CALM;
            case TIRED -> NarrativeEmotion.GENTLE;
            case STRESSED -> NarrativeEmotion.WARM;
        };
    }
}
```

---

## 6. Tool 注册与使用（新设计）

### 6.1 无需 ToolRegistry

**旧方式（已废弃）**：
```java
// ❌ 不再需要 ToolRegistry
@Component
public class ToolRegistry {
    private final Map<String, VibeTool> tools = new ConcurrentHashMap<>();
    // ...
}
```

**新方式**：
```java
// ✅ LangChain4j 自动管理 Tools
// 只需通过 AiServices.builder() 注册即可
```

### 6.2 Tool 配置（新设计）

```java
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            .tools(musicTool, lightTool, narrativeTool)  // ✅ 直接注册 Tools
            .chatMemoryProvider(memoryId ->
                MessageWindowChatMemory.withMaxMessages(20))
            .build();
    }
}
```

**关键变化**：
- ✅ 不需要手动注册 Tool
- ✅ 不需要 ToolRegistry 类
- ✅ LangChain4j 自动扫描 `@Tool` 注解的方法
- ✅ LangChain4j 自动生成 Tool 的 JSON Schema 给 LLM

---

## 7. Tool 调用流程（简化后）

```
┌─────────────────┐
│   Vibe Agent    │
│   (AI Service)  │
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────┐
│      LangChain4j 框架                │
│  ┌────────────────────────────────┐ │
│  │ 1. 自动发现 @Tool 方法         │ │
│  │ 2. 生成 Tool JSON Schema       │ │
│  │ 3. LLM 决定调用哪个 Tool       │ │
│  │ 4. 自动执行 Tool 方法          │ │
│  │ 5. 将结果返回给 LLM            │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
         │
         ↓
┌─────────────────┐
│   Tool 实例     │
│ (MusicTool等)   │
└────────┬────────┘
         │ 返回结果
         ↓
┌─────────────────┐
│   Vibe Agent    │
│  (整合结果)     │
└─────────────────┘
```

---

## 8. 扩展新 Tool（新设计）

### 8.1 扩展步骤（简化）

1. **创建 Tool 类**（添加 `@Component` 和 `@Tool` 注解）
2. **实现 Mock 版本**（使用 `@Profile("mock")`）
3. **注册到 AiServices**（在配置类中添加到 `.tools(...)` 参数）
4. 完成！

**无需**：
- ❌ 定义接口
- ❌ 继承 VibeTool
- ❌ 手动注册到 ToolRegistry

### 8.2 示例：添加香氛控制 Tool

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.springframework.stereotype.Component;

/**
 * 香氛控制工具（示例扩展）
 */
@Component
public class FragranceTool {

    private final FragranceService fragranceService;

    public FragranceTool(FragranceService fragranceService) {
        this.fragranceService = fragranceService;
    }

    @Tool("""
        控制车内香氛系统。
        - 疲劳时释放薄荷提神
        - 压力时释放薰衣草舒缓
        - 可调节浓度（低/中/高）
        """)
    public FragranceSetting setFragrance(
        @P("香氛类型: mint/lavender/ocean/forest") String type,
        @P("浓度级别: low/medium/high") String intensity
    ) {
        return fragranceService.setFragrance(type, intensity);
    }
}

@Description("香氛设置")
public record FragranceSetting(
    @Description("香氛类型：mint/lavender/ocean/forest")
    String type,

    @Description("浓度级别：low/medium/high")
    String intensity,

    @Description("持续时长（分钟）")
    int durationMinutes
) {}

// 注册到 AI Service（在配置类中）
@Bean
public VibeAgent vibeAgent(
        ChatLanguageModel chatModel,
        MusicTool musicTool,
        LightTool lightTool,
        NarrativeTool narrativeTool,
        FragranceTool fragranceTool) {  // ✅ 新增 Tool

    return AiServices.builder(VibeAgent.class)
        .chatLanguageModel(chatModel)
        .tools(musicTool, lightTool, narrativeTool, fragranceTool)  // ✅ 添加到这里
        .build();
}
```

---

## 9. 安全模式下的 Tool 行为

| Tool | L1 正常 | L2 专注 | L3 静默 |
|------|---------|---------|---------|
| MusicTool | ✅ 正常 | ✅ 正常 | ✅ 仅响应指令 |
| LightTool | ✅ 动态效果 | ⚠️ 仅静态 | ❌ 禁用 |
| NarrativeTool | ✅ 正常音量 | ✅ 正常 | ⚠️ 音量 -30% |

```java
/**
 * 安全模式过滤器
 */
public class SafetyModeFilter {

    public LightSetting filter(LightSetting setting, SafetyMode mode) {
        return switch (mode) {
            case L3_SILENT -> null;  // 禁用灯光
            case L2_FOCUS -> setting.withMode(LightMode.STATIC);  // 仅静态
            case L1_NORMAL -> setting;
        };
    }

    public Narrative filter(Narrative narrative, SafetyMode mode) {
        return switch (mode) {
            case L3_SILENT -> narrative.withReducedVolume();
            default -> narrative;
        };
    }
}
```
