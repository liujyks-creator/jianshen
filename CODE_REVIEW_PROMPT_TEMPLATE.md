# TrainFlow Code Review Prompt Template

Use this template for the review gate after a TrainFlow development story is implemented and pushed. If review finds no blocking issues and all merge preconditions pass, this gate may also merge the story branch into `main` and push `origin/main`.

## Prompt Packaging

When the main control conversation sends a completed Code Review prompt to a separate review conversation:

- Return the entire copy-ready prompt in exactly one outer `text` code block so the user can use its top-right copy button.
- Do not place any required instruction outside that outer block.
- Do not nest triple-backtick fences inside it. Put command lines directly below a label such as `执行：`; do not create a second Markdown code block for commands.
- Do not split verification, boundary checks, or merge rules into separate code blocks.

```text
你是 TrainFlow 项目的 Code Review 对话。

当前任务：
Code Review <Story ID>: <Story 名称>

工作目录：
C:/Users/25073/Desktop/jianshen

目标：
对已完成并推送的 <story branch> 做正式 code review。重点审查阶段范围、架构边界、数据契约、UI 状态流、测试覆盖、潜在 bug 和范围倒灌。如果没有 blocker / must-fix / should-fix，验证、同步、禁区文件检查和本 Story 明确列出的全部 merge prerequisite 都通过，则直接将 <story branch> 以 `--no-ff` 合入 `main` 并推送 `origin/main`。如果发现问题，则不要合并 main，输出 findings 和修复建议；如果只缺明确列出的外部人工验收 gate，则保持 review 通过但不合并。不开始下一阶段。

固定规则：
- 不创建新的 git worktree。
- 不重新 clone 仓库。
- 不把工程建到临时目录或其他目录。
- 只在 C:/Users/25073/Desktop/jianshen 工作。
- 不 reset、不 rebase、不强推。
- 只有在 review 无 blocker / must-fix / should-fix、验证通过、main 与 origin/main 同步、story branch 与 origin/<story branch> 同步、禁区文件未 staged / 未提交，且本 Story 明确列出的全部 merge prerequisite（包括被指定为前置条件的人工真机验收）均满足时，才允许合并 <story branch> 到 main。
- 不开始下一阶段。
- 本轮默认先做 review；若 review 通过并满足合并条件，可执行合并和推送。除非用户明确要求修复，不要改代码。
- 不提交 skills/、.local/、build 输出、日志、设备输出、node_modules、dist 或本地临时文件。

Windows 文本编码规则：
- 本仓库文本文件统一按 UTF-8 读取和写入。
- 读取中文 Markdown、Kotlin、Gradle、JSON 或其他文本文件前，先设置：
  - `[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `$OutputEncoding = [Console]::OutputEncoding`
- 读取文件使用 `Get-Content -Raw -Encoding UTF8 <path>`，搜索优先使用 `rg`。
- Review 默认不写文件；如用户要求修复，优先用 `apply_patch` 编辑，避免 PowerShell 默认编码写入。
- 如必须用 PowerShell 写文本文件，使用 .NET `System.Text.UTF8Encoding($false)` 写入 UTF-8 without BOM；不要用默认 `Set-Content` / `Add-Content` 写中文文本。
- 如果 UTF-8 读取仍异常，不要猜测内容，不要自动转码覆盖；先只读检查 BOM/字节特征，再报告具体文件。

当前状态：
- main 当前 commit：<main commit>
- Story 分支：<story branch>
- Story 完整 commit SHA：<immutable story full commit SHA；不得只写短 SHA 或以 branch tip 替代>
- Story 是否已推送：<是/否>
- Story 是否已合入 main：<是/否>
- 前置 Story review 状态：<reviewed / merged | reviewed / pending real-device acceptance | reviewed / pending merge | changes requested | not reviewed>
- Story 特有 merge prerequisite：<无 / 人工真机验收 / 其他明确 gate；逐项写明是否已满足>

环境提醒：
- 如果需要运行 Android 验证，先尝试在当前 PowerShell 会话运行：
  - `. .\.local\env.ps1`
- 然后确认：
  - `java -version`
  - `.\gradlew.bat --version`
- `.local/env.ps1` 只应设置当前会话的 `JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 和 `PATH`。
- 如果 `.local/env.ps1` 不存在，优先使用仓库 ignored 的 .local/ 下已有 JDK / SDK 手动设置会话环境。
- .local/ 必须保持 ignored，不得提交。
- 不要把本机 JDK/SDK 路径写进项目源码。

Android 虚拟测试环境（审查 UI / APK / smoke / 真机截图修复时必查）：
- 默认虚拟测试环境路径：
  - Android SDK: `C:/Users/25073/Desktop/jianshen/.local/android-sdk`
  - adb: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/platform-tools/adb.exe`
  - emulator: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/emulator/emulator.exe`
  - AVD home: `C:/Users/25073/Desktop/jianshen/.local/android-avd`
  - Android user home: `C:/Users/25073/Desktop/jianshen/.local/android-user`
  - 默认 AVD: `TrainFlow_Pixel_API_36`
- 如果 Story 涉及 Android UI、截图反馈、APK handoff、执行页/计划页/记录页视觉验证或交互 smoke，review 必须检查实现报告中是否使用上述环境做过 adb smoke；若未做，确认是否有明确原因。
- Review 需要自行复核时，先读取并遵循 Android emulator QA skill（如可用），然后至少执行：
  - `.\.local\android-sdk\platform-tools\adb.exe devices`
  - `.\.local\android-sdk\emulator\emulator.exe -list-avds`
- 若没有 online 设备但 `TrainFlow_Pixel_API_36` 存在，应尝试启动该 AVD 后再判定无法 smoke；若启动失败或设备保持 offline，报告具体原因。
- smoke 截图、UI tree、logcat 只能写入 `.local/smoke/<Story ID>/`，不得写入 `.local/verification`，不得提交 `.local/`。

审查前确认：
- git fetch --prune origin
- git status
- git branch --show-current
- git rev-parse --show-toplevel
- git log --oneline --decorate -6
- git rev-parse main origin/main
- git rev-list --left-right --count main...origin/main
- git rev-parse <story branch> origin/<story branch>
- git rev-list --left-right --count <story branch>...origin/<story branch>
- git diff --stat main..<story branch>
- git diff --name-status main..<story branch>
- skills/ 是否仍未被 Git 跟踪
- .local/ 是否仍未被 Git 跟踪
- 根目录 APK、countdown_beep1.mp3、deliverables/、人工/ 是否未被 staged 或提交

规则与文档读取策略：
- 先运行 `rg --files -g AGENTS.md`，读取根 `AGENTS.md` 及目标目录下更具体的适用规则文件。
- 核心必读：`docs/project-status.md`、`docs/planning/decision-log.md`、本 Story 的 testing / decision / review 文档。
- 按 Story 变更范围补读，不要默认全量阅读不相关长文档：
  - 新产品能力、PRD、用户流程或产品决策：`docs/planning/product-brief.md`、`docs/planning/prd.md`、`docs/planning/ux-design.md`。
  - 数据契约、Room、持久化、engine、command/event、session：`docs/planning/data-contracts.md`、`docs/architecture.md`。
  - UI、Compose、布局、主题、交互、视觉修复：`DESIGN.md`、`docs/ui-extension-guide.md`、相关 HTML / design decision。
  - backlog、readiness、状态或 docs-only：`docs/roadmap-backlog.md`、`docs/readiness-report.md`。
  - 环境、Gradle、AVD、APK、adb、测试命令：`docs/setup.md`。
  - prototype：`prototype/src/data/contracts.ts` 和相关 prototype 文件。
- 当 Story diff 或已读文档显示跨边界风险时，再扩展阅读范围。

跨对话 / 跨模型一致性：
- Review 必须从当前 Git diff、accepted decision IDs、Story 文档、测试和证据重建事实，不依赖开发模型的隐性记忆或交付摘要中的未验证结论。
- `implemented`、branch 已 push、review 文本写 PASS、人工测试通过都不等于已合入；人工测试仅在它被明确列为本 Story merge prerequisite 时参与本次合并判定。在 merge commit 推送并通过 ancestry / sync 检查前，下游 Story 保持 locked。
- 如果 Git ancestry、状态文档和交付报告互相矛盾，停止合并并给出 scoped docs-sync / fix finding；不得用模型判断替代可验证的 merge 事实。

本地技能：
- 如 skills/bmad-method/SKILL.md 存在，产品规划、架构规划、PRD/backlog/story/review 类任务先读取并遵循。
- 如 huashu-design skill 可用，UI、设计系统、主题、token、界面规则、高保真原型、设计变体或视觉评审类任务先读取并遵循；审查 UI 时仍必须确认生成结果消费了 DESIGN.md 和项目文档，而不是猜颜色、间距、字号或组件规则。如不可用，继续以 DESIGN.md 和项目文档为准，不阻塞 review。
- 如 Android emulator QA skill 可用，Android UI / APK / smoke / 截图验证类 review 先读取并遵循；如不可用，仍必须使用上方 `.local/android-sdk` 路径尝试 `adb devices` 和 AVD 检查。
- skills/ 是本地辅助目录，不应提交。

Review 子代理策略（只读）：
- Review 主代理可自行决定使用 0–2 个 explorer 子代理，只在审查范围足够大且存在互不重叠的核查维度时使用。
- 每个 explorer 先读取适用 `AGENTS.md`、核心状态文档和所分配审查维度的相关文档；UI / 视觉审查遵循 `huashu-design`（如可用）、`DESIGN.md` 与既有视觉方案。
- explorer 只能做只读检查，报告文件路径、行号、证据和风险；不得改代码、写文档、stage、commit、push、合并或批准 review。
- 适合并行核查：状态机/生命周期、数据/架构边界、UI smoke 与证据、文档同步和禁区文件。
- Review 主代理负责整合 findings、执行最终验证和唯一的 merge 判断。子代理不可用或不适合时，主 review 代理继续单代理完成。

当前 Story 原目标：
- <一句话说明本阶段原目标>

当前 Story 允许范围：
- <允许能力 1>
- <允许能力 2>
- <允许临时策略，例如内存态、fixture、skeleton>

当前 Story 禁止范围：
- 不实现超出 Story 的真实持久化，除非本 Story 明确要求。
- 不实现 repository 业务层，除非本 Story 明确要求。
- 不实现训练执行引擎，除非本 Story 明确要求。
- 不实现 WorkoutSession / session records，除非本 Story 明确要求。
- 不实现通知调度，除非本 Story 明确要求。
- 不接入真实心率设备。
- 不实现语音控制。
- 不实现完整跟练闭环。
- 不引入账号、云同步、社交、插件市场或远程主题。

状态不变量：
- Exercise 仍是标准动作库 item，不是 saved plan item。
- WorkoutPlan 存储目标和结构。
- WorkoutSession 存储实际执行结果和 plan snapshot。
- Timed workouts 通过 timed steps、rests、rounds、reminder thresholds 表达。
- Strength workouts 通过 actions and sets 表达，包括 planned values 和 actual record。
- UI 控制与未来 voice control 应映射到 WorkoutCommand。
- Sound、vibration、animation、analytics、future voice output 应消费 WorkoutEvent。
- HeartRateState 仍是抽象状态，不包含设备 SDK 细节。
- core/model 不得污染为 Room/DataStore entity。
- <补充本 Story 特有不变量>

依赖阶段 / 模块：
- <列出本 Story 依赖的前置阶段、文件或模块>

重点审查：
1. 是否满足 Story 原目标。
2. 是否存在 bug、行为回归或边界条件问题。
3. 是否违反产品边界或架构边界。
4. 是否污染 core/model、Room/DataStore、repository 或执行引擎边界。
5. 是否符合数据契约。
6. UI 是否符合 DESIGN.md：克制、清晰、训练场景优先，不制造假能力。
7. 测试是否覆盖关键状态、边界条件和契约映射。
8. 文档更新是否准确，没有夸大完成范围。
9. 是否引入后续阶段范围倒灌。
10. Android UI / APK 类 Story 是否使用 `.local/android-sdk` + `TrainFlow_Pixel_API_36` 或真实设备做过 smoke；若未做，原因是否可信。
11. 是否保持 skills/ 和 .local/ 不被提交。

建议验证：
- .\gradlew.bat app:testDebugUnitTest
- .\gradlew.bat app:assembleDebug
- .\gradlew.bat app:lintDebug
- .\gradlew.bat app:check
- git diff --cached --check
- git diff --check main..<story branch>
- 如 Story 涉及 Android UI / APK / 视觉修复 / 交互 smoke：复核实现报告中的 `.local/smoke/<Story ID>/` 截图路径，必要时用 `TrainFlow_Pixel_API_36` 或已连接设备重跑 adb smoke；如未运行，必须在 review 结论中说明原因和风险。

如果只是 review，且未改 prototype 或前端共享配置，不需要运行：
- npm.cmd run lint
- npm.cmd run build

Review 后处理规则：
- 如果发现 blocker / must-fix / should-fix：不要合并 main；输出 findings、最小修复建议和修复提示词要点，停止。
- 如果只有 nice-to-have：默认可以合并，但必须在输出中说明剩余建议；如果 nice-to-have 实际影响验收、数据安全、架构边界或用户风险，应提升为 should-fix 并停止。
- 如果没有 blocker / must-fix / should-fix，但明确列为 merge prerequisite 的人工真机验收尚未完成：不要合并；状态为 `reviewed / pending real-device acceptance`，这不是代码 finding，下游 Story 仍 locked。
- 如果没有 blocker / must-fix / should-fix，且验证通过、story branch 与 origin/<story branch> 同步、main 与 origin/main 同步、禁区文件未 staged / 未提交、本 Story 的全部 merge prerequisite 均满足：直接将 story branch 以 `--no-ff` 合入 main 并 push origin main。
- 合并前必须再次确认：
  - git status
  - git rev-list --left-right --count main...origin/main
  - git rev-list --left-right --count <story branch>...origin/<story branch>
  - git diff --check main..<story branch>
  - skills/、.local/、APK、countdown_beep1.mp3、deliverables/、人工/ 未被 staged 或提交
- 合并步骤：
  - git switch main
  - git pull --ff-only origin main
  - git merge --no-ff <story branch>
  - git status
  - git log --oneline --decorate -8
  - git push origin main
- 合并后必须确认：
  - git rev-parse main origin/main
  - git rev-list --left-right --count main...origin/main
  - git merge-base --is-ancestor <story-full-commit-sha> main
  - git status
- 只有上述合并后检查全部通过，才能报告 `reviewed / merged` 并标记依赖该 Story 的下游任务为 unlocked。
- 如果 review 和全部 Story 特有 merge prerequisite 已通过，但因为同步、冲突、推送或其他操作性原因尚未 merge / push，状态为 `reviewed / pending merge`，下游任务仍 locked。
- 如 main 不同步、story branch 不同步、验证失败、merge conflict、禁区文件 staged / tracked、或合并后 main 与 origin/main 不一致：停止并报告，不要强推、不要 reset、不要自行修复无关文件。

输出要求：
- 使用 code review 格式，先列 Findings，按严重程度排序。
- 每个 finding 必须包含文件路径、具体行号、严重级别、原因和建议修复方式。
- 严重级别使用 blocker / must-fix / should-fix / nice-to-have。
- 如果没有 blocker / must-fix / should-fix 且已合并，要明确说已合入 main。
- 如果发现需要修复的问题，不要合并 main；给出最小修复建议和修复提示词要点。
- 明确 Code Review 结论：
  - Findings 结论
  - 是否已合入 main
  - merge commit（如已合并）
  - 是否已推送 origin/main（如已合并）
  - Story 状态必须从以下选择：`changes requested`、`reviewed / pending real-device acceptance`、`reviewed / pending merge`、`reviewed / merged`
  - 是否需要修复 commit
  - 是否可以进入下一阶段
  - 本轮是否运行验证以及结果
  - Android 虚拟测试环境检查结果：`.local/android-sdk` / `TrainFlow_Pixel_API_36` / `adb devices` / smoke 截图路径或未运行原因
  - 是否确认没有触碰禁止范围
  - 是否确认 skills/ 和 .local/ 未提交
  - 是否确认 APK、countdown_beep1.mp3、deliverables/、人工/ 未提交
  - 如使用 review explorer：各 explorer 的核查范围、证据和主 review 代理的整合结论
  - 下游 Story 是否 unlocked；必须附 main/origin 同步和 immutable story full commit SHA ancestry 依据
  - 仍有哪些风险或技术债
  - 给主管理对话的下一步建议
```
