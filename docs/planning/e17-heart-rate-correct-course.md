# E17 心率子系统 Correct-course 计划

**状态：** E17-0 implemented / needs review；E17-1 至 E17-4 locked
**日期：** 2026-07-12
**性质：** 产品、设备、架构、测试与 implementation-readiness 重新规划

## 1. E17 目标

E17 基于 E16 遗产，为小型本地优先训练 App 重新设计合适的心率子系统。从产品价值、Band 9 / 标准 HRS 可行性、权限、BLE runtime、状态模型、测试层级和真机验收重新对齐，不把旧 E16 实现视为默认正确。

E17 将浮动胶囊视觉与互动作为 `adopted / frozen / direct reuse` 资产直接采用，优先稳定、可解释、用户可见、手动可恢复；自动重连不是默认前置。

## 1.1 历史归档与权威层级

E16 原始代码及 testing、planning、design 文档整体为 `sealed historical archive / reference only`。E17 不再为当前状态逐行修改这些材料；其中历史语境下的 current、next、dependency / depends on、unlocked、in progress 不属于当前指令，不参与 E17 当前状态逐行一致性检查，不能生成任务、解锁 Story 或覆盖 D-079。Review 不得仅因这些历史措辞保留而提出当前状态 finding；未经新的明确 correct-course decision，不得修改 sealed archive。sealed 不删除 Git 历史或分支，不否认历史测试与 Band 9 / HRS 证据，也不把旧实现变成 E17 合同或自动复用旧 production 架构。

文档权威层级为：`AGENTS.md` > accepted superseding decision D-079 > 当前 E17 权威文档（project-status、E17 correct-course、readiness、roadmap、architecture）> E16 retrospective / supersession 索引 > sealed E16 原始文档与代码（historical evidence only）。低层不得覆盖高层。

## 2. 非目标

- correct-course 阶段不写 production 代码。
- 不直接进入自动重连。
- 不进入心率记录、分析、医疗告警或训练控制。
- 不重做胶囊视觉、布局、拖动或动画。
- 不构建通用 BLE 框架。
- 不复制一套 Android GATT 对象模型。
- 不为尚未确认的设备或厂商做过度抽象。
- 不把旧 E16 Story / SHA 当作 E17 解锁前置。

## 3. 重新规划阶段

### E17-0：Retrospective / legacy audit / reset

- 正式关闭 E16 历史支线并记录 supersession。
- 只读审计 main 与失败分支，分类 freeze / revalidate / reference-only / rewrite / reject。
- 重置 project status、decision、roadmap、readiness、architecture 和提示词流程。
- 状态：`implemented / needs review`。
- 下一步：独立 E17-0 Code Review；E17-1 仍 locked。

### E17-1：Band 9 与标准 HRS 重新复验

- 重新确认广播条件、`0x180D`、`0x2A37`、CCCD、notify、payload、identifier 变化和 Huawei Health 互斥。
- 只做设备与协议可行性复验，不形成 production provider 架构。
- 真机截图、日志、APK 与设备输出只进入 `.local/`；可追溯结论进入文档。
- 真实操作由用户执行；开发对话只准备 APK、日志入口和清单。
- 当前状态：`planned / locked`。

### E17-2：重新定义产品范围

- 决定 opt-in、权限、隐私、未训练显示、saved device、手动恢复、状态语义、记录边界和自动重连价值。
- 明确哪些 E16 产品结论保留、修改或拒绝。
- 自动重连若有价值，只能形成独立 decision 与独立后续 Story。
- 当前状态：`planned / locked`。

### E17-3：最小技术架构

- 设计原生 GATT 所有权、callback 串行化、permission failure、scan / connect / close、状态事实与 presentation 分离和测试 seam。
- 架构必须与小型 App 体量匹配，默认优先直接使用 Android BLE 类型。
- 明确 callback ownership 后才能切 implementation Story。
- 当前状态：`planned / locked`。

### E17-4：Implementation readiness

- 对齐产品、设备证据、架构、胶囊状态接线、测试矩阵和 Story 拆分。
- readiness 未通过不得写 production 代码。
- 当前状态：`planned / locked`。

### E17 后续 implementation Story

当前只允许记录 provisional 方向，例如 parser / platform boundary、用户手动 scan-connect、presentation mapping 和独立可选 reconnect。E17-0 不锁定最终编号、文件、接口或实现细节；只有 E17-4 reviewed / merged 后，主管理才能生成正式 implementation Story。

## 4. 测试分层

| 层级 | 负责验证 | 明确不能代表 |
|---|---|---|
| 纯 Kotlin 测试 | parser、policy、状态转换、presentation input/output | Android BLE wiring、真实设备 |
| Provider 边界测试 | 少量平台异常、资源关闭、生命周期与 callback ownership | 射频、真实 GATT 时序 |
| AVD | 权限、process、前后台、Bluetooth 状态、no-crash / no-ANR | Band 9 scan / connect / notify |
| Band 9 真机 | scan、connect、discover、CCCD、notify、断流、恢复 | 其他设备或厂商通用性 |

硬规则：

- 不模拟射频信号并把它当真实设备证据。
- injected callback 不是设备验证。
- AVD 不是 Band 9 验证。
- helper / 源码字符串搜索只可用于静态边界检查，不是 production behavior coverage。
- 文档只声明测试实际执行并断言过的行为。
- 人工真机操作由用户执行，开发对话准备 APK、日志与清单。

## 5. 设计和实现约束

- 默认优先直接使用原生 Android BLE 类型；Android 类型留在平台层。
- 不为测试创建第二套完整 GATT 模型。
- permission-sensitive 调用使用窄边界，并逐项定义异常语义。
- 不捕获全部 `RuntimeException` 或 `IllegalStateException`。
- 不使用异常 message / 厂商字符串分类平台状态。
- callback ownership、串行化上下文、旧 callback 拒绝和 close 顺序必须在 E17-3 明确。
- 自动重连必须有独立产品价值决策和独立 Story。
- freshness 与自动重连不得默认绑在同一 Story。
- Repair 不得为测试便利扩展 production 架构。

## 6. Adopted / frozen / direct reuse 胶囊资产

### 直接采用的实现与视觉资产

- `app/src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateFloatingCapsule.kt`
- `app/src/main/java/com/liujyks/trainflow/ui/shell/official/HeartRateCapsuleGeometry.kt`
- `app/src/main/java/com/liujyks/trainflow/ui/theme/TrainFlowMotionTokens.kt` 中已批准的相关 motion 语义
- `docs/design/e16-3-heart-rate-ui-html/index.html`

相关 E16 design 文档本身仍属于 sealed historical archive；D-079 从中点名采用的视觉与互动资产则是 E17 当前合同，不是 reference-only、revalidate、rewrite、provisional 或待重新设计。

### 冻结行为

- 胶囊视觉设计、布局和信息层级。
- collapsed / expanded 视觉。
- 轻点、拖动、movement threshold、左右安全边吸附。
- viewport clamp、安全区避让、expanded compact fallback、IME 收缩。
- 互动动画、边缘锚定、克制 motion 和 reduce-motion 兼容方向。

E17 不重新进行胶囊视觉设计、交互设计、HTML 视觉方案、动画探索、颜色 / 尺寸 / 布局 / motion 变体或 `huashu-design` 视觉重做。

### Runtime 可重新设计

- `HeartRateFloatingCapsuleState.kt` 中的 provider 语义、状态文案、mapper 和状态优先级。
- presentation state DTO 在不改变冻结视觉合同前提下的输入结构。
- state source、provider state、事实状态与 UI 状态的映射。
- `TrainFlowApp` 中的 runtime wiring 和 lifecycle ownership。
- settings copy、manual recovery flow 与 saved-device 语义（须经 E17-2 接受）。

E17 新 provider/runtime 通过胶囊外部 presentation mapper 适配现有胶囊；不让胶囊迁就 BLE 架构，不把 Android BLE 对象传入胶囊，不在胶囊内部实现 permission、scan、connect 或 reconnect。冻结的是视觉与互动，不是旧 runtime 设计；`HeartRateFloatingCapsuleState.kt` 中混入的旧 runtime 语义不自动继承。如现有 presentation DTO 无法表达 E17 接受的必要状态，必须另开兼容性 decision，不能借此修改视觉与互动。

## 7. E17 解锁规则

1. E17-0 独立 Code Review 通过。
2. E17-0 Story 分支以 `--no-ff` 合入 `main` 并推送，E17-0 immutable Story full SHA 成为 `main` ancestor。
3. 随后完成独立 E17-0 closeout docs-sync；该 docs-sync 必须独立开发、独立 Review、合入 `main` 并推送，其 immutable full SHA 成为 `main` ancestor。
4. `main` 与 `origin/main` 同步为 `0 0`，且 project-status、roadmap、readiness 与本计划状态一致后，E17-1 才可解锁。
5. E17-1 reviewed / merged，并完成该 Story 明确要求的 Band 9 真机复验后，才可开始 E17-2。
6. E17-2 reviewed / merged 后才可开始 E17-3。
7. E17-3 reviewed / merged 后才可开始 E17-4。
8. E17-4 implementation readiness reviewed / merged 且结论通过后，才能创建正式 production implementation Story。

每一关都必须：

- 使用 prerequisite 的 immutable full commit SHA。
- `git merge-base --is-ancestor <full-sha> main` 成功。
- `main` 与 `origin/main` 同步为 `0 0`。
- Git ancestry 与 project-status / roadmap / readiness 一致。

push、Review 文本、人工测试或分支存在都不等于已合入。E16 分支、E16 Story tip 和 `89d1e23f870185a2e279d35bb293883f64fe70ba` 均不得作为 E17 解锁前置。

当前仍为 E17-0 `implemented / needs review`，E17-1 至 E17-4 `locked`。下一步只能是 E17-0 独立 Code Review；不得开始 Band 9 / HRS 复验，不得开始 E17 产品、架构、readiness 或 production 实现。E17-0 merge 本身不会立即解锁 E17-1，必须先完成上述独立 closeout docs-sync 全部门禁。

## 8. E17-0 验收

- E16 历史 merge fact 被保留，umbrella 被 correct-course 关闭。
- E16-10b-2 保持 `changes requested` 且永久禁止合并。
- 胶囊 `adopted / frozen / direct reuse` 范围在 retrospective、E17 计划、decision log 与 project-status 一致。
- E16 原始代码与文档作为 sealed historical archive，不参与 E17 当前状态逐行一致性检查。
- E17-0 merge 后仍须独立 closeout docs-sync reviewed / merged / ancestry / sync / docs consistency 全部通过，才能解锁 E17-1。
- E17-1 至 E17-4 全部 locked。
- E17 implementation readiness 明确未通过。
- 通用提示词流程包含连续 Review、Repair 结构、最小修改、Evidence、体积控制与 correct-course 职责门禁。
- 本 Story 只修改 Markdown / 根目录提示词模板，不修改生产或测试代码。
