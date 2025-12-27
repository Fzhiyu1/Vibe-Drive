import { ref } from 'vue'

const TTS_API_BASE = 'http://localhost:3002'

/**
 * 流式 TTS 播放
 * 使用 MediaSource API 实现边收边播，降低首字延迟
 */
export function useStreamingTTS() {
  const isSpeaking = ref(false)
  let currentAudio: HTMLAudioElement | null = null
  let currentMediaSource: MediaSource | null = null

  /**
   * 等待 SourceBuffer 更新完成
   */
  function waitForUpdateEnd(sourceBuffer: SourceBuffer): Promise<void> {
    return new Promise((resolve) => {
      if (!sourceBuffer.updating) {
        resolve()
        return
      }
      sourceBuffer.addEventListener('updateend', () => resolve(), { once: true })
    })
  }

  /**
   * 流式播放 TTS
   */
  async function speak(text: string, options?: { volume?: number }) {
    if (!text?.trim()) {
      console.warn('[StreamingTTS] Empty text, skipping')
      return
    }

    // 停止当前播放
    stop()

    console.log('[StreamingTTS] Speaking:', text.substring(0, 30) + '...')

    const audio = new Audio()
    currentAudio = audio
    audio.volume = options?.volume ?? 0.8

    // 检查浏览器是否支持 MediaSource
    if (!window.MediaSource) {
      console.warn('[StreamingTTS] MediaSource not supported, falling back to direct URL')
      await fallbackSpeak(text, options)
      return
    }

    const mediaSource = new MediaSource()
    currentMediaSource = mediaSource
    audio.src = URL.createObjectURL(mediaSource)

    mediaSource.addEventListener('sourceopen', async () => {
      try {
        // MP3 MIME type
        const mimeType = 'audio/mpeg'
        if (!MediaSource.isTypeSupported(mimeType)) {
          console.warn('[StreamingTTS] MIME type not supported:', mimeType)
          await fallbackSpeak(text, options)
          return
        }

        const sourceBuffer = mediaSource.addSourceBuffer(mimeType)

        // 发起流式请求
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
            // 等待最后一个 buffer 更新完成
            await waitForUpdateEnd(sourceBuffer)
            if (mediaSource.readyState === 'open') {
              mediaSource.endOfStream()
            }
            break
          }

          // 追加数据到 buffer
          await waitForUpdateEnd(sourceBuffer)
          if (mediaSource.readyState === 'open') {
            sourceBuffer.appendBuffer(value)
          }
        }
      } catch (error) {
        console.error('[StreamingTTS] Error:', error)
        isSpeaking.value = false
      }
    })

    // 监听播放事件
    audio.addEventListener('play', () => {
      isSpeaking.value = true
    })

    audio.addEventListener('ended', () => {
      isSpeaking.value = false
      cleanup()
    })

    audio.addEventListener('error', (e) => {
      console.error('[StreamingTTS] Audio error:', e)
      isSpeaking.value = false
      cleanup()
    })

    // 开始播放
    try {
      await audio.play()
    } catch (error) {
      console.error('[StreamingTTS] Play failed:', error)
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

    audio.addEventListener('play', () => {
      isSpeaking.value = true
    })

    audio.addEventListener('ended', () => {
      isSpeaking.value = false
    })

    audio.addEventListener('error', (e) => {
      console.error('[StreamingTTS] Fallback audio error:', e)
      isSpeaking.value = false
    })

    try {
      await audio.play()
    } catch (error) {
      console.error('[StreamingTTS] Fallback play failed:', error)
      isSpeaking.value = false
    }
  }

  /**
   * 清理资源
   */
  function cleanup() {
    if (currentMediaSource && currentMediaSource.readyState === 'open') {
      try {
        currentMediaSource.endOfStream()
      } catch (e) {
        // ignore
      }
    }
    currentMediaSource = null
  }

  /**
   * 停止播放
   */
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
