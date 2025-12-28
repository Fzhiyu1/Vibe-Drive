import * as THREE from 'three'

/**
 * 告示牌类型
 */
type SignType = 'mileage' | 'speed_limit' | 'exit' | 'service'

interface SignConfig {
  type: SignType
  text: string
  subText?: string
}

interface RoadSign {
  mesh: THREE.Mesh
  pole: THREE.Mesh
  config: SignConfig
  side: 'left' | 'right'
  visible: boolean
  cooldown: number  // 冷却时间（秒）
}

// 告示牌样式配置
const SIGN_CONFIGS: SignConfig[] = [
  { type: 'mileage', text: 'G60', subText: '杭州 120km' },
  { type: 'speed_limit', text: '120' },
  { type: 'exit', text: '嘉兴出口', subText: '2km' },
]

// 常量配置
const SIGN_START_Z = -80    // 告示牌起始位置
const SIGN_END_Z = 5        // 告示牌结束位置
const SIGN_SPACING = 35     // 初始间距
const SIGN_X_OFFSET = 3     // 道路两侧偏移
const SPEED_FACTOR = 0.012  // 速度转换因子
const COOLDOWN_MIN = 20     // 最小冷却时间（秒）
const COOLDOWN_MAX = 40     // 最大冷却时间（秒）

// 树的配置
const TREE_COUNT = 12       // 树的数量
const TREE_START_Z = -100   // 树起始位置
const TREE_SPACING = 8      // 树间距
const TREE_X_MIN = 4        // 树最小X偏移
const TREE_X_MAX = 8        // 树最大X偏移

// 场景物体接口
interface SceneObject {
  group: THREE.Group
  side: 'left' | 'right'
  baseX: number
}

/**
 * 创建告示牌纹理
 */
function createSignTexture(config: SignConfig): THREE.CanvasTexture {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')!

  canvas.width = 256
  canvas.height = 192

  // 根据类型绘制不同样式
  switch (config.type) {
    case 'speed_limit':
      drawSpeedLimitSign(ctx, config.text)
      break
    case 'mileage':
      drawMileageSign(ctx, config.text, config.subText)
      break
    case 'exit':
      drawExitSign(ctx, config.text, config.subText)
      break
    case 'service':
      drawServiceSign(ctx, config.text, config.subText)
      break
  }

  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  return texture
}

/**
 * 绘制限速牌（圆形红边白底）
 */
function drawSpeedLimitSign(ctx: CanvasRenderingContext2D, speed: string) {
  const cx = 128, cy = 96, r = 70

  // 白色圆底
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.fillStyle = '#ffffff'
  ctx.fill()

  // 红色边框
  ctx.lineWidth = 10
  ctx.strokeStyle = '#cc0000'
  ctx.stroke()

  // 速度数字
  ctx.fillStyle = '#000000'
  ctx.font = 'bold 48px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(speed, cx, cy)
}

/**
 * 绘制里程牌（绿底白字）
 */
function drawMileageSign(ctx: CanvasRenderingContext2D, road: string, distance?: string) {
  // 绿色背景
  ctx.fillStyle = '#006633'
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.fill()

  // 白色边框
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 3
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.stroke()

  // 道路编号
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 36px Arial'
  ctx.textAlign = 'center'
  ctx.fillText(road, 128, 70)

  // 距离信息
  if (distance) {
    ctx.font = '24px Arial'
    ctx.fillText(distance, 128, 120)
  }
}

/**
 * 绘制出口牌（绿底白字带箭头）
 */
function drawExitSign(ctx: CanvasRenderingContext2D, name: string, distance?: string) {
  // 绿色背景
  ctx.fillStyle = '#006633'
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.fill()

  // 白色边框
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 3
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.stroke()

  // 出口名称
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 28px Arial'
  ctx.textAlign = 'center'
  ctx.fillText(name, 128, 70)

  // 距离
  if (distance) {
    ctx.font = '22px Arial'
    ctx.fillText(distance, 128, 110)
  }

  // 箭头
  ctx.beginPath()
  ctx.moveTo(128, 140)
  ctx.lineTo(118, 155)
  ctx.lineTo(138, 155)
  ctx.closePath()
  ctx.fill()
}

/**
 * 绘制服务区牌（蓝底白字）
 */
function drawServiceSign(ctx: CanvasRenderingContext2D, name: string, distance?: string) {
  // 蓝色背景
  ctx.fillStyle = '#0055aa'
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.fill()

  // 白色边框
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 3
  roundRect(ctx, 20, 20, 216, 152, 8)
  ctx.stroke()

  // 服务区名称
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 28px Arial'
  ctx.textAlign = 'center'
  ctx.fillText(name, 128, 80)

  // 距离
  if (distance) {
    ctx.font = '22px Arial'
    ctx.fillText(distance, 128, 120)
  }
}

/**
 * 绘制圆角矩形
 */
function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number, y: number,
  w: number, h: number,
  r: number
) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

/**
 * 速度效果 composable
 */
export function useSpeedEffect(scene: THREE.Scene) {
  const signs: RoadSign[] = []
  const trees: SceneObject[] = []
  let currentSpeed = 0

  // 材质
  const poleMat = new THREE.MeshStandardMaterial({
    color: 0x888888,
    roughness: 0.6,
    metalness: 0.3
  })

  const trunkMat = new THREE.MeshStandardMaterial({
    color: 0x4a3728,
    roughness: 0.9
  })

  const leavesMat = new THREE.MeshStandardMaterial({
    color: 0x228b22,
    roughness: 0.8
  })

  /**
   * 创建单个告示牌
   */
  function createSign(config: SignConfig, zPos: number, side: 'left' | 'right'): RoadSign {
    // 牌面
    const texture = createSignTexture(config)
    const signMat = new THREE.MeshBasicMaterial({
      map: texture,
      transparent: true,
      side: THREE.DoubleSide
    })

    const signGeo = new THREE.PlaneGeometry(1.2, 0.9)
    const mesh = new THREE.Mesh(signGeo, signMat)

    // 位置
    const xPos = side === 'left' ? -SIGN_X_OFFSET : SIGN_X_OFFSET
    mesh.position.set(xPos, 2.5, zPos)
    mesh.rotation.y = side === 'left' ? Math.PI / 8 : -Math.PI / 8

    // 杆子
    const poleGeo = new THREE.CylinderGeometry(0.03, 0.03, 2.5, 8)
    const pole = new THREE.Mesh(poleGeo, poleMat)
    pole.position.set(xPos, 1.25, zPos)

    scene.add(mesh)
    scene.add(pole)

    return { mesh, pole, config, side, visible: true, cooldown: 0 }
  }

  /**
   * 创建单棵树
   */
  function createTree(zPos: number, side: 'left' | 'right'): SceneObject {
    const group = new THREE.Group()

    // 随机大小
    const scale = 0.8 + Math.random() * 0.6

    // 树干
    const trunkGeo = new THREE.CylinderGeometry(0.08 * scale, 0.12 * scale, 1.5 * scale, 6)
    const trunk = new THREE.Mesh(trunkGeo, trunkMat)
    trunk.position.y = 0.75 * scale
    group.add(trunk)

    // 树冠（圆锥）
    const leavesGeo = new THREE.ConeGeometry(0.6 * scale, 1.8 * scale, 6)
    const leaves = new THREE.Mesh(leavesGeo, leavesMat)
    leaves.position.y = 2.0 * scale
    group.add(leaves)

    // 位置
    const xOffset = TREE_X_MIN + Math.random() * (TREE_X_MAX - TREE_X_MIN)
    const baseX = side === 'left' ? -xOffset : xOffset
    group.position.set(baseX, 0, zPos)

    scene.add(group)
    return { group, side, baseX }
  }

  /**
   * 初始化告示牌
   */
  function init() {
    // 在道路两侧交替放置告示牌
    SIGN_CONFIGS.forEach((config, index) => {
      const zPos = SIGN_START_Z + index * SIGN_SPACING
      const side = index % 2 === 0 ? 'right' : 'left'
      signs.push(createSign(config, zPos, side))
    })

    // 在道路两侧放置树
    for (let i = 0; i < TREE_COUNT; i++) {
      const zPos = TREE_START_Z + i * TREE_SPACING
      const side = i % 2 === 0 ? 'left' : 'right'
      trees.push(createTree(zPos, side))
    }
  }

  /**
   * 更新速度
   */
  function updateSpeed(speed: number) {
    currentSpeed = speed
  }

  /**
   * 动画帧更新
   */
  function tick(delta: number) {
    // 计算移动距离
    const moveDistance = currentSpeed * SPEED_FACTOR * delta * 60

    // 更新告示牌
    signs.forEach((sign) => {
      if (!sign.visible) {
        sign.cooldown -= delta
        if (sign.cooldown <= 0) {
          respawnSign(sign)
        }
        return
      }

      if (currentSpeed <= 0) return

      sign.mesh.position.z += moveDistance
      sign.pole.position.z += moveDistance

      if (sign.mesh.position.z > SIGN_END_Z) {
        hideSign(sign)
        sign.cooldown = COOLDOWN_MIN + Math.random() * (COOLDOWN_MAX - COOLDOWN_MIN)
        sign.visible = false
      }
    })

    // 更新树
    if (currentSpeed > 0) {
      trees.forEach((tree) => {
        tree.group.position.z += moveDistance

        // 循环到后方
        if (tree.group.position.z > SIGN_END_Z) {
          // 找到最远的树
          let minZ = 0
          trees.forEach(t => {
            if (t.group.position.z < minZ) minZ = t.group.position.z
          })
          tree.group.position.z = minZ - TREE_SPACING

          // 随机新位置
          const newSide = Math.random() > 0.5 ? 'left' : 'right'
          const xOffset = TREE_X_MIN + Math.random() * (TREE_X_MAX - TREE_X_MIN)
          tree.group.position.x = newSide === 'left' ? -xOffset : xOffset
        }
      })
    }
  }

  /**
   * 隐藏告示牌
   */
  function hideSign(sign: RoadSign) {
    sign.mesh.visible = false
    sign.pole.visible = false
  }

  /**
   * 重新显示告示牌
   */
  function respawnSign(sign: RoadSign) {
    // 随机选择一侧
    const newSide = Math.random() > 0.5 ? 'left' : 'right'
    const newX = newSide === 'left' ? -SIGN_X_OFFSET : SIGN_X_OFFSET

    sign.mesh.position.set(newX, 2.5, SIGN_START_Z)
    sign.mesh.rotation.y = newSide === 'left' ? Math.PI / 8 : -Math.PI / 8
    sign.pole.position.set(newX, 1.25, SIGN_START_Z)
    sign.side = newSide

    sign.mesh.visible = true
    sign.pole.visible = true
    sign.visible = true
  }

  /**
   * 清理资源
   */
  function dispose() {
    signs.forEach(sign => {
      scene.remove(sign.mesh)
      scene.remove(sign.pole)
      sign.mesh.geometry.dispose()
      ;(sign.mesh.material as THREE.MeshBasicMaterial).map?.dispose()
      ;(sign.mesh.material as THREE.MeshBasicMaterial).dispose()
      sign.pole.geometry.dispose()
    })
    signs.length = 0

    trees.forEach(tree => {
      scene.remove(tree.group)
      tree.group.traverse((obj) => {
        if (obj instanceof THREE.Mesh) {
          obj.geometry.dispose()
        }
      })
    })
    trees.length = 0

    poleMat.dispose()
    trunkMat.dispose()
    leavesMat.dispose()
  }

  // 初始化
  init()

  return {
    updateSpeed,
    tick,
    dispose
  }
}
