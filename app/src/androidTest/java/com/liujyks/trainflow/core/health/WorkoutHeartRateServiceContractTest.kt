package com.liujyks.trainflow.core.health

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.os.Process
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.ProcessVisibilityFact
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.notifications.ActiveWorkoutForegroundPhase
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutHeartRateServiceContractTest {
    @Test
    fun handoffUsesOnePlatformNotificationAcrossPromotionReleaseAndReentry() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val controller = application.activeWorkoutNotifications
        when (arguments.getString("phase")) {
            "handoff" -> {
                val original = application.preferencesDataSource.preferences.first()
                println("E20_S02_AVD_ORIGINAL enabled=${original.heartRateDisplayEnabled} " +
                    "target=${original.bleHeartRateDeviceIdentifier} " +
                    "name=${original.bleHeartRateDeviceDisplayName} " +
                    "suppressed=${original.heartRateManualSuppressed}")
                ActivityScenario.launch(MainActivity::class.java).use {
                    withTimeout(5_000) {
                        application.processVisibility.first { fact -> fact == ProcessVisibilityFact.VISIBLE }
                    }
                    application.setHeartRateEnabled(true)
                    application.selectHeartRateDevice("AA:BB:CC:DD:EE:71", "AVD test target")
                    val producer = controller.beginSession("avd-handoff")
                    controller.update(producer, 1, state(producer.sessionId, "开始"))
                    withTimeout(5_000) { controller.foregroundState.first { state -> state.active } }
                    val first = requireNotNull(controller.foregroundState.value.generation)
                    assertPlatformForeground(application)
                    println("E20_S02_AVD firstGeneration=$first pid=${Process.myPid()}")

                    InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                        application.packageName, Manifest.permission.POST_NOTIFICATIONS
                    )
                    controller.refreshPermission()
                    controller.update(producer, 2, state(producer.sessionId, "最新状态"))
                    application.disconnectHeartRateDevice()
                    withTimeout(5_000) {
                        controller.foregroundState.first { state ->
                            state.phase == ActiveWorkoutForegroundPhase.NONE
                        }
                    }
                    assertEquals("最新状态 · 00:03", activeNotificationText(application))

                    application.reconnectHeartRateDevice()
                    withTimeout(5_000) { controller.foregroundState.first { state -> state.active } }
                    val second = requireNotNull(controller.foregroundState.value.generation)
                    assertNotEquals(first, second)
                    assertPlatformForeground(application)
                    controller.update(producer, 3, state(producer.sessionId, "结束", SessionStatus.COMPLETED))
                    withTimeout(5_000) {
                        controller.foregroundState.first { state ->
                            state.phase == ActiveWorkoutForegroundPhase.NONE
                        }
                    }
                    assertFalse(activeWorkoutNotifications(application).any { it.id == 7_200 })

                    val finalProducer = controller.beginSession("avd-left-running")
                    controller.update(finalProducer, 1, state(finalProducer.sessionId, "保持"))
                    withTimeout(5_000) { controller.foregroundState.first { state -> state.active } }
                    assertPlatformForeground(application)
                    println("E20_S02_AVD_LEAVE_ACTIVE pid=${Process.myPid()} " +
                        "owner=${System.identityHashCode(application.heartRateRuntimeOwner)} " +
                        "generation=${controller.foregroundState.value.generation}")
                }
            }
            "after_restart" -> {
                val oldPid = arguments.getString("oldPid")?.toIntOrNull()
                    ?: error("oldPid argument is required")
                assertNotEquals(oldPid, Process.myPid())
                assertEquals(null, controller.currentTraining.value)
                assertEquals(ActiveWorkoutForegroundPhase.NONE, controller.foregroundState.value.phase)
                assertFalse(runningWorkoutService(application))
                assertFalse(activeWorkoutNotifications(application).any { it.id == 7_200 })
                val original = JSONObject(requireNotNull(arguments.getString("originalPreferences")))
                val source = application.preferencesDataSource
                if (original.isNull("target")) {
                    source.clearBleHeartRateDevicePreference()
                } else {
                    source.setBleHeartRateDevicePreference(
                        original.getString("target"), original.optString("name", "")
                    )
                }
                source.setHeartRateManualSuppressed(original.getBoolean("suppressed"))
                source.setHeartRateDisplayEnabled(original.getBoolean("enabled"))
                println("E20_S02_AVD_AFTER_RESTART oldPid=$oldPid pid=${Process.myPid()} " +
                    "owner=${System.identityHashCode(application.heartRateRuntimeOwner)} " +
                    "restored=true")
            }
            else -> error("phase must be handoff or after_restart")
        }
    }

    @Test
    fun readSelectedSavedWorkoutHeartRateEvidence() = runBlocking {
        val rawSelection = requireNotNull(InstrumentationRegistry.getArguments().getString("selection"))
        val selections = JSONArray(rawSelection)
        require(selections.length() == 1 || selections.length() == 3)
        val application = ApplicationProvider.getApplicationContext<TrainFlowApplication>()
        val database = application.trainFlowDatabase
        val dao = database.canonicalTimelineHeartRateDao()
        val seen = mutableSetOf<String>()
        for (index in 0 until selections.length()) {
            val selection = selections.getJSONObject(index)
            val mode = selection.getString("mode")
            require(mode in setOf("timed", "strength", "follow_along"))
            val from = selection.getLong("startUtcSecondFrom")
            val to = selection.getLong("startUtcSecondTo")
            val anchor = selection.getLong("bindingAnchorElapsedMs")
            require(from <= to && anchor >= 0L)
            require(seen.add("$mode:$from:$to"))
            val sessionIds = database.openHelper.readableDatabase.query(
                "SELECT id FROM workout_sessions WHERE mode = ? AND " +
                    "CAST(strftime('%s', started_at) AS INTEGER) BETWEEN ? AND ?",
                arrayOf<Any>(mode, from, to)
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            check(sessionIds.size == 1) {
                "Selection $index matched ${sessionIds.size} sessions; no latest fallback"
            }
            val sessionId = sessionIds.single()
            val session = requireNotNull(dao.sessionById(sessionId))
            check(session.status == "completed" || session.status == "abandoned")
            val recordings = dao.recordingsForSession(sessionId)
            check(recordings.size <= 1)
            val recording = recordings.singleOrNull()
            val samples = recording?.let { dao.samplesInCanonicalOrder(it.recordingId) }.orEmpty()
            val acquisitions = recording?.let { dao.acquisitionsInSequence(it.recordingId) }.orEmpty()
            var line = 0
            Log.i(READBACK_TAG, "line=${line++} BEGIN mode=$mode session=$sessionId " +
                "recording=${recording?.recordingId ?: "NONE"} anchor=$anchor " +
                "from=$from to=$to status=${session.status}")
            samples.forEach { sample ->
                Log.i(READBACK_TAG, "line=${line++} SAMPLE session=$sessionId " +
                    "sequence=${sample.sampleSequence} offsetMs=${sample.offsetMs} " +
                    "mutationSequence=${sample.mutationSequence} bpm=${sample.bpm}")
            }
            acquisitions.forEach { acquisition ->
                Log.i(READBACK_TAG, "line=${line++} ACQUISITION session=$sessionId " +
                    "sequence=${acquisition.sequence} startOffsetMs=${acquisition.startOffsetMs} " +
                    "endOffsetMs=${acquisition.endOffsetMs} deviceState=${acquisition.deviceState} " +
                    "deviceReason=${acquisition.deviceReason} " +
                    "recordingIntent=${acquisition.recordingIntent} intentReason=${acquisition.intentReason}")
            }
            Log.i(READBACK_TAG, "line=$line END session=$sessionId " +
                "sampleCount=${samples.size} acquisitionCount=${acquisitions.size}")
        }
    }

    private fun state(
        sessionId: String,
        primaryText: String,
        status: SessionStatus = SessionStatus.ACTIVE
    ) = ActiveWorkoutNotificationState(
        sessionId = sessionId,
        mode = WorkoutMode.FOLLOW_ALONG,
        planTitle = "AVD test training",
        status = status,
        phaseLabel = "训练",
        primaryText = primaryText,
        timerText = "00:03",
        progressText = "1 / 2",
        secondaryText = "继续"
    )

    private fun assertPlatformForeground(application: TrainFlowApplication) {
        val info = application.getSystemService(ActivityManager::class.java)
            .getRunningServices(Int.MAX_VALUE)
            .single { it.service.className == WorkoutHeartRateService::class.java.name }
        assertTrue(info.foreground)
    }

    private fun runningWorkoutService(application: TrainFlowApplication): Boolean =
        application.getSystemService(ActivityManager::class.java)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == WorkoutHeartRateService::class.java.name }

    private fun activeWorkoutNotifications(application: TrainFlowApplication) =
        application.getSystemService(NotificationManager::class.java).activeNotifications

    private fun activeNotificationText(application: TrainFlowApplication): String? =
        activeWorkoutNotifications(application).singleOrNull { it.id == 7_200 }
            ?.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()

    private companion object {
        const val READBACK_TAG = "TrainFlowHrReadback"
    }
}
