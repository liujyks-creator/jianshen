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

## 待决策项

| ID | 状态 | 问题 | 影响 |
|---|---|---|---|
| O-001 | Accepted | 首批导入哪些动作，内容深度到哪里？ | 已按 D-025 收敛为 11 个动作与内容审核标准；E1.2 基于 `docs/planning/action-content-slice.md` 导入 fixture。 |
| O-002 | Open | 跟练首版只支持预置流程，还是允许兼容的计时计划切换为跟练视图？ | 影响计划元数据、导航和编辑规则。 |
| O-003 | Open | 首版是否播放语音读秒，还是只保留语音接口？ | 影响音频素材、设置和测试。 |
| O-004 | Accepted | 训练日程提醒是否要强于普通通知？ | 已按 D-019 收敛为普通通知基线；强提醒暂不进入 MVP。 |
| O-005 | Accepted | Android 具体架构和模块拆分是什么？ | 已按 D-016、D-017、D-018 和 `docs/architecture.md` 收敛。 |
| O-006 | Open | 后续健康数据与可穿戴设备的接入策略是什么？ | 影响权限、适配层、可靠性承诺和支持设备范围。 |
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

当来源文档改变了已接受的产品或架构方向时，同步更新本决策日志。
