# E14.4-2b-5c Session Record Compatibility

**Date:** 2026-06-28
**Status:** Complete; focused compatibility tests, full unit tests, build/lint, Android smoke, and boundary checks passed.

## Scope

This gate verifies that timed composition v2 can enter the existing session record path without changing the persisted session model, Room schema, workout commands, workout events, TimerDial implementation, or heart-rate boundaries.

The compatibility contract for this gate is:

- V2 timed composition execution can produce a `WorkoutSession` and terminal summary without crashing.
- `WorkoutSession.planSnapshot` preserves the v2 `TimedCompositionBlock` JSON payload through the existing repository path.
- Actual timed step records use deterministic adapter-derived step ids that can be reconstructed from the session snapshot.
- `+15s` on a v2 rest target records added seconds through the existing timed rest extension record structure.
- Synthetic between-round rest extension records either cleanly or is documented as follow-up.
- Work, warmup, and cooldown steps do not produce rest extension records.
- Legacy timed session records remain unchanged.
- History / records mappers can read v2 session snapshots without crashing; v2 trend grouping remains future work.
- Unsupported or empty v2 plans fail closed and do not create malformed step or rest extension records.
- No Room schema migration is needed.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- E14.4-2b process, data-model, TimerDial semantics, editor, timeline, bridge, and start-gate testing docs through `docs/testing/e14-4-2b-5b-3-v2-start-gate-smoke.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- Android emulator QA skill
- Local `skills/bmad-method/SKILL.md`

## Test Changes

Added `TimedCompositionSessionRecordCompatibilityTest` with focused coverage for:

- Terminal v2 timed composition session record and summary creation.
- Repository round-trip of v2 `WorkoutSession.planSnapshot` through existing session storage.
- Deterministic actual step ids matching `TimedCompositionTimelineAdapter.expand(block)`.
- V2 rest target `+15s` recording through `timedRestExtensionRecords`.
- Synthetic between-round rest extension recording through the same structure.
- Work, warmup, and cooldown `ExtendRest` attempts not creating rest extension records.
- Legacy timed rest extension record shape staying unchanged.
- History / records UI-state mappers reading v2 snapshots without crashing.
- Unsupported-version and empty v2 plans failing closed with empty step / rest histories.

Updated `TimedCompositionBoundaryGuardTest` only to allow the new focused session record compatibility test to reference adapter-owned timeline terms. The production boundary remains unchanged.

## Production Fixes

None. The focused tests pass against the existing E14.4-2b-5b-3 production path.

## Compatibility Conclusions

- Session record path: v2 execution can produce completed or abandoned `WorkoutSession` records without requiring a new model.
- Plan snapshot: v2 `TimedCompositionBlock` payload round-trips through the existing repository `plan_snapshot_json` field.
- Actual step records: stored ids match adapter-derived engine step ids and can be reconciled with the snapshot-expanded timeline.
- Rest extension records: real v2 rest targets and synthetic between-round rest steps record added seconds through the existing `TimedRestExtensionRecord` shape.
- Non-rest steps: work, warmup, and cooldown steps do not create rest extension records.
- Legacy records: legacy timed session step ids, rest stage ids, and plan snapshot shape remain unchanged.
- History / records: v2 snapshots can be read into history detail, stats, and trend state without crashing; v2 comparable trend grouping remains future E12 polish.
- Unsupported / empty v2 plans: fail closed with no executable steps, no step history, and no rest extension records.
- Room schema: no migration is needed for this compatibility gate.

## Boundaries Preserved

- No TimerDial continuous progress fix.
- No TimerDial outer-ring semantic mapping.
- No Room schema or migration change.
- No session record model redesign.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No heart-rate UI, manual input, unavailable placeholder, or trend restoration.
- No `.local/verification` output.

## Verification

Executed:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*Session*Compatibility*" --tests "*TimedComposition*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
```

Results:

- Focused compatibility / timed composition tests passed.
- Full debug unit tests passed.
- `assembleDebug` and `lintDebug` passed.
- `git diff --check` and boundary checks passed after the smoke/doc update.
- Pre-commit staged files were empty before precise staging.
- No Room schema / database diff was present.
- No TimerDial file diff was present.
- No `WorkoutCommand` / `WorkoutEvent` diff was present.
- Old debug / smoke seed entry search returned no matches.
- Heart-rate search matched only existing boundary docs / regression-test references; this task did not restore production heart-rate UI/input/statistics.
- Forbidden design-skill search returned no matches.

## Smoke Evidence

Evidence path:

`.local/smoke/e14-4-2b-5c-session-record-compatibility/`

Result: passed on AVD `TrainFlow_Pixel_API_36`.

Smoke coverage:

- Installed the current debug APK, cleared app data, and launched TrainFlow.
- Entered the default timed composition v2 editor path from `编辑计时计划`.
- Confirmed editor `开始训练` is enabled and enters the existing ready gate.
- Started training from the ready gate.
- Reached warmup and work states; `+15s` remained disabled on non-rest states.
- Reached a v2 rest state, triggered `+15s`, and saw the later summary report `休息延长 15 秒`.
- Ended the session through the existing confirmation flow, producing an abandoned local session record.
- Reopened the app, navigated to `记录`, and confirmed the history page reads `1 条本地记录`, `abandoned 1 次`, and `计时额外休息 15秒`.
- UI tree forbidden-term scan covered `COMPOSITION_V2`, `TimedComposition`, `HeartRatePanel`, `ManualHeartRate`, `未获取心率`, `手动心率`, and `平均心率趋势`; result: no matches.
- TrainFlow process logcat and app-crash keyword scans returned no TrainFlow crash matches.
- AVD was shut down after smoke; `adb devices` returned an empty device list.

## Next

After this gate, continue with either an isolated TimerDial continuous progress fix story or E14.4-2b-6 TimerDial mapping planning / implementation. Do not combine continuous progress repair with outer-ring semantic mapping.
