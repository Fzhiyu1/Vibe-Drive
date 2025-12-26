# Vibe Drive

基于 AI Agent 的车载智能氛围编排系统，通过环境感知驱动"时空叙事"体验。

## 项目结构

```
Vibe-Drive/
├── vibe-drive-backend/    # Java 后端 (Spring Boot 3.x)
├── vibe-drive-frontend/   # 前端 (Vue 3 + TypeScript)
├── vibe-drive-tts/        # TTS 语音服务 (Node.js)
├── services/music-api/    # 音乐 API 服务 (Go)
├── docker/                # Docker 配置
└── docs/                  # 文档
```

## 环境要求

- **Docker Desktop** (用于音乐服务)
- **Node.js 20+** (用于 TTS 服务和前端)
- **Java 21** (用于后端)
- **Maven** (用于后端构建)

## 快速启动

### 1. 启动音乐服务 (Docker)

```bash
cd docker
docker-compose up -d
```

验证：`curl http://localhost:8081/api/music/search?keyword=test`

### 2. 启动 TTS 服务 (本地 Node.js)

```bash
cd vibe-drive-tts
npm install    # 首次运行
npm run dev
```

验证：`curl http://localhost:3002/health`

> ⚠️ TTS 服务需本地运行，Docker 内有兼容性问题

### 3. 启动后端 (Java)

```bash
cd vibe-drive-backend
./mvnw spring-boot:run
```

验证：`curl http://localhost:8080/api/vibe/status`

### 4. 启动前端 (Vue)

```bash
cd vibe-drive-frontend
npm install    # 首次运行
npm run dev
```

访问：http://localhost:5173

## 服务端口

| 服务 | 端口 | 技术栈 | 说明 |
|------|------|--------|------|
| 音乐 | 8081 | Go | 网易云音乐 API |
| TTS | 3002 | Node.js | Edge TTS 语音合成 |
| 后端 | 8080 | Spring Boot | AI Agent 编排 |
| 前端 | 5173 | Vue 3 | 用户界面 |

## 配置

### 后端配置

编辑 `vibe-drive-backend/src/main/resources/application.yml`：

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key  # DeepSeek API Key
```

## 访问

打开浏览器访问 http://localhost:5173
