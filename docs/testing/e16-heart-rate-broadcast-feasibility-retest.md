# E16 Heart-rate broadcast feasibility retest

**Status:** Implemented / needs retest; latest screenshot is inconclusive for Band 9 notify
**Date:** 2026-07-05
**Scope:** HUAWEI Band 9 heart-rate broadcast mode on non-Huawei Android

## Why this retest exists

After E11.2a, the user clarified that HUAWEI Band 9 heart-rate broadcast was not enabled during the earlier smoke. The device UI says enabling heart-rate broadcast will connect the band as a third-party Bluetooth device and disconnect Huawei Health.

That changes the interpretation of E11.2a:

- E11.2a remains valid for the condition where Huawei Health is connected and the band is not in heart-rate broadcast mode.
- E11.2a should not be read as proof that Band 9 can never expose standard BLE Heart Rate Service.
- The new retest is limited to the broadcast-on condition.

## Product boundary

This retest does not restore heart-rate UI to the MVP. TrainFlow still does not show heart-rate cards, manual bpm input, missing-heart-rate placeholders, average heart-rate trends, medical alerts, or training interruption decisions.

If the retest succeeds, the next step is an explicit future BLE HRS adapter spike. It is not an automatic production integration and is not an MVP Alpha blocker.

## Test tool

Added debug-only entry points:

- Primary entry: open TrainFlow debug APK, then tap the top home button `HR Broadcast Smoke`.
- Secondary entry: standalone launcher activity label `HR Broadcast Smoke` when the device launcher shows it.
- Activity: `app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`
- Permissions are declared only in `app/src/debug/AndroidManifest.xml`.
- The home button is wired only in debug builds. Release / production builds do not show this entry.

The tool scans all BLE advertisements, lists bonded devices, connects to a selected device, discovers GATT services, and attempts to subscribe to:

- Heart Rate Service `0x180D`
- Heart Rate Measurement characteristic `0x2A37`
- CCCD `0x2902` notification / indication

It logs bpm notifications if received. After the source-label fix, each bpm notification also includes the current GATT source device label. It does not persist data and does not call TrainFlow `HeartRateProvider`.

## Real-device evidence

### 2026-07-05 18:32 screenshot

User screenshot: `C:/Users/25073/Downloads/Screenshot_2026-07-05-18-32-03-26_168a3d1b6f3b71..jpg`.

Observed timeline:

- `18:30:06` to `18:30:10`: log shows repeated `RESULT: heart-rate notify bpm=85 bytes=06 55`.
- `18:30:09`: bonded list shows two devices: `Galaxy Buds Pro (5508)` and `HUAWEI Band 9-OD7 D8:EF:42:01:90:D7`.
- `18:30:11`: first visible `Connecting HUAWEI Band 9-OD7 D8:EF:42:01:90:D7`.
- `18:30:29` and `18:30:32`: additional visible HUAWEI Band 9 connection attempts.
- `18:30:41`: log shows `GATT connection status=147 state=0` followed by `GATT disconnected`.

Interpretation:

- The visible `heart-rate notify bpm=85` lines happened before the first visible HUAWEI Band 9 connection attempt, so they cannot be attributed to `HUAWEI Band 9-OD7` from this screenshot.
- The screenshot proves that Band 9 becomes visible in the bonded device list in broadcast mode.
- The screenshot does not prove Band 9 GATT service discovery, `0x180D`, `0x2A37`, CCCD notify enablement, or Band-attributed bpm notify.
- The visible Band 9 connection attempt appears to fail with GATT `status=147 state=0`.
- The retest remains inconclusive for BLE HRS adapter feasibility until a new log shows source-attributed Band 9 service discovery and notify.

Follow-up tool fix:

- `Clear` now logs that it only clears the log/device list and does not disconnect active GATT.
- Heart-rate notify logs now include `source=<device name address>` so future screenshots can distinguish old active connections from newly tapped devices.

## Retest matrix

1. Baseline: broadcast off, Huawei Health connected.
2. Main retest: enable Band 9 heart-rate broadcast, accept that Huawei Health may disconnect, open TrainFlow, tap the top `HR Broadcast Smoke` button, then scan.
3. Optional: broadcast on, Huawei Health killed / background restricted, then scan again.
4. Cleanup: turn broadcast off and confirm Huawei Health can reconnect normally.

## Pass criteria

BLE HRS feasibility requires all of these:

1. Scan or bonded list shows the Band 9 / broadcast device.
2. GATT services include `0x180D`.
3. `0x180D` contains `0x2A37`.
4. `0x2A37` supports notify or indicate.
5. CCCD write succeeds and the log shows `RESULT: 0x2A37 notify enabled`.
6. While worn and measuring, the log shows `RESULT: heart-rate notify bpm=... source=HUAWEI Band 9-...`.
7. The result records whether Huawei Health disconnects, whether it reconnects after broadcast stops, and whether the connection is stable for at least one short session.

## Fail / inconclusive criteria

- If the band never appears while broadcast is on, the BLE adapter spike remains blocked.
- If the band appears but `0x180D` is missing, it is not a standard BLE HRS route.
- If `0x180D` exists but `0x2A37` or notify is missing, it is not enough for real-time execution heart rate.
- If notifications work only while Huawei Health is disconnected, that must be documented as a user-facing tradeoff before any future feature decision.

## Current recommendation

Run the retest as device research only. Keep MVP Alpha readiness focused on the completed training loop, records, audio coexistence, permissions, and known user-test fixes. Heart-rate broadcast can inform a future User Test Fix Pack / device research story after the user returns real-device logs.
