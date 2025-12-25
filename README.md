# Vibe Drive

基于 AI Agent 的车载智能氛围编排系统，通过环境感知驱动"时空叙事"体验。

> 让 AI 赋予百万辆车有趣的灵魂

## 项目简介

Vibe Drive 是一个通用智能体（General-purpose Agent）架构的车载空间自适应编排引擎。它打破传统"点播-播放"的单向模式，利用 LLM 的语义推理能力，实现环境感知驱动的内容编排。

### 核心特性

- **环境感知**：支持 GPS 标签、天气、车速、用户情绪、时段等多维度环境数据
- **语义理解**：利用 LLM 识别环境背后的情感意图，而非简单的规则匹配
- **跨模态编排**：同时调度音乐、灯光、香氛、TTS 等多个模块
- **安全优先**：根据车速自动调整交互模式（正常/专注/静默）

### 典型场景

| 场景 | 环境 | Agent 响应 |
|------|------|-----------|
| 深夜归途 | 午夜 + 雨天 + 高架桥 + 疲劳 | 舒缓爵士乐 + 暖黄氛围灯 + 轻柔按摩 |
| 周末出游 | 早晨 + 晴天 + 郊区 + 4人 + 开心 | 欢快流行乐 + 活力蓝绿灯效 |
| 通勤早高峰 | 早晨 + 阴天 + 闹市区 + 压力 | 轻音乐 + 柔和白光 + 薄荷香氛 |

## 技术栈

### 后端

- **Java 21** (LTS)
- **Spring Boot 3.4.x**
- **LangChain4j 0.36.x** - AI Agent 框架
- **SpringDoc OpenAPI** - API 文档

### 微服务

- **Go 1.21+** - 音乐 API 微服务
- **go-musicfox/netease-music** - 网易云音乐 API

### 前端

- **Vue 3** + **TypeScript**
- **Three.js** / **TresJS** - 3D 可视化
- **Vite** - 构建工具
- **Pinia** - 状态管理

### AI 服务

- OpenAI GPT-4 / Claude 3.5 Sonnet（支持 Function Calling）

## 项目结构

```
Vibe-Drive/
├── vibe-drive-backend/       # Java 后端服务
│   ├── src/main/java/com/vibe/
│   └── pom.xml
├── vibe-drive-frontend/      # Vue 前端应用
│   ├── src/
│   └── package.json
├── services/                 # 微服务
│   └── music-api/            # Go 音乐 API 服务
└── docs/                     # 项目文档
    ├── 需求规格说明书.md
    └── plan/                 # 开发计划
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Maven 3.9+
- Go 1.21+

### 音乐微服务启动

```bash
cd services/music-api
go run main.go
# 服务运行在 http://localhost:8081
```

### 后端启动

```bash
cd vibe-drive-backend
mvn spring-boot:run
```

### 前端启动

```bash
cd vibe-drive-frontend
npm install
npm run dev
```

## 开发进度

| 阶段 | 名称 | 状态 |
|------|------|------|
| 阶段 1 | 项目初始化 | ✅ 已完成 |
| 阶段 2 | 设计文档 | ✅ 已完成 |
| 阶段 3 | 数据模型实现 | ✅ 已完成 |
| 阶段 4 | Tool 层实现 | ✅ 已完成 |
| 阶段 5 | Agent 编排 | ✅ 已完成 |
| 阶段 6 | API 与 Mock | ✅ 已完成 |
| 阶段 7 | 前端开发 | ⏳ 待开始 |

## 文档

- [需求规格说明书](docs/需求规格说明书.md)
- [开发计划](docs/plan/index.md)

## License

[MIT](LICENSE)