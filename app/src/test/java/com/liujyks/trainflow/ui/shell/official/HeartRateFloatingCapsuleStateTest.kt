package com.liujyks.trainflow.ui.shell.official

import com.liujyks.trainflow.core.health.HeartRateRuntimeFact
import com.liujyks.trainflow.core.health.HeartRateSourceHint
import com.liujyks.trainflow.core.health.toHeartRateState
import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import com.liujyks.trainflow.feature.settings.HeartRateBlePermissionStatus
import com.liujyks.trainflow.feature.settings.heartRateSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateFloatingCapsuleStateTest {
    private val source = HeartRateSourceHint("D8:F0:42:01:90:D7", "HUAWEI Band HR-OD7")

    @Test
    fun disabledPreferenceOrDisabledFactHidesCapsule() {
        val preferenceDisabled = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(enabled = false)
        )
        val factDisabled = capsule(HeartRateRuntimeFact.Disabled.toHeartRateState())

        assertFalse(preferenceDisabled.visible)
        assertFalse(factDisabled.visible)
    }

    @Test
    fun permissionBluetoothNotConnectedScanningConnectingAndWaitingUseAccurateCopy() {
        val permission = capsule(HeartRateRuntimeFact.PermissionRequired(source).toHeartRateState())
        val bluetooth = capsule(HeartRateRuntimeFact.BluetoothOff(source).toHeartRateState())
        val notConnected = capsule(HeartRateRuntimeFact.NotConnected(source).toHeartRateState())
        val scanning = capsule(HeartRateRuntimeFact.Scanning(source).toHeartRateState())
        val connecting = capsule(HeartRateRuntimeFact.Connecting(source).toHeartRateState())
        val waiting = capsule(HeartRateRuntimeFact.WaitingFirstData(source).toHeartRateState())

        assertEquals("权限未赋予", permission.collapsedLabel)
        assertEquals("蓝牙关闭", bluetooth.collapsedLabel)
        assertEquals("未连接", notConnected.collapsedLabel)
        assertEquals("正在扫描", scanning.collapsedLabel)
        assertTrue(scanning.detailBody.contains("不代表设备已经连接"))
        assertEquals("正在连接", connecting.collapsedLabel)
        assertTrue(connecting.detailBody.contains("还没有实时心率"))
        assertEquals("等待数据", waiting.collapsedLabel)
        assertTrue(waiting.detailBody.contains("第一条有效"))
    }

    @Test
    fun settingsPermissionStillFailsClosedBeforeContradictoryLiveFact() {
        val state = heartRateFloatingCapsuleUiState(
            settings = settings(permission = HeartRateBlePermissionStatus.DENIED),
            liveState = liveState(105)
        )

        assertEquals(HeartRateFloatingCapsuleStatus.PERMISSION_DENIED, state.status)
        assertEquals("权限未赋予", state.collapsedLabel)
    }

    @Test
    fun savedIdentifierIsAHintNotConnectedFactAndRawAddressIsNotDisplayed() {
        val rawIdentifier = "D8:F0:42:01:90:D7"
        val state = heartRateFloatingCapsuleUiState(
            settings = heartRateSettingsUiState(
                enabled = true,
                savedDeviceIdentifier = rawIdentifier,
                savedDeviceDisplayName = null,
                blePermissionStatus = HeartRateBlePermissionStatus.GRANTED
            )
        )
        val allCopy = buildString {
            append(state.collapsedLabel)
            append(state.detailTitle)
            append(state.detailBody)
            append(state.deviceHint)
            state.infoTiles.forEach { append(it.value) }
        }

        assertEquals(HeartRateFloatingCapsuleStatus.SAVED_DEVICE, state.status)
        assertEquals("未连接", state.collapsedLabel)
        assertFalse(allCopy.contains(rawIdentifier))
        assertTrue(allCopy.contains("自动恢复"))
        assertFalse(allCopy.contains("仅供主动连接"))
    }

    @Test
    fun liveBpmAndOptionalZoneRemainVisualOnlyAndDoNotClaimRecording() {
        val bpmOnly = capsule(liveState(105))
        val zoned = capsule(liveState(122), age = 40)
        val overLimit = capsule(liveState(188), age = 40, overLimit = 180)

        assertEquals(HeartRateFloatingCapsuleStatus.BPM_ONLY, bpmOnly.status)
        assertEquals("心率 105 bpm", bpmOnly.collapsedLabel)
        assertEquals("当前只显示", bpmOnly.tile("记录"))
        assertEquals(HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN, zoned.status)
        assertEquals("燃脂 122 bpm", zoned.collapsedLabel)
        assertEquals(HeartRateFloatingCapsuleStatus.OVER_LIMIT, overLimit.status)
        assertTrue(overLimit.detailBody.contains("不触发声音、震动、强制暂停或医疗告警"))
    }

    @Test
    fun age101IsValidAndPersonalMaximumTakesPrecedenceOverAgeEstimate() {
        val ageOnly = capsule(liveState(84), age = 101)
        val personalMaximum = capsule(
            liveState(150),
            age = 40,
            personalMax = 200
        )

        assertEquals(HeartRateFloatingCapsuleStatus.ZONE_AEROBIC, ageOnly.status)
        assertEquals("有氧 84 bpm", ageOnly.collapsedLabel)
        assertEquals(HeartRateFloatingCapsuleStatus.ZONE_AEROBIC, personalMaximum.status)
        assertEquals("有氧 150 bpm", personalMaximum.collapsedLabel)
        assertTrue(personalMaximum.detailBody.contains("个人最大心率"))
    }

    @Test
    fun zoneBoundariesUseExactUnroundedRatio() {
        val expected = listOf(
            99 to HeartRateFloatingCapsuleStatus.ZONE_LOW,
            100 to HeartRateFloatingCapsuleStatus.ZONE_WARMUP,
            119 to HeartRateFloatingCapsuleStatus.ZONE_WARMUP,
            120 to HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN,
            139 to HeartRateFloatingCapsuleStatus.ZONE_FAT_BURN,
            140 to HeartRateFloatingCapsuleStatus.ZONE_AEROBIC,
            159 to HeartRateFloatingCapsuleStatus.ZONE_AEROBIC,
            160 to HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC,
            179 to HeartRateFloatingCapsuleStatus.ZONE_ANAEROBIC,
            180 to HeartRateFloatingCapsuleStatus.ZONE_LIMIT,
            200 to HeartRateFloatingCapsuleStatus.ZONE_LIMIT,
            201 to HeartRateFloatingCapsuleStatus.ZONE_LIMIT
        )

        expected.forEach { (bpm, status) ->
            assertEquals(status, capsule(liveState(bpm), personalMax = 200).status)
        }
    }

    @Test
    fun alertIsIndependentStrictAndWorksWithoutEffectiveMaximum() {
        val equal = capsule(liveState(180), overLimit = 180)
        val exceeded = capsule(liveState(181), overLimit = 180)
        val invalidLow = capsule(liveState(100), overLimit = 29)

        assertEquals(HeartRateFloatingCapsuleStatus.BPM_ONLY, equal.status)
        assertEquals(HeartRateFloatingCapsuleStatus.OVER_LIMIT, exceeded.status)
        assertEquals(HeartRateFloatingCapsuleStatus.BPM_ONLY, invalidLow.status)
    }

    @Test
    fun interruptedDisconnectFailureAndStopNeverDisplayOldBpmAsCurrent() {
        val interrupted = capsule(HeartRateRuntimeFact.DataInterrupted(source).toHeartRateState())
        val disconnected = capsule(HeartRateRuntimeFact.LinkDisconnected(source).toHeartRateState())
        val failed = capsule(
            HeartRateRuntimeFact.TechnicalFailure(
                HeartRateTechnicalFailure.PLATFORM_FAILURE,
                source
            ).toHeartRateState()
        )
        val stopped = capsule(HeartRateRuntimeFact.IntentionalStop(source).toHeartRateState())

        assertEquals("数据中断", interrupted.collapsedLabel)
        assertEquals(HeartRateFloatingCapsuleStatus.STALE, interrupted.status)
        assertEquals("连接已断开", disconnected.collapsedLabel)
        assertEquals(HeartRateFloatingCapsuleStatus.OFFLINE, disconnected.status)
        assertEquals("连接异常", failed.collapsedLabel)
        assertEquals(HeartRateFloatingCapsuleStatus.ERROR, failed.status)
        assertEquals("已停止", stopped.collapsedLabel)
        listOf(interrupted, disconnected, failed, stopped).forEach { state ->
            assertFalse(state.collapsedLabel.contains("105"))
            assertFalse(state.detailBody.contains("105"))
        }
    }

    @Test
    fun platformDetailsAddressAndExceptionTextNeverEnterCapsuleCopy() {
        val raw = "status=19 D8:F0:42:01:90:D7 java.lang.IllegalStateException"
        val invalidPublicState = HeartRateState(
            kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
            sourceKind = HeartRateSourceKind.DEVICE,
            fact = HeartRateFact.TECHNICAL_FAILURE,
            sourceId = "D8:F0:42:01:90:D7",
            message = raw,
            technicalFailure = HeartRateTechnicalFailure.PLATFORM_FAILURE
        )
        val state = capsule(invalidPublicState)
        val copy = state.collapsedLabel + state.detailTitle + state.detailBody + state.deviceHint

        assertEquals(HeartRateFloatingCapsuleStatus.ERROR, state.status)
        assertFalse(copy.contains("status=19"))
        assertFalse(copy.contains("D8:F0"))
        assertFalse(copy.contains("IllegalStateException"))
    }

    @Test
    fun interruptedOutcomeFailsClosedWithoutCachedReadingOrErrorCopy() {
        val mapped = HeartRateRuntimeFact.DataInterrupted(
            HeartRateSourceHint(identifier = "id", displayName = "Band")
        ).toHeartRateState()
        val state = capsule(mapped)
        val copy = buildString {
            append(state.collapsedLabel)
            append(state.detailTitle)
            append(state.detailBody)
            append(state.deviceHint)
            state.infoTiles.forEach { append(it.value) }
        }

        assertEquals(HeartRateFact.DATA_INTERRUPTED, mapped.fact)
        assertEquals(HeartRateFloatingCapsuleStatus.STALE, state.status)
        assertNull(mapped.bpm)
        assertNull(mapped.measuredAt)
        assertNull(mapped.recordedAt)
        assertNull(mapped.message)
        assertNull(mapped.technicalFailure)
        assertFalse(mapped.fact == HeartRateFact.LIVE)
        assertFalse(mapped.fact == HeartRateFact.LINK_DISCONNECTED)
        assertFalse(mapped.fact == HeartRateFact.TECHNICAL_FAILURE)
        assertFalse(copy.contains("88"))
        assertFalse(copy.contains("raw parse failure"))
        assertFalse(copy.contains("parse"))
    }

    @Test
    fun legacyManualAndDeviceKindsAreNotNewPresentationInputs() {
        val legacyStates = listOf(
            HeartRateState(
                kind = HeartRateStateKind.MANUAL_READING,
                sourceKind = HeartRateSourceKind.MANUAL,
                bpm = 126,
                recordedAt = "legacy"
            ),
            HeartRateState(
                kind = HeartRateStateKind.DEVICE_READING,
                sourceKind = HeartRateSourceKind.DEVICE,
                bpm = 105,
                measuredAt = "legacy"
            )
        )

        legacyStates.forEach { legacy ->
            val state = capsule(legacy)
            assertEquals(HeartRateFloatingCapsuleStatus.NO_SOURCE, state.status)
            assertFalse(state.collapsedLabel.contains("bpm"))
        }
    }

    @Test
    fun frozenPresentationDtoFieldsAndInfoHierarchyRemainAvailable() {
        val state = capsule(liveState(105), forceCollapsed = true)

        assertTrue(state.visible)
        assertTrue(state.forceCollapsed)
        assertEquals(
            listOf("来源", "记录", "区间", "更新"),
            state.infoTiles.map { it.label }
        )
        assertEquals("实时", state.tile("更新"))
    }

    private fun liveState(bpm: Int) = HeartRateRuntimeFact.Live(
        bpm = bpm,
        measuredAt = "2026-07-19T13:16:04Z",
        source = source
    ).toHeartRateState()

    private fun settings(
        permission: HeartRateBlePermissionStatus = HeartRateBlePermissionStatus.GRANTED
    ) = heartRateSettingsUiState(
        enabled = true,
        blePermissionStatus = permission
    )

    private fun capsule(
        state: HeartRateState,
        age: Int? = null,
        personalMax: Int? = null,
        overLimit: Int? = null,
        forceCollapsed: Boolean = false
    ) = heartRateFloatingCapsuleUiState(
        settings = settings(),
        liveState = state,
        userAgeYears = age,
        personalMaxHeartRateBpm = personalMax,
        overLimitThresholdBpm = overLimit,
        forceCollapsed = forceCollapsed
    )

    private fun HeartRateFloatingCapsuleUiState.tile(label: String) =
        infoTiles.first { it.label == label }.value
}
