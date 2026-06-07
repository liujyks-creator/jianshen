# TrainFlow 项目状态

**状态日期:** 2026-06-07
**仓库:** `liujyks-creator/jianshen`
**主分支:** `main`

## 当前状态

TrainFlow 已经具备首版产品基线、UX 基线、初始数据契约草案、Android 首版架构草案、MVP roadmap/backlog 草案、实现准备检查报告、官方设计系统草案、开源 UI 定制边界草案、一个 React/Vite 前端原型，以及 E0.1 Android 生产工程骨架、E0.2 Android 模块/包边界、E0.3 核心 Kotlin 模型映射、E0.4 Room/DataStore 持久化基础骨架、E1.1 首批动作内容切片、E1.2 首批动作 fixture、E1.3 只读动作库列表与筛选、E1.4 只读动作详情、E2.1 Android 首页与训练入口、E2.2 计时计划编辑基础、E2.3 力量计划编辑基础、E2.4 计划列表/详情/复制/删除基础、E3.1 计时训练执行引擎、E3.2 计时训练执行页、E3.3 临近结束提醒、E3.4 暂停/跳过/延长休息/提前结束控制历史边界、E4.1 力量训练执行引擎、E4.2 力量训练执行页、E4.3 单组完成确认层、E4.4 动作替换与跳过、E4 UI smoke fix、E5.1 计时训练总结、E5.2 力量训练总结、E5.3 训练历史与基础趋势、E5.4 基础恢复建议、E6.1 跟练雏形计划入口、E6.2 基础跟练执行页、E6.3 心率抽象状态展示、E7.1 训练提醒通知、E7.2 活跃训练通知边界、E7.3 训练偏好设置、E8.1 内置 UI 皮肤 contract / registry、E8.2 Tile Flow 关键页面磁贴式皮肤、E8.3 Big Type 大字训练皮肤、E8.4 UI skin review checklist 与用户测试前 UI readiness、E9.1 训练状态恢复与关键回归测试基线、E9.2 权限与隐私文案收口、E9.3 MVP 验收清单与用户测试问题模板、E9.4 用户测试修复包 1，以及 E10.1 训练模式边界与执行页交互原则记录。

项目已经从早期头脑风暴进入 Android 工程脚手架和 MVP story 实施阶段。`Story E0.1: 创建 Android 生产工程` 已按默认工程参数落地：包名 `com.liujyks.trainflow`、Gradle Kotlin DSL、Jetpack Compose + Material 3、单 `app` module 起步。`Story E0.2: 建立模块与包边界` 已在单 `app` module 内收敛核心/feature/UI/platform package 边界，并用轻量架构测试约束明显的反向依赖；物理 Gradle module 拆分继续留到代码体量需要时再做。`Story E0.3: 映射核心 Kotlin 模型` 已在 `core.model` 包内落地 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState` 和恢复建议相关契约。`Story E0.4: 建立 Room 与 DataStore 基础` 已在 `core.database` 与 `core.datastore` 包内落地最小可编译持久化骨架、Room schema 导出和 smoke test。`Story E1.1: 定义首批动作内容切片` 已收敛 11 个首批动作、必填字段、训练类型适配、指导内容边界、恢复/替代映射草案和审核标准。`Story E1.2: 导入动作 fixture` 已将 11 个首批动作导入 Android fixture，并用 fixture 校验测试约束 ID、必填字段、能力标签、默认建议、恢复映射和替代动作边界。`Story E1.3: 动作库列表与筛选` 已基于 E1.2 fixture 落地只读 Compose 列表、训练类型/身体部位/器械/难度筛选、清除筛选、空状态和动作摘要卡片。`Story E1.4: 动作详情` 已基于 E1.2 fixture 和 E1.3 列表入口落地只读动作详情，展示短提示、设置与执行步骤、发力要点、常见错误、呼吸提示、安全说明、替代动作和恢复映射。`Story E2.1: 首页与训练入口` 已建立轻量官方 shell、训练首页、计时训练推荐默认入口、力量训练同层入口和可进入 E1.3/E1.4 的动作库入口。`Story E2.2: 计时计划编辑` 已让首页计时推荐入口进入内存态计时计划编辑页，支持计划名称、热身/拉伸时长、动作时长、动作后休息、轮数、轮间休息、动作/休息临近结束提醒阈值和提醒形式开关，并能生成符合 `WorkoutPlan` / `TimedCircuitBlock` / `TimedExerciseItem` / `CueSettings` 契约的本次草稿预览；E2.2 retro fix 已关闭计时提醒阈值边界问题。`Story E2.3: 力量计划编辑` 已让首页力量训练入口进入内存态力量计划编辑页，支持计划名称、力量动作选择、目标重量、默认 `8-12` 次区间、固定次数、正式组数、动作内热身组、组间休息和逐组目标展开编辑，并能生成符合 `WorkoutPlan` / `StrengthExerciseBlock` / `StrengthExerciseTarget` / `StrengthSetPlan` 契约的本次草稿预览。`Story E2.4: 计划列表、详情、复制与删除` 已在官方 shell 中启用“计划”入口，复用 E2.2/E2.3 的 `WorkoutPlan` 草稿契约种子化内存态计划集合，支持计划列表、详情、复制和删除确认。`Story E3.1: 计时训练执行引擎` 已在 `core.engine` 内落地纯 Kotlin 状态机，可从有效计时计划/快照展开动作、休息和轮次步骤，支持开始、暂停、继续、跳过、延长休息和提前结束命令，并产出动作开始、动作临近结束、休息开始、休息临近结束、暂停、继续和完成事件。`Story E3.2: 计时训练执行页` 已在 `feature.workoutsession` 中新增计时训练执行 UI state、Compose route 和深色执行页，从现有内存态计划详情仅启用计时计划开始入口，执行页复用 E3.1 `TimedWorkoutEngine` 展示当前动作/休息、主倒计时、轮次/步骤进度、下一步、动作短提示和辅助心率占位，并通过 `WorkoutCommand` 支持暂停、继续、跳过、延长休息和结束训练。`Story E4.1: 力量训练执行引擎` 已在 `core.engine` 内新增纯 Kotlin `StrengthWorkoutEngine`，可从有效力量计划/快照按动作和组推进准备、开始本组、完成本组、确认记录、组间休息和完成/废弃终态。`Story E4.2: 力量训练执行页` 已在 `feature.workoutsession` 中新增力量训练执行 UI state、Compose route 和深色执行页，从内存态力量计划详情启用开始入口，执行页复用 E4.1 `StrengthWorkoutEngine` 展示当前动作、本组目标、组耗时、最小确认、休息倒计时和下一组目标，并通过 `WorkoutCommand` 支持开始本组、完成本组、按计划确认、暂停/继续、休息中提前开始下一组和结束训练。`Story E4.3: 单组完成确认层` 已将 Confirm 状态升级为可编辑确认层，展示计划重量、计划次数、本组耗时、组类型、动作名和组序号，默认回填实际重量/次数，支持次数区间快捷选择与 easy / good / hard / form_breakdown 主观感受，并通过 `WorkoutCommand.ConfirmStrengthSet` 生成正式 `StrengthSetRecord`。`Story E4.4: 动作替换与跳过` 已让力量训练支持通过 `WorkoutCommand.ReplaceExercise` 替换当前动作并在 `StrengthSetRecord.substitutedFromExerciseId` 保留原动作引用，也支持通过 `WorkoutCommand.SkipStep` 跳过当前动作剩余未完成组并继续后续动作。`Story E5.1: 计时训练总结` 已在 `feature.workoutsession` 中新增计时训练 summary UI state / mapper，消费 E3.4 的 step history、control history、rest extension history 和 early-end 记录，在 completed / abandoned 终态展示总时长、完成阶段、步骤/轮次进度、跳过内容、休息延长、提前结束进度、训练部位摘要和恢复建议入口；`Story E5.2: 力量训练总结` 已在同一 feature 边界新增 strength summary UI state / mapper，消费 E4.1-E4.4 的 strength records、history、replacement 和 skip 边界，在 completed / abandoned 终态展示动作、组数、重量、次数、组耗时、实际休息、计划/实际差异、替换/跳过摘要和恢复建议入口；`Story E5.3: 训练历史与基础趋势` 已启用内存态历史和基础趋势；`Story E5.4: 基础恢复建议` 已启用训练后恢复建议入口；`Story E6.1: 跟练雏形计划入口` 已启用基础跟练入口和选择页，展示一个只使用 supportsFollowAlong 动作的内存态 preset；`Story E6.2: 跟练执行页` 已让 preset 进入基础跟练执行页；`Story E6.3: 心率抽象状态展示` 已统一三类执行页的低层级心率状态展示，`Story E7.1: 训练提醒通知` 已建立计划提醒普通通知边界。当前仍未引入真实 `WorkoutSession` 持久化、Room/DataStore repository 闭环、前台服务、后台训练可靠计时、真实心率设备、语音能力、视频播放或任意计时计划切换为跟练视图。

`Story E4.1: 力量训练执行引擎`、`Story E4.2: 力量训练执行页`、`Story E4.3: 单组完成确认层`、`Story E4.4: 动作替换与跳过`、E4 UI smoke fix、`Story E5.1: 计时训练总结`、`Story E5.2: 力量训练总结`、`Story E5.3: 训练历史与基础趋势`、`Story E5.4: 基础恢复建议`、`Story E6.1: 跟练雏形计划入口`、`Story E6.2: 跟练执行页`、`Story E6.3: 心率抽象状态展示`、`Story E7.1: 训练提醒通知`、`Story E7.2: 活跃训练通知边界`、`Story E7.3: 训练偏好设置`、`Story E8.1: Skin contract and registry`、`Story E8.2: Tile Flow 完整视觉重做`、`Story E8.3: Big Type 完整视觉重做`、`Story E8.4: UI skin review checklist`、`Story E9.1: 训练状态恢复与回归测试`、`Story E9.2: 权限与隐私文案`、`Story E9.3: MVP 验收清单` 和 E9.4 用户测试修复包 1 均已合入 `main`。当前阶段分支 `codex/e10-1-training-mode-interaction-decisions` 基于 `main` / `origin/main` 的 `7dd7169b7119ab05a54a10481441bb55d3889b53` 实施 E10.1：只记录训练模式边界与执行页交互原则，不实现 E10.2 UI、不重写训练引擎、不新增真实记录持久化、心率设备、语音或统计图表。

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
- E10.1 用户测试后训练模式边界、计时训练纯间歇计时器方向、大圆盘执行页原则、统一动作选择页决策、执行页主操作即时可达原则，以及 E10.2/E10.3/E10.4/E11/E12/E13 后续拆分。

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
5. 后续心率数据源策略和健康数据权限流；E11 先考虑手动心率输入并保留真实设备接口，Health Connect / Wear OS / BLE / 厂商 SDK 仍需另行决策。
6. 各 story 的详细开发说明、测试清单和验收记录。
7. 官方默认 UI 是否首版同时提供暗色主题，还是先提供浅色工作区 + 深色训练执行页。
8. `docs/planning/data-contracts.md` 与 `prototype/src/data/contracts.ts` 的 `WorkoutCommand` 细节需要后续对齐；E0.3 已以文档和决策日志为准，保留文档中的 `update_actual_weight`、`update_actual_reps` 和更细的力量组完成/确认输入结构。
9. E10.2 重做计时训练时统一核对所有阶段的最后 N 秒提醒：热身、工作、休息、放松和自定义阶段都应有一致的提醒边界。
10. 用户测试时核对训练提示音与其他 App 音频的共存行为。用户可能在训练中播放音乐或观看视频；TrainFlow 的短促提示音不应请求会降低、暂停或打断其他 App 音频的 audio focus，也不应主动执行 ducking。若不同 Android 版本或设备存在异常，应与测试问题一起归入后续音频适配修复。

## 建议下一步

除非用户改变方向，建议按以下顺序推进：

1. 完成 `codex/e10-1-training-mode-interaction-decisions` 分支 Review Gate。
2. 下一轮进入 E10.2：计时训练编辑页与执行页重做，按纯间歇计时器、大圆盘、阶段图标/颜色、拖动排序和暂停记录推进。
3. 后续按 E10.3/E10.4/E11/E12/E13 依次处理力量/跟练主操作可达性、真实记录闭环、手动心率输入、真实统计图表和声音/女声 cue。

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

## 新 Codex 会话提示词

新会话可从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md、docs/readiness-report.md 以及 docs/planning 下的规划文档。
然后检查当前仓库状态与 prototype 原型，基于当前已接受的 MVP 基线继续推进 TrainFlow，不要静默扩大范围。
```
