# Root-Cause Tracing

## When to Apply

Use backward tracing when an error, bad value, wrong state, or side effect appears downstream from its source, or when one test contaminates another. The governing task defines scope and accepted contracts. This method does not authorize downstream guards, new validation layers, or unrelated cleanup.

## Trace Backward to the Earliest Wrong State

Start with a concrete symptom and its identity: value, state, side effect, timestamp or sequence, caller, and failing oracle.

Repeat these questions one boundary at a time:

1. Who produced or supplied this value or state?
2. When was it first observably wrong?
3. What input and invariant reached this boundary?
4. Which preceding boundary was last known correct?
5. What evidence distinguishes the producer from its caller or consumer?

Use existing stack traces, structured logs, debugger facilities, runner output, artifact metadata, or a minimal temporary probe. Capture only the safe fields necessary to locate the transition; never expose secrets, personal data, or sensitive health data. Remove task-owned probes after use unless the task explicitly adopts them.

Stop tracing when the earliest controllable source is supported by evidence. If the chain ends at an external or inaccessible boundary, report the last correct boundary, first bad observation, missing evidence, and recovery condition instead of inventing a cause.

## Fix at the Source

Correct the earliest controllable source that violates an accepted contract, then rerun the original symptom oracle and directly affected regression set. Do not add a guard at each layer merely because the bad value passed through it. Trust accepted internal invariants; validate at a real user, persistence, network, external API, or device boundary only when its contract requires validation there.

A downstream guard is justified only if that downstream component independently owns an accepted boundary contract. Otherwise it hides the source and creates a second behavior owner.

## Isolating Test Pollution With Existing Tools

Use this when a fixed victim test passes alone but fails after other tests or leaves unexpected state.

1. Confirm the victim passes in isolation and that the pollution is absent before the run.
2. Reproduce a fixed failing order or preceding set with the existing test runner and its filters.
3. Split the preceding set approximately in half while keeping the victim and environment fixed.
4. Run one half before the victim and observe the same pollution oracle.
5. Keep the half that reproduces the pollution; if neither does, test ordering, shared environment, or interaction between subsets is part of the hypothesis.
6. Repeat until the smallest reproducing polluter or interacting set is identified.
7. Trace the polluter's state or side effect back to the missing ownership or cleanup contract.

Prefer existing runner selection, ordering, sharding, and filter features. Do not create a single-use script, wrapper, or manager for bisection. If the runner cannot express the stable order, report that evidence limitation rather than substituting a different oracle.

## Completion and Failure Signals

Tracing is complete only when evidence identifies the last correct boundary, the first wrong boundary, and the earliest controllable cause, and the causal fix makes the original oracle pass without masking its signal.

Stop and report if reaching the cause requires an unapproved owner, schema, core interface, architecture change, or external access. Failure signals include fixing only the symptom, adding blanket defense-in-depth, changing several boundaries at once, relying on an arbitrary test order, or claiming a source without an observable transition.
