# TrainFlow Code Review Prompt Template

Use this template for the review gate after a TrainFlow development story is implemented and pushed.

```text
你是 TrainFlow 项目的 Code Review 对话。

当前任务：
Code Review <Story ID>: <Story 名称>

工作目录：
C:/Users/25073/Desktop/jianshen

目标：
对已完成并推送的 <story branch> 做正式 code review。重点审查阶段范围、架构边界、数据契约、UI 状态流、测试覆盖、潜在 bug 和范围倒灌。默认只做 review，不合并 main，不开始下一阶段。

固定规则：
- 不创建新的 git worktree。
- 不重新 clone 仓库。
- 不把工程建到临时目录或其他目录。
- 只在 C:/Users/25073/Desktop/jianshen 工作。
- 不 reset、不 rebase、不强推。
- 不合并 <story branch> 到 main。
- 不开始下一阶段。
- 本轮默认只做 review。除非用户明确要求修复，不要改文件。
- 不提交 skills/、.local/、build 输出、日志、设备输出、node_modules、dist 或本地临时文件。

当前状态：
- main 当前 commit：<main commit>
- Story 分支：<story branch>
- Story commit：<story commit>
- Story 是否已推送：<是/否>
- Story 是否已合入 main：<是/否>
- 前置 Story review 状态：<reviewed / changes requested / not reviewed>

环境提醒：
- 如果需要运行 Android 验证，可能需要先在当前 PowerShell 会话恢复本地 JDK / Android SDK。
- 优先使用仓库 ignored 的 .local/ 下已有 JDK / SDK。
- .local/ 必须保持 ignored，不得提交。
- 不要把本机 JDK/SDK 路径写进项目源码。

审查前确认：
- git status
- git branch --show-current
- git rev-parse --show-toplevel
- git log --oneline --decorate -6
- git diff --stat main..<story branch>
- git diff --name-status main..<story branch>
- skills/ 是否仍未被 Git 跟踪
- .local/ 是否仍未被 Git 跟踪

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
- 如 skills/design-md/SKILL.md 存在，UI、设计系统、主题、token、界面规则类任务先读取并遵循。
- skills/ 是本地辅助目录，不应提交。

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
10. 是否保持 skills/ 和 .local/ 不被提交。

建议验证：
- .\gradlew.bat app:testDebugUnitTest
- .\gradlew.bat app:assembleDebug
- .\gradlew.bat app:lintDebug
- .\gradlew.bat app:check
- git diff --cached --check
- git diff --check main..<story branch>

如果只是 review，且未改 prototype 或前端共享配置，不需要运行：
- npm.cmd run lint
- npm.cmd run build

输出要求：
- 使用 code review 格式，先列 Findings，按严重程度排序。
- 每个 finding 必须包含文件路径、具体行号、严重级别、原因和建议修复方式。
- 严重级别使用 blocker / must-fix / should-fix / nice-to-have。
- 如果没有 blocker 或 must-fix，要明确说可以合入 main。
- 如果发现需要修复的问题，不要合并 main；给出最小修复建议。
- 明确 Code Review 结论：
  - 是否建议合入 main
  - Story 状态应为 implemented、changes requested，还是 reviewed
  - 是否需要修复 commit
  - 是否可以进入下一阶段
  - 本轮是否运行验证以及结果
  - 是否确认没有触碰禁止范围
  - 是否确认 skills/ 和 .local/ 未提交
  - 仍有哪些风险或技术债
  - 给主管理对话的下一步建议
```
