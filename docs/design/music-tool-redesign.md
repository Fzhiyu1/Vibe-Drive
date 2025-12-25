# MusicTool 改造设计文档

## 概述

本文档描述如何将 MusicTool 从 Mock 数据改造为调用网易云音乐 API，并支持 ReAct 模式。

## 当前实现

### 问题

| 项目 | 现状 |
|------|------|
| 数据来源 | 本地 `mock-songs.json` |
| Tool 方法 | 只有 `recommendMusic()` |
| AI 能力 | 无法搜索、无法选择 |

### 当前代码

```java
@Tool
public MusicRecommendation recommendMusic(mood, timeOfDay, passengerCount, genre) {
    return musicService.recommend(...);  // 从本地 JSON 筛选
}
```

## 改造目标

1. 数据来源改为 **Go 微服务（网易云 API）**
2. 拆分为 **searchMusic + playMusic** 两个 Tool
3. 支持 **ReAct 模式**：AI 搜索 → 观察 → 选择 → 播放

## 新 Tool 设计

### Tool 1: searchMusic

**功能**：搜索歌曲，返回候选列表

**输入参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| keyword | String | 搜索关键词 |

**返回数据**：

```java
public record SearchResult(
    List<SongCandidate> songs,
    int total
) {}

public record SongCandidate(
    String id,
    String name,
    String artist,
    int duration,      // 秒
    long plays,        // 播放量
    int fee,           // 0=免费, 1=VIP, 8=低音质免费
    String coverUrl
) {}
```

**Tool 注解**：

```java
@Tool("""
    搜索音乐。返回候选列表供选择。
    根据播放量、歌手、时长等信息选择合适的歌曲。
    - fee=0 或 fee=8 可以播放
    - fee=1 需要 VIP，暂不支持
    - 时长太短（<60秒）可能不是正经音乐
    """)
public SearchResult searchMusic(
    @P("搜索关键词") String keyword
)
```

### Tool 2: playMusic

**功能**：播放指定歌曲

**输入参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | String | 歌曲 ID |

**返回数据**：

```java
public record PlayResult(
    String id,
    String name,
    String artist,
    String url,        // 播放地址
    int duration,      // 秒
    String coverUrl
) {}
```

**Tool 注解**：

```java
@Tool("播放指定歌曲。传入歌曲 ID，返回播放信息。")
public PlayResult playMusic(
    @P("歌曲 ID") String id
)
```

## ReAct 流程示例

```
用户: "来点轻松的音乐"

AI 思考: 用户想要轻松的音乐，我来搜索一下
AI 调用: searchMusic("轻松 纯音乐")

观察结果:
[
  { id: "123", name: "Canon in D", artist: "Pachelbel", plays: 5000000, fee: 0 },
  { id: "456", name: "River Flows", artist: "Yiruma", plays: 3000000, fee: 8 },
  { id: "789", name: "某翻唱", artist: "Unknown", plays: 100, fee: 0 },
]

AI 思考: Canon in D 播放量最高，是经典曲目，fee=0 可以播放
AI 调用: playMusic("123")

观察结果:
{ id: "123", name: "Canon in D", url: "http://...", duration: 240 }

AI 回复: 为您播放《Canon in D》，这是一首经典的轻松钢琴曲。
```

## 架构设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   AI Agent  │────▶│  MusicTool  │────▶│ MusicService│
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ Go 微服务   │
                                        │ :8081       │
                                        └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ 网易云 API  │
                                        └─────────────┘
```

## 实现步骤

### 步骤 1：新增数据模型

在 `com.vibe.model` 包下创建：
- `SongCandidate.java`
- `SearchResult.java`
- `PlayResult.java`

### 步骤 2：改造 MusicService

调用 Go 微服务获取数据。

### 步骤 3：改造 MusicTool

拆分为 `searchMusic()` 和 `playMusic()` 两个方法。

### 步骤 4：删除旧代码

删除 `recommendMusic()` 方法。

## 注意事项

1. Go 微服务需要先启动（端口 8081）
2. fee=1 的歌曲暂不支持播放
3. 返回结果限制 20 条
