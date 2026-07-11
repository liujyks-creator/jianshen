# TrainFlow Dev Story Prompt Template

Use this template when starting a new TrainFlow development story in a fresh execution conversation.

## Prompt Packaging

When the main control conversation sends a completed Dev Story prompt to a separate execution conversation:

- Return the entire copy-ready prompt in exactly one outer `text` code block so the user can use its top-right copy button.
- Do not place any required instruction outside that outer block.
- Do not nest triple-backtick fences inside it. Put command lines directly below a label such as `执行：`; do not create a second Markdown code block for commands.
- Do not split verification, boundary checks, or merge / handoff rules into separate code blocks.

```text
你是 TrainFlow 项目的阶段开发对话。

当前任务：
Dev Story <Story ID>: <Story 名称>

工作目录：
C:/Users/25073/Desktop/jianshen

固定规则：
- 不创建新的 git worktree。
- 不重新 clone 仓库。
- 不把工程建到临时目录或其他目录。
- 只在 C:/Users/25073/Desktop/jianshen 工作。
- 不 reset、不 rebase、不强推。
- 如确实需要新分支，只能在当前目录创建普通 git branch，并先说明原因。
- 不提交 skills/、.local/、build 输出、日志、设备输出、node_modules、dist 或本地临时文件。
- 本阶段完成后只推送阶段分支，不自行合入 main，除非主管理对话明确要求。
- 根目录 APK、`countdown_beep1.mp3`、`deliverables/`、`人工/`、`.local/`、build 输出、截图、日志和设备输出都属于禁区文件，不得 stage / commit / push。

Windows 文本编码规则：
- 本仓库文本文件统一按 UTF-8 读取和写入。
- 读取中文 Markdown、Kotlin、Gradle、JSON 或其他文本文件前，先设置：
  - `[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `$OutputEncoding = [Console]::OutputEncoding`
- 读取文件使用 `Get-Content -Raw -Encoding UTF8 <path>`，搜索优先使用 `rg`。
- 优先用 `apply_patch` 编辑代码和文档，避免 PowerShell 默认编码写入。
- 如必须用 PowerShell 写文本文件，使用 .NET `System.Text.UTF8Encoding($false)` 写入 UTF-8 without BOM；不要用默认 `Set-Content` / `Add-Content` 写中文文本。
- 如果 UTF-8 读取仍异常，不要猜测内容，不要自动转码覆盖；先只读检查 BOM/字节特征，再报告具体文件。

当前基线：
- main 状态：<main 当前 commit / 是否已推送>
- 前一阶段：<Story ID / commit / branch>
- 前一阶段 review 状态：<reviewed / changes requested / not reviewed>
- 前一阶段是否已合入 main：<是/否>
- 本 Story 的前置 commit / branch：<逐项列出；没有则写无>
- 当前阶段分支：<codex/...>
- `push`、`review accepted`、人工测试通过都不等于已合入 main；只有前置 commit / branch 已是 main ancestor 且 main 与 origin/main 同步，才解锁本 Story。
- 如存在前置合并、验证、docs sync 或环境阻塞，先处理前置任务；前置任务不清楚时停止并报告。

开始前确认：
- git fetch --prune origin
- git status
- git branch --show-current
- git rev-parse --show-toplevel
- git log --oneline --decorate -6
- git rev-parse main origin/main
- git rev-list --left-right --count main...origin/main
- 对每个前置 commit / branch 执行：git merge-base --is-ancestor <required-commit-or-branch> main
- 任一前置 ancestry 检查失败时，在创建 Story branch 或修改文件前停止并报告缺失 gate
- skills/ 是否仍未被 Git 跟踪
- .local/ 是否仍未被 Git 跟踪
- 根目录 APK、countdown_beep1.mp3、deliverables/、人工/ 是否未被 staged 或提交

本地环境恢复：
- 如果 `.local/env.ps1` 存在，先运行：
  - `. .\.local\env.ps1`
- 然后确认：
  - `java -version`
  - `.\gradlew.bat --version`
- `.local/env.ps1` 只应设置当前 PowerShell 会话的 `JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 和 `PATH`，不得提交 `.local/`。
- 如果 `.local/env.ps1` 不存在或 JDK/Android SDK 不可用，停止并报告，不要修改项目源码来硬编码本机路径。

Android 虚拟测试环境（UI / APK / 真机截图修复 / smoke 类 Story 必查）：
- 本项目默认虚拟测试环境位于当前仓库 `.local/` 下，不需要用户再次提醒：
  - Android SDK: `C:/Users/25073/Desktop/jianshen/.local/android-sdk`
  - adb: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/platform-tools/adb.exe`
  - emulator: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/emulator/emulator.exe`
  - AVD home: `C:/Users/25073/Desktop/jianshen/.local/android-avd`
  - Android user home: `C:/Users/25073/Desktop/jianshen/.local/android-user`
  - 默认 AVD: `TrainFlow_Pixel_API_36`
- 如本 Story 涉及 Android UI、截图反馈、APK handoff、执行页/计划页/记录页视觉验证或交互 smoke，必须先读取并遵循 Android emulator QA skill（如可用），然后至少执行：
  - `.\.local\android-sdk\platform-tools\adb.exe devices`
  - `.\.local\android-sdk\emulator\emulator.exe -list-avds`
- 若没有 online 设备但 `TrainFlow_Pixel_API_36` 存在，应尝试启动该 AVD 后再判断无法 smoke；若启动失败或设备保持 offline，报告具体原因，不要省略模拟测试说明。
- smoke 截图、UI tree、logcat 只能写入 `.local/smoke/<Story ID>/`，不得写入 `.local/verification`，不得提交 `.local/`。

规则与文档读取策略：
- 先运行 `rg --files -g AGENTS.md`，读取根 `AGENTS.md` 及目标目录下更具体的适用规则文件。
- 核心必读：`docs/project-status.md`、`docs/planning/decision-log.md`、本 Story 的 testing / decision / review 文档。
- 按 Story 需要补读，不要默认全量阅读不相关长文档：
  - 新产品能力、PRD、用户流程或产品决策：`docs/planning/product-brief.md`、`docs/planning/prd.md`、`docs/planning/ux-design.md`。
  - 数据契约、Room、持久化、engine、command/event、session：`docs/planning/data-contracts.md`、`docs/architecture.md`。
  - UI、Compose、布局、主题、交互、视觉修复：`DESIGN.md`、`docs/ui-extension-guide.md`、相关 HTML / design decision。
  - backlog、readiness、状态或 docs-only：`docs/roadmap-backlog.md`、`docs/readiness-report.md`。
  - 环境、Gradle、AVD、APK、adb、测试命令：`docs/setup.md`。
  - prototype：`prototype/src/data/contracts.ts` 和相关 prototype 文件。
- 当任务跨边界、现有决策不清楚或已读文档要求引用其他来源时，再扩展阅读范围。

跨对话 / 跨模型一致性：
- 不把上一模型、上一对话或主管理摘要中的隐性记忆当作事实源；以当前 main 中的已接受 decision ID、Story 文档、测试、证据和 Git ancestry 重建上下文。
- Story 提示词应引用具体 decision ID、前置 Story 文档和 required commit / branch，不使用“之前已经做过”这类不可验证表述。
- 如果提示词声称前置已 merged，但 ancestry 检查失败，必须停止；不得自行把已 push 分支当作 main 基线，也不得绕过缺失的 review / docs-sync gate。
- 如果 main 文档仍写 pending，而 Git 已合并，或文档写 merged 但 Git 未合并，先报告并完成独立 docs-sync / review，不能在矛盾状态上继续实现。

本地技能：
- 如 skills/bmad-method/SKILL.md 存在，产品规划、架构规划、PRD/backlog/story/review 类任务先读取并遵循。
- 如 huashu-design skill 可用，UI、设计系统、主题、token、界面规则、高保真原型、设计变体或视觉评审类任务先读取并遵循；生成 UI 前仍必须读取 DESIGN.md 和项目文档，不得猜颜色、间距、字号或组件规则。如不可用，继续以 DESIGN.md 和项目文档为准，不阻塞开发。
- 如 Android emulator QA skill 可用，Android UI / APK / smoke / 截图验证类任务先读取并遵循；如不可用，仍必须使用上方 `.local/android-sdk` 路径尝试 `adb devices` 和 AVD 检查。
- skills/ 是本地辅助目录，不应提交。

子代理策略（已授权，按需使用）：
- 开始前先由开发主代理做简短拆分，确定当前关键路径由谁直接负责。
- 可自行决定使用 0–3 个子代理；只在子任务彼此独立、可并行、能实质推进工作，且文件 / 模块责任范围不重叠时调用。
- 每个子代理先读取适用 `AGENTS.md`，再读取其分配范围所需的核心与任务相关文档；UI / 视觉任务不得跳过 `huashu-design`、`DESIGN.md` 和既有视觉方案。
- 适合下放：独立 provider/parser/unit test、独立 Compose UI、独立 story testing doc 草稿，或只读边界 / 证据核查。
- 不要下放：单文件或紧耦合小闭环、尚未确定的产品/架构决策、需要立即依赖结果的关键路径、最终集成、完整验证、stage、commit、push。
- 实现型子代理必须声明其拥有的文件 / 模块，不得修改其他代理负责的文件，不得回退已有改动，不得处理禁区文件。
- 子代理回交后，开发主代理必须审阅并整合其产物；共享状态文档（`project-status`、`roadmap-backlog`、`decision-log`、`readiness-report`）由开发主代理统一更新。
- 子代理不得自行 stage、commit、push、合并或批准 review。子代理不可用或不适合时，开发主代理继续单代理完成。

依赖阶段 / 模块：
- <列出本 Story 依赖的已完成阶段、文件或模块>
- <例如 E1.2 fixture、E2.2 计时计划编辑、E2.3 力量计划编辑等>

当前 Story 目标：
- <一句话说明本阶段要交付的最小可验收能力>

允许范围：
- <本阶段允许实现的能力 1>
- <本阶段允许实现的能力 2>
- <允许使用的临时策略，例如内存态、fixture、skeleton>
- <允许更新的文档>

禁止范围：
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
- UI 控制与未来 voice control 应映射到 WorkoutCommand。
- Sound、vibration、animation、analytics、future voice output 应消费 WorkoutEvent。
- HeartRateState 仍是抽象状态，不包含设备 SDK 细节。
- core/model 不得污染为 Room/DataStore entity。
- <补充本 Story 特有不变量，例如“保存仅生成内存草稿预览”>

文档要求：
- 如改变产品决策、数据契约、架构边界或 backlog 状态，必须同步更新相关文档。
- 若只是按既有 backlog 实现，可更新 docs/project-status.md 或 docs/roadmap-backlog.md。
- 如发现契约不足，记录未决事项，不要静默扩大模型。

验证命令：
- .\gradlew.bat app:testDebugUnitTest
- .\gradlew.bat app:assembleDebug
- .\gradlew.bat app:lintDebug
- .\gradlew.bat app:check
- git diff --cached --check
- 如本 Story 涉及 Android UI / APK / 视觉修复 / 交互 smoke：使用 `TrainFlow_Pixel_API_36` 或已连接设备做 adb smoke，截图保存到 `.local/smoke/<Story ID>/`；如未运行，必须说明是 AVD 缺失、设备 offline、构建失败还是流程不涉及 UI。

如果改 prototype 或前端共享配置，还必须运行：
- npm.cmd run lint
- npm.cmd run build

提交与推送：
- 完成并验证通过后提交本地 Git。
- commit message 建议：<commit message>
- 推送到 origin/<阶段分支>。
- 不自行合入 main，交给主管理对话验收和 review gate。

完成后必须报告：
- 创建或修改了哪些代码和文档
- 当前 Story 状态是什么：implemented / blocked / needs review
- commit hash 和 commit message
- 是否已推送 GitHub
- git status
- git branch --show-current
- git rev-parse --show-toplevel
- 验证命令结果
- Android 虚拟测试环境检查结果：`.local/android-sdk` / `TrainFlow_Pixel_API_36` / `adb devices` / smoke 截图路径或未运行原因
- 是否符合允许范围
- 是否触碰禁止范围
- 是否保持状态不变量
- 是否有 blocking / must-fix / should-fix 风险
- skills/ 是否未提交
- .local/ 是否未提交
- 是否有未提交内容
- 如使用子代理：各子代理的任务、修改文件、验证结果，以及开发主代理的整合检查结果
- 给主管理对话的交付摘要、风险和下一步建议
- 下一轮建议：review gate（如无 blocker / must-fix / should-fix 且验证通过，可由 review 对话直接合并 main）/ 修复 / 下一 Story
```
