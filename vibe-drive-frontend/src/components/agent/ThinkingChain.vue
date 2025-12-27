<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()
const logContainer = ref<HTMLElement | null>(null)
const inputText = ref('')

function toggleExpand() {
  store.chainExpanded = !store.chainExpanded
}

function handleSend() {
  if (!inputText.value.trim() || store.agentRunning) return
  store.sendMessage(inputText.value)
  inputText.value = ''
}

function handleQuickAnalyze() {
  if (store.agentRunning) return
  store.sendMessage('根据当前环境帮我编排氛围')
}

function formatTime(timestamp: number) {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function formatJson(obj: unknown): string {
  if (!obj) return ''
  try {
    if (typeof obj === 'string') {
      return obj.length > 200 ? obj.slice(0, 200) + '...' : obj
    }
    const str = JSON.stringify(obj, null, 2)
    return str.length > 300 ? str.slice(0, 300) + '...' : str
  } catch {
    return String(obj)
  }
}

function getAgentLabel(agent?: string) {
  return agent === 'master' ? 'MASTER' : agent === 'vibe' ? 'VIBE' : 'AGENT'
}

// 自动滚动到底部
watch(() => store.thinkingChain, async () => {
  await nextTick()
  if (logContainer.value) {
    logContainer.value.scrollTop = logContainer.value.scrollHeight
  }
}, { deep: true })
</script>

<template>
  <div class="thinking-chain">
    <div class="header" @click="toggleExpand">
      <span class="title">Agent 思维链</span>
      <span v-if="store.agentRunning" class="status running">
        <span class="dot"></span> 运行中
      </span>
      <span v-else-if="store.thinkingChain.length > 0" class="status done">
        已完成
      </span>
      <span class="toggle">{{ store.chainExpanded ? '▼' : '▲' }}</span>
    </div>

    <div v-if="store.chainExpanded" ref="logContainer" class="log-container">
      <template v-for="(step, index) in store.thinkingChain" :key="index">
        <!-- 用户输入 -->
        <div v-if="step.type === 'user_input'" class="log-block user-input">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[USER]</span>
            <span class="label">{{ step.content }}</span>
          </div>
        </div>

        <!-- AI 回复 -->
        <div v-else-if="step.type === 'ai_response'" class="log-block ai-response">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[{{ getAgentLabel(step.agent) }}]</span>
          </div>
          <div class="log-content ai-text">{{ step.content }}</div>
        </div>

        <!-- 调用子智能体 -->
        <div v-else-if="step.type === 'agent_call'" class="log-block agent-call">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[AGENT]</span>
            <span class="label">→ 调用氛围智能体</span>
          </div>
        </div>

        <!-- Thinking -->
        <div v-else-if="step.type === 'thinking'" class="log-block thinking">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[THINK]</span>
            <span class="label">{{ getAgentLabel(step.agent) }} 思考中...</span>
          </div>
          <div class="log-content">{{ step.content }}<span class="cursor">▌</span></div>
        </div>

        <!-- Tool Start -->
        <div v-else-if="step.type === 'tool_start'" class="log-block tool-start" :class="{ 'vibe-tool': step.agent === 'vibe' }">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[{{ step.agent === 'vibe' ? 'VIBE' : 'TOOL' }}]</span>
            <span class="label">调用 <span class="tool-name">{{ step.toolName }}</span></span>
          </div>
          <div v-if="step.toolInput" class="log-content code">
            <span class="input-label">输入:</span>
            <pre>{{ formatJson(step.toolInput) }}</pre>
          </div>
        </div>

        <!-- Tool End -->
        <div v-else-if="step.type === 'tool_end'" class="log-block tool-end" :class="{ 'vibe-tool': step.agent === 'vibe' }">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[{{ step.agent === 'vibe' ? 'VIBE' : 'DONE' }}]</span>
            <span class="label"><span class="tool-name">{{ step.toolName }}</span> 完成</span>
          </div>
          <div v-if="step.toolOutput" class="log-content code">
            <span class="output-label">输出:</span>
            <pre>{{ formatJson(step.toolOutput) }}</pre>
          </div>
        </div>

        <!-- Complete -->
        <div v-else-if="step.type === 'complete'" class="log-block complete" :class="{ 'vibe-complete': step.agent === 'vibe' }">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[{{ step.agent === 'vibe' ? 'VIBE' : 'OK' }}]</span>
            <span class="label">{{ step.content }}</span>
          </div>
        </div>

        <!-- Error -->
        <div v-else-if="step.type === 'error'" class="log-block error">
          <div class="log-header">
            <span class="time">{{ formatTime(step.timestamp) }}</span>
            <span class="prefix">[ERROR]</span>
            <span class="label">{{ step.content }}</span>
          </div>
        </div>
      </template>

      <div v-if="store.thinkingChain.length === 0" class="empty-log">
        <span class="prompt">$</span> 输入消息开始对话...
      </div>
    </div>

    <!-- 输入区域 -->
    <div v-if="store.chainExpanded" class="input-area">
      <input
        v-model="inputText"
        placeholder="输入消息..."
        :disabled="store.agentRunning"
        @keydown.enter="handleSend"
      />
      <button class="quick-btn" @click="handleQuickAnalyze" :disabled="store.agentRunning">
        ⚡ 编排
      </button>
      <button class="send-btn" @click="handleSend" :disabled="!inputText.trim() || store.agentRunning">
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.thinking-chain {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.header {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem;
  cursor: pointer;
  user-select: none;
  background: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.title {
  font-weight: 600;
  color: #e0e0e0;
}

.status {
  margin-left: 0.75rem;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.status.running { color: #4ec9b0; }
.status.done { color: #808080; }

.dot {
  width: 6px;
  height: 6px;
  background: #4ec9b0;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.toggle {
  margin-left: auto;
  color: #808080;
}

.log-container {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
  background: #1e1e1e;
  color: #d4d4d4;
  font-size: 0.8rem;
  line-height: 1.4;
}

.log-block {
  margin-bottom: 0.5rem;
  border-left: 3px solid #404040;
  padding-left: 0.5rem;
}

.log-block.thinking { border-color: #569cd6; }
.log-block.tool-start { border-color: #dcdcaa; }
.log-block.tool-end { border-color: #4ec9b0; }
.log-block.vibe-tool { border-color: #c678dd; }
.log-block.vibe-complete { border-color: #c678dd; }
.log-block.complete { border-color: #b5cea8; }
.log-block.error { border-color: #f14c4c; }
.log-block.user-input { border-color: #61afef; }
.log-block.ai-response { border-color: #98c379; }
.log-block.agent-call { border-color: #c678dd; }

.log-header {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  padding: 0.25rem 0;
}

.time {
  color: #6a9955;
  flex-shrink: 0;
}

.prefix {
  font-weight: bold;
  flex-shrink: 0;
}

.thinking .prefix { color: #569cd6; }
.tool-start .prefix { color: #dcdcaa; }
.tool-end .prefix { color: #4ec9b0; }
.vibe-tool .prefix { color: #c678dd; }
.vibe-complete .prefix { color: #c678dd; }
.complete .prefix { color: #b5cea8; }
.error .prefix { color: #f14c4c; }
.user-input .prefix { color: #61afef; }
.ai-response .prefix { color: #98c379; }
.agent-call .prefix { color: #c678dd; }

.label { color: #d4d4d4; }

.tool-name {
  color: #ce9178;
  font-weight: 500;
}

.log-content {
  padding: 0.25rem 0;
  color: #9cdcfe;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-content.code {
  background: #252526;
  padding: 0.5rem;
  border-radius: 4px;
  margin-top: 0.25rem;
}

.log-content pre {
  margin: 0;
  font-size: 0.75rem;
  color: #ce9178;
}

.input-label, .output-label {
  color: #808080;
  font-size: 0.7rem;
  display: block;
  margin-bottom: 0.25rem;
}

.cursor {
  color: #569cd6;
  animation: cursor-blink 0.8s step-end infinite;
}

@keyframes cursor-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.empty-log {
  padding: 0.5rem;
  color: #808080;
}

.prompt {
  color: #4ec9b0;
  margin-right: 0.5rem;
}

/* AI 回复文本 */
.ai-text {
  color: #98c379;
  font-style: normal;
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  background: #2d2d2d;
  border-top: 1px solid #404040;
}

.input-area input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #404040;
  border-radius: 4px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: inherit;
  font-size: 0.85rem;
}

.input-area input:focus {
  outline: none;
  border-color: #4ec9b0;
}

.input-area input:disabled {
  opacity: 0.5;
}

.input-area button {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 500;
}

.quick-btn {
  background: #c678dd;
  color: white;
}

.send-btn {
  background: #4ec9b0;
  color: #1e1e1e;
}

.input-area button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-area button:not(:disabled):hover {
  filter: brightness(1.1);
}
</style>
