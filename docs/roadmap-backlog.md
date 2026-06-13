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
| E10 | 训练模式边界与执行页交互修正 | 用户测试后续 |
| E11 | 手动心率输入与设备接口策略 | 用户测试后续 |
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

## Epic E6: 跟练雏形与心率占位

目标：交付可用但克制的跟练雏形，并保留心率展示状态。

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
- 页面展示当前动作、演示/媒体占位、倒计时、阶段进度、动作短提示、下一动作预告、低层级心率占位、控制按钮和基于 fixture 的动作详情。
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

- `feature.workoutsession` 新增共享 `HeartRateDisplayUiState` / mapper，计时训练、力量训练和基础跟练执行页统一消费 `HeartRateState`。
- 支持 disabled、not_connected、connecting、available、stale、error 六种 `HeartRateAvailability`，available 可显示 bpm。
- measuredAt、sourceId 和 message 只进入低层级辅助文案；`warningLevel` 继续只作为模型字段保留，不驱动颜色、告警、训练规则、训练状态或主控按钮。
- `core.health` 新增 `HeartRateProvider`、`DisabledHeartRateProvider` 和 `MockHeartRateProvider` 边界，仅输出抽象 `HeartRateState`，不接真实设备或平台 SDK。
- 新增单元测试覆盖六种状态、三类执行页一致映射、available 的 bpm / measuredAt / sourceId / message、warningLevel 负向、主控不受心率状态影响、越界文案负向和 Manifest 权限负向检查。
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

- 明确首版不启用 foreground service。理由是 target 34+ 的 foreground service 需要声明匹配类型和权限；本阶段只做训练状态摘要，不适合冒用 data sync / media 类型，health 类型会牵出健康、传感器或活动识别权限，超出 MVP 禁区。
- `core.notifications` 新增 active workout notification contract、普通 ongoing channel/content、permission-gated policy 和 Android `NotificationManager.notify/cancel` 控制器。
- 计时训练、力量训练和基础跟练执行 route 从各自 UI state / engine status 映射活跃训练摘要；active / paused 时显示 ongoing notification，ready / completed / abandoned / route disposed 时清理。
- 通知文案明确为普通状态提示，不承诺后台精确计时、闹铃级提醒、锁屏强打断或医疗/危险状态提醒。
- 新增测试覆盖 active / paused 展示、permission denied 不阻塞训练、terminal 清理、三类执行页摘要映射、manifest 负向权限和不启用 foreground service / exact alarm / 健康 / 传感器 / 蓝牙 / 定位权限。
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
我想理解通知、震动、心率占位和健康数据边界，  
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
- 记录用户反馈：计时训练后续可能应更接近纯间歇计时器；基础跟练后续应承担动作选择、动作编排和推荐；动作选择应进入独立页面；记录页后续需要总统计、图表、平均心率趋势和计划调整证据。
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
- 用户反馈已分流：记录未真实闭环与历史清理进入 E10.4/E12，Timer Dial 圆盘视觉语言进入 E10.5-E10.8，手动心率输入进入 E11，真实设备进入 E11 或独立阶段，统计图表和趋势进入 E12，声音/女声 cue/音频共存进入 E13。
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
- 热身固定在编辑列表开头，放松固定在末尾，中间的工作、休息和自定义阶段可排序；执行顺序与编辑页显示顺序保持一致。计划主题色 / 整体配色编辑仍留作后续 polish。
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

**状态:** Planned from user-test feedback

作为训练计划用户，
我想自定义计时阶段、秒数、轮次、颜色和图标能真实保存并在退出后恢复，
以便计划编辑不是一次性的临时草稿。

**验收标准:**

- Then 自定义计时训练阶段、秒数、轮次、颜色、图标、名称和排序保存到本地持久化计划。
- Then 退出 App、切换页面或重新进入计划详情后，计时计划按保存内容恢复，不回到默认值。
- Then 检查计时、力量和跟练相关计划保存入口是否真实可用；不可用入口必须改为明确禁用、待实现或进入对应 story。
- Then 计划保存继续遵守 `WorkoutPlan` 存目标和结构、`WorkoutSession` 存实际执行结果和计划快照的边界。
- Then 不改训练执行引擎语义，不实现声音播放，不做统计图表，不提交 `.local`、APK、`人工/`、build 输出或截图日志。

### Story E10.11: Huashu Timer Dial HTML prototype exploration

**状态:** Planned; prototype-only, not this docs story

作为设计探索者，
我想用已安装的 `huashu-design` skill 做 Timer Dial 高保真 HTML 原型方向，
以便在不改生产代码前比较视觉方案和状态表现。

**验收标准:**

- Then 使用 `huashu-design` skill 输出 3 个 HTML 高保真 Timer Dial 原型方向：黑红高对比、TrainFlow Official 融合、赛博霓虹。
- Then 每个方向覆盖 active、rest、paused、final 5 seconds 和 rest extension。
- Then 原型只使用 TrainFlow 自己的 HTML/CSS/Canvas/SVG/图标语义，不复制外部 APK 或参考项目代码、资源、图标、字体、音频、命名、动效参数或逐像素视觉。
- Then 原型结果服务 E10.9 Review Fix 或后续视觉评审，不自动进入生产实现。
- Then 不修改 Kotlin/Gradle/prototype，不接入音频，不移动或提交根目录 APK、`.local/`、`人工/`、build 输出、截图或日志。

### Story E10.x: 后续力量训练新版 UI 设计

力量训练完整新版 UI 设计单独开启，不塞进 E10.3。该阶段可重审力量训练信息架构、确认层、历史趋势入口和高级组设置，但必须保留计划值预填实际记录、训练命令、训练事件和核心引擎语义。

## Epic E11: 手动心率输入与设备接口策略

目标：先允许用户手动录入心率，让心率进入记录和分析，同时保留真实设备接口。

### Story E11.1: 手动心率输入

作为用户，
我想在训练或记录中手动录入心率，
以便没有设备时也能保存心率信息用于回顾。

**验收标准:**

- Then 用户可手动录入心率。
- Then 手动心率进入训练记录或后续分析数据源。
- Then UI 明确手动心率不是实时设备数据。
- Then 继续保留 `HeartRateState` / provider 抽象。
- Then 不接 Health Connect、Wear OS、BLE 或厂商 SDK。
- Then 不做医疗判断、危险告警或训练中断依据。

### Story E11.x: 真实心率设备策略

真实设备、Health Connect、Wear OS、BLE 或厂商 SDK 接入另开 story 或独立阶段。进入前必须重新确认权限、数据来源、非医疗文案、设备支持范围、后台行为和失败状态。

## Epic E12: 真实记录、统计图表与趋势分析

目标：基于真实 `WorkoutSession` 提供可信统计、图表、趋势和记录清理能力。

### Story E12.1: 真实记录与基础统计

作为用户，
我想看到真实训练记录和总统计，
以便知道自己完成了多少训练。

**验收标准:**

- Then 记录页读取真实持久化数据。
- Then 展示训练次数、总时长、有效训练时间等总统计。
- Then 统计口径区分 fixture、内存态和真实记录。
- Then 数字、时间、次数、轮次、总时长、有效时长和暂停时长有回归验证。
- Then 为同日多轮运动保留可分组分析口径，避免把不同轮次、不同计划结构或不可比阶段混在一起。

### Story E12.2: 图表与趋势分析

作为用户，
我想查看计划趋势、平均心率趋势和力量/计时表现变化，
以便做后续训练调整。

**验收标准:**

- Then 提供总统计图表、计划趋势和平均心率趋势。
- Then 分析时比较同类数据：同一计划、同一阶段、同一轮次或同一动作。
- Then 不把某天第一轮和另一天最后一轮直接比较。
- Then 平均心率趋势只消费明确来源的手动心率或后续真实设备数据；没有来源时不画假趋势。
- Then 不用不可比数据得出强弱、康复或医疗结论。

### Story E12.3: 历史记录清理

作为用户，
我想清理历史记录，
以便管理本地训练数据。

**验收标准:**

- Then 支持全部清除。
- Then 支持按训练计划清除。
- Then 支持按日期清除。
- Then 删除前有明确确认。
- Then 仅对真实持久化记录执行真实删除，不做假删除。

## Epic E13: 声音提示、固定女声 cue 与音频共存

目标：建立悦耳、克制、不打断其他 App 的训练音频提示。

### Story E13.1: 声音提醒与音频共存

作为训练中的用户，
我想听到短促提醒但不影响正在播放的音乐或视频，
以便训练节奏提示不会打断我的其他 App。

**验收标准:**

- Then 最后 N 秒声音提醒按偏好触发。
- Then `countdown_beep1.mp3` 用于 5 / 4 / 3 / 2 等最后 N 秒前几声 beep；具体触发秒数由 `CountdownCue.thresholdSec` 和偏好控制。
- Then `.local/audio/stage_bell_copper_clean.wav` 可作为最后 1 秒或阶段切换铃声候选；接入 App 时由执行 story 复制到 `app/src/main/res/raw/`，本地 `.local` 原文件不得提交。
- Then 声音提醒不降低、暂停或打断其他 App 音乐/视频。
- Then 不主动执行 ducking。
- Then 不请求会打断外部音频的 audio focus。
- Then 覆盖手机扬声器和蓝牙耳机 smoke，并记录不同 Android 版本和设备的回归结果。

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
| 用户测试后训练模式边界 | E10.1, E10.2, E10.3 |
| Timer Dial 设计与真实记录、统计、心率和音频后续 | E10.4, E10.5, E10.6, E10.7, E10.8, E10.9, E10.10, E10.11, E11, E12, E13 |

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
10. E9.1 到 E9.4：硬化、验收与用户测试修复包。
11. E10.1 到 E10.5：训练模式边界、计时训练重做、执行页主操作可达性、记录闭环前置和 Timer Dial 设计工作流。
12. E10.6 到 E10.9：Timer Dial 静态视觉方案、Compose 原型、生产集成、连续进度 polish 和用户测试 APK。
13. E10.9 Review Fix / User Test Fix：Timer Dial 视觉减字、总剩余时间居中放大、圆盘放大、环线层级和动态浅点修复。
14. E10.10：计划保存持久化和保存入口真实可用性检查。
15. E10.11：使用 `huashu-design` 做 3 个 Timer Dial HTML 高保真原型方向。
16. E11：手动心率输入与真实设备接口策略。
17. E12：真实记录、总统计、图表、趋势分析、同日多轮运动分析和历史记录清理。
18. E13：声音提醒、固定女声 cue、蓝牙/扬声器 smoke 和音频共存。

## 7. 下一轮建议

当前状态说明：

```text
E6.1 跟练雏形计划入口已合入 main。
E6.2 基础跟练执行页已合入 main。
E6.2 只支持 E6.1 内存态 preset 启动；E10.1 后，O-002 已收敛为跟练后续通过统一动作选择页编排，不再依赖计时计划切换为跟练视图。
E6.3 心率抽象状态展示已合入 main。
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
E10.1 已记录训练模式边界与执行页交互原则：计时训练回归纯间歇计时器，跟练/力量后续使用统一动作选择页，三类执行页遵守主操作即时可达原则，并把记录、心率、统计、声音和固定 cue 分流到 E10.4/E11/E12/E13。
E10.2 已完成计时训练纯阶段编辑页和大圆盘执行页首版实现。
E10.3 已完成力量 / 跟练执行页主操作可达性修复。
E10.4 已完成训练记录闭环前置并合入 main，计时 / 力量 / 基础跟练 completed 与 abandoned 终态可写入本地 Room session records，记录页生产入口读取真实本地记录。
E10.5 已记录 Timer Dial 设计工作流与重构范围：外部 APK / 截图只做 UI / 交互研究，不复制代码、资源或资产；工具路线为 Figma 静态规格、可选 HTML / Canvas 动效验证、Jetpack Compose Canvas 生产实现，Rive / Lottie 仅用于小图标或装饰动效；Timer Dial 规格包含顶部总剩余时间、外圈当前运动+休息周期、内圈总进度、中心圆当前阶段和底部少量图标操作；后续拆为 E10.6 / E10.7 / E10.8，E12 统计和 E13 声音保持独立。
E10.6 已记录 Timer Dial Figma / static visual variants：主文档为 `docs/planning/timer-dial-static-visual-variants.md`，覆盖 Official Flow 执行页状态帧、计时编辑页关键状态帧、Tile Flow / Big Type 适配、互动动画语义、小屏 / 无障碍检查、法律边界和 E10.7 handoff。E10.6 只改 Markdown / 设计文档，不实现 Android、不写 Kotlin、不改 Gradle、不改 prototype、不复制 APK 资产或动效参数、不新增第四套 skin、不混入 E11/E12/E13。
E10.7 已实现 Timer Dial Compose prototype：`feature.workoutsession` 新增 Timer Dial UI state / visual tokens / Canvas component / preview demo，低风险接入计时执行页，展示外圈阶段结构、当前阶段推进、内圈总进度、中心自绘阶段符号和 paused / final countdown 状态；新增 state/tokens/semantics 单元测试。E10.7 仍是 prototype，不是最终生产集成，不改 Room/session repository、engine 语义、声音、统计、心率设备或第四套 skin。
E10.8 已实现 Timer Dial production integration / animation polish：计时训练生产页默认使用 Official Flow Timer Dial；外圈只展示当前一次运动+休息周期，内圈展示整次训练总进度；中心圆负责暂停 / 继续，底部跳过和结束使用图标，结束仍需二次确认，`+15秒` 仅延长当前休息 15 秒。已完成 unit / assemble / lint / check 和 720x1280 emulator active / paused / rest smoke；最后 N 秒视觉截图窗口仍留作 review 关注点。
E10.9 已实现 Timer Dial reference polish / continuous progress / user-test APK：`r-design.md` 作为参考桥接文档纳入分支；Timer Dial active 状态下用 Compose frame clock 做最多当前 1 秒的连续进度投影，文案数字仍按秒更新；paused / completed / abandoned 不推进；`+15秒` rest extension 后进度不倒退；production controls 仍是 skip、`+15秒`、end。E10.9 是 Timer Dial 参考风格与连续动画 polish，不进入 E11/E12/E13。
E10.9 用户测试反馈计划已记录：Timer Dial 后续进入 Review Fix / User Test Fix，处理视觉减字、总剩余时间居中放大、圆盘放大、线条层级、底层宽圆环、动态浅点和中心圆简化；E10.10 处理计时/力量/跟练计划保存持久化和保存入口真实可用性；E10.11 使用 `huashu-design` 做 3 个 HTML 高保真 Timer Dial 原型方向；E13 处理 `countdown_beep1.mp3`、`.local/audio/stage_bell_copper_clean.wav`、蓝牙耳机/扬声器 smoke 和不抢占外部音乐视频；E12 继续处理总统计、图表、平均心率趋势和同日多轮运动分析。
```

下一轮建议按用户测试优先级进入：

```text
Story E10.9 Review Fix / User Test Fix；随后 E10.10 Plan Persistence、E10.11 Huashu Timer Dial Prototype、E13 Sound Cue System、E12 Stats / Records 分别推进。
```

E10.9 Review Fix / User Test Fix 建议重点确认：

1. 视觉修复是否只改变 Timer Dial 呈现，不破坏 E10.8/E10.9 的 continuous progress、pause freeze、terminal freeze 和 rest extension monotonic progress。
2. “总剩余”、下一阶段提示框、已启用声音提示框等文字是否被移除或弱化，总剩余时间是否更大、更居中。
3. 圆盘是否更大，外圈/内圈线条是否同比例更细，marker 与外圈是否不再重叠。
4. 宽底层圆环和浅色小点是否复用内圈阶段 marker 的动态角度计算，而不是固定装饰点。
5. 中心圆是否只保留图标、必要编号和时间，填充色来自阶段预设色，文字/图标为白色。
6. E10.10 计划保存、E13 声音播放、E12 统计图表、Room/session repository 和 prototype / Kotlin 以外的工作继续不混入该视觉修复 story。

## 8. 暂缓事项

以下事项不进入 E10.1/E10.2/E10.3 当前实现范围；只有进入上文明确拆出的后续 story 或更新决策日志后才能实施：

- 真实心率设备接入，需进入 E11 或独立设备阶段。
- Health Connect 历史数据读取，需进入 E11 或独立健康数据阶段。
- 用户任意文本 TTS、语音读秒大范围能力和自动语音教练。
- AI 实时动作纠错。
- 课程运营后台。
- 云同步和账号系统。
- iOS 工程。
- 运行时插件市场和远程主题下载。
