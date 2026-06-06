# TrainFlow E9.1 训练状态恢复与关键回归测试清单

**状态:** E9.1 用户测试前恢复与回归基线
**适用范围:** 计时训练、力量训练、基础跟练复用的计时执行流、三套内置 UI 皮肤、普通通知与提示反馈边界。
**使用时机:** E9.1 Review Gate、用户测试前 smoke、用户测试问题回看。

本文档只定义当前恢复能力、回归检查和用户测试观察项。E9.1 不实现真实 `WorkoutSession` 持久化，不接 Room repository 闭环，不新增 foreground service、后台可靠计时、exact alarm、notification action、真实心率设备、语音或健康平台接入。

## 1. 当前恢复能力结论

| 场景 | 当前期望 | E9.1 结论 |
|---|---|---|
| 暂停后返回训练页 | 引擎状态仍在当前内存态 route 中；暂停时剩余时间、本组耗时或休息剩余时间冻结 | 支持内存态恢复；单元测试覆盖 |
| App 进入后台后再回前台 | 未被系统杀死时，当前 Activity / Compose route 内存态保留；活跃训练普通 notification 只显示摘要 | 可 smoke；不承诺后台精确推进 |
| 屏幕旋转或 Activity 重建 | 当前未建立真实 session state 持久化或 SavedState 恢复闭环 | 记录为 E9 风险；不得标记为完整支持 |
| 进程被系统杀死后再打开 | 当前没有真实 `WorkoutSession` 持久化和 repository 闭环，无法恢复当前训练步骤、剩余时间或确认草案 | 明确不支持；后续 story 处理 |
| completed / abandoned 终态后收到命令 | 后续 pause / resume / skip / extend / confirm / end 等命令不得污染 terminal state、records 或 history | 单元测试覆盖 |

用户测试报告中必须区分“未被系统杀死的内存态返回”和“进程死亡后的恢复”。当前 App 的普通 ongoing notification 不等于后台可靠计时，也不等于进程死亡恢复能力。

## 2. 计时训练回归

### 必测路径

- 开始计时训练后暂停，等待一段时间，再继续。
- 暂停期间 tick 不推进剩余时间。
- 进入休息后 `+15秒` 只影响当前休息步骤。
- 跳过当前步骤后进入下一动作或休息，最后一步跳过进入 completed。
- 提前结束进入 abandoned，不伪装为 completed。
- completed / abandoned 后再次发送暂停、继续、跳过、延长休息或结束训练，不改变 terminal state。

### 用户测试观察

- 热身、正式动作、动作后休息、轮间休息、放松/拉伸是否都应支持最后 N 秒提醒。
- 当前已明确覆盖动作与休息临近结束提醒；若热身、放松或其他计时阶段缺少最后 N 秒声音、震动或屏幕强化，记录为 E9 后续修复项。
- 跟练雏形复用计时执行流；不得把跟练 smoke 结果写成完整课程平台能力。

## 3. 力量训练回归

### 必测路径

- 准备态 `开始本组` 可达。
- 进行态 `完成本组` 可达，组耗时正常累计。
- 完成本组后进入确认态，计划重量和计划次数预填实际记录。
- 确认态暂停后，确认草案和本组耗时保持不变；继续后可确认。
- 组间休息暂停后，剩余时间冻结；继续后可继续休息或提前开始下一组。
- 提前结束进入 abandoned，不生成 completed 事件。
- completed / abandoned 后再次发送开始、完成、确认、跳过、替换或结束训练，不污染 `StrengthSetRecord`、history 或 terminal state。

### 用户测试观察

- 力量确认层在小屏上必须能看到实际重量、实际次数、感受选择和确认动作。
- 主控制必须保持可达：开始/完成/确认本组、暂停/继续、结束训练。
- 替换和跳过是训练中调整能力，不等于自动改写原始计划。

## 4. 三套 Skin 与小屏控制回归

每次 E9.1 smoke 至少覆盖 Official Flow、Tile Flow、Big Type：

| 页面 | 720x1280 小屏检查 |
|---|---|
| 训练首页 | 计时默认入口、力量入口、跟练入口、动作库/计划/设置入口不互相遮挡 |
| 计时执行页 | 当前动作/休息、主倒计时、暂停/继续、跳过、`+15秒`、结束训练即时可见 |
| 力量执行页 | 当前动作、本组目标、开始/完成/确认、暂停/继续、结束训练即时可见 |
| 力量确认层 | 实际重量、实际次数、感受选择和确认动作不溢出；计划值预填语义不变 |
| 设置页 | UI 皮肤、训练反馈、通知边界说明可读 |

Skin 切换只能改变 UI 表现、布局倾向和 token。不得改变 `WorkoutPlan`、`WorkoutCommand`、`WorkoutEvent`、`WorkoutSession`、训练执行引擎、通知、权限、心率或恢复建议语义。三套 skin 的 mode pill 对比度必须保持 WCAG AA。

## 5. 通知与后台边界

- 计划提醒和活跃训练通知都是普通通知。
- 活跃训练 ongoing notification 是状态摘要，不是 foreground service。
- 通知权限关闭时，训练执行闭环仍可正常使用；只是通知不展示或不弹出。
- 当前不申请 `SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM`、`FOREGROUND_SERVICE`、健康、身体传感器、蓝牙或定位权限。
- 不新增 notification action 控制训练；训练控制仍通过 UI 发 `WorkoutCommand`。

## 6. 音频共存回看

- 训练提示音不得请求会降低、暂停或打断其他 App 音乐/视频的 audio focus。
- 不主动 ducking，不调用 `requestAudioFocus`，不使用 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`。
- 用户测试时记录 Android 版本、设备型号、音乐/视频 App、是否蓝牙输出，以及是否出现音量降低、暂停、抢焦点或提示音不可闻。
- 若不同设备存在异常，记录为后续平台音频适配问题；E9.1 不扩大为完整音频策略改造。

## 7. 非医疗化边界

- 心率展示仍是抽象状态，占位或 mock 状态不得驱动训练中断、危险判断或医疗告警。
- 未接入真实设备时，不请求健康、身体传感器、蓝牙或定位权限。
- 恢复建议只基于训练动作映射到基础放松区域，不写成诊断、康复治疗、疾病适应性建议或治疗承诺。

## 8. E9.1 Review 记录格式

| 项目 | 结果 |
|---|---|
| 分支 / commit |  |
| 单元测试 | Pass / Issue |
| `app:assembleDebug` | Pass / Issue |
| `app:lintDebug` | Pass / Issue |
| `app:check` | Pass / Issue |
| 模拟器 smoke | Not run / Pass / Issue |
| Official Flow 小屏 | Not run / Pass / Issue |
| Tile Flow 小屏 | Not run / Pass / Issue |
| Big Type 小屏 | Not run / Pass / Issue |
| 后台返回 | Not run / Pass / Issue |
| Activity 重建 / 旋转 | Not supported / Issue / Follow-up |
| 进程杀死恢复 | Not supported by current architecture |
| 通知权限关闭 | Not run / Pass / Issue |
| 音频共存 | Not run / Pass / Issue |
| 非医疗化文案 | Pass / Issue |

## 9. 后续事项

- E9.2：权限与隐私文案完整收口，包括通知、心率、恢复建议和未接入能力说明。
- E9.3：MVP 最终验收清单，统一汇总用户测试问题和首版非目标。
- 后续恢复 story：若决定支持 Activity 重建或进程死亡恢复，需要先设计 `WorkoutSession` 持久化、repository 闭环、状态快照、后台计时策略和冲突恢复规则。
- 后续音频适配：若用户测试发现音乐/视频被影响，再单独评估 AudioAttributes、音量、设备差异和提示音可闻性。
