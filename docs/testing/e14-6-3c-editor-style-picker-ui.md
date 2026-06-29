# E14.6-3c Editor Style Picker UI

**Date:** 2026-06-30
**Status:** Implemented; final verification completed in this story
**Nature:** Narrow Compose editor implementation, focused tests, Android smoke

## Scope

This story exposes the E14.6-3b timed composition stage style payload in the timed plan editor only.

Implemented:

- Boundary stage style editing for warmup, cooldown, and rest between rounds.
- Stage group style editing.
- Target style editing.
- One picker flow that combines existing color swatches with a built-in icon grid.
- Editor draft persistence into the existing v2 timed composition JSON payload.

Not implemented:

- TimerDial production consumption.
- New icon, image, SVG, drawable, or raw resources.
- User-uploaded images, file pickers, gallery access, URLs, or asset paths.
- Room schema / migration changes.
- Session record, workout engine, command, or event changes.
- E12 records / trends polish.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/planning/decision-log.md`
- `docs/planning/data-contracts.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/testing/e14-6-3-stage-style-icon-planning.md`
- `docs/testing/e14-6-3a-stage-style-data-contract-decision.md`
- `docs/testing/e14-6-3b-stage-style-model-serializer-tests.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `docs/planning/product-brief.md`
- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/readiness-report.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill
- Android emulator QA skill

## Implementation

Editor state and draft adapter:

- `TimedCompositionEditorDraft` now carries optional `warmupStyle`, `cooldownStyle`, and `restBetweenRoundsStyle`.
- `TimedCompositionBlock.toDraft()` reads those fields from v2 payloads.
- V2 export writes those fields back through the existing `TimedCompositionBlock` serializer.
- Legacy timed plans still produce `null` boundary styles in the editor draft.

Picker state:

- Added `TimedCompositionBoundaryStyleTarget` with exactly three editable boundary targets:
  - warmup
  - cooldown
  - rest between rounds
- Rounds intentionally have no style target.
- Added style picker UI state that wraps the existing color picker swatches and adds the closed built-in icon set:
  - `warmup`
  - `work`
  - `speed_up`
  - `sprint`
  - `rest`
  - `recover_breathe`
  - `cooldown`
  - `strength`
  - `mobility`
  - `custom`
- Picker labels are user-facing Chinese labels; engineering keys are not used as primary picker text.

Compose UI:

- The basic time / rounds card now has a `阶段样式` section with entries for warmup, cooldown, and rest between rounds.
- Stage group swatches now open the combined `阶段样式` picker.
- Target swatches now open the combined `目标样式` picker.
- Existing recommended / more color swatch sections are reused.
- Icon choices are rendered as a grid of white monochrome Compose Canvas glyphs, with no resource additions.
- Swatches and icon buttons include content descriptions and selected state descriptions.

## Tests

Added / updated focused tests:

- Boundary styles update warmup, cooldown, and rest-between-rounds payload fields through save.
- Rounds do not expose a boundary style target.
- Stage group and target style updates persist color and icon.
- Picker state keeps color swatches and user-facing Chinese icon labels.
- V2 draft adapter reads / writes boundary styles.
- Legacy timed draft adapter output keeps boundary styles absent.

Existing E14.6-3b tests still cover invalid color and invalid / asset-like icon key normalization.

## Verification

Required commands for this story:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*StageStyle*" --tests "*TimedComposition*" --tests "*PlanEditor*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
git diff --check
```

Android smoke evidence path:

```text
.local/smoke/e14-6-3c-editor-style-picker-ui/
```

## Boundary Confirmation

- No `TimerDial.kt` or `TimerDialUiState.kt` production consumption change.
- No `TimedWorkoutEngine` change.
- No Room database or schema change.
- No `WorkoutCommand` / `WorkoutEvent` change.
- No `app/src/main/res` changes.
- No images, SVGs, uploaded asset paths, URLs, gallery access, or file picker support.
- No heart-rate UI, manual heart-rate input, average trend, BLE, Health Connect, or medical alert behavior.
