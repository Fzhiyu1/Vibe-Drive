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
    <!-- 主智能体运行状态 -->
    <div v-if="store.masterRunning" class="thinking-indicator">
      <span class="thinking-icon">🧠</span>
      <span class="thinking-dot"></span>
      <span class="thinking-dot"></span>
      <span class="thinking-dot"></span>
      <span class="thinking-text">思考中</span>
    </div>

    <button
      v-else
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
      {{ store.masterRunning ? '' : isProcessing ? '识别中...' : isRecording ? '松开发送' : '按住说话' }}
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

/* 思考中动画 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  background: var(--bg-secondary);
  border-radius: 2rem;
  border: 2px solid var(--accent-primary);
}

.thinking-icon {
  font-size: 1.5rem;
  margin-right: 0.25rem;
  animation: pulse 1.5s infinite;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  background: var(--accent-primary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.thinking-dot:nth-child(1) { animation-delay: -0.32s; }
.thinking-dot:nth-child(2) { animation-delay: -0.16s; }
.thinking-dot:nth-child(3) { animation-delay: 0s; }

.thinking-text {
  margin-left: 0.5rem;
  font-size: 0.875rem;
  color: var(--accent-primary);
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
