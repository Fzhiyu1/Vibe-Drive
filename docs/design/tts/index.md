# 语音系统设计

## 概述

Vibe Drive 语音系统负责将 AI 生成的叙事文本转换为语音播报，增强车载氛围体验。

## 技术选型

| 项目 | 选择 | 理由 |
|------|------|------|
| TTS 服务 | Edge TTS | 免费、中文质量优秀 |
| 语音 | 晓伊（XiaoyiNeural） | 声音甜美，试听效果最佳 |
| 播放方式 | 流式播放 | 延迟最低 |

## 文档结构

- [架构设计](./architecture.md) - 系统架构与数据流
- [接口设计](./api.md) - API 接口定义
- [前端实现](./frontend.md) - Web Audio 流式播放

## 相关文档

- [探索文档](../../explore/探索文档-语音API.md) - 技术调研记录
- [实验代码](../../explore/experiments/tts-test/) - TTS 测试脚本
