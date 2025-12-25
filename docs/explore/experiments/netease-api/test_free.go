package main

import (
	"encoding/json"
	"fmt"

	"github.com/go-musicfox/netease-music/service"
)

func main() {
	// 先搜索一首免费歌曲
	fmt.Println("搜索: 纯音乐 轻松")

	searchService := service.SearchService{
		S:     "纯音乐 轻松",
		Type:  "1",
		Limit: "3",
	}

	code, response := searchService.Search()
	fmt.Printf("搜索返回码: %.0f\n\n", code)

	var searchResult map[string]interface{}
	json.Unmarshal(response, &searchResult)

	// 提取第一首歌的 ID
	result := searchResult["result"].(map[string]interface{})
	songs := result["songs"].([]interface{})

	for i, song := range songs {
		s := song.(map[string]interface{})
		id := s["id"].(float64)
		name := s["name"].(string)
		fee := s["fee"].(float64)

		ar := s["ar"].([]interface{})
		artist := ar[0].(map[string]interface{})["name"].(string)

		fmt.Printf("%d. %s - %s (ID: %.0f, fee: %.0f)\n", i+1, artist, name, id, fee)

		// 测试获取 URL
		urlService := service.SongUrlService{
			ID: fmt.Sprintf("%.0f", id),
			Br: "128000",
		}
		urlCode, urlResponse := urlService.SongUrl()

		var urlResult map[string]interface{}
		json.Unmarshal(urlResponse, &urlResult)

		data := urlResult["data"].([]interface{})
		songData := data[0].(map[string]interface{})

		innerCode := songData["code"].(float64)
		url := songData["url"]

		if url != nil {
			fmt.Printf("   ✅ URL: %s\n\n", url.(string)[:80]+"...")
		} else {
			fmt.Printf("   ❌ 不可用 (code: %.0f, urlCode: %.0f)\n\n", innerCode, urlCode)
		}
	}
}
