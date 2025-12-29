@echo off
REM Vibe Drive 稳定服务打包脚本 (tts + music-api)
REM 这些服务基本不变，只需部署一次
REM 在项目根目录运行: scripts\pack-stable.bat

setlocal enabledelayedexpansion

set PACK_NAME=vibe-drive-stable
set PACK_DIR=stable-pack

echo === Vibe Drive 稳定服务打包 ===
echo.

REM 清理旧的打包目录
if exist %PACK_DIR% rmdir /s /q %PACK_DIR%
mkdir %PACK_DIR%

echo [1/2] 构建音乐 API...
cd services\music-api
set GOOS=linux
set GOARCH=amd64
go build -o music-api-linux .
if errorlevel 1 (
    echo 错误: 音乐 API 构建失败
    exit /b 1
)
cd ..\..

echo [2/2] 复制服务文件...

REM TTS 服务
mkdir %PACK_DIR%\tts
xcopy /E /I vibe-drive-tts\src %PACK_DIR%\tts\src
copy vibe-drive-tts\package*.json %PACK_DIR%\tts\

REM 音乐 API 二进制
mkdir %PACK_DIR%\music-api
copy services\music-api\music-api-linux %PACK_DIR%\music-api\

REM Docker 配置
mkdir %PACK_DIR%\docker
copy docker\docker-compose.stable.yml %PACK_DIR%\docker\docker-compose.yml

echo.
echo [压缩中...]
powershell -Command "Compress-Archive -Path '%PACK_DIR%\*' -DestinationPath '%PACK_NAME%.zip' -Force"

REM 清理
rmdir /s /q %PACK_DIR%
del services\music-api\music-api-linux 2>nul

echo.
echo === 打包完成 ===
echo 文件: %PACK_NAME%.zip
for %%A in (%PACK_NAME%.zip) do echo 大小: %%~zA bytes

endlocal
