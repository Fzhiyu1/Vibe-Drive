# 阶段 4: Tool 层实现

## 状态

🟢 已完成 (100%)

## 目标

实现 Agent 可调用的三个核心工具：MusicTool、LightTool、NarrativeTool。

**重要**：基于重构后的设计，使用 `@Component` + `@Tool` 注解，无需接口和 Registry。

## 前置依赖

- [x] 阶段 2: 设计文档（含重构）
- [x] 阶段 3: 数据模型实现（需要 Environment 和 AmbiencePlan 定义）
- [x] 参考：`docs/design/refactoring-guide.md` Phase 2

## 任务清单

### MusicTool ✅

- [x] 创建 `MusicTool.java` @Component 类
- [x] 添加 `@Tool` 方法
- [x] 注入 MusicService 依赖
- [x] 实现筛选逻辑（根据 mood 匹配 BPM 和流派）
- [x] 创建 Mock 曲库数据（mock-songs.json，30首歌）
- [x] 编写单元测试

### LightTool ✅

- [x] 创建 `LightTool.java` @Component 类
- [x] 添加 `@Tool` 方法
- [x] 注入 LightService 依赖
- [x] 定义氛围-灯光映射规则（LightPresets）
- [x] 实现安全模式过滤（L2/L3 模式禁用动态效果）
- [x] 编写单元测试

### NarrativeTool ✅

- [x] 创建 `NarrativeTool.java` @Component 类
- [x] 添加 `@Tool` 方法（参数已优化为简单类型）
- [x] 注入 NarrativeService 依赖
- [x] 实现叙事模板库（NarrativeTemplates）
- [x] 编写单元测试

### Service 层 ✅

- [x] MusicService - 曲库加载 + 推荐算法
- [x] LightService - 预设映射 + 安全过滤
- [x] NarrativeService - 模板选择 + 文本生成

### 辅助类 ✅

- [x] LightPresets - 灯光预设常量
- [x] NarrativeTemplates - 叙事模板库

### ~~Mock Tool 实现~~（已简化）

- [x] ~~创建 MockMusicTool.java~~ → **不需要，Service 层已包含完整逻辑**
- [x] ~~创建 MockLightTool.java~~ → **不需要**
- [x] ~~创建 MockNarrativeTool.java~~ → **不需要**
- [x] 准备 Mock 数据（mock-songs.json）✅

**说明**：采用简化设计，Tool 层委托给 Service 层，Service 层包含完整业务逻辑，无需单独的 Mock Tool。

## 相关文件

```
src/main/java/com/vibe/
├── tool/                       # Tool 层（3个）
│   ├── MusicTool.java
│   ├── LightTool.java
│   └── NarrativeTool.java
├── service/                    # Service 层（3个）
│   ├── MusicService.java
│   ├── LightService.java
│   └── NarrativeService.java
└── support/                    # 辅助类（2个）
    ├── LightPresets.java
    └── NarrativeTemplates.java

src/main/resources/mock-data/
└── mock-songs.json             # Mock 曲库数据（30首歌）

src/test/java/com/vibe/
├── service/                    # Service 测试（3个）
│   ├── MusicServiceTest.java
│   ├── LightServiceTest.java
│   └── NarrativeServiceTest.java
└── tool/                       # Tool 集成测试
    └── ToolIntegrationTest.java
```

## 完成标准

- [x] 所有 Tool 使用 @Component + @Tool 注解 ✅
- [x] 所有 Tool 可被 LangChain4j Agent 正确调用 ✅
- [x] 单元测试覆盖核心逻辑 ✅ (43个新测试，共166个测试通过)
- [x] Mock 曲库数据准备完成 ✅
- [ ] 代码已提交到 Git（待用户指令）

## 问题与笔记

### 2025-12-23: 设计优化

1. **NarrativeTool 参数优化**：将 `environmentJson` 拆分为简单参数（timeOfDay, weather, gpsTag, userMood），对 LLM 更友好
2. **简化 Mock 策略**：不创建单独的 Mock Tool，Service 层直接包含完整逻辑
3. **forFocusMode() 修复**：L2_FOCUS 模式强制使用 STATIC，而不仅仅过滤 isDynamic() 的模式

### 统计

- Tool 类：3 个
- Service 类：3 个
- 辅助类：2 个
- 曲库数据：30 首歌
- 新增测试：43 个
- 总测试数：166 个（全部通过）
