import * as THREE from 'three'
import type { ScentSetting, ScentType } from '@/types/api'
import type { ScentParticleConfig } from '@/types/three'

// 香氛配置表
const SCENT_PARTICLES: Record<ScentType, ScentParticleConfig> = {
  LAVENDER: { color: 0x9370db, buff: '压力↓↓', emoji: '🪻' },
  PEPPERMINT: { color: 0x98fb98, buff: '清醒↑↑', emoji: '🌿' },
  OCEAN: { color: 0x00ced1, buff: '放松↑', emoji: '🌊' },
  FOREST: { color: 0x228b22, buff: '专注↑', emoji: '🌲' },
  CITRUS: { color: 0xffa500, buff: '活力↑', emoji: '🍊' },
  VANILLA: { color: 0xffe4c4, buff: '幸福感↑', emoji: '🍦' },
  NONE: { color: 0x000000, buff: '', emoji: '' }
}

const MAX_PARTICLES = 100
const MAX_BUFF_SPRITES = 8  // Buff 文字数量

// 香氛出口位置
const OUTLET_X = 0.3
const OUTLET_Y = 0.88
const OUTLET_Z = -0.48

export interface UseScentParticlesReturn {
  updateScent: (setting: ScentSetting | null) => void
  tick: (delta: number) => void
  dispose: () => void
}

// Buff Sprite 数据
interface BuffSpriteData {
  sprite: THREE.Sprite
  velocity: THREE.Vector3
  life: number
}

export function useScentParticles(scene: THREE.Scene): UseScentParticlesReturn {
  let particles: THREE.Points | null = null
  let particlePositions: Float32Array | null = null
  let particleVelocities: Float32Array | null = null
  let activeCount = 0
  let currentConfig: ScentParticleConfig | null = null

  // Buff 文字 Sprite 池
  const buffSprites: BuffSpriteData[] = []
  let buffTexture: THREE.CanvasTexture | null = null
  let spawnTimer = 0
  const spawnInterval = 0.8  // 每 0.8 秒生成一个新文字

  // 香氛出口发光圆环
  const scentRing = new THREE.Mesh(
    new THREE.RingGeometry(0.04, 0.06, 32),
    new THREE.MeshBasicMaterial({ color: 0x00ff88, transparent: true, opacity: 0.3 })
  )
  scentRing.position.set(OUTLET_X, OUTLET_Y, OUTLET_Z)
  scentRing.rotation.x = -0.3
  scene.add(scentRing)

  // 创建 Buff 文字纹理
  function createBuffTexture(text: string, color: number): THREE.CanvasTexture {
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 64
    const ctx = canvas.getContext('2d')!

    ctx.clearRect(0, 0, 256, 64)
    ctx.fillStyle = `#${color.toString(16).padStart(6, '0')}`
    ctx.font = 'bold 36px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, 128, 32)

    return new THREE.CanvasTexture(canvas)
  }

  // 生成一个新的 Buff Sprite
  function spawnBuffSprite() {
    if (!currentConfig || !buffTexture) return
    if (buffSprites.length >= MAX_BUFF_SPRITES) return

    const material = new THREE.SpriteMaterial({
      map: buffTexture,
      transparent: true,
      opacity: 1,
      depthTest: false
    })
    const sprite = new THREE.Sprite(material)
    sprite.scale.set(0.25, 0.06, 1)

    // 从出口位置开始，带随机偏移
    sprite.position.set(
      OUTLET_X + (Math.random() - 0.5) * 0.05,
      OUTLET_Y,
      OUTLET_Z + (Math.random() - 0.5) * 0.05
    )

    // 随机速度，和粒子一致
    const velocity = new THREE.Vector3(
      (Math.random() - 0.5) * 0.02,
      0.02 + Math.random() * 0.03,
      (Math.random() - 0.5) * 0.02
    )

    scene.add(sprite)
    buffSprites.push({ sprite, velocity, life: 0 })
  }

  // 初始化粒子系统
  function initParticles() {
    const geometry = new THREE.BufferGeometry()
    particlePositions = new Float32Array(MAX_PARTICLES * 3)
    particleVelocities = new Float32Array(MAX_PARTICLES * 3)

    for (let i = 0; i < MAX_PARTICLES; i++) {
      resetParticle(i)
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3))

    const material = new THREE.PointsMaterial({
      color: 0x00ff88,
      size: 0.05,
      transparent: true,
      opacity: 0.8,
      blending: THREE.AdditiveBlending
    })

    particles = new THREE.Points(geometry, material)
    scene.add(particles)
  }

  function resetParticle(index: number) {
    if (!particlePositions || !particleVelocities) return
    const i3 = index * 3
    particlePositions[i3] = OUTLET_X + (Math.random() - 0.5) * 0.05
    particlePositions[i3 + 1] = OUTLET_Y
    particlePositions[i3 + 2] = OUTLET_Z + (Math.random() - 0.5) * 0.05
    particleVelocities[i3] = (Math.random() - 0.5) * 0.02
    particleVelocities[i3 + 1] = 0.02 + Math.random() * 0.03
    particleVelocities[i3 + 2] = (Math.random() - 0.5) * 0.02
  }

  function updateScent(setting: ScentSetting | null) {
    if (!setting || setting.type === 'NONE' || setting.intensity === 0) {
      activeCount = 0
      currentConfig = null
      ;(scentRing.material as THREE.MeshBasicMaterial).opacity = 0.3
      // 清除所有 Buff Sprites
      buffSprites.forEach(data => {
        scene.remove(data.sprite)
        data.sprite.material.dispose()
      })
      buffSprites.length = 0
      return
    }

    const scentType = setting.type.toUpperCase() as ScentType
    currentConfig = SCENT_PARTICLES[scentType]
    if (!currentConfig) {
      activeCount = 0
      return
    }

    activeCount = Math.min(setting.intensity * 10, MAX_PARTICLES)

    // 更新粒子颜色
    if (particles) {
      (particles.material as THREE.PointsMaterial).color.setHex(currentConfig.color)
    }

    // 更新圆环颜色
    (scentRing.material as THREE.MeshBasicMaterial).color.setHex(currentConfig.color)
    scentRing.material.opacity = 0.8

    // 创建 Buff 纹理
    if (buffTexture) buffTexture.dispose()
    buffTexture = createBuffTexture(currentConfig.buff, currentConfig.color)

    // 清除旧的 Buff Sprites
    buffSprites.forEach(data => {
      scene.remove(data.sprite)
      data.sprite.material.dispose()
    })
    buffSprites.length = 0

    // 初始化粒子
    if (!particles) initParticles()
  }

  function tick(delta: number) {
    // 定时生成新的 Buff Sprite
    if (currentConfig && activeCount > 0) {
      spawnTimer += delta
      if (spawnTimer >= spawnInterval) {
        spawnTimer = 0
        spawnBuffSprite()
      }
    }

    // 更新 Buff Sprites（和粒子相同的运动逻辑）
    for (let i = buffSprites.length - 1; i >= 0; i--) {
      const data = buffSprites[i]
      if (!data) continue
      data.life += delta

      // 更新位置（和粒子一致）
      data.sprite.position.x += data.velocity.x * delta * 10
      data.sprite.position.y += data.velocity.y * delta * 10
      data.sprite.position.z += data.velocity.z * delta * 10

      // 淡出效果
      const opacity = Math.max(0, 1 - data.life / 3)
      ;(data.sprite.material as THREE.SpriteMaterial).opacity = opacity

      // 超出范围或完全透明则移除
      if (data.sprite.position.y > 2.0 || opacity <= 0) {
        scene.remove(data.sprite)
        data.sprite.material.dispose()
        buffSprites.splice(i, 1)
      }
    }

    // 更新粒子
    if (!particles || !particlePositions || !particleVelocities || activeCount === 0) return

    for (let i = 0; i < activeCount; i++) {
      const i3 = i * 3
      particlePositions[i3] = particlePositions[i3]! + particleVelocities[i3]! * delta * 10
      particlePositions[i3 + 1] = particlePositions[i3 + 1]! + particleVelocities[i3 + 1]! * delta * 10
      particlePositions[i3 + 2] = particlePositions[i3 + 2]! + particleVelocities[i3 + 2]! * delta * 10

      if (particlePositions[i3 + 1]! > 2.0) {
        resetParticle(i)
      }
    }

    particles.geometry.attributes.position!.needsUpdate = true
  }

  function dispose() {
    if (particles) {
      particles.geometry.dispose()
      ;(particles.material as THREE.Material).dispose()
      scene.remove(particles)
    }
    buffSprites.forEach(data => {
      scene.remove(data.sprite)
      data.sprite.material.dispose()
    })
    if (buffTexture) buffTexture.dispose()
    scene.remove(scentRing)
  }

  return { updateScent, tick, dispose }
}
