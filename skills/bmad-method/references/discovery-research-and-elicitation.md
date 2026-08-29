# F2 — Discovery、Research 与 Load-bearing Elicitation

## 何时加载

用户目标、problem、stakes、constraints、source facts 或 viable directions 尚不足以进入 product/Architecture；用户要求 brainstorm、pressure-test/forge、research、比较选择，或现有规划需要重新发现时加载。

F2 发现和证明，不起草/宣布完成 Product Brief、PRD、UX、Architecture 或 Story。

## Intake

1. 若用户尚未完整表达，邀请一次完整 brain dump 和已有 sources。
2. 若已有长叙述，先反射目标、why-now、现实场景、约束、张力和被拒方向，再只给一次自然补充机会。
3. 校准 stakes、form factor、new/brownfield、decision deadline、可逆性和 evidence 标准。
4. 建立事实/假设/unknown/conflict 与 decision agenda；从项目 source 可发现的事实自行读取，不问用户复述。
5. 只把阻塞 product、UX、Architecture、scope、owner 或 evidence 的事项升级为承重问题。

## Invocation 与 return identity

进入 F2 时先区分 `initial_discovery` 与 `called_subflow`。初始 Discovery 没有 caller return context；任何由 F3/F4/F5 调用的 research/elicitation，以及对 existing research 的 `refresh/deepen`，都是 `called_subflow`。

`called_subflow` 在开始工作前记录并冻结 `returnContext`：exact caller identity（function/node 与 identity-bound artifact/candidate identity）、caller `currentNode` 和 caller `firstUnfinishedAction`。F2 产生的 evidence、decision input 或 consumer notice 不能覆盖这些字段，也不能创建新的调用节点。若 non-initial invocation 缺少或无法唯一证明 caller/return identity，保留 `UNKNOWN` 及其影响，立即以 `terminal=CALLER_RETURN_IDENTITY_MISSING` 停止，设置 `firstUnfinishedAction=resolve exact caller identity, currentNode, and firstUnfinishedAction`；不得以 F3 作为 fallback。

## Discovery 输出

- problem/context 与 named stakeholders；
- why-now、stakes、constraints、success/failure signals；
- source inventory 和 FactClaims；
- accepted/rejected directions 与理由；
- research needs、decision agenda、blockers；
- handoff 给 F3 的 discovery state。

## Brainstorming

Brainstorm 的目的先发散、后收敛，不替用户生成未经检验的战略。

### Stance

- `expansive`：改变 stakeholder、time horizon、constraint、analogy、opposite、combination，保留异常候选。
- `critical`：找隐含假设、failure case、counterexample、dependency。
- `synthesis`：聚类、命名主题、识别组合与缺口。

默认先发散。用户说方向足够或明确要求选择后才收敛。不要边生成边排序。

### 方法选择

按当前目标只选一个方法并说明为什么：

- reframing / first principles / inversion；
- SCAMPER / analogy / constraint removal；
- stakeholder or jobs lens；
- pre-mortem / assumption audit / boundary sweep；
- impact-effort / NUF / PMI / affinity / MoSCoW（只用于收敛）。

方法是思考工具，不是 authority。价值/风险权重由用户决定。

## Generic idea forge

当用户要求 pressure-test、harden、clarify 或 cheaply kill idea：

1. 固定 idea、session goal、new/existing context；existing context 以真实 project sources 为准。
2. 一次只推进一个依赖最高的问题。模糊术语先精确定义；项目 claim 与 source 冲突时先解决冲突。
3. 用户可要求 `attack`、`defend` 或 `switch`。attack 找矛盾/失败/薄弱假设；defend 构造 strongest viable version；两者都不表演 persona。
4. 记录 `assumption / crack / rejected / direction / lock`。`lock` 只能来自用户接受或 authority 证明。
5. 每个分支解决后给用户补充机会，再移动。

合法退出：

- `HARDENED`：形成极短的锁定决定、被拒选项和理由，可作为 F3 input；
- `KILLED`：核心假设不成立，明确原因；
- `CLEARER`：理解改善但没有可交付 hardened idea。

任何退出都不授权“开始实现”。不要强制生成 HTML、seal、persona roster 或 runtime workspace。

## Advanced elicitation checkpoint

在重要候选形成、用户要求深化或风险较高时，选择一个最能改变判断的 lens：

- pre-mortem；
- assumption audit；
- boundary/edge sweep；
- map-is-not-territory；
- second-order effects；
- stakeholder conflict；
- red-team / devil's advocate；
- feasibility or evidence challenge；
- simplification / subtraction。

展示：选择理由、发现、是否改变候选、需要的用户决定。用户接受后才能修改 accepted candidate。不要为了“使用高级方法”循环所有 lens。

## Decision-grade research

### Research firewall

在搜索前写清：

```text
decision to inform
scope and exclusions
claim types
required freshness
acceptable source quality
success/stop criteria
known assumptions
```

Research 产出事实、证据强度、implication 和 unknown，不替 decision owner 做价值选择。未引用、过时或二手 claim 不得升级为 proven。

### Research packs

按决策只加载必要 pack：

1. `user/problem`：named user、context、behavior、JTBD、pain、alternative、evidence limits。
2. `market/competitive`：segments、alternatives（含不行动）、competitors、positioning、switching forces。
3. `industry/domain`：value chain、regulation、standards、economics、domain vocabulary。
4. `technical/feasibility`：exact versions/APIs、constraints、security/privacy、integration、operational evidence；load-bearing conclusion 交 F5。
5. `ecosystem/organizational`：stakeholders、buyers/payers/operators、dependencies、governance、adoption。
6. `selection/comparison`：criteria、hard gates、weights、evidence、sensitivity 和 recommendation。
7. `academic/literature`：seminal work、recent surveys、state of the art/benchmarks、methods/limitations、open debates 与 relevant labs；适用于研究型技术路线、empirical claim、literature review 或研究级 build-vs-adopt 判断。

不要把用户、buyer、payer、operator、admin 合并，除非 source/decision 明确同一角色。

#### Academic literature pack

先找到一篇可靠 recent survey，再沿其 references 与 forward citations 双向追踪原始论文；不能用大量 abstract 搜索代替 lineage。每条引用标明 `preprint / peer-reviewed`，preprint 不等于接受；benchmark 数字回到 original paper，不从竞争者表格转引。

对 load-bearing empirical claim 检查 retraction/correction 与 replication status。仅由同一 lab 或同一 underlying dataset重复报告的结果仍是 lead，不是独立事实；结论至少需要 independent replication 或不同来源 corroboration。记录 methods、assumptions、limitations、measurement 与 field disagreement，不只记录最好结果。

State-of-the-art freshness默认不超过12个月，ML/AI不超过6个月；seminal work无固定期限，但必须检查是否已被 superseded。为 major conclusion主动寻找 contrary evidence、failed replication 与 strongest good-faith objection；发现冲突时保留 `verified / disputed / unverified / overturned` 区分。证据不足、retracted、single-lab 或超出 freshness时缩窄 claim、降 confidence 或保持 UNKNOWN，绝不补齐 certainty。

### Source quality 与 freshness

优先级依 claim 而定：官方 docs/source/law/standard/primary data > first-party current material > reputable analysis > secondary summary。记录 publication/effective/version date、访问日期、适用地域/版本和 conflict。对易变 claim 设 freshness window；过期 evidence 标 `STALE`，不能用模型记忆补齐。

至少核对：source 是否真的支持 claim、单位/分母/定义是否一致、引用是否可追、结论是否超出证据、反证是否存在。Claim ledger保留 publisher、publication/version date、access date、source class、status 与 staleness；`disputed / unverified / overturned`不能在摘要中被折叠成 proven。

### Selection research

1. Research 前由用户或 accepted project source 确认 requirements frame：hard gates、weighted preferences，以及 budget、scale、compliance、team、existing stack 和 exit-cost tolerance。Web research 只能提供 candidate evidence，不能替 decision owner 设 frame、gate 或 weight。
2. 建立 credible candidate screen，记录 leaders、strong challengers 与一个 wildcard；逐项记录 hard-gate cuts 及理由，收敛到 3–5 个可比较 finalists。任何 failed 或 unresolved hard gate 都阻止 selection，以 `terminal=SELECTION_HARD_GATE_UNRESOLVED` 保留失败信号，不得输出 pick。
3. 对每个 finalist 逐 criterion 记录 current-version evidence、score、confidence、unknown 和 gate result。top candidates 间每个 contested cell 都有 direct citation；vendor claim 与 independent evidence 冲突时保留双方 identity 与 divergence。pricing、performance/scale，以及决定 top two 的 cell 使用两类来源。
4. 比较 product horizon 内 total cost：license/subscription、hosting、operations、learning，以及 lock-in 与真实 exit/migration cost。Candidate screening evidence 默认不超过六个月，current pricing 默认不超过三个月且直接读取；selection report 超过两个季度必须在行动前 refresh，并在报告中保留适用 freshness 边界。
5. 输出完整、可检查且可重加权的 weighted matrix，并做合理权重 sensitivity。条件式 verdict 必须包含 pick、named runner-up、runner-up 胜出条件、strongest argument against the pick 和最低成本 reversibility hedge。该结果明确交给 F3/F5；recommendation 不是 accepted decision，接受权仍属于用户或 authorized decision owner。

### Refresh / deepen / stop

- `refresh/deepen` 的 first gate 是 exact existing research identity、claim ledger 与适用 freshness windows。old identity 缺失、含糊或不一致时，不得生成 delta claim；保留 `UNKNOWN` 并以 `terminal=RESEARCH_DELTA_IDENTITY_MISSING` 停止，设置 `firstUnfinishedAction=resolve exact existing research identity, claim ledger, and freshness windows`。
- `refresh`：decision 与 accepted scope 未变但易变 evidence 过期。由 old ledger 形成 identity-bound stale set，先确认 stale/affected claims，再只重新验证该集合；不得从头全量研究。
- Delta 对每个 claim 至少记录 `confirmed / changed / overturned / new sources`，绑定 claim identity、old/new source identity 与 old/new status。stale set 外的 claim 保持原 identity 与 status；新的 summary 或全量 run 不得静默覆盖它们。
- overturned load-bearing premise 必须产生 named affected-decision/consumer notice，列出已经消费它的 F3/F5/F7 或其他 exact downstream artifact。按已存在的 F1/root routing 恢复 exact caller 或 lowest affected node，并把重开该决定/consumer 作为 first unfinished action；F2 不成为第二 routing owner。
- `deepen`：只处理一个最可能改变 decision 的 dimension；未受影响 claims/sections 保持原 identity、status 与内容。若 conclusion 未改变，delta 必须明确记录。
- 只有 accepted scope 本身改变时才建立带 old-scope lineage 的新 research scope；不得把 scope change 伪装成无 lineage 的 `refresh`。
- `stop`：success criteria 满足、下一信息不会合理改变 decision，或客观 source unavailable；保留 unknown。

## 承重问题与用户 checkpoint

每轮最多三题，使用 SKILL 统一格式。Research fact 可证明时不问。用户拥有方向、权重、风险承受、scope 和价值判断；Planner 可推荐但不能接受。

## Failure/stop

- decisive fact 无来源：`UNKNOWN`，阻塞依赖决定；
- sources 冲突：`CONFLICT`，列 identities、authority/freshness、impact；
- 用户术语承重但含糊：停在定义；
- existing-project claim 与 source 冲突：先解决，不继续假设；
- research 不能证明结论：缩窄 claim，不伪造 certainty；
- Discovery completion 不满足：不得切到 F3。

## Completion 与 handoff

`initial_discovery` 只有在 problem/stakes/context、sources、facts/assumptions、decision agenda、accepted/rejected directions 和 blocking research 足以让 F3 不必重新做 Discovery 时完成。此时记录 `currentNode=product_scope` 和从 discovery state 形成 product contract 的 first action，然后卸载本 reference，加载 F3 reference；门禁未满足时保持 F2。

`called_subflow` 只在约定的 research/elicitation scope 完成或以明确 failure terminal 停止后退出。只要 `returnContext` 可信，无论成功、拒绝还是 scoped failure，都恢复冻结的 exact caller identity、caller `currentNode` 与 caller `firstUnfinishedAction`，并把本次 delta/evidence 或原始 terminal 附给该动作；不得默认回 F3、重放已完成规划或生成新的调用节点。caller/return identity 不可信时使用上面的 `CALLER_RETURN_IDENTITY_MISSING` terminal，不执行 handoff。

最终路由继续由既有 F1/root routing 按当前最低未完成 intent 选择 F3/F4/F5：初始 Discovery 的最低节点是 F3，普通子流程返回 exact caller，overturned premise 则携带 named consumer notice 恢复 lowest affected node。F2 只提供 handoff facts，不创建第二 routing owner。
