# 光照系统设计

## 概述

采用 **Three.js Sky Shader + 全局光照** 方案，实现随时间自动变化的光照效果。

## 依赖导入

```javascript
import { Sky } from 'three/addons/objects/Sky.js';
import { RectAreaLightUniformsLib } from 'three/addons/lights/RectAreaLightUniformsLib.js';

// 必须初始化
RectAreaLightUniformsLib.init();
```

## 渲染器配置

```javascript
const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 0.5;  // 控制曝光，避免太阳刺眼
```

## Sky Shader 配置

```javascript
const sky = new Sky();
sky.scale.setScalar(450000);
scene.add(sky);

const skyUniforms = sky.material.uniforms;
skyUniforms['turbidity'].value = 1;           // 浑浊度
skyUniforms['rayleigh'].value = 0.5;          // 瑞利散射
skyUniforms['mieCoefficient'].value = 0.003;  // 米氏系数
skyUniforms['mieDirectionalG'].value = 0.65;  // 米氏方向性
```

## 时间光照配置

```javascript
const TIME_LIGHTING = {
    dawn: {
        ambient: new THREE.Color(0xffeedd),
        ambientIntensity: 0.3,
        sun: new THREE.Color(0xff8844),
        sunIntensity: 0.4,
        sunPosition: new THREE.Vector3(-3, 1, -5),
        background: new THREE.Color(0x1a1520),
        skyElevation: 5,
        skyAzimuth: 180
    },
    morning: {
        ambient: new THREE.Color(0xffffff),
        ambientIntensity: 0.5,
        sun: new THREE.Color(0xffffee),
        sunIntensity: 0.6,
        sunPosition: new THREE.Vector3(-2, 3, -4),
        background: new THREE.Color(0x202530),
        skyElevation: 30,
        skyAzimuth: 180
    },
    noon: {
        ambient: new THREE.Color(0xffffff),
        ambientIntensity: 0.7,
        sun: new THREE.Color(0xffffff),
        sunIntensity: 0.8,
        sunPosition: new THREE.Vector3(0, 5, -3),
        background: new THREE.Color(0x303540),
        skyElevation: 80,
        skyAzimuth: 180
    },
    evening: {
        ambient: new THREE.Color(0xffddcc),
        ambientIntensity: 0.4,
        sun: new THREE.Color(0xff6633),
        sunIntensity: 0.5,
        sunPosition: new THREE.Vector3(3, 1, -5),
        background: new THREE.Color(0x1a1015),
        skyElevation: 10,
        skyAzimuth: 250  // 西边落下，与夜晚连续
    },
    night: {
        ambient: new THREE.Color(0x334455),
        ambientIntensity: 0.15,
        sun: new THREE.Color(0x111122),
        sunIntensity: 0.05,
        sunPosition: new THREE.Vector3(0, -2, -5),
        background: new THREE.Color(0x020204),
        skyElevation: -30,
        skyAzimuth: 270
    }
};
```

## 时间进度映射

```javascript
const TIME_PROGRESS = {
    dawn: 0.2,
    morning: 0.35,
    noon: 0.5,
    evening: 0.7,
    night: 0.9
};

const timePoints = [
    { p: 0.0, t: 'night' },
    { p: 0.2, t: 'dawn' },
    { p: 0.35, t: 'morning' },
    { p: 0.5, t: 'noon' },
    { p: 0.7, t: 'evening' },
    { p: 0.9, t: 'night' },
    { p: 1.0, t: 'night' }
];
```

## 光源清单

| 光源 | 类型 | 颜色 | 强度 | 位置 | 用途 |
|------|------|------|------|------|------|
| ambientLight | AmbientLight | 随时间变化 | 0.15-0.7 | - | 基础照明 |
| sunLight | DirectionalLight | 随时间变化 | 0.05-0.8 | 随时间变化 | 主光源 |
| fillLight | DirectionalLight | 0x8888ff | 0.2 | (2, 3, 2) | 天空散射补光 |
| interiorLight | PointLight | 0xffffff | 0.5 | (0, 1.5, -0.5) | 车内顶灯 |
| screenLight | RectAreaLight | 0xffffff | 15 | (0, 1.05, -0.48) | 屏幕发光 |
| dashLight | RectAreaLight | 0x00aaff | 20 | (0, 0.72, -0.48) | 仪表台氛围灯 |
| lDoorLight | RectAreaLight | 0x00aaff | 10 | (-0.99, 0.8, 0) | 左车门氛围灯 |
| rDoorLight | RectAreaLight | 0x00aaff | 10 | (0.99, 0.8, 0) | 右车门氛围灯 |
| leftHeadLight | SpotLight | 0xffffee | 3000 | (-0.8, 0.5, -1.5) | 左远光灯 |
| rightHeadLight | SpotLight | 0xffffee | 3000 | (0.8, 0.5, -1.5) | 右远光灯 |

## 夜晚特殊处理

```javascript
// 夜晚隐藏天空，用纯色背景（避免 Sky Shader 残光）
if (currentLighting.skyElevation < -10) {
    sky.visible = false;
    scene.background.copy(currentLighting.background);
} else {
    sky.visible = true;
}
```

## 太阳位置计算

```javascript
const sunVector = new THREE.Vector3();
const phi = THREE.MathUtils.degToRad(90 - currentLighting.skyElevation);
const theta = THREE.MathUtils.degToRad(currentLighting.skyAzimuth);
sunVector.setFromSphericalCoords(1, phi, theta);
skyUniforms['sunPosition'].value.copy(sunVector);
```

## 线性插值动画

```javascript
function lerp(a, b, t) {
    return a + (b - a) * t;
}

// 在 animate 循环中
currentLighting.ambient.lerpColors(fromLight.ambient, toLight.ambient, segmentProgress);
currentLighting.ambientIntensity = lerp(fromLight.ambientIntensity, toLight.ambientIntensity, segmentProgress);
currentLighting.sun.lerpColors(fromLight.sun, toLight.sun, segmentProgress);
currentLighting.sunIntensity = lerp(fromLight.sunIntensity, toLight.sunIntensity, segmentProgress);
currentLighting.sunPosition.lerpVectors(fromLight.sunPosition, toLight.sunPosition, segmentProgress);
currentLighting.skyElevation = lerp(fromLight.skyElevation, toLight.skyElevation, segmentProgress);
currentLighting.skyAzimuth = lerp(fromLight.skyAzimuth, toLight.skyAzimuth, segmentProgress);
```

## Environment.timeOfDay 映射

后端 `Environment.timeOfDay` 枚举值到光照配置的映射：

| timeOfDay | skyElevation | skyAzimuth | ambientIntensity | 说明 |
|-----------|-------------|------------|------------------|------|
| dawn | 5° | 180° | 0.3 | 黎明，太阳刚升起 |
| morning | 30° | 180° | 0.5 | 上午，阳光明亮 |
| noon | 80° | 180° | 0.7 | 正午，太阳最高 |
| afternoon | 50° | 200° | 0.6 | 下午，太阳西移 |
| evening | 10° | 250° | 0.4 | 黄昏，日落时分 |
| night | -30° | 270° | 0.15 | 夜晚，无太阳 |
| midnight | -30° | 270° | 0.1 | 深夜，最暗 |

### 前端 Composable 接口

```typescript
// useTimeOfDay.ts
export function useTimeOfDay(scene: Scene) {
  function updateTime(timeOfDay: TimeOfDay) {
    const config = TIME_LIGHTING[timeOfDay]
    // 更新 Sky Shader
    // 更新环境光
    // 更新太阳光
  }

  return { updateTime }
}
```
