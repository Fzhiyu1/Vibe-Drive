<script setup lang="ts">
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()
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
      :class="{ expanded: store.chainExpanded }"
    >
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
  height: 80px;
  transition: height var(--transition-normal);
  overflow: hidden;
}

.panel-bottom.expanded {
  height: 200px;
}
</style>
