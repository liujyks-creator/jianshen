# E14.6-3b Stage Style Model / Serializer Tests

**Date:** 2026-06-29
**Status:** Implemented
**Nature:** Kotlin model / JSON serializer / focused unit tests

## Scope

This story implements the E14.6-3a stage style data contract in Kotlin model and JSON storage only. It does not implement editor style picker UI, TimerDial production consumption, Room schema changes, image / icon resources, uploaded images, AVD smoke, APK handoff, E12 records / trends polish, or heart-rate UI.

Inputs read:

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
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- Android emulator QA skill, only to confirm this non-UI story does not launch AVD or run smoke.

Startup checks:

- `git status --short` showed only pre-existing untracked forbidden / local artifacts: root APK, `countdown_beep1.mp3`, `deliverables/`, and `人工/`.
- `git rev-list --left-right --count main...origin/main` returned `0 0`.
- `git log -8 --oneline` showed main ending at `7d20ac8 Document stage style data contract decision`.
- `git diff --cached --name-only` was empty.
- `. .\.local\env.ps1` loaded local JDK / Android SDK successfully.
- `git diff --name-only -- app/src/main/res` was empty.

## Implementation

Model changes:

- Added `TimedStageStyle` with optional `colorHex` and optional `iconKey`.
- Added the closed MVP built-in icon key contract:
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
- Added optional boundary style fields to `TimedCompositionBlock`:
  - `warmupStyle`
  - `cooldownStyle`
  - `restBetweenRoundsStyle`
- Kept existing `TimedCompositionStageGroup.colorHex` / `iconKey` and `TimedCompositionTarget.colorHex` / `iconKey` as flat fields.

Serializer changes:

- `WorkoutPlan.blocks` JSON now writes and reads `warmupStyle`, `cooldownStyle`, and `restBetweenRoundsStyle` for `timed_composition` v2 payloads.
- `WorkoutSession.planSnapshot` JSON round-trips the same fields because snapshots use the same storage serializer.
- Legacy `TimedCircuitBlock` / `TimedExerciseItem` JSON shape remains unchanged.
- Existing v2 payloads without boundary style fields still decode with those style fields as `null`.

## Validation Rules

`colorHex`:

- Accepts only `#[0-9A-Fa-f]{6}`.
- Normalizes valid values to uppercase.
- Invalid, blank, or missing values become `null` on `TimedStageStyle`.
- Existing stage group / target color fallback remains through `normalizeStageColorHex`.

`iconKey`:

- Accepts only known built-in keys.
- Uses lowercase snake-case format before registry lookup.
- Unknown, blank, malformed, URL-like, path-like, resource-like, SVG-like, file-name-like, base64 image-like, or uploaded asset-like values become `null`.
- No image path, Android resource path, SVG, URL, bitmap, base64 image, or uploaded asset reference is persisted.

## Tests

Added / updated focused tests:

- `TimedStageStyleTest`
  - Valid color / known icon normalization.
  - Invalid color and unknown icon normalization.
  - Built-in icon key contract coverage.
  - URL / path / resource / SVG / image-like / uploaded-like key rejection.
- `TimedCompositionModelTest`
  - Boundary style normalization on `TimedCompositionBlock`.
  - Stage group and target `iconKey` normalization to the built-in contract.
- `TimedCompositionStorageJsonTest`
  - v2 plan JSON round-trip preserves warmup / cooldown / between-round style.
  - session snapshot JSON round-trip preserves boundary style.
  - old v2 payload without style fields still decodes.
  - legacy timed plan JSON remains non-composition and does not gain boundary style fields.

Existing full unit tests continue to cover Room database version remaining `4`.

## Verification

Passed:

```powershell
. .\.local\env.ps1
.\gradlew.bat app:testDebugUnitTest --tests "*StageStyle*" --tests "*TimedComposition*" --no-daemon --console=plain
.\gradlew.bat app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat app:assembleDebug app:lintDebug --no-daemon --console=plain
```

Passed boundary checks:

- Whitespace diff check.
- Cached diff emptiness check.
- Room / schema / resource diff check.
- Plans UI / TimerDial production diff check.
- Resource and image filename diff scan.
- Old composition editor entry scan.
- Heart-rate regression term scan.
- Deprecated design-skill reference scan.

Notes:

- `git diff --check` passed with Git line-ending warnings only.
- Cached diff, Room / schema / resource diff, and UI / TimerDial production diff were empty.
- Resource / image search, old entry search, and deprecated design-skill search returned no matches.
- Heart-rate regression search returned only existing historical documentation / guard references, not new production code.

## Self-Review

- PASS: E14.6-3a Option A is implemented in Kotlin model and JSON serializer.
- PASS: Boundary style fields are optional and live only in versioned timed composition JSON payloads.
- PASS: Legacy timed plans and old v2 payloads remain compatible.
- PASS: Invalid style input is normalized to safe absence / existing fallback behavior.
- PASS: Built-in icon key contract is model-level only; no resources, images, SVG, or upload support were added.
- PASS: Room database version and schema were not changed.
- PASS: No editor style picker UI was implemented.
- PASS: No TimerDial production consumption changes were made.
- PASS: No AVD was launched and no `.local/smoke` or `.local/verification` output was written.
