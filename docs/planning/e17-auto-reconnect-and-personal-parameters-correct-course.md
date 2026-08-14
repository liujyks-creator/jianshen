# E17 自动恢复与个人参数 Correct-course

**状态判定：** D-082 Correct-course与E17-7a/7b已形成accepted/merged事实；E17-7b candidate `cec16f697a036409693943289d471955ef7a47bc` 由merge `e3f2de2106342e06f75c3dce7eaef562ad2a6356`合入。Validated lifecycle Correct Course V2取代ordinary-only E17-8；原子E17-8仍为`NOT READY / needs fresh Exact Story shaping/readiness`
**日期：** 2026-07-26
**性质：** 用户产品行为确认、架构边界校正、Story 重新拆分与 requirement receipt；docs-only
**Accepted base：** `24ba90ebc3fe7b4377b9fbf9c2579b6cd596167f`

## 2026-08-15 current operative supersession

本文件的eligibility、persistent suppression、个人参数、heart-rate owner与7a/7b evidence合同继续有效。E17-7b已`reviewed / merged`；其candidate SHA与merge SHA分别记录candidate identity和integration truth。Validated E17-8 lifecycle Correct Course V2只接受上游planning correction，不等于implementation `READY`；本docs-only sync不实现E17-8。

一个原子Corrected E17-8必须在同一candidate同时交付：Activity-retained active-workout runtime唯一owner；Timed/Strength/Follow-along三Route仅attach/detach；真实session identity/current producer authority；`PRE_START / STARTED_NON_TERMINAL / TERMINAL / INVALIDATED`；terminal transition exactly-once；persistence attempt at-most-once且successful record为0或1；Application ordinary ID `7200`唯一projection coordinator；configuration continuity；process-death non-recovery/fail-closed cleanup；same-plan A→B stale isolation。Application coordinator不持workout engine、不是session registry；既有Application heart-rate/GATT owner保持独立唯一，E17-9 Service不持engine/GATT。

Timed READY且`startedAt = null`永久销毁时不产生`ABANDONED`或persistence attempt；STARTED_NON_TERMINAL永久销毁固定既有`ABANDONED` exactly-once并至多attempt一次。Persistence failure不retry、upsert、queue、补写、不产生duplicate record、不新增schema/migration、不rollback或revive，identity invalidation与`7200` clear-once继续。Route dispose无terminal、permanent-destruction或cleanup权限；configuration detach不清有效通知。Process death不恢复active workout、不猜测或合成terminal/record；same-plan A的迟到command/callback/terminal/projection不得影响B。

不得存在可分别实施、Review或merge的E17-8a/E17-8b，也不得有临时owner、compatibility bridge、transient registry或未来Story补救。Workout-producer generation与E17-9 `handoffGeneration`在名称、状态、目的和验证上分离。Current sequence为：本docs sync reviewed/merged -> fresh atomic E17-8 Exact Story shaping/readiness -> future exact accepted merge -> E17-9 -> E17-10；E17-10继续evidence-only、`production changes = 0`。

## 1. Correct-course 结论

用户于 2026-07-26 明确要求 TrainFlow 在用户 opt-in 且保存过精确目标后，把自动恢复、个人心率参数和显式断开作为同一条可交付产品能力继续推进。该产品行为确认与任何旧实现路线分离：它窄 supersede D-080 中 manual-only / App start 不使用 saved target 自动恢复的冲突范围，以及 D-081 中断连后不 reconnect 的冲突范围；不恢复 D-078、E16-10b-2 controller / wrapper，也不改变 D-079 的 sealed archive 与冻结胶囊合同。

以下既有资产继续有效：Application / 进程级唯一 `HeartRateRuntimeOwner`；main-looper serialization；generation、attempt ID、raw GATT identity；permission TOCTOU；先失效 identity / 引用再取消任务并 best-effort stop / disconnect / close；runtime facts、公共状态与 presentation 分层；freshness 与 reconnect 分离；唯一 Application notification coordinator；ID `7200` 单一 writer；合法 `connectedDevice` FGS；`START_NOT_STICKY`；E17-5 / E17-6 已合并资产；D-079 `adopted / frozen / direct reuse` 胶囊。

## 2. 变更前 oracle

Accepted base 的可证伪断点为：

1. `docs/planning/e17-2-heart-rate-product-scope.md`、`docs/planning/e17-3-heart-rate-minimum-architecture.md` 与现有 E17 计划把明确状态 + 手动恢复作为 production 基线，并拒绝 App start / foreground return 使用 saved target 自动恢复。
2. 自动恢复被标为 `accepted product value / deferred implementation / requires separate product decision refinement and Story`，但现有 E17-5 至 E17-10 序列没有负责该能力的目标 Story。
3. PRD / UX 已出现年龄、最大心率和提醒阈值，但现有 readiness 没有定义参数范围、优先级、持久化 round-trip、消费方或 evidence，形成 orphan requirement。
4. 现有 E17-7 验收把 active training 在 FGS 未实现前的 background 统一写成 cleanup，容易把普通 `ON_STOP` 误当停止信号，与“活跃训练锁屏 / 临时后台维持同一连接并在意外断连时恢复”冲突。

本轮只修正当前权威规划，不重写 E17-2 / E17-3 历史正文，不修改 sealed E16 文档、代码或证据。

## 3. 恢复资格与生命周期合同

### 3.1 Eligibility

自动恢复 eligibility 必须同时满足：

- 用户已 opt-in；
- 存在用户保存的 exact target identifier；display name 仍只作提示；
- 当前 BLE permission 合法；
- Bluetooth 已开启；
- 不存在跨进程持久化的 manual suppression；
- App 明确 visible，或 active / paused training 已建立并能合法使用 `connectedDevice` FGS。

任一条件不满足时不得开始 scan / connect。名称、地址或附近其他 HRS 设备不能替代 exact target；自动恢复不得自动换 target。

### 3.2 前台与非训练后台

App 启动、process recreation 后首次进入 visible，或回到前台时，只要 eligibility 成立，就自动进入 saved exact target 恢复。意外断连、out-of-range 或暂时未发现目标后使用有间隔的 bounded scan windows；每个窗口有限时且窗口之间有间隔，policy 在 eligibility 仍成立时保持长期 armed。单轮失败、固定次数耗尽或 freshness timeout 都不得把用户永久留在“已停止自动恢复”；只有资格消失或用户停止才 disarm。

bounded-window delay、下一窗口 eligibility 复核与 recovery timing 只能实现为唯一 `HeartRateRuntimeOwner` 内的 concrete main-looper policy，并继续复用该 owner 已有的串行 action / callback / time-control 路径。禁止新增 standalone 或 generic retry scheduler、watchdog、backoff controller，禁止为 reconnect timing 创建新的 production interface、actor、wrapper 或相关抽象。测试可以使用现有确定性 main queue / time control 驱动窗口与间隔，但不得为了 fake clock、closure 或断言便利反向新增 production scheduler abstraction。

非训练进入 background 时必须停止当前 scan / connect / GATT 并 cleanup，不持续 scan。它不清除 opt-in、saved target、个人参数或正常的 armed intent；回到 visible 后重新计算 eligibility，成立则自动恢复。

### 3.3 活跃训练后台

active / paused training 从前台进入 background 或 lockscreen 时必须优先保持同一 heart-rate connection；普通 Activity / Route `ON_STOP` 本身不是 cleanup 信号。连接从未 cleanup 且未丢失时，回到前台必须继续观察同一 Application owner、同一 attempt lineage 和 current bpm。若连接在后台意外丢失且 eligibility 仍成立，唯一 owner 只能在合法 `connectedDevice` FGS 下以新 generation / attempt 自动恢复 exact target；这种恢复不得伪称“同一 attempt”，但仍不得创建第二 owner。恢复窗口期间 FGS 和 ID `7200` 唯一 writer 必须保持 active，notification 必须准确显示“正在重新连接”而不是伪报 live 或降级 ordinary。

只有 eligibility 失败、训练 terminal、显式断开、opt-out、permission loss、Bluetooth loss、用户清除 target、FGS 无法合法建立 / 维持，或 App 已明确回到 foreground 且不再需要 FGS 时，才停止训练后台恢复并 demote / stop FGS。FGS 失败时不得静默宣称后台保证；不得为了恢复启动第二 GATT owner、第二 Service owner或后台无限 scan。

## 4. 显式断开与偏好语义

设置页“断开设备”是用户停止自动恢复的明确意图：

- 立即停止 scan / connect / GATT，执行统一 cleanup；
- 保留 opt-in、saved exact target、年龄、个人最大心率与提醒阈值；
- 写入跨 process 持久化 manual suppression；
- App 重启、回前台、Bluetooth 恢复、permission 恢复和训练开始都不能清除 suppression；
- 只有用户明确点击“重新连接”或选择一个目标（包括再次选择同一目标）才清除 suppression 并重新 armed。

“清除已保存设备”删除 target 并使 eligibility 失败，但不等同 opt-out，也不删除个人参数。“关闭心率”是 opt-out，隐藏胶囊并停止能力；它与断开、清除 target 是三个不同动作。

## 5. 个人参数与 presentation 合同

### 5.1 参数

- 年龄可选，sanity 范围为 `1..130`；它不是连接或恢复资格。`101` 是有效输入，必须保存、重启 round-trip 且不得 clamp。
- 个人最大心率可选，范围 `30..260 bpm`。
- 上限提醒阈值可选，范围 `30..260 bpm`，与个人最大心率相互独立。
- effective max precedence：合法个人最大心率优先；否则合法年龄使用 `220 - age`；两者都没有时不推导区间。
- 没有 effective max 时仍显示 bpm。只有上限提醒阈值时，超过阈值仍可显示视觉提示。

### 5.2 区间与提示优先级

区间用未取整的 `bpm / effectiveMax * 100` 比率判断：

| 比率 | 非医疗区间 |
|---|---|
| `< 50%` | 低强度 |
| `[50%, 60%)` | 热身 |
| `[60%, 70%)` | 燃脂 |
| `[70%, 80%)` | 有氧 |
| `[80%, 90%)` | 无氧 |
| `>= 90%` | 极限 |

比较使用原始比率，不先四舍五入。`bpm > alertThreshold` 是严格大于，并优先于区间 presentation；相等不触发。所有状态直接复用 E16 已冻结的胶囊状态与颜色能力，不改变 capsule geometry、布局、颜色 token、collapsed / expanded、拖动、吸附或 motion。

## 6. Requirement receipt ledger

每项 requirement 必须绑定 source、accepted decision、目标 Story / AC 与 evidence。候选文档在独立 Review / merge 前只能称 `candidate`，不得自称用户确认之外的 Git acceptance；任何 deferred 项都必须有明确 target，不能只写“后续”。

| Requirement | Source | Accepted decision / boundary | Story / acceptance criterion | Required evidence |
|---|---|---|---|---|
| HR-CC-001 | 用户 2026-07-26 | 自动恢复与个人参数成为 E17 当前方向；只窄 supersede manual-only / no-reconnect 冲突 | Current docs Correct-course：十份文档一致且唯一新 decision row | 独立 docs Review、three-dot scope、D-ID / cross-doc check |
| HR-CC-002 | 用户 2026-07-26 + D-079 / D-081 + D-084 | 保留唯一heart-rate/GATT owner、main serialization、identity、cleanup、FGS、`7200`、`START_NOT_STICKY`与冻结胶囊；另由原子E17-8建立唯一workout runtime owner与唯一ordinary projection owner | E17-7a/7b已完成；原子E17-8与E17-9不得新增controller、wrapper、第二owner、registry或第三notification interface | static ownership search、retained/runtime/coordinator tests、Review diff |
| HR-CC-003 | 用户 2026-07-26 | eligibility 是 opt-in + saved exact + permission + Bluetooth + no suppression + visible 或合法 active-training FGS | E17-7a AC：纯 policy 全组合矩阵 | deterministic policy tests |
| HR-CC-004 | 用户 2026-07-26 | visible unexpected disconnect / out-of-range 使用间隔 bounded windows 且长期 armed | E17-7a AC：单窗口有限、间隔可控、失败不永久耗尽；timing 只属于唯一 owner 内 concrete main-looper policy，不新增 scheduler / watchdog / backoff abstraction | 复用现有 deterministic reducer、main queue / time control 的纯测试；不包含 AVD 或 Android lifecycle claim |
| HR-CC-005 | 用户 2026-07-26 | 非训练 background cleanup、不持续 scan；回前台 eligible 自动恢复 | E17-7b AC：Home / return / process recreation；mapper / copy / semantics区分自动恢复等待下一bounded window、自动恢复正在查找、单窗口未匹配但仍armed且之后eligible继续，以及用户主动扫描结果；自动状态不得冒充用户主动扫描、手动停止或永久停止 | E17-7b final executable source 的 AVD lifecycle / scan-count / cleanup / foreground-return状态证据，覆盖上述自动恢复三态并证明其与用户主动扫描相区分 |
| HR-CC-006 | 用户 2026-07-26 + D-084 | active training background / lockscreen保持同一heart-rate连接；意外断连在合法FGS下恢复；workout runtime ownership不转移给Service | E17-9 AC：前置为原子E17-8 exact accepted merge；普通`ON_STOP`不直接cleanup；Service不持workout engine/GATT；`handoffGeneration`不复用workout-producer generation | final executable source的AVD lifecycle/notification与Band 9真实GATT/notify分层证据；另证明owner/generation边界 |
| HR-CC-007 | 用户 2026-07-26 | 只有 eligibility失败、terminal、manual disconnect、opt-out、permission / Bluetooth loss、target clear、FGS非法，或明确foreground不再需要FGS时才demote / stop | E17-7a / 9 AC：停止原因矩阵且幂等；unexpected disconnect本身不demote | owner policy tests、Service tests、Band negative gates |
| HR-CC-008 | 用户 2026-07-26 | explicit disconnect 保留 opt-in / target / parameters，并持久 suppress 到 explicit reconnect / select | E17-7a data AC + E17-7b settings AC | DataStore round-trip、process recreation、UI action tests |
| HR-CC-009 | 用户 2026-07-26 | disconnect、clear target、opt-out 三种语义分离 | E17-7b AC：三个独立 control 与准确 copy | Compose semantics / mapper tests、AVD |
| HR-CC-010 | 用户 2026-07-26 | age optional `1..130`、非 eligibility；`101` 有效且不 clamp | E17-7a data AC | boundary + `101` restart round-trip tests |
| HR-CC-011 | 用户 2026-07-26 | personal max optional `30..260` 且优先于 age estimate | E17-7a policy / data AC | `30/260` boundaries、precedence tests |
| HR-CC-012 | 用户 2026-07-26 | alert threshold optional `30..260`、独立；无 effective max 时 bpm-only，alert-only 仍可提示 | E17-7a presentation AC | parameter cross-product mapper tests |
| HR-CC-013 | 用户 2026-07-26 | effective max 为 personal max，否则 `220-age`，否则 none | E17-7a AC | pure calculation tests including age `101` |
| HR-CC-014 | 用户 2026-07-26 | 未取整比率使用六段 `[50,60,70,80,90]` 边界 | E17-7a presentation AC | exact-below / exact / exact-above ratio tests |
| HR-CC-015 | 用户 2026-07-26 + D-079 | strict `bpm > alert` 优先，直接复用冻结状态 / 颜色，不改胶囊 | E17-7a mapper + E17-7b wiring AC | equality / exceed tests、capsule geometry / visual boundary regression |
| HR-CC-016 | 用户 2026-07-26 | `fda5f7cfd3c31af3399dfe231733ea00467a68e8` frozen / unmergeable / reference-only / permanently prohibited from merge；E17-5 / 6 保留 | Current docs + all downstream preflight | ancestry / scope check；禁止整段 cherry-pick |
| HR-CC-017 | 用户2026-07-26 + validated lifecycle V2 | 7a/7b已完成；current sequence为docs sync -> fresh atomic E17-8 shaping/readiness -> exact merge -> E17-9 -> E17-10 | readiness / roadmap / D-084 contracts | prerequisite full-SHA ancestry、AC-to-evidence matrix；不允许8a/8b独立节点 |
| HR-CC-018 | 用户独立硬要求2026-07-26 + Git truth | E17-7b candidate/merge已是Accepted base ancestors；Correct Course `VALIDATED`不等于E17-8 implementation `READY` | Current docs sync + project status index | candidate/merge ancestry、八文档一致；docs sync后fresh Exact Story readiness |

## 7. 新 implementation 序列

| Story | 单一主要风险轴 | 最小范围 | 关键 merge gate |
|---|---|---|---|
| Current docs sync | 将E17-7b Git truth与validated lifecycle V2同步为accepted current state | 本次八份Markdown；不实现E17-8 | 独立Review + merge/ancestry/sync + 八文档一致 |
| E17-7a / E17-7b | 已完成owner policy/data与Application/settings/capsule wiring | 保留既有identity-bound evidence | `reviewed / merged`；candidate/merge ancestry见current status |
| Atomic E17-8 Retained Runtime + Ordinary Coordination | 同时闭合workout runtime lifetime与ordinary ID `7200` projection lifetime | retained owner、三Route attach/detach、identity/四态/two gates、Application coordinator、recreation/process death/A→B | 当前`NOT READY`；先fresh Exact Story shaping/readiness；未来同一candidate完整Review/merge，不得8a/8b拆分 |
| E17-9 Connected-device FGS + Training Background Recovery | 合法FGS、训练后台retain/reconnect、`7200` handoff、M1 | Service/Manifest/coordinator policy/shared-owner observer/final freshness；不持engine/GATT | 前置为atomic E17-8 exact accepted merge + evidence/status一致；`handoffGeneration`独立 |
| E17-10 Integrated Production Acceptance | 已合并能力的端到端证据 | evidence-only；`production changes = 0` | production finding返回责任Story，不得在E17-10修补 |

7a与7b的独立历史Review/merge保持。Lifecycle V2只改变后续current contract：原子E17-8必须同时交付retained runtime owner与ordinary coordinator，不得拆成8a/8b，也不得把ownership正确性留给E17-9。E17-9只负责already-authorized FGS/handoff/training-background boundary；E17-10仍不得修改production。

## 8. 冻结候选与禁止路线

Candidate `fda5f7cfd3c31af3399dfe231733ea00467a68e8` 的 disposition 固定为：

```text
frozen / unmergeable / reference-only /
permanently prohibited from merge
```

它不是 prerequisite，不是 accepted base，不得整体 cherry-pick、merge、rebase 进当前路线，也不得恢复其中的 E16 controller / wrapper 或以其代码替代 E17-5 / E17-6 已合并资产。需要的产品事实必须由本 correct-course 文档与新 Story 合同独立表达；实现必须从同步 `main` 和已接受 owner / policy 边界开发。

## 9. Merge-stable truth

E17-7b current Git truth已经固定：candidate `cec16f697a036409693943289d471955ef7a47bc`，merge `e3f2de2106342e06f75c3dce7eaef562ad2a6356`。Branch name、push成功、候选自述、人工反馈或Review文本不能替代full-SHA ancestry。

Lifecycle Correct Course V2的`VALIDATED`只接受planning replacement；本docs sync merge后仍不得把原子E17-8称为implementation `READY`。主管理必须从同步`main`建立fresh Exact Story shaping/readiness；只有未来原子E17-8 exact accepted merge full SHA成为同步`main` ancestor且required evidence/status一致，才满足E17-9 prerequisite。E17-10保持production 0。

## 10. 非目标与验收

- 本轮不修改 Kotlin、测试、Manifest、Gradle、prototype、assets 或 `.local`。
- 不修改 E17-2 / E17-3 历史正文或 sealed E16。
- 不实现 Room 心率记录、复盘、导出、AI 分析、医疗告警、声音 / 震动强制提醒、自动暂停或训练中断。
- 不改胶囊视觉 / geometry / motion，不新增 BLE framework、controller、wrapper、第二 owner、第二 GATT model 或第三 notification interface。
- 本轮docs sync验收要求：唯一新decision `D-084`；E17-7b candidate/merge truth；atomic replacement与fresh-readiness gate；八份文档一致；UTF-8 without BOM/NUL；Markdown heading/fence/table结构有效；exact scope。不得声称E17-8 implementation `READY`。
