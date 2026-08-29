# F6 — Epic、Story、Dependency DAG 与 Capacity

## 何时加载

Accepted Product、UX、Architecture 与 confirmed requirements inventory 需要转成有用户/治理价值、依赖闭合、可实施和可独立审查的 Epic/Story 时加载。

F6 不首次决定 product、UX、core owner、schema/data responsibility 或 evidence oracle；发现这些缺口时返回相应 F3/F4/F5/F7。

## Prerequisite gate

开始分解前确认：

- current/future/deferred/excluded scope 与 residual map 已接受；
- applicable UX journeys/surfaces/states/human gates 闭合；
- load-bearing Architecture owner/lifecycle/feasibility 已接受；
- C22 requirements inventory 已由用户明确确认，consumer 与 evidence responsibility 已知；
- immutable prerequisite identity 可证明或明确为尚未解锁。

任一缺口只返回最早责任高度，记录恢复 F6 的 `firstUnfinishedAction`；不能用更长 Story 吸收缺失规划。

## C22 — Epic design 前的 confirmed requirements inventory

在设计任何 Epic/Story 前，先固定并展示全部 confirmed inputs 的 inclusion/exclusion，再逐项提取 stable-ID inventory。输入全集至少检查：

- FR 与 NFR；
- UX requirement、journey/state/interaction/accessibility/chart/human decision；
- Architecture decision、inherited invariant 与 framework/owner/lifecycle constraint；
- data、evidence、human gate 与 direct-consumer obligation；
- domain concern、non-goal；
- residual、future、deferred、rejected/superseded language；
- 会影响 scope、meaning、priority、failure 或 acceptance 的定性 intent。

每项保留 stable ID、exact source/identity、classification、所属 input 的 inclusion/exclusion disposition 与理由、coverage status、适用 consumer。Excluded input 也必须展示其 identity、authority reason 和 non-trigger/downstream disposition；不能因不进入当前 scope 而从 inventory 消失。

把完整 inventory 以及所有 included/excluded inputs 一次展示给用户，允许指出 missing、misclassified、duplicated 或错误排除项；修正后取得对“输入全集及逐项 inventory 完整、准确”的明确 confirmation。摘要、前几项示例、artifact/hash存在、机械 count/coverage 或先前 blanket approval 都不是该确认。

任何 `UNKNOWN`、unmapped requirement、source conflict、遗漏信号或未确认 inventory 都 fail fast，且不得创建 Epic/Story：

- 无法解析来源/映射/冲突时输出 `REQUIREMENTS_INVENTORY_BLOCKED`，保留原始 source 信号，并把 `firstUnfinishedAction` 指向明确的 source reconciliation/correction；
- inventory 尚未被用户明确确认时输出 `REQUIREMENTS_INVENTORY_UNCONFIRMED`，设置 `currentNode=requirements_inventory_confirmation`，把 `firstUnfinishedAction` 指向完整 inventory/input disposition 的用户 correction/confirmation。

Confirmed inventory 是 C23–C26、T4 和 F7 coverage 的唯一 input universe。下游只能用明确 accepted delta 更新它；不得静默删减、从已设计的 Epic structure 反推输入全集，或用 Story coverage 补造 source obligation。Inventory confirmation 不自动接受 Epic structure；Epic value grouping/sequence 仍按下节单独展示和接受，机械 coverage 也不能替代 semantic confirmation。

## Epic design

Epic 按完整 user/business/governance outcome 组织，不按 database/API/UI/testing 技术层。每个 Epic 记录：

```text
epicId + title
named outcome and done state
covered obligations/journeys/surfaces
prerequisite identities and dependencies
primary owner/risk boundary
evidence/readiness signal
non-goals and residual disposition
```

Epic structure 或 delivery sequence 会改变用户价值/暴露风险时属于承重决定，由用户接受。Epic 不能依赖未来 Epic 才产生当前承诺的价值。

## T4 — Obligations/owners 到 Story DAG

每个 Story 至少包含：

```text
immutable Story identity/version
title + user/business/governance value
objective and observable old→new
source/obligation IDs
exact production change boundary
accepted owner/lifecycle/data/error contracts
non-goals and residuals
immutable prerequisite full identities
acceptance criteria
independent evidence/human gates
allowed/protected paths and actions
failure/stop terminals
downstream consumers
capacity verdict and rationale
```

Story围绕一个因果完整的 production state change 与匹配 evidence boundary。按技术层拆分但不能独立提供价值/治理结果的工作，可以作为同一 Story 的步骤；可独立 merge、独立验证或拥有不同 lifecycle/owner 的改变通常需要拆分并以 DAG连接。

## T4 转换闭合合同

- **Input authority**：C22 confirmed requirements inventory（保留 T1 obligations、T2 accepted decisions 与 T3 ownership map）、accepted value slicing 与 immutable prerequisites。
- **Output schema**：Epic/Story nodes、value、obligation IDs、owner/lifecycle set、production delta、non-goals、immutable predecessors、evidence boundary、status、residual disposition。
- **Coverage/invariant**：每个 non-deferred obligation 恰有一个 implementing Story/governance consumer；edges顺 prerequisite、DAG无环、owner/lifecycle稳定、capacity通过。
- **Allowed loss**：可 split/merge Stories，但保留 obligation lineage、value 与 review independence；future defer必须有 residual owner/revisit。
- **Forbidden loss**：lost obligation、cycle/unbound或branch-only prerequisite、一个 Story聚合多个独立 owner/lifecycle/evidence bundle、Story首次做 Architecture决定。
- **Unknown**：affected Story 保持 `NOT_READY`；展示 split/dependency routes；value sequence由用户决定，机械 DAG修复不升级。
- **Checkpoint**：用户接受承重 value slicing/sequence；保持 accepted semantics 的 capacity split 无需发明 scope。
- **Failure terminal**：`DAG_INVALID / STORY_OVER_CAPACITY / UNBOUND_PREREQUISITE / OWNER_DRIFT`。
- **Downstream/oracle**：T5/T6消费；机械 coverage/DAG + 独立 single-Writer/single-Reviewer capacity判定。

## Dependency DAG

节点用 immutable Story/commit/artifact identity；branch 名只能作 locator。Edge 记录 predecessor、consumer、解锁 invariant 与证明命令/证据。

机械检查：

- DAG acyclic；
- no self-edge、orphan、duplicate primary ownership 或 forward dependency；
- 每个 dependent Story 的 prerequisite 是 immutable，并在适用 host 中证明已 merged/accepted；
- roots/sinks 与 delivery outcome 一致；
- current obligations 恰有一个 Story disposition；residual 没有被 universe 静默清零。

无法证明 predecessor 时输出 `UNBOUND_PREREQUISITE`，不能因 branch 存在、旧报告 PASS 或测试曾通过而解锁。

## C25 planning-status producer handoff

F6只拥有 tracking mechanics 所需的 semantic input：immutable plan identity、accepted Epic/Story IDs、每个 Epic 内的 Story source order、适用 retrospective identity、status vocabulary/rank及其 source。把它们作为一个 canonical ordered key contract交给 F1 的唯一 planning-status mechanical owner。

F6不生成/修改 status file，不从文件、commit或实现证据推断当前 status，也不以 tracking结构通过证明 Story/readiness完整。duplicate/unstable identity、未接受顺序或 status vocabulary不唯一时，F6返回 `STORY_IDENTITY_BLOCKED`；不能让 mechanical process发明 key 或 semantic state。

## Story capacity 十项结构判据

逐项给 `PASS / FAIL / UNKNOWN` 与 evidence：

1. **Value coherence**：一个清楚 old→new outcome。
2. **Obligation closure**：in-scope obligations 可一次闭合，无隐含 residual。
3. **Owner stability**：不首次创建/改变 core owner 或第二 authority。
4. **Lifecycle coherence**：同一可审查 lifecycle/transaction/state boundary。
5. **Dependency closure**：prerequisites immutable、先序、无环且已满足或明确阻塞。
6. **Production boundary**：exact change/allowed/protected paths 可绑定。
7. **Evidence coherence**：每个 AC 有 matching-layer independent oracle/human gate。
8. **Single-Writer feasibility**：无需新增产品/UX/Architecture/evidence决定即可因果完整实施。
9. **Single-Reviewer judgeability**：一个 fresh Reviewer 可覆盖完整 delta/contract，不依赖 Planner解释。
10. **Failure/recovery closure**：真实边界、rollback/retry/migration/consumer failure在范围内闭合。

不得用 token、word、文件数、代码行、估点或“实现简单”替代这些判据。

## Capacity terminals

- `CAPACITY_PASS`：十项均 PASS，且没有跨 Story 未闭合义务。
- `SPLIT_REQUIRED`：存在两个或更多可独立 merge/review 的 owner、lifecycle、state transition 或 evidence component；给出 DAG split 与 obligation disposition。
- `ARCHITECTURE_RETURN_REQUIRED`：拆分无法解决，因为 owner/interface/data responsibility 尚未接受或不稳定。
- `CAPACITY_BLOCKED`：关键 identity、source 或 evidence capability 不可用，无法判断。

典型 overload：一个 Story 同时改变独立的 physical schema/migration、semantic validator、canonical encoder/digest、repository transaction/read assembly、direct consumer 与 status lifecycle。只有证明它们是不可分割的单一 atomic invariant、同 owner/lifecycle、同 evidence boundary 后才可不拆；“都与同一功能有关”不够。

## Acceptance criteria

AC 来自 accepted obligation，而不是 implementation checklist。每条包含 actor/producer、condition、observable behavior/result、failure语义、boundary/consumer 与验证方式。required-iff、forbidden、identity/binding、ordering、atomicity、transition、error、migration 和 direct consumer 不能揉成模糊的“works correctly”。

不要规定不承重的函数拆分或局部算法；也不能把需要用户/Architecture决定的选择留给 Writer。

## Coverage 与 residuals

维护双向关系：

```text
source obligation → Epic → Story → AC → oracle/evidence → consumer
Story/AC/evidence → obligation/source/decision/owner
```

每个 obligation 只能有一个 primary Story disposition：implemented now、dependency、deferred、excluded、superseded。MERGE 可共享一个 Story/AC，但保留全部 source IDs。任何 exclusion/defer 需 authority reason、owner、revisit/non-trigger condition。

## Failure / stop

以下情况不得 READY：requirements inventory/input set 未确认、任何 `UNKNOWN`/unmapped/source conflict/遗漏、cycle、branch-only prerequisite、duplicate obligation、forward dependency、Story 首次决定高层 contract、owner/lifecycle跨越、unbounded evidence、missing direct consumer、capacity 任一 FAIL/UNKNOWN。

若 split 会改变 delivery semantics、MVP 或 user exposure，展示 2–3 个 viable slice、trade-off、推荐、ripple 和 user owner；普通技术 sequencing 不需要新产品决定。

## 输出与 handoff

产出 confirmed input inclusion/exclusion、confirmed requirements inventory、Epic inventory、Story contracts、DAG nodes/edges、obligation coverage、owner/lifecycle map、non-goals/residuals、capacity results 和 blockers。`CAPACITY_PASS` 只解锁 F7 contract/evidence closure 与 F8 readiness；它本身不是 `READY`，也不授权 implementation。
