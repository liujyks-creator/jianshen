# E16-8 Heart-rate floating capsule

**Status:** Implemented; review-fix for real-device feedback complete; unit and AVD smoke revalidated
**Date:** 2026-07-09
**Scope:** Android app-shell floating heart-rate capsule overlay, capsule UI state mapper, drag / snap geometry, focused tests, docs

## 2026-07-09 Review Fix: 用户真机反馈

用户在 debug APK 真机测试中反馈：

1. 点击浮动心率胶囊后 expanded 显示不完整，浮层过大，像巨大椭圆遮住设置页内容。
2. expanded 内的 `心率与设备` 点击无反应。
3. debug APK 默认启动仍进入 `进入 TrainFlow` / `HR Broadcast Smoke` 两按钮 debug entry 页，阻挡日常 TrainFlow 测试。

本轮只修复 E16-8 review feedback，不进入 E16-9 state mapping，不接真实 bpm，不写记录，不做分析。

修复方式：

- expanded 从大圆角胶囊收敛为轻量 popover 卡片，最大宽度 / 高度受限；小屏或避让区不足时自动使用 compact expanded，仍不足则直接保持 collapsed，避免遮挡设置页主内容、confirm-record 输入、固定底部动作和底部导航。
- 自定义 pointer input 不再消费普通 tap；拖动只在超过 movement threshold 后消费事件，expanded 内 `心率与设备` 按钮可正常点击。
- `心率与设备` 点击会收起胶囊并打开设置页，同时通过 request key 滚动到现有 `心率与设备` 设置卡片；如果从训练 session 打开，返回设置页会回到原 session，不通过普通底部导航绕过 session lock。
- debug manifest 不再移除 MainActivity launcher，也不再给 `DebugEntryActivity` 注册 launcher intent；debug APK 默认启动直接进入 TrainFlow。`HR Broadcast Smoke` 仍保留在 `app/src/debug`，可通过 adb explicit activity 启动，但不作为普通启动首屏。

Focused tests 新增覆盖：

- regular expanded 无安全落点时可降级到 compact expanded。
- compact expanded 也无安全落点时拒绝展开。
- 胶囊心率设置捷径可从训练首页进入设置。
- 胶囊心率设置捷径可从 active timed session 打开设置并返回原 session。
- debug source set 可保留 smoke activity，但 debug manifest 不再声明 launcher；production manifest 不包含 debug entry / smoke。

## 2026-07-10 Capsule Information Panel / Settings Action Visual Fix

用户继续反馈：

1. 设置页 `心率与设备` 里的 `授权蓝牙权限` 和 `扫描心率设备` 视觉上像普通说明文字，不像可点击按钮。
2. 胶囊 expanded 里重复出现 `心率与设备` 入口，信息结构不清；expanded 应直接展示当前心率系统的具体状态信息。
3. 胶囊拖动不可靠，用户感觉“不能移动”。
4. 系统蓝牙权限弹窗点取消后，设置页授权入口可能消失，需要重启 App 才能再次看到。
5. 年龄、估算最大心率、可选手动最大心率、上限提醒阈值、区间说明和非医疗提示需要进入后续心率个人参数设置，但不在本轮完成。

修复方式：

- 胶囊 expanded 从“状态文案 + `心率与设备` 入口”改成信息面板，展示 `来源`、`记录`、`区间`、`更新` 四个状态格，并保留一行短说明。
- expanded 内不再把 `心率与设备` 作为主按钮；进入权限、扫描、选择设备仍通过设置页完成，避免胶囊误触直接触发权限或 BLE 行为。
- 设置页把 `授权蓝牙权限`、`扫描心率设备`、`停止扫描`、`选择设备` 改成明确的全宽 Button / OutlinedButton，避免看起来像注释文本。
- 胶囊移除单独的 `clickable` 修饰，统一由 pointer input 判断轻点 / 拖动；`awaitFirstDown(requireUnconsumed = false)` 避免 click / drag 互相抢事件，让拖动更可靠。
- 蓝牙权限弹窗取消后回到 `DENIED`，继续显示 `重新授权蓝牙权限` 按钮；如果后续进入系统不再弹窗状态，按钮仍显示为 `去系统设置开启`，并打开 TrainFlow 应用详情页。
- 心率个人参数设置记录为后续任务：年龄、估算最大心率、可选手动最大心率、上限提醒阈值、区间说明、非医疗提示。本轮不新增 DataStore 字段，不做区间设置 UI，不做 E16-9 state mapping，不接 live bpm，不写记录。

## 实现范围

本轮在 official app shell 顶层实现 App 内浮动心率胶囊 overlay。

- 胶囊挂在 `TrainFlowApp` 的 `Scaffold` 内容外层 `Box` 中，只显示在 TrainFlow App 内。
- `heartRateDisplayEnabled=false` 时不显示胶囊。
- `heartRateDisplayEnabled=true` 时，App 内页面显示胶囊；默认生产路径只消费设置、权限、source status 和已保存设备偏好。
- 胶囊支持 collapsed / expanded，轻点切换展开收起。
- 胶囊支持拖动，使用 movement threshold 避免轻点误判为拖动。
- 拖动释放后吸附到左右安全边。
- expanded 内展示 `来源`、`记录`、`区间`、`更新` 信息格，不再重复放置 `心率与设备` 主按钮。
- 设置页 `心率与设备` 卡片仍是权限授权、扫描设备和选择设备的唯一操作区域；胶囊 expanded 不会请求权限、扫描、连接或打开 debug smoke。
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
git diff --cached --check
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
- expanded safe placement regular -> compact fallback
- expanded safe placement no-space -> collapsed
- capsule settings shortcut and active-session return path from the earlier review fix
- debug manifest launcher boundary

## Boundary Scans

已执行：

```powershell
rg -n "SYSTEM_ALERT_WINDOW|ACTION_MANAGE_OVERLAY_PERMISSION" app/src/main
rg -n "未获取心率|手动心率|平均心率趋势|HeartRatePanel|ManualHeartRate" app/src/main
rg -n "HR Broadcast Smoke|HeartRateBroadcastSmoke|DebugEntryActivity|进入 TrainFlow" app/src/main
rg -n "HR Broadcast Smoke|HeartRateBroadcastSmoke|DebugEntryActivity|进入 TrainFlow" app/src/debug
rg -n "WorkoutSession|Room|Migration|Record|History|Trend|WorkoutCommand|WorkoutEvent|TimedWorkoutEngine|StrengthWorkoutEngine|TimerDial|Notification|Sound|Haptic" app/src/main/java/com/liujyks/trainflow/ui/shell/official app/src/main/java/com/liujyks/trainflow/feature/settings app/src/main/java/com/liujyks/trainflow/core/health app/src/main/java/com/liujyks/trainflow/core/model
rg -n "startScan|connectGatt|setCharacteristicNotification|0x2A37|2A37|heart-rate notify|bpm" app/src/main
```

结果：

- overlay 权限：无命中。
- 旧 UI 文案 / 组件：无命中。
- production source set 不包含 `HR Broadcast Smoke` / `HeartRateBroadcastSmoke` / `DebugEntryActivity` / `进入 TrainFlow`。
- debug source set 仍包含 debug-only smoke activity / debug entry class，但 debug manifest 不再声明 debug launcher；默认 launcher 由 main manifest 的 `MainActivity` 提供。
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
.local/smoke/e16-8-heart-rate-floating-capsule-review-fix/
```

Review-fix AVD smoke 使用固定 AVD `TrainFlow_Pixel_API_36`；`emulator-5554` 已完成默认启动直进 TrainFlow、expanded compact settings、点击 `心率与设备` 后进入 / 定位设置卡片、confirm-record 安全性、logcat fatal / ANR scan 和 bounds evidence 采集。

```text
.local/smoke/e16-8-heart-rate-floating-capsule-review-fix/00-adb-devices.txt
```

review-fix 已保存以下证据：

- `00-adb-devices.txt`
- `01-launch-direct-trainflow.png` / `01-launch-direct-trainflow.xml`
- `02-expanded-compact-settings.png` / `02-expanded-compact-settings.xml`
- `03-click-heart-rate-device-entry.png` / `03-click-heart-rate-device-entry.xml`
- `04-settings-card-visible.png` / `04-settings-card-visible.xml`
- `05-confirm-record-expanded-safe.png` / `05-confirm-record-expanded-safe.xml`
- `apk-badging-launcher.txt`
- `resolve-activity.txt`
- `bounds-check.txt`
- `bounds-evidence.json`
- `logcat-fatal-anr-scan.txt`

Review-fix 模拟器覆盖：

- 默认 debug launcher 直接打开 `com.liujyks.trainflow/.app.MainActivity`，首屏不再显示 `进入 TrainFlow` / `HR Broadcast Smoke` 两按钮 debug entry。
- 设置页 expanded 胶囊在 720x1280 viewport 上收敛为 `[196,234][692,590]` 的轻量 popover，`心率与设备` 按钮完整可见。
- 点击 expanded 内 `心率与设备` 后，胶囊收起并定位到设置页现有 `心率与设备` 卡片。
- confirm-record 场景空间不足时保持 collapsed；实际重量、实际次数和确认按钮保持完整可见，不被 expanded 遮挡。
- `bounds-check.txt` / `bounds-evidence.json` 结果为 `overall=PASS`。
- `logcat-fatal-anr-scan.txt` 结果为 PASS：未发现 FATAL / ANR / debug entry / BLE scan / connect / notify 关键词。
- 生产 TrainFlow UI 未出现 `HR Broadcast Smoke`。

真实 Band 9 / BLE live bpm 仍为后续人工测试，不阻塞 E16-8；本轮未接真实 bpm，也未进入 E16-9 state mapping。

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

心率个人参数设置后续另拆：年龄、估算最大心率、可选手动最大心率、上限提醒阈值、区间说明和非医疗提示。本轮只记录该需求，不实现 DataStore、设置 UI 或训练记录消费。

E16-10 stale / offline policy、E16-11 recording model / 1-second sampling persistence、E16-12 analysis / zones / post-workout summary 仍未开始。由于本轮没有落库或分析，后续编号无需调整。
