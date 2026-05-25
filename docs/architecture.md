---
workflowType: architecture
projectName: TrainFlow
documentLanguage: zh-Hans
status: draft
date: 2026-05-26
inputDocuments:
  - docs/project-status.md
  - docs/planning/decision-log.md
  - docs/planning/prd.md
  - docs/planning/ux-design.md
  - docs/planning/data-contracts.md
stepsCompleted:
  - architecture-baseline
  - module-boundaries
  - data-and-engine-boundaries
  - platform-adapter-boundaries
---

# TrainFlow Android 首版架构

**文档状态:** 首版架构草案  
**范围:** Android MVP 生产工程与未来 iOS 共享边界  
**不包含:** 具体 Android 工程脚手架、UI 视觉定稿、完整动作内容导入、真实设备接入实现

## 1. 架构目标

本架构要支持 TrainFlow 首版的 4 个核心结果：

1. 用户可以创建、保存、复用计时训练和力量训练计划。
2. 用户可以稳定执行训练，训练中计时、休息、临近结束提醒和力量组记录不丢状态。
3. 动作库、训练计划、训练会话和恢复建议之间有清晰数据边界。
4. 未来语音、心率、媒体和 iOS 适配可以接入，但不增加首版实现负担。

## 2. 架构原则

1. **Android 原生首发。** 首版生产 App 使用 Kotlin 与 Jetpack Compose，优先保证 Android 训练执行、通知、震动、音频和后台行为可靠。
2. **业务核心与平台能力分离。** 训练计划模型、训练执行引擎、训练命令、训练事件和恢复规则放在业务层；通知、音频、震动、健康数据和设备接入放在平台适配层。
3. **本地优先。** 首版以本地持久化为主，不依赖云同步，不要求登录账号。
4. **执行引擎优先于页面状态。** UI 只发命令并展示状态；训练推进、计时、事件产生和记录写入由可测试的业务组件负责。
5. **先保留接口，不展示假能力。** 心率、语音、课程、音乐节拍、AI 纠错等能力只保留模型和适配边界，首版页面不提供不可工作的假入口。

## 3. 技术基线

| 领域 | 首版选择 | 说明 |
|---|---|---|
| App 平台 | Android 原生 | 未来 iOS 另行适配，业务语义保持可迁移。 |
| 语言 | Kotlin | 与 Android 生态、协程、Room、Compose 配合。 |
| UI | Jetpack Compose | 单 Activity，多页面导航，适合训练执行页状态驱动更新。 |
| 架构风格 | 分层架构 + feature 模块 | UI、domain、data、platform adapter 分离。 |
| 异步 | Kotlin Coroutines + Flow | 训练计时、状态订阅、数据库流式观察。 |
| 本地数据库 | Room | 保存动作、计划、会话、组记录、恢复映射。 |
| 偏好设置 | DataStore | 保存提醒偏好、心率展示偏好、训练默认值。 |
| 依赖注入 | Hilt | 生产实现与测试替身解耦。 |
| 后台与提醒 | Notification + WorkManager/Alarm 边界 | 首版普通提醒，不做闹铃级强提醒硬依赖。 |
| 最小网络 | 无必需网络 | 首版动作内容可随包或本地导入，后续再加同步。 |

## 4. 应用模块建议

首版 Android 工程建议从以下模块起步。早期可以先用较少 Gradle 模块实现，代码包结构仍按此边界组织；当体量增加后再拆物理模块。

```text
app
core:model
core:database
core:datastore
core:domain
core:engine
core:notifications
core:media
core:health
feature:home
feature:plans
feature:exercise-library
feature:workout-session
feature:history
feature:recovery
feature:settings
```

### 4.1 `app`

- 提供 Android Application、MainActivity、导航图、主题、Hilt 入口。
- 不直接包含训练业务规则。
- 只组装 feature 页面和全局依赖。

### 4.2 `core:model`

- Kotlin data class 与枚举，映射 `docs/planning/data-contracts.md`。
- 包含 `Exercise`、`WorkoutPlan`、`PlanBlock`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent`、`HeartRateState`、`RecoveryRecommendation`。
- 不依赖 Android SDK，未来可迁移到 KMP 或 iOS 共享语义。

### 4.3 `core:domain`

- 用例和业务规则。
- 计划创建、计划复制、计划校验、动作筛选、恢复建议生成、训练总结生成。
- 不直接读写 Room，不直接调用 Android 通知、震动、音频。

### 4.4 `core:engine`

- 训练执行引擎，负责命令到状态的转换。
- 输入是 `CommandEnvelope` 和时间 tick。
- 输出是 `WorkoutSessionState` 与 `WorkoutEvent`。
- 首版必须覆盖计时训练和力量训练两条状态机。
- 不直接操作 Compose，不直接展示 UI。

### 4.5 `core:database`

- Room entities、DAO、migration。
- 保存标准动作、训练计划、训练会话、步骤记录、力量组记录、恢复区域和恢复映射。
- 计划快照必须随训练会话保存，避免历史记录被后续计划修改污染。

### 4.6 `core:datastore`

- 保存轻量偏好：
  - 默认临近结束提醒秒数。
  - 声音、震动、强化动画开关。
  - 力量训练本组计时默认模式。
  - 心率占位显示偏好。

### 4.7 `core:notifications`

- 计划提醒通知。
- 活跃训练通知。
- 前台训练服务边界。
- 不包含训练状态机，只消费 `WorkoutEvent` 或显示 session 摘要。

### 4.8 `core:media`

- 提示音播放。
- 后续语音提示、动作媒体播放、跟练媒体播放的接口边界。
- 首版只需要可由训练事件触发的提示音能力。

### 4.9 `core:health`

- `HeartRateProvider` 抽象接口与 mock/disabled 实现。
- 首版不绑定任何具体手环 SDK。
- 后续可接 Health Connect、Wear OS 或设备厂商 SDK，但不能反向污染训练执行引擎。

### 4.10 feature 模块

| 模块 | 职责 |
|---|---|
| `feature:home` | 训练首页、最近计划、继续训练入口。 |
| `feature:plans` | 计时/力量计划创建、编辑、详情、提醒设置。 |
| `feature:exercise-library` | 动作库列表、筛选、动作详情、训练中动作详情入口。 |
| `feature:workout-session` | 计时训练执行页、力量训练执行页、跟练雏形页、确认层。 |
| `feature:history` | 训练总结、训练记录、基础趋势。 |
| `feature:recovery` | 训练后恢复建议。 |
| `feature:settings` | 训练偏好、通知偏好、心率显示偏好。 |

## 5. 分层数据流

```text
Compose Screen
  -> ViewModel
  -> UseCase / WorkoutEngine
  -> Repository
  -> Room / DataStore / Platform Adapter

WorkoutEngine
  -> WorkoutEvent
  -> UI / Sound / Haptics / Notification / Analytics
```

### 5.1 UI 层

- Compose 页面只展示 `UiState` 并发出用户意图。
- 训练中按钮不直接改 session 数据，而是发 `WorkoutCommand`。
- 训练执行页必须从引擎状态恢复，不把关键状态只存在 ViewModel 内存。

### 5.2 Domain 层

- 统一计划校验：
  - 计时训练必须有可执行动作或阶段。
  - 力量训练必须有动作和至少一组。
  - 临近结束阈值不能大于对应动作或休息时长。
- 统一动作能力校验：
  - 计时计划只能加入支持计时训练的动作。
  - 力量计划只能加入支持 reps 或 weight 的动作。
  - 跟练雏形优先使用支持跟练展示的动作。

### 5.3 Data 层

- Repository 输出 domain model，不把 Room entity 暴露给 UI。
- 首版允许对 `PlanBlock`、`WorkoutPlanSnapshot` 使用 JSON 字段保存，以降低多态结构早期建模成本；关键查询字段仍应单独列化。
- 后续若需要复杂统计，再把 blocks 和 session steps 进一步规范化。

## 6. 数据模型映射

### 6.1 Room 表建议

| 表 | 说明 | 关键字段 |
|---|---|---|
| `exercises` | 标准动作库 | `id`、`name`、`category`、`equipment`、`difficulty`、`capabilities_json`、`content_status` |
| `workout_plans` | 用户训练计划 | `id`、`mode`、`title`、`blocks_json`、`reminder_json`、`preferences_json`、`created_at`、`updated_at` |
| `workout_sessions` | 训练会话 | `id`、`plan_id`、`mode`、`status`、`plan_snapshot_json`、`started_at`、`ended_at` |
| `session_step_records` | 执行步骤记录 | `id`、`session_id`、`kind`、`exercise_id`、`started_at`、`ended_at`、`skipped` |
| `strength_set_records` | 力量组记录 | `id`、`session_id`、`exercise_id`、`set_order`、`planned_json`、`actual_json`、`effort` |
| `recovery_areas` | 恢复区域 | `id`、`name`、`body_region`、`summary` |
| `recovery_recommendations` | 训练恢复建议 | `id`、`session_id`、`trained_muscle_ids_json`、`area_ids_json` |

### 6.2 快照规则

- 开始训练时生成 `WorkoutSession`。
- 会话保存 `WorkoutPlanSnapshot`。
- 后续编辑计划不影响历史会话。
- 力量组记录保存计划值与实际值，不能只保存差异。

## 7. 训练执行引擎

训练执行引擎是首版架构核心。

### 7.1 输入

- `WorkoutPlanSnapshot`
- `WorkoutCommand`
- 时间 tick
- 用户偏好

### 7.2 输出

- 当前 `SessionStep`
- 当前剩余时间
- 训练进度
- 待确认记录草案
- `WorkoutEvent`
- 可持久化的 `WorkoutSession` 变更

### 7.3 计时训练状态

```mermaid
stateDiagram-v2
    [*] --> Ready
    Ready --> TimedWork: "start_session"
    TimedWork --> TimedRest: "work duration ended"
    TimedRest --> TimedWork: "rest duration ended"
    TimedWork --> Paused: "pause_session"
    TimedRest --> Paused: "pause_session"
    Paused --> TimedWork: "resume_session"
    Paused --> TimedRest: "resume_session"
    TimedWork --> Completed: "last step ended"
    TimedRest --> Completed: "last rest ended"
    TimedWork --> Completed: "end_session"
    TimedRest --> Completed: "end_session"
```

计时训练必须区分：

- 动作临近结束事件。
- 休息临近结束事件。
- 跳过动作。
- 延长休息。
- 暂停时剩余时间冻结。

### 7.4 力量训练状态

```mermaid
stateDiagram-v2
    [*] --> Ready
    Ready --> PrepareSet: "start_session"
    PrepareSet --> ActiveSet: "start_strength_set"
    ActiveSet --> ConfirmSet: "complete_strength_set"
    ConfirmSet --> StrengthRest: "confirm_strength_set"
    StrengthRest --> PrepareSet: "rest ended"
    StrengthRest --> ActiveSet: "start_strength_set"
    PrepareSet --> Completed: "all sets done"
    ActiveSet --> Paused: "pause_session"
    StrengthRest --> Paused: "pause_session"
    Paused --> ActiveSet: "resume_session"
    Paused --> StrengthRest: "resume_session"
```

力量训练必须保证：

- `开始本组` 后才记录本组耗时。
- `完成本组` 后先生成确认草案。
- 确认层默认回填计划重量和计划次数。
- 用户确认后再写入正式 `StrengthSetRecord`。
- 休息倒计时和休息临近结束提醒独立于动作组耗时。

## 8. 平台能力边界

### 8.1 通知与训练提醒

首版采用普通训练提醒，不把闹铃级强提醒作为 MVP 硬依赖。

- 计划提醒：通过通知调度实现，允许系统延迟。
- 活跃训练：训练进行中可显示 ongoing notification。
- 后台训练：若训练离开前台仍需可靠推进，使用前台服务边界；是否首版启用取决于实现 story。
- 权限：清楚解释通知权限用途。

### 8.2 声音与震动

- 声音和震动都是 `WorkoutEvent` 的消费者。
- 倒计时最后 N 秒是否播放声音/震动由 `CueSettings` 控制。
- 首版不播放语音读秒；仅保留 `voiceCueEnabled` 和语音输出适配接口。

### 8.3 心率与健康数据

首版只实现抽象状态和占位：

```kotlin
interface HeartRateProvider {
    val heartRateState: Flow<HeartRateState>
}
```

- 默认实现可以是 `DisabledHeartRateProvider` 或 mock。
- UI 可以展示未连接、连接中、正常、数据中断等状态。
- 不做实时心率预警闭环。
- 不因没有设备而阻塞训练闭环。

### 8.4 媒体

- 动作媒体字段允许为空。
- 首版动作详情可先用文字、图文占位或本地静态资源。
- 跟练雏形复用计时流程，不依赖教练视频课程。

## 9. 权限与隐私

首版可能涉及的权限：

| 权限/能力 | 首版用途 | 约束 |
|---|---|---|
| 通知权限 | 训练提醒、活跃训练提示 | 必须说明用途，可关闭。 |
| 前台服务 | 训练离开前台时持续提示 | 仅在需要后台训练计时时使用。 |
| 震动 | 临近结束提醒 | 由用户偏好控制。 |
| 健康数据 | 首版预留 | 未接入时不请求。 |

首版不采集医疗数据，不做医疗结论，不上传训练数据到远端服务。

## 10. 测试策略

### 10.1 单元测试

优先覆盖：

- 计时训练状态推进。
- 动作临近结束和休息临近结束事件。
- 暂停、继续、跳过、延长休息。
- 力量训练开始本组、完成本组、确认记录、进入休息。
- 计划快照不被后续计划修改污染。
- 恢复建议由训练肌群映射到恢复区域。

### 10.2 ViewModel 测试

- 首页入口状态。
- 计划编辑校验。
- 训练执行页从 engine state 恢复 UI。
- 单组确认层默认值。

### 10.3 Android 集成测试

- Room migration smoke test。
- 关键导航路径。
- 通知权限关闭时训练闭环仍可使用。
- 横竖屏或进后台后训练状态恢复。

## 11. 未来 iOS 与共享边界

首版不做 iOS，但以下语义必须保持稳定：

- `WorkoutPlan`
- `PlanBlock`
- `WorkoutSession`
- `WorkoutCommand`
- `WorkoutEvent`
- `HeartRateState`
- `RecoveryRecommendation`

未来可迁移的部分：

- 训练执行状态机。
- 计划校验。
- 恢复建议规则。
- 动作能力筛选。

必须平台侧实现的部分：

- 通知与提醒。
- 音频与震动。
- 健康数据权限。
- 蓝牙或可穿戴设备连接。
- 后台运行策略。

## 12. 首版明确不做

- 不建设课程运营平台。
- 不接入真实 AI 动作纠错。
- 不交付自动语音教练。
- 不做医学化心率告警。
- 不做云同步与账号体系。
- 不做 Android 与 iOS 同步开发。
- 不把具体手环 SDK 字段写进核心模型。

## 13. 待实现前确认

进入 Android 工程脚手架前需要确认：

1. 最低 Android 版本与目标 Android SDK。
2. 是否首版要求训练退到后台后仍持续准确计时。
3. 首批动作内容是随包静态 JSON，还是 Room seed 数据。
4. 是否在首版显示心率未连接占位，还是默认隐藏到设置中。
5. 训练提醒是否只做普通通知，还是在后续版本增加精确提醒选项。

## 14. 参考

- `docs/planning/prd.md`
- `docs/planning/ux-design.md`
- `docs/planning/data-contracts.md`
- Android 官方 App Architecture 指南
- Android 官方 Jetpack Compose 文档
- Android 官方 Room、DataStore、WorkManager 与 Health Connect 文档
