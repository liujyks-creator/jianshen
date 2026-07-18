# E17 心率子系统 Correct-course 计划

**状态判定：** E17-0、E17-1、E17-2、E17-3 `reviewed / merged`；E17-4 immutable SHA 尚非 `main` ancestor，或独立 Review、merge / push、`main...origin/main = 0 0`、五份文档一致性任一未完成时为 `implemented / needs review`，production implementation 继续 locked；全部门禁满足且 readiness 结论仍通过后，E17-4 自动视为 `reviewed / merged`、E17-5 gate 自动 satisfied
**日期：** 2026-07-12；状态同步：2026-07-18
**性质：** 产品、设备、架构、测试与 implementation-readiness 重新规划

## 1. E17 目标

E17 基于 E16 遗产，为小型本地优先训练 App 重新设计合适的心率子系统。从产品价值、Band 9 / 标准 HRS 可行性、权限、BLE runtime、状态模型、测试层级和真机验收重新对齐，不把旧 E16 实现视为默认正确。

E17 将浮动胶囊视觉与互动作为 `adopted / frozen / direct reuse` 资产直接采用，优先稳定、可解释、用户可见、手动可恢复；自动重连不是默认前置。

## 1.1 历史归档与权威层级

E16 原始代码及 testing、planning、design 文档整体为 `sealed historical archive / reference only`。E17 不再为当前状态逐行修改这些材料；其中历史语境下的 current、next、dependency / depends on、unlocked、in progress 不属于当前指令，不参与 E17 当前状态逐行一致性检查，不能生成任务、解锁 Story 或覆盖 D-079。Review 不得仅因这些历史措辞保留而提出当前状态 finding；未经新的明确 correct-course decision，不得修改 sealed archive。sealed 不删除 Git 历史或分支，不否认历史测试与 Band 9 / HRS 证据，也不把旧实现变成 E17 合同或自动复用旧 production 架构。

文档权威层级为：`AGENTS.md` > accepted superseding decisions D-079 / D-080 / D-081 > 当前 E17 权威文档（project-status、E17-2 产品合同、E17-3 最小架构、E17 correct-course、readiness、roadmap、architecture）> E16 retrospective / supersession 索引 > sealed E16 原始文档与代码（historical evidence only）。低层不得覆盖高层。

## 2. 非目标

- correct-course 阶段不写 production 代码。
- 不在 correct-course 规划 Story 中直接实现自动恢复。
- 不在 correct-course 规划 Story 中实现心率记录、分析、医疗告警或训练控制；接受的后续产品方向必须另拆。
- 不重做胶囊视觉、布局、拖动或动画。
- 不构建通用 BLE 框架。
- 不复制一套 Android GATT 对象模型。
- 不为尚未确认的设备或厂商做过度抽象。
- 不把旧 E16 Story / SHA 当作 E17 解锁前置。

## 3. 重新规划阶段

### E17-0：Retrospective / legacy audit / reset

- 正式关闭 E16 历史支线并记录 supersession。
- 只读审计 main 与失败分支，分类 freeze / revalidate / reference-only / rewrite / reject。
- 重置 project status、decision、roadmap、readiness、architecture 和提示词流程。
- 状态：`reviewed / merged`。
- Immutable Story SHA：`abce4b712139c373f534a6fabab423fe138fc29c`；merge commit：`2eee72cc44c2c7733cb565ea665ebfae48610085`。
- Story SHA 已是 `main` ancestor；E17-0 合并完成时的基线已确认 `main...origin/main = 0 0`，E17-0 本体 Review 无 finding 并完成 merge / push。
- 当 closeout 门禁尚未满足时，允许动作仅限独立 Review / merge，不得启动 E17-1；门禁满足后直接切换为由主管理生成 E17-1 提示词。

### E17-1：Band 9 与标准 HRS 重新复验

- 重新确认广播条件、`0x180D`、`0x2A37`、CCCD、notify、payload、identifier 变化和 Huawei Health 互斥。
- 只做设备与协议可行性复验，不形成 production provider 架构。
- 真机截图、日志、APK 与设备输出只进入 `.local/`；可追溯结论进入文档。
- 真实操作由用户执行；开发对话只准备 APK、日志入口和清单。
- 最终状态：`reviewed / merged`；设备/协议结论：`passed`。
- Immutable Story SHA：`b7a48b980b54e34763212699c64ce387866ec064`；merge commit：`17a305725a4241810ea4dbd26a29414c2be2582b`。
- Story SHA 已是 `main` ancestor；E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。
- Review 无 blocker、must-fix 或 should-fix。持续 notify 后顶部 `Stop / Disconnect` 不易访问，以及 debug 工具的 `currentGatt` callback / UI 共享状态未显式串行化，均为 debug-only nice-to-have，不扩展为 production 重构任务。
- 当前真机环境为 `PLU110`、Android 16 / SDK 36、HUAWEI Band 9；测试 APK SHA256 为 `60abda376470a667ec5c94d16a24e996b2e3e7033df2cc7b4dc6d4132e8dbbc7`。
- 广播关闭扫描窗口未发现标准 HRS source；四个广播开启周期均发现 `0x180D`，并完成 GATT、notify 型 `0x2A37 properties=0x10`、`0x2902`、CCCD `01 00` 和连续 notify；真实 raw payload 与当前 parser bpm 一致。Huawei Health 在广播开启时断开、广播关闭后可恢复。
- 四周期 label / address 相同仅属于本次观察，不证明永久稳定身份；两次 `status=19` 是链路不稳定事实；Band 固件与 Huawei Health 版本仍未确认；最终恢复缺少额外截图，但有周期间恢复截图与用户现场观察。
- `passed` 不证明 production provider 稳定、production 架构完成、lifecycle / reconnect 正确、自动重连可用、永久设备身份、其他手机 / 固件 / Band / Huawei Health 版本通用；AVD 不能证明真实 BLE / GATT。

### E17-2：重新定义产品范围

- 用户已确认完整产品合同，主文档为 `docs/planning/e17-2-heart-rate-product-scope.md`，决策为 D-080。
- 心率默认关闭；用户在训练偏好显式开启后是重要训练能力。权限只在主动扫描 / 连接时请求；saved device 是便利提示，用户点击后才做有限时精确匹配。
- 胶囊在 TrainFlow 前台跨页面显示，未训练只显示不记录；实时内容为 bpm、非医疗区间和用户上限视觉提示。
- 训练中心率记录、平均 / 最高心率、曲线、区间时长 / 占比、覆盖缺口、用户主动导出到电脑和由用户导入外部模型分析是接受的后续方向，但数据、分析、导出和复盘视觉审查必须另拆。
- 前台持续自动恢复及活跃训练后台断连自动恢复已确认有产品价值，Disposition 为 `accepted product value / deferred implementation / requires separate product decision refinement and Story`；初始可交付恢复基线仍为手动恢复，不恢复 D-078。
- 两张划船机截图只作为未来单次训练详情的信息层级与效果参考，不复制或提交；后续按产品 / 数据讨论 -> 数据能力 -> 独立视觉审查 -> 用户确认 -> UI 实现推进。
- 最终状态为 `reviewed / merged`；immutable Story SHA `b50778c90cf0232b08b857fda32ba6605fbef224` 已成为 `main` ancestor。本 Story 只完成产品范围定义，不进入技术架构或 production implementation。

### E17-3：最小技术架构

- 用户已确认方案 A，主文档为 `docs/planning/e17-3-heart-rate-minimum-architecture.md`，决策为 D-081。
- 一个 Application / 进程级 `HeartRateRuntimeOwner` 实现现有 `HeartRateProvider`，唯一持有 scanner、target、GATT 与 callback；settings、胶囊、Compose 和 Service 不能成为第二 owner。
- 所有动作 / callback / timer 串行到 Android main looper；generation、attempt ID 与 raw GATT identity 拒绝旧 callback；cleanup 先失效引用再幂等 stop / disconnect / close。
- Android BLE facts、用户 `HeartRateState` 与 capsule presentation 分层；permission TOCTOU 只在具体 BLE 调用处窄捕获 `SecurityException`；freshness 与 reconnect 解耦。
- malformed payload 只作为内部 diagnostic，不刷新 last-valid-sample、不续写 Live、不立即形成公共 `TechnicalFailure`；公共状态只按有效样本 freshness 或独立平台 / 连接失败事实推进。
- D-081 是 D-027 / E7.2 的窄例外和部分 supersession：普通训练继续使用 ordinary notification；只有活跃训练已有合法当前心率连接时采用 `connectedDevice` FGS，连接结束而训练继续则降级回 ordinary notification。
- 复用现有 `ActiveWorkoutNotificationController` contract 的唯一 Application-scoped production coordinator；Route 只提交状态，固定 ID `7200` 在 `NONE` / ordinary / FGS 三种概念模式间有序、幂等交接。`POST_NOTIFICATIONS` 拒绝不免除 FGS 构造并向 `startForeground()` 提交 notification 的义务。
- 训练 terminal 明确在前台时停止 FGS / 移除 `7200` 后，可在 eligibility 仍合法时保留同一条从未 cleanup 的 live attempt，并转为非训练前台只显示不记录；后台、锁屏或进程可见性不确定时必须 cleanup。两者均不自动 scan / connect / reconnect。
- 非训练后台停止，`START_NOT_STICKY`，process death 后手动恢复。用户已接受持续通知 / 系统任务管理器可见成本。
- 零新增第三方依赖；最多一个唯一 owner、一个确有需要的窄 typed BLE seam和一个 concrete Service，不构建通用 BLE framework。
- 最终状态为 `reviewed / merged`；immutable Story SHA `b09ed116558eb3537fc86985b9c39b96bbbca6ff` 已通过 merge commit `1e0a7a9cf0b118ca829a5843d066795b4420eb5f` 成为 `main` ancestor，E17-4 gate 已 satisfied。

### E17-4：Implementation readiness

- 主文档为 `docs/planning/e17-4-heart-rate-implementation-readiness.md`；真实代码盘点校正了旧 provider、空 Application composition root、Compose owner、三个 Route 通知 writer、Manifest / Service 缺口和冻结胶囊适配范围。
- 候选结论为 `ready for implementation review`；默认不新增 `AndroidBleOperations` seam，采用原生 Android BLE 类型加窄 callback identity harness。无法安全覆盖早到 callback 时必须停止并返回主管理，implementation 不得自行扩张 seam。
- freshness 采用无循环门禁：E17-5 以前台 Band 9 monotonic notify 测量形成 provisional internal threshold；E17-9 在 runnable FGS 后补测锁屏 / 临时后台余量，并在最终 presentation 启用前锁定纯 Kotlin 边界。
- implementation 拆为 E17-5 facts / freshness / presentation core、E17-6 deterministic BLE owner、E17-7 Application / settings / capsule wiring、E17-8 ordinary notification coordinator、E17-9 connected-device FGS / ID `7200` handoff、E17-10 integrated acceptance。记录、Room、分析、导出和自动恢复均排除。
- E17-6只新增并确定性测试新`HeartRateRuntimeOwner`；新owner仅test可实例化，旧provider/scanner/DTO继续维持既有production/debug接线编译并仍是唯一production可达BLE owner。E17-7在同一Story从`TrainFlowApplication`唯一创建点切换Application、Activity、Compose、settings、manual/saved-device、capsule与debug `HeartRateBroadcastSmokeActivity.kt`，再退休旧scanner/GATT ownership和旧DTO production consumer；E17-7后不得保留可重新实例化旧owner的production入口。
- E17-7 process visibility固定为main-looper reducer，以Activity identity集合及`ForegroundConfirmed` / `BackgroundConfirmed` / `ConfigurationTransition` / `Unknown`等价facts判定；configuration transition使用generation与确定性completion/timeout，重复/不平衡/mismatch/未知事件fail-closed。E17-9前active training真实后台仍cleanup。
- ID `7200`降级ack是Service在main-looper路径调用`stopForeground(STOP_FOREGROUND_REMOVE)`正常返回后发布的进程内`ForegroundWriterReleased(generation)`事实，不是系统UI删除确认。coordinator只接受当前handoff generation，冻结ordinary writer至匹配release，replay latest state；stale/duplicate/wrong-generation ack、失败与Service destroyed-before-ack不得创建第二writer。
- E17-10已收窄为evidence-only：production files/lines/methods均为0，任何`app/src/main`行为问题返回E17-6/7/8/9独立Repair并在合入同步main后重建APK、重跑受影响gate。E17-5修改debug M0工具后必须重新build/install并记录source preparation SHA、APK SHA256、设备与分层日志；任何后续影响debug APK的可执行变化立即使旧证据失效，E17-1 APK不得沿用。
- 条件式状态：E17-4 immutable SHA 尚非 `main` ancestor，或独立 Review、merge / push、`main...origin/main = 0 0`、五份文档一致性任一未完成时，E17-4 为 `implemented / needs review`，production implementation 继续 locked；全部门禁满足且 readiness 结论仍通过后，E17-4 自动视为 `reviewed / merged`、E17-5 gate 自动 satisfied。不创建状态 docs-sync，不硬编码未来 merge commit。

### E17 后续 implementation Story

D-081 已锁定最小技术架构，但不授权 implementation。只有 E17-4 reviewed / merged 且 readiness 结论通过后，主管理才能生成正式 production implementation Story；自动重连、记录、分析和导出仍需各自后续决策与 Story。

## 4. 测试分层

| 层级 | 负责验证 | 明确不能代表 |
|---|---|---|
| 纯 Kotlin 测试 | parser、policy、状态转换、presentation input/output | Android BLE wiring、真实设备 |
| Provider 边界测试 | 少量平台异常、资源关闭、生命周期与 callback ownership | 射频、真实 GATT 时序 |
| AVD | 权限、process、前后台、Bluetooth 状态、no-crash / no-ANR | Band 9 scan / connect / notify |
| Band 9 真机 | scan、connect、discover、CCCD、notify、断流、恢复 | 其他设备或厂商通用性 |

硬规则：

- 不模拟射频信号并把它当真实设备证据。
- injected callback 不是设备验证。
- AVD 不是 Band 9 验证。
- helper / 源码字符串搜索只可用于静态边界检查，不是 production behavior coverage。
- 文档只声明测试实际执行并断言过的行为。
- 人工真机操作由用户执行，开发对话准备 APK、日志与清单。

## 5. 设计和实现约束

- 默认优先直接使用原生 Android BLE 类型；Android 类型留在平台层。
- 不为测试创建第二套完整 GATT 模型。
- permission-sensitive 调用使用窄边界，并逐项定义异常语义。
- 不捕获全部 `RuntimeException` 或 `IllegalStateException`。
- 不使用异常 message / 厂商字符串分类平台状态。
- callback ownership、串行化上下文、旧 callback 拒绝和 close 顺序必须在 E17-3 明确。
- 自动重连必须有独立产品价值决策和独立 Story。
- freshness 与自动重连不得默认绑在同一 Story。
- Repair 不得为测试便利扩展 production 架构。

## 6. Adopted / frozen / direct reuse 胶囊资产

### 直接采用的实现与视觉资产

- `app/src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsule.kt`
- `app/src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateCapsuleGeometry.kt`
- `app/src/main/java/com/liujyks/trainflow/ui/theme/TrainFlowMotionTokens.kt` 中已批准的相关 motion 语义
- `docs/design/e16-3-heart-rate-ui-html/index.html`

相关 E16 design 文档本身仍属于 sealed historical archive；D-079 从中点名采用的视觉与互动资产则是 E17 当前合同，不是 reference-only、revalidate、rewrite、provisional 或待重新设计。

### 冻结行为

- 胶囊视觉设计、布局和信息层级。
- collapsed / expanded 视觉。
- 轻点、拖动、movement threshold、左右安全边吸附。
- viewport clamp、安全区避让、expanded compact fallback、IME 收缩。
- 互动动画、边缘锚定、克制 motion 和 reduce-motion 兼容方向。

E17 不重新进行胶囊视觉设计、交互设计、HTML 视觉方案、动画探索、颜色 / 尺寸 / 布局 / motion 变体或 `huashu-design` 视觉重做。

### Runtime 可重新设计

- `HeartRateFloatingCapsuleState.kt` 中的 provider 语义、状态文案、mapper 和状态优先级。
- presentation state DTO 在不改变冻结视觉合同前提下的输入结构。
- state source、provider state、事实状态与 UI 状态的映射。
- `TrainFlowApp` 中的 runtime wiring 和 lifecycle ownership。
- settings copy、manual recovery flow 与 saved-device 语义（须经 E17-2 接受）。

E17 新 provider/runtime 通过胶囊外部 presentation mapper 适配现有胶囊；不让胶囊迁就 BLE 架构，不把 Android BLE 对象传入胶囊，不在胶囊内部实现 permission、scan、connect 或 reconnect。冻结的是视觉与互动，不是旧 runtime 设计；`HeartRateFloatingCapsuleState.kt` 中混入的旧 runtime 语义不自动继承。如现有 presentation DTO 无法表达 E17 接受的必要状态，必须另开兼容性 decision，不能借此修改视觉与互动。

## 7. E17 解锁规则

1. E17-0 与其 closeout 的既有独立 Review、merge / push、immutable SHA ancestry 与同步门禁已完成；对应 Git 事实保持不变。
2. E17-1 已通过独立 Code Review，Review 无 blocker、must-fix 或 should-fix；immutable Story full SHA `b7a48b980b54e34763212699c64ce387866ec064` 已通过 merge commit `17a305725a4241810ea4dbd26a29414c2be2582b` 合入并成为 `main` ancestor，最终设备/协议结论为 `passed`，且 E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。
3. E17-1 状态 docs-sync 的既有独立 Review、merge / push、immutable SHA ancestry、`main...origin/main = 0 0` 与权威文档一致性门禁已完成，E17-2 已据此启动并取得用户合同确认。
4. 当 E17-2 最终 immutable Story SHA 尚未成为 `main` ancestor 时，只允许 E17-2 独立 Code Review / merge，不得启动 E17-3。
5. 当 E17-2 通过独立 Review、merge / push，其最终 immutable Story SHA 成为 `main` ancestor，`main...origin/main = 0 0` 且权威文档一致时，E17-3 门禁自动满足。主管理从 Git 解析 E17-2 最终 SHA 并生成 E17-3 提示词；不要求 E17-2 状态 docs-sync，也不得创建“closeout 的 closeout”。
6. E17-2 immutable Story SHA `b50778c90cf0232b08b857fda32ba6605fbef224` 已是 `main` ancestor，E17-3 gate 已满足并已完成用户方案 A 确认与 docs-only 架构。
7. E17-3 immutable Story SHA `b09ed116558eb3537fc86985b9c39b96bbbca6ff` 已通过 merge commit `1e0a7a9cf0b118ca829a5843d066795b4420eb5f` 成为 `main` ancestor；E17-3 为 `reviewed / merged`，E17-4 gate 已 satisfied。
8. 当 E17-4 immutable SHA 尚非 `main` ancestor，或独立 Implementation Readiness Review、merge / push、`main...origin/main = 0 0` 和五份文档一致性任一未完成时，E17-4 为 `implemented / needs review`，production implementation locked；只允许 E17-4 独立 Review / Repair。
9. 上述门禁全部满足且 readiness 结论仍通过后，E17-4 自动视为 `reviewed / merged`，E17-5 gate 自动 satisfied；主管理从 Git 解析最终 SHA，不做递归 closeout 或状态 docs-sync。

每一关都必须：

- 使用 prerequisite 的 immutable full commit SHA。
- `git merge-base --is-ancestor <full-sha> main` 成功。
- `main` 与 `origin/main` 同步为 `0 0`。
- Git ancestry 与 project-status / roadmap / readiness 一致。

push、Review 文本、人工测试或分支存在都不等于已合入。E16 分支、E16 Story tip 和 `89d1e23f870185a2e279d35bb293883f64fe70ba` 均不得作为 E17 解锁前置。

E17-0、E17-1、E17-2 与 E17-3 为 `reviewed / merged`。E17-4 immutable SHA 尚非 `main` ancestor，或独立 Review / merge / push、`main...origin/main = 0 0`、五份文档一致性任一未完成时，E17-4 为 `implemented / needs review`，production implementation locked；全部门禁满足且 readiness 结论仍通过后，E17-4 自动视为 `reviewed / merged`、E17-5 gate 自动 satisfied。主管理直接从 Git 解析最终 SHA，不产生额外状态 docs-sync 或递归 closeout。

## 8. E17-0 验收

- E16 历史 merge fact 被保留，umbrella 被 correct-course 关闭。
- E16-10b-2 保持 `changes requested` 且永久禁止合并。
- 胶囊 `adopted / frozen / direct reuse` 范围在 retrospective、E17 计划、decision log 与 project-status 一致。
- E16 原始代码与文档作为 sealed historical archive，不参与 E17 当前状态逐行一致性检查。
- E17-0 已 reviewed / merged；Story SHA `abce4b712139c373f534a6fabab423fe138fc29c` 已通过 merge commit `2eee72cc44c2c7733cb565ea665ebfae48610085` 成为 `main` ancestor。
- E17-0 closeout 后续已完成独立 Review / merge / push / ancestry / sync / docs consistency 门禁；未创建递归 closeout。
- E17-0、E17-1、E17-2、E17-3 已 `reviewed / merged`；E17-4 状态按第 7 节条件式门禁自动判定，本开发对话不宣称 reviewed / merged。
- E17-4 readiness 候选结论为 `ready for implementation review`；production implementation 在 E17-4 独立 Review / merge 门禁完成前继续 locked。
- 通用提示词流程包含连续 Review、Repair 结构、最小修改、Evidence、体积控制与 correct-course 职责门禁。
- 本 Story 只修改 Markdown / 根目录提示词模板，不修改生产或测试代码。
