# 前端架构设计

## 技术栈

| 类别 | 技术选型 | 说明 |
|------|----------|------|
| 框架 | Vue 3 | Composition API + `<script setup>` |
| 语言 | TypeScript | 类型安全 |
| 构建 | Vite | 快速开发体验 |
| 状态管理 | Pinia | 单个 Store |
| CSS | UnoCSS | 原子化 CSS，按需生成 |
| 3D 可视化 | TresJS | Vue 生态的 Three.js 封装 |
| SSE | fetch + ReadableStream | 支持 POST 和自定义 Header |

## 设计规范

### 屏幕适配

- **目标比例**：21:9 超宽屏
- **参考分辨率**：2560 × 1080 或 1920 × 810

### 主题模式

支持浅色和深色两种主题，可根据时间或用户偏好切换。

#### 浅色主题（白色调内饰）

```css
:root {
  /* 背景层次 */
  --bg-primary: #FFFFFF;
  --bg-secondary: #F5F5F7;
  --bg-tertiary: #E8E8ED;

  /* 文字 */
  --text-primary: #1D1D1F;
  --text-secondary: #6E6E73;
  --text-muted: #AEAEB2;

  /* 强调色 */
  --accent: #007AFF;
  --accent-success: #34C759;
  --accent-warning: #FF9500;
  --accent-danger: #FF3B30;

  /* 氛围灯辉光 */
  --ambience-glow: rgba(0, 122, 255, 0.15);
}
```

#### 深色主题（夜间模式）

```css
:root.dark {
  /* 背景层次 */
  --bg-primary: #000000;
  --bg-secondary: #1C1C1E;
  --bg-tertiary: #2C2C2E;

  /* 文字 */
  --text-primary: #FFFFFF;
  --text-secondary: #AEAEB2;
  --text-muted: #636366;

  /* 强调色保持不变 */
  --accent: #0A84FF;
  --accent-success: #30D158;
  --accent-warning: #FF9F0A;
  --accent-danger: #FF453A;

  /* 氛围灯辉光 */
  --ambience-glow: rgba(10, 132, 255, 0.2);
}
```

## 页面布局

21:9 超宽屏四区域布局：

```
┌─────────────────────────────────────────────────────────────┐
│  环境信息面板  │      氛围可视化（3D）      │  音乐 + 串词  │
│    (左侧)     │         (中央)            │    (右侧)     │
│               │                           │               │
│  - GPS 标签   │    ┌─────────────────┐    │  ♪ 歌曲信息   │
│  - 天气       │    │                 │    │  ━━━━●━━━━━   │
│  - 车速       │    │   氛围灯动效    │    │               │
│  - 时段       │    │                 │    │  "串词文本    │
│  - 乘客       │    └─────────────────┘    │   打字机..."  │
│  - 情绪选择   │                           │               │
├─────────────────────────────────────────────────────────────┤
│                    Agent 思维链（底部，可折叠）              │
│  🤔 分析环境... → 🔧 调用 MusicTool → 🔧 调用 LightTool → ✅ │
└─────────────────────────────────────────────────────────────┘
```

## 项目结构

```
vibe-drive-frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── assets/                # 静态资源
│   │   └── styles/
│   │       └── theme.css      # 主题变量
│   ├── components/            # 组件
│   │   ├── layout/
│   │   │   └── AppLayout.vue  # 主布局
│   │   ├── environment/
│   │   │   ├── EnvironmentPanel.vue
│   │   │   └── MoodSelector.vue
│   │   ├── ambience/
│   │   │   └── AmbienceVisualizer.vue
│   │   ├── music/
│   │   │   └── MusicPlayer.vue
│   │   ├── narrative/
│   │   │   └── NarrativeDisplay.vue
│   │   └── agent/
│   │       └── ThinkingChain.vue
│   ├── composables/           # 组合式函数
│   │   ├── useSSE.ts          # SSE 连接管理
│   │   └── useTheme.ts        # 主题切换
│   ├── services/              # API 服务
│   │   ├── api.ts             # API 封装
│   │   └── types.ts           # API 类型定义
│   ├── stores/                # Pinia Store
│   │   └── vibeStore.ts       # 全局状态
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── uno.config.ts              # UnoCSS 配置
```

## 状态管理

单个 Pinia Store 管理所有状态：

```typescript
// stores/vibeStore.ts
export const useVibeStore = defineStore('vibe', () => {
  // 环境数据
  const environment = ref<Environment | null>(null)

  // 氛围方案
  const plan = ref<AmbiencePlan | null>(null)

  // Agent 状态
  const agentRunning = ref(false)
  const thinkingChain = ref<ThinkingStep[]>([])

  // UI 状态
  const theme = ref<'light' | 'dark'>('light')
  const demoMode = ref(false)
  const chainExpanded = ref(false)

  // Actions
  async function analyze() { ... }
  async function analyzeStream() { ... }
  function setEnvironment(env: Environment) { ... }
  function toggleTheme() { ... }

  return { ... }
})
```

## SSE 流式处理

使用 `fetch + ReadableStream` 处理 POST SSE：

```typescript
// composables/useSSE.ts
export function useSSE() {
  async function connectStream(
    url: string,
    body: object,
    handlers: {
      onToken?: (text: string) => void
      onToolStart?: (name: string, input: object) => void
      onToolEnd?: (name: string, result: string) => void
      onComplete?: (plan: AmbiencePlan) => void
      onError?: (error: Error) => void
    }
  ) {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      // 解析 SSE 事件...
    }
  }

  return { connectStream }
}
```

## 演示模式

调用后端 `EnvironmentSimulator` 生成场景数据：

```typescript
// 演示流程
async function startDemo() {
  const scenarios = ['LATE_NIGHT_RETURN', 'WEEKEND_FAMILY_TRIP', 'MORNING_COMMUTE']

  for (const scenario of scenarios) {
    // 1. 获取模拟环境
    const env = await api.getScenario(scenario)

    // 2. 触发分析
    await analyzeStream(env)

    // 3. 等待一段时间
    await sleep(10000)
  }
}
```

## 依赖清单

```json
{
  "dependencies": {
    "vue": "^3.4.x",
    "pinia": "^2.1.x",
    "@tresjs/core": "^4.x",
    "three": "^0.160.x"
  },
  "devDependencies": {
    "typescript": "^5.3.x",
    "vite": "^5.x",
    "unocss": "^0.58.x",
    "@unocss/preset-icons": "^0.58.x",
    "vue-tsc": "^1.8.x"
  }
}
```
