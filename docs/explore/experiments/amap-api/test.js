import 'dotenv/config'

const AMAP_KEY = process.env.AMAP_KEY

if (!AMAP_KEY || AMAP_KEY === 'your_api_key_here') {
  console.error('请先在 .env 文件中配置 AMAP_KEY')
  process.exit(1)
}

// 测试地点
const testLocations = [
  { name: '上海陆家嘴', lng: 121.499763, lat: 31.239703 },
  { name: '沪杭高速', lng: 120.873, lat: 30.456 },
  { name: '杭州西湖', lng: 120.148732, lat: 30.242489 },
  { name: '北京故宫', lng: 116.397026, lat: 39.918058 },
]

// 逆地理编码
async function regeo(lng, lat) {
  const url = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${lng},${lat}&extensions=all`
  const res = await fetch(url)
  return res.json()
}

// 天气查询
async function weather(cityCode) {
  const url = `https://restapi.amap.com/v3/weather/weatherInfo?key=${AMAP_KEY}&city=${cityCode}&extensions=base`
  const res = await fetch(url)
  return res.json()
}

// 主测试
async function main() {
  console.log('=== 高德地图 API 验证实验 ===\n')

  for (const loc of testLocations) {
    console.log(`\n📍 测试地点: ${loc.name}`)
    console.log(`   坐标: ${loc.lng}, ${loc.lat}`)
    console.log('-'.repeat(50))

    try {
      const data = await regeo(loc.lng, loc.lat)

      if (data.status === '1') {
        const r = data.regeocode
        console.log(`   地址: ${r.formatted_address}`)
        console.log(`   省份: ${r.addressComponent.province}`)
        console.log(`   城市: ${r.addressComponent.city}`)
        console.log(`   区县: ${r.addressComponent.district}`)
        console.log(`   街道: ${r.addressComponent.streetNumber?.street || '无'}`)

        // POI
        if (r.pois?.length > 0) {
          console.log(`   附近POI:`)
          r.pois.slice(0, 3).forEach(poi => {
            console.log(`     - ${poi.name} (${poi.type}) ${poi.distance}m`)
          })
        }

        // 道路
        if (r.roads?.length > 0) {
          console.log(`   道路: ${r.roads[0].name}`)
        }

        // 查询天气
        const adcode = r.addressComponent.adcode
        const weatherData = await weather(adcode)
        if (weatherData.status === '1' && weatherData.lives?.[0]) {
          const w = weatherData.lives[0]
          console.log(`   天气: ${w.weather} ${w.temperature}°C`)
        }
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
