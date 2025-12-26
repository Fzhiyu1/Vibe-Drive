import express from 'express'
import cors from 'cors'
import { synthesize } from './ttsService.js'
import { config } from './config.js'

const app = express()

app.use(cors({
  origin: ['http://localhost:5173', 'http://localhost:3000'],
  credentials: true
}))

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'vibe-drive-tts' })
})

// TTS 接口
app.get('/api/tts/speak', async (req, res) => {
  const { text, voice } = req.query

  if (!text) {
    return res.status(400).json({ error: 'text is required' })
  }

  try {
    console.log(`[TTS] Synthesizing: "${text.substring(0, 30)}..."`)

    const audioStream = await synthesize(text, { voice })

    res.setHeader('Content-Type', 'audio/mpeg')
    res.setHeader('Transfer-Encoding', 'chunked')
    res.setHeader('Cache-Control', 'no-cache')

    audioStream.pipe(res)
  } catch (error) {
    console.error('[TTS] Error:', error.message)
    res.status(500).json({ error: error.message })
  }
})

app.listen(config.port, () => {
  console.log(`[TTS] Service running on http://localhost:${config.port}`)
})
