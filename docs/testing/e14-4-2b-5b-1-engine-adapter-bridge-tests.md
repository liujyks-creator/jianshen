# E14.4-2b-5b-1 Engine Adapter Bridge Tests

**Date:** 2026-06-27
**Nature:** Test-first implementation gate
**Status:** Focused bridge expectation tests added; original red gate consumed by E14.4-2b-5b-2 minimum engine bridge.

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

## Original Red Result

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

Because this was an intentional test-first red gate, the full unit suite was not claimed green until E14.4-2b-5b-2 implemented the minimum bridge.

## Follow-Up Implementation Result

E14.4-2b-5b-2 implemented the minimum bridge in `TimedWorkoutEngine` and kept the existing route start gate disabled.

Result after the bridge:

- The focused command now passes.
- V2 `TimedCompositionBlock` plans expand through `TimedCompositionTimelineAdapter` at the engine timeline construction boundary.
- Adapter step ids become engine step ids.
- `compositionBlockId` maps to `TimedSessionStep.blockId`.
- Real target ids and synthetic boundary / between-round rest target ids map to `TimedSessionStep.itemId`.
- V2 rest target and synthetic between-round rest steps are rest-extendable.
- V2 warmup, work, custom, and cooldown steps are not rest-extendable.
- Legacy timed plans continue through the existing `TimedCircuitBlock` path.
- Unsupported / empty v2 structures fail closed without falling back into legacy timed execution.

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

Next story should be **E14.4-2b-5b-3 v2 start gate enablement and smoke planning / implementation gate**. It must still avoid TimerDial mapping until the separate E14.4-2b-6 story.
