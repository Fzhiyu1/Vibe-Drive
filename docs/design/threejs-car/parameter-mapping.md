# 参数映射规范

## 设计理念

**底层平台**：暴露细节给上层系统，而非前端推导。

## LightSetting 扩展字段

| 参数 | 类型 | 说明 |
|------|------|------|
| colorA | LightColor | 主色 |
| colorB | LightColor | 副色（渐变/流光用） |
| brightness | int | 亮度 0-100 |
| mode | LightMode | 模式（STATIC/BREATHING/FLOWING） |
| speed | float | 流动速度 0.0-2.0 |
| sharpness | float | 锐度 1.0-10.0 |
| transitionDuration | int | 过渡时长(ms) |
| zones | List | 分区设置 |

## 后端 → 前端映射

```
LightSetting (后端)           前端 Shader Uniform
─────────────────────────────────────────────────
colorA.hex      ──────────►  uColorA
colorB.hex      ──────────►  uColorB
brightness/100  ──────────►  uBrightness
speed           ──────────►  uSpeed
sharpness       ──────────►  uSharpness
mode            ──────────►  动画逻辑 (STATIC 时 speed=0)
transitionDuration ───────►  颜色切换 lerp 时长
zones           ──────────►  分区灯带独立控制
```

## Environment → 光照映射

```
Environment.timeOfDay  ──────►  TIME_LIGHTING 配置
Environment.weather    ──────►  天空参数调整（未来）
```

## ScentSetting → 粒子系统映射

### 香氛类型映射表

| ScentType | 颜色 | Buff 文字 | Emoji |
|-----------|------|----------|-------|
| LAVENDER | 0x9370DB | 压力↓↓ | 🪻 |
| PEPPERMINT | 0x98FB98 | 清醒↑↑ | 🌿 |
| OCEAN | 0x00CED1 | 放松↑ | 🌊 |
| FOREST | 0x228B22 | 专注↑ | 🌲 |
| CITRUS | 0xFFA500 | 活力↑ | 🍊 |
| VANILLA | 0xFFE4C4 | 幸福感↑ | 🍦 |

### 强度映射

```
ScentSetting.intensity (0-10)
    │
    ├──► 粒子密度 = intensity * 10  (0-100 个粒子)
    │
    └──► Buff 出现频率 = intensity / 10  (0.0-1.0)
```

## 数据流总览

```
后端 Tool 层
    │
    ├── LightTool.setLight() → LightSetting
    │       ↓
    │   useAmbienceLight(setting)
    │       ↓
    │   Shader Uniforms 更新
    │
    ├── ScentTool.setScent() → ScentSetting
    │       ↓
    │   useScentParticles(setting)
    │       ↓
    │   粒子系统更新
    │
    └── Environment.timeOfDay
            ↓
        useTimeOfDay(timeOfDay)
            ↓
        Sky Shader + 全局光照更新
```
