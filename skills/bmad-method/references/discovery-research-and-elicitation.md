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

不要把用户、buyer、payer、operator、admin 合并，除非 source/decision 明确同一角色。

### Source quality 与 freshness

优先级依 claim 而定：官方 docs/source/law/standard/primary data > first-party current material > reputable analysis > secondary summary。记录 publication/effective/version date、访问日期、适用地域/版本和 conflict。对易变 claim 设 freshness window；过期 evidence 标 `STALE`，不能用模型记忆补齐。

至少核对：source 是否真的支持 claim、单位/分母/定义是否一致、引用是否可追、结论是否超出证据、反证是否存在。

### Selection research

先定义不可妥协 hard gates，再由 decision owner 接受 criteria/weights。每个 candidate 记录 evidence、confidence、unknown 和 gate result。完成 sensitivity：改变合理权重是否改变排序；若改变，推荐应表述为条件式而非绝对结论。

### Refresh / deepen / stop

- `refresh`：decision 相同但易变 evidence 过期；只更新受影响 claims。
- `deepen`：已有 evidence 不足以区分 viable routes；研究最能改变决策的 gap。
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

只有 problem/stakes/context、sources、facts/assumptions、decision agenda、accepted/rejected directions和 blocking research 足够让 F3 不必重新做 Discovery 时完成。记录 `currentNode=product_scope` 和从 discovery state 形成 product contract 的 first action，然后卸载本 reference，加载 F3 reference。
