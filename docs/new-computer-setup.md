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
7. `DESIGN.md`
8. `docs/ui-extension-guide.md`

## 6. 启动前端原型

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

## 7. 每次开始开发前

```powershell
cd $HOME\Documents\jianshen
git switch main
git pull --ff-only
git status
```

如果要做一块新功能，先开分支：

```powershell
git switch -c codex/<任务名>
```

示例：

```powershell
git switch -c codex/android-architecture
```

## 8. 每次完成一段工作后

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

## 9. 本项目相关技能

本项目可选使用两个本地技能：

1. `skills/bmad-method`
2. `skills/design-md`

它们是当前电脑上的本地工作副本，用来辅助产品方法论和设计系统规划，不属于仓库交付内容。根目录 `.gitignore` 会忽略 `skills/`，不要把这些技能目录提交到 GitHub。

新电脑继续使用它们时，任选一种方式：

1. 把技能复制到新电脑的项目目录 `skills/bmad-method` 和 `skills/design-md`。
2. 把这些技能放进你自己的私有 skills 仓库，再在新电脑安装或同步到项目目录。
3. 如果某个技能中的方法论确实需要长期沉淀进项目，只整理必要结论到 `docs/`，不要直接提交完整技能目录。

对新 Codex 会话可以这样说明：

```text
产品设计与规划阶段，如本项目目录下已存在 skills/bmad-method 和 skills/design-md，请先读取它们的 SKILL.md 再继续。
如果技能不存在，先基于仓库 docs/planning 继续，不要阻塞开发。
```

## 10. 当前项目接力原则

换电脑后，项目状态以 GitHub 仓库内容为准：

1. 产品范围先以 `docs/planning` 为准。
2. 前端原型先以 `prototype` 为准。
3. 新决策要写回文档并提交到 Git。
4. 不依赖单个 Codex 会话记住所有讨论过程。

