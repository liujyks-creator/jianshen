# E16-10b-2 Foreground heart-rate reconnect controller

**状态：** Implemented / Needs review

**日期：** 2026-07-12

**分支：** `codex/e16-10b-2-foreground-reconnect-controller`

**前置：** E16-10b-1 Story tip `09d17616f213c1df7905e46662f4a195345fdd9a` 与 closeout docs-sync tip `c7fc435806f4f670b3ca513d33ff996d8e495d24` 均已是 `main` ancestor；实现起点 `main == origin/main == ccd1609549fd3d088c0b72d5e1c70898409adf97`。

## 实现结论

本 Story 新增单一、纯 Kotlin `HeartRateForegroundReconnectController`。controller 独占当前进程 runtime target generation、attempt generation、E16-10b-1 freshness timeline、2 / 5 / 10 秒 retry budget、10 秒 connect-stage watchdog、scheduler cancellation 与 foreground eligibility。production `AndroidBleHeartRateProvider` 通过 main `Handler` 串行转发 GATT callback、timer、availability、scan 与 lifecycle 事件，并执行 controller 的 direct-connect / close / publish effect；Compose 不创建 retry loop 或 freshness timer。

production monotonic clock adapter 只在 Android provider 中读取 `SystemClock.elapsedRealtime()`。纯 controller 注入 `HeartRateMonotonicClock` 与 `HeartRateControllerScheduler`；JVM tests 使用 fake clock 和可精确推进 1 ms 的 deterministic fake scheduler，不使用真实 sleep。`Instant.now()` 只继续生成 UI display metadata `measuredAt`，不参与 freshness、retry 或 watchdog。

## Runtime state 与 freshness

- notify enabled 后 `<15s` 为 waiting，`15s..<30s` 为 stale，`30s` 为 `first_sample_silence` technical error。
- valid bpm 后 `<10s` 为 live，`10s..<30s` 为 stale，`30s` 为 `notify_silence` technical error。
- valid bpm 取消旧 freshness deadline、重置 last-valid monotonic timestamp，并且只有 valid bpm 才清零 retry attempt。
- malformed payload 形成 `parse_failed` technical error，不刷新 last-valid timestamp。
- stale / offline provider state 不再携带旧 bpm 或旧 `measuredAt` 作为当前读数。
- provider state 增加稳定 `freshnessReason`、`currentReconnectAttempt`、`retryBudgetExhausted`、`reconnectInProgress` metadata；本 Story不实现 b3 的最终中文文案或视觉映射。

## Retry、watchdog 与资格

自动恢复只对本进程内实际保留、曾产生 valid live bpm 的同一 `BluetoothDevice` runtime target 生效。初始用户连接不计入预算；失败后依次等待 2 / 5 / 10 秒，最多 3 次，每次 runnable 执行前重新检查 display、permission、Bluetooth、foreground、stop suppression、scan conflict、target、ever-live、budget 与 active-attempt 资格。controller 不存在 scan effect，不按 saved identifier/displayName 发现设备，也不自动选择或切换 target。

10 秒 watchdog 只覆盖 connect / discover / characteristic / CCCD 阶段。CCCD notify enabled 后立即取消 watchdog并进入 15 / 30 秒 first-sample freshness；notify enabled 不清零 retry budget，第一条 valid bpm 才视为恢复成功。

## Race 与 intentional close

每次 `connectGatt` 创建独立 callback，并以 `BluetoothGatt` 对象身份、target generation、attempt generation 三重校验。纯 `HeartRateGattAttemptGuard` 覆盖相同地址的 old-GATT identity；旧 connection/discovery/descriptor/characteristic/disconnect、retry、watchdog 与 freshness 事件均被忽略，不消耗新 budget或覆盖新 target/live bpm。

controller 在发出 `CloseAttempt` effect 前先增加 attempt generation、取消 watchdog/freshness/retry并使旧 attempt 失效。Android adapter 随后先清空 current identity binding，再调用 `disconnect()` / `close()`；延迟 `STATE_DISCONNECTED` 无法重新建立 retry queue。

## Cancellation、scan 与 lifecycle

display off、permission lost、Bluetooth off、background、user stop、target change/clear/new selection、provider close、retry exhausted，以及 disconnected + pending retry 时的 manual scan，都会取消 backoff/watchdog/freshness并关闭必要 GATT。相应条件恢复、回到前台或 retry exhausted 后不自动恢复；必须等待新的手动连接/选择动作。

live 状态下用户扫描其他设备继续与 provider/GATT 分离，candidate/timeout 不改变当前 target或 bpm；只有选择新设备才清除旧 generation。controller 从不调用 `startScan()`。`TrainFlowApp` 只增加 `Lifecycle.Event.ON_START/ON_STOP` glue；没有 foreground service、后台 service、notification 或 overlay。

## Deterministic tests

`HeartRateForegroundReconnectControllerTest` 与现有 HeartRate tests 覆盖：

- 14,999 / 15,000 / 29,999 / 30,000 ms first-sample 边界与 9,999 / 10,000 / 29,999 / 30,000 ms valid-sample 边界；stale 不发布旧 bpm。
- new valid sample deadline reset、parse failure、2 / 5 / 10 秒三次 retry、manual attempt free、valid-bpm-only reset与 exhausted fact preservation。
- watchdog -> notify-enabled freshness handoff、old watchdog/retry/callback、same-address old-GATT identity与 intentional-close ordering。
- never-live/cold/no-runtime-target eligibility、display/permission/Bluetooth/background/stop/clear/close/scan cancellation、foreground restore/manual recovery与 live scan coexistence。
- provider stable metadata 与 stale/offline old-bpm suppression。

## Review Fix（2026-07-12）

被审 Story tip `a93cbeffa13cbb52f7199084a3051fbd6f98e7c6` 的 controller / provider race findings 已按同一 Story 边界修复，当前仍为 **Implemented / Needs review**：

- 新 target selection 与同 runtime target 的 manual attempt 已拆开。只有 `selectNewTarget()` 初始化 target generation、ever-live、timeline 与 retry budget；`beginManualAttempt()` 不消耗或清零 budget，不清除 exhausted，也不丢 ever-live。partial / exhausted budget、manual failure、notify success-before-bpm 均保留既有事实；只有 valid bpm 清零，选择新 target 才初始化新事实。
- display off、permission lost、Bluetooth off、`ON_STOP`、user stop、target switch / clear 与 provider close 均会停止实际 `BluetoothLeScanner`、移除当前 12 秒 timeout、失效 scan generation 与 queued scan callbacks。permission / Bluetooth 丢失后的 stop 使用安全幂等关闭；条件恢复只刷新 availability，不自动 scan / connect。
- manual scan 成为独立 active 状态。live scan 不关闭 GATT、不改变 target / bpm；scan active 期间 disconnect / notify silence 只记录并暂停剩余 direct-reconnect queue。scan 正常结束或 timeout 后，仅当它从 live target 开始、target generation 未变、资格仍成立、未选择新设备且 budget 可用时恢复。non-live scan、资格丢失或新 target 会永久失效旧队列。
- provider 增加 closed、lifecycle generation 与 scan generation gate。scan result / batch / failure / timeout、Bluetooth receiver / availability post、GATT callbacks、controller effect 与 state / candidate sinks 在执行前校验 open、generation，并在 GATT 路径继续校验 target / attempt / object identity。stop / close / switch / clear 失效 generation；close 幂等，close 后不更新 state / candidates 或调用 sinks。
- controller 增加 recovery cancellation epoch。retry、watchdog、freshness closure 同时校验 epoch、target、attempt / freshness generation、eligibility、scan active 与 closed；测试 scheduler 会保留并主动执行已取消 closure，证明条件后来恢复也不会 resurrect 旧 attempt。

新增或扩展的 focused JVM matrix 覆盖 partial / exhausted manual attempt、manual failure、notify-before-bpm、valid-bpm reset、新 target reset、active-scan disconnect / silence / end-resume / timeout、新 target / eligibility loss / non-live no-resume，以及 close 后 scan / receiver / availability / GATT / sink callback gate。provider scan-filter boundary test同步到每次 scan 独立 callback instance。

Review-fix AVD smoke 使用固定 `TrainFlow_Pixel_API_36` / `emulator-5554`，证据位于 `.local/smoke/e16-10b-2-foreground-reconnect-controller-review-fix/`（未提交）。已确认 active scan 后按 Home 触发 `ON_STOP`，恢复后不再显示 scanning 且不自动 connect；active scan 中关闭心率显示后立即进入“未开启”、不再显示 scanning；撤销 Bluetooth permissions 后 cold launch 无崩溃且不 scan / connect，重新授予后也不自动恢复；通过 emulator Bluetooth shell disable / enable 覆盖关闭与恢复 cold launch，未出现 TrainFlow FATAL / ANR，恢复后不自动 scan / connect。AVD 无真实 BLE HRS target，因此没有声称或伪造 Band 9 reconnect / live bpm 证据。

## AVD non-BLE smoke

固定 AVD `TrainFlow_Pixel_API_36` / `emulator-5554` 安装并启动 debug APK成功。证据位于 `.local/smoke/e16-10b-2-foreground-reconnect-controller/`，未提交。

已验证：

- display preference 关闭后 cold start 不显示心率胶囊，不出现 scanning/connecting。
- 权限未赋予状态可启动且无崩溃；foreground -> background -> foreground 不自动 scan/connect。
- 胶囊 tap / drag 不触发 scan/connect。
- 设置页现有手动 `扫描心率设备` 入口仍可用，用户动作后进入扫描并在约 12 秒结束为未发现设备。
- production launcher 为 TrainFlow MainActivity，UI tree 不出现 `HR Broadcast Smoke`。
- logcat 无 TrainFlow FATAL / ANR。

该 AVD 没有可选择并保存的 BLE HRS target，因此没有伪造“saved-device AVD reconnect”证据；cold saved identifier 绝不自动连接由既有 E16-9 boundary 与本 Story controller eligibility tests覆盖。AVD 不支持真实 Band 9 GATT 广播恢复，不能证明 2 / 5 / 10 秒真实设备 reconnect、耗尽或手动恢复；这些属于 E16-10b-4。

## 隔离确认

- 未新增/修改 Room、migration、DataStore schema、WorkoutSession、HeartRateSample、1 秒采样、records/history/trends或分析。
- 未修改 WorkoutCommand、WorkoutEvent、训练引擎、TimerDial、声音、震动、通知或 cue。
- 未新增自动 scan、自动换 target、foreground service、系统 overlay或新视觉/UI文案。
- E16-10b-3 / E16-10b-4 仍 locked / not started；E16-11 / E16-12 仍 not started。

本 Story 当前只可进入独立 E16-10b-2 Code Review gate，不得直接启动 b3、b4、E16-11 或 E16-12。controller/provider 自动测试和 AVD non-BLE smoke 通过不等于真实 Band 9 reconnect closeout 完成。
