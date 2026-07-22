# E17-6 Deterministic Android BLE Runtime Owner

**状态：** `reviewed / merged`

**Git 事实：** immutable SHA `f9188c09275cd01dbf182823b3886635b17105bc`；merge commit `503d3151d731565837ab76f44fbebc25bb982e0d`

**日期：** 2026-07-21

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

以下为merge前历史分支快照：

- merge前：E17-6曾为`implemented / needs review`；E17-7曾因E17-6前置而`planned / prerequisite-gated`。
- 该E17-6门禁现已满足；E17-6状态以页首Git事实为准。此处不生成E17-7当前任务；Planning Repair / E17-7状态按本文末统一条件式真值判定。
- 未创建E17-6状态docs-sync、递归closeout或E17-7分支。

本轮 Review Repair 2 的 executable source commit 为 `2b1a974d67d4774cb0699434ecbbbf5a655c02ed`，commit message 为 `Classify nonzero GATT callback failures`。Repair 2 APK 由该 commit 的 executable tree 强制重建；后续最终 Story tip 只增加本文档证据记录，不改变 APK、production 或 tests。前一 Review Repair 的 executable source `018332eb13636771b79d6cc8bb8216738ead5093` 与其 APK / AVD evidence 只保留为历史分层证据，不是本轮 executable tree 的等价物。

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

显式 concrete sealed actions 为：`Enable`、`Disable`、`PermissionLost`、`BluetoothOff`、`BackgroundCleanup`、`StartScan`、`StopScan`、`Connect(identifier)`、`Disconnect` 与 `Stop`。没有新增 interface。构造、permission 恢复、Bluetooth 恢复、timeout、freshness 与 callback 都不会创建 scan、connect、reconnect、retry 或 target switch。

可逆 lifecycle 合同如下：

- 新 owner 的 `enabled=false`，初始公开事实为 `Disabled`。disabled 时 `StartScan` / `Connect` 不读取 permission / adapter、不调用 BLE 平台，事实保持 `Disabled`。
- `Enable` 只把无 active resource 的 owner 恢复为 `NotConnected`；不恢复旧 attempt、candidate、GATT、callback、timeout 或用户动作。active scan / attempt 上重复 `Enable` 是 no-op。
- `Disable` 先使 operation 不再 eligible，再由统一 cleanup 失效 generation / attempt 和引用、取消 tasks、关闭 detached resources，最终发布 `Disabled` 并清除 bpm / `measuredAt`。重复 `Disable` 不再触碰平台资源。
- enabled 时 `PermissionLost` / `BluetoothOff` / `BackgroundCleanup` 分别 cleanup 并发布 `PermissionRequired` / `BluetoothOff` / `IntentionalStop`；disabled 时三者均保持 `Disabled`。这些 loss / cleanup actions 会冻结后续排队的 scan / connect，只有新的 `Enable` 才恢复到 `NotConnected`。
- `Disconnect` 保持可逆主动断开，不设置 `ownerClosed`；`Stop` 才永久 close owner。`Disable -> Enable ->` 用户显式 scan / connect 和 `Disconnect ->` 用户显式 scan 都在同一 owner 实例上通过测试。
- 排队回归覆盖 `PermissionLost -> StartScan -> Connect -> Enable`：loss 真正消费后，旧 scan / connect 即使在恢复竞态中排队也不能启动，最后只到 `NotConnected`。

对外只输出：

- 现有 `HeartRateState` / `HeartRateProvider` flow。
- 现有窄 `BleHeartRateDeviceCandidate` 列表。
- 现有窄 `BleHeartRateScanState` compatibility fact。

`BluetoothDevice`、address 的平台对象语义、`BluetoothGatt`、callback、characteristic、descriptor 与异常 message 均留在 owner 内。identifier 只在当前候选和用户动作中使用，不作为 raw callback identity 或永久设备身份。

所有 public action 总是先 post；scan / GATT callback 若已在 owner main looper 上可同步重入，否则只捕获必要参数并 post。generation、attempt、raw resource 引用、timeline 与 public state 只在同一个 main `Handler` 上改变。

## 6. Scan 与 callback identity 证据

Scan 使用标准 HRS `0x180D` `ScanFilter` 和有限 window。每次 scan 创建单调 generation 与唯一 callback；重复 start 先失效并停止旧 callback，再创建新 generation。result / failure / timeout 同时匹配 current generation 与 callback identity；旧 result、failure、timeout 和 cleanup 后 callback 不增加 candidate、不改状态、不连接。

Connect 固定顺序改为：先同时验证新 identifier 仍存在于当前 candidate device map 与 candidate fact；验证成功后才停止 scan、失效并关闭旧 attempt；再激活带 ID / generation / target / phase 的新 attempt、创建捕获 immutable identity 的 callback并调用 API 26+ handler overload：

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

stale candidate 保护固定为：active attempt 存在时，stale / invalid / 已消失 identifier 直接忽略，不 cleanup 当前 GATT、不发布全局 `CONNECT_FAILED`、不改 timeline / bpm / `measuredAt`、不调用 platform connect。无 active attempt 时 invalid target 继续形成 typed `CONNECT_FAILED`。有效新 target 的 interceptor 内断言旧 GATT 已先关闭，证明顺序为“验证新候选 -> 失效/关闭旧 attempt -> 新 connect”。

## 7. 标准 HRS 与 freshness

当前 matching attempt 严格推进：connected -> `discoverServices()` -> `0x180D` -> `0x2A37` -> notify 优先 / indicate fallback -> `0x2902` -> `setCharacteristicNotification` -> descriptor write -> descriptor success -> `WaitingFirstData`。

raw GATT identity gate 之后还必须通过 phase gate：

| callback | 唯一允许 phase | 错误 phase 结果 |
|---|---|---|
| `STATE_CONNECTED` | `CONNECTING` | 忽略；不重复 discover |
| `onServicesDiscovered` | `DISCOVERING` | 忽略；不访问 service / characteristic，不重复 notification / descriptor write |
| `onDescriptorWrite` | `SUBSCRIBING` | 忽略；不得重建 Waiting timeline；Live 不回退 |
| notify overloads | `WAITING_FIRST_DATA` / `LIVE` / `DATA_INTERRUPTED` | 订阅完成前及其他 phase 忽略，不解析、不刷新 timeline |

`CONNECTING` 在 `connectGatt()` 前建立，`DISCOVERING` 在 `discoverServices()` 前建立，`SUBSCRIBING` 在 descriptor write 前建立，因此现有同步早到 callback-first / return-same 证据继续成立。production callback 回归覆盖 duplicate connected、duplicate / late services、Live 后 late descriptor success、descriptor 完成前 notify、CONNECTING 中 out-of-order services / descriptor / notify，以及错误 phase 不产生后续平台调用、状态回退、timeline / freshness 重排或合法 GATT close。

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

明确 active GATT 的平台 disconnect callback先通过 attempt / raw GATT identity，再按 phase 分类，不能先让非零 status 覆盖 `STATE_DISCONNECTED`：

| `newState` / phase | status | public fact |
|---|---:|---|
| `STATE_DISCONNECTED` + `CONNECTING` | `0`、`19` 或其他 | `TechnicalFailure(CONNECT_FAILED)` |
| `STATE_DISCONNECTED` + `DISCOVERING` / `SUBSCRIBING` / `WAITING_FIRST_DATA` / `LIVE` / `DATA_INTERRUPTED` | `0`、`19` 或其他 | `LinkDisconnected` |
| 非 disconnected callback | 非零 | 对应 typed technical failure；不按 message、厂商字符串或 status 19 特判 |

Review Repair 2 发现 `handleConnectionStateChange()` 的旧顺序为“明确 disconnected -> 成功 connected phase gate -> 非零 status”，导致 WAITING / LIVE 中非 disconnected 的 `status = 19` 被 phase gate 提前吞掉。当前顺序固定为：先通过 raw GATT、attempt ID、owner generation 与 target identity gate；再按当前 phase 分类明确 `STATE_DISCONNECTED`；随后把任何其他非零 status 发布为 `TechnicalFailure(CONNECT_FAILED)`并统一 cleanup；只有 status 成功后才对 `STATE_CONNECTED` 应用仅允许 `CONNECTING` 的 phase gate。其他成功但无对应转换的 callback 继续无副作用。

production callback 回归在 WAITING_FIRST_DATA 与 LIVE 中分别发送 `onConnectionStateChange(currentGatt, 19, STATE_CONNECTED)`，断言 typed failure、bpm / `measuredAt` 清空、freshness取消、attempt失效、当前GATT disconnect / close，以及旧 freshness closure和迟到 notify / services / descriptor / connected callback均不能恢复 attempt或Live。成功 duplicate connected 对照在 WAITING / LIVE 中仍被 phase gate忽略，不重复`discoverServices()`、不改公共事实或timeline、不关闭合法GATT。

用户 `Disconnect` / `Disable` / background cleanup / `Stop` 已先失效 attempt；随后迟到的 status 19 disconnected callback被 identity gate 拒绝，不覆盖 intentional / disabled fact。测试覆盖 status 19 在 CONNECTING、WAITING、LIVE 与 intentional cleanup 后四种路径；所有非 Live 结果均断言 bpm / `measuredAt` 为空。E17-1 的 status 19 仍只作为历史设备观察，本 Repair 没有运行或宣称 Band 9 验证。

## 10. Evidence 分层与边界

- 纯状态 / identity harness：generation、attempt ID、raw GATT首次绑定与 mismatch gate；它不证明 Android framework 或 RF。
- production owner deterministic path：所有 queue、callback、HRS、freshness、TOCTOU 与 cleanup tests 直接实例化并执行 production `HeartRateRuntimeOwner`。
- Robolectric / Android 平台路径：API 35覆盖 API 33+ path；API 32覆盖 legacy descriptor path；真实 Android BLE types / shadows 证明调用形状与同步重入，不证明无线电。
- AVD：`TrainFlow_Pixel_API_36` 仅做旧 production path 的 install / launcher / settings permission 与 Bluetooth no-crash smoke。新 owner未接 production，因此 AVD不能证明其行为；E17-9锁屏/后台证据必须观察同一Application owner。
- 真实 BLE / Band 9：本 Story不要求且未运行。E17-7原子接入后才有 production Band 9 basic gate；E17-9才有锁屏 / 后台 gate。

AVD 不能证明 RF、GATT、`0x180D`、`0x2A37`、`0x2902`、CCCD、notify 或 Band 9。

## 11. 依赖、容量与已知限制

- 新增第三方依赖：0。
- 新增 interface / BLE seam / wrapper / platform adapter / scheduler / actor / dispatcher ownership层：0。
- production 仅有一个 concrete owner 文件：Repair 后为 `1224` physical LOC、`54` 个 `fun` declarations（包含 public / override callback 与 private helper）。无其他 production 文件修改。
- E17-4 对 E17-6 的规划估算为 650–950 production 行、30–45 个窄方法；Repair 后分别高于上限 `274` 行（`+28.8%`）和 `9` 个方法（`+20.0%`）。新增体积来自五个 concrete lifecycle handler、operation eligibility、callback phase matrix、disconnect phase classification 与 candidate-before-cleanup ordering；没有用抽象或依赖换取缩行。独立 Review 必须继续审查该容量偏差。

已知限制 / E17-7 handoff：

- 新 owner 当前无 production consumer，默认不可从 Application、Activity、Compose、settings、胶囊或 debug launcher取得。
- 旧 provider/scanner/DTO仍存在并保持当前 production/debug wiring；E17-7必须在同一 Story 原子切换并退休旧 runtime ownership path。
- 本 Story没有 process visibility；E17-7实现 fail-closed reducer。
- 本 Story没有 FGS / background keepalive；E17-9实现connected-device FGS，并以shared Application owner observer形成M1，不能复用独立GATT工具。
- 不实现 retry、backoff、reconnect、saved DataStore wiring、Room记录或分析。

## 12. Review Repair finding 关闭记录

1. **Must-fix — 可逆 owner lifecycle inputs：已关闭。** 初始 `Disabled`、可逆 Enable / Disable、permission loss、Bluetooth off、background cleanup、可逆 Disconnect、永久 Stop、disabled platform no-op、同 owner restart 与排队恢复竞态均由 production owner tests 覆盖。
2. **Must-fix — attempt phase gate：已关闭。** raw identity 之后加入 callback-phase 矩阵；同步早到 callback所需 phase 均在具体平台调用前建立；duplicate / late / out-of-order production callback不产生平台操作、timeline刷新或 Live回退。
3. **Must-fix — 非零 status 明确断连分类：已关闭。** disconnected先按 phase 分类；CONNECTING -> `CONNECT_FAILED`，已建立链路 phase -> `LinkDisconnected`，status 19不再覆盖明确断连语义；intentional cleanup 后迟到 callback被拒绝。
4. **Should-fix — invalid target保护当前连接：已关闭。** candidate验证先于旧 attempt cleanup；active Live + stale / invalid保持原 state / timeline / GATT，有效新 target按序切换，无 active attempt invalid仍保留 typed failure。
5. **Review Repair 2 must-fix — 非 disconnected 的非零 GATT status 被 phase gate 吞掉：已关闭。** 判断顺序已固定为 `disconnected -> nonzero status -> successful connected phase gate`；WAITING / LIVE 的非 disconnected status 19 均形成 `TechnicalFailure(CONNECT_FAILED)`并 cleanup，成功 duplicate connected仍保持忽略。前四项 finding 的 production regression 与完整 owner / HeartRate / unit suites继续通过并保持关闭。

本 Repair 没有开始 E17-7，没有 production 接线，没有 reconnect / retry，也没有修改冻结胶囊、旧 provider / scanner / DTO、E17-5 core、Application、Compose、settings、debug Activity、Manifest、Gradle、Service、FGS、notification、Room或训练 Route。

## 13. Android 官方依据

- [BluetoothDevice / connectGatt](https://developer.android.com/reference/android/bluetooth/BluetoothDevice)
- [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)
- [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback)
- [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

当前 compile / target SDK 为 36；API 37 deprecation不属于本 Story。

## 14. Repair 验证记录

环境：Eclipse Temurin OpenJDK `17.0.19+10`、Gradle `9.4.1`、Kotlin `2.3.0`、Windows 11。以下均为 `BUILD SUCCESSFUL`，JUnit 数字顺序为 tests / failures / errors / skipped：

- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerTest*"`：`11 / 0 / 0 / 0`。
- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerCallbackIdentityTest*"`：`6 / 0 / 0 / 0`。
- `:app:testDebugUnitTest --tests "*HeartRateRuntimeOwnerPlatformTest*"`：`28 / 0 / 0 / 0`。
- 三个 focused files 合计 `45` tests；原 Story 为 `29`，前一 Repair 新增 `14` 个回归，本 Repair 2 再新增 `2` 个 production callback 回归。
- `:app:testDebugUnitTest --tests "*HeartRate*"`：`128 / 0 / 0 / 0`（19 suites）。
- `:app:testDebugUnitTest`：`747 / 0 / 0 / 0`（76 suites）。
- `:app:assembleDebug --rerun-tasks`：通过。
- `:app:lintDebug --no-daemon --console=plain "-Dkotlin.incremental=false"`：通过。
- `:app:check`：通过。

Robolectric API 35执行API 33+ path，method-level API 32执行legacy path。本 Repair 2未运行AVD；前一 executable source的API 36 AVD只保留为旧production runtime no-regression历史证据，新owner仍未接线。

Repair 2 APK build identity：

- Executable source SHA：`2b1a974d67d4774cb0699434ecbbbf5a655c02ed`。
- Debug APK：variant `debug`，applicationId `com.liujyks.trainflow`，`14763914` bytes，SHA256 `C5C3E773C55DE83C8C5CD8255A62BFB6FD3A5CE69ED48FC7E1C6DC57412ACA72`。
- 该APK只完成强制build与身份记录，未install、未运行AVD、未运行Band 9，也不构成新owner设备验证。由于新owner仍无production consumer，AVD只会执行旧production runtime，本轮确定性production callback Robolectric回归是本finding的行为证据。

前一 Review Repair AVD历史证据：

- 前一 executable source SHA：`018332eb13636771b79d6cc8bb8216738ead5093`。
- 前一 debug APK：variant `debug`，applicationId `com.liujyks.trainflow`，`14763914` bytes，SHA256 `C050DD9F3D793DFD5F6437A4818F1627A448C945DC075C6735470A99D0836478`；当时`adb install -r`成功。
- AVD：`TrainFlow_Pixel_API_36` / `emulator-5554` / `sdk_gphone64_x86_64` / Android 16 / API 36。
- ordinary `MainActivity` cold launch成功；既有心率设置入口可达；通过adb revoke观察permission denied / required事实并重新grant；Bluetooth service OFF / ON均无crash；旧路径明确进入“扫描中 / 约12秒”，到期形成“未发现心率设备 / 重新扫描”。
- `logcat` TrainFlow FATAL / ANR、crash buffer package match与`dumpsys activity lastanr` package match均为0；最终进程仍存活。
- 前一 Repair evidence：`.local/smoke/e17-6-deterministic-android-ble-runtime-owner/repair/`，未提交。
- 此旧 smoke只证明前一 executable source下的API 36 launcher/settings/旧production runtime no-regression；不执行或证明新owner，不证明RF / GATT / HRS / CCCD / notify / Band 9，也不是本轮tip的完整可执行等价物。
- 本 Repair 2不运行AVD或Band 9，不要求用户人工测试；旧APK、旧AVD与旧test count均不作为当前Repair executable等价物。

Production reachability 静态门禁：

- `rg -n "HeartRateRuntimeOwner\\s*\\(" app/src/main app/src/debug app/src/test`：`app/src/main`仅命中新 owner 类定义；`app/src/debug` 0；所有实例化均在三个E17-6测试文件。
- `TrainFlowApp.kt`仍创建 `AndroidHeartRateDeviceScanner`；`HeartRateDeviceScanner.kt`仍创建旧 `AndroidBleHeartRateProvider`；debug `HeartRateBroadcastSmokeActivity.kt`仍直接引用旧 provider。
- 这是静态 reachability / wiring evidence，不是 runtime双owner证明。由于新 owner无production consumer，旧 runtime仍是唯一production可达BLE owner。

提交前/后另执行 `git diff --check`、`git diff --cached --check` 与 `git diff --check origin/main...HEAD`。

当前交接的稳定事实：E17-6已完成独立Review/merge；新owner的production/debug实例化仍为0，旧`AndroidBleHeartRateProvider` / `AndroidHeartRateDeviceScanner`路径仍production可达。

**Planning Repair / E17-7 统一条件式真值：** 若本Planning Repair immutable SHA尚未通过独立Review，或尚未完成`--no-ff` merge/push，或该SHA尚不是同步后的`main`与`origin/main` ancestor，或`main...origin/main`不为`0 0`，或七份权威文档不一致，则Planning Repair=`implemented / needs review`、E17-7 planning prerequisite=`not satisfied`、E17-7=`planned / prerequisite-gated`，只允许独立Review/Repair本Planning Repair，不得启动E17-7。全部条件满足后，Planning Repair自动为`reviewed / merged`、E17-7 planning prerequisite自动为`satisfied`；不需要额外docs-sync，不创建递归closeout，主管理从Git解析最终Repair SHA与merge事实后决定后续提示词。Git ancestry是merge事实；branch name仅为locator，不是merge事实。
