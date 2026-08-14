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

## Workflow, Design Skills, And Manual Role Relay

This repository uses a manual role relay:

- The primary management conversation reconstructs accepted facts, chooses the next gate, fills the applicable root prompt template, and evaluates returned terminal reports.
- The user copies that complete prompt into a new independent Dev, Repair, or Review conversation and copies the complete terminal report back to the primary management conversation.
- The primary management conversation MUST NOT call native collaboration agents, automatically dispatch roles, or run an automatic Story-delivery state machine for this repository.

The project uses only the following project-local skill sources. These paths, not similarly named global or plugin-cache copies, are authoritative for this repository:

- `skills/bmad-method/SKILL.md` for accepted-state reconstruction, product or architecture planning, Story decomposition, readiness, planning Review, and Correct Course. At one exact ready Story it returns control to the primary management conversation, which prepares the manual Dev prompt.
- `skills/superpowers/test-driven-development/SKILL.md` for behavior changes and bug fixes.
- `skills/superpowers/systematic-debugging/SKILL.md` only after a failure or unexpected behavior requires root-cause debugging.
- `skills/superpowers/verification-before-completion/SKILL.md` before a Writer claims completion or commits.
- `skills/design-md/SKILL.md` only for `DESIGN.md`, design-system, theme-token, or component-contract work. Broader UI decisions remain governed by the accepted product and UX sources listed above.

Do not load Superpowers orchestration, subagent, brainstorming, worktree, branch-finishing, or Review-dispatch skills for this workflow. A Reviewer follows `CODE_REVIEW_PROMPT_TEMPLATE.md` directly and does not load BMAD or implementation workflow skills. Skills are advisory and cannot override accepted project instructions, the complete filled root role template, decisions, Story scope, validation gates, evidence requirements, or user authority. External/global skill directories are not project sources and MUST NOT be committed.

Formal role contracts are exclusive:

- Writer and Repair: one complete filled `DEV_STORY_PROMPT_TEMPLATE.md` outer block with zero unresolved placeholders.
- Reviewer and re-Reviewer: one complete filled `CODE_REVIEW_PROMPT_TEMPLATE.md` outer block with zero unresolved placeholders.
- Do not create a freehand Role Packet, abbreviated substitute, split dispatch, additional delivery role, or repository CI/orchestration platform.

The immutable Story, approved finding batch, and validation artifact own task-specific objective, old→new, acceptance criteria, and validation details. When a role can read that identity-bound source locally or from Git, the root prompt references it instead of reproducing those sections. A filled prompt carries only the role, accepted/candidate identities, exact allowed paths and actions, required gates, protected state, and terminal report requirements needed to execute safely.

All user-facing manual-relay artifacts MUST use Simplified Chinese. This includes primary-management delivery, complete copy-ready prompts, role dispatches, Writer/Repair terminal reports, Reviewer/re-Reviewer findings and terminal reports, human-test instructions, and next-step handoffs. Preserve exact technical identities—SHA values, refs, paths, commands, code symbols, filenames, tool names, and fixed machine-readable status tokens—in their original form when translation could make them ambiguous. The accepted root prompt templates themselves MUST be written in Simplified Chinese. A role may read English source material, but its user-facing delivery remains Chinese unless the user explicitly requests another language for that artifact.

Every complete copy-ready primary-management, Dev/Repair, and Review/re-Review prompt MUST end with a user-facing runtime recommendation selected by the primary management conversation for that specific role and task:

```text
Recommended Codex runtime:
- Model: <management-selected model>
- Reasoning effort: <management-selected level>
- Rationale: <one concise task-specific sentence>
```

The selection MUST consider task complexity, correctness risk, expected context load, tool use, and cost; it MUST NOT be a permanently fixed model or reasoning level. The footer is a reminder for the user to select the recommended runtime manually before starting the new conversation. It does not authorize an agent to change its own model or reasoning level. The complete relayed prompt must contain concrete values and zero unresolved placeholders.

Each new manually created role conversation reads the applicable skill once, the accepted `AGENTS.md` from its pinned base, its accepted role template, the immutable task source, and only directly relevant evidence. It does not produce a separate read manifest. Writer, Repair, Reviewer, and re-Reviewer conversations MUST NOT create subagents; if genuinely independent work is needed, the primary management conversation prepares a separate manual role prompt for the user to relay.

Automatic context compaction inside the same conversation is not a cold restart. Use the system summary as a locator, confirm the current role and first unfinished action, and reread only a source whose critical fact is unclear or changed. Do not replay completed work or roles.

A genuinely new management or role conversation performs its template's cold-start reads once. A role or phase change is handled by a new manually created conversation and its applicable complete template, not by reloading every source in the existing conversation.

Manual delivery order is:

```text
primary management prepares Dev/Repair prompt
→ user relays it to a fresh Writer conversation
→ Writer validates, commits and pushes the Story branch but never merges
→ user relays WRITER_COMPLETE to primary management
→ required identity-bound human/device acceptance, when applicable
→ primary management prepares Review prompt
→ user relays it to a fresh independent Reviewer conversation
→ Reviewer completes one full current-node Review
   ├─ PASS: same Reviewer performs mechanical --no-ff merge and push
   └─ not PASS: no edits or integration; return one complete findings/report batch
→ user relays REVIEW_COMPLETE to primary management
```

The primary management conversation, not the Reviewer, decides the next Repair or Correct Course after a non-PASS report. A Repair uses a new Writer conversation with the complete finding batch; re-Review uses a different fresh Reviewer and repeats the full current-node Review.

“Full Review” means complete coverage of the currently authorized node: its exact base/candidate delta, accepted contract, acceptance criteria, directly affected behavior, required evidence, Git gates, and protected state. It does not authorize re-auditing the whole repository, all historical Stories, upstream skills/plugins, or unrelated modules. Finding one issue blocks integration but does not end the remaining current-node Review; the Reviewer completes all remaining applicable axes and returns one atomic findings batch unless an objective authority, safety, or evidence blocker makes the remainder impossible.

Repair is minimum but causally complete, not the fewest changed files. The Writer must address the complete approved finding batch in one Repair attempt. If the same Story has two consecutive complete Reviews with must-fix findings and another Repair would change core ownership, architecture, data responsibility, or multiple module boundaries, stop the local patch loop and return to a scoped Correct Course instead of continuing incremental Repair.

## Evidence-First Minimal Execution

- Do not assume or hide uncertainty. Separate proven facts, reproducible inference, and unknowns; expose the affected decision and trade-off. An unrelated unknown does not block proven work.
- Before mutation, define the current problem, success criteria, and observable proof. Implement only the minimum causally complete change, touch only necessary paths, and clean up only what the task created.
- Do not add speculative features, incidental refactors, future compatibility, or one-use helpers, wrappers, managers, registries, adapters, or abstractions.
- Trust accepted internal contracts, types, tests, and framework guarantees. Validate at real boundaries such as user input, network, external APIs, devices, and persisted data. Do not add fallback, null/default handling, or defensive branches for states the accepted contract excludes.
- Never swallow failures through broad catches or silent defaults. Fail fast on real invariant violations and preserve the original error signal.
- E16 lesson: before implementation, bind the exact production change set, ownership/lifecycle boundaries, and evidence layers. A Repair that needs a new core interface, platform wrapper, callback owner, scheduler/test seam, or unapproved cross-module responsibility returns to Correct Course. Helper existence, source strings, fake/no-op behavior, AVDs, and injected seams do not prove production or physical-device behavior.

Fresh verification means independent evidence against the exact candidate identity, not an automatic full-repository suite. Use only the validation required by the Story's risk profile. UI/physical-device evidence and other human gates remain identity-bound and cannot be replaced by fakes, AVDs, or source inspection.

For Android UI, APK, screenshot, or smoke work, reuse the existing JDK, Android SDK, system image, AVD, and device configuration named by `docs/setup.md` or the filled Story prompt. Do not install or upgrade SDK components, download another system image, create/clone/wipe an AVD, or replace the configured emulator merely to obtain a fresh run unless the user explicitly authorizes that environment change. Store generated screenshots, UI trees, logs, and device evidence only in ignored `.local/` evidence paths, and never stage or commit them unless the Story explicitly adopts named files.

## Cross-Conversation Source Of Truth

- Do not rely on a previous model's or conversation's implicit memory. Cross-model and cross-conversation handoffs must be reconstructed from the current `main` branch, accepted decision-log entries, Story documents, tests, evidence records, and Git history.
- A pushed branch, an accepted review report, or completed manual testing does not by itself unlock a dependent Story. The prerequisite is merged only when its immutable required full commit SHA is an ancestor of `main`, `main` matches `origin/main`, and the applicable status documents agree.
- Before starting a dependent Story, fetch `origin` and verify each named prerequisite with `git merge-base --is-ancestor <required-full-commit-sha> main`. A branch name may be recorded only as a locator for resolving and cross-checking that immutable SHA; never use a movable or deleted branch tip as the downstream unlock fact. If any check fails, stop before creating a branch or modifying files and return to the missing review / merge / docs-sync gate.
- If a prompt, status document, and Git history disagree, do not choose the most convenient version. Treat Git ancestry as the merge fact, then resolve the documentation inconsistency in a scoped reviewable change before continuing.
- Review Story scope from the merge base, because `main` may advance after the Story branch is created. After fetching and confirming remote synchronization, use `git diff origin/main...origin/<story-branch>` (three-dot) or an explicit merge-base-to-Story diff. Do not use `git diff main..<story-branch>` (two-dot) for Story scope; it can misreport later `main` changes as reverse Story changes.

## Working Habits

- Read the current repo state before changing files.
- On Windows, prefer the already installed PowerShell 7 (`pwsh`) for UTF-8, hashing, and validation when available. Do not install or upgrade PowerShell as a routine task step.
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
