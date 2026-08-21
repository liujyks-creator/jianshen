---
name: test-driven-development
description: Use for approved behavior changes and bug fixes that can be exercised before implementation; use the governing contract's real artifact or device oracle when a production unit test cannot prove the change.
---

# Test-Driven Development

## Purpose and Authority

Use strict RED → GREEN → REFACTOR to prove an approved behavior change. This skill supplies a method; it does not grant scope, permissions, architecture changes, or a new evidence standard.

Before changing a test or implementation, apply the governing authority in this order:

1. user, system, and developer instructions;
2. the repository instructions and complete role contract;
3. the immutable task, finding batch, and evidence contract;
4. accepted product, data, and code contracts;
5. this method.

Record the exact candidate and allowed paths, accepted behavior and non-goals, contract-excluded states, observable success and failure, pre-existing failures or warnings, required evidence layer, and protected or unrelated state. Stop if any of these cannot be determined without a new scope, ownership, architecture, or evidence decision.

## When the Cycle Applies

Use strict TDD by default for behavior changes and bug fixes whose result can be exercised by an automated oracle. Refactoring belongs in the REFACTOR phase after behavior is protected; do not fabricate a new failing behavior for a structure-only change.

Pure documentation, metadata, non-executable artifacts, and behavior provable only at an external or physical boundary do not earn a fabricated production unit test. Use the real oracle required by the governing contract—such as schema validation, compilation, rendering, artifact comparison, an integration run, a physical-device step, or a human gate—and disclose the TDD exception.

When accepted implementation already exists, preserve it. First establish a regression or characterization oracle for the approved new behavior, then make the smallest causal change. Do not delete accepted, user-authored, protected, or pre-existing code to recreate a clean-slate TDD sequence.

## The Strict Cycle

### RED — Prove the Missing Behavior

Write one focused test for one consumer-observable behavior or real invariant. Before implementation:

1. state the production mutation or missing behavior the test should detect;
2. use an expectation derived independently from the code under test;
3. run the focused test and read its relevant output and exit status;
4. confirm it fails because the approved behavior is missing.

A syntax error, broken fixture, unavailable environment, or unrelated baseline failure is not RED. If the test passes unexpectedly, investigate existing behavior or the oracle; do not proceed to GREEN until the failure is meaningful.

### GREEN — Implement the Minimum Causal Behavior

Write only the smallest causally complete implementation that makes the current RED pass. Do not add future options, extra validation, silent defaults, retries, fallbacks, unrelated cleanup, or speculative abstractions unless the current accepted behavior and RED require them.

Trust accepted internal invariants. Add validation only at a real input, persistence, network, external API, or device boundary when the accepted contract requires it. Preserve the original failure signal; do not hide it with a broad catch or default.

Run the focused test again. If it still fails, change the implementation or correct a proven oracle defect—never weaken the accepted assertion merely to obtain GREEN.

### REFACTOR — Improve Only the Affected Structure

After GREEN, improve names or remove duplication only within the affected structure. Do not add behavior. Re-run the focused test after each meaningful refactor.

Then run the directly affected regression set: consumers, state transitions, persistence or boundary contracts that the change can actually influence. A repository-wide suite is required only when the task contract, an existing gate, or demonstrated risk propagation calls for it.

## Existing Work and Code-First Recovery

Code that existed before the current attempt—accepted base content, user dirty work, another candidate, or protected state—must remain intact. Process only the approved behavior or complete finding batch. Do not create a one-use helper, wrapper, script, or manager when an existing framework feature or a scoped inline change is sufficient.

If the current agent wrote implementation before RED, removal is allowed only when every removed line is precisely attributable to the current agent's current undelivered attempt, has not been delivered or adopted by accepted work, is not protected, and has no accepted consumer or dependency. Preserve all other content and remove the attributable change with a scoped edit, never a destructive Git reset or checkout. Then establish RED. Local commit or push status alone does not define delivery or adoption.

Exploration may be discarded only when it was identified in advance as temporary, was created by the current task, has not been delivered or adopted by accepted work, and has no accepted consumer or dependency. Its removal remains subject to the scoped-edit and protected-state limits above regardless of local commit status.

## Test Selection and Evidence Layers

Test observable contracts rather than private calls, mock existence, framework guarantees, or method presence. Trivial forwarding, data holders, and generated code need no test of their own; cover the first consumer-visible outcome. A new function does require coverage when it owns an observable branch, state transition, side effect, or error contract.

Derive edge and error cases only from the accepted contract, a real boundary, or a demonstrated failure. If the contract excludes null, default, malformed, or other states, do not add tests, guards, or fallback behavior for them.

Mocks and fakes may isolate an uncontrollable external boundary, but prove only the injected layer. Unit tests, source inspection, fakes, and emulators do not prove production wiring, a physical device, or human experience. Preserve every identity-bound external or human gate.

For detailed test construction, read [writing-good-tests.md](writing-good-tests.md) when writing or changing tests, mocks, fixtures, or test helpers.

## Baseline and Completion

Candidate-introduced regressions must be fixed inside the approved scope. For a pre-existing or unrelated failure or warning:

1. reproduce or otherwise identify the baseline;
2. compare it with the candidate result;
3. preserve evidence and report it without changing unrelated code;
4. avoid any claim that the unrun or failing larger set passed.

Before claiming the TDD cycle complete, the meaningful RED must have been observed, the same oracle must be GREEN on the current candidate, the directly affected regression set must be fresh, and every required real-boundary gate must be accurately reported. Stop rather than expanding scope when the complete fix would require a new owner, schema, core interface, cross-module responsibility, or architecture decision.
