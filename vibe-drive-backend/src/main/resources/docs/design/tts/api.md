# 语音系统 API 设计

## 接口概览

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/tts/synthesize | 文本转语音（流式返回） |

## 接口详情

### POST /api/tts/synthesize

将文本转换为语音，流式返回音频数据。

#### 请求

```json
{
  "text": "夜色温柔，让音乐陪你穿过这片星空",
  "voice": "zh-CN-XiaoyiNeural"
}
```

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| text | string | 是 | 要转换的文本 |
| voice | string | 否 | 语音ID，默认 zh-CN-XiaoyiNeural |

#### 响应

- Content-Type: `audio/mpeg`
- Transfer-Encoding: `chunked`

流式返回 MP3 音频数据。

#### 错误码

| 状态码 | 描述 |
|--------|------|
| 400 | 文本为空或过长 |
| 500 | TTS 服务调用失败 |
| 503 | TTS 服务不可用 |
