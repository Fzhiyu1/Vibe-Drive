@echo off
REM Vibe Drive 生产部署打包脚本 (只打包 backend + frontend)
REM 稳定服务 (tts/music-api/whisper) 单独部署，见 pack-stable.bat
REM 在项目根目录运行: scripts\pack-prod.bat

setlocal enabledelayedexpansion

set PACK_NAME=vibe-drive-prod-%date:~0,4%%date:~5,2%%date:~8,2%
set PACK_DIR=prod-pack

echo === Vibe Drive 生产打包 (backend + frontend) ===
echo.

REM 清理旧的打包目录
if exist %PACK_DIR% rmdir /s /q %PACK_DIR%
mkdir %PACK_DIR%

echo [1/3] 构建后端 JAR...
cd vibe-drive-backend
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo 错误: 后端构建失败
    exit /b 1
)
cd ..

echo [2/3] 构建前端 dist...
cd vibe-drive-frontend
call npm run build-only
if errorlevel 1 (
    echo 错误: 前端构建失败
    exit /b 1
)
cd ..

echo [3/3] 复制构建产物...

REM Docker 配置
mkdir %PACK_DIR%\docker
copy docker\docker-compose.prod.yml %PACK_DIR%\docker\docker-compose.yml
copy docker\nginx.conf %PACK_DIR%\docker\

REM 后端 JAR
mkdir %PACK_DIR%\backend
for %%f in (vibe-drive-backend\target\*.jar) do copy "%%f" %PACK_DIR%\backend\app.jar

REM 前端 dist
mkdir %PACK_DIR%\frontend
xcopy /E /I vibe-drive-frontend\dist %PACK_DIR%\frontend\dist

REM 配置模板
copy vibe-drive-backend\src\main\resources\application.yml.example %PACK_DIR%\application.yml.example

echo.
echo [压缩中...]
powershell -Command "Compress-Archive -Path '%PACK_DIR%\*' -DestinationPath '%PACK_NAME%.zip' -Force"

REM 清理
rmdir /s /q %PACK_DIR%

echo.
echo === 打包完成 ===
echo 文件: %PACK_NAME%.zip
for %%A in (%PACK_NAME%.zip) do echo 大小: %%~zA bytes
echo.
echo 注意: 稳定服务 (tts/music-api) 需单独部署，运行 pack-stable.bat

endlocal
