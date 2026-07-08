# E16-5 Heart-rate settings / opt-in UI implementation

**Status:** Implemented and validated
**Date:** 2026-07-08
**Scope:** Android settings UI, app-level heart-rate display preference, DataStore glue, focused tests, documentation

## 实现范围

本轮在现有 Android 设置页新增 `心率与设备` 设置卡片，并接入 app-level `heartRateDisplayEnabled` DataStore 偏好。

- 心率显示默认关闭。
- 用户可通过设置页 switch 显式开启 / 关闭心率显示偏好。
- 关闭状态明确显示：不显示心率胶囊、不扫描、不连接、不记录。
- 开启状态只显示：已启用显示偏好、后续可选择设备、未连接源 / 待选择设备。
- 如已有 `bleHeartRateDeviceDisplayName`，仅作为已保存设备名称提示，并提供 `清除已保存设备` 动作。
- 写入 `heartRateDisplayEnabled` 时保持旧 `showDisconnectedHeartRatePlaceholder=false`，避免恢复旧的未连接占位 UI。

## 用户路径

当前路径为：

```text
训练首页 -> 设置 -> 心率与设备
```

设置页内可完成：

- 查看当前状态：`未启用` / `已启用`。
- 开启或关闭 `心率显示`。
- 查看用途、记录边界、隐私、非医疗、权限和 overlay 边界说明。
- 清除已保存设备名称。

## 文案边界

设置页中文案说明：

- 用途：训练中显示 App 内实时心率胶囊，作为训练参考。
- 记录边界：当前阶段只保存显示偏好；训练记录采样另拆后续任务。
- 隐私：无训练时只在 App 内显示状态或实时心率，不写入训练记录。
- 非医疗：心率区间仅作训练参考，不诊断疾病，不替代医生建议，不自动中断训练。
- 权限：BLE 权限只会在后续用户主动选择设备或扫描时请求；本轮不请求权限。
- Overlay：不使用系统 overlay / 显示在其他应用上层权限，未来胶囊只显示在 TrainFlow App 内。

## 验证命令

已通过：

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "com.liujyks.trainflow.feature.settings.TrainingPreferencesUiStateTest" --tests "com.liujyks.trainflow.core.datastore.TrainFlowPreferencesBoundaryTest" --tests "com.liujyks.trainflow.app.TrainingPreferencesAppMapperTest"
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
git diff --check
git diff --cached --check
rg "BLUETOOTH_SCAN|BLUETOOTH_CONNECT|ACCESS_FINE_LOCATION|SYSTEM_ALERT_WINDOW" app/src/main
rg "未获取心率|手动心率|平均心率趋势|HeartRatePanel|ManualHeartRate" app/src/main
```

`git diff --check` 仅输出 Windows LF/CRLF 工作区提示，无 whitespace error。`git diff --cached --check` 在未暂存状态下无输出。

权限关键词检查结果：

- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` / `ACCESS_FINE_LOCATION` 仅命中既有 `BleHeartRatePermissionPlanner.kt` 权限规划常量。
- `SYSTEM_ALERT_WINDOW` 无命中。
- production manifest 未在本轮新增 BLE、location 或 overlay 权限。

旧 UI 关键词检查结果：

- `未获取心率` / `手动心率` / `平均心率趋势` / `HeartRatePanel` / `ManualHeartRate` 无命中。

## Smoke 证据路径

AVD smoke 证据目录：

```text
.local/smoke/e16-5-heart-rate-settings-opt-in-ui/
```

该目录只用于本地证据，不提交。

固定 AVD：`TrainFlow_Pixel_API_36`。

已验证：

- 设置入口 `训练偏好` 可见并可进入。
- `心率与设备` 设置卡片可见。
- 清空 app 数据后心率开关默认关闭。
- 打开后显示 `已启用`、`已启用显示偏好；后续可选择设备。`、`未连接源 / 待选择设备。`
- 关闭后显示 `未启用`，并明确 `不显示胶囊、不扫描、不连接、不记录`。
- 开启 / 关闭过程未出现 BLE 权限弹窗，未进入扫描或连接状态。
- 首页和设置路径未出现 `HR Broadcast Smoke` 生产入口。
- `logcat.txt` / `logcat-crash.txt` 无 TrainFlow `FATAL EXCEPTION` 或 ANR；`AndroidRuntime` 命中来自 `uiautomator` 抓树工具的正常启动 / 退出。

## 禁止范围确认

本轮未做：

- 未新增 / 修改 production manifest BLE、location 或 overlay 权限。
- 未触发 Android runtime permission request。
- 未做 BLE scan / connect。
- 未接训练页浮动胶囊。
- 未改 `HeartRateProvider` 行为。
- 未写 session record。
- 未改 Room schema / migration。
- 未改 records / history / trends。
- 未改 `WorkoutCommand` / `WorkoutEvent`。
- 未改 `TimedWorkoutEngine` / `StrengthWorkoutEngine`。
- 未改 TimerDial。
- 未改声音、震动、通知或 cue。
- 未恢复旧心率卡片、`未获取心率` 占位、手动心率输入或平均心率趋势。

## 后续

仍未开始：

- E16-6 permission request flow
- E16-7 device picker / source status
- E16-8 app-shell floating capsule implementation
- E16-11 recording model / 1-second sampling persistence
- E16-12 analysis / zones / post-workout summary
