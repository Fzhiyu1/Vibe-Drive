package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/go-musicfox/netease-music/service"
)

func main() {
	// 注册路由
	http.HandleFunc("/api/music/search", corsMiddleware(handleSearch))
	http.HandleFunc("/api/music/url", corsMiddleware(handleUrl))
	http.HandleFunc("/api/music/detail", corsMiddleware(handleDetail))

	fmt.Println("Music API 服务启动: http://localhost:8081")
	log.Fatal(http.ListenAndServe(":8081", nil))
}

// CORS 中间件
func corsMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
		w.Header().Set("Content-Type", "application/json")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next(w, r)
	}
}

// 搜索歌曲
func handleSearch(w http.ResponseWriter, r *http.Request) {
	keyword := r.URL.Query().Get("keyword")
	if keyword == "" {
		http.Error(w, `{"error": "keyword required"}`, http.StatusBadRequest)
		return
	}

	limit := r.URL.Query().Get("limit")
	if limit == "" {
		limit = "10"
	}

	searchService := service.SearchService{
		S:     keyword,
		Type:  "1", // 1=单曲
		Limit: limit,
	}

	code, response := searchService.Search()

	if code != 200 {
		http.Error(w, `{"error": "search failed"}`, http.StatusInternalServerError)
		return
	}

	w.Write(response)
}

// 获取播放 URL
func handleUrl(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("id")
	if id == "" {
		http.Error(w, `{"error": "id required"}`, http.StatusBadRequest)
		return
	}

	urlService := service.SongUrlService{
		ID: id,
		Br: "320000", // 320kbps
	}

	code, response := urlService.SongUrl()

	if code != 200 {
		http.Error(w, `{"error": "get url failed"}`, http.StatusInternalServerError)
		return
	}

	w.Write(response)
}

// 获取歌曲详情
func handleDetail(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("id")
	if id == "" {
		http.Error(w, `{"error": "id required"}`, http.StatusBadRequest)
		return
	}

	detailService := service.SongDetailService{
		Ids: id,
	}

	code, response := detailService.SongDetail()

	if code != 200 {
		http.Error(w, `{"error": "get detail failed"}`, http.StatusInternalServerError)
		return
	}

	w.Write(response)
}

// 用于格式化 JSON 输出（调试用）
func prettyJSON(data []byte) string {
	var result map[string]interface{}
	json.Unmarshal(data, &result)
	pretty, _ := json.MarshalIndent(result, "", "  ")
	return string(pretty)
}
