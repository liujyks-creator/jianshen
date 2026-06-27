# E14.4-2b-4 Editor UI Visual/Code Gate

**Date:** 2026-06-27
**Status:** Implemented and pushed as `d8d784d`; closed for E14.4-2b-5 planning handoff

## Scope

This slice connects the timed plan editor UI to the E14.4-2b-3 timed composition editor draft adapter.

Implemented:

- Compact `基础时间与轮次` card for `热身`, `放松`, `轮次`, and `轮间休息`.
- `阶段编排` as collapsible stage cards.
- Collapsed stage header limited to swatch, stage name, derived stage duration, expand/collapse entry, and drag entry.
- Expanded stage editing for name, derived total duration, target list, add target, copy stage, and delete stage.
- Stage and target colors are edited by tapping the row-leading rounded swatch; expanded panels do not duplicate a color card.
- Target rows with independent collapsed / expanded settings state.
- Separate target settings entry and drag entry.
- Expanded target editing shows target name, direct numeric duration input, copy, and delete; the target kind remains in the draft model but is not exposed as a visible field in this compact pass.
- Target copy inserts a matching target immediately after the source target, with the same name, duration, color, and kind, while still respecting the 5-target limit.
- Max 5 targets per stage in UI state and tests.
- Save exports editor-side timed composition v2 payload through the draft adapter.
- v2 `开始训练` remains disabled with copy: `待执行映射完成后可开始`.
- Saved v2 plans remain editable from plan detail but cannot start execution from plan detail.
- Bottom navigation keeps compact labels `训 / 计 / 动 / 录` as a small-screen polish found during the editor smoke pass; full content descriptions remain, and navigation / training semantics are unchanged.

Deferred:

- Complex target drag animation.
- Advanced cue setting UI.
- Complete target-kind icon library.

## Explicit Non-Scope

This slice does not implement:

- `TimedWorkoutEngine` timeline expansion.
- TimerDial production mapping.
- Training execution page display for v2.
- Session record v2 semantics.
- Room schema migration.
- `WorkoutCommand` or `WorkoutEvent` changes.
- Sound cue semantic changes.
- Heart-rate UI, input, statistics, devices, or medical alerts.

## Next Step

Review / commit the E14.4-2b-5 docs-only planning gate, then enter E14.4-2b-5a timeline adapter model/tests. Do not skip directly to engine integration or TimerDial production mapping.
