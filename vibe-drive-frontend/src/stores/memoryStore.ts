import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const MEMORY_KEY = 'vibe-drive-memory'

const DEFAULT_MEMORY = `# 记忆

## 摘要（长期记忆）
暂无

## 最近（短期记忆）
暂无
`

export const useMemoryStore = defineStore('memory', () => {
  // 从 localStorage 加载
  const memory = ref<string>(loadFromStorage())

  function loadFromStorage(): string {
    return localStorage.getItem(MEMORY_KEY) || DEFAULT_MEMORY
  }

  function saveToStorage() {
    localStorage.setItem(MEMORY_KEY, memory.value)
  }

  // 自动持久化
  watch(memory, saveToStorage, { deep: true })

  // 同步到后端
  async function syncToBackend(sessionId: string) {
    try {
      console.log('[memoryStore] 同步记忆到后端, sessionId:', sessionId, '内容长度:', memory.value.length)
      const res = await fetch(`/api/memory/sync?sessionId=${sessionId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: memory.value })
      })
      const data = await res.json()
      console.log('[memoryStore] 同步结果:', data)
    } catch (e) {
      console.warn('[memoryStore] 同步记忆失败:', e)
    }
  }

  // 从后端获取（可选）
  async function fetchFromBackend(sessionId: string) {
    try {
      const res = await fetch(`/api/memory?sessionId=${sessionId}`)
      const data = await res.json()
      if (data.content) {
        memory.value = data.content
      }
    } catch (e) {
      console.warn('[memoryStore] 获取记忆失败:', e)
    }
  }

  // 清空记忆
  function clearMemory() {
    memory.value = DEFAULT_MEMORY
    saveToStorage()
  }

  return {
    memory,
    syncToBackend,
    fetchFromBackend,
    clearMemory
  }
})
