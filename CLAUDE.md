# Vibe Drive 项目规则

## 项目概述

Vibe Drive 是一个基于 AI Agent 的车载智能氛围编排系统，通过环境感知驱动"时空叙事"体验。

## Plan 文档规则

### 开始工作前

每次开始新的开发任务时，**必须先读取计划文档**：

1. 首先读取 `docs/plan/index.md` 了解整体进度和当前阶段
2. 根据当前阶段，读取对应的阶段文档（如 `docs/plan/阶段2-设计文档.md`）
3. 确认当前任务后再开始工作

### 工作过程中

1. 开始某个任务时，将对应的 `- [ ]` 保持不变，但在阶段文档中将状态改为 `🟡 进行中`
2. 完成任务后，立即将 `- [ ]` 改为 `- [x]`
3. 遇到问题时，记录在对应阶段文档的「问题与笔记」部分

### 完成阶段后

1. 将阶段文档的状态改为 `🟢 已完成`
2. 更新 `docs/plan/index.md` 中的进度百分比和状态
3. 更新「最近更新」记录

### 状态标记规范

| 标记 | 含义 |
|------|------|
| ⚪ | 待开始 |
| 🟡 | 进行中 |
| 🟢 | 已完成 |
| 🔴 | 阻塞/有问题 |

### 计划文档结构

```
docs/plan/
├── index.md                 # 主索引（必读）
├── 阶段1-项目初始化.md
├── 阶段2-设计文档.md
├── 阶段3-数据模型实现.md
├── 阶段4-Tool层实现.md
├── 阶段5-Agent编排.md
├── 阶段6-API与Mock.md
└── 阶段7-前端开发.md
```

## 代码规范

### 后端 (Java)

- 使用 **Java 21**（LTS 版本）
- 充分利用 Java 21 特性（Virtual Threads、Record Patterns、Pattern Matching for switch 等）
- 遵循 Spring Boot 3.x 最佳实践
- 包结构：`com.vibe.{module}`

### 前端 (React)

- 使用 TypeScript
- 组件使用函数式组件 + Hooks
- 状态管理使用 Zustand 或 React Context

## Git 提交规范

- 不添加 AI 工具署名
- 提交信息简洁明了
- 格式：`<type>: <description>`
  - feat: 新功能
  - fix: 修复
  - docs: 文档
  - refactor: 重构
  - test: 测试

## 重要文档

- 需求规格说明书：`docs/需求规格说明书.md`
- 开发计划：`docs/plan/index.md`
