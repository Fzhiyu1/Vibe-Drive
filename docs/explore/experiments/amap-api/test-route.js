import 'dotenv/config'

const AMAP_KEY = process.env.AMAP_KEY

if (!AMAP_KEY) {
  console.error('请先配置 AMAP_KEY')
  process.exit(1)
}

// 测试路线：上海 → 杭州
const routes = [
  {
    name: '上海→杭州（G60沪昆高速）',
    origin: '121.473701,31.230416',  // 上海人民广场
    destination: '120.155070,30.274084'  // 杭州武林广场
  }
]

// 路线规划 API
async function driving(origin, destination) {
  const url = `https://restapi.amap.com/v3/direction/driving?key=${AMAP_KEY}&origin=${origin}&destination=${destination}&extensions=all`
  const res = await fetch(url)
  return res.json()
}

async function main() {
  console.log('=== 高德路线规划 API 测试 ===\n')

  for (const route of routes) {
    console.log(`📍 路线: ${route.name}`)
    console.log('-'.repeat(50))

    try {
      const data = await driving(route.origin, route.destination)

      if (data.status === '1' && data.route?.paths?.length > 0) {
        const path = data.route.paths[0]

        console.log(`   距离: ${(path.distance / 1000).toFixed(1)} km`)
        console.log(`   时间: ${Math.round(path.duration / 60)} 分钟`)
        console.log(`   路段数: ${path.steps?.length || 0}`)

        // 显示前5个路段
        console.log(`\n   路段详情（前5个）:`)
        path.steps?.slice(0, 5).forEach((step, i) => {
          console.log(`   ${i + 1}. ${step.road || '无名路'} - ${step.instruction}`)
        })

        // 提取坐标点（用于模拟行驶）
        const polyline = path.steps?.map(s => s.polyline).join(';')
        const points = polyline?.split(';') || []
        console.log(`\n   坐标点数量: ${points.length}`)
        console.log(`   前3个坐标: ${points.slice(0, 3).join(' → ')}`)

      } else {
        console.log(`   错误: ${data.info}`)
      }
    } catch (e) {
      console.log(`   请求失败: ${e.message}`)
    }
  }

  console.log('\n=== 测试完成 ===')
}

main()
