import * as THREE from 'three'
import { Sky } from 'three/addons/objects/Sky.js'
import type { TimeOfDay } from '@/types/api'
import type { SceneContext, TimeLightingConfig } from '@/types/three'

// 时间光照配置表
const TIME_LIGHTING: Record<TimeOfDay, TimeLightingConfig> = {
  DAWN: {
    ambient: new THREE.Color(0xffeedd),
    ambientIntensity: 0.3,
    sun: new THREE.Color(0xff8844),
    sunIntensity: 0.4,
    sunPosition: new THREE.Vector3(-3, 1, -5),
    background: new THREE.Color(0x1a1520),
    skyElevation: 5,
    skyAzimuth: 180
  },
  MORNING: {
    ambient: new THREE.Color(0xffffff),
    ambientIntensity: 0.5,
    sun: new THREE.Color(0xffffee),
    sunIntensity: 0.6,
    sunPosition: new THREE.Vector3(-2, 3, -4),
    background: new THREE.Color(0x202530),
    skyElevation: 30,
    skyAzimuth: 180
  },
  NOON: {
    ambient: new THREE.Color(0xffffff),
    ambientIntensity: 0.7,
    sun: new THREE.Color(0xffffff),
    sunIntensity: 0.8,
    sunPosition: new THREE.Vector3(0, 5, -3),
    background: new THREE.Color(0x303540),
    skyElevation: 80,
    skyAzimuth: 180
  },
  AFTERNOON: {
    ambient: new THREE.Color(0xffffff),
    ambientIntensity: 0.6,
    sun: new THREE.Color(0xffffdd),
    sunIntensity: 0.7,
    sunPosition: new THREE.Vector3(2, 3, -4),
    background: new THREE.Color(0x252535),
    skyElevation: 50,
    skyAzimuth: 200
  },
  EVENING: {
    ambient: new THREE.Color(0xffddcc),
    ambientIntensity: 0.4,
    sun: new THREE.Color(0xff6633),
    sunIntensity: 0.5,
    sunPosition: new THREE.Vector3(3, 1, -5),
    background: new THREE.Color(0x1a1015),
    skyElevation: 10,
    skyAzimuth: 250
  },
  NIGHT: {
    ambient: new THREE.Color(0x334455),
    ambientIntensity: 0.15,
    sun: new THREE.Color(0x111122),
    sunIntensity: 0.05,
    sunPosition: new THREE.Vector3(0, -2, -5),
    background: new THREE.Color(0x020204),
    skyElevation: -30,
    skyAzimuth: 270
  },
  MIDNIGHT: {
    ambient: new THREE.Color(0x223344),
    ambientIntensity: 0.1,
    sun: new THREE.Color(0x111111),
    sunIntensity: 0.02,
    sunPosition: new THREE.Vector3(0, -3, -5),
    background: new THREE.Color(0x010102),
    skyElevation: -30,
    skyAzimuth: 270
  }
}

export interface UseTimeOfDayOptions {
  headLights?: THREE.SpotLight[]
  headLightBulbs?: THREE.Mesh[]
}

export interface UseTimeOfDayReturn {
  updateTime: (timeOfDay: TimeOfDay) => void
  getCurrentConfig: () => TimeLightingConfig | null
}

export function useTimeOfDay(context: SceneContext, options?: UseTimeOfDayOptions): UseTimeOfDayReturn {
  const { scene } = context
  const headLights = options?.headLights ?? []
  const headLightBulbs = options?.headLightBulbs ?? []

  let currentConfig: TimeLightingConfig | null = null
  let sky: Sky | null = null
  let ambientLight: THREE.AmbientLight | null = null
  let sunLight: THREE.DirectionalLight | null = null

  // 初始化 Sky
  sky = new Sky()
  sky.scale.setScalar(450000)
  scene.add(sky)

  const skyUniforms = sky.material.uniforms
  skyUniforms['turbidity'].value = 1
  skyUniforms['rayleigh'].value = 0.5
  skyUniforms['mieCoefficient'].value = 0.003
  skyUniforms['mieDirectionalG'].value = 0.65

  // 初始化光源
  ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)

  sunLight = new THREE.DirectionalLight(0xffffff, 0.5)
  sunLight.position.set(0, 5, -3)
  scene.add(sunLight)

  // 补光
  const fillLight = new THREE.DirectionalLight(0x8888ff, 0.2)
  fillLight.position.set(2, 3, 2)
  scene.add(fillLight)

  // 车内顶灯
  const interiorLight = new THREE.PointLight(0xffffff, 0.5)
  interiorLight.position.set(0, 1.5, -0.5)
  scene.add(interiorLight)

  function updateTime(timeOfDay: TimeOfDay) {
    const key = timeOfDay.toUpperCase() as TimeOfDay
    const config = TIME_LIGHTING[key]
    if (!config) {
      console.warn('[useTimeOfDay] Unknown timeOfDay:', timeOfDay)
      return
    }

    currentConfig = config

    // 更新环境光
    if (ambientLight) {
      ambientLight.color.copy(config.ambient)
      ambientLight.intensity = config.ambientIntensity
    }

    // 更新太阳光
    if (sunLight) {
      sunLight.color.copy(config.sun)
      sunLight.intensity = config.sunIntensity
      sunLight.position.copy(config.sunPosition)
    }

    // 更新天空
    if (sky) {
      const phi = THREE.MathUtils.degToRad(90 - config.skyElevation)
      const theta = THREE.MathUtils.degToRad(config.skyAzimuth)
      const sunVector = new THREE.Vector3()
      sunVector.setFromSphericalCoords(1, phi, theta)
      sky.material.uniforms['sunPosition'].value.copy(sunVector)

      // 夜晚隐藏天空
      if (config.skyElevation < -10) {
        sky.visible = false
        scene.background = config.background
      } else {
        sky.visible = true
      }
    }

    // 控制远光灯：只在夜晚/傍晚开启
    const isNight = config.skyElevation < 20
    headLights.forEach(light => {
      light.visible = isNight
    })
    headLightBulbs.forEach(bulb => {
      bulb.visible = isNight
    })
  }

  function getCurrentConfig() {
    return currentConfig
  }

  // 默认设置为 NOON
  updateTime('NOON')

  return {
    updateTime,
    getCurrentConfig
  }
}
