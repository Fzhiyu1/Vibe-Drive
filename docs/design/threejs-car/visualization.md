# 可视化系统设计

## 车机屏幕（音乐播放器）

**方案**：CanvasTexture

**基础功能**：
- 播放/暂停图标
- 歌曲名（歌手 - 标题）
- 进度条 + 时间显示

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

**升级路径**：
```
阶段1：静态文字 → 阶段2：进度条动画 → 阶段3：频谱可视化 → 阶段4：封面图片
```

## 香氛可视化

**方案**：粒子 + Buff 文字

**Buff 映射表**：

| 香氛 | Buff 效果 |
|------|----------|
| 🪻 薰衣草 | 压力 ↓↓ |
| 🌿 薄荷 | 清醒 ↑↑ |
| 🍊 柑橘 | 活力 ↑ |
| 🌊 海洋 | 放松 ↑ |
| 🌲 森林 | 专注 ↑ |
| 🍦 香草 | 幸福感 ↑ |

**香氛出口**：中控内置式（发光圆环）

```javascript
const scentRing = new THREE.Mesh(
  new THREE.RingGeometry(0.02, 0.03, 32),
  new THREE.MeshBasicMaterial({ color: 0x00ff88 })
);
scentRing.position.set(0.3, 0.85, -2.2);
```

## ScentSetting → 粒子系统映射

后端 `ScentTool.setScent()` 返回的 `ScentSetting` 到粒子系统的映射：

### 香氛类型配置

```typescript
const SCENT_PARTICLES = {
  LAVENDER:   { color: 0x9370DB, buff: '压力↓↓', emoji: '🪻' },
  PEPPERMINT: { color: 0x98FB98, buff: '清醒↑↑', emoji: '🌿' },
  OCEAN:      { color: 0x00CED1, buff: '放松↑',  emoji: '🌊' },
  FOREST:     { color: 0x228B22, buff: '专注↑',  emoji: '🌲' },
  CITRUS:     { color: 0xFFA500, buff: '活力↑',  emoji: '🍊' },
  VANILLA:    { color: 0xFFE4C4, buff: '幸福感↑', emoji: '🍦' }
}
```

### 强度映射

| ScentSetting 字段 | 转换公式 | 粒子参数 |
|------------------|---------|---------|
| type | SCENT_PARTICLES[type] | 粒子颜色、Buff文字 |
| intensity | × 10 | 粒子数量 (0-100) |
| intensity | / 10 | Buff出现频率 (0.0-1.0) |

### 前端 Composable 接口

```typescript
// useScentParticles.ts
export function useScentParticles(scene: Scene) {
  function updateScent(setting: ScentSetting) {
    const config = SCENT_PARTICLES[setting.type]
    // 更新粒子颜色
    // 更新粒子数量
    // 更新 Buff 文字
  }

  return { updateScent }
}
```
