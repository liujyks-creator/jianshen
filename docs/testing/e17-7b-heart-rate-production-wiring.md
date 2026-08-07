# E17-7b Heart-rate Production Wiring

## Merge-stable status

This document records the E17-7b candidate contract and evidence boundaries. The candidate remains `implemented / needs review`; it is not a merge or phone / HUAWEI Band 9 pass. The final candidate is the commit containing this document. Git ancestry and synchronized `main` remain the merge truth.

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
- expanded leading dot/label, regular 276dp × max 214dp and compact 252dp × max 190dp envelopes, expanded padding, four-tile information layout, geometry algorithm, drag/snap, expand/collapse, motion, viewport, safe-area, and IME behavior remain frozen and unchanged;
- no E17-9 active-training FGS or background-ownership work.

## Automated evidence

Automated evidence is valid only for the exact final candidate and executable-equivalent APK recorded in the ignored evidence manifest. Required gates are:

- focused RuntimeOwner platform, settings action/state, application mapper/lifecycle, and capsule tests;
- all `*HeartRate*` unit tests, full debug unit suite, `assembleDebug --rerun-tasks`, `lintDebug` with Kotlin incremental disabled, and `app:check`;
- exact three-dot scope, protected-path, index, geometry-zero-delta, and APK identity checks;
- fresh API 36 AVD install, settings screenshots / UI trees, Bluetooth-settings handoff, and fatal / crash / ANR scan.

## Evidence separation

- The protected `.local/audit/e17-7b-post-human-ux/` capture and APK SHA256 `98C19501AEAB34E01C47F44FEAA091FB7197A47946CD7F77BE9DBF3ECB5D33F5` are stale pre-repair evidence. They explain the repair but cannot accept it.
- Fresh automated and AVD evidence for the selected visual fidelity repair belongs under ignored `.local/smoke/e17-7b-selected-visual-fidelity-repair/<final-candidate-full-sha>/`; it is not physical BLE / GATT evidence.
- Final phone + HUAWEI Band 9 evidence is pending and must cover compact actions/settings, manual disconnect/reconnect/change-device, foreground/app-return/proximity recovery, live zone readability, opt-out, and Bluetooth off/on without crash.

## Bluetooth-off timeout Review Repair

The complete Review found one production crash race: an active finite scan could reach its timeout after the Bluetooth adapter had already turned off. Android may then throw `IllegalStateException` from `BluetoothLeScanner.stopScan()`. The explicit `BluetoothOff` cleanup path was already narrow, but the timeout path did not classify the adapter state before propagating the exception.

The Repair keeps the existing generation invalidation and cleanup order. `detachAndStopActiveScan()` now tolerates that `IllegalStateException` only when the current adapter is provably disabled or unavailable, publishes the typed Bluetooth-off fact, and rejects the detached scan's late callback. When the adapter remains enabled, the same exception is still observable. The implementation does not inspect exception messages, catch general runtime exceptions, add a BLE wrapper/seam, or add reconnect / E17-9 behavior.

The regression proof first failed on the pre-Repair candidate for `active scan -> adapter off -> timeout -> stopScan IllegalStateException`; after the Repair both that path and the adapter-on negative control pass. The final delivery APK must be built once from the committed immutable Repair tip and copied to a new ignored delivery directory. All earlier candidate APK identities are superseded and cannot be used for the phone / Band 9 gate.

## Shortest final human gate

1. Install the final debug APK recorded in the ignored evidence manifest.
2. Enable heart rate, grant Bluetooth permission, select Band 9, and confirm live bpm plus readable zone capsule.
3. Verify manual disconnect shows only `重新连接` + `更换设备`; exercise reconnect, rescan/change-device, opt-out, and saved-device clearing.
4. Toggle Bluetooth off/on during a scan and during a connection; verify Android Bluetooth settings opens and TrainFlow has no crash / ANR.
5. Exercise foreground return and proximity-loss recovery. Record device / Band firmware / Huawei Health context, screenshots, and logcat.

Until those steps pass on the final APK, merge remains blocked.

## Capsule snap-edge resize Repair

The 2026-08-07 acceptance observation found that a capsule snapped to the right could move to the left when its collapsed / expanded size changed. The resize effect reused the drag-release snap function, which inferred LEFT or RIGHT from the current top-left coordinate and the newly measured width. During a resize, that coordinate belongs to the previous size, so it is not a valid nearest-edge release point; in particular, an expanded right-edge x-coordinate can be reclassified as LEFT after collapse.

The Repair keeps the stored `HeartRateCapsuleSnapEdge` as the source of truth for size, viewport, label, expansion, and exclusion-policy repositioning. A narrow geometry helper places the new size at that explicit edge while reusing the existing safe-inset, vertical clamp, and exclusion-zone logic. Pointer drag release still calls the original nearest-edge inference and updates the stored edge.

The focused regression first failed against the pre-Repair behavior for expanded-right top-left `x=70` resized to a 116 px collapsed capsule in a 360 px viewport: nearest-edge inference returned LEFT. The final geometry coverage verifies RIGHT across collapsed / expanded sizes, LEFT across both sizes, and the existing left / right drag-release nearest-edge cases. No capsule dimensions, content, colors, motion, movement threshold, compact fallback, safe-area policy, or BLE / runtime / settings behavior changed.
