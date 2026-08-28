# F5 — Architecture、Ownership 与 Feasibility

## 何时加载

Accepted product/UX contract 需要稳定 system boundary、owner/lifecycle、data/identity、state/error、security/privacy、integration、operation 或 exact framework/version feasibility 决定时加载。

F5 选择可行的 HOW 和稳定责任边界；不重写产品 outcome，不把 unresolved UX choice 当技术默认，也不实施代码。

## Accepted inputs 与 evidence

- F3/F4 accepted contracts、obligation IDs 与 non-goals；
- verified brownfield code/schema/API/runtime facts；
- exact framework/library/platform version；
- current official docs、source、schema/export 或 smallest relevant PoC；
- host constraints、operational boundaries 与 user Architecture authority。

推荐路线前先写 feasibility question、版本、claim、需要的证明、stop condition。模型记忆、helper 名称、source string、fake/no-op、旧版本 docs 或“通常支持”不能证明可行。

Evidence 记录 exact source identity/location、适用版本、观察、限制、日期与被证明/未证明的 claim。PoC 只证明它真实执行的边界，不自动证明 production ownership、lifecycle 或 operation。

## T2 — Obligation 到 Architecture decision

只把 mechanism/owner/boundary 问题放在 Architecture height。每个承重 decision 记录：

```text
decisionId + obligationIds
responsibility dimension
verified current facts
2–3 mutually exclusive feasible routes
trade-offs and failure modes
recommendation + rationale
user-owned accepted route/conditions
rejected routes
direct and downstream ripple
```

若 exact framework 不能表达 product contract，不能假装可表达，也不能静默引入 callback、open-helper、wrapper、scheduler seam 或第二 authority。先呈现：改变产品合同、选择单一 semantic owner、采用另一已证明机制，或 `BLOCKED`。用户拥有 load-bearing route。

## T3 — Stable owner/lifecycle contract

为每个 responsibility dimension 建立且只建立一个 primary owner：

- physical schema / DDL / migration；
- semantic validation；
- identity generation 与 immutable binding；
- canonical serialization/digest；
- mutation/transaction；
- read/assembly/ordering；
- state machine/lifecycle；
- error taxonomy/recovery；
- external integration/security/privacy；
- evidence generation 与 direct consumer boundary。

Owner contract 至少包含：

```text
responsibilityId
primary owner + production path
created/used/destroyed lifecycle
accepted inputs and outputs
mutation authority
invariants and error semantics
direct consumers
explicit non-owners
feasibility evidence identity
```

一条 responsibility 不得有第二 writer/validator/schema/lifecycle owner。Narrow validated type 可以保护一个已决定的 boundary，但不得变成通用 wrapper、第二 domain model、调度器或持久化 owner。

## T3 转换闭合合同

- **Input authority**：accepted T2 Architecture decisions、verified code/framework facts 与 exact-version feasibility evidence。
- **Output schema**：responsibility ID、decision IDs、component/path、physical/semantic/lifecycle owner、mutation entry、read path、error owner、boundary class、prohibited second owners。
- **Coverage/invariant**：每个责任维度一个 primary owner；lifecycle start/end、所有 production entry/read path、direct consumer 和 exact-version feasibility 均闭合。
- **Allowed loss**：机械同一 owner 的 alias 可合并；非承重 implementation detail 可留为 Story discretion但必须有归属。
- **Forbidden loss**：第二 schema/validator/state authority、hidden callback/wrapper/scheduler owner、unverified API、无 lifecycle责任、让 Writer发明 core owner。
- **Unknown**：未知 feasibility/owner 阻塞 affected Story；建立 evidence plan 或 viable routes，不 defer 给 Writer。
- **Checkpoint**：用户接受竞争的 load-bearing owner/mechanism；纯 evidence confirmation 不需要价值选择。
- **Failure terminal**：`FEASIBILITY_BLOCKED / OWNER_CONFLICT / LIFECYCLE_UNCLOSED / SECOND_AUTHORITY`。
- **Downstream/oracle**：T4/T5/F8消费；authoritative docs/source/PoC + production-path ownership trace，第二入口应使 oracle失败。

## Architecture spine

只表达支撑当前 accepted scope 所需的最小 spine：

1. system/context 与 trust boundaries；
2. components/modules 及责任；
3. data objects、identity、ownership 与 lifecycle；
4. command/event/state flow；
5. write/read/assembly/serialization/consumer paths；
6. concurrency、ordering、atomicity、failure/recovery；
7. security/privacy/external/operational boundaries；
8. exact production modification set 与 protected/excluded paths；
9. evidence surfaces 与 known limitations。

路径/类型名称需来自 verified project facts；若尚未存在，Story 必须明确创建点和唯一 owner，不能让 Writer临场发明 core interface。

## Internal guarantee 与 real boundary

对每项 state/constraint 分类：

- `TRUSTED_INTERNAL_GUARANTEE`：accepted type/compiler/framework contract 排除；不为 impossible state 加 guard/default/test。
- `UNTRUSTED_BOUNDARY`：user input、persisted/import data、network/API、file、device/platform；明确验证与 fail-fast/error contract。
- `ARCHITECTURE_INVARIANT`：跨 owner/lifecycle 必须由 named production owner维护。
- `DEFERRED/EXCLUDED`：有 authority、consumer disposition 和 non-trigger proof。

不要在所有层重复 validation。真实 boundary 失败保留原始 signal 或使用 accepted typed error；禁止 broad catch、silent default、fake success 和 defensive overengineering。

## Data、identity 与 flow closure

分别证明：

- stable object identity；
- identity 对 immutable payload/version 的 binding；
- digest/integrity value；
- ordering 与 canonical representation；
- transaction/atomicity；
- state transition 和 retry/idempotence；
- read assembly 到唯一 semantic validator/consumer 的 production path。

这些是不同 obligation，不能用 digest 代替 identity，也不能用测试中整理后的 graph 证明 production DAO/read path。每项标 primary owner、producer、consumer、error 和 evidence boundary。

## Cross-cutting dimensions

按适用性而不是仪式覆盖：security/privacy、authorization、data retention、observability、performance/scale、availability/offline、localization/accessibility support、deployment/rollback/migration、compatibility/versioning。

无法量化或无 consumer 的“secure/scalable/fast”保持 UNKNOWN。跨功能要求不能创建一个模糊的 shared owner；把责任放回明确 production owner。

## Failure / stop

以下情况返回 `ARCHITECTURE_BLOCKED`、上游选择或 Correct Course，而不是交 Writer猜：

- exact framework/API/version feasibility 未证明或被证伪；
- 需要新 core interface、wrapper、callback owner、scheduler/test seam、第二 schema/validator authority；
- owner/lifecycle/data responsibility 冲突或随 Story 漂移；
- implementation 才能决定的 route 会产生不同 product/persistence/external behavior；
- product requirement 在可行机制下无法满足；
- fake/helper/source string 被当 production evidence；
- unsupported constraint 被宣称由 framework/DB强制。

## 输出与 observable success

产出 accepted Architecture decisions、spine、responsibility/owner/lifecycle maps、production modification set、data/identity/flow/error/security/operation contracts、feasibility evidence identities、rejected routes、limitations 与 downstream ripple。

成功时每个 load-bearing mechanism 对 exact version 可证明；每个责任维度只有一个 primary owner；Writer 不需新增 core ownership；Architecture 可稳定跨 Story，并能由 F6/F7 建立独立 evidence。然后记录 `currentNode=epic_story_dag` 或最早缺口，并 handoff F6/F7。
