import { ref, shallowRef, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

// 高德 JS API Key
const AMAP_KEY = '46200126775d0027fe154b5bcce66076'

// 类型声明
declare global {
  interface Window {
    _AMapSecurityConfig: { securityJsCode: string }
  }
}

export interface RouteStep {
  road: string
  instruction: string
  distance: number
  polyline: string  // "lng,lat;lng,lat;..."
}

export interface MapHandlers {
  onPositionChange?: (position: [number, number]) => void
  onStepChange?: (stepIndex: number, step: RouteStep) => void
}

/**
 * 高德地图 composable
 * 用于地图显示、路线绘制、车辆动画
 */
export function useAMap(containerId: string) {
  const isLoaded = ref(false)
  const isPlaying = ref(false)
  const currentPosition = ref<[number, number] | null>(null)
  const currentStepIndex = ref(0)

  // 使用 shallowRef 避免深度响应
  const map = shallowRef<any>(null)
  const marker = shallowRef<any>(null)
  const polyline = shallowRef<any>(null)

  let AMap: any = null
  let handlers: MapHandlers = {}
  let routeSteps: RouteStep[] = []
  let fullPath: [number, number][] = []

  /**
   * 初始化地图
   */
  async function init(options?: { center?: [number, number]; zoom?: number }) {
    if (isLoaded.value) return

    // 安全配置（生产环境需要配置）
    window._AMapSecurityConfig = {
      securityJsCode: ''
    }

    try {
      AMap = await AMapLoader.load({
        key: AMAP_KEY,
        version: '2.0',
        plugins: ['AMap.MoveAnimation']
      })

      map.value = new AMap.Map(containerId, {
        zoom: options?.zoom ?? 10,
        center: options?.center ?? [121.473701, 31.230416], // 默认上海
        mapStyle: 'amap://styles/dark'  // 深色主题
      })

      isLoaded.value = true
    } catch (error) {
      console.error('地图加载失败:', error)
      throw error
    }
  }

  /**
   * 绘制路线
   * @param steps 路线步骤数组
   */
  function drawRoute(steps: RouteStep[]) {
    if (!map.value || !AMap) return

    routeSteps = steps
    fullPath = []

    // 解析所有坐标点
    for (const step of steps) {
      const points = step.polyline.split(';').map(p => {
        const [lng, lat] = p.split(',').map(Number)
        return [lng, lat] as [number, number]
      })
      fullPath.push(...points)
    }

    // 清除旧路线
    if (polyline.value) {
      map.value.remove(polyline.value)
    }

    // 绘制新路线
    polyline.value = new AMap.Polyline({
      path: fullPath,
      strokeColor: '#3366FF',
      strokeWeight: 6,
      strokeOpacity: 0.8,
      lineJoin: 'round'
    })
    map.value.add(polyline.value)

    // 自适应视野
    map.value.setFitView([polyline.value])

    // 创建车辆标记（圆点）
    if (marker.value) {
      map.value.remove(marker.value)
    }

    // 使用 canvas 创建圆点图标
    const canvas = document.createElement('canvas')
    canvas.width = 20
    canvas.height = 20
    const ctx = canvas.getContext('2d')!
    ctx.beginPath()
    ctx.arc(10, 10, 8, 0, Math.PI * 2)
    ctx.fillStyle = '#3366FF'
    ctx.fill()
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()

    marker.value = new AMap.Marker({
      position: fullPath[0],
      icon: canvas.toDataURL(),
      offset: new AMap.Pixel(-10, -10)
    })
    map.value.add(marker.value)

    currentPosition.value = fullPath[0] ?? null
    currentStepIndex.value = 0
  }

  /**
   * 开始/继续动画
   * @param duration 总时长（毫秒）
   * @param fromIndex 从第几个点开始（用于速度调节时从当前位置继续）
   */
  function startAnimation(duration: number = 30000, fromIndex: number = 0) {
    if (!marker.value || !fullPath.length) return

    isPlaying.value = true

    // 获取剩余路径
    const remainingPath = fromIndex > 0 ? fullPath.slice(fromIndex) : fullPath
    if (remainingPath.length < 2) return

    // 计算剩余时长
    const remainingDuration = fromIndex > 0
      ? duration * (remainingPath.length / fullPath.length)
      : duration

    // 监听移动事件
    marker.value.on('moving', (e: any) => {
      const pos = e.passedPath[e.passedPath.length - 1]
      currentPosition.value = [pos.lng, pos.lat]
      handlers.onPositionChange?.([pos.lng, pos.lat])

      // 检查是否进入新路段
      checkStepChange([pos.lng, pos.lat])
    })

    marker.value.on('movealong', () => {
      isPlaying.value = false
    })

    // 开始移动
    marker.value.moveAlong(remainingPath, {
      duration: remainingDuration,
      autoRotation: true
    })
  }

  /**
   * 获取当前位置在路径中的索引
   */
  function getCurrentPathIndex(): number {
    if (!currentPosition.value) return 0
    const pos = currentPosition.value
    const index = fullPath.findIndex(
      p => Math.abs(p[0] - pos[0]) < 0.001 && Math.abs(p[1] - pos[1]) < 0.001
    )
    return index > 0 ? index : 0
  }

  /**
   * 暂停动画
   */
  function pauseAnimation() {
    if (!marker.value) return
    marker.value.pauseMove()
    isPlaying.value = false
  }

  /**
   * 继续动画
   */
  function resumeAnimation() {
    if (!marker.value) return
    marker.value.resumeMove()
    isPlaying.value = true
  }

  /**
   * 停止动画
   */
  function stopAnimation() {
    if (!marker.value) return
    marker.value.stopMove()
    isPlaying.value = false
  }

  /**
   * 检查是否进入新路段
   */
  function checkStepChange(position: [number, number]) {
    if (!routeSteps.length) return

    // 找到当前位置在 fullPath 中的索引
    const currentIndex = fullPath.findIndex(
      p => Math.abs(p[0] - position[0]) < 0.001 && Math.abs(p[1] - position[1]) < 0.001
    )
    if (currentIndex === -1) return

    // 计算当前位置属于哪个路段
    let accumulatedPoints = 0
    for (let i = 0; i < routeSteps.length; i++) {
      const step = routeSteps[i]
      if (!step) continue
      const stepPoints = step.polyline.split(';').length
      accumulatedPoints += stepPoints

      if (currentIndex < accumulatedPoints) {
        // 只在路段真正变化时触发
        if (i !== currentStepIndex.value) {
          currentStepIndex.value = i
          handlers.onStepChange?.(i, step)
        }
        break
      }
    }
  }

  /**
   * 设置事件处理器
   */
  function setHandlers(h: MapHandlers) {
    handlers = h
  }

  /**
   * 销毁地图
   */
  function destroy() {
    if (map.value) {
      map.value.destroy()
      map.value = null
    }
    marker.value = null
    polyline.value = null
    isLoaded.value = false
  }

  onUnmounted(() => {
    destroy()
  })

  return {
    // 状态
    isLoaded,
    isPlaying,
    currentPosition,
    currentStepIndex,
    map,
    marker,
    // 方法
    init,
    drawRoute,
    startAnimation,
    pauseAnimation,
    resumeAnimation,
    stopAnimation,
    setHandlers,
    getCurrentPathIndex,
    destroy
  }
}
