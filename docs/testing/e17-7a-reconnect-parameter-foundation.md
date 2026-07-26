# E17-7a Heart-rate Reconnect And Parameter Foundation

**交付时分支状态：** `implemented / needs review`（仅为分支交付快照；合并事实以 immutable SHA ancestry 为准）

**日期：** 2026-07-26

## 1. 目标与边界

本 Story 在尚未接入 production composition root 的 E17-6 `HeartRateRuntimeOwner` 上补齐自动恢复基础、持久手动抑制、个人参数和纯 presentation 计算。

本 Story 不接入 `TrainFlowApplication`、Activity、Compose settings 或实际胶囊调用，不退休旧 provider/scanner，不增加 Service、FGS、notification、Room、训练记录、通用 scheduler、watchdog、backoff controller、BLE wrapper、platform adapter、interface 或第三方依赖。新 owner 在 production/debug 的实例化仍为 0；当前 App 仍运行旧 runtime。E17-7b 才负责唯一 Application owner、设置页明确断开/重新连接/清除目标/关闭功能、胶囊接线、旧 runtime 原子退休以及 AVD/Band 9 basic gate。

## 2. 自动恢复合同

`HeartRateRecoveryEligibilityInput` 固定以下全部条件：

- 心率功能已 opt-in。
- 存在非空 saved exact target identifier。
- BLE permission 当前合法。
- Bluetooth 当前开启。
- 不存在持久 manual suppression。
- App 明确 visible，或 active/paused training 的 `connectedDevice` FGS 已经合法建立并处于 active；仅“理论上可启动”不得传 true。

任一条件缺失时输出 typed stop reason，并在 owner main looper 上取消 pending recovery、scan、attempt 和 GATT。自动恢复只匹配 exact identifier；名称和其他 HRS candidate 不得替代目标。

eligible 时，owner 使用已有 main `Handler` 排队 bounded scan window。单个 window 有限，window 之间有间隔；miss、scan failure、unexpected disconnect 和 out-of-range 不消耗永久次数，eligibility 持续成立时保持长期 armed。manual scan/stop scan 不会永久消耗自动恢复义务。

没有增加独立 scheduler/controller。所有 recovery runnable、scan generation、attempt identity、raw GATT callback、freshness 与 cleanup 仍由唯一 concrete owner 串行管理。

## 3. 手动断开与持久化

DataStore 新增：

- `heartRateManualSuppressed`
- `heartRateAgeYears`
- `heartRatePersonalMaxBpm`
- `heartRateAlertThresholdBpm`

手动断开在 owner 内立即抑制当前实例的 recovery；E17-7b 必须先持久化 suppression，再提交 owner disconnect。suppression 保留 opt-in、saved target 和个人参数；重新选择设备会在同一 DataStore edit 中清除 suppression。E17-7b 的明确“重新连接”动作也必须清除 suppression，普通 lifecycle、permission/Bluetooth 恢复或进程重建不得清除。

文件型 DataStore 重建测试确认 opt-in、exact target、display name、suppression、年龄 `101`、个人最大心率和提醒阈值均可恢复。纯动作语义测试使用 test-local in-memory DataStore，避免 Windows 文件型 DataStore 连续写入的临时重命名噪声；production 未增加测试专用写入 API或文件系统 workaround。

## 4. 个人参数与区间

- 年龄可选，合法范围 `1..130`；`101` 有效且不 clamp。
- 个人最大心率可选，当前输入防错范围 `30..260`。
- 提醒阈值可选，当前输入防错范围 `30..260`，它不是医学极限或第七区间。
- effective maximum 的唯一顺序为：有效个人最大心率优先；否则有效年龄使用 `220 - age`；否则为 none。
- personal maximum 和 age estimate 同时存在时，年龄值不得参与区间计算。
- 无 effective maximum 时只显示 bpm；提醒阈值仍可独立严格触发。
- 提醒仅在 `bpm > threshold` 时优先；相等不触发。
- 六区间使用未取整整数比率边界：`<50%` 低强度、`[50,60)%` 热身、`[60,70)%` 燃脂、`[70,80)%` 有氧、`[80,90)%` 无氧、`>=90%` 极限。

优先级测试使用年龄 40、个人最大心率 200、bpm 150：若错误使用 `220-age=180` 会得到无氧；正确优先使用个人最大值 200 得到有氧。因此测试能够真实区分两条路径。

只修改胶囊外部 presentation mapper。`HeartRateFloatingCapsule.kt`、`HeartRateCapsuleGeometry.kt`、布局、颜色、尺寸、动画和交互均未修改。

## 5. 测试与失败证据

测试先于实现加入，初次编译因缺少 recovery policy/action、DataStore 字段和 personal maximum mapper 输入而失败，形成 RED。实现后逐步关闭：

- 128 组合 eligibility 全矩阵。
- visible 与合法 training FGS 的替代关系。
- bounded windows、间隔和长期 armed。
- wrong candidate 不连接，exact target 自动连接。
- saved target 改变时关闭旧 attempt，再恢复新 target。
- recovery scan 期间 target 从 A 改为 B 时立即失效旧 callback，旧 A 不能连接，B 使用新 window。
- 重复提交相同 eligible context 不重启 scan、不把 `SEARCHING` 错写为 waiting。
- 无效手动 target 在 candidate 验证前不得替换 owner 内已 armed 的 exact target。
- unexpected status 19 disconnect、scan failure 后继续 armed。
- permission TOCTOU fail-closed，不形成恢复循环。
- queued eligibility loss 在平台动作前取消恢复。
- persistent suppression、设备选择清除 suppression、参数边界和文件重建。
- 个人最大心率优先、年龄 101、六区间精确边界和 strict alert。

## 6. 文件范围

Production：

- `core/datastore/TrainFlowPreferenceKeys.kt`
- `core/datastore/TrainFlowPreferences.kt`
- `core/datastore/TrainFlowPreferencesDataSource.kt`
- `core/health/HeartRateRecoveryPolicy.kt`
- `core/health/HeartRateRuntimeOwner.kt`
- `ui/shell/official/HeartRateFloatingCapsuleState.kt`

Tests：

- `core/datastore/TrainFlowPreferencesBoundaryTest.kt`
- `core/health/HeartRateRecoveryPolicyTest.kt`
- `core/health/HeartRateRuntimeOwnerRecoveryTest.kt`
- `ui/shell/official/HeartRateFloatingCapsuleStateTest.kt`

Documentation：

- 本文档。

## 7. Git 与 executable identity

- Accepted base：`57075700fe6754de1d1cd6dbc062ad985aa34f5a`
- Foundation commit：`2e11f3c7928a97b6c144caa7ed039cde57dd223d`（`Add heart-rate recovery and personal parameter foundation`）
- Target-transition Repair / final executable source SHA：`f425945ea8cc63568528c5d76b45d6e814f924b3`
- Target-transition commit：`Protect exact recovery target transitions`
- Branch：`codex/e17-7a-reconnect-parameter-foundation-v2`
- Debug APK bytes：`14780298`
- Debug APK SHA256：`F42A5A0039FCFB363BC3B5DE6F73A3B3FEFE83AD8792121F801A5652F9AE75B0`
- Variant/applicationId：`debug` / `com.liujyks.trainflow`

APK 只证明 implementation source 的 build identity。由于新 owner 尚未 production/debug 接线，本 Story 不安装 APK、不运行 AVD/Band 9，也不宣称自动恢复已在用户 App 中可见。设备行为证据属于 E17-7b 和 E17-9。

## 8. 自动验证

环境：Temurin JDK 17.0.19、Gradle 9.4.1、Kotlin 2.3.21。

- recovery policy + owner focused：`16 / 0 / 0 / 0`
- owner/policy/DataStore/presentation expanded focused：`83 / 0 / 0 / 0`
- `*HeartRate*`：21 suites，`146 / 0 / 0 / 0`
- 全量 `:app:testDebugUnitTest`：78 suites，`768 / 0 / 0 / 0`
- final source `:app:assembleDebug --rerun-tasks`：通过，5分2秒
- final source `:app:lintDebug -Dkotlin.incremental=false`：通过，0 errors、35 warnings、16 hints，5分56秒
- final source `:app:check`：通过，27秒
- `git diff --check` 与 staged diff check：通过

命令外层的 60/120 秒等待窗口曾先返回，但同一 Gradle/Java 进程持续有 CPU 活动；没有重复启动任务。最终结果从 JUnit XML、Gradle daemon log、lint report 和 APK artifact读取。长耗时来自强制全量 Kotlin/Android lint，而不是重复验证、阶段回退或任务死循环。

提交前 fresh diff self-review 发现 target-transition 竞态：进行中的 A recovery scan 在 saved target 改为 B 后仍可能连接 A；无效手动 identifier 也可能在 candidate 验证前污染后续 recovery target。三个新测试先分别失败，再由 `f425945...` 修复。该 executable 变化使前一 `2e11f3c...` APK 身份失效，因此最终 APK、lint 和 check 均从 `f425945...` 重新生成/验证。

## 9. E17-7b 交接

E17-7b 必须完成：

1. `TrainFlowApplication` 唯一创建并暴露同一个 owner。
2. 设置页提供明确的“断开心率设备”“重新连接”“清除已保存设备”“关闭心率功能”，四种动作和文案不得混淆。
3. 断开先持久 suppression，再 cleanup；重新连接/选择设备清除 suppression。
4. 把年龄、个人最大心率和提醒阈值接到设置与胶囊 mapper，个人最大心率继续优先于 `220-age`。
5. 接入 visibility facts，验证非训练后台 cleanup、返回前台 eligible 自动恢复。
6. 原子退休旧 provider/scanner ownership；production/debug 只能有一个可达 owner。
7. 使用 final executable source 完成 AVD lifecycle/state evidence 与 Band 9 basic GATT/notify gate。

E17-7a 未完成上述 UI/production/device验收，也不得被描述为用户已能看到自动重连或手动断开按钮。
