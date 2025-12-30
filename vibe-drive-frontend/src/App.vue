<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useVibeStore } from '@/stores/vibeStore'
import { useMemoryStore } from '@/stores/memoryStore'
import PageWindow from '@/components/common/PageWindow.vue'

const store = useVibeStore()
const memoryStore = useMemoryStore()

function showMemory() {
  const pageId = store.openPageWindow('用户记忆', 'document')
  store.appendPageContent(pageId, memoryStore.memory)
}

// 测试：注入长期记忆
function injectTestMemory() {
  const testMemory = `# 记忆

## 摘要（长期记忆）
- **音乐偏好**：用户喜欢爵士乐和轻音乐，不喜欢重金属
- **香氛偏好**：用户不喜欢薰衣草香氛，偏好柑橘类香氛
- **按摩偏好**：用户喜欢全身按摩，强度中等
- **灯光偏好**：用户喜欢暖色调灯光，不喜欢太亮

## 最近（短期记忆）
暂无
`
  memoryStore.memory = testMemory
  memoryStore.syncToBackend(store.sessionId)
  alert('已注入测试长期记忆')
}
</script>

<template>
  <RouterView />

  <!-- 记忆拉绳 -->
  <button class="memory-pull" @click="showMemory" title="查看记忆">
    🧠
  </button>

  <!-- 测试按钮：注入长期记忆 -->
  <button class="inject-memory-btn" @click="injectTestMemory" title="注入测试记忆">
    💉
  </button>

  <!-- 全局弹窗 -->
  <PageWindow
    v-for="win in store.pageWindows"
    :key="win.id"
    :window="win"
    @close="store.closePageWindow"
    @focus="store.focusPageWindow"
    @update-position="store.updateWindowPosition"
    @update-size="store.updateWindowSize"
    @toggle-minimize="store.toggleWindowMinimize"
  />
</template>

<style scoped>
.memory-pull {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: var(--bg-secondary, #1e1e2e);
  border: 1px solid var(--border-color, #444);
  border-top: none;
  border-radius: 0 0 8px 8px;
  padding: 4px 16px 8px;
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.memory-pull:hover {
  padding-bottom: 12px;
  background: var(--bg-tertiary, #2a2a3e);
}

.inject-memory-btn {
  position: fixed;
  top: 0;
  left: calc(50% + 50px);
  transform: translateX(-50%);
  z-index: 9999;
  background: var(--bg-secondary, #1e1e2e);
  border: 1px solid var(--border-color, #444);
  border-top: none;
  border-radius: 0 0 8px 8px;
  padding: 4px 12px 8px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.inject-memory-btn:hover {
  padding-bottom: 12px;
  background: var(--bg-tertiary, #2a2a3e);
}
</style>
