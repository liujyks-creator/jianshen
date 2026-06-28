# E14.6-2 Completion Recap Page Redesign Planning

**Status:** Planning complete; visual gate ready for implementation split
**Date:** 2026-06-28
**Scope:** Docs-only planning / visual gate for the post-workout completion recap page

## 1. Goal

Real-device feedback showed that after finishing a timed workout, TrainFlow still visually feels like an execution page: the large TimerDial remains the dominant object and the completion content reads like a terminal/debug card. The next product direction is to switch the completed terminal state into a dedicated "本次数据统计复盘页面".

This page should confirm completion, give the user a small positive finish moment, and then summarize the actual session data already available from the existing summary and session record pipeline.

## 2. Inputs Read

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
- `docs/planning/e10-training-mode-interaction-plan.md`
- `docs/planning/timer-dial-design-workflow.md`
- `docs/testing/e14-6-real-device-timerdial-feedback-planning.md`
- `docs/testing/e14-6-1-timerdial-progress-rebound-fix.md`
- `docs/testing/e14-4-2b-closeout.md`
- `docs/testing/e14-4-2b-5c-session-record-compatibility.md`
- `docs/testing/e14-4-2b-6c-timerdial-mapping-smoke-visual-qa.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- `huashu-design` skill and relevant workflow/content references for restrained visual planning
- Local `skills/bmad-method/SKILL.md` for story split and roadmap consistency
- Android emulator QA skill, only to confirm this round does not launch an emulator or run smoke

## 3. Source Audit, Read-Only

No Kotlin, Compose, Room, or test files were modified in this planning story.

Read-only source inspection confirmed:

- Current timed terminal rendering is in `TimedWorkoutSessionRoute.kt`, where `uiState.isTerminal` routes into the existing terminal screen instead of a dedicated recap page.
- Existing reusable timed recap data is already modeled by `TimedWorkoutSummaryUiState`.
- Existing reusable strength recap data is already modeled by `StrengthWorkoutSummaryUiState`.
- History / record detail mapping already reads persisted `WorkoutSession` records and plan snapshots, so the completion page does not need a new record semantic or fake statistics.

## 4. Information Architecture

The completed terminal state should become a recap page with this hierarchy:

1. **Top celebration completion area**
   - Show a compact completion badge, check mark, or small celebratory mark.
   - Clearly label the status as `已完成`.
   - Include a short completion phrase such as `本次训练已完成`.
   - The plan name or training type can appear as supporting text, below the status.

2. **Key summary metrics**
   - Timed sessions should reuse existing summary values: duration, completed stages / rounds, step progress, skipped steps, and rest extension summary.
   - Strength sessions should reuse existing summary values: completed actions / sets, planned-vs-actual records, set duration, actual rest, replacements, and skipped items.
   - If pause duration is already available in the relevant session summary or record detail, it may be shown as a secondary metric. If not available in the current UI state, do not invent it.

3. **Existing recap / data overview**
   - Reuse the existing completion recap content and session summary panels.
   - Keep rest extension, skipped content, early-end notes, trained area, recovery entry, and per-action / per-stage details where already supported.
   - Do not introduce new trend charts, aggregate insights, health data, or unimplemented comparisons in this page.

4. **Rest extension, skipped, pause, and early-end details**
   - Rest extension must continue to reflect `timedRestExtensionRecords` / existing summary text.
   - Skipped steps or skipped sets must continue to come from existing execution history.
   - Pause summary must come from existing `pausedElapsedSec` or current summary mapping when exposed.
   - Early-ended sessions must not be mislabeled as completed.

5. **Bottom return action**
   - The primary bottom action should be `返回训练首页`.
   - The button should sit in a bottom safe area and remain reachable on small screens.
   - A first implementation can fall back to the current plan/list callback only if navigation cannot yet reach the training home, but the product target is returning to the top-level training workspace.

## 5. Visual Direction

The page should feel like an earned finish moment, not a marketing landing page.

- Use a restrained celebration treatment: a success badge, subtle halo, small check mark, or light celebratory particles.
- Use TrainFlow success / accent colors for completion state, but keep the page calm enough for a post-workout, sweaty-hands context.
- Do not use the large TimerDial as the main visual after completion.
- If any TimerDial reference remains, it should only be a small completion badge or training-type marker, not the dominant control surface.
- Avoid oversized hero cards, fake decorative stats, heavy gradients, confetti overload, or a page that looks detached from the training product.
- Keep current action, time, set, weight, reps, and actual session data above decorative content.

## 6. State Split

- `ready`, `running`, `paused`, and `rest` remain execution-page states.
- `completed` moves into the dedicated completion recap page.
- `abandoned` should use the same recap shell only as an ended-session summary, with different tone and label such as `已结束` or `提前结束`.
- `abandoned` must not show the completed celebration treatment or say `已完成`.

## 7. Motion

- Completion may use a single lightweight entry motion for the badge / check / halo.
- Motion should be short and non-blocking; it must never delay record writing, navigation, or the availability of the return action.
- With reduce-motion enabled, the celebration motion should be disabled or snapped to a static completed state.
- TimerDial continuous projection must not keep running in `completed` or `abandoned`.

## 8. Return And Secondary Entry Recommendation

Default return recommendation: `返回训练首页`.

Reasoning:

- A completed workout is the end of an execution flow, so the user should land in the training workspace rather than feel pushed back into an execution residue.
- Returning to a plan detail page can remain an implementation fallback or later contextual route, but it should not define the completion page's information architecture.
- The first version should avoid two competing primary buttons.

`查看记录` can be added later as a low-hierarchy text action after the session id and record-detail route are available. It should not compete visually with the primary return action in the first completion recap implementation.

## 9. Existing Content Reuse

The completion recap page should reuse:

- Existing timed completion summary items from `TimedWorkoutSummaryUiState`.
- Existing strength completion summary items from `StrengthWorkoutSummaryUiState`.
- Existing per-session record detail semantics from `WorkoutSession` and plan snapshots.
- Existing recovery entry content, where already available.

The page must not:

- Change `WorkoutSession` semantics.
- Change `WorkoutSession.planSnapshot`.
- Change `timedRestExtensionRecords`.
- Add Room schema or migration work.
- Introduce E12 aggregate trends / charts.
- Reintroduce heart-rate display, manual heart-rate input, or heart-rate trends.

## 10. Implementation Split Recommendation

Recommended split:

1. **E14.6-2b Compose implementation**
   - Build the dedicated completion recap page.
   - Reuse current summary UI state and session summary content.
   - Route `completed` away from the execution-dial-dominant terminal visual.
   - Keep `abandoned` as an ended-session recap with separate tone.
   - Preserve records, commands, events, engine state, TimerDial mapping, and Room semantics.

2. **E14.6-2c smoke / visual QA**
   - Run emulator or real-device visual checks after implementation.
   - Capture evidence for small screen, completed, rest extension, skipped steps, abandoned, and reduce-motion where feasible.

Optional split:

- **E14.6-2a static visual mock / prototype** only if a screenshot-level visual direction is required before Compose implementation. Current planning is clear enough to proceed directly to E14.6-2b unless the user wants a visual mock first.

## 11. Test / Smoke Plan For Implementation

The implementation story should cover:

- Completed legacy timed session.
- Completed v2 timed composition session.
- Session with rest extension records.
- Session with skipped timed steps.
- Abandoned / ended-early session behavior.
- Strength completed summary if the shared completion recap shell is reused there.
- 720x1280 small screen and bottom navigation / safe-area behavior.
- Reduce-motion path if feasible.

This planning story does not run Gradle, launch AVD, generate APK, or write smoke artifacts.

## 12. Self-Review

- PASS: This story is planning / visual gate only; no implementation files are modified.
- PASS: Completed state is explicitly planned to leave the large TimerDial execution page and enter a dedicated recap page.
- PASS: Existing recap content and `WorkoutSession` record semantics are reused; no session record semantics change is planned.
- PASS: Celebration is positive but restrained, not a marketing page.
- PASS: Bottom return behavior is defined, with `返回训练首页` as the recommended default.
- PASS: E14.6-3 stage color/icon system and E12 records/trends implementation stay out of scope.
- PASS: No `.local`, APK, screenshots, videos, audio resources, logs, `deliverables/`, or `人工/` artifacts are part of this story.
