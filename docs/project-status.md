# TrainFlow 项目状态

**状态日期:** 2026-05-27
**仓库:** `liujyks-creator/jianshen`
**主分支:** `main`

## 当前状态

TrainFlow 已经具备首版产品基线、UX 基线、初始数据契约草案、Android 首版架构草案、MVP roadmap/backlog 草案、实现准备检查报告、官方设计系统草案、开源 UI 定制边界草案、一个 React/Vite 前端原型，以及 E0.1 Android 生产工程骨架、E0.2 Android 模块/包边界、E0.3 核心 Kotlin 模型映射、E0.4 Room/DataStore 持久化基础骨架、E1.1 首批动作内容切片和 E1.2 首批动作 fixture。

项目已经从早期头脑风暴进入 Android 工程脚手架和 MVP story 实施阶段。`Story E0.1: 创建 Android 生产工程` 已按默认工程参数落地：包名 `com.liujyks.trainflow`、Gradle Kotlin DSL、Jetpack Compose + Material 3、单 `app` module 起步。`Story E0.2: 建立模块与包边界` 已在单 `app` module 内收敛核心/feature/UI/platform package 边界，并用轻量架构测试约束明显的反向依赖；物理 Gradle module 拆分继续留到代码体量需要时再做。`Story E0.3: 映射核心 Kotlin 模型` 已在 `core.model` 包内落地 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState` 和恢复建议相关契约。`Story E0.4: 建立 Room 与 DataStore 基础` 已在 `core.database` 与 `core.datastore` 包内落地最小可编译持久化骨架、Room schema 导出和 smoke test。`Story E1.1: 定义首批动作内容切片` 已收敛 11 个首批动作、必填字段、训练类型适配、指导内容边界、恢复/替代映射草案和审核标准。`Story E1.2: 导入动作 fixture` 已将 11 个首批动作导入 Android fixture，并用 fixture 校验测试约束 ID、必填字段、能力标签、默认建议、恢复映射和替代动作边界；本阶段仍未引入训练引擎、动作库内容加载、repository、通知调度、真实心率设备或语音能力。

## 已有产物

### 产品与规划

当前规划来源文件为：

1. `docs/planning/product-brief.md`
2. `docs/planning/prd.md`
3. `docs/planning/ux-design.md`
4. `docs/planning/data-contracts.md`
5. `docs/planning/decision-log.md`
6. `docs/architecture.md`
7. `docs/roadmap-backlog.md`
8. `docs/readiness-report.md`
9. `DESIGN.md`
10. `docs/ui-extension-guide.md`
11. `docs/planning/action-content-slice.md`

这些文档覆盖：

- 产品目标、目标用户、MVP 边界和后续阶段设想。
- 计时训练与力量训练流程。
- 跟练雏形能力。
- 动作倒计时、休息提醒和力量组记录。
- 动作库内容要求、首批动作切片与数据接口。
- 心率展示边界和后续设备接入边界。
- 面向未来语音交互与平台适配的训练命令和训练事件。
- Android 原生首版架构、模块边界、本地持久化、训练执行引擎和平台适配边界。
- MVP 里程碑、Epic、Story 和验收顺序。
- E0.1 启动条件、当前 blocking/P1/P2 风险和实现范围边界。
- 官方默认 UI 设计系统 token、组件语义和训练执行页设计规则。
- 开源社区定制 UI shell、主题、首页布局和按钮位置的边界。

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
7. Android 首版采用 Kotlin、Jetpack Compose、本地优先和训练执行引擎独立业务核心。
8. 首版训练提醒以普通通知为基线，不把闹铃级强提醒作为 MVP 硬依赖。
9. 官方默认 UI 以 `DESIGN.md` 为设计系统单一真源。
10. 开源社区可以定制 UI shell、主题和布局，但不能破坏核心训练引擎、命令事件和数据契约。

精简决策记录见 `docs/planning/decision-log.md`。

## 仍待确定

以下事项在生产实现深入前仍需继续收敛：

1. 首批导入动作库的动作清单和内容深度已由 E1.1 收敛，详见 `docs/planning/action-content-slice.md`；`sourceMeta`/`extensions` 对齐策略已由 E1.2 记录到 `docs/planning/decision-log.md` 和 `docs/planning/data-contracts.md`。
2. 跟练雏形首版的精确边界。
3. 首版是否真的播放语音读秒，还是只保留语音接口。
4. Android 工程脚手架 E0.1 已采用 `minSdk 26`、`compileSdk/targetSdk 36`、包名 `com.liujyks.trainflow`、Kotlin DSL、单 `app` module 起步。
5. 后续心率数据源策略和健康数据权限流。
6. 各 story 的详细开发说明、测试清单和验收记录。
7. 官方默认 UI 是否首版同时提供暗色主题，还是先提供浅色工作区 + 深色训练执行页。
8. `docs/planning/data-contracts.md` 与 `prototype/src/data/contracts.ts` 的 `WorkoutCommand` 细节需要后续对齐；E0.3 已以文档和决策日志为准，保留文档中的 `update_actual_weight`、`update_actual_reps` 和更细的力量组完成/确认输入结构。

## 建议下一步

除非用户改变方向，建议按以下顺序推进：

1. 进入 `docs/roadmap-backlog.md` 中的 `Story E1.3: 动作库列表与筛选`，基于 E1.2 fixture 实现只读动作列表与筛选。
2. 在 E1.3 前继续避免提前实现训练引擎、通知调度、真实心率设备接入或 voice control。
3. E1.3 如需要读取动作 fixture，应保持 Room entity 不泄漏到 feature/UI，并避免提前建设完整 repository 业务层之外的能力。
4. 在 E0/E1 完成后，再决定是否进入 Figma 视觉细化或社区 UI shell 示例。

## 验证快照

当前前端原型曾用以下命令检查：

```powershell
cd .\prototype
npm.cmd run lint
npm.cmd run build
```

新克隆仓库后，应在 `npm.cmd install` 后重新执行这些命令。

Android E0.1 工程曾用以下命令检查：

```powershell
$env:JAVA_HOME = "<JDK 17 path>"
$env:ANDROID_HOME = "<Android SDK path>"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat tasks --all
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
```

当前 E0.1 本地验证使用 Gradle 9.4.1、Android Gradle Plugin 9.2.0、Kotlin/Compose compiler 2.3.21、Compose BOM 2026.05.00、Android SDK Platform 36。
E0.2 继续沿用同一技术基线，新增 package boundary 架构测试随 `app:check` 执行。
E0.3 继续沿用同一技术基线，新增 `core.model` 轻量契约测试随 `app:check` 执行。
E0.4 在同一单 `app` module 中新增 Room 2.8.4、DataStore Preferences 1.2.1、KSP 2.3.9、Robolectric 4.16.1 和 AndroidX Test Core 1.7.0。Room schema 导出到 `app/schemas`，新增 Room/DataStore smoke test 随 `app:check` 执行。

## 新 Codex 会话提示词

新会话可从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md、docs/readiness-report.md 以及 docs/planning 下的规划文档。
然后检查当前仓库状态与 prototype 原型，基于当前已接受的 MVP 基线继续推进 TrainFlow，不要静默扩大范围。
```
