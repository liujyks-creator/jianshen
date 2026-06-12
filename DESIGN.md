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
- **Info:** 心率连接状态、设备说明、帮助提示。

### Color Rules

- 正文和关键倒计时必须满足 WCAG AA 对比度。
- 不使用单一绿色铺满所有页面；绿色只承担确认和品牌路径。
- 训练执行页中，`{colors.action}` 的使用要短促、有目的，避免整页橙色造成紧张。
- 心率状态不能使用医疗化红色警报，除非未来有明确非医疗提示规则。

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
- 训练执行页的主数字不能被心率、次级标签或装饰元素抢占。
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
- 辅助心率状态位。

主倒计时和主按钮的层级永远高于心率和历史信息。

E10.1 后，计时训练后续按纯间歇计时器处理，执行页主信息是当前阶段、阶段倒计时和大圆盘主控制区，不再要求展示动作库动作或动作详情入口。

### Timer Dial

E10.5 后，计时训练大圆盘进一步收敛为 Timer Dial 圆盘视觉语言。Timer Dial 可以参考黑红高对比的运动现场感，但必须使用 TrainFlow 自己的 token、图标语义、弧线层级和动效规则，不复制外部 APK / 截图的代码、资源、图标、字体、音频、专有动画或逐像素视觉。

Timer Dial 的设计结构：

- 顶部显示本次训练总剩余时间，但低于中心倒计时层级。
- 外圈表达当前一次运动+休息周期；当前阶段弧线按线性动画匀速填充。
- 处于 `work` 阶段时，`work` 为粗弧、同周期 `rest` 为细弧；处于 `rest` 阶段时，`rest` 为粗弧、已完成 `work` 退为细弧。
- 内圈表达按运动阶段数量推进的整次训练总进度，不画未经过底轨，只像画笔一样沿圆弧匀速画出已经过的部分。
- 内圈 12 点位置用数字圆标显示总运动阶段数；一个运动阶段包含 work+rest，完成阶段节点显示数字或圆点。
- 中心圆表达当前阶段图标、阶段编号或名称、当前阶段倒计时，并承担暂停 / 继续主交互。
- E10.8 production 底部只保留跳过 / 下一阶段、`+15秒` 和结束等少量操作；结束训练仍需要二次确认。Reset 只属于 preview/demo 或未来命令设计，生产实现前必须明确 `WorkoutCommand`、确认和 session record 边界。

Timer Dial 动效必须来自 engine state / UI state / `WorkoutEvent`，不能使用视觉假进度。阶段弧线推进、总进度推进、work / rest 颜色和粗细变化、阶段切换、暂停态和最后 N 秒提醒都应服从真实训练状态和用户 cue settings。休息延长后，当前 rest 外圈弧和内圈 work+rest cycle progress 必须单调、不倒退，并在 active tick 继续推进；paused、completed 和 abandoned 状态不继续动画。

后续可以探索赛博霓虹、Official Flow、Tile Flow 和 Big Type 的 Timer Dial 适配，但 MVP 不新增第四套 skin。先定义圆盘语言，再讨论它如何融入 TrainFlow 风格。

## Open Source UI Customization

TrainFlow 允许社区替换主题、首页布局、按钮位置和页面组合，但必须遵守核心契约：

- 不改变 `WorkoutCommand` 的语义。
- 不改变 `WorkoutEvent` 的语义。
- 不绕过训练执行引擎直接写 session。
- 不删除必要的安全和权限说明。
- 不把医疗化心率告警伪装成首版能力。

推荐社区自定义集中在 UI shell、主题 token、组件样式和 feature 页面组合层。

## Do's and Don'ts

### Do

1. 训练中优先展示当前动作、时间、组目标和主控制。
2. 所有颜色、字号、间距和圆角优先使用 token。
3. 让高级设置可展开，但不阻塞开始训练。
4. 保持心率、语音、媒体和 AI 能力的视觉层级克制。
5. 开源主题可以有个性，但要保留运动中可读性。

### Don't

1. 不把首页做成营销落地页。
2. 不用大面积装饰渐变或光斑遮蔽训练信息。
3. 不在训练执行页同时强调多个主按钮。
4. 不把心率展示写成医疗判断。
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
