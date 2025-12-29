#!/bin/bash
# Vibe Drive 服务器部署脚本
# 使用方法: bash deploy.sh

set -e

echo "=== Vibe Drive 部署 ==="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: 请先安装 Docker Compose"
    exit 1
fi

# 检查配置文件
CONFIG_FILE="vibe-drive-backend/src/main/resources/application.yml"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误: 请先配置 $CONFIG_FILE"
    echo "参考 application.yml.example 模板"
    exit 1
fi

# 检查 API Key 是否已配置
if grep -q "your-api-key-here" "$CONFIG_FILE"; then
    echo "警告: 请在 $CONFIG_FILE 中配置真实的 API Key"
    read -p "是否继续? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "[1/3] 构建镜像..."
cd docker
docker-compose build

echo ""
echo "[2/3] 启动服务..."
docker-compose up -d

echo ""
echo "[3/3] 检查服务状态..."
sleep 5
docker-compose ps

echo ""
echo "=== 部署完成 ==="
echo ""
echo "服务地址:"
echo "  - 前端: http://localhost:80"
echo "  - 后端: http://localhost:8080"
echo "  - TTS:  http://localhost:3002"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
