---
workflowType: roadmap-backlog
projectName: TrainFlow
documentLanguage: zh-Hans
status: draft
date: 2026-05-26
inputDocuments:
  - docs/planning/prd.md
  - docs/planning/ux-design.md
  - docs/planning/data-contracts.md
  - docs/architecture.md
stepsCompleted:
  - requirements-inventory
  - milestone-breakdown
  - epic-and-story-draft
  - acceptance-sequencing
---

# TrainFlow 路线图与 MVP Backlog


## 2026-07-26 E17 历史交付序列（non-operative / historical / superseded）

本段自此标题起至下一个独立的 E17 remainder V11 canonical roadmap 标题之前，仅保存历史事实；不得生成当前任务，也不得覆盖 V11 canonical roadmap。

1. 当前 docs Correct-course：18 项 receipt 与十文档一致。
2. E17-7a Reconnect + Parameter Foundation：owner policy、长期 armed eligibility、persistent suppression、age / max / alert data、zone / alert presentation 与 tests / architecture evidence。
3. E17-7b Application / Settings / Capsule Wiring：唯一 owner activation、旧 runtime retirement、disconnect / reconnect / clear / opt-out、AVD / Band basic。
4. E17-8 ordinary ID `7200` coordinator；E17-9 合法 FGS + active-training retain / recovery + handoff + M1；E17-10 evidence-only、production 0。

每项均须前序 immutable full SHA 已 Review / merge / ancestry / sync。当前 candidate 在十文档一致前 needs review / 7a gated，满足后自动 satisfied，无 docs-sync。保留 E17-5 / 6；禁止 `fda5f7cfd3c31af3399dfe231733ea00467a68e8` merge、整体 cherry-pick 或 prerequisite。下文旧 E17-7 单 Story / manual-only / deferred-without-target 状态均为历史快照。

## E17 remainder V11 canonical roadmap（当前 authoritative）

E17 remainder 的唯一 detailed Story / AC / evidence / owner / schema / DAG authority 是 `docs/planning/e17-remainder-epic-story-plan.md`。其 source 为 `INLINE-E17-REMAINDER-EPIC-STORY-PLAN-V11`（`SHA-256=6A92D46A835B637DDFBB9DEC09A661D72736768C07FD16866F88AAF62EAB8736`），已通过 re-Planning Review Attempt 5（`SHA-256=92C11E019EFEBA016C9E3DFCC0FECCADD2B902A8FD785A9048D850A9CAD8570B`，`PASS`）和 scoped Consistency re-Audit Attempt 2（`SHA-256=39FB55004A24A331BAB078BF02D546CDC749836DCCDF7830B5F58E25DF7C8541`，`PASS / CONSISTENCY=PASS`）。以下只保存机械 roadmap 摘要，不在本文件复制或改写 AC：

- Story set：`CS-01`、`CS-02`、`CS-03`、`CS-04`、`CS-05`、`CS-06`、`CS-07`、`CS-08`、`CS-09`、`CS-10`、`CS-11`、`CS-12`，共 12 nodes。
- Roots=`CS-01/CS-03`，sink=`CS-12`，materialEdges=`28`，uniqueEdges=`28`，acyclic=`true`，orphans=`0`，longestMaterialPath=`8`。
- 8-node witness：`CS-03 -> CS-04 -> CS-05 -> CS-06 -> CS-09 -> CS-10 -> CS-11 -> CS-12`；`CS-06 -> CS-09` 是显式 material edge。
- Material graph：`CS-01 -> CS-02`；`CS-01, CS-02, CS-05 -> CS-06/CS-07/CS-08`；`CS-03 -> CS-04 -> CS-05`；`CS-05, CS-06 -> CS-09`；`CS-03, CS-05, CS-06, CS-07, CS-08, CS-09 -> CS-10`；`CS-06, CS-07, CS-08, CS-09, CS-10 -> CS-11`；`CS-02, CS-10, CS-11 -> CS-12`。
- 实施顺序由上述 material DAG 和 canonical Story prerequisites 唯一约束；tracked planning sync 完成后，主管理一次只从当前 ready nodes 中选择一个 exact Story。第一个 Story 必须从 roots=`CS-01/CS-03` 选择，不得跳过 predecessor、并行假定两个 root 已完成或把 roadmap 顺序改写成新 owner。
- `U-A`、`R-A`、`CC-D03-B`、`P-BALANCED-V2` 及 CS-03 / CS-05 / CS-09 / CS-12 唯一责任边界均以 canonical 为准；CS-10 / CS-11 consumer contract 不得在摘要中重写。
- 当前状态是 `TRACKED_PLANNING_SYNC_CANDIDATE / NOT_IMPLEMENTATION_READY`。只有本 docs-sync 通过 fresh 独立 Review、合并、推送并成为同步 `main` / `origin/main` ancestor 后，才允许主管理选择第一个 exact root Story。当前没有任何 CS Story implementation 或 runtime evidence 完成。

本节以下旧 E17 roadmap、E16-11 / E16-12 “historical not started / superseded”以及“记录 / 分析 / 导出仍未规划”的带日期文字均为 `non-operative / historical`；它们保留历史事实，但不能覆盖 V11 accepted canonical roadmap。

**文档状态:** 首版拆分草案  
**用途:** 将 PRD、UX、数据契约和 Android 架构拆成可执行里程碑、Epic、Story 与验收顺序。  
**范围:** Android MVP 与当前 React/Vite 原型承接。原始 Android MVP backlog 曾不包含真实可穿戴设备接入；D-080 已将用户显式 opt-in 后的标准 HRS 心率设备与冻结胶囊显示纳入当前 E17 产品范围，D-081 已确认唯一进程 owner + 活跃训练 `connectedDevice` foreground service 的最小架构，D-082 已接受 exact-target 自动恢复、persistent manual suppression 与个人参数。E17-4/5/6已reviewed/merged，E17-4 readiness=`passed`；本Correct-course与E17-7a prerequisite按页首统一条件式真值判定。全天候健康监测、Health Connect 历史同步、医疗告警和通用设备兼容承诺仍不在本范围；E17 remainder 的记录、分析与用户导出已经 V11 接受为 CS-01 至 CS-12 的计划合同，但尚未实现，详细边界只以 canonical 计划为准。后续商业化、云同步和完整课程平台仍不在本范围。

## 1. 需求库存

### 1.1 功能需求来源

| 来源 | 覆盖范围 |
|---|---|
| `FR-001` 到 `FR-002` | 首页、入口、计划与历史入口。 |
| `FR-010` 到 `FR-013` | 训练计划创建、阶段、管理、提醒。 |
| `FR-020` 到 `FR-025` | 计时训练配置、执行、提醒。 |
| `FR-030` 到 `FR-037` | 力量训练配置、执行、记录、替换动作。 |
| `FR-040` 到 `FR-042` | 跟练雏形与内容扩展字段。 |
| `FR-050` 到 `FR-054` | 动作库接口、字段、教学、能力标签、恢复与替代映射。 |
| `FR-060` 到 `FR-062` | 训练控制命令与训练事件。 |
| `FR-065` 到 `FR-068` | E17 心率产品合同：默认关闭；显式开启后前台跨页面显示 bpm / 非医疗区间 / 上限视觉；D-082 exact-target自动恢复与显式断开suppression分E17-7a / 7b / 9交付；记录、复盘和用户导出另列后续阶段。 |
| `FR-070` 到 `FR-081` | 训练总结、历史趋势、主观反馈、恢复建议。 |

### 1.2 非功能需求

- 训练计时和状态恢复稳定。
- 运动中可读性高。
- 默认值足够好，高级设置不阻塞开始训练。
- 权限、健康数据和设备能力边界清楚。
- Android 首版保留未来 iOS 业务边界。

### 1.3 首版不做

- 云同步、账号体系、社区、排行榜。
- 完整课程运营平台。
- 自动语音教练。
- AI 实时动作纠错。
- 医疗级心率告警。
- 完整饮食与体重管理。
- Android 与 iOS 同期交付。

## 2. 里程碑

| 里程碑 | 目标 | 退出标准 |
|---|---|---|
| M0 工程与架构地基 | 建立 Android 工程、模块边界、基础模型和本地持久化骨架。 | 工程可运行，核心模块存在，基础模型与 Room smoke test 通过。 |
| M1 动作库与计划基础 | 导入首批动作 fixture，实现动作库、计划模型、计划列表和详情。 | 可浏览动作，可保存/读取计划。 |
| M2 计时训练闭环 | 完成计时计划编辑、执行引擎、提醒事件和总结。 | 用户可完成一套计时训练。 |
| M3 力量训练闭环 | 完成力量计划编辑、单组状态机、确认记录、休息与总结。 | 用户可完成一套力量训练并看到记录差异。 |
| M4 历史、恢复与偏好 | 完成历史记录、趋势基础、恢复建议、提醒与训练偏好。 | 训练后可查看总结、恢复建议和基础历史。 |
| M5 跟练雏形与健康数据边界 | 完成跟练雏形视图、媒体位，并保留未来健康数据模型边界。 | 跟练页不是空壳；首版不因心率设备不可用而影响训练。 |
| M6 MVP 硬化与验收 | 稳定性、权限说明、状态恢复、回归测试、alpha 包准备。 | MVP 验收清单通过，准备用户试用。 |

## 3. Epic 总览

| Epic | 名称 | 主要里程碑 |
|---|---|---|
| E0 | Android 工程与架构地基 | M0 |
| E1 | 动作库与内容基础 | M1 |
| E2 | 训练计划创建与管理 | M1-M2 |
| E3 | 计时训练执行闭环 | M2 |
| E4 | 力量训练执行闭环 | M3 |
| E5 | 训练总结、历史与恢复建议 | M4 |
| E6 | 跟练雏形与健康数据边界 | M5 |
| E7 | 通知、声音、震动与偏好 | M2-M6 |
| E8 | 设计系统、UI Shell 与开源定制边界 | M0-M6 |
| E9 | MVP 验收与发布准备 | M6 |
| E10 | 训练模式边界与执行页交互修正 | 用户测试后续 |
| E11 | 心率数据源策略与设备接口边界 | 用户测试后续 |
| E12 | 真实记录、统计图表与趋势分析 | 用户测试后续 |
| E13 | 声音提示、固定女声 cue 与音频共存 | 用户测试后续 |

## 4. Epic 与 Stories

## Epic E0: Android 工程与架构地基

目标：把 React/Vite 原型之外的 Android 生产工程搭起来，并让核心模型和架构边界可测试。

### Story E0.1: 创建 Android 生产工程

作为开发者，  
我想创建 Android 原生工程和基础模块，  
以便后续功能在生产 App 中实现。

**验收标准:**

- Given 仓库根目录没有生产 Android 模块，When 创建工程，Then 新增 Android App 工程可在本机打开和构建。
- Given 工程创建完成，When 运行空壳 App，Then 能进入基础首页占位。
- Then 不删除或替代现有 `prototype` 目录。

### Story E0.2: 建立模块与包边界

作为开发者，  
我想按架构文档建立核心模块和 feature 边界，  
以便训练业务、数据层和平台能力解耦。

**验收标准:**

- Given 架构文档定义模块边界，When 初始化模块，Then 至少存在 `app`、核心模型/数据/领域/执行引擎边界和首批 feature 包。
- Then UI 模块不能直接依赖 Room entity。
- Then 平台适配不能反向依赖 feature UI。

### Story E0.3: 映射核心 Kotlin 模型

作为开发者，  
我想把数据契约映射为 Kotlin 模型，  
以便计划、会话、动作和事件有统一类型。

**验收标准:**

- Given `docs/planning/data-contracts.md`，When 编写 Kotlin model，Then 覆盖 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState`。
- Then 模型不依赖 Android UI。
- Then 与首版非目标相关的字段只保留扩展位，不启动功能。

### Story E0.4: 建立 Room 与 DataStore 基础

作为开发者，  
我想建立本地数据库和偏好存储，  
以便计划、动作和训练记录可以持久化。

**验收标准:**

- Given 初始数据库，When 启动 App，Then 能创建数据库并通过 smoke test。
- Then 训练偏好保存在 DataStore。
- Then 计划快照字段预留。

## Epic E1: 动作库与内容基础

目标：先建立动作库接口和首批 fixture，支持计划编辑、动作详情、跟练和恢复映射。

### Story E1.1: 定义首批动作内容切片

**状态:** Done in `docs/planning/action-content-slice.md`

作为产品负责人，  
我想确定首批动作清单和内容深度，  
以便动作库导入有明确边界。

**验收标准:**

- Given PRD 和数据契约，When 输出首批动作清单，Then 覆盖徒手、哑铃、杠铃、单侧、热身、拉伸、可替代、可跟练等模型差异。
- Then 首批数量建议为 8 到 12 个。
- Then 明确每个动作的必填内容和可后补媒体。

**交付结果:**

- 首批动作数量确定为 11 个。
- 文档明确每个动作的训练类型适配、身体部位、器械、难度、设置指导、执行提示、安全注意、常见错误、计时默认建议和力量默认建议。
- 文档补充与 `Exercise` contract 对齐的必填字段、内容边界、审核标准、恢复区域 ID 草案和后续 fixture 未决事项。

### Story E1.2: 导入动作 fixture

**状态:** Done in Android fixture and validation tests

作为用户，  
我想在动作库中看到首批动作，  
以便创建训练计划时选择动作。

**验收标准:**

- Given 首批动作 fixture，When 打开动作库，Then 可看到动作名称、部位、器械、难度和能力标签。
- Then 每个可选动作都有 `shortCue`。
- Then 未满足必填字段的动作不进入可选列表。

**交付结果:**

- Android 侧新增首批 11 个 `Exercise` fixture，并保留 fixture-only 训练类型支持、计时默认建议、力量默认建议、onboarding suitability 和审核备注。
- 校验测试覆盖 ID 唯一、必填字段、训练类型与默认建议一致、恢复区域、器械/部位指导和替代动作边界。
- `sourceMeta`/`extensions` 策略已记录：`sourceMeta` 写入 fixture，`extensions` 保持为空，不把默认建议静默扩展进核心 `Exercise` 契约。

### Story E1.3: 动作库列表与筛选

**状态:** Done in Android read-only list, filters, and validation tests

作为用户，  
我想按训练模式、部位、器械和难度筛选动作，  
以便快速找到适合计划的动作。

**验收标准:**

- Given 动作库列表，When 选择计时训练筛选，Then 只显示支持计时训练的动作。
- Given 力量训练计划编辑入口，When 添加动作，Then 优先筛选支持 reps 或 weight 的动作。
- Then 筛选状态可清除。

**交付结果:**

- Android 侧新增首版只读动作库 Compose 页面，直接消费 E1.2 首批 11 个动作 fixture。
- 支持训练类型、身体部位、器械和难度筛选；力量筛选按支持 reps 或 weight 的动作返回，服务后续计划编辑选动作入口。
- 动作摘要卡展示名称、分类、难度、主要部位、器械、能力标签、训练中短提示和 fixture 默认建议摘要。
- 新增纯 Kotlin 筛选逻辑和 fixture 到 UI state 映射测试；本 story 未引入完整 repository、训练引擎、动作详情完整页或计划编辑闭环。

### Story E1.4: 动作详情

**状态:** Done in Android read-only detail surface and mapper tests

作为用户，  
我想查看动作步骤、要点和常见错误，  
以便在训练前或训练中确认动作做法。

**验收标准:**

- Given 动作详情页，When 打开动作，Then 展示步骤、发力要点、常见错误、呼吸提示和短提示。
- Then 训练中动作详情可快速关闭并回到原训练状态。

**交付结果:**

- Android 侧新增 fixture-only 动作详情 UI state 和动作 id 查找逻辑，继续直接消费 E1.2 首批 11 个动作 fixture。
- E1.3 动作库列表卡片可进入只读详情页；详情展示短提示、设置与执行步骤、发力要点、常见错误、呼吸提示、安全说明、替代动作和恢复映射。
- 缺失动作 id 返回空状态；新增 mapper 测试覆盖详情 UI state 映射、动作 id 查找和列表到详情入口所需的轻量逻辑。
- 本 story 未引入完整 repository、计划编辑闭环、训练引擎、通知调度、真实心率设备、语音能力或课程平台能力。

## Epic E2: 训练计划创建与管理

目标：让用户能创建、保存、复用计时和力量计划。

### Story E2.1: 首页与训练入口

**状态:** Done in Android official shell and home entry state

作为用户，  
我想从首页清楚看到计时训练、力量训练和跟练入口，  
以便快速开始合适的训练。

**验收标准:**

- Given 首页，When 用户进入 App，Then 计时训练是默认推荐入口。
- Then 力量训练入口同屏可见。
- Then 跟练入口标识为雏形或基础体验，不暗示完整课程平台。

**交付结果:**

- Android 侧新增轻量官方 shell 状态，底部导航当前启用 `训练` 和 `动作库`，`计划` 与 `记录` 在真实闭环前保持禁用。
- 首页以计时训练作为推荐默认入口，同时同层展示力量训练和动作库；计时/力量计划编辑未实现前明确标注后续 story 接入，不提供假启动按钮。
- 动作库入口接入 E1.3/E1.4 已完成的只读动作列表、筛选和详情。
- 跟练雏形、session records 与恢复建议仅作为后续边界说明，保持禁用；本 story 未实现训练引擎、计划编辑闭环、真实记录流、repository 业务层、通知、真实心率、语音或跟练闭环。

### Story E2.2: 计时计划编辑

**状态:** Done in Android in-memory timed plan editor and contract mapping tests

作为用户，  
我想创建包含动作时长、休息和轮数的计时计划，  
以便执行 HIIT、跳操、热身或拉伸训练。

**验收标准:**

- Given 计时计划编辑页，When 添加动作，Then 可设置动作时长、休息时长、轮数和轮间休息。
- Then 临近结束提醒默认最后 5 秒。
- Then 可保存计划并进入详情或立即开始。

**交付结果:**

- Android 侧新增 `feature:plans` 的计时计划编辑页，首页推荐计时入口已可进入真实编辑基础。
- 编辑页使用内存态状态，支持计划名称、热身/拉伸时长、动作时长、动作后休息、轮数、轮间休息、动作/休息临近结束提醒阈值、声音、震动和强化动画开关。
- 添加动作复用 E1.2 首批 fixture 中支持计时训练的动作；不引入完整动作 repository，不让 Room entity 泄漏到 feature/UI。
- 保存按钮只生成本次编辑中的 `WorkoutPlan` 草稿预览，映射到 `TimedCircuitBlock`、`TimedExerciseItem` 和 `CueSettings`；真实持久化、计划列表/详情和立即开始训练留给后续 story。
- E2.2 retro fix 已关闭动作/休息临近结束提醒阈值在短时长配置下的边界问题。
- 本 story 未实现训练执行引擎、通知调度、真实 session records、真实心率设备、语音控制或跟练完整闭环。

### Story E2.3: 力量计划编辑

**状态:** Done in Android in-memory strength plan editor and contract mapping tests

作为用户，  
我想创建包含重量、次数、组数和休息的力量计划，  
以便按计划完成力量训练。

**验收标准:**

- Given 力量计划编辑页，When 添加力量动作，Then 默认次数区间为 `8-12`。
- Then 可设置目标重量、组数、组间休息。
- Then 可展开配置热身组和逐组目标。

**交付结果:**

- Android 侧新增 `feature:plans` 的力量计划编辑页，首页力量训练入口已可进入真实编辑基础。
- 编辑页使用内存态状态，支持计划名称、力量动作选择、目标重量、默认 `8-12` 次区间、固定次数、正式组数、动作内热身组、组间休息和逐组目标展开编辑。
- 添加动作复用 E1.2 首批 fixture 中支持 reps 或 weight 的动作；不引入完整动作 repository，不让 Room entity 泄漏到 feature/UI。
- 保存按钮只生成本次编辑中的 `WorkoutPlan` 草稿预览，映射到 `StrengthExerciseBlock`、动作级 `StrengthExerciseTarget`、`StrengthSetPlan`、替代动作候选和默认 `manual_start` 组计时模式；真实持久化、计划列表/详情和力量训练执行引擎留给后续 story。
- 本 story 未实现完整训练执行引擎、strength set confirmation、actual record 执行闭环、session records、通知调度、真实心率设备、语音控制或跟练完整闭环。

### Story E2.4: 计划列表、详情、复制与删除

**状态:** Done in Android in-memory plan management list/detail/copy/delete

作为用户，  
我想管理已有计划，  
以便复用、修改或删除训练安排。

**验收标准:**

- Given 已保存计划，When 打开计划列表，Then 显示模式、名称、预计时长或动作数。
- Then 可查看详情、复制、编辑、删除。
- Then 删除前有确认。

**交付结果:**

- Android 侧新增 `feature:plans` 的计划管理列表与详情页，并在官方 shell 中启用底部“计划”入口。
- 计划管理页复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约种子化内存态计时与力量计划集合，列表显示模式、名称、预计时长/动作数和提醒/组间休息摘要。
- 详情页展示计时计划结构、力量动作与组摘要，支持复制计划和删除计划；删除前必须先进入确认对话框。
- 复制和删除仅更新本阶段内存态集合，不接入 Room/DataStore repository；编辑回填和训练启动入口保留后续接入状态。
- 本 story 未实现训练执行引擎、`WorkoutSession` / session records、通知调度、真实心率设备、语音控制或跟练完整闭环。

## Epic E3: 计时训练执行闭环

目标：用户可以启动并完整执行一套计时训练，包含动作、休息、提醒和总结。

### Story E3.1: 计时训练执行引擎

**状态:** Implemented in `core.engine` pure Kotlin state machine and unit tests

作为用户，  
我想 App 按动作和休息自动推进，  
以便训练中不用自己记阶段。

**验收标准:**

- Given 一个有效计时计划，When 开始训练，Then 引擎按动作、休息和轮次推进。
- Then 生成动作开始、动作临近结束、休息开始、休息临近结束、训练完成事件。
- Then 暂停后剩余时间冻结，继续后从原状态恢复。

**交付结果:**

- Android 侧新增纯 Kotlin `TimedWorkoutEngine`，可从 `WorkoutPlan` 或 `WorkoutPlanSnapshot` 展开有效计时 timeline。
- 支持 `start_session`、`pause_session`、`resume_session`、`skip_step`、`extend_rest` 和 `end_session` 命令；`end_session` 进入 `abandoned` 状态，不伪装为正常完成。
- 支持 `TimedCircuitBlock` 的 items、rounds、item restAfterSec 和 restBetweenRoundsSec，并可产出动作开始、动作临近结束、休息开始、休息临近结束、暂停、继续和完成事件。
- 支持全局 `PlanPreferences.cueSettings` 与 `TimedExerciseItem.cueSettings`，item 级提醒覆盖全局提醒；大于动作/休息时长的阈值会按当前阶段时长裁剪，短阶段最多覆盖整个阶段。
- 新增单元测试覆盖核心状态推进、事件触发、暂停恢复、跳过、延长休息、轮次推进、短时长阈值边界和提前结束废弃状态。
- 本 story 未接入 UI、ViewModel、Room repository、真实 session records、通知调度、声音、震动、动画、心率设备、语音、完整跟练闭环或力量训练执行引擎。

### Story E3.2: 计时训练执行页

**状态:** Implemented in Android Compose timed session route and UI state mapper

作为用户，  
我想在训练中看到当前动作、剩余时间和下一步，  
以便运动中一眼知道该做什么。

**验收标准:**

- Given 训练执行页，When 进入动作阶段，Then 当前动作和倒计时是主信息。
- Given 进入休息阶段，Then 显示休息倒计时和下一动作。
- Then 未来心率浮动胶囊保持辅助层级，不挤压当前动作、倒计时或主控制。

**交付结果:**

- Android 侧新增 `feature.workoutsession` 计时训练执行 UI state mapper 和 Compose route/screen，页面使用深色训练执行面板展示当前动作或休息、主倒计时、步骤/轮次进度、下一步、动作短提示和当时的辅助心率占位；E11.3 后生产执行页已撤下心率位，当前 MVP 不显示心率。
- 计划管理详情中仅 `timed` 计划启用“开始计时训练”，`strength` 计划继续保留到 E4 后续接入；官方 shell 新增内存态计时训练执行 destination。
- 执行页复用 E3.1 `TimedWorkoutEngine`，启动和训练中控制均通过 `WorkoutCommand.StartSession`、`PauseSession`、`ResumeSession`、`SkipStep`、`ExtendRest` 和 `EndSession` 分发，不在 UI 中手写第二套计时状态机。
- 支持 completed / abandoned 的轻量结束状态；新增纯 Kotlin UI state mapper 测试和 shell/navigation state 测试。
- 本 story 未实现真实 `WorkoutSession` 持久化、session records 写库、Room/DataStore repository 闭环、通知调度、前台服务、声音、震动、平台动画消费者、真实心率设备、语音能力、完整训练总结、跟练闭环或力量训练执行页。

### Story E3.3: 临近结束提醒

作为用户，  
我想动作和休息临近结束时得到明显但克制的提醒，  
以便准备切换。

**验收标准:**

- Given 动作倒计时进入最后 N 秒，Then 触发动作临近结束状态。
- Given 休息倒计时进入最后 N 秒，Then 触发休息临近结束状态。
- Then 声音、震动和强化动画按偏好开关工作。

**状态:** Merged to `main` from Android timed session UI, feedback dispatcher boundary, and unit tests

**交付结果:**

- 计时执行页现在消费 E3.1 `TimedWorkoutEngine` 产出的 `timed_work_ending` / `rest_ending` 事件，保留事件驱动边界，不在 UI 中复制计时推进逻辑。
- `TimedSessionStep` 保留有效 `CountdownCue`，UI state 暴露动作临近结束、休息临近结束、剩余秒数、提醒类型、提醒文案以及声音/震动/强化动画启用项。
- Compose 执行页在动作或休息进入提醒窗口时强化倒计时颜色、面板边框和短促提示文案；动作提醒与休息提醒在标签和文案上区分。E11.3 后心率占位已撤下，当前 MVP 不显示心率。
- 新增 `core.media` 反馈分发边界，根据 `WorkoutEvent` 与 `CueSettings` / `CountdownCue` 生成声音、震动和强化动画请求；Android route 仅做薄 in-app 声音和触感消费，不接通知或前台服务。
- 新增单元测试覆盖动作提醒、休息提醒、cue 开关、阈值关闭、短时长阈值裁剪和事件驱动反馈请求。
- 本 story 未实现通知调度、前台服务、真实 `WorkoutSession` 持久化、session records 写库、Room/DataStore repository 闭环、真实心率设备、语音能力、完整训练总结、跟练闭环或力量训练执行页。

### Story E3.4: 暂停、跳过、延长休息与提前结束

作为用户，  
我想在训练中调整流程，  
以便适应真实训练节奏。

**验收标准:**

- Then 支持暂停、继续、跳过当前步骤、延长休息、提前结束。
- Then 这些操作通过 `WorkoutCommand` 进入引擎。
- Then 操作结果写入 session history。

**状态:** Closed and merged to `main` from `core.engine` timed session history, Android UI state mapper, and unit tests

**交付结果:**

- `TimedWorkoutEngine` 继续只通过 `WorkoutCommand.StartSession`、`PauseSession`、`ResumeSession`、`SkipStep`、`ExtendRest` 和 `EndSession` 接收训练中控制，不在 UI 中绕过执行边界。
- 计时引擎新增纯 Kotlin session history 边界，记录 step started / completed / skipped / abandoned、控制事件、休息延长明细和提前结束进度。
- 跳过记录包含 step id、step kind、title、剩余时间和实际执行时长；延长休息只在 active rest step 生效，并记录增加秒数与该 rest step 的累计延长秒数。
- 提前结束进入 `ABANDONED` terminal state，记录 reason、当前步骤、剩余时间和当前步骤实际执行时长；terminal state 之后的 pause/resume/skip/extend/end 命令不会继续污染 history。
- 计时执行 UI state mapper 暴露跳过数、累计延长休息秒数、最后控制事件和轻量历史摘要；Compose 执行页只做轻量展示，不做完整训练总结页。
- 新增单元测试覆盖 pause/resume 冻结与恢复、skip history、extend_rest history、early end history、terminal command 不污染状态和 UI state summary。
- 本 story 未实现真实 `WorkoutSession` 持久化、Room/DataStore repository 闭环、session records 写库、通知、前台服务、真实心率设备、语音、完整总结、跟练闭环或力量训练执行页。

## Epic E4: 力量训练执行闭环

目标：用户可以逐组开始、完成、确认实际记录并进入休息。

### Story E4.1: 力量训练执行引擎

**状态:** Completed and merged to `main` in `core.engine` pure Kotlin state machine and unit tests

作为用户，  
我想力量训练按准备、进行、确认和休息推进，  
以便每组记录清楚。

**验收标准:**

- Given 力量计划，When 开始训练，Then 进入准备本组状态。
- When 点击开始本组，Then 开始记录本组耗时。
- When 点击完成本组，Then 进入确认记录状态。
- When 确认记录，Then 保存组记录并进入休息。

**交付结果:**

- Android 侧新增纯 Kotlin `StrengthWorkoutEngine`，可从 `WorkoutPlan` 或 `WorkoutPlanSnapshot` 展开有效力量训练组步骤。
- 支持 `start_session`、`pause_session`、`resume_session`、`start_strength_set`、`complete_strength_set`、`confirm_strength_set` 和 `end_session`；`end_session` 进入 `ABANDONED`，不伪装成正常完成。
- 支持准备本组、进行本组、确认记录、组间休息和 completed / abandoned terminal state；开始本组后才累计 `activeDurationSec`。
- 完成本组后生成 `StrengthSetDraft` 确认草案，不直接写正式记录；确认后生成内存态 `StrengthSetRecord`，并进入休息或下一组准备。
- 默认回填规则固定为组级目标优先，缺失时使用动作级目标；固定次数直接回填，次数区间使用 `minReps` 作为稳定默认。
- 组间休息通过 tick 推进，支持休息临近结束事件；暂停/继续会冻结 active set 计时和 rest 剩余时间，terminal state 后命令不会污染 records/history。
- 新增单元测试覆盖状态推进、事件触发、默认回填、暂停恢复、休息推进、提前开始下一组、非法命令忽略、完成/废弃边界和空计划完成。
- 本 story 未接入 UI、ViewModel、Room/DataStore repository、真实 `WorkoutSession` 持久化、session records 写库、通知调度、声音、震动、真实心率设备、语音、完整总结、跟练闭环、动作替换或跳过。

### Story E4.2: 力量训练执行页

**状态:** Completed and merged to `main` in `feature.workoutsession` Compose route, UI state mapper, official shell start path, and unit tests

作为用户，  
我想看到当前动作、本组目标和主操作按钮，  
以便训练中不用搜索关键信息。

**验收标准:**

- Prepare 状态主按钮为 `开始本组`。
- Active 状态主按钮为 `完成本组`。
- Rest 状态主信息为休息倒计时和下一组目标。

**交付结果:**

- Android 侧新增 `StrengthWorkoutSessionScreenState` mapper，覆盖 prepare / active / confirm / rest / completed / abandoned 的当前动作、本组目标、组进度、主计时、下一组提示、轻量历史摘要和抽象心率 UI 状态。
- 新增 `StrengthWorkoutSessionRoute` Compose 深色执行页，复用 E4.1 `StrengthWorkoutEngine`，不在 UI 中手写第二套力量状态机。
- 计划详情中 strength plan 启用“开始力量训练”；官方 shell 新增 `STRENGTH_SESSION` 导航状态、active strength plan 边界和训练中锁定底部导航。
- Prepare 主按钮为“开始本组”，Active 主按钮为“完成本组”，Confirm 仅提供“按计划确认”，Rest 主信息展示休息倒计时和下一组/下一动作目标，并允许休息中提前开始下一组。
- 支持暂停、继续和提前结束；completed / abandoned 仅展示轻量结束态，不生成完整训练总结。
- 新增单元测试覆盖力量执行 UI mapper、计划详情 strength start enable、官方 shell strength session 导航与锁定边界。
- 本 story 未实现 E4.3 可编辑确认层、E4.4 动作替换与跳过、真实 `WorkoutSession` 持久化、session records 写库、Room/DataStore repository 业务闭环、训练总结、通知调度、声音、震动、真实心率设备、语音、完整跟练闭环或恢复建议。

### Story E4.3: 单组完成确认层

**状态:** Completed and merged to `main` in `feature.workoutsession` editable confirmation layer, input validation, command dispatch, and unit tests

作为用户，  
我想完成一组后快速确认实际重量和次数，  
以便记录不打断训练。

**验收标准:**

- Then 实际重量默认带入计划重量。
- Then 实际次数默认带入固定次数或提供区间快捷选择。
- Then 可记录感受：轻松、刚好、很吃力、动作变形。

**交付结果:**

- Android 侧将力量执行页 Confirm 状态从只读“按计划确认”升级为可编辑确认层，继续复用 E4.1 `StrengthWorkoutEngine` 和 `WorkoutCommand.ConfirmStrengthSet`，不在 UI 中写第二套 session 语义。
- 确认层展示动作名、当前组序号、组类型、计划重量、计划次数和本组耗时；实际重量默认回填组级计划重量，缺失时使用动作级目标重量；实际次数默认回填固定次数，次数区间使用区间下限作为稳定默认并提供区间内快捷选择。
- 实际重量和实际次数支持输入修改；重量非法数字或负数、次数小于 1 时禁用确认并显示轻量错误。
- 感受使用四个清晰选项：轻松 / 刚好 / 很吃力 / 动作变形，对应 `SetEffort.EASY` / `GOOD` / `HARD` / `FORM_BREAKDOWN`。
- 点击“确认本组”后发送携带 actual weight、actual reps 和 effort 的 `WorkoutCommand.ConfirmStrengthSet`，由引擎继续推进到组间休息、下一组或 completed。
- 新增单元测试覆盖固定 reps、range reps、无重量计划、组级覆盖动作级目标、非法输入禁用以及确认命令 payload。
- 本 story 未实现 E4.4 动作替换与跳过、真实 `WorkoutSession` 持久化、session records 写库、Room/DataStore repository 业务闭环、训练总结、通知调度、声音、震动、真实心率设备、语音、完整跟练闭环或恢复建议。

### Story E4.4: 动作替换与跳过

**状态:** Completed and merged to `main` in `core.engine` replace/skip handling, strength execution UI controls, and unit tests

作为用户，  
我想训练中替换或跳过动作，  
以便设备不可用或身体状态变化时继续训练。

**验收标准:**

- Then 替换动作保留原动作引用。
- Then 本次训练记录能区分替换来源。
- Then 跳过动作不破坏后续组顺序。

**交付结果:**

- `StrengthWorkoutEngine` 支持 `WorkoutCommand.ReplaceExercise` 的力量训练路径，在不改写原 `WorkoutPlan` 的前提下更新当前 block 的 effective exercise，并让后续 `StrengthSetRecord.substitutedFromExerciseId` 保留原动作引用。
- `StrengthWorkoutEngine` 支持 `WorkoutCommand.SkipStep` 的力量训练路径，跳过当前动作剩余未完成组后进入下一 strength block；若跳过最后一个动作则进入 completed。
- strength step history 和 control history 记录 replace / skip 控制语义，便于 E5 总结消费；terminal state 后 replace / skip 继续被忽略。
- 力量训练执行页新增低层级动作调整面板，替换候选来自计划/动作替代映射和首批 fixture 中适合力量训练的动作；跳过动作需要明确确认。
- 新增/更新单元测试覆盖替换来源记录、跳过后顺序保持、最后动作跳过完成、active / confirm / rest 跳过语义、terminal state 后忽略命令、UI 候选过滤和命令分发边界。
- 本 story 未实现真实 `WorkoutSession` 持久化、session records 写库、Room/DataStore repository 业务闭环、训练总结、通知调度、声音、震动、真实心率设备、语音、完整跟练闭环或恢复建议。

## Epic E5: 训练总结、历史与恢复建议

目标：训练后生成有价值记录，并给出基础恢复建议。

### Story E5.1: 计时训练总结

**状态:** Implemented in `feature.workoutsession` timed summary UI state, terminal summary panel, and unit tests

作为用户，  
我想看到计时训练完成情况，  
以便知道本次完成了什么。

**验收标准:**

- Then 展示总时长、完成阶段、完成轮数、跳过内容、休息延长情况。
- Then 提供恢复建议入口。

**交付结果:**

- 新增 `TimedWorkoutSummaryUiState` 与 mapper，直接消费 `TimedWorkoutEngineState` 的 `activeElapsedSec`、`stepHistory`、`controlHistory`、`restExtensionHistory` 和 `earlyEnd`，不新增第二套训练结果来源。
- completed / abandoned 终态在计时训练执行页展示轻量总结面板，覆盖总时长、完成阶段、步骤进度、轮次进度、跳过内容、休息延长、提前结束进度和训练部位摘要。
- 总时长明确标注为 engine active elapsed，不伪装成真实 wall-clock `startedAt` / `endedAt` 会话耗时。
- 恢复建议入口以禁用占位呈现，文案明确 E5.4 后续接入，不暗示已经生成完整恢复建议。
- 新增 summary mapper 测试覆盖 completed 时长/阶段/轮次、跳过摘要、休息延长、abandoned 原因与进度、无跳过/无延长空状态和恢复建议占位。
- 本 story 未实现 E5.2 力量训练总结、E5.3 历史趋势、E5.4 完整恢复建议、真实 `WorkoutSession` 持久化、Room/DataStore repository 业务闭环、通知、真实心率设备、语音或跟练闭环。

### Story E5.2: 力量训练总结

**状态:** Implemented in `feature.workoutsession` strength summary UI state, terminal summary panel, and unit tests

作为用户，  
我想看到力量训练计划与实际差异，  
以便回顾表现。

**验收标准:**

- Then 展示动作、组数、重量、次数、组耗时、实际休息。
- Then 展示计划与实际差异。
- Then 不自动给出加重量建议。

**交付结果:**

- 新增 `StrengthWorkoutSummaryUiState` 与 mapper，直接消费 `StrengthWorkoutEngineState` 的 `sessionElapsedSec`、`strengthSetRecords`、`stepHistory`、`controlHistory` 和 `earlyEnd`，不新增第二套训练结果来源。
- completed / abandoned 终态在力量训练执行页展示轻量总结面板，覆盖动作数、已确认组/计划组、每动作组记录、planned / actual 重量与次数、组耗时、实际休息、替换动作、跳过动作/组、提前结束原因与进度。
- 文案明确当前为引擎内存态记录，不伪装成真实 wall-clock `startedAt` / `endedAt` 或持久化 session records。
- 恢复建议入口以禁用占位呈现，文案明确 E5.4 后续接入，不暗示已经生成完整恢复建议。
- 新增 summary mapper 测试覆盖 completed 动作数/组数、planned vs actual 重量次数、组耗时、实际休息、replacement 标记、skipped set 摘要、abandoned 原因与进度，以及不生成自动加重量建议。
- 本 story 未实现 E5.3 历史趋势、E5.4 完整恢复建议、真实 `WorkoutSession` 持久化、Room/DataStore repository 业务闭环、通知、真实心率设备、语音或跟练闭环。

### Story E5.3: 训练历史与基础趋势

**状态:** Implemented in `feature.history` in-memory history screen, basic trends, official shell records entry, and unit tests

作为用户，  
我想查看历史训练记录和基础趋势，  
以便复盘和调整计划。

**验收标准:**

- Then 支持按训练日期查看历史。
- Then 支持单动作重量/次数历史。
- Then 支持训练容量历史。
- Then 不使用医疗或过度结论文案。

**交付结果:**

- Android 侧新增 `HistoryScreenState`、历史列表 item、单次详情、基础趋势 UI state 和内存态 `WorkoutSession` seed，覆盖计时 / 力量、completed / abandoned mixed session 展示。
- 官方 shell 的“记录”入口已启用并进入 `feature.history` Compose 页面，页面按训练日期分组展示记录列表，并支持选择单次记录查看摘要详情。
- 基础趋势展示单动作重量 / 次数历史，以及按力量训练已确认组汇总的总组数、总次数和实际重量 * 次数训练容量历史；计时训练不被硬纳入重量容量。
- 文案明确当前为内存态 / 示例历史，不读取 Room session records，不伪装真实持久化历史。
- 新增 history UI state 测试和 shell/navigation state 测试，覆盖日期排序、timed / strength mixed list、单次详情选择、单动作重量 / 次数历史、训练容量历史、空状态和不生成医疗或过度结论文案。
- 本 story 未实现真实 `WorkoutSession` 持久化、Room/DataStore repository 业务闭环、数据库读取历史列表、E5.4 完整恢复建议、自动训练建议、自动加重量建议、医疗/心率/热量判断、云同步、账号、社交或排行。

### Story E5.4: 基础恢复建议

**状态:** Implemented in `core.domain.recovery`, `feature.recovery`, workout summary recovery entries, official shell recovery destination, and unit tests

作为用户，  
我想根据本次训练部位获得放松建议，  
以便训练后知道该恢复哪里。

**验收标准:**

- Then 根据训练动作的肌群映射恢复区域。
- Then 展示推荐放松区域和基础说明。
- Then 不做康复治疗或医疗诊断表述。

**交付结果:**

- 新增 5 个首批恢复区域 fixture：`lower-body-release`、`posterior-chain-release`、`chest-shoulder-release`、`upper-back-release`、`core-breathing-reset`。
- 新增 `BasicRecoveryRecommendationGenerator`，根据本次已完成计时动作或已确认力量组的动作 recovery 映射汇总训练肌群和恢复区域，并保持去重与展示顺序稳定。
- 新增 `feature.recovery` UI state 与浅色 Compose 页面，展示主要训练部位、来源动作、推荐放松区域、基础说明和“不做康复治疗或医疗诊断”的边界文案。
- E5.1 / E5.2 summary 的“查看恢复建议”已从禁用占位变为可用入口；未识别动作或无 completed / confirmed 内容时展示诚实空状态。
- 官方 shell 新增内存态 `RECOVERY` destination；当前仍不读取 Room session records，不写入真实 `recovery_recommendations` 表，不实现 repository 业务闭环。
- 新增 domain、feature recovery、workout summary 和 shell/navigation 测试，覆盖区域去重、肌群汇总、无映射空状态、非医疗文案、计时/力量两类 session。
- 本 story 未实现自动训练建议、康复治疗建议、医疗诊断、疼痛判断、心率/热量判断、通知调度、真实心率设备、语音控制、完整跟练闭环或大规模恢复内容库。

## Epic E6: 跟练雏形与健康数据边界

历史目标：交付可用但克制的跟练雏形，并保留健康数据模型 / provider 边界；E11.3 当时的 MVP 不显示、不录入、不统计心率。D-080 已 supersede 该心率产品范围，但不改写 E6 的历史交付结果；当前 E17 implementation 仍受 E17-3 条件式 merge gate 与 E17-4 readiness 约束。

### Story E6.1: 跟练雏形计划入口

**状态:** Implemented in `feature.followalong` basic entry screen, follow-along preset seed, home/shell navigation, and unit tests

作为用户，  
我想选择一个基础跟练流程，  
以便体验动作演示和流程提示。

**验收标准:**

- Then 至少有一个基础跟练计划或可跟练计时计划。
- Then 入口不暗示完整课程平台。

**交付结果:**

- 首页将“基础跟练”作为同层入口启用，文案明确是基础跟练/雏形体验，复用计时流程与动作短提示。
- 官方 shell 新增 `FOLLOW_ALONG_ENTRY` destination，跟练入口页仍归属训练底部导航，不新增独立底部 tab。
- 新增 `feature.followalong` boundary、UI state 和 Compose route，展示一个内存态基础跟练 preset。
- preset 使用 `WorkoutMode.FOLLOW_ALONG`、`FollowAlongPlanMeta(preset=true)`、`TimedCircuitBlock` 和 `TimedExerciseItem`，动作只来自首批 fixture 中同时支持 `supportsFollowAlong` 与计时流程的动作。
- 跟练选择页展示动作数、预计时长、动作短提示、媒体位说明、当前能力边界和 E6.2 跟练执行页后续接入的禁用状态。
- 新增单元测试覆盖首页入口启用、shell destination、preset seed、`supportsFollowAlong` 过滤、空状态和边界文案。
- 本 story 未实现完整跟练执行页、视频播放、课程平台、教练视频库、AI 纠错、音乐编排、语音教练、真实心率设备、通知调度、真实 `WorkoutSession` 持久化、Room/DataStore repository 闭环或 session records 写库。

### Story E6.2: 跟练执行页

**状态:** Implemented in `feature.workoutsession` basic follow-along session route, UI state mapper, shell session destination, and unit tests

作为用户，  
我想在跟练页看到当前动作、演示位、倒计时、短提示和下一动作，  
以便跟着流程训练。

**验收标准:**

- Then 跟练复用计时训练引擎。
- Then 展示媒体位，即使首版媒体为空也不显得像坏掉。
- Then 支持暂停、跳过、动作详情和结束。

**交付结果:**

- E6.1 内存态 `WorkoutMode.FOLLOW_ALONG` preset 的开始按钮已启用，进入 `FOLLOW_ALONG_SESSION` destination。
- 官方 shell 新增跟练执行 active plan 状态；跟练执行中底部导航仍选中“训练”并锁定，结束后返回基础跟练入口。
- 新增 `FollowAlongWorkoutSessionRoute` 与 `FollowAlongWorkoutSessionUiState`，执行页复用 `TimedWorkoutEngine.create(plan)` 和 `WorkoutCommand` 推进开始、暂停、继续、跳过和提前结束。
- 页面展示当前动作、演示/媒体占位、倒计时、阶段进度、动作短提示、下一动作预告、当时的低层级心率占位、控制按钮和基于 fixture 的动作详情；E11.3 后生产跟练执行页已撤下心率位。
- 媒体为空时展示明确占位文案，不加载远程资源，也不伪装为加载失败。
- completed / abandoned 终态展示“基础跟练完成 / 提前结束”的轻量总结，并明确当前仍是引擎内存态，不写入真实 session records。
- 新增单元测试覆盖 preset 可开始、shell 启动与导航锁定、follow-along plan 复用计时引擎、UI state 映射、控制到 `WorkoutCommand` 的映射、媒体空状态和保留能力文案边界。
- 本 story 当时只支持 E6.1 preset 启动；未支持任意计时计划切换为跟练视图。E10.1 后，O-002 已收敛为跟练后续通过统一动作选择页编排，不再依赖计时计划切换为跟练视图。
- 本 story 未实现视频播放、远程资源、真实媒体播放器、自动语音、动作分析、真实心率设备、心率告警、热量判断、通知调度、Room/DataStore repository 闭环、真实 `WorkoutSession` 持久化或 session records 写库。

### Story E6.3: 心率抽象状态展示

**状态:** Implemented in shared workout session heart-rate display mapper, abstract health providers, and unit tests

作为用户，  
我想训练页可以显示心率状态，  
以便未来接入设备时不用改训练流程。

**验收标准:**

- Then 支持 disabled、not_connected、connecting、available、stale、error 状态。
- Then 没有设备时训练闭环完整可用。
- Then 不显示医疗级告警结论。

**交付结果:**

- E6.3 曾新增共享心率展示 mapper；E11.3 后生产执行页已撤下心率展示 UI，仅保留未来 `HeartRateState` / provider 模型边界。
- 支持 disabled、not_connected、connecting、available、stale、error 六种旧 `HeartRateAvailability` baseline，available 可显示 bpm；E11.1 已把该 baseline 收口为 source-aware `HeartRateState`。
- measuredAt、sourceId 和 message 只进入低层级辅助文案；E11.1 起废弃 `warningLevel` 口径，不通过心率状态驱动颜色、告警、训练规则、训练状态或主控按钮。
- `core.health` 新增 `HeartRateProvider`、`DisabledHeartRateProvider` 和 `MockHeartRateProvider` 边界，仅输出抽象 `HeartRateState`，不接真实设备或平台 SDK。
- 新增单元测试覆盖六种状态、三类执行页一致映射、available 的 bpm / measuredAt / sourceId / message、心率告警负向、主控不受心率状态影响、越界文案负向和 Manifest 权限负向检查。
- 本 story 未接入 Health Connect、Wear OS、BLE、厂商 SDK、真实传感器、健康/身体传感器/蓝牙权限、心率告警、危险状态判断、医疗结论、热量估算、训练强度判断或恢复建议联动。

## Epic E7: 通知、声音、震动与偏好

目标：建立首版提醒能力和可配置偏好。

### Story E7.1: 训练提醒通知

**状态:** Implemented in `core.notifications`, plan detail reminder UI, ordinary notification permission/channel/scheduler boundary, and unit tests

作为用户，  
我想收到训练计划提醒，  
以便按计划开始训练。

**验收标准:**

- Then 可为计划设置提醒时间。
- Then 通知权限关闭时 App 有清楚提示。
- Then 首版不承诺闹铃级强提醒。

**交付结果:**

- Android 侧新增 `core.notifications` 训练计划提醒边界，覆盖 `PlanReminderScheduleRequest`、Android 13+ `POST_NOTIFICATIONS` 权限状态、普通通知 channel / content、调度策略和 Android 普通 alarm 调度适配。
- Manifest 仅新增 `android.permission.POST_NOTIFICATIONS` 和非导出的 `PlanReminderNotificationReceiver`；未申请 `SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM`、`FOREGROUND_SERVICE`、健康、身体传感器、蓝牙或定位权限。
- 计划详情页在现有内存态计划流中新增训练提醒区，支持用未来时间快捷设置 `PlanReminder`、关闭提醒，并显示提醒状态。
- Android 13+ 通知权限关闭时，计划详情展示清楚、克制文案：提醒暂不会弹出，但训练执行闭环仍可正常使用，并提供通知权限请求入口。
- 通知 channel、通知内容和 UI 文案均明确首版是普通通知，允许系统延迟，不承诺闹钟级强提醒、全屏提示或锁屏强打断。
- 新增单元测试覆盖权限状态映射、调度 disabled / 缺少时间 / 过去时间 / 权限关闭边界、普通通知内容、manifest 负向权限和不使用 exact alarm / foreground service。
- 本 story 未实现 E7.2 活跃训练 ongoing notification、前台服务、后台训练可靠计时、闹钟级强提醒、真实 `WorkoutSession` 持久化、session records 写库、语音、真实心率设备、Health Connect、Wear OS、BLE、厂商 SDK 或 E7.3 训练偏好设置总页。

### Story E7.2: 活跃训练通知边界

**状态:** Implemented in `core.notifications` active workout notification boundary, session route dispatch, and unit tests

作为用户，  
我想训练进行中离开 App 时仍知道训练状态，  
以便不中断训练。

**验收标准:**

- Then 明确是否首版启用前台服务。
- If 启用，Then ongoing notification 显示当前训练摘要。
- Then 不把通知逻辑写入训练引擎。

**交付结果:**

- 本 Story 的历史交付与普通训练当前基线是不启用 foreground service：训练状态摘要不冒用 data sync / media / health 类型。D-081 后，只有“活跃训练 + 已有合法当前心率连接”成为 `connectedDevice` FGS 窄例外；普通 active / paused 训练仍沿用本 ordinary notification。
- `core.notifications` 新增 active workout notification contract、普通 ongoing channel/content、permission-gated policy 和 Android `NotificationManager.notify/cancel` 控制器。
- 历史实现由计时、力量和基础跟练 Route 从各自 UI state / engine status 映射摘要并直接 update / clear。D-081 production 合同 supersede Route 的最终通知所有权：Route 只提交状态，唯一 Application-scoped coordinator 持有训练事实；Route dispose 不得取消仍活跃训练的通知，terminal 由协调者最终清理一次。
- 通知文案明确为普通状态提示，不承诺后台精确计时、闹铃级提醒、锁屏强打断或医疗/危险状态提醒。
- 历史测试覆盖 active / paused 展示、permission denied 不阻塞训练、terminal 清理、三类执行页摘要映射、manifest 负向权限和 ordinary notification 边界；D-081 的 `connectedDevice` FGS、ID `7200` 单一 writer、权限拒绝 content、升降级与 terminal handoff 必须由 E17-4 后续 implementation evidence 独立覆盖。
- 本 story 未实现后台精确计时系统、foreground service、notification action 控制训练、真实 `WorkoutSession` 持久化、语音、健康/传感器/蓝牙/定位权限或 E7.3 训练偏好设置总页。

### Story E7.3: 训练偏好设置

**状态:** Implemented in `core.datastore`, `feature.settings`, official shell settings entry, plan editor default mapping, and unit tests

作为用户，  
我想设置提醒秒数、声音、震动和动画开关，  
以便训练反馈符合偏好。

**验收标准:**

- Then 可设置默认临近结束秒数。
- Then 可开关动作提醒、休息提醒、声音、震动、强化动画。
- Then 力量训练本组计时默认模式可设置。

**交付结果:**

- `core.datastore` 扩展训练偏好读写边界，可持久化默认临近结束秒数、动作提醒开关、休息提醒开关、声音、震动、强化动画和力量训练本组计时默认模式，并对阈值和模式做契约夹紧。
- 首页新增“训练偏好”入口，官方 shell 新增非底部 tab 的 settings destination；设置页展示训练内倒计时反馈设置，并用克制文案区分 E7.1 计划提醒通知、E7.2 活跃训练普通 ongoing notification 和本 story 的训练内倒计时反馈。
- 新建计时计划从偏好生成默认 `CueSettings`；新建力量计划从偏好生成默认 `StrengthSetTimerMode`。已生成计划中的显式提醒设置和 block 级组计时模式不会被全局偏好静默覆盖。
- 新增单元测试覆盖 DataStore 保存、设置 UI state、app 层映射、计时/力量计划默认消费和 shell 入口。
- 本 story 未实现新的通知调度、foreground service、exact alarm、notification action 控制训练、语音读秒、自动语音教练、后台可靠计时、真实 `WorkoutSession` 持久化、session records 写库、真实心率设备、Health Connect、Wear OS、BLE、厂商 SDK 或健康/传感器/蓝牙/定位权限。

## Epic E8: 设计系统、UI Shell 与开源定制边界

目标：让官方默认 UI 优雅、克制、专业，同时让开源社区可以定制主题、首页布局和按钮位置，而不破坏核心训练引擎。

### Story E8.1: Skin contract and registry

**状态:** Implemented in `ui.theme`, `core.datastore`, settings UI, app theme mapping, and unit tests

作为设计维护者，  
我想建立内置 UI 皮肤的 contract、registry 和本地偏好边界，
以便用户能在不改变训练语义的前提下切换官方内置视觉风格。

**验收标准:**

- Then App 有 Official Flow、Tile Flow、Big Type 三套内置皮肤 metadata。
- Then Official Flow 是默认皮肤，并继续对应 `DESIGN.md` 官方默认方向。
- Then 设置页可切换皮肤并持久化当前 skin id。
- Then 非法或未知 skin id 回退到 Official Flow。
- Then shell/theme 能消费当前皮肤状态，并至少体现轻量 token 差异。
- Then 皮肤不能改变训练计划、训练记录、训练命令、训练事件或训练执行引擎语义。

**交付结果:**

- `ui.theme` 新增 `BuiltInUiSkin`、`TrainFlowSkin`、`TrainFlowSkinTokens` 和 `SkinRegistry`，注册 Official Flow、Tile Flow、Big Type 三套内置皮肤，包含 skin id、显示名、描述、目标用户、能力边界、默认标记和 token。
- `TrainFlowTheme` 现在可消费当前 `TrainFlowSkin` 并从 skin tokens 生成 Material color scheme；Tile Flow / Big Type 仅有轻量 token 差异，不承诺完整页面重排。
- `core.datastore` 新增 `uiSkinId` 偏好，保存三套内置 skin id，并对未知或非法 id 回退到 `official_flow`。
- 设置页新增“UI 皮肤”选择入口，展示三套内置皮肤 metadata，并通过 `MainActivity` 持久化选择；App theme 根据偏好解析当前 skin。
- 新增单元测试覆盖 registry 元数据、默认皮肤、非法 id 回退、DataStore 保存读取、settings 选项映射、app mapper 和 theme token 映射。
- 本 story 未实现 Tile Flow 完整磁贴页面、Big Type 完整大字执行页、运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也未改变 `WorkoutCommand`、`WorkoutEvent`、`WorkoutPlan`、`WorkoutSession`、通知、心率、恢复建议或 `core.engine` 边界。

### Story E8.2: Tile Flow 完整视觉重做

**状态:** Implemented in `ui.theme`, `ui.designsystem`, home/plans/settings, workout execution routes, and unit tests

作为开发者，  
我想把 Tile Flow 从内置注册占位扩展为完整磁贴式视觉体验，
以便偏好清爽信息块的用户获得更明确的页面形态。

**验收标准:**

- Then Tile Flow 有完整 token、组件和关键页面布局映射。
- Then 训练执行页仍保留当前动作、时间/组目标、主按钮和必要控制。
- Then 不改变训练命令、事件、计划、记录或核心引擎语义。

**交付结果:**

- `TrainFlowTheme` 通过 composition local 向真实 Compose 页面提供当前 skin；Tile Flow token 扩展页面横向留白、分组间距、普通磁贴和主磁贴圆角，`ui.designsystem` 新增可复用磁贴与指标条组件。
- Tile Flow 训练首页使用最大计时训练主磁贴、并列力量/跟练次级磁贴，以及动作库、最近计划、训练偏好、提醒状态和记录工作区；所有启用入口仍指向现有真实页面。
- Tile Flow 计划列表与详情使用磁贴和指标条表达动作数、轮次、时长、休息和提醒状态；设置页使用统一磁贴分组展示训练反馈、力量默认、UI 皮肤和通知边界。
- 计时与力量训练执行页只做轻度 Tile Flow 适配，消费当前皮肤的深色容器、间距、圆角和动作色；当前动作、主倒计时/组目标、主按钮、必要控制和心率辅助层级不变。
- Official Flow 保持原有页面组合、默认尺寸与颜色；Big Type 继续保持 E8.1 占位状态。动作库、计划编辑、跟练入口/执行、记录、恢复和总结细节继续沿用 Official Flow 页面组合。
- 新增单元测试覆盖 Tile Flow 布局 token、Big Type 占位、首页工作区入口和计划指标映射。
- 本 story 未实现运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也未改变 `WorkoutCommand`、`WorkoutEvent`、`WorkoutPlan`、`WorkoutSession`、通知、权限、心率、恢复建议或 `core.engine` 边界。

### Story E8.3: Big Type 完整视觉重做

**状态:** Implemented in `ui.theme`, home/settings, timed/strength workout execution routes, and unit tests

作为用户，
我想使用大字训练皮肤，
以便运动中更容易看清当前动作、时间、组目标和主按钮。

**验收标准:**

- Then Big Type 有完整 token、组件和关键执行页布局映射。
- Then 训练执行页主信息更大更易扫读，心率仍保持辅助层级。
- Then 不隐藏权限说明、通知边界、心率非医疗化或恢复建议非医疗化文案。

**交付结果:**

- `TrainFlowSkinTokens` 扩展关键字体、计时器、按钮高度、执行面板内边距和固定控制区预留 token；Big Type metadata 不再是占位，明确面向远距离可读、少信息、大按钮和高对比训练体验。
- Big Type 训练首页使用最大计时训练入口、同层力量/跟练大入口和精简工具入口；设置页更新 Big Type 实际覆盖范围说明。
- Big Type 计时执行页放大当前动作与倒计时，减少次级说明，并继续使用固定底部控制区保证暂停/继续、跳过、`+15秒` 和结束训练即时可见。
- Big Type 力量执行页放大当前动作、本组目标、组耗时/休息倒计时和主按钮，并把开始/完成/确认本组、暂停/继续和结束训练统一放入固定底部控制区；确认层使用单列实际重量/次数输入和高对比深色输入 token。
- 计划编辑、动作详情、计划管理、跟练入口/执行、记录、恢复和总结细节继续沿用 Official/Tile 现有页面组合；Official Flow 与 Tile Flow 保持原有页面组合、尺寸与行为。
- 新增单元测试覆盖 Big Type 完整布局 token、metadata、Official/Tile 布局 token 回归和 settings 选项映射；720x1280 与常规尺寸模拟器检查确认两类执行页主控制即时可见。
- 本 story 未实现运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也未改变 `WorkoutCommand`、`WorkoutEvent`、`WorkoutPlan`、`WorkoutSession`、通知、权限、心率、恢复建议或 `core.engine` 边界。

### Story E8.4: 社区主题和布局审查清单

**状态:** Implemented in UI skin review checklist documentation and registry/readiness tests

作为维护者，  
我想有一份 UI 贡献审查清单，  
以便保证社区 UI 不牺牲可读性、权限说明和训练稳定性。

**验收标准:**

- Then 审查清单覆盖训练可读性、对比度、主按钮、心率表述、未实现能力和引擎边界。
- Then 主题贡献必须说明目标用户、token 映射和训练执行页表现。

**交付结果:**

- 新增 `docs/testing/ui-skin-readiness-checklist.md`，作为三套内置 UI 皮肤、E9 用户测试前 UI readiness 和社区主题/layout 贡献的可执行审查清单。
- `docs/ui-extension-guide.md` 已补充 E8.4 review gate，覆盖 Official Flow、Tile Flow、Big Type 的审查要点、训练执行页固定主控制、720x1280 小屏、权限说明、心率非医疗化、恢复建议非医疗化和普通通知边界。
- 社区 UI 定制边界已明确：可以改 theme、shell、布局和组件外观；不得改 `WorkoutCommand`、`WorkoutEvent`、`WorkoutPlan`、`WorkoutSession`、训练执行引擎、权限/健康边界或力量确认语义。
- E9 用户测试回看事项已纳入清单：热身、动作、动作后休息、轮间休息、放松/拉伸是否都应支持最后 N 秒提醒；训练提示音不得降低、暂停或打断其他 App 音乐/视频，也不主动执行 ducking。
- 继续禁止运行时插件市场、远程皮肤下载、第三方皮肤安装和动态代码加载；本 story 未新增第四套皮肤，未重做三套内置皮肤，未改变核心训练、通知、权限、心率、恢复建议或 `core.engine` 边界。

## Epic E9: MVP 验收与发布准备

目标：完成首版质量门，准备小范围试用。

### Story E9.1: 训练状态恢复与回归测试

**状态:** Implemented as training recovery checklist and regression test baseline

作为开发者，  
我想验证训练中暂停、后台、返回和异常退出恢复，  
以便减少真实训练中断风险。

**验收标准:**

- Then 关键训练状态有单元测试。
- Then 关键 ViewModel 有测试。
- Then 后台或重建后的恢复策略已验证。
- Then 用户测试后回看计时训练临近结束提醒是否覆盖所有可设时长阶段，包括热身、动作、动作后休息、轮间休息和最后放松/拉伸；若当前只覆盖动作与休息，应记录为回归或后续修复项。
- Then 用户测试时验证训练提示音不会请求导致其他 App 音乐或视频被降低、暂停或打断的 audio focus，也不会主动执行 ducking；不同 Android 版本或设备上的异常应记录为后续音频适配问题。

**交付结果:**

- 新增 `docs/testing/training-state-recovery-checklist.md`，明确暂停后返回、后台再前台、屏幕旋转或 Activity 重建、进程被杀后的当前边界、completed / abandoned 终态防污染、三套 skin 小屏主控制、最后 N 秒提醒覆盖回看、音频共存、普通通知、心率和恢复建议非医疗化边界。
- 计时训练新增 E9.1 回归测试，覆盖暂停后后台 tick 不推进、继续后原步骤恢复、休息延长只影响当前休息、提前结束进入 abandoned、terminal state 后 tick 和 late commands 不污染 history。
- 力量训练新增 E9.1 回归测试，覆盖确认草案暂停/继续、休息暂停/继续、实际记录稳定、提前结束进入 abandoned、terminal state 后 late commands 不污染 `StrengthSetRecord`。
- 执行页新增回归测试，覆盖计时页暂停/继续、跳过、`+15秒`、结束训练控制可达，力量页开始/完成/确认、暂停/继续、结束训练控制可达，三套 skin 固定控制 token、mode pill 对比度和训练 UI state 语义不随 skin 切换改变。
- 新增音频边界测试，确保倒计时短提示不请求 audio focus、不使用 ducking，不主动降低、暂停或打断其他 App 音乐/视频。
- 明确当前仍不支持进程被系统杀死后的训练步骤恢复，也不把普通 ongoing notification 写成后台可靠计时。
- 本 story 未实现真实 `WorkoutSession` 持久化、Room repository 业务闭环、foreground service、exact alarm、notification action 控制训练、语音、真实心率设备、Health Connect、Wear OS、BLE 或厂商 SDK。

### Story E9.2: 权限与隐私文案

**状态:** Implemented in permission/privacy copy contract, settings/plans/workout/recovery copy, readiness checklist, and unit tests

作为用户，  
我想理解通知、震动和健康数据边界，
以便放心使用。

**验收标准:**

- Then 通知权限用途清楚。
- Then 心率和热量不被描述为医疗结论。
- Then 未接入设备时不请求健康数据权限。

**交付结果:**

- `core.model.PermissionPrivacyCopy` 统一当前用户测试前的权限与隐私边界文案，覆盖通知权限、活跃训练通知、心率、恢复建议、音频提示、语音和数据。
- 设置页新增“权限与隐私”说明区，保持短说明，不引入长篇法律文本。
- 计划提醒文案明确通知用于计划提醒和训练中状态提示，关闭通知后训练仍可正常使用，只是不弹通知；普通通知可能被系统延迟，不承诺闹钟级强提醒。
- 活跃训练通知文案明确只是训练状态摘要，不是 foreground service，不保证后台可靠计时或进程死亡恢复。
- 计时训练、力量训练和基础跟练执行页的心率展示补充抽象占位边界：当前未接入真实设备、手环、手表或健康数据，不做医疗告警、危险判断或训练强度判断。
- 恢复建议页和生成器文案明确当前基于训练动作 / 部位做基础放松映射，不是医疗诊断、康复治疗或疼痛处理建议。
- 音频提示文案明确只是短促训练提示音，目标是不降低、暂停或打断其他 App 音乐 / 视频，但不同设备和 Android 版本表现仍需用户测试回看。
- 文案明确当前只保留训练命令 / 事件边界，未实现语音控制、语音读秒或自动语音教练；当前多数计划、历史和恢复仍是内存态、fixture 或基础展示边界，不代表云同步、账号体系或真实长期记录已完成。
- 新增 `docs/testing/permission-privacy-readiness-checklist.md`，用于 E9.2 Review Gate 和用户测试前 smoke。
- 新增 / 更新单元测试覆盖文案 contract、设置页权限隐私区、计划提醒普通通知边界、active workout notification 非 foreground service / 非后台可靠计时边界、心率非设备/非医疗边界、恢复非医疗边界和音频提示设备差异边界。
- Manifest 仍仅包含 `POST_NOTIFICATIONS`；未新增健康、身体传感器、蓝牙、定位、foreground service 或 exact alarm 权限。
- 本 story 未实现 Health Connect、Wear OS、BLE、厂商 SDK、真实心率设备、语音控制、语音读秒、自动语音教练、foreground service、后台可靠计时、notification action 控制训练、真实 `WorkoutSession` 持久化、Room repository 业务闭环、云同步或账号体系。

### Story E9.3: MVP 验收清单

**状态:** Implemented as MVP acceptance checklist, user-test issue template, and lightweight acceptance evidence tests

作为产品负责人，  
我想逐条验收 MVP 功能，  
以便判断是否进入试用。

**验收标准:**

- Then PRD 10.1 到 10.4 的验收标准逐项有结果。
- Then 首版非目标没有被静默实现或暗示。
- Then 已知问题分级记录。

**交付结果:**

- 新增 `docs/testing/mvp-acceptance-checklist.md`，逐项记录计时训练、基础跟练、力量训练、数字输入、记录/数据分析、心率、通知/声音/隐私、UI skin、状态恢复和 MVP 非目标的 Pass / Partial / Deferred / Out of Scope / Risk / Bug 结论。
- 新增 `docs/testing/user-test-issue-template.md`，用于用户测试记录 P0/P1/P2/P3 和 Product Decision 问题。
- 新增轻量验收证据测试，在 E9.3 当时记录计时/力量计划编辑页的 `立即开始（E3 接入）` / `开始力量训练（E4 接入）` 禁用状态，并记录计划编辑整数输入无法表达临时空值的当前 Bug；E9.4 已将这些测试更新为修复证据。
- 记录用户反馈：计时训练后续可能应更接近纯间歇计时器；基础跟练后续应承担动作选择、动作编排和推荐；动作选择应进入独立页面；记录页后续需要总统计、图表和计划调整证据；平均心率趋势已随 E11.3 撤销。
- 明确 E10/E11/E12 后续方向：E10 训练模式边界重构与统一动作选择页，E11 心率设备/健康数据策略，E12 数据分析趋势。
- 本 story 未实现 E10 训练模式重构、统一动作选择页、完整跟练编排、真实 `WorkoutSession` 持久化、Room repository 业务闭环、foreground service、后台可靠计时、真实心率设备、Health Connect / Wear OS / BLE、语音或完整数据分析图表。

### Story E9.4: User Test Fix Pack 1

**状态:** Implemented in plan editor input state, editor start actions, documentation, and regression tests

作为用户测试准备者，
我想修复计划编辑页数字输入清空和编辑页开始按钮问题，
以便用户测试 APK 不带 P1/P2 已知可修缺陷。

**验收标准:**

- Then 计时计划编辑页的热身时间、动作时间、休息时间、轮数、轮间休息和放松时间允许临时清空。
- Then 力量计划编辑页的重量、次数、组数和休息秒数允许临时清空。
- Then 空值或非法值时保存 / 开始按钮禁用，并显示明确原因。
- Then 计时编辑页有效草稿可直接进入计时训练执行页，继续复用 `TimedWorkoutEngine` / `WorkoutCommand`。
- Then 力量编辑页有效草稿可直接进入力量训练执行页，继续复用 `StrengthWorkoutEngine` / `WorkoutCommand`。
- Then 历史记录清理的全部清除、按计划清除、按日期清除只登记为后续能力，不实现假删除。

**交付结果:**

- 计时计划编辑 state 增加 raw text 草稿输入，覆盖热身、拉伸、轮数、轮间休息、动作秒数、动作后休息和提醒阈值；空字符串可停留在输入框，保存 / 开始时统一校验。
- 力量计划编辑 state 增加 raw text 草稿输入，覆盖计划重量、次数区间、固定次数、正式组数、热身组数、组间休息、逐组重量和逐组次数；带重量动作清空重量时会禁用保存 / 开始并提示。
- 计时编辑页 `立即开始` 现在将当前有效草稿转换为 `WorkoutPlan` 并交给官方 shell 的 `startTimedSession`，由计时执行 route 继续通过 `TimedWorkoutEngine` 和 `WorkoutCommand` 推进。
- 力量编辑页 `开始力量训练` 现在将当前有效草稿转换为 `WorkoutPlan` 并交给官方 shell 的 `startStrengthSession`，由力量执行 route 继续通过 `StrengthWorkoutEngine` 和 `WorkoutCommand` 推进。
- 更新 E9.3 验收证据测试为 E9.4 修复证据，并新增 / 更新计时编辑、力量编辑和官方 shell 状态测试。
- `docs/testing/mvp-acceptance-checklist.md` 已记录 E9.4 修复结果，并将历史记录清理能力登记为 E12 / 持久化闭环后的后续项。
- 本 story 未实现真实 `WorkoutSession` 持久化、Room repository 业务闭环、历史记录真实删除、foreground service、后台可靠计时、真实心率设备、语音、完整跟练编排、总统计 / 图表或 E10 训练模式重构。

## Epic E10: 训练模式边界与执行页交互修正

目标：吸收用户测试后的产品判断，把计时训练、跟练、力量训练的边界和执行页主操作原则收口，避免后续实现继续沿用过宽的旧模式。

### Story E10.1: 训练模式边界与执行页交互原则记录

**状态:** Documented in `docs/planning/e10-training-mode-interaction-plan.md`

作为产品负责人，
我想记录计时训练、跟练/力量动作选择和执行页主操作原则，
以便后续 E10/E11/E12/E13 不在实现时临时决策。

**验收标准:**

- Then 决策日志记录计时训练回归纯间歇计时器，不再绑定动作库。
- Then UX 文档记录计时训练大圆盘执行页原则、阶段模型和主操作可达原则。
- Then roadmap/backlog 拆出 E10.2、E10.3、E10.4、E10.5、E10.6、E10.7、E10.8、E11、E12、E13。
- Then 用户测试反馈按后续阶段分流。
- Then 不实现 E10.2 UI，不改训练引擎，不新增真实记录持久化、心率设备、语音或统计图表。

**交付结果:**

- 新增 E10 训练模式交互计划，记录计时训练纯间歇计时器边界、阶段名称/时间/图标/颜色、阶段增删复制拖动排序、计划主题色、大圆盘执行页、暂停时长记录、固定阶段 cue 预留、统一执行页主操作原则和跟练/力量统一动作选择页。
- 用户反馈已分流：记录未真实闭环与历史清理进入 E10.4/E12，Timer Dial 圆盘视觉语言进入 E10.5-E10.8，心率数据源状态、设备优先策略和可选手动输入进入 E11，统计图表和趋势进入 E12，声音/女声 cue/音频共存进入 E13。
- 本 story 未实现任何生产 UI、训练引擎、持久化、手动心率、真实设备、语音、TTS、音频资源、foreground service、notification action 或统计图表。

### Story E10.2: 计时训练编辑页与执行页重做

**状态:** Implemented in Android timed interval editor, engine, session dial UI, contract docs, and regression tests

作为用户，
我想把计时训练当作纯间歇计时器编辑和执行，
以便快速创建热身、工作、休息、放松和自定义阶段。

**验收标准:**

- Given 计时训练编辑页，When 用户添加阶段，Then 可设置阶段名称、时间、图标和颜色。
- Then 支持添加、复制、删除和拖动排序阶段。
- Then 阶段卡可选择内置阶段颜色；计划主题色或整体配色编辑作为后续 polish，不作为 E10.2 阻塞验收。
- Given 计时训练执行页，When 训练开始，Then 大圆盘是核心视觉和主控制区。
- Then 顶部显示总剩余时间但不抢主层级。
- Then 中心显示当前阶段图标、阶段名称或编号和当前阶段倒计时。
- Then 圆环显示整体进度、当前阶段进度和轮次/阶段位置。
- Then 点击中心圆盘可暂停/继续，并记录暂停总时长。
- Then 记录语义预留本次总耗时、有效训练时间和暂停总时长。
- Then 最后 N 秒支持屏幕闪烁/动画提醒和声音提醒。
- Then 当前阶段开始只预留固定阶段词 cue，不实现用户任意文本 TTS。
- Then 不绑定动作库，不进入动作选择页，不展示动作详情或动作推荐。
- Then 外部大圆盘参考只能转化为 TrainFlow 自己的视觉语言，不逐像素照搬。

**禁止范围:**

- 不新增真实 session 持久化。
- 不接心率设备。
- 不实现语音、TTS 或音频资源。
- 不新增统计图表。
- 不重写 strength / follow-along engine。

**交付结果:**

- 计时训练编辑页已从动作编排式入口改为纯阶段编辑：阶段包含名称、时间、类型、图标 key 和颜色；阶段卡提供内置颜色 swatch，阶段类型选择会同步图标 key；支持添加、复制、删除、右侧手柄长按拖拽排序，并保留上移和下移作为备用排序路径。
- 拖拽排序只由阶段行右侧明确手柄触发；阶段名称输入、时间输入、类型选择、颜色 / 图标入口、复制 / 删除和行空白区域不触发排序，非手柄区域上下滑动继续滚动编辑页。
- E14.4-2 排序补修后，热身和放松作为默认模板阶段也可移动；工作、休息、自定义、热身和放松都按编辑页显示顺序执行。拖动中按相邻卡片半高阈值计算目标槽位，用非拖动卡片让位来预览排序，松手后提交真实顺序。计划主题色 / 整体配色编辑仍留作后续 polish。
- `TimedExerciseItem.exerciseId` 已改为可选，并新增 `TimedStageType`、`iconKey`、`colorHex` 以复用 `WorkoutPlan` / `TimedCircuitBlock` 表达纯 interval stage；跟练仍可继续用动作库 `exerciseId`。
- `TimedWorkoutEngine` 已支持纯阶段展开，`stageType=rest` 会生成真实休息步骤；暂停 tick 不推进有效训练时间，并通过 `pausedElapsedSec` 累计暂停时长。
- 计时训练执行页已改为大圆盘主视觉：顶部显示总剩余时间，中心显示阶段图标 key、阶段名和阶段倒计时，圆环显示整体进度与当前阶段进度，点击圆盘可暂停 / 继续；底部保留跳过、`+15秒` 和结束训练。
- 最后 N 秒继续复用现有 `WorkoutEvent` / `CountdownCue` / 声音震动动画边界，不新增语音、TTS 或音频资源。
- 新增 / 更新单元测试覆盖纯阶段计划、阶段增删复制排序、临时空输入、总时长、阶段顺序推进、暂停累计、恢复推进、终态防污染和 UI state 映射。
- 本阶段仍不实现真实 `WorkoutSession` 持久化、Room repository 闭环、历史真实写入 / 删除、心率设备、Health Connect / Wear OS / BLE、foreground service、exact alarm、notification action、语音、TTS、音频资源、统计图表、跟练 / 力量 UI 重做。

### Story E10.3: 力量/跟练执行页主操作可达性修复

**状态:** Implemented in Android strength/follow-along session routes, UI state metadata, end confirmation, and regression tests

作为训练中的用户，
我想不用滚动就能操作当前训练，
以便运动中快速暂停、继续、跳过、开始本组或确认本组。

**验收标准:**

- Then 力量训练的开始本组、完成本组、确认本组、暂停/继续和结束训练即时可达。
- Then 跟练训练的暂停/继续、跳过/下一步和结束训练即时可达。
- Then 结束训练即时可达但必须二次确认。
- Then 心率、说明、提示、下一步信息低于当前动作/阶段/时间/主操作。
- Then 不完整重做力量训练新版 UI，不改变力量记录确认语义。

**禁止范围:**

- 不重写力量训练或跟练训练引擎。
- 不改 `WorkoutCommand` / `WorkoutEvent` 语义。
- 不新增真实记录持久化、心率设备、语音或统计图表。

**交付结果:**

- 力量执行页保留现有 UI 信息架构，不做新版力量 UI 重做；开始本组、完成本组、确认本组、休息中提前开始本组、暂停 / 继续和结束训练进入固定底部控制区。
- 力量当前动作 / 组计时 / 休息倒计时主面板可作为暂停 / 继续入口，确认层输入仍保留滚动空间，不被固定控制区遮挡。
- 基础跟练执行页改为可滚动内容 + 固定底部控制区，暂停 / 继续、跳过 / 下一步和结束训练即时可达；倒计时区域也可暂停 / 继续。
- 两类执行页的结束训练先显示二次确认，确认后才分发 `WorkoutCommand.EndSession(reason = "user_requested")`，取消不会结束训练。
- 新增即时控制 metadata 和回归测试，覆盖力量 active/rest/confirm、跟练 active/paused、三套 skin 下 control metadata 不丢，以及结束确认 reducer。
- 本阶段未重写力量或跟练训练引擎，未改变 `WorkoutCommand` / `WorkoutEvent` / 力量确认记录语义，未新增真实记录持久化、心率设备、语音、TTS、音频资源、notification action 或统计图表。

### Story E10.4: 训练记录闭环前置

**状态:** Done and merged to `main`

作为用户，
我想完成训练后在记录页看到本次真实训练，
以便记录和后续统计不是 fixture 或内存态假数据。

**验收标准:**

- Then 完成计时/力量/跟练训练后写入真实 `WorkoutSession` 记录。
- Then 记录包含计划快照、实际执行结果、开始/结束时间。
- Then 计时训练记录区分本次总耗时、有效训练时间和暂停总时长。
- Then 记录页显示今天刚完成的训练。
- Then 真实记录源优先于示例 fixture；生产记录页不再用内存态 seed 伪装真实记录。
- Then E10.4 不实现假删除、假统计或假趋势。

**边界:**

- 可与 E12 真实统计前置项协调。
- 本阶段已接入最小 Room repository / DAO / mapper，保存 completed 与 abandoned session、完整 MVP 计划快照 blocks、计时步骤摘要、力量已确认组记录，以及 total / effective / paused 秒数。
- 计划快照写库必须保留计时阶段/轮次/休息结构、力量动作/计划组/目标/休息结构、preferences/cueSettings 和 followAlong 元数据；历史详情的计划步骤/组数从恢复后的 snapshot 计算，不再依赖空 blocks。
- `totalElapsedSec` 使用 startedAt 到 endedAt 的 wall-clock 总耗时，包含准备、确认、休息、正式组和暂停；`effectiveElapsedSec` 不包含暂停，力量训练当前不把 prepare / confirm 停留时间计入 effective；`pausedElapsedSec` 单独记录暂停累计。
- completed / abandoned 终态写库仍是本地 Room MVP，使用一次性 guard 和异常吞并边界避免重复插入或 Room 异常打断 UI；这不是云同步、统计图表、历史清理或后台可靠计时承诺。
- 不在 E10.4 中实现完整图表、趋势分析、历史记录清理、心率设备、Health Connect / Wear OS / BLE、语音、foreground service、exact alarm 或 notification action。

**交付结果:**

- E10.4 已完成 Review Gate PASS 并 fast-forward 合入 `main`。
- 本地真实 `WorkoutSession` write-through 已具备，计时 / 力量 / 基础跟练 completed 与 abandoned 终态可以写入 Room session records。
- 记录页生产入口读取真实本地记录；示例 fixture 仅保留给 preview / 测试。
- E10.5 以后不再处理记录闭环，不改 Room、DAO、session repository 或记录页数据源。

### Story E10.5: Timer Dial 设计工作流与重构范围

**状态:** Documented in `docs/planning/timer-dial-design-workflow.md`

作为产品负责人和设计负责人，
我想明确 Timer Dial 圆盘视觉语言重构的工具路线、设计边界、视觉规格、动效规格和后续实现拆分，
以便后续 E10.6/E10.7/E10.8 只做清晰的设计与实现验证，不把记录、统计、心率或声音能力混入本阶段。

**验收标准:**

- Then 文档记录 E10.4 已完成并合入 `main`，TrainFlow 已具备本地真实 session record write-through。
- Then E10.5 不再处理记录闭环，不改 Room/session repository。
- Then 文档明确外部 APK / 截图只用于观察和学习 UI / 交互，不复制代码、资源、图标、字体、音频或专有动画资产。
- Then 文档明确目标是 TrainFlow 自己的 Timer Dial 圆盘语言。
- Then 文档明确工具路线：Figma 做静态界面和规格，HTML / Canvas 可选验证动画，Jetpack Compose Canvas 是最终 Android 生产实现方式，Rive / Lottie 只用于小图标或装饰动效，APK 观察只做研究记录。
- Then 文档明确 Timer Dial 规格：顶部总剩余时间、外圈当前运动+休息周期、内圈总进度、中心圆当前阶段与暂停 / 继续、底部少量图标操作。
- Then 文档明确动效规格：阶段弧线推进、总进度推进、work / rest 颜色和粗细变化、阶段切换、暂停态和最后 N 秒提醒，并且全部来自 engine state。
- Then 文档明确黑红高对比只是参考方向，不新增第四套 skin。
- Then roadmap/backlog 拆出 E10.6、E10.7、E10.8，并保持 E12 统计图表和 E13 声音 / 女声 cue 独立。

**禁止范围:**

- 不解析或复制 APK 代码 / 资源。
- 不提交 APK、截图、录屏、反编译输出、日志或本地临时文件。
- 不写生产 Kotlin。
- 不改 Gradle。
- 不改 prototype。
- 不改 Room/session record。
- 不做手动心率、统计图表、语音 / TTS、真实设备、foreground service、exact alarm 或 notification action。
- 不新增第四套 skin。

**交付结果:**

- 新增 `docs/planning/timer-dial-design-workflow.md`，作为 E10.5 主文档。
- 决策日志记录 E10.5 docs-only 范围、工具路线和外部参考边界。
- E10 规划、UX、DESIGN 和项目状态同步 Timer Dial 视觉语言、动效和后续拆分。

### Story E10.6: Timer Dial Figma / static visual variants

**状态:** Documented in `docs/planning/timer-dial-static-visual-variants.md`

作为设计负责人，
我想在 Figma 中输出 Timer Dial 静态视觉方案和规格，
以便先比较风格、颜色、弧线厚度、中心圆和操作层级，再进入 Compose 原型。

**验收标准:**

- Then 至少输出 Official Flow 方向的 Timer Dial 静态规格。
- Then 执行页静态帧覆盖 active work、active rest、warmup / cooldown、paused、resume transition、stage transition、last-N-seconds cue、completed、abandoned、end confirmation 和 720x1280 小屏状态。
- Then 计时编辑页静态帧覆盖 header、阶段列表、阶段行 / 阶段卡、添加阶段 sheet、复制阶段、删除确认、颜色 / 图标 picker、快捷时长、时长细调、展开 / 收起、拖动排序、上移 / 下移、保存 / 取消反馈和小屏底部操作。
- Then 可探索黑红高对比、赛博霓虹、Tile Flow 和 Big Type 适配，但不新增第四套 skin。
- Then 规格覆盖顶部总剩余时间、外圈当前运动+休息周期、work / rest / warmup / cooldown 颜色区分、当前阶段粗弧、非当前阶段细弧、当前阶段弧线状态、内圈总进度、中心圆、底部图标操作、暂停态和最后 N 秒提醒状态。
- Then Timer Dial 进度必须绑定 `TimedWorkoutEngine` / UI state / `WorkoutEvent`，不允许视觉假进度。
- Then 结束训练仍需二次确认，最后 N 秒提醒不得遮挡主控制。
- Then 不实现 Android 生产 UI，不改 Kotlin / Gradle / prototype。

**禁止范围:**

- 不复制 APK 代码、XML、资源、图标、字体、音频、SVG/PNG、animated SVG、动画 XML、easing、duration、关键帧、路径、控件命名、资源命名或逐像素视觉。
- 不使用健身姿势动画或动作教学 animated SVG。
- 不新增第四套 skin。
- 不混入 E11 心率、E12 统计、E13 声音 / 女声 cue。
- 不启动 E10.7 Compose prototype。

**交付结果:**

- 新增 `docs/planning/timer-dial-static-visual-variants.md` 作为 E10.6 主文档。
- 文档按 Findings、E10.6 Design Scope、Timer Dial Static Frames、Editing Flow Static Frames、Interaction Animation Spec、Official Flow / Tile Flow / Big Type Adaptation、Accessibility And Small Screen Checks、Do Not Use / Legal Boundary、Suggested E10.7 Handoff Notes 和 Verification Notes 输出。
- 明确 Figma page / frame 分组建议、Official Flow 视觉 token、圆盘弧线厚度、执行页 11 组状态帧、计时编辑页 15 组状态帧、互动动画语义、三套内置 skin 适配、小屏 / 无障碍检查和法律边界。
- 决策日志记录 E10.6 静态规格必须先于 Compose 原型，并保持 engine-state-only 进度边界。

### Story E10.7: Timer Dial Compose prototype

**状态:** Implemented prototype in Android Compose

作为 Android 开发者，
我想用 Jetpack Compose Canvas 验证 Timer Dial 圆盘绘制、状态映射和关键动效，
以便生产集成前确认 engine state 驱动的进度、暂停和阶段切换都可靠。

**验收标准:**

- Then Compose Canvas 可表达外圈阶段弧线、内圈总进度、中心阶段和点击暂停 / 继续。
- Then 阶段弧线和总进度只来自计时训练 UI state / engine state，不使用视觉假进度。
- Then 覆盖 work / rest 颜色和粗细变化、阶段切换、暂停态和最后 N 秒提醒动效。
- Then 不改 Room/session repository，不做统计图表、声音、语音或真实设备接入。

**交付结果:**

- 在 `feature.workoutsession` 中新增 `TimerDialUiState`、`TimerDialTokens`、`TimerDial` Compose Canvas 组件和 `TimerDialPreview` demo。
- 现有计时执行页低风险接入 prototype 组件：中心点击仍只调用既有 pause / resume route callback，训练控制继续通过 `WorkoutCommand` 和 `TimedWorkoutEngine` 推进。
- `TimerDialUiState` 显式包含 total remaining、total progress、current stage progress、stage type / label / index / remaining、paused、final countdown、stage segments 和 visual variant；progress 在 UI state 层 clamp 到 `0f..1f`。
- 外圈按真实 engine steps 绘制阶段结构，work 粗弧、rest 细弧，warmup / cooldown / custom 使用差异化语义；内圈表达整次训练总进度；中心圆使用自绘阶段符号、阶段编号 / 名称 / 倒计时和 paused / final countdown 状态。
- 三类 prototype visual variant 已实现：黑红高对比、赛博霓虹、TrainFlow Official Flow 融合；它们不是新增第四套 skin，也不改变 Official Flow / Tile Flow / Big Type registry。
- 新增单元测试覆盖 progress clamp、total / stage progress mapping、work/rest stroke semantics、visual variant token 数量、final countdown flag 和 paused state mapping。
- 本阶段未改 Room/session repository、训练记录业务逻辑、workout engine 语义、声音 / TTS / 女声 cue、统计图表、心率设备、foreground service、exact alarm、notification action 或前端 prototype。E10.8 已接续完成 production integration and animation polish，等待 review。

### Story E10.8: Timer Dial production integration and animation polish

**状态:** Implemented in Android production timer route, pending review

作为计时训练用户，
我想在生产计时执行页使用完整 Timer Dial 圆盘语言，
以便运动中更直观地看到阶段结构、当前进度、总进度和主控制。

**验收标准:**

- Then 生产计时训练执行页使用通过 E10.6/E10.7 验证的 Timer Dial。
- Then Official Flow、Tile Flow 和 Big Type 都有明确适配，不新增第四套 skin。
- Then 暂停 / 继续、跳过 / 下一阶段、`+15秒` 和结束操作保持即时可达，结束训练仍需二次确认。
- Then reset remains preview/demo or future command design, not an E10.8 production control.
- Then 动效继续由 engine state / `WorkoutEvent` / UI state 驱动。
- Then 小屏可读性、主控制可达性和终态不污染进度有回归验证。
- Then 不混入 E12 统计图表、E13 声音 / 女声 cue、心率设备或后台可靠计时。

**交付结果:**

- 生产计时训练执行页默认使用 Official Flow Timer Dial；Black / Red High Contrast 与 Cyber Neon 仅保留为 preview/demo visual variants，不进入 Official Flow / Tile Flow / Big Type skin registry。
- Timer Dial 外圈从整次训练全部阶段收窄为当前一次运动+休息周期；work active 时 work 粗弧填充、rest 细弧，rest active 时 rest 粗弧填充、已经过的 work 细弧；外圈和内圈进度动画均使用线性推进。
- 内圈按运动阶段数量表达整次训练总进度，不画未经过底轨；12 点数字圆标显示总运动阶段数，一个阶段包含 work+rest，最新完成节点显示数字，之前完成节点退为实心圆点。
- 顶部精简为总剩余时间；圆盘卡移除重复阶段标签、计划标题、步骤和进行中状态；中心圆显示当前阶段倒计时并承担暂停 / 继续点击。
- 底部跳过和结束改为图标按钮，结束训练接入二次确认；`+15秒` 只在 active rest 可用，用于延长当前休息 15 秒，不修改原计划。
- Rest extension progress 按单调、不倒退、状态驱动口径修复：`+15秒` 后当前 rest 外圈弧和内圈 work+rest cycle progress 保持不小于延长前，并在 active tick 继续推进；paused 和 terminal 状态不推进。
- Reset 口径收敛为 preview/demo 或未来命令设计项；E10.8 production controls are `skip`, `+15秒`, `end`。未来若实现 reset，需要先明确 `WorkoutCommand`、二次确认、session record 边界和测试。
- 新增/更新单元测试覆盖 production 默认 variant、preview-only variant 边界、current-cycle outer segments、current segment stroke semantics、rest extension progress 不倒退、paused / terminal 冻结、7 阶段 45+15 内圈 marker progress、final countdown 偏好开关和 timed route end confirmation。
- 已运行 `app:testDebugUnitTest`、`app:assembleDebug`、`app:lintDebug`、`app:check`、`git diff --check HEAD` 和 `git diff --cached --check`。720x1280 emulator visual smoke 覆盖 active、paused、rest + `+15秒`；最后 N 秒截图窗口本轮未稳定捕获，单元测试已覆盖提醒 flag。
- 本阶段未改 Room/session repository、训练引擎语义、记录统计、心率设备、foreground service、exact alarm、notification action、声音 / TTS / 女声 cue、前端 prototype 或第四套 skin。

### Story E10.9: Timer Dial reference polish / continuous progress / user-test APK

**状态:** Implemented in Android Timer Dial polish branch

作为计时训练用户，
我想 Timer Dial 圆环在秒级 engine tick 之间也能连续推进，
以便训练中进度反馈更接近真实流动，而倒计时数字仍保持清晰的秒级更新。

**验收标准:**

- Then active 状态下 Timer Dial 用 Compose frame clock 对当前 1 秒做 bounded progress projection，外圈当前阶段弧和内圈总进度在 engine tick 之间连续推进。
- Then 中心倒计时文案仍只来自 engine/UI state 的秒级文本，不做毫秒级文案抖动。
- Then paused、completed、abandoned 和不可暂停/继续状态不投影进度。
- Then 投影最多覆盖当前 1 秒，后台恢复、卡顿或长帧不会让圆环超跑。
- Then `+15秒` rest extension 后当前 rest 外圈弧和内圈 work+rest cycle progress 不倒退，并在 active 状态继续推进。
- Then production controls 仍是 skip、`+15秒`、end；不新增 reset command。
- Then `r-design.md` 只作为参考设计桥接文档纳入分支，不替代官方 `DESIGN.md`。
- Then 生成可安装 debug APK 供用户测试。
- Then 不进入 E11 手动心率、E12 统计图表 / 历史趋势或 E13 声音 / 女声 cue。

**交付结果:**

- `TimerDial` 改用 Compose `withFrameNanos` 记录当前 UI state key 下的帧间 elapsed，并把 elapsed clamp 到 1,000ms 后交给 `TimerDialUiState` 投影函数。
- `TimerDialUiState` 新增可测试的 smooth projection helpers：active 且可暂停/继续时才推进；paused / completed / abandoned 冻结；fallback 总进度、当前 cycle 总进度和当前 stage 进度均 clamp 到 `0f..1f`。
- `+15秒` rest extension 后继续使用 E10.8 的单调 floor，投影值不低于 base `totalProgress` / `currentStageProgress`，并按剩余秒数继续前进。
- 新增/更新单元测试覆盖秒间 stage projection、秒间 inner progress projection、投影最多 1 秒、paused / completed / abandoned 冻结和 rest extension 后投影单调。
- `r-design.md` 记录外部参考项目只读发现、禁止复制范围、颜色角色、动效原则和 TrainFlow 适配边界；外部 APK、`人工/`、`.local/` 与 build 输出均不提交。
- 本阶段未改 Room/session repository、训练引擎语义、记录统计、心率设备、foreground service、exact alarm、notification action、声音 / TTS / 女声 cue、前端 prototype 或第四套 skin。

### Story E10.9 Review Fix / User Test Fix: Timer Dial visual reduction and ring polish

**状态:** Planned from user-test feedback

作为计时训练用户，
我想执行页进一步减少文字、放大圆盘和总剩余时间，
以便运动中主要依靠圆盘、图标、颜色和倒计时理解训练状态。

**验收标准:**

- Then Timer Dial 继续保留 E10.8/E10.9 已验证的 continuous progress、pause freeze、terminal freeze 和 rest extension monotonic progress。
- Then 执行页移除或弱化“总剩余”文字标签、下一阶段提示框、已启用声音提示框等非必要文案。
- Then 总剩余时间更大、更居中，并在层级上低于中心倒计时但高于辅助说明。
- Then 圆盘整体放大；外圈和内圈线条同比例变细，避免 marker 与外圈视觉重叠。
- Then 增加内圈总进度线下方的宽底层圆环，宽底层只作为进度承托，不制造假进度。
- Then 底层圆环上的浅色小点复用内圈阶段 marker 的动态角度计算；阶段数量、阶段时长或轮次变化时，小点位置必须随同变化，不做固定装饰点。
- Then 中心圆只保留阶段图标、必要编号和当前阶段时间，减少“阶段01”“训练”等解释性文字。
- Then 中心圆填充使用当前阶段预设色，中心文字和图标使用白色并保持对比度。
- Then 不接入声音、不实现计划保存、不修改统计/记录语义，不复制外部 APK 或参考项目资源。

### Story E10.10: Plan persistence and save-entry audit

**状态:** Implemented in Android Room plan repository and save-entry audit

作为训练计划用户，
我想自定义计时阶段、秒数、轮次、颜色和图标能真实保存并在退出后恢复，
以便计划编辑不是一次性的临时草稿。

**验收标准:**

- Then 自定义计时训练阶段、秒数、轮次、颜色、图标、名称和排序保存到本地持久化计划。
- Then 退出 App、切换页面或重新进入计划详情后，计时计划按保存内容恢复，不回到默认值。
- Then 检查计时、力量和跟练相关计划保存入口是否真实可用；不可用入口必须改为明确禁用、待实现或进入对应 story。
- Then 计划保存继续遵守 `WorkoutPlan` 存目标和结构、`WorkoutSession` 存实际执行结果和计划快照的边界。
- Then 不改训练执行引擎语义，不实现声音播放，不做统计图表，不提交 `.local`、APK、`人工/`、build 输出或截图日志。

**交付结果:**

- `core.data.WorkoutPlanRepository` 接入 `workout_plans` Room 表，提供本地计划 observe / read / upsert / delete，并继续向 UI 输出 `WorkoutPlan` domain model，不暴露 Room entity。
- 计划 blocks / reminder / preferences / followAlong 复用 E10.4 plan snapshot JSON 编解码边界；计时计划的自定义阶段名称、秒数、轮次、轮间休息、颜色、图标、类型和排序可 round-trip 保存与恢复。
- 计时编辑页和力量编辑页的“保存计划”入口改为真实本地保存；`立即开始` / `开始力量训练` 仍可直接启动当前有效草稿，但不伪装成保存。
- 计划页改为消费本地 `workoutPlans`，空状态不再展示 seed 内存态计划；复制、删除、设置提醒和关闭提醒都会同步写回本地计划。删除计划不改写既有 `WorkoutSession` 历史快照。
- 跟练当前只有清楚标识的基础 preset 启动入口，没有保存按钮；计划详情里的 follow-along 计划启动保持禁用并标为“待完整编排”，不留下假保存入口。
- 新增/更新测试覆盖 plan repository Room round-trip、计时自定义阶段持久化、力量计划目标结构不写 session records、编辑保存文案和计划页本地状态恢复。
- 本阶段未改训练执行引擎语义，未实现声音播放、统计图表、心率设备、foreground service、exact alarm、notification action、prototype 前端或 Timer Dial 视觉，也未提交 `.local`、APK、`人工/`、deliverables、截图、日志或 build 输出。

### Story E10.11: Huashu Timer Dial HTML prototype exploration

**状态:** Implemented; HTML prototype ready for review

作为设计探索者，
我想用已安装的 `huashu-design` skill 做 Timer Dial 高保真 HTML 原型方向，
以便在不改生产代码前比较视觉方案和状态表现。

**验收标准:**

- Then 使用 `huashu-design` skill 输出 3 个 HTML 高保真 Timer Dial 原型方向：黑红高对比、TrainFlow Official 融合、赛博霓虹。
- Then 每个方向覆盖 active、rest、paused、final 5 seconds 和 rest extension。
- Then 原型只使用 TrainFlow 自己的 HTML/CSS/Canvas/SVG/图标语义，不复制外部 APK 或参考项目代码、资源、图标、字体、音频、命名、动效参数或逐像素视觉。
- Then 原型结果服务 E10.9 Review Fix 或后续视觉评审，不自动进入生产实现。
- Then 不修改 Kotlin/Gradle/prototype，不接入音频，不移动或提交根目录 APK、`.local/`、`人工/`、build 输出、截图或日志。

**交付结果:**

- `docs/prototypes/e10-11/index.html` 新增纯 HTML/CSS/Canvas 原型入口，支持 Black / Red High Contrast、TrainFlow Official Fusion 和 Cyber Neon restrained version 三个方向切换。
- 每个方向复用同一套状态数据，覆盖 active work、rest、paused、final 5 seconds 和 rest extended by `+15`，并展示大号总剩余时间、更大的圆盘、更细的外圈 / 内圈、内圈总进度下方的宽底层圆环、阶段色中心圆和最少文字。
- 原型中的内圈阶段 marker 与底层浅色小点复用同一套动态角度计算，避免固定装饰点；rest extension 使用 floor cue 表达进度不倒退。
- `docs/prototypes/e10-11/README.md` 记录三套方向的设计意图、建议进入 Android Compose 生产实现的元素、仅探索不建议进入 MVP 的元素、`TimerDialUiState` / Compose Canvas 映射和后续声音 cue 位置。
- 本 story 只提交 HTML / Markdown 设计探索与状态文档更新；未修改 Android Kotlin、Gradle、React prototype、训练引擎、Room/session repository、声音播放、计划保存、统计图表、心率设备、foreground service、exact alarm、notification action 或第四套 skin，也未提交 `.local`、APK、`人工/`、截图、日志或 build 输出。

### Story E10.12: Timer Dial Compose landing

**状态:** Implemented in Android Compose production Timer Dial

作为计时训练用户，
我想把 E10.11 中最适合作为生产候选的 `TrainFlow Official Fusion` 方向落到真实执行页，
以便运动中主要依靠更大的圆盘、阶段色、图标、marker 和倒计时理解训练状态。

**验收标准:**

- Then 生产默认方向使用 TrainFlow Official Fusion，不把 Black / Red 或 Cyber Neon 作为默认生产 UI，也不新增第四套 skin。
- Then 计时执行页移除“总剩余”文字标签、下一阶段提示框、提醒说明 / 已启用声音提示框和训练中控制历史提示。
- Then 总剩余时间更大、更居中，Timer Dial 整体放大并适配 720x1280 小屏。
- Then 外圈 / 内圈线条同比例变细，marker 与外圈 / 中心圆保持清晰间距。
- Then 内圈总进度线下方新增宽底层浅色圆环；底层浅点复用内圈阶段 marker 的同一套动态 marker 数据。
- Then 中心圆使用当前阶段色填充，内部只保留白色或高对比 token 的图标、必要编号和阶段剩余时间。
- Then final 5 seconds 只做轻量强调，paused 状态冻结并克制表达，rest extension 后外圈和内圈 progress 不倒退。
- Then 保留 E10.9 active Compose frame clock continuous progress、秒级文案 tick、paused / terminal freeze、最多投影当前 1 秒和 rest extension monotonic progress。
- Then 不接入声音播放、不复制音频到 `res/raw`、不实现计划保存、不做统计图表、不改 Room/session repository、不接真实心率设备、不新增 foreground service / exact alarm / notification action。

**交付结果:**

- `TimerDial` Canvas 使用放大的 layout spec、变细外圈 / 内圈、宽底层内圈、同源动态浅点和 marker 绘制语义；Official 默认 token 继续映射现有 TrainFlow skin token。
- `TimerDialUiState` 新增可测试的内圈 marker 数据和 marker progress helper，Canvas 与测试共用该数据，避免浅点成为固定装饰。
- 生产执行页移除训练中不会看的说明卡，保留大总剩余时间、Timer Dial 和底部 skip / `+15秒` / end controls；E11.3 后不再保留辅助心率 UI。
- 单元测试覆盖同源 marker 数据、marker / ring layer 语义、Official token 映射、final countdown、paused / terminal freeze 和 rest extension monotonic progress。
- Review fix 重新布局 marker 轨道，所有内置 skin 的 center gap、outer gap 和 marker internal gap 均以 `3.5dp` 最小间距测试约束；暂停态中心圆保留整圆可点击继续语义和“继续训练”可访问文案。本轮未实现 ready/start gate、Stage color picker、motion timing rules、声音、统计图表、Room/session repository 语义、真实心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

### Story E10.13: Ready Start Gate

**状态:** Implemented in timed workout route ready/start gate

作为计时训练用户，
我想从编辑页或计划详情点开始后先确认自己已经准备好，
以便由我主动点击圆盘后再真正开始训练。

**验收标准:**

- Given 计时训练 route，When 从编辑页“立即开始”或计划详情“开始计时训练”进入，Then 初始显示同一个 ready/start gate，而不是自动开始训练。
- Then ready gate 展示计划名、大中心圆、播放图标和低层级预计总时长 / 阶段数 / 轮数。
- Then 点击中心圆任意区域才 dispatch `WorkoutCommand.StartSession`，不是只有播放图标可点击。
- Then ready 状态不推进 engine tick、不增加 `activeElapsedSec`、不触发 countdown reminder / sound / haptics。
- Then ready 状态离开页面不写 abandoned session record；completed / abandoned 本地记录仍只在真实启动后写入。
- Then 保留 E10.9 / E10.12 的 continuous progress、pause freeze、terminal freeze 和 rest extension monotonic progress。
- Then 不实现 rest extension recording、motion timing rules、Stage color picker、声音播放、统计图表、Room/session repository 新语义、真实心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

**交付结果:**

- `TimedWorkoutSessionRoute` 初始只创建 `TimedWorkoutEngine` 的 `READY` state，移除进入 route 后立即派发 `StartSession` 的行为。
- 新增极简 ready/start gate UI，复用深色训练执行页和中心圆语言；点击中心圆本体设置真实 `startedAt` 并通过 `WorkoutCommand.StartSession` 进入既有 Timer Dial 执行页。
- tick loop、countdown reminder feedback 和 terminal session record 写入均加上 ready/started 边界，避免 ready 状态误推进、误反馈或误写废弃记录。
- 新增 `TimedReadyStartGateTest` 覆盖 ready 初始状态、tick 不推进、中心圆启动、ready 不触发反馈、ready 不写 abandoned、真实启动后终态写入，以及编辑页入口和计划详情入口共用 ready gate。
- 本阶段未改训练执行引擎核心语义、Room/session repository 语义、prototype 前端、声音资源、统计图表、心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

### Story E10.14: Rest Extension Semantics And Recording

**状态:** Implemented in timed rest extension session records

作为计时训练用户，
我想 `+15秒` 明确表示延长当前休息阶段，并让 App 记录每次额外休息，
以便后续能回看和分析哪些阶段、轮次或计划后更常需要恢复时间。

**验收标准:**

- Given 计时训练处于 active rest step，When 用户点击 `+15秒`，Then 当前休息剩余时间增加 15 秒，不插入新 step，不修改原 `WorkoutPlan` 或 plan snapshot。
- Then 生产 UI 采用二段式确认：第一次点击只显示 `确认 +15秒`，2 秒内第二次点击才真正延长并记录；超时自动恢复，不记录、不加时。
- Then 确认成功后短暂显示 `已加 15秒`，约 800ms 后恢复。
- Then 每个 rest step 最多确认成功 4 次，即最多额外休息 60 秒；达到上限后禁用该休息阶段的 `+15秒`，提示“已额外休息 1 分钟，需要更久可以暂停训练”，但不自动暂停。
- Then 额外休息不同于暂停：不增加 `pausedElapsedSec`，不冻结训练流程，继续计入本次训练 active / total 用时。
- Then 每次延长都会保存可持久化记录，至少包含 session、round index、step index、当前 rest step / 阶段、前一个 work/custom 阶段、addedSec、plannedRestSec、点击时剩余秒数、已休息秒数和当前 rest 累计 extra rest。
- Then `extensionCount`、`cumulativeExtraRestSec` 和 `hitExtensionLimit` 在 UI / 分析层可从当前 rest step 的 extension records 推导；数据记录只发生在二次确认成功后。
- Then completed 和 abandoned 终态都会写入已发生的 rest extension records；ready gate 未真实启动时不会产生 rest extension record。
- Then Timer Dial 的 rest extension monotonic progress、pause freeze 和 terminal freeze 不回归。
- Then 总结页展示最小额外休息摘要，例如 `额外休息 +30秒` 和 `第 1 轮 工作后 +30秒`。
- Then 本阶段不实现 E12 统计图表 / 趋势分析、不接真实心率设备、不实现 motion timing rules、Stage color picker、声音播放、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

**交付结果:**

- `core.engine.TimedWorkoutEngine` 保持 `WorkoutCommand.ExtendRest` 只在 active rest 生效，并为每次额外休息补齐 step index、round、planned rest、点击时剩余秒数、已休息秒数、前一个阶段和当前 rest 累计 extra rest。
- `feature.workoutsession` 在 `TimedWorkoutSessionRoute` / UI state 层加入 `+15秒` 二段式确认；第一次点击不派发 `ExtendRest`，第二次确认成功才派发，pending confirm 会随当前 rest step 改变或 2 秒超时清除。
- 每个 rest step 的确认上限为 4 次 / 60 秒；达上限后 UI 禁用并提示可暂停训练，但不会自动进入暂停。
- `core.model.WorkoutSession` 新增 `timedRestExtensionRecords`，作为实际 session 结果保存；`WorkoutPlan` 和 `WorkoutPlanSnapshot` 仍保留原计划结构和计划休息秒数。
- Room 数据库升级到 version 4，新增 `timed_rest_extension_records` 表、DAO relation、repository round-trip 和 3→4 migration / schema 导出；repository 读回按 `eventElapsedSec -> stepIndex -> cumulativeExtraRestSec -> id` 排序，覆盖同秒 10+ 条记录，避免字符串 id 顺序错位。
- 计时终态记录 mapper 将 engine rest extension history 写入真实 `WorkoutSession`；completed / abandoned 都会保存已发生记录，ready gate 未启动时仍不记录。
- 计时训练总结最小展示额外休息总量、次数和前序阶段明细；没有实现复杂统计、图表、趋势或同日多轮分析。

### Story E10.15: Motion Timing Rules

**状态:** Implemented as motion timing tokens and rules

作为训练中的用户，
我想界面反馈有统一、克制且可中断的节奏，
以便 ready gate、Timer Dial、play/pause、阶段切换和页面切换不会妨碍下一次有效操作或真实计时。

**验收标准:**

- Given 训练交互动效规则，Then 触摸反馈时长在 `80-120ms`，元素状态切换在 `120-180ms`，局部布局切换在 `180-240ms`，页面切换在 `220-300ms`。
- Then motion token 集中在设计系统 / theme 边界，不把 duration / easing 写成散落 magic number。
- Then reduce-motion 有明确 fallback：非必要动效可禁用，状态切换 snap，continuous projection 可关闭。
- Then Timer Dial continuous progress 仍只是 UI projection；paused / terminal / ready 未启动状态不继续推进。
- Then 动画不改变 engine state、不派发 `WorkoutCommand`、不伪造 `WorkoutEvent`、不写 session record、不影响 `pausedElapsedSec`、extra rest 或 total elapsed 口径。
- Then `+15秒` 二段式确认、rest extension monotonic progress、pause freeze 和 terminal freeze 不回归。
- Then 本阶段不实现 E10.16 Motion Landing、不大改 ready gate / Timer Dial / summary / navigation 动画、不实现 Stage color picker、声音播放、统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

**交付结果:**

- `ui.theme.TrainFlowMotionTokens` 新增 touch feedback、state transition、local layout transition、page transition、continuous projection、reduce-motion policy、touch scale、alpha 和 easing token。
- `TimerDial` 的 final countdown pulse 改为消费 motion token；Timer Dial 秒间 continuous projection 继续沿用 1 秒上限，并保持 UI projection 语义。
- 新增 `TrainFlowMotionTokensTest` 覆盖 duration range、命名用途、reduce-motion fallback 和 token 值边界。
- `DESIGN.md` 新增 motion token 和 motion rules，明确动画服务训练节奏、可中断、状态驱动、reduce-motion 降级，以及不驱动业务状态。
- 本阶段只做规则和最小 token 接入，没有实现完整 Motion Landing、页面切换动画落地、color picker、sound、stats、heart-rate device 或其他后续能力。

### Story E10.16: Motion Landing

**状态:** Implemented as minimal timed workout motion landing with reduce-motion review fix

作为计时训练用户，
我想 ready gate、中心圆、阶段变化和 `+15秒` 确认反馈有短促、统一且可中断的动效，
以便界面能确认我的操作但不阻塞下一次点击或真实训练计时。

**验收标准:**

- Given E10.15 motion token，Then ready gate、center dial、play/pause、Timer Dial marker / ring 状态和 `+15秒` 反馈都使用集中 motion specs，不写散落 magic duration。
- Then ready gate -> execution 只做局部切换；点击 center circle 仍立即通过 `WorkoutCommand.StartSession` 启动。
- Then center dial play/pause glyph、中心圆颜色 / 边框和按压反馈只消费 UI state，不改变 engine state。
- Then Timer Dial marker / ring / center color 状态变化可以平滑，但 continuous progress 仍只是 bounded UI projection，paused / terminal 不推进真实进度。
- Then `+15秒` label 在 `+15秒` / `确认 +15秒` / `已加 15秒` 之间使用 state transition 和 touch feedback，但第一次点击仍只进入 pending，第二次确认才 dispatch `ExtendRest(15)`。
- Then reduce-motion specs 降级为 `0ms` snap，并由生产 UI 消费系统 animation scale / local reduce-motion source，不改变业务状态。
- Then 本阶段不实现大型页面转场系统、Stage color picker、声音播放、E12 统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 prototype 前端改动。

**交付结果:**

- `feature.workoutsession.TimerDialMotionSpecs` 新增 ready/execution local layout、touch feedback、play/pause state、marker/ring state、color state、`+15秒` state 和 reduce-motion snap specs，全部消费 `TrainFlowMotionTokens`。
- Review fix 新增 `LocalTrainFlowReduceMotion` / `rememberTrainFlowReduceMotion()`，由 Android root composition 读取系统 animator / transition / window animation scale 后提供给生产 Compose UI；当前只作为训练执行 motion landing 的降级路径，不代表完整无障碍设置系统。
- `TimedWorkoutSessionRoute` 使用局部 `Crossfade` 承载 ready gate -> execution；ready center circle 和 `+15秒` 控件加入 tokenized touch feedback，`+15秒` label 使用 state transition，并在 reduce-motion 时 snap / 关闭非必要触摸 scale。
- `TimerDial` center dial 加入 tokenized touch feedback、play/pause glyph state transition、center color / border transition，以及 marker / ring alpha state transition；reduce-motion 时 final countdown pulse 关闭，continuous projection 不启动 frame loop，只消费 engine / UI state tick。进度投影、pause freeze、terminal freeze 和 rest extension monotonic progress 语义不变。
- 测试覆盖 motion spec token usage、真实 reduce-motion source / production call site 消费、reduce-motion fallback、ready gate motion 不启动 session、play/pause 和 `+15秒` 使用 state transition、`+15秒` motion 不改变二段确认规则，并保留既有 Timer Dial / rest extension 回归。
- 本阶段未改变 `WorkoutCommand` / `WorkoutEvent` / engine / session record / extra rest / paused elapsed 语义，未新增声音、统计、设备、通知 action、reset production command 或第四套 skin。

### Story E10.17: Stage Color Picker

**状态:** Implemented in Android timed plan editor, stage color presets, persistence fallback, and Timer Dial color consumption

作为计时训练用户，
我想从推荐色和更多颜色中为每个计时阶段选择颜色，
以便训练中能更快识别热身、工作、休息、放松或自定义阶段。

**验收标准:**

- Given 计时阶段编辑页，When 用户打开阶段颜色选择，Then 先展示 5-8 个推荐色，并提供覆盖 handoff 20 个 Material-like 色值的更多颜色。
- Then 色板由集中 `StageColorPreset` 定义，包含 id、name、hex、tone、recommendedUse、textColor、isHighAttention 和 isRecommended，不在 UI 中散落 hex。
- Then 选中态不能只靠颜色表达，必须有形状 / 描边、对勾和 TalkBack 文案。
- Then 选择颜色后更新当前阶段 `colorHex`，并映射到保存的 `WorkoutPlan.blocks` / `TimedExerciseItem.colorHex`。
- Then 已保存计划从本地持久化恢复后继续保留阶段色，进入详情、ready gate 和执行页时继续消费该阶段色。
- Then 执行页深色背景下，Timer Dial 中心圆和当前周期外圈消费阶段色，圆内文字 / 图标使用 preset `textColor` 或安全 fallback 保持高对比。
- Then 非法 `colorHex` 在编辑状态、JSON 读回和执行页 UI state 中回退到阶段默认安全色，不崩溃。
- Then 本阶段不新增第四套 skin、远程主题、运行时插件市场、统计图表、声音播放、真实心率设备、foreground service、exact alarm、notification action、reset production command，也不改变 `WorkoutCommand` / `WorkoutEvent` / engine / session record 语义。

**交付结果:**

- `core.model.StageColorPreset` 新增推荐色 / 更多色集中色板、hex 校验、lookup、fallback 和 textColor helper；推荐色数量控制在 5-8 个，更多色覆盖 handoff 中 20 个 Material-like 色值。
- `feature.plans` 的计时阶段编辑页从内联 8 色 swatch 升级为颜色选择对话框；阶段卡展示当前 swatch，可打开推荐色 / 更多颜色色板；每个色块尺寸稳定，选中态有外圈和对勾，语义文案包含色名、用途、高注意色和选中状态。
- `TimedPlanEditorScreenState.updateStageColor` 规范化合法 hex，非法值回退当前阶段默认色；picker UI state 区分推荐色、更多色和当前选中项，并暴露 `hasCheckIndicator` 语义。
- `WorkoutPlanSnapshotStorageJson` 读回时规范化 `colorHex`，保护 E10.10 本地计划持久化 round-trip；旧数据或损坏 JSON 不会把非法颜色传入执行页。
- `TimedWorkoutSessionScreenState` / `TimerDialUiState` / `TimerDial` 现在携带并消费当前阶段和当前周期 segment 的 `colorHex`，中心圆文字 / 图标使用 preset `textColor`，非法色回退阶段默认色。
- 测试覆盖 preset 字段、picker state、颜色选择到 plan mapping、非法色 fallback、计划持久化恢复、Timer Dial 消费自定义阶段色，并保留 ready gate、rest extension 和 reduce-motion 回归。
- 本阶段未改变训练引擎、命令、事件、session record、`+15秒` 二段确认、rest extension 记录、声音、统计、真实心率、通知 action 或 skin registry。

### Story E10.18: Plan Edit Backfill

**状态:** Implemented in Android plan detail edit entry, timed / strength editor backfill, same-plan save, and snapshot-safe plan editing

作为已经保存过训练计划的用户，
我想从计划详情进入编辑并看到原来的计划内容被回填，
以便不用重新创建计划也能调整阶段、动作和目标。

**验收标准:**

- Given 计划详情页展示本地计时或力量计划，When 用户点击编辑，Then 进入对应编辑器并回填当前 `WorkoutPlan` 的标题、描述、结构和目标。
- Then 计时计划编辑回填阶段顺序、阶段类型、名称、时长、轮次、轮间休息、颜色、图标、整体 cue settings 和阶段级 cue settings。
- Then 力量计划编辑回填动作顺序、动作 ID / 名称、重量目标、次数目标、热身组 / 正式组数量、每组计划覆盖、组间休息、左右侧设置、替代动作和 set id。
- Then 编辑已保存计划后保存回同一个 `WorkoutPlan.id` 并保留 `createdAt`，只更新当前计划结构、目标和 `updatedAt`。
- Then 编辑已保存计划时保留原 reminder 和非当前编辑器管理的 preferences；计时编辑只替换 `preferences.cueSettings`，力量编辑保留原 `preferences`。
- Then 编辑保存后，如果 reminder enabled，则先取消同 plan id 的旧提醒再按当前 reminder / permission / policy 重新调度；如果 reminder disabled 或 null，则确保旧调度被清理。
- Then 复制计划仍生成新计划 ID，删除计划仍不改写历史训练记录，设置提醒 / 关闭提醒 / 开始训练语义不变。
- Then 既有 `WorkoutSession.planSnapshot` 不被回写；历史记录继续按训练当时的计划快照展示和供后续统计使用。
- Then 跟练计划不暴露假的完整编辑入口，直到完整跟练编排另开 story。
- Then 本阶段不实现计划版本历史、撤销 / 重做、云同步冲突解决、完整跟练编排、统计图表、声音播放、真实心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

**交付结果:**

- `PlanManagementRoute` 的计划详情页新增编辑入口；计时和力量计划可进入对应编辑器，跟练计划保持不可编辑文案，不伪造完整编排能力。
- `TimedPlanEditorRoute` / `TimedPlanEditorScreenState` 支持从已保存 `WorkoutPlan` 回填，保留 plan id、createdAt、description、rounds、restBetweenRounds、阶段顺序、阶段颜色 / 图标、cue settings、原 reminder 和非 cueSettings preferences；保存时更新同一个本地计划并只替换 cueSettings。
- `StrengthPlanEditorRoute` / `StrengthPlanEditorScreenState` 支持从已保存 `WorkoutPlan` 回填，保留 plan id、createdAt、动作 block id、set id、目标重量、次数目标、每组计划覆盖、休息、替代动作、原 reminder 和原 preferences；保存时更新同一个本地计划。
- `OfficialShellState` 新增 plan edit source 状态，创建入口会清理 edit source，保存后回到计划页并选中更新后的计划；编辑保存复用既有 plan reminder replacement 路径，先取消旧提醒，再按 reminder / permission / policy 重新调度或清理；复制和删除语义不变。
- 单元测试覆盖计时 / 力量回填、same-id save、malformed plan 安全 fallback、reminder / preferences preservation、编辑保存 reminder cancel + reschedule / clear、create mode 不继承 reminder、copy plan reminder 语义、历史 snapshot 不回写、计划详情 edit availability 和 shell route mode 切换。
- 本阶段没有改写既有 `WorkoutSession.planSnapshot`，没有实现版本历史、云同步、完整跟练编辑、统计、声音、真实心率、通知 action、reset production command 或 skin registry。

### Story E10.x: 后续力量训练新版 UI 设计

力量训练完整新版 UI 设计单独开启，不塞进 E10.3。该阶段可重审力量训练信息架构、确认层、历史趋势入口和高级组设置，但必须保留计划值预填实际记录、训练命令、训练事件和核心引擎语义。

## Epic E11: 心率数据源策略与设备接口边界（历史实施阶段）

历史目标：收敛健康数据和设备接口边界，并在当时的 HUAWEI Band 9 smoke 失败后撤下旧心率 UI。E11 当时不显示、不录入、不统计心率，`HeartRateState` 当时只保留未来平台适配边界；这些是 E11 的范围与实现事实。D-080 已 supersede “全面不显示心率、不接真实设备”的产品范围，但不恢复旧内联心率卡片、手动输入、虚假占位或无数据支撑的旧趋势。当前 E17 采用显式 opt-in、标准 HRS 和冻结胶囊；production 受 E17-3 条件式 merge gate 与 E17-4 readiness 约束。

### Story E11.1: Heart-rate source boundary / unavailable state refinement

**状态:** Implemented as Android source-aware provider/model boundary; production UI no longer displays heart rate

作为用户，
我想让健康数据边界保留在模型层，
以便未来设备接入可以复用抽象状态，但当前训练执行页不展示不可用的心率能力。

**验收标准:**

- Then `HeartRateState` 可表达 `unavailable / no_source`、设备无读数、设备读数、手动读数、过期读数、provider unavailable 和 permission unavailable。
- Then 当前生产执行页不显示心率卡片，也不显示“未获取心率”占位。
- Then `HeartRateState` 必须携带 `sourceKind: none | device | manual`，手动数据不得伪装成设备数据。
- Then 继续保留 source-aware `HeartRateProvider: Flow<HeartRateState>` 抽象。
- Then 不接 Health Connect、Wear OS、BLE 或厂商 SDK。
- Then 不申请真实健康、蓝牙或身体传感器权限。
- Then 不持久化心率，不绘制平均心率趋势。
- Then 不做医疗判断、危险告警、训练中断依据或相关文案。

**交付结果:**

- Android `HeartRateState` 已收口为 `kind + sourceKind` source-aware 模型，覆盖 no source、device connected no reading、device reading、manual reading、stale reading、permission unavailable 和 provider unavailable。
- `DisabledHeartRateProvider` / `MockHeartRateProvider` 继续只输出抽象 TrainFlow 状态；mock 可表达设备、手动、过期和不可用状态，但不代表真实设备接入。
- 当前生产执行页、历史页和趋势页不消费心率 UI mapper；心率抽象仅作为未来设备接入边界保留。
- 本阶段未接 Health Connect、Wear OS、BLE、HealthKit、Huawei Health Kit / Health Service Kit 或厂商 SDK，未申请健康 / 蓝牙或身体传感器权限，未实现手动输入 UI，未持久化心率，未绘制平均心率趋势，未改变训练引擎、`WorkoutCommand` 或 `WorkoutEvent` 语义。

### Story E11.2a: HUAWEI Band 9 on non-Huawei Android feasibility smoke

**状态:** Completed for original condition; E16 broadcast-on retest captured positive BLE HRS evidence; not production device integration

作为开发者，
我想先用当前真实设备条件验证 HUAWEI Band 9 在非华为 Android 上的第三方可用心率通道，
以便决定后续是 BLE HRS adapter spike、Huawei SDK feasibility、历史同步，还是暂不接设备。

**当前真实设备条件:**

- 用户当前有 HUAWEI Band 9。
- 手机不是华为手机。
- 手机已安装华为运动健康。
- 华为运动健康可以读取手环数据。
- 这只证明华为运动健康能读设备数据，不证明 TrainFlow 第三方 App 可以实时读取心率。
- 2026-07-05 用户补充：此前未开启 Band 9 心率广播；设备提示开启心率广播会连接第三方蓝牙设备并断开华为运动健康。因此 E11.2a negative result 只覆盖原条件，广播开启条件拆到 E16 retest。

**验收标准:**

- Then 下一步不直接做生产设备接入，只做 feasibility smoke。
- Then 验证 Band 9 是否暴露标准 BLE Heart Rate Service `0x180D`。
- Then 验证 Heart Rate Measurement characteristic `0x2A37` 是否可 notify。
- Then 如果 BLE HRS 可用，后续优先拆 Android BLE HRS adapter spike。
- Then 如果 BLE HRS 不可用，再验证 Huawei Health Kit / Health Service Kit 是否能在非华为 Android + Band 9 + HMS Core 条件下授权读取实时心率。
- Then 如果只能通过华为运动健康查看或同步历史数据，则不作为执行页实时心率来源。
- Then E11.2a 不持久化心率，不绘制平均心率趋势，不把 UI `HeartRateState` 当历史事实。
- Then 设备数据必须经 `HeartRateProvider` adapter 输出统一 `HeartRateState`，并标注 `sourceKind`、`sourceId` / `sourceLabel`。
- Then 不做医疗判断、危险告警、训练中断依据或康复结论。
- Then 手动输入不倒灌到 E11.2；E11.3 后续基于 UI 变形反馈撤销首版手动心率输入。

**路线判定:**

- BLE Heart Rate Service：标准 BLE HRS 仍是 Android-first 最通用的实时路线。E11.2a 原条件未发现可用 HRS；E16 广播开启 retest 已形成 Band 9 正向 BLE HRS 证据，E16-1 已完成 debug adapter spike，E16-2 已完成 production-capable provider / permission / lifecycle 地基。E16-3 顶部 pill 初版已被后续讨论取代，E16-3a 已完成 App 内可拖动浮动心率胶囊 HTML 修订，E16-4 已完成 opt-in / settings / permission rationale planning。Android 12+ 权限实现、设备记忆稳定性、真机 clean stop、stale policy、1 秒采样记录模型和未来 Android UI 仍是后续真实接入风险。
- Huawei Health Kit / Health Service Kit：官方生态存在，但实时心率、地区、账号、设备支持、权限申请、非华为手机兼容性都要验证；Band 9 当前只是 feasibility 样本，不直接承诺生产接入。
- Health Connect：更适合未来历史摘要 / 趋势候选，不作为当前实时执行页来源；当前 MVP 不规划平均心率趋势。
- Apple Watch / HealthKit：仍保留为未来 iOS 第一优先路线；合理路线是 iOS app + watchOS companion，使用 HealthKit / HKWorkoutSession / HKLiveWorkoutBuilder。当前 Android-first 阶段不进入 dev，且 Apple SDK model 不得泄漏到 TrainFlow UI / history / analytics。
- 所有真实设备、Health Connect、Wear OS、HealthKit、Huawei、BLE 或厂商 SDK 接入都必须另开 story 或独立阶段，并继续统一输出 TrainFlow `HeartRateState`。

**交付结果:**

- 已新增 `docs/testing/e11-2a-huawei-band9-feasibility-smoke.md` 记录当前 HUAWEI Band 9 + 非华为 Android + 华为运动健康可读数据条件下的 feasibility smoke。
- 本轮 Codex 环境没有可用 ADB / GATT scan 入口，因此通过 debug-only APK 在用户手机侧 smoke；用户 2026-06-21 反馈新版 smoke APK 仍无法发现华为设备，在广播未开启 / Huawei Health 连接占用的 E11.2a 原条件下没有 Band 9 暴露标准 BLE Heart Rate Service `0x180D` 或 Heart Rate Measurement `0x2A37 notify` 的物理证据。
- 官方资料只证明 Band 9 有心率传感器、支持 BLE、Huawei Health 可读取 / 展示心率；这不等价于 TrainFlow 第三方 App 可实时读取心率。
- E11.2a 原条件路线建议是暂不接设备：用户 2026-06-21 反馈新版 smoke APK 仍无法发现华为设备，当时没有 `0x180D` / `0x2A37 notify` 或 bpm notify 证据，不从 E11.2a 进入 BLE adapter spike。E16 广播开启 retest 后已有正向 BLE HRS 证据，E16-1 / E16-2 已分别关闭 debug adapter 和 provider hardening 地基；MVP 仍不显示心率或未获取心率占位。Health Connect 只作为未来独立历史摘要 / 趋势候选。
- 本阶段最终不保留生产训练页心率 UI、debug BLE smoke launcher、Gradle、main Android Manifest、资源、Room schema 或 `HeartRateProvider` 生产接入；未持久化心率，未绘制平均心率趋势，未实现或保留手动输入。

### Story E16-HR: Heart-rate broadcast feasibility retest

**状态:** Positive BLE HRS evidence reviewed / merged to main (`bbd4296`); no production integration in this story

作为开发者，
我想在 Band 9 明确开启心率广播、且华为运动健康可能断开的条件下重新做 BLE HRS smoke，
以便判断旧 E11.2a negative result 是否只是广播未开启导致。

**验收标准:**

- Then 只提供 debug-only 测试入口，不改生产 `app/src/main` manifest 或 release 权限。
- Then 扫描 BLE 广播和系统 bonded devices，允许用户选择疑似 Band 9 / HUAWEI / Heart Rate 设备连接。
- Then GATT discover 后枚举 service / characteristic，并重点记录 `0x180D`、`0x2A37`、notify / indicate 和 CCCD 写入结果。
- Then 收到 bpm notify 时只在 smoke log 中显示，不持久化、不写 `WorkoutSession`、不调用生产 `HeartRateProvider`。
- Then 记录 Huawei Health 是否断开、广播关闭后是否能恢复连接。
- Then 不恢复 heart-rate UI、手动输入、平均心率趋势，不新增生产设备接入、医学判断、危险告警或训练中断判断。

**交付结果:**

- 新增 `docs/testing/e16-heart-rate-broadcast-feasibility-retest.md`，并随 main merge commit `bbd4296` 合入。
- 新增 debug-only `HR Broadcast Smoke` Activity、独立 debug launcher 和 debug manifest 蓝牙权限。
- 不在 `app/src/main` 暴露 smoke route、首页按钮、callback、Activity 引用或 debug 文案；如设备 launcher 不展示独立 debug launcher，可用 `adb shell am start -n com.liujyks.trainflow/.app.HeartRateBroadcastSmokeActivity` 启动。
- 2026-07-05 18:32 用户截图显示 Band 9 出现在 bonded devices，但 bpm notify 发生在首次可见连接 Band 9 之前，不能归因于 Band 9；后续 Band 9 连接 `GATT connection status=147 state=0` 并断开。
- Debug smoke 后续已追加 heart-rate notify source label，并说明 `Clear` 不会断开 active GATT，避免旧连接 notify 污染下一轮判断。
- 2026-07-05 18:46 用户截图形成正向证据链：扫描到 `HUAWEI Band HR-OD7 D8:F0:42:01:90:D7 services=[0x180D]`，连接同一地址成功，GATT 发现 `0x180D` / `0x2A37 props=notify`，CCCD `0x2902` 写入成功并连续收到 bpm notify。
- E16 结论已先后进入 E16-1 debug adapter spike 和 E16-2 production provider hardening；这仍不恢复当前 MVP 心率 UI，不新增 production manifest 权限或训练页设备接入。未来真正展示心率前必须先做 HTML 视觉方案 / 高保真案例评审，再进入单独 Android UI 实现。
- E11.2a 文档、decision log 和项目状态已标注旧 negative result 的条件边界。

### Story E16-1: BLE HRS adapter spike

**状态:** Implemented; real-device smoke passed

作为开发者，
我想把 E16 的标准 BLE HRS 正向路径封装为最小 debug-only adapter spike，
以便后续能在真实 Android 手机 + Band 9 心率广播条件下验证扫描、连接、discover、notify 和 bpm 状态流。

**验收标准:**

- Then 提供标准 Heart Rate Measurement payload parser，覆盖 8-bit bpm、16-bit bpm、flags 和 malformed / empty payload。
- Then parser 是无 Android SDK 依赖的纯 Kotlin utility，并有 focused unit tests。
- Then debug-only adapter harness 可输出 scanning、device found、connecting、service discovered、notify enabled、bpm received、disconnected / stopped 状态。
- Then adapter 输出 TrainFlow `HeartRateState`，不得把 Android BLE SDK model 泄漏到训练 engine、UI、records 或 history。
- Then 生产默认心率路径仍不启用真实 BLE provider。
- Then 不修改 production manifest 蓝牙权限，不恢复训练执行页心率 UI，不写 session record，不做心率统计。
- Then 真机 BLE HRS 结论仍只能来自真实 Android 手机 + HUAWEI Band 9 心率广播，不能用 AVD 证明。

**交付结果:**

- 新增 `docs/testing/e16-1-ble-hrs-adapter-spike.md`。
- 新增纯 Kotlin `HeartRateMeasurementParser` 和 focused tests，覆盖 8-bit、16-bit、flags、empty / malformed payload。
- 新增 debug-only `BleHeartRateProvider`，封装 BLE scan、bonded listing、GATT connect、service discovery、`0x2A37` notify / CCCD 和 bpm mapping。
- `HR Broadcast Smoke` 继续作为 debug launcher，但 Activity 只负责权限、按钮和日志展示；BLE lifecycle 在 debug provider 内。
- 生产 source set 只新增 parser，不新增 production BLE provider、production manifest 权限、训练 UI、Room schema、records / trends 心率字段、`WorkoutCommand`、`WorkoutEvent` 或训练引擎语义。
- `app:testDebugUnitTest --tests "*HeartRate*"` 和 `app:assembleDebug` 已通过；adb server 当前 protocol fault，未完成 emulator launch smoke。AVD 只可验证 debug Activity 基本路径，不可作为 BLE 外设可行性证据。
- 用户已在真实 Android 手机 + HUAWEI Band 9 heart-rate broadcast mode 下完成真机 smoke，截图时间约 2026-07-06 01:14。证据链记录 `Connecting HUAWEI Band HR-OD7 D8:F0:42:01:90:D7`、`GATT connection status=0 state=2`、`Services discovered status=0 count=9`、`service 0x180D`、`characteristic 0x2A37 props=notify`、`RESULT: HRS 0x180D found`、`RESULT: characteristic 0x2A37 found props=notify`、`setCharacteristicNotification=true`、`write CCCD result=0`、`Descriptor write 0x2902 status=0 for 0x2A37`、`RESULT: 0x2A37 notify enabled`、`RESULT: heart-rate notify bpm=100 flags=0x6 format=uint8 ... bytes=06 64`、`RESULT: heart-rate notify bpm=99 flags=0x6 format=uint8 ... bytes=06 63`、`Closing GATT` 和 `adapter stopped`。
- 真机结论：`passed: Band 9 broadcast -> BLE HRS adapter -> HeartRateState bpm flow`。这只关闭 E16-1 debug adapter smoke，不恢复生产心率 UI，不写 session record，不接 records / history / trends，也不改变 MVP 不显示、不录入、不统计心率的边界。

### Story E16-2: Production BLE HRS provider hardening

**状态:** Implemented; real-device smoke passed

作为开发者，
我想把 E16-1 已验证的 BLE HRS 路径收敛成 production-capable provider / permission / device selection / lifecycle 地基，
以便后续若真正进入心率能力时，有清晰的 opt-in 和平台边界，而不是从 debug spike 直接接 UI。

**验收标准:**

- Then `core.health` 有 production-capable BLE HRS provider / state / permission / lifecycle 边界。
- Then 默认生产训练页不消费该 provider，不显示、不录入、不统计心率。
- Then 权限不在 app 启动时请求；只能由显式用户动作触发。
- Then BLE SDK model 不泄漏到 engine、records、session 或 UI state 主模型。
- Then 可保存一个用户选择的 BLE device identifier / display name preference，但不写 Room，不保存 SDK model。
- Then provider 不做无限后台扫描，stop / disconnect / close 和失败路径能清理 GATT。
- Then parser tests 仍通过，provider state / permission / preference boundary 有 focused tests。

**交付结果:**

- 新增 `docs/testing/e16-2-production-ble-hrs-provider-hardening.md`。
- 新增 production source set `AndroidBleHeartRateProvider`、`BleHeartRateProviderState`、`BleHeartRatePermissionPlanner`。
- Provider 状态可表达 no source、permission required、bluetooth disabled、scanning、device found / selected、connecting、connected waiting for data、live bpm、stale / disconnected、stopped 和 recoverable error，并可映射到 `HeartRateState`。
- Debug `HR Broadcast Smoke` 改为薄 harness，显式按钮触发 permission / scan / connect / stop；旧 debug-only provider 删除，避免两套 lifecycle 漂移。
- DataStore 新增 nullable `bleHeartRateDeviceIdentifier` / `bleHeartRateDeviceDisplayName`，只保存选择偏好，不保存 `BluetoothDevice`、`BluetoothGatt`、SDK model、bpm 样本或 session summary；文档说明 BLE address / Android privacy / Band broadcast label 不能当稳定医疗设备身份。
- 生产 `app/src/main/AndroidManifest.xml` 未新增 BLE 权限；debug manifest 继续服务手动 smoke。未来若移入 production manifest，必须先有显式 opt-in、权限说明、隐私 / 非医疗文案和 UI gate。
- `app:testDebugUnitTest --tests "*HeartRate*"` 已通过；用户 2026-07-07 真机截图已覆盖 E16-2 Band 9 smoke：debug APK 入口为 `TrainFlow Debug` -> `DebugEntryActivity`，包含 `进入 TrainFlow` / `HR Broadcast Smoke` 两个明确按钮；`HR Broadcast Smoke` 可见且未污染 TrainFlow 首页；扫描发现 `HUAWEI Band HR-OD7 D8:F0:42:01:90:D7 services=[0x180D]`；覆盖 `bluetooth_disabled` recoverable state，以及 `scanning`、`device_found`、`device_selected`、`connecting`、`connected_waiting_for_data`；已 enable Heart Rate Measurement notify；持续收到 live bpm `84`、`85`、`86`、`87`、`88`、`89`、`90`、`91`；Stop 后出现 `stopped: BLE HRS provider stopped`。用户已检查 TrainFlow 训练页没有心率 UI。
- 本 story 不恢复训练页心率 UI、心率卡片、`未获取心率` 占位、手动心率输入或平均心率趋势，不改 Room、`WorkoutSession`、records/history/trends、`WorkoutCommand`、`WorkoutEvent`、TimedWorkoutEngine、StrengthWorkoutEngine、TimerDial 或声音。

### Story E16-3: Heart Rate UI HTML Visual Planning

**状态:** Initial HTML visual planning complete; top-pill recommendation superseded by floating-capsule decision

作为产品和 UI 设计执行，
我想先用 HTML 高保真方案验证心率在训练执行页中的展示位置、状态表达和层级，
以便后续若进入 Android UI 时不破坏 E15 已收口的力量训练和 TimerDial 体验。

**验收标准:**

- Then 只产出 HTML / CSS / JS 视觉方案和文档，不改 Android Kotlin、production manifest、Room、session record、records/history/trends、`WorkoutCommand`、`WorkoutEvent`、训练引擎、TimerDial 或声音。
- Then 至少提供 2-3 个基于 TrainFlow 现有 UI 的 placement 变体。
- Then 覆盖 timed training 和 strength training，特别是 strength active / rest / confirm-record。
- Then 覆盖未启用、权限未授予、蓝牙关闭、正在连接、已连接等待数据、live bpm、stale / disconnected 和默认无入口纯净状态。
- Then 心率视觉层级低于当前动作、主时间、组目标、重量 / 次数、下一步和固定底部主操作。
- Then 不恢复大心率卡片、`未获取心率` 占位、手动心率输入或平均心率趋势。

**交付结果:**

- 新增 `docs/design/e16-3-heart-rate-ui-html/index.html`。
- 新增 `docs/design/e16-3-heart-rate-ui-html/README.md`。
- HTML 原型提供 Variant A 顶部状态 pill、Variant B 当前卡片角标、Variant C 底部微状态三个旧探索方向。
- 经 2026-07-07 讨论，Variant A 顶部 pill 与 `进行中` session 状态、长标题、TimerDial、strength confirm-record 和固定底部主按钮存在空间竞争风险，不再作为推荐实现。
- 新增 `docs/design/e16-heart-rate-floating-capsule-decision.md` 作为当前决策来源：后续方向改为 App 内可拖动浮动心率胶囊，不申请系统级悬浮窗权限；偏好开启后 app 内显示，未训练只显示不记录，训练中 1 秒采样记录 timed / strength 全过程；无 bpm 时显示连接 / 数据状态，有 bpm 且用户已设置年龄时显示“区间 + bpm”并按区间着色。
- 当前 E16-3 不做 Android 实现；E16-3a 已完成 floating capsule HTML 视觉修订，E16-4 已完成 opt-in / settings / permission rationale planning。
- 后续若进入 Android 实现，仍必须从 settings / opt-in UI 开始，再拆 permission request flow、source selection、stale policy、recording model、capsule implementation 和 analysis story。

### Story E16-4: Heart-rate opt-in / settings / permission rationale planning

**状态:** Docs-only planning complete

作为产品和 Android 后续实现规划，
我想先定义心率功能的显式开启、设置入口、权限说明、设备选择、隐私和非医疗边界，
以便后续 Android 实现 story 不从 provider 或视觉方案直接跳到训练页 UI。

**验收标准:**

- Then 心率功能默认关闭，必须由用户显式 opt-in。
- Then canonical 入口是 `设置 -> 训练偏好 -> 心率`，设备状态和胶囊展开态只作为已启用后的捷径。
- Then 开启前必须说明用途、BLE scan / connect 权限、Android 11 及以下 scan compatibility location、隐私和非医疗边界。
- Then 不使用系统 overlay / “显示在其他应用上层”权限。
- Then BLE 权限只在用户主动开启 / 选择设备 / 重新扫描后请求，不在 app 启动、进入训练页或开始训练时请求。
- Then 选择设备只保存 provider identifier / display name，不保存 GATT / SDK model / bpm 样本 / session summary。
- Then 未训练时只显示连接状态或 live bpm，不记录；训练中 1 秒采样记录模型另拆。
- Then 权限拒绝、蓝牙关闭、设备离线、等待数据、数据过期和 scan timeout 文案边界明确。
- Then `超过上限` 只做视觉提示，不诊断疾病、不替代医生建议、不自动中断训练。
- Then 后续 Android 实现拆分建议明确。

**交付结果:**

- 新增 `docs/testing/e16-4-heart-rate-opt-in-settings-planning.md`。
- 明确心率入口以训练偏好页为 canonical setup；首页 / 设备状态可后置，胶囊展开态只能作为已启用后的设置捷径。
- 明确 opt-in flow：默认关闭 -> 用户点击开启 -> 查看 rationale -> 点击选择设备 -> 权限请求 / bounded scan -> 选择设备 -> 胶囊可显示。
- 明确权限说明：BLE scan / connect 只用于发现和连接用户主动选择的心率设备；Android 11 及以下 location 仅为蓝牙扫描兼容，不用于定位；不使用系统悬浮窗权限；不后台无限扫描；不无提示扫描。
- 明确隐私说明：心率来自用户主动选择的设备；未训练时只显示不记录；训练中未来可按 1 秒采样记录，但 recording model / Room / summary / history / trends 另拆。
- 明确非医疗说明：区间只做训练参考，`超过上限` 只做深红视觉提示，不播放声音、不震动、不强制暂停、不做疾病诊断或医生建议替代。
- 明确关闭行为：胶囊消失，停止扫描，断开连接，不重连，不记录；已保存设备 identifier / display name 可作为 convenience hint 保留，并提供清除入口。
- 明确后续拆分：E16-5 settings / opt-in UI、E16-6 permission request flow、E16-7 device picker / source status、E16-8 app-shell floating capsule、E16-9 `HeartRateState` -> capsule mapping、E16-10 stale / offline policy、E16-11 recording model / 1s sampling persistence、E16-12a recap HTML visual gate、E16-12 analysis / zones / post-workout summary。

E16-12 已记录外部划船训练详情 / 图表参考：只借鉴训练后单次记录详情的摘要、曲线和区间时长层级，不复制截图资产或划船专属距离、配速、桨频、功率、卡路里、恢复时长和训练压力指标。实际页面必须等待 E16-11 已保存来源明确的样本，再经 E16-12a `huashu-design` HTML 视觉评审后实现，详见 `docs/testing/e16-12-heart-rate-recap-visual-reference.md`。
- 本 story 未改 Android Kotlin、production manifest、Gradle、Room、session record、records/history/trends、`WorkoutCommand`、`WorkoutEvent`、TimedWorkoutEngine、StrengthWorkoutEngine、TimerDial、声音、震动或通知；未恢复旧心率卡片、`-- bpm`、`未获取心率`、手动心率输入或旧平均心率趋势。

### Story E16-6: Heart-rate BLE permission request flow

**状态:** Implemented

作为已开启心率显示的用户，
我想在准备连接设备前先看到蓝牙权限用途说明，并由我主动触发系统授权，
以便 TrainFlow 只在明确同意后获得后续设备选择所需权限。

**验收标准:**

- Then 默认关闭状态不请求权限。
- Then 开启 `心率显示` switch 后仍不自动请求权限。
- Then 用户主动点击 `准备连接设备` 后先展示 App 内中文 rationale。
- Then rationale 覆盖查找并连接蓝牙心率设备、不使用系统悬浮窗、不后台无限扫描、不无提示扫描、无训练只显示不记录、训练记录采样另拆、非医疗边界。
- Then Android 12+ 使用 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` runtime permission。
- Then Android 11 及以下仅按 planner 使用 `ACCESS_FINE_LOCATION maxSdkVersion=30` 作为蓝牙扫描兼容 fallback，不写成定位能力。
- Then granted 后显示 `蓝牙权限已允许 / 可选择设备` 或等同文案。
- Then denied 后显示 `权限未赋予` 并说明可稍后重试。
- Then permanently denied / 不再询问可检测时提示去系统设置开启。
- Then 本 story 不扫描、不连接、不展示设备列表、不写训练记录、不恢复旧心率 UI。

**交付结果:**

- 新增 `docs/testing/e16-6-heart-rate-permission-request-flow.md`。
- 生产 manifest 新增 `BLUETOOTH_SCAN`（`neverForLocation`）、`BLUETOOTH_CONNECT` 和 `ACCESS_FINE_LOCATION maxSdkVersion=30`。
- 设置页 `心率与设备` 卡片新增两段式权限入口：`准备连接设备` 显示 rationale，`授权蓝牙权限` 才调用 Activity Result permission launcher。
- UI state 覆盖未请求、rationale visible、granted、denied、permanently denied 状态。
- Focused tests 覆盖默认关闭不请求、开启后未授权、点击前只显示 rationale、granted / denied 文案、关闭后不请求，以及 manifest 只允许本 story 的 scoped BLE permissions。
- 本 story 未做 BLE scan、device picker、GATT connect、训练页浮动胶囊、session record、Room / records / history / trends、`WorkoutCommand` / `WorkoutEvent`、训练引擎、TimerDial、声音、震动、通知或 cue。

### Story E11.3: 放弃首版心率显示、录入和统计

**状态:** Implemented as removal / rollback; no first-version heart-rate display, manual input, or heart-rate statistics

作为用户，
我不希望不可用的心率能力挤压训练执行页，
以便计时、力量和跟练页面保持稳定、清晰，并且历史统计不出现无意义的心率占位。

**验收标准:**

- Then 计时、力量和基础跟练执行页不显示心率卡片。
- Then 不提供手动心率输入。
- Then 记录页 / 趋势页不显示未获取心率占位，不绘制平均心率趋势。
- Then 不新增心率持久化字段，不写入 `WorkoutSession`，不改 Room schema。
- Then 不做医疗判断、危险告警或训练中断依据。

**交付结果:**

- 从计时、力量和基础跟练执行页撤下生产心率卡片和 E11.3 手动 bpm 输入，修复因心率输入造成的执行页 UI 变形。
- 从历史聚合趋势撤下未获取心率占位和 `averageHeartRateTrend` 占位字段；历史页只保留真实非心率统计和趋势。
- 删除 debug-only Band 9 HRS smoke launcher / BLE 权限入口，避免当前 APK 继续暴露已降级的心率调研入口。
- 底层 `HeartRateState` / provider 边界暂时保留为未来架构扩展点，但当前 UI、记录和统计不消费它。
- 本阶段未接真实设备、BLE、Huawei SDK、Health Connect、HealthKit、Wear OS 或任何健康权限，也未改变训练引擎、`WorkoutCommand`、`WorkoutEvent`、通知、声音或恢复建议语义。

## Epic E12: 真实记录、统计图表与趋势分析

目标：基于真实 `WorkoutSession` 提供可信统计、图表、趋势和记录清理能力。

### Story E12.1: 真实记录与基础统计

**状态:** Implemented in Android history real record stats

作为用户，
我想看到真实训练记录和总统计，
以便知道自己完成了多少训练。

**验收标准:**

- Then 记录页读取真实持久化数据。
- Then 展示训练次数、总时长、有效训练时间等总统计。
- Then 统计口径区分 fixture、内存态和真实记录。
- Then 数字、时间、次数、轮次、总时长、有效时长和暂停时长有回归验证。
- Then 为同日多轮运动保留可分组分析口径，避免把不同轮次、不同计划结构或不可比阶段混在一起。

**交付结果:**

- 记录页继续消费本地 Room `WorkoutSession` 真实记录，非空真实记录时展示“真实记录基础统计”；preview / fixture 示例记录不进入生产统计，空记录页不显示假统计。
- 新增 `WorkoutRecordStats` / `WorkoutRecordStatsUiState`，从真实 session list 推导训练总次数、completed / abandoned 分开计数、`totalElapsedSec`、`effectiveElapsedSec`、`pausedElapsedSec`、计划休息、实际休息、计时额外休息和计时 / 力量 / 跟练 mode breakdown。
- 统计口径明确区分 planned rest、actual rest、extra rest、paused elapsed、total elapsed 和 effective elapsed；计时 extra rest 仅来自 `timedRestExtensionRecords.addedSec`，不并入 `pausedElapsedSec`。
- 单次记录详情补充总用时、有效训练时间、暂停时间、计划休息、实际休息和额外休息；计划休息继续按历史 `WorkoutSession.planSnapshot` 计算，计划编辑不会回写历史统计。
- 本阶段不实现 E12.2 图表趋势、同类趋势比较、真实心率设备、Health Connect、Wear OS、BLE、声音播放、云同步、账号体系、历史记录清理、foreground service、exact alarm、notification action、reset production command 或第四套 skin；平均心率趋势已随 E11.3 撤销。

### Story E12.2: 图表与趋势分析

作为用户，
我想查看计划趋势和力量/计时表现变化，
以便做后续训练调整。

**验收标准:**

- Then 提供总统计图表、计划趋势和非心率表现趋势。
- Then 分析时比较同类数据：同一计划、同一阶段、同一轮次或同一动作。
- Then 不把某天第一轮和另一天最后一轮直接比较。
- Then E12 当时的非心率图表范围不显示、不录入、不统计心率；E12 不提供未获取心率占位或平均心率趋势。该历史验收不覆盖 D-080 的当前 E17 路线。
- Then 不用不可比数据得出强弱、康复或医疗结论。

### Story E12.2a: Non-heart-rate history charts and aggregate trends

**状态:** Implemented in Android history aggregate charts

作为用户，
我想先看到不依赖心率来源的真实记录图表，
以便了解训练次数、状态、用时、休息和训练类型分布的基础变化。

**验收标准:**

- Then 图表只消费真实持久化 `WorkoutSession` list，不使用 preview / fixture / 内存示例记录。
- Then 按 `startedAt` 日期聚合训练总次数趋势。
- Then completed / abandoned 趋势分开显示。
- Then `totalElapsedSec` / `effectiveElapsedSec` / `pausedElapsedSec` 趋势分开显示。
- Then planned rest / actual rest / extra rest 趋势分开显示，extra rest 不并入 paused。
- Then mode breakdown 展示 timed / strength / follow_along 数量和占比。
- Then 空记录或不足 2 个日期点时显示暂无趋势，不绘制假曲线。
- Then planned rest 继续来自历史 `WorkoutSession.planSnapshot`，当前计划编辑不回写旧趋势。
- Then 不输出心率占位或平均心率趋势数据。

**交付结果:**

- Android 记录页新增“非心率图表与聚合趋势”区，基于真实 Room session records 的 UI state mapper 推导图表数据。
- 新增轻量 Compose 折线图和训练类型分布条，保持记录页浅色、克制、可读；样本不足时只显示空状态文案。
- 单元测试覆盖真实 session list 聚合、completed / abandoned 分离、total / effective / paused 分离、planned / actual / extra rest 分离、mode breakdown、空记录 / 不足时间点、历史 planSnapshot 使用，以及不输出心率占位或平均心率趋势数据。
- 本阶段不实现设备心率获取、手动心率输入、持久化心率模型、E12.2b 力量同类 set 趋势、E12.2c 计时同类阶段 / 轮次深趋势、E12.3 历史记录清理、声音播放、云同步、账号体系、foreground service、exact alarm、notification action 或 reset production command。

### Story E12.2b: Strength comparable set trends

**状态:** Planned / review boundary clarification

作为力量训练用户，
我想在历史 / 趋势页看到同一力量动作的可比 set 计划值与实际值变化，
以便回顾同一动作、同一组类型和计划组位置下的实际完成情况。

**验收标准:**

- Then 只消费真实持久化 `WorkoutSession` list，不使用 preview / fixture / 内存示例记录。
- Then 只分析 `WorkoutMode.STRENGTH` 记录中的 `StrengthSetRecord`，不生成计时趋势或跟练趋势。
- Then 同类比较必须限定同一 `StrengthSetRecord.exerciseId`，不得跨不同动作比较。
- Then planned values 来自每条历史 `WorkoutSession.planSnapshot`，不得用编辑后的当前计划反推旧 session。
- Then planned lookup 优先使用 `sourceSetPlanId`，并且 lookup 必须限定在对应 `exerciseId` 的 `StrengthExerciseBlock` 内。
- Then 只有 `sourceSetPlanId == null` 时，才允许在同一 `exerciseId` block 内 fallback 到 `setOrder + setKind`。
- Then `sourceSetPlanId != null` 但找不到同一动作 matching set 时，显示数据不足或降级说明，不得 fallback。
- Then 替换动作的 planned values 只能来自 `substitutedFromExerciseId` 对应原动作 block；非替换动作的 planned values 只能来自 record 的 `exerciseId` 对应 block。
- Then 不拼接原动作和替换后动作候选，不把替换动作自动并入原动作趋势；替换记录必须标注替换来源。
- Then 趋势可以展示 planned / actual weight、planned / actual reps、set kind、set order、active duration、actual rest 和 date。
- Then 样本不足、缺少 planSnapshot block、缺少 matching set 或替换来源不完整时显示暂无趋势 / 数据不足或降级说明，不绘制假趋势。
- Then 不输出训练强弱、恢复不足、康复、医疗、平均心率、自动调整计划或加重量建议。

**实现边界:**

- mapper 应在历史 `WorkoutSession.planSnapshot` 内重建 strength block descriptor，并以 `exerciseId`、planned set identity 和替换来源标注形成可比 key。
- `sourceSetPlanId` lookup 不得离开当前 record 的动作边界；替换记录的 planned lookup 使用原动作边界，实际趋势行仍标注替换后的 `exerciseId`。
- `sourceSetPlanId` 存在但匹配失败是数据不足，不是 fallback 触发条件。
- E12.2b 不修改训练执行引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、`WorkoutSession.planSnapshot`、历史删除语义、心率数据源或 E12.2c 计时趋势语义。

### Story E12.3: 历史记录清理

**状态:** Implemented in Android real persisted session cleanup

作为用户，
我想清理历史记录，
以便管理本地训练数据。

**验收标准:**

- Then 支持全部清除。
- Then 支持按训练计划清除。
- Then 支持按日期清除。
- Then 删除前有明确确认。
- Then 仅对真实持久化记录执行真实删除，不做假删除。

**交付结果:**

- `core.data.WorkoutSessionRepository` 和 Room `WorkoutSessionDao` 新增真实删除能力，支持全部清除、按 `WorkoutSession.planId` 清除，以及按 `startedAt` 展示日期清除。
- 删除以事务清理 `workout_sessions` 及其 `session_step_records`、`timed_rest_extension_records`、`strength_set_records` 子记录；不删除 `WorkoutPlan`、`Exercise`、fixture / preview 数据，也不改写任何历史 `planSnapshot`。
- 记录页新增“历史记录清理”区，按全部 / 训练计划 / startedAt 日期生成清理入口；每一种删除都必须先进入明确确认对话框，确认后才调用 repository 删除。
- 删除后记录页继续消费 Room Flow 的真实剩余 records，列表、E12.1 基础统计、E12.2a 聚合趋势、mode breakdown 和空状态随真实数据自动刷新，不在 Compose 层做假过滤。
- 新增 / 更新测试覆盖全部删除、按计划删除、按日期删除、删除后不影响 WorkoutPlan、子记录清理、剩余记录统计 / 图表重算、未确认不删除 / 确认后才发出清理目标，以及不触碰心率趋势或计划快照语义。
- 本阶段不实现 undo / recycle bin / 版本历史、云同步、账号体系、远端删除、E12.2b 力量同类趋势、E12.2c 计时阶段 / 额外休息深趋势、心率数据源、声音播放、foreground service、exact alarm、notification action 或 reset production command。

### Story E12.2c: Timed comparable stage and extra rest trends

**状态:** Implemented in Android timed comparable rest trends

作为计时训练用户，
我想在历史 / 趋势页看到同类计时休息阶段中的计划休息、实际休息和额外休息变化，
以便回顾哪些轮次、阶段位置或前序阶段后更常发生额外休息。

**验收标准:**

- Then 只消费真实持久化 `WorkoutSession` list，不使用 preview / fixture / 内存示例记录。
- Then 只分析 `WorkoutMode.TIMED` 记录，不生成力量同类 set 趋势或跟练趋势。
- Then planned rest 来自历史 `WorkoutSession.planSnapshot`，actual rest 来自真实 `SessionStepRecord`，extra rest 只来自 `timedRestExtensionRecords.addedSec`。
- Then 只比较同一历史计划结构、同一 REST 阶段类型、同一阶段顺序、同一轮次、同一 step index、同一 restStageId 和同一 previousStageId 关系。
- Then 不把某天第一轮和另一天最后一轮直接比较，不跨不同计划结构硬比，不混合 warmup / work / rest / cooldown / custom 阶段语义。
- Then 可展示 extra rest 发生位置中的 roundIndex、restStageId、previousStageId 和 stepIndex。
- Then 样本不足、缺少 step record 或位置字段不完整时显示暂无趋势 / 数据不足或降级说明，不绘制假趋势。
- Then 不输出训练强弱、恢复不足、康复、医疗、平均心率或自动调整计划建议。

**交付结果:**

- `feature.history.HistoryScreenState` 新增 timed comparable rest trend UI state，继续在 mapper 层从真实 session list 推导，Compose 不做复杂统计。
- mapper 基于历史 `WorkoutSession.planSnapshot` 重建计时休息阶段 descriptor，并用结构签名、REST 阶段类型、阶段顺序、round index、step index、restStageId 和 previousStageId 形成同类比较 key。
- 趋势行展示每条可比记录的 date、plan snapshot title、planned rest、actual rest、extra rest 和位置信息；同类样本不足 2 条时只显示暂无趋势。
- 字段不完整的 `timedRestExtensionRecords` 不进入阶段级 extra rest 计算，并在 UI state 中显示降级说明；extra rest 不并入 `pausedElapsedSec`。
- 新增 / 更新单元测试覆盖只消费真实 timed sessions、排除 strength / follow_along、同结构同阶段同轮次比较、不同计划结构不混比、不同轮次不混比、planned / actual / extra rest 分离、extra rest 只来自 `addedSec`、位置字段缺失降级、缺少 step records 不造样本，以及不输出心率趋势、力量趋势或训练结论。
- 本阶段不实现 E12.2b 力量同类趋势、平均心率趋势、设备心率获取、手动心率输入、Health Connect、Wear OS、BLE、声音播放、云同步、账号体系、foreground service、exact alarm、notification action 或 reset production command；不修改训练执行引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、`WorkoutSession.planSnapshot` 或历史删除语义。

### Story E12.2b: Strength comparable set trends

**状态:** Implemented in Android strength comparable set history trends

作为力量训练用户，
我想在历史 / 趋势页看到同一动作、同一计划组来源或同类组序下的 planned / actual set 记录变化，
以便回顾不同训练日同类 set 的重量、次数、组耗时、实际休息和主观感受。

**验收标准:**

- Then 只消费真实持久化 `WorkoutSession` list，不使用 preview / fixture / 内存示例记录。
- Then 只分析 `WorkoutMode.STRENGTH` 记录，不生成计时趋势或跟练趋势。
- Then 只比较同一 `exerciseId` 下的 set。
- Then 优先使用同一 `sourceSetPlanId` 做同类比较；`sourceSetPlanId` 缺失时才降级到同一 `setOrder + setKind`。
- Then 替换动作不得自动并入原动作趋势；如展示，必须标注 `substitutedFromExerciseId` 来源。
- Then planned weight / reps 与 actual weight / reps 分开展示，planned 值来自真实 set record 或历史 `WorkoutSession.planSnapshot`。
- Then 展示 active duration、actual rest 和 effort；关键字段不足时显示数据不足，不造样本。
- Then 样本不足时显示暂无趋势，不绘制假曲线。
- Then 不输出训练强弱、加重量建议、康复、医疗、平均心率或自动调整计划建议。

**交付结果:**

- `feature.history.HistoryScreenState` 新增 strength comparable set trend UI state，继续在 mapper 层从真实 session list 推导，Compose 只展示已生成的 UI state。
- mapper 对 strength session 生成同类 set 样本 key：有 `sourceSetPlanId` 时使用 `exerciseId + sourceSetPlanId`；缺失时才使用 `exerciseId + setOrder + setKind` fallback。
- planned weight / reps 优先使用 `StrengthSetRecord` 字段，缺失时从历史 `WorkoutSession.planSnapshot` 的 `StrengthSetPlan` 或 block target 推导；actual weight / reps、active duration、actual rest 和 effort 必须来自真实 set record。
- 替换动作记录按替换后的 `exerciseId` 单独成组，趋势行显式显示 `substitutedFromExerciseId`，不把替换动作样本自动并入原动作。
- 字段不完整、key 不完整或同类样本不足时显示暂无趋势 / 数据不足 / 降级说明，不造样本、不画假曲线。
- 新增 / 更新单元测试覆盖只消费真实 strength sessions、排除 timed / follow_along、sourceSetPlanId 优先比较、source 缺失 fallback、不同 exerciseId / setOrder / setKind 不混比、替换动作标注且不并入原动作、planned / actual 分离、active duration / actual rest / effort 展示、字段不足不造样本、删除后剩余记录自然刷新，以及不输出强弱判断、加重量建议、心率趋势或计时趋势变更。
- 本阶段不实现平均心率趋势、设备心率获取、手动心率输入、Health Connect、Wear OS、BLE、声音播放、云同步、账号体系、foreground service、exact alarm、notification action 或 reset production command；不修改训练执行引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、`WorkoutSession.planSnapshot` 或历史删除语义。

### Story E12.4: Records / trends polish planning and visual gate

**状态:** Planning / audit / visual gate complete; E12-1 records data semantics foundation and E12-2 records IA / chart UI polish implemented and verified

作为训练记录用户，
我想在记录页先看到清晰的最近训练、可信统计和可比较趋势，
以便理解每次训练记录和长期变化，而不是被不可比图表或过长卡片流干扰。

**验收标准:**

- Then 本阶段只输出规划、审计和视觉方案，不改 Kotlin / Compose / Room / app resources / tests，不生成 APK。
- Then 审计当前 records / history / trends 源码与测试，明确已有真实数据能力和 UI 问题。
- Then 明确 legacy timed session、timed composition v2 session 和 strength session 的记录解释与趋势比较边界。
- Then 明确 completed / abandoned、skipped、pause、rest extension、planned rest、actual rest 和 extra rest 的语义。
- Then legacy timed 与 v2 timed composition 默认不硬比；没有可证明等价 mapper 时，不把不同结构画成同一趋势。
- Then v2 timed composition 的趋势 key 设计考虑 `compositionVersion`、block / stage group / target id、`targetKind`、round / stage / target instance、planned duration 和 structure signature。
- Then 图表方案包含轴线、单位、legend、空状态和数据不足状态。
- Then 不恢复 heart-rate UI、手动输入、平均心率趋势，不新增设备接入、医学判断、危险告警或训练中断判断。
- Then implementation foundation 先处理 v2 timed record interpretation 和趋势 key 边界，不做完整记录页视觉重排或复杂图表 UI。
- Then legacy timed 与 composition v2 trend key 必须分离；strength comparable trend 不被 timed v2 语义放宽。

**交付结果:**

- 新增 `docs/testing/e12-records-trends-polish-planning-visual-gate.md`，记录输入文档 / skill、源码审计、当前能力、主要缺口、数据语义、信息架构、图表 UI、视觉方向、implementation split 和后置项。
- 新增本地视觉预览 `.local/smoke/e12-records-trends-polish-visual-gate/index.html`；该文件仅用于本机视觉确认，不提交 Git。
- 审计确认当前 `feature.history.HistoryUiState` 已有真实 Room session 基础统计、非心率聚合趋势、legacy timed comparable rest、strength comparable set 和 cleanup，但 v2 timed composition 目前只通过 compatibility tests 保证历史记录可读不崩，尚未按 `TimedCompositionTimelineAdapter` metadata 形成可比趋势分组。
- 推荐后续 implementation 拆为 v2 timed record interpretation、legacy/v2 trend key hardening、records page IA polish、chart axis / empty state polish 和 screenshot-level visual QA。
- E12-1 新增 `docs/testing/e12-1-records-data-semantics-v2-foundation.md`，实现 timed composition v2 record interpretation foundation：history detail rows 可解释 stageGroup / target / boundary rest / rest extension，planned rest 来自 adapter-expanded snapshot，v2 comparable rest key 使用独立 `timed_composition_v2` family。
- E12-1 legacy timed trend key 继续使用 `legacy_timed` family，composition v2 不与 legacy timed 默认合并；混合数据只产生隔离 / 数据不足说明，等待未来明确等价 mapper。
- E12-1 strength comparable set trend 保持既有 exerciseId / sourceSetPlanId / replacement boundary 语义，不被 timed v2 key 影响。
- E12-1 不改 Room schema / migration、training engine、TimerDial、`WorkoutCommand`、`WorkoutEvent`、completion recap summary 语义、heart-rate UI / input / trend 或任何设备接入；未做 visible records page redesign。
- E12-2 新增 `docs/testing/e12-2-records-ia-chart-ui-polish.md`，将记录页可见 IA 重排为概览摘要 -> 筛选区 -> 最近训练 -> 选中详情 -> 趋势区 -> 历史管理，并完成 Android smoke evidence。
- E12-2 图表 UI 补齐 X/Y 轴、单位、tick、`Legend · 最新值`、空状态和数据不足状态；最近列表补状态 tone 与 skipped / pause / extra rest / actual rest flags；详情区分别展示 legacy timed、timed composition v2 和 strength 记录解释。
- E12-2 继续保持 `legacy_timed`、`timed_composition_v2` 与 `strength_comparable_set` 趋势分组隔离，不改 Room schema / migration、training engine、TimerDial、`WorkoutCommand`、`WorkoutEvent`、session record 存储语义、heart-rate UI / input / trend 或任何设备接入。

## Epic E13: 声音提示、固定女声 cue 与音频共存

目标：建立悦耳、克制、不打断其他 App 的训练音频提示。

### Story E13.1: 声音提醒与音频共存

**状态:** Implemented in Android sound cue playback and audio coexistence boundary

作为训练中的用户，
我想听到短促提醒但不影响正在播放的音乐或视频，
以便训练节奏提示不会打断我的其他 App。

**验收标准:**

- Then 最后 N 秒声音提醒按偏好触发。
- Then `countdown_beep1.mp3` 用于 5 / 4 / 3 / 2 / 1 等最后 N 秒 beep；具体触发秒数由 `CountdownCue.thresholdSec`、阶段时长裁剪和偏好控制。
- Then `.local/audio/stage_bell_copper_clean.mp3` 可作为倒数到 0 后下阶段开始铃声候选；接入 App 时由执行 story 复制到 `app/src/main/res/raw/`，本地 `.local` 原文件不得提交。
- Then 声音提醒不降低、暂停或打断其他 App 音乐/视频。
- Then 不主动执行 ducking。
- Then 不请求会打断外部音频的 audio focus。
- Then 覆盖手机扬声器和蓝牙耳机 smoke，并记录不同 Android 版本和设备的回归结果。

**交付结果:**

- `core.media` 新增声音提示 request mapper、重复事件去重 controller 和音频共存策略；声音继续消费既有 `WorkoutEvent` / `CueSettings`，不修改 `WorkoutCommand` / `WorkoutEvent` / training engine 语义。
- 计时执行页用阶段自身 cue settings 触发最后 N 秒临近结束声音：剩余 N / ... / 1 秒 beep，倒数到 0 后的下阶段开始 bell；最后阶段没有下阶段时，在 session completed 的 0 秒边界同样 bell；开始训练的第一阶段不额外插入 bell。所有计时阶段都可触发临近结束提醒，阈值等于阶段时长时覆盖整个阶段，阈值超过阶段时长时按阶段全长裁剪覆盖。力量执行页继续用计划 cue settings 触发准备下一组和休息临近结束声音。
- Android 播放层使用 `SoundPool` 与 `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC` media audio attributes，走媒体音量通道，不请求 audio focus，不 duck，不暂停外部音乐 / 视频，并在 route dispose 时释放资源。
- `app/src/main/res/raw/countdown_beep1.mp3` 和 `app/src/main/res/raw/stage_bell_copper_clean.mp3` 是 App 内提交资源副本；素材由用户确认为用户本人 / 项目内制作，授权用于 TrainFlow App 内短提示音分发。根目录 `countdown_beep1.mp3` 原文件和 `.local/audio/stage_bell_copper_clean.mp3` 原文件不得提交。计时临近结束按阈值 N 映射：剩余 N / ... / 1 秒播放 beep，倒数到 0 后的下阶段开始播放 bell；最后阶段完成时也在 0 秒边界 bell；开始训练的第一阶段不额外插入 bell。
- E15-1 regression fix 已补齐力量休息 cue 与自动开始执行链路：力量休息临近结束使用同一个 `WorkoutEvent.RestEnding` / `CueSettings.restEnding` / `COUNTDOWN_BEEP` 路径，默认 5 秒阈值在短休息中按实际休息时长裁剪；`soundEnabled=false` 不发声音请求。`StrengthExerciseBlock.setTimerMode == auto_after_rest` 时休息结束后自动进入下一组 active set，`manual_start` 仍进入 prepare set 等待用户手动开始。该修复不新增音频资源，不改 `WorkoutCommand` / `WorkoutEvent` 语义，不改 Room schema。
- E15-1a review fix 已把 `auto_after_rest` 休息结束自动进入下一组 active set 时的阶段 bell 收窄到唯一自动换阶段路径：strength route 只在 engine tick 从 `STRENGTH_REST` 自然进入 `STRENGTH_ACTIVE_SET` 时为 `WorkoutEvent.StrengthSetStarted` 供应上一段 rest cue，并通过专用 auto-rest transition request 复用既有 `STAGE_BELL` / `stage_bell_copper_clean.mp3`；dispatcher 不再把 `StrengthSetReady` / `StrengthSetStarted` 当作通用阶段铃。初次 prepare、普通 set-ready、manual_start 休息结束、用户手动开始本组和休息中提前开始本组都不请求该 bell；`soundEnabled=false` 仍阻止请求。训练偏好仍只作为新建 / 编辑计划默认值，执行旧计划继续消费保存到计划中的 `StrengthExerciseBlock.setTimerMode`。
- E15-1b implementation 已在力量计划编辑页暴露计划级本组计时模式设置：用户可以把当前计划显式保存为“手动开始下一组”或“休息后自动开始下一组”，保存后写回同一个 plan id，并在计划详情摘要看到保存后的用户可读模式。训练偏好继续只作为新建编辑草稿默认值，不运行时覆盖旧计划；执行引擎继续消费 `StrengthExerciseBlock.setTimerMode`。该 story 不改 engine 语义、Room schema、E15-1 / E15-1a 声音系统、TimerDial、记录、心率或设备接入。
- E15-2 TimerDial clipping and short-target linear progress fix 已完成窄范围修复：TimerDial 外圈绘制按 active glow 最大可见 stroke 预留 safe inset，避免上下边缘被 Canvas 裁切；1s / 2s active target 在 engine anchor 前移时不再使用固定 1 秒 catch-up，而是对齐真实锚点后继续按 frame elapsed / remaining duration 匀速投影。正常 target 保留 E14.6-1 防回弹行为；v2 stage group target planned duration ratio、内圈总进度、12 点数字圆标、`+15s` 语义、engine timeline、commands/events、Room、records、completion、声音和心率 / 设备边界均不改变。详见 `docs/testing/e15-2-timerdial-clipping-linear-progress.md`。
- E15-3 Stage icon semantic clarity 已完成并按用户后续反馈资源化：timed composition editor picker 和 TimerDial 的阶段图标从内置 Canvas helper 切换为项目内置白色 PNG 资源，由 `StageIconImage` 把既有 `warmup`、`work`、`speed_up`、`sprint`、`rest`、`recover_breathe`、`cooldown`、`strength`、`mobility` 和 `custom` key 映射到 `drawable-nodpi/stage_icon_*.png`；picker 继续 4 列图标 + 中文 label + 语义 content description，默认新草稿的“冲刺组合 / 冲刺”使用既有 `sprint` key。该 story 不新增 public icon key，不保存图片路径 / drawable 路径 / URL / 上传资产，不改 saved plan / session snapshot、Room、engine、TimerDial progress、commands/events、声音、力量、心率或设备边界。详见 `docs/testing/e15-3-stage-icon-semantic-clarity.md`。
- E15-4 Strength confirm-record UI collapse review fix 已 review / merged：力量训练进入 confirm-record / 确认本组阶段时，当前组大卡片自动折叠为紧凑摘要，只保留动作名、当前组序号 / 总组数、完成耗时或暂停状态、计划目标摘要和正式组 / 热身组标签；review fix 进一步压缩 compact summary 和确认卡片，并把“轻松 / 刚好 / 很吃力 / 动作变形”感受选择提前到实际重量 / 次数输入之前，使 720x1280 下 compact summary、实际输入、四个感受选项和固定底部 `确认本组` 同时可见。Prepare / active set / rest 阶段不做大改；不改 StrengthWorkoutEngine、`WorkoutCommand` / `WorkoutEvent`、Room schema / migration、session record、声音、TimerDial、E15-3 icons、records、心率或设备边界。详见 `docs/testing/e15-4-strength-confirm-record-collapse.md`。
- E15-5 Real-device polish planning gate 已完成：基于用户 2026-07-03 真机截图和反馈，下一轮拆为 E15-5a TimerDial short-target motion diagnostic + fix gate、E15-5b Strength set timer mode selector layout polish、E15-5c Strength completion sticky return action、E15-5d Strength editor and execution simplification。E15-5a 必须先用生产执行页 1s / 2s timed composition 计划采集 frame / screenrecord 证据，不再凭公式猜测；E15-5b 解决长中文选项横向溢出；E15-5c 让 completed / abandoned 复盘页固定显示返回主动作；E15-5d 按用户 2026-07-04 反馈删除力量目标组颜色占位入口，并从力量执行页移除低价值动作短提示，同时重排删除后的空白：目标组休息输入全宽或独占一行，执行主卡自然收缩，下一组卡压缩为摘要。详见 `docs/testing/e15-5-real-device-polish-planning.md`。
- E15-5a TimerDial short-target motion diagnostic + fix gate 已 review / merged：baseline 复现 ready gate route clock 旧 delay 在用户 Start / Skip 后提前 tick 新 target，导致 2s target 不到完整 1 秒就显示 `00:01` 和接近半圈外圈；修复为 timed route clock 增加 manual command anchor，`StartSession` / `PauseSession` / `ResumeSession` / `SkipStep` 后重置 tick 相位并阻止 stale coroutine tick 新 target。TimerDial projection、Canvas geometry、v2 planned-duration ratio、inner total progress、12 点数字圆标、`+15s`、engine、commands/events、Room、records、声音、心率 / 设备边界均未改变。详见 `docs/testing/e15-5a-timerdial-short-target-motion.md`。
- E15-5b Strength set timer mode selector layout polish 已 review / merged：力量计划编辑页“本组计时模式”从横向 chip 改为竖向 radio-card selector，解决 720x1280 下长中文标签突出 / 裁切问题；selector option 完整显示在卡片内，UI tree 显示当前 / 未选、radio / checkable 状态和用户可读 content description，raw `manual_start` / `auto_after_rest` 未暴露。保存语义仍通过既有 `StrengthSetTimerMode` 和 `StrengthExerciseBlock.setTimerMode` mapping；不改 engine、commands/events、Room、声音、TimerDial、E15-5a route clock、records/history、completion、目标组颜色或心率 / 设备边界。
- E15-5c Strength completion sticky return action 已 review / merged：力量 completed / abandoned 终态将 `返回计划` 移到 screen-level 固定底部 action，复盘内容预留 fixed-bottom padding，`StrengthTerminalPanel` 内不再放主返回按钮；completed 首屏、completed 滚动底部、点击返回、abandoned 固定返回、confirm-record、active/rest 非 terminal 回归均有 smoke 证据。completed / abandoned 语义保持，E15-4 confirm-record 固定 `确认本组` 不回归；不改 engine、commands/events、Room、summary 数据、声音、TimerDial、records/history、目标组颜色或心率 / 设备边界。详见 `docs/testing/e15-5c-strength-completion-sticky-return.md`。
- E15-5d Strength editor and execution simplification 已 review / merged：删除力量计划编辑页目标组颜色占位并重排目标组输入，删除力量 prepare / active / rest 当前组短提示，下一组卡片压缩为动作 / 组序号 / 重量 / 次数摘要；用户 APK 测试通过。E15 维护踩坑与解决办法已沉淀到 `docs/testing/e15-maintenance-lessons-learned.md`，后续维护短 target、声音、confirm-record、sticky return、selector 和力量 UI 减法时应先查该文档。
- 用户确认的 Timer Dial 交互回归基准：运行 -> 暂停应有圆环 / 节点 / 进度弧向中心暂停圆盘收束的连续 morph；暂停 -> 运行应反向展开并继续真实进度；左上角显示训练 / 计划名称而非阶段数字；E11.3 后不再出现心率卡片，底部按钮真机完整可见；倒计时和声音行为不得回归。
- 新增单元测试覆盖最后 N 秒映射、阶段切换映射、休息相关事件映射、声音关闭、重复事件去重和不请求 disruptive focus / duck / pause 的音频共存策略。
- E13 sound cue asset / audio coexistence audit and QA gate 已记录到 `docs/testing/e13-sound-cue-audio-coexistence-audit.md`：确认两个生产 raw 资源是当前使用资源，根目录 `countdown_beep1.mp3` 与 `.local/audio/` 仍不得提交，现有代码侧音频共存边界满足首版，后续进入真机扬声器 / 蓝牙音频共存 QA。
- 本阶段未实现固定女声 cue、TTS、自动语音教练、notification action、foreground service、exact alarm、真实心率设备、云同步、账号体系或训练引擎语义变更。手机扬声器 / 蓝牙耳机 smoke 需要在可用设备上补记录。

### Story E13.2: 固定女声阶段 cue

作为用户，
我想听到悦耳、有磁性的固定阶段提示，
以便知道阶段切换而不用看屏幕。

**验收标准:**

- Then 支持固定阶段词，例如 warm up / work / rest / cool down。
- Then 声音素材或播放策略符合 TrainFlow 克制、清晰的训练体验。
- Then 固定女声 cue 是后续增强，不阻塞 E13.1 的短提示音和音频共存。
- Then 不支持用户任意文本 TTS。
- Then 不实现自动语音教练或语音控制。

### Story E14.2: Timer Dial real-device proportion restore

**状态:** Implemented in Android timed execution visual fix

作为真机测试用户，
我希望计时执行页的 Timer Dial 在小屏手机上仍保持同心圆、稳定间距和底部按钮对齐，
以便训练中不被变形圆盘或跳动按钮干扰。

**验收标准:**

- Then E11.3 后不恢复心率显示、手动心率输入、未获取心率占位或平均心率趋势。
- Then Timer Dial Canvas 在小屏 compact morph 容器内仍保持正方形，不被父容器压成椭圆。
- Then 外圈、内圈、阶段 marker 和中心圆保持可读间距，中心圆不挤压底部按钮。
- Then 顶部训练 / 计划名称可读但不占据过多纵向节奏，总剩余时间保持辅助层级。
- Then 底部 `skip` / `+15s` / `end` 三按钮完整可见且至少 `48dp` 可点击高度。
- Then `+15s` 第一次点击进入 `确认+15s` 待确认态时，三个按钮外框高度和位置不变化，文字不换行、不裁切、不挤压左右按钮。
- Then 不改变训练引擎、`WorkoutCommand`、`WorkoutEvent`、rest extension recording、声音提示或 session record 语义。

**交付结果:**

- 计时执行页移除心率后的纵向比例重新收口：总剩余时间块变小，Timer Dial 上方弹性空间小于下方弹性空间，底部控制区保留导航栏安全区。
- `TimerDial` Canvas 使用 required square size，`TimerDialPauseMorph` compact parent 高度不再小于 `timerDialLayoutSpec.dialSizeDp`，防止圆环在真机上变成椭圆。
- `TimerDialTokens` 收窄中心圆与内圈 / 外圈关系，恢复 E10 风格三圆盘层级。
- rest extension 底部确认态改用 `+15s` / `确认+15s` / `已加+15s`，三个按钮固定同高，触摸反馈只缩放中间内容。
- 新增 / 更新回归测试覆盖无心率布局、底部按钮最小高度、Timer Dial 内部间距、正方形 Canvas / morph parent 约束和 `确认+15s` 稳定排版。
- 真机截图触发记录见 `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`；本阶段未提交 `.local/verification`、APK、截图、日志、`deliverables/` 或 `人工/`。

### Story E14.3: UI quality audit and polish sequencing

**状态:** Completed docs-only audit

作为产品维护者，
我希望先审计各功能 UI 的问题和优先级，
以便后续优化不演变成大范围 redesign 或训练语义变更。

**验收标准:**

- Then 先基于真机截图 / emulator smoke 列出计时执行、力量执行、计划编辑 / 详情、记录页、动作库和跟练雏形的 UI 问题清单。
- Then 每个问题标注影响范围：阻塞训练、影响可读性、影响触控、视觉 polish 或未来增强。
- Then 明确哪些 UI 问题必须在用户测试前修，哪些可以进入后续 polish。
- Then 不在 audit 阶段修改训练引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、session record、心率边界或声音语义。

**交付结果:**

- 新增 `docs/testing/e14-3-ui-quality-audit.md`，记录 docs-only UI 审计范围、输入文档、代码页面、720x1280 emulator smoke 证据、E14.2 真机记录引用和各功能 UI 问题清单。
- 每个问题按功能区域、状态 / 场景、问题描述、影响等级、建议修复批次、是否需要真机确认、是否需要测试或 source-pattern 回归约束标注。
- 用户测试前优先项收敛为：跟练执行页倒计时不可被底部控制遮挡、力量确认 / 休息态小屏确认、计时执行页最新矩阵与 `确认+15s` 真机确认、计划编辑保存 / 开始入口可达性、计划空状态创建入口。
- 本阶段未修改 UI 代码、训练引擎、`WorkoutCommand`、`WorkoutEvent`、session record、Room schema、声音语义或心率边界；截图保存在 `.local/smoke/e14-3-ui-audit/`，不进入 `.local/verification` 或 Git。

### Story E14.4: Feature UI polish batches

**状态:** In progress; E14.4-1 training execution common polish implemented and real-device checked; E14.4-2 low-coupling plan edit / detail polish implemented; E14.4-2b timed composition visual / semantic gate and data model decision retained; E14.4-2b-3 restart model / serializer / editor adapter foundation implemented; E14.4-2b-4 editor UI visual/code gate implemented; E14.4-2b-5 engine timeline planning gate complete; E14.4-2b-5a timeline adapter model/tests implemented and pushed as `6888e31`; E14.4-2b-5b engine integration planning gate complete; E14.4-2b-5b-1 engine adapter bridge tests added as an intentional test-first red gate; E14.4-2b-5b-2 minimum engine bridge implemented; E14.4-2b-5b-3 v2 start gate enablement + smoke implemented; E14.4-2b-5c session record compatibility tests / smoke review implemented; E14.5 TimerDial continuous progress fix complete; E14.4-2b-6 TimerDial mapping planning gate complete; E14.4-2b-6a TimerDial mapping model/state tests complete; E14.4-2b-6b production TimerDial mapping implemented; E14.4-2b-6c smoke / visual QA review gate complete; E14.4-2b closed in `docs/testing/e14-4-2b-closeout.md`

建议分批顺序：

1. 训练执行页共性 polish：计时 / 力量 / 跟练的底部控制、暂停态、结束确认、触摸高度、文字裁切、小屏安全区和跟练倒计时遮挡问题。
2. 计划编辑 / 计划详情 polish：保存入口、回填状态、阶段 / set 卡片密度、颜色选择、小屏滚动效率和计划空状态创建入口。
3. 记录页数据分析和图表 polish：真实统计、非心率趋势卡片、额外休息趋势、历史清理确认和空状态层级。
4. 动作库 / 动作详情 polish：筛选、详情可读性、替代动作入口和未来统一动作选择页准备。
5. 跟练雏形 polish：只优化已存在的 partial flow，不伪装成完整课程平台。

E14.4-2 起，每批 UI polish 必须先走视觉方案 gate：先提交 docs-only / mock-only 方案，至少包含当前问题、方案 A 保守修补、方案 B 结构优化、推荐选择、真机确认点和后续代码拆分；用户确认后，下一轮才允许进入 Kotlin / Compose / 测试实现。视觉方案阶段不得改生产代码、Room schema 或训练语义，不生成实现 APK。实现阶段再补对应回归测试或 smoke 记录，并生成 APK 给真机确认；不得恢复心率 UI、手动输入、平均心率趋势或新增真实设备接入，也不得改变训练引擎、命令、事件、session record 或 Room schema。

**E14.4-1 交付结果:**

- 新增 `docs/testing/e14-4-1-training-execution-common-polish.md`，记录训练执行页共性 polish 的输入、修复、守卫边界和回归约束。
- 新增共享 `TrainingExecutionBottomControlsSpec`，统一力量和基础跟练执行页固定底部控制区的内容 reserve、导航栏安全区预留、主 / 次按钮最小高度、行间距和垂直 padding。
- 基础跟练执行页的倒计时 / 当前内容不再被固定底部控制区遮挡，暂停 / 继续、跳过 / 下一步和结束训练仍即时可达；跟练仍保持 partial follow-along 边界，不伪装成完整课程平台。
- 力量执行页 prepare / active / confirm / rest 状态使用稳定底部 reserve，确认本组和休息态内容不会被底部按钮挤压；开始本组、完成本组、确认本组、提前开始本组、暂停 / 继续和结束训练语义不变。
- 计时执行页不重新设计 Timer Dial，保留 E14.2 的正圆 / 同心圆 / `+15s` / `确认+15s` 结果，并补 ready / running / paused / rest / rest-extension 语义回归。
- 本批不恢复心率 UI、手动输入、未获取心率占位或平均心率趋势，不接真实设备，不改训练引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、session record 或声音提示语义。
- 2026-06-22 用户真机确认 E14.4-1 四个重点项无问题：计时 ready / running / pause / rest / `确认+15s`、力量 prepare / active / confirm / rest、跟练倒计时 / 当前内容遮挡、三类执行页导航栏安全区和底部按钮可点性。

**E14.4-2 视觉方案 gate / low-coupling implementation:**

- `docs/testing/e14-4-2-plan-edit-detail-visual-proposal.md` 已完成 visual proposal，并在 E14.4-2a 确认采用方案 B：结构优化，但不做大 redesign。本阶段只做文档和只读代码 / mock 审计，不直接改代码。
- 视觉方案覆盖计划空状态、计划列表 / 详情主操作、计时计划编辑、力量计划编辑、小屏输入后的保存 / 开始训练可达性。
- 当前问题清单聚焦：空状态缺直接创建入口；计划详情开始训练层级过低；编辑页保存 / 开始入口只在长页面底部；阶段 / set 卡片默认密度高；部分预览文案偏工程契约。
- 方案 A 为保守修补：补空状态 CTA、上移详情主操作、编辑页增加靠前保存 / 开始入口、清理预览文案。
- 已确认的方案 B 为结构优化：计划详情采用可折叠计划播放列表；`开始训练`、编辑、复制和 `删除当前计划` 都归属当前展开计划卡片；计划颜色是用户手动设置的计划色，不从首个阶段或目标组推断；编辑页底部 sticky action 以绿色 `保存计划` 为主按钮、深色实心 `开始训练` 为次按钮；颜色选择复用推荐色 / 更多颜色大色板；力量目标组默认折叠，展开后设置重量、次数和休息；工程化预览文案替换为用户可读摘要。E15-5d 后，力量目标组颜色不进入当前 MVP。
- E14.4-2 low-coupling implementation 已落地，并在用户 2026-06-23 真机截图反馈后完成 review fix：计划空状态直接创建计时 / 力量计划；计划管理改为默认折叠的计划播放列表；当前计划卡片内归属 `开始训练`、编辑、复制和 `删除当前计划`；计划色块使用安全默认展示，不新增计划级持久化字段；计时 / 力量编辑页新增底部 sticky `保存计划` / `开始训练`，且 `计划预览` 卡不再重复显示这两个按钮；计时阶段卡默认折叠并可展开编辑，但不新增两层计时结构；力量目标组默认折叠，展开后设置重量、次数和休息；E15-5d 后目标组颜色占位入口已删除。工程化预览文案已替换为用户可读摘要。后续输入法真机截图补修确认：编辑数字字段时 sticky action 在 IME 可见期间隐藏，不再上浮到键盘上方遮挡表单，键盘收起后恢复。排序补修确认：力量动作 / 目标组卡片补拖动排序手柄并写入 block 顺序；计时阶段和力量动作拖拽按半卡阈值计算目标槽位，`热身` / `放松` 作为默认模板阶段可移动，保存顺序跟随编辑器最终顺序。
- 2026-06-24 顶部拖拽截图补修确认：当第一张可见卡片被屏幕上沿裁切时，拖动按钮向下拖不应跳跃；实现上采用“手指锚定 + 占位预览 + 松手提交”模型：拖动中不把被拖卡片放入重排后的 `LazyColumn` 槽位，被拖卡片始终只按手指位移移动，其他卡片临时上移 / 下移预览目标占位，松手后才提交真实顺序。后续若丰富动画，只应装饰非拖动卡片的占位位移、阴影、透明度或拖动卡片浮层感，不应让被拖卡片脱离手指锚点，也不应恢复 active-drag 列表重排模型；详细交接记录在 `docs/testing/e14-4-2-plan-edit-detail-visual-proposal.md`。
- 本轮仍未实现的低耦合后续项包括：计时阶段卡片密度继续细化、添加阶段 / 添加动作选择体验、计划颜色持久化决策；力量目标组颜色已由 E15-5d 明确排除出当前 MVP。力量“添加动作”弹窗仍只是临时低耦合改良，需另开独立任务做搜索、分类、动作卡、空状态和自定义动作创建体验优化。这些都不得在 UI polish 中静默改 Room schema。
- 必须拆出的范围：计时阶段内部目标 / 小节扩展、内部目标新增 / 删除 / 拖动 / 重命名 / 时长 / 颜色、阶段总时长由内部目标时长求和、TimerDial 原圆盘 UI 的外圈按当前阶段内部目标时长占比分段。这些内容进入独立 **E14.4-2b Timed composition editor and TimerDial ring semantics**，先规划 / 视觉确认，再代码实现；轮次与轮间休息仍保持当前编辑器顶部位置，阶段编排仍在下方，不把本轮做成全新编辑器方向。
- E14.4-2 implementation 仍不得恢复心率 UI、手动输入、平均心率趋势或真实设备接入，不得改训练引擎、`WorkoutCommand`、`WorkoutEvent`、session record、Room schema 或声音提示语义。若计划颜色或计时两层结构需要新的持久化字段，必须先拆数据 / persistence 决策 story。

### Story E14.4-2b: Timed composition editor and TimerDial ring semantics

**状态:** Completed / closed; visual / semantic gate retained; E14.4-2b-1 visual prototype / mock retained in `.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/index.html`; TimerDial existing UI overlay correction retained in `.local/smoke/e14-4-2b-timerdial-existing-ui-overlay/index.html`; E14.4-2b-2 data model decision retained in `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`; E14.4-2b-3 restart serializer / model and editor adapter foundation implemented; E14.4-2b-4 editor UI visual/code gate implemented; E14.4-2b-5 engine timeline planning gate documented in `docs/testing/e14-4-2b-5-engine-timeline-planning-gate.md`; E14.4-2b-5a timeline adapter model/tests implemented as adapter-only and pushed as `6888e31`; E14.4-2b-5b engine integration planning gate documented in `docs/testing/e14-4-2b-5b-engine-integration-planning-gate.md`; E14.4-2b-5b-1 engine adapter bridge tests documented in `docs/testing/e14-4-2b-5b-1-engine-adapter-bridge-tests.md`; E14.4-2b-5b-2 minimum engine bridge documented in `docs/testing/e14-4-2b-5b-2-minimum-engine-bridge.md`; E14.4-2b-5b-3 v2 start gate enablement + smoke documented in `docs/testing/e14-4-2b-5b-3-v2-start-gate-smoke.md`; E14.4-2b-5c session record compatibility documented in `docs/testing/e14-4-2b-5c-session-record-compatibility.md`; E14.4-2b-6 TimerDial mapping planning gate documented in `docs/testing/e14-4-2b-6-timerdial-mapping-planning-gate.md`; E14.4-2b-6a TimerDial mapping model/state tests documented in `docs/testing/e14-4-2b-6a-timerdial-mapping-model-state-tests.md`; E14.4-2b-6b production TimerDial mapping documented in `docs/testing/e14-4-2b-6b-timerdial-production-mapping.md`; E14.4-2b-6c smoke / visual QA review documented in `docs/testing/e14-4-2b-6c-timerdial-mapping-smoke-visual-qa.md`; closeout recorded in `docs/testing/e14-4-2b-closeout.md`

**流程纠偏:** 2026-06-26 已新增 `docs/testing/e14-4-2b-process-reset.md`。此前本地 E14.4-2b-3 / E14.4-2b-4 实现未通过 review gate，已回滚且不得继承；后续 E14.4-2b-3 restart 已从已提交规划重新实现 model / serializer / editor adapter foundation，E14.4-2b-4 restart 已完成 editor UI 与 editor draft adapter 连接并推送，E14.4-2b-5 已完成 docs-only timeline planning / source-boundary audit，E14.4-2b-5a 已完成并推送 adapter-only timeline model/tests，E14.4-2b-5b 已完成 docs-only engine integration planning gate，E14.4-2b-5b-1 已新增 engine adapter bridge expectation tests 并按 test-first 预期红，E14.4-2b-5b-2 已实现 minimum engine bridge 并让 focused bridge tests 转绿，E14.4-2b-5b-3 已解除有效 v2 plan 的 start gate 并完成 smoke，E14.4-2b-5c 已验证 session record compatibility，E14.5 已独立修复 TimerDial continuous progress，E14.4-2b-6 已完成 docs-only mapping planning gate，E14.4-2b-6a 已完成 mapping model/state expectation tests，E14.4-2b-6b 已完成 production TimerDial mapping 接入，E14.4-2b-6c 已完成 smoke / visual QA review gate。后续不得把 TimerDial continuous progress fix 和后续 TimerDial mapping / visual QA 修补混在同一轮。Android UI / APK / 真机截图修复类任务的 smoke 证据只能写入 `.local/smoke/<Story ID>/`，不得写入 `.local/verification` 或提交 `.local/`。

作为计时训练用户，
我想在既有计时阶段内部扩展更多目标 / 小节，
以便保留当前轮次、轮间休息和阶段编排 UI，同时让 TimerDial 外圈准确表达当前阶段内部目标的时长比例。

**验收标准:**

- Given 进入该 story 的规划阶段，Then 先输出 docs-only / mock-only 视觉与数据边界方案，不直接写 Kotlin / Compose / Room / 测试代码，不生成实现 APK。
- Given 计时计划编辑器设计，Then 轮次与轮间休息仍保持在当前 UI 的上侧位置，阶段编排仍在下方。
- Given 用户展开一个既有阶段，Then 阶段内部目标支持新增、删除、拖动、重命名、设置时长和设置颜色。
- Then 阶段总时长等于内部所有目标时长之和。
- Then 默认模板仍可以是 `热身 / 高强度工作 / 轮间休息 / 放松` 等阶段，但阶段内部可扩展 `01 开始 / 02 加速 / 03 休息` 等目标 / 小节。
- Then 颜色选择位置直接显示色块，不把中文颜色名作为主要选项文字挤进编辑卡。
- Then TimerDial 保持原 UI，外圈分段按当前阶段内部目标时长占比分割，而不是按阶段等分或按固定视觉块分割。
- Then story 在实现前必须明确是否影响 `WorkoutPlan` blocks、plan snapshot、统计比较 key、Room schema 或 TimerDial UI state；任何影响都必须有独立批准的规划结论。

**视觉 / 语义 gate 结论:**

- 本轮只做 Markdown 规划与只读语义评估，未改 Kotlin / Compose / Room / 测试代码，未生成 APK。
- 已比较三条路线：方案 A `UI-only compatibility wrapper`、方案 B `explicit timed composition model`、方案 C `visual grouping only`。
- 推荐方向是方案 B 作为长期目标，但先用方案 A 做兼容 / 视觉验证层；不得把 UI wrapper 当作最终持久化语义。
- TimerDial 保持当前已确认的圆盘 UI，只调整外圈语义：外圈表达当前阶段内部目标，并按 target planned duration ratio 分段；active 目标为粗弧，已完成目标退为细弧 / 已经过弧，阶段切换时外圈切换到下一个阶段内部结构。
- TimerDial 内圈继续表达整次训练总进度；中心圆继续表达当前 active 目标 / 阶段、倒计时和暂停 / 继续主控制；12 点数字圆标暂时沿用现有总运动阶段数语义，不作为本轮重设计项。
- `+15秒` 仍只延长当前 active rest step，不插入新阶段，不修改 `WorkoutPlan` 或 plan snapshot，不改变 `timedRestExtensionRecords` 语义；外圈和内圈 progress 必须保持 monotonic，不倒退。
- 当前 `WorkoutPlan.blocks` / `TimedCircuitBlock` / `TimedExerciseItem` 可支持旧计划兼容 wrapper，但不足以稳定表达阶段内部目标 id、目标颜色、嵌套目标、计划快照和历史趋势比较 key。
- 旧计划打开时建议自动包一层兼容目标展示；仅查看不改写，用户明确保存 / 转换后才写入未来新结构；既有 `WorkoutSession.planSnapshot` 不回写。

**E14.4-2b-1 视觉原型结果:**

- 新增本地 HTML mock：`.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/index.html`。
- 重做完整视觉设计稿：`.local/smoke/e14-4-2b-complete-visual-design/index.html`。该稿合并编辑器总览、阶段展开、旧计划 compatibility wrapper 和既有 TimerDial 外圈语义，作为后续讨论主稿。
- 原型覆盖 4 个视图 / 状态：阶段编排折叠长计划、阶段展开 + 内部目标、旧单层计划 compatibility wrapper、TimerDial 外圈语义草图。
- 计时编辑器视觉推荐：沿用当前编辑器，但层级进一步明确为 `热身` / `放松`、`轮次` / `轮间休息` 合并进一个紧凑顶部设置卡，`阶段编排` 在下方且只表示轮内重复阶段；阶段默认折叠，展示阶段名称、颜色、总时长、展开状态和阶段拖拽入口；展开后再显示内部目标数量、内部目标的名称、时长、色块、折叠 / 展开设置入口、拖拽和编辑 / 复制 / 删除入口；阶段总时长明确由内部目标求和。
- 阶段内部目标上限：每个重复阶段最多 5 个目标；默认建议为 2 个目标，即动作 + 休息，额外目标用于用户自定义训练节奏。
- 用户复审修正：阶段卡片头部只显示阶段名和总时长，不展示目标数、目标名称或 `2:30 = 45s + ...` 这类求和公式；求和来源、目标数量和剩余可添加容量只放在展开细节或辅助说明中。
- 文字防溢出规则：阶段名称软限制 10 个中文字符 / 20 个 ASCII 字符，目标名称软限制 6 个中文字符 / 14 个 ASCII 字符；列表行必须单行省略，操作列固定，未来实现应使用等价 `minmax(0, 1fr)` 的布局，避免按钮把长文字推出卡片边界。
- 旧计划 wrapper 推荐：默认按 `stageType` 自动分组为热身 / 工作 / 休息 / 放松来帮助理解，但必须标注为视觉 wrapper；查看不自动改写旧 plan，保存 / 转换前旧计划数据不被静默改写。
- TimerDial 推荐：保持原圆盘 UI，只让外圈表达当前阶段内的目标，并按时长占比分段；active 目标为粗弧，已完成目标退为细弧 / 已经过弧；内圈继续表达整次训练总阶段进度；中心圆显示当前目标 / 阶段、倒计时和暂停 / 继续。
- TimerDial UI / 动画边界：HTML 圆盘只是外圈比例语义的简化示意，不是生产视觉或动画规格；不得改生产 TimerDial UI、Canvas 几何、中心圆、内圈、底部按钮、`TimerDialPauseMorph`、continuous progress、reduce-motion、final countdown、rest extension monotonic progress、center touch feedback 或 marker / ring / center color transitions。
- 12 点数字圆标处理：它属于内圈总阶段 UI，与外圈无关；生产代码来源为 `totalWorkoutStageCount`。示例结构为热身 + 3 轮 x 2 阶段 + 2 次轮间休息 + 放松 = 10 个总阶段，所以 12 点圆标显示 `10`；当阶段 6 已完成、阶段 7 正在运行时，内圈表达 completed marker 与当前 progress brush。`当前目标序号 / 总数` 或 `当前阶段内目标数` 不进入本轮默认视觉。中心暂停图标不得用易误读为罗马数字的 `Ⅱ` 字符代替。
- 外圈比例处理：外圈归一化表达当前阶段内部目标占比，不表达固定 60 秒；`动作 45s / 休息 15s` 和 `动作 90s / 休息 30s` 都显示为 3:1。
- 原型仍是 docs / mock only，未改 Kotlin / Compose / Room / 测试代码，未生成 APK，也未改变 `WorkoutPlan`、`TimedCircuitBlock`、`TimedExerciseItem`、TimerDial 生产实现、训练引擎、命令、事件、session record、Room schema 或声音提示语义。
- TimerDial 纠偏结果：上一版独立圆盘 mock 只保留“外圈按内部目标时长占比分段”的抽象语义，不再作为视觉方向；不得继承 appbar、手机壳、说明卡、小按钮、新圆盘比例或新页面布局。纠偏 mock 以用户提供的绿色中心圆正常 TimerDial 截图 `C:/Users/25073/Downloads/Screenshot_2026-06-21-22-30-15-56_168a3d1b6f3b71..jpg`、E14.2 描述和生产代码为当前视觉基准；最新完整稿仅在外圈叠加动作 / 休息目标比例示例，例如 `45s / 15s` 与 `90s / 30s` 都显示为 3:1。`.local/smoke/e14-2-runtime/running.png` 的黄色中心圆不是可继承基准。已补读 `TimerDial.kt`、`TimerDialTokens.kt`、`TimerDialUiState.kt` 和 `TimedWorkoutSessionRoute.kt`，确认现有 `OFFICIAL_FLOW` 圆盘尺寸关系、12 点数字圆标来源和 duration-based 外圈绘制方向。

**E14.4-2b-2 数据模型决策结果:**

- 新增 `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`，本轮仍为 docs-only / planning-only，未改 Kotlin / Compose / Room / 测试代码，未生成 APK。
- 正式采用两层 timed composition model 作为长期数据方向：顶部 `warmupSec` / `cooldownSec` / `rounds` / `restBetweenRoundsSec`，下方 `stageGroups` 表达每轮内重复阶段，每个 stage group 内含最多 5 个 targets。
- 推荐方案 B：新增 versioned timed composition payload，但优先仍存入现有 `WorkoutPlan.blocks` JSON 和 `WorkoutSession.planSnapshot` JSON，不新增 Room table / column。
- 概念字段包括 `compositionVersion`、`warmupSec`、`cooldownSec`、`rounds`、`restBetweenRoundsSec`、`stageGroups`、stage id / name / color / order、target id / name / kind / durationSec / color / order、cue settings 解析层级和 compatibility metadata。
- 旧 `TimedCircuitBlock` / `TimedExerciseItem` 继续通过 compatibility wrapper 打开、查看和执行；查看不写回。用户只改旧结构可兼容字段时可保存回旧结构；新增 / 编辑真实内部 targets 或 per-target color 时，必须明确提示并转换当前 plan 为 composition v2。
- 执行 timeline 结论：warmup 在 rounds 前；每轮展开 stageGroups 和 targets；between-round rest 只插入轮与轮之间，最后一轮后不插入；cooldown 在末尾；action / custom target 映射 timed work，rest target 和 synthetic round rest 映射 timed rest；`+15s` 仍只延长当前 active rest step。
- TimerDial 结论：原生产 UI 不重做；外圈从当前 stage group targets 得到 planned duration ratio；active target 粗弧、completed target 细弧 / 已经过弧；rest extension 不重算比例，按 planned ratio 展示并用 monotonic progress floor 防倒退；内圈总阶段数按 warmup + rounds * stageGroups + between-round rests + cooldown 计算，12 点圆标稳定。
- Room / serialization 结论：仅扩展 JSON payload 时不需要 Room schema migration，但必须做 serializer compatibility、unknown version fallback、old JSON parse、新 JSON round-trip 和 snapshot immutability 测试；若未来新增 entity / table / column，必须另拆 Room migration story。
- E12 结论：timed comparable trend key 必须纳入 compositionVersion、composition block id、stageGroupId、targetId、targetKind、round / stage instance 和结构签名；旧结构和新结构默认不比较，除非 compatibility mapper 证明等价；E12 继续只消费每条历史 `WorkoutSession.planSnapshot`。

**E14.4-2b-5 engine timeline planning gate 结果:**

- 新增 `docs/testing/e14-4-2b-5-engine-timeline-planning-gate.md`，本轮只做 Markdown planning、只读 source-boundary audit 和数据影响评估，未改 Kotlin / Compose / Room / 测试代码，未生成 APK。
- v2 payload 后续应由 adapter-owned deterministic timeline 展开：warmup 在 rounds 前；每轮按 stageGroups 顺序展开 target steps；action / custom target 映射 timed work，rest target 映射 timed rest；between-round rest 只插入轮与轮之间；cooldown 在末尾。
- 每个 executable step 需要可从 `WorkoutSession.planSnapshot` 重建的 stable metadata，包括 compositionVersion、compositionBlockId、timelineStageId、timelineStageKind、stageGroupId、targetId、targetKind、roundIndex、stageGroupIndex、targetIndex、stageInstanceIndex、targetInstanceIndex、plannedDurationSec、displayName、colorHex、iconKey 和 resolved cue settings。
- legacy 计划继续走现有 engine；v2 计划走 adapter-expanded timeline；旧计划和旧 snapshot 不静默改写。E14.4-2b-4 当时的 v2 禁用开始训练策略已由 E14.4-2b-5b-3 替换为 adapter-expandable / fail-closed start gate。
- `+15s` 只允许延长当前 active rest target 或 synthetic between-round rest step；不插入新 target，不修改 plan snapshot，不重算 TimerDial planned ratio，progress 必须保持 monotonic。
- `WorkoutSession.planSnapshot` 继续原样保存 v2 JSON；默认不改 session record model 或 Room schema。若未来需要显式持久化 compositionVersion / stageGroupId / targetId 等字段，必须拆独立 migration / compatibility story。
- `WorkoutCommand` / `WorkoutEvent` 默认不变；如未来事件必须携带 composition metadata，需先记录独立 future decision。
- TimerDial 后续只消费 adapter-expanded timeline 和当前 stage/target metadata；内圈总阶段数按 warmup + rounds * stageGroups + between-round rests + cooldown，外圈只按当前 stageGroup targets planned duration ratio，12 点数字圆标继续是内圈总阶段语义。
- E12 records / trends 需要 v2 descriptor branch：trend key 区分 compositionVersion、composition block、stageGroupId、targetId、targetKind、round / stage / target instance 和结构签名；legacy 与 v2 默认不硬比。

**E14.4-2b-5b engine integration planning gate 结果:**

- 新增 `docs/testing/e14-4-2b-5b-engine-integration-planning-gate.md`，本轮只做 Markdown planning、只读 source-boundary audit、self-review 和验证，未改 Kotlin / Compose / Room / 测试代码，未生成 APK，未启动 AVD。
- 当时 `TimedWorkoutEngine` 从 `WorkoutPlanSnapshot.toTimedSteps()` 展开 flat `TimedSessionStep`，legacy warmup / stretch / cooldown / rest / timed circuit block 有现有路径，`TimedCompositionBlock` 尚不会生成可执行 step，因此 v2 禁用开始训练仍正确。该状态后续已由 E14.4-2b-5b-2 minimum bridge 和 E14.4-2b-5b-3 start gate 替换。
- v2 最小接入点应在 engine timeline construction boundary：对 `TimedCompositionBlock` 调用 `TimedCompositionTimelineAdapter`，再转换为现有 `TimedSessionStep`；route、TimerDial、record mapper 不直接解析 raw v2 JSON。
- 第一版 bridge 保持 `WorkoutCommand` / `WorkoutEvent` 不变，不新增 Room schema / migration，不改 session record model，不做 TimerDial production mapping。
- session record 兼容默认依赖 `WorkoutSession.planSnapshot` 原样保存 v2 JSON，并通过 deterministic adapter step ids 重建 v2 descriptors；rest extension 先通过 `TimedSessionStep.blockId` 携带 composition block id、`itemId` 携带 real/synthetic target id。
- v2 start 在 E14.4-2b-5b-3 已按 adapter-expandable gate 开放：bridge tests、legacy regression、unsupported / empty fail-closed、route gate 和 smoke 覆盖通过后，编辑页与计划详情可进入现有计时执行 ready gate；E14.4-2b-5c 已验证 session record compatibility，后续仍不得把 TimerDial continuous progress fix 和 TimerDial mapping 混在同一轮。
- Rollback plan: 未来若 bridge 回归，只移除 / 禁用 v2 branch，legacy path 保持不变，v2 编辑可保留但继续禁用开始训练；无 schema 变更时不需要 Room rollback。

**E14.4-2b-5b-1 engine adapter bridge tests 结果:**

- 新增 `docs/testing/e14-4-2b-5b-1-engine-adapter-bridge-tests.md`，记录 test-first bridge expectation、预期红测、边界守卫和下一步。
- 新增 focused `TimedCompositionEngineBridgeTest`：期望 v2 `TimedCompositionBlock` 先经 `TimedCompositionTimelineAdapter` 展开，再映射为现有 `TimedSessionStep`，覆盖 warmup/action/custom/rest/between-round-rest/cooldown、deterministic step id、round repeat distinct ids、rest extension、legacy unaffected、unsupported / empty fail-closed 和 v2 start gate。E14.4-2b-5b-1 初始 gate disabled expectation 已由 E14.4-2b-5b-3 更新为 bridge 后可 start。
- 新增 / 更新 source-boundary tests，限制 adapter timeline terms 只出现在 core adapter、adapter tests、boundary guard 和 bridge expectation tests；`WorkoutCommand` / `WorkoutEvent` 不携带 v2 bridge payload。
- Focused run 原始预期红：生产 `TimedWorkoutEngine` 仍不处理 `TimedCompositionBlock`，因此 v2 engine steps 为空；legacy、unsupported / empty fail-closed、start gate 和 source-boundary guard 保持绿。
- 该红测已由 E14.4-2b-5b-2 minimum engine bridge 消费并转绿。

**E14.4-2b-5b-2 minimum engine bridge 结果:**

- 新增 `docs/testing/e14-4-2b-5b-2-minimum-engine-bridge.md`，记录 minimum bridge、mapping、rest extension 和验证结果。
- `TimedWorkoutEngine` 只在 snapshot-to-step construction boundary 为 v2 `TimedCompositionBlock` 调用 `TimedCompositionTimelineAdapter`。
- Adapter step id 作为 engine step id；`compositionBlockId` 映射 `blockId`；real / synthetic target id 映射 `itemId`。
- V2 rest target 与 synthetic between-round rest 映射为 rest-extendable steps；warmup / work / cooldown 不可被 `ExtendRest` 处理。
- Legacy timed path 不变，unsupported / empty v2 fail closed；E14.4-2b-5b-3 已在 route/editor gate 层开放 adapter-expandable v2 plan start。
- 5b-2 本轮未改 route gate、TimerDial、Room schema、session record、`WorkoutCommand` 或 `WorkoutEvent`。5b-3 仅解除 start gate；5c 已完成 session record compatibility tests / smoke review，未发现需要 production fix 或 Room migration 的兼容性阻塞。

**E14.4-2b-5c session record compatibility 结果:**

- 新增 `docs/testing/e14-4-2b-5c-session-record-compatibility.md`，记录 focused compatibility tests、smoke review 和边界结论。
- 新增 focused `TimedCompositionSessionRecordCompatibilityTest`，覆盖 v2 terminal session record / summary、plan snapshot repository round-trip、adapter-derived deterministic step ids、v2 rest target `+15s`、synthetic between-round rest `+15s`、non-rest steps 不写 rest extension、legacy timed record shape、history mapper no-crash、unsupported / empty v2 fail-closed。
- 只更新 source-boundary guard 以允许该 focused compatibility test 引用 adapter-owned timeline terms；production boundary 仍限制在 engine bridge 处消费 adapter timeline。
- 本轮未做 production fix，未改 TimerDial continuous progress / mapping、Room schema / migration、session record model、`WorkoutCommand`、`WorkoutEvent` 或心率 UI / 输入 / 统计。
- 兼容结论：v2 `WorkoutSession.planSnapshot` 可通过现有 `plan_snapshot_json` 保存和读回；step records 可用 snapshot-expanded timeline 重建；真实 rest target 与 synthetic between-round rest 使用既有 `TimedRestExtensionRecord` 结构记录；legacy records 不变；history / records mapper 可读 v2 snapshot，v2 trend grouping 留给后续 E12 polish。

**E14.4-2b-6 TimerDial mapping planning gate 结果:**

- 新增 `docs/testing/e14-4-2b-6-timerdial-mapping-planning-gate.md`，记录 docs-only source-boundary audit、自审和后续拆分；本轮未改 Kotlin / Compose / Room / 测试代码，未启动 AVD，未生成 APK。
- 当前 TimerDial UI state 已可表达总进度、当前阶段进度、内圈 marker、12 点数字圆标、外圈 segments、中心内容、rest extension 后的 monotonic progress，以及 E14.5 smooth identity / anchor split；当前 production mapping 仍是 legacy work/rest cycle semantics。
- V2 mapping inputs 来自 adapter-expanded timeline：`timelineStageId`、`timelineStageKind`、`stageInstanceIndex`、`targetInstanceIndex`、`stageGroupId`、`targetId`、`targetKind`、`roundIndex`、`stageGroupIndex`、`targetIndex`、`plannedDurationSec`、`displayName`、`colorHex` 和 work/rest flags。
- Inner ring 继续表达整次训练总阶段进度；12 点数字圆标继续表达 inner total stage count，按 warmup + rounds * stageGroups + between-round rests + cooldown 计算，target 不增加该 count。
- Outer ring 对 v2 stageGroup 表达 1-5 个 targets 的 planned duration ratio；1 target 是 full ring，2 targets 按时长切两段，3-5 targets 按各自 `plannedDurationSec` 切多段；action / custom / rest target 都参与比例，active / completed / future 状态由 segment progress / current 标志表达。
- Warmup、cooldown 和 synthetic between-round rest 不是 stageGroup targets；外圈走 fallback single-segment current-stage / legacy-like semantics，synthetic between-round rest 可延长但不进入 target-ratio set。
- `+15s` 不重算 planned ratio、不插入 target、不增加第 6 段、不改 plan snapshot、不改 session record model；active rest target 或 synthetic rest 的中心倒计时可延长，外圈和内圈 progress 必须 monotonic。
- E14.5 continuous progress fix 保持独立；后续 mapper 不得把 per-second tick、remainingSec 或 segment.progress 放回 animation identity key。
- 数据影响：优先从 engine active step id + adapter-expanded snapshot metadata 推导 ephemeral mapping model；不要求 Room migration、不改 session record model、不改 engine / commands / events。
- 后续拆分改为 6a mapping model/state tests、6b production mapping implementation、6c smoke / visual QA；若 mapping regressions 出现，可回退 v2 outer ring 为 legacy-like current-stage progress，E14.5 fix 不应随意回退。

**E14.4-2b-6a TimerDial mapping model/state tests 结果:**

- 新增 `docs/testing/e14-4-2b-6a-timerdial-mapping-model-state-tests.md`，记录 test-first expectation surface、test-only mapper seam、source-boundary guard、自审边界和后续 6b 接入要求。
- 新增 `TimerDialCompositionMappingTest`，用 test-only pure expectation mapper 锁定 v2 `TimerDialUiState` 预期；该 helper 只存在于 `app/src/test`，不进入 production runtime。
- Tests 覆盖 inner total stage count 与 current stageGroup target count 分离、v2 total stage count 公式、1 target full-ring、2 targets duration ratio、3-5 targets duration ratio、action / custom / rest 参与比例、target color -> stageGroup color -> safe default fallback、completed / active / future progress、warmup / cooldown / synthetic between-round rest fallback、legacy timed plan 现有 semantics、`+15s` no-ratio-recalc / no-insert / no-sixth-segment / progress monotonic，以及 E14.5 smooth identity 不包含 per-second progress / remaining。
- 更新 `TimedCompositionBoundaryGuardTest`，只把 `feature/workoutsession/TimerDialCompositionMappingTest.kt` 加入 timeline adapter terms 的测试白名单；production TimerDial / engine / route source 仍不得出现 v2 timeline adapter mapping terms。
- 本轮未改 production TimerDial mapping、TimerDial Canvas / geometry、TimedWorkoutEngine、TimedCompositionTimeline semantics、Room、session record model、`WorkoutCommand`、`WorkoutEvent`、heart-rate UI / input / statistics，未启动 AVD，未生成 APK。
- Focused TimerDial tests 已通过；后续 E14.4-2b-6b 可在这些 expectations 之上实现最小 production mapper，并继续保留 E14.5 continuous progress identity / anchor split。

**E14.4-2b-6b TimerDial production mapping 结果:**

- 新增 `docs/testing/e14-4-2b-6b-timerdial-production-mapping.md`，记录 production mapper、测试和边界结论。
- `TimedWorkoutSessionRoute` 只读传入当前 `WorkoutPlan` context；`TimedWorkoutSessionUiState` 将 plan blocks 交给 `TimerDialUiState` mapper，不把 v2 payload 写入 engine state。
- Production `TimerDialUiState` 对 active v2 `TimedCompositionBlock` 通过 `TimedCompositionTimelineAdapter` 找到当前 timeline step 和 current `timelineStageId` targets；stageGroup 外圈按 1-5 target planned duration ratio 生成 segments，warmup / cooldown / synthetic between-round rest 走 single current-stage fallback。
- Inner ring count 继续使用 timeline stage instance count，不使用当前 stageGroup target count；12 点数字圆标保持 inner total count 语义。
- Target color fallback 为 `target color -> stageGroup color -> stage type safe default`；active / completed / future state 来自同一 stageGroup 内 target instance 顺序和当前 step progress。
- `+15s` rest extension 不重算 planned ratio、不插入 target、不产生第 6 段；current rest progress / total progress 保持 monotonic。
- 6a expectation tests 已改为经 `TimedWorkoutEngine` + `toTimedWorkoutSessionScreenState(plan = plan)` 读取 production mapper；legacy timed plan 不传 v2 plan context 时保持既有 work/rest cycle semantics。
- 本轮未改 `TimerDial.kt` Canvas geometry / layout、`TimedWorkoutEngine`、`TimedCompositionTimeline` semantics、Room、session record model、`WorkoutCommand`、`WorkoutEvent`、heart-rate UI / input / statistics；E14.5 smooth identity 仍只含 structural segment signature，per-second progress / remaining 留在 anchor。

**E14.4-2b-6c TimerDial mapping smoke / visual QA review gate 结果:**

- 新增 `docs/testing/e14-4-2b-6c-timerdial-mapping-smoke-visual-qa.md`，只复查既有 `.local/smoke/e14-4-2b-6b-timerdial-production-mapping/` 证据；未启动 AVD，未补 smoke，未生成 APK，未改 Kotlin / Compose / Room / 测试代码。
- 现有 6b 证据覆盖 v2 1 target、2 targets、5-target max-density、rest extension、warmup fallback、cooldown fallback、between-round rest fallback、legacy plan、continuous progress screenrecord、pause / resume 和 UI tree forbidden scan；5-target 作为 3-5 target bucket 的最密度视觉代表，单独 3 / 4 target 截图可作为后续非阻塞覆盖项。
- Visual QA 结论：TimerDial 仍为正圆 / 同心圆；外圈多段比例可读，不挤压中心圆；active / completed / future 可区分；target color fallback 和 boundary fallback 没有明显错误态；rest extension 未新增第 6 段、未见圆环倒退或视觉 reset；legacy plan 仍保持 legacy-like TimerDial。
- Motion QA 结论：E14.5 continuous progress 从既有 mp4 / probe 证据看仍独立且平滑，pause freeze / resume 合理；reduce-motion smoke 证据未覆盖，记录为后续 follow-up。
- Boundary QA 结论：6b smoke UI tree 不暴露工程文案或心率相关 UI；没有 TimerDial geometry/layout、engine、timeline、Room、session record、command、event 或 route drift 证据；本 gate 不进入 E12 records/trends polish 或其他 UI polish。

**后续拆分建议:**

1. E14.4-2b-1 visual prototype / mock：已完成，用于验证当前编辑器内的阶段卡、阶段内部目标行、色块入口、拖拽区分、旧计划 wrapper 和 TimerDial ring sketch。
2. E14.4-2b-2 data model decision：已完成，正式采用 versioned two-layer timed composition payload，优先存入 existing JSON，不改 Room table shape。
3. E14.4-2b-3 serializer / model and editor adapter foundation：restart implemented。本轮只落地纯 model、现有 JSON serializer / deserializer、legacy wrapper / editor draft adapter 和 focused tests；不包含 UI、engine、TimerDial、Room migration、APK 或 smoke 输出。
4. E14.4-2b-4 editor UI visual/code gate：implemented。本轮只连接计时编辑页与 editor draft adapter，保存 editor-side v2 payload；阶段 / 目标颜色通过行首圆角色块打开颜色选择，目标展开态只展示目标名称、直接数字时长输入、复制和删除；复制目标会在当前目标后方插入同参数目标；当时 `开始训练` 对 v2 draft 禁用并显示“待执行映射完成后可开始”，该 gate 已在 E14.4-2b-5b-3 bridge 后被 adapter-expandable 判定替换。官方底部导航短标签 `训 / 计 / 动 / 录` 作为本轮小屏编辑 smoke 发现的 polish 保留，只压缩可见标签且不改变导航 / 训练语义。复杂拖拽动画、高级 cue 设置和 target kind 完整图标库延后。
5. E14.4-2b-5 engine timeline planning gate：docs-only complete。本轮只定义 expansion、metadata、legacy/v2 coexistence、rest extension、snapshot/records、commands/events、TimerDial input 和 E12 impact，不实现 engine。
6. E14.4-2b-5a timeline adapter model/tests：implemented and pushed as `6888e31`。本轮只做 pure adapter model 和 focused unit tests，把 normalized composition v2 payload 展开为 deterministic timeline steps / stage instances；不接 production engine、TimerDial 或 UI route。
7. E14.4-2b-5b engine integration planning gate：docs-only complete。本轮只规划 engine bridge 接入点、v2 start 开放条件、legacy/v2 共存、records/rest extension 兼容、commands/events 不变、TimerDial 输入边界、E12 影响、拆分建议和 rollback；不实现 engine。
8. E14.4-2b-5b-1 engine adapter bridge tests：test-first red gate complete。已新增 bridge expectation tests / source-boundary tests，证明 adapter timeline 到 engine steps 的预期行为、legacy unaffected 和初始 v2 start gate disabled；该 disabled expectation 已在 E14.4-2b-5b-3 更新为 bridge 后可 start。
9. E14.4-2b-5b-2 minimum engine bridge：implemented。已接入 v2 adapter-expanded timeline，legacy path 不变，commands/events 不变，并让 5b-1 红测转绿。
10. E14.4-2b-5b-3 v2 start gate enablement + smoke planning / implementation gate：implemented。本轮只在编辑页 sticky action 和计划详情 / 计划入口解除 adapter-expandable v2 plan start gate，legacy start 不变，unsupported / empty v2 fail closed，并用 emulator smoke 验证可进入现有计时执行基础流程；仍不进入 TimerDial mapping。
11. E14.4-2b-5c session record compatibility tests / smoke review：implemented。已验证 v2 snapshot 原样保存、actual step records 可重建、rest extension 可定位、legacy records 不变、history mapper no-crash；未发现需要新 persisted metadata 字段或 Room migration 的阻塞。
12. E14.4-2b-6 TimerDial mapping planning gate：docs-only complete。已规划 current state、v2 inputs、inner total stage count、outer 1-5 targets planned ratio、boundary fallback、rest extension、continuous progress boundary、data impact、tests、split 和 rollback；不实现 production mapping。
13. E14.4-2b-6a TimerDial mapping model/state tests：implemented。已用 test-only mapper seam 证明 v2 inner / outer mapping、legacy preservation、rest extension monotonic 和 E14.5 identity non-regression，不接 production TimerDial mapping。
14. E14.4-2b-6b production mapping implementation：implemented。已实施最小 mapper，消费 adapter timeline 和当前 plan context，不重做 TimerDial UI。
15. E14.4-2b-6c smoke / visual QA：implemented。已复查 6b smoke 证据、UI tree、visual / motion / boundary 风险；无阻塞问题。Reduce-motion 与单独 3 / 4 target visual captures 记录为后续非阻塞覆盖项。
16. E14.4-2b-7 migration / compatibility / E12 trend polish：不作为 E14.4-2b closeout 阻塞项。5c / 6c 已确认当前链路不需要 Room migration 或 session record model change；若后续要深化 E12 trend key、旧新结构比较或更广泛兼容 UI，应另开 E12 records/trends polish 或独立 compatibility story。

**Closeout:**

- E14.4-2b timed composition editor + engine + records + TimerDial mapping 链路已完成并关闭，详见 `docs/testing/e14-4-2b-closeout.md`。
- 未覆盖但不阻塞关闭的后续项：reduce-motion TimerDial mapping smoke、单独 3 / 4 target visual captures、E12 records/trends polish、其他 UI polish。
- 下一步不再继续 E14.4-2b implementation；2026-06-28 真机反馈已拆入 E14.6 planning gate，E14.6-1 TimerDial progress rebound fix 已完成，E14.6-2 Completion recap page redesign planning / visual gate、E14.6-2b Compose implementation、E14.6-2c smoke / visual QA review gate 和 E14.6-2d screenshot evidence recapture 已完成；E14.6-2 screenshot-level visual QA 已由有效非黑屏截图补齐并收口；E14.6-3 stage style / icon planning、E14.6-3a data contract / model decision、E14.6-3b model / serializer tests、E14.6-3c editor style picker UI、E14.6-3d TimerDial style consumption / visual QA 和 E14.6-3e visual QA closeout 已完成，后续按用户优先级进入 E12 或其他独立 polish。

**边界:**

- 不混入 E14.4-2 普通计划编辑 / 详情 polish。
- 不静默修改训练引擎、`WorkoutCommand`、`WorkoutEvent`、session record、Room schema、声音提示语义或历史统计口径。
- 不恢复心率显示、手动心率输入、未获取心率占位、平均心率趋势或真实设备接入。

### Story E14.6: Real-device TimerDial feedback planning gate

**状态:** Planning complete; docs-only gate recorded in `docs/testing/e14-6-real-device-timerdial-feedback-planning.md`

作为产品与实现维护者，
我想把 2026-06-28 真机 TimerDial 视频 / 截图反馈拆成独立后续 story，
以便先修复最影响运动中的视觉信任感的问题，同时不把完成页、阶段样式和 TimerDial progress 混成一轮实现。

**验收标准:**

- Given 用户提供真机视频和截图，Then 文档确认文件存在于用户 Downloads，但不复制、不提交、不把素材写入仓库。
- Given 观察到 normal motion 下 TimerDial 外圈 / 当前 active segment 每秒前跳再回弹，Then 后续第一优先级 story 是 E14.6-1 progress rebound fix。
- Then E14.6-1 只处理 progress monotonic / continuous behavior，不改 outer-ring semantic mapping、TimerDial geometry、engine timeline、Room、session records、commands 或 events。
- Then E14.6-2 单独处理训练完成后的“本次数据统计复盘页面”，不把完成态停在执行页大圆盘和调试卡片上。
- Then E14.6-3 单独规划阶段颜色和图标：热身、放松、轮间休息支持颜色；轮数不需要颜色；第一版图标只用内置白色 icon key，不支持用户上传图片。
- Then planning gate 不写 Kotlin / Compose / Room / tests，不生成 APK，不启动 AVD，不恢复心率或混入 E12 records / trends。

**后续拆分:**

1. E14.6-1 TimerDial progress rebound fix。
2. E14.6-2 Completion recap page redesign。
3. E14.6-3 Stage style system planning / design。

**边界:**

- 视觉承托圆环加粗属于后续 polish，不和 E14.6-1 默认合并。
- 用户下载目录视频 / 截图只作为人工反馈来源，不进入 Git。
- E14.6-3a 已完成阶段样式 data contract / model decision；若未来新增 Room table / column、上传资产或独立查询模型，必须另拆 migration / asset-storage story。

### Story E14.6-1: TimerDial progress rebound fix

**状态:** Implemented; fix and verification recorded in `docs/testing/e14-6-1-timerdial-progress-rebound-fix.md`

作为计时训练用户，
我希望 TimerDial normal motion 下 active 外圈 / active segment 连续单调推进，
以便每秒 tick 不会出现先往前跳一下再回来的视觉回弹。

**验收标准:**

- Given same stage / segment identity，Then displayed active progress 不得被一秒 tick anchor 拉回更小值。
- Given 新的 tick anchor 尚未应用，Then 不复用旧 frame elapsed 去投影新 anchor。
- Given stage / segment identity 变化、skip、pause / resume projectability 变化、terminal / non-projectable 状态或 reduce-motion 变化，Then 允许 displayed progress reset / freeze 到对应边界。
- Given rest extension 更新当前休息时长，Then active ring / segment displayed progress 不倒退。
- Then 本 story 不改 completion recap page、stage color / icon system、outer-ring semantic mapping、Canvas geometry / layout、engine、timeline、Room、session record、commands、events 或 heart-rate UI。

**验证覆盖:**

- TimerDial focused tests 覆盖 pending anchor、same-identity monotonic clamp、active segment tick monotonic、identity reset、pause / terminal freeze、reduce-motion discrete 和 rest extension monotonic。
- Android smoke 捕捉 running TimerDial 多秒运行、pause / resume、rest extension 可行路径、UI tree 和 logcat。

### Story E14.6-2: Completion recap page redesign planning / visual gate

**状态:** Planning complete; docs-only visual gate recorded in `docs/testing/e14-6-2-completion-recap-page-planning.md`

作为完成一次训练的用户，
我希望训练结束后看到“本次数据统计复盘页面”，
以便明确知道训练已经完成，并快速回看本次真实数据，而不是继续停在执行页大圆盘和完成卡片上。

**验收标准:**

- Given timed 或 strength session 进入 `completed`，Then UI 信息架构应切换到 completion recap page，而不是把大 TimerDial 保留为主视觉。
- Then 页面顶部明确展示 `已完成` 状态，并提供克制的庆祝感，例如完成徽章、轻量 check / halo 或短促完成动效。
- Then 页面中部展示关键数据摘要，并复用已有 completion recap / session summary 内容，不造假趋势、不引入未实现健康数据。
- Then rest extension、skipped、pause summary 和 early-end 信息只来自既有 summary / session record 映射；缺少当前 UI state 暴露时不编造。
- Then 页面底部提供主返回动作，产品推荐默认返回 `训练首页`；`查看记录` 只作为低层级次入口候选，不与返回形成两个主按钮。
- Then `abandoned` 使用同一 recap shell 的结束摘要语气，标记 `已结束` / `提前结束`，不显示 completed celebration，也不标注 `已完成`。
- Then reduce-motion 时关闭或 snap 庆祝动效，并保留静态完成状态。
- Then 本 story 不写 Kotlin / Compose / Room / tests，不生成 APK，不启动 AVD，不改 `WorkoutSession` / plan snapshot / rest extension record 语义，不进入 E14.6-3 stage color/icon system 或 E12 records/trends implementation。

**视觉方向:**

- 有完成反馈和轻微庆祝，但保持训练产品的克制感，不做营销页。
- 完成页主视觉不是大 TimerDial；如保留圆盘元素，仅作为小型完成徽章或训练类型标记。
- 复盘内容优先于装饰，底部返回保持小屏和系统导航安全区可达。

**实现拆分建议:**

1. E14.6-2b Compose implementation：已实现 dedicated completion recap page，复用 summary UI state 和 session summary，完成/放弃终态分 tone，不改记录语义。
2. E14.6-2c smoke / visual QA review gate：已复查既有 smoke 证据；UI tree 语义 / 交互覆盖可用，但截图证据损坏，visual pixel QA 当时未收口。
3. E14.6-2d screenshot evidence recapture：已补有效截图证据和视觉复核；不改记录语义、Room、commands、events、TimerDial、E12、E14.6-3 或心率边界。
4. E14.6-2a static visual mock 不再作为 E14.6-2 收口阻塞项；2d 已补齐当前 app 的截图级视觉确认。

### Story E14.6-2b: Completion recap page Compose implementation

**状态:** Implemented; closeout recorded in `docs/testing/e14-6-2b-completion-recap-page-compose.md`

作为完成一次计时训练的用户，
我希望 completed / abandoned 终态进入独立复盘页面，
以便用清晰的训练后信息架构回看本次真实数据，而不是继续停留在执行页大 TimerDial 主视觉上。

**实现结果:**

- Completed terminal state renders a dedicated recap page with compact completion badge, `已完成`, `本次复盘`, key summary metrics, reused session overview / recap details, and bottom `返回训练首页` primary action.
- Abandoned / early-ended state reuses the recap shell with `已结束` / `提前结束` tone and no completed celebration.
- Existing recap data is reused from `TimedWorkoutSessionScreenState.summary`, `terminalSummary`, and the existing timed session summary panel; skipped, rest extension, pause, end-state, trained-area, and recovery content are not recomputed or invented.
- The shell return action brings the user back to the training home after terminal recap.

**验证覆盖:**

- Focused training execution / workout session tests.
- Full debug unit tests.
- Debug build and lint.
- Boundary diff checks for Room/schema, workout commands/events, and TimerDial files.
- Android emulator smoke covering completed recap, rest extension evidence, abandoned / early-ended recap, UI-tree forbidden search, logcat fatal search, and AVD shutdown.

**边界:**

- No session record semantic change.
- No Room schema / migration change.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No TimerDial progress / mapping / geometry change.
- No E12 records / trends polish.
- No E14.6-3 stage color / icon system work.
- No heart-rate UI / input / statistics restoration.

### Story E14.6-2c: Completion recap smoke / visual QA review gate

**状态:** Review complete; screenshot-level visual QA was blocked by evidence quality at this gate and later resolved by E14.6-2d, recorded in `docs/testing/e14-6-2c-completion-recap-smoke-visual-qa.md`

作为准备收口完成页体验的团队，
我希望复查 E14.6-2b 的 smoke 证据、UI tree、视觉层级和边界，
以便确认 completed / abandoned recap 是否真的可以作为 E14.6-2 的完成页收口，而不是只依赖实现说明。

**复查结果:**

- E14.6-2b UI tree 覆盖 completed recap top state：`训练已完成`、`已完成`、`本次复盘`、`本次训练已完成`、`关键数据摘要`、`总时长`、`完成阶段` 和底部 `返回训练首页`。
- E14.6-2b UI tree 覆盖 recap summary / details：skipped、rest extension、pause、recovery recommendation、session overview / details 和 bottom return。
- E14.6-2b UI tree 覆盖 abandoned / early-ended shell：`训练已提前结束`、`已结束`、`提前结束`、reused recap content 和 bottom return。
- Forbidden UI tree search 未发现 old composition smoke entries 或 heart-rate UI terms。
- Focused logcat fatal scan 未发现 TrainFlow fatal exception、process crash、ANR 或 fatal signal。
- E14.6-2b 所有 `.png` 文件不是有效 PNG，无法作为视觉截图证据。
- E14.6-2c 补证据尝试未生成 APK；仅安装既有 debug APK。该尝试下系统 Home 截图正常，但 TrainFlow 当前截图为全黑且未能通过点击重新跑到 recap。

**结论:**

- Semantic / interaction smoke evidence is accepted from E14.6-2b UI trees.
- Screenshot-level visual QA was not accepted from the E14.6-2c evidence.
- E14.6-2d later recaptured valid screenshot evidence and closed this blocker.

**边界:**

- No Kotlin / Compose / Room / production test changes.
- No session record semantic change.
- No Room schema / migration change.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No TimerDial progress / mapping / geometry change.
- No E12 records / trends polish.
- No E14.6-3 stage color / icon implementation.
- No heart-rate UI / manual input / trend restoration.

### Story E14.6-2d: Completion recap screenshot evidence recapture

**状态:** Completed; screenshot evidence recaptured and visual QA accepted, recorded in `docs/testing/e14-6-2d-completion-recap-screenshot-recapture.md`

作为收口完成页体验的团队，
我希望重新采集有效的 completion recap 页面截图证据，
以便把 E14.6-2b 的 UI tree 语义覆盖补成可打开、非黑屏、可视觉评审的截图证据。

**诊断结果:**

- E14.6-2b 的 16 个 PNG 都不是零字节，但文件头为 `FF FE FD FF 50 00 4E 00 ...`，不是有效 PNG signature，判断为 text / UTF-16-style binary corruption。
- E14.6-2c 的 TrainFlow current screenshots 是有效 PNG 但全黑；系统 Home 截图正常、UI tree 仍有 TrainFlow home、logcat 无 fatal / ANR。2d 新 AVD 会话未复现该黑屏，当前判断为 transient emulator / Surface / capture timing 或旧 runnable state 问题，不是 production recap 渲染阻塞。
- 2d 使用 `adb shell screencap -p /sdcard/<name>.png` + `adb pull` 重新采集，避免 stdout text redirection 破坏二进制 PNG。

**验收结果:**

- Completed recap top / summary / details PNG 均为有效 `720x1280` PNG，非黑屏，可打开并显示 `已完成`、`本次复盘`、关键数据摘要、scrolled recap details 和 bottom `返回训练首页`。
- Abandoned / early-ended shell PNG 有效且非黑屏，显示 `已结束` / `本次训练已提前结束`，不显示 completed celebration。
- UI tree、logcat tail、PNG validation 和 AVD shutdown evidence 已写入 `.local/smoke/e14-6-2d-completion-recap-screenshot-recapture/`，但不提交 `.local/`。
- E14.6-2 screenshot-level visual QA 已收口。

**边界:**

- No Kotlin / Compose / Room / production test changes.
- No session record semantic change.
- No Room schema / migration change.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No TimerDial progress / mapping / geometry change.
- No E12 records / trends polish.
- No E14.6-3 stage color / icon implementation.
- No heart-rate UI / manual input / trend restoration.

### Story E14.6-3: Stage style / icon planning

**状态:** Planning complete; docs-only plan recorded in `docs/testing/e14-6-3-stage-style-icon-planning.md`

作为计时训练用户，
我希望热身、放松、轮间休息、普通阶段和阶段内目标都能拥有清晰的阶段样式，
以便 TimerDial 中心圆和外圈能用颜色与内置图标准确表达当前训练状态，而不是只让重复阶段有样式。

**规划结论:**

- Stage style 是 contract-level 概念，由颜色和稳定内置 `iconKey` 组成。
- 热身、放松、轮间休息、普通 stage group 和内部 targets 都可以解析 style。
- 轮数只是重复结构和计数，不需要颜色或 icon。
- 第一版只提供项目内置白色单色 icon key；不保存图片路径、SVG 路径、资源路径、URL 或上传资产引用。
- 推荐内置 key 至少覆盖 `warmup`、`work`、`speed_up`、`sprint`、`rest`、`recover_breathe`、`cooldown`、`strength`、`mobility` 和 `custom`。
- 颜色 fallback 为 active target style -> parent stage group style -> boundary stage style -> type safe default -> final safe fallback。
- Icon fallback 为 active target style -> parent stage group style -> boundary stage style -> boundary / type default -> `custom`。
- 现有 legacy timed `TimedExerciseItem` 和 composition v2 stage group / target 已有 `colorHex` / `iconKey` 保存路径；E14.6-3a 已决定 warmup / cooldown / synthetic between-round rest 的用户可编辑持久化样式只新增到 versioned timed composition JSON payload。
- Boundary style fields 为 `warmupStyle`、`cooldownStyle` 和 `restBetweenRoundsStyle`，每个字段只包含可选 `colorHex` 与可选 `iconKey`。
- 不建议 Room migration；新增 Room table / column 必须另拆 migration story。
- 用户上传图片、自定义图片库、裁剪、存储、备份、版权处理和远程 icon pack 均为 post-MVP / later story。

**后续拆分:**

1. E14.6-3a data contract / model decision。（Completed; see `docs/testing/e14-6-3a-stage-style-data-contract-decision.md`）
2. E14.6-3b model / serializer tests。（Completed; see `docs/testing/e14-6-3b-stage-style-model-serializer-tests.md`）
3. E14.6-3c editor UI style picker。（Completed; see `docs/testing/e14-6-3c-editor-style-picker-ui.md`）
4. E14.6-3d TimerDial consumption / visual QA。（Completed; see `docs/testing/e14-6-3d-timerdial-style-consumption.md`）
5. E14.6-3e TimerDial visual QA closeout。（Completed; see `docs/testing/e14-6-3e-stage-style-timerdial-visual-qa.md`）
6. Optional TimerDial visual polish：内部阶段圆环下浅色承托圆环可单独加粗，但不混入 style/icon data planning。

**边界:**

- No Kotlin / Compose / Room / production test changes in this planning story.
- No icon picker implementation.
- No resource, SVG, image, or icon asset files.
- No user-uploaded image support.
- No TimerDial production change or Canvas geometry change.
- No E12 records / trends polish.
- No heart-rate UI / manual input / trend restoration.

### Story E14.6-3b: Stage style model / serializer tests

**状态:** Implemented; model / serializer / focused tests recorded in `docs/testing/e14-6-3b-stage-style-model-serializer-tests.md`

作为后续阶段样式 UI 和 TimerDial 消费的基础，
我想先把 E14.6-3a 决策落到 Kotlin model、JSON serializer 和 focused tests，
以便 warmup、cooldown 和 between-round rest 的样式能随计划和 session snapshot 稳定 round-trip，且旧计划不破坏。

**交付结果:**

- `core.model` 新增 `TimedStageStyle` 和 MVP 内置 `TimedStageIconKey` 合同。
- `TimedCompositionBlock` 新增可选 `warmupStyle`、`cooldownStyle`、`restBetweenRoundsStyle`。
- `WorkoutPlan.blocks` JSON 和 `WorkoutSession.planSnapshot` JSON round-trip 这些 boundary style 字段。
- `TimedCompositionStageGroup` / `TimedCompositionTarget` 继续保留现有 flat `colorHex` / `iconKey` 字段。
- `colorHex` 只接受 `#[0-9A-Fa-f]{6}` 并规范化为大写；invalid style color 在 `TimedStageStyle` 上归一为 `null`。
- `iconKey` 只接受内置 key；未知、URL、path、resource、SVG、图片名、base64 和 uploaded-asset-like 值归一为 `null`。
- old v2 payload without boundary style 仍可解码；legacy timed JSON 不回写 composition v2 或新增 style fields。

**边界:**

- No editor style picker UI.
- No TimerDial production consumption change.
- No Room schema / migration / `app/schemas` change.
- No resource, image, SVG, icon asset, or upload support.
- No session record model, `WorkoutCommand`, or `WorkoutEvent` change.
- No AVD smoke or named user APK generation.

### Story E14.6-3c: Editor style picker UI

**状态:** Implemented; editor UI, focused tests, and smoke recorded in `docs/testing/e14-6-3c-editor-style-picker-ui.md`

作为计时计划编辑用户，
我希望能直接为热身、放松、轮间休息、阶段组和阶段内目标选择颜色与内置图标，
以便后续 TimerDial 可以消费同一份 v2 style payload，而不需要新增图片资源或数据表。

**交付结果:**

- Timed composition editor 的基础时间与轮次卡新增 `阶段样式` 区。
- warmup、cooldown 和 rest between rounds 可编辑 style；rounds 不显示 style 控制。
- Stage group 使用 `阶段样式` picker。
- Target 使用 `目标样式` picker。
- Picker 在一个 flow 内复用现有推荐色 / 更多颜色 swatches，并新增内置 icon grid。
- Icon grid 使用 Compose Canvas 白色单色 glyph，不新增 `app/src/main/res` 资源、图片、SVG 或 material icons dependency。
- 选择结果写入 editor v2 draft payload，保存后通过现有 JSON serializer 持久化。
- Picker state 与 tests 使用中文 label / content description，不把工程 key 作为主 UI 文案。

**边界:**

- No TimerDial production consumption, Canvas geometry, or `TimerDialUiState` change.
- No `TimedWorkoutEngine` change.
- No Room schema / migration / `app/schemas` change.
- No session record, `WorkoutCommand`, or `WorkoutEvent` change.
- No resource, image, SVG, upload, file picker, gallery, URL, or asset-path support.
- No E12 records / trends polish or heart-rate UI restoration.

### Story E14.6-3d: TimerDial style consumption / visual QA

**状态:** Implemented; TimerDial style consumption, focused tests, and smoke recorded in `docs/testing/e14-6-3d-timerdial-style-consumption.md`

作为执行计时训练的用户，
我希望 TimerDial 能消费已保存的阶段 / 目标样式，
以便外圈颜色和中心图标与编辑器里的阶段编排保持一致，同时不改变训练执行语义。

**交付结果:**

- TimerDial v2 active stageGroup 外圈 segment 仍按 1-5 个 targets 的 planned duration ratio 分段。
- Segment color 按 target style -> stageGroup style -> boundary style -> type default -> safe fallback 解析。
- Warmup、cooldown 和 restBetweenRounds 使用各自 boundary style；rounds 仍无颜色和 icon。
- Active center icon 按当前 target / stageGroup / boundary resolved `iconKey` 绘制。
- 中心 icon 是 Compose Canvas 自绘白色单色 built-in key，不新增图片、SVG、drawable、raw resource、上传入口或外部依赖。
- Legacy timed plans 保持既有 TimerDial 语义和默认 fallback。
- Invalid / missing `colorHex` 或 `iconKey` 安全 fallback，不崩溃。
- Rest extension 后不重算 planned ratio，不插入第 6 段，不导致 progress 倒退。
- E14.5 / E14.6-1 continuous progress identity 保持 structural，不重新引入 per-second progress / remaining-second identity。

**边界:**

- No TimerDial Canvas geometry, dial dimensions, layout ratio, or bottom control change.
- No `TimedWorkoutEngine` execution order / duration / rest extension semantic change.
- No Room schema / migration / `app/schemas` change.
- No session record model, `WorkoutCommand`, or `WorkoutEvent` change.
- No E12 records / trends polish or heart-rate UI restoration.
- No resource, image, SVG, upload, file picker, gallery, URL, or asset-path support.

### Story E14.6-3e: Stage style / TimerDial visual QA closeout

**状态:** Review complete; docs-only closeout recorded in `docs/testing/e14-6-3e-stage-style-timerdial-visual-qa.md`

作为发布前复查者，
我希望基于 E14.6-3d 的 smoke 证据确认 stage style / icon 链路能收口，
以便下一轮可以切到 E12 records / trends polish 或其他独立 polish，而不是继续扩张 E14.6-3 数据链路。

**复查结论:**

- `.local/smoke/e14-6-3d-timerdial-style-consumption/` 的 PNG / XML / logcat / scan 证据足以支持 closeout。
- Editor style picker 已覆盖 warmup、cooldown、restBetweenRounds、stage group 和 target 的 color + built-in `iconKey` 保存链路。
- TimerDial 已消费 target -> stageGroup -> boundary -> type fallback style：外圈颜色、中心高对比 built-in Canvas icon、boundary fallback 均有代表截图或 focused test 覆盖。
- Warmup、rest target、styled work target、between-round rest、cooldown、pause / resume 和 `确认+15s` 均有复查证据。
- TimerDial visual QA 通过：圆盘保持正圆 / 同心，外圈多色 segment 可读不过花，中心 icon 不压迫倒计时，boundary style 不像错误 fallback。
- Legacy timed、rest extension no-sixth-segment / no-ratio-recompute / no-rebound、E14.5 continuous progress 和 E14.6-1 rebound fix 由 focused tests / prior smoke 继续守住。
- No-motion / reduce-motion style-specific smoke、单独 3 / 4 target 截图、dedicated legacy screenshot、显式 `adb-devices-after` artifact 和承托环厚度 polish 均记录为非阻塞 follow-up。

**边界:**

- No Kotlin / Compose / Room / production test / resource changes in this closeout.
- No TimerDial Canvas geometry, dial dimensions, layout ratio, engine timeline, rest extension semantics, session record, `WorkoutCommand`, or `WorkoutEvent` change.
- No E12 records / trends polish in this closeout.
- No heart-rate UI / manual input / trend restoration.
- No resource, image, SVG, upload, file picker, gallery, URL, or asset-path support.

## 5. FR 覆盖映射

| FR | 覆盖 Epic/Story |
|---|---|
| FR-001, FR-002 | E2.1, E2.4 |
| FR-010, FR-011, FR-012, FR-013 | E2.2, E2.3, E2.4, E7.1 |
| FR-020 到 FR-025 | E3.1, E3.2, E3.3, E3.4, E7.3 |
| FR-030 到 FR-037 | E4.1, E4.2, E4.3, E4.4 |
| FR-040 到 FR-042 | E6.1, E6.2 |
| FR-050 到 FR-054 | E1.1, E1.2, E1.3, E1.4, E5.4 |
| FR-060 到 FR-062 | E3.4, E4.1, E7.2 |
| FR-065 到 FR-068 | E6.3（历史抽象边界）、E17-2（当前产品合同）、E17-3 / E17-4（后续架构与 readiness） |
| FR-070 到 FR-081 | E5.1, E5.2, E5.3, E5.4 |
| UI 定制与设计系统 | E8.1, E8.2, E8.3, E8.4 |
| 用户测试后训练模式边界 | E10.1, E10.2, E10.3 |
| Timer Dial 设计与真实记录、统计、心率和音频后续 | E10.4, E10.5, E10.6, E10.7, E10.8, E10.9, E10.10, E10.11, E10.12, E10.13, E10.14, E10.15, E10.16, E10.17, E10.18, E11, E12, E13, E14.6, E14.6-2 |

## 6. 推荐实施顺序

1. E0.1 到 E0.4：先建 Android 工程与核心地基。
2. E1.1 到 E1.4：动作库和内容基础。
3. E2.1 到 E2.4：计划创建与管理。
4. E3.1 到 E3.4：计时训练闭环。
5. E4.1 到 E4.4：力量训练闭环。
6. E5.1 到 E5.4：总结、历史、恢复。
7. E6.1 到 E6.3：跟练雏形与健康数据边界。
8. E7.1 到 E7.3：提醒、声音、震动、偏好。
9. E8.1 到 E8.4：设计系统、UI shell 和开源定制边界。
10. E9.1 到 E9.4：硬化、验收与用户测试修复包。
11. E10.1 到 E10.5：训练模式边界、计时训练重做、执行页主操作可达性、记录闭环前置和 Timer Dial 设计工作流。
12. E10.6 到 E10.9：Timer Dial 静态视觉方案、Compose 原型、生产集成、连续进度 polish 和用户测试 APK。
13. E10.10：计划保存持久化和保存入口真实可用性检查。（Implemented）
14. E10.11：使用 `huashu-design` 做 3 个 Timer Dial HTML 高保真原型方向。（Implemented; prototype served as E10.12 input）
15. E10.12：Timer Dial Compose landing，把 E10.11 `TrainFlow Official Fusion` 方向落到 Android 生产执行页，完成视觉减字、总剩余时间居中放大、圆盘放大、环线层级和动态浅点修复。（Implemented）
16. E10.13：Ready Start Gate，计时训练从编辑页或计划详情开始后先进入极简启动界面，点击中心圆才真正开始训练。（Implemented）
17. E10.14：Rest Extension Semantics And Recording，明确 `+15秒` 只延长当前休息阶段，加入二段式确认和每段上限，并把确认成功的额外休息保存为真实 session record。（Implemented）
18. E10.15：Motion Timing Rules，建立训练交互动效 token、时长范围、easing、可中断和 reduce-motion 边界。（Implemented）
19. E10.16：Motion Landing，把 E10.15 token 最小落地到计时训练 ready gate、center dial、Timer Dial 状态变化和 `+15秒` 二段确认反馈，并补齐生产 reduce-motion source / snap 降级路径。（Implemented）
20. E10.17：Stage Color Picker，为计时阶段编辑页提供推荐色 / 更多颜色选择、集中色板、可访问选中态、计划持久化恢复和 Timer Dial 阶段色消费。（Implemented）
21. E10.18：Plan Edit Backfill，从计划详情进入计时 / 力量编辑器，回填已保存计划并保存回同一 plan id，同时保持历史 session snapshot 不回写。（Implemented）
22. E11.1：Heart-rate source boundary / unavailable state refinement；收口 source-aware provider/model 边界，不接设备、不做手动输入、不持久化心率、不画平均心率趋势。（Implemented; UI later hidden）
23. E11.2a / E16：HUAWEI Band 9 on non-Huawei Android feasibility smoke；E11.2a 原条件为广播未开启 / Huawei Health 连接占用，未发现华为设备。E16 广播开启 retest 已实现 debug-only `HR Broadcast Smoke` Activity、独立 debug launcher 和 debug 蓝牙权限，`app/src/main` 不暴露 smoke route / 首页按钮 / callback / Activity 引用；18:32 用户截图不可归因，18:46 用户截图已形成正向证据链：扫描到 `HUAWEI Band HR-OD7` 广播 `services=[0x180D]`，连接同一地址成功，发现 `0x180D` / `0x2A37 notify`，CCCD 写入成功并连续收到 bpm notify。E16 已 reviewed / merged 到 main（merge commit `bbd4296`）。该结果已进入 E16-1 / E16-2 provider 地基，但仍不做训练页 UI 或记录接入；E16-3 / E16-3a 已完成 HTML 视觉方案，顶部 pill 推荐已被 App 内可拖动浮动胶囊取代；E16-4 已完成 opt-in / settings / permission rationale 规划。（Positive evidence captured; provider foundation hardened; floating capsule visual revision and opt-in planning complete）
24. E16-2：Production BLE HRS provider hardening；将 E16-1 debug spike 收敛为 production-capable `core.health` provider / state / permission planner / DataStore selected-device preference / lifecycle cleanup 地基，不改 production manifest，不接训练页 UI，不写 session record，不做 heart-rate statistics；2026-07-07 真机 smoke 已覆盖 debug entry、HR Broadcast Smoke、Band 9 `0x180D` scan、selected / connecting / waiting / notify / live bpm `84-91` 和 stop 状态。（Implemented; real-device smoke passed）
25. E16-3 / E16-3a：Heart Rate UI HTML Visual Planning；E16-3 初版新增 HTML 高保真视觉入口和 README，提供顶部状态 pill、当前卡片角标、底部微状态 3 个变体作为探索。后续讨论确认顶部 pill 有布局冲突风险，当前推荐已改为 App 内可拖动浮动心率胶囊，并新增 `docs/design/e16-heart-rate-floating-capsule-decision.md`；E16-3a 已完成浮动胶囊 HTML 修订，覆盖拖动吸附、安全区、连接 / 数据状态、区间 + bpm、深红超上限、未训练只显示不记录、训练记录模型另拆和 720x1280 no-overlap evidence。（Visual planning complete; Android implementation still split later）
26. E16-4：Heart-rate opt-in / settings / permission rationale planning；新增 `docs/testing/e16-4-heart-rate-opt-in-settings-planning.md`，明确默认关闭、设置页显式开启、开启前 rationale、BLE 权限只在用户主动开启 / 选择设备 / 重新扫描后触发、不使用系统 overlay 权限、未训练只显示不记录、设备偏好只保存 identifier / display name、关闭后不扫描 / 不连接 / 不记录、异常状态文案、非医疗文案和 E16-5 之后实现拆分。（Docs-only planning complete）
27. E16-5：Heart-rate settings / opt-in UI implementation；在现有 Android 设置页新增 `心率与设备` 卡片和 `heartRateDisplayEnabled` DataStore 显式开关，默认关闭；关闭后明确不显示胶囊、不扫描、不连接、不记录；开启后仅显示已启用显示偏好、后续可选择设备和未连接源 / 待选择设备，不请求权限、不扫描、不连接、不接训练页浮动胶囊、不记录心率。（Implemented）
28. E16-6：Heart-rate BLE permission request flow；在设置页为已开启心率显示的用户提供 `准备连接设备` / `授权蓝牙权限` 两段式入口，先展示中文 rationale，再触发 Android runtime permission request；production manifest 新增 scoped BLE permissions 与 Android 11 及以下 scan compatibility fallback，但仍不扫描、不连接、不展示设备列表、不记录心率。（Implemented）
29. E16-7：Heart-rate device picker / source status；在 `心率与设备` 设置页实现用户主动 12 秒有限时扫描、HRS 候选设备列表、选择设备、保存 `bleHeartRateDeviceIdentifier` / `bleHeartRateDeviceDisplayName`、清除已保存设备和 source status 文案；仍不做 GATT connect、`0x2A37 notify`、bpm 读取、训练页胶囊或记录落库。（Implemented）
30. E16-8：App-shell floating heart-rate capsule；在 official app shell 内实现浮动心率胶囊 overlay、collapsed / expanded、tap、drag threshold、左右安全边 snap、fixed exclusion zones 和 mapper-ready 状态。固定 AVD `TrainFlow_Pixel_API_36` 已以 `emulator-5554` 完成 AVD UI smoke，`bounds-check.txt` / `bounds-evidence.json` 为 `overall=PASS`，rectangular shadow fix 和 shadow-fix smoke 已完成；2026-07-09 review fix 已按真机反馈收敛 expanded 为紧凑 popover / compact fallback，debug APK 默认 launcher 直进 TrainFlow，`HR Broadcast Smoke` 保留 debug-only explicit tool；2026-07-10 follow-up 将 expanded 改为 `来源` / `记录` / `区间` / `更新` 信息面板，不再把 `心率与设备` 作为主按钮，并把设置页授权、扫描、停止扫描、选择设备入口改为明确全宽按钮；`记录` 格只显示 `未记录`、`当前只显示状态` 或 `训练记录：后续开启`，不显示 `1s sampling`；胶囊轻点 / 拖动统一由 pointer input 处理；蓝牙权限 runtime result 区分普通拒绝和永久拒绝，普通拒绝保留重新授权入口，永久拒绝进入系统设置路径。年龄、估算最大心率、可选手动最大心率、上限提醒阈值、区间说明和非医疗提示已记录为后续心率个人参数设置，不在 E16-8 完成；仍不做 GATT connect、live bpm 生产接入、训练记录落库或分析。（Implemented; AVD smoke completed; shadow fix completed; review fixes implemented）
31. E16-9：HeartRateState -> floating capsule live state mapping；official app shell 只读收集 provider/source/live state 并映射到浮动胶囊，状态优先级覆盖 hidden、权限未赋予、蓝牙关闭、未连接源、已保存但未连接、正在连接、等待数据、live bpm、数据过期、离线和 recoverable error。Band 9 人工测试已确认 production path 可连接并获得 live bpm，关闭广播后显示 `连接异常`；feedback fix 已把 active `providerState` 与设置页 `scanState` / candidates 分离，修复已连接时重新扫描的 12 秒窗口内 bpm / `正在连接` 交替。E16-9b 明确 saved identifier/displayName 只是偏好：cold start 显示 `未连接 + 已保存设备`，不自动 scan/connect，也不保留旧 runtime error；设置页有 `连接已保存设备`，用户点击后才进行一次 12 秒 HRS `0x180D` scan，只有 exact identifier match 才自动进入既有 connect path，同名不自动连接，未匹配显示 `未找到已保存设备` 并保留其他 HRS candidates 手选。live bpm 时为 `扫描其他设备`，scan active/candidates/timeout 不影响当前 GATT 或 bpm，只有手选新设备后才切换 target。2026-07-11 已从 `4b7689a` 重新 build/install 到固定 AVD，确认默认关闭时胶囊隐藏、开启显示偏好后显示 `权限未赋予`；code review accepted，随后 Band 9 修复后人工验收通过，并已 reviewed / merged 到 `main`（merge commit `3271697fbc5c3d3385fbcdbc214f4d1a9a2c6832`）。E16-10 继续负责广播恢复自动重连是否允许、retry/backoff 和 error -> stale/offline 时序。本 story 不写 session record、不做 1s sampling persistence、不新增 Room / migration、不改 records/history/trends、commands/events、engines、TimerDial、声音、震动、通知或 cue。（Reviewed / merged; real-device acceptance passed）
   - 2026-07-11 Band 9 修复后人工验收已通过：live bpm 下完整 `扫描其他设备` 窗口、未选择其他设备、scan timeout、冷启动 `未连接 + 已保存设备` 与用户点击后的 exact identifier reconnect 均符合 E16-9b 语义。最终 review 已通过并合入 main；不进入 E16-10。（Reviewed / merged; real-device acceptance passed）
32. E16-10a：Heart-rate freshness / offline / reconnect policy planning。主管理已批准方案 B、10 秒 live stale、15 秒 waiting stale、30 秒 notify-abnormal、2/5/10 秒退避、最多 3 次、每次 10 秒 watchdog。有限 direct GATT reconnect 只对当前前台进程、本次已 live bpm 的同一 runtime target 生效，不 scan、不自动换 target；冷启动、回到前台、蓝牙恢复、权限重新授予和 retry 耗尽后均不自动创建 retry queue，不自动 scan/connect，也不恢复旧 attempt。权限丢失时取消 retry、stop scan、关闭 GATT并停止相关动作；重新授予后等待用户明确点击 `连接已保存设备` 或选择新设备。设置页需提供 `停止连接` 取消队列并 suppress 本前台周期自动恢复；retry exhausted 本身不产生事实状态，最近明确断开保持 `离线`，最近 connect / service discovery / CCCD / notify silence / parse 技术失败保持 `连接异常`，手动恢复文案不得改变底层状态。该 docs-only policy 已 reviewed / merged（merge commit `56d8029719889d329680f3dc099a77ae94909142`），不表示 timer/retry/backoff、UI 操作或 BLE runtime 已实现。（Reviewed / merged; policy approved）
33. E16-10b：Heart-rate freshness / foreground reconnect implementation。历史 umbrella 已由 E17 correct-course 终止，Disposition 为 `closed by correct-course / superseded by E17`。E16-10b-1 的 reviewed / merged Git 事实保留；该事实不解锁任何旧 E16 下游。（Historical / superseded）
   - E16-10b-1：Heart-rate freshness policy core。新增纯 Kotlin monotonic timeline / policy / stable reason codes，覆盖 waiting `<15s` / stale `15s..<30s` / first-sample silence `>=30s`，live `<10s` / stale `10s..<30s` / notify silence `>=30s`；valid bpm 重置 freshness，parse failure 不刷新，明确 disconnect 与技术失败事实分离，retry exhausted 不改事实，异常时间 fail closed。Story tip `09d17616f213c1df7905e46662f4a195345fdd9a` 已通过 merge commit `5cdee7ce1bd7a2b0f76f83adf069179a547fd16c` 合入 `main`。production provider/runtime 尚未消费该 policy，未实现 timer、scheduler、watchdog、callback race 或 reconnect。（Reviewed / merged）
   - E16-10b-2：Foreground reconnect controller。状态保持 `changes requested`；失败分支 `codex/e16-10b-2-foreground-reconnect-controller` immutable tip `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor，Disposition 为 `superseded by E17 / permanently prohibited from merge`。（Changes requested / permanently prohibited from merge）
   - E16-10b-3：UI mapper / settings copy。保留历史 `locked / not started`，旧路线终止。（Historical locked / not started）
   - E16-10b-4：Verification / closeout。保留历史 `locked / not started`，不得开始旧 Band 9 reconnect 验收。（Historical locked / not started）
34. E16-11：Heart-rate recording model / 1 秒持久化。旧规划不自动进入 E17。（Historical not started / superseded）
35. E16-12：Heart-rate analysis / recap。旧规划不自动进入 E17。（Historical not started / superseded）

### E17 心率子系统重新规划

1. **E17-0 Heart-rate correct-course / E16 retrospective and E17 reset**：将 E16 原始代码与文档封存为 sealed historical archive / reference only；胶囊视觉与互动作为 `adopted / frozen / direct reuse` 资产直接采用；审计失败分支并重置状态、decision、roadmap、readiness、architecture 和提示词流程。Immutable Story SHA `abce4b712139c373f534a6fabab423fe138fc29c` 已通过 merge commit `2eee72cc44c2c7733cb565ea665ebfae48610085` 合入，Story SHA 已是 `main` ancestor，E17-0 本体 Review 无 finding 并完成 merge / push。（Reviewed / merged）
2. **E17-1 Band 9 与标准 HRS 重新复验**：最终状态为 `reviewed / merged`，设备/协议结论为 `passed`。Immutable Story SHA `b7a48b980b54e34763212699c64ce387866ec064` 已通过 merge commit `17a305725a4241810ea4dbd26a29414c2be2582b` 合入并成为 `main` ancestor；E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。Review 无 blocker、must-fix 或 should-fix；持续 notify 后顶部 Stop 不易访问，以及 debug 工具 `currentGatt` callback / UI 共享状态未显式串行化，均为 debug-only nice-to-have，不扩展为 production 重构任务。（Reviewed / merged; device/protocol passed）
   - 当前 `PLU110`、Android 16 / SDK 36、HUAWEI Band 9 与 APK SHA256 `60abda376470a667ec5c94d16a24e996b2e3e7033df2cc7b4dc6d4132e8dbbc7` 的证据覆盖：广播关闭扫描无标准 HRS source；四个广播开启周期发现 `0x180D`；GATT、notify 型 `0x2A37 properties=0x10`、`0x2902`、CCCD `01 00`、连续 notify 与真实 raw payload / parser bpm 一致；Huawei Health 在广播开启时断开、广播关闭后可恢复。四周期 label / address 相同只属于本次观察，不证明永久身份；两次 `status=19` 是链路不稳定事实；Band 固件与 Huawei Health 版本未确认；最终恢复缺少额外截图，但有周期间恢复截图和用户现场观察。该结论不证明 production provider 稳定、production 架构完成、lifecycle / reconnect 正确、自动重连可用、其他环境通用；AVD 不能证明真实 BLE / GATT。
3. **E17-2 产品范围重新定义**：用户已确认完整产品合同，决策为 D-080，主文档为 `docs/planning/e17-2-heart-rate-product-scope.md`。心率默认关闭；用户显式开启后是重要训练能力，权限只在主动 scan / connect 时请求，saved device 用户点击后才有限时精确匹配，胶囊前台跨页面显示 bpm / 非医疗区间 / 上限视觉，未训练只显示不记录。训练记录 / 复盘 / 用户导出均确认价值但分独立后续 Story；D-080 当时的 manual-only / 自动恢复 defer 仅是历史范围，冲突部分已由 D-082 窄 supersede。两张划船机截图只作未来单次训练详情的信息层级与效果参考，后续须独立视觉审查。Immutable Story SHA `b50778c90cf0232b08b857fda32ba6605fbef224` 已是 `main` ancestor。（Reviewed / merged）
4. **E17-3 最小技术架构**：用户已确认方案 A，决策为 D-081，主文档为 `docs/planning/e17-3-heart-rate-minimum-architecture.md`。采用一个 Application / 进程级 `HeartRateRuntimeOwner`，以 Android main looper、generation、attempt ID 与 raw GATT identity 串行并拒绝旧 callback；permission TOCTOU 窄处理，facts / presentation 分层，cleanup 幂等，freshness 与 reconnect 解耦。D-081 是 D-027 / E7.2 的窄 FGS 例外；ID `7200` 单一 writer，非训练后台停止，零新增第三方依赖。其 no-reconnect / manual-only 冲突已由 D-082 窄 supersede；D-081 的 owner、identity、cleanup、FGS与notification安全继续有效。Immutable Story SHA `b09ed116558eb3537fc86985b9c39b96bbbca6ff` 已通过 merge commit `1e0a7a9cf0b118ca829a5843d066795b4420eb5f` 成为 `main` ancestor。（Reviewed / merged）
5. **E17-4 Implementation readiness**：readiness=`passed`；immutable SHA `1ea67561b4866aa76c41b854da74da85c208aa25`，merge commit `4b354f5116bbf7f7610e79845210d481c839fed6`。详细后续计划唯一来源为`docs/planning/e17-4-heart-rate-implementation-readiness.md`。（Reviewed / merged）
6. **E17-5 Facts / freshness / presentation core**：immutable SHA `959146a7e41a38d654b4988ba0d443f2aea0d874`，merge commit `bfb065b92d2ec78ca794fa679f7e25e85093bc79`；provisional foreground waiting/live=`3000 / 2500 ms`。（Reviewed / merged）
7. **E17-6 Deterministic Android BLE runtime owner**：immutable SHA `f9188c09275cd01dbf182823b3886635b17105bc`，merge commit `503d3151d731565837ab76f44fbebc25bb982e0d`。新owner已独立Review但production/debug实例化仍为0，旧provider/scanner/DTO继续production可达。（Reviewed / merged）
8. **E17 D-082 自动恢复 / 个人参数 Correct-course**：docs-only 对齐 eligibility、persistent suppression、active-training background retain/recovery、个人参数、冻结区间资产、Story / AC / evidence 与 merge-stable truth；禁止 candidate `fda5f7cfd3c31af3399dfe231733ea00467a68e8` 永久不得合并且不是 prerequisite。（按下方条件式真值判定）
9. **E17-7a Reconnect + Parameter Foundation**：扩展 E17-6 owner 的纯 policy / facts / tests，交付 eligibility、有间隔 bounded windows且长期 armed、typed stop reasons、persistent suppression、age `1..130`、personal max / alert `30..260`、effective max、未取整六区间与strict alert优先；仅 effective max 为 none 时bpm-only，personal max或age-derived max单独有效均可计算区间，alert独立且不是第七区间；不接Application UI、FGS或Band claim。（Planned / prerequisite-gated）
10. **E17-7b Application / settings / capsule production wiring**：由`TrainFlowApplication`唯一创建owner，原子切换Activity / Compose / settings / saved-device / capsule并退休旧runtime；debug Activity只能观察同一owner或成为无资源说明页。必须区分disconnect / reconnect / clear target / opt-out，验证foreground自动恢复、非训练background cleanup、AVD lifecycle与Band 9 basic gate；不改冻结capsule视觉/geometry。（Planned / prerequisite-gated）
11. **E17-8 Ordinary notification coordinator**：notification业务身份使用真实workout session ID + producer token + 同session内单调`stateVersion`，plan ID不作实例唯一身份；Route dispose进入bounded detach，旧Route/旧session迟到事件不得覆盖新session，process recreation无active fact时只幂等清理旧`7200`一次。（Planned / prerequisite-gated）
12. **E17-9 Connected-device FGS / ID `7200` handoff / training-background recovery**：使用同一Application owner的debug-only observer完成五阶段measurement/final APK身份链；不得复用独立GATT工具。`handoffGeneration`与workout producer generation分离；release未确认进入`ReleaseUnconfirmed`或等价态。普通`ON_STOP`不cleanup合法训练连接；未断链回前台必须同owner/same attempt/current bpm。后台unexpected disconnect且eligible时FGS与ID`7200`writer保持active、notification显示reconnecting，只允许同owner/new generation-attempt恢复；只有停止矩阵或明确foreground no-longer-needs-FGS才demote。最终source的AVD与Band 9证据职责分层。（Planned / prerequisite-gated）
13. **E17-10 Integrated AVD / Band 9 production acceptance**：evidence-only，不是implementation Story；production files/lines/methods均为0。只允许testing/evidence文档、`.local`设备证据，以及不改变production行为的test fixture/debug harness或断言修正。任何production finding返回E17-6/7a/7b/8/9独立Repair，合入同步main后重建APK并重跑全部受影响gate，旧APK/截图/日志不得复用；E17-1和sealed E16 evidence不能替代production acceptance。（Planned / prerequisite-gated）

**D-082 Correct-course / E17-7a 统一条件式真值：** 若本Correct-course immutable SHA尚未通过独立Review，或尚未完成`--no-ff` merge/push，或该SHA尚不是同步后的`main`与`origin/main` ancestor，或`main...origin/main`不为`0 0`，或本次十份文档不一致，则Correct-course=`implemented / needs review`、E17-7a prerequisite=`not satisfied`、E17-7a=`planned / prerequisite-gated`，只允许独立Review/Repair本Correct-course，不得启动E17-7a。全部条件满足后Correct-course自动为`reviewed / merged`、E17-7a prerequisite自动为`satisfied`；不需要额外docs-sync，不创建递归closeout，主管理从Git解析最终SHA与merge事实。Git ancestry是merge事实；branch name仅为locator，不是merge事实。

前台、非训练后台返回与活跃训练后台自动恢复已由D-082绑定E17-7a / E17-7b / E17-9，不再是无目标的未来候选。
32. E11.3：放弃首版心率显示、录入和统计；撤下执行页心率卡片、手动输入、历史心率占位和 debug smoke 入口，仅保留未来模型边界。（Implemented）
31. E12.1：真实记录与基础统计。（Implemented）
32. E12.2a：非心率历史图表与聚合趋势。（Implemented）
33. E12.3：历史记录清理。（Implemented）
34. E12.2c：计时同类阶段 / 轮次与额外休息趋势。（Implemented）
35. E12.2b：力量同类 set 趋势。（Implemented）
36. E12.4：Records / trends polish planning and visual gate；审计现有记录页数据能力、legacy/v2 timed composition 和 strength trend 语义，提出 records IA、chart axis / legend / empty state 和 implementation split。（Planning / visual gate complete）
37. E12-1：Records data semantics + v2 interpretation foundation；history / record mapper 可解释 timed composition v2 stageGroup / target / boundary rest / rest extension，v2 timed trend key 与 legacy timed key 分离，strength comparable trend 不变。（Implemented; verified）
38. E12-2：Records IA / chart UI polish；记录页重排为概览摘要 -> 筛选区 -> 最近训练 -> 选中详情 -> 趋势区，图表补 X/Y 轴、单位、Legend、空/不足状态，recent/detail 可区分 completed / abandoned / skipped / pause / rest extension、legacy timed、timed composition v2 与 strength。（Implemented; verified）
37. E13：E13.1 声音提醒与音频共存已实现；E13 sound cue asset / audio coexistence audit and QA gate 已完成；E15-1 已修复力量休息最后 N 秒 beep 与 `auto_after_rest` 休息后自动开始回归；E15-1a review fix 已将 `auto_after_rest` 休息自然结束后自动进入下一组 active set 的 `STAGE_BELL` 请求限定到 tick 驱动的 rest -> active set 路径，初始 prepare、手动开始、提前开始和 `manual_start` prepare 路径均不响铃；E15-1b 计划补齐力量计划编辑页中的计划级本组计时模式设置，使旧计划可通过显式编辑保存切换 `manual_start` / `auto_after_rest`，但全局训练偏好仍不运行时覆盖旧计划。蓝牙 / 扬声器真机 smoke 仍待继续补证据，固定女声 cue、力量完成 bell 和力量 haptics parity 留给后续。
38. E14.4-2b：Timed composition editor and TimerDial ring semantics visual / semantic gate + E14.4-2b-1 visual prototype / mock + E14.4-2b-2 data model decision retained；E14.4-2b-3 restart serializer / model and editor adapter foundation implemented；E14.4-2b-4 editor UI visual/code gate implemented；E14.4-2b-5 engine timeline planning gate docs-only complete；E14.4-2b-5a timeline adapter model/tests implemented adapter-only and pushed as `6888e31`；E14.4-2b-5b engine integration planning gate docs-only complete；E14.4-2b-5b-1 engine adapter bridge tests added as a test-first red gate；E14.4-2b-5b-2 minimum engine bridge implemented；E14.4-2b-5b-3 v2 start gate enablement + smoke implemented；E14.4-2b-5c session record compatibility tests / smoke review implemented；E14.5 TimerDial continuous progress fix complete；E14.4-2b-6 TimerDial mapping planning gate docs-only complete；E14.4-2b-6a TimerDial mapping model/state tests complete；E14.4-2b-6b production mapping implementation complete；E14.4-2b-6c smoke / visual QA review gate complete；E14.4-2b closeout complete。
39. E14.6：Real-device TimerDial feedback planning gate，拆出 E14.6-1 progress rebound fix、E14.6-2 completion recap page redesign、E14.6-3 stage style system planning / design。（Planning complete）
40. E14.6-1：TimerDial progress rebound fix，修复 normal motion 下一秒 tick anchor handoff 造成的 active ring / active segment forward-then-back rebound。（Implemented; followed by E14.6-2 planning）
41. E14.6-2：Completion recap page redesign planning / visual gate，确认 completed 终态进入“本次数据统计复盘页面”：顶部克制庆祝 + `已完成`，中部复用现有 summary / recap / session 数据，底部主动作推荐 `返回训练首页`，不保留大 TimerDial 作为主视觉，不改 session record 语义。（Planning complete; E14.6-2b implemented）
42. E14.6-2b：Completion recap page Compose implementation，计时训练 completed / abandoned 终态进入独立复盘 shell，复用现有 summary / recap 内容并完成 Android smoke。（Implemented; E14.6-2c reviewed evidence）
43. E14.6-2c：Completion recap smoke / visual QA review gate，复查 E14.6-2b smoke / UI tree / visual QA / boundary。UI tree 语义与交互覆盖可用，但截图证据损坏且补证据当前截图全黑，因此该 gate 未收口 screenshot-level visual QA。（Review complete; resolved by E14.6-2d）
44. E14.6-2d：Completion recap screenshot evidence recapture，诊断 2b PNG binary corruption 与 2c current screenshots black issue，使用 binary-safe screencap + pull 补齐 completed recap top / summary / details、bottom return 和 abandoned shell 有效非黑屏截图证据。（Completed; followed by E14.6-3 planning）
45. E14.6-3：Stage style / icon planning，确认阶段样式由颜色和内置 `iconKey` 组成，热身 / 放松 / 轮间休息 / 普通阶段 / 阶段内目标均可解析 style，轮数不需要颜色或 icon，第一版只使用项目内置白色 icon key，用户上传图片列为 post-MVP。（Planning complete）
46. E14.6-3a：Stage style data contract / model decision，选择 Option A：复用 composition v2 stage group / target 现有 `colorHex` / `iconKey`，只在 versioned timed composition JSON payload 中增加 `warmupStyle`、`cooldownStyle`、`restBetweenRoundsStyle`，不做 Room migration，legacy plans 和 old snapshots 通过 resolver defaults 有效。（Decision complete; E14.6-3b implemented）
47. E14.6-3b：Stage style model / serializer tests，将 boundary style payload 落到 Kotlin model、plan / snapshot JSON serializer 和 focused tests，验证 valid / invalid color、known / unknown icon、asset-like icon rejection、v2 plan / session snapshot round-trip、old v2 missing fields、legacy JSON unaffected、Room unchanged 和 no resources / no UI / no TimerDial boundary。（Implemented; followed by E14.6-3c）
48. E14.6-3c：Editor style picker UI，把 E14.6-3b payload 暴露到 timed composition editor，支持 warmup / cooldown / restBetweenRounds、stage group 和 target 的颜色 + 内置 iconKey 选择；rounds 无 style；复用色板、使用 Compose Canvas 内置 icon grid，不新增资源 / 上传路径 / TimerDial consumption / Room 变更。（Implemented; followed by E14.6-3d）
49. E14.6-3d：TimerDial style consumption / visual QA，把 saved v2 stage style 消费到 TimerDial 外圈颜色和中心白色 built-in Canvas icon，保持 planned duration ratio、boundary fallback、legacy fallback、rest extension monotonic progress 和 continuous progress identity。（Implemented; followed by E14.6-3e）
50. E14.6-3e：Stage style / TimerDial visual QA closeout，复查 E14.6-3d smoke 的 PNG / XML / logcat / scan 证据，确认 editor picker、TimerDial style consumption、boundary fallback、legacy timed、rest extension、continuous progress、资源 / 上传 / heart-rate 禁区均无阻塞；非阻塞 follow-up 仅保留 reduce-motion style-specific smoke、3 / 4 target 截图、dedicated legacy screenshot、显式 adb-after artifact 和承托环厚度 polish。（Review complete; followed by E12 records / trends polish planning gate）
51. E15-3：Stage icon semantic clarity，优化 timed composition editor picker 与 TimerDial 阶段 icon 语义；用户后续确认生成图资源方案后，既有内置 key 映射到项目打包的白色 PNG 资源，覆盖热身、动作进行、加速、冲刺、普通休息、轮间恢复、放松、力量、灵活和自定义。picker 使用 4 列图标 + 中文 label 和语义 content description，默认新草稿 sprint 目标使用既有 `sprint` key，保持旧 key / saved plan / session snapshot 兼容，不保存图片路径或上传资产。（Merged into main）
52. E15-4：Strength confirm-record UI collapse，在力量训练确认本组阶段折叠上方当前组 / 时间数据大卡片为紧凑摘要，保留动作名、当前组序号 / 总组数、完成耗时或暂停状态、计划目标摘要和正式组 / 热身组标签；review fix 已压缩确认卡片并把感受选择提前到实际输入之前，让 720x1280 下四个感受选项、实际重量 / 次数和固定底部主动作同时可见；prepare / active / rest 阶段保持既有行为和视觉边界。（Reviewed / merged）
53. E15-5：Real-device polish planning gate，基于用户 2026-07-03 真机截图拆分短 target TimerDial 证据先行修复、力量本组计时模式 selector 防溢出、力量完成页固定返回动作、力量编辑 / 执行减法四个后续任务；E15-5d 后续按 2026-07-04 反馈收窄为删除目标组颜色占位和力量执行短提示。（Planning complete）
54. E15-5a：TimerDial short-target motion diagnostic + fix gate，复现 ready gate route clock 旧 delay 提前 tick 新 1s / 2s target 的真实原因，并通过 manual command anchor 重置 tick 相位；baseline / fixed / skip / reduce-motion evidence 已复核。（Reviewed / merged）
55. E15-5b：Strength set timer mode selector layout polish，将力量计划编辑页本组计时模式从横向 chip 改为竖向 radio-card selector，解决 720x1280 长中文溢出，保持 `StrengthSetTimerMode` 保存语义不变。（Reviewed / merged）
56. E15-5c：Strength completion sticky return action，将力量 completed / abandoned 终态返回主动作固定到底部，复盘内容预留 padding，避免用户滑到长复盘底部才能返回。（Reviewed / merged）
57. E15-5d：Strength editor and execution simplification，删除力量计划编辑页目标组颜色占位入口并重排目标组输入，删除力量执行页当前组短提示并让主卡自然收缩，下一组卡压缩为动作 / 组序号 / 重量 / 次数摘要；不新增颜色字段或数据持久化。（Reviewed / merged；APK 测试通过）

## 7. 下一轮建议

当前状态说明：

```text
E6.1 跟练雏形计划入口已合入 main。
E6.2 基础跟练执行页已合入 main。
E6.2 只支持 E6.1 内存态 preset 启动；E10.1 后，O-002 已收敛为跟练后续通过统一动作选择页编排，不再依赖计时计划切换为跟练视图。
E6.3 心率抽象状态曾合入 main；E11.3 后生产执行页已不再显示心率位，仅保留未来模型边界。
E7.1 训练提醒通知已合入 main。
E7.2 活跃训练通知边界已合入 main。
E7.3 训练偏好设置已合入 main。
E7.3 偏好回归保护已合入 main。
E8.1 内置 UI 皮肤 contract / registry 已合入 main。
E8.2 Tile Flow 关键页面磁贴式皮肤已合入 main。
E8.3 Big Type 大字训练皮肤已合入 main。
E8.4 UI skin review checklist 和用户测试前 UI readiness 已合入 main。
E9.1 训练状态恢复与回归测试基线已合入 main。
E9.2 权限与隐私文案已合入 main。
E9.3 MVP 验收清单已合入 main，记录用户测试前能力状态、问题分级、数字输入清空 Bug、编辑页开始按钮状态和 E10/E11/E12 后续方向。
E9.4 User Test Fix Pack 1 已合入 main，修复计划编辑页数字输入临时清空、计时编辑页立即开始、力量编辑页开始训练，并把历史记录全部 / 按计划 / 按日期清理登记为后续能力。
E10.1 已记录训练模式边界与执行页交互原则：计时训练回归纯间歇计时器，跟练/力量后续使用统一动作选择页，三类执行页遵守主操作即时可达原则，并把记录、健康数据边界、统计、声音和固定 cue 分流到 E10.4/E11/E12/E13。
E11.1 与 E16 已合入 Story 继续保留历史 Git 事实；E16 umbrella 已由 correct-course 关闭并被 E17 supersede，E16 原始代码和文档为 sealed historical archive / reference only。E16-10b-2 failed tip `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor，永久禁止合并；b3 / b4 旧路线终止，E16-11 / E16-12 不自动进入 E17。浮动胶囊视觉与互动为 `adopted / frozen / direct reuse`，runtime、provider state、mapper、文案、优先级和 wiring 不冻结。E17-0至E17-6已reviewed/merged；D-082 Correct-course与E17-7a prerequisite按本页统一条件式真值自动判定。
E10.2 已完成计时训练纯阶段编辑页和大圆盘执行页首版实现。
E10.3 已完成力量 / 跟练执行页主操作可达性修复。
E10.4 已完成训练记录闭环前置并合入 main，计时 / 力量 / 基础跟练 completed 与 abandoned 终态可写入本地 Room session records，记录页生产入口读取真实本地记录。
E10.5 已记录 Timer Dial 设计工作流与重构范围：外部 APK / 截图只做 UI / 交互研究，不复制代码、资源或资产；工具路线为 Figma 静态规格、可选 HTML / Canvas 动效验证、Jetpack Compose Canvas 生产实现，Rive / Lottie 仅用于小图标或装饰动效；Timer Dial 规格包含顶部总剩余时间、外圈当前运动+休息周期、内圈总进度、中心圆当前阶段和底部少量图标操作；后续拆为 E10.6 / E10.7 / E10.8，E12 统计和 E13 声音保持独立。
E10.6 已记录 Timer Dial Figma / static visual variants：主文档为 `docs/planning/timer-dial-static-visual-variants.md`，覆盖 Official Flow 执行页状态帧、计时编辑页关键状态帧、Tile Flow / Big Type 适配、互动动画语义、小屏 / 无障碍检查、法律边界和 E10.7 handoff。E10.6 只改 Markdown / 设计文档，不实现 Android、不写 Kotlin、不改 Gradle、不改 prototype、不复制 APK 资产或动效参数、不新增第四套 skin、不混入 E11/E12/E13。
E10.7 已实现 Timer Dial Compose prototype：`feature.workoutsession` 新增 Timer Dial UI state / visual tokens / Canvas component / preview demo，低风险接入计时执行页，展示外圈阶段结构、当前阶段推进、内圈总进度、中心自绘阶段符号和 paused / final countdown 状态；新增 state/tokens/semantics 单元测试。E10.7 仍是 prototype，不是最终生产集成，不改 Room/session repository、engine 语义、声音、统计、心率设备或第四套 skin。
E10.8 已实现 Timer Dial production integration / animation polish：计时训练生产页默认使用 Official Flow Timer Dial；外圈只展示当前一次运动+休息周期，内圈展示整次训练总进度；中心圆负责暂停 / 继续，底部跳过和结束使用图标，结束仍需二次确认，`+15秒` 仅延长当前休息 15 秒。已完成 unit / assemble / lint / check 和 720x1280 emulator active / paused / rest smoke；最后 N 秒视觉截图窗口仍留作 review 关注点。
E10.9 已实现 Timer Dial reference polish / continuous progress / user-test APK：`r-design.md` 作为参考桥接文档纳入分支；Timer Dial active 状态下用 Compose frame clock 做最多当前 1 秒的连续进度投影，文案数字仍按秒更新；paused / completed / abandoned 不推进；`+15秒` rest extension 后进度不倒退；production controls 仍是 skip、`+15秒`、end。E10.9 是 Timer Dial 参考风格与连续动画 polish，不进入 E11/E12/E13。
E10.10 已完成计时/力量计划本地持久化和保存入口真实可用性检查；E10.11 已使用 `huashu-design` 做 3 个 HTML 高保真 Timer Dial 原型方向；E10.12 已将 E10.11 `TrainFlow Official Fusion` 方向落到 Android Compose 生产 Timer Dial：处理视觉减字、总剩余时间居中放大、圆盘放大、线条层级、底层宽圆环、动态浅点和中心圆简化，并保留 E10.9 continuous progress、pause freeze、terminal freeze 和 rest extension monotonic progress；E10.13 已实现 Ready Start Gate，计时训练从编辑页或计划详情进入后先显示极简启动界面，点击中心圆才真正 `StartSession`，ready 状态不 tick、不触发 feedback、不写 abandoned；E10.14 已实现并收口 Rest Extension Semantics And Recording，`+15秒` 只延长当前休息阶段，不插入新阶段、不改计划、不污染暂停时长，生产 UI 使用二段式确认、2 秒确认窗口、确认成功短反馈和每个 rest step 4 次 / 60 秒上限，并将每次确认成功的额外休息保存为真实 session record；E10.15 已定义 motion timing rules 和集中 token，明确触摸反馈、状态切换、局部布局、页面切换、continuous projection、可中断和 reduce-motion 边界，且不让动画驱动 engine / records / commands / events；E10.16 已将 motion token 最小落地到计时训练 ready gate、center dial、Timer Dial marker / ring / center color 状态变化和 `+15秒` 二段确认反馈，并补齐生产 reduce-motion source，reduce-motion 时 ready/execution snap、非必要 scale / pulse 关闭、Timer Dial continuous projection 不启动 frame loop，同时保持 ready/start、pause/resume、rest extension、session record 和业务语义不变；E10.17 已完成 Stage Color Picker，计时阶段编辑页可从推荐色 / 更多颜色中选择阶段色，保存后通过本地计划持久化恢复并被 Timer Dial 外圈 / 中心圆消费，非法色回退阶段默认安全色，选中态包含对勾、外圈和 TalkBack 语义；E10.18 已完成 Plan Edit Backfill，计划详情可进入计时 / 力量编辑器并回填已保存计划，保存回同一个本地 plan id，保留原 reminder / preferences，并对编辑保存后的 reminder 执行取消 + 重调度或清理，跟练不暴露假的完整编辑入口，既有 `WorkoutSession.planSnapshot` 不回写；E12.1、E12.2a、E12.3、E12.2c、E12.2b、E12-1 和 E12-2 已覆盖真实基础统计、非心率聚合趋势、历史清理、计时同类阶段 / 额外休息趋势、力量同类 set 趋势、timed composition v2 记录解释和记录页 IA / chart UI polish；E13 处理 `countdown_beep1.mp3`、`.local/audio/stage_bell_copper_clean.mp3`、蓝牙耳机/扬声器 smoke、媒体音量通道和不抢占外部音乐视频；首版不再规划平均心率趋势。
E14.6 已完成 real-device TimerDial feedback planning gate：E14.6-1 已修复 normal motion 外圈 / active segment progress rebound；E14.6-2 completion recap page redesign planning / visual gate 已完成 docs-only 规划；E14.6-2b 已实现计时训练 completed / abandoned 独立复盘页面，复用现有 summary / recap / session 数据，底部主动作 `返回训练首页`，不保留大 TimerDial 作为完成页主视觉，不改 session record 语义；E14.6-2c 已完成 docs-only smoke / visual QA review，UI tree 语义与交互覆盖可用但 screenshot-level visual QA 因证据质量未收口；E14.6-2d 已补采有效非黑屏截图并关闭该证据阻塞；E14.6-3 已完成阶段样式 / 内置白色图标系统规划；E14.6-3a 已完成 stage style data contract / model decision，boundary style 字段只进入 versioned composition JSON，不做 Room migration；E14.6-3b 已完成 model / serializer / focused tests；E14.6-3c 已完成 editor style picker UI；E14.6-3d 已完成 TimerDial style consumption / visual QA；E14.6-3e 已完成 stage style / TimerDial visual QA closeout；E15-3 已按用户后续确认把内置阶段 icon 升级为项目打包的白色 PNG 资源，保持旧 `iconKey` 兼容且不保存图片路径或上传资产；E15-4 review fix 已折叠并进一步压缩力量确认本组阶段的当前组摘要 / 确认卡片，把感受选项提前到实际输入之前，让 720x1280 下四个感受选项与固定底部主动作共同可见；E15-5a / 5b / 5c / 5d 已完成短 target route clock、力量 selector、completion sticky return、力量目标组颜色占位删除和执行短提示删除等用户反馈收口；E15-5d 已 review / merged，merge commit `0fa28463e4c24bf039944402a209f8f55c922c1b`，story commit `d9875bd48cd3e51b560c677efc3f6d4440efc89a`，用户 APK 测试通过；E15 维护入口为 `docs/testing/e15-maintenance-lessons-learned.md`。E16 heart-rate broadcast retest 已 review / merged，merge commit `bbd4296`。下一步建议进入 MVP Alpha readiness 前检查；若用户继续提供真机问题，则进入 User Test Fix Pack 2。
```

下一轮建议按 E15 收口后的发布准备优先级进入：

```text
E15 系列已收口并保留原历史事实。E16 系列现为 `closed by correct-course / superseded by E17`，原始代码和文档为 sealed historical archive / reference only；已合入 Story 的 merge fact 保留，但不自动成为 E17 acceptance。E16-10b-2 `89d1e23f870185a2e279d35bb293883f64fe70ba` 永久禁止合并。胶囊视觉与互动为 `adopted / frozen / direct reuse`，runtime、provider state、mapper、文案、优先级和 wiring 不冻结。E17-4/5/6已reviewed/merged，E17-4 readiness=`passed`；D-082 Correct-course与E17-7a prerequisite按本页统一条件式真值自动判定，条件未全部满足时只允许独立Correct-course Review/Repair。
```

E10.15 Motion Timing Rules 回看重点：

1. Motion token duration 是否仍落在 story 约定范围，且命名不混淆触摸反馈、状态切换、局部布局和页面切换。
2. reduce-motion fallback 是否明确为 snap / disable non-essential motion / disable continuous projection。
3. Timer Dial continuous projection 是否仍最多 1 秒，paused / completed / abandoned / ready 未启动状态不推进。
4. 动画是否仍只消费 UI state / engine state / `WorkoutEvent`，不派发或改变 `WorkoutCommand`、engine state、session record、`pausedElapsedSec`、extra rest 或 total elapsed。
5. `+15秒` 二段确认、rest extension monotonic progress、pause freeze 和 terminal freeze 是否不回归。
6. E10.15 不混入 E10.16 Motion Landing、页面切换动画落地、Stage color picker、声音播放、E12 统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 prototype 前端行为改造。

## 8. 暂缓事项

以下事项不进入 E10.1/E10.2/E10.3 当前实现范围；只有进入上文明确拆出的后续 story 或更新决策日志后才能实施：

- E10.1/E10.2/E10.3 当时未包含真实心率设备接入；D-080已将显式opt-in后的标准HRS与冻结胶囊显示纳入当前E17产品范围，D-082已把exact-target自动恢复、显式断开suppression与个人参数绑定到E17-7a / 7b / 9。E17-4/5/6已完成，本Correct-course / E17-7a prerequisite按本页统一条件式真值自动判定；其他设备路线及记录、分析、导出仍需独立后续Story。
- Health Connect 历史数据读取，需进入 E11 或独立健康数据阶段。
- 用户任意文本 TTS、语音读秒大范围能力和自动语音教练。
- AI 实时动作纠错。
- 课程运营后台。
- 云同步和账号系统。
- iOS 工程。
- 运行时插件市场和远程主题下载。
