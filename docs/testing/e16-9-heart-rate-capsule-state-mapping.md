# E16-9 Heart-rate capsule state mapping

**Status:** Implemented; unit verified; AVD smoke completed
**Date:** 2026-07-10
**Scope:** Provider/source/live state to floating capsule mapping, app-shell read-only live state wiring, focused tests, documentation

## 实现范围

本轮把 E16-8 的 floating capsule 从静态设置状态升级为只读 provider/source/live state 显示。

- `TrainFlowApp` 现在收集 `AndroidHeartRateDeviceScanner.providerState`，通过 provider 已有的 `toHeartRateState()` 抽象状态传给 `heartRateFloatingCapsuleUiState(...)`。
- 胶囊 mapper 优先级调整为 display disabled、permission missing、bluetooth disabled 高于旧 live state，避免权限丢失后继续显示旧 bpm。
- mapper 新增 recoverable error 状态，显示 `连接异常`，expanded 更新格为 `异常`，并引导用户回到设置页处理。
- `HeartRateDeviceScanner` 暴露 `connectSelectedDevice()` 与 `stopAndDisconnect()`，用于 app shell lifecycle。
- `AndroidBleHeartRateProvider.stopScan()` 现在只停止扫描窗口，不清除 selected device 或断开 live connection；关闭显示、权限丢失、蓝牙不可用或 dispose 仍走 provider `stop()` / `close()`。
- 选择设备后，只有在心率显示已开启、BLE 权限已授予、provider 内已有本次用户选择的设备时，app shell 才调用 `connectSelectedDevice()`。
- 本轮不做 cold-start saved-device 自动重连。原因是 DataStore 只保存 provider identifier / display name，不保存 `BluetoothDevice` / `BluetoothGatt` / SDK model；provider 冷启动后没有可安全连接的 Android `BluetoothDevice` 实例。后续如要自动重连，必须另拆明确的 bounded reconnect / bonded-device lookup 策略，且不得静默扫描。

## 状态 Mapping 表

| 输入优先级 | Collapsed label | Expanded 来源 | Expanded 记录 | Expanded 区间 | Expanded 更新 |
|---|---|---|---|---|---|
| display disabled | hidden | hidden | hidden | hidden | hidden |
| permission missing | `权限未赋予` | `未授权` 或已保存设备名 | `未记录` | `无` | `无数据` |
| bluetooth disabled | `蓝牙关闭` | `蓝牙关闭` 或已保存设备名 | `未记录` | `无` | `无数据` |
| no selected source | `未连接源` | `未连接` | `未记录` | `无` | `无数据` |
| saved / selected source, not connected | `已选择设备` | 设备名 | `当前只显示状态` | `无` | `无数据` |
| provider connecting / scanning | `正在连接` | 设备名或 `查找中` | `当前只显示状态` | `无` | `无数据` |
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

## 验证命令

已通过：

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

## 真机人工测试边界

如果要验证 Band 9 live bpm：

1. 安装 debug APK。
2. 打开 TrainFlow 设置页，开启 `心率显示`。
3. 授权蓝牙权限。
4. 确认 Band 9 已开启心率广播模式。
5. 点击 `扫描心率设备`，选择 `HUAWEI Band HR-OD7` 或当前广播名。
6. 返回首页或训练页，观察胶囊从 `正在连接` / `等待数据` 进入 `心率 {bpm} bpm`。
7. 若无 bpm，截图胶囊、设置页 source status，并保存 logcat 中 provider state。

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

- E16-10 stale / offline policy 仍未开始。
- E16-11 recording model / 1s sampling persistence 仍未开始。
- E16-12 analysis / zones / post-workout summary 仍未开始。
