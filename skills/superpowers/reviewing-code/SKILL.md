---
name: reviewing-code
description: Use when acting as a fresh independent reviewer of a candidate change before acceptance or integration; performs read-only source-first requirements, quality, and evidence review without becoming the implementer or planner.
---

# Reviewing Code

## 批准范围内自行修错与人工决定
- 批准范围内的代码、脚本、参数、导入、接线和验证载体实现错误，由当前agent自行定位和修正，不请求重新批准，不转交用户修改。使实现恢复到合同规定的行为，不属于新增业务决定。
- 真实冲突：两条有效要求无法同时满足，例如一条要求“有引用的项目必须保留”，另一条要求“删除时连同引用一起删除”。先定点核对来源和优先级；确实无法依据已有决定解决，才交用户裁定。
- 业务未决选择：合同缺少一个会影响用户实际结果的规则，必须在不同业务行为之间选择，例如尚未规定“停用项目是否允许新增关联”。已有决定直接沿用；符合相同合同的技术写法由agent自行选择，语法、导入、参数和接线错误不属于业务未决选择。例子只解释标准，不新增当前任务的规则或测试。
- 越界、超出合同或缺少必要操作授权：下一动作实际超出已批准路径、业务规则、验证内容、运行次数、环境或角色权限，才说明准确增量并请用户决定。不能仅因发生错误、技术方法尚未查清或修复改变了错误结果，就声称缺少授权。
- 停止失败的执行、保留现场和暂不声称通过，不等于冻结批准范围内的定位与修正。仅暂停依赖未决决定或缺少授权的动作；其他已授权工作继续。恢复、再次执行等动作按原批准内容和次数判断，不自动增加次数、不重跑未受影响通过项。
- 将载体修正为正确执行原定输入、步骤、断言和编排行为，不算扩大验证；新增或改变这些已批准要求才属于范围变更。不得借修错放宽断言、改输入、增加场景或反复改变编排来绕过失败。本规则不授予Planner实施、Reviewer修改候选或其他角色之外的权限。
来源：2026-10-11本PubMan主管理对话用户明确决定。本定义优先于旧的无条件暂停、遇到疑问即审批或按失败次数自动升级措辞。


## 共享模板入口

代码审查角色使用 `C:/Users/25073/.codex/skills/CODE_REVIEW_PROMPT_TEMPLATE.md`。编写或修订提示词前从此准确路径完整读取，不采用旧打包或其他项目副本。统一来源及同步记录见 `C:/Users/25073/.codex/AGENTS.md`；该文件的直接人工讨论门禁与用户批准边界优先于本技能中的旧转交措辞，本入口不授权 Reviewer 修改候选或派发角色。

## Story实施、审查与修复边界

1. **首次Writer完整实现批准的Story**，把范围内的行为和接线做完整。
2. **首次Reviewer独立完整审查；后续Reviewer只检查当前批准的修复及其明确影响。** 原来通过的审查结论和测试结果按原范围保留，不自动失效、不重复审查或重跑。
3. **Repair Writer严格按批准内容修改。** 指定的问题、路径和验证范围就是边界；需要改别处或增加验证，先说明并取得批准，不能借“完整原因链”自行越界。

首次完整检查应覆盖批准范围内的实际行为和必要直接接线；后续Review只完成当前批准修复及明确影响的审查，继承原范围内已通过的结论和测试。完整提示词、完整报告和fresh Reviewer均不表示重审整个Story或重跑旧测试。

## Purpose

Review the exact candidate against its accepted contract and real evidence. The role template supplies identity, scope, permissions, and output fields; this skill owns the review method.

## 先按真实流程和保存语义判断

- 先从已接受的用户流程确认入口、操作顺序、保存／确认动作及实际反馈，再判断源码是否违反该流程。函数或接口能够串接，不足以证明自行拼出的用户场景属于任务。
- 区分正在编辑、待保存内容、保存确认、已持久化结果及当前正式归属；沿用用户已确定的提交语义。未保存选择不能当作已保存的归属变更，草稿变化不能混称正式数据变化。共享更新依据已接受的共同owner和保存规则判断。
- 误操作、中断、交错编辑或未保存切换场景，必须有已接受流程／入口规则及具体可达依据；缺少依据时说明未知，不能据此列阻塞finding、要求新增修复或测试。不把“不常见”说成“不可能”，也不把技术可调用说成业务必须支持。
- 用户明确裁定操作流程或场景边界后，按该边界更新判定，保留有效审查和测试。被排除场景不继续作为当前阻塞；保留源码事实，不冒称路径不存在、代码已修复或所有场景已证明。不得自行新增锁、自动保存、丢弃草稿或页面限制来强制流程。

## 用户流程与最小必要检查

以下规则约束本文件中的完整读取、身份核验、独立核对和交付检查，不新增审批层、报告文件、测试或检查轮次。
1. 涉及既有功能的 Story，先继承已接受产品决定、相关 UI/UX 设计和真实使用流程，明确“用户所在页面→看到的控件→实际点击/手势→页面反馈与去向”，再对应本次内部接线及保留行为。交接必须给出准确来源和必要章节；单条测试流程、源码调用链或接口状态不能代替整个 Story 的用户流程。只读相关片段，不为继承依据通读全部产品文档；已有决定直接沿用，真正冲突才讨论。
2. 疑问先说明具体用户操作链、当前影响及依据。错误返回通道、理论并发窗口、纯函数或未使用回调的存在，不自动构成用户场景、产品需求或实施阻塞；无具体可达依据时如实保留未知，不据此扩展失败页面、恢复机制或测试。实际正常操作出现接线缺陷应修正接线，不以失败提示替代修复；不吞错、不把未证实说成不可能。
3. 同一任务中已读取、已核对且未变化的技能、文档、源码及证据直接复用；不得为压缩恢复、续作、交付措辞或“更保险”再次全读、算哈希或重跑门禁。提示词模板适用下一条的每次必读例外。新独立角色只对自身必需输入做一次初始绑定；只有明确变化、实际身份冲突或当前动作依赖尚未确认的状态时，才定点补核，不把上一角色结论冒充自己的独立审查。
4. 每次编写或修订提示词前，必须从准确路径重新完整读取对应模板正文一次；上下文压缩后继续编写同样先读，不能凭记忆、压缩摘要或上次填写稿代替。此要求仅授权读取模板，不自动触发模板完整性审计、哈希核验、Git检查或其他来源重读。未修改原模板时，不重新审计其正文完整性。新生成提示词只对本次填写/修改内容及其必须保留的规则做一次必要检查；不能把原样保留模板变成全文反复比对任务。交接检查首先确认用户流程、已有决定、此次改动及直接消费者的对应关系；占位符清零、哈希一致和格式完整不能代替这项判断。
5. Git及保护状态核对只在当前动作确有需要的入场/工作树绑定、暂存提交或集成节点执行必要集合；同一节点已完成且状态未变化的检查不得重复。规划续作、普通答疑、文案整理不得自动触发整套Git、哈希或保护资产审计；不取消已批准且尚未完成的必要门禁。
6. 搜索先限定直接相关文件、章节和符号；长文档先定位再取片段，不用宽泛关键词跨大量历史全文输出。已有证据足够回答或完成当前修改立即停止；输出过宽时收窄，不追加更大调查。不得为证明自己检查过而创建额外清单、脚本、报告或测试。
7. 发生交接失误时，依据实际记录说明遗漏和影响；不能未经检查就宣称整段实现都在猜、全部无效或必须重开。保留已完成工作，只修正有依据的缺口。用户要求减少消耗时立即收窄或停止无必要操作，不能拿治理要求解释反复检查。

## 官方行为与需求核对

先查官方与现有能力，优先复用：
- 从需求澄清、方案设计和技术选型阶段就先查证，再确定设计与实现方案；实现、修复及缺陷判断同样遵循此规则，不以已经出现缺陷为查询前提。先定点查阅与实际版本有关的官方文档/源码、GitHub仓库及Issues、其他可靠网络资料和适用的成熟实现，不限于官方网站，并核对项目现有代码。优先原始来源和维护者资料，区分官方保证、维护者结论与社区经验，不把社区猜测当系统保证。明确系统默认行为、配置/API已经提供的能力，以及当前需求真正缺少的部分；不先假设需要自写逻辑。
- 优先复用系统能力、框架API和项目现有实现；确需引入成熟开源实现时核对适用版本与许可，不搬入整套无关架构。已有能力能满足需求时，不再自建等价owner、包装层、调度或恢复机制。
- 先确认真实用户入口和已接受需求。未使用回调、纯函数、框架API或潜在技术场景的存在，不证明有用户可触发的缺陷，也不授权新增入口、按钮或功能来制造迁移/测试的必要性。
- 简短交代来源链接/版本、复用能力及必须自写的部分和理由。同版本已核实资料直接复用；足够回答当前问题即停止搜索，不新增研究轮次、不重读全部历史。资料不足或冲突时明确UNKNOWN/CONFLICT，不猜成事实，不把默认行为说成所有版本/设备上的保证。
- 网上方案先核对来源、适用版本及既有业务规则。常识明确、与既有业务规则一致且必要操作已获授权的做法直接落实，不因来源在网上而额外确认；仅存在经核实的真实冲突、合同未定的业务规则选择或缺少必要操作授权时，才向用户说明来源、用途、收益与代价及范围/验证影响并取得决定。网上建议本身不构成授权。
- 查阅、核对和比较资料可以按已有授权进行。满足上述讨论条件的方案，在取得决定前不得实施依赖它的修改、接入依赖或替换已批准设计；其余明确且已授权的工作直接继续。来源变化但方案及批准边界未变时复用原批准；网上建议和示例不是用户需求或实施授权。
- 网上建议和示例不是需求或权限。新增/升级依赖、安装工具、产品/架构/生命周期改变、额外路径及验证增量，仍受下列越界门禁约束；源码/API/hash只能证明其对应层，不能冒充实际运行。

Review只核对当前候选及其实际需求，不承担新的方案设计。不能仅因网上存在另一方案、未使用回调或潜在框架场景就报功能缺失；finding必须指出实际违反的已接受义务及可达条件。复用已有有效资料，不为fresh Review机械重做联网研究。

越界立即停止与完整报告：
- 一旦判断下一步超出已获准确授权的需求、范围、包数、写路径、owner、生命周期、接口、数据责任、依赖或环境，或需要改变已批准的验证要求（新增方法/场景/输入/步骤/断言，或改变fixture/调度/callback/等待/超时/命令/回归的既定行为），必须在相关新增工作、修改或运行前停止，保留现场与有效证据。
- 技能、模板、Review、因果必要性、风险传播、“完整性”“更保险”“最佳实践”均不能代替用户批准。本门禁同样约束后文关于扩大诊断/验证或重新运行的表述；已获准确批准的范围无需重复审批。
- 交回主管理的报告一次写清六点：
  1. 具体问题、文件/方法和实际行为，区分PROVEN、INFERENCE、UNKNOWN。
  2. 原定工作已完成和已证明的内容。
  3. 具体缺少哪项行为证明或合同依据。
  4. 拟增加的准确路径、输入、步骤、断言、设施或命令及各自用途。
  5. 原范围为什么不能解决，不增加会留下什么实际缺口。
  6. 当前改动、产物、证据、未完成事项及获批后的唯一下一动作。
- 标记角色对应的BLOCKED/REVIEW_BLOCKED，不声称完成或全部通过；未获用户明确批准不得执行增量，不能通过其他角色绕过。获批后只执行准确批准范围。Reviewer可完成其余已授权审查，但不得把待批准增量写成Repair必须执行的要求。
- 不循环改变fixture/调度/等待/超时的已批准行为来绕过失败，不重复命令碰运气，不新增生产test seam，不削弱断言、改变输入或事务边界绕过失败。产品、设施和环境失败必须分清。
- 已通过且未受影响的结果继续有效；只复验准确修复对应的已批准集合，不因局部修改重跑整批，不在交付前追加测试、build、设备、性能或人工门禁。

## Lifecycle and transitions

```text
BOUND -> CONTRACT_RECONSTRUCTED -> DELTA_REVIEWED -> EVIDENCE_REVIEWED
-> PASS | CHANGES_REQUESTED | REVIEW_BLOCKED | NEEDS_USER -> RETURN
```

- `BOUND`: bind the immutable base/candidate, accepted sources, authorized scope and amendments, hard-forbidden boundaries, protected state, validation profile, and integration permission.
- `CONTRACT_RECONSTRUCTED`: derive the complete current-node obligations independently of the Writer report.
- `DELTA_REVIEWED`: inspect the exact delta and every direct owner, consumer, state, persistence, error, security, and lifecycle effect required by this node.
- `EVIDENCE_REVIEWED`: map each load-bearing claim to one fresh risk-matched oracle at the accepted boundary.
- `PASS`: SPEC, QUALITY, and EVIDENCE all pass with no blocker, must-fix, or should-fix.
- `CHANGES_REQUESTED`: return one complete atomic findings batch after all applicable axes are complete.
- `REVIEW_BLOCKED`: objective authority, safety, identity, or claim-proving evidence prevents completion.
- `NEEDS_USER`: a specifically human-owned decision or gate is required.
- `RETURN`: give the complete terminal to management; never dispatch Repair or the next Story.

## Workflow

1. Bind the exact accepted base, candidate, contract, allowed scope, and protected state. Treat branch names and delivery reports as locators, not proof.
2. Reconstruct expected obligations from accepted sources before accepting the Writer's inventory or conclusions.
3. Inspect the exact delta and its direct consumers. Follow affected ownership, lifecycle, state, persistence, error, security, and evidence paths only as far as the current node requires.
4. Compare every load-bearing obligation to implementation and an independent observable oracle. Include only contract-relevant negative or excluded-state cases within the accepted workflow and approved evidence profile; do not invent operation ordering or expand verification. Passing examples prove only their actual scope.
5. Run only fresh checks that can prove the current claims at the real boundary required by the contract. Do not replace production or persistence evidence with source inspection, mocks, no-ops, simulations, or injected seams unless that is the accepted boundary.
6. Complete all applicable review axes after finding an issue. Return one atomic findings batch and a clear verdict.

Review the complete current node, not the entire repository or accepted history. Read-only inspection may follow a direct effect outside the expected path list, but do not turn adjacent discovery into a general audit. Classify an extra candidate path as authorized causal expansion only when the accepted contract or amendment already covers the same behavior, direct owner/consumer/test, and no new product, architecture, ownership, schema, migration, dependency, public-interface, security, or evidence decision. Otherwise report the exact scope violation or missing decision; never normalize it after the fact.

Before running commands, define a claim-to-evidence budget. Size it by demonstrated semantic risk propagation from the accepted contract, owner/consumer paths, public contracts, state, persistence, concurrency, packaging, real boundaries, or observed failures—not by character count, file count, apparent simplicity, or imagined possibility. Focused and directly affected checks are the default. A repository-wide suite is required only when it is an accepted gate, propagation is demonstrated as repository-wide, or narrower affected evidence proves unable to cover the claim. Device, performance, external, human, and artifact gates run only when propagation reaches that boundary or the contract requires them. Fresh Review does not mean repeating every Writer command. Stop validation when all current-node claims have one sufficient independent proof; continue the remaining source/evidence axes without adding ceremonial work.

## Required Checks

- Do not present guesses as facts or conceal uncertainty. Rely on explicit user decisions and applicable type, database, contract, and framework guarantees. Mark decision-relevant facts as `PROVEN`, reproducible `INFERENCE`, `UNKNOWN`, or `CONFLICT`; an unsupported scenario alone is not a finding or blocker. Discuss actual unresolved choices under the Story gate.
- Require the smallest complete change for the approved problem, including necessary direct callers, data handling, and UI wiring. Fewer files is not a reason to leave half a feature. Reject speculative features, incidental refactors, and unapproved scope.
- Confirm only necessary places changed and only task-created problems were cleaned up.
- Require explicit success criteria and valid evidence before a passing claim. Stay within approved validation; explain actual failures, do not repeat unchanged runs or change inputs, weaken assertions, or extend waits to obtain a pass.
- Check that validation is placed where data is accepted or a rule is enforced, including required business-state checks. Do not require duplicate downstream checks while an established guarantee remains valid.
- Do not demand guards, fallbacks, or extra validation for unsupported hypothetical scenarios or states excluded by explicit applicable guarantees; do not guess that an unknown state is impossible.
- Check that accepted error handling preserves the original cause and required cleanup. Reject failure disguised as success, an empty result, or a default; do not classify a catch as swallowing merely by its breadth, or demand a new failure flow merely because an error channel exists.
- Evaluate structure by its concrete current responsibility, not call count. A function or module with one caller can be appropriate in either existing code or a new project; reject abstractions justified only by hypothetical reuse.
- Require an explicit, truthful failure outcome under the accepted contract; this does not require crashing the whole application or adding unapproved recovery behavior.
- Verify approved tests exercise real behavior and the decisive boundary, including the approved negative cases; this does not authorize adding scenarios or rerunning passed checks.

## Independence And Permissions

- Stay read-only before the accepted PASS integration gate. Do not edit, Repair, stage, commit, rebase, merge, push, plan new behavior, dispatch roles, or create subagents.
- Do not load BMAD, implementation, orchestration, worktree, branch-finishing, or Review-dispatch skills. This restriction does not apply to this `reviewing-code` skill or the required completion-verification skill.
- Before claiming PASS or performing an explicitly authorized mechanical integration, read and apply `../verification-before-completion/SKILL.md` to the exact verdict and integration claims.
- A missing or conflicting load-bearing source blocks only the affected decision; report the exact missing authority instead of guessing.

## Findings And Verdict

For each actionable finding, provide the source obligation, exact location, reproducible scenario or evidence, consequence, severity, and minimum causal Repair direction. Do not implement the fix.

PASS requires requirements, quality, and evidence to pass for the complete current node with no blocking findings. A finding prevents PASS but never justifies skipping the remaining applicable review.

Only a filled role template may authorize post-PASS mechanical integration. If authorized, verify the bound candidate and refs again, integrate without content edits, verify the resulting identity and protected state, and then return. Conflict, unexpected tree change, failed push, or post-check failure transitions to `REVIEW_BLOCKED`; do not repair it inside Review.
