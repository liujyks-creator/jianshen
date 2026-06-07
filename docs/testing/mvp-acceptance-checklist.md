# TrainFlow E9.4 User Test Fix Pack 1 验收清单与细测记录

**状态:** E9.4 用户测试修复包 1
**日期:** 2026-06-07
**分支:** `codex/e9-4-user-test-fix-pack-1`
**基线:** `3557a63` / `main` / E9.3 已合入

本文档记录 MVP 用户测试前的能力状态、细测结果、风险分级和 E9.4 修复结果。不在 E9.4 实现 E10 训练模式重构、真实 session 持久化、后台可靠计时、真实心率设备、语音教练、统一动作选择页、完整跟练编排或历史记录真实清理。

## 1. 总结论

当前 TrainFlow 可以生成用户测试 debug APK，但应带着明确告知进入测试：

- **Pass:** 计时训练执行闭环、力量训练执行闭环、动作库 fixture / 详情、基础跟练 preset、三套 UI skin registry、权限/隐私边界、普通通知边界、心率抽象占位、基础历史/趋势和恢复建议均有实现或测试保护。
- **Partial:** 计划编辑、计划列表、历史、恢复、跟练和提醒多为内存态、fixture 或基础展示；仍不代表真实长期记录和 Room repository 业务闭环完成。
- **Pass:** E9.4 已修复计划编辑页数字输入临时清空和编辑页立即开始：计时/力量编辑页的数字草稿可为空，空值时保存/开始禁用并显示原因；有效草稿可直接进入对应执行页。
- **Risk:** 计时训练当前仍要求动作编排；用户反馈倾向于后续把计时训练重构为更接近纯间歇计时器。
- **Product Decision:** 基础跟练后续应承担动作选择、动作推荐、排序和内容展示；动作选择后续应进入独立动作选择页面。这些归入 E10，不在 E9.3 修改。
- **Deferred:** 真实 `WorkoutSession` 持久化、总统计图表、计划级多次趋势、历史记录全部清除 / 按计划清除 / 按日期清除、后台可靠计时、进程死亡恢复、真实心率设备、Health Connect / Wear OS / BLE、语音控制和完整课程平台均不进入 MVP 当前实现。

## 2. 状态标记

| 状态 | 含义 |
|---|---|
| Pass | 当前实现和测试证据满足用户测试前预期。 |
| Partial | 有可用雏形或基础展示，但依赖 fixture、内存态或能力边界。 |
| Deferred | 已确认后续 story / epic 处理。 |
| Out of Scope | 首版明确不做。 |
| Risk | 用户测试前需要显式告知或观察。 |
| Bug | 当前已发现或可从代码/测试证据复现的问题。 |

## 3. MVP 验收矩阵

### A. 计时训练

| 项目 | 状态 | 结论 |
|---|---|---|
| 创建计时训练计划 | Partial | 编辑页可生成内存态 `WorkoutPlan` 草稿；真实保存和编辑回填仍未接 repository。 |
| 当前是否要求添加动作 | Risk | 当前计时计划默认包含动作，添加动作也在编辑页内完成；用户反馈后续应更接近纯间歇计时器。 |
| 动作时间、休息时间、轮数、轮间休息 | Pass | 合同映射和 UI state 测试覆盖；E9.4 已支持临时清空并在空值时禁用保存/开始。 |
| 热身和放松设置 | Partial | 支持全局秒数 block；最后 N 秒提醒覆盖需继续回看。 |
| 临近结束提醒 | Partial | 动作与休息提醒已实现；热身、放松、轮间休息覆盖作为用户测试风险项。 |
| 暂停/继续/跳过/+15秒/结束 | Pass | 计时执行页和引擎测试覆盖。 |
| 总倒计时与当前阶段倒计时 | Pass | 执行 UI state 覆盖当前阶段、主倒计时和进度。 |
| 立即开始按钮状态 | Pass | E9.4 已接通计时编辑页 `立即开始`，有效草稿会进入计时训练执行页；无效草稿保持禁用并显示原因。 |

### B. 基础跟练

| 项目 | 状态 | 结论 |
|---|---|---|
| 跟练入口 | Pass | 首页和跟练选择页可进入基础 preset。 |
| 是否支持选择动作 | Partial | 只支持内存态 preset；不支持用户自由选择动作。 |
| 是否支持推荐动作 | Deferred | 需要 E10 训练模式边界重构后设计。 |
| 是否支持动作排序 | Deferred | 当前不支持自定义排序。 |
| 热身、动作、休息、轮数、放松 | Partial | 复用计时 `TimedCircuitBlock`；当前 preset 不是完整课程编排器。 |
| 用户反馈 | Product Decision | 跟练后续应承担动作选择、编排、推荐和动作内容展示，动作选择应进入独立页面。 |

### C. 力量训练

| 项目 | 状态 | 结论 |
|---|---|---|
| 创建力量计划 | Partial | 编辑页可生成内存态 `WorkoutPlan` 草稿；真实保存和编辑回填仍未接 repository。 |
| 动作选择 | Partial | 可从首批 fixture 添加力量动作，但仍嵌在编辑页横向 chip 中。 |
| 独立动作选择页 | Deferred | 归入 E10；当前不实现。 |
| 动作顺序编排 | Partial | 默认顺序按加入顺序，未提供完整拖拽/独立排序页。 |
| 目标重量、次数、组数、休息 | Pass | 合同映射和测试覆盖。 |
| 热身组、逐组目标 | Pass | UI state 支持热身组与逐组目标展开。 |
| 开始力量训练按钮状态 | Pass | E9.4 已接通力量编辑页 `开始力量训练`，有效草稿会进入力量训练执行页；无效草稿保持禁用并显示原因。 |
| 开始本组、完成本组、确认实际重量/次数/感受 | Pass | 引擎与 UI state 测试覆盖，计划值会预填实际记录。 |
| 替换动作、跳过动作 | Pass | E4.4 引擎与 UI state 覆盖，记录替换来源和跳过摘要。 |

### D. 数字输入

| 字段 | 状态 | 结论 |
|---|---|---|
| 时间输入清空 | Pass | E9.4 计时编辑页热身、动作秒数、动作后休息、轮间休息和拉伸均以 raw text 草稿支持临时为空。 |
| 重量输入清空 | Pass | E9.4 力量计划重量以 raw text 草稿支持临时为空；带重量动作为空时禁用保存/开始并提示重量不能为空。 |
| 次数输入清空和区间顺序 | Pass | E9.4 力量计划次数区间、固定次数和逐组次数均支持临时为空；Review Gate 修复已覆盖可见区间 `12-8` 禁用保存/开始并提示最大次数不能小于最小次数。 |
| 组数输入清空 | Pass | E9.4 力量计划正式组数和热身组数均支持临时为空，空值时禁用保存/开始并显示原因。 |
| 轮数输入清空 | Pass | E9.4 计时计划轮数支持临时为空，空值时禁用保存/开始并显示原因。 |
| 清空后继续输入 | Pass | E9.4 UI state 测试覆盖清空后继续输入有效数字并恢复保存/开始。 |
| 非法输入提示 | Pass | 计划编辑页保存/开始区域展示第一条明确校验原因；力量确认层原有空值校验未回退。 |

**E9.4 修复结果:** 计划编辑数字输入已改为 raw text 草稿 state，空字符串会留在输入框中；保存/立即开始时再要求合法数字，并在预览区展示明确原因。

### E. 记录与数据分析

| 项目 | 状态 | 结论 |
|---|---|---|
| 历史记录列表 | Partial | 有示例 / 内存态历史列表。 |
| 单次记录详情 | Partial | 可展示单次记录详情和摘要。 |
| 力量趋势 | Partial | 有单动作重量/次数历史和训练容量基础趋势。 |
| 总统计 | Deferred | 尚无完整总统计面板。 |
| 图表 | Deferred | 当前为文字/行式基础趋势，不含图表。 |
| 计划级多次趋势 | Deferred | 需要真实 session records 和计划快照持久化后实现。 |
| 记录清理 | Deferred | 后续需要支持全部清除、按训练计划清除、按日期清除；当前历史仍多为内存态 / fixture / 基础展示，本轮不实现假删除。 |
| 用户反馈 | Product Decision | 后续 E12 需要总统计、图表、平均心率趋势和计划调整证据。 |

### F. 心率

| 项目 | 状态 | 结论 |
|---|---|---|
| 心率占位状态 | Pass | disabled / not connected / connecting / available / stale / error 状态映射已实现。 |
| 未接真实设备说明 | Pass | E9.2 文案和测试覆盖。 |
| 无医疗/危险/强度判断 | Pass | `warningLevel` 不驱动训练中断或医疗化告警。 |
| 后续方向 | Deferred | E11 评估手环、手表、Health Connect、BLE 和设备策略。 |

### G. 通知、声音、隐私

| 项目 | 状态 | 结论 |
|---|---|---|
| 计划提醒普通通知 | Pass | 普通通知 contract、权限关闭边界和 manifest 测试覆盖。 |
| 活跃训练通知 | Pass | 普通 ongoing 摘要，不是 foreground service。 |
| 通知权限关闭不阻塞训练 | Pass | 文案和测试覆盖。 |
| 声音提醒 | Partial | 倒计时反馈 request 边界存在；实际设备表现需用户测试。 |
| 不 duck / 不打断其他 App 音乐视频 | Partial | 代码测试确认不请求 audio focus / ducking；真机仍需用户测试记录。 |
| 权限与隐私文案 | Pass | E9.2 已收口，manifest 仅含 `POST_NOTIFICATIONS`。 |

### H. UI skin

| 项目 | 状态 | 结论 |
|---|---|---|
| Official Flow | Pass | 默认皮肤，符合 `DESIGN.md`。 |
| Tile Flow | Pass | 关键页面磁贴式皮肤已接入。 |
| Big Type | Pass | 首页和两类执行页大字能力已接入。 |
| 三套 skin 切换保存 | Pass | DataStore 偏好和 registry fallback 测试覆盖。 |
| 720x1280 小屏主控制 | Partial | 单元测试覆盖 token 和控制可达；模拟器 smoke 结果见第 6 节。 |
| 关键文案不溢出 | Risk | 需要用户测试继续观察，尤其是计划编辑和确认层长中文。 |

### I. 状态恢复

| 项目 | 状态 | 结论 |
|---|---|---|
| 暂停后继续 | Pass | 计时和力量引擎测试覆盖。 |
| 进后台再回前台 | Partial | Activity 未被杀死时依赖内存态 route；普通通知只显示摘要。 |
| Activity 重建 / 旋转 | Risk | 当前未建立真实 session state 持久化或 SavedState 恢复闭环。 |
| 进程被杀 | Deferred | 当前不支持，需真实 `WorkoutSession` 持久化和后台策略。 |

### J. MVP 非目标确认

| 项目 | 状态 | 结论 |
|---|---|---|
| 真实设备心率 | Out of Scope | 不支持。 |
| Health Connect / Wear OS / BLE | Out of Scope | 不支持。 |
| 语音教练 | Out of Scope | 不支持。 |
| 真实云同步账号 | Out of Scope | 不支持。 |
| 后台可靠计时 | Out of Scope | 不支持。 |
| 医疗判断 | Out of Scope | 不支持。 |
| 大规模课程平台 | Out of Scope | 不支持。 |
| 远程皮肤插件市场 | Out of Scope | 不支持。 |

## 4. 已发现问题清单

| ID | 分级 | 类型 | 问题 | 复现/证据 | 建议 |
|---|---|---|---|---|---|
| E9.3-BUG-001 | P1 | Fixed in E9.4 | 计划编辑页整数输入无法清空，旧值回填 | `TimedPlanEditorUiStateTest.integerDurationFieldsCanBeTemporarilyBlankAndThenReentered`、`StrengthPlanEditorUiStateTest.strengthNumericFieldsCanBeTemporarilyBlankAndThenReentered` | 已改为 raw text 草稿 state + 保存/开始校验 |
| E9.3-BUG-002 | P2 | Fixed in E9.4 | 计时编辑页 `立即开始（E3 接入）` 灰色不可点 | `MvpAcceptanceChecklistEvidenceTest.editorStartButtonsAreConnectedInPlanEditorRoutes`、`OfficialShellStateTest.timedEditorDraftStartsTimedSessionDestination` | 有效草稿直接进入计时训练执行页 |
| E9.3-BUG-003 | P2 | Fixed in E9.4 | 力量编辑页 `开始力量训练（E4 接入）` 灰色不可点 | `MvpAcceptanceChecklistEvidenceTest.editorStartButtonsAreConnectedInPlanEditorRoutes`、`OfficialShellStateTest.strengthEditorDraftStartsStrengthSessionDestination` | 有效草稿直接进入力量训练执行页 |
| E9.4-REVIEW-001 | P1 | Fixed in E9.4 Review Gate | 力量次数区间可见 `min=12/max=8` 时仍可能保存/开始 | `StrengthPlanEditorUiStateTest.visibleRepRangeWithMaxBelowMinDisablesSaveAndStartUntilCorrected` | 不再在输入中静默修正；按可见 raw text 校验并阻止无效草稿映射 |
| E9.4-REVIEW-002 | P2 | Fixed in E9.4 Review Gate | 编辑页文案仍暗示执行接入是后续状态 | `MvpAcceptanceChecklistEvidenceTest.editorHeaderCopyMatchesCurrentStartAndSaveState` | 计时/力量编辑页均说明可直接开始当前草稿，也可保存后进入计划详情 |
| E9.3-PD-001 | Product Decision | Product Decision | 计时训练是否应改为纯间歇计时器 | 用户反馈；当前计时训练强依赖动作 | E10 重构训练模式边界 |
| E9.3-PD-002 | Product Decision | Product Decision | 基础跟练是否应承担动作选择、推荐和排序 | 用户反馈；当前只支持 preset | E10 设计跟练编排 |
| E9.3-PD-003 | Product Decision | Product Decision | 动作选择是否独立成页面 | 用户反馈；当前嵌在入口/编辑页 chip | E10 统一动作选择页 |
| E9.3-RISK-001 | P2 | Risk | 热身、放松、轮间休息最后 N 秒提醒覆盖需要真机/模拟器继续观察 | 当前核心覆盖动作与休息，文档要求用户测试回看 | 用户测试记录具体阶段 |
| E9.3-RISK-002 | P2 | Risk | 提示音与其他 App 音频共存需真机验证 | 单元测试确认不请求 audio focus / ducking，但设备差异未知 | 用户测试记录设备、Android 版本和音频 App |
| E9.3-RISK-003 | P2 | Risk | 记录页缺总统计、图表和计划级趋势 | 当前仅基础历史/趋势 | E12 数据分析趋势 |
| E9.3-RISK-004 | P2 | Risk | Activity 重建和进程死亡恢复不完整 | E9.1 已明确边界 | 后续持久化与后台策略 story |
| E9.4-DEFER-001 | P2 | Deferred | 历史记录清理需要全部清除、按训练计划清除、按日期清除 | 当前历史记录仍多为内存态 / fixture / 基础展示 | 后续在真实 session records / repository 闭环后实现 |

## 5. 用户测试前告知

- 当前 APK 适合验证首页入口、计划编辑、计划详情启动、计时训练执行、力量训练执行、基础跟练 preset、历史/恢复/设置/skin 文案和整体可用性。
- 测试者需要知道：计划、历史、恢复和部分记录仍多为内存态、fixture 或基础展示。
- 测试者不要把心率占位理解为已接真实设备，也不要把恢复建议当成医疗或康复建议。
- 如果测试提示音共存，请同时记录设备型号、Android 版本、音频 App、蓝牙/扬声器输出和是否出现 duck / 暂停 / 抢焦点。

## 6. 验证记录

| 项目 | 结果 | 备注 |
|---|---|---|
| `java -version` | Pass | OpenJDK 17.0.19。 |
| `gradlew --version` | Pass | Gradle 9.4.1。 |
| Android SDK / adb / emulator | Pass | `.local/android-sdk` 可用；AVD: `TrainFlow_Pixel_API_36`。 |
| `app:testDebugUnitTest` | Pass | 2026-06-07 执行通过。 |
| `app:assembleDebug` | Pass | 2026-06-07 执行通过。 |
| `app:lintDebug --no-daemon --console=plain` | Pass | 2026-06-07 执行通过。 |
| `app:check --no-daemon --console=plain` | Pass | 2026-06-07 执行通过。 |
| `git diff --check HEAD` | Pass | 提交前执行通过。 |
| `git diff --cached --check` | Pass | stage 后执行通过。 |
| 模拟器 smoke | Not run | E9.4 本轮未执行模拟器 smoke；行为由 UI state / shell tests、assemble、lint 和 check 覆盖。 |
| 用户测试 APK | Pass | `.local/deliverables/TrainFlow-e9.4-fix-pack-1-debug.apk`；构建时间 `2026-06-07T03:38:25.8690520+08:00`；SHA-256 `8E8F07E45B1F0D56B1AC07AB5AA2736FD87E8417B2EC0819395C5422D14F4997`；commit hash 以最终提交为准。 |

## 7. 后续建议

1. Review Gate：先审 E9.4 修复是否满足用户测试前 P1/P2 收口。
2. 用户测试：带着上方告知范围发 debug APK。
3. E10：训练模式边界重构，重点处理纯计时器、跟练编排和统一动作选择页。
4. E11：心率设备策略，评估 Health Connect、Wear OS、BLE 和非医疗化提示。
5. E12：数据分析趋势，补总统计、图表、计划级趋势、平均心率趋势、计划调整证据和记录清理能力。
