# F8 — Implementation Readiness、Exact READY 与 Manual Handoff

## 何时加载

一个 exact Story candidate 已完成 F3–F7，拟进入 implementation；或需要判断它为何不是 READY、恢复 interrupted readiness、生成完整 manual Writer handoff 时加载。

F8 是 ordinary readiness 的直接 owner。不要把 ordinary readiness 路由给 F9；F9 是 fresh independent Planning Review。F8 不实施、不派发、不创建 Writer/Reviewer。

## Accepted inputs

- identity-bound Product/UX/Architecture/Epic/Story/DAG/obligation/evidence artifacts；
- exact Story identity、base、immutable prerequisite identities 与 ancestry/acceptance proof；
- allowed/protected paths/actions、permissions、environment constraints；
- commands/oracles、human/device/external gates 与执行时机；
- host accepted formal Dev template 和 manual relay policy；
- current PlanningState、approval 与 protected state。

任一 identity/path/template 不唯一或相互冲突时 fail closed；不要寻找近似文件或补占位符。

## T6 — Readiness axes

先从 accepted sources 重建 expected obligations，不信 candidate 自己的 coverage。完成所有适用轴，发现 finding 后继续其余轴，一次形成 atomic batch：

1. **Authority/identity**：source、decision、candidate、base、prerequisite、template 固定且一致。
2. **Product/scope**：objective、old→new、current/future/non-goal/residual 无 pending承重语义。
3. **UX/human**：journey/surface/state/accessibility/human gate 完整且时机明确。
4. **Architecture/feasibility**：exact version/API 可行；owner/lifecycle/data/error/consumer 稳定，无第二 authority。
5. **Story/DAG/capacity**：prerequisite immutable/acyclic；十项 capacity 全 PASS。
6. **Contract/coverage**：source→consumer forward/reverse 闭合；required/forbidden/identity/binding/order/atomic/state/error 无 loss。
7. **Evidence**：每个 AC 有 matching-layer independent oracle；human/external boundary 不被 fake替代。
8. **Environment/permissions**：Writer 可执行必要 mutation/validation；禁止动作明确。
9. **Git/protected state**：base/ref/worktree/index/scope policy 按 host contract 可证明。
10. **Handoff completeness**：formal template、literal paths、terminal report、post-READY owner boundary 均可零歧义填写。

只有 objective authority/safety/evidence blocker 使剩余轴客观无法检查时才能停止；列出未检查轴和最小恢复条件。

## T6 转换闭合合同

- **Input authority**：immutable T1–T5 outputs、accepted F3–F5 contracts、Story/DAG、environment/permissions、host manual template 与 approval state。
- **Output schema**：exact Story identity、coverage totals、capacity、prerequisites、allowed/protected scope、commands/evidence/human gates、unknowns、verdict、zero-placeholder relay。
- **Coverage/invariant**：required rows闭合、forward/reverse对账、feasibility current、capacity PASS、environment sufficient、final approval explicit、handoff identity固定。
- **Allowed loss**：optional/deferred且无Writer consumer的artifact可不进入上下文；rationale可摘要但operative clause不能丢。
- **Forbidden loss**：load-bearing unknown、unresolved decision、missing consumer/oracle、stale prerequisite、placeholder、implied approval、Planner自评作证。
- **Unknown**：`NOT_READY`，列全部 atomic findings 与 first unfinished axis；candidate与accepted READY identity分开。
- **Checkpoint**：用户明确接受完整 planning candidate；早先 `Continue` 无效。
- **Failure terminal**：`NOT_READY / HANDOFF_FORBIDDEN / IDENTITY_DRIFT`。
- **Downstream/oracle**：T7消费；需要时F9/T8独立验证；source-world Review + mechanical identity/coverage，hash存在不够。

## Readiness verdict

- `READY_CANDIDATE`：全部轴证据闭合，但 final planning approval 尚未给出。
- `READY`：全部轴闭合、capacity PASS、用户明确接受 final planning、exact identity 固定。
- `NOT_READY`：一个或多个 finding/unknown/conflict；给 atomic batch 和 first unfinished axis。
- `BLOCKED`：控制 authority/identity/evidence 客观缺失，无法完成必要轴。

Artifact/hash存在、Planner自评、测试文件存在、此前“全部完成”、聊天继续或 blanket instruction 不能产生 READY。

## C25 planning-status readiness gate

Tracking适用时，F8消费 F1 planning-status owner 的 fresh read-only `validate`/dry-run report，并把它与 F6 canonical identity/order contract对账：missing/duplicate/illegal/legacy/orphan/unrecognized、unexpected downgrade、write identity与validation结果必须可见。机械结构为 valid 只证明 tracking artifact；F8仍从 accepted sources独立判断 semantic readiness。

需要 status semantic repair时，F8返回最早 F/T gap或 F10，不自行改 status。只有 source-backed proposal经用户确认后，F1 mechanical owner才执行显式 mapping/status/disposition与atomic write；ambiguity、corruption、permission或validation failure保持 `NOT_READY/BLOCKED`。

## Final planning checkpoint

向用户展示 exact candidate identity、所有 readiness axes、accepted decisions、remaining non-blocking unknowns、Story capacity、evidence/human gates、protected state、post-READY route 和 handoff内容。只有用户明确接受当前展示的 final planning 才将 `READY_CANDIDATE` 变成 `READY`。

`Continue` 只在明确展示“批准 final planning 并进入 handoff”为下一 step 时有效；不能接受从未展示的产品/UX/Architecture选择。

## T7 — Complete manual Writer handoff

READY 后由主管理依据 host accepted Dev template 生成一个完整 outer block，必须含：

- role 与 planning-only→implementation boundary；
- exact accepted base、Story/candidate/prerequisite identities；
- literal accepted source/template/artifact paths；
- allowed paths/actions、protected state 与禁止事项；
- implementation objective 只引用 immutable Story，不零散改写；
- required validation/evidence/human gates 与 exact commands（若已授权）；
- Git/commit/push/merge policy；
- stop-on-missing-decision 与 return-to-management contract；
- terminal report schema；
- zero unresolved placeholders；
- user-facing runtime recommendation（host要求时）。

BMAD 展示完整 handoff 后停止；用户手工复制到 fresh Writer conversation。BMAD 不调用 native collaboration/thread dispatch，不自动创建角色，不执行 handoff内容。

## T7 转换闭合合同

- **Input authority**：T6 exact READY package、host accepted complete Dev template 与 manual relay authority。
- **Output schema**：一个完整、简体中文、零占位 outer block，含 exact identities/paths/actions/protected state、TDD/debug/verification route、evidence/human gates、terminal/runtime footer。
- **Coverage/invariant**：relay引用 immutable Story而不重写合同；占位全解析；allowed/prohibited/stop明确；BMAD展示后停止。
- **Allowed loss**：Story中可直接读取的 rationale不重复；runtime recommendation按任务选择。
- **Forbidden loss**：new scope/decision、freehand substitute、split prompt、native dispatch、BMAD implementation、Writer补planning gap。
- **Unknown**：承重 gap使 READY失效并返回T2–T6；literal path/prompt assembly error可修正但不改变Story。
- **Checkpoint**：用户手工复制完整 prompt；没有自动 role creation/dispatch。
- **Failure terminal**：`WRITER_HANDOFF_BLOCKED / READY_INVALIDATED / MANUAL_RELAY_REQUIRED`。
- **Downstream/oracle**：fresh Writer按 accepted implementation workflow执行并回主管理；结构/template检查 + fresh behavior证明BMAD停、Writer拒绝承重gap。

## Post-READY executable boundary

Handoff 必须明确：

1. BMAD 到 exact READY + complete manual handoff 为止，绝不实施代码。
2. Writer 只实施 accepted Story；行为改变先使用 host/project-local TDD。
3. 只有观察到 failure/unexpected behavior 后才使用 systematic debugging；不能以 debugging 重开产品或 Architecture。
4. Writer 声称完成、commit 或交付前使用 verification-before-completion，并报告原始 evidence。
5. Writer 不补 product、UX、Architecture、Story、owner 或 evidence oracle 决定。
6. Fresh code Reviewer 只使用 host accepted code-review template，不加载 BMAD 或 implementation workflow skills，不成为第二 Planner。
7. Code Review 中原 Story 明确承载的 implementation defect 可由管理路由 ordinary Repair；Story 遗漏既有 accepted planning obligation 必须 `PLANNING_ESCAPE` 回 BMAD。
8. Writer/Reviewer 不自动创建 subagent；任何独立角色由主管理生成完整 prompt，用户手工 relay。

如果 host 使用不同命名但等价的 TDD/debug/verification/template contract，引用其 accepted local identity；不得把外部/global package 当项目 authority。

## Writer intake failure

Writer 声称缺决定时，修改代码前停止并报告缺项、影响、已查 sources 与 terminal。

- 答案已在 immutable accepted source：修正引用/完整 prompt 后重新 manual relay；不以聊天零散补充改变 Story。
- 确实缺少 product/UX/Architecture/owner/evidence 决定：READY 失效，返回最早 F/T 或 Correct Course，形成新的 exact READY identity 后才再 handoff。

在新 accepted decision 和 Story identity 形成前不得 implementation、Repair 或 Review。

## Handoff success oracle

Fresh Writer 只读 handoff 与 identity-bound Story 就能：定位 exact base/scope、知道允许/禁止动作、无需做承重决定、运行正确 evidence、在 blocker 时返回、在完成时给可判定 terminal。Prompt存在或零 placeholder 的机械检查只证明格式；部署前必须以 fresh behavior 证明边界。

## Failure / stop

任何 readiness gap、unresolved placeholder、identity conflict、over-capacity、unproven feasibility、self-oracle、missing consumer/permission/human gate 都禁止 relay。完整 handoff 展示后终态为 `STOPPED_AT_READY_HANDOFF`，`currentNode=manual_writer_relay_by_user`，`firstUnfinishedAction=等待用户在fresh Writer conversation手工relay并回传terminal`。
