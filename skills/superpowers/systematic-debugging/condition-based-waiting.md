# Condition-Based Waiting

## When to Apply

Use this method when correctness depends on observing an asynchronous condition and a fixed sleep is only guessing when that condition will become true. The governing contract defines the condition and evidence layer; this method does not authorize a new scheduler, helper, callback owner, or abstraction.

Do not replace a duration when elapsed time itself is the accepted behavior, such as a debounce interval or countdown threshold. In that case, control or measure the contract time explicitly.

## Choose the Best Observation Mechanism

Prefer, in order of fit to the system:

1. an existing event, callback, completion signal, or framework wait primitive;
2. an existing virtual clock or scheduler control for time-based logic;
3. an existing shared helper with the same condition semantics;
4. bounded polling when no direct signal exists.

Do not poll merely because it is familiar. A direct event proves the transition without repeatedly sampling; a virtual clock avoids wall-clock delay when time is the modeled input.

## Define the Waiting Contract

Before implementation, write down:

- **condition:** the fresh observable predicate or event that means success;
- **timeout:** the accepted maximum or a justified diagnostic bound;
- **mechanism:** event, callback, virtual clock, existing helper, or polling;
- **diagnostic:** the state, counts, last observation, and elapsed time reported on timeout;
- **cleanup:** listener, callback, timer, or task state released on every exit path.

For polling, evaluate fresh state on each iteration, use a bounded timeout, and choose an interval proportional to the operation rather than a universal constant. Timeout must fail with a diagnostic signal; it must not silently continue or return a default.

## Keep the Implementation Proportional

For one call site, keep the minimum condition wait local. Reuse an existing helper when its semantics match. Extract a new shared helper only when multiple real consumers share the same condition, timeout, diagnostic, and lifecycle contract.

Do not create a one-use helper, wrapper, manager, script, or domain-specific example. Do not replace an arbitrary sleep with a more elaborate abstraction that the accepted design does not own.

## When a Fixed Duration Is Valid

A fixed duration is valid only when the duration itself is under test or mandated by an external contract. First synchronize on the triggering condition, then advance a virtual clock or measure the documented interval. Record why the duration is required and what tolerance the contract permits. A guessed buffer is not evidence.

## Completion and Failure Signals

The wait is complete when the real condition is observed, cleanup runs, and the consuming assertion verifies the resulting behavior. On timeout, surface the diagnostic state and preserve the failure.

Stop if the needed event, clock, or ownership change is outside the approved task. Failure signals include an arbitrary sleep, an unbounded loop, stale cached state, polling without diagnostics, an emulator or fake substituted for a required physical boundary, or a new abstraction serving only one call site.
