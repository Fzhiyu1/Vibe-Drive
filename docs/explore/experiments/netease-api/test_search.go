package main

import (
	"encoding/json"
	"fmt"

	"github.com/go-musicfox/netease-music/service"
)

func main() {
	// 测试搜索 API
	fmt.Println("测试网易云音乐 API...")
	fmt.Println("搜索: 周杰伦")

	searchService := service.SearchService{
		S:     "周杰伦",
		Type:  "1", // 1=单曲
		Limit: "5",
	}

	code, response := searchService.Search()

	fmt.Printf("返回码: %.0f\n", code)

	if code == 200 {
		// 解析 JSON
		var result map[string]interface{}
		json.Unmarshal(response, &result)

		// 格式化输出
		prettyJSON, _ := json.MarshalIndent(result, "", "  ")
		fmt.Println("结果:")
		fmt.Println(string(prettyJSON[:min(len(prettyJSON), 2000)])) // 只显示前2000字符
	} else {
		fmt.Println("请求失败")
		fmt.Println(string(response))
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
