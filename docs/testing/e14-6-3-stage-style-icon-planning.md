# E14.6-3 Stage Style / Icon Planning

**Date:** 2026-06-29
**Status:** Planning complete; ready for split implementation stories
**Nature:** docs-only / visual-system planning / data-contract review

## Scope

This story only plans the stage style and built-in icon system. It does not implement Kotlin, Compose, Room, tests, icon picker UI, TimerDial production changes, APK generation, AVD smoke, `.local` output, E12 records / trends polish, or heart-rate UI.

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
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/testing/e14-6-real-device-timerdial-feedback-planning.md`
- `docs/testing/e14-6-1-timerdial-progress-rebound-fix.md`
- `docs/testing/e14-6-2-completion-recap-page-planning.md`
- `docs/testing/e14-6-2b-completion-recap-page-compose.md`
- `docs/testing/e14-6-2d-completion-recap-screenshot-recapture.md`
- `docs/testing/e14-4-2b-closeout.md`
- `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `huashu-design` skill for restrained visual-system planning
- `skills/bmad-method/SKILL.md` for roadmap / contract consistency
- Android emulator QA skill only to confirm this planning story does not launch AVD or run smoke

Startup checks:

- `git status --short` showed only pre-existing untracked forbidden/local artifacts: root APK, `countdown_beep1.mp3`, `deliverables/`, and `人工/`.
- `git rev-list --left-right --count main...origin/main` returned `0 0`.
- `git log -8 --oneline` showed main ending at `7a3f3b4 Document completion recap screenshot recapture`.
- `git diff --name-only -- app/src/main app/src/test` was empty.
- `git diff --cached --name-only` was empty.
- No AVD was launched.

## Planning Conclusion

Stage style is a small contract-level concept, not only decoration. A style means:

- `colorHex` or a resolved color token.
- `iconKey` that points to a project-owned built-in icon.

The first version should allow style resolution for:

- Warmup.
- Cooldown.
- Between-round rest.
- Normal repeated stage groups.
- Internal composition targets.

Rounds do not receive color or icon. A round is a repeat count / structure marker, not a stage surface.

## Built-In Icon Set

The first version uses only built-in white monochrome icons owned by the project. Stored plan data should keep stable icon keys, not image paths, vector paths, file names, URLs, or uploaded asset references.

Recommended initial keys:

| Key | Meaning |
|---|---|
| `warmup` | Warmup / prepare movement |
| `work` | Work / action target |
| `speed_up` | Accelerate / increase pace |
| `sprint` | High-intensity burst |
| `rest` | Rest interval |
| `recover_breathe` | Recovery / breathing |
| `cooldown` | Cooldown / finish downshift |
| `strength` | Strength training context |
| `mobility` | Mobility / stretch / range work |
| `custom` | User-defined neutral fallback |

Icon rendering rule: the icon is white or otherwise high-contrast monochrome, placed on the stage color in the TimerDial center circle, stage swatch, target swatch, or other compact style surface.

Invalid or missing icon keys must fall back to the stage / target type default icon, then to `custom`.

## Color System

Use the existing stage color / recommended preset system instead of inventing a separate palette.

Required decisions:

- Warmup, cooldown, and `restBetweenRounds` should have independent default colors.
- Repeated stage groups and internal targets continue to use saved `colorHex`.
- TimerDial should avoid a noisy rainbow by recommending a compact default set and encouraging only meaningful color differences.
- High-attention colors should remain marked and used sparingly for work, sprint, or alert-like phases.

Fallback order:

1. Active target style color.
2. Parent stage group style color.
3. Boundary stage default color for warmup / cooldown / between-round rest.
4. Stage or target kind default safe color.
5. Final safe fallback if data is invalid.

## Data Contract Impact

Existing timed models already contain style fields in the places that matter for repeated stages and targets:

- Legacy `TimedExerciseItem` has `iconKey?: string` and `colorHex?: string`.
- Composition v2 `TimedCompositionStageGroup` has `colorHex` and `iconKey?: string`.
- Composition v2 `TimedCompositionTarget` has `colorHex` and `iconKey?: string`.

Current top-level `warmupSec`, `cooldownSec`, and `restBetweenRoundsSec` are durations, not user-authored target rows. E14.6-3a has now decided the JSON payload shape for user-editable boundary-stage styles: keep existing stage group / target `colorHex` and `iconKey` fields, and add optional `warmupStyle`, `cooldownStyle`, and `restBetweenRoundsStyle` objects inside the versioned timed composition payload only. Each style object contains optional `colorHex` and optional `iconKey`.

No Room migration is recommended for the first style/icon implementation. Style should remain inside existing plan JSON / snapshot JSON, including the new boundary style fields when present. Adding a Room table or column requires a separate migration story.

WorkoutSession snapshots should preserve whatever style existed at training start. Historical records must not be restyled from an edited current plan unless a future story explicitly adds a historical style migration.

Strength currently has stable `StrengthSetPlan` ids and set kinds, but no style fields equivalent to timed `colorHex` / `iconKey`. The built-in `strength` icon can be used as a default mode icon, but per-set strength style persistence should not be added silently in this story.

## UI Planning

Recommended labels:

- `阶段样式` for stage group / warmup / cooldown / between-round rest.
- `目标样式` for internal composition targets.

Picker behavior:

- Color and icon can be selected in the same style panel.
- Use swatches and an icon grid, not a long text list.
- Show recommended colors first, then more colors.
- Show built-in icons as tappable tiles with accessibility labels.
- Selection state must not rely on color alone; use outline / check / selected semantics.
- Drag handles, expand / collapse controls, and style entry buttons must be separate to avoid accidental drag while changing style.

The first picker implementation should not expose upload, crop, asset library, remote icon, or custom image controls.

## TimerDial Planning

TimerDial consumption should preserve the accepted E14.4-2b-6 semantics:

- Outer ring segments still follow current stage group target planned-duration ratio.
- Inner ring still expresses whole-workout stage progress.
- The 12 o'clock number marker still uses the whole-workout stage count.
- `+15s` still extends only the current active rest step and does not insert a target or resize planned ratios.

Style consumption:

- Center circle icon uses the active target `iconKey` when present, else the active stage group icon, else boundary style, else the boundary / type default.
- Center circle fill uses the active target color when present, else stage group color, else boundary style, else boundary / type default.
- Warmup, cooldown, and between-round rest use their persisted boundary style when present and valid; otherwise they use their own default color and icon even when they are synthetic timeline steps.
- Outer ring segments use resolved style color for targets and fallback stages.
- Missing or invalid style data must not crash drawing or change training execution.

The pale support ring below the internal stage ring can be made thicker later as TimerDial visual polish. It is not part of this data/style planning story.

## Post-MVP Custom Image Plan

User-uploaded images, custom image libraries, crop tools, remote icon packs, and user-owned picture storage are post-MVP / later-story work.

The current documentation may reserve an interface direction, but this round does not implement:

- Upload.
- Image cropping.
- Local image storage.
- Backup / sync.
- Copyright review.
- Custom asset deletion.
- Remote marketplace.

## Follow-Up Split

Recommended split:

1. **E14.6-3a data contract / model decision** - completed
   - Decision: persist optional `warmupStyle`, `cooldownStyle`, and `restBetweenRoundsStyle` in the versioned composition JSON payload.
   - Decision: keep existing stage group / target fields flat for compatibility.
   - Decision: no Room migration unless a future story explicitly adds a new table / column.

2. **E14.6-3b model / serializer tests**
   - Add boundary style fields and focused round-trip / fallback tests.
   - Keep legacy timed plans and old snapshots valid.
   - Confirm invalid color / icon fallback and no resource-path or uploaded-image persistence.

3. **E14.6-3c editor UI style picker**
   - Implement the stage / target style picker with swatches + icon grid.
   - Preserve drag / expand / style-entry separation.
   - Add editor UI and accessibility tests.

4. **E14.6-3d TimerDial consumption / visual QA**
   - Resolve style from active target / stage group / boundary style / boundary defaults.
   - Verify warmup, cooldown, between-round rest, 1-5 targets, legacy fallback, invalid color, and invalid icon behavior.
   - Capture visual QA evidence after implementation.

4. **TimerDial visual polish follow-up**
   - Consider a thicker pale support ring under the internal stage ring.
   - Keep separate from model / icon planning unless explicitly merged by a future story.

## Testing And Smoke Plan

Future implementation tests should include:

- Data contract / serializer tests for `colorHex`, `iconKey`, invalid icon fallback, invalid color fallback, legacy timed plans, composition v2 plans, and snapshot preservation.
- Editor picker tests for swatch selection, icon grid selection, accessibility labels, drag-handle separation, and no upload controls.
- TimerDial mapper tests for target color first, stage group fallback, boundary-stage defaults, type default fallback, 1-5 target segments, warmup, cooldown, between-round rest, and no round color/icon.
- Smoke / visual QA for warmup, cooldown, between-round rest, 1 target, 2 targets, 5 targets, legacy fallback, invalid icon fallback, and no visible heart-rate regression.

This planning story does not run Gradle or AVD smoke.

## Self-Review

- PASS: Planning only; no implementation files are modified by this story.
- PASS: Rounds are explicitly structure only and do not need color or icon.
- PASS: Warmup, cooldown, and between-round rest are stages for style resolution.
- PASS: First icon version is a built-in white icon-key set.
- PASS: User-uploaded custom images are post-MVP / later story only.
- PASS: No resource files, images, SVG, APK, `.local`, screenshots, or logs are part of the planned changes.
- PASS: TimerDial support-ring thickness is recorded only as later visual polish.
- PASS: E12 records / trends polish remains separate.
- PASS: Heart-rate UI / input / trends remain out of scope.
