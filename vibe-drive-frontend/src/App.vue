<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useVibeStore } from '@/stores/vibeStore'
import PageWindow from '@/components/common/PageWindow.vue'

const store = useVibeStore()
</script>

<template>
  <RouterView />

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
