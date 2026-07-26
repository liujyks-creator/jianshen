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

For Story planning, development, Repair, Review, evidence, merge, or governance work, also read `docs/process/story-workflow-contract.md`. During Review, read the accepted copy from the pinned review-base commit; a Story's proposed change to that contract cannot govern its own Review.

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

- `$bmad-method` for task routing, product or architecture planning, Story decomposition, readiness, planning Review, and correct-course. It decides whether work is ready and which route applies; it does not govern an approved Story's Dev/Review loop.
- `$supervised-story-delivery` when the user explicitly authorizes automatic delivery of an already approved Story or finite ordered Story sequence. The primary conversation remains the manager while native subagents perform project inspection, writing, validation, fresh Review, Repair, integration, and post-merge verification.
- `huashu-design` for UI, design-system, theme, token, layout, interaction, high-fidelity prototype, design-variant, motion, and visual-review work.

These roles are complementary. BMAD selects or corrects the route, supervised delivery executes an authorized route, and `huashu-design` preserves visual discipline. `huashu-design` MUST NOT be removed or replaced by either workflow skill.

The skills are global Codex capabilities rather than repository dependencies, and their directories MUST NOT be committed. A repository-local ignored `skills/bmad-method` copy, if present, is a legacy local aid rather than the active method. The three root prompt templates are mandatory role skeletons: Main Control for Manager, Preflight, Validator, and Device Prep; Dev Story for Writer and Repair Writer; Code Review for Fresh Reviewer. Every automatic role validates packet version, template path/commit/blob, role, base/candidate, exact echo, and lease fields before action; free-hand packets cannot acquire a lease. The templates also provide the manual fallback and are not a second workflow authority.

Skills cannot override accepted project instructions, decisions, Story scope, validation gates, evidence requirements, or explicit user authority. If `$bmad-method` is unavailable, route from accepted repository documents. If `$supervised-story-delivery` is unavailable, stop automatic mode and offer the manual fallback instead of silently imitating it. If `huashu-design` is unavailable, preserve accepted visual decisions and request direction before creating a new subjective design.

Role leases may be 60/90 minutes or longer when the approved packet and tool timeouts justify them; large roles remain atomic and report at natural phase boundaries. At expiry the manager may renew once only with concrete immutable progress evidence, otherwise it enters recovery.
All automated Story, Writer, Repair, Validator, Reviewer, and device roles use the repository contract's template binding, automatic role execution lease, durable ledger, and recovery rules. The same rules govern root-template automation; they do not govern ordinary question answering. Preserve approved atomic Story boundaries: recovery resumes from ledger, Git, and disk instead of rebuilding or decomposing the Story. The manager advances routine passing phases without asking whether to continue and closes or reports Process Flow issues according to the accepted contract.

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
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
```

Read files with explicit UTF-8, for example `Get-Content -Raw -Encoding UTF8 <path>`. Prefer `apply_patch` for code and documentation edits. If PowerShell must write a text file, write UTF-8 without BOM through .NET APIs instead of relying on default `Set-Content` or `Add-Content` behavior.

Do not report routine recoverable console encoding noise to the user. If a file still cannot be read reliably as UTF-8, do not guess or rewrite it; inspect it read-only for BOM or byte-level encoding clues and report the specific file before editing.
