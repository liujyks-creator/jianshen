# E17-4 心率 Production Implementation Readiness

**状态判定：** 本 Story immutable SHA 尚非 `main` ancestor，或独立 Review、merge / push、`main...origin/main = 0 0`、五份权威文档一致性任一未完成时，E17-4 为 `implemented / needs review`，production implementation 继续 locked；全部门禁满足且本结论仍通过后，E17-4 自动视为 `reviewed / merged`，E17-5 gate 自动 satisfied

**日期：** 2026-07-18

**性质：** docs-only readiness；production 代码、测试、Manifest、Gradle、资源、prototype 与 sealed E16 archive 均未修改

## 1. Readiness 候选结论

**候选结论：ready for implementation review。** D-080 产品合同与 D-081 最小架构之间未发现矛盾；当前 production 代码迁移点、单 owner / 单 writer 收口、optional BLE seam、freshness 无循环门禁、Android 平台成本、六个风险隔离 Story、AVD / Band 9 evidence 层级及容量预算均已形成可执行计划。没有发现必须新增未经批准核心 ownership 层、修改 `docs/architecture.md` / decision log、重开 D-080 / D-081，或把 readiness 判为失败的待决产品选择。

本候选结论不等于 reviewed / merged，也不授权 production implementation。当前 Git 前置为：E17-2 immutable SHA `b50778c90cf0232b08b857fda32ba6605fbef224` 与 E17-3 immutable SHA `b09ed116558eb3537fc86985b9c39b96bbbca6ff` 均是 `main` ancestor；禁止 E16 SHA `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor；E17-4 开始时 `main == origin/main == 1e0a7a9cf0b118ca829a5843d066795b4420eb5f` 且 `main...origin/main = 0 0`。

## 2. 盘点基线与硬边界

- 实际工程为单 `app` module，`minSdk 26`、`compileSdk 36`、`targetSdk 36`；AGP `9.2.0`、Kotlin / Compose compiler `2.3.21`、Compose BOM `2026.05.00`、DataStore `1.2.1`、Room `2.8.4`、Robolectric `4.16.1`、AndroidX Test Core `1.7.0`。
- 当前 `TrainFlowApplication` 是 5 行空 Application；实际 database、repository、preferences、心率 scanner/provider 与通知 controller 均在 Activity / Compose / Route 层创建。D-081 要求把心率 owner 与通知 coordinator 移到 Application composition root，但不要求把全部 App 依赖同时重构。
- 当前 `AndroidBleHeartRateProvider.kt` 为 627 行，直接同时持有 scan、candidate、target、GATT、callback 与 DTO mapping；它不是 E17-3 预计中的轻量 adapter。后续按 replace 处理，不能在其上继续叠 owner、FGS 或 reconnect。
- 当前三个 workout Route 各自 `remember(AndroidActiveWorkoutNotificationController)`，各自 update，并在 `DisposableEffect` 中以 `ROUTE_DISPOSED` clear；虽然都写 ID `7200`，仍是三个业务 writer。D-081 要求先收口为唯一 Application-scoped coordinator。
- 当前 Manifest 已有 `POST_NOTIFICATIONS`、Android 12+ `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` 与 API 30 以下 `ACCESS_FINE_LOCATION`；没有 `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_CONNECTED_DEVICE` 或 Service。
- E17 不写 Room、`WorkoutSession` 心率样本、记录、统计、分析、导出、自动恢复、reconnect scheduler 或医疗逻辑；胶囊视觉、互动、geometry、motion 与 adopted HTML 不改。

## 3. 当前代码资产清单

除非表中写出完整路径，本节 production 路径均相对 `app/src/main/java/com/liujyks/trainflow/`，unit test 路径均相对 `app/src/test/java/com/liujyks/trainflow/`；Manifest 与 debug 工具使用表中明确的 `app/src/...` 路径。

### 3.1 Runtime、状态与 parser

| 路径 / 类 | 当前责任 | E17 分类 | Implementation Story | 迁移风险 |
|---|---|---|---|---|
| `core/health/HeartRateBoundary.kt` / `HeartRateProvider` | 单一 `Flow<HeartRateState>` provider 边界；另含 disabled / mock provider | adopt as-is（fixture 语义 adapt） | E17-5 | 若另建 provider 会形成双抽象；禁止平行 provider |
| `core/health/AndroidBleHeartRateProvider.kt` | 627 行 scan + device map + selected address + 单 callback + GATT + CCCD + parser + provider mapping | replace | E17-6 | Compose owner、无 generation / attempt、只按 address 匹配、callback 非统一 main queue、早到 callback、malformed 立即 ERROR、宽 `runCatching` cleanup、旧 bpm 泄漏 |
| `core/health/BleHeartRateProviderBoundary.kt` | BLE candidate / selection / provider / scan DTO，message 与 public mapping 混合 | replace（candidate presentation 可窄 adapt） | E17-5 / E17-6 / E17-7 | 技术 message 成为用户状态输入；STALE / DISCONNECTED 均携带旧 bpm；STOPPED 映射成 NO_SOURCE |
| `core/health/HeartRateDeviceScanner.kt` / `AndroidHeartRateDeviceScanner` | 再包一层 provider，供 Compose 创建、关闭并直接操作 | isolate/retire | E17-6 / E17-7 | 保留会成为第二 runtime owner；不得由 settings / Compose 持有资源 |
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
| `core/notifications/ActiveWorkoutNotificationContracts.kt` | ordinary state/content/policy；ID `7200`；permission denied -> Ignored；clear reason含 Route disposed | adapt | E17-8 / E17-9 | FGS 不能复用 Ignored -> clear；ordinary copy 明确写“不是 FGS” |
| `core/notifications/AndroidActiveWorkoutNotifications.kt` / controller | `NotificationManager.notify/cancel(7200)`；Ignored 时 clear | adapt 为 Application-scoped coordinator production instance | E17-8 / E17-9 | 当前没有模式状态或 handoff；多个 Route 实例是多个 writer |
| `feature/workoutsession/ActiveWorkoutNotificationUiMapper.kt` | timed / strength / follow-along UI -> notification state | adopt mapping core，adapt submission ownership | E17-8 | mapper 不应持有 controller 或 Service |
| `feature/workoutsession/TimedWorkoutSessionRoute.kt` | Route 内创建 controller、update、dispose clear | adapt | E17-8 | 当前 writer 1；dispose 可误删仍 active 通知 |
| `feature/workoutsession/StrengthWorkoutSessionRoute.kt` | 同上 | adapt | E17-8 | 当前 writer 2 |
| `feature/workoutsession/FollowAlongWorkoutSessionRoute.kt` | 同上 | adapt | E17-8 | 当前 writer 3 |
| `core/notifications/ActiveWorkoutNotificationContractsTest.kt` | ordinary content、active / paused、terminal、permission denied | adapt + 保留 ordinary baseline | E17-8 / E17-9 | 当前断言“不是 FGS”只能适用于 ordinary content |
| `feature/workoutsession/ActiveWorkoutNotificationUiMapperTest.kt` | 三 route summary mapper | adopt as-is + submission tests | E17-8 | 不证明唯一 writer / Route dispose |
| `core/notifications/PlanReminderNotificationManifestBoundaryTest.kt` | 当前正向 BLE permissions，负向断言无 FGS / Service | replace相关负向断言 | E17-9 | D-081 已 supersede “全局无 FGS”；计划提醒边界仍保留 |
| `app/src/main/AndroidManifest.xml` | notification + scoped BLE permissions；Application / Activity / receiver | adapt | E17-9 | 缺 FGS permissions / service type；不得改为 `health` |
| `app/build.gradle.kts` / version catalog | SDK / AndroidX / test依赖 | defer；仅在 ServiceCompat 无直接可用坐标时窄 adapt | E17-9 | 0 新第三方依赖；不得引入 BLE/DI/framework 库 |
| `core/notifications/ActiveWorkoutHeartRateService.kt`（预计；当前不存在） | 当前不存在 | add one concrete Service | E17-9 | Service不得成为 GATT或训练业务owner；`START_NOT_STICKY` |

### 3.4 E17-1 debug / `.local` 证据

| 资产 | 当前责任 | E17 分类 | 后续用途 | 风险 |
|---|---|---|---|---|
| `app/src/debug/.../E17Band9HrsRevalidationActivity.kt` | debug-only scan/GATT/HRS/CCCD/raw notify/cleanup 工具 | adapt 仅作测量工具 | E17-5 foreground monotonic measurement；E17-9 screen-off margin measurement | 不能成为 production owner或架构证据；新改动不能冒充 E17-1 已测 APK |
| `docs/testing/e17-1-band9-hrs-revalidation.md` | 当前设备/协议 feasibility `passed` | reference only | 两个新真机 gate 的前置可行性 | 不能替代新 owner / FGS 验收 |
| `.local/smoke/e17-1-band9-hrs-revalidation/` | APK、截图、日志、设备输出 | read-only / never commit | 历史对照 | 存在不等于新 production evidence；后续各 Story 使用独立目录 |

## 4. 当前差距结论

1. runtime ownership 当前不合格：provider/scanner 由 Compose 创建和关闭，Application 没有 owner。
2. callback determinism 当前不合格：无 generation、attempt ID、raw GATT identity gate，且 `connectGatt()` 未使用 Handler overload。
3. public state 当前不完整：manual / stale / provider-unavailable 语义与 D-081 facts 不一致；malformed 被错误升级为 public technical failure。
4. freshness 当前不可采用：policy 固定旧 10 / 15 / 30 秒且 production 未消费。
5. notification ownership 当前不合格：三个 Route production writer 共用 ID `7200`，Route dispose 直接 cancel。
6. FGS 当前不存在：Manifest、Service、coordinator mode / handoff、permission-denied FGS content 均未实现。
7. 当前设置与 saved-device UX 大部分可复用；cold start 已不自动 scan / connect，但连接状态仍由旧 provider DTO 驱动。
8. 冻结胶囊本体和 geometry 不需要修改；只有外部 presentation mapper 与输入事实需要适配。

## 5. 产品—架构—实现—证据追踪矩阵

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
| 非训练后台 cleanup | process visibility fact由owner消费 | 当前无process lifecycle source | E17-7 | ActivityLifecycleCallbacks fact tests | AVD Home/ON_STOP/screen off |
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

## 6. 公共状态与内部事实矩阵

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

## 7. Optional BLE seam 最终规则

**结论：默认不新增 `AndroidBleOperations` seam。** 当前所需确定性可以由原生 Android BLE types + 一个只负责 attempt ID / raw GATT 首次绑定与 mismatch 拒绝的窄 callback harness 完成；具体 `startScan`、`stopScan`、`connectGatt`、`discoverServices`、`setCharacteristicNotification`、`writeDescriptor`、`disconnect`、`close` 继续是 owner 内的真实 production 调用。callback harness 不复制 GATT 模型、不接收 arbitrary business lambda、不做异常归类。

E17-6 明确禁止自行新增 `AndroidBleOperations`、完整 GATT wrapper、parallel device model或 `call(operation, block)`。若在实现前的 test-first owner race 证明：不用 seam 无法确定性覆盖 callback 早于 `connectGatt()` 返回，Story 必须停止并返回主管理做最小架构复核；不得在实现中临时扩张。即使未来复核允许，也只能包围 owner实际使用的单个 `connectGatt` 调用，production consumer只能是owner，且：

- 只分类该调用官方声明的 `SecurityException`；未知 `IllegalStateException`、`RuntimeException`、`Throwable` 继续抛出。
- 不按exception message或厂商字符串分类。
- attempt激活、provider mapping、state transition与legacy mutation全部在异常边界外。
- callback holder先捕获attempt ID并允许raw GATT首次绑定；不得使用会被早到callback读取的未初始化 `lateinit connection`。

## 8. Freshness 测量与无循环门禁

E17 不继承 D-078 / E16 的 10 / 15 / 30 秒。顺序固定如下：

1. **E17-5 M0，任何policy编码前：** 在现有E17 debug工具中仅加入monotonic receive timestamp / interval与typed outcome日志；使用Band 9前台连续notify测量valid payload间隔，分别记录malformed、真实disconnect和平台失败，证据只写 `.local/smoke/e17-5-heart-rate-fact-core/`。
2. E17-5实现者根据前台分布提出一个保守、内部、非最终的 `FreshnessThresholds`，独立Review锁定 waiting、live freshness、data-interrupted三条边界。阈值不得从E16复制；证据不足则E17-5不提交。
3. 阈值进入纯Kotlin边界测试：阈值前Live、阈值点清除bpm、阈值后保持interrupted、malformed不续命、invalid monotonic fail-closed。所有时间为elapsed realtime；wall clock只作展示。
4. E17-6 / 7只启用前台manual链路；active workout进入后台时，在FGS未实现前必须cleanup，不能宣称后台Live保证。
5. **E17-9 M1：** 先实现但不宣称验收完成的connected-device FGS路径；在同一Story合并前，用Band 9测量screen off / lockscreen /临时后台的valid notify调度分布和余量，仍区分malformed、真实disconnect、平台失败。
6. E17-9实现者据M0+M1提出最终阈值，独立Review锁定；更新同一纯Kotlin阈值测试后，才允许完成FGS Story并对外描述最终Live边界。M1证据不足则E17-9不得merge，production保持E17-7前台manual能力与后台cleanup。
7. E17-10只复验最终阈值下的完整链路，不重新选择数字；发现回归时返回E17-9 Repair，而不是在QA Story临时调值。

因此锁屏余量依赖FGS，但不形成循环：前台 provisional threshold只支持E17-7前台能力；FGS在E17-9内先可运行、后测量、再锁最终值、最后合并。没有任何阶段把旧bpm或未测数字宣传为最终Live合同。

## 9. Android 平台与 Manifest 矩阵

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

Android一手资料（核验日期2026-07-18）：

- [Connected device foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device)：当前仍要求`connectedDevice` type、`FOREGROUND_SERVICE_CONNECTED_DEVICE`、`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`，并以已授予的Bluetooth runtime permission等作为运行前置。
- [Launch a foreground service](https://developer.android.com/develop/background-work/services/fgs/launch) 与 [Troubleshoot foreground services](https://developer.android.com/develop/background-work/services/fgs/troubleshooting)：先`startForegroundService()`，Service须在数秒内`ServiceCompat.startForeground()`，否则可能触发`ForegroundServiceDidNotStartInTimeException`。
- [Background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)：Android 12+后台启动受限；不满足例外会抛`ForegroundServiceStartNotAllowedException`。本架构只从明确可见前台启动。
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)：Android 13+拒绝通知权限时，FGS notice仍可见于Task Manager，但不在notification drawer；因此拒绝不能跳过`startForeground` notification。
- [BLE background communication](https://developer.android.com/develop/connectivity/bluetooth/ble/background)：后台BLE仍要求进程存活，进程被杀连接关闭；需长期维持时可使用`connectedDevice` FGS，且仍受后台启动限制。
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)：Android 12+ scan/connect runtime permissions及API 30以下兼容边界与当前Manifest一致。
- [`BluetoothDevice`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice) 与 [`BluetoothGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)：API 26–36可用带`Handler`的`connectGatt` overload；owner只实现实际使用的discover、notification、descriptor、disconnect/close路径。API 37 deprecation不改变当前target 36 Story，但后续升级target时需另行审计。

未发现官方当前要求与D-081 `connectedDevice`选择冲突。

## 10. Notification ID `7200` 单一 writer 验收矩阵

Route只提交最新训练状态；唯一 Application-scoped `ActiveWorkoutNotificationController` production coordinator持有mode与latest state。Service不是第二业务owner，也不是GATT owner；Service只有在coordinator明确交接后对同一ID调用`startForeground`。

| 输入 / 迁移 | 唯一有权动作 | 结果 / 必测断言 |
|---|---|---|
| `NONE -> ORDINARY` | coordinator notify | 仅一条7200；permission deny则NONE但训练继续 |
| `ORDINARY -> FGS` | coordinator锁writer并启动Service；Service接管7200 | 无第二条；Service立即foreground；FGS copy准确 |
| `FGS -> ORDINARY` | Service先demote/stop并ack交还；coordinator再notify | 交接顺序固定；同ID；无间歇双writer |
| `FGS -> NONE` | Service以`STOP_FOREGROUND_REMOVE`移除7200并ack；coordinator只记NONE | terminal/cleanup幂等；最终移除只一次 |
| `ORDINARY -> NONE` | coordinator cancel一次 | terminal最终移除 |
| 重复terminal | coordinator去重 | 最终移除只一次 |
| Route dispose | Route不cancel | active notification保持；仅提交route unavailable fact（如需要） |
| Service stop / 重复callback | coordinator按mode与latest state幂等接收ack；必要时转ordinary或NONE | 不二次terminal、不重发旧state |
| ordinary `POST_NOTIFICATIONS`拒绝 | coordinator不notify；若旧ordinary仍存在则cancel一次 | 不阻塞训练；不进入FGS错误 |
| FGS路径 `POST_NOTIFICATIONS`拒绝 | 仍build并submit FGS notification | 不走ordinary Ignored分支 |
| FGS启动/提升失败 | coordinator记录failure并按latest训练状态恢复ordinary或NONE；前台可保留连接，随后后台必须cleanup | 不创建第二Service/GATT owner，不宣称后台保证 |
| 训练继续、心率停止 | `FGS -> ORDINARY` | 训练状态最新；HR停止不终止训练 |
| 前台terminal且连接eligible | `FGS -> NONE`，owner保留同一attempt | 只显示不记录；不是reconnect |
| 后台terminal | `FGS -> NONE` + owner cleanup | 回前台不自动恢复 |

E17-9的固定基线是：Service先以`STOP_FOREGROUND_REMOVE`移除自身的7200并ack；目标为ordinary时，coordinator只在ack后按latest state重发同一ID，目标为NONE时不再调用第二次cancel。允许短暂无通知窗口，不允许双writer或两条常驻通知。若Android实测证明该顺序无法满足平台合法性或用户可观察验收，E17-9必须停止并返回主管理复核；不得在implementation内自行切换detach handoff或新增协调抽象。

## 11. 风险隔离后的 implementation Story 序列

每个后续Story只有在前一个Story独立Review / merge / push完成、其immutable full SHA成为`main` ancestor、`main...origin/main = 0 0`且权威文档一致时才解锁。分支名不是解锁事实；不增加状态docs-sync或递归closeout。

### E17-5 Heart-rate Fact / Freshness / Presentation Core

- **唯一主要风险轴：** public facts、malformed / freshness与冻结presentation兼容。
- **允许范围：** `HeartRateState.kt`、`HeartRateBoundary.kt`、`BleHeartRateProviderBoundary.kt`、`HeartRateFreshnessPolicy.kt`、`HeartRateFloatingCapsuleState.kt`；上述pure tests；debug `E17Band9HrsRevalidationActivity.kt`仅允许添加monotonic measurement字段；新增该Story testing/evidence文档。
- **禁止：** scanner/GATT owner、Application wiring、Route、Service、Manifest、notification、Room、记录、reconnect、胶囊本体/geometry。
- **前置：** E17-4 reviewed/merged immutable SHA为main ancestor且main同步。
- **Acceptance：** M0证据成立；状态矩阵完整；malformed不failure/不续命；旧bpm不live；presentation不改视觉DTO；不复制旧数字。
- **Evidence：** pure Kotlin + `.local/smoke/e17-5-heart-rate-fact-core/` Band 9前台间隔；独立Review锁provisional阈值。
- **Rollback / disabled：** 仅core未接新owner；回滚不影响现有训练；默认opt-in仍false。

### E17-6 Deterministic Android BLE Runtime Owner

- **唯一主要风险轴：** scanner/GATT/callback identity、main-queue serialization与cleanup确定性。
- **允许范围：** 新`HeartRateRuntimeOwner.kt`；替换/退休`AndroidBleHeartRateProvider.kt`、`HeartRateDeviceScanner.kt`与旧BLE DTO；adapt permission planner；owner/callback/platform tests；parser只消费不修改。
- **禁止：** Application/settings/capsule wiring、notification/Route/Service/Manifest、FGS、Room、reconnect/scheduler、`AndroidBleOperations` seam。
- **前置：** E17-5 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **Acceptance：** main queue；generation/attempt/raw GATT首次绑定；callback早于返回；old callback/target/generation拒绝；permission TOCTOU；具体BLE调用；cleanup顺序/幂等；intentional stop不伪造disconnect；unknown exception继续抛；无parallel owner。
- **Evidence：** owner确定性tests + Android平台tests；AVD只做permission/Bluetooth/no-crash，不声称RF。
- **Rollback / disabled：** owner尚未接production UI；旧provider不得与新owner同时production可达。

### E17-7 Application / Settings / Capsule Production Wiring

- **唯一主要风险轴：** composition root、process visibility、manual user flow与跨页面同owner接线。
- **允许范围：** `TrainFlowApplication.kt`、`MainActivity.kt`、`TrainFlowApp.kt`、新窄`ProcessVisibilityTracker.kt`（平台`ActivityLifecycleCallbacks`）、settings state/route/app mapper、DataStore兼容、capsule mapper tests、Application owner tests。
- **禁止：** FGS/Service/notification ownership、Route notification代码、Room/session HR、自动恢复、胶囊visual/geometry。
- **前置：** E17-6 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **Acceptance：** Application只创建一个owner；Activity recreation/page navigation不重建；离开settings只停scan；cold start只恢复saved hint；opt-out/permission loss/Bluetooth off/non-training background cleanup；主动permission/scan/saved exact match/manual select/connect；active training background在FGS未实现时也cleanup且不宣称保证。
- **Evidence：** pure/UI mapping、Application/platform tests、AVD grant/deny/revoke/navigation/Home/process recreation；**Band 9 basic gate必须通过后才merge**。
- **Rollback / disabled：** default off；若Band gate失败不merge；不保留第二owner或假“后台持续”copy。

### E17-8 Application-scoped Ordinary Notification Coordinator

- **唯一主要风险轴：** 先消除三个Route业务writer，建立ID7200 ordinary单writer与terminal幂等。
- **允许范围：** active notification contracts/controller/mapper、TrainFlowApplication/MainActivity窄composition接线、三Workout Route的state submission与tests。
- **禁止：** Service/Manifest/FGS、BLE owner、胶囊、Room、notification core新interface。
- **前置：** E17-7 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **Acceptance：** 唯一Application production instance；Route只submit；Route dispose不cancel；active/paused ordinary行为保持；terminal最终一次；permission denied ordinary可不发；三Route切换不产生旧writer。
- **Evidence：** pure coordinator transition tests、Android notification port tests、AVD ordinary allow/deny/Route dispose。
- **Rollback / disabled：** ordinary notification是既有真实能力；失败可回滚此Story而不触碰BLE owner，不能merge双writer过渡态。

### E17-9 Connected-device FGS And ID 7200 Handoff

- **唯一主要风险轴：** Android connected-device FGS合法启动、Service lifecycle与ordinary/FGS单writerhandoff。
- **允许范围：** 新`ActiveWorkoutHeartRateService.kt`、Manifest、active notification contracts/controller、Application coordinator与必要first-party AndroidX direct dependency声明、Service/platform tests、最终freshness阈值/test更新、该Story evidence doc。
- **禁止：** 新GATT owner、Service持有scanner/GATT、background scan/connect/reconnect、notification core新interface、Room/record、胶囊visual。
- **前置：** E17-8 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致。
- **Acceptance：** 全7200矩阵；visible-start；immediateServiceCompat.startForeground；connectedDevice type；ordinary vs FGS deny分支；`START_NOT_STICKY`；failure、process death、Task Manager、terminal visibility；M1锁屏调度测量后锁最终freshness。
- **Evidence：** pure coordinator、Android Service/Manifest、AVD全handoff + **Band 9 active-training lockscreen/background gate**；两者未全过不merge。
- **Rollback / disabled：** 在M1/真机gate完成前不启用后台保证；失败保持E17-7前台manual能力与后台cleanup。零新增第三方依赖。

### E17-10 Integrated AVD / Band 9 Production Acceptance

- **唯一主要风险轴：** 已合并组件的端到端证据与发布资格，不再设计owner/GATT/FGS。
- **允许范围：** 新testing/evidence文档；必要时仅修测试fixture或已批准threshold/activation常量，总production修改上限80行。超过上限或需要ownership变化则停止并拆Repair。
- **禁止：** 新抽象、wrapper、seam、reconnect、Room/record/analysis/export、UI redesign。
- **前置：** E17-9 独立 Review / merge / push完成，immutable full SHA为同步`main` ancestor且文档一致；M1与final freshness已在E17-9合并前锁定。
- **Acceptance：** 本文第12节AVD与两个Band gate全通过；APK/build identity可追溯；无crash/ANR；未通过即结论failed/inconclusive，不把E17-1证据代替。
- **Evidence：** `.local/smoke/e17-10-heart-rate-production-acceptance/` + committed conclusion doc。
- **Rollback / disabled：** 未通过不宣称production ready；按finding返回最小E17-7/8/9 Repair，功能默认off保持核心训练可用。

六个Story没有把owner、GATT、FGS与真机acceptance重新塞进同一变更：E17-5隔离事实/时间；E17-6隔离平台runtime；E17-7隔离composition/user wiring；E17-8先消除notification多writer；E17-9只做FGS/handoff并在同Story锁必要锁屏threshold；E17-10只做端到端证据。这比机械五段多一个ordinary coordinator收口，避免E16-10b-2把owner、callback、scheduler、FGS与验收同时扩张。

## 12. 测试与证据计划

### 12.1 纯 Kotlin

- parser 8 / 16-bit、flags、empty/truncated malformed。
- runtime fact -> public state；disabled、permission、Bluetooth、not connected、scanning、connecting、waiting、live、interrupted、disconnect、failure、intentional stop。
- provisional/final freshness exact boundaries；malformed不刷新；旧bpm不进入新Live；invalid monotonic fail-closed。
- presentation mapper文案、区间/上限visual-only与冻结DTO兼容。

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
- Service/notification contract、ID7200全部handoff与permission分支。
- source search / Manifest test只作为静态补充，不冒充行为证据。

### 12.4 AVD

固定环境：SDK `.local/android-sdk`；AVD `TrainFlow_Pixel_API_36`；每Story证据 `.local/smoke/<Story ID>/`。

覆盖permission grant/deny/revoke、Bluetooth off/on、Home/ON_STOP、screen off/on、ordinary/FGS content、ID7200单writer、upgrade/downgrade/terminal、Route dispose、process recreation无old bpm、no-crash/no-ANR。AVD不能证明RF、GATT、CCCD、notify或Band 9。

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

## 13. 文件、容量与依赖预算

实际627行旧provider、339行presentation mapper、三个Route writer与新增Service表明D-081中的“production Kotlin 450–750行、20–30方法、APK <50KiB”作为**全序列**估算不可信。新预算仍是规划区间，不是验证事实：

| Story | 预计production文件 / test文件 | production新增/重写行 | test新增/重写行 | 窄方法数 | 第三方依赖 | APK验证 |
|---|---|---:|---:|---:|---:|---|
| E17-5 | 5–6 / 4–6 | 220–360 | 300–500 | 12–20 | 0 | release baseline对比；纯core预期小 |
| E17-6 | 4–6 / 5–8 | 650–950 | 550–850 | 30–45 | 0 | 对比旧627行provider替换后的net dex，不按源码行推APK |
| E17-7 | 7–10 / 5–8 | 260–430 | 350–600 | 15–25 | 0 | release APK + apkanalyzer package delta |
| E17-8 | 6–8 / 4–6 | 220–360 | 300–500 | 12–20 | 0 | notification core package delta |
| E17-9 | 4–7 / 5–8 | 280–460 | 400–700 | 15–25 | 0 | Service/Manifest前后release APK与DEX包差异 |
| E17-10 | 0–2 / 0–3 | 0–80 | 0–180 | 0–5 | 0 | 最终release APK、SHA256、apkanalyzer复核 |
| **总计** | 去重后约20–27 / 18–30 | **1,630–2,640** | **1,900–3,330** | **84–140** | **0** | 规划净APK区间30–120KiB，必须实测 |

总行数是新增/重写工作量，不等于净仓库增长；旧627行provider、旧DTO与Route writer会被删除/重写，因此净行数更低。`<50 KiB`不再作为先验承诺；新的规划净release APK区间为30–120KiB，只有各Story同toolchain的before/after release APK、`apkanalyzer dex packages`与最终SHA才是事实。

不引入厂商SDK、BLE library、DI library、scheduler、通用framework或新第三方依赖。若`ServiceCompat`需要停止依赖transitive classpath，E17-9可把现有AndroidX Core作为直接first-party坐标声明；它不计第三方依赖，但必须记录版本和dependency delta。全序列最多一个runtime owner、**零默认BLE seam**、一个concrete Service，不新增notification core interface。

## 14. Readiness 通过标准与稳定状态

| 标准 | 结论 |
|---|---|
| D-080与D-081无矛盾 | Pass |
| 当前代码迁移清单完整 | Pass；已按真实627行provider、空Application、三Route writer校正 |
| 目标无双runtime owner / 双notification writer | Pass；E17-6与E17-8分别设置不可merge门禁 |
| optional seam明确 | Pass；默认不新增，无法实现则stop / management review |
| freshness无循环 | Pass；E17-5前台M0 -> E17-9可运行FGS M1 -> final lock |
| Story按风险拆分 | Pass；E17-5至E17-10六段 |
| 每Story范围、验收、证据、禁止项 | Pass |
| AVD / Band 9 evidence层级准确 | Pass；两个Band gate独立，E17-1仅reference |
| 排除Room/记录/分析/导出/自动恢复 | Pass |
| 胶囊冻结资产不重做 | Pass |
| 未解决产品/架构选择 | None |

稳定状态真值：

- E17-4 immutable SHA尚非`main` ancestor，或独立Review、merge/push、`main...origin/main = 0 0`、五份文档一致性任一未完成：E17-4 = `implemented / needs review`；production implementation = locked。
- 全部门禁满足且本readiness结论仍通过：E17-4自动视为`reviewed / merged`；E17-5 gate自动satisfied。
- 不创建E17-4状态docs-sync，不硬编码未来merge commit，不在本开发对话宣称reviewed / merged。

下一步只能是独立 **E17-4 Implementation Readiness Review**。
