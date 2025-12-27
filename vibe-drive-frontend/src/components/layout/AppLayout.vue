<script setup lang="ts">
import { ref, computed } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

// 拖拽调整高度
const panelHeight = ref(200)
const isDragging = ref(false)
const MIN_HEIGHT = 80
const MAX_HEIGHT = 600

const panelStyle = computed(() => ({
  height: store.chainExpanded ? `${panelHeight.value}px` : '40px'
}))

function startDrag(e: MouseEvent) {
  if (!store.chainExpanded) return
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault()
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value) return
  const newHeight = window.innerHeight - e.clientY
  panelHeight.value = Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, newHeight))
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
</script>

<template>
  <div class="app-layout">
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：环境面板 -->
      <aside class="panel-left">
        <slot name="environment" />
      </aside>

      <!-- 中央：氛围可视化 -->
      <main class="panel-center">
        <slot name="ambience" />
      </main>

      <!-- 右侧：音乐 + 串词 + 香氛 + 按摩 -->
      <aside class="panel-right">
        <slot name="music" />
        <slot name="narrative" />
        <slot name="scent" />
        <slot name="massage" />
      </aside>
    </div>

    <!-- 底部：思维链 -->
    <footer
      class="panel-bottom"
      :class="{ expanded: store.chainExpanded, dragging: isDragging }"
      :style="panelStyle"
    >
      <!-- 拖拽手柄 -->
      <div
        v-if="store.chainExpanded"
        class="resize-handle"
        @mousedown="startDrag"
      />
      <slot name="thinking" />
    </footer>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  max-height: 100vh;
  background-color: var(--bg-primary);
  overflow: hidden;
}

.main-content {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  flex: 1;
  min-height: 0;
}

.panel-left,
.panel-center,
.panel-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-left {
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  padding: 1rem;
  overflow-y: auto;
}

.panel-center {
  background-color: var(--bg-primary);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-right {
  background-color: var(--bg-secondary);
  border-left: 1px solid var(--border-color);
  padding: 1rem;
  gap: 1rem;
  overflow-y: auto;
}

.panel-bottom {
  background-color: var(--bg-tertiary);
  border-top: 1px solid var(--border-color);
  transition: height 0.2s ease;
  overflow: hidden;
  position: relative;
}

.panel-bottom.dragging {
  transition: none;
  user-select: none;
}

.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  cursor: ns-resize;
  background: transparent;
  z-index: 10;
}

.resize-handle:hover,
.panel-bottom.dragging .resize-handle {
  background: var(--accent);
  opacity: 0.5;
}
</style>
