import { ref } from 'vue'

// 生产环境通过 nginx 代理，开发环境直连
const TTS_API_BASE = import.meta.env.DEV ? 'http://localhost:3002' : '/tts'

/**
 * TTS 播放（流式版本，支持语音队列）
 */
export function useTTS() {
  const isSpeaking = ref(false)
  let currentAudio: HTMLAudioElement | null = null
  let currentMediaSource: MediaSource | null = null

  // 语音队列
  const speechQueue: Array<{ text: string; options?: { volume?: number } }> = []
  let isProcessingQueue = false

  /**
   * 安全地追加数据到 SourceBuffer
   */
  function createBufferQueue(sourceBuffer: SourceBuffer) {
    const queue: Uint8Array[] = []
    let processing = false

    async function processBufferQueue() {
      if (processing || queue.length === 0) return
      processing = true

      while (queue.length > 0) {
        const chunk = queue.shift()!
        while (sourceBuffer.updating) {
          await new Promise(resolve =>
            sourceBuffer.addEventListener('updateend', resolve, { once: true })
          )
        }
        try {
          sourceBuffer.appendBuffer(chunk as unknown as ArrayBuffer)
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
        processBufferQueue()
      },
      async waitForDrain() {
        while (queue.length > 0 || sourceBuffer.updating) {
          await new Promise(resolve => setTimeout(resolve, 10))
        }
      }
    }
  }

  /**
   * 加入语音队列
   */
  function speak(text: string, options?: { volume?: number }) {
    if (!text?.trim()) return

    speechQueue.push({ text, options })
    console.log('[TTS] 加入队列:', text.substring(0, 30) + '...', '队列长度:', speechQueue.length)
    processSpeechQueue()
  }

  /**
   * 处理语音队列
   */
  async function processSpeechQueue() {
    if (isProcessingQueue || speechQueue.length === 0) return
    isProcessingQueue = true

    while (speechQueue.length > 0) {
      const item = speechQueue.shift()!
      await speakImmediate(item.text, item.options)
    }

    isProcessingQueue = false
  }

  /**
   * 立即播放（返回 Promise，播放结束后 resolve）
   */
  function speakImmediate(text: string, options?: { volume?: number }): Promise<void> {
    return new Promise((resolve) => {
      console.log('[TTS] 播放:', text.substring(0, 30) + '...')

      if (!window.MediaSource) {
        fallbackSpeak(text, options).then(resolve)
        return
      }

      const audio = new Audio()
      currentAudio = audio
      audio.volume = options?.volume ?? 0.8

      const mediaSource = new MediaSource()
      currentMediaSource = mediaSource
      audio.src = URL.createObjectURL(mediaSource)

      // 播放结束时 resolve
      audio.addEventListener('ended', () => {
        isSpeaking.value = false
        cleanup()
        resolve()
      })

      audio.addEventListener('error', () => {
        isSpeaking.value = false
        cleanup()
        resolve()
      })

      mediaSource.addEventListener('sourceopen', async () => {
        try {
          const mimeType = 'audio/mpeg'
          if (!MediaSource.isTypeSupported(mimeType)) {
            await fallbackSpeak(text, options)
            resolve()
            return
          }

          const sourceBuffer = mediaSource.addSourceBuffer(mimeType)
          const bufferQueue = createBufferQueue(sourceBuffer)
          const url = `${TTS_API_BASE}/api/tts/speak?text=${encodeURIComponent(text)}`
          const response = await fetch(url)

          if (!response.ok) throw new Error(`TTS failed: ${response.status}`)

          const reader = response.body?.getReader()
          if (!reader) throw new Error('Response not readable')

          isSpeaking.value = true

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
          console.error('[TTS] Error:', error)
          isSpeaking.value = false
          resolve()
        }
      })

      audio.play().catch(() => {
        isSpeaking.value = false
        resolve()
      })
    })
  }

  /**
   * 降级方案
   */
  function fallbackSpeak(text: string, options?: { volume?: number }): Promise<void> {
    return new Promise((resolve) => {
      const audio = new Audio()
      currentAudio = audio
      audio.src = `${TTS_API_BASE}/api/tts/speak?text=${encodeURIComponent(text)}`
      audio.volume = options?.volume ?? 0.8

      audio.addEventListener('play', () => { isSpeaking.value = true })
      audio.addEventListener('ended', () => { isSpeaking.value = false; resolve() })
      audio.addEventListener('error', () => { isSpeaking.value = false; resolve() })

      audio.play().catch(() => resolve())
    })
  }

  function cleanup() {
    if (currentMediaSource?.readyState === 'open') {
      try { currentMediaSource.endOfStream() } catch (e) { /* ignore */ }
    }
    currentMediaSource = null
  }

  function stop() {
    // 清空队列
    speechQueue.length = 0
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
