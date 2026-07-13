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

> 2026-07-06 刷新：E16-1 BLE HRS adapter spike 已实现为 debug-only adapter harness + 纯 Kotlin Heart Rate Measurement parser，并已在真实 Android 手机 + HUAWEI Band 9 heart-rate broadcast mode 下通过 smoke。证据链覆盖连接 Band 9、发现 `0x180D` / `0x2A37 props=notify`、CCCD `0x2902` 写入成功、收到 bpm notify 并映射到 `HeartRateState` flow，结论为 `passed: Band 9 broadcast -> BLE HRS adapter -> HeartRateState bpm flow`。该结果不改变 MVP readiness：生产训练 UI、记录、Room、commands/events、history/trends 和 production manifest 仍不接心率。未来生产心率仍需要单独权限 / opt-in / lifecycle / privacy / UI 高保真 gate。

> 2026-07-06 刷新：E16-2 Production BLE HRS provider hardening 已实现 provider / state / permission / device-selection preference / lifecycle 地基。`core.health` 现在可生产编译 BLE HRS provider，但生产训练页仍不消费它，production manifest 仍未声明 BLE 权限，App 启动不请求权限、不扫描、不连接；debug `HR Broadcast Smoke` 只是显式手动 harness。MVP readiness 仍保持：不显示心率、不录入心率、不统计心率；未来展示心率前仍必须完成 opt-in、权限说明、隐私 / 非医疗文案、真机 smoke 和 `huashu-design` HTML 高保真 UI gate。

> 2026-07-07 刷新：E16-2 真机 smoke 已按用户 Android 手机截图补齐并通过。Debug APK 入口为 `TrainFlow Debug` -> `DebugEntryActivity`，包含 `进入 TrainFlow` / `HR Broadcast Smoke` 两个明确按钮；`HR Broadcast Smoke` 可见且未污染 TrainFlow 首页。截图覆盖 `bluetooth_disabled` recoverable state、`scanning`、`device_found`、`device_selected`、`connecting`、`connected_waiting_for_data`、HUAWEI Band HR-OD7 `D8:F0:42:01:90:D7 services=[0x180D]`、Heart Rate Measurement notify enabled、live bpm `84-91` 和 stop 后 `stopped: BLE HRS provider stopped`。用户已确认 TrainFlow 训练页没有心率 UI。该结果关闭 E16-2 real-device smoke 缺口，但仍不是生产心率 UI 上线许可。

> 2026-07-07 刷新：E16-3 初版 HTML 高保真视觉规划已完成，但顶部状态 pill 推荐方向已被后续讨论取代。当时确认的心率 UI 方向是 E16-3a App 内可拖动浮动心率胶囊：不使用系统级悬浮窗权限，偏好开启后可在 TrainFlow app 内显示，未训练时只显示不记录，训练中 1 秒采样记录 timed / strength 全过程。E16-3a 需要先做 `huashu-design` HTML 高保真视觉修订，验证拖动、点击展开、吸附安全区、连接 / 数据状态和“区间 + bpm”颜色，不得直接进入 Android UI；该视觉修订后续已在 2026-07-08 刷新中标记完成。

> 2026-07-08 刷新：E16-3a App 内可拖动浮动心率胶囊 HTML 视觉修订已完成并合入（merge commit `24c84c7`）。E16-4 Heart-rate opt-in / settings / permission rationale planning 已完成，主文档为 `docs/testing/e16-4-heart-rate-opt-in-settings-planning.md`。当前 readiness 结论仍不是生产心率 UI 或记录落库许可：心率默认关闭，必须由设置页显式开启；BLE 权限只能在用户主动开启 / 选择设备 / 重新扫描后触发；不使用系统 overlay 权限；未训练只显示不记录；设备偏好只保存 identifier / display name；训练中 1 秒采样持久化、Room / session schema、history / trends 和分析另拆。下一步若继续心率，应从 E16-5 settings / opt-in UI 开始，而不是直接进入训练页 Android UI。

> 2026-07-08 刷新：E16-5 Heart-rate settings / opt-in UI implementation 已实现，主文档为 `docs/testing/e16-5-heart-rate-settings-opt-in-ui.md`。当前 readiness 结论仍不是权限、设备选择、浮动胶囊或记录落库许可：本轮只在设置页提供 `心率与设备` 卡片和 `heartRateDisplayEnabled` 显式开关，默认关闭；开启后仅表示显示偏好已启用、后续可选择设备，未连接源 / 待选择设备；关闭后明确不显示胶囊、不扫描、不连接、不记录。生产 manifest 未新增 BLE / location / overlay 权限，App 不请求 runtime permission，不扫描、不连接、不写 session record，不恢复旧心率卡片、`未获取心率`、手动心率输入或平均心率趋势。后续仍需从 E16-6 permission request flow、E16-7 device picker / source status、E16-8 app-shell floating capsule、E16-11 recording model 和 E16-12 analysis 分 story 推进。

> 2026-07-08 刷新：E16-6 Heart-rate BLE permission request flow 已实现，主文档为 `docs/testing/e16-6-heart-rate-permission-request-flow.md`。当前 readiness 结论仍不是设备选择、扫描、连接、浮动胶囊或记录落库许可：本轮仅新增 production manifest 中 Android 12+ `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` runtime permissions，以及 Android 11 及以下 `ACCESS_FINE_LOCATION maxSdkVersion=30` 蓝牙扫描兼容 fallback；设置页只有在用户已开启心率显示并主动点击 `准备连接设备` 后才显示 App 内 rationale，继续点击 `授权蓝牙权限` 才触发系统权限弹窗。默认关闭和仅开启 switch 都不会请求权限；权限允许 / 拒绝 / 可检测的不再询问状态只回填 UI 文案。仍不扫描、不连接、不展示设备列表、不接训练页胶囊、不写 session record、不改 Room / records / history / trends、不恢复旧心率 UI；E16-7 device picker / source status 仍需单独 story。

> 2026-07-09 刷新：E16-7 Heart-rate device picker / source status 已实现，主文档为 `docs/testing/e16-7-heart-rate-device-picker-source-status.md`。当前 readiness 结论仍不是训练页浮动胶囊、GATT 连接、bpm 读取或记录落库许可：本轮只在 `心率与设备` 设置页为已开启心率显示且已授权蓝牙权限的用户提供主动点击扫描入口，scan window 为 12 秒，扫描中可停止，页面离开 / 关闭心率 / 权限丢失 / 蓝牙关闭 / 超时 / 失败时停止 scanner；候选列表只展示 HRS-capable devices，选择后仅保存 `bleHeartRateDeviceIdentifier` / `bleHeartRateDeviceDisplayName`，并提供清除入口。仍不调用 `connectGatt`、不订阅 `0x2A37 notify`、不读取 bpm、不接训练页、不写 session record、不改 Room / records / history / trends、不恢复旧心率 UI；E16-8 app-shell floating capsule 仍需单独 story。

> 2026-07-09 刷新：E16-8 App-shell floating heart-rate capsule 已实现，主文档为 `docs/testing/e16-8-heart-rate-floating-capsule.md`。当前 readiness 结论仍不是 GATT 连接、live bpm 生产接入、训练记录落库或分析许可：本轮只在 official app shell 顶层实现 App 内 overlay 胶囊，偏好关闭时隐藏，偏好开启时显示设置 / 权限 / source / 已选设备状态，并支持 collapsed / expanded、tap、drag threshold、左右安全边 snap 和 fixed exclusion zones。Mapper 已预留 bpm-only、zone+bpm、stale/offline 和 `超过上限` 深红视觉-only 分支，但生产路径不伪造真实 bpm，不启动 scan / connect / notify，不写心率样本。unit/build/lint/check 已通过；固定 AVD `TrainFlow_Pixel_API_36` 已启动为 `emulator-5554` 并完成 E16-8 AVD UI smoke，`bounds-check.txt` / `bounds-evidence.json` 为 `overall=PASS`，已有 no-overlap / no-crash evidence；rectangular shadow 已修复，shadow-fix smoke 已完成。真实 Band 9 / BLE live bpm 人工测试后续进行，不阻塞 E16-8。E16-9 state mapping / real provider hardening、E16-10 stale / offline policy、E16-11 recording model 和 E16-12 analysis 仍需单独 story。

> 2026-07-09 review-fix 刷新：E16-8 真机人工测试反馈已修复，仍不改变 readiness 边界。浮动心率胶囊 expanded 现在有最大宽高限制，小屏 / 避让区不足时降级为 compact expanded 或保持 collapsed，不再以巨大椭圆遮挡设置页、confirm-record 输入或底部导航；expanded 内 `心率与设备` 按钮现在可点击，并只导航 / 定位到设置页现有心率与设备卡片，不请求权限、不扫描、不连接、不打开 debug smoke；debug APK 默认 launcher 恢复为 TrainFlow MainActivity，`HR Broadcast Smoke` 保留在 `app/src/debug` 作为 explicit debug-only 工具，不阻挡普通测试。Review-fix 证据目录为 `.local/smoke/e16-8-heart-rate-floating-capsule-review-fix/`。E16-9、E16-10、E16-11 和 E16-12 仍需单独 story，不能从本修复直接进入 live bpm、记录落库或分析。

> 2026-07-11 刷新：E16-9 / E16-9b HeartRateState -> floating capsule live state mapping 与 saved-device clarity 已 reviewed / merged 到 `main`（merge commit `3271697fbc5c3d3385fbcdbc214f4d1a9a2c6832`），主文档为 `docs/testing/e16-9-heart-rate-capsule-state-mapping.md`。`4b7689a` 已通过 code review；固定 AVD 已从该 commit 重新 build/install，确认默认关闭时胶囊隐藏、显式开启后显示 `权限未赋予`；用户随后完成 Band 9 修复后人工验收，确认精确 identifier reconnect 与 live bpm “扫描其他设备”窗口通过。readiness 结论仍不是训练记录落库或分析许可：provider/source/live state 只读映射到 App 内浮动胶囊，只有用户已开启心率显示、已授权蓝牙并在设置页主动选择 provider 已知设备后才允许 `connectSelectedDevice()`；胶囊点击、展开和拖动不触发 scan。冷启动不自动连接，runtime error 不跨进程保存；不写 `WorkoutSession`、不做 1s sampling persistence、不新增 Room / migration、不改 records/history/trends、`WorkoutCommand` / `WorkoutEvent`、TimedWorkoutEngine、StrengthWorkoutEngine、TimerDial、声音 / 震动 / 通知或 cue。E16-10 stale / offline policy、E16-11 recording model 和 E16-12 analysis 仍需单独 story。

> 2026-07-12 E16-10 closeout 刷新：E16-10a docs-only policy 已 reviewed / merged（merge commit `56d8029719889d329680f3dc099a77ae94909142`）；E16-10b umbrella 保持 in progress。E16-10b-1 Heart-rate freshness policy core 已 reviewed / merged（Story tip `09d17616f213c1df7905e46662f4a195345fdd9a`，merge commit `5cdee7ce1bd7a2b0f76f83adf069179a547fd16c`），只合入 `core.health` 纯 Kotlin monotonic timeline / policy / reason codes 与 JVM tests。production provider/controller、浮动胶囊、设置页和 GATT runtime 尚未消费该 policy，运行时行为不变。E16-10b-2 仅为 unlocked / not started，可在 docs-sync 合入并通过独立 Review gate 后创建独立 Story；真实 timer、scheduler、watchdog、retry controller、callback race、old-target guard、lifecycle cancellation 与 reconnect 仍未实现。E16-10b-3 / E16-10b-4 保持 locked / not started；纯 JVM tests 不代表真实 BLE / reconnect readiness。该结果不是 E16-11 recording / 1 秒持久化或 E16-12 analysis / recap 许可。

> 2026-07-13 E17-0 closeout 刷新：上段 E16-10b 的“下一阶段”描述现仅为历史快照。E16 umbrella 已以 `closed by correct-course / superseded by E17` 关闭；E16-10b-2 failed tip `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor，状态保持 `changes requested` 并永久禁止合并。E17-0 已 `reviewed / merged`：immutable Story SHA `abce4b712139c373f534a6fabab423fe138fc29c` 已是 `main` ancestor，merge commit 为 `2eee72cc44c2c7733cb565ea665ebfae48610085`，E17-0 本体 Review 无 finding 并完成 merge / push；E17-0 合并完成时的基线已确认 `main...origin/main = 0 0`。**E17 implementation readiness 仍未通过。** E17-1 是下一项 `planned / prerequisite-gated` 设备/协议复验 Story，不是 production implementation；其启动资格由本 closeout 的条件式门禁决定：immutable SHA 尚未成为 `main` ancestor 时只允许独立 Review / merge；独立 Review、merge / push、ancestry、`main...origin/main = 0 0` 与权威文档一致性全部满足后，门禁自动满足并由主管理生成 E17-1 提示词。E17-2 产品边界、E17-3 最小架构和 E17-4 readiness 仍为 `planned / locked`，E17-4 通过前禁止 production implementation。旧 E16 代码、测试和真机证据不等于 E17 ready；胶囊视觉与互动继续 `adopted / frozen / direct reuse`，runtime 状态、mapper、文案、优先级和接线不冻结；自动重连不是默认前置。

## 1. Readiness Decision

| 范围 | 结论 | 说明 |
|---|---|---|
| E0.1 Android 生产工程 | Go with confirmations | 规划、架构、设计和 backlog 已经给出足够锚点；开始前需要确认 Android 工程参数。 |
| E0.2-E0.4 架构地基 | Ready after E0.1 | 模块边界、核心模型、Room/DataStore 方向明确，但依赖工程实际结构落地。 |
| E1 动作库 | Partially ready after E1.1 | `O-001` 已由 `docs/planning/action-content-slice.md` 收敛；E1.2 可进入 fixture 导入，但仍不得提前实现完整动作库业务层或 UI 闭环。 |
| E6 跟练雏形 | Not yet | 需要先收敛 `O-002` 跟练边界。 |
| E7 通知、声音、震动 | Partially ready | 普通通知方向明确；声音倒计时和前台服务策略仍需 Story 前确认。 |
| 真实心率/健康数据 | Deferred | 首版只保留抽象状态、provider 边界、设置页 opt-in / 权限 / device picker、E16-8 App 内浮动胶囊和 E16-9 provider/source/live state 只读映射，不录入、不统计心率；E16 正向 BLE HRS 证据、E16-1 debug adapter spike、E16-2 production-capable provider hardening / real-device smoke pass、E16-3a floating capsule HTML、E16-4 opt-in planning、E16-5 / E16-6 / E16-7 设置页实现、E16-8 shell overlay 和 E16-9 live state mapping 都不是训练记录落库或分析许可。 |

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
- `O-006` / E16 历史快照：E16 曾验证 Band 9 可通过心率广播暴露标准 BLE HRS；E16-1 至 E16-10b-1 中已经合入 `main` 的 Story 继续保留各自 immutable merge fact。E16-10a 当时接受的有限前台 direct reconnect 与 10 / 15 / 30 秒 freshness policy 已 reviewed / merged（merge commit `56d8029719889d329680f3dc099a77ae94909142`）；E16-10b-1 纯 Kotlin policy core 也已 reviewed / merged（Story tip `09d17616f213c1df7905e46662f4a195345fdd9a`，merge commit `5cdee7ce1bd7a2b0f76f83adf069179a547fd16c`）。这些 `reviewed / merged` 仅是历史事实，不表示 E17 继承其产品或技术合同。
- 当前状态与门禁：E16 umbrella 已为 `closed by correct-course / superseded by E17`。E16-10b-2 保持 `changes requested`；失败分支 `codex/e16-10b-2-foreground-reconnect-controller` 的 immutable tip 为 `89d1e23f870185a2e279d35bb293883f64fe70ba`，disposition 为 `superseded by E17 / permanently prohibited from merge`。E16-10b-3 / E16-10b-4 旧路线终止，E16-11 / E16-12 不自动进入 E17；D-074 至 D-078 和 O-009 只保留为历史接受事实，不再是 E17 当前合同。E17 implementation readiness 未通过。E17-0 已 `reviewed / merged`，Story SHA `abce4b712139c373f534a6fabab423fe138fc29c` 经 merge commit `2eee72cc44c2c7733cb565ea665ebfae48610085` 合入且已是 `main` ancestor，E17-0 本体 Review 无 finding；E17-1 为 `planned / prerequisite-gated`，E17-2 至 E17-4 为 `planned / locked`。当 closeout immutable SHA 尚未成为 `main` ancestor 时，允许动作仅限独立 Review / merge，不得启动 E17-1；当独立 Review、merge / push、closeout SHA ancestry、`main...origin/main = 0 0` 和权威文档一致全部满足时，门禁自动满足，主管理可直接生成 E17-1 提示词。不需要也不得创建 closeout 的 closeout。E17-1 是设备/协议复验，不是 production implementation；自动重连不得作为默认前置。
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

原 `MVP Alpha readiness 前检查` 及其可复制提示词已经失效，只保留在 Git 历史中，不得继续使用。E16 umbrella 已由 correct-course 关闭并被 E17 替代；旧 E16-1 / E16-2 只能作为 historical / reference 或 revalidation 输入，不能直接作为 E17 provider 地基合同。自动重连不得作为默认前置。

E17-0 已 `reviewed / merged`；Story SHA 为 `abce4b712139c373f534a6fabab423fe138fc29c`，merge commit 为 `2eee72cc44c2c7733cb565ea665ebfae48610085`。E17-1 保持 `planned / prerequisite-gated`。当本 closeout immutable SHA 尚未成为 `main` ancestor 时，closeout 仍处于 Review / merge 门禁中，允许动作仅限独立 Review / merge，不得启动 E17-1；当独立 Review、merge / push、closeout SHA ancestry、`main...origin/main = 0 0` 和当前权威状态文档一致全部满足时，门禁自动满足。门禁满足后，主管理可通过 Git 解析 closeout immutable SHA 并生成 E17-1 提示词；不需要也不得创建“closeout 的 closeout”。

E17-2 至 E17-4 继续 `planned / locked`。E17-1 未完成前不得进入 E17-2；E17-2 产品边界、E17-3 最小架构与 E17-4 readiness 尚未完成，E17-4 通过前不得开始 production implementation。旧 E16 代码、测试和真机证据不等于 E17 ready；在 closeout 门禁满足前也不得开始 Band 9 / HRS 复验。自动重连不是默认前置。
