# E17-1 HUAWEI Band 9 与标准 BLE HRS 重新复验

**Story 状态：** reviewed / merged
**当前结论：** passed（当前真机证据满足 E17-1 设备与协议复验条件）
**日期：** 2026-07-15
**性质：** 设备与标准 BLE HRS 协议可行性复验；不是 production provider 实现

## Scope

本 Story 使用当前代码构建的 debug APK，在当前 Android 手机与 HUAWEI Band 9 环境中重新取得独立证据，验证：

- 心率广播关闭、开启、关闭后重新开启时的可发现条件。
- Huawei Health 与心率广播是否互斥，以及广播关闭后的恢复行为。
- 标准 Heart Rate Service `0x180D`。
- Heart Rate Measurement characteristic `0x2A37` 及其 notify / indicate 属性。
- Client Characteristic Configuration Descriptor `0x2902` 写入启动结果与 callback 结果。
- 同一已连接 Band 9 source 在订阅成功后的连续通知。
- 每条代表性通知的 raw payload bytes 与当前 `HeartRateMeasurementParser` 输出 bpm。
- 至少两个扫描 / 广播周期中的 label 与 identifier 实际表现。
- Stop / disconnect / 广播关闭后的清理结果。

## Authority and sealed-history boundary

本 Story 受 `AGENTS.md`、D-079 与 `docs/planning/e17-heart-rate-correct-course.md` 约束。E16 原始代码、testing、planning 与 design 文档是 sealed historical archive / reference only；它们只说明历史上曾观察到什么，不构成 E17-1 acceptance。

以下 sealed 文档已作为历史输入审阅，但不会被本 Story 修改：

- `docs/testing/e16-heart-rate-broadcast-feasibility-retest.md`
- `docs/testing/e16-1-ble-hrs-adapter-spike.md`
- `docs/testing/e16-2-production-ble-hrs-provider-hardening.md`

E17-1 不继承旧 APK、旧截图、旧日志、旧 label / address、旧 parser 测试或旧真机结论。本 Story 只有当前构建与当前设备环境的证据可以进入最终结论。

## Tested build identity

| 字段 | 当前值 |
|---|---|
| Story branch | `codex/e17-1-band9-hrs-revalidation` |
| Immutable Story SHA | `b7a48b980b54e34763212699c64ce387866ec064` |
| Merge commit | `17a305725a4241810ea4dbd26a29414c2be2582b` |
| Main ancestry | Story SHA 已是 `main` ancestor；E17-1 合并完成时的基线已确认 `main...origin/main = 0 0` |
| Preparation APK Git commit | `a55aa59fe4ee897f604938d78e087d9a1f203484` |
| APK file | `.local/smoke/e17-1-band9-hrs-revalidation/trainflow-e17-1-debug.apk` |
| APK SHA256 | `60abda376470a667ec5c94d16a24e996b2e3e7033df2cc7b4dc6d4132e8dbbc7` |
| Build variant | `debug` |
| Evidence log tag | `TrainFlowE17Hrs` |
| Launcher path | `TrainFlow Debug` -> `E17-1 Band 9 HRS Revalidation` |

最终结论必须记录用户实际安装 APK 对应的完整 Git commit 与 SHA256。若测试期间 APK 变化，必须重新计算 SHA256，并明确哪一轮证据属于哪个 APK；不得把不同构建的证据混成同一条链路。

## Device/environment information

| 字段 | 当前值 |
|---|---|
| 手机型号 | `PLU110`（debug Activity 运行时日志） |
| Android 版本 / SDK | Android 16 / SDK 36（debug Activity 运行时日志） |
| HUAWEI Band 9 固件 | 未确认 |
| Huawei Health 版本 | 未确认 |
| Band 9 佩戴 / 测量条件 | 用户按两轮广播测试清单操作；其他细节未确认 |
| AVD | `TrainFlow_Pixel_API_36`；两个 launcher 均可查询，`TrainFlow Debug` 可进入 E17-1 Activity；仅用于 Activity / 权限入口 / 日志 UI smoke |

无法从证据可靠确认的版本或环境字段必须保持“未确认”，不得推测。

## Test matrix

| ID | 条件 / 操作 | 必须采集的当前证据 | Pass 条件 | Failed / inconclusive 条件 | 状态 |
|---|---|---|---|---|---|
| M1 | 心率广播关闭，Huawei Health 已连接；在 E17-1 工具扫描 12 秒 | Huawei Health 状态、Band label / identifier 是否出现、扫描起止日志 | 实际观察被记录；不预设必须发现或不得发现 | 缺截图 / 日志，或无法确认广播确已关闭 | passed：用户确认广播关闭且 Huawei Health 已连接；`22:34:30.660` 至 `22:34:42.665` 无 `SCAN_SOURCE` |
| M2 | 开启 Band 9 心率广播 | Band 提示、Huawei Health 状态变化 | 互斥 / 不互斥事实被当前证据记录 | 仅沿用旧 E16 描述 | passed：用户现场观察为开启广播后 Huawei Health 断开 |
| M3 | 广播开启后的第一次标准 HRS 扫描 | `SCAN_SOURCE` 的 label、identifier、RSSI、services | Band source 被发现，advertised services 含 `0x180D` | Band 不出现为 failed；环境状态不清为 inconclusive | passed：`HUAWEI Band HR-OD7` / `D8:F0:42:01:90:D7` / `[0x180D]` |
| M4 | 选择 M3 的同一 source 并连接 | `CONNECT_REQUEST`、`GATT_CONNECTION`、`SERVICE_DISCOVERY_RESULT` | 同一 source 连接成功且 service list 含 `0x180D` | status 非成功、断开、超时或 source 对不上 | passed：`status=0 state=2 success=true`；service list 含 `0x180D` |
| M5 | 检查 Heart Rate Measurement | `HRS_MEASUREMENT` properties | 同一连接发现 `0x2A37`，且 notify 或 indicate 至少一个为真 | characteristic 缺失或两种属性都无 | passed：`0x2A37 found=true properties=0x10 modes=[notify]` |
| M6 | 订阅 `0x2A37` | `CCCD_DISCOVERY`、`LOCAL_NOTIFICATION`、`CCCD_WRITE_START`、`CCCD_WRITE` | `0x2902` 存在，本地通知开启成功，write start 与 callback 均成功 | descriptor 缺失、任一步失败或 callback 未返回 | passed：两轮均发现 `0x2902`，写入 `01 00`，callback `status=0 success=true` |
| M7 | 维持同一连接短时间 | 多条连续 `NOTIFY` | notify enabled 后，同一 source identifier 收到至少 3 条连续通知 | 只有 0–1 条、source 变化、断流或时间线不完整 | passed：两轮均从同一 source 连续收到远多于 3 条通知 |
| M8 | 对照 raw payload 与 parser | 同一 `NOTIFY` 行中的 raw、flags、format、parsed bpm | 至少一个代表性 raw payload 可由当前 parser 得到该行 bpm；最终文档记录至少 2 条代表样例 | 只有 bpm 无 raw、只有 raw 无 parser 输出、解析为 null | passed：例如 `06 57 -> 87`、`06 5D -> 93`、`06 55 -> 85` |
| M9 | 点击 Stop / Disconnect | `CLEANUP`、后续 disconnect 观察 | scan 停止、GATT disconnect requested / closed 被记录；无继续 notify | cleanup 不完整或停止后仍持续收到同一连接通知 | passed with observation：第二轮 `CLEANUP reason=user_stop ... requested=true ... closed=true`；第一轮先发生 `status=19` 断开 |
| M10 | 关闭广播并重新开启，执行第二轮扫描 / 链路 | 第二次 label、identifier、`0x180D`、GATT / CCCD / notify | 第二周期记录 label + identifier，且仍可完成标准 HRS 链路 | 只有一次扫描周期，或第二轮关键链路缺失 | passed：第二轮同一观察字段并再次完成完整标准 HRS 链路 |
| M11 | 最终关闭广播并恢复 Huawei Health | 广播关闭、工具 cleanup、Huawei Health 状态 | stop / disconnect / 广播关闭与 Huawei Health 恢复结果均有记录 | 恢复未测试或证据不足 | passed：用户执行最终关闭广播并观察 Huawei Health 自动重新连接 |

### Overall pass rule

只有 M1–M11 的关键事实均有当前 Band 9 真机证据，且 APK / 日志 / 截图 / 设备输出只在 `.local/` 中，E17-1 才能写为 `passed`。任一关键链路缺失时，结论只能是 `evidence inconclusive` 或 `failed`，并保持 E17-2 locked。

## Raw evidence summary

以下时间来自 2026-07-15 用户回传的 E17-1 debug Activity 截图内日志。截图按日志时间而非 Downloads 文件时间排序。`22:30–22:38` 为初始验收，`23:07–23:12` 为补充复验；补充复验同时提供了 Huawei Health 连接 / 断开页面截图。

| 时间 / 轮次 | 证据摘录 | 解释 |
|---|---|---|
| `22:30:50.748` | `E17_1_HRS_REVALIDATION_READY model="PLU110" android=16 sdk=36` | 当前真机运行环境 |
| 广播关闭 / `22:34:30.660–22:34:42.665` | `SCAN_STARTED filter_service=0x180D window_ms=12000`，随后 `SCAN_STOPPED reason=scan_window_ended`；窗口内无 `SCAN_SOURCE` | 在用户确认广播关闭的条件下未发现标准 HRS source |
| 第一轮 / `22:35:38.007` 起 | `SCAN_SOURCE label="HUAWEI Band HR-OD7" identifier="D8:F0:42:01:90:D7" ... services=[0x180D]` | 广播开启后 source 被过滤扫描发现；字段仅关联本次观察 |
| 第一轮 / `22:36:21.398–22:36:22.444` | GATT `status=0 state=2 success=true`；service discovery 含 `0x180D`；`0x2A37 properties=0x10 modes=[notify]`；`0x2902 found=true`；写入 `01 00`，callback `status=0 success=true` | 同一 source 完成标准 HRS 发现与 notify 订阅 |
| 第一轮 / `22:36:22.928` 起 | 同一 source 连续 `NOTIFY`，包括 `raw="06 57" parsed_bpm=87`、`raw="06 5D" parsed_bpm=93`、`raw="06 5A" parsed_bpm=90` | 当前 parser 在真实 payload 上输出 uint8 bpm |
| 第一轮 / `22:36:58.547` | `GATT_CONNECTION ... status=19 state=0 success=false` | 通知持续约 36 秒后出现非成功断开；截图未显示该轮 `CLEANUP reason=user_stop`，不得写成主动 Stop 成功 |
| 第二轮 / `22:37:35–22:37:43` | 再次连续出现同一 label、identifier 与 `services=[0x180D]` | 关闭并重新开启广播后的第二个观察周期；本轮字段与第一轮相同 |
| 第二轮 / `22:37:42.304` | `VISIBLE_LOG_CLEARED active_connection_unchanged=false` | 用户确认在第一轮已断开后清理过可见日志；只清空页面日志，不代表仍有 active connection，也不替代第二轮链路证据 |
| 第二轮 / `22:37:50.130–22:37:51.299` | 再次 GATT 成功；发现 `0x180D`、`0x2A37 properties=0x10 modes=[notify]`、`0x2902`；CCCD callback 成功 | 第二周期再次完成标准 HRS 链路 |
| 第二轮 / `22:37:51.459` 起 | 同一 source 连续 `NOTIFY`，包括 `06 59 -> 89`、`06 5A -> 90`、`06 58 -> 88`、`06 55 -> 85` | 第二轮连续真实 payload 与 parser 对照 |
| 第二轮 / `22:38:28.199` | `CLEANUP reason=user_stop ... gatt_disconnect_requested=true gatt_closed=true` | 明确的用户 Stop / disconnect / close 证据；之后截图中无继续 notify |
| 补充广播关闭基线 / `23:07:49.791–23:08:01.813`、`23:08:46.157–23:08:58.179` | 两个 12 秒 `0x180D` 过滤扫描均以 window ended 结束，窗口内无 `SCAN_SOURCE` | 再次支持广播关闭时 HRS source 不可发现 |
| 补充第一周期 / `23:09` | Huawei Health 的 Band 9 卡片显示红色 `重新连接` | 广播开启后的 Huawei Health 断开截图证据 |
| 补充第一周期 / `23:10:00.708–23:10:43.131` | 扫描得到同一 label / identifier / `[0x180D]`；GATT、`0x180D`、notify 型 `0x2A37`、`0x2902` 与 CCCD callback 再次成功 | 第三个广播观察周期再次完成标准 HRS 链路 |
| 补充第一周期 / `23:10:43.314–23:11:02.833` | 连续通知包含 `06 55 -> 85`、`06 54 -> 84`、`06 56 -> 86`；随后 `status=19 state=0 success=false` | 再次观察到约 20 秒后由 status 19 结束，不是主动 Stop |
| 周期间关闭广播 / `23:11` | Huawei Health 卡片显示 Band 电量 `89%`，不再显示 `重新连接` | 广播关闭后 Huawei Health 恢复连接的截图证据 |
| 补充第二周期 / `23:11` | Huawei Health 再次显示红色 `重新连接` | 再次开启广播后 Huawei Health 再次断开的截图证据 |
| 补充第二周期 / `23:12:17–23:12:27.098` | 扫描再次得到同一 label / identifier / `[0x180D]`；GATT、HRS、CCCD callback 再次成功 | 第四个广播观察周期完成标准 HRS 链路 |
| 补充第二周期 / `23:12:27.235` 起 | 连续 `06 58 -> 88` 通知 | 同一 source 连续通知；截图未记录该周期的主动 cleanup |
| 补充第二周期结束 | 用户报告 notify 日志自动向下滚动，无法稳定点击顶部 `Stop / Disconnect`，随后直接关闭 Band 心率广播并确认 Huawei Health 最终重新连接 | 这是 debug-only 工具可用性限制；不得写成本周期 `user_stop` 成功。整体 cleanup acceptance 仍来自 `22:38:28.199` 的独立真实记录 |

## Protocol observations

| 项目 | 当前观察 |
|---|---|
| Band source scan | 初始两周期与补充两周期均发现 `HUAWEI Band HR-OD7` / `D8:F0:42:01:90:D7` |
| `0x180D` advertised | 四个广播开启周期均有 `SCAN_SOURCE services=[0x180D]` |
| GATT connection | 四个周期均至少一次 `status=0 state=2 success=true` |
| `0x180D` discovered | 四个周期均 `found=true`，service discovery list 亦含 `0x180D` |
| `0x2A37` discovered | 四个周期均 `found=true` |
| notify / indicate properties | `properties=0x10 modes=[notify]`；未观察到 indicate |
| `0x2902` discovered | 四个周期均 `descriptor=0x2902 found=true` |
| CCCD write start / callback | 四个周期均写入 notify 值 `01 00`，start 成功且 callback `status=0 success=true` |
| Continuous notifications from same source | 四个周期均在订阅成功后从同一 source 收到多条连续通知 |
| Stop / disconnect cleanup | 初始第二周期主动 cleanup 完整；初始第一周期与补充第一周期以 `status=19` 结束；补充第二周期因自动滚屏未点到 Stop，不能计作 cleanup 成功 |

## Identifier observations

至少记录两个扫描 / 广播周期；label 与 identifier 只作为该次观察中的 source correlation 字段，不声明为永久身份。

| 周期 | 广播条件 | label | identifier | `0x180D` | 标准 HRS 链路 |
|---|---|---|---|---|---|
| 1 | 关闭后开启 | `HUAWEI Band HR-OD7` | `D8:F0:42:01:90:D7` | advertised / discovered | completed；末尾 `status=19` 断开 |
| 2 | 再次关闭后重新开启 | `HUAWEI Band HR-OD7` | `D8:F0:42:01:90:D7` | advertised / discovered | completed；`user_stop` cleanup |
| 3（补充） | 关闭后开启 | `HUAWEI Band HR-OD7` | `D8:F0:42:01:90:D7` | advertised / discovered | completed；末尾再次 `status=19` |
| 4（补充） | 再次关闭后重新开启 | `HUAWEI Band HR-OD7` | `D8:F0:42:01:90:D7` | advertised / discovered | completed；未点到 Stop，最终关闭广播 |

无论各周期的字段相同或不同，最终结论都不得依赖静态名称或地址。一次 label、一次 Bluetooth address、bonded-device label 或系统配对视图均不能证明稳定设备身份。

## Huawei Health mutual-exclusion observations

| 条件 | 当前观察 |
|---|---|
| 广播关闭、Huawei Health 已连接 | 用户现场确认；补充复验 `23:11` Huawei Health 卡片显示 Band 电量 `89%`，作为已恢复连接的页面证据 |
| 开启心率广播后的 Huawei Health 状态 | 用户现场观察为断开；补充复验两次显示红色 `重新连接`，提供页面截图证据 |
| E17 工具连接期间 Huawei Health 状态 | 广播开启期间 Huawei Health 显示 `重新连接`；E17 工具同时取得标准 HRS 链路 |
| Stop / disconnect 后状态 | 工具第二轮完成主动 cleanup；仅 Stop 后、广播仍开启时 Huawei Health 是否立即恢复未单独确认 |
| 关闭广播后 Huawei Health 恢复 | 周期间关闭广播后有 `89%` 已连接页面截图；补充第二周期最终关闭广播后的再次恢复由用户现场确认，未另附最终页面截图 |

这组证据支持“当前 Band 9 / 手机环境中，心率广播开启与 Huawei Health 常规连接互斥，关闭广播后 Huawei Health 可恢复连接”。它不证明所有固件、Huawei Health 版本或其他手机均有相同行为。

## Parser evidence

纯 Kotlin parser 测试只证明指定 payload 的解析逻辑，不证明 Android BLE wiring 或 Band 9 行为。当前 `HeartRateMeasurementParserTest` 覆盖 8-bit、16-bit、flags 与 malformed payload；E17 formatter 测试锁定同一日志行同时包含 source、raw bytes、flags、format 与 parsed bpm。

真机代表样例：

| source | raw payload | flags / format | parsed bpm | 证据状态 |
|---|---|---|---|---|
| `D8:F0:42:01:90:D7` | `06 57` | flags `0x06` / `uint8` | 87 | 第一轮 screenshot evidence |
| `D8:F0:42:01:90:D7` | `06 5D` | flags `0x06` / `uint8` | 93 | 第一轮 screenshot evidence |
| `D8:F0:42:01:90:D7` | `06 5A` | flags `0x06` / `uint8` | 90 | 第一、二轮 screenshot evidence |
| `D8:F0:42:01:90:D7` | `06 55` | flags `0x06` / `uint8` | 85 | 第二轮 screenshot evidence |
| `D8:F0:42:01:90:D7` | `06 54` | flags `0x06` / `uint8` | 84 | 补充第一周期 screenshot evidence |
| `D8:F0:42:01:90:D7` | `06 58` | flags `0x06` / `uint8` | 88 | 补充第二周期 screenshot evidence |

## Evidence-layer limitations

| 证据层 | 本 Story 可证明 | 明确不能证明 |
|---|---|---|
| 源码审计 | debug Activity 预期输出字段、production 边界未被修改 | 代码在真实 Android / Band 9 上已运行 |
| 纯 Kotlin tests | parser fixture 与日志格式纯逻辑 | BLE scan、GATT、射频、Band 9 |
| AVD | Activity 可启动、无立即崩溃、权限入口与日志 UI 可用 | BLE 外设发现、`0x180D` / `0x2A37`、CCCD、notify、Huawei Health 互斥 |
| Band 9 真机 | 当前 APK / 手机 / Band 环境中的 scan / GATT / CCCD / notify 与恢复 | 其他 Band、其他厂商、所有固件、永久身份、production architecture 正确性 |

AVD、fake、injection、源码搜索、旧 E16 证据和 parser unit tests 都不得写成当前 Band 9 evidence。

补充复验与独立 Review 保留两项 debug-only nice-to-have：持续 notify 会让页面日志自动滚动，用户无法稳定回到并点击顶部 `Stop / Disconnect`；debug 工具的 `currentGatt` callback / UI 共享状态未显式串行化。它们不否定初始第二周期取得的真实 cleanup evidence，不构成 blocker、must-fix 或 should-fix，也不得扩展为 production 重构任务。若未来另行处理，只能保持最小 debug-only 范围，且新 APK 不能反向冒充本次已测试 APK。

## Pass/fail/inconclusive conclusion

**最终结论：passed。** 用户使用与 preparation commit `a55aa59fe4ee897f604938d78e087d9a1f203484` 对应、SHA256 为 `60abda376470a667ec5c94d16a24e996b2e3e7033df2cc7b4dc6d4132e8dbbc7` 的 debug APK，在 `PLU110` / Android 16 上完成了初始两周期与补充两周期复验。证据覆盖标准 HRS 扫描、GATT、`0x180D`、notify 型 `0x2A37`、`0x2902` 写入成功、同一 source 连续通知、raw payload / parser bpm，以及初始第二周期主动 cleanup；补充截图直接显示 Huawei Health 在广播开启时为 `重新连接`、广播关闭后恢复为显示 Band 电量的已连接状态。

初始第一周期与补充第一周期都在连续通知后出现 `GATT status=19` 非成功断开，说明当前链路并非没有不稳定现象；E17-1 只确认当前环境中的设备与标准协议可行性，不把这些断开解释为 production 稳定性、重连能力或生命周期正确性。四个广播开启周期的 label 与 identifier 相同只证明这些观察中未变化，不构成永久设备身份。

该 `passed` 结论本身不直接解锁 E17-2。E17-1 已完成独立 Code Review、merge / push；Review 无 blocker、must-fix 或 should-fix，immutable Story SHA `b7a48b980b54e34763212699c64ce387866ec064` 已通过 merge commit `17a305725a4241810ea4dbd26a29414c2be2582b` 成为 `main` ancestor。E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。

## Non-goals

- 不形成或修改 production BLE provider 架构。
- 不修改 `app/src/main` provider、parser、权限、scanner、状态模型或 lifecycle wiring。
- 不实现自动重连、retry、backoff、watchdog、freshness 或后台连接。
- 不修改浮动胶囊视觉、互动、布局、geometry、动画或 HTML。
- 不进入 E17-2、E17-3、E17-4。
- 不写 `WorkoutSession`、Room、records、history、trends、analytics 或训练控制。
- 不把一次 label / address 声明为稳定设备身份。

## Verification commands

阶段 A / B 根据实际修改运行：

```powershell
. .\.local\env.ps1
.\gradlew.bat :app:testDebugUnitTest --tests "*HeartRate*" --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:lintDebug --no-daemon --console=plain "-Dkotlin.incremental=false"
.\gradlew.bat :app:check --no-daemon --console=plain
git diff --check
git diff --cached --check
```

AVD 只执行 debug Activity 启动、权限入口和日志 UI smoke；真实协议矩阵由用户在 Band 9 上执行。

## Local evidence paths

所有非 Git 证据只保存在：

```text
.local/smoke/e17-1-band9-hrs-revalidation/
```

预期本地文件：

- `trainflow-e17-1-debug.apk`
- `trainflow-e17-1-debug.apk.sha256`
- `build-identity.txt`
- `manual-test-checklist.md`
- `avd-ui.xml` / `avd-ui-summary.txt`
- `avd-activity.png`
- `avd-logcat.txt`
- 用户回传的真机截图 / 日志 / 设备信息（文件名按实际证据记录）

初始八张与补充十张原始真机截图均已复制（未移动）到上述目录，文件名保持用户回传原名。初始组覆盖 `22:34:30` 至 `22:38:28`；补充组覆盖 `23:07:49` 至 `23:12:29`，其中包括 Huawei Health 的断开、周期间恢复连接及再次断开页面。补充第二周期最终关闭广播后的恢复仍按用户现场观察分层记录。

`.local/`、APK、SHA 文件、截图、日志与设备输出不得被 Git 跟踪、暂存或提交。

## Cleanup steps

每轮扫描 / 连接后：

1. 点击 `Stop / Disconnect`，保留 `CLEANUP` 与 disconnect 相关日志。
2. 确认工具不再收到同一连接的 notify。
3. 最终关闭 Band 9 心率广播。
4. 观察 Huawei Health 是否恢复；只记录可见事实。
5. 必要时从系统设置强制停止 E17 debug APK，但不删除证据。
6. 将截图、logcat 与设备信息保存在 `.local/smoke/e17-1-band9-hrs-revalidation/`。

## Review and merge gate

- E17-1 已完成独立 Code Review、merge / push；最终状态为 `reviewed / merged`，设备/协议结论保持 `passed`。
- Review 无 blocker、must-fix 或 should-fix；两项 debug-only nice-to-have 不扩大为 production 重构任务。
- Immutable Story SHA `b7a48b980b54e34763212699c64ce387866ec064` 已通过 merge commit `17a305725a4241810ea4dbd26a29414c2be2582b` 成为 `main` ancestor；E17-1 合并完成时的基线已确认 `main...origin/main = 0 0`。
- E17-2 为 `planned / prerequisite-gated`，不得在本状态 docs-sync Review / merge gate 完成前启动。
- 当本 E17-1 状态 docs-sync 的 immutable SHA 尚未成为 `main` ancestor 时，只允许该 docs-sync 的独立 Review / merge；当该 docs-sync 通过独立 Review、merge / push，其 immutable SHA 成为 `main` ancestor，`main...origin/main = 0 0` 且当前权威文档一致时，门禁自动满足。
- 门禁满足后，主管理可直接从 Git 解析 docs-sync immutable SHA 并生成 E17-2 提示词；不需要也不得创建“closeout 的 closeout”。
- 本 Story 未形成新的产品或架构决策，因此不修改 D-079，也不新增 decision。
