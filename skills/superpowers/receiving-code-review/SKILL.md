---
name: receiving-code-review
description: Use when a Writer or Repair Writer receives a code-review finding batch; verifies the feedback against the accepted contract and codebase before making the smallest causally complete correction.
---

# Receiving Code Review

## 批准范围内自行修错与人工决定
- 批准范围内的代码、脚本、参数、导入、接线和验证载体实现错误，由当前agent自行定位和修正，不请求重新批准，不转交用户修改。使实现恢复到合同规定的行为，不属于新增业务决定。
- 真实冲突：两条有效要求无法同时满足，例如一条要求“有引用的项目必须保留”，另一条要求“删除时连同引用一起删除”。先定点核对来源和优先级；确实无法依据已有决定解决，才交用户裁定。
- 业务未决选择：合同缺少一个会影响用户实际结果的规则，必须在不同业务行为之间选择，例如尚未规定“停用项目是否允许新增关联”。已有决定直接沿用；符合相同合同的技术写法由agent自行选择，语法、导入、参数和接线错误不属于业务未决选择。例子只解释标准，不新增当前任务的规则或测试。
- 越界、超出合同或缺少必要操作授权：下一动作实际超出已批准路径、业务规则、验证内容、运行次数、环境或角色权限，才说明准确增量并请用户决定。不能仅因发生错误、技术方法尚未查清或修复改变了错误结果，就声称缺少授权。
- 停止失败的执行、保留现场和暂不声称通过，不等于冻结批准范围内的定位与修正。仅暂停依赖未决决定或缺少授权的动作；其他已授权工作继续。恢复、再次执行等动作按原批准内容和次数判断，不自动增加次数、不重跑未受影响通过项。
- 将载体修正为正确执行原定输入、步骤、断言和编排行为，不算扩大验证；新增或改变这些已批准要求才属于范围变更。不得借修错放宽断言、改输入、增加场景或反复改变编排来绕过失败。本规则不授予Planner实施、Reviewer修改候选或其他角色之外的权限。
来源：2026-10-11本PubMan主管理对话用户明确决定。本定义优先于旧的无条件暂停、遇到疑问即审批或按失败次数自动升级措辞。


## Story实施、审查与修复边界

1. **首次Writer完整实现批准的Story**，把范围内的行为和接线做完整。
2. **首次Reviewer独立完整审查；后续Reviewer只检查当前批准的修复及其明确影响。** 原来通过的审查结论和测试结果按原范围保留，不自动失效、不重复审查或重跑。
3. **Repair Writer严格按批准内容修改。** 指定的问题、路径和验证范围就是边界；需要改别处或增加验证，先说明并取得批准，不能借“完整原因链”自行越界。

首次完整检查应覆盖批准范围内的实际行为和必要直接接线；后续Review只完成当前批准修复及明确影响的审查，继承原范围内已通过的结论和测试。完整提示词、完整报告和fresh Reviewer均不表示重审整个Story或重跑旧测试。

## Purpose

Turn one complete accepted finding batch into a technically verified Repair without blind agreement, partial understanding, or scope expansion.

## Lifecycle and transitions

```text
BATCH_BOUND -> FINDINGS_VERIFIED -> SUCCESS_DEFINED -> REPAIR_EXECUTED
-> VERIFIED -> RETURNED
```

- `BATCH_BOUND`: bind the complete accepted batch, reviewed candidate, authority, expected paths, hard-forbidden boundaries, causal-expansion rule, validation profile, and protected state.
- `FINDINGS_VERIFIED`: reproduce or source-prove every finding and identify shared causes without treating Reviewer wording as authority.
- `SUCCESS_DEFINED`: state the minimum causally complete production modification set and claim-to-evidence budget before editing.
- `REPAIR_EXECUTED`: use the accepted oracle and `../test-driven-development/SKILL.md` for behavior changes; route unexpected failures to `../systematic-debugging/SKILL.md`.
- `VERIFIED`: use `../verification-before-completion/SKILL.md` on the exact Repair candidate.
- `RETURNED`: return one complete terminal to management. Do not self-Review, merge, dispatch a Reviewer, or start another Story.

If the accepted oracle remains red, return to the supported root cause and the appropriate TDD/debugging state; do not weaken the oracle, pile on speculative changes, or repeat an unchanged gate until it happens to pass. Reach `RETURNED/DONE` only from `VERIFIED`. A missing authority or human-owned decision returns the precise blocked terminal instead of looping.

## Workflow

1. Read the complete batch before editing. Restate each requirement in technical terms and bind it to the accepted contract, exact candidate, affected path, and observable failure.
2. Verify every finding against the current codebase. Resolve ordinary uncertainty by scoped investigation; correct implementation defects under the accepted contract and allowed paths without renewed approval. Pause affected work and request a decision only for confirmed incompatible valid requirements, an undecided business rule, or an action that exceeds the contract or lacks authorization. An unclear technical method alone is not a blocker; preserve contrary evidence for an invalid finding instead of modifying correct code.
3. Identify the shared root cause and adjacent direct consumers needed for a causally complete correction. Do not turn the scan into a general audit.
4. Use `../test-driven-development/SKILL.md` for approved behavior changes. Use `../systematic-debugging/SKILL.md` only for an observed unexpected or unexplained failure. Use `../verification-before-completion/SKILL.md` before any completion, commit, or push claim.
5. Close the accepted batch in one Repair candidate and report finding-to-change-to-evidence mapping. Do not self-Review, merge, or dispatch the next role.

Read-only investigation may inspect adjacent code, but a finding is not write authority outside the filled contract. An additional write path requires either an already accepted causal-expansion rule or an exact scope amendment before editing. If the complete Repair needs a new product, architecture, ownership, schema, migration, dependency, public-interface, security, or evidence decision, return `SCOPE_EXPANSION_REQUIRED` or the role contract's blocked terminal. Do not evade a closed boundary with a wrapper, adapter, fallback, duplicate authority, or partial fix.

Define the smallest sufficient evidence set before execution. Size it by demonstrated semantic risk propagation from the accepted contract, owner/consumer paths, public contracts, state, persistence, concurrency, packaging, real boundaries, or observed failures—not by change size or imagined possibility. Run focused and directly affected checks by default. Run a repository-wide suite only for an accepted gate, demonstrated repository-wide propagation, or proof that a narrower accepted suite cannot cover the claim. Rebuild artifacts or perform device/performance/external/human steps only when propagation reaches that boundary or the contract requires them. Do not repeat a passing unchanged gate. Stop when the complete accepted batch is closed, every claim has one fresh risk-matched proof, and all mandatory gates are satisfied.

## Repair Constraints

- Do not present guesses as facts, conceal uncertainty, or implement feedback merely because a Reviewer asserted it. Rely on explicit user decisions and applicable type, database, contract, and framework guarantees. An unsupported scenario alone is not a Repair requirement or blocker; discuss actual unresolved choices before dependent work.
- Make the smallest complete correction for verified findings, including necessary direct callers, data handling, and UI wiring within the approved scope; clean up only problems introduced by the current Repair.
- Do not add speculative behavior or failure flows based only on an error channel or an unsupported scenario; do not turn an unknown into a claim of impossibility.
- Place checks where data is accepted or a rule is enforced, including required business-state checks. Do not repeat a check downstream while the established guarantee remains valid.
- Reuse accepted error handling, preserve the original cause, and perform required cleanup. Never disguise failure as success, an empty result, or a default. Catch breadth alone does not determine correctness; explicit failure does not require crashing the whole application.
- A function or module may serve a concrete current responsibility even with one caller. Do not add interfaces, wrappers, factories, managers, or configuration only for hypothetical reuse; any scope change still requires approval.
- Define success criteria before editing and use only approved validation. Explain actual failures; do not repeat unchanged runs, change inputs, weaken assertions, or extend waits merely to obtain a pass.
