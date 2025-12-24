import * as THREE from 'three'
import type { LightSetting } from '@/types/api'
import type { AmbienceUniforms, CarInteriorMeshes } from '@/types/three'

// Shader 代码
const vertexShader = `
varying vec2 vUv;
void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`

const fragmentShader = `
uniform float uTime;
uniform vec3 uColorA;
uniform vec3 uColorB;
uniform float uSpeed;
uniform float uSharpness;
uniform float uBrightness;
varying vec2 vUv;

void main() {
    float coord = vUv.x;
    float phase = uTime * uSpeed;
    float wave = sin(coord * 6.28 + phase);
    wave = (wave + 1.0) * 0.5;
    wave = pow(wave, uSharpness);
    vec3 finalColor = mix(uColorA, uColorB, wave);
    // 最低50%亮度，避免太暗
    float brightness = 0.5 + 0.5 * uBrightness;
    gl_FragColor = vec4(finalColor * brightness, 1.0);
}
`

export interface UseAmbienceLightReturn {
  updateLight: (setting: LightSetting) => void
  uniforms: AmbienceUniforms
  tick: (delta: number) => void
}

export function useAmbienceLight(
  scene: THREE.Scene,
  meshes: CarInteriorMeshes
): UseAmbienceLightReturn {
  // 创建 Uniforms
  const uniforms: AmbienceUniforms = {
    uTime: { value: 0 },
    uColorA: { value: new THREE.Color(0x00aaff) },
    uColorB: { value: new THREE.Color(0x44ddff) },
    uSpeed: { value: 0.5 },
    uSharpness: { value: 1.0 },
    uBrightness: { value: 1.0 }
  }

  // 创建 ShaderMaterial
  const stripMat = new THREE.ShaderMaterial({
    vertexShader,
    fragmentShader,
    uniforms,
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  })

  // 应用到灯带（共享同一个材质，保证动画同步）
  meshes.dashboardStrip.material = stripMat
  meshes.leftDoorStrip.material = stripMat
  meshes.rightDoorStrip.material = stripMat

  // 创建 RectAreaLight
  const dashLight = new THREE.RectAreaLight(0x00aaff, 20, 2.2, 0.1)
  dashLight.position.set(0, 0.72, -0.48)
  dashLight.lookAt(0, 0.72, 0)
  scene.add(dashLight)

  const lDoorLight = new THREE.RectAreaLight(0x00aaff, 10, 0.1, 1.5)
  lDoorLight.position.set(-0.99, 0.8, 0)
  lDoorLight.lookAt(0, 0.8, 0)
  scene.add(lDoorLight)

  const rDoorLight = new THREE.RectAreaLight(0x00aaff, 10, 0.1, 1.5)
  rDoorLight.position.set(0.99, 0.8, 0)
  rDoorLight.lookAt(0, 0.8, 0)
  scene.add(rDoorLight)

  // 屏幕发光
  const screenLight = new THREE.RectAreaLight(0xffffff, 15, 0.8, 0.3)
  screenLight.position.set(0, 1.05, -0.48)
  screenLight.rotation.x = -0.3
  scene.add(screenLight)

  function updateLight(setting: LightSetting) {
    console.log('[useAmbienceLight] updateLight:', setting)

    // 更新主色
    if (setting.color?.hex) {
      console.log('[useAmbienceLight] 设置颜色:', setting.color.hex)
      uniforms.uColorA.value.set(setting.color.hex)
      dashLight.color.set(setting.color.hex)
      lDoorLight.color.set(setting.color.hex)
      rDoorLight.color.set(setting.color.hex)
    }

    // 更新副色
    if (setting.colorB?.hex) {
      uniforms.uColorB.value.set(setting.colorB.hex)
    } else if (setting.color?.hex) {
      // 如果没有副色，生成对比色（色相偏移+亮度提升）
      uniforms.uColorB.value.set(setting.color.hex)
      uniforms.uColorB.value.offsetHSL(0.1, 0.2, 0.3)
    }

    // 更新亮度
    const brightnessRatio = setting.brightness / 100
    uniforms.uBrightness.value = brightnessRatio
    // 同步更新 RectAreaLight 的 intensity
    dashLight.intensity = 20 * brightnessRatio
    lDoorLight.intensity = 10 * brightnessRatio
    rDoorLight.intensity = 10 * brightnessRatio

    // 更新速度：优先使用 setting.speed，否则根据 mode fallback
    if (setting.speed !== undefined && setting.speed !== null) {
      uniforms.uSpeed.value = setting.speed
    } else {
      switch (setting.mode) {
        case 'STATIC':
          uniforms.uSpeed.value = 0
          break
        case 'BREATHING':
          uniforms.uSpeed.value = 1.0
          break
        case 'GRADIENT':
          uniforms.uSpeed.value = 2.0
          break
        case 'PULSE':
          uniforms.uSpeed.value = 4.0
          break
        default:
          uniforms.uSpeed.value = 1.5
      }
    }

    // 更新锐度
    if (setting.sharpness !== undefined && setting.sharpness !== null) {
      uniforms.uSharpness.value = setting.sharpness
    }
  }

  function tick(delta: number) {
    uniforms.uTime.value += delta
  }

  return { updateLight, uniforms, tick }
}
