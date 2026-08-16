---
name: bmad-method
description: BMAD-METHOD v6.3.0 经典蒸馏版，选择性吸收至 v6.11.0 的 PRFAQ、planning validation 与 Correct Course 规划层改进。用于 greenfield 或 brownfield 的产品探索、PRD、UX、架构、Epic/Story、readiness、planning validation 与 Correct Course；在项目合同要求时也可作为 Story 交付方法索引。触发词包括“按 BMAD 走”“bmad-help”“下一步做什么”“写 PRD”“出架构”“分 story”“做 project context”“Correct Course”“DP”“GPC”“CP”“CA”“CE”。
---

# BMAD-METHOD 方法论 · 精简通用版

## 定位、authority 与源头

这是以 **BMAD-METHOD v6.3.0** 为结构基线、选择性吸收至 **v6.11.0** 规划层改进的蒸馏版。它同时提供 workflow 路由和可执行的 planning 方法，但不安装官方包、不复制项目专属合同，也不自动创建交付角色。

项目自己的 `AGENTS.md`、accepted decisions、identity-bound artifacts、角色模板和权限始终优先。技能只能约束如何完成已获准任务，不能扩大任务或工具权限。

**源头**：https://github.com/bmad-code-org/BMAD-METHOD · MIT License · 结构基线 v6.3.0，规划层更新核对至 v6.11.0（Create Epics and Stories 与 Correct Course 行为核对 commit `9ce3c397c9b238de96f7365da8019f6f66b059da`）。

**为什么不装官方 npm 包**：官方包会展开数百个 workflow 文件；本技能只保留实际规划需要、无法稳定从常识重建的路由、步骤、状态和门禁。

## 核心信条

1. **Facilitator，不是静默生成器**：AI 与用户共同形成承重决定；推荐不能冒充用户决定。
2. **Accepted-state reconstruction**：先从 Git、accepted artifacts、decision log、状态索引和直接证据重建当前事实，再找首个未闭合门禁；不重做身份可证明已完成的工作。
3. **Evidence first**：区分已证明事实、可复现推论和 `UNKNOWN`；来源不明的材料不能升级为 accepted 决定或权限。
4. **No guessing**：缺少会改变产品、UX、Architecture、ownership、scope、Story、acceptance 或 evidence 的输入时停在当前 step，由用户决定。
5. **Minimum causally complete**：只处理当前目标和直接 ripple，不加入推测性功能、未来兼容层、顺手重构或一次性抽象。
6. **One workflow, one current node**：一次只执行当前 workflow；完成后按项目合同交回，不自动串行下一 workflow 或角色。

## 四阶段与 workflow 路由

| 阶段 | 目标 | 常用 workflow |
|---|---|---|
| 1. Analysis | 理解 WHY | DP document-project、GPC project-context、BP brainstorming、MR/DR/TR research、CB product-brief、WB PRFAQ |
| 2. Planning | 确定 WHAT | CP create-prd、VP validate-prd、EP/Edit edit-prd、CU create-ux-design |
| 3. Solutioning | 确定 HOW | CA create-architecture、CE create-epics-and-stories、IR implementation-readiness |
| 4. Implementation | 实际交付 | SP、CS、VS、DS、CR、QA、ER；只作方法索引，项目正式模板优先 |

典型规划路径是 `CP → VP → (EP) → CU → CA → CE → IR`，但只补 accepted state 中首个真实缺口。CC Correct Course 是跨阶段的 planning correction，不是 implementation 交付。已批准 planning candidate 在完整 Planning Review 或 Consistency Audit 后只有 bounded findings 时，优先走后文 `Post-validation Planning Repair fast path`，不得重启 CE 或 Correct Course。Brownfield 先重建 accepted state，再决定是否需要 DP/GPC；已有可靠上下文时不机械重做。PRFAQ 在进入 PRD 前证明 customer、problem、stakes 与 solution，把事实和假设分开，并产出 verdict 与 PRD distillate。

### 三类执行必须分开

1. **Workflow 路由**：回答“下一步做什么”，只判断当前状态、依赖、产物和门禁，不代替目标 workflow。
2. **Interactive planning workflow**：CP、CU、CA、CE、Edit、Correct Course 及其他创建/修改型 planning 使用下述通用交互协议，由同一 Planner 对话逐 step 与用户共同完成。
3. **Independent planning validation/readiness Review**：VP、IR、fresh Planning Review 独立验证 candidate，不与用户共同设计 candidate；规则见后文独立 Review 章节。

Implementation 交付不属于 interactive planning。项目有正式 Writer/Reviewer 合同时，BMAD 只到 exact Story `READY`，随后交回主管理。

## 创建/修改型 planning 的通用交互协议

本节是所有 interactive planning workflow 的唯一通用状态合同。具体 workflow 章节只定义专属 step 内容；不得用“用户要求一次规划完整”“批量模式”或“尽快完成”绕过这里的停止点。一次规划完整表示同一 Planner 对话负责完整 workflow，不表示一轮回复静默完成。

### 1. 启动与第一轮

先读项目 authority 和当前 workflow 直接相关输入，不加载无关历史。第一轮必须展示：

- workflow 目标；
- 本次有序步骤；
- 已发现并准备纳入的 accepted inputs；
- missing inputs 与 optional inputs；
- 初始 decision agenda；
- 当前只处理的第一个 step。

如果项目提供 output artifact，则先确认路径和既有身份；未经 workflow 指定的输入确认，不得把候选初始化为 accepted artifact。

### 2. 每轮 elicitation 与决定记录

- 每轮最多提出 **3 个**真正 load-bearing 问题。只有会改变产品、UX、Architecture、ownership、scope、Story、acceptance 或 evidence 的决定才要求用户选择；其余按 accepted facts 继续。
- 每次用户回答后，先记录决定及条件，说明它对当前规划结构的直接影响，更新 pending decisions，再继续当前 step 或显示当前 step 的菜单。
- 用户未回答的承重问题保持 pending；Planner 不代选，不用推荐、默认值或 `UNKNOWN` 偷渡结论。
- 用户提出材料性调整时，显式更新受影响的 coverage、dependency/DAG、ownership、下游内容和 validation；不隐藏 ripple。

`NEEDS_USER` 不能替代 Planner 与当前用户的正常 elicitation。只有用户不在当前对话、权限/authority 缺失，或项目合同要求返回主管理时，才使用终态 `NEEDS_USER`。

### 3. Step 门禁

一次只执行当前 step，不预做未来 step。每个 step 都按以下顺序：

1. 核验进入条件和当前 step 输入；
2. 与用户完成本 step 的分析、选择和候选；
3. 展示本 step 结果、pending decisions、影响和确认菜单；
4. **停止并等待**；
5. 只有用户明确选择 `Continue` 或明确批准当前 step，才保存本 step 状态并进入下一 step。

聊天、追问、自动上下文压缩、沉默、模糊肯定或“继续完成整个计划”的旧指令都不是当前菜单的 `Continue`。未获确认时留在当前 step，不加载或执行下一 step。

### 4. Artifact 与可恢复状态

Artifact 按 step append-only 渐进构建：只追加已完成 step 的内容，不预填未来 step；当前 step 内可按用户反馈修订其候选。Frontmatter 至少维护：

- `stepsCompleted`：只在当前 step 获得所需确认并完成保存后追加；
- `inputDocuments`：只列用户确认纳入的输入；
- `currentStep`：当前尚未获准退出的 step；
- `acceptedDecisions`：用户已明确接受的决定及条件；
- `pendingDecisions`：仍需用户选择的问题。

草案只能称为 draft 或 planning candidate，不能称为 accepted 或 complete。不得用 bytes 或 hash 作为跨对话 transport gate；identity 与接受状态遵循项目合同。

### 5. 最终批准与 completion

最终完成前必须同时展示：

- 完整规划草案；
- requirements coverage；
- Story/DAG/ownership 或当前 workflow 的等价结构；
- 自检结果、remaining findings、unknowns 和 load-bearing decisions。

然后停止并取得用户对完整草案的明确最终批准。未批准时继续讨论或保持 waiting；不得报告 `COMPLETE`，不得把 candidate 当 accepted，也不得进入实施或下一角色。批准后才记录最后 step、标记 workflow complete，并只交回项目合同规定的下一门禁。

### 6. 自动上下文压缩后的 fail-closed 恢复

自动压缩不是冷启动，压缩不是 `Continue`，压缩不是用户批准。使用系统 summary 和 artifact frontmatter 确认 `currentStep`、`stepsCompleted`、`inputDocuments`、accepted decisions、pending decisions 与批准状态：

- 状态可证明时，从第一个未完成动作继续，不重放已完成 step 或决定；
- 任一关键状态或 approval 无法证明时，重新读取本技能当前 workflow 的相关小节，保持当前 step，并向用户确认；
- 不因摘要写了“完成计划”或候选存在而自动越过菜单或最终批准。

## CE · Create Epics and Stories 四阶段协议

CE 只能按下列唯一顺序执行。每个 step 的确认和停止行为沿用通用交互协议。

### Step 1 — Validate prerequisites and extract requirements

**进入条件**：已路由到 CE，并已重建 accepted state。

1. 在第一轮说明 CE 目标与四阶段计划。
2. 发现并列出 PRD、Architecture、UX、research、project context 和项目指定输入，区分纳入、排除、缺失、optional；请用户确认纳入、排除和补充项。
3. 缺少会改变规划的必需输入时停止，不猜。
4. 从用户确认的完整输入中提取并展示 FR、NFR、Architecture requirements、UX requirements 和 additional requirements，形成 requirements inventory。
5. 让用户明确确认输入集合与 requirements inventory；有修订则留在 Step 1。

**退出条件**：用户明确确认后，才初始化或更新 planning artifact；把确认输入写入 `inputDocuments`，追加 inventory，记录 Step 1，并在 `Continue` 门禁后进入 Step 2。

### Step 2 — Collaboratively design Epics

**进入条件**：Step 1 已记录完成，requirements inventory 已确认。

1. 解释 Epic 设计原则：按用户价值组织，不按数据库、API、UI 等纯技术层拆分；每个 Epic 尽可能独立交付完整价值，不依赖未来 Epic 才能成立。
2. 展示完整 Epic list、每个 Epic 的目标、覆盖要求、dependency 和必要的 ownership/risk boundary。
3. 建立并展示 requirements coverage map；讨论分组、顺序、重叠和遗漏，按用户反馈调整。
4. 请求用户明确批准完整 Epic 结构。

**退出条件**：只有完整 Epic list 与 coverage map 获得明确批准，才追加并记录 Step 2，在 `Continue` 门禁后进入 Story 创建。未批准时留在 Step 2，不生成最终 Story 集，也不宣称方向已确定。

### Step 3 — Create Stories sequentially

**进入条件**：只使用 Step 2 已批准的 Epic 结构。

按 Epic 顺序逐个处理，不先静默生成全部 Story：

1. 先展示当前 Epic 的目标、覆盖要求、dependency 和适用 constraints。
2. 与用户形成当前 Epic 的 Story breakdown。每个 Story 至少包含：user/business value 或明确治理价值、objective、prerequisite、old→new、scope、non-goals、ownership/lifecycle、acceptance criteria、evidence/human gate、单 Writer/单 Reviewer capacity。
3. Story 只能依赖已完成或先序节点；不得依赖同 Epic 或后续 Epic 中尚未完成的未来 Story，除非项目合同允许且 DAG 明确表达。
4. 当前 Epic 完成后展示 Story 集、coverage 与依赖，取得用户确认后才追加该 Epic，并进入下一 Epic。
5. 若发现新的承重决定，按通用协议每轮最多询问 3 项并等待；取得决定后更新 coverage、DAG、ownership 和所有受影响下游 Story。

**退出条件**：每个 Epic 分别确认并追加；全部已批准 Epic 处理完后，记录 Step 3，在 `Continue` 门禁后进入 Step 4。

### Step 4 — Final validation

**进入条件**：全部 Epic 的 Story 集已逐个确认。

1. 验证全部 requirements 有 Story/AC coverage。
2. 验证 Architecture、UX、data、ownership、lifecycle 和 evidence 责任一致。
3. 验证每个 Story 可由单 Writer 独立实施、单 Reviewer 独立 Review 和合并。
4. 验证 DAG 无环、无 orphan、无未表达的 forward dependency。
5. 验证没有 unresolved load-bearing decision；unknown 必须分类并说明影响。
6. 按通用最终批准协议展示完整草案、自检和 remaining unknowns。

**退出条件**：只有用户明确最终批准，才记录 Step 4 并报告 CE complete；随后只交回项目合同的下一门禁，不自动实施或派发角色。

## Correct Course 六阶段协议

Correct Course 使用同一通用交互协议。局部问题可选 incremental 展示，跨 artifact 问题可选 batch 展示；模式只改变提案呈现粒度，不取消逐 step 确认、用户决定或最终批准。

### Step 1 — Establish trigger and evidence

**进入条件**：已路由到 Correct Course。普通 bounded post-validation findings 受后文 fast path 的优先规则约束；只有主管理按该门禁确认结构性失效后，才可把对应 finding 路由到 scoped Correct Course。

明确触发问题、当前症状、成功标准和支持证据；区分 accepted facts、reproducible inference 和 unknown。发现并确认必需 PRD、当前 Epic/Story authority 与项目指定输入。缺少明确 trigger、证据或必需 authority 时停止。

**退出条件**：用户确认 trigger/evidence 边界和输入集合后，记录 Step 1；经 `Continue` 进入 Step 2。

### Step 2 — Complete impact analysis

**进入条件**：Step 1 已完成。

检查当前 Epic/Story、剩余 Epic/Story，以及受影响的 PRD、Architecture、UX、data、evidence、readiness 和项目指定 artifact。区分 direct impact 与 downstream ripple；只跟随受影响关系，不加载无关历史。

**退出条件**：展示完整 impact map、findings 与 unknowns，用户确认分析边界后记录 Step 2；经 `Continue` 进入 Step 3。

### Step 3 — Evaluate paths and decisions

**进入条件**：impact analysis 已确认。

比较 direct adjustment、rollback/revert、MVP/scope review 或项目适用路线，展示各自权衡、风险、影响和推荐。会改变产品、Architecture、ownership、scope 或用户行为的路线必须由用户选择；Correct Course 不得把推荐冒充决定。

**退出条件**：所有承重路线决定由用户明确选择并记录后，展示选定方向；经 `Continue` 进入 Step 4。未决定则留在 Step 3。

### Step 4 — Draft exact proposal

**进入条件**：Step 3 路线和承重决定已确认。

形成完整 proposal：Issue Summary；Minor / Moderate / Major 分类；逐项 old→new 与理由；受影响 artifact、Story/DAG、ownership/lifecycle 调整；精确 non-goals、acceptance、evidence 和 handoff。材料性 ripple 必须显式纳入。

**退出条件**：向用户展示完整 proposal 和 remaining unknowns；处理反馈并保持 planning candidate。用户确认可以进入批准门禁后记录 Step 4，经 `Continue` 进入 Step 5。

### Step 5 — Obtain explicit approval

**进入条件**：完整 proposal 已展示。

回答问题并按反馈修订，随后要求明确 `yes/no` 批准并记录全部条件。`no` 或 `revise` 返回适用的先前 step；未取得明确 `yes` 时只能保留 planning candidate，不实施、不报告 complete。

**退出条件**：仅明确批准后记录 Step 5，并进入 Step 6。

### Step 6 — Complete and hand off

**进入条件**：Step 5 的明确批准可证明。

完成 approved planning candidate，复核批准条件、直接 ripple 和下一责任。到 exact `READY` Story 时交回主管理；不进入代码，不创建或派发 Writer/Reviewer，不 merge 或 push。

**退出条件**：展示最终批准内容和 handoff 后记录 Step 6，Correct Course complete。下一动作仍由项目合同和主管理决定。

## Independent planning validation、readiness 与 Planning Review

VP、IR 和 fresh Planning Review 是独立审查，不使用 interactive planning 的共同设计步骤：

- 从 accepted base、exact candidate identity、适用合同和直接证据独立检查当前授权节点；不继承 candidate 自评或旧 verdict。
- 完整覆盖 requirements、Architecture、UX、data、ownership/lifecycle、Story/DAG、acceptance、evidence、Git/identity 和 protected state 中适用的轴。
- 发现一个 finding 只阻止 `PASS`，不停止其他可执行轴；完成全部适用轴后一次性返回 atomic findings batch。
- 只有 objective authority blocker 才向用户请求输入；权限、安全或 claim-proving evidence blocker 使剩余轴无法执行时，停止并如实报告，不转成共同设计。Reviewer 不与用户共同设计 candidate，也不代替 Planner 取得产品决定。
- Readiness 是实施硬门禁：未通过 IR 或项目等价 readiness，不进入代码。Review 不能把 draft/candidate 升级为 accepted；正式 Reviewer 的编辑、merge 和 push 权限由项目模板决定。

## Post-validation Planning Repair fast path

本 fast path 只处理已批准 planning candidate 在完整 Planning Review 或 Consistency Audit 后收到的 bounded findings，并优先于普通 workflow 路由：route to post-validation Planning Repair, **not CE Step 1 or Correct Course Step 1**。Do not replay CE 的四阶段或 Correct Course 的六阶段；candidate 版本变化本身不是升级理由，也不得重新确认 accepted inputs、locked decisions、Story inventory、DAG、scope、ownership 或 unaffected content。

1. 读取 current candidate、complete identity-bound finding artifact，以及 only directly affected accepted sources。Review 或 audit 即使先发现 must-fix，也必须继续全部适用轴并一次性返回 **complete atomic findings batch**；不得逐条 Repair 或逐条交付 finding。
2. 把完整 batch 作为一次 atomic Repair，保留 unaffected candidate content 与 accepted decisions，产出一个修复全部 finding 的 next-version complete planning candidate。新版本在 fresh independent validation 通过前仍是 planning candidate。
3. 没有新的 load-bearing decision 时直接 Repair，不开启新的用户批准循环。若 finding 暴露一个新的承重决定：**Ask only the new load-bearing decision**, 记录答案并 **resume the same Repair**；不得重问既有决定或重启 workflow。
4. 若 finding 使 **Epic/Story structure**、core Architecture、**core ownership/data responsibility** 或 overall product scope 失效，立即停止 fast path，准确报告失效边界并把控制权交回主管理，由其决定是否授权 **scoped Correct Course**。Planner 不得自行扩权或静默扩大 Repair。
5. 同一 Repair 对话发生自动上下文压缩时，使用 summary 与 candidate state 从 **first unfinished Repair action** 恢复；不得把压缩当作冷启动，不得重放已完成工作或门禁，也不得重启 CE、Correct Course 或本 fast path。

严格执行以下门禁顺序，不得跳步：

```text
Planning Review FAIL
→ repair the complete atomic findings batch once
→ fresh full re-Planning Review
→ after PASS, fresh Consistency Audit

Consistency Audit FAIL
→ repair the complete atomic findings batch once
→ fresh full re-Planning Review
→ after PASS, fresh full re-Consistency Audit

Only after both current candidate gates PASS
→ tracked docs sync or the next gate required by the project contract
```

每次 Planning Repair 后都必须重新执行完整 Planning Review。Planning Review 未 PASS 时不得进入 Consistency Audit 或 re-Consistency Audit；Consistency Audit 失败后的 Repair 也不得直接跳回 audit。

## Implementation 与正式角色边界

- BMAD 只决定做什么、为什么、范围、边界和验收。到一个 exact ready Story 后停止并交回主管理；不得自动创建 subagent、Writer、Reviewer、状态机或角色流水线。
- 项目正式 Writer/Reviewer 模板拥有 TDD、根因调试、完成前验证、代码 Review、Git、人工/UI/设备 evidence 和 integration 顺序；本技能的 DS/CR 等索引不能替代它们。
- Story 在 READY 前绑定准确 production modification set、ownership/lifecycle、错误分类和 evidence 层级；超出单 Writer 因果完整交付或单 Reviewer 独立判定能力时，先拆 Story 或 Correct Course。
- Repair 若需要新增核心 interface、platform wrapper、callback owner、scheduler/test seam，或跨越未批准的多模块责任边界，触发 E16 结构性失败门禁并回到 scoped Correct Course。连续完整 Review 的 finding 显示核心责任仍不稳定时，不用局部补丁堆叠。
- Test seam、helper、源码字符串、fake/no-op、AVD 或注入点不能冒充 production wiring、真实设备、权限、并发或外部系统 evidence。

## 证据优先与反过度工程

1. 先定义问题、成功标准和可观察证明，再形成 candidate。
2. 信任 accepted contract、类型、测试和框架保证；只在用户输入、网络、外部 API、设备等真实边界增加验证。
3. 不为 accepted contract 排除的不可能状态增加 fallback、空值默认或防御分支；不宽泛 catch、吞错或静默降级。真实 invariant 失败应 fail-fast 并保留原始信号。
4. 规划只包含当前目标与直接因果 ripple；不加入未批准的功能、兼容层、manager、registry、adapter、wrapper 或一次性 helper。
5. 可读且 identity-bound 的 immutable artifact 承载任务正文；下游合同引用它，不复制 old→new、AC 或验证矩阵。

## 参考索引

本技能只保持一层本地引用：

- `references/templates/project-context-template.md` — GPC 产出骨架
- `references/templates/prd-template.md` — PRD 产出骨架
- `references/templates/architecture-decision-template.md` — CA 产出骨架
- `references/templates/epics-template.md` — CE 产出骨架
- `references/workflow-catalog.csv` — BMAD workflow 官方描述索引

扩展阅读：https://docs.bmad-method.org 。升级前比较实际能力、项目需要和上下文成本；不得仅因上游文件更多就引入整包。

## 每次使用时

1. 读取项目 authority 并重建 accepted state。
2. 区分本次是路由、interactive planning、independent Review，还是已到 implementation 边界。
3. 选择最低必要规划高度，定义当前产物、成功标准和验证方法。
4. 执行当前 workflow 的唯一顺序和门禁；完成后按项目合同交回，不自动运行下一 workflow 或角色。
