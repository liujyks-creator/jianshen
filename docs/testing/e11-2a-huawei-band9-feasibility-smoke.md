# TrainFlow E11.2a HUAWEI Band 9 非华为 Android 可行性 Smoke

**状态:** Completed for the original condition; E16 broadcast-on retest captured positive BLE HRS evidence
**日期:** 2026-06-21
**范围:** HUAWEI Band 9 + 非华为 Android + 已安装华为运动健康

本文记录第三方实时心率通道可行性。E11.2a 期间曾提供 debug-only BLE HRS smoke APK；E11.3 后续根据用户反馈撤销首版心率显示、录入和统计，并移除 debug smoke 入口。当前不做生产设备接入，不持久化心率，不绘制平均心率趋势，不把执行页 `HeartRateState` 当历史事实。

2026-07-05 更新：用户补充说明，前次测试时 HUAWEI Band 9 未开启“心率广播”。设备提示开启心率广播后会作为第三方蓝牙设备连接，并会断开华为运动健康。这个新信息将 E11.2a 的结论收窄为“广播未开启 / Huawei Health 连接占用条件下未发现标准 BLE HRS”。开启心率广播后的可行性已在 `docs/testing/e16-heart-rate-broadcast-feasibility-retest.md` 重新测试并取得正向 BLE HRS 证据；该 retest 已 reviewed / merged 到 main，merge commit `bbd4296`，但不恢复 MVP 心率 UI，也不自动启动生产 BLE adapter。

## 1. 当前真实条件

- 用户有 HUAWEI Band 9。
- 手机不是华为手机。
- 手机已安装华为运动健康。
- 华为运动健康可以读取手环数据。
- 这只能证明华为运动健康在当前配对链路中能读数据，不能证明 TrainFlow 第三方 App 可以实时读取心率。

## 2. 本轮验证

| 项目 | 结果 | 说明 |
|---|---|---|
| 本地 Android / ADB 入口 | Not available | 当前 Codex PowerShell 会话中 `adb` 不在 PATH，无法直接从本机对用户手机执行 BLE/GATT smoke。 |
| Debug-only APK | Removed after smoke | E11.2a 曾在 `app/src/debug` 新增 `Band9BleSmokeActivity` 和 debug Manifest 权限用于用户手机 smoke；E11.3 收口后已移除该入口，避免当前 APK 继续暴露已降级的心率调研能力。 |
| 用户首轮截图 | Inconclusive | 用户确认 Band 9 已连接手机且 Huawei Health 正开启；日志显示权限已授予、BLE scan 已启动，但 Devices 列表为空。该结果只能说明当时未收到可展示 BLE 广播，不能判定 Band 9 没有 HRS。 |
| Debug APK second pass | Implemented | Smoke 工具升级为低延迟扫描，并新增 `Bonded` 按钮读取系统已配对设备；若华为运动健康占用连接导致手环不广播，可先从 bonded list 尝试 GATT discover。 |
| 用户 second-pass 截图 | Strong negative for system-level exposure while Huawei Health connected | 用户确认 Band 9 已连接手机且 Huawei Health 正开启；`Bonded` 列表只显示 MERACH 和 Galaxy Buds Pro 等非目标设备，未显示 HUAWEI / Band 9；扫描日志仍未反馈到任何 Band 9 设备卡片。该结果强化了“华为运动健康可读不等于第三方 App 可通过系统 bonded/GATT 路径读取”的判断。 |
| Band 9 官方规格 | Partial evidence | 官方规格显示 Band 9 有 optical heart rate sensor，连接能力包含 BT 5.0 / BLE；规格未声明暴露标准 BLE Heart Rate Service。 |
| 华为运动健康读数 | Confirmed by condition | 华为文档说明华为运动健康 / 手环可显示实时心率和运动中心率；这仍是华为 App / 设备生态能力，不等于第三方实时 GATT 通道。 |
| 标准 BLE HRS 判定标准 | Known | 若设备实现标准 Heart Rate Service，应能发现 `0x180D`，且 `Heart Rate Measurement` characteristic 支持 notify。 |
| Band 9 `0x180D` | Not found in original condition | 用户安装 E11.2a 新版 smoke APK 后仍无法发现华为设备；该结论只覆盖广播未开启 / Huawei Health 连接占用条件。E16 广播开启 retest 后已有 `0x180D` 正向证据。 |
| Band 9 `0x2A37 notify` | Not found in original condition | E11.2a 原条件没有 Heart Rate Measurement characteristic、notify enabled 或 bpm notify 证据。E16 广播开启 retest 后已有 `0x2A37 notify`、CCCD 写入成功和 bpm notify 证据。 |

## 2.1 Follow-up: heart-rate broadcast caveat

2026-07-05 用户反馈：此前未开启 Band 9 心率广播；Band 9 UI 提示开启心率广播会连接第三方蓝牙设备，并断开运动健康 App。

因此 E11.2a 的 negative result 只覆盖当时条件，不覆盖“广播开启后作为标准第三方心率设备广播”的条件。后续判断以 E16 retest 为准：

- E16 retest 已在广播开启条件下取得正向 BLE HRS 证据：`0x180D`、`0x2A37 notify`、CCCD 写入成功和持续 bpm notify。
- 该证据只允许后续另拆 `E16-1 BLE HRS adapter spike`，不能从当前 story 直接接入生产心率。
- 不论 retest 结果如何，当前 MVP 不恢复心率卡片、手动输入、未获取心率占位或平均心率趋势。

## 2.2 Debug APK 使用步骤（历史记录）

以下步骤仅记录 E11.2a 当时如何完成 smoke。E11.3 后 debug smoke 入口已移除，当前 APK 不再提供该入口。

1. 安装 `app/build/outputs/apk/debug/app-debug.apk`。
2. 桌面上会出现正常 TrainFlow 入口和 `Band 9 HRS Smoke` 入口；打开 `Band 9 HRS Smoke`。
3. 点击 `Grant`，授予 Nearby devices / Bluetooth 相关权限。
4. 点击 `Scan`，让 Band 9 靠近手机并保持唤醒；新版工具使用 low-latency scan，10 秒无结果会写入日志。
5. 如果 Devices 为空，点击 `Bonded`。在 Huawei Health 已连接手环时，这一步比广播扫描更有价值。
6. 在 Devices 列表中选择疑似 Band 9 / HUAWEI / Band 设备，点击 `Connect / discover`。
7. 将屏幕日志反馈回来，重点看：
   - 是否有 `service 0x180D`。
   - 是否有 `characteristic 0x2A37 props=...notify...`。
   - 是否出现 `RESULT: 0x2A37 notify enabled`。
   - 是否出现 `RESULT: heart-rate notify bpm=...`。
   - 如果日志显示 `HRS 0x180D not found`，则当前连接不能作为执行页实时心率来源。

## 3. 外部资料判断

- Huawei Band 9 官方规格确认：设备包含 optical heart rate sensor，连接能力为 `2.4 GHz, BT 5.0, and BLE`。这支持继续做 BLE smoke，但不等价于公开标准 HRS。
  来源: https://consumer.huawei.com/en/wearables/band9/specs/
- HUAWEI 支持文档确认：华为手表 / 手环可通过 Huawei Health 配置连续心率，运动中也可在可穿戴设备和 Huawei Health 结果中查看实时 / 平均 / 最大心率。该资料没有说明第三方 App 可直接实时读取 Band 9 心率。
  来源: https://consumer.huawei.com/en/support/content/en-us00737153/
- Bluetooth SIG HRS 规范确认：Heart Rate Service 用于暴露心率数据；Heart Rate Measurement 是必需 characteristic，properties 为 Notify；配置 notification 后有测量值时应 notify。
  来源: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/HRS_v1.0/out/en/index-en.html
- Huawei Health Service Kit / Health Kit 文档入口显示存在 atomic sampling data 和 real-time heart rate 相关能力，但本轮未验证开发者权限、地区、账号、Band 9 设备支持、HMS Core、非华为 Android 兼容性和训练中实时授权行为。
  来源: https://developer.huawei.com/consumer/en/doc/hmscore-guides/health-sampling-data-0000001177416909
- Health Connect 官方文档以读取 datastore records 为核心，可读取 `HeartRateRecord` 和历史数据，并有后台 / 历史读取权限边界；它不应作为 E11.2a 训练执行页实时心率首选。
  来源: https://developer.android.com/health-and-fitness/health-connect/read-data
- Apple / iOS 路线仍不同：HealthKit workout session / live builder 是未来 iOS + Apple Watch 或外部 HRS 设备路线，不进入当前 Android-first E11.2a 实现。
  来源: https://developer.apple.com/videos/play/wwdc2025/322/

## 4. 路线结论

| 路线 | 判断 | 后续动作 |
|---|---|---|
| Android BLE HRS adapter spike | Do not start from E11.2a | E11.2a 原条件未发现 HUAWEI / Band 9，也没有 `0x180D`、`0x2A37 notify` 或 bpm notify 证据；E16 广播开启 retest 后已有正向 BLE HRS 证据，但只能另拆 `E16-1 BLE HRS adapter spike`，不能作为当前 story 的生产接入。 |
| Huawei SDK feasibility | Optional next device research | 若仍要继续 Band 9 设备方向，只能另开 feasibility 验证 HMS Core、Huawei 账号授权、地区、审核、Band 9 支持和非华为 Android 兼容性。 |
| Health Connect 历史同步 | Later / historical only | 可作为训练后历史摘要或趋势候选，不作为执行页实时来源；进入前必须另设计 source 标注、样本边界和权限文案。 |
| Apple HealthKit | Future iOS route | 保留 iOS app + watchOS companion + HealthKit / HKWorkoutSession / HKLiveWorkoutBuilder 方向，当前不开发。 |
| 暂不接设备 | Current recommendation | 当前 MVP 不接 Band 9 实时设备，且 E11.3 已撤销首版心率显示、录入和统计。 |

## 5. 解锁 BLE adapter spike 所需的最小证据

后续若要把路线切到 BLE HRS adapter spike，需要在同一台非华为 Android 手机上记录；E16 retest 已在广播开启条件下满足这些最小证据，但只解锁未来单独的 `E16-1 BLE HRS adapter spike`：

1. BLE scanner 可以发现 Band 9 或其可连接 BLE peripheral。
2. GATT service 列表包含 Heart Rate Service `0x180D`。
3. `0x180D` 下存在 Heart Rate Measurement characteristic `0x2A37`。
4. `0x2A37` properties 包含 notify，且 Client Characteristic Configuration Descriptor 可写入开启通知。
5. 开启 notify 后，在佩戴手环并开始测量 / 运动时可以收到 bpm 更新。
6. 记录 Huawei Health 是否正在连接、是否需要断开 / 关闭后台、是否影响 GATT 订阅稳定性。

如果任何一项失败，不能把 Band 9 当作 TrainFlow 执行页实时设备来源。

## 5.1 Second-pass 观察结论

2026-06-20 用户回传第二张截图后，当前证据为：

- Band 9 已连接手机，Huawei Health 正开启。
- Debug APK 权限正常，Android 36 使用 `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`。
- Bluetooth adapter enabled，且 multiple advertisement supported。
- `Bonded` 可列出系统已配对设备，但列表里只有 MERACH / Galaxy Buds Pro 等非目标设备，没有 HUAWEI / Band 9。
- 截图可见范围内没有 Band 9 扫描设备卡片，也没有 `service 0x180D`、`characteristic 0x2A37` 或 bpm notify 证据。

当时判断：

- 在 Huawei Health 已连接状态下，Band 9 没有作为系统 bonded BLE device 暴露给 TrainFlow smoke 工具。
- E11.2a 当时仍不能启动 BLE HRS adapter spike。
- 该 second-pass 之后，用户在 2026-06-21 继续反馈新版 smoke APK 仍无法发现华为设备；结合本节证据，BLE HRS 路线已降级，不进入 BLE adapter spike 或生产设备接入。


## 5.2 User Follow-up: Huawei Device Still Not Discoverable

2026-06-21 用户反馈：安装新版 smoke APK 后，仍无法发现华为设备。

当时判断：

- 在当前非华为 Android + HUAWEI Band 9 + Huawei Health 条件下，debug smoke 仍没有发现 HUAWEI / Band 设备。
- 没有 `0x180D` Heart Rate Service、`0x2A37` Heart Rate Measurement notify 或 bpm notify 证据。
- Android BLE HRS adapter spike 不应从 E11.2a 启动。
- E11.2a 已完成：在广播未开启 / Huawei Health 连接占用的原条件下，不建议进入 BLE HRS adapter spike 或生产设备接入。
- 后续只剩两条合理方向：单独评估 Huawei SDK feasibility，或 MVP 暂不接 Band 9 实时设备。
## 6. 明确未做

- 未修改生产 Gradle、main Android Manifest、资源或 Room schema。
- E11.2a 曾新增 debug-only smoke Activity 和 debug Manifest 蓝牙权限；E11.3 后已移除该入口。E16 retest 后新的 smoke 入口也仅存在于 `app/src/debug` 的独立 launcher Activity，release / main manifest 不新增 BLE、BODY_SENSORS、Health Connect 或 Huawei SDK 权限。
- 已运行 `app:assembleDebug` 和 `app:lintDebug`。
- 未持久化任何心率数据。
- 未绘制平均心率趋势。
- E11.3 已撤销首版心率显示、录入和统计；E11.2a 本身不倒灌手动输入逻辑。
- 未做医疗判断、危险告警、训练中断依据或恢复 / 康复结论。
- 未触碰 `.local/verification`。
