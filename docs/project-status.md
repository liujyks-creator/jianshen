# TrainFlow 项目状态

**状态日期:** 2026-05-21  
**仓库:** `liujyks-creator/jianshen`  
**主分支:** `main`

## 当前状态

TrainFlow 已经具备首版产品基线、UX 基线、初始数据契约草案和一个 React/Vite 前端原型。

项目已经可以从早期头脑风暴进入技术架构与开发排期阶段。现有文档足以指导下一轮实现决策，但 Android 生产架构和交付 backlog 还没有完成。

## 已有产物

### 产品与规划

当前规划来源文件为：

1. `docs/planning/product-brief.md`
2. `docs/planning/prd.md`
3. `docs/planning/ux-design.md`
4. `docs/planning/data-contracts.md`
5. `docs/planning/decision-log.md`

这些文档覆盖：

- 产品目标、目标用户、MVP 边界和后续阶段设想。
- 计时训练与力量训练流程。
- 跟练雏形能力。
- 动作倒计时、休息提醒和力量组记录。
- 动作库内容要求与数据接口。
- 心率展示边界和后续设备接入边界。
- 面向未来语音交互与平台适配的训练命令和训练事件。

### 前端原型

`prototype` 目录包含一个基于 React/Vite 的原型，使用假数据和可交互预览流验证产品方向。

原型当前覆盖：

- 训练首页。
- 计时计划编辑预览。
- 计时训练执行与倒计时状态。
- 动作与休息临近结束强调状态。
- 力量计划编辑预览。
- 力量训练单组流程。
- 完成本组时以计划重量和次数预填实际记录。
- 跟练雏形预览。
- 动作库和动作详情。
- 训练总结和基础恢复映射。
- 模拟心率状态。

原型核心文件：

1. `prototype/src/App.tsx`
2. `prototype/src/App.css`
3. `prototype/src/data/contracts.ts`
4. `prototype/src/data/fixtures.ts`

## 已接受方向

当前已接受方向为：

1. Android 首发，未来保留 iOS 路径。
2. 计时训练和力量训练都属于首版能力。
3. 计时训练是默认推荐入口。
4. 跟练首版只做雏形，复用计时流程和动作内容。
5. 动作库先定接口，再扩大内容量。
6. 心率展示、语音交互、丰富媒体、音乐节奏和 AI 分析先保留接口，不扩张首版交付范围。

精简决策记录见 `docs/planning/decision-log.md`。

## 仍待确定

以下事项在生产实现深入前仍需继续收敛：

1. 首批导入动作库的动作清单和内容深度。
2. 跟练雏形首版的精确边界。
3. 首版是否真的播放语音读秒，还是只保留语音接口。
4. Android 具体架构、模块拆分、持久化方案、通知方案与未来 iOS 共享边界。
5. 后续心率数据源策略和健康数据权限流。
6. 首版交付 backlog、里程碑和验收顺序。

## 建议下一步

除非用户改变方向，建议按以下顺序推进：

1. 创建 `docs/architecture.md`，明确 Android 首版技术架构和未来 iOS 边界。
2. 创建 `docs/roadmap-backlog.md`，拆出里程碑、Epic、MVP stories 和验收顺序。
3. 定义首批动作库导入切片与内容审核清单。
4. 决定下一步先搭 Android 工程，还是先进入 Figma 与视觉设计细化。

## 验证快照

当前前端原型曾用以下命令检查：

```powershell
cd .\prototype
npm.cmd run lint
npm.cmd run build
```

新克隆仓库后，应在 `npm.cmd install` 后重新执行这些命令。

## 新 Codex 会话提示词

新会话可从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md 以及 docs/planning 下的规划文档。
然后检查当前仓库状态与 prototype 原型，基于当前已接受的 MVP 基线继续推进 TrainFlow，不要静默扩大范围。
```
