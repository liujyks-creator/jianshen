# E10 训练模式边界与执行页交互计划

**状态:** E10.1 决策记录；E10.2-E10.9 已落地；E10.9 用户测试反馈计划已记录；E14.6-2 完成复盘页边界已补充
**日期:** 2026-06-13
**范围:** 产品边界、执行页交互原则、后续 story 拆分、用户测试反馈分流、E10.2-E10.9 实现结果、Timer Dial 视觉修复 / 计划保存 / huashu 原型 / 音频 / 统计后续边界，以及 E14.6-2 completed terminal recap page 边界
**仍不包含:** 本轮不实现生产 Timer Dial 修复、计划保存、huashu 原型、心率设备、语音、TTS、音频资源接入、统计图表、力量 / 跟练完整新版 UI

## 1. E10.1 结论

E10.1 只记录 TrainFlow 在用户测试后的训练模式边界调整和后续计划。当前 Android 代码、训练引擎、数据契约语义和 UI 实现不在本 story 中修改。

已接受的方向是：

1. 计时训练回归“纯间歇计时器”，不再绑定动作库。
2. 跟练训练和力量训练共用后续统一动作选择页。
3. 三类训练执行页都必须遵守“主操作即时可达”的执行页原则。
4. 记录、心率、声音、语音和统计分析都按 E10.4/E11/E12/E13 或后续阶段推进，不在 E10.1 伪实现。

## 2. 计时训练模式边界

### 2.1 新边界

计时训练应回归纯间歇计时器，服务用户快速创建按阶段推进的训练节奏。它不再表达“从动作库选择动作并按动作教学执行”的训练模式。

计时训练计划由阶段组成，阶段类型包括：

- 热身。
- 工作。
- 休息。
- 放松。
- 自定义阶段。

每个阶段至少支持：

- 阶段名称。
- 阶段时间。
- 阶段图标。
- 阶段颜色。

阶段图标是阶段类型或状态图标，例如热身、工作、休息、放松、自定义，不是动作内容指导，也不承诺动作教学、动作推荐或动作详情。

### 2.2 编辑能力

计时训练编辑页后续应支持：

- 添加阶段。
- 复制阶段。
- 删除阶段。
- 拖动排序阶段。
- 后续修改训练计划主题色或配色；E10.2 先补阶段颜色入口。

计时训练不进入动作选择页，不提供动作库筛选、动作详情、动作推荐或动作内容提示。

### 2.3 视觉参考边界

用户提供的大圆盘 UI 可作为 E10.2 计时训练执行页的视觉参考，但不得逐像素照搬。E10.2 需要转化为 TrainFlow 自己的视觉语言，使用现有 `DESIGN.md` 设计原则、官方 skin token 和训练执行页信息层级，避免版权和风格依赖风险。

## 3. 计时训练执行页交互原则

计时训练执行页后续以大圆盘作为核心视觉和主控制区。

执行页信息层级：

1. 顶部显示总剩余时间，但不喧宾夺主。
2. 中心显示当前阶段图标、阶段名称或编号、当前阶段倒计时。
3. 圆环显示整体进度、当前阶段进度、轮次或阶段位置。
4. 心率、提示、下一阶段信息低于当前阶段、时间和主控制。

核心交互：

- 点击中心圆盘可暂停或继续训练。
- 暂停时必须记录暂停时长。
- 结束训练即时可达，但需要二次确认防误触。
- 跳过或下一阶段控制应在训练中即时可达。
- 最后 N 秒支持屏幕闪烁或动画提醒，也支持声音提醒。

训练记录后续应能区分：

- 本次总耗时。
- 有效训练时间。
- 暂停总时长。

语音 cue 边界：

- 当前阶段开始可预留固定阶段词 cue，例如 `warm up`、`work`、`rest`、`cool down`。
- 第一版不做用户自定义文字 TTS。
- 第一版不做自动语音教练或任意文本朗读。

## 4. 统一执行页原则

所有训练执行页都遵守以下原则：

1. 训练中的主操作必须即时可达。
2. “看到时间或动画的位置，就是可以控制训练节奏的位置”。
3. 暂停/继续、结束、跳过/下一步、开始本组、确认本组等关键操作不能藏到需要滚动才能找到的位置。
4. 结束训练可以即时可达，但必须有二次确认。
5. 心率、说明、提示、下一步信息都低于当前动作/阶段/时间/主操作层级。
6. 力量训练和跟练训练后续也必须遵守该原则，但 E10.1 不重做其完整 UI。

E10.3 只修复力量训练和跟练训练中主操作可达性，不做完整新版力量 UI 设计。

E10.3 已按该原则落地：力量和基础跟练执行页都保留现有视觉系统，仅把主控制固定到即时可达位置，并让当前时间 / 倒计时主区域承担暂停 / 继续入口。

## 5. 跟练/力量统一动作选择页

统一动作选择页的目标是避免入口页和编辑页过于拥挤。动作库能力应集中在专门的选择流中，而不是塞在训练入口或编辑页第一屏。

计时训练不进入统一动作选择页。

跟练训练使用统一动作选择页，至少包括：

- 搜索。
- 分类。
- 推荐。
- 动作详情预览。
- 多选。
- 已选动作顺序管理。

力量训练使用统一动作选择页，选择动作后回到力量编辑页设置：

- 重量。
- 次数。
- 组数。
- 休息。

入口页和编辑页只展示已选结果和核心参数，不堆大量动作列表。

跟练训练后续结构应类似计时训练，但动作来自动作选择页：

- 热身。
- 动作。
- 休息。
- 轮次。
- 放松。

## 6. 用户测试反馈分流

| 反馈 | 分流 | 处理原则 |
|---|---|---|
| 完成一次计时训练后，记录页没有出现今天记录；当前记录仍可能是 fixture / 内存态 / 未真实持久化闭环。 | E10.4 或 E12 前置项 | 先完成真实 `WorkoutSession` 写入记录闭环，再做统计和删除；不得继续用假记录暗示真实持久化。 |
| 主页面和文案中的旧 E5 记录 / 建议边界提示。 | E10.4 已清理 | 保持文案与真实记录闭环一致，避免用户以为未接入或把恢复建议误读为完整分析。 |
| 需要测试数字、时间、次数、轮次、总时长、有效时长、暂停时长是否准确。 | E10.2/E10.3/E12 回归项 | 计时大改、力量主控修复和真实统计前都要补充数字准确性验证。 |
| 新增手动心率输入需求：真实设备接口要保留，但先允许用户手动录入心率，从而进入记录和分析。 | E11 | 已被 E11.3 反向收口：手动输入导致执行页 UI 变形，首版放弃心率显示、录入和统计；设备接口只保留未来边界。 |
| 心率真实设备、Health Connect / Wear OS / BLE 后续新开 E11 或独立阶段。 | E11 或独立健康设备阶段 | 进入前重新确认权限、数据来源、非医疗文案和设备支持边界。 |
| 数据分析后续需要总统计、图表、计划趋势。 | E12 | 依赖真实记录闭环和可比较数据分组，不先做静态图表；平均心率趋势已随 E11.3 撤销。 |
| 分析时要比较同类数据：同一计划、同一阶段、同一轮次或同一动作；不要把某天第一轮和另一天最后一轮直接比较。 | E12 | 统计口径先定义，再实现趋势和图表。 |
| 手机连接蓝牙耳机时没有听到声音提示。 | E13 | 声音系统 story 必须覆盖手机扬声器与蓝牙耳机 smoke，记录设备 / Android 版本差异；本轮不接入声音播放代码。 |
| 用户希望使用根目录本地 `countdown_beep1.mp3` 作为最后 N 秒提醒。 | E13 | 可作为 5 / 4 / 3 / 2 / 1 等最后 N 秒 beep 素材候选；执行 story 接入前确认授权、目标资源路径和偏好开关。 |
| 用户确认 `.local/audio/stage_bell_copper_clean.mp3` 作为下阶段开始铃声。 | E13 | `.local` 原文件当前不能提交；如果后续接入 App，由执行 story 复制到 `app/src/main/res/raw/` 并只提交 rights-cleared app resource。 |
| 声音提醒不应降低、暂停或打断其他 App 的音乐/视频声音。 | E13 | 音频共存是验收项；短提示音不得主动 ducking 或抢占会打断外部音频的 focus。 |
| 后续需要悦耳、有磁性的女性人声 cue；第一版只做固定阶段词，不做用户任意文本 TTS。 | E13 | 先做固定阶段词和素材策略；任意文本 TTS、自动语音教练继续不进第一版。 |
| 自定义计时训练阶段、秒数、轮次、颜色、图标等好像不能保存，退出后恢复默认。 | E10.10 | 计划保存持久化 story 需要覆盖计时计划结构恢复；不能继续把保存入口表现成真实持久化但只保存在内存态。 |
| 各种计划保存入口都需要检查是否真实可用。 | E10.10 | 统一 audit 计时、力量、跟练计划保存入口；不可用入口改为禁用、明确待实现或进入对应 story。 |
| Timer Dial 需要减少文字，去掉“总剩余”、下一阶段提示框、已启用声音提示框等。 | E10.9 Review Fix / User Test Fix | 视觉修复只处理 Timer Dial 呈现，不接入音频，不改计划保存。 |
| 总剩余时间应更大、更居中，圆盘整体应更大。 | E10.9 Review Fix / User Test Fix | 保持中心倒计时为最高运动中信息，总剩余时间提高可读性但不抢主控制。 |
| 外圈 / 内圈线条应同比例更细，避免 marker 和外圈视觉重叠。 | E10.9 Review Fix / User Test Fix | 调整弧线和 marker 层级，保留 E10.8/E10.9 progress 语义。 |
| 需要增加内圈总进度线下方的宽底层圆环。 | E10.9 Review Fix / User Test Fix | 宽底层圆环只做承托，不产生假进度。 |
| 底层圆环上的浅色小点必须复用内圈阶段 marker 的动态角度计算。 | E10.9 Review Fix / User Test Fix | 阶段数量、时长、轮次变化时小点必须跟随变化；禁止固定装饰点。 |
| 中心圆只保留图标、必要编号 / 时间，填充用阶段预设色，文字 / 图标用白色。 | E10.9 Review Fix / User Test Fix | 减少“阶段01”“训练”等解释性文字，保持白色内容对比度。 |
| 后续用 `huashu-design` skill 做 Timer Dial 高保真 HTML 原型探索。 | E10.11 | 原型 story 覆盖黑红高对比、TrainFlow Official 融合、赛博霓虹，以及 active / rest / paused / final 5 seconds / rest extension；本轮不生成原型。 |
| 历史记录后续需要清理功能：全部清除、按训练计划清除、按日期清除；不要在没有真实持久化前做假删除。 | E12 或持久化闭环后续项 | 真实持久化完成后再实现真实删除与确认流程。 |

## 7. 后续阶段拆分

### E10.2 计时训练编辑页与执行页重做

目标：把计时训练改为纯间歇计时器。

状态：E10.2 已在 Android 生产代码首版实现。计时训练编辑页不再读取动作 fixture 或选择动作库动作，改为阶段名称、阶段时间、阶段类型、图标 key 和颜色的纯阶段列表；支持添加、复制、删除、右侧手柄长按拖拽排序，并保留上移 / 下移作为备用和无障碍排序路径。拖拽只由阶段行右侧明确手柄触发，热身固定在开头，放松固定在最后，中间的工作、休息和自定义阶段可排序。阶段卡已提供内置颜色 swatch，阶段类型选择会同步图标 key；计划主题色 / 整体配色编辑仍保留为后续 polish。计时执行页已改为大圆盘主视觉，顶部显示总剩余时间，中心显示阶段图标 key、阶段名称和当前倒计时，圆环表达整体进度与当前阶段进度，点击中心圆盘可暂停 / 继续。`TimedWorkoutEngineState.pausedElapsedSec` 已记录本次执行中的暂停累计时长，供 E10.4 真实记录闭环使用。

范围：

- 阶段模型和 UI 映射：热身、工作、休息、放松、自定义。
- 阶段名称、时间、图标、颜色。
- 添加、复制、删除、拖动排序。
- 阶段颜色入口；计划主题色或整体配色编辑保留为后续 polish。
- 大圆盘执行页。
- 圆环整体进度、当前阶段进度、轮次或阶段位置。
- 点击中心暂停/继续。
- 暂停时长记录。
- 最后 N 秒屏幕动画/闪烁和声音提醒。
- 固定阶段词 cue 预留。

E10.2 已确认未实现：

- 未实现真实 `WorkoutSession` 持久化或记录页真实写入。
- 未实现 Health Connect / Wear OS / BLE / 厂商手环 SDK。
- 未实现语音、TTS、固定女声 cue、音频资源或统计图表。

禁止：

- 不绑定动作库。
- 不做动作选择、动作详情或动作推荐。
- 不逐像素照搬外部参考 UI。
- 不实现真实记录持久化、心率设备、TTS 或统计图表。

### E10.3 力量/跟练执行页主操作可达性修复

目标：让训练中主操作固定且即时可达。

状态：E10.3 已在 Android 生产代码首版实现。力量执行页保留现有深色执行页和确认层，不做完整新版力量 UI；开始本组、完成本组、确认本组、休息中提前开始本组、暂停 / 继续和结束训练进入固定底部控制区，当前动作 / 本组耗时 / 休息倒计时主面板可点击暂停或继续。基础跟练执行页改为滚动内容加固定底部控制区，暂停 / 继续、跳过 / 下一步和结束训练不再藏在页面底部，当前倒计时区域可点击暂停或继续。两类执行页的结束训练都会先显示二次确认，确认后才分发 `WorkoutCommand.EndSession(reason = "user_requested")`。

范围：

- 力量训练开始本组、完成本组、确认本组、暂停/继续、结束训练即时可达。
- 跟练训练暂停/继续、跳过/下一步、结束训练即时可达。
- 结束训练保留二次确认。
- 心率、提示、下一步、动作说明保持辅助层级。
- 保持 Official Flow / Tile Flow / Big Type 三套 skin 的主控制 metadata，不新增第四套 skin。

禁止：

- 不完整重做力量训练新版 UI。
- 不重写力量或跟练训练引擎。
- 不改变力量记录确认语义。
- 不实现真实记录持久化、心率设备、语音、TTS、音频资源、统计图表或完整跟练平台。

### E10.4 或后续 训练记录闭环

目标：完成训练后写入真实记录，为 E12 统计做前置。

状态：E10.4 已完成 Review Gate PASS 并合入 `main`。TrainFlow 已具备本地真实 session record write-through；计时、力量和基础跟练 completed / abandoned 终态可写入 Room session records，记录页生产入口读取真实本地记录。后续 Timer Dial 工作不再处理记录闭环，不改 Room、DAO、session repository 或记录页数据源。

范围：

- 完成训练后生成真实 `WorkoutSession` 记录。
- 保存完整 MVP 计划快照 blocks，至少覆盖计时阶段/轮次/休息结构、力量动作/计划组/目标/休息结构、preferences/cueSettings 和 followAlong 元数据；历史详情用恢复后的 snapshot 计算计划步骤/组数。
- 区分计划快照、实际执行、暂停时长、有效训练时间和总耗时。`totalElapsedSec` 使用 startedAt 到 endedAt 的 wall-clock 总耗时，力量 prepare / confirm 停留只进入 total；`effectiveElapsedSec` 不包含暂停，力量当前只包含正式组与休息推进；`pausedElapsedSec` 单独记录暂停累计。
- 让记录页显示本次完成的真实训练。
- completed / abandoned 终态写库使用一次性 guard 和异常吞并边界，避免 route 重组重复插入或 Room 异常直接打断 UI。

边界：

- 可与 E12 真实统计前置项协调。
- 在真实持久化前不做假删除或假长期趋势。
- 当前仍是本地 Room MVP，不承诺云同步、统计图表、历史清理或后台可靠计时。

### E10.5 Timer Dial 设计工作流与重构范围

目标：把用户喜欢的 Timer Dial 圆盘视觉语言重构为 TrainFlow 自己的设计语言，并明确工具路线、视觉规格、动效规格和后续实现拆分。

状态：E10.5 是 docs-only planning story，不实现生产 UI，不写 Kotlin，不改 Gradle，不改 prototype，不改记录闭环。

参考边界：

- 外部 APK / 截图只用于观察 UI / 交互和节奏，不复制代码、资源、图标、字体、音频、专有动画或逐像素视觉。
- APK 静态 / 运行观察只做研究记录，APK、截图、录屏、反编译输出和临时研究产物不得提交。
- 目标是 TrainFlow 自己的圆盘语言。

工具路线：

- Figma：静态界面、风格方案、颜色 / 组件 / 布局规格。
- HTML / Canvas：可选，用于快速验证圆盘动画和阶段弧线节奏。
- Jetpack Compose Canvas：最终 Android 生产实现方式。
- Rive / Lottie：只适合小图标或装饰动效，不用于核心计时进度，因为进度必须实时绑定 engine state。
- APK 静态 / 运行观察：只做研究记录，不提交产物。

视觉规格：

- 顶部显示本次训练总剩余时间。
- 外圈表达本轮或当前运动结构：`work` / `rest` / `warmup` / `cooldown` 用不同颜色区分，`work` 弧线更粗，`rest` 弧线更细，当前阶段弧线有进度动画。
- 内圈表达整次训练总进度。
- 中心圆表达当前阶段：阶段图标、阶段编号或名称、当前阶段倒计时，并作为点击暂停 / 继续入口。
- 底部只保留少量图标操作：重置、跳过 / 下一阶段、结束。
- 文字尽量少，用图标、颜色、粗细和层级表达信息。

动效规格：

- 阶段弧线推进。
- 总进度弧线推进。
- `work` / `rest` 切换时颜色和粗细变化。
- 阶段切换动效。
- 暂停态动效。
- 最后 N 秒提醒动效。
- 所有动效必须来自 TrainFlow engine state，不允许视觉假进度。

主题与风格：

- 黑红高对比作为参考方向。
- 后续可探索赛博霓虹、Official Flow、Tile Flow、Big Type 适配。
- 不新增第四套 skin。
- 先定义圆盘语言，再讨论如何融入 TrainFlow 风格。

后续拆分：

- E10.6 Timer Dial Figma / static visual variants。
- E10.7 Timer Dial Compose prototype。
- E10.8 Timer Dial production integration and animation polish。
- E13 声音 / 女声 cue 与视觉提醒联动。
- E12 统计图表 / 历史趋势，不混入 E10.5。

### E10.6 Timer Dial Figma / static visual variants

目标：输出 Timer Dial 静态视觉方案和规格，先让 Official Flow 的执行页和计时编辑页关键状态可评审，再把 Tile Flow / Big Type 作为适配规则交给 E10.7 / E10.8。

状态：E10.6 已记录到 `docs/planning/timer-dial-static-visual-variants.md`，仍为 docs-only design specification，不实现 Android，不写 Kotlin，不改 Gradle，不改 prototype，不开始 E10.7。

执行页静态帧：

- active work。
- active rest。
- warmup / cooldown。
- paused。
- resume transition。
- stage transition。
- last-N-seconds cue。
- completed。
- abandoned。
- end confirmation。
- 720x1280 小屏状态。

计时编辑页静态帧：

- 编辑页 header。
- 阶段列表。
- 阶段行 / 阶段卡两种方案。
- 添加阶段 sheet。
- 复制阶段。
- 删除确认。
- 颜色选择 picker。
- 图标选择 picker。
- 快捷时长选择。
- 时长细调，stepper / input 必须存在，滑块只能作为辅助。
- 展开 / 收起。
- 拖动排序。
- 上移 / 下移替代路径。
- 保存 / 取消反馈。
- 小屏底部操作不遮挡编辑内容。

规格边界：

- 顶部总剩余时间低于中心倒计时层级。
- 外圈表达本轮 / 当前运动结构，work / rest / warmup / cooldown 颜色区分，work 粗弧、rest 细弧。
- 当前阶段弧线、内圈总进度、暂停、完成、废弃和最后 N 秒提醒都必须来自 `TimedWorkoutEngine` / UI state / `WorkoutEvent`。
- 中心圆显示阶段图标、编号 / 名称、倒计时，并承担暂停 / 继续主交互。
- 底部只保留少量图标操作；结束训练必须二次确认。
- 不复制 APK 代码、XML、资源、图标、字体、音频、动画参数、命名或逐像素视觉。
- 不使用健身姿势动画或动作教学 animated SVG。
- 不新增第四套 skin，不混入 E11 / E12 / E13。

### E10.9 Review Fix / User Test Fix

目标：基于 E10.9 用户测试反馈做 Timer Dial 视觉减字和圆盘层级修复，同时保留已验证的计时语义。

范围：

- 移除或弱化执行页中不必要的文字：`总剩余` 标签、下一阶段提示框、已启用声音提示框等。
- 总剩余时间更大、更居中；中心倒计时仍是训练中的最高层级信息。
- 圆盘整体放大；外圈与内圈线条同比例变细，避免 marker 与外圈视觉重叠。
- 增加内圈总进度线下方的宽底层圆环。
- 底层圆环上的浅色小点复用内圈阶段 marker 的动态角度计算，随阶段数量、阶段时长和轮次变化而变化。
- 中心圆只保留图标、必要编号和当前阶段时间，减少“阶段01”“训练”等解释性文字。
- 中心圆填充使用当前阶段预设色，文字和图标使用白色并保持对比度。

必须保留：

- E10.9 continuous progress。
- paused freeze。
- completed / abandoned terminal freeze。
- `+15秒` rest extension monotonic progress。
- skip、`+15秒`、end 的 production controls，不新增 reset。

禁止：

- 不接入声音播放。
- 不实现计划保存。
- 不改统计图表或记录语义。
- 不复制外部 APK / 参考项目资源、图标、字体、音频、代码、命名或逐像素视觉。

### E10.10 Plan Persistence

目标：解决用户测试发现的计划保存不可靠感，确保自定义计划结构能真实保存并恢复。

范围：

- 保存并恢复自定义计时训练阶段、秒数、轮次、颜色、图标、名称和排序。
- 检查计时、力量和跟练计划保存入口是否真实可用。
- 对不可用入口做明确禁用、边界提示或拆到对应后续 story，避免假保存。
- 继续遵守 `WorkoutPlan` 存目标和结构、`WorkoutSession` 存实际执行结果与计划快照的边界。

禁止：

- 不接入声音播放。
- 不改 Timer Dial 生产视觉。
- 不做统计图表。
- 不提交 `.local`、APK、`人工/`、build 输出、截图或日志。

### E10.11 Huashu Timer Dial Prototype

目标：使用已安装的 `huashu-design` skill 做 Timer Dial 高保真 HTML 原型探索，用于后续视觉评审，不直接进入生产实现。

范围：

- 3 个 HTML 高保真方向：黑红高对比、TrainFlow Official 融合、赛博霓虹。
- 每个方向覆盖 active、rest、paused、final 5 seconds 和 rest extension。
- 原型必须用 TrainFlow 自己的 HTML/CSS/Canvas/SVG 语义重建，不复制外部 APK 或参考项目代码、资源、图标、字体、音频、命名、动效参数或逐像素视觉。

边界：

- 本轮不运行 `huashu-design` 原型生成。
- 原型 story 不修改 Kotlin/Gradle/prototype，不接入声音，不实现计划保存。

### E11 健康数据边界与首版心率能力撤销

目标：在 Band 9 smoke 无实时 BLE HRS 证据且手动输入破坏执行页布局后，首版放弃心率显示、录入和统计，同时保留未来真实设备接口边界。

范围：

- 生产执行页不显示心率卡片。
- 不提供手动心率输入。
- 心率不进入 session 或分析数据源。
- 保留 `HeartRateState` / provider 抽象作为未来边界。
- 明确真实设备接入仍是后续独立能力。

边界：

- 不接 Health Connect、Wear OS、BLE 或厂商 SDK，除非另开设备阶段。
- 不做医疗判断或危险告警。

### E12 真实记录、统计、图表和趋势分析

目标：基于真实记录提供可解释的数据分析。

范围：

- 真实记录列表。
- 总统计。
- 图表。
- 计划趋势。
- 同日多轮运动分析。
- 历史记录清理：全部清除、按训练计划清除、按日期清除。

分析原则：

- 比较同类数据。
- 计时训练按同一计划、同一阶段、同一轮次或同类阶段比较。
- 力量训练按同一动作、同一计划结构或同一组语义比较。
- 同日多轮运动需要先分清轮次、计划结构和阶段语义，不把不可比轮次合并成单一结论。
- 首版不再规划平均心率趋势。
- 不把不可比的数据直接得出强弱结论。

### E13 声音、固定女声 cue 与音频共存

目标：建立悦耳、克制、不打断其他 App 的训练音频提示。

范围：

- 最后 N 秒声音提醒。
- `countdown_beep1.mp3` 作为 5 / 4 / 3 / 2 / 1 等最后 N 秒 beep 候选。
- `.local/audio/stage_bell_copper_clean.mp3` 作为倒数到 0 后下阶段开始铃声候选；执行 story 接入时再复制到 `app/src/main/res/raw/`，`.local` 原文件不提交。
- 固定阶段词女声 cue。
- 音频共存策略。
- 手机扬声器和蓝牙耳机 smoke。
- 设备差异测试。

边界：

- 不做用户任意文本 TTS。
- 不做自动语音教练。
- 不降低、暂停或打断其他 App 音乐/视频。
- 不主动 ducking。
- 不请求会打断外部音频的 audio focus。

### 后续单独阶段 力量训练新版 UI

力量训练完整新版 UI 设计单独开启，不塞进 E10.3。该阶段可以重审力量训练信息架构、确认层、历史趋势入口和高级组设置，但仍必须保留 `WorkoutCommand`、`WorkoutEvent`、计划值预填实际记录和训练引擎边界。

### E14.4-2b 阶段内部目标扩展与 TimerDial 外圈语义

E14.4-2a 已确认计划编辑 / 详情 polish 采用方案 B，但不把阶段内部目标扩展 + TimerDial 外圈时间比例语义塞进普通 UI polish。E14.4-2b visual / semantic gate 与 E14.4-2b-2 data model decision 已完成，详见 `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md` 和 `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`；后续进入代码实现前应按 serializer / model、editor、engine timeline、TimerDial mapping 和 compatibility / E12 tests 拆分。

确认方向：

- 计时编辑器沿用当前 UI：轮次与轮间休息保持在上侧位置，阶段编排保持在下方。
- 新增能力只在既有阶段内部扩展更多目标 / 小节。
- 阶段内部目标可新增、删除、拖动、重命名、设置时长和设置颜色。
- 阶段总时长等于内部所有目标时长之和。
- 默认模板仍可以是 `热身 / 高强度工作 / 轮间休息 / 放松` 等阶段，但阶段内部可扩展顺序目标。
- 颜色选择位置直接显示色块，不把中文颜色名作为主要选项文字。
- TimerDial 保持原圆盘 UI，外圈按当前阶段内部目标时长占比分段表达。

语义 gate 结论：

- 已比较方案 A `UI-only compatibility wrapper`、方案 B `explicit timed composition model` 和方案 C `visual grouping only`。
- 推荐方案 B 作为长期目标，但先用方案 A 做兼容 / 视觉验证层，避免没有迁移策略时直接改持久化。
- TimerDial 保持当前已确认的圆盘 UI，只调整外圈语义：外圈表达当前阶段内部 targets，按 target planned duration ratio 分段；active target 用粗弧，已完成 target 退为细弧 / 已经过弧，阶段切换时外圈切换到下一个阶段内部结构。
- TimerDial 内圈继续表达整次训练总进度；中心圆继续表达当前 active 目标 / 阶段、倒计时和暂停 / 继续主控制；12 点数字圆标暂时沿用现有总运动阶段数语义，不作为本轮重设计项。
- `+15秒` 仍只延长当前 active rest step，不插入新阶段，不修改 `WorkoutPlan` 或 plan snapshot，不改变 `timedRestExtensionRecords` 语义；外圈和内圈 progress 必须保持 monotonic，不倒退。
- 当前 `WorkoutPlan.blocks` / `TimedCircuitBlock` / `TimedExerciseItem` 可以近似表达旧计划 wrapper，但不足以稳定表达阶段内部目标 id、目标颜色、嵌套目标、计划快照和历史趋势比较 key。
- 旧计划应通过 compatibility wrapper 打开，查看不自动改写；只有用户在未来 composition editor 中明确保存 / 转换后才写入新结构，既有 `WorkoutSession.planSnapshot` 不回写。

数据模型决策结论：

- 正式采用两层 timed composition 作为长期数据方向。
- 推荐方案 B：新增 versioned timed composition payload，但优先仍存入现有 `WorkoutPlan.blocks` JSON 和 `WorkoutSession.planSnapshot` JSON，不新增 Room table / column。
- 概念结构包含 `compositionVersion`、`warmupSec`、`cooldownSec`、`rounds`、`restBetweenRoundsSec`、`stageGroups` 和内部 `targets`；stage / target 都需要稳定 id、order、name、color 和 duration。
- 旧 `TimedCircuitBlock` / `TimedExerciseItem` 计划继续通过 compatibility wrapper 显示和执行，查看不写回；只有用户明确保存 / 转换时，当前 plan 才写入 composition v2。
- `轮间休息` 继续作为顶层 round configuration，执行 timeline 中只插入轮与轮之间，最后一轮后不插入。
- Target `action` / `custom` 映射 timed work，target `rest` 和 synthetic between-round rest 映射 timed rest；`+15秒` 仍只延长当前 active rest step，不插入新 target，不改 plan snapshot。
- TimerDial 外圈按当前 stage group targets 的 planned duration ratio 分段，rest extension 不重算比例而使用 planned ratio + monotonic progress floor；内圈总阶段数按 warmup + rounds * stageGroups + between-round rests + cooldown 计算，12 点圆标稳定。
- E12 timed comparable trend key 必须纳入 compositionVersion、composition block id、stageGroupId、targetId、targetKind、round / stage instance 和结构签名；旧结构和新结构默认不比较，除非 compatibility mapper 证明等价。
- 仅扩展 JSON payload 时不需要 Room schema migration；若未来新增 entity / table / column，必须拆独立 migration story。

后续拆分：

1. E14.4-2b-1 visual prototype / mock。
2. E14.4-2b-2 data model decision。（Completed）
3. E14.4-2b-3 serializer / model and editor adapter foundation。（Restart implemented / pushed；旧本地实现 rolled back / not accepted）
4. E14.4-2b-4 editor UI visual/code gate。（Restart implemented / pushed；旧本地实现 stopped / rolled back / not accepted）
5. E14.4-2b-5 engine timeline planning gate。（Docs-only complete；不实现 engine）
6. E14.4-2b-5a timeline adapter model/tests。
7. E14.4-2b-5b engine integration。
8. E14.4-2b-5c session record compatibility tests。
9. E14.4-2b-6 TimerDial mapping implementation。
10. E14.4-2b-7 migration / compatibility / E12 trend polish if needed。

E14.4-2b rollback / restart note：E14.4-2b-3 / E14.4-2b-4 的旧本地 model / serializer / editor / UI 实现未通过 review gate，已 rolled back / not accepted；restart 版已重新完成并推送。当前生产基线已有 accepted v2 model / serializer / editor adapter foundation 和 editor UI visual/code gate，但 v2 计划仍以 `待执行映射完成后可开始` 禁用开始训练。E14.4-2b-5 已完成 docs-only planning gate，只允许后续先进入 E14.4-2b-5a timeline adapter model/tests；不得跳过 5a 直接修改 `TimedWorkoutEngine`、TimerDial、`WorkoutCommand`、`WorkoutEvent`、session record、Room schema 或声音语义。

该 story 影响 `WorkoutPlan` blocks、计划快照、统计比较 key、TimerDial UI state 和持久化边界。实现时不得静默修改 Room schema、训练引擎、`WorkoutCommand`、`WorkoutEvent`、session record 或声音语义；若仅扩展 JSON payload，不需要 Room schema migration，但仍必须做 serializer / compatibility 测试。

### E14.6-2 训练完成复盘页

E14.6-2 completion recap page redesign planning / visual gate 已确认：训练完成后的 `completed` terminal state 应切换到独立“本次数据统计复盘页面”，不再继续停留在大 TimerDial 执行页和完成卡片上。

确认方向：

- `ready`、`running`、`paused` 和 `rest` 仍是 execution page；只有 terminal presentation 切换到 recap page。
- 顶部明确 `已完成`，使用克制庆祝感，例如完成徽章、check 或轻量 halo，不做营销页。
- 中部复用现有 timed / strength summary、session recap 和数据总览，不引入假趋势、健康数据或 E12 新实现。
- rest extension、skipped、pause summary 和 early-end 内容只能来自既有 summary / session record 映射，不补造当前 UI state 未暴露的数据。
- 底部主动作推荐 `返回训练首页`；`查看记录` 只作为低层级文字次入口候选，第一版不应与返回形成两个主按钮。
- `abandoned` 可复用同一 recap shell，但必须标注 `已结束` / `提前结束`，不显示 completed celebration，也不说 `已完成`。
- 大 TimerDial 不作为 completed 主视觉；若保留圆盘元素，只能作为小型完成徽章或训练类型标识。
- reduce-motion 时关闭或 snap 庆祝动效，保留静态完成状态。

边界：

- 不改变 `WorkoutSession`、plan snapshot、`timedRestExtensionRecords`、Room schema、训练引擎、`WorkoutCommand`、`WorkoutEvent` 或声音语义。
- 不进入 E14.6-3 stage color/icon system。
- 不进入 E12 records / trends implementation。
- 不恢复心率显示、手动心率输入、未获取心率占位或平均心率趋势。

## 8. 禁止范围

E10.1 明确不做：

- 不实现 E10.2 UI。
- 不重写 timed / strength / follow-along engine。
- 不改 `WorkoutCommand` / `WorkoutEvent` / `WorkoutPlan` / `WorkoutSession` 语义。
- 不新增真实 session 持久化。
- 不新增 Room repository 闭环。
- 不实现手动心率输入。
- 不接 Health Connect / Wear OS / BLE / 厂商手环 SDK。
- 不新增 foreground service、exact alarm、notification action。
- 不实现语音、TTS 或音频资源。
- 不新增统计图表。

E10.5 额外明确不做：

- 不解析或复制 APK 代码 / 资源。
- 不提交 APK、截图、录屏、反编译输出或本地研究临时文件。
- 不实现生产 Timer Dial UI。
- 不改 Room / session repository / 记录闭环。
- 不做手动心率、统计图表、语音 / TTS、声音素材或真实设备接入。
- 不新增第四套 skin。
