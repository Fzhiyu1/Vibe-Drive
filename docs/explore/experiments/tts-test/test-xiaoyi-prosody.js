/**
 * 测试晓伊（XiaoyiNeural）prosody 情感模拟
 * 通过调整语速(rate)和音调(pitch)模拟不同情感
 */
import { MsEdgeTTS, OUTPUT_FORMAT } from 'msedge-tts'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outputDir = path.join(__dirname, 'output', 'xiaoyi-prosody')

// 确保输出目录存在
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true })
}

// 测试文本
const testText = '夜色温柔，让音乐陪你穿过这片星空'

// 情感配置：通过 prosody 模拟
const emotions = [
  { id: 'default', name: '默认', rate: '0%', pitch: '+0Hz' },
  { id: 'calm', name: 'CALM 平静', rate: '-10%', pitch: '-50Hz' },
  { id: 'warm', name: 'WARM 温暖', rate: '-5%', pitch: '+0Hz' },
  { id: 'energetic', name: 'ENERGETIC 活力', rate: '+10%', pitch: '+50Hz' },
  { id: 'romantic', name: 'ROMANTIC 浪漫', rate: '-15%', pitch: '-30Hz' },
  { id: 'adventurous', name: 'ADVENTUROUS 冒险', rate: '+5%', pitch: '+30Hz' }
]

async function testEmotion(emotion) {
  const tts = new MsEdgeTTS()
  await tts.setMetadata('zh-CN-XiaoyiNeural', OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3)

  const filename = `xiaoyi_${emotion.id}.mp3`
  const outputPath = path.join(outputDir, filename)

  const { audioStream } = await tts.toStream(testText, {
    rate: emotion.rate,
    pitch: emotion.pitch
  })

  const writeStream = fs.createWriteStream(outputPath)

  return new Promise((resolve, reject) => {
    audioStream.pipe(writeStream)
    writeStream.on('finish', () => resolve({ ...emotion, success: true, file: filename }))
    writeStream.on('error', (err) => resolve({ ...emotion, success: false, error: err.message }))
    audioStream.on('error', (err) => resolve({ ...emotion, success: false, error: err.message }))
  })
}

async function runTests() {
  console.log('=== 晓伊 Prosody 情感测试 ===\n')
  console.log(`测试文本: "${testText}"`)
  console.log(`输出目录: ${outputDir}\n`)

  const results = []

  for (const emotion of emotions) {
    console.log(`测试: ${emotion.name}`)
    console.log(`  rate: ${emotion.rate}, pitch: ${emotion.pitch}`)

    try {
      const result = await testEmotion(emotion)
      if (result.success) {
        const stats = fs.statSync(path.join(outputDir, result.file))
        console.log(`  ✅ 成功 (${(stats.size / 1024).toFixed(1)}KB)\n`)
        results.push({ ...result, size: stats.size })
      } else {
        console.log(`  ❌ 失败: ${result.error}\n`)
        results.push(result)
      }
    } catch (err) {
      console.log(`  ❌ 失败: ${err.message}\n`)
      results.push({ ...emotion, success: false, error: err.message })
    }
  }

  // 汇总
  console.log('=== 测试汇总 ===\n')
  const successful = results.filter(r => r.success)
  console.log(`成功: ${successful.length}/${emotions.length}`)
  console.log(`\n请到 ${outputDir} 目录试听效果。`)
}

runTests().catch(console.error)
