import * as THREE from 'three'
import type { Weather } from '@/types/api'

/**
 * 天气配置
 */
interface WeatherConfig {
  fogDensity: number | null
  particles: 'rain' | 'snow' | null
  lightMod: number
}

const WEATHER_CONFIG: Record<Weather, WeatherConfig> = {
  SUNNY: { fogDensity: null, particles: null, lightMod: 1.0 },
  CLOUDY: { fogDensity: null, particles: null, lightMod: 0.6 },
  RAINY: { fogDensity: 0.015, particles: 'rain', lightMod: 0.4 },
  SNOWY: { fogDensity: 0.01, particles: 'snow', lightMod: 0.5 },
  FOGGY: { fogDensity: 0.04, particles: null, lightMod: 0.3 }
}

// 粒子配置
const RAIN_CONFIG = {
  count: 600,
  speed: 0.8,
  spread: { x: 30, y: 15, z: 60 },
  color: 0xaaaacc,
  size: 0.15
}

const SNOW_CONFIG = {
  count: 300,
  speed: 0.08,
  drift: 0.03,
  spread: { x: 30, y: 15, z: 60 },
  color: 0xffffff,
  size: 0.12
}

/**
 * 天气效果 composable
 */
export function useWeatherEffect(scene: THREE.Scene) {
  let currentWeather: Weather = 'SUNNY'
  let rainParticles: THREE.Points | null = null
  let snowParticles: THREE.Points | null = null
  let rainPositions: Float32Array | null = null
  let snowPositions: Float32Array | null = null
  let snowDrifts: Float32Array | null = null  // 雪花横向漂移相位
  let originalFog: THREE.FogExp2 | THREE.Fog | null = null

  // 初始化雨滴粒子
  function initRain() {
    const geometry = new THREE.BufferGeometry()
    rainPositions = new Float32Array(RAIN_CONFIG.count * 3)

    for (let i = 0; i < RAIN_CONFIG.count; i++) {
      const i3 = i * 3
      rainPositions[i3] = (Math.random() - 0.5) * RAIN_CONFIG.spread.x
      // Y 位置分布在整个下落范围内，避免同时落下
      rainPositions[i3 + 1] = -5 + Math.random() * (RAIN_CONFIG.spread.y + 5)
      rainPositions[i3 + 2] = -Math.random() * RAIN_CONFIG.spread.z
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(rainPositions, 3))

    const material = new THREE.PointsMaterial({
      color: RAIN_CONFIG.color,
      size: RAIN_CONFIG.size,
      transparent: true,
      opacity: 0.6,
      sizeAttenuation: true
    })

    rainParticles = new THREE.Points(geometry, material)
    rainParticles.visible = false
    scene.add(rainParticles)
  }

  // 初始化雪花粒子
  function initSnow() {
    const geometry = new THREE.BufferGeometry()
    snowPositions = new Float32Array(SNOW_CONFIG.count * 3)
    snowDrifts = new Float32Array(SNOW_CONFIG.count)

    for (let i = 0; i < SNOW_CONFIG.count; i++) {
      const i3 = i * 3
      snowPositions[i3] = (Math.random() - 0.5) * SNOW_CONFIG.spread.x
      snowPositions[i3 + 1] = Math.random() * SNOW_CONFIG.spread.y
      snowPositions[i3 + 2] = -Math.random() * SNOW_CONFIG.spread.z
      snowDrifts[i] = Math.random() * Math.PI * 2  // 随机初始相位
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(snowPositions, 3))

    const material = new THREE.PointsMaterial({
      color: SNOW_CONFIG.color,
      size: SNOW_CONFIG.size,
      transparent: true,
      opacity: 0.8,
      sizeAttenuation: true
    })

    snowParticles = new THREE.Points(geometry, material)
    snowParticles.visible = false
    scene.add(snowParticles)
  }

  // 初始化
  initRain()
  initSnow()

  /**
   * 更新天气
   */
  function updateWeather(weather: Weather) {
    // 转换为大写以匹配配置键
    const weatherKey = weather.toUpperCase() as Weather
    if (currentWeather === weatherKey) return
    currentWeather = weatherKey

    const config = WEATHER_CONFIG[weatherKey]
    if (!config) {
      console.warn('[WeatherEffect] Unknown weather:', weather)
      return
    }

    // 更新雾效果
    if (config.fogDensity) {
      scene.fog = new THREE.FogExp2(0x888888, config.fogDensity)
    } else {
      scene.fog = null
    }

    // 更新粒子可见性
    if (rainParticles) {
      rainParticles.visible = config.particles === 'rain'
    }
    if (snowParticles) {
      snowParticles.visible = config.particles === 'snow'
    }

    console.log('[WeatherEffect] Weather changed to:', weather)
  }

  /**
   * 动画帧更新
   */
  function tick(delta: number) {
    const config = WEATHER_CONFIG[currentWeather]
    if (!config) return  // 安全检查

    // 限制 delta 最大值，防止挂机后恢复时一次性移动太多
    const clampedDelta = Math.min(delta, 0.1)

    // 更新雨滴
    if (config.particles === 'rain' && rainPositions && rainParticles) {
      for (let i = 0; i < RAIN_CONFIG.count; i++) {
        const i3 = i * 3
        rainPositions[i3 + 1] = (rainPositions[i3 + 1] ?? 0) - RAIN_CONFIG.speed * clampedDelta * 60

        // 重置到顶部
        if ((rainPositions[i3 + 1] ?? 0) < 0) {
          rainPositions[i3] = (Math.random() - 0.5) * RAIN_CONFIG.spread.x
          rainPositions[i3 + 1] = RAIN_CONFIG.spread.y
          rainPositions[i3 + 2] = -Math.random() * RAIN_CONFIG.spread.z
        }
      }
      rainParticles.geometry.attributes.position!.needsUpdate = true
    }

    // 更新雪花
    if (config.particles === 'snow' && snowPositions && snowDrifts && snowParticles) {
      for (let i = 0; i < SNOW_CONFIG.count; i++) {
        const i3 = i * 3
        snowPositions[i3 + 1] = (snowPositions[i3 + 1] ?? 0) - SNOW_CONFIG.speed * clampedDelta * 60

        // 横向漂移（正弦波动）
        snowDrifts[i] = (snowDrifts[i] ?? 0) + clampedDelta * 2
        snowPositions[i3] = (snowPositions[i3] ?? 0) + Math.sin(snowDrifts[i] ?? 0) * SNOW_CONFIG.drift * clampedDelta * 60

        // 重置到顶部
        if ((snowPositions[i3 + 1] ?? 0) < 0) {
          snowPositions[i3] = (Math.random() - 0.5) * SNOW_CONFIG.spread.x
          snowPositions[i3 + 1] = SNOW_CONFIG.spread.y
          snowPositions[i3 + 2] = -Math.random() * SNOW_CONFIG.spread.z
          snowDrifts[i] = Math.random() * Math.PI * 2
        }
      }
      snowParticles.geometry.attributes.position!.needsUpdate = true
    }
  }

  /**
   * 获取当前天气的光照修正系数
   */
  function getLightModifier(): number {
    return WEATHER_CONFIG[currentWeather].lightMod
  }

  /**
   * 清理资源
   */
  function dispose() {
    if (rainParticles) {
      scene.remove(rainParticles)
      rainParticles.geometry.dispose()
      ;(rainParticles.material as THREE.PointsMaterial).dispose()
    }
    if (snowParticles) {
      scene.remove(snowParticles)
      snowParticles.geometry.dispose()
      ;(snowParticles.material as THREE.PointsMaterial).dispose()
    }
    scene.fog = null
  }

  return {
    updateWeather,
    tick,
    getLightModifier,
    dispose
  }
}
