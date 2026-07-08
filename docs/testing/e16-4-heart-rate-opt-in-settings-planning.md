# E16-4 Heart-rate opt-in / settings / permission rationale planning

**Status:** Docs-only planning complete
**Date:** 2026-07-08
**Scope:** Future heart-rate opt-in, settings, permission rationale, privacy, non-medical copy, state entry, and follow-up story split

## Context

E16-2 proved a production-capable BLE HRS provider boundary in `core.health`, and E16-3a settled the future display direction as an in-app draggable floating heart-rate capsule. E16-4 does not implement Android UI, does not add production BLE permissions, and does not connect provider output to records.

This planning gate answers how a user deliberately turns heart rate on, chooses or changes a device, grants permissions, understands data use, and turns the feature off before any Android implementation story starts.

## Product Decision

Heart-rate display is default off. The user must explicitly enable it from settings or a capsule/settings entry after seeing a short explanation of use, privacy, permissions, and non-medical limits.

The canonical entry is **Settings -> Training preferences -> Heart rate**. The same destination may also be reachable from a lightweight device/status row and from the expanded floating capsule after the user has enabled the feature. First-use guidance is a rationale screen inside this settings flow, not an automatic app-launch prompt.

## Entry Points

| Entry | Role | Boundary |
|---|---|---|
| Training preferences / Heart rate | Canonical setup, opt-in, age, threshold, selected device, clear device, disable | This is the only required first implementation entry. |
| Device status row | Shortcut to source status and device picker | It should never scan silently; scan starts only after the user taps choose / scan. |
| Floating capsule expanded state | Quick status and link back to settings | Available only after heart-rate preference is on; it is not the first-use permission trigger by itself. |
| First enable guide | One-time rationale inside settings | Explains purpose, permissions, privacy, and non-medical limits before scanning or permission request. |

Home may later show a small device-status shortcut, but it must not compete with the main training entry or imply heart rate is required for TrainFlow.

## Explicit Opt-in Flow

1. User opens `Heart rate` in training preferences.
2. Default state shows `关闭` and explains: `开启后，TrainFlow 可以在 App 内显示来自已连接心率设备的实时心率。未训练时只显示，不记录。`
3. User taps `开启心率显示`.
4. App shows a short rationale sheet/page:
   - What it is for: training reference during TrainFlow use.
   - What permissions may be requested: nearby Bluetooth scan/connect, and Android 11 or below location compatibility for scanning.
   - What is not used: no system overlay permission, no background infinite scanning, no silent scanning.
   - Privacy and non-medical copy.
5. User taps `选择设备`.
6. Only now may the app request required BLE permissions and start a bounded scan window.
7. After device selection, the setting becomes on and the in-app floating capsule can appear on TrainFlow pages.

If the user backs out before choosing a device, keep the feature off or show `已开启，未连接源` only if the user explicitly enabled display without selecting a source. The first Android story should prefer the simpler rule: enabling requires completing permission/source setup or saving an explicit `enabled but no source` state from the rationale page.

## Device Selection And Source Status

Device selection stores only:

- `identifier`: provider-level selected-device hint.
- `displayName`: user-visible label at selection time.

It must not store:

- `BluetoothDevice`
- `BluetoothGatt`
- GATT service/characteristic handles
- Android BLE SDK model
- bpm samples
- session summary
- medical-device identity

Because Android privacy, BLE private addresses, and wearable broadcast labels can change, the saved identifier is only a convenience hint. The settings UI should say: `TrainFlow 会记住你选择过的设备名称，方便下次连接；如果设备名称或地址变化，你可能需要重新选择。`

Recommended source status labels:

| State | Settings text | Capsule text |
|---|---|---|
| Preference off | `心率显示已关闭` | No capsule |
| Enabled, no source | `未选择心率设备` | `未连接源` |
| Permission denied | `缺少蓝牙权限，无法扫描或连接心率设备` | `权限未赋予` |
| Bluetooth off | `蓝牙已关闭，开启后可连接已选择设备` | `蓝牙关闭` |
| Scanning | `正在查找附近的心率设备` | `正在连接` or `等待数据` |
| Device found | `发现设备：{displayName}` | Usually no capsule change until selected |
| Selected, connecting | `正在连接 {displayName}` | `正在连接` |
| Connected, waiting | `{displayName} 已连接，等待心率数据` | `等待数据` |
| Live bpm | `{displayName} 正在提供心率` | Zone + bpm, or `心率 {bpm} bpm` without age |
| Stale | `最近没有收到新的心率数据` | `数据过期` |
| Offline | `{displayName} 当前离线` | `离线` |

Changing devices always goes through `更换设备` -> permission check if needed -> bounded scan -> select. Clearing a device removes identifier/display name but does not necessarily turn the feature off; the recommended first implementation should keep the feature on and show `未连接源`, while also offering `关闭心率显示`.

## Permission Rationale Copy

Recommended short copy:

> TrainFlow 使用蓝牙查找并连接你主动选择的心率设备，用于在 App 内显示训练参考心率。

Bullets:

- `扫描附近设备` 用于发现支持标准心率广播的设备。
- `连接设备` 用于接收实时心率数据。
- Android 12 及以上可能请求 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`。
- Android 11 及以下扫描蓝牙设备可能需要位置兼容权限；TrainFlow 不用它定位你的位置。
- TrainFlow 不使用系统悬浮窗权限，心率胶囊只显示在 TrainFlow App 内。
- TrainFlow 不会在后台无限扫描，不会在没有提示的情况下开始扫描。
- 关闭心率显示后，不扫描、不连接、不记录心率。

Permission request timing:

- Do not request BLE permission on app start.
- Do not request BLE permission just because the user opens TrainFlow.
- Do not request BLE permission just because a workout starts.
- Request only after the user taps `开启心率显示` / `选择设备` / `重新扫描` and has seen rationale.

## Privacy Copy

Recommended settings copy:

> 心率数据来自你主动选择并连接的心率设备。未训练时，TrainFlow 只在 App 内显示当前状态或实时心率，不写入训练记录。训练中未来可按 1 秒采样保存，用于训练后回顾；记录模型会在单独 story 中实现。

Detailed boundaries:

- Source is the selected heart-rate provider/device, not a manual input field.
- Non-workout display is live-only.
- Active workout recording is future work and should cover timed and strength sessions only after the recording model exists.
- Future 1-second sampling must preserve source kind, source label, bpm, measured time, recorded time, and session context.
- Do not infer historical trends from transient `HeartRateState`.
- Do not upload heart-rate data or introduce cloud sync without a separate product and privacy decision.

## Non-medical Copy

Recommended short copy:

> 心率区间只作为训练参考，不用于诊断疾病，也不能替代医生建议。

Rules:

- Zones are training references based on user settings such as age and threshold.
- `超过上限` is a visual prompt only.
- Do not use disease, danger, emergency, diagnosis, treatment, or medical monitoring wording.
- Do not auto-pause, auto-end, or block a workout because of heart rate.
- Do not play sound, vibrate, or send notifications for over-limit in the first implementation.
- If the user feels unwell, use generic self-care wording only: `如有不适，请停止训练并寻求专业建议。`

## Disable Behavior

When the user turns heart-rate display off:

- Floating capsule disappears immediately.
- Provider stops scanning.
- Provider disconnects from active GATT if connected.
- No reconnect is attempted.
- No new samples are recorded.
- Existing selected-device identifier/display name may be retained as a convenience, but settings must offer `清除已保存设备`.

Recommended settings structure:

- Toggle: `心率显示`
- Status: current permission / Bluetooth / device / live state
- Action: `选择设备` or `更换设备`
- Action: `清除已保存设备`
- Action: `关闭心率显示`

If off with a saved device, show: `已关闭。TrainFlow 不会扫描或连接。已保存的设备名称仅用于下次你重新开启时快速选择。`

## Denied / Off / Offline / Stale Copy

| Condition | Settings copy | Capsule / compact copy |
|---|---|---|
| Permission denied | `蓝牙权限未授予。TrainFlow 需要你主动授权后，才能扫描和连接心率设备。` | `权限未赋予` |
| Permission denied permanently | `权限已被系统拒绝。请到系统设置中重新允许蓝牙权限。` | `权限未赋予` |
| Bluetooth off | `蓝牙已关闭。开启蓝牙后，TrainFlow 才能连接已选择的心率设备。` | `蓝牙关闭` |
| No selected device | `还没有选择心率设备。` | `未连接源` |
| Device offline | `未找到已选择的设备。请确认设备已开启心率广播并靠近手机。` | `离线` |
| Waiting for data | `设备已连接，正在等待心率数据。` | `等待数据` |
| Stale data | `最近没有收到新的心率数据。当前数值不会作为实时心率展示。` | `数据过期` |
| Scan timeout | `这次没有找到心率设备。你可以确认设备广播已开启后重新扫描。` | `未连接源` |

Stale policy for later implementation:

- A stale reading must not be shown as live bpm.
- Capsule can show `数据过期` and optionally a subdued last-known detail in expanded state.
- Stale samples must not be recorded as fresh 1-second samples.
- Exact stale threshold should be defined in the Android state-mapping story; start conservative and test with real Band 9 behavior.

## Settings IA

Recommended first implementation page:

```text
设置
  训练偏好
    心率
      心率显示: 开 / 关
      当前状态: 权限 / 蓝牙 / 设备 / 数据状态
      选择设备 / 更换设备
      年龄: optional, used only for zone display
      上限提示阈值: optional, visual-only
      隐私与非医疗说明
      清除已保存设备
```

Age is used only for zone classification. If age is missing, show bpm without zone, for example `心率 105 bpm`.

## Follow-up Android Story Split

Recommended sequencing:

1. **E16-5 Settings / opt-in UI**
   Add the settings IA, default-off preference, rationale page, age / threshold fields, saved-device display, disable / clear-device actions. No scanning yet if needed.

2. **E16-6 Permission request flow**
   Add production manifest permissions only in this story if approved; request BLE scan/connect only after explicit user action and rationale. No app-start or workout-start permission request.

3. **E16-7 Device picker / source status**
   Bounded scan, select device, save identifier/display name, reconnect status, denied / Bluetooth off / timeout copy. No records.

4. **E16-8 App-shell floating capsule implementation**
   Implement in-app overlay capsule from E16-3a, safe snap, tap/drag threshold, collapsed/expanded states. No session persistence yet.

5. **E16-9 `HeartRateState` -> capsule mapping**
   Map provider states, bpm-only, zone + bpm, stale, offline, over-limit visual-only state. Define stale threshold and last-known display rules.

6. **E16-10 Stale / offline policy hardening**
   Real-device policy tests for Band 9 disconnect, broadcast stop, Bluetooth off, permission revoke, scan timeout, GATT close cleanup.

7. **E16-11 Recording model / 1-second sampling persistence**
   Design and implement samples separately from `HeartRateState`, likely with dedicated sample records and session summary derivation. This is the first story allowed to touch Room/session record/history inputs.

8. **E16-12 Analysis / zones / post-workout summary**
   Derive average, peak, zone duration, over-limit duration, rest recovery, and workout curve. Keep non-medical wording and do not restore old average-heart-rate trend as the only output.

## Explicit Non-goals For E16-4

- No Android Kotlin changes.
- No production manifest / Gradle changes.
- No BLE permission additions.
- No Room / session record / records / history / trends changes.
- No `WorkoutCommand` / `WorkoutEvent` changes.
- No `TimedWorkoutEngine` / `StrengthWorkoutEngine` changes.
- No `TimerDial` changes.
- No sound, vibration, notification, or cue changes.
- No old heart-rate card, `-- bpm`, `未获取心率` placeholder, manual heart-rate input, or old average-heart-rate trend.

## Acceptance Check

- Default off and explicit opt-in are required.
- System overlay permission is explicitly out of scope.
- BLE permissions are requested only after user action and rationale.
- Non-workout display is live-only and not recorded.
- Training recording remains a separate future model.
- Selected device preference stores only identifier/display name.
- Permission, Bluetooth off, offline, and stale copy boundaries are defined.
- Non-medical copy is defined.
- E16-5 and later story split is defined.
