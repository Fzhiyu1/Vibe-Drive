# 探索文档：语音 API (TTS)

## 探索目标

研究如何为 Vibe Drive 项目实现语音合成（Text-to-Speech）功能，让 NarrativeTool 生成的叙事文本能够真正播报出来。

## 背景

### 当前实现

`NarrativeTool` 生成叙事文本，返回 `Narrative` 对象：

```java
public record Narrative(
    String text,           // 叙事文本
    String voice,          // 语音类型
    double speed,          // 语速 0.5-2.0
    double volume,         // 音量 0.0-1.0
    NarrativeEmotion emotion  // 情感基调
) {}
```

**问题**：目前只返回文本，没有真正的语音合成。前端无法播放语音。

### 需求

1. 将文本转换为音频（MP3/WAV）
2. 支持中文语音
3. 支持情感/语调控制
4. 延迟要求：< 2秒（车载场景）
5. 成本可控

## 待探索内容

- [ ] TTS API 选型对比
- [ ] 免费/低成本方案调研
- [ ] 情感语音支持情况
- [ ] 延迟与性能测试
- [ ] 集成方案设计

---

## TTS API 选型

### 1. 国内云服务

| 服务商 | API | 免费额度 | 价格 | 中文支持 | 情感控制 |
|--------|-----|---------|------|---------|---------|
| 阿里云 | 智能语音交互 | 3个月免费 | ¥0.002/字 | ✅ 优秀 | ✅ 支持 |
| 腾讯云 | 语音合成 | 每月100万字 | ¥0.002/字 | ✅ 优秀 | ✅ 支持 |
| 百度云 | 语音合成 | 每日5万字 | ¥0.003/字 | ✅ 优秀 | ✅ 支持 |
| 讯飞 | 语音合成 | 每日500次 | ¥0.003/字 | ✅ 优秀 | ✅ 支持 |

### 2. 国际云服务

| 服务商 | API | 免费额度 | 价格 | 中文支持 | 情感控制 |
|--------|-----|---------|------|---------|---------|
| Azure | Speech Services | 每月50万字 | $4/100万字 | ✅ 支持 | ✅ SSML |
| Google | Cloud TTS | 每月100万字 | $4/100万字 | ✅ 支持 | ✅ SSML |
| AWS | Polly | 每月500万字(12个月) | $4/100万字 | ✅ 支持 | ✅ SSML |
| OpenAI | TTS | 无免费 | $15/100万字 | ✅ 支持 | ❌ 有限 |

### 3. 免费/开源方案

| 方案 | 类型 | 中文支持 | 情感控制 | 延迟 | 备注 |
|------|------|---------|---------|------|------|
| Edge TTS | 免费API | ✅ 优秀 | ✅ 支持 | 快 | 微软Edge浏览器TTS |
| pyttsx3 | 本地 | ⚠️ 依赖系统 | ❌ | 快 | 质量一般 |
| Coqui TTS | 开源 | ⚠️ 需训练 | ✅ | 慢 | 需要GPU |
| VITS | 开源 | ✅ 有模型 | ✅ | 中 | 需要部署 |

---

## 探索记录

### 话题1：Edge TTS 调研

**讨论日期**：2025-12-26

**为什么关注 Edge TTS**：
- 完全免费，无调用限制
- 中文语音质量优秀
- 支持多种语音和情感
- 延迟低（流式输出）

#### 1.1 Edge TTS 简介

Edge TTS 是微软 Edge 浏览器内置的语音合成服务，可以通过非官方 API 调用。

**Python 库**：`edge-tts`
**Node.js 库**：`edge-tts` (npm)

#### 1.2 支持的中文语音

| 语音ID | 名称 | 性别 | 风格 |
|--------|------|------|------|
| zh-CN-XiaoxiaoNeural | 晓晓 | 女 | 温暖、亲切 |
| zh-CN-YunxiNeural | 云希 | 男 | 阳光、活力 |
| zh-CN-YunjianNeural | 云健 | 男 | 沉稳、专业 |
| zh-CN-XiaoyiNeural | 晓伊 | 女 | 甜美、可爱 |
| zh-CN-YunyangNeural | 云扬 | 男 | 新闻播报 |
| zh-CN-XiaochenNeural | 晓辰 | 女 | 轻松、随和 |

#### 1.3 情感风格支持

晓晓（Xiaoxiao）支持的情感风格：
- `cheerful` - 欢快
- `sad` - 悲伤
- `angry` - 愤怒
- `fearful` - 恐惧
- `disgruntled` - 不满
- `serious` - 严肃
- `affectionate` - 亲切
- `gentle` - 温柔
- `lyrical` - 抒情

#### 1.4 验证结果

- [x] 实际调用延迟测试 → **平均 1291ms**
- [ ] 音频质量评估 → 待人工试听
- [ ] 长期稳定性（非官方API）
- [ ] 并发调用限制

---

### 话题2：集成方案设计

**讨论日期**：2025-12-26

**选定方案**：方案B - 流式播放

#### 方案对比

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| A. 生成后丢弃 | 生成完整音频→播放→删除 | 实现简单 | 每次1-2秒延迟 |
| **B. 流式播放** | 边生成边播放 | 延迟最低 | 实现稍复杂 |
| C. 缓存机制 | 生成后缓存复用 | 可复用 | 叙事文本每次不同，命中率低 |

#### 选择理由

1. Vibe Drive 的叙事文本是 AI 动态创作的，每次都不同
2. 缓存意义不大
3. 流式播放体验最好，延迟最低

#### 实现架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   前端      │     │   后端      │     │  Edge TTS   │
│  (Vue)      │────▶│  (Spring)   │────▶│   API       │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │
      │   SSE/WebSocket   │   音频流          │
      │◀──────────────────│◀──────────────────│
      │                   │                   │
      ▼                   │                   │
  Web Audio API           │                   │
  边接收边播放            │                   │
```

#### 实现要点

1. **后端**：
   - 新增 TTS 服务，调用 Edge TTS API
   - 通过 SSE 或 WebSocket 转发音频流
   - 可用 Java 调用 Node.js 脚本，或直接用 Java WebSocket 客户端

2. **前端**：
   - 使用 Web Audio API 流式播放
   - 接收音频 chunk，解码后播放
   - 播放完成后内存自动释放

3. **触发时机**：
   - `generateNarrative` 工具返回结果时
   - 前端收到 `tool_end` 事件，提取文本
   - 调用 TTS API 开始播放

---

### 话题3：最终方案确定

**讨论日期**：2025-12-26

**选定服务**：Edge TTS（免费）

**选定语音**：晓伊（zh-CN-XiaoyiNeural）

**选择理由**：
1. 完全免费，无调用限制
2. 声音甜美可爱，试听效果最佳
3. 延迟可接受（~1.3秒）

#### 情感映射方案

**结论**：prosody 模拟情感效果不佳，统一使用默认设置。

| NarrativeEmotion | 设置 |
|------------------|------|
| 全部 | 默认（无特殊处理） |

> 实验验证：通过 rate/pitch 调整模拟情感，试听效果不自然，放弃。

#### 待实现

- [ ] 后端 TTS 服务（调用 Edge TTS）
- [ ] SSE 音频流转发
- [ ] 前端 Web Audio API 流式播放
- [ ] 情感风格 SSML 支持

---

## 实验记录

### 实验1：Edge TTS 测试

**状态**：✅ 已完成

**日期**：2025-12-26

**目标**：
1. 测试 Edge TTS 的调用方式
2. 评估音频质量
3. 测量延迟

**测试脚本位置**：`docs/explore/experiments/tts-test/`

**使用的 npm 包**：`msedge-tts@2.0.3`

#### 测试结果

| 语音 | 文本 | 延迟 | 文件大小 |
|------|------|------|---------|
| 晓晓 | 夜色温柔... | 1238ms | 22.8KB |
| 晓晓 | 雨滴敲窗... | 1306ms | 22.9KB |
| 晓晓 | 好心情... | 1232ms | 19.4KB |
| 云希 | 夜色温柔... | 1310ms | 24.0KB |
| 云希 | 雨滴敲窗... | 1588ms | 22.6KB |
| 云希 | 好心情... | 1149ms | 20.1KB |
| 晓伊 | 夜色温柔... | 1232ms | 24.2KB |
| 晓伊 | 雨滴敲窗... | 1288ms | 24.5KB |
| 晓伊 | 好心情... | 1276ms | 21.1KB |

**汇总**：
- 生成文件数：9 个
- 平均延迟：**1291ms**
- 音频质量：待人工试听

**生成的音频文件**：
```
docs/explore/experiments/tts-test/output/
├── xiaoxiao_calm.mp3
├── xiaoxiao_warm.mp3
├── xiaoxiao_energetic.mp3
├── yunxi_calm.mp3
├── yunxi_warm.mp3
├── yunxi_energetic.mp3
├── xiaoyi_calm.mp3
├── xiaoyi_warm.mp3
└── xiaoyi_energetic.mp3
```

---

## 参考资源

- [Edge TTS (Python)](https://github.com/rany2/edge-tts)
- [Edge TTS (Node.js)](https://www.npmjs.com/package/edge-tts)
- [Azure Speech Services](https://azure.microsoft.com/zh-cn/products/ai-services/text-to-speech)
- [阿里云智能语音](https://ai.aliyun.com/nls/tts)
- [腾讯云语音合成](https://cloud.tencent.com/product/tts)
