/**
 * AI 音乐推荐收敛性测试
 *
 * 测试目标：
 * 1. 同一场景多次调用，AI 是否返回相同的搜索关键词
 * 2. 不同场景是否收敛到固定的关键词模式
 *
 * 运行: node test.js
 */

const API_URL = 'http://localhost:8080/api/vibe/analyze';

// 测试场景
const SCENARIOS = {
  '夜间高速': {
    gpsTag: 'highway', weather: 'sunny', speed: 50,
    userMood: 'calm', timeOfDay: 'night', passengerCount: 1, routeType: 'highway'
  },
  '清晨城市': {
    gpsTag: 'urban', weather: 'sunny', speed: 40,
    userMood: 'happy', timeOfDay: 'morning', passengerCount: 1, routeType: 'commute'
  },
  '雨天下午': {
    gpsTag: 'urban', weather: 'rainy', speed: 30,
    userMood: 'calm', timeOfDay: 'afternoon', passengerCount: 1, routeType: 'urban'
  },
  '海边日落': {
    gpsTag: 'coastal', weather: 'sunny', speed: 45,
    userMood: 'excited', timeOfDay: 'evening', passengerCount: 2, routeType: 'scenic'
  },
};

async function callAI(sceneName, env, testNum) {
  const sessionId = `test-${sceneName}-${testNum}-${Date.now()}`;
  const resp = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, environment: env }),
  });

  const data = await resp.json();
  const playResult = data?.data?.plan?.playResult;
  if (playResult) {
    return `${playResult.name} - ${playResult.artist}`;
  }
  return 'NO_RESULT';
}

async function main() {
  console.log('='.repeat(60));
  console.log('AI 音乐推荐收敛性测试');
  console.log('='.repeat(60));

  const results = {};

  for (const [sceneName, env] of Object.entries(SCENARIOS)) {
    console.log(`\n### ${sceneName} ###`);
    results[sceneName] = [];

    for (let i = 1; i <= 3; i++) {
      process.stdout.write(`  测试${i}: `);
      const song = await callAI(sceneName, env, i);
      results[sceneName].push(song);
      console.log(song);
    }
  }

  // 汇总
  console.log('\n' + '='.repeat(60));
  console.log('汇总分析');
  console.log('='.repeat(60));

  for (const [sceneName, songs] of Object.entries(results)) {
    const unique = [...new Set(songs)];
    console.log(`\n【${sceneName}】`);
    songs.forEach((s, i) => console.log(`  ${i+1}. ${s}`));
    console.log(`  → 去重后: ${unique.length} 首不同歌曲`);
  }
}

main().catch(console.error);
