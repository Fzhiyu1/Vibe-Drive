import * as THREE from 'three'
import type { CarInteriorMeshes } from '@/types/three'

// 材质定义
const darkMat = new THREE.MeshStandardMaterial({
  color: 0x111111,
  roughness: 0.7,
  metalness: 0.2
})

const seatMat = new THREE.MeshStandardMaterial({
  color: 0x222222,
  roughness: 0.9
})

const chassisMat = new THREE.MeshStandardMaterial({
  color: 0x111111,
  roughness: 0.9
})

const roadMat = new THREE.MeshStandardMaterial({
  color: 0x222222,
  roughness: 0.8
})

const lineMat = new THREE.MeshBasicMaterial({ color: 0xffff00 })

// 创建座椅
function createSeat(x: number): THREE.Group {
  const group = new THREE.Group()

  // 座垫
  group.add(new THREE.Mesh(
    new THREE.BoxGeometry(0.55, 0.15, 0.6),
    seatMat
  ))

  // 靠背
  const back = new THREE.Mesh(
    new THREE.BoxGeometry(0.55, 0.7, 0.15),
    seatMat
  )
  back.position.set(0, 0.4, 0.25)
  back.rotation.x = -0.1
  group.add(back)

  // 头枕
  const head = new THREE.Mesh(
    new THREE.BoxGeometry(0.3, 0.2, 0.1),
    seatMat
  )
  head.position.set(0, 0.85, 0.2)
  group.add(head)

  group.position.set(x, 0.3, 0.2)
  return group
}

// 创建方向盘
function createSteeringWheel(): THREE.Group {
  const group = new THREE.Group()

  // 方向盘圆环
  group.add(new THREE.Mesh(
    new THREE.TorusGeometry(0.18, 0.02, 16, 50),
    darkMat
  ))

  // 横杆
  group.add(new THREE.Mesh(
    new THREE.BoxGeometry(0.34, 0.04, 0.02),
    darkMat
  ))

  // 转向柱
  const column = new THREE.Mesh(
    new THREE.CylinderGeometry(0.025, 0.03, 0.4, 16),
    darkMat
  )
  column.rotation.x = Math.PI / 2
  column.position.z = -0.2
  group.add(column)

  group.position.set(-0.45, 0.85, -0.2)
  group.rotation.x = -0.3
  return group
}

export function useCarInterior(scene: THREE.Scene): CarInteriorMeshes {
  // 地板
  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(10, 10),
    chassisMat
  )
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)

  // 底盘
  const chassis = new THREE.Mesh(
    new THREE.BoxGeometry(2.5, 0.1, 4),
    chassisMat
  )
  chassis.position.set(0, 0.05, -0.5)
  scene.add(chassis)

  // 道路
  const road = new THREE.Mesh(
    new THREE.PlaneGeometry(4, 200),
    roadMat
  )
  road.rotation.x = -Math.PI / 2
  road.position.set(0, 0.01, -100)
  scene.add(road)

  // 道路中线
  const roadLine = new THREE.Mesh(
    new THREE.PlaneGeometry(0.1, 200),
    lineMat
  )
  roadLine.rotation.x = -Math.PI / 2
  roadLine.position.set(0, 0.02, -100)
  scene.add(roadLine)

  // 仪表台
  const dashboard = new THREE.Mesh(
    new THREE.BoxGeometry(2.2, 0.6, 1.0),
    darkMat
  )
  dashboard.position.set(0, 0.7, -1.0)
  scene.add(dashboard)

  // 中控屏幕
  const screenCanvas = document.createElement('canvas')
  screenCanvas.width = 400
  screenCanvas.height = 150
  const screenTexture = new THREE.CanvasTexture(screenCanvas)
  const screenMat = new THREE.MeshBasicMaterial({ map: screenTexture })

  const screen = new THREE.Mesh(
    new THREE.PlaneGeometry(0.8, 0.3),
    screenMat
  )
  screen.position.set(0, 1.05, -0.49)
  screen.rotation.x = -0.3
  scene.add(screen)

  // 灯带 - 仪表台（使用 BoxGeometry 有厚度，各角度可见）
  const dashboardStrip = new THREE.Mesh(
    new THREE.BoxGeometry(2.2, 0.02, 0.02),
    new THREE.MeshBasicMaterial({ color: 0x00aaff })
  )
  dashboardStrip.position.set(0, 0.72, -0.49)
  scene.add(dashboardStrip)

  // 灯带 - 左车门（旋转90度让UV.x沿灯带方向）
  const leftDoorStrip = new THREE.Mesh(
    new THREE.BoxGeometry(2.0, 0.02, 0.02),
    new THREE.MeshBasicMaterial({ color: 0x00aaff })
  )
  leftDoorStrip.position.set(-1.0, 0.8, 0.5)
  leftDoorStrip.rotation.y = Math.PI / 2
  scene.add(leftDoorStrip)

  // 灯带 - 右车门
  const rightDoorStrip = new THREE.Mesh(
    new THREE.BoxGeometry(2.0, 0.02, 0.02),
    new THREE.MeshBasicMaterial({ color: 0x00aaff })
  )
  rightDoorStrip.position.set(1.0, 0.8, 0.5)
  rightDoorStrip.rotation.y = Math.PI / 2
  scene.add(rightDoorStrip)

  // 座椅
  scene.add(createSeat(-0.45))
  scene.add(createSeat(0.45))

  // 方向盘
  scene.add(createSteeringWheel())

  // 远光灯
  const headLightColor = 0xffffee
  const headLights: THREE.SpotLight[] = []
  const headLightBulbs: THREE.Mesh[] = []

  const createHeadLight = (x: number) => {
    const light = new THREE.SpotLight(headLightColor, 3000, 100, Math.PI / 8, 0.3)
    light.position.set(x, 0.5, -1.5)
    light.target.position.set(x, 0, -10)
    scene.add(light)
    scene.add(light.target)
    headLights.push(light)

    const bulb = new THREE.Mesh(
      new THREE.SphereGeometry(0.05, 16, 16),
      new THREE.MeshBasicMaterial({ color: headLightColor })
    )
    bulb.position.set(x, 0.5, -1.5)
    scene.add(bulb)
    headLightBulbs.push(bulb)
  }
  createHeadLight(-0.8)
  createHeadLight(0.8)

  return {
    dashboardStrip,
    leftDoorStrip,
    rightDoorStrip,
    screen,
    screenTexture,
    headLights,
    headLightBulbs
  }
}
