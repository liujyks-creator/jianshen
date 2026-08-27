# F1 — Accepted State、Routing 与交互恢复

## 何时加载

新规划请求、恢复中断对话、状态/Git/decision 相互冲突、用户询问下一步，或任一 planning node 切换前加载。F1 只决定“现在真实处于哪里、最低必要 workflow 是什么”，不替领域 owner 完成产品、UX、Architecture 或 Review。

## Accepted inputs

按 `SKILL.md` authority 顺序收集并固定 identity：

- 用户本轮明确决定；
- host/repository instructions、formal templates、权限和 Git policy；
- accepted decision log、状态产物、Story/Review/evidence identity；
- Git commit/tree/ancestry 与 clean/protected state；
- 当前任务的直接 sources。

记录 excluded、missing 和 optional inputs。旧副本、branch report、聊天摘要、非祖先候选或低 authority 文档只能作为待核对 evidence，不能静默替换 accepted state。

## 重建算法

1. **固定 authority 与 identity。** 对 commit 使用 full SHA；对文件使用 accepted Git blob/内容 hash；对外部 source 使用 immutable version/commit/archive identity。mutable branch 只作 locator。
2. **机械证明 Git fact。** 区分 branch tip、merge ancestry、working tree、index、remote synchronization。status 文档不能覆盖 ancestry；ancestry 也不能替用户接受产品决定。
3. **建立 FactClaim。** 每项标 `PROVEN / INFERENCE / UNKNOWN / CONFLICT`，附 source、适用边界和影响。
4. **重建 approval。** 只接受明确 decision 或当前 step 的 `Continue`。artifact/hash、先前“全部完成”、聊天继续、角色报告或压缩摘要都不改变 gate。
5. **定位 first unfinished gate。** 从最低未闭合高度选择 F2–F10。已完成 step 不重放；后续 step 不因更有趣而抢跑。
6. **更新单一 PlanningState。** 写入 node、steps、accepted/pending decisions、approval、identities、protected state 和 first action。

## Permission-bound project context（C02）

当 repository rules、established conventions 或 observed pitfalls 需要重建时，F1 在现有 authority 下运行一个 project-context 子流程；它只整理适用 instructions，不形成第二 authority，也不默认修改 `AGENTS.md`。

先固定 intent：`setup`（尚无 context）、`refresh`（重新验证已有 context）、`record`（记录一次有证据的 agent mistake）或 `audit`（逐行复核和收缩）。然后：

1. 解析 exact repository/worktree target 与 write permission。只有 workspace/build manifest 或独立 build unit 能证明 child scope；sibling repository 不是 child。多个 target、不可提交 target 或 nested scope 不明确时，先展示证据并停止 mutation。
2. 读取所有适用 instruction files、executable config/CI、直接 source/docs 和必要的 targeted history。每个候选标为 `REPO_DERIVED`（附可解析 path/identity）、`HUMAN_ONLY`（用户治理/安全/冻结区等，不能从代码冒充）、`OBSERVED_PITFALL`（附本次或历史错误证据）或 `CONFLICT`。
3. repo 已直接表达的普通命令、目录树、stack 和 style 不复制；只保留无法从 repo 得出的 human rule、非默认 caveat、observed pitfall 与可观察 trigger。机械可防止的规则优先提议 check，而不是再写一条 prose。
4. `refresh` 从记录的 provenance 重新验证 path、rename/delete 与 human caveat；`record` 只接受已观察 mistake，先检查是否已有覆盖；`audit` 对每行区分仍有证据、需窄化和可删除，policy/pitfall 的删除必须由证据或用户 retire 支持。
5. 输出 root 与每个 accepted child 的完整最小 context preview，逐条列 source、verification identity、保留/排除理由、contradiction 与 proposed target。用户必须看到整组最终文本后明确批准 exact writes；host scope不允许写、用户未批准或 target含糊时只返回 findings。获准写时只改批准的 managed region，并保持其外 bytes 不变；永不 commit。

成功输出是 `verified context preview/findings` 或已获授权的 exact context delta；每个 path-backed claim 可解析，human-only rule 不伪装成 repo fact，nested rule 只约束其目录。contradiction、stale provenance、missing path 或 permission/approval gap 返回 `PROJECT_CONTEXT_BLOCKED`，保留 competing evidence 和最小恢复条件。

## Deterministic planning status（C25；唯一 mechanical owner）

F1 是 planning-status mechanics 的唯一 direct owner；F6 提供 accepted semantic identities/order，F8消费 validation report，F10只决定 semantic Repair authority。任何脚本或手工等价过程都不得判断 requirement、Story 或 readiness 是否语义完整。

### 输入与 canonical model

- immutable plan/source identity；F6 接受的 Epic/Story IDs 与 source order；声明的 status vocabulary/rank；
- existing tracking bytes、metadata/custom fields/comments 与 user-confirmed overrides；
- exact target、write permission 和 dry-run/write intent。

Canonical key 必须从 accepted identity 以一个声明且稳定的规则产生；同一输入总是同一 key。默认顺序为 Epic、该 Epic 的 Stories（accepted source order）、其 retrospective（若该计划包含此概念），再进入下一 Epic。不能按文件枚举、时间或当前 status 重排。生成/refresh 必须：

1. 新 identity 使用该类型的初始 status；existing legal status 按同类型 rank preserve，绝不自动 downgrade。
2. legacy status 只经声明的一对一 semantic map归一化并逐项报告；不识别或非法 status 报告为 invalid，不能伪装为初始值。
3. old key 与 accepted plan不再对应时作为 `orphan` 连同原 status/metadata 报告；可能 rename、删除或分拆时不静默丢弃。
4. unmanaged metadata、custom fields、comments 与 action/learning records原样保留，除非用户明确批准其 disposition。

`view` 只从已验证结构报告 counts、risks、orphans/illegal/legacy 与 next mechanical candidate，不宣布语义 READY。`validate` 是只读的，检查 parseability、required fields、key grammar、canonical order、status vocabulary、duplicate identity、legacy/orphan 与结构 invariants；valid structure 不等于完整 planning。

### Repair、write 与 failure

先 dry-run 形成 `new / preserved / upgraded / legacy-mapped / illegal / orphan / unrecognized / in-sync` report。corrupt/ambiguous state、identity collision 或 orphan disposition不唯一时，机械流程停止；F10/F6提供 source-backed semantic proposal，用户确认 exact mapping/status/disposition 后，显式 override 才能 downgrade、rename 或删除。脚本不得从 code、Story prose、commit subject 或文件存在推断 `done` 等产品状态。

获准写时先完整 serialize 到同目录 temporary file，flush并原子 replace；随后重新读取、validate并比对 canonical entries/metadata。失败时保留 original error 并恢复原 bytes；无法恢复必须明确报告可能损坏，禁止 silent success。最终 report绑定 input/output identity、write/no-write、validation与每项显式 override；本 package不依赖 upstream script、`uv`、runtime或installer。

## Workflow routing

| 当前最低缺口 | Route |
|---|---|
| 用户/problem/stakes/source/事实仍不清楚 | F2 Discovery/research |
| customer outcome、FR/NFR、scope/non-goals 未关闭 | F3 Product |
| journey/surface/state/visual/human gate 未关闭 | F4 UX |
| owner/lifecycle/data/error/framework feasibility 未关闭 | F5 Architecture |
| obligations 尚未形成 Epic/Story/DAG 或 capacity 失败 | F6 |
| source→owner→AC/evidence→consumer coverage 不闭合 | F7 |
| 一个 exact candidate 需要 ordinary readiness/handoff | F8 |
| planning candidate 需要 fresh independent source-first Review/Audit | F9 |
| accepted change/finding 需要 Correct Course、planning Repair 或 escape | F10 |
| repository instructions/context 需要 setup、refresh、record 或 audit | F1 project-context 子流程 |
| validated plan 需要 status generate、view、validate 或 repair | F1 planning-status mechanical contract；semantic gap仍回其 F/T owner |

create、update、validate 是 intent，不是 completion 状态。普通 readiness 由 F8 直接拥有；不要把它路由给 F9。Planning Review/Consistency Audit 是独立 gate，不能用 ordinary readiness PASS 替代。

## 第一轮输出

在进入任何新长 workflow 前展示：

- 目标与有序步骤；
- accepted、excluded、missing、optional inputs；
- `PROVEN / INFERENCE / UNKNOWN / CONFLICT`；
- decision agenda；
- 当前只处理的第一个 step；
- `currentNode` 与 `firstUnfinishedAction`。

## 承重问题

只问答案会改变 product、UX、Architecture、scope、owner、evidence 或完成判据的问题。每轮最多三题；每题必须含：

```text
Facts
Why now
2–3 mutually exclusive viable options
Trade-off per option
Recommendation + rationale
Direct ripple
Final decision owner
```

从 sources、Git、code 或 framework 可证明的事实先自行调查。可逆实现细节留给 accepted Story owner。若只有一个可行解，证明它并说明约束，不制造假选项。

## Approval state machine

```text
CANDIDATE
  ├─ user accepts displayed decision → ACCEPTED_DECISION
  ├─ user Continue after displayed step result → NEXT_NAMED_STEP_APPROVED
  ├─ Revise → CURRENT_STEP_OPEN
  ├─ Question → NO_STATE_CHANGE
  └─ Stop → SAFE_TERMINAL
```

接受后先回显 exact choice、conditions、rejected alternatives、ripple 和被解锁/仍阻塞的 node，再继续。`Continue` 不接受未展示的决定或未来 step。

## Conflict 与失败

- authority 排序可解决：记录低层 source 被 supersede 的范围与理由。
- 同 rank source 冲突且会改变 route：展示 exact identities、不同事实、选项、trade-off、影响和 owner；停在当前 node。
- identity 漂移或 exact path 缺失：`BLOCKED`；报告期望/实际 identity 和最小恢复条件，不找近似文件。
- unrelated UNKNOWN：记录但不阻塞已证明工作。

## Compaction 与恢复

1. summary 只作 locator；读取单一 PlanningState。
2. 校验 role、currentNode、approvalState、candidate identities、first action。
3. 只重读已变化或无法证明的 controlling source。
4. 报告恢复点并继续 first action；不重问 accepted decision、不重做已完成 role。
5. 无法证明批准时保持当前 gate。

## 输出 schema

```text
role
currentNode
terminal
sourceIdentities
accepted/excluded/missing/optional inputs
PROVEN / INFERENCE / UNKNOWN / CONFLICT
stepsCompleted
acceptedDecisions / pendingDecisions / approvalState
protectedState
firstUnfinishedAction
selectedFunction
```

## Observable success 与停止

同一 immutable inputs 总是产生同一 node；stale status 被暴露；完成工作不重放；方便但无 authority 的版本不被采用。route-changing conflict、missing authority 或 identity drift 终态为 `BLOCKED`。F1 完成后只加载所选 direct reference。
