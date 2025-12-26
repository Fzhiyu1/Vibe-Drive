# Edge TTS 语音测试

测试 Edge TTS 语音合成效果，评估是否适合 Vibe Drive 叙事播报。

## 快速开始

```bash
# 安装依赖
npm install

# 运行测试（生成音频文件）
npm test

# 查看支持的中文语音
npm run voices
```

## 测试内容

### 测试语音

| 语音 | 特点 | 适合场景 |
|------|------|---------|
| 晓晓 Xiaoxiao | 温暖亲切 | 日常叙事 |
| 云希 Yunxi | 阳光活力 | 活力场景 |
| 晓伊 Xiaoyi | 甜美可爱 | 轻松氛围 |

### 测试文本

| 情感 | 文本 | 风格 |
|------|------|------|
| calm | 夜色温柔，让音乐陪你穿过这片星空 | gentle |
| warm | 雨滴敲窗，就让这首歌温暖归途 | affectionate |
| energetic | 好心情配好音乐，继续出发吧 | cheerful |

## 输出文件

运行测试后，音频文件保存在 `output/` 目录：

```
output/
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

## 评估标准

1. **自然度** - 是否像真人说话
2. **情感表达** - 风格是否明显
3. **延迟** - 生成速度是否够快
4. **清晰度** - 发音是否清晰

## 晓晓支持的情感风格

```
cheerful      欢快
sad           悲伤
angry         愤怒
fearful       恐惧
disgruntled   不满
serious       严肃
affectionate  亲切
gentle        温柔
lyrical       抒情
```
