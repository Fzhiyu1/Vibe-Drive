# 阶段 4: Tool 层实现

## 状态

⚪ 待开始

## 目标

实现 Agent 可调用的三个核心工具：MusicTool、LightTool、NarrativeTool。

**重要**：基于重构后的设计，使用 `@Component` + `@Tool` 注解，无需接口和 Registry。

## 前置依赖

- [x] 阶段 2: 设计文档（含重构）
- [ ] 阶段 3: 数据模型实现（需要 Environment 和 AmbiencePlan 定义）
- [x] 参考：`docs/design/refactoring-guide.md` Phase 2

## 任务清单

### MusicTool（简化设计）

- [ ] 创建 `MusicTool.java` @Component 类
- [ ] 添加 `@Tool` 方法：
  ```java
  @Tool("根据用户情绪、时段和乘客数量推荐合适的音乐")
  public MusicRecommendation recommendMusic(
      @P("目标情绪") String mood,
      @P("时段") String timeOfDay,
      @P("乘客数量") int passengerCount,
      @P("偏好流派，可选") String genre
  )
  ```
- [ ] 注入 MusicService 依赖
- [ ] 实现筛选逻辑（根据 mood 匹配 BPM 和流派）
- [ ] 创建 Mock 曲库数据（JSON 文件，约 50-100 首歌）
- [ ] 编写单元测试

**预计工作量**：2-3 小时

### LightTool（简化设计）

- [ ] 创建 `LightTool.java` @Component 类
- [ ] 添加 `@Tool` 方法：
  ```java
  @Tool("根据情绪、时段和天气设置车内氛围灯")
  public LightSetting setLight(
      @P("目标情绪") String mood,
      @P("时段") String timeOfDay,
      @P("天气") String weather
  )
  ```
- [ ] 注入 LightService 依赖
- [ ] 定义氛围-灯光映射规则（Presets）：
  - 放松：暖黄色（2700K），亮度 30%
  - 活力：金色（4000K），亮度 60%
  - 专注：冷白色（5000K），亮度 70%
  - 浪漫：粉紫色，亮度 20%
- [ ] 实现安全模式过滤（L2/L3 模式禁用动态效果）
- [ ] 编写单元测试

**预计工作量**：2-3 小时

### NarrativeTool（简化设计）

- [ ] 创建 `NarrativeTool.java` @Component 类
- [ ] 添加 `@Tool` 方法：
  ```java
  @Tool("生成 TTS 播报文本，将窗外风景与音乐进行时空编织")
  public Narrative generateNarrative(
      @P("当前环境JSON") String environmentJson,
      @P("当前歌曲名称") String currentSong,
      @P("叙事主题") String theme
  )
  ```
- [ ] 注入 NarrativeService 依赖
- [ ] 实现叙事模板库：
  - 深夜 + 雨天：温暖陪伴型
  - 早晨 + 晴天：活力鼓励型
  - 傍晚 + 海滨：惬意浪漫型
- [ ] 集成 LLM 调用（可选，或使用模板）
- [ ] 编写单元测试

**预计工作量**：3-4 小时

### Mock 实现（用于测试和演示）

- [ ] 创建 `MockMusicTool.java` @Profile("mock")
- [ ] 创建 `MockLightTool.java` @Profile("mock")
- [ ] 创建 `MockNarrativeTool.java` @Profile("mock")
- [ ] 准备 Mock 数据（mock-songs.json）

**预计工作量**：1-2 小时

### ~~Tool 注册~~（已简化）

- [x] ~~创建 ToolConfig 配置类~~ → **不需要，AiServices 自动注册**
- [x] ~~将所有 Tool 注册到 Registry~~ → **不需要，框架自动扫描 @Tool**

**说明**：Tool 注册在阶段 5（Agent 编排）中通过 `AiServices.builder().tools(...)` 完成

## 相关文件

```
src/main/java/com/vibe/tool/
├── MusicTool.java          # 音乐推荐 Tool
├── LightTool.java          # 灯光控制 Tool
├── NarrativeTool.java      # 叙事生成 Tool
├── MockMusicTool.java      # Mock 实现（@Profile("mock")）
├── MockLightTool.java      # Mock 实现
└── MockNarrativeTool.java  # Mock 实现

src/main/java/com/vibe/service/
├── MusicService.java       # 音乐业务逻辑
├── LightService.java       # 灯光业务逻辑
└── NarrativeService.java   # 叙事业务逻辑

src/main/resources/mock-data/
└── mock-songs.json         # Mock 曲库数据
```

## 完成标准

- [ ] 所有 Tool 使用 @Component + @Tool 注解
- [ ] 所有 Tool 可被 LangChain4j Agent 正确调用
- [ ] 单元测试覆盖核心逻辑
- [ ] Mock 实现完成（用于演示）
- [ ] Mock 曲库数据准备完成
- [ ] 代码已提交到 Git

## 问题与笔记

### 重构要点

1. **无需定义接口**：直接使用 @Component 类
2. **无需 ToolRegistry**：AiServices 自动管理
3. **详细的 @Tool 描述**：LLM 会读取描述来决定何时调用
4. **清晰的 @P 参数说明**：包含可选值和类型

### 参考示例

查看 `docs/design/tool-interface.md` 获取完整的代码示例和最佳实践。
