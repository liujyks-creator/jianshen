# TrainFlow E9.2 权限与隐私文案检查清单

**状态:** E9.2 权限与隐私文案收口
**适用范围:** 设置页、计划提醒、活跃训练通知、心率占位、恢复建议、音频提示、语音预留、内存态数据边界。
**使用时机:** E9.2 Review Gate、用户测试前 smoke、用户测试问题回看。

本文档只检查当前文案和轻量 UI 状态是否清楚，不引入真实权限能力，不实现健康设备、语音、foreground service、后台可靠计时或真实 session records 持久化。

## 1. Review 记录格式

| 项目 | 结果 |
|---|---|
| 分支 / commit |  |
| 设置页权限与隐私区 | Pass / Issue |
| 计划提醒权限文案 | Pass / Issue |
| 活跃训练通知文案 | Pass / Issue |
| 心率占位文案 | Pass / Issue |
| 恢复建议边界文案 | Pass / Issue |
| 音频提示文案 | Pass / Issue |
| 语音未实现说明 | Pass / Issue |
| 数据边界说明 | Pass / Issue |
| Manifest 禁止权限 | Pass / Issue |
| 单元测试 | Pass / Issue |
| 模拟器 smoke | Not run / Pass / Issue |

## 2. 必须说清楚

- 通知用于计划提醒和训练中状态提示。
- 通知关闭后，训练仍可正常使用，只是不弹通知。
- 普通通知可能被系统延迟，不是闹钟级强提醒。
- 活跃训练通知只是训练状态摘要，不是 foreground service，不保证后台可靠计时。
- 心率当前是抽象状态 / 占位展示，未接入真实设备、手环、手表或健康数据。
- 心率不做医疗告警、危险判断或训练强度判断。
- 恢复建议基于训练动作 / 部位做基础放松映射，不是医疗诊断、康复治疗或疼痛处理建议。
- 音频提示是短促训练提示音，目标是不降低、暂停或打断其他 App 音乐 / 视频。
- 音频共存不承诺所有设备或 Android 版本完全一致，需用户测试回看。
- 当前只保留训练命令 / 事件边界，未实现语音控制、语音读秒或自动语音教练。
- 当前多数计划、历史、恢复仍是内存态、fixture 或基础展示边界，不代表云同步、账号体系或真实长期记录已完成。

## 3. 禁止暗示

- 不写成已启用闹钟级强提醒、全屏强打断或锁屏强打断。
- 不写成 active notification 能提供后台可靠计时、进程死亡恢复或 notification action 控制训练。
- 不写成已启用 foreground service。
- 不写成已接入 Health Connect、Wear OS、BLE、厂商 SDK、手环、手表或真实健康数据。
- 不写成心率危险判断、医疗告警、疾病监测、热量判断或训练强度判断。
- 不写成恢复建议提供医疗诊断、康复治疗、疼痛处理或疾病适应性建议。
- 不写成已实现语音控制、语音读秒或自动语音教练。
- 不写成已完成云同步、账号体系、真实长期记录或 Room repository 业务闭环。

## 4. Manifest 检查

当前允许：

- `android.permission.POST_NOTIFICATIONS`

当前不得新增：

- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.USE_EXACT_ALARM`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.BODY_SENSORS`
- `android.permission.BODY_SENSORS_BACKGROUND`
- `android.permission.ACTIVITY_RECOGNITION`
- `android.permission.BLUETOOTH`
- `android.permission.BLUETOOTH_ADMIN`
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.BLUETOOTH_SCAN`
- 定位权限
- Health Connect / Android Health 读取心率、运动或热量权限

## 5. 用户测试 Smoke

- 设置页能看到“权限与隐私”说明区，文案可读且不吓人。
- 计划详情提醒区能说明通知权限用途、关闭影响和普通通知延迟。
- 训练中离开 App 后的活跃训练通知只表达状态摘要。
- 计时、力量和基础跟练执行页的心率说明保持辅助层级，不抢当前动作、时间、组目标和主按钮。
- 恢复建议页能说明当前建议来源和非医疗边界。
- 三套 skin 下权限、通知、心率和恢复边界说明不被隐藏或遮挡。
- 音频共存异常只记录为设备 / Android 版本差异，不把 E9.2 扩大为平台音频策略改造。
