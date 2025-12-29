<script setup lang="ts">
defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  accept: []
}>()

function handleAccept() {
  emit('accept')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>
            <span class="icon">🚗</span>
            欢迎体验 Vibe Drive
          </h3>
        </div>

        <div class="modal-body">
          <p class="description">
            AI 驱动的车载智能氛围编排系统
          </p>

          <div class="notice">
            <div class="notice-title">
              <span class="warning-icon">⚠️</span>
              使用须知：
            </div>
            <ul class="notice-list">
              <li>本网站会使用麦克风进行语音输入</li>
              <li>本网站会播放音乐和语音</li>
            </ul>
          </div>

          <div class="guide">
            <div class="guide-title">
              <span class="guide-icon">📖</span>
              快速开始：
            </div>
            <ol class="guide-list">
              <li>点击左侧「选择场景」，输入「上海-杭州」生成场景</li>
              <li>点击「开始编排」，AI 将自动编排氛围</li>
              <li>编排完成后可继续语音对话，调整氛围</li>
            </ol>
          </div>

          <p class="consent-text">
            点击"同意并开始"即表示您同意上述内容
          </p>

          <button class="accept-btn" @click="handleAccept">
            同意并开始
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.modal {
  background: var(--bg-primary);
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  padding: 1.5rem 1.5rem 0;
  text-align: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.modal-header .icon {
  font-size: 1.5rem;
}

.modal-body {
  padding: 1.5rem;
}

.description {
  text-align: center;
  color: var(--text-secondary);
  margin-bottom: 1.5rem;
}

.notice {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.notice-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.warning-icon {
  font-size: 1rem;
}

.notice-list {
  margin: 0;
  padding-left: 1.25rem;
  color: var(--text-secondary);
}

.notice-list li {
  margin: 0.25rem 0;
}

.guide {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.guide-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.guide-icon {
  font-size: 1rem;
}

.guide-list {
  margin: 0;
  padding-left: 1.25rem;
  color: var(--text-secondary);
}

.guide-list li {
  margin: 0.25rem 0;
}

.consent-text {
  text-align: center;
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: 1rem;
}

.accept-btn {
  width: 100%;
  padding: 0.875rem;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.accept-btn:hover {
  opacity: 0.9;
}
</style>
