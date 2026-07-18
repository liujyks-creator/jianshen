---
workflowType: architecture
projectName: TrainFlow
documentLanguage: zh-Hans
status: implemented / needs review
date: 2026-07-18
stepsCompleted:
  - investigation
  - architecture-options
  - user-confirmation
  - architecture-documentation
inputDocuments:
  - docs/planning/e17-2-heart-rate-product-scope.md
  - docs/planning/e17-heart-rate-correct-course.md
  - docs/testing/e17-1-band9-hrs-revalidation.md
  - docs/testing/e16-heart-rate-retrospective-and-supersession.md
  - docs/architecture.md
---

# E17-3 心率最小技术架构

**状态判定：** E17-3 合并门禁未全部满足时为 `implemented / needs review`；独立 Review 通过、`--no-ff` merge / push 完成、最终 immutable Story SHA 成为 `main` ancestor、`main...origin/main = 0 0` 且权威文档一致后，自动视为 `reviewed / merged`，E17-4 gate 同步转为 satisfied

**用户确认：** 2026-07-17 确认方案 A，接受活跃训练锁屏 / 临时后台维持当前 BLE 连接时的持续通知和系统任务管理器可见成本

**Story 边界：** docs-only architecture；不修改 Kotlin、测试、Manifest、Gradle、资源或 prototype，不开始 production implementation

## 1. 背景与目标

E17-1 已在当前 Android 手机、HUAWEI Band 9 和标准 BLE Heart Rate Service（HRS）条件下证明：广播开启后能够发现 `0x180D`，完成 GATT 连接、发现 notify 型 `0x2A37`、写入 `0x2902` CCCD，并持续收到可由现有 parser 正确解析的 bpm。该证据证明当前设备 / 协议路径可行，不证明 production provider、生命周期、callback race、后台维持或其他设备兼容性。

E17-2 已冻结用户合同：心率默认关闭，用户显式开启；权限、扫描和连接由用户主动操作触发；saved device 只是便利提示；前台跨页面显示冻结胶囊；未训练只显示不记录；活跃训练允许锁屏 / 临时后台维持当前连接；初始断连恢复为手动恢复。

本架构的目标是在不扩建通用 BLE framework 的前提下，为小型、本地 Android App 提供一个可验证、可维护的唯一 BLE runtime owner，明确资源所有权、callback 串行化、identity、权限竞态、cleanup、状态分层、freshness 和后台边界，并给 E17-4 提供 implementation-readiness 输入。

## 2. 产品输入与非目标

### 2.1 必须满足的产品事实

- 默认关闭；只有设置页显式 opt-in 后才显示和准备设备能力。
- 权限只在用户主动扫描或连接时请求；权限恢复后不自动扫描或连接。
- 扫描由用户主动发起且有限时；saved identifier 只有在用户点击后才用于有限时精确匹配。
- display name、address 和当前观察到的 identifier 都不是永久设备身份。
- 冻结胶囊在 TrainFlow 前台跨页面显示；页面切换和 Compose 重组不能改变连接 owner。
- 未训练只显示，不记录。
- 活跃训练已有当前连接时，锁屏或临时后台可以维持该连接；非训练进入后台后必须停止采集。
- 初始断连恢复为用户手动恢复；不自动 scan、connect、reconnect 或换 target。
- 数据中断后旧 bpm 不能继续显示为实时数据。
- 不做医疗告警、声音 / 震动强制提醒、自动暂停或训练中断。

### 2.2 本 Story 明确不做

- 不实现 production Kotlin、测试、Manifest、Gradle、资源或 prototype。
- 不设计或实现 Room、心率样本记录、复盘、趋势、导出或外部模型分析。
- 不恢复 D-078 的 10 / 15 / 30 秒 freshness 或 2 / 5 / 10 秒 retry 默认值。
- 不实现自动重连 controller、retry scheduler、后台扫描或冷启动恢复。
- 不建设通用 BLE framework，不复制 Android GATT 完整对象模型，不引入厂商 SDK。
- 不重做 `HeartRateFloatingCapsule.kt`、`HeartRateCapsuleGeometry.kt`、已批准 motion 或 HTML 视觉资产。
- 不承诺 Band 9 之外的设备、手机、固件或厂商环境通用性。

## 3. 当前 `main` 遗产分类

代码已存在于 `main` 不代表默认复用；以下分类以 E17 产品合同、最小架构预算和当前源码审计为准。

| 遗产 | 分类 | 结论与理由 |
|---|---|---|
| `core/health/HeartRateBoundary.kt` 中 `HeartRateProvider` | adopt as-is | 保留单一 `heartRateState: Flow<HeartRateState>` 观察边界；新的 runtime owner 实现它，不创建平行 provider。用户动作是 owner 的窄命令，不扩张 provider 为通用平台接口。 |
| `core/health/AndroidBleHeartRateProvider.kt` | replace | 当前类同时承载扫描、设备 DTO、GATT 和状态，但没有 attempt generation + raw GATT identity 双重校验，且由 Compose `remember` 创建；替换为进程级唯一 `HeartRateRuntimeOwner`。有效 HRS 流程可参考后重写，不复制旧生命周期。 |
| `core/health/BleHeartRatePermissionPlanner.kt` | adapt | 保留 API 级别与所需权限规划；把 availability 与用户动作分开，并补充具体 BLE 调用处的 TOCTOU `SecurityException` 处理。 |
| `core/health/BleHeartRateProviderBoundary.kt` | replace | 旧 provider / scan / recoverable DTO 把技术 message、scan 和用户事实混在同一边界；改为 owner 内部 Android runtime facts + 外部 `HeartRateState`，不让 message 成为状态输入。 |
| `core/health/HeartRateDeviceScanner.kt` | isolate/retire | 当前 wrapper 使 Compose 持有扫描 / provider 生命周期；不再作为第二 owner。必要的候选 UI 数据由唯一 owner 输出。 |
| `core/health/HeartRateMeasurementParser.kt` | adopt as-is | 纯 Kotlin、无 Android 依赖，E17-1 真实 payload 已与 parser bpm 一致；malformed payload 返回失败且不得刷新 freshness。 |
| `core/health/HeartRateFreshnessPolicy.kt` | adapt | 保留 monotonic time、invalid timeline fail-closed 和明确 reason 的思想；移除 E16 固定阈值与 reconnect 假设，只判断最近有效样本是否仍为 current。 |
| `core/model/HeartRateState.kt` | adapt | 保留 source-aware core model，但补足 disabled、not connected、connecting、waiting data、live、data interrupted / stale、explicit link disconnect、permission unavailable、Bluetooth off、technical failure 与 intentional stop 的事实区分；移除当前无产品入口的 manual reading 语义。 |
| `ui/shell/official/TrainFlowApp.kt` runtime wiring | adapt | 移除 Compose 内 `remember` owner 和 `DisposableEffect.close()`；只绑定 Application owner 的 state / actions，页面离开只停止设置页 scan，不关闭另一页面仍在用的连接。 |
| `ui/shell/official/HeartRateFloatingCapsuleState.kt` | adapt | mapper、文案、优先级和 runtime DTO 可重写；mapper 必须位于胶囊外部，只把用户事实映射为冻结 presentation state。 |
| `HeartRateFloatingCapsule.kt` | adopt as-is | 视觉、信息布局、collapsed / expanded、tap / drag 和 motion 已冻结，direct reuse。 |
| `HeartRateCapsuleGeometry.kt` | adopt as-is | clamp、safe-zone、snap、IME / viewport 避让已冻结，direct reuse。 |
| settings / device picker | adapt | 保留 opt-in、权限 rationale、主动有限时扫描、手动选择和 saved-device 精确匹配；移除对 BLE runtime DTO 的直接依赖，只发用户动作并观察用户事实 / scan presentation。 |
| DataStore `heartRateDisplayEnabled` 与 saved identifier / display name | adopt as-is | 默认关闭和便利提示合同有效；仍不得保存 `BluetoothDevice`、GATT、bpm 或 session summary。 |
| DataStore `showDisconnectedHeartRatePlaceholder` | isolate/retire | 旧 placeholder key 不再驱动当前产品；实现 Story 决定只读兼容或清理方式，不恢复占位 UI。 |
| `app/src/main/AndroidManifest.xml` 现有 BLE 权限 | adopt as-is | `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT` 与 API 30 以下 location fallback 保留；方案 A 另需 FGS permission / service 声明。 |
| parser / capsule geometry 单元测试 | adopt as-is | 纯函数证据仍有效，但不能代表 runtime 或真实 BLE。 |
| state / permission / settings / DataStore 测试 | adapt | 更新为新的事实状态、主动动作、opt-out 和 TOCTOU 合同。 |
| E16 固定 freshness 阈值、源码字符串搜索、Compose owner lifecycle 测试 | replace | 旧阈值被 supersede；helper / source search / 可能 no-op closure 不是 production behavior；新测试必须验证 owner 的实际 race 与资源行为。 |

## 4. 方案比较与决策

### 4.1 方案 A：进程级 owner + 活跃训练 `connectedDevice` FGS（采用）

一个由 `TrainFlowApplication` 创建的 `HeartRateRuntimeOwner` 实现现有 `HeartRateProvider`，唯一持有 BLE runtime 资源。TrainFlow 前台跨页面时 owner 随进程存在；当“活跃训练 + 已有当前连接”首次同时成立时，必须趁 App 仍可见立即启动 `connectedDevice` foreground service，而不是等 App 已进入后台再启动。Service 复用 / 升级现有训练进行中通知和 notification ID，只负责把同一个进程 owner 保持在合法的前台服务场景中，不持有第二套 scanner 或 GATT。

优点：满足锁屏 / 临时后台连接保证；连接 owner 不随页面变化；资源和 callback race 可集中测试。成本：增加一个 concrete Service、Manifest 声明、持续通知和系统任务管理器可见；系统仍可终止进程，不能承诺 process-death 恢复。

### 4.2 方案 B：进程级 owner，仅依赖前台进程（可行备选，未采用）

同样使用唯一进程 owner，但不启用 FGS。前台跨页面能力不变，锁屏 / 后台时仅依赖进程仍存活和系统调度。

优点：少一个 Service、FGS permission 和持续通知，体积与运维更简单。缺点：无法对 targetSdk 36 的活跃训练锁屏 / 临时后台维持作出产品保证，不满足 D-080 已接受的当前连接后台维持要求。

### 4.3 Service 永久持有 GATT（未采用）

让 Service 从 opt-in 起长期成为连接 owner。虽然资源集中，但未训练后台也会维持连接并产生持续通知，扩大功耗和用户可见成本；还容易让 Service 与 UI 生命周期形成双向控制，不符合默认关闭、非训练后台停止和最小 App 预算。

### 4.4 E16-10b-2 通用 controller / wrapper 路线（拒绝）

失败分支引入 platform-call boundary、完整 GATT wrapper、reconnect controller、callback gate 和 scheduler 等大规模抽象，并把 freshness 与 retry 绑定。它超过当前需求，不得 merge、cherry-pick 或复制；D-078 也不恢复。

## 5. 推荐组件与依赖方向

```text
Settings actions ------\
                       > HeartRateRuntimeOwner --> Android BLE scanner / GATT
Workout/app lifecycle -/          |
                                  +--> HeartRateState (user facts)
                                             |
                                             v
                                  capsule presentation mapper
                                             |
                                             v
                                  frozen floating capsule

ActiveWorkoutHeartRateService -- references the same process owner
                              -- owns no scanner / callback / GATT
```

### 5.1 `HeartRateRuntimeOwner`

- Application / 进程级唯一实例，实现现有 `HeartRateProvider`。
- 唯一持有 scanner、scan callback、active target、`BluetoothGatt`、`BluetoothGattCallback`、scan / connect timeout 和 freshness timer。
- 负责主动 scan、精确 saved identifier 匹配、手动选择、connect、discover、HRS / characteristic / CCCD 检查、notify、disconnect 和 close。
- 接收 settings 用户动作、App 前后台和训练 active / terminal 事实；不依赖 Compose 页面。
- 输出用户可理解的 `HeartRateState` 与设置页所需的窄候选 / scan presentation，不输出 Android BLE 对象。

### 5.2 `ActiveWorkoutHeartRateService`

- concrete Android Service，不是第二 provider、owner 或新接口。
- 只在活跃训练已有当前连接时由前台可见状态启动，并在锁屏 / 临时后台期间保持；不依赖后台启动例外。
- `START_NOT_STICKY`；进程死亡后不重建连接、不自动 scan / connect。
- 复用训练进行中 notification channel / ID；调用 `startForeground(..., FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)`。
- training terminal、用户停止、opt-out、权限丢失或 Bluetooth off 时停止 foreground 并结束自身；真正 cleanup 委托同一 owner。

### 5.3 presentation mapper

- 位于冻结胶囊外部；输入只有 `HeartRateState`、opt-in / saved-device 等产品事实和用户参数。
- 输出 `HeartRateFloatingCapsuleUiState`；文案和优先级只存在于 presentation 层。
- 胶囊不请求权限、不扫描、不连接、不重连，也不持有 owner。

### 5.4 依赖规则

- Android BLE types 只存在于 Android runtime owner 与可选窄 platform seam 内。
- `HeartRateState` 不含 `BluetoothDevice`、address、`BluetoothGatt`、callback、descriptor 或异常 message。
- settings、训练引擎、记录、胶囊和 presentation mapper 不依赖 Android BLE model。
- 用户文案不是状态机输入；状态转换使用 typed facts / reason codes。

## 6. 唯一资源所有权

| 资源 / 行为 | 唯一 owner | 其他组件允许的行为 |
|---|---|---|
| `BluetoothLeScanner`、scan callback、scan generation、timeout | `HeartRateRuntimeOwner` | settings 发 start / stop action 并观察候选与状态 |
| 当前 target、active connect attempt | `HeartRateRuntimeOwner` | settings 传用户选择的 opaque identifier |
| `BluetoothGatt`、`BluetoothGattCallback` | `HeartRateRuntimeOwner` | Service / settings / capsule 不可持有或关闭 |
| connect、discover、CCCD、notify、disconnect、close | `HeartRateRuntimeOwner` | 其他组件只能发 typed action |
| FGS 生命周期与 notification | `ActiveWorkoutHeartRateService` | owner 提供是否需要维持的事实；Service 不操作 GATT |
| 用户事实 | owner 输出的 `HeartRateState` | mapper / settings 只读 |
| 胶囊 presentation | 外部 mapper | 冻结胶囊只渲染与发 UI 互动 |

`TrainFlowApplication` 是实例组合根。Activity、destination、ViewModel 或 Compose 重组都不得 new / remember 第二个 runtime owner。Service 必须从同一 Application 容器取得该实例。

## 7. Callback 串行化与 identity

### 7.1 串行边界

采用 Android main looper 作为唯一状态转换边界，避免为小型 App 新增 actor / dispatcher owner：

- settings action、App / training lifecycle、timeout 和 freshness tick 都投递到同一 main `Handler`。
- scan callback 收到平台事件后只捕获必要参数并 `post` 到 main handler；不在 callback 原线程修改状态。
- API 26–36 的 `connectGatt()` 使用带 `Handler` 的 overload，把 GATT callback 定向到同一 main looper。
- 所有 active generation、attempt、raw GATT binding、状态发布和 cleanup 引用变更都只在该边界执行。

### 7.2 generation 与 attempt

- 每次用户 scan / connect 分配单调递增 generation；stop、opt-out、权限失败、Bluetooth off、后台策略停止和 cleanup 都先递增 / 失效 generation。
- scan result 必须同时匹配 active scan generation 和当前 scan callback；迟到 result / failure 被忽略。
- 每次 connect 先建立 `Attempt(id, targetIdentifier, phase)` 并设置为 active，再创建捕获 `attemptId` 的 callback，最后调用 `connectGatt()`。
- attempt 不保存一个尚未初始化的 wrapper；callback 可以先用 raw `BluetoothGatt` 尝试绑定 active attempt。
- `connectGatt()` 返回值也回到相同串行边界执行 `bindGattIfAbsent`。首次绑定成功后，后续 callback 必须同时满足 `attemptId == activeAttempt.id` 且 `gatt === activeAttempt.gatt`。
- 若 callback 先绑定的 raw GATT 与随后返回值不一致，或返回时 attempt 已失效，立即关闭不属于 active attempt 的 GATT，不改变用户状态。
- identifier / address 只用于本次用户动作的 target 匹配，不能代替 raw GATT 对象 identity。

### 7.3 迟到 callback 规则

- 旧 generation、旧 attempt、旧 target 或 raw GATT 引用不匹配的 callback 不得 discover、写 CCCD、发布 live、恢复状态或启动新连接。
- cleanup 先使 attempt 不可达并清空 owner 引用，再关闭平台资源；因此 cleanup 后迟到 callback 只能被拒绝并尽力 close 自带 GATT。
- terminal cleanup 后不会因 `STATE_DISCONNECTED` callback 把 intentional stop 改写成 explicit link disconnect。

## 8. 状态分层

### 8.1 Android BLE runtime facts（owner 内部）

内部事实描述资源和平台阶段，例如 disabled、permission unavailable、Bluetooth off、idle、scanning、target selected、connecting、discovering、subscribing、waiting first sample、live transport、link disconnected、technical failure、stopping。它可以引用 Android 资源，但不越过 owner。

### 8.2 用户事实 `HeartRateState`

至少表达：

| 用户事实 | 语义 |
|---|---|
| Disabled | 用户未 opt-in；隐藏胶囊、无 BLE 资源 |
| PermissionUnavailable | 当前动作缺少 / 丢失权限；已失效动作并 cleanup |
| BluetoothOff | adapter 当前不可用；已 cleanup，等待用户操作 |
| NotConnected | 已启用但无当前连接；saved device 仍只是提示 |
| Connecting | 用户动作已进入有限连接流程 |
| WaitingData | notify 已准备但还没有有效 bpm |
| Live | 最近有效 bpm 在 freshness 窗内 |
| DataInterrupted / Stale | 已有数据流中断；旧 bpm 不再作为实时值 |
| LinkDisconnected | active GATT 明确报告非 intentional 的断开事实 |
| TechnicalFailure | scan / connect / discover / characteristic / CCCD / parse 等具体技术失败 |
| IntentionallyStopped | user stop、opt-out、非训练后台策略或训练终态主动停止；不得伪装为 LinkDisconnected |

`explicit link disconnect`、permission unavailable、Bluetooth off、technical failure、waiting、live 与 stale 是不同事实。用户主动停止的最终 presentation 可以回到未连接 / 已关闭，但底层 reason 必须保留到本次转换完成，不能制造“设备掉线”事实。

### 8.3 presentation state

presentation mapper 决定冻结胶囊展示为隐藏、权限、蓝牙关闭、正在连接、等待数据、实时 bpm / 区间、数据中断、离线或异常。mapper 可以调整文案和优先级，但不能反向改变 owner 状态，不能凭旧 bpm 构造 live。

## 9. Permission TOCTOU 与失败分类

availability check 只是调用前快照。用户可能在 check 后撤销权限，因此每个具体 Android BLE 调用仍可能抛出 `SecurityException`。

- 只在 `startScan`、`stopScan`、读取 permission-gated device 属性、`connectGatt`、`discoverServices`、notification / descriptor 操作、`disconnect` / `close` 等具体平台调用处捕获官方合同允许的 `SecurityException`。
- 可选测试 seam 只能提供 typed platform operation；不得接受 arbitrary business lambda，也不得统一捕获 lambda 的 `IllegalStateException`。
- 不按 exception message 或厂商字符串分类。
- 当前 Android 官方 `startScan()` 合同没有允许把任意 `IllegalStateException` 归类为 adapter-state race 的依据，因此本架构不捕获它并伪装为 Bluetooth off。只有未来官方单一 API 明确记录该 race 时，才能在该调用点增加一项窄分类。
- 未知程序错误继续暴露为测试 / crash / technical defect，不发布为 permission 或 Bluetooth fact。
- permission failure 的顺序是：失效 scan generation / connect attempt -> 清空 active 引用 -> 尽力停止 scanner / disconnect / close -> 发布 `PermissionUnavailable` -> 停止 FGS。权限恢复后等待下一次用户主动操作。

cleanup 中某个 permission-sensitive stop / disconnect 失败不能阻止后续 `close()` 和引用清理；每个调用各自 best effort，状态只发布一次。

## 10. Scan、connect 与 cleanup 生命周期

```text
User action
  -> bounded HRS scan
  -> saved-id exact match OR manual selection
  -> stop scan
  -> connect attempt
  -> discover 0x180D
  -> find notify/indicate 0x2A37 + 0x2902
  -> local notification enable + CCCD write
  -> waiting first valid sample
  -> live bpm
  -> stale/data interrupted OR explicit disconnect/failure
  -> manual user recovery

Any terminal stop
  -> invalidate generation/attempt
  -> clear owner references
  -> stop scan + disconnect/close GATT (idempotent)
  -> publish one typed fact
  -> stop FGS when active
```

### 10.1 主动 scan 与选择

1. 校验 opt-in、动作来源和当前 availability；需要权限时先进入 rationale / permission flow。
2. 用户确认后启动 HRS filter 的有限时 scan；重复点击先幂等停止旧 scan 并创建新 generation。
3. “连接已保存设备”只在该 scan window 内用完整 saved identifier 精确匹配；display name 不参与自动选择。
4. 手动扫描展示候选，只有用户点击某候选才固定本次 target。
5. timeout、用户停止、离开设置扫描流程、opt-out、权限丢失或 Bluetooth off 都停止 scan；scan 不在后台继续。

### 10.2 connect 到 live

1. 固定 attempt target 后停止 scan，失效旧 attempt / GATT。
2. 调用 `connectGatt(autoConnect=false, TRANSPORT_LE, mainHandler)`。
3. active callback 报告 connected 后 discover services。
4. 校验 `0x180D`、`0x2A37` 的 notify / indicate capability 和 `0x2902`。
5. 启用本地 characteristic notification，按 API 版本写 CCCD `01 00`（notify）或 indication value。
6. descriptor 成功后进入 WaitingData；首个有效 parser 结果进入 Live，并记录 monotonic receive time。
7. malformed payload 发布 / 统计为 parse technical fact，但不刷新 last-valid-sample；是否立即从 live 降级由 freshness 决定。

### 10.3 幂等 cleanup

统一 `cleanup(reason)` 必须可重复调用：先失效 generation / attempt、取消 timeout / freshness tick、快照并清空 scanner / GATT / target 活跃引用，再对快照资源执行 stop / disconnect / close。资源只由 owner 关闭；Service、settings、胶囊和 Compose disposal 均不得重复关闭。

触发条件包括 user stop、opt-out、Bluetooth off、permission loss、非训练进入后台、训练完成 / 放弃、process termination 的系统资源回收和 owner close。process termination 没有可靠应用 callback，因此不能把 `onDestroy()` 写成唯一 cleanup 证明；系统杀进程后的产品状态是连接消失、下次前台由用户手动恢复。

## 11. Freshness 与手动恢复

- freshness 使用 monotonic elapsed time，只回答“最近有效 bpm 是否仍可视为当前实时值”。wall-clock 字符串不驱动状态机。
- last-valid-sample 只由成功解析且通过 active attempt / raw GATT identity 校验的 notify 更新。
- 到达 stale 边界时立即清除 presentation 可用 bpm 或把它移出 live 字段；可保留仅供诊断的 last value，但不能渲染为实时。
- freshness timer 不调用 scan、connect 或 reconnect，不排队 retry，也不切换 target。
- 明确 GATT disconnect 可以立即形成 LinkDisconnected，不必等待 freshness；仅 notify silence 才由 freshness 形成 DataInterrupted / Stale。
- E17 不采用 D-078 的 10 / 15 / 30 秒或 2 / 5 / 10 秒。E17-4 必须确认首个 runtime implementation Story 包含阈值测量与验收任务；具体单一阈值由该 Story 在编码前根据 Band 9 连续 notify 间隔分布、锁屏 / FGS 调度余量和边界测试确定并记录。测试至少覆盖阈值前仍 live、阈值点清除 live、阈值后保持 stale、malformed payload 不续命和 monotonic 异常 fail-closed。
- stale、link disconnect 和 technical failure 都等待用户点击连接已保存设备或重新扫描；自动恢复另拆产品决策、架构和真机 Story。

## 12. 前台跨页面与活跃训练后台

### 12.1 TrainFlow 前台跨页面

Application owner 与进程同寿命，Activity / destination / Compose 只观察同一 state。前台页面切换不停止当前连接；设置页 scan 可以在离开设置页时停止，但不得影响既有 live GATT。Compose 重组或导航不能创建第二 owner。

### 12.2 活跃训练锁屏 / 临时后台

Android 官方建议：后台持续接收 BLE notification 可使用 `connectedDevice` foreground service；进程终止会关闭连接。方案 A 在以下条件全部满足时、趁 App 仍可见立即启用，并在后续锁屏 / 临时后台期间保持：

- 用户已 opt-in；
- 用户先在前台主动建立了当前连接；
- 当前 workout session 为 active；
- 该 active session 仍允许随后锁屏 / 临时后台维持当前连接。

Service 从可见前台启动，避免依赖后台启动例外；不等到 `onStop` 后才尝试启动，也不在后台新 scan / connect。targetSdk 36 预期 Manifest 成本：

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`
- service 的 `android:foregroundServiceType="connectedDevice"`
- 已有 `BLUETOOTH_CONNECT` 继续作为运行时前提；scan 仍按现有 API 级别权限规划。

FGS 必须显示通知。Android 13+ 即使用户拒绝通知权限，foreground service 仍会在系统 Task Manager / Active apps 中可见；通知抽屉可见性由系统权限决定。实现应复用 / 升级现有“训练进行中”notification channel 和 notification ID，避免同一训练出现第二条常驻通知，但 E17-4 必须审查现有通知 contract 是否能合法承载 FGS。

### 12.3 停止边界

- 非训练状态进入后台：立即 cleanup，不维持采集，不启动 FGS。
- 活跃训练结束或放弃：停止 FGS 并 cleanup；回到前台也不自动恢复。
- user stop / opt-out：停止 FGS、失效动作、cleanup；saved preference 是否保留按 E17-2 合同处理。
- permission loss / Bluetooth off：停止 FGS、cleanup、发布对应事实；恢复后等待用户操作。
- process death / 系统停止：`START_NOT_STICKY`，不自动重建 Service 或连接；下次启动不展示旧 bpm 为 live。
- 用户从系统停止 App / FGS：视为 runtime 终止；不绕过用户意图自动重启。

### 12.4 Android 官方来源

以下均为 Android 官方文档 / API reference，一手来源访问日期为 2026-07-17：

- [Communicate in the background — Bluetooth Low Energy](https://developer.android.com/develop/connectivity/bluetooth/ble/background)
- [Foreground service types — Connected device](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device)
- [Restrictions on starting a foreground service from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Launch a foreground service](https://developer.android.com/develop/background-work/services/fgs/launch)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Notification runtime permission — exemptions](https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions)
- [`BluetoothDevice.connectGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice)
- [`BluetoothGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)

## 13. 测试与证据矩阵

| 层级 | 必须验证 | 不能证明 |
|---|---|---|
| 纯 Kotlin parser / state / presentation | HRS 8/16-bit parser、malformed fail-safe、typed facts、freshness 边界、文案 mapper、旧 bpm 不进入 live | Android BLE 调用、线程、资源关闭、真实设备 |
| runtime owner 确定性测试 | 单 main queue 下 generation、callback 早到 / 迟到、旧 target、raw GATT mismatch、connectGatt 返回竞态、幂等 cleanup、permission failure、intentional stop 不伪造 disconnect | Android framework 实际 callback 与射频 |
| Android 平台边界测试 | API 分支、permission planner、窄 `SecurityException` 分类、Service / notification / Manifest contract、同一 Application owner | Band 9、GATT 服务和 notify |
| AVD permission / lifecycle / no-crash | permission grant / deny / revoke、前后台、训练终态、Service start / stop、process recreation 后无旧 live bpm、no-crash / no-ANR | 射频、scan、真实 GATT、Band 9 |
| 真实 Band 9 基本链路 | 主动 scan、精确 match / 手选、connect、discover、`0x180D` / `0x2A37` / `0x2902`、CCCD、连续 notify、手动 stop、cleanup 后无恢复 | 其他设备通用性、自动恢复 |
| 活跃训练锁屏 / 后台真机 | 已连接后开始 active training、锁屏 / 临时后台持续 notify、单一持续通知、回前台仍同一 attempt、训练结束 / opt-out / permission loss / Bluetooth off 停止 | process death 后恢复、无限后台、自动重连 |

证据硬规则：

- fake / injection 只证明确定性逻辑，不等于真实 BLE。
- AVD 不等于射频、GATT 或 Band 9。
- helper 存在、源码搜索、mock closure 被调用或可能 no-op 的 cleanup 不算 production behavior 证据。
- E17-1 只证明当前设备 / 协议可行性，不证明新 production 架构；新 owner 必须重新完成真机证据。
- 每一层只声明实际执行且有断言 / 可追溯观察的行为。

## 14. 抽象、文件、依赖与容量预算

### 14.1 新增 production abstraction

1. `HeartRateRuntimeOwner`：唯一责任是持有并串行化心率 BLE runtime 与输出 `HeartRateProvider` state；production consumer 是 Application composition root、settings action wiring、presentation mapper 和同进程 Service。
2. 可选窄 typed `AndroidBleOperations` seam：只封装 owner 实际调用的 scanner / GATT 操作与 typed result，production consumer 只有 `HeartRateRuntimeOwner`。若 owner race 能通过更窄的 callback harness 测试，则不创建该接口。

不新增第三个 production interface，不创建 parallel provider、通用 scanner interface、完整 GATT wrapper、reconnect controller 或 scheduler。`ActiveWorkoutHeartRateService` 是 concrete Android 组件，不是第二 owner / abstraction。

更简单方案是方案 B：只保留 owner、不加 Service / FGS；它减少一个 production 文件和 Manifest 成本，但无法保证活跃训练锁屏 / 临时后台维持，因此未采用。

### 14.2 预计 production 文件影响（不在本 Story 实现）

| 影响 | 预计文件 |
|---|---|
| 新增 / 重写唯一 owner | `core/health/HeartRateRuntimeOwner.kt`，替换 `AndroidBleHeartRateProvider.kt` 的 runtime 职责 |
| 可选窄 seam | `core/health/AndroidBleOperations.kt`；只有测试证明需要时新增 |
| 新增 concrete Service | `core/notifications/ActiveWorkoutHeartRateService.kt` 或相邻 platform package |
| 适配 core facts | `HeartRateState.kt`、`HeartRateFreshnessPolicy.kt`、`BleHeartRatePermissionPlanner.kt`、相关 health boundary |
| 适配 composition / lifecycle | `TrainFlowApplication.kt`、`TrainFlowApp.kt`、现有 active-workout notification wiring |
| 适配 settings / presentation | `TrainingPreferencesUiState.kt`、`SettingsRoute.kt`、`HeartRateFloatingCapsuleState.kt`、必要 mapper |
| 适配平台声明 | `app/src/main/AndroidManifest.xml` |
| 保留不改 | parser、冻结 capsule / geometry、DataStore opt-in / saved-device keys |

预计 production Kotlin 新增 / 重写约 450–750 行、20–30 个窄方法；release APK 增量估计 `< 50 KiB`。零新增第三方依赖。FGS 增量主要是一个 Service 类、两项 manifest permission、service type 和复用通知 wiring；没有资源包、厂商 SDK 或数据库容量成本。该估算必须由 implementation Story 的 release APK before / after 实测复核。

## 15. E17-4 readiness 输入

E17-4 必须逐项确认后才能通过：

- D-081 与本架构通过独立 Review 并满足 immutable SHA ancestry / remote sync 门禁。
- implementation Story 只创建一个 process owner，Service 不成为第二 GATT owner。
- 新状态枚举 / facts 能完整区分 disabled、permission、Bluetooth、not connected、connecting、waiting、live、stale、link disconnect、technical failure 和 intentional stop。
- typed user actions、scan / attempt generation、raw GATT identity 和 cleanup 顺序有可执行测试计划。
- permission TOCTOU 只在具体平台调用处捕获 `SecurityException`，未知错误不伪装。
- freshness 数值的测量、确定责任和边界测试写入首个 runtime Story；不继承 D-078。
- `connectedDevice` FGS permission、service type、启动时机、单一通知、通知拒绝表现、停止条件和 `START_NOT_STICKY` 有 AVD + 真机计划。
- Band 9 基本链路与活跃训练锁屏 / 后台证据分别列为真机 gate；不能由 fake / AVD 替代。
- production delta 仍排除 Room、记录、复盘、导出、分析和自动恢复。

## 16. 明确 defer

- 前台持续自动恢复、活跃训练后台断连自动恢复、retry / backoff、后台 scan 和冷启动恢复。
- 心率采样、Room schema、session 记录、平均 / 最高、曲线、区间时长 / 占比和覆盖缺口。
- 导出格式、文件保护、用户导入外部模型分析。
- Health Connect、Wear OS、Huawei SDK、其他厂商 SDK、iOS / watchOS。
- 通用设备兼容矩阵、永久设备身份或 pairing 管理。
- 医疗告警、危险判断、声音 / 震动强制提醒、自动暂停和训练中断。
- 胶囊视觉、互动、geometry、motion、颜色、尺寸和布局重做。

## 17. 风险与开放问题

| 风险 / 开放问题 | 处理 |
|---|---|
| Android / 厂商在锁屏下的 notify 调度差异 | E17-4 要求 Band 9 真机 active-training FGS 证据；不从 AVD 推断。 |
| Band 9 address / label 可能变化 | saved identifier 只作 convenience；失败后让用户手选，不按名称自动连接。 |
| FGS 通知与现有训练通知 contract 冲突 | 首个 implementation Story 前审计 channel / ID / stop action；目标是一条训练通知，不创建第二个 owner。 |
| callback 早于 `connectGatt()` 返回 | attempt 先激活、callback 捕获 ID、raw GATT 首次绑定，返回值再 bind；不读取未初始化 wrapper。 |
| cleanup 期间权限撤销 | 每个具体调用窄捕获、引用先清空、close best effort、只发布一次 permission fact。 |
| freshness 阈值尚无 production 依据 | 首个 runtime Story 在编码前用 Band 9 notify 间隔和锁屏余量确定；E17-4 gate 测量与测试计划。 |
| process death 无法保证 finally / onDestroy | 不把 callback 当 cleanup 证明；`START_NOT_STICKY`，下次启动清空 live 并手动恢复。 |

本架构不解锁 production implementation。E17-3 独立 Architecture Code Review 是唯一下一步；E17-4 仍为 `planned / prerequisite-gated`。
