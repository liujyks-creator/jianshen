# E17-5 Heart-rate Fact / Freshness / Presentation Core

**状态：** `implemented / needs review` 仅在本 Story 的 M0、实现、最终 APK 复验与自动验证全部完成后成立；当前为 `in progress / M0 preparation`

**日期：** 2026-07-18

## 1. 目标与范围

本 Story 先用当前 HUAWEI Band 9 和新的 debug APK 测量前台标准 HRS notify 的 monotonic M0 分布，再依据真实数据建立 E17 runtime fact 到公共 `HeartRateState` 的纯 Kotlin 合同、provisional freshness 边界，以及冻结浮动胶囊外部 presentation mapper 的兼容层。

本 Story 不实现新的 BLE runtime owner，不修改 scanner / GATT / callback ownership，不连接 Application / MainActivity / TrainFlowApp，不修改 settings 连接流程，不实现 Service、FGS、notification、ID `7200`、Room、训练记录、复盘、分析、导出、retry、backoff、自动恢复或 E17-6。`HeartRateFloatingCapsule.kt`、`HeartRateCapsuleGeometry.kt` 及其视觉、布局、尺寸、颜色、motion 和互动保持冻结。

## 2. Git 前置与用户文件基线

- 开始基线：`main == origin/main == 4b354f5116bbf7f7610e79845210d481c839fed6`，`main...origin/main = 0 0`。
- E17-4 immutable SHA `1ea67561b4866aa76c41b854da74da85c208aa25` 是 `main` ancestor；merge commit 为 `4b354f5116bbf7f7610e79845210d481c839fed6`。
- E17-3 SHA `b09ed116558eb3537fc86985b9c39b96bbbca6ff` 是 `main` ancestor。
- 禁止 E16 SHA `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor。
- 初始受保护 tracked dirty 文件：`AGENTS.md`、`CODE_REVIEW_PROMPT_TEMPLATE.md`、`DEV_STORY_PROMPT_TEMPLATE.md`、`MAIN_CONTROL_RESTART_PROMPT_TEMPLATE.md`、`docs/new-computer-setup.md`、`docs/setup.md`。
- 初始未跟踪禁区：根 APK、`countdown_beep1.mp3`、`deliverables/`、`人工/`。这些文件和 `.local/` 均不修改、不 stage、不 stash、不 reset、不移动、不删除、不提交。

## 3. M0 Preparation

### 3.1 Debug measurement contract

`E17Band9HrsRevalidationActivity.kt` 只增加测量字段和 typed outcome 日志，不改变扫描 filter、连接、service discovery、CCCD、notify 或 cleanup 流程：

- 使用 `SystemClock.elapsedRealtime()` 记录 notify enabled 与 payload receive monotonic timestamp；wall clock 只作为 logcat 可读前缀。
- 每次新 connection session 和每次 CCCD 成功后的 notify cycle 都重置 valid interval baseline。
- `VALID_SAMPLE` 记录 connection / notify cycle、source、received elapsed time、notify-enabled origin、first-valid delay、相邻 valid interval、bpm、raw payload、flags 和 bpm format。
- `MALFORMED_PAYLOAD` 记录同一身份、received elapsed time 与 raw payload，并明确 `last_valid_interval_origin_unchanged=true`；它不写回 last-valid origin。
- 当前连接收到明确 `STATE_DISCONNECTED` 时记录 `EXPLICIT_DISCONNECT`。
- scan / GATT / discovery / service / characteristic / CCCD 的明确失败使用 `PLATFORM_FAILURE` + typed stage / failure code / numeric platform status；不按 exception message 或厂商字符串分类。

### 3.2 第一版 preparation APK identity（M0 前已作废）

以下字段在 preparation commit、build 和 install 后填写：

- Preparation full SHA：`b703deae923dd293fdef03e30016fdc0723c7a89`
- APK 路径：`.local/smoke/e17-5-heart-rate-fact-core/trainflow-e17-5-m0-preparation-debug.apk`
- Build variant：`debug`
- Application ID：`com.liujyks.trainflow`
- Activity：`com.liujyks.trainflow.app.E17Band9HrsRevalidationActivity`
- APK SHA256：`1f6a640403b84db7057311b38699a07c35bb580f0f53bfa874289568d44f5dad`
- APK size：`15215429` bytes
- Build / copy time：2026-07-18 22:32:49 +08:00
- JDK：Eclipse Temurin OpenJDK `17.0.19+10`
- Gradle：`9.4.1`（Launcher / Daemon JVM 17.0.19）
- adb serial / model / Android / API / install time：pending

Preparation commit 上的 focused `E17HrsEvidenceFormatterTest` + `HeartRateMeasurementParserTest` 和 `:app:assembleDebug` 均为 `BUILD SUCCESSFUL`。第一次执行在外层 2 分钟工具时限内未返回，不能作为结果；确认孤儿 Gradle 进程退出后重跑成功。首次 `adb devices -l` 于 2026-07-18 22:33 +08:00 返回空设备列表，因此安装与真机身份记录等待用户连接 / 授权真实手机。

用户在 M0 开始前指出旧详细日志与新 measurement 日志同时滚动，不利于现场识别。为保持所有 interval 可审计同时消除重复刷新，debug Activity 的主标签 `TrainFlowE17Hrs` 和手机可见日志只保留：connection / notify cycle 起点、`VALID_SAMPLE`、`MALFORMED_PAYLOAD`、`EXPLICIT_DISCONNECT`、`PLATFORM_FAILURE` 与 `CLEANUP`。原 scan / service / characteristic / legacy `NOTIFY` 诊断保留在独立 `TrainFlowE17HrsVerbose` 标签，不改变 scanner、GATT、CCCD、notify 或 cleanup 行为。

该改动影响 debug APK 可执行代码，因此上方 `b703deae923dd293fdef03e30016fdc0723c7a89` APK 在任何 M0 取证前即作废，不得用于 threshold evidence。

### 3.3 精简日志后的 M0 preparation APK identity

- Preparation full SHA：pending filtered preparation commit
- APK 路径：`.local/smoke/e17-5-heart-rate-fact-core/trainflow-e17-5-m0-preparation-filtered-debug.apk`
- Build variant：`debug`
- Application ID：`com.liujyks.trainflow`
- Activity：`com.liujyks.trainflow.app.E17Band9HrsRevalidationActivity`
- APK SHA256 / size / build time：pending rebuild
- adb serial / model / Android / API / install time：由用户实机测试记录

## 4. 第一次 M0 与 provisional threshold

第一次 M0 必须至少尽量覆盖 3 个独立 connection / notify cycle 和总计不少于 30 个 valid intervals，并记录每周期样本数、持续时间、CCCD 到 first valid delay、valid interval min / median / p95 / max、malformed、explicit disconnect、platform failure 和 `status=19` 等真实链路观察。

当前结果：pending real Band 9 measurement。证据只存放在 `.local/smoke/e17-5-heart-rate-fact-core/`，不提交 APK、日志、截图或设备输出。M0 证据不足时停止，不修改 policy 数字，不实现 provisional thresholds，不开始 E17-6。

## 5. Core / presentation / final APK

Pending sufficient M0 evidence. 本节将在 M0 足以支持明确数值后记录阈值分布、推导公式、余量、公共 fact/state、malformed / silence / disconnect / invalid monotonic 语义、presentation 兼容、implementation commit、最终 APK 身份、最终 M0 复验和 source commit 到 Story tip 的 executable diff 检查。

Provisional 数字只适用于 E17-5 / E17-7 的前台 manual 能力；E17-9 M1 锁屏 / 后台证据仍必须重新锁定 final thresholds。它不表示自动恢复、后台保证或最终跨生命周期合同。

## 6. 验证与 evidence 层级

Pending. 纯 Kotlin tests 只能证明 parser、facts、freshness 和 presentation；debug Activity + 当前真机 Band 9 M0 只能证明该 APK、手机、Band 9、前台广播条件下的测量分布。AVD、fake、E17-1 evidence 均不能替代本 Story 的真实 M0。

## 7. 条件式状态与后续 gate

- merge 前且全部 acceptance 已完成：E17-5 = `implemented / needs review`。
- 独立 Review、merge / push、immutable Story SHA ancestry、`main...origin/main = 0 0` 与文档一致后：E17-5 自动为 `reviewed / merged`，E17-6 gate 自动 satisfied。
- 不创建 E17-5 状态 docs-sync 或递归 closeout。
- 本 Story 未完成或 M0 证据不足时，E17-6 继续 `planned / prerequisite-gated`；下一步只能是补齐 M0 或独立 E17-5 Code Review，不能开始 E17-6。
