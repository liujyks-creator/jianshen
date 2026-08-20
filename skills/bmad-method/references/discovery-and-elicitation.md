# Discovery 与 Elicitation

## When to load

当 `currentNode = discovery_intake`，产品意图仍模糊，需要 Product Brief 的 intake/discovery、brainstorm、来源协调，用户说“帮我一起想”，或当前候选需要 Advanced Elicitation 时加载。它唯一拥有 Product Brief 的发现阶段，不负责 draft/update/validate/completion；已进入 `product_scope`、PRD、UX 或 Architecture node 且 Discovery 状态足够时，不为仪式性重启本流程。

## Goal

在收敛范围前，让用户完整表达真实目标、why-now、现实情境、约束与被拒方向；主动暴露用户尚未想到但会改变规划的 concern，并把发现写入共同状态而不是留在对话记忆里。完成的是可供 Product/Scope 起草 Product Brief 的 discovery state，而不是 Product Brief 本身。

## Inputs

- 用户当前请求与已有叙述。
- 用户指向的 memo、transcript、research、prior artifact、代码或 accepted decision。
- 项目 authority、当前 planning ledger 与直接相关 domain evidence。
- 若为 Update：current artifact、原始 sources、既有 decisions/rejections 和 change signal。

先读用户已经提供的输入；不要再问输入已经回答的事实。每个承重 claim 绑定来源或 `UNKNOWN`，并区分 accepted、candidate 与 historical evidence。

## Collaboration mechanics

### 1. 打开完整表达空间

Coaching 的 first action 是邀请用户做一次完整 brain dump，并同时指出可提供的既有材料。用户已经给出长叙述时，先反射你听到的目标和张力，再明确给一次补充空间；不要立刻切成细节问答。brain dump 后只问一次自然的“还有什么是我应该知道、但我们还没说到的？”

随后校准：

- `stakes`：hobby、internal decision、public launch、regulated/high-risk 等；它决定追问深度，不改变事实。
- `form-factor`：mobile、web、desktop、multi-surface、hardware、API 或组合。
- 工作模式：默认 Coaching；只有用户明确要快时进入 Fast path。

### 2. Coaching 提问纪律

- 每轮只问一个开放问题；不用问题墙，不把 LLM 预制范围做成伪选择菜单。
- 先让用户叙述真实场景，再结构化；“告诉我 Mary 从需要出现到得到结果的全过程”优于先列屏幕或阶段让用户选。
- 答案薄弱时，先反射缺口及其影响，再追问具体例子、反例或失败场景。
- 答案矛盾或承重时，展示证据、真实备选与 trade-off；推荐可明确，但不能写入 `acceptedDecisions`，直到用户接受。
- 用户疲劳或 stakes 低时降低深度；不能因此隐藏 phase blocker。

Fast path 可批量询问最少缺口，但推断逐项标 `[ASSUMPTION]`，未回答项保留在 `openQuestions`，阻塞项保留在 `phaseBlockers`。

### 3. Source preservation 与 reconciliation

对每份输入提取：目标、qualitative intent、named users/journeys、能力、约束、non-goals、rejected alternatives、future language 与承重 claims。对照 current artifact，逐项标：captured、conflict、missing、deferred 或 process noise。

冲突不静默择一：记录双方来源和影响；若会改变 scope、UX、Architecture、ownership、evidence 或下一 phase，加入 `phaseBlockers` 并一次只请用户关闭一个。未冲突且已有 accepted identity 的合同作为 inherited fact，不重新审问。

### 4. Open concern scan

在理解产品情境后主动扫真实 concern，不套固定表。至少考虑是否适用：privacy、security、hardware/device、platform、data governance/retention/export、accessibility、offline/poor network、notifications、operational constraints、permissions、localization、regulated language、external integration、failure recovery。

对每个实际 concern 记录：为什么适用、来源、当前已知、未知、影响哪个 node。只有会改变下一规划高度安全性的未知才成为 `phaseBlockers`；其余进入 `openQuestions` 或带 owner/revisit condition 的 `deferred`。

### 5. 发散与收敛分离

产品方向尚未充分展开时只发散：改变视角、约束、stakeholder 或反例，保留怪异与被拒候选，不边生成边排序。用户表示方向已充分展开或要求选择时才收敛：先把完整候选场反射回来，再选一个适合目标的筛选方法（如 affinity、impact-effort、NUF、PMI、MoSCoW），由用户给出价值判断。记录 surviving direction 与 rejected alternative 的理由。

不得因为已有一条看似可行路线就跳过未探索的用户场景、scope 边界或 concern。

### 6. Advanced Elicitation checkpoint

当出现一个 load-bearing candidate 或准备离开 Discovery 时，按风险只选一个最有穿透力的方法：

- pre-mortem：假设计划失败，倒推遗漏；
- assumption audit：按影响与置信度压力测试 assumptions；
- boundary sweep / inversion：寻找 zero/null/max/reverse/类型或必败路径；
- stakeholder lens：从遗漏用户、operator、owner、reviewer 看候选；
- map-is-not-territory：比较文档模型与真实产品行为；
- second-order / red-team：检查下游连锁和可被攻击的前提。

先展示“方法发现了什么、建议改变什么、若不改的风险”；等待用户接受、拒绝或修订。只有接受的改变进入 candidate/decisions；拒绝项进入 `rejectedAlternatives`。完成后恢复原 `currentNode` 与 `firstUnfinishedAction`，不启动新 workflow。

## State and output

Discovery 产物使用项目既有 Product Brief 的 discovery 区域、intent artifact 或 planning ledger，至少包含：

- problem/why-now、named user 与现实情境；
- goal、success signal、counter-signal；
- qualitative intent、voice/experience intent；
- applicable form-factor、stakes 与 concern scan；
- source reconciliation；
- candidate directions、rejected alternatives；
- shared state 的 assumptions/openQuestions/phaseBlockers/deferred/sources；
- `currentNode` 与 `firstUnfinishedAction`。

不要为了套模板填充没有价值的章节；也不要在本 node 起草、更新、验证或宣布完成 Product Brief，更不要把尚未共同决定的内容写成完整 PRD 或 Story。

## Blockers

暂停并保持在当前 node，当：

- 用户目标、named user、核心情境或 form-factor 的缺失会改变产品方向；
- source conflict 会改变 scope/UX/Architecture/ownership/evidence；
- concern 暴露下一阶段无法安全承担的未知；
- 需要新 authority、permission 或 load-bearing user decision。

普通措辞、章节顺序和非承重结构选择不是 blocker，由 Planner按已知目标处理。

## Completion / readiness checks

Discovery 只有在以下条件满足时才可交给 Product/Scope：

- 用户已得到完整 brain dump 与一次“还有什么”的机会；stakes、form-factor 和模式已校准；
- 至少一个真实用户/情境、why-now、目标与输入来源可追；
- applicable concern 已扫，source conflicts 已关闭或显式阻塞；
- 发散发生在收敛之前，承重方向由用户选择；
- assumptions、openQuestions、phaseBlockers、deferred 与 rejected alternatives 没有被草案长度掩盖；
- Advanced Elicitation 的发现已展示，只有用户接受的改变进入候选；
- 退出时原子记录 `currentNode = product_scope`，并把 `firstUnfinishedAction` 设为“从已协调的 Discovery 状态 draft/update Product Brief”的首个具体动作；
- 状态写入后卸载 Discovery reference，再只加载 Product/Scope reference。任一 completion 未满足时保持 `currentNode = discovery_intake`，不得提前加载 Product/Scope 或同时加载两个 reference。
