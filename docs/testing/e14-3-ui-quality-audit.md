# E14.3 UI Quality Audit And Polish Sequencing

**日期:** 2026-06-21
**状态:** Docs-only audit completed
**范围:** UI quality audit, screenshot matrix, issue priority, and E14.4 polish sequencing

## Boundaries

E14.3 只做审计和排序，不直接重写页面，不修改训练执行语义，不修改 `WorkoutCommand`、`WorkoutEvent`、session record 或 Room schema。

本轮继续遵守 E11.3 和 E14.2 边界：不恢复心率显示，不恢复手动心率输入，不恢复平均心率趋势，不接真实设备、BLE、Huawei SDK、Health Connect、HealthKit 或 Wear OS，不做医疗判断、危险告警或训练中断依据。

截图只保存到 `.local/smoke/e14-3-ui-audit/`。本轮不保存到 `.local/verification`，不提交截图、APK、日志或 build output。

## Inputs Read

- `AGENTS.md`
- `docs/project-status.md`
- `docs/planning/decision-log.md`
- `docs/roadmap-backlog.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`
- `docs/setup.md`
- Local `huashu-design` skill review guidance, used only as an audit lens.

## Code Surfaces Reviewed

- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimedWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/StrengthWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/FollowAlongWorkoutSessionRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDial.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/workoutsession/TimerDialTokens.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/PlanManagementRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/history/HistoryUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/exerciselibrary/ExerciseLibraryRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/followalong/FollowAlongRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/ui/shell/official/TrainFlowApp.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/home/HomeRoute.kt`

## Smoke Evidence

Emulator smoke used a 720 x 1280, density 320 viewport. `app:installDebug` succeeded before smoke. The run focused on screenshots and interaction reachability, not production code changes.

| Area | State / scene | Evidence | Result |
|---|---|---|---|
| Home | Default first viewport | `01-home.png` | Timed entry is strongly primary; strength and follow-along require scroll. |
| Plans | Empty plan state | `02-plans-list-detail.png` | Empty state explains source of plans but has no direct create CTA. |
| Records | Non-empty local records | `03-records-empty.png` | Filename is historical; screen showed 3 real local records and basic stats. |
| Exercise library | Filter top | `04-exercise-library.png` | Filter card consumes first viewport; right-side chips are partially clipped by horizontal scroll. |
| Timed editor | Top and lower editor | `06-timed-editor-top.png`, `07-timed-editor-scroll-short-area*.png` | Long editor works, but save/start actions are deep and input focus makes recovery slow. |
| Strength editor | Top and action area | `14-strength-editor-top.png`, `15-strength-editor-actions.png` | Save/start reachable at bottom; add-action chip row can clip. |
| Strength execution | Prepare, active, confirm, rest | `16-strength-execution-prepare2.png`, `17-strength-active-or-next.png`, `18-strength-confirm-set.png`, `19-strength-rest-or-confirmed.png` | Bottom controls are reachable; confirm/rest content sits close to fixed control area on small screen. |
| Follow-along entry | Top and lower entry | `21-follow-entry.png`, `22-follow-entry-lower.png` | Partial boundary is clear; start requires scrolling to card bottom. |
| Follow-along execution | Running | `23-follow-execution.png` | Countdown content is partially covered by fixed bottom controls on small screen. |
| Timed execution | Ready / running / paused / rest / rest extension | E14.2 real-device record plus source review | E14.2 fixed the Timer Dial geometry and `确认+15s` layout. E14.3 did not introduce code changes; final post-fix screenshot matrix should be refreshed before E14.4 implementation. |

## Issue Register

Impact levels use the requested labels: 阻塞训练, 影响可读性, 影响触控, 视觉 polish, 未来增强.

| ID | 功能区域 | 状态 / 场景 | 问题描述 | 影响等级 | 建议修复批次 | 真机确认 | 测试或 source-pattern 回归约束 |
|---|---|---|---|---|---|---|---|
| E14.3-TIM-01 | 计时训练执行页 | rest extension 确认态 | E14.2 已修复 `确认+15s` 不撑高按钮的源码和测试边界，但仍应用最新 APK 在 T-02 上确认真实触控状态。 | 影响触控 | 1. 训练执行页共性 polish | 是 | 保留底部三按钮同高、至少 `48dp`、`确认+15s` 不换行不挤压。 |
| E14.3-TIM-02 | 计时训练执行页 | ready / running / pause / rest 矩阵 | 当前审计主要复用 E14.2 证据；E14.4 开工前应补一次同一 APK 的 ready、running、pause morph、rest、rest extension 矩阵，避免基于过期截图调 UI。 | 视觉 polish | 1. 训练执行页共性 polish | 是 | Screenshot smoke 覆盖小屏安全区和文字裁切，不改 engine 状态。 |
| E14.3-STR-01 | 力量训练执行页 | prepare / active | 当前动作、set 进度、目标重量次数和底部主操作层级清楚，底部按钮可达；这是可保留的基线。 | 视觉 polish | 1. 训练执行页共性 polish | 是 | Polish 时不得降低主操作可达性，不改变 set state machine。 |
| E14.3-STR-02 | 力量训练执行页 | confirm set | 确认记录态主卡较高，固定底部按钮与内容边界很近；小屏上需确认确认层字段、快捷次数和主按钮不会被遮挡或需要别扭滚动。 | 影响触控 | 1. 训练执行页共性 polish | 是 | Source-pattern 约束：计划值继续预填 actual record；`ConfirmStrengthSet` 语义不变。 |
| E14.3-STR-03 | 力量训练执行页 | rest | 休息态显示清楚，`提前开始本组` 可达；但固定底部区和主卡底部贴近，后续需统一执行页内容 reserve 和 bottom controls spacing。 | 影响可读性 | 1. 训练执行页共性 polish | 是 | 覆盖 rest、pause、end confirmation 的小屏 smoke。 |
| E14.3-FOL-01 | 跟练雏形执行页 | running | `23-follow-execution.png` 显示倒计时大字被固定底部控制遮住，训练中最关键的时间信息不可完整读取。 | 影响可读性 | 1. 训练执行页共性 polish | 是 | 先修布局 reserve / scroll behavior；不把跟练升级成完整课程平台。 |
| E14.3-FOL-02 | 跟练雏形入口 | preset card lower section | Partial boundary 文案清楚，但开始按钮在卡片底部，720x1280 需要滚动到下方才能启动。 | 影响触控 | 5. 跟练雏形 polish | 可选 | 保持“雏形 / 无真实媒体播放”边界，不新增假视频或课程能力。 |
| E14.3-PLAN-01 | 计划编辑 | timed / strength editors | 保存和开始训练入口只在长页面底部；文本输入获得焦点后，小屏回到动作按钮的路径慢，影响创建后立即开始训练。 | 影响触控 | 2. 计划编辑 / 计划详情 polish | 是 | 可做 sticky action 或顶部/底部 quick action，但保存 plan id、edit backfill 和 reminder 语义不变。 |
| E14.3-PLAN-02 | 计划页 | empty state | 计划为空时只说明从训练首页创建，没有直接进入计时/力量创建的 CTA。 | 影响触控 | 2. 计划编辑 / 计划详情 polish | 可选 | 新入口只路由到既有 editor，不制造新 plan model。 |
| E14.3-PLAN-03 | 计划编辑 | stage / set cards | 阶段和 set 卡片承载密度高，移动、复制、删除、颜色、类型、目标值等控件造成小屏滚动效率低。 | 影响可读性 | 2. 计划编辑 / 计划详情 polish | 是 | 保持简单默认、深层控制展开的 DESIGN 原则。 |
| E14.3-PLAN-04 | 计划编辑 | action selection chips | 力量添加动作横向 chip 在右侧裁切明显，选择动作效率依赖横向滚动。 | 影响触控 | 2. 计划编辑 / 计划详情 polish | 可选 | 为未来统一动作选择页保留，不改变 `Exercise`/plan contract。 |
| E14.3-HIS-01 | 记录页 | non-empty records | 基础统计、聚合趋势、同类阶段趋势和清理入口都已在同页叠加，信息来源真实但首屏扫描负担偏重。 | 影响可读性 | 3. 记录页数据分析和图表 polish | 可选 | 继续只消费真实 Room sessions 和 plan snapshot，不生成假统计。 |
| E14.3-HIS-02 | 记录页 | trend charts | 非心率趋势图需要更清楚的日期/轴/legend 层级；首版应聚焦训练量、时长、额外休息和 strength set trends。 | 视觉 polish | 3. 记录页数据分析和图表 polish | 可选 | 明确不得加入平均心率趋势或心率占位。 |
| E14.3-HIS-03 | 记录页 | cleanup / empty state | 历史清理确认语义从代码上完整；还需补空记录截图，确认清理后空状态不会残留统计卡或误导文案。 | 影响可读性 | 3. 记录页数据分析和图表 polish | 可选 | 清理仍走 repository 事务；不删除 plans、exercises 或 fixture。 |
| E14.3-EX-01 | 动作库 | filter top | 筛选区占据第一个 viewport，动作卡片需要滚动后才出现；chip 行右侧裁切降低可发现性。 | 影响触控 | 4. 动作库 / 动作详情 polish | 可选 | 保留只读动作库和 fixture contract，不引入假内容。 |
| E14.3-EX-02 | 动作库 / 动作详情 | card and detail text | 动作卡片和详情中仍可见 fixture id / draft mapping 语感，用户可读性偏工程化。 | 视觉 polish | 4. 动作库 / 动作详情 polish | 否 | 文案 polish 不改 `Exercise.id` 或替代映射。 |
| E14.3-EX-03 | 动作详情 | replacement actions | 替代动作当前偏只读信息，未来统一动作选择页需要准备“选择/替换”入口模式。 | 未来增强 | 4. 动作库 / 动作详情 polish | 否 | 仍以 action library contract 为先，不在本批扩动作内容规模。 |
| E14.3-HOME-01 | Shell / Home | bottom navigation | 底部导航使用单字“训/计/动/录”近似占位图标，视觉完成度低于主要页面。 | 视觉 polish | 1 或 4 | 否 | 可替换为正式 iconography；不改变 destination 和 mode semantics。 |
| E14.3-HOME-02 | Home | first viewport | 计时训练作为默认入口合理，但力量和跟练同层入口在小屏需要滚动才可见，可能影响“力量与计时并行首版能力”的发现。 | 影响可读性 | 2 或 5 | 可选 | 保持计时默认推荐，不把跟练包装成完整课程。 |

## User-Test-Before Fixes

These should be fixed or at least re-smoked before handing the next APK to user testing:

1. Follow-along execution countdown must not be hidden by fixed bottom controls.
2. Strength confirm/rest states need small-screen screenshot confirmation after bottom control spacing is unified.
3. Timed execution needs a fresh ready/running/pause/rest/rest-extension screenshot matrix on the latest APK, including `确认+15s`.
4. Plan editor save/start should become easier to reach after editing on a small screen.
5. Plans empty state should offer direct create actions if the Plans tab is expected to be a normal user entry.

## Later Polish

These can wait until after core user-test blockers are addressed:

- Records chart axis / legend polish and section grouping.
- Exercise library filter compaction and detail hierarchy polish.
- Fixture-id-facing copy cleanup.
- Bottom navigation icon polish.
- Follow-along entry card density, as long as the execution countdown blocker is fixed first.

## E14.4 Recommended Sequencing

1. **训练执行页共性 polish**: Fix shared bottom control reserve, small-screen safety, pause/rest/end-confirmation states, follow-along countdown occlusion, and timed rest-extension screenshot matrix.
2. **计划编辑 / 计划详情 polish**: Make save/start easier to reach, improve edit backfill affordance, reduce stage/set card friction, and add create entry from empty Plans if needed.
3. **记录页数据分析和图表 polish**: Improve real-stat section grouping, non-heart-rate chart readability, extra-rest trend display, cleanup confirmation screenshots, and empty-state proof.
4. **动作库 / 动作详情 polish**: Compact filters, improve action card/detail readability, remove user-facing draft wording, and prepare replacement/action selection entry patterns.
5. **跟练雏形 polish**: After shared execution fix, keep the partial-flow boundary clear, improve entry/start reachability, and avoid any full-course-platform implication.

## E14.4 Guardrails

- Do not change `WorkoutCommand`, `WorkoutEvent`, engine state machines, session record semantics, Room schema, or action-library contracts during visual polish.
- Do not restore heart-rate UI, manual heart-rate input, average heart-rate trend, or unavailable heart-rate placeholder.
- Do not connect real devices or add health-device SDKs.
- From E14.4-2 onward, each UI polish batch must start with a docs-only / mock-only visual proposal gate. The proposal must include current issues, at least two directions, a recommended direction, user-confirmation points, and implementation split. Do not change Kotlin / Compose / Room / tests or generate an implementation APK until the user confirms the visual direction.
- After user confirmation and during implementation, add a small screenshot matrix or source-pattern regression before generating a user-test APK.
