---
name: bmad-method
description: 用于正式编码前创建、更新、审查或修正规划：从 accepted state、Discovery、产品/PRD、UX、Architecture、Epic/Story/DAG、合同与证据推进到一个 exact READY Story。也用于 Planning Review、Correct Course 和识别规划遗漏；不用于代码实现或代码 Review。
---

# BMAD Method

## 批准范围内自行修错与人工决定
- 批准范围内的代码、脚本、参数、导入、接线和验证载体实现错误，由当前agent自行定位和修正，不请求重新批准，不转交用户修改。使实现恢复到合同规定的行为，不属于新增业务决定。
- 真实冲突：两条有效要求无法同时满足，例如一条要求“有引用的项目必须保留”，另一条要求“删除时连同引用一起删除”。先定点核对来源和优先级；确实无法依据已有决定解决，才交用户裁定。
- 业务未决选择：合同缺少一个会影响用户实际结果的规则，必须在不同业务行为之间选择，例如尚未规定“停用项目是否允许新增关联”。已有决定直接沿用；符合相同合同的技术写法由agent自行选择，语法、导入、参数和接线错误不属于业务未决选择。例子只解释标准，不新增当前任务的规则或测试。
- 越界、超出合同或缺少必要操作授权：下一动作实际超出已批准路径、业务规则、验证内容、运行次数、环境或角色权限，才说明准确增量并请用户决定。不能仅因发生错误、技术方法尚未查清或修复改变了错误结果，就声称缺少授权。
- 停止失败的执行、保留现场和暂不声称通过，不等于冻结批准范围内的定位与修正。仅暂停依赖未决决定或缺少授权的动作；其他已授权工作继续。恢复、再次执行等动作按原批准内容和次数判断，不自动增加次数、不重跑未受影响通过项。
- 将载体修正为正确执行原定输入、步骤、断言和编排行为，不算扩大验证；新增或改变这些已批准要求才属于范围变更。不得借修错放宽断言、改输入、增加场景或反复改变编排来绕过失败。本规则不授予Planner实施、Reviewer修改候选或其他角色之外的权限。
- 同一个问题累计经过三次不同的、确实需要询问用户并由用户决定的授权后，仍未解决或再次出现时，停止继续提出或实施第四项处理，先与用户复盘并讨论纠错方向。这三次可以是三次不同的越界请求、合同外追加内容或三种不同的处理方案，不限定为三轮代码修复。按实际记录说明每项授权要解决什么、决定及边界是什么、实际做了什么、为什么仍错、现有证据和未解决事项；不得未经核对就宣称全部实现无效，不自行升级架构或扩大验证。讨论确定纠错方向及必要授权后继续，未受影响的有效成果和其他已授权工作保留。
- 上述“三次”按针对同一问题的三项不同且必要的用户授权决定计算。同一授权的重复确认不重复计数；批准范围内的代码、脚本、参数、导入、接线或验证载体错误，本来就应由agent自行定位和修正，既不请求授权，也不计入这三次。不得把本应自行解决的错误包装成越界或业务选择来索取授权。用户授权要求完成准确批准的工作，不代表验收错误结果；不得反复索取新的授权代替复盘。
来源：2026-10-11本PubMan主管理对话用户明确决定及后续澄清。本定义及同一问题三项不同必要授权后的复盘门禁优先于旧的无条件暂停、遇到疑问即审批或按失败次数自动升级措辞。


## 共享模板入口

主管理续作使用 `C:/Users/25073/.codex/skills/MAIN_CONTROL_RESTART_PROMPT_TEMPLATE.md`；完成准确 READY Story 后的手工 Writer 交接使用 `C:/Users/25073/.codex/skills/DEV_STORY_PROMPT_TEMPLATE.md`。编写或修订提示词前从对应准确路径完整读取，不采用旧打包或其他项目副本。统一来源及同步记录见 `C:/Users/25073/.codex/AGENTS.md`；本入口不扩大 BMAD 的角色权限。

## Story实施、代码审查与修复交接边界

1. **首次Writer完整实现批准的Story**，把范围内的行为和接线做完整。
2. **首次Reviewer独立完整审查；后续Reviewer只检查当前批准的修复及其明确影响。** 原来通过的审查结论和测试结果按原范围保留，不自动失效、不重复审查或重跑。
3. **Repair Writer严格按批准内容修改。** 指定的问题、路径和验证范围就是边界；需要改别处或增加验证，先说明并取得批准，不能借“完整原因链”自行越界。

首次完整检查应覆盖批准范围内的实际行为和必要直接接线；后续Review只完成当前批准修复及明确影响的审查，继承原范围内已通过的结论和测试。完整提示词、完整报告和fresh Reviewer均不表示重审整个Story或重跑旧测试。

本节用于软件Story的Writer、代码Reviewer及Repair交接，不扩大BMAD权限，也不改变规划Review的授权范围。

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

## 官方能力与复用优先

先查官方与现有能力，优先复用：
- 从需求澄清、方案设计和技术选型阶段就先查证，再确定设计与实现方案；实现、修复及缺陷判断同样遵循此规则，不以已经出现缺陷为查询前提。先定点查阅与实际版本有关的官方文档/源码、GitHub仓库及Issues、其他可靠网络资料和适用的成熟实现，不限于官方网站，并核对项目现有代码。优先原始来源和维护者资料，区分官方保证、维护者结论与社区经验，不把社区猜测当系统保证。明确系统默认行为、配置/API已经提供的能力，以及当前需求真正缺少的部分；不先假设需要自写逻辑。
- 优先复用系统能力、框架API和项目现有实现；确需引入成熟开源实现时核对适用版本与许可，不搬入整套无关架构。已有能力能满足需求时，不再自建等价owner、包装层、调度或恢复机制。
- 先确认真实用户入口和已接受需求。未使用回调、纯函数、框架API或潜在技术场景的存在，不证明有用户可触发的缺陷，也不授权新增入口、按钮或功能来制造迁移/测试的必要性。
- 简短交代来源链接/版本、复用能力及必须自写的部分和理由。同版本已核实资料直接复用；足够回答当前问题即停止搜索，不新增研究轮次、不重读全部历史。资料不足或冲突时明确UNKNOWN/CONFLICT，不猜成事实，不把默认行为说成所有版本/设备上的保证。
- 网上方案先核对来源、适用版本及既有业务规则。常识明确、与既有业务规则一致且必要操作已获授权的做法直接落实，不因来源在网上而额外确认；仅存在经核实的真实冲突、合同未定的业务规则选择或缺少必要操作授权时，才向用户说明来源、用途、收益与代价及范围/验证影响并取得决定。网上建议本身不构成授权。
- 查阅、核对和比较资料可以按已有授权进行。满足上述讨论条件的方案，在取得决定前不得实施依赖它的修改、接入依赖或替换已批准设计；其余明确且已授权的工作直接继续。来源变化但方案及批准边界未变时复用原批准；网上建议和示例不是用户需求或实施授权。
- 网上建议和示例不是需求或权限。新增/升级依赖、安装工具、产品/架构/生命周期改变、额外路径及验证增量，仍受下列越界门禁约束；源码/API/hash只能证明其对应层，不能冒充实际运行。

越界立即停止与完整报告：
- 一旦判断下一步超出已获准确授权的需求、范围、包数、写路径、owner、生命周期、接口、数据责任、依赖或环境，或需要改变已批准的验证要求（新增方法/场景/输入/步骤/断言，或改变fixture/调度/callback/等待/超时/命令/回归的既定行为），必须在相关新增工作、修改或运行前停止，保留现场与有效证据。
- 技能、模板、Review、因果必要性、风险传播、“完整性”“更保险”“最佳实践”均不能代替用户批准。本门禁同样约束后文关于扩大诊断/验证或重新运行的表述；已获准确批准的范围无需重复审批。
- 在当前对话直接交用户讨论的范围变更报告一次写清六点；批准记录随最终报告向主管理汇总，不要求用户往返传递：
  1. 具体问题、文件/方法和实际行为，区分PROVEN、INFERENCE、UNKNOWN。
  2. 原定工作已完成和已证明的内容。
  3. 具体缺少哪项行为证明或合同依据。
  4. 拟增加的准确路径、输入、步骤、断言、设施或命令及各自用途。
  5. 原范围为什么不能解决，不增加会留下什么实际缺口。
  6. 当前改动、产物、证据、未完成事项及获批后的唯一下一动作。
- 标记角色对应的BLOCKED/REVIEW_BLOCKED，不声称完成或全部通过；未获用户明确批准不得执行增量，不能通过其他角色绕过。获批后只执行准确批准范围。Reviewer可完成其余已授权审查，但不得把待批准增量写成Repair必须执行的要求。
- 不循环改变fixture/调度/等待/超时的已批准行为来绕过失败，不重复命令碰运气，不新增生产test seam，不削弱断言、改变输入或事务边界绕过失败。产品、设施和环境失败必须分清。
- 已通过且未受影响的结果继续有效；只复验准确修复对应的已批准集合，不因局部修改重跑整批，不在交付前追加测试、build、设备、性能或人工门禁。

首份提示词与续作补充：
- 首份正式提示词必须完整保留本模板正文及所有适用约束，填写任务字段后逐项核对；不得自行概括、去重、缩写或以“已有记忆/技能/原对话会继承”为由省略规则，尤其不得省略越界停止、六点报告、验证范围和保护状态。
- 可引用可读的身份绑定任务产物承载任务数据；这不授权删减角色和治理规则。续作回补可以简短，只说明准确增量，不能撤销或弱化原合同。用户要求完整版本时直接提供整合后的完整文本。

## Story 阶段人工讨论门禁

1. 常识明确、且与既有业务规则一致的问题，在已有角色权限和操作授权内直接落实，不再停下来确认。只有经核实的真实冲突、合同未定的业务规则选择，或缺少必要操作授权时，才交用户决定。不得把猜测当常识；本规则不新增写路径、测试/重跑次数、环境操作或角色权限。本门禁适用于 Story 的规划、实施、Repair 和 Review。
2. 先做判定是否满足上述条件所需的最小事实定位。仅对需要用户决定的问题，指出具体入口、源码或合同依据及已知/未知内容；足以说明问题即停止依赖它的工作，不先展开完整方案、测试设施或大合同。明确可直接落实的问题继续已授权工作。
3. 用用户能理解的话说明：实际会发生什么、为什么影响当前 Story、需要用户决定什么。已有依据足够时可给简短建议；不得为了凑齐选项继续调查。
4. 进度消息、“已列为待裁定”“稍后写进提案”均不构成人工讨论。未取得用户决定，不得把问题挂起后继续推动依赖它的设计、实施或验证。
5. 用户决定后，先准确记录决定及其边界，再继续已批准工作。后续问题只有满足第1条的讨论条件时才再次进入本门禁。已明确且未变化的决定不重复询问，未受影响的有效成果保留。
6. 需要扩大范围时，沿用现有六点报告并直接在当前对话交用户讨论；普通澄清直接提出具体问题，不为每个疑问新增报告文件、规划包或审批轮次。
7. 主 agent、子 agent 及手工独立角色遇到疑问，均可直接与用户讨论，不要求用户把问题复制回主管理再传回答案。用户在当前对话作出的准确批准有效；记录批准及边界后按角色权限继续，无需主管理重复批准。若运行环境只能由主 agent 展示子 agent 的问题，主 agent 只负责转达问题和答案，不增加决策层或让用户手工搬运。
8. MANUAL_RELAY 仍约束角色启动和最终交付，不强制疑问往返接力，也不授权自动创建子 agent 或任务。批准记录随原任务已有产物或最终报告汇总；无产物写权限时先保留完整对话批准记录，由主管理在既有台账同步。直接讨论不授权 Planner 实施、Reviewer 修改候选或执行未获批准的动作。

以下普通细节自主处理、整轮 Review 条款与上述条件共同适用：明确且已授权的问题直接处理，必要的用户决定不能因整轮汇总而推迟。

## 用户流程与最小必要检查

以下规则约束本文件中的完整读取、身份核验、独立核对和交付检查，不新增审批层、报告文件、测试或检查轮次。
1. 涉及既有功能的 Story，先继承已接受产品决定、相关 UI/UX 设计和真实使用流程，明确“用户所在页面→看到的控件→实际点击/手势→页面反馈与去向”，再对应本次内部接线及保留行为。交接必须给出准确来源和必要章节；单条测试流程、源码调用链或接口状态不能代替整个 Story 的用户流程。只读相关片段，不为继承依据通读全部产品文档；已有决定直接沿用，真正冲突才讨论。
2. 疑问先说明具体用户操作链、当前影响及依据。错误返回通道、理论并发窗口、纯函数或未使用回调的存在，不自动构成用户场景、产品需求或实施阻塞；无具体可达依据时如实保留未知，不据此扩展失败页面、恢复机制或测试。实际正常操作出现接线缺陷应修正接线，不以失败提示替代修复；不吞错、不把未证实说成不可能。
3. 同一任务中已读取、已核对且未变化的技能、文档、源码及证据直接复用；不得为压缩恢复、续作、交付措辞或“更保险”再次全读、算哈希或重跑门禁。提示词模板适用下一条的每次必读例外。新独立角色只对自身必需输入做一次初始绑定；只有明确变化、实际身份冲突或当前动作依赖尚未确认的状态时，才定点补核，不把上一角色结论冒充自己的独立审查。
4. 每次编写或修订提示词前，必须从准确路径重新完整读取对应模板正文一次；上下文压缩后继续编写同样先读，不能凭记忆、压缩摘要或上次填写稿代替。此要求仅授权读取模板，不自动触发模板完整性审计、哈希核验、Git检查或其他来源重读。未修改原模板时，不重新审计其正文完整性。新生成提示词只对本次填写/修改内容及其必须保留的规则做一次必要检查；不能把原样保留模板变成全文反复比对任务。交接检查首先确认用户流程、已有决定、此次改动及直接消费者的对应关系；占位符清零、哈希一致和格式完整不能代替这项判断。
5. Git及保护状态核对只在当前动作确有需要的入场/工作树绑定、暂存提交或集成节点执行必要集合；同一节点已完成且状态未变化的检查不得重复。规划续作、普通答疑、文案整理不得自动触发整套Git、哈希或保护资产审计；不取消已批准且尚未完成的必要门禁。
6. 搜索先限定直接相关文件、章节和符号；长文档先定位再取片段，不用宽泛关键词跨大量历史全文输出。已有证据足够回答或完成当前修改立即停止；输出过宽时收窄，不追加更大调查。不得为证明自己检查过而创建额外清单、脚本、报告或测试。
7. 发生交接失误时，依据实际记录说明遗漏和影响；不能未经检查就宣称整段实现都在猜、全部无效或必须重开。保留已完成工作，只修正有依据的缺口。用户要求减少消耗时立即收窄或停止无必要操作，不能拿治理要求解释反复检查。

## 不变量

1. 不把猜测当事实，不隐藏困惑。用户已经决定的行为，以及类型、数据库约束和当前框架明确保证的事情，直接沿用。没有依据的判断必须说明未知，不能据此新增需求或阻塞任务。发现问题先在批准范围内做最小定位并自行修正；仅符合本文件人工决定条件时才与用户讨论。 Story阶段仍遵守人工讨论门禁；其他规划阶段只问影响产品、UX、Architecture、scope、owner、evidence或完成判据的承重unknown。
2. 每轮最多三个承重问题；每题给事实、互斥选项、trade-off、推荐、直接 ripple 和最终 decision owner。推荐不是决定。
3. `Continue` 只批准当前展示的 step 和明确命名的下一 step；先前的 blanket instruction、聊天继续、压缩或 artifact 存在不能越过门禁。
4. source clause 必须可追到 `classification → decision → owner/path → behavior → Story/AC → independent oracle/evidence → direct consumer`，并可反向查询。
5. framework/version/API feasibility 在 READY 前由真实 docs/source/PoC 证明。不能表达的合同不留给 Writer 猜，也不偷渡 callback、wrapper 或第二 authority。
6. 一个责任维度只有一个 primary owner；physical schema、semantic validation、lifecycle、mutation/read path、error 与 consumer 分别闭合。
7. Story capacity 是结构判据，不以 token、文件、代码行或“看起来简单”证明。完成当前要求所必需的直接调用方、数据处理和页面接线，应一起处理，不能为了少改文件留下半套功能。验证只能在批准范围内进行；失败后先说明具体原因，不重复试跑，不靠改输入、放宽断言或延长等待取得通过。
8. 检查放在负责接收数据或执行规则的位置。同一条件已经检查通过，而且中途没有改变，就不在每层重复检查。持久化数据、外部输入和实际业务状态需要哪些检查，依据当前合同确定，不能凭空添加。 直接依赖已明确的类型、数据库约束、合同和当前框架保证；未知不等于不可能，错误通道本身不构成新需求。
9. 不得把失败伪装成成功、空结果或默认值。已有错误处理约定直接复用，保留原始原因。确有资源需要释放时完成清理；是否提示用户、允许离场或重试，沿用已批准行为，不凭错误通道的存在新增一套页面流程。 可以为当前明确职责提取函数或模块，即使只调用一次。不得仅为将来可能复用，新增接口、包装层、工厂、管理器或配置机制。新增结构必须说清它解决了当前哪一个问题。 这些判断适用于存量修改与从零构建，不单凭调用次数或catch范围判缺陷。
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

可以按已授权范围核对 source、Git、code 或 framework 中的事实；常识明确且与既有业务规则一致的问题，在已有规划权限内直接落实。仅经核实的真实冲突、合同未定的业务规则选择或缺少必要操作授权时，完成最小事实定位后交用户决定。已明确批准且未变化的细节不重复审批；Independent Review 不共同设计 candidate，已批准的 bounded planning Repair 不因普通问题增加批准循环。

## F1–F10 与直接 references

先用 F1 找到最低未完成规划高度，只加载当前 intent 的 direct reference。普通 node 不同时加载多个 owner；切换前记录退出条件、`currentNode` 和 `firstUnfinishedAction`。Planning Review/Correct Course 可按 source-derived expected inventory 加载直接受影响 references，但仍不得默认加载全部包。

| Function | Intent | Direct reference |
|---|---|---|
| `F1` | accepted-state reconstruction、routing、project-context discovery/audit、deterministic planning status、approval/compaction recovery | [state-routing-and-interaction.md](references/state-routing-and-interaction.md) |
| `F2` | Discovery、brainstorm/forge、advanced elicitation、typed research（含 academic literature） | [discovery-research-and-elicitation.md](references/discovery-research-and-elicitation.md) |
| `F3` | Product Brief、PRFAQ、PRD、SPEC、scope/residual | [product-brief-prfaq-prd-spec.md](references/product-brief-prfaq-prd-spec.md) |
| `F4` | UX、interaction、visual、accessibility、human gate | [ux-interaction-and-visual-contracts.md](references/ux-interaction-and-visual-contracts.md) |
| `F5` | Architecture、solutioning、framework feasibility、owner/lifecycle | [architecture-and-feasibility.md](references/architecture-and-feasibility.md) |
| `F6` | Epic/Story、dependency DAG、capacity | [epic-story-dag-and-capacity.md](references/epic-story-dag-and-capacity.md) |
| `F7` | obligations、traceability、contract closure、evidence 与 retrospective evidence inventory | [obligation-traceability-and-evidence.md](references/obligation-traceability-and-evidence.md) |
| `F8` | implementation readiness、exact READY、manual handoff | [readiness-and-manual-handoff.md](references/readiness-and-manual-handoff.md) |
| `F9–F10` | independent Planning Review、Consistency Audit、planning-only retrospective、Correct Course、planning Repair/escape | [planning-review-and-correct-course.md](references/planning-review-and-correct-course.md) |

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
