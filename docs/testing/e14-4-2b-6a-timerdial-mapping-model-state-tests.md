# E14.4-2b-6a TimerDial mapping model/state tests

**日期:** 2026-06-28
**类型:** test-first expectation tests / source-boundary guard / docs
**状态:** Implemented as green test-only expectation surface; production TimerDial mapping remains unimplemented.

## Scope

本轮只锁定 v2 TimerDial mapping 的 model / UI state 预期，不接入 production runtime mapper。

- 新增 `TimerDialCompositionMappingTest`，使用 test-only pure expectation mapper 从 `TimedCompositionTimelineAdapter` 输出的 timeline metadata 构造预期 `TimerDialUiState`。
- 更新 `TimedCompositionBoundaryGuardTest`，只把新的 TimerDial mapping expectation test 加入 timeline adapter terms 的测试白名单。
- 未修改 `TimerDial.kt`、`TimerDialUiState.kt`、`TimedWorkoutEngine.kt`、`TimedCompositionTimeline.kt`、Room、session record、`WorkoutCommand` 或 `WorkoutEvent`。

## Test Seam

`TimerDialCompositionMappingTest` 内部的 mapper 只存在于 `app/src/test`，不会进入 `app/src/main`，也不会改变生产运行行为。它的职责是把后续 production mapper 必须满足的状态形状提前固定下来：

- v2 active `STAGE_GROUP` 映射为 1-5 个 outer ring segments。
- warmup、cooldown、synthetic between-round rest 映射为 single current-stage fallback segment。
- stage color fallback 按 `target color -> stageGroup color -> stage type safe default` 表达预期。
- E14.5 的 smooth identity / anchor split 继续保护每秒 progress / remaining 更新不重置动画 key。

## Expectations Locked

1. Inner ring / 12 点数字圆标
   - 整次训练 stage progress 与当前 stageGroup target 数分离。
   - v2 total stage count = warmup + rounds * stageGroups + between-round rests + cooldown。
   - 当前 stageGroup target count 不替代 `totalWorkoutStageCount`。

2. Outer ring v2 stageGroup mapping
   - 1 target 映射为单个 full-ring segment。
   - 2 targets 按 `plannedDurationSec` 比例分段。
   - 3-5 targets 按 `plannedDurationSec` 比例分段。
   - action、custom、rest target 都参与比例。
   - target color fallback 顺序固定为 target color、stageGroup color、stage type safe default。

3. Active / completed / future
   - active target 前的 targets progress 为 completed。
   - active target 标记 `isCurrent=true` 并携带当前 progress。
   - active target 后的 targets progress 为 future。

4. Fallback states
   - warmup 不参与 stageGroup target ratio，回退为 single current-stage segment。
   - cooldown 同样回退。
   - synthetic between-round rest 回退为 single rest segment。
   - legacy timed plan 保持现有 work/rest cycle semantics。

5. Rest extension
   - `+15s` 不重算 planned ratios。
   - 不插入 target。
   - 不创建第 6 段。
   - active segment progress 与 total progress 不倒退。

6. Continuous progress non-regression
   - mapping identity 不包含每秒变化的 progress / remaining。
   - progress / remaining 变化只进入 smooth anchor。

## Verification

- Focused TimerDial tests: passed.

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*TimerDial*Composition*" --tests "*TimerDial*UiState*" --no-daemon --console=plain
```

- Full unit tests: passed.

```powershell
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
```

- Build / lint: passed.

```powershell
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
```

- `git diff --check`: passed.
- Production mapping boundary diff for `TimerDial.kt`, `TimerDialUiState.kt`, `TimedWorkoutEngine.kt`, and `TimedCompositionTimeline.kt`: empty.
- Old entry search: no matches in `app/src/main` or `app/src/test`.
- Design skill forbidden-term search: no matches.
- Heart-rate full repository search still reports existing historical / boundary documentation and regression guard references; diff-level search for this task adds no matching heart-rate UI/input/statistics terms.
- Android path check only: `adb.exe` exists, `emulator.exe` exists, and `TrainFlow_Pixel_API_36` is listed. The AVD was not started.

## Boundaries

This gate does not implement production TimerDial mapping. E14.4-2b-6b should consume these tests when adding the smallest production mapper that reads v2 timeline metadata, while preserving the existing Canvas geometry, E14.5 continuous progress behavior, engine semantics, commands, events, session record shape, and heart-rate removal boundary.
