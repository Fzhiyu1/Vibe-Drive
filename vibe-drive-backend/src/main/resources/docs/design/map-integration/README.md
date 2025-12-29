# 地图集成设计文档

## 概述

在 Web 端集成高德地图，实现位置可视化和模拟车辆行驶功能。

## 目标

1. 显示地图，用户可查看/选择位置
2. 模拟车辆沿高速公路行驶
3. 位置变化触发环境数据更新
4. 环境变化触发氛围更新

## 技术选型

| 项目 | 选型 |
|------|------|
| 地图 SDK | 高德 JS API 2.0 |
| 加载方式 | @amap/amap-jsapi-loader |
| 路线绘制 | AMap.Polyline |
| 车辆动画 | AMap.MoveAnimation |
| 环境更新 | steps 关键点触发 |

## 架构设计

```
┌─────────────────────────────────────────────────────┐
│                    MapView.vue                       │
│  ┌───────────────┐  ┌───────────────────────────┐   │
│  │   地图容器     │  │      控制面板              │   │
│  │  (AMap)       │  │  - 开始/暂停              │   │
│  │               │  │  - 速度调节               │   │
│  │  [车辆标记]   │  │  - 路线选择               │   │
│  │  [路线]       │  │                           │   │
│  └───────────────┘  └───────────────────────────┘   │
└─────────────────────────────────────────────────────┘
          │
          │ 关键点触发
          ▼
┌─────────────────────────────────────────────────────┐
│                   vibeStore                          │
│  - setEnvironment(location, gpsTag, ...)            │
└─────────────────────────────────────────────────────┘
          │
          │ 环境变化
          ▼
┌─────────────────────────────────────────────────────┐
│                   氛围系统                           │
│  - 灯光/音乐/香氛/叙事                              │
└─────────────────────────────────────────────────────┘
```

## 数据流

### 1. 路线数据获取

```
后端路线规划 API → 返回 steps + polyline → 前端存储
```

### 2. 车辆移动

```
用户点击开始 → MoveAnimation 沿 polyline 移动 → 监听位置变化
```

### 3. 环境更新

```
经过 step 边界 → 调用逆地理编码 → 更新 vibeStore.environment
```

## 组件设计

### MapView.vue

主组件，包含地图和控制面板。

**Props**：无

**State**：
- `isPlaying`: 是否正在播放
- `speed`: 播放速度倍率
- `currentStep`: 当前所在路段

### useAMap.ts

地图初始化和操作的 composable。

```typescript
export function useAMap(containerId: string) {
  const map = shallowRef<AMap.Map | null>(null)
  const marker = shallowRef<AMap.Marker | null>(null)

  async function init() { ... }
  function drawRoute(path: number[][]) { ... }
  function startAnimation(path: number[][], duration: number) { ... }
  function pauseAnimation() { ... }

  return { map, marker, init, drawRoute, startAnimation, pauseAnimation }
}
```

## API 配置

| Key 类型 | Key | 用途 |
|----------|-----|------|
| Web服务 | `a0d1d4b964ebf5e1aafebf3d0268222b` | 后端逆地理编码 |
| JS API | `46200126775d0027fe154b5bcce66076` | 前端地图显示 |

## 实现步骤

1. 安装依赖 `@amap/amap-jsapi-loader`
2. 创建 `useAMap.ts` composable
3. 创建 `MapView.vue` 组件
4. 添加路由 `/map`
5. 集成 vibeStore 环境更新
6. 测试完整流程
