---
name: test-driven-development
description: Use for approved behavior changes and bug fixes that can be exercised before implementation; use the governing contract's real artifact or device oracle when a production unit test cannot prove the change.
---

# Test-Driven Development

## 批准范围内自行修错与人工决定
- 批准范围内的代码、脚本、参数、导入、接线和验证载体实现错误，由当前agent自行定位和修正，不请求重新批准，不转交用户修改。使实现恢复到合同规定的行为，不属于新增业务决定。
- 真实冲突：两条有效要求无法同时满足，例如一条要求“有引用的项目必须保留”，另一条要求“删除时连同引用一起删除”。先定点核对来源和优先级；确实无法依据已有决定解决，才交用户裁定。
- 业务未决选择：合同缺少一个会影响用户实际结果的规则，必须在不同业务行为之间选择，例如尚未规定“停用项目是否允许新增关联”。已有决定直接沿用；符合相同合同的技术写法由agent自行选择，语法、导入、参数和接线错误不属于业务未决选择。例子只解释标准，不新增当前任务的规则或测试。
- 越界、超出合同或缺少必要操作授权：下一动作实际超出已批准路径、业务规则、验证内容、运行次数、环境或角色权限，才说明准确增量并请用户决定。不能仅因发生错误、技术方法尚未查清或修复改变了错误结果，就声称缺少授权。
- 停止失败的执行、保留现场和暂不声称通过，不等于冻结批准范围内的定位与修正。仅暂停依赖未决决定或缺少授权的动作；其他已授权工作继续。恢复、再次执行等动作按原批准内容和次数判断，不自动增加次数、不重跑未受影响通过项。
- 将载体修正为正确执行原定输入、步骤、断言和编排行为，不算扩大验证；新增或改变这些已批准要求才属于范围变更。不得借修错放宽断言、改输入、增加场景或反复改变编排来绕过失败。本规则不授予Planner实施、Reviewer修改候选或其他角色之外的权限。
- 同一个问题累计经过三次不同的、确实需要询问用户并由用户决定的授权后，仍未解决或再次出现时，停止继续提出或实施第四项处理，先与用户复盘并讨论纠错方向。这三次可以是三次不同的越界请求、合同外追加内容或三种不同的处理方案，不限定为三轮代码修复。按实际记录说明每项授权要解决什么、决定及边界是什么、实际做了什么、为什么仍错、现有证据和未解决事项；不得未经核对就宣称全部实现无效，不自行升级架构或扩大验证。讨论确定纠错方向及必要授权后继续，未受影响的有效成果和其他已授权工作保留。
- 上述“三次”按针对同一问题的三项不同且必要的用户授权决定计算。同一授权的重复确认不重复计数；批准范围内的代码、脚本、参数、导入、接线或验证载体错误，本来就应由agent自行定位和修正，既不请求授权，也不计入这三次。不得把本应自行解决的错误包装成越界或业务选择来索取授权。用户授权要求完成准确批准的工作，不代表验收错误结果；不得反复索取新的授权代替复盘。
来源：2026-10-11本PubMan主管理对话用户明确决定及后续澄清。本定义及同一问题三项不同必要授权后的复盘门禁优先于旧的无条件暂停、遇到疑问即审批或按失败次数自动升级措辞。


## Story实施、审查与修复边界

1. **首次Writer完整实现批准的Story**，把范围内的行为和接线做完整。
2. **首次Reviewer独立完整审查；后续Reviewer只检查当前批准的修复及其明确影响。** 原来通过的审查结论和测试结果按原范围保留，不自动失效、不重复审查或重跑。
3. **Repair Writer严格按批准内容修改。** 指定的问题、路径和验证范围就是边界；需要改别处或增加验证，先说明并取得批准，不能借“完整原因链”自行越界。

首次完整检查应覆盖批准范围内的实际行为和必要直接接线；后续Review只完成当前批准修复及明确影响的审查，继承原范围内已通过的结论和测试。完整提示词、完整报告和fresh Reviewer均不表示重审整个Story或重跑旧测试。

## Purpose and Authority

Use strict RED → GREEN → REFACTOR to prove an approved behavior change. This skill supplies a method; it does not grant scope, permissions, architecture changes, or a new evidence standard.

Before changing a test or implementation, apply the governing authority in this order:

1. user, system, and developer instructions;
2. the repository instructions and complete role contract;
3. the immutable task, finding batch, and evidence contract;
4. accepted product, data, and code contracts;
5. this method.

Record the exact candidate and allowed paths, accepted behavior and non-goals, contract-excluded states, observable success and failure, pre-existing failures or warnings, required evidence layer, and protected or unrelated state. Stop if any of these cannot be determined without a new scope, ownership, architecture, or evidence decision.

## Lifecycle and transitions

Use this state sequence and do not skip a transition:

```text
BOUND -> RED_PROVEN -> GREEN -> REFACTORED -> VERIFIED -> RETURN
```

- `BOUND`: the approved behavior, current candidate, scope, non-goals, oracle, evidence budget, and protected state are fixed.
- `RED_PROVEN`: the chosen oracle fails for the intended missing behavior. A broken fixture, environment failure, or unrelated baseline is not this state.
- `GREEN`: the minimum causal implementation passes the same oracle without weakening it.
- `REFACTORED`: only affected structure was improved and the oracle stayed green. This state may be skipped when no cleanup is needed.
- `VERIFIED`: the directly affected regression set and every contract-required real-boundary gate have fresh evidence. Use `../verification-before-completion/SKILL.md` for this transition.
- `RETURN`: report the actual terminal to the governing role; do not dispatch a Reviewer, merge, or start another Story.

An unexpected or unexplained failure transitions to `../systematic-debugging/SKILL.md`; an expected meaningful RED does not. A structure-only or external-boundary change uses the accepted real oracle and records why an automated RED is inapplicable.

Read-only investigation may inspect adjacent code, but necessity is not write authority. An additional write path is allowed only when the filled role contract already authorizes causal expansion or an exact amendment is accepted before editing. If the change introduces a new product, architecture, ownership, schema, migration, dependency, public-interface, security, or evidence decision, return `SCOPE_EXPANSION_REQUIRED` without a partial workaround.

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

Implement the smallest complete change for the approved behavior, including necessary direct callers, data handling, and UI wiring. Do not leave half a feature to reduce file count. Do not add future options, unrelated cleanup, or speculative abstractions. Validation stays within the approved set; explain an actual failure before proceeding, and do not obtain GREEN through repeated unchanged runs, changed inputs, weaker assertions, or longer waits.

Rely on explicit user decisions, types, database constraints, and current framework guarantees; do not present guesses as facts or invent blockers from unknowns. Validate at the place responsible for accepting data or enforcing a rule, including required business-state checks. Do not repeat a check downstream while its guarantee remains valid. Reuse accepted error handling, preserve the original cause, and perform required cleanup; catching an error is not itself swallowing it. Never turn failure into success, an empty result, or a default, and do not invent failure screens from an error channel.

Run the focused test again. If it still fails, change the implementation or correct a proven oracle defect—never weaken the accepted assertion merely to obtain GREEN.

### REFACTOR — Improve Only the Affected Structure

After GREEN, improve names or remove duplication only within the affected structure. Do not add behavior. Re-run the focused test after each meaningful refactor.

Then run the directly affected regression set: consumers, state transitions, persistence or boundary contracts that the change can actually influence. Establish that influence from the accepted contract, owner/consumer paths, state or persisted relationships, public boundaries, or an observed failure—not from diff size or imagined possibility. A repository-wide suite is required only when it is an accepted gate, propagation is demonstrated as repository-wide, or narrower affected evidence proves unable to cover the claim.

## Existing Work and Code-First Recovery

Code that existed before the current attempt—accepted base content, user dirty work, another candidate, or protected state—must remain intact. Process only the approved behavior or complete finding batch. A function or module may express a current responsibility even with one caller. Prefer existing capabilities; add structure only for a concrete present need, not hypothetical reuse. This applies to both existing-code changes and new projects and does not expand approved paths or test facilities.

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

Define the claim-to-evidence set before running validation. Focused and directly affected checks are the default. Size the set by demonstrated semantic risk propagation, never by character count, file count, or apparent simplicity. Do not add a repository-wide suite, device run, performance series, artifact rebuild, or repeated passing gate merely for confidence or appearance; run it only when the accepted contract or demonstrated propagation reaches that boundary. Stop the cycle when every accepted claim has one fresh risk-matched proof and all required gates are satisfied.
