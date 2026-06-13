# E10.11 Huashu Timer Dial HTML Prototype

**状态:** prototype ready for review
**范围:** 只做 HTML/CSS/Canvas 视觉探索和说明文档，不修改 Android Kotlin / Gradle / React prototype，不接入声音，不实现计划保存。

## 原型入口

- `docs/prototypes/e10-11/index.html`

浏览器打开后，可用右侧方向切换查看三套视觉方向，并用右侧 review 状态按钮查看：

- active work
- rest
- paused
- final 5 seconds
- rest extended by `+15`

这些状态按钮只服务原型评审，用于快速切换 `TimerDialUiState` 的五个状态；它们不是训练执行页的生产 UI，也不应进入 Android Compose 生产界面。

## 三个方向

### Direction A: Black / Red High Contrast

黑场、红橙运动强调、白色总进度线。适合验证用户偏好的强训练现场感，也最接近“减少文字、靠颜色和圆盘读状态”的方向。建议把高对比层级、中心阶段色填充、final 5 seconds 的轻量脉冲作为候选，但不要把过强黑红情绪直接定为 MVP 默认。

### Direction B: TrainFlow Official Fusion

使用 `DESIGN.md` 的 official tokens：深色执行页、珊瑚橙 work、蓝色 rest、绿色 warmup。它是最适合进入 Android Compose 生产修复的方向，因为它保留 TrainFlow 官方设计系统，不新增第四套 skin。

### Direction C: Cyber Neon Restrained

使用克制的青色 / 红粉霓虹作为训练状态反馈，背景仍保持干净黑场。适合探索更强的运动科技感，但 glow、霓虹底色和 final countdown 强调都应视为探索，不建议直接进入 MVP 默认实现。

## 建议进入 Compose 生产实现的元素

- 顶部只显示更大、更居中的总剩余时间，不显示“总剩余”文字标签。
- 圆盘整体放大，同时外圈 / 内圈线条同比例变细。
- 外圈、内圈和中心圆应形成更紧凑的同心仪表盘比例；中心圆盘可以更大，内圈总进度应靠近中心圆，避免内部显得空洞。
- 进度圆圈、浅色底层圆环和阶段 marker 这一组应靠近最外侧阶段圆圈，只保留窄缝。
- 内圈总进度线下方增加更宽、浅、低对比的底层圆环，让总进度有足够承托。
- 底层圆环浅色小点和内圈阶段 marker 复用同一套动态角度计算，阶段数字 marker 和配套圆点应保持清楚可读，不应过度缩小。
- 中心圆使用当前阶段预设色填充，图标、编号和阶段时间使用白色。
- 中心圆减少文字，只保留图标、必要编号和当前阶段时间。
- `paused` 状态冻结圆环进度，并用低饱和遮罩或虚线边界表达“已停住”。
- `final 5 seconds` 使用轻量 pulse / border / color intensity，不做全屏强告警。
- `rest extended by +15` 使用 progress floor / cue 表达进度不倒退，而不是把当前 rest 弧重置。

## 探索但不建议直接进入 MVP 的元素

- Direction A 的强黑红情绪可作为评审方向，但默认 official skin 不应整体变成单一红黑主题。
- Direction C 的霓虹 glow 只适合低强度使用；过量会损害训练中可读性。
- 原型里的说明面板、方向切换和状态切换只服务 review，不是生产执行页 UI。
- `+15` 的视觉 badge 只是表达 rest extension 状态，不是新增 Android 命令。

## 映射到 `TimerDialUiState` / Compose Canvas

HTML 原型使用一套轻量数据结构模拟当前生产 UI state：

- `total` 对应 `TimerDialUiState.totalRemainingText`。
- `stageType` 对应 `currentStageType`，并映射到阶段预设色。
- `stageNo` 对应 `currentStageIndex` 或后续更精简的中心编号。
- `time` 对应 `currentStageTimeText`。
- `totalProgress` 对应 `totalProgress` 或 `projectedTotalProgress(elapsedMillis)`。
- `currentProgress` 对应 `currentStageProgress` 或 `projectedStageProgress(elapsedMillis)`。
- `segments` 对应 `stageSegments`，用于绘制当前 work/rest cycle 外圈。
- `paused` 对应 `isPaused`，paused 时不推进投影。
- `final` 对应 `isFinalCountdown`。
- `completed` 对应 `completedWorkoutStageCount`。
- `stageCount` 对应 `totalWorkoutStageCount`。

Compose Canvas 落地时，建议把 HTML 的 `markerPoints(cx, cy, radius, count)` 思路映射为一个纯函数，例如：

```kotlin
private fun markerProgress(index: Int, count: Int): Float =
    index.coerceAtLeast(0).toFloat() / count.coerceAtLeast(1).toFloat()
```

然后让“内圈完成 marker”和“底层浅色小点”都从这个函数得到 angle / point，避免浅点变成固定装饰。`+15` rest extension 可以继续沿用现有 `currentStepDisplayProgress()` / `projectedTotalProgress()` 的单调 floor 思路，生产实现不应让 progress 低于延长前。

## 声音位置说明

本轮不接入音频播放。后续 E13 可以按以下视觉时机绑定声音：

- `final 5 seconds`: `countdown_beep1.mp3` 作为 5 / 4 / 3 / 2 等前几声 beep 候选。
- 阶段切换或最后 1 秒: `.local/audio/stage_bell_copper_clean.wav` 作为铃声候选。

音频接入必须留到 E13，且需要覆盖手机扬声器、蓝牙耳机、不 ducking、不打断其他 App 音频。本原型没有读取、复制或提交这些音频文件。

## 资源与边界声明

- 本原型为原创 HTML/CSS/Canvas 实现。
- 未复制外部 APK、`C:/Users/25073/Desktop/12/WorkoutTimer_Project`、`人工/` 或任何参考项目中的代码、资源、字体、音频、SVG/PNG、vector path、resource name 或 class name。
- 仅吸收抽象设计原则：深色高对比、圆环层级、少文字、连续进度、暂停冻结、final countdown 轻量强调和 rest extension 不倒退。
- 未提交 `.local`、APK、截图、日志或 build 输出。
