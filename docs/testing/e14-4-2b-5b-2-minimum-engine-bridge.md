# E14.4-2b-5b-2 Minimum Engine Bridge

**Date:** 2026-06-27
**Status:** Implemented

## Scope

This slice implements the smallest production bridge needed to turn the E14.4-2b-5b-1 bridge expectation tests green.

Implemented:

- `TimedWorkoutEngine` now recognizes `TimedCompositionBlock` at the snapshot-to-engine-step construction boundary.
- The engine calls `TimedCompositionTimelineAdapter.expand(block)` for v2 composition blocks.
- Adapter timeline steps are mapped into existing `TimedSessionStep` records without adding a new engine step model.
- Unsupported v2 versions and empty v2 timelines fail closed without falling back into legacy timed execution.
- Boundary guard tests now allow timeline adapter terms only in the minimal engine bridge surface.

## Mapping

The bridge maps adapter metadata into fields that already exist on `TimedSessionStep`:

- adapter `id` -> engine step `id`;
- `compositionBlockId` -> `blockId`;
- real target id or synthetic boundary / between-round rest target id -> `itemId`;
- adapter `WORK` -> `TimedSessionStepKind.WORK` / `SessionStepKind.TIMED_WORK`;
- adapter `REST` -> `TimedSessionStepKind.REST` / `SessionStepKind.TIMED_REST`;
- adapter `displayName` -> `title`;
- adapter `plannedDurationSec` -> `durationSec`;
- adapter `roundIndex` -> `round`;
- adapter-expanded max round index -> `roundCount`;
- adapter `targetKind` -> existing `TimedStageType`;
- adapter `iconKey`, `colorHex`, and cue settings -> existing engine step fields.

Repeated targets across rounds keep distinct engine step ids because the adapter id includes the round and stage instance metadata. The engine still stores the real source target id in `itemId`, so session records and future E12 descriptors can reconstruct richer metadata from the immutable snapshot and adapter timeline.

## Rest Extension

`WorkoutCommand.ExtendRest` is unchanged.

V2 rest targets and synthetic between-round rest steps are ordinary engine rest steps, so `+15s` can extend only the active rest step. Warmup, action, custom, and cooldown steps map to work steps and ignore `ExtendRest`.

The bridge does not insert new targets, does not modify the plan snapshot, and does not resize future TimerDial planned ratios.

## Explicit Non-Scope

This slice does not implement:

- TimerDial production mapping;
- v2 start-gate enablement in the editor or plan detail route;
- Room schema / migrations;
- session record model changes;
- `WorkoutCommand` or `WorkoutEvent` changes;
- UI route changes;
- debug seed plans, smoke plans, APK output, emulator launch, screenshots, `.local/smoke`, or `.local/verification`;
- heart-rate UI, manual heart-rate input, unavailable heart-rate placeholders, average heart-rate trends, or device integration.

## Verification

Executed:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*TimedComposition*Bridge*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
```

Results:

- Focused bridge tests passed.
- Full unit tests passed.
- `assembleDebug` and `lintDebug` passed.

## Next Step

Next is **E14.4-2b-5b-3 v2 start gate enablement and smoke planning / implementation gate**.

Do not jump directly to E14.4-2b-6 TimerDial mapping. TimerDial should only consume adapter-derived timeline metadata after the v2 start gate and record compatibility path are accepted.
