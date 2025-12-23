<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

const narrative = computed(() => store.plan?.narrative)

// 打字机效果
const displayText = ref('')
const isTyping = ref(false)

watch(
  () => narrative.value?.text,
  async (newText) => {
    if (!newText) {
      displayText.value = ''
      return
    }

    isTyping.value = true
    displayText.value = ''

    for (let i = 0; i < newText.length; i++) {
      displayText.value += newText[i]
      await new Promise((r) => setTimeout(r, 30))
    }

    isTyping.value = false
  }
)

const emotionLabels: Record<string, string> = {
  NEUTRAL: '中性',
  WARM: '温暖',
  ENERGETIC: '活力',
  CALM: '平静',
  GENTLE: '轻柔',
}
</script>

<template>
  <div class="narrative-display">
    <h3 class="section-title">串词</h3>

    <div v-if="narrative" class="narrative-content">
      <div class="emotion-tag">
        {{ emotionLabels[narrative.emotion] }}
      </div>
      <p class="text">
        {{ displayText }}
        <span v-if="isTyping" class="cursor">|</span>
      </p>
    </div>

    <div v-else class="no-narrative">
      暂无串词
    </div>
  </div>
</template>

<style scoped>
.narrative-display {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.narrative-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.emotion-tag {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  background: var(--accent);
  color: white;
  border-radius: 4px;
  font-size: 0.75rem;
  width: fit-content;
}

.text {
  color: var(--text-primary);
  line-height: 1.6;
  font-size: 0.9rem;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.no-narrative {
  color: var(--text-muted);
  text-align: center;
  padding: 1rem;
}
</style>
