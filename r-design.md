# r-design.md - Timer Reference Design Notes

## Purpose

This file records reference-only design findings from:

`C:\Users\25073\Desktop\12\WorkoutTimer_Project`

The goal is to help TrainFlow absorb a cleaner timed-training dial language: dark immersive layout, minimal text, strong icons, circular progress, and lightweight interaction feedback.

This file is not a replacement for `DESIGN.md`. It is a reference bridge for future E10 timer refinements.

## Provenance Boundary

The reference project contains APK-derived resources and reconstructed Android files. Even if parts were manually repaired, TrainFlow should not copy its source code or assets directly.

Do not copy into TrainFlow:

- Java/Kotlin source code from the reference project.
- XML layouts, drawables, vector paths, animation XML, or style files.
- Font files.
- Audio files or sound names.
- SVG/PNG icons, action illustrations, launcher assets, or original assets.
- Package names, class names, resource names, or exact animation implementations.

Allowed to use:

- Abstract product interaction patterns.
- Color role ideas and contrast hierarchy.
- Motion principles such as continuous progress, pause freeze, and short feedback durations.
- Architecture ideas reimplemented with TrainFlow's own Compose Canvas and engine state.
- Layout concepts expressed in TrainFlow's own tokens, components, and naming.

## Reference Palette

The reference direction is a high-contrast dark timer interface with a small number of strong accents.

| Role | Reference value | Notes |
| --- | --- | --- |
| Background | `#000000` | OLED black, immersive workout surface. |
| Row surface | `#111111` | Near-black list/editor rows. |
| Divider | `#333333` | Low-contrast structure line. |
| Primary text | `#FFFFFFFF` | Main time and active labels. |
| Secondary text | `#B3FFFFFF` | About 70% white, supporting labels. |
| Tertiary text | `#99FFFFFF` | About 60% white, low-priority labels. |
| Disabled icon/text | `#40FFFFFF` | About 25% white. |
| Subtle track | `#26FFFFFF` | About 15% white for inactive rails. |
| Ring shadow/backdrop | `#32000000` | Dark circular depth. |
| Overlay | `#8C000000` | Modal dim layer. |
| Warm accent | `#FFC107` | Amber direction from manual palette. |
| Orange accent | `#FF9800` | Strong timer accent from resource values. |
| Alert red | `#FF4444` | Error/final countdown intensity. |
| Light divider | `#DADADC` | Used sparingly; avoid on dark timer surfaces unless needed. |

## TrainFlow Token Mapping

Use these as design directions, not fixed global app colors.

| TrainFlow use | Suggested mapping |
| --- | --- |
| Timer screen background | Deep black or near-black token, not generic app surface. |
| Current work phase | Strong warm accent or existing TrainFlow action color. |
| Rest phase | Cooler and thinner ring, lower visual priority than work. |
| Warmup/cooldown | Distinct but quieter than work; avoid pretending they are actions. |
| Total progress ring | Thin white or high-contrast neutral line. |
| Completed markers | Small solid dots; latest completed marker may show a number. |
| Final 5 seconds | Short pulse/flash using accent or alert color, without aggressive full-screen alarm by default. |
| Disabled/unavailable controls | Low-opacity white; keep tap targets stable. |

## Timer Dial Anatomy

The reference UI works because almost everything is concentrated into one readable timer object.

TrainFlow should preserve this hierarchy:

1. Top area: total remaining time and minimal context.
2. Outer ring: current cycle or stage structure.
3. Inner ring: total workout progress.
4. Center circle: current phase icon, phase number/name, remaining phase time, and pause/resume tap target.
5. Bottom controls: only high-frequency commands, such as skip, extend rest, and end.

Avoid adding explanatory text to the execution screen unless it resolves a real ambiguity. Icons, color, thickness, and progress should carry most of the meaning.

## Motion Principles

The timer should feel continuous, not stepwise.

- Active progress advances smoothly between engine ticks using a bounded frame projection.
- Text countdown can still update once per second.
- Pause freezes every ring and marker.
- Terminal states do not continue projecting progress.
- Rest extension must never make progress visually jump backward.
- Final countdown emphasis should be lightweight: pulse, brief scale, or color intensity shift.
- Micro feedback should usually stay in the `100ms` to `300ms` range.
- Larger state transitions can use about `300ms` to `450ms`, but should not delay training commands.

## Architecture Guidance

The reference project uses a custom circular view pattern. TrainFlow should keep the same architectural idea but not the implementation.

Recommended TrainFlow approach:

- Use Compose Canvas for the dial.
- Keep `TimerDialUiState` as the rendering contract.
- Map only from TrainFlow engine/session state into the dial state.
- Keep progress deterministic, clamped, and testable.
- Do not create fake animation state that can drift away from `WorkoutCommand`, `WorkoutEvent`, or engine status.
- Keep visual variants token-driven and limited to the existing built-in skin system.

## Interaction Patterns We Can Absorb

Good candidates for TrainFlow:

- Center dial tap toggles pause/resume.
- Current work arc is thicker than rest arc.
- Rest arc is thinner and lower intensity.
- Inner progress is a clean total-progress line, without a heavy inactive rail.
- Completed cycle markers become small dots; latest completed marker can show a number.
- Drag handles belong only on explicit handles in editors; page scrolling remains available outside the handle.
- Color editing can be exposed as swatches rather than text-heavy configuration.
- Icon selection can be a compact grid, but TrainFlow should use its own icon system.

## Interaction Patterns To Defer

These are interesting but should remain separate product decisions:

- Reset command in production timer controls.
- Slide-to-confirm end behavior replacing the current end confirmation flow.
- Voice prompts and female cue recordings.
- Full sound asset design and audio coexistence testing.
- Copying the reference icon set or animated SVG style.
- New fourth skin only for this timer direction.

## Timer-Specific Visual Direction

For the timed-training screen, a reference-inspired direction can be explored inside the existing TrainFlow skin system:

### Black/Red High Contrast

- Background: pure black or near-black.
- Work: red/orange thick arc.
- Rest: pale gray or cool muted thin arc.
- Center: strong filled circle for active phase.
- Final 5 seconds: red/orange pulse with restrained scale.

### Cyber Neon

- Background: black with restrained high-contrast neon accents.
- Work: electric cyan or magenta, but not both as dominant colors.
- Rest: thinner cool secondary arc.
- Center: glow is allowed only if it does not reduce legibility or create visual noise.
- Final 5 seconds: short neon pulse, no decorative bokeh/orbs.

### TrainFlow Official Fusion

- Use TrainFlow's current official tokens for primary actions.
- Keep the reference dial geometry and minimal text.
- Use white inner total progress for clarity.
- Keep heart-rate display auxiliary and outside the dial's primary hierarchy.

These are visual directions inside the current skin system, not new skins by themselves.

## What Can Be Put Into TrainFlow

Yes:

- Color roles and contrast hierarchy.
- Compose Canvas dial architecture inspired by the custom view pattern.
- Continuous ring projection and pause-freeze behavior.
- Work/rest thickness difference.
- Minimal text and icon-forward execution layout.
- Swatch-based color customization concepts.
- Abstract animation timing principles.

No:

- Direct Java/XML code.
- APK-derived resources.
- Extracted fonts.
- Extracted audio.
- Extracted SVG, PNG, vector paths, or animated drawable files.
- Exact class/resource names.
- Exact visual tracing or pixel-level reproduction.

Conditional:

- If a future file is fully original and rights-cleared, it can be considered, but TrainFlow should still prefer reimplementation in Compose with local tokens and tests.

## Next Implementation Fit

The next timer work should not restart from scratch. It should refine the existing E10 Timer Dial:

- Keep the E10.8/E10.9 continuous progress direction.
- Preserve pause freeze, terminal freeze, and rest extension monotonic progress.
- Reduce execution-screen text: remove or weaken total-remaining labels, next-stage explanation boxes, and sound-enabled explanation boxes.
- Make total remaining time larger and more centered, while keeping the center countdown as the primary in-workout focus.
- Increase the overall dial size and make outer / inner strokes proportionally thinner so markers do not visually collide with the outer ring.
- Add a wide base ring under the inner total-progress line.
- Any pale dots on the base ring must reuse the same dynamic angle calculation as inner-stage markers; they must react to stage count, stage duration, and rounds instead of acting as fixed decoration.
- Simplify the center circle to the phase icon, necessary number, and time. The center fill should use the phase preset color, with white text and icon.
- Use this palette as a reference variant, not a global app redesign.
- Add visual smoke for active, paused, rest extension, and final 5 seconds.
- Keep production controls scoped to the current command model unless a new command is explicitly designed.
- Keep records and statistics separate from visual polish work.
- Keep sound cue implementation separate in E13. `countdown_beep1.mp3` and `.local/audio/stage_bell_copper_clean.wav` are local user-confirmed candidates, but this reference note does not make `.local` audio submit-ready.

## Huashu Prototype Fit

A future E10.11 story can use the installed `huashu-design` skill for high-fidelity HTML exploration. It should compare three directions:

- Black/Red High Contrast.
- TrainFlow Official Fusion.
- Cyber Neon.

Each direction should cover active, rest, paused, final 5 seconds, and rest extension. These prototypes must not copy external APK or reference-project resources, and they should not move into production without a separate implementation story.
