# E17-7a Heart-rate Reconnect + Parameter Foundation

## 状态与边界

- Story 状态：`implemented / needs review`
- Accepted base：`57075700fe6754de1d1cd6dbc062ad985aa34f5a`
- Implementation commits：
  - `37b6f7007f363647c2e94b64d9174553a84814e0` — deterministic recovery policy / owner / tests
  - `bee8e2106a2019f6d3c180102f280b3ccf24d169` — personalization / persistent preferences / tests
- E17-7b 继续为 `planned / prerequisite-gated`；本 Story 没有接入 Application、settings、capsule、FGS、Service、notification、旧 runtime retirement 或设备 evidence。
- 当前新 `HeartRateRuntimeOwner` 仍只由测试实例化；旧 provider / scanner 仍是唯一 production 可达 BLE runtime。

## Recovery 合同

- Eligibility 是 opt-in、非空 saved exact target、permission、Bluetooth、无 persistent manual suppression，以及 App visible 或合法 training FGS 的合取。
- opt-out、target missing 与 manual suppression 解除 armed intent；permission、Bluetooth 与 visibility / FGS 只阻塞当前平台操作，保留恢复意图。
- eligible context 首次进入时立即启动有限 scan window；单窗口结束且未匹配时进入 `WINDOW_NO_MATCH_ARMED`，固定间隔 `10_000 ms` 后重新检查 generation、exact target、eligibility、scan 与 attempt，再开始下一窗口。
- 没有 retry count、exhaustion、backoff、watchdog 或 standalone scheduler。只使用唯一 owner 已有 main `Handler` 和新增的 recovery context generation。
- scan result 只按 identifier 精确匹配自动连接；相同名称、不同 identifier 只作为候选，不触发自动连接。
- established link 的意外断开立即进入新的 exact-target recovery window；其他明确平台 / connect failure 保持 armed，并从固定 gap 后重新尝试。
- manual scan 与 recovery scan 使用不同 origin。手动扫描不会发布 `AUTO_SEARCHING`、`WINDOW_NO_MATCH_ARMED` 或 `ARMED_WAITING`。
- opt-out、manual disconnect、permission loss、Bluetooth off、target clear、non-training background / unknown 和 permanent owner stop 都取消当前 recovery closure。旧 generation closure、旧 attempt callback 和旧 raw GATT 均不能恢复状态。
- manual disconnect 只在 owner 内形成即时 suppression；跨进程 persistence 由 DataStore 字段承担。明确重新连接或重新选择目标可通过清除该字段并提交新 context 恢复。

## Personalization 与 persistence 合同

- 新增可选 `ageYears`，sanity 范围 `1..130`；`101` 合法、可 round-trip，绝不 clamp。
- 新增可选 `personalMaxHeartRateBpm` 与 `alertThresholdBpm`，范围均为 `30..260`；无效或 `null` 输入删除各自 key，读取损坏值时 fail-closed 为 `null`。
- `heartRateManualDisconnectSuppressed` 默认 `false`，跨 DataSource recreation 保留；opt-in toggle、target clear 与参数修改不清除；明确 clear 或选择目标才清除。
- effective max 优先级为 personal max、`220 - age`、none。
- 区间使用整数交叉相乘而不取整：`<50%` 低强度、`[50,60)` 热身、`[60,70)` 燃脂、`[70,80)` 有氧、`[80,90)` 无氧、`>=90%` 极限。
- alert 为独立 strict threshold：仅 `bpm > alert` 优先进入 `OVER_LIMIT`，相等不触发；没有 effective max 时仍为 bpm-only，但 alert-only 仍可提示。
- 本 Story 只交付纯 presentation calculation，不修改冻结 capsule 本体、geometry、layout、颜色、motion 或 interaction。

## TDD 与回归证据

RED：

- recovery / personalization policy tests 先因 production symbol 不存在而编译失败。
- owner recovery tests 先因 `UpdateRecoveryContext`、`recoveryState` 与 recovery gap identity 不存在而编译失败。
- DataStore tests 先因字段、keys 与写入 API 不存在而编译失败。

GREEN：

- focused 6 suites：`75 tests / 0 failures / 0 errors / 0 skipped`
  - owner：17
  - callback identity：7
  - platform：30
  - recovery policy：4
  - personalization：6
  - DataStore boundary：11
- `*HeartRate*`：21 suites，`147 / 0 / 0 / 0`
- full `:app:testDebugUnitTest`：78 suites，`770 / 0 / 0 / 0`
- `:app:assembleDebug --rerun-tasks`：`BUILD SUCCESSFUL`
- `:app:lintDebug -Dkotlin.incremental=false`：`BUILD SUCCESSFUL`
- `:app:check`：`BUILD SUCCESSFUL`

测试环境：

- JDK：Temurin `17.0.19`
- Gradle：`9.4.1`
- owner 最终规模：1482 physical LOC、61 个 `fun` declaration；相对 E17-6 同口径新增 7 个方法，未越过 1550 LOC / `+12` methods 停止线。
- 新增 production interface、BLE seam、GATT wrapper、platform adapter、standalone scheduler、actor、第三方 dependency：均为 0。

## APK build identity

- Executable source SHA：`bee8e2106a2019f6d3c180102f280b3ccf24d169`
- Variant：`debug`
- Application ID：`com.liujyks.trainflow`
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- Bytes：`14,780,298`
- SHA256：`2C48A1AD6A5689EB5C1FD4690B23FE49C46FFD73F2A2D6B01F7CA329555D0CC5`

该 APK 只证明当前 executable tree 可构建，不是 AVD、Band 9、RF、GATT、锁屏、后台、FGS、UI 或 production wiring evidence。本 Story 按合同不运行 AVD、adb 或 Band 9；E17-7b / E17-9 仍须分别完成其 final-source 设备 gate。

## 范围、reachability 与后续门禁

- Story delta 精确为 13 个获准文件：7 个 production、5 个 tests、1 个 evidence 文档。
- 未修改 Application、Compose、settings、capsule、Manifest、Gradle、Service、notification、Route、Room、旧 provider / scanner / DTO 或 sealed E16 archive。
- 新 owner 在 `app/src/main` 只有定义，`app/src/debug` 没有实例化；直接实例化仍只存在于三个 owner test 文件。
- 用户已有 dirty / untracked 文件、`.local`、APK、音频、`deliverables/`、`人工/`、`skills/` 未纳入 Story。
- 下一步只能进行新的独立 E17-7a Code Review。最终 immutable Story SHA 通过 Review、`--no-ff` merge / push、成为同步 `main` 与 `origin/main` ancestor 且适用状态一致前，不得启动 E17-7b。
