# E16-6 Heart-rate BLE permission request flow

**Status:** Implemented; unit/build/smoke validated, lint/check timeout noted
**Date:** 2026-07-08
**Scope:** Android BLE permission manifest, settings permission rationale, runtime permission request flow, UI result state, focused tests

## 实现范围

本轮在 E16-5 设置页 `心率与设备` 卡片中新增“准备连接设备 / 授权蓝牙权限”入口。

- 默认关闭时不请求权限。
- 开启 `心率显示` switch 后仍不自动请求权限。
- 只有用户在已开启状态下主动点击 `准备连接设备`，才显示 App 内中文权限说明。
- 只有用户在说明区继续点击 `授权蓝牙权限`，才触发 Android runtime permission request。
- 请求结果回填到设置页，显示已允许、未赋予或需要去系统设置开启。
- 本轮仍不展示设备列表，不扫描、不连接、不写训练记录。

## Manifest 权限变化

生产 `app/src/main/AndroidManifest.xml` 新增：

```xml
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission
    android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="30" />
```

说明：

- Android 12+ runtime flow 使用 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`。
- `BLUETOOTH_SCAN` 标记 `neverForLocation`，本功能不用于定位。
- Android 11 及以下沿用 `BleHeartRatePermissionPlanner` 的 scan compatibility fallback：`ACCESS_FINE_LOCATION` 仅用于蓝牙扫描兼容，且 `maxSdkVersion=30`；本轮不做扫描。
- 未声明 `SYSTEM_ALERT_WINDOW`，未声明旧 `BLUETOOTH` / `BLUETOOTH_ADMIN`，未声明健康 / 身体传感器权限。

## 用户路径

```text
训练首页 -> 设置 -> 心率与设备
  1. 默认：心率显示关闭，无权限请求。
  2. 打开 switch：显示已启用、未连接源 / 待选择设备，无权限请求。
  3. 点击准备连接设备：显示授权前说明。
  4. 点击授权蓝牙权限：触发系统权限弹窗。
  5. 允许：显示“蓝牙权限已允许 / 可选择设备”。
  6. 拒绝：显示“权限未赋予”，说明可稍后重试。
  7. 不再询问 / permanently denied 可检测时：提示到系统设置开启。
```

## 权限文案

授权前说明包含：

- 用途：查找并连接你主动选择的蓝牙心率设备。
- 不用途：不使用系统悬浮窗，不后台无限扫描，不无提示扫描。
- 记录边界：无训练时只显示不记录；训练记录采样另拆后续任务。
- 非医疗边界：心率区间仅训练参考，不诊断疾病，不替代医生建议，不自动中断训练。

结果状态文案：

- 已允许：`蓝牙权限已允许 / 可选择设备。本轮仍不展示真实设备列表，不扫描、不连接。`
- 已拒绝：`权限未赋予。你可以稍后再次点击授权蓝牙权限重试；关闭心率显示后不会继续请求权限。`
- 不再询问：`权限未赋予，系统可能不再弹出授权窗口。请到系统设置中为 TrainFlow 开启蓝牙权限。`

## 验证命令

已通过：

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*" --tests "com.liujyks.trainflow.feature.settings.TrainingPreferencesUiStateTest" --tests "com.liujyks.trainflow.core.notifications.PlanReminderNotificationManifestBoundaryTest"
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
```

已尝试但未在本机完成：

```powershell
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
```

`app:lintDebug` 曾以 3 分钟、6 分钟超时重试，`--no-daemon --no-configuration-cache app:lintDebug` 也在 6 分钟超时；`app:check` 在 6 分钟超时。超时后已停止残留 Gradle wrapper 进程。超时发生在 lint/check worker 未结束阶段，未产生本轮代码变更后的新失败报告。

最终 diff whitespace 检查在提交前执行。

## Boundary scans

已执行：

```powershell
rg -n "SYSTEM_ALERT_WINDOW|ACTION_MANAGE_OVERLAY_PERMISSION" app/src/main
rg -n "BLUETOOTH_SCAN|BLUETOOTH_CONNECT|ACCESS_FINE_LOCATION" app/src/main
rg -n "startScan|BluetoothLeScanner|connectGatt|GATT|scanForHeartRate|selectDevice" app/src/main
rg -n "未获取心率|手动心率|平均心率趋势|HeartRatePanel|ManualHeartRate" app/src/main
```

结果：

- `SYSTEM_ALERT_WINDOW|ACTION_MANAGE_OVERLAY_PERMISSION`：无命中。
- `BLUETOOTH_SCAN|BLUETOOTH_CONNECT|ACCESS_FINE_LOCATION`：仅命中生产 manifest 与 `BleHeartRatePermissionPlanner`。`ACCESS_FINE_LOCATION` 是 Android 11 及以下 BLE scan 兼容 fallback，manifest 限制 `maxSdkVersion=30`，Android 12+ 不申请 location。
- `startScan|BluetoothLeScanner|connectGatt|GATT|scanForHeartRate|selectDevice`：仅命中既有 `AndroidBleHeartRateProvider`；生产 UI 未实例化该 provider，也没有从设置页触发 scan/connect/device picker。
- 旧心率 UI 文案与组件：无命中。

## Smoke 证据路径

Android smoke 固定 AVD：

```text
TrainFlow_Pixel_API_36
```

证据目录：

```text
.local/smoke/e16-6-heart-rate-permission-request-flow/
```

已覆盖：

- default off
- enabled but no permission request
- rationale visible
- system permission prompt
- denied state
- granted state
- no scan / device list
- no HR Broadcast Smoke production entry
- logcat 无 FATAL / ANR

`.local/` 不提交。

关键证据文件：

- `03-heart-rate-default-off.xml/png`：默认关闭，无权限请求。
- `04-heart-rate-enabled-no-request.xml/png`：开启 switch 后仍停留在 App 设置页，无系统权限弹窗。
- `05b-rationale-visible-details.xml/png`：系统权限弹窗前展示中文用途、不用途、记录边界、非医疗边界。
- `06-system-permission-prompt.xml/png`：点击 `授权蓝牙权限` 后出现 Android Nearby devices 权限弹窗。
- `07-permission-denied.xml/png`：拒绝后显示 `权限未赋予` 与稍后重试文案。
- `08b-system-permission-prompt-second.xml/png`：第二次系统弹窗，系统拒绝按钮已变为 don't ask again 语义。
- `09-permission-granted.xml/png`：允许后显示 `蓝牙权限已允许 / 可选择设备`，并声明本轮不展示设备列表、不扫描、不连接。
- `01-trainflow-home.xml/png`：生产 TrainFlow 首页无 `HR Broadcast Smoke` 入口；`00-debug-entry.*` 中存在该入口，仅属于 debug entry。
- `logcat.txt` / `logcat-crash.txt`：未发现 `FATAL EXCEPTION` / `ANR`。

## 禁止范围确认

本轮未做：

- 未做 BLE scan。
- 未做设备列表 / device picker。
- 未做 GATT connect。
- 未接训练页浮动胶囊。
- 未写 session record。
- 未改 Room schema / migration。
- 未改 records / history / trends。
- 未改 `WorkoutCommand` / `WorkoutEvent`。
- 未改 `TimedWorkoutEngine` / `StrengthWorkoutEngine`。
- 未改 TimerDial。
- 未改声音、震动、通知或 cue。
- 未使用 `SYSTEM_ALERT_WINDOW` / “显示在其他应用上层”权限。
- 未后台无限扫描，未无提示扫描。
- 未恢复旧心率卡片、`未获取心率`、手动心率输入或旧平均心率趋势。

## 后续

E16-7 设备选择 / scan 仍未开始。后续仍需另拆：

- E16-7 device picker / source status
- E16-8 app-shell floating capsule implementation
- E16-9 `HeartRateState` -> capsule mapping
- E16-10 stale / offline policy
- E16-11 recording model / 1-second sampling persistence
- E16-12 analysis / zones / post-workout summary
