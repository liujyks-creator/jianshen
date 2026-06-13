# TrainFlow Dev Story Prompt Template

Use this template when starting a new TrainFlow development story in a fresh execution conversation.

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
- 当前阶段分支：<codex/...>
- 如存在前置合并、验证或环境阻塞，先处理前置任务；前置任务不清楚时停止并报告。

开始前确认：
- git status
- git branch --show-current
- git rev-parse --show-toplevel
- git log --oneline --decorate -6
- skills/ 是否仍未被 Git 跟踪
- .local/ 是否仍未被 Git 跟踪

本地环境恢复：
- 如果 `.local/env.ps1` 存在，先运行：
  - `. .\.local\env.ps1`
- 然后确认：
  - `java -version`
  - `.\gradlew.bat --version`
- `.local/env.ps1` 只应设置当前 PowerShell 会话的 `JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 和 `PATH`，不得提交 `.local/`。
- 如果 `.local/env.ps1` 不存在或 JDK/Android SDK 不可用，停止并报告，不要修改项目源码来硬编码本机路径。

必读状态文档：
1. AGENTS.md
2. docs/project-status.md
3. docs/roadmap-backlog.md
4. docs/readiness-report.md
5. docs/planning/decision-log.md
6. docs/planning/product-brief.md
7. docs/planning/prd.md
8. docs/planning/ux-design.md
9. docs/planning/data-contracts.md
10. docs/architecture.md
11. DESIGN.md
12. docs/ui-extension-guide.md
13. docs/setup.md
14. prototype/src/data/contracts.ts

本地技能：
- 如 skills/bmad-method/SKILL.md 存在，产品规划、架构规划、PRD/backlog/story/review 类任务先读取并遵循。
- 如 huashu-design skill 可用，UI、设计系统、主题、token、界面规则、高保真原型、设计变体或视觉评审类任务先读取并遵循；如不可用，继续以 DESIGN.md 和项目文档为准，不阻塞开发。
- skills/ 是本地辅助目录，不应提交。

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
- 是否符合允许范围
- 是否触碰禁止范围
- 是否保持状态不变量
- 是否有 blocking / must-fix / should-fix 风险
- skills/ 是否未提交
- .local/ 是否未提交
- 是否有未提交内容
- 给主管理对话的交付摘要、风险和下一步建议
- 下一轮建议：review / 修复 / 合并 main / 下一 Story
```
