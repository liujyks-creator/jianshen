# E12-1 records data semantics + v2 interpretation foundation

**Date:** 2026-06-30
**Status:** Implemented and verified

## Scope

This slice implements the data semantics foundation for records / history / trends before any larger records page IA or chart polish work.

In scope:

- Interpret timed composition v2 sessions in history detail rows from the historical `WorkoutSession.planSnapshot`.
- Build a separated v2 timed comparable-rest trend key family.
- Keep legacy timed trend keys isolated from v2 timed composition keys unless a future equivalence mapper proves compatibility.
- Preserve existing strength comparable set trend semantics.
- Add focused tests for v2 interpretation, legacy/v2 isolation, boundary rest handling, rest extension mapping, and strength regression.

Out of scope:

- Records page visual redesign, chart component redraw, full E12 IA polish, heart-rate charts, health device integrations, Room migration, TimerDial changes, workout engine changes, `WorkoutCommand`, or `WorkoutEvent` changes.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/testing/e12-records-trends-polish-planning-visual-gate.md`
- `docs/planning/decision-log.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/testing/e14-4-2b-closeout.md`
- `docs/testing/e14-4-2b-5c-session-record-compatibility.md`
- `docs/testing/e14-6-2b-completion-recap-page-compose.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- Local `skills/bmad-method/SKILL.md`
- `test-android-apps:android-emulator-qa` skill, only to confirm no UI smoke is required when no visible UI rendering changes are made

## Implementation Summary

- `HistoryUiState` now expands `TimedCompositionBlock` records through `TimedCompositionTimelineAdapter` when building record detail rows and timed comparable rest descriptors.
- V2 session details include composition step count, stageGroup / target summaries, boundary rest rows, and rest-extension positioning when records can be mapped back to real rest targets or deterministic synthetic between-round rest targets.
- Planned timed step count and planned rest totals now account for v2 adapter-expanded rest steps.
- Timed comparable rest trends now use separated key families:
  - `legacy_timed`
  - `timed_composition_v2`
- V2 trend keys include composition version, composition block id, timeline stage kind, stageGroup id, target id, target kind, round index, stageGroup / target indexes, stage / target instance indexes, planned duration, and an ordered structure signature from the historical snapshot.
- Mixed legacy/v2 timed data produces a data-quality row instead of merging sessions into one comparable trend.
- Unsupported or empty v2 timeline snapshots are preserved as records but excluded from comparable timed trends.
- Strength comparable set trend generation was not changed.

## Tests Added

Focused history tests now cover:

- V2 timed session detail rows for stageGroup / target interpretation and synthetic boundary rest.
- V2 rest extension records mapping to real rest targets and between-round rest when possible.
- V2 comparable rest trend key fields and row position labels.
- Between-round rest trend grouping.
- Legacy timed and v2 timed composition sessions not merging into the same comparable trend.
- Strength comparable trend still building when v2 timed sessions are present.

The timed composition boundary guard now explicitly allows `HistoryUiStateTest` as the E12 record interpretation expectation surface while keeping production engine / TimerDial boundary scanning unchanged.

## Verification

- `. .\.local\env.ps1`
- `.\gradlew.bat app:testDebugUnitTest --tests "*History*" --tests "*Record*" --tests "*Trend*" --tests "*TimedComposition*" --no-daemon --console=plain` passed.
- `.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain` passed.
- `.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain` passed.
- `git diff --check` passed with only expected Git CRLF working-copy warnings.
- Room schema / migration diff check returned `NO_ROOM_SCHEMA_OR_MIGRATION_DIFF`.
- TimerDial / workoutsession execution / engine / `WorkoutCommand` / `WorkoutEvent` diff check returned `NO_TIMERDIAL_ENGINE_COMMAND_EVENT_DIFF`.
- `design-md` search returned no matches.
- Heart-rate and health-device term searches only matched existing boundary documents, existing history no-heart-rate tests, or this slice's explicit "not added" notes; no new production UI, input, trend, or integration was added.
- Android path check: `adb.exe=True`, `emulator.exe=True`, and `TrainFlow_Pixel_API_36` listed after loading `.local\env.ps1`.
- UI smoke was not run because this slice changed record interpretation / UI state and tests only, not visible Compose rendering or records page layout.

## Boundary Confirmation

- Room schema / migrations: no intended changes.
- Training engine: no intended changes.
- TimerDial: no intended changes.
- `WorkoutCommand` / `WorkoutEvent`: no intended changes.
- Completion recap continues to reuse existing summary data; no recap semantic change in this slice.
- No visible records page layout or chart redesign was implemented.
- No heart-rate UI, heart-rate input, average heart-rate trend, BLE, Huawei, Health Connect, HealthKit, or Wear OS integration was added.
- No AVD smoke is planned because this slice changes record interpretation / UI state and unit tests, not Compose rendering or visible screen layout.
