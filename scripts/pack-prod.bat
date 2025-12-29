@echo off
REM Vibe Drive 生产部署打包脚本 (只打包构建产物)
REM 在项目根目录运行: scripts\pack-prod.bat

setlocal enabledelayedexpansion

set PACK_NAME=vibe-drive-prod-%date:~0,4%%date:~5,2%%date:~8,2%
set PACK_DIR=prod-pack

echo === Vibe Drive 生产打包 ===
echo.

REM 清理旧的打包目录
if exist %PACK_DIR% rmdir /s /q %PACK_DIR%
mkdir %PACK_DIR%

echo [1/5] 构建后端 JAR...
cd vibe-drive-backend
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo 错误: 后端构建失败
    exit /b 1
)
cd ..

echo [2/5] 构建前端 dist...
cd vibe-drive-frontend
call npm run build-only
if errorlevel 1 (
    echo 错误: 前端构建失败
    exit /b 1
)
cd ..

echo [3/5] 构建音乐 API...
cd services\music-api
set GOOS=linux
set GOARCH=amd64
go build -o music-api-linux .
if errorlevel 1 (
    echo 错误: 音乐 API 构建失败
    exit /b 1
)
cd ..\..

echo [4/5] 复制构建产物...

REM Docker 配置
mkdir %PACK_DIR%\docker
copy docker\docker-compose.prod.yml %PACK_DIR%\docker\docker-compose.yml
copy docker\nginx.conf %PACK_DIR%\docker\
copy docker\application-docker.yml %PACK_DIR%\docker\

REM 后端 JAR
mkdir %PACK_DIR%\backend
for %%f in (vibe-drive-backend\target\*.jar) do copy "%%f" %PACK_DIR%\backend\app.jar

REM 前端 dist
mkdir %PACK_DIR%\frontend
xcopy /E /I vibe-drive-frontend\dist %PACK_DIR%\frontend\dist

REM TTS 服务
mkdir %PACK_DIR%\tts
xcopy /E /I vibe-drive-tts\src %PACK_DIR%\tts\src
copy vibe-drive-tts\package*.json %PACK_DIR%\tts\

REM 音乐 API 二进制
mkdir %PACK_DIR%\music-api
copy services\music-api\music-api-linux %PACK_DIR%\music-api\

REM 配置模板
copy vibe-drive-backend\src\main\resources\application.yml.example %PACK_DIR%\application.yml.example

REM 部署脚本
copy scripts\deploy-prod.sh %PACK_DIR%\deploy.sh
copy scripts\README-deploy.md %PACK_DIR%\README.md

echo [5/5] 压缩...
powershell -Command "Compress-Archive -Path '%PACK_DIR%\*' -DestinationPath '%PACK_NAME%.zip' -Force"

REM 清理
rmdir /s /q %PACK_DIR%
del services\music-api\music-api-linux 2>nul

echo.
echo === 打包完成 ===
echo 文件: %PACK_NAME%.zip
for %%A in (%PACK_NAME%.zip) do echo 大小: %%~zA bytes

endlocal
