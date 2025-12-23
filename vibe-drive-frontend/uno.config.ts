import {
  defineConfig,
  presetUno,
  presetIcons,
  presetAttributify,
} from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.2,
      cdn: 'https://esm.sh/',
    }),
  ],
  theme: {
    colors: {
      // 浅色主题
      primary: '#FFFFFF',
      secondary: '#F5F5F7',
      tertiary: '#E8E8ED',
      accent: '#007AFF',
      success: '#34C759',
      warning: '#FF9500',
      danger: '#FF3B30',
    },
  },
  shortcuts: {
    'btn': 'px-4 py-2 rounded-lg cursor-pointer transition-all duration-200',
    'btn-primary': 'btn bg-accent text-white hover:opacity-90',
    'card': 'bg-secondary rounded-xl p-4',
  },
})
