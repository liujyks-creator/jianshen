# Product、PRD 与 Scope Planning

## When to load

当需要创建/更新 Product Brief 或 PRD、定义产品能力与 current/future 边界、确定当前 Epic done state，或审查 residual capability 时加载。若 Discovery 输入仍不足，返回 Discovery blocker；不要用 PRD 结构替代发现。

## Goal

定义“为谁、为什么、做什么、做到何种程度、不做什么”，把每项能力诚实分类，并证明当前 Epic 完成后的用户世界与剩余能力没有被 Story 编号或技术分解吞掉。

## Inputs

- Discovery 产物、brain dump、accepted Product Brief/PRD 与其原始 sources。
- Named user journeys、research、success evidence、domain concerns。
- Accepted roadmap/Epic identity、non-goals、prior decisions/rejections。
- 当前 shared state；Update 时还需 change signal 与 current candidate。

先做 input reconciliation。未受 delta 影响的 accepted facts/decisions 按来源继承；冲突与缺失写入 ledger，不静默补齐。

## Collaboration mechanics

### 1. 建立产品骨架

与用户共同关闭以下内容，深度随 stakes 调整：

- why、why-now、named user 与现实 journey；
- user outcome、capability 与业务规则；
- success metric 及 counter-metric；
- non-goal、constraint、applicable concern；
- FR/NFR 或项目等价要求。

从用户叙述提取 journey，不替用户发明主人公、动机、屏幕或分期。Coaching 一次问一个开放问题；Fast path 批量最少缺口并标 `[ASSUMPTION]`。

### 2. 能力 scope ledger

每项产品能力必须逐项记录：

| 字段 | 规则 |
|---|---|
| capability | 用户可识别的结果，不是文件/技术层 |
| source | accepted source 或 `UNKNOWN` |
| user value / journey | 谁在何时为何需要 |
| classification | `CURRENT` / `FUTURE_CANDIDATE` / `OUT_OF_SCOPE` / `UNKNOWN` |
| rationale | 为什么属于该分类 |
| decision owner | 谁接受分类 |
| revisit condition | future/unknown 何时重看 |
| downstream | 受影响 UX、Architecture、Epic、evidence |

“future”“later”“reserved”“以后”只产生 `FUTURE_CANDIDATE`，不能自动生成当前 Story，也不能自动成为下一 Epic。`UNKNOWN` 不是 `OUT_OF_SCOPE`；必须写影响和 owner。分类会改变当前范围时，展示选项/trade-off/推荐并等待用户决定。

### 3. Phase blocker 规则

在进入 UX、Architecture 或 Epic/Story 前，以下未知通常是 `phaseBlockers`：核心用户/结果、current/future 边界、当前 success、适用 form-factor、会改变主要 journey 的 concern、会决定核心数据/ownership 的产品语义。

不阻塞当前阶段的 future research 可进入 `deferred`，但必须有 owner 与 revisit condition。不得因 `openQuestions` 数量为零就宣布完整；逐项检查 source、journey、surface/consumer 和 residual map。

### 4. Current-Epic done state

规划当前 Epic 前，用用户可见语言描述：

- 完成后 named user 能完成什么完整结果；
- 哪些 current capabilities/requirements 已承载；
- 哪些明确不在这个 Epic；
- 完成的 observable success/evidence；
- 哪些 upstream decisions 必须保持。

done state 不得只写“所有 Story 合并”“DAG 完成”或技术组件存在。

### 5. Post-Epic residual capability map

在分 Epic/Story 前和规划完成时各检查一次。逐项列出：

| 字段 | 内容 |
|---|---|
| residual capability | 当前 Epic 完成后仍未提供的用户能力 |
| source / user value | 来源与为什么重要 |
| classification | future candidate / out of scope / unknown |
| why not current | 不进入当前 Epic 的已接受原因 |
| candidate next Epic | 名称/identity，或 `UNKNOWN / NOT_DISCOVERED` |
| readiness | 是否已有 PRD/UX/Architecture 输入 |
| owner / open question | 谁在什么条件下决定 |

Story suffix 不代表独立 Epic 已存在；例如 `E17-18` 仍必须与独立 `E18` 分开核对。没有定义下一 Epic 时写 `UNKNOWN / NOT_DISCOVERED`，不能写 remaining=0 或从编号推断内容。

### 6. Source preservation check

对每个 source 中的定性意图、future language、rejected direction、named journey、success/counter-metric 与 domain concern，标记它落在 PRD、scope ledger、residual map、deferred 或 rejected 的哪里。FR 编号覆盖不能替代这项检查。

### 7. Advanced Elicitation checkpoint

范围候选形成后选一次 assumption audit、boundary sweep、stakeholder lens 或 pre-mortem。尤其压力测试：future 是否被吸入 current、当前 Epic 是否被当成封闭世界、non-goal 是否与 success 冲突。先展示发现，用户接受后才更新分类或 done state。

## State and output

产物沿用项目既有 Product Brief/PRD/roadmap 格式，必须可恢复地包含或引用：product spine、scope ledger、FR/NFR、current-Epic done state、post-Epic residual map、source reconciliation 与共同状态。每项 scope classification 的 authority 可追。

## Blockers

以下情况暂停，不进入 UX/Architecture/CE：

- `CURRENT` 与 `FUTURE_CANDIDATE` 边界未由有权用户决定；
- named journey、核心结果、success 或 non-goal 互相冲突；
- `UNKNOWN` 会改变主要 surface、core owner/data responsibility 或 Epic boundary；
- residual map 无法说明已知 future value 去向；
- 来源或决定 authority 缺失。

## Completion / readiness checks

- why、named user/journey、capability、success/counter-metric、non-goal、FR/NFR 与 applicable concerns 足够关闭；
- 每项能力只有一个明确 classification、来源、owner 与必要 revisit condition；
- 所有 phase blockers 已关闭，其他 unknown 被诚实保留；
- current-Epic done state 是用户结果，不是 Git/DAG/文件计数；
- residual map 覆盖 accepted sources 中所有未进入当前 Epic 的能力，下一 Epic 不存在时明确 `UNKNOWN / NOT_DISCOVERED`；
- source preservation 没有被 requirements coverage 替代；
- 用户拥有所有 load-bearing scope 决定；
- `firstUnfinishedAction` 指向适用 UX 或 Architecture 的首个具体动作。
