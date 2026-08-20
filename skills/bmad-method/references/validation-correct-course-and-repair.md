# Validation、Correct Course 与 Post-validation Repair

## When to load

当用户要求 Planning Review、Consistency Audit、Correct Course，或一个 approved planning candidate 收到完整 Review/Audit findings batch 需要 Repair 时加载。Ordinary implementation readiness 只加载 `epic-story-and-readiness.md`，不得为该 intent 加载本 reference；不要把普通 create/update 误路由为 Review，也不要把 bounded Repair 重启为 Discovery/CE/Correct Course Step 1。

## Goal

独立发现 candidate 内外的完整问题，一次返回 atomic findings batch；对结构性变化做 scoped Correct Course；对 bounded findings 做最小但因果完整 Repair，并严格执行 fresh Review → fresh Audit 门禁。

## Inputs

### Review / Audit

- exact base/candidate identity、当前授权 node 与 review contract；
- original accepted sources、decisions、PRD/scope、UX、Architecture、Epic/Story、data/evidence；
- current-Epic done state、residual map、domain concerns、downstream consumers；
- applicable Git/permission/protected-state evidence（由项目合同决定）。

### Correct Course

- clear trigger、success criteria、reproducible evidence；
- current/future Epic/Story、PRD、UX、Architecture、data、evidence 与直接 ripple。

### Repair

- current candidate；
- complete identity-bound atomic finding artifact；
- only directly affected accepted sources 与 preserved unchanged contracts。

旧 self-review/verdict 只能作为历史证据，不继承结论。

## Collaboration mechanics

### 1. 区分门禁

- **Planning Review**：在 ordinary implementation readiness 之外独立判断当前授权 planning candidate 的 quality、external completeness、Story readiness 与 evidence sufficiency。
- **Consistency Audit**：判断 Product→UX→Architecture→data→Story/AC/evidence 的传播、矛盾、owner/identity 一致性。
- **Correct Course**：处理 trigger 改变 scope/Epic/UX/Architecture/ownership/data 等规划结构。
- **Post-validation Repair**：只修完整 bounded findings batch，不重新规划 unaffected world。

不得用一个门禁的 PASS 替代另一个。

### 2. Planning Review

First action：不看 candidate coverage 声明，先从 accepted sources、named journeys、surfaces/states/components/chart/mock、domain concerns、downstream consumers、done state 与 residual map 独立写 expected inventory。对每项记录 source、expected artifact location、consumer/evidence。

然后完整检查当前授权 node：

1. expected inventory 与 candidate 实际 coverage；
2. scope classification、phase blockers、source preservation；
3. UX journey/surface/state/component/accessibility/visual/chart/mock closure；
4. Architecture divergence、inherited invariants、owner/data/error/operational dimensions；
5. Epic/Story value、capacity、dependency、AC/evidence；
6. identity、permission、protected state（适用时）。

Candidate 的 DAG/hash/path count/AC 自洽不能消除 expected inventory 的缺失。发现一个 finding 后继续所有剩余适用轴；只有 objective authority/safety/claim-proving evidence blocker 使余轴无法执行时提前停止并说明未检查内容。

一次输出完整 atomic findings batch：severity、source/evidence、affected artifacts/consumers、why it matters、minimum causally complete correction、是否 load-bearing/structural。无 findings 且所有适用轴有证据才 PASS。

### 3. Consistency Audit

从每项 accepted product/scope claim 向下追踪到 UX surface/state、Architecture/data owner、Epic/Story、AC 与 evidence，再反向从 Story/owner/data/export/consumer 回到来源。检查丢失、冲突、重复 owner、分类漂移、evidence 不能证明 claim。仍要完成所有适用轴并返回一个 atomic batch。

### 4. Scoped Correct Course

确认 trigger 与 evidence；不清楚或缺必需 PRD/Epic authority 时阻塞。建立 impact map：current/future Epic、Story/DAG、PRD/scope、UX、Architecture、data、evidence/readiness，以及每条 direct/downstream ripple。

区分：

- `Minor`：不改变 upstream contract 的局部调整；
- `Moderate`：需要 backlog/多个 artifact 同步，但不改变核心方向；
- `Major`：改变 overall scope、Epic boundary、core Architecture、ownership 或 data responsibility。

展示 direct adjustment、rollback/revert、scope/MVP review 或真正适用路线的 trade-off、风险、推荐；用户选择 load-bearing 路线后，产出逐 artifact old→new、non-goals、AC/evidence 与 handoff。局部 technical finding 不自动重开全部规划，只提升到它真正影响的最低 planning 高度。

### 5. Bounded post-validation Repair

Repair first action 是锁定 current candidate + complete findings batch + directly related sources，并把 unaffected accepted decisions/contracts/evidence 标为 preserved。不得重问、重推导或重验未受 delta 影响的 session/Room/coverage 等合同。

在修改前执行 **bounded adjacent-omission scan**：

1. 从每个 finding 的 claim 找同一 producer/owner；
2. 找同一 state family、boundary 与 error classifications；
3. 找直接 consumers（read model、UI、export、analytics、evidence 等）；
4. 找同一 coverage/traceability 链和相邻 AC/evidence；
5. 只把因同一根因受影响的遗漏加入本次 Repair，记录无关领域为何不受影响。

例如 interval sorting finding 需要检查同一 normalization partition、read model、export/summary consumer 与验证链是否依赖排序；它不授权重开整个产品或重新设计 SQLite。

把完整 approved batch 与 adjacent omissions 一次修完，保留 assumptions/openQuestions/deferred 与 unaffected content。没有新 load-bearing decision 时直接完成 Repair，不插入批准循环；出现唯一新承重决定时只问该决定，回答后从同一 `firstUnfinishedAction` 恢复。

### 6. Structural escalation

Repair 若需要改变 overall scope、Epic boundary、core Architecture、new core interface/platform wrapper、ownership、schema/data responsibility 或跨多个未批准 module boundaries，立即停止 local patch。记录：触发 finding、被破坏的 inherited contract、所需 planning altitude、尚未执行的修改；交回主管理决定 scoped Correct Course。不得用更长 Story AC 静默吸收。

### 7. Repair 后门禁顺序

```text
atomic Repair complete
→ fresh full Planning Review
→ only after PASS: fresh full Consistency Audit
→ only after both current-candidate gates PASS: project-defined next gate
```

Planning Review 或 Audit 失败后，下一轮仍处理该门禁返回的 complete atomic batch。不得回到 Discovery/CE/Correct Course Step 1；不得从 Audit Repair 直接跳回 Audit；不得把 Writer 自评称作 fresh independent gate。

## State and output

- Review/Audit：expected inventory、coverage evidence、完整 axes、atomic findings batch、verdict、unreviewed blockers。
- Correct Course：trigger/evidence、impact map、classification、user-owned route、old→new proposal、non-goals、handoff/readiness。
- Repair：candidate/finding identities、preserved contracts、adjacent scan、complete delta、retained shared state、next gate 与 `firstUnfinishedAction`。

## Blockers

- Review authority/candidate identity/claim-proving evidence 缺失且余轴无法继续；
- Correct Course trigger/evidence 或 required PRD/Epic authority 不清；
- Repair finding artifact 不是完整 atomic batch；
- 新 load-bearing decision 未由用户回答；
- Repair 触发 structural escalation；
- required fresh gate 尚未执行或未 PASS。

## Completion / readiness checks

- Review 从 candidate 外部重建 expected inventory，完成所有适用轴；一个 finding 没有提前终止检查；
- Planning Review 与 Consistency Audit 职责、证据和 verdict 分开；
- Correct Course impact 覆盖 current/future Epic、PRD、UX、Architecture、data、evidence 与直接 ripple，且只提升到必要高度；
- Repair 使用 current candidate + complete batch + direct sources，保留 unaffected contracts 与 shared state；
- adjacent-omission scan 覆盖同 owner/state/boundary/consumer/evidence 链，没有扩到无关领域；
- structural change 已停止并升级，没有静默塞进 AC；
- Repair 后 fresh full Planning Review PASS，再 fresh full Consistency Audit PASS；
- 未声称 independent/manual/implementation evidence 已完成，下一责任服从项目合同。
