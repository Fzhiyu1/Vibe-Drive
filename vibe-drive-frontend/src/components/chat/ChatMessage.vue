<script setup lang="ts">
import type { ChatMessage } from '@/types/api'

defineProps<{
  message: ChatMessage
}>()
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="message-bubble">
      <div class="message-content">{{ message.content || '...' }}</div>
      <div v-if="message.toolCalls?.length" class="tool-calls">
        <div v-for="call in message.toolCalls" :key="call.toolName" class="tool-call">
          <span class="tool-name">{{ call.toolName }}</span>
          <span v-if="call.output" class="tool-status">✓</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: 0.75rem;
}

.chat-message.user {
  justify-content: flex-end;
}

.chat-message.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  word-break: break-word;
}

.user .message-bubble {
  background: var(--accent);
  color: white;
  border-bottom-right-radius: 4px;
}

.assistant .message-bubble {
  background: var(--surface-secondary);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}

.message-content {
  white-space: pre-wrap;
  font-size: 0.9rem;
  line-height: 1.5;
}

.tool-calls {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.assistant .tool-calls {
  border-top-color: var(--border-color);
}

.tool-call {
  font-size: 0.75rem;
  padding: 0.125rem 0.5rem;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.tool-name {
  opacity: 0.8;
}

.tool-status {
  color: #4caf50;
}
</style>
