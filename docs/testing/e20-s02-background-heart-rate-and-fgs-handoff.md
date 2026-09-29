# E20-S02-P1 后台心率与训练通知交接证据

## 当前合同

用户在“心率与设备”开启心率功能并连接已选设备后，只要 App 进程存活，训练中或未训练、前台或后台均继续接收。未训练时不新增持续通知；训练使用既有单一 `7200` 通知与 `connectedDevice` FGS 交接。训练结束移除训练通知，合格心率连接继续由 Application 唯一 owner 持有，但不再写入已结束训练。关闭心率功能主动停止；系统结束进程后不自动重启，下次打开按已保存目标和当前资格重新连接。三模式原训练、Saved 和历史流程保持。

原方案来源为 `.local/planning/E20-S02-IMPLEMENTATION-PROPOSAL.md` §8–12、§14–18；本轮用户批准的窄变更记录在 `.local/evidence/E20-S02/writer-attempt-1/approval-decisions.md`。旧方案中“非训练后台清理”“后台终态清理”和以FGS状态作为BLE恢复资格的预期由本决定替代。未获批准的其他设备步骤、用例或写路径不纳入本报告。

## 已执行的自动化

原首批 19 次因 V12 已批准拆为五个独立 Application 方法，另有四个直接受新BLE资格影响的既有方法，合计 23 个不同方法。第一次完整编译被 `WorkoutHeartRateServiceTest` 读取 Robolectric 私有字段阻断，**没有执行测试方法**；修正为读取 Android `Service.foregroundServiceType` 后，23 个方法执行，21 通过、2 项测试设施失败。用户只批准修正这两项并各重跑一次，结果均通过；21 项未重跑。通过结果分别绑定以下原始输出，不拼接为一次运行：

- `.local/evidence/E20-S02/writer-attempt-1/unit-measurement/first-batch-23-after-compile-fix.txt`：23 executed、21 passed、2 failed。失败方法是 `WorkoutHeartRateServiceTest.servicePromotesImmediatelyWithDeniedNotificationPermissionAndIsNotSticky` 和 `WorkoutSessionTimelineRecorderTest.backgroundCleanupProducesDisconnectedGapWithoutChangingRecordingIdentity`。
- `.local/evidence/E20-S02/writer-attempt-1/unit-measurement/two-failed-methods-one-rerun.txt`：上述两方法各一次，`BUILD SUCCESSFUL`。Service 测试使用真实 Application 可见/心率/目标/BLE资格；Recorder 测试用 Room 2.8.4 表失效 Flow 等待原样本和gap写入，保留全部原断言、原样本/业务输入及 5000ms 上限。

首批实际执行方法调用 25 次，23 个不同方法均有通过结果，但不是同一批次的“23/23通过”。M1 后阈值改变，V15/V06–V08 四个指定方法各通过一次，原始输出为 `.local/evidence/E20-S02/writer-attempt-1/unit-final/v15-v06-v07-v08.txt`。后续获批的 `liveBackgroundDisconnectReconnectsSameGattAndResubscribes` 定向方法通过；`failedReconnectAfterLiveSubscriptionKeepsGattForAnotherConnect` 先有有效 RED，修复后通过。第一次编译/设施失败和各轮旧失败均保留，不将分批结果拼成一次全绿运行。

## M1、final 与当前修复版的证据

- measurement 源码为 `7f00055a26195e63439dbf90ddcfedec9d7fe8f9`（tree `349d8978235e8eccd0b72c2c0fd76cc6142492c2`），App APK SHA-256 为 `840CEC85F940330B22B56BFDCFC060C4E8219B0AE3FBBA30BB9208D471523CE3`。M1 同场保存后定向读回为 1617 条样本、35 条 acquisition；真实接收、锁屏与其他 App 阶段及断链分别按原日志判定，不把物理断链间隔算作健康样本间隔。原始身份与路径见 `.local/evidence/E20-S02/writer-attempt-1/measurement-evidence-binding.json`。
- M1 合格订阅后的首个有效样本最大等待为 5011ms，同一未断链 attempt 的相邻有效样本最大间隔为 1233ms。按已批准的 M0/M1 公式，最终 `W=15500ms`、`L=2500ms`；输入、计数和计算见 `.local/evidence/E20-S02/writer-attempt-1/freshness-measurements.json`。
- 原 final 源码为 `0aafac6c0d1418c2b8b367e1bdf7825760c4cf99`（tree `8438e9a2f643d737d8f9d8aa4dd15f7c542a05e2`），App APK SHA-256 为 `1D67E98CAB7E7039E7FA36078DD20AE7E65BBC4274F2136F59B67CD8A2763911`。AVD 的 `handoff`、`after_restart` 各运行一次且各 `OK (1 test)`；它们证明平台通知与进程边界，不证明实体 BLE。Band 9 原 U01/U02/U03 三场均按原入口保存，唯一一次定向读回分别有 1205/638/419 条样本和 57/1/1 条 acquisition；U02 终态后 66ms 的通知未进入该场记录。原 U01 训练后台 120 秒恢复失败，仍是旧 APK 的失败事实。原始身份与路径见 `.local/evidence/E20-S02/writer-attempt-1/final-evidence-binding.json` 和同目录 `HUMAN_ACCEPTANCE.md`。
- 第二修复版应用源码由 `b1e688e3e0e9bc939a3d4085fff1953d1b780f8d`（tree `a9c6a3bf88b43817831848de3375b6b3b8df4efe`）承载，已签名 App APK SHA-256 为 `78E765643D0F51A7F2F4EC6E783CD6327819928A9994F504E0B08E9BFA6EA6A1`。后续批准仅覆盖已订阅同一 GATT 的意外断链和重连失败回调，保留同一 GATT 并再次 `connect()`；主动停止仍释放。该版非训练桌面后台一轮与自由跟练 U01 训练桌面后台一轮均观察到原进程、同一 GATT 多次失败后重新订阅和有效心率；U01 恢复样本提交到原 binding。用户报告从通知返回并正常结束。日志分别为 `.local/evidence/E20-S02/writer-attempt-1/phone-final/post-diagnostic-second-repair-verification-logcat.txt` 和 `post-diagnostic-second-repair-u01-targeted-logcat.txt`。

单元测试只证明相应 JVM/Shadow 与应用接线层；AVD 证明平台通知交接，手机日志证明相应 APK 与场次的真实 BLE 回调及输入提交。第二修复版 U01 未新增保存数据读回，旧三场读回不能冒充新版 U01 的持久化实测。广播物理重新开启的精确时刻未采集，“120 秒内”仅沿用户回复与日志观察边界判定。首轮 Review 请求修正文档；交付状态由修订后 Review 结论、准确候选的 main 祖先关系和远端同步决定，本报告不提前宣称集成。
