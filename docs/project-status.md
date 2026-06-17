# TrainFlow 项目状态

**状态日期:** 2026-06-17
**仓库:** `liujyks-creator/jianshen`
**主分支:** `main`

## 当前状态

TrainFlow 已经具备首版产品基线、UX 基线、初始数据契约草案、Android 首版架构草案、MVP roadmap/backlog 草案、实现准备检查报告、官方设计系统草案、开源 UI 定制边界草案、一个 React/Vite 前端原型，以及 E0.1 Android 生产工程骨架、E0.2 Android 模块/包边界、E0.3 核心 Kotlin 模型映射、E0.4 Room/DataStore 持久化基础骨架、E1.1 首批动作内容切片、E1.2 首批动作 fixture、E1.3 只读动作库列表与筛选、E1.4 只读动作详情、E2.1 Android 首页与训练入口、E2.2 计时计划编辑基础、E2.3 力量计划编辑基础、E2.4 计划列表/详情/复制与删除基础、E3.1 计时训练执行引擎、E3.2 计时训练执行页、E3.3 临近结束提醒、E3.4 暂停/跳过/延长休息/提前结束控制历史边界、E4.1 力量训练执行引擎、E4.2 力量训练执行页、E4.3 单组完成确认层、E4.4 动作替换与跳过、E4 UI smoke fix、E5.1 计时训练总结、E5.2 力量训练总结、E5.3 训练历史与基础趋势、E5.4 基础恢复建议、E6.1 跟练雏形计划入口、E6.2 基础跟练执行页、E6.3 心率抽象状态展示、E7.1 训练提醒通知、E7.2 活跃训练通知边界、E7.3 训练偏好设置、E8.1 内置 UI 皮肤 contract / registry、E8.2 Tile Flow 关键页面磁贴式皮肤、E8.3 Big Type 大字训练皮肤、E8.4 UI skin review checklist 与用户测试前 UI readiness、E9.1 训练状态恢复与关键回归测试基线、E9.2 权限与隐私文案收口、E9.3 MVP 验收清单与用户测试问题模板、E9.4 用户测试修复包 1、E10.1 训练模式边界与执行页交互原则记录、E10.2 计时训练纯间歇编辑页与大圆盘执行页首版实现、E10.3 力量/跟练执行页主操作可达性修复、E10.4 训练记录闭环前置、E10.5 Timer Dial 设计工作流与重构范围记录、E10.6 Timer Dial 静态视觉帧规格、E10.7 Timer Dial Compose prototype、E10.8 Timer Dial production integration / animation polish、E10.9 Timer Dial reference polish / continuous progress / user-test APK 准备、E10.9 用户测试反馈计划、E10.10 Plan persistence and save-entry audit、E10.11 Huashu Timer Dial HTML prototype exploration、E10.12 Timer Dial Compose landing、E10.13 Ready Start Gate、E10.14 Rest Extension Semantics And Recording、E10.15 Motion Timing Rules、E10.16 Motion Landing、E10.17 Stage Color Picker、E10.18 Plan Edit Backfill、E12.1 Real Records And Basic Stats、E12.2a Non-heart-rate history charts and aggregate trends、E12.3 History Cleanup，以及 E12.2c Timed comparable stage and extra rest trends。

项目已经从早期头脑风暴进入 Android 工程脚手架和 MVP story 实施阶段。`Story E0.1: 创建 Android 生产工程` 已按默认工程参数落地：包名 `com.liujyks.trainflow`、Gradle Kotlin DSL、Jetpack Compose + Material 3、单 `app` module 起步。`Story E0.2: 建立模块与包边界` 已在单 `app` module 内收敛核心/feature/UI/platform package 边界，并用轻量架构测试约束明显的反向依赖；物理 Gradle module 拆分继续留到代码体量需要时再做。`Story E0.3: 映射核心 Kotlin 模型` 已在 `core.model` 包内落地 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState` 和恢复建议相关契约。`Story E0.4: 建立 Room 与 DataStore 基础` 已在 `core.database` 与 `core.datastore` 包内落地最小可编译持久化骨架、Room schema 导出和 smoke test。`Story E1.1: 定义首批动作内容切片` 已收敛 11 个首批动作、必填字段、训练类型适配、指导内容边界、恢复/替代映射草案和审核标准。`Story E1.2: 导入动作 fixture` 已将 11 个首批动作导入 Android fixture，并用 fixture 校验测试约束 ID、必填字段、能力标签、默认建议、恢复映射和替代动作边界。`Story E1.3: 动作库列表与筛选` 已基于 E1.2 fixture 落地只读 Compose 列表、训练类型/身体部位/器械/难度筛选、清除筛选、空状态和动作摘要卡片。`Story E1.4: 动作详情` 已基于 E1.2 fixture 和 E1.3 列表入口落地只读动作详情，展示短提示、设置与执行步骤、发力要点、常见错误、呼吸提示、安全说明、替代动作和恢复映射。`Story E2.1: 首页与训练入口` 已建立轻量官方 shell、训练首页、计时训练推荐默认入口、力量训练同层入口和可进入 E1.3/E1.4 的动作库入口。`Story E2.2: 计时计划编辑` 已让首页计时推荐入口进入内存态计时计划编辑页，支持计划名称、热身/拉伸时长、动作时长、动作后休息、轮数、轮间休息、动作/休息临近结束提醒阈值和提醒形式开关，并能生成符合 `WorkoutPlan` / `TimedCircuitBlock` / `TimedExerciseItem` / `CueSettings` 契约的本次草稿预览；E2.2 retro fix 已关闭计时提醒阈值边界问题。`Story E2.3: 力量计划编辑` 已让首页力量训练入口进入内存态力量计划编辑页，支持计划名称、力量动作选择、目标重量、默认 `8-12` 次区间、固定次数、正式组数、动作内热身组、组间休息和逐组目标展开编辑，并能生成符合 `WorkoutPlan` / `StrengthExerciseBlock` / `StrengthExerciseTarget` / `StrengthSetPlan` 契约的本次草稿预览。`Story E2.4: 计划列表、详情、复制与删除` 已在官方 shell 中启用“计划”入口，复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约种子化内存态计划集合，支持计划列表、详情、复制和删除确认。`Story E3.1: 计时训练执行引擎` 已在 `core.engine` 内落地纯 Kotlin 状态机，可从有效计时计划/快照展开动作、休息和轮次步骤，支持开始、暂停、继续、跳过、延长休息和提前结束命令，并产出动作开始、动作临近结束、休息开始、休息临近结束、暂停、继续和完成事件。`Story E3.2: 计时训练执行页` 已在 `feature.workoutsession` 中新增计时训练执行 UI state、Compose route 和深色执行页，从现有内存态计划详情仅启用计时计划开始入口，执行页复用 E3.1 `TimedWorkoutEngine` 展示当前动作/休息、主倒计时、轮次/步骤进度、下一步、动作短提示和辅助心率占位，并通过 `WorkoutCommand` 支持暂停、继续、跳过、延长休息和结束训练。`Story E4.1: 力量训练执行引擎` 已在 `core.engine` 内新增纯 Kotlin `StrengthWorkoutEngine`，可从有效力量计划/快照按动作和组推进准备、开始本组、完成本组、确认记录、组间休息和完成/废弃终态。`Story E4.2: 力量训练执行页` 已在 `feature.workoutsession` 中新增力量训练执行 UI state、Compose route 和深色执行页，从内存态力量计划详情启用开始入口，执行页复用 E4.1 `StrengthWorkoutEngine` 展示当前动作、本组目标、组耗时、最小确认、休息倒计时和下一组目标，并通过 `WorkoutCommand` 支持开始本组、完成本组、按计划确认、暂停/继续、休息中提前开始下一组和结束训练。`Story E4.3: 单组完成确认层` 已将 Confirm 状态升级为可编辑确认层，展示计划重量、计划次数、本组耗时、组类型、动作名和组序号，默认回填实际重量/次数，支持次数区间快捷选择与 easy / good / hard / form_breakdown 主观感受，并通过 `WorkoutCommand.ConfirmStrengthSet` 生成正式 `StrengthSetRecord`。`Story E4.4: 动作替换与跳过` 已让力量训练支持通过 `WorkoutCommand.ReplaceExercise` 替换当前动作并在 `StrengthSetRecord.substitutedFromExerciseId` 保留原动作引用，也支持通过 `WorkoutCommand.SkipStep` 跳过当前动作剩余未完成组并继续后续动作。`Story E5.1: 计时训练总结` 已在 `feature.workoutsession` 中新增计时训练 summary UI state / mapper，消费 E3.4 的 step history、control history、rest extension history 和 early-end 记录，在 completed / abandoned 终态展示总时长、完成阶段、步骤/轮次进度、跳过内容、休息延长、提前结束进度、训练部位摘要和恢复建议入口；`Story E5.2: 力量训练总结` 已在同一 feature 边界新增 strength summary UI state / mapper，消费 E4.1-E4.4 的 strength records、history、replacement 和 skip 边界，在 completed / abandoned 终态展示动作、组数、重量、次数、组耗时、实际休息、计划/实际差异、替换/跳过摘要和恢复建议入口；`Story E5.3: 训练历史与基础趋势` 已启用内存态历史和基础趋势；`Story E5.4: 基础恢复建议` 已启用训练后恢复建议入口；`Story E6.1: 跟练雏形计划入口` 已启用基础跟练入口和选择页，展示一个只使用 supportsFollowAlong 动作的内存态 preset；`Story E6.2: 跟练执行页` 已让 preset 进入基础跟练执行页；`Story E6.3: 心率抽象状态展示` 已统一三类执行页的低层级心率状态展示，`Story E7.1: 训练提醒通知` 已建立计划提醒普通通知边界。E10.4 已引入最小真实 `WorkoutSession` 本地写入和记录页读取闭环；当前仍未引入前台服务、后台训练可靠计时、真实心率设备、语音能力、视频播放、历史记录删除、统计图表或任意计时计划切换为跟练视图。

E10.10 Plan Persistence、E10.11 Huashu Timer Dial HTML prototype exploration、E10.12 Timer Dial Compose landing、E10.13 Ready Start Gate、E10.14 Rest Extension Semantics And Recording、E10.15 Motion Timing Rules、E10.16 Motion Landing、E10.17 Stage Color Picker 和 E10.18 Plan Edit Backfill 均已基于最新 `main` 推进。E10.18 让计划详情的计时 / 力量计划进入对应编辑页并回填已有配置，保存时用原 `WorkoutPlan.id` 覆盖本地计划；编辑保存会保留原 reminder 和非当前编辑器管理的 preferences，并在已有 reminder 路径上取消后按当前 policy 重新调度或清理旧提醒；跟练仍不提供假编辑入口。E10.18 不改变训练执行引擎、`WorkoutSession` repository、历史 session plan snapshot、`WorkoutCommand`、`WorkoutEvent`、E10.13 ready gate、E10.14 rest extension、E10.16 motion 或 E10.17 stage color picker 语义；不实现版本历史、undo / redo、云同步、完整跟练编排、统计图表、声音播放 / 音频复制、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 UI 视觉重做；不移动音频，不提交 `.local`、APK、`人工/`、deliverables、截图、日志或 build 输出。

E12.1 Real Records And Basic Stats 已实现；记录页继续读取本地 Room `WorkoutSession` 真实记录，并在非空真实记录时显示克制的“真实记录基础统计”区。统计从 session list 直接推导，不新增 Room 聚合或 schema migration，覆盖训练总次数、completed / abandoned 分开计数、`totalElapsedSec`、`effectiveElapsedSec`、`pausedElapsedSec`、计划休息、实际休息、计时额外休息 `timedRestExtensionRecords.sum(addedSec)` 和计时 / 力量 / 跟练 mode breakdown。preview / fixture 示例记录不进入真实统计，空记录页不显示假统计；单次记录详情也补充总用时、有效训练时间、暂停时间、计划休息、实际休息和额外休息，继续使用历史 `WorkoutSession.planSnapshot` 而不是编辑后的当前计划反推。E12.1 不实现 E12.2 图表趋势、平均心率趋势、同类数据趋势比较、真实心率设备、Health Connect、Wear OS、BLE、声音播放、云同步、账号体系、历史记录清理、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

E12.2a Non-heart-rate history charts and aggregate trends 已实现；记录页在真实持久化 session 非空时新增“非心率图表与聚合趋势”，从真实 `WorkoutSession` list 按 `startedAt` 日期聚合训练总次数、completed / abandoned、`totalElapsedSec` / `effectiveElapsedSec` / `pausedElapsedSec`、planned rest / actual rest / extra rest，并展示 timed / strength / follow_along mode breakdown。趋势不足 2 个日期点时只显示“暂无趋势”，不绘制假曲线；planned rest 继续来自历史 `WorkoutSession.planSnapshot`，actual rest 来自真实执行记录，extra rest 只来自 `timedRestExtensionRecords.addedSec` 且不并入暂停时间。当前没有明确来源的设备心率或可选手动心率记录时，记录页显示未获取心率，不输出平均心率趋势数据。E12.2a 不实现设备心率获取、手动心率输入、持久化心率模型、E12.2b 力量同类 set 趋势、E12.2c 计时同类阶段 / 轮次深趋势、历史清理、声音播放、云同步、账号体系、foreground service、exact alarm、notification action 或 reset production command。

E12.3 History Cleanup 已实现；记录页新增真实本地历史清理能力，支持全部清除、按 `WorkoutSession.planId` 清除和按 `startedAt` 展示日期清除。每种清理都必须先进入明确确认对话框，确认后才调用 `WorkoutSessionRepository` 的 Room 事务删除；删除会清理 `workout_sessions` 及其 step records、timed rest extension records 和 strength set records 子表。删除后记录列表、E12.1 基础统计、E12.2a 聚合趋势、mode breakdown 和空状态继续由 Room Flow 的剩余真实记录自动刷新，不在 Compose 层做假过滤。E12.3 不删除 `WorkoutPlan`、`Exercise`、fixture / preview 数据，不改写任何 `WorkoutSession.planSnapshot`，不实现 undo / recycle bin / 版本历史、云同步、账号体系、远端删除、E12.2b、E12.2c、心率数据源、声音播放、foreground service、exact alarm、notification action 或 reset production command。

E12.2c Timed comparable stage and extra rest trends 已实现；记录页在真实持久化 session 非空时新增“计时同类阶段与额外休息趋势”，只消费 `WorkoutMode.TIMED` 的真实 `WorkoutSession`。mapper 基于历史 `WorkoutSession.planSnapshot` 重建计时休息阶段结构，并只在同一计划结构、同一 REST 阶段类型、同一阶段顺序、同一轮次、同一 step index、同一 restStageId 和 previousStageId 关系下比较 planned rest、actual rest 和 extra rest；actual rest 来自真实 `SessionStepRecord`，extra rest 只来自字段完整的 `timedRestExtensionRecords.addedSec`。样本不足、缺少可匹配 step record、不同计划结构、不同轮次或额外休息位置字段缺失时显示暂无趋势 / 数据不足或降级说明，不绘制假趋势，不跨结构硬比。E12.2c 不实现 E12.2b 力量同类趋势、平均心率趋势、设备心率获取、手动心率输入、Health Connect、Wear OS、BLE、声音播放、云同步、账号体系、foreground service、exact alarm、notification action 或 reset production command，也不修改训练执行引擎、`WorkoutCommand`、`WorkoutEvent`、Room schema、`WorkoutSession.planSnapshot` 或历史删除语义。

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
11. `docs/testing/ui-skin-readiness-checklist.md`
12. `docs/testing/training-state-recovery-checklist.md`
13. `docs/testing/permission-privacy-readiness-checklist.md`
14. `docs/testing/mvp-acceptance-checklist.md`
15. `docs/testing/user-test-issue-template.md`
16. `docs/planning/action-content-slice.md`
17. `docs/planning/e10-training-mode-interaction-plan.md`
18. `docs/planning/timer-dial-design-workflow.md`
19. `docs/planning/timer-dial-static-visual-variants.md`

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
- 三套内置 UI 皮肤审查、E9 用户测试前 UI readiness 和社区主题/layout 贡献检查清单。
- 权限、隐私、普通通知、心率占位、恢复建议、音频提示、语音预留和内存态数据边界检查清单。
- MVP 用户测试前总验收结论、问题分级、数字输入清空复现、编辑页开始按钮状态、E10/E11/E12 后续方向和用户测试 issue 模板。
- E10.1 用户测试后训练模式边界、计时训练纯间歇计时器方向、大圆盘执行页原则、统一动作选择页决策、执行页主操作即时可达原则，以及 E10.2/E10.3/E10.4/E10.5/E10.6/E10.7/E10.8/E10.9/E11/E12/E13 后续拆分。
- E10.6 Timer Dial 静态视觉帧、计时编辑页关键状态、Official Flow / Tile Flow / Big Type 适配、小屏 / 无障碍检查和 E10.7 handoff。
- E10.7 Timer Dial Compose prototype、UI state / tokens / preview demo、三类 visual variant、paused / final countdown 状态和 E10.8 production integration handoff。
- E10.8 Timer Dial 生产集成、Official Flow 默认变体、preview-only visual variants、当前运动+休息周期外圈、整次训练总进度内圈、中心圆暂停 / 继续、底部图标操作和 timed route 结束确认。
- E10.9 Timer Dial 参考风格 polish、Compose frame clock 秒间连续进度投影、paused / completed / abandoned 冻结、`+15秒` rest extension 单调进度、最后 N 秒 smoke 和 user-test debug APK 交付；`r-design.md` 是参考桥接文档，不替代官方 `DESIGN.md`。
- E10.9 用户测试反馈计划：Timer Dial 视觉减字 / 圆盘层级修复、计划保存持久化、huashu HTML 原型探索、声音提示系统和统计记录后续拆分。

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
11. MVP 阶段只支持三套内置 UI 皮肤切换；不做运行时插件市场、远程主题下载或第三方皮肤安装。
12. E10.1 后计时训练回归纯间歇计时器，不再绑定动作库，不做动作选择、动作详情或动作推荐。
13. 跟练训练和力量训练后续使用统一动作选择页；计时训练不进入动作选择页。
14. 三类训练执行页都必须遵守主操作即时可达原则，心率、说明、提示和下一步信息低于当前动作/阶段/时间/主操作层级。

精简决策记录见 `docs/planning/decision-log.md`。

## 仍待确定

以下事项在生产实现深入前仍需继续收敛：

1. 首批导入动作库的动作清单和内容深度已由 E1.1 收敛，详见 `docs/planning/action-content-slice.md`；`sourceMeta`/`extensions` 对齐策略已由 E1.2 记录到 `docs/planning/decision-log.md` 和 `docs/planning/data-contracts.md`。
2. 跟练后续不再通过“计时计划切换为跟练视图”解决完整编排，而是通过统一动作选择页选择动作，再形成热身、动作、休息、轮次、放松结构；完整跟练编排仍待 E10 后续实现。
3. 固定阶段词 cue 可在后续保留，但用户任意文本 TTS、语音读秒大范围能力和自动语音教练不属于第一版。
4. Android 工程脚手架 E0.1 已采用 `minSdk 26`、`compileSdk/targetSdk 36`、包名 `com.liujyks.trainflow`、Kotlin DSL、单 `app` module 起步。
5. 后续心率数据源策略和健康数据权限流；E11 已收敛为设备获取优先、手动输入可选补充、两者都没有时显示未获取心率。Health Connect / Wear OS / BLE / 厂商 SDK 的权限、来源、设备范围和失败状态仍需另行决策。
6. 各 story 的详细开发说明、测试清单和验收记录。
7. 官方默认 UI 是否首版同时提供暗色主题，还是先提供浅色工作区 + 深色训练执行页。
8. `docs/planning/data-contracts.md` 与 `prototype/src/data/contracts.ts` 的 `WorkoutCommand` 细节需要后续对齐；E0.3 已以文档和决策日志为准，保留文档中的 `update_actual_weight`、`update_actual_reps` 和更细的力量组完成/确认输入结构。
9. E10.2 已统一核对计时阶段最后 N 秒提醒边界：热身、工作、休息、放松和自定义阶段均复用 `CountdownCue` / `WorkoutEvent` 边界；声音素材和固定女声 cue 仍留给 E13。
10. 用户测试时核对训练提示音与其他 App 音频的共存行为。用户可能在训练中播放音乐或观看视频；TrainFlow 的短促提示音不应请求会降低、暂停或打断其他 App 音频的 audio focus，也不应主动执行 ducking。若不同 Android 版本或设备存在异常，应与测试问题一起归入后续音频适配修复。
11. E10.9 用户测试确认 `countdown_beep1.mp3` 可作为最后 N 秒前几声 beep 候选，`.local/audio/stage_bell_copper_clean.wav` 可作为最后 1 秒或阶段切换铃声候选；`.local` 原文件当前不能提交，后续若接入 App 由 E13 执行 story 复制到 `app/src/main/res/raw/`。
12. E10.10 已补齐计时/力量计划本地保存和恢复：计时阶段的名称、秒数、轮次、颜色、图标、类型和排序写入 `WorkoutPlan.blocks` 并通过 Room `workout_plans` 恢复；跟练当前没有保存入口，基础 preset 仍清楚标为待完整编排。
13. Timer Dial 后续视觉修复需要减少说明文字、放大总剩余时间与圆盘、调整环线粗细和 marker 层级、增加宽底层圆环与动态浅点，并简化中心圆内容。
14. E10.15 已建立 motion timing rules：动画服务训练节奏，不驱动 engine state、真实倒计时、session record、`pausedElapsedSec`、extra rest、`WorkoutCommand` 或 `WorkoutEvent`；reduce-motion 降级为 snap / disable non-essential motion / disable continuous projection。
15. E10.16 已把 motion token 最小落地到计时训练 ready gate、center dial、Timer Dial 状态变化和 `+15秒` 二段确认反馈；review fix 已补齐生产 reduce-motion source 与 UI 消费路径，动效仍只消费 UI state / engine state，不改变训练业务语义。

## 建议下一步

除非用户改变方向，建议按以下顺序推进：

1. E10.14 Rest Extension Semantics And Recording 已完成收口；计时训练 `+15秒` 只延长当前休息阶段，采用二段式确认防误触，并把确认成功的额外休息作为真实 session record 保存。
2. E10.13 Ready Start Gate 已完成；计时训练从编辑页或计划详情开始后先进入极简 ready gate，点击中心圆才真正 `StartSession`。
3. E10.12 Timer Dial Compose landing 已把 E10.11 `TrainFlow Official Fusion` 方向落到 Android 生产 Timer Dial：执行页减字、总剩余时间放大居中、圆盘放大、线条变细、宽底层圆环、同源动态浅点和阶段色中心圆；继续保留 continuous progress、pause freeze、terminal freeze、rest extension monotonic progress。
4. E10.16 Motion Landing 已完成；后续若继续训练执行页 polish，仍只消费既有 motion token，不改变训练语义、真实记录或倒计时口径。
5. E12 Stats / Records 已具备真实基础统计、非心率聚合图表、历史清理和计时同类阶段 / 额外休息趋势；后续若继续 E12，应优先进入 E12.2b 力量同类 set 趋势或另行拆分同日多轮分析，仍不得回填或修改原计划结构。平均心率趋势必须等待明确来源的设备心率或可选手动心率数据；如果两者都没有，历史页和趋势页显示未获取心率，不画假趋势。
6. E13 Sound Cue System 处理 `countdown_beep1.mp3`、`.local/audio/stage_bell_copper_clean.wav`、蓝牙耳机 / 手机扬声器 smoke、不申请会打断外部音频的 audio focus、不 duck、不暂停音乐 / 视频，以及后续女声 cue / 阶段名朗读。
7. E11 继续保持独立阶段，但不再默认手动心率先行；后续按“未获取展示 / 设备心率接入策略 / 可选手动输入”拆分，真实心率设备、Health Connect、Wear OS、BLE 或厂商 SDK 仍需另行决策。

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

E4 UI smoke fix 已完成并合入 `main`；`emulator-5554` UI 抽查已通过，力量训练从计划详情进入执行、单组确认、替换/跳过与结束态的关键 UI 路径完成收口。当前 E4 力量训练闭环已完成，后续已进入 Epic E5 的训练总结阶段。

E5.1 新增计时训练轻量总结；completed / abandoned 终态不再只显示轻量结束文案，而是展示 summary 面板。summary 消费 `TimedWorkoutEngine` 的 `activeElapsedSec`、`stepHistory`、`controlHistory`、`restExtensionHistory` 和 `earlyEnd`，展示总时长、完成阶段、步骤/轮次进度、跳过内容、休息延长、提前结束进度、训练部位摘要和禁用的“查看恢复建议”占位入口。总时长文案明确说明本阶段仍是 engine active elapsed，不等同真实 wall-clock startedAt / endedAt；本阶段仍不写入 Room session records、不实现历史/趋势、不实现 E5.4 完整恢复建议。

E5.2 新增力量训练轻量总结；completed / abandoned 终态不再只显示力量训练结束文案，而是展示 strength summary 面板。summary 消费 `StrengthWorkoutEngine` 的 `sessionElapsedSec`、`strengthSetRecords`、`stepHistory`、`controlHistory` 和 `earlyEnd`，展示动作数量、完成组数/计划组数、每动作组摘要、planned / actual 重量与次数、组耗时、实际休息、替换动作来源、跳过动作/组摘要、提前结束原因与进度，以及禁用的“查看恢复建议”占位入口。文案明确当前为引擎内存态记录，不等同真实 wall-clock session records；本阶段仍不写入 Room session records、不实现历史列表/趋势、不实现 E5.4 完整恢复建议、不生成自动加重量建议。

E5.3 新增训练历史与基础趋势首版内存态展示；官方 shell 的“记录”入口已启用并进入 `feature.history` 页面。历史页使用内存态 `WorkoutSession` seed 展示按日期分组的计时 / 力量 mixed session 列表、单次记录详情、单动作重量 / 次数历史，以及按总组数、总次数和实际重量 * 次数汇总的训练容量历史。页面和测试均明确当前不读取 Room session records，不接真实持久化业务闭环，不实现 E5.4 完整恢复建议，不生成自动加重量建议、表现判断、医疗结论、心率或热量判断。

E5.4 新增基础恢复建议首版实现；`core.data.fixture` 提供 5 个恢复区域 fixture，`core.domain.recovery` 根据已完成计时动作或已确认力量组的动作 recovery 映射生成去重且顺序稳定的 `RecoveryRecommendation`，`feature.recovery` 新增浅色恢复建议页，官方 shell 新增内存态 `RECOVERY` destination。E5.1/E5.2 summary 的“查看恢复建议”入口已变为可用，无法识别动作时保持诚实空状态。页面和测试均明确当前不读取 Room session records，不写入真实 `recovery_recommendations` 表，不接 repository 业务闭环，不生成自动训练建议、康复治疗承诺、医疗诊断、心率判断、热量判断或疾病适应性判断。

E6.1 新增跟练雏形计划入口；首页将“基础跟练”作为同层入口启用，官方 shell 新增 `FOLLOW_ALONG_ENTRY` destination，并在 `feature.followalong` 中提供基础跟练选择页。页面展示一个内存态 `WorkoutMode.FOLLOW_ALONG` preset，复用 `TimedCircuitBlock` / `TimedExerciseItem` 与 `FollowAlongPlanMeta(preset=true)`，动作只来自首批 fixture 中同时支持跟练和计时流程的动作；页面展示动作数、预计时长、动作短提示和媒体位边界。

E6.2 新增基础跟练执行页；E6.1 preset 的开始按钮进入 `FOLLOW_ALONG_SESSION` destination，训练中仍归属“训练”底部导航并锁定导航。执行页复用 `TimedWorkoutEngine` 推进动作、休息、倒计时、暂停、继续、跳过和提前结束，所有控制继续通过 `WorkoutCommand` 分发；UI 展示当前动作、演示/媒体占位、倒计时、阶段进度、动作短提示、下一动作预告、低层级心率占位、控制按钮和 fixture 动作详情。completed / abandoned 终态展示“基础跟练完成 / 提前结束”的轻量总结，并明确当前仍是引擎内存态，不写入真实 session records。当前仍不支持任意计时计划切换为跟练视图，不实现视频播放、真实媒体、自动语音、动作分析、真实心率设备、通知调度、Room session records 写库或完整课程能力；当时 O-002 未解决，E10.1 后已收敛为跟练后续通过统一动作选择页编排，不再依赖计时计划切换为跟练视图。

E6.3 新增心率抽象状态展示；`feature.workoutsession` 现在通过共享 `HeartRateDisplayUiState` / mapper 消费 `HeartRateState`，计时训练、力量训练和基础跟练执行页统一展示 disabled、not_connected、connecting、available、stale、error 六种状态。available 可显示 bpm，measuredAt、sourceId 和 message 只作为低层级辅助信息；`warningLevel` 继续只保留为模型字段，不驱动颜色、告警、训练状态或控制按钮。`core.health` 新增 `HeartRateProvider`、`DisabledHeartRateProvider` 和 `MockHeartRateProvider` 边界，仅输出抽象状态，不接 Health Connect、Wear OS、BLE、厂商 SDK、真实传感器或健康/蓝牙权限。当前训练闭环在禁用、未连接、连接中、数据中断或错误状态下仍完整可用。

E7.1 新增训练提醒通知首版实现；`core.notifications` 新增计划提醒请求、Android 13+ 通知权限状态映射、普通通知 channel/content、普通 alarm 调度接口和非导出 `PlanReminderNotificationReceiver`，manifest 仅新增 `POST_NOTIFICATIONS` 权限。`feature.plans` 在内存态计划详情中展示训练提醒设置，可用未来时间快捷设置或关闭 `PlanReminder`，并在通知权限关闭时展示“训练仍可正常使用、提醒暂不会弹出”的克制提示。新增单元测试覆盖权限映射、调度 disabled / missing schedule / past schedule / permission denied 边界、普通通知文案、manifest 负向权限和不使用 exact alarm / foreground service。当前仍不实现前台服务、后台训练可靠计时、闹钟级强提醒、真实 session records 持久化、语音、健康/传感器/蓝牙/定位权限或真实设备接入。

E7.2 新增活跃训练通知边界；`core.notifications` 新增 active workout notification contract、普通 ongoing channel/content、permission-gated policy 和 Android `NotificationManager.notify/cancel` 控制器。计时训练、力量训练和基础跟练执行 route 从各自 UI state / engine status 映射训练摘要，active / paused 时展示当前训练状态，completed / abandoned / route disposed 后清理通知。E7.2 明确首版不启用 foreground service：target 34+ foreground service 需要匹配类型和权限，而本阶段只做状态摘要，不适合冒用 data sync / media 类型；health 类型会牵出健康、传感器或活动识别权限，超出 MVP 禁区。新增测试覆盖 active / paused 展示、permission denied 不阻塞训练、terminal 清理、三类执行页摘要映射、manifest 负向权限和不启用 exact alarm / foreground service / 健康 / 传感器 / 蓝牙 / 定位权限。当前仍不实现后台精确计时系统、foreground service、notification action 控制训练、真实 `WorkoutSession` 持久化、session records 写库、语音或真实心率设备。

E7.3 新增训练偏好设置；`core.datastore` 的 `TrainFlowPreferencesDataSource` 现在可保存默认临近结束秒数、动作提醒、休息提醒、声音、震动、强化动画和力量训练本组计时默认模式，且会夹紧阈值与模式契约。训练首页新增“训练偏好”入口，官方 shell 新增非底部 tab 的 settings destination，设置页展示训练内倒计时反馈与 E7.1/E7.2 通知边界说明。新建计时计划会从偏好生成默认 `CueSettings`，新建力量计划会从偏好生成默认 `StrengthSetTimerMode`；已生成计划中的显式 `CueSettings` 和 block 级 set timer mode 不会被全局偏好静默覆盖。新增测试覆盖 DataStore 保存、设置 UI state、app 映射、计划编辑默认消费和 shell 入口。当前仍不实现新的通知调度、foreground service、exact alarm、notification action 控制训练、语音读秒、自动语音教练、真实 session records 持久化、真实心率设备或后台可靠计时。

E8.1 新增内置 UI 皮肤 contract 和 registry；`ui.theme` 现在注册 Official Flow、Tile Flow、Big Type 三套内置皮肤，包含 skin id、显示名、描述、目标用户、能力边界、默认标记和 token。`core.datastore` 新增 `uiSkinId` 偏好并把未知或非法 id 回退到 `official_flow`；设置页新增“UI 皮肤”选择入口，`MainActivity` 根据持久化偏好解析当前 skin 并传给 `TrainFlowTheme`，theme 层从 skin tokens 生成 Material color scheme。Official Flow 继续对应 `DESIGN.md` 官方默认方向；Tile Flow 和 Big Type 在 E8.1 只提供轻量 token 与 metadata 差异，不承诺完整页面重排。新增测试覆盖 registry、默认皮肤、非法 id 回退、DataStore 保存读取、settings 选项、app mapper 和 theme token 映射。当前仍不实现 Tile Flow 完整磁贴页面、Big Type 完整大字执行页、运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也不改变 `WorkoutCommand`、`WorkoutEvent`、`WorkoutPlan`、`WorkoutSession`、通知、心率、恢复建议或 `core.engine` 边界。

E8.2 新增 Tile Flow 关键页面磁贴式皮肤；`TrainFlowTheme` 通过 composition local 向真实页面暴露当前 skin，Tile Flow 扩展页面横向留白、分组间距、普通磁贴和主磁贴圆角 token，并在 `ui.designsystem` 提供可复用磁贴与指标条组件。Tile Flow 首页使用最大计时训练主磁贴、并列力量/跟练次级磁贴，以及动作库、最近计划、训练偏好、提醒状态和记录工作区；计划列表/详情用指标条表达动作、轮次、时长、休息和提醒；设置页按训练反馈、力量默认、UI 皮肤和通知边界分组；计时与力量执行页只调整当前皮肤的深色容器、间距、圆角和动作色，仍保持当前动作、倒计时/组目标和主按钮最高层级。动作库、计划编辑、跟练入口/执行、记录、恢复和总结细节继续沿用 Official Flow 页面组合；Official Flow 保持原有默认尺寸与颜色，Big Type 继续保持占位状态。新增测试覆盖 Tile Flow 布局 token、Big Type 占位、首页工作区入口和计划指标映射。当前仍不实现运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也不改变训练计划、记录、命令、事件、通知、权限、心率、恢复建议或 `core.engine` 边界。

E8.3 新增 Big Type 大字训练皮肤；`TrainFlowSkinTokens` 扩展关键字体、计时器、按钮高度、执行面板内边距和固定控制区预留 token，Big Type 首页使用最大计时训练入口、同层力量/跟练大入口和精简工具入口。计时执行页放大当前动作与倒计时并保持暂停/继续、跳过、`+15秒` 和结束训练固定可见；力量执行页放大当前动作、本组目标和组计时，并在 Big Type 下把开始/完成/确认本组、暂停/继续和结束训练统一放入固定底部区。力量确认层在 Big Type 下使用单列实际重量/次数输入并提高深色输入对比度。设置页更新 Big Type 说明；计划编辑、动作详情、计划管理、跟练、记录、恢复和总结细节继续沿用 Official/Tile 现有页面组合。720x1280 与常规尺寸模拟器检查确认两类执行页主控制即时可见，Official Flow 与 Tile Flow 抽查无回归。当前仍不实现运行时插件市场、远程主题下载、第三方皮肤安装、动态代码加载，也不改变训练计划、记录、命令、事件、通知、权限、心率、恢复建议或 `core.engine` 边界。

E8.4 新增 UI skin review checklist 与用户测试前 UI readiness；`docs/testing/ui-skin-readiness-checklist.md` 明确 Official Flow、Tile Flow、Big Type 三套内置皮肤审查标准、训练执行页固定主控制、720x1280 小屏检查、通知权限不可隐藏、心率非医疗化、恢复建议非医疗化、普通通知边界、E9 用户测试回看事项和社区 UI 定制要求。`docs/ui-extension-guide.md` 已补充 E8.4 review gate 与禁止范围，`docs/roadmap-backlog.md` 已记录 E8.4 交付结果和下一步 Review Gate / E9 用户测试准备。新增轻量 registry/readiness 测试覆盖三套内置 skin、Official Flow 默认、mode pill 对比度、metadata 完整性和未知 skin 回退。当前仍不实现运行时插件市场、远程皮肤下载、第三方皮肤安装、动态代码加载、第四套皮肤、通知/foreground service/exact alarm、语音、真实心率设备或 session records 持久化，也不改变训练计划、记录、命令、事件、权限、心率、恢复建议或 `core.engine` 边界。

E9.1 新增训练状态恢复与关键回归测试基线；`docs/testing/training-state-recovery-checklist.md` 明确暂停后返回、后台再前台、屏幕旋转或 Activity 重建、进程被系统杀死后的当前边界、completed / abandoned 终态防污染、三套 skin 小屏固定控制、最后 N 秒提醒覆盖回看、音频不 ducking、普通通知边界、心率非医疗化和恢复建议非医疗化。新增回归测试覆盖计时训练暂停/后台 tick/继续/休息延长/终态命令污染，力量训练确认草案/休息暂停/继续/终态 records 防污染，执行页主控制可达性，三套 skin 不改变训练 UI state 语义、固定控制 token 和 mode pill 对比度，以及倒计时提示音不请求 audio focus / ducking。当前仍不实现真实 `WorkoutSession` 持久化、Room repository 业务闭环、foreground service、后台可靠计时、exact alarm、notification action 控制训练、真实心率设备、语音或进程死亡恢复。

E9.2 新增权限与隐私文案收口；`core.model.PermissionPrivacyCopy` 统一通知权限、活跃训练通知、心率、恢复建议、音频提示、语音和数据边界文案，设置页新增“权限与隐私”说明区，计划提醒、active workout notification、三类训练页心率占位和恢复建议页均对齐当前能力边界。`docs/testing/permission-privacy-readiness-checklist.md` 记录用户测试前检查项。新增文案测试覆盖普通通知用途、通知关闭后训练仍可用、普通通知可能延迟、active notification 不是 foreground service、心率未接真实设备且非医疗判断、恢复建议非诊断/康复/疼痛处理、音频提示不承诺所有设备一致、语音未实现、数据仍多为内存态/fixture/基础展示。当前仍不新增权限，不实现 Health Connect、Wear OS、BLE、真实心率设备、语音控制、语音读秒、自动语音教练、foreground service、后台可靠计时、notification action 控制训练、真实 session records 持久化、云同步或账号体系。

E9.3 新增 MVP 用户测试前总验收清单和问题模板；`docs/testing/mvp-acceptance-checklist.md` 逐项汇总计时训练、基础跟练、力量训练、数字输入、记录/数据分析、心率、通知/声音/隐私、三套 UI skin、状态恢复和 MVP 非目标，并把用户反馈中的纯间歇计时器、跟练编排、独立动作选择页、总统计/图表、心率设备策略和数据分析趋势归入 E10/E11/E12 后续方向。`docs/testing/user-test-issue-template.md` 提供用户测试问题记录格式。E9.4 User Test Fix Pack 1 已将 E9.3 记录的 P1/P2 修复：计时编辑页热身、动作秒数、动作后休息、轮数、轮间休息和拉伸支持临时清空；力量编辑页重量、次数、组数和休息支持临时清空；空值时保存 / 开始禁用并显示原因；计时编辑页 `立即开始` 和力量编辑页 `开始力量训练` 可从有效草稿直接进入对应执行页，仍通过 `TimedWorkoutEngine` / `StrengthWorkoutEngine` 与 `WorkoutCommand` 推进。历史记录全部清除、按训练计划清除、按日期清除已登记为后续能力，当前不实现假删除。当前仍不实现 E10 训练模式重构、统一动作选择页、完整跟练编排、真实 `WorkoutSession` 持久化、Room repository 业务闭环、历史记录真实删除、Health Connect / Wear OS / BLE、语音或完整数据分析图表。

E10.1 新增训练模式边界与执行页交互原则记录；`docs/planning/e10-training-mode-interaction-plan.md` 明确计时训练回归纯间歇计时器，不再绑定动作库，不做动作选择、动作详情或动作推荐；计时训练由热身、工作、休息、放松和自定义阶段组成，阶段支持名称、时间、图标、颜色、添加、复制、删除和拖动排序，训练计划后续可支持主题色/配色。计时训练执行页后续以大圆盘作为核心视觉和主控制区，顶部总剩余时间保持辅助，中心显示阶段图标、阶段名称或编号和当前阶段倒计时，圆环表达整体进度、当前阶段进度和轮次/阶段位置，点击中心圆盘暂停/继续并记录暂停时长，记录后续应区分本次总耗时、有效训练时间和暂停总时长。E10.1 同时记录三类执行页主操作即时可达原则、跟练/力量统一动作选择页决策，以及用户测试反馈到 E10.4/E11/E12/E13 的分流。本阶段只改文档，不实现 E10.2 UI，不重写训练引擎，不新增真实记录持久化、Room repository 闭环、手动心率、真实心率设备、语音、TTS、音频资源、foreground service、notification action 或统计图表。

E10.2 新增计时训练纯间歇编辑页与大圆盘执行页首版实现；`feature.plans` 的计时编辑页不再消费动作 fixture 或动作库选择，改为阶段名称、阶段时间、阶段类型、图标 key 和颜色的阶段列表，支持添加、复制、删除、右侧手柄长按拖拽排序，并保留上移 / 下移排序作为备用路径。Review Gate 修复后，拖拽只由右侧手柄触发，热身固定在开头，放松固定在最后，中间阶段可排序；阶段卡提供内置颜色 swatch，阶段类型选择同步图标 key，计划主题色 / 整体配色编辑仍留作后续 polish。`core.model` 将 `TimedExerciseItem.exerciseId` 改为可选并新增 `TimedStageType`、`iconKey`、`colorHex`，`core.engine.TimedWorkoutEngine` 可从纯阶段生成执行步骤，`stageType=rest` 会进入真实休息步骤；暂停状态下 tick 不推进 `activeElapsedSec`，并用 `pausedElapsedSec` 记录暂停累计，route 暂停态也会继续推进暂停累计。计时执行页已改为深色大圆盘主视觉，顶部显示总剩余时间，中心显示阶段图标 key、阶段名称和当前阶段倒计时，圆环显示整体进度与当前阶段进度，点击中心圆盘暂停 / 继续，底部保留跳过、`+15秒` 和结束训练。`docs/planning/data-contracts.md` 与 `prototype/src/data/contracts.ts` 已同步纯阶段契约。当前仍不实现真实 `WorkoutSession` 持久化、Room repository 闭环、历史真实写入或删除、心率设备、Health Connect / Wear OS / BLE、foreground service、exact alarm、notification action、语音、TTS、音频资源、统计图表、跟练 / 力量 UI 重做。

E10.3 新增力量 / 跟练执行页主操作可达性修复；`feature.workoutsession` 为力量和基础跟练补充即时控制 metadata 与结束训练二次确认 UI state。力量执行页保留现有深色执行页和确认层，不做新版力量 UI 重做；开始本组、完成本组、确认本组、休息中提前开始本组、暂停 / 继续和结束训练进入固定底部控制区，当前动作 / 组计时 / 休息倒计时主面板也可作为暂停 / 继续入口。跟练执行页改为可滚动内容 + 固定底部控制区，暂停 / 继续、跳过 / 下一步和结束训练无需滚动到底部查找，倒计时区域也可暂停 / 继续。两类执行页的结束训练都先打开二次确认，确认后才分发 `WorkoutCommand.EndSession(reason = "user_requested")`；取消不会结束训练。新增回归测试覆盖力量 active/rest/confirm、跟练 active/paused、三套 skin 的 control metadata 保留和结束确认 reducer。当前仍不实现真实 `WorkoutSession` 持久化、Room repository 闭环、历史真实写入或删除、心率设备、Health Connect / Wear OS / BLE、foreground service、exact alarm、notification action、语音、TTS、音频资源、统计图表、完整跟练平台或力量新版 UI。

E10.4 新增训练记录闭环前置；`core.data.WorkoutSessionRepository` 通过 Room DAO / mapper 写入和读取本地真实 `WorkoutSession`，`workout_sessions` 记录 plan id、完整 MVP plan snapshot blocks、mode、status、startedAt / endedAt、total / effective / paused 秒数，`session_step_records` 保存计时 / 跟练步骤摘要，`strength_set_records` 保存力量训练已确认组的计划值、实际值、组耗时、实际休息和替换来源。E10.4 Review Gate 修复后，plan snapshot JSON 不再只保存 title / mode，读回后历史详情可从恢复后的 blocks 计算计划步骤/计划组数；力量训练 `totalElapsedSec` 采用 startedAt 到 endedAt 的 wall-clock 总耗时并包含 prepare / confirm 停留，`effectiveElapsedSec` 不包含暂停且当前不把 prepare / confirm 计入有效推进，`pausedElapsedSec` 单独保存暂停累计。计时、力量和基础跟练执行 route 在 completed / abandoned 终态首次出现时写入本地记录，并使用一次性 guard 与异常吞并边界避免重复插入或 Room 异常打断 UI；记录页生产入口改为读取 Room session records，示例 fixture 仅保留给 preview / 测试，不再覆盖真实记录。首页旧的 E5 记录 / 建议边界提示已清理。当前仍不实现历史记录清理、统计图表、趋势分析、云同步、账号体系、后台可靠计时、心率设备、Health Connect / Wear OS / BLE、foreground service、exact alarm、notification action、语音、TTS、音频资源、完整跟练平台或力量新版 UI。

E10.5 新增 Timer Dial 设计工作流与重构范围；`docs/planning/timer-dial-design-workflow.md` 明确 E10.4 已完成并合入 `main`，TrainFlow 已具备本地真实 session record write-through，E10.5 不再处理 Room / session repository / 记录闭环。外部 APK / 截图只用于观察和学习 UI / 交互，不复制代码、资源、图标、字体、音频或专有动画资产，目标是 TrainFlow 自己的圆盘语言。工具路线收敛为 Figma 静态界面与规格、可选 HTML / Canvas 动效验证、Jetpack Compose Canvas 生产实现；Rive / Lottie 只适合小图标或装饰动效，不用于核心计时进度。Timer Dial 规格包括顶部总剩余时间、外圈当前运动+休息周期、内圈总进度、中心圆当前阶段与暂停 / 继续、底部少量图标操作；E10.8 production 已收敛为跳过、`+15秒` 和结束，reset 只保留为 preview/demo 或未来命令设计项。动效包括阶段弧线推进、总进度推进、work / rest 颜色和粗细变化、阶段切换、暂停态和最后 N 秒提醒，且必须来自 engine state。后续拆为 E10.6 Figma / static visual variants、E10.7 Compose prototype、E10.8 production integration and animation polish；E12 统计图表 / 历史趋势与 E13 声音 / 女声 cue 保持独立。当前仍不实现生产 Kotlin、Gradle、prototype、统计图表、心率设备、语音 / TTS、音频资源、foreground service、exact alarm、notification action 或第四套 skin。

E10.6 新增 Timer Dial Figma / static visual variants 规格；`docs/planning/timer-dial-static-visual-variants.md` 按 Findings、E10.6 Design Scope、Timer Dial Static Frames、Editing Flow Static Frames、Interaction Animation Spec、Official Flow / Tile Flow / Big Type Adaptation、Accessibility And Small Screen Checks、Do Not Use / Legal Boundary、Suggested E10.7 Handoff Notes 和 Verification Notes 输出。执行页静态帧覆盖 active work、active rest、warmup / cooldown、paused、resume transition、stage transition、last-N-seconds cue、completed、abandoned、end confirmation 和 720x1280 小屏状态；计时编辑页关键状态覆盖 header、阶段列表、阶段行 / 阶段卡、添加阶段 sheet、复制、删除确认、颜色 / 图标 picker、快捷时长、时长细调、展开 / 收起、拖动排序、上移 / 下移、保存 / 取消反馈和小屏底部操作。规格明确顶部总剩余时间层级、外圈当前运动+休息周期、work / rest / warmup / cooldown 颜色区分、当前阶段粗弧、非当前阶段细弧、当前阶段弧线静态表达、内圈总进度、中心圆暂停 / 继续和底部少量图标操作；结束训练必须二次确认，最后 N 秒提醒不得遮挡主控制。E10.6 仍只改 Markdown / 设计文档，不实现 Android、不写 Kotlin、不改 Gradle、不改 prototype、不开始 E10.7、不复制 APK 代码 / XML / 资源 / 图标 / 字体 / 音频 / SVG / PNG / animated SVG / 动画参数 / 命名 / 逐像素视觉，不新增第四套 skin，也不混入 E11 心率、E12 统计或 E13 声音 / 女声 cue。

E10.7 新增 Timer Dial Compose prototype；`feature.workoutsession` 新增 `TimerDialUiState`、`TimerDialTokens`、`TimerDial` Canvas 组件和 `TimerDialPreview` demo，现有计时执行页以低风险方式消费 `TimedWorkoutEngineState.toTimedWorkoutSessionScreenState().timerDial` 展示外圈阶段结构、当前阶段推进、内圈总进度、中心自绘阶段符号、阶段编号 / 名称 / 倒计时、paused 冻结态和 final 5 seconds 轻量强调。三类 visual variant 已作为原型 token 实现：黑红高对比、赛博霓虹和 TrainFlow Official Flow 融合；它们不是新增 UI skin。新增单元测试覆盖 progress clamp、total / stage progress mapping、work/rest stroke semantics、visual variant 数量、final countdown flag 和 paused mapping。E10.7 仍是 prototype complete，不是 E10.8 最终生产集成；当前未改 Room/session repository、训练记录业务逻辑、workout engine 语义、声音 / TTS / 女声 cue、统计图表、心率设备、foreground service、exact alarm、notification action、前端 prototype 或第四套 skin。

E10.8 新增 Timer Dial production integration / animation polish；生产计时训练执行页默认使用 Official Flow Timer Dial，Black / Red High Contrast 与 Cyber Neon 仅保留为 preview/demo visual variants，不进入三套内置 UI skin registry。`TimerDialUiState` mapper 将外圈 segments 收窄为当前一次运动+休息周期：work active 时 work 粗弧填充、rest 细弧；rest active 时 rest 粗弧填充、已完成 work 细弧；外圈和内圈进度动画均使用线性推进，避免秒针式跳格。内圈不再画未经过底轨，而是按运动阶段数量从 12 点像画笔一样画出总进度：总数为 work/custom 运动阶段数量，一个阶段包含 work+rest；12 点数字圆标显示总运动阶段数，最新完成节点显示数字，之前完成节点退为实心圆点。计时执行页顶部精简为总剩余时间，圆盘卡移除重复阶段标签、计划标题、步骤和进行中状态，中心圆显示当前阶段倒计时并承担暂停 / 继续点击；底部跳过和结束改为图标按钮，结束训练接入二次确认，`+15秒` 只在 active rest 可用，用于延长当前休息 15 秒且不修改原计划。E10.8 Review Gate 修复后，rest extension progress 按“单调、不倒退、状态驱动”口径处理：`+15秒` 后当前 rest 外圈弧和内圈 work+rest cycle progress 不小于延长前，active tick 继续推进，paused 和 terminal 状态冻结。E10.8 production controls are `skip`, `+15秒`, `end`；reset 只保留为 preview/demo 或未来命令设计项，未来若实现需要先明确 `WorkoutCommand`、二次确认、session record 边界和测试。人工 APK/UI/动画/配色分析只吸收深色高对比、单一强强调色、约 100-300ms 轻量反馈、final countdown 轻量强调和 Canvas 自绘圆盘等抽象原则，不复制代码、资源、字体、音频、命名或逐像素视觉。新增/更新测试覆盖 production 默认 variant、preview-only variant 边界、current-cycle outer segments、当前阶段粗弧语义、rest extension progress 不倒退、paused / terminal 冻结、7 阶段 45+15 的内圈 marker progress、final countdown 偏好开关和 timed route end confirmation；已运行 `app:testDebugUnitTest`、`app:assembleDebug`、`app:lintDebug`、`app:check`、`git diff --check HEAD` 和 `git diff --cached --check`。720x1280 emulator visual smoke 覆盖 active、paused、rest + `+15秒`，截图保存在未跟踪的 `.local/verification/e10-8/`；最后 N 秒视觉 smoke 本轮未稳定截到 1-5 秒窗口，保留为 review 关注点。本阶段未改 Room/session repository、训练记录业务逻辑、workout engine 语义、声音 / TTS / 女声 cue、统计图表、心率设备、foreground service、exact alarm、notification action、前端 prototype 或第四套 skin。

E10.9 新增 Timer Dial reference polish / continuous progress / user-test APK；`r-design.md` 记录外部参考项目的只读设计发现和禁止复制边界，只作为参考桥接文档纳入分支，不替代 `DESIGN.md`。Timer Dial 生产绘制改用 Compose frame clock 对 active 状态做最多 1 秒的 bounded projection，让外圈当前阶段弧和内圈总进度在 engine 秒级 tick 之间连续推进；中心倒计时文字仍只消费 engine/UI state 的秒级文本。paused、completed、abandoned 和不可暂停/继续状态不投影；`+15秒` rest extension 后当前 rest 外圈弧和内圈 work+rest cycle progress 保持单调不倒退，并在 active 状态继续秒间推进。E10.9 保留 E10.8 production controls：skip、`+15秒`、end；不新增 reset command。本阶段是 Timer Dial 参考风格与连续动画 polish，并准备用户测试 APK，不进入 E11 心率数据源、E12 统计图表 / 历史趋势或 E13 声音 / 女声 cue；未改 Room/session repository、训练记录业务逻辑、workout engine 语义、声音 / TTS / 女声 cue、统计图表、心率设备、foreground service、exact alarm、notification action、前端 prototype 或第四套 skin。

E10.9 用户测试反馈计划新增 docs-only 记录；`docs/roadmap-backlog.md`、`docs/planning/e10-training-mode-interaction-plan.md`、`docs/planning/timer-dial-design-workflow.md` 和 `r-design.md` 已明确后续拆分：E10.9 Review Fix / User Test Fix 只做 Timer Dial 视觉减字、总剩余时间放大居中、圆盘放大、环线层级、底层宽圆环、动态浅点和中心圆简化，并保留 continuous progress、pause freeze、terminal freeze、rest extension monotonic progress；E10.10 Plan Persistence 处理自定义计时计划和各计划保存入口真实可用性；E10.11 Huashu Timer Dial Prototype 使用 `huashu-design` 做 3 个 HTML 高保真方向但不复制外部 APK 或参考项目资源；E13 Sound Cue System 处理 `countdown_beep1.mp3`、`.local/audio/stage_bell_copper_clean.wav`、蓝牙耳机 / 手机扬声器 smoke 和不打断外部音频；E12 Stats / Records 处理总统计、图表、明确来源心率趋势和同日多轮运动分析。本轮未修改 Kotlin、Gradle、prototype，未移动或提交 `.local/audio`、根目录 APK、`人工/`、build 输出、截图或日志，也未实现声音播放、计划保存或 Timer Dial 生产代码修改。

E10.10 Plan persistence and save-entry audit 已实现；`core.data.WorkoutPlanRepository` 接入 Room `workout_plans` 表，计划编辑页的“保存计划”可写入本地 `WorkoutPlan`，计划页消费本地 plans 并支持切换页面或重启后恢复详情。计时计划阶段名称、秒数、轮次、轮间休息、阶段类型、图标、颜色和排序保存在 `WorkoutPlan.blocks`，力量计划目标重量、次数、组、休息和替代候选继续作为计划目标结构保存；`WorkoutSession` 仍只保存实际执行结果与计划快照。计划页复制、删除、设置提醒和关闭提醒会同步写回本地计划，删除计划不改写已有训练历史快照。跟练当前没有保存按钮，只保留基础 preset 启动入口和待完整编排标识，不留下假保存入口。本阶段未改训练执行引擎语义，未实现声音播放、统计图表、真实心率设备、foreground service、exact alarm、notification action、prototype 前端或 Timer Dial 视觉，也未提交 `.local`、APK、`人工/`、deliverables、截图、日志或 build 输出。

E10.11 Huashu Timer Dial HTML prototype exploration 已实现并准备评审；`docs/prototypes/e10-11/index.html` 提供一个纯 HTML/CSS/Canvas 原型入口，可切换 Black / Red High Contrast、TrainFlow Official Fusion 和 Cyber Neon restrained version 三个方向，并覆盖 active work、rest、paused、final 5 seconds 和 rest extended by `+15` 五个状态。`docs/prototypes/e10-11/README.md` 记录三套方向的设计意图、建议进入 Android Compose 的元素、仅探索不建议进入 MVP 的元素、`TimerDialUiState` / Compose Canvas 映射、声音 cue 后续位置和资源边界声明。本阶段未修改 Android Kotlin、Gradle、React prototype、训练引擎、Room/session repository、声音播放、计划保存、统计图表、心率设备、foreground service、exact alarm、notification action 或第四套 skin；未复制外部 APK、`C:/Users/25073/Desktop/12/WorkoutTimer_Project`、`人工/` 或参考项目代码 / 资源 / 字体 / 音频 / SVG / PNG / vector path / 命名，也未提交 `.local`、APK、截图、日志或 build 输出。

E10.12 Timer Dial Compose landing 已将 E10.11 中最适合作为生产候选的 `TrainFlow Official Fusion` 方向落到 Android Compose Timer Dial。生产计时执行页移除“总剩余”文字标签、下一阶段提示框、提醒说明 / 已启用声音提示框和训练中控制历史提示；总剩余时间改为更大居中显示，圆盘整体放大，外圈 / 内圈线条同比例变细。`TimerDialTokens` 新增宽底层内圈和浅点 token，Official 默认映射继续来自 TrainFlow skin token；底层浅点和内圈阶段 marker 复用 `TimerDialUiState` 的同一套动态 marker 数据，阶段数量和轮次变化时同步变化。中心圆改为当前阶段色填充，内部只保留白色阶段图标、必要编号和阶段剩余时间；paused 状态冻结并用克制虚线 / 低饱和表达，final 5 seconds 保持轻量强调，rest extension 后外圈和内圈 progress 继续单调不倒退。Review fix 已重新布局 marker 轨道，所有内置 skin 的 center gap、outer gap 和 marker internal gap 均以 `3.5dp` 最小间距测试约束；暂停态中心圆保留整圆可点击继续语义和“继续训练”可访问文案。本阶段未接入声音播放、未复制音频到 `res/raw`、未实现 Stage color picker、motion timing rules、计划保存、统计图表、未改 Room/session repository、未接真实心率设备、未新增 foreground service / exact alarm / notification action、reset production command 或第四套 skin，也未提交 `.local`、APK、`人工/`、deliverables、截图、日志或 build 输出。

E10.13 Ready Start Gate 已实现；用户从计时编辑页“立即开始”或计划详情“开始计时训练”进入同一个 `TimedWorkoutSessionRoute` 后，route 初始停留在极简 ready/start gate，不自动 dispatch `WorkoutCommand.StartSession`。Ready gate 展示计划名、大中心圆、播放图标以及低层级预计时长 / 阶段数 / 轮数；点击中心圆任意区域才记录真实 startedAt、派发 `StartSession` 并进入既有 Timer Dial 执行页。ready 状态不推进 tick loop，不触发 countdown reminder / sound / haptics，不写 abandoned session record；completed / abandoned 本地记录仍只在真实启动后写入。新增测试覆盖 ready 不自动启动、不推进 activeElapsedSec、中心圆启动、ready 不触发反馈、不写废弃记录、真实启动后终态写入、编辑页与计划详情入口共用 ready gate，并保留 E10.9 / E10.12 的 continuous progress、pause freeze、terminal freeze 和 rest extension monotonic progress 回归边界。本阶段未实现 rest extension recording、motion timing rules、Stage color picker、声音播放 / 音频复制、统计图表、Room/session repository 新语义、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 prototype 前端改动。

E10.14 Rest Extension Semantics And Recording 已实现并完成收口；`WorkoutCommand.ExtendRest` / 生产 UI `+15秒` 明确为“延长当前 active rest step”，不插入新休息阶段，不修改原 `WorkoutPlan` 或 plan snapshot，不把额外休息伪装成 planned rest，也不增加 `pausedElapsedSec`。生产 UI 的 `+15秒` 加入轻量防误触：第一次点击只进入 `确认 +15秒` 待确认态，2 秒内第二次点击才 dispatch `ExtendRest` 并记录，超时自动恢复；确认成功短暂显示 `已加 15秒`，每个 rest step 最多确认 4 次 / 60 秒，达上限后禁用并提示“已额外休息 1 分钟，需要更久可以暂停训练”，但不自动暂停。`TimedWorkoutEngine` 的 rest extension history 现在记录 step index、round index、当前 rest 阶段、前一个 work/custom 阶段、addedSec、plannedRestSec、点击时剩余秒数、已休息秒数、当前 rest 累计 extra rest 和发生时 engine elapsed；completed 与 abandoned 终态通过 `WorkoutSession.timedRestExtensionRecords` 写入真实 Room session record。Room 升级到 version 4，新增 `timed_rest_extension_records` 表、DAO relation、repository round-trip、3→4 migration 和 schema 导出；repository 读回按 `eventElapsedSec -> stepIndex -> cumulativeExtraRestSec -> id` 稳定排序，覆盖同秒 10+ 条记录，避免字符串 id 把 `-10` 排到 `-2` 前。计时总结页展示最小额外休息摘要，例如 `额外休息 +30秒` 和按轮次 / 前序阶段汇总的明细。Ready gate 未真实启动时仍不会产生 rest extension record；Timer Dial rest extension monotonic progress、pause freeze 和 terminal freeze 保持回归测试边界。本阶段未实现 E12 统计图表 / 趋势分析、真实心率设备、motion timing rules、Stage color picker、声音播放 / 音频复制、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 UI 视觉重做。

E10.15 Motion Timing Rules 已实现；`ui.theme.TrainFlowMotionTokens` 集中定义触摸反馈 `100ms`、状态切换 `160ms`、局部布局 `220ms`、页面切换 `260ms`、continuous projection 最多 `1000ms`、reduce-motion `0ms` snap / disable fallback、touch scale、alpha 和 easing token。`TimerDial` final countdown pulse 已消费 motion token；continuous progress 仍只是 UI projection，文案数字和真实倒计时继续消费 engine / UI state。新增 `TrainFlowMotionTokensTest` 覆盖 duration range、命名用途、reduce-motion fallback 和 token 值边界，并保留 Timer Dial pause freeze、terminal freeze、rest extension monotonic progress 与 E10.14 `+15秒` 二段式确认回归。本阶段未实现 E10.16 Motion Landing、页面切换动画落地、Stage color picker、声音播放、统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 prototype 前端改动。

E10.16 Motion Landing 已实现；`feature.workoutsession` 新增集中 motion specs，将 ready gate -> execution 局部切换、ready gate center circle touch feedback、Timer Dial center dial touch feedback、play/pause glyph state transition、Timer Dial marker / ring / center color state transition 和 `+15秒` label / touch feedback 全部接到 E10.15 motion token，并提供 reduce-motion `0ms` snap spec。Ready gate 点击仍立即通过 `WorkoutCommand.StartSession` 启动；Timer Dial continuous progress 仍只是 bounded UI projection；paused / terminal 不推进真实进度；`+15秒` 仍第一次点击 pending、第二次确认才 dispatch `ExtendRest(15)`，每段 4 次 / 60 秒上限和记录语义不变。Review fix 补齐真实生产 reduce-motion path：root composition 读取系统 animation scale 并提供 `LocalTrainFlowReduceMotion`，计时训练 motion call site 显式消费该值，reduce-motion 时 ready/execution snap、触摸 scale / final pulse 关闭、Timer Dial continuous projection 不启动 frame loop。新增 / 更新测试覆盖 motion specs token usage、真实 reduce-motion source / call site 消费、reduce-motion fallback、ready gate motion 不启动 session、play/pause / rest extension 使用 state transition 而不是 local/page timing、`+15秒` motion 不改变二段确认规则，并保留 pause freeze、terminal freeze、rest extension monotonic progress 回归边界。本阶段未实现大型页面转场系统、Stage color picker、声音播放 / 音频复制、E12 统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 prototype 前端改动。

E10.17 Stage Color Picker 已实现；`core.model` 新增集中 `StageColorPreset` 色板，提供 6 个推荐色和覆盖 handoff 20 个 Material-like 色值的更多色，并为每个色块保留名称、色调、推荐用途、圆内文字色、高注意色标记和可访问标签。计时阶段编辑页从内联 8 色 swatch 升级为颜色选择对话框，阶段卡显示当前 swatch，可打开推荐色 / 更多颜色色板；选中态同时使用外圈、对勾和 TalkBack 文案，不只靠颜色表达。选择结果继续写入 `TimedExerciseItem.colorHex`，保存到 `WorkoutPlan.blocks` 后可通过 E10.10 本地持久化恢复；非法 `colorHex` 在编辑状态、JSON 读回和 Timer Dial UI state 中回退到阶段默认安全色。Timer Dial 外圈当前周期 segment 和中心圆现在消费保存后的阶段色，并使用 preset `textColor` 保证圆内文字 / 图标对比度。新增测试覆盖 preset 唯一性 / 合法 hex / 推荐色数量 / TalkBack 字段 / high-attention 标记、picker UI state、选择后计划映射、非法色 fallback、计划持久化 round-trip、Timer Dial 消费更新色，以及 E10.13 ready gate、E10.14 rest extension、E10.16 reduce-motion 相关回归。本阶段未新增第四套 skin、远程主题、运行时插件市场、统计图表、声音播放 / 音频复制、真实心率设备、foreground service、exact alarm、notification action、reset production command 或 `WorkoutCommand` / `WorkoutEvent` / engine / session record 语义变更。

E10.18 Plan Edit Backfill 已实现；计划详情页对计时和力量计划提供真实编辑入口，按 mode 分流到对应编辑页，follow-along 仍显示待完整编排且不出现假编辑入口。计时计划编辑器可从已保存 `WorkoutPlan` 回填 plan id、title / description、warmup / cooldown、阶段顺序、duration、round count、round rest、stage type、iconKey、colorHex 和 cue settings；保存时继续写回同一个 plan id，保留原 reminder，保留 `heartRateDisplay` 等非 cueSettings preferences，只替换当前编辑器管理的 cueSettings，并让计划详情、ready gate 和执行页消费更新后的计划。力量计划编辑器可回填 plan id、title / description、动作、目标重量、reps 区间 / 固定次数、正式组、热身组、组间休息、逐组覆盖和替代动作候选；保存时同样更新原计划并保留原 reminder / preferences。编辑已保存计划后，已有 reminder 路径会先取消该 plan id 的旧提醒，再按当前 reminder / permission / policy 重新调度；disabled 或 null reminder 会清理旧调度。复制计划仍创建新 id，删除 / 提醒 / 开始训练语义不变，历史 `WorkoutSession.planSnapshot` 不被编辑计划改写。本阶段未实现版本历史、撤销 / redo、云同步、完整跟练编排、统计图表、声音播放 / 音频复制、真实心率设备、foreground service、exact alarm、notification action、reset production command、第四套 skin 或 UI 视觉重做。

## 新 Codex 会话提示词

新会话可从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md、docs/readiness-report.md 以及 docs/planning 下的规划文档。
然后检查当前仓库状态与 prototype 原型，基于当前已接受的 MVP 基线继续推进 TrainFlow，不要静默扩大范围。
```
