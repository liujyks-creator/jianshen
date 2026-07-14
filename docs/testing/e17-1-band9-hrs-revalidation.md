# E17-1 HUAWEI Band 9 与标准 BLE HRS 重新复验

**Story 状态：** in progress / pending real-device acceptance
**当前结论：** evidence inconclusive（阶段 A 尚未取得本 Story 的 Band 9 真机证据）
**日期：** 2026-07-13
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
| 手机型号 | 待用户真机回传 |
| Android 版本 / SDK | 待用户真机回传 |
| HUAWEI Band 9 固件 | 未确认 |
| Huawei Health 版本 | 未确认 |
| Band 9 佩戴 / 测量条件 | 待用户真机回传 |
| AVD | `TrainFlow_Pixel_API_36`；两个 launcher 均可查询，`TrainFlow Debug` 可进入 E17-1 Activity；仅用于 Activity / 权限入口 / 日志 UI smoke |

无法从证据可靠确认的版本或环境字段必须保持“未确认”，不得推测。

## Test matrix

| ID | 条件 / 操作 | 必须采集的当前证据 | Pass 条件 | Failed / inconclusive 条件 | 状态 |
|---|---|---|---|---|---|
| M1 | 心率广播关闭，Huawei Health 已连接；在 E17-1 工具扫描 12 秒 | Huawei Health 状态、Band label / identifier 是否出现、扫描起止日志 | 实际观察被记录；不预设必须发现或不得发现 | 缺截图 / 日志，或无法确认广播确已关闭 | pending real device |
| M2 | 开启 Band 9 心率广播 | Band 提示、Huawei Health 状态变化 | 互斥 / 不互斥事实被当前证据记录 | 仅沿用旧 E16 描述 | pending real device |
| M3 | 广播开启后的第一次标准 HRS 扫描 | `SCAN_SOURCE` 的 label、identifier、RSSI、services | Band source 被发现，advertised services 含 `0x180D` | Band 不出现为 failed；环境状态不清为 inconclusive | pending real device |
| M4 | 选择 M3 的同一 source 并连接 | `CONNECT_REQUEST`、`GATT_CONNECTION`、`SERVICE_DISCOVERY_RESULT` | 同一 source 连接成功且 service list 含 `0x180D` | status 非成功、断开、超时或 source 对不上 | pending real device |
| M5 | 检查 Heart Rate Measurement | `HRS_MEASUREMENT` properties | 同一连接发现 `0x2A37`，且 notify 或 indicate 至少一个为真 | characteristic 缺失或两种属性都无 | pending real device |
| M6 | 订阅 `0x2A37` | `CCCD_DISCOVERY`、`LOCAL_NOTIFICATION`、`CCCD_WRITE_START`、`CCCD_WRITE` | `0x2902` 存在，本地通知开启成功，write start 与 callback 均成功 | descriptor 缺失、任一步失败或 callback 未返回 | pending real device |
| M7 | 维持同一连接短时间 | 多条连续 `NOTIFY` | notify enabled 后，同一 source identifier 收到至少 3 条连续通知 | 只有 0–1 条、source 变化、断流或时间线不完整 | pending real device |
| M8 | 对照 raw payload 与 parser | 同一 `NOTIFY` 行中的 raw、flags、format、parsed bpm | 至少一个代表性 raw payload 可由当前 parser 得到该行 bpm；最终文档记录至少 2 条代表样例 | 只有 bpm 无 raw、只有 raw 无 parser 输出、解析为 null | pending real device |
| M9 | 点击 Stop / Disconnect | `CLEANUP`、后续 disconnect 观察 | scan 停止、GATT disconnect requested / closed 被记录；无继续 notify | cleanup 不完整或停止后仍持续收到同一连接通知 | pending real device |
| M10 | 关闭广播并重新开启，执行第二轮扫描 / 链路 | 第二次 label、identifier、`0x180D`、GATT / CCCD / notify | 第二周期记录 label + identifier，且仍可完成标准 HRS 链路 | 只有一次扫描周期，或第二轮关键链路缺失 | pending real device |
| M11 | 最终关闭广播并恢复 Huawei Health | 广播关闭、工具 cleanup、Huawei Health 状态 | stop / disconnect / 广播关闭与 Huawei Health 恢复结果均有记录 | 恢复未测试或证据不足 | pending real device |

### Overall pass rule

只有 M1–M11 的关键事实均有当前 Band 9 真机证据，且 APK / 日志 / 截图 / 设备输出只在 `.local/` 中，E17-1 才能写为 `passed`。任一关键链路缺失时，结论只能是 `evidence inconclusive` 或 `failed`，并保持 E17-2 locked。

## Raw evidence summary

阶段 A 尚无当前 Band 9 raw evidence。阶段 B 只按时间顺序摘录用户回传证据实际显示的行，不复制旧 E16 结果。

| 时间 / 轮次 | 证据摘录 | 解释 |
|---|---|---|
| pending | 未取得 | 不形成设备或协议结论 |

## Protocol observations

| 项目 | 当前观察 |
|---|---|
| Band source scan | 未确认 |
| `0x180D` advertised | 未确认 |
| GATT connection | 未确认 |
| `0x180D` discovered | 未确认 |
| `0x2A37` discovered | 未确认 |
| notify / indicate properties | 未确认 |
| `0x2902` discovered | 未确认 |
| CCCD write start / callback | 未确认 |
| Continuous notifications from same source | 未确认 |
| Stop / disconnect cleanup | 未确认 |

## Identifier observations

至少记录两个扫描 / 广播周期；label 与 identifier 只作为该次观察中的 source correlation 字段，不声明为永久身份。

| 周期 | 广播条件 | label | identifier | `0x180D` | 标准 HRS 链路 |
|---|---|---|---|---|---|
| 1 | 待测试 | 未确认 | 未确认 | 未确认 | 未确认 |
| 2 | 待测试 | 未确认 | 未确认 | 未确认 | 未确认 |

无论两个周期的字段相同或不同，最终结论都不得依赖静态名称或地址。一次 label、一次 Bluetooth address、bonded-device label 或系统配对视图均不能证明稳定设备身份。

## Huawei Health mutual-exclusion observations

| 条件 | 当前观察 |
|---|---|
| 广播关闭、Huawei Health 已连接 | 未确认 |
| 开启心率广播后的 Huawei Health 状态 | 未确认 |
| E17 工具连接期间 Huawei Health 状态 | 未确认 |
| Stop / disconnect 后状态 | 未确认 |
| 关闭广播后 Huawei Health 恢复 | 未确认 |

## Parser evidence

纯 Kotlin parser 测试只证明指定 payload 的解析逻辑，不证明 Android BLE wiring 或 Band 9 行为。当前 `HeartRateMeasurementParserTest` 覆盖 8-bit、16-bit、flags 与 malformed payload；E17 formatter 测试锁定同一日志行同时包含 source、raw bytes、flags、format 与 parsed bpm。

真机代表样例待阶段 B 填写：

| source | raw payload | flags / format | parsed bpm | 证据状态 |
|---|---|---|---|---|
| 未确认 | 未确认 | 未确认 | 未确认 | pending real device |

## Evidence-layer limitations

| 证据层 | 本 Story 可证明 | 明确不能证明 |
|---|---|---|
| 源码审计 | debug Activity 预期输出字段、production 边界未被修改 | 代码在真实 Android / Band 9 上已运行 |
| 纯 Kotlin tests | parser fixture 与日志格式纯逻辑 | BLE scan、GATT、射频、Band 9 |
| AVD | Activity 可启动、无立即崩溃、权限入口与日志 UI 可用 | BLE 外设发现、`0x180D` / `0x2A37`、CCCD、notify、Huawei Health 互斥 |
| Band 9 真机 | 当前 APK / 手机 / Band 环境中的 scan / GATT / CCCD / notify 与恢复 | 其他 Band、其他厂商、所有固件、永久身份、production architecture 正确性 |

AVD、fake、injection、源码搜索、旧 E16 证据和 parser unit tests 都不得写成当前 Band 9 evidence。

## Pass/fail/inconclusive conclusion

**当前结论：evidence inconclusive。** 阶段 A 只准备了 E17-1 debug-only 证据工具、测试矩阵和构建流程；尚未由用户在真实 Band 9 上执行 M1–M11，因此不能写 `passed`，也不能解锁 E17-2。

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

- 当前 Story 未 reviewed、未 merged。
- 阶段 A 完成后必须暂停，等待用户真实 Band 9 证据。
- 真机矩阵、自动验证与范围检查全部完成后，Story 才可写为 `implemented / needs review` 并完成最终提交 / push。
- E17-2 保持 locked。只有 E17-1 通过独立 Code Review、merge / push、immutable Story SHA ancestry 与 `main...origin/main = 0 0` 同步检查后，主管理才可生成 E17-2 提示词。
- 不创建递归 closeout，不在本 Story 中宣称 reviewed / merged，不自行合入 `main`。
- 本 Story 未形成新的产品或架构决策，因此不修改 D-079，也不新增 decision。
