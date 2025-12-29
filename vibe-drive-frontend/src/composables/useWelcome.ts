import { ref, onMounted } from 'vue'

const STORAGE_KEY = 'vibe-drive-welcome-accepted'

export function useWelcome() {
  const showWelcome = ref(false)

  function checkWelcomeStatus() {
    const accepted = localStorage.getItem(STORAGE_KEY) === 'true'
    showWelcome.value = !accepted
  }

  function acceptWelcome() {
    localStorage.setItem(STORAGE_KEY, 'true')
    showWelcome.value = false
  }

  // 用于开发/测试：重置欢迎状态
  function resetWelcome() {
    localStorage.removeItem(STORAGE_KEY)
    showWelcome.value = true
  }

  onMounted(() => {
    checkWelcomeStatus()
  })

  return {
    showWelcome,
    acceptWelcome,
    resetWelcome
  }
}
