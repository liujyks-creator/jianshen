# E16-8 Heart-rate floating capsule

**Status:** Implemented; unit/build/lint/check validated; AVD smoke completed on `TrainFlow_Pixel_API_36`
**Date:** 2026-07-09
**Scope:** Android app-shell floating heart-rate capsule overlay, capsule UI state mapper, drag / snap geometry, focused tests, docs

## 实现范围

本轮在 official app shell 顶层实现 App 内浮动心率胶囊 overlay。

- 胶囊挂在 `TrainFlowApp` 的 `Scaffold` 内容外层 `Box` 中，只显示在 TrainFlow App 内。
- `heartRateDisplayEnabled=false` 时不显示胶囊。
- `heartRateDisplayEnabled=true` 时，App 内页面显示胶囊；默认生产路径只消费设置、权限、source status 和已保存设备偏好。
- 胶囊支持 collapsed / expanded，轻点切换展开收起。
- 胶囊支持拖动，使用 movement threshold 避免轻点误判为拖动。
- 拖动释放后吸附到左右安全边。
- expanded 内提供 `心率与设备` 入口，复用现有设置页 destination，不新增复杂导航。
- 键盘 / IME 可见或强制 compact 时收起，避免 confirm-record + keyboard 空间不足。
- 位置保留在 shell 内存中；本轮不持久化位置。

本轮没有接真实 live bpm provider 到生产训练页，不启动 BLE scan / connect，不做训练记录落库，不做分析。

## 状态 Mapping

新增 `heartRateFloatingCapsuleUiState(...)`，将设置 / 权限 / source / 可选 `HeartRateState` 映射为胶囊状态。

当前生产路径：

| 输入 | Collapsed label |
|---|---|
| `heartRateDisplayEnabled=false` | 隐藏 |
| enabled + permission not granted | `权限未赋予` |
| enabled + Bluetooth disabled | `蓝牙关闭` |
| enabled + scanning source status | `正在连接` |
| enabled + saved selected device | `已选择设备` |
| enabled + permission granted + no source | `未连接源` |

Mapper-ready / test state：

| 输入 | Collapsed label |
|---|---|
| `DEVICE_CONNECTED_NO_READING` | `等待数据` |
| stale reading | `数据过期` |
| disconnected stale reading | `离线` |
| live bpm without age | `心率 {bpm} bpm` |
| live bpm with age | `低强度 / 热身 / 燃脂 / 有氧 / 无氧 / 极限 {bpm} bpm` |
| above user threshold | `超过上限 {bpm} bpm` |

年龄缺失时只显示 bpm，不显示区间。`超过上限` 只作为深红视觉状态支持，不触发声音、震动、强制暂停、通知或医疗告警。

## Drag / Snap / Safe-area 策略

新增纯 Kotlin 几何函数：

- `snapHeartRateCapsuleToSafeEdge(...)`
- `hasMovedBeyondHeartRateCapsuleDragThreshold(...)`

策略：

- 释放点按屏幕左右半区吸附到 left / right safe edge。
- 顶部避让 status bar。
- 底部避让 navigation bar / bottom nav / fixed bottom actions。
- timed / follow-along session 使用较高 bottom exclusion zone，保护训练主操作。
- strength session 使用更保守 bottom exclusion zone，保护 active / rest / confirm-record 控件、感受选择和输入区。
- IME 可见时强制 collapsed，并用更高 exclusion zone 避让键盘区域。
- 如果候选吸附点与 exclusion zone 相交，优先选择最近的 zone 上方或下方安全 Y 坐标。

这是首版固定 exclusion zones + viewport clamp 策略。后续如需要更精细避让单个 Compose 节点，可另拆基于 layout coordinates 的动态 exclusion story。

## 验证命令

已通过：

```powershell
$env:JAVA_HOME='C:\Users\25073\.cache\codex-jdks\jdk-17.0.19+10'
$env:ANDROID_HOME='C:\Users\25073\Desktop\jianshen\.local\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat app:testDebugUnitTest --tests "*HeartRate*"
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:lintDebug
.\gradlew.bat app:check
git diff --check
```

Focused tests 覆盖：

- disabled -> hidden
- enabled no source -> `未连接源`
- permission denied -> `权限未赋予`
- bluetooth disabled -> `蓝牙关闭`
- selected source -> `已选择设备`
- waiting data -> `等待数据`
- stale / offline
- bpm without age -> bpm-only
- bpm with age -> zone + bpm
- over limit -> deep red visual-only state
- left / right snap
- bottom button and confirm-record exclusion snap-away
- status / gesture inset clamp
- tap movement threshold does not become drag

## Boundary Scans

已执行：

```powershell
rg -n "SYSTEM_ALERT_WINDOW|ACTION_MANAGE_OVERLAY_PERMISSION" app/src/main
rg -n "未获取心率|手动心率|平均心率趋势|HeartRatePanel|ManualHeartRate" app/src/main
rg -n "WorkoutSession|Room|Migration|Record|History|Trend|WorkoutCommand|WorkoutEvent|TimedWorkoutEngine|StrengthWorkoutEngine|TimerDial|Notification|Sound|Haptic" app/src/main/java/com/liujyks/trainflow/ui/shell/official app/src/main/java/com/liujyks/trainflow/feature/settings app/src/main/java/com/liujyks/trainflow/core/health app/src/main/java/com/liujyks/trainflow/core/model
rg -n "startScan|connectGatt|setCharacteristicNotification|0x2A37|2A37|heart-rate notify|bpm" app/src/main
```

结果：

- overlay 权限：无命中。
- 旧 UI 文案 / 组件：无命中。
- `WorkoutSession` / records / history / sound 等命中来自既有 app shell 参数、settings 偏好、model boundary 和已有 provider / model 文件；本轮未改 Room、records/history/trends、commands/events、engines、TimerDial、sound、haptic、notification。
- `startScan` 命中既有 E16-7 设置页用户主动扫描入口、`HeartRateDeviceScanner` 和 `AndroidBleHeartRateProvider`；本轮胶囊只读状态，不调用 scan / connect / notify。
- `connectGatt` / `setCharacteristicNotification` / `0x2A37` / live `bpm` 命中既有 provider/parser/model 与 mapper-ready UI state；胶囊生产接入不启动 GATT，不订阅 notify，不伪造训练页真实 bpm。

## AVD Smoke

固定 AVD 要求：

```text
TrainFlow_Pixel_API_36
```

证据目录：

```text
.local/smoke/e16-8-heart-rate-floating-capsule/
```

补采结果：`TrainFlow_Pixel_API_36` 已确认存在，已启动为 `emulator-5554`，`sys.boot_completed=1`，并完成 E16-8 App-shell floating heart-rate capsule AVD UI smoke。

```text
.local/smoke/e16-8-heart-rate-floating-capsule/00-adb-devices.txt
```

已保存以下证据：

- `00-adb-devices.txt`
- `01-disabled-hidden.png` / `01-disabled-hidden.xml`
- `02-enabled-no-source-capsule.png` / `02-enabled-no-source-capsule.xml`
- `03-expanded-state.png` / `03-expanded-state.xml`
- `04-drag-snap-left.png` / `04-drag-snap-left.xml`
- `05-drag-snap-right.png` / `05-drag-snap-right.xml`
- `06-settings-no-overlap.png` / `06-settings-no-overlap.xml`
- `07-timed-active-no-overlap.png` / `07-timed-active-no-overlap.xml`
- `08-strength-active-no-overlap.png` / `08-strength-active-no-overlap.xml`
- `09-strength-rest-no-overlap.png` / `09-strength-rest-no-overlap.xml`
- `10-strength-confirm-record-no-overlap.png` / `10-strength-confirm-record-no-overlap.xml`
- `11-strength-completion-no-overlap.png` / `11-strength-completion-no-overlap.xml`
- `12-bottom-nav-no-overlap.png` / `12-bottom-nav-no-overlap.xml`
- `bounds-check.txt`
- `bounds-evidence.json`
- `logcat-fatal-anr-scan.txt`

模拟器覆盖：

- 默认 `heartRateDisplayEnabled=false` 时胶囊隐藏。
- 授予模拟器蓝牙运行时权限并开启心率显示后，胶囊显示 `未连接源`，未出现设备列表。
- 轻点胶囊可展开，expanded 态提供 `心率与设备` 入口；展开本身不请求权限、不扫描、不连接。
- 胶囊可拖动并吸附到左右安全边，轻点没有被误判为拖动。
- settings、timed active、strength active、strength rest、strength confirm-record、strength completion、bottom nav 的关键控件无遮挡。
- `bounds-check.txt` / `bounds-evidence.json` 结果为 `overall=PASS`；胶囊未与 bottom nav、固定底部主按钮、confirm-record 实际重量 / 次数输入、感受选择、completion 返回按钮、TimerDial center / main action 相交。
- `logcat-fatal-anr-scan.txt` 结果为 PASS：TrainFlow 进程 logcat 未发现 FATAL / ANR / BLE scan / connect / notify 关键词，crash buffer 未发现 TrainFlow FATAL / ANR。
- 生产 TrainFlow UI 未出现 `HR Broadcast Smoke`。

本次未改功能代码，属于 docs-only smoke update。真机 BLE 外设测试仍只能由用户人工完成；真实 Band 9 / BLE bpm 数据获取仍为后续人工测试，不阻塞 E16-8。

## 禁止范围确认

本轮未做：

- 未写 session record。
- 未改 Room schema / migration。
- 未改 records / history / trends。
- 未做 1s sampling persistence。
- 未做 post-workout analysis / zones summary。
- 未改 `WorkoutCommand` / `WorkoutEvent`。
- 未改 `TimedWorkoutEngine` / `StrengthWorkoutEngine`。
- 未改 TimerDial。
- 未改声音、震动、通知或 cue。
- 未使用 `SYSTEM_ALERT_WINDOW` / “显示在其他应用上层”权限。
- 未做后台浮窗。
- 未恢复旧心率卡片、`未获取心率`、手动心率输入或旧平均心率趋势。
- 未提交 `.local/`、APK、音频、deliverables、人工、截图、日志或 build 输出。

## 后续

E16-9 `HeartRateState` -> capsule mapping / real provider state hardening 仍未开始；本轮只提供 mapper-ready 分支和 focused tests。

E16-10 stale / offline policy、E16-11 recording model / 1-second sampling persistence、E16-12 analysis / zones / post-workout summary 仍未开始。由于本轮没有落库或分析，后续编号无需调整。
