# TrainFlow 新电脑继续开发指南

**适用场景:** 新电脑已经安装好 Git、Codex、Node.js/npm，准备继续开发本项目。  
**仓库:** `liujyks-creator/jianshen`  
**当前主分支:** `main`

## 1. 克隆项目

在 PowerShell 中执行：

```powershell
cd $HOME\Documents
git clone https://github.com/liujyks-creator/jianshen.git
cd .\jianshen
git switch main
git pull --ff-only
git status
```

正常情况下，最后应看到当前分支是 `main`，工作区没有未提交改动。

## 2. 配置当前项目的 Git 身份

以下命令只配置这个仓库，不修改全局 Git 配置：

```powershell
git config user.name "liujyks-creator"
git config user.email "liujyks@gmail.com"
git config --get user.name
git config --get user.email
```

如果以后想用 GitHub 隐私邮箱提交，把 `user.email` 替换成你的 GitHub noreply 邮箱即可。

## 3. 检查 GitHub 拉取和推送能力

先做只读检查：

```powershell
git remote -v
git fetch origin
git status
```

如果 `fetch` 成功，说明本地 Git 已能访问 GitHub。

第一次推送时，如果 Git Credential Manager 要求登录 GitHub，按系统弹窗完成授权即可。也可以先检查已知账号：

```powershell
git credential-manager github list
```

## 4. GitHub 网络不通时的代理处理

如果 Git 报错类似：

```text
Failed to connect to github.com port 443
```

先确认你本机代理端口。当前旧电脑使用过的 HTTP 代理示例是：

```powershell
git config http.proxy http://127.0.0.1:10808
git fetch origin
```

这个配置只会写进当前仓库。

如果新电脑的代理端口不是 `10808`，把命令里的端口换成新电脑实际端口。

确认 GitHub 已能直连后，如需移除仓库代理：

```powershell
git config --unset http.proxy
```

## 5. 打开 Codex 继续项目

用 Codex 打开本仓库目录：

```text
<你的用户目录>\Documents\jianshen
```

新会话建议先给 Codex 这段指令：

```text
先阅读 docs/planning 下的产品文档，了解 TrainFlow 当前已确定的 MVP 范围、UX 流程和数据契约。
再检查仓库状态与 prototype 原型，告诉我当前项目状态、未定事项和下一步开发建议。
```

当前应优先阅读：

1. `docs/planning/product-brief.md`
2. `docs/planning/prd.md`
3. `docs/planning/ux-design.md`
4. `docs/planning/data-contracts.md`
5. `docs/architecture.md`
6. `docs/roadmap-backlog.md`
7. `docs/readiness-report.md`
8. `DESIGN.md`
9. `docs/ui-extension-guide.md`

## 6. 配置 PowerShell 文本编码

本仓库文本文件统一按 UTF-8 读取和写入。新电脑或新 PowerShell 会话读取中文 Markdown、Kotlin、Gradle、JSON 或其他文本文件前，先设置：

```powershell
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
```

读取文件时使用：

```powershell
Get-Content -Raw -Encoding UTF8 <path>
```

代码和文档编辑优先使用 `apply_patch`。如果必须由 PowerShell 写文本文件，使用 .NET `System.Text.UTF8Encoding($false)` 写入 UTF-8 without BOM，不依赖默认 `Set-Content` 或 `Add-Content`。如果 UTF-8 读取仍异常，先只读检查 BOM 或字节特征，不要猜测内容或自动转码覆盖。

## 7. 启动前端原型

进入原型目录并安装依赖：

```powershell
cd .\prototype
npm.cmd install
```

启动开发服务器：

```powershell
npm.cmd run dev
```

按终端输出打开本地地址。Vite 常见默认地址是：

```text
http://127.0.0.1:5173
```

提交前建议执行：

```powershell
npm.cmd run lint
npm.cmd run build
```

## 8. 每次开始开发前

```powershell
cd $HOME\Documents\jianshen
git switch main
git pull --ff-only
git status
```

如果本机已经在 ignored 的 `.local/` 下准备了 JDK 和 Android SDK，可以先恢复当前 PowerShell 会话环境：

```powershell
. .\.local\env.ps1
java -version
.\gradlew.bat --version
```

`.local/env.ps1` 是本机辅助脚本，不随 Git 提交。如果新电脑没有这个脚本，请先安装或准备 JDK 17 与 Android SDK，再按 `docs/setup.md` 手动设置 `JAVA_HOME`、`ANDROID_HOME` 和 `ANDROID_SDK_ROOT`。

如果要做一块新功能，先开分支：

```powershell
git switch -c codex/<任务名>
```

示例：

```powershell
git switch -c codex/android-architecture
```

## 9. 每次完成一段工作后

先检查改动：

```powershell
git status
git diff
```

再提交：

```powershell
git add -- <需要提交的文件或目录>
git commit -m "<简短提交说明>"
git push -u origin HEAD
```

不要把未确认的临时文件、密钥、设备日志或无关改动一起提交。

## 10. 本项目相关技能

本项目使用安装在 Codex 全局技能目录中的两个 workflow skill：

1. `$bmad-method`：产品/能力规划、架构决策、PRD/backlog/Story 拆分、readiness、planning Review 和 correct-course。
2. `$supervised-story-delivery`：在用户明确授权 approved Story 或有限有序 Story 序列后，由只读主管理调度 preflight、唯一 writer、fresh independent Review、Repair、integration 与 post-merge verification。

它们不是仓库依赖，不应复制进项目 `skills/` 或提交到 GitHub。新电脑应把两个技能同步或安装到 Codex 全局 skills 目录，并重启/刷新 Codex 使其进入 active skills list。仓库内若仍存在 ignored `skills/bmad-method`，它只是旧本地副本，不再作为当前方法入口。

UI、设计系统、主题、token、界面规则、高保真原型、视觉方案、视觉评审、motion 或设计变体继续使用 Codex 环境中的 `huashu-design` skill（如可用），并继续以 `DESIGN.md` 和项目文档作为 TrainFlow 设计真源。不得删除 `huashu-design`，也不得用两个 workflow skill 取代它。

对新 Codex 会话可以这样说明：

```text
产品规划、架构规划、PRD/backlog/Story/readiness/correct-course 使用全局 $bmad-method。
自动交付已批准 Story 或有限序列时使用全局 $supervised-story-delivery；主管理保持只读，Dev/Review/Repair/integration 使用独立角色代理。
UI、设计系统、主题、token、高保真原型、视觉方案、motion 或视觉评审阶段，如 huashu-design 可用，请先读取并遵循；同时读取 DESIGN.md 和相关项目文档。
技能不存在时继续依据 AGENTS.md、canonical workflow contract、docs/planning、DESIGN.md 和相关文档，不得降低项目门禁。
```

## 11. 当前项目接力原则

换电脑后，项目状态以 GitHub 仓库内容为准：

1. 产品范围先以 `docs/planning` 为准。
2. 实现启动边界先看 `docs/readiness-report.md`。
3. 前端原型先以 `prototype` 为准。
4. 新决策要写回文档并提交到 Git。
5. 不依赖单个 Codex 会话记住所有讨论过程。

