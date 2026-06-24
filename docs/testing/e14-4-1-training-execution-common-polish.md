# E14.4-1 Training Execution Common Polish

**Status:** Implemented and user real-device checked
**Date:** 2026-06-22
**Scope:** Timed / strength / follow-along execution-page common bottom-control polish after E14.3 audit.

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
- `docs/testing/e14-3-ui-quality-audit.md`
- `docs/setup.md`

## Fix Summary

- Added a shared `TrainingExecutionBottomControlsSpec` for strength and follow-along execution pages.
- The shared spec keeps primary and secondary fixed-bottom controls at least `48dp`, includes navigation-bar safe-area reserve, and calculates a bottom content reserve from the real two-row control band.
- Follow-along execution content now uses the shared fixed-bottom reserve, preventing the countdown / current action card from being hidden behind the fixed controls on small screens.
- Strength execution content now uses the same reserve for prepare / active / confirm / rest states, so confirmation fields and rest-state content are not squeezed into the fixed bottom controls.
- Strength fixed-bottom primary action now always uses the shared minimum height, not only Big Type mode.
- Timed execution Timer Dial layout was not redesigned. The E14.2 square / concentric Timer Dial and `+15s` / `确认+15s` stable bottom-control behavior are preserved and covered by regression tests.

## Guardrails

This change does not:

- Restore heart-rate display, manual heart-rate input, unavailable heart-rate placeholders, or average heart-rate trends.
- Connect BLE, Huawei SDK, Health Connect, HealthKit, Wear OS, or any real device source.
- Change `WorkoutCommand`, `WorkoutEvent`, timed or strength workout engines, session record semantics, Room schema, or sound cue semantics.
- Expand follow-along into a full course platform.
- Touch `.local/verification`.

## Regression Coverage

`TrainingExecutionRegressionUiStateTest` now additionally constrains:

- Shared execution bottom controls reserve exists and includes a navigation safe-area allowance.
- Follow-along and strength screens consume the shared content reserve and keep `navigationBarsPadding()`.
- Follow-along and strength primary / secondary bottom controls use shared minimum heights.
- Strength confirm / rest immediate-control semantics remain unchanged.
- Timed ready / running / paused / rest state semantics remain unchanged.
- `+15s` and `确认+15s` labels remain compact and stable.

## Smoke Evidence

Simulation / real-device handoff screenshots should be saved under:

```text
.local/smoke/e14-4-1-training-execution-polish/
```

Do not save screenshots, APKs, logs, or verification artifacts under `.local/verification`, and do not commit `.local` contents.

The 2026-06-22 verification handoff covered:

- `timed_ready.png`, `timed_running.png`, `timed_paused.png`, `timed_rest.png`, `timed_confirm_15s.png`
- `strength_prepare.png`, `strength_active.png`, `strength_confirm.png`, `strength_rest.png`
- `follow_running.png`, `follow_paused.png`

The user then confirmed the four real-device focus areas had no issue:

- Timed execution ready / running / pause / rest / `确认+15s`, including square / concentric Timer Dial and stable bottom controls.
- Strength execution prepare / active / confirm / rest bottom reserve.
- Follow-along countdown and current content no longer hidden by fixed bottom controls.
- All three execution pages keep navigation-bar safety and complete tappable bottom buttons.

## Next UI Polish Gate

E14.4-2 `Plan edit / detail polish` must start as visual proposal only. It should not change Kotlin / Compose / Room / tests or generate an implementation APK until the user confirms the proposed plan-edit and plan-detail UI direction.
