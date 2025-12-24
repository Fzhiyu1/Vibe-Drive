# 场景结构设计

## 相机配置

```javascript
const camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 100);
camera.position.set(0, 1.3, 0.6);  // 两座椅中间，头部高度

const controls = new OrbitControls(camera, renderer.domElement);
controls.target.set(0, 0.8, -1.5);  // 看向仪表台
controls.enablePan = false;
controls.maxPolarAngle = Math.PI / 1.9;
controls.minDistance = 0.1;
controls.maxDistance = 5;
```

## 材质定义

```javascript
const darkMat = new THREE.MeshStandardMaterial({
    color: 0x111111,
    roughness: 0.7,
    metalness: 0.2
});

const seatMat = new THREE.MeshStandardMaterial({
    color: 0x222222,
    roughness: 0.9
});

const chassisMat = new THREE.MeshStandardMaterial({
    color: 0x111111,
    roughness: 0.9
});

const roadMat = new THREE.MeshStandardMaterial({
    color: 0x222222,
    roughness: 0.8
});

const screenMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
const lineMat = new THREE.MeshBasicMaterial({ color: 0xffff00 });
```

## 几何体清单

| 组件 | 几何体 | 尺寸 | 位置 | 旋转 |
|------|--------|------|------|------|
| 仪表台 | BoxGeometry | 2.2 × 0.6 × 1.0 | (0, 0.7, -1.0) | - |
| 中控屏 | PlaneGeometry | 0.8 × 0.3 | (0, 1.05, -0.49) | x: -0.3 |
| 地板 | PlaneGeometry | 10 × 10 | Y=0 | x: -π/2 |
| 底盘 | BoxGeometry | 2.5 × 0.1 × 4 | (0, 0.05, -0.5) | - |
| 道路 | PlaneGeometry | 4 × 200 | (0, 0.01, -100) | x: -π/2 |
| 道路中线 | PlaneGeometry | 0.1 × 200 | (0, 0.02, -100) | x: -π/2 |
| A柱×2 | CylinderGeometry | 0.04/0.06 × 1.5 | (±0.8, 1.5, -0.5) | z: ±0.3, x: -0.5 |

## 方向盘

```javascript
const wheelGroup = new THREE.Group();

// 方向盘圆环
wheelGroup.add(new THREE.Mesh(
    new THREE.TorusGeometry(0.18, 0.02, 16, 50),
    darkMat
));

// 横杆
wheelGroup.add(new THREE.Mesh(
    new THREE.BoxGeometry(0.34, 0.04, 0.02),
    darkMat
));

// 转向柱
const column = new THREE.Mesh(
    new THREE.CylinderGeometry(0.025, 0.03, 0.4, 16),
    darkMat
);
column.rotation.x = Math.PI / 2;
column.position.z = -0.2;
wheelGroup.add(column);

wheelGroup.position.set(-0.45, 0.85, -0.2);
wheelGroup.rotation.x = -0.3;
```

## 座椅

```javascript
function createSeat(x) {
    const group = new THREE.Group();

    // 座垫
    group.add(new THREE.Mesh(
        new THREE.BoxGeometry(0.55, 0.15, 0.6),
        seatMat
    ));

    // 靠背
    const back = new THREE.Mesh(
        new THREE.BoxGeometry(0.55, 0.7, 0.15),
        seatMat
    );
    back.position.set(0, 0.4, 0.25);
    back.rotation.x = -0.1;
    group.add(back);

    // 头枕
    const head = new THREE.Mesh(
        new THREE.BoxGeometry(0.3, 0.2, 0.1),
        seatMat
    );
    head.position.set(0, 0.85, 0.2);
    group.add(head);

    group.position.set(x, 0.3, 0.2);
    return group;
}

scene.add(createSeat(-0.45));  // 左座椅
scene.add(createSeat(0.45));   // 右座椅
```

## 远光灯

```javascript
const headLightColor = 0xffffee;

// 左远光灯
const leftHeadLight = new THREE.SpotLight(headLightColor, 3000, 100, Math.PI / 8, 0.3);
leftHeadLight.position.set(-0.8, 0.5, -1.5);
leftHeadLight.target.position.set(-0.8, 0, -10);
scene.add(leftHeadLight);
scene.add(leftHeadLight.target);

// 左灯发光体
const leftLightBulb = new THREE.Mesh(
    new THREE.SphereGeometry(0.05, 16, 16),
    new THREE.MeshBasicMaterial({ color: headLightColor })
);
leftLightBulb.position.set(-0.8, 0.5, -1.5);
scene.add(leftLightBulb);

// 右远光灯（同理）
const rightHeadLight = new THREE.SpotLight(headLightColor, 3000, 100, Math.PI / 8, 0.3);
rightHeadLight.position.set(0.8, 0.5, -1.5);
rightHeadLight.target.position.set(0.8, 0, -10);
```
