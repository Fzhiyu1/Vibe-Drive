# 网易云音乐 API 实验

## 实验目的

验证 `go-musicfox/netease-music` Go 包是否可用于调用网易云音乐 API。

## 实验日期

2025-12-25

## 依赖项目

- **go-musicfox**: https://github.com/go-musicfox/go-musicfox
- **netease-music**: go-musicfox 内置的 API 封装包

## 测试文件

| 文件 | 说明 |
|------|------|
| test_search.go | 测试搜索 API |
| test_url.go | 测试获取歌曲 URL |
| test_free.go | 综合测试（搜索 + 获取 URL）|

## 测试结果

### 1. 搜索 API ✅

```
搜索: 周杰伦
返回码: 200
结果: 260 首歌曲
```

### 2. 获取歌曲 URL

| 歌曲 | fee | 结果 |
|------|-----|------|
| 周杰伦 - 晴天 | VIP | ❌ 404 无版权 |
| V.A. - Coins Obtained | 0 (免费) | ✅ 可用 |
| ViccyC - Kamasutra | 8 (VIP) | ✅ 可用 |
| dylanf - 卡农 | 8 (VIP) | ✅ 可用 |

## 结论

**`go-musicfox/netease-music` 包可用！**

- ✅ 搜索 API 正常
- ✅ 获取播放 URL 正常
- ⚠️ 部分歌曲（如周杰伦）因版权限制不可用
- ✅ 大部分歌曲（包括标记 VIP 的）可获取 URL

## 核心 API

### 搜索

```go
searchService := service.SearchService{
    S:     "关键词",
    Type:  "1",      // 1=单曲
    Limit: "10",
}
code, response := searchService.Search()
```

### 获取播放 URL

```go
urlService := service.SongUrlService{
    ID: "歌曲ID",
    Br: "320000",  // 比特率
}
code, response := urlService.SongUrl()
```

## 运行方式

需要在 go-musicfox 项目目录下运行（依赖 vendor）：

```bash
cd C:\Users\24038\Documents\run\go-musicfox
go run test_free.go
```
