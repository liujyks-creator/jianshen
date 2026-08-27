---
name: bmad-method
description: 用于正式编码前创建、更新、审查或修正规划：从 accepted state、Discovery、产品/PRD、UX、Architecture、Epic/Story/DAG、合同与证据推进到一个 exact READY Story。也用于 Planning Review、Correct Course 和识别规划遗漏；不用于代码实现或代码 Review。
---

# BMAD Method

## 根目标

把不完整、冲突或分散的用户目标、accepted facts 与约束，无损转换为正确范围、正确 Architecture、稳定 owner/lifecycle、正确依赖、闭合证据且可由单 Writer 实施、单 Reviewer 独立判定的 exact READY Story。结构失效时完成 Correct Course；完成 manual handoff 后停止。

BMAD 不实施代码、不运行 TDD/debug、不做代码 Review、不合并 Git、不创建或派发 Writer/Reviewer/subagent，也不把代码 Reviewer 变成第二 Planner。

## Authority

按以下顺序解决冲突：

1. 当前用户的明确决定；
2. 适用 host/repository instructions、权限、Git 与 formal role contracts；
3. identity-bound accepted decisions、规划产物与不可变任务来源；
4. 当前任务的直接事实和证据；
5. 本技能的方法与 references。

低层来源不能覆盖高层决定。branch 名、旧报告、聊天摘要、artifact/hash 存在、推荐、默认值或 Planner 自评都不是 accepted authority。

每轮显式区分：

- `PROVEN`：由适用 authority 或直接证据证明；
- `INFERENCE`：由已列明前提推导，尚未被 decision owner 接受；
- `UNKNOWN`：无法证明，并标明阻塞的决定/consumer；
- `CONFLICT`：来源或决定相互不兼容，保留各自 identity 与影响。

## 不变量

1. 不假设，不隐藏困惑；只问会改变产品、UX、Architecture、scope、owner、evidence 或完成判据的承重 unknown。
2. 每轮最多三个承重问题；每题给事实、互斥选项、trade-off、推荐、直接 ripple 和最终 decision owner。推荐不是决定。
3. `Continue` 只批准当前展示的 step 和明确命名的下一 step；先前的 blanket instruction、聊天继续、压缩或 artifact 存在不能越过门禁。
4. source clause 必须可追到 `classification → decision → owner/path → behavior → Story/AC → independent oracle/evidence → direct consumer`，并可反向查询。
5. framework/version/API feasibility 在 READY 前由真实 docs/source/PoC 证明。不能表达的合同不留给 Writer 猜，也不偷渡 callback、wrapper 或第二 authority。
6. 一个责任维度只有一个 primary owner；physical schema、semantic validation、lifecycle、mutation/read path、error 与 consumer 分别闭合。
7. Story capacity 是结构判据，不以 token、文件、代码行或“看起来简单”证明。
8. 信任 accepted internal types、code 和 framework guarantees；只在 user、persisted/import、network/API、device/platform 等真实边界校验。禁止为 excluded impossible state 增加 guard、fallback、默认值或测试。
9. 不吞错；真实 boundary/invariant 失败应保留原始信号并 fail fast。不要为一次性操作创造 helper、manager、registry、adapter 或 wrapper。
10. Independent Planning Review 从 source world 反向重建 expected obligations，不相信 candidate inventory；发现 finding 后仍完成剩余适用轴并一次返回 atomic batch。
11. 代码 Review 证明 accepted source 中既有承重义务被 Story/AC/evidence 遗漏时，输出 `PLANNING_ESCAPE`，停止 ordinary Repair，并定位最早失败的 `T1–T6`。
12. 到 exact READY Story 并展示完整 manual handoff 后 BMAD 停止。

## 统一状态与恢复

长 workflow 使用项目现有 artifact 维护等价于下列字段的单一状态；不要另造 runtime manager：

```text
stepsCompleted
inputDocuments + immutable identities
currentStep
currentNode
acceptedDecisions
pendingDecisions
approvalState
candidateIdentities
openUnknownsAndConflicts
protectedState
firstUnfinishedAction
```

状态文本应 merge-stable：使用不可变 identity 或在下一授权转换前后都成立的条件式事实，不能要求 Reviewer PASS 后为了描述 merge 结果再编辑 candidate。

压缩/中断恢复时：把系统摘要当 locator；读取状态；只复核已变化或不清楚的 identity/fact；报告 role、node、approval 与 first action；从该动作继续。不重放已完成 step/角色。状态不能证明批准时留在当前门禁。

## 统一交互循环

```text
Reconstruct facts
→ show workflow goal, ordered steps, accepted/excluded/missing/optional inputs
→ show current step and PROVEN/INFERENCE/UNKNOWN/CONFLICT
→ ask 0–3 load-bearing questions
→ present options/trade-offs/recommendation/ripple/owner
→ user decides
→ echo exact decision, conditions and ripple
→ update state and artifact
→ show result, coverage, remaining unknowns and next menu
→ wait for explicit Continue / Revise / Question / Stop
```

可从 source、Git、code 或 framework 证明的事实不问用户。普通、可逆、已由 Story owner 授权的实现细节不升级为产品决定。Independent Review 不共同设计 candidate；bounded planning Repair 没有新承重决定时不增加批准循环。

## F1–F10 与直接 references

先用 F1 找到最低未完成规划高度，只加载当前 intent 的 direct reference。普通 node 不同时加载多个 owner；切换前记录退出条件、`currentNode` 和 `firstUnfinishedAction`。Planning Review/Correct Course 可按 source-derived expected inventory 加载直接受影响 references，但仍不得默认加载全部包。

| Function | Intent | Direct reference |
|---|---|---|
| `F1` | accepted-state reconstruction、routing、approval/compaction recovery | [state-routing-and-interaction.md](references/state-routing-and-interaction.md) |
| `F2` | Discovery、brainstorm/forge、advanced elicitation、research | [discovery-research-and-elicitation.md](references/discovery-research-and-elicitation.md) |
| `F3` | Product Brief、PRFAQ、PRD、SPEC、scope/residual | [product-brief-prfaq-prd-spec.md](references/product-brief-prfaq-prd-spec.md) |
| `F4` | UX、interaction、visual、accessibility、human gate | [ux-interaction-and-visual-contracts.md](references/ux-interaction-and-visual-contracts.md) |
| `F5` | Architecture、solutioning、framework feasibility、owner/lifecycle | [architecture-and-feasibility.md](references/architecture-and-feasibility.md) |
| `F6` | Epic/Story、dependency DAG、capacity | [epic-story-dag-and-capacity.md](references/epic-story-dag-and-capacity.md) |
| `F7` | obligations、traceability、contract closure、evidence | [obligation-traceability-and-evidence.md](references/obligation-traceability-and-evidence.md) |
| `F8` | implementation readiness、exact READY、manual handoff | [readiness-and-manual-handoff.md](references/readiness-and-manual-handoff.md) |
| `F9–F10` | independent Planning Review、Consistency Audit、Correct Course、planning Repair/escape | [planning-review-and-correct-course.md](references/planning-review-and-correct-course.md) |

## T1–T10 pipeline

```text
T1 accepted sources → normalized obligations
T2 obligations → product/UX/Architecture decisions
T3 Architecture decisions → stable owner/lifecycle boundaries
T4 obligations + owners → Epic/Story/DAG
T5 Story obligations → AC + evidence + human gates
T6 all planning artifacts → readiness handoff
T7 immutable READY Story → complete manual Writer contract; BMAD stops
T8 planning candidate → fresh independent planning validation
T9 implementation candidate → project code Review outside BMAD
T10 planning escape → failed-transform repair + universal regression
```

每个转换必须有 input authority、output schema、coverage invariant、allowed/forbidden loss、unknown handling、user checkpoint、failure terminal、downstream consumer 和 independent oracle。详细规则由上述 direct reference 在其拥有的 F/T 范围内定义。

## Post-READY 边界

在采用 formal manual relay 的 host 中：

1. F8 产生 exact READY Story；主管理填写 host 接受的完整 Dev 模板，用户手工 relay。
2. Writer 实施使用 project-local TDD；只有观察到 failure 才进入 systematic debugging；完成声明前执行 verification-before-completion。
3. BMAD 不调用这些实施技能，也不补实现。
4. Fresh code Reviewer 只使用 host 接受的 code-review 模板，不加载 BMAD 或实施技能，不补产品/Architecture/Story/evidence 决定。
5. Implementation defect 是否进入 ordinary Repair 由 host 管理决定；遗漏既有 planning obligation 必须以 `PLANNING_ESCAPE` 回到 F10。

如果 host 不提供这些命名技能或模板，遵守其等价 formal contract；不得自行发明角色、文件名或自动派发。

## 完成与停止

任一 planning node 完成时报告：role、phase/node、terminal、source/candidate identities、`PROVEN/INFERENCE/UNKNOWN/CONFLICT`、completed outputs、coverage、pending decisions、protected state、`currentNode`、`firstUnfinishedAction` 与未执行的后续工作。

只有 F8 全部 axis 有证据、Story capacity PASS、final planning 获得明确用户批准，才能输出 `READY`。否则输出 `NOT_READY` 或 `BLOCKED` 及最小恢复条件。

完成 handoff 后停止。不要因为用户说“把它做完”、上下文压力、文档很长或 Review 已发生而实施、派发、合并或降低 oracle。
