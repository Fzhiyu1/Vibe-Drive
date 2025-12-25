package main

import (
	"encoding/json"
	"fmt"

	"github.com/go-musicfox/netease-music/service"
)

func main() {
	// 测试获取歌曲 URL
	// 使用一首歌的 ID（周杰伦 - 晴天: 186016）
	songId := "186016"

	fmt.Println("测试获取歌曲播放 URL...")
	fmt.Printf("歌曲 ID: %s\n", songId)

	urlService := service.SongUrlService{
		ID: songId,
		Br: "320000", // 320kbps
	}

	code, response := urlService.SongUrl()

	fmt.Printf("返回码: %.0f\n", code)

	var result map[string]interface{}
	json.Unmarshal(response, &result)

	prettyJSON, _ := json.MarshalIndent(result, "", "  ")
	fmt.Println("结果:")
	fmt.Println(string(prettyJSON))
}
