# Three.js 车内环境设计文档

## 概述

本文档描述 Vibe Drive 项目中 Three.js 3D 车内环境的设计方案，基于探索阶段的讨论结论。

## 技术选型

**方案**：原生 Three.js + Vue Composable 封装

| 对比项 | 原生 Three.js | TresJS |
|--------|--------------|--------|
| AI 生成可靠性 | ✅ 高（训练数据多） | ⚠️ 一般（较新） |
| 性能 | ✅ 最优 | 有抽象层开销 |
| 灵活性 | ✅ 完全可控 | 受框架限制 |

**代码结构**：
```
composables/
├── useThreeScene.ts      # 场景初始化/销毁
├── useAmbienceLight.ts   # 氛围灯控制
├── useTimeOfDay.ts       # 时间光照控制
└── useScentParticles.ts  # 香氛粒子系统
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [场景结构](./scene-structure.md) | 车内几何体、材质、布局 |
| [光照系统](./lighting-system.md) | 时间光照、天空、全局光 |
| [氛围灯系统](./ambience-light.md) | 流光效果、Shader、参数 |
| [可视化系统](./visualization.md) | 香氛粒子、车机屏幕 |
| [参数映射](./parameter-mapping.md) | 后端 → 前端数据映射 |

## 设计原则

1. **固定视角** - 两座椅中间，沉浸感强
2. **时间联动** - 光照随 timeOfDay 自动变化
3. **参数暴露** - 底层平台，细节可控
4. **渐进增强** - 基础功能先行，效果逐步升级

## 工具接口融合

### 数据流

```
后端 Tool 层
    │
    ├── LightTool.setLight()
    │       ↓
    │   LightSetting
    │       ↓
    │   useAmbienceLight(setting)
    │
    ├── ScentTool.setScent()
    │       ↓
    │   ScentSetting
    │       ↓
    │   useScentParticles(setting)
    │
    └── Environment.timeOfDay
            ↓
        useTimeOfDay(timeOfDay)
            ↓
        Three.js 场景更新
```

### 核心映射

| 后端数据 | 前端 Composable | 3D 效果 |
|---------|----------------|--------|
| LightSetting | useAmbienceLight | 流光 Shader + RectAreaLight |
| ScentSetting | useScentParticles | 粒子系统 + Buff 文字 |
| Environment.timeOfDay | useTimeOfDay | Sky Shader + 全局光照 |

详见 [参数映射](./parameter-mapping.md)。
