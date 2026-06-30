# E12-2 Records IA / Chart UI Polish

**Date:** 2026-07-01
**Scope:** implementation
**Evidence path:** `.local/smoke/e12-2-records-ia-chart-ui-polish/`

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/testing/e12-records-trends-polish-planning-visual-gate.md`
- `docs/testing/e12-1-records-data-semantics-v2-foundation.md`
- `docs/planning/decision-log.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- AGENTS read-first support docs: `docs/readiness-report.md`, `docs/planning/product-brief.md`, `docs/planning/prd.md`, `docs/planning/ux-design.md`

Skills read:

- `skills/bmad-method/SKILL.md`
- `huashu-design/SKILL.md`
- `huashu-design` references: `content-guidelines.md`, `verification.md`, `critique-guide.md`
- `test-android-apps:android-emulator-qa/SKILL.md`

The forbidden design skill was not used.

## Implementation Summary

The records page now follows the E12 planning-gate IA:

1. Overview summary
2. Filters
3. Recent sessions
4. Selected session detail
5. Trends
6. History cleanup

This round only changes the history UI state and Compose presentation. It does not change saved session semantics, Room schema, migrations, training engines, TimerDial, workout commands, workout events, heart-rate UI, or device integrations.

## UI State Changes

- Added mode and status filters to `HistoryScreenState`.
- Added overview summary UI state for total records, completed, abandoned, total elapsed, effective elapsed, pause, planned rest, actual rest, and extra rest.
- Recent list items now expose status tone and flags for completed / abandoned, skipped, pause, actual rest, and extra rest.
- Selected detail rows now distinguish:
  - legacy timed step/rest interpretation,
  - timed composition v2 stageGroup / target / boundary rest interpretation,
  - strength action/set weight, reps, and rest interpretation.
- Aggregate trend charts now expose x-axis label, y-axis label, unit, state label, y ticks, x ticks, and legend rows.
- Timed and strength comparable trend sections now expose grouping explanations for `legacy_timed`, `timed_composition_v2`, and `strength_comparable_set`.

## Chart UI

Charts are intentionally simple. They show:

- `X 轴：startedAt 日期`
- Y-axis label by chart type, including count or seconds unit.
- Y tick labels based on real values.
- First and last date tick labels.
- `Legend · 最新值` rows for visible series.
- `空状态` when there are no dated points.
- `数据不足` when only one dated point exists.

No fake lines, fake records, or fake trend values are generated. Empty filtered results use a visible empty state: `当前筛选无记录` / `不会补假记录或假趋势`.

## Record-Type Display

- Legacy timed: detail rows explain legacy `TimedCircuitBlock` work/rest ordering and continue using the `legacy_timed` trend family.
- Timed composition v2: detail rows are interpreted from the historical `WorkoutSession.planSnapshot` through `TimedCompositionTimelineAdapter`, including stageGroup, target, target kind, boundary rest, and rest extension location. V2 keeps the `timed_composition_v2` trend family.
- Strength: detail rows show action/set count plus recorded weight, reps, and actual rest. Strength comparable trend grouping keeps existing `exerciseId` / source set semantics.

Legacy timed and timed composition v2 remain separate by default. There is still no legacy/v2 equivalence mapper.

## Verification

Commands:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*History*" --tests "*Record*" --tests "*Trend*" --tests "*TimedComposition*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
```

Results:

- Focused history / record / trend / timed composition unit tests: passed.
- Full `app:testDebugUnitTest`: passed.
- `app:assembleDebug app:lintDebug`: passed.

## Android Smoke

Smoke environment:

- AVD: `TrainFlow_Pixel_API_36`
- Evidence directory: `.local/smoke/e12-2-records-ia-chart-ui-polish/`
- Seed DB: local smoke-only `trainflow.db`; not committed.

Final evidence uses the `rerun-*` files in the smoke directory. Earlier non-rerun files were intermediate diagnostics and are not the final gate.

Covered evidence:

- `rerun-01-overview-first-screen.png/xml`: overview summary first screen.
- `rerun-02-filters.png/xml`: filters.
- `rerun-03-recent-list.xml`: recent-list section context.
- `rerun-04-detail-legacy-completed.png/xml`: completed legacy timed detail with legacy step/rest interpretation.
- `rerun-05-detail-v2-composition.png/xml`: timed composition v2 detail with stageGroup / target / boundary rest interpretation.
- `rerun-06-detail-strength.png/xml`: strength detail with action/set, kg, reps, and rest.
- `rerun-07b-trends-axis-legend-full.png/xml`: trend chart x-axis, y-axis, units, and legend.
- `rerun-08-filtered-empty-state.png/xml`: filtered empty state.
- `rerun-09-abandoned-skipped-rest-pause-visible.png/xml`: abandoned / skipped / pause / extra rest visibility.
- `rerun-smoke-coverage-summary.txt`: semantic coverage assertions passed.
- `rerun-ui-tree-forbidden-scan.txt`: visible text/content-desc forbidden scan passed.
- `rerun-small-screen-bounds-check.txt`: visible text/content-desc bounds inside 720x1280 passed.
- `rerun-logcat-fatal-anr-scan.txt`: no fatal / ANR / app crash lines. Normal `D/I AndroidRuntime` command-wrapper lines from `uiautomator` / `svc` were ignored.
- `rerun-adb-devices-after.txt`: `adb devices` empty after cleanup.

## Boundary Checks

- No Room schema or migration changes.
- No training engine changes.
- No TimerDial changes.
- No `WorkoutCommand` or `WorkoutEvent` changes.
- No heart-rate UI, manual input, unavailable placeholder, or average heart-rate trend restored.
- No BLE, Huawei, Health Connect, HealthKit, or Wear OS integration added.
- No `.local/verification` changes.
- Smoke artifacts remain under `.local/` and are not committed.
