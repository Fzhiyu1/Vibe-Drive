<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useAMap, type RouteStep } from '@/composables/useAMap'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()
const {
  isLoaded,
  isPlaying,
  map,
  marker,
  init,
  drawRoute,
  startAnimation,
  pauseAnimation,
  setHandlers,
  getCurrentPathIndex
} = useAMap('map-panel-container')

// 拖拽状态
const panelRef = ref<HTMLElement | null>(null)
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 控制状态
const speed = ref(1)
const duration = ref(60000)
const currentRoad = ref('')
const isRouteLoaded = ref(false)
const lastPosition = ref<[number, number] | null>(null)

// 监听行驶模拟状态
watch(() => store.drivingSimulationActive, (active) => {
  if (active && isLoaded.value) {
    // 隐藏位置标记
    if (locationMarker.value) {
      locationMarker.value.hide()
    }
    loadRouteAndStart()
  } else if (!active) {
    // 停止动画
    pauseAnimation()
    // 显示位置标记
    if (locationMarker.value) {
      locationMarker.value.show()
    }
  }
})

// 监听环境变化，更新地图位置
watch(() => store.environment?.location, (location) => {
  if (location && isLoaded.value && !store.drivingSimulationActive) {
    // 非行驶模拟时，根据环境坐标更新地图
    updateMapPosition(location.longitude, location.latitude)
  }
}, { deep: true })

// 预设路线：上海 → 杭州
const presetRoute: RouteStep[] = [
  {
    road: '人民大道',
    instruction: '从人民广场出发',
    distance: 500,
    polyline: '121.473701,31.230416;121.474,31.229;121.475,31.228'
  },
  {
    road: '延安高架',
    instruction: '进入延安高架',
    distance: 5000,
    polyline: '121.475,31.228;121.48,31.225;121.50,31.22;121.55,31.21'
  },
  {
    road: 'G60沪昆高速',
    instruction: '进入G60沪昆高速',
    distance: 150000,
    polyline: '121.55,31.21;121.4,31.1;121.2,30.9;121.0,30.7;120.8,30.5;120.6,30.4;120.4,30.35;120.2,30.3'
  },
  {
    road: '杭州绕城高速',
    instruction: '进入杭州绕城高速',
    distance: 20000,
    polyline: '120.2,30.3;120.18,30.29;120.16,30.28;120.155,30.274'
  }
]

onMounted(async () => {
  await init({ center: [120.8, 30.8], zoom: 6 })

  setHandlers({
    onPositionChange: (pos) => {
      lastPosition.value = pos
    },
    onStepChange: (index, step) => {
      currentRoad.value = step.road
      updateEnvironment(step)
    }
  })
})

function loadRoute() {
  drawRoute(presetRoute)
  currentRoad.value = presetRoute[0].road
  isRouteLoaded.value = true
}

function loadRouteAndStart() {
  if (!isRouteLoaded.value) {
    loadRoute()
  }
  // 先把地图移动到起点（上海）
  if (map.value) {
    map.value.setCenter([121.473701, 31.230416])
    map.value.setZoom(8)
  }
  startAnimation(duration.value / speed.value, 0)
}

// 位置标记（非行驶模拟时使用）
const locationMarker = ref<any>(null)

// 更新地图位置（非行驶模拟时）
function updateMapPosition(lng: number, lat: number) {
  if (!map.value) return
  map.value.setCenter([lng, lat])
  map.value.setZoom(13)

  // 清除行驶路线
  if (isRouteLoaded.value) {
    // 如果有路线，先不清除，让用户可以看到
  }

  // 创建或更新位置标记
  if (!locationMarker.value) {
    // 创建圆点标记
    const canvas = document.createElement('canvas')
    canvas.width = 24
    canvas.height = 24
    const ctx = canvas.getContext('2d')!
    ctx.beginPath()
    ctx.arc(12, 12, 10, 0, Math.PI * 2)
    ctx.fillStyle = '#ef4444'
    ctx.fill()
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()

    locationMarker.value = new (window as any).AMap.Marker({
      position: [lng, lat],
      icon: canvas.toDataURL(),
      offset: new (window as any).AMap.Pixel(-12, -12)
    })
    map.value.add(locationMarker.value)
  } else {
    locationMarker.value.setPosition([lng, lat])
  }
}

function togglePlay() {
  if (!isRouteLoaded.value) {
    loadRoute()
  }

  if (isPlaying.value) {
    pauseAnimation()
  } else {
    startAnimation(duration.value / speed.value, 0)
  }
}

function updateEnvironment(step: RouteStep) {
  const roadName = step.road
  let gpsTag: 'CITY_ROAD' | 'HIGHWAY' | 'CITY_EXPRESSWAY' = 'CITY_ROAD'
  let cityName = store.environment?.location?.cityName || '上海'

  if (roadName.includes('高速')) {
    gpsTag = 'HIGHWAY'
    if (roadName.includes('杭州')) {
      cityName = '杭州'
    }
  } else if (roadName.includes('高架')) {
    gpsTag = 'CITY_EXPRESSWAY'
  }

  const pos = lastPosition.value || [121.473701, 31.230416]

  // 只更新位置信息，保留其他环境数据
  store.setEnvironment({
    gpsTag,
    location: {
      ...store.environment?.location,
      latitude: pos[1],
      longitude: pos[0],
      cityName,
      roadName
    }
  })
}

function adjustSpeed(delta: number) {
  speed.value = Math.max(0.5, Math.min(4, speed.value + delta))
  if (isPlaying.value) {
    const currentIndex = getCurrentPathIndex()
    pauseAnimation()
    startAnimation(duration.value / speed.value, currentIndex)
  }
}

// 拖拽功能
function startDrag(e: MouseEvent) {
  if (!panelRef.value) return
  isDragging.value = true
  const rect = panelRef.value.getBoundingClientRect()
  dragOffset.value = {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault()
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value || !panelRef.value) return
  const parent = panelRef.value.parentElement
  if (!parent) return

  const parentRect = parent.getBoundingClientRect()
  let x = e.clientX - parentRect.left - dragOffset.value.x
  let y = e.clientY - parentRect.top - dragOffset.value.y

  // 限制在父容器内
  x = Math.max(0, Math.min(x, parentRect.width - panelRef.value.offsetWidth))
  y = Math.max(0, Math.min(y, parentRect.height - panelRef.value.offsetHeight))

  panelRef.value.style.left = `${x}px`
  panelRef.value.style.top = `${y}px`
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})
</script>

<template>
  <div ref="panelRef" class="map-panel" :class="{ dragging: isDragging }">
    <div class="panel-header" @mousedown="startDrag">
      <span class="title">地图</span>
      <span class="route-label" v-if="store.environment?.location?.cityName">
        {{ store.environment.location.cityName }}
        <template v-if="store.drivingSimulationActive"> → 杭州</template>
      </span>
    </div>

    <!-- 地图容器 -->
    <div id="map-panel-container" class="map-container"></div>

    <!-- 控制区 -->
    <div class="controls" v-if="store.drivingSimulationActive">
      <div class="info-row">
        <span class="label">路段</span>
        <span class="value">{{ currentRoad || '行驶中' }}</span>
      </div>

      <div class="speed-row">
        <span class="label">速度</span>
        <div class="speed-controls">
          <button class="speed-btn" @click="adjustSpeed(-0.5)">-</button>
          <span class="speed-value">{{ speed }}x</span>
          <button class="speed-btn" @click="adjustSpeed(0.5)">+</button>
        </div>
      </div>

      <button
        class="play-btn"
        @click="togglePlay"
      >
        {{ isPlaying ? '暂停' : '继续' }}
      </button>
    </div>

    <!-- 未激活时的提示 -->
    <div class="controls hint" v-else>
      <span v-if="store.environment?.location">
        {{ store.environment.location.roadName || '当前位置' }}
      </span>
      <span v-else>选择场景查看位置</span>
    </div>
  </div>
</template>

<style scoped>
.map-panel {
  display: flex;
  flex-direction: column;
  background: var(--bg-tertiary);
  border-radius: 8px;
  overflow: hidden;
}

.map-panel.dragging {
  opacity: 0.9;
  user-select: none;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: var(--bg-secondary);
  cursor: move;
  border-bottom: 1px solid var(--border-color);
}

.title {
  font-weight: 600;
  font-size: 0.9rem;
}

.route-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.map-container {
  width: 100%;
  height: 180px;
}

.controls {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.controls.hint {
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.info-row, .speed-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}

.label {
  color: var(--text-secondary);
}

.value {
  color: var(--text-primary);
}

.speed-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.speed-btn {
  width: 24px;
  height: 24px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.speed-btn:hover {
  background: var(--bg-tertiary);
}

.speed-value {
  width: 32px;
  text-align: center;
  font-weight: 500;
}

.play-btn {
  width: 100%;
  padding: 0.5rem;
  border: none;
  border-radius: 6px;
  background: var(--accent);
  color: white;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
}

.play-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.play-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
