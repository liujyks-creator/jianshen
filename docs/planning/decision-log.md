# TrainFlow 决策日志

**状态:** 活跃产品决策记录
**开始日期:** 2026-05-21

本日志记录应跨 Codex 会话、跨开发电脑保留下来的产品与实现边界决策。

## 状态标签

| 标签 | 含义 |
|---|---|
| Accepted | 当前基线，后续工作默认按此执行。 |
| Reserved | 预留接口或边界，但当前不交付完整能力。 |
| Open | 仍需继续决策或收窄首版定义。 |

## 已接受决策

| ID | 状态 | 决策 | 说明 |
|---|---|---|---|
| D-001 | Accepted | Android 首发，保留未来 iOS 路径。 | 健康数据、通知、音频和设备层要与训练业务逻辑保持可分离。 |
| D-002 | Accepted | 计时训练和力量训练都是首版并联能力。 | 计时训练不是力量记录的替代品。 |
| D-003 | Accepted | 计时训练作为默认推荐入口。 | 先降低新用户开始训练的理解成本。 |
| D-004 | Accepted | 计时训练同时支持动作倒计时和休息倒计时的临近结束提醒。 | 动作时间与休息时间都要有提醒状态。 |
| D-005 | Accepted | 临近结束提醒可配置。 | 默认最后 5 秒；声音、震动和强化动画属于首版提醒形式。 |
| D-006 | Accepted | 力量训练默认由用户主动开始本组计时。 | 默认控件是 `开始本组`；休息结束后自动计时仍可作为选项。 |
| D-007 | Accepted | 力量计划支持目标重量、目标次数、组数和休息。 | 默认目标次数区间为 `8-12`，也支持固定次数。 |
| D-008 | Accepted | 力量单组完成记录先引用计划值。 | 计划重量和次数先回填到实际记录，再由用户确认或修改。 |
| D-009 | Accepted | 记录力量单组耗时用于趋势和回顾。 | 首版先展示记录与趋势，不硬承诺自动加重量判断。 |
| D-010 | Accepted | 力量计划可表达动作内热身组与逐组目标。 | 训练中替换动作仍应保留。 |
| D-011 | Accepted | 动作库先定接口，再扩大内容规模。 | 动作数据要包含短提示、教学内容、能力标签、媒体位、替代动作和恢复映射钩子。 |
| D-012 | Accepted | 跟练作为首版雏形能力存在。 | 复用计时流程执行和动作内容。 |
| D-013 | Accepted | 基础恢复建议进入首版范围。 | 第一阶段是训练部位到恢复区域的文字或基础图文映射。 |
| D-014 | Accepted | 训练控制要有命令边界，训练状态变化要有事件边界。 | 便于 UI、未来语音、声音、震动、动画和分析解耦。 |
| D-015 | Accepted | 训练执行页预留实时心率展示位和抽象 UI 状态。 | 即使没有心率源，首版训练闭环也必须完整可用。 |
| D-016 | Accepted | Android 首版采用原生 Kotlin 与 Jetpack Compose。 | 业务核心与通知、音频、震动、健康数据等平台能力分离，未来保留 iOS 语义迁移边界。 |
| D-017 | Accepted | 首版采用本地优先架构。 | 计划、动作、会话和恢复建议优先保存在本地；云同步、账号体系和远端服务不进入 MVP。 |
| D-018 | Accepted | 训练执行引擎作为独立业务核心。 | UI 发送 `WorkoutCommand`，引擎产生状态和 `WorkoutEvent`，事件再驱动声音、震动、动画和通知。 |
| D-019 | Accepted | 首版训练提醒以普通通知为基线。 | 不把闹铃级强提醒作为 MVP 硬依赖；是否使用前台服务由活跃训练 story 单独验证。 |
| D-020 | Accepted | 首版 backlog 按工程地基、动作库、计划、计时训练、力量训练、总结恢复、跟练占位和硬化验收推进。 | 具体拆分见 `docs/roadmap-backlog.md`。 |
| D-021 | Accepted | 官方默认 UI 以 `DESIGN.md` 作为设计系统单一真源。 | 默认风格追求清晰、克制、可信和运动现场能量，训练执行页主信息优先。 |
| D-022 | Accepted | 开源社区可以定制 UI shell、主题、首页布局和按钮位置。 | 定制不得改变 `WorkoutCommand`、`WorkoutEvent`、训练执行引擎、数据契约和权限/健康边界。 |
| D-023 | Accepted | MVP 阶段不做运行时插件市场或远程主题下载。 | 开源定制先通过 fork、编译期主题/shell 和社区 PR 实现。 |
| D-024 | Accepted | E0.1 通过实现准备检查，但必须先确认 Android 工程参数。 | `docs/readiness-report.md` 是进入 Android 工程前的 readiness gate；当前允许启动 E0.1，不代表全量 MVP story 已全部无条件开工。 |
| D-025 | Accepted | 首批动作内容切片确定为 11 个动作，优先支持计时训练默认入口，同时覆盖力量训练最小闭环。 | 详见 `docs/planning/action-content-slice.md`；E1.1 只定义内容、字段、审核标准和 fixture 输入，不实现动作库导入、训练引擎、repository 或 UI 闭环。 |
| D-026 | Accepted | E1.2 以 `docs/planning/data-contracts.md` 为准补齐 prototype `Exercise` 的 `sourceMeta`/`extensions` 字段，并在 Android fixture 中写入 `sourceMeta`。 | `extensions` 在首批 fixture 中保持为空；训练类型支持、计时默认建议、力量默认建议和审核备注保留在 fixture-only 元数据中，不静默扩展核心 `Exercise` 契约。 |
| D-027 | Accepted | E7.2 首版不启用 foreground service，只提供普通 ongoing active workout notification 边界。 | Android 14+ foreground service 需要匹配类型和权限；当前训练状态摘要不适合冒用 data sync / media 类型，health 类型会牵出健康、传感器或活动识别权限，超出 MVP 禁区。活跃训练通知只展示 UI/engine state 摘要，completed / abandoned / route disposed 后清理，不承诺后台精确计时。 |
| D-028 | Accepted | MVP 阶段支持三套内置 UI 皮肤注册与本地切换，但不做运行时插件市场、远程下载或第三方皮肤安装。 | E8.1 只建立 Official Flow、Tile Flow、Big Type 的 skin contract、registry、DataStore preference、设置入口和 theme token 映射。皮肤只能改变 UI 表现、布局倾向和 token，不能改变训练计划、训练记录、`WorkoutCommand`、`WorkoutEvent`、训练执行引擎或权限/健康边界。 |
| D-029 | Accepted | 计时训练回归纯间歇计时器，不再绑定动作库。 | 计时训练由热身、工作、休息、放松和自定义阶段组成；阶段支持名称、时间、图标和颜色。阶段图标表达阶段类型或状态，不提供动作内容指导。计时训练不进入动作选择页，不做动作选择、动作详情或动作推荐。 |
| D-030 | Accepted | 计时训练执行页以后以大圆盘作为核心视觉和主控制区。 | 顶部可显示总剩余时间但不抢主层级；中心显示当前阶段图标、阶段名称或编号和当前阶段倒计时；圆环表达整体进度、当前阶段进度和轮次/阶段位置；点击中心圆盘暂停/继续，并记录暂停时长。 |
| D-031 | Accepted | 三类训练执行页都必须遵守主操作即时可达原则。 | 看到时间或动画的位置，就是可以控制训练节奏的位置。暂停/继续、结束、跳过/下一步、开始本组、确认本组不能藏到滚动后；结束训练可即时可达但必须二次确认；心率、说明、提示和下一步信息保持辅助层级。 |
| D-032 | Accepted | 跟练和力量训练后续使用统一动作选择页，计时训练不使用。 | 统一动作选择页承担搜索、分类、推荐、动作详情预览、多选和已选顺序管理。跟练用它选择动作并形成热身、动作、休息、轮次、放松结构；力量用它选择动作后回到力量编辑页设置重量、次数、组数和休息。 |
| D-033 | Accepted | E10.5 是 Timer Dial 圆盘视觉语言的 docs-only 规划 story，不再处理 E10.4 记录闭环。 | E10.4 已完成并合入 `main`，本地真实 `WorkoutSession` write-through 已具备。E10.5 不改 Room、DAO、session repository、记录页数据源、生产 Kotlin、Gradle 或 prototype。 |
| D-034 | Accepted | Timer Dial 重构采用“Figma 静态规格 / 可选 HTML Canvas 动效验证 / Jetpack Compose Canvas 生产实现”的路线。 | Figma 用于静态界面、风格方案、颜色、组件和布局规格；HTML / Canvas 只可选用于快速验证圆盘动画和阶段弧线节奏；Android 最终用 Compose Canvas，并且核心进度必须实时绑定 engine state。Rive / Lottie 只适合小图标或装饰动效，不用于核心计时进度。 |
| D-035 | Accepted | 外部 APK / 截图只能作为 Timer Dial 研究素材，目标是 TrainFlow 自己的圆盘语言。 | 可以观察 UI / 交互和节奏，但不得解析或复制代码、资源、图标、字体、音频、专有动画资产或逐像素视觉；APK、截图、录屏、反编译输出和研究临时产物不得提交。黑红高对比只是参考方向，不新增第四套 skin。 |
| D-036 | Accepted | E10.6 先输出 Timer Dial 静态视觉帧和计时编辑页关键状态规格，再进入 Compose 原型。 | E10.6 至少覆盖 Official Flow 的执行页状态帧和计时编辑页状态帧，并说明 Tile Flow / Big Type 适配；所有进度、暂停、完成、废弃和最后 N 秒提醒都必须绑定 `TimedWorkoutEngine` / UI state / `WorkoutEvent`，不得使用视觉假进度。本阶段不写 Kotlin、不改 Gradle、不改 prototype、不新增第四套 skin，也不混入 E11/E12/E13。 |
| D-037 | Accepted | E10.8 生产 Timer Dial 外圈只表达当前一次运动+休息周期，内圈表达按运动阶段数量推进的整次训练进度。 | 外圈当前阶段弧线按线性动画匀速填充；处于 work 阶段时 work 为粗弧、同周期 rest 为细弧，处于 rest 阶段时 rest 为粗弧、已完成 work 退为细弧。内圈不画未经过底轨，只像画笔一样沿圆弧匀速画出总进度；12 点位置用数字圆标显示总运动阶段数，每个运动阶段包含 work+rest，完成的阶段节点显示数字或圆点。暂停 / 继续由中心圆点击触发，跳过和结束使用底部图标，结束仍需二次确认；黑红高对比和赛博霓虹仅保留为 preview/demo 变体，不进入 UI skin registry。 |
| D-038 | Accepted | E10.9 用户测试反馈拆为独立后续 story，不混入已完成 polish。 | Timer Dial 视觉减字、总剩余时间放大居中、圆盘放大、环线层级、宽底层圆环、动态浅点和中心圆简化进入 E10.9 Review Fix / User Test Fix，并保留 continuous progress、pause freeze、terminal freeze、rest extension monotonic progress。计划保存真实持久化和保存入口 audit 进入 E10.10；`huashu-design` HTML 高保真原型进入 E10.11；`countdown_beep1.mp3`、`.local/audio/stage_bell_copper_clean.wav`、蓝牙 / 扬声器 smoke 和不 duck / 不抢占外部音频进入 E13；总统计、图表、平均心率趋势和同日多轮运动分析进入 E12。本轮 docs-only，不提交 `.local`、APK、`人工/`、build 输出或音频资源。 |
| D-039 | Accepted | 计时训练从编辑页或计划详情开始后先进入 ready/start gate，用户点击中心圆才真正开始训练。 | Ready gate 是 route/UI 启动边界，不是 `WorkoutSession` 的 completed / abandoned / paused 状态；ready 期间不推进 engine tick、不触发 countdown reminder / sound / haptics、不写 abandoned session record。真实启动仍通过 `WorkoutCommand.StartSession` 进入 `TimedWorkoutEngine`，completed / abandoned 记录只在用户实际启动后写入。 |
| D-040 | Accepted | 计时训练 `+15秒` 表示延长当前休息阶段，并作为实际会话记录保存。 | `WorkoutCommand.ExtendRest` 只作用于 active rest step；生产 UI 用二段式确认防误触，第一次点击只显示 `确认 +15秒`，2 秒内第二次点击才加时并记录，超时不记录。每个 rest step 最多确认 4 次 / 60 秒，达到上限后禁用并提示“已额外休息 1 分钟，需要更久可以暂停训练”，但不自动暂停。该命令不插入新休息阶段，不修改原 `WorkoutPlan` 或 plan snapshot，不把额外休息伪装成 planned rest。额外休息是用户主动增加恢复时间，区别于暂停；它继续计入训练 active / total 用时，不增加 `pausedElapsedSec`，并通过 `timedRestExtensionRecords` 独立保存发生 session、round、step、前一个阶段、计划休息、点击时机、addedSec 和累计额外休息，供 E12 后续分析。Ready gate 未真实启动时不能产生额外休息记录。 |
| D-041 | Accepted | TrainFlow motion timing 采用集中 token，并且动画只消费状态不驱动训练。 | 触摸反馈使用 `80-120ms`，状态切换 `120-180ms`，局部布局 `180-240ms`，页面切换 `220-300ms`，Timer Dial continuous projection 最多投影 `1000ms` 且只作为 UI projection。所有训练动效必须可中断、状态驱动、支持 reduce-motion snap / disabled fallback；不得驱动 engine state、倒计时、session record、`pausedElapsedSec`、额外休息记录、`WorkoutCommand` 或 `WorkoutEvent`。E10.15 只定义规则和 token，不落地完整 Motion Landing。 |

## 预留能力

| ID | 状态 | 能力 | 当前边界 |
|---|---|---|---|
| R-001 | Reserved | 语音交互 | 保留训练命令与事件接口，供未来语音输入和语音输出复用。 |
| R-002 | Reserved | 大量课程内容 | 首版不建设课程运营平台。 |
| R-003 | Reserved | 教练视频课程体系 | 保留媒体字段和跟练元数据，但不让视频课程成为首版依赖。 |
| R-004 | Reserved | 自动语音教练 | 仅保留提示和事件钩子。 |
| R-005 | Reserved | AI 实时动作纠错 | 仅保留后续分析扩展点。 |
| R-006 | Reserved | 完整音乐节拍编排 | 仅保留计时与跟练扩展点。 |
| R-007 | Reserved | 基于实时心率的告警和热量修正 | 保留抽象状态和平台边界，不让设备接入阻塞训练闭环。 |
| R-008 | Reserved | 固定阶段语音 cue | 后续可为计时阶段开始预留 `warm up`、`work`、`rest`、`cool down` 等固定词。第一版不做用户任意文本 TTS 或自动语音教练。 |

## 待决策项

| ID | 状态 | 问题 | 影响 |
|---|---|---|---|
| O-001 | Accepted | 首批导入哪些动作，内容深度到哪里？ | 已按 D-025 收敛为 11 个动作与内容审核标准；E1.2 基于 `docs/planning/action-content-slice.md` 导入 fixture。 |
| O-002 | Accepted | 跟练首版只支持预置流程，还是允许兼容的计时计划切换为跟练视图？ | E10.1 后计时训练回归纯间歇计时器；跟练后续不再依赖“计时计划切换为跟练视图”，而是通过统一动作选择页选择动作，再形成热身、动作、休息、轮次、放松结构。 |
| O-003 | Accepted | 首版是否播放语音读秒，还是只保留语音接口？ | E10.1 收敛为后续只预留固定阶段词 cue；第一版不做用户任意文本 TTS、自动语音教练或语音读秒大范围能力。 |
| O-004 | Accepted | 训练日程提醒是否要强于普通通知？ | 已按 D-019 收敛为普通通知基线；强提醒暂不进入 MVP。 |
| O-005 | Accepted | Android 具体架构和模块拆分是什么？ | 已按 D-016、D-017、D-018 和 `docs/architecture.md` 收敛。 |
| O-006 | Open | 后续健康数据与可穿戴设备的接入策略是什么？ | E10.1 将手动心率输入归入 E11，并保留真实设备接口；Health Connect、Wear OS、BLE 或厂商 SDK 仍需在 E11 或独立设备阶段继续决策。 |
| O-007 | Accepted | E1.2 如何处理 `sourceMeta`/`extensions` 与 prototype contract 的差异？ | 已按 D-026 收敛：prototype contract 补齐字段，fixture 写入来源信息，扩展与默认建议不进入核心动作模型。 |

## 来源文档

更完整的论证当前位于：

1. `docs/planning/product-brief.md`
2. `docs/planning/prd.md`
3. `docs/planning/ux-design.md`
4. `docs/planning/data-contracts.md`
5. `docs/architecture.md`
6. `docs/roadmap-backlog.md`
7. `docs/readiness-report.md`
8. `DESIGN.md`
9. `docs/ui-extension-guide.md`
10. `docs/planning/action-content-slice.md`
11. `docs/planning/e10-training-mode-interaction-plan.md`
12. `docs/planning/timer-dial-design-workflow.md`
13. `docs/planning/timer-dial-static-visual-variants.md`
14. `r-design.md`

当来源文档改变了已接受的产品或架构方向时，同步更新本决策日志。
