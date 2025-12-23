# 阶段 6: API 与 Mock

## 状态

⚪ 待开始

## 目标

实现后端 REST API，创建环境数据模拟器，支持前端实时获取氛围方案。

## 前置依赖

- [x] 阶段 5: Agent 编排（需要 Agent 可正常工作）

## 任务清单

### REST API

- [ ] POST `/api/vibe/analyze` - 分析环境，返回氛围方案
  - 输入：Environment JSON
  - 输出：AmbiencePlan JSON
- [ ] GET `/api/vibe/status` - 获取当前氛围状态
- [ ] POST `/api/vibe/feedback` - 用户反馈（喜欢/不喜欢）
- [ ] 实现统一响应格式（ApiResponse）
- [ ] 实现全局异常处理

### WebSocket（可选）

- [ ] 配置 WebSocket 端点 `/ws/vibe`
- [ ] 实现环境变化实时推送
- [ ] 实现氛围方案实时更新

### Mock 环境数据生成器

- [ ] 创建 EnvironmentSimulator 类
- [ ] 实现场景模板
  - 深夜归途场景
  - 周末家庭出游场景
  - 通勤早高峰场景
  - 自定义随机场景
- [ ] 实现环境渐变逻辑（模拟真实驾驶）
- [ ] 支持定时推送环境变化

### API 文档

- [ ] 集成 Swagger/OpenAPI
- [ ] 编写 API 使用说明

### 测试

- [ ] 编写 Controller 单元测试
- [ ] 编写 API 集成测试
- [ ] 使用 Postman 测试 API

## 相关文件

```
src/main/java/com/vibe/controller/
├── VibeController.java
└── WebSocketController.java (可选)

src/main/java/com/vibe/simulator/
├── EnvironmentSimulator.java
└── ScenarioTemplate.java

src/main/java/com/vibe/config/
└── WebSocketConfig.java (可选)
```

## 完成标准

- [ ] API 可正常调用并返回正确结果
- [ ] Mock 数据生成器可模拟多种场景
- [ ] API 文档完整
- [ ] 代码已提交到 Git

## 问题与笔记

（开发过程中遇到的问题和解决方案记录在这里）
