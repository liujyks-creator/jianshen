package com.liujyks.trainflow.core.health

import com.liujyks.trainflow.core.model.HeartRateFact
import com.liujyks.trainflow.core.model.HeartRateSourceKind
import com.liujyks.trainflow.core.model.HeartRateState
import com.liujyks.trainflow.core.model.HeartRateStateContractViolation
import com.liujyks.trainflow.core.model.HeartRateStateKind
import com.liujyks.trainflow.core.model.HeartRateTechnicalFailure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateProviderBoundaryTest {
    private val source = HeartRateSourceHint("saved-id", "Saved HRS")

    @Test
    fun disabledProviderOutputsExplicitDisabledFact() = runBlocking {
        val state = DisabledHeartRateProvider().heartRateState.first()

        assertEquals(HeartRateFact.DISABLED, state.fact)
        assertEquals(HeartRateSourceKind.NONE, state.sourceKind)
        assertTrue(state.isValidE17State())
    }

    @Test
    fun everyRuntimeFactMapsToUniquePublicFact() {
        val facts = listOf(
            HeartRateRuntimeFact.Disabled,
            HeartRateRuntimeFact.PermissionRequired(source),
            HeartRateRuntimeFact.BluetoothOff(source),
            HeartRateRuntimeFact.NotConnected(source),
            HeartRateRuntimeFact.Scanning(source),
            HeartRateRuntimeFact.Connecting(source),
            HeartRateRuntimeFact.WaitingFirstData(source),
            HeartRateRuntimeFact.Live(88, "2026-07-19T13:16:04Z", source),
            HeartRateRuntimeFact.DataInterrupted(source),
            HeartRateRuntimeFact.LinkDisconnected(source),
            HeartRateRuntimeFact.TechnicalFailure(
                HeartRateTechnicalFailure.CONNECT_FAILED,
                source
            ),
            HeartRateRuntimeFact.IntentionalStop(source)
        )

        val states = facts.map { it.toHeartRateState() }

        assertEquals(HeartRateFact.entries.toSet(), states.mapNotNull { it.fact }.toSet())
        assertTrue(states.all(HeartRateState::isValidE17State))
    }

    @Test
    fun onlyLiveCarriesCurrentBpmAndMeasuredAt() {
        val live = HeartRateRuntimeFact.Live(
            bpm = 88,
            measuredAt = "2026-07-19T13:16:04Z",
            source = source
        ).toHeartRateState()
        val nonLive = listOf(
            HeartRateRuntimeFact.WaitingFirstData(source),
            HeartRateRuntimeFact.DataInterrupted(source),
            HeartRateRuntimeFact.LinkDisconnected(source),
            HeartRateRuntimeFact.TechnicalFailure(
                HeartRateTechnicalFailure.PLATFORM_FAILURE,
                source
            ),
            HeartRateRuntimeFact.IntentionalStop(source),
            HeartRateRuntimeFact.NotConnected(source),
            HeartRateRuntimeFact.PermissionRequired(source),
            HeartRateRuntimeFact.BluetoothOff(source)
        ).map { it.toHeartRateState() }

        assertEquals(88, live.bpm)
        assertEquals("2026-07-19T13:16:04Z", live.measuredAt)
        nonLive.forEach { state ->
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertTrue(state.isValidE17State())
        }
    }

    @Test
    fun invalidLiveFactFailsClosedWithoutThrowingOrLeakingReading() {
        val invalidBpm = HeartRateRuntimeFact.Live(
            bpm = 0,
            measuredAt = "2026-07-19T13:16:04Z",
            source = source
        ).toHeartRateState()
        val missingTime = HeartRateRuntimeFact.Live(
            bpm = 88,
            measuredAt = "",
            source = source
        ).toHeartRateState()

        listOf(invalidBpm, missingTime).forEach { state ->
            assertEquals(HeartRateFact.TECHNICAL_FAILURE, state.fact)
            assertEquals(HeartRateTechnicalFailure.INVALID_FACT, state.technicalFailure)
            assertNull(state.bpm)
            assertNull(state.measuredAt)
            assertTrue(state.isValidE17State())
        }
    }

    @Test
    fun illegalPublicFieldCombinationsAreDetectable() {
        val staleWithOldReading = HeartRateState(
            kind = HeartRateStateKind.STALE_READING,
            sourceKind = HeartRateSourceKind.DEVICE,
            fact = HeartRateFact.DATA_INTERRUPTED,
            bpm = 99,
            measuredAt = "old"
        )
        val liveManual = HeartRateState(
            kind = HeartRateStateKind.MANUAL_READING,
            sourceKind = HeartRateSourceKind.MANUAL,
            fact = HeartRateFact.LIVE,
            bpm = 99,
            measuredAt = "now"
        )
        val failureWithoutReason = HeartRateState(
            kind = HeartRateStateKind.PROVIDER_UNAVAILABLE,
            sourceKind = HeartRateSourceKind.DEVICE,
            fact = HeartRateFact.TECHNICAL_FAILURE
        )

        assertTrue(
            HeartRateStateContractViolation.NON_LIVE_CARRIES_CURRENT_READING in
                staleWithOldReading.e17ContractViolations()
        )
        assertTrue(
            HeartRateStateContractViolation.LIVE_REQUIRES_DEVICE_SOURCE in
                liveManual.e17ContractViolations()
        )
        assertTrue(
            HeartRateStateContractViolation.TECHNICAL_FAILURE_REQUIRES_REASON in
                failureWithoutReason.e17ContractViolations()
        )
    }

    @Test
    fun savedIdentifierIsOnlyAHintAndNeverConnectedFact() {
        val state = HeartRateRuntimeFact.NotConnected(source).toHeartRateState()

        assertEquals("saved-id", state.sourceId)
        assertEquals("Saved HRS", state.sourceLabel)
        assertEquals(HeartRateFact.NOT_CONNECTED, state.fact)
        assertFalse(state.fact == HeartRateFact.LIVE)
    }

    @Test
    fun legacyManualReadingDoesNotBecomeE17LiveFact() = runBlocking {
        val provider = MockHeartRateProvider()
        val legacy = HeartRateState(
            kind = HeartRateStateKind.MANUAL_READING,
            sourceKind = HeartRateSourceKind.MANUAL,
            bpm = 126,
            recordedAt = "2026-06-03T14:42:00Z"
        )

        provider.update(legacy)

        assertEquals(legacy, provider.heartRateState.first())
        assertNull(legacy.fact)
        assertTrue(
            HeartRateStateContractViolation.MISSING_E17_FACT in legacy.e17ContractViolations()
        )
    }
}
