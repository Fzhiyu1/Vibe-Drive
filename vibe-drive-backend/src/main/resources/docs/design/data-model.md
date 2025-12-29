# Vibe Drive 数据模型设计

## 1. 概述

本文档定义 Vibe Drive 系统中所有核心数据模型的结构和约束。

---

## 2. 环境数据模型 (Environment)

### 2.1 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Environment",
  "description": "车载环境感知数据",
  "type": "object",
  "required": ["gpsTag", "weather", "speed", "userMood", "timeOfDay", "passengerCount", "routeType"],
  "properties": {
    "gpsTag": {
      "type": "string",
      "description": "地理标签",
      "enum": ["highway", "tunnel", "bridge", "urban", "suburban", "mountain", "coastal", "parking"]
    },
    "weather": {
      "type": "string",
      "description": "天气状况",
      "enum": ["sunny", "cloudy", "rainy", "snowy", "foggy"]
    },
    "speed": {
      "type": "number",
      "description": "车速 (km/h)",
      "minimum": 0,
      "maximum": 200
    },
    "userMood": {
      "type": "string",
      "description": "用户情绪",
      "enum": ["happy", "calm", "tired", "stressed", "excited"]
    },
    "timeOfDay": {
      "type": "string",
      "description": "时段",
      "enum": ["dawn", "morning", "noon", "afternoon", "evening", "night", "midnight"]
    },
    "passengerCount": {
      "type": "integer",
      "description": "乘客数量",
      "minimum": 1,
      "maximum": 7
    },
    "routeType": {
      "type": "string",
      "description": "路线类型",
      "enum": ["highway", "urban", "mountain", "coastal", "tunnel"]
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "数据时间戳"
    }
  }
}
```

### 2.2 Java Record 定义

```java
import dev.langchain4j.model.output.structured.Description;

@Description("车载环境感知数据，包含地理位置、天气、车速、用户状态等信息")
public record Environment(
    @Description("地理标签，表示当前所在位置类型：highway/tunnel/bridge/urban/suburban/mountain/coastal/parking")
    GpsTag gpsTag,

    @Description("当前天气状况：sunny/cloudy/rainy/snowy/foggy")
    Weather weather,

    @Description("当前车速，单位 km/h，范围 0-200")
    double speed,

    @Description("用户情绪状态：happy/calm/tired/stressed/excited")
    UserMood userMood,

    @Description("时段：dawn/morning/noon/afternoon/evening/night/midnight")
    TimeOfDay timeOfDay,

    @Description("车内乘客数量，范围 1-7")
    int passengerCount,

    @Description("路线类型：highway/urban/mountain/coastal/tunnel")
    RouteType routeType,

    @Description("数据采集时间戳")
    Instant timestamp
) {
    public Environment {
        // 校验
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Speed must be between 0 and 200");
        }
        if (passengerCount < 1 || passengerCount > 7) {
            throw new IllegalArgumentException("Passenger count must be between 1 and 7");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * 获取当前安全模式
     */
    public SafetyMode getSafetyMode() {
        if (speed >= 100) return SafetyMode.L3_SILENT;
        if (speed >= 60) return SafetyMode.L2_FOCUS;
        return SafetyMode.L1_NORMAL;
    }
}
```

### 2.3 示例数据

```json
{
  "gpsTag": "highway",
  "weather": "rainy",
  "speed": 80,
  "userMood": "tired",
  "timeOfDay": "midnight",
  "passengerCount": 1,
  "routeType": "highway",
  "timestamp": "2025-12-23T23:30:00Z"
}
```

---

## 3. 氛围方案模型 (AmbiencePlan)

### 3.1 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AmbiencePlan",
  "description": "氛围编排方案",
  "type": "object",
  "required": ["id", "music", "light", "narrative", "safetyMode"],
  "properties": {
    "id": {
      "type": "string",
      "description": "方案唯一标识"
    },
    "music": {
      "$ref": "#/definitions/MusicRecommendation"
    },
    "light": {
      "$ref": "#/definitions/LightSetting"
    },
    "narrative": {
      "$ref": "#/definitions/Narrative"
    },
    "safetyMode": {
      "type": "string",
      "enum": ["L1_NORMAL", "L2_FOCUS", "L3_SILENT"]
    },
    "reasoning": {
      "type": "string",
      "description": "决策说明（简短说明原因，不输出逐步思维链）"
    },
    "createdAt": {
      "type": "string",
      "format": "date-time"
    }
  }
}
```

### 3.2 Java Record 定义

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
) {
    public AmbiencePlan {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * 根据安全模式过滤输出
     */
    public AmbiencePlan applyingSafetyFilter() {
        return switch (safetyMode) {
            case L3_SILENT -> new AmbiencePlan(
                id, music, null, narrative.withReducedVolume(), safetyMode, reasoning, createdAt
            );
            case L2_FOCUS -> new AmbiencePlan(
                id, music, light != null ? light.forFocusMode() : null, narrative, safetyMode, reasoning, createdAt
            );
            case L1_NORMAL -> this;
        };
    }
}
```

---

## 4. 音乐推荐模型 (MusicRecommendation)

### 4.1 JSON Schema

```json
{
  "title": "MusicRecommendation",
  "type": "object",
  "required": ["songs", "mood", "genre"],
  "properties": {
    "songs": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/Song"
      },
      "minItems": 1,
      "maxItems": 10
    },
    "mood": {
      "type": "string",
      "description": "目标情绪"
    },
    "genre": {
      "type": "string",
      "description": "音乐流派"
    },
    "bpmRange": {
      "type": "object",
      "properties": {
        "min": { "type": "integer" },
        "max": { "type": "integer" }
      }
    }
  }
}
```

### 4.2 Song 模型

```json
{
  "title": "Song",
  "type": "object",
  "required": ["id", "title", "artist"],
  "properties": {
    "id": {
      "type": "string",
      "description": "歌曲 ID"
    },
    "title": {
      "type": "string",
      "description": "歌曲名称"
    },
    "artist": {
      "type": "string",
      "description": "艺术家"
    },
    "album": {
      "type": "string",
      "description": "专辑名称"
    },
    "duration": {
      "type": "integer",
      "description": "时长（秒）"
    },
    "bpm": {
      "type": "integer",
      "description": "节拍速度"
    },
    "genre": {
      "type": "string",
      "description": "流派"
    },
    "coverUrl": {
      "type": "string",
      "format": "uri",
      "description": "封面图片 URL"
    }
  }
}
```

### 4.3 Java Record 定义

```java
import dev.langchain4j.model.output.structured.Description;

@Description("音乐推荐结果，包含歌曲列表和元数据")
public record MusicRecommendation(
    @Description("推荐的歌曲列表，1-10首")
    List<Song> songs,

    @Description("目标情绪标签")
    String mood,

    @Description("音乐流派")
    String genre,

    @Description("推荐歌曲的BPM（节拍速度）范围")
    BpmRange bpmRange
) {}

@Description("歌曲信息")
public record Song(
    @Description("歌曲唯一标识")
    String id,

    @Description("歌曲名称")
    String title,

    @Description("艺术家/演唱者")
    String artist,

    @Description("专辑名称")
    String album,

    @Description("时长，单位秒")
    int duration,

    @Description("节拍速度 BPM（Beats Per Minute）")
    int bpm,

    @Description("音乐流派：pop/rock/jazz/classical等")
    String genre,

    @Description("封面图片URL")
    String coverUrl
) {}

@Description("BPM（节拍速度）范围")
public record BpmRange(
    @Description("最小BPM值")
    int min,

    @Description("最大BPM值")
    int max
) {
    public BpmRange {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Invalid BPM range");
        }
    }
}
```

---

## 5. 灯光设置模型 (LightSetting)

### 5.1 JSON Schema

```json
{
  "title": "LightSetting",
  "type": "object",
  "required": ["color", "brightness", "mode"],
  "properties": {
    "color": {
      "type": "object",
      "properties": {
        "hex": {
          "type": "string",
          "pattern": "^#[0-9A-Fa-f]{6}$"
        },
        "temperature": {
          "type": "integer",
          "description": "色温 (K)",
          "minimum": 2700,
          "maximum": 6500
        }
      }
    },
    "brightness": {
      "type": "integer",
      "description": "亮度 (0-100)",
      "minimum": 0,
      "maximum": 100
    },
    "mode": {
      "type": "string",
      "description": "灯光模式",
      "enum": ["static", "breathing", "gradient", "pulse"]
    },
    "transitionDuration": {
      "type": "integer",
      "description": "过渡时长（毫秒）",
      "default": 1000
    },
    "zones": {
      "type": "array",
      "description": "分区设置（可选）",
      "items": {
        "type": "object",
        "properties": {
          "zone": { "type": "string" },
          "color": { "type": "string" },
          "brightness": { "type": "integer" }
        }
      }
    }
  }
}
```

### 5.2 Java Record 定义

```java
import dev.langchain4j.model.output.structured.Description;

@Description("氛围灯设置，包含颜色、亮度和动态效果")
public record LightSetting(
    @Description("灯光颜色和色温")
    LightColor color,

    @Description("亮度，范围 0-100")
    int brightness,

    @Description("灯光模式：static（静态）/breathing（呼吸）/gradient（渐变）/pulse（脉冲）")
    LightMode mode,

    @Description("颜色过渡时长，单位毫秒")
    int transitionDuration,

    @Description("分区设置，可选，用于多区域灯光控制")
    List<ZoneSetting> zones
) {
    public LightSetting {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        }
        if (transitionDuration <= 0) {
            transitionDuration = 1000;
        }
    }
}

@Description("灯光颜色配置")
public record LightColor(
    @Description("十六进制颜色代码，格式 #RRGGBB")
    String hex,

    @Description("色温，单位开尔文（K），范围 2700-6500")
    Integer temperature
) {
    public static LightColor warmWhite() {
        return new LightColor("#FFE4B5", 2700);
    }

    public static LightColor coolWhite() {
        return new LightColor("#F0F8FF", 6500);
    }
}

public enum LightMode {
    STATIC,      // 静态
    BREATHING,   // 呼吸
    GRADIENT,    // 渐变
    PULSE        // 脉冲
}

@Description("分区灯光设置，用于车内不同区域的独立灯光控制")
public record ZoneSetting(
    @Description("区域名称，如 dashboard/door/roof")
    String zone,

    @Description("该区域的颜色，十六进制格式 #RRGGBB")
    String color,

    @Description("该区域的亮度，范围 0-100")
    int brightness
) {}
```

---

## 6. 叙事文本模型 (Narrative)

### 6.1 JSON Schema

```json
{
  "title": "Narrative",
  "type": "object",
  "required": ["text"],
  "properties": {
    "text": {
      "type": "string",
      "description": "TTS 播报文本",
      "maxLength": 500
    },
    "voice": {
      "type": "string",
      "description": "语音角色",
      "default": "default"
    },
    "speed": {
      "type": "number",
      "description": "语速 (0.5-2.0)",
      "minimum": 0.5,
      "maximum": 2.0,
      "default": 1.0
    },
    "volume": {
      "type": "number",
      "description": "音量 (0-1)",
      "minimum": 0,
      "maximum": 1,
      "default": 0.8
    },
    "emotion": {
      "type": "string",
      "description": "情感色彩",
      "enum": ["neutral", "warm", "energetic", "calm", "gentle"]
    }
  }
}
```

### 6.2 Java Record 定义

```java
import dev.langchain4j.model.output.structured.Description;

@Description("叙事文本，用于TTS语音播报，将环境与音乐进行时空编织")
public record Narrative(
    @Description("播报文本内容，应简短温馨，不超过50字")
    String text,

    @Description("语音角色ID，默认为 default")
    String voice,

    @Description("语速，范围 0.5-2.0，1.0为正常速度")
    double speed,

    @Description("音量，范围 0-1，L3静默模式下会自动降低30%")
    double volume,

    @Description("情感色彩：neutral（中性）/warm（温暖）/energetic（活力）/calm（平静）/gentle（轻柔）")
    NarrativeEmotion emotion
) {
    public Narrative {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Narrative text cannot be empty");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Narrative text too long");
        }
        if (voice == null) voice = "default";
        if (speed <= 0) speed = 1.0;
        if (volume <= 0) volume = 0.8;
        if (emotion == null) emotion = NarrativeEmotion.NEUTRAL;
    }

    /**
     * 降低音量（用于 L3 静默模式）
     */
    public Narrative withReducedVolume() {
        return new Narrative(text, voice, speed, volume * 0.7, emotion);
    }
}

public enum NarrativeEmotion {
    NEUTRAL,    // 中性
    WARM,       // 温暖
    ENERGETIC,  // 活力
    CALM,       // 平静
    GENTLE      // 轻柔
}
```

---

## 7. 枚举类型定义

### 7.1 地理标签 (GpsTag)

```java
public enum GpsTag {
    HIGHWAY("高速公路"),
    TUNNEL("隧道"),
    BRIDGE("高架桥"),
    URBAN("城市道路"),
    SUBURBAN("郊区"),
    MOUNTAIN("山路"),
    COASTAL("海滨"),
    PARKING("停车场");

    private final String displayName;

    GpsTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 7.2 天气 (Weather)

```java
public enum Weather {
    SUNNY("晴天"),
    CLOUDY("阴天"),
    RAINY("雨天"),
    SNOWY("雪天"),
    FOGGY("雾天");

    private final String displayName;

    Weather(String displayName) {
        this.displayName = displayName;
    }
}
```

### 7.3 用户情绪 (UserMood)

```java
public enum UserMood {
    HAPPY("开心"),
    CALM("平静"),
    TIRED("疲劳"),
    STRESSED("压力"),
    EXCITED("兴奋");

    private final String displayName;

    UserMood(String displayName) {
        this.displayName = displayName;
    }
}
```

### 7.4 时段 (TimeOfDay)

```java
public enum TimeOfDay {
    DAWN("黎明", 5, 7),
    MORNING("上午", 7, 12),
    NOON("中午", 12, 14),
    AFTERNOON("下午", 14, 18),
    EVENING("傍晚", 18, 20),
    NIGHT("夜晚", 20, 23),
    MIDNIGHT("深夜", 23, 5);

    private final String displayName;
    private final int startHour;
    private final int endHour;

    TimeOfDay(String displayName, int startHour, int endHour) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public static TimeOfDay fromHour(int hour) {
        for (TimeOfDay tod : values()) {
            if (tod.startHour <= tod.endHour) {
                if (hour >= tod.startHour && hour < tod.endHour) return tod;
            } else {
                if (hour >= tod.startHour || hour < tod.endHour) return tod;
            }
        }
        return MIDNIGHT;
    }
}
```

### 7.5 路线类型 (RouteType)

```java
public enum RouteType {
    HIGHWAY("高速"),
    URBAN("城市"),
    MOUNTAIN("山路"),
    COASTAL("海滨"),
    TUNNEL("隧道");

    private final String displayName;

    RouteType(String displayName) {
        this.displayName = displayName;
    }
}
```

### 7.6 安全模式 (SafetyMode)

```java
public enum SafetyMode {
    L1_NORMAL("正常模式", 0, 60),
    L2_FOCUS("专注模式", 60, 100),
    L3_SILENT("静默模式", 100, 200);

    private final String displayName;
    private final int minSpeed;
    private final int maxSpeed;

    SafetyMode(String displayName, int minSpeed, int maxSpeed) {
        this.displayName = displayName;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    public static SafetyMode fromSpeed(double speed) {
        if (speed >= 100) return L3_SILENT;
        if (speed >= 60) return L2_FOCUS;
        return L1_NORMAL;
    }

    public boolean allowsVisualEffects() {
        return this == L1_NORMAL;
    }

    public boolean allowsProactiveRecommendation() {
        return this != L3_SILENT;
    }
}
```

---

## 8. 智能体状态模型

### 8.1 Vibe Agent 状态

```java
public record VibeAgentStatus(
    boolean running,
    SafetyMode currentSafetyMode,
    AmbiencePlan currentPlan,
    Environment lastEnvironment,
    Instant lastUpdateTime,
    int totalPlansGenerated
) {
    public boolean isIdle() {
        return running && currentPlan == null;
    }
}
```

### 8.2 Vibe 通知

```java
public record VibeNotification(
    String id,
    NotificationType type,
    AmbiencePlan plan,
    String summary,
    Instant timestamp
) {}

public enum NotificationType {
    AMBIENCE_CHANGED,    // 氛围已切换
    SAFETY_MODE_CHANGED, // 安全模式变化
    AGENT_STARTED,       // Agent 启动
    AGENT_STOPPED        // Agent 停止
}
```

### 8.3 Vibe 命令

```java
public sealed interface VibeCommand {
    record Start() implements VibeCommand {}
    record Stop() implements VibeCommand {}
    record SetSafetyMode(SafetyMode mode) implements VibeCommand {}
    record ForceRefresh() implements VibeCommand {}
    record SetPreference(String key, Object value) implements VibeCommand {}
}
```

---

## 9. 会话状态模型

### 9.1 会话状态

```java
public class ConversationState {
    private String id;
    private List<ChatMessage> messages;
    private Map<String, Object> metadata;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 创建快照（Fork）
     */
    public ConversationState fork(String branchId) {
        ConversationState branch = new ConversationState();
        branch.id = branchId;
        branch.messages = new ArrayList<>(this.messages);
        branch.metadata = new HashMap<>(this.metadata);
        branch.version = this.version;
        branch.createdAt = Instant.now();
        return branch;
    }

    /**
     * 获取自某版本以来的新消息
     */
    public List<ChatMessage> getMessagesSince(long sinceVersion) {
        // 实现略
    }
}
```

### 9.2 合并结果

```java
public sealed interface MergeResult {
    record Success(List<ChatMessage> mergedMessages) implements MergeResult {}
    record Conflict(String reason, ConversationState branch) implements MergeResult {}
    record Discarded(String reason) implements MergeResult {}
}
```

---

## 10. API 请求/响应模型

### 10.1 分析请求

```java
public record AnalyzeRequest(
    @Description("会话 ID（对应 LangChain4j @MemoryId）")
    String sessionId,

    @Description("环境数据")
    Environment environment,

    @Description("用户偏好（可选）")
    Map<String, Object> preferences,

    @Description("是否异步处理（默认 false）")
    boolean async
) {}
```

### 10.2 分析响应（增强版，包含执行元数据）

**重要变更**：添加了 `tokenUsage` 和 `toolExecutions` 字段，用于成本监控和性能分析。

```java
public enum AnalyzeAction {
    APPLY,
    NO_ACTION
}

@Description("分析响应，包含动作决策、氛围方案和执行元数据")
public record AnalyzeResponse(
    @Description("动作：APPLY（应用新方案）/NO_ACTION（本次不更新）")
    AnalyzeAction action,

    @Description("可选提示信息（通常在 NO_ACTION 时返回原因）")
    String message,

    @Description("生成的氛围方案")
    AmbiencePlan plan,

    @Description("Token 使用统计，用于成本监控")
    TokenUsageInfo tokenUsage,

    @Description("工具执行详情，用于性能分析")
    List<ToolExecutionInfo> toolExecutions,

    @Description("处理耗时（毫秒）")
    long processingTimeMs
) {
    public static AnalyzeResponse applied(AmbiencePlan plan,
                                          TokenUsageInfo tokenUsage,
                                          List<ToolExecutionInfo> toolExecutions,
                                          long timeMs) {
        return new AnalyzeResponse(AnalyzeAction.APPLY, null, plan, tokenUsage, toolExecutions, timeMs);
    }

    public static AnalyzeResponse noAction(String message) {
        return new AnalyzeResponse(AnalyzeAction.NO_ACTION, message, null, null, List.of(), 0);
    }
}
```

### 10.3 Token 使用统计模型

```java
@Description("Token 使用统计，用于成本监控和分析")
public record TokenUsageInfo(
    @Description("输入 Token 数量")
    Integer inputTokenCount,

    @Description("输出 Token 数量")
    Integer outputTokenCount,

    @Description("总 Token 数量")
    Integer totalTokenCount
) {
    /**
     * 从 LangChain4j TokenUsage 转换
     */
    public static TokenUsageInfo from(dev.langchain4j.model.output.TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return null;
        }
        return new TokenUsageInfo(
            tokenUsage.inputTokenCount(),
            tokenUsage.outputTokenCount(),
            tokenUsage.totalTokenCount()
        );
    }

    /**
     * 估算成本（基于 OpenAI GPT-4o 定价）
     */
    public double estimateCost() {
        if (inputTokenCount == null || outputTokenCount == null) {
            return 0.0;
        }
        // GPT-4o: $2.50 / 1M input tokens, $10.00 / 1M output tokens
        double inputCost = (inputTokenCount / 1_000_000.0) * 2.50;
        double outputCost = (outputTokenCount / 1_000_000.0) * 10.00;
        return inputCost + outputCost;
    }
}
```

### 10.4 工具执行详情模型

```java
@Description("工具执行详情，用于性能分析和调试")
public record ToolExecutionInfo(
    @Description("工具名称")
    String toolName,

    @Description("执行参数（JSON 格式）")
    String arguments,

    @Description("执行结果（JSON 格式）")
    String result,

    @Description("执行耗时（毫秒）")
    Long durationMs,

    @Description("是否执行成功")
    boolean success,

    @Description("错误信息，仅在失败时存在")
    String error
) {
    /**
     * 从 LangChain4j ToolExecution 转换
     */
    public static ToolExecutionInfo from(dev.langchain4j.agent.tool.ToolExecution execution) {
        return new ToolExecutionInfo(
            execution.toolName(),
            execution.arguments(),
            execution.result(),
            execution.duration() != null ? execution.duration().toMillis() : null,
            true,  // LangChain4j ToolExecution 表示成功的执行
            null
        );
    }

    public static ToolExecutionInfo error(String toolName, String arguments, String error) {
        return new ToolExecutionInfo(toolName, arguments, null, null, false, error);
    }
}
```

### 10.5 使用 Result<T> 获取执行元数据

**AI Service 定义（使用 Result<T>）**：

```java
import dev.langchain4j.service.Result;

public interface VibeAgent {

    @SystemMessage(fromResource = "prompts/vibe-system.txt")
    @UserMessage("""
        请分析以下车载环境数据，并编排合适的氛围方案：
        {{environment}}
        """)
    Result<AmbiencePlan> analyzeEnvironment(
        @MemoryId String sessionId,
        @V("environment") String environmentJson,
        @V("preferences") String preferencesJson
    );  // ✅ 返回 Result<T> 包含元数据
}
```

**获取元数据示例**：

```java
@Service
public class VibeService {

    private final VibeAgent vibeAgent;
    private final ObjectMapper objectMapper;

    public AnalyzeResponse analyze(String sessionId, Environment environment, Map<String, Object> preferences) {
        long startTime = System.currentTimeMillis();

        // 调用 Agent，获取 Result
        Result<AmbiencePlan> result = vibeAgent.analyzeEnvironment(
            sessionId,
            toJson(environment),
            preferences == null ? null : toJson(preferences)
        );

        // 提取各项元数据
        AmbiencePlan plan = result.content();                           // 结果内容
        TokenUsage tokenUsage = result.tokenUsage();                    // Token 使用量
        List<ToolExecution> toolExecutions = result.toolExecutions();   // Tool 执行详情
        FinishReason finishReason = result.finishReason();              // 完成原因

        // 记录日志
        logger.info("Token usage: input={}, output={}, total={}",
            tokenUsage.inputTokenCount(),
            tokenUsage.outputTokenCount(),
            tokenUsage.totalTokenCount());

        for (ToolExecution exec : toolExecutions) {
            logger.info("Tool: {}, Duration: {}ms",
                exec.toolName(),
                exec.duration().toMillis());
        }

        // 转换为响应模型
        long processingTime = System.currentTimeMillis() - startTime;
        return AnalyzeResponse.applied(
            plan,
            TokenUsageInfo.from(tokenUsage),
            toolExecutions.stream().map(ToolExecutionInfo::from).toList(),
            processingTime
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

**配置 strictJsonSchema（推荐）**：

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
    public VibeAgent vibeAgent(ChatLanguageModel chatModel,
                                MusicTool musicTool,
                                LightTool lightTool,
                                NarrativeTool narrativeTool) {
        return AiServices.builder(VibeAgent.class)
            .chatLanguageModel(chatModel)
            .tools(musicTool, lightTool, narrativeTool)
            .build();
    }
}
```

**优势**：
- ✅ **成本监控**：实时跟踪 Token 使用量，计算 API 成本
- ✅ **性能分析**：了解每个 Tool 的执行时间，优化性能瓶颈
- ✅ **调试支持**：快速定位问题，查看完整的执行链路
- ✅ **审计追踪**：记录每次请求的完整元数据

---

## 11. 模型关系图

```
┌─────────────────┐
│   Environment   │
│   (环境输入)     │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│   VibeAgent     │
│   (处理)        │
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────────────────────┐
│                   AmbiencePlan                      │
│  ┌─────────────────────────────────────────────┐   │
│  │ ┌───────────────┐ ┌───────────┐ ┌─────────┐ │   │
│  │ │MusicRecommend │ │LightSetting│ │Narrative│ │   │
│  │ │ ┌───────────┐ │ │           │ │         │ │   │
│  │ │ │  Song[]   │ │ │           │ │         │ │   │
│  │ │ └───────────┘ │ │           │ │         │ │   │
│  │ └───────────────┘ └───────────┘ └─────────┘ │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## 12. SSE 流式事件模型

本章定义 SSE (Server-Sent Events) 流式输出相关的数据模型，用于支持 LangChain4j TokenStream 的实时推送。

### 12.1 流式分析事件

#### TokenEvent - Token 输出事件

```java
@Description("LLM 输出的 token 事件")
public record TokenEvent(
    @Description("token 内容")
    String content,

    @Description("事件时间戳")
    Instant timestamp
) {
    public TokenEvent(String content) {
        this(content, Instant.now());
    }
}
```

#### ToolStartEvent - Tool 开始执行事件

```java
@Description("Tool 开始执行事件")
public record ToolStartEvent(
    @Description("工具名称")
    String toolName,

    @Description("执行参数（JSON 格式）")
    String arguments,

    @Description("事件时间戳")
    Instant timestamp
) {
    public ToolStartEvent(String toolName, String arguments) {
        this(toolName, arguments, Instant.now());
    }
}
```

#### ToolEndEvent - Tool 执行完成事件

```java
@Description("Tool 执行完成事件")
public record ToolEndEvent(
    @Description("工具名称")
    String toolName,

    @Description("执行耗时（毫秒）")
    long durationMs,

    @Description("是否执行成功")
    boolean success,

    @Description("错误信息，仅在失败时存在")
    String error,

    @Description("事件时间戳")
    Instant timestamp
) {
    public ToolEndEvent(String toolName, long durationMs, boolean success) {
        this(toolName, durationMs, success, null, Instant.now());
    }

    public static ToolEndEvent error(String toolName, String error) {
        return new ToolEndEvent(toolName, 0, false, error, Instant.now());
    }
}
```

#### complete - 流式完成事件

`complete` 事件的 data 直接复用 `AnalyzeResponse`（见 10.2，与 `POST /vibe/analyze` 的 `data` 一致），避免维护两份 DTO。

#### ErrorEvent - 错误事件

```java
@Description("错误事件")
public record ErrorEvent(
    @Description("错误码")
    String code,

    @Description("错误信息")
    String message,

    @Description("事件时间戳")
    Instant timestamp
) {
    public ErrorEvent(String code, String message) {
        this(code, message, Instant.now());
    }
}
```

### 12.2 实时推送事件

#### AmbienceChangedEvent - 氛围变化事件

```java
@Description("氛围方案变化事件")
public record AmbienceChangedEvent(
    @Description("方案 ID")
    String planId,

    @Description("音乐推荐")
    MusicRecommendation music,

    @Description("灯光设置")
    LightSetting light,

    @Description("叙事文本")
    Narrative narrative,

    @Description("当前安全模式")
    SafetyMode safetyMode,

    @Description("触发原因：environment_change/user_request/scheduled")
    String trigger,

    @Description("事件时间戳")
    Instant timestamp
) {
    public AmbienceChangedEvent(AmbiencePlan plan, String trigger) {
        this(plan.id(), plan.music(), plan.light(), plan.narrative(),
             plan.safetyMode(), trigger, Instant.now());
    }
}
```

#### SafetyModeChangedEvent - 安全模式变化事件

```java
@Description("安全模式变化事件")
public record SafetyModeChangedEvent(
    @Description("之前的安全模式")
    SafetyMode previousMode,

    @Description("当前安全模式")
    SafetyMode currentMode,

    @Description("当前车速")
    double speed,

    @Description("事件时间戳")
    Instant timestamp
) {
    public SafetyModeChangedEvent(SafetyMode previousMode, SafetyMode currentMode, double speed) {
        this(previousMode, currentMode, speed, Instant.now());
    }
}
```

#### AgentStatusChangedEvent - Agent 状态变化事件

```java
@Description("Agent 状态变化事件")
public record AgentStatusChangedEvent(
    @Description("Agent 是否运行中")
    boolean running,

    @Description("状态变化事件：started/stopped/error")
    String event,

    @Description("错误信息，仅在 event=error 时存在")
    String error,

    @Description("事件时间戳")
    Instant timestamp
) {
    public static AgentStatusChangedEvent started() {
        return new AgentStatusChangedEvent(true, "started", null, Instant.now());
    }

    public static AgentStatusChangedEvent stopped() {
        return new AgentStatusChangedEvent(false, "stopped", null, Instant.now());
    }

    public static AgentStatusChangedEvent error(String error) {
        return new AgentStatusChangedEvent(false, "error", error, Instant.now());
    }
}
```

#### EnvironmentUpdateEvent - 环境数据更新事件

```java
@Description("环境数据更新事件（通常来自模拟器或车端采集）")
public record EnvironmentUpdateEvent(
    @Description("位置标签")
    GpsTag gpsTag,

    @Description("天气")
    Weather weather,

    @Description("车速（km/h）")
    double speed,

    @Description("事件时间戳")
    Instant timestamp
) {
    public EnvironmentUpdateEvent(GpsTag gpsTag, Weather weather, double speed) {
        this(gpsTag, weather, speed, Instant.now());
    }
}
```

#### HeartbeatEvent - 心跳事件

```java
@Description("心跳事件，用于保持 SSE 连接活跃")
public record HeartbeatEvent(
    @Description("事件时间戳")
    Instant timestamp
) {
    public HeartbeatEvent() {
        this(Instant.now());
    }
}
```

### 12.3 SSE 事件类型汇总

| 事件类型 | Record 类 | 用途 |
|----------|-----------|------|
| `token` | `TokenEvent` | LLM 输出的 token（调试事件） |
| `tool_start` | `ToolStartEvent` | Tool 开始执行（调试事件） |
| `tool_end` | `ToolEndEvent` | Tool 执行完成（调试事件） |
| `complete` | `AnalyzeResponse` | 流式分析完成（最终结果） |
| `error` | `ErrorEvent` | 发生错误 |
| `ambience_changed` | `AmbienceChangedEvent` | 氛围方案变化 |
| `safety_mode_changed` | `SafetyModeChangedEvent` | 安全模式变化 |
| `agent_status_changed` | `AgentStatusChangedEvent` | Agent 状态变化 |
| `environment_update` | `EnvironmentUpdateEvent` | 环境数据更新 |
| `heartbeat` | `HeartbeatEvent` | 心跳保活 |

### 12.4 SSE 事件 JSON 示例

**token 事件**
```json
{"content": "正在分析深夜雨天场景", "timestamp": "2025-12-23T23:30:00Z"}
```

**tool_start 事件**
```json
{"toolName": "recommendMusic", "arguments": "{\"mood\":\"calm\"}", "timestamp": "2025-12-23T23:30:00Z"}
```

**tool_end 事件**
```json
{"toolName": "recommendMusic", "durationMs": 120, "success": true, "error": null, "timestamp": "2025-12-23T23:30:00Z"}
```

**complete 事件**
```json
{
  "action": "APPLY",
  "message": null,
  "plan": { "id": "plan_001", "music": {...}, "light": {...}, "narrative": {...} },
  "tokenUsage": { "inputTokenCount": 1234, "outputTokenCount": 567, "totalTokenCount": 1801 },
  "toolExecutions": [...],
  "processingTimeMs": 1850
}
```

**ambience_changed 事件**
```json
{
  "planId": "plan_001",
  "music": {...},
  "light": {...},
  "narrative": {...},
  "safetyMode": "L1_NORMAL",
  "trigger": "environment_change",
  "timestamp": "2025-12-23T23:30:00Z"
}
```

**environment_update 事件**
```json
{
  "gpsTag": "highway",
  "weather": "rainy",
  "speed": 80,
  "timestamp": "2025-12-23T23:30:00Z"
}
```

**heartbeat 事件**
```json
{"timestamp": "2025-12-23T23:30:00Z"}
```
