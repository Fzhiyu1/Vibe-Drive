<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAMap, type RouteStep } from '@/composables/useAMap'
import { useVibeStore } from '@/stores/vibeStore'

const store = useVibeStore()
const {
  isLoaded,
  isPlaying,
  currentStepIndex,
  init,
  drawRoute,
  startAnimation,
  pauseAnimation,
  resumeAnimation,
  setHandlers,
  getCurrentPathIndex
} = useAMap('map-container')

// 控制状态
const speed = ref(1)
const duration = ref(60000) // 默认60秒
const currentRoad = ref('')
const isRouteLoaded = ref(false)
const lastPosition = ref<[number, number] | null>(null)

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
  await init({ center: [121.473701, 31.230416], zoom: 8 })

  // 设置事件处理器
  setHandlers({
    onPositionChange: (pos) => {
      lastPosition.value = pos
      console.log('位置变化:', pos)
    },
    onStepChange: (index, step) => {
      currentRoad.value = step.road
      console.log('进入路段:', step.road)

      // 触发环境更新
      updateEnvironment(step)
    }
  })
})

function loadRoute() {
  drawRoute(presetRoute)
  currentRoad.value = presetRoute[0].road
  isRouteLoaded.value = true
}

function togglePlay() {
  if (!isRouteLoaded.value) {
    loadRoute()
  }

  if (isPlaying.value) {
    pauseAnimation()
  } else {
    if (currentStepIndex.value === 0) {
      startAnimation(duration.value / speed.value)
    } else {
      resumeAnimation()
    }
  }
}

function updateEnvironment(step: RouteStep) {
  // 根据路段更新环境数据
  const roadName = step.road

  // 简单的路段 → GPS标签映射
  let gpsTag: 'CITY_ROAD' | 'HIGHWAY' | 'CITY_EXPRESSWAY' = 'CITY_ROAD'
  if (roadName.includes('高速')) {
    gpsTag = 'HIGHWAY'
  } else if (roadName.includes('高架')) {
    gpsTag = 'CITY_EXPRESSWAY'
  }

  // 获取当前坐标
  const pos = lastPosition.value || [121.473701, 31.230416]

  // 更新 store 环境
  store.setEnvironment({
    gpsTag,
    location: {
      latitude: pos[1],
      longitude: pos[0],
      roadName
    }
  })
}

function adjustSpeed(delta: number) {
  speed.value = Math.max(0.5, Math.min(4, speed.value + delta))

  // 如果正在播放，从当前位置继续，应用新速度
  if (isPlaying.value) {
    const currentIndex = getCurrentPathIndex()
    pauseAnimation()
    startAnimation(duration.value / speed.value, currentIndex)
  }
}
</script>

<template>
  <div class="map-view">
    <!-- 地图容器 -->
    <div id="map-container" class="map-container"></div>

    <!-- 控制面板 -->
    <div class="control-panel">
      <h3>地图演示</h3>

      <!-- 路线信息 -->
      <div class="route-info">
        <div class="info-item">
          <span class="label">路线</span>
          <span class="value">上海 → 杭州</span>
        </div>
        <div class="info-item">
          <span class="label">当前路段</span>
          <span class="value">{{ currentRoad || '未开始' }}</span>
        </div>
      </div>

      <!-- 速度控制 -->
      <div class="speed-control">
        <span class="label">速度</span>
        <button class="speed-btn" @click="adjustSpeed(-0.5)">-</button>
        <span class="speed-value">{{ speed }}x</span>
        <button class="speed-btn" @click="adjustSpeed(0.5)">+</button>
      </div>

      <!-- 播放控制 -->
      <button
        class="play-btn"
        :disabled="!isLoaded"
        @click="togglePlay"
      >
        {{ isPlaying ? '暂停' : (isRouteLoaded ? '继续' : '开始行驶') }}
      </button>

      <!-- 返回主页 -->
      <router-link to="/" class="back-link">
        返回主页
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.map-view {
  width: 100vw;
  height: 100vh;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
}

.control-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 280px;
  padding: 1.5rem;
  background: rgba(30, 30, 40, 0.95);
  border-radius: 12px;
  border: 1px solid #444;
  color: #fff;
}

.control-panel h3 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
  color: #3b82f6;
}

.route-info {
  margin-bottom: 1rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #444;
}

.info-item .label {
  color: #aaa;
}

.info-item .value {
  font-weight: 500;
}

.speed-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.speed-control .label {
  color: #aaa;
  margin-right: auto;
}

.speed-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #555;
  border-radius: 6px;
  background: #333;
  color: #fff;
  cursor: pointer;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.speed-btn:hover {
  background: #555;
}

.speed-btn:active {
  background: #666;
}

.speed-value {
  width: 40px;
  text-align: center;
  font-weight: 600;
}

.play-btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.play-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.play-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.back-link {
  display: block;
  margin-top: 1rem;
  text-align: center;
  color: #aaa;
  text-decoration: none;
}

.back-link:hover {
  color: #3b82f6;
}
</style>
