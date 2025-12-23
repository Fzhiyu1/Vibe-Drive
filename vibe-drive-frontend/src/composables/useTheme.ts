import { ref, watch } from 'vue'

const isDark = ref(false)

export function useTheme() {
  function toggle() {
    isDark.value = !isDark.value
  }

  function setTheme(dark: boolean) {
    isDark.value = dark
  }

  // 监听变化，更新 DOM
  watch(isDark, (dark) => {
    document.documentElement.classList.toggle('dark', dark)
  }, { immediate: true })

  return { isDark, toggle, setTheme }
}
