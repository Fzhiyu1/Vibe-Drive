<script setup lang="ts">
import { useVoiceInput } from '@/composables/useVoiceInput'
import { useVibeStore } from '@/stores/vibeStore'

const { isRecording, isProcessing, error, startRecording, stopRecording } = useVoiceInput()
const store = useVibeStore()

async function handleMouseDown() {
  await startRecording()
}

async function handleMouseUp() {
  const text = await stopRecording()
  if (text) {
    store.sendMessage(text)
  }
}
</script>

<template>
  <div class="voice-input">
    <button
      class="voice-btn"
      :class="{ recording: isRecording, processing: isProcessing }"
      @mousedown="handleMouseDown"
      @mouseup="handleMouseUp"
      @mouseleave="isRecording && handleMouseUp()"
      :disabled="isProcessing"
    >
      <span v-if="isProcessing" class="icon">⏳</span>
      <span v-else-if="isRecording" class="icon">🎙️</span>
      <span v-else class="icon">🎤</span>
    </button>
    <p class="hint">
      {{ isProcessing ? '识别中...' : isRecording ? '松开发送' : '按住说话' }}
    </p>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.voice-input {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.voice-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.voice-btn:hover {
  background: var(--bg-tertiary);
}

.voice-btn.recording {
  background: #ef4444;
  border-color: #ef4444;
  animation: pulse 1s infinite;
}

.voice-btn.processing {
  opacity: 0.6;
  cursor: wait;
}

.icon {
  font-size: 1.5rem;
}

.hint {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.error {
  font-size: 0.75rem;
  color: #ef4444;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
</style>
