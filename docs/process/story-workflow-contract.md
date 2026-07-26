# Story Workflow Contract

This is the canonical generic delivery workflow for an approved software
Story. Product and architecture decisions remain in their accepted sources;
this contract governs delivery, continuity, Review, Repair, and integration.

Candidate changes to this file cannot govern their own Review. Writer and
Reviewer packets bind the accepted copy from the pinned base.

## Template-Bound Entry

The accepted root templates are mandatory role skeletons:

| Role | Template |
|---|---|
| Manager | `MAIN_CONTROL_RESTART_PROMPT_TEMPLATE.md` |
| Writer or Repair Writer | `DEV_STORY_PROMPT_TEMPLATE.md` |
| Fresh Reviewer | `CODE_REVIEW_PROMPT_TEMPLATE.md` |

Before any repository or global-skill read, command, dispatch, or mutation,
the role packet must be rendered from the complete accepted template and
contain:

```text
TEMPLATE_BOUND; template=<root template>; accepted template commit=<full SHA>; accepted template blob=<full blob SHA>; role=<role>; base=<full SHA>; candidate=<full SHA|unborn>
```

The role echoes that exact line to the manager before acting. The manager
verifies the named template blob at the accepted commit. A missing field,
mismatch, unverified echo, abbreviated SHA, or free-hand packet is a
fail-closed `BLOCKED` result and cannot acquire an execution lease.

Template binding is version identity, not authority. Every packet must still
fill all template fields, including immutable requirements, scope,
permissions, evidence, role budget/tool timeouts, and terminal schema.

## Durable Ignored Ledger And Bootstrap

Before dispatch, the manager atomically creates an untracked local ledger
outside the repository, for example
`$CODEX_HOME/workflows/<repository-id>/<workflow-id>.json`. A repository-local
location may be used only when an existing ignore rule is verified before the
ledger is written. The ledger is never staged or committed. It is the
continuity source across compaction, replacement, recovery, and a new
conversation, and is updated after each binding, lease change, transition,
report, qualifying wait window, finding change, attestation, health change,
human gate, and Process Flow event.

The minimum schema is:

```json
{
  "schema_version": 2,
  "workflow_id": "stable identifier",
  "phase": "PREFLIGHT",
  "binding": {
    "mode": "TEMPLATE_BOUND",
    "template": "DEV_STORY_PROMPT_TEMPLATE.md",
    "template_commit": "40 hex",
    "template_blob": "40 hex",
    "role": "WRITER",
    "base": "40 hex",
    "candidate": "unborn or 40 hex",
    "echoed": true,
    "verified": true
  },
  "base_sha": "40 hex",
  "candidate_sha": null,
  "role": "PREFLIGHT",
  "attempt": 1,
  "findings": {},
  "audited_scope": [],
  "unaudited_scope": ["all"],
  "validation_attestations": [],
  "liveness": {
    "role_lease": {
      "status": "ACTIVE",
      "acquired_at": "RFC 3339",
      "expires_at": "RFC 3339",
      "basis": "accepted role budget/tool timeout",
      "renewal_used": false
    },
    "operation_lease": null,
    "external_wait": null,
    "qualifying_window_count": 0,
    "last_report_at": "RFC 3339"
  },
  "process_flow": {
    "events": [],
    "issues": {}
  },
  "health": "HEALTHY",
  "human_gate": null,
  "timestamps": {
    "created_at": "RFC 3339",
    "updated_at": "RFC 3339"
  }
}
```

Finding entries include stable ID, severity, violated contract, evidence,
causal repair direction, verification state, status, closed candidate SHA,
and remaining reason. Attestations include candidate SHA, exact command,
environment/toolchain, artifact identity, result, timestamps, and evidence
location.

Bootstrap and resume fail closed:

1. locate and schema-validate the ledger with `workflow_guard.py`;
2. render its binding, immutable SHAs, phase, role/attempt, findings,
   audited/unaudited scope, attestations, lease/window state, health, human
   gate, Process Flow events, and issue follow-up state;
3. verify immutable facts against Git and accepted sources;
4. compare persisted phase with Git, disk, and evidence so completed work is
   not replayed;
5. continue only the recorded next legal transition.

Missing, unreadable, inconsistent, or incomplete state is `BLOCKED`. Recovery
resumes the approved atomic Story from the ledger, Git, disk, and persisted
evidence. It never reconstructs the Story from conversational memory,
restarts completed work, or decomposes the Story to solve a timing problem.

## Finite-State Machine

```text
BOOTSTRAP
  -> PREFLIGHT
  -> WRITER
  -> VALIDATOR
  -> REVIEW
       -> PASS + authority -> INTEGRATION -> POST_MERGE_VERIFY -> COMPLETE
       -> PASS - authority -> READY_TO_MERGE
       -> complete CHANGES_REQUESTED -> REPAIR -> VALIDATOR -> NEW REVIEW
       -> REVIEW_BLOCKED | NEEDS_USER | BUDGET_EXHAUSTED (terminal gate)
```

Only one mutating Writer exists at a time. Close it before replacement.
Validator and Reviewer are distinct read-only roles. Each re-Review is fresh.
An integration conflict or content change produces a new candidate and
requires new validation and Review. The manager advances every legal routine
passing transition without asking whether to continue.

### Preflight

Reconstruct accepted base, prerequisites, requirements, acceptance-to-
validation matrix, exact scope, permissions, protected state, baseline
validation, and human gates. Use immutable SHAs; branch names are locators.
Ambiguous authority, ownership, prerequisite, environment, or unlisted
baseline failure blocks writing.

### Writer

Use the accepted Writer template and return its terminal proof object.
`DONE` means a committed immutable candidate is ready for Validator; it does
not mean accepted. `NEEDS_USER`, `BLOCKED`, and `BUDGET_EXHAUSTED` are
terminal for that attempt.

### Validator

Validator owns mechanical attestation: SHA and ancestry, three-dot scope,
index/protected state, exact commands, environment/toolchain, artifact
identity, and matrix coverage. Reviewer does not repeat mechanical commands
unless a risk-critical independent check requires it or disputes the
attestation.

Attestation reuse identity is exactly:

```text
candidate SHA + command + environment/toolchain + artifact identity
```

Reuse only a successful attestation whose four dimensions are byte-for-byte
identical and whose evidence still exists. Invalidation occurs when any
dimension changes, a command runs under a different environment, an artifact
is rebuilt or replaced, an input affecting the artifact changes, evidence is
missing or corrupt, or a risk-critical dispute identifies a concrete reason
the attestation is unreliable.

### Review

A finding blocks integration only; it does not stop remaining read-only
Review. The Reviewer completes every acceptance criterion and named risk axis
still feasible, records exact `audited_scope` and `unaudited_scope`, and
continues discovering independent findings.

Reviewer terminal schema includes:

```text
status: PASS | CHANGES_REQUESTED | REVIEW_BLOCKED | NEEDS_USER | BUDGET_EXHAUSTED
SPEC: PASS | FAIL
QUALITY: PASS | FAIL
EVIDENCE: PASS | FAIL | BLOCKED
full_finding_set: true | false
acceptance_coverage_complete: true | false
risk_coverage_complete: true | false
findings: complete stable-ID set
audited_scope: exact list
unaudited_scope: exact list plus reason
validation_attestations_reused: exact identities
risk_critical_checks: commands/inspections and results
Process Flow Report: mandatory terminal section
```

`PASS` requires all three verdicts PASS, all coverage markers true,
`full_finding_set: true`, and no blocking findings. `CHANGES_REQUESTED`
requires the same complete coverage/full-set markers and the complete
verified open-finding set. An incomplete Review, `REVIEW_BLOCKED`,
`NEEDS_USER`, or `BUDGET_EXHAUSTED` cannot trigger Repair or integration.

### Repair

One Repair consumes the complete verified open-finding batch from one
complete Review. Intermediate one-finding Repair requests are rejected.
Minimum Repair means causally complete: every directly necessary
implementation, test, document, configuration, and evidence change inside
authority, not the fewest filenames.

Repair terminal adds:

```text
input_finding_ids: exact complete batch
closed_finding_ids: exact IDs
remaining_finding_ids: exact IDs and reasons
```

Closed and remaining IDs must partition the input batch. After Repair, run a
Validator and a different fresh Reviewer. Two unsuccessful Repair cycles or
a boundary-crossing finding returns to correct-course.

## Automatic Role Execution Lease And Liveness

After a role echoes a verified template binding, the manager automatically
acquires:

```text
ROLE_EXECUTION_LEASE role=<role> max=<accepted role budget/tool timeout>
```

The packet supplies the bounded basis; the user does not manage timers. While
the role lease is active, long reasoning, editing, Review, validation, build,
or device work is not treated as stalled, and two-minute heartbeat silence
does not trigger a nudge.

Before a specific command may exceed the remaining role lease, the role may
declare:

```text
LONG_OPERATION_LEASE operation=<exact command> max=<declared tool timeout>
```

This sublease extends only that command and cannot exceed its declared tool
timeout. Completion or expiry returns control to the remaining role lease or
the qualifying-window sequence.

The two-minute silence-window protocol starts only when:

1. binding or the automatic role lease was never acquired;
2. the role lease or valid operation sublease expires; or
3. the role explicitly enters an external unchanged-wait state.

| Qualifying two-minute silence windows | Required action |
|---:|---|
| 1 | send nudge 1 requesting phase/progress or terminal schema |
| 2 | send nudge 2 as the final request before recovery |
| 3 | interrupt, close, and recover from persisted immutable facts |

A progress or terminal report resets the qualifying count. An unchanged
external wait remains qualifying. Compaction, manager restart, and recovery
do not reset a persisted count. Confirm a mutating role is closed before
replacement. Recovery resumes the same atomic Story; it does not rebuild or
split it.

The approved packet may bind a 60/90 minute or longer role lease when the role
budget and tool timeouts justify it. A one-hour implementation or Review
remains one atomic role and reports at natural phase boundaries; it is not
forced into two-minute heartbeats or smaller tasks. At role-lease expiry, the
manager may renew it once only when the role supplies concrete immutable
progress evidence recorded in the ledger. Without that evidence, or after the
single renewal expires, the qualifying silence-window sequence begins.

Record `HEARTBEAT_NO_REPORT_WITHOUT_LEASE` only when an active role is silent
in a qualifying state without a valid lease. Its initial follow-up is
`PENDING_REAL_TASK`. On a later real task, the same signature becomes
`RECURRENCE_CONFIRMED`. Do not create a separate smoothness task.

## Mandatory Process Flow Report

Every terminal role and terminal workflow report includes a Process Flow
Report with phase path, validation path, resume/recovery facts, lease/window
facts, detected issue IDs/signatures, follow-up observation, recurrence
state, corrective action, and remaining observation gate.

A one-hour blocking build or device command may receive a command sublease
equal to its declared tool timeout. The sublease does not renew the whole
role or authorize unrelated work.

Detailed causal analysis is required only when at least one of these occurs:

- repeated waits;
- repeated validation without new candidate or evidence identity;
- phase regression;
- resume replay of completed work.

Pure long duration or timeout is factual only: record phase, elapsed interval,
active/expired lease, qualifying window count, and action. Do not create an
estimate, deadline, estimate/actual ratio, or revised-estimate model.

`FLOW-001` records the prior liveness/process-flow failure. Its correction is
automatic role execution leases, bounded command subleases, durable window
state, and resume from persisted facts. The manager closes the issue only
after verification; otherwise it reports the exact pending or recurrence
state and advances the project through every satisfied gate.

## Execution Health And Human Gates

`HEALTHY`, `SUSPECT`, and `DEGRADED` describe execution reliability, not
ordinary findings, long work under a valid lease, or liveness recovery. On
one anomaly, hold mutation/integration and perform exactly one immutable-fact
probe. A repeated or unresolved anomaly is `DEGRADED`: close mutators,
preserve refs/evidence, and stop for the user.

Stop only for accepted human gates, unauthorized scope/authority, missing
immutable evidence, degraded health, or terminal budget/correct-course
thresholds. Ordinary passing phases continue without asking permission.

## Guard

Use the standard-library helper shipped with the supervised delivery skill:

```text
python scripts/workflow_guard.py validate <ledger.json>
python scripts/workflow_guard.py next-action <ledger.json>
```

The guard validates template binding, schema and bootstrap, legal
transitions, complete Review before Repair, complete finding batches,
attestation reuse/invalidation identity, automatic leases, persistent
qualifying windows, Process Flow reporting, and issue recurrence. A guard
rejection is a workflow gate; do not edit the ledger to bypass it.