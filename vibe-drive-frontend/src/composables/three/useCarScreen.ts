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

  // 滚动相关
  let scrollOffset = 0
  let textWidth = 0
  const scrollSpeed = 60 // 像素/秒（加快）
  const maxWidth = 360 // 文字显示区域宽度

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

    // 播放图标 + 歌曲名（带滚动）
    const songText = currentSong.title
    ctx.font = 'bold 18px Arial'
    textWidth = ctx.measureText(songText).width

    // 绘制播放图标
    ctx.fillStyle = '#fff'
    ctx.fillText('▶', 20, 45)

    // 裁剪区域绘制滚动歌曲名
    ctx.save()
    ctx.beginPath()
    ctx.rect(50, 25, maxWidth - 30, 30)
    ctx.clip()

    if (textWidth > maxWidth - 50) {
      // 文字太长，需要滚动
      ctx.fillText(songText, 50 - scrollOffset, 45)
      // 循环显示
      ctx.fillText(songText, 50 - scrollOffset + textWidth + 50, 45)
    } else {
      ctx.fillText(songText, 50, 45)
    }
    ctx.restore()

    // 作者名（固定显示在进度条下方）
    ctx.fillStyle = '#999'
    ctx.font = '14px Arial'
    ctx.fillText(currentSong.artist, 20, 130)

    // 进度条背景
    ctx.fillStyle = '#333'
    ctx.fillRect(20, 70, 360, 8)

    // 进度条
    ctx.fillStyle = '#00aaff'
    ctx.fillRect(20, 70, 360 * currentProgress, 8)

    // 时间显示
    const current = Math.floor(currentSong.duration * currentProgress)
    const total = currentSong.duration
    ctx.fillStyle = '#999'
    ctx.font = '12px Arial'
    ctx.fillText(formatTime(current) + ' / ' + formatTime(total), 20, 100)

    meshes.screenTexture.needsUpdate = true
  }

  function formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  function updateSong(song: Song | null) {
    console.log('[useCarScreen] updateSong:', song)
    currentSong = song
    currentProgress = 0
    scrollOffset = 0  // 重置滚动
    drawPlayer()
  }

  function updateProgress(progress: number) {
    currentProgress = Math.max(0, Math.min(1, progress))
    drawPlayer()
  }

  function tick(delta: number) {
    // 文字滚动动画
    if (currentSong && textWidth > maxWidth - 50) {
      scrollOffset += scrollSpeed * delta
      // 循环滚动
      if (scrollOffset > textWidth + 50) {
        scrollOffset = 0
      }
      drawPlayer()
    }
  }

  // 初始绘制
  drawPlayer()

  return { updateSong, updateProgress, tick }
}
