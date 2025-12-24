import type * as THREE from 'three'

// ============ 场景上下文 ============

export interface SceneContext {
  scene: THREE.Scene
  camera: THREE.PerspectiveCamera
  renderer: THREE.WebGLRenderer
  clock: THREE.Clock
}

// ============ 氛围灯 Uniforms ============

export interface AmbienceUniforms {
  uTime: { value: number }
  uColorA: { value: THREE.Color }
  uColorB: { value: THREE.Color }
  uSpeed: { value: number }
  uSharpness: { value: number }
  uBrightness: { value: number }
}

// ============ 时间光照配置 ============

export interface TimeLightingConfig {
  ambient: THREE.Color
  ambientIntensity: number
  sun: THREE.Color
  sunIntensity: number
  sunPosition: THREE.Vector3
  background: THREE.Color
  skyElevation: number
  skyAzimuth: number
}

// ============ 香氛粒子配置 ============

export interface ScentParticleConfig {
  color: number
  buff: string
  emoji: string
}

// ============ 车内几何体引用 ============

export interface CarInteriorMeshes {
  dashboardStrip: THREE.Mesh
  leftDoorStrip: THREE.Mesh
  rightDoorStrip: THREE.Mesh
  screen: THREE.Mesh
  screenTexture: THREE.CanvasTexture
  headLights: THREE.SpotLight[]
  headLightBulbs: THREE.Mesh[]
}

// ============ 动画回调类型 ============

export type AnimationCallback = (delta: number) => void
