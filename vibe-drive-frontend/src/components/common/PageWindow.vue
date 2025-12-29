<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { marked } from 'marked'
import type { PageWindowState } from '@/types/api'

const props = defineProps<{
  window: PageWindowState
}>()

const emit = defineEmits<{
  close: [id: string]
  focus: [id: string]
  updatePosition: [id: string, position: { x: number; y: number }]
  updateSize: [id: string, size: { width: number; height: number }]
  toggleMinimize: [id: string]
}>()

// 渲染 Markdown 内容
const renderedContent = computed(() => {
  if (props.window.type === 'code') {
    return props.window.content
  }
  return marked(props.window.content) as string
})

// 拖拽状态
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 缩放状态
const isResizing = ref(false)
const resizeDirection = ref('')

// 窗口样式
const windowStyle = computed(() => ({
  left: `${props.window.position.x}px`,
  top: `${props.window.position.y}px`,
  width: `${props.window.size.width}px`,
  height: props.window.minimized ? 'auto' : `${props.window.size.height}px`,
  zIndex: props.window.zIndex
}))

// 拖拽开始
function startDrag(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('.window-controls')) return
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - props.window.position.x,
    y: e.clientY - props.window.position.y
  }
  emit('focus', props.window.id)
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value) return
  emit('updatePosition', props.window.id, {
    x: Math.max(0, e.clientX - dragOffset.value.x),
    y: Math.max(0, e.clientY - dragOffset.value.y)
  })
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 缩放开始
function startResize(e: MouseEvent, direction: string) {
  isResizing.value = true
  resizeDirection.value = direction
  emit('focus', props.window.id)
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

function onResize(e: MouseEvent) {
  if (!isResizing.value) return
  const minWidth = 300
  const minHeight = 200
  let newWidth = props.window.size.width
  let newHeight = props.window.size.height

  if (resizeDirection.value.includes('e')) {
    newWidth = Math.max(minWidth, e.clientX - props.window.position.x)
  }
  if (resizeDirection.value.includes('s')) {
    newHeight = Math.max(minHeight, e.clientY - props.window.position.y)
  }
  emit('updateSize', props.window.id, { width: newWidth, height: newHeight })
}

function stopResize() {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
})
</script>

<template>
  <Teleport to="body">
    <div
      class="page-window"
      :class="{ minimized: window.minimized, dragging: isDragging }"
      :style="windowStyle"
      @mousedown="emit('focus', window.id)"
    >
      <!-- 标题栏 -->
      <div class="window-header" @mousedown="startDrag">
        <span class="window-title">{{ window.title }}</span>
        <div class="window-controls">
          <button class="control-btn minimize" @click="emit('toggleMinimize', window.id)">
            <span>−</span>
          </button>
          <button class="control-btn close" @click="emit('close', window.id)">
            <span>×</span>
          </button>
        </div>
      </div>

      <!-- 内容区 -->
      <div v-if="!window.minimized" class="window-body">
        <div class="content" :class="window.type">
          <pre v-if="window.type === 'code'"><code>{{ window.content }}</code></pre>
          <div v-else class="markdown-content" v-html="renderedContent"></div>
        </div>
      </div>

      <!-- 缩放手柄 -->
      <template v-if="!window.minimized">
        <div class="resize-handle resize-e" @mousedown="startResize($event, 'e')" />
        <div class="resize-handle resize-s" @mousedown="startResize($event, 's')" />
        <div class="resize-handle resize-se" @mousedown="startResize($event, 'se')" />
      </template>
    </div>
  </Teleport>
</template>

<style scoped>
.page-window {
  position: fixed;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-window.dragging {
  user-select: none;
  opacity: 0.9;
}

.window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  cursor: move;
}

.window-title {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.window-controls {
  display: flex;
  gap: 0.5rem;
}

.control-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  transition: opacity 0.2s;
}

.control-btn.minimize {
  background: #f59e0b;
  color: white;
}

.control-btn.close {
  background: #ef4444;
  color: white;
}

.control-btn:hover {
  opacity: 0.8;
}

.window-body {
  flex: 1;
  overflow: auto;
  padding: 1rem;
  min-height: 100px;
}

.content {
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--text-primary);
}

.content.code pre {
  background: var(--bg-tertiary);
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0;
}

.content.code code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.85rem;
}

.markdown-content {
  word-break: break-word;
}

/* Markdown 渲染样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin: 1rem 0 0.5rem;
  font-weight: 600;
}

.markdown-content :deep(h1) { font-size: 1.5rem; }
.markdown-content :deep(h2) { font-size: 1.25rem; }
.markdown-content :deep(h3) { font-size: 1.1rem; }

.markdown-content :deep(p) {
  margin: 0.5rem 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.markdown-content :deep(li) {
  margin: 0.25rem 0;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: var(--accent-primary, #3b82f6);
}

.markdown-content :deep(code) {
  background: var(--bg-tertiary, #374151);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  font-family: 'Consolas', monospace;
  font-size: 0.85em;
}

.markdown-content :deep(pre) {
  background: var(--bg-tertiary, #374151);
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
}

/* 缩放手柄 */
.resize-handle {
  position: absolute;
}

.resize-e {
  right: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: ew-resize;
}

.resize-s {
  bottom: 0;
  left: 0;
  right: 0;
  height: 6px;
  cursor: ns-resize;
}

.resize-se {
  right: 0;
  bottom: 0;
  width: 12px;
  height: 12px;
  cursor: nwse-resize;
}

/* 最小化状态 */
.page-window.minimized {
  height: auto !important;
}
</style>
