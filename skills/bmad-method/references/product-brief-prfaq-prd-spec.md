# F3 — Product Brief、PRFAQ、PRD、SPEC 与 Scope

## 何时加载

F2 Discovery completion 已满足，需要创建、更新或验证 Product Brief、PRFAQ、PRD、SPEC，或关闭 capability/scope/current-future-residual 边界时加载。F3 拥有 accepted product semantics；不选择 Architecture mechanism。

## 选择正确产物

| Intent/uncertainty | Product mode |
|---|---|
| 概念已较清楚，需要 concise why/user/outcome/scope alignment | Product Brief |
| 概念必须从未来 customer experience 反向 pressure-test，可能被否决 | PRFAQ |
| 多 stakeholder/capability/NFR/metric 需要实现前 accepted contract | PRD |
| 任意 intent sources 需要稳定、极简、可机械保真的 WHAT kernel | SPEC |
| 已有产物遇到 accepted change signal | Update，不从零 create |
| 完整 candidate 需要 source-first quality/coverage 检查 | Validate，不共同重写 |

可串行使用，不并行制造第二 authority。例如 PRFAQ/Brief 可成为 PRD/SPEC source；下游必须保留 stable source IDs，而不是复制后失去 lineage。

## T1 — Source 到 obligation inventory

在写产物前枚举每个 in-scope source clause：

```text
obligationId
source identity + exact location/clause
modality: REQUIRED / FORBIDDEN / CONDITIONAL / OPTIONAL / DEFERRED
subject / condition / behavior / object
qualifiers: identity, binding, ordering, atomicity, transition, temporal, version, error
direct consumer or unresolved consumer
classification: PRESERVE / ADAPT / MERGE / REFERENCE / EXCLUDE
status: OPEN / MAPPED / DEFERRED / EXCLUDED / CONFLICT
```

每项只有一个 primary classification。允许 loss：格式、persona、安装/presentation shell、真正重复 prose；必须保留所有 source IDs 和 semantic lineage。禁止 loss：modality、qualifier、rejected/future status、identity/binding、consumer、error/transition、范围条件。

机械对账：source clause 总数 = dispositions；non-excluded unmapped 必须为零。语义完整性仍需 source-first Review，不能由计数或 Planner 自评证明。

## T2 — Obligation 到 decision height

每个 obligation 分类为：

- `PRODUCT`：用户能做什么、结果/规则/范围/metric；
- `UX`：journey/surface/state/interaction/visual/human experience；
- `ARCHITECTURE`：稳定 owner、mechanism、boundary、version feasibility；
- `STORY_DISCRETION`：已被上层 contract 约束的普通可逆实现细节；
- `DEFERRED/EXCLUDED`：有 authority reason、owner 和 revisit/non-trigger proof。

低层不能接受高层决定；推荐/默认不等于 accepted。重复问题可合并，但保留所有 obligation IDs。

## T1–T2 转换闭合合同

### T1

- **Input authority**：用户 accepted decisions、host/root authority、identity-bound accepted planning sources，按 `SKILL.md` authority 排序。
- **Output schema**：上文 stable obligation rows。
- **Coverage/invariant**：每个 in-scope clause 恰有一个 disposition，保留 exact lineage/qualifier，reverse lookup 回原 clause。
- **Allowed loss**：非规范格式、重复 presentation、persona/installer shell 和不承重 prose order；必须保留分类与 lineage。
- **Forbidden loss**：modality、conditional/forbidden、identity/binding、ordering、atomicity、transition/error、version/temporal、rejected/future、consumer、provenance。
- **Unknown**：row 保持 `UNKNOWN/CONFLICT` 且不计 covered；事实 gap 到 F2，authority conflict 到 F1/user。
- **Checkpoint**：只由用户决定 authority 无法解决的承重 inclusion/exclusion/conflict；普通 normalization 无需批准。
- **Failure terminal**：`UNMAPPED_SOURCE / DUPLICATE_PRIMARY_DISPOSITION / AUTHORITY_CONFLICT_BLOCKED`。
- **Downstream/oracle**：T2/F7消费；机械计数 + fresh independent source→obligation反向审计，Planner自评不是 oracle。

### T2

- **Input authority**：T1 inventory、accepted decisions、F2 evidence 和 decision-height rules。
- **Output schema**：decision ID、obligation IDs、height/owner、alternatives/facts/trade-offs/recommendation、accepted conditions/ripple/pending。
- **Coverage/invariant**：每个 non-excluded obligation 恰有一个 height 或已证明闭合；低层不能替高层决定；accepted choice 绑定 authority identity 与 ripple。
- **Allowed loss**：语义相同问题可合并但保留全部 IDs；非阻塞 future decision 可附 owner/revisit 后 defer。
- **Forbidden loss**：Product语义沉入 Architecture、UX当技术默认、unverified framework claim、recommendation冒充accepted、conflicting owners。
- **Unknown**：保持 `PENDING` 并标 blocked consumers；最多三个真实承重问题。
- **Checkpoint**：每个新 Product/UX/load-bearing Architecture决定由用户明确接受；authority-proven fact 无需选择。
- **Failure terminal**：`UNOWNED_DECISION / HEIGHT_VIOLATION / UNAPPROVED_LOAD_BEARING_DECISION`。
- **Downstream/oracle**：F3/F4/F5→T3/T4；双向 obligation↔decision trace + independent decision-height/acceptance Review。

## Product Brief

### Create/update schema

- source identities、status/version、decision history；
- problem/context/why-now；
- named users、buyers/payers/operators 与差异；
- desired outcomes/jobs 和 current alternatives；
- value proposition、differentiation hypotheses；
- high-level capabilities；
- success metrics与不可接受结果；
- MVP/current scope、future/residual、non-goals；
- risks/assumptions/unknowns/rejected directions；
- downstream consumers 与 first unresolved decision。

先完成 F2 intake，再 draft/update。Product Brief 不由 Discovery reference 起草；F2 只交付协调后的 discovery state。

### Validation

反向检查 source、用户/problem、outcome、capability、scope、metric、non-goal、unknown、consumer。漂亮叙事不能覆盖 source loss 或 unresolved trade-off。

## PRFAQ / Working Backwards

PRFAQ 是概念 gauntlet，不是 marketing 文案。

### Press release

- specific customer/context；
- current problem/alternative；
- finished experience 和可观察 outcome；
- why this solution is materially better；
- proof/metric 与限制；
- customer quote 只能是明确 hypothetical，不可伪造 research。

### External FAQ

覆盖：who/when、how it works at capability level、cost/value、switching、failure/trust/privacy/accessibility、what it does not do、why now。

### Internal FAQ

覆盖：evidence、market/domain/technical unknown、dependencies、operating model、risks、go/no-go assumptions、measurement、reversibility、rejected alternatives。Mechanism 仍作为待 F5 验证的 question，不在 PRFAQ 内偷渡 accepted Architecture。

合法结论：`PROCEED / REVISE / KILL / RESEARCH_BLOCKED`。只有 user/authority 接受结论；“概念可写成 PRFAQ”不等于值得实现。

## PRD

### Essential spine

1. identity、sources、status、change signal；
2. executive intent/problem/outcomes；
3. named users/stakeholders/context；
4. journeys/use cases 与完成结果；
5. capabilities、FRs、business rules；
6. NFRs 与可观察 measurement boundary；
7. data/integration/security/privacy/accessibility needs（WHAT，不锁 HOW）；
8. success metrics、instrumentation questions；
9. MVP/current/future/residual/non-goals；
10. assumptions/unknowns/risks/dependencies；
11. requirement/source/journey/consumer coverage；
12. accepted/pending/rejected/superseded decisions。

### Stable vocabulary 和 IDs

为 users、capabilities、requirements、states 和 external concepts 建立 canonical term/ID。Update 保留既有 ID；删除/替换记录 disposition 和 affected consumers。禁止重排/改名导致 downstream trace 断裂。

### FR/NFR 规则

- 一个 requirement 一个可观察行为/约束；
- 写 actor、condition、result、boundary 和 failure expectation；
- 产品 conditional/forbidden semantics 明示；
- solution mechanism 只在产品本身规定 protocol/format 时进入 PRD；否则交 F5；
- “fast/secure/intuitive/scalable”没有 metric/boundary 时为 UNKNOWN，不是 NFR；
- every stakeholder voice represented 不等于每个请求都进 scope，rejection 要有理由。

### Update lifecycle

1. 固定 current accepted artifact identity 和 change signal。
2. 提取 old→new，区分 clarification、scope change、supersession、conflict。
3. 建 ripple map：Brief/PRFAQ、UX、Architecture、data、Epic/Story、evidence、status。
4. 只修改受影响 sections，但重跑 source/consumer/residual coverage。
5. 新承重决定由 user 接受；稳定 ID/decision history 不丢失。

### Validation rubric

分别给 verdict，不用总分掩盖 must-fix：

- source preservation；
- problem/user/outcome clarity；
- requirement testability；
- current/future/non-goal honesty；
- vocabulary/ID stability；
- journey/capability/requirement coverage；
- solution leakage/Architecture assumption；
- NFR/evidence feasibility；
- unknown/conflict/decision state；
- downstream consumer readiness。

发现一个 finding 后继续全部适用 rubric；一次返回 atomic batch。Validate 不直接把 candidate 改成 PASS。

## SPEC kernel 与 preservation law

SPEC 用于把 mixed intent source 蒸馏为最小但无损 WHAT，不是任意压缩。

### Kernel

```text
Purpose / outcome
Users / actors
Scope + non-goals
Behavioral rules and states
Inputs / outputs / contracts
Quality / safety / compliance constraints
Acceptance or success oracles
Unknowns / decisions / dependencies
Source map
```

### Companions

当内容不能在 kernel 中简洁且无损表达时，按真实语义创建直接 companion：data/schema、state/flow、examples/vectors、decision log、evidence matrix。Companion 不是把重要义务藏到深链，也不是固定模板要求。

### Preservation law

- distill 可以删 repetition/prose，不得删 distinction/condition/exception/non-goal/source；
- 每个 kernel/companion statement 反向映射 source；每个 source clause 有 disposition；
- conflicting source 不能被“综合”成虚假中间值；
- source 不足时写 UNKNOWN；
- downstream 只能从 accepted SPEC identity 消费，不从聊天记忆补齐。

## Scope/residual ledger

每个 capability/requirement/decision 有且只有一个状态：

- `CURRENT`：本规划范围内必须实现；
- `FUTURE`：明确不在当前范围，有 owner/revisit condition；
- `RESIDUAL`：accepted source 中存在但尚未被 current/future/other consumer处置；完成前必须归类；
- `REJECTED`：明确不做及原因；
- `SUPERSEDED`：被哪个 identity 替代；
- `EXCLUDED`：authority reason 和 non-trigger proof。

不能用“later”隐藏 residual，也不能为文档整洁清空 unknown。

## 用户交互

按 SKILL 统一协议逐 step 展示 candidate、coverage totals、unknown/conflict、direct ripple 和 pending decision。每轮最多三题。Fast path 可快速形成完整 candidate，但所有 inference 标明且 blocker 保持真实；不能因用户要求“先给完整草案”宣布 accepted。

## Completion / failure

完成需要：source dispositions `unmapped=0`；requirement 有 source/outcome/boundary/status；scope/residual closed；所有承重 decisions 明确；solution leakage 已移交；direct consumers 命名；用户明确批准当前 product step。

以下终止并保持当前 node：unresolved product trade-off、source conflict、missing user/outcome、不可观察 requirement、residual 丢失、recommendation 冒充 acceptance。完成后加载 F4/F5 中最低必要 reference；不直接跳 Story。
