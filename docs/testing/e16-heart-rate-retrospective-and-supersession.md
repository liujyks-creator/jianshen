# E16 心率支线回顾与替代说明

**状态：** E16 已由 correct-course 关闭；sealed historical archive / reference only；由 E17 重新规划
**日期：** 2026-07-12
**E17-0 状态引用：** `reviewed / merged`；immutable Story SHA `abce4b712139c373f534a6fabab423fe138fc29c`；merge commit `2eee72cc44c2c7733cb565ea665ebfae48610085`
**Closeout 门禁引用：** immutable SHA 尚未成为 `main` ancestor 时仅允许独立 Review / merge；Review、merge / push、ancestry、`main...origin/main = 0 0` 与权威文档一致性全部满足后门禁自动满足，并由主管理生成 E17-1 提示词；不需要也不得创建“closeout 的 closeout”

## 1. E16 历史定位

E16 是 TrainFlow 对标准 BLE Heart Rate Service、HUAWEI Band 9、心率设置流程、浮动胶囊和 freshness / reconnect 的历史探索与实现支线。已经合入 `main` 的 Story、commit、测试和文档继续作为 Git 历史事实保留，不删除、不改写。

`reviewed / merged` 只表示某个历史 Story 在当时范围内通过并合入，不表示 E17 自动继承其产品决策、runtime 架构、状态模型、测试结论或验收结果。E16 整体没有形成一条可以继续扩展的稳定心率基线，因此 umbrella disposition 为：

```text
closed by correct-course / superseded by E17
```

E16 不得描述为正常完成、release-ready 或 E17 implementation-ready。

### 1.1 Sealed historical archive 规则

E16 原始代码及其 testing、planning、design 文档整体定义为 `sealed historical archive / reference only`。sealed 表示：E17 不再逐行修改这些原始材料；其中当时的 `current`、`next`、`depends on` / dependency、`unlocked`、`in progress` 等措辞只代表文档创建时的历史时间点，不要求与 E17 当前状态逐行一致，不能生成当前任务、解锁 E17 Story，旧执行入口也不能覆盖 D-079 和 E17 当前权威文档。Review 不得仅因 sealed archive 保留历史措辞而提出 E17 当前状态 finding；未经新的明确 correct-course decision，不得修改 sealed archive。

sealed 不表示删除 Git 历史或分支，不否认历史测试、Band 9 / HRS 证据，也不把旧实现认定为 E17 当前合同或自动复用旧 production 架构。

当前文档权威层级从高到低为：

1. `AGENTS.md`
2. accepted superseding decision D-079
3. 当前 E17 权威文档：`docs/project-status.md`、`docs/planning/e17-heart-rate-correct-course.md`、`docs/readiness-report.md`、`docs/roadmap-backlog.md`、`docs/architecture.md`
4. 本 retrospective / supersession 索引
5. sealed E16 原始文档与代码（historical evidence only）

低层材料不得覆盖高层决策或当前状态。

## 2. E16 取得的成果

### 2.1 历史设备与协议事实

- Band 9 开启心率广播、与 Huawei Health 断开占用后，可以作为第三方标准 BLE HRS 设备被发现。
- 历史真机证据包含 Heart Rate Service `0x180D`、Heart Rate Measurement `0x2A37`、CCCD `0x2902` 写入成功和连续真实 bpm notify。
- 广播设备 label / identifier 与配对视图可能变化，不能把显示名或一次地址当作永久设备身份。
- 标准 payload parser 积累了 8-bit / 16-bit bpm、flags、sensor contact、energy expended、RR interval 和 malformed payload 的处理经验。

这些是 E16 历史证据，不是 E17-1 真机验收完成。

### 2.2 产品与操作经验

- 默认关闭、显式 opt-in、权限 rationale、Android 12+ BLE 权限和 Android 11 以下 scan compatibility 的经验。
- 用户主动 bounded scan、HRS candidate、device picker、saved identifier / display name、manual reconnect 的经验。
- saved device 是偏好而不是在线事实；scan state 与 active provider state 需要分离。
- 未训练只显示不记录、非医疗表达、系统 overlay 禁止等边界得到验证。
- freshness、stale/offline、retry budget 与 manual recovery 的策略探索为 E17 提供反例和参考。

### 2.3 视觉与互动成果

- App shell 内浮动心率胶囊的视觉设计、布局和信息层级。
- collapsed / expanded 形态与轻量信息面板。
- 轻点、拖动、movement threshold、左右吸附、viewport clamp、安全区避让和 IME 收缩。
- 受控 width / layout morph、边缘锚定和克制 motion 表现。
- 已批准的 HTML 高保真方案及 720x1280 no-overlap 思路。

### 2.4 证据分层

| 证据层 | E16 已有内容 | 能证明什么 | 不能证明什么 |
|---|---|---|---|
| 历史文档 / Git | Story 文档、decision、merge commit、失败分支 diff | 当时做过什么、当时接受过什么 | E17 已接受或现架构仍正确 |
| 自动测试 | parser、policy、mapper、geometry、provider boundary、AVD smoke | 被实际断言的纯逻辑、边界与无崩溃行为 | 射频、真实 GATT 时序、Band 9 恢复 |
| AVD / injection | 权限、Bluetooth 状态、前后台、process、fake callback | Android 非射频流程和 deterministic failure path | 真实外设 discover / CCCD / notify / reconnect |
| Band 9 真机 | `0x180D` / `0x2A37` / CCCD / bpm，E16-9 手动恢复路径 | 当时设备与 APK 条件下的真实 BLE 路径 | E17 acceptance、自动重连或新架构正确性 |

## 3. E16 出现的问题

E16-10b-2 的范围失控。失败分支相对 `origin/main` 涉及 20 个文件、约 2,293 行新增和 435 行删除；controller、provider、permission、scanner、GATT、callback、lifecycle、scheduler、测试和状态文档集中在同一个 Story。

主要问题：

- 局部 Review Fix 不断引入新的核心抽象，而不是在已批准结构内最小修复。
- generic boundary 吞并异常，模糊 permission / Bluetooth 状态竞争与 programming error。
- typed BLE/GATT wrapper 复制平台对象边界，显著扩大了小型 App 的复杂度。
- callback wrapper / identity gate 存在初始化和延迟 callback 竞态风险。
- characteristic / descriptor 的 permission failure 映射曾遗漏。
- fake scheduler 可能执行 no-op helper，却被描述成 race / cancellation 的 production 证据。
- production coverage 与文档声明多次不一致，随后依靠更大的测试 seam 修补声明。
- “最小修改”没有变成可执行硬门禁，Repair 可以继续增加 interface、wrapper、ownership 层和测试模型。
- Review 提示词持续膨胀，历史 finding 被反复复制，提示词逐渐替代架构与 test matrix。
- 对一个小型本地优先训练 App，runtime 复杂度和维护成本已经超过当前产品价值。

## 4. 为什么停止修复

停止不是因为标准 HRS 不可行，不是因为 Band 9 无法连接，也不是因为浮动胶囊设计失败。E16 已经留下这三项正向历史证据。

停止的原因是 runtime 架构、Story 拆分、测试边界和 Review 流程同时失控。继续在旧分支上修复会继续扩大核心抽象、callback ownership 和平台 seam；风险与成本高于从产品范围、设备复验、最小架构和 implementation readiness 重新开始。

## 5. E16-10b-2 最终处理

- Story 状态：`changes requested`
- Disposition：`superseded by E17 / permanently prohibited from merge`
- 分支：`codex/e16-10b-2-foreground-reconnect-controller`
- Immutable tip：`89d1e23f870185a2e279d35bb293883f64fe70ba`
- 2026-07-12 核验：该 tip **不是** `main` ancestor（`git merge-base --is-ancestor ... main` exit code `1`）。

永久处理规则：不删除分支，不 rebase，不 reset，不强推，不 cherry-pick，不合并，不改写该分支历史。它只作为失败路线的只读审计材料。

## 6. 旧后续 Story 处理

- E16-10b-3、E16-10b-4：保留历史 `locked / not started`，旧路线终止。
- E16-11、E16-12：旧规划不自动进入 E17，必须重新评估产品价值、数据边界与验收方式。
- E16 umbrella：`closed by correct-course / superseded by E17`。
- 已合入 Story 的历史 merge fact 保持不变，但不构成 E17 解锁或继承事实。

## 7. E17 遗产分类

| 资产 / 能力 | 来源 Story / 文件 | 历史证据 | 分类 | E17 处理方式 | 所需重新验收 |
|---|---|---|---|---|---|
| 胶囊视觉设计与信息层级 | E16-3a、E16-8；`HeartRateFloatingCapsule.kt` | HTML、AVD、真机反馈 | adopted / frozen / direct reuse | E17 原样直接采用 | E17 runtime 接线后只验兼容与无回归 |
| collapsed / expanded 与互动动画 | E16-3a、E16-8；motion tokens | HTML、AVD、UI tests | adopted / frozen / direct reuse | 直接采用形态、克制 morph 和相关 motion | reduce-motion / 新状态适配回归 |
| 拖动、movement threshold、左右吸附 | `HeartRateFloatingCapsule.kt`、`HeartRateCapsuleGeometry.kt` | geometry tests、AVD | adopted / frozen / direct reuse | 直接采用行为 | 小屏、左右边缘和误触回归 |
| viewport clamp、安全区、IME 收缩 | 同上及 shell overlay host | bounds evidence、AVD | adopted / frozen / direct reuse | 直接采用行为 | 训练页 / confirm-record / IME 回归 |
| 已批准 HTML 高保真方案 | `docs/design/e16-3-heart-rate-ui-html/` | interactive prototype | adopted / frozen / direct reuse | 作为 E17 已采用视觉资产 | 不重新做视觉评审 |
| `HeartRateFloatingCapsuleState.kt` presentation / runtime DTO | E16-8 / E16-9 | mapper tests | not frozen / compatibility decision if needed | 不自动继承旧 runtime 语义；外部 mapper 优先 | 新 mapper 定义后重测 |
| provider 状态、文案、mapper、优先级 | E16-9；`HeartRateFloatingCapsuleState.kt` | unit + Band 9 历史反馈 | runtime / redesignable | E17-2 / E17-3 重新定义 | 产品语义、状态转换、真机一致性 |
| Band 9 标准 HRS 可行性 | E16 retest / E16-1 / E16-2 | 真实设备 `0x180D` / `0x2A37` / CCCD / bpm | revalidate | E17-1 重新复验 | 新 APK、新清单、用户真机证据 |
| payload parser | `HeartRateMeasurementParser.kt` | JVM tests + 历史 notify | revalidate | 作为候选最小纯 Kotlin资产 | parser matrix + E17-1 payload |
| opt-in / permission / privacy | E16-4 至 E16-6 | 文档、AVD、manifest tests | revalidate | E17-2 重新确认产品范围 | 产品 review + AVD 权限矩阵 |
| scan / picker / saved device / manual reconnect | E16-7 / E16-9 | unit、AVD、Band 9 历史验收 | revalidate | 重新评估是否保留及最小语义 | E17-2 产品 + E17-3 架构 + Band 9 |
| production BLE provider | `AndroidBleHeartRateProvider.kt` | unit、AVD、历史真机 | rewrite | 不作为默认正确基线 | 新架构 review、provider tests、Band 9 |
| abstract provider/model boundary | `BleHeartRateProviderBoundary.kt`、`HeartRateState.kt` | JVM tests | revalidate | 检查是否适合 E17 状态事实 / presentation 分离 | E17-3 consistency review |
| freshness policy | E16-10a / E16-10b-1 | policy 文档与 JVM tests | reference-only | 不自动继承 10/15/30 与 reason model | E17-2 价值 + E17-3 policy test |
| 自动重连方案 B | D-078 / E16-10a | 仅策略与失败实现分支 | reject | 不作为默认前置；必须独立价值决策 / Story | 独立产品决策、架构和真机验收 |
| E16-10b-2 controller / wrappers | failed branch `89d1e23...` | review findings、injected tests、AVD | reject | 永久禁止合并；只作反例 | 不验收，不移植 |
| E16-11 / E16-12 记录与分析规划 | 历史 backlog / visual reference | docs-only | reference-only | E17 不自动继承 | 如重启需新产品、数据、隐私和视觉 gate |

心率胶囊视觉与互动的正式状态是 `adopted / frozen / direct reuse`，不是 reference-only、revalidate、rewrite、provisional 或待重新设计。E17 直接采用 `HeartRateFloatingCapsule.kt` 的视觉与互动，以及 `HeartRateCapsuleGeometry.kt` 的 geometry、clamp、safe-zone 和 snap 行为；范围包括 collapsed / expanded 视觉、信息布局与层级、拖动和 movement threshold、左右吸附、viewport clamp、status bar / bottom nav / 训练固定区域避让、IME 收缩、展开 / 收起 / 移动 / 吸附动画、已批准 HTML 高保真方案与相关 motion。E17 不再进行胶囊视觉或交互设计、HTML 视觉方案、动画探索、颜色 / 尺寸 / 布局 / motion 变体，也不使用 `huashu-design` 重做这些资产。

冻结的不是 runtime 语义。E17 新 provider/runtime 必须通过胶囊外部的 presentation mapper 适配现有胶囊，不让胶囊迁就 BLE 架构，不把 Android BLE 对象传入胶囊，也不在胶囊内部实现 permission、scan、connect 或 reconnect。provider state、状态文案、优先级、state source 和 runtime wiring 均可重新设计；`HeartRateFloatingCapsuleState.kt` 中混入的旧 runtime 语义不自动继承。如现有 presentation DTO 确实无法表达 E17 接受的必要状态，必须另开兼容性 decision，不能借此修改视觉与互动。

## 8. 历史教训

E17 不得重复以下做法：

- 用越来越长的提示词代替清晰架构、Story contract 和 test matrix。
- 在 Repair 中新增核心 interface、平台 wrapper、callback ownership 层或数据模型。
- 未列出 production 修改清单就开始修复，或以测试便利扩大 production 架构。
- 用源码字符串搜索、helper 存在、helper 被调用来声称行为或 production coverage。
- 把可能 no-op 的 fake scheduler / canceled closure 调用描述为真实 race 证据。
- 把 injection、fake GATT、AVD 或模拟状态写成真实 BLE / Band 9 验证。
- 把 reviewed / merged 的历史 Story 写成后续路线必须继承的设计。
- 把 freshness 和自动重连默认捆绑进同一 Story。
- 在同一 Story 同时解决 controller、provider、permission、scanner、GATT、callback、lifecycle、scheduler 和文档。

E17 必须让产品范围、设备证据、架构、测试层级、Story 拆分和 readiness 先一致，再生成 production implementation Story。
