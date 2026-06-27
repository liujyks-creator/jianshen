# E14.4-2b-5b-1 Engine Adapter Bridge Tests

**Date:** 2026-06-27
**Nature:** Test-first implementation gate
**Status:** Focused bridge expectation tests added; expected red until the minimum engine bridge is implemented.

## Scope

This gate defines the expected bridge from v2 `TimedCompositionBlock` to the existing flat `TimedSessionStep` model without changing production execution behavior.

Allowed changes in this gate:

- Add focused unit tests for v2 adapter-expanded steps entering `TimedWorkoutEngine`.
- Add source-boundary tests to keep v2 adapter terms out of TimerDial, route, command, event, and unintended test surfaces.
- Keep v2 start disabled until a later implementation gate explicitly opens it.

Out of scope:

- No production `TimedWorkoutEngine` bridge implementation.
- No TimerDial mapping.
- No Room schema, session record model, `WorkoutCommand`, or `WorkoutEvent` changes.
- No APK generation and no AVD launch.

## Tests Added

New focused tests:

- `TimedCompositionEngineBridgeTest.v2CompositionExpandsThroughTimelineAdapterIntoEngineCompatibleTimedSteps`
- `TimedCompositionEngineBridgeTest.engineStepIdsComeFromTimelineMetadataAndRepeatedTargetsAreDistinct`
- `TimedCompositionEngineBridgeTest.v2RestTargetsAndSyntheticBetweenRoundRestMapToRestExtendableStepsOnly`
- `TimedCompositionEngineBridgeTest.v2RestTargetAndSyntheticBetweenRoundRestAcceptExtendRestWhenActive`
- `TimedCompositionEngineBridgeTest.v2WorkWarmupAndCooldownStepsDoNotAcceptExtendRest`
- `TimedCompositionEngineBridgeTest.legacyTimedPlanStillUsesExistingEnginePath`
- `TimedCompositionEngineBridgeTest.unsupportedCompositionVersionFailsClosedWithoutExecutableV2Steps`
- `TimedCompositionEngineBridgeTest.emptyV2TimelineFailsClosedWithoutExecutableSteps`
- `TimedCompositionEngineBridgeTest.v2StartGateRemainsDisabledUntilBridgeImplementationOpensIt`

Updated boundary tests:

- `TimedCompositionBoundaryGuardTest.timelineAdapterTermsInTestsStayInAdapterAndBridgeExpectationSurfaces`
- `TimedCompositionBoundaryGuardTest.workoutCommandAndEventDoNotGrowCompositionBridgePayload`

## Expected Bridge Behavior

The future bridge should call `TimedCompositionTimelineAdapter` at the engine timeline construction boundary, then map each timeline step into an existing `TimedSessionStep`.

Expected mapping:

- Warmup becomes a work timed step with deterministic timeline-derived id, display name, duration, color, and icon.
- Action and custom targets become work timed steps.
- Rest targets and synthetic between-round rests become rest timed steps.
- Cooldown becomes a work timed step.
- Engine step ids come from deterministic timeline metadata; repeated targets across rounds must produce distinct step ids.
- `blockId` carries the composition block id.
- `itemId` carries the real or synthetic timeline target id.
- Round-scoped target steps and between-round rests carry round metadata.
- Rest extension applies only to rest target and synthetic between-round rest steps.
- Work, warmup, and cooldown steps do not accept rest extension.

## Expected Red Result

Focused command:

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*TimedComposition*Bridge*" --no-daemon --console=plain
```

Current result:

- `11 tests completed, 5 failed`
- Red is expected because production `TimedWorkoutEngine` still ignores `TimedCompositionBlock` and produces no executable v2 steps.

Expected failing tests:

- `v2CompositionExpandsThroughTimelineAdapterIntoEngineCompatibleTimedSteps`: expected 9 adapter-derived engine steps, actual engine steps were empty.
- `engineStepIdsComeFromTimelineMetadataAndRepeatedTargetsAreDistinct`: expected repeated target steps for rounds 1 and 2, actual engine steps were empty.
- `v2RestTargetsAndSyntheticBetweenRoundRestMapToRestExtendableStepsOnly`: expected v2 rest target and between-round rest ids, actual rest steps were empty.
- `v2RestTargetAndSyntheticBetweenRoundRestAcceptExtendRestWhenActive`: expected an active v2 rest step, actual current step was `null`.
- `v2WorkWarmupAndCooldownStepsDoNotAcceptExtendRest`: expected an active warmup/work/cooldown step, actual current step was `null`.

Expected green coverage in the same focused run:

- Legacy timed plan still uses the existing engine path.
- Unsupported composition version and empty v2 timeline fail closed without executable v2 steps.
- Saved v2 plan start gate remains disabled in plan management state.
- Added source-boundary guard tests pass.

Because this is an intentional test-first red gate, the full unit suite is not claimed green until E14.4-2b-5b-2 implements the minimum bridge.

## Boundary Result

This gate does not change production files. It does not add adapter terms to:

- `TimedWorkoutEngine`
- `TimedWorkoutSessionRoute`
- TimerDial production files
- `WorkoutCommand`
- `WorkoutEvent`
- Room schema or migrations
- session record model

The source-boundary tests keep timeline adapter terms limited to the core adapter, adapter tests, boundary guard, and the bridge expectation test file.

## Next Step

Next story should be **E14.4-2b-5b-2 minimum engine bridge implementation**. It may make the red bridge tests pass by adding the smallest production bridge at the engine timeline construction boundary while keeping legacy behavior, route start gate, commands/events, records, Room schema, and TimerDial mapping unchanged unless a separate gate approves them.
