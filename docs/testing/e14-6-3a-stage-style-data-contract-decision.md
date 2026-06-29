# E14.6-3a Stage Style Data Contract Decision

**Date:** 2026-06-29
**Status:** Decision complete; E14.6-3b model / serializer tests implemented
**Nature:** docs-only / data-contract decision / model-boundary planning

## Scope

This story decides the stage style data contract and model boundary for E14.6-3. It does not implement Kotlin, Compose, Room, tests, icon picker UI, TimerDial production changes, resources, images, SVG, APK generation, AVD smoke, `.local` output, E12 records / trends polish, or heart-rate UI.

Inputs read:

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
- `docs/readiness-report.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-6-real-device-timerdial-feedback-planning.md`
- `docs/testing/e14-6-3-stage-style-icon-planning.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `skills/bmad-method/SKILL.md`
- Android emulator QA skill, only to confirm this docs-only story does not launch AVD or run smoke.

Startup checks confirmed `main...origin/main` was `0 0`, staged files were empty, and `app/src/main`, `app/src/test`, and `app/src/main/res` had no uncommitted diff. Pre-existing untracked local / forbidden artifacts remain untracked and are not part of this task.

## Code Boundary Findings

Read-only code inspection confirmed:

- Legacy timed style already persists through `TimedExerciseItem.colorHex` and `TimedExerciseItem.iconKey`.
- Composition v2 repeated stage style already persists through `TimedCompositionStageGroup.colorHex` and `TimedCompositionStageGroup.iconKey`.
- Composition v2 target style already persists through `TimedCompositionTarget.colorHex` and `TimedCompositionTarget.iconKey`.
- Boundary stages are currently durations only: `warmupSec`, `cooldownSec`, and `restBetweenRoundsSec`.
- The timeline adapter currently assigns boundary colors / icons from `TimedStageType` defaults.
- `WorkoutPlan` persists blocks through `workout_plans.blocks_json`; `WorkoutSession` persists snapshots through `workout_sessions.plan_snapshot_json`.
- Room is currently version 4. Adding style inside the existing versioned JSON payload does not require a Room table / column migration.

## StageStyle Contract

Stage style means:

```ts
interface StageStyle {
  colorHex?: string;
  iconKey?: string;
}
```

Rules:

- `colorHex` is an optional hex color string.
- `iconKey` is an optional stable string / enum-like key.
- `iconKey` is not an image path, resource path, SVG path, vector path, file name, URL, or uploaded asset reference.
- The first version uses a project-owned built-in white monochrome icon set.
- User-uploaded or custom image assets are post-MVP only.

## Scope Decision

Style applies to:

- Internal composition targets.
- Repeated stage groups.
- Boundary stages:
  - warmup.
  - cooldown.
  - rest between rounds.

Rounds do not have style. A round is a structural repeat count, not a visual stage surface.

Strength can use the built-in `strength` key as a mode or future default icon, but this story does not add strength set style persistence.

## Model Options

| Option | Shape | Pros | Cons | Decision |
|---|---|---|---|---|
| A | Keep existing `colorHex` / `iconKey` on stage groups and targets. Add boundary style fields to composition v2 payload only. | Smallest change, preserves current serializer shape, supports editable boundary styles, no Room migration, legacy plans keep default resolver. | Not perfectly uniform because repeated groups / targets remain flat fields while boundaries use a style object. | **Recommended.** |
| B | Introduce a shared `StageStyle` value object across all style-bearing payload surfaces. | Cleanest long-term type symmetry and easier UI resolver API. | Larger model / serializer churn, would touch group and target contracts already working, increases compatibility risk for no user-visible gain yet. | Postpone. Use `StageStyle` as a contract concept and boundary-field shape, not a full payload refactor. |
| C | Do not persist boundary style; use defaults only. | Zero model change, lowest implementation cost. | Does not support future user-editable warmup / cooldown / round-rest style and repeats the unresolved E14.6-3 gap. | Reject for E14.6-3a. Defaults remain the legacy fallback only. |

## Recommendation

Choose Option A.

Persist user-editable boundary style inside the versioned timed composition JSON payload:

```ts
interface TimedCompositionBlock extends PlanBlockBase {
  kind: "timed_composition";
  compositionVersion: 2;
  warmupSec: number;
  warmupStyle?: StageStyle;
  cooldownSec: number;
  cooldownStyle?: StageStyle;
  rounds: number;
  restBetweenRoundsSec: number;
  restBetweenRoundsStyle?: StageStyle;
  stageGroups: TimedCompositionStageGroup[];
  compatibility?: TimedCompositionCompatibilityMeta;
}
```

Keep existing fields unchanged:

```ts
interface TimedCompositionStageGroup {
  colorHex: string;
  iconKey?: string;
}

interface TimedCompositionTarget {
  colorHex: string;
  iconKey?: string;
}
```

No Room migration is recommended. The style payload lives in existing `WorkoutPlan.blocks` JSON and `WorkoutSession.planSnapshot` JSON.

Snapshots store style as part of the plan snapshot when present. Old snapshots are not rewritten, and edited current plans must not restyle historical sessions.

## Compatibility Rules

- Legacy timed plans remain valid.
- Legacy `TimedExerciseItem.colorHex` / `iconKey` continue to be read and written as before.
- Existing composition v2 plans without boundary style fields remain valid.
- Missing `colorHex` or `iconKey` uses resolver defaults.
- Invalid color or icon key falls back safely.
- Unknown future style fields are ignored by older readers unless a future version explicitly changes payload semantics.
- Unsupported composition versions continue to fail closed.
- Old snapshots are not migrated or rewritten.

## Style Resolver

Resolver priority:

1. Active target style.
2. Parent stage group style.
3. Boundary stage style.
4. Stage / target type default.
5. Final safe fallback.

Boundary style only applies to the synthetic / boundary surfaces it names:

- `warmupStyle` applies to warmup timeline steps.
- `cooldownStyle` applies to cooldown timeline steps.
- `restBetweenRoundsStyle` applies to synthetic between-round rest timeline steps.

Default table:

| Surface | Default `colorHex` | Default `iconKey` | Notes |
|---|---|---|---|
| Warmup | `#F2B84B` | `warmup` | Preparation stage before repeated work. |
| Work / action target | `#F26B4F` | `work` | Normal work target. |
| Speed-up work target | `#F26B4F` | `speed_up` | Faster pace inside work context. |
| Sprint / burst target | `#F26B4F` | `sprint` | High-intensity work target. |
| Rest target | `#2FBF8F` | `rest` | Rest inside a stage group. |
| Between-round rest | `#2FBF8F` | `recover_breathe` | Synthetic rest between rounds. |
| Cooldown | `#65A9FF` | `cooldown` | Downshift after repeated work. |
| Strength mode default | `#795548` | `strength` | Mode-level default only; no per-set style persistence in this story. |
| Mobility / stretch default | `#607D8B` | `mobility` | Future or fallback mobility surface. |
| Custom | `#A8B3BE` | `custom` | Neutral fallback. |

## Built-In Icon Keys

| Key | Semantics |
|---|---|
| `warmup` | Preparation, gradual activation, start-of-training warmup. |
| `work` | Standard work / action target. |
| `speed_up` | Increase pace or intensity within a work stage. |
| `sprint` | High-intensity burst or short peak effort. |
| `rest` | Short rest interval. |
| `recover_breathe` | Longer recovery, between-round breathing, reset interval. |
| `cooldown` | Finish downshift, cooldown, easy ending. |
| `strength` | Strength-training context or future strength default. |
| `mobility` | Mobility, stretch, range-of-motion work. |
| `custom` | User-defined neutral fallback. |

The registry should be closed for MVP. Future icon additions are allowed if they are project-owned built-ins and still referenced by key.

## Validation Rules

`colorHex`:

- Accept only `#[0-9A-Fa-f]{6}`.
- Normalize to uppercase when persisted or resolved.
- Invalid, blank, missing, or non-string values are treated as absent and resolved through defaults.

`iconKey`:

- Accept only known built-in keys.
- Recommended key format is lowercase snake case: `[a-z][a-z0-9_]*`.
- Unknown, blank, missing, or malformed keys are treated as absent.
- Resolver should use the stage / target type default when context exists, and final fallback `custom`.

Asset safety:

- Do not accept remote image URLs.
- Do not accept local file paths.
- Do not accept Android resource paths, SVG paths, vector paths, file names, or uploaded asset ids as `iconKey`.
- Do not persist bitmap, SVG, vector path, or base64 image data in the plan payload.

## Future Extension

Custom uploaded images are explicitly post-MVP.

Before custom images can enter the product, a separate story must decide:

- Asset storage.
- Cropping and safe aspect ratios.
- Sanitization and file type restrictions.
- Backup / sync and export semantics.
- Deletion and orphan cleanup.
- Copyright review.
- Privacy review.
- Open-source customization and theme-pack boundaries.

This story only reserves the fact that custom image work is separate; it does not create upload controls, storage fields, or resource files.

## Implementation Split

Recommended split:

1. **E14.6-3b model / serializer tests** - completed
   - Implemented `TimedStageStyle` and the optional boundary style fields in the versioned composition payload.
   - Covered JSON round-trip for plan blocks and session snapshots.
   - Covered missing / invalid color and icon normalization.
   - Covered legacy timed plans and composition v2 plans without boundary fields.
   - Confirmed no Room schema / resource / TimerDial production changes were needed. See `docs/testing/e14-6-3b-stage-style-model-serializer-tests.md`.

2. **E14.6-3c editor style picker UI**
   - Add stage / target / boundary style picker UI.
   - Use swatches plus built-in icon grid.
   - Keep drag handles, expand / collapse controls, and style entry controls separate.
   - Do not expose upload, crop, remote icon, or custom image controls.

3. **E14.6-3d TimerDial consumption / smoke**
   - Consume resolved style in TimerDial mapping.
   - Verify warmup, cooldown, between-round rest, repeated stage groups, 1-5 targets, legacy fallback, invalid color, and invalid icon behavior.
   - Run visual smoke only in that implementation story.

E12 records / trends polish remains separate.

## Self-Review

- PASS: This is decision / planning only.
- PASS: No Room migration is recommended.
- PASS: Persisted user-editable boundary style lives in versioned timed composition JSON payload.
- PASS: Existing legacy plans and snapshots remain valid through resolver defaults.
- PASS: User-uploaded images are explicitly post-MVP.
- PASS: No resource files, images, SVG, APK, screenshots, logs, `.local`, Kotlin, Compose, Room, or tests are part of this story.
- PASS: Style picker and TimerDial consumption are split into later stories.
- PASS: E12 records / trends and heart-rate UI remain out of scope.
