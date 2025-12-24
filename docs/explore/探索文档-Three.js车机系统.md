# 探索文档：Three.js 车机系统

## 探索目标

研究如何使用 Three.js 在车机系统中实现 3D 可视化效果。

## 背景

当前 Vibe Drive 项目使用 CSS 动效实现氛围可视化，探索使用 Three.js 实现更丰富的 3D 效果。

## 待探索内容

- [x] Three.js 基础集成方案
- [ ] Vue 3 + Three.js 最佳实践
- [x] 车内 3D 场景建模
- [x] 氛围灯光效实现
- [ ] 性能优化策略

## 探索记录

### 话题1：技术选型

**讨论日期**：2025-12-24

**方案对比**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 原生 Three.js | 文档完善、社区庞大、无抽象层、性能最优 | 需手动管理生命周期、与 Vue 响应式集成需额外代码 |
| TresJS | Vue 生态原生、声明式语法、自动响应式 | 较新、训练数据少、API 可能变化 |

**AI 生成友好度考量**：
- 原生 Three.js 训练数据量大，代码模式固定，生成准确率高
- TresJS 2023 年才流行，AI 生成可能出现配置问题

**结论**：采用 **原生 Three.js + Vue Composable 封装**

```
composables/
├── useThreeScene.ts      # 场景初始化/销毁
└── useAmbienceLight.ts   # 氛围灯控制
```

**理由**：
- 底层用原生 Three.js（AI 生成可靠）
- 上层用 Vue composable 封装（保持响应式）
- 代码可控，出问题容易排查

---

### 话题2：AI 生成 3D 能力测试

**讨论日期**：2025-12-24

**测试方法**：
- 设计 8 个难度递增的测试用例
- 分别用 Claude 和 Gemini 生成代码
- 对比验收结果

**测试用例**：见 `experiments/threejs-car/requirements/`

---

### 话题3：Vue 3 + Three.js 最佳实践

**讨论日期**：2025-12-24

#### 3.1 视角控制

**问题**：车机 3D 场景应该用什么视角？

**方案对比**：

| 方案 | 描述 | 优缺点 |
|------|------|--------|
| 完全固定 | 锁定视角，不可旋转 | ✅ 简洁、沉浸感强 |
| 预设切换 | 2-3个预设视角 | 需要 UI 支持 |
| 有限旋转 | 限制旋转范围 | 增加复杂度 |
| 自动漫游 | 相机自动移动 | 工作量大 |

**结论**：采用 **完全固定视角**

**视角位置**：两座椅中间（非驾驶位）

**理由**：
- 驾驶位会被方向盘挡住中控屏
- 中间位置可以看到完整仪表台
- 两侧氛围灯带对称可见
- 更适合"展示"而非"驾驶"

**参考代码**：
```javascript
// 移除 OrbitControls，使用固定视角
camera.position.set(0, 1.1, 0.5);   // X=0 中间, Y=头部高度, Z=座椅后方
camera.lookAt(0, 0.9, -1.5);        // 看向仪表台中央
```

#### 3.2 车机屏幕显示（音乐播放器）

**问题**：如何在 3D 车机屏幕上显示音乐播放信息？

**方案对比**：

| 方案 | 初期难度 | 升级空间 | 视觉效果 |
|------|----------|----------|----------|
| HTML 覆盖层 | 最简单 | 升级需重写 | 割裂感 |
| **CanvasTexture** | 稍复杂 | 天然可扩展 | 沉浸感强 |

**结论**：采用 **CanvasTexture** 方案

**基础功能**：
- 播放/暂停图标
- 歌曲名（歌手 - 标题）
- 进度条 + 时间显示

**升级路径**：
```
阶段1：静态文字 → 阶段2：进度条动画 → 阶段3：频谱可视化 → 阶段4：封面图片
```

**参考代码**：
```javascript
function drawPlayer(ctx, song, progress) {
  ctx.fillStyle = '#1a1a1a';
  ctx.fillRect(0, 0, 400, 120);

  ctx.fillStyle = '#fff';
  ctx.fillText(`▶  ${song.artist} - ${song.title}`, 20, 40);

  ctx.fillStyle = '#333';
  ctx.fillRect(20, 60, 360, 8);

  ctx.fillStyle = '#00aaff';
  ctx.fillRect(20, 60, 360 * progress, 8);
}
```

#### 3.3 流光灯效果

**问题**：如何实现氛围灯带的流光效果？

**Gemini 实现方案**：自定义 ShaderMaterial

```glsl
float wave = sin(coord * 6.28 + uTime * uSpeed);  // 正弦波流动
wave = pow(wave, uSharpness);                      // 锐化控制
vec3 color = mix(colorA, colorB, wave);            // 颜色混合
```

**评价**：7/10
- ✅ 效果流畅、可配置、模式适配
- ❌ Shader 门槛高、调试困难

**结论**：**流光灯效果由 Gemini 负责实现**

**理由**：Gemini 在视觉效果和 Shader 方面表现更好

#### 3.4 流光灯参数设计

**问题**：LightTool 返回的参数如何映射到 Shader？

**项目定位**：底层平台（暴露细节给上层系统）

**LightSetting 扩展字段**：

| 参数 | 类型 | 说明 |
|------|------|------|
| colorA | LightColor | 主色 |
| colorB | LightColor | 副色（渐变/流光用） |
| brightness | int | 亮度 0-100 |
| mode | LightMode | 模式 |
| speed | float | 流动速度 0.0-2.0 |
| sharpness | float | 锐度 1.0-10.0 |
| transitionDuration | int | 过渡时长 |
| zones | List | 分区设置 |

**后端 → 前端映射**：

```
LightSetting (后端)           前端处理
─────────────────────────────────────────────────
colorA.hex      ──────────►  uColorA
colorB.hex      ──────────►  uColorB
brightness/100  ──────────►  uBrightness (透明度/强度)
speed           ──────────►  uSpeed
sharpness       ──────────►  uSharpness
mode            ──────────►  动画逻辑判断 (STATIC 时 speed=0)
transitionDuration ───────►  颜色切换的 lerp 时长
zones           ──────────►  分区灯带独立控制
```

#### 3.5 香氛系统可视化

**问题**：香氛是看不见的，如何可视化？

**方案对比**：

| 方案 | 难度 | 效果 |
|------|------|------|
| 图标+文字 | ⭐ | 简单直接 |
| 粒子飘散 | ⭐⭐⭐ | 有动感 |
| 雾气效果 | ⭐⭐⭐⭐ | 需要体积渲染 |

**结论**：采用 **粒子 + Buff 文字** 混合方案

**设计理念**：游戏化，香氛带来 Buff 效果

**Buff 映射表**：

| 香氛 | Buff 效果 |
|------|----------|
| 🪻 薰衣草 | 压力 ↓↓ |
| 🌿 薄荷 | 清醒 ↑↑ |
| 🍊 柑橘 | 活力 ↑ |
| 🌊 海洋 | 放松 ↑ |
| 🌲 森林 | 专注 ↑ |
| 🍦 香草 | 幸福感 ↑ |

**视觉效果**：
- 粒子从出风口飘出
- 粒子中夹杂 Buff 文字（如 "压力↓"）
- intensity 控制粒子密度和文字出现频率

**技术实现**：
- 粒子：`THREE.Points`
- 文字：`CSS2DRenderer` 或 `Sprite`

**香氛出口硬件**：采用 **中控内置式**

| 类型 | 制作难度 | 直观性 |
|------|----------|--------|
| 出风口挂件 | 需要栅格+挂件 | 一般 |
| **中控内置** | 一个发光圆孔 | ✅ 直观 |

**3D 实现**：
```javascript
// 香氛出口（发光圆环）
const scentRing = new THREE.Mesh(
  new THREE.RingGeometry(0.02, 0.03, 32),
  new THREE.MeshBasicMaterial({ color: 0x00ff88 })
);
scentRing.position.set(0.3, 0.85, -2.2); // 中控台位置
```

#### 3.6 按摩系统可视化

**结论**：**不在 3D 场景中可视化**

**理由**：现有右侧状态栏已显示按摩状态，无需重复

#### 3.7 车窗外景

**问题**：如何显示车窗外的环境？

**方案对比**：

| 方案 | 时间变化 | 天气变化 |
|------|----------|----------|
| 静态贴图 | ❌ | ❌ |
| 多套贴图切换 | ✅ 切换 | ✅ 切换 |
| **Three.js Sky** | ✅ 太阳位置 | ⚠️ 有限 |

**结论**：采用 **Three.js Sky Shader**

**理由**：可根据 `Environment.timeOfDay` 动态调整太阳位置

**参考代码**：
```javascript
import { Sky } from 'three/addons/objects/Sky.js';

const sky = new Sky();
const sun = new THREE.Vector3();

function updateSky(timeOfDay) {
  const elevation = {
    dawn: 5, morning: 30, noon: 90, evening: 15, night: -10
  }[timeOfDay];

  sun.setFromSphericalCoords(1, Math.PI/2 - elevation * Math.PI/180, 0);
  sky.material.uniforms.sunPosition.value.copy(sun);
}
```

#### 3.8 时间光照实验

**实验目的**：验证全局光照 + 天空盒能否随时间自动变化

**实验文件**：`experiments/time-lighting/base.html`

**实现功能**：
- 5时段光照配置（黎明/上午/正午/黄昏/夜晚）
- 30秒线性渐进循环
- Three.js Sky 动态天空
- 氛围灯 RectAreaLight 发光
- 远光灯照亮道路

**关键技术细节**：

1. **Sky Shader 参数**（避免太阳刺眼）：
```javascript
skyUniforms['turbidity'].value = 1;
skyUniforms['rayleigh'].value = 0.5;
skyUniforms['mieCoefficient'].value = 0.003;
skyUniforms['mieDirectionalG'].value = 0.65;
```

2. **Tone Mapping**（控制曝光）：
```javascript
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 0.5;
```

3. **RectAreaLight**（氛围灯均匀发光）：
```javascript
import { RectAreaLightUniformsLib } from 'three/addons/lights/RectAreaLightUniformsLib.js';
RectAreaLightUniformsLib.init();

const dashLight = new THREE.RectAreaLight(0x00aaff, 20, 2.2, 0.1);
```

4. **夜晚处理**（隐藏天空用纯色背景）：
```javascript
if (skyElevation < -10) {
    sky.visible = false;
    scene.background.copy(backgroundColor);
}
```

5. **远光灯**：
```javascript
const headLight = new THREE.SpotLight(0xffffee, 3000, 100, Math.PI/8, 0.3);
```

**最终参数**：

| 参数 | 值 |
|------|-----|
| 相机位置 | (0, 1.3, 0.6) |
| 氛围灯强度 | 10-20 |
| 远光灯强度 | 3000 |
| 夜晚环境光 | 0.15 |

**实验结论**：
- ✅ 全局光照 + 天空盒方案可行
- ✅ 时间变化时，车内亮度、氛围灯显眼度自动联动
- ✅ RectAreaLight 比 PointLight 更适合灯带发光
- ⚠️ Sky Shader 夜晚有残光，需隐藏处理
- ⚠️ 参数调优需要反复实验

---

## 结论

### AI 能力对比

| 维度 | Claude | Gemini |
|------|--------|--------|
| 视觉效果 | 一般（方形粒子） | **更好**（圆形粒子、Shader） |
| 代码逻辑 | **更清晰**（配置化、模块化） | 较复杂（Shader 内联） |
| 创意性 | 中规中矩 | **更有想法**（雾效、A柱） |
| 可维护性 | **更好** | 一般 |

### 关键发现

1. **需求文档很重要**
   - 详细需求 → 结果相似
   - 开放需求 → 能区分 AI 能力
   - 缺少的要求（如光照）→ 两个都会忽略

2. **AI 生成 3D 的现状**
   - 简单几何体：可靠
   - 复杂模型：堪堪可用
   - 需要人工调整：位置、光照、穿模

3. **修复难度降低**
   - 代码结构清晰，定位问题快
   - 参数调整直观
   - 比"前世代"模型好改
