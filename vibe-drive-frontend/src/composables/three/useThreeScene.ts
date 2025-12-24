import { ref, onMounted, onUnmounted, type Ref, shallowRef } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { RectAreaLightUniformsLib } from 'three/addons/lights/RectAreaLightUniformsLib.js'
import type { SceneContext, AnimationCallback } from '@/types/three'

export interface UseThreeSceneOptions {
  container: Ref<HTMLElement | null>
  enableControls?: boolean
}

export interface UseThreeSceneReturn {
  context: Ref<SceneContext | null>
  isReady: Ref<boolean>
  addToAnimationLoop: (callback: AnimationCallback) => void
  removeFromAnimationLoop: (callback: AnimationCallback) => void
}

export function useThreeScene(options: UseThreeSceneOptions): UseThreeSceneReturn {
  const { container, enableControls = true } = options

  const context = shallowRef<SceneContext | null>(null)
  const isReady = ref(false)
  const animationCallbacks = new Set<AnimationCallback>()
  let animationId: number | null = null
  let controls: OrbitControls | null = null

  function init() {
    if (!container.value) return

    // 初始化 RectAreaLight 支持
    RectAreaLightUniformsLib.init()

    // 创建场景
    const scene = new THREE.Scene()

    // 创建相机
    const camera = new THREE.PerspectiveCamera(
      60,
      container.value.clientWidth / container.value.clientHeight,
      0.1,
      100
    )
    camera.position.set(0, 1.5, 0.5)
    camera.lookAt(0, 0.9, -1.0)

    // 创建渲染器
    const renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setSize(container.value.clientWidth, container.value.clientHeight)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 0.5
    container.value.appendChild(renderer.domElement)

    // 创建时钟
    const clock = new THREE.Clock()

    // 创建控制器
    if (enableControls) {
      controls = new OrbitControls(camera, renderer.domElement)
      controls.target.set(0, 0.8, -1.5)
      controls.enablePan = false
      controls.maxPolarAngle = Math.PI / 1.9
      controls.minDistance = 0.1
      controls.maxDistance = 5
      controls.update()
    }

    // 保存上下文
    context.value = { scene, camera, renderer, clock }
    isReady.value = true

    // 启动动画循环
    animate()
  }

  function animate() {
    if (!context.value) return

    animationId = requestAnimationFrame(animate)

    const delta = context.value.clock.getDelta()

    // 执行所有动画回调
    animationCallbacks.forEach(callback => callback(delta))

    // 更新控制器
    if (controls) controls.update()

    // 渲染
    context.value.renderer.render(context.value.scene, context.value.camera)
  }

  function handleResize() {
    if (!context.value || !container.value) return

    const { camera, renderer } = context.value
    const width = container.value.clientWidth
    const height = container.value.clientHeight

    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
  }

  function cleanup() {
    if (animationId !== null) {
      cancelAnimationFrame(animationId)
    }

    if (controls) {
      controls.dispose()
    }

    if (context.value) {
      context.value.renderer.dispose()
      context.value.renderer.domElement.remove()
    }

    animationCallbacks.clear()
    context.value = null
    isReady.value = false
  }

  function addToAnimationLoop(callback: AnimationCallback) {
    animationCallbacks.add(callback)
  }

  function removeFromAnimationLoop(callback: AnimationCallback) {
    animationCallbacks.delete(callback)
  }

  onMounted(() => {
    init()
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    cleanup()
  })

  return {
    context,
    isReady,
    addToAnimationLoop,
    removeFromAnimationLoop
  }
}
