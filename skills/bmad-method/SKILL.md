---
name: bmad-method
description: BMAD-METHOD v6.3.0 经典蒸馏版，选择性吸收至 v6.11.0 的 PRFAQ、planning validation 与 Correct Course 规划层改进。用于 greenfield 或 brownfield 的产品探索、PRD、UX、架构、Epic/Story、readiness、planning validation 与 Correct Course；在项目合同要求时也可作为 Story 交付方法索引。触发词包括“按 BMAD 走”“bmad-help”“下一步做什么”“写 PRD”“出架构”“分 story”“做 project context”“Correct Course”“DP”“GPC”“CP”“CA”“CE”。
---

# BMAD-METHOD 方法论 · 精简通用版

## 这 Skill 是什么

这是以 **BMAD-METHOD v6.3.0** 为结构基线、选择性吸收至 **v6.11.0** 规划层改进的蒸馏版本，将官方大型工作流压缩为一份可直接使用的**方法论手册 + 关键模板参考**。项目自己的 `AGENTS.md`、accepted decisions、角色模板和权限始终优先；本技能不复制项目专属合同，也不自动创建角色或交付流程。

**源头**: https://github.com/bmad-code-org/BMAD-METHOD · MIT License · 结构基线 v6.3.0，规划层更新核对至 v6.11.0
**为什么不装官方 npm 包**：官方包会展开数百个 workflow 文件；本技能只保留完成实际规划任务所需的路由、产物和门禁。

---

## 核心信条 (读懂 BMAD 先读这)

1. **AI 不替你思考, AI 陪你思考**. BMAD 不是"输入需求自动出代码", 是 **facilitator** — 在 PRD/架构/story 每步做 structured elicitation 把你脑子里的东西挖出来并结构化.
2. **fresh chat per workflow**. 每个 workflow（product-brief / create-prd / create-architecture / etc.）按项目合同在独立任务对话中运行，避免一个对话连跑多个角色或 workflow。
3. **step-file architecture**. 每个 workflow 拆成 micro-files (step-01, step-02...). 一次只加载当前 step, 做完等用户按 Continue 才进下一步. **永不同时加载多个 step**.
4. **append-only document building**. PRD / Architecture / Stories 这类产物是**一步步追加**建起来的, 不是一次性写完再改. frontmatter 里 `stepsCompleted: []` 追踪状态.
5. **不猜**。缺少会改变结论的产品、scope、ownership 或架构决定时，明确报告 UNKNOWN 和权衡；由项目合同决定是在当前 workflow 取得选择，还是以 `NEEDS_USER` 结束并由新任务继续。
6. **证据先于结论**。区分已证明事实、可复现推论和未知；来源不明的材料不能升级为 accepted 决定或权限。
7. **最小但因果完整**。只解决当前问题和直接 ripple，不把未来功能、一次性抽象或理论防御分支塞进规划。

---

## 4 阶段 · 产品到代码的流水线

```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ 1. Analysis  │──▶│ 2. Planning  │──▶│ 3. Solutioning│──▶│4. Implementation│
│   (理解 WHY) │   │   (定 WHAT)  │   │   (出 HOW)    │   │   (真写代码)   │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
   brainstorm         create-prd       create-architecture   create-story
   product-brief      validate-prd     create-epics-stories  dev-story
   document-project   create-ux-design check-impl-readiness  code-review
                                                             retrospective
```

每阶段下的 workflow 都是一个 slash-style skill. 阶段间有硬依赖 (CSV `before`/`after` 字段), 不能跳.

---

## Workflow 完整索引 · 按阶段

### 🔍 阶段 1 · Analysis (理解 WHY)

| 代号 | Workflow | 何时用 | 产出 |
|:---:|---|---|---|
| **DP** | `bmad-document-project` | **brownfield 首步**. 让 agent 扫你已有代码库生成 AI 友好文档 | project-knowledge/ 目录 |
| **GPC** | `bmad-generate-project-context` | brownfield 必做. 生成 LLM 优化的 `project-context.md` (AI coding 规则) | `project-context.md` |
| **BP** | `bmad-brainstorming` | 想法还没清晰时做发散 | brainstorming session 记录 |
| **MR** / **DR** / **TR** | `bmad-market-research` / `bmad-domain-research` / `bmad-technical-research` | 分别做市场 / 领域 / 技术可行性调研 | research 文档 |
| **CB** | `bmad-product-brief` | 想法清晰了, 出 1-2 页产品简报 | `product-brief.md` |
| **WB** | `bmad-prfaq` | 想法还不确定，要用证据压力测试（Amazon Working Backwards） | PR、Customer/Internal FAQ、verdict 与 PRD distillate |

**brownfield 场景**：先重建 accepted state，再决定是否需要 DP/GPC；已有可靠项目上下文时不得机械重做。

**PRFAQ 规划升级**：先证明 customer、problem、stakes 与 solution；缺少其中会改变结论的输入时回到探索，不代填。按商业、内部、开源或非营利等实际 concept type 调整问题，不套同一商业模板。市场、竞争和可行性 claim 使用当前可核验来源，假设必须显式分开。最终产物同时给出对外 PR、Customer FAQ、Internal FAQ、继续/调整/停止 verdict，以及供后续 PRD 使用的紧凑 distillate。

---

### 📋 阶段 2 · Planning (定 WHAT)

| 代号 | Workflow | 何时用 | 依赖 | 产出 |
|:---:|---|---|---|---|
| **CP** | `bmad-create-prd` | **必做**. 做 PRD (产品需求文档) | CB(推荐) | `prd.md` |
| **VP** | `bmad-validate-prd` | CP 后, 独立对话验证 PRD 质量 | CP | PRD 验证报告 |
| **EP** | `bmad-edit-prd` | VP 报告有问题, 回来改 | VP | 更新的 PRD |
| **CU** | `bmad-create-ux-design` | UI 交互占主导的项目使用 | CP | UX 设计文档 |

典型路径：`CP → VP → (EP) → CU`；只执行当前缺失的门禁。

---

### 🏗 阶段 3 · Solutioning (出 HOW)

| 代号 | Workflow | 何时用 | 依赖 | 产出 |
|:---:|---|---|---|---|
| **CA** | `bmad-create-architecture` | **必做**. 做技术架构决策 | CP | `architecture.md` |
| **CE** | `bmad-create-epics-and-stories` | **必做**. 把 PRD+Arch 切成 epics/stories | CA | `epics/` + `stories/` 目录 |
| **IR** | `bmad-check-implementation-readiness` | **必做**. 跨对照 PRD/UX/架构/stories 有没有对不齐 | CE | readiness 报告 |

**没 IR 过关不能动代码**. 这是 BMAD 的硬闸.

---

### 🔨 阶段 4 · Implementation (真写代码)

两种路径:

#### 路径 A · 正式 sprint 循环 (推荐 · 长项目)
```
SP (sprint-planning) → CS (create-story) → VS (validate-story) → DS (dev-story) → CR (code-review) → 下一 CS ...
                                                                                     │
                                                                                     ↘ ER (retrospective) · epic 结束
```

| 代号 | Workflow | 做什么 |
|:---:|---|---|
| **SP** | `bmad-sprint-planning` | 按 epics/stories 出 sprint 计划 (sprint-status.yaml) |
| **CS** | `bmad-create-story` (action: create) | 从 sprint 里挑下一个 story, 塞上 dev notes 上下文 |
| **VS** | `bmad-create-story` (action: validate) | 独立对话验 story 够不够 dev 下嘴 |
| **DS** | `bmad-dev-story` | **真·写代码**. red-green-refactor. story 里有 halt 条件必须停 |
| **CR** | `bmad-code-review` | **另换一个 LLM** 做 code review (推荐: GPT/Gemini 做 CR 避免同模型盲区) |
| **CK** | `bmad-checkpoint-preview` | 人类 commit/branch/PR 审 |
| **QA** | `bmad-qa-generate-e2e-tests` | DS 完成后生成 E2E 自动化测试 |
| **ER** | `bmad-retrospective` | epic 结束做回顾 |
| **CC** | `bmad-correct-course` | 路子走歪了, 评估是重启 PRD / 重做架构 / 重切 stories |

#### 路径 B · Quick Dev (bug 修 / 小改动)
| 代号 | Workflow | 做什么 |
|:---:|---|---|
| **QQ** | `bmad-quick-dev` | 跳过 SP→CS→DS, **一步从 intent 到 code**. 适合 L1/L2 级改动 |

`QQ` 只适合无需重开产品、架构或 ownership 决定的局部改动。

**Correct Course 规划升级**：只读取受影响的 PRD、UX、Architecture、SPEC、Epic/Story 和适用 `AGENTS.md` 上下文。局部问题用 incremental 模式，跨产物问题用 batch 模式。输出 Issue Summary、直接与下游 Impact Analysis、Minor/Moderate/Major 分类、推荐路线、逐项 old→new 与理由，以及实施交接；获得明确批准前只形成 planning candidate，不进入代码修改。

**交付边界**：v6.11.0 的 `bmad-build`、代码交付 Review、自动实现循环和 Git 集成不进入本技能。项目有正式 Writer/Reviewer 模板时，BMAD 在 exact Story `READY` 后停止；TDD、根因调试、完成前验证、代码 Review 和 Git 交付由项目模板及其指定的实现方法负责。

---

## 每个 Workflow 的运行协议 (共用)

每个官方 BMAD skill 启动时都走这 6 步. 本项目没装官方, 但要遵循精神:

1. **Resolve workflow block** · 查项目根的 `_bmad/custom/<skill>.toml` 做自定义覆盖 (如有). 本项目可以简化, 直接用默认.
2. **Execute prepend steps** · 激活前置钩子
3. **Load persistent facts** · 载入贯穿整 workflow 的事实 (如 `project-context.md`)
4. **Load config** · 从 `_bmad/bmm/config.yaml` 读 user_name / communication_language / document_output_language / paths
5. **Greet the user** · 用 communication_language 问候
6. **Execute append steps** · 激活后置钩子

然后进入 workflow 主体 — step-01.md → step-02.md → ... 按顺序, 每步有 menu 等用户.

**停止与继续规则**：
- 缺少会改变决定的用户选择、权限、必要来源或安全条件 → 停止受影响动作并明确报告。
- Validation/Review 发现一个问题 → 只阻止 PASS，不停止剩余可执行检查；完成当前节点全部适用轴后一次性返回完整 findings batch。
- 只有客观权限、安全或 claim-proving evidence 缺失导致剩余轴无法执行时，才提前阻断。
- 同一局部 Repair 连续失败，且继续会改变核心 ownership、架构、数据职责或多个模块边界 → 转 scoped Correct Course，不继续堆补丁。

---

## 项目适配边界

- 先读项目自己的 `AGENTS.md`、accepted decision log、状态索引和当前 artifact；不要把其他项目的路径、设备、账号或流程写入全局技能。
- 已经 accepted 且身份可核验的 PRD、架构、Story 或 evidence 不重做；只补首个真实缺口及其直接 ripple。
- BMAD 只决定“做什么、为什么、范围、边界和验收”。项目若另有 Writer/Reviewer 模板，READY 后按项目合同交回，不用 BMAD 的 DS/CR 条目取代正式角色。

---

## 如何在本 Skill 加载后触发各 Workflow

本 skill 是**方法论手册**，不是自动执行器。用户触发以下意图时，按对应 workflow 的精神执行：

| 用户说 | 方法 |
|---|---|
| "开始 BMAD 第一步" / "bmad-help" | 看项目状态 (有无 product-brief.md/prd.md/architecture.md 等), 推荐下一步 |
| "做 project context" / "GPC" | 按 `references/templates/project-context-template.md` 格式生成 |
| "写产品简报" / "product brief" / "CB" | 按 product-brief workflow 5 阶段走 (intent / discovery / elicitation / draft / finalize) |
| "写 PRD" / "CP" | 按 create-prd workflow step-file 风格执行 |
| "出架构" / "CA" | 按 create-architecture workflow 做 |
| "分 epics 和 stories" / "CE" | 按 create-epics-and-stories 做 |
| "做 sprint planning" / "SP" | 生成 sprint-status.yaml |
| "dev 下一个 story" / "DS" | 按 dev-story workflow (10 step, red-green-refactor) 实施 |
| "code review" / "CR" | 对刚写的代码做 review (建议用户换个 LLM 进另一个对话做) |

**严格遵守**：
- 是否新开任务、产物路径、语言、终态和人工 checkpoint 由当前项目合同决定。
- 一个 workflow 只负责一个当前节点；不自动串行运行下一 workflow、Writer 或 Reviewer。
- 可读且 identity-bound 的 immutable artifact 是任务正文；下游提示词引用它，不重复展开 old→new、AC 和验证矩阵。

---

## 证据优先与反过度工程

1. 先写清当前问题、成功标准和能证明成功的信号，再形成 candidate 或修改建议。
2. 不假设、不隐藏困惑。事实不足时标明 `UNKNOWN`，说明它影响哪个决定和可选权衡；无关未知不得阻断已证明工作。
3. 只规划当前目标和直接因果 ripple；不加入推测性功能、未来兼容层、顺手重构或一次性 helper/wrapper/manager/registry/adapter。
4. 信任已由 accepted contract、类型、测试或框架证明的内部保证；只在用户输入、网络、外部 API、设备等真实系统边界要求校验。
5. 不为 accepted contract 排除的“不可能状态”增加 fallback、空值检查、默认值或错误处理；不允许宽泛 catch、吞错或静默默认。真实 invariant 失败应 fail-fast 并保留原始信号。
6. 实施型 Story 适用时采用 RED → GREEN → focused regression；若 RED 不适用，必须先定义独立 oracle。不得把 helper 存在、源码字符串、fake/no-op 或注入 seam 冒充生产行为或真实外部系统 evidence。

## 结构性失败门禁

- Story 在 READY 前列出准确 production modification set、ownership、生命周期、错误分类和 evidence 层级；范围大到无法由一名 Writer 因果完整交付和一名 Reviewer 独立判定时，先拆 Story 或 Correct Course。
- Repair 只修完整 approved findings batch。若修复需要新增核心 interface、platform wrapper、callback owner、scheduler/test seam，或跨越未批准的多模块责任边界，停止局部 Repair 并返回 Correct Course，不用更长提示词掩盖架构缺口。
- 测试 seam 只服务可验证性，不能替代 production wiring；fake/no-op 不能证明真实权限、并发、生命周期或外部系统行为。
- Review 必须核对实际 production 变化清单、runtime ownership、race/error 边界、测试声明与 evidence。发现第一个问题后继续全部剩余可执行轴，最后一次性返回完整 findings；不得只审旧 finding，也不得扩张成全仓历史审计。

---

## 参考索引

本 skill 的 `references/` 目录存放关键模板:

- `references/templates/project-context-template.md` — GPC 产出骨架
- `references/templates/prd-template.md` — PRD 产出骨架
- `references/templates/architecture-decision-template.md` — CA 产出骨架
- `references/templates/epics-template.md` — CE 产出骨架
- `references/workflow-catalog.csv` — BMAD 全部 31 个 workflow 官方描述 (CSV 原版)

扩展阅读（非必读）：
- 官方文档站：https://docs.bmad-method.org
- 每个 workflow 的详细 step-01/02/03... 在上游源仓库 `src/bmm-skills/<phase>/<skill>/steps/`

---

## FAQ · 常见问题

**Q: 为什么不直接 `npx bmad-method install`?**
A: 官方装法会展开大量 workflow 文件。蒸馏版优先保证一次读取即可完成路由和当前方法；是否切换官方包应由用户另行决定。

**Q: 本 skill 会不会过时?**
A: 会。升级前应比较实际能力、项目需要和上下文成本；不得仅因上游文件更多或版本更新就重新引入整包。

**Q: 为什么蒸馏? 直接读源仓库不就行了吗?**
A: 源仓库文件很多。蒸馏目标是让一次完整读取足以选择正确 workflow、产物与门禁，而不是保留每个上游步骤的文字副本。

---

## 给下次对话

新任务加载本 skill 时：
1. 先读项目权威来源并重建 accepted state。
2. 找出首个真实未闭合门禁，不重做已证明完成的 workflow。
3. 选择最低必要规划高度并定义当前产物、成功标准和验证方法。
4. 完成当前 workflow 后停止，按项目合同交回下一门禁；不要自动连跑下一角色。
