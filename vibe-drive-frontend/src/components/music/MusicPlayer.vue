<script setup lang="ts">
import { computed, watch } from 'vue'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()

// 当前歌曲（优先 playlist，其次 playResult）
const currentSong = computed(() => {
  const playlist = store.plan?.playlist
  if (playlist && playlist.songs.length > 0) {
    const index = store.currentPlaylistIndex
    const song = playlist.songs[Math.min(index, playlist.songs.length - 1)]
    if (song) {
      return {
        id: song.id,
        title: song.name,
        artist: song.artist,
        coverUrl: song.coverUrl,
        duration: song.duration,
        url: song.url
      }
    }
  }
  const playResult = store.plan?.playResult
  if (playResult) {
    return {
      id: playResult.id,
      title: playResult.name,
      artist: playResult.artist,
      coverUrl: playResult.coverUrl,
      duration: playResult.duration,
      url: playResult.url
    }
  }
  return store.plan?.music?.songs?.[0] || null
})

// 歌单列表（优先新的 playlist）
const playlist = computed(() => {
  return store.plan?.playlist?.songs || store.plan?.music?.songs || []
})

const currentIndex = computed(() => store.currentPlaylistIndex)
const hasNext = computed(() => currentIndex.value < playlist.value.length - 1)
const hasPrevious = computed(() => currentIndex.value > 0)

// 使用 store 的音频状态
const isPlaying = computed(() => store.isPlaying)
const progress = computed(() => store.audioProgress)

// 监听 playlist 变化，自动播放第一首
watch(() => store.plan?.playlist, (newPlaylist) => {
  if (newPlaylist && newPlaylist.songs.length > 0) {
    store.currentPlaylistIndex = 0
    const firstSong = newPlaylist.songs[0]
    if (firstSong?.url) {
      store.playMusic(firstSong.url)
    }
  }
}, { immediate: true })

// 监听 playResult 变化（兼容单首模式）
watch(() => store.plan?.playResult?.url, (url) => {
  // 只有在没有 playlist 时才使用 playResult
  if (url && !store.plan?.playlist) {
    store.playMusic(url)
  }
}, { immediate: true })

function togglePlay() {
  store.toggleAudio()
}

function playNext() {
  store.playNext()
}

function playPrevious() {
  store.playPrevious()
}

function playSongAt(index: number) {
  store.playSongAt(index)
}
</script>

<template>
  <div class="music-player">
    <h3 class="section-title">音乐</h3>

    <div v-if="currentSong" class="now-playing">
      <!-- 封面 -->
      <div class="cover">
        <img v-if="currentSong.coverUrl" :src="currentSong.coverUrl" alt="封面" class="cover-img" />
        <div v-else class="cover-placeholder">♪</div>
      </div>

      <!-- 歌曲信息 -->
      <div class="song-info">
        <div class="title">{{ currentSong.title }}</div>
        <div class="artist">{{ currentSong.artist }}</div>
        <!-- 歌单进度指示 -->
        <div v-if="playlist.length > 1" class="playlist-progress">
          {{ currentIndex + 1 }} / {{ playlist.length }}
        </div>
      </div>

      <!-- 进度条 -->
      <div class="progress-bar">
        <div class="progress" :style="{ width: progress + '%' }" />
      </div>

      <!-- 控制按钮 -->
      <div class="controls">
        <button class="ctrl-btn" :disabled="!hasPrevious" @click="playPrevious">⏮</button>
        <button class="ctrl-btn play" @click="togglePlay">
          {{ isPlaying ? '⏸' : '▶' }}
        </button>
        <button class="ctrl-btn" :disabled="!hasNext" @click="playNext">⏭</button>
      </div>
    </div>

    <div v-else class="no-music">
      暂无音乐推荐
    </div>

    <!-- 播放列表 -->
    <div v-if="playlist.length > 1" class="playlist">
      <div class="playlist-title">歌单 ({{ playlist.length }}首)</div>
      <div
        v-for="(song, index) in playlist"
        :key="song.id"
        class="playlist-item"
        :class="{ active: index === currentIndex }"
        @click="playSongAt(index)"
      >
        <span class="index">{{ index + 1 }}</span>
        <span class="name">{{ song.name || song.title }}</span>
        <span class="artist">{{ song.artist }}</span>
        <span v-if="index === currentIndex" class="playing-indicator">♪</span>
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

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-progress {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.playlist-item {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  font-size: 0.875rem;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
  align-items: center;
}

.playlist-item:hover {
  background: var(--bg-tertiary);
}

.playlist-item.active {
  background: rgba(var(--accent-rgb, 99, 102, 241), 0.1);
}

.playlist-item .artist {
  color: var(--text-secondary);
  font-size: 0.75rem;
}

.playing-indicator {
  color: var(--accent);
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.ctrl-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
</style>
