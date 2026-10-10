---
name: verification-before-completion
description: Use before claiming a candidate is complete, fixed, passing, ready, or safe to commit or push; maps the exact claim to fresh, risk-matched evidence and reports any missing gate honestly.
---

# Verification Before Completion

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

本技能的fresh证据要求仅用于本次新增或有明确影响的claim，不使原范围内仍有效的已通过结果自动失效。

## Purpose and Authority

Evidence must precede every completion, correctness, readiness, commit, or push claim. This skill defines a verification method; it does not grant scope, permission, integration authority, review authority, or permission to replace a human or device gate.

First apply user and system instructions, repository and role contracts, the immutable task or complete finding batch, and accepted product and code contracts. Fix the exact candidate identity, allowed scope, protected and unrelated state, accepted behavior and non-goals including excluded states, baseline failures or warnings, observable success and failure, and required evidence layers. Stop if a new authority or architecture decision is needed.

Verification does not create new write authority; corrections already authorized by the current role contract do not need renewed approval. Candidate regressions return to the approved implementation scope; unrelated baseline issues remain evidenced and reported. Rely on explicit user decisions and applicable type, database, contract, and framework guarantees; label unsupported claims as unknown rather than inventing blockers. Check the complete current responsibility, including direct callers, data handling, and UI wiring. Validation belongs where a guarantee is established or a rule is enforced, with no duplicate downstream check while that guarantee remains valid. A function with one caller may serve a real current responsibility; neither call count nor catch breadth alone proves overengineering. Verify that accepted error handling preserves the cause and required cleanup without disguising failure as success, an empty result, or a default. Do not create speculative infrastructure or change inputs, assertions, waits, or retry behavior merely to make an oracle pass; stay within approved validation and explain actual failures.

## Lifecycle and transitions

```text
CLAIM_BOUND -> EVIDENCE_SELECTED -> EVIDENCE_EXECUTED
-> PROVEN | NOT_PROVEN -> RETURN
```

- `CLAIM_BOUND`: identify the exact candidate, claim, direct risks, required gates, and protected state.
- `EVIDENCE_SELECTED`: map every direct risk to the smallest sufficient real oracle and define the stop condition before execution.
- `EVIDENCE_EXECUTED`: each selected oracle reaches terminal output against the bound candidate.
- `PROVEN`: every risk inside the claim has adequate fresh evidence and every mandatory gate passed.
- `NOT_PROVEN`: report the failing or missing oracle, evidence limit, and recovery condition without converting verification into implementation.
- `RETURN`: return the verified state to the current Writer, Reviewer, Repair, or management role. Do not dispatch or advance another role yourself.

## The Claim-to-Evidence Gate

Before a positive claim:

1. **Claim:** State exactly what is complete, fixed, passing, or ready, and identify the candidate being evaluated.
2. **Risks:** List every risk directly covered by that claim: changed behavior, direct consumers, state or persistence boundaries, artifact identity, scope, and any external, device, or human gate.
3. **Oracles:** Select a real oracle for each applicable risk.
4. **Fresh run:** Run only approved commands and evidence steps for current new or demonstrably affected claims. Preserve passed unaffected results in their original scope without repeating them.
5. **Full result:** Let each selected command finish; read its complete relevant output, exit code, failure and warning counts, and produced artifact identity.
6. **Honest state:** Make only the claim the evidence supports. Otherwise report the actual result, missing gate, and recovery condition.

Old output or another candidate's artifact cannot freshly prove newly changed behavior; source inspection alone or confidence cannot replace a required runtime oracle. Previously passed tests and review conclusions remain valid in their original unaffected scope even when the candidate changes. Retain their original identity and scope; do not claim that inherited results are runtime proof of the new change.

## Complete Command vs. Complete Risk Set

A **complete selected command** is one chosen oracle run from start to terminal exit without truncating it, stopping after a favorable line, or extrapolating from a subset.

The **claim-bound minimum complete risk set** is the collection of commands and evidence steps needed to cover every applicable risk in the exact claim. It may contain a focused test plus directly affected regressions, a build plus an artifact identity check, or a device or human gate in addition to automation.

These concepts are complementary. Neither means “run the entire repository by default.” Run a repository-wide suite only when the governing task, an accepted project gate, or demonstrated risk propagation requires it. Never call a focused set “all tests.”

## Size Evidence by Demonstrated Risk Propagation

Choose the amount of testing and verification from the semantic risk propagation, not from character count, diff size, file count, or whether the edit looks simple. Before execution, identify how the changed behavior can propagate through accepted ownership, direct consumers, public contracts, state, persistence, concurrency, packaging, and real external boundaries. Support that propagation map with the accepted contract, dependency or call paths, persisted relationships, or an observed failure; do not expand validation merely because an effect is imaginable.

- For behavior whose propagation is bounded to a local owner and known consumers, run the decisive focused oracle plus the directly affected regressions, then stop.
- For shared core behavior, a public API, schema or migration, persistence, concurrency, cross-module state, or another wide boundary, extend evidence to every demonstrated affected consumer and boundary. Use a broader affected suite only to the extent that the propagation map reaches it.
- Run a repository-wide suite only when it is an explicit accepted gate, demonstrated propagation is repository-wide, or focused and directly affected evidence reveals that the claim cannot be covered by a narrower accepted suite. “Fresh,” “small edit,” uncertainty by itself, and desire for confidence are not reasons.
- Run device, performance, external-service, or human validation only when the claim or changed behavior reaches that real boundary or the accepted contract explicitly requires the gate.
- A non-executable change may need only its relevant parser, schema, format, diff, or identity oracle; a one-character executable change may require broad regression evidence. The semantic effect decides, not the form of the edit.

Record why the selected set is sufficient and what evidence would require expanding it. If a selected check exposes a wider causal effect, update the propagation map and add only the newly required evidence. Do not repeat an unchanged passing gate.

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

Verify against the immutable candidate identity when the workflow provides one. Rebuild a corresponding artifact when the accepted claim includes that artifact, the contract requires the build gate, or demonstrated propagation reaches compilation, packaging, installation, or runtime construction. Do not rebuild every artifact merely because some executable source changed.

For any failure or warning, determine whether the candidate introduced it. A candidate regression within scope blocks the claim and must be fixed. A proven pre-existing or unrelated failure is preserved and disclosed; it does not authorize unrelated changes and prevents only claims that include the failing set.

A warning blocks completion only when the candidate introduced it, it makes a required command fail, or the accepted contract forbids it. Otherwise report it accurately without claiming pristine global output.

## Before Commit, Push, or Terminal Report

Verify every acceptance criterion with its actual oracle, not merely a passing test count. Check the exact candidate diff and allowed paths, format or artifact requirements, index state, protected state, and remote identity required by the role contract. Read the complete results before committing; after a commit or push, verify the new immutable identity and remote state where required.

Do not claim an unperformed Reviewer, merge, physical-device, human, Android, or external gate. Do not move to the next role yourself when the governing workflow assigns that responsibility elsewhere.

## Stop and Failure Signals

When an oracle cannot run, its output is incomplete, its candidate identity is uncertain, or required evidence is unavailable, stop the failed execution and withhold unsupported passing claims. Continue authorized investigation and correction within the current role's allowed paths; ordinary code or carrier defects do not require user approval. Request a decision only for confirmed incompatible valid requirements, an undecided business rule, or a next action that exceeds the contract, validation budget, environment, paths, or role permissions. Stopping a failing execution does not freeze already-authorized corrections.

Failure signals include: “should” or “probably” replacing a required run, a partial command represented as complete, a large unrelated suite substituted for the direct oracle, a successful build represented as behavior proof, an emulator represented as a physical device, old evidence falsely presented as proof of newly changed behavior, or a Writer represented as an independent Reviewer. Preserving passed results in their original unaffected scope is not a failure signal.

Once every accepted claim has one fresh risk-matched proof and all required gates are satisfied, stop. Do not repeat a passing command when the candidate and relevant environment are unchanged, add a broader suite for appearance, hash unrelated files, or manufacture extra evidence. A new failure returns to the authorized implementation or diagnostic role; an unapproved write, evidence layer, or decision returns `SCOPE_EXPANSION_REQUIRED` or the role contract's blocked terminal.
