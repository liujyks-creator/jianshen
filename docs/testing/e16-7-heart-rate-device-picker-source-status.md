# E16-7 Heart-rate device picker / source status

**Status:** Implemented and validated
**Date:** 2026-07-09
**Scope:** Settings heart-rate device picker, user-triggered bounded BLE scan, candidate list UI state, selected source preference, source status UI, focused tests, AVD smoke

## 实现范围

本轮在 `心率与设备` 设置卡片中新增设备来源状态和用户主动扫描 / 选择设备流程。

- 心率显示关闭时不显示可触发扫描的入口，不触发 scan。
- 未授权蓝牙权限时不扫描，继续引导到 E16-6 的权限 rationale / runtime permission flow。
- 蓝牙关闭或不可用时显示 `设备来源：蓝牙关闭`，扫描按钮 disabled。
- 蓝牙权限允许后，用户必须点击 `扫描心率设备` 才会启动 BLE scan。
- 扫描窗口为 12 秒；手动停止、timeout、关闭心率、权限丢失或离开设置页都会停止 scanner。
- 扫描期间显示 `设备来源：扫描中`、`正在扫描` 和 `停止扫描`。
- 候选列表只展示广播标准心率服务 `0x180D` 的设备；列表展示 display name、遮蔽后的 identifier、RSSI / 信号未知和 `广播标准心率服务 0x180D`。
- 选择设备后只保存 `bleHeartRateDeviceIdentifier` / `bleHeartRateDeviceDisplayName`。
- 已选择设备显示 `已选择设备 / 可用于后续连接` 同等文案，并保留清除已保存设备入口。
- 清除后回到未连接源 / 待选择设备状态。

本轮没有接训练页浮动胶囊，没有 GATT connect，没有 notify / bpm 读取，没有记录落库。

## 用户路径

```text
训练首页 -> 训练偏好 -> 心率与设备
  1. 默认关闭：不扫描、不连接、不记录。
  2. 开启心率显示：仍不自动请求权限或扫描。
  3. 点击准备连接设备：展示授权前说明。
  4. 点击授权蓝牙权限：触发系统 Nearby devices 权限。
  5. 权限允许：显示设备来源状态和扫描心率设备入口。
  6. 点击扫描心率设备：进入 12 秒扫描窗口。
  7. 有候选：展示 HRS 候选并允许选择。
  8. 无候选 timeout：显示未发现心率设备和 HUAWEI Band 9 心率广播提示。
```

## Scan Window / Stop Policy

- Scan window: 12 秒。
- 用户主动点击 `扫描心率设备` 才开始 scan。
- 用户点击 `停止扫描` 会停止 scanner。
- scan timeout 后自动停止并显示 no-devices 状态。
- 离开设置页、关闭心率显示、权限不满足或 composable dispose 时停止 scanner。
- 设置页只调用 device scanner 的 `startScan` / `stopScan` / `selectDevice`，不调用 `connectSelectedDevice`。

## 保存设备偏好字段

只保存：

- `bleHeartRateDeviceIdentifier`
- `bleHeartRateDeviceDisplayName`

不保存：

- `BluetoothDevice`
- `BluetoothGatt`
- GATT service / characteristic model
- SDK model
- bpm 样本
- session summary

## 验证命令

已通过：

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*" --tests "com.liujyks.trainflow.feature.settings.TrainingPreferencesUiStateTest" --tests "com.liujyks.trainflow.core.datastore.TrainFlowPreferencesBoundaryTest" --tests "com.liujyks.trainflow.app.TrainingPreferencesAppMapperTest"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
```

Focused tests 覆盖：

- 未开启时不能扫描。
- 未授权时不能扫描。
- 蓝牙关闭时不能扫描。
- scan start -> scanning state。
- HRS candidates -> devicesFound state。
- no candidates timeout -> noDevicesFound state。
- select device -> selected state / DataStore identifier + display name boundary。
- clear selection -> idle no source state。
- scan failure -> scanFailed state。

## Boundary Scans

已执行：

```powershell
rg -n "SYSTEM_ALERT_WINDOW|ACTION_MANAGE_OVERLAY_PERMISSION" app/src/main
rg -n "connectGatt|BluetoothGatt|discoverServices|setCharacteristicNotification|0x2A37|2A37|heart-rate notify|bpm" app/src/main
rg -n "startScan|BluetoothLeScanner|ScanCallback" app/src/main
rg -n "未获取心率|手动心率|平均心率趋势|HeartRatePanel|ManualHeartRate" app/src/main
```

结果：

- overlay 权限：无命中。
- connect / notify / bpm：命中既有 `AndroidBleHeartRateProvider`、parser 和 boundary model；本轮设置页只新增 scan/select wrapper，不调用 connect / notify / bpm。
- startScan / ScanCallback：命中既有 provider、本轮窄 `HeartRateDeviceScanner` 和 settings shell 的用户点击 scan 调用；无后台 / 自动扫描入口。
- 旧心率 UI 文案和组件：无命中。

## AVD Smoke

固定 AVD：

```text
TrainFlow_Pixel_API_36
```

证据目录：

```text
.local/smoke/e16-7-heart-rate-device-picker-source-status/
```

已覆盖：

- `01-main-home.*`：生产 `MainActivity` 首页无 `HR Broadcast Smoke` 入口。
- `03-heart-rate-default-off.*`：心率显示关闭，`未启用`，无可触发扫描入口。
- `04d-enabled-permission-required-action.*`：开启后未授权，显示 `准备连接设备`、`设备来源：需要蓝牙权限`、`未授权时不会扫描设备`。
- `06-rationale-visible.*` / `06c-auth-button-visible.*`：授权前说明和 `授权蓝牙权限`。
- `07-system-permission-prompt.*`：Android Nearby devices 权限弹窗。
- `08-permission-granted-scan-ready.*`：授权后显示 `扫描心率设备`。
- `09-scanning.*`：点击扫描后显示 `正在扫描` / `停止扫描`。
- `10b-scan-timeout-no-devices-title.*`：12 秒窗口后显示 `未发现心率设备`、`没有找到心率设备`、`心率广播模式` 和 `重新扫描`。
- `11-bluetooth-disabled.*`：蓝牙关闭时显示 `设备来源：蓝牙关闭` / `蓝牙已关闭`，扫描按钮 disabled。
- `logcat.txt` / `logcat-crash.txt`：无 TrainFlow `FATAL EXCEPTION` / ANR；`AndroidRuntime` 普通命中来自 `uiautomator` 抓树工具。

AVD 没有真实 BLE HRS 外设，不能验证真实候选选择和清除已保存设备按钮的人工路径；该路径由 focused unit tests 和 DataStore boundary 覆盖，真机 Band 9 选择 / 清除可由用户后续人工回传。

## 真机 BLE 外设验证边界

本 story 不要求 Codex 环境完成真机 BLE 外设测试。真实 Band 9 验证应由用户在心率广播模式下人工完成，期望能看到类似：

```text
HUAWEI Band HR-OD7
services=[0x180D]
```

如果设备名称或地址变化，用户可能需要重新扫描选择；identifier 只作为 convenience hint，不是稳定医疗设备身份。

## 禁止范围确认

本轮未做：

- 未做 GATT connect。
- 未订阅 `0x2A37 notify`。
- 未读取 bpm。
- 未接训练页浮动胶囊。
- 未写 session record。
- 未改 Room schema / migration。
- 未改 records / history / trends。
- 未改 `WorkoutCommand` / `WorkoutEvent`。
- 未改 `TimedWorkoutEngine` / `StrengthWorkoutEngine`。
- 未改 TimerDial。
- 未改声音、震动、通知或 cue。
- 未使用 `SYSTEM_ALERT_WINDOW` / “显示在其他应用上层”权限。
- 未后台扫描、未无提示扫描、未无限扫描。
- 未恢复旧心率卡片、`未获取心率`、手动心率输入或旧平均心率趋势。

## 后续

E16-8 app-shell floating capsule 仍未开始。后续仍需另拆：

- E16-8 app-shell floating capsule implementation
- E16-9 `HeartRateState` -> capsule mapping
- E16-10 stale / offline policy
- E16-11 recording model / 1-second sampling persistence
- E16-12 analysis / zones / post-workout summary
