# E15-3 Stage Icon Semantic Clarity

**日期:** 2026-07-03
**状态:** Implemented, needs review
**分支:** `codex/e15-3-stage-icon-semantic-clarity`

## 背景

本轮最初响应用户反馈：阶段图标偏抽象，需要变成“简单、能表达、但不抽象”。第一版先把 timed composition editor picker 与执行页 TimerDial 的 Canvas 图标改成更直观的火苗、动作小人、雪花、轮间恢复、下行 cooldown 和闪电语义。

用户随后反馈“现阶段的图标库还是太简单”，并明确要求调用 image generation 重新设计，再处理成图标库所需图片文件替换现有图标库。因此本 follow-up 把阶段图标从 Compose Canvas helper 切换为项目内置、随 APK 打包的白色 PNG 资源，同时继续保持 `iconKey` 数据契约不变。

## Image Generation

使用 `imagegen` skill 的内置 image generation 工作流生成 5x2 图标 atlas；本机未设置 `OPENAI_API_KEY`，因此没有使用需要 key 的 CLI fallback。生成源图和处理中间文件保存在本地禁区目录，不提交：

- Codex 生成源：`C:/Users/25073/.codex/generated_images/019f238a-62f2-7b92-9839-bf69ba4c829c/ig_0339c73944f077e9016a4693f60f108197947adde3018989f5.png`
- 工作区中间源：`C:/Users/25073/Desktop/jianshen/.local/generated-stage-icons/stage-icon-atlas-source.png`
- 处理报告：`C:/Users/25073/Desktop/jianshen/.local/generated-stage-icons/stage-icon-processing-report.txt`
- 预览图：`C:/Users/25073/Desktop/jianshen/.local/generated-stage-icons/stage-icon-sheet-preview.png`

Prompt：

```text
Use case: logo-brand
Asset type: mobile app built-in stage icon atlas for a fitness interval timer
Primary request: Create a clean production-ready icon atlas containing exactly ten separate fitness stage icons, no text labels, no numbers, no letters.
Icons required in reading order, arranged in a perfectly aligned 5 columns x 2 rows grid with generous equal padding in every cell:
1 warmup: simple flame / heating up symbol
2 work: active training / exercising person in motion
3 speed up: forward arrow with restrained speed lines
4 sprint: lightning bolt with short speed lines
5 rest: snowflake / cool down rest symbol
6 round recovery: circular recovery arrow with subtle breathing wave
7 cooldown: downward temperature / downshift arrow with calm wave
8 strength: dumbbell
9 mobility: joint mobility / connected joints with curved arc
10 custom: neutral target dot in circle
Style/medium: modern vector-like raster icons, crisp white single-color glyphs, friendly but not childish, suitable for Android app UI at 24dp and 48dp.
Composition/framing: exact 5x2 atlas, each icon centered in its own invisible square cell, same visual weight, consistent 2.5px rounded stroke feel, simple silhouette, no decorative background inside cells.
Scene/backdrop: perfectly flat solid #00ff00 chroma-key background across the entire image, no grid lines, no cell borders, no shadows, no gradients, no texture.
Color palette: icons pure white #ffffff only; background pure #00ff00 only.
Constraints: no text, no captions, no labels, no watermark, no mockup, no UI frame, no drop shadow, no glow, no perspective, no 3D, no extra icons. Keep all icons separated from the background with crisp antialiased edges and enough padding for cropping. Avoid thin fragile details; all icons must remain recognizable when scaled to 24dp.
```

本地处理步骤：

- 将 5x2 atlas 裁切为 10 个单独图标。
- 使用 chroma-key 移除绿色背景，保留 alpha。
- 清理绿色边缘并统一白色单色输出。
- 输出 10 个 `drawable-nodpi` PNG，供 Compose `painterResource` 加载和 tint。

## 实现

- 新增 `StageIconImage`，集中把既有内置 key 映射到 `R.drawable.stage_icon_*`：
  - `warmup`
  - `work`
  - `speed_up`
  - `sprint`
  - `rest`
  - `recover_breathe`
  - `cooldown`
  - `strength`
  - `mobility`
  - `custom`
- `TimerDialStageGlyph` 改为消费 `StageIconImage`，执行页中心圆不再使用阶段 Canvas helper。
- timed composition editor picker 的 `StageStyleIconGlyph` 改为消费同一套 `StageIconImage`，确保 picker 与执行页视觉一致。
- 旧阶段 Canvas helper 已删除；暂停态播放三角仍保留为局部 Canvas，因为它不是阶段 `iconKey` 库的一部分。
- picker 继续 4 列展示图标 + 中文 label，保留 TalkBack content description，避免暴露 `speed_up` / `recover_breathe` 等 raw key。
- 新建默认草稿里的“冲刺组合 / 冲刺”继续使用既有 `sprint` key。旧保存计划和旧 session snapshot 不回写、不改语义。

## 新增资源

新增的资源都是项目内置阶段图标，不是用户图片、远程资源或运行时可替换资源：

- `app/src/main/res/drawable-nodpi/stage_icon_warmup.png`
- `app/src/main/res/drawable-nodpi/stage_icon_work.png`
- `app/src/main/res/drawable-nodpi/stage_icon_speed_up.png`
- `app/src/main/res/drawable-nodpi/stage_icon_sprint.png`
- `app/src/main/res/drawable-nodpi/stage_icon_rest.png`
- `app/src/main/res/drawable-nodpi/stage_icon_recover_breathe.png`
- `app/src/main/res/drawable-nodpi/stage_icon_cooldown.png`
- `app/src/main/res/drawable-nodpi/stage_icon_strength.png`
- `app/src/main/res/drawable-nodpi/stage_icon_mobility.png`
- `app/src/main/res/drawable-nodpi/stage_icon_custom.png`

## 测试覆盖

- `StageStyleIconSemanticsTest`
  - 验证 10 个 `stage_icon_*.png` 资源存在且非空。
  - 验证 `StageIconImage` 映射每个内置 key 到对应 drawable。
  - 验证 picker 与 TimerDial 都消费 `StageIconImage`。
  - 验证旧阶段 Canvas helper 不再保留在 picker 或 TimerDial。
  - 验证 picker 使用中文 label，而不是 raw `iconKey`。
  - 验证 icon content description 包含火苗、动作进行、闪电、雪花、循环恢复和下行降温语义。
  - 验证默认新草稿中的“冲刺组合 / 冲刺”使用 `sprint` key。
- `TimedCompositionPlanEditorUiStateTest`
  - 继续覆盖 picker selected label：`work` 显示“训练中”，`recover_breathe` 显示“轮间恢复”。
  - 继续覆盖 warmup/rest/recover/cooldown content description 的语义词。

## 验证

- `.\gradlew.bat app:testDebugUnitTest --tests "*StageStyle*"` passed.
- `.\gradlew.bat app:testDebugUnitTest --tests "*TimerDial*"` passed.
- `.\gradlew.bat app:testDebugUnitTest --tests "*TimedComposition*"` passed.
- `.\gradlew.bat app:testDebugUnitTest` passed.
- `.\gradlew.bat app:assembleDebug` passed.
- `.\gradlew.bat app:lintDebug` passed.
- `.\gradlew.bat app:check` passed.

## Android Smoke

Smoke 证据目录：

`C:/Users/25073/Desktop/jianshen/.local/smoke/e15-3-stage-icon-semantic-clarity/`

设备与构建：

- AVD: `TrainFlow_Pixel_API_36`
- Serial: `emulator-5554`
- Viewport: `720x1280`, density `320`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

截图使用设备侧 `screencap -p /sdcard/...` 后 `adb pull`，避免 PowerShell 二进制重定向损坏 PNG。

已覆盖：

- `03-icon-picker.png` / `03-icon-picker.xml`: editor icon picker 显示 10 个内置图标，中文 label 可见，UI tree 包含火苗、动作进行、闪电、雪花、循环恢复、下行降温等语义描述。
- `05-timer-warmup.png` / `05-timer-warmup.xml`: TimerDial warmup 中心 icon 可见。
- `06-timer-work.png` / `06-timer-work.xml`: TimerDial work/action 中心 icon 可见。
- `07-timer-rest.png` / `07-timer-rest.xml`: TimerDial rest 雪花中心 icon 可见。
- `08-timer-sprint.png` / `08-timer-sprint.xml`: TimerDial sprint 中心 icon 可见。
- `09-timer-round-rest.png` / `09-timer-round-rest.xml`: TimerDial restBetweenRounds / round recovery 中心 icon 可见。
- `10-timer-cooldown.png` / `10-timer-cooldown.xml`: TimerDial cooldown/recover 中心 icon 可见。
- `smoke-checks.txt`: 像素检查确认截图非黑屏、中心 icon 白色像素存在；raw key 扫描为空；logcat fatal / ANR 扫描为空。
- `logcat.txt`, `adb-devices-after.txt`, `avd-list.txt`, `wm-size.txt`, `wm-density.txt`: 设备、日志与 viewport 证据。

## Boundary Check

- Added only project-owned built-in PNG stage icon resources under `app/src/main/res/drawable-nodpi/`.
- Did not add SVG, vector drawable, raw audio, image upload, custom image picker, remote icon pack, runtime plugin market, or new dependency.
- Did not add, remove, or rename any public `iconKey`; reused existing built-in keys.
- Did not save image paths, drawable names, resource paths, URLs, local file paths, base64, or uploaded asset references into `WorkoutPlan` or `WorkoutSession`.
- Did not change saved plan or session snapshot semantics; old `iconKey` values remain safe.
- Did not change Room schema / migration.
- Did not change `WorkoutCommand` / `WorkoutEvent`.
- Did not change engine timeline, TimerDial progress, E15-2 clipping / linear progress logic, rest extension, records, completion, sounds, strength logic, heart-rate UI, BLE, Huawei, Health Connect, HealthKit, Wear OS, voice, or TTS.
