# E10.6 Timer Dial Figma / Static Visual Variants

**状态:** E10.6 docs-only design specification
**日期:** 2026-06-10
**范围:** Timer Dial 静态视觉帧、Figma 输出建议、计时编辑页关键状态帧、三套内置 skin 适配说明、后续 E10.7 handoff
**不包含:** Android 实现、Kotlin、Gradle、prototype、E10.7 Compose prototype、E10.8 production integration、E11 心率、E12 统计、E13 声音 / 女声 cue、第四套 skin

## Findings

1. E10.4 已完成本地真实 `WorkoutSession` write-through；E10.6 不处理 Room、DAO、session repository、记录页数据源或历史清理。
2. E10.5 已把 Timer Dial 路线收敛为 Figma 静态规格、可选 HTML / Canvas 动效验证、Jetpack Compose Canvas 生产实现；E10.6 只完成第一段静态规格。
3. 外部 APK 解析 / 反编译参考目录的只读宽扫描只用于确认信息架构、编辑页分层和 UI 微交互方向；本规格不复制 APK 代码、XML、资源、动画参数、图标、字体、音频、命名或逐像素视觉。
4. Timer Dial 不是新训练模式。它是计时训练纯间歇执行页的视觉语言，必须继续由 `TimedWorkoutEngine`、计时 UI state、`WorkoutCommand` 和 `WorkoutEvent` 驱动。
5. E10.6 的静态帧必须先覆盖 Official Flow。Tile Flow 和 Big Type 只做适配规则，不新增第四套 skin，也不把计时执行页拆成碎片化 dashboard。

## E10.6 Design Scope

E10.6 的设计输出建议放在 Figma 一个独立 page 中，命名为 `E10.6 Timer Dial Static Variants`。推荐 frame 分组：

1. `01 Findings And Constraints`：一页约束摘要，标明 engine-state-only、no copied assets、no E11/E12/E13。
2. `02 Execution / Official Flow`：执行页状态帧，Official Flow 为主线。
3. `03 Execution / Skin Adaptation`：Tile Flow、Big Type 适配帧。
4. `04 Editing / Timer Plan`：计时编辑页关键状态帧。
5. `05 Components And Tokens`：圆盘层级、弧线厚度、颜色语义、图标语义、底部图标按钮状态。
6. `06 Accessibility / Small Screen`：720x1280 小屏、触控、对比度、TalkBack 标签和 reduce-motion 标注。

静态设计必须表达状态，不假装已经实现动画。所有进度数值、阶段结构、暂停、完成和废弃终态都应标注来源：`TimedWorkoutEngineState`、计时 session UI state 或 `WorkoutEvent`。

### Visual Tokens

Official Flow 建议以 `DESIGN.md` token 为基础：

| 语义 | 建议颜色 | 用途 |
|---|---|---|
| Page background | `#0F1720` | 训练执行页深色背景。 |
| Dial secondary surface | `#17212B` | 圆盘底轨、中心圆、暂停态遮罩底色。 |
| Work | `#F26B4F` | 工作阶段主弧线、最后 N 秒强调。 |
| Rest | `#65A9FF` | 休息阶段细弧线、休息状态图标。 |
| Warmup | `#2FBF8F` | 热身阶段、进入训练前准备状态。 |
| Cooldown | `#367FD6` | 放松 / cooldown 阶段。 |
| Custom | `#D9921E` | 自定义阶段，需配图标 / 标签避免只靠颜色。 |
| Completed | `#2EAD72` | 完成终态。 |
| Abandoned / danger | `#D84B4B` | 结束确认和废弃终态。 |

圆盘规格建议：

- 主视口按 360dp 宽设计时，Timer Dial 外径建议 280-304dp；720x1280 小屏按 360x640dp 等效检查时外径可降到 248-264dp。
- 外圈当前阶段弧线 16-20dp；同一运动+休息周期中的非当前阶段弧线 8-10dp；`warmup` / `cooldown` / `custom` 按当前阶段语义选择粗弧或辅助细弧，并用阶段类型图标辅助识别。
- 外圈段间 gap 2-4 度，避免阶段粘连；当前阶段可用更高亮度、轻微外发光或端点强调表达焦点，但不得遮挡其他阶段结构。
- 内圈总进度 5-7dp，低于当前阶段弧线层级；不画未经过底轨，只画已经过的白色 / 高对比进度线。active 时像画笔一样线性推进，paused 冻结，completed / abandoned 停止。
- 内圈 12 点位置显示总运动阶段数数字圆标；一个运动阶段包含 work+rest。完成阶段节点显示最新完成数字或实心圆点，当前画笔点直径略大于内圈弧线粗度。
- 中心圆直径建议为外径的 58%-64%；中心倒计时使用 `timerL` 语义，Official Flow 常规帧约 64-72sp，小屏压到 52-60sp。

## Style Variant Directions

E10.6 静态帧需要让评审者能比较 Timer Dial 在不同视觉方向下的可读性、训练现场感和 TrainFlow 品牌一致性。以下方向都只用于 Timer Dial 视觉变体研究，不新增第四套 skin，不改变 Official Flow、Tile Flow、Big Type 三套内置 skin 的产品边界，也不复制 APK 代码、XML、资源、图标、字体、音频、布局 XML、动画参数或逐像素视觉。

### Black / Red High Contrast Reference

黑红高对比方向来自用户偏好的黑红圆盘 mood，但它只能作为情绪、对比度和运动现场感参考。设计输出必须重构为 TrainFlow 自己的 Timer Dial 语言，不复用 APK 资源、图标、字体、音频、布局 XML、命名、动画参数或逐像素结构。

颜色规则：

- 使用深色页面背景和深色圆盘底轨，保证训练时环境干扰低。
- `work` arc 使用高亮红或红橙强调色，作为当前工作阶段的最高视觉焦点。
- `rest` arc 使用低饱和或偏冷色，避免休息阶段与工作阶段同权。
- 中心文字使用白色或浅色，倒计时优先，阶段名 / 状态为次级。

弧线层级：

- 外圈只展示当前一次运动+休息周期，不展示整次训练所有运动+休息阶段。
- 当前阶段使用粗弧表达焦点和当前推进；非当前阶段使用细弧表达同周期上下文。
- 当处于 work 时，work 为粗弧并填充，rest 为细弧；当处于 rest 时，rest 为粗弧并填充，已完成 work 退为细弧。
- 内圈总进度保持低干扰，只表达整次训练按运动阶段数量推进的进展，不制造第二个主焦点。

最后 5 秒可以使用红色强调、轻微闪烁、中心圆呼吸或端点增强，但必须绑定真实 last-N cue state；不得用独立动画假装进度，也不得在 paused、completed 或 abandoned 后继续变化。本方向是 TrainFlow Timer Dial 的风格探索，不是新增第四套 skin。

静态帧差异点：

| 状态 | 输出要求 |
|---|---|
| Warmup | 深色背景保持一致；热身弧线低于 work 强度，可用绿色 / 青绿色或低饱和准备色；中心文字浅色且不使用红色警示。 |
| Work | 当前 work 粗红弧为主焦点；中心大数字白色 / 浅色；已完成弧线弱化但仍可识别阶段结构。 |
| Rest | 当前 rest 粗弧使用冷色或低饱和色并填充推进；已完成 work 退为细弧结构，不抢休息倒计时。 |
| Paused | 所有弧线冻结并降低饱和度；中心显示 `已暂停` / 继续入口；红色强调停止脉冲。 |
| Final 5 seconds | 当前 work 可加强红色、端点或中心圆呼吸；rest 最后 5 秒只做克制提醒；两者都不得遮挡底部操作或伪造进度。 |

### Cyber Neon Exploration

赛博霓虹方向可以探索深色底加霓虹青、紫、红等高对比弧线，用于评估 Timer Dial 是否能在更强风格下仍保持训练读数清楚。它不新增 skin，只作为 Official Flow、Tile Flow、Big Type 中 Timer Dial 的视觉变体研究。

视觉规则：

- 页面底色保持深色，弧线可使用霓虹青 / 紫 / 红等高对比色区分 work、rest、warmup 和 cue。
- 当前弧线可以有 glow / halo 语言，但必须克制，不能影响倒计时、阶段名称、总剩余时间或底部操作读数。
- 中心圆保持信息清晰，数字优先；阶段名、状态、下一阶段预告都必须服从中心倒计时层级。
- 最后 5 秒可以用霓虹边缘脉冲、端点 halo 或短暂颜色强化，但必须可在 reduce-motion 下退化为静态颜色 / 字重变化。

静态帧差异点：

| 状态 | 输出要求 |
|---|---|
| Warmup | 可使用青绿或青色细 glow 表达准备感；glow 不覆盖中心圆边界或顶部总剩余时间。 |
| Work | 当前 work 弧线最亮，可用红 / 紫红 neon；粗弧和端点焦点清楚，但中心倒计时仍是最高层级。 |
| Rest | Rest 使用青 / 蓝紫等冷色细弧；halo 更弱，避免把休息误读为高强度 work。 |
| Paused | Neon glow 收敛或熄灭到低亮度；弧线位置冻结；中心暂停状态必须比装饰 halo 更清楚。 |
| Final 5 seconds | 使用边缘脉冲或短暂颜色强化表达 cue；不得全屏闪烁，不得遮挡暂停 / 继续、跳过或结束确认路径。 |

### TrainFlow Official Flow Integration

Official Flow 融合方向用于说明如何把圆盘语言落回现有 `DESIGN.md` 和 TrainFlow 官方默认体验。它应使用现有 token、较克制色彩、更少 glow、更清晰文本层级，并保持训练场景优先，不做营销化 hero、装饰化仪表盘或大面积品牌展示。

融合规则：

- 使用 `DESIGN.md` 已定义的深色训练背景、action work 色、rest / accent 辅助色、error / success 语义色和现有字号层级。
- Glow 只作为当前阶段焦点的轻量辅助；默认帧应能在无 glow 情况下读清楚。
- 文本层级按中心倒计时、当前阶段、总剩余 / 下一阶段、底部操作排序，不能让品牌文案或装饰信息进入主层级。
- 需要和 Tile Flow / Big Type 适配边界兼容：Tile Flow 可把圆盘作为主磁贴或主区域，Big Type 可放大中心数字并减少辅助信息，但两者都不改变 engine state、命令和事件语义。

静态帧差异点：

| 状态 | 输出要求 |
|---|---|
| Warmup | 使用准备色和较克制弧线强度；中心阶段名和倒计时清楚，不引入动作教学素材。 |
| Work | 使用 Official Flow action 色作为当前粗弧；内圈总进度低干扰；底部图标操作保持可达。 |
| Rest | 使用 rest / accent 辅助色和细弧；中心文案明确休息状态，下一阶段预告为辅助层级。 |
| Paused | 背景和弧线降饱和，中心继续入口明确；所有进度冻结，避免误读为仍在训练。 |
| Final 5 seconds | 使用颜色、字重、端点或轻微中心圆强调；默认不依赖强 glow，reduce-motion 下仍有清晰静态差异。 |

### Frame Output Requirements

E10.6 静态帧至少要能评审上述三类方向：Black / Red High Contrast Reference、Cyber Neon Exploration、TrainFlow Official Flow Integration。每个方向至少输出或标注 warmup、work、rest、paused、final 5 seconds 的静态帧要求或差异点；可以用单独 frames，也可以在同一状态 frame 旁用 variant notes 标明差异，但必须足够让设计评审判断颜色、弧线粗细、中心文字、最后 5 秒 cue 和小屏可读性。

所有方向都必须遵守：

1. E10.6 只输出 Markdown / Figma 静态规格；E10.7 才进入 Compose prototype 和动效验证。
2. 不新增第四套 skin；黑红高对比和赛博霓虹只作为 Official / Tile / Big Type 中 Timer Dial 的视觉变体研究。
3. 不复制 APK 资源、代码、XML、图标、字体、音频、布局 XML、命名、动画参数或逐像素视觉。
4. Warmup / work / rest / paused / final 5 seconds 的进度、冻结、强调和 cue 都必须绑定 `TimedWorkoutEngine` / UI state / `WorkoutEvent`，不得使用视觉假进度。

## Timer Dial Static Frames

执行页状态帧至少输出以下 11 组。每组建议同时标注：状态来源、当前阶段、剩余秒数、总进度 fraction、当前阶段 fraction、可用命令。

### 1. Active Work

- 顶部总剩余时间放在页面顶端，使用 caption / label 层级，例如 `总剩余 18:42`，不大于中心倒计时的 30%-36%。
- 外圈显示当前一次运动+休息周期，当前 `work` 段为粗弧且从空到高亮填充，同周期 `rest` 段为细弧；不把整次训练所有 work/rest 都排到外圈。
- 内圈显示整次训练按运动阶段数量推进的总进度，使用无底轨细弧，不与当前阶段争抢；12 点数字圆标显示总运动阶段数，已完成节点显示数字或圆点。
- 中心圆显示阶段图标、阶段编号或名称、主倒计时和暂停入口。例如：`WORK 03`、`深蹲节奏` 或用户命名阶段、`00:28`。
- 底部保留生产少量操作：跳过 / 下一阶段、`+15秒`（仅 active rest）和结束。结束图标必须进入二次确认；reset 只保留在 preview / demo 或未来命令设计讨论中。

### 2. Active Rest

- 顶部总剩余时间层级保持不变。
- 当前 `rest` 段使用粗弧和休息色，并从空到高亮填充；已经过的同周期 `work` 段退为细弧，避免和当前休息倒计时争抢。
- 中心显示休息图标、`REST` / 用户命名休息阶段、倒计时和可点击继续 / 暂停区域。
- 下一阶段预告可放在中心圆下方或圆盘下缘的辅助文本，不进入主字号。
- 底部可保留跳过 / 下一阶段、`+15秒` 和结束；`+15秒` 只在 active rest 可用，结束仍可达但不抢主层级。

### 3. Warmup / Cooldown

- `warmup` 使用准备感颜色，视觉强度低于 work 但高于底轨。
- `cooldown` 使用冷却 / 收束感颜色，不使用 completed 绿色，避免误读为训练已完成。
- 中心图标应表达阶段类型，不使用动作教学图、姿势动画或外部素材。
- 顶部总剩余时间仍是辅助；中心倒计时保持主信息。

### 4. Paused

- active 弧线和内圈总进度冻结在暂停瞬间的位置。
- 中心圆切换为暂停态：显示暂停图标 / `已暂停` / 当前剩余时间，继续入口明确。
- 背景和弧线可整体降低饱和度；不要让暂停态看起来仍在推进。
- 底部跳过 / 结束可用；结束保留二次确认。Reset 若未来会清空本次进度，需另行定义命令、确认和记录边界。

### 5. Resume Transition

- 静态帧表达从 paused 恢复到 active 的中间状态：中心显示继续图标或 `继续中`，当前阶段弧线焦点恢复到原位置。
- 规格中只定义语义：恢复后从暂停冻结点继续，不补跑暂停期间的视觉进度。
- 该帧是 E10.7 动效参考，不指定 APK 或第三方 easing、duration、关键帧。

### 6. Stage Transition

- 输出 `rest -> work`、`work -> rest` 或 `warmup -> work` 的焦点迁移帧。
- 上一阶段弧线停止并弱化，下一阶段弧线获得焦点；中心圆同步切换图标、名称和倒计时。
- 阶段切换必须由 `WorkoutEvent` / UI state 触发，不使用独立动画时钟改写训练状态。

### 7. Last-N-Seconds Cue

- 最后 N 秒提醒可强调中心数字、当前阶段弧线端点或轻微脉冲环。
- 强调层不得遮挡中心暂停 / 继续，也不得遮挡底部结束、跳过等主控制。
- Work 和 rest 的最后 N 秒要分别可辨：work 可使用 action 强调，rest 可使用 rest 色增强加数字强调。
- 若用户关闭强化动画，静态规格应保留非动画状态：颜色 / 字重 / 数字大小变化即可。

### 8. Completed

- 圆盘停止推进，内圈总进度显示完成状态。
- 中心显示完成图标、`已完成`、总时长或有效训练时间摘要，但不进入 E12 图表 / 趋势。
- 底部操作替换为查看总结 / 返回等后续导航建议；不再显示可推进训练的跳过按钮。
- completed 不能继续消费 tick 造成进度变化。

### 9. Abandoned

- 圆盘停止推进，保留废弃发生时的进度快照。
- 中心显示 `已结束` / `已放弃` 和简要状态，不伪装为完成。
- 使用 error / danger 语义但避免全屏医疗化警报。
- 底部可建议返回总结或记录，但不展示继续推进控件。

### 10. End Confirmation

- 结束训练必须二次确认。
- 推荐用底部 sheet 或紧凑 dialog：标题 `结束本次训练？`，正文说明将保存已完成进度或记录为提前结束，主危险按钮为确认结束，次按钮为继续训练。
- Sheet 不应完全遮挡中心倒计时；背景可冻结并弱化，用户能确认自己正在结束哪一段训练。
- 取消确认后回到原 active / paused 状态，不发送 `EndSession`。

### 11. 720x1280 Small Screen State

- 以 720x1280 px 或等效 360x640dp frame 检查 Official Flow active work、active rest、paused、end confirmation。
- 顶部总剩余时间、中心倒计时、中心暂停 / 继续、底部跳过 / 下一阶段、`+15秒`、结束都必须首屏可见。
- 小屏下可减少下一阶段预告、说明文字和装饰 glow；不得压缩触控目标低于 48dp。
- 最后 N 秒提醒不得把底部操作挤出或覆盖。

## Editing Flow Static Frames

计时编辑页关键状态帧至少覆盖以下 15 组。编辑页可以信息更丰富，但必须保持三段式扫描：左侧身份标识，中间名称 / 时间 / 摘要，右侧操作。

### 1. Editing Header

- Header 展示计划名称、预计总时长、阶段数量和保存状态。
- 主操作为保存 / 立即开始；取消或返回是次操作。
- 若草稿无效，保存 / 开始禁用并给出短原因，不用长说明占满首屏。

### 2. Stage List

- 列表清楚区分热身、工作、休息、放松、自定义阶段。
- 列表顶部可显示总时长和轮次 / 阶段结构摘要。
- 阶段颜色和图标用于识别，不引入动作教学素材。

### 3. Stage Row And Stage Card Variants

- 阶段行方案：左侧为色块 / 图标 / 类型，中央为阶段名、时间、提醒摘要，右侧为拖动手柄、更多操作或编辑入口。
- 阶段卡方案：适合 Tile Flow 或宽松编辑；仍保持左中右区域，不让复制、删除、拖动混在同一区域。
- 选择态、编辑态、拖动态、错误态要分别有明确边界。

### 4. Add Stage Sheet

- Sheet 提供阶段类型、名称、时长、颜色和图标。
- 默认提供常用类型：work、rest、warmup、cooldown、custom。
- 添加后插入位置应可见；热身 / 放松固定边界需要在 UI 上明确。

### 5. Duplicate Stage

- 复制操作从阶段行右侧更多菜单或图标进入。
- 新阶段名称可自动加 `副本` 或编号，但不复制外部命名规则。
- 复制后给出短反馈，并保持用户在列表上下文中。

### 6. Delete Confirmation

- 删除需要确认，尤其当阶段有自定义名称、颜色或时长时。
- 危险按钮使用 error 语义；取消按钮优先保持安全路径。
- 删除热身 / 放松固定阶段时，如后续仍允许，应展示边界说明；当前建议固定阶段只允许调整时长 / 收起，不建议直接删除。

### 7. Color Picker

- 使用色块 / 网格 + 当前选择反馈。
- 每个色块应有名称或 TalkBack 标签，例如 `工作橙`、`休息蓝`。
- 当前选择用边框、勾选图标和标签共同表达，不只靠颜色。

### 8. Icon Picker

- 图标选择只表达阶段类型或状态：热身、工作、休息、放松、自定义、轮次、节奏等。
- 不使用健身姿势动画、动作教学 animated SVG、外部 APK 图标或专有图形。
- 当前选择用边框、勾选和名称表达。

### 9. Quick Duration Choices

- 提供常用快捷时长，例如 10s、20s、30s、45s、60s、90s、120s。
- 快捷值是辅助输入，不替代细调。
- 选择后更新时长 input，并同步预计总时长。

### 10. Duration Fine Tune

- 必须提供 stepper 和 / 或 input；滑块只能作为辅助，不作为唯一输入。
- Stepper 建议支持小步 5s / 大步 15s 或 30s；具体实现留给 E10.7/E10.8。
- 输入清空状态要可表达，保存 / 开始禁用并显示短原因，沿用 E9.4 数字输入边界。

### 11. Expand / Collapse

- 阶段行默认显示名称、时间、类型和摘要。
- 展开后显示颜色、图标、提醒、复制 / 删除、上移 / 下移等高级操作。
- 收起后保留修改结果摘要，避免用户忘记隐藏设置。

### 12. Drag Sorting

- 拖动只从明确手柄触发。
- 拖动态显示浮起、目标插入位置和不可拖动边界。
- 输入框、颜色 / 图标入口、复制、删除和行空白区域不触发拖动。

### 13. Non-Drag Sorting Alternative

- 每个可排序阶段提供上移 / 下移路径，服务小屏、辅助功能和不习惯拖拽的用户。
- 上移 / 下移禁用态要明确，例如第一项不能上移、最后可排序项不能下移。
- 热身固定开头、放松固定结尾时，边界应在禁用态说明。

### 14. Save / Cancel Feedback

- 保存成功使用 success 语义短反馈。
- 保存失败使用 error 语义和具体可修复原因。
- 取消编辑前如有未保存修改，应确认；无修改可直接返回。

### 15. Small Screen Bottom Actions

- 720x1280 小屏下，底部保存 / 开始操作不能遮挡正在编辑的阶段内容。
- 若使用固定底部栏，列表底部应有足够 padding，最后一行完全可滚动到固定栏上方。
- Sheet 打开时应遵守安全区，不遮挡关键 input、stepper 或确认按钮。

## Interaction Animation Spec

本节只定义设计语义，不实现动画。E10.7 / E10.8 应使用 TrainFlow 自己的 Compose / Material 动效实现，不复制 APK 动画参数、duration、easing、关键帧或路径。

1. Center tap pause / resume：点击中心圆后发送 `PauseSession` 或 `ResumeSession`；paused 静态帧冻结弧线，resume 从冻结点继续。
2. Dial active progress：active 时当前阶段弧线和内圈总进度按 engine / UI state 线性推进；rest extension 后外圈当前 rest 弧和内圈 work+rest cycle progress 必须单调、不倒退；paused、completed、abandoned 不推进。
3. Stage focus migration：阶段切换由 `WorkoutEvent` 或 UI state 差异触发；上一阶段停住，下一阶段获得焦点。
4. Work / rest change：work 切 rest 时粗弧转为细弧和休息色；rest 切 work 时恢复粗弧和 action 色。变化应清晰但不整页闪烁。
5. Last-N-seconds cue：根据用户 cue settings 表达数字强调、弧线增强或轻微脉冲；提醒不得遮挡主控制，不引入声音或女声 cue。
6. Bottom icon buttons：pressed、disabled、focus、danger confirm 状态要有静态规格；结束按钮不能在一次点击后直接结束。
7. Editing add / expand / sort / picker / delete：添加 sheet、展开 / 收起、拖动排序、颜色 / 图标选择和删除确认都应有反馈状态，但只作为 E10.7/E10.8 的设计 handoff。
8. Reduce motion：用户关闭强化动画时，保留状态色、字号和静态强调，不做脉冲或强闪烁。

## Official Flow / Tile Flow / Big Type Adaptation

### Official Flow

- Official Flow 是 E10.6 必须完成的主方案。
- 使用 `DESIGN.md` 的深色训练执行面板、冷静底色、action work 色和 accent / focus 辅助色。
- 页面应安静、可扫读、控件少；当前阶段、倒计时和中心暂停 / 继续是最高层级。

### Tile Flow

- Tile Flow 可把 Timer Dial 放入更模块化的训练工作区，但执行页不能碎片化。
- 圆盘仍是单一主磁贴或全屏主区域；下一阶段、总剩余和少量指标可以作为辅助磁贴，但不得高于中心倒计时。
- 阶段列表编辑页可以使用阶段卡方案，但不能卡片嵌套卡片，也不能让拖动、编辑、删除区域混乱。

### Big Type

- Big Type 优先远距离可读：中心倒计时更大，阶段名称更短，辅助信息更少。
- 外圈可简化非当前阶段标签，只保留当前运动+休息周期的颜色 / 粗细 / 段位结构。
- 底部图标按钮触控目标应更大，建议 56dp 或以上；文字标签可只在必要时显示。
- 720x1280 小屏下 Big Type 仍必须保留中心暂停 / 继续、跳过 / 下一阶段和结束确认路径。

## Accessibility And Small Screen Checks

1. 对比度：中心倒计时、顶部总剩余、底部图标和确认按钮必须满足 WCAG AA；非文本图形元素至少满足 3:1。
2. 触控：中心圆、底部图标按钮、编辑页拖动手柄、stepper、picker 色块和确认按钮不小于 48dp。
3. 非颜色表达：阶段类型必须同时用颜色、图标和文本 / 可访问标签表达；work / rest 不能只靠红蓝区分。
4. TalkBack：中心圆标签应读出当前状态，例如 `工作阶段，剩余 28 秒，双击暂停`；paused 时读 `已暂停，双击继续`。
5. 最后 N 秒：强化提醒不能闪烁过强，不能遮挡主控；reduce-motion 下使用静态强调。
6. 小屏：720x1280 检查 active work、active rest、paused、last-N cue、end confirmation、编辑页底部操作和 picker sheet。
7. 误触：结束训练必须二次确认；E10.8 production 不实现 reset。Reset 若会清空本次进度，应延后为未来命令设计项，并先明确 `WorkoutCommand`、二次确认、session record 边界和测试。

## Do Not Use / Legal Boundary

1. 不复制 APK 代码、XML、资源、图标、字体、音频、SVG/PNG、animated SVG、动画 XML、easing、duration、关键帧、路径、控件命名、资源命名或逐像素视觉。
2. 不提交 APK、截图、录屏、反编译输出、日志或临时研究产物。
3. 不使用健身姿势动画或动作教学 animated SVG；阶段图标只表达阶段类型或状态。
4. 不新增第四套 skin。黑红高对比、赛博霓虹只能作为三套内置 skin 的视觉探索或 mood 参考。
5. 不混入 E11 心率、E12 统计图表 / 趋势、E13 声音 / 女声 cue / 音频资源。
6. 不允许 Timer Dial 使用视觉假进度、独立计时器或绕过 `TimedWorkoutEngine` 的状态。

## Suggested E10.7 Handoff Notes

E10.7 Compose prototype 已按以下输入实现为 Android prototype complete；后续生产收口进入 E10.8：

1. 建立 `TimerDialUiState` 或等价 mapper，字段至少包含：phase type、phase name、icon key、color hex、current phase progress、total progress、remaining seconds、total remaining seconds、status、last-N cue state、available commands。
2. Compose Canvas 绘制外圈当前运动+休息周期、当前阶段推进弧和内圈总进度；所有 fraction 来自 UI state。
3. 中心圆点击只分发 `WorkoutCommand.PauseSession` / `ResumeSession`，不直接修改计时。
4. 阶段切换和 last-N cue 消费 `WorkoutEvent` / UI state；不引入声音、TTS、女声 cue 或音频素材。
5. 对 Official Flow 先实现可运行 prototype，再验证 Tile Flow / Big Type token 和布局适配。
6. 回归检查包含 720x1280、小屏 TalkBack 标签、终态停止推进、结束二次确认、paused 冻结、completed / abandoned 防污染。

E10.7 implementation note:

- Prototype 文件位于 Android `feature.workoutsession`：`TimerDialUiState.kt`、`TimerDialTokens.kt`、`TimerDial.kt` 和 `TimerDialPreview.kt`。
- 三类 visual variant 已作为 Timer Dial prototype tokens 实现：Black / Red High Contrast、Cyber Neon、TrainFlow Official Flow Integration；它们不是新增 UI skin。
- 计时执行页已低风险消费该 prototype；E10.8 已将生产默认收口到 Official Flow Timer Dial，外圈只展示当前运动+休息周期，内圈展示整次训练总进度，中心圆负责暂停 / 继续，底部跳过和结束使用图标，结束仍需二次确认。
- E10.7 未实现声音、TTS、女声 cue、统计图表、心率设备、Room/session repository 改动、foreground service、exact alarm 或 notification action。

E10.8 implementation note:

- `TimerDialUiState` 的生产默认 visual variant 为 Official Flow；Black / Red High Contrast 与 Cyber Neon 只保留为 preview/demo visual variants，不新增第四套 skin。
- 外圈阶段 segments 由 UI state mapper 收窄到当前一次运动+休息周期：work active 时 work 粗弧填充、rest 细弧；rest active 时 rest 粗弧填充、已完成 work 细弧。
- 内圈总进度由 raw second progress 收窄为 workout-stage marker progress：总数为 work/custom 运动阶段数量，一个阶段包含 work+rest；12 点圆标显示总数，最新完成节点显示数字，之前完成节点退为实心圆点，未经过部分不画底轨。
- 计时执行页移除重复的计划标题、步骤标签、状态 pill 和圆盘外重复阶段标签；顶部保留总剩余时间，中心显示当前阶段倒计时，点击中心圆暂停 / 继续。
- `+15秒` 只在 active rest 可用，用于给当前休息增加 15 秒，不修改原计划。
- Rest extension progress fix must follow the monotonic, non-regressing, state-driven Timer Dial rule: extending rest keeps the current outer rest arc and inner cycle progress at least at the pre-extension value, then continues forward while active; paused and terminal states stay frozen.

## Verification Notes

E10.6 只改 Markdown / 设计文档。E10.8 production integration 后，验证要求扩展为：

1. 运行 `git diff --check`。
2. 运行 `git diff --name-status`。
3. 运行 Android unit test / assemble / lint / check，确认 Timer Dial state mapping 和 existing session flow 不回退。
4. 如可运行 emulator，截图放 `.local/verification/e10-8/` 且不提交；不可运行时说明原因。
5. 确认未 stage、commit、移动 APK、截图、录屏、反编译输出或参考解析产物。
