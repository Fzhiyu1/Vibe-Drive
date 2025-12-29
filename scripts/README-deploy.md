# Vibe Drive 部署指南

## 服务器要求

- Docker 20.10+
- Docker Compose 2.0+
- 内存: 4GB+
- 磁盘: 10GB+

## 快速部署

### 1. 上传并解压

```bash
# 上传 vibe-drive-deploy-xxx.tar.gz 到服务器
mkdir vibe-drive && cd vibe-drive
tar -xzvf ../vibe-drive-deploy-xxx.tar.gz
```

### 2. 配置 API Key

```bash
# 编辑配置文件
vim vibe-drive-backend/src/main/resources/application.yml
```

必须配置的项目：
- `langchain4j.open-ai.chat-model.api-key` - OpenAI/DeepSeek API Key
- `langchain4j.anthropic.chat-model.api-key` - Anthropic API Key（如使用）

### 3. 启动服务

```bash
bash deploy.sh
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 80 | Nginx 静态服务 |
| 后端 | 8080 | Spring Boot API |
| TTS | 3002 | 语音合成服务 |
| 音乐API | 8081 | 网易云音乐代理 |

## 常用命令

```bash
cd docker

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f
docker-compose logs -f backend  # 只看后端

# 重启服务
docker-compose restart
docker-compose restart backend  # 只重启后端

# 停止服务
docker-compose down

# 重新构建
docker-compose build --no-cache
docker-compose up -d
```

## 故障排查

### 音乐搜索无结果
检查音乐 API 服务日志：
```bash
docker-compose logs music-api
```

### 后端连接失败
检查 API Key 配置和网络：
```bash
docker-compose logs backend | grep -i error
```

### 前端无法访问
检查 Nginx 配置：
```bash
docker-compose logs frontend
```
