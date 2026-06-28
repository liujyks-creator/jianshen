# E14.4-2 Plan Edit / Detail Polish Visual Proposal

**Status:** Visual proposal complete; Scheme B confirmed; low-coupling implementation review fix completed; E14.4-2b still split out
**Date:** 2026-06-23
**Scope:** Plan empty state, plan list / detail, timed plan editor, strength plan editor, confirmed Scheme B direction, and split-out timed composition / TimerDial story.

## Purpose

E14.4-1 training execution common polish has passed simulation handoff and user real-device focus checks. E14.4-2 is the next UI polish batch, focused on the pre-workout path: creating, editing, reviewing, and starting plans.

The initial pass was a visual proposal confirmation and implementation split only. A later low-coupling implementation pass has now landed the confirmed pieces that do not require E14.4-2b data or TimerDial work.

## E14.4-2 Low-Coupling Implementation Result

The first low-coupling implementation pass is complete. It intentionally implements only the confirmed pieces that do not require timed outer-target composition, TimerDial outer-ring semantic changes, Room schema changes, engine changes, command/event changes, session-record changes, or sound-cue semantic changes.

### 2026-06-23 Review Fix From Real-Device Screenshots

After the first implementation APK, user real-device screenshots showed that the editor pages still differed from the confirmed Scheme B visual direction:

- Timed and strength editor `计划预览` cards still rendered their own `保存计划` and `开始训练` buttons, duplicating the bottom sticky action.
- Timed stage cards were still fully expanded, so the foldable planning pattern was not visible.
- Plan playlist cards could collapse after tapping, but the default first view still opened a plan by default rather than reading as a collapsed playlist.
- A later keyboard screenshot showed the editor sticky action bar being lifted above the IME while editing a numeric field, covering the strength target settings area.
- Follow-up editor testing showed strength action / target-group cards had no drag handle, while timed stage cards could be visually dragged but often snapped back because the drop threshold was too strict.

The review fix keeps the same low-coupling boundary and corrects those implementation deviations:

- Timed and strength preview cards now only show summary, status, validation, and saved-plan information; save/start actions live only in the sticky bottom action.
- Timed stage cards now default collapsed and can be expanded or collapsed from the card header, without adding the E14.4-2b two-level timed composition model.
- Plan management opens as a collapsed playlist by default; tapping a plan expands it, tapping again collapses it, and save/copy/reminder actions still expand the affected plan when appropriate.
- Strength target-group collapse is corrected to cover the whole target settings area: groups default collapsed with a scan summary and expand to weight, reps, rest, per-set targets, and the color entry.
- Editor sticky actions are now hidden while the IME is visible instead of being padded upward above the keyboard; when the keyboard closes, the bottom actions return.
- Strength action / target-group cards now expose the same drag-handle interaction as timed stages, and reordering updates the saved strength block order.
- Timed stage drag now updates the editor order while the finger is still down: crossing half of a neighboring card swaps the order, and dragging back across the threshold swaps it back. `热身` and `放松` are treated as movable default-template stages rather than fixed first / last boundaries. Saving preserves the editor order by emitting plan blocks in the current stage order, grouping only consecutive work / rest / custom stages into circuit blocks.
- Follow-up drag review corrected the live-sort feel: timed stages and strength action cards now keep a drag-start order snapshot, calculate the target slot from the original index plus total finger movement, animate only the non-dragged cards into the placeholder, and commit the real editor order only on drag end. This covers adjacent moves such as item 4 moving below item 5 and longer moves such as item 1 moving past items 2 and 3 without a jumpy two-card swap.
- A second drag review fixed multi-span dragging by keeping the drag-start order immutable while updating only the current target slot. The dragged item can now continue past multiple neighbors in one press, and the same path is used for timed stages and strength action cards. The strength add-action entry was also changed from a cramped horizontal chip row to a modal selector, with existing action-library choices plus a plan-local custom strength action name entry.
- 2026-06-24 top-edge drag screenshot review found a remaining jump when the first visible card is clipped by the top of the screen and the user starts dragging from the upper handle. Code-level simulation showed that keeping the dragged card inside the reordered LazyColumn slot can still let list anchoring and partial clipping pull the handle away from the finger. The fix changes the live-drag model: during drag, the list keeps the dragged item in its original slot, the dragged card moves only by the finger delta, and neighboring cards shift up or down to preview the target placeholder. The real order is committed only after drag end.
- Regression/source-pattern coverage now checks preview-card action removal, sticky action retention, keyboard-hidden sticky behavior, plan playlist collapse/expand, timed card collapse affordance, user-facing copy cleanup, and the no-heart-rate regression search.

This review fix still does not implement E14.4-2b, and it does not change training engines, `WorkoutCommand`, `WorkoutEvent`, session records, Room schema, TimerDial ring semantics, or heart-rate UI.

Simulation note: the review-fix APK builds and passes Gradle verification. AVD `TrainFlow_Pixel_API_36` was started for an adb smoke attempt, but it remained `offline`, so no valid emulator screenshots were captured for the follow-up drag-placeholder fix.

### Drag Implementation Handoff For Future Animation

The final E14.4-2 drag behavior uses a finger-anchored placeholder-preview model for both timed stage cards and strength action cards. This is the implementation contract to preserve if later work enriches the animation:

- On drag start, capture the dragged item id, start index, drag-start order ids, current target index, the dragged card height, the list gap, and the cumulative drag offset.
- During drag, keep the rendered list in the drag-start order. Do not move the dragged item into a newly reordered `LazyColumn` slot while the finger is still down.
- The dragged card stays in its original list slot and receives only the direct finger delta as its visual translation. This keeps the drag handle attached to the user's finger, including when the first visible card is clipped by the top of the screen.
- The target slot is calculated from the start index plus cumulative movement, crossing a neighboring card when the dragged center passes that neighbor's half-height threshold.
- Non-dragged cards between the start slot and target slot shift up or down by the dragged card height plus item gap, creating the visible placeholder where the dragged card would land.
- On drag end, commit the real model order once with `withItemMoved(startIndex, targetIndex)` through `reorderStages` or `reorderExercises`. Cancelling or ending at the same target leaves the order unchanged.

Animation guidance:

- Safe future animation should decorate the non-dragged placeholder shifts, card elevation, scale, opacity, shadow, or placeholder affordance.
- The dragged card's finger translation should stay immediate rather than tweened; otherwise the handle can lag behind or jump away from the press point.
- Do not reintroduce active-drag list reordering as the primary visual model. The earlier live-reordered `LazyColumn` approach caused top-edge jumps and swap-like motion.
- If auto-scroll is later added, preserve the same invariant: the handle remains under the finger, while target calculation includes any scroll delta accumulated during the drag.
- If variable-height cards become more complex, the helper threshold math can be extended, but the finger-anchor plus placeholder-preview contract should remain.

Regression coverage for this contract lives in `PlanEditorDragTest`: half-height threshold targeting, multi-row movement, bounds clamping, item move commit, displacement math, and placeholder shift behavior. The code-level helpers are intentionally shared by timed and strength editors so later animation work does not fork the drag semantics.

Implemented:

- Plans empty state now offers direct create actions for timed plans and strength plans; no fake complete follow-along creation entry was added.
- Plan management now behaves as a foldable plan playlist: tapping a plan expands or collapses that plan card, and `开始训练`, edit, copy, and `删除当前计划` live inside the expanded current-plan card.
- Plan card left swatches display the plan color concept with a safe default red display. Because `WorkoutPlan` has no plan-level persisted color field yet, this pass does not persist plan color or change Room schema.
- Timed and strength editors now use a shared sticky bottom action: green `保存计划` primary action and dark filled `开始训练` secondary action, with bottom reserve above the navigation area; while the keyboard is visible, the sticky action hides instead of jumping above the IME.
- Timed and strength saved-preview copy no longer exposes `WorkoutPlan`, `strength block`, `manual_start`, `planned set`, `interval stage`, or `rest step` as product-screen copy.
- Saved strength plans now backfill target groups collapsed by default. The collapsed state summarizes weight, reps, set kinds, and rest; expanding shows weight, reps, rest, and a color entry note.
- Strength set rest editing uses the existing `StrengthSetPlan.restAfterSec` field and does not change strength target / record semantics.
- Strength target-group color is currently a UI-level placeholder entry only. It records the intended interaction point without persisting a new field.

Regression coverage added:

- Plan-management UI state tests cover playlist expand / collapse, plan-color display, and user-facing detail copy.
- Strength editor tests cover saved-plan target groups defaulting collapsed and per-set rest mapping through the existing plan field.
- Source-pattern tests cover sticky action presence, keyboard-hidden sticky behavior, dark filled start action, and removal of user-facing engineering preview copy.

Still deferred:

- Plan color persistence and editable plan-color picker require a data / persistence decision before any schema or model work.
- Persisted strength target-group color also remains deferred because the current `StrengthSetPlan` model has no color field.
- Strength add-action selection needs a dedicated design/implementation task: the current modal selector is only a low-coupling replacement for the cramped horizontal chips. A later task should design a richer picker with search, categories, clearer action-library cards, better custom-action creation, and small-screen validation.
- Historical at the time of this proposal: E14.4-2b timed composition editor and TimerDial ring semantics were deferred. Superseded by `docs/testing/e14-4-2b-closeout.md`, where the editor + engine + records + TimerDial mapping chain is completed / closed.

## E14.4-2a Confirmation Result

E14.4-2a confirms **Scheme B: structure optimization without a broad redesign**. The confirmed direction can proceed to a later E14.4-2 implementation story in small slices, while the timed composition editor and TimerDial ring-semantics work must be split out.

Confirmed for later E14.4-2 implementation:

- Plan detail uses a **plan playlist** structure: each plan appears as a foldable card, tapping expands or collapses the plan structure, and all actions that affect a plan stay inside that plan card.
- `开始训练` belongs inside the currently expanded plan card, not as a detached detail-page bottom action.
- `删除当前计划` also belongs inside the currently expanded plan card, so users do not read the action as deleting every plan in the playlist.
- Plan color is a user-set plan-level color. It is not auto-derived from the first timed stage or first strength target group.
- Plan color defaults to red in the visual direction and should later be editable through the same large color palette pattern.
- The plan-list / detail-card left swatch can be used as an entry for changing plan color.
- Timed and strength editors use sticky bottom actions: `保存计划` is the green primary button; `开始训练` is a dark filled secondary button and must not use a red filled style.
- Color selection uses a large palette page / sheet with `推荐色`, `更多颜色`, large swatches, and completion action. Editor cards show only the current color swatch and a `选择颜色` entry.
- Timed target color, timed internal stage color, strength target-group color, and plan color should reuse the same color-selection interaction pattern, while keeping their meanings separate.
- Strength target groups default collapsed. The collapsed state behaves like a playlist item and shows the whole target summary; tapping expands detailed settings and tapping again collapses.
- Strength target-group details include weight, reps, rest, and color without changing strength plan or record semantics.
- Engineering copy is removed from user-facing UI: `WorkoutPlan`, `strength block`, `manual_start`, `planned set`, `interval stage`, and `rest step` should not appear in product screens.

Must split out of E14.4-2 implementation:

- Timed composition editor with outer target arrangements and inner stages.
- TimerDial outer-ring semantics that split the ring by internal stage duration ratio.
- Any data-contract, Room schema, engine, `WorkoutCommand`, `WorkoutEvent`, session record, snapshot compatibility, or statistics comparison-key work needed for that timed composition.

## User Direction After Review

The user selected and confirmed **Scheme B** as the preferred implementation direction, with these refinements:

- Editor bottom sticky action primary button should be **Save plan** / `保存计划`.
- Editor bottom sticky action secondary **Start training** / `开始训练` should be a dark filled button, while still staying visually below the green `保存计划` primary action.
- Plan detail should use a plan-playlist structure: the upper area can contain multiple foldable plan cards, tapping a plan expands its structure, tapping again collapses it, and **Start training** / `开始训练` should live inside the expanded plan card instead of as a detached bottom action.
- Plan-detail edit / copy / delete actions should belong to the currently expanded plan card. `删除当前计划` must not sit outside the plan card because that can imply deleting all plans in the list.
- Strength per-set targets should default to collapsed, using foldable cards: tap a target set to expand detailed settings, tap again to collapse, and keep a concise configured summary visible in the collapsed state.
- Timed target cards in the current E14.4-2 scope should use the same foldable-card pattern as strength target sets, without adding the new two-level timed composition model in this story.
- Timed plan color selection should be visual swatches rather than text labels, because tapping color will open a richer color picker.
- Strength plan editing also needs color selection for target groups / blocks.
- Color selection should use a screenshot-matched page-style modal / sheet in the visual model: the editor background dims, a large rounded panel appears, and the panel contains `推荐色`, `更多颜色`, many large circular swatches, and a `完成` action. Editor cards should only show the current color entry, not four inline choices.
- Plan-detail list colors should be user-selectable **plan colors**, not derived from the first main stage / target group. The user noted that derived colors could make many plans look identical. The visual proposal now shows the left plan-list swatch and an expanded-card `选择计划颜色` entry using the same color modal pattern.
- Implementation must confirm how plan color is persisted before coding. If existing plan storage has no suitable field, this should be split into a small data / persistence decision rather than silently changing Room schema inside E14.4-2.
- Engineering preview copy such as `WorkoutPlan`, `strength block`, `planned set`, `manual_start`, `interval stage`, and `rest step` should be explicitly called out in the visual proposal and replaced with user-facing plan summaries in implementation.
- A future user-facing theme color setting is desirable, but it should be split into a later story after E14.4-2. E14.4-2 should keep TrainFlow defaults: green save / dark start / phase and set swatches.

The visual direction is now approved for implementation planning. The next step is not a full direct implementation; it is to implement the low-coupling E14.4-2 pieces separately from the timed composition / TimerDial semantics story.

## Latest Consolidated Discussion Results

The latest HTML visual preview is:

```text
.local/smoke/e14-4-2-plan-edit-detail-visual-proposal/index-v2.html
```

The local preview has continued to explore the B direction, including a later timed composition idea. For E14.4-2 implementation, preserve the confirmed B plan-edit / detail direction below and treat the timed composition screens in the preview as split-story input only.

- **Plan detail / plan playlist**
  - The plan detail page behaves like a playlist of plans.
  - Each plan card can be expanded or collapsed.
  - `开始训练`, `编辑`, `复制`, and `删除当前计划` belong inside the currently expanded plan card.
  - `开始训练` should not sit as a detached bottom action on the plan detail page.
  - `删除当前计划` must not sit outside the plan card, because that can imply deleting every plan in the list.
- **Plan color**
  - The left swatch in the plan list / plan detail card is a user-selectable plan color.
  - It should not be auto-derived from the first timed stage or first strength target, because many plans could end up with the same color.
  - Initial visual default can be red.
  - Clicking the plan swatch, or `选择计划颜色` inside the expanded card, uses the same color modal style as stage / target color.
  - Implementation must confirm persistence before code; if no existing storage field is suitable, plan color should become a small data / persistence story rather than a silent Room schema change in E14.4-2.
- **Editor sticky actions**
  - Timed and strength editors keep sticky bottom actions.
  - `保存计划` is the primary action.
  - `开始训练` is a dark filled secondary action and should not visually exceed `保存计划`.
- **Timed editor target card direction for current E14.4-2 visual scope**
  - Timed target cards use collapsed playlist-like cards.
  - Expanded settings should avoid large low-information cards.
  - Color setting inside a timed target card is a single current-color entry, not four inline color choices.
  - The screenshot-matched color modal contains `推荐色`, `更多颜色`, large circular swatches, and `完成`.
- **Strength editor target card direction**
  - Strength target sets default collapsed like playlist items.
  - Tapping a target set expands detailed settings; tapping again collapses.
  - Collapsed state shows configured summary for scan and drag.
  - Color setting uses the same current-color entry and screenshot-matched modal pattern.
- **Copy cleanup**
  - User-facing preview should remove engineering copy such as `WorkoutPlan`, `strength block`, `planned set`, and `manual_start`.
- **Deferred theme settings**
  - Global theme color customization is accepted as a future independent story.
  - It should not be included in the current E14.4-2 implementation.
- **Deferred timed-training structure**
  - The nested timed-training idea below is recorded as a separate story: **E14.4-2b Timed composition editor and TimerDial ring semantics**.
  - It must not be quietly folded into E14.4-2.

## Split Story: E14.4-2b Timed Composition Editor And TimerDial Ring Semantics

The user confirmed this timed-training direction, but it is too large for ordinary E14.4-2 UI polish and must be planned / visually confirmed before code implementation:

- Timed plans use two visible levels:
  - Outer target arrangements such as `热身`, `高强度工作`, `轮间休息`, and `放松`.
  - Inner stages inside an outer target arrangement, such as `开始 10s`, `加速 10s`, and `休息 10s`.
- Outer target arrangements can be added, deleted, dragged, renamed, and assigned their own color.
- Inner stages can be added, deleted, dragged, renamed, assigned duration, and assigned color.
- Outer target total duration equals the sum of all inner-stage durations.
- TimerDial outer-ring segments should be split by each inner stage's duration ratio.
- The default template can be `热身 / 高强度工作 / 轮间休息 / 放松`, but users can clear it and build a fully custom structure.
- Top-level primary display should no longer rely on labels such as `01 热身 / 02 高强度工作 / 03 轮间休息`.
- Inner stages may keep ordered labels such as `01 开始 / 02 加速 / 03 休息` when useful.

This story may affect plan structure, editor hierarchy, execution visualization, snapshot compatibility, and statistics comparison keys. It must not be folded silently into E14.4-2 implementation, and it must not change `WorkoutCommand`, `WorkoutEvent`, workout engine, session record, sound semantics, or Room schema without a dedicated approved planning / visual gate.

## Inputs Read

- `AGENTS.md`
- `docs/project-status.md`
- `docs/planning/decision-log.md`
- `docs/roadmap-backlog.md`
- `docs/architecture.md`
- `DESIGN.md`
- `docs/ui-extension-guide.md`
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/testing/e14-3-ui-quality-audit.md`
- `docs/testing/e14-4-1-training-execution-common-polish.md`
- `docs/testing/e14-4-2-plan-edit-detail-visual-proposal.md`
- `docs/setup.md`
- `skills/bmad-method/SKILL.md`
- `huashu-design` skill guidance, used only as a visual review lens.
- Product Design `get-context` guidance, used in playback mode because the task brief was already complete.

## Code Surfaces Reviewed Read-Only

- `app/src/main/java/com/liujyks/trainflow/feature/plans/PlanManagementRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/PlanManagementUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/TimedPlanEditorUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorRoute.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/plans/StrengthPlanEditorUiState.kt`
- `app/src/main/java/com/liujyks/trainflow/feature/home/HomeRoute.kt`
- `app/src/test/java/com/liujyks/trainflow/feature/plans/PlanManagementUiStateTest.kt`

## Visual Proposal Gate

This first E14.4-2 pass remains docs-only / mock-only:

- Do not change Kotlin, Compose, Room, tests, Gradle, or production resources.
- Do not generate an implementation APK.
- Do not stage or commit screenshots, APKs, logs, `.local`, `deliverables/`, `人工/`, root APKs, or audio files.
- Do not write to `.local/verification`.
- User confirmation is required before entering implementation.

## Current Issue List

| ID | Page / state | Problem | User impact | Priority |
|---|---|---|---|---|
| E14.4-2-PLAN-01 | Plan empty state | Empty state explains that plans come from the training home, but it does not offer direct create actions for timed or strength plans. | A user who opens the Plans tab first cannot continue from the empty state; they must infer another navigation path. | P0 |
| E14.4-2-PLAN-02 | Plan list | Plan cards are readable, but the action path is indirect: tap card, scroll or inspect detail, then find the start action. | Starting a known saved plan takes more attention than it should. | P1 |
| E14.4-2-PLAN-03 | Plan detail | `Start training` appears after structure rows, reminder controls, edit status, edit/copy/delete actions, and can be buried in a long detail card. | The primary user goal on detail, starting the workout, is visually lower than management actions. | P0 |
| E14.4-2-PLAN-04 | Plan detail actions | Edit, copy, delete, reminder presets, permission request, and start all live in one vertical card with similar button weight. | Users must parse too many controls before deciding what is primary, secondary, or destructive. | P1 |
| E14.4-2-PLAN-05 | Plan detail structure | Timed detail rows summarize stages as text; strength detail rows summarize set kinds as text. Metrics exist, but the structure is not yet glanceable enough on small screens. | Users can miss rounds, rest, set count, or action structure before starting. | P1 |
| E14.4-2-TIMED-01 | Timed editor | Save and immediate start live only in the final preview card. | After editing title, rounds, stages, colors, or cues, users must scroll to the bottom before saving or starting. | P0 |
| E14.4-2-TIMED-02 | Timed editor with keyboard | Text input focus on a small screen makes the bottom save/start card harder to recover. | Editing feels slow even when the user only changed one field. | P1 |
| E14.4-2-TIMED-03 | Timed stage card | Each stage card is fully expanded: name, duration, type chips, guidance copy, color picker, copy/delete, move controls, drag handle. | Stage scanning and reordering become high-friction when the plan has several stages. | P1 |
| E14.4-2-TIMED-04 | Timed stage type and add chips | Stage type and add-stage chips use horizontal scrolling. | Right-side options can be hidden or require horizontal gesture discovery. | P2 |
| E14.4-2-TIMED-05 | Timed preview copy | Saved preview exposes contract-shaped text such as `WorkoutPlan` and `interval stage`. | The UI can feel like a development preview rather than a user-facing planner. | P2 |
| E14.4-2-STR-01 | Strength editor | Save and start live only in the bottom preview card. | After editing multiple actions, users must scroll past every card to complete the task. | P0 |
| E14.4-2-STR-02 | Strength exercise card | Action card mixes exercise identity, cue, weight, reps, working sets, warmup sets, rest, per-set expansion, substitutions, and remove. | The main plan values are present but not quick enough to scan. | P1 |
| E14.4-2-STR-03 | Strength per-set expanded state | Loaded existing plans expand per-set targets by default. | Editing an existing multi-action plan can become long immediately, even if the user only wants top-level changes. | P1 |
| E14.4-2-STR-04 | Strength add-action chips | Remaining strength actions are presented as a horizontally scrolling chip row. | Adding an action is discoverable but inefficient, and long exercise names can make options clip. | P2 |
| E14.4-2-STR-05 | Strength preview copy | Saved preview exposes contract-shaped text such as `strength block`, `planned set`, and `manual_start`. | The plan editor feels less polished than the execution flow. | P2 |

Priority meaning:

- **P0:** Must address before the next implementation APK because it affects the core pre-workout path.
- **P1:** Should address in E14.4-2 if the chosen direction allows it without broad redesign.
- **P2:** Good polish or copy cleanup; can be batched with E14.4-2 if low risk.

## Recommended Design Direction

The pre-workout path should feel like a compact training tool, not a marketing page and not a debug editor. The direction should preserve the current TrainFlow design system: shallow cards, restrained color, clear primary action, and information density that is higher than execution pages but still scannable.

### Empty State

Replace the passive empty card with a tool-like creation panel:

```text
Plans
No saved plans yet.

[Create timed plan]    [Create strength plan]
Timed is the recommended quick start. Strength uses planned weight, reps, sets, and rest.

Follow-along complete plan building is not available yet.
```

Rules:

- The timed create action should be first and use the accent path.
- The strength create action should be the same level but slightly secondary.
- Do not show a complete follow-along creation CTA.
- Keep the explanatory copy short; this is an entry state, not onboarding.

### Plan List / Detail

The plan detail page should behave as a plan playlist. It should separate ownership clearly so actions are visually attached to the current plan:

1. **Foldable plan cards:** Each saved plan appears as a card with plan name, mode, structure summary, and the user-set plan color swatch.
2. **Expanded current plan:** Tapping the card expands the structure, metrics, reminder controls, and plan actions.
3. **Plan-owned actions:** `开始训练`, edit, copy, and `删除当前计划` all live inside the expanded plan card.

Proposed hierarchy:

```text
[color] [Plan title]               [mode]
3 stages · 3 rounds · 预计 12分     [展开/收起]

Metrics: stages / rounds / rest / reminder
Structure preview
Reminder controls
[保存? no, detail only]
[开始训练] [编辑] [复制]
[删除当前计划]
```

The start button should not sit as a detached detail-page bottom action. Delete should stay visually separated as destructive management and remain inside the current plan card, so its scope is not ambiguous.

### Timed Plan Editor

Add a persistent action pattern so save/start is never only at the bottom:

- Top summary after header: current plan summary and save state.
- Bottom sticky action bar on editor screens: `保存计划` green primary, `开始训练` dark filled secondary.
- Keep final preview card, but demote it to a summary/validation section rather than the only action location.

Within the current E14.4-2 scope, timed target cards should become more scan-first without introducing the split-out two-level timed composition model:

```text
[color]  01 Work        45s       [drag]
Training                 icon: work
[Name] [Duration]
Type chips and current-color entry collapsed or compact
[Duplicate] [Delete]
```

Recommended density adjustment:

- Show stage color, order, type label, name, and duration in the card header.
- Keep move / drag affordance in the header.
- Keep name and duration as the default visible edit controls.
- Move type, current-color entry, duplicate, delete, and guidance copy into a compact `More settings` area or a lower-density secondary row.
- The color entry opens the shared large palette pattern; do not show four inline text color choices.
- Add-stage should use 2-row wrapping chips or a bottom sheet-style selector in implementation, not a long single horizontal row.
- Outer target arrangements and inner timed stages belong to E14.4-2b, not to this implementation slice.

### Strength Plan Editor

Strength cards should distinguish default action-level planning from per-set overrides:

```text
Bench Press
20kg · 8-12 reps · 3 working sets · 90s rest

[Weight] [Working sets]
[Reps mode + reps fields]
[Warmup] [Rest]
[Per-set targets collapsed]
```

Recommended density adjustment:

- The card header should summarize weight, reps, working sets, warmup count, and rest in one readable line.
- The main controls should stay action-level by default.
- Per-set / target-group cards should default collapsed, show weight, reps, rest, and color in the collapsed summary when available, and expand only when the user taps them.
- Expanded target-group details include weight, reps, rest, and a shared current-color entry.
- Substitute candidate copy should be moved to low-level metadata or hidden behind details; it should not compete with weight/reps/sets/rest.
- Add action should become a compact selector entry; full unified action selection remains a later story.

### Small Screen Scroll And Bottom Action

For timed and strength editors, the implementation story should use a bottom action reserve similar in spirit to E14.4-1 execution controls, but visually lighter:

- Sticky bottom area above navigation bar.
- Stable minimum button height of at least `48dp`.
- Content bottom padding equal to the sticky action height plus navigation safe-area padding.
- Save/start labels must not wrap on 720 x 1280.
- Keyboard focus should not make the only save/start path disappear.

## Scheme A: Conservative Repair

Scheme A keeps the existing screen structure and mostly rearranges actions.

Changes:

- Add direct `Create timed plan` and `Create strength plan` actions to the Plans empty state.
- Move start training above edit/copy/delete in Plan detail.
- Keep detail card structure, but separate destructive delete into its own low-priority row.
- Add duplicated save/start actions near the top of timed and strength editors, while keeping the bottom preview card.
- Shorten contract-like preview copy to user-facing saved-state copy.
- Keep stage and strength cards mostly expanded.

Pros:

- Lowest implementation risk.
- Smallest test surface.
- Preserves most current Compose layout and state mapping.
- Good enough to unblock user testing quickly.

Cons:

- Does not meaningfully reduce stage / set card length.
- Still leaves long editor pages feeling heavy.
- May create repeated buttons unless the hierarchy is carefully written.
- Strength multi-action editing remains slow on small screens.

Best for:

- A quick E14.4-2 implementation if the next APK needs only primary-action reachability fixes.

## Scheme B: Structure Optimization

Scheme B preserves the current visual language but changes the information architecture of the planning path.

Changes:

- Empty state becomes a two-action tool panel.
- Plan detail gets a top primary action zone and separates structure, reminder, and management.
- Editors gain a sticky bottom action bar for save/start.
- Timed stage cards become scan-first with compact headers and secondary settings rows.
- Strength exercise cards become action-summary-first, with per-set targets collapsed unless needed.
- Add-stage and add-action controls move away from long single-row horizontal chips where possible.
- Development-preview copy is replaced with user-facing status and validation language.

Pros:

- Addresses both reachability and scan density.
- Better matches `DESIGN.md`: simple defaults, deeper controls when expanded.
- Reduces small-screen scrolling fatigue.
- Creates a reusable planning-page pattern for future record/action-library polish.

Cons:

- More implementation work than Scheme A.
- Needs careful regression tests for action availability and plan mapping.
- Requires fresh small-screen screenshots for both editors and plan detail.

Best for:

- The recommended E14.4-2 implementation if the goal is a durable polish pass before broader user testing.

## Comparison

| Criteria | Scheme A: conservative repair | Scheme B: structure optimization |
|---|---|---|
| Implementation risk | Low | Medium |
| Small-screen improvement | Medium | High |
| Stage / set scan improvement | Low | High |
| Time to implement | Short | Moderate |
| Likelihood of needing another polish pass | Higher | Lower |
| Product fit | Acceptable | Stronger |

## Recommendation

Recommend **Scheme B**, implemented in small slices.

Reasoning:

- The largest current issue is not only button position; it is the combination of buried actions and long fully expanded editing cards.
- E14.4-1 already established the value of stable bottom control reserves for training execution. The editor can use a lighter version of that pattern without becoming an execution page.
- Scheme B still avoids a broad redesign: it keeps the existing Compose screens, local plan state, plan contracts, reminders, editor routes, colors, and TrainFlow card language.
- Scheme B better supports the actual first-version product job: define a plan, understand it quickly, save it, and start training.

Implementation should still be broken down so the first commit can land the low-risk action hierarchy before tightening card density.

## Confirmed User Decisions

These points are confirmed for later implementation planning:

1. Collapsed strength target groups behave like playlist items and should show the whole useful target summary, including rest when it helps scanning.
2. Plan detail edit/copy/delete actions appear inside the expanded plan card. `删除当前计划` must not be placed outside the card.
3. Color selection uses the shared large palette pattern with recommended colors and more colors; editor cards only expose a current swatch / `选择颜色` entry.
4. Plan color selection is accepted in the visual direction, but implementation must confirm persistence before code. If it requires schema work, split it into a dedicated data / persistence decision rather than silently changing Room schema inside E14.4-2.
5. Theme color customization is confirmed as a later independent story, not part of E14.4-2 implementation.
6. Timed composition editor and TimerDial ring-semantics changes are confirmed as a separate story, not part of E14.4-2 implementation.

## Later Implementation Split

Recommended split after visual confirmation:

| Slice | Scope | Can be together? | True-device confirmation | Regression coverage |
|---|---|---|---|---|
| E14.4-2 implementation slice 1 | Plans empty-state create actions and plan-playlist detail with plan-owned start/edit/copy/delete. | Yes, first implementation slice. | Optional but useful on 720 x 1280. | PlanManagement UI state / route source-pattern tests for empty create callbacks, start/edit availability, delete ownership, and no fake follow-along edit. |
| E14.4-2 implementation slice 2 | Timed and strength editor sticky bottom actions: green `保存计划`, dark filled `开始训练`, bottom padding reserve, keyboard-safe scroll recovery. | Can pair with slice 1 if kept simple. | Required on small real device. | Source-pattern or Compose-facing tests for button labels, enabled states, no red filled start button, and action reserve constants if introduced. |
| E14.4-2 implementation slice 3 | Shared large palette interaction for plan color, timed target color, and strength target-group color, using existing persistence only if available. | Separate if persistence is unclear. | Useful for palette layout. | StageColorPreset / UI state tests; if plan color needs new persistence, stop and split a data decision story. |
| E14.4-2 implementation slice 4 | Current timed card density only: compact header, secondary settings grouping, shared color entry, add-stage selector polish. | Separate story if risk grows. | Required if card layout changes materially. | Timed editor mapper tests remain unchanged; UI source-pattern tests for copy/delete/move/color availability. |
| E14.4-2 implementation slice 5 | Strength card density: action summary, target groups default collapsed, expanded weight/reps/rest/color details, add-action selector polish. | Separate story recommended. | Required with at least two actions and expanded target groups. | Strength editor mapper tests for planned values, target groups, plan export, and loaded-plan backfill. |
| E14.4-2 implementation slice 6 | Copy polish for user-facing summaries and validation text. | Can be bundled with relevant editor slice. | Not required. | Text/source-pattern tests only if current tests assert the strings. |
| E14.4-2b separate story | Timed composition editor and TimerDial ring semantics: outer target arrangements, inner stages, duration-ratio ring segmentation. | No. Must remain separate. | Required after its own visual confirmation. | Planning / visual gate first; later tests depend on approved model and persistence boundaries. |

## True-Device Smoke Matrix For Implementation

When implementation begins in a later story, request or capture a small matrix under `.local/smoke/e14-4-2-plan-edit-detail-implementation/`:

- Plans empty state: both create buttons visible and tappable.
- Plan list + timed detail: start/edit visible without parsing management controls.
- Plan list + strength detail: start/edit visible and delete separated.
- Timed editor top, mid-stage list, keyboard after editing stage duration, bottom action area.
- Strength editor top, two-action plan, expanded per-set targets, keyboard after editing weight, bottom action area.

Do not save implementation screenshots to `.local/verification`, and do not commit `.local` contents.

## HTML Review Mock

A static webpage-style visual review mock was created for user decision-making:

```text
.local/smoke/e14-4-2-plan-edit-detail-visual-proposal/index.html
```

The mock compares Scheme A and Scheme B across plan empty state, plan detail, timed editor, and strength editor mobile frames. It is a local review artifact only and does not represent Android implementation.

After the user selected Scheme B, a second static webpage-style visual scheme was created:

```text
.local/smoke/e14-4-2-plan-edit-detail-visual-proposal/index-v2.html
```

The v2 mock reflects the selected direction and subsequent user refinements:

- Plan detail uses a foldable plan-playlist structure, with `开始训练` inside the expanded plan card so the action belongs clearly to that plan.
- Plan-detail edit / copy / delete actions are shown inside the expanded plan card so destructive action ownership is clear.
- Timed editor and strength editor both use foldable target cards with collapsed summaries and expanded detailed settings.
- Editor sticky actions make `保存计划` the green primary button and keep `开始训练` as a dark filled secondary button.
- Timed and strength color selection is represented as a single current-color entry in editor cards. The `选择阶段颜色` modal mock follows the supplied screenshot pattern: dimmed background, large rounded panel, `推荐色`, `更多颜色`, many circular swatches, and `完成`.
- Plan-detail list color is a selectable plan color in the visual direction. It is no longer derived from the first training stage or first target group.
- Plan color, timed stage color, and strength target color all use the same modal style, but they are separate concepts.
- The preview copy cleanup is annotated, including `WorkoutPlan`, `strength block`, `planned set`, and `manual_start`.

## Guardrails

This visual proposal must not:

- Restore heart-rate display, manual heart-rate input, unavailable heart-rate placeholders, or average heart-rate trends.
- Connect BLE, Huawei SDK, Health Connect, HealthKit, Wear OS, or real device sources.
- Change `WorkoutCommand`, `WorkoutEvent`, workout engines, session record semantics, Room schema, sound cue semantics, or action-library contracts.
- Expand follow-along into a full course platform.
- Present unimplemented capabilities as available.

## Suggested Evidence Path

Screenshots, mocks, or marked-up images for this visual proposal should stay under:

```text
.local/smoke/e14-4-2-plan-edit-detail-visual-proposal/
```

Do not save them under `.local/verification`, and do not commit `.local` contents.

## Next Step After User Approval

After the user confirms a visual direction, open a separate implementation story for E14.4-2. That implementation story should update Kotlin / Compose only within the confirmed scope, add focused regression tests or source-pattern checks, run Gradle verification, generate a uniquely named APK, and request real-device confirmation.
