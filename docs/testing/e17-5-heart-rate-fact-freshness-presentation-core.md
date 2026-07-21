# E17-5 Heart-rate Fact / Freshness / Presentation Core

**状态：** `reviewed / merged`

**Git 事实：** immutable SHA `959146a7e41a38d654b4988ba0d443f2aea0d874`；merge commit `bfb065b92d2ec78ca794fa679f7e25e85093bc79`

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

- Preparation full SHA：`e8e9d53844cc188be15d3f00aa58d61d080d4d30`
- APK 路径：`.local/smoke/e17-5-heart-rate-fact-core/trainflow-e17-5-m0-preparation-filtered-debug.apk`
- Build variant：`debug`
- Application ID：`com.liujyks.trainflow`
- Activity：`com.liujyks.trainflow.app.E17Band9HrsRevalidationActivity`
- APK SHA256：`a5566fce828e54e50316faeb9f39fe9d2ff3a41e18d22bd44a5aeb7618004885`
- APK size：`15215429` bytes
- Build / copy time：2026-07-19 01:23:32 +08:00
- adb serial / model / Android / API / install time：由用户实机测试记录

Filtered preparation commit 上的 debug Kotlin compilation、focused formatter / parser tests 与 `:app:assembleDebug` 均为 `BUILD SUCCESSFUL`。该 APK 是第一次 M0 的唯一有效 source identity；上方旧 APK 不得继续使用。

## 4. 第一次 M0 与 provisional threshold

用户使用 preparation SHA `e8e9d53844cc188be15d3f00aa58d61d080d4d30` 对应的 filtered debug APK 和当前 HUAWEI Band 9 完成三个独立前台 connection / notify cycle。六张截图已复制到 `.local/smoke/e17-5-heart-rate-fact-core/m0-first-run-screenshots/`，逐图 SHA256 和重建记录保存在未提交的本地 evidence 目录；wall-clock 前缀只用于排列截图，所有时长和 interval 均取显式 monotonic 字段。

截图时间顺序为 `21:16` connection-1、`21:17` connection-2、`21:18` / `21:19` connection-3。截图存在中段缺口，因此每周期样本数按可见下限记录，不用约 1 秒 cadence 反推未截图行：

| 周期 | notify enabled | first valid delay | 可见样本 / interval | 可审计持续时间 | interval min / median / p95 / max | 结束事实 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| connection-1 / notify-1 | `1016759832` | `949 ms` | `>=20 / 19` | first-to-last `60090 ms` | `941 / 998 / 1140 / 1140 ms` | `CLEANUP reason=user_stop` |
| connection-2 / notify-2 | `1016844850` | `253 ms` | `4 / 3` | notify-to-failure `3728 ms` | `925 / 999 / 1028 / 1028 ms` | `PLATFORM_FAILURE`, connection-state `status=19` |
| connection-3 / notify-3 | `1016922030` | `231 ms` | `>=10 / 8` | first-to-last `66993 ms` | `951 / 990 / 1081 / 1081 ms` | `CLEANUP reason=user_stop` |

三个周期合计有 30 个截图中显式可审计的 `valid_interval_ms`：min `925 ms`、median `992.5 ms`、nearest-rank p95 `1081 ms`、max `1140 ms`。三个 first-valid delay 为 `949 / 253 / 231 ms`，min `231 ms`、median `253 ms`、max `949 ms`。可见 evidence 中 malformed `0`、`EXPLICIT_DISCONNECT` `0`、platform failure `1`、intentional user cleanup `2`；由于截图缺口，两个零计数只限定为“可见 evidence 中为零”，不外推到缺失区间。真实 `status=19` 出现在 connection-2 最后一个 valid sample 后 `523 ms`，因此是独立平台失败事实，不是 silence / freshness 超时。

### 4.1 Provisional threshold 推导

- First-sample waiting boundary = `3000 ms`：`3 * max(first-valid delay) = 3 * 949 = 2847 ms`，向上取下一个 `500 ms` 边界。该余量覆盖三倍实测最坏首样本延迟。
- Live freshness boundary = `2500 ms`：`2 * max(valid interval) = 2 * 1140 = 2280 ms`，向上取下一个 `500 ms` 边界。该余量允许漏掉一个约 1 秒 notify，并覆盖实测 cadence jitter。
- 两个边界均采用严格小于语义：边界前保持 waiting / live，精确到达边界即进入 data interrupted，并清除 bpm / `measuredAt`。
- 时间流逝只形成 data interrupted；没有独立平台 / 连接失败事实时，不形成 technical failure。

这些数字只适用于 E17-5 / E17-7 当前前台 manual 能力，不表示 retry、自动恢复、锁屏 / 后台保证或最终跨生命周期数字。E17-9 M1 必须由同一Application owner的debug-only observer记录锁屏 / 后台evidence，再锁定final thresholds；E17-1、E17-5 M0、独立GATT工具或measurement APK均不能冒充final evidence，final threshold变化后必须重建最终APK并重跑受影响gate。

## 5. Core / presentation / final APK

### 5.1 Public fact / state contract

纯 Kotlin `HeartRateRuntimeFact` 分层表达 disabled、permission required、Bluetooth off、not connected、scanning、connecting、waiting first data、live、data interrupted、explicit link disconnected、technical failure 和 intentional stop，再映射到公共 `HeartRateState.fact`。Android BLE 对象、SDK model、exception / 厂商字符串和用户文案不进入 runtime fact；technical failure 使用稳定 typed reason。

只有合法 `LIVE` 可携带正 bpm 和非空 `measuredAt`。waiting、interrupted、disconnect、failure、stop、permission / Bluetooth unavailable 和 not connected 均清除当前 reading。`HeartRateState` 提供可测试的 illegal-combination validation；非法 live fact fail-closed 为 typed technical failure，不抛出 App 异常。saved identifier / display name 只作为 hint，不能建立 connected fact。

旧 `HeartRateStateKind`、`HeartRateSourceKind`、`recordedAt`、manual kind、旧 unavailable reason / message 暂留给现有 production owner 编译兼容，删除责任属于 E17-7。新胶囊 mapper 只消费 `HeartRateState.fact`；legacy manual / device kind 不能重新成为 live presentation 输入。

### 5.2 Freshness / malformed contract

`HeartRateFreshnessConfig` 显式保存 `3000 / 2500 ms` provisional 数字；timeline 只用 notify-enabled / last-valid / terminal fact 的 monotonic origin。每个 valid sample 更新 last-valid origin 和当前 reading；malformed 只饱和增加 diagnostic counter，不刷新 origin、不修改原截止点、不创建 `measuredAt`、不发布 public technical failure。首样本前 malformed 保持 waiting 到原 waiting 边界；live 后 malformed 只让旧样本活到原 freshness 边界；后续 valid 可建立新 origin。

explicit disconnect、intentional stop 和 technical failure 是互相独立的 terminal facts。负值、未来值、sample-before-notify、terminal-before-current-cycle、缺字段和非法 config 均 fail-closed 到 `INVALID_MONOTONIC_TIME`，清除 bpm / `measuredAt`。已删除 E16 retry-exhausted、10 / 15 / 30 秒和 `FIRST_SAMPLE_SILENCE` / `NOTIFY_SILENCE` technical-failure 语义。

### 5.3 Frozen capsule presentation compatibility

只修改冻结胶囊外部 mapper；`HeartRateFloatingCapsule.kt`、geometry、DTO 视觉字段、collapsed / expanded、拖动、吸附、safe zone、IME、motion、颜色、尺寸、布局和互动未改。disabled 隐藏；permission / Bluetooth / not connected / scanning / connecting / waiting 使用分离的非医疗文案；live 显示 bpm，并保留年龄区间与 over-limit 的 visual-only 语义；interrupted 不显示旧 bpm；disconnect 和 technical error 文案分离；malformed 不产生新 public error。mapper 不显示 raw identifier / address、exception message 或 BLE reason，也不宣称训练记录、后台心率或自动恢复。

### 5.4 Implementation / final APK identity

- Implementation full SHA：`b3bcac55c92e0863deeba25fc5b0491db357f7db`
- Final APK path：`.local/smoke/e17-5-heart-rate-fact-core/trainflow-e17-5-m0-implementation-b3bcac55-debug.apk`
- Variant / applicationId / Activity：`debug` / `com.liujyks.trainflow` / `com.liujyks.trainflow.app.E17Band9HrsRevalidationActivity`
- Final APK size / SHA256：`14747530` bytes / `2fad2fa2fbcf0fdc58997a338d40482757b032e0ee5862d01f452b3fd279cc65`
- Final APK forced rebuild / copy time：2026-07-19，`assembleDebug --rerun-tasks` 为 `BUILD SUCCESSFUL`；copy time `21:51:32 +08:00`
- Final device evidence：用户操作真实手机与当前 HUAWEI Band 9；截图 EXIF software 为 `Android PLU110_16.0.8.300(CN01)`，截图尺寸 `1272 x 2772`，测试时间 2026-07-19 `23:33-23:36 +08:00`。用户侧未提供 adb serial、营销型号、Android API level 或独立 install timestamp，因此 evidence 边界不扩展到这些未记录字段；安装发生在首张 final M0 截图前。
- Final M0 revalidation：完成。最终 APK 截图重建出四个独立 connection / notify cycle；三个主要周期从 notify origin 到最后可见 valid sample 分别跨 `51152 / 60288 / 12931 ms`，另有一个 `1599 ms` 短周期。
- Final screenshot subset：28 个去重后的显式 interval，min `897 ms`、median `1005 ms`、nearest-rank p95 `1080 ms`、max `1080 ms`；显式 first-valid delay 为 `310 / 577 ms`，两个较长周期的 first-valid 行被截图裁掉。可见 malformed `0`、explicit disconnect `0`、platform failure `0`；三个带 source 的 intentional cleanup 可见，connection-3 转入 connection-4 前没有可见 typed terminal 行。
- Threshold comparison：final max `1080 ms` 小于 first-M0 max `1140 ms`，final p95 `1080 ms` 与 first-M0 p95 `1081 ms` 一致；`2500 ms` freshness 比 final max 多 `1420 ms`。final first delays 均低于 first-M0 worst `949 ms`，`3000 ms` waiting 仍超过其三倍。最终分布不要求修改 provisional 数字。
- Evidence judgment：final 截图子集比初始建议的 30 个显式 interval 少 2 个，但覆盖四个 cycle，并在三个主要周期中形成超过 126 秒的 notify-origin-to-last-visible observation；结合第一次已满足门槛的 30 个 interval / 三个 first delay，足以复核最终 APK 没有推翻阈值依据。没有从截图缺口猜 interval 或 first delay；28 个 final 数字、两个 final first delay 和零 outcome 计数都按可见 evidence 限定。
- Debug tooling limitation：继承的 Activity 把控制和日志放在同一整页滚动区域，造成现场操作与截图困难。fixed controls、独立 log pane、cycle summary / export 应另拆工具改进；E17-5 明确禁止修 debug UI，且此时修改 Activity 会再次使 APK evidence 失效。
- Final M0 APK 对应的 implementation source 是 `b3bcac55c92e0863deeba25fc5b0491db357f7db`。
- Implementation source 到 Repair 前 Story tip：`b3bcac55c92e0863deeba25fc5b0491db357f7db..4e7f92c058369f37a3e9c01b3134e365d0f662b5`。three-dot / commit-range 文件结果均只包含 `docs/testing/e17-5-heart-rate-fact-freshness-presentation-core.md`，因此截至 `4e7f92c058369f37a3e9c01b3134e365d0f662b5`，Final M0 APK 之后没有 executable 变化。
- Repair 前 tip 到 Repair source：`4e7f92c058369f37a3e9c01b3134e365d0f662b5..1874d042fc93596e30d3fb87c96dabb34e02da0a`，精确包含：
  - `app/src/main/java/com/liujyks/trainflow/core/health/BleHeartRateProviderBoundary.kt`
  - `app/src/test/java/com/liujyks/trainflow/core/health/BleHeartRateProviderBoundaryTest.kt`
  - `app/src/test/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsuleStateTest.kt`
  - `docs/testing/e17-5-heart-rate-fact-freshness-presentation-core.md`
- Repair 修改了 legacy malformed public-state mapper 及其回归测试；没有修改 `E17Band9HrsRevalidationActivity`、scan、GATT、CCCD、notify 或 provisional threshold。因此原 Final M0 APK 不能描述为当前 Story tip 的完整可执行等价物。
- Repair APK SHA256 `6056804dc23b061230cca5c644ef569c51eb07b7cea546b3d48a3d34b3f56e23` 对应 Repair source `1874d042fc93596e30d3fb87c96dabb34e02da0a`。该 APK 只证明构建身份，不是新的 Band 9、AVD 或设备行为证据。
- 本次新增提交只修改本 Markdown，不会再次改变 executable / APK identity。

### 5.5 Code Review Repair：legacy malformed fail-closed

Code Review 发现 legacy compatibility adapter 在 `PARSE_FAILED + cached reading` 时会继续发布旧 `Live`。由于 legacy runtime 没有 monotonic freshness timeline，旧 wall-clock bpm / `measuredAt` 无法证明仍在原 freshness 截止点内；该路径可能永久保留旧 Live。

Repair 将 legacy `PARSE_FAILED` fail-closed 为 `DataInterrupted`：公共状态为 `DATA_INTERRUPTED`，清除旧 bpm、`measuredAt`、`recordedAt` 和其他旧测量字段，不发布公共 `TechnicalFailure`，也不刷新 freshness。后续合法 reading 仍可重新建立 `Live`，但只使用该条新 reading。`DISCONNECTED`、`STOPPED`、saved hint 和其他真正 typed technical reason 的独立映射保持不变。直接依赖旧合同的 `HeartRateFloatingCapsuleStateTest` malformed compatibility 用例已随合同迁移，断言 capsule presentation 为 `STALE`，不显示缓存 bpm，不泄漏 raw parse message，也不把 malformed 映射为 disconnect、technical failure 或 Live。

原第一次 M0 和 final M0 仍只用于支持前台 provisional `3000 / 2500 ms` threshold；本 Repair 不重写、不替换或冒充原 Band 9 evidence、APK、截图和 interval 分布。Debug `E17Band9HrsRevalidationActivity` 未修改。Repair 后重新构建的 debug APK 只证明 Repair source commit 对应的构建身份，不属于新的 Band 9 或设备行为证据。本轮不安装 APK，不运行 adb / AVD / Band 9，也不要求用户重新测试。

**Merge前历史分支快照：** Repair完成时E17-5曾为`implemented / needs review`、E17-6曾为`planned / prerequisite-gated`；该开发期状态与旧Review入口已由页首Git事实supersede，不能作为当前项目状态或下一步。

## 6. 验证与 evidence 层级

Implementation 工作树验证环境为 Eclipse Temurin JDK `17.0.19+10`、Gradle `9.4.1`、Kotlin `2.3.0`、Windows 11。以下命令均为 `BUILD SUCCESSFUL`：

- `:app:testDebugUnitTest --tests "*HeartRateFreshnessPolicyTest*"`
- `:app:testDebugUnitTest --tests "*HeartRateProviderBoundaryTest*"`
- `:app:testDebugUnitTest --tests "*BleHeartRateProviderBoundaryTest*"`
- `:app:testDebugUnitTest --tests "*HeartRateFloatingCapsuleStateTest*"`
- `:app:testDebugUnitTest --tests "*HeartRateMeasurementParserTest*"`
- `:app:testDebugUnitTest --tests "*HeartRate*"`
- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- `:app:lintDebug -Dkotlin.incremental=false`
- `:app:check`

首次 combined focused 执行在外层 2 分钟工具时限内未返回，不能作为结果；确认 wrapper 结束后重跑 combined focused 成功，并再次逐条执行上述五个 focused suite 成功。`git diff --check`、`git diff --cached --check` 和 Story scope diff check 均无 whitespace error；用户既有 dirty / untracked 禁区未 stage、未 stash、未 reset、未移动、未删除。

纯 Kotlin tests 只能证明 parser、facts、freshness 和 presentation；debug Activity + 当前真机 Band 9 M0 只能证明特定 APK、手机、Band 9、前台广播条件下的测量分布。截图不是完整 raw logcat，样本数采用可见下限，零 malformed / disconnect 只对可见 evidence 成立。AVD、fake、E17-1 evidence 均不能替代本 Story 的真实 M0；第一次 M0 也不能替代 implementation commit 对应最终 APK 的第二轮复验。

## 7. 合并状态与后续 gate

- E17-5已完成独立Review、merge/push并成为`main` ancestor；页首immutable SHA与merge commit是稳定历史事实。
- 前台provisional threshold保持waiting `3000 ms`、live `2500 ms`；E17-9仍须以shared-owner M1锁定final值。
- E17-6也已reviewed/merged；本Story旧开发期状态与Review入口仅为merge前历史分支快照。
- 当前Planning Repair为`implemented / needs review`；E17-7尚未开始，为`planned / prerequisite-gated`且只受其独立Review/merge门禁阻挡。Repair成为同步`main` ancestor且七份文档一致后E17-7 gate自动`satisfied`，不创建docs-sync或递归closeout。
- 不创建E17-5状态docs-sync或递归closeout。当前阶段只由Git ancestry、同步main与当前E17状态索引判定。
