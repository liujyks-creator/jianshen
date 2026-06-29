# E14.6-2d Completion Recap Screenshot Evidence Recapture

**Date:** 2026-06-29
**Status:** Completed; screenshot-level visual evidence recaptured and accepted

## Scope

This story was limited to evidence recapture, smoke diagnostics, visual evidence review, and documentation. It did not change Kotlin, Compose, Room, schemas, app tests, `WorkoutCommand`, `WorkoutEvent`, TimerDial progress / mapping / geometry, E12 records / trends, E14.6-3 stage style / icon planning, or heart-rate UI / input / statistics.

Evidence was written only under:

```text
.local/smoke/e14-6-2d-completion-recap-screenshot-recapture/
```

The `.local/` evidence remains local and is not staged or committed.

## Inputs Read

- `AGENTS.md`
- `DEV_STORY_PROMPT_TEMPLATE.md`
- `CODE_REVIEW_PROMPT_TEMPLATE.md`
- `docs/project-status.md`
- `docs/roadmap-backlog.md`
- `docs/testing/e14-6-2-completion-recap-page-planning.md`
- `docs/testing/e14-6-2b-completion-recap-page-compose.md`
- `docs/testing/e14-6-2c-completion-recap-smoke-visual-qa.md`
- `docs/setup.md`
- `docs/new-computer-setup.md`
- Android emulator QA skill
- `huashu-design` skill and visual critique guide
- `skills/bmad-method/SKILL.md`

## Startup Checks

- `git status --short` showed only pre-existing local untracked artifacts: root APK, `countdown_beep1.mp3`, `deliverables/`, and `人工/`.
- `git rev-list --left-right --count main...origin/main` returned `0 0`.
- `git diff --name-only -- app/src/main app/src/test` was empty.
- `git diff --cached --name-only` was empty.
- `. .\.local\env.ps1` loaded the local JDK and Android SDK paths.
- `.local/android-sdk/platform-tools/adb.exe` existed.
- `.local/android-sdk/emulator/emulator.exe` existed.
- `emulator -list-avds` included `TrainFlow_Pixel_API_36`.

## E14.6-2b PNG Diagnosis

All 16 E14.6-2b `.png` files were non-zero size, so the failure was not a zero-byte capture or missing pull.

The first sampled file, `abandoned-confirm-prompt.png`, was 164,508 bytes and started with:

```text
FF FE FD FF 50 00 4E 00 47 00 0D 00 0A 00 1A 00
```

A valid PNG should start with:

```text
89 50 4E 47 0D 0A 1A 0A
```

The observed bytes match a text / UTF-16-style corruption path: a BOM-like prefix, a replacement-style byte sequence for the binary `89`, and null-padded ASCII PNG signature bytes. The E14.6-2b XML evidence remains usable for semantic / interaction coverage, but the E14.6-2b PNG files are not valid visual evidence.

## E14.6-2c Black Screenshot Diagnosis

The E14.6-2c `current-*` TrainFlow screenshots were valid PNG files but sampled as fully black. The same attempt captured a non-black Android Home screenshot, and the UI tree still showed TrainFlow home content plus `编辑计时计划`. Focused logcat did not show a TrainFlow fatal exception, process crash, ANR, or fatal signal.

The current E14.6-2d rerun did not reproduce the black TrainFlow capture. Fresh AVD startup plus binary-safe screenshot pull captured TrainFlow launch, editor, execution, completed recap, and abandoned recap screens normally. Current judgment: the E14.6-2c black screenshots were a transient emulator / Surface / capture-timing or stale-runnable-state problem, not evidence of a production completion recap rendering or navigation blocker.

E14.6-2c also had some older valid non-black recap screenshots, but those showed the previous TimerDial terminal presentation rather than the E14.6-2b dedicated recap page, so they were not accepted as E14.6-2b completion recap evidence.

## Recapture Method

- Ran `app:assembleDebug` only as a smoke APK freshness check; the build succeeded and tasks were up to date.
- Started AVD `TrainFlow_Pixel_API_36`.
- Installed `app/build/outputs/apk/debug/app-debug.apk`.
- Cleared app data and launched `com.liujyks.trainflow/.app.MainActivity`.
- Navigated through the current timed training path using existing UI only.
- Completed a short timed session by advancing through steps with existing controls.
- Captured PNGs with a binary-safe path: `adb shell screencap -p /sdcard/<name>.png` followed by `adb pull`.
- Captured matching UI tree XML, focused logcat tail, activity dumps, and AVD shutdown evidence.
- Closed the AVD and confirmed `adb devices` was empty.

## Evidence Captured

Primary completed recap evidence:

- `completed-recap-top.png` and `completed-recap-top.xml`
- `completed-recap-summary.png` and `completed-recap-summary.xml`
- `completed-recap-details.png` and `completed-recap-details.xml`
- `return-home-after-completed.png` and `return-home-after-completed.xml`

Supplemental terminal evidence:

- `abandoned-confirm-prompt-2.png` and `abandoned-confirm-prompt-2.xml`
- `abandoned-recap.png` and `abandoned-recap.xml`

Diagnostics and support files:

- `png-validation.txt`
- `logcat-tail.txt`
- `completion-detection.txt`
- `rest-detection.txt`
- `adb-devices-after-close.txt`
- `system-home-before-install.png` and `trainflow-launch.png`

Rest-state screenshots were also captured for diagnostics (`rest-before-extension.png`, `rest-confirm-extension.png`, and `rest-after-extension.png`). They prove the rest state and `+15s` affordance were reachable during the recapture run, but this story does not claim a new rest-extension recap record from those screenshots because the completed recap in this run still reported `休息延长 0 秒`. The earlier E14.6-2b XML remains the semantic evidence for rest-extension recap content.

## PNG Validation

`png-validation.txt` reported every new PNG as `OK`, with valid PNG signature:

```text
89 50 4E 47 0D 0A 1A 0A
```

All validated screenshots were `720x1280`. The completed recap captures sampled as non-black at `25/25` sample points:

- `completed-recap-top.png`
- `completed-recap-summary.png`
- `completed-recap-details.png`

`abandoned-recap.png` also sampled non-black at `25/25`. The confirm-prompt captures sampled non-black at `7/25`, which is expected for a dark overlay and still confirms they are not blank black frames.

## Visual QA Result

Screenshot-level visual QA is now accepted for the E14.6-2 completion recap page.

- `completed-recap-top.png` shows the dedicated recap page, compact check badge, `已完成`, `本次复盘`, `本次训练已完成`, key summary cards, and bottom `返回训练首页`.
- `completed-recap-summary.png` and `completed-recap-details.png` show scrolled summary / recap sections without visual overlap and keep the bottom return action reachable.
- `abandoned-recap.png` shows the early-ended shell with `已结束` / `本次训练已提前结束`, reused recap content, and no completed celebration.
- The completed recap no longer uses the large TimerDial as the terminal primary visual.

## Logcat And Device Result

Focused logcat tail did not show a TrainFlow fatal exception, process crash, ANR, or fatal signal during the recapture.

`adb-devices-after-close.txt` contained only:

```text
List of devices attached
```

## Self Review

- PASS: The work stayed within evidence recapture, smoke diagnostics, visual review, and Markdown documentation.
- PASS: No app source or app test files were changed.
- PASS: The new completed recap PNGs are valid PNG files, openable, and non-black.
- PASS: The E14.6-2b PNG invalid-byte diagnosis is recorded.
- PASS: The E14.6-2c black screenshot judgment is recorded.
- PASS: `.local/smoke` remains unstaged and uncommitted.
- PASS: No E14.6-3 stage style / icon planning was started.
- PASS: No E12 records / trends polish was started.
- PASS: No heart-rate UI / input / statistics work was restored.
- PASS: No `.local/verification` output was written.

## Next

E14.6-2 screenshot-level visual QA is closed. The next story can move to E14.6-3 stage style / icon planning.
