#!/bin/bash
# Vibe Drive 生产部署脚本 (使用预构建产物)

set -e

echo "=== Vibe Drive 生产部署 ==="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 请先安装 Docker"
    exit 1
fi

# 检查配置文件
if [ ! -f "application.yml" ]; then
    echo "错误: 请先配置 application.yml"
    echo "参考 application.yml.example 模板"
    exit 1
fi

# 检查构建产物
echo "检查构建产物..."
[ -f "backend/app.jar" ] || { echo "缺少 backend/app.jar"; exit 1; }
[ -d "frontend/dist" ] || { echo "缺少 frontend/dist"; exit 1; }
[ -f "music-api/music-api-linux" ] || { echo "缺少 music-api/music-api-linux"; exit 1; }

# 设置可执行权限
chmod +x music-api/music-api-linux

# TTS 安装依赖
echo "安装 TTS 依赖..."
cd tts && npm install --production && cd ..

# 启动服务
echo "启动服务..."
cd docker
docker compose up -d

echo ""
docker compose ps

echo ""
echo "=== 部署完成 ==="
echo "前端: http://localhost:80"
echo "后端: http://localhost:8080"
