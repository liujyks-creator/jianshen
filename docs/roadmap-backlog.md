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

**文档状态:** 首版拆分草案  
**用途:** 将 PRD、UX、数据契约和 Android 架构拆成可执行里程碑、Epic、Story 与验收顺序。  
**范围:** Android MVP 与当前 React/Vite 原型承接；不包含后续商业化、云同步、完整课程平台、真实可穿戴设备接入。

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
| `FR-065` 到 `FR-068` | 心率展示位和抽象状态。 |
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
| M5 跟练雏形与心率占位 | 完成跟练雏形视图、媒体位、心率抽象状态展示。 | 跟练页不是空壳，心率占位不影响训练。 |
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
| E6 | 跟练雏形与心率占位 | M5 |
| E7 | 通知、声音、震动与偏好 | M2-M6 |
| E8 | 设计系统、UI Shell 与开源定制边界 | M0-M6 |
| E9 | MVP 验收与发布准备 | M6 |

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
- 支持全局 `PlanPreferences.cueSettings` 与 `TimedExerciseItem.cueSettings`，item 级提醒覆盖全局提醒；大于动作/休息时长的阈值会被忽略，避免短时长边界重复触发。
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
- Then 心率展示位保持辅助层级。

**交付结果:**

- Android 侧新增 `feature.workoutsession` 计时训练执行 UI state mapper 和 Compose route/screen，页面使用深色训练执行面板展示当前动作或休息、主倒计时、步骤/轮次进度、下一步、动作短提示和辅助心率占位。
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
- Compose 执行页在动作或休息进入提醒窗口时强化倒计时颜色、面板边框和短促提示文案；动作提醒与休息提醒在标签和文案上区分，心率占位仍保持辅助层级。
- 新增 `core.media` 反馈分发边界，根据 `WorkoutEvent` 与 `CueSettings` / `CountdownCue` 生成声音、震动和强化动画请求；Android route 仅做薄 in-app 声音和触感消费，不接通知或前台服务。
- 新增单元测试覆盖动作提醒、休息提醒、cue 开关、阈值关闭、短时长阈值忽略和事件驱动反馈请求。
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

作为用户，  
我想看到力量训练计划与实际差异，  
以便回顾表现。

**验收标准:**

- Then 展示动作、组数、重量、次数、组耗时、实际休息。
- Then 展示计划与实际差异。
- Then 不自动给出加重量建议。

### Story E5.3: 训练历史与基础趋势

作为用户，  
我想查看历史训练记录和基础趋势，  
以便复盘和调整计划。

**验收标准:**

- Then 支持按训练日期查看历史。
- Then 支持单动作重量/次数历史。
- Then 支持训练容量历史。
- Then 不使用医疗或过度结论文案。

### Story E5.4: 基础恢复建议

作为用户，  
我想根据本次训练部位获得放松建议，  
以便训练后知道该恢复哪里。

**验收标准:**

- Then 根据训练动作的肌群映射恢复区域。
- Then 展示推荐放松区域和基础说明。
- Then 不做康复治疗或医疗诊断表述。

## Epic E6: 跟练雏形与心率占位

目标：交付可用但克制的跟练雏形，并保留心率展示状态。

### Story E6.1: 跟练雏形计划入口

作为用户，  
我想选择一个基础跟练流程，  
以便体验动作演示和流程提示。

**验收标准:**

- Then 至少有一个基础跟练计划或可跟练计时计划。
- Then 入口不暗示完整课程平台。

### Story E6.2: 跟练执行页

作为用户，  
我想在跟练页看到当前动作、演示位、倒计时、短提示和下一动作，  
以便跟着流程训练。

**验收标准:**

- Then 跟练复用计时训练引擎。
- Then 展示媒体位，即使首版媒体为空也不显得像坏掉。
- Then 支持暂停、跳过、动作详情和结束。

### Story E6.3: 心率抽象状态展示

作为用户，  
我想训练页可以显示心率状态，  
以便未来接入设备时不用改训练流程。

**验收标准:**

- Then 支持 disabled、not_connected、connecting、available、stale、error 状态。
- Then 没有设备时训练闭环完整可用。
- Then 不显示医疗级告警结论。

## Epic E7: 通知、声音、震动与偏好

目标：建立首版提醒能力和可配置偏好。

### Story E7.1: 训练提醒通知

作为用户，  
我想收到训练计划提醒，  
以便按计划开始训练。

**验收标准:**

- Then 可为计划设置提醒时间。
- Then 通知权限关闭时 App 有清楚提示。
- Then 首版不承诺闹铃级强提醒。

### Story E7.2: 活跃训练通知边界

作为用户，  
我想训练进行中离开 App 时仍知道训练状态，  
以便不中断训练。

**验收标准:**

- Then 明确是否首版启用前台服务。
- If 启用，Then ongoing notification 显示当前训练摘要。
- Then 不把通知逻辑写入训练引擎。

### Story E7.3: 训练偏好设置

作为用户，  
我想设置提醒秒数、声音、震动和动画开关，  
以便训练反馈符合偏好。

**验收标准:**

- Then 可设置默认临近结束秒数。
- Then 可开关动作提醒、休息提醒、声音、震动、强化动画。
- Then 力量训练本组计时默认模式可设置。

## Epic E8: 设计系统、UI Shell 与开源定制边界

目标：让官方默认 UI 优雅、克制、专业，同时让开源社区可以定制主题、首页布局和按钮位置，而不破坏核心训练引擎。

### Story E8.1: 建立 DESIGN.md 设计系统

作为设计维护者，  
我想建立机器可读和人可读的设计系统，  
以便官方 UI 和 AI coding 都有稳定风格锚点。

**验收标准:**

- Then 根目录存在 `DESIGN.md`。
- Then 设计系统包含颜色、排版、间距、圆角和关键组件 token。
- Then 明确训练执行页、力量确认层、动作库和恢复建议的设计规则。

### Story E8.2: 建立官方 UI shell 边界

作为开发者，  
我想把官方 App shell 与核心训练逻辑分离，  
以便后续社区 UI 可以替换页面组合而不改核心引擎。

**验收标准:**

- Then 架构中存在 `ui:designsystem`、`ui:theme`、`ui:shell-official` 边界。
- Then shell 通过 feature ViewModel/use case 调用能力。
- Then shell 不直接写入 `WorkoutSession`。

### Story E8.3: 开源 UI 定制指南

作为开源贡献者，  
我想知道哪些 UI 能改、哪些核心不能改，  
以便安全地贡献主题和布局。

**验收标准:**

- Then 存在 `docs/ui-extension-guide.md`。
- Then 明确可定制主题、首页、按钮位置、页面组合和训练布局。
- Then 明确不能改变 `WorkoutCommand`、`WorkoutEvent`、训练执行引擎和数据契约。

### Story E8.4: 社区主题和布局审查清单

作为维护者，  
我想有一份 UI 贡献审查清单，  
以便保证社区 UI 不牺牲可读性、权限说明和训练稳定性。

**验收标准:**

- Then 审查清单覆盖训练可读性、对比度、主按钮、心率表述、未实现能力和引擎边界。
- Then 主题贡献必须说明目标用户、token 映射和训练执行页表现。

## Epic E9: MVP 验收与发布准备

目标：完成首版质量门，准备小范围试用。

### Story E9.1: 训练状态恢复与回归测试

作为开发者，  
我想验证训练中暂停、后台、返回和异常退出恢复，  
以便减少真实训练中断风险。

**验收标准:**

- Then 关键训练状态有单元测试。
- Then 关键 ViewModel 有测试。
- Then 后台或重建后的恢复策略已验证。

### Story E9.2: 权限与隐私文案

作为用户，  
我想理解通知、震动、心率占位和健康数据边界，  
以便放心使用。

**验收标准:**

- Then 通知权限用途清楚。
- Then 心率和热量不被描述为医疗结论。
- Then 未接入设备时不请求健康数据权限。

### Story E9.3: MVP 验收清单

作为产品负责人，  
我想逐条验收 MVP 功能，  
以便判断是否进入试用。

**验收标准:**

- Then PRD 10.1 到 10.4 的验收标准逐项有结果。
- Then 首版非目标没有被静默实现或暗示。
- Then 已知问题分级记录。

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
| FR-065 到 FR-068 | E6.3 |
| FR-070 到 FR-081 | E5.1, E5.2, E5.3, E5.4 |
| UI 定制与设计系统 | E8.1, E8.2, E8.3, E8.4 |

## 6. 推荐实施顺序

1. E0.1 到 E0.4：先建 Android 工程与核心地基。
2. E1.1 到 E1.4：动作库和内容基础。
3. E2.1 到 E2.4：计划创建与管理。
4. E3.1 到 E3.4：计时训练闭环。
5. E4.1 到 E4.4：力量训练闭环。
6. E5.1 到 E5.4：总结、历史、恢复。
7. E6.1 到 E6.3：跟练雏形与心率占位。
8. E7.1 到 E7.3：提醒、声音、震动、偏好。
9. E8.1 到 E8.4：设计系统、UI shell 和开源定制边界。
10. E9.1 到 E9.3：硬化与验收。

## 7. 下一轮建议

当前状态说明：

```text
E5.1 计时训练总结已在 codex/e5-1-timed-session-summary 实施，等待 Review Gate。
E5.1 summary 消费 E3.4 history 边界，不写真实 WorkoutSession，不实现历史趋势或完整恢复建议。
当前无已知 blocker。
```

下一轮建议进入：

```text
Story E5.1 Review Gate
```

E5.1 Review Gate 建议重点确认：

1. completed / abandoned 终态是否都能看到 summary。
2. summary 是否覆盖总时长、完成阶段、轮次进度、跳过内容和休息延长情况。
3. active elapsed 文案是否足够清楚，未伪装为真实 wall-clock `startedAt` / `endedAt`。
4. 恢复建议入口是否保持 E5.4 占位，没有暗示完整建议已生成。
5. 是否保持不接真实持久化、通知、声音、震动、真实心率、语音、跟练闭环或力量训练总结。

## 8. 暂缓事项

以下事项不进入当前 MVP backlog，除非决策日志更新：

- 真实心率设备接入。
- Health Connect 历史数据读取。
- 语音读秒和自动语音教练。
- AI 实时动作纠错。
- 课程运营后台。
- 云同步和账号系统。
- iOS 工程。
- 运行时插件市场和远程主题下载。
