# E10.5 Timer Dial Design Workflow

**状态:** E10.5 docs-only planning story；E10.6-E10.12 已落地；E10.12 已完成 Android Compose landing
**日期:** 2026-06-13
**范围:** Timer Dial 圆盘视觉语言重构的工具路线、研究边界、视觉规格、动效规格、E10.9 后续视觉修复、huashu HTML 原型和 E12/E13 分流
**不包含:** 本轮不实现生产 Kotlin、Jetpack Compose 修复、Gradle、prototype、Room/session repository、统计图表、心率设备、语音/TTS、音频资源接入、foreground service、exact alarm、notification action、第四套 skin

## 1. 背景与当前状态

E10.4 已完成 Review Gate PASS 并 fast-forward 合入 `main`。TrainFlow 现在已经具备本地真实 `WorkoutSession` write-through：计时、力量和基础跟练 completed / abandoned 终态可以写入本地 Room session records，记录页生产入口读取真实记录。

因此 E10.5 不再处理记录闭环，不改 Room、DAO、session repository、session write guard、历史读取或记录页数据源。E10.5 的目标是把用户喜欢的 Timer Dial 圆盘视觉语言重新定义为 TrainFlow 自己的设计与后续实现路线。

## 2. E10.5 目标

E10.5 只做规划和设计范围收口：

1. 明确 Timer Dial 是计时训练执行页的视觉语言重构方向，不是新的训练模式。
2. 将外部 APK / 截图仅作为观察和学习 UI / 交互的研究素材。
3. 禁止复制外部代码、资源、图标、字体、音频、命名、专有动画资产或逐像素视觉。
4. 定义 TrainFlow 自己的圆盘结构、颜色层级、信息层级和动效语义。
5. 将静态设计、动画验证、Compose 原型和生产集成拆成后续 story。

## 3. 研究与资产边界

允许：

- 本地静态或运行观察 APK 的页面结构、信息层级、交互节奏和用户偏好点。
- 记录研究结论到文档。
- 用自己的 token、组件、图标语义和动画策略重建 TrainFlow 视觉语言。

禁止：

- 解析、反编译、复制或移植 APK 代码。
- 提交 APK、截图、录屏、反编译输出、日志或临时研究产物。
- 复制外部图标、字体、音频、位图资源、专有动画或逐帧动效。
- 因外部参考新增不可用功能或破坏 TrainFlow 已有训练命令 / 事件 / 数据契约。

当前根目录 APK `Workout Timer - HIIT Tabata v1.2.55 [FileCR].apk` 只能作为本地研究素材，不得 stage、提交、移动或复制进 Git。

## 4. 工具路线

| 工具 | 用途 | 边界 |
|---|---|---|
| Figma | 静态界面、风格方案、颜色 / 组件 / 布局规格 | 用于定稿视觉语言和多方案比较，不替代产品决策。 |
| HTML / Canvas | 可选快速验证圆盘动画、阶段弧线节奏和最后 N 秒提醒节奏 | 只做探索，不作为生产实现，也不提交无关产物。 |
| Jetpack Compose Canvas | 最终 Android 生产实现方式 | 圆盘、弧线、进度、点击暂停 / 继续都要绑定真实 engine state。 |
| Rive / Lottie | 小图标、装饰微动效或阶段切换辅助 | 不用于核心计时进度，因为进度必须实时绑定 engine state。 |
| APK 静态 / 运行观察 | 研究信息架构和交互节奏 | 只做观察记录，不提交 APK、截图、录屏或反编译输出。 |

E10.11 已使用 `huashu-design` skill 做 3 个 HTML 高保真 Timer Dial 原型方向：黑红高对比、TrainFlow Official 融合、赛博霓虹。E10.12 已选择其中最适合作为生产候选的 `TrainFlow Official Fusion` 落到 Android Compose Timer Dial；Black / Red 和 Cyber Neon 继续只作为探索 / preview 方向，不作为默认生产 UI。

## 5. Timer Dial 视觉规格

### 5.1 页面结构

计时训练执行页仍遵守 E10.1 主操作原则：当前阶段、当前倒计时和主控制最高层级，心率、提示和下一步保持辅助。

从上到下建议：

1. 顶部：本次训练总剩余时间。
2. 中部：Timer Dial 圆盘主视觉和主控制区。
3. 底部：少量图标操作。

### 5.2 外圈

外圈表达当前一次运动+休息周期，不表达整次训练中所有运动+休息阶段，避免和内圈总进度重复：

- 处于 `work` 阶段时，当前 `work` 弧线为粗弧并按阶段进度从空到填充，同周期 `rest` 弧线为细弧。
- 处于 `rest` 阶段时，当前 `rest` 弧线为粗弧并按阶段进度从空到填充，已经过的同周期 `work` 弧线退为细弧。
- `warmup`、`cooldown` 和 `custom` 可按当前阶段单段或相邻结构展示，但仍不能扩展成整次训练的全部阶段列表。
- `warmup` 与 `cooldown` 可使用更克制的辅助颜色。
- 当前阶段弧线有进度动画。
- 外圈可表达阶段位置和阶段切换，但不展示动作库教学信息。

### 5.3 内圈

内圈表达按运动阶段数量推进的整次训练总进度：

- 进度只来自 `TimedWorkoutEngine` / 计时 UI state 的真实推进。
- 一个运动阶段包含 work+rest；例如 7 个 work 阶段和 7 个 rest 阶段时，内圈总阶段数为 7。
- 12 点位置用数字圆标显示总运动阶段数。
- 未经过部分不画底轨；已经过部分像一支笔从 12 点开始沿圆弧匀速画出。
- 当前画笔点直径略大于内圈弧线粗度。
- 每完成一个运动阶段，在对应圆弧节点显示完成标记：最新完成节点可显示数字，之前完成节点退为实心圆点。
- 总进度弧线持续线性推进，暂停时冻结。
- 终态 completed / abandoned 后不继续视觉推进。

### 5.4 中心圆

中心圆表达当前阶段，并承担暂停 / 继续主交互：

- 阶段图标。
- 阶段编号或阶段名称。
- 当前阶段倒计时。
- 点击中心圆暂停 / 继续。
- 暂停态要明确显示，不让用户误以为计时仍在推进。

中心圆不展示动作详情、动作库短提示、心率主值或统计图表。

### 5.5 底部操作

底部只保留少量图标操作：

- 跳过 / 下一阶段。
- `+15s`（仅 active rest；记录 / 总结可继续用中文秒数）。
- 结束。

E10.8 production controls 收敛为 `skip`、`+15s`、`end`。Reset 只保留在 preview / demo 或未来命令设计讨论中；如果后续进入生产实现，需要先明确 `WorkoutCommand` 语义、二次确认、本次 session record 边界和回归测试。底部操作应使用图标和可访问标签表达，文字尽量少。结束训练仍需要二次确认。

## 6. 动效规格

动效必须来自 TrainFlow engine state，不允许视觉假进度。

必须覆盖：

1. 阶段弧线推进。
2. 总进度弧线推进。
3. `work` / `rest` 切换时颜色和粗细变化。
4. 阶段切换动效。
5. 暂停态动效。
6. 最后 N 秒提醒动效。

动效原则：

- `active` 时推进；`paused` 时冻结并显示暂停状态。
- 休息延长后的外圈当前 rest 弧和内圈 work+rest cycle 进度必须保持单调、不倒退，并继续由 active elapsed / UI state 线性推进。
- `work` 到 `rest` 的变化应短促清晰，避免整页闪烁过强。
- 最后 N 秒提醒可增强弧线、中心数字或节奏，但必须服从用户 cue settings。
- 外部 APK / 人工分析只作为观察材料；可吸收深色高对比、单一强强调色、约 100-300ms 轻量反馈和 final countdown 轻量强调等抽象原则，不复制代码、资源、字体、音频、命名或逐像素视觉。
- 动效消费者使用 `WorkoutEvent` / UI state，不直接发明独立计时。
- 声音、震动和未来固定女声 cue 留给 E13，不在 E10.5 实现。

## 7. 主题与风格

黑红高对比可作为参考方向，但不是直接复制目标。E10.5 先定义圆盘语言，再讨论如何融入 TrainFlow 官方风格和三套内置 skin。

后续可探索：

- 赛博霓虹：更强高对比和运动现场感。
- Official Flow：贴合现有 `DESIGN.md` 的冷静底色、清爽确认色和运动动作色。
- Tile Flow：圆盘嵌入磁贴式工作区，但执行页不能碎片化。
- Big Type：远距离可读，中心倒计时和主控制更大。

本阶段不新增第四套 skin。后续方案只能适配或扩展 Official Flow、Tile Flow、Big Type 的表现，不改变 skin registry 的 MVP 边界。

## 8. 后续拆分建议

### E10.6 Timer Dial Figma / static visual variants

目标：输出 Timer Dial 静态视觉方案和规格。

状态：E10.6 已记录到 `docs/planning/timer-dial-static-visual-variants.md`，仍为 docs-only design specification，不实现 Android、不写 Kotlin、不改 Gradle、不改 prototype。

范围：

- Figma 静态页面。
- Official Flow、Tile Flow、Big Type 的适配说明。
- 黑红高对比、赛博霓虹和 TrainFlow Official Flow 方向的对比。
- 颜色、弧线厚度、中心圆、图标、底部操作和暂停态规格。
- 执行页 active work、active rest、warmup / cooldown、paused、resume transition、stage transition、last-N-seconds cue、completed、abandoned、end confirmation 和 720x1280 小屏状态帧。
- 计时编辑页 header、阶段列表、阶段行 / 阶段卡、添加、复制、删除、颜色 / 图标 picker、快捷时长、时长细调、展开 / 收起、拖动排序、上移 / 下移、保存 / 取消和小屏底部操作状态帧。

不实现 Android 生产 UI。

### E10.7 Timer Dial Compose prototype

目标：用 Jetpack Compose Canvas 做可运行原型，验证圆盘绘制、点击控制和状态映射。

状态：E10.7 已实现 prototype complete。Android `feature.workoutsession` 中新增 `TimerDialUiState`、`TimerDialTokens`、`TimerDial` Canvas 组件和 `TimerDialPreview` demo；计时执行页以低风险方式消费该 prototype，中心点击仍映射既有 pause / resume callback，不改变 `TimedWorkoutEngine` 或 `WorkoutCommand` 语义。

范围：

- Compose Canvas 圆盘。
- 外圈阶段弧线和内圈总进度。
- engine state / UI state 映射。
- 暂停、继续、阶段切换和最后 N 秒动效验证。
- 三类 prototype visual variant：黑红高对比、赛博霓虹、TrainFlow Official Flow 融合。它们不是新增第四套 skin。
- 单元测试覆盖 progress clamp、total / stage progress mapping、work/rest stroke semantics、visual variant token 数量、final countdown flag 和 paused state mapping。

不改 Room/session repository，不做统计图表、声音、语音或真实设备接入。本阶段不是 E10.8 production integration；最终小屏、TalkBack、reduce-motion、三套内置 skin polish 和生产行为收口仍在 E10.8。

### E10.8 Timer Dial production integration and animation polish

目标：把经过验证的 Timer Dial 纳入计时训练生产执行页，并完成动画 polish。

状态：E10.8 已在 `codex/e10-8-timer-dial-production-polish` 分支实现，等待 review。

范围：

- 计时训练生产执行页默认使用 Official Flow Timer Dial；Black / Red High Contrast 与 Cyber Neon 仅保留为 preview/demo visual variants，不进入 UI skin registry。
- 外圈从“整次训练全部阶段”收窄为当前一次运动+休息周期；内圈继续表达整次训练总进度。
- 中心圆承担暂停 / 继续主交互；顶部只保留总剩余时间；底部跳过和结束改为图标，结束训练仍走二次确认。
- `+15秒` 仅在 active rest 可用，用于延长当前休息 15 秒，不修改原计划。
- Timer Dial 进度按“单调、不倒退、状态驱动”验收；rest extension 后外圈当前 rest 弧和内圈当前 work+rest cycle progress 不得回到 0 或小于延长前值。
- Reset remains preview/demo or future command design. E10.8 production does not add reset; future reset work must define command semantics, confirmation, session-record boundaries, and tests before entering production.
- 补齐回归测试和 720x1280 emulator visual smoke；最后 N 秒视觉 smoke 已尝试捕获，但本轮未稳定截到 1-5 秒窗口，单元测试覆盖提醒 flag 与偏好开关。
- 保持 `WorkoutCommand` / `WorkoutEvent` / `TimedWorkoutEngine` 语义不变。

不新增第四套 skin，不混入 E11 心率、E12 统计图表或 E13 音频能力。

### E10.9 Timer Dial reference polish / continuous progress / user-test APK

目标：补齐 E10.8 后的参考风格 polish 和秒间连续进度，让用户测试 APK 中的圆环在 engine 秒级 tick 之间也保持流动。

状态：E10.9 已在 `codex/e10-9-timer-dial-reference-polish` 分支完成并推送；`r-design.md` 作为参考桥接文档纳入分支，不替代官方 `DESIGN.md`。

范围：

- Active 状态下使用 Compose frame clock 做最多当前 1 秒的 bounded progress projection。
- 中心倒计时文本仍只来自 engine / UI state 的秒级文本。
- Paused、completed、abandoned 和不可暂停 / 继续状态不推进投影。
- `+15秒` rest extension 后当前 rest 外圈弧和内圈 work+rest cycle progress 保持单调不倒退。
- 生成 user-test debug APK。

边界：

- 不改 Room/session repository、训练引擎语义、记录统计、心率设备、foreground service、exact alarm、notification action、声音 / TTS / 女声 cue、前端 prototype 或第四套 skin。
- 不提交外部 APK、`人工/`、`.local/`、build 输出、截图、日志或音频资源。

### E10.9 Review Fix / User Test Fix

目标：针对用户测试反馈继续修复 Timer Dial 视觉层级，不改变 E10.8/E10.9 的训练语义。

范围：

- 减少执行页文字，移除或弱化“总剩余”、下一阶段提示框、已启用声音提示框等。
- 总剩余时间更大、更居中；圆盘整体更大。
- 外圈 / 内圈线条同比例变细，避免 marker 与外圈视觉重叠。
- 增加内圈总进度线下方的宽底层圆环。
- 底层圆环浅色小点复用内圈阶段 marker 的动态角度计算，随阶段数量、时长和轮次变化。
- 中心圆只保留阶段图标、必要编号和当前阶段时间。
- 中心圆填充使用阶段预设色，文字和图标使用白色。

必须保留：

- Continuous progress。
- Pause freeze。
- Terminal freeze。
- Rest extension monotonic progress。
- Production controls: skip、`+15秒`、end；reset 仍不是生产控制。

不包含：

- 不接入声音播放。
- 不实现计划保存。
- 不做统计图表。
- 不复制外部 APK 或参考项目资源。

### E10.10 Plan Persistence

计划保存持久化不是 Timer Dial 视觉工作，但来自同一轮用户测试反馈。它应单独 story 处理自定义计时训练阶段、秒数、轮次、颜色、图标、名称、排序的保存与恢复，并 audit 计时、力量、跟练计划保存入口是否真实可用。Timer Dial 视觉 story 不实现计划保存。

### E10.11 Huashu Timer Dial HTML prototype

已使用 `huashu-design` skill 做 3 个 HTML 高保真 Timer Dial 原型方向：

- 黑红高对比。
- TrainFlow Official 融合。
- 赛博霓虹。

每个方向覆盖 active、rest、paused、final 5 seconds 和 rest extension。原型不复制外部 APK 或参考项目代码、资源、图标、字体、音频、命名、动效参数或逐像素视觉，不修改 Kotlin/Gradle/prototype，不接入声音，不实现计划保存。

### E10.12 Timer Dial Compose landing

E10.12 已把 E10.11 中的 `TrainFlow Official Fusion` 方向落到 Android Compose 生产 Timer Dial。

落地内容：

- 计时执行页移除“总剩余”文字标签、下一阶段提示框、提醒说明 / 已启用声音提示框和训练中控制历史提示。
- 顶部总剩余时间更大、更居中；Timer Dial 圆盘整体放大以适配 720x1280 小屏和常规屏。
- 外圈 / 内圈线条同比例变细，marker 与外圈 / 中心圆保持清晰间距。
- 内圈总进度线下方增加宽底层浅色圆环；底层浅点和阶段 marker 复用 `TimerDialUiState` 的同一套动态 marker 数据。
- 中心圆使用当前阶段色填充，内部只保留白色图标、必要编号和阶段剩余时间。
- `final 5 seconds` 继续使用轻量中心/边界强调，不做全屏强提醒。
- `paused` 状态冻结进度，并用低饱和阶段色和虚线边界克制表达。
- `+15秒` rest extension 后外圈和内圈 progress 继续单调不倒退。
- Review fix 已将所有内置 skin 的 marker 轨道重新布局到中心圆和总进程外圈之间，并用 `3.5dp` 最小间距测试约束 center gap、outer gap 和 marker 内部浅点边界；暂停态中心圆继续作为整块可点击的“继续训练”入口，只保留继续图标、当前编号和当前阶段倒计时。

边界：

- 颜色来自 TrainFlow skin token、Timer Dial token 和 UI state；Official 默认不硬编码 E10.11 HTML 原型色值。
- 保留 E10.9 active Compose frame clock continuous progress、秒级文案 tick、paused / terminal freeze、最多投影当前 1 秒和 rest extension monotonic progress。
- 不接入声音播放，不复制 `countdown_beep1.mp3` 或 `.local/audio/stage_bell_copper_clean.mp3` 到 `res/raw`。
- 本轮 review fix 不实现 ready/start gate、阶段颜色 picker、motion timing rules、计划保存、统计图表，不改 Room/session repository，不接真实心率设备，不新增 foreground service、exact alarm、notification action、reset production command 或第四套 skin。

### E13 声音 / 女声 cue 与视觉提醒联动

固定阶段词、声音提醒、女声 cue、音频共存和不 ducking 进入 E13。E10.5 只定义视觉提醒与事件消费边界。E13 需要记录本轮素材边界：`countdown_beep1.mp3` 用于 5 / 4 / 3 / 2 / 1 等最后 N 秒 beep；`.local/audio/stage_bell_copper_clean.mp3` 用于倒数到 0 后下阶段开始铃声候选，后续接入 App 时由执行 story 复制到 `app/src/main/res/raw/`，`.local` 原文件不提交。E13 还必须覆盖手机扬声器和蓝牙耳机 smoke，不主动 ducking，不请求会打断外部音乐 / 视频的 audio focus。

### E13.1 Timer Dial pause / resume morph 回归基准

E13.1 sound cue/media fix 的用户确认版同时收口了计时执行页运行态、暂停态、声音和 App 图标的人工体验基准。E11.3 后心率卡片已撤销；后续修改 Timer Dial 动画、暂停页、底部控制或声音交互时，必须回看以下 4 个点，避免把用户已确认的体验回退：

1. 运行 -> 暂停：Timer Dial 外圈、节点和进度弧应有向中心收束并落到暂停大圆盘的连续感，不能退回整屏硬切或只有 page crossfade。
2. 暂停 -> 运行：暂停大圆盘应反向缩回并展开回 Timer Dial，运行态进度继续消费真实 engine / UI state，不伪造进度。
3. 布局稳定性：左上角显示训练 / 计划名称，不显示阶段数字；不再出现心率卡片，底部按钮、总剩余时间和 Timer Dial 不互相挤压，底部按钮在真机上完整可见。
4. 声音与倒计时不回归：剩余 N / ... / 1 秒播放 `countdown_beep1.mp3`，倒数到 0 后播放 `stage_bell_copper_clean.mp3`；声音继续走媒体音量，不 duck、不暂停或抢占外部音乐 / 视频。

该基准是后续 animation polish / visual smoke 的验收输入，不要求每次都做新功能；若只调整视觉，也不得改变 `WorkoutCommand`、`WorkoutEvent`、engine、session record、raw 音频资源或心率数据源语义。

E14.2 真机截图比例修复补充：E11.3 移除心率后，计时执行页不能继续保留心率时代的纵向弹性节奏。总剩余时间应作为辅助块，Timer Dial 上方弹性空间必须小于下方弹性空间，底部 `skip` / `+15s` / `end` 控制保持至少 `48dp` 点击高度并位于导航栏安全区上方；Timer Dial Canvas 必须保持正方形约束，不能被 compact parent 压成椭圆；rest extension 二段确认态使用 `确认+15s`，按钮外框不得因文字状态切换变高或挤压相邻按钮。详见 `docs/testing/e14-2-timer-dial-real-device-proportion-restore.md`。

### E14 后续 UI 优化节奏

E14.2 完成前，不应启动跨功能 UI 重做；Timer Dial 真机比例、同心圆和底部确认态必须先由用户真机截图确认稳定。确认后建议进入 E14.3 `UI quality audit and polish sequencing`，只做审计、截图矩阵和优先级，不直接重写页面。E14.4 以后再按功能分批做 UI polish：先训练执行页共性控件，再计划编辑 / 计划详情，再记录页图表与历史清理，最后动作库和跟练雏形。每批 UI 优化都必须保留当前训练引擎、`WorkoutCommand`、`WorkoutEvent`、session record、心率撤销和健康数据边界。

E14.4-2a 已确认计划编辑 / 详情 polish 采用方案 B，但计时阶段内目标扩展 + TimerDial 外圈时间比例语义必须拆成独立 E14.4-2b。E14.4-2b visual / semantic gate 已记录在 `docs/testing/e14-4-2b-timed-composition-timerdial-semantics.md`，E14.4-2b-2 data model decision 已记录在 `docs/testing/e14-4-2b-timed-composition-data-model-decision.md`。该 gate 经用户校准后确认：当前计时编辑器 UI 不重做，顶部轮次与轮间休息保持现有位置，下面继续是阶段编排；新增能力只是在既有阶段内部扩展更多目标 / 小节，阶段总时长由内部目标时长求和。

E14.4-2b 的 TimerDial 语义结论是：圆盘整体沿用当前已确认 UI，不重新规划轮次、轮间休息、内圈总进度、中心圆或既有 12 点数字圆标。增量只在外圈：外圈表达当前 stage group 内部的 targets，并按 target planned duration ratio 分段；active target 用粗弧，已完成 target 退为细弧 / 已经过弧，阶段切换时外圈切换到下一个 stage group 的 target 结构。内圈继续表达整次训练总进度；中心圆继续表达当前 active target / stage、阶段剩余时间和暂停 / 继续主控制；12 点数字圆标继续沿用整次执行 timeline 的总阶段数语义，按 warmup + rounds * stageGroups + between-round rests + cooldown 计算，并在一次执行中保持稳定。`+15秒` 仍只延长当前 active rest step，不插入新阶段或 target，不修改 `WorkoutPlan` 或 plan snapshot，不改变 `timedRestExtensionRecords` 语义；外圈按 planned ratio 保持分段比例，不因 rest extension 重算 sweep，外圈和内圈 progress 必须保持 monotonic，不倒退。

E14.4-2b-1 visual prototype / mock 已在 `.local/smoke/e14-4-2b-timed-composition-timerdial-semantics/index.html` 验证该语义方向：编辑页保留轮次 / 轮间休息在顶部，阶段编排在下方，阶段展开后显示内部目标；颜色入口只显示色块，不在选择位置显示中文颜色名；TimerDial 保持原 UI，只让外圈按当前阶段内部目标时长比例分段。E14.4-2b-2 已进一步决定：采用 versioned timed composition payload，优先存入现有 `WorkoutPlan.blocks` JSON / `WorkoutSession.planSnapshot` JSON，不新增 Room table / column；旧计划通过 compatibility wrapper 显示和执行，查看不写回，用户明确保存 / 转换后才写入 composition v2。

E14.4-2b-1 TimerDial 纠偏补充：第一版独立 TimerDial mock 偏离了已确认生产 UI，只能作为语义草图，不作为后续视觉方向。本轮新增 `.local/smoke/e14-4-2b-timerdial-existing-ui-overlay/index.html`，以用户提供的绿色中心圆正常 TimerDial 截图 `C:/Users/25073/Downloads/Screenshot_2026-06-21-22-30-15-56_168a3d1b6f3b71..jpg`、E14.2 已确认比例和生产代码为基准，仅在外圈叠加当前阶段内部目标的时长比例分段。已补读 `TimerDial.kt`、`TimerDialTokens.kt`、`TimerDialUiState.kt` 和 `TimedWorkoutSessionRoute.kt`：生产 `OFFICIAL_FLOW` 的普通圆盘关系保持 `dialSizeDp = 320` / `centerSizeDp = 180` / `innerInsetDp = 42`，12 点数字圆标当前来自 `totalWorkoutStageCount`，外圈从 12 点方向按 duration-based sweep 绘制。`.local/smoke/e14-2-runtime/running.png` 的黄色中心圆视觉不是可继承基准；纠偏 mock 不继承 appbar、手机壳、说明文字、小按钮、新圆盘比例或新页面结构。

该 gate 比较了方案 A `UI-only compatibility wrapper`、方案 B `explicit timed composition model` 和方案 C `visual grouping only`，E14.4-2b-2 接受方案 B 作为长期数据目标。当前 `WorkoutPlan.blocks` / `TimedCircuitBlock` / `TimedExerciseItem` 可支持旧计划 wrapper，但不足以稳定表达阶段内部目标 id、目标颜色、嵌套目标、计划快照和历史趋势比较 key。后续实现应先做 serializer / model and editor adapter foundation，再分别实现 editor UI、TimedWorkoutEngine timeline mapping、TimerDial mapping 和 migration / compatibility / E12 tests。该工作不得混入普通 E14.4-2 plan polish，也不得静默修改 Room schema、训练引擎、命令、事件、session record、声音语义或统计比较 key；仅扩展 JSON payload 时不需要 Room schema migration，但仍必须做 serializer compatibility 和 old/new snapshot 测试。

E14.4-2b-3 / E14.4-2b-4 的旧本地实现未通过 review gate，已 rolled back / not accepted；restart 版已重新落地并推送：E14.4-2b-3 只接受 model / serializer / editor adapter foundation，E14.4-2b-4 只接受 editor UI visual/code gate。当前生产 TimerDial 仍不读取 composition v2，不读取 stageGroups / targets，也不改变外圈 mapping；v2 editor / plan detail 仍以 `待执行映射完成后可开始` 禁用开始训练。后续不得直接进入 E14.4-2b-6；必须先完成 E14.4-2b-5 docs-only planning gate，再做 E14.4-2b-5a timeline adapter model/tests，并在执行 timeline 和兼容边界通过后，才允许在保持当前 TimerDial UI、暂停 morph、continuous projection、reduce-motion、final countdown 和 rest extension monotonic progress 的前提下，把外圈数据源切到当前 stage group targets 的 planned duration ratio。

E14.4-2b-5 planning gate 补充：TimerDial 后续 mapping 不应直接解析原始 v2 JSON，而应消费 adapter-expanded timeline。内圈输入为稳定 stage instance total / completed / current progress，stage count 仍按 warmup + rounds * stageGroups + between-round rests + cooldown 计算；12 点数字圆标继续表达该内圈总阶段数，不改成 target 数。外圈输入为当前 active stage group 的 target segments，每个 segment 至少包含 targetId、targetKind、plannedDurationSec、displayName、colorHex、progress 和 isCurrent；warmup、cooldown、between-round rest 作为单 segment stage 处理。`+15s` 只延长当前 active rest target / synthetic rest step，外圈 planned ratio 不重算，progress 继续使用 monotonic floor。

### E12 统计图表 / 历史趋势

总统计、图表、计划趋势、同日多轮运动分析和历史记录清理进入 E12，不混入 E10.5 / E10.6 / E10.7 / E10.8 / E10.9。E11.3 后首版不再规划平均心率趋势。

## 9. 验收标准

E10.5 完成时应满足：

- 文档明确 E10.4 已完成并合入 `main`，E10.5 不再处理记录闭环。
- 文档明确外部 APK / 截图只做研究，不复制代码或资产。
- 文档明确 Figma、HTML / Canvas、Jetpack Compose Canvas、Rive / Lottie 和 APK 观察的工具边界。
- 文档明确 Timer Dial 的顶部、外圈、内圈、中心圆和底部操作规格。
- 文档明确动效必须来自 engine state，不能使用视觉假进度。
- 文档明确黑红高对比只是参考方向，不新增第四套 skin。
- roadmap/backlog 拆出 E10.6、E10.7、E10.8、E10.9 Review Fix、E10.10、E10.11、E10.12，并把 E12 / E13 保持在各自阶段。
