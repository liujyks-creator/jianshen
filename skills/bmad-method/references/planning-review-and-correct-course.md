# F9–F10 — Independent Planning Review、Consistency Audit、Correct Course 与 Planning Escape

## Intent routing

- `F9 Planning Review`：fresh independent Reviewer 从 source world 反向审查 exact planning candidate。
- `F9 Consistency Audit`：审计 Product→UX→Architecture→Story/AC/evidence 的传播与反向一致性。
- `F10 Bounded planning Repair`：修 accepted Story/规划合同已经承载的 bounded finding batch，不新增承重决定。
- `F10 Correct Course`：accepted reality、scope、Architecture、owner/data/evidence 或 Story boundary 需结构改变。
- `F10 PLANNING_ESCAPE`：implementation Review 证明既有 accepted obligation 在 T1–T6 传播中丢失。

Ordinary readiness 属 F8。Code Review 属 host 的 T9，不由本 reference 执行。

## F9 manual independence contract

主管理固定 accepted sources、candidate/base identity、scope、evidence、protected state 与完整 Planning Review template/prompt；用户手工 relay 给 fresh Reviewer。Candidate Planner 不自审，不创建/派发 Reviewer。

Fresh Reviewer：

1. 不信 Planner coverage/inventory/verdict；先从 accepted sources 独立建立 expected obligations。
2. 绑定 exact base/candidate delta 与 current authorized node；不扩到 whole repository/history。
3. 完成所有适用 axes；一个 finding 阻止 PASS，但不提前停止其他轴。
4. 不编辑、实施、修复、merge、派发或共同设计 candidate。
5. 一次返回完整 atomic findings batch；objective blocker 时列 unreviewed axes 和最小恢复条件。

执行者只得到真实 review request、candidate skill/artifacts 和完成 Review 的必要 sources；不泄露预期答案、已知 bug 或修复建议。Scoring rubric 与 expected invariants由独立 authority持有。

## T8 — Planning validation axes

Source-first expected inventory 建立后至少检查：

- authority、scope/classification、source preservation；
- Product requirements、residuals 与承重 acceptance；
- UX journey/surface/state/accessibility/visual/human gates；
- Architecture feasibility、owner/lifecycle/data/error/security/operation；
- Epic/Story value、DAG、capacity、prerequisite；
- obligations、AC、independent evidence、direct consumers；
- forward/reverse trace 与 exclusions；
- applicable identity、permissions、Git/protected state；
- state lifecycle 与 merge-stable text。

Finding eligibility 必须有 accepted source/decision、外部可观察差异、持久化/迁移/API/security/core ownership影响，或使 Story objective/AC/evidence 客观不可实施/不可判定。普通局部算法、函数拆分、测试组织和已在 accepted owner内的可逆选择不是 planning must-fix。

## T8 转换闭合合同

- **Input authority**：exact accepted sources、planning candidate identity/scope/protected state 与 accepted manual planning-Review contract。
- **Output schema**：independent expected inventory、axis results、forward/reverse coverage、atomic findings、`PASS/NON-PASS/BLOCKED`、objective blocker下的unreviewed axes。
- **Coverage/invariant**：source-first、全部适用轴、finding后继续、不编辑、不信Planner coverage。
- **Allowed loss**：同一根因finding可合并但保留全部 obligations/consumers；unrelated repository history排除。
- **Forbidden loss**：Planner self-review、finding-first exit、unrelated audit、candidate edit、missing source axis静默PASS。
- **Unknown**：objective blocker→`BLOCKED`+最小恢复；普通ambiguity作为finding，不协同设计。
- **Checkpoint**：没有candidate设计checkpoint；用户只relay，management在verdict后路由。
- **Failure terminal**：`INDEPENDENCE_INVALID / REVIEW_INCOMPLETE / NON_PASS / OBJECTIVE_BLOCKED`。
- **Downstream/oracle**：PASS回host/readiness gate，NON-PASS到F10；fresh conversation最小上下文，scoring oracle独立持有。

## Planning Review 输出

```text
review role + exact node
accepted/base/candidate identities
independently derived expected inventory + coverage totals
axes completed / objectively unreviewed
atomic findings: severity, source clause, affected artifact/consumer,
  consequence, minimum causal correction, structural/load-bearing flag
verdict: PASS | NON-PASS | BLOCKED
no-edit/no-integration/protected-state evidence
currentNode + firstUnfinishedAction
```

PASS 只证明当前授权 planning node。NON-PASS 后 Reviewer不决定 Repair/Correct Course；返回主管理/用户。

## Consistency Audit

从每个 accepted claim 向下追到 UX surface/state、Architecture/data owner、Epic/Story、AC/evidence/consumer，再反向到 source。检查丢失、冲突、重复 owner、classification漂移、stale identity、evidence不能证明claim，以及 merge 前后失真的状态表述。

Planning Review 与 Consistency Audit 的 scope、oracle 和 verdict 分开；一个 PASS 不能代替另一个。只有 host/accepted contract 要求时才运行 Audit。

## F10 classification

收到完整 finding/change signal 后，先固定 chronology、candidate identities、accepted sources 和 T1–T6 trace，再分类：

- `BOUNDED_PLANNING_REPAIR`：finding 已由原 planning contract明确承载；无新产品/UX/Architecture/owner/data/evidence决定；在授权 scope内可用既定oracle验证。
- `CORRECT_COURSE_REQUIRED`：改变 scope/Epic/Story boundary、core Architecture、owner/lifecycle/data responsibility、evidence oracle 或跨未授权模块。
- `PLANNING_ESCAPE`：implementation/code Review 证明实现前 already-accepted load-bearing obligation 未进入 Story/AC/evidence。
- `CLASSIFICATION_BLOCKED`：provenance/identity不足，不能判断是 implementation defect 还是 planning loss。

Patch大小或 Review次数本身不决定分类。

## Bounded planning Repair

锁定 complete atomic batch、current candidate、direct sources，并标记 unaffected contracts preserved。修改前做 bounded adjacent-omission scan：同 producer/owner、state family、boundary/error class、direct consumers、coverage/AC/evidence chain。

一次修完整 batch 和同根因 adjacent omissions；不重开 unaffected world、不重问 accepted决定、不降低 oracle。没有新承重决定时无需额外批准循环。Repair后绑定新 candidate identity，fresh applicable Planning Review 覆盖 batch、共同根因、direct consumers 和实际变化的相邻合同；不以 Writer自评代替。

若 batch涉及 planning status，F10只决定 semantic classification、source-backed old→new与需要用户确认的 mapping/status/disposition；F1仍是唯一 mechanical generate/view/validate/atomic-write owner。F10不得手改 tracking、复制第二套 key/order/merge算法，或让脚本判断 planning completeness。

## Planning-only retrospective（C35；F10唯一 method owner）

当 completed或failed work产生跨 Story、跨 Review 或跨planning-transform的可复用lesson时，F10可运行 planning-only retrospective。它不是delivery retrospective、自动修复器或sprint lifecycle owner；F7提供 evidence chain，F9提供独立 source-first验证纪律，二者不复制本方法。

### Gather 与 evidence boundary

固定 exact accepted spec/criteria、Story identities、full diff/range、per-Story commits、tests/runtime evidence、Review batches与可用session logs。F7 inventory逐项记录 `AVAILABLE / MISSING`、identity与checked scope；每个分析轴声明需要的evidence并输出 `CHECKED_CLEAN / FINDING / NOT_CHECKED`。diff range无法包含第一项、log不存在或runtime未执行时明确窄化，绝不能把 never checked报告为clean。

### Analyze 与 reconcile

只对完整evidence执行适用的 aggregate views：cross-Story Architecture/owner delta、duplication、size/complexity growth、pattern divergence、verification gap及 spec-to-implementation reconciliation。每个finding必须引用 file/line、commit、diff、test或log；source不支持的root cause/模式直接丢弃。实现偏离spec时区分 implementation defect、accepted deviation与待用户确认的spec reconciliation，不自动选择。

从成立的finding提炼 prevention lesson：指出最早可防止它的 F1–F10/T1–T10 gate或owner、需要改变的通用method/oracle及source evidence。项目名称、Story编号、历史token或单次修复不能冒充通用lesson；同一lab/model自评或candidate inventory不能证明预防有效。

### Human disposition、verdict 与 output

向用户展示每个finding的两个独立disposition：本次 `fix now / defer / accept as-is`，以及未来的source-backed prevention proposal。Action item必须有owner、source、affected planning gate与验收信号；remediation/spec reconciliation都只是proposal，只有human authority可批准应用。

Acceptance verdict针对declared criteria；没有declared criteria时可从accepted spec/diff profile，但必须标 `PROFILED`。输出 `ACCEPTED / ACCEPTED_WITH_OPEN_ITEMS / REJECTED / VERDICT_BLOCKED`、依据与human override/confirmation。缺少适用evidence使claim窄化；blocking criteria失败且无人决定时不得默认为accepted。恢复时从identity-bound inventory、findings与首个未完成disposition继续，不重做已完成分析。

最终产物包含 evidence inventory、checked/not-checked map、sourced findings、spec reconciliation、prevention lessons、human dispositions/action items、acceptance verdict、open questions与下一planning gate。禁止写code/spec、auto-fix、角色派发、delivery automation、sprint-status/implementation lifecycle mutation或宣称实现已经接受。

## Scoped Correct Course

建立 trigger/evidence 与 impact map：Product/scope、UX、Architecture、data/owner/lifecycle、Epic/Story/DAG、contract/evidence、status/readiness、direct/downstream consumers。

按最小必要高度分类：

- `Minor`：不改变 upstream contract 的局部规划修正；
- `Moderate`：多个 artifacts/Stories 同步，但核心方向/owner不变；
- `Major`：scope、Epic boundary、core Architecture、owner/lifecycle、data responsibility 或 evidence oracle改变。

提供 2–3 个 viable structural routes、trade-off、risk、recommendation、ripple 和 final decision owner。用户接受后形成 identity-bound old→new、preserved/non-goals、corrected obligations/Story/evidence、regression 和 restart gate；不能用零散聊天补充旧 READY Story。

## T9 — Code Review boundary（BMAD 外部）

Fresh code Reviewer 只使用 host accepted code-review template 与 exact Story/candidate/evidence，不加载 BMAD 或 Writer workflow skills，不补 product/UX/Architecture/Story/evidence，不成为第二 Planner。

Reviewer完成当前 node 全部适用轴并一次返回 atomic batch，不因第一个 finding 提前结束。Reviewer不修改、不合并 NON-PASS candidate，也不决定 Repair/Correct Course。普通 implementation defect 追到原 Story/AC；疑似 planning loss 必须给 accepted pre-implementation source 与 Story omission evidence。

## T9 转换闭合合同

- **Input authority**：Writer base/candidate identities、immutable Story/evidence、host accepted code-review template 与 protected state。
- **Output schema**：code Review verdict、完整atomic batch、exact delta/evidence/Git/protected-state；PASS integration仅服从host contract。
- **Coverage/invariant**：Reviewer不加载BMAD/implementation workflow skills，按Story审实现、完成current-node axes、不成为Planner，并区分finding provenance。
- **Allowed loss**：不把planning capability传给Reviewer；code-review evidence可摘要但identity/claim不丢。
- **Forbidden loss**：Reviewer发明产品/Architecture/evidence、加载规划或实施技能、NON-PASS编辑、把Story omission当普通code requirement。
- **Unknown**：objective Story/source ambiguity阻塞affected axis并返回主管理；Reviewer只报告provenance不重规划。
- **Checkpoint**：Review内无planning checkpoint；manual workflow与host template控制integration。
- **Failure terminal**：`CODE_REVIEW_NON_PASS / PLANNING_SOURCE_GAP_REPORTED / REVIEW_BLOCKED`。
- **Downstream/oracle**：implementation defect可进入host-authorized Repair；pre-existing omission进入T10；fresh Reviewer + exact merge-base delta/evidence，merged tree必须等于reviewed tree。

## T10 — Planning escape

立即触发条件：implementation/code Review 已证明一项 load-bearing obligation：

1. 在 implementation 前的 accepted source 中已存在；
2. 对当前 candidate/consumer适用；
3. Story/AC/evidence 未承载；
4. 不是 Reviewer新发明的产品或 Architecture要求。

满足即输出 exact token `PLANNING_ESCAPE`，冻结 candidate、禁止 ordinary Repair/integration，定位最早失败转换：

- `T1_FAILURE`：source clause/qualifier 未进入 obligation inventory；
- `T2_FAILURE`：decision height/acceptance/ripple丢失；
- `T3_FAILURE`：owner/lifecycle/feasibility未稳定；
- `T4_FAILURE`：obligation未进入正确 Story/DAG/capacity；
- `T5_FAILURE`：AC/oracle/consumer/evidence传播丢失；
- `T6_FAILURE`：readiness在 gap 存在时错误放行。

若连续两次完整 implementation Review 都发现新的、此前已存在的承重 obligation，必须 `PLANNING_ESCAPE` 并停止 patch loop；Review count只是强制检查信号，核心原因仍是 proven planning loss。局部 Repair不能通过跨越 core responsibility/multiple module boundaries吸收遗漏。

Correction 顺序：

```text
freeze implementation candidate
→ preserve complete Review batches and chronology
→ rebuild affected source obligations plus adjacent lineage
→ repair earliest failed T1–T6 transform
→ propagate through all dependent transforms
→ create corrected identity-bound Story/evidence/state
→ add generic regression
→ F8 readiness + required fresh F9 gate
→ new complete manual handoff
```

不能只把 final AC加长，也不能把历史 failure token、项目路径或产品名写进通用 regression。

## T10 转换闭合合同

- **Input authority**：complete implementation Review chronology、exact accepted pre-implementation sources、original T1–T6 artifacts、candidate/Repair identities 与 finding provenance。
- **Output schema**：`PLANNING_ESCAPE` report，含escaped obligation/source、chronology、failed transforms、owners/Stories/consumers/evidence、frozen candidate、corrected authorization、generic regression、restart gate。
- **Coverage/invariant**：proven pre-existing Story omission立即回流；连续完整Reviews的新既有遗漏强制停止patch loop；每条escape定位earliest failed transform并传播到全部污染artifact。
- **Allowed loss**：原Story已明确承载的implementation-only defect不算escape；同根因findings可共享transform但不能丢consumer。
- **Forbidden loss**：称为新要求、ordinary Repair、只修末端artifact、丢Review chronology、project-specific regression、自动重跑全规划。
- **Unknown**：provenance未证明则`UNKNOWN/CONFLICT`与`PROVENANCE_BLOCKED`，不得猜token。
- **Checkpoint**：classification由evidence决定；只有多个structural ownership/scope routes时用户选择。
- **Failure terminal**：`PLANNING_ESCAPE / CORRECT_COURSE_REQUIRED / PROVENANCE_BLOCKED`。
- **Downstream/oracle**：回earliest T1–T6后重跑dependent transforms、F8/T8、manual T7；independent source/history reconstruction + generic regression证明token/localization/no-dispatch/no ordinary Repair。

## Merge-stable state

Status/decision/readiness text 应引用 immutable identity，或写成 merge 前后都真的条件式事实，例如“若 reviewed candidate 成为 accepted branch 的祖先，则解锁 X”。不得要求 Reviewer PASS/merge 后再编辑同一 reviewed candidate 才让状态正确。

## Failure / stop 与 recovery

- provenance 不足：`CLASSIFICATION_BLOCKED`，列 competing evidence；不猜 `PLANNING_ESCAPE`。
- finding batch 不完整：不开始 Repair；等待完整 batch。
- Repair 需要新 owner/interface/data/evidence decision：停止并 `CORRECT_COURSE_REQUIRED`。
- fresh Reviewer 缺 controlling authority：`BLOCKED`，列 unreviewed axes；不伪PASS。
- compaction 后恢复完整 batches、chronology、failed transform、approval 与 first action；摘要不授权 Repair。

F10成功时输出 classification、impact/finding map、failed transforms、accepted old→new、preserved boundaries、corrected artifacts、generic regression、restart gate、`currentNode` 与 `firstUnfinishedAction`。只有新 exact candidate重新通过适用 F8/F9，才可再次 handoff；F10本身不实施或派发。
