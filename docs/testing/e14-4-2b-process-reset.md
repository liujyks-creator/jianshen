# E14.4-2b Process Reset

**Status:** Process correction recorded; no Kotlin / Compose / Room / training semantics change in this note

**Date:** 2026-06-26

## Trigger

After `E14.4-2b complete visual design`, several follow-up prompts drifted away from the project prompt templates and workflow gates. The visible symptoms were:

- visual work did not consistently treat the existing TrainFlow UI as the immutable baseline;
- follow-up stories did not consistently name and load the relevant skills before planning, visual review, or implementation;
- implementation work moved forward without the full `DEV_STORY_PROMPT_TEMPLATE.md` story protocol;
- review / handoff did not consistently use `CODE_REVIEW_PROMPT_TEMPLATE.md`;
- Android virtual smoke was treated as optional memory instead of a default environment step.

## Starting Point Of Drift

The first clear visual drift started in `E14.4-2b-1`, when the TimerDial / complete visual design prompt allowed a broad semantic prototype instead of saying: "use the current production TimerDial and editor UI as non-negotiable baselines, and only add the outer-ring target-ratio meaning."

The process drift became operational in `E14.4-2b-3` and `E14.4-2b-4`: these became implementation slices, but were not run through the full development story template, branch / commit / push / review gate loop, and required Android UI smoke discipline.

## Template Corrections

`DEV_STORY_PROMPT_TEMPLATE.md` and `CODE_REVIEW_PROMPT_TEMPLATE.md` now explicitly include the local Android virtual test environment:

- Android SDK: `C:/Users/25073/Desktop/jianshen/.local/android-sdk`
- adb: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/platform-tools/adb.exe`
- emulator: `C:/Users/25073/Desktop/jianshen/.local/android-sdk/emulator/emulator.exe`
- AVD home: `C:/Users/25073/Desktop/jianshen/.local/android-avd`
- Android user home: `C:/Users/25073/Desktop/jianshen/.local/android-user`
- default AVD: `TrainFlow_Pixel_API_36`

For Android UI, APK, screenshot feedback, interaction smoke, or handoff work, future prompts must require:

```powershell
.\.local\android-sdk\platform-tools\adb.exe devices
.\.local\android-sdk\emulator\emulator.exe -list-avds
```

If no online device exists but `TrainFlow_Pixel_API_36` exists, the agent should try to start that AVD before reporting that smoke cannot run. Screenshots, UI tree dumps, and logcat output belong under `.local/smoke/<Story ID>/`; `.local/verification` remains off-limits.

## Required Skill Gates

Future E14.4 / E14.4-2b prompts must include these gates explicitly:

- product / architecture / backlog / story / review work: read `skills/bmad-method/SKILL.md` if present;
- UI, design-system, visual prototype, visual review, high-fidelity mock, or design variant work: read `huashu-design` if available and read `DESIGN.md`;
- Android UI / APK / smoke / screenshot validation: read Android emulator QA skill if available and use the local `.local/android-sdk` paths above;
- design token / component generation work: consume `DESIGN.md` before emitting UI and avoid guessing colors, spacing, typography, or component behavior.

## Current Handling Rule

Do not continue directly into `E14.4-2b-5` or `E14.4-2b-6` from the current local worktree. First run a template-based review / reset pass over the existing `E14.4-2b-3` and `E14.4-2b-4` local changes:

1. identify which changes are local-only, committed, or already accepted;
2. confirm whether the visual gate was actually satisfied for each UI change;
3. run the full Gradle verification expected by the dev story template;
4. run or explicitly fail the Android virtual smoke check using `TrainFlow_Pixel_API_36`;
5. only then decide whether to commit, split, revert, or revise.

## Non-Goals

This process reset does not:

- change the timed composition data decision by itself;
- revert or rewrite local implementation work;
- authorize engine timeline mapping;
- authorize TimerDial production mapping;
- authorize Room schema migration;
- restore heart-rate UI, manual heart-rate input, unavailable heart-rate placeholders, or average heart-rate trends.
