# E16-10b-1 Heart-rate freshness policy core

**状态：** Reviewed / merged

**日期：** 2026-07-11

**前置：** E16-10a reviewed / merged to `main` at `56d8029719889d329680f3dc099a77ae94909142`

**Story tip：** `09d17616f213c1df7905e46662f4a195345fdd9a`

**Merge commit：** `5cdee7ce1bd7a2b0f76f83adf069179a547fd16c`

**范围：** 纯 Kotlin monotonic freshness policy / model 与 focused JVM tests

## 实现结论

本 Story 在 `core.health` 新增不可变 `HeartRateFreshnessTimeline`、纯 `HeartRateFreshnessPolicy`、`HeartRateFreshnessDecision`、状态与稳定 reason code。policy 只接收调用方显式提供的 monotonic elapsed-time 数值，不读取 `SystemClock`、wall clock 或用于显示的 `measuredAt`。

生产 `AndroidBleHeartRateProvider`、`BleHeartRateProviderState`、`HeartRateState`、浮动胶囊和设置页均未接入该 policy，因此当前连接、断开、parse error 与显示行为保持 E16-9 基线不变。本 Story 不包含 timer、scheduler、watchdog、callback race guard、retry queue 或 `connectGatt` 自动重连。

## Policy 输入与输出

输入 timeline：

- `notifyEnabledAtElapsedMs`：CCCD notify 成功启用的 monotonic 起点。
- `lastValidSampleElapsedMs`：最近一条成功解析 valid bpm 的 monotonic 起点。
- `latestFailureReason`：最近明确连接事实；disconnect 与技术失败保持分离。
- `nowElapsedMs`：由调用方在求值时显式传入的 monotonic 当前时间。

输出：

- `WAITING / waiting_first_sample`
- `LIVE / live_valid_sample`
- `STALE / first_sample_stale | sample_stale`
- `OFFLINE / gatt_disconnected`
- `TECHNICAL_ERROR / connect_failed | service_discovery_failed | cccd_failed | first_sample_silence | notify_silence | parse_failed | invalid_monotonic_time`

reason code 不包含 GATT status code，也不直接承担用户文案；后续 provider / UI mapper 可据此做稳定映射。

## 时间边界

| 场景 | monotonic age | 判定 |
|---|---:|---|
| notify enabled，尚无 valid bpm | `< 15,000 ms` | waiting |
| notify enabled，尚无 valid bpm | `15,000..<30,000 ms` | stale / `first_sample_stale` |
| notify enabled，尚无 valid bpm | `>= 30,000 ms` | technical error / `first_sample_silence` |
| 已有 valid bpm | `< 10,000 ms` | live |
| 已有 valid bpm | `10,000..<30,000 ms` | stale / `sample_stale` |
| 已有 valid bpm | `>= 30,000 ms` | technical error / `notify_silence` |

每条 valid bpm 都以新的 `lastValidSampleElapsedMs` 清除旧技术失败并重置 freshness。malformed payload / parse failure 保留之前的 last-valid timestamp，并立即形成 `parse_failed` 技术失败事实，不伪造新 live 数据。

## 事实与异常输入

- 明确 GATT disconnect 形成 `gatt_disconnected`，判为 offline。
- connect、service discovery、CCCD、notify silence 与 parse failure 判为 technical error。
- `retryExhausted()` 返回同一事实 timeline；它只代表后续 controller 的恢复预算耗尽，不创造新状态。
- failure-only timeline 可以合法不含 notify / sample timestamp：明确 disconnect 仍判为 `offline / gatt_disconnected`，明确技术失败仍保持其对应的 `technical_error` reason。
- timeline 会在 failure fact 映射前先检查所有实际存在 timestamp 的 monotonic 结构，以及 `latestFailureReason` 的 failure fact 结构合法性。
- 合法 failure fact 仅限 `gatt_disconnected` 或标记为 technical failure 的 reason；通过 constructor 或 `copy()` 把 waiting / live / stale reason 放入 `latestFailureReason` 时，即使 timestamp 合法，也会 fail closed 为 `technical_error / invalid_monotonic_time`。
- 所有实际存在的 timestamp 都必须先通过 monotonic 校验：`now` 与 timestamp 非负，timestamp 不晚于 `nowElapsedMs`；notify / sample 同时存在时，sample 不早于 notify。任一结构校验失败都必须在 failure fact 映射前 fail closed 为 `technical_error / invalid_monotonic_time`。
- 只有 timeline 没有 failure fact、需要计算 waiting / live / stale freshness 时，缺少 notify timestamp 才 fail closed 为 `technical_error / invalid_monotonic_time`；不抛异常、不伪造 live。
- wall-clock / `measuredAt` 不属于 policy API，无法参与超时判定。

## Focused tests

`HeartRateFreshnessPolicyTest` 精确覆盖：

1. first sample waiting：14,999 ms、15,000 ms、29,999 ms、30,000 ms。
2. valid sample：9,999 ms、10,000 ms、29,999 ms、30,000 ms。
3. 后续 valid bpm 重置 freshness 起点；parse failure 后 valid bpm 可恢复并重新计时。
4. wall-clock 不在输入边界内，相同 monotonic 输入得到相同结果。
5. malformed / parse failure 不刷新 last-valid timestamp。
6. 明确 disconnect 始终保持 offline。
7. connect / discovery / CCCD / notify silence / parse failure 保持 technical error。
8. retry exhausted 不改变最近事实。
9. failure-only timeline 无 timestamp 时保持 disconnect / technical failure 事实；constructor / `copy()` 中的合法 disconnect 与 technical failure 继续保持原事实。
10. constructor 参数化覆盖 waiting / live / first-sample-stale / sample-stale 非 failure reason，`copy()` 覆盖合法 timestamp 下的非法 failure fact；非法 reason 与非法 monotonic timestamp 组合稳定 fail closed，`technicalFailure()` 对非 technical reason 的既有归一化保持不变。
11. 无 failure fact 且需要 freshness 计算时缺少 notify timestamp，以及负时间、时间回退或不可能的 timestamp 顺序，安全 fail closed。
12. timeline transition 不可变，policy evaluation 不产生 scan / connect / retry / record side effect。

## Review-fix 说明

- 修复 internal data class constructor / `copy()` 可向 `latestFailureReason` 注入非 failure reason 的矛盾 model state。
- `evaluate()` 现在同时验证实际存在 timestamp 的 monotonic 结构与 failure fact 结构，二者都在 failure 映射前完成；任一非法输入都稳定返回 `technical_error / invalid_monotonic_time`。
- 未改变 failure-only 无 timestamp、合法 disconnect / technical failure、`technicalFailure()` 归一化、retry exhausted 与 10 / 15 / 30 秒边界。

## Review-fix 验证

- `. .\.local\env.ps1` 与 `java -version`：通过，Temurin OpenJDK 17.0.19。
- `app:testDebugUnitTest --tests "*HeartRateFreshnessPolicyTest"`、`--tests "*HeartRate*"` 与全量 `app:testDebugUnitTest`：通过。
- `app:assembleDebug`、`app:lintDebug` 与 `app:check`：通过。
- `git diff --check`、`git diff --cached --check`、Story 三点 diff 范围检查：通过。
- 本轮只修改纯 Kotlin policy、focused JVM tests 与本 Story 文档，不改变 UI / BLE lifecycle，因此明确不运行 AVD 或 Band 9 smoke。

## 隔离确认

- 未修改 production provider、GATT callback lifecycle、scanner、device picker、target switch、Compose、胶囊、设置页或用户文案。
- 未新增 coroutine、Handler、alarm、scheduler、retry attempt 或 watchdog。
- 未修改 manifest、BLE 权限、Gradle、Room、migration、DataStore schema、WorkoutSession、records/history/trends。
- 未修改 WorkoutCommand、WorkoutEvent、TimedWorkoutEngine、StrengthWorkoutEngine、TimerDial、声音、震动、通知或 cue。
- E16-11 的训练中 1 秒采样 / 持久化与 E16-12 的分析 / 复盘未开始。

E16-10b-1 已关闭。production provider/controller/runtime 仍未接入该 policy；真实 timer、scheduler、watchdog、retry controller、callback race、old-target guard、lifecycle cancellation 与 foreground direct reconnect 仍未实现，全部属于 E16-10b-2。下一步可独立启动 E16-10b-2 Dev Story；这只表示 **unlocked / not started**，不表示 runtime 已实现、进行中或已验证。
