<script setup lang="ts">
import { useVibeStore } from '@/stores/vibeStore'
import type { UserMood } from '@/types/api'

const store = useVibeStore()

const moods: { value: UserMood; label: string; icon: string }[] = [
  { value: 'HAPPY', label: '开心', icon: '😊' },
  { value: 'CALM', label: '平静', icon: '😌' },
  { value: 'TIRED', label: '疲惫', icon: '😴' },
  { value: 'STRESSED', label: '压力', icon: '😰' },
  { value: 'EXCITED', label: '兴奋', icon: '🤩' },
]

function selectMood(mood: UserMood) {
  store.setEnvironment({ userMood: mood })
}
</script>

<template>
  <div class="mood-selector">
    <span class="label">当前情绪</span>
    <div class="mood-options">
      <button
        v-for="mood in moods"
        :key="mood.value"
        class="mood-btn"
        :class="{ active: store.environment?.userMood === mood.value }"
        @click="selectMood(mood.value)"
      >
        <span class="icon">{{ mood.icon }}</span>
        <span class="text">{{ mood.label }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.mood-selector {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.label {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.mood-options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.mood-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-width: 50px;
}

.mood-btn:hover {
  border-color: var(--accent);
}

.mood-btn.active {
  border-color: var(--accent);
  background: var(--accent);
  color: white;
}

.icon {
  font-size: 1.25rem;
}

.text {
  font-size: 0.625rem;
  margin-top: 0.25rem;
}
</style>
