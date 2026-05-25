# TrainFlow 环境与工作说明

**状态:** 当前仓库环境说明
**更新日期:** 2026-05-21

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
| `prototype` | React/Vite UX 原型及 TypeScript 假数据与契约。 |

当前仓库还没有生产 Android App 模块。

## 2. 前置环境

当前阶段需要安装：

1. Git。
2. Codex。
3. Node.js 与 npm。

后续进入 Android 生产开发时，再根据最终架构和脚手架补齐 Android 开发工具。

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

## 6. 启动前端原型

```powershell
cd .\prototype
npm.cmd install
npm.cmd run dev
```

打开 Vite 在终端输出的本地地址。常见默认地址为：

```text
http://127.0.0.1:5173
```

## 7. 验证原型改动

在 `prototype` 目录执行：

```powershell
npm.cmd run lint
npm.cmd run build
```

当 PowerShell 执行策略拦住 `npm` shim 时，使用 `npm.cmd`。

## 8. 用 Codex 开始工作

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

## 9. 分支与提交流

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

## 10. 本地技能

当前项目可选使用两个本地技能：

1. `skills/bmad-method`
2. `skills/design-md`

它们是当前电脑的本地工作副本，不是仓库依赖，也不应提交到 GitHub。根目录 `.gitignore` 已忽略 `skills/`。

如果当前电脑存在这些技能，可在产品规划、架构规划、PRD/backlog 拆分或设计系统规划时先读取对应 `SKILL.md`。若不存在，继续以仓库文档为准，并把新的长期决策写回 `docs/planning/decision-log.md`。
