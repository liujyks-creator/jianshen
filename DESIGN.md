---
version: alpha
name: TrainFlow Design System
description: Android-first fitness training assistant design system focused on calm execution, elegant planning, and open-source UI customization.

colors:
  primary: "#0F1720"
  secondary: "#17212B"
  accent: "#2FBF8F"
  action: "#F26B4F"
  focus: "#65A9FF"
  surface: "#FFFFFF"
  surfaceMuted: "#F5F7FA"
  neutral50: "#FAFBFC"
  neutral100: "#EEF2F5"
  neutral200: "#D7DEE5"
  neutral300: "#A8B3BE"
  neutral500: "#65717D"
  neutral700: "#35424E"
  neutral900: "#111820"
  success: "#2EAD72"
  warning: "#D9921E"
  error: "#D84B4B"
  info: "#367FD6"

typography:
  displayL:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "44px"
    fontWeight: 700
    lineHeight: 1.08
  headingXl:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "34px"
    fontWeight: 700
    lineHeight: 1.16
  headingL:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.22
  headingM:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "22px"
    fontWeight: 650
    lineHeight: 1.28
  headingS:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "18px"
    fontWeight: 650
    lineHeight: 1.34
  bodyL:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.55
  bodyM:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
  bodyS:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.45
  labelM:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 650
    lineHeight: 1.35
  captionM:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.35
  timerL:
    fontFamily: "Inter, Roboto, system-ui, sans-serif"
    fontSize: "72px"
    fontWeight: 750
    lineHeight: 0.95
  codeM:
    fontFamily: "JetBrains Mono, Menlo, Consolas, monospace"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5

spacing:
  "0": 0
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "20px"
  "6": "24px"
  "8": "32px"
  "10": "40px"
  "12": "48px"
  "16": "64px"
  "20": "80px"

rounded:
  none: 0
  xs: "2px"
  sm: "4px"
  md: "8px"
  lg: "10px"
  xl: "14px"
  "2xl": "20px"
  full: "9999px"

motion:
  touchFeedbackDurationMs: 100
  stateTransitionDurationMs: 160
  localLayoutTransitionDurationMs: 220
  pageTransitionDurationMs: 260
  continuousProjectionMaxDurationMs: 1000
  reducedMotionDurationMs: 0
  touchFeedbackScale: 0.97
  pressedAlpha: 0.86
  disabledMotionAlpha: 0.72
  standardEasing: "cubic-bezier(0.16, 1, 0.30, 1)"
  emphasisEasing: "cubic-bezier(0.34, 1.16, 0.64, 1)"
  continuousProgressEasing: "linear"

components:
  buttonPrimary:
    backgroundColor: "{colors.accent}"
    color: "{colors.neutral900}"
    paddingX: "{spacing.5}"
    paddingY: "{spacing.3}"
    borderRadius: "{rounded.md}"
    fontSize: "{typography.labelM.fontSize}"
    fontWeight: 700
  buttonSecondary:
    backgroundColor: "{colors.secondary}"
    color: "{colors.neutral50}"
    paddingX: "{spacing.4}"
    paddingY: "{spacing.3}"
    borderRadius: "{rounded.md}"
    borderColor: "{colors.neutral700}"
    borderWidth: "1px"
  trainingPrimaryButton:
    backgroundColor: "{colors.action}"
    color: "{colors.neutral50}"
    paddingX: "{spacing.6}"
    paddingY: "{spacing.4}"
    borderRadius: "{rounded.lg}"
    fontSize: "{typography.headingS.fontSize}"
    fontWeight: 750
  input:
    backgroundColor: "{colors.surface}"
    borderColor: "{colors.neutral200}"
    borderWidth: "1px"
    borderRadius: "{rounded.md}"
    paddingX: "{spacing.3}"
    paddingY: "{spacing.3}"
    color: "{colors.neutral900}"
    placeholderColor: "{colors.neutral500}"
    focusBorderColor: "{colors.focus}"
  card:
    backgroundColor: "{colors.surface}"
    borderColor: "{colors.neutral100}"
    borderWidth: "1px"
    borderRadius: "{rounded.lg}"
    padding: "{spacing.5}"
  executionPanel:
    backgroundColor: "{colors.primary}"
    color: "{colors.neutral50}"
    borderRadius: "{rounded.xl}"
    padding: "{spacing.6}"
  bottomNav:
    backgroundColor: "{colors.surface}"
    activeColor: "{colors.accent}"
    inactiveColor: "{colors.neutral500}"
    borderColor: "{colors.neutral100}"
  chip:
    backgroundColor: "{colors.surfaceMuted}"
    color: "{colors.neutral700}"
    borderRadius: "{rounded.full}"
    paddingX: "{spacing.3}"
    paddingY: "{spacing.2}"
---

# TrainFlow Design System

## Overview

TrainFlow 是 Android 首发的训练计划执行助手。它帮助自定义训练者把计划稳定地执行完，并在训练后留下可回顾记录。

设计性格是：**清晰、克制、可信、带一点运动现场的能量**。  
它不是社交健身内容流，也不是炫技课程平台。官方默认界面要让用户在运动中少思考、少误触，训练前编辑足够完整，训练中执行足够安静。

默认体验采用浅色工作区 + 深色训练执行面板的组合：

- 计划、动作库、记录页面以浅色为主，便于阅读和编辑。
- 训练执行页使用深色高对比面板，让倒计时、当前动作和主按钮更聚焦。
- 社区可以替换主题和 UI shell，但不得改变训练命令、事件和数据契约语义。

## Colors

调色策略是“冷静底色 + 清爽确认色 + 运动动作色”。绿色用于开始、保存、确认等正向操作；珊瑚橙用于训练中强动作，例如完成本组、临近切换、重要倒计时。蓝色只作为焦点和信息辅助，不主导品牌。

### Brand Colors

- **Primary (`{colors.primary}`):** 训练执行面板、沉浸式倒计时背景、深色导航区域。
- **Secondary (`{colors.secondary}`):** 深色面板内的次级区域、训练状态容器。
- **Accent (`{colors.accent}`):** 计划保存、开始训练、选中状态、成功路径的主品牌色。
- **Action (`{colors.action}`):** 训练中的高优先级动作，包括完成本组、临近结束强调、立即开始。
- **Focus (`{colors.focus}`):** 输入焦点、无障碍焦点环、信息性高亮。

### Neutral Colors

- **neutral50 / neutral100:** 浅色页面背景、分隔层。
- **neutral200 / neutral300:** 边框、禁用边界、辅助图标。
- **neutral500:** 次要正文、说明、未选中导航。
- **neutral700:** 主要编辑文本的辅助层级、标签。
- **neutral900:** 浅色页面主文本。

### Semantic Colors

语义色只用于状态，不用于装饰：

- **Success:** 保存成功、训练完成、校验通过。
- **Warning:** 计划缺项、提醒权限缺失、动作内容待审核。
- **Error:** 删除、无法保存、危险操作确认。
- **Info:** 设备说明、帮助提示和未来健康数据边界说明。

### Color Rules

- 正文和关键倒计时必须满足 WCAG AA 对比度。
- 不使用单一绿色铺满所有页面；绿色只承担确认和品牌路径。
- 训练执行页中，`{colors.action}` 的使用要短促、有目的，避免整页橙色造成紧张。
- 未来若恢复健康数据展示，不得使用医疗化红色警报，除非有明确非医疗提示规则。

### Stage Colors

计时阶段颜色服务阶段识别，不只是装饰。阶段色保存为 `TimedExerciseItem.colorHex`，由计划编辑、计划持久化、ready gate 和 Timer Dial 共同消费；颜色选择不能改变训练引擎、命令、事件或 session record 语义。

- 阶段色板由集中 `StageColorPreset` 定义，字段包含 `id`、`name`、`hex`、`tone`、`recommendedUse`、`textColor`、`isHighAttention` 和 `isRecommended`。
- 推荐色保持 5-8 个，服务快速创建；更多色可覆盖完整 20 色 Material-like 色板，服务用户自定义。
- 默认体验保持克制，不鼓励同一计划堆叠大量高饱和或高注意色。红、深橙、橙、琥珀、柠黄绿等高注意色应明确标记，用于工作、爆发或提醒感阶段。
- 执行页深色背景下，阶段色填充 Timer Dial 中心圆时，圆内文字和图标必须使用 preset `textColor` 或安全 fallback 保持高对比。
- 非法 `colorHex` 必须回退到阶段默认安全色，不应导致计划详情、ready gate 或执行页崩溃。
- E14.6-3 规划补充：热身、放松和轮间休息都应按阶段处理并支持颜色；轮数只是结构计数，不需要自己的颜色。
- E14.6-3a 数据契约决策补充：composition v2 的 boundary stage 可选样式字段为 `warmupStyle`、`cooldownStyle` 和 `restBetweenRoundsStyle`，每个字段只包含可选 `colorHex` 与可选 `iconKey`，随 `WorkoutPlan.blocks` / `WorkoutSession.planSnapshot` JSON 保存；不新增 Room migration。
- Timer Dial 色彩解析遵守 target color 优先，其次 stage group color，再到 warmup / cooldown / between-round rest 默认色，最后回退到阶段类型安全色。默认推荐色必须足够少，避免外圈在 1-5 targets 场景下变成噪声。

### Stage Color Picker

- 阶段卡、计时目标卡和计划详情卡应展示当前色块和可打开的颜色选择入口；颜色选择器优先展示推荐色，再展示更多颜色。力量目标组颜色当前不进入 MVP。
- 力量目标组颜色不进入当前 MVP；编辑页不得展示“目标组颜色 / 后续保存”这类未完成占位入口。力量目标组识别以动作名称、组序号、热身 / 正式 / 递减 / 回退标签、目标重量、目标次数和休息为主。
- 删除力量目标组颜色入口后，目标组编辑区必须重新收拢布局，不能留下原颜色卡片位置造成的空洞。推荐重量 / 次数并排，休息输入独占一行或全宽显示；每个目标组之间保持清晰但紧凑的分隔。
- 力量执行页不需要展示动作短提示作为主内容；力量训练大多依靠器械、动作名称和组目标执行，自定义动作也只要求用户输入名称。计时 / 跟练仍可保留动作短提示，力量执行页应保持更克制。删除短提示后，当前组主卡应自然收缩高度，不用新的解释文案填空。
- 力量执行页“下一组”区域应是摘要而不是说明卡：优先显示下一组动作名、组序号 / 总组序号、重量和次数，避免重复解释“力量训练按动作和组推进”这类低价值文案。
- 若未来重新引入力量目标组颜色，必须先拆 model / serializer decision，不能在 UI polish 中静默新增 `StrengthSetPlan` 字段、plan snapshot JSON 或 Room schema；颜色仍不能表示训练强弱、加重量建议、康复建议或医疗含义。
- E14.6-3 后，阶段 / 目标样式入口可以在同一 panel 中同时选择颜色和内置 icon，但拖拽手柄、展开 / 收起和样式入口必须分开，避免误触。
- 色块尺寸应稳定，避免选中、hover、TalkBack 或文案变化导致布局跳动。
- 选中态不能只靠颜色表达，必须至少包含外圈 / 描边、对勾和 TalkBack 文案。
- 每个色块的可访问文案应包含颜色名称、推荐用途、高注意色状态和当前是否选中。
- 色板应优先使用可复用的大色板 bottom sheet / dialog / page-style panel，包含 `推荐色`、`更多颜色`、大色块、icon grid 和完成动作；编辑卡片不使用多个文字颜色选项或图标长文字列表挤占主编辑区域。
- 计划颜色是用户手动设置的计划级颜色，不自动从首个阶段、阶段内部目标或力量目标组推断。计划颜色默认可使用红色，并可通过计划列表 / 详情左侧色块或展开计划内的颜色入口修改；若实现需要新的持久化字段，必须先拆数据决策，不在 UI polish 中静默改 Room schema。
- 色板不能引入第四套 skin、远程主题、运行时插件市场或第三方皮肤安装。

### Stage Icons

阶段图标用于快速识别当前训练阶段或目标，不是动作教学图。第一版只使用项目提供的内置白色图标集，通过稳定 `iconKey` 引用，并叠加在阶段色中心圆或阶段色块上。

- E15-3 用户反馈后，内置 `iconKey` 的呈现可由项目打包的白色 PNG 资源实现，以提升小尺寸语义清晰度；这只是 APK 内部实现细节，不把图片路径或 drawable/raw 资源名写入计划或训练快照。
- 热身、工作、休息、放松、自定义、轮间休息和 composition target 都应有默认图标 fallback。
- 阶段 / 目标可保存 `iconKey`；无效或缺失的 key 回退到阶段类型默认图标，不影响训练执行。
- 推荐首批 key 覆盖 `warmup`、`work`、`speed_up`、`sprint`、`rest`、`recover_breathe`、`cooldown`、`strength`、`mobility` 和 `custom`。
- 计划数据只保存 icon key，不保存图片路径、SVG 路径、vector path、资源路径、URL 或上传资产引用。
- `iconKey` 应是已知内置 key；未知、空值、URL-like、path-like、resource-like、SVG-like 或上传资产-like 值都按缺失处理，最终安全回退到类型默认 icon 或 `custom`。
- 图标和阶段色必须一起满足对比度；深色执行页中优先使用白色或 `StageColorPreset.textColor`。
- 第一版不支持用户上传图片、自定义图片库或远程图标资源。自定义图片只作为 post-MVP / later story，并且必须先明确存储、权限、备份和开源定制边界。
- 图标不能承诺动作内容指导、AI 纠错、语音教练或健康设备能力。

## Typography

字体栈优先使用 Inter，Android 实现可落到 Roboto/system。排版目标是运动中可扫读：数字大、标签短、正文稳。

### Levels

- **displayL:** 训练执行页主倒计时以外的大型状态标题，慎用。
- **timerL:** 训练执行页主倒计时和组耗时，必须居于视觉中心。
- **headingXl / headingL:** 页面标题、训练总结关键结果。
- **headingM / headingS:** 卡片标题、当前动作名称、确认层标题。
- **bodyL:** 训练中短提示、动作详情重要段落。
- **bodyM:** 默认正文、计划配置说明。
- **bodyS:** 次要说明、列表辅助信息。
- **labelM:** 按钮、表单标签、状态标签。
- **captionM:** 时间戳、单位、辅助说明。
- **codeM:** 数据契约、调试标识、开发文档示例。

### Typography Rules

- 不使用负字距。
- 训练执行页的主数字不能被次级标签、未来健康数据或装饰元素抢占。
- 中文文案优先短句，按钮文字优先动词短语。
- 长说明放在详情页或展开层，不放在运动中的主控制区。

## Layout

### Spacing

TrainFlow 使用 4px 基数，移动端页面默认横向 padding 为 `{spacing.4}` 到 `{spacing.5}`。训练执行页允许更大的垂直留白，让用户在运动中更容易点按。

### Mobile Layout

- 首页采用纵向扫描：今日状态、主入口、继续训练、最近计划、快捷入口。
- 训练执行页主信息垂直居中，控制区固定在底部安全区上方。
- 力量训练确认层使用底部 sheet 或轻量 modal，不跳转离开训练上下文。
- 动作详情在训练中优先使用覆盖层，关闭后回到原 session。

### Density

- 编辑页可以信息更密，但必须有分组和折叠。
- 计划详情可以采用可折叠计划播放列表；当前计划的开始、编辑、复制和删除操作必须归属展开计划卡片，避免用户误读删除范围。
- 计划编辑页的保存和开始训练可以使用轻量 sticky bottom action。`保存计划` 是绿色主按钮；`开始训练` 是深色实心次按钮，不使用红色实心样式抢占保存层级。
- 力量目标组默认折叠，折叠态展示目标组摘要；展开态再编辑重量、次数和休息。
- 选项文案较长的二选一 / 多选一控件应自适应为竖向 selector 或多行布局，不用横向 chip 把中文标签推出卡片边界；小屏和 Big Type 下不得依赖横向滚动才能读完关键选项。
- 执行页信息密度必须低，最多同时强调一个主动作。
- 记录和趋势页以可比较为主，不做营销式大卡片堆叠。

## Elevation & Depth

官方默认界面主要使用边框、底色差和少量阴影建立层级。

- **Level 0:** 页面背景 `{colors.surfaceMuted}`。
- **Level 1:** 卡片、列表项 `{colors.surface}` + 1px 边框。
- **Level 2:** 底部 sheet、确认层、动作详情覆盖层。
- **Level 3:** 训练执行沉浸面板 `{colors.primary}`，不依赖重阴影。

不要使用大面积渐变、装饰光斑或纯氛围背景。训练类产品要让用户看清当前动作，而不是欣赏装饰。

## Shapes

圆角整体克制：

- `rounded.sm` / `rounded.md`: 标签、输入框、普通按钮。
- `rounded.lg`: 卡片、主要按钮、训练状态块。
- `rounded.xl`: 训练执行大面板、底部确认层。
- `rounded.full`: chip、计数徽标、头像占位。

卡片圆角默认不超过 10px，除非是训练执行页的大面板或底部 sheet。

## Components

### Buttons

- **Primary:** 保存、开始训练、确认记录。每个页面最多一个。
- **Training Primary:** 训练执行页主动作，例如开始本组、完成本组、继续训练。
- **Secondary:** 查看详情、复制计划、进入设置。
- **Ghost/Icon:** 暂停、跳过、动作详情、关闭覆盖层。
- **Danger:** 删除计划、提前结束训练。

训练执行页按钮必须具备足够触控面积，主按钮不小于 48px 高。

### Cards

卡片用于单个计划、动作、训练记录或恢复建议。不把整页 section 做成浮动卡片，也不嵌套多层卡片。

### Inputs

输入用于计划名称、重量、次数、时间和提醒配置。数字输入要优先提供步进或快捷值，避免运动中键盘输入。

### Chips

Chip 用于动作能力、部位、器械、难度、训练状态。Chip 不是主按钮，不承载破坏性操作。

### Bottom Navigation

首版底部导航为训练、计划、动作库、记录。设置可放在个人/更多入口，不抢主导航。

### Training Execution Panel

训练执行面板必须包含：

- 当前动作、当前阶段或休息状态。
- 主倒计时或组耗时。
- 下一动作或下一组。
- 主控制按钮。
- 不再放置心率状态位；未来健康数据只能在不挤压主训练信息时重新评估。
- E16 Band 9 心率广播 retest 已提供 BLE HRS 正向证据，但这只支持未来另拆 adapter spike；真正展示心率 UI 前，必须先做 HTML 视觉方案 / 高保真案例评审，再进入单独 Android UI 实现。
- E16-1 已实现 debug-only BLE HRS adapter spike 和纯 parser 测试；这仍不是生产 UI 许可。训练执行页不得因为 debug adapter 存在而恢复心率卡片、未获取心率占位、手动输入或平均心率趋势。
- E16-2 已实现 production-capable BLE HRS provider / permission / lifecycle 地基；这仍不是训练执行页 UI 许可。任何未来心率展示都必须先经过 `huashu-design` HTML 高保真视觉 gate，并证明心率不会高于当前阶段、主时间、组目标、下一步和主控制。
- E16-3 初版顶部轻量状态 pill 只保留为 overlap-risk 视觉探索，不作为未来 Android 实现依据。当前心率 UI 方向改为 E16-3a **App 内可拖动浮动心率胶囊**：胶囊是 TrainFlow app shell 内 overlay，不使用系统级悬浮窗 / “显示在其他应用上层”权限，不参与训练页布局，不推动 TimerDial、strength active/rest/confirm-record 或固定底部主按钮重排。偏好开启后可在 TrainFlow app 内页面显示；未训练时只显示不记录；训练中才允许按 1 秒采样记录。
- E16-4 明确心率默认关闭，必须通过设置页显式开启。开启前应展示用途、BLE scan / connect 权限、隐私和非医疗说明；BLE 权限只能在用户主动开启 / 选择设备 / 重新扫描后触发，不在 app 启动、训练页进入或训练开始时触发。设备偏好只保存 identifier / display name；关闭后胶囊消失，不扫描、不连接、不记录，并提供清除已保存设备入口。
- 浮动心率胶囊必须区分两类状态：无可用 bpm 时显示连接 / 数据状态（`未启用`、`未连接源`、`权限未赋予`、`蓝牙关闭`、`正在连接`、`等待数据`、`数据过期`、`离线`），这些状态使用中性或弱提示颜色；有 bpm 且用户已设置年龄时显示“区间 + bpm”，例如 `热身 105 bpm`、`燃脂 122 bpm`，整体颜色跟随低强度、热身、燃脂、有氧、无氧、极限或超过上限。未设置年龄时只显示 bpm，不做区间判断。
- `超过上限` 仅表示超过用户设置的提醒阈值，首版只用深红视觉提示；不得播放声音、震动、强制暂停，也不得写成医疗告警、危险诊断或训练中断依据。胶囊拖动必须有安全区：松手后不得停在主按钮、底部导航、confirm-record 控件、输入框、键盘区域、系统状态栏或手势导航上方；点击展开和拖动移动必须有明确阈值以减少误触。

主倒计时和主按钮的层级永远高于历史信息、说明和未来健康数据。

E10.1 后，计时训练后续按纯间歇计时器处理，执行页主信息是当前阶段、阶段倒计时和大圆盘主控制区，不再要求展示动作库动作或动作详情入口。

### Completion Recap Page

训练完成后应进入“本次数据统计复盘页面”，而不是继续停在执行页大圆盘加卡片的完成态。完成页仍属于训练闭环，但信息层级从“控制训练”切换为“确认完成并回看本次数据”。

- 顶部使用克制但有庆祝感的完成效果，并明确标注 `已完成`。推荐使用完成徽章、check、轻量 halo 或短促 entry motion；不要做夸张营销页或大量装饰 confetti。
- 顶部可以显示简短完成语和计划名 / 训练类型，但不把解释性文案做成大段说明。
- 中部先展示关键数据摘要，再复用已有训练总结、数据总览和 session summary，不编造尚未实现的趋势或健康数据。
- 未来 E16-12 心率复盘只在 E16-11 已保存来源明确的训练中心率样本后进入单次记录详情；可参考“摘要 + 心率曲线 + 区间时长”的层级，但不复制外部截图、划船距离 / 配速 / 桨频 / 功率 / 卡路里，或未经模型验证的恢复结论。实现前必须先完成 E16-12a `huashu-design` HTML 高保真视觉评审。
- 计时训练复盘应继续来自现有 summary：总时长 / 有效信息、完成阶段或步骤、轮次进度、跳过内容、额外休息、训练部位和恢复入口。力量训练复盘应继续来自现有 summary：动作 / 组数完成情况、计划值与实际记录、组耗时、实际休息、替换和跳过。
- rest extension、skipped、pause summary 和 early-end 信息只能来自既有 session record / summary 映射；当前 UI state 没有暴露的数据不得补假值。
- 底部提供单一主返回入口，产品默认推荐 `返回训练首页`，保持小屏和导航栏安全区可达。完成页内容较长时，返回主动作应固定在底部导航 / 系统安全区上方，页面内容增加对应 bottom padding，不能要求用户滑到复盘卡片底部才离开。`查看记录` 可作为低层级文字次入口候选，但第一版不应与返回形成两个主按钮。
- `completed` 才使用完成庆祝；`abandoned` 可使用同一 recap shell 的结束摘要语气，标注 `已结束` / `提前结束`，不显示 completed celebration。
- 完成页不使用大 TimerDial 作为主视觉。若保留任何圆盘元素，只能作为小型完成徽章或训练类型标识，而不是继续占据执行页主控制层级。
- reduce-motion 时关闭或 snap 完成庆祝动效，保留静态完成状态，不影响返回和记录写入。
- 完成页不得改变 `WorkoutSession` 语义，不得把 E12 records / trends polish 混入完成态重设计。

### Timer Dial

E10.5 后，计时训练大圆盘进一步收敛为 Timer Dial 圆盘视觉语言。Timer Dial 可以参考黑红高对比的运动现场感，但必须使用 TrainFlow 自己的 token、图标语义、弧线层级和动效规则，不复制外部 APK / 截图的代码、资源、图标、字体、音频、专有动画或逐像素视觉。

E14.4-2b 后，计时训练阶段内目标扩展已作为独立语义链路完成并关闭。当前基线采用 versioned timed composition payload 存入现有 `WorkoutPlan.blocks` JSON / `WorkoutSession.planSnapshot` JSON，不新增 Room table / column；旧计划通过 compatibility wrapper 展示和执行，查看不得静默改写。E14.4-2b-3 到 6c 已完成 editor-side composition v2 payload、adapter-expanded deterministic timeline、minimum engine bridge、adapter-expandable start gate、session record compatibility、TimerDial production mapping 和 smoke / visual QA review。后续不再继续 E14.4-2b implementation；reduce-motion mapping smoke、单独 3 / 4 target captures、E12 records/trends polish 或其他 UI polish 都必须另开任务。

Timer Dial 的设计结构：

- 顶部显示本次训练总剩余时间，但低于中心倒计时层级。
- 外圈表达当前一次运动+休息周期；当前阶段弧线按线性动画匀速填充。
- 处于 `work` 阶段时，`work` 为粗弧、同周期 `rest` 为细弧；处于 `rest` 阶段时，`rest` 为粗弧、已完成 `work` 退为细弧。
- 内圈表达按运动阶段数量推进的整次训练总进度，不画未经过底轨，只像画笔一样沿圆弧匀速画出已经过的部分。
- 内圈 12 点位置用数字圆标显示总运动阶段数；一个运动阶段包含 work+rest，完成阶段节点显示数字或圆点。
- 中心圆表达当前阶段图标、阶段编号或名称、当前阶段倒计时，并承担暂停 / 继续主交互。
- E10.8 production 底部只保留跳过 / 下一阶段、`+15s` 和结束等少量操作；结束训练仍需要二次确认。Reset 只属于 preview/demo 或未来命令设计，生产实现前必须明确 `WorkoutCommand`、确认和 session record 边界。

E14.4-2b 的下一版 Timer Dial 语义必须保持当前已确认的圆盘 UI：轮次 / 轮间休息、内圈总进度、中心圆暂停 / 继续和既有 12 点数字圆标不作为本轮重设计对象。增量只在外圈：当当前 stage group 包含多个内部 targets 时，外圈按这些 targets 的 planned duration ratio 分段，active target 为粗弧，已完成 target 退为细弧 / 已经过弧；切换到下一个 stage group 时，外圈切换到该 stage group 自己的 target 结构。内圈仍表达整次训练总进度；中心圆仍表达当前 active target / stage、阶段剩余时间和暂停 / 继续主控制。12 点数字圆标继续沿用整次执行 timeline 的总阶段数语义，按 warmup + rounds * stageGroups + between-round rests + cooldown 计算。该语义进入生产前必须通过独立实现 story 和兼容测试确认，不能混入普通 UI polish。

Timer Dial 动效必须来自 engine state / UI state / `WorkoutEvent`，不能使用视觉假进度。阶段弧线推进、总进度推进、work / rest 颜色和粗细变化、阶段切换、暂停态和最后 N 秒提醒都应服从真实训练状态和用户 cue settings。休息延长后，当前 rest 外圈弧和内圈 work+rest cycle progress 必须单调、不倒退，并在 active tick 继续推进；paused、completed 和 abandoned 状态不继续动画。

E14.6 真机反馈补充：如果 normal motion 下外圈或当前 active segment 出现每秒前跳再回弹，必须拆 E14.6-1 单独修复。该修复只处理 progress monotonic / continuous behavior，不改 outer-ring semantic mapping、Canvas geometry、engine、timeline、Room、session records、commands 或 events。内部阶段圆环下的浅色承托圆环可以在后续 visual polish 中稍微加粗，但不得和 progress rebound fix 混为同一代码修复，除非后续 story 明确允许。

E15-5 真机反馈补充：若 1s / 2s 短 target 仍出现一小段加速感，后续修复必须证据先行。实现前先用生产执行页中的 1s / 2s timed composition 计划采集 frame / screenrecord 或等价采样，证明 active segment displayed progress 的 delta 是否稳定；不能只靠修改 projection helper 或单元测试断言宣称视觉已修复。该修复仍不得改变 outer-ring planned-duration ratio、Canvas geometry、inner total progress、12 点数字圆标、engine timeline、Room、session record、commands/events、声音或心率边界。

E14.6-3 / E14.6-3a stage style / icon planning 补充：Timer Dial 中心圆图标使用 active target `iconKey`，缺失时回退到 stage group icon，再回退到 `warmupStyle` / `cooldownStyle` / `restBetweenRoundsStyle` 或阶段类型默认 icon；中心圆和外圈颜色使用 target -> stageGroup -> boundary style -> type default fallback。Warmup、cooldown 和 synthetic between-round rest 即使不是 stageGroup targets，也应拥有自己的默认颜色和 icon。轮数不产生颜色或 icon。该规划不得改变 E14.4-2b-6 的外圈 planned-ratio 语义、内圈总阶段语义、12 点数字圆标、`+15s` rest extension 语义或 E14.5 continuous projection 边界。

后续可以探索赛博霓虹、Official Flow、Tile Flow 和 Big Type 的 Timer Dial 适配，但 MVP 不新增第四套 skin。先定义圆盘语言，再讨论它如何融入 TrainFlow 风格。

## Motion

TrainFlow 的动效只服务训练节奏和可操作性，不作为炫技层。运动中的关键控制反馈优先于完整播放；用户二次点击、暂停、跳过、结束确认、`+15s` 二段确认等操作不得被动画阻塞。

### Motion Tokens

- **Touch feedback (`100ms`, range `80-120ms`):** 按下、松开、中心圆轻微响应、按钮轻微缩放。缩放建议为 `0.97`，只表达“已接收到触摸”，不制造弹跳表演。
- **State transition (`160ms`, range `120-180ms`):** play/pause、`确认+15s` / `已加+15s`、阶段颜色切换、marker 状态变化。用于状态确认，必须可被新状态立即打断。
- **Local layout transition (`220ms`, range `180-240ms`):** ready gate -> execution、paused -> active、局部控制显隐、训练页内部小范围布局切换。
- **Page transition (`260ms`, range `220-300ms`):** 计划详情 -> ready gate、ready gate -> execution、execution -> summary。页面切换不应慢到让用户怀疑点击未生效。
- **Continuous projection (`max 1000ms`):** Timer Dial 秒间连续进度可持续投影，但只消费 UI state / engine state，不更新真实倒计时、不写 session record、不改变 `WorkoutCommand` / `WorkoutEvent`。

默认 easing 使用 `cubic-bezier(0.16, 1, 0.30, 1)`，让状态落位明确；强调反馈可用 `cubic-bezier(0.34, 1.16, 0.64, 1)`，但仅用于短促状态强调；持续进度必须使用 linear projection，避免真实时间和视觉速度产生偏差。

### Motion Rules

- 动画必须状态驱动、可中断。新 UI state 到达时，旧动画应取消或 snap 到新状态。
- 动画不得驱动 engine state、倒计时、session record、暂停时长、额外休息记录或业务分析字段。
- 训练中主控制的反馈必须比视觉完整播放优先；暂停 / 继续、跳过、结束、`+15s` 确认等二次点击不能等待动画结束。
- reduce-motion 默认降级为 `0ms` snap，关闭非必要动效和 continuous projection；文案数字仍消费真实 UI state。
- paused、completed、abandoned、ready gate 未启动等状态不得继续推进持续进度动画。
- 最后 N 秒提醒可以更强，但仍应短促、克制，并遵守用户 cue settings；声音、震动和动画都消费 `WorkoutEvent` / UI state，不互相伪造事件。

E10.16 已把 motion token 最小落地到计时训练关键交互：ready gate -> execution 使用局部布局切换；ready gate center circle、Timer Dial center dial 和 `+15s` 使用短触摸反馈；Timer Dial play/pause glyph、marker / ring alpha、center color / border 和 `+15s` label 使用 state transition。E14.2 后，rest extension 二段确认态文案为 `确认+15s`，触摸反馈只能缩放按钮内容或文字，不得改变底部三个按钮外框高度或挤压相邻按钮。E10.16 review fix 后，Android root composition 会基于系统 animator / transition / window animation scale 提供 `LocalTrainFlowReduceMotion`；生产 ready/execution、ready touch、Timer Dial alpha / color / play-pause / touch、`+15s` label / touch 和 final countdown pulse 都消费该值。reduce-motion 为 true 时，非必要 scale / pulse / fade snap 或关闭，Timer Dial continuous projection 不启动 frame loop，只显示 engine / UI state 的真实秒级进度。动效仍只消费 UI state / engine state，不驱动 engine、倒计时、session record、`WorkoutCommand`、`WorkoutEvent`、`pausedElapsedSec` 或 extra rest。E10.16 不实现完整无障碍设置系统、大型页面转场系统、Stage color picker、声音播放、统计图表、真实心率设备、foreground service、exact alarm、notification action、reset production command 或第四套 skin。

## Open Source UI Customization

TrainFlow 允许社区替换主题、首页布局、按钮位置和页面组合，但必须遵守核心契约：

- 不改变 `WorkoutCommand` 的语义。
- 不改变 `WorkoutEvent` 的语义。
- 不绕过训练执行引擎直接写 session。
- 不删除必要的安全和权限说明。
- 不把医疗化心率告警伪装成首版能力。

推荐社区自定义集中在 UI shell、主题 token、组件样式和 feature 页面组合层。

## UI Polish Handoff

E14.4 起，任何功能级 UI 优化都必须先提交视觉方案，再进入代码实现。视觉方案阶段默认只产出 Markdown、mock、截图标注或 layout spec；必须包含当前问题、至少两个方案方向、推荐选择、真机确认点和后续实现拆分。未经过用户确认前，不改 Kotlin / Compose / Room / 测试代码，不生成实现 APK，也不把方案直接落到生产 UI。

## Do's and Don'ts

### Do

1. 训练中优先展示当前动作、时间、组目标和主控制。
2. 所有颜色、字号、间距和圆角优先使用 token。
3. 让高级设置可展开，但不阻塞开始训练。
4. 保持健康数据、语音、媒体和 AI 能力的视觉层级克制。
5. 开源主题可以有个性，但要保留运动中可读性。

### Don't

1. 不把首页做成营销落地页。
2. 不用大面积装饰渐变或光斑遮蔽训练信息。
3. 不在训练执行页同时强调多个主按钮。
4. 不把健康数据展示写成医疗判断。
5. 不让社区 UI 改动绕开核心训练引擎。

### Correct Example

```kotlin
TrainingPrimaryButton(
    onClick = onCompleteSet
) {
    Text("完成本组")
}
```

### Incorrect Example

```kotlin
Button(
    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
    shape = RoundedCornerShape(28.dp),
    onClick = onCompleteSet
) {
    Text("完成本组")
}
```

这会绕过 token，并把训练主动作变成不受控的视觉风格。
