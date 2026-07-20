# E17-6 Deterministic Android BLE Runtime Owner

**状态：** `implemented / needs review`

**日期：** 2026-07-20

## 1. 目标与范围

本 Story 新增尚未接入 production composition root、只由 unit / Robolectric tests 直接实例化的 `HeartRateRuntimeOwner`。它实现现有 `HeartRateProvider`，在单一 Android main `Handler` 上完成 scanner、scan callback、target、connect attempt、raw `BluetoothGatt`、`BluetoothGattCallback`、timeout、freshness 与 cleanup 的确定性所有权。

本 Story 不修改或退休旧 `AndroidBleHeartRateProvider`、`HeartRateDeviceScanner` 与旧 BLE DTO，不接入 `TrainFlowApplication`、Activity、Compose、settings、胶囊、Route、notification、Service、FGS、Manifest、Room、记录、复盘、导出、分析或自动恢复。E17-6 合并后，旧 runtime 仍是唯一 production / debug 可达 BLE owner；E17-7 才能原子切换并退休旧 ownership path。

## 2. Git 与条件式状态

开始时重新执行并通过：

- `main == origin/main == bfb065b92d2ec78ca794fa679f7e25e85093bc79`。
- `main...origin/main = 0 0`。
- E17-5 immutable SHA `959146a7e41a38d654b4988ba0d443f2aea0d874` 为 `main` ancestor。
- E17-4 immutable SHA `1ea67561b4866aa76c41b854da74da85c208aa25` 为 `main` ancestor。
- 禁止 E16 SHA `89d1e23f870185a2e279d35bb293883f64fe70ba` 不是 `main` ancestor。
- 分支从同步后的 `main` 创建：`codex/e17-6-deterministic-android-ble-runtime-owner`。

状态真值固定为：

- merge 前：E17-6 = `implemented / needs review`；E17-7 = `planned / prerequisite-gated`。
- 独立 Review 通过、merge / push 完成、E17-6 immutable full SHA 成为 `main` ancestor、`main...origin/main = 0 0` 且权威文档一致后：E17-6 自动为 `reviewed / merged`，E17-7 gate 自动 satisfied。
- 不创建 E17-6 状态 docs-sync、递归 closeout 或 E17-7 分支。

## 3. 实际文件范围

Production：

- 新增 `app/src/main/java/com/liujyks/trainflow/core/health/HeartRateRuntimeOwner.kt`。
- `BleHeartRatePermissionPlanner.kt` 无需修改。

Tests：

- 新增 `HeartRateRuntimeOwnerTest.kt`。
- 新增 `HeartRateRuntimeOwnerCallbackIdentityTest.kt`。
- 新增 `HeartRateRuntimeOwnerPlatformTest.kt`。
- 所有 fixture、Robolectric shadow 与 helper 均位于上述测试文件内；未新增共享测试架构。

Documentation：

- 新增本文档。

未修改 parser、E17-5 freshness policy、旧 provider/scanner/DTO、debug Activity 或任何其他 production 文件。

## 4. Test-first seam 可行性结论

实现前先确认现有 Robolectric 4.16.1 的 `ShadowBluetoothDevice.BluetoothGattConnectionInterceptor` 在 `connectGatt(...)` 内、raw `BluetoothGatt` 返回前同步执行。测试可在 interceptor 中：

1. 取得平台创建的 raw `BluetoothGatt`。
2. 从 `ShadowBluetoothGatt` 取得本次 production owner 创建的真实 `BluetoothGattCallback`。
3. 在 `connectGatt()` 尚未返回时同步调用 `onConnectionStateChange()`。
4. 使用同一 raw GATT 验证 callback-first / return-same；或构造第二个 raw GATT 验证 callback-first / return-mismatch。

因此没有新增 `AndroidBleOperations`、platform adapter、generic `call(operation, block)`、完整 GATT wrapper、parallel device model、第二 callback owner 或核心 interface。owner 内的窄 identity harness 只比较 attempt ID、owner generation、target identifier 与 raw GATT identity，不包装平台 API、不接收 arbitrary business lambda、不分类通用异常。

## 5. Owner action、state 与资源边界

显式 typed action 只有：`StartScan`、`StopScan`、`Connect(identifier)`、`Disconnect` 与 `Stop`。构造、permission 恢复、Bluetooth 恢复、timeout、freshness 与 callback 都不会创建 scan、connect、reconnect、retry 或 target switch。

对外只输出：

- 现有 `HeartRateState` / `HeartRateProvider` flow。
- 现有窄 `BleHeartRateDeviceCandidate` 列表。
- 现有窄 `BleHeartRateScanState` compatibility fact。

`BluetoothDevice`、address 的平台对象语义、`BluetoothGatt`、callback、characteristic、descriptor 与异常 message 均留在 owner 内。identifier 只在当前候选和用户动作中使用，不作为 raw callback identity 或永久设备身份。

所有 public action 总是先 post；scan / GATT callback 若已在 owner main looper 上可同步重入，否则只捕获必要参数并 post。generation、attempt、raw resource 引用、timeline 与 public state 只在同一个 main `Handler` 上改变。

## 6. Scan 与 callback identity 证据

Scan 使用标准 HRS `0x180D` `ScanFilter` 和有限 window。每次 scan 创建单调 generation 与唯一 callback；重复 start 先失效并停止旧 callback，再创建新 generation。result / failure / timeout 同时匹配 current generation 与 callback identity；旧 result、failure、timeout 和 cleanup 后 callback 不增加 candidate、不改状态、不连接。

Connect 固定顺序为：停止 scan -> 失效旧 attempt -> 激活带 ID / generation / target / phase 的新 attempt -> 创建捕获 immutable identity 的 callback -> 调用 API 26+ handler overload：

```kotlin
device.connectGatt(
    context,
    false,
    callback,
    BluetoothDevice.TRANSPORT_LE,
    BluetoothDevice.PHY_LE_1M_MASK,
    mainHandler
)
```

同步 interceptor test 在该调用返回前推进 callback raw GATT 首次绑定、service discovery、notification enable、CCCD write 与 `WaitingFirstData`；测试内的断言明确位于 interceptor 内。return-same 保持 active；return-mismatch 关闭 returned non-active GATT并保留 callback-bound raw GATT；early failure 先失效 attempt，随后返回对象被关闭。old attempt、wrong target/generation、wrong raw GATT 与 cleanup 后 late service / descriptor / notify / disconnect callback 均被拒绝。

production owner 不存在 `lateinit connection`；同步重入 test 真实执行 production callback / identity / platform path，不是独立 helper 或源码搜索。

## 7. 标准 HRS 与 freshness

当前 matching attempt 严格推进：connected -> `discoverServices()` -> `0x180D` -> `0x2A37` -> notify 优先 / indicate fallback -> `0x2902` -> `setCharacteristicNotification` -> descriptor write -> descriptor success -> `WaitingFirstData`。

- API 33+ 使用 `BluetoothGatt.writeDescriptor(descriptor, value)` 并以 `BluetoothStatusCodes.SUCCESS` 判断。
- API 26–32 先在异常分类边界外设置 legacy descriptor value，再调用 legacy `writeDescriptor(descriptor)`。
- API 33+ value callback 与 legacy callback 均支持；同一 characteristic、payload 与 monotonic receive point 的双 overload 回调只消费一次。
- matching raw GATT 的 valid payload 通过既有 `HeartRateMeasurementParser` 进入 `Live`。
- malformed / zero reading 只递增 attempt 内 diagnostic，不刷新 last-valid origin、不替换 public fact、不续命。
- 既有 E17-5 `HeartRateFreshnessPolicy` 在 waiting / live deadline 运行，精确边界清除 bpm / measuredAt 并进入 `DataInterrupted`。
- service、characteristic、CCCD、notification enable、descriptor status 或 write start 的明确失败形成 typed technical failure并 cleanup。

本 Story只消费 parser 与 freshness policy，未修改二者。

## 8. Permission TOCTOU

availability 只是 action 开始时的快照。production owner 在以下具体调用分别只捕获 `SecurityException`：

- adapter enabled / scanner 读取。
- `startScan` / `stopScan`。
- callback 时 device identifier / display name 读取。
- handler overload `connectGatt`。
- `discoverServices`。
- service / characteristic / descriptor 访问。
- `setCharacteristicNotification`。
- API 33+ 与 legacy descriptor write。
- `disconnect` / `close`。

Robolectric platform tests 通过真实 production 调用与 test-file-local shadows 注入 scan start/stop、connect、discover、notification、descriptor、disconnect 与 close 的撤权；connect test 使用只让 availability snapshot 看见 granted 的 `ContextWrapper`，同时撤销 Robolectric instrumentation permission，使真实 `BluetoothDevice.connectGatt()` 抛 `SecurityException`。permission 恢复后不自动产生任何动作。

未知 `IllegalStateException` 注入 discovery 后从 main queue 原样抛出，测试确认它没有被改写成 permission 或 Bluetooth off。production 没有捕获 `IllegalStateException`、`RuntimeException`、`Throwable`，也不按 exception message 或厂商字符串分类。

## 9. Cleanup 顺序与幂等

统一 cleanup 顺序为：

1. 递增 / 失效 scan generation 与 attempt sequence。
2. 清空 active scan、attempt、target device map 与 candidates。
3. 取消 scan timeout 与 freshness runnable。
4. 使用只属于本次 cleanup 的 detached resource snapshot。
5. best-effort stop scan。
6. best-effort disconnect。
7. best-effort close。
8. 若任一 cleanup 平台调用撤权，发布一次 `PermissionRequired`；否则发布请求的准确 terminal fact。

active 引用在 `disconnect()` 前已清空，因此 Robolectric shadow 的同步 `STATE_DISCONNECTED` callback 也只能走 stale identity gate，不能把 intentional stop 改写为 `LinkDisconnected`。stop / disconnect 抛 `SecurityException` 不阻止 close；重复 Stop 不再触碰 detached resource；已取消 timeout / freshness closure和旧 callback即使被测试主动执行也不能恢复 scan、attempt 或 `Live`。owner close 后忽略所有新 action，不自动启动任何能力。

明确 active GATT 的平台 disconnect callback产生 `LinkDisconnected`；用户 `Disconnect` / `Stop` cleanup产生 intentional fact，两条路径分层。

## 10. Evidence 分层与边界

- 纯状态 / identity harness：generation、attempt ID、raw GATT首次绑定与 mismatch gate；它不证明 Android framework 或 RF。
- production owner deterministic path：所有 queue、callback、HRS、freshness、TOCTOU 与 cleanup tests 直接实例化并执行 production `HeartRateRuntimeOwner`。
- Robolectric / Android 平台路径：API 35覆盖 API 33+ path；API 32覆盖 legacy descriptor path；真实 Android BLE types / shadows 证明调用形状与同步重入，不证明无线电。
- AVD：`TrainFlow_Pixel_API_36` 仅做旧 production path 的 install / launcher / settings permission 与 Bluetooth no-crash smoke。新 owner未接 production，因此 AVD不能证明其行为。
- 真实 BLE / Band 9：本 Story不要求且未运行。E17-7原子接入后才有 production Band 9 basic gate；E17-9才有锁屏 / 后台 gate。

AVD 不能证明 RF、GATT、`0x180D`、`0x2A37`、`0x2902`、CCCD、notify 或 Band 9。

## 11. 依赖、容量与已知限制

- 新增第三方依赖：0。
- 新增 interface / seam / wrapper / platform adapter / scheduler / actor / dispatcher ownership层：0。
- production 仅新增一个 concrete owner 文件：`1099` physical LOC、`49` 个 `fun` declarations（包含 public/override callback 与 private helper）。无其他 production 修改行。
- E17-4 对 E17-6 的规划估算为 650–950 production 行、30–45 个窄方法；实际分别高 `149` 行（相对上限 `+15.7%`）和 `4` 个方法（相对上限 `+8.9%`）。超出部分来自：在没有 operations seam / wrapper 的单文件内保留每个具体 BLE 调用的独立 `SecurityException` 边界、scan 与 attempt 双 identity、API 33+ / legacy callback和descriptor分支，以及明确的 ordered cleanup。没有用抽象或依赖换取缩行；独立 Review 必须把该偏差作为容量审查输入。

已知限制 / E17-7 handoff：

- 新 owner 当前无 production consumer，默认不可从 Application、Activity、Compose、settings、胶囊或 debug launcher取得。
- 旧 provider/scanner/DTO仍存在并保持当前 production/debug wiring；E17-7必须在同一 Story 原子切换并退休旧 runtime ownership path。
- 本 Story没有 process visibility；E17-7实现 fail-closed reducer。
- 本 Story没有 FGS / background keepalive；E17-9实现 connected-device FGS。
- 不实现 retry、backoff、reconnect、saved DataStore wiring、Room记录或分析。

## 12. Android 官方依据

- [BluetoothDevice / connectGatt](https://developer.android.com/reference/android/bluetooth/BluetoothDevice)
- [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)
- [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback)
- [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

当前 compile / target SDK 为 36；API 37 deprecation不属于本 Story。

## 13. 提交前验证记录

环境：Eclipse Temurin OpenJDK `17.0.19+10`、Gradle `9.4.1`、Kotlin `2.3.0`、Windows 11。以下均为 `BUILD SUCCESSFUL`：

- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerTest*"`：通过。
- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerCallbackIdentityTest*"`：通过。
- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerPlatformTest*"`：通过。
- `:app:testDebugUnitTest --tests "*HeartRate*"`：通过。
- `:app:testDebugUnitTest`：通过。
- `:app:assembleDebug --rerun-tasks`：通过。
- `:app:lintDebug -Dkotlin.incremental=false`：通过。
- `:app:check`：通过。

三个新增 test files 共 `29` 个 `@Test`。Robolectric API 35 执行 API 33+ path，method-level API 32执行 legacy path；API 36由下述 AVD no-regression smoke覆盖，但新 owner未接线。

AVD：

- `TrainFlow_Pixel_API_36` / `emulator-5554` / `sdk_gphone64_x86_64` / Android 16 / API 36。
- 当前 forced debug APK SHA256：`C2B740775AE592D95F0BB72744CB0E481FF7B8C948F63440C5A46C7AE94C2CFC`；`adb install -r` 成功。
- ordinary `MainActivity`启动、既有心率设置入口、permission deny / grant、Bluetooth off / on与显式旧路径 scan / stop均无立即崩溃。
- `logcat`、crash buffer与`dumpsys activity lastanr` 搜索 TrainFlow FATAL / ANR 为 0；最终进程仍存活。
- evidence：`.local/smoke/e17-6-deterministic-android-ble-runtime-owner/`，未提交。
- 此 smoke只证明 API 36 当前 launcher/settings/旧production runtime no-regression；不执行或证明新 owner，不证明 RF / GATT / HRS / CCCD / notify / Band 9。

Production reachability 静态门禁：

- `rg -n "HeartRateRuntimeOwner\\s*\\(" app/src/main app/src/debug app/src/test`：`app/src/main`仅命中新 owner 类定义；`app/src/debug` 0；所有实例化均在三个E17-6测试文件。
- `TrainFlowApp.kt`仍创建 `AndroidHeartRateDeviceScanner`；`HeartRateDeviceScanner.kt`仍创建旧 `AndroidBleHeartRateProvider`；debug `HeartRateBroadcastSmokeActivity.kt`仍直接引用旧 provider。
- 这是静态 reachability / wiring evidence，不是 runtime双owner证明。由于新 owner无production consumer，旧 runtime仍是唯一production可达BLE owner。

提交前/后另执行 `git diff --check`、`git diff --cached --check` 与 `git diff --check origin/main...HEAD`。

下一步只能是独立 **E17-6 Code Review**；不得开始 E17-7。
