---
name: bmad-method
description: 协作完成正式编码前的产品发现与规划，包括 Product Brief、PRD、范围、UX/视觉、Architecture、Epic/Story、implementation readiness、planning validation 与 Correct Course。用于创建、更新、审查或修复这些规划产物；不用于代码实现、代码 Review 或自动交付。
---

# BMAD Method：正式编码前规划

## 目的与边界

把用户意图转成用户拥有、来源可追、可实施且可独立审查的规划合同。技能负责 Discovery、Product/Scope、UX/Visual、Architecture、Epic/Story、Readiness、Planning Review、Consistency Audit、Correct Course 与 post-validation Repair；到一个 exact ready Story 即停止并交回项目的主管理流程。

不接管实现、代码 Review、角色派发或 Git 集成，不因完成规划而扩大当前权限。

## Authority 与来源诚实

按以下优先级工作：

1. 当前项目的 `AGENTS.md`、accepted decisions、identity-bound artifacts、角色模板与用户明确决定。
2. 当前任务的 immutable requirement、accepted sources 与直接证据。
3. 本技能的共同合同及按需 reference。

本技能依据官方 [BMAD-METHOD](https://github.com/bmad-code-org/BMAD-METHOD) 6.10.0（MIT）中 Discovery、PRD、UX、Architecture、Epic/Story、Readiness、Advanced Elicitation 与 Correct Course 的可执行机制重建，并依据官方 Codex `skill-creator` 组织 progressive disclosure；它不是官方包的逐字复刻，也不依赖官方包或本机源码路径运行。被替换的项目旧蒸馏版只可作为回归 baseline，不能反推正确规则。

事实不足时写 `UNKNOWN` 并说明影响。推荐、常见做法、候选自身声明、旧 verdict 或 Planner 推断都不能升级为 accepted decision。

## 共同状态合同

任何创建或更新型 planning artifact 都要用项目现有格式可恢复地维护下列状态。若现有 frontmatter 不适合，可在正文建立等价 ledger；不引入另一套脚本或模板。

| 状态 | 必须记录 |
|---|---|
| `acceptedFacts` | 可证明事实、来源、适用边界 |
| `acceptedDecisions` | 用户或 accepted authority 已决定的事项、条件、来源 |
| `assumptions` | 尚未确认的推断；Fast path 中逐项标 `[ASSUMPTION]` |
| `openQuestions` | 未回答问题、影响、owner |
| `phaseBlockers` | 不关闭就不能进入下一规划高度或实现的事项 |
| `deferred` | 当前不阻塞的事项、owner、revisit condition |
| `rejectedAlternatives` | 被否决方向及原因，防止压缩或换对话后复活 |
| `sources` | 每项承重 claim 的来源；没有来源就写 `UNKNOWN` |
| `currentNode` | 当前 planning node |
| `firstUnfinishedAction` | 下一项尚未完成的具体动作 |

每次承重回答后更新相关 ledger、直接 ripple、`currentNode` 与 `firstUnfinishedAction`。不得用 `pendingDecisions=[]`、文档长度、DAG 无环、hash/path count/Git gate 或用户曾点过 `Continue` 证明产品规划完整。

自动上下文压缩不是冷启动或批准。先从系统摘要、当前 artifact 与 ledger 恢复角色、`currentNode` 和 `firstUnfinishedAction`；不重放已完成决定、审批、验证或工作。关键事实无法证明时只重读该事实的 authority，并把它保持为 open/blocked，不从头重跑。

## 协作模式

### Coaching（默认）

用户没有明确要求速度时使用。先给完整表达空间，再一次只问一个开放问题；反射薄弱、矛盾或承重回答并追问、给反例或展示 trade-off。Planner 可提出真实备选与推荐，但只有用户能接受 load-bearing product、scope、visual 或 architecture 决定。

### Fast path

只在用户明确选择或明显要求速度时使用。可以把最少缺口合并成一到两个批次，但每项推断必须标 `[ASSUMPTION]`；`openQuestions` 和 `phaseBlockers` 保持真实，不能为交付一份完整草案而清零。遇到新承重决定、scope conflict、phase blocker 或 authority/permission 缺失即暂停。

用户可一次授权连续完成没有新承重决定的机械步骤。不要用逐页 `Continue` 模拟协作；只有真实停止条件才暂停。

## 路由与按需加载

先识别 intent：create、update、validate、Correct Course 或 post-validation Repair；再选择最低必要 planning 高度。普通任务只读当前 node 的一个直接 reference，不默认加载其他领域；切换 reference 前必须先记录前一 node 的退出条件与状态转换。

| 当前 node / intent | 何时加载 | 直接 reference |
|---|---|---|
| Discovery intake、Product Brief discovery、brainstorm、需求仍模糊 | 需要展开意图、来源、concern 或二次反思 | [discovery-and-elicitation.md](references/discovery-and-elicitation.md) |
| Product Brief draft/update/validate、PRD、产品能力、范围或 Epic 边界 | Discovery 已完成，需要定义 why/user/journey/capability/scope/done state | [product-and-scope-planning.md](references/product-and-scope-planning.md) |
| UX、visual direction、surface、state、component、图表 | 需要关闭体验或视觉合同 | [ux-and-visual-contracts.md](references/ux-and-visual-contracts.md) |
| Architecture / solutioning | 下一级单元可能对非显然承重问题给出不兼容答案 | [architecture-and-solutioning.md](references/architecture-and-solutioning.md) |
| Epic/Story 分解或 implementation readiness | 上游产品、适用 UX 与 Architecture 已足够关闭 | [epic-story-and-readiness.md](references/epic-story-and-readiness.md) |
| Planning Review、Consistency Audit、Correct Course、Repair | 审查、变更或修复现有 candidate | [validation-correct-course-and-repair.md](references/validation-correct-course-and-repair.md) |
| 维护或验证本技能行为 | 需要逐项走查能力 oracle | [regression-scenarios.md](references/regression-scenarios.md) |

“创建/更新 Product Brief”是两个串行 node，不是两个 reference 共同拥有同一 node：

1. 设置 `currentNode = discovery_intake`，只加载 Discovery reference，完成 brain dump/source intake、stakes/form-factor、concern scan 与 discovery blockers。
2. 只有 Discovery completion 全部满足时，记录 `currentNode = product_scope`，把 `firstUnfinishedAction` 设为从已协调的 Discovery 状态 draft/update Product Brief；随后卸载 Discovery reference，只加载 Product/Scope reference。
3. Product/Scope 唯一负责 Product Brief 的 draft/update/validate 与 completion。若发现必须返回 Discovery 的缺口，先记录反向状态转换与具体 blocker，再卸载 Product/Scope reference；任一时刻不得同时加载两者完成 ordinary Product Brief。

“检查 implementation readiness”是单一 ordinary node：只加载 Epic/Story/Readiness reference并在其中形成 expected inventory、完成全部适用检查、返回 findings batch 与 readiness verdict。不要为该 intent 加载 Validation reference；后者只拥有 Planning Review、Consistency Audit、Correct Course 与 post-validation Repair。

跨阶段 Planning Review/Consistency Audit 先独立重建应有 inventory，再只加载验证该 inventory 所需的直接 references。Correct Course 只加载 trigger 影响到的领域。Repair 只加载 current candidate、完整 finding batch 与直接相关 accepted sources。

## 阶段边界

```text
Discovery intake / Product Brief discovery
→ Product Brief / Product / PRD / Scope draft-update-validate
→ applicable UX / Visual
→ Architecture / Solutioning
→ Epic / Story / Implementation Readiness
→ Planning Review + Consistency Audit
→ exact ready Story
→ return to project management
```

- Discovery 先发现，不把第一份表达直接压成范围；完成后显式切换到 Product/Scope，两个 reference 不并行驻留。
- Product/Scope 关闭当前与未来边界及 phase blockers 后，才能进入适用 UX/Architecture。
- UX 关闭 journey/surface/state/component/accessibility，以及适用 visual/chart/mock 合同后，才能进入 Epic/Story。
- Architecture 只锁定下一级实现会分叉的 non-obvious invariants；继承未受 delta 影响的 accepted parent invariants。
- Epic/Story 不得首次决定产品范围、UX 语义、core owner 或 data responsibility。
- Ordinary Implementation Readiness 在 Epic/Story reference 内同时检查 candidate 内部一致性与 candidate 外部完整性；Planning Review/Consistency Audit 是之后由 Validation reference 拥有的独立门禁。

## Advanced Elicitation

当范围、UX、Architecture 或 Epic structure 形成关键候选时，在当前 node 自然停顿。按风险选一个合适方法（如 pre-mortem、assumption audit、boundary sweep、inversion、stakeholder lens、map-is-not-territory、second-order thinking、red-team），展示它发现的问题与建议；只有用户接受后才修改候选。方法本身不是新 authority，也不能替用户接受决定。执行细节见当前领域 reference 的 checkpoint。

## 完成与 MANUAL_RELAY

完成当前 node 前执行该 reference 的 completion/readiness checks，并诚实保留 unknown、assumptions、open questions、deferred 与 phase blockers。创建/更新型产物只有在用户拥有的承重决定已记录、适用 blocker 已关闭、来源与直接 ripple 可追时才可进入下一高度。

在采用 `MANUAL_RELAY` 的项目里，到一个 exact ready Story 后：

- 记录 Story identity、accepted sources、scope/non-goals、ownership/lifecycle、AC、evidence 与剩余风险；
- 把控制权交回主管理对话；
- 不创建 subagent，不自动派发 Writer/Reviewer，不开始实现，不执行 merge/push。

Validation 与 integration 的角色、权限和顺序始终服从项目正式模板。
