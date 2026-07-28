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
- zone capsules use the accepted palette as a soft 12–18% tint, a solid accent rail, explicit zone text, and adaptive high-contrast foreground;
- zero geometry, drag/snap, expand/collapse, motion, viewport, safe-area, or IME change;
- no E17-9 active-training FGS or background-ownership work.

## Automated evidence

Automated evidence is valid only for the exact final candidate and executable-equivalent APK recorded in the ignored evidence manifest. Required gates are:

- focused RuntimeOwner platform, settings action/state, application mapper/lifecycle, and capsule tests;
- all `*HeartRate*` unit tests, full debug unit suite, `assembleDebug --rerun-tasks`, `lintDebug` with Kotlin incremental disabled, and `app:check`;
- exact three-dot scope, protected-path, index, geometry-zero-delta, and APK identity checks;
- fresh API 36 AVD install, settings screenshots / UI trees, Bluetooth-settings handoff, and fatal / crash / ANR scan.

## Evidence separation

- The protected `.local/audit/e17-7b-post-human-ux/` capture and APK SHA256 `98C19501AEAB34E01C47F44FEAA091FB7197A47946CD7F77BE9DBF3ECB5D33F5` are stale pre-repair evidence. They explain the repair but cannot accept it.
- Fresh automated and AVD evidence belongs under ignored `.local/smoke/e17-7b-repair-final/`; it is not physical BLE / GATT evidence.
- Final phone + HUAWEI Band 9 evidence is pending and must cover compact actions/settings, manual disconnect/reconnect/change-device, foreground/app-return/proximity recovery, live zone readability, opt-out, and Bluetooth off/on without crash.

## Shortest final human gate

1. Install the final debug APK recorded in the ignored evidence manifest.
2. Enable heart rate, grant Bluetooth permission, select Band 9, and confirm live bpm plus readable zone capsule.
3. Verify manual disconnect shows only `重新连接` + `更换设备`; exercise reconnect, rescan/change-device, opt-out, and saved-device clearing.
4. Toggle Bluetooth off/on during a scan and during a connection; verify Android Bluetooth settings opens and TrainFlow has no crash / ANR.
5. Exercise foreground return and proximity-loss recovery. Record device / Band firmware / Huawei Health context, screenshots, and logcat.

Until those steps pass on the final APK, merge remains blocked.
