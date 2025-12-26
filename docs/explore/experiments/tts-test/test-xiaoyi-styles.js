/**
 * 测试晓伊（XiaoyiNeural）支持的情感风格
 */
import { MsEdgeTTS, OUTPUT_FORMAT } from 'msedge-tts'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outputDir = path.join(__dirname, 'output', 'xiaoyi-styles')

// 确保输出目录存在
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true })
}

// 测试文本
const testText = '夜色温柔，让音乐陪你穿过这片星空'

// 晓晓支持的风格（测试晓伊是否也支持）
const styles = [
  'default',
  'cheerful',
  'sad',
  'angry',
  'fearful',
  'disgruntled',
  'serious',
  'affectionate',
  'gentle',
  'lyrical'
]

async function testStyle(style) {
  const tts = new MsEdgeTTS()
  await tts.setMetadata('zh-CN-XiaoyiNeural', OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3)

  const filename = `xiaoyi_${style}.mp3`
  const outputPath = path.join(outputDir, filename)

  // 使用 SSML 设置风格
  let ssmlText
  if (style === 'default') {
    ssmlText = testText
  } else {
    ssmlText = `<mstts:express-as style="${style}">${testText}</mstts:express-as>`
  }

  const { audioStream } = await tts.toStream(ssmlText)
  const writeStream = fs.createWriteStream(outputPath)

  return new Promise((resolve, reject) => {
    audioStream.pipe(writeStream)
    writeStream.on('finish', () => resolve({ style, success: true, file: filename }))
    writeStream.on('error', (err) => resolve({ style, success: false, error: err.message }))
    audioStream.on('error', (err) => resolve({ style, success: false, error: err.message }))
  })
}

async function runTests() {
  console.log('=== 晓伊情感风格测试 ===\n')
  console.log(`测试文本: "${testText}"`)
  console.log(`输出目录: ${outputDir}\n`)

  const results = []

  for (const style of styles) {
    console.log(`测试风格: ${style}...`)

    try {
      const result = await testStyle(style)
      if (result.success) {
        const stats = fs.statSync(path.join(outputDir, result.file))
        console.log(`  ✅ 成功 (${(stats.size / 1024).toFixed(1)}KB)`)
        results.push({ ...result, size: stats.size })
      } else {
        console.log(`  ❌ 失败: ${result.error}`)
        results.push(result)
      }
    } catch (err) {
      console.log(`  ❌ 失败: ${err.message}`)
      results.push({ style, success: false, error: err.message })
    }
  }

  // 汇总
  console.log('\n=== 测试汇总 ===')
  const supported = results.filter(r => r.success)
  const failed = results.filter(r => !r.success)

  console.log(`\n支持的风格 (${supported.length}):`)
  supported.forEach(r => console.log(`  - ${r.style}`))

  if (failed.length > 0) {
    console.log(`\n不支持的风格 (${failed.length}):`)
    failed.forEach(r => console.log(`  - ${r.style}: ${r.error}`))
  }

  console.log(`\n请到 ${outputDir} 目录试听，确认风格效果。`)
}

runTests().catch(console.error)
