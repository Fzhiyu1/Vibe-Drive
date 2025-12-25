"""
测试 AI 音乐推荐是否会收敛到固定歌曲

测试目标：
1. 同一场景多次调用，是否推荐相同歌曲
2. 不同场景（夜间/白天/雨天）是否收敛到固定歌曲

使用方法：
python test_music_convergence.py
"""

import requests
import json
from collections import defaultdict

API_URL = "https://api.deepseek.com/chat/completions"
API_KEY = "sk-45bc3912cb954ca9868c52f571b6d7af"

SYSTEM_PROMPT = """你是 Vibe Drive 氛围编排智能体，负责根据车载环境数据编排合适的氛围方案。

## 可用工具
- searchMusic: 搜索音乐，返回候选列表

## 任务
根据环境数据，调用 searchMusic 搜索合适的音乐。
只需要返回你会使用的搜索关键词，格式：
搜索关键词: <你的关键词>
"""

# 测试场景
SCENARIOS = {
    "夜间高速": {
        "gpsTag": "highway",
        "weather": "sunny",
        "speed": 50,
        "userMood": "calm",
        "timeOfDay": "night",
        "passengerCount": 1
    },
    "清晨城市": {
        "gpsTag": "urban",
        "weather": "sunny",
        "speed": 40,
        "userMood": "happy",
        "timeOfDay": "morning",
        "passengerCount": 1
    },
    "雨天下午": {
        "gpsTag": "urban",
        "weather": "rainy",
        "speed": 30,
        "userMood": "calm",
        "timeOfDay": "afternoon",
        "passengerCount": 1
    }
}

def call_ai(scenario_name, env_data):
    """调用 DeepSeek API"""
    user_prompt = f"当前环境：{json.dumps(env_data, ensure_ascii=False)}\n请给出搜索关键词。"

    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }

    payload = {
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt}
        ],
        "temperature": 0.7
    }

    try:
        resp = requests.post(API_URL, headers=headers, json=payload, timeout=30)
        resp.raise_for_status()
        content = resp.json()["choices"][0]["message"]["content"]
        return content
    except Exception as e:
        return f"Error: {e}"

def main():
    print("=" * 60)
    print("AI 音乐推荐收敛性测试")
    print("=" * 60)

    results = defaultdict(list)

    # 每个场景测试 3 次
    for scenario_name, env_data in SCENARIOS.items():
        print(f"\n### {scenario_name} ###")
        for i in range(3):
            print(f"  测试 {i+1}...", end=" ")
            response = call_ai(scenario_name, env_data)
            results[scenario_name].append(response)
            # 提取关键词
            if "搜索关键词:" in response:
                keyword = response.split("搜索关键词:")[-1].strip()
            else:
                keyword = response[:50]
            print(f"关键词: {keyword}")

    # 汇总结果
    print("\n" + "=" * 60)
    print("汇总结果")
    print("=" * 60)

    for scenario_name, responses in results.items():
        print(f"\n【{scenario_name}】")
        for i, resp in enumerate(responses):
            print(f"  {i+1}. {resp[:80]}...")

if __name__ == "__main__":
    main()
