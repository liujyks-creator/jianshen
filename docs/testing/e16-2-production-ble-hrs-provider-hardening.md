# E16-2 Production BLE HRS provider hardening

**Status:** Implemented; real-device smoke passed; needs review
**Date:** 2026-07-07
**Scope:** Production-capable BLE Heart Rate Service provider foundation, permission planning, device selection preference, lifecycle hardening

## Goal

E16-2 turns the E16-1 debug-only BLE HRS spike into a production-capable provider foundation without restoring heart-rate UI.

This story does not display heart rate in timed, strength, or follow-along execution pages. It does not write heart-rate samples or summaries to `WorkoutSession`, Room, records, history, trends, analytics, `WorkoutCommand`, `WorkoutEvent`, `TimedWorkoutEngine`, or `StrengthWorkoutEngine`.

## Implementation

Production source-set additions:

- `AndroidBleHeartRateProvider`
  - Android BLE HRS provider boundary in `core.health`.
  - Scans with an explicit `startScan()` call only.
  - Uses a fixed scan window and stops scanning automatically; it does not keep an infinite background scan alive.
  - Requires an in-memory user-selected candidate before connecting.
  - Closes GATT on stop, disconnect, connection failure, missing service / characteristic / CCCD, notify setup failure, and provider close.
  - Maps notify payloads through `HeartRateMeasurementParser` and emits TrainFlow `HeartRateState`; Android BLE SDK models stay inside the provider.
- `BleHeartRateProviderState`
  - Expresses `unavailable / no source`, `permission required`, `bluetooth disabled`, `scanning`, `device found`, `device selected`, `connecting`, `connected waiting for data`, `live bpm`, `stale / disconnected`, `stopped`, and recoverable `error`.
  - Provides `toHeartRateState()` mapping for existing source-aware model consumers.
- `BleHeartRatePermissionPlanner`
  - Android 12+ requires `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
  - Android 11 and below use `ACCESS_FINE_LOCATION` for BLE scan compatibility.
  - Permission requests are allowed only for `EXPLICIT_USER_ACTION`, not app startup or screen open.
- DataStore preference boundary
  - Added nullable `bleHeartRateDeviceIdentifier`.
  - Added nullable `bleHeartRateDeviceDisplayName`.
  - Does not save `BluetoothDevice`, `BluetoothGatt`, SDK model fields, samples, bpm history, or session summaries.

Debug source-set changes:

- `HR Broadcast Smoke` now uses `AndroidBleHeartRateProvider` as a thin manual harness.
- The old debug-only `BleHeartRateProvider` implementation was removed to avoid duplicate lifecycle logic.
- The harness still has explicit buttons for permissions, scan, bonded devices, stop / disconnect, and clear.
- The harness remains debug-only and does not write TrainFlow records.

## Production manifest

E16-2 does not modify `app/src/main/AndroidManifest.xml`.

BLE permissions remain declared only in `app/src/debug/AndroidManifest.xml` for the manual smoke harness. The production-capable provider can compile in `app/src/main`, but production App startup cannot request BLE permissions and no production screen calls scan or connect.

Reason: the story is provider hardening, not user-facing production device integration. Future production UI must first add an explicit opt-in surface, permission rationale, privacy copy, and non-medical wording before moving BLE permissions into the production manifest.

## Device identity and privacy

The selection preference stores only a provider-facing identifier and display name so a future opt-in flow can remember what the user chose.

For BLE HRS devices this identifier may be a Bluetooth address in the current Android callback. That is not guaranteed to be stable across Android privacy behavior, BLE private addresses, Band broadcast mode, device resets, or firmware changes. E16 evidence already showed different Band labels / addresses between bonded and broadcast views. Future production source selection must treat the stored identifier as a convenience hint, not a permanent account or medical-device identity.

## Why UI still stays off

MVP still does not display, record, or trend heart rate because:

- The first product job is plan execution and training records, not monitoring.
- E11.3 removed heart-rate UI after it harmed execution-page layout and value clarity.
- E16-2 only hardens provider / permission / lifecycle foundation.
- A future heart-rate UI still needs user opt-in UX, permission copy, privacy policy, data retention policy, non-medical wording, sampling / stale policy, and visual hierarchy review.
- Future heart-rate UI must first go through `huashu-design` HTML visual direction / high-fidelity case review before Android UI implementation.

## Tests and checks

Focused tests added / updated:

- `BleHeartRateProviderBoundaryTest`
  - Android 12+ permission set.
  - Android 11 and below scan compatibility permission.
  - explicit-user-action-only permission request gate.
  - BLE provider state to `HeartRateState` mapping.
  - recoverable error mapping.
- `TrainFlowPreferencesBoundaryTest`
  - BLE HRS device selection preference persists identifier / display name only.
  - new preference does not enable heart-rate display or disconnected placeholder.
- Existing `HeartRateMeasurementParserTest` remains the parser regression guard.

Verification performed so far:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*HeartRate*" --no-daemon --console=plain
```

Result: passed after loading `.local/env.ps1`.

Review-prep checks performed after the 2026-07-07 real-device evidence:

```powershell
git diff --check origin/main..HEAD
git diff --cached --check
rg -n "心率|heart rate|HeartRate|bpm|未获取心率|平均心率|manual heart|手动心率" app/src/main/java/com/liujyks/trainflow/feature/workoutsession
rg -n "BLUETOOTH_SCAN|BLUETOOTH_CONNECT|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|BODY_SENSORS|android.permission.BLUETOOTH" app/src/main/AndroidManifest.xml
```

## Real-device smoke

E16-2 real-device smoke passed based on the user's 2026-07-07 Android phone screenshots with HUAWEI Band 9 heart-rate broadcast mode enabled.

Smoke conclusions:

- Debug APK entry is fixed as `TrainFlow Debug` -> `DebugEntryActivity`.
- `DebugEntryActivity` shows two explicit buttons: `进入 TrainFlow` and `HR Broadcast Smoke`.
- `HR Broadcast Smoke` is visible only through the debug entry and does not pollute the TrainFlow home page.
- Scan discovered `HUAWEI Band HR-OD7 D8:F0:42:01:90:D7` with `services=[0x180D]`.
- The recoverable `bluetooth_disabled` state was covered.
- Provider state coverage includes `scanning`, `device_found`, `device_selected`, `connecting`, `connected_waiting_for_data`, and live reading states.
- Heart Rate Measurement notify was enabled for standard HRS `0x180D` / `0x2A37`.
- Live bpm values were received continuously: `84`, `85`, `86`, `87`, `88`, `89`, `90`, `91`.
- Stop produced `stopped: BLE HRS provider stopped`, confirming the manual stop path is visible to the harness.
- The user also checked that TrainFlow training pages still have no heart-rate UI.

Review conclusion:

- E16-2 provider hardening is real-device smoke-passed for scan / selected-device / connect / notify / live bpm / stop behavior.
- This evidence closes the E16-2 smoke gap, but it does not change the product boundary: E16-2 is still a production-capable provider foundation, not production heart-rate UI launch.
- No smoke screenshots, logs, APKs, `.local/smoke/` files, or device artifacts are committed.

## Boundaries preserved

- No training execution page heart-rate UI.
- No heart-rate card.
- No `未获取心率` placeholder.
- No manual heart-rate input.
- No average heart-rate trend.
- No Room schema or migration.
- No session record heart-rate write.
- No `WorkoutCommand` or `WorkoutEvent` change.
- No timed or strength engine semantic change.
- No TimerDial, sound, records, history, or trends heart-rate integration.
- No production manifest BLE permissions.
- No production App startup scan, connect, or permission request.
- Future heart-rate UI must first go through `huashu-design` HTML high-fidelity visual gate and should preserve the existing TrainFlow final UI hierarchy instead of redesigning the training layout around heart rate.
