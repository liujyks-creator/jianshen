---
workflowType: implementation-readiness
projectName: TrainFlow
documentLanguage: zh-Hans
status: conditional-pass
date: 2026-05-26
inputDocuments:
  - docs/planning/product-brief.md
  - docs/planning/prd.md
  - docs/planning/ux-design.md
  - docs/planning/data-contracts.md
  - docs/architecture.md
  - docs/roadmap-backlog.md
  - DESIGN.md
  - docs/ui-extension-guide.md
  - docs/planning/decision-log.md
stepsCompleted:
  - source-review
  - consistency-check
  - scope-boundary-check
  - e0-entry-check
---

# TrainFlow 实现准备检查报告

**结论:** 条件通过，可以进入 `Story E0.1: 创建 Android 生产工程`。  
**限制:** 这不是完整 MVP 全量开工许可，只确认工程地基阶段可以启动。后续 E1 到 E9 仍需要按 Story 做局部检查、实现和验收。

> 2026-07-05 刷新：上述 E0.1 结论是 Android 工程启动期历史 gate。当前 main 已推进到 E15 / E16 收口后：E15-5d 已 review / accepted / merged（merge commit `0fa28463e4c24bf039944402a209f8f55c922c1b`，story commit `d9875bd48cd3e51b560c677efc3f6d4440efc89a`），用户 APK 测试已通过，维护入口为 `docs/testing/e15-maintenance-lessons-learned.md`；E16 heart-rate broadcast feasibility retest 已 reviewed / merged（merge commit `bbd4296`），Band 9 广播开启条件下已有 BLE HRS 正向证据，但只允许未来另拆 `E16-1 BLE HRS adapter spike`。后续 readiness 入口应进入 MVP Alpha readiness 前检查；若出现新的真机反馈，另拆 User Test Fix Pack 2。

> 2026-07-06 刷新：E16-1 BLE HRS adapter spike 已实现为 debug-only adapter harness + 纯 Kotlin Heart Rate Measurement parser。该 spike 可支持真实 Android 手机 + Band 9 心率广播继续验证 adapter 状态流，但不改变 MVP readiness：生产训练 UI、记录、Room、commands/events、history/trends 和 production manifest 仍不接心率。未来生产心率仍需要单独权限 / opt-in / lifecycle / privacy / UI 高保真 gate。

## 1. Readiness Decision

| 范围 | 结论 | 说明 |
|---|---|---|
| E0.1 Android 生产工程 | Go with confirmations | 规划、架构、设计和 backlog 已经给出足够锚点；开始前需要确认 Android 工程参数。 |
| E0.2-E0.4 架构地基 | Ready after E0.1 | 模块边界、核心模型、Room/DataStore 方向明确，但依赖工程实际结构落地。 |
| E1 动作库 | Partially ready after E1.1 | `O-001` 已由 `docs/planning/action-content-slice.md` 收敛；E1.2 可进入 fixture 导入，但仍不得提前实现完整动作库业务层或 UI 闭环。 |
| E6 跟练雏形 | Not yet | 需要先收敛 `O-002` 跟练边界。 |
| E7 通知、声音、震动 | Partially ready | 普通通知方向明确；声音倒计时和前台服务策略仍需 Story 前确认。 |
| 真实心率/健康数据 | Deferred | 首版只保留抽象状态和 provider 边界，不显示、不录入、不统计心率；E16 正向 BLE HRS 证据和 E16-1 debug adapter spike 只支持未来生产化评估，不是 MVP 生产接入。 |

## 2. 已检查文档

- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `docs/roadmap-backlog.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/decision-log.md`
- `docs/project-status.md`

## 3. 一致性检查

| 检查项 | 结果 | 备注 |
|---|---|---|
| PRD 与 UX | Pass | 训练前编辑、训练中低干扰执行、训练后总结恢复的路径一致。 |
| PRD 与 Data Contracts | Pass | `Exercise`、`WorkoutPlan`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent` 能覆盖 MVP 主流程。 |
| Data Contracts 与 Architecture | Pass | 训练引擎独立于 UI 和平台能力，适合未来跨端迁移。 |
| Architecture 与 Backlog | Pass | E0-E9 顺序符合先地基、再内容、再闭环、再增强的实现路径。 |
| DESIGN 与 UI Extension Guide | Pass | 官方 UI 可以保持优雅一致，社区 UI 可以改 shell/theme/layout，但不能改训练语义。 |
| Decision Log 与当前计划 | Pass | Android 原生、local-first、核心引擎独立、普通通知优先等决策互不冲突。 |

## 4. 范围边界

### MVP 内

- Android-first 原生 App。
- 计时训练和力量训练两个可用闭环。
- 动作库基础内容、计划创建、执行页、训练总结、历史和基础恢复建议。
- 跟练雏形，复用计时训练流程。
- 心率状态抽象和 provider 边界；当前 MVP 不显示、不录入、不统计心率。
- 通知、声音、震动和偏好设置的基础能力。
- 官方默认设计系统和可 fork 的 UI shell 边界。

### MVP 外

- 云同步、账号体系、社交、排行和内容信息流。
- 完整课程运营平台和大型教练视频库。
- 自动语音教练、AI 实时动作纠错、音乐节拍编排。
- 医疗级心率告警、疾病判断、康复治疗建议。
- 真实可穿戴设备接入和 Health Connect 历史数据读取。
- 运行时插件市场、远程主题下载和 App 内安装第三方 UI 包。

## 5. E0.1 启动条件

E0.1 可以开始，但在创建 Android 工程前应确认以下参数：

1. Android package name。
2. minSdk / targetSdk。
3. 是否使用 Gradle Kotlin DSL。
4. 首轮使用单 `app` module 加 package 边界，还是立即启用多 Gradle module。
5. 首个空壳 App 是否同时预留 Compose Material 3 theme/token 文件。
6. 活跃训练的后台计时/前台服务是否只预留边界，还是在 E0 就放入 scaffold。

推荐默认值供下一轮确认：

- Package name: `com.liujyks.trainflow`
- Gradle: Kotlin DSL
- UI: Jetpack Compose + Material 3
- E0.1 结构: 先创建可构建的 `app` 工程和清晰 package 边界；多 module 可在 E0.2 拆出
- 后台计时: E0.1 只预留接口和文档，不实现前台服务

## 6. Blocking / P1 / P2

### Blocking

当前没有阻塞 E0.1 的产品或架构矛盾。

### P1

- `O-001` 首批动作库内容切片已在 E1.1 确定；`sourceMeta`/`extensions` 与 prototype contract 的对齐方式已在 E1.2 按 D-026 记录。
- Android 工程参数必须在 E0.1 开始前确认，否则 package、SDK 和 Gradle 结构会影响后续代码迁移成本。

### P2

- `O-002` 跟练边界需要在 E6 前确认：是只做固定预设，还是允许兼容的计时训练计划切换到跟练视图。
- `O-003` 语音倒计时需要在 E7 前确认：首版是否只做声音/震动/强化动画，还是加入语音读秒。
- `O-006` 健康数据和可穿戴策略不阻塞 MVP 核心闭环。E16 已证明 Band 9 心率广播可暴露 BLE HRS，E16-1 已提供 debug-only adapter spike；进入真实生产心率前仍必须另拆生产化 story，并重新设计权限、数据来源、用户 opt-in、非医疗提示、连接生命周期、隐私策略和 UI 视觉方案。
- `DESIGN.md` 已建立机器可读 token，但设计 lint 曾出现超时，后续如接入自动校验应单独处理。

## 7. 架构适配检查

- 训练执行应通过 `WorkoutCommand` 进入核心引擎，不能由 UI 直接改写 session。
- `WorkoutEvent` 是训练历史、提醒、总结和恢复建议的事实来源之一。
- `WorkoutPlan` 表达目标和结构，`WorkoutSession` 表达实际执行和结果，二者不能混写。
- Room entity 不能泄漏到 feature UI。
- 通知、音频、震动、心率和媒体属于平台适配边界，不应反向依赖 feature UI。
- 心率首版是抽象状态和 provider 边界，不读取生产真实设备、不显示 UI、不做医疗告警。

## 8. UI 与开源定制检查

- 官方 UI 的默认方向明确：浅色工作区 + 深色训练执行面板，重点信息优先。
- 社区可以改 theme token、首页布局、按钮位置、导航结构和页面组合。
- 社区 UI 不能绕过训练引擎，不能改 `WorkoutCommand` / `WorkoutEvent` 语义，不能隐藏必要权限说明。
- MVP 不做运行时插件市场。开源定制方式是 fork、编译期选择 shell/theme、提交主题或 layout PR。

## 9. 安全与隐私检查

- 当前范围不涉及 API Key、外部模型 API 或云端账号。
- 训练数据默认 local-first。
- 通知权限需要清晰说明用途。
- 未接入真实设备时不请求健康数据权限。
- 心率、热量、恢复建议必须使用非医疗化表达。

## 10. 下一轮建议

当前阶段不再从本报告的 E0.1 模板启动。E15 和 E16 retest 已收口，下一轮进入：

```text
MVP Alpha readiness 前检查
```

建议新对话启动提示词：

```text
继续 TrainFlow 项目。

当前任务：MVP Alpha readiness 前检查。

启动前请先读取：
- AGENTS.md
- docs/project-status.md
- docs/planning/decision-log.md
- docs/readiness-report.md
- docs/architecture.md
- docs/roadmap-backlog.md
- DESIGN.md
- docs/ui-extension-guide.md
- docs/testing/e15-maintenance-lessons-learned.md
- docs/testing/e16-heart-rate-broadcast-feasibility-retest.md

任务范围：
- 只做 MVP Alpha readiness / release-blocking 前检查。
- 复核 E15 已收口、用户 APK 测试通过和 E15 maintenance lessons 是否已被后续维护入口引用。
- 复核 E16 heart-rate broadcast retest 已合入 main，且只作为未来 `E16-1 BLE HRS adapter spike` 的输入。
- 核对是否还有 P0 / release blocker、真机 smoke 缺口、音频共存风险、禁区文件风险、handoff 文档缺口。
- 不启动新功能，不改 Kotlin / Compose / Gradle / Room / APK / 测试代码。
- 不恢复心率卡片、未获取心率、手动心率输入或平均心率趋势。
- 不把 Band 9 正向 BLE HRS 证据解释为当前生产心率 UI 或生产设备接入；未来展示心率前必须先做 HTML 视觉方案 / 高保真案例评审。
- 不恢复力量目标组颜色占位；若未来重新引入，必须先做 model / serializer decision。
- 若用户给出新的具体真机问题，另拆 User Test Fix Pack 2。

完成后请说明：
- readiness 结论和 release-blocking 项。
- 是否需要 User Test Fix Pack 2。
- E15 maintenance lessons 是否已纳入后续维护入口。
- 禁区文件是否未 stage / 未提交。
- 下一轮应该进入哪个 Story 或 gate。
```
