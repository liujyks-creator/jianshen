# Architecture 与 Solutioning

## When to load

当两个下一级实现单元可能对同一非显然承重问题给出不兼容答案，或需要创建/更新/验证 architecture spine 时加载。若问题仍是产品范围、UX 语义或用户结果，返回相应 planning 高度，不用 Architecture 代选。

## Goal

形成 lean architecture spine：只锁定跨下一级单元必须一致的 paradigm、boundary、state mutation、owner/shared data、error/invariant 与 operational envelope；继承 accepted parent invariants，不重问或重验未受 delta 影响的合同。

## Inputs

- Accepted PRD/scope、applicable UX、data contract、non-goals 与 evidence needs。
- Brownfield 的真实代码、当前 architecture、dependency graph 与 project conventions。
- Parent architecture spine/accepted decisions 及其 immutable identity。
- 当前 change/delta、shared state 和 directly affected consumers。

Brownfield first action 是调查实际代码和 accepted parent invariants；不要先问用户代码已经证明的事实。Parent invariants 记录为 read-only inherited constraints，原 ID/来源保持稳定。

## Collaboration mechanics

### 1. Altitude 与 divergence test

明确本 spine 约束的下一级：initiative→Epic、Epic→Story 或 feature→unit。对每个候选架构项问：

> 两个下一级单元独立实现时，是否可能选出不兼容答案？该答案是否非显然且存在真实 trade-off？

只有两个答案都为是才升级为 architecture decision。可从 compliant code 直接读出的 stack/tree/full schema 属于 seed/context；单 Story 私有实现细节留给 Story；不为“完整文档”锁死非承重细节。

### 2. Inherited invariants

建立 inherited ledger：ID、rule、source identity、适用边界、当前 delta 是否触及。未触及项直接采用，不重新询问、不重复机械验证。当前候选若冲突或弱化 inherited rule，标 `phaseBlocker` 并向 parent planning height 升级；本地不能覆盖。

Identity-bound evidence 只在 delta 影响其前提、consumer 或 observable claim 时重验。实现将来仍需的 production/device evidence 不因设计继承而被声称完成。

### 3. 结构维度 closure

逐个适用维度标 `DECIDED / DEFERRED / OPEN / N/A`：

| 维度 | 必须说明 |
|---|---|
| paradigm | 共同模型与它防止的分叉 |
| boundary/dependency | 模块/层/进程边界与依赖方向 |
| state mutation | 真源、写入入口、transaction/concurrency/lifecycle |
| owner | runtime/control/error/decision 的唯一 owner |
| shared data responsibility | schema、serialization、retention、migration、consumer 责任 |
| error/invariant | typed failure、fail-fast/rollback、禁止 silent fallback |
| operational envelope | environment、permission、offline、performance、observability、deployment/device 等适用边界 |

`DEFERRED` 必须证明不会让当前下一级单元分叉，并带 owner/revisit condition；否则是 `OPEN`。整个维度沉默是 finding，不是精简。

### 4. 承重技术选择

Coaching 默认用开放问题理解约束，再展示 2–3 个真实可行路线、verified facts、trade-off、推荐与它防止的 divergence。用户选择后才写入 `acceptedDecisions`。Fast path 可给完整候选，但推断标 `[ASSUMPTION]`，承重 stack/paradigm/boundary/owner 仍需用户或 accepted authority 接受。

每个 decision 至少记录：stable ID、`Binds`、`Prevents`、`Rule`、source/owner。Rationale 可在 decision ledger，spine 保持可执行。

### 5. Product/UX 与 CE 边界

Architecture 不得首次决定：current/future scope、用户看到的 chart semantics、关键 surface、visual direction、Epic boundary。若这些仍 open，返回对应 node。

CE 不得首次决定 paradigm、core boundary、owner、state truth 或 shared data responsibility。若 Epic/Story 分解暴露这类分叉，先回 Architecture 关闭，再恢复原 CE `firstUnfinishedAction`。

### 6. Advanced Elicitation checkpoint

Spine 候选形成后做一次 divergence review、boundary sweep、second-order 或 red-team：让两个假想下一级单元分别解释每条 Rule，找不兼容答案、隐性第二 owner、error 被吞或 operational dimension 沉默。先展示发现，用户接受后才修改 decision。

## State and output

沿用项目 architecture artifact，输出：scope/altitude、inherited invariants、minimal seed、decision spine、dimension closure、diagrams（只有结构关系需要时）、Deferred/Open、sources 与共同状态。每个承重 decision 可追到用户或 accepted authority。

## Blockers

- 产品/UX 语义仍会改变架构；
- inherited invariant 冲突或来源 identity 不明；
- paradigm、boundary、state mutation、core owner/data responsibility 有适用 `OPEN`；
- 两个下一级单元仍可能在未记录维度产生不兼容答案；
- 决定需要用户选择而未接受；
- brownfield 未调查真实代码便要绑定新规则。

## Completion / readiness checks

- altitude 和下一级单位明确；每条新 decision 通过 divergence test；
- parent invariants 被继承、未重问，冲突已升级而非本地覆盖；
- 所有适用结构维度为 `DECIDED`、安全 `DEFERRED` 或有理由的 `N/A`，无阻塞 `OPEN`；
- owner 与 shared data responsibility 唯一、dependency direction 和 mutation path 可执行；
- error/invariant 与 operational envelope 足以让 Story 独立实施不分叉；
- 没有 Architecture 首次决定产品/UX，也没有把 core decision 推给 CE；
- 未受 delta 影响的 identity-bound evidence 不重复验证；
- `firstUnfinishedAction` 指向 Epic/Story prerequisites 的首个具体动作。
