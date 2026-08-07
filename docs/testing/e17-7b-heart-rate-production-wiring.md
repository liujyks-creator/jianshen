# E17-7b Heart-rate Production Wiring

## Merge-stable status

This document records the E17-7b candidate contract and evidence boundaries. The final candidate is the commit containing this document, and its executable tree must equal executable source `2b243e60640e51878442836c5e82b940738ff84c`. The candidate remains `implemented / needs review` until a fresh independent Review passes, a `--no-ff` merge is pushed, the final candidate full SHA is an ancestor of synchronized `main` / `origin/main`, `main...origin/main = 0 0`, and the current status documents agree. When all conditions hold, E17-7b automatically becomes `reviewed / merged` and E17-8 prerequisite automatically becomes `satisfied`; there is no separate docs-sync / recursive closeout and no future merge SHA is predeclared.

## Accepted repair

The 2026-07-28 post-device decision requires:

- one top switch labeled `启用心率功能`;
- four compact sections: `心率功能`, `设备连接`, `心率区间与提醒`, `隐私与使用边界`;
- one primary connection action and at most one secondary connection action for each supported state;
- manual-disconnected + saved target shows primary `重新连接`, secondary `更换设备`, and never `连接已保存设备`;
- Bluetooth-off opens Android Bluetooth settings;
- adapter-off cleanup tolerates only the causal `stopScan` `IllegalStateException`; unrelated `IllegalStateException` remains observable;
- the user-selected option 3 `Soft Zone Halo` replaces the obsolete solid rail: zone capsules use the accepted palette as a soft 12–18% component-local halo / gradient plus subtly tinted surface, explicit zone text, and adaptive high-contrast foreground;
- the three personal parameters share one 10dp outer frame and use 64dp-minimum rows with 12dp/6dp padding, exact 20dp official Material person / monitor-heart / notifications-none icons, a 16dp icon-to-label-column gap, 12sp/16sp subtitles, 60dp × 40dp value boxes with 10dp corners and 8dp internal padding, 8dp to an official 20dp auto-mirrored chevron-right, and explicit 1dp dividers; the four vectors are narrowly vendored from `google/material-design-icons` under Apache-2.0 with official viewport/path data preserved and no broad icon dependency;
- D-079 / D-082 are superseded only for collapsed content geometry: the capsule uses an intrinsic centered Row inside `widthIn(min = 116.dp, max = 180.dp)`, keeps 13dp/9dp padding, restores the existing status/zone tone as an 8dp dot with an exact 8dp label gap for every neutral and zone state, and keeps one safely ellipsized line without collapsed `fillMaxWidth`;
- the 2026-08-07 later user acceptance narrowly supersedes the prior zero-change wording: size, viewport, label, expansion, or exclusion-policy repositioning preserves the stored `HeartRateCapsuleSnapEdge`, while pointer drag release still performs nearest-edge inference and updates the stored edge; regular 276dp × max 214dp and compact 252dp × max 190dp envelopes, expanded leading dot/label and padding, four-tile information layout, capsule dimensions, vertical clamp, safe-area / exclusion policy, movement threshold, drag gesture, expand/collapse, motion, visual/content/state, IME, and all other geometry remain frozen and unchanged;
- no E17-9 active-training FGS or background-ownership work.

## Automated evidence

Automated evidence is valid only for the exact final candidate and executable-equivalent APK recorded in the ignored evidence manifest. Required gates are:

- focused RuntimeOwner platform, settings action/state, application mapper/lifecycle, and capsule tests;
- all `*HeartRate*` unit tests, full debug unit suite, `assembleDebug --rerun-tasks`, `lintDebug` with Kotlin incremental disabled, and `app:check`;
- exact three-dot scope, protected-path, index, frozen-boundary delta, executable-tree-equivalence, and APK identity checks;
- fresh API 36 AVD install, settings screenshots / UI trees, Bluetooth-settings handoff, and fatal / crash / ANR scan.

## Evidence separation

- The protected `.local/audit/e17-7b-post-human-ux/` capture and APK SHA256 `98C19501AEAB34E01C47F44FEAA091FB7197A47946CD7F77BE9DBF3ECB5D33F5` remain stale pre-repair finding evidence and do not accept the final candidate.
- User-passed physical evidence is bound to old phone-tested executable source `12e33626b2af78708c14a7083d7a825db8e9cecf`, APK SHA256 `C0B6F495A5C9C1E417468A9571EA60BA1236362B65D7899C7DBB95B0216785D3`. Exact relevant-source equivalence from that source to `2b243e60640e51878442836c5e82b940738ff84c` preserves the accepted BLE / runtime / recovery / settings / DataStore / parameter / zone observations; the only intervening executable changes are the capsule snap-edge Repair, which does not change those criteria.
- Fresh API 36 AVD acceptance is bound to executable source `2b243e60640e51878442836c5e82b940738ff84c`, 14,783,384-byte APK SHA256 `FFF73C0F79018F9871F75C512BD84817B82D94D010262EA2A42A1ACC4C46D955`. It proves RIGHT resize anchoring, LEFT resize anchoring, pointer drag-back nearest-edge behavior, and no observed crash / ANR for that sequence.
- The AVD layer is emulator geometry / interaction evidence only. It creates no new phone, HUAWEI Band 9, RF, GATT, CCCD, notify, reconnect, or physical BLE claim and does not substitute for the preserved source-bound physical observations above.

## Bluetooth-off timeout Review Repair

The complete Review found one production crash race: an active finite scan could reach its timeout after the Bluetooth adapter had already turned off. Android may then throw `IllegalStateException` from `BluetoothLeScanner.stopScan()`. The explicit `BluetoothOff` cleanup path was already narrow, but the timeout path did not classify the adapter state before propagating the exception.

The Repair keeps the existing generation invalidation and cleanup order. `detachAndStopActiveScan()` now tolerates that `IllegalStateException` only when the current adapter is provably disabled or unavailable, publishes the typed Bluetooth-off fact, and rejects the detached scan's late callback. When the adapter remains enabled, the same exception is still observable. The implementation does not inspect exception messages, catch general runtime exceptions, add a BLE wrapper/seam, or add reconnect / E17-9 behavior.

The regression proof first failed on the pre-Repair candidate for `active scan -> adapter off -> timeout -> stopScan IllegalStateException`; after the Repair both that path and the adapter-on negative control pass. Old phone-tested source `12e33626b2af78708c14a7083d7a825db8e9cecf` already contains this timeout Repair and the accepted visual-fidelity implementation. The relevant BLE / runtime / recovery / settings / DataStore / parameter / zone source remains equivalent in executable source `2b243e60640e51878442836c5e82b940738ff84c`; only the separately bounded capsule snap-edge Repair requires the fresh AVD acceptance recorded above.

## Capsule snap-edge resize Repair

The 2026-08-07 acceptance observation found that a capsule snapped to the right could move to the left when its collapsed / expanded size changed. The resize effect reused the drag-release snap function, which inferred LEFT or RIGHT from the current top-left coordinate and the newly measured width. During a resize, that coordinate belongs to the previous size, so it is not a valid nearest-edge release point; in particular, an expanded right-edge x-coordinate can be reclassified as LEFT after collapse.

The Repair keeps the stored `HeartRateCapsuleSnapEdge` as the source of truth for size, viewport, label, expansion, and exclusion-policy repositioning. A narrow geometry helper places the new size at that explicit edge while reusing the existing safe-inset, vertical clamp, and exclusion-zone logic. Pointer drag release still calls the original nearest-edge inference and updates the stored edge.

The focused regression first failed against the pre-Repair behavior for expanded-right top-left `x=70` resized to a 116 px collapsed capsule in a 360 px viewport: nearest-edge inference returned LEFT. The final geometry coverage verifies RIGHT across collapsed / expanded sizes, LEFT across both sizes, and the existing left / right drag-release nearest-edge cases. No capsule dimensions, content, colors, motion, movement threshold, compact fallback, safe-area policy, or BLE / runtime / settings behavior changed.
