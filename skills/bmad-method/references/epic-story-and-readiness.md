# Epic、Story 与 Implementation Readiness

## When to load

当需要创建/更新 Epic/Story、验证分解、检查 ordinary implementation readiness，或准备一个 exact ready Story 时加载。本 reference 是 implementation readiness node 的唯一 direct owner，可单独完成检查并给出 verdict；若 Product/Scope、适用 UX 或 Architecture 仍有 phase blocker，先返回对应 node，不用 Story AC 吸收上游决定。

## Goal

把已关闭的产品、UX 与 Architecture 合同分解为按用户价值组织、依赖方向正确、单 Writer 可因果完整实施、单 Reviewer 可独立判定的 Story；在编码前独立完成 ordinary implementation readiness，同时证明 candidate 内部一致性与来源世界的外部完整性。

## Inputs

- Accepted PRD/scope ledger、current-Epic done state、post-Epic residual map。
- Applicable UX journey/surface/state/component/accessibility/chart/mock contracts。
- Architecture spine、data contracts、inherited invariants、owner/evidence boundaries。
- Original accepted sources、domain concerns、downstream consumers。
- 当前 Epic/Story candidate、shared state 与项目 delivery/evidence constraints。

## Collaboration mechanics

### 1. Prerequisite gate

开始 CE 前先核验：

- 所有 `CURRENT` capabilities 与 current-Epic done state 已由用户接受；
- applicable UX/visual/chart closure 完成；
- Architecture 的 applicable dimensions 无阻塞 `OPEN`；
- source conflicts 与 phase blockers 已关闭；
- residual map 明确 current 之外的能力去向。

任一失败就把 `currentNode` 指向正确上游，记录恢复 CE 的 `firstUnfinishedAction`，停止分解。CE 不首次决定 product/UX/core architecture/owner/data responsibility。

### 2. Requirements inventory

从所有 confirmed inputs 提取并保留原 identity：FR、NFR、UX requirements、Architecture/invariant requirements、data/evidence/human gates、domain concerns、non-goals、residual capabilities。每项给 stable ID、source、classification、适用 Epic 与 coverage status。

先展示完整 inventory 和 input inclusion/exclusion；让用户纠正遗漏。只有 inventory 确认后才设计 Epic structure。Source preservation 包含定性意图、rejected/future language，不只 numbered FR。

### 3. Epic structure

Epic 按完整用户结果组织，不按 database/API/UI 等技术层。对每个 Epic 记录：

- named user outcome 与 done state；
- covered requirements/surfaces；
- prerequisites 与 material dependencies；
- owner/risk boundary；
- non-goals 与 residual effect；
- evidence/readiness signal。

每个 Epic 只能依赖先序 Epic，不能要求未来 Epic 才产生当前价值。展示 requirements coverage 与 dependency direction，让用户明确接受整个 Epic structure；机械保存无需每页 `Continue`，但结构本身是 load-bearing decision。

Epic identity 与 Story suffix 分开解析。当前 Epic 含 `E17-18` 时仍要检查独立 `E18` 或 residual `UNKNOWN / NOT_DISCOVERED`，不能从名称推断下一阶段已规划。

### 4. Story contract

逐 Epic 分解。每个 Story 至少包含：

| 字段 | 要求 |
|---|---|
| identity/title/value | 用户/业务/治理价值，稳定 ID |
| objective / old→new | 可观察的状态改变 |
| scope / exact affected contract | 最小但因果完整的生产变化集合 |
| non-goals | 明确不吸收 future 或相邻功能 |
| prerequisites | 只指向 immutable、已完成或先序 identity |
| owner/lifecycle/data | 采用上游决定；不能临场发明 core owner |
| acceptance criteria | 具体、可判定，含 applicable failure/state/boundary |
| evidence / human gate | 自动、人工、UI/device/external 的真实层级 |
| capacity | 一个 Writer 可完成，一个 independent Reviewer 可判定 |

Story 不得依赖未来 Story；若自然顺序无法消除 forward dependency，重排或拆分。数据库/entity 只在首次需要它的 Story 建立，不做无用户价值的“大预建”。若单 Story 需改变多个 core ownership/module boundaries，回到 Architecture/Correct Course。

### 5. Coverage 与 dependency checks

建立双向 coverage：requirement→Epic→Story/AC/evidence，Story→sources/requirements。检查：

- all `CURRENT` FR/NFR/UX/Architecture/data/evidence coverage；
- source preservation 与 qualitative intent；
- journey→surface→Story 与 mock/spine-only coverage；
- dependency direction、DAG、forward dependency、orphan；
- post-Epic residual map 未被 Story universe 清零；
- owner/lifecycle/error/evidence 在 Story 间一致。

路径计数、DAG 无环或 coverage 声明只能证明内部机械结构，不能关闭外部 phase blocker。

### 6. Implementation Readiness：外部重建优先

进入 ordinary readiness 时设置 `currentNode = implementation_readiness`。First action 不是信任 candidate 的 inventory，而是从以下来源独立重建 expected inventory：

1. original accepted sources 与每项 load-bearing claim；
2. named users/journeys 及其完整结果；
3. IA surfaces、states、components、chart/visual/mock contracts；
4. domain concerns 与 operational boundaries；
5. downstream consumers、owners、data/evidence responsibilities；
6. current-Epic done state 与 residual capabilities。

再对照 candidate 的 PRD/UX/Architecture/Epic/Story，识别“没有写进 candidate 因而内部 coverage 看不见”的缺失，并在本 reference 内完成全部适用轴：

1. expected inventory 与 candidate coverage、scope classification、phase blockers、source preservation；
2. requirements→Epic→Story/AC/evidence 与反向 traceability、Story value/capacity、dependency/DAG/forward dependency；
3. UX journey/surface/state/component/accessibility，以及 visual/chart/mock coverage；
4. Architecture divergence/inherited invariants，以及 data、owner/lifecycle、error/invariant、operational responsibility；
5. automated/human/UI/device/external evidence 层级是否真正证明 AC；
6. immutable identity、permission、Git/index 与 protected state（项目合同要求时）。

发现一个 finding 只阻止 `READY`，不停止其余适用轴；只有 objective authority、safety 或 claim-proving evidence blocker 使剩余检查不可能时才提前停止，并列出未检查内容。完成后一次输出完整 atomic findings batch，每项包含 severity、source/evidence、affected artifact/consumer、影响、minimum causally complete correction 与是否 structural/load-bearing；无 finding 且全部适用轴有证据才给 `READY`，否则给 `NOT_READY`/项目规定的等价 verdict。不得加载或转交给 Validation reference 来完成 ordinary readiness。

### 7. Advanced Elicitation checkpoint

Requirements inventory 与 Epic structure 各形成候选时，可按风险做 stakeholder lens、boundary sweep、map-is-not-territory 或 pre-mortem。尤其问：什么 source/journey/surface/residual 没有被当前 universe 承载？先展示发现，用户接受后才改结构。

## State and output

沿用项目 Epic/Story/readiness artifacts，至少包含：confirmed inputs、requirements inventory、Epic list/done states、coverage maps、dependency graph、Story contracts、residual map、readiness expected inventory、完整检查轴、atomic findings batch/verdict 与 shared state。每个 ready Story 有 immutable identity 与可独立判定证据。

## Blockers

- Product/Scope、applicable UX/chart/mock 或 Architecture 有 phase blocker；
- requirements inventory/input set 未确认；
- Epic structure 或 current/future boundary 未由用户接受；
- Story 首次决定 product/UX/core owner/schema/data responsibility；
- forward dependency、orphan、uncovered current requirement 或 source-preservation gap；
- Story 超出单 Writer/Reviewer capacity；
- evidence 层级不能证明 AC。
- readiness authority/candidate identity/claim-proving evidence 缺失，且导致尚未检查的适用轴客观无法完成。

## Completion / readiness checks

- prerequisite gate 全部通过；requirements inventory 与 Epic structure 已由用户接受；
- Epics 按用户价值、顺序独立，current done state 与 residual map 保持；
- 每个 Story 有完整 contract、只依赖先序 identity、capacity 合理；
- requirements/source/journey/surface/mock/architecture/evidence 双向 coverage；
- readiness 从 sources/journeys/surfaces/concerns/consumers/residual 独立重建预期，而非只审 candidate；内部 traceability、Story/dependency、UX/chart/mock、Architecture/data/owner、evidence 与适用 identity/protected state 均已检查；
- finding 没有提前终止其余轴；完整 atomic findings batch 与 `READY`/`NOT_READY` verdict 已在本 reference 内产生，没有加载或转交 Validation reference；
- 无 phase blocker；机械 gate 没有替代产品/UX完整性；
- 到一个 exact ready Story 后设置 `firstUnfinishedAction = return to project management`，遵守 `MANUAL_RELAY`，不进入实现或自动派发角色。
