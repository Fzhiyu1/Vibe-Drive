/**
 * Edge TTS 语音合成测试
 * 测试不同语音和情感风格
 */
import { MsEdgeTTS, OUTPUT_FORMAT } from 'msedge-tts'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outputDir = path.join(__dirname, 'output')

// 确保输出目录存在
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true })
}

// 测试文本（模拟 Vibe Drive 叙事）
const testTexts = [
  { id: 'calm', text: '夜色温柔，让音乐陪你穿过这片星空' },
  { id: 'warm', text: '雨滴敲窗，就让这首歌温暖归途' },
  { id: 'energetic', text: '好心情配好音乐，继续出发吧' }
]

// 测试语音列表
const voices = [
  { id: 'xiaoxiao', name: 'zh-CN-XiaoxiaoNeural', desc: '晓晓（女，温暖亲切）' },
  { id: 'yunxi', name: 'zh-CN-YunxiNeural', desc: '云希（男，阳光活力）' },
  { id: 'xiaoyi', name: 'zh-CN-XiaoyiNeural', desc: '晓伊（女，甜美可爱）' }
]

async function synthesize(text, voiceName, outputFile) {
  const tts = new MsEdgeTTS()
  await tts.setMetadata(voiceName, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3)

  const { audioStream } = await tts.toStream(text)
  const writeStream = fs.createWriteStream(outputFile)

  return new Promise((resolve, reject) => {
    audioStream.pipe(writeStream)
    writeStream.on('finish', resolve)
    writeStream.on('error', reject)
    audioStream.on('error', reject)
  })
}

async function runTests() {
  console.log('=== Edge TTS 语音测试 ===\n')
  console.log(`输出目录: ${outputDir}\n`)

  const results = []

  for (const voice of voices) {
    console.log(`\n--- 测试语音: ${voice.desc} ---`)

    for (const test of testTexts) {
      const filename = `${voice.id}_${test.id}.mp3`
      const outputPath = path.join(outputDir, filename)

      console.log(`  生成: ${filename}`)
      console.log(`    文本: "${test.text}"`)

      const startTime = Date.now()

      try {
        await synthesize(test.text, voice.name, outputPath)
        const duration = Date.now() - startTime

        if (fs.existsSync(outputPath)) {
          const stats = fs.statSync(outputPath)
          console.log(`    ✅ 成功 (${duration}ms, ${(stats.size / 1024).toFixed(1)}KB)`)
          results.push({ voice: voice.desc, text: test.id, file: filename, duration, size: stats.size })
        }
      } catch (err) {
        console.log(`    ❌ 失败: ${err.message}`)
      }
    }
  }

  // 输出汇总
  console.log('\n\n=== 测试汇总 ===')
  console.log(`生成文件数: ${results.length}`)
  if (results.length > 0) {
    const avgDuration = results.reduce((a, b) => a + b.duration, 0) / results.length
    console.log(`平均延迟: ${avgDuration.toFixed(0)}ms`)
  }
  console.log(`\n请到 ${outputDir} 目录播放音频文件，评估语音质量。`)
}

runTests().catch(console.error)
