# E10.5 Timer Dial Design Workflow

**状态:** E10.5 docs-only planning story
**日期:** 2026-06-09
**范围:** Timer Dial 圆盘视觉语言重构的工具路线、研究边界、视觉规格、动效规格和后续实现拆分
**不包含:** 生产 Kotlin、Jetpack Compose 实现、Gradle、prototype、Room/session repository、统计图表、心率设备、语音/TTS、音频资源、foreground service、exact alarm、notification action、第四套 skin

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

## 5. Timer Dial 视觉规格

### 5.1 页面结构

计时训练执行页仍遵守 E10.1 主操作原则：当前阶段、当前倒计时和主控制最高层级，心率、提示和下一步保持辅助。

从上到下建议：

1. 顶部：本次训练总剩余时间。
2. 中部：Timer Dial 圆盘主视觉和主控制区。
3. 底部：少量图标操作。

### 5.2 外圈

外圈表达本轮或当前运动结构：

- `warmup`、`work`、`rest`、`cooldown` 使用不同颜色区分。
- `work` 弧线更粗，表示主要训练负荷。
- `rest` 弧线更细，避免休息和工作同权。
- `warmup` 与 `cooldown` 可使用更克制的辅助颜色。
- 当前阶段弧线有进度动画。
- 外圈可表达阶段位置和阶段切换，但不展示动作库教学信息。

### 5.3 内圈

内圈表达整次训练总进度：

- 进度只来自 `TimedWorkoutEngine` / 计时 UI state 的真实推进。
- 总进度弧线持续推进，暂停时冻结。
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

- 重置。
- 跳过 / 下一阶段。
- 结束。

底部操作应使用图标和可访问标签表达，文字尽量少。结束训练仍需要二次确认。

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
- `work` 到 `rest` 的变化应短促清晰，避免整页闪烁过强。
- 最后 N 秒提醒可增强弧线、中心数字或节奏，但必须服从用户 cue settings。
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

范围：

- Compose Canvas 圆盘。
- 外圈阶段弧线和内圈总进度。
- engine state / UI state 映射。
- 暂停、继续、阶段切换和最后 N 秒动效验证。

不改 Room/session repository，不做统计图表、声音、语音或真实设备接入。

### E10.8 Timer Dial production integration and animation polish

目标：把经过验证的 Timer Dial 纳入计时训练生产执行页，并完成动画 polish。

范围：

- 替换或升级现有计时训练大圆盘执行页。
- 适配三套内置 skin。
- 补齐回归测试和小屏可达性检查。
- 保持 `WorkoutCommand` / `WorkoutEvent` / `TimedWorkoutEngine` 语义不变。

不新增第四套 skin，不混入 E12 统计图表或 E13 音频能力。

### E13 声音 / 女声 cue 与视觉提醒联动

固定阶段词、声音提醒、女声 cue、音频共存和不 ducking 进入 E13。E10.5 只定义视觉提醒与事件消费边界。

### E12 统计图表 / 历史趋势

总统计、图表、计划趋势、平均心率趋势和历史记录清理进入 E12，不混入 E10.5 / E10.6 / E10.7 / E10.8。

## 9. 验收标准

E10.5 完成时应满足：

- 文档明确 E10.4 已完成并合入 `main`，E10.5 不再处理记录闭环。
- 文档明确外部 APK / 截图只做研究，不复制代码或资产。
- 文档明确 Figma、HTML / Canvas、Jetpack Compose Canvas、Rive / Lottie 和 APK 观察的工具边界。
- 文档明确 Timer Dial 的顶部、外圈、内圈、中心圆和底部操作规格。
- 文档明确动效必须来自 engine state，不能使用视觉假进度。
- 文档明确黑红高对比只是参考方向，不新增第四套 skin。
- roadmap/backlog 拆出 E10.6、E10.7、E10.8，并把 E12 / E13 保持在各自阶段。
