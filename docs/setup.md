# TrainFlow 环境与工作说明

**状态:** 当前仓库环境说明
**更新日期:** 2026-05-27

本文档说明如何准备当前仓库并开始工作。
更短的跨电脑接力版本见 `docs/new-computer-setup.md`。

## 1. 当前仓库结构

当前仓库包含：

| 路径 | 用途 |
|---|---|
| `AGENTS.md` | Codex 与协作者接手项目时应遵循的工作说明。 |
| `docs/planning` | 产品简报、PRD、UX、数据契约和决策日志。 |
| `docs/architecture.md` | Android 首版技术架构、模块边界、执行引擎和平台适配边界。 |
| `docs/roadmap-backlog.md` | MVP 里程碑、Epic、Story 和验收顺序。 |
| `docs/readiness-report.md` | 实现准备检查、E0.1 启动条件和当前阻塞项。 |
| `DESIGN.md` | 官方默认 UI 设计系统 token、组件语义和界面规则。 |
| `docs/ui-extension-guide.md` | 开源社区定制主题、UI shell、首页和布局的边界。 |
| `docs/project-status.md` | 当前项目状态与建议下一步。 |
| `app` | Android 原生生产 App module，当前为 E0.4 单 module + package 边界、Room/DataStore 持久化骨架。 |
| `prototype` | React/Vite UX 原型及 TypeScript 假数据与契约。 |

当前仓库已经包含生产 Android App module。E0.4 继续采用单 `app` module 起步，在代码包和架构测试中保留后续可拆分边界；多 Gradle module 留到代码体量或依赖隔离需求明确后再拆分。

## 2. 前置环境

当前阶段需要安装：

1. Git。
2. Codex。
3. Node.js 与 npm。
4. JDK 17。
5. Android SDK Platform 36 与 Build Tools 36.0.0。

Android 生产开发建议使用 Android Studio 或等价命令行环境。当前工程使用 Gradle Kotlin DSL、Android Gradle Plugin 9.2.0、Gradle 9.4.1、Jetpack Compose + Material 3、Room 2.8.4、DataStore Preferences 1.2.1、KSP 2.3.9。

## 3. 克隆仓库

```powershell
cd $HOME\Documents
git clone https://github.com/liujyks-creator/jianshen.git
cd .\jianshen
git switch main
git pull --ff-only
git status
```

## 4. 为当前仓库配置 Git

如果这台电脑还没有合适的 Git 提交身份：

```powershell
git config user.name "liujyks-creator"
git config user.email "liujyks@gmail.com"
git config --get user.name
git config --get user.email
```

以上命令只配置当前仓库。

检查 GitHub 访问：

```powershell
git remote -v
git fetch origin
git status
```

## 5. GitHub 连通性

如果 Git 提示无法连接 `github.com:443`，先检查当前网络或代理配置。

旧开发电脑使用过的本地 HTTP 代理示例：

```powershell
git config http.proxy http://127.0.0.1:10808
git fetch origin
```

要使用当前电脑真实的代理端口。不再需要仓库级代理时移除：

```powershell
git config --unset http.proxy
```

如果 Git Credential Manager 要求 GitHub 授权，按提示完成。查看已知 GitHub 账号：

```powershell
git credential-manager github list
```

## 6. PowerShell 文本编码

本仓库文本文件统一按 UTF-8 读取和写入。Windows PowerShell 默认编码可能导致中文文档显示乱码或写入 BOM，因此每次新会话读取中文 Markdown、Kotlin、Gradle、JSON 或其他文本文件前，先设置：

```powershell
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
```

读取文件时使用显式 UTF-8：

```powershell
Get-Content -Raw -Encoding UTF8 <path>
```

代码和文档编辑优先使用 `apply_patch`。如果必须由 PowerShell 写文本文件，使用 .NET `System.Text.UTF8Encoding($false)` 写入 UTF-8 without BOM，不依赖默认 `Set-Content` 或 `Add-Content`。如果 UTF-8 读取仍异常，先只读检查 BOM 或字节特征，不要猜测内容或自动转码覆盖。

## 7. 启动前端原型

```powershell
cd .\prototype
npm.cmd install
npm.cmd run dev
```

打开 Vite 在终端输出的本地地址。常见默认地址为：

```text
http://127.0.0.1:5173
```

## 8. 验证原型改动

在 `prototype` 目录执行：

```powershell
npm.cmd run lint
npm.cmd run build
```

当 PowerShell 执行策略拦住 `npm` shim 时，使用 `npm.cmd`。

## 9. 验证 Android 工程

在仓库根目录执行：

```powershell
. .\.local\env.ps1
java -version
.\gradlew.bat --version
.\gradlew.bat tasks --all
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
```

当前开发机可以使用 ignored 的 `.local/env.ps1` 恢复本机 JDK/Android SDK 会话环境。该脚本只设置当前 PowerShell 会话中的 `JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 和 `PATH`，用于避免重启或新对话后丢失 Java/SDK 环境。

如果 `.local/env.ps1` 不存在，可以手动设置：

```powershell
$env:JAVA_HOME = "<JDK 17 path>"
$env:ANDROID_HOME = "<Android SDK path>"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

E0.4 后 Room schema 导出到 `app/schemas`，该目录是数据库迁移历史的一部分，应随相关数据库结构变更提交。不要提交 `.local/`、`.gradle/`、`app/build/`、lint report、APK 或其他构建输出。

E1.2 首批动作 fixture 变更随 `app:check` 执行 `FirstActionExerciseFixturesTest`，校验 11 个动作 ID、必填字段、训练类型能力、计时/力量默认建议、恢复映射和替代动作边界。若同时改动 `prototype/src/data/contracts.ts` 或原型 fixture，也在 `prototype` 目录执行：

```powershell
npm.cmd run lint
npm.cmd run build
```

如果 Android SDK 没有放在系统默认位置，也可以创建本地 `local.properties`：

```properties
sdk.dir=C\:/path/to/android-sdk
```

不要提交 `local.properties`、`.gradle/`、`.local/`、`app/build/` 或任何本地 SDK/JDK/构建输出。

如果网络环境需要代理才能下载 Gradle 或 Maven 依赖，可以只在当前 PowerShell 会话中设置：

```powershell
$env:GRADLE_OPTS = "-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=<port> -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=<port>"
```

## 10. 用 Codex 开始工作

用 Codex 打开仓库目录，并从以下指令开始：

```text
读取 AGENTS.md、docs/project-status.md、docs/planning/decision-log.md、docs/readiness-report.md 以及 docs/planning 下的规划文档。
在改变范围或架构前，先检查仓库状态和 prototype 原型。
```

产品主阅读顺序为：

1. `docs/project-status.md`
2. `docs/planning/decision-log.md`
3. `docs/planning/product-brief.md`
4. `docs/planning/prd.md`
5. `docs/planning/ux-design.md`
6. `docs/planning/data-contracts.md`
7. `docs/architecture.md`
8. `docs/roadmap-backlog.md`
9. `docs/readiness-report.md`
10. `DESIGN.md`
11. `docs/ui-extension-guide.md`

## 11. 分支与提交流

开始工作前：

```powershell
git switch main
git pull --ff-only
git status
```

创建任务分支：

```powershell
git switch -c codex/<task-name>
```

提交前：

```powershell
git status
git diff
```

只提交和推送本次任务需要的文件：

```powershell
git add -- <paths>
git commit -m "<short summary>"
git push -u origin HEAD
```

## 12. Codex workflow、UI 与 QA 技能

当前项目使用两个全局 workflow skill：

1. `$bmad-method`：产品/能力规划、架构决策、PRD/backlog/Story 拆分、readiness、planning Review 和 correct-course。
2. `$supervised-story-delivery`：自动交付用户已授权的 approved Story 或有限有序 Story 序列；主管理保持只读，分别调度 preflight、唯一 writer、fresh independent Review、Repair、integration 与 post-merge verification。

它们不是仓库依赖，不应复制或提交到项目 `skills/`。仓库内 ignored `skills/bmad-method` 如仍存在，只是旧本地副本，不再作为当前入口。技能不可覆盖 `AGENTS.md`、canonical workflow contract、accepted decisions、exact scope、validation、evidence 或用户权限；缺失时继续依据仓库文档和薄模板执行，不降低门禁。

`huashu-design` 继续作为独立 UI/视觉技能保留。涉及 UI、设计系统、主题、token、界面规则、高保真原型、视觉方案、视觉评审、motion 或设计变体时，应读取 `huashu-design`，并继续以 `DESIGN.md` 和项目文档作为 TrainFlow 设计真源；不得用两个 workflow skill 替代或删除它。涉及 Android UI、APK、截图反馈、交互 smoke 或虚拟设备验证时，应读取 Android emulator QA skill，并使用下方固定 `.local` 虚拟测试环境路径。

## 13. Android 虚拟测试环境

当前开发电脑的默认 Android 虚拟测试环境固定在仓库 `.local/` 下。新 Story 提示词和 Review 提示词不得省略这些路径：

- Android SDK: `C:/Users/25073/Desktop/jianshen/.local/android-sdk`
- adb: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/platform-tools/adb.exe`
- emulator: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/emulator/emulator.exe`
- AVD home: `C:/Users/25073/Desktop/jianshen/.local/android-avd`
- Android user home: `C:/Users/25073/Desktop/jianshen/.local/android-user`
- 默认 AVD: `TrainFlow_Pixel_API_36`

涉及 Android UI、执行页、计划页、记录页、APK handoff、真机截图反馈或 smoke 的任务，启动后必须先检查：

```powershell
.\.local\android-sdk\platform-tools\adb.exe devices
.\.local\android-sdk\emulator\emulator.exe -list-avds
```

如果没有 online 设备但 `TrainFlow_Pixel_API_36` 存在，应尝试启动该 AVD 后再判定无法 smoke。截图、UI tree、logcat 只写入 `.local/smoke/<story-id>/`，不得写入 `.local/verification`，也不得提交 `.local/`。
