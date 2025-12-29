#!/bin/bash
# Vibe Drive 部署打包脚本
# 在项目根目录运行: bash scripts/pack-deploy.sh

set -e

PACK_NAME="vibe-drive-deploy-$(date +%Y%m%d)"
PACK_DIR="deploy-pack"

echo "=== Vibe Drive 部署打包 ==="

# 清理旧的打包目录
rm -rf $PACK_DIR
mkdir -p $PACK_DIR

echo "[1/6] 复制 Docker 配置..."
mkdir -p $PACK_DIR/docker
cp docker/docker-compose.yml $PACK_DIR/docker/
cp docker/Dockerfile.* $PACK_DIR/docker/
cp docker/nginx.conf $PACK_DIR/docker/
cp docker/application-docker.yml $PACK_DIR/docker/

echo "[2/6] 复制后端代码..."
mkdir -p $PACK_DIR/vibe-drive-backend
cp -r vibe-drive-backend/src $PACK_DIR/vibe-drive-backend/
cp vibe-drive-backend/pom.xml $PACK_DIR/vibe-drive-backend/
cp vibe-drive-backend/mvnw $PACK_DIR/vibe-drive-backend/
cp -r vibe-drive-backend/.mvn $PACK_DIR/vibe-drive-backend/
# 复制配置模板
cp vibe-drive-backend/src/main/resources/application.yml.example \
   $PACK_DIR/vibe-drive-backend/src/main/resources/application.yml

echo "[3/6] 复制前端代码..."
mkdir -p $PACK_DIR/vibe-drive-frontend
cp -r vibe-drive-frontend/src $PACK_DIR/vibe-drive-frontend/
cp -r vibe-drive-frontend/public $PACK_DIR/vibe-drive-frontend/
cp vibe-drive-frontend/package*.json $PACK_DIR/vibe-drive-frontend/
cp vibe-drive-frontend/vite.config.ts $PACK_DIR/vibe-drive-frontend/
cp vibe-drive-frontend/tsconfig*.json $PACK_DIR/vibe-drive-frontend/
cp vibe-drive-frontend/index.html $PACK_DIR/vibe-drive-frontend/
cp vibe-drive-frontend/env.d.ts $PACK_DIR/vibe-drive-frontend/ 2>/dev/null || true

echo "[4/6] 复制 TTS 服务..."
mkdir -p $PACK_DIR/vibe-drive-tts
cp -r vibe-drive-tts/src $PACK_DIR/vibe-drive-tts/
cp vibe-drive-tts/package*.json $PACK_DIR/vibe-drive-tts/

echo "[5/6] 复制音乐 API 服务..."
mkdir -p $PACK_DIR/services/music-api
cp services/music-api/*.go $PACK_DIR/services/music-api/
cp services/music-api/go.* $PACK_DIR/services/music-api/

echo "[6/6] 复制部署脚本..."
cp scripts/deploy.sh $PACK_DIR/
cp scripts/README-deploy.md $PACK_DIR/README.md

# 打包
echo ""
echo "正在压缩..."
tar -czvf ${PACK_NAME}.tar.gz -C $PACK_DIR .

# 清理
rm -rf $PACK_DIR

echo ""
echo "=== 打包完成 ==="
echo "文件: ${PACK_NAME}.tar.gz"
echo "大小: $(du -h ${PACK_NAME}.tar.gz | cut -f1)"
echo ""
echo "上传到服务器后运行: bash deploy.sh"
