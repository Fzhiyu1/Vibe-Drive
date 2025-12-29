@echo off
REM Vibe Drive 部署打包脚本 (Windows)
REM 在项目根目录运行: scripts\pack-deploy.bat

setlocal enabledelayedexpansion

set PACK_NAME=vibe-drive-deploy-%date:~0,4%%date:~5,2%%date:~8,2%
set PACK_DIR=deploy-pack

echo === Vibe Drive 部署打包 ===

REM 清理旧的打包目录
if exist %PACK_DIR% rmdir /s /q %PACK_DIR%
mkdir %PACK_DIR%

echo [1/6] 复制 Docker 配置...
mkdir %PACK_DIR%\docker
copy docker\docker-compose.yml %PACK_DIR%\docker\
copy docker\Dockerfile.* %PACK_DIR%\docker\
copy docker\nginx.conf %PACK_DIR%\docker\
copy docker\application-docker.yml %PACK_DIR%\docker\

echo [2/6] 复制后端代码...
mkdir %PACK_DIR%\vibe-drive-backend
xcopy /E /I vibe-drive-backend\src %PACK_DIR%\vibe-drive-backend\src
copy vibe-drive-backend\pom.xml %PACK_DIR%\vibe-drive-backend\
copy vibe-drive-backend\mvnw %PACK_DIR%\vibe-drive-backend\
xcopy /E /I vibe-drive-backend\.mvn %PACK_DIR%\vibe-drive-backend\.mvn
REM 复制配置模板作为默认配置
copy vibe-drive-backend\src\main\resources\application.yml.example ^
     %PACK_DIR%\vibe-drive-backend\src\main\resources\application.yml

echo [3/6] 复制前端代码...
mkdir %PACK_DIR%\vibe-drive-frontend
xcopy /E /I vibe-drive-frontend\src %PACK_DIR%\vibe-drive-frontend\src
xcopy /E /I vibe-drive-frontend\public %PACK_DIR%\vibe-drive-frontend\public
copy vibe-drive-frontend\package*.json %PACK_DIR%\vibe-drive-frontend\
copy vibe-drive-frontend\vite.config.ts %PACK_DIR%\vibe-drive-frontend\
copy vibe-drive-frontend\tsconfig*.json %PACK_DIR%\vibe-drive-frontend\
copy vibe-drive-frontend\index.html %PACK_DIR%\vibe-drive-frontend\
copy vibe-drive-frontend\env.d.ts %PACK_DIR%\vibe-drive-frontend\ 2>nul

echo [4/6] 复制 TTS 服务...
mkdir %PACK_DIR%\vibe-drive-tts
xcopy /E /I vibe-drive-tts\src %PACK_DIR%\vibe-drive-tts\src
copy vibe-drive-tts\package*.json %PACK_DIR%\vibe-drive-tts\

echo [5/6] 复制音乐 API 服务...
mkdir %PACK_DIR%\services\music-api
copy services\music-api\*.go %PACK_DIR%\services\music-api\
copy services\music-api\go.* %PACK_DIR%\services\music-api\

echo [6/6] 复制部署脚本...
copy scripts\deploy.sh %PACK_DIR%\
copy scripts\README-deploy.md %PACK_DIR%\README.md

echo.
echo 正在压缩...
powershell -Command "Compress-Archive -Path '%PACK_DIR%\*' -DestinationPath '%PACK_NAME%.zip' -Force"

REM 清理
rmdir /s /q %PACK_DIR%

echo.
echo === 打包完成 ===
echo 文件: %PACK_NAME%.zip
echo.
echo 上传到服务器后:
echo   1. unzip %PACK_NAME%.zip -d vibe-drive
echo   2. cd vibe-drive
echo   3. 编辑 application.yml 配置 API Key
echo   4. bash deploy.sh

endlocal
