import { ref } from 'vue'

// 通过 Vite 代理转发，避免 CORS 问题
const WHISPER_API = '/asr'

export function useVoiceInput() {
  const isRecording = ref(false)
  const isProcessing = ref(false)
  const transcript = ref('')
  const error = ref<string | null>(null)

  let mediaRecorder: MediaRecorder | null = null
  let audioChunks: Blob[] = []

  // 开始录音
  async function startRecording() {
    try {
      error.value = null
      audioChunks = []

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          audioChunks.push(e.data)
        }
      }

      mediaRecorder.start()
      isRecording.value = true
    } catch (err) {
      error.value = '无法访问麦克风'
      console.error('录音失败:', err)
    }
  }

  // 停止录音并转写
  async function stopRecording(): Promise<string> {
    return new Promise((resolve) => {
      if (!mediaRecorder || mediaRecorder.state === 'inactive') {
        resolve('')
        return
      }

      mediaRecorder.onstop = async () => {
        isRecording.value = false
        isProcessing.value = true

        try {
          const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
          const text = await transcribe(audioBlob)
          transcript.value = text
          resolve(text)
        } catch (err) {
          error.value = '转写失败'
          console.error('转写失败:', err)
          resolve('')
        } finally {
          isProcessing.value = false
        }
      }

      mediaRecorder.stop()
      mediaRecorder.stream.getTracks().forEach(track => track.stop())
    })
  }

  // 调用 Whisper API 转写
  async function transcribe(audioBlob: Blob): Promise<string> {
    const formData = new FormData()
    formData.append('audio_file', audioBlob, 'recording.webm')

    const response = await fetch(`${WHISPER_API}?language=zh&output=json`, {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error(`Whisper API 错误: ${response.status}`)
    }

    const result = await response.json()
    return result.text || ''
  }

  return {
    isRecording,
    isProcessing,
    transcript,
    error,
    startRecording,
    stopRecording
  }
}
