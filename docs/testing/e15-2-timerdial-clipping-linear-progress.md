# E15-2 TimerDial Clipping And Short Target Linear Progress

**日期:** 2026-07-02
**状态:** Implemented, needs review
**分支:** `codex/e15-2-timerdial-clipping-linear-progress`

## 背景

本轮响应真机反馈中的两个 TimerDial 问题：

1. TimerDial 外圈上侧 / 下侧偶发看起来被黑色遮挡或裁切。
2. composition v2 当前 stage group 内 target 只有 1s / 2s 时，外圈 active target progress 出现固定追赶式加速填充感。

设计要求保持不变：TimerDial progress 必须来自 engine / UI state；v2 外圈仍按当前 stage group 内 targets 的 planned duration ratio 分段；内圈总进度、12 点数字圆标、`+15s` rest extension、command / event / Room / records 语义均不改变。

## 实现

- `TimerDialLayoutSpec` 新增外圈绘制安全尺寸：`outerSafeStrokeDp` / `outerSafeInsetDp` / `outerSafeDiameterDp`。原 `outerMaxStrokeDp`、`outerDiameterDp` 和 planned-duration ratio 语义保留。
- `TimerDial` Canvas 外圈 arc rect 改用 safe stroke inset。active segment 本体 stroke 仍按原 segment stroke 绘制，只是把 glow 的额外 8dp stroke 预留进安全绘制区域，避免 glow 在 Canvas 边缘被裁。
- `TimerDialUiState.monotonicDisplayedProgress` 在当前 active segment planned duration 为 1s / 2s 时，不再对向前的 engine anchor 使用固定 1s catch-up。短 target 会先对齐真实 engine anchor，再继续按 frame elapsed 和 remaining duration 线性投影。
- 正常时长 target 仍保留 E14.6-1 的防回弹 catch-up 逻辑，避免恢复旧的 tick anchor 回弹和 first tick lunge。

## 测试覆盖

- `TimerDialUiStateTest`
  - 外圈 safe stroke 覆盖 current segment max stroke + glow extra stroke。
  - 1s target 的 0 / 250 / 500 / 750 / 1000ms progress 为 0 / 0.25 / 0.5 / 0.75 / 1。
  - 2s target 的首秒 progress 以 0.125/250ms 线性推进。
  - 2s target tick anchor 后从 0.5 开始继续 0.125/250ms 线性推进，不再固定 1s 追赶。
- `TimerDialCompositionMappingTest`
  - v2 stage group `[2s work, 1s rest]` 保持 planned segment durations `[2, 1]`。
  - 2s work target tick anchor 后 active progress 0ms = 0.5、500ms = 0.75。
  - 同一 v2 stage group total progress 0ms = 1/3、500ms = 0.5。
  - 1s rest target 500ms project 到 0.5。

## 验证

- `.\gradlew.bat app:testDebugUnitTest --tests "*TimerDial*"` passed.
- `.\gradlew.bat app:testDebugUnitTest --tests "*TimedComposition*"` passed.
- `.\gradlew.bat app:testDebugUnitTest` passed.
- `.\gradlew.bat app:assembleDebug` passed.
- `.\gradlew.bat app:lintDebug` passed.
- `.\gradlew.bat app:check` passed.

后续还需在提交前完成 `git diff --check` 和 `git diff --cached --check`。

## Android Smoke

Smoke 证据目录：

`C:/Users/25073/Desktop/jianshen/.local/smoke/e15-2-timerdial-clipping-linear-progress/`

环境：

- AVD: `TrainFlow_Pixel_API_36`
- Display override: `720x1280`
- Density override: `320`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

覆盖：

- `03-active-normal-target.png` / `.xml`: 720x1280 active work target TimerDial。
- `04-paused.png` / `.xml`: paused morph state。
- `06-rest.png` / `.xml`: rest TimerDial with `+15s` enabled。
- `07-rest-plus15-confirm.png` / `.xml`: `确认+15s` state。
- `timerdial-pixel-check.txt`: active/rest/+15 visible dial pixels remain inside TimerDial bounds with top/bottom margins; paused morph visually checked because its surface intentionally fills its semantic bounds.
- `short-target-frame-evidence.txt`: 1s / 2s frame progress samples from focused tests.
- `logcat-fatal-anr-scan.txt`: no fatal exception, ANR, fatal signal, or TrainFlow process death matched.
- `adb-devices-before.txt`, `emulator-list-avds.txt`, `adb-devices-after-boot.txt`, `adb-devices-after-smoke.txt`, `adb-install.txt`, `wm-size-before.txt`, `wm-density-before.txt`.

The seeded smoke plan does not expose an ergonomic 1s / 2s production target path, so short-target smoothness is proven by focused tests plus frame evidence. A real-device subjective retest is still recommended for visual feel.

## Boundary Check

- Did not redo TimerDial visual design.
- Did not change v2 outer ring planned-duration ratio semantics.
- Did not change inner total progress semantics.
- Did not change 12 o'clock number marker semantics.
- Did not change `+15s` rest extension semantics.
- Did not change timeline, `WorkoutCommand`, `WorkoutEvent`, Room schema, records/history, completion page, sounds, strength editor logic, icons, heart-rate UI, BLE, Health Connect, HealthKit, Wear OS, voice, or TTS.
- Did not add resources, images, SVG, drawables, audio, or external dependencies.
