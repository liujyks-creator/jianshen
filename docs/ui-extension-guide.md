---
workflowType: ui-extension-guide
projectName: TrainFlow
documentLanguage: zh-Hans
status: draft
date: 2026-06-06
inputDocuments:
  - DESIGN.md
  - docs/architecture.md
  - docs/roadmap-backlog.md
  - docs/planning/data-contracts.md
  - docs/testing/ui-skin-readiness-checklist.md
stepsCompleted:
  - customization-boundaries
  - ui-shell-contract
  - contribution-rules
  - ui-skin-review-checklist
---

# TrainFlow 开源 UI 定制指南

**目标:** 允许开源社区制作不同风格的 App 主界面、主题、页面组合和按钮位置，同时保护训练核心逻辑、数据契约和安全边界。

## 1. 定制愿景

TrainFlow 的核心是训练计划执行引擎，不是某一种固定 UI。  
官方版本提供一套优雅、克制、专业的默认设计；开源社区可以在不破坏训练语义的前提下创建不同 UI shell。

社区可以探索：

- 更轻量的居家训练首页。
- 更偏力量训练的健身房首页。
- 暗色优先主题。
- 大字版无障碍训练界面。
- 不同按钮位置、底部导航结构或 dashboard 组合。

但所有 UI 变体都必须尊重同一套训练数据、命令、事件和权限边界。

## 2. 可自由定制的内容

| 层级 | 可以定制 | 示例 |
|---|---|---|
| Theme tokens | 颜色、字体、圆角、间距、动效节奏 | 暗色主题、暖色主题、大字主题 |
| UI shell | 首页布局、导航结构、dashboard 信息排序 | 力量训练优先首页、计时训练优先首页 |
| Feature composition | 页面入口组合和默认 tab | 首页显示最近计划或今日训练 |
| Component skin | 按钮样式、卡片样式、chip 样式 | 更扁平、更硬朗、更柔和 |
| Training layout | 训练执行页信息排布 | 主按钮底部、倒计时居中、心率隐藏 |
| Content presentation | 动作详情、恢复建议、历史趋势展示 | 列表、图表、分段控件 |

## 3. 不能破坏的核心边界

社区 UI 不得：

1. 直接绕过训练执行引擎写入 `WorkoutSession`。
2. 改变 `WorkoutCommand` 的语义。
3. 改变 `WorkoutEvent` 的语义。
4. 删除力量训练单组确认流程中的实际记录确认。
5. 把计划值和实际值混为一个字段。
6. 把未来健康数据或心率边界描述为医疗诊断或危险状态判断。
7. 在未实现的情况下展示自动语音教练、AI 纠错或完整课程平台入口。
8. 跳过通知、健康数据或设备权限说明。
9. 让训练执行页无法一眼看到当前动作、剩余时间或当前组目标。

## 4. 推荐 Android UI 边界

Android 工程建议保留以下 UI 层边界：

```text
ui:designsystem
ui:shell-official
ui:theme
feature:home
feature:plans
feature:exercise-library
feature:workout-session
feature:history
feature:recovery
```

### 4.1 `ui:designsystem`

- 官方 token、组件、图标使用规则。
- 读取或映射 `DESIGN.md` 中的语义 token。
- 提供 `TrainFlowButton`、`TrainingPrimaryButton`、`PlanCard`、`ExerciseChip`、`SessionTimer` 等基础组件。

### 4.2 `ui:shell-official`

- 官方首页布局。
- 官方底部导航。
- 官方页面组合。
- 可被社区 fork 或替换。

### 4.3 `ui:theme`

- 主题 token 映射。
- 明暗主题策略。
- 社区主题可新增在独立 package/module 中。

### 4.4 Feature UI

Feature 页面可以调整布局，但必须通过 ViewModel 和 use case 与核心层交互。

计划编辑 / 计划详情类 UI polish 还应保留以下语义：

- 计划详情可采用可折叠计划播放列表，但 `开始训练`、编辑、复制和 `删除当前计划` 必须归属当前展开计划卡片，不能放成像作用于全部计划的全局操作。
- 计划颜色是用户手动设置的计划级颜色，不从首个阶段、阶段内部目标或力量目标组自动推断；社区 layout 可以改变入口样式，但不能改变颜色来源语义。
- 计时目标、计时内部阶段、力量目标组和计划颜色应复用同一类推荐色 / 更多颜色大色板交互，不能把颜色选择退化为多个难扫描的文字选项。
- 若某类颜色当前没有持久化字段，UI 可以提供安全展示或后续入口说明，但不得为了 polish 静默新增 Room schema、改变 `WorkoutPlan` / `PlanBlock` 语义或把颜色硬塞进不相干字段。
- 编辑页 sticky action 若存在，`保存计划` 是主确认动作，`开始训练` 是次级启动动作；不要用红色实心开始按钮盖过保存计划。
- 力量目标组可以默认折叠，但不得删除重量、次数、休息和计划值预填实际记录的语义。

## 5. UI Shell 合同

一个可接受的 UI shell 必须提供：

1. 训练首页入口。
2. 计划列表入口。
3. 动作库入口。
4. 训练记录入口。
5. 启动计时训练路径。
6. 启动力量训练路径。
7. 活跃训练恢复入口。
8. 设置或偏好入口。

UI shell 可以改变入口排序，但不能让计时训练、力量训练或活跃训练恢复不可发现。

## 6. 训练执行页合同

E10.1 后，计时训练回归纯间歇计时器。下表中计时训练的“当前动作”在 E10.2 后应理解为“当前阶段”，即热身、工作、休息、放松或自定义阶段；计时训练不再要求展示动作库动作、动作详情或动作推荐。

E10.5 后，计时训练执行页的 Timer Dial 圆盘视觉语言可以被 skin 或社区 layout 适配，但只能改变表现，不能改变训练语义。外圈阶段弧线、内圈总进度、中心倒计时、暂停 / 继续、跳过 / 下一阶段和结束训练都必须来自计时训练 engine state / UI state / `WorkoutEvent`，不能使用视觉假进度或绕过 `WorkoutCommand`。

E14.6-2 后，完成训练不应继续停在执行页大圆盘和执行态卡片上；completed terminal state 应进入“本次数据统计复盘页面”。页面顶部明确 `已完成` 并有克制庆祝感，中部先展示关键数据摘要，再复用既有训练总结 / 数据总览 / session summary，rest extension、skipped、pause 和 early-end 信息只能来自既有 summary / session record 映射，底部主动作推荐 `返回训练首页`。`查看记录` 只能作为低层级次入口候选，不能与返回形成两个主按钮。大 TimerDial 不应作为 completed 主视觉；如保留圆盘元素，只能作为小型完成徽章或训练类型标识。`abandoned` 可复用 recap shell，但必须是 `已结束` / `提前结束` 语气，不显示 completed celebration。该页面仍不得改 `WorkoutSession`、session records、E12 trends、E14.6-3 stage color/icon system、心率边界或训练命令 / 事件语义。

任何训练执行页变体都必须展示：

| 信息 | 计时训练 | 力量训练 |
|---|---|---|
| 当前动作 | 必须 | 必须 |
| 主时间 | 动作/休息倒计时 | 本组耗时或休息倒计时 |
| 当前阶段 | 动作/休息/暂停 | 准备/进行/确认/休息 |
| 下一步 | 下一动作或下一轮 | 下一组或下一动作 |
| 主控制 | 暂停/继续/跳过/结束 | 开始本组/完成本组/确认/休息控制 |
| 心率位 | 首版隐藏 | 首版隐藏 |

E11.3 后，首版训练执行页不显示心率位、不显示未获取心率占位，也不提供手动心率输入。后续如果重新评估健康设备或心率展示，必须先更新产品决策和权限 / 数据边界，并且不能挤压当前动作、主时间、下一步和主控制。

## 7. 主题贡献要求

社区主题应包含：

1. 主题名称。
2. 设计意图。
3. 颜色 token 映射。
4. 字体和字号策略。
5. 训练执行页截图或说明。
6. 可读性自检。
7. 是否支持浅色、暗色或系统跟随。

主题必须满足：

- 正文对比度不低于 WCAG AA。
- 训练主倒计时对比度足够高。
- 主要按钮和危险按钮视觉上可区分。
- 语义色不能只靠颜色表达，应配合文字或图标。

## 8. 页面布局贡献要求

社区 layout 应说明：

1. 它面向哪类用户。
2. 改动了哪些入口和排序。
3. 是否影响首版默认入口“计时训练”。
4. 活跃训练恢复入口在哪里。
5. 是否保留力量训练入口同屏可见或同层可达。
6. 是否改变训练执行页主按钮位置。

布局可以创新，但不能牺牲训练中可操作性。

## 9. 推荐贡献路径

1. 先 fork 或新建社区 UI shell。
2. 不改 `core:model`、`core:engine`、`core:domain` 的语义。
3. 用官方 fixture 跑通计时训练和力量训练。
4. 对照 `DESIGN.md` 和本文档自检。
5. 提交主题或 layout 文档、截图和测试说明。

E14.4 起，功能级 UI polish 还必须先经过视觉方案 gate：先提交 docs-only / mock-only 方案，说明当前问题、方案 A / B、推荐方向、真机确认点和实现拆分；用户确认后，才进入生产代码实现、回归测试、APK 和真机确认。视觉方案阶段不得改 Kotlin / Compose / Room / 测试代码，不得生成实现 APK。

E14.4-2 已确认采用方案 B 做计划编辑 / 详情结构优化，但计时阶段内部目标扩展 + TimerDial 外圈时间比例语义必须拆成独立 story 先规划 / 视觉确认。任何 UI shell 或社区 layout 都不得在普通 plan polish 中静默引入阶段内部目标的新数据结构、Room schema 变更或 TimerDial 外圈语义变更。

E14.4-2b visual / semantic gate 与 E14.4-2b-2 data model decision 已确认阶段内部目标结构是产品语义而不是普通视觉分组，并已在 E14.4-2b-3 到 6c 完成 editor-side v2 payload、adapter-expanded deterministic timeline、minimum engine bridge、adapter-expandable start gate、session record compatibility、TimerDial production mapping 和 smoke / visual QA，详见 `docs/testing/e14-4-2b-closeout.md`。社区 UI 仍只能调整表现，不能改变以下语义：轮次与轮间休息仍保持当前编辑器上侧位置；阶段编排仍在下方，且只表示每轮内重复 stageGroups；长期数据方向是 versioned timed composition payload，优先存入现有 `WorkoutPlan.blocks` JSON / `WorkoutSession.planSnapshot` JSON；阶段总时长由内部目标时长求和；阶段折叠头不得塞入目标数、可添加容量或公式文案；展开后的阶段内目标标题行应直接提供添加目标入口，并说明阶段时长来自目标时长相加；目标行的设置 / 收起入口必须和拖拽入口分开；阶段和目标排序以拖拽手柄为主，不显示重复的上移 / 下移备用入口；每个阶段最多 5 个目标；颜色选择位置直接显示色块，不把中文颜色名作为主要选项文字。有效且 adapter-expandable 的 v2 计划可以进入 ready gate / execution；unsupported 或 empty v2 仍必须 fail closed。TimerDial 保持原圆盘 UI，v2 外圈表达当前 stage group 内部 targets 并按 planned duration ratio 分段；内圈继续表达整次训练总进度；中心圆继续表达当前 active target / stage 和暂停 / 继续主控制；12 点数字圆标沿用整次执行 timeline 的总阶段数语义，按 warmup + rounds * stageGroups + between-round rests + cooldown 计算；`+15秒` 仍只延长当前 active rest step，不插入新阶段或 target、不修改 plan snapshot、不改变 `timedRestExtensionRecords`。旧计划和旧 snapshot 必须通过兼容 adapter 解释，不能因为 UI skin 或 layout 打开页面就静默改写。

E14.6 stage style / icon planning 进一步约束：热身、放松和轮间休息都是阶段化显示面，允许阶段色和内置图标 fallback；轮数只是重复结构，不需要自己的颜色或图标。第一版阶段 / 目标图标只使用项目内置白色 icon key，不支持用户上传图片、自定义图片库、远程图标资源或图片路径持久化。UI skin 可以改变图标呈现方式、描边、尺寸或动效，但不得改变 `iconKey`、`colorHex`、target -> stageGroup -> boundary default -> type default fallback、训练引擎、TimerDial mapping、session record、命令或事件语义。若未来要让 warmup / cooldown / between-round rest 的样式成为用户可编辑持久化字段，必须先拆 data contract / model decision；不得通过 skin 自行增加 Room schema 或上传资产存储。

E14.6-1 若修复 TimerDial normal motion 下外圈 / active segment 的每秒前跳再回弹，只允许处理 progress monotonic / continuous behavior。不得借该修复重做 outer-ring semantic mapping、Canvas geometry、stage count、engine timeline、Room schema、session records、`WorkoutCommand` 或 `WorkoutEvent`。

## 10. UI Skin Review Checklist

合并 UI 定制、内置 skin 调整或社区主题 PR 前，应先对照 `docs/testing/ui-skin-readiness-checklist.md` 记录审查结论。最小检查项如下：

### 10.1 训练闭环

- 是否仍能创建计时计划。
- 是否仍能创建力量计划。
- 是否能完整执行计时训练。
- 是否能完成力量训练单组确认，且计划重量和次数仍预填实际记录。
- 是否保留训练总结入口。
- 是否保留恢复建议入口。
- 是否没有直接写数据库绕过引擎。

### 10.2 三套内置皮肤

- **Official Flow** 是否继续符合 `DESIGN.md` 的官方默认方向。
- **Tile Flow** 是否保持磁贴语言，同时不牺牲训练执行效率。
- **Big Type** 是否保持远距离大字可读，同时不把主控制挤出首屏。
- 三套内置 skin 是否都仍在 registry 中，且 Official Flow 仍是默认。
- 未知或非法 skin id 是否回退到 Official Flow，不能直接进入 UI。
- 三套 skin 的 metadata 是否保留名称、描述、目标用户和能力边界。
- 三套 skin 的 mode pill 对比度是否达到 WCAG AA。

### 10.3 训练执行页

- 720x1280 小屏下，计时执行页的当前动作/休息状态、主倒计时、暂停/继续、跳过、`+15s` 和结束训练是否即时可见。
- 720x1280 小屏下，力量执行页的当前动作、本组目标、开始/完成/确认本组、暂停/继续和结束训练是否即时可见。
- 当前首版是否没有心率卡片、未获取心率占位或手动心率输入；未来健康数据不得高于当前动作、倒计时、组目标和主按钮。
- Timer Dial 是否仍显示顶部总剩余时间、外圈阶段结构、内圈总进度、中心当前阶段倒计时和少量底部图标操作。
- Timer Dial 的阶段弧线推进、总进度推进、暂停态和最后 N 秒提醒是否由真实 engine state 驱动。
- Completed terminal state 是否已离开大 TimerDial 执行页，进入本次数据统计复盘页，并保留底部返回。
- Abandoned / ended early 是否没有被误标为 `已完成` 或显示 completed celebration。
- 跟练雏形是否仍复用计时流程和动作内容，没有暗示完整课程平台。

### 10.4 权限和健康边界

- 通知权限说明是否可见，不能被主题、布局或折叠层隐藏。
- 普通通知不能写成闹铃级强提醒、全屏强打断或后台精确计时承诺。
- 心率不得写成医疗告警、危险状态判断、疾病监测或训练中断依据。
- 恢复建议不得写成康复治疗、医疗诊断或治疗承诺。
- 未实现能力不得展示为可用入口，包括自动语音教练、AI 纠错、真实心率设备和完整课程平台。

### 10.5 用户测试回看事项

- E9 用户测试应核对热身、动作、动作后休息、轮间休息、放松/拉伸是否都应支持最后 N 秒提醒。
- 训练提示音不得降低、暂停或打断其他 App 音乐/视频，不主动执行 ducking；异常设备表现应记录为后续音频适配 issue。

## 11. 内置皮肤切换边界

E8.1 开始，App 可以在设置页切换三套内置 UI 皮肤：

1. **Official Flow**：当前 `DESIGN.md` 官方默认皮肤。
2. **Tile Flow**：清爽、模块化的磁贴式内置皮肤；E8.2 已适配训练首页、计划列表/详情、设置页，并对计时与力量训练执行页做轻度适配。
3. **Big Type**：远距离可读的大字训练皮肤；E8.3 已适配训练首页、计时执行页、力量执行页和力量确认层，并在设置页说明实际覆盖范围。

内置皮肤切换只允许改变 UI 表现、布局倾向和 theme token。它不能改变 `WorkoutPlan`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、训练执行引擎、通知调度、权限说明、心率非医疗化文案或恢复建议非医疗化边界。

未知或非法 skin id 必须回退到 Official Flow，不能让远程、第三方或拼写错误的 id 直接进入 UI。Tile Flow 在 E8.2 只改变视觉、入口组合和布局倾向：训练首页可使用不同大小磁贴表达优先级，计划页可用平铺指标表达动作、轮次、时长、休息和提醒，设置页可按偏好形成分组磁贴；不得嵌套卡片，不得把训练执行页拆成大量零碎磁贴。动作库、计划编辑、跟练入口/执行、记录、恢复和总结细节当前继续沿用 Official Flow 页面组合。

Tile Flow 与 Big Type 的可复用组件与 token 应集中在 `ui.designsystem` / `ui.theme`，真实页面通过当前 skin 状态选择布局表现。Big Type 重点放大当前动作、主倒计时/组目标和主按钮，并使用滚动内容 + 固定底部控制区保护小屏可操作性；历史、次级提示和未来健康数据必须保持辅助层级，且当前首版不得恢复心率显示。计时执行页固定控制区保留暂停/继续、跳过、`+15s` 和结束训练；`+15s` 的待确认态可显示 `确认+15s`，但不得改变底部三个按钮外框高度、触摸高度或相邻按钮位置。力量执行页固定控制区保留开始/完成/确认本组、暂停/继续和结束训练。

Big Type 不要求所有页面大字化。计划编辑、动作详情、计划管理、跟练入口/执行、记录、恢复和总结细节当前继续沿用 Official/Tile 现有页面组合或只消费轻量 token，避免大字号破坏信息密集页面可用性。力量确认层可以调整输入排布以保护小屏使用，但不能删除实际记录确认或改变计划值预填语义。

## 12. 暂不支持的插件能力

MVP 阶段不做运行时插件市场，不做远程主题下载，不做用户在 App 内安装第三方 UI 包。

首版开源定制方式是：

- fork。
- 编译期选择 UI shell 或主题。
- 提交社区主题或 layout PR。

禁止通过 skin 或社区 layout 引入动态代码加载、远程皮肤下载、第三方皮肤安装、运行时插件市场、自动语音教练、AI 实时动作纠错、完整课程平台、真实心率设备或医疗告警入口。运行时主题市场只能在核心训练闭环稳定后另行评估，并需要更新决策日志和相关规划文档。
