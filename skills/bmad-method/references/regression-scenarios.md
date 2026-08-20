# BMAD Capability Regression Scenarios

## When to load

只在修改/审查本技能、验证规划方法没有退化，或某个真实 planning failure 需要按行为 oracle 复现时加载。普通 Product/UX/Architecture 任务不要加载此文件来提示预期答案。

## Goal

用真实输入→动作→停止条件→artifact state 判定技能是否保留解决任务的能力。关键词、标题、regex、frontmatter、hash、path count 或 `quick_validate` 只能证明结构，不能替代 walkthrough。

## Inputs

- 当前 candidate `SKILL.md` 与适用 direct reference。
- 下列每个 scenario 的最小原始输入；不要把 expected behavior 预先告诉被测 fresh Planner。
- 若为维护验证：exact base/candidate identity 与本次能力 delta。

## Collaboration mechanics

逐项读取规则后记录：input、first action、must-not action、blocker/completion、expected artifact state、failure signal、适用 reference 与 exact rule。Planner 的回答必须能从 candidate 合同推导，而不是仅复述本 oracle。

## R-01 — Future direction 不进入当前 Epic

- **Input:** 当前训练 Epic 的材料写“未来可以加入用户导出”。
- **First action:** 在 capability scope ledger 中把导出标 `FUTURE_CANDIDATE`，绑定来源、用户价值、owner/revisit condition，并询问它与当前 Epic 的边界。
- **Must not:** 直接生成当前 Epic 的 export Story，或仅因“未来”就创建下一 Epic。
- **Blocker/completion:** 若分类会改变 current scope，在用户决定前阻塞 CE；用户确认后 current done state 与 residual map 同步。
- **Expected artifact state:** 当前 Epic coverage 不含 export；residual map 含该能力和 next Epic=`UNKNOWN / NOT_DISCOVERED` 或用户接受的 identity。
- **Failure signal:** export 自动出现在 current Story/FR coverage，或 future item 消失。
- **Applicable:** `product-and-scope-planning.md`、`epic-story-and-readiness.md`。

## R-02 — `E17-18` 不等于 Epic `E18`

- **Input:** Current Epic E17 的最后一个 Story 名为 `E17-18`，sources 还提到后续价值。
- **First action:** 分开解析 Story identity 与 Epic identity，建立 post-E17 residual capability map。
- **Must not:** 从字符串 suffix 推断 Epic E18 已规划、为空或已完成。
- **Blocker/completion:** E17 可在自己的 done state 关闭；E18 未定义不一定阻塞 E17，但必须保持 `UNKNOWN / NOT_DISCOVERED`、owner/open question，不能宣称 roadmap complete。
- **Expected artifact state:** E17 Story list 保留 `E17-18`；residual map 独立记录 E18 候选/未知。
- **Failure signal:** `E17-18` 被当作 E18，或 remaining capabilities=0 无来源证明。
- **Applicable:** `product-and-scope-planning.md`、`epic-story-and-readiness.md`。

## R-03 — “显示心率曲线”必须关闭图表合同

- **Input:** PRD/UX 只有一句“训练复盘显示心率曲线”。
- **First action:** 建 chart closure table，逐项检查 axes、series、units、domain/ticks/legend、raw/aggregation、sampling/downsampling、smoothing、gap/excluded/unknown、long-session、states、interaction、small-screen、TalkBack/非颜色编码。
- **Must not:** 自行选 bpm 线/平滑算法，写 `known omissions=0`，或直接生成 UI Story。
- **Blocker/completion:** 任何适用项 `OPEN` 都阻塞 CE；用户/accepted authority 决定后才能标 `DECIDED`。
- **Expected artifact state:** 每项为 `DECIDED/OPEN/N/A`，有 source/owner/downstream；openQuestions/phaseBlockers 非空直到关闭。
- **Failure signal:** 只有“画曲线”Story/AC，或仅提 accessibility 而缺数据语义。
- **Applicable:** `ux-and-visual-contracts.md`。

## R-04 — Recap/export/future analytics 共同澄清

- **Input:** 用户同时描述训练 recap、导出和 future analytics，但没有给阶段边界。
- **First action:** 让用户从真实目标与 journey 叙述三者关系，再分别建立 capability/source/classification 候选。
- **Must not:** 先给一个 LLM 自制 MVP/Phase 2/Phase 3 菜单并当成 accepted scope。
- **Blocker/completion:** 会改变 current scope 的分类是 phase blocker；用户接受后才更新 done state/residual map。
- **Expected artifact state:** 三项分别有分类、理由、owner 与 residual effect；推荐仍是 candidate。
- **Failure signal:** Planner 静默分期，或把 analytics 自动并入 export/current。
- **Applicable:** `discovery-and-elicitation.md`、`product-and-scope-planning.md`。

## R-05 — Sorting Repair 做 bounded adjacent scan

- **Input:** Consistency Audit 的 complete batch 指出 coverage interval sorting 缺失。
- **First action:** 锁定 candidate/batch/direct sources；从 sorting claim 扫同 normalization partition、same owner/state/boundary、read model、summary/export consumers 与 AC/evidence。
- **Must not:** 只补一句 sort，或重开 Discovery/CE/全产品；不得重新设计不受影响的 SQLite。
- **Blocker/completion:** 同一根因的相邻遗漏加入同一 atomic Repair；若需新 owner/schema/Epic boundary，停止并结构升级。
- **Expected artifact state:** Repair 记录 preserved contracts、adjacent scan、完整 delta；下一步是 fresh full Planning Review，不是直接 re-Audit。
- **Failure signal:** 只修 reviewer 原句、漏直接 consumer，或无限扩域。
- **Applicable:** `validation-correct-course-and-repair.md`。

## R-06 — 内部自洽仍发现外部 surface 缺口

- **Input:** Candidate 的 DAG/hash/AC/paths 全部自洽；accepted journey 要求复盘图表，但 candidate 无 axis/series contract。
- **First action:** Review 从 accepted sources、journey 与 surface 独立重建 expected inventory，再与 candidate 对照。
- **Must not:** 以 candidate coverage、DAG 或 hash PASS 结束，也不能发现一项后停止其余轴。
- **Blocker/completion:** 缺失 chart contract 是阻止 readiness PASS 的 must-fix finding；Review 完成所有适用轴后一次返回 atomic batch。
- **Expected artifact state:** Finding 引用 source/journey、缺失 UX contract、受影响 Story/consumer/evidence 和最小修复高度。
- **Failure signal:** Review PASS，或只报告机械一致性。
- **Applicable:** `epic-story-and-readiness.md`、`validation-correct-course-and-repair.md`。

## R-07 — Fast path 不冒充确认

- **Input:** 用户要求“快速给我完整 PRD 草案”。
- **First action:** 进入 Fast path，批量询问最少缺口；对每项推断标 `[ASSUMPTION]`，保留 openQuestions/phaseBlockers。
- **Must not:** 把默认/推荐写入 acceptedDecisions，因文档完整而清零未知，或越过 blocker 进入 CE。
- **Blocker/completion:** 非承重 assumption 可随草案继续；load-bearing scope/UX/owner blocker 必须暂停。
- **Expected artifact state:** 草案完整但 status 仍 candidate，assumptions/openQuestions/phaseBlockers 真实可恢复。
- **Failure signal:** `pendingDecisions=[]` 被当作完整，或无 `[ASSUMPTION]` 的推断。
- **Applicable:** `SKILL.md`、`product-and-scope-planning.md`。

## R-08 — UX delta 继承已验证技术合同

- **Input:** 任务只补心率复盘图表 UX；accepted baseline 已固定 session/Room/coverage 技术合同与 identity-bound evidence。
- **First action:** 把技术 baseline 列为 inherited constraint，只检查 UX choices 与它是否冲突。
- **Must not:** 重问 pre-enabled/lineage/Room owner，重跑 SQLite literal mechanics，或把 UX 选择改成新的 data owner。
- **Blocker/completion:** UX 可在不冲突时完成；若选择需要新 schema/owner/data responsibility，结构升级到 Architecture/Product。
- **Expected artifact state:** UX chart decisions + inherited source identity；unchanged technical evidence 标 preserved/not rerun。
- **Failure signal:** 重复旧验证、重新选择 accepted 技术决定，或 UX 静默改数据真源。
- **Applicable:** `ux-and-visual-contracts.md`、`architecture-and-solutioning.md`、`validation-correct-course-and-repair.md`。

## R-09 — Compaction 从 first unfinished action 恢复

- **Input:** 自动上下文压缩发生在 Discovery、Repair 或 Review 中。
- **First action:** 用摘要、current artifact 与 ledger 确认角色、currentNode、firstUnfinishedAction、已完成 decisions/evidence。
- **Must not:** 冷启动、重放 brain dump/用户批准、重跑 baseline、重启 Repair 或把压缩当 `Continue`。
- **Blocker/completion:** 关键事实可证明则继续第一未完成动作；只有该事实无法恢复时局部重读 authority 并保持 open/blocked。
- **Expected artifact state:** 原 identity/decisions 不变，firstUnfinishedAction 单调前进或停在缺失事实。
- **Failure signal:** 已完成步骤/决定被重复，或 Review/Repair 从 Step 1 开始。
- **Applicable:** `SKILL.md` 与当前 direct reference。

## R-10 — Local Repair 结构升级

- **Input:** 一个 bounded finding 的修复需要新增 core owner、改变 schema/data responsibility 或移动 Epic boundary。
- **First action:** 停止 local patch，记录被破坏的 inherited contract、所需 altitude 与未执行动作，交回主管理选择 scoped Correct Course。
- **Must not:** 用更长 AC、wrapper 或局部默认掩盖结构变化；不得自行扩 scope。
- **Blocker/completion:** 新 load-bearing decision/structural authority 是 blocker；经授权后在 Product/Architecture/Epic 高度处理，原 Repair 保留恢复点。
- **Expected artifact state:** Candidate 未发生越权结构修改；escalation artifact 精确指向 owner/schema/Epic ripple。
- **Failure signal:** Repair 直接添加新 owner/schema 或重新分 Epic 后仍称 bounded。
- **Applicable:** `architecture-and-solutioning.md`、`validation-correct-course-and-repair.md`。

## R-11 — Coaching 的 brain dump 与薄弱答案追问

- **Input:** 用户说“我想做一个训练记录产品，帮我规划”，上下文很少。
- **First action:** 邀请完整 brain dump 与已有 sources；反射后只问一次“还有什么”；再校准 stakes/form-factor/mode。
- **Must not:** 第一轮发问题墙、给 feature 菜单、直接写 PRD/Epic，或用 Planner 想法填满薄弱回答。
- **Blocker/completion:** 一次一个开放问题；薄弱/矛盾/load-bearing 回答继续追问，直到 Discovery completion 或明确 phase blocker。
- **Expected artifact state:** 用户意图、sources、concerns、assumptions/rejections 被记录；用户仍拥有方向。
- **Failure signal:** 预制选项替代叙述，或没有 anything-else/追问便收敛。
- **Applicable:** `discovery-and-elicitation.md`。

## R-12 — 机械 gate 不覆盖产品/UX blocker

- **Input:** DAG、path count、hash、Git gate 与 quick validation 全通过，但一个 CURRENT chart surface 仍缺图表语义。
- **First action:** 对照 phaseBlockers 与 external expected inventory，识别未关闭 UX contract。
- **Must not:** 以机械 gate、文档长度、`pendingDecisions=[]` 或旧用户 `Continue` 宣布 READY。
- **Blocker/completion:** chart phase blocker 关闭前 CE/readiness 不 PASS；机械验证仍保留为结构证据但权重不升级。
- **Expected artifact state:** readiness=not ready/changes required，finding 指向具体 UX closure 与 downstream Story/evidence。
- **Failure signal:** 因所有机械检查绿而 PASS。
- **Applicable:** `SKILL.md`、`ux-and-visual-contracts.md`、`epic-story-and-readiness.md`。

## State and output

维护一份 walkthrough matrix，逐场景记录 candidate exact section、推导出的 first action/must-not/blocker/artifact state、实际 verdict 与 evidence。若某场景只有关键词命中，状态仍为 `NOT_WALKED`。

## Blockers

- Candidate/reference identity 不明确；
- 无法读取适用完整规则；
- scenario 被泄漏给声称独立的 forward test；
- 只完成格式/regex 检查而没有语义推导。

## Completion / readiness checks

- R-01–R-12 全部从 candidate 规则逐项走查；
- 每项 first action、must-not、blocker/completion 与 expected artifact state 都唯一可判定；
- failure signal 能区分“出现关键词”和“真正解决任务”；
- 无 scenario 依赖本机绝对路径、旧 template/catalog 或未授权角色；
- fresh manual forward test 若需要，由项目主管理在合并后另行组织，Writer 自评不冒充独立测试。
