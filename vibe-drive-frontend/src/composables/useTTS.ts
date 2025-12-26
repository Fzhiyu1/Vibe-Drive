import { ref } from 'vue'

const TTS_API_BASE = 'http://localhost:3002'

export function useTTS() {
  const isSpeaking = ref(false)
  const ttsAudio = new Audio()

  ttsAudio.addEventListener('play', () => {
    isSpeaking.value = true
  })

  ttsAudio.addEventListener('ended', () => {
    isSpeaking.value = false
  })

  ttsAudio.addEventListener('error', (e) => {
    console.error('[TTS] Audio error:', e)
    isSpeaking.value = false
  })

  async function speak(text: string, options?: { volume?: number }) {
    if (!text?.trim()) {
      console.warn('[TTS] Empty text, skipping')
      return
    }

    if (isSpeaking.value) {
      stop()
    }

    const params = new URLSearchParams({ text })
    const url = `${TTS_API_BASE}/api/tts/speak?${params.toString()}`

    console.log('[TTS] Speaking:', text.substring(0, 30) + '...')

    ttsAudio.src = url
    ttsAudio.volume = options?.volume ?? 0.8

    try {
      await ttsAudio.play()
    } catch (error) {
      console.error('[TTS] Play failed:', error)
      isSpeaking.value = false
    }
  }

  function stop() {
    ttsAudio.pause()
    ttsAudio.currentTime = 0
    isSpeaking.value = false
  }

  return { isSpeaking, speak, stop }
}
