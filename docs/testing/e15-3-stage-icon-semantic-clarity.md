# E15-3 Stage Icon Semantic Clarity

**日期:** 2026-07-02
**状态:** Implemented, needs review
**分支:** `codex/e15-3-stage-icon-semantic-clarity`

## 背景

本轮响应用户反馈：现有阶段图标偏抽象，需要变成“简单、能表达、但不抽象”。重点覆盖 timed composition editor picker 与执行页 TimerDial 中的内置 `iconKey` 语义，包括热身、动作进行、加速、冲刺、普通休息、轮间恢复和放松 / cooldown。

## 实现

- `TimerDial` 的内置白色 Canvas 图标更新为更直接的语义图形：
  - `warmup`: 火苗升温。
  - `work`: 正在训练 / 动作进行小人。
  - `speed_up`: 速度线 + 方向箭头。
  - `sprint`: 闪电 + 速度线。
  - `rest`: 雪花冷静符号。
  - `recover_breathe`: 循环恢复箭头 + 呼吸波，用于轮间恢复。
  - `cooldown`: 下行降温箭头 + 舒缓波。
  - `strength` / `mobility` / fallback 继续保持白色单色、小尺寸可读。
- timed composition editor picker 使用同一套语义图形，改为 4 列图标按钮，并在按钮内显示中文 label，避免只看抽象图形。
- icon picker 文案补充用户可读说明和 TalkBack content description，例如“雪花冷静符号”“循环恢复和呼吸符号”“下行降温和舒缓符号”。
- 新建默认草稿里的“冲刺组合 / 冲刺”改用已有 `sprint` 内置 key。旧保存计划和旧 session snapshot 不回写、不改语义。

## 测试覆盖

- `StageStyleIconSemanticsTest`
  - 验证 picker 与 TimerDial 都包含热身火苗、动作进行、加速箭头、冲刺闪电、休息雪花、轮间恢复和 cooldown downshift 图形 helper。
  - 验证 picker 使用中文 label，而不是 raw `iconKey`。
  - 验证 icon content description 包含火苗、动作进行、闪电、雪花、循环恢复和下行降温语义。
  - 验证默认新草稿中的“冲刺组合 / 冲刺”使用 `sprint` key。
- `TimedCompositionPlanEditorUiStateTest`
  - 更新 picker selected label 期望：`work` 显示“训练中”，`recover_breathe` 显示“轮间恢复”。
  - 覆盖 warmup/rest/recover/cooldown content description 的语义词。

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

环境：

- AVD: `TrainFlow_Pixel_API_36`
- Display override: `720x1280`
- Density override: `320`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

覆盖：

- `03-icon-picker.png` / `.xml`: editor icon picker 显示热身、训练中、加速、冲刺、休息、轮间恢复、放松、力量、灵活、自定义；UI tree 未出现 `manual_start`、`auto_after_rest`、`speed_up`、`recover_breathe`、`warmup`、`cooldown` raw token。
- `05-timer-warmup.png` / `.xml`: TimerDial warmup 火苗 icon。
- `06-timer-work.png` / `.xml`: TimerDial work/action 动作进行 icon。
- `07-timer-rest.png` / `.xml`: TimerDial 普通 rest 雪花 icon。
- `08-timer-sprint.png` / `.xml`: TimerDial sprint 闪电 icon。
- `09-timer-round-rest.png` / `.xml`: TimerDial 轮间恢复循环恢复 icon。
- `10-timer-cooldown.png` / `.xml`: TimerDial cooldown 下行降温 icon。
- `smoke-checks.txt`: PNG 均为 720x1280 且非黑屏；TimerDial 中心 icon ROI 有白色 icon 像素；raw key scan 为 false；logcat fatal / ANR scan 为 false。
- `logcat.txt`: smoke 期间完整 logcat dump。

## Boundary Check

- Did not add PNG, SVG, drawable, raw, image upload, icon library, or new dependency.
- Did not add, remove, or rename any public `iconKey`; reused existing built-in keys.
- Did not change saved plan or session snapshot semantics; old `iconKey` values remain safe.
- Did not change Room schema / migration.
- Did not change `WorkoutCommand` / `WorkoutEvent`.
- Did not change engine timeline, TimerDial progress, E15-2 clipping / linear progress logic, rest extension, records, completion, sounds, strength logic, heart-rate UI, BLE, Huawei, Health Connect, HealthKit, Wear OS, voice, or TTS.
