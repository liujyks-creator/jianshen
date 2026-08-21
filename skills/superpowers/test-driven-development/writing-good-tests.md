# Writing Good Tests

## When to Apply

Use this reference when writing or changing a test, mock, fixture, or test helper. The governing task and accepted contract decide which behaviors are in scope. This reference does not authorize additional production behavior, validation, abstractions, or test coverage.

## Start With the Observable Contract

Before writing the body, state:

- the consumer-visible outcome or real invariant;
- the realistic production mutation that must make the test fail;
- the accepted input and output boundary;
- the independently derived expected result.

If the only possible failure is that a private call, symbol, constant, exact source line, or mock changed, redesign the test around an observable outcome. Test framework behavior only when the project owns an explicit boundary contract with that framework.

Expectations must not reuse the implementation or its helpers. Prefer a hand-checked literal or a fixture whose expected result was derived independently. A test that passes after the named production mutation is not sensitive to the behavior and must be corrected or removed.

## Use Realistic Fixtures

A fixture must represent the accepted shape and lifecycle at the boundary being exercised. Include required documented fields and meaningful relationships so that the wrong branch cannot pass accidentally. Do not create impossible internal states merely to increase coverage, and do not omit a required external field just because the current implementation does not read it.

Keep setup proportional to the behavior. If a fixture builder hides the expected value, mirrors production logic, or creates more policy than the contract, replace it with a clearer hand-checked fixture.

## Mock Only the Uncontrollable Boundary

Use real components when they are deterministic and reasonably fast. Mock or fake the narrowest network, device, clock, process, or external service boundary that cannot be controlled directly.

Before mocking a method, identify its real side effects. Keep every side effect on which the behavior depends; otherwise the double changes the contract being tested. Make responses and failures specific enough that the wrong call, order, or branch cannot satisfy the test.

Assert the component's observable result, not that the mock exists. Call counts and arguments are valid assertions only when they are themselves part of the accepted boundary contract. A fake, injected seam, or emulator proves only that layer and cannot substitute for production or physical-device evidence.

## Cover Behavior, Not Every Function

Trivial forwarding, plain data holders, generated code, and accessors with no validation, derivation, side effect, or state transition do not receive one test per function. Exercise the first consumer-visible result that depends on them.

When a function owns a branch, normalization, state transition, side effect, or error contract, test that observable behavior. Test helpers belong in test code. Do not add a production method, wrapper, or manager solely to make one test convenient.

## Contract-Bound Edges and Errors

Build the edge and error matrix from exactly one of these sources:

- an accepted behavior or invariant;
- a real user, persistence, network, external API, or device boundary;
- a reproduced failure included in the task.

For each case, name the contract and expected observable result. Do not turn generic lists such as zero, empty, null, default, malformed, or unauthorized into blanket requirements. If the accepted contract excludes a state, do not add a test, guard, fallback, retry, or silent default for it.

Preserve original error signals at real boundaries unless the accepted contract explicitly maps them to a different user-visible result.

## Mutation-Sensitivity Check

For each test, mentally or experimentally apply the realistic mutation it claims to catch:

- choose the wrong accepted branch;
- omit the required state change or side effect;
- produce the wrong contract value;
- mishandle a boundary failure that the contract includes.

At least one assertion must fail for that mutation. Do not invent excluded states to make this checklist longer. If a test cannot detect a meaningful regression, remove or redesign it.

## Scope, Stop, and Completion Signals

Keep the test change within the approved behavior and its directly affected consumers. Prefer existing runners, fixtures, clocks, and helpers. A new shared helper is justified only by multiple real consumers with the same semantics, not by a single call site.

Stop and return to the governing task when testing would require a new production owner, core interface, schema, cross-module responsibility, or evidence layer. A blocked physical, human, or external oracle remains a reported gate; it is not permission to replace that evidence with a mock.

A good test has a meaningful observed RED, passes on the current candidate, fails for its named realistic mutation, and asserts an observable accepted contract without widening scope.
