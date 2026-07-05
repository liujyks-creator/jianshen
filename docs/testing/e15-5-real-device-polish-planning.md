# E15-5 Real-device Polish Planning Gate

**日期:** 2026-07-03
**状态:** Planning / design gate complete; E15-5a / E15-5b / E15-5c / E15-5d completed and merged
**范围:** docs-only planning; no Kotlin / Compose / Room / tests / APK changes

> 2026-07-05 收口：本 planning gate 拆出的 E15-5a、E15-5b、E15-5c 和 E15-5d 均已完成并合入 main。E15-5d merge commit 为 `0fa28463e4c24bf039944402a209f8f55c922c1b`，story commit 为 `d9875bd48cd3e51b560c677efc3f6d4440efc89a`，用户 APK 测试已通过。后续不要再从本文进入 E15-5d；维护同类问题先读 `docs/testing/e15-maintenance-lessons-learned.md`，下一阶段进入 MVP Alpha readiness 或按新增真机反馈拆 User Test Fix Pack 2。

## Inputs

本轮基于用户 2026-07-03 真机截图和反馈：

- 1s / 2s 短 target 的 TimerDial 外圈仍能看到一小段加速感。
- 力量计划编辑页“本组计时模式”两枚选项横向并排，长文案在窄屏突出边界。
- 力量训练完成页缺少明显返回入口；`返回计划` 目前在长复盘内容底部，需要滑动才能到达。
- 力量目标组里的“目标组颜色 / 后续保存”入口设计意图不清；用户后续确认当前 MVP 不需要目标组颜色能力，应删除该占位入口。
- 力量执行页当前动作短提示（例如“哑铃贴近胸前，稳定下蹲站起。”）在力量训练中价值偏低；力量训练大多为器械训练，用户可依靠动作名称识别动作，自定义动作也只需要用户输入名称，不需要扩展复杂提示配置。

## Findings

### 1. TimerDial Short-target Motion Is Not Closed

E15-2 已修掉固定 1 秒 catch-up 的显性逻辑，但文档也记录了短 target smoothness 没有生产路径 smoke：当时 seeded smoke plan 没有 1s / 2s production target，短 target 主要靠 focused tests 和 frame evidence 证明。用户真机反馈说明问题未关闭。

下一轮不能继续猜测公式。必须先建立可复现的视觉 / frame 证据，再修实现。

建议拆为 **E15-5a TimerDial short-target motion diagnostic + fix gate**：

- 先构造可运行的 1s / 2s timed composition 计划，从生产执行页进入真实 TimerDial。
- 采集 720x1280 AVD screenshot / screenrecord / frame-sample 证据，并尽量补真机主观复测。
- 输出每帧或近似采样的 displayed active segment progress，检查 progress delta 是否稳定、单调、无首帧 snap、无 tick anchor 后突进。
- 若诊断能复现加速段，修复才可进入同一 story 的 implementation；若无法复现，必须停止并报告证据差异，不宣称修复。

验收重点：

- 1s target 在 0 / 250 / 500 / 750 / 1000ms 的视觉采样接近匀速。
- 2s target 首秒和 tick anchor 后第二秒的 active segment progress delta 接近一致。
- `TimerDial` progress 仍来自 engine / UI state，不用 fake progress。
- 不改 outer-ring planned-duration ratio、Canvas geometry、inner total progress、12 点数字圆标、`+15s`、Room、commands/events、records、声音或心率 / 设备边界。

### 2. Strength Set Timer Mode Selector Overflows

当前代码把“手动开始下一组”和“休息后自动开始下一组”放在同一行可横向滚动 `FilterChip` 中。图 1 显示第二个长标签在窄屏右侧被截断，用户会以为控件突出卡片边界。

建议拆为 **E15-5b Strength set timer mode selector layout polish**：

- 推荐把并排 chip 改成竖向 radio-card / selector list。
- 每个选项展示短标题、必要说明和选中态，不横向滚动。
- 选中态使用边框 / 背景 / check 或状态文字，不只靠浅紫色填充。
- 文案继续隐藏 `manual_start` / `auto_after_rest` raw token。

验收重点：

- 720x1280 下两个选项完整可见，不突出卡片边界。
- 小屏和 Big Type 下长中文不裁切、不需要横向滚动。
- 保存语义仍写回 `StrengthExerciseBlock.setTimerMode`，训练偏好仍只影响新建 / 编辑默认值。
- 不改 StrengthWorkoutEngine、Room、声音、TimerDial 或记录语义。

### 3. Strength Completion Needs A Fixed Return Action

图 2 里的力量完成页顶部有 `完成` 状态，但真正可离开的 `返回计划` 在 `StrengthTerminalPanel` 内部，跟随复盘内容滚动，长内容时必须滑到底部。完成训练后用户应该能立即退出训练闭环，不应该在复盘卡里找返回。

建议拆为 **E15-5c Strength completion sticky return action**：

- 在 strength completed / abandoned terminal 状态增加固定底部 action surface，位置类似计划编辑页 sticky `保存计划 / 开始训练`。
- 主动作建议统一为 `返回训练首页` 或继续沿用路由语义为 `返回计划`，但必须固定在底部导航 / 安全区上方。
- 滚动内容增加 bottom padding，避免统计卡片被固定 action 遮挡。
- `查看记录` 如未来加入，只能作为低层级次入口，不能和返回形成双主按钮。

验收重点：

- 720x1280 首屏无需滚动即可看到完成状态、关键复盘内容和固定返回主动作。
- 滚动到复盘底部时，固定返回动作不遮挡最后一行内容。
- completed 和 abandoned / early-ended 语气分开；abandoned 不显示完成庆祝。
- 不改 session record、Room、StrengthWorkoutEngine、commands/events、records/history/completion summary 数据语义、声音、TimerDial 或心率 / 设备边界。

### 4. Strength Plan Editor / Execution Should Remove Low-value Placeholders

当前“目标组颜色 / 后续保存”更像未完成能力的占位入口：它只画固定珊瑚色圆点，没有真实保存、回填或训练识别价值。用户已确认当前 MVP 不需要继续实现目标组颜色，直接删除该入口即可。后续如果重新需要颜色能力，再单独做 model / serializer decision，不在本轮 UI polish 中扩展 `StrengthSetPlan`、plan snapshot JSON 或 Room。

力量执行页的动作短提示在计时 / 跟练中仍有价值，但在力量训练中容易变成低价值说明文字：力量训练大多围绕器械和动作名称执行，用户也可能输入自定义动作名称；如果为了每个自定义动作补短提示，会把简单的力量计划编辑变成复杂内容管理。当前 MVP 应以动作名称、组序号、组类型、重量、次数和休息作为主信息，去掉力量执行页中的动作短提示展示。

建议拆为 **E15-5d Strength editor and execution simplification**：

- 删除力量计划编辑页每个目标组里的“目标组颜色 / 后续保存”入口。
- 删除颜色入口后，目标组编辑区不能留下右侧空白格；推荐将“本组休息秒数”改为全宽，或采用重量 / 次数并排、休息独占一行的稳定布局。
- 删除力量执行页 active / prepare / rest 当前组卡片中的动作短提示展示；保留动作名称、组序号、组类型、目标重量 / 次数、下一组摘要和必要状态文案。
- 删除短提示后，当前组主卡应自然收缩高度，不用新的说明文案填空；下方“下一组”卡片应随之上移。
- 下一组卡片也应压缩为摘要：保留下一组动作名、组序号 / 总组序号、重量和次数，删除“力量训练按动作和组推进...”这类重复解释句。
- 不改变动作库、计时训练、跟练中的 `shortCue` 数据和展示；本轮只处理力量训练执行页。
- 不新增目标组颜色字段，不改 `StrengthSetPlan`、serializer、Room、session record 或历史趋势。

验收重点：

- 720x1280 下力量计划编辑页删除颜色入口后没有明显空洞，重量 / 次数 / 休息输入仍对齐、可读、可点。
- 720x1280 下力量执行 active / prepare / rest 首屏更紧凑，当前组主卡不保留短提示留下的空白高度。
- 下一组摘要不再像说明卡，训练中主要信息为动作名、当前第几组、重量、次数、时间、下一组和主按钮。
- confirm-record 首屏可见性、completed / abandoned sticky `返回计划`、E15-5b selector 和 E15-5a route clock 均不回归。

## Recommended Story Split

1. **E15-5a TimerDial short-target motion diagnostic + fix gate**
   证据先行。先复现 / 采样 1s / 2s production TimerDial，再修，不允许只改公式。

2. **E15-5b Strength set timer mode selector layout polish**
   把横向 chip 改为不溢出的竖向 selector，保持保存与执行语义不变。

3. **E15-5c Strength completion sticky return action**
   completed / abandoned 终态增加固定底部返回主动作，滚动内容不遮挡。

4. **E15-5d Strength editor and execution simplification**
   删除力量目标组颜色占位入口，并移除力量执行页低价值动作短提示；保留动作名称和计划目标作为力量训练主识别。

## Boundary

- 本 planning gate 不实现代码、不生成 APK、不启动 AVD。
- 后续 implementation story 必须各自补 focused unit / UI state tests、`app:testDebugUnitTest`、`app:assembleDebug`、`app:lintDebug`、`app:check`、diff whitespace checks 和 Android smoke。
- Android smoke 证据放入 `.local/smoke/<story-id>/`，不得提交 `.local/`、APK、截图、日志或 build 输出。
- 不恢复心率卡片、手动心率输入、平均心率趋势或任何设备接入。
