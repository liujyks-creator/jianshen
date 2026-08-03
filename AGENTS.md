# AGENTS.md

## Project

This repository contains the early product baseline and frontend prototype for TrainFlow, an Android-first fitness training assistant with a future iOS path.

TrainFlow is not a generic fitness content feed. Its first job is to turn a user-defined workout plan into a clear training execution flow with useful records afterward.

## Read First

Before making product or implementation changes, first use `rg --files -g AGENTS.md` and read this file plus any closer applicable `AGENTS.md` files.

Then read the core context:

1. `docs/project-status.md`
2. `docs/planning/decision-log.md`
3. The current story's testing, decision, or review document when one exists

Add only the documents relevant to the task type:

- New product capability, product decision, PRD, or UX flow: `docs/planning/product-brief.md`, `docs/planning/prd.md`, and `docs/planning/ux-design.md`.
- Data contract, Room, persistence, engine, command, event, or session work: `docs/planning/data-contracts.md` and `docs/architecture.md`.
- UI, Compose, layout, theme, interaction, or visual review: `DESIGN.md`, `docs/ui-extension-guide.md`, and the relevant approved visual/design decision.
- Roadmap, readiness, phase status, or docs-only work: `docs/roadmap-backlog.md` and `docs/readiness-report.md` when affected.
- Environment, Gradle, AVD, APK, adb, or test-command work: `docs/setup.md`.
- Prototype work: `prototype/src/data/contracts.ts` and the relevant prototype files.

Do not read unrelated long planning documents by default. Expand the read set when the task crosses boundaries, a current decision is unclear, or the focused documents point to another source.

Treat `docs/planning/decision-log.md` as the compact record of accepted decisions. Use longer planning documents for rationale and detail only when they are relevant.

## Current Product Baseline

The first product baseline is:

- Android first, future iOS adaptation.
- Timed training and strength training are parallel first-version capabilities.
- Timed training is the recommended default entry for new users.
- Follow-along training is only an early partial experience in the first version.
- The action library contract comes before scaling action content.
- Training execution must reserve boundaries for future voice interaction and heart-rate device integration.

## First-Version Boundaries

Keep the first version focused on:

- Plan editing for timed and strength workouts.
- Workout execution, countdowns, rest handling, reminders, and session records.
- Action library selection and action detail guidance.
- Strength set confirmation with planned values prefilled for actual records.
- A partial follow-along view that reuses the timed flow and action content.
- Basic recovery recommendations mapped from trained areas.

Do not silently expand the first version into:

- A full course platform.
- Large coach video libraries.
- Automatic voice coaching.
- AI real-time form correction.
- Full music beat choreography.
- Medical diagnosis or medical-grade heart-rate alerts.

If scope changes, update the decision log and the relevant planning document in the same change.

## Data And Architecture Boundaries

Preserve these modeling choices unless a documented decision changes them:

- `Exercise` is a standard action-library item, not a saved plan item.
- `WorkoutPlan` stores targets and structure.
- `WorkoutSession` stores actual execution results and a plan snapshot.
- Timed workouts progress through timed steps, rests, rounds, and reminder thresholds.
- Strength workouts progress through actions and sets, including start-set, complete-set, confirm-record, and rest states.
- UI controls and future voice controls should map to workout commands.
- Sound, vibration, animation, analytics, and future voice output should consume workout events.
- Heart-rate UI should consume an abstract heart-rate state rather than a device-specific SDK model.

The TypeScript prototype contracts live in `prototype/src/data/contracts.ts` and mirror the planning draft in `docs/planning/data-contracts.md`.

The Android production architecture and MVP implementation sequence live in `docs/architecture.md` and `docs/roadmap-backlog.md`.

The implementation readiness gate lives in `docs/readiness-report.md`. Check it before starting Android engineering work, especially E0.1.

## Prototype Guidance

The current `prototype` directory is a React/Vite UX prototype. It validates the product flow and data boundaries; it is not the Android production app.

When editing the prototype:

- Reuse the existing fixture and contract structure before inventing new models.
- Keep training execution screens scannable during exercise.
- Keep real-time heart rate visually secondary to the current action, time, set, weight, and reps.
- Validate timed-work reminders and rest reminders as separate states.
- Preserve the strength flow where planned weight and reps prefill the completion record.

Run relevant checks from `prototype`:

```powershell
npm.cmd run lint
npm.cmd run build
```

## Design Direction

Frontend work should prioritize an actual usable training experience over a marketing landing page.

The established UX direction is:

- Simple defaults during creation, deeper controls when expanded.
- Rich editing pages, restrained workout execution pages.
- Strong countdown feedback only when the workout state deserves it.
- No fake first-version controls for reserved capabilities that do not work yet.

Later Figma work should use the current UX documents and prototype as inputs, not replace product decisions by accident.

When changing UI, theme, layout, or components, read `DESIGN.md` first. When changing open-source customization boundaries, read `docs/ui-extension-guide.md` and preserve the core training engine, command, event, and data-contract semantics.

## Workflow And Design Skills

This project may use three complementary Codex skills when they are available:

- `$bmad-method` for accepted-state reconstruction, product or architecture planning, Story decomposition, project/Epic readiness, exact Story shaping, Story-ready validation, planning Review, and correct-course. It exits immediately after one exact Story is independently `ready`; it does not govern an approved Story's Dev/Review loop.
- `$supervised-story-delivery` when the user explicitly authorizes automatic delivery of an already approved Story or finite ordered Story sequence. The primary conversation remains the manager while native subagents perform project inspection, writing, validation, fresh Review, Repair, integration, and post-merge verification.
- `huashu-design` for UI, design-system, theme, token, layout, interaction, high-fidelity prototype, design-variant, motion, and visual-review work.

These roles are complementary. BMAD selects or corrects the route, supervised delivery executes an authorized route, and `huashu-design` preserves visual discipline. `huashu-design` MUST NOT be removed or replaced by either workflow skill.

The skills are global Codex capabilities rather than repository dependencies, and their directories MUST NOT be committed. A repository-local ignored `skills/bmad-method` copy, if present, is a legacy local aid rather than the active method. Root prompt templates define the generic management, Writer, and Fresh Reviewer role inputs/outputs and also provide a manual fallback; automatic delivery uses the skill plus native subagents and does not treat those templates as a second workflow authority.

Skills cannot override accepted project instructions, decisions, Story scope, validation gates, evidence requirements, or explicit user authority. If `$bmad-method` is unavailable, route from accepted repository documents. If `$supervised-story-delivery` is unavailable, stop automatic mode and offer the manual fallback instead of silently imitating it. If `huashu-design` is unavailable, preserve accepted visual decisions and request direction before creating a new subjective design.

Automatic native-agent delivery only replaces the user's manual copy/paste step. A formal Writer or Repair dispatch MUST be the complete filled `DEV_STORY_PROMPT_TEMPLATE.md` content in one outer block, and a formal Reviewer or re-Reviewer dispatch MUST be the complete filled `CODE_REVIEW_PROMPT_TEMPLATE.md` content in one outer block. Fill every field from immutable accepted facts, verify that no placeholder remains unresolved, and do not substitute a freehand summary or abbreviated packet. This relay rule does not create a repository workflow platform, canonical contract, validator, manifest, receipt system, CI system, or project-specific role catalog.

Every dispatched role MUST fully read the exact applicable global skills, the accepted `AGENTS.md` from its pinned base, its complete accepted role template, and only the Story/decision/testing/evidence documents relevant to its task before acting. Its terminal report MUST identify every source by path plus immutable blob/hash or full SHA where available and explicitly confirm each complete read; do not preload unrelated skills or long document bundles.

Repeat those complete reads for every new Agent, new task or task recovery, explicit context-compaction/summary signal, role or phase change, accepted-base change, skill-file or template-file change, or inability to prove the accepted base, candidate, completed gate, or next transition. Resolve repository sources with `git show <accepted-base>:<path>` or a worktree proven at that base; if a path is absent, record it as absent and never substitute a same-named file from another checkout, branch, commit, or dirty overlay. Ordinary progress messages do not require repeated full reads while those facts remain provable. After compaction, reconstruct the accepted base, candidate SHA, remote refs and ancestry, current role/attempt and terminal status, validation/evidence identity, completed gates, and first incomplete gate from Git, accepted Story/testing/evidence documents, and terminal reports; do not replay completed roles. Do not create a persistent ledger, workflow workspace, manifest, receipt system, or report-package platform for this recovery.

Automatic Story delivery has one acceptance order: Writer candidate → Candidate Validation → required human acceptance or explicitly authorized fully automatable acceptance → fresh complete Review. A Writer that produced a verifiable candidate returns `WRITER_COMPLETE / DONE` with next gate `CANDIDATE_VALIDATION`, even when later device or visual acceptance is required. `NEEDS_USER` before Candidate Validation is allowed only when no candidate exists and a user product, authority, or external action is required before one can exist. Candidate Validation binds exact SHA, scope, commands, artifact identity, and protected state before any candidate is sent to the user or Reviewer; missing human evidence is never PASS.

Fresh verification means independent evidence against the exact identity, not an automatic full-repository suite. Follow the Story/template validation profile in proportion to risk: a tiny local UI change normally uses focused UI/static/compile evidence plus the required screenshot or human candidate check; ordinary local logic uses focused tests plus affected regression; concurrency, persistence, migration, security, shared ownership, and platform lifecycle may require broader targeted validation. Writer, Candidate Validator, acceptance Validator, Reviewer, and integration each run only the evidence required for that layer.

A Reviewer remains read-only through its complete scope, acceptance, validation, evidence, Git, and protected-state review, waits for every explorer it started, and emits findings only once in exactly one complete terminal `REVIEW_COMPLETE` batch. Progress, partial findings, duplicate output, a report with missing required fields, or any output without `REVIEW_COMPLETE` is nonterminal and MUST NOT start Repair or integration. Repair receives the complete verified finding batch unchanged, then a fresh Validator and a different fresh Reviewer perform validation and re-Review.

The different fresh re-Reviewer MUST repeat the complete Review rather than perform a scoped findings-only check. Two unsuccessful Repair cycles on the same risk axis, or a Repair that crosses core product, architecture, ownership, data, lifecycle, persistence, security, or integration boundaries, MUST route to BMAD Correct Course for root cause, ripple audit, `retain | adapt | replace | retire | defer` classification, and the smallest affected planning boundary.

Liveness counts only intervals without meaningful concrete progress. After two minutes without progress, send nudge #1; after another two inactive minutes, send nudge #2 as the final nudge; after a third inactive interval, confirm nonresponse, interrupt/close, and replace from immutable facts. Meaningful progress resets the timer, even for an hour-long role. Partial Review output is nonterminal progress and never starts Repair. Confirm a mutating role is closed before another Writer starts.

## Cross-Conversation Source Of Truth

- Do not rely on a previous model's or conversation's implicit memory. Cross-model and cross-conversation handoffs must be reconstructed from the current `main` branch, accepted decision-log entries, Story documents, tests, evidence records, and Git history.
- A pushed branch, an accepted review report, or completed manual testing does not by itself unlock a dependent Story. The prerequisite is merged only when its immutable required full commit SHA is an ancestor of `main`, `main` matches `origin/main`, and the applicable status documents agree.
- Before starting a dependent Story, fetch `origin` and verify each named prerequisite with `git merge-base --is-ancestor <required-full-commit-sha> main`. A branch name may be recorded only as a locator for resolving and cross-checking that immutable SHA; never use a movable or deleted branch tip as the downstream unlock fact. If any check fails, stop before creating a branch or modifying files and return to the missing review / merge / docs-sync gate.
- If a prompt, status document, and Git history disagree, do not choose the most convenient version. Treat Git ancestry as the merge fact, then resolve the documentation inconsistency in a scoped reviewable change before continuing.
- Review Story scope from the merge base, because `main` may advance after the Story branch is created. After fetching and confirming remote synchronization, use `git diff origin/main...origin/<story-branch>` (three-dot) or an explicit merge-base-to-Story diff. Do not use `git diff main..<story-branch>` (two-dot) for Story scope; it can misreport later `main` changes as reverse Story changes.

## Working Habits

- Read the current repo state before changing files.
- Keep edits scoped to the requested task and current product boundary.
- Add or update documentation when a product decision changes.
- Prefer explicit branches for feature work, using `codex/<task-name>` when a branch is needed.
- Do not commit secrets, device logs, generated build output, or unrelated local files.

## Text Encoding

Repository text files are read and written as UTF-8.

On Windows, set PowerShell console encoding before reading Chinese Markdown, Kotlin, Gradle, JSON, or other text files:

```powershell
chcp 65001 > $null
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
```

Read files with explicit UTF-8, for example `Get-Content -Raw -Encoding UTF8 <path>`. Prefer `apply_patch` for code and documentation edits. If PowerShell must write a text file, write UTF-8 without BOM through .NET APIs instead of relying on default `Set-Content` or `Add-Content` behavior.

Do not report routine recoverable console encoding noise to the user. If a file still cannot be read reliably as UTF-8, do not guess or rewrite it; inspect it read-only for BOM or byte-level encoding clues and report the specific file before editing.
