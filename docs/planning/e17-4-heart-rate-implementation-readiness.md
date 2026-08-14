# E17-4 心率 Production Implementation Readiness

**状态：** `reviewed / merged`；readiness = `passed`

**Git 事实：** immutable SHA `1ea67561b4866aa76c41b854da74da85c208aa25`；merge commit `4b354f5116bbf7f7610e79845210d481c839fed6`

**日期：** 2026-07-18

**性质：** docs-only readiness；production 代码、测试、Manifest、Gradle、资源、prototype 与 sealed E16 archive 均未修改

## 2026-08-15 Correct Course delta

本页既有E17-4/5/6 reviewed/merged与readiness passed事实继续有效；E17-7a/7b也已完成当前合并事实。E17-7b最终candidate `cec16f697a036409693943289d471955ef7a47bc` 由merge commit `e3f2de2106342e06f75c3dce7eaef562ad2a6356`合入。第2至14节继续完整保留为historical/non-operative snapshot，不为本次状态同步改写。

Validated E17-8 lifecycle Correct Course V2取代ordinary-only E17-8 current contract。`VALIDATED`只接受planning correction，不建立implementation `READY`；本docs-only sync不实现E17-8。Current sequence为：docs sync reviewed/merged -> fresh atomic E17-8 Exact Story shaping/readiness -> future exact accepted merge -> E17-9 -> E17-10。

一个原子E17-8必须在同一candidate同时交付Activity-retained active-workout runtime唯一owner与Application ordinary ID `7200`唯一projection coordinator；不得有可分别实施/Review/merge的E17-8a/E17-8b、临时owner、compatibility bridge、transient registry或未来Story补救。Application coordinator不持workout engine、不是session registry；既有Application heart-rate/GATT owner保持独立唯一，E17-9 Service不持engine/GATT。

Retained owner必须持有真实session identity/current producer authority、immutable plan snapshot、engine/progress/`startedAt`、`PRE_START / STARTED_NON_TERMINAL / TERMINAL / INVALIDATED`、terminal-transition gate与persistence-attempt gate；三Route只attach/detach，configuration detach不清有效通知。Terminal transition exactly-once；persistence attempt at-most-once且successful record为0或1。Failure后不retry/upsert/queue/补写、不产生duplicate record、不新增schema/migration、不rollback/revive，identity invalidation与`7200` clear-once继续。

Timed READY且`startedAt = null`为PRE_START；其永久销毁不产生`ABANDONED`或attempt。STARTED_NON_TERMINAL永久销毁固定既有`ABANDONED` exactly-once并至多attempt一次；已TERMINAL不重复。Configuration recreation保持同一runtime/session/start/engine/gates，Route dispose只detach；process death不恢复active workout、不猜测或合成terminal/record并fail-closed清旧`7200`；same-plan A→B使用不同identity且A迟到input不能影响B。Workout-producer generation与E17-9 `handoffGeneration`完全分离。

## 1. Readiness 结论

**结论：E17-4 historical readiness=`passed`；原子E17-8 implementation readiness未建立。** D-080/D-081基础readiness、E17-5/6/7a/7b事实保持；lifecycle V2为后续atomic E17-8提供planning input，但必须经fresh Exact Story shaping/readiness后才能进入implementation。

E17-4/5/6 SHA事实保持；E17-7a immutable SHA `e1b1654168c79edc5bbf78d233fb861e2177c215` 与E17-7b candidate/merge均是Accepted base ancestors。禁止E16 SHA `89d1e23f870185a2e279d35bb293883f64fe70ba` 与frozen candidate `fda5f7cfd3c31af3399dfe231733ea00467a68e8`均不是Accepted base ancestors且不得作为prerequisite。

## 2026-08-15 当前可执行 readiness 替换

本节与页首delta是当前readiness authority。它只允许先进行fresh atomic E17-8 Exact Story shaping/readiness，不直接生成implementation任务。其余第2至14节完整保留为2026-07-18/2026-07-22 **historical/non-operative snapshot**；旧ordinary-only E17-8、旧single-writer reducer范围、旧next-step/gate不得覆盖本节或生成当前任务。

### 当前 atomic E17-8 lifecycle / ownership

- Activity-retained owner是active workout runtime唯一owner；三Route只attach/detach。进入Route、创建runtime、composition rebuild或Route dispose均不等于训练started。
- 每次训练使用独立于plan ID的真实session identity；所有command/callback/terminal/projection匹配current identity与producer authority，单调workout-producer generation拒绝duplicate/late/out-of-order/stale/invalidated input。
- Runtime四态为`PRE_START / STARTED_NON_TERMINAL / TERMINAL / INVALIDATED`。Timed READY且`startedAt=null`是PRE_START；Strength/Follow-along沿用各自accepted start boundary。
- Configuration recreation reattach同一runtime、session identity、start classification、engine/progress、`startedAt`和两个gate；Route dispose无terminal/permanent-destruction/cleanup权限。
- PRE_START永久销毁只invalidate/clear-once，不产生`ABANDONED`或persistence attempt；STARTED_NON_TERMINAL使用既有`ABANDONED` exactly-once并至多attempt一次；persistence failure不改变terminal、不retry/rollback/revive，successful record允许0。
- Process death不依赖`onCleared()`，不恢复active workout、不从destination/plan/notification/stale projection合成session或record；Application coordinator无可信session时fail-closed幂等清旧`7200`。
- Same-plan A→B使用不同identity；A terminal/invalidated后producer authority失效，任何A迟到input不得改变B或其persistence/`7200` projection。
- Application ordinary coordinator是ID `7200`唯一projection owner，不持engine且不是registry；Application heart-rate/GATT owner继续独立唯一。Workout-producer generation与E17-9 `handoffGeneration`分离。

### 当前 Story / acceptance / evidence 矩阵

| Story | 唯一主要风险轴与允许范围 | 必须验收 | Evidence / merge gate |
|---|---|---|---|
| E17-7a / E17-7b | 已完成heart-rate owner policy/data与Application/settings/capsule wiring | 保留identity-bound behavior/evidence，不由本docs sync重测或改写 | `reviewed / merged`；E17-7b candidate/merge见页首 |
| Atomic E17-8 Retained Runtime + Ordinary Coordination | 同一candidate同时交付retained workout runtime owner与ordinary ID `7200` projection coordinator；三Route只attach/detach；不含FGS/BLE扩张 | identity/四态/two gates；PRE_START/started永久销毁；recreation/process death/A→B；terminal exactly-once、attempt at-most-once、success 0/1；coordinator不持engine/非registry；无8a/8b seam | `NOT READY`；先fresh Exact Story shaping/readiness。后续identity-bound lifecycle/failure/ownership证据与独立Review/merge |
| E17-9 Connected-device FGS + Training Background Recovery | concrete Service/Manifest、ID `7200` handoff、合法active-training retain/recovery、shared-owner observer、M1/final freshness；Service不持engine/GATT | 前置为atomic E17-8 exact accepted merge；`handoffGeneration`独立；既有FGS/heart-rate gate保持 | future exact merge成为同步main ancestor且evidence/status一致后才可开始 |
| E17-10 Integrated Production Acceptance | evidence-only；`production changes = 0` | 已合并production能力的identity-bound端到端gate；不得修补production | finding返回责任Story；Repair合并后重建/重跑，旧evidence失效 |

### 当前 preserved presentation 与冻结边界

`ageYears: Int?` 的 `1..130` 只是 sanity guard，不是连接 eligibility；`101` 是合法值。`personalMaxHeartRateBpm: Int?` 与 `alertThresholdBpm: Int?` 均为 `30..260`，alert 独立于 max。effective max 依次为 personal max、`220-age`、none。区间使用未取整比率 `<50`低强度、`[50,60)`热身、`[60,70)`燃脂、`[70,80)`有氧、`[80,90)`无氧、`>=90`极限；严格 `bpm > alert` 优先，相等不触发。无 effective max 仍显示 bpm，alert-only 可提示。上述 presentation 直接复用 D-079 冻结的 E16 capsule 状态 / 颜色 / 互动资产，不改 capsule 本体、geometry、layout、motion 或交互。

### 当前停止条件与禁止候选

若fresh shaping不能由一个Writer在同一candidate因果完整交付runtime owner + ordinary coordinator，或需要8a/8b分别merge、临时owner、bridge、registry、future repair seam、第二workout runtime owner，立即停止并返回Correct Course；不得形成不安全中间production节点。

若实现需要第二heart-rate/GATT owner、generic BLE seam/wrapper、恢复D-078/E16 controller、Service持有engine/GATT、后台无限scan、自动换target、第三notification interface、修改冻结capsule视觉，或E17-10修改production，停止并返回管理复核。两个禁止SHA继续不是prerequisite。

## Historical 2. 盘点基线与硬边界（2026-07-18 snapshot；non-operative）

- 实际工程为单 `app` module，`minSdk 26`、`compileSdk 36`、`targetSdk 36`；AGP `9.2.0`、Kotlin / Compose compiler `2.3.21`、Compose BOM `2026.05.00`、DataStore `1.2.1`、Room `2.8.4`、Robolectric `4.16.1`、AndroidX Test Core `1.7.0`。
- 当前 `TrainFlowApplication` 是 5 行空 Application；实际 database、repository、preferences、心率 scanner/provider 与通知 controller 均在 Activity / Compose / Route 层创建。D-081 要求把心率 owner 与通知 coordinator 移到 Application composition root，但不要求把全部 App 依赖同时重构。
- 当前 `AndroidBleHeartRateProvider.kt` 为 627 行，直接同时持有 scan、candidate、target、GATT、callback 与 DTO mapping；它不是 E17-3 预计中的轻量 adapter。后续按 replace 处理，不能在其上继续叠 owner、FGS 或 reconnect。
- 当前三个 workout Route 各自 `remember(AndroidActiveWorkoutNotificationController)`，各自 update，并在 `DisposableEffect` 中以 `ROUTE_DISPOSED` clear；虽然都写 ID `7200`，仍是三个业务 writer。D-081 要求先收口为唯一 Application-scoped coordinator。
- 当前 Manifest 已有 `POST_NOTIFICATIONS`、Android 12+ `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` 与 API 30 以下 `ACCESS_FINE_LOCATION`；没有 `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_CONNECTED_DEVICE` 或 Service。
- E17 不写 Room、`WorkoutSession` 心率样本、记录、统计、分析、导出、自动恢复、reconnect scheduler 或医疗逻辑；胶囊视觉、互动、geometry、motion 与 adopted HTML 不改。
- E16 sealed命名文档继续只留在仓库、不进入APK；冻结胶囊/geometry、parser、permission经验与既有DataStore字段是正式资产，不是待清垃圾。当前旧provider/scanner/mixed boundary仍production可达，E17-6新owner的production/debug实例化为0；失败E16-10b-2 controller/wrapper在`main`为0，D-078自动重连不恢复。E17-7合并后不得保留旧BLE runtime owner入口。

## Historical 3. 当前代码资产清单（snapshot；non-operative）

除非表中写出完整路径，本节 production 路径均相对 `app/src/main/java/com/liujyks/trainflow/`，unit test 路径均相对 `app/src/test/java/com/liujyks/trainflow/`；Manifest 与 debug 工具使用表中明确的 `app/src/...` 路径。

### 3.1 Runtime、状态与 parser

| 路径 / 类 | 当前责任 | E17 分类 | Implementation Story | 迁移风险 |
|---|---|---|---|---|
| `core/health/HeartRateBoundary.kt` / `HeartRateProvider` | 单一 `Flow<HeartRateState>` provider 边界；另含 disabled / mock provider | adopt as-is（fixture 语义 adapt） | E17-5 | 若另建 provider 会形成双抽象；禁止平行 provider |
| `core/health/AndroidBleHeartRateProvider.kt` | 627 行 scan + device map + selected address + 单 callback + GATT + CCCD + parser + provider mapping | E17-6 暂留 compatibility；E17-7 retire runtime path | E17-6 / E17-7 | E17-6 合并后仍是唯一 production 可达 BLE owner，仅维持既有接线编译；新 owner 只由测试实例化。E17-7 必须原子切走全部 consumer，不能让新旧 owner 同进程运行 |
| `core/health/BleHeartRateProviderBoundary.kt` | BLE candidate / selection / provider / scan DTO，message 与 public mapping 混合 | E17-6 暂留旧接线；E17-7 移除 production consumer | E17-5 / E17-6 / E17-7 | 技术 message 成为用户状态输入；STALE / DISCONNECTED 均携带旧 bpm；STOPPED 映射成 NO_SOURCE。遗留类型若保留只能缩减为不持有 scanner / GATT 的纯 compatibility 数据映射 |
| `core/health/HeartRateDeviceScanner.kt` / `AndroidHeartRateDeviceScanner` | 再包一层 provider，供 Compose 创建、关闭并直接操作 | E17-6 暂留 compatibility；E17-7 删除 ownership path | E17-6 / E17-7 | E17-6 不新增第二 production 可达 owner；E17-7 切换后不得保留可重新实例化 scanner / GATT owner 的 production 入口 |
| `core/health/BleHeartRatePermissionPlanner.kt` | API 31+ scan/connect 与 API <=30 location 权限计划，只允许 explicit action request | adapt | E17-6 / E17-7 | availability check 不能覆盖调用点 TOCTOU；permission recovery 不得自动动作 |
| `core/health/HeartRateMeasurementParser.kt` | 标准 HRS `0x2A37` 8 / 16-bit、flags、malformed null | adopt as-is | E17-5 验证；E17-6 消费 | parser 不是 BLE / Band 证据；malformed 不得刷新有效样本时间 |
| `core/health/HeartRateFreshnessPolicy.kt` | E16 10 / 15 / 30 秒 policy；malformed 被写为 `PARSE_FAILED` technical failure | replace policy 数值与 malformed 语义，adopt monotonic / fail-closed 思路 | E17-5；E17-9 最终锁值 | 旧数字与 D-081 冲突；production 当前尚未消费，不能复制为 E17 默认 |
| `core/model/HeartRateState.kt` | source-aware 通用状态，仍含 manual / recordedAt；不能区分完整 E17 facts | adapt | E17-5 | public state不足会迫使 UI猜测；Android object / exception message 禁止进入 core |
| `core/health/HeartRateMeasurementParserTest.kt` | 8-bit、16-bit、flags、malformed | adopt as-is + 补代表性 regression | E17-5 | 不能代表 Android / GATT / Band 9 |
| `core/health/HeartRateFreshnessPolicyTest.kt` | 锁定旧 10 / 15 / 30 秒、parse failure technical error、immutable / reason code | replace | E17-5 / E17-9 | 旧测试是 superseded 合同，不能作为新 readiness evidence |
| `core/health/HeartRateProviderBoundaryTest.kt` | disabled / mock / manual / stale source-aware fixture | adapt | E17-5 | manual reading 与旧 stale fixture 会泄漏旧合同 |
| `core/health/BleHeartRateProviderBoundaryTest.kt` | permission planner、旧 BLE DTO mapping、availability refresh | adapt / replace | E17-5 / E17-7 | 测 DTO 不等于 owner race；message-based mapping 需移除 |
| `core/health/AndroidBleHeartRateProviderScanFilterTest.kt` | 读取源码字符串证明 HRS filter 存在 | replace | E17-6 | source search 不是具体 production 操作行为证据 |

### 3.2 Settings、偏好、Application 与胶囊接线

| 路径 / 类 | 当前责任 | E17 分类 | Implementation Story | 迁移风险 |
|---|---|---|---|---|
| `core/datastore/TrainFlowPreferences.kt` | 默认关闭 opt-in、saved identifier / display name；另有旧 placeholder key | opt-in / saved fields adopt as-is；placeholder isolate/retire | E17-7 | 不得保存 GATT、bpm、online fact；旧 placeholder 不得恢复 UI |
| `core/datastore/TrainFlowPreferenceKeys.kt` / `core/datastore/TrainFlowPreferencesDataSource.kt` | DataStore read/write；关闭时把 placeholder false；saved preference 分别保存 / 清除 | adopt as-is，必要兼容清理 adapt | E17-7 | cold start 只能恢复 preference，不能恢复 live / failure / connection |
| `app/TrainingPreferencesAppMapper.kt` | preference -> settings UI state | adapt | E17-7 | 不得把 saved hint 映射成 connected |
| `feature/settings/TrainingPreferencesUiState.kt` | permission rationale、12 秒 scan 文案、saved exact match、manual picker、runtime DTO copy | adapt | E17-7 | 12 秒是 scan window，不是 freshness；当前直接依赖旧 provider DTO |
| `feature/settings/SettingsRoute.kt` | opt-in、permission、scan / select / clear 可见 controls | adapt | E17-7 | 只发 typed action；离开设置只停止 scan，不关闭 live connection |
| `app/TrainFlowApplication.kt` | 空 Application | adapt 为唯一 composition root | E17-7；E17-8 增加 coordinator | 没有 Application owner 是当前最大 ownership gap |
| `app/MainActivity.kt` | 在 Compose 中创建 DataStore / Room repository 并传 callback | adapt（仅接入 Application heart-rate / notification instances） | E17-7 / E17-8 | 不做全 App DI 重构；Activity recreation 不能创建第二 owner |
| `ui/shell/official/TrainFlowApp.kt` | `remember(AndroidHeartRateDeviceScanner)`、permission launcher、scan purpose、saved match、自动 connect selected、dispose close、capsule mapper | adapt | E17-7 | 当前 Compose 是 runtime owner；dispose 会关闭跨页面连接；无可靠 process visibility source |
| `ui/shell/official/HeartRateFloatingCapsuleState.kt` | `HeartRateState` + settings -> 339 行 presentation DTO / zone / copy | adapt，保留外部 mapper | E17-5 / E17-7 | manual state、旧 ERROR / stale语义；不能把 BLE object 送入胶囊 |
| `ui/shell/official/HeartRateFloatingCapsule.kt` | 494 行冻结 overlay、tap / drag / collapsed / expanded / exclusion | adopt as-is | 不进入 implementation delta | 任何视觉或互动修改均越界 |
| `ui/shell/official/HeartRateCapsuleGeometry.kt` | 166 行冻结 clamp、safe edge、snap、threshold | adopt as-is | 不进入 implementation delta | 任何 geometry 修改均越界 |
| `docs/design/e16-3-heart-rate-ui-html/index.html` 与 README | adopted 胶囊视觉 / interaction 方案 | adopt as-is | defer（仅 reference） | sealed 文字不得覆盖 D-080 / D-081；不得重做 HTML |
| `ui/shell/official/HeartRateLiveProviderLifecycleTest.kt` | 纯函数测试 display / permission / old provider kind -> connect or stop | replace | E17-7 | 不测试 Application owner、进程可见性或真实 cleanup |
| `ui/shell/official/HeartRateFloatingCapsuleStateTest.kt` | mapper 状态、zone、旧 bpm 不 live、scan isolation、cold start saved hint | adapt | E17-5 / E17-7 | 保留视觉合同，替换旧 public-state fixtures |
| `ui/shell/official/HeartRateCapsuleGeometryTest.kt` | snap、exclusion、threshold、小屏 fallback | adopt as-is | 不进入 implementation delta | 冻结资产回归保护 |
| `ui/shell/official/TrainFlowAppPermissionResultTest.kt`、`feature/workoutsession/HeartRatePermissionBoundaryTest.kt` | permission result 与 Manifest/source boundary | adapt | E17-7 / E17-9 | 源码/Manifest搜索只能作静态 gate，不是 runtime evidence |
| `feature/settings/HeartRateSettingsActionVisualBoundaryTest.kt` | 可见按钮和单 pointer gesture 源码保护 | adopt as-is | E17-7 回归 | 只能保护冻结交互形态 |
| `core/datastore/TrainFlowPreferencesBoundaryTest.kt` | opt-in默认值、saved identifier / name和清理边界 | adapt + 保留默认关闭回归 | E17-7 | 只证明持久化偏好，不证明runtime连接 |
| `app/TrainingPreferencesAppMapperTest.kt`、`feature/settings/TrainingPreferencesUiStateTest.kt` | preference / permission / saved hint到settings状态与文案 | adapt | E17-7 | saved hint不得被测试固化为connected fact |

### 3.3 训练通知、Route 与平台声明

| 路径 / 类 | 当前责任 | E17 分类 | Implementation Story | 迁移风险 |
|---|---|---|---|---|
| `core/notifications/ActiveWorkoutNotificationContracts.kt` | ordinary state/content/policy；ID `7200`；`sessionKey` 当前只承载 plan-derived key；permission denied -> Ignored；clear reason含 Route disposed | adapt | E17-8 / E17-9 | FGS 不能复用 Ignored -> clear；notification 业务身份必须改用真实 workout session ID |
| `core/notifications/AndroidActiveWorkoutNotifications.kt` / controller | `NotificationManager.notify/cancel(7200)`；Ignored 时 clear | adapt 为 Application-scoped coordinator production instance | E17-8 / E17-9 | 当前没有模式状态或 handoff；多个 Route 实例是多个 writer |
| `feature/workoutsession/ActiveWorkoutNotificationUiMapper.kt` | 三个 mapper 当前分别生成 `timed:$planId`、`strength:$planId`、`follow_along:$planId` | adapt mapping 与 submission ownership | E17-8 | plan ID 不能唯一标识一次真实训练实例；mapper 不应持有 controller 或 Service |
| `feature/workoutsession/TimedWorkoutSessionRoute.kt` | 已有真实 `engineState.sessionId`，但通知仍传 plan-derived key；Route 内创建 controller、update、dispose clear | adapt | E17-8 | 当前 writer 1；dispose 可误删仍 active 或更新后的 session 通知 |
| `feature/workoutsession/StrengthWorkoutSessionRoute.kt` | 已有真实 `engineState.sessionId`，但通知仍传 plan-derived key；Route 内创建 controller、update、dispose clear | adapt | E17-8 | 当前 writer 2；同一 plan 快速重开会混淆实例 |
| `feature/workoutsession/FollowAlongWorkoutSessionRoute.kt` | 已有真实 `engineState.sessionId`，但通知仍传 plan-derived key；Route 内创建 controller、update、dispose clear | adapt | E17-8 | 当前 writer 3；旧 Route 迟到事件可覆盖新 session |
| `core/notifications/ActiveWorkoutNotificationContractsTest.kt` | ordinary content、active / paused、terminal、permission denied | adapt + 保留 ordinary baseline | E17-8 / E17-9 | 当前断言“不是 FGS”只能适用于 ordinary content |
| `feature/workoutsession/ActiveWorkoutNotificationUiMapperTest.kt` | 三 route summary mapper | adopt as-is + submission tests | E17-8 | 不证明唯一 writer / Route dispose |
| `core/notifications/PlanReminderNotificationManifestBoundaryTest.kt` | 当前正向 BLE permissions，负向断言无 FGS / Service | replace相关负向断言 | E17-9 | D-081 已 supersede “全局无 FGS”；计划提醒边界仍保留 |
| `app/src/main/AndroidManifest.xml` | notification + scoped BLE permissions；Application / Activity / receiver | adapt | E17-9 | 缺 FGS permissions / service type；不得改为 `health` |
| `app/build.gradle.kts` / version catalog | SDK / AndroidX / test依赖 | defer；仅在 ServiceCompat 无直接可用坐标时窄 adapt | E17-9 | 0 新第三方依赖；不得引入 BLE/DI/framework 库 |
| `core/notifications/ActiveWorkoutHeartRateService.kt`（预计；当前不存在） | 当前不存在 | add one concrete Service | E17-9 | Service不得成为 GATT或训练业务owner；`START_NOT_STICKY` |

### 3.4 E17-1 debug / `.local` 证据

| 资产 | 当前责任 | E17 分类 | 后续用途 | 风险 |
|---|---|---|---|---|
| `app/src/debug/.../E17Band9HrsRevalidationActivity.kt` | debug-only、独立 scanner/GATT/HRS/CCCD/raw notify/cleanup 工具 | reference only；E17-5 M0 已完成 | E17-1 feasibility / E17-5 foreground M0 历史证据 | 不能观察 production owner，禁止冒充 E17-9 shared-owner + FGS M1 evidence |
| `app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt` | debug-only harness 直接实例化旧 `AndroidBleHeartRateProvider` 并消费旧 DTO | E17-6 保持编译兼容；E17-7 最小迁移或退休 | E17-6 / E17-7 | E17-6 不能先删除旧类型导致 debug 编译失败；E17-7 必须移除对旧 provider 的直接引用，但不得把 debug harness 变成 production owner 或第二 runtime owner |
| `docs/testing/e17-1-band9-hrs-revalidation.md` | 当前设备/协议 feasibility `passed` | reference only | 两个新真机 gate 的前置可行性 | 不能替代新 owner / FGS 验收 |
| `.local/smoke/e17-1-band9-hrs-revalidation/` | APK、截图、日志、设备输出 | read-only / never commit | 历史对照 | 存在不等于新 production evidence；后续各 Story 使用独立目录 |

## Historical 4. 当前差距结论（snapshot；non-operative）

1. runtime ownership 当前不合格：provider/scanner 由 Compose 创建和关闭，Application 没有 owner。
2. callback determinism 当前不合格：无 generation、attempt ID、raw GATT identity gate，且 `connectGatt()` 未使用 Handler overload。
3. public state 当前不完整：manual / stale / provider-unavailable 语义与 D-081 facts 不一致；malformed 被错误升级为 public technical failure。
4. freshness 当前不可采用：policy 固定旧 10 / 15 / 30 秒且 production 未消费。
5. notification ownership 当前不合格：三个 Route production writer 共用 ID `7200`，Route dispose 直接 cancel。
6. FGS 当前不存在：Manifest、Service、coordinator mode / handoff、permission-denied FGS content 均未实现。
7. 当前设置与 saved-device UX 大部分可复用；cold start 已不自动 scan / connect，但连接状态仍由旧 provider DTO 驱动。
8. 冻结胶囊本体和 geometry 不需要修改；只有外部 presentation mapper 与输入事实需要适配。

## Historical 5. 产品—架构—实现—证据追踪矩阵（snapshot；non-operative）

| 产品事实（D-080） | 架构规则（D-081） | 当前代码影响 | Story | 自动化证据 | AVD / 真机 gate |
|---|---|---|---|---|---|
| 默认关闭、显式 opt-in | Disabled 时无 BLE 资源 | DataStore 已默认 false；owner需消费 opt-out | E17-5 / 7 | pure state + DataStore / owner cleanup | AVD cold start / opt-out；Band cleanup |
| 权限只由主动 scan/connect 触发 | typed user action；TOCTOU窄处理 | planner可复用；launcher当前在 Compose | E17-6 / 7 | permission planner + owner TOCTOU | AVD grant/deny/revoke |
| 有限时 HRS scan | owner唯一持有 scanner/generation/timeout | 旧12秒 scan可参考；无 generation | E17-6 / 7 | deterministic scan generation / timeout | AVD UI/no-crash；Band RF scan |
| saved identifier精确匹配，不是永久身份 | target只在本次attempt使用 | 现有 exact match可复用；address目前还承担callback identity | E17-6 / 7 | old target / exact match tests | Band saved match + manual fallback |
| 手动选择设备 | settings只发typed action | picker已存在，直接依赖旧DTO | E17-7 | mapper/action tests | AVD picker flow；Band manual choice |
| 前台跨页面胶囊 | Application owner；Compose只观察 | 当前 Compose remember / dispose | E17-7 | Application unique owner / recreation | AVD navigation；Band same attempt |
| 未训练只显示不记录 | owner不依赖Room/session | 当前无心率写库 | E17-7 | source search + repository boundary | AVD foreground display；不产生记录 |
| public state与runtime fact分离 | Android facts -> `HeartRateState` -> presentation | 旧 BLE DTO message直映射 | E17-5 / 6 | pure fact mapping | AVD observable states |
| malformed语义 | diagnostic only；不刷新valid time、不立即failure | 当前 `publishError(PARSE_FAILED)` | E17-5 / 6 | parser + freshness + owner callback tests | Band raw/malformed注入只属确定性层 |
| 旧 bpm不伪装 Live | freshness只认last valid monotonic sample | 旧 stale DTO仍携带 bpm / measuredAt | E17-5 | exact boundary + presentation tests | AVD process recreation no old bpm |
| manual recovery基线 | 不自动 scan/connect/reconnect | 当前 saved click可主动scan；无retry | E17-6 / 7 | canceled closure / no action tests | Band disconnect后仅手动恢复 |
| 非训练后台 cleanup | tracker只发布visibility fact；Application policy组合其他eligibility后向owner发cleanup action | 当前无process lifecycle source | E17-7 | ActivityLifecycleCallbacks fact tests + policy tests | AVD Home/ON_STOP/screen off |
| active workout + existing connection用 `connectedDevice` FGS | 可见前台启动；Service共享同owner | 当前无Service | E17-9 | Service contract + coordinator state machine | AVD FGS；Band lockscreen |
| ID `7200` 单一 writer | Application coordinator，三模式 | 当前三Route writer | E17-8 / 9 | transition table / fake notification port | AVD dumpsys / UI观察单条 |
| notification permission拒绝 | ordinary可不发；FGS仍startForeground提交 | 当前 Ignored会clear且无content | E17-9 | two permission branches | AVD allow/deny |
| FGS升级/降级/terminal handoff | ordered、idempotent、latest state | 当前无模式或handoff | E17-9 | coordinator reducer + Service contract | AVD upgrade/downgrade/terminal；Band active |
| 前台terminal保留同一eligible attempt | visibility明确且attempt从未cleanup | 当前 Route terminal与provider无协调 | E17-7 / 9 | terminal fact matrix | AVD foreground terminal；Band same attempt |
| 后台/锁屏/不确定terminal cleanup | stop FGS + owner cleanup | 当前无visibility fact | E17-7 / 9 | owner/coordinator sequence | AVD + Band background terminal |
| process death / Task Manager / `START_NOT_STICKY` | 不依赖finally；不恢复连接 | 当前无Service，cold start不自动连接 | E17-9 / 10 | Service return + recreation tests | AVD force-stop/process recreation；真机Task Manager观察 |
| 不自动scan/connect/reconnect | action closure失效后不可复活 | 当前选中后自动connect仅来自明确选择；无retry | E17-6 / 7 | generation / canceled closure | AVD/Band回前台无动作 |
| 不写Room/session记录/分析/导出/自动恢复 | owner与data/engine隔离 | 当前无HR写库，继续保持 | 全序列 | boundary/source guards only | 记录页无HR数据；不能冒充行为覆盖 |
| 胶囊视觉与互动不修改 | 外部mapper兼容冻结DTO | capsule/geometry adopt as-is | E17-5 / 7 only mapper | frozen geometry/gesture tests | AVD视觉回归，不重新评审 |

## Historical 6. 公共状态与内部事实矩阵（snapshot；non-operative）

| 场景 | Runtime fact（owner内部） | `HeartRateState` | 胶囊 presentation | bpm / measuredAt 清理 |
|---|---|---|---|---|
| disabled | opt-in false；resources absent | `Disabled` | hidden | 立即从public state清除 |
| permission required / unavailable | concrete call前缺失或调用抛`SecurityException` | `PermissionUnavailable` | 需要权限 | cleanup开始即清除 |
| Bluetooth off | adapter absent/off typed fact | `BluetoothOff` | 蓝牙关闭 | cleanup开始即清除 |
| not connected | enabled、无active attempt | `NotConnected` | 未连接；saved hint可单独显示 | 必为空 |
| scanning | active scan generation + deadline | `Scanning`（或窄 scan presentation，不携带Android device） | 正在查找；既有合法Live不得被旁路scan覆盖 | 既有连接未切换时保留其fresh值；无连接则空 |
| connecting | active attempt / selected target | `Connecting` | 正在连接 | 新attempt开始清除旧target的值 |
| waiting data | connected/subscribed，尚无valid sample | `WaitingData` | 已连接，等待心率 | 必为空 |
| live | active attempt + raw GATT match + last valid sample fresh | `Live(bpm, measuredAt)` | bpm / 区间 / 上限视觉 | 仅valid payload更新；`measuredAt`来自样本事实，不是wall-clock freshness输入 |
| stale / data interrupted | transport可能仍在，但last valid已越界 | `DataInterrupted` | 数据已中断 | 越界点从public live字段清除；诊断副本不得进入mapper |
| explicit link disconnect | active raw GATT非intentional断开 | `LinkDisconnected` | 连接已断开 | 立即清除 |
| technical failure | connect/discovery/HRS/CCCD/明确平台失败 | `TechnicalFailure(reasonCode)` | 连接未成功/异常 | 立即清除 |
| intentional stop | user stop、opt-out、非训练后台、后台terminal cleanup | `IntentionallyStopped(reason)`，稳定presentation可归到Disabled/NotConnected | 已关闭或未连接 | cleanup失效引用时清除 |

Malformed payload 只是 `MalformedPayload(attemptId, receivedAtElapsed, optional counter)` diagnostic。它不刷新 `lastValidSampleAt`，不创建新 `measuredAt`，不立即进入 public `TechnicalFailure`；已有 valid sample 只活到原 freshness 截止，无样本时保持 WaitingData。intentional cleanup 在失效 attempt 后到来的 `STATE_DISCONNECTED` 必须被 old-generation / old-attempt gate 丢弃，不能伪造 `GATT_DISCONNECTED`。process recreation 从 Disabled / NotConnected + saved hint 重建，绝不恢复旧 live bpm。old callback、old target、old generation、mismatched raw GATT 与已取消 closure 均不能 discover、写CCCD、发布Live或创建新attempt。

现有 `HeartRateFloatingCapsuleUiState` 足以承载上述 presentation；E17-5 只允许外部 mapper / 窄 presentation input 兼容，不修改冻结 UI DTO 的视觉字段语义，不把 `BluetoothDevice`、GATT、address或异常message放入胶囊。

## Historical 7. Optional BLE seam 最终规则（snapshot；non-operative）

**结论：默认不新增 `AndroidBleOperations` seam。** 当前所需确定性可以由原生 Android BLE types + 一个只负责 attempt ID / raw GATT 首次绑定与 mismatch 拒绝的窄 callback harness 完成；具体 `startScan`、`stopScan`、`connectGatt`、`discoverServices`、`setCharacteristicNotification`、`writeDescriptor`、`disconnect`、`close` 继续是 owner 内的真实 production 调用。callback harness 不复制 GATT 模型、不接收 arbitrary business lambda、不做异常归类。

E17-6 明确禁止自行新增 `AndroidBleOperations`、完整 GATT wrapper、parallel device model或 `call(operation, block)`。若在实现前的 test-first owner race 证明：不用 seam 无法确定性覆盖 callback 早于 `connectGatt()` 返回，Story 必须停止并返回主管理做最小架构复核；不得在实现中临时扩张。即使未来复核允许，也只能包围 owner实际使用的单个 `connectGatt` 调用，production consumer只能是owner，且：

- 只分类该调用官方声明的 `SecurityException`；未知 `IllegalStateException`、`RuntimeException`、`Throwable` 继续抛出。
- 不按exception message或厂商字符串分类。
- attempt激活、provider mapping、state transition与legacy mutation全部在异常边界外。
- callback holder先捕获attempt ID并允许raw GATT首次绑定；不得使用会被早到callback读取的未初始化 `lateinit connection`。

## Historical 8. Freshness 测量与无循环门禁（snapshot；non-operative）

E17 不继承 D-078 / E16 的 10 / 15 / 30 秒。顺序固定如下：

1. **E17-5 M0，任何policy编码前：** 在现有E17 debug工具中仅加入monotonic receive timestamp / interval与typed outcome日志；使用Band 9前台连续notify测量valid payload间隔，分别记录malformed、真实disconnect和平台失败，证据只写 `.local/smoke/e17-5-heart-rate-fact-core/`。
2. E17-5实现者根据前台分布提出一个保守、内部、非最终的 `FreshnessThresholds`，独立Review锁定 waiting、live freshness、data-interrupted三条边界。阈值不得从E16复制；证据不足则E17-5不提交。
3. 阈值进入纯Kotlin边界测试：阈值前Live、阈值点清除bpm、阈值后保持interrupted、malformed不续命、invalid monotonic fail-closed。所有时间为elapsed realtime；wall clock只作展示。
4. E17-6 / 7只启用前台manual链路；active workout进入后台时，在FGS未实现前必须cleanup，不能宣称后台Live保证。
5. **E17-9 M1：** 先实现但不宣称验收完成的connected-device FGS路径；在同一Story合并前，用Band 9测量screen off / lockscreen /临时后台的valid notify调度分布和余量，仍区分malformed、真实disconnect、平台失败。
6. E17-9实现者据M0+M1提出最终阈值，独立Review锁定；更新同一纯Kotlin阈值测试后，才允许完成FGS Story并对外描述最终Live边界。M1证据不足则E17-9不得merge，production保持E17-7前台manual能力与后台cleanup。
7. E17-10只复验最终阈值下的完整链路，不重新选择数字；发现回归时返回E17-9 Repair，而不是在QA Story临时调值。

因此锁屏余量依赖FGS，但不形成循环：前台 provisional threshold只支持E17-7前台能力；FGS在E17-9内先可运行、后测量、再锁最终值、最后合并。没有任何阶段把旧bpm或未测数字宣传为最终Live合同。

### 8.1 E17-5 M0 APK 身份门禁

E17-5 修改 debug `E17Band9HrsRevalidationActivity.kt` 后必须重新 build、install 并形成新的 M0 APK 身份；禁止沿用 E17-1 APK 或其 SHA256。E17-5 evidence 文档必须记录：

- 包含 debug 测量工具修改的完整 source / preparation commit SHA、APK 路径、build variant、applicationId、测量 Activity 与 APK SHA256。
- build / install 时间，以及测试手机 serial、型号、Android 版本与 API level。
- Band 9 测试条件、M0 monotonic 日志路径与采样窗口，并分别记录 valid payload 间隔、malformed、真实 disconnect 和平台失败。
- 后续 Story tip 相对 APK preparation commit 是否只有文档变化；仅当 Git diff 证明没有任何可执行变化时，该 APK 身份才可继续使用。

APK preparation commit 之后，只要存在任何影响 debug APK 的代码、Manifest、Gradle 或资源变化，旧 M0 APK 证据立即失效；必须重新 build / install、重新计算 SHA256 并重跑 M0。`.local` 中的 APK、日志、截图和设备输出不得提交。E17-5 独立 Review 必须交叉核验 source SHA、APK SHA256 与真实 Band 9 日志一致，不能把 E17-1 feasibility 证据当作 E17-5 threshold evidence。

### 8.2 E17-9 shared-owner M1 与 final APK 身份门禁

E17-9 的 M1 只能由 debug-only shared-owner evidence observer 观察同一个 `TrainFlowApplication` owner。默认复用 E17-7 已移除旧 provider 实例化后的 `app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`；只允许为该 observer 窄适配 `app/src/debug/AndroidManifest.xml` 与 `app/src/debug/java/com/liujyks/trainflow/app/DebugEntryActivity.kt`。observer 只收集 owner 现有 public `heartRateState`，不得创建 scanner、GATT、callback、attempt 或第二 owner，也不得复用独立 GATT 的 `E17Band9HrsRevalidationActivity.kt` 冒充 production owner + FGS 证据。若需要在 APK 内显示 source SHA，`app/build.gradle.kts` 只可增加 debug-only build identity field；不得借此改变 production 行为或新增第三方依赖。

最低可用 evidence UI 必须提供固定可访问控制区、独立滚动日志、周期摘要、可复制或导出的文本证据，并清楚显示 source full SHA、installed APK SHA256 / bytes、variant 与 applicationId。observer 可在收到 public Live emission 时用自身 `SystemClock.elapsedRealtime()` 记录观察时点与 emission interval，并记录 Waiting / DataInterrupted / LinkDisconnected / TechnicalFailure；public `measuredAt` 是展示用 wall clock，malformed 不发布到 public state，`StateFlow` 也可能合并观察。因此日志不得宣称 raw notify、完整 callback 或 malformed 计数。E17-9 进入 M1 前必须先证明现有 public state 足以确定性记录所需分布；若不足，立即停止回主管理，不得修改 `HeartRateRuntimeOwner.kt`、新增核心诊断 interface 或临时创建独立 BLE runtime。

E17-9 是同一 Story 内不可拆分合入 `main` 的五阶段链：

1. 提交 runnable FGS implementation 与 shared-owner observer source。
2. 从该 source commit 强制 build / install measurement APK，记录下方完整 measurement identity tuple。
3. 使用该 APK 完成 M1 锁屏 / 临时后台 evidence，并提交 evidence 记录。
4. 依据 M0 + M1 提交 final freshness threshold 代码与 tests；不得猜测 final threshold。
5. 从最终 executable source 强制重建 final APK，记录下方完整 final identity tuple，并重跑全部受影响 unit / platform tests、AVD handoff 与 Band 9 active-training lockscreen / background gate。

**Measurement APK identity tuple（M1记录必填）：**

- runnable FGS executable source full SHA；APK SHA256；APK bytes；build variant；applicationId。
- measurement Activity完整类名或明确入口路径；启动入口与App内导航方式，必须足以让Review复现到shared-owner observer。
- 设备型号；可取得的设备identifier/serial；无法取得时写`unknown`并记录具体原因，不猜测。Android系统版本与API level分别记录。
- build timestamp、install timestamp、measurement开始/结束时间或measurement window，三者均记录timezone。
- 当前实际使用的production Application owner身份：Application完整类名、owner完整类名、唯一创建点路径，以及observer取得同一实例而未创建scanner/GATT/第二owner的核对结果。
- 对应M1日志、截图和原始数据的精确位置；记录各路径与本measurement APK身份的绑定关系。
- 已知设备固件版本与配套App名称/版本；任何无法取得的字段写`unknown`并记录原因，不猜测。

**Final APK identity tuple（final gate记录必填）：**

- final threshold代码与tests commit完成后必须强制重新build；记录final executable source full SHA、APK SHA256、APK bytes、build variant与applicationId。
- Activity完整类名或明确入口、启动入口/导航方式；设备型号与可取得的identifier/serial，无法取得时写`unknown`及原因。
- Android系统版本与API level；build、install、test开始/结束时间或test window，全部记录timezone。
- 明确列出final APK实际重跑的每一个受影响AVD gate与Band 9 gate及结果；未实际重跑的gate不得写为通过。
- final日志、截图和原始数据的精确位置，并把每项证据绑定到final APK identity。
- 记录measurement APK与final APK是否来自同一executable Git tree，并给出tree/diff核对依据；若不同，必须明确写`not equivalent`，measurement APK及其证据不得证明final executable或final gate。相同文件名不代表相同APK。

**Executable identity与evidence失效规则：** 以下任一变化都会产生新的executable identity，并使旧APK以及绑定旧APK的截图、日志、UI tree和设备输出不能证明新的executable或final gate：`app/src/main` production executable变化、`app/src/debug` harness/observer变化、Android Manifest变化、Gradle或dependency变化、Android资源变化、build variant/applicationId/入口变化，以及任何影响编译产物的build配置变化。发生变化后必须重新build/install/hash并重跑所有受影响gate；measurement APK截图或日志不能证明后续发生executable变化的final APK。

仅文档修改，或完全不改变executable tree的测试证据整理，可以不改变APK identity，但必须以Git tree/diff证明没有任何executable变化。不得用旧E17-1 APK替代E17-9 M1，不得用独立自持GATT工具证明shared Application owner/FGS，不得用相同文件名冒充相同APK，也不得猜测缺失的设备、版本或时间字段。measurement APK只能支撑与其身份绑定的M1；final threshold修改后必须重新build/install/hash/验证。E17-5 M0、旧AVD与旧E16截图均不能替代M1或final evidence；M1不足时E17-9不得merge。

## Historical 9. Android 平台与 Manifest 矩阵（snapshot；non-operative）

| 项目 | 当前事实 | E17 implementation规则 | Story / evidence |
|---|---|---|---|
| SDK | min 26 / target 36 / compile 36 | 所有API分支按真实值实现 | E17-6 / 9 static + AVD |
| Android 12+ BLE | Manifest已有 `BLUETOOTH_SCAN`（`neverForLocation`）/ `BLUETOOTH_CONNECT` | 主动scan/connect时请求；具体调用仍窄捕获`SecurityException` | E17-6 / 7 AVD |
| Android 11及以下 | `ACCESS_FINE_LOCATION maxSdkVersion=30`；debug另有legacy BLE | 保留main兼容permission；不称为定位能力 | E17-7 static |
| FGS base | 当前缺失 | 加 `android.permission.FOREGROUND_SERVICE` | E17-9 Manifest test |
| Android 14+ connected device | 当前缺失 | 加 `FOREGROUND_SERVICE_CONNECTED_DEVICE`；不改为`health` | E17-9 Manifest / AVD |
| Service type | 当前无Service | 唯一 concrete Service 声明 `android:foregroundServiceType="connectedDevice"` | E17-9 static / AVD |
| runtime prerequisite | 已有BLE runtime permissions | FGS运行前至少满足官方connected-device前置；本路径依赖已授予的BLE权限 | E17-9 reducer / AVD |
| start timing | 当前无FGS | 仅从App明确可见前台、active workout + existing connection成立时调用`startForegroundService()`；不等ON_STOP | E17-9 AVD |
| promotion | 当前无Service | `onStartCommand()`无App人为延时，立即 `ServiceCompat.startForeground(7200, ..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)` | E17-9 Android test / AVD |
| notification deny | ordinary当前Ignored并clear | ordinary可不发；FGS仍构造并提交，系统可能只在Task Manager显示 | E17-9 tests / AVD allow+deny |
| return mode | 当前无Service | `START_NOT_STICKY` | E17-9 test |
| background start | 当前无Service | 不依赖例外；后台启动失败记录准确fact并cleanup策略 | E17-9 AVD |
| Task Manager stop | 无Service | 尊重用户停止，不自动重启/重连 | E17-10真机观察 |
| process death | Compose冷启动只恢复preference | 不依赖finally/onDestroy；新进程无old bpm，Service不sticky | E17-9/10 AVD |
| cleanup / return foreground | 当前非训练后台无明确cleanup | 无自动scan/connect/reconnect；manual recovery | E17-7/9 AVD + Band |

### 9.1 E17-7 process visibility reducer 合同

E17-7 必须实现一个全部输入与状态转换都串行到 Android main looper 的进程可见性 reducer；只写“使用 `ActivityLifecycleCallbacks`”不构成验收。实现不要求创建同名 Kotlin enum，但必须能表达等价的 `ForegroundConfirmed`、`BackgroundConfirmed`、`ConfigurationTransition` 与 `Unknown` 四类概念 fact，且不引入第三方 lifecycle framework。`ProcessVisibilityTracker` 只发布 visibility fact：不得持有 heart-rate owner、不得直接调用 `BackgroundCleanup`，也不得决定训练或 FGS eligibility。

输入与计数规则固定如下：

- 使用 Activity object identity 集合跟踪 started Activity，而不是单个布尔值或可为负的计数器。resumed Activity identity 集合可用于诊断与测试，但 terminal 是否保留连接以 started visibility 和明确 transition fact 为准。
- 所有 lifecycle callback 先投递到 main looper，再修改集合、generation 或发布 fact。重复、迟到、无法匹配、不平衡或无法解释的 callback 一律进入 `Unknown`，不得猜测前台。
- 至少一个 Activity 已 started 时为 `ForegroundConfirmed`；普通 paused 但仍 started 的 Activity 继续属于明确前台。
- 最后一个 started Activity 在非 configuration-change 条件下停止时为 `BackgroundConfirmed`。Home、真实 `ON_STOP`、screen off 与 lockscreen 都必须进入后台路径。
- Route 是否存在、Compose 是否仍在 composition、通知是否存在均不能替代进程可见性 fact。

Configuration change 使用受控 generation：最后一个 Activity 以 `isChangingConfigurations=true` 停止时进入 `ConfigurationTransition`，记录 transition generation；replacement Activity start 只有匹配当前 generation 才恢复 `ForegroundConfirmed`。generation mismatch、重复 replacement、超出受控 transition或其他无法解释事件进入 `Unknown`。Tracker 必须定义确定性的 main-queue transition completion、timeout或失效门禁，不能无限等待 replacement；正常 configuration change 必须被确定性表达，不能被误判为真实后台。

cleanup 决策属于 Application policy，而不是 Tracker。E17-7 的 Application policy 消费 visibility、opt-in、permission、Bluetooth 与 training facts；在 E17-9 尚未合并时，对 `BackgroundConfirmed` / `Unknown` 向 owner 提交 cleanup，terminal 只有在 `ForegroundConfirmed` 或受控且 generation 匹配的 `ConfigurationTransition` 才可保留同一 eligible attempt。`Unknown`、竞态、集合异常、generation mismatch 与无法确认均 fail-closed；正常 configuration change 不得错误 cleanup。E17-9 只调整 Application / coordinator policy：只有合法 FGS 已建立且 training / connection eligibility 仍成立时才允许后台维持同一 attempt，不重新设计 Tracker。

E17-7 确定性测试矩阵必须覆盖：cold start、first Activity start、pause 但仍 started、Home / `ON_STOP`、screen off / lockscreen、configuration-change stop / start、replacement Activity 迟到或缺失、快速 Activity 切换、多 Activity 重叠、duplicate / unbalanced callback、terminal 分别发生在 foreground / background / configuration transition / unknown、Activity recreation 不创建第二 owner，以及 E17-9 前 active training background 必须 cleanup。

Android一手资料（核验日期2026-07-18）：

- [Connected device foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device)：当前仍要求`connectedDevice` type、`FOREGROUND_SERVICE_CONNECTED_DEVICE`、`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`，并以已授予的Bluetooth runtime permission等作为运行前置。
- [Launch a foreground service](https://developer.android.com/develop/background-work/services/fgs/launch) 与 [Troubleshoot foreground services](https://developer.android.com/develop/background-work/services/fgs/troubleshooting)：先`startForegroundService()`，Service须在数秒内`ServiceCompat.startForeground()`，否则可能触发`ForegroundServiceDidNotStartInTimeException`。
- [Background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)：Android 12+后台启动受限；不满足例外会抛`ForegroundServiceStartNotAllowedException`。本架构只从明确可见前台启动。
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)：Android 13+拒绝通知权限时，FGS notice仍可见于Task Manager，但不在notification drawer；因此拒绝不能跳过`startForeground` notification。
- [BLE background communication](https://developer.android.com/develop/connectivity/bluetooth/ble/background)：后台BLE仍要求进程存活，进程被杀连接关闭；需长期维持时可使用`connectedDevice` FGS，且仍受后台启动限制。
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)：Android 12+ scan/connect runtime permissions及API 30以下兼容边界与当前Manifest一致。
- [`BluetoothDevice`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice) 与 [`BluetoothGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)：API 26–36可用带`Handler`的`connectGatt` overload；owner只实现实际使用的discover、notification、descriptor、disconnect/close路径。API 37 deprecation不改变当前target 36 Story，但后续升级target时需另行审计。

未发现官方当前要求与D-081 `connectedDevice`选择冲突。

## Historical 10. Notification ID `7200` 单一 writer 验收矩阵（snapshot；non-operative）

Route只提交最新训练状态；唯一 Application-scoped `ActiveWorkoutNotificationController` production coordinator持有mode与latest state。业务身份使用真实 workout session ID，不使用 plan-derived key；coordinator另行签发 producer `submissionGeneration` / `routeToken`，notification state携带同一 session 内严格单调的`stateVersion`。Service不是第二业务owner，也不是GATT owner；Service只有在coordinator明确交接后对同一ID调用`startForeground`。

| 输入 / 迁移 | 唯一有权动作 | 结果 / 必测断言 |
|---|---|---|
| `NONE -> ORDINARY` | coordinator notify | 仅一条7200；permission deny则NONE但训练继续 |
| `ORDINARY -> FGS` | coordinator锁writer并启动Service；Service接管7200 | 无第二条；Service立即foreground；FGS copy准确 |
| `FGS -> ORDINARY` | Service先demote/stop并ack交还；coordinator再notify | 交接顺序固定；同ID；无间歇双writer |
| `FGS -> NONE` | Service以`STOP_FOREGROUND_REMOVE`移除7200并ack；coordinator只记NONE | terminal/cleanup幂等；最终移除只一次 |
| `ORDINARY -> NONE` | coordinator cancel一次 | terminal最终移除 |
| 重复terminal | coordinator去重 | 最终移除只一次 |
| 同一plan session A -> B | B的真实session ID成为current | A的迟到submit / terminal不能覆盖或清除B |
| 旧Route迟到submit | session ID、token或`stateVersion`不匹配 | 无副作用；不能回退latest state |
| Route dispose / detach | Route不cancel；进入有限、确定性、test-controlled bounded detach | active notification保持；匹配reattach或受控replacement前不把dispose当terminal |
| configuration recreation | 匹配session/token reattach，或为同session签发replacement token并原子失效旧token | replacement继承version floor；Activity/Route重建不清理当前通知 |
| detach timeout / mismatch | fail-closed | ID `7200`最多clear一次；不得无限等待或猜测归属 |
| process recreation | 不恢复旧内存session/token/version | 无可恢复active-training事实时幂等清理旧`7200`一次 |
| Service stop / 重复callback | coordinator按mode与latest state幂等接收ack；必要时转ordinary或NONE | 不二次terminal、不重发旧state |
| ordinary `POST_NOTIFICATIONS`拒绝 | coordinator不notify；若旧ordinary仍存在则cancel一次 | 不阻塞训练；不进入FGS错误 |
| FGS路径 `POST_NOTIFICATIONS`拒绝 | 仍build并submit FGS notification | 不走ordinary Ignored分支 |
| FGS启动/提升失败 | coordinator记录failure并按latest训练状态恢复ordinary或NONE；前台可保留连接，随后后台必须cleanup | 不创建第二Service/GATT owner，不宣称后台保证 |
| 训练继续、心率停止 | `FGS -> ORDINARY` | 训练状态最新；HR停止不终止训练 |
| 前台terminal且连接eligible | `FGS -> NONE`，owner保留同一attempt | 只显示不记录；不是reconnect |
| 后台terminal | `FGS -> NONE` + owner cleanup | 回前台不自动恢复 |

E17-9的固定基线是：Service先以`STOP_FOREGROUND_REMOVE`移除自身的7200并ack；目标为ordinary时，coordinator只在ack后按latest state重发同一ID，目标为NONE时不再调用第二次cancel。允许短暂无通知窗口，不允许双writer或两条常驻通知。若Android实测证明该顺序无法满足平台合法性或用户可观察验收，E17-9必须停止并返回主管理复核；不得在implementation内自行切换detach handoff或新增协调抽象。

### 10.1 `ForegroundWriterReleased` 进程内 ack 协议

这里的 ack 只表示：Service 在 main-looper 串行路径中调用 `stopForeground(STOP_FOREGROUND_REMOVE)`，并且该调用正常返回后，向 Application coordinator 发布 `ForegroundWriterReleased(generation)` 进程内协议事实。它不是 Android framework 提供的系统 UI 通知移除确认，不是 `NotificationManager` callback，不是用户已经看不到通知的证据，也不是 binder 或 System UI completion event。实现和证据不得把该进程内 release 事实描述成系统 UI 删除成功。

概念 handoff 状态至少携带：`handoffGeneration`、`desiredTargetMode`、latest workout notification state / version，以及当前 FGS writer generation。`handoffGeneration`只标识ordinary / FGS writer交接，与E17-8的workout session ID、`submissionGeneration` / `routeToken`和`stateVersion`是不同概念，禁止复用。降级顺序固定如下：

1. coordinator 创建新的 handoff generation，保存 latest state / version，并冻结 ordinary writer 发布权。
2. coordinator 向当前 Service writer 发出 demote / stop 请求；Service 只接受匹配当前 FGS writer generation 的请求。
3. Service 在 main-looper 串行路径调用 `stopForeground(STOP_FOREGROUND_REMOVE)`。
4. 只有该调用正常返回后，Service 才发布 `ForegroundWriterReleased(generation)` 进程内事实。
5. coordinator 只接受当前 pending handoff generation 的 release；stale、duplicate 或 generation mismatch ack 全部忽略。
6. 若 latest desired mode 仍为 ordinary，coordinator 取得 writer 权并只用最新训练状态重新发布同一 ID `7200`；若 latest desired mode 为 `NONE`，不发布 ordinary，也不再调用第二次 cancel。
7. handoff 期间 desired mode 再次变为 FGS 时必须创建新 generation，不得复用旧 ack 或旧 writer generation。

handoff 期间训练状态变化只更新 latest state / version，取得 writer 权后只 replay 最新状态。desired mode 从 ordinary 改为 `NONE` 时不发布 ordinary。terminal 重复、Service stop 重复、Route dispose 和 cleanup 都必须幂等，且不得形成 concurrent Service / ordinary writer。

失败语义固定为内部 `ReleaseUnconfirmed` 或等价稳定态：`stopForeground(STOP_FOREGROUND_REMOVE)` 抛出、未正常返回或 Service destroyed-before-ack 时不得发布 release ack；ordinary writer继续冻结，失败路径不得创建第二writer、不得宣称系统UI已移除通知，也不得复用旧ack。此时若visibility进入`BackgroundConfirmed` / `Unknown`，Application policy必须cleanup BLE且不宣称后台保证；terminal仍幂等。process death不恢复旧writer generation、handoff、workout producer generation或Live；新进程无可恢复active-training事实时只幂等清理旧`7200`一次。回到前台仍不自动scan / connect / reconnect。

### 10.2 E17-9 handoff 必测矩阵

E17-9 的 pure / Android / AVD 计划必须覆盖 normal `FGS -> ORDINARY`、`FGS -> NONE`、stale ack、duplicate ack、wrong generation、latest-state replay、handoff 中 target mode 改变、`stopForeground` failure injection、`ReleaseUnconfirmed`、Service destroyed before ack、background / unknown cleanup、repeated terminal，以及 no double notify / cancel。该协议不授权新增 notification 核心 interface。

## Historical 11. 风险隔离后的 implementation Story 序列（snapshot；non-operative）

每个后续Story只有在前一个Story独立Review / merge / push完成、其immutable full SHA成为`main` ancestor、`main...origin/main = 0 0`且权威文档一致时才解锁。分支名不是解锁事实；不增加状态docs-sync或递归closeout。

### E17-5 Heart-rate Fact / Freshness / Presentation Core

- **状态：** `reviewed / merged`；immutable SHA `959146a7e41a38d654b4988ba0d443f2aea0d874`；merge commit `bfb065b92d2ec78ca794fa679f7e25e85093bc79`。
- **唯一主要风险轴：** public facts、malformed / freshness与冻结presentation兼容。
- **允许范围：** `HeartRateState.kt`、`HeartRateBoundary.kt`、`BleHeartRateProviderBoundary.kt`、`HeartRateFreshnessPolicy.kt`、`HeartRateFloatingCapsuleState.kt`；上述pure tests；debug `E17Band9HrsRevalidationActivity.kt`仅允许添加monotonic measurement字段；新增该Story testing/evidence文档。
- **禁止：** scanner/GATT owner、Application wiring、Route、Service、Manifest、notification、Room、记录、reconnect、胶囊本体/geometry。
- **历史前置：** 已满足；E17-4 immutable SHA已是同步`main` ancestor。
- **Acceptance：** M0证据成立；状态矩阵完整；malformed不failure/不续命；旧bpm不live；presentation不改视觉DTO；不复制旧数字。
- **Evidence：** pure Kotlin + `.local/smoke/e17-5-heart-rate-fact-core/` Band 9前台间隔；修改 debug 测量 Activity 后重新 build / install，按第 8.1 节记录 source preparation SHA、APK path / variant / applicationId / Activity / SHA256、build / install time、设备与分层日志；独立Review交叉核验APK身份并锁provisional阈值。
- **Rollback / disabled：** 仅core未接新owner；回滚不影响现有训练；默认opt-in仍false。

### E17-6 Deterministic Android BLE Runtime Owner

- **状态：** `reviewed / merged`；immutable SHA `f9188c09275cd01dbf182823b3886635b17105bc`；merge commit `503d3151d731565837ab76f44fbebc25bb982e0d`。
- **唯一主要风险轴：** scanner/GATT/callback identity、main-queue serialization与cleanup确定性。
- **允许范围：** 新增并完成`HeartRateRuntimeOwner.kt`、确定性 owner/callback/platform tests，adapt permission planner，parser只消费不修改；实现 main queue、generation、attempt ID、raw GATT identity、具体 BLE 调用、TOCTOU 与 cleanup。新 owner 只在测试中实例化。旧`AndroidBleHeartRateProvider`、`HeartRateDeviceScanner`与旧 DTO 暂留，仅维持现有 production / debug 接线编译。
- **禁止：** Application/settings/capsule wiring、notification/Route/Service/Manifest、FGS、Room、reconnect/scheduler、`AndroidBleOperations` seam；禁止 production composition root、Activity、Compose、settings、debug launcher取得或实例化新owner；禁止自动启动、Service入口或隐式singleton getter；禁止在本Story宣称旧runtime已删除或retire。
- **历史前置：** 已满足；E17-5 immutable SHA已是同步`main` ancestor。
- **Acceptance：** main queue；generation/attempt/raw GATT首次绑定；callback早于返回；old callback/target/generation拒绝；permission TOCTOU；具体BLE调用；cleanup顺序/幂等；intentional stop不伪造disconnect；unknown exception继续抛。E17-6合并后旧runtime仍是唯一production可达BLE owner；新owner默认不可从production composition root取得。测试实例化新owner不等于production双owner；新旧源码可暂时共存，但不得在同一production进程中同时实例化、扫描或持有GATT。
- **Evidence：** owner确定性tests + Android平台tests；AVD只做permission/Bluetooth/no-crash，不声称RF。
- **Rollback / disabled：** owner尚未接production UI；E17-6切换前仍可编译，`HeartRateBroadcastSmokeActivity.kt`继续通过旧provider编译。旧runtime的production retirement只属于E17-7原子切换。

### E17-7 Application / Settings / Capsule Production Wiring

- **唯一主要风险轴：** composition root、process visibility、manual user flow与跨页面同owner接线。
- **状态：** 按第14节Planning Repair / E17-7统一条件式真值自动判定。
- **允许范围：** 仅限下列原子切换文件、直接legacy tests与验证动作，不得把“原子切换”解释为可修改任意health文件：
  - production composition与consumer接线：`app/src/main/java/com/liujyks/trainflow/app/TrainFlowApplication.kt`创建唯一新owner；`app/src/main/java/com/liujyks/trainflow/app/MainActivity.kt`与`app/src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt`切换到该owner；`app/src/main/java/com/liujyks/trainflow/feature/settings/TrainingPreferencesUiState.kt`、`app/src/main/java/com/liujyks/trainflow/feature/settings/SettingsRoute.kt`与`app/src/main/java/com/liujyks/trainflow/app/TrainingPreferencesAppMapper.kt`迁移settings、manual scan/connect与saved-device consumer；`app/src/main/java/com/liujyks/trainflow/core/datastore/TrainFlowPreferences.kt`、`app/src/main/java/com/liujyks/trainflow/core/datastore/TrainFlowPreferenceKeys.kt`与`app/src/main/java/com/liujyks/trainflow/core/datastore/TrainFlowPreferencesDataSource.kt`只作DataStore兼容接线；`app/src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsuleState.kt`及其`TrainFlowApp.kt`接线迁移capsule presentation mapper。上述文件还明确承载Activity recreation / page navigation不重建owner、离开settings只停scan、cold start只恢复saved hint、opt-out / permission loss / Bluetooth off / non-training background cleanup、E17-9前active training background cleanup，以及主动permission / scan / saved exact match / manual select / connect动作。该授权只用于把E17-6已经规划的新owner接入E17-7 production composition。
  - process visibility与唯一owner验证：新增窄`app/src/main/java/com/liujyks/trainflow/app/ProcessVisibilityTracker.kt`（平台`ActivityLifecycleCallbacks`），以及`app/src/test/java/com/liujyks/trainflow/app/ProcessVisibilityTrackerTest.kt`和`app/src/test/java/com/liujyks/trainflow/app/TrainFlowApplicationHeartRateOwnerTest.kt`，分别覆盖第9.1节reducer全矩阵与Application唯一owner / recreation / navigation不重建行为。
  - 旧runtime原子退休：E17-7可以修改、迁移、缩减或删除`app/src/main/java/com/liujyks/trainflow/core/health/AndroidBleHeartRateProvider.kt`、`app/src/main/java/com/liujyks/trainflow/core/health/HeartRateDeviceScanner.kt`与`app/src/main/java/com/liujyks/trainflow/core/health/BleHeartRateProviderBoundary.kt`，以删除旧scanner/provider资源ownership路径与旧DTO production consumer。`BleHeartRateProviderBoundary.kt`不得整文件盲删；当前`HeartRateRuntimeOwner.kt`实际依赖的精确保留清单为`BleHeartRateScanState`、`BleHeartRateScanStateKind`、`BleHeartRateDeviceCandidate`、`BleHeartRateRecoverableReason`，其中新owner只消费`SCAN_FAILED`。四类只允许表达candidate、scan或无资源reason fact，不持有scanner、`BluetoothGatt`、callback、attempt或Android平台对象，不创建BLE资源、不成为第二owner、不包含旧provider lifecycle，也不把saved identifier当connected或永久设备身份。必须退休`BleHeartRateDeviceSelection`、`BleHeartRateProviderState`、`BleHeartRateProviderStateKind`、`providerStateAfterAvailabilityRefresh`、旧provider mapper及旧runtime reason/consumer；`BleHeartRateRecoverableReason`随旧runtime退休收缩到仍有consumer的scan/no-resource语义。若迁移四个纯类型要求修改`HeartRateRuntimeOwner.kt`或三个E17-6 tests，立即停止回主管理。
  - debug consumer：默认保留`app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`类、debug Manifest声明与`DebugEntryActivity`入口，只移除其对`AndroidBleHeartRateProvider`的直接实例化。Activity取得同一Application owner或降级为明确“不持有BLE资源”的说明/观察页；不得创建scanner、GATT、callback、attempt或第二owner，不做视觉重设计、固定控制栏、日志导出或M1工具。默认无需修改debug Manifest、`DebugEntryActivity.kt`或`HeartRatePermissionBoundaryTest.kt`；若实现者认为必须删除整个Activity，停止并返回主管理扩展显式文件授权。
  - direct legacy tests：仅因上述旧runtime / DTO切换造成的直接编译或合同迁移，E17-7可以迁移、重写或删除`app/src/test/java/com/liujyks/trainflow/core/health/AndroidBleHeartRateProviderScanFilterTest.kt`、`app/src/test/java/com/liujyks/trainflow/core/health/BleHeartRateProviderBoundaryTest.kt`、`app/src/test/java/com/liujyks/trainflow/feature/settings/TrainingPreferencesUiStateTest.kt`、`app/src/test/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsuleStateTest.kt`与`app/src/test/java/com/liujyks/trainflow/ui/shell/official/HeartRateLiveProviderLifecycleTest.kt`。不得扩张为全量heart-rate test重写；parser、freshness核心、geometry、notification、FGS与Room tests不因本Repair进入E17-7范围。删除源码字符串搜索型旧测试时，必须以新owner / Application行为测试替代，不得简单减少coverage；presentation与settings测试必须继续保护saved hint不等于connected、旧bpm不进入Live、胶囊视觉DTO不被BLE对象污染等合同。
  - envelope：production允许文件精确为14（上述10个composition/consumer文件、`ProcessVisibilityTracker.kt`、旧provider/scanner/boundary三个文件）；debug允许文件1；可修改tests精确为7（两个新增Application/visibility tests + 五个direct legacy tests），合计源码授权22。不得继续使用“production 7–10文件”估算。
  - 必须运行、默认不得修改的回归tests：`core/datastore/TrainFlowPreferencesBoundaryTest.kt`、`app/TrainingPreferencesAppMapperTest.kt`、`ui/shell/official/TrainFlowAppPermissionResultTest.kt`、`feature/workoutsession/HeartRatePermissionBoundaryTest.kt`（同时覆盖debug Manifest）、`feature/settings/HeartRateSettingsActionVisualBoundaryTest.kt`、`core/health/HeartRateMeasurementParserTest.kt`、`core/health/HeartRateFreshnessPolicyTest.kt`、`core/health/HeartRateProviderBoundaryTest.kt`、`core/health/HeartRateRuntimeOwnerTest.kt`、`core/health/HeartRateRuntimeOwnerCallbackIdentityTest.kt`、`core/health/HeartRateRuntimeOwnerPlatformTest.kt`、`ui/shell/official/HeartRateCapsuleGeometryTest.kt`。若正确合同要求修改任一run-only test，停止回主管理。
  - 验证动作：允许执行Application / owner tests、process visibility reducer tests、第12.3节production唯一owner创建点静态搜索、第12.4节E17-7适用AVD矩阵，以及第12.5节Band 9 basic gate；Band 9 basic gate未通过不得merge。
- **禁止：** FGS/Service/notification ownership、Route notification代码、Room/session HR、自动恢复、胶囊visual/geometry。
- **前置：** E17-6已reviewed/merged；E17-7 planning prerequisite按第14节统一条件式真值自动判定。
- **Acceptance：** 在同一Story原子完成：`TrainFlowApplication`创建唯一新owner；`MainActivity`、`TrainFlowApp`、settings、manual scan/connect、saved-device与capsule mapper全部切换；迁移/退休debug smoke旧provider引用；删除旧`HeartRateDeviceScanner`资源ownership路径；删除旧`AndroidBleHeartRateProvider`可实例化runtime路径，或只保留不持有scanner/GATT的纯compatibility数据映射；移除旧BLE DTO的production consumer；通过`rg`与测试证明production composition只有一个owner创建点。Activity recreation/page navigation不重建；离开settings只停scan；cold start只恢复saved hint；opt-out/permission loss/Bluetooth off/non-training backgroundcleanup；主动permission/scan/saved exact match/manual select/connect；active training background在FGS未实现时也cleanup且不宣称保证。E17-7合并后不得保留可重新实例化旧scanner/GATT owner的production入口。
- **Evidence：** pure/UI mapping、Application/platform tests、第9.1节visibility reducer全矩阵、`rg`唯一owner创建点、AVD grant/deny/revoke/navigation/Home/screen-off/lockscreen/process recreation；**Band 9 basic gate必须通过后才merge**。E17-6切换前与E17-7切换后都必须可编译。
- **Rollback / disabled：** default off；若Band gate失败不merge；原子切换不得落在新旧production owner并存的中间态，也不得保留假“后台持续”copy。

E17-7仍是一个不可拆分合入`main`的原子Story，但同一分支内必须按三个阶段提交：

1. **阶段A：纯准备。** 完成fact-only process visibility reducer及tests、四个纯compatibility类型的收缩准备、Application owner test骨架；不改变production owner可达性，旧runtime仍是唯一production owner。
2. **阶段B：原子composition切换。** 在同一提交由`TrainFlowApplication`创建唯一新owner，并切换`MainActivity`、`TrainFlowApp`、settings、manual scan/connect、saved device与capsule mapper；不得留下新旧owner同时production可达，Activity recreation与page navigation不得重建owner。
3. **阶段C：退休与证据。** 删除/缩减旧provider、scanner与旧DTO surface，迁移debug Activity，执行静态唯一owner门禁、full tests、AVD与Band 9 production basic gate；实现记录必须逐项列出删除的旧类型、保留的四个纯类型、每个consumer、为何不是runtime owner及后续删除责任。

三个阶段不得分别合入`main`，最终作为同一E17-7 Story独立Review和原子合并。出现以下任一情况立即停止回主管理：需要修改`HeartRateRuntimeOwner.kt`；需要修改三个E17-6 test文件；需要第二owner或新BLE seam/wrapper/adapter；普通configuration change进入`Unknown`或错误cleanup；需要未授权production/debug/test文件；Band 9 basic gate失败；或production双owner中间态无法在同一提交闭合。

### E17-8 Application-scoped Ordinary Notification Coordinator

- **唯一主要风险轴：** 先消除三个Route业务writer，建立ID7200 ordinary单writer与terminal幂等。
- **允许范围：** active notification contracts/controller/mapper、TrainFlowApplication/MainActivity窄composition接线、三Workout Route的state submission与tests。
- **禁止：** Service/Manifest/FGS、BLE owner、胶囊、Room、notification core新interface。
- **前置：** E17-7 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **身份合同：** notification业务身份必须使用真实workout session ID（直接来自各Route的`engineState.sessionId`）；plan ID只可作显示或分组信息，不能作为当前训练实例唯一身份。coordinator为每个producer/Route签发`submissionGeneration`或等价`routeToken`；所有submit、terminal、detach与reattach必须同时匹配workout session ID + token。notification state带同一session内严格单调、跨受控reattach不回退的`stateVersion`；replacement token继承version floor，只有`version > lastAcceptedVersion`才可更新。
- **生命周期合同：** session B成为current后，同一plan的旧session A迟到submit/terminal无副作用；旧Route不能覆盖新session。Route dispose不cancel，只进入由有限、确定性、test-controlled常量锁定的bounded detach；configuration recreation可使用匹配session/token reattach，或为同session受控签发replacement token并原子失效旧token。detach timeout、mismatch或无法确认时fail-closed并最多clear `7200`一次。process death不恢复旧内存session/token/version；新进程没有可恢复active-training事实时只幂等清理旧`7200`一次。
- **Acceptance：** 唯一Application production instance；Route只submit；active/paused ordinary行为保持；duplicate terminal最终一次；permission denied ordinary可不发；三Route快速切换不产生旧writer；不得新增notification core interface，只使用现有contracts/controller/mapper与Route范围。E17-9的`handoffGeneration`与本Story的workout session producer generation是两个概念，禁止复用。
- **Evidence：** pure coordinator transition tests、Android notification port tests与AVD必须覆盖same plan session A -> B、old terminal after new session、old Route late submit、duplicate/out-of-order `stateVersion`、detach/reattach、controlled replacement、configuration recreation、process recreation / cancel once、duplicate terminal，以及timed -> strength -> follow-along三Route快速切换。
- **Rollback / disabled：** ordinary notification是既有真实能力；失败可回滚此Story而不触碰BLE owner，不能merge双writer过渡态。

### E17-9 Connected-device FGS And ID 7200 Handoff

- **唯一主要风险轴：** Android connected-device FGS合法启动、Service lifecycle与ordinary/FGS单writerhandoff。
- **允许范围：** 新`ActiveWorkoutHeartRateService.kt`、Manifest、active notification contracts/controller、Application/coordinator policy与必要first-party AndroidX direct dependency声明、Service/platform tests、最终freshness阈值/test更新、该Story evidence doc；按第8.2节窄适配shared-owner debug observer、debug Manifest/entry与必要的debug-only source identity build field。
- **禁止：** 新GATT owner、Service持有scanner/GATT、background scan/connect/reconnect、notification core新interface、Room/record、胶囊visual。
- **visibility边界：** E17-9只调整Application/coordinator policy；只有合法FGS已经建立且training/connection eligibility成立时，后台才保留同一attempt。`ProcessVisibilityTracker.kt`默认不可修改，若必须修改tracker立即停止回主管理；不得让tracker持owner、调用cleanup或决定FGS eligibility。
- **前置：** E17-8 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **Acceptance：** 全7200矩阵与第10.1节进程内release协议；visible-start；immediate `ServiceCompat.startForeground`；connectedDevice type；ordinary vs FGS deny分支；`START_NOT_STICKY`；failure、process death、Task Manager、terminal visibility；stale/duplicate/wrong-generation ack、latest replay、handoff target变化、`ReleaseUnconfirmed`、Service destroyed-before-ack、repeated terminal与no double notify/cancel；`handoffGeneration`不复用workout producer generation；按第8.2节完成五阶段身份链后锁final freshness。
- **Evidence：** shared Application owner observer、pure coordinator、Android Service/Manifest、final-source AVD全handoff + **Band 9 active-training lockscreen/background gate**；M1、最终APK身份与两类gate未全过不merge。
- **Rollback / disabled：** 在M1/真机gate完成前不启用后台保证；失败保持E17-7前台manual能力与后台cleanup。零新增第三方依赖。

### E17-10 Integrated AVD / Band 9 Production Acceptance

- **性质：** evidence-only acceptance，不是implementation Story。
- **唯一主要风险轴：** 已合并组件的端到端证据与发布资格，不再设计或修改owner/GATT/FGS/production合同。
- **允许范围：** testing/evidence文档；`.local`中的AVD、Band 9、APK、日志、截图和设备证据；不改变production行为的test fixture或debug evidence harness修正；必要的测试断言修正，但不得改变被验收production合同。
- **禁止：** 任何`app/src/main`行为变化，包括ownership、Application activation、BLE runtime、FGS、notification handoff、Manifest、final freshness threshold、presentation mapping、permission行为、cleanup、lifecycle与production activation constant；也禁止新抽象、wrapper、seam、reconnect、Room/record/analysis/export或UI redesign。
- **前置：** E17-9 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致；M1与final freshness已在E17-9合并前锁定。
- **Acceptance：** 本文第12节AVD与两个Band gate全通过；APK/build identity可追溯；无crash/ANR；未通过即结论failed/inconclusive。E17-1 feasibility与sealed E16 evidence均不能替代production acceptance。
- **Evidence：** `.local/smoke/e17-10-heart-rate-production-acceptance/` + committed conclusion doc。
- **Repair routing：** owner/GATT/callback问题返回E17-6或E17-7 scoped Repair；Application/settings/capsule问题返回E17-7 Repair；ordinary notification问题返回E17-8 Repair；FGS/ID`7200`/final freshness问题返回E17-9 Repair。Repair必须独立Review、merge/push并成为同步`main` ancestor，随后重新build APK并重跑全部受影响的AVD/Band 9 gate；旧APK、旧截图与旧日志不得继续作为修复后证据。
- **Fixture / harness规则：** 必须证明变更不改变production行为；重新build受影响APK，记录新source SHA与APK SHA256，并重跑全部受影响evidence；不得用fixture修改掩盖production缺陷。
- **Rollback / disabled：** 未通过不宣称production ready；功能默认off保持核心训练可用。E17-10本身不修production，必须返回对应前置Story Repair。

六个Story没有把owner、GATT、FGS与真机acceptance重新塞进同一变更：E17-5隔离事实/时间；E17-6隔离平台runtime；E17-7隔离composition/user wiring；E17-8先消除notification多writer；E17-9只做FGS/handoff并在同Story锁必要锁屏threshold；E17-10只做端到端证据。这比机械五段多一个ordinary coordinator收口，避免E16-10b-2把owner、callback、scheduler、FGS与验收同时扩张。

## Historical 12. 测试与证据计划（snapshot；non-operative）

### 12.1 纯 Kotlin

- parser 8 / 16-bit、flags、empty/truncated malformed。
- runtime fact -> public state；disabled、permission、Bluetooth、not connected、scanning、connecting、waiting、live、interrupted、disconnect、failure、intentional stop。
- provisional/final freshness exact boundaries；malformed不刷新；旧bpm不进入新Live；invalid monotonic fail-closed。
- presentation mapper文案、区间/上限visual-only与冻结DTO兼容。
- E17-8 notification identity reducer：same-plan session A -> B、old terminal after new session、old Route late submit、duplicate/out-of-order `stateVersion`、bounded detach/reattach、controlled replacement、duplicate terminal与三Route快速切换。

### 12.2 Owner 确定性

- main queue串行；generation与attempt ID。
- callback早于`connectGatt()`返回；raw GATT首次绑定与mismatch。
- old target / callback / generation；permission TOCTOU。
- 先失效引用、取消任务、快照资源、stop/disconnect/close；幂等。
- intentional stop不发布disconnect；canceled closure不能resurrect。

### 12.3 Android 平台

- 只测production实际调用：filtered `startScan/stopScan`、handler `connectGatt`、discover、HRS/CCCD、notification/descriptor、disconnect/close。
- API26–30与31+permission/Manifest分支；unknown exception继续抛。
- Application唯一owner；Activity recreation不重建。
- E17-8 configuration/process recreation：匹配session/token reattach、replacement version floor、新进程无active fact时cancel `7200` once。
- Service/notification contract、ID7200全部handoff、`ReleaseUnconfirmed`与permission分支。
- source search / Manifest test只作为静态补充，不冒充行为证据。

### 12.4 AVD

固定环境：SDK `.local/android-sdk`；AVD `TrainFlow_Pixel_API_36`；每Story证据 `.local/smoke/<Story ID>/`。

覆盖permission grant/deny/revoke、Bluetooth off/on、Home/ON_STOP、screen off/on、ordinary/FGS content、ID7200单writer、same-plan session replacement、三Route快速切换、upgrade/downgrade/terminal、Route bounded detach/reattach、configuration recreation、process recreation无old bpm且旧7200幂等清理一次、no-crash/no-ANR。AVD不能证明RF、GATT、CCCD、notify或Band 9。

### 12.5 Band 9 gate 1：production基本链路（E17-7）

- 用户确认广播开启条件与Huawei Health互斥说明。
- 主动HRS scan；saved exact match失败可manual choice；不按name自动选。
- connect/discover `0x180D`；notify `0x2A37`；descriptor `0x2902`；CCCD成功；连续valid notify；parser bpm。
- manual stop；cleanup后无自动恢复；关闭广播后Huawei Health观察按可见事实记录。

### 12.6 Band 9 gate 2：active workout锁屏/后台（E17-9）

- 已连接后开始training；锁屏/临时后台持续valid notify且仍同一attempt。
- 单一ID7200；notification permission允许/拒绝的可观察边界；回前台。
- 心率停止而训练继续时降级ordinary；前台terminal保留同一eligible attempt；后台/不确定terminal cleanup。
- opt-out、permission loss、Bluetooth off；无自动reconnect。

E17-1只作设备/协议feasibility参考，不能替代两个新gate。

## Historical 13. 文件、容量与依赖预算（snapshot；non-operative）

实际627行旧provider、339行presentation mapper、三个Route writer与新增Service表明D-081中的“production Kotlin 450–750行、20–30方法、APK <50KiB”作为**全序列**估算不可信。已完成Story使用实测事实，未完成Story只保留方向性预算：

| Story | production | tests | debug / evidence | 状态与成本边界 | APK验证 |
|---|---|---|---|---|---|
| E17-5 | **实际5文件**；churn约`+571/-291`，net `+280` | **实际5文件**；churn约`+707/-679`，net `+28` | **实际1文件**；churn约`+238/-9`，net `+229` | 已完成事实；净增长与总churn分开，不以net掩盖重写成本 | 历史M0/Repair APK身份按Story文档分层 |
| E17-6 | **实际1文件**；`1224` physical LOC；`54` methods | **实际3文件**；约`1742` LOC；focused tests `45` | 0 | 已完成事实；新增interface/seam/wrapper/adapter/scheduler/actor/dependency均为0 | build identity不是production reachability或真机证据 |
| E17-7 | **允许14文件** | **可修改7文件**，另有12个run-only tests | **允许1文件** | 固定14/1/7 envelope，合计源码授权22；行数只作弱估算 | release APK + `apkanalyzer` package delta |
| E17-8 | 预计6–8文件 | 预计6–9文件 | 0 | session/token/version、bounded detach/reattach与三Route矩阵增加测试成本 | notification package delta只作方向参考 |
| E17-9 | 预计4–7 production文件 | 预计6–10文件 | shared-owner observer默认最多3个debug文件；五阶段双APK与重复设备证据 | observer、M1、final threshold、重建与重跑均计入Story成本 | measurement与final APK分别记录身份；只认final release验证 |
| E17-10 | **production files 0；lines 0；methods 0** | 预计0–3文件 | evidence-only | fixture/debug harness修正不得改变production行为 | 最终release APK、SHA256、`apkanalyzer`复核 |

E17-5实际总churn为production `862`、tests `1386`、debug `247` 行，不能与各自net增长混写。E17-7至E17-9的源码行数和APK变化只能作为弱估算；旧provider、旧DTO与Route writer会被删除、退休或重写。规划净release APK `30–120 KiB`只保留为未验证方向性估算，最终只能由同toolchain的release APK、SHA256与`apkanalyzer dex packages`验证；不得用debug APK简单差值证明release预算。

不引入厂商SDK、BLE library、DI library、scheduler、通用framework或新第三方依赖。若`ServiceCompat`需要停止依赖transitive classpath，E17-9可把现有AndroidX Core作为直接first-party坐标声明；它不计第三方依赖，但必须记录版本和dependency delta。全序列最多一个runtime owner、**零默认BLE seam**、一个concrete Service，不新增notification core interface。

## Historical 14. Readiness 通过标准与稳定状态（snapshot；non-operative）

| 标准 | 结论 |
|---|---|
| D-080与D-081无矛盾 | Pass |
| 当前代码迁移清单完整 | Pass；已按真实627行provider、空Application、三Route writer校正 |
| 目标无双runtime owner / 双notification writer | Pass；E17-6新owner仅test可达且旧runtime继续唯一production owner，E17-7同Story原子切换/退休旧入口；E17-8收口ordinary writer，E17-9按generation ack交接7200 |
| optional seam明确 | Pass；默认不新增，无法实现则stop / management review |
| freshness无循环 | Pass；E17-5前台M0 `3000 / 2500 ms` -> E17-9 shared-owner runnable FGS M1 -> final lock与final APK重验 |
| Story按风险拆分 | Pass；E17-5至E17-10六段 |
| process visibility fail-closed | Pass；E17-7固定四类fact、Activity identity集合、configuration generation/失效门禁与确定性测试矩阵 |
| E17-5 M0 APK身份 | Pass；debug测量变更后必须重新build/install并记录source SHA与APK SHA256，任何后续可执行变化使旧证据失效 |
| E17-10无production修改 | Pass；evidence-only，production finding返回E17-6/7/8/9独立Repair并重跑受影响证据 |
| 每Story范围、验收、证据、禁止项 | Pass |
| AVD / Band 9 evidence层级准确 | Pass；两个Band gate独立，E17-1仅reference |
| 排除Room/记录/分析/导出/自动恢复 | Pass |
| 胶囊冻结资产不重做 | Pass |
| 未解决产品/架构选择 | None |

稳定状态真值：

- E17-4、E17-5、E17-6均为`reviewed / merged`；immutable SHA与merge commit见第1节，E17-4 readiness为`passed`。
- E17-6新`HeartRateRuntimeOwner`已完成独立Review，但production/debug实例化仍为0；当前App继续运行旧provider/scanner路径，等待E17-7原子退休。
- **Planning Repair / E17-7 统一条件式真值：** 若本Planning Repair immutable SHA尚未通过独立Review，或尚未完成`--no-ff` merge/push，或该SHA尚不是同步后的`main`与`origin/main` ancestor，或`main...origin/main`不为`0 0`，或七份权威文档不一致，则Planning Repair=`implemented / needs review`、E17-7 planning prerequisite=`not satisfied`、E17-7=`planned / prerequisite-gated`，只允许独立Review/Repair本Planning Repair，不得启动E17-7。全部条件满足后，Planning Repair自动为`reviewed / merged`、E17-7 planning prerequisite自动为`satisfied`；不需要额外docs-sync，不创建递归closeout，主管理从Git解析最终Repair SHA与merge事实后决定后续提示词。Git ancestry是merge事实；branch name仅为locator，不是merge事实。
- 已完成Story的immutable SHA与merge commit是稳定历史事实；未完成Story只写条件式门禁，不同时保留矛盾的无条件状态。Story开发期状态在合并后必须标为历史分支快照。
- 当前阶段由Git ancestry、`main...origin/main = 0 0`与`docs/project-status.md`的当前E17状态索引共同判定。后续Story不再创建独立状态docs-sync，并须在自身开发分支文档中使用合并后稳定的双条件表述。
