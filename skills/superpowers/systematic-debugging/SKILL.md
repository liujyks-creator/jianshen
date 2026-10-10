---
name: systematic-debugging
description: Use after a bug, test failure, build failure, or unexpected behavior is observed, before proposing a fix; traces evidence to a falsifiable root cause without expanding task scope.
---

# Systematic Debugging

## 批准范围内自行修错与人工决定
- 批准范围内的代码、脚本、参数、导入、接线和验证载体实现错误，由当前agent自行定位和修正，不请求重新批准，不转交用户修改。使实现恢复到合同规定的行为，不属于新增业务决定。
- 真实冲突：两条有效要求无法同时满足，例如一条要求“有引用的项目必须保留”，另一条要求“删除时连同引用一起删除”。先定点核对来源和优先级；确实无法依据已有决定解决，才交用户裁定。
- 业务未决选择：合同缺少一个会影响用户实际结果的规则，必须在不同业务行为之间选择，例如尚未规定“停用项目是否允许新增关联”。已有决定直接沿用；符合相同合同的技术写法由agent自行选择，语法、导入、参数和接线错误不属于业务未决选择。例子只解释标准，不新增当前任务的规则或测试。
- 越界、超出合同或缺少必要操作授权：下一动作实际超出已批准路径、业务规则、验证内容、运行次数、环境或角色权限，才说明准确增量并请用户决定。不能仅因发生错误、技术方法尚未查清或修复改变了错误结果，就声称缺少授权。
- 停止失败的执行、保留现场和暂不声称通过，不等于冻结批准范围内的定位与修正。仅暂停依赖未决决定或缺少授权的动作；其他已授权工作继续。恢复、再次执行等动作按原批准内容和次数判断，不自动增加次数、不重跑未受影响通过项。
- 将载体修正为正确执行原定输入、步骤、断言和编排行为，不算扩大验证；新增或改变这些已批准要求才属于范围变更。不得借修错放宽断言、改输入、增加场景或反复改变编排来绕过失败。本规则不授予Planner实施、Reviewer修改候选或其他角色之外的权限。
来源：2026-10-11本PubMan主管理对话用户明确决定。本定义优先于旧的无条件暂停、遇到疑问即审批或按失败次数自动升级措辞。


## Purpose and Authority

Debug by moving from evidence to pattern to one falsifiable hypothesis to one causal fix. Do not propose or implement a fix before locating the earliest supported cause.

This skill is a diagnostic method, not scope, permission, architecture, or evidence authority. First apply user and system instructions, repository and role contracts, the immutable task or complete finding batch, and accepted code or product contracts. Then record:

- the exact candidate, allowed paths, protected state, and current failure;
- accepted behavior, non-goals, and contract-excluded states;
- stable reproduction or the specific unknown preventing it;
- pre-existing failures and warnings;
- the evidence layer and observable success signal.

If the complete fix needs a new owner, schema, core interface, cross-module responsibility, architecture decision, or unapproved path, stop and return the evidence to the governing workflow.

## Lifecycle and transitions

```text
OBSERVED -> REPRODUCED -> ROOT_CAUSE_SUPPORTED -> ORACLE_ESTABLISHED
-> FIX_GREEN -> VERIFIED -> RETURN
```

- Remain in `OBSERVED` while reproduction is unstable; report the exact unknown instead of guessing.
- Enter `ROOT_CAUSE_SUPPORTED` only when evidence locates the earliest controllable wrong state and distinguishes it from competing explanations.
- `ORACLE_ESTABLISHED` is a meaningful failing regression or the accepted real-boundary oracle.
- `FIX_GREEN` contains one minimum causally complete change at the supported source.
- `VERIFIED` uses `../verification-before-completion/SKILL.md` with the directly affected risk set.
- `RETURN` reports evidence and the actual state to the governing role; it does not dispatch, merge, or widen the assignment.

Read-only tracing may cross the expected modification set. A write outside the authorized set requires an explicit causal-expansion permission or exact accepted amendment before editing. A new product, architecture, ownership, schema, migration, dependency, public-interface, security, or evidence decision transitions to `SCOPE_EXPANSION_REQUIRED`; do not route around it with a wrapper, adapter, fallback, duplicate authority, or partial symptom fix.

## Phase 1 — Evidence and Root Cause

Read the complete relevant error, stack, exit code, warning, and artifact identity. Reproduce the symptom with exact inputs and environment when possible. Check the candidate delta and relevant recent change rather than assuming temporal correlation is causation. Treat an explanation as a hypothesis until supported, while relying on explicit user decisions and applicable type, database, and framework guarantees. An unproven scenario is not a new requirement or an implementation blocker.

Choose the smallest oracle that reproduces the real failure:

- an existing test and runner filter;
- a repeatable command and its exit or output;
- an artifact diff or schema/render/build check;
- a boundary trace of input, output, and state;
- an integration, emulator, physical-device, or human step when that is the actual boundary.

The absence of a test framework does not require a one-off script. If reproduction is unstable, state the unknown and gather evidence that distinguishes hypotheses; do not guess-fix.

For a deep symptom or suspected test pollution, read [root-cause-tracing.md](root-cause-tracing.md).

### Boundary Instrumentation

Instrument only boundaries relevant to competing explanations. Capture the minimum safe values needed to show where correct state becomes incorrect. Never log secrets, credentials, personal data, or sensitive health data. Prefer existing logging and runner facilities. A function or module may isolate a concrete current responsibility even with one caller; do not invent reusable diagnostic infrastructure for hypothetical later use. New probes, scripts, or test facilities still require the task's exact authorization.

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

Implement the smallest complete fix at the earliest controllable source, including necessary direct consumers. Put checks where data is accepted or a rule is enforced; do not repeat a still-valid guarantee downstream. Reuse accepted error handling, preserve the cause, and perform required cleanup without disguising failure as success, an empty result, or a default. Do not add unrelated refactors, hypothetical reuse, failure screens, retries, or recovery. Run only approved validation; explain each actual failure and do not repeat unchanged runs, alter inputs, weaken assertions, or extend waits to obtain a pass.

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

After each failed local fix attempt, return to Phase 1 with the new evidence. A failed-attempt count alone does not require user approval or architecture escalation. Continue evidence-backed diagnosis and correction within the accepted contract, allowed paths, role permissions, and existing validation budget; do not repeat unchanged commands, invent additional runs, or stack speculative patches. Request a decision only when confirmed valid requirements conflict, a business rule is undecided, or the next action exceeds the contract or lacks authorization. Report actual unresolved limits honestly without treating an ordinary implementation error as a new business decision.

Immediate failure signals are: a proposed fix without root-cause evidence, multiple variables changed in one probe, an unstable oracle represented as fact, temporary instrumentation left behind, a swallowed external error, evidence-layer substitution, or scope expansion.

For timing and flakiness, read [condition-based-waiting.md](condition-based-waiting.md).

Define the minimum diagnostic and verification evidence before running it. One stable reproduction and one discriminating probe are enough when they support the cause. Expand diagnostics or regression coverage only when accepted dependency, ownership, state, persistence, boundary, or observed-failure evidence demonstrates wider propagation; change size and uncertainty alone do not. Do not repeat passing gates, collect unrelated logs, scan the repository for similar issues, or run broad suites merely for confidence. Stop when the root cause is supported, the causal fix is verified by the accepted oracle, and every required gate is satisfied.
