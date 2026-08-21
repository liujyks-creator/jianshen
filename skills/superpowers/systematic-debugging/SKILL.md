---
name: systematic-debugging
description: Use after a bug, test failure, build failure, or unexpected behavior is observed, before proposing a fix; traces evidence to a falsifiable root cause without expanding task scope.
---

# Systematic Debugging

## Purpose and Authority

Debug by moving from evidence to pattern to one falsifiable hypothesis to one causal fix. Do not propose or implement a fix before locating the earliest supported cause.

This skill is a diagnostic method, not scope, permission, architecture, or evidence authority. First apply user and system instructions, repository and role contracts, the immutable task or complete finding batch, and accepted code or product contracts. Then record:

- the exact candidate, allowed paths, protected state, and current failure;
- accepted behavior, non-goals, and contract-excluded states;
- stable reproduction or the specific unknown preventing it;
- pre-existing failures and warnings;
- the evidence layer and observable success signal.

If the complete fix needs a new owner, schema, core interface, cross-module responsibility, architecture decision, or unapproved path, stop and return the evidence to the governing workflow.

## Phase 1 — Evidence and Root Cause

Read the complete relevant error, stack, exit code, warning, and artifact identity. Reproduce the symptom with exact inputs and environment when possible. Check the candidate delta and relevant recent change rather than assuming temporal correlation is causation.

Choose the smallest oracle that reproduces the real failure:

- an existing test and runner filter;
- a repeatable command and its exit or output;
- an artifact diff or schema/render/build check;
- a boundary trace of input, output, and state;
- an integration, emulator, physical-device, or human step when that is the actual boundary.

The absence of a test framework does not require a one-off script. If reproduction is unstable, state the unknown and gather evidence that distinguishes hypotheses; do not guess-fix.

For a deep symptom or suspected test pollution, read [root-cause-tracing.md](root-cause-tracing.md).

### Boundary Instrumentation

Instrument only boundaries relevant to competing explanations. Capture the minimum safe values needed to show where correct state becomes incorrect. Never log secrets, credentials, personal data, or sensitive health data. Prefer existing logging and runner facilities; do not create a one-use helper, wrapper, script, manager, or monitoring owner for a single investigation.

Mark temporary probes as task-owned and remove them after the hypothesis is resolved unless the governing task explicitly adopts them as durable telemetry. The probe's output is evidence, not a production fix.

## Phase 2 — Pattern

Find a working comparator governed by the same contract. Read the relevant implementation and configuration completely enough to understand its lifecycle. List every observed difference between working and failing cases, including inputs, owner, timing, state, environment, and evidence layer. Do not dismiss a difference until evidence makes it irrelevant.

The output of this phase is a ranked set of factual differences, not a list of proposed fixes.

## Phase 3 — One Hypothesis

State one hypothesis in falsifiable form:

```text
Cause: X is the earliest wrong state.
Because: evidence Y shows the preceding boundary is correct and this boundary is not.
Probe: change or observe one variable Z.
Expected result: observation Q confirms it; observation R rejects it.
```

Run the smallest safe probe and read the result. If rejected, remove or revert only the task-owned probe, update the evidence ledger, and form a new hypothesis. Do not stack multiple speculative changes.

## Phase 4 — Causal Fix and Regression

After a root cause is supported, establish the correct failing regression or other real oracle. For an automated behavior, use strict RED → GREEN → REFACTOR. For a document, artifact, external service, or physical behavior, use the contract's actual oracle and disclose the evidence layer.

Implement one minimum causally complete fix at the earliest controllable source. Do not add unrelated refactors, blanket validation, or any retry, fallback, silent default, broad catch, or future monitoring that the accepted contract does not require. Run the focused oracle and the directly affected regression set.

Candidate-introduced regressions must be repaired within scope. Prove, preserve, and report pre-existing or unrelated failures; do not fix them or claim the unrun larger suite passed.

## External and Environment Failures

For a network, SDK, environment, or device failure, preserve the original signal and identify the failing real boundary. Add error mapping, retry, timeout, fallback, or monitoring only when the accepted contract explicitly requires that behavior at that boundary. Otherwise report the external blocker and recovery condition. An injected fake, source inspection, or emulator result cannot replace required production, physical-device, or human evidence.

## Repair Findings

When debugging an approved repair batch:

1. read the complete batch and restate each technical claim;
2. reproduce or inspect the exact candidate evidence for every claim;
3. identify shared causes before editing;
4. if valid, address the complete atomic batch within its authority;
5. if invalid or unprovable, report the contrary or missing evidence without changing correct code.

Do not blindly implement reviewer wording, fix only the first item, or treat a finding as permission to widen scope.

## Stop Rules and Failure Signals

After each failed local fix attempt, return to Phase 1 with the new evidence. After three evidence-backed local fixes fail, do not attempt a fourth patch. Stop, summarize the attempts and observations, and escalate the architecture, owner, test seam, or problem definition to the governing workflow or Correct Course.

Immediate failure signals are: a proposed fix without root-cause evidence, multiple variables changed in one probe, an unstable oracle represented as fact, temporary instrumentation left behind, a swallowed external error, evidence-layer substitution, or scope expansion.

For timing and flakiness, read [condition-based-waiting.md](condition-based-waiting.md).
