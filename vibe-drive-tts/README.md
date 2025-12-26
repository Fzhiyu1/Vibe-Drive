# Vibe Drive TTS 微服务

Edge TTS 语音合成服务，为 Vibe Drive 提供 TTS 功能。

## 启动

```bash
npm install
npm run dev
```

## API

### GET /api/tts/speak

文本转语音，返回音频流。

**参数**：
- `text` - 要转换的文本（必填）
- `voice` - 语音ID（可选，默认 zh-CN-XiaoyiNeural）

**示例**：
```
GET /api/tts/speak?text=你好世界
```

### GET /health

健康检查。
