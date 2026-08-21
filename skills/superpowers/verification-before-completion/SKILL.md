---
name: verification-before-completion
description: Use before claiming a candidate is complete, fixed, passing, ready, or safe to commit or push; maps the exact claim to fresh, risk-matched evidence and reports any missing gate honestly.
---

# Verification Before Completion

## Purpose and Authority

Evidence must precede every completion, correctness, readiness, commit, or push claim. This skill defines a verification method; it does not grant scope, permission, integration authority, review authority, or permission to replace a human or device gate.

First apply user and system instructions, repository and role contracts, the immutable task or complete finding batch, and accepted product and code contracts. Fix the exact candidate identity, allowed scope, protected and unrelated state, accepted behavior and non-goals including excluded states, baseline failures or warnings, observable success and failure, and required evidence layers. Stop if a new authority or architecture decision is needed.

Verification does not authorize a fix. Candidate regressions return to the approved implementation scope; unrelated baseline issues remain evidenced and reported. Trust accepted internal invariants and use real-boundary evidence where required. Do not create a one-use helper, wrapper, script, manager, validation layer, retry, fallback, silent default, or broad catch merely to make an oracle pass.

## The Claim-to-Evidence Gate

Before a positive claim:

1. **Claim:** State exactly what is complete, fixed, passing, or ready, and identify the candidate being evaluated.
2. **Risks:** List every risk directly covered by that claim: changed behavior, direct consumers, state or persistence boundaries, artifact identity, scope, and any external, device, or human gate.
3. **Oracles:** Select a real oracle for each applicable risk.
4. **Fresh run:** Run the smallest complete set of selected commands and evidence steps against the current candidate.
5. **Full result:** Let each selected command finish; read its complete relevant output, exit code, failure and warning counts, and produced artifact identity.
6. **Honest state:** Make only the claim the evidence supports. Otherwise report the actual result, missing gate, and recovery condition.

Old output, another candidate's artifact, source inspection alone, or confidence is not fresh evidence.

## Complete Command vs. Complete Risk Set

A **complete selected command** is one chosen oracle run from start to terminal exit without truncating it, stopping after a favorable line, or extrapolating from a subset.

The **claim-bound minimum complete risk set** is the collection of commands and evidence steps needed to cover every applicable risk in the exact claim. It may contain a focused test plus directly affected regressions, a build plus an artifact identity check, or a device or human gate in addition to automation.

These concepts are complementary. Neither means “run the entire repository by default.” Run a repository-wide suite only when the governing task, an accepted project gate, or demonstrated risk propagation requires it. Never call a focused set “all tests.”

## Choose Oracles That Match the Risk

| Risk or claim | Direct evidence | Does not prove it |
|---|---|---|
| Consumer behavior | Focused RED/GREEN plus directly affected regression | Source text or compilation alone |
| Compilation/build | Fresh required build with exit 0 | Lint or unit tests alone |
| Artifact correctness | Fresh artifact plus identity and relevant inspection/run | An older artifact or source diff |
| Scope/protected state | Candidate diff, index, and protected-state comparison | Passing behavior tests |
| External integration | Real boundary response and original error handling | A permissive mock |
| Production wiring | Production-path integration evidence | An injected seam, fake, or no-op |
| Physical device | Identity-bound physical-device evidence | Unit test, source inspection, or emulator |
| Human experience | The specified human acceptance | Screenshot existence or automated assertion |
| Requirements | Acceptance-to-evidence walkthrough | A keyword, regex, or format validator |

Automated evidence proves only its layer. Preserve required identity-bound Reviewer, physical-device, and human gates; a Writer's verification does not complete them.

## Candidate and Baseline Discipline

Verify against the immutable candidate identity when the workflow provides one. If an executable source changed, rebuild the corresponding artifact unless exact equivalence is independently proven.

For any failure or warning, determine whether the candidate introduced it. A candidate regression within scope blocks the claim and must be fixed. A proven pre-existing or unrelated failure is preserved and disclosed; it does not authorize unrelated changes and prevents only claims that include the failing set.

A warning blocks completion only when the candidate introduced it, it makes a required command fail, or the accepted contract forbids it. Otherwise report it accurately without claiming pristine global output.

## Before Commit, Push, or Terminal Report

Verify every acceptance criterion with its actual oracle, not merely a passing test count. Check the exact candidate diff and allowed paths, format or artifact requirements, index state, protected state, and remote identity required by the role contract. Read the complete results before committing; after a commit or push, verify the new immutable identity and remote state where required.

Do not claim an unperformed Reviewer, merge, physical-device, human, Android, or external gate. Do not move to the next role yourself when the governing workflow assigns that responsibility elsewhere.

## Stop and Failure Signals

Stop and report the actual state when an oracle cannot run, its output is incomplete, the candidate identity is uncertain, a required evidence layer is unavailable, or verification would expand scope or authority.

Failure signals include: “should” or “probably” replacing a run, a partial command represented as complete, a large unrelated suite substituted for the direct oracle, a successful build represented as behavior proof, an emulator represented as a physical device, old evidence reused for a new candidate, or a Writer represented as an independent Reviewer.
