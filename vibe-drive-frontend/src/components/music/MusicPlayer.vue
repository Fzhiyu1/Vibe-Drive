<script setup lang="ts">
import { computed, ref } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

const currentSong = computed(() => {
  return store.plan?.music?.songs?.[0] || null
})

const playlist = computed(() => {
  return store.plan?.music?.songs || []
})

const isPlaying = ref(false)
const progress = ref(0)

function togglePlay() {
  isPlaying.value = !isPlaying.value
}
</script>

<template>
  <div class="music-player">
    <h3 class="section-title">音乐</h3>

    <div v-if="currentSong" class="now-playing">
      <!-- 封面 -->
      <div class="cover">
        <div class="cover-placeholder">♪</div>
      </div>

      <!-- 歌曲信息 -->
      <div class="song-info">
        <div class="title">{{ currentSong.title }}</div>
        <div class="artist">{{ currentSong.artist }}</div>
      </div>

      <!-- 进度条 -->
      <div class="progress-bar">
        <div class="progress" :style="{ width: progress + '%' }" />
      </div>

      <!-- 控制按钮 -->
      <div class="controls">
        <button class="ctrl-btn">⏮</button>
        <button class="ctrl-btn play" @click="togglePlay">
          {{ isPlaying ? '⏸' : '▶' }}
        </button>
        <button class="ctrl-btn">⏭</button>
      </div>
    </div>

    <div v-else class="no-music">
      暂无音乐推荐
    </div>

    <!-- 播放列表 -->
    <div v-if="playlist.length > 1" class="playlist">
      <div class="playlist-title">播放列表</div>
      <div
        v-for="(song, index) in playlist.slice(0, 5)"
        :key="song.id"
        class="playlist-item"
      >
        <span class="index">{{ index + 1 }}</span>
        <span class="name">{{ song.title }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.music-player {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.now-playing {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cover {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-tertiary);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: var(--text-muted);
}

.song-info {
  text-align: center;
}

.title {
  font-weight: 600;
  color: var(--text-primary);
}

.artist {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.progress-bar {
  height: 4px;
  background: var(--bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
}

.progress {
  height: 100%;
  background: var(--accent);
  transition: width 0.1s linear;
}

.controls {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.ctrl-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--bg-tertiary);
  cursor: pointer;
  font-size: 1rem;
}

.ctrl-btn.play {
  width: 44px;
  height: 44px;
  background: var(--accent);
  color: white;
}

.no-music {
  color: var(--text-muted);
  text-align: center;
  padding: 2rem;
}

.playlist {
  margin-top: 0.5rem;
}

.playlist-title {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.playlist-item {
  display: flex;
  gap: 0.5rem;
  padding: 0.25rem 0;
  font-size: 0.875rem;
}

.index {
  color: var(--text-muted);
  width: 1.5rem;
}

.name {
  color: var(--text-primary);
}
</style>
