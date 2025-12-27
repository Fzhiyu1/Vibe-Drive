<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'
import ChatMessage from './ChatMessage.vue'

const store = useVibeStore()
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// 自动滚动到底部
watch(() => store.chatMessages.length, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
})

function handleSend() {
  if (!inputText.value.trim() || store.chatRunning) return
  store.sendMessage(inputText.value)
  inputText.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-panel" :class="{ open: store.chatPanelOpen }">
    <div class="chat-header">
      <h3>对话</h3>
      <button class="close-btn" @click="store.toggleChatPanel">×</button>
    </div>

    <div ref="messagesContainer" class="chat-messages">
      <div v-if="store.chatMessages.length === 0" class="empty-hint">
        开始与 Vibe Drive 对话吧
      </div>
      <ChatMessage
        v-for="msg in store.chatMessages"
        :key="msg.id"
        :message="msg"
      />
      <div v-if="store.chatRunning" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>
    </div>

    <div class="chat-input">
      <textarea
        v-model="inputText"
        placeholder="输入消息..."
        :disabled="store.chatRunning"
        @keydown="handleKeydown"
      />
      <button
        class="send-btn"
        :disabled="!inputText.trim() || store.chatRunning"
        @click="handleSend"
      >
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  position: fixed;
  top: 0;
  right: -400px;
  width: 400px;
  height: 100vh;
  background: var(--surface-primary);
  border-left: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: right 0.3s ease;
  z-index: 1000;
}

.chat-panel.open {
  right: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid var(--border-color);
}

.chat-header h3 {
  margin: 0;
  font-size: 1rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 0;
  line-height: 1;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.empty-hint {
  text-align: center;
  color: var(--text-secondary);
  padding: 2rem;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 0.5rem;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--text-secondary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

.chat-input {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid var(--border-color);
}

.chat-input textarea {
  flex: 1;
  resize: none;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 0.5rem;
  font-size: 0.9rem;
  min-height: 40px;
  max-height: 100px;
  background: var(--surface-secondary);
  color: var(--text-primary);
}

.send-btn {
  padding: 0.5rem 1rem;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
