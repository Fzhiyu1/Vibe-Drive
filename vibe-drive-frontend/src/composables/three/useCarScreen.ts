import * as THREE from 'three'
import type { Song } from '@/types/api'
import type { CarInteriorMeshes } from '@/types/three'

export interface UseCarScreenReturn {
  updateSong: (song: Song | null) => void
  updateProgress: (progress: number) => void
  tick: (delta: number) => void
}

export function useCarScreen(meshes: CarInteriorMeshes): UseCarScreenReturn {
  const canvas = meshes.screenTexture.image as HTMLCanvasElement
  const ctx = canvas.getContext('2d')!

  let currentSong: Song | null = null
  let currentProgress = 0

  function drawPlayer() {
    // 背景（浅色，模拟屏幕发光）
    ctx.fillStyle = '#2a3a4a'
    ctx.fillRect(0, 0, 400, 150)

    if (!currentSong) {
      ctx.fillStyle = '#666'
      ctx.font = '16px Arial'
      ctx.fillText('暂无播放', 160, 75)
      meshes.screenTexture.needsUpdate = true
      return
    }

    // 播放图标 + 歌曲信息
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 18px Arial'
    ctx.fillText(`▶  ${currentSong.artist} - ${currentSong.title}`, 20, 50)

    // 进度条背景
    ctx.fillStyle = '#333'
    ctx.fillRect(20, 80, 360, 8)

    // 进度条
    ctx.fillStyle = '#00aaff'
    ctx.fillRect(20, 80, 360 * currentProgress, 8)

    // 时间显示
    const current = Math.floor(currentSong.duration * currentProgress)
    const total = currentSong.duration
    ctx.fillStyle = '#999'
    ctx.font = '12px Arial'
    ctx.fillText(formatTime(current) + ' / ' + formatTime(total), 20, 110)

    meshes.screenTexture.needsUpdate = true
  }

  function formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  function updateSong(song: Song | null) {
    currentSong = song
    currentProgress = 0
    drawPlayer()
  }

  function updateProgress(progress: number) {
    currentProgress = Math.max(0, Math.min(1, progress))
    drawPlayer()
  }

  function tick(_delta: number) {
    // 可用于动画效果
  }

  // 初始绘制
  drawPlayer()

  return { updateSong, updateProgress, tick }
}
