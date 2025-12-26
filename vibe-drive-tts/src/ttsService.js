import { MsEdgeTTS, OUTPUT_FORMAT } from 'msedge-tts'
import https from 'https'
import { config } from './config.js'

// Edge 浏览器 User-Agent
const EDGE_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0'

// 自定义 Agent 设置 User-Agent
class EdgeAgent extends https.Agent {
  constructor() {
    super({ keepAlive: true })
  }

  createConnection(options, callback) {
    options.headers = options.headers || {}
    options.headers['User-Agent'] = EDGE_USER_AGENT
    return super.createConnection(options, callback)
  }
}

export async function synthesize(text, options = {}) {
  const voice = options.voice || config.defaultVoice

  try {
    const tts = new MsEdgeTTS(new EdgeAgent())
    await tts.setMetadata(voice, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3)

    const { audioStream } = await tts.toStream(text)
    return audioStream
  } catch (err) {
    console.error('[TTS] Synthesize error:', err)
    throw new Error(err?.message || String(err) || 'TTS synthesis failed')
  }
}
