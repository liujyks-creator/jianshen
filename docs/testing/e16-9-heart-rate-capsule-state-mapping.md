# E16-9 Heart-rate capsule state mapping

**Status:** Implemented; code review accepted; pending Band 9 post-fix real-device acceptance before merge
**Date:** 2026-07-11
**Scope:** Provider/source/live state to floating capsule mapping, app-shell read-only live state wiring, focused tests, documentation

## 实现范围

本轮把 E16-8 的 floating capsule 从静态设置状态升级为只读 provider/source/live state 显示。

- `TrainFlowApp` 分别收集 `AndroidHeartRateDeviceScanner.providerState` 与独立 `scanState`；只有 provider state 经 `toHeartRateState()` 进入 `heartRateFloatingCapsuleUiState(...)`，scanner lifecycle 只进入设置页 device picker。
- 胶囊 mapper 优先级调整为 display disabled、permission missing、bluetooth disabled 高于旧 live state，避免权限丢失后继续显示旧 bpm。
- mapper 新增 recoverable error 状态，显示 `连接异常`，expanded 更新格为 `异常`，并引导用户回到设置页处理。
- `HeartRateDeviceScanner` 暴露 `connectSelectedDevice()` 与 `stopAndDisconnect()`，用于 app shell lifecycle。
- `AndroidBleHeartRateProvider.stopScan()` 现在只停止扫描窗口，不清除 selected device 或断开 live connection；关闭显示、权限丢失、蓝牙不可用或 dispose 仍走 provider `stop()` / `close()`。
- `startScan()`、HRS candidate 更新、scan failure 和 12 秒 timeout 不再发布 `SCANNING` / `DEVICE_FOUND` / scan `STOPPED` 到 active provider state，因此不会覆盖 `LIVE_BPM` / waiting / connection error。
- 进入设置页触发的 availability refresh 在权限和蓝牙仍可用时保留 active provider state，不把 live connection 重置为 no source。
- 已连接时设置页显示 `正在扫描其他设备`，扫描不会断开当前 GATT，也不会自动替换连接目标；只有用户选择候选设备后才停止扫描、清空候选 UI、发布新 target 的 selected / connecting state 并切换连接。
- 选择设备后，只有在心率显示已开启、BLE 权限已授予、provider 内已有本次用户选择的设备时，app shell 才调用 `connectSelectedDevice()`。
- 本轮不做 cold-start saved-device 自动重连。原因是 DataStore 只保存 provider identifier / display name，不保存 `BluetoothDevice` / `BluetoothGatt` / SDK model；provider 冷启动后没有可安全连接的 Android `BluetoothDevice` 实例。后续如要自动重连，必须另拆明确的 bounded reconnect / bonded-device lookup 策略，且不得静默扫描。

## 2026-07-10 E16-9b：已保存设备清晰度与显式连接

- 冷启动从 DataStore 恢复的只有 identifier / display name 是**已保存偏好**，不是当前 provider 连接。胶囊 collapsed 显示中性 `未连接`；expanded 的 `来源` 显示 `已保存：{设备名}`，`更新` 显示 `未连接`。旧的运行时 `连接异常` 不跨进程保存，重启后如尚未验证可用性则回到这一路径。
- 设置页明确分列 `心率显示：已开启`、`连接状态：未连接` 和 `已保存设备：{设备名}`，并说明保存设备不代表在附近、已开启广播、正在连接或已经连接。
- 有 saved identifier 且当前未连接时，主操作为 `连接已保存设备`。只有这次用户点击才启动既有约 12 秒、标准 HRS `0x180D` filter 的 scan；只有候选 identifier 与保存 identifier 精确相等时，才自动 `selectDevice()` 并沿用现有 `connectSelectedDevice()` 生命周期。display name 相同不足以自动连接。
- timeout 未匹配显示 `未找到已保存设备`，不连接、不重试；其他 HRS candidates 继续保留在列表供用户手动选择。无保存设备仍为 `扫描心率设备`。
- 当前 live bpm 时主操作为 `扫描其他设备`。fbaabf9 的 provider / scan state 分离保持不变：scan active、candidate update 和 timeout 都不覆盖当前 GATT 或 bpm；只有手动选择新设备才切换 target。
- 力量训练默认模式卡片只显示中文 label `手动开始` / `休息后自动`，不展示 `manual_start` / `auto_after_rest` contract token；contract、DataStore、计划编辑器和训练引擎不变。

## 状态 Mapping 表

| 输入优先级 | Collapsed label | Expanded 来源 | Expanded 记录 | Expanded 区间 | Expanded 更新 |
|---|---|---|---|---|---|
| display disabled | hidden | hidden | hidden | hidden | hidden |
| permission missing | `权限未赋予` | `未授权` 或已保存设备名 | `未记录` | `无` | `无数据` |
| bluetooth disabled | `蓝牙关闭` | `蓝牙关闭` 或已保存设备名 | `未记录` | `无` | `无数据` |
| no selected source | `未连接源` | `未连接` | `未记录` | `无` | `无数据` |
| saved source, no active provider (cold start) | `未连接` | `已保存：设备名` | `当前只显示状态` | `无` | `未连接` |
| provider connecting | `正在连接` | 设备名 | `当前只显示状态` | `无` | `无数据` |
| scanner active while provider is live | 保持当前 live bpm | 保持当前设备 | `训练记录：后续开启` | 保持当前区间 | `实时` |
| connected waiting first data | `等待数据` | 设备名 | `当前只显示状态` | `无` | `无数据` |
| live bpm without age | `心率 {bpm} bpm` | 设备名 | `训练记录：后续开启` | `无` | `实时` |
| live bpm with age | `{区间} {bpm} bpm` | 设备名 | `训练记录：后续开启` | 区间名 | `实时` |
| stale reading | `数据过期` | 设备名或状态名 | `当前只显示状态` | `无` | `无数据` |
| disconnected / offline | `离线` | 设备名或状态名 | `当前只显示状态` | `无` | `无数据` |
| recoverable error | `连接异常` | 设备名或状态名 | `当前只显示状态` | `无` | `异常` |

年龄缺失时只显示 `心率 {bpm} bpm`，不显示区间。当前 app 还没有心率个人参数设置，zone mapping 保持 mapper-ready，不强行引入年龄 / 阈值 DataStore 字段。`超过上限` 仍只是深红视觉状态，不播放声音、不震动、不强制暂停、不发通知、不作为医疗告警。

## Live BPM Provider 接线

本轮已接入 live provider 的只读状态显示，但范围是有边界的：

- 用户必须先在设置页开启心率显示。
- 用户必须已授予 BLE 权限。
- 用户必须通过设置页主动扫描并选择本次 provider 已知的 HRS 设备。
- 已连接时可继续扫描其他 HRS 设备；scanner state 和候选列表不改变胶囊，未选择候选时当前连接继续工作。
- 连接由 selected device state 触发，不由胶囊点击、展开或拖动触发。
- 胶囊只消费 `HeartRateState`，不保存 GATT / SDK model。
- 关闭显示、权限丢失、蓝牙不可用或 provider dispose 会 stop / disconnect。

未接入：

- 冷启动后仅凭保存的 identifier 自动连接。
- 后台扫描或自动扫描。
- session record 写入。
- 1s sampling persistence。
- post-workout analysis / zones summary。

## Focused Tests

新增 / 更新 tests 覆盖：

- disabled -> hidden。
- permission missing 优先于旧 live bpm -> `权限未赋予`。
- bluetooth disabled -> `蓝牙关闭`。
- no selected source -> `未连接源`。
- selected source -> source label。
- waiting -> `等待数据`。
- live bpm no age -> `心率 {bpm} bpm`。
- stale -> `数据过期`。
- disconnected/offline -> `离线`。
- recoverable error -> `连接异常`。
- recording copy remains future-only。
- display disabled / permission missing / bluetooth off 不启动 provider connect。
- selected device only connects after display enabled + permission granted。
- no source 不扫描、不连接。
- connected live bpm + 12 秒 scan active -> 继续显示 live bpm，不显示 `正在连接`。
- scan window 内 candidates 更新、发现其他未选择设备 -> 继续显示当前来源 live bpm。
- scan timeout -> 继续显示当前 live bpm / provider state。
- 用户选择新设备 -> 新 target 进入 selected / connecting / waiting / live 流程，旧 target 不再覆盖新状态。
- broadcast / provider connection error -> `连接异常`。
- cold start 只有保存的 source hint、provider 无已知设备 -> `未连接 + 已保存：设备名`，不自动 scan / connect，旧 error 不跨进程展示。
- 点击 `连接已保存设备` 才开始 bounded scan；同名不同 identifier 不自动连接，精确 identifier 才可进入既有 connecting / waiting / live 路径。
- saved reconnect timeout -> `未找到已保存设备`，其他 HRS candidates 保留供手选。
- SettingsRoute 不展示 `manual_start` / `auto_after_rest`。

## 2026-07-10 Band 9 人工真机测试反馈

用户在真实 Android 手机 + HUAWEI Band 9 心率广播模式下确认：

- TrainFlow 可以发现、连接 Band 9，并持续获得 live bpm；这是 E16-9 production path 的正向证据。
- 关闭 Band 9 心率广播后，胶囊显示 `连接异常`；本轮接受当前 error 映射，完整 stale / offline 时序仍属于 E16-10。
- E16-9b 的实现语义是：退出 App 再进入后，胶囊显示 `未连接`，并在 expanded / 设置页明确标出已保存设备；不会自动连接。用户可点击 `连接已保存设备` 后进行一次 bounded、精确 identifier 匹配尝试；重新开启 Band 9 广播不会自动重连。该修复后的 Band 9 路径仍待下方最终人工验收。
- 已连接并显示 bpm 时点击 `重新扫描`，原实现只在 12 秒扫描窗口内出现 `心率` / `正在连接` 交替；窗口结束后交替停止。该时间边界证明 scanner `scanning` / candidate state 覆盖了 active provider display state，而不是 notify 自身抖动。
- 本修复已将 scanner lifecycle / candidates 与 provider connection / live bpm 分离。扫描窗口、候选变化、发现其他未选择设备和 timeout 都不再污染胶囊；当前 live bpm 保持显示，只有用户选择新设备后才切换 target。

## 2026-07-11 APK provenance / AVD 再验证

- 用户 22:10 / 22:11 的“已选择设备”截图早于 E16-9b commit `4b7689a`（22:54），只作为修复前问题证据，不能用于否定修复后行为。
- 已在 `4b7689a` 分支重新执行 `app:assembleDebug`，并将新 debug APK 安装到固定 `TrainFlow_Pixel_API_36`；安装包 SHA-256 与本地新构建产物一致。
- AVD 清空 TrainFlow 数据后的默认状态为 `心率显示：未开启`，胶囊隐藏符合默认关闭规则；开启显示偏好后，胶囊显示 `权限未赋予`，证明当前 APK 的胶囊状态映射仍在工作，不是心率 UI 被移除。
- 证据位于 `.local/smoke/e16-9b-apk-provenance-diagnosis/`，不提交。
- AVD 没有保存的真实 Band 9 或 live HRS source，不能替代下面的精确 identifier reconnect 和 live bpm 扫描窗口人工验收。

后续 reconnect / stale-offline policy 必须单独决定：

- App cold start 是否对上次设备做 bounded reconnect，以及如何安全恢复 `BluetoothDevice`。
- 已连接设备广播恢复后是否自动重连，是否需要用户动作、重试上限和 backoff。
- 连接失败、notify 中断或广播关闭多久后从 `连接异常` 进入 stale / offline。
- identifier / address 变化、Android privacy 和 Band 广播名称变化时如何 fail closed；不得用无限后台扫描兜底。

## 验证命令

2026-07-10 E16-9b 已通过：

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*"
```

已通过：

```powershell
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
git diff --check
```

2026-07-10 E16-9b 复跑 focused / full unit / assemble / lint / check 均通过；boundary scans 确认没有新增 overlay permission、旧心率卡片 / 手动输入 / 平均心率趋势，也没有把心率接入 records、Room、engines、commands/events、TimerDial 或 cue。

待 staging 后执行：

```powershell
git diff --cached --check
```

## AVD Smoke

固定 AVD：

```text
TrainFlow_Pixel_API_36
```

证据目录：

```text
.local/smoke/e16-9-heart-rate-capsule-state-mapping/
```

已覆盖：

- disabled hidden。
- enabled no source -> `未连接源`。
- permission missing -> `权限未赋予`。
- selected source state -> source label visible，通过 app-private DataStore 注入 AVD-only fake saved source `Fake Band 9 AVD`。
- expanded panel shows future-only recording copy。
- no scan triggered by capsule tap / expand / drag。
- logcat no FATAL / ANR。
- no production HR Broadcast Smoke。

已保存证据：

```text
.local/smoke/e16-9-heart-rate-capsule-state-mapping/summary.txt
.local/smoke/e16-9-heart-rate-capsule-state-mapping/01-disabled-hidden.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/04-enabled-permission-missing.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/05-enabled-no-source.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/06-expanded-no-source-panel.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/07-selected-source-collapsed.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/08-selected-source-expanded.png
.local/smoke/e16-9-heart-rate-capsule-state-mapping/09-logcat-keyword-scan.txt
```

AVD 没有真实 BLE HRS 外设，不能证明 Band 9 live bpm。真实 live bpm 需要用户真机人工测试。

### E16-9b saved-device clarity evidence

固定 AVD `TrainFlow_Pixel_API_36` 已使用新 debug APK 重测；证据只位于：

```text
.local/smoke/e16-9b-saved-device-clarity/
```

已覆盖：

- 默认关闭时不显示胶囊；力量设置 XML 仅显示 `手动开始` / `休息后自动`，不含 raw `manual_start` / `auto_after_rest`。
- app-private DataStore 注入仅用于 AVD fake saved source 后 cold start：胶囊显示 `未连接`，logcat 没有 TrainFlow `startScan` / `connectGatt`，不把旧 runtime error 当成当前状态。
- 设置页显示 `心率显示：已开启`、`连接状态：未连接`、`已保存设备：Fake Band 9 AVD`，并出现 `连接已保存设备`。
- 点击该按钮才出现“仅会自动连接 identifier 完全匹配”的 12 秒扫描 UI；无匹配 timeout 后显示 `设备来源：未找到已保存设备`，不连接、不重试。
- 最终 logcat `FATAL EXCEPTION` / `ANR` scan 为空；未启动 production `HR Broadcast Smoke`。

`live fake state + 扫描其他设备` 无法在本 AVD 注入成可用 HRS/GATT source，且本轮禁止使用 production HR Broadcast Smoke；因此该路径由 `HeartRateFloatingCapsuleStateTest` / `TrainingPreferencesUiStateTest` 的 provider-state fixture 覆盖，不依赖 Band 9 或 AVD BLE 外设能力。

关键证据：

```text
.local/smoke/e16-9b-saved-device-clarity/18-cold-start-saved-source.xml
.local/smoke/e16-9b-saved-device-clarity/20-settings-saved-unconnected-detail-text.txt
.local/smoke/e16-9b-saved-device-clarity/22-explicit-connect-scan-text.txt
.local/smoke/e16-9b-saved-device-clarity/29-fixed-saved-device-timeout-text.txt
.local/smoke/e16-9b-saved-device-clarity/30-final-logcat-fatal-anr.txt
```

feedback fix debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
SHA-256: 0410919335FE797060BE851361447E9757EF291E8DFE976F53BC64E36ADBDE6B
```

该 APK 已安装到固定 `TrainFlow_Pixel_API_36` 并完成 launcher / no-crash smoke；新增证据目录为 `.local/smoke/e16-9-scan-state-contamination-fix/`。AVD 不具备真实 Band 9 BLE HRS，不能替代下面的真机 12 秒扫描窗口复测。

## 真机人工测试边界

如果要验证 Band 9 live bpm：

1. 安装 debug APK。
2. 打开 TrainFlow 设置页，开启 `心率显示`。
3. 授权蓝牙权限。
4. 确认 Band 9 已开启心率广播模式。
5. 点击 `扫描心率设备`，选择 `HUAWEI Band HR-OD7` 或当前广播名。
6. 返回首页或训练页，观察胶囊从 `正在连接` / `等待数据` 进入 `心率 {bpm} bpm`。
7. 若无 bpm，截图胶囊、设置页 source status，并保存 logcat 中 provider state。

针对本次 feedback fix 的建议复测：

1. 连接 Band 9，确认胶囊显示 live bpm。
2. 保持连接时点击 `重新扫描`，确认整个 12 秒窗口内胶囊持续显示 bpm，不在 bpm / `正在连接` 之间交替。
3. 扫描到其他 HRS 设备但不选择，确认胶囊仍显示 Band 9 bpm。
4. 等扫描窗口结束，确认胶囊仍显示原 bpm / 当前 provider state。
5. 选择另一设备时，确认此时才切换 target 并进入 `正在连接` / `等待数据` / live。
6. 关闭 Band 9 心率广播，确认显示 `连接异常` 或后续离线状态。
7. 重启 App，确认显示 `未连接 + 已保存设备` 且不自动连接；点击 `连接已保存设备` 后才允许一次 12 秒精确 identifier 查找，并把自动重连 / backoff / error 时序留给 E16-10。

## 禁止范围确认

本轮未做：

- 未写 session record。
- 未新增 Room table / migration。
- 未改 records / history / trends。
- 未实现 1s sampling persistence。
- 未做 post-workout analysis / zones summary。
- 未改 `WorkoutCommand` / `WorkoutEvent`。
- 未改 `TimedWorkoutEngine` / `StrengthWorkoutEngine`。
- 未改 TimerDial。
- 未改 sound / haptic / notification / cue。
- 未使用系统 overlay permission。
- 未自动扫描。
- 未在胶囊点击 / 展开 / 拖动时触发 scan。
- 未恢复旧心率卡片、`未获取心率`、手动心率输入或旧平均心率趋势。

## 后续

- E16-10 仍负责更广泛 reconnect / backoff、广播恢复是否允许自动重连，以及 error -> stale / offline 时序；本轮没有实现这些。
- E16-11 recording model / 1s sampling persistence 仍未开始。
- E16-12 analysis / zones / post-workout summary 仍未开始。
