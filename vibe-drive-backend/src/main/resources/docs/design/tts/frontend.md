# 语音系统前端实现

## 模块结构

```
src/
├── services/
│   └── ttsService.ts    # TTS 服务调用
├── composables/
│   └── useAudioPlayer.ts # 音频播放器
└── stores/
    └── vibeStore.ts     # 监听叙事事件
```

## ttsService.ts

```typescript
const TTS_API = '/api/tts/synthesize'
const DEFAULT_VOICE = 'zh-CN-XiaoyiNeural'

export async function synthesize(text: string): Promise<ReadableStream<Uint8Array>> {
  const response = await fetch(TTS_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, voice: DEFAULT_VOICE })
  })

  if (!response.ok) {
    throw new Error(`TTS 请求失败: ${response.status}`)
  }

  return response.body!
}
```

## useAudioPlayer.ts

```typescript
import { ref } from 'vue'

export function useAudioPlayer() {
  const isPlaying = ref(false)
  const audioContext = new AudioContext()

  async function playStream(stream: ReadableStream<Uint8Array>) {
    isPlaying.value = true
    const reader = stream.getReader()
    const chunks: Uint8Array[] = []

    // 收集所有 chunks
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      chunks.push(value)
    }

    // 合并并解码
    const audioData = mergeChunks(chunks)
    const audioBuffer = await audioContext.decodeAudioData(audioData.buffer)

    // 播放
    const source = audioContext.createBufferSource()
    source.buffer = audioBuffer
    source.connect(audioContext.destination)
    source.onended = () => { isPlaying.value = false }
    source.start()
  }

  function mergeChunks(chunks: Uint8Array[]): Uint8Array {
    const totalLength = chunks.reduce((acc, chunk) => acc + chunk.length, 0)
    const result = new Uint8Array(totalLength)
    let offset = 0
    for (const chunk of chunks) {
      result.set(chunk, offset)
      offset += chunk.length
    }
    return result
  }

  return { isPlaying, playStream }
}
```

## vibeStore 集成

```typescript
// 在 vibeStore.ts 中添加

import { synthesize } from '@/services/ttsService'
import { useAudioPlayer } from '@/composables/useAudioPlayer'

const { playStream } = useAudioPlayer()

// 监听叙事工具完成
function applyToolResult(toolName: string, resultJson: string) {
  // ... 现有代码 ...

  if (toolName === 'generateNarrative') {
    const narrative = JSON.parse(resultJson)
    playNarrative(narrative.text)
  }
}

async function playNarrative(text: string) {
  try {
    const stream = await synthesize(text)
    await playStream(stream)
  } catch (e) {
    console.error('[TTS] 播放失败:', e)
  }
}
```
