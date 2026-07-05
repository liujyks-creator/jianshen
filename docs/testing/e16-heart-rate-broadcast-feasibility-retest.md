# E16 Heart-rate broadcast feasibility retest

**Status:** Positive BLE HRS evidence reviewed / merged to main; future adapter spike only
**Date:** 2026-07-05
**Scope:** HUAWEI Band 9 heart-rate broadcast mode on non-Huawei Android
**Main merge:** `bbd4296`

## Why this retest exists

After E11.2a, the user clarified that HUAWEI Band 9 heart-rate broadcast was not enabled during the earlier smoke. The device UI says enabling heart-rate broadcast will connect the band as a third-party Bluetooth device and disconnect Huawei Health.

That changes the interpretation of E11.2a:

- E11.2a remains valid for the condition where Huawei Health is connected and the band is not in heart-rate broadcast mode.
- E11.2a should not be read as proof that Band 9 can never expose standard BLE Heart Rate Service.
- The new retest is limited to the broadcast-on condition.

## Product boundary

This retest does not restore heart-rate UI to the MVP. TrainFlow still does not show heart-rate cards, manual bpm input, missing-heart-rate placeholders, average heart-rate trends, medical alerts, or training interruption decisions.

If the retest succeeds, the only valid next step is a separately scoped `E16-1 BLE HRS adapter spike`. It is not an automatic production integration, not a production UI change, and not an MVP Alpha blocker. Before any future heart-rate value is shown in TrainFlow, the product must first go through an HTML visual direction / high-fidelity case review and only then consider Android UI implementation.

## Test tool

Added debug-only entry point:

- Primary entry: standalone debug launcher activity label `HR Broadcast Smoke` when the device launcher shows it.
- ADB fallback: `adb shell am start -n com.liujyks.trainflow/.app.HeartRateBroadcastSmokeActivity`.
- Activity: `app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`
- Permissions are declared only in `app/src/debug/AndroidManifest.xml`.
- There is no home-screen button, route, callback, or Activity reference in `app/src/main`. Release / production builds do not show this entry.

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

### 2026-07-05 18:46 screenshots

User screenshots:

- `C:/Users/25073/Downloads/Screenshot_2026-07-05-18-46-32-34_168a3d1b6f3b71..jpg`
- `C:/Users/25073/Downloads/Screenshot_2026-07-05-18-46-26-65_168a3d1b6f3b71..jpg`

Observed timeline:

- `18:46:07`: scan sees `HRS_ADV rssi=-46 name=HUAWEI Band HR-OD7 address=D8:F0:42:01:90:D7 services=[0x180D]`.
- `18:46:08`: scan sees the same device again with `services=[0x180D]`.
- `18:46:08`: scan stops.
- `18:46:08`: tool connects to the same address: `Connecting HUAWEI Band HR-OD7 D8:F0:42:01:90:D7`.
- `18:46:08`: GATT connects successfully: `GATT connection status=0 state=2`, then starts service discovery.
- `18:46:09`: service discovery succeeds: `Services discovered status=0 count=9`.
- `18:46:09`: services include `service 0x180D`.
- `18:46:09`: `service 0x180D` contains `characteristic 0x2A37 props=notify`.
- `18:46:09`: result lines confirm `RESULT: HRS 0x180D found` and `RESULT: characteristic 0x2A37 found props=notify`.
- `18:46:09`: notify setup succeeds: `setCharacteristicNotification=true`, `write CCCD result=0`, `Descriptor write 0x2902 status=0 for 0x2A37`, and `RESULT: 0x2A37 notify enabled`.
- `18:46:10` onward: the same connection receives heart-rate notifications, starting with `RESULT: heart-rate notify bpm=90 bytes=06 5A`, then `89`, `88`, `87`, etc.

Interpretation:

- This is positive evidence that Band 9 heart-rate broadcast mode can expose standard BLE Heart Rate Service `0x180D`.
- The connected broadcast device exposes Heart Rate Measurement `0x2A37` with notify support.
- CCCD subscription succeeds and bpm notifications arrive after the successful connection and notify setup.
- The device name during broadcast appears as `HUAWEI Band HR-OD7`, with address `D8:F0:42:01:90:D7`; this differs from the earlier bonded label `HUAWEI Band 9-OD7 D8:EF:42:01:90:D7`, so future implementation should not rely on the paired label or static address alone.
- This result is sufficient to justify the separately scoped future `E16-1 BLE HRS adapter spike`, but it still does not restore MVP heart-rate UI or create production device integration.

## Retest matrix

1. Baseline: broadcast off, Huawei Health connected.
2. Main retest: enable Band 9 heart-rate broadcast, accept that Huawei Health may disconnect, open the debug-only `HR Broadcast Smoke` launcher activity or start it with `adb shell am start -n com.liujyks.trainflow/.app.HeartRateBroadcastSmokeActivity`, then scan.
3. Optional: broadcast on, Huawei Health killed / background restricted, then scan again.
4. Cleanup: turn broadcast off and confirm Huawei Health can reconnect normally.

## Pass criteria

BLE HRS feasibility requires all of these:

1. Scan or bonded list shows the Band 9 / broadcast device.
2. GATT services include `0x180D`.
3. `0x180D` contains `0x2A37`.
4. `0x2A37` supports notify or indicate.
5. CCCD write succeeds and the log shows `RESULT: 0x2A37 notify enabled`.
6. While worn and measuring, the log shows `RESULT: heart-rate notify bpm=...` after connecting and enabling notify on the same Band broadcast address. Newer smoke logs should also show `source=HUAWEI Band ...`.
7. The result records whether Huawei Health disconnects, whether it reconnects after broadcast stops, and whether the connection is stable for at least one short session.

## Fail / inconclusive criteria

- If the band never appears while broadcast is on, the BLE adapter spike remains blocked.
- If the band appears but `0x180D` is missing, it is not a standard BLE HRS route.
- If `0x180D` exists but `0x2A37` or notify is missing, it is not enough for real-time execution heart rate.
- If notifications work only while Huawei Health is disconnected, that must be documented as a user-facing tradeoff before any future feature decision.

## Current recommendation

Treat the 18:46 retest as successful device research evidence. The next step, if heart-rate work is prioritized later, is only the separate `E16-1 BLE HRS adapter spike` with connection lifecycle, source labelling, permissions, UX opt-in, and non-medical boundaries. It must not directly restore production heart-rate UI. Before a future heart-rate display is implemented, TrainFlow must first complete an HTML visual direction / high-fidelity case review and then enter a separate Android UI story. Keep MVP Alpha readiness focused on the completed training loop, records, audio coexistence, permissions, and known user-test fixes.
