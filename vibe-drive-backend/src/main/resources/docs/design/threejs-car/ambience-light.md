# 氛围灯系统设计

## 概述

采用 **ShaderMaterial（视觉效果）+ RectAreaLight（真实发光）** 双层方案。

## 灯带几何体

| 灯带 | 几何体 | 尺寸 | 位置 |
|------|--------|------|------|
| 仪表台 | BoxGeometry | 2.2 × 0.02 × 0.02 | (0, 0.72, -0.49) |
| 左车门 | BoxGeometry | 0.02 × 0.02 × 2.0 | (-1.0, 0.8, 0.5) |
| 右车门 | BoxGeometry | 0.02 × 0.02 × 2.0 | (1.0, 0.8, 0.5) |

## 流光 Shader

### Vertex Shader

```glsl
varying vec2 vUv;
void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
```

### Fragment Shader

```glsl
uniform float uTime;
uniform vec3 uColorA;
uniform vec3 uColorB;
uniform float uSpeed;
uniform float uSharpness;
varying vec2 vUv;

void main() {
    float coord = vUv.x;
    float phase = uTime * uSpeed;
    float wave = sin(coord * 6.28 + phase);
    wave = (wave + 1.0) * 0.5;
    wave = pow(wave, uSharpness);
    vec3 finalColor = mix(uColorA, uColorB, wave);
    gl_FragColor = vec4(finalColor, 1.0);
}
```

## Uniforms 配置

```javascript
const ambienceUniforms = {
    uTime: { value: 0 },
    uColorA: { value: new THREE.Color(0x44ddff) },
    uColorB: { value: new THREE.Color(0x00aaff) },
    uSpeed: { value: 0.5 },
    uSharpness: { value: 1.0 }
};
```

## ShaderMaterial 创建

```javascript
const stripMat = new THREE.ShaderMaterial({
    vertexShader: lightStripVertexShader,
    fragmentShader: lightStripFragmentShader,
    uniforms: ambienceUniforms,
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false
});
```

## RectAreaLight 发光

```javascript
// 仪表台灯带光源
const dashLight = new THREE.RectAreaLight(0x00aaff, 20, 2.2, 0.1);
dashLight.position.set(0, 0.72, -0.48);
dashLight.lookAt(0, 0.72, 0);

// 左车门灯带光源
const lDoorLight = new THREE.RectAreaLight(0x00aaff, 10, 0.1, 1.5);
lDoorLight.position.set(-0.99, 0.8, 0);
lDoorLight.lookAt(0, 0.8, 0);

// 右车门灯带光源
const rDoorLight = new THREE.RectAreaLight(0x00aaff, 10, 0.1, 1.5);
rDoorLight.position.set(0.99, 0.8, 0);
rDoorLight.lookAt(0, 0.8, 0);
```

## 屏幕发光

```javascript
const screenLight = new THREE.RectAreaLight(0xffffff, 15, 0.8, 0.3);
screenLight.position.set(0, 1.05, -0.48);
screenLight.rotation.x = -0.3;
screenLight.lookAt(0, 1.05, 1);  // 朝向前方，避免照到背后
```

## 动画更新

```javascript
// 在 animate 循环中
ambienceUniforms.uTime.value = clock.getElapsedTime();
```

## LightSetting → Shader 映射

后端 `LightTool.setLight()` 返回的 `LightSetting` 到 Shader Uniforms 的映射：

| LightSetting 字段 | 转换公式 | Shader Uniform |
|------------------|---------|----------------|
| color.hex | hexToVec3() | uColorA |
| colorB.hex | hexToVec3() | uColorB |
| brightness | / 100 | uBrightness |
| speed | / 100 | uSpeed (0.0-2.0) |
| sharpness | / 10 | uSharpness (1.0-10.0) |

### 前端 Composable 接口

```typescript
// useAmbienceLight.ts
export function useAmbienceLight(scene: Scene) {
  function updateLight(setting: LightSetting) {
    // 更新 Shader Uniforms
    ambienceUniforms.uColorA.value.set(setting.color?.hex)
    ambienceUniforms.uColorB.value.set(setting.colorB?.hex)
    ambienceUniforms.uSpeed.value = (setting.speed ?? 50) / 100
    ambienceUniforms.uSharpness.value = (setting.sharpness ?? 50) / 10

    // 更新 RectAreaLight 颜色
    dashLight.color.set(setting.color?.hex)
  }

  return { updateLight }
}
```
