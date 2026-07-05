# E15 维护踩坑与解决办法

**日期:** 2026-07-05
**范围:** E15-1 到 E15-5d 用户真机反馈、AVD smoke、review gate 和后续测试通过后的维护记录。
**目的:** 给后续开发者一份高信号避坑笔记，避免重复用猜测修 UI / 计时 / 声音问题。

**E15 收口状态:** E15-5d 已 review / accepted / merged，merge commit `0fa28463e4c24bf039944402a209f8f55c922c1b`，story commit `d9875bd48cd3e51b560c677efc3f6d4440efc89a`；用户 APK 测试已通过。下一阶段进入 MVP Alpha readiness，或按新增真机反馈拆 User Test Fix Pack 2。

## 总原则

- 先复现，再修复。真机反馈里的视觉和动效问题，必须先有 screenshot / screenrecord / UI XML / frame summary 或等价证据。
- UI polish 不改业务语义。力量和计时执行页的改动不得顺手改 engine、`WorkoutCommand`、`WorkoutEvent`、Room、session record、声音或心率边界。
- 不保留假入口。当前 MVP 不准备实现的能力，不要以“后续保存”“占位”形式留在主流程里。
- 720x1280 是必须覆盖的小屏基线。只看预览或大屏模拟，很容易漏掉底部 fixed action 遮挡、长中文溢出和首屏不可见。
- smoke evidence 要能回答用户问题。只跑单元测试不足以关闭“看起来不顺”“必须滑动才能点”的反馈。

## 1. 1s / 2s TimerDial 短 target 加速感

**症状:** 用户多次反馈 1s / 2s target 外圈仍有一小段加速效果。之前只从 TimerDial projection 角度修，仍没有真正解决。

**真正根因:** E15-5a baseline 证明问题不在 TimerDial Canvas geometry，也不在 planned-duration ratio。`TimedWorkoutSessionRoute` 的 1s route clock 在 ready gate 阶段已经启动 delay；用户点击 Start / Skip 后，旧 delay 可能马上醒来并 tick 新 target，导致短 target 过早从 `00:02` 跳到 `00:01`，外圈看起来提前过半。

**解决办法:** 给 timed route clock 增加 manual command anchor，在 `StartSession`、`PauseSession`、`ResumeSession`、`SkipStep` 后重置 tick 相位，并阻止 stale coroutine tick 新 target。

**后续维护要求:**

- 不要再只改 TimerDial projection helper 来宣称修复短 target。
- 需要复核 baseline / fixed PNG 或 screenrecord，确认点击后短 target 不会立刻跳秒。
- reduce-motion 也要覆盖，确认不会启动连续 projection。
- 禁止改变 outer-ring planned-duration ratio、TimerDial geometry、inner total progress、12 点数字圆标、engine timeline、Room、session record、commands/events、声音或心率边界。

## 2. 力量休息 countdown beep 与 auto-after-rest

**症状:** 力量休息最后 5 秒没有 countdown beep；`auto_after_rest` 休息结束后没有自动进入下一组 active。

**解决办法:** E15-1 复用既有 `WorkoutEvent.RestEnding`、`CueSettings.restEnding` 和 `COUNTDOWN_BEEP` 路径；`auto_after_rest` 在休息自然结束后自动进入下一组 active，`manual_start` 保持进入 prepare 等待手动开始。

**维护边界:**

- 不新增音频资源。
- 不请求 audio focus。
- 不 duck 或暂停外部音乐 / 视频。
- `soundEnabled=false` 必须阻止声音请求。
- 不改 `WorkoutCommand` / `WorkoutEvent` 语义。

## 3. auto-after-rest stage bell 误响风险

**症状:** 如果把 `StrengthSetReady` / `StrengthSetStarted` 直接当作通用 stage bell，手动开始、提前开始、初次 prepare 都可能误响。

**解决办法:** E15-1a 把 stage bell 严格限定为 `auto_after_rest` 休息自然结束并自动进入下一组 active 的 transition。手动开始、提前开始、首次 prepare、`manual_start` 都不响。

**后续维护要求:**

- 判断依据应是 route tick 中从 `STRENGTH_REST` 到 `STRENGTH_ACTIVE_SET` 的自动转场。
- 不要把所有 set start 事件都映射成 bell。

## 4. 本组计时模式 selector 长中文溢出

**症状:** 力量计划编辑页中“手动开始下一组”和“休息后自动开始下一组”横向并排，长中文在 720x1280 下突出边界。

**解决办法:** E15-5b 将横向 chip 改为竖向 radio-card options，完整显示文本、当前 / 未选状态、radio / checkable 状态和用户可读 content description。

**后续维护要求:**

- 中文长标签不要强行横排。
- 不暴露 raw `manual_start` / `auto_after_rest`。
- 保存语义仍通过既有 `StrengthSetTimerMode` 和 `StrengthExerciseBlock.setTimerMode`。
- 训练偏好只影响新建 / 编辑默认值，不运行时覆盖旧计划。

## 5. confirm-record 首屏不可确认

**症状:** 力量训练进入“确认本组”后，上方当前组 / 时间大卡占用高度，用户必须下滑才能看到确认信息和确认按钮。第一次 E15-4 修复只折叠顶部，但“感受”选项仍被固定底部 controls 挡住。

**解决办法:** E15-4 review fix 不只折叠顶部，还压缩确认卡片，把“轻松 / 刚好 / 很吃力 / 动作变形”提前到实际重量 / 次数输入之前，并用 XML bounds 检查四个 effort labels、实际输入和固定底部“确认本组”同时可见。

**后续维护要求:**

- confirm-record 的验收不是“顶部变小”就够，而是首屏能完整确认。
- 需要检查 scroll viewport 与 fixed controls 的 y 坐标关系。
- 新增字段或文案时要重新验证 720x1280。

## 6. completed / abandoned 返回按钮不可达

**症状:** 力量训练完成后的复盘内容较长，返回计划按钮在页面底部，用户必须滑到底才能返回。

**解决办法:** E15-5c 将 `返回计划` 从 `StrengthTerminalPanel` 内部移到 screen-level 固定底部 action，复盘内容预留 fixed-bottom padding。completed 和 abandoned 都有固定返回；completed 显示 `已完成`，abandoned 只显示 `已结束` / `提前结束`。

**后续维护要求:**

- 长复盘页的主返回动作必须固定可达。
- 内容底部必须留 padding，避免最后一行被 fixed action 遮挡。
- 不要把 completed 与 abandoned 的语义混在一起。

## 7. 目标组颜色占位和动作短提示的减法

**症状:** 力量计划编辑页出现“目标组颜色 / 后续保存”占位，但当前 MVP 没有真实颜色模型、保存、执行页映射或记录语义；力量执行页动作短提示对器械训练价值偏低，还占用当前组卡片高度。

**解决办法:** E15-5d 删除目标组颜色占位，不新增 `StrengthSetPlan.colorHex`、JSON snapshot、Room 或 session record 字段。删除后重排目标组输入：重量 / 次数并排，休息输入全宽。力量 prepare / active / rest 当前组卡片移除短提示，下一组卡片压缩为动作 / 组序号 / 重量 / 次数摘要。

**后续维护要求:**

- 未实现的数据能力不要留“后续保存”入口。
- 删除 UI 后必须补布局，不要留下空洞。
- 如果未来重新引入力量目标组颜色，必须先做 model / serializer decision；颜色不得被解释为训练强度、康复建议、加重量建议、医疗含义或趋势算法含义。
- 自定义动作可先依靠动作名称输入，不要为了短提示扩展复杂动作库能力。

## 8. Android smoke 证据要看 bounds，不只看截图

**踩坑:** 截图能看出大问题，但底部 fixed controls、scroll viewport、长中文溢出和 TalkBack content description 往往需要 UI XML / bounds 才能稳定判断。

**建议证据组合:**

- PNG 截图：确认真实视觉。
- UI XML：确认文本是否存在、是否被遮挡、bounds 是否在 viewport 内。
- bounds check 文本：对关键标签和按钮给出 y 坐标。
- logcat fatal / ANR scan：排除崩溃。
- `adb devices` / AVD 名称：记录执行环境。

**固定 AVD:** `TrainFlow_Pixel_API_36`

## 9. Worktree 与禁区文件管理

**踩坑:** 项目根长期有 APK、音频候选、deliverables、人工目录、`.local` smoke 证据和本地 planning docs dirty。review / dev 很容易误 stage。

**维护要求:**

- stage 必须显式列文件，避免 `git add .`。
- 禁止提交：`skills/`、`.local/`、根目录 APK、`countdown_beep1.mp3`、`deliverables/`、`人工/`、截图、日志、build 输出。
- smoke evidence 只放 `.local/smoke/<story-id>/`，不提交。
- 如果只是生成 APK 给用户，不要把 APK 纳入 Git。

## 10. 文档同步点

E15 系列变更涉及产品决策、设计约束和维护边界。后续同类改动至少同步检查：

- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- 对应 `docs/testing/<story-id>.md`

不要只更新测试文档而让 roadmap / status 继续显示旧方向，否则下一轮 story prompt 容易从过期计划出发。

## 快速回归清单

- 1s / 2s timed target 点击开始后不提前跳秒。
- 力量休息最后 5 秒有 countdown beep，且 `soundEnabled=false` 阻止声音。
- `auto_after_rest` 自然结束后自动进入下一组 active 并响 transition bell；手动路径不响。
- 本组计时模式 selector 不溢出，不暴露 raw token。
- confirm-record 首屏同时可见实际输入、四个 effort choices 和固定确认按钮。
- completed / abandoned 固定返回按钮可见，滚动到底部不遮挡内容。
- 目标组颜色占位不再出现。
- 力量 prepare / active / rest 当前组卡片不再显示短提示。
- 下一组卡片为紧凑摘要。
- 心率卡片、未获取心率、手动心率输入和平均心率趋势不应恢复。
