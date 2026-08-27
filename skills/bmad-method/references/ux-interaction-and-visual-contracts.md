# F4 — UX、Interaction 与 Visual Contracts

## 何时加载

Accepted product behavior 已存在，需要定义 journey、information architecture、surface、state、interaction、responsive/accessibility、visual direction、microcopy 或 identity-bound human acceptance 时加载。

F4 负责体验如何被用户理解和完成，不改变产品语义，不创建新的 data/engine/lifecycle owner，也不代替 F5 选择技术机制。

## Accepted inputs

- F3 accepted Product Brief/PRFAQ/PRD/SPEC 与 stable requirement IDs；
- named users、contexts、jobs、success/failure outcomes；
- accepted platform、accessibility、privacy、safety 与 brand constraints；
- current product surfaces、design-system facts 和既有 human evidence；
- 用户对主观体验与 visual direction 的最终决定权。

输入缺失时按 `PROVEN / INFERENCE / UNKNOWN / CONFLICT` 分开。可从 accepted product source 证明的 behavior 不重新询问；会改变产品结果的 UX 分歧返回 F3。

## T2 — Product obligations 到 UX decisions

对每个 UX-relevant obligation 建立：

```text
uxDecisionId
source obligation IDs
named user/context/outcome
journey + surface/state implications
viable alternatives
verified constraints
trade-offs + recommendation
accepted option/conditions or PENDING
downstream ripple + final decision owner
```

交互或 visual 推荐不能自动成为 accepted。普通平台惯例可作为 verified constraint；当不同 viable routes 会改变用户能否完成、信息密度、操作顺序、风险感知或 subjective experience 时，使用最多三个承重问题让用户决定。

## Journey、surface 与 state closure

先建立 journey，而不是先画 screen：

1. named actor、entry condition、intent 和完成 outcome；
2. required decisions/actions 与信息需求；
3. surface/transition；
4. normal、empty、loading、offline、error、permission、conflict、interruption、recovery、completion；
5. exit、resume、cancel、retry 与 destructive confirmation；
6. downstream product/Architecture/evidence consumer。

维护双向矩阵：

```text
product outcome/requirement → journey → surface → state → interaction → evidence/human gate
surface/state → journey/outcome → source requirement
```

每个 required outcome 必须可达；每个 visible state 必须有来源或明确 rationale。不能用 happy path、mock screen 数量或 component inventory 代替 closure。

## Interaction contract

对关键 interaction 明确：

- trigger、precondition、user intent 与 system response；
- primary/secondary/destructive action hierarchy；
- state transition、ordering、interrupt/resume、retry 与 idempotence 的体验语义；
- input、validation feedback、error ownership 与 recovery；
- focus、keyboard/assistive operation、target size、reading order；
- latency/async/offline 时真实状态和 progress language；
- 不可用、未交付、失败、部分完成与成功的区别；
- analytics 或 automation 只能观察，不能成为第二产品 owner。

禁止以 optimistic 文案把 local/pending/failed 状态伪装为完成，也不得在 UX 层发明持久化、同步或数据一致性规则。

## Information architecture 与 content

- 用用户心智模型组织 object、task、navigation 和 hierarchy；
- 每个 canonical term 追到 F3 vocabulary，避免同一概念多名或同名异义；
- microcopy 描述真实 state/action/result，不承诺未被产品或 Architecture 保证的结果；
- current、future、reserved 和 unavailable capability 在界面上诚实区分；
- critical decision、error、recovery 与 status 不只靠颜色或 transient feedback；
- progressive disclosure 不得隐藏完成任务所必需的信息或约束。

## Responsive、platform 与 accessibility

记录 breakpoint/size class、orientation、input modality、content growth、localization 和 dynamic text 下必须保持的 hierarchy 与 task continuity。平台 convention 只在不改变 accepted behavior 时采用；冲突会改变 outcome 时返回用户决定。

Accessibility 至少覆盖 semantic structure、name/role/value、focus/order、keyboard/switch、contrast、motion、time limits、error identification 与 recovery。不能只写“accessible”；要绑定可观察 oracle 和适用 human gate。

## Visual direction 与 artifacts

Visual work按顺序推进：experience principles → hierarchy/density → visual direction variants → tokens/components → representative flows/states → implementation contract。

候选 variant 必须展示同一 accepted behavior，说明各自对 scanability、attention、density、learnability、error risk、brand 和 platform fit 的 trade-off。若 visual choice 主观且承重，用户决定；不把 Planner 推荐、生成图或 design-system惯例当作接受。

Mock、wireframe、prototype、visual spec 都是合同证据的一部分，但不能单独证明：

- 所有 product outcomes 已覆盖；
- production behavior 已实现；
- responsive/accessibility 在真实运行中成立；
- 用户已经完成 subjective human acceptance。

## Human acceptance

主观 visual/experience claim 使用 identity-bound human gate，记录：

```text
candidate identity
environment/build/artifact identity
task and starting state
observable checkpoints
what automation already proved
what only the human decides
result + date + decision owner
```

Screenshot、source inspection、fake、simulated state 或 automated assertion 不能替代真正声明为 human-only 的判断。Human gate 何时执行由 Story/evidence contract 明确；未执行不得写成 PASS。

## 输出 schema

- UX source/decision identities；
- journeys、surfaces、states 与 transition matrices；
- interaction primitives 和 error/recovery contracts；
- IA/navigation/content vocabulary；
- responsive/platform/accessibility rules；
- visual directions、tokens/components 和 rejected alternatives；
- product↔UX forward/reverse coverage；
- automated evidence 与 human gates；
- Architecture implications、open unknowns、non-goals 与 downstream consumers。

## Failure / stop

以下任一项阻止 F4 completion：

- required journey/outcome 不可达或缺 failure/recovery state；
- pending subjective/load-bearing UX decision 被静默默认；
- UX 引入产品行为、data owner、schema、engine 或 lifecycle 决定；
- surface/state 无 source，或 product requirement 无 surface disposition；
- accessibility/responsive claim 没有可观察规则；
- fake/build/source inspection 被用作 human acceptance；
- UX/Architecture feasibility conflict 未返回正确高度。

## Completion 与 handoff

当 product↔journey↔surface/state 双向闭合、承重选择已接受、human gates 已定义、没有 UX ownership 漂移，F4 才完成。把技术机制/feasibility questions交 F5，把 UX obligations、artifacts 与 human-gate identities 交 F6/F7/F8，并记录下一个 `currentNode` 与 `firstUnfinishedAction`。
