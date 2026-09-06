# E18–E22 正式 Epic 与 Story 计划

<a id="current-status"></a>

## 当前状态、来源与阅读规则

**规划状态：已接受。文档落地状态按下述条件判定；本文件不授予代码 Writer 权限。**

E17 以胶囊封口，已合并 CS-03、CS-04A、CS-05、CS-04B 与胶囊资产保留。E18 → E19 → E20 → E21 → E22 的五组成果、24 个 Story、42 条唯一依赖已通过第四轮独立规划审查且获用户接受；力量与跟练分别接入、分别验收。技术根与数据依赖不改变成果验收顺序。

本计划文档 candidate 尚未通过独立 Review 并成为同步 main 的祖先时，tracked landing 为 pending；满足这些条件后 landed，E18-S01 进入 F8 完成步骤；这不自动解锁代码 Writer 或把未来 Story 标 done。文档候选在 Review / 集成前为 `DOCS_IMPLEMENTED_CANDIDATE_AWAITING_REVIEW`。E18-S01 只进入准备，其 F8 执行绑定与代码实施尚未完成；未来运行、设备、依赖解析、性能和主观体验门禁不因规划接受而通过。

**审查与接受身份：** V2 F.40 记录 F9 attempt=4、independence=VALID、overall / SPEC / QUALITY / EVIDENCE=PASS，审查对象为写前 V2（442883 bytes，SHA256=`CA8C9EE5894F6361D8E0496E354B011E237B0D53BFACE1257F7D6E5AE7684DCD`），报告 SHA256=`3CE2750B8FD418B70B1EF8A42D3BC0B7175002A52BE3930365E0DCF875416D30`。用户随后接受已展示规划并授权正式落地与 S01 实施准备；该 PASS 不覆盖后续 V2 状态更新或本次七文档转录 candidate。P003 已关闭，P001/P002 无回退；N001 已仅将 kind 索引计数更正为九，literal 值不变，不重开这些 finding。

**当前导航：**

- [F.37 当前 Story selector](#f37-当前候选包与手工规划审查入口) 是 24 个当前 Story 的唯一入口；F.26 是当前 42 边 DAG，F.36 是逐项 capacity。各 Story 同时消费 F.37 指明的 typed / AC / oracle / consumer 补充合同。
- B–D 与 F.1 保留需求、资产、成果与非目标；F.20、F.22–F.35 保留 UX、字段、union、NULL、ordering、atomicity、retry、接线及证据义务。不得用本页导航替代完整正文。
- [保全正文](#accepted-contract)中的旧 DRAFT、NOT_READY、候选、待审、下一步、角色、派发和执行权限文字均保留其原始语境，不是当前操作指令；F.38/F.39 是修正历史，F.2/F.3 的旧拆分及旧 CS-06 不复活。F.37 中关于原 artifact、A、F.40–F.42、G/H 或后部历史快照的引用，定位原 V2 的来源/管理记录；当前状态与正式入口由本节承接，不要求重启旧 Review 或寻找另一任务。
- [来源到正文映射](#source-map)用于核对承接。App 比较/长期分析/进阶、App AI、低空间功能、HIIT 次数及新库均不因落地新增；真实写入失败、原始错误、事务回滚、诚实保存和释放状态保持。

### Immutable main 与路径解释

所有 main 直接来源均固定为 Git commit `d2c9ac48027177389092d56c208c64447a3c6a93`，tree=`c621b8385726a2b1b65097f337ab08134e4aed56`。以该 commit 的 repo 内路径读取正文，不以分支名或漂移工作区替代 Git 对象。主要技术来源是 [E17 remainder 合同](e17-remainder-epic-story-plan.md)的 §3–§9，按 B–D / F.37 的窄继承与替代规则消费；同文件在本次只新增入口，旧技术正文保持。

保全区中的 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/` 及其反斜杠等价前缀仅是原始 provenance / base locator。去掉此前缀后的 `docs/...`、`DESIGN.md`、`app/...` 等是 repo 内路径：已有输入解析为上述 commit 的 Git 对象；F.28 等明确拟新增的路径只表示未来 Story 的输出定位，不能冒充已存在输入。该解释不授权在 Integration 工作区写入，也不允许推导或替换未提供的本地文件。

其他直接 main 来源：[project-status](../project-status.md)、[decision-log](decision-log.md)、[roadmap-backlog](../roadmap-backlog.md)、[readiness-report](../readiness-report.md)、[architecture](../architecture.md)、[自动恢复与个人参数 Correct-course](e17-auto-reconnect-and-personal-parameters-correct-course.md)、[心率最小架构](e17-3-heart-rate-minimum-architecture.md)、[心率 Correct-course](e17-heart-rate-correct-course.md)、[心率 implementation readiness](e17-4-heart-rate-implementation-readiness.md)、[DESIGN](../../DESIGN.md)、[UI extension guide](../ui-extension-guide.md)。本行链接供仓库内导航，源身份仍固定为上述 commit；旧编号、旧阶段状态和旧候选不因被引用而成为当前任务。

### 本地来源身份

下列机器路径仅记录原始输入 provenance；有效规划义务由本文件正文或上述 pinned-main 合同承接，不依赖读者持有这些本地文件。报告仅保存身份和证据限界，不复制附件全文。

| 来源 | 原始 literal 路径 | SHA256 / 身份 |
|---|---|---|
| 本次 immutable V2 | `C:/Users/25073/Desktop/jianshen/.local/planning/E18-ONWARD-REQUIREMENTS-AND-ASSET-INVENTORY-V2.md` | 452781 bytes；`DAC9FBE8CCED971D3D503B81D6D1091AB6E218D29B99026F6AE18F98659E2E6D` |
| 分层讨论保全 | `C:/Users/25073/Desktop/jianshen/.local/planning/E17-CLEAN-SHEET-DISCUSSION-SALVAGE.md` | `66086CBEF35E6FD6BCFED8D9DA3439E59B0CB10B622040ACD7706FBD4296DD80` |
| D5 已接受行为保证 | `C:/Users/25073/Desktop/jianshen/.local/planning/E17-CS-06-CORRECT-COURSE-D-REPAIR-5-CANDIDATE.md` | `D2908408FC620BBC69CF3B4884286F0FB2B4AB07361E626A73BC34AF6CBDA46A` |
| F9 attempt=4 完整报告 | `C:/Users/25073/.codex/attachments/98ecb8b1-1c5e-4ad0-9fba-91dc064df695/pasted-text.txt` | `3CE2750B8FD418B70B1EF8A42D3BC0B7175002A52BE3930365E0DCF875416D30`；仅证明上述旧 V2 的规划审查 |

**转录边界：** 以下从 B 标题至 F.39 末尾保全 V2 正文；不复制 A/G/H、F.40–F.42 完整管理记录及原历史后缀。唯一已获用户明确批准的格式例外：V2 第 952 行（F.15.1 两段之间）四个 ASCII 空格在本文件中删除，空行保留，V2 原件不改；其他保全正文不变。此例外不改变规划语义，也不构成其他格式或 scope 扩张授权。

<a id="accepted-contract"></a>

## B. 用户已确认的需求变化与保留项

以下稳定 ID 是本次接受内容的索引，不是新增 Story。authority 为本主管理对话中的明确用户决定；历史条目只在标明的冲突范围内被窄替代，其他语义继续保留。

| ID | 已接受决定与影响 | 旧索引 / 下游 |
|---|---|---|
| E18-R01 | E17 实现胶囊即封口；未交付能力从 E18 起重排，不限定结束编号，不为凑数增加能力。开源只减少绕路，不预选库、不默认大规模扩充 OpenTracks。 | AS-01–07、全部后续规划 |
| E18-R02 | 不新增 App 内跨场比较、长期评价或进阶建议；比较、瓶颈判断与改进分析由用户导出后交给外部 AI。已有非 HR 趋势资产保留。此前力量连续 3 次可比成功训练后的窄进阶提示也暂缓。 | 窄替代 AD-P-011–016、RES-01–04；旧四窗口不再是当前实现要求 |
| E18-R03 | 不新增计时/HIIT 动作次数录入、识别或推断；保留力量已有实际重量、次数和 effort（含 form_breakdown）。心率曲线是事实，不据较低/较慢上升自动断言进步。 | 三模式记录、图表、导出；旧模型存在不等于所有反馈 UI 已接通 |
| E18-R04 | 日历显示每天记录的运动类型与数量；同日多模式分开表达。无记录的日期只表示没有训练记录，不推断休息或漏练。 | CAP-22、原 RES-12 从 DEFERRED 进入本次范围 |
| E18-R05 | 日期可在日历选择，也可通过弹出输入起止年月日；支持跨月跨年。可选单/多/全部运动，以及单/多计划；预览具体场次后取消勾选不想导出的记录。 | 日历、选择、导出 |
| E18-R06 | 单场、多场、多模式一起导出；各场身份独立。误取消或放弃的训练仍是可选记录；取消勾选只影响本次导出，不删除计划或本地训练。 | CAP-22/23、AD-P-019 |
| E18-R07 | 保存实际开始/结束的已知世界时间，并保留时区/UTC 偏移与相对时间的关系。新场次在真正开始时固定本地日期、时区及偏移；时区后改不移动历史归属。跨午夜归开始日，日期首尾均包含，导出整场而不在午夜截断。 | E17-ARCH-05；数据库、历史、日历、导出 |
| E18-R08 | 原始日期/时区无法证明的旧记录保留未知，不套用当前时区。单独列出“日期信息不完整的旧记录”，仍可逐条选择导出；不混入准确日期过滤。 | 旧记录兼容、日期筛选、导出 |
| E18-R09 | 自描述 JSON 保留版本、生成时间、选择范围及实际包含场次、immutable plan snapshot、实际执行、阶段/动作/轮组、原始点、暂停/额外休息、力量实际值、冻结参数、原始分析绑定、算法和统计规则。字典解释每项名称、类型、单位、来源、用途、关联、空值及枚举。 | 第 7 节旧字段合同 + 本文 D 的 v2 差异 |
| E18-R10 | 缺失、显式 0、未记录、零样本、无 eligible、无 zones、设备缺口、用户排除和进程未知不能混淆；保留毫秒和同毫秒排序。计划值、用户输入、设备测量和派生数据区分。字典可共用，不能靠聊天或外部文档才能解释文件。 | CAP-22；CT-06、旧 §7.5；AI 独立解释 oracle |
| E18-R11 | 保存与系统分享均保留。用户明确授权取消严格 600000ms 新打开期限，采用 Android 临时只读 URI 授权；不承诺第 10 分钟失效。系统交付只称“已交给系统分享”，不冒称接收方成功。 | 窄替代 CC-D03-B / CT 分享期限与 boot lease 机制；原数据内容、完整准备及外部副本边界保留 |
| E18-R12 | 离开勾选页面去 App 内另一页面后，原勾选清空；再次进入重新加载当前记录并重新勾选。不能以“返回仍保留旧勾选但记录已删除”的排除场景新增产品流程。 | F4 选择生命周期；撤回此前以该例要求选择整批失败策略的问题 |
| E18-R13 | 生成中主动返回或跳转 App 内其他页面：取消生成、清空勾选。旋转屏幕：保留状态与进度。切其他 App/锁屏：进程仍在时尽力继续，无可靠后台完成承诺。进程终止：下次重选，不自动恢复/重发。打开系统保存/分享面是正常交付，不当作取消。 | 导出 ViewModel、文件交付；不新增导出专用常驻服务 |
| E18-R14 | 固定选择后逐场完整读取并写同一个临时 JSON，显示已处理场次并允许取消；全部完成校验后才保存/分享。不删点、不降精度、不用摘要替代原始事实。 | 批量导出；每场一致读取、整文件完整交付 |
| E18-R15 | 未交付且失败/取消的临时文件清理；系统保存成功后清理 App 副本。已交给系统分享的副本保留，超过 24 小时在下次启动/生成导出前清理；进程中断的未完成残留下次清理。专用 App-private 目录排除备份；不新增后台定时清理，不承诺到点删除，不影响外部已保存副本。 | CT 分享/临时文件生命周期的本轮 accepted 替代规则 |
| E18-R16 | 在现有记录库增量补必要时间信息，不建第二套记录库。单场/批量统一新版导出；数据库、导出和分析版本分别管理。图表与导出共用严格历史读取，各自呈现，不重算默认统计、不以当前计划补写历史。 | 本轮三项 Architecture choices，用户明确接受 |
| E18-R17 | 新版字段合同及单场/多场性能基准已接受；数据结构与判定详见 D。数字是验收目标，不是已测结果或用户硬上限。 | Export v2、数据字典、多场 operational envelope |
| E18-R18 | 不增加App低空间预检查、阈值、提醒或对应开始限制，低空间提醒交给Android系统；不重复系统功能。实际数据库/文件保存失败仍沿已接受错误语义如实处理。 | 用户2026-09-06明确决定；窄替代B.1旧预检查方向及F.10全部未接受提醒建议；S07/E19/导出均不承接此新增功能 |

### B.1 继续保留的需求，不因本次摘要而删除

- 胶囊与现有参数/前台恢复；普通通知唯一协调；D-082 合法 active/paused 后台心率及 exact target 有界恢复；同一 Application BLE owner、ID `7200` 单 writer。不能以 E17 封口冒称这些未交付部分完成。
- 计时、力量、跟练的实际阶段与差异；开始/中途启用、pause、off/on、缺口、诚实终结、配置变化保留、进程死亡不续跑。D5 的无损观测、开始原子性、准确终态、陈旧请求隔离、原始失败与清理未确定时不放行新写保证保留。
- 单场 recap/历史入口、HR 卡及分析页；raw 实测曲线、阶段带、平均/最高、六区间、raw scrub、结构聚焦、显式横屏/返回恢复、大字/TalkBack/非颜色编码。
- 既有 2500ms 统计有效期、同阶段不超过 20s 的纯视觉虚线、80%/50%/70% 精确充分性、整数分析规则、三个正交状态轴、no-eligible 优先级，以及 prepare/pause 排除主要统计但保留 raw。后续不重新计算已绑定的原始分析。
- 本地优先、用户主动导出、不自动上传；无设备地址/GATT/原始内部诊断进入导出；旧 notes/user feedback 排除条款未被本次静默扩大。
- 记录失败只处理实际数据库/文件/平台边界，不为内部合同排除的状态堆防御逻辑。低空间提醒按E18-R18交给系统，App预检查/阈值/提示从本次范围排除；这不删除实际写入失败的既有处理，也不把系统提醒当作保存成功证明。

### B.2 当前 residual 处置

RES-01–03 的新增 App 内跨场比较和 RES-04 窄进阶提示当前暂缓，外部 AI 使用流程保留；RES-12 批量导出已进入当前范围。RES-05 App 内 AI、RES-06 自动上传/云分析、RES-07 医疗研究、RES-08 新设备/Health Connect/厂商 SDK、RES-09 自由缩放、RES-10 肌群自动配平/动作推荐、RES-13 完整进程续跑及其他历史 future/non-goal，继续按原分类和重访条件保留。旧非 HR 已合并趋势不删除。旧 Epic/Story 数量、12-path、旧 DAG/Route A 及派发顺序不约束新分组。

## C. 已接受架构与主管理退出核对记录

架构高度：`initiative → Epic`。本节固定下级交付不得分叉的责任；具体 Story 局部实现留在后续合同，不以接口数量或文件数声称 capacity PASS。

| 责任 | 唯一主要 owner / 生产归属 | 接口与生命周期约束 |
|---|---|---|
| BLE 和无损规范化观测 | 现有 `HeartRateRuntimeOwner`，`core/health/HeartRateRuntimeOwner.kt` | Application 唯一创建；串行 main-looper facts；同值 BPM 的多次 notify 不折叠。现有显示 StateFlow 不是无损记录输入；不新增第二 GATT owner。 |
| 训练行为与入口 | 现有三模式 engine + 各模式 Activity-retained ViewModel；MainActivity/TrainFlowApp/shell 接入 | 页面只发命令/观察。活动计划入口、engine、时钟与 Recorder 一起跨配置变化保留；真实退出与 process death 分开。每场真实 Start 时固定时间信息。 |
| canonical 排序和本场收尾 | session-scoped `WorkoutSessionTimelineRecorder`，拟创建点沿已讨论的 `core/data/WorkoutSessionTimelineRecorder.kt` | 阶段、规范化观测、记录意图和终结进入同一顺序；不推进 engine、不扫描 BLE、不计算图表。onCleared 同步交接先锁住本场，再由 Recorder-owned 工作收尾；不能在已取消的 viewModelScope 中补做最后保存。 |
| 正常化设备状态映射 | `core/data/CanonicalHeartRateObservationMapper.kt` 的单一 mapper 责任 | 保留 accepted state/reason matrix、receipt 与源边界；不合成生产不可达的 watchdog timeout；不把内部诊断落库。上述新文件名来自旧设计定位，不授权采用 04C 候选内容。 |
| 持久化、准入与事务 | 现有 `WorkoutSessionRepository` + `WorkoutSessionDao` / `CanonicalTimelineHeartRateDao` | D5 准入/释放、原始错误、开始原子性、准确清理、旧 token 隔离保证保留；不另建 registry/manager。现有通用 upsert 拒绝覆盖 canonical；三模式必须正式接入新路径，不删除该 guard。 |
| 终结与原始分析 | 现有 CS-05 `finalizeRecordingSession` / `CanonicalAnalysisV1` / 已有 validators | 同一仓库外层事务可把必要执行/结束信息和已有终结调用一并提交；不复制或重新实现分析/终结 owner。准确同一请求重试识别既有结果。CS-04B 继续负责新进程 durable 封口。 |
| 时间数据与日历归属 | 现有数据库/entity 增量扩展，仓库写入与严格读取 | 当前 DB v5；新时间元数据需要后续增量迁移。开始信息贯通 entity/domain/read/export，不被结束或旧更新覆盖。日历、筛选、按日删除使用同一冻结日期规则。旧记录未知不回填。 |
| 历史/原始分析读取 | 同一仓库提供 version-aware terminal read；复用已有 validator 和 original binding | 新图表/导出不直接用含 fallbackMode 的旧通用读取构造真值。只从当时快照解析名称/结构；每场一致读取，不创建持久化 export snapshot 或第二套原始事实。 |
| 图表投影和页面状态 | 既有 accepted 纯 `HeartRateChartProjector` 责任 + analysis ViewModel/SavedState | canonical 分析仍由 CS-05；竖/横/聚焦共用投影、raw scrub 查真实点；同 Activity 显式横屏，不新增横屏 Activity。大屏方向政策限制仍需对应 UI 证据。 |
| 普通通知/FGS | 现有 `ActiveWorkoutNotificationController` contract 的 Application 唯一协调实例 + connectedDevice Service | 同一 session identity、ID 7200、单 writer 有序交接；Service 不接管 engine/GATT。D-082 有资格的后台断联保留 FGS 并恢复；START_NOT_STICKY，合法启动边界与通知拒绝分支保持。 |
| 导出文件/系统交付 | 一个统一导出 capability，由一个导出页面状态 owner 消费；系统只读 FileProvider / 保存入口 | 单场与批量同格式；每场事务而非整批长事务，逐场写文件，Android JsonWriter/JsonReader 可流式处理。文件全成且验证后交付，24h 清理规则按 B。无新库安装/复制授权。 |

### C.1 版本与时间真值

物理数据库版本、time metadata 来源合同、exportContractVersion 与 analysisVersion 分开。新元数据来源版本为 1；旧未采集记录为空。v2 导出正常结束使用实际观察时间；process_interrupted 的实际 endedAt 未知可为空，另输出由已知 start anchor + trusted offset 推导的终点并标记来源。该推导不填补进程死亡至重启的未知时段，也不伪造原始时区。既有 timeline/analysis v1 语义不因包装升级重算。

### C.2 退出核对的证据层与限制

- 源码证明现有 main 是 Room v5；通用 upsert 明确拒绝 canonical；current finalizer/reconciliation 保存可信相对终点，不意味着保存了实际观察到的 wall end。三模式的生产记录接入尚未实现。
- AndroidX Activity 1.12.3 源码证明配置变化保留 ViewModelStore，真正销毁时清除；Lifecycle ViewModel 2.9.4 源码证明资源/scope 在 onCleared 前清理。因此必须沿既有 D5 的同步 barrier + Recorder-owned handoff，不能靠 onCleared 启动 viewModelScope 收尾。
- Room Android 2.8.4 源码证明 nested suspend transaction 继承同一事务上下文；这是复用同一终结 owner 的机制依据，不是项目原子性测试通过。
- Android FileProvider 临时权限不提供固定十分钟有效期；采用系统授权是用户已接受的保证调整。JsonReader/JsonWriter 从 API 11 提供，当前 minSdk26 支持；不据此声称输出正确或性能达标。
- 当前源码与外部官方来源证明方法/API 边界；实际 dependency resolution、项目接入、Activity/Service 行为、完整字典独立解释、性能、真实 Band/RF/GATT 与 UI 仍各需对应 implementation/evidence gate。
- 主管理退出自检是本次规划阶段记录，不是独立审查。F6 若暴露承重 owner/interface/UX/feasibility 缺口，返回对应节点；不能把缺口塞入一个更大的 Story。

## D. 用户已接受的 Export v2 字段与性能合同

### D.1 无损继承与窄替代

基准源为 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\docs\planning\e17-remainder-epic-story-plan.md`，main=`d2c9ac48027177389092d56c208c64447a3c6a93`，SHA256=`1A8AFD8E9B5E915546D14E583394E8BAD1A786E7C672F0BB62667FAEF62140EE`。其 §7 execution/heartRate 字段、排序、单位、NULL、参数、phaseIdentity、原始分析、durationAudit、正交矩阵和严格验证继续继承；§3–5 的被引用定义同时继承，不重新粘贴成第二套数学/schema authority。

明确替代：旧单场 root 升级为含 sessions 数组的 v2；添加选择说明、共用字典与时间来源。允许可信相对终点已知但实际结束 wall time 未知的 canonical process_interrupted 记录 endedAt 为空，并单独标 derived endpoint。旧 §8 十分钟 lease/boot/new-open 机制由已接受的系统授权与 24h best-effort cleanup 替代；完整准备、只读、外部副本边界和诚实交付提示保留。unknown/corrupt 数据仍不能被默认补值或悄悄丢弃；这不是恢复 E18-R12 所排除的旧勾选产品场景。

### D.2 root 与 selection

根对象仍为 `trainFlowSessionExport`。已接受字段：`exportContractVersion=2`、`generatedAt`（本次生成动作 UTC，含毫秒）、`displayLocale`、`displayContractVersion`、`selection`、`dataDictionary`、`sessions`。单场同样使用 sessions 数组。

selection 字段：`source`（单场入口/日历入口）、`startDateInclusive/endDateInclusive`（含首尾；无日期限制时均为空）、`modeFilter/planFilter`、`includedSessionIds`（不重复、顺序与 sessions 一致）、`includedUnknownDateSessionIds`（实际包含集合的子集）、`selectionScope="user_selected_subset"`。文件没有某天/某场不能证明没有训练。计划筛选不依赖今天的计划名称来重写历史。

每个 sessions 项保留 `session/execution/heartRate`。execution 继续包含 sessionStepRecords/phases/timedRestExtensions/strengthSetRecords；heartRate 继续包含 status/recording/intentAndAcquisition/samples/originalAnalysis/durationAudit。canonical/legacy 与 HR 各轴不混淆；旧 full keys 和 union/matrix 以 D.1 精确来源为合同。

### D.3 时间字段和字典

session 保留 startedAt/endedAt 原始已知值；新 `timeMetadata` 保存 startLocalDate、startZoneId、startUtcOffsetSeconds、sourceContractVersion（新采集 1、旧未采集为空），并包含 startTimestampBasis/endTimestampBasis（实际观察/旧记录原值/未知）、trustedEndAtUtc/trustedEndAtBasis（由锚点及可信 offset 推导或不可用）。字段位置以此处的 timeMetadata 归组澄清，不改变已接受的任何时间语义。

字典必须覆盖所有字段及 planSnapshotJson 内部结构，解释名称、类型、单位、来源、用途、关联、枚举和空值；标清同秒/同毫秒排序、planned/user/device/derived 区分、phase 与 acquisition/统计排除关系、精确整数解析及 legacy step 时间提示的局限。不能用 projected 曲线点代替 raw samples，不能因为字典扩大而删原始事实。完整字段字典实例、枚举序列化细节与生成/解析覆盖须在相应合同/evidence 闭合，不把本索引当作已经生成了可执行 JSON Schema 或完成 consumer 测试。

### D.4 性能验收基准（已接受的目标，未运行）

| 项目 | 单场密集基准 | 多场基准 |
|---|---:|---:|
| 场次 | 1 | 100 |
| canonical samples 总数 | 250000 | 250000 |
| phase intervals 总数 | 10000 | 10000 |
| acquisition intervals 总数 | 10000 | 10000 |
| preparation 最大时间 | 30000ms | 30000ms |
| absolute peak process total PSS | 384MiB | 384MiB |
| 完整 validated UTF-8 JSON | 128MiB | 128MiB |

单场保持旧 8h、same-offset burst<=32、zero-duration boundary、有效期/视觉 gap、各状态/reason/区间/最高点/排除事实密集风险 profile。多场包含三模式，保持同总量来检查分散读取开销；各模式配比、每场 fixture 的合法分配和源码 fixture 身份在相应 evidence 合同固定，不将某个未讨论的配比冒称 accepted 数据。无 HR、zero/no-eligible、legacy、unknown date 等另有功能矩阵，不能为做性能数字从功能范围移除。

沿旧指定环境/JDK/AVD 身份，不新建/升级设备。2 次 warmup、3 次 measured，每次均通过；使用单调计时和每 50ms process totalPss 采样，保留绝对峰值。新 preparation 窗口从开始本次数据读取前到完整写入、flush/file-sync/close、流式结构校验及私有 ready 文件完整发布后；旧已取消的 lease manifest commit 不再成为结束条件。系统 chooser 和外部目标写入不计入 preparation 时间，取消/失败/平台交付另有直接验证。指标不构成用户日期、场次、训练时长上限；超限不能截断、降精度或换设备改善数字。旧 finalizer/projector 基准及其证据边界继续保留。

## E. 新增来源与可复核证据

历史 §6 的 exact 输入身份继续保留；本更新还使用同一 BMAD 技能的 F4/F5/F6 直接 references：

- `C:\Users\25073\.codex\skills\bmad-method\references\ux-interaction-and-visual-contracts.md`。
- `C:\Users\25073\.codex\skills\bmad-method\references\architecture-and-feasibility.md`。
- `C:\Users\25073\.codex\skills\bmad-method\references\epic-story-dag-and-capacity.md`（本次首次加载；只用于成果分组候选，不宣称 Story capacity 完成）。

同一 main 的新增直接源码读取包括以下完整路径（部分文件只读相关段落，不声称全文件 Code Review）：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\app\MainActivity.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\ui\shell\official\TrainFlowApp.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwner.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\model\WorkoutSession.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\model\WorkoutPlan.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\entity\WorkoutSessionEntity.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\WorkoutSessionDao.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\CanonicalTimelineHeartRateDao.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\TrainFlowDatabase.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\CanonicalSessionValidators.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\notifications\ActiveWorkoutNotificationContracts.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TerminalWorkoutSessionRecordWriter.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\StrengthWorkoutSessionUiState.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\HistoryUiState.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\HistoryRoute.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\gradle\libs.versions.toml`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\build.gradle.kts`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\docs\architecture.md`（仅直接相关且未被更晚合同替代的说明）。

### E.1 官方来源（2026-09-05 读取；非运行证据）

- https://developer.android.com/training/secure-file-sharing/share-file — Intent 临时读授权随接收 Activity task 生命周期结束，不是固定 10 分钟。
- https://developer.android.com/training/data-storage/shared/documents-files — ACTION_CREATE_DOCUMENT 系统保存、外部 provider 边界。
- https://developer.android.com/reference/androidx/core/content/FileProvider — 只读 URI 授权与限定目录；Activity/Service grant 生命周期说明。
- https://developer.android.com/training/data-storage/app-specific 和 https://developer.android.com/identity/data/autobackup — 私有文件/缓存与备份排除。最终已接受专用 private 目录排除备份，不把可能被系统提前回收的缓存当作无条件保留保证。
- https://developer.android.com/topic/libraries/architecture/viewmodel — scope/lifecycle；当前文档的新 API 不自动等于项目实际 resolved 版本可用。
- https://developer.android.com/develop/background-work/services/fgs/service-types 和 https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start — connectedDevice 权限/合法启动边界；部分检索片段不构成覆盖所有例外的完整证明。
- https://developer.android.com/reference/android/util/JsonWriter 和 https://developer.android.com/reference/android/util/JsonReader — API 11 起可用的流式 JSON API。

以下官方固定版本 sources JAR 只在内存读取，未安装/复制到项目：

| 完整来源 | SHA256 | 实际证明范围 |
|---|---|---|
| https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.12.3/activity-1.12.3-sources.jar | `9FF6A4EDD9C650E52832B0D5B99258998CD05564666D4B0E91257C7E5AAB7B1A` | ComponentActivity.kt 配置保留与 ViewModelStore 清除 |
| https://dl.google.com/dl/android/maven2/androidx/core/core/1.16.0/core-1.16.0-sources.jar | `ED8EC0B5D74352B852C5EDA44A6F21CDB726FD2A94293DC2609219330136B207` | FileProvider.java openFile/grant/path 机制，无 fixed lease 验证 |
| https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/2.8.4/room-runtime-2.8.4-sources.jar | `FCA3B96C419811C469890006226138071BE388A933CC95C23BD0FBE63F7E2C61` | common transaction 说明；不单独替代 Android compat 路径 |
| https://dl.google.com/dl/android/maven2/androidx/room/room-runtime-android/2.8.4/room-runtime-android-2.8.4-sources.jar | `86F67184ADAF59862C237296EBE092BC45923E37D8D74DCCD5A031BB7C5A09AE` | RoomDatabase.android.kt 2040–2066 nested withTransaction 继承上下文 |
| https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-android/2.9.4/lifecycle-viewmodel-android-2.9.4-sources.jar | `7FC71221F470885593B974FE086E5B14EE24669A94C010254DE1C64FB4C2218D` | ViewModel clearing sequence 与 ViewModelImpl 清理 scope 的先后 |

另读取 activity/activity-compose 1.12.3 官方 POM，证明其声明的 core/lifecycle 依赖；没有执行 Gradle dependency resolution，因此不宣称实际 resolved graph 已 fresh 核验。完整 POM locator 分别为 `https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.12.3/activity-1.12.3.pom` 和 `https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/1.12.3/activity-compose-1.12.3.pom`。

## F. 已接受：E18–E22 五成果组

### F.1 五成果组、新顺序与阶段边界（用户已接受）

用户明确授权增加到 E22 并重新排序，不因此增加产品能力。主管理展示下列五组及阶段边界后，用户明确回复“接受”，并追问跟练与力量放在同组的相似性。五组、顺序与阶段完成边界为 `USER_ACCEPTED`；正式 Story 切分、依赖身份和 capacity 尚未完成。此前将三模式生命周期全部前移到后台 Epic 的建议已撤回，将全部三模式记录直接移入 E18 的四组替代也未被单独接受。F.2 仅保留此前分组与讨论来源，不作为当前执行顺序。

| 已接受编号与成果 | 完成时用户能获得什么 | 责任与前置边界 | 所需证据，不代表已执行 |
|---|---|---|---|
| E18 计时训练真实记录与可信保存 | 一次计时/HIIT 训练的实际阶段、轮次、暂停/额外休息、原始心率及开始时间能真实记录；完成或提前结束后可从历史读取本场记录；无心率仍能保存，进程中断按既有规则诚实封口。 | 仅接通计时模式的运行生命周期与新记录生产链。必要公共时间元数据、无损观测、仓库协议、Recorder、严格历史读取分别评估 Story；保留已合并 CS-03/04A/05/04B，不新增动作次数录入，不依赖后续图表/导出才形成可用成果。 | 公共部分匹配 migration/真实 Room/顺序与终结 oracle；计时接入匹配真实 Start、phase、pause/off-on、退出、配置重建及新进程 oracle；本场历史入口证明持久化事实、original binding 和删除失效。 |
| E19 力量与跟练真实记录接入 | 力量能保存实际动作/组、prepare/confirm/rest、实际重量/次数/effort；跟练能保存自身既有动作与阶段。两种模式均有可信时间、结束/中断记录和历史读取。 | 复用 E18 已验证的公共记录与读取合同；力量与跟练分别处理各自运行生命周期、实际事实映射和生产接入，分别验收。不得把跟练冒充计时，不重新建立 Recorder/分析 owner；不等 E20 才提供本组承诺的记录结果。 | 各模式独立的真实 engine→记录→历史证据；力量替换/跳过/确认/prepare 排除语义与跟练结构身份各有 oracle；配置保留、退出、重复终结、新进程封口及保留旧功能分别验证。 |
| E20 训练通知、锁屏与后台心率 | 三模式在合法 active/paused 后台条件下保持心率连接，并在资格成立时恢复 exact target；通知准确反映训练和恢复状态，结束或资格消失时正确停止。 | 消费 E18/E19 已稳定的训练身份与运行状态。普通通知唯一协调与 FGS/后台接合分别评估 Story；复用 Application 唯一 BLE owner。无需再次迁移三模式 engine，不重做已合并前台恢复。 | 普通通知单 writer/陈旧更新隔离；Android Service、权限、前后台与通知交接证据；真实 Band/RF/GATT 的连接保持、断联恢复及停止矩阵。后台所得真实样本仍进入已有记录链，按同一最终 executable 身份证明。 |
| E21 日历选择与完整数据导出 | 按日期、单/多模式、单/多计划与逐场勾选导出完整自描述 JSON；单场入口、系统保存/分享、进度、取消及文件清理可用，可交给外部 AI。 | 消费 E18/E19 的时间、terminal 与严格读取合同；覆盖已有及后续 E20 所得真实记录，不依赖图表。数据字典/编码、逐场生成与文件生命周期、选择页面、系统交付分别评估责任与 Story。 | 完整原始字段与独立 consumer 解释、未知日期/跨午夜/多模式筛选、页面重选/旋转/取消、系统交付/临时权限/清理、D 节单场及多场性能合同。 |
| E22 单场复盘与心率图表 | 三模式单场 recap/HR 卡、实测曲线、阶段与区间、raw 点查看、结构聚焦、横屏与返回恢复可用；大字/TalkBack/非颜色表达闭合。 | 消费 E18/E19 同一严格读取与 original analysis；保留全部已接受图表数学与 UX。投影、卡片/事实摘要、分析交互、横屏分别评估 Story；复用 E21 导出入口，不新增 App 内跨场比较或长期评价。 | 纯投影精确数学/锚点/raw 与显示点区别及既定性能；不足/无 recording/零样本/无 eligible/无 zones 矩阵；真实 UI、导航/方向/状态恢复和人工可访问性证据随各页面交付。 |

第一组仍承担必要的公共记录工作，但只以一种真实模式完成端到端生产闭环。它不是一个“共享底座 Story”：物理迁移、runtime 观测、仓库事务/准入、Recorder 排序、mode 生命周期/接入、历史消费者的独立责任不能因都服务计时训练而揉成一项。后续逐项完成十维 capacity 后才能确定正式 Story 数量；本次不声称 E18 已足够小或 capacity 已通过。

选择计时先行的已接受优先级，依据用户本轮明确描述的 HIIT 使用目标与可先取得一场完整记录的价值；不是从旧 CS-06/Route A 编号自动恢复派发。E19 内力量与跟练必须分别成因果完整的接入任务，不能再以“三模式接入”作为一个统包任务。

E19 同组理由澄清：两者共享 E18 的 session 时间坐标、原始 HR、Recorder、可信终结/原始分析绑定、严格历史读取等公共能力，但不共享一套模式执行语义。现有跟练复用 TimedWorkoutEngine，其既有动作/阶段更接近计时；力量有独立的动作/组、prepare/confirm/rest 以及实际重量/次数/effort。E19 的用户成果是把已验证记录能力覆盖到其余两种模式，不是声称两模式高度同构，也不授权共同重写 engine、合并事实 mapper 或合成一个 Writer/Review 任务。两者分别接入、分别验收；现有跟练范围保持不扩张。

分阶段承诺：E18/E19 完成的是当时已支持运行条件下的真实记录与可信保存；合法锁屏/后台持续心率保证在 E20 才交付。前两组不承诺后台持续采集，不用插值/补点/伪造完整覆盖掩盖缺口；原有显式用户排除与设备缺口仍区分。E20 是明确保留的后续能力，不能被前两组验收吞掉或冒称完成。未接通新链的模式继续保留既有能力，直到其 E19 接入完成，不把普通旧记录冒称 canonical 新记录。

候选依赖和价值顺序分开：

- E19 的公共生产/读取前提来自 E18；E20 的完整三模式后台成果消费 E18/E19 稳定的训练状态。
- E21/E22 的数据合同前提来自 E18/E19；E20 在它们之前交付，是本轮推荐的产品价值顺序，不把 FGS 代码虚构成所有导出/投影算法的硬依赖。
- E21 先于 E22，继续服务用户外部 AI 比较/长期分析目标；图表投影本身不因该价值排序依赖导出实现。
- 每个 Epic 在自己的阶段提供上述可用结果。必要的历史读取不能留到 E22；普通通知/FGS 不再要求未来阶段先完成生命周期迁移。具体验证随各项交付，不新增最后统一补证的 Epic。

需求 lineage 候选归属：E18/E19 分别承担各模式的 E17-CAP-04/07–13/21/23、E17-ARCH-05–25、CT-01–05/09–12 与 E18-R03/07/08/16；公共义务由 E18 内唯一 implementing Story 持有，E19 是明确消费者，不重复实现。E20 承担 E17-CAP-05、D-082 的未交付普通通知/后台部分及其与记录链的接合；E21 承担 E17-CAP-22/23、CT-06/07/08/12、E18-R04–17 的选择/导出消费者与 D 节；E22 承担 E17-CAP-14–20、AD-U-001–021/024/025、E17-ARCH-01–04/11–16、CT-04/05/08 的呈现消费者。跨节点条目须在正式矩阵细分 producer/consumer obligation 并保留唯一主要责任，当前索引映射不冒充 `unmapped=0`。

所有非目标、暂缓与保留资产继续沿 B 节；新增 Epic 只拆现有范围。正式 Story、immutable 节点/前置身份、完整 DAG、精确路径/AC/evidence、十项 capacity 和独立 Planning Review 均未完成，保持 `NOT_READY`。若需要改变既有 core owner/interface/data responsibility，返回对应架构门禁；不让增加一个 Epic 替代设计闭合。

五成果组及其顺序、阶段边界已确认，不再重复询问。主管理下一动作是形成精确 Story 边界、义务映射、依赖与十项 capacity 结果。普通技术拆分沿已接受语义推进；若出现新的承重范围/价值或架构决定，单独呈现。完整设计尚未形成，后续适用的独立规划审查/readiness 门禁未解锁，本次不派发角色。

### F.2 历史：此前接受的四成果组及未接受的前置调整

以下保留当时内容。其四组顺序因用户最新指令进入重排，旧“唯一下一门禁”等时态只指当时，不覆盖当前 F.1 与 G。先前接受的产品/架构语义继续由 B–D 保持；旧前置调整已撤回，不能据此派发。

本节的四成果组与价值顺序为 `USER_ACCEPTED`：用户在主管理展示四组及“导出先于图表”后明确回复“接受”。E18–E21 是已接受的成果分组编号；这不接受尚未生成的 Story 合同或派发序列。每组围绕可用成果，而非 DB/API/UI 技术层；组内必须再按独立 owner/lifecycle/evidence 拆 Story，尤其不把共同记录基础与三模式接入塞回一个 04C。

| 候选 | 完整用户成果与 done 边界 | 前提 / 责任与证据 |
|---|---|---|
| E18 训练中的锁屏与后台心率 | 训练中合法锁屏/临时后台连接及有资格的断联恢复；同一通知准确反映状态。不是对无 FGS 场景可靠后台计时的扩大承诺。 | 已合并胶囊/Runtime；普通通知协调、FGS 交接分开安排；Android 与真实 Band 证据随交付，不推迟到最后。 |
| E19 三模式真实记录与可信保存 | 计时、力量、跟练均接入真实阶段/HR/时间元数据、可信终结与进程中断封口；完成后可从历史读取/核验本场记录。 | 复用 CS-03/04A/05/04B；共同观测/记录/事务边界、三模式各自接入、必要的严格历史读取分别拆分；保存成果不依赖未来图表才成立。 |
| E20 日历选择与完整数据导出 | 日历/日期输入、模式/计划/逐场选择，新版自描述单/多场 JSON，保存与系统分享、取消和临时文件清理完整可用。 | 依赖 E19 时间/terminal/严格读取合同；不依赖图表投影或分析页面。交付可直接服务用户外部 AI 分析目标。 |
| E21 单场复盘与心率图表 | 单场 recap/HR 卡/完整曲线/阶段/区间/raw scrub/聚焦/横屏/返回/可访问性，保留所有既有图表语义。 | 依赖 E19 的 raw 与 original analysis；使用同一严格读取。已有导出入口可复用。无新跨场比较/长期评价。 |

已接受的价值顺序：先补后台心率 → 三模式可信记录 → 可选择的数据导出 → 单场图表。导出先于图表，服务用户将跨场比较和长期分析交给外部 AI 的目标。先图表后导出的替代顺序未被选用，不再作为当前待确认问题。

这只是 Epic 级先后约束，**不是所有 Story 必须串行的 DAG**。记录基础中不依赖 FGS 的部分、图表与导出的共同读取边界等，后续依据实际依赖构建边。E19 必须包含完成自身用户成果所需的历史读取，不把它藏在未来 E20/E21；不让某 Epic 等到未来 Epic 才满足当前 done 承诺。

当前 evidence/capacity：正式 Story 未创建，十维 capacity=`NOT_EVALUATED`；完整 obligation→Story→AC→oracle 映射、DAG 检查与 exact prerequisites/readiness 尚未执行。未来不可用 prerequisite 明确保持未解锁。图表、schema、导出、FGS、各 mode 的独立证据均归其交付，不另造末尾大测试 Epic 替前面补证。

#### F.2.1 历史 Story 分解草案与前置责任提议（已撤回）

本小节为新产生的 `PLANNER_PROPOSAL / NOT_READY`，不是上述“接受”的追溯对象。以下是因果边界候选，不是正式 Story identity，也不以条目数承诺最终 Story 数量。

本次直接核对同一 main 的三个 mode Route：session identity、engine 等仍由 `remember` 持有，通知 controller 仍在各 Route 构造，`onDispose` 会提交 `ROUTE_DISPOSED` 清理。既有 D-082 要求训练后台保持同一 Application owner，普通页面停止不是断开信号。C 节已经接受的 Activity-retained mode ViewModel 可承接此责任；无需新增生命周期架构，但不能让 E18 的完成状态等待 E19 才获得有效训练身份。

推荐窄调整：将 E18 自身必需的三模式运行状态保持分别列入 E18；E19 仍各自负责三模式真实记录接入。每个模式分成先稳定运行生命周期、再接真实记录的两个因果边界，不把迁移三模式和整套 Recorder 合为一项。这里不扩大无 FGS 场景的后台计时承诺，不恢复进程死亡后的训练。

| 成果组 | 拆分候选：每个分号隔开的责任需分别评估 Story 边界 | 直接前提与验收重点 |
|---|---|---|
| E18 | 计时运行状态保持；力量运行状态保持；跟练运行状态保持；唯一普通通知协调；合法 FGS 与训练后台保持/有资格恢复 | 三模式各自证明配置重建、真实退出、session identity 与现有训练行为；协调者证明迟到更新隔离和单 writer；FGS 消费稳定训练身份与协调者，Android 生命周期/通知证据和真实 Band 连接/恢复证据分别闭合。前台已合并恢复不重做。 |
| E19 | 必要时间元数据增量存储；无损规范化观测；仓库准入/写入/终结协议；本场串行 Recorder；计时记录接入；力量记录接入；跟练记录接入；严格历史读取及本场记录可核验入口 | 复用 CS-03/04A/05/04B。迁移证明旧值保持未知及已有数据不丢；观测证明同值测量不折叠；仓库与 Recorder 分别匹配真实 Room 事务和有序输入/退出 barrier；各模式证明真实生产阶段与反馈进入持久化；历史读取证明 original binding、legacy 区分、当前计划修改不污染历史及删除失效。仓库协议是否需进一步拆分仍待 capacity 核对，不能预先称一个小 Story。 |
| E20 | v2 完整编码及自描述字典；逐场生成、完整校验与私有文件生命周期；日历/日期/模式/计划/逐场选择及页面状态；系统保存/分享与单场入口接入 | 共同消费 E19 严格读取；编码有独立 consumer 解释/字段完整性 oracle，生成有单/多场性能及取消/清理 oracle，选择有未知日期/跨午夜/返回重选 oracle，交付有系统界面/权限/诚实成功语义 oracle。各页面必须在自己的交付中完成实际接线，不能依赖未来图表才可用。文件生成与页面/系统交付分别持有其生命周期，不能为拆分而另造 owner。 |
| E21 | 无状态图表投影；单场复盘 HR 卡与事实摘要；竖屏分析、区间、raw 点查看与结构聚焦；显式横屏与往返状态保持 | 消费 E19 同一原始记录和 original analysis。投影承担数学/锚点/raw 与显示点区别及性能；卡片承担零样本/无 eligible/无 zones/低覆盖等事实呈现；分析交互和横屏各有真实 UI、配置/返回及人工可访问性证据。可访问性属于每个页面自身完成条件，不推迟到最后一项。 |

来源 lineage：E18 主要消费 E17-CAP-05、E17-ARCH-25、D-082 与 C 节训练/通知 owner；E19 消费 E17-CAP-04/07–13/21/23、E17-ARCH-05–25、CT-01–05/09–12 及 E18-R03/07/08/16；E20 消费 E17-CAP-22/23、CT-06/07/08/12、E18-R04–17 及 D 节；E21 消费 E17-CAP-14–20、AD-U-001–021/024/025、E17-ARCH-01–04/11–16、CT-04/05/08。E17-CAP-24 随每个消费者承担验证。该表是索引级候选映射，尚不是细粒度 obligation→Story→AC→oracle 的完整双向覆盖证明。

候选依赖：各模式运行状态保持 → 该模式记录接入；稳定训练身份 + 唯一普通通知协调 → FGS；时间数据/观测/仓库协议 → Recorder → 各模式记录接入；可信持久化合同 → 严格历史读取；严格历史读取 → 导出与图表各自消费者。E20 的价值交付排在 E21 前，不人为增加“图表算法必须依赖导出实现”的代码依赖。

以上仅为依赖方向，不冒称完整 DAG。新前置产物均尚不存在，其 immutable identity、exact allowed paths、每条 AC 和十项 capacity 尚未闭合，全部保持 `NOT_READY`。正式化时若发现一个候选仍聚合多个独立 owner/lifecycle/evidence，则继续拆；若需要改变已接受 owner/interface/data responsibility，返回 F5。已合并源身份沿 A 与历史资产表，新节点不得用 branch 或本段文字冒充已满足 prerequisite。

唯一待用户讨论的变化：是否接受将 E18 自身必需的三模式运行状态保持提前纳入 E18，分别交付；E19 保留各自的真实记录接入。推荐接受，四个成果组及其价值顺序均保持。若不接受前移，则 E18 的完整交付次序须回到成果排序讨论，不能暗中依赖未来 E19。其余技术拆分仍由主管理依照 capacity 完成，不逐项新增产品批准。

本次额外读取的直接来源：同一 immutable main 下 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\docs\planning\e17-auto-reconnect-and-personal-parameters-correct-course.md` 的 §3/6/7，以及历史 §6 已列三个 mode Route 与通知合同；V1 §7 与 D5 相关段落定位。没有读取/采用 04C 候选代码。用户最新接受只针对 F 节四成果组与价值顺序。

### F.3 Story 责任分解 DRAFT-1（当前主管理工作稿）

用户再次回复“ok，接受”，确认 E19 共用记录基础、力量与跟练分别接入/验收；五成果组不再重问。本节是该接受之后产生的技术分解候选，不能追溯为用户已经接受的 Story 合同。ID `E18-S01` 等是本工作稿内的候选标签，尚不是 immutable 可派发 Story。普通技术拆分不逐项创建新的产品确认门禁。

#### F.3.1 E18：计时训练真实记录的九个责任单元

| 候选 | 因果完整的 old→new 与唯一主要责任 | 来源义务及独立验收边界 | 候选直接前置 |
|---|---|---|---|
| E18-S01 时间元数据持久化 | 当前实体/领域记录没有冻结的当地开始日期、时区及偏移 → 现有库增量承载已接受时间元数据，现有映射/更新保持该事实。只负责数据保存与兼容，不采集训练事件、不新增另一记录库。 | E18-R07/08/16；迁移旧记录不猜时区，新增信息 round-trip，旧写入口不能擦除已有开始信息。物理 schema/版本和所有直接 mapping 消费者需一起固定；旧 v5 与 canonical 语义保留。 | 已合并 main 资产 |
| E18-S02 无损规范化心率观测 | 现有显示 StateFlow 不保证每次同值测量都被消费 → 同一 HeartRateRuntimeOwner 提供 D5 绑定/receipt/snapshot 观测，并按已接受矩阵正常化。 | CT-09、E17-ARCH-18/19；D5 §4.1。无 backlog、相同 BPM 多次 notify 不折叠、receipt 顺序、exact bind/unbind disposition、无设备内部标识落入载荷；不改变 BLE owner、恢复/freshness policy 或添加 watchdog。runtime+纯映射是同一规范化观测输出边界，不包括 Room/Recorder。 | 已合并 main 资产 |
| E18-S03 原子开始与活动持久化 | 现有 startCanonicalSession 只插 session/initial phase，既有 guarded append 分散存在 → 一次 Start 事务容纳冻结初始化批次与可选 HR，活动批次按 expected tuple 原子提交。 | CT-01/03/09、D5 §4.2、E17-CAP-04/07–10；真实 Room 证明全提交/全回滚、相同请求结果丢失后的完整 row graph 识别、同毫秒顺序、phase/intent/acquisition partition、zero-sample 阶段。沿现有 DAO/validator，不另造 digest/schema 或获取 runtime owner。 | E18-S01、E18-S02 的数据/receipt 合同 |
| E18-S04 可信终结持久化接合 | 已有 CS-05 管理 recording graph 与 original analysis 终结，但未完整承接本次执行结果/实际结束时间；普通 no-recording terminal 也需补齐 → 仓库同一外层事务提交执行结果/已知结束信息及适用终结分支。 | CT-10、E17-CAP-12/13、E18-R07/16、D5 §4.3；有 HR 调用原 CS-05，无 HR 不造 HR/snapshot 行；expected tuple/rowCount、readback、全回滚、同请求幂等、晚到请求不能改历史。实际结束信息来自冻结的终结请求；重试不重新采时写成新的结束时刻。process_interrupted 仍由已合并 CS-04A/04B 按 durable 事实处理，不补未知 wall end。 | E18-S01；已合并 CS-05/04B |
| E18-S05 唯一仓库准入与释放 | 当前 prepareRecorder 直接复用已完成 cache，尚无 D5 完整单场 ownership gate → 现有 repository gate 管理独占 token、PENDING/BLOCKED 与 cache 释放顺序。 | CT-10/11、D5 §5–6.1；OPEN/ACTIVE/PENDING/BLOCKED、抢占前 cache 检查、真实 Room 回滚/终结结果、binding 明确 absent/installed/conflict/unresolved、exact unbind 后才能释放、原始失败和 stale token 不扰动新场。只管理既有仓库准入/释放，不创建 Recorder/Activity owner；生产方法直接并发证据不能冒称 Android 生命周期证据。 | E18-S02、E18-S03、E18-S04 |
| E18-S06 单场串行 Recorder | runtime/engine 的事实尚无生产串行记录 owner → session-scoped Recorder 串行接合上述生产能力，保存 canonical 顺序并独立承接退出收尾。 | CT-09/10/11、E17-ARCH-05/08/17/20、D5 §4–6.1；冻结 receipt cut、post-cut 排队、late enable 真实边界、off/on 同 identity、ACTIVE_PERSISTENCE_FAILED 不驱动 engine、on-clear 同步 barrier 后 Recorder-owned 收尾、terminal/clear 竞争与 stale request。只编排调用，不重复定义 DB 事务、准入状态机或分析算法。 | E18-S05（传递包含 S02/03/04） |
| E18-S07 计时模式生产接入 | 计时 session/engine/clock 仍由 Route remember 持有，terminal 走普通 legacy mapper/write → Activity-retained 计时状态在真实 Start 接入 Recorder、canonical phase/metadata 和冻结参数，结束后保留真实持久化结果。 | E17-CAP-02/04/07–13、E17-ARCH-22/23/25、CT-04/05/11、E18-R03/07；legacy_timed/composition 的精确 family/index/真实 work-rest predicate、pause/extra-rest、Start gate、配置保持、真实退出及 fresh-process gate。真实 Activity/生产调用证明，不以 helper/barrier 测试代替；不新增动作次数。保留既有倒计时、提醒、音振和终态体验的直接回归。 | E18-S06 |
| E18-S08 严格历史读取 | 当前通用记录读取含宽松 domain decode，未提供完整 version-aware terminal graph/original binding 读取 → 同一仓库提供 canonical/legacy 严格读取，供历史、导出、图表共同消费。 | E17-CAP-13/21/23、CT-02/03/12、E18-R08/16；canonical 复用已有 strict validator，legacy 保留真实 NULL version且不 fallback/default/mapNotNull 丢项，original binding 不回退 latest、不重算、不回写。每场一致读取、缺失/非法显式区分，删除后不能返回已缓存完整旧 graph。列表不能为展示摘要无条件加载全部历史 raw samples。 | E18-S01、E18-S04；已合并 validator/snapshot 资产 |
| E18-S09 计时记录历史入口闭合 | 新链记录不能仅证明数据库里有数据 → 现有历史/结束后入口消费严格持久化结果，可查看本场已知时间、执行事实及可信终态；当前计划修改不污染结果。 | E18 的 done、E17-CAP-21/23、E18-R07/08/16；真实生产计时结果进入页面、冻结日期归组与按日删除一致、未知日期不冒充准确当地日、删除后 detail 失效、既有非 HR 趋势与其他模式旧记录保留。这里只补 E18 必需的可用入口，不提前实现 E22 HR 卡/分析曲线。 | E18-S07、E18-S08 |

S03 与 S04 分别承担开始/活动事务和终结事务；S05 承担其外侧的准入/释放状态，S06 消费这些结果并编排。**不能为了更少节点把 S02–S06 再合成一个共享底座 Story。** 同样不能只按文件把一个原子 Start transaction 拆成多次独立提交；冻结初始化 cut 内的插入与观测折叠是 S03 单一事务义务。

旧 D5 的开始/清理/终结保证完整保留，旧 12-path、单 Story、capacity PASS 和 Route A 先后不作为新结构约束。S05 中 rollback、bind result-loss、clear race、cache/release 的完整矩阵仍需在自己的合同一次闭合；若十项 capacity 证明还存在多个独立状态/证据责任，继续拆分，不因本表已经写成九项就锁死数量。

#### F.3.2 E19–E22 的消费者候选

| 候选 | 独立结果与责任 | 关键 AC / 所需 oracle | 候选直接前置 |
|---|---|---|---|
| E19-S01 力量模式记录接入 | 在同一公共记录与历史合同上闭合力量的 runtime 生命周期、真实阶段与实际组记录。 | CT-04 strength family；prepare raw 排除主要统计、confirm/rest、实际重量/次数/effort、替换/跳过 append-only metadata、配置/退出/中断与严格历史呈现；同一模式真实 engine→Room→历史 + Android evidence。不得重新创建 Recorder 或分析 owner。 | E18-S09 |
| E19-S02 跟练模式记录接入 | 独立闭合现有跟练范围的生命周期、动作/阶段和记录消费。 | CT-04 follow_along family 独立于 timed family；同一公共时间/HR/终结合同、真实跟练入口/配置/退出/中断/历史 oracle。不扩张课程或视频能力，不因复用 TimedWorkoutEngine 伪造 timed identity。 | E18-S09（力量实现不是前置） |
| E20-S01 普通通知唯一协调 | 三模式 Route 分别拥有 notification controller → Application 唯一协调者消费稳定 session/producer/version，ID 7200 单 writer。 | CAP-05、D-082 未交付普通部分；迟到 producer/version、终态、Route dispose、权限拒绝和有效更新的生产证据；不加入 FGS 或第二 BLE owner。 | E18-S07、E19-S01、E19-S02 |
| E20-S02 合法后台心率与 FGS 接合 | 稳定训练状态和普通协调者接通合法 connectedDevice FGS，active/paused 锁屏保持及有资格恢复，真实数据继续进入既有 Recorder。 | D-082 eligible/停止矩阵、START_NOT_STICKY、普通/FGS 单 writer 交接、不断链同 attempt与断链后新 attempt 区分。Android 与真实 Band 证据分别承担，不互相冒充；不把普通无 FGS 场景扩大为可靠后台计时。 | E20-S01、E18-S06 |
| E21-S01 自描述 Export v2 编码 | 严格持久化输入 → 单一 v2 JSON 内容合同/完整字典与独立解析结果。 | D 节和继承 §7 全字段/类型/NULL/order/union；同值 raw 不丢、planSnapshotJson 原文及内部字典、每秒多数据来源关联、未知日期和用户选择子集说明；独立 consumer 不靠聊天解释文件。不得输出 projected 点或设备内部标识。 | E18-S08 |
| E21-S02 完整生成与私有文件生命周期 | 编码输出 → 逐场一致读取/顺序写入一个临时文件，完整校验后 ready，并按接受规则取消/失败/中断/24h 清理。 | E18-R13–15、D 性能；真实文件写入、flush/sync/close/校验/ready 边界、部分文件不交付、保存后/分享后不同清理处置、目录排除备份。按 D 单场/多场验收，不加常驻导出 Service。 | E21-S01 |
| E21-S03 日历和导出选择页 | 现有训练记录 → 可按日历/输入日期、模式、计划及逐场选择的实际页面状态。 | E18-R04–08/12；同日多模式数量、无记录日、含首尾/跨午夜/冻结日期、未知旧日期单列、排除误取消场次、离开到 App 其他页清空并重新加载。需固定页面生产接线，不能只交静态日历。该节点不冒称已经完成文件交付。 | E18-S09、E19-S01、E19-S02 |
| E21-S04 保存/分享与导出流程闭合 | 单场入口和选择页消费统一生成能力，旋转/取消/系统选择器/保存/分享全过程可用。 | E18-R11–15；旋转保持、主动 App 内离开取消、系统交付不当作取消、进程终止不自动重发、临时只读授权及诚实提示；真实系统交付 evidence。依赖 S02 的文件状态，不重做编码或清理 owner。 | E21-S02、E21-S03 |
| E22-S01 无状态图表投影 | 同一 raw 与 original analysis → 符合已接受合同的曲线、阶段和聚焦结构投影。 | E17-ARCH-01/02、CT-04/05/08；mandatory anchors、分实测段、同阶段 20s 虚线、2500ms 统计规则不混用、raw scrub 不用显示点替代、既定密集性能 oracle。不持页面状态或算第二份 analysis。 | E18-S08、E18-S07 的计时结构 predicate 合同 |
| E22-S02 单场 HR 卡与事实摘要 | 已有结束/历史入口 → 独立紧凑 HR 卡和诚实事实/不足状态。 | E17-CAP-14/18/19、AD-U-001/002/014–017/024/025；completed/abandoned、not_recorded/zero/no_eligible/no_zones、80/50/70、主要统计/排除单列、重大不足持续可见；三模式真实来源、UI/人工可访问性证据。 | E18-S09、E19-S01、E19-S02 |
| E22-S03 竖屏分析与点位/结构交互 | HR 卡/历史入口 → 独立可用单场分析，曲线、区间、raw 点查看、顺序导航与聚焦。 | CAP-15–17/20、AD-U-003–009/013/018–021/025；点击详情/拖动scrub/显式聚焦区分、六区间与未覆盖、大字/TalkBack/非颜色、同一持久化来源/删除失效；真实 UI 和手工体验 oracle。既有 E21 导出入口复用，不扩新比较。 | E22-S01、E22-S02 |
| E22-S04 显式横屏与返回保持 | 已可用竖屏分析 → 同 Activity 显式横屏，继承选择并在返回时恢复视角/焦点/滚动/scrub。 | AD-U-010–012/021、E17-ARCH-03/04；真实 Activity 方向/重建/返回、系统大屏方向约束下竖屏仍可用、可访问性；不新增另一分析 owner或横屏 Activity。 | E22-S03 |

以上21个候选标签只是本轮分解结果，不是用户硬性数量或 capacity 证明；组内仍可能因因果完整性继续拆分。未来前置的实现 full SHA 尚不存在，不以候选标签、branch 或旧 Writer 报告冒充满足。用户接受的 Epic 交付顺序仍为 E18→E19→E20→E21→E22；表内代码/合同依赖不虚构成所有技术工作都必须串行。正式手工派发仍每次只解锁一个 exact Story。

#### F.3.3 生产范围定位与未完成的精确绑定

以下均是同一 main 下本轮直接检查的生产定位，**不是此主管理的代码修改授权，也不是可直接交给 Writer 的最终 allowed-path 清单**：

| 消费候选 | 已定位的完整 literal path |
|---|---|
| E18-S01 | `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\TrainFlowDatabase.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\entity\WorkoutSessionEntity.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\model\WorkoutSession.kt` |
| E18-S02 | `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwner.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateBoundary.kt`；新 mapper 责任沿 C 节定位，未读取 04C 实现 |
| E18-S03/04/05/08 | `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionRepository.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\WorkoutSessionDao.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\CanonicalTimelineHeartRateDao.kt` |
| E18-S07、E19 消费者 | `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\WorkoutSessionRecordMappers.kt`；其余 Activity/shell/三个 Route 定位沿 E 与历史 §6 |
| E18-S09 | `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\HistoryUiState.kt`；`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\HistoryRoute.kt`；实际 App 生产连接沿 E 节 |

现有测试/环境的定位沿 main 原文件与用户指定技术源：migration、guarded-write、reconciliation、finalizer、runtime、mode mapper/UI 与 history 测试均已存在相关入口；新增测试/源码 artifact 的 exact literal path、正式 Story identity 和完整命令集合尚未固定，不从本候选 ID 自动派生文件名。只运行了只读源码/文件定位和规划图检查，没有运行项目测试/build/设备验证。

#### F.3.4 候选依赖图与 capacity 初检

本轮对上述21个标签和表内34条直接边执行内存机械检查：无未声明端点、无 self-edge、无重复边，拓扑可排序。该结果仅证明**这张候选标签图**无环；不证明 immutable prerequisite 满足、完整义务覆盖、真实可实施或 CAPACITY_PASS。按 accepted Epic 次序的一个合法排列为 E18-S01…S09 → E19-S01…S02 → E20-S01…S02 → E21-S01…S04 → E22-S01…S04；同组标签顺序只是候选 source order，尚未形成正式 canonical ordered key contract，不生成 planning-status 文件。

十项 capacity 当前结果按可证明范围记录：

| 判据 | 当前结果与依据 |
|---|---|
| 1 Value coherence | 初检可定位各单元 old→new；E18 以真实计时记录和历史入口收口，其他组也有自己的完成状态。完整逐 Story 判定尚未完成。 |
| 2 Obligation closure | UNKNOWN：本节已有来源与直接消费者映射，但127项历史索引及新增17项决定内部仍需细粒度 implementing/dependency/residual 处置，不能从表行数宣布无遗漏。 |
| 3 Owner stability | 沿 C 节 accepted owners，未提议第二 authority；逐接口签名与各 Story 最终边界仍待绑定。 |
| 4 Lifecycle coherence | S02 观测、S03/04 事务、S05 仓库准入、S06 单场编排、S07 Android生命周期、S08 读取、S09页面消费已明确区分；S05/S06 和模式接入必须进一步核对完整 transition 与跨边错误，不自动 PASS。 |
| 5 Dependency closure | 候选标签图无环；新实现身份未产生，所有 dependent Story 保持未解锁。已有 full SHA 资产沿 A 与历史表，不改为 branch-only prerequisite。 |
| 6 Production boundary | UNKNOWN：已有上述代码定位，但 exact per-Story allowed/protected paths、新增文件与新 schema 的全部物理合同尚未完成，不能使用目录范围作为最终 Writer 授权。 |
| 7 Evidence coherence | UNKNOWN：本节固定主要所需证据层，仍需每条 AC 对应独立 oracle、exact fixture/环境/artifact 与明确消费者。未执行任何运行/性能/设备/UI证据。 |
| 8 Single-Writer feasibility | UNKNOWN：依赖完整接口/错误/物理schema及允许路径闭合，不能由 Writer 首次补承重决定。 |
| 9 Single-Reviewer judgeability | UNKNOWN：当前不是一份可独立全轴 Review 的 immutable Story 合同，也没有独立审查 verdict。 |
| 10 Failure/recovery closure | D5完整失败/回滚/清理保证及本轮导出生命周期均有来源；需要逐 Story 把 producer/consumer 的失败交接和同请求 identity 明确传播，当前不作总 PASS。 |

本轮结论是 `STORY_DECOMPOSITION_DRAFT / CAPACITY_NOT_COMPLETE / NOT_READY`。这些待闭合项是主管理接下来的合同工作，不代表用户还需要逐项重新授权技术拆分；若实际暴露未接受的架构/数据责任才返回对应决定门禁。不能把本草案交作 Writer prompt，不能据此独立 Review、实施、commit/merge/push。

#### F.3.5 剩余合同工作与保护态

首先固定 E18-S01 的时间元数据物理 schema、版本/映射和独立迁移 oracle，再按上述依赖逐项固定生产接口、交易前后条件、exact allowed paths 和 AC；同时完成全体 obligation 的唯一主要责任/消费者/residual 映射。遇到初检暴露的真实 owner/interface 缺口回 F5；否则继续 F6，完成后进入适用 F7/F8 及独立规划门禁，不能将“草案已落盘”升级为 READY。

E17 封口资产、04C HELD、旧候选不复用、MANUAL_RELAY 和所有保护态保持。仅更新当前 V2；原19520-byte V2历史后缀保持原身份，V1/历史输入/技能/模板/源码均不修改。本次将 E19 两模式独立接入的再次接受保存为决定，其他本节内容仍为主管理候选。

### F.4 E18-S01 时间信息持久化合同 DRAFT-1

本节完成用户“确认”后明确命名的下一步：时间字段、兼容规则与验证合同。中断前仅进行只读核对；用户随后要求继续，主管理在确认 V2 未变后写入本节。状态为 `CONTRACT_DRAFT_COMPLETE / NOT_REVIEWED / NOT_READY`。本节是主管理候选合同，不是代码实施、独立 Planning Review 或最终 readiness 结论；不由用户此前接受分组追溯接受全部物理细节。

#### F.4.1 身份、结果与边界

- 候选标签：`E18-S01`，本节版本 `DRAFT-1`。正文载体仅为当前 exact V2；交付时绑定其实际完整 hash，后续正文改变须重新绑定，不能只凭标签认定同一合同。
- Base：main=`d2c9ac48027177389092d56c208c64447a3c6a93`，tree=`c621b8385726a2b1b65097f337ab08134e4aed56`。本轮只读核对 Integration clean；未重复 fetch，未把旧远端证明冒称本轮 fresh。
- Accepted obligations：E18-R07/08/16、C.1、D.3；既有时间轴语义沿 E17-ARCH-05、现有存储兼容沿 CT-01。旧记录开始/结束原值不重写；数据库、时间来源、导出、分析版本独立。
- Observable old→new：当前 Room v5 的 session/entity/domain 没有冻结的当地开始日期、时区和偏移 → Room v6 能原样持久化和读回这四项新增属性，升级保留旧数据且旧值仍未知。
- 唯一责任：现有 session 持久化表示、迁移和直接映射。此节点不读取系统时钟/当前时区、不触发训练、不变更 canonical 分析/Recorder 准入、不实现日历/导出。真实开始采集仍归模式接入，真实结束写入归 E18-S04；本节点不冒称新训练已经生产这些信息。

#### F.4.2 物理字段与表示

现有 `workout_sessions.started_at`、`ended_at` 继续承载已有的已知开始/结束时间，不再新增一套同义 UTC 字段。`trusted_end_offset_ms` 继续承载可信相对终点；未知实际结束时间不能由该 offset 回填成 observed ended_at。

在 `workout_sessions` 增加且仅增加下列四列，均 nullable、默认 SQL NULL：

| SQLite 列 | 存储类型 | entity/domain 属性 | 含义与合法生产值 |
|---|---|---|---|
| `start_local_date` | TEXT | `startLocalDate: String?` | 真实 Start 时固定的 ISO 当地日期；后续日历使用该原值，不用当前时区重新求日期。 |
| `start_zone_id` | TEXT | `startZoneId: String?` | 真实 Start 使用的 ZoneId 标识原值，例如 Asia/Shanghai；允许平台合法的区域或固定偏移标识，不只接受一个地区列表。 |
| `start_utc_offset_seconds` | INTEGER | `startUtcOffsetSeconds: Long?` | 同一开始时刻的 UTC 偏移秒数；显式0代表 UTC，不代表未知。存储载体保留 SQLite 整数，不提前按小时/分钟舍入。 |
| `time_metadata_source_contract_version` | INTEGER | `timeMetadataSourceContractVersion: Long?` | 新采集合同为1；旧未采集为NULL。该值不是 Room version=6、exportContractVersion=2 或 analysisVersion=1。 |

沿现有 `WorkoutSessionEntity` 与 `WorkoutSession` 增加四个同名 nullable 属性，默认 null；不另建时间 repository、manager 或第二存储表。这里是持久化载体，导出时才按 D.3 归入 `session.timeMetadata`，并把内部 `timeMetadataSourceContractVersion` 映射为导出 `sourceContractVersion`。Kotlin Long 表示存储整数字段，不改变 JSON 的整数语义，也不将较小的合法偏移约束扩大为产品值域。

有效的新生产记录由模式 Start 从同一次开始锚点取得完整四项；source version=1 时四项齐全，UTC偏移来自该开始时刻。旧未采集记录四项均NULL。该完整性/格式/支持版本的语义由后续写入与严格读取边界按同一合同检查，不能把不完整或未知版本静默当旧记录；本节点只承载原值，不增加第二套语义 validator、不用默认值修复数据。S03/S07 与 S08 的直接消费义务见 F.4.5。

`startTimestampBasis`、`endTimestampBasis`、`trustedEndAtUtc`、`trustedEndAtBasis` 是后续严格读取/导出的来源说明或派生值，不在本节点重复持久化。不得把导出对象的所有字段机械复制成数据库列。

#### F.4.3 迁移与现有读写规则

1. `TrainFlowDatabase` 从5增量升级到6，注册 `MIGRATION_5_6`，保留既有1→2→3→4→5链和1–5 schema文件。迁移只进行四条 ADD COLUMN，不重建/清空记录表、不回填时区/日期、不升级旧 timeline/analysis/plan snapshot 版本。
2. 对应的 SQL 形状固定为 `ALTER TABLE workout_sessions ADD COLUMN <列名> <TEXT或INTEGER> DEFAULT NULL`；列名、类型及加入顺序以 F.4.2 为准。entity 的 `ColumnInfo(defaultValue = "NULL")` 与实际迁移、新安装schema一致。此处是规划合同，主管理没有执行SQL。
3. 新安装与v5升级到v6后的 schema结构一致，包括四列的类型、NULL、默认值；原表/索引/外键语义保留。既有schema JSON不可改写伪造历史，新的6.json由未来实现使用既有 Room schema 导出流程生成并核验，不能手工猜 identity hash。
4. 当前 `WorkoutSession.toEntity()` 与 `WorkoutSessionWithRecords.toDomain()` 对四项直接逐字段保持；不读 `ZoneId.systemDefault()`、不从 `started_at` 字符串截日期、不把NULL变0/空串、不把 source version 转成当前数据库版本。这里不顺带重写旧 plan snapshot decoder；严格版本读取仍归 S08。
5. 既有 legacy update SQL未列出这四个新增列，更新既有普通记录时保留数据库里的四项原值；新增字段不是通过通用 upsert 修改既有开始事实的授权。既有 upsert 拒绝覆盖 canonical session 的 guard保持，失败事务不得先改 header 或删除/替换子记录。
6. canonical Start当前直接接收entity；增加字段不能绕开已有graph/tuple验证，后续 S03负责将完整开始元数据纳入一个原子开始批次。S01不提前启动该生产采集链，也不让普通legacy流程声称自己已完成新canonical记录。
7. 迁移失败沿现有 Room 异常传播，保留原始失败；不新增 destructive migration fallback、清库重建或吞错后继续打开数据库的分支。历史规范事实和原始UTF-8 JSON内容必须保持。

#### F.4.4 AC 与匹配证据合同

| AC | 条件与可观察结果 | 独立 oracle / 直接消费者 |
|---|---|---|
| T01 旧数据保全 | 含legacy三模式记录及合法canonical active/terminal图的v5库升级后，原列值、status、tuple、phase、raw sample、analysis及original binding、plan JSON内容保持；所有迁移旧行四个新增列均NULL。 | 实际 Room/SQLite v5→v6迁移 fixture；按主键比较已有逻辑行/JSON原文及各表数量，不用“打开成功”代替数据保全。消费者为既有记录/CS-04B及后续S03/S08。 |
| T02 安装与升级一致 | 新建v6与真实v5→v6升级有同一schema结构和四列默认/NULL规则；既有升级链仍可到达v6。 | 既有 MigrationTestHelper、Room新库和schema6验证；增加一个旧版本完整升级链检查，不复制或重跑所有无关功能。保留旧1–5 schema identity。 |
| T03 原值读回 | 对固定、合法的四项数据写入真实库，关闭并重新打开后，entity和当前repository读取映射保持相同值；含UTC偏移0、非整小时偏移以及NULL旧记录。 | 使用现有DAO/仓库生产入口与真实Room；独立预期常量，不由被测映射函数生成预期。值保持即可，不据此宣称模式真实采集时间已完成。 |
| T04 既有更新不擦除 | 既有普通记录更新不改四列；相同更新仍保存本来可更新的执行字段。canonical session经通用upsert的拒绝行为和事务回滚保持。 | repository真实DB测试；保存前后四列、header/子记录对比，复用当前canonical guard fixture。不新增“修改训练开始时间”产品入口。 |
| T05 不推算/不串版本 | 读取迁移旧记录时四项仍NULL；系统当前时区不参与存储映射；显式0偏移原样保留，时间来源1不变成数据库6/导出2。 | 固定fixture + 直接代码/映射检查；跨时区/跨午夜归日的用户界面仍在S09/E21验收，不以此替代。 |
| T06 范围与错误 | 只发生四列的增量承载及必要直接映射；无旧raw/analysis重算、无字段回填、无fallback清库或宽泛吞错。 | exact delta/source检查和T01/T02的真实迁移结果；不要求为此增加AVD、真实Band、全仓库性能测试或新的production test seam。 |

验证执行由未来获授权的Writer与独立Reviewer分别承担。本轮未运行这些测试。S01的所需层是已有Robolectric SDK35/MigrationTestHelper下的真实SQLite/Room，加对应源码与schema差异；它不需要设备HR、FGS或主观UI验收。既有测试入口直接读取已确认，具体工具链命令/环境与evidence输出身份在手工handoff前绑定既有setup，不在此猜测新环境或派生本地evidence路径。

#### F.4.5 Direct-consumer ledger

| 消费者 | 必须承接的义务 | 不得错误归给S01的结果 |
|---|---|---|
| E18-S03 原子开始 | 复用四列承载；在整个Start批次提交时写入完整冻结元数据；其真实写入边界遵守同一版本/完整性语义，不产生一半开始事实。 | 模式按钮/时钟采集及初始化receipt编排不是迁移测试能证明的。 |
| E18-S04 终结 | 结束/执行结果更新保留开始四项及原始开始锚点；保存实际观察结束，unknown actual end与derived endpoint分开。 | 不把可信offset或重启时刻写成实际结束时刻。 |
| E18-S07、E19-S01/S02 | 真正Start时固定开始Instant、当地日期、ZoneId与该时刻偏移；不是进页面、首次HR启用或结束时才采集；各自配置保留/退出流程不重新定义开始。 | 生产采时与三模式生命周期是各模式的证据。 |
| E18-S08 | 真实persisted boundary严格区分未采集、合法支持版本、不完整/非法、未知版本；共用约束定义，不容错变成NULL；原始值不回写。已有canonical/legacy计划与analysis验证不由时间映射替代。 | S01四项nullable载体不是完整terminal/legacy strict reader。 |
| E18-S09、E21选择 | 按已冻结当地日归组/过滤/按日删除；跨午夜归开始日，旧信息未知单独表达，不能用UTC字符串前10位冒充当地日。 | 不在本节点改写历史页面或新增日历。 |
| E21-S01编码、E22呈现 | 依D.3输出timeMetadata和来源/派生标记；UI使用同一记录事实。已知开始anchor + offset可生成有标记的派生时刻，不冒称该点另有实际墙钟观测。 | 不生成第二份持久化时间真值，不自行重算分析。 |

后续S03写入完整性和S08读边界必须明确复用同一语义定义/唯一validator责任，不能各自发明版本/NULL解释；若现有validator适配涉及未接受owner变更，返回F5。此项是已接受数据合同的下游闭合工作，不用让S01扩张为全部开始、读、导出和UI实现。

#### F.4.6 未来实现范围定位（当前主管理无代码写授权）

本节点计划涉及的生产路径只有：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\TrainFlowDatabase.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\entity\WorkoutSessionEntity.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\model\WorkoutSession.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionRepository.kt`，仅四字段直接映射，不改finalizer/准入/通用解码策略。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\schemas\com.liujyks.trainflow.core.database.TrainFlowDatabase\6.json`，未来由既有Room流程生成；本轮未创建该文件。

复用的验证源码路径：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\database\TrainFlowDatabaseMigrationTest.kt`，追加v5→v6和完整升级链直接用例。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\database\CanonicalSchemaMigrationTest.kt`，仅对fresh-current-schema与旧固定版本测试的必要兼容调整及数据保全用例；不删除旧4→5证明。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionRepositoryTest.kt`，四项映射/重开/更新保持/已有canonical拒写相关用例。

这些是Integration base定位，不授权Writer在受保护Integration直接实施。未来正式根模板必须绑定用户授权的实际Story worktree以及其完整literal路径、最新适用base和验证artifact；当前没有创建任何branch/worktree或派发角色。不得为了绕开该门禁使用此处路径修改main。

#### F.4.7 退出记录

本局部步骤完成：四列及存储类型、版本独立性、迁移方法、直接读写保持规则、六条AC与独立oracle、未来生产范围及下游责任已经写成可审阅合同草案。新源码/测试/schema均未实现，所列运行证据均未执行。

容量结论仍不是PASS：本节点的单一结果/存储责任/迁移边界明确，且不消费未来模式实现才能证明自己的存储结果；但是全体Story的细粒度coverage、准确fixture/evidence身份与正式独立判定尚未闭合，当前draft不解锁实施。其他节点的语义validator接合、actual end与请求identity以及完整接口合同继续按其责任推进，不回塞S01。

下一主管理步骤：闭合E18-S02的无损观测输入/输出、receipt和绑定/解绑合同，使S03/S05/S06消费同一事实与错误语义；继续保持同一Application BLE owner和既有恢复策略。没有新的产品偏好问题要求用户重复决定，也不触发Writer/Review handoff。

### F.5 E18-S02 无损规范化观测与绑定合同 DRAFT-1

本节承接用户“进入下一步”，仅细化已接受的CT-09/D5无损观测与唯一runtime owner责任。状态为 `CONTRACT_DRAFT_COMPLETE / NOT_REVIEWED / NOT_READY`；不实施代码，不恢复04C候选，不形成独立Code Review或新的运行PASS。

#### F.5.1 来源与本轮直接事实

Base仍为main=`d2c9ac48027177389092d56c208c64447a3c6a93`；本轮Integration clean，未再次fetch。方法技能exact身份仍为 `C:\Users\25073\.codex\skills\bmad-method\SKILL.md`，SHA256=`0815CFD0E0414178ADCE676FAF4BFF18490CCA9A91B8F96EF1FDF954D98DF497`。

义务源：D5 §4.1/§5.1–5.2，文件身份仍为 `D2908408FC620BBC69CF3B4884286F0FB2B4AB07361E626A73BC34AF6CBDA46A`；V1的CT-09、E17-ARCH-18/19及V2的E18-R09/10。规范化状态/原因含义直接承接salvage §14.23–14.24及较晚main技术合同，旧阶段编号不构成派发顺序。

本轮只读证据：

1. `HeartRateRuntimeOwner.publish` 只在公开state不相等时更新StateFlow；这个显示通道不证明每次有效测量均有记录输入。
2. `handleCharacteristicChanged` 在解析前调用 `ActiveAttempt.isDuplicateCallback`，以同characteristic、同elapsed毫秒、同payload字节提前返回。因此同一合法入口连续收到两个相同值、同一毫秒处理的有效通知时，当前实现可能过滤第二个。这里只证明代码条件，不声称已观察到真实Band丢点。
3. 当前两个回调overload均转交同一处理函数；API33路径既有测试人工调用新/旧overload并检查freshness不重复刷新。该测试不证明平台每个notify实际会调用两个overload，也不能替代每次有效测量的计数oracle。
4. 官方固定版本Android12的BluetoothGatt.onNotify调用旧两参数overload；Android13、15调用带value的新overload。Android13的BluetoothGattCallback新overload默认实现才转调旧overload；项目override新overload且不调用super，就不会由该默认实现再转调旧入口。精确来源和hash见F.5.8。此为framework可行性证据，不是所有OEM/真实设备行为已验证。

#### F.5.2 唯一入口与观测载荷

唯一producer仍为Application里的 `HeartRateRuntimeOwner`，沿其现有main Handler串行边界处理。保留现有GATT身份、attempt/generation、characteristic、phase、permission检查和payload解析；只有通过这些真实入口检查的有效measurement才属于本合同的样本保证。旧/错误GATT、错误characteristic、不可接受phase或malformed payload不能变成正常样本。

版本入口采用本草案的确定性规则：API33及以上只让带value的新overload进入测量处理；旧overload在该范围不重复派发。API26–32使用旧overload并立即复制其value；新overload不作为旧平台的第二入口。两条路径都在转交main前复制字节，不调用super形成第二次分发。不再用payload/BPM相同或处理毫秒相同判断“同一次notify”。这是为满足既有无损要求而细化现有入口，不新增BLE owner、不调整恢复/扫描/超时策略，也不是采用04C代码。

**保证范围是runtime实际接收并接受的有效测量，不是设备未发送、无线链路未送达或平台未回调的数据。** 每一次这样的measurement均单独产生 `ValidMeasurement(bpm)`；连续两次88 BPM或同毫秒两次相同字节仍是两个观测。原有UI StateFlow可以继续按显示状态去重，不能作为记录入口。

观测的语义形状固定为：

- 内存信封：opaque `bindingId`、checked Long `receipt`、main串行处理时刻的单调毫秒坐标、一个typed payload。单调坐标描述owner接受该事实的时间，不宣称是设备传感器的采样墙钟。
- payload仅三类：`CurrentSnapshot(cause)`、`RuntimeTransition(cause)`、`ValidMeasurement(bpm)`。cause是可无歧义投影的稳定语义事实；不是公开UI文案，也不携带SourceHint中的地址/identifier、GATT对象、attempt/generation、原始SDK阶段或异常文本。
- `CurrentSnapshot`仅描述绑定时当前状态/原因。即使此时胶囊正显示live BPM，也不能把该旧显示值复制成一条新测量或回填训练开始之前的样本。
- 首个有效measurement使状态从waiting/stale等变为live时，先发布必要的state transition，再发布该measurement；两者可同一毫秒，用不同receipt保持先后。已是相同live状态/原因时不必制造额外transition，但每个有效measurement仍必须发出。
- 相同规范化state/reason的重复发布允许不产生新的interval变更；**状态去重与measurement去重分开**。receipt是观测交接顺序，不能直接冒充canonical mutation sequence；Recorder负责phase/intent/设备/样本合流与实际mutation。
- 无绑定时只保留现有runtime当前状态，不积累待补发的测量历史；进程死亡不回放。信封中的binding和绝对单调时刻仅用于进程内交接，Room/export仍只保存已接受canonical字段，不扩展sample schema。

#### F.5.3 绑定、查询与解绑

这些操作在同一main-looper cut同步完成；调用者通过既有coroutine/main调度到达该边界。runtime不创建新线程/总线/注册表，也不在bind方法内部启动随后才安装sink的悬空任务。

| 操作与条件 | 确定结果 / 不变量 | 下游责任 |
|---|---|---|
| 首次绑定且无active sink | 消费由repository准入预留的opaque bindingId，安装唯一sink；保存不可变的单调anchor和 `CurrentSnapshot(receipt=0)`，作为同一bind结果返回。 | S05唯一铸造/授权binding identity；S06预先创建同一个本场sink。S02不要求S05已经实现才可独立验证runtime API，但不自己生成仓库owner token。 |
| 同一binding的原请求重试 | 返回同一个已安装binding、原anchor、原snapshot0，不换sink、不重置receipt、不再次向sink插入snapshot0。 | 原请求复用原Recorder；不能用重试偷偷接管另一个consumer。 |
| 已有另一个binding | 返回明确conflict，保留原binding和sink，不覆盖、不解绑、不广播给两个consumer。 | S05保持不放行；不是“自动替换旧训练”。 |
| bind结果交付丢失/取消后查询 | 在同一串行边界查询exact reserved bindingId，区分KNOWN_ABSENT、MATCHING_INSTALLED、CONFLICTING_INSTALLED；无法取得该确定答案时保持UNRESOLVED。MATCHING返回可恢复的原bind结果。 | S05按D5保留原始错误，并在明确不存在或exact unbind成功后才能释放；不能把查询失败猜成不存在。 |
| 查询声明不存在 | 已在该串行边界确定没有matching sink，且此前这次bind的安装操作已经完成或被取消而不会随后执行。不能在一个尚未执行的bind前查询后就宣称永远不存在。 | 标准main同步安装/调用顺序提供边界；不引入延迟安装任务或跨线程任意抢跑。 |
| exact解绑当前binding | 同一main cut撤下matching sink；返回后不再向该binding发出新观测。BLE连接、前台恢复及UI继续依既有策略工作。 | 已交给S06队列的观测不是被撤回；S06负责terminal cut与晚到写入拒绝。解绑记录sink不等于断开设备。 |
| 解绑时无matching binding / 存在他人binding | 返回明确absent或conflict disposition，不影响其他binding。 | 旧token/重复释放能否改变仓库状态由S05判断；runtime不建立第二套released-result或准入authority。 |

绑定之后每个后续观测的receipt从1开始严格checked递增；同毫秒可以有多个receipt。初始化结果0经bind返回，后续1…N可能先进入Recorder的初始化缓冲；S06必须把原snapshot0置于冻结批次最前，并在S03的一次Start事务消费该cut。重试/查询不重播后续测量，runtime不保留第二份日志。

sink是内部非阻塞交接：在main cut接收immutable观测并由Recorder排队，不能同步做Room/文件IO、推进engine或重入runtime控制。S02不使用conflated/采样/丢弃型Flow替代交接，不把队列溢出或receipt溢出伪装成成功。内部正常交接合同被信任；不为任意恶意sink新增wrapper或模拟异常产品流程。实际记录失败由S06保持原始原因、明确终止后续canonical写入；不能反向伪造设备失败或让runtime替Recorder吞掉错误。记录已明确失败后的行为不能继续宣称“全部落库”。

#### F.5.4 规范化cause与隐私边界

唯一 `CanonicalHeartRateObservationMapper` 负责runtime语义cause到既有12-state/14-reason pair。完整允许矩阵直接引用同一main的 `AcquisitionV1Validator.DEVICE_REASON_MATRIX`（位于CanonicalSessionValidators.kt，私有矩阵，仅作为源码合同引用）及salvage §14.23–14.24；不复制成另一套DAO/Route/export mapper，不增加vocabulary，不重解释既有持久化行。

cause必须在有真实上下文的producer分支产生，至少保留：

- 初始采集与自动恢复，permission首次缺失与已授予后撤销，首样本等待期限与已有样本失效期限。
- 无选择source、搜索窗口未取得source、意外断联及仍armed等待恢复的区别；source_unavailable不等同已证明超出距离。
- 平台采集能力不可用与用户关闭蓝牙；不能建立标准测量流的能力失败与真实platform failure。不能从异常文案或SDK阶段字符串猜reason。
- 主动停用/手动断开与非训练后台或无合法FGS导致的采集停止。后者不能因为公开UI也是IntentionalStop就变成用户排除；expected intent下不使用not_observing掩盖缺口。状态足够表达且没有准确的既有稳定reason时允许NULL，不能滥用source_unavailable或platform_failure补一个原因。

具体字面意义、NULL及pair是否合法以引用合同为准；`connection_timeout`仍是schema合法但当前production不可达，禁止为填满矩阵新增watchdog。单次malformed payload既不产生ValidMeasurement，也不刷新freshness，不单独造technical_failure；若之后既有freshness policy真的发生transition，则记录该真实transition。

规范化信息不决定recording intent、phase、eligible分母或covered统计。尤其user_excluded的三个原因由实际用户意图路径进入Recorder；设备不可用/平台停止不能自行变成opt-out。S06将控制意图与设备观测按同一canonical顺序接合，S03沿已有validator验证interval，不接受相互矛盾的组合或事后修复。只有真实cause与既有分类无法对应、需要新state/reason或改变mapping含义时才回到版本/架构门禁，不由Writer默认归technical_failure。

#### F.5.5 AC 与证据

| AC | 条件与可观察结果 | Matching oracle / 不得冒称的范围 |
|---|---|---|
| O01 每次有效measurement独立 | SDK35的新入口与SDK32的旧入口分别在同一elapsed毫秒接收两次相同合法payload，sink收到两个ValidMeasurement和连续receipt；不同值、16-bit有效payload亦保留解析事实。 | 现有Robolectric真实production owner/回调、标准parser和公开新增绑定边界；预期数与BPM序列独立给出。不用StateFlow最后一个值证明计数，不以该测试冒称真实Band每个物理样本都送达。 |
| O02 版本入口与字节快照 | API33+新入口一次处理且不会通过super转旧入口重复处理；旧入口不重复派发；API26–32旧入口可用。调用后改变原数组/characteristic值不能改变已捕获观测。 | SDK32/33/35可用现有Robolectric配置与官方source相互印证；更新既有overload测试的事实解释，保持一次合法通知不双计；无需新AVD或安装SDK。 |
| O03 既有身份/phase门禁 | 错误或迟到的GATT/attempt/generation、错误characteristic及不可接受phase不进入新sink；合法同值通知不能被当作这类重复协议回调。 | 复用现有callback identity/phase负向fixture及新的sink计数；不新增第二GATT，也不削弱现有过滤。 |
| O04 snapshot0与不回填 | 无绑定期间的live数据不累计；绑定返回原anchor和snapshot0，snapshot不会变成新样本；第一次后续有效notify才计样本。 | main串行生产调用与独立sink ledger；在绑定前后各发已知观测，检查无回放，重复bind/query不重置或重复snapshot。 |
| O05 同一cut与顺序 | 绑定/状态变化/measurement/解绑均按main边界顺序；同毫秒的state transition先于触发它的measurement，重复live显示仍不丢measurement。 | sink ledger逐项比较receipt/单调坐标/payload；源码确认未从conflated显示流采集、不运行IO、不添加采样或丢弃策略。S03/S06另证明canonical事务/排序。 |
| O06 单绑定和exact清理 | 同一bind可重试/查询，另一个bind不抢占；exact unbind后无新交付，错误binding操作不影响当前consumer。 | 直接production API、两个独立测试sink及明确的返回disposition；真实result-delivery取消/丢失相关组合在S05/S06与coroutine边界进一步验证，不能用source注释代替仓库准入证明。 |
| O07 状态/原因准确且无诊断泄漏 | 已有cause分支覆盖既有规范化矩阵；permission/初始恢复/first-later stale/平台与用户停止不混淆；malformed不新增样本/故障，connection_timeout不被生产合成。 | 纯mapper固定预期 + 可执行runtime分支；沿现有validator验证pair，不从被测mapper生成预期。载荷不包含设备标识、GATT、attempt/generation或异常文本。schema允许但production不可达分支只做合法性区分，不造运行路径。 |
| O08 消费者分界与既有行为 | 解绑观测只停止本场记录交接，不关闭BLE或重启训练；既有前台恢复、freshness、设置/胶囊状态不因新通道换owner或改阈值。 | 直接受影响的runtime/平台/恢复回归与source delta；最终可执行源码改变后的设备证据需按后续合同重绑，不沿用旧APK身份宣布新实机PASS。 |

没有运行上述测试、build或设备动作。SDK分支源码证明不替代项目接入和设备证据；S02为观测生产边界，Room/terminal/Android Activity生命周期/FGS分别由S03–S07和E20承担。

#### F.5.6 范围与消费者

计划生产边界沿既有位置，仅包括 `HeartRateBoundary.kt` 中的观测/绑定typed合同、`HeartRateRuntimeOwner.kt` 中单slot/观测发出/版本入口，以及C节已定位的 `CanonicalHeartRateObservationMapper.kt` 单一规范化责任。完整base路径：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateBoundary.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwner.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\CanonicalHeartRateObservationMapper.kt`（拟新增源文件，未读取或采用04C候选版本，当前没有创建）。

验证定位沿已读的 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerPlatformTest.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerCallbackIdentityTest.kt` 及已有runtime/recovery测试。独立observation/mapper测试的exact新增文件及evidence artifact应在正式合同/handoff绑定，不按候选ID自动派生路径。

S03消费snapshot0与后续receipt cut；S05供应bindingId并消费四类disposition与exact unbind结果；S06是唯一实际记录sink和canonical顺序owner。S02的方法可先以固定opaque identity和测试consumer独立验证其真实边界，不导入尚未实现的Recorder/repository生命周期owner、不得形成S02↔S05循环依赖；这种测试不证明模式生产接线。

本节不增加schema/Room写入、连接watchdog、Service/通知、模式ViewModel、训练控制、UI、导出或新的依赖。callback入口修正必须留在现有owner内，不引入第三套兼容wrapper。旧“完全不触及callback入口”的过宽描述不能用于保留与无损要求相冲突的内容/毫秒过滤；这只是规划候选中的准确范围说明，当前仍无代码写授权。

#### F.5.7 局部退出与下一步

本节形成了可审阅的输入/输出、起始快照、receipt、bind/query/unbind、错误与隐私边界、八项AC及消费者合同。版本入口与每次测量计数已有固定官方source机制依据；没有运行/独立审查或capacity PASS。完整production cause→normalized pair逐分支矩阵与新增测试artifact精确身份仍需在最终合同覆盖表闭合，不能仅凭允许pair表声称全部production分支已证明。

下一主管理步骤为E18-S03：冻结初始化批次与原子开始/活动持久化合同，消费本节同一snapshot/receipt语义，并把S01时间元数据纳入同一开始事务。binding失败、result-loss与释放的全矩阵仍按D5归S05/S06继续闭合；不把本节source可行性当作这些门禁已经通过。

#### F.5.8 官方版本来源（只读内存检索，未安装或复制进仓库）

| Exact URL | UTF-8源码字节 / SHA256 | 本次证明范围 |
|---|---|---|
| https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/tags/android-13.0.0_r1/framework/java/android/bluetooth/BluetoothGattCallback.java?format=TEXT | 12014 / `E3C7C5FDA8F5424A9F91A65ABB8AC41AA6A3E095DF8D023F054F719725AD09EE` | 新overload默认实现转调旧overload |
| https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/tags/android-13.0.0_r1/framework/java/android/bluetooth/BluetoothGatt.java?format=TEXT | 79771 / `279CE9B23D99E3A8034013877BA0A57027C4B343FE6216D400CFD737C76AE475` | onNotify直接调用带value的新overload |
| https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java?format=TEXT | 66953 / `06008453F83A75DA00F2598EF4CBBC3847CECDB8900A6AC4AE46DC2FCCC5867B` | onNotify调用旧overload |
| https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/tags/android-15.0.0_r1/framework/java/android/bluetooth/BluetoothGatt.java?format=TEXT | 88100 / `95FEDBDEF38C3D5F43C7AF623C08E2931BA47B304568F4E726A33703E5CBD2C6` | SDK35对应官方版本仍调用新overload |

以上hash针对base64解码后的源码字节；仅阅读有关派发段落，不声称独立审查整份Android源码或证明全部OEM实现。其他项目source仍绑定本节开头的immutable main；本次未fetch项目远端、修改源码、执行测试、使用子代理或派发角色。

### F.6 E18-S03 冻结初始化批次与原子开始/活动事务 DRAFT-1

来源：本文件 F.3 的 S03、F.4/F.5，D5 §4.2/§5.2–5.3（身份见历史 §6），同一 immutable main 技术合同 §4.2.1–4.3、§5 及现有 `WorkoutSessionRepository` / 两个 DAO / `CanonicalSessionGraphV1Validator`。本轮直接核对 `startCanonicalSession` 当前只插入 session/initialPhase；各 append/transition 方法分别做 expected-state 检查、guarded 写和同事务验证。现有 guarded-write 测试使用真实 Room，已有 SQL trigger 证明 guard rowCount 0/2 后整体回滚；只读确认此能力，没有运行测试。

#### F.6.1 输入与唯一事务责任

old→new：独立的 session 开始和 HR 初始化写入入口 → 一次不可拆开的 Start 提交包含 session、首 phase、适用 recording/acquisition 及冻结初始化观测的全部结果。S03 只负责现有仓库的 durable transaction；不 bind BLE、不分配仓库 owner token、不创建 Recorder、不驱动训练。

冻结请求包含：原 session identity、当时的 plan/display 快照、S01 时间来源四项及真实 started_at、首 phase、初始 intent、可选 recording identity/当场冻结参数、S02 原 anchor/snapshot0，以及连续 receipt 1…N 的不可变初始化批次。未依赖 runtime 的确定性输入先验证；依赖 snapshot/mapper 的合法性在该快照可用后验证。请求重试复用原值，不重新采时、读取当前计划/参数、换 identity 或把新到通知并入旧请求。

- Session/首 phase 从 canonical tuple `(0,0)` 建立；开始时已选择记录心率，recording/初始 acquisition 同次事务从 `(0,0)` 建立，即使设备尚未 live。没选择时，不造 recording/acquisition/sample/snapshot；保留计时阶段和 session。无 recording 时最新设备事实的内存保存归 S06，不在此另建存储。
- snapshot0 是初始化状态，不产样本。初始化 receipt 按原顺序折叠；每个实际 canonical mutation 使用 checked sequence，receipt 与 mutation sequence 不要求数值相等。规范化重复状态可 no-op，已接受的 measurement 在适用 recording window 内每条独立入库。同毫秒不同 mutation 合法，phase/acquisition/sample 的独立 sequence 不互相冒充。
- >N 输入只能在 commit 成功后作为活动输入处理。Start 返回成功的唯一条件是该冻结批次的同一次 Room transaction 已提交；禁止 commit 后再“初始化 drain”并把其失败改称开始回滚。
- 一次活动命令若同时改变 phase、session active/paused、intent/device interval 或 append-only metadata，所有必要行及 header durable tuple 一起提交。沿现有 DAO guard 扩展真正缺少的字段更新，不能拆成可观察的中间状态；不通过通用 legacy upsert 绕开 canonical guard。
- 数据库边界验证沿现有版本、closed JSON、pair、phase/acquisition partition 和 expected tuple。session id/status/durable tuple/open row id/recording id 与预期必须一致；header、close/open/insert 的 guard 不弱化。mutation offset 不倒退，checked sequence 不复用；禁止 clamp/default/修补非法行。新版时间元数据全套有效且与该开始来源一致，旧 NULL 不在此批量回填。
- mapper/candidate validation/DAO/guard/写后 validation 任一步在 commit 前失败，所有本事务写入回滚，保留原错误和最后 confirmed tuple。完整图的枚举和排序沿既有 DAO/validator；没有新 digest 列、第二 validator 或另一个 owner。

#### F.6.2 提交结果不确定与活动失败

同一 Start 的 commit-result 丢失只能以冻结请求确定产生的**完整持久化 row graph**识别：session全部合同字段、时间/快照原文、phase、recording、acquisition、sample及应为空的 snapshot/执行子记录。比较稳定键和合同顺序，不依赖查询自然顺序；缺行、多行、任何值冲突均不能认成功。完全不存在且事务已结束/确认回滚才是 rolled-back；无法确认则 unresolved。不得以仅存在 sessionId、最终 tuple 相同或记录条数相同代替完整比较。确定提交成功后不重复插入，清理 disposition 归 S05；同一请求尚未确定前 S06 不放行 >N，所以不能把“已经有后续活动写入”猜成这个初始化图。

普通活动写入的首次失败：S03 返回原错误和未推进的确认边界，S06 进入 `ACTIVE_PERSISTENCE_FAILED`，之后不继续 canonical 写入、不伪造设备故障/用户排除/成功终态，不改变 engine；S05 仍保留 owner。活动 commit-result 不确定同样不能凭内存判回滚或推进，保持失败/未确定并禁止新写；不新增活动通用重放日志。fresh-process 的 durable reconciliation 沿 CS-04B，当前进程不能借用 process_interrupted 解除阻塞。

#### F.6.3 AC、直接消费者与路径

| AC | 条件 → 可观察结果 | oracle / 消费者 |
|---|---|---|
| B01 冻结 Start 全提交 | 无 HR、已开 HR 但 zero-sample、含多个初始化 receipt 三类请求 → 一次提交完整合法图和时间值；snapshot 不造点，后续 receipt 不混进批次。 | production repository + 真实 Room 独立预期行；S06/S07 |
| B02 Start 全回滚 | 在候选验证、真实插入/guard、末次图验证的可达持久化边界失败 → 所有 start 行均无残留，原错误未替换。 | 真实 Room/已有 SQL trigger 方法；预写后写逐表对照，不新增 production 故障钩子；S05 |
| B03 exact 结果识别 | 原批次已提交但调用者未得成功 → 完整相同图认原成功；单字段/样本/时间/多余行冲突不得成功；未完成事务不报 absent。 | 独立持久化 fixtures + coroutine 调用结果交付边界；不以重复返回 mock 成功证明；S05/S06 |
| B04 活动原子切换 | pause/resume、phase/intent/device、extra-rest metadata 的必要组合 → header 与相邻 interval 在同一 cut 更新，partition连续，零时长合法。 | 固定同毫秒前后 row graph；现有 validator；S06/各 mode |
| B05 guard 与错误 | stale expected tuple/open-row、真实约束/guard失败 → 本项不留 partial write、不改此前 confirmed graph，原始错误返回。 | 既有 rowCount0/2 和相邻 consumer fixtures；S06 |
| B06 无损与首次启用 | 合法同毫秒同值两测量都保留；late enable 只从真实命令 cut 建立唯一 recording；off/on 不换 identity。 | 固定 mutation/sample序列；写边界拒绝第二 recording；意图和时间来源另由S06证明 |
| B07 版本与时间 | 新请求版本/四项时间及快照满足来源合同；结束/活动更新不能覆盖已冻结 start；无 HR 不制造 HR graph。 | 真实 Room round-trip + 版本/来源错误输入；S01/S04/S08 |
| B08 活动失败分界 | repository错误/提交结果不确定 → 不冒认下一 durable tuple、不触发 engine 或 runtime 操作。 | 真实仓库结果和source delta；S06另证停止后续写入和原错误持续可见 |

计划变更定位仅现有下列生产路径，formal allowed envelope仍需按届时 accepted base 绑定：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionRepository.kt`
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\WorkoutSessionDao.kt`
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\database\dao\CanonicalTimelineHeartRateDao.kt`

已有验证位置：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionRecorderGuardedWriteTest.kt`。S03 不改 CS-03 validator/schema、CS-05 分析、观测 owner、模式/UI/通知/导出。若新 typed 请求确需另立源文件，formal contract 先明确 literal path，不能由 Writer从 Story 名派生。状态：`DRAFT_1_TRANSACTION_BOUNDARIES_DEFINED / NOT_REVIEWED / NOT_READY`；完整 subclause coverage 与 future prerequisites 尚未完成。

### F.7 E18-S04 可信终结与执行结果持久化接合 DRAFT-1

来源：F.3/S04、C 的同仓库外层事务决定、D5 §4.3/§5.5–5.8、main §4.2.2–4.2.3及既有 CS-05 `finalizeRecordingSession`。本轮源码证明当前方法严格要求 active graph，在一次 Room transaction 关闭 phase/acquisition、terminalize session/recording、派生并插入 analysis v1、绑定 original version 后重新验证；没有完整的 terminal-request 幂等识别或 actual ended_at/执行子记录写入。它是被复用资产，不是待重做算法。

#### F.7.1 终结请求与事务分支

同一 session 的终结请求冻结 predecessor expected tuple、finalOffset、terminal status/reason、真实已知 ended_at、执行结果/append-only metadata最终值及适用 recording identity。S01 开始信息原样保留。request semantic identity沿D5：session/recording/status/reason/finalOffset/predecessor及derived final sequence；实际结束时间和执行结果是该请求不可替换的 payload。同一 semantic identity 但这些 payload 不同是冲突，不能在重试时覆盖。snapshotCreatedAt 仍为首次写入 metadata，不作为更换分析/重试身份的理由。

| 分支 | 同一外层 Room 事务内动作 | 不变量 |
|---|---|---|
| recording-backed，exact active graph | 先验证原输入和冻结执行结果；写适用实际结束/执行字段，调用已有CS-05终结；在外层commit前验证完整terminal graph、执行子记录及 original binding。具体SQL先后须维持既有CS-05前置条件。 | 外层失败回滚包括CS-05所有行；不另算分析，不复制finalizer，不多建snapshot。 |
| no-recording，exact active graph | expected status/tuple/open phase guard；一次关闭phase、写terminal header/真实已知结束与执行事实并完整readback。 | completed/completed；abandoned/user_abandoned或owner_cleared；没有 recording/acquisition/sample/snapshot/binding。 |
| exact matching terminal graph | 对比冻结终结身份和完整请求payload，验证持久化graph/原始分析binding/执行记录后返回原成功。 | 不写新 ended_at、不补当前计划、不换snapshot；readback未确定不能称read-ready。 |
| conflicting/missing/invalid graph | 返回typed冲突或原始持久化/validation错误。 | 不降级legacy、不绕guard upsert、不把冲突当幂等成功。 |
| fresh-process遗留 | CS-04A/04B按最后durable offset调用适用已有分支。 | S04不在当前进程调用process_interrupted；实际wall end未知保持未知，不用重启时间冒充结束时间。 |

正常 terminal cut 后不再追加样本/阶段掩盖终结失败；仅保留exact同意图重试。终结失败不对用户声称已保存。事务成功后何时 invalidate cache、unbind 和释放唯一owner由S05负责；durable成功与RELEASED/read-ready是不同结果，清理失败不得重做分析。记录失败后保留 confirmed durable图的规则沿D5，S04不制造用于“成功结束”的假行。

#### F.7.2 AC、路径与消费者

| AC | 条件 → 可观察结果 | oracle / 消费者 |
|---|---|---|
| T01 recording整体终结 | 已验证active graph + 冻结终结请求 → 执行事实/实际结束、closed graph、唯一original analysis同次提交。 | production外层repository+真实Room；直接读取所有相关表，沿CS-05 oracle；S06/S08 |
| T02 no-HR终结 | completed/user_abandoned/owner_cleared、含zero-duration phase → 合法terminal且HR/snapshot表无本场行。 | 独立no-HR三类fixture，既有validator；S06/S08 |
| T03 嵌套回滚 | 执行写、CS-05 guard/insert/binding、末次readback失败 → 整个外层恢复pre-terminal图，原错误保留。 | 实际Room事务与现有测试SQL trigger方法；框架源码仅证明机制，不代替此oracle |
| T04 同请求重试 | result-loss或清理失败后再调同请求 → 原结果，唯一snapshot，所有首次时间和执行值不变。 | 真实persisted graph比对；不同ended_at/执行字段/tuple/reason故意冲突，均不得改历史 |
| T05 晚到与竞争 | terminal后append、错误predecessor或不同终结意图 → 拒绝且完整terminal图不变。 | 生产仓库并发 + 真实Room；S05/S06分别证owner竞争 |
| T06 原分析与时间来源 | 改当前计划/参数、晚重试或process restart → 不重算original，不填未知wall end、不擦除冻结开始元数据。 | 独立原始绑定/时间 fixture + 既有fresh-process资产回归；S08/导出 |

生产位置沿F.6列明的三个现有文件；终结原算法/语义validator保持复用。验证沿现有 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionRecorderReconciliationTest.kt` 及 guarded-write 测试，正式合同仍须精确绑定新增fixture归属/evidence literal paths。没有修改或运行它们。S04不接管S05释放、S06终结意图冻结、模式实际值采集、S08严格历史/legacy读取或E22呈现。

状态：`DRAFT_1_TERMINAL_BOUNDARIES_DEFINED / NOT_REVIEWED / NOT_READY`。S01 future prerequisite尚未实现；这里的拟定合同和官方nested-transaction机制不等于集成或运行PASS。

### F.8 E18-S05 唯一仓库准入/释放 DRAFT-1

来源：C、F.3/S05，D5完整§5.1–5.8/§6.1及F.5–F.7接口。已有repository的 `prepareRecorder()` 在进入mutex前直接返回 `completedRecorderGate`；现有canonical写入口会重新调用prepare。因此S05必须同时改正cache前检查和已准入写消费者，不能只加一个busy字段而让自己的活动写入被prepare拒绝，也不能保留另一条无token canonical入口绕过准入。

#### F.8.1 一个状态authority和两个调用层

现有 `WorkoutSessionRepository` 唯一拥有进程内 `OPEN / ACTIVE_OWNER / OWNER_CLEAR_PENDING / OWNER_BLOCKED` 状态、entryId/ownerToken/bindingId及匹配清理身份，语义完整继承D5。runtime只消费binding；ViewModel/Recorder不各建一套仓库准入真值。ordinary prepare/admit在读取cached success之前以及实际准入线性化点核对OPEN；一个entry赢得原子准入，重复同entry只取得原结果，不同entry busy，不抢占。

S03/S04已准入事务的入口消费exact owner授权，不能走“新训练prepare”路径。仓库区分新准入、matching owner的活动操作、matching pending的已冻结Start/terminal收尾及fresh-process reconciliation；blocked不允许新canonical活动写，只有同token的确定性核对/清理，不能凭token解除已发生的持久化失败。旧通用upsert仍拒绝canonical。所有直接canonical consumer及tests迁移到唯一授权入口；不对生产保留一个供测试调用的无token旁路。

`beginOwnerClearHandoff`为非挂起、线性化状态转换：返回前matching ACTIVE→PENDING已成立。具体同步原语留给Writer在既有repository单状态实现；不能拿挂起Mutex/launch的未来任务冒充同步barrier，也不能在main阻塞等待Room。其他gate/cache操作必须在发布结果前再核对同一状态/owner，不能被已完成cache、正在scan的旧结果或晚到callback反向打开。reconciliation结果属于扫描缓存，不是session所有权；实际新Start总需独占准入。

#### F.8.2 全部释放/阻塞分支与顺序

下表是D5各分支的新primary disposition，不重启旧Repair。`ACTIVE/PENDING`均指exact matching token，失败cause保留最早原始错误，清理错误只作secondary；没有先前错误时首次清理错误为primary。

| 分支ID / 真实边界 | 持久化与binding证据 | 最终状态与cache处理 |
|---|---|---|
| G01 准入前验证失败/另entry竞争 | 未准入；原owner、Room、binding不变 | 不改状态/cache，不执行清理 |
| G02 准入后bind known absent | 确定bind不会随后安装；未运行Start，无本场行 | 保留cache，exact ACTIVE/PENDING→OPEN；不请求terminal/unbind |
| G03 matching installed，含bind结果丢失 | 查到原binding；未运行Start，无本场行；丢弃未提交buffer；exact unbind成功 | 保留cache，再exact ACTIVE/PENDING→OPEN |
| G04 bind后snapshot/receipt/mapper验证失败 | 与G03相同，但primary是原validation错误 | exact清理后保留cache并OPEN；不造owner_cleared终态 |
| G05 conflict/unresolved/bind清理或token释放失败 | 不解绑别人；不能证明安全不存在就不猜absent | OWNER_BLOCKED；禁止cache捷径和新Start；仅same-token确定性resolution/cleanup或进程终止结束该block |
| G06 Room Start确定全回滚 | 全部start行不存在，且原binding exact unbind成功 | 保留cache，再exact ACTIVE/PENDING→OPEN；旧entry/token stale，后续显式Start新准入 |
| G07 Start回滚/提交结果仍不确定 | F.6完整graph核对尚未得到确定答案，或清理失败 | OWNER_BLOCKED；既不发布Start成功，也不释放到OPEN |
| G08 Start成功、正常active | 原owner持续；配置重建复用同entry/Recorder；设备transition不释放 | ACTIVE；新entry busy；不得为每个sample重新prepare |
| G09 active首次持久化失败 | 本项回滚或commit disposition不确定，保留已确认事实/原错误 | owner仍占用，禁止后续canonical写；clear转PENDING后不能假terminal成功，按D5阻塞，fresh-process仅读durable事实 |
| G10 normal terminal成功 | F.7提交且全量readback→Recorder记原结果→仓库invalidate cache→exact unbind→exact ACTIVE/PENDING→OPEN | 仅顺序全部完成才RELEASED/read-ready；下次prepare重新scan；不再finalize |
| G11 terminal事务失败，owner仍在 | 无可信terminal结果；保留exact terminal intent/primary | ACTIVE，不开放准入；只允许原意图终结重试；若clear已成立则BLOCKED |
| G12 durable terminal成功，清理失败 | 保存原terminal成功供识别；不重复snapshot/执行写 | BLOCKED；retry仅完成未完成的exact invalidate/unbind/release，全部成功才OPEN |
| G13 completed/user_abandoned与clear竞争 | clear沿同token安装PENDING；保留已有terminal intent，成功走G10，失败走G11/12 | 不新发owner_cleared、不第二次终结 |
| G14 STARTING期间clear | 同步PENDING先成立；未进Room走G02–05；回滚走G06；若Start最终提交则收尾owner_cleared | 未确定/清理失败不放新Start；已提交才有可终结session |
| G15 ACTIVE期间clear | 同步PENDING→Recorder按原顺序冻结owner_cleared intent并收尾 | 成功走G10，否则BLOCKED；不从已取消viewModelScope启动收尾 |
| G16 released/stale/重复清理 | RELEASED ViewModel本地no-op；旧token到达仓库返回typed stale；不同binding不改 | 新owner/cache/binding原样；仓库不保留第二份released-result缓存 |
| G17 fresh process | 原token/binding/queue不复活；原CS-04B扫描durable graph | 仅新repository处理process_interrupted；legacy residual不是当前owner |

G02–G07中若clear已安装PENDING，改变的只是matching最终状态，不能因为有clear就给不存在session伪造terminal。成功terminal后的cache失效是受序列化保护的内存更新，不设计虚构IO失败；真实unbind/release不确定仍必须fail closed。

#### F.8.3 AC与证据边界

G01–G17每行同时作为本Story AC，执行oracle为真实repository生产调用、现有Room数据库、S02真实binding边界及独立预期state/row/cache结果。并发至少覆盖：双准入；clear先于Room；Start提交/回滚与clear；terminal与clear；cache命中与新Start；旧token晚到新owner；cleanup失败后的新Start。每个分支断言当下结果以及**后续prepare/start确实放行或拒绝且无意外写入**。仅验证private字段或mock成功返回不能证明准入安全。

测试编排只用测试端协调、真实coroutine调用结果交付、已有Room SQL trigger或实际生产边界可达失败；不能为暂停流程新增production hook/wrapper/owner。若届时某个claim无法在授权生产边界独立证明，记录具体证据缺口，不能靠注释、反射或更低层模拟宣布PASS。仓库测试不证明Android调用了onCleared；该消费者由S07/E19模式合同承担。

计划生产主路径是F.6中的 `WorkoutSessionRepository.kt`，仅为同一gate授权贯通而调整S03/S04调用合同；S02已提供的query/unbind接口直接消费，不反向增加runtime新owner。验证沿F.6/F.7已列两个现有测试路径；新测试/evidence exact路径在formal合同绑定。没有Activity/Service/通知/图表/导出/schema改动。本合同状态 `DRAFT_1_ALL_D5_GATE_BRANCHES_DISPOSITIONED / NOT_REVIEWED / NOT_READY`；分支被列明不等于证据执行或十维capacity通过。

### F.9 E18-S06 单场串行Recorder DRAFT-1

来源：C的已接受session-scoped owner、D5 §4–6、salvage §14.38及F.5–F.8。唯一拟新增生产位置为此前明确定位的 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionTimelineRecorder.kt`。这是本场输入排序和异步收尾owner，不新增engine/GATT/仓库authority。

#### F.9.1 一场一次初始化和有序输入

- 一个已接受entry/session创建一个Recorder；预先准备同一sink，完成S05准入，再通过S02绑定取得原anchor/snapshot0。确定性输入在准入前验证；绑定后的失败按F.8相应行处理，不留隐式第二个Start路径。
- Recorder冻结初始化snapshot0…N和原请求；>N只进入本场队列。S03 commit成功前不作为活动写入，不重新切N；结果不确定按完整graph恢复原结果，不能靠当前显示BPM重建。Start前无HR时只记最新设备事实于内存；初次开启时从真实命令cut创建recording，不回填之前测量。
- phase facts、用户记录意图、normalized设备事实、每个有效measurement、实际extra-rest/append-only metadata、终结进入同一serialized顺序。单调offset沿本场anchor，允许同毫秒多个输入；canonical mutation sequence checked递增，设备receipt仅证明其自身交接顺序。不得对事件重新按BPM/墙钟排序、clamp倒退或把不同轴sequence混用。
- runtime接收端只作非阻塞入队；不在main回调同步Room IO，不使用conflated/drop-oldest/sample节流，不新增另一个后台总线或可重播日志。真正已明确记录失败后不能继续声称所有点已保存。若后续实施/性能证据表明现有责任内无法保持无损，应返回真实可行性门禁，不能静默丢点。
- 没有recording时仅保留最近设备状态，不持续存raw；第一次enable使用该命令前最新事实。off/on保持同一recording与参数快照；user_excluded原因来自真实用户动作。pause仍保留适用raw/设备事实，eligible由已有分析phase规则确定，不由Recorder凭BPM推断。
- phase family/index、strength/follow实际事实由各mode producer给出；Recorder不猜engine阶段或读取当前计划修补。设备cause规范化唯一调用S02 mapper；严格事务和原始分析分别调用S03/S04，不重复实现。

#### F.9.2 失败、终结和退出

首次活动持久化/不变量失败停在 `ACTIVE_PERSISTENCE_FAILED`，保留原cause与confirmed tuple，之后canonical命令失败返回同cause，不驱动engine暂停、结束或回退，也不造成功保存/设备故障。训练行为和记录结果分别可观察；具体页面消费由S07/E19完成。

第一次正常terminal命令冻结意图、offset、已知wall end和执行结果；该cut后的sample/phase不得进入终结图。重复同请求复用本场结果，不同意图拒绝，不刷新结束时刻。terminal事务失败进入可诊断failed状态，仅exact请求可重试；durable成功后继续S05清理，全部完成才向模式发布RELEASED。

真实owner clear入口在返回前同步调用S05 barrier，随后由**Recorder自己持有、可完成收尾的工作生命周期**执行F.8矩阵；不得依赖已取消的viewModelScope。STARTING后提交才可owner_cleared终结，rollback则零行清理；已有completed/user_abandoned intent不被clear替换。终结/清理成功后关闭本场队列/工作资源；BLOCKED保留必要cause和exact cleanup身份，不转成一个自动重启训练/重连设备的全局服务。配置重建不触发clear；process death不重播队列、不恢复运行态，仅CS-04B读取durable事实。

#### F.9.3 AC与消费者

| AC | 输入条件 → observable结果 | oracle / 责任界线 |
|---|---|---|
| R01 一场与初始化cut | bind返回前后多个通知、snapshot0已有live、同毫秒同BPM → 初始化与post-cut边界准确，无重复/漏接/旧点回填。 | S02真实producer→Recorder→S03真实Room的固定ledger；不以UI StateFlow证明 |
| R02 四类事实合流 | phase/intent/device/sample夹杂以及pause/extra-rest → 顺序和durable图按相同cut闭合。 | 独立逐项预期tuple、phase/acquisition/sample行；各mode实际发出事实仍归后续 |
| R03 late enable/off-on | 先无recording、再启用/关闭/重开 → 仅一recording，冻结参数不换，启用前无样本补写，intent与设备缺口正交。 | actual Recorder调用+Room，固定enable前后样本与零样本时段 |
| R04 active失败独立性 | 首次真实持久化失败 → 原cause、confirmed tuple保留，后续不写、不修改engine或制造terminal。 | 实际Room失败和后续命令的DB不变；source依赖方向无engine控制；mode另证UI/engine继续 |
| R05 terminal freeze | 完成/放弃与晚到notify/重复terminal竞争 → 一次exact终结、唯一original、冻结wall end不漂移，失败仅same-intent重试。 | S04真实外层事务+Recorder端结果观察 |
| R06 on-clear前置barrier | 直接调用clear后立刻另entry prepare/start → 已PENDING而拒绝；STARTING/ACTIVE/TERMINATING/FAILED/RELEASED各走F.8对应行。 | 真实repository/Recorder并发，不宣称Android lifecycle；S07负责framework调用 |
| R07 清理与stale | 清理成功才RELEASED；旧entry、已释放callback和错误binding不影响后来训练。 | 两场顺序/并发真实生产调用及DB/binding/cache结果 |
| R08 进程及边界 | 取消原调用scope仍可按barrier收尾；新repository只见durable事实、不见旧队列/owner；解绑不关BLE。 | Recorder自有收尾边界+原reconciliation资产；force-stop/设备另由mode/E20承担 |

拟验证位置采用D5已经明确给出的 literal path：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionTimelineRecorderTest.kt`（尚未创建，未读取04C候选）。可复用既有真实Room fixture但不得为测试加生产seam。正式allowed paths仍需按S05后base绑定必要repository typed消费，不默认扩展到Activity、mode、Service、schema、export或算法。

状态：`DRAFT_1_RECORDER_ORDER_AND_HANDOFF_DEFINED / NOT_REVIEWED / NOT_READY`。S05前置及S02完整production mapping仍未成为已实现证据；本节不宣称capacity PASS。下一主管理工作是mode生产接线与必要历史消费者，并检查尚未确定的真实UX取舍；不是要求用户再次批准S03–S06每个普通细节。

### F.10 E18-S07 入口核对与 UX-SPACE-01 决定记录（已关闭）

本轮已核对immutable main中计时Route：session identity、startedAt、engine、clock anchor和终态写入状态仍用 `remember`；真实ready gate点击时采集 `Instant.now()` 并发出Start。这个事实支持F.3/S07的Activity-retained生产接线责任，不是本轮已实施。S07完整family/index/phase映射、MainActivity/shell生命周期接线、错误surface和匹配Android evidence尚未闭合；不能把本节当成S07完成合同。

直接读取源路径：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionRoute.kt`。本轮对同一main的app/src/main及app/src/test检索 `StatFs / StorageManager / usableSpace / freeSpace / low.storage / 低…空间 / 空间…不足 / 清理空间` 没有命中；这只证明该检索范围没有定位到现成预检查，不证明所有间接平台行为不存在。

#### F.10.1 为什么此处需要用户决定

来源是用户先前明确提出的“提前检查手机剩余空间，低于多少建议先清理”，本文件B.1已保存“具体提醒阈值尚未作为已接受参数固定”。既有D5已经规定真实持久化失败和训练engine独立的行为；本次问题是此前未确定的**训练开始前的提醒体验**，不是重新讨论内部不可能状态或已接受的失败合同。

`UX-SPACE-01` 初始候选建议，状态 `WITHDRAWN_200_MB_RECOMMENDATION_NOT_ACCEPTED`：用户随后要求先核对Android系统提醒阈值；核验事实和最新判断见F.10.3。下表仅保留此前建议来源，不能继续当作当前推荐或已接受合同。

| 项目 | 提交用户讨论的具体建议 |
|---|---|
| 提醒线 | App记录数据库所在存储卷可用空间严格小于 `200000000` bytes（200 MB）时提醒；等于或大于不触发。该数字是保守产品提醒线建议，没有被容量实测证明为足够保存任意一场训练。 |
| 呈现 | 在训练准备/开始页显示“手机可用空间不足200 MB，建议先清理，以减少记录保存失败的风险。”沿页面现有提示样式，不用强制弹窗打断。 |
| 是否允许训练 | 低空间提示本身不禁用开始。正常Start仍受已接受的仓库准入及事务结果约束；不能用“允许点击”绕过实际保存错误，也不承诺空间检查能保证随后写入成功。 |
| 检查时机 | 进入本场准备页及用户实际点击开始时刷新可用空间；只在尚未开始的入口消费此提示，不轮询打断正在训练。配置重建沿同一入口状态。 |
| 责任与后续 | E18-S07先闭合计时入口，E19两个mode复用同一已确定规则并分别验收；不由runtime/Recorder/数据库validator弹UI，不建立存储管理owner或自动清理用户文件。导出目标/provider写失败仍按其真实边界处理，不能以数据库卷剩余空间替代。 |

此前要求用户选择200 MB的提问已撤回。系统阈值事实已回答，用户随后决定不加入重复功能，最终处置见F.10.4；本小节和F.10.3中的候选方向仅保留讨论历史，不再是待回答门禁。

#### F.10.2 本轮完成与剩余覆盖（恢复检查表）

- F.6/S03：8条AC，冻结Start、完整graph结果识别、活动事务和原始错误边界已形成草案。
- F.7/S04：6条AC，existing CS-05外层接合、no-HR、exact终结payload、回滚/重试边界已形成草案。
- F.8/S05：17条D5准入/释放分支均有新primary disposition，注明cache/owner/已准入消费者不能绕过或自阻塞；没有宣称测试执行。
- F.9/S06：8条AC，snapshot cut、合流、late enable、失败独立性、terminal/clear及stale边界已形成草案。
- 上述写后曾回验：V2为152025 bytes，SHA256=`9C6321DE7726732E137955A14B557BDAB0C2963932D4DB8ED62A96DE7E7E401B`；UTF-8无BOM/LF、原19520-byte历史后缀身份一致；Primary49条status逐项与本场基线一致、Integration clean且main未变。此为加入F.10前身份，不是本文件最终hash。
- 剩余：S02完整production cause→pair映射；S07完整模式合同（UX-SPACE-01已关闭）；S08严格读取、S09历史入口；E19–E22其余候选正式边界；完整subclause双向coverage、exact生产/测试/evidence路径、immutable前置及十维capacity；正式规划审查/接受/readiness仍未执行。21标签/34边只证明已有候选图，尚不证明全义务coverage或正式DAG。
- 十维capacity当前仍无任何新增 `CAPACITY_PASS`。S03–S06的outcome/transaction或owner边界已具体化，不能用AC/分支数量代替obligation closure、future prerequisite满足、单Writer/Reviewer可判定性或完整evidence能力证明。

#### F.10.3 Android系统低空间阈值核验（2026-09-06，source-only）

用户问：“首先，你查看下安卓系统在空间低于多少的时候系统会报警？”本次只读固定Android官方AOSP源码，HTTP内容仅在内存base64解码/计算hash，没有下载到项目、安装或操作设备。没有声称已核验用户手机的厂商配置。

`PROVEN`（所列官方版本）：Android13/15/16的 `StorageManager.getStorageLowBytes(path)` 常规默认阈值为 `min(volumeTotalBytes * 5 / 100, 500 * 1024 * 1024)`。500 MiB = 524288000 bytes，约524 MB；计算对象为存储卷而非手机宣传容量，也不是固定“剩余5%就通知”而忽略上限。两个值分别可由 `Settings.Global.SYS_STORAGE_THRESHOLD_PERCENTAGE` 和 `SYS_STORAGE_THRESHOLD_MAX_BYTES` 配置覆盖；这些是系统源码/隐藏API，不自动成为普通App可调用接口。

Android16的 `DeviceStorageMonitorService.checkLow()` 先在接近阈值时尝试清缓存：可用空间低于lowBytes的150%时尝试清到200%；随后依据实际剩余空间判断。检测到 `usableBytes <= lowBytes` 进入LOW，`updateNotifications`在进入LOW时构造系统低存储通知；不是承诺必有弹窗、声音或每次跨阈值即刻展示。该源码另有默认内部卷低于250 MiB的boot-image保护分支；FULL默认阈值为1 MiB，与较早的LOW提醒不是同一等级。普通较大内部卷的5%已超过500 MiB时，常规默认提醒线落在500 MiB。

`UNKNOWN`：用户具体手机品牌/Android版本、厂商修改及实际阈值/提醒呈现。本轮没有设备权限范围内的实机查询，因此不能把AOSP默认值说成所有Android手机统一500 MiB，也不能把系统通知已显示当作已测事实。

主管理判断：此前200 MB建议晚于所核验AOSP常见默认LOW提醒，并无训练容量实测依据，因此撤回该**固定值推荐**，保留用户“提前检查/建议清理”的需求。下一步优先讨论与系统低空间判定保持一致、是否还需App准备页提示；普通App可用公开机制及本项目适配需要在选定方向的局部设计核对，不能直接调用上述hidden方法或私自按500 MiB再写一套全厂商阈值。此为研究结果及后续方向建议，不是用户已接受的新UX/架构合同。

| Exact官方来源（hash是base64解码后源码字节） | Bytes / SHA256 | 证明范围 |
|---|---|---|
| https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/core/java/android/os/storage/StorageManager.java?format=TEXT | 116847 / `AA037BFC3F93ADAE3DFC9E017A14DF1B14F92071623820D2CAB15EF51A5B3C12` | 默认5%/500MiB、配置覆盖、FULL1MiB及hidden标记 |
| https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/storage/DeviceStorageMonitorService.java?format=TEXT | 25844 / `1BAE7DAC66291DB887217253DE0E710D6478A206E179CF9653D61F9ADA74AF9E` | 清缓存、LOW/boot保护判定及通知/broadcast生产调用 |
| https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/core/java/android/os/storage/StorageManager.java?format=TEXT | 114573 / `6501EF7C388C8AE29C240161317DFD1C04CF26AB81CFB6528F1FB1B17FD3019A` | Android15同一默认公式 |
| https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-13.0.0_r1/core/java/android/os/storage/StorageManager.java?format=TEXT | 114243 / `2EA3F94EFCAA553AAB7CFABB028434CA573110C8281046A5C890CDCAFA598955` | Android13同一默认公式 |

#### F.10.4 用户最终决定（2026-09-06）

用户原话：“所以忽略低空间，低空间时系统自己就报警了，不需要我们来加入这个重复功能。”

状态 `USER_ACCEPTED / CLOSED / APP_LOW_STORAGE_FEATURE_EXCLUDED`。低空间提醒由系统承担；不增加App预检查、200MB/500MiB或其他阈值、准备页提示、系统低空间广播接线、轮询/后台监控、对应Start限制或存储管理owner。没有保留“以后Writer自己决定提醒数值”的隐含工作。正常读写失败、原始错误、事务回滚和诚实保存状态仍沿既有合同；此决定不要求删除它们。除非用户明确重新提出App低空间体验，后续合同/coverage不自动重开此项。唯一primary disposition为E18-R18的excluded，不派新增Story。

### F.11 E18-S07 细分：事实映射与计时生产接入 DRAFT-2

本轮容量结论为原候选 `E18-S07: SPLIT_REQUIRED`，不是新增用户能力。旧一个候选混合了可单独审查的纯domain identity/predicate和Android状态保留/退出边界；拆分沿C已接受owners，不改变E18的done或五Epic顺序。新候选标签 `E18-S07A` 和 `E18-S07B`；F.3及其他旧段落中的S07为历史合称，按本节primary disposition消费。正式Story及immutable前置仍未创建。

#### F.11.1 E18-S07A 计时canonical事实与唯一结构predicate

old→new：现有engine/adapter的阶段和执行记录没有完整canonical identity消费 → 为 `legacy_timed_v1` / `timed_composition_v2` 生成与当时plan snapshot一致的phase/display事实，并提供main §6.5的唯一true-work/true-rest及结构解析规则供接入、严格历史、聚焦共用。不负责clock、BLE、Room事务、ViewModel或UI。

输入为当场冻结snapshot、真实engine状态/事件和实际命令结果；输出为既有版本的typed phase/display/extension事实。exact keys、R/N/M、literal、ordering和payload version直接绑定同一main技术合同§5.1/§5.4/§6.1–6.2/§6.5–6.6；不重新抄一份不完整variant表，不变更CS-03 validator。不从名称字符串猜结构，不把current plan补入历史，不把work UI颜色当true-work；真实rest必须逐分支positive duration，warmup/cooldown不触发真work eligibility，rounds=1仍必须roundIndex0=0。raw phase存在不以是否有sample为条件，pause有其literal variant，extra-rest保持真实parent/interval关联。

| AC | 合同来源 → 行为与失败 | 独立oracle / 消费者 |
|---|---|---|
| M01 legacy全variant | main§6.1全部行及NULL/missing → 精确family/payload/index，rest/boundary不补不存在exercise。 | 固定snapshot+真实engine展开，每行独立literal预期；S07B/S08 |
| M02 composition全variant | main§6.2 → real/synthetic ID、round/stage/target/instance/step index准确，rounds1与多round都可解析。 | 真实adapter固定计划；8个不同索引含义不得互替；S07B/S08 |
| M03 真work/rest | main§6.5每个positive-duration分支 → 同一predicate；warmup-only、rest0、解析失败不冒充可聚焦。 | 正反fixture由合同给期望，不调用被测predicate构造期望；S08/E22投影 |
| M04 实际phase轨迹 | pause/resume、skip、结束、零样本/零时长phase → 保留真实可达轨迹、同cut顺序与metadata append-only引用。 | engine命令事件序列；不以最终界面一个currentStep证明中间轨迹 |
| M05 已有执行事实 | SessionStepRecord/实际rest extension与冻结snapshot一致；同一实际事实可被S04/严格历史消费。 | 原mapping和新typed事实逐项一致，计划值不冒充实际；不新增HIIT次数 |
| M06 边界与版本 | unknown/corrupt/mismatched identity → typed失败，不clamp/default、fallbackMode或改snapshot。 | 当前strict validator+独立invalid输入；不新增第二validator/分析算法 |

候选生产位置采用D5已经定位的 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TimedCanonicalFactsV1.kt`（拟新增，未创建/未读取04C）；直接mapping消费者为同目录已存在 `WorkoutSessionRecordMappers.kt`。现有 `core/engine/TimedWorkoutEngine.kt` 和 `core/model/TimedCompositionTimelineAdapter.kt` 是只读输入边界，除非发现现有真实事件无法表达合同事实且已明确批准因果范围，不默认重写engine/adapter。正式新增测试literal path与所有variant逐条索引仍须F7绑定。

#### F.11.2 E18-S07B 计时Activity-retained生产接入

old→new：Route的remember持有engine/session/time，shell的remember持有active plan，退出直接移除入口 → 同一Activity内真实训练entry及当时plan/engine/clock/Recorder由既有决定中的计时ViewModel保留，Route仅发命令/展示，真实退出先完成或交接S06收尾。

本轮直接源码事实：MainActivity已取Application唯一repository，但通过 `onRecordWorkoutSession` 调通用upsert；TrainFlowApp多个结束回调直接finishTimedSession移除activePlan；Route终态使用 `TerminalWorkoutSessionRecordWriter` 的attemptedSessionId标记和异常message。该旧helper不能证明canonical exact重试、原cause或可靠RELEASED。因此S07B要把计时生产链整体接到S06/S04结果，不调用旧helper再次保存同一canonical场次；保留E19尚未接入的模式旧路径，不用空回调让功能看起来通过。

| AC | 条件 → 生产可见行为 | oracle |
|---|---|---|
| L01 真实开始 | ready gate显式Start先走已接受reconciliation/准入与冻结初始化，成功后是一场唯一训练；重复点击不换entry。开始采集真实wall/zone/date与本场单调anchor，前台HR不可用不阻止无样本合法记录。 | 真实Activity点击→ViewModel→Recorder→Room，冻结值/唯一graph；失败按S05 disposition，不造成功开始/无主记录 |
| L02 配置保留 | 旋转/Activity配置重建及训练内打开设置再返回 → 同一entry/engine/clock/Recorder/token/plan，pause状态、阶段和倒计时不重新Start。 | ActivityScenario真实recreate+生产identity/状态/DB；不能只测ViewModel工厂或helper |
| L03 计时phase接线 | 实际engine tick/命令产生的全部phase/意图/extra-rest事实进入S07A/S06，zero-sample照样完整；时钟不由recomposition重置。 | 两family实际训练序列与Room行；同毫秒输入及pause/额外休息；不从最终engine状态跳过中间事件 |
| L04 结束与入口移除 | completed/user_abandoned先冻结唯一终结请求；正常返回/恢复推荐/回训练主页只在RELEASED后移除该训练入口。终结失败保留原意图、可见保存失败与exact重试；没有“已保存”假成功。 | 三条真实shell callback与Room/read-ready状态；失败/晚到callback不能重新终结或移除新场 |
| L05 真实退出 | Activity finish/真正ViewModel clear在返回前调用S06同步barrier，随后Recorder-owned工作收尾；STARTING/ACTIVE/TERMINATING/FAILED/RELEASED逐状态沿F.8。 | 实际onCleared派发+并发新Activity/Start在PENDING下拒绝；不在取消的viewModelScope中补保存 |
| L06 进程中断 | 两阶段force-stop/relaunch不复活engine/token/queue，必须调用原CS-04B，仅读durable事实封口。 | 新进程实际persistent DB证据；重启时间不伪造ended_at，旧APK证据不沿用 |
| L07 原始失败与训练独立 | 真实活动记录失败在界面明确显示，engine不被Recorder停止；HR掉线沿设备状态表达，不混成数据库失败。 | 真实生产失败consumer和后续engine命令；显示层不吞为“本次无数据”、不泄漏异常堆栈/设备标识 |
| L08 既有体验与边界 | ready gate、倒计时、音振/提醒、暂停、extra-rest、完成/提前结束的已接受体验保持；不新增HIIT次数和E18-R18低空间功能。 | 直接受影响UI/音振回归和用户体验验收；不提前承诺E20后台HR或统一通知已完成 |

生产定位：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\app\MainActivity.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\ui\shell\official\TrainFlowApp.kt`、同目录 `OfficialShellState.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionRoute.kt`，以及D5已定位的新 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionViewModel.kt`。依赖使用已存在Activity/Lifecycle能力；只在actual resolved graph证明必要时才提精确依赖变更，不安装/升级环境。

L01–08是本Story验证范围，Android与真实BLE/UI evidence各自负责，不把Robolectric当实机。正式androidTest/test/evidence paths及设备身份仍须F7绑定。S07B是现有UI/runtime生命周期的一次因果完整接线；不能再仅按MainActivity/Route文件拆成多个各自无法保留entry的变更。若完整AC核对发现仍有独立owner或未接受UX，保持capacity未通过并返回对应边界。

#### F.11.3 拆分后的依赖与剩余风险

S07A根于已合并canonical snapshot/validator及真实engine/adapter；S07B依赖S07A和S06。S08结构严格解析/true-work-rest直接依赖S07A，S09依赖S07B和S08；E20通知消费S07B运行identity，E22投影消费S07A predicate。这修正F.3中S08缺少明确predicate前置的候选边，不改变用户交付顺序。旧21节点标签图作为历史证明保留；更新后的完整候选图另行机械核对，不能把旧34边count套到新图。

当前状态 `S07_SPLIT_DRAFT_2 / S07A_FACTS_AC_DEFINED / S07B_LIFECYCLE_AC_DEFINED / NOT_REVIEWED / NOT_READY`。没有创建新的实现分支/文件，没有自动派发。S07A/S07B的完整variant ledger、最终路径和evidence能力仍需闭合；候选标签不是用户已接受的正式Story数量。

### F.12 E18-S08 严格历史读取与 S09 必要历史入口 DRAFT-1

#### F.12.1 S08 一个版本感知读取边界

old→new：通用 `WorkoutSessionWithRecords.toDomain()` 会使用fallbackMode、默认enum、宽松planned/actual解析 → 同一repository提供明确区分canonical/legacy/invalid/not-found/nonterminal的严格历史输入，供本场历史、E21导出和E22图表消费。列表使用必要header/摘要；打开单场才获取一致的完整图，不能为了日历数量/列表无条件装载所有场次raw samples。完整单场读取的一致性由一次Room read transaction提供，不能拼接不同时间的header/raw/snapshot；多场导出按已接受D分别读，不要求整个批次锁一个长事务。

严格读取含三类不互相替代的验证：现有CS-03 canonical header/plan/graph/structural validators；现有CS-05 original binding与分析语义；main§4.4明确指定而尚未实现的legacy unversioned严格decoder。legacy decoder必须保留旧writer真正允许省略的nullable字段，不能把所有missing一概判坏，也不能default补标题/mode/blocks或丢元素。显式未知版本不是无版本；canonical partial header不能降级legacy。执行子记录的planned/user数值、0/NULL、enum、稳定顺序及原字符串含义沿main§7闭合；现有toFields/mapNotNull/toMap/default逻辑不能作为新严格合同的成功oracle。

| AC | 条件 → 可观察结果 | oracle / primary来源 |
|---|---|---|
| H01 canonical完整读取 | 合法terminal/no-HR或有recording → 每场一致的同cut graph，保留原始plan/display JSON和S01时间值，raw稳定排序。 | production repository+真实Room；main§4.2/§4.3/§7、E18-R09/10/16 |
| H02 original唯一绑定 | originalVersion指向具体snapshot → 只读该row和其合法input cut，不回退latest、不重算/回写；缺失/非法明确typed失败。 | 有其他版本/错误binding/当前参数变化的独立fixtures；CS-05已有validator |
| H03 legacy无损读取 | 合法旧无version root及三mode子结构 → 原值/原NULL完整读取，timeline与snapshotStorage版本仍未知；legacy nonterminal独立标记，不能导出为terminal。 | main§4.4的legacy正反矩阵；旧writer nullable省略与真正缺失required分别验证 |
| H04 非法不降级 | unknown version/mode、corrupt JSON、错误元素/执行字段或legacy+recording → typed unavailable/失败，不default、element drop、重写原行或冒充not_recorded。 | 独立bad fixtures；坏单场不从列表静默消失，导出该场按整份失败规则 |
| H05 日期真值 | frozen local date/zone/offset完整则返回原归属；旧未知单列，晚改时区/跨午夜不移动记录。 | E18-R07/08和S01版本来源；当前系统时区不作为旧数据修复输入 |
| H06 删除与一致性 | read transaction前已删返回not-found；读取期间按DB一致性完成或明确失败；后续重读不返回旧完整cache。 | 真实Room并发删除和再读；不用旧勾选回页例子新增流程，E18-R12保持 |
| H07 结构resolver | 名称/结构来自当时snapshot；精确family/signature/block/round/phase绑定，S07A predicate单一消费。无法解析按原typed unresolved/NULL label。 | main§6.5/§6.6和原CS-09四类resolver分支；不从current plan猜相似阶段 |
| H08 消费与负载边界 | 列表不读取全历史raw；单场/导出/图表共用strict input及failure，同一阶段没样本也保留phase。 | production查询/调用路径+原样本/phase fixture；性能数值在原匹配合同实施时验证 |

生产定位沿F.6两DAO和repository；legacy decoder可沿明确命名的 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutPlanSnapshotStorageJson.kt` 的现有类型/表示实现严格入口，或formal合同先给新literal path；不改旧计划编辑器宽松读取而造成全项目回归。现有通用读取服务的已合并非HR趋势仍保留；新strict consumer不得回退通用toDomain伪造可用数据。字段decoder是该read boundary的无状态内部转换，不新增持久化snapshot/解析owner或修改schema。

完整legacy各block/union及执行字段的正反fixture账本、四类unresolved逐条来源和新增evidence路径尚须F7闭合；H01–08不是宣称该全量ledger已完成。S08依赖S01、S04和S07A明确predicate产品；所有future实现身份保持UNBOUND，不因草案能引用方法名就解锁。

#### F.12.2 S09 必要历史入口与删除日期一致

old→new：现有history仅消费通用 `WorkoutSession` / 原日期前缀 → E18真实计时canonical结果经S08进入已有列表/单场详情，显示冻结开始日期/已知起止时间、执行阶段/轮次/暂停/额外休息与可信终态；E22 HR卡/曲线仍后交付。现有history已含统计/非HR图及删除入口，不重建页面、不清除这些已合并资产。

本轮源码确认dateGroups基于 `dateKey`，repository按日删除目前使用started_at前10位，相关子表也按同一旧条件删除。因此S09要同时更新新记录的列表分组与按日删除选择条件，不能只改日期标签使用户删到另一天。冻结日期完整的记录用该日；旧不完整的记录单列且不混入精确日期过滤。旧不完整场次仍可通过既有适用plan/all清理以及后续逐场导出选择处理，不擅自增加新的删除产品入口。

| AC | 条件 → 页面/存储可见结果 | oracle |
|---|---|---|
| U01 真实闭环 | S07B完成或用户提前结束并RELEASED → 真实持久化本场可从已有历史/结束后入口查看，重开App仍一致。 | 真实生产计时→Room→history UI，两个family/no-HR/zero-sample |
| U02 实际与计划分清 | 计划之后修改/删除或参数改变 → 当时名称、执行与时间不改；计划阶段、实际完成/跳过/休息分别呈现。 | 固定旧场再改计划的生产UI；禁止detail读current plan补数 |
| U03 统一日期 | 跨午夜、UTC日期不同于本地开始日、后来时区变化 → 固定开始日分组/按日删除同一集合；未知旧记录独立显示。 | 页面选日→真实DAO删除→完整related rows核对；不得substring替代新日期合同 |
| U04 删除失效 | 当前选中记录被合法清理 → detail失效且无旧cache可继续当真实数据查看；关联raw/snapshot随session cascade。 | 真实删除回调、Room关系和UI刷新；不新增“旧导出勾选还在”的场景 |
| U05 失败/未知诚实 | not-found、invalid/unsupported、legacy incomplete与no-HR/zero/no-eligible分别可辨，未保存不能标已保存。 | S08 typed结果实际接线，不能所有失败变空列表或模拟sample0 |
| U06 brownfield保留 | 旧合法记录、其他模式现有写入、mode/status过滤、已有非HR统计和清理确认仍可用；不增加跨场HR比较。 | 现有HistoryUiStateTest/生产history直接回归；只验证受影响行为 |

生产范围定位：F.6 repository/WorkoutSessionDao（日期选择与相关删除条件）、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\HistoryUiState.kt`、同目录 `HistoryRoute.kt`，以及F.11已列MainActivity/TrainFlowApp的真实consumer wiring。已有测试定位 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\history\HistoryUiStateTest.kt` 与repository测试。日期查询/删除和页面同属一个用户选择集合的不变量，不能只交helper而让生产SQL仍按旧前缀。

S09依赖S07B/S08；新UI若需改变已接受journey、删除范围或记录失败后的用户选择，先回局部UX，不让Writer发明。当前S08/S09均为 `DRAFT_1_BOUNDARIES_AND_AC_DEFINED / NOT_REVIEWED / NOT_READY`，本轮未运行上述任何测试/设备/性能检查。

### F.13 候选依赖图 DRAFT-2（S07细分后机械核对）

F.11变更后，本轮在内存重建完整候选标签图并检查：22节点、36条边、36条唯一边，无unknown endpoint/self-edge，拓扑可排序。该结果替代F.3的21节点/34边作为**截至本节的候选图**；仍不证明immutable前置满足、完整coverage、capacity或READY。原标签S07退出当前节点集，其责任由S07A/B承接；不把它同时留作隐藏第23节点。

| 节点 | 直接前置候选（空表示根于已合并资产，仍需正式绑定） |
|---|---|
| E18-S01 | — |
| E18-S02 | — |
| E18-S03 | E18-S01、E18-S02 |
| E18-S04 | E18-S01 |
| E18-S05 | E18-S02、E18-S03、E18-S04 |
| E18-S06 | E18-S05 |
| E18-S07A | — |
| E18-S07B | E18-S06、E18-S07A |
| E18-S08 | E18-S01、E18-S04、E18-S07A |
| E18-S09 | E18-S07B、E18-S08 |
| E19-S01 | E18-S09 |
| E19-S02 | E18-S09 |
| E20-S01 | E18-S07B、E19-S01、E19-S02 |
| E20-S02 | E20-S01、E18-S06 |
| E21-S01 | E18-S08 |
| E21-S02 | E21-S01 |
| E21-S03 | E18-S09、E19-S01、E19-S02 |
| E21-S04 | E21-S02、E21-S03 |
| E22-S01 | E18-S08、E18-S07A |
| E22-S02 | E18-S09、E19-S01、E19-S02 |
| E22-S03 | E22-S01、E22-S02 |
| E22-S04 | E22-S03 |

唯一义务primary分配不能从此邻接表反推：F.11/F.12补上的S07A→S08是明确resolver/predicate consumer invariant，其他边继续继承F.3对应责任。后续capacity再拆分时必须再次更新当前图；此表不可当成已接受最终Story数量。

### F.14 E19 力量与跟练分别接入 DRAFT-1

用户已接受“同Epic、分别接入、分别验收”；不合并为一个三模式生命周期Story。继承F.11/S07B的L01–L08生命周期**语义及证据层**，但每模式都必须有自己的实际生产证据，不能引用计时PASS冒称力量/跟练已通过。E19两个候选均依赖E18-S09的完整可用记录链；不重新实现S02–S06，不提前承诺E20后台HR，不增加E18-R18低空间功能。

#### F.14.1 E19-S01 力量模式

本轮main事实：StrengthRoute在 `remember` 保存session/time/engine，进入页面的LaunchedEffect发Start并tick；已有确认输入、effort选项、实际重量/次数、替换动作、跳过和结束路径。生产迁移保留真实入口体验；不能照搬计时ready页面要求用户多点一次开始，也不能把打开页面的多次composition当多场训练。真正进入已准入训练的单次Start才固定时间，失败不冒认开始成功。

| AC | source → 生产结果 | matching oracle |
|---|---|---|
| S01 阶段identity | main§6.3五variant，四个同set非paused phase共享exact身份，planned/actual/substitution关系严格成立。 | 真实StrengthWorkoutEngine的prepare/active/confirm/rest/pause→Recorder→Room，逐行固定期望 |
| S02 实际组结果 | 已有确认UI提交实际重量/次数/effort、左右侧及替换/跳过 → 当场执行结果进入S04/历史，0与未填不同，不用计划数覆盖实际。 | 实际控件输入→ConfirmStrengthSet等生产命令→持久化→strict history；含form_breakdown，不据此推断未记录动作质量 |
| S03 raw与统计职责 | prepare/pause保留raw、完整零样本阶段，主要统计排除仍由既有分析合同判断；不由UI/Recorder删点。 | 原始行与original analysis读取分开断言；不重做CS-05数学 |
| S04 生命周期 | 配置重建保留entry/engine/Recorder及未提交确认输入；真实退出/terminal/clear/新进程按L01–L08，晚到旧组确认不改新场。 | 力量真实Activity/控件/Room/两阶段进程证据，不以计时fixture替代 |
| S05 既有交互闭环 | 实际组确认、自动/手动休息结束、替换/跳过、音振和结束保存/失败重试、历史呈现保持真实含义。 | 模式生产回归和用户UI验收；不新增自动进阶、动作次数识别或医疗推断 |

主生产位置为 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\StrengthWorkoutSessionRoute.kt`、F.11列明的MainActivity/TrainFlowApp/shell及existing WorkoutSessionRecordMappers；新力量ViewModel/事实转换的精确源/test/evidence路径须在formal合同逐条指定。真实engine是 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\engine\StrengthWorkoutEngine.kt`，不是新runtime owner。

#### F.14.2 E19-S02 跟练模式

本轮main事实：FollowAlongRoute复用 `TimedWorkoutEngine`，进入页面LaunchedEffect发Start并tick，session/time/engine仍remember；不是力量组确认流。该模式的domain family必须是follow_along_v1，不能因为用了计时engine就持久化成timed family或力量set。

| AC | source → 生产结果 | matching oracle |
|---|---|---|
| F01 完整family | main§6.4的circuit/non-circuit action/rest-after、between-round、block-rest、boundary、paused → exact Required/NULL/missing/index。 | 真实preset/snapshot+TimedWorkoutEngine展开与每行独立literal预期；不沿timed family默认映射 |
| F02 真正Start与实际轨迹 | 当场单次准入Start固定时间/参数，pause/resume/skip、结束和零样本阶段进入Recorder；重组页面不再Start。 | 跟练实际页面/engine→Room；当前计划改变不污染snapshot |
| F03 生命周期与收尾 | 同L01–L08的配置保留、进入设置返回、onClear barrier、terminal/RELEASED、fresh-process中断，分别绑定跟练实际入口。 | 跟练Activity和persistent DB证据；不得以力量/计时测试替代 |
| F04 保存与历史 | 完成/提前结束/no-HR/失败 → 历史读取跟练当时动作/轮次/休息，失败不伪报保存成功；无新力量确认或动作次数录入。 | 真实history consumer+UI验收，旧跟练preset/控制保留 |

主生产位置为 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\FollowAlongWorkoutSessionRoute.kt` 及F.11相同shell入口，engine仍复用existing TimedWorkoutEngine。新跟练ViewModel/事实转换精确路径同样在formal合同绑定，不能偷偷创建公共三模式engine wrapper。

两模式当前 `DRAFT_1_MODE_SPECIFIC_AC_AND_LIFECYCLE_CONSUMERS_DEFINED / NOT_READY`。这里的纯mode事实仅供各自生产接线，尚未证明像S07A那样有独立跨消费者predicate责任；暂保留每mode一个候选，最终capacity仍可继续拆分，不能因名称相似自动照抄拆分或反向合并两模式。

### F.15 E20 普通通知协调与合法后台心率 DRAFT-1

直接authority：同一main `docs/planning/decision-log.md` 的D-081及较晚D-082；本轮为核对真实eligibility和通知consumer额外读取 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\docs\planning\e17-auto-reconnect-and-personal-parameters-correct-course.md` §3/§6–7，SHA256=`BFC124C79064141411371660589F0D169977D20D09D8485DDB3DC2CF06B3D6DC`。只承接已接受行为/owner，旧编号、旧未实施状态及“当前不含Room”范围不覆盖本次Git事实/重排。本文件须计入未来Planner inputDocuments总数，分类为accepted main行为来源，不当作新candidate。

#### F.15.1 E20-S01 Application唯一普通通知协调

当前三个Route各创建 `AndroidActiveWorkoutNotificationController` 并在dispose时clear；existing contract只有update/clear。old→new为Application进程内唯一实例消费三模式retained entry的最新状态，ID7200单writer且能拒绝旧producer/version，不把planId当一次训练identity。

| AC | 条件 → 可观察结果 | oracle |
|---|---|---|
| N01 identity唯一 | sessionId+producer token+单调stateVersion匹配才更新；相同计划下一场、旧页面迟到update/clear不扰动当前场。 | 唯一production coordinator和两个真实producer的有序/乱序fixture；Android notification实际状态 |
| N02 页面不是owner | rotate、临时去设置、Route移除但retained训练producer仍ACTIVE/PAUSED → 同一producer/token/version连续，不因dispose进入producer detach或清通知。真正terminal及N06/N08的真实失联/新进程清理分别处理，不能把清理限定成只可能terminal。 | 三模式真实路由/Activity配置重建证据：同entry/token、不中断提交、不重复Start、不清当前通知；不能只测policy/helper |
| N03 既有普通通知 | 无HR或未具备FGS条件时沿ordinary通知内容/权限；permission拒绝不伪报已发布。 | 既有ActiveWorkoutNotificationContracts及真实平台permission分支；不扩展为FGS PASS |
| N04 单一实例生产接线 | Application唯一实例，全部三模式提交retained状态；不创建第三核心notification interface或旁路notify/cancel。 | Application/Route/Service前置source ownership与平台调用ledger；不得无生产consumer |
| N05 前后场清理 | 同token重复terminal幂等，旧token不能清掉新场7200；pending handoff的合法后续状态不会被旧状态回滚。terminal使该producer的pending detach/reattach失效，晚到超时或重接不能恢复已结束场。 | 实际coordinator生产并发+结果检查；terminal/detach/replacement/下一场乱序，旧清理最多处置其原身份，不清新场；为S02交付writer边界，不预实现FGS |
| N06 真正producer脱离 | 脱离指retained训练producer与Application coordinator的当前提交关系真实解除，不是Route dispose、ON_STOP、静默心率、pause或正常无新状态。存在同场重接的真实脱离时，在有限、确定性、可由既有测试时间控制的窗口内保持最后active通知；匹配重接按N07。到界仍未重接或当前脱离身份无法匹配时，fail-closed使该身份失效并将ordinary转NONE，最多clear一次；不无限等待或猜归属。已知terminal沿N05立即收束，不人为等待窗口。 | 生产coordinator明确detach→保持→匹配重接或到界/不匹配的固定序列；窗口前/到界/重复触发及晚到事件用独立期望；实际三模式接线证明只有真实producer脱离能触发。无变化但仍attached的producer不触发超时；不加周期心跳、watchdog或为测试新增scheduler抽象 |
| N07 重接与版本连续 | 窗口内同session/token重新attached，保留已接收version floor；同session确需replacement时，由同coordinator受控替换并原子使旧token失效，replacement继承已接受的version floor，不能以version归零覆盖最新状态。普通旧token submit/reattach不能冒充replacement请求，也不能清当前有效producer。已terminal/过期身份不能靠迟到reattach复活。 | 按固定输入断言：同token重接、同场受控replacement、旧token晚到/旧version/重复请求、到界与重接竞争；各线性化结果唯一，通知不回退、不清新场。正常配置重建仍按N02同token路径，不为了覆盖replacement而人为替换retained owner |
| N08 新进程清理 | 新Application不恢复旧session/token/stateVersion内存事实；既有进程中断政策不续跑engine。确认无可恢复active-training事实时，由同coordinator幂等清旧ordinary7200一次，不以本进程内存尚未发布过通知为由跳过。启动清理须在接纳本进程新producer发布前完成协调，迟到/重复初始化不能清新场；不创建持久化producer恢复库或第二清理owner。 | 独立的平台遗留ordinary7200初始状态→新Application初始化→清旧一次；重复初始化/立即新Start/晚到旧清理不清新场。真实进程重建与Android通知证据分开绑定，若force-stop自身清通知，不能用空通知状态冒称已证明App清理分支 |
| N09 ordinary权限失败 | 普通POST_NOTIFICATIONS被拒绝时不notify；如存在旧ordinary通知，按当前身份幂等清一次，训练继续；保留实际平台错误信号，不吞为成功。此结果不替代S02的FGS权限处理。 | 当前ordinary→permission denied、重复通知输入、下一场的真实平台分支；调用结果与系统显示分别取证，不将FGS的build/submit分支当ordinary发布成功 |

S02接入FGS时，N01/N05–N09的workout producer identity仍独立存在；不能将它复用为handoffGeneration。coordinator对真实脱离的有界处置只关闭通知提交/发布权，不伪造workout completed/abandoned，不替Recorder保存、释放或停止engine/BLE。S02负责将这些最新身份/目标状态消费到F.15.3/B09–B11：当前FGS或release未确认时，不由ordinary路径旁路cancel7200或恢复notify；清理诉求由既有交接协议处理，Service release ack后coordinator只记相应NONE，不重复clear。合法active/paused后台意外BLE断连不等于producer detach，仍按D-082保持FGS并恢复。

retained架构下的窄适用：F.11.2/L02及F.14已保证配置重建保留producer，N06不能把旧Route-owned承载方式恢复回来。真实owner clear沿L05同步barrier/Recorder收尾，通知producer解除须同步交给现有coordinator，不能依赖已取消viewModelScope后续发送；已知terminal走N05，其余确有pending重接的脱离按N06收束。N07是受控同场替换的既有协议保证，不要求增加用户可触发的替换功能。有限窗口的具体本地数值/任务组织属于既有coordinator内可逆实现细节，必须可确定结束并可测；不能借此新建owner、心跳服务或scheduler接口。若实际接线需要改变上述已接受生命周期或撤销保证，先回主管理与用户讨论，不由Writer自行决定。

定位：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\notifications\AndroidActiveWorkoutNotifications.kt`、同目录 `ActiveWorkoutNotificationContracts.kt`、Application及三mode生产consumer。复用existing contract；此Story不实现Service/Manifest FGS、BLE策略、Recorder或history/export。

#### F.15.2 E20-S02 connectedDevice FGS与后台恢复接线

S01的普通writer与Service的FGS writer按同一ID7200交接，Service不是engine/GATT owner。后台HR资格沿D-082：opt-in+saved exact target+permission+Bluetooth+无manual suppression，并且App明确visible或active/paused训练能合法使用FGS。合法性失败不宣称后台持续采集；普通ON_STOP本身不等于训练终结或清理。

| AC | 场景 → 应有行为 | 所需独立oracle |
|---|---|---|
| B01 合法提升 | 允许的可见前台触发startForegroundService；onStartCommand立即提交connectedDevice的7200通知，无人为delay，Service START_NOT_STICKY。 | Manifest/实际Android Service入口、foreground type和系统状态；不以调用helper成功替代 |
| B02 通知权限拒绝 | POST_NOTIFICATIONS拒绝时仍构造并提交FGS通知，不沿ordinary Ignored路径清空；系统表面呈现按平台事实说明。 | 实际平台permission分支与FGS状态 |
| B03 writer交接 | NONE/ORDINARY/FGS各转换有序且幂等，只有一个实际writer；最新训练/恢复内容被保留，旧callback不能抢回7200。交接身份、release ack含义、未确认冻结和target变化必须同时满足F.15.3/B09–B11。 | coordinator+Service生产调用序列和系统通知；系统表面与进程内ack分开证明，不能用“只看见一条”替代失败矩阵 |
| B04 无断链的锁屏/返回 | active/paused前后台/锁屏，未cleanup且未断链 → 同Application owner、同attempt lineage，记录输入继续进入原Recorder。 | Android生命周期/通知证据 + 真Band/RF/GATT lineage；两层不得互相替代 |
| B05 后台断链恢复 | eligibility仍成立 → FGS/7200维持，显示reconnecting，同owner新generation/attempt按已有bounded policy恢复exact target。 | 真Band断联/恢复、scan窗口和持久化gap/live；不冒称same attempt、不新增watchdog/scheduler |
| B06 资格失效 | opt-out/显式断开/清target/permission或Bluetooth loss/FGS失败/terminal → 对应停止或demote；manual suppression不被自动恢复反悔。release未确认并处于后台/Unknown时沿B10 cleanup且不宣称后台保证；合法后台unexpected_disconnect本身不触发此失败规则。 | D-082各真实producer分支+平台/设备及B10 visibility交叉矩阵；设备原因不冒充用户统计排除 |
| B07 terminal可见性 | 明确前台且原live attempt未cleanup，可留作非训练前台显示；后台/锁屏/可见性未知terminal停止FGS并cleanup。 | 真实terminal与process visibility组合；不以Route存在判前台 |
| B08 启动/提升失败及新进程 | 失败如实表达，不能在无合法FGS后台继续冒称保持；release失败不得伪ack或解冻ordinary。进程死不恢复旧handoff/writer/producer generation、训练或Service，重新visible按D-082已有资格政策处理设备。 | 实际失败/force-stop重启及B09–B11，既有CS-04B durable封口；无第二owner、不恢复旧manual-only禁令 |

生产定位包括S01协调者、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\AndroidManifest.xml`、Application/process visibility、existing HeartRateRuntimeOwner中训练/FGS资格接线及一个connectedDevice Service。当前完整合同沿B01–B15、F28.3路径和F38；旧“仅八行/待F7补合同”的生成时点状态已被替代。实际worktree、设备identity、执行命令与evidence产物literal路径仍需未来F8绑定，不能把规划条款当作已执行的requirement receipt或设备PASS。

E20当前 `DRAFT_2_COMPLETE_F9_BATCH_REPAIR_PENDING_FRESH_REVIEW / NOT_READY`；E20-S02当前合同为B01–B15及F.38，替代原仅八行范围。foreground recovery/参数/胶囊为保留资产，只改新训练后台资格和直接消费者所需delta；不重做既有恢复算法、视觉或扩大扫描设备范围。

#### F.15.3 E20-S02 release失败合同与M1最终证据链（F9-P001/P002修正）

本节只承接F.38绑定的accepted来源；旧Story顺序、旧manual-only规则和旧测试文件命名不恢复。primary均为E20-S02；这些是B03/B06/B08及FGS完成边界的展开，不是新的独立产品能力。

| AC | 条件、状态与直接消费者 | matching-layer独立oracle |
|---|---|---|
| B09 交接身份与发布权 | Application coordinator维护每次交接独立的handoffGeneration、当前FGS writer identity、desired target与latest训练state/version。handoffGeneration不可复用sessionId、workout producer token、stateVersion或BLE attempt generation；同一场多次交接必须能区分。降级先冻结ordinary，再请求当前Service释放；只接受当前pending handoff且匹配writer的release ack。ack只证明串行路径中的stopForeground(STOP_FOREGROUND_REMOVE)正常返回，不是系统UI删除确认。匹配ack后仅当最新target=ORDINARY才用最新状态重发7200；target=NONE不发ordinary，不追加第二次cancel。 | 独立输入序列：同一workout连续两次FGS→ordinary、FGS→NONE、迟到/重复/错误generation ack、交接中更新state或target。断言真实coordinator的发布/取消调用顺序、发布权与最新内容；Android Service入口证明ack来自正常返回，系统表面单独验收。 |
| B10 ReleaseUnconfirmed | stopForeground抛出/未正常返回、或Service在ack前销毁：不发布ack，进入ReleaseUnconfirmed或等价稳定态，ordinary保持冻结，保留原始错误/失败事实。不能靠旧ack、timeout猜成功、重建writer或宣称系统UI已移除来解冻。处于或随后进入Background/Unknown时Application按现有owner执行BLE cleanup，eligibility不得继续宣称合法后台保证。明确visible时沿D-082前台资格策略，ordinary仍不能绕过release合同；terminal/cleanup重复幂等。 | 测试端驱动production coordinator/Service真实失败边界与destroy-before-ack；覆盖失败时及失败后visibility进入后台/Unknown、重复terminal、旧ack到达。断言ordinary零新发布、无双writer/cancel、原错误、owner cleanup/资格事实。平台失败注入只证明该层失败处理，不能宣称真机必然出现该异常；不可新增production hook/interface制造证据。 |
| B11 相邻消费者闭合 | handoff中target重新变FGS必须使用新generation，不能借旧release；旧producer清理不能影响新场；process death不恢复旧writer/handoff事实。Service只拥有foreground发布责任，Application拥有visibility/eligibility决策，HeartRateRuntimeOwner独占BLE；UI消费真实后台可用性，F.5/F.19规范化cause及Recorder消费已有状态/采样，不造Live或重复样本。release失败不是用户opt-out，不回填为用户排除或存储写失败。合法FGS中的unexpected_disconnect仍按B05维持/恢复。 | F.28.3真实coordinator/Application/Service接线，加S02 observation与S06 Recorder直接消费者回归：失败→cleanup产生真实typed cause/gap；合法恢复仍同owner/new attempt，未断链仍same attempt。进程测试分kill前后；纯policy、AVD和Band各限本层，不重做CS-04B。 |
| B12 同一owner测量责任 | runnable FGS具备后，由E20-S02在既有debug入口窄适配shared-owner observer，观察同一TrainFlowApplication HeartRateRuntimeOwner；不创建scanner/GATT/callback/第二owner。测量时间用monotonic观测时点，wall-clock只用于展示/证据时间。区分public state emission与raw notify：StateFlow合并、malformed未公开等限制必须披露，不把emission interval冒称完整notify间隔。进入M1前证明既有观察面足以支撑所声明测量；不足则停止并回主管理明确缺口，不新增核心diagnostic seam或抢占Recorder sink。 | source/实际实例identity证明shared owner；真实Band9锁屏/临时后台M1绑定measurement APK，分开记录保持/恢复、样本中断/真实disconnect/平台失败。独立GATT工具、前台M0、AVD或fake只证明各自层，均不能替代M1；观察面不足不能用记录条数或更低层成功掩盖。 |
| B13 M1→final freshness | E20-S02承担runnable FGS→measurement APK→M1→依据既有M0与本次M1确认或调整final freshness→final rebuild/gates的完整因果链；这些是同一Story内顺序门禁，不能把runnable当FGS已完成。最终等待/Live边界沿现有纯Kotlinfreshness policy锁定；保持原值也须有测量依据和边界测试，当前不猜最终数值。最终FGS/presentation完成声明前必须经独立Review确认该锁定依据。 | M0来源与M1实际日志→阈值理由/保留或修改disposition→既有freshness policy/test身份逐项映射；在边界前、恰到边界、边界后分别验证Waiting/Live→interrupted，原malformed/断链/平台失败语义保持。Live/stale UI、normalized cause、Recorder输入和最终设备验收均消费同一final规则，不能出现两套threshold。 |
| B14 measurement/final身份链 | measurement与final各自绑定source full SHA/executable tree、APK SHA256/bytes、variant/applicationId、入口、唯一owner获取点、设备型号/API/系统/固件及可得identifier、build/install/测量或验收时间和timezone、日志/截图/原始数据完整literal路径。不可得字段写unknown及原因，不猜。最终threshold处理后从final executable重建/install/hash，再执行所有受影响unit/platform、AVD handoff与Band9锁屏后台保持/恢复gate。 | 两套tuple与Git tree/diff比较，明确equivalent或not equivalent及依据；任何production/debug observer/Manifest/resources/Gradle/dependency/variant等executable变化使旧APK证据不能证明新gate，重建并重跑影响范围。相同文件名/旧M1 PASS不等于final PASS；仅文档变化的等价性也须Git证据。即使阈值确认不改数值，仍完成final身份绑定与规定final gates。 |
| B15 失败与接受责任 | M1数据或观察充分性不足、final边界未锁、final-source任一必要gate未过，E20-S02保持NOT_READY/未验收，不声称后台保证、不解锁后续依赖。届时需要新owner/API或无法在既有policy与本Story闭合的行为改变，返回主管理Correct Course；不由Writer猜阈值或新增测试seam。实际设备/产物路径在届时手工合同绑定，本规划不伪造运行结果。 | 单Writer交完整measurement→final包，主管理组织身份绑定人工Band门禁，fresh Reviewer检查因果链及全部直接回归；只有各自授权下通过才可进入后续实施集成。E22最终性能整合gate不能替代本Story的M1/final-source gate，反之亦然。 |

本节B09–B11的pure序列预期应来自accepted状态转换，不能由被测coordinator自行生成；B12–B15的device结果由实际硬件产生，不能由管理候选文字或观察页存在证明。具体命令/设备/evidence产物路径仍属于未来F8手工执行身份绑定，合同中的owner、顺序、失败后果和证据层则已在此固定。

### F.16 E21 自描述JSON、文件、选择与系统交付 DRAFT-1

四个候选分别承担编码、文件准备、选择页面、系统交付。旧单场root/十分钟lease已被B/D窄替代，main§7的数据合同和salvage AD-U-022/023的知情说明、失败不交半文件、用户主动分享继续有效。S01/S02不能只交测试helper无生产consumer：S04最终将S03选定集合、S02文件与Android交付接通后才是E21可用成果。

#### F.16.1 E21-S01 唯一v2 encoder和文件内字典

生产输入是S08的strict terminal结果及冻结selection/generatedAt；单场也通过同一sessions数组。root/session/execution/heartRate与timeMetadata逐字段沿D，不从ViewModel重新拼raw/分析。唯一version-aware encoder消费validated persisted JSON原文和typed执行字段；外层包装升级不重算analysis v1、修改storage version或把legacy NULL合成为1。

| AC | 义务 → 可观察输出/失败 | oracle |
|---|---|---|
| J01 完整结构 | D.2–D.3及main§7各字段/union/required-iff/forbidden/NULL/enum → 一个version2自包含JSON，selection实际ID顺序与sessions一致、无重复。 | 独立full-schema goldens和extra/missing/type/version negatives；三mode与single/multi同格式 |
| J02 精度/事实 | raw同毫秒排序、单位、计划/实际、0/NULL、no-HR/zero/no-eligible/no-zone、gap/user exclusion分清；2500ms统计与20s视觉不互换。 | 原整数/小数边界、kg/lb显式0及invalid NaN/Infinity、各status priority overlap和原分析binding fixture |
| J03 字典全覆盖 | 每字段及planSnapshotJson内部schema都解释名称/类型/单位/来源/用途/关系/空值/enum；同秒多类型可按identity定位，时间锚点derived/observed/unknown明确。 | 字段/枚举双向ledger，无字段漏释/假字段；独立consumer仅看文件可定位某点phase/acquisition/样本及其统计意义，不依赖对话 |
| J04 legacy/时间 | 无版本旧场保留unknown/legacy时区状态，实际endedAt未知和trusted derived终点分开；输出没选某场不能表示那天没训练。 | canonical/legacy正交矩阵、unknown-date、跨午夜/改时区、process_interrupted fixture |
| J05 失败/隐私 | unknown/corrupt/mismatched strict输入整份失败；不降精度、截断、兜底、输出设备地址/GATT或原始诊断。旧notes排除仍保持。 | 非法输入输出不可发布；独立字段allowlist与隐私negative，不能用writer自己枚举字段证明无遗漏 |

完整dataDictionary具体实例、各field path/枚举序列化literal、独立consumer问题集和golden文件literal路径仍待F7闭合，不能以本表代替。新encoder归C的统一export capability；不新增serialization库，既有Android JsonWriter可行性已读，resolved dependency/schema接口按formal base重绑。

#### F.16.2 E21-S02 原子完整文件准备与生命周期

冻结实际选定session IDs后逐场完整consistent read→同一个App-private临时JSON；不先将全部多场raw装内存，不跨场持有一个长Room事务。每场出错整份不交付；全部写完、flush/file-sync/close、流式结构核验后同目录完整发布ready文件。没有旧lease/boot/十分钟打开授权协议；系统URI交付归S04。

| AC | 情况 → 文件/调用结果 | oracle |
|---|---|---|
| P01 单/多场完整准备 | fixed选择、全部terminal合法 → 完整JSON+实际included集合+ready结果，仅ready可交付。 | 真实strict读/真实文件IO/独立JSON读回；不能只检验文件存在 |
| P02 失败/取消 | read、编码、write/flush/sync/close/校验/ready发布任一真实失败，或用户取消 → 不交半文件，原失败可诊断，未交付temp按规则清理。 | 真实文件/DB边界与可取消producer，无production异常钩子；错误不转空成功 |
| P03 进度与作用域 | 已处理场次进度，配置重建继续；回App其他页取消清选，切App/锁屏进程仍活着尽力完成；进程死不自动重发。 | 真实producer+页面生命周期；未授权导出常驻Service |
| P04 保留清理 | 未完成残留下次启动/生成前清理；已分享副本>24h才在规定触发点清理，已成功保存清App副本，外部副本不动。 | 真实专用目录文件和状态，清理只作用自身exact artifact；24h非即时删除承诺 |
| P05 性能/无损 | D.4单场250000点及100场同总量，两warmup/三measured，每次完整prepare<=30000ms、PSS<=384MiB、文件<=128MiB。 | 绑定实现SHA/APK/指定既有环境的原计时PSS规则；不得缩fixture、丢点、换设备或使用编码-only时间 |

S02保存ready文件及准备状态所需最少事实，不建立持久化export snapshot数据库/registry。sharing已可能交给系统的副本不能按未交付取消清掉；precise ready/share时间与重启分类的最小持久化表示需F7绑定实际文件协议，不能借“删了旧manifest”遗漏保留义务或自动恢复旧lease。专用目录排除备份，具体path/provider scope/backup XML允许路径届时逐个固定，不授权通配清理用户文件。

#### F.16.3 E21-S03 日历/日期/模式/计划/逐场选择页面

页面实现B的E18-R04–08/12，直接消费S08/S09当时记录身份/冻结日期，不从今天计划表还原历史。日历上同日多mode分别显示种类和数量，无记录日只表示无记录。支持日历点选及弹出起止年月日、跨月跨年/含首尾，单/多/全部mode及单/多plan，然后预览/取消勾选具体场次；不因abandoned或无HR自动排除它。

| AC | 情况 → 用户可观察结果 | oracle |
|---|---|---|
| C01 日历真值 | 同日三mode/多场、无记录日、跨午夜、后来时区变化 → 种类数量和日期归属准确。 | 固定真实DB集合与页面逐日计数；无推断休息/训练次数补值 |
| C02 双日期入口 | 日历选择与输入日期得到同一包含首尾范围；无效/倒置日期不默改，允许用户纠正。 | 页面实际两种入口/跨月跨年边界，底层匹配同一date合同 |
| C03 多维与逐场 | mode/plan过滤后所有具体场次可审阅和排除，单场/多场选择都保留各场identity，取消勾选不删除任何数据。 | 三mode/同plan不同历史snapshot/放弃/no-HR fixture和实际点击 |
| C04 未知旧日期 | 独立旧记录区可逐条选，日期不完整场次不混入精确日期结果，实际includedUnknown集合可追踪。 | 新旧混合真实fixture与J01–04消费者 |
| C05 选择生命周期 | 去App其他页清空，再进入重读最新记录；配置变化保留；系统交付面不视为离开App选择流程。 | 真实路由/Activity，不添加“去删后旧勾选仍保留”的产品分支 |
| C06 完整选择交接 | 向export capability传冻结ID集合/筛选描述；生成期间不偷偷改变该批选择，任何子场失败不默默导出剩余场。 | 实际页面→S02请求，重复点生成同operation不双发；完整系统交付由S04闭合 |

S03不编码/写文件、不建立第二history仓库。新的日期弹出/日历/selection state归C已接受导出页面owner；完整UX状态矩阵/真实Route入口/无障碍与精确路径仍需F7绑定。

#### F.16.4 E21-S04 知情保存/系统分享接通

| AC | 情况 → 平台/页面结果 | oracle |
|---|---|---|
| D01 知情交付 | 确认导出包含自定义名称、训练时间/参数等后，由用户选择保存或分享；只交S02完整ready文件，无自动上传。 | salvage AD-U-022/023真实页面→platform intent；不是新增每次后台授权流程 |
| D02 保存 | 系统CreateDocument选定URI后写完整字节并完成flush/close才称已保存；取消不称成功，失败保留实际信号/可重试，不保证外部provider可撤回半成品。 | 真系统文档provider写入/取消/错误与导出字节核对 |
| D03 分享 | FileProvider只读临时URI授权，范围仅专用ready文件；提交后只显示“已交给系统分享”，不称接收方保存成功。 | 真实Android chooser/外部reader，intent/ClipData/grant和provider scope；不使用hidden API/固定十分钟lease |
| D04 返回与不确定交付 | 系统chooser取消/返回不冒认成功或重发，不立即清理可能被对方持有的副本；>24h清理按B；App主动离开未交付流程按B取消。 | 系统面/回App/进程中断真实生命周期与私有副本状态 |
| D05 单场/批量统一 | 历史单场入口与日历多场经相同版本/capability/知情说明交付；非法/not-found记录不能继续导出旧cache。 | 两个真实生产入口端到端，外部JSON独立解析；无复制encoder/provider |

E21的exact新source/test/evidence文件、完整字典、public API/URI/cleanup表示及完整平台分支尚待F7，四候选均 `DRAFT_1_AC_DEFINED / NOT_READY`。E18-R18排除低空间监控不改变真实文件失败义务。

### F.17 E22 单场心率复盘与图表 DRAFT-1

不恢复App跨场比较、长期评价或AI建议。所有模式共享S08 strict输入、CS-05 original分析和单一纯projector；UI只显示本场。旧salvage AD-U-001–025未被B窄替代的UX及main§6/§7.5/§9继续有效，phase/意图/设备区间不混为一条时间线。

| 候选 | 原有义务及本次AC | matching oracle与消费者 |
|---|---|---|
| E22-S01 纯图表投影 | Q01：连续实测段独立first/min/max/last+mandatory anchors，raw不改。Q02：20s仅同phase视觉虚线，2500ms统计含义不改，不跨phase/真实缺口画假实线。Q03：raw scrub查原点，同毫秒严格按canonical顺序；最早点最高值/边界锚点依原合同保留。Q04：所有mandatory+<=1600非mandatory，总点<=mandatory+1600，mandatory<=20000的规定profile；1500ms/320MiB，按既有P-BALANCED-V2。Q05：S07A唯一true-work/rest、main§6.6多block同round身份和invalid restore规则直接消费。 | 独立固定ordered raw/anchor goldens+pure性能窗口；不是Compose/GPU/frame-time claim。依S08/S07A，输出供S03/S04；不重算统计/删除raw。 |
| E22-S02 独立HR紧凑卡 | K01：完成/放弃/历史同一persisted result，紧凑HR卡独立于通用本次数据卡，可按accepted状态整体出现/消失。K02：not_recorded、zero_samples、no_eligible、no_zones、低覆盖等按原三个正交轴/priority呈现，legacy nonterminal typed unavailable。K03：暂停与额外休息独立标签/事实源，设备不可用不当用户opt-out。K04：三mode结束/历史真实入口消费同一结果，不以当前参数刷新历史。 | main§7.5/原status语义完整UI矩阵+真实三mode生产consumer/用户验收；卡不拥有serializer/projector/finalizer。 |
| E22-S03 竖屏单场分析 | V01：真实单线、阶段带、原主要训练均线/最高点、六区间/未覆盖条及单位图例。V02：phase点击选中/显示详情不立即改轴，拖动scrub与tap阈值区分，密集阶段有顺序导航。V03：whole/round/work/rest按准确family/block/round/phase身份及predicate切换，不跨block猜相似阶段。V04：低覆盖与密集短缺口提示持续，zero/unknown与数据缺失不能混淆。V05：大字/TalkBack/非颜色编码，竖屏即可完成必要信息访问；删除后失效。 | projector输出+原analysis事实的真实Compose/UI矩阵，720×1280/Big Type/TalkBack和用户视觉验收；不修改projection采样或统计。 |
| E22-S04 显式横屏与状态恢复 | W01：用户点明确按钮进入同Activity横屏专注图表，不新增横屏Activity；与竖屏同数据/projector。W02：back先恢复原竖屏selection/focus/scrub，普通rotation保留有效状态；失效identity回whole。W03：设备方向政策/大屏不执行方向请求时仍可用，不假报横屏已生效。W04：横屏非无障碍必经路径，真实大字/TalkBack/返回/删除/进程重建按sessionId再读，旧数据不回写。 | 实际Android orientation/Activity与UI证据；工具无法证明的主观视觉仍由用户验收，不能用纯SavedState单测替代。 |

S01全部算法细目和S02–04完整S4–S7状态/微文案仍以既有来源为准，上表是primary责任/AC索引，不是删除未列微条款的授权。确切Projector/AnalysisViewModel/Route/test/evidence literal路径、mandatory anchor完整ledger、visual provenance和各state oracle继续在F7闭合。原E17末尾evidence-only项的必要cross-layer身份复核分配到这些实际交付的acceptance，不新增一个末尾大Story来补前面缺证。

当前 `DRAFT_1_SINGLE_SESSION_PROJECTOR_AND_UI_AC_DEFINED / NOT_REVIEWED / NOT_READY`。候选依赖图仍F.13的22/36，但source义务和formal合同/evidence尚未全部绑定；不能把这些表当正式Planning Review PASS。

### F.18 F7细粒度核对：S02 cause来源、旧UX和排除项

方法入口新增读取 `C:\Users\25073\.codex\skills\bmad-method\references\obligation-traceability-and-evidence.md`（F7）；另读取 `C:\Users\25073\.codex\skills\bmad-method\references\readiness-and-manual-handoff.md` 的readiness门禁定义以避免把候选提前升级，没有执行F8/独立Review。两者是方法，不是产品authority。

#### F.18.1 S02 cause→pair 生产分支补充草案

本节消费F.5/已有salvage§14.23–24。直接源码仍绑定同一main；新增细读HeartRateRuntimeOwner的action/eligibility/recovery/scan/connection/订阅/freshness分支，及 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\HeartRateFreshnessPolicy.kt`。现有UI fact会丢弃原因：`DataInterrupted`不携带first/later区别，`IntentionalStop`可来自manual suppression或BACKGROUND_WITHOUT_FGS，`NotConnected`可来自无目标或scan window结束。因此规范化cause必须从真实分支产生，不能只对最终UI fact做一张when表；runtime私有身份/源地址不进入cause。

下表是F.5/O07的branch disposition细化，不改恢复策略/阈值；schema vocabulary沿现有AcquisitionV1Validator，connection_timeout仍production unreachable。`NULL`表示不制造没有证据的原因，不允许后续reader从日志补原因。

| source分支/条件 | cause语义 → normalized pair | 必须分开的证据 |
|---|---|---|
| Disable / OPTED_OUT、明确manual Disconnect / MANUAL_SUPPRESSION | 用户停止采集 → not_observing/NULL；实际用户intent由S06独立消费 | 同时核对user_turned_off/opted_out/suppression各真实命令，不用device pair反推intent |
| enabled idle，NO_SAVED_TARGET且没有仍在运行的manual scan | 未选择source → no_source_selected/source_not_selected | clear target不同于opt-out；manual scan仍在运行时不能以无saved target覆盖searching |
| enabled idle但已有target、尚未进入scan/connect、手动StopScan后无attempt | 当前未连接且没有更准确原因 → disconnected/NULL | 不把所有NotConnected都解释成source_not_selected或source_unavailable |
| 首次无permission；既有grant后丢失/受保护平台调用SecurityException | permission_required/permission_missing或permission_revoked，按producer已有grant/授权操作上下文区分 | 不能仅从一个PermissionRequired UI fact猜；若不存在此前grant事实则不能声称revoked |
| adapter/platform采集能力缺失，与用户关蓝牙 | bluetooth_unavailable/platform_unavailable 与 bluetooth_unavailable/bluetooth_off | 两种producer上下文保持，不让同一个UI提示抹去来源 |
| startScanOnMain的MANUAL初始采集与RECOVERY窗口 | searching/initial_acquisition 或 searching/automatic_recovery | actual ScanOrigin，不从“训练第几秒”猜原因 |
| connectOnMain、DISCOVERING/SUBSCRIBING尚未订阅完成 | connecting/initial_acquisition或automatic_recovery，沿此attempt的原始采集来源 | 不把内部GATT子阶段作为export enum；来源在attempt内部保持，不新增owner |
| CCCD成功，notifyEnabled、等待首个测量 | waiting_first_sample/原始initial_acquisition或automatic_recovery | 通知启用不是已有sample；snapshot0不复制旧BPM |
| 有效measurement使freshness为LIVE | live/NULL，再发独立ValidMeasurement | 状态no-op不得丢measurement；同毫秒原数组副本及receipt顺序沿O01–05 |
| freshness FIRST_SAMPLE_INTERRUPTED与SAMPLE_INTERRUPTED | stale/first_sample_timeout 与 stale/sample_stale_timeout | 直接消费现有HeartRateFreshnessDecision.reason，不从DataInterrupted补猜；不新加计时器 |
| scan timeout且无active attempt | disconnected/source_unavailable，之后若恢复仍armed进入对应恢复等待事实 | window结束不证明out-of-range；没有给后续armed状态加“永久停止”含义 |
| 已建立attempt发生STATE_DISCONNECTED | disconnected/unexpected_disconnect；有资格的恢复等待/恢复开始 → reconnecting/automatic_recovery及后续searching/automatic_recovery | 原始断开与恢复两个真实cut均保留，recovery发布不掩盖缺口，不称same attempt |
| 连接前candidate已不可用/CONNECTING阶段失败且没有更具体稳定原因 | disconnected/NULL | 不能虚构搜索超时或connection_timeout；真正平台异常按下行，不因共享CONNECT_FAILED enum合并所有原因 |
| 明确scan platform failure或真实平台connect调用失败，且没有更精确既有归类 | technical_failure/platform_failure | 仅确认的platform/runtime失败可用；不能把任意unknown state兜底成该pair |
| service/measurement characteristic不存在、notify/indicate能力缺失、CCCD无法建立标准流 | technical_failure/measurement_stream_unavailable | 能力失败经真实分支；只存稳定原因，不存service/CCCD/GATT码；真正permission loss仍优先其准确事实 |
| BACKGROUND_WITHOUT_FGS、非用户的owner停止采集 | disconnected/NULL（仍expected时）；不是not_observing，更不自动改user_excluded | 区分用户stop与平台/生命周期stop；E20合法后台实现后按实际eligibility路径改输入，不回填历史 |
| malformed payload、旧/wrong GATT/characteristic、不可接受phase回调 | 不产measurement；只在现有policy真实产生transition时记录对应cause | 不刷新freshness，不为丢弃的协议输入伪造故障或样本 |
| unknown future cause、内部合同不成立 | 明确typed contract failure/诊断，不能选默认device pair | 拒绝未知vocabulary；不为内部不可能状态新增产品防御流程 |

此表仍需把每个列举源分支精确对应到formal fixture/错误路径，特别是：permission history的输入来源、连接失败与平台失败的精确分界、cleanup最终permission/Bluetooth覆盖后的真实cause、recovery等待与原故障的发布顺序。没有因此宣称全部production branch ledger完毕或运行PASS。若实现表明需新增事实表示以保留上述既有含义，可在已接受typed cause/owner边界内细化；若改变state/reason含义、恢复策略或新增核心owner才回F5/用户门禁。

#### F.18.2 旧UX微条款的明确primary承接

为防F.17摘要遗漏，以下是新增具体AC细目（不是新增需求）：

| source | primary Story/AC | 保留约束 |
|---|---|---|
| AD-U-005 | E22-S01/Q06 | bpm Y轴动态外取整/最小跨度、不强制0；具体既有数值需从salvage对应条款绑定，不由Writer随意选 |
| AD-U-007 | E22-S03/V06 | 不加面积填充或连续zone背景；单线/phase带/均线/最高点按原视觉方向 |
| AD-U-008 | E22-S03/V07 | 六区间加eligible_uncovered 100%水平条/斜纹；excluded单列，不塞入分母 |
| AD-U-011 | E22-S04/W05 | 横屏只聚焦图表，不复制区间长文/导出控制；不是另一个分析页面owner |
| AD-U-012 | E22-S04/W06 | 返回还原竖屏滚动位置、view/focus/selection/scrub，不能只恢复sessionId |
| AD-U-014/015 | E22-S03/V08 + S02/K02直接consumer | 一行quality、结构化详情默认折叠；重大不足直接持续可见，不全部藏折叠，不显示raw开发日志 |
| AD-U-016/017 | E18-S08/H02依赖；E22-S03/V09呈现 | no-effective-max仍可显示bpm/均值/最高/阶段；历史不因后来max出现而自动补zones |
| AD-U-018 | E22-S03/V10 | 阶段真实宽度；碰撞可隐藏标签/顺序导航，不能拓宽interval或缩字到不可读 |
| AD-U-019 | E22-S03/V03细分 | timed→轮/work-rest；strength→动作/组-rest；follow→既有动作/阶段；不把四family当同一轮次结构 |
| AD-U-025 | E22-S02/K03、E22-S03/V11消费者 | 主要训练统计/训练最高明确标签，excluded raw点照常显示，tooltip说明不计主要统计；原分析primary为AS-05依赖 |
| AD-U-022/023 | E21-S04/D01 | 用户自定义名称/训练时间/参数仍属个人信息；知情说明不因没有设备地址而省略 |
| UX-DATA-01..12、AE-UX-01..07 | 分别沿既有source映射到F.11–F.17直接consumer，完整逐条ledger继续待补 | 它们是已确认inventory的子义务，不可因不在127个父ID计数内而删掉 |

视觉artifact本轮只核对salvage§13.8记录的文件名/hash/provenance，没有读到原图像，因此只承接已接受文字语义和方向，不宣称视觉像素/原图访问已验证。实际UI视觉仍有各自人工gate；不重画胶囊，不用旧mock示例数值改变算法。

#### F.18.3 authority和非目标恢复防线

E18-R18是唯一新的accepted范围delta；F.11–F.18均为主管理候选/合同细化，不由“继续”自动变成accepted Story。原127索引的AS-01–06保留已合并事实，AS-07 held/unreviewed；AD-P-011/012/014/016和RES-01–04按E18-R02暂缓App比较/进阶，AD-P-013的当场参数不可变仍保留，不因比较暂缓连同快照删掉。RES-12批量进入E21；RES-05/06/07/08/09/10/13和roadmap其他future保持原重访条件。低空间App功能只按E18-R18排除，不扩大为删除实际失败处理。上述条目必须出现在最终双向coverage的disposition中，不能把excluded/residual universe清零。

### F.19 S02 cause细粒度来源与证据边界 DRAFT-2

本节对F.18.1作精确限定；F.18概括行与本节不同处以本节候选细化为准，但不能改变salvage§14.23–24/E17-ARCH-18/19的accepted含义。仍为规划source inspection，未实施或运行测试。只读取同一immutable main的runtime与既有platform测试；没有读取04C候选。

#### F.19.1 生产事实、观测上下文和失败优先序

- `updateRecoveryEligibilityOnMain`目前先覆盖`recoveryEligibilityInput`，`cleanup`收到PermissionRequired时又写false；这些当前值不能单独证明“之前授予后撤销”。S02应在同一现有runtime owner、覆盖前保留实际观察到的授权事实：资格输入明确grant或真实权限检查全通过是已观察grant；首次实际缺权限且本进程无grant证据为permission_missing；已观察grant后实际失去权限为permission_revoked。重复同一缺失不把revoked退回missing。新owner进程不继承旧进程权限历史，不新增持久化列/权限历史服务。受保护调用SecurityException仍走现有权限失败路径，reason取真实grant上下文；不能只凭异常名称宣称曾经授权。
- 初始/恢复来源必须在生产发起点留下：手动StartScan/Connect是initial，恢复窗口及其目标命中自动Connect是automatic。`connectOnMain`会撤下activeScan，因此在撤下前保留本attempt来源至等待首样本，不从后来的recoveryState.CONNECTING_OR_CONNECTED倒推。所需私有typed上下文仍属于现有owner/attempt，不成为Room字段、export字段或第二owner。
- `cleanup`先失效身份、拆下资源，再确定finalFact，最后可能scheduleRecovery。记录通道先消费**最终有效cause**，不把requestedFact先发布一次再把清理覆盖当第二真实状态。清理里实际SecurityException且`permissionLossOverridesFact=true`时，按权限cause覆盖；false分支保留明确用户停用、无目标、蓝牙关闭或后台停止事实。主动intent由S06独立记录，即使设备最终cause被权限覆盖也不丢用户意图。
- `publishRecovery`表达调度/资格，并非每次调用都形成新的设备状态。只有实际armed等待恢复且没有active attempt/仍在工作的manual scan、没有明确permission/Bluetooth/platform-capability停止条件时才产生reconnecting/automatic_recovery；随后真实开始恢复scan才是searching/automatic_recovery。CONNECTING_OR_CONNECTED不覆盖connecting/waiting/live/stale，disarmed也不从UI枚举单独制造用户停止。这样保留原故障→真实恢复等待→实际搜索的cut，避免一个重复资格更新抹掉更准确故障。
- 现有`cleanup`仅对requested PermissionRequired更新资格；清理末尾才发现的权限异常不能因旧资格仍true而被记录为权限已恢复。S02在cause投影中保持最终权限事实，直到真实资格/权限恢复并进入有效采集行为。该记录限定不修改扫描重试策略；若实现必须修改已接受的恢复策略才能满足它，则返回该架构/行为门禁，不能隐含修复。

#### F.19.2 精确分支→pair→独立oracle（S02/O07子行）

全部行的primary owner均为F.5.6的`HeartRateRuntimeOwner.kt`产生typed cause、唯一`CanonicalHeartRateObservationMapper.kt`映射；trusted内部cause交接不加fallback，平台/协议输入为真实外部边界。下游均为S06合流、S03写入既有acquisition validator，再由S08/E21/E22消费原literal。每行fixture必须断言独立literal和receipt顺序，不用production mapper生成expected。

| 子行 | exact生产分支及条件 | 必须可观察结果与负向界限 | 匹配fixture入口/能力 |
|---|---|---|---|
| O07-P1 | `startScanOnMain/connectOnMain`初次hasRequiredPermissions=false；首次资格输入无grant | permission_required/permission_missing；不伪称revoked | 新建真实owner，Robolectric权限deny，绑定sink后真实action；再bind检查snapshot0 |
| O07-P2 | 已观察grant后`PermissionLost`/资格撤销/平台受保护调用失败 | permission_required/permission_revoked；重复撤销保持，同进程重新grant后可采集 | 既有grant→deny/action及TOCTOU上下文；观察新cause而非旧UI文案 |
| O07-P3 | scan start/stop、device address/name、connect、discover、getService/characteristic/descriptor、notify/write、disconnect/close的SecurityException | 每个真实调用点沿权限语义，已知grant上下文才判revoked；不变成platform_failure/stream失败 | 扩展已有platform shadows/真实production action；每调用点独立failure配置，不新增production seam |
| O07-P4 | `cleanup`默认覆盖，包含清理期间迟到/wrong GATT close权限异常 | 仅final权限cause；旧callback不产生measurement；随后错误的调度状态不冒称恢复 | 已有清理/迟到callback fixture+sink ledger，检查同一main cut顺序 |
| O07-P5 | `permissionLossOverridesFact=false`各明确停止分支 | 保留requested语义；后台停用不是user_excluded；主动intent不因设备清理改变 | Disable、OPTED_OUT、MANUAL_SUPPRESSION、NO_SAVED_TARGET、PERMISSION_UNAVAILABLE、BLUETOOTH_OFF、BACKGROUND分别驱动 |
| O07-B1 | manager/adapter/scanner不存在 | bluetooth_unavailable/platform_unavailable；不称用户关闭蓝牙 | 现有平台shadow可配置的真实能力缺失边界 |
| O07-B2 | adapter存在且isEnabled=false或明确BluetoothOff action | bluetooth_unavailable/bluetooth_off | 已有蓝牙关闭/scan cleanup fixture |
| O07-B3 | `detachAndStopActiveScan`的IllegalState且adapter缺失或disabled | 缺能力与disabled需保留实际predicate证据，分别沿B1/B2；不能仅由旧helper Boolean都判用户关闭 | 同一异常下adapter不存在、disabled、仍enabled三支；enabled的未知异常原样传播，不兜底pair |
| O07-C1 | `connectOnMain`candidate/device已不存在，且无active attempt | disconnected/NULL；不是搜索超时、不是已证明平台故障 | 真实action传已失效候选，检查原连接未被抢占/无新样本 |
| O07-C2 | `connectGatt`返回NULL | disconnected/NULL；返回空本身不证明具体故障原因，不能凭CONNECT_FAILED枚举归platform_failure | 已有真实connectGatt平台shadow返回路径；不新造watchdog |
| O07-C3 | `STATE_DISCONNECTED`发生于CONNECTING | disconnected/NULL；不称已建立连接后unexpected_disconnect | callback状态+真实attempt phase；与C4独立fixture |
| O07-C4 | `STATE_DISCONNECTED`发生于DISCOVERING/SUBSCRIBING/WAITING_FIRST_DATA/LIVE | disconnected/unexpected_disconnect；最后有效样本不能跨边界 | 已建立连接后各可达phase的callback；检查旧attempt迟到输入拒绝 |
| O07-C5 | 非DISCONNECTED但status非GATT_SUCCESS | disconnected/NULL；原status不可直接变成稳定原因或导出GATT码 | 与C3/C4分离的非success callback；不从status数值猜原因 |
| O07-T1 | active scan `onScanFailed`且无active attempt | technical_failure/platform_failure，然后仅在实际可恢复条件下产生恢复等待cut | 已有SCAN_FAILED_INTERNAL_ERROR回调；原SDK码/字符串不进payload |
| O07-T2 | scan失败/timeout但另有active attempt | scan列表/恢复调度可改变，当前连接cause不可被扫描结果覆盖 | 并存scan/attempt现有production路径；live仍独立发measurement |
| O07-T3 | 无active attempt，搜索期限结束且本窗口有expected source | disconnected/source_unavailable，再按真实armed状态恢复；不称out-of-range | recovery目标存在的timeout与manual scan有目标timeout分开 |
| O07-T4 | 无expected source的manual browse timeout | disconnected/NULL；不能从“扫描过”推导“指定source不可用”；后续NO_SAVED_TARGET真实更新可产生no_source_selected/source_not_selected | 未选目标manual scan→自然timeout；该行窄化F.18过宽的所有scan timeout映射 |
| O07-S1 | 已连接，discoverServices=false或services callback非success | technical_failure/measurement_stream_unavailable；该source未能建立标准流，不输出SDK阶段 | existing discovery失败fixture，分别覆盖调用返回与callback结果 |
| O07-S2 | HRS service/measurement characteristic缺失 | technical_failure/measurement_stream_unavailable | 固定服务树两种缺失分别驱动真实订阅路径 |
| O07-S3 | notify/indicate均无、CCCD缺失、setCharacteristicNotification=false | 同S2；能力事实不能被混成permission或platform_failure | 每个能力谓词single mutation，existing HRS fixture |
| O07-S4 | API33+ descriptor write非SUCCESS、旧API write=false、descriptor callback失败 | 同S2；失败与wrong descriptor迟到回调区别 | 两SDK入口和合法descriptor身份；wrong descriptor不产生新cause |
| O07-R1 | 实际恢复等待、WINDOW_MISSED_ARMED、真正恢复scan/自动connect | 原故障→reconnecting/automatic_recovery→searching/automatic_recovery→connecting/automatic_recovery→waiting_first_sample/automatic_recovery | 使用现有recovery测试action/主looper受控时间、sink有序ledger；不得只看最终state |
| O07-R2 | 初始采集、manual scan与有效attempt上的重复资格更新 | initial来源保持；调度state不能覆盖已有connecting/waiting/live/stale | manual与recovery不同fixture，重复资格输入不能造额外样本/原因变化 |
| O07-F1 | 首样本期限、已有样本失效期限 | stale/first_sample_timeout与stale/sample_stale_timeout，沿现有freshness decision | 现有纯policy固定时钟和真实owner callback+deadline，两层分别验证 |
| O07-F2 | 有效测量与malformed/wrong callback | 合法transition先于measurement且每次保留；非法输入无样本/无新故障，不刷新freshness | O01–O05既有计数/身份oracle，独立预期raw序列 |
| O07-X1 | schema合法但runtime未生产connection_timeout；未知内部cause | 不新增生产timeout；未知typed合同不能默认成pair | 静态封闭分支核对+mapper合法/非法矩阵；不为不可达状态新增生产事件 |

P3是调用点索引，正式fixture必须分别展开其列举调用，不能以一次SecurityException测试宣布整组运行PASS。R1必须涵盖原故障发生时同毫秒恢复排程的有序receipt；不强求故障具有虚构的正时长。P5的用户intent消费者证据属于S06，S02只证明设备cause，不能由一项测试宣称两个owner均已闭合。

#### F.19.3 已有测试路径与证据限制

下列完整literal路径位于已核验main，是未来可修改测试的定位，当前未修改/运行：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerPlatformTest.kt`：平台返回/权限TOCTOU/服务与CCCD/清理；本轮读过相应测试正文。已有反射获取private runnable的旧用法不成为本次新生产证据标准；新顺序证明应经真实action及既有主looper时间推进。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerCallbackIdentityTest.kt`：旧/wrong GATT、attempt、descriptor与measurement身份。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerRecoveryTest.kt`：已定位；对应recovery分支可用性的逐fixture核对仍需完成，路径存在不等于oracle足够。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateFreshnessPolicyTest.kt`：pure边界，不替代owner真实发出观测。

没有创建evidence文件，也没有从候选ID派生artifact路径。正式handoff仍需绑定实际允许的完整source/test/evidence路径、命令和candidate身份；当前仅V2有写授权。F.19使production分支定义更具体，**不等于S02全合同ledger/证据能力已关闭或capacity PASS**。

### F.20 UX-DATA与AE-UX逐条consumer ledger DRAFT-1

来源为已绑定salvage§13的`UX-DATA-01..12`、`AE-UX-01..07`原行；本轮再次读取原行而非从AD-U-023合计数推导覆盖。每行一个primary disposition，复用既有accepted数学/存储时分类为REFERENCE，未交付production consumer为PRESERVE/ADAPT；不是重做AS-03/05。下面oracle均为未来证据合同，未执行。exact代码路径沿各Story边界定位；尚未给出的新路径不能从此表视为已授权。

| obligationId / exact source行 | classification及normalized行为 | primary Story/AC；直接consumer | 独立oracle与必须拒绝的错例 |
|---|---|---|---|
| UX-DATA-01 | ADAPT / REQUIRED：terminal identity/mode/status、真实开始/结束及可信相对时长保持；按E18-R07/08区分真实wall、derived及未知 | E18-S08/H01/H04；E18-S09/U01、E21-S01/J04、E22-S02/K01 | 固定completed/abandoned/process_interrupted/legacy四种真Room输入；同一ID读回，unknown endedAt不可默认成推导时间 |
| UX-DATA-02 | PRESERVE / REQUIRED：名称、结构和历史mode取当时immutable snapshot，当前计划/动作库改名不污染 | E18-S08/H07；E21/J03、E22/V03 | 原计划记录后修改/删除当前计划，严格reader仍解析原label；无法解析用typed unresolved不猜相似对象 |
| UX-DATA-03 | PRESERVE / REQUIRED：完整phase含零样本，真实pause宽度；各family层级按main§6 | E18-S07A/M01–M06；S07B/L03、E19两producer、E18-S08/H07及E22/Q05/V03 | 四family variant fixtures+真实模式调用→持久化→resolver；zero sample不能让phase消失，pause不能从X轴删除 |
| UX-DATA-04 | ADAPT / REQUIRED：raw canonical sample顺序、offset、bpm原值可追溯；source-validity沿较晚ARCH-24仅保留合法字段，不恢复设备ID | E18-S06/R02–R04；S03事务、S08、E21/J02、E22/Q03 | 同毫秒同值多measurement独立receipt/sequence及最终raw；golden检查ordinal而非测试先排序；projector丢显示点不能改变raw |
| UX-DATA-05 | PRESERVE / REQUIRED：phase/intent/device与derived分类分别有来源，不从线条断开猜opt-out/process_unknown | E18-S06/R02/R04；E18-S08及E22/K03/V04 | expected+disconnected、user_excluded、paused、process_unknown分别fixture；沿AS-05分类结果，不建立第四套分类owner |
| UX-DATA-06 | REFERENCE / REQUIRED：eligible/covered/coverage及excluded/unknown按原durations和80/50/70规则读取 | E18-S08/H02；E21/J02、E22/K02/V04 | AS-05原绑定结果+reader消费者；恰好阈值/零分母/未知tail，禁止按label或rounded百分比重算判断 |
| UX-DATA-07 | REFERENCE / REQUIRED：avg/max/highest anchor/zones/summary reasons按三轴typed状态读取 | E18-S08/H02；E21/J02、E22/K02/Q01/V01 | 独立原analysis goldens，0与NULL、zero与no-zones分开；最高值四字段anchor同源且不能用downsample结果重选 |
| UX-DATA-08 | PRESERVE / REQUIRED：当场冻结年龄/personal/effective max/来源/alert/六区间；历史不追随设置改动 | E18-S06/R01/R03；三模式producer、E18-S08、E21/J03、E22/V09 | Start已启用与late-enable首次建recording分别检查冻结边界；改设置后原快照不变；alert非第七zone |
| UX-DATA-09 | PRESERVE / REQUIRED：session-scoped严格读取identity；删除使旧read及后续export失效 | E18-S08/H06；S09/U04、E21/D05、E22/V05/W04 | 从真实repository删除并观察打开页面失效；不能只清测试cache，不能继续提交已删除session的旧read |
| UX-DATA-10 | ADAPT / REQUIRED：terminal一致读取、自包含export v2、HR显式status、隐私、整份完成后交付 | E21-S01/J01/J03/J05；S02/P01/P02、S04/D05 | 每场独立事务快照、两场混合版本/损坏单场整份拒绝；实际输出字节由独立reader解释 |
| UX-DATA-11 | PRESERVE / REQUIRED：竖横聚焦同投影；phase/gap/均值/最高/选中raw anchors按合同保留 | E22-S01/Q01/Q03/Q05；E22-S03/V01及S04/W01 | 固定raw+mandatory anchor独立golden，每segment保留与projection不变性；不拿被测projector生成expected |
| UX-DATA-12 | ADAPT / REQUIRED：session、viewport、focus、selected/scrub、quality展开、返回来源可恢复；不保存Activity | E22-S04/W02/W04/W06；E22-S03/V02/V08、E21-S04/D04 | 真实Activity旋转/显式横屏返回/系统面返回，删除则失效；E18-R12/13窄替代选择页离开后旧勾选恢复，不清掉分析页正常返回语义 |
| AE-UX-01 | PRESERVE / REQUIRED：长pause不压缩canonical轴；用已有结构导航检查被挤窄的active段 | E22-S03/V02/V03；Q05、W01 | 长pause真实时长fixture，whole轴比例与聚焦前后身份一致；禁止删pause或增加自由zoom来绕过 |
| AE-UX-02 | PRESERVE / REQUIRED：每个<=20s虚线规则独立，整场<80%持续banner；图例“虚线期间没有记录” | E22-S03/V04；Q02、K02 | 密集短缺口但总覆盖低于80%的固定分析/投影组合；图能连视觉虚线仍必须显示不足，零/50/80边界不靠取整 |
| AE-UX-03 | PRESERVE / REQUIRED：知情说明列名称、时间、参数；JSON排设备/内部诊断；无内置模型推荐或准确性承诺 | E21-S04/D01；J03/J05 | 实际导出确认与独立字段allowlist；有自定义名称的样例，不能因无MAC就省略个人信息说明 |
| AE-UX-04 | ADAPT / REQUIRED：每场terminal一致point-in-time，整文件完成才交付；失败无App可交付半文件 | E21-S02/P01–P03；S08/H05、S04/D02/D03 | 生成操作内read/IO失败及取消、配置变化/进程中止；E18-R12已排除离页删后旧勾选场景，不为此再加产品流程；外部provider部分副本限制沿D02 |
| AE-UX-05 | PRESERVE / REQUIRED：heartRate始终存在；convenience status为原三轴投影，typed NULL/empty/reasons | E21-S01/J01/J02；E22/K02 | main§7.5每正交组合独立golden，包括overlap时no-eligible优先；不能从一个status逆推丢掉其他轴 |
| AE-UX-06 | ADAPT / REQUIRED：系统分享取消不是错误；恢复来源分析状态/选择页operation，不重复生成重发；横屏无导出入口 | E21-S04/D04；E22-S04/W05/W06、E21-S03/C05 | 真实chooser取消/返回与来源页状态；系统面与主动App内离页两套fixture，按R12/13区分 |
| AE-UX-07 | PRESERVE / REQUIRED：已删session页面明确不存在、返回记录；不继续显示旧read或导出缓存 | E18-S08/H06；E22/V05/W04、E21/D05 | 持久层真实delete→订阅刷新→竖/横界面与导出准入失效；外部已交付副本不被宣称可撤回 |

本子集对账为19个source行、19个primary disposition、0个漏行；只证明该子集索引完整。每行包含的field/union/ordering等具体typed子义务仍须沿main§3–7/D逐项核对；不得把19行算成全工程coverage PASS。反向消费者索引：S07A←03；S06←04/05/08；S08←01/02/06/07/09；E21-S01←10/AE05；E21-S02←AE04；E21-S04←AE03/AE06；E22-S01←11；E22-S03←AE01/AE02；E22-S04←12；AE07通过S08失效同源传给UI/export。表内其余跨Story链接均是direct-consumer义务，不重复认领primary。

补充AD-U-005/Q06：本轮回读salvage原行及Y-axis视觉合同，只规定“按有效bpm动态向外取整、最小可读跨度、不强制0”；该源未给固定tick/跨度数值。因此F.18.2的“既有数值需绑定”不能误读为已经接受某个10/20/40bpm数值。projector应在其正式算法合同中明确可测试显示规则和独立goldens，不能冒称用户选定数值，也不需要让用户重新讨论已接受轴方向。

### F.21 本轮source核对续接记录（未完成项不冒充闭合）

本轮在F.20之后回读同一main技术合同§4.4、§5共同规则/5.1–5.6/5.8–5.8.1、§6.1–6.6及§7.1–7.5，目的是后续逐字段/union ledger；没有改写main或扩大到全仓库Review。关键无损承接点如下，都是既有条款而非新需求：

- planSnapshotJson导出是strict接受的persisted原文string，canonical字节等价与legacy未升级分开；dictionary必须解释该string内的嵌套schema，不能只给外层string一句说明。
- strength的WeightValue显式0合法、NULL表示未记录；fixed/range RepTarget的closed key sets不互混，planned/actual各自保留。HIIT不因此新增次数录入。
- 四family原矩阵共有12/7/5/8行（其中legacy boundary_block_work为三个不同source行），paused保留variant及compositionVersion=2等固定键，其余指定键为显式NULL；不能把整payload置NULL。实际field级mutation账本尚未据此宣称完成。
- timed predicate每个rest分支独立positive-duration；warmup/cooldown/standalone rest不单独解锁间歇切换。composition所有stage group含rounds=1都在真实round loop。
- main§6.6的block-local round/work/rest矩阵明确覆盖两个timed family；力量/跟练的已接受动作/组/阶段导航语义仍保留，后续必须分别绑定其已有phase identity和UI状态，不把timed round字段强套到它们上，也不从仅whole一行声称两模式完整导航已交付。此为待细化消费者表示，不是现在要求用户重新决定是否要这些已接受导航。
- reason presence是required-iff/forbidden矩阵，scope、positive duration/NULL、固定顺序、duplicate及raw/phase attribution须逐行保留。no-eligible可与sample-axis reason并存，top status优先级不能反向删reason。
- 原export exact root的v1包形被D的v2窄替代；内部session/execution/heartRate字段继续继承。normal/no-zone、zero/no-eligible和legacy/recording正交矩阵不得靠一个顶层status替代。

压缩后从这些已读条款继续建立field/variant ledger和准确新v2外层表示，不重读全部source；其后仍需正式生产/test/evidence literal路径与能力核对、全127父索引及其typed子条款双向对账、十维capacity。F.19的recovery测试能力尚需对应正文核对，F.20的19行对账不覆盖整个项目。尚无新用户承重问题、独立审查或READY。

### F.22 Embedded plan、字段空值与四family子义务账本 DRAFT-1

2026-09-06续接同一F7。V2续接前identity为237439 bytes / `B256637E3C6F958A5DFCA7372FBAC5FBCBFE02A495B0625E599542F02E692F82`，与上一保存一致；不运行冷启动/fetch。新增直接细读同一main以下exact来源：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutPlanSnapshotStorageJson.kt`的writer函数100–345及636–649行，兼容decoder只用作现有宽松路径定位。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\PlanSnapshotStorageV1Validator.kt`的required/optional/closed-shape定义与既有单一validator定位。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeOwnerRecoveryTest.kt`的主looper推进、有限窗口、资格失效、scan failure/断开和TOCTOU测试正文。现有`idleFor`直接推进Robolectric主looper，F.19/R1可扩展此真实owner调用路径收集sink receipt；没有运行测试，也不以测试名字证明既有样例已覆盖新cause。

#### F.22.1 两种空值合同不得互相升级

`planSnapshotJson`是导出内的**原始JSON文本string**，并非export serializer新建的普通子object。Canonical plan root的七个keys全部required，其中planId/preferences/followAlong显式NULL；其内部各block/nested shape按已有writer/validator的required+optional规则消费。writer的`jsonObject`会省略Kotlin null成员，现有validator明确接受相应optional缺失。main§5八种storage JSON及§7 export新建object的“nullable key存在且显式null”不得机械套到embedded plan的全部nested keys。

义务PL-NULL-01：S08/H03严格接受各embedded schema合法缺失，但不接受missing required、未知member、错type、默补默认或丢元素；legacy允许旧writer合法省略的root nullable项，不能据此允许任意字段缺失。PL-NULL-02：E21/J01/J03原文逐字节保留，dictionary区分`required_nullable`与`optional_if_not_recorded`，不为了外层统一样式重序列化/填NULL。PL-NULL-03：S08/H03拒绝错payload并保留原row，E21/J05整份失败，history呈typed unavailable；不得用旧toPlanSnapshot(fallbackMode)冒充strict reader。

独立oracle：一份固定合法snapshot每次只移除一个required字段、移除一个合法optional字段、给optional置错误type、加unknown字段；分别断言strict结果及原文identity。Canonical是否接受explicit NULL必须按该具体optional的既有validator判定，不把“optional”自动解读为“也允许explicit NULL”。所有golden在测试端给literal JSON与预期，不由production writer创建自己的expected。

#### F.22.2 Embedded shape→reader与dictionary coverage

下表每行是一个closed-shape义务族，其列出的每个field继续保留各自原type/literal/optional条件；不会用通用“计划信息”覆盖其名字和用途。Primary实现consumer为E18-S08/H03的legacy严格读取（canonical validator为已合并dependency）；E21-S01/J03是每行字典的直接consumer，不另建版本parser authority。字段顺序与canonical表示以既有writer/validator为准。所有行均PRESERVE；root缺version按legacy分支ADAPT。

| shape ID / exact writer函数 | field inventory / closed variant | 独立oracle及下游解释重点 |
|---|---|---|
| PL-01 `WorkoutPlanSnapshot.toStorageJson` | planSnapshotStorageContractVersion、planId、title、mode、blocks、preferences、followAlong | 七key、mode绑定session、v1/legacy/unknown三分；嵌入文本原文不替换为当前plan |
| PL-02 `commonBlockFields` | id、kind、title、order | stable block ID不同于显示名/order；title省略规则不删block |
| PL-03 `WarmupBlock/StretchBlock/CooldownBlock` | common + durationSec、items | 三kind各有fixture；不因空items或可省略duration擅自变成另一kind |
| PL-04 `RestBlock` | common + durationSec、label | planned rest与真实pause/extra-rest分开，label省略不是无rest |
| PL-05 `TimedCircuitBlock` | common + rounds、restBetweenRoundsSec、items | rounds/各item/轮间rest逐个保留，0-based canonical round不同于计划rounds数量 |
| PL-06 `TimedCompositionBlock` | common + compositionVersion、warmupSec、warmupStyle、cooldownSec、cooldownStyle、rounds、restBetweenRoundsSec、restBetweenRoundsStyle、stageGroups、compatibility | 固定v2；canonical不调用normalized改变事实；group/target对象类型和数组顺序沿现有validator，不另加source未要求的非空数量阈值 |
| PL-07 `StrengthExerciseBlock` | common + exerciseId、target、sets、substitutions、setTimerMode | planned exercise、允许替换项与实际替换分别解释；manual_start/auto_after_rest不是历史组是否完成 |
| PL-08 `TimedCompositionStageGroup` | id、order、name、colorHex、iconKey、targets、cueSettings、compatibility | real group身份/数组顺序，显示风格不是统计分类 |
| PL-09 `TimedStageStyle` | colorHex、iconKey | optional shape可无成员，不能default成新颜色并写回原文 |
| PL-10 `TimedCompositionTarget` | id、order、name、kind、durationSec、colorHex、iconKey、cueSettings、autoAdvance、compatibility | action/rest/custom closed kind，planned duration不是实际phase时长 |
| PL-11 `TimedCompositionCompatibilityMeta` | sourceVersion、legacyBlockId、legacyItemId、legacyStageType、convertedAt | 兼容来源是历史元数据，不是新canonical stable identity；不得按旧ID猜新对象 |
| PL-12 `TimedExerciseItem` | id、exerciseId、labelOverride、side、stageType、iconKey、colorHex、workDurationSec、restAfterSec、cueSettings、autoAdvance | item与动作ID分开、未绑定exercise合法不补当前动作库；work/rest/custom等真实literal |
| PL-13 `StrengthExerciseTarget` | weight、repTarget、restAfterSetSec | 计划target与实际Weight/Reps分开；optional target不存在不能合成0 |
| PL-14 `StrengthSetPlan` | id、order、kind、side、targetWeight、repTarget、restAfterSec | warmup/working/drop/backoff；计划setOrder与canonical set indices不同；不得default为working |
| PL-15 `WeightValue` | value、unit，kg/lb | finite non-negative；显式0合法且必须原样保留；无值与无单位整体NULL/省略按所在schema |
| PL-16 `RepTarget` | fixed：kind/reps；range：kind/minReps/maxReps | 两closed union分别positive/negative，1..200且min<=max；禁止fixed带range keys和反向混合 |
| PL-17 `PlanPreferences` | cueSettings、heartRateDisplay | 计划偏好是输入快照，不证明本场已开启HR或通知已交付 |
| PL-18 `CueSettings` | actionEnding、restEnding | nullable/optional分别按writer；计划提示参数不同于采集/样本事实 |
| PL-19 `CountdownCue` | enabled、thresholdSec、soundEnabled、vibrationEnabled、emphasisAnimationEnabled、voiceCueEnabled | 明确boolean与秒；不从true推断实机已经播放声音 |
| PL-20 `HeartRateDisplayPreference` | enabled、showDisconnectedPlaceholder | 仅显示偏好，不是recording intent/covered真值 |
| PL-21 `FollowAlongPlanMeta` | preset、coverMediaId、coachMediaIds、chapterIds、timelineCueIds、musicTrackIds、aiAnalysisProfileId | 数组原样；这些是既有计划引用，文件不承诺带媒体/内置AI或外部可访问资源；不得因字段含ai恢复App AI能力 |

#### F.22.3 Phase variant独立行与predicate消费者

Source为main§6.1–6.4的32个表行；PH-L01..12、PH-C01..07、PH-S01..05、PH-F01..08分别按该源表原顺序逐行绑定，而不是按distinct variant字符串去重（legacy boundary_block_work有三条不同条件）。下表保留可判定fixture身份；required/N/M/literal完整字段值直接引用该exact source行，禁止复制后丢掉限定。

| 独立source行ID | primary Story/AC | fixture必须逐一覆盖 |
|---|---|---|
| PH-L01/L02/L03 | E18-S07A/M01 | warmup/stretch/cooldown的boundary_block_work三支，legacyStageType分别warmup/cooldown/cooldown，item/exercise/round为NULL |
| PH-L04/L05/L06 | E18-S07A/M01 | boundary_item_work、boundary_item_rest、boundary_rest_after_item；item exercise绑定或必须NULL不可互换 |
| PH-L07/L08/L09/L10 | E18-S07A/M01 | circuit_item_work、circuit_item_rest、circuit_rest_after_item、between_round_rest；round必要、rest引用规则各不相同 |
| PH-L11/L12 | E18-S07A/M01 | standalone_rest；paused保留variant且其余七项显式NULL |
| PH-C01 | E18-S07A/M02 | warmup synthetic stage/group/target身份，targetIndex0=0、round/group index=NULL |
| PH-C02/C03/C04 | E18-S07A/M02 | stage_group_action/custom/rest各real group/target及所有required indices；rounds=1仍roundIndex0=0 |
| PH-C05/C06/C07 | E18-S07A/M02 | between_round_rest synthetic且round必要；cooldown无round；paused仍compositionVersion=2，其余指定字段NULL |
| PH-S01/S02/S03/S04 | E19-S01/S01 | prepare/active/confirm/rest四phase除variant/phaseKind外同set完整identity；planned/actual/substituted规则各有一致与冲突fixture |
| PH-S05 | E19-S01/S01 | paused保留variant，其余八项NULL |
| PH-F01/F02 | E19-S02/F01 | circuit/non-circuit action，只有circuit带round；均为follow family |
| PH-F03/F04 | E19-S02/F01 | 两种rest_after_action引用真实action/item/exercise，round条件分别验证 |
| PH-F05/F06/F07/F08 | E19-S02/F01 | between_round_rest、block_rest、boundary、paused的item/exercise/round NULL规则逐项；不借用timed family |

全部PH行的既有`PhaseIdentityV1Validator`是dependency，S08/H07、E21/J01/J03、E22/Q05/V03是直接consumer。每行至少固定合法真实engine/adapter展开输入，再single mutation每个required/M/绑定/版本字段；未知variant、额外key、missing discriminator、wrong family/mode/phase/signature分别是封闭union负向类。该计划不要求重做已合并validator实现，只在新producer/reader消费边界证明其合法输出和拒绝错误。

Predicate单列PF-LW、PF-LR1/LR2/LR3、PF-CW1/CW2、PF-CR1/CR2、PF-N、PF-I：分别绑定main§6.5的legacy work、三个rest、composition action/custom、两个rest、boundary/standalone排除、identity/signature/structure合法性。每个duration独立测试0与positive，不能用另一个positive rest掩盖本支错误。Primary E18-S07A/M03，S08/H07、E22/Q05/V03共用输出；不从HR图像识别HIIT。

Focus单列FI-W、FI-LR、FI-LW、FI-LRest、FI-CR、FI-CW、FI-CRest：展开main§6.6的whole及两timed family的round/work/rest矩阵；phase存在唯一、同session/signature/block/round、true predicate、字段互斥和restore重新验证分别保留。两个block都roundIndex0=0的fixture必须拒绝跨block恢复；invalid restore只回whole，不找相似phase。Primary E18-S07A/M04–M06的identity/predicate结果，E22-S04/W02是Android恢复consumer，不以pure矩阵证明Activity调用。

本节补足PL21个shape族、PH32个source行和PF/FI的定位；**完整PL field级type/optional枚举及formal fixture身份仍需对应原validator进一步核对**，不把族数量当全字段coverage PASS。

### F.23 新v2包装、时间与字典表示合同 DRAFT-1

本节是D已接受字段/语义的可判定序列化草案，不新增产品字段类别、不改变数据库或analysis版本、不冒称已经接受了正式export schema。Primary E21-S01/J01/J03/J04；S08负责可信输入，S02文件与S04系统交付消费同一encoder输出。以下新literal/结构在正式规划接受前保持candidate；不要求Writer自行选择隐含语义。

#### F.23.1 外层、selection与时间表示

| typed义务ID / path | exact shape / required-null / 绑定 | 独立oracle |
|---|---|---|
| EX-ROOT / `$` | 唯一required key `trainFlowSessionExport`，其value为object | 多一个根key、wrong type或missing根拒绝；object文本顺序不是消费者语义 |
| EX-ENV / `$.trainFlowSessionExport` | required exact keys按D顺序：exportContractVersion、generatedAt、displayLocale、displayContractVersion、selection、dataDictionary、sessions；无NULL；版本2、UTC generatedAt含毫秒、display v1 | 单场/多场同一shape；v1 root内session/execution/heartRate三key不能混入v2；未知version整份失败 |
| EX-SESSIONS / `sessions[]` | 每项exact session/execution/heartRate三key、完整terminal数据；至少1场、sessionId唯一，顺序=selection.includedSessionIds；预先全选后取消到0不生成文件 | 双mode/同plan多场/重复ID/空数组/顺序错配分别fixture；不把同plan ID当session ID |
| EX-SEL / `selection` | required exact keys source、startDateInclusive、endDateInclusive、modeFilter、planFilter、includedSessionIds、includedUnknownDateSessionIds、selectionScope；nullable仅两日期与按下行定义的filter；scope固定user_selected_subset | header选择描述与实际场次核对，不从文件中缺日期/缺场推断“当天没训练” |
| EX-SOURCE / `selection.source` | non-null enum `single_session/calendar`；分别标单场入口/批量选择入口 | 两真实入口使用相同encoder，不从sessions数量推断source；calendar可只选1场 |
| EX-DATE / `selection.*DateInclusive` | 两key均存在且同时NULL或同时ISO yyyy-MM-dd；有值start<=end，首尾包含；unknown-date记录可单独选入且在unknown子集列出 | 跨月/闰日/跨年/同一天、倒置/单边NULL；跨午夜整场归冻结开始日；unknown不能因“可选”混入精确date匹配 |
| EX-MODE / `selection.modeFilter` | NULL明确表示没有模式限制；否则为非空、无重复的合法mode string数组；输出按timed/strength/follow_along固定顺序；实际选定场次为过滤后的用户子集 | 全模式/单/多、empty array非法；取消某場不能偷偷改写原filter为另一个意思 |
| EX-PLAN / `selection.planFilter` | NULL表示没有计划限制；否则为非空、无重复的历史planId string数组；按ID字典序输出；过滤不凭当前title重写；无planId场次仍可在无计划限制时逐场选 | 同ID不同历史snapshot、计划改名/删除、planId=NULL；无ID不合成“默认计划ID” |
| EX-IDS / `selection.includedSessionIds` | non-empty string数组、无重复，与sessions逐项同ID；顺序取本次冻结选择，不从serializer重新挑场 | 两数组顺序/数量/内容必须同源；同毫秒开始的两场不合并 |
| EX-UNKNOWN / `selection.includedUnknownDateSessionIds` | string数组，允许empty；为实际included的精确unknown-date子集，按included顺序 | 多列/漏列/非included ID拒绝；旧缺时区信息不能用当前时区伪装已知 |
| EX-TIME / `sessions[].session.timeMetadata` | 所有项required exact keys startLocalDate、startZoneId、startUtcOffsetSeconds、sourceContractVersion、startTimestampBasis、endTimestampBasis、trustedEndAtUtc、trustedEndAtBasis | timeMetadata作为一个新member加入main§7.2 session的原14个member，其内含此八key；原14个member不减，不追加每sample设备墙钟字段 |
| TM-ORIGIN / 前四项 | 新来源1时四项非NULL；无采集证据时四项全NULL；不完整或未知来源version由S08拒绝，不补值 | 0偏移合法、非整小时偏移、历史NULL；不把Room6/export2/analysis1当来源版本 |
| TM-START / startTimestampBasis | closed enum `observed/legacy_stored/unknown`；新真实Start锚点observed；旧持久化值无新来源证据legacy_stored；没有可证明值unknown且startedAt=NULL | 不能仅因timestamp可解析就宣称新观测；不能为legacy构造时区 |
| TM-END / endTimestampBasis | 同三literal；正常新completed/用户结束保存实际观察endedAt；process_interrupted实际结束未知时unknown且endedAt=NULL；旧持久化值legacy_stored | 重启时刻/最后durable写入时刻都不能冒充actual end；重试复用原冻结请求 |
| TM-DERIVED / trustedEndAtUtc与trustedEndAtBasis | basis为`start_anchor_plus_trusted_offset/unavailable`；有可用开始锚点及canonical可信offset时前者，UTC值由checked加法导出；否则后者且值NULL | 原endedAt与derived值可以不同，不用derived覆盖observed；溢出/损坏拒绝，不clamp |
| TM-CLOCK / 字典时间关系 | raw offset是session单调坐标；start+offset是有标记的推导时刻，不能声明每个点额外观测了设备/系统wall clock；canonical排序只用既有tuple | 同offset不同mutation两点、开始后调系统时钟/时区的解释fixture；相对时间不因wall变化回退或重排 |

上述filter的NULL仅为**无筛选限制**，不是“数据未知”；timeMetadata的NULL才按各字段表示未知/不可用。这一区别必须写在文件内字典。planId缺失旧记录仍可在全部计划下逐场勾选，不新增“无计划ID归一化”功能。系统日期变更不搬动历史分组；读取冻结时区/偏移时不得用当前时区规则重算并覆盖原值。

#### F.23.2 文件内字典的closed表示

`dataDictionary` candidate exact keys为`dictionaryContractVersion=1`、`language="zh-CN"`、`pathNotation`、`fields`、`rules`。这是export v2内的解释合同版本，不改变持久化版本。

- `pathNotation`为文件内non-empty string，完整说明：普通字段用JSON路径，`[]`表示数组元素；`::json`表示先将左侧string解析为JSON再访问右侧路径；variant由entry适用条件约束。不得要求消费者打开外部文档理解记号。
- `fields`为entry数组，按`path + condition`固定字典序；每entry required exact keys为`path/name/type/unit/provenance/purpose/condition/presence/nullMeaning/enumValues/relations`。path/name/type/provenance/purpose/condition/presence为non-empty string；unit/nullMeaning为string或显式NULL；enumValues为literal解释对象数组（每项exact `value/meaning`），非enum为空；relations为说明string数组，無关系时为空。`presence`仅`required_non_null/required_nullable/optional_non_null`，embedded schema合法optional缺失的意义写入purpose/condition，不能称它是0。
- `provenance`明确指出用户计划、用户实际输入、平台测量、runtime已接受事实、冻结参数、已绑定派生分析、导出操作描述或字典解释元数据。单个字段同时关联多个来源时用清楚句子逐一注明，不用“系统数据”掩盖计划/实际差异。
- `condition`始终存在；无条件写`always`；union下说明exact discriminator及适用schema/version。enumValues解释每个合法literal，包括nullable分支的含义，不把schema合法但本版runtime未生产的connection_timeout描述为已交付watchdog。
- `rules`为required exact `id/description/relatedPaths`的数组，固定id升序、id唯一。它承载跨字段公式、关联及禁止推断（含同毫秒排序、三个正交轴不可相加、actual/derived wall区别、whole/phase阈值和缺数据≠0），不以每个field重复整套数学制造另一真源。
- 字典解释本身也有固定fields/rules schema说明，通配数组entry只定义一次；不无限递归复制解释。每个实际schema field必须恰好有一个适用entry，每个union每个key的存在性分别可判断；不需要给每个raw点重复同一字典。
- Embedded plan的root/21个PL shape及closed unions必须覆盖；followAlong.media/profile IDs仅解释为当时计划引用，不许外部模型由ID推断未导出的媒体内容或已有内置AI。

本节定义的是可直接实现/审阅的字典表示与覆盖规则，**尚未生成完整dictionary实例或证明独立consumer理解通过**。完整字段实例在E21-S01实现/evidence交付内逐条绑定source，不要求用户手工填写数据词典；formal planning Review仍需从source核对该表示是否足以无歧义承载所有字段。

#### F.23.3 独立consumer与精度oracle

E21/J03的最小独立问题集IC-01..10：仅拿完整导出文件及通用JSON解析器，定位某一场/某点所属session、phase/动作/轮组；区分同秒/同毫秒多样本顺序；区分planned与actual力量值；区分0/NULL/optional缺失；指出某gap属于设备还是用户排除；解释prepare/pause不计主要统计但raw仍在；正确解释no-HR/zero/no-eligible/no-zones；列出实际included子集而不声称未选日期没训练；还原已知开始时区/日期并说明derived时间限制；识别原analysis binding/版本及2500ms/20s/80-50-70各自用途。任何一题需要聊天上下文、当前计划表或外部项目文档才能回答即SELF_DESCRIPTION_GAP。

该oracle可由独立Reviewer用与production encoder分离的解析器和人工预期执行，不以外部AI某次回答“看懂了”作为唯一PASS，不要求联网AI服务或上传真实隐私数据。Fixture使用合成训练记录，不虚构用户的真实训练。

精度负向类NUM-01..06：整数字段不得经过Double往返（用>2^53且仍在Long域的合法identity/tuple合成边界）；显式weight0.0与NULL；合法kg/lb；NaN/Infinity拒绝；小数侵入duration/sequence/index拒绝；越界与checked加法溢出拒绝。BLE raw bpm合法structural域1..65535照原sample合同，不clamp到参数30..260，不宣称为医疗可靠范围。输出无BOM UTF-8、中文转义/换行/引号反斜杠完整，原planSnapshotJson经JSON string解码后的UTF-8与persisted原文相同；外层JSON escaping不等于改写原文。

### F.24 127个父索引逐项处置账本 DRAFT-1

Source universe为已确认V2历史§5指定的V1各表（AS7、CAP24、AD-P19、AD-U25、ARCH27、CT12、RES13），加本次B的明确accepted deltas。这里逐父索引给一个primary classification与child闭合路线，多个Story链接表示父条目内部有不同子义务，**不是让多个Story争夺同一typed obligation**。实现primary依F.4–F.23对应子行；REFERENCE不是重做已合并资产，DEFERRED/EXCLUDE都有B的authority及重访/非触发边界。

| source父ID | 唯一classification | 当前子义务/consumer与authority处置 |
|---|---|---|
| AS-01 | REFERENCE | 既有非HR训练/历史/趋势资产；E18-S09/U06直接回归 |
| AS-02 | REFERENCE | 已合并胶囊/设置/前台恢复；S02/O08、E20新后台范围分开 |
| AS-03 | REFERENCE | CS-03 schema/validators dependency；S01增量，不重跑v5实施 |
| AS-04 | REFERENCE | CS-04A guarded/reconciliation dependency；S03–S05接合 |
| AS-05 | REFERENCE | CS-05 finalizer/original分析唯一owner；S04/S08消费 |
| AS-06 | REFERENCE | CS-04B fresh-process gate已合并；S07B/E19新consumer分别重证 |
| AS-07 | DEFERRED | 04C held/unreviewed/non-main；无当前实现或代码复用授权 |
| E17-CAP-01 | REFERENCE | AS-02；HR不可用仍可训练，S07B/L08、E19回归 |
| E17-CAP-02 | ADAPT | AS-02设置；S06/R01/R03快照→S08→E21/J03 |
| E17-CAP-03 | ADAPT | AS-02前台；S02/O07、E20/B05/B06后台新增 |
| E17-CAP-04 | PRESERVE | S06/R01/R03/R04；S03/B01/B06原子写 |
| E17-CAP-05 | ADAPT | E20-S02/B01–B08；普通唯一协调E20-S01 |
| E17-CAP-06 | REFERENCE | AS-02胶囊；S02/O08不改UI/新鲜度政策 |
| E17-CAP-07 | PRESERVE | S07A/M01/M02、S07B/L03；E19-S01/S01、E19-S02/F01 |
| E17-CAP-08 | PRESERVE | S06/R02/R04、各mode→AS-05；E22/K03/V04 |
| E17-CAP-09 | PRESERVE | S06/R03/R04→S03/B06；user intent独立 |
| E17-CAP-10 | ADAPT | AS-02设备恢复；S02/O07、S06；E22/Q02虚线 |
| E17-CAP-11 | ADAPT | AS-04/06；S07B/L07与E19各Android新consumer |
| E17-CAP-12 | ADAPT | S04/T01–T06；S06/R05/R07终结 |
| E17-CAP-13 | ADAPT | S03/S04生产冻结；S08/H01/H02/H07严格读 |
| E17-CAP-14 | PRESERVE | E22-S02/K01/K02/K04 |
| E17-CAP-15 | PRESERVE | E22-S03/V01/V04/V05；S08同源 |
| E17-CAP-16 | PRESERVE | E22-S01/Q01–Q06、E22-S03/V01/V02 |
| E17-CAP-17 | ADAPT | AS-05数学；S08/H02→E22/V07 |
| E17-CAP-18 | ADAPT | AS-05阈值；E22/K02/V04不评训练效果 |
| E17-CAP-19 | PRESERVE | E22/K01/K02；E21/J02完整no-HR矩阵 |
| E17-CAP-20 | PRESERVE | E22-S03/V05、E22-S04/W01–W06 |
| E17-CAP-21 | PRESERVE | E18-S08/H01–H08→S09/E22 |
| E17-CAP-22 | ADAPT | E21-S01..S04；R05/06/09/11/14/15替代旧batch排除/lease |
| E17-CAP-23 | ADAPT | AS-03 cascade；S08/H06、S09/U04、E21/D05、E22/V05 |
| E17-CAP-24 | PRESERVE | 逐Story matching-layer证据；F7治理consumer，不设末尾补证Story |
| AD-P-001 | PRESERVE | E22/K02/V01/V04，仅本场事实 |
| AD-P-002 | PRESERVE | S07A/M03→E22/Q05/V03，不看曲线判模式 |
| AD-P-003 | PRESERVE | S07A/M03/PF全部分支→E22/V03 |
| AD-P-004 | PRESERVE | E22/V04文案：不因不典型HR曲线否定计划性质 |
| AD-P-005 | PRESERVE | E22/K02/V04只事实，不输出效果/医疗断言 |
| AD-P-006 | PRESERVE | S07A、E19-S01/S01、E19-S02/F01→E22/V03 |
| AD-P-007 | PRESERVE | S08/H02、E22/K02/V04；不足不补数据 |
| AD-P-008 | PRESERVE | E21/J02、E22/K01/K02两种状态分开 |
| AD-P-009 | DEFERRED | RES-05/06/07，用户新决定与隐私/研究门禁后重访 |
| AD-P-010 | PRESERVE | E21/J03与D01，RES-11外部工作流 |
| AD-P-011 | DEFERRED | R02→RES-01/02；不恢复App比较 |
| AD-P-012 | DEFERRED | R02→RES-01；旧四窗口非当前实现 |
| AD-P-013 | ADAPT | 参数不可变→S06/R01/R03；跨场兼容部分随R02暂缓 |
| AD-P-014 | DEFERRED | R02→RES-03；不新建停训分析 |
| AD-P-015 | EXCLUDE | RES-10，未经新产品决定不做自动配平/推荐 |
| AD-P-016 | DEFERRED | R02→RES-04；力量实际输入S02保留不等于交付进阶 |
| AD-P-017 | PRESERVE | S06/R02/R04、E22/K03；pause≠extra-rest |
| AD-P-018 | REFERENCE | AS-05精确阈值；S08/H02与E22/K02/V04直接消费 |
| AD-P-019 | ADAPT | E21/D05，单/批统一，所有terminal可选 |
| AD-U-001 | PRESERVE | E22/K01/K04，S08同源 |
| AD-U-002 | PRESERVE | S04可信终结→E22/K01文案 |
| AD-U-003 | PRESERVE | S06/R03→E22/Q01/V01，全程late-enable空白 |
| AD-U-004 | PRESERVE | S08/H02→E22/V04，不造unknown宽度百分比 |
| AD-U-005 | PRESERVE | E22/Q06、F20最后一段数值未被用户固定 |
| AD-U-006 | PRESERVE | E22/Q01/Q03，raw与display分开 |
| AD-U-007 | PRESERVE | E22/V06 |
| AD-U-008 | PRESERVE | E22/V07，excluded不进100%条分母 |
| AD-U-009 | PRESERVE | E22/V02/V03，RES-09自由zoom仍暂缓 |
| AD-U-010 | PRESERVE | E22/W01 |
| AD-U-011 | PRESERVE | E22/W05 |
| AD-U-012 | PRESERVE | E22/W02/W06 |
| AD-U-013 | PRESERVE | E22/Q02→V02图例/未记录tooltip |
| AD-U-014 | PRESERVE | E22/V08，结构化质量默认折叠 |
| AD-U-015 | PRESERVE | E22/K02/V04/V08 |
| AD-U-016 | PRESERVE | E22/V09 |
| AD-U-017 | PRESERVE | S08/H02→E22/V09 |
| AD-U-018 | PRESERVE | E22/V10/V02 |
| AD-U-019 | PRESERVE | E22/V03；PL/PH/PF/FI身份consumer |
| AD-U-020 | PRESERVE | E22/V02 |
| AD-U-021 | PRESERVE | E22/V02/V05、W04 |
| AD-U-022 | PRESERVE | E21/J01、D01–D05；旧lease由R11/15窄替代 |
| AD-U-023 | PRESERVE | F20七AE逐条primary，不当一条笼统完成 |
| AD-U-024 | PRESERVE | E22/K01/K03，独立卡及两种休息 |
| AD-U-025 | PRESERVE | E22/K03/V11、Q03 raw tooltip |
| E17-ARCH-01 | ADAPT | E22-S01纯projector与S03/S04页面state边界 |
| E17-ARCH-02 | ADAPT | E22/Q01/Q03，segment+mandatory全部保留 |
| E17-ARCH-03 | ADAPT | E22-S03/S04唯一分析页面状态owner |
| E17-ARCH-04 | ADAPT | E22/W01/W03/W04，同Activity/方向政策 |
| E17-ARCH-05 | ADAPT | S06 canonical clock；S07B/E19真实Start，S01时间承载 |
| E17-ARCH-06 | ADAPT | AS-04/06已合并；S07B/E19 process消费者 |
| E17-ARCH-07 | ADAPT | AS-03正交存储；S03生产phase/recording写入 |
| E17-ARCH-08 | ADAPT | S06唯一排序；S02 BLE、mode engine、S03 DB分工 |
| E17-ARCH-09 | ADAPT | AS-03/05原analysis；S08绑定读取，不重分析 |
| E17-ARCH-10 | ADAPT | AS-05终结；S04外层事务/执行payload接合 |
| E17-ARCH-11 | ADAPT | AS-05 sample validity数学；S08原结果、E22视觉20s分开 |
| E17-ARCH-12 | ADAPT | E19-S01/S03；E22 raw保留/tooltip |
| E17-ARCH-13 | ADAPT | AS-05阈值整数；E22消费不重算 |
| E17-ARCH-14 | ADAPT | AS-05加权数学；S08/E21精度原值 |
| E17-ARCH-15 | ADAPT | AS-05最高四字段；S08/E22/Q01/Q03 |
| E17-ARCH-16 | ADAPT | AS-03/05三轴；E21/J02、E22/K02 |
| E17-ARCH-17 | ADAPT | S06/R01/R03/R04；S03/B06初启/off-on |
| E17-ARCH-18 | ADAPT | S02/O07、F19所有cause；S03沿原validator |
| E17-ARCH-19 | ADAPT | S02/O07/F19，matrix保留，无生产watchdog |
| E17-ARCH-20 | ADAPT | S03/B04与S06有序事实；AS-05 audit派生 |
| E17-ARCH-21 | ADAPT | S07A/E19身份producer；S08/H07冻结resolver |
| E17-ARCH-22 | ADAPT | F22 PH32行；四family/pause/extra-rest语义 |
| E17-ARCH-23 | ADAPT | S03/B04、S07A/M04、E19/S01替换metadata |
| E17-ARCH-24 | ADAPT | AS-03 header/sample字段；S02规范化输出→E21/J05隐私 |
| E17-ARCH-25 | ADAPT | S07B/L01–L08、E19各真实lifecycle；D5取代旧entry假设 |
| E17-ARCH-26 | ADAPT | E21/P05与E22/Q04；无产品时长上限/无损 |
| E17-ARCH-27 | ADAPT | E21/J01–J05、F23字典，original绑定不回退 |
| CT-01 | ADAPT | AS-03/04/05/06依赖，S01增量、S03/S04、S08消费者 |
| CT-02 | ADAPT | S08/H03、F22/PL，v1依赖+legacy strict reader |
| CT-03 | ADAPT | AS-03/05八JSON依赖；S08/H02 dispatch、E21/J01/J02逐字段 |
| CT-04 | ADAPT | F22/PH32行→S07A与E19两mode各producer |
| CT-05 | ADAPT | F22/PF/FI→S07A、S08/H07、E22/Q05/V03 |
| CT-06 | ADAPT | main§7+R09/17/D/F23→E21/J01–J05 |
| CT-07 | ADAPT | R11/R15替代十分钟lease/boot，新E21/P01/P02/P04与D02–D04 |
| CT-08 | ADAPT | AS-05已合并证据不重算；E22/Q04与E21/P05新候选证据 |
| CT-09 | ADAPT | S02/O01–O08、S03/B01–B08、S06/R01–R04，F19细分 |
| CT-10 | ADAPT | S04/T01–T06、S05/G01–G17、S06终结，不恢复旧04C聚合 |
| CT-11 | ADAPT | S05/S06 barrier与S07B/E19 Android调用分层证明 |
| CT-12 | ADAPT | S08/H05/H06、E21/P04/D04；普通训练无TTL/降raw |
| RES-01 | DEFERRED | R02暂缓App跨场比较，未来新用户决定才重访 |
| RES-02 | DEFERRED | R02暂缓可比性算法；现存快照/版本数据继续保存 |
| RES-03 | DEFERRED | R02暂缓App停训分析；外部AI数据工作流保留 |
| RES-04 | DEFERRED | R02暂缓力量进阶提示；不得从effort保留推断已解锁 |
| RES-05 | DEFERRED | App AI未来；须新产品owner/隐私同意/费用决定 |
| RES-06 | DEFERRED | 自动上传未来；须用户新授权与云账号/删除设计 |
| RES-07 | DEFERRED | 医疗future research；须临床/责任/法规研究 |
| RES-08 | DEFERRED | 新来源/设备SDK暂缓；真实需求/设备证据后重访 |
| RES-09 | DEFERRED | 自由zoom暂缓；既有语义聚焦确有不足后重访 |
| RES-10 | EXCLUDE | 自动肌群配平/推荐不做，必须新产品决定 |
| RES-11 | REFERENCE | 用户主动导出后外部AI；不控制结论 |
| RES-12 | ADAPT | R04–06已采纳批量→E21四Story，不再DEFERRED |
| RES-13 | DEFERRED | 完整进程续跑未来；必须三mode engine/命令/通知完整新设计 |

机械对账：127行/127唯一ID。此数是父索引完整性，不能取代原source全文的modality、field/union、state/error与consumer语义核对。E18-R01..18是后续delta，UX-DATA12/AE-UX7和PL/PH/PF/FI等为父条目展开，分别保持自身账本，不能混加成“新增几百项需求”。

反向读取规则：从每个Story AC回到表内指向它的父ID，再回F.19–F.23及引用main/D5/salvage的exact子条款；若一条新AC没有此source链或B的明确delta则为ORPHAN_PROPOSAL，不自动采纳。每个deferred/excluded项不得生成生产功能、App数据上传、比较/进阶、恢复engine或低空间监控；现有资产直接回归不因此被删除。父账本机械一致不是独立Planning Review PASS。

### F.25 S08容量细分与消费者重接 DRAFT-3

F7展开PL21个schema族及strict transaction/identity/resolver后，原S08存在三种可独立验证和审阅的state boundary：legacy外部JSON严格接受、Room一致terminal graph读取、历史显示/结构解析。它们不是同一个原子事务内不可分割的三步；当前同repo facade的架构不要求一个Writer一次同时首次交付三种边界。原S08记为`SPLIT_REQUIRED`，普通capacity拆分沿C既有owner，不增加页面、功能或第二数据源。

| 新候选 | 因果完整old→new与scope | 原H义务primary / 直接consumer | 能独立完成的证据 |
|---|---|---|---|
| E18-S08A 旧计划快照严格接受 | 现有legacy fallback decoder不能提供可信输入→由已指定LegacyUnversionedPlanSnapshotReader严格接受旧writer合法原文，保留无version；复用已有JSON/shape合同，不在调用层伪造v1 | H03的plan部分、H04的legacy parse失败；F22/PL-NULL及PL01–21。S08B调用它，E21 dictionary引用该schema | 原文literal正反/field/union/mode/version矩阵、完整元素计数与无回写；pure production parser边界，无Room migration/UI/graph finalizer |
| E18-S08B 一致的严格记录读取 | repository通用宽松读取→同一Room读事务获取terminal graph/执行字段/冻结时间及exact original binding，调用原validator和S08A，返回typed真实结果；列表只读必要header | H01/H02/H04 persisted graph与执行字段/H05/H06/H08；H03 nonterminal分类。S08C、S09、E21、E22共用同一repository结果 | 真Room并发读/删除/绑定/排序和corrupt fixture；不同版本snapshot不能fallback latest；不存在不返回旧cache；不是puredecoder测试替代DB |
| E18-S08C 冻结历史显示与结构解析 | 尚无可信统一历史label/导航结果→从S08B frozen输入解析旧名称及结构，调用S07A唯一timed predicate/focus，不查当前计划/动作库 | H07完整；AD-U-017历史稳定的display consumer、PL/PH/PF/FI消费者。S09、E21输出display、E22使用同一解析结果 | 独立四family frozen数据、同ID计划后来改名、各合法resolved/unresolved schema分支、多block同round/无样本；无Room mutation/第二read model cache |

S08C仍是C已经接受的无状态resolver责任，不新增独立仓库、registry或中心manager；repository facade可组合它，消费者不直接重读另一个数据源。S08A的common parsing复用只在既有strict职责内实现，不让legacy结果调用“补version1”办法穿过canonical byte-identity验证。S08B不拥有显示label回退算法。

新的精确AC沿用对应H编号并在新Story内按分支限定：S08A/H03/H04，S08B/H01/H02/H03-terminal/H04/H05/H06/H08，S08C/H07。其他章节旧`S08/Hxx`均是历史合称，必须按本表重定向，不能将S08保留为第25个隐藏节点或重复派发。F24父索引的classification不变，只改变实现child路由。

Read失败与display unresolved分层：unknown/corrupt plan/header/graph不能为展示label而被降级成valid terminal export；resolver的`unresolved_missing_metadata/unresolved_invalid_metadata/unresolved_invalid_identity/unsupported_identity_version`是main§7.3允许的显示分类，不自动证明它们在每一种严格读取成功结果都production可达。正式证据逐分支标明真实可达性，schema合法但已被前层拒绝的分支不制造production bypass；UI应消费typed unavailable，E21对未知version/坏graph仍整体失败。

新增候选的base生产literal定位（未创建/未授权实施）：

- S08A：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\LegacyUnversionedPlanSnapshotReader.kt`，及F.22已列`PlanSnapshotStorageV1Validator.kt`/`WorkoutPlanSnapshotStorageJson.kt`的必要strict复用；不全局替换旧编辑器decoder。
- S08B：F.6现有`WorkoutSessionRepository.kt`及两DAO；typed读取合同可留同一repo文件，不新增read coordinator。必要时间语义校验仍沿S03选定的唯一validator由S08B调用。
- S08C：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionHistoricalResolver.kt`，调用F.11的`TimedCanonicalFactsV1.kt`，不复制predicate或修改已合并分析。

以上是规划新文件名的明确literal声明，不是从任务ID生成evidence文件，更不是在Integration执行写入。未来正式handoff必须将允许工作区完整literal路径绑定到用户实际指定的Story worktree；当前仍仅V2可写。

本拆分不改变E18完成值：计时记录必须经过完整读取与历史入口后才算E18交付。不会把力量/跟练或E20后台提前塞入E18，也不会由于多拆两个Story增加用户操作。其他候选capacity仍需十维结果，不能由此宣布已完成全体capacity。

### F.26 候选DAG DRAFT-3与机械边界

S08拆分并补齐F.34时间validator前置后的唯一当前标签图：24节点、42条唯一边、无self/unknown endpoint、可拓扑排序。此前24/41、F.13的22/36及更早21/34保留作历史，不再作为当前count。仍是planning标签图，无未来implementation SHA、不等于已满足前置或READY。

| 节点 | 直接前置候选 |
|---|---|
| E18-S01 | —（已合并资产） |
| E18-S02 | —（已合并资产） |
| E18-S03 | E18-S01、E18-S02 |
| E18-S04 | E18-S01 |
| E18-S05 | E18-S02、E18-S03、E18-S04 |
| E18-S06 | E18-S05 |
| E18-S07A | —（已合并资产） |
| E18-S07B | E18-S06、E18-S07A |
| E18-S08A | —（已合并资产） |
| E18-S08B | E18-S01、E18-S03、E18-S04、E18-S08A |
| E18-S08C | E18-S08B、E18-S07A |
| E18-S09 | E18-S07B、E18-S08B、E18-S08C |
| E19-S01 | E18-S09 |
| E19-S02 | E18-S09 |
| E20-S01 | E18-S07B、E19-S01、E19-S02 |
| E20-S02 | E20-S01、E18-S06 |
| E21-S01 | E18-S08B、E18-S08C |
| E21-S02 | E21-S01 |
| E21-S03 | E18-S09、E19-S01、E19-S02 |
| E21-S04 | E21-S02、E21-S03 |
| E22-S01 | E18-S08B、E18-S08C、E18-S07A |
| E22-S02 | E18-S09、E19-S01、E19-S02 |
| E22-S03 | E22-S01、E22-S02 |
| E22-S04 | E22-S03 |

S08B输出真实一致graph，S08C输出同源显示/结构；S09/E21-S01/E22-S01同时消费两者，所以拆分后各明确两条边。E22-S01保留对S07A predicate的直接来源依赖。五Epic用户顺序仍E18→E19→E20→E21→E22；图上的技术根可独立核对，不授权改变用户成果验收次序或提前派发后Epic。

### F.27 文件生命周期、权限与最终证据责任 DRAFT-1

#### F.27.1 统一导出文件的最少状态（E21-S02/P01–P04、S04/D02–D04）

采用一个既有export capability内部的私有文件状态；不建立export Room表、后台任务、lease/provider owner或跨文件manager。候选实现可用三个专用子目录分别表达未完成、未交付完整、可能已交付文件，避免为了保留24h重新实现旧十分钟lease。目录都是App filesDir下专用导出范围；FileProvider只暴露可能交付的完整JSON目录，不暴露数据库/整个filesDir/临时文件。普通session数据仍无TTL。

| transition ID / 状态 | 原子性、错误与用户结果 | 独立生产oracle |
|---|---|---|
| FL-01 IDLE→WRITING | 冻结operation/selection/generatedAt；单个本operation coroutine按场读取与流式编码，重复点不另开相同operation；无完整JSON前不创建可授予URI | 真实strict repository→encoder→文件；独立read回，不只看存在 |
| FL-02 WRITING→READY | 全部session已处理，结束JSON、flush/file-sync/close、完整stream验证后才同卷rename为完整ready；validation不得删点或改raw | 每个真实可失败IO边界保留原error；校验失败无ready，旧ready不被覆盖 |
| FL-03 WRITING失败/用户App内离页取消 | 取消producer并停止后续场读取，先关闭资源再清本operation未完成文件；若清理亦失败保留主失败与次失败，不能声称已删 | 数据库/文件实际失败；用户取消不同于失败提示；下次清理残留；无新低空间流程 |
| FL-04 READY→SAVE_PENDING | 用户通过系统CreateDocument取得URI后，将完整ready文件复制给provider；cancel/NULL URI保持无成功，可重试或离页清理本地未交付ready | 真系统provider，包括open/write/flush/close失败；部分外部副本不能谎称已回收 |
| FL-05 SAVE_PENDING→SAVED | provider写入/flush/close均成功才显示已保存，随后清理App副本；如果最终结果未送达因进程死则下次不冒认成功/不自动重发 | 真实返回与文件字节identity，保存结果和App清理结果分开；无需新持久化导出列表 |
| FL-06 READY→SHARE_DISPATCH_PENDING | 在交给系统前，将该完整文件转到保留区并记录分享尝试时刻；完成该私有状态后才构造只读URI授予/发送chooser | 精确先后order与crash切点；状态未可靠记录则不交付；文件名/目录状态不含用户计划名或设备标识 |
| FL-07 已可能交付→RETAINED | 系统交付可能发生后的不确定结果、chooser取消或回App均不立刻删对方可能持有的副本；只称已交给系统分享，不称接收方保存成功 | chooser/外部reader/返回/中断；无自动再发；若确认根本未调用平台，可按未交付清理 |
| FL-08 RETAINED cleanup | 记录的分享尝试时刻超过24h后，在下次App启动或生成导出前清理专用保留文件；未到24h不得按普通未完成残留清除 | 固定时间、两种trigger、未到/恰好/超过24h；用户wall clock跳变按best-effort而不承诺真实时间定点删除；不重建boot lease |
| FL-09 process restart | 清未完成/未交付ready残留；保留区按24h策略，不恢复operation/勾选、不重发；不触碰用户保存到外部的文件 | 真进程中止后私有目录状态与页面结果；文件系统状态可证明分类，不靠旧ViewModel内存 |
| FL-10 backup/provider scope | 专用导出目录排除云备份及device transfer；FileProvider exported=false，grantUriPermissions=true，只读临时grant；无write/persistable URI权限，不能开未完成文件 | manifest/backup XML精确scope+真实外部读取；不能以source搜索代替平台授权效果 |

FL-06的“可能交付”提前记录是B的保守不确定交付规则实现；不恢复600000ms new-open承诺。候选最少表示固定为App `filesDir/session-exports/`下的`incomplete/ready/shared`三目录：未完成文件只在incomplete，验证通过同卷rename进ready；分享尝试前同卷rename进shared，shared basename同时携带该次尝试的UTC epoch毫秒和不含用户信息的随机operation标识。由文件位置与basename恢复分类/清理基准，不建立sidecar manifest、lease数据库或仅在ViewModel存布尔量。分享时间不是JSON generatedAt，不改变已完成JSON字节；保存走ready→系统URI。FileProvider只映射shared子目录，provider authority使用`${applicationId}.sessionexports`。

这是App运行时文件命名合同，不是主管理artifact路径，也不授权派生本地Review/evidence文件。专用目录由同一export文件实现持有，清理只处理符合自身命名合同的普通文件，不递归跟随路径链接、不删外部保存副本。表示无法可靠写入时不交付；不能用catch-all假装完成。当前未创建任何目录/文件/provider或安装依赖。24h基准按设备UTC wall clock作best-effort cleanup，系统时间变化导致实际保留时长差异不被冒称成精确lease；本节未接受新的严格物理时钟保证。

#### F.27.2 时间元数据唯一校验点

S03/B07拥有仓库边界四项时间完整性/来源语义的单一校验定义；S08B/H05调用该同一定义。它可以位于现有repository文件的内部实现，不增加独立管理owner，不改CS-03原canonical v1语义或把legacy四NULL判坏。S01仅存储承载；mode Start实际捕获值；E21仅编码与解释。不得因为两消费者都需要校验就在两个Route各复制逻辑。

#### F.27.3 证据分类与final source要求不能漏掉

- S01/S03/S04/S05/S08B：真实SQLite/Room schema、事务、guard、readback、ordering、rollback；S02真实production owner+Robolectric平台回调；S06合流/clear使用production API和真实coroutine/Room。不得为省测试增加production hook或由fake成功结果替代。
- S07B与E19各自：Activity-retained实际入口、配置保留、真正onCleared同步barrier及两阶段进程证据；Recorder直接clear测试不证明framework调用。计时、力量、跟练互不代替实际consumer evidence。两阶段force-stop/relaunch由外部测试编排控制，不能在被kill的instrumentation里声称后半段仍执行。
- E20：普通notification和connectedDevice FGS分别有平台证据；B09–B11包括release未确认、独立generation与visibility/consumer矩阵；B12–B15固定shared-owner observer→measurement APK/M1→final freshness→final APK及受影响gate重跑。Band9未断链保持与断链新attempt恢复各自有final-source证据，AVD不能代替Band/RF。E20-S02承担此完整链，不能只填写通用identity schema或推到E22；Manifest只增加必要FOREGROUND_SERVICE/FOREGROUND_SERVICE_CONNECTED_DEVICE和非exported Service，Service不新增BLE/engine owner。
- E21-S02/P05：完整数据库读取到流式文件验证的时间/PSS窗口，单场和100场同总量；E22-S01/Q04：pure projector窗口。类名沿main§9.3的`E17ExportPerformanceContractTest`与`E17ProjectorPerformanceContractTest`，新Epic编号不授权改名后偷换计量。已有`E17FinalizerPerformanceContractTest`为已合并资产。
- main§9.4原“最终整合candidate重建、安装并重跑三项performance contract”的最终source要求没有被用户决定删除。当前把它放在**E22成果最终整合验收门禁**（由最后交付的acceptance package明确承接），不新增末尾代码Story、不让末尾测试替代前面各Story证据。最后源码影响相关测量时需finalizer/projector/export三项同final source证据；源码/APK/installed identity、原指定AVD/JDK与测量规则保持。不得仅拼接三个旧candidate PASS。
- UI主观视觉/可访问性实际使用、真实Band和外部保存/分享结果沿人工门禁；自动检查只能证明各自客观前置。当前没有开始任何测试/build/ADB/AVD/实机或独立Review。evidence文件路径必须在后续手工根模板中由主管理明确literal声明，不从Story/Role/Attempt派生；本轮只在V2记录合同。

### F.28 当前候选生产与测试literal定位补齐（base路径，不是派发授权）

以下新文件均为**拟新增、未创建、未读取任何04C副本**；已存在路径只沿同一main读取定位。为避免重复长路径导致第二套边界，S01/F4、S02/F5+F19、S03/F6、S04/F7、S05/F8、S06/F9、S07A/B/F11、S08A/B/C/F25、S09/F12已有完整base路径继续适用。此节补足此前仅有类名的路径；不把源文件数量用作capacity结果。每条测试只能证明列明边界。

#### F.28.1 E18直接新增测试与实际生命周期

- S02 observation：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\health\HeartRateRuntimeObservationTest.kt`；mapper：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\CanonicalHeartRateObservationMapperTest.kt`。两者名称沿D5，不沿旧12-path恢复04C范围；平台与recovery真实调用仍用F19已有测试。
- S05实际gate/事务证据复用现有 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionRecorderGuardedWriteTest.kt` 和 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionRecorderReconciliationTest.kt`；S04 outer transaction还可扩展现有 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionFinalizerTest.kt` 的真实Room fixture，不修改已合并数学算法。
- S06：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionTimelineRecorderTest.kt`；串行、receipt、terminal和clear生产编排，不能在此声称Activity已调用clear。
- S07A：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\workoutsession\TimedCanonicalFactsV1Test.kt`；F22/PH/PF/FI及existing engine真实展开。FI精确focus需要补明确`M07 Focus identity` AC，不把它错误塞到只管实际phase轨迹的M04或执行结果的M05；M07由本节明确新增为既有main§6.6的下游义务，无新产品行为。
- S07B生产：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionViewModel.kt`；pure retained状态测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionViewModelTest.kt`；实际Activity：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\workoutsession\TimedWorkoutSessionLifecycleContractTest.kt`。
- S08A：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\LegacyUnversionedPlanSnapshotReaderTest.kt`；S08B复用F4的WorkoutSessionRepositoryTest；S08C：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionHistoricalResolverTest.kt`。
- S09实际历史接线：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\history\WorkoutHistoryContractTest.kt`；已有HistoryUiStateTest只证明纯呈现，不冒充Activity/Room生命周期。

#### F.28.2 E19两个真实consumer

- 力量production ViewModel：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\StrengthWorkoutSessionViewModel.kt`；事实转换沿既有WorkoutSessionRecordMappers.kt及已列StrengthRoute/engine；测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\workoutsession\StrengthWorkoutSessionViewModelTest.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\workoutsession\StrengthWorkoutSessionLifecycleContractTest.kt`。
- 跟练production ViewModel：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\workoutsession\FollowAlongWorkoutSessionViewModel.kt`；事实转换沿既有WorkoutSessionRecordMappers.kt及FollowAlongRoute/TimedWorkoutEngine；测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\workoutsession\FollowAlongWorkoutSessionViewModelTest.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\workoutsession\FollowAlongWorkoutSessionLifecycleContractTest.kt`。

#### F.28.3 E20通知与FGS

- 唯一ordinary coordinator继续F15两个existing core/notifications文件及 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\app\TrainFlowApplication.kt` 和已列三mode consumer；测试复用 `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\notifications\ActiveWorkoutNotificationContractsTest.kt`。
- connectedDevice Service候选：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\health\WorkoutHeartRateService.kt`；Manifest已列。实际Service/foreground/notification测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\core\health\WorkoutHeartRateServiceContractTest.kt`。Band/M1报告仍需届时同candidate真设备identity，不能用此测试文件替代。
- B09–B11修正同一F15 coordinator/Service/Application的handoff发布权与visibility接线；直接consumer沿既有 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/main/java/com/liujyks/trainflow/app/ProcessVisibilityTracker.kt`、`C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/main/java/com/liujyks/trainflow/core/health/HeartRateRuntimeOwner.kt` 及F.5/F.19已列observation/cause与F.9 Recorder边界。只消费原事实，不让Tracker拥有cleanup策略；不加通知interface或诊断owner。
- B12–B14测量与final边界：复用并窄适配 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`；必要debug声明/入口为 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/debug/AndroidManifest.xml`、`C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/debug/java/com/liujyks/trainflow/app/DebugEntryActivity.kt`。当前base的该Activity仅无BLE说明页，未实现observer；新增debug观察不重做生产胶囊，不自动开放诊断产品功能。真实观察仍只取同Application owner，不得创建或复制独立GATT工具。
- final阈值及边界测试分别为 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/main/java/com/liujyks/trainflow/core/health/HeartRateFreshnessPolicy.kt` 和 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/test/java/com/liujyks/trainflow/core/health/HeartRateFreshnessPolicyTest.kt`；若需要APK内debug-only source identity，只能窄改 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/build.gradle.kts` 对应debug field，不增依赖或改变production策略。当前代码3000/2500 ms标为provisional；本修正未把它们宣布final。上述路径仅规划delta定位，尚未授权实施或设备执行。

#### F.28.4 E21同一导出capability

- encoder/dictionary：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionExport.kt`；独立golden/mutation/consumer测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionExportTest.kt`。Golden literal可放此测试资源内，future handoff若使用外部fixture文件必须逐路径列明，不用生产encoder生成expected。
- 文件生命周期：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\WorkoutSessionExportFiles.kt`；真实文件状态测试：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\WorkoutSessionExportFilesTest.kt`；性能：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\core\data\E17ExportPerformanceContractTest.kt`。
- 唯一页面状态/选择/系统交付consumer：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\WorkoutSessionExportViewModel.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\WorkoutSessionExportRoute.kt`，连接已列HistoryRoute/MainActivity/TrainFlowApp。S03交selection，S04追加真实系统handoff，同一ViewModel不复制两套operation owner。
- UI/platform验证：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\history\WorkoutSessionExportContractTest.kt`；provider与backup配置：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\res\xml\session_export_paths.xml`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\res\xml\session_export_backup_rules.xml`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\res\xml\session_export_data_extraction_rules.xml`及已列Manifest。只排除专用导出目录，不全局关闭用户其他数据备份。

#### F.28.5 E22投影与单场复盘

- 纯projector：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\data\HeartRateChartProjector.kt`；test：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\core\data\HeartRateChartProjectorTest.kt`；性能：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\core\data\E17ProjectorPerformanceContractTest.kt`。
- 独立HR卡：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\WorkoutSessionHeartRateCard.kt`，接既有结束/历史UI，不另建analysis。
- 唯一分析状态与竖/横Route：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\WorkoutSessionAnalysisViewModel.kt`、`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\feature\history\WorkoutSessionAnalysisRoute.kt`。横屏只增同一页面模式/恢复，不新增横屏Activity或数据owner。
- 状态test：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\test\java\com\liujyks\trainflow\feature\history\WorkoutSessionAnalysisViewModelTest.kt`；实际UI/lifecycle/accessibility：`C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\androidTest\java\com\liujyks\trainflow\feature\history\WorkoutSessionAnalysisContractTest.kt`。主观视觉仍需要绑定candidate的用户验收，不从文件存在/截图像素count宣布通过。

所有Story均允许且仅允许为其直接consumer在上述已命名Application/Activity/shell/Route追加必要接线；不能因共享文件在多个候选出现而取得整文件任意改写权。当前没有新的实际Story worktree、candidate、evidence artifact，故这些base定位不是可复制执行的DEV根模板，也不解锁Writer。

### F.29 持久化→导出typed子合同与oracle账本 DRAFT-1

本节消费main§3–7已冻结定义，不再抄DDL/数学常量作为第二authority。每个named field及每个closed-object member的type/presence/literal直接绑定其source表行/Exact keys块；以下将**不同种类的不变量**分别编号与primary消费者，避免用“完整JSON测试”遮住差异。既有validator实现为AS-03/05 dependency，新consumer必须调用它们，不能因有本账本重写算法。

| 子义务ID / exact source | 分类与normalized要求 | primary Story/AC→consumer | 独立oracle类 |
|---|---|---|---|
| DB-ID / §3.1–3.6各PK/FK及§4.2 | REFERENCE：session/recording/sample/snapshot各自stable ID，跨row只能同session/recording；同value或digest不能替代ID | S03/B01/B05、S04/T01→S08B/H01/H02 | 新producer写后按PK读取；错parent/重复PK/同payload异ID，不能只比hash |
| DB-HEADER / §4.1/4.3 | REFERENCE：legacy/canonical/active/terminal/version七列组合，partial header不降级legacy | S08B/H01/H04→E21/J01、E22/K02 | 五状态与版本组合原fixture逐source条件，unknown/corrupt typed失败、不回写 |
| DB-START / §4.2.2/4.2.3、D5§4.2 | ADAPT：全Start batch同cut；phase/recording/acquisition/sample与metadata对应；结果未确定不认absent | S03/B01–B03→S05/G/Recorder | 真实Room全提交/回滚+相同semantic request不同payload冲突，fixed rows而非mock |
| DB-ORDER / §3.2/3.4/3.5、§4.2.3 | REFERENCE：显式DAO排序；sample按offset/mutation/sample，phase/acquisition按sequence；same-ms允许有序多个事实 | S08B/H01/H08→E21/J02、E22/Q03 | 故意非插入顺序的真实rows，断言production读取顺序；测试端不得先sort掩盖DAO遗漏 |
| DB-PARTITION / §4.2.2/§5.7 | REFERENCE：真实相邻interval连续、唯一open/terminalclosed；intent、phase、device正交、合法零时长不丢 | S03/B04/B05→S06；S08B/H01 | 真实close/open事务前后比较，gap/overlap/duplicate/open marker single mutation |
| DB-ORIGINAL / §3.6/§4.2/§7.4 | REFERENCE：originalVersion精确绑定同recording snapshot与inputLastMutation；不是latest | S08B/H02→E21/J01、E22/K02 | 同recording第二版本、错误recording binding、缺snapshot、tuple漂移；不重算修复 |
| DB-TERMINAL / D5§4.3/§5.5–5.8 | ADAPT：完整terminal请求identity/payload、no-HR、exact重试与清理/RELEASED分开 | S04/T01–T06、S05/G→S06/模式UI | 完整row graph、changed endedAt/执行值冲突、结果取消、迟到clear与新token并发 |
| JS-CLOSED / §5共同规则、§7.1 | PRESERVE：八storage对象/export新对象每一个key presence/type/literal；known-version extra/missing/错NULL失败；embedded plan例外按F22 | S08B/H02/H04调用原validators；E21/J01/J05 | 每named member single mutation；错误定位须指出object path/member及原typed失败，不能吞成not_recorded |
| JS-DISPLAY / §5.1 | REFERENCE：display metadata stableId唯一、首次引用排序、active append-only、terminal不可变 | S03/B04、S07A/M04、E19-S01/S01→S08C/H07 | 重复/改旧entry/终态追加拒绝；实际替换动作与original计划label分别固定 |
| JS-ZONE / §3.3/§5.2 | REFERENCE：六zone固定order/ID/bounds，effective与recording typed columns同源，alert非第七zone | S08B/H02→E21/J01/J03、E22/V07/V09 | 每zone上下界/顺序/缺项mutation、effective mismatch、no-effective max，原NULL不补默认 |
| JS-PHASE / §5.3/§6 | REFERENCE/consumer：七key envelope、signature bytes/digest与payload shape/version、mode/phase/plan binding分别成立 | S07A/M01/M02/M06/M07、E19两phaseproducer→S08C/E21 | F22 PH32+PF/FI、unknown/mismatch、sameblock round；hash正确但ID错仍失败 |
| JS-CONFIG / §5.4 | REFERENCE：17个analysisConfig named keys全部Required非NULL且exact literal；版本/2500/5000/7000/8000等不互换 | S08B/H02→E21/J01/J03、E22/K02 | 独立固定config及逐member单改；不读取production常量生成expected |
| JS-ZONE-SUM / §5.5 | REFERENCE：六zone duration exact和=covered；unavailable或eligible0整对象NULL | S08B/H02→E21/J02、E22/V07 | 和差1、某zone负数、可用却NULL、不可用却对象、zero-denominator overlap |
| JS-PHASE-AGG / §5.6 | REFERENCE：primary phase aggregates唯一有序、whole和/积分一致，各phase最高引用本phase raw；无per-phase zones | S08B/H02→E21/J02、E22/V03/V04 | extra excluded aggregate、漏primary、顺序重复、crossphase highest、extra zones键、whole sum mismatch |
| JS-DURATION / §5.7 | REFERENCE：whole/intent/phase/primary partition八个列明方程，12 state和14reason key全存在，未出现写0，reason和允许小于window | S08B/H02→E21/J02/J03、E22/K03/V04 | 每方程独立±1；device axes被相加当互斥partition应失败；显式0与遗漏不同 |
| JS-QUALITY / §5.8/5.8.1 | REFERENCE：14code每个session/phase required-iff/forbidden、duration positive/NULL、真实phase绑定、固定order/无duplicate | S08B/H02→E21/J01/J02、E22/K02/V08 | 每code正/反/scope/duration单改；no-eligible与sample reason并存，不能被top status抹掉 |
| EX-SESSION / §7.2+D.3 | ADAPT：原14字段+timeMetadata，types/null/terminalreason/storage原文/display保持；实际unknown endedAt由R07窄替代 | E21/J01/J04、F23/TM→S04/D05 | canonical completed/abandoned/process_interrupted与legacy各literal golden；原14key逐missing/extra/type mutation |
| EX-STEP / §7.3 sessionStepRecords | PRESERVE：11字段全部输出，kind为9个原literal，nullable hints/实际与计划时长不改；parsed start再stepId排序 | E21/J01/J02/J03；S08B执行strict前置 | nullable真实缺失/显式NULL规则、不同timezone文本同Instant排序、skipped boolean，hints不生成phase |
| EX-PHASE / §7.3 phases/display | PRESERVE：8phase keys、4display keys，terminal无open end，locale绑定root、resolved iff合法非空label条件，其余NULL；机器identity不丢 | E21/J01，S08C/H07→E22 | display status/label错配、wrong locale、phase排序重复/未closed；strict失败不能为伪label绕过 |
| EX-EXTENSION / §7.3 timedRestExtensions | PRESERVE：14字段；added/planned>0、cumulative>=added；brownfield roundIndex1不是canonical roundIndex0；排序4-key | E21/J01/J02，S07A/M05→S08B | 每正值/nullable/index/排序边界，增加rest与pause不能合并；既有原值逐字段检查 |
| EX-STRENGTH / §7.3 strengthSetRecords | PRESERVE：14字段、WeightValue、RepTarget closedunion；actual 0合法、NULL未填；effort form_breakdown保留；notes排除 | E21/J01/J02/J05，E19/S02→S08B | NUM及PL15/16、same setOrder tie recordId、planned vs actual、自定义note隐私negative |
| EX-HR / §7.4/7.5 | PRESERVE：HR始终六key；recording17key、acquisition9key、sample4key、originalAnalysis22key；status、originalbinding及durationAudit同原事实 | E21/J01/J02/J03，S08B→系统文件 | 逐key/type/版本/排序/NULL、active recording/缺end/缺binding拒绝；counts不因display downsample变小 |
| EX-ORTHOGONAL / §7.5 | PRESERVE：legacy/no-HR合法、canonical/no-HR合法、canonical/recording合法、legacy/recording失败；无eligible优先zero，normal+nozone专属状态 | E21/J02/J04，E22/K02 | 四timeline-HR组合及7status全部，eligible0+samples0重叠与only-excluded，前置异常不降级成status |
| EX-PRECISION / §7.1/§7.3、R09/10 | PRESERVE：integer/finite decimal、0/NULL、单位、same-ms排序原值；JSON文本escaping不变embedded内容 | E21/J02/J03/F23 NUM | 独立精确整数parser、>2^53合法tuple、0重量、NaN/Infinity拒绝、中文/控制字符goldens |
| EX-LIFETIME / R11–15、CT07/12 | ADAPT：F27 FL01–10单场consistent读取/完整文件/只读系统权限/24hcleanup；旧lease排除 | E21-S02/P01–P04、S04/D01–D05 | 真Room/文件/platform三层分别证明；不以fake chooser成功代替外部读取 |

补充精确member数量只是机械交叉检查，不是验收代替物：EX-HR的recording为17、acquisition为9、sample为4、originalAnalysis为22；JS-CONFIG为17。任何后续source核对发现计数误差先修正索引，字段权威始终为同一main exact keys，不能为匹配计数删field。每个“逐member mutation”在实现证据中有明确fixture索引并返回可定位错误；无需为受信任compiler内部不可能状态制造生产防御分支。

这里完成当前已读取§3–7关键**typed义务族**的primary/oracle路由；尚不能声称每个源子条款都已形成完整独立fixture清单。尤其完整dataDictionary实例、S08C各resolver分支production可达性、各具体真实IO故障如何被现有测试边界触发，仍需在正式规划接受前检查。此限制继续阻止F7完成与READY，不用表格较长冒充闭合。

### F.30 PL类型/存在性闭合与图表consumer细化 DRAFT-1

#### F.30.1 PL存在性/类型的可判定矩阵

本轮继续读过PlanSnapshotStorageV1Validator各canonical函数、required/optional keys与literal集合。以下补齐F22的PL字段模式；每个required字段缺失、每个optional字段缺失/explicit NULL/错误type分别有单字段mutation，不新增违反既有writer的限制。全部nested optional **若出现必须为所列非NULL类型**；仅PL01的三个root nullable字段允许显式NULL。Legacy root另外允许这三key省略、没有version；nested仍只接受旧writer实际合法shape，不补当前默认值。

| PL / variant | required非NULL字段类型 | optional非NULL字段类型及局部规则 |
|---|---|---|
| PL01 canonical root | version=1 integer；title:string；mode合法enum；blocks:object数组 | planId:string或显式NULL，preferences:PL17或NULL，followAlong:PL21或NULL，三key仍required；legacy缺version分类不合成1 |
| PL02 common | id:nonempty string；kind:对应block enum；order:非负integer | title:string；不要求string必须非空，不能把空title换“未命名” |
| PL03 boundary三kind | common + items:PL12数组 | durationSec:非负integer；无items元素不能被mapNotNull静默形成空数组 |
| PL04 rest | common + durationSec:非负integer | label:string |
| PL05 circuit | common + rounds:positive integer；items:PL12数组 | restBetweenRoundsSec:非负integer |
| PL06 composition | common + compositionVersion=2；warmupSec/cooldownSec/restBetweenRoundsSec:非负integer；rounds:positive integer；stageGroups:PL08数组 | warmupStyle/cooldownStyle/restBetweenRoundsStyle:PL09；compatibility:PL11 |
| PL07 strength | common + exerciseId:nonempty string；sets:PL14数组；substitutions:nonempty string元素数组；setTimerMode=manual_start/auto_after_rest | target:PL13 |
| PL08 group | id:nonempty string；order:非负integer；name:string；colorHex:nonempty string；targets:PL10数组 | iconKey:nonempty string；cueSettings:PL18；compatibility:PL11 |
| PL09 style | 无required member，object本身非NULL | colorHex/iconKey:nonempty string；空object合法 |
| PL10 target | id:nonempty string；order/durationSec:非负integer；name:string；kind=action/rest/custom；colorHex:nonempty string；autoAdvance:boolean | iconKey:nonempty string；cueSettings:PL18；compatibility:PL11 |
| PL11 compatibility | 无required member，object非NULL | sourceVersion=legacy_timed_circuit/composition_v2；legacyBlockId/legacyItemId/convertedAt:string；legacyStageType=warmup/work/rest/cooldown/custom；不把convertedAt仅string的历史规则擅自加成强制RFC3339 |
| PL12 item | id/iconKey/colorHex:nonempty string；stageType=warmup/work/rest/cooldown/custom；workDurationSec:非负integer；autoAdvance:boolean | exerciseId:nonempty string；labelOverride:string；side=both/left/right/alternating；restAfterSec:非负integer；cueSettings:PL18 |
| PL13 strength target | 无required member，object非NULL | weight:PL15；repTarget:PL16；restAfterSetSec:非负integer；空object不补默认 |
| PL14 set | id:nonempty string；order:非负integer；kind=warmup/working/drop/backoff | side=both/left/right/alternating；targetWeight:PL15；repTarget:PL16；restAfterSec:非负integer |
| PL15 weight | exact value:nonnegative finite number；unit=kg/lb | 无optional key；0合法，未知unit失败 |
| PL16 fixed | exact kind=fixed；reps:integer 1..200 | 无range字段 |
| PL16 range | exact kind=range；minReps/maxReps:integer 1..200且min<=max | 无fixed reps字段 |
| PL17 preferences | 无required member，object非NULL | cueSettings:PL18；heartRateDisplay:PL20 |
| PL18 cues | 无required member，object非NULL | actionEnding/restEnding:PL19 |
| PL19 countdown | exact enabled/soundEnabled/vibrationEnabled/emphasisAnimationEnabled/voiceCueEnabled:boolean；thresholdSec:非负integer | 无optional字段；不从开关值推断实际播放成功 |
| PL20 HR display | exact enabled/showDisconnectedPlaceholder:boolean | 无optional字段；不等于recording启用 |
| PL21 follow metadata | preset:boolean；coachMediaIds/chapterIds/timelineCueIds/musicTrackIds:string元素数组 | coverMediaId/aiAnalysisProfileId:string；不添加没有source依据的非空ID或外部资源访问要求 |

PL root重复/unknown member、wrongmode、非1version、wrongblockkind属于其已有strict错误分类；既有PlanSnapshotStorageV1Validator为canonical唯一判定，S08A接受legacy使用同一shape含义而非旧fallback consumer。计划描述性字段不进入orderedStructureSignature的规则继续由已合并OrderedStructureSignatureInputV1提供；consumer不得把完整plan文本hash当结构signature，也不能用正确signature忽略stable ID错配。

当前PL类型/存在性已从source展开为上述矩阵，F22末尾“仍需逐validator核对PL类型”的断点由本节窄替代。具体golden字节/每个mutation case的测试实现是S08A/E21的实现交付，当前没有创建fixture/test文件、没有宣称测试PASS。

#### F.30.2 非timed导航与restore不套用timed Focus v1

E22/V03、W02消费者细化：timed继续使用S07A/M07的main§6.6 focus contract；strength/follow的动作/组/阶段导航直接以已验证phaseSequence定位叶节点，父层级从该phase的frozen family payload取block/set/item/round identity。力量至少以blockId及set identity区分同名/同exercise重复块；跟练以blockId/item及可有round区分重复动作。已接受的按动作/组/阶段查看保留，不给两个模式伪造timed true-work/rest toggle或新round UUID。

SavedState只保留sessionId、family、结构signature、所选有效phaseSequence及view/展开/scroll/scrub语义；恢复重新从S08B/C验证它们，再派生父层级。失效回whole，不跨block找相似动作。仍是唯一分析ViewModel状态，不新增通用selection manager，不改持久化phase/focus version，不给导出强加UI状态字段。

#### F.30.3 projector显示规则的证据限界

Y轴的具体nice ticks/min span应在projector中作为确定性纯显示算法，由固定输入→ticks/bounds golden证明；无需新用户决定，但不能把source没有的数值冒称已接受。所有raw点、可见主要训练均线/最高锚点必须在有效绘图范围内；当前focus不允许为了显示局部均值再产生第二份analysis。范围裁剪与标签碰撞只能影响绘制，不改canonical phase宽度或raw。无自由zoom/平滑/新增医疗阈值。

mandatory anchors及首/min/max/尾分别按实际连续段处理；同一raw点兼多个anchor可共享显示点，但不能因此删其anchor语义。20s只连接同phase中原合同允许的视觉短缺口，scrub虚线区明确未记录，统计2500ms仍完全独立。raw point匹配同毫秒按canonical tuple，不用“nearest显示降采样点”代替。

### F.31 main§7直接member逐字段ledger（不含嵌套schema的计数）

从同一immutable main§7的Exact keys逐object提取，再按F29约束与D的v2窄替代绑定。下表127个**直接member行**与F24的127个**父索引**是不同集合、数量恰好相同，禁止混用。每行primary为E21-S01/J01/J03，类型/精度还受J02，输入consumer边界为S08B/C；type、required/nullable/forbidden、enum及跨字段条件由列出的exact source及F29 named规则提供完整任务正文，不复制成不同版本。实现证据逐行有missing/extra/wrong-type/错误NULL及适用binding/order单改定位；同一production getter生成expected不算独立oracle。

| field义务ID | v2文件内字段路径 | exact类型/存在性/值域source与typed规则 |
|---|---|---|
| EF-session-sessionId | `sessions[].session.sessionId` | main§7.2 session；EX-SESSION |
| EF-session-planId | `sessions[].session.planId` | main§7.2 session；EX-SESSION |
| EF-session-mode | `sessions[].session.mode` | main§7.2 session；EX-SESSION |
| EF-session-terminalStatus | `sessions[].session.terminalStatus` | main§7.2 session；EX-SESSION |
| EF-session-terminalReason | `sessions[].session.terminalReason` | main§7.2 session；EX-SESSION |
| EF-session-startedAt | `sessions[].session.startedAt` | main§7.2 session；EX-SESSION |
| EF-session-endedAt | `sessions[].session.endedAt` | main§7.2 session；EX-SESSION+D.1/C.1 |
| EF-session-timelineStatus | `sessions[].session.timelineStatus` | main§7.2 session；EX-SESSION |
| EF-session-timelineVersion | `sessions[].session.timelineVersion` | main§7.2 session；EX-SESSION |
| EF-session-trustedEndOffsetMs | `sessions[].session.trustedEndOffsetMs` | main§7.2 session；EX-SESSION |
| EF-session-canonicalSessionDurationMs | `sessions[].session.canonicalSessionDurationMs` | main§7.2 session；EX-SESSION |
| EF-session-planSnapshotStorageContractVersion | `sessions[].session.planSnapshotStorageContractVersion` | main§7.2 session；EX-SESSION |
| EF-session-planSnapshotJson | `sessions[].session.planSnapshotJson` | main§7.2 session；EX-SESSION |
| EF-session-displayMetadata | `sessions[].session.displayMetadata` | main§7.2 session；EX-SESSION |
| EF-execution-sessionStepRecords | `sessions[].execution.sessionStepRecords` | main§7.3 execution；EX-STEP/EX-PHASE/EX-EXTENSION/EX-STRENGTH |
| EF-execution-phases | `sessions[].execution.phases` | main§7.3 execution；EX-STEP/EX-PHASE/EX-EXTENSION/EX-STRENGTH |
| EF-execution-timedRestExtensions | `sessions[].execution.timedRestExtensions` | main§7.3 execution；EX-STEP/EX-PHASE/EX-EXTENSION/EX-STRENGTH |
| EF-execution-strengthSetRecords | `sessions[].execution.strengthSetRecords` | main§7.3 execution；EX-STEP/EX-PHASE/EX-EXTENSION/EX-STRENGTH |
| EF-step-stepId | `sessions[].execution.sessionStepRecords[].stepId` | main§sessionStepRecords[]；EX-STEP |
| EF-step-kind | `sessions[].execution.sessionStepRecords[].kind` | main§sessionStepRecords[]；EX-STEP |
| EF-step-blockId | `sessions[].execution.sessionStepRecords[].blockId` | main§sessionStepRecords[]；EX-STEP |
| EF-step-itemId | `sessions[].execution.sessionStepRecords[].itemId` | main§sessionStepRecords[]；EX-STEP |
| EF-step-setPlanId | `sessions[].execution.sessionStepRecords[].setPlanId` | main§sessionStepRecords[]；EX-STEP |
| EF-step-exerciseId | `sessions[].execution.sessionStepRecords[].exerciseId` | main§sessionStepRecords[]；EX-STEP |
| EF-step-startedAt | `sessions[].execution.sessionStepRecords[].startedAt` | main§sessionStepRecords[]；EX-STEP |
| EF-step-endedAt | `sessions[].execution.sessionStepRecords[].endedAt` | main§sessionStepRecords[]；EX-STEP |
| EF-step-skipped | `sessions[].execution.sessionStepRecords[].skipped` | main§sessionStepRecords[]；EX-STEP |
| EF-step-actualDurationSec | `sessions[].execution.sessionStepRecords[].actualDurationSec` | main§sessionStepRecords[]；EX-STEP |
| EF-step-plannedDurationSec | `sessions[].execution.sessionStepRecords[].plannedDurationSec` | main§sessionStepRecords[]；EX-STEP |
| EF-phase-sequence | `sessions[].execution.phases[].sequence` | main§phases[]；EX-PHASE |
| EF-phase-startOffsetMs | `sessions[].execution.phases[].startOffsetMs` | main§phases[]；EX-PHASE |
| EF-phase-endOffsetMs | `sessions[].execution.phases[].endOffsetMs` | main§phases[]；EX-PHASE |
| EF-phase-startMutationSequence | `sessions[].execution.phases[].startMutationSequence` | main§phases[]；EX-PHASE |
| EF-phase-endMutationSequence | `sessions[].execution.phases[].endMutationSequence` | main§phases[]；EX-PHASE |
| EF-phase-phaseKind | `sessions[].execution.phases[].phaseKind` | main§phases[]；EX-PHASE |
| EF-phase-phaseIdentity | `sessions[].execution.phases[].phaseIdentity` | main§phases[]；EX-PHASE |
| EF-phase-display | `sessions[].execution.phases[].display` | main§phases[]；EX-PHASE |
| EF-display-displayContractVersion | `sessions[].execution.phases[].display.displayContractVersion` | main§7.3 phases.display；EX-PHASE |
| EF-display-locale | `sessions[].execution.phases[].display.locale` | main§7.3 phases.display；EX-PHASE |
| EF-display-resolutionStatus | `sessions[].execution.phases[].display.resolutionStatus` | main§7.3 phases.display；EX-PHASE |
| EF-display-label | `sessions[].execution.phases[].display.label` | main§7.3 phases.display；EX-PHASE |
| EF-extension-recordId | `sessions[].execution.timedRestExtensions[].recordId` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-stepId | `sessions[].execution.timedRestExtensions[].stepId` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-stepIndex0 | `sessions[].execution.timedRestExtensions[].stepIndex0` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-roundIndex1 | `sessions[].execution.timedRestExtensions[].roundIndex1` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-restStageId | `sessions[].execution.timedRestExtensions[].restStageId` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-restStageTitle | `sessions[].execution.timedRestExtensions[].restStageTitle` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-previousStageId | `sessions[].execution.timedRestExtensions[].previousStageId` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-previousStageTitle | `sessions[].execution.timedRestExtensions[].previousStageTitle` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-addedSec | `sessions[].execution.timedRestExtensions[].addedSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-plannedRestSec | `sessions[].execution.timedRestExtensions[].plannedRestSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-restElapsedBeforeExtensionSec | `sessions[].execution.timedRestExtensions[].restElapsedBeforeExtensionSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-extensionAtRemainingSec | `sessions[].execution.timedRestExtensions[].extensionAtRemainingSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-cumulativeExtraRestSec | `sessions[].execution.timedRestExtensions[].cumulativeExtraRestSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-extension-eventElapsedSec | `sessions[].execution.timedRestExtensions[].eventElapsedSec` | main§timedRestExtensions[]；EX-EXTENSION |
| EF-set-recordId | `sessions[].execution.strengthSetRecords[].recordId` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-exerciseId | `sessions[].execution.strengthSetRecords[].exerciseId` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-sourceSetPlanId | `sessions[].execution.strengthSetRecords[].sourceSetPlanId` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-setOrder | `sessions[].execution.strengthSetRecords[].setOrder` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-setKind | `sessions[].execution.strengthSetRecords[].setKind` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-side | `sessions[].execution.strengthSetRecords[].side` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-plannedWeight | `sessions[].execution.strengthSetRecords[].plannedWeight` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-plannedRepTarget | `sessions[].execution.strengthSetRecords[].plannedRepTarget` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-actualWeight | `sessions[].execution.strengthSetRecords[].actualWeight` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-actualReps | `sessions[].execution.strengthSetRecords[].actualReps` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-activeDurationSec | `sessions[].execution.strengthSetRecords[].activeDurationSec` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-actualRestAfterSec | `sessions[].execution.strengthSetRecords[].actualRestAfterSec` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-effort | `sessions[].execution.strengthSetRecords[].effort` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-set-substitutedFromExerciseId | `sessions[].execution.strengthSetRecords[].substitutedFromExerciseId` | main§strengthSetRecords[]；EX-STRENGTH |
| EF-hr-status | `sessions[].heartRate.status` | main§7.4 heartRate；EX-HR |
| EF-hr-recording | `sessions[].heartRate.recording` | main§7.4 heartRate；EX-HR |
| EF-hr-intentAndAcquisition | `sessions[].heartRate.intentAndAcquisition` | main§7.4 heartRate；EX-HR |
| EF-hr-samples | `sessions[].heartRate.samples` | main§7.4 heartRate；EX-HR |
| EF-hr-originalAnalysis | `sessions[].heartRate.originalAnalysis` | main§7.4 heartRate；EX-HR |
| EF-hr-durationAudit | `sessions[].heartRate.durationAudit` | main§7.4 heartRate；EX-HR |
| EF-recording-recordingId | `sessions[].heartRate.recording.recordingId` | main§recording；EX-HR |
| EF-recording-status | `sessions[].heartRate.recording.status` | main§recording；EX-HR |
| EF-recording-startedOffsetMs | `sessions[].heartRate.recording.startedOffsetMs` | main§recording；EX-HR |
| EF-recording-startedMutationSequence | `sessions[].heartRate.recording.startedMutationSequence` | main§recording；EX-HR |
| EF-recording-endedOffsetMs | `sessions[].heartRate.recording.endedOffsetMs` | main§recording；EX-HR |
| EF-recording-endedMutationSequence | `sessions[].heartRate.recording.endedMutationSequence` | main§recording；EX-HR |
| EF-recording-sourceContractVersion | `sessions[].heartRate.recording.sourceContractVersion` | main§recording；EX-HR |
| EF-recording-sourceKind | `sessions[].heartRate.recording.sourceKind` | main§recording；EX-HR |
| EF-recording-acquisitionContractVersion | `sessions[].heartRate.recording.acquisitionContractVersion` | main§recording；EX-HR |
| EF-recording-parameterSnapshotVersion | `sessions[].heartRate.recording.parameterSnapshotVersion` | main§recording；EX-HR |
| EF-recording-age | `sessions[].heartRate.recording.age` | main§recording；EX-HR |
| EF-recording-personalMaxBpm | `sessions[].heartRate.recording.personalMaxBpm` | main§recording；EX-HR |
| EF-recording-effectiveMaxBpm | `sessions[].heartRate.recording.effectiveMaxBpm` | main§recording；EX-HR |
| EF-recording-effectiveMaxSource | `sessions[].heartRate.recording.effectiveMaxSource` | main§recording；EX-HR |
| EF-recording-alertThresholdBpm | `sessions[].heartRate.recording.alertThresholdBpm` | main§recording；EX-HR |
| EF-recording-zoneSnapshot | `sessions[].heartRate.recording.zoneSnapshot` | main§recording；EX-HR |
| EF-recording-originalAnalysisVersion | `sessions[].heartRate.recording.originalAnalysisVersion` | main§recording；EX-HR |
| EF-acquisition-sequence | `sessions[].heartRate.intentAndAcquisition[].sequence` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-startOffsetMs | `sessions[].heartRate.intentAndAcquisition[].startOffsetMs` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-startMutationSequence | `sessions[].heartRate.intentAndAcquisition[].startMutationSequence` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-endOffsetMs | `sessions[].heartRate.intentAndAcquisition[].endOffsetMs` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-endMutationSequence | `sessions[].heartRate.intentAndAcquisition[].endMutationSequence` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-recordingIntent | `sessions[].heartRate.intentAndAcquisition[].recordingIntent` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-intentReason | `sessions[].heartRate.intentAndAcquisition[].intentReason` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-deviceState | `sessions[].heartRate.intentAndAcquisition[].deviceState` | main§intentAndAcquisition[]；EX-HR |
| EF-acquisition-deviceReason | `sessions[].heartRate.intentAndAcquisition[].deviceReason` | main§intentAndAcquisition[]；EX-HR |
| EF-sample-sampleSequence | `sessions[].heartRate.samples[].sampleSequence` | main§samples[]；EX-HR |
| EF-sample-offsetMs | `sessions[].heartRate.samples[].offsetMs` | main§samples[]；EX-HR |
| EF-sample-mutationSequence | `sessions[].heartRate.samples[].mutationSequence` | main§samples[]；EX-HR |
| EF-sample-bpm | `sessions[].heartRate.samples[].bpm` | main§samples[]；EX-HR |
| EF-analysis-analysisVersion | `sessions[].heartRate.originalAnalysis.analysisVersion` | main§originalAnalysis；EX-HR |
| EF-analysis-createdAt | `sessions[].heartRate.originalAnalysis.createdAt` | main§originalAnalysis；EX-HR |
| EF-analysis-inputLastMutationSequence | `sessions[].heartRate.originalAnalysis.inputLastMutationSequence` | main§originalAnalysis；EX-HR |
| EF-analysis-analysisConfig | `sessions[].heartRate.originalAnalysis.analysisConfig` | main§originalAnalysis；EX-HR |
| EF-analysis-sampleStatus | `sessions[].heartRate.originalAnalysis.sampleStatus` | main§originalAnalysis；EX-HR |
| EF-analysis-coverageStatus | `sessions[].heartRate.originalAnalysis.coverageStatus` | main§originalAnalysis；EX-HR |
| EF-analysis-zoneStatus | `sessions[].heartRate.originalAnalysis.zoneStatus` | main§originalAnalysis；EX-HR |
| EF-analysis-status | `sessions[].heartRate.originalAnalysis.status` | main§originalAnalysis；EX-HR |
| EF-analysis-canonicalSampleCount | `sessions[].heartRate.originalAnalysis.canonicalSampleCount` | main§originalAnalysis；EX-HR |
| EF-analysis-primaryPointSampleCount | `sessions[].heartRate.originalAnalysis.primaryPointSampleCount` | main§originalAnalysis；EX-HR |
| EF-analysis-eligibleDurationMs | `sessions[].heartRate.originalAnalysis.eligibleDurationMs` | main§originalAnalysis；EX-HR |
| EF-analysis-coveredDurationMs | `sessions[].heartRate.originalAnalysis.coveredDurationMs` | main§originalAnalysis；EX-HR |
| EF-analysis-coverageBasisPoints | `sessions[].heartRate.originalAnalysis.coverageBasisPoints` | main§originalAnalysis；EX-HR |
| EF-analysis-weightedBpmMs | `sessions[].heartRate.originalAnalysis.weightedBpmMs` | main§originalAnalysis；EX-HR |
| EF-analysis-observedAvgBpm | `sessions[].heartRate.originalAnalysis.observedAvgBpm` | main§originalAnalysis；EX-HR |
| EF-analysis-observedMaxBpm | `sessions[].heartRate.originalAnalysis.observedMaxBpm` | main§originalAnalysis；EX-HR |
| EF-analysis-highestOffsetMs | `sessions[].heartRate.originalAnalysis.highestOffsetMs` | main§originalAnalysis；EX-HR |
| EF-analysis-highestMutationSequence | `sessions[].heartRate.originalAnalysis.highestMutationSequence` | main§originalAnalysis；EX-HR |
| EF-analysis-highestSampleSequence | `sessions[].heartRate.originalAnalysis.highestSampleSequence` | main§originalAnalysis；EX-HR |
| EF-analysis-zoneDurations | `sessions[].heartRate.originalAnalysis.zoneDurations` | main§originalAnalysis；EX-HR |
| EF-analysis-phaseAggregates | `sessions[].heartRate.originalAnalysis.phaseAggregates` | main§originalAnalysis；EX-HR |
| EF-analysis-qualityReasons | `sessions[].heartRate.originalAnalysis.qualityReasons` | main§originalAnalysis；EX-HR |

每行的forward链是source exact member→EF行→F29规则→S08B/C可信输入→E21/J01/J02/J03→独立encoder/golden/consumer证据→S02/S04完整文件；reverse按同一EF ID返回，不只按关键字搜索称覆盖。Weight/RepTarget的nested union沿PL15/16和main§7.3；八storage子对象沿F29 JS族及下一字段表；phase.payload逐variant沿PH source行。F23新v2外层/time/dictionary字段作为accepted语义的candidate表示单独列账，未因旧member数字而省略。

### F.32 八storage对象的嵌套字段ledger

Source为main§5.1–5.8的closed JSON及其后完整规则，以下逐字段列出其在export中的位置。数组同shape字段只定义一次，zone六种fixed bounds/顺序和quality不同reason条件并未因此合并，分别由JS-ZONE/JS-QUALITY及原required-iff矩阵承接。示意JSON中出现0/null不代表该字段唯一值或唯一类型，不能把示意值当golden；完整类型/存在性看exact source规则。

Primary为S08B/H02的既有validator dispatch/绑定consumer，E21/J01/J02/J03无损输出/字典为direct consumer，E22只读typed结果。每SF行按F29规则生成独立member/type/presence以及适用cross-field mutation；既有AS-03/05 validator本身不重新实现。EF引用子object，SF补该object内部member，不重复认领同一field。

| field义务ID | v2内字段路径 | 完整source/typed规则 |
|---|---|---|
| SF-display-displayMetadataContractVersion | `sessions[].session.displayMetadata.displayMetadataContractVersion` | main§5.1；JS-DISPLAY |
| SF-display-entries | `sessions[].session.displayMetadata.entries` | main§5.1；JS-DISPLAY |
| SF-display-entries[].entityKind | `sessions[].session.displayMetadata.entries[].entityKind` | main§5.1；JS-DISPLAY |
| SF-display-entries[].stableId | `sessions[].session.displayMetadata.entries[].stableId` | main§5.1；JS-DISPLAY |
| SF-display-entries[].displayNameAtFirstReference | `sessions[].session.displayMetadata.entries[].displayNameAtFirstReference` | main§5.1；JS-DISPLAY |
| SF-display-entries[].customNameAtFirstReference | `sessions[].session.displayMetadata.entries[].customNameAtFirstReference` | main§5.1；JS-DISPLAY |
| SF-display-entries[].resolutionSource | `sessions[].session.displayMetadata.entries[].resolutionSource` | main§5.1；JS-DISPLAY |
| SF-zone-zoneSnapshotContractVersion | `sessions[].heartRate.recording.zoneSnapshot.zoneSnapshotContractVersion` | main§5.2；JS-ZONE |
| SF-zone-unit | `sessions[].heartRate.recording.zoneSnapshot.unit` | main§5.2；JS-ZONE |
| SF-zone-effectiveMaxBpm | `sessions[].heartRate.recording.zoneSnapshot.effectiveMaxBpm` | main§5.2；JS-ZONE |
| SF-zone-effectiveMaxSource | `sessions[].heartRate.recording.zoneSnapshot.effectiveMaxSource` | main§5.2；JS-ZONE |
| SF-zone-zones | `sessions[].heartRate.recording.zoneSnapshot.zones` | main§5.2；JS-ZONE |
| SF-zone-zones[].zoneId | `sessions[].heartRate.recording.zoneSnapshot.zones[].zoneId` | main§5.2；JS-ZONE |
| SF-zone-zones[].lowerBoundBasisPointsInclusive | `sessions[].heartRate.recording.zoneSnapshot.zones[].lowerBoundBasisPointsInclusive` | main§5.2；JS-ZONE |
| SF-zone-zones[].upperBoundBasisPointsExclusive | `sessions[].heartRate.recording.zoneSnapshot.zones[].upperBoundBasisPointsExclusive` | main§5.2；JS-ZONE |
| SF-phaseid-phaseIdentityContractVersion | `sessions[].execution.phases[].phaseIdentity.phaseIdentityContractVersion` | main§5.3；JS-PHASE |
| SF-phaseid-family | `sessions[].execution.phases[].phaseIdentity.family` | main§5.3；JS-PHASE |
| SF-phaseid-payloadVersion | `sessions[].execution.phases[].phaseIdentity.payloadVersion` | main§5.3；JS-PHASE |
| SF-phaseid-mode | `sessions[].execution.phases[].phaseIdentity.mode` | main§5.3；JS-PHASE |
| SF-phaseid-phaseKind | `sessions[].execution.phases[].phaseIdentity.phaseKind` | main§5.3；JS-PHASE |
| SF-phaseid-orderedStructureSignature | `sessions[].execution.phases[].phaseIdentity.orderedStructureSignature` | main§5.3；JS-PHASE |
| SF-phaseid-orderedStructureSignature.signatureContractVersion | `sessions[].execution.phases[].phaseIdentity.orderedStructureSignature.signatureContractVersion` | main§5.3；JS-PHASE |
| SF-phaseid-orderedStructureSignature.algorithm | `sessions[].execution.phases[].phaseIdentity.orderedStructureSignature.algorithm` | main§5.3；JS-PHASE |
| SF-phaseid-orderedStructureSignature.digestHexLowercase | `sessions[].execution.phases[].phaseIdentity.orderedStructureSignature.digestHexLowercase` | main§5.3；JS-PHASE |
| SF-phaseid-payload | `sessions[].execution.phases[].phaseIdentity.payload` | main§5.3；JS-PHASE |
| SF-config-analysisConfigContractVersion | `sessions[].heartRate.originalAnalysis.analysisConfig.analysisConfigContractVersion` | main§5.4；JS-CONFIG |
| SF-config-sampleValidityCapMs | `sessions[].heartRate.originalAnalysis.analysisConfig.sampleValidityCapMs` | main§5.4；JS-CONFIG |
| SF-config-sampleIntervalContractVersion | `sessions[].heartRate.originalAnalysis.analysisConfig.sampleIntervalContractVersion` | main§5.4；JS-CONFIG |
| SF-config-partialLowerBoundBasisPoints | `sessions[].heartRate.originalAnalysis.analysisConfig.partialLowerBoundBasisPoints` | main§5.4；JS-CONFIG |
| SF-config-phaseConclusionBasisPoints | `sessions[].heartRate.originalAnalysis.analysisConfig.phaseConclusionBasisPoints` | main§5.4；JS-CONFIG |
| SF-config-normalBasisPoints | `sessions[].heartRate.originalAnalysis.analysisConfig.normalBasisPoints` | main§5.4；JS-CONFIG |
| SF-config-coverageThresholdRule | `sessions[].heartRate.originalAnalysis.analysisConfig.coverageThresholdRule` | main§5.4；JS-CONFIG |
| SF-config-coverageBasisPointsRule | `sessions[].heartRate.originalAnalysis.analysisConfig.coverageBasisPointsRule` | main§5.4；JS-CONFIG |
| SF-config-displayPercentRule | `sessions[].heartRate.originalAnalysis.analysisConfig.displayPercentRule` | main§5.4；JS-CONFIG |
| SF-config-weightedAverageRule | `sessions[].heartRate.originalAnalysis.analysisConfig.weightedAverageRule` | main§5.4；JS-CONFIG |
| SF-config-averageDisplayRule | `sessions[].heartRate.originalAnalysis.analysisConfig.averageDisplayRule` | main§5.4；JS-CONFIG |
| SF-config-zeroCoveredRule | `sessions[].heartRate.originalAnalysis.analysisConfig.zeroCoveredRule` | main§5.4；JS-CONFIG |
| SF-config-observedMaxRule | `sessions[].heartRate.originalAnalysis.analysisConfig.observedMaxRule` | main§5.4；JS-CONFIG |
| SF-config-zoneAttributionContractVersion | `sessions[].heartRate.originalAnalysis.analysisConfig.zoneAttributionContractVersion` | main§5.4；JS-CONFIG |
| SF-config-zoneAttributionRule | `sessions[].heartRate.originalAnalysis.analysisConfig.zoneAttributionRule` | main§5.4；JS-CONFIG |
| SF-config-statusProjectionContractVersion | `sessions[].heartRate.originalAnalysis.analysisConfig.statusProjectionContractVersion` | main§5.4；JS-CONFIG |
| SF-config-durationPartitionContractVersion | `sessions[].heartRate.originalAnalysis.analysisConfig.durationPartitionContractVersion` | main§5.4；JS-CONFIG |
| SF-zoneduration-zoneDurationsContractVersion | `sessions[].heartRate.originalAnalysis.zoneDurations.zoneDurationsContractVersion` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-below50DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.below50DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-from50To60DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.from50To60DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-from60To70DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.from60To70DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-from70To80DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.from70To80DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-from80To90DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.from80To90DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-zoneduration-atOrAbove90DurationMs | `sessions[].heartRate.originalAnalysis.zoneDurations.atOrAbove90DurationMs` | main§5.5；JS-ZONE-SUM |
| SF-aggregate-phaseAggregatesContractVersion | `sessions[].heartRate.originalAnalysis.phaseAggregates.phaseAggregatesContractVersion` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].phaseSequence | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].phaseSequence` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].phaseKind | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].phaseKind` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].eligibleDurationMs | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].eligibleDurationMs` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].coveredDurationMs | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].coveredDurationMs` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].coverageBasisPoints | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].coverageBasisPoints` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].coverageStatus | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].coverageStatus` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].conclusionEligible | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].conclusionEligible` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].weightedBpmMs | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].weightedBpmMs` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].observedAvgBpm | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].observedAvgBpm` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].observedMaxBpm | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].observedMaxBpm` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].highestOffsetMs | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].highestOffsetMs` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].highestMutationSequence | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].highestMutationSequence` | main§5.6；JS-PHASE-AGG |
| SF-aggregate-aggregates[].highestSampleSequence | `sessions[].heartRate.originalAnalysis.phaseAggregates.aggregates[].highestSampleSequence` | main§5.6；JS-PHASE-AGG |
| SF-duration-durationBreakdownContractVersion | `sessions[].heartRate.durationAudit.durationBreakdownContractVersion` | main§5.7；JS-DURATION |
| SF-duration-canonicalSessionDurationMs | `sessions[].heartRate.durationAudit.canonicalSessionDurationMs` | main§5.7；JS-DURATION |
| SF-duration-recordingWindowDurationMs | `sessions[].heartRate.durationAudit.recordingWindowDurationMs` | main§5.7；JS-DURATION |
| SF-duration-notRequestedBeforeRecordingStartMs | `sessions[].heartRate.durationAudit.notRequestedBeforeRecordingStartMs` | main§5.7；JS-DURATION |
| SF-duration-intentAxis | `sessions[].heartRate.durationAudit.intentAxis` | main§5.7；JS-DURATION |
| SF-duration-intentAxis.expectedRecordingDurationMs | `sessions[].heartRate.durationAudit.intentAxis.expectedRecordingDurationMs` | main§5.7；JS-DURATION |
| SF-duration-intentAxis.userExcludedDurationMs | `sessions[].heartRate.durationAudit.intentAxis.userExcludedDurationMs` | main§5.7；JS-DURATION |
| SF-duration-intentAxis.userTurnedOffDurationMs | `sessions[].heartRate.durationAudit.intentAxis.userTurnedOffDurationMs` | main§5.7；JS-DURATION |
| SF-duration-intentAxis.userOptedOutDurationMs | `sessions[].heartRate.durationAudit.intentAxis.userOptedOutDurationMs` | main§5.7；JS-DURATION |
| SF-duration-intentAxis.userDisconnectedSuppressRecoveryDurationMs | `sessions[].heartRate.durationAudit.intentAxis.userDisconnectedSuppressRecoveryDurationMs` | main§5.7；JS-DURATION |
| SF-duration-phaseAxis | `sessions[].heartRate.durationAudit.phaseAxis` | main§5.7；JS-DURATION |
| SF-duration-phaseAxis.primaryEligibleDurationMs | `sessions[].heartRate.durationAudit.phaseAxis.primaryEligibleDurationMs` | main§5.7；JS-DURATION |
| SF-duration-phaseAxis.phaseExcludedDurationMs | `sessions[].heartRate.durationAudit.phaseAxis.phaseExcludedDurationMs` | main§5.7；JS-DURATION |
| SF-duration-phaseAxis.strengthPrepareExcludedDurationMs | `sessions[].heartRate.durationAudit.phaseAxis.strengthPrepareExcludedDurationMs` | main§5.7；JS-DURATION |
| SF-duration-phaseAxis.pausedExcludedDurationMs | `sessions[].heartRate.durationAudit.phaseAxis.pausedExcludedDurationMs` | main§5.7；JS-DURATION |
| SF-duration-primaryAnalysisPartition | `sessions[].heartRate.durationAudit.primaryAnalysisPartition` | main§5.7；JS-DURATION |
| SF-duration-primaryAnalysisPartition.primaryEligibleDurationMs | `sessions[].heartRate.durationAudit.primaryAnalysisPartition.primaryEligibleDurationMs` | main§5.7；JS-DURATION |
| SF-duration-primaryAnalysisPartition.eligibleCoveredDurationMs | `sessions[].heartRate.durationAudit.primaryAnalysisPartition.eligibleCoveredDurationMs` | main§5.7；JS-DURATION |
| SF-duration-primaryAnalysisPartition.eligibleUncoveredDurationMs | `sessions[].heartRate.durationAudit.primaryAnalysisPartition.eligibleUncoveredDurationMs` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations | `sessions[].heartRate.durationAudit.deviceStateDurations` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.not_observing | `sessions[].heartRate.durationAudit.deviceStateDurations.not_observing` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.no_source_selected | `sessions[].heartRate.durationAudit.deviceStateDurations.no_source_selected` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.permission_required | `sessions[].heartRate.durationAudit.deviceStateDurations.permission_required` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.bluetooth_unavailable | `sessions[].heartRate.durationAudit.deviceStateDurations.bluetooth_unavailable` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.searching | `sessions[].heartRate.durationAudit.deviceStateDurations.searching` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.connecting | `sessions[].heartRate.durationAudit.deviceStateDurations.connecting` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.waiting_first_sample | `sessions[].heartRate.durationAudit.deviceStateDurations.waiting_first_sample` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.live | `sessions[].heartRate.durationAudit.deviceStateDurations.live` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.stale | `sessions[].heartRate.durationAudit.deviceStateDurations.stale` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.reconnecting | `sessions[].heartRate.durationAudit.deviceStateDurations.reconnecting` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.disconnected | `sessions[].heartRate.durationAudit.deviceStateDurations.disconnected` | main§5.7；JS-DURATION |
| SF-duration-deviceStateDurations.technical_failure | `sessions[].heartRate.durationAudit.deviceStateDurations.technical_failure` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations | `sessions[].heartRate.durationAudit.deviceReasonDurations` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.initial_acquisition | `sessions[].heartRate.durationAudit.deviceReasonDurations.initial_acquisition` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.automatic_recovery | `sessions[].heartRate.durationAudit.deviceReasonDurations.automatic_recovery` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.source_not_selected | `sessions[].heartRate.durationAudit.deviceReasonDurations.source_not_selected` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.source_unavailable | `sessions[].heartRate.durationAudit.deviceReasonDurations.source_unavailable` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.permission_missing | `sessions[].heartRate.durationAudit.deviceReasonDurations.permission_missing` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.permission_revoked | `sessions[].heartRate.durationAudit.deviceReasonDurations.permission_revoked` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.bluetooth_off | `sessions[].heartRate.durationAudit.deviceReasonDurations.bluetooth_off` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.platform_unavailable | `sessions[].heartRate.durationAudit.deviceReasonDurations.platform_unavailable` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.first_sample_timeout | `sessions[].heartRate.durationAudit.deviceReasonDurations.first_sample_timeout` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.sample_stale_timeout | `sessions[].heartRate.durationAudit.deviceReasonDurations.sample_stale_timeout` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.unexpected_disconnect | `sessions[].heartRate.durationAudit.deviceReasonDurations.unexpected_disconnect` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.connection_timeout | `sessions[].heartRate.durationAudit.deviceReasonDurations.connection_timeout` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.measurement_stream_unavailable | `sessions[].heartRate.durationAudit.deviceReasonDurations.measurement_stream_unavailable` | main§5.7；JS-DURATION |
| SF-duration-deviceReasonDurations.platform_failure | `sessions[].heartRate.durationAudit.deviceReasonDurations.platform_failure` | main§5.7；JS-DURATION |
| SF-duration-orthogonalityContract | `sessions[].heartRate.durationAudit.orthogonalityContract` | main§5.7；JS-DURATION |
| SF-duration-orthogonalityContract.contractVersion | `sessions[].heartRate.durationAudit.orthogonalityContract.contractVersion` | main§5.7；JS-DURATION |
| SF-duration-orthogonalityContract.rule | `sessions[].heartRate.durationAudit.orthogonalityContract.rule` | main§5.7；JS-DURATION |
| SF-quality-qualityReasonsContractVersion | `sessions[].heartRate.originalAnalysis.qualityReasons.qualityReasonsContractVersion` | main§5.8；JS-QUALITY |
| SF-quality-sessionReasons | `sessions[].heartRate.originalAnalysis.qualityReasons.sessionReasons` | main§5.8；JS-QUALITY |
| SF-quality-phaseReasons | `sessions[].heartRate.originalAnalysis.qualityReasons.phaseReasons` | main§5.8；JS-QUALITY |
| SF-quality-sessionReasons[].reasonCode | `sessions[].heartRate.originalAnalysis.qualityReasons.sessionReasons[].reasonCode` | main§5.8；JS-QUALITY |
| SF-quality-sessionReasons[].durationMs | `sessions[].heartRate.originalAnalysis.qualityReasons.sessionReasons[].durationMs` | main§5.8；JS-QUALITY |
| SF-quality-phaseReasons[].phaseSequence | `sessions[].heartRate.originalAnalysis.qualityReasons.phaseReasons[].phaseSequence` | main§5.8；JS-QUALITY |
| SF-quality-phaseReasons[].reasonCode | `sessions[].heartRate.originalAnalysis.qualityReasons.phaseReasons[].reasonCode` | main§5.8；JS-QUALITY |
| SF-quality-phaseReasons[].durationMs | `sessions[].heartRate.originalAnalysis.qualityReasons.phaseReasons[].durationMs` | main§5.8；JS-QUALITY |

Root phaseIdentity.payload的各field由PH32个source variant行逐key R/N/M/literal限定，不能因为示例payload={}就宣称没有nested字段。planSnapshotJson嵌入文本由PL/F30独立覆盖，不能套用本八schema的NULL规则。

### F.33 PH逐variant逐字段限定ledger

本表由同一main§6.1–6.4原矩阵逐行逐列对照，保留原R/N/M/literal文字；F22已定义每个PH source行的唯一primary Story，表内再次绑定AC以便反向核对。值域文字的正式authority仍是immutable source；此表不是新schema。每cell分别建立presence/type/literal或binding positive/negative oracle，行内variant/phaseKind/bound field必须共同满足。32个source行、327个限定cell；不把cell计数叫新增需求或Story。

| field义务ID / exact source cell | 原限定 | primary Story/AC |
|---|---|---|
| PH-L01.variant（main§6.1原表第1行/variant） | `boundary_block_work` warmup | E18-S07A/M01 |
| PH-L01.phaseKind（main§6.1原表第1行/phaseKind） | `timed_work` | E18-S07A/M01 |
| PH-L01.blockId（main§6.1原表第1行/blockId） | R | E18-S07A/M01 |
| PH-L01.stepIndex0（main§6.1原表第1行/stepIndex0） | R | E18-S07A/M01 |
| PH-L01.legacyBlockKind（main§6.1原表第1行/legacyBlockKind） | `warmup` | E18-S07A/M01 |
| PH-L01.legacyStageType（main§6.1原表第1行/legacyStageType） | `warmup` | E18-S07A/M01 |
| PH-L01.itemId（main§6.1原表第1行/itemId） | M | E18-S07A/M01 |
| PH-L01.exerciseId（main§6.1原表第1行/exerciseId） | M | E18-S07A/M01 |
| PH-L01.roundIndex0（main§6.1原表第1行/roundIndex0） | M | E18-S07A/M01 |
| PH-L02.variant（main§6.1原表第2行/variant） | `boundary_block_work` stretch | E18-S07A/M01 |
| PH-L02.phaseKind（main§6.1原表第2行/phaseKind） | `timed_work` | E18-S07A/M01 |
| PH-L02.blockId（main§6.1原表第2行/blockId） | R | E18-S07A/M01 |
| PH-L02.stepIndex0（main§6.1原表第2行/stepIndex0） | R | E18-S07A/M01 |
| PH-L02.legacyBlockKind（main§6.1原表第2行/legacyBlockKind） | `stretch` | E18-S07A/M01 |
| PH-L02.legacyStageType（main§6.1原表第2行/legacyStageType） | `cooldown` | E18-S07A/M01 |
| PH-L02.itemId（main§6.1原表第2行/itemId） | M | E18-S07A/M01 |
| PH-L02.exerciseId（main§6.1原表第2行/exerciseId） | M | E18-S07A/M01 |
| PH-L02.roundIndex0（main§6.1原表第2行/roundIndex0） | M | E18-S07A/M01 |
| PH-L03.variant（main§6.1原表第3行/variant） | `boundary_block_work` cooldown | E18-S07A/M01 |
| PH-L03.phaseKind（main§6.1原表第3行/phaseKind） | `timed_work` | E18-S07A/M01 |
| PH-L03.blockId（main§6.1原表第3行/blockId） | R | E18-S07A/M01 |
| PH-L03.stepIndex0（main§6.1原表第3行/stepIndex0） | R | E18-S07A/M01 |
| PH-L03.legacyBlockKind（main§6.1原表第3行/legacyBlockKind） | `cooldown` | E18-S07A/M01 |
| PH-L03.legacyStageType（main§6.1原表第3行/legacyStageType） | `cooldown` | E18-S07A/M01 |
| PH-L03.itemId（main§6.1原表第3行/itemId） | M | E18-S07A/M01 |
| PH-L03.exerciseId（main§6.1原表第3行/exerciseId） | M | E18-S07A/M01 |
| PH-L03.roundIndex0（main§6.1原表第3行/roundIndex0） | M | E18-S07A/M01 |
| PH-L04.variant（main§6.1原表第4行/variant） | `boundary_item_work` | E18-S07A/M01 |
| PH-L04.phaseKind（main§6.1原表第4行/phaseKind） | `timed_work` | E18-S07A/M01 |
| PH-L04.blockId（main§6.1原表第4行/blockId） | R | E18-S07A/M01 |
| PH-L04.stepIndex0（main§6.1原表第4行/stepIndex0） | R | E18-S07A/M01 |
| PH-L04.legacyBlockKind（main§6.1原表第4行/legacyBlockKind） | `warmup/stretch/cooldown` | E18-S07A/M01 |
| PH-L04.legacyStageType（main§6.1原表第4行/legacyStageType） | `warmup/work/cooldown/custom`，等于snapshot item literal | E18-S07A/M01 |
| PH-L04.itemId（main§6.1原表第4行/itemId） | R | E18-S07A/M01 |
| PH-L04.exerciseId（main§6.1原表第4行/exerciseId） | N：等于item.exerciseId或NULL | E18-S07A/M01 |
| PH-L04.roundIndex0（main§6.1原表第4行/roundIndex0） | M | E18-S07A/M01 |
| PH-L05.variant（main§6.1原表第5行/variant） | `boundary_item_rest` | E18-S07A/M01 |
| PH-L05.phaseKind（main§6.1原表第5行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L05.blockId（main§6.1原表第5行/blockId） | R | E18-S07A/M01 |
| PH-L05.stepIndex0（main§6.1原表第5行/stepIndex0） | R | E18-S07A/M01 |
| PH-L05.legacyBlockKind（main§6.1原表第5行/legacyBlockKind） | `warmup/stretch/cooldown` | E18-S07A/M01 |
| PH-L05.legacyStageType（main§6.1原表第5行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L05.itemId（main§6.1原表第5行/itemId） | R | E18-S07A/M01 |
| PH-L05.exerciseId（main§6.1原表第5行/exerciseId） | M | E18-S07A/M01 |
| PH-L05.roundIndex0（main§6.1原表第5行/roundIndex0） | M | E18-S07A/M01 |
| PH-L06.variant（main§6.1原表第6行/variant） | `boundary_rest_after_item` | E18-S07A/M01 |
| PH-L06.phaseKind（main§6.1原表第6行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L06.blockId（main§6.1原表第6行/blockId） | R | E18-S07A/M01 |
| PH-L06.stepIndex0（main§6.1原表第6行/stepIndex0） | R | E18-S07A/M01 |
| PH-L06.legacyBlockKind（main§6.1原表第6行/legacyBlockKind） | `warmup/stretch/cooldown` | E18-S07A/M01 |
| PH-L06.legacyStageType（main§6.1原表第6行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L06.itemId（main§6.1原表第6行/itemId） | R | E18-S07A/M01 |
| PH-L06.exerciseId（main§6.1原表第6行/exerciseId） | N：等于被引用item.exerciseId或NULL | E18-S07A/M01 |
| PH-L06.roundIndex0（main§6.1原表第6行/roundIndex0） | M | E18-S07A/M01 |
| PH-L07.variant（main§6.1原表第7行/variant） | `circuit_item_work` | E18-S07A/M01 |
| PH-L07.phaseKind（main§6.1原表第7行/phaseKind） | `timed_work` | E18-S07A/M01 |
| PH-L07.blockId（main§6.1原表第7行/blockId） | R | E18-S07A/M01 |
| PH-L07.stepIndex0（main§6.1原表第7行/stepIndex0） | R | E18-S07A/M01 |
| PH-L07.legacyBlockKind（main§6.1原表第7行/legacyBlockKind） | `timed_circuit` | E18-S07A/M01 |
| PH-L07.legacyStageType（main§6.1原表第7行/legacyStageType） | `work/custom` | E18-S07A/M01 |
| PH-L07.itemId（main§6.1原表第7行/itemId） | R | E18-S07A/M01 |
| PH-L07.exerciseId（main§6.1原表第7行/exerciseId） | N：等于item.exerciseId或NULL | E18-S07A/M01 |
| PH-L07.roundIndex0（main§6.1原表第7行/roundIndex0） | R | E18-S07A/M01 |
| PH-L08.variant（main§6.1原表第8行/variant） | `circuit_item_rest` | E18-S07A/M01 |
| PH-L08.phaseKind（main§6.1原表第8行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L08.blockId（main§6.1原表第8行/blockId） | R | E18-S07A/M01 |
| PH-L08.stepIndex0（main§6.1原表第8行/stepIndex0） | R | E18-S07A/M01 |
| PH-L08.legacyBlockKind（main§6.1原表第8行/legacyBlockKind） | `timed_circuit` | E18-S07A/M01 |
| PH-L08.legacyStageType（main§6.1原表第8行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L08.itemId（main§6.1原表第8行/itemId） | R | E18-S07A/M01 |
| PH-L08.exerciseId（main§6.1原表第8行/exerciseId） | M | E18-S07A/M01 |
| PH-L08.roundIndex0（main§6.1原表第8行/roundIndex0） | R | E18-S07A/M01 |
| PH-L09.variant（main§6.1原表第9行/variant） | `circuit_rest_after_item` | E18-S07A/M01 |
| PH-L09.phaseKind（main§6.1原表第9行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L09.blockId（main§6.1原表第9行/blockId） | R | E18-S07A/M01 |
| PH-L09.stepIndex0（main§6.1原表第9行/stepIndex0） | R | E18-S07A/M01 |
| PH-L09.legacyBlockKind（main§6.1原表第9行/legacyBlockKind） | `timed_circuit` | E18-S07A/M01 |
| PH-L09.legacyStageType（main§6.1原表第9行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L09.itemId（main§6.1原表第9行/itemId） | R | E18-S07A/M01 |
| PH-L09.exerciseId（main§6.1原表第9行/exerciseId） | N：等于被引用item.exerciseId或NULL | E18-S07A/M01 |
| PH-L09.roundIndex0（main§6.1原表第9行/roundIndex0） | R | E18-S07A/M01 |
| PH-L10.variant（main§6.1原表第10行/variant） | `between_round_rest` | E18-S07A/M01 |
| PH-L10.phaseKind（main§6.1原表第10行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L10.blockId（main§6.1原表第10行/blockId） | R | E18-S07A/M01 |
| PH-L10.stepIndex0（main§6.1原表第10行/stepIndex0） | R | E18-S07A/M01 |
| PH-L10.legacyBlockKind（main§6.1原表第10行/legacyBlockKind） | `timed_circuit` | E18-S07A/M01 |
| PH-L10.legacyStageType（main§6.1原表第10行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L10.itemId（main§6.1原表第10行/itemId） | M | E18-S07A/M01 |
| PH-L10.exerciseId（main§6.1原表第10行/exerciseId） | M | E18-S07A/M01 |
| PH-L10.roundIndex0（main§6.1原表第10行/roundIndex0） | R | E18-S07A/M01 |
| PH-L11.variant（main§6.1原表第11行/variant） | `standalone_rest` | E18-S07A/M01 |
| PH-L11.phaseKind（main§6.1原表第11行/phaseKind） | `timed_rest` | E18-S07A/M01 |
| PH-L11.blockId（main§6.1原表第11行/blockId） | R | E18-S07A/M01 |
| PH-L11.stepIndex0（main§6.1原表第11行/stepIndex0） | R | E18-S07A/M01 |
| PH-L11.legacyBlockKind（main§6.1原表第11行/legacyBlockKind） | `rest` | E18-S07A/M01 |
| PH-L11.legacyStageType（main§6.1原表第11行/legacyStageType） | `rest` | E18-S07A/M01 |
| PH-L11.itemId（main§6.1原表第11行/itemId） | M | E18-S07A/M01 |
| PH-L11.exerciseId（main§6.1原表第11行/exerciseId） | M | E18-S07A/M01 |
| PH-L11.roundIndex0（main§6.1原表第11行/roundIndex0） | M | E18-S07A/M01 |
| PH-L12.variant（main§6.1原表第12行/variant） | `paused` | E18-S07A/M01 |
| PH-L12.phaseKind（main§6.1原表第12行/phaseKind） | `paused` | E18-S07A/M01 |
| PH-L12.blockId（main§6.1原表第12行/blockId） | M | E18-S07A/M01 |
| PH-L12.stepIndex0（main§6.1原表第12行/stepIndex0） | M | E18-S07A/M01 |
| PH-L12.legacyBlockKind（main§6.1原表第12行/legacyBlockKind） | M | E18-S07A/M01 |
| PH-L12.legacyStageType（main§6.1原表第12行/legacyStageType） | M | E18-S07A/M01 |
| PH-L12.itemId（main§6.1原表第12行/itemId） | M | E18-S07A/M01 |
| PH-L12.exerciseId（main§6.1原表第12行/exerciseId） | M | E18-S07A/M01 |
| PH-L12.roundIndex0（main§6.1原表第12行/roundIndex0） | M | E18-S07A/M01 |
| PH-C01.variant（main§6.2原表第1行/variant） | `warmup` | E18-S07A/M02 |
| PH-C01.phaseKind（main§6.2原表第1行/phaseKind） | `timed_work` | E18-S07A/M02 |
| PH-C01.compositionVersion（main§6.2原表第1行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C01.compositionBlockId（main§6.2原表第1行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C01.timelineStageId（main§6.2原表第1行/timelineStageId） | R synthetic | E18-S07A/M02 |
| PH-C01.timelineStageKind（main§6.2原表第1行/timelineStageKind） | `warmup` | E18-S07A/M02 |
| PH-C01.stageGroupId（main§6.2原表第1行/stageGroupId） | R=`timelineStageId` | E18-S07A/M02 |
| PH-C01.targetId（main§6.2原表第1行/targetId） | R=`timelineStageId + ":target"` | E18-S07A/M02 |
| PH-C01.targetKind（main§6.2原表第1行/targetKind） | `warmup` | E18-S07A/M02 |
| PH-C01.roundIndex0（main§6.2原表第1行/roundIndex0） | M | E18-S07A/M02 |
| PH-C01.stageGroupIndex0（main§6.2原表第1行/stageGroupIndex0） | M | E18-S07A/M02 |
| PH-C01.targetIndex0（main§6.2原表第1行/targetIndex0） | `0` | E18-S07A/M02 |
| PH-C01.stageInstanceIndex0（main§6.2原表第1行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C01.targetInstanceIndex0（main§6.2原表第1行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C01.stepIndex0（main§6.2原表第1行/stepIndex0） | R | E18-S07A/M02 |
| PH-C02.variant（main§6.2原表第2行/variant） | `stage_group_action` | E18-S07A/M02 |
| PH-C02.phaseKind（main§6.2原表第2行/phaseKind） | `timed_work` | E18-S07A/M02 |
| PH-C02.compositionVersion（main§6.2原表第2行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C02.compositionBlockId（main§6.2原表第2行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C02.timelineStageId（main§6.2原表第2行/timelineStageId） | R | E18-S07A/M02 |
| PH-C02.timelineStageKind（main§6.2原表第2行/timelineStageKind） | `stage_group` | E18-S07A/M02 |
| PH-C02.stageGroupId（main§6.2原表第2行/stageGroupId） | R real | E18-S07A/M02 |
| PH-C02.targetId（main§6.2原表第2行/targetId） | R real | E18-S07A/M02 |
| PH-C02.targetKind（main§6.2原表第2行/targetKind） | `action` | E18-S07A/M02 |
| PH-C02.roundIndex0（main§6.2原表第2行/roundIndex0） | R | E18-S07A/M02 |
| PH-C02.stageGroupIndex0（main§6.2原表第2行/stageGroupIndex0） | R | E18-S07A/M02 |
| PH-C02.targetIndex0（main§6.2原表第2行/targetIndex0） | R | E18-S07A/M02 |
| PH-C02.stageInstanceIndex0（main§6.2原表第2行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C02.targetInstanceIndex0（main§6.2原表第2行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C02.stepIndex0（main§6.2原表第2行/stepIndex0） | R | E18-S07A/M02 |
| PH-C03.variant（main§6.2原表第3行/variant） | `stage_group_custom` | E18-S07A/M02 |
| PH-C03.phaseKind（main§6.2原表第3行/phaseKind） | `timed_work` | E18-S07A/M02 |
| PH-C03.compositionVersion（main§6.2原表第3行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C03.compositionBlockId（main§6.2原表第3行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C03.timelineStageId（main§6.2原表第3行/timelineStageId） | R | E18-S07A/M02 |
| PH-C03.timelineStageKind（main§6.2原表第3行/timelineStageKind） | `stage_group` | E18-S07A/M02 |
| PH-C03.stageGroupId（main§6.2原表第3行/stageGroupId） | R real | E18-S07A/M02 |
| PH-C03.targetId（main§6.2原表第3行/targetId） | R real | E18-S07A/M02 |
| PH-C03.targetKind（main§6.2原表第3行/targetKind） | `custom` | E18-S07A/M02 |
| PH-C03.roundIndex0（main§6.2原表第3行/roundIndex0） | R | E18-S07A/M02 |
| PH-C03.stageGroupIndex0（main§6.2原表第3行/stageGroupIndex0） | R | E18-S07A/M02 |
| PH-C03.targetIndex0（main§6.2原表第3行/targetIndex0） | R | E18-S07A/M02 |
| PH-C03.stageInstanceIndex0（main§6.2原表第3行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C03.targetInstanceIndex0（main§6.2原表第3行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C03.stepIndex0（main§6.2原表第3行/stepIndex0） | R | E18-S07A/M02 |
| PH-C04.variant（main§6.2原表第4行/variant） | `stage_group_rest` | E18-S07A/M02 |
| PH-C04.phaseKind（main§6.2原表第4行/phaseKind） | `timed_rest` | E18-S07A/M02 |
| PH-C04.compositionVersion（main§6.2原表第4行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C04.compositionBlockId（main§6.2原表第4行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C04.timelineStageId（main§6.2原表第4行/timelineStageId） | R | E18-S07A/M02 |
| PH-C04.timelineStageKind（main§6.2原表第4行/timelineStageKind） | `stage_group` | E18-S07A/M02 |
| PH-C04.stageGroupId（main§6.2原表第4行/stageGroupId） | R real | E18-S07A/M02 |
| PH-C04.targetId（main§6.2原表第4行/targetId） | R real | E18-S07A/M02 |
| PH-C04.targetKind（main§6.2原表第4行/targetKind） | `rest` | E18-S07A/M02 |
| PH-C04.roundIndex0（main§6.2原表第4行/roundIndex0） | R | E18-S07A/M02 |
| PH-C04.stageGroupIndex0（main§6.2原表第4行/stageGroupIndex0） | R | E18-S07A/M02 |
| PH-C04.targetIndex0（main§6.2原表第4行/targetIndex0） | R | E18-S07A/M02 |
| PH-C04.stageInstanceIndex0（main§6.2原表第4行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C04.targetInstanceIndex0（main§6.2原表第4行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C04.stepIndex0（main§6.2原表第4行/stepIndex0） | R | E18-S07A/M02 |
| PH-C05.variant（main§6.2原表第5行/variant） | `between_round_rest` | E18-S07A/M02 |
| PH-C05.phaseKind（main§6.2原表第5行/phaseKind） | `timed_rest` | E18-S07A/M02 |
| PH-C05.compositionVersion（main§6.2原表第5行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C05.compositionBlockId（main§6.2原表第5行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C05.timelineStageId（main§6.2原表第5行/timelineStageId） | R synthetic | E18-S07A/M02 |
| PH-C05.timelineStageKind（main§6.2原表第5行/timelineStageKind） | `between_round_rest` | E18-S07A/M02 |
| PH-C05.stageGroupId（main§6.2原表第5行/stageGroupId） | R=`timelineStageId` | E18-S07A/M02 |
| PH-C05.targetId（main§6.2原表第5行/targetId） | R=`timelineStageId + ":target"` | E18-S07A/M02 |
| PH-C05.targetKind（main§6.2原表第5行/targetKind） | `between_round_rest` | E18-S07A/M02 |
| PH-C05.roundIndex0（main§6.2原表第5行/roundIndex0） | R | E18-S07A/M02 |
| PH-C05.stageGroupIndex0（main§6.2原表第5行/stageGroupIndex0） | M | E18-S07A/M02 |
| PH-C05.targetIndex0（main§6.2原表第5行/targetIndex0） | `0` | E18-S07A/M02 |
| PH-C05.stageInstanceIndex0（main§6.2原表第5行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C05.targetInstanceIndex0（main§6.2原表第5行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C05.stepIndex0（main§6.2原表第5行/stepIndex0） | R | E18-S07A/M02 |
| PH-C06.variant（main§6.2原表第6行/variant） | `cooldown` | E18-S07A/M02 |
| PH-C06.phaseKind（main§6.2原表第6行/phaseKind） | `timed_work` | E18-S07A/M02 |
| PH-C06.compositionVersion（main§6.2原表第6行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C06.compositionBlockId（main§6.2原表第6行/compositionBlockId） | R | E18-S07A/M02 |
| PH-C06.timelineStageId（main§6.2原表第6行/timelineStageId） | R synthetic | E18-S07A/M02 |
| PH-C06.timelineStageKind（main§6.2原表第6行/timelineStageKind） | `cooldown` | E18-S07A/M02 |
| PH-C06.stageGroupId（main§6.2原表第6行/stageGroupId） | R=`timelineStageId` | E18-S07A/M02 |
| PH-C06.targetId（main§6.2原表第6行/targetId） | R=`timelineStageId + ":target"` | E18-S07A/M02 |
| PH-C06.targetKind（main§6.2原表第6行/targetKind） | `cooldown` | E18-S07A/M02 |
| PH-C06.roundIndex0（main§6.2原表第6行/roundIndex0） | M | E18-S07A/M02 |
| PH-C06.stageGroupIndex0（main§6.2原表第6行/stageGroupIndex0） | M | E18-S07A/M02 |
| PH-C06.targetIndex0（main§6.2原表第6行/targetIndex0） | `0` | E18-S07A/M02 |
| PH-C06.stageInstanceIndex0（main§6.2原表第6行/stageInstanceIndex0） | R | E18-S07A/M02 |
| PH-C06.targetInstanceIndex0（main§6.2原表第6行/targetInstanceIndex0） | R | E18-S07A/M02 |
| PH-C06.stepIndex0（main§6.2原表第6行/stepIndex0） | R | E18-S07A/M02 |
| PH-C07.variant（main§6.2原表第7行/variant） | `paused` | E18-S07A/M02 |
| PH-C07.phaseKind（main§6.2原表第7行/phaseKind） | `paused` | E18-S07A/M02 |
| PH-C07.compositionVersion（main§6.2原表第7行/compositionVersion） | `2` | E18-S07A/M02 |
| PH-C07.compositionBlockId（main§6.2原表第7行/compositionBlockId） | M | E18-S07A/M02 |
| PH-C07.timelineStageId（main§6.2原表第7行/timelineStageId） | M | E18-S07A/M02 |
| PH-C07.timelineStageKind（main§6.2原表第7行/timelineStageKind） | M | E18-S07A/M02 |
| PH-C07.stageGroupId（main§6.2原表第7行/stageGroupId） | M | E18-S07A/M02 |
| PH-C07.targetId（main§6.2原表第7行/targetId） | M | E18-S07A/M02 |
| PH-C07.targetKind（main§6.2原表第7行/targetKind） | M | E18-S07A/M02 |
| PH-C07.roundIndex0（main§6.2原表第7行/roundIndex0） | M | E18-S07A/M02 |
| PH-C07.stageGroupIndex0（main§6.2原表第7行/stageGroupIndex0） | M | E18-S07A/M02 |
| PH-C07.targetIndex0（main§6.2原表第7行/targetIndex0） | M | E18-S07A/M02 |
| PH-C07.stageInstanceIndex0（main§6.2原表第7行/stageInstanceIndex0） | M | E18-S07A/M02 |
| PH-C07.targetInstanceIndex0（main§6.2原表第7行/targetInstanceIndex0） | M | E18-S07A/M02 |
| PH-C07.stepIndex0（main§6.2原表第7行/stepIndex0） | M | E18-S07A/M02 |
| PH-S01.variant（main§6.3原表第1行/variant） | `prepare_set` | E19-S01/S01 |
| PH-S01.phaseKind（main§6.3原表第1行/phaseKind） | `strength_prepare_set` | E19-S01/S01 |
| PH-S01.blockId（main§6.3原表第1行/blockId） | R | E19-S01/S01 |
| PH-S01.setPlanId（main§6.3原表第1行/setPlanId） | R | E19-S01/S01 |
| PH-S01.plannedExerciseId（main§6.3原表第1行/plannedExerciseId） | R | E19-S01/S01 |
| PH-S01.actualExerciseId（main§6.3原表第1行/actualExerciseId） | R | E19-S01/S01 |
| PH-S01.exerciseSetIndex0（main§6.3原表第1行/exerciseSetIndex0） | R | E19-S01/S01 |
| PH-S01.globalSetIndex0（main§6.3原表第1行/globalSetIndex0） | R | E19-S01/S01 |
| PH-S01.setKind（main§6.3原表第1行/setKind） | R=`warmup/working/drop/backoff` | E19-S01/S01 |
| PH-S01.substitutedFromExerciseId（main§6.3原表第1行/substitutedFromExerciseId） | N | E19-S01/S01 |
| PH-S02.variant（main§6.3原表第2行/variant） | `active_set` | E19-S01/S01 |
| PH-S02.phaseKind（main§6.3原表第2行/phaseKind） | `strength_active_set` | E19-S01/S01 |
| PH-S02.blockId（main§6.3原表第2行/blockId） | R | E19-S01/S01 |
| PH-S02.setPlanId（main§6.3原表第2行/setPlanId） | R | E19-S01/S01 |
| PH-S02.plannedExerciseId（main§6.3原表第2行/plannedExerciseId） | R | E19-S01/S01 |
| PH-S02.actualExerciseId（main§6.3原表第2行/actualExerciseId） | R | E19-S01/S01 |
| PH-S02.exerciseSetIndex0（main§6.3原表第2行/exerciseSetIndex0） | R | E19-S01/S01 |
| PH-S02.globalSetIndex0（main§6.3原表第2行/globalSetIndex0） | R | E19-S01/S01 |
| PH-S02.setKind（main§6.3原表第2行/setKind） | R=`warmup/working/drop/backoff` | E19-S01/S01 |
| PH-S02.substitutedFromExerciseId（main§6.3原表第2行/substitutedFromExerciseId） | N | E19-S01/S01 |
| PH-S03.variant（main§6.3原表第3行/variant） | `confirm_set` | E19-S01/S01 |
| PH-S03.phaseKind（main§6.3原表第3行/phaseKind） | `strength_confirm_set` | E19-S01/S01 |
| PH-S03.blockId（main§6.3原表第3行/blockId） | R | E19-S01/S01 |
| PH-S03.setPlanId（main§6.3原表第3行/setPlanId） | R | E19-S01/S01 |
| PH-S03.plannedExerciseId（main§6.3原表第3行/plannedExerciseId） | R | E19-S01/S01 |
| PH-S03.actualExerciseId（main§6.3原表第3行/actualExerciseId） | R | E19-S01/S01 |
| PH-S03.exerciseSetIndex0（main§6.3原表第3行/exerciseSetIndex0） | R | E19-S01/S01 |
| PH-S03.globalSetIndex0（main§6.3原表第3行/globalSetIndex0） | R | E19-S01/S01 |
| PH-S03.setKind（main§6.3原表第3行/setKind） | R=`warmup/working/drop/backoff` | E19-S01/S01 |
| PH-S03.substitutedFromExerciseId（main§6.3原表第3行/substitutedFromExerciseId） | N | E19-S01/S01 |
| PH-S04.variant（main§6.3原表第4行/variant） | `rest` | E19-S01/S01 |
| PH-S04.phaseKind（main§6.3原表第4行/phaseKind） | `strength_rest` | E19-S01/S01 |
| PH-S04.blockId（main§6.3原表第4行/blockId） | R | E19-S01/S01 |
| PH-S04.setPlanId（main§6.3原表第4行/setPlanId） | R | E19-S01/S01 |
| PH-S04.plannedExerciseId（main§6.3原表第4行/plannedExerciseId） | R | E19-S01/S01 |
| PH-S04.actualExerciseId（main§6.3原表第4行/actualExerciseId） | R | E19-S01/S01 |
| PH-S04.exerciseSetIndex0（main§6.3原表第4行/exerciseSetIndex0） | R | E19-S01/S01 |
| PH-S04.globalSetIndex0（main§6.3原表第4行/globalSetIndex0） | R | E19-S01/S01 |
| PH-S04.setKind（main§6.3原表第4行/setKind） | R=`warmup/working/drop/backoff` | E19-S01/S01 |
| PH-S04.substitutedFromExerciseId（main§6.3原表第4行/substitutedFromExerciseId） | N | E19-S01/S01 |
| PH-S05.variant（main§6.3原表第5行/variant） | `paused` | E19-S01/S01 |
| PH-S05.phaseKind（main§6.3原表第5行/phaseKind） | `paused` | E19-S01/S01 |
| PH-S05.blockId（main§6.3原表第5行/blockId） | M | E19-S01/S01 |
| PH-S05.setPlanId（main§6.3原表第5行/setPlanId） | M | E19-S01/S01 |
| PH-S05.plannedExerciseId（main§6.3原表第5行/plannedExerciseId） | M | E19-S01/S01 |
| PH-S05.actualExerciseId（main§6.3原表第5行/actualExerciseId） | M | E19-S01/S01 |
| PH-S05.exerciseSetIndex0（main§6.3原表第5行/exerciseSetIndex0） | M | E19-S01/S01 |
| PH-S05.globalSetIndex0（main§6.3原表第5行/globalSetIndex0） | M | E19-S01/S01 |
| PH-S05.setKind（main§6.3原表第5行/setKind） | M | E19-S01/S01 |
| PH-S05.substitutedFromExerciseId（main§6.3原表第5行/substitutedFromExerciseId） | M | E19-S01/S01 |
| PH-F01.variant（main§6.4原表第1行/variant） | `circuit_action` | E19-S02/F01 |
| PH-F01.phaseKind（main§6.4原表第1行/phaseKind） | `follow_along_action` | E19-S02/F01 |
| PH-F01.blockId（main§6.4原表第1行/blockId） | R | E19-S02/F01 |
| PH-F01.stepIndex0（main§6.4原表第1行/stepIndex0） | R | E19-S02/F01 |
| PH-F01.followAlongStepKind（main§6.4原表第1行/followAlongStepKind） | `action` | E19-S02/F01 |
| PH-F01.itemId（main§6.4原表第1行/itemId） | R | E19-S02/F01 |
| PH-F01.exerciseId（main§6.4原表第1行/exerciseId） | R | E19-S02/F01 |
| PH-F01.roundIndex0（main§6.4原表第1行/roundIndex0） | R | E19-S02/F01 |
| PH-F02.variant（main§6.4原表第2行/variant） | `non_circuit_action` | E19-S02/F01 |
| PH-F02.phaseKind（main§6.4原表第2行/phaseKind） | `follow_along_action` | E19-S02/F01 |
| PH-F02.blockId（main§6.4原表第2行/blockId） | R | E19-S02/F01 |
| PH-F02.stepIndex0（main§6.4原表第2行/stepIndex0） | R | E19-S02/F01 |
| PH-F02.followAlongStepKind（main§6.4原表第2行/followAlongStepKind） | `action` | E19-S02/F01 |
| PH-F02.itemId（main§6.4原表第2行/itemId） | R | E19-S02/F01 |
| PH-F02.exerciseId（main§6.4原表第2行/exerciseId） | R | E19-S02/F01 |
| PH-F02.roundIndex0（main§6.4原表第2行/roundIndex0） | M | E19-S02/F01 |
| PH-F03.variant（main§6.4原表第3行/variant） | `circuit_rest_after_action` | E19-S02/F01 |
| PH-F03.phaseKind（main§6.4原表第3行/phaseKind） | `follow_along_rest` | E19-S02/F01 |
| PH-F03.blockId（main§6.4原表第3行/blockId） | R | E19-S02/F01 |
| PH-F03.stepIndex0（main§6.4原表第3行/stepIndex0） | R | E19-S02/F01 |
| PH-F03.followAlongStepKind（main§6.4原表第3行/followAlongStepKind） | `rest_after_action` | E19-S02/F01 |
| PH-F03.itemId（main§6.4原表第3行/itemId） | R | E19-S02/F01 |
| PH-F03.exerciseId（main§6.4原表第3行/exerciseId） | R，等于被引用action | E19-S02/F01 |
| PH-F03.roundIndex0（main§6.4原表第3行/roundIndex0） | R | E19-S02/F01 |
| PH-F04.variant（main§6.4原表第4行/variant） | `non_circuit_rest_after_action` | E19-S02/F01 |
| PH-F04.phaseKind（main§6.4原表第4行/phaseKind） | `follow_along_rest` | E19-S02/F01 |
| PH-F04.blockId（main§6.4原表第4行/blockId） | R | E19-S02/F01 |
| PH-F04.stepIndex0（main§6.4原表第4行/stepIndex0） | R | E19-S02/F01 |
| PH-F04.followAlongStepKind（main§6.4原表第4行/followAlongStepKind） | `rest_after_action` | E19-S02/F01 |
| PH-F04.itemId（main§6.4原表第4行/itemId） | R | E19-S02/F01 |
| PH-F04.exerciseId（main§6.4原表第4行/exerciseId） | R，等于被引用action | E19-S02/F01 |
| PH-F04.roundIndex0（main§6.4原表第4行/roundIndex0） | M | E19-S02/F01 |
| PH-F05.variant（main§6.4原表第5行/variant） | `between_round_rest` | E19-S02/F01 |
| PH-F05.phaseKind（main§6.4原表第5行/phaseKind） | `follow_along_rest` | E19-S02/F01 |
| PH-F05.blockId（main§6.4原表第5行/blockId） | R | E19-S02/F01 |
| PH-F05.stepIndex0（main§6.4原表第5行/stepIndex0） | R | E19-S02/F01 |
| PH-F05.followAlongStepKind（main§6.4原表第5行/followAlongStepKind） | `between_round_rest` | E19-S02/F01 |
| PH-F05.itemId（main§6.4原表第5行/itemId） | M | E19-S02/F01 |
| PH-F05.exerciseId（main§6.4原表第5行/exerciseId） | M | E19-S02/F01 |
| PH-F05.roundIndex0（main§6.4原表第5行/roundIndex0） | R | E19-S02/F01 |
| PH-F06.variant（main§6.4原表第6行/variant） | `block_rest` | E19-S02/F01 |
| PH-F06.phaseKind（main§6.4原表第6行/phaseKind） | `follow_along_rest` | E19-S02/F01 |
| PH-F06.blockId（main§6.4原表第6行/blockId） | R | E19-S02/F01 |
| PH-F06.stepIndex0（main§6.4原表第6行/stepIndex0） | R | E19-S02/F01 |
| PH-F06.followAlongStepKind（main§6.4原表第6行/followAlongStepKind） | `block_rest` | E19-S02/F01 |
| PH-F06.itemId（main§6.4原表第6行/itemId） | M | E19-S02/F01 |
| PH-F06.exerciseId（main§6.4原表第6行/exerciseId） | M | E19-S02/F01 |
| PH-F06.roundIndex0（main§6.4原表第6行/roundIndex0） | M | E19-S02/F01 |
| PH-F07.variant（main§6.4原表第7行/variant） | `boundary` | E19-S02/F01 |
| PH-F07.phaseKind（main§6.4原表第7行/phaseKind） | `follow_along_action` | E19-S02/F01 |
| PH-F07.blockId（main§6.4原表第7行/blockId） | R | E19-S02/F01 |
| PH-F07.stepIndex0（main§6.4原表第7行/stepIndex0） | R | E19-S02/F01 |
| PH-F07.followAlongStepKind（main§6.4原表第7行/followAlongStepKind） | `boundary` | E19-S02/F01 |
| PH-F07.itemId（main§6.4原表第7行/itemId） | M | E19-S02/F01 |
| PH-F07.exerciseId（main§6.4原表第7行/exerciseId） | M | E19-S02/F01 |
| PH-F07.roundIndex0（main§6.4原表第7行/roundIndex0） | M | E19-S02/F01 |
| PH-F08.variant（main§6.4原表第8行/variant） | `paused` | E19-S02/F01 |
| PH-F08.phaseKind（main§6.4原表第8行/phaseKind） | `paused` | E19-S02/F01 |
| PH-F08.blockId（main§6.4原表第8行/blockId） | M | E19-S02/F01 |
| PH-F08.stepIndex0（main§6.4原表第8行/stepIndex0） | M | E19-S02/F01 |
| PH-F08.followAlongStepKind（main§6.4原表第8行/followAlongStepKind） | M | E19-S02/F01 |
| PH-F08.itemId（main§6.4原表第8行/itemId） | M | E19-S02/F01 |
| PH-F08.exerciseId（main§6.4原表第8行/exerciseId） | M | E19-S02/F01 |
| PH-F08.roundIndex0（main§6.4原表第8行/roundIndex0） | M | E19-S02/F01 |

通用mutation终态：missing R/N/M的key、M变非NULL、R变NULL、wrong literal/version/family/mode、wrong snapshot引用、extra field都在已有PhaseIdentityV1Validator或新producer可信边界拒绝；不得把错误phase导出或靠resolver换一个相似对象。M不是optional；N不是可以任意NULL，仍受该cell条件；legacy boundary三行没有按同variant去重。Composition warmup/cooldown/between-round的synthetic规则、rounds=1仍有roundIndex0，strength替换identity的跨phase一致、follow不得借timed family，均按source cross-field条件及F22对应PF/FI补充矩阵覆盖。

### F.34 真实接线、读取可达性与失败证据收口

本节是主管理可行性/合同细化，绑定A的immutable main；没有运行代码、独立Review或新设备验证。F.26的S08B新增对S03的直接前置：F.27.2已将时间元数据唯一语义校验归S03，S08B必须消费它，不能在没有该前置时另写一个validator。当前标签图因此为24节点/42唯一边；旧41边是修正前定位。

#### F.34.1 engine → 模式producer → Recorder

本轮直接读取的exact源码：

- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\model\WorkoutEvent.kt`。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\engine\TimedWorkoutEngine.kt`，tick、dispatch、advanceToNextStep及history记录边界。
- `C:\Users\25073\Desktop\jianshen\.local\worktrees\main-integration\app\src\main\java\com\liujyks\trainflow\core\engine\StrengthWorkoutEngine.kt`，tick、Start、prepare/active/rest转换边界。
- F.11/F.14已经列明的三个完整Route路径，实际applyEngineResult、dispatch及tick调用。

`PROVEN`：三个当前Route均每次delay(1000)后调用默认seconds=1的tick；计时advanceToNextStep一次前进一个step。WorkoutEvent的阶段启动事件有step/exercise/set身份，但没有canonical单调时刻；engine的activeElapsedSec/sessionElapsedSec与旧history秒值不是包含pause的canonical全程毫秒轴。

`INFERENCE / CONTRACT`：沿已接受模式ViewModel单一生产命令入口，在实际Start/tick/命令处理cut捕获本场单调时刻，保留before state、真实result/events及after state，按实际发生顺序交给模式事实映射和同一Recorder。一次合法转换的关闭/打开可以同毫秒、不同mutation sequence；不可从UI最终currentStep反推整段历史，不可把计划时长或engine旧秒值伪装成独立实测毫秒。声音提示事件不是额外phase。无状态变化的倒计时tick不凭空新增阶段。

当前证据没有证明生产存在批量catch-up丢中间phase，故不扩大engine/adapter修改范围。将计时循环保留在Activity-retained owner时保持既有命令推进语义；未来若确需批量推进且现有结果不能表达每个实际转换cut，先返回该真实合同缺口，不私自从计划时间重建事实。这个限制不禁止同毫秒合法多事件，也不承诺后台精确调度。

| primary AC | 独立输入与应观察结果 | matching boundary / consumer |
|---|---|---|
| S07A/M04、S07B/L03 | 真实engine依次Start、自然阶段结束、pause/resume、skip、extra-rest、提前结束；预期按合同人工列出phase kind/identity顺序，不由同一个mapper生成 | pure事实映射检查每个cut，真实ViewModel→Recorder→Room检查完整轨迹；不拿前者替代后者 |
| E19-S01/S01–S05 | 真实prepare→active→confirm→rest及自然/手动结束休息、替换/跳过；实际确认值与planned值分别读取 | 真实力量命令与控件→持久化→历史；同set四类非pause身份绑定，不套计时predicate |
| E19-S02/F01–F04 | 跟练自动Start只发生一次，circuit/non-circuit、round/block/boundary、pause/skip及终结保留跟练family | 跟练实际preset/engine/ViewModel→Room；不能拿计时模式fixture证明跟练 |

#### F.34.2 strict read与display结果分层

本轮直接source为F.29已绑定的`CanonicalSessionValidators.kt`及`CanonicalStorageJsonValidators.kt`；后者实际定义PhaseIdentityV1Validator，不存在本轮采用的同名独立源码文件。

| 输入与分支 | production disposition | primary/oracle |
|---|---|---|
| canonical缺display metadata或其版本/JSON非法 | 现有header validator拒绝；S08B返回typed unavailable，选中导出整份失败 | S08B/H04真实坏row，E21/J05消费；不造成功terminal graph |
| phase identity非法、未知version或与plan/mode/phase不绑定 | 现有graph validator逐phase调用PhaseIdentityV1Validator并拒绝 | S08B/H04→E21/J05；保留原始行，不通过resolver修复 |
| 合法graph，所需历史label引用无法从冻结metadata找到 | S08C按main§7.3的unresolved_missing_metadata/NULL表达；仅在合法fixture实际满足前层约束时认定该生产分支可达 | S08C/H07；fixture必须先通过原validator，否则它属于上一行的read失败 |
| 合法graph及所需冻结名称/结构齐全 | resolved与非空label，locale为本次displayLocale，机器identity完整保留 | S08C/H07独立four-family输入；当前计划改名不能改变输出 |
| schema词汇中的unresolved_invalid_metadata、unresolved_invalid_identity、unsupported_identity_version | 保留字典/格式解释；不是要求S08B为坏graph开放成功通道。当前strict成功链无此可达证明，不新增防御性生产分支 | E21/J01/J03格式与独立consumer词汇验证；production路径证明其在read边界被拒绝 |
| legacy terminal | 严格legacy plan读取仍可用；不造不存在的canonical phase identity/display，旧step hints不升级为canonical阶段 | S08A/H03、S08B/H03与E21/J04 |

这澄清F.25的“各合法resolved/unresolved schema分支”：格式允许性、前置校验和真实生产可达性是三个不同claim。不能要求以绕过strict graph的fixture作为正常导出端到端PASS。

#### F.34.3 IO与错误的证据能力

下表固定未来证据选择的边界，不降低实际失败处理，也不要求逐个操作系统故障都造production seam。测试失败点是测试端安排的真实资源/事务状态；不可修改用户数据库、填满手机或引入生产故障开关。

| 边界 / primary AC | 可判定证据 | 明确限界与原错误规则 |
|---|---|---|
| Room Start/active/terminal，S03/B02/B05、S04/T03 | 在测试库沿既有SQL trigger/constraint/guard触发真实事务失败，逐表比较全回滚；正常嵌套事务及结果取消单独覆盖 | SQL失败不自称真实磁盘耗尽；源码检查没有吞错/清库。原异常保留，cleanup错误为secondary |
| 同请求结果交付取消，S03/B03、S04/T04、S05/G | 用测试端coroutine调度及真实持久化已提交graph，随后调用生产查询/重试，核对完整payload及后续准入 | 不用mock commit成功代替DB结果；无法控制的时序不报告已跑过 |
| 私有文件创建/发布/清理，E21-S02/P02/P04 | 测试专用目录中以目标被普通文件占用、发布目标冲突、存在残留及实际删除结果等公共文件操作触发可达结果；正常输出完整JSON读回与字节identity | 用真实操作证明相应失败类，不能以一个create失败声称flush/sync/close各点都动态覆盖；清理失败不误报已删除 |
| write/flush/sync/close，E21-S02/P01/P02 | 对实际生产资源生命周期作精确source检查：所有操作成功前无READY；异常沿同一失败出口，finally/use关闭且不掩盖primary；结合真实成功IO和公共可达失败证据 | 框架资源/异常传播保证属于其本层证据，不是逐点运行证据。若Writer必须加入新factory/interface才可证明某claim，停止并报告exact gap，不自行扩展生产边界 |
| 系统文档provider，E21-S04/D02 | 实际Android CreateDocument/外部URI打开、完整复制、返回取消/失败以及外部读取字节；可在测试端独立provider制造真实平台错误 | 测试provider证明content resolver协议，不能冒称用户具体云盘均已成功；外部半成品不可撤回必须诚实说明 |
| 分享，E21-S04/D03/D04 | 真实chooser与外部只读reader、权限范围、返回/进程中断及保留目录状态 | chooser调用成功不证明接收方保存；保留24h按已接受best-effort触发条件 |
| Activity/进程/FGS/真实HR | F.27.3所列分层证据，运行身份必须绑定candidate+APK+环境，主观UI和Band/RF仍是人工门禁 | 本轮没有执行；不以源码/AVD替代实机，不授权下载新环境 |

F.29所指“具体IO故障能力检查”在规划层按本表闭合：保留真正可证明的claim并区分source/framework/integration/human层。未来实际evidence若不足仍阻止对应Story通过；并非所有低层操作都必须使用人工注入逐点运行，亦非免测失败出口。

### F.35 补充语义索引与证据身份合同

本节收口细化中的遗漏信号，不生成另一套分析数学。AS-05已经实现的公式/reason producer与validator继续为REFERENCE；新代码只承接生产写入/版本读取/字典与UI消费者。下面每行primary为S08B/H02的原始绑定读取义务，直接消费者E21-S01/J02/J03与E22-S02/K02；phase呈现另沿E22-S03/V08。未来oracle用独立合法base逐项单改，所有错误不得降级成正常状态或删掉reason。

#### F.35.1 Reason required-iff逐行索引

exact source为同一main技术合同§5.8.1对应reasonCode原行及additional exact rules；本表只给稳定索引和可定位限定，不替代原矩阵。每行condition不成立即forbidden，不把iff弱化成可选提示。

| ID / reasonCode | Session / phase限定与duration oracle |
|---|---|
| QR01 no_eligible_duration | whole eligible=0；phase为该primary aggregate eligible=0；duration NULL；可与真实sample-axis reason并存 |
| QR02 no_canonical_samples | whole取sample_status；phase需eligible>0且其eligible partition原点数为0；duration NULL |
| QR03 canonical_only_excluded | whole取sample_status；phase forbidden；duration NULL |
| QR04 eligible_uncovered_present | whole需expectedRecording>0且uncovered>0；phase为eligible-covered>0；duration必须是对应positive差值 |
| QR05 insufficient_coverage | whole/phase各自coverageStatus=insufficient iff存在；duration NULL；不得与partial互换 |
| QR06 partial_coverage | whole/phase各自coverageStatus=partial iff存在；duration NULL；normal/no-eligible时forbidden |
| QR07 unavailable_no_effective_max | 同snapshot zone unavailable且对应eligible>0；duration NULL；available或eligible0时forbidden |
| QR08 not_requested_before_recording_start | whole同名duration>0；phase forbidden；duration等于该whole原值 |
| QR09 strength_prepare_excluded | whole轴同名positive值；phase必须是strength_prepare_set且与recording window有positive交集；duration等于对应值/交集 |
| QR10 paused_excluded | whole轴同名positive值；phase必须为paused且与recording window有positive交集；duration等于对应值/交集 |
| QR11 user_turned_off_excluded | whole同名intent duration>0；phase与对应acquisition的positive交集；duration等于相应值 |
| QR12 user_opted_out_excluded | 同上但必须是opted_out来源，不能用turned_off总时长替代 |
| QR13 user_disconnected_suppress_recovery_excluded | 同上但必须是用户断开抑制恢复来源，不能拿设备unexpected_disconnect补值 |
| QR14 process_interrupted | session terminal_reason exact匹配才存在；phase forbidden；duration NULL，不用重启时刻计算未知尾段 |
| QR15 order/identity | session固定enum顺序、phase按phaseSequence再enum；无重复pair，phase必须引用同snapshot真实closed phase；错序、错phase、missing/extra或错duration按原invalid_quality_reasons_v1失败 |

#### F.35.2 Duration方程逐行索引

exact source同一main§5.7 Equations，primary与消费者同上；检查是消费原validator结果及不改变原始值，不重写CS-05。每个等式用两侧独立已知常量以及±1单改检查，最后不等式同时覆盖等于、小于和大于。

| ID | 原方程限定 |
|---|---|
| DA01 | canonicalSessionDurationMs = notRequestedBeforeRecordingStartMs + userExcludedDurationMs + phaseExcludedDurationMs + primaryEligibleDurationMs |
| DA02 | recordingWindowDurationMs = canonicalSessionDurationMs - notRequestedBeforeRecordingStartMs |
| DA03 | expectedRecordingDurationMs + userExcludedDurationMs = recordingWindowDurationMs |
| DA04 | userExcludedDurationMs = userTurnedOffDurationMs + userOptedOutDurationMs + userDisconnectedSuppressRecoveryDurationMs |
| DA05 | phaseExcludedDurationMs = strengthPrepareExcludedDurationMs + pausedExcludedDurationMs |
| DA06 | primaryEligibleDurationMs = eligibleCoveredDurationMs + eligibleUncoveredDurationMs |
| DA07 | sum(deviceStateDurations) = recordingWindowDurationMs；12 keys全存在，未出现仍显式0 |
| DA08 | sum(deviceReasonDurations) <= recordingWindowDurationMs；14 keys全存在，未出现仍显式0 |
| DA09 | primary互斥分区与device独立轴不可相加；orthogonalityContract literal保持，字典必须解释这一点 |

#### F.35.3 S02恢复分支的可用测试层

本轮读到F.19精确路径`HeartRateRuntimeOwnerRecoveryTest.kt`的真实测试正文：production owner.submit、主looper idleFor、scanner/GATT callback、permission loss、exact target、unexpected disconnect/scan failure和取消排队恢复均有测试端驱动方法。对应F.19/O07 cause→pair可以在这些真实owner调用后追加观测sink断言，无须生产故障hook。原测试的shadow GATT/scanner证明Robolectric层行为，不证明真实Band/RF，也不证明新增观测已实现。本轮未运行测试；未读部分不称完整Code Review。

#### F.35.4 每项未来证据的身份schema

一个evidence记录须包含：Story selector及合同artifact SHA256、accepted base full SHA、candidate full SHA/tree、准确delta、AC/obligation IDs、测试源码/fixture identity、执行命令与结果、环境/JDK/SDK/设备身份、所证明的claim与未证明边界、产物完整literal路径及hash。UI/设备项追加APK hash、安装应用与executable identity、人工操作及结果；进程中断证据分别标注kill前后阶段，不能沿用旧APK证据。

在未来用户手工角色门禁中指定实际worktree和evidence路径后，主管理才能填写完整根模板；本轮未创建这些位置。候选标签不能充当implementation SHA，未来前置依然明确阻塞。没有要求用户现在运行全部设备矩阵；真实人机门禁随对应Story到达，保持F.27.3最终整合性能门禁。

E20-S02额外按F.15.3/B12–B15使用measurement和final两套身份及先后因果链。单条schema齐全只能证明该条可定位，不能代替M1到final freshness的推导、final executable变化分析与受影响gate重跑责任。

完整dictionary实例属于E21-S01实现交付，规划在F.23定义其closed表示、逐字段覆盖、IC/NUM独立oracle即可判定该交付。实例尚未生成及consumer尚未执行，不等于缺少新产品决定；不得由主管理模拟一份“已经验证通过”的导出。正式Planning Review需要独立检查F.23能否承载source全部语义。

### F.36 24候选十维capacity主管理判定

方法为已核验BMAD F6十项结构判据。本表是候选规划的主管理逐项判断，**不是fresh独立Planning Review、用户接受或READY**。每个P表示本候选在该结构判据上PASS，引用行尾合同及下方限定；不是运行测试PASS。未来实现身份尚不存在，D5的P仅表示前置已明确绑定到候选包并阻塞下游，绝不表示已经合并。

十列依次为：D1单一价值；D2义务闭合；D3owner稳定；D4生命周期一致；D5依赖闭合/明确阻塞；D6精确生产边界可绑定；D7匹配层证据；D8单Writer可实施；D9单Reviewer可判定；D10失败恢复闭合。

| 候选 | D1 | D2 | D3 | D4 | D5 | D6 | D7 | D8 | D9 | D10 | 该行判定依据与直接消费者 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| E18-S01 | P | P | P | P | P | P | P | P | P | P | F4/T01–06：一次存储载体迁移及必要直接映射；不采时、不扩semantic validator；真实v5迁移/重开/更新保持→S03/S04/S08B |
| E18-S02 | P | P | P | P | P | P | P | P | P | P | F5、F19、F35.3：同runtime有序无损观测出口，bind/snapshot/receipt/cause属于这个输出合同；不含仓库/Recorder，owner回调→S03/S05/S06 |
| E18-S03 | P | P | P | P | P | P | P | P | P | P | F6/B01–08、F27.2：Start/active是同一canonical写入边界；冻结批次、expected tuple与完整row graph原子性不可拆成部分提交；真实Room→S05/S06/S08B |
| E18-S04 | P | P | P | P | P | P | P | P | P | P | F7/T01–06：终结外层事务同一冻结请求，有/无HR分支，CS-05复用；不含gate释放或UI，真实回滚/重试→S05/S06/S08B |
| E18-S05 | P | P | P | P | P | P | P | P | P | P | F8/G01–17：唯一仓库准入状态机及全部直接写消费者授权；cache/token/PENDING/清理顺序是同一个排他不变量，不可分别允许新Start→S06 |
| E18-S06 | P | P | P | P | P | P | P | P | P | P | F9/R01–08：同场合流与terminal/clear handoff；DB/绑定/分析语义均来自前置，不增第二owner；队列+真实仓库→三模式 |
| E18-S07A | P | P | P | P | P | P | P | P | P | P | F11/M01–07、F22/PH/PF/FI、F33/F34：唯一timed事实/predicate/focus输出，无Activity生命周期；独立原表fixtures→S07B/S08C/E22 |
| E18-S07B | P | P | P | P | P | P | P | P | P | P | F11/L01–08、F34：同一计时Activity-retained entry生产接线；MainActivity/shell/Route共同维护该entry，拆文件不能独立保留生命周期→S09/E20 |
| E18-S08A | P | P | P | P | P | P | P | P | P | P | F25、F22/F30 PL：legacy外部JSON严格接受，无Room/label/UI；required与旧optional分别可测，原文不升级→S08B |
| E18-S08B | P | P | P | P | P | P | P | P | P | P | F25/H01–06/H08、F29/F34/F35：同一事务一致terminal read，版本/执行/original/time是同一结果合法性；补S03前置，复用validators→S08C/S09/E21/E22 |
| E18-S08C | P | P | P | P | P | P | P | P | P | P | F25/H07、F30/F34：冻结历史显示/结构，无数据库mutation或当前计划回填；格式合法性与read失败层分开，resolved/unresolved证据需注明入口→S09/E21/E22 |
| E18-S09 | P | P | P | P | P | P | P | P | P | P | F12/U：必要历史入口与同源日期/删除失效；不做日历选择或HR图表；实际计时结果→已有history/UI，闭合E18完成值 |
| E19-S01 | P | P | P | P | P | P | P | P | P | P | F14/S01–05、F34：同一个力量模式接入，既有事实转换是其生产命令链步骤，不新建共享predicate authority；prepare/confirm/实际组值与生命周期同场验证→history/E20/E21 |
| E19-S02 | P | P | P | P | P | P | P | P | P | P | F14/F01–04、F34：同一个跟练模式接入，无力量前置、无视频扩张；原engine适配与retained调用端同一因果链→history/E20/E21 |
| E20-S01 | P | R | P | R | P | P | R | R | R | R | attempt=3否定D2/D4/D7/D8/D9/D10；R为F39管理修正重评，待fresh F9。F15/N01–09闭合同一coordinator的producer生命周期、版本、新进程清理；三模式及S02为必要消费者，无新owner |
| E20-S02 | P | P | P | P | P | P | P | P | P | P | attempt=3在其绑定版本上十维成立并关闭P001/P002；本版B01–B15保留，F15.1补N06–09直接消费衔接，S01前置仍阻塞。P不证明新版独立PASS或未来实现/人工验收 |
| E21-S01 | P | P | P | P | P | P | P | P | P | P | F16/J01–05、F23/F29–33/F35：同一自描述JSON结果，字段与字典是该结果不可缺的两部分；golden/mutation/独立IC，不涉文件寿命/UI→S02 |
| E21-S02 | P | P | P | P | P | P | P | P | P | P | F16/P01–05、F27/FL、F34：同一私有导出文件资源生命周期和完整preparation窗口；producer取消与清理同owner，页面/chooser接线另归S04→S04 |
| E21-S03 | P | P | P | P | P | P | P | P | P | P | F16/C01–06、R04–08/R12：同一选择页面产出冻结批次，双日期入口共享同一语义；不编码/分享→S04 |
| E21-S04 | P | P | P | P | P | P | P | P | P | P | F16/D01–05、F27/FL、F34：同一用户导出operation接平台交付，Save/Share是该操作已有两条出口；文件策略/encoder消费前置，不另建owner；真实外部reader与人工gate |
| E22-S01 | P | P | P | P | P | P | P | P | P | P | F17/Q01–06、F20/F30：纯投影单一输出，raw/anchor/predicate/性能同层；不重算analysis、不持Activity→S03/S04 |
| E22-S02 | P | P | P | P | P | P | P | P | P | P | F17/K01–03、F20：三模式共享卡的同一事实呈现，不新增统计owner；原始绑定与不足矩阵在真实结束/history入口→S03 |
| E22-S03 | P | P | P | P | P | P | P | P | P | P | F17/V01–11、F18/F20：单场竖屏分析交互，scrub/phase/focus/quality/可访问性是同一页面状态；纯投影另有前置→S04 |
| E22-S04 | P | P | P | P | P | P | P | P | P | P | F17/W01–06、F18/F20：同Activity显式横屏及返回恢复，无第二页面数据owner；方向政策/删除失效/人工使用分别可判定，承接E22最终成果gate |

逐维证据边界：

1. D1/D4不是按“都有关”合并：schema迁移、observations、Room写/终结、仓库gate、Recorder、strict legacy/read/resolver、UI/文件/投影已经分开。被同一原子性或同一页面entry约束的直接调用链保留在本Story；没有再扩大04C。
2. D2沿F24父项处置、F20的UX子项、F22–33 typed ledger、F35 QR/DA及B的accepted delta判定为候选映射闭合。主实现与direct consumer不同；REFERENCE不重做已合并资产，EXCLUDE/DEFER不悄悄恢复。**这不证明fresh source-first语义审查通过**，独立审查必须重建source universe而非相信本表P。
3. D3沿C的accepted owner/lifecycle map；F23/27的序列化及目录表达仍是候选技术表示，需正式规划接受，不因本表通过而授权Writer选择另一套结构。未发现需用户新增产品或核心ownership决定。
4. D5采用F26+F34当前24/42标签图，四个root依A已合并资产，其余前置明确未实现/未满足。按F6“已满足或明确阻塞”评估规划结构P；所有implementation prerequisite仍须届时full SHA/ancestry证明。
5. D6基于F4–F19/F25/F28的完整Integration base路径和限定delta，能在正式handoff绑定exact实际worktree；当前不允许在Integration实施。没有actual worktree/validation artifact不是可忽略项，仍属F8必须完成的执行身份门禁。
6. D7沿F34/35 matching-layer证据合同；人工UI、Band、平台外部结果仍有明确人机边界。未来证据未执行不是当前冒认PASS的理由，也不要求规划阶段运行项目测试。无法执行时保持对应Story NOT_READY/证据缺口，不降低层级。
7. D8/D9根据固定来源、AC、owner、paths与oracle可由一个角色判断，不用文件数、token或估点证明。E19两模式各自独立，不能因为都复用公共链合成一个Writer；E18-S07A因跨历史/投影共享predicate有独立consumer，和E19局部模式转换不同，不机械复制拆分数量。
8. D10的rollback/retry/migration与consumer失败已经在F4–F9、F12–17、F27/F34分层；尤其durable saved不等于RELEASED、系统dispatch不等于外部保存、source-only不等于每IO点运行，均有禁止伪成功规则。

attempt=3在F39所绑定版本判定23个Story结构成立，E20-S01六维失败；P001/P002已关闭，F38的pending陈述仅为当时历史。当前E20-S01的R列为F39管理修正重评，不能消除独立NON-PASS；未改行的P也不升级为新版独立PASS。24节点/42边不变，完整新版等待fresh F9 attempt=4，不新增正式Story、implementation candidate或Writer解锁。

### F.37 当前候选包与手工规划审查入口

本候选的唯一artifact是本exact V2，身份使用写后回验的全文SHA256，不在正文填自身hash。审阅者按本文A/B/C/D的authority层次、以下精确selector消费合同，不将F2、F3原S07/S08或后部历史快照当作当前任务。section anchor与StoryId组成候选selector；它们不是implementation commit或已接受Story。

| Epic / 数量 | 当前候选selector与主要合同 |
|---|---|
| E18 / 12 | S01/F4；S02/F5+F19+F35.3；S03/F6；S04/F7；S05/F8；S06/F9；S07A/F11.1+M07/F28；S07B/F11.2；S08A/S08B/S08C均按F25分别限定；S09/F12.2 |
| E19 / 2 | S01/F14.1；S02/F14.2；模式差异及实际接线沿F30/F34 |
| E20 / 2 | S01/F15.1/N01–N09及retained/FGS消费者；S02/F15.2+F15.3/B01–B15；F38为attempt=2修正史，F39为attempt=3完整batch及当前修正/capacity；D-082、F27.3/F28.3/F35.4的measurement/final边界共同适用 |
| E21 / 4 | S01/F16.1+F23；S02/F16.2+F27；S03/F16.3；S04/F16.4+F27；F34 IO限界必读 |
| E22 / 4 | S01–S04/F17；F18.2与F20补Q06/V06–11/W05–06，F30限定非timed导航；F27.3最终整合performance gate保留 |

共享义务索引：F20 UX子项；F24全127父索引分类；F22/23/29–33/35的field、union、variant、required-iff、duration与reason；F28 production/test完整base路径；F34接线/strict/IO；F26当前42边；F36逐项capacity。旧Story合称的子AC只按F11/F25新primary路由，不重复认领。E18-R18低空间为EXCLUDE；App比较/进阶/AI与其他residual按B/F24，不从候选包中删除其来源。

管理自检已完成可审阅的候选规划及当前已知缺口处置；**不宣称全source语义独立审查通过**。正式Planning Reviewer必须从accepted source世界反向重建expected obligations，对本包的classification/丢失/owner/AC/oracle/capacity及可达性作独立判定，发现一个finding后仍完成剩余适用轴并一次交完整batch。不能把本包的父项数、字数、hash、P列或管理结论当作独立证明。

第四轮独立规划审查及用户最终接受均已完成，精确身份见F40；本入口不再派attempt=4。下一手工门禁为F41正式计划文档落地Writer，S01代码Writer仍须完成F8。

沿已有持续推进与手工审查授权，绑定写后全文artifact hash和全部实际inputDocuments身份/分类数目，提供一份完整可手工复制的规划审查任务。不得从本索引派生其他本地artifact路径、套Code Review的实现/集成权限或让审查者改V2/merge/push。独立审查结果回传后主管理处置；正式Story/worktree/evidence身份、用户最终接受与F8 readiness仍须逐项通过。当前模型/模式不自动切换，04C继续HELD。

### F.38 F9完整finding batch处置与bounded planning Repair（2026-09-06）

#### F.38.1 终态、版本与来源层次

用户回传唯一完整`REVIEW_COMPLETE`：`reviewKind=PLANNING_REVIEW`、`role=fresh_independent_planning_reviewer`、`attempt=2`、`resumeReason=INPUT_PATH_BINDING_CORRECTION`、`selectedFunction=F9`、`mode=MANUAL_RELAY`。独立审查对象是本exact V2的修正前版本：402214 bytes，UTF-8无BOM/LF，SHA256=`3B3801457D07422C96F661550C1F181B0A2E1811FE6C036AAB43B908E536C04D`；review-base=`d2c9ac48027177389092d56c208c64447a3c6a93`。overall/SPEC/QUALITY/EVIDENCE均为NON-PASS，WriterUnlock=NONE。用户在主管理对话提交的完整报告是本节Review provenance；以下保存完整atomic batch的合同、触发、影响、证据边界和最小修正，非独立报告的新副本或新增accepted产品source。

attempt=1的F9-B001/B002是缺少路径分隔符导致的输入BLOCKED；attempt=2用已更正的正斜杠literal paths核验26项本地身份并解除，不能再次重开为当前阻断。attempt=2无客观blocker，全部适用规划轴完成、无NOT_CHECKED；仅两项must-fix，无should-fix/nice-to-have。Reviewer报告23项Story规划capacity成立，E20-S02的D2/D4/D6/D7/D8/D9/D10不足；未认定E20-S02必然过大或必须拆分。该结论只适用修正前hash；新版须fresh F9，不把原报告升级为新版已审。

主管理分类=`BOUNDED_PLANNING_REPAIR`，不是implementation defect或Code Review触发的PLANNING_ESCAPE。两项义务在原accepted source已承载，丢失发生于T1限定提取以及T4/T5的Story/AC/证据传播；F36自检未发现遗漏，F9已阻止进入T6/F8。保持B–D用户决定、五Epic价值/顺序、24候选节点和42唯一依赖、唯一owner及oracle层级；不增加产品/UX/核心owner/库，不重问已接受决定。若后续证据证明需要新增承重选择，届时回主管理，不用本分类预授权。

#### F.38.2 直接source身份与窄继承

下表文档都绑定同一review-base，从Git对象核对；working tree内容不替代immutable来源。

| ID | 完整literal路径 | 身份与本次实际使用 |
|---|---|---|
| PR-S01 | `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/docs/planning/e17-auto-reconnect-and-personal-parameters-correct-course.md` | 已有D20；SHA256=`BFC124C79064141411371660589F0D169977D20D09D8485DDB3DC2CF06B3D6DC`；§3/6/7，尤其§7当前FGS/shared-owner observer/final freshness/M1/final-source证据责任 |
| PR-S02 | `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/docs/planning/e17-heart-rate-correct-course.md` | Git blob=`605f5873a8d713bf0299426520048e000cfec72a`；当前约95/100行的runnable FGS后补测、五阶段身份链、generation分离、release未确认冻结、后台/Unknown cleanup |
| PR-S03 | `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/docs/planning/e17-4-heart-rate-implementation-readiness.md` | Git blob=`1c21474fd94e854a269a61a7e134aa4ae464daef`；页首替换边界、当前矩阵约48行及当前停止条件；旧§8.2/10.1只解释被当前条款明确承接的measurement/final与release协议术语，不整体恢复旧章节 |

D-082当前资格/恢复政策优先：合法active/paused后台unexpected_disconnect保持FGS/7200并恢复exact target；重新visible可按现有策略恢复设备。旧manual-only/no-reconnect、原Story编号/顺序、旧文件命名/提交分段与旧capacity数量均不重新成为约束。旧“当前Application为空/无Room”等盘点由Git事实替代。PR-S03 historical章节不是独立authority；只从PR-S01/02及PR-S03当前条款保留的同一承重义务追溯定义，不能从其余historical句子增添新禁令。

本次相邻源码核对：同base的 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/main/java/com/liujyks/trainflow/core/health/HeartRateFreshnessPolicy.kt` blob=`fa599c8e1126cc6c8bb958f95b5e68d7955b1da8` 全文，明确3000/2500 ms是待M1确认/替换的provisional值；`C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt` blob=`47b08345279bd669b845aeafd8cb5a5df9db2512` 全文，当前只是无BLE说明页；`C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/app/src/main/java/com/liujyks/trainflow/app/TrainFlowApplication.kt` blob=`152638d7f92d124cb122de7a014473c52d14562c` 全文，唯一HR owner创建点及当前FGS eligibility尚未接通。源码证明既有边界和拟改位置，不证明observer、FGS、M1已实现或运行。

#### F.38.3 完整atomic finding batch与主管理disposition

**F9-P001 — must-fix：FGS writer释放未确认时的承重合同未完整传播。**

- 控制来源：PR-S01 §7约124行；PR-S02约100行；PR-S03当前矩阵约48行。其generation分离、release ack/ReleaseUnconfirmed、ordinary冻结、后台/Unknown cleanup未被用户重排替代。
- 原candidate selector：F15.2/B03、B06、B08约957/960/962–964行；F28.3约1576–1577行；F36/E20-S02约2416行；F37/E20约2447行，均为修正前hash的行号。
- Primary=E20-S02；owner为Application notification coordinator/Service foreground writer交接；BLE仍由HeartRateRuntimeOwner独占。direct consumers为ordinary writer、process visibility/后台eligibility、terminal cleanup、后台保证UI以及Recorder输入链。
- 触发：同一训练多次FGS/ordinary切换且旧callback迟到；释放调用失败/未确认；Service在确认前销毁。旧合同只有有序/幂等/单writer/拒旧callback，无法排除复用同一workout token关联多次交接或未确认就恢复ordinary发布，缺稳定失败态及visibility后果，失败AC不可独立判定。
- Reviewer证据及限界：对pinned-main当前条款及候选相关术语全文交叉核对；既有通知interface/Application接线不补足上述合同。无需运行测试才能确认规划遗漏，不恢复历史全部API/恢复策略/Story顺序。
- 最小因果修正：承接generation分离、release未确认/ordinary冻结、visibility后果和失败oracle，并闭合coordinator/Service/Application直接消费者；无需新interface/owner/production hook。影响为核心责任边界、平台失败语义、证据承诺及Story义务闭合；不是新增产品范围。
- 主管理核验=`SOURCE_CONFIRMED / FIX_IN_CURRENT_V2`。本次修正B03/B06/B08、增加B09–B11，传播到N01/N05消费者说明、F27.3/F28.3、F36/F37及本节capacity。相邻同根因限定包括ack≠系统UI移除、target在pending中改变、重复/旧ack、NONE不双cancel、同workout多次handoff、process death不复用身份、失败cause不冒充用户排除。`MANAGEMENT_REPAIR_RECORDED / INDEPENDENT_CLOSURE_PENDING`；不宣称Reviewer已关闭。

**F9-P002 — must-fix：M1测量到final freshness/final APK的因果证据链未进入当前Story合同。**

- 控制来源：PR-S01 §7约124–127行；PR-S02约95/100行；PR-S03当前矩阵约48行。保留shared-owner observer、M1、final freshness、measurement/final APK身份链、final rebuild以及final-source AVD/Band责任；用户仅替代旧分解/顺序。
- 原candidate selector：F15.2/B01–B08约949–966行；F27.3约1550行；F28.3约1577行；F35.4约2385–2389行；F36/E20-S02约2416行，均为修正前hash行号。
- Primary=E20-S02；owner是同一Application HR owner的测量观察责任、既有freshness policy最终边界与FGS验收；direct consumers为Live/stale呈现、normalized acquisition cause、Recorder和final-source AVD/Band验收。
- 触发：runnable FGS完成M1后需确认/调整freshness；measurement APK和最终executable不同。旧候选只说M1由既有设备/risk profile确认，未固定observer归属、M1怎样进入final规则、最终executable变化后的重建和重跑责任；单项identity schema不能替代先后因果链。可完成一般B01–B08场景而仍漏final freshness，F36全P不足。
- Reviewer证据及限界：当前source与候选全部M1/freshness/observer位置核对；没有进行M1，也不要求当前有测量值、最终阈值数字或运行PASS；不恢复historical旧文件名/提交分段/编号。
- 最小因果修正：固定existing shared-owner测量、M1→final规则处理、measurement/final executable关系及受影响gate，传播到AC、production范围、evidence/capacity。设备身份和产物literal paths可以后续授权绑定，承重因果关系不能留给实施者猜。影响为保留freshness行为、数据状态消费者、证据承诺和容量；不要求另选架构、诊断owner或重做已完成实施。
- 主管理核验=`SOURCE_CONFIRMED / FIX_IN_CURRENT_V2`。B12–B15补完整链，F28.3补既有debug入口/freshness policy/test路径，F27.3/F35.4/F36/F37同步。相邻同根因限定包括public emission不等于raw notify、观察充分性失败回主管理、测量和final各自tuple、确认不改阈值仍需依据、所有影响executable的变化触发重建/受影响重跑、不把E22性能final gate当M1替代。`MANAGEMENT_REPAIR_RECORDED / INDEPENDENT_CLOSURE_PENDING`。

完整batch共2项，均一次处理；无遗漏/拆批/延后至Writer。本版不改其余23个Story语义；E20-S01只是S02接入后的direct consumer限定。没有在本轮核验或新增运行证据，不把Reviewer49个文档资源触及数冒称全文读取或PASS。

#### F.38.4 相邻遗漏扫描与双向传播

| source obligation族 | classification / primary | owner→AC→oracle→consumer与本次相邻扫描 |
|---|---|---|
| PR-S02/03 generation及release失败（Reviewer I15） | PRESERVE；E20-S02 | coordinator/Service→B03/B09/B10→独立顺序/失败/visibility矩阵→ordinary/Application cleanup；N01/N05不再可误用producer token |
| PR-S01/02/03 shared-owner与final chain（Reviewer I16） | PRESERVE；E20-S02 | 同Application HR owner/既有policy→B12–B15→M1、阈值理由/边界test、双APK/final gate→UI/cause/Recorder/AVD/Band |
| 失败输入和后台真实保证 | PRESERVE direct consumer；无新Story | B06/B08/B11→平台事实而非用户opt-out→F5/F19 observation、F9 Recorder。保留D-082合法后台断链恢复，不用release异常覆盖正常恢复策略 |
| 平台返回事实与用户可见结果 | PRESERVE adjacent qualifier | B09/B10→ack仅调用正常返回，不代表系统删除→同generation coordinator和平台证据分层，避免双writer/双cancel |
| 测量真实性/资源/隐私范围 | PRESERVE adjacent qualifier | B12/F28.3→既有debug说明入口变shared-owner observer，不复制独立GATT、不抢Recorder绑定、不加产品诊断面；证据只存未来授权ignored位置，无自动上传 |
| 身份变化与完成声明 | PRESERVE adjacent qualifier | B13–B15/F35.4→measurement不能覆盖变更后的final，未通过即未验收→F36/R列、F37/G手工fresh F9，不把HASH或source存在当运行PASS |

本次有界扫描轴为同notification/HR owner、release状态族、Service/platform失败、visibility/terminal、新旧generation、observer/threshold/final identity、上述direct consumers及capacity链。上述source限定均有primary和consumer；反向从每条新增B09–B15回到PR-S01/02/03和原finding，不引入新的用户功能、普通App低空间提醒、跨场AI或04C实现。此为主管理source-backed修正论证，独立完整性仍由fresh F9判定。

#### F.38.5 E20-S02修正后的十维capacity候选重评

| 维度 | 管理候选结论 | 依据与实际限界 |
|---|---|---|
| D1 单一价值 | PASS | 交付合法且经真实测量/最终源码验证的训练后台HR；runnable FGS不是独立已完成产品承诺，不能靠后续E22补足 |
| D2 义务闭合 | PASS_PENDING_F9 | B01–B15承接资格、release失败、shared-owner测量和final chain；I15/I16均映射，原遗漏不再留给Writer猜 |
| D3 owner稳定 | PASS | 已接受的Application coordinator/policy、Service foreground writer、唯一HR owner保持；observer是debug消费者，无新owner/seam |
| D4 生命周期 | PASS_PENDING_F9 | 一条FGS资格/发布权生命周期及其final证据反馈；observer随测量入口订阅/解除而不拥有BLE；release未确认/terminal/process death明确 |
| D5 前置 | PASS | 仍为E20-S01及已列Recorder/三模式前置，F26图不变；实际未实现前置保持阻塞。M1是同Story内部顺序门禁，不制造未来Story→本Story循环 |
| D6 生产边界 | PASS_PENDING_F9 | F28.3绑定coordinator/Service/Application、existing debug入口、freshness policy/test和必要debug identity field；不借此重构其他系统，正式worktree/evidence paths仍须F8绑定 |
| D7 匹配证据 | PASS_PENDING_F9 | B09–B11分pure/Android/系统表面；B12–B15为M1→policy边界→final AVD/Band；public state局限、真实观察充分性和失败终态明确，不要求本轮运行 |
| D8 单Writer | PASS_PENDING_F9 | 一个Writer可在同一candidate串行完成runnable、observer、measurement、final阈值及重建；需要用户提供实机结果是既有人工门禁，不是新产品选择。若观察不足需改核心边界，停止回管理，不能自行扩张 |
| D9 单Reviewer | PASS_PENDING_F9 | 完整输入/双身份、阈值disposition、失败矩阵与final gates可独立核对；任何缺gate即不能完成本Story，不依赖主管理口头解释 |
| D10 失败恢复 | PASS_PENDING_F9 | ReleaseUnconfirmed稳定冻结/cleanup、错误保留、generation隔离；测量不足/final gate失败不宣称完成，executable变化失效与重跑明确 |

重评为`MANAGEMENT_CANDIDATE_CAPACITY_PASS_PENDING_FRESH_F9`。保留E20-S02一个Story的因果依据是：writer交接、后台资格、同owner测量及最终freshness一起决定该后台能力能否诚实验收，内部runnable阶段不能先作为完成能力合入；不是因为“都有关”或文件少。observer没有独立产品/资源owner，final阈值只服务这次同一能力完成边界。此结论可被fresh Reviewer否定；若其证明独立owner/lifecycle或无法闭合，则按证据返回相应规划高度，不机械坚持24项或人为凑数量。

#### F.38.6 修正范围、回验与恢复合同

本次唯一修改路径为本exact V2；文本delta限定A/A.1、F15的direct contract、F27.3/F28.3/F35.4、F36/F37、新F38和G恢复状态。原始19520 bytes历史后缀必须保持SHA256=`AFBEB3A4DA473DC4011EBB9594DE7CDA76BD3BD6F9A5EA20D31BA458DB4128E9`。B–D、五Epic价值与顺序、F26及其他Story正文均保留，不改原始历史输入。

规划回归规则：对每个source声明的状态转换，逐一检查identity、authority、order、失败、direct consumer及oracle；对每条measurement→decision→final证据链，逐一检查输入是否充分、反馈归属、final executable身份、变化失效及受影响重跑。不得用父项覆盖计数代替这些限定。该规则仅本次规划检查，不修改全局技能/模板、不创建production tests。

写后只核对UTF-8无BOM/LF、历史后缀hash、唯一允许文本delta、Story/依赖图及Git保护态；这些检查不能证明独立F9或运行通过。本节旧candidate hash是审查定位，不是新版自引用hash。新版hash在手工提示词外部绑定；Reviewer必须开始/结束复核，任一内容漂移不得把本版与其他同名文件混用。

下一唯一门禁为另一名fresh independent Planning Reviewer的F9 attempt=3：完整当前包source-first review，含完整P001/P002、共同根因、direct consumers及实际相邻修改，不能只勾旧finding或扩大全仓审计。需要的原source与实际新增直接source全部列入inputDocuments；Review报告与correction使用本文件F38作为两个selector而非冒称另有本地报告。所有报告仍在对话返回；用户手工复制，主管理不派发角色。

### F.39 F9 attempt=3完整batch处置与P003有界规划修正（2026-09-06）

#### F.39.1 身份、完整终态与分类

用户回传完整REVIEW_COMPLETE：fresh_independent_planning_reviewer，independence=VALID，attempt=3，reviewReason=BOUNDED_PLANNING_REPAIR_RE_REVIEW，F9，MANUAL_RELAY；reviewBase=`d2c9ac48027177389092d56c208c64447a3c6a93`。唯一候选为本exact V2修正前429315 bytes，SHA256=`5EF1CB31ADCB5730216AF8C92CBE5EEC124E5B571CDB7505ADF8CA370288B3C4`，UTF-8无BOM/LF。overall/SPEC/QUALITY/EVIDENCE=NON-PASS；唯一must-fix=P003，无blocker/should-fix/nice-to-have；全部适用规划轴完成，无NOT_CHECKED。报告回传是Review provenance，不是新增产品authority。

P001/P002在上述版本由独立Reviewer判定RESOLVED，仅关闭规划合同；不声称FGS、observer、M1或设备结果已实现。报告34个独立义务族中I20未闭合；24 Story/240维中234 P、6 F，E20-S01失败D2/D4/D7/D8/D9/D10，其他23项结构成立，不能升级为新修正版独立PASS。报告DAG 24/42与32variant/327cell机械结果亦只绑定旧版。Reviewer无编辑、tests/build/设备/安装/Git mutation/派发；结束身份和保护态由其报告陈述，不作为主管理本次fresh证据替代。F38保留attempt=2完整chronology，其“等待attempt=3/closure pending”在本节起仅为当时状态。

#### F.39.2 完整finding及source核验

**F9-P003 — MUST-FIX：普通通知producer脱离、重接及新进程清理义务未闭合。**

- Source：F38/PR-S03即D54，完整路径 `C:/Users/25073/Desktop/jianshen/.local/worktrees/main-integration/docs/planning/e17-4-heart-rate-implementation-readiness.md`，同reviewBase Git blob=`1c21474fd94e854a269a61a7e134aa4ae464daef`，当前47行保留bounded detach/reattach及process recreation无事实时清旧7200一次；339–342行只解释当前承接的有限脱离、匹配重接/原子replacement、version floor、超时/不匹配幂等清理。不复活Route-owned架构或旧恢复政策。
- 旧candidate selector：F15.1/N01–N05（941–947行）；B08/B11只禁止旧内存事实恢复，不能替代新进程ordinary清理。Primary=E20-S01，owner=Application ordinary coordinator；直接消费者=三模式retained producer、Application lifecycle、E20-S02 ordinary/FGS交接。
- 缺口及反例：只拒旧token、忽略Route dispose、去重terminal的实现，仍可在真正producer不再返回时无限保持最后通知，或新进程空内存却不清系统遗留7200；同场replacement可能重置version而回退。均可能通过旧N01–N05。结果是通知陈旧、版本连续性和失联清理不可独立验收。S02 handoffGeneration不能补ordinary producer协议。
- Reviewer证据限界：核对当前source、候选、Application/通知contract/controller/三模式Route；现有Route-owned dispose清理不证明未来合同，未复现运行缺陷。最小修正仅承接既有保证及AC/oracle/直接消费者/capacity，不要求特定定时器、算法、owner或拆Story。若撤销保证或新增owner，须用户明确决定。
- 主管理核验：从同base Git对象独立读当前47行及330–346行的直接定义；精读本候选C、F11.2/L01–08、F14两模式、F15。配置重建保留entry/token是现有接受决定，因此旧Route detach触发位置被窄替代，有限失联结果未被撤销。普通权限拒绝清遗留ordinary为相邻344行/既有N03的完整结果，N09补明，不增加权限请求或产品能力。

分类=`BOUNDED_PLANNING_REPAIR`；最早遗漏为T1限定提取→T4/T5的Story/AC/消费者传播，F9阻止进入F8。不是Code Review/implementation触发的PLANNING_ESCAPE。保留现有owner、生命周期、产品、数据与证据层；无授权越界决定。若后续必须引入新owner、改变retained lifecycle或删除保证，暂停该分支，先向用户说明现有边界、理由、范围内方案及越界建议/影响，等用户明确决定。

#### F.39.3 修正与同根因相邻传播

| 保留义务 / disposition | 当前primary与合同 | 独立oracle及直接消费者 |
|---|---|---|
| 页面和真正producer分离 / PRESERVE+窄适配 | S01/N02/N06；F11/L02/L05及F14语义不改 | 三模式真实配置/设置往返同identity；不把Route消失、无HR新点或无状态更新当detach |
| 真实detach有限收束 / PRESERVE | S01/N06/N05；同coordinator，无新owner | attached静默不超时；真实detach窗口前/到界/重接/terminal固定预期；真实clear同步解除提交，Recorder收尾独立 |
| matching/replacement/version floor / PRESERVE | S01/N01/N07 | 不匹配旧submit无副作用；受控replacement原子失效旧token、继承floor；terminal/超时后迟到请求不复活，下一场不受旧清理影响 |
| 新进程清旧ordinary / PRESERVE | S01/N08→Application、新producer | 遗留平台通知/重复初始化/新Start顺序；真实进程证据与平台清理分支证据分开，force-stop清通知不能冒充App执行 |
| 普通权限拒绝遗留清理 / PRESERVE adjacent | S01/N03/N09 | ordinary实际权限及清理分支；训练继续，平台错误不吞；不改变FGS的权限分支 |
| 发布权/失败/后台消费者 / PRESERVE | S02消费N05–09和B09–B11；B01–B15正文保留 | detach清理与FGS/release未确认竞争，ordinary不旁路notify/cancel、不双clear；后台BLE掉线仍保持合法FGS，不误作producer detach |

通知清理不伪造持久化terminal，不替代准入、Recorder或engine责任。N06/N07保留已有协议结果，不为配置保留保证排除的状态制造生产防御。数值/内部排程只在现有coordinator内选择，证据需可确定到界；无定时器库、通用scheduler、心跳、wrapper或新测试hook授权。反向每条增量均可回到上述source/现有决定，未改E18/E19记录语义、B–D需求、B01–B15、F26或其他Epic范围。

生产和证据仍沿F15.1/F28.3既有coordinator/Application/三模式及通知test/lifecycle路径；不新增文件定位。S01须实际证明N01–09，S02须证明release交接下相同清理请求的受控结果；pure测试证明身份/顺序，真实Activity与平台证明接线和通知，AVD不证明RF。范围检查、hash和管理自检均不能替代这些未来证据。

#### F.39.4 E20-S01十维管理候选重评

| 维度 | 管理候选结论 | 依据 |
|---|---|---|
| D1 单一价值 | PASS_PENDING_F9 | 同一ordinary通知可靠协调，清理/连续性是同一结果，不新增训练能力 |
| D2 义务闭合 | PASS_PENDING_F9 | N01–09承接identity、attached/detached、重接/替换、version floor、terminal、process、权限 |
| D3 owner稳定 | PASS_PENDING_F9 | 原Application coordinator与三模式retained owner不变 |
| D4 生命周期 | PASS_PENDING_F9 | 页面保留、真实脱离、重接、terminal、新进程分别闭合；不代替Recorder |
| D5 前置 | PASS_PENDING_F9 | 原三模式前置保持明确阻塞；不新增反向S02依赖 |
| D6 生产边界 | PASS_PENDING_F9 | 现有通知边界/Application及必要producer接线；不实现FGS/BLE |
| D7 匹配证据 | PASS_PENDING_F9 | N06–09独立时序/版本oracle、真实三模式生命周期、Android遗留通知清理分层 |
| D8 单Writer | PASS_PENDING_F9 | 一套既定ordinary协议及直接producer接线；不留新owner/保证取舍给Writer |
| D9 单Reviewer | PASS_PENDING_F9 | source到正常/失败结果明确；可拒绝无限保留、版本回退、清新场及伪process证据 |
| D10 失败恢复 | PASS_PENDING_F9 | timeout/mismatch/terminal幂等与身份隔离、权限失败、新进程清理；S02不得旁路release |

这是管理候选论证，未关闭独立P003；不以文本增量少或24节点不变证明容量。E20-S02原已审合同保留，本次direct-consumer衔接仍需fresh Review，不能沿用其旧PASS替代新版审查。

#### F.39.5 修正边界与下一门禁

唯一修改本V2的A/A.1、F15.1、F36/F37、F39和G。F38完整保留为上一轮chronology；原19520 bytes历史后缀必须逐字节不变，SHA256=`AFBEB3A4DA473DC4011EBB9594DE7CDA76BD3BD6F9A5EA20D31BA458DB4128E9`。本轮不改技能/模板/测试任务、不写代码、不测试/build/设备/安装/集成/派发。

写后核验exact delta、UTF-8/LF/无BOM、后缀、F26图未变及Git保护态。新版全文身份在外部核验绑定，不填自引用hash。当前P003=`MANAGEMENT_REPAIR_RECORDED_INDEPENDENT_CLOSURE_PENDING`。下一唯一门禁是由用户手工交另一名fresh Reviewer执行完整F9 attempt=4，覆盖当前候选全部适用轴、P003及同根因/直接消费者，并检查P001/P002保留；不是只勾选一条finding。后续用户最终规划接受、F8和Writer解锁仍未完成。


<a id="source-map"></a>

## 来源到正式正文的转录核对

此表只定位已接受条款及其窄替代，不新增合同，也不以摘要替代正文。原 V1 / salvage 的旧编号作为 lineage 保留；当前分类及 owner / consumer 以 F.24 和 F.37 选择的完整合同为准。候选建议、旧容量、旧派发状态与废弃机制不升级为 authority。

| 直接来源条款 / 来源族 | 本文件正式正文 selector | 承接与窄替代边界 |
|---|---|---|
| V2 B–D、F.1 | 同名 B–D、F.1；当前状态见页首，Story 见 F.37 | E17 封口、保留资产、五成果顺序；力量与跟练分别接入验收；需求保留/暂缓/排除不改 |
| V2 F.4–F.39 | 同名原文；F.37 → 当前 24 Story，F.26 → 42 边 | 全部 Story / AC / typed / oracle / owner / consumer / capacity 原文转录；F.38/F.39 为修正 chronology |
| V2 F.40–F.42 | 页首当前状态、来源身份、路径解释及本表；六份旧文档当前入口 | 仅承接已核实审查/用户接受、文档 candidate 与条件式 landing、S01 F8 锁定；管理合同不全文复制 |
| salvage §2–§5、§12–§13 的已接受产品与 UX；UG / AD-P / AD-U / CAP / RES lineage | B、D、F.1、F.14–F.18、F.20、F.24、F.30.2；当前 owner 按 F.37 | 单场事实、三模式、图表/导出/无障碍义务保留；比较/进阶等 residual 按 B/F.24 当前处置，旧 Epic 编号不恢复 |
| salvage §14.2、§14.5–§14.31 的已接受 time / raw / original snapshot / axes / typed / owner 保证 | C、F.4–F.9、F.11、F.14、F.22–F.25、F.29–F.35 | 单调坐标、已知/未知时间、raw 与派生分离、状态/原因/NULL/partition/order、唯一 owner；继承的数学与 schema 仍指向 pinned-main E17 remainder §3–§7 |
| salvage §14.32、§14.35–§14.39 的有效性能、export、失败与隐私义务 | D、F.16、F.17、F.23、F.27、F.29、F.34、F.35 | 自描述 v2、完整生成与真实 I/O 失败、独立 consumer、平台外部副本限界；旧 lease / 清理机制只按 D/F.27 窄替代，不恢复旧容量 |
| D5 §4.1、§5.1–§5.2 | E18-S02/F.5+F.19+F.35.3；E18-S05/F.8 | 无 backlog、同值 notify 不折叠、receipt/snapshot、binding disposition、exact unbind、原始失败；不采用04C实现 |
| D5 §4.2–§4.3、§5.3–§5.8 | E18-S03/F.6、S04/F.7、S05/F.8、S06/F.9 | Start 全事务、commit-result loss完整 graph、终结幂等、cache/释放顺序、PENDING/BLOCKED、stale token、owner-clear barrier及失败保持 |
| D5 §6.1 及 lifecycle 证据分层；salvage §14.30、§14.38 | F.8–F.9、F.11、F.14、F.25、F.28、F.34 | repository/Recorder直接并发证据与真实 mode ViewModel/Activity生命周期证据分别归属；旧CS-04C/CS-06编号及12-path envelope不恢复 |
| pinned-main `docs/planning/e17-remainder-epic-story-plan.md` §3–§9 | D、F.22–F.23、F.25、F.27、F.29–F.35 | 保留引用的完整schema/数学/closed JSON/phase/export字段、正交轴、owner与性能合同；v2 root/时间/选择/系统授权和24h清理仅按已接受窄替代消费 |
| pinned-main D-079–D-084；自动恢复 Correct-course §3/§6/§7；architecture 当前覆盖；E17-3 owner / identity；E17-4 当前矩阵及其绑定的 M1 / release 协议 | B/C/F.1、F.15、F.27.3、F.28.3、F.35.4；F.38/F.39 修正说明 | 胶囊与已合并资产保留；普通/FGS唯一writer、generation分离、release ack/ReleaseUnconfirmed、shared-owner M1→final身份链仍有效；旧编号/顺序不是派发入口 |
| pinned-main `DESIGN.md` 与 `docs/ui-extension-guide.md` 的训练/复盘边界 | B、F.14、F.17–F.18、F.20、F.30、F.34；按 F.37 归属消费者 | 真实计划/实际记录、完成/提前结束、raw图表及暂停/额外休息区分、冻结胶囊与既有UI边界；不提前宣称视觉或设备门禁通过 |
