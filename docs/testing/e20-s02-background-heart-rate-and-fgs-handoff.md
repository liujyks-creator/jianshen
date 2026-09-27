# E20-S02-P1 后台心率与训练通知交接证据

## 当前合同

用户在“心率与设备”开启心率功能并连接已选设备后，只要 App 进程存活，训练中或未训练、前台或后台均继续接收。未训练时不新增持续通知；训练使用既有单一 `7200` 通知与 `connectedDevice` FGS 交接。训练结束移除训练通知，合格心率连接继续由 Application 唯一 owner 持有，但不再写入已结束训练。关闭心率功能主动停止；系统结束进程后不自动重启，下次打开按已保存目标和当前资格重新连接。三模式原训练、Saved 和历史流程保持。

原方案来源为 `.local/planning/E20-S02-IMPLEMENTATION-PROPOSAL.md` §8–12、§14–18；本轮用户批准的窄变更记录在 `.local/evidence/E20-S02/writer-attempt-1/approval-decisions.md`。旧方案中“非训练后台清理”“后台终态清理”和以FGS状态作为BLE恢复资格的预期由本决定替代。未获批准的其他设备步骤、用例或写路径不纳入本报告。

## 已执行的自动化

原首批 19 次因 V12 已批准拆为五个独立 Application 方法，另有四个直接受新BLE资格影响的既有方法，合计 23 个不同方法。第一次完整编译被 `WorkoutHeartRateServiceTest` 读取 Robolectric 私有字段阻断，**没有执行测试方法**；修正为读取 Android `Service.foregroundServiceType` 后，23 个方法执行，21 通过、2 项测试设施失败。用户只批准修正这两项并各重跑一次，结果均通过；21 项未重跑。通过结果分别绑定以下原始输出，不拼接为一次运行：

- `.local/evidence/E20-S02/writer-attempt-1/unit-measurement/first-batch-23-after-compile-fix.txt`：23 executed、21 passed、2 failed。失败方法是 `WorkoutHeartRateServiceTest.servicePromotesImmediatelyWithDeniedNotificationPermissionAndIsNotSticky` 和 `WorkoutSessionTimelineRecorderTest.backgroundCleanupProducesDisconnectedGapWithoutChangingRecordingIdentity`。
- `.local/evidence/E20-S02/writer-attempt-1/unit-measurement/two-failed-methods-one-rerun.txt`：上述两方法各一次，`BUILD SUCCESSFUL`。Service 测试使用真实 Application 可见/心率/目标/BLE资格；Recorder 测试用 Room 2.8.4 表失效 Flow 等待原样本和gap写入，保留全部原断言、原样本/业务输入及 5000ms 上限。

首批实际执行方法调用 25 次，23 个不同方法均有通过结果，但不是同一批次的“23/23通过”。计划中 M1 后的最终阈值方法、final AVD 两阶段、手机两次定向只读及真实设备步骤尚未运行。若阈值改变，三个原有受影响方法才按批准条件运行。已批准的后续自动化总调用上限为正常 30 次、阈值改变时 33 次；实际次数以分批原始产物报告。

## 尚未取得的证据

- measurement 与 final 各自的 source SHA/tree、两份 APK hash/bytes、两次限定构建和安装身份。
- M1 的 Band 9 真机后台保持、真实断链恢复、同owner/attempt线索、记录保存后定向读回及 freshness W/L 依据。
- final AVD 同一方法的 handoff / after_restart 两阶段系统通知与进程证据。
- final 手机 U01–U03 原训练步骤；U01 保存后另有已批准的 120 秒未训练后台接收和一次本 App 通知状态快照。它不增加构建、安装或设备轮次。
- final 本场记录读回、独立 Review、集成与人工验收。

单元测试只能证明相应 JVM/Shadow 及应用接线层；Android 服务系统状态须由 AVD 证明，真实 Band 9 的 BLE 回调、后台接收和写库须由手机证据证明。上述证据未取得前，本候选保持未验收。
