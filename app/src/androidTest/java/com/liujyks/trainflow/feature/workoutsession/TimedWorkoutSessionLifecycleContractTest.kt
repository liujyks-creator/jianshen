package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liujyks.trainflow.app.MainActivity
import com.liujyks.trainflow.app.TrainFlowApplication
import com.liujyks.trainflow.core.data.WorkoutPlanRepository
import com.liujyks.trainflow.core.data.WorkoutSessionStrictReadResult
import com.liujyks.trainflow.core.datastore.TrainFlowPreferenceKeys
import com.liujyks.trainflow.core.datastore.trainFlowPreferencesDataStore
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimedWorkoutSessionLifecycleContractTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun normalTimedStartPauseEndSavedReturnsHome() = runBlocking {
        val application = compose.activity.application as TrainFlowApplication
        val database = application.trainFlowDatabase
        val plans = WorkoutPlanRepository(database)
        val sessions = application.workoutSessionRepository
        val sessionDao = database.workoutSessionDao()
        val dataStore = application.trainFlowPreferencesDataStore
        val originalPlanIds = plans.getPlans().map { it.id }.toSet()
        val originalPreferences = dataStore.data.first()
        val originalHeartRate = originalPreferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled]
        val originalPlaceholder = originalPreferences[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder]
        val heartRateToggle = isToggleable() and hasAnySibling(hasText("启用心率功能"))
        var testPlanId: String? = null
        var failure: Throwable? = null
        try {
            compose.waitUntil { compose.onAllNodesWithText("训练偏好").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("训练偏好").performScrollTo().performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
            compose.waitUntil { compose.onAllNodes(heartRateToggle).fetchSemanticsNodes().size == 1 }
            if (originalHeartRate == true) compose.onNode(heartRateToggle).performScrollTo().performClick()
            dataStore.data.first { it[TrainFlowPreferenceKeys.heartRateDisplayEnabled] != true }
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            compose.onNodeWithText("返回训练首页").performScrollTo().performClick()
            compose.waitUntil { compose.onAllNodesWithText("编辑计时计划").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("编辑计时计划").performScrollTo().performClick()
            compose.waitUntil { compose.onAllNodesWithText("保存计划").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("保存计划").performClick()
            val createdPlans = plans.plans.first { saved -> saved.any { it.id !in originalPlanIds } }
                .filter { it.id !in originalPlanIds }
            testPlanId = createdPlans.single().id
            val planBeforeStart = requireNotNull(plans.getPlan(requireNotNull(testPlanId)))
            val composition = planBeforeStart.blocks.filterIsInstance<TimedCompositionBlock>().single()
            compose.waitUntil { compose.onAllNodesWithText("开始计时训练").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("开始计时训练").performScrollTo().performClick()
            compose.waitUntil {
                compose.onAllNodesWithContentDescription("开始计时训练").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithContentDescription("开始计时训练").performClick()
            compose.waitUntil { compose.onAllNodesWithContentDescription("暂停训练").fetchSemanticsNodes().size == 1 }
            val started = sessionDao.observeSessionsWithRecords().first { rows ->
                rows.any { it.session.planId == testPlanId }
            }.filter { it.session.planId == testPlanId }.single().session
            val sessionId = started.id
            println("E18-S07B testPlanId=$testPlanId sessionId=$sessionId")
            compose.onNodeWithContentDescription("暂停训练").performClick()
            compose.waitUntil {
                compose.onAllNodesWithContentDescription("结束此次计时训练").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithContentDescription("结束此次计时训练").performClick()
            compose.onNodeWithText("确认结束").performClick()
            val terminalRows = sessionDao.observeSessionsWithRecords().first { rows ->
                rows.any { it.session.id == sessionId && it.session.status == "abandoned" &&
                    it.session.terminalReason == "user_abandoned" && it.session.trustedEndOffsetMs != null }
            }.filter { it.session.planId == testPlanId }
            assertEquals(1, terminalRows.size)
            assertEquals(sessionId, terminalRows.single().session.id)
            val beforeReturn = sessions.readSessionStrict(sessionId)
            assertTrue(beforeReturn is WorkoutSessionStrictReadResult.CanonicalTerminal)
            val terminal = beforeReturn as WorkoutSessionStrictReadResult.CanonicalTerminal
            assertEquals("abandoned", terminal.graph.session.status)
            assertEquals("user_abandoned", terminal.graph.session.terminalReason)
            val storedSnapshot = sessions.sessions.first { rows -> rows.any { it.id == sessionId } }
                .single { it.id == sessionId }.planSnapshot
            assertEquals(planBeforeStart.id, storedSnapshot.planId)
            assertEquals(planBeforeStart.title, storedSnapshot.title)
            assertEquals(planBeforeStart.mode, storedSnapshot.mode)
            assertEquals(planBeforeStart.blocks, storedSnapshot.blocks)
            assertEquals(planBeforeStart.preferences, storedSnapshot.preferences)
            assertEquals(planBeforeStart.followAlong, storedSnapshot.followAlong)
            val firstPhase = terminal.graph.phases.first()
            assertEquals("timed_work", firstPhase.phaseKind)
            val firstPayload = JSONObject(firstPhase.phaseIdentityJson).getJSONObject("payload")
            assertEquals("warmup", firstPayload.getString("variant"))
            assertEquals(2, firstPayload.getInt("compositionVersion"))
            assertEquals(composition.id, firstPayload.getString("compositionBlockId"))
            assertTrue(terminal.graph.phases.any { it.phaseKind == "paused" })
            compose.waitUntil {
                compose.onAllNodes(hasText("返回训练首页") and isEnabled()).fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithText("返回训练首页").assertIsEnabled().performClick()
            compose.waitUntil { compose.onAllNodesWithText("编辑计时计划").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("编辑计时计划").assertIsDisplayed()
            val afterRows = sessionDao.getSessionsWithRecords().filter { it.session.planId == testPlanId }
            assertEquals(1, afterRows.size)
            assertEquals(sessionId, afterRows.single().session.id)
            assertEquals(beforeReturn, sessions.readSessionStrict(sessionId))
            println("E18-S07B terminal and return assertions passed")
        } catch (cause: Throwable) {
            failure = cause
        } finally {
            try {
                if (testPlanId != null) {
                    sessions.deleteSessionsForPlan(requireNotNull(testPlanId))
                    plans.deletePlan(requireNotNull(testPlanId))
                    println("E18-S07B deleted testPlanId=$testPlanId and its sessions")
                } else {
                    println("E18-S07B testPlanId not uniquely identified; no data deletion attempted")
                }
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
            try {
                compose.onNodeWithText("训练偏好").performScrollTo().performClick()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
                compose.waitUntil { compose.onAllNodes(heartRateToggle).fetchSemanticsNodes().size == 1 }
                val currentEnabled = dataStore.data.first()[TrainFlowPreferenceKeys.heartRateDisplayEnabled] == true
                if (currentEnabled != (originalHeartRate == true)) {
                    compose.onNode(heartRateToggle).performScrollTo().performClick()
                }
                dataStore.data.first { (it[TrainFlowPreferenceKeys.heartRateDisplayEnabled] == true) ==
                    (originalHeartRate == true) }
                println("E18-S07B original effective heart-rate setting restored through UI")
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
            try {
                dataStore.edit { preferences ->
                    if (originalHeartRate == null) preferences.remove(TrainFlowPreferenceKeys.heartRateDisplayEnabled)
                    else preferences[TrainFlowPreferenceKeys.heartRateDisplayEnabled] = originalHeartRate
                    if (originalPlaceholder == null) preferences.remove(TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder)
                    else preferences[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder] = originalPlaceholder
                }
                dataStore.data.first { it[TrainFlowPreferenceKeys.heartRateDisplayEnabled] == originalHeartRate &&
                    it[TrainFlowPreferenceKeys.showDisconnectedHeartRatePlaceholder] == originalPlaceholder }
                println("E18-S07B both preference keys restored: heartRate=$originalHeartRate placeholder=$originalPlaceholder")
            } catch (cause: Throwable) {
                if (failure == null) failure = cause else requireNotNull(failure).addSuppressed(cause)
            }
        }
        failure?.let { throw it }
        Unit
    }
}
