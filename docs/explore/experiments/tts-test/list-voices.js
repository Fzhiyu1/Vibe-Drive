/**
 * 列出 Edge TTS 支持的中文语音
 */
import { MsEdgeTTS } from 'msedge-tts'

async function listVoices() {
  console.log('=== Edge TTS 中文语音列表 ===\n')

  const voices = await MsEdgeTTS.getVoices()

  // 筛选中文语音
  const chineseVoices = voices.filter(v => v.Locale.startsWith('zh-'))

  console.log(`共找到 ${chineseVoices.length} 个中文语音:\n`)

  for (const voice of chineseVoices) {
    console.log(`${voice.ShortName}`)
    console.log(`  名称: ${voice.FriendlyName}`)
    console.log(`  性别: ${voice.Gender}`)
    console.log(`  地区: ${voice.Locale}`)
    console.log()
  }
}

listVoices().catch(console.error)
