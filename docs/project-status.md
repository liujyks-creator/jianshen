# TrainFlow 项目状态

**状态日期:** 2026-06-01
**仓库:** `liujyks-creator/jianshen`
**主分支:** `main`

## 当前状态

TrainFlow 已经具备首版产品基线、UX 基线、初始数据契约草案、Android 首版架构草案、MVP roadmap/backlog 草案、实现准备检查报告、官方设计系统草案、开源 UI 定制边界草案、一个 React/Vite 前端原型，以及 E0.1 Android 生产工程骨架、E0.2 Android 模块/包边界、E0.3 核心 Kotlin 模型映射、E0.4 Room/DataStore 持久化基础骨架、E1.1 首批动作内容切片、E1.2 首批动作 fixture、E1.3 只读动作库列表与筛选、E1.4 只读动作详情、E2.1 Android 首页与训练入口、E2.2 计时计划编辑基础、E2.3 力量计划编辑基础、E2.4 计划列表/详情/复制/删除基础、E3.1 计时训练执行引擎、E3.2 计时训练执行页、E3.3 临近结束提醒、E3.4 暂停/跳过/延长休息/提前结束控制历史边界、E4.1 力量训练执行引擎、E4.2 力量训练执行页、E4.3 单组完成确认层、E4.4 动作替换与跳过，以及 E4 UI smoke fix。

项目已经从早期头脑风暴进入 Android 工程脚手架和 MVP story 实施阶段。`Story E0.1: 创建 Android 生产工程` 已按默认工程参数落地：包名 `com.liujyks.trainflow`、Gradle Kotlin DSL、Jetpack Compose + Material 3、单 `app` module 起步。`Story E0.2: 建立模块与包边界` 已在单 `app` module 内收敛核心/feature/UI/platform package 边界，并用轻量架构测试约束明显的反向依赖；物理 Gradle module 拆分继续留到代码体量需要时再做。`Story E0.3: 映射核心 Kotlin 模型` 已在 `core.model` 包内落地 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState` 和恢复建议相关契约。`Story E0.4: 建立 Room 与 DataStore 基础` 已在 `core.database` 与 `core.datastore` 包内落地最小可编译持久化骨架、Room schema 导出和 smoke test。`Story E1.1: 定义首批动作内容切片` 已收敛 11 个首批动作、必填字段、训练类型适配、指导内容边界、恢复/替代映射草案和审核标准。`Story E1.2: 导入动作 fixture` 已将 11 个首批动作导入 Android fixture，并用 fixture 校验测试约束 ID、必填字段、能力标签、默认建议、恢复映射和替代动作边界。`Story E1.3: 动作库列表与筛选` 已基于 E1.2 fixture 落地只读 Compose 列表、训练类型/身体部位/器械/难度筛选、清除筛选、空状态和动作摘要卡片。`Story E1.4: 动作详情` 已基于 E1.2 fixture 和 E1.3 列表入口落地只读动作详情，展示短提示、设置与执行步骤、发力要点、常见错误、呼吸提示、安全说明、替代动作和恢复映射。`Story E2.1: 首页与训练入口` 已建立轻量官方 shell、训练首页、计时训练推荐默认入口、力量训练同层入口和可进入 E1.3/E1.4 的动作库入口。`Story E2.2: 计时计划编辑` 已让首页计时推荐入口进入内存态计时计划编辑页，支持计划名称、热身/拉伸时长、动作时长、动作后休息、轮数、轮间休息、动作/休息临近结束提醒阈值和提醒形式开关，并能生成符合 `WorkoutPlan` / `TimedCircuitBlock` / `TimedExerciseItem` / `CueSettings` 契约的本次草稿预览；E2.2 retro fix 已关闭计时提醒阈值边界问题。`Story E2.3: 力量计划编辑` 已让首页力量训练入口进入内存态力量计划编辑页，支持计划名称、力量动作选择、目标重量、默认 `8-12` 次区间、固定次数、正式组数、动作内热身组、组间休息和逐组目标展开编辑，并能生成符合 `WorkoutPlan` / `StrengthExerciseBlock` / `StrengthExerciseTarget` / `StrengthSetPlan` 契约的本次草稿预览。`Story E2.4: 计划列表、详情、复制与删除` 已在官方 shell 中启用“计划”入口，复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约种子化内存态计划集合，支持计划列表、详情、复制和删除确认。`Story E3.1: 计时训练执行引擎` 已在 `core.engine` 内落地纯 Kotlin 状态机，可从有效计时计划/快照展开动作、休息和轮次步骤，支持开始、暂停、继续、跳过、延长休息和提前结束命令，并产出动作开始、动作临近结束、休息开始、休息临近结束、暂停、继续和完成事件。`Story E3.2: 计时训练执行页` 已在 `feature.workoutsession` 中新增计时训练执行 UI state、Compose route 和深色执行页，从现有内存态计划详情仅启用计时计划开始入口，执行页复用 E3.1 `TimedWorkoutEngine` 展示当前动作/休息、主倒计时、轮次/步骤进度、下一步、动作短提示和辅助心率占位，并通过 `WorkoutCommand` 支持暂停、继续、跳过、延长休息和结束训练。`Story E4.1: 力量训练执行引擎` 已在 `core.engine` 内新增纯 Kotlin `StrengthWorkoutEngine`，可从有效力量计划/快照按动作和组推进准备、开始本组、完成本组、确认记录、组间休息和完成/废弃终态。`Story E4.2: 力量训练执行页` 已在 `feature.workoutsession` 中新增力量训练执行 UI state、Compose route 和深色执行页，从内存态力量计划详情启用开始入口，执行页复用 E4.1 `StrengthWorkoutEngine` 展示当前动作、本组目标、组耗时、最小确认、休息倒计时和下一组目标，并通过 `WorkoutCommand` 支持开始本组、完成本组、按计划确认、暂停/继续、休息中提前开始下一组和结束训练。`Story E4.3: 单组完成确认层` 已将 Confirm 状态升级为可编辑确认层，展示计划重量、计划次数、本组耗时、组类型、动作名和组序号，默认回填实际重量/次数，支持次数区间快捷选择与 easy / good / hard / form_breakdown 主观感受，并通过 `WorkoutCommand.ConfirmStrengthSet` 生成正式 `StrengthSetRecord`。`Story E4.4: 动作替换与跳过` 已让力量训练支持通过 `WorkoutCommand.ReplaceExercise` 替换当前动作并在 `StrengthSetRecord.substitutedFromExerciseId` 保留原动作引用，也支持通过 `WorkoutCommand.SkipStep` 跳过当前动作剩余未完成组并继续后续动作；本阶段仍未引入真实 `WorkoutSession` 持久化、Room/DataStore repository 闭环、通知调度、声音/震动平台能力、真实心率设备、语音能力、完整训练总结或跟练闭环。

`Story E4.1: 力量训练执行引擎`、`Story E4.2: 力量训练执行页`、`Story E4.3: 单组完成确认层`、`Story E4.4: 动作替换与跳过` 和 E4 UI smoke fix 均已通过 Review Gate 并合入 `main`。当前 `main` / `origin/main` 为 `fe92eff4a2440a455133426722ca174f31335116`。`emulator-5554` UI 抽查已通过，E4 力量训练闭环可以标记为完成。当前无已知 blocker。

## 已有产物

### 产品与规划

当前规划来源文件为：

1. `docs/planning/product-brief.md`
2. `docs/planning/prd.md`
3. `docs/planning/ux-design.md`
4. `docs/planning/data-contracts.md`
5. `docs/planning/decision-log.md`
6. `docs/architecture.md`
7. `docs/roadmap-backlog.md`
8. `docs/readiness-report.md`
9. `DESIGN.md`
10. `docs/ui-extension-guide.md`
11. `docs/planning/action-content-slice.md`

根目录还包含 story 工作流提示模板：

1. `DEV_STORY_PROMPT_TEMPLATE.md`
2. `CODE_REVIEW_PROMPT_TEMPLATE.md`

这些文档覆盖：

- 产品目标、目标用户、MVP 边界和后续阶段设想。
- 计时训练与力量训练流程。
- 跟练雏形能力。
- 动作倒计时、休息提醒和力量组记录。
- 动作库内容要求、首批动作切片与数据接口。
- 心率展示边界和后续设备接入边界。
- 面向未来语音交互与平台适配的训练命令和训练事件。
- Android 原生首版架构、模块边界、本地持久化、训练执行引擎和平台适配边界。
- MVP 里程碑、Epic、Story 和验收顺序。
- E0.1 启动条件、当前 blocking/P1/P2 风险和实现范围边界。
- 官方默认 UI 设计系统 token、组件语义和训练执行页设计规则。
- 开源社区定制 UI shell、主题、首页布局和按钮位置的边界。

### 前端原型

`prototype` 目录包含一个基于 React/Vite 的原型，使用假数据和可交互预览流验证产品方向。

原型当前覆盖：

- 训练首页。
- 计时计划编辑预览。
- 计时训练执行与倒计时状态。
- 动作与休息临近结束强调状态。
- 力量计划编辑预览。
- 力量训练单组流程。
- 完成本组时以计划重量和次数预填实际记录。
- 跟练雏形预览。
- 动作库和动作详情。
- 训练总结和基础恢复映射。
- 模拟心率状态。

原型核心文件：

1. `prototype/src/App.tsx`
2. `prototype/src/App.css`
3. `prototype/src/data/contracts.ts`
4. `prototype/src/data/fixtures.ts`

## 已接受方向

当前已接受方向为：

1. Android 首发，未来保留 iOS 路径。
2. 计时训练和力量训练都属于首版能力。
3. 计时训练是默认推荐入口。
4. 跟练首版只做雏形，复用计时流程和动作内容。
5. 动作库先定接口，再扩大内容量。
6. 心率展示、语音交互、丰富媒体、音乐节奏和 AI 分析先保留接口，不扩张首版交付范围。
7. Android 首版采用 Kotlin、Jetpack Compose、本地优先和训练执行引擎独立业务核心。
8. 首版训练提醒以普通通知为基线，不把闹铃级强提醒作为 MVP 硬依赖。
9. 官方默认 UI 以 `DESIGN.md` 为设计系统单一真源。
10. 开源社区可以定制 UI shell、主题和布局，但不能破坏核心训练引擎、命令事件和数据契约。

精简决策记录见 `docs/planning/decision-log.md`。

## 仍待确定

以下事项在生产实现深入前仍需继续收敛：

1. 首批导入动作库的动作清单和内容深度已由 E1.1 收敛，详见 `docs/planning/action-content-slice.md`；`sourceMeta`/`extensions` 对齐策略已由 E1.2 记录到 `docs/planning/decision-log.md` 和 `docs/planning/data-contracts.md`。
2. 跟练雏形首版的精确边界。
3. 首版是否真的播放语音读秒，还是只保留语音接口。
4. Android 工程脚手架 E0.1 已采用 `minSdk 26`、`compileSdk/targetSdk 36`、包名 `com.liujyks.trainflow`、Kotlin DSL、单 `app` module 起步。
5. 后续心率数据源策略和健康数据权限流。
6. 各 story 的详细开发说明、测试清单和验收记录。
7. 官方默认 UI 是否首版同时提供暗色主题，还是先提供浅色工作区 + 深色训练执行页。
8. `docs/planning/data-contracts.md` 与 `prototype/src/data/contracts.ts` 的 `WorkoutCommand` 细节需要后续对齐；E0.3 已以文档和决策日志为准，保留文档中的 `update_actual_weight`、`update_actual_reps` 和更细的力量组完成/确认输入结构。

## 建议下一步

除非用户改变方向，建议按以下顺序推进：

1. 生成并启动 `Story E5.1: 计时训练总结` 的 Dev Story 提示词。
2. E5.1 应消费 E3 的 timed session history、step history、control history 与 rest extension history，不回头让 UI 直接写训练结果。
3. E5.1 只定义和实现计时训练总结所需的最小闭环，不提前实现 E5.2 力量训练总结、E5.3 历史趋势或 E5.4 恢复建议完整能力。
4. 后续计划管理与编辑入口应继续复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约、E1.3/E1.4 的 fixture/domain model 与动作详情 UI state，不让 Room entity 泄漏到 feature/UI，也不把动作详情扩展成课程内容平台。

## 验证快照

当前前端原型曾用以下命令检查：

```powershell
cd .\prototype
npm.cmd run lint
npm.cmd run build
```

新克隆仓库后，应在 `npm.cmd install` 后重新执行这些命令。

Android E0.1 工程曾用以下命令检查：

```powershell
$env:JAVA_HOME = "<JDK 17 path>"
$env:ANDROID_HOME = "<Android SDK path>"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat tasks --all
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
```

当前 E0.1 本地验证使用 Gradle 9.4.1、Android Gradle Plugin 9.2.0、Kotlin/Compose compiler 2.3.21、Compose BOM 2026.05.00、Android SDK Platform 36。
E0.2 继续沿用同一技术基线，新增 package boundary 架构测试随 `app:check` 执行。
E0.3 继续沿用同一技术基线，新增 `core.model` 轻量契约测试随 `app:check` 执行。
E0.4 在同一单 `app` module 中新增 Room 2.8.4、DataStore Preferences 1.2.1、KSP 2.3.9、Robolectric 4.16.1 和 AndroidX Test Core 1.7.0。Room schema 导出到 `app/schemas`，新增 Room/DataStore smoke test 随 `app:check` 执行。
E1.3 基于首批 11 个动作 fixture 新增只读动作库列表和筛选，新增筛选逻辑与 fixture-to-UI-state 映射测试，并随 `app:check` 执行。
E1.4 基于 E1.2 fixture 和 E1.3 列表新增只读动作详情，新增动作详情 UI state、动作 id 查找、缺失动作空状态和列表到详情入口的轻量映射测试，并随 `app:check` 执行。
E2.1 新增官方轻量 shell 与训练首页入口状态，新增首页 UI state 测试；动作库入口复用 E1.3/E1.4 列表与详情，未启用的计时计划、力量计划、跟练、记录和恢复入口均明确标注后续接入并保持禁用。
E2.2 新增内存态计时计划编辑 UI 与状态映射测试；首页计时推荐入口已进入编辑页，草稿可映射为 `WorkoutPlan`、`TimedCircuitBlock`、`TimedExerciseItem` 和全局 `CueSettings`，但仍不写入 Room、不接入 repository、不启动训练执行引擎。
E2.2 retro fix 已修正计时提醒阈值边界，关闭动作/休息临近结束提醒在短时长配置下的边界问题。
E2.3 新增内存态力量计划编辑 UI 与状态映射测试；首页力量训练入口已进入编辑页，草稿可映射为 `WorkoutPlan`、`StrengthExerciseBlock`、`StrengthExerciseTarget`、`StrengthSetPlan`、替代动作候选和默认 `manual_start` 组计时模式，但仍不写入 Room、不接入 repository、不启动训练执行引擎。
E2.4 新增内存态计划管理 UI 与状态映射测试；官方 shell 的“计划”入口已启用，列表/详情复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约，支持复制计划和删除确认，但仍不写入 Room、不接入 repository、不启动训练执行引擎、不创建 `WorkoutSession` 或 session records。
E3.1 新增 `core.engine` 纯 Kotlin 计时训练执行引擎和单元测试；引擎消费 `WorkoutPlan` / `WorkoutPlanSnapshot` 与 `WorkoutCommand`，按 `TimedCircuitBlock` 的动作、休息、轮次和轮间休息推进，产出 `WorkoutEvent`，并固定暂停恢复、跳过、延长休息、提醒阈值覆盖/忽略和提前结束废弃状态边界；本阶段仍不接 UI、Room repository、真实 session records、通知、声音、震动、心率设备、语音或力量训练执行引擎。
E3.2 新增 `feature.workoutsession` 计时训练执行 UI state、Compose route 和执行页单元测试；计划详情中仅计时计划启用开始入口，官方 shell 使用内存态计划进入深色训练执行页；页面展示当前动作/休息、主倒计时、进度、下一步、短提示和辅助心率占位，暂停、继续、跳过、延长休息和结束训练均通过 `WorkoutCommand` 分发给 E3.1 `TimedWorkoutEngine`；本阶段仍不写入 Room、不创建真实 `WorkoutSession` / session records、不接通知、声音、震动、真实心率设备、语音、完整总结、跟练闭环或力量训练执行页。
E3.3 新增计时训练临近结束提醒消费链路；执行页捕获 E3.1 的 `timed_work_ending` 与 `rest_ending` 事件，按动作/休息各自的 `CountdownCue` 展示克制强化状态、剩余秒数和提示文案，并通过薄反馈分发边界按 `soundEnabled`、`vibrationEnabled` 与 `emphasisAnimationEnabled` 开关触发声音、触感和视觉强调；本阶段仍不接通知调度、前台服务、真实 session records、真实心率设备、语音或完整总结。
E3.4 新增计时训练控制历史边界；`TimedWorkoutEngine` 现在在开始、暂停、继续、跳过、延长休息和提前结束时维护 step history、control history、rest extension history 与 early-end 进度记录，UI state mapper 暴露跳过数、延长休息总秒数和轻量历史摘要；本阶段仍不接 Room/DataStore repository、真实 session records 写库、通知、前台服务、真实心率设备、语音、完整总结、跟练闭环或力量训练执行页。
E4.1 新增 `core.engine` 纯 Kotlin 力量训练执行引擎和单元测试；引擎消费 `WorkoutPlan` / `WorkoutPlanSnapshot` 与 `WorkoutCommand`，按 `StrengthExerciseBlock` 和 `StrengthSetPlan` 展开组步骤，支持 `start_session`、`pause_session`、`resume_session`、`start_strength_set`、`complete_strength_set`、`confirm_strength_set` 和 `end_session`。完成本组后只生成确认草案，不直接写正式记录；确认后生成内存态 `StrengthSetRecord`，记录计划重量/次数、实际重量/次数、本组耗时、实际休息和主观感受。暂停会冻结 active set 计时和 rest 剩余时间；提前结束进入 `ABANDONED`，terminal state 后命令不污染 records/history。本阶段仍不接 UI、ViewModel、Room/DataStore repository、真实 session records 写库、通知、声音、震动、真实心率设备、语音、完整总结、跟练闭环、动作替换或跳过。
E4.2 新增 `feature.workoutsession` 力量训练执行 UI state、Compose route 和执行页单元测试；计划详情中 strength plan 启用“开始力量训练”入口，官方 shell 使用内存态力量计划进入深色力量执行页；页面展示当前动作、本组序号/组类型、计划重量/次数、组耗时、休息倒计时、下一组目标、轻量心率占位和轻量 completed / abandoned 结束态。Prepare / Active / Confirm / Rest 的主操作分别通过 `WorkoutCommand.StartStrengthSet`、`WorkoutCommand.CompleteStrengthSet`、`WorkoutCommand.ConfirmStrengthSet(StrengthSetCompletionInput())` 和休息中提前 `StartStrengthSet` 分发给 E4.1 `StrengthWorkoutEngine`；Confirm 仅提供“按计划确认”，不编辑 actual weight、actual reps、effort 或 notes。本阶段仍不写入 Room、不创建真实 `WorkoutSession` / session records、不接通知、声音、震动、真实心率设备、语音、完整总结、跟练闭环、动作替换或跳过。
E4.3 新增 `feature.workoutsession` 力量 Confirm 状态的可编辑单组完成确认层和输入校验；确认层展示计划重量、计划次数、本组耗时、组类型、动作名和组序号，实际重量默认回填计划重量，实际次数默认回填固定次数或区间下限，区间次数提供快捷选择，主观感受支持 easy / good / hard / form_breakdown 对应“轻松 / 刚好 / 很吃力 / 动作变形”。确认按钮发送携带 actual weight、actual reps 和 effort 的 `WorkoutCommand.ConfirmStrengthSet`，继续复用 E4.1 `StrengthWorkoutEngine` 推进到休息、下一组或完成。新增 UI state / 输入校验测试覆盖固定 reps、range reps、无重量计划、组级覆盖动作级目标和非法输入禁用。本阶段仍不写入 Room、不创建真实 `WorkoutSession` / session records、不接通知、声音、震动、真实心率设备、语音、完整总结、跟练闭环、动作替换或跳过。

E4.4 新增力量训练动作替换与跳过能力；`StrengthWorkoutEngine` 现在消费 `WorkoutCommand.ReplaceExercise` 和 `WorkoutCommand.SkipStep` 的力量路径，在不改写原 `WorkoutPlan` 的前提下维护当前 block 的 effective exercise，后续 `StrengthSetRecord` 通过 `substitutedFromExerciseId` 保留原计划动作引用；跳过当前动作会跳过当前 `StrengthExerciseBlock` 的剩余未完成组并进入下一动作或 completed。力量执行页新增克制的动作调整面板，替换候选来自计划/动作替代映射和首批 fixture 中适合力量训练的动作，跳过动作需要明确确认。新增引擎与 UI state 测试覆盖替换来源记录、跳过顺序、最后动作跳过完成、active/confirm/rest 跳过语义、terminal state 后忽略命令、候选过滤和命令分发边界。本阶段仍不写入 Room、不创建真实 `WorkoutSession` / session records、不接通知、声音、震动、真实心率设备、语音、完整总结、跟练闭环或恢复建议。

E4 UI smoke fix 已完成并合入 `main`；`emulator-5554` UI 抽查已通过，力量训练从计划详情进入执行、单组确认、替换/跳过与结束态的关键 UI 路径完成收口。当前 E4 力量训练闭环标记为完成，下一步进入 Epic E5 的 `Story E5.1: 计时训练总结`，本状态刷新不提前设计或实现 E5.1。

## 新 Codex 会话提示词

新会话可从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md、docs/readiness-report.md 以及 docs/planning 下的规划文档。
然后检查当前仓库状态与 prototype 原型，基于当前已接受的 MVP 基线继续推进 TrainFlow，不要静默扩大范围。
```
