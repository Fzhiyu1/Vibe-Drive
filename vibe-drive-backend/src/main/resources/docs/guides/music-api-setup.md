# Music API 微服务搭建指南

## 概述

本指南描述如何在 Vibe Drive 项目中搭建 Go 微服务，封装网易云音乐 API。

## 前置条件

- Go 1.21+（已安装）
- 了解基本的 Go 语法
- 了解 HTTP REST API 概念

## 目标

创建一个 Go HTTP 服务，提供以下 API：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/music/search` | GET | 搜索歌曲 |
| `/api/music/url` | GET | 获取播放 URL |
| `/api/music/detail` | GET | 获取歌曲详情 |

## 步骤

### 步骤 1：创建项目目录

```bash
cd C:\Users\24038\Documents\run\Vibe-Drive
mkdir -p services/music-api
cd services/music-api
```

### 步骤 2：初始化 Go 模块

```bash
go mod init vibe-drive/music-api
```

### 步骤 3：复制依赖包

从 go-musicfox 项目复制 netease-music 包：

```bash
mkdir -p pkg
cp -r C:\Users\24038\Documents\run\go-musicfox\vendor\github.com\go-musicfox\netease-music pkg/
```

或者直接引用（需要网络）：

```bash
go get github.com/go-musicfox/netease-music
```

### 步骤 4：创建主程序

创建 `main.go`：

```go
package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"

    "github.com/go-musicfox/netease-music/service"
)

func main() {
    http.HandleFunc("/api/music/search", handleSearch)
    http.HandleFunc("/api/music/url", handleUrl)
    http.HandleFunc("/api/music/detail", handleDetail)

    fmt.Println("Music API 服务启动: http://localhost:8081")
    log.Fatal(http.ListenAndServe(":8081", nil))
}
```

### 步骤 5：实现搜索接口

在 `main.go` 中添加：

```go
func handleSearch(w http.ResponseWriter, r *http.Request) {
    // 设置 CORS
    w.Header().Set("Access-Control-Allow-Origin", "*")
    w.Header().Set("Content-Type", "application/json")

    // 获取参数
    keyword := r.URL.Query().Get("keyword")
    if keyword == "" {
        http.Error(w, `{"error": "keyword required"}`, 400)
        return
    }

    limit := r.URL.Query().Get("limit")
    if limit == "" {
        limit = "10"
    }

    // 调用网易云 API
    searchService := service.SearchService{
        S:     keyword,
        Type:  "1",  // 1=单曲
        Limit: limit,
    }
    code, response := searchService.Search()

    if code != 200 {
        http.Error(w, `{"error": "search failed"}`, 500)
        return
    }

    // 解析并简化返回结果
    var result map[string]interface{}
    json.Unmarshal(response, &result)

    // 返回结果
    w.Write(response)
}
```

### 步骤 7：实现歌曲详情接口

```go
func handleDetail(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Access-Control-Allow-Origin", "*")
    w.Header().Set("Content-Type", "application/json")

    id := r.URL.Query().Get("id")
    if id == "" {
        http.Error(w, `{"error": "id required"}`, 400)
        return
    }

    detailService := service.SongDetailService{
        Ids: id,
    }
    code, response := detailService.SongDetail()

    if code != 200 {
        http.Error(w, `{"error": "get detail failed"}`, 500)
        return
    }

    w.Write(response)
}
```

### 步骤 8：运行服务

```bash
cd services/music-api
go run main.go
```

输出：
```
Music API 服务启动: http://localhost:8081
```

## 测试

### 测试搜索

```bash
curl "http://localhost:8081/api/music/search?keyword=周杰伦&limit=5"
```

### 测试获取 URL

```bash
curl "http://localhost:8081/api/music/url?id=186016"
```

## 与 Vibe Drive 后端集成

在 Java 后端中调用 Go 微服务：

```java
// MusicService.java
@Service
public class MusicService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String musicApiUrl = "http://localhost:8081";

    public SearchResult search(String keyword, int limit) {
        String url = musicApiUrl + "/api/music/search?keyword=" + keyword + "&limit=" + limit;
        return restTemplate.getForObject(url, SearchResult.class);
    }

    public SongUrl getSongUrl(String id) {
        String url = musicApiUrl + "/api/music/url?id=" + id;
        return restTemplate.getForObject(url, SongUrl.class);
    }
}
```

## 目录结构

完成后的目录结构：

```
services/music-api/
├── go.mod
├── go.sum
├── main.go
└── README.md
```

## 注意事项

1. **端口**：Go 服务使用 8081 端口，避免与 Java 后端（8080）冲突
2. **CORS**：已配置允许跨域，前端可直接调用（开发环境）
3. **版权**：仅用于学习/Demo，不要用于商业用途
4. **依赖**：需要能访问 GitHub 下载 go-musicfox/netease-music 包
