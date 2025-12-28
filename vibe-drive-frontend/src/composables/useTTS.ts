import { ref } from 'vue'

const TTS_API_BASE = 'http://localhost:3002'

/**
 * TTS 播放（流式版本）
 * 使用 MediaSource API 实现边收边播，降低首字延迟
 */
export function useTTS() {
  const isSpeaking = ref(false)
  let currentAudio: HTMLAudioElement | null = null
  let currentMediaSource: MediaSource | null = null

  /**
   * 安全地追加数据到 SourceBuffer（使用队列避免并发）
   */
  function createBufferQueue(sourceBuffer: SourceBuffer) {
    const queue: Uint8Array[] = []
    let processing = false

    async function processQueue() {
      if (processing || queue.length === 0) return
      processing = true

      while (queue.length > 0) {
        const chunk = queue.shift()!

        // 等待 SourceBuffer 空闲
        while (sourceBuffer.updating) {
          await new Promise(resolve =>
            sourceBuffer.addEventListener('updateend', resolve, { once: true })
          )
        }

        try {
          sourceBuffer.appendBuffer(chunk)
        } catch (e) {
          console.warn('[TTS] appendBuffer failed:', e)
          break
        }
      }

      processing = false
    }

    return {
      push(chunk: Uint8Array) {
        queue.push(chunk)
        processQueue()
      },
      async waitForDrain() {
        while (queue.length > 0 || sourceBuffer.updating) {
          await new Promise(resolve => setTimeout(resolve, 10))
        }
      }
    }
  }

  /**
   * 流式播放 TTS
   */
  async function speak(text: string, options?: { volume?: number }) {
    if (!text?.trim()) {
      console.warn('[TTS] Empty text, skipping')
      return
    }

    // 停止当前播放
    stop()

    console.log('[TTS] Speaking (streaming):', text.substring(0, 30) + '...')

    // 检查浏览器是否支持 MediaSource
    if (!window.MediaSource) {
      console.warn('[TTS] MediaSource not supported, using fallback')
      await fallbackSpeak(text, options)
      return
    }

    const audio = new Audio()
    currentAudio = audio
    audio.volume = options?.volume ?? 0.8

    const mediaSource = new MediaSource()
    currentMediaSource = mediaSource
    audio.src = URL.createObjectURL(mediaSource)

    mediaSource.addEventListener('sourceopen', async () => {
      try {
        const mimeType = 'audio/mpeg'
        if (!MediaSource.isTypeSupported(mimeType)) {
          console.warn('[TTS] MIME type not supported, using fallback')
          await fallbackSpeak(text, options)
          return
        }

        const sourceBuffer = mediaSource.addSourceBuffer(mimeType)
        const bufferQueue = createBufferQueue(sourceBuffer)
        const url = `${TTS_API_BASE}/api/tts/speak?text=${encodeURIComponent(text)}`
        const response = await fetch(url)

        if (!response.ok) {
          throw new Error(`TTS request failed: ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error('Response body is not readable')
        }

        isSpeaking.value = true

        // 边收边播
        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            await bufferQueue.waitForDrain()
            if (mediaSource.readyState === 'open') {
              mediaSource.endOfStream()
            }
            break
          }

          if (mediaSource.readyState === 'open') {
            bufferQueue.push(value)
          }
        }
      } catch (error) {
        console.error('[TTS] Streaming error:', error)
        isSpeaking.value = false
      }
    })

    audio.addEventListener('ended', () => {
      isSpeaking.value = false
      cleanup()
    })

    audio.addEventListener('error', (e) => {
      console.error('[TTS] Audio error:', e)
      isSpeaking.value = false
      cleanup()
    })

    try {
      await audio.play()
    } catch (error) {
      console.error('[TTS] Play failed:', error)
      isSpeaking.value = false
    }
  }

  /**
   * 降级方案：直接使用 URL 播放
   */
  async function fallbackSpeak(text: string, options?: { volume?: number }) {
    const audio = new Audio()
    currentAudio = audio

    const url = `${TTS_API_BASE}/api/tts/speak?text=${encodeURIComponent(text)}`
    audio.src = url
    audio.volume = options?.volume ?? 0.8

    audio.addEventListener('play', () => { isSpeaking.value = true })
    audio.addEventListener('ended', () => { isSpeaking.value = false })
    audio.addEventListener('error', () => { isSpeaking.value = false })

    try {
      await audio.play()
    } catch (error) {
      console.error('[TTS] Fallback play failed:', error)
      isSpeaking.value = false
    }
  }

  function cleanup() {
    if (currentMediaSource && currentMediaSource.readyState === 'open') {
      try { currentMediaSource.endOfStream() } catch (e) { /* ignore */ }
    }
    currentMediaSource = null
  }

  function stop() {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio.currentTime = 0
      currentAudio = null
    }
    cleanup()
    isSpeaking.value = false
  }

  return { isSpeaking, speak, stop }
}
