# F7 — Obligation、Traceability、Contract Closure 与 Evidence

## 何时加载

任一 accepted source clause 必须无损传播到 decision、owner/path、behavior、Story/AC、independent oracle/evidence 与 direct consumer；或需要关闭 identity、binding、ordering、atomicity、state/error、union、raw derivation、human/external boundary 时加载。

F7 设计合同和可判定证据，不实施 validator/test，不把 candidate 内部 coverage 当完整性证明。

## Accepted inputs

- 所有 identity-bound accepted sources/decisions 及 exact clauses；
- F3–F6 artifacts、stable IDs、owner/lifecycle 与 Story/DAG；
- exact framework guarantees、production paths 与 direct consumers；
- evidence capabilities、human/device/external gates 和 protected state。

旧总结、keyword、测试数量、helper存在、Planner声称“覆盖”只能是待验证 evidence。

## T1 — Clause 到 normalized obligation

逐 clause 建立稳定 row：

```text
obligationId
sourceId + exact location/clause
modality: REQUIRED | FORBIDDEN | CONDITIONAL | OPTIONAL | DEFERRED
subject + condition + behavior + object
qualifiers: identity | binding | ordering | atomicity | transition | temporal | version | error
direct consumer or unresolved consumer
classification: PRESERVE | ADAPT | MERGE | REFERENCE | EXCLUDE
classification reason/authority
status: OPEN | MAPPED | DEFERRED | EXCLUDED | CONFLICT
```

每个 source capability/clause 只有一个 primary classification。允许 loss：非规范格式、重复 presentation、persona/installer/runtime shell 和有 lineage 的等价合并。禁止 loss：modality、required-iff、forbidden、identity/binding、ordering、atomicity、transition/error、temporal/version qualifier、rejected/future 状态、consumer 与 provenance。

机械对账 `source clauses = all dispositions` 且 non-excluded unmapped 为零；语义正确性仍由 fresh source-first Review证明。

## Typed obligation closure

每条 non-excluded obligation 扩展到：

```text
decisionId + accepted evidence
primary owner + exact production path
trusted/untrusted boundary classification
observable behavior and failure semantics
StoryId + ACId
independent oracle/evidence + evidence identity schema
direct consumer and downstream consequence
forward/reverse coverage status
```

以下 obligation 必须分别建 row，不能用一个“大验证”声明覆盖：

- stable identity；
- identity→payload/version binding；
- canonical representation 与 digest/integrity；
- included/excluded field coverage；
- required-iff、optional、nullable、forbidden；
- discriminator、closed union、nested/unknown-field behavior；
- execution ordering、uniqueness、continuity；
- raw rows→derived/summary invariant；
- atomic mutation/transaction/rollback；
- state transition、retry/idempotence；
- error taxonomy 与 failure propagation；
- production read/assembly→single validator→direct consumer。

Digest 不能替代 stable identity；对 payload A 计算 digest 后导出重新编码的 payload B 不构成 immutable binding；测试排序后的 graph 不能证明 DAO production ordering。

## Contract derivation templates

### Required-iff / forbidden

```text
IF predicate THEN required field/state/behavior
IFF when biconditional is intended
ELSE forbidden/absent/null rule
producer + validator owner + direct consumer
positive/negative/boundary mutations
exact failure/path oracle
```

区分 missing、explicit `null`、empty、default 和 forbidden。不要把一向的 `if` 错写成 `iff`。

### Closed union matrix

每个 variant 明列：discriminator、required、optional、nullable、forbidden、nested structures、unknown-field rule、status/reason 组合与 version behavior。检查所有 variant 以及 unknown/mismatched discriminator；只覆盖 version 字段不构成 closed union。

### Identity / canonical / digest

分别定义 object ID 唯一性与更新稳定性、payload field matrix、schema/version、key/array order、missing/null/forbidden、string/number/escaping/newline/UTF-8/BOM、digest excluded self-field、immutable byte buffer 和 direct export consumer。Oracle 必须能证明“求 digest 的 bytes 就是实际消费/导出的 bytes”。

### Ordering / raw derivation

定义 stored order authority、base/continuity/duplicates/negative、assembly规则、consumer order。Derived/summary contract 列出 raw inputs、eligibility predicate、`NULL iff` 或其他 modality、deterministic derivation 与 corruption failure。

### Atomicity / state / error

定义 transaction boundary、all-or-nothing participants、failure injection points、rollback oracle；state machine 定义 allowed/forbidden transition、retry/recovery、terminal state；error contract 定义 preservation/translation owner，禁止 broad catch、silent default 或伪成功。

## Boundary classification

- `TRUSTED_INTERNAL_GUARANTEE`：accepted compiler/type/framework保证；不为不可能状态添加 guard/fallback/test。
- `UNTRUSTED_BOUNDARY`：user、persisted/import、network/API、file/device/platform；需要明确 validation/failure。
- `INTERNAL_CROSS_OWNER_INVARIANT`：由一个 named production owner维护并由 direct consumer证据观察。
- `HUMAN_ONLY_CLAIM`：绑定 human gate；automation只能证明其前置事实。
- `EXCLUDED`：authority reason + non-trigger regression。

不要把真实 external/persistence 边界误当内部保证，也不要把 accepted impossible internal state转化为防御性工程。

## T5 — Story obligations 到 AC/evidence

为每条 Story obligation 选择 matching-layer oracle：

| Claim | Evidence layer |
|---|---|
| schema/migration | exported/fresh/migrated schema inspection + fixture |
| pure semantic rule | independent fixtures/mutation matrix + exact error/path |
| canonical bytes/digest | independent golden vector/known-answer + same-buffer assertion |
| transaction/rollback | integration/failure injection at real transaction boundary |
| production read/consumer | end-to-end production path observation, not reordered test helper |
| UI/experience | automation for objective states plus identity-bound human gate for subjective claim |
| external/device/platform | real boundary evidence required by accepted risk contract |

Oracle 必须独立于被测 production mechanism。Production encoder/validator常量生成自己的 expected value 是 self-oracle；fake/no-op、source keyword、mock存在或测试数量不是 production evidence。

Mutation matrix 按 contract 类命名，而不是承诺任意数量：每类列合法 base、single mutation、expected terminal、exact path/error、受影响 consumer。至少覆盖 applicable required、forbidden、discriminator、conditional、identity/order、raw derivation、digest/tamper、unknown-field/error semantics。

## T5 转换闭合合同

- **Input authority**：exact T4 Story、T1 typed obligations、T3 owners/boundaries、available independent evidence 与 human authority。
- **Output schema**：AC ID、obligation ID、precondition/input、observable/forbidden behavior、failure semantics、direct consumer、oracle type/source、evidence command/artifact、human gate、identity binding。
- **Coverage/invariant**：每个 Story obligation 有 matching-layer positive/negative/boundary evidence；contract matrices闭合；oracle独立于 production implementation/constants；human claim仍由human判定。
- **Allowed loss**：只有一次 failure仍能定位全部 covered obligations时才合并 tests；framework-guaranteed impossible internal state 可带 exclusion proof不测试。
- **Forbidden loss**：happy-path-only、digest代替identity、production encoder自证、fake/no-op作production proof、source/build/simulated environment代替human proof、excluded state防御性工程。
- **Unknown**：unprovable claim保持open并阻塞 READY；区分 evidence capability gap 与上层决定，若产生新 owner则返回 F5。
- **Checkpoint**：用户只决定承重 evidence trade-off或执行human gate；accepted oracle内的测试选择不是产品批准。
- **Failure terminal**：`EVIDENCE_GAP / SELF_ORACLE / BOUNDARY_MISMATCH / HUMAN_GATE_UNBOUND`。
- **Downstream/oracle**：T6/T8与Writer消费；独立 mutation/golden/consumer/integration oracle，plausible seeded regression必须失败，human result绑定candidate identity。

## Coverage reconciliation

同时维护：

```text
forward: source → obligation → decision → owner/path → behavior → Story/AC → evidence → consumer
reverse: consumer/evidence/AC/Story/path → owner/decision → obligation → source clause
```

计数、link、DAG、schema 检查可以机械化；classification、semantic preservation、owner correctness 与 oracle independence 需要 fresh Review。每个 `EXCLUDE` 有 authority reason 和“不能被误触发”的 regression。

## C35 retrospective evidence bridge

当 completed/failed work 需要反馈到 planning，F7只提供 planning-only retrospective 的 evidence/traceability输入，不另建 retrospective authority。为 exact spec/Story/diff/commit/test/runtime/log/Review artifact记录 identity、可用性、checked scope与缺失原因；每个分析轴必须输出 `CHECKED_CLEAN`、`FINDING` 或 `NOT_CHECKED`，因此 missing evidence永远不会被写成 clean。

每个 learning claim沿现有链反向绑定 `source evidence → observed behavior/divergence → affected obligation/consumer → earliest F/T gate or owner → proposed prevention oracle`。没有 primary source reference的原因推断被丢弃；缺少 evidence只窄化 claim/scope。F10拥有 retrospective method与human disposition，F9提供独立验证纪律；F7不自动应用lesson、修改spec/code或写delivery/status state。

## Failure / stop

以下任一项输出 `NOT_READY` 并指向最早失败的 F/T：unmapped clause、duplicate primary disposition/owner、missing consumer、qualifier loss、self-oracle、evidence layer mismatch、unknown-field/union gap、production path未绑定、human claim被自动化冒充、excluded impossible state被要求实现。

Unclear clause 标 `UNKNOWN/CONFLICT`，不能编造 constraint；若改变 product/UX/Architecture，返回 F3/F4/F5 和 decision owner。

## Completion 与 handoff

只有 forward/reverse coverage 均闭合、typed obligations 分离、每项有一个 owner/consumer、independent evidence 与 failure terminal，F7 才完成。输出 obligation/evidence matrix、coverage totals、exclusions、human gates、unknowns 和 F6/F8/F9 consumers；完成不等于 fresh evidence 已运行，也不等于 `READY`。
