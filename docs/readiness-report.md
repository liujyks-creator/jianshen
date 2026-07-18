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

> 2026-07-16 E17-1 closeout 刷新：E17-1 已 `reviewed / merged`，最终设备/协议结论为 `passed`。Immutable Story SHA `b7a48b980b54e34763212699c64ce387866ec064` 已通过 merge commit `17a305725a4241810ea4dbd26a29414c2be2582b` 合入并成为 `main` ancestor；E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。独立 Review 无 blocker、must-fix 或 should-fix；持续 notify 后顶部 Stop 不易访问，以及 debug 工具 `currentGatt` callback / UI 共享状态未显式串行化，均为 debug-only nice-to-have，不扩展为 production 重构任务。当前 `PLU110`、Android 16 / SDK 36、HUAWEI Band 9 与 APK SHA256 `60abda376470a667ec5c94d16a24e996b2e3e7033df2cc7b4dc6d4132e8dbbc7` 的证据已确认广播关闭扫描无标准 HRS source、四个广播开启周期的 `0x180D`、GATT、notify 型 `0x2A37 properties=0x10`、`0x2902`、CCCD `01 00`、连续 notify、真实 raw payload / parser bpm 一致，以及 Huawei Health 广播开启时断开、广播关闭后可恢复。四周期 label / address 相同不证明永久身份；两次 `status=19` 是链路不稳定事实；Band 固件与 Huawei Health 版本未确认；最终恢复缺少额外截图但有周期间恢复截图与用户现场观察。上述结果不证明 production provider 稳定、production 架构完成、lifecycle / reconnect 正确、自动重连可用、其他环境通用，也不能用 AVD 代替真实 BLE / GATT。**E17 implementation readiness 仍未通过。** E17-2 为 `planned / prerequisite-gated`，只负责重新定义产品范围；E17-3 与 E17-4 为 `planned / locked`，E17-4 通过前禁止 production implementation。胶囊视觉与互动继续 `adopted / frozen / direct reuse`，runtime、provider state、mapper、文案、优先级和 wiring 不冻结；自动重连不是默认前置。

> 2026-07-16 E17-2 历史刷新：用户已确认 `docs/planning/e17-2-heart-rate-product-scope.md` 的完整产品合同，决策为 D-080。心率默认关闭；用户在训练偏好显式开启后是重要训练能力，胶囊在 TrainFlow 前台跨页面显示 bpm、非医疗区间和用户上限视觉提示，未训练只显示不记录。权限只在主动扫描 / 连接时请求；saved device 只作便利提示，用户点击后才有限时精确匹配。训练中心率记录、平均 / 最高心率、曲线、区间时长 / 占比、覆盖缺口、用户主动导出到电脑和由用户导入外部模型分析均为接受的后续方向，但 Room、采样、分析、导出和复盘视觉审查另拆。前台持续自动恢复及活跃训练后台断连自动恢复已确认有价值，但为 `accepted product value / deferred implementation / requires separate product decision refinement and Story`，初始恢复基线仍是手动恢复。当时 E17 implementation readiness 未通过，E17-2 / E17-3 状态仍按旧门禁判定、E17-4 为 `planned / locked`；该状态已由下方 2026-07-18 刷新 supersede。

> 2026-07-18 E17-3 刷新：E17-2 immutable Story SHA `b50778c90cf0232b08b857fda32ba6605fbef224` 已是 `main` ancestor，状态为 `reviewed / merged`。用户已确认方案 A，D-081 与 `docs/planning/e17-3-heart-rate-minimum-architecture.md` 采用一个 Application / 进程级 `HeartRateRuntimeOwner`，以 Android main looper 串行化 generation、attempt、raw GATT identity 与进程可见性事实，统一处理 permission TOCTOU、scan / connect / CCCD / notify、freshness 和幂等 cleanup。D-081 作为 D-027 / E7.2 的窄例外，只在“活跃训练 + 已有合法当前心率连接”时使用 `connectedDevice` FGS；现有 notification controller contract 的唯一 Application-scoped production coordinator 以 ID `7200` 管理 ordinary / FGS handoff，`POST_NOTIFICATIONS` 拒绝仍须向 `startForeground()` 提交 FGS notification。前台 terminal 可保留同一从未 cleanup 且仍 eligible 的 live attempt 并转为只显示不记录，后台 / 锁屏 / 可见性不确定 terminal 必须 cleanup；两者都不自动恢复。Malformed payload 只作内部 diagnostic，不立即形成公共 `TechnicalFailure`。用户接受持续通知 / 系统任务管理器可见成本；非训练后台停止，`START_NOT_STICKY`。**E17 implementation readiness 仍未通过。** 若 E17-3 immutable SHA 尚不是 `main` ancestor，或独立 Review / merge / push、`main...origin/main = 0 0`、权威文档一致性任一未满足，则 E17-3 为 `implemented / needs review`、E17-4 为 `planned / prerequisite-gated`；全部满足后 E17-3 自动视为 `reviewed / merged`、E17-4 gate 自动 satisfied。本 Repair 不宣称门禁已满足；E17-4 readiness 通过前禁止 production implementation。

## 1. Readiness Decision

| 范围 | 结论 | 说明 |
|---|---|---|
| E0.1 Android 生产工程 | Go with confirmations | 规划、架构、设计和 backlog 已经给出足够锚点；开始前需要确认 Android 工程参数。 |
| E0.2-E0.4 架构地基 | Ready after E0.1 | 模块边界、核心模型、Room/DataStore 方向明确，但依赖工程实际结构落地。 |
| E1 动作库 | Partially ready after E1.1 | `O-001` 已由 `docs/planning/action-content-slice.md` 收敛；E1.2 可进入 fixture 导入，但仍不得提前实现完整动作库业务层或 UI 闭环。 |
| E6 跟练雏形 | Not yet | 需要先收敛 `O-002` 跟练边界。 |
| E7 通知、声音、震动 | Partially ready | D-027 / E7.2 ordinary notification 基线与 D-081 `connectedDevice` FGS 窄例外策略均已确认；声音倒计时仍按各 Story 门禁。FGS production implementation、权限、ID `7200` 通知交接、后台行为和设备 evidence 尚待 E17-4 readiness 验证。 |
| E17 真实心率能力 | Product scope and minimum architecture confirmed / implementation not ready | D-080 产品合同与 D-081 方案 A 最小架构已确认；E17-3 尚需独立 Architecture Review / merge，E17-4 readiness 尚未开始。唯一 owner、terminal 前台 / 后台分流、malformed 语义、单一通知 coordinator、FGS 升降级与通知权限拒绝合同已确定；具体 production implementation 与 AVD / Band 9 evidence 尚未授权。 |

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
- `docs/planning/e17-3-heart-rate-minimum-architecture.md`

## 3. 一致性检查

| 检查项 | 结果 | 备注 |
|---|---|---|
| PRD 与 UX | Pass | 训练前编辑、训练中低干扰执行、训练后总结恢复的路径一致。 |
| PRD 与 Data Contracts | Pass | `Exercise`、`WorkoutPlan`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent` 能覆盖 MVP 主流程。 |
| Data Contracts 与 Architecture | Pass | 训练引擎独立于 UI 和平台能力，适合未来跨端迁移。 |
| Architecture 与 Backlog | Pass | E0-E9 顺序符合先地基、再内容、再闭环、再增强的实现路径。 |
| DESIGN 与 UI Extension Guide | Pass | 官方 UI 可以保持优雅一致，社区 UI 可以改 shell/theme/layout，但不能改训练语义。 |
| Decision Log 与当前计划 | Pass | Android 原生、local-first、核心引擎独立、D-027 / E7.2 ordinary notification 基线与 D-081 的窄 FGS 例外互不冲突。 |

## 4. 范围边界

### 已实现 MVP 与历史心率基线

- Android-first 原生 App。
- 计时训练和力量训练两个可用闭环。
- 动作库基础内容、计划创建、执行页、训练总结、历史和基础恢复建议。
- 跟练雏形，复用计时训练流程。
- E11 / E16 之前的 MVP 曾全面撤下页面内联心率显示、手动心率输入、`未获取心率` 占位和旧平均心率趋势，并未接入生产真实设备；这是历史范围事实，不是 D-080 之后的永久产品排除项。
- 通知、声音、震动和偏好设置的基础能力。
- 官方默认设计系统和可 fork 的 UI shell 边界。

### 当前 E17 产品与架构已接受但尚未 implementation-ready

- D-080 已接受默认关闭、用户显式 opt-in 后读取标准 HRS 设备心率，并通过已冻结胶囊在 TrainFlow 前台跨页面显示 bpm、非医疗区间和用户上限视觉提示。
- D-081 已接受唯一进程 owner、main-looper serialization、attempt / raw GATT identity、窄 permission TOCTOU、facts / presentation 分层、manual recovery，以及活跃训练 `connectedDevice` FGS / 单一训练通知方案；malformed payload 不立即形成公共 `TechnicalFailure`。
- D-081 是 D-027 / E7.2 的窄例外和部分 supersession：ordinary training 不普遍启用 FGS；只有活跃训练已有合法当前心率连接时升级，连接结束而训练继续时降级 ordinary notification。
- 现有 `ActiveWorkoutNotificationController` production instance 作为唯一 Application-scoped coordinator，Route 只提交状态；ID `7200` 在 ordinary / FGS 间单一 writer handoff。`POST_NOTIFICATIONS` 拒绝时 ordinary notification 可不发布，但 FGS 仍须构造并提交 notification。
- 明确前台 terminal 可保留同一从未 cleanup 且仍 eligible 的 live attempt，转为非训练前台只显示不记录；后台、锁屏或可见性不确定 terminal 必须 cleanup。Route existence 不作为前台事实，两条路径均不自动 scan / connect / reconnect。
- production implementation 尚未完成。若 E17-3 合并门禁任一未满足，只允许 E17-3 Review / Repair；全部满足后 E17-4 gate 自动 satisfied。E17-4 readiness 通过前不得开始 production implementation。
- 训练记录、复盘分析、覆盖缺口、用户导出和自动恢复仍是后续独立能力，不是 E17-3 默认前置。

### 当前未纳入或继续排除

- 云同步、账号体系、社交、排行和内容信息流。
- 完整课程运营平台和大型教练视频库。
- 自动语音教练、AI 实时动作纠错、音乐节拍编排。
- 医疗级心率告警、疾病判断、声音 / 震动强制提醒、自动暂停、训练中断和康复治疗建议。
- Health Connect 历史读取、全天候健康监测，以及未经单独接受的通用品牌 / 设备兼容承诺；D-080 对标准 HRS + 当前 Band 9 验证目标的接受不自动扩张这些范围。
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
- 当前状态与门禁：E16 umbrella 已为 `closed by correct-course / superseded by E17`。E16-10b-2 保持 `changes requested`；失败分支 `codex/e16-10b-2-foreground-reconnect-controller` immutable tip `89d1e23f870185a2e279d35bb293883f64fe70ba` 永久禁止合并。D-074 至 D-078 和 O-009 只保留为历史接受事实，不再是 E17 当前合同。E17-0、E17-1、E17-2 已 `reviewed / merged`；若 E17-3 immutable SHA 尚不是 `main` ancestor，或独立 Review / merge / push、`main...origin/main = 0 0`、权威文档一致性任一未满足，则 E17-3 为 `implemented / needs review`、E17-4 为 `planned / prerequisite-gated`；全部满足后 E17-3 自动视为 `reviewed / merged`、E17-4 gate 自动 satisfied。自动恢复为已接受价值的延后独立候选，不是 E17-3 默认架构。
- `DESIGN.md` 已建立机器可读 token，但设计 lint 曾出现超时，后续如接入自动校验应单独处理。

## 7. 架构适配检查

- 训练执行应通过 `WorkoutCommand` 进入核心引擎，不能由 UI 直接改写 session。
- `WorkoutEvent` 是训练历史、提醒、总结和恢复建议的事实来源之一。
- `WorkoutPlan` 表达目标和结构，`WorkoutSession` 表达实际执行和结果，二者不能混写。
- Room entity 不能泄漏到 feature UI。
- 通知、音频、震动、心率和媒体属于平台适配边界，不应反向依赖 feature UI。
- 历史 MVP 曾只保留抽象心率状态 / provider 边界，并撤下生产真实设备读取和 UI；D-080 已 supersede 其中的全面排除范围，接受显式 opt-in 后的标准 HRS 设备心率与冻结胶囊显示，但当前 production implementation 仍须先通过 E17-3 与 E17-4。
- 当前心率接入继续消费抽象状态 / 平台边界，不把设备 SDK 模型泄漏到 UI；医疗判断、危险告警、声音 / 震动强制提醒、自动暂停或训练中断继续排除。

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

E17-0、E17-1、E17-2 已 `reviewed / merged`。E17-3 方案 A 最小架构已获用户确认，决策为 D-081，主文档为 `docs/planning/e17-3-heart-rate-minimum-architecture.md`；E17-3 / E17-4 当前值按下段条件式 Git 与文档门禁判定，本 Repair 不宣称门禁已满足。

当 E17-3 尚未完成独立 Architecture Code Review、`--no-ff` merge / push、最终 immutable Story SHA ancestry、`main...origin/main = 0 0` 与权威文档一致性时，只允许 E17-3 Review / Repair，不得开始 E17-4。全部满足后 E17-3 自动视为 `reviewed / merged`、E17-4 gate 自动 satisfied；主管理从 Git 解析最终 SHA，不要求状态 docs-sync。E17-4 readiness 通过前不得开始 production implementation。记录、分析、用户导出与自动恢复均需各自后续门禁。
