<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()
const inputText = ref('')
const expandedItems = ref<Set<string>>(new Set())

function toggleExpand() {
  store.chainExpanded = !store.chainExpanded
}

function handleSend() {
  if (!inputText.value.trim() || store.masterRunning) return
  store.sendMessage(inputText.value)
  inputText.value = ''
}

function handleQuickAnalyze() {
  if (store.masterRunning) return
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

function formatJson(obj: unknown, expanded = false): string {
  if (!obj) return ''
  try {
    if (typeof obj === 'string') {
      if (expanded) return obj
      return obj.length > 200 ? obj.slice(0, 200) + '...' : obj
    }
    const str = JSON.stringify(obj, null, 2)
    if (expanded) return str
    return str.length > 300 ? str.slice(0, 300) + '...' : str
  } catch {
    return String(obj)
  }
}

function isContentTruncated(obj: unknown): boolean {
  if (!obj) return false
  try {
    if (typeof obj === 'string') return obj.length > 200
    return JSON.stringify(obj, null, 2).length > 300
  } catch {
    return false
  }
}

function toggleItemExpand(key: string) {
  if (expandedItems.value.has(key)) {
    expandedItems.value.delete(key)
  } else {
    expandedItems.value.add(key)
  }
}

function isItemExpanded(key: string): boolean {
  return expandedItems.value.has(key)
}

function getAgentLabel(agent?: string) {
  return agent === 'master' ? 'MASTER' : agent === 'vibe' ? 'VIBE' : 'AGENT'
}

const masterContainer = ref<HTMLElement | null>(null)
const vibeContainer = ref<HTMLElement | null>(null)

// 自动滚动到底部
watch(() => store.masterChain, async () => {
  await nextTick()
  if (masterContainer.value) {
    masterContainer.value.scrollTop = masterContainer.value.scrollHeight
  }
}, { deep: true })

watch(() => store.vibeChain, async () => {
  await nextTick()
  if (vibeContainer.value) {
    vibeContainer.value.scrollTop = vibeContainer.value.scrollHeight
  }
}, { deep: true })
</script>

<template>
  <div class="thinking-chain">
    <div class="header" @click="toggleExpand">
      <span class="title">Agent 思维链</span>
      <span v-if="store.masterRunning || store.vibeRunning" class="status running">
        <span class="dot"></span> 运行中
      </span>
      <span v-else-if="store.masterChain.length > 0 || store.vibeChain.length > 0" class="status done">
        已完成
      </span>
      <span class="toggle">{{ store.chainExpanded ? '▼' : '▲' }}</span>
    </div>

    <!-- 两栏布局 -->
    <div v-if="store.chainExpanded" class="dual-panel">
      <!-- 左栏：主智能体 (2/3) -->
      <div class="master-panel">
        <div class="panel-header">
          <span class="panel-title">主智能体</span>
          <span v-if="store.masterRunning" class="panel-status running">
            <span class="dot"></span>
          </span>
        </div>
        <div ref="masterContainer" class="log-container">
          <template v-for="(step, index) in store.masterChain" :key="'m-'+index">
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

            <!-- 环境变化检测 -->
            <div v-else-if="step.type === 'env_change'" class="log-block env-change">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[ENV]</span>
                <span class="label">{{ step.content }}</span>
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
            <div v-else-if="step.type === 'tool_start'" class="log-block tool-start">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[TOOL]</span>
                <span class="label">调用 <span class="tool-name">{{ step.toolName }}</span></span>
              </div>
              <div
                v-if="step.toolInput"
                class="log-content code"
                :class="{ expandable: isContentTruncated(step.toolInput), expanded: isItemExpanded(`m-input-${index}`) }"
                @click="isContentTruncated(step.toolInput) && toggleItemExpand(`m-input-${index}`)"
              >
                <span class="input-label">
                  输入:
                  <span v-if="isContentTruncated(step.toolInput)" class="expand-hint">
                    {{ isItemExpanded(`m-input-${index}`) ? '▲' : '▼' }}
                  </span>
                </span>
                <pre>{{ formatJson(step.toolInput, isItemExpanded(`m-input-${index}`)) }}</pre>
              </div>
            </div>

            <!-- Tool End -->
            <div v-else-if="step.type === 'tool_end'" class="log-block tool-end">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[DONE]</span>
                <span class="label"><span class="tool-name">{{ step.toolName }}</span></span>
              </div>
              <div
                v-if="step.toolOutput"
                class="log-content code"
                :class="{ expandable: isContentTruncated(step.toolOutput), expanded: isItemExpanded(`m-output-${index}`) }"
                @click="isContentTruncated(step.toolOutput) && toggleItemExpand(`m-output-${index}`)"
              >
                <span class="output-label">
                  输出:
                  <span v-if="isContentTruncated(step.toolOutput)" class="expand-hint">
                    {{ isItemExpanded(`m-output-${index}`) ? '▲' : '▼' }}
                  </span>
                </span>
                <pre>{{ formatJson(step.toolOutput, isItemExpanded(`m-output-${index}`)) }}</pre>
              </div>
            </div>

            <!-- Complete -->
            <div v-else-if="step.type === 'complete'" class="log-block complete">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[OK]</span>
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

          <div v-if="store.masterChain.length === 0" class="empty-log">
            <span class="prompt">$</span> 输入消息开始对话...
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <input
            v-model="inputText"
            placeholder="输入消息..."
            :disabled="store.masterRunning"
            @keydown.enter="handleSend"
          />
          <button class="quick-btn" @click="handleQuickAnalyze" :disabled="store.masterRunning">
            ⚡ 编排
          </button>
          <button class="send-btn" @click="handleSend" :disabled="!inputText.trim() || store.masterRunning">
            发送
          </button>
        </div>
      </div>

      <!-- 右栏：Vibe智能体 (1/3) -->
      <div class="vibe-panel">
        <div class="panel-header">
          <span class="panel-title">Vibe 智能体</span>
          <span v-if="store.vibeRunning" class="panel-status running">
            <span class="dot"></span>
          </span>
        </div>
        <div ref="vibeContainer" class="log-container">
          <template v-for="(step, index) in store.vibeChain" :key="'v-'+index">
            <!-- Thinking -->
            <div v-if="step.type === 'thinking'" class="log-block thinking">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[THINK]</span>
              </div>
              <div class="log-content">{{ step.content }}<span class="cursor">▌</span></div>
            </div>

            <!-- Tool Start -->
            <div v-else-if="step.type === 'tool_start'" class="log-block tool-start vibe-tool">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[VIBE]</span>
                <span class="label">调用 <span class="tool-name">{{ step.toolName }}</span></span>
              </div>
              <div
                v-if="step.toolInput"
                class="log-content code"
                :class="{ expandable: isContentTruncated(step.toolInput), expanded: isItemExpanded(`v-input-${index}`) }"
                @click="isContentTruncated(step.toolInput) && toggleItemExpand(`v-input-${index}`)"
              >
                <span class="input-label">
                  输入:
                  <span v-if="isContentTruncated(step.toolInput)" class="expand-hint">
                    {{ isItemExpanded(`v-input-${index}`) ? '▲' : '▼' }}
                  </span>
                </span>
                <pre>{{ formatJson(step.toolInput, isItemExpanded(`v-input-${index}`)) }}</pre>
              </div>
            </div>

            <!-- Tool End -->
            <div v-else-if="step.type === 'tool_end'" class="log-block tool-end vibe-tool">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[DONE]</span>
                <span class="label"><span class="tool-name">{{ step.toolName }}</span></span>
              </div>
              <div
                v-if="step.toolOutput"
                class="log-content code"
                :class="{ expandable: isContentTruncated(step.toolOutput), expanded: isItemExpanded(`v-output-${index}`) }"
                @click="isContentTruncated(step.toolOutput) && toggleItemExpand(`v-output-${index}`)"
              >
                <span class="output-label">
                  输出:
                  <span v-if="isContentTruncated(step.toolOutput)" class="expand-hint">
                    {{ isItemExpanded(`v-output-${index}`) ? '▲' : '▼' }}
                  </span>
                </span>
                <pre>{{ formatJson(step.toolOutput, isItemExpanded(`v-output-${index}`)) }}</pre>
              </div>
            </div>

            <!-- Complete -->
            <div v-else-if="step.type === 'complete'" class="log-block complete vibe-complete">
              <div class="log-header">
                <span class="time">{{ formatTime(step.timestamp) }}</span>
                <span class="prefix">[OK]</span>
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

          <div v-if="store.vibeChain.length === 0" class="empty-log">
            等待任务...
          </div>
        </div>
      </div>
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

/* 两栏布局 */
.dual-panel {
  flex: 1;
  display: flex;
  min-height: 0;
}

.master-panel {
  flex: 2;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #404040;
  min-width: 0;
  overflow: hidden;
}

.vibe-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 0.25rem 0.5rem;
  background: #252526;
  border-bottom: 1px solid #404040;
  font-size: 0.75rem;
}

.panel-title {
  color: #808080;
  font-weight: 500;
}

.panel-status.running {
  margin-left: 0.5rem;
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
.log-block.env-change { border-color: #e5c07b; }

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
.env-change .prefix { color: #e5c07b; }

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
  overflow-x: auto;
}

.log-content pre {
  margin: 0;
  font-size: 0.75rem;
  color: #ce9178;
  white-space: pre-wrap;
  word-break: break-all;
  overflow-wrap: break-word;
}

.input-label, .output-label {
  color: #808080;
  font-size: 0.7rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.expand-hint {
  color: #569cd6;
  font-size: 0.65rem;
  cursor: pointer;
}

.log-content.expandable {
  cursor: pointer;
}

.log-content.expandable:hover {
  background: #2a2a2a;
}

.log-content.expanded pre {
  max-height: none;
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
