# E16-1 BLE HRS adapter spike

**Status:** Implemented; real-device smoke passed; needs review
**Date:** 2026-07-06
**Scope:** Standard BLE Heart Rate Service adapter spike for debug verification only

## Goal

Validate the minimum Android path for standard BLE Heart Rate Service:

- Heart Rate Service `0x180D`
- Heart Rate Measurement `0x2A37`
- notify / CCCD `0x2902`
- Heart Rate Measurement payload parsing
- adapter status flow mapped to TrainFlow `HeartRateState`

This story does not restore heart-rate UI to the MVP. It does not write heart-rate samples to `WorkoutSession`, Room, records, history, trends, or analytics. It does not connect heart rate to timed or strength workout execution pages.

## Implementation

Code added / changed:

- `app/src/main/java/com/liujyks/trainflow/core/health/HeartRateMeasurementParser.kt`
  - Pure Kotlin parser for BLE Heart Rate Measurement payloads.
  - Handles 8-bit bpm, 16-bit little-endian bpm, flags, sensor-contact bits, energy expended flag, RR interval flag, and empty / malformed payloads.
  - Does not depend on Android BLE SDK classes.
- `app/src/test/java/com/liujyks/trainflow/core/health/HeartRateMeasurementParserTest.kt`
  - Focused parser tests for 8-bit bpm, 16-bit bpm, flags, and malformed / empty payloads.
- `app/src/debug/java/com/liujyks/trainflow/core/health/BleHeartRateProvider.kt`
  - Debug-only BLE HRS adapter spike.
  - Scans BLE advertisements, lists bonded devices, connects to selected device, discovers services, subscribes to `0x2A37`, parses notify payloads, and maps bpm to source-aware `HeartRateState`.
  - Emits debug adapter statuses for `scanning`, `device found`, `connecting`, `service discovered`, `characteristic found`, `notify enabled`, `bpm received`, `disconnected`, `stopped`, and `error`.
  - Android BLE SDK models stay inside the debug provider; they do not leak to training engine, production UI, records, or history.
- `app/src/debug/java/com/liujyks/trainflow/app/HeartRateBroadcastSmokeActivity.kt`
  - Reuses the debug-only provider as a test harness.
  - Keeps the launcher label `HR Broadcast Smoke`.
  - Shows the adapter status stream and candidate device buttons.

Production source-set change:

- Only the pure parser is in `app/src/main`.
- No production BLE provider, production UI route, production manifest permission, Room schema, session record, `WorkoutCommand`, `WorkoutEvent`, or training engine change was added.
- Default production heart-rate behavior remains `DisabledHeartRateProvider` / no-source behavior.

## Debug smoke path

Use a real Android phone plus HUAWEI Band 9 in heart-rate broadcast mode. AVD can verify the debug Activity exists, but cannot prove BLE peripheral feasibility.

1. Build and install debug APK.
2. Open launcher activity `HR Broadcast Smoke`, or start:

```powershell
adb shell am start -n com.liujyks.trainflow/.app.HeartRateBroadcastSmokeActivity
```

3. Tap `Grant Bluetooth Permissions`.
4. Enable Band 9 heart-rate broadcast mode.
5. Tap `Scan All BLE Devices`.
6. Select `HUAWEI Band HR-*`.
7. Record log evidence for:
   - `Scan started`
   - `scan HRS_ADV ... services=[0x180D]`
   - `Connecting ...`
   - `Services discovered ...`
   - `RESULT: HRS 0x180D found`
   - `RESULT: characteristic 0x2A37 found props=notify`
   - `RESULT: 0x2A37 notify enabled`
   - `RESULT: heart-rate notify bpm=... source=...`
   - `GATT disconnected` or `adapter stopped`

Smoke evidence should be saved locally under:

```text
.local/smoke/e16-1-ble-hrs-adapter-spike/
```

Do not commit screenshots, logs, APKs, `.local/`, or device output.

## Real-device smoke result

Real-device smoke passed on a real Android phone with HUAWEI Band 9 heart-rate broadcast mode enabled. Screenshot evidence time was approximately 2026-07-06 01:14 and has been accepted for review.

Accepted evidence chain:

- `Connecting HUAWEI Band HR-OD7 D8:F0:42:01:90:D7`
- `GATT connection status=0 state=2`
- `Services discovered status=0 count=9`
- `service 0x180D`
- `characteristic 0x2A37 props=notify`
- `RESULT: HRS 0x180D found`
- `RESULT: characteristic 0x2A37 found props=notify`
- `setCharacteristicNotification=true`
- `write CCCD result=0`
- `Descriptor write 0x2902 status=0 for 0x2A37`
- `RESULT: 0x2A37 notify enabled`
- `RESULT: heart-rate notify bpm=100 flags=0x6 format=uint8 ... bytes=06 64`
- `RESULT: heart-rate notify bpm=99 flags=0x6 format=uint8 ... bytes=06 63`
- `Closing GATT`
- `adapter stopped`

Conclusion:

```text
passed: Band 9 broadcast -> BLE HRS adapter -> HeartRateState bpm flow
```

## Current conclusion

E16 already proved that Band 9 heart-rate broadcast mode can expose standard BLE HRS on the user's real device. E16-1 now packages that positive path into a reusable debug-only adapter spike, validates the standard payload parser in unit tests, and has passed the real-device Band 9 broadcast smoke path through `HeartRateState` bpm flow.

The spike is enough to continue future production planning for a BLE HRS source, but it is not a production feature.

## Productionization gaps

Before production heart rate can be enabled, TrainFlow still needs separate stories for:

- User opt-in and source selection UX.
- Production permission rationale and Android 12+ Bluetooth permission flow.
- Connection lifecycle policy: scan windows, reconnect, stop, background limits, and battery behavior.
- Device identity and source labelling across changing Band broadcast names / addresses.
- Error states and user-facing recovery copy.
- Non-medical disclaimer and no-danger-alert boundary.
- Data sampling policy if records are ever added.
- Privacy / retention design for any future persisted samples.
- HTML visual direction / high-fidelity case review before any training execution page heart-rate UI.

## Why MVP heart-rate UI stays off

MVP still does not display, record, or trend heart rate because:

- The product value of the first version is training plan execution and records, not device monitoring.
- E11.3 removed heart-rate UI after manual input distorted the execution page and added low-value complexity.
- E16 / E16-1 only prove a technical adapter path under Band 9 broadcast mode.
- Production heart rate needs permission, source, opt-in, privacy, lifecycle, and non-medical UX decisions.
- Future execution-page heart-rate display must first go through HTML visual direction / high-fidelity case review, then a separate Android UI implementation story.

## Verification

Commands run with `.local/env.ps1`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HeartRate*" --no-daemon --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "*HeartRate*" :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:lintDebug --no-daemon --console=plain "-Dkotlin.incremental=false"
.\gradlew.bat :app:check --no-daemon --console=plain
git diff --check
```

Results:

- `:app:testDebugUnitTest --tests "*HeartRate*"` passed.
- `:app:assembleDebug` passed.
- `:app:testDebugUnitTest` passed.
- `:app:lintDebug` passed after stopping Gradle daemons and rerunning sequentially with `"-Dkotlin.incremental=false"`. A previous parallel lint/check attempt hit Kotlin incremental cache registration conflicts and was not treated as a code failure.
- `:app:check` passed.
- `git diff --check` passed with Windows line-ending warnings only.
- `.local/env.ps1` restored the local JDK / Android SDK environment.
- AVD list showed `TrainFlow_Pixel_API_36`.
- `adb` server returned a local protocol fault, so emulator launch smoke was not completed in this pass.
- Permission scan showed Bluetooth / location permissions only under `app/src/debug`; `app/src/main` did not gain Bluetooth permissions.
- Forbidden heart-rate UI keyword scan found only historical / boundary documentation references, not restored production UI.

Real-device Band 9 smoke:

- Passed on real Android phone + HUAWEI Band 9 heart-rate broadcast mode.
- Evidence accepted for review: scan/connect/discover `0x180D`, find `0x2A37 notify`, enable CCCD `0x2902`, receive bpm notify payloads `06 64` and `06 63`, close GATT, and stop adapter.
- Final conclusion: `passed: Band 9 broadcast -> BLE HRS adapter -> HeartRateState bpm flow`.
