package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.RecorderValidationException
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.feature.workoutsession.TimedCanonicalStepFactsV1
import com.liujyks.trainflow.feature.workoutsession.compositionTimedTransitionFactsV1
import com.liujyks.trainflow.feature.workoutsession.toTimedRestExtensionRecords
import com.liujyks.trainflow.feature.workoutsession.toTimedSessionStepRecords
import java.time.Instant

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineAdapter
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStep
import com.liujyks.trainflow.core.model.TimedCompositionTimelineStepKind
import com.liujyks.trainflow.core.model.TimedCompositionTimelineTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.feature.plans.PlanManagementScreenState
import com.liujyks.trainflow.feature.plans.buildDefaultTimedCompositionPlanEditorState
import com.liujyks.trainflow.feature.plans.selectPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedCompositionEngineBridgeTest {
    @Test
    fun v2CompositionExpandsThroughTimelineAdapterIntoEngineCompatibleTimedSteps() {
        val block = bridgedCompositionBlock()
        val expected = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .map { step -> step.toExpectedEngineStep(roundCount = block.rounds) }

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertEquals(expected, state.steps.map { step -> step.toBridgeExpectation() })
    }

    @Test
    fun engineStepIdsComeFromTimelineMetadataAndRepeatedTargetsAreDistinct() {
        val block = bridgedCompositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            restBetweenRoundsSec = 0,
            stageGroups = listOf(
                stageGroup(
                    id = "main",
                    targets = listOf(actionTarget(id = "repeated-action"))
                )
            )
        )
        val expectedRepeatedIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.targetId == "repeated-action" }
            .map { step -> step.id }

        val state = TimedWorkoutEngine.create(workoutPlan(block))
        val actualRepeatedIds = state.steps
            .filter { step -> step.itemId == "repeated-action" }
            .map { step -> step.id }

        assertEquals(2, expectedRepeatedIds.size)
        assertEquals(expectedRepeatedIds.distinct(), expectedRepeatedIds)
        assertEquals(expectedRepeatedIds, actualRepeatedIds)
    }

    @Test
    fun v2RestTargetsAndSyntheticBetweenRoundRestMapToRestExtendableStepsOnly() {
        val block = bridgedCompositionBlock()
        val timelineSteps = TimedCompositionTimelineAdapter.expand(block).steps
        val expectedRestStepIds = timelineSteps
            .filter { step -> step.isRest }
            .map { step -> step.id }
        val expectedNonRestStepIds = timelineSteps
            .filterNot { step -> step.isRest }
            .map { step -> step.id }

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertEquals(expectedRestStepIds, state.steps.filter { step -> step.kind == TimedSessionStepKind.REST }.map { it.id })
        assertEquals(expectedNonRestStepIds, state.steps.filterNot { step -> step.kind == TimedSessionStepKind.REST }.map { it.id })
    }

    @Test
    fun v2RestTargetAndSyntheticBetweenRoundRestAcceptExtendRestWhenActive() {
        val block = bridgedCompositionBlock()
        val plan = workoutPlan(block)
        val restStepIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.isRest }
            .map { step -> step.id }

        restStepIds.forEach { restStepId ->
            val activeRest = activeStateAt(plan, restStepId)
            val extended = TimedWorkoutEngine.dispatch(
                activeRest,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state

            assertEquals(activeRest.remainingSec + 15, extended.remainingSec)
            assertEquals(activeRest.extendedRestSec + 15, extended.extendedRestSec)
            assertTrue(extended.restExtensionHistory.isNotEmpty())
        }
    }

    @Test
    fun v2WorkWarmupAndCooldownStepsDoNotAcceptExtendRest() {
        val block = bridgedCompositionBlock()
        val plan = workoutPlan(block)
        val workStepIds = TimedCompositionTimelineAdapter.expand(block)
            .steps
            .filter { step -> step.isWork }
            .map { step -> step.id }

        workStepIds.forEach { workStepId ->
            val activeWork = activeStateAt(plan, workStepId)
            val afterExtendAttempt = TimedWorkoutEngine.dispatch(
                activeWork,
                WorkoutCommand.ExtendRest(seconds = 15)
            ).state

            assertEquals(activeWork.remainingSec, afterExtendAttempt.remainingSec)
            assertEquals(activeWork.extendedRestSec, afterExtendAttempt.extendedRestSec)
            assertEquals(activeWork.restExtensionHistory, afterExtendAttempt.restExtensionHistory)
        }
    }

    @Test
    fun legacyTimedPlanStillUsesExistingEnginePath() {
        val state = TimedWorkoutEngine.create(legacyTimedPlan())

        assertEquals(
            listOf(
                "legacy-warmup-work",
                "legacy-circuit-r1-main-work",
                "legacy-circuit-r1-main-rest",
                "legacy-circuit-r1-round-rest",
                "legacy-circuit-r2-main-work",
                "legacy-circuit-r2-main-rest",
                "legacy-cooldown-work"
            ),
            state.steps.map { step -> step.id }
        )
        assertEquals(
            listOf(
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.WORK,
                TimedSessionStepKind.REST,
                TimedSessionStepKind.WORK
            ),
            state.steps.map { step -> step.kind }
        )

        val started = TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state

        assertEquals(SessionStatus.ACTIVE, started.status)
        assertEquals("legacy-warmup-work", started.currentStep?.id)
    }

    @Test
    fun unsupportedCompositionVersionFailsClosedWithoutExecutableV2Steps() {
        val block = bridgedCompositionBlock(compositionVersion = TIMED_COMPOSITION_CURRENT_VERSION + 1)

        assertThrows(IllegalArgumentException::class.java) {
            TimedCompositionTimelineAdapter.expand(block)
        }

        val createResult = runCatching {
            TimedWorkoutEngine.create(workoutPlan(block))
        }

        createResult.exceptionOrNull()?.let { error ->
            assertTrue(error is IllegalArgumentException)
            return
        }

        val state = createResult.getOrThrow()
        assertTrue(state.steps.isEmpty())
        assertEquals(
            SessionStatus.COMPLETED,
            TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state.status
        )
    }

    @Test
    fun emptyV2TimelineFailsClosedWithoutExecutableSteps() {
        val block = bridgedCompositionBlock(
            warmupSec = 0,
            cooldownSec = 0,
            rounds = 1,
            restBetweenRoundsSec = 0,
            stageGroups = emptyList()
        )

        assertTrue(TimedCompositionTimelineAdapter.expand(block).steps.isEmpty())

        val state = TimedWorkoutEngine.create(workoutPlan(block))

        assertTrue(state.steps.isEmpty())
        assertEquals(
            SessionStatus.COMPLETED,
            TimedWorkoutEngine.dispatch(state, WorkoutCommand.StartSession).state.status
        )
    }

    @Test
    fun v2StartGateOpensAfterMinimumEngineBridge() {
        val v2Plan = buildDefaultTimedCompositionPlanEditorState(planId = "timed-composition-start-gate")
            .toWorkoutPlan(timestamp = "2026-06-27T01:00:00Z")
        val detail = PlanManagementScreenState(plans = listOf(v2Plan))
            .selectPlan(v2Plan.id)
            .selectedDetail

        assertTrue(v2Plan.blocks.single() is TimedCompositionBlock)
        assertTrue(requireNotNull(detail).canEditPlan)
        assertTrue(detail.canStartTraining)
        assertEquals("开始计时训练", detail.startStatus)
    }


    @Test
    fun compositionCanonicalFactsPreserveOrderedExecutionAndRestExtensions() {
        val prefix = bridgedCompositionBlock(
            warmupSec = 0, cooldownSec = 0, rounds = 1, restBetweenRoundsSec = 0,
            stageGroups = listOf(stageGroup(
                id = "group-p",
                targets = listOf(actionTarget(id = "target-p", durationSec = 1))
            ))
        ).copy(id = "composition-prefix")
        val block = bridgedCompositionBlock(
            warmupSec = 2, cooldownSec = 2, rounds = 2, restBetweenRoundsSec = 3,
            stageGroups = listOf(
                stageGroup(id = "group-a", targets = listOf(
                    actionTarget(id = "a", durationSec = 4),
                    customTarget(id = "c", order = 2, durationSec = 3),
                    restTarget(id = "r", order = 3, durationSec = 2)
                )),
                stageGroup(id = "group-b", order = 2, targets = listOf(
                    actionTarget(id = "b", durationSec = 1)
                ))
            )
        ).copy(order = 2)
        val plan = workoutPlan(prefix, block)
        val frozen = WorkoutPlanSnapshot(
            planId = plan.id, title = plan.title, mode = plan.mode, blocks = plan.blocks,
            preferences = plan.preferences, followAlong = plan.followAlong
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            frozen.toStorageJson(), frozen.mode
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val timelines = frozen.blocks.filterIsInstance<TimedCompositionBlock>()
            .map(TimedCompositionTimelineAdapter::expand)
        val startedAt = Instant.parse("2026-09-11T00:00:00Z")
        val initial = TimedWorkoutEngine.create(frozen, sessionId = "a2-ordered")
        val start = TimedWorkoutEngine.dispatch(initial, WorkoutCommand.StartSession)
        val startFacts = compositionTimedTransitionFactsV1(prepared, timelines, initial, start, startedAt)
        val firstTick = TimedWorkoutEngine.tick(start.state, 10)
        val firstTickFacts = compositionTimedTransitionFactsV1(prepared, timelines, start.state, firstTick, startedAt)
        val firstExtension = TimedWorkoutEngine.dispatch(firstTick.state, WorkoutCommand.ExtendRest(5))
        val firstExtensionFacts = compositionTimedTransitionFactsV1(
            prepared, timelines, firstTick.state, firstExtension, startedAt
        )
        val secondTick = TimedWorkoutEngine.tick(firstExtension.state, 8)
        val secondTickFacts = compositionTimedTransitionFactsV1(
            prepared, timelines, firstExtension.state, secondTick, startedAt
        )
        val secondExtension = TimedWorkoutEngine.dispatch(secondTick.state, WorkoutCommand.ExtendRest(4))
        val secondExtensionFacts = compositionTimedTransitionFactsV1(
            prepared, timelines, secondTick.state, secondExtension, startedAt
        )
        val finish = TimedWorkoutEngine.tick(secondExtension.state, 19)
        val finishFacts = compositionTimedTransitionFactsV1(
            prepared, timelines, secondExtension.state, finish, startedAt
        )
        val cuts = listOf(startFacts, firstTickFacts, firstExtensionFacts, secondTickFacts, secondExtensionFacts, finishFacts)
        assertEquals(listOf(1, 4, 0, 2, 0, 5), cuts.map { it.phaseStarts.size })
        assertEquals(listOf(0, 4, 0, 2, 0, 6), cuts.map { it.completedSteps.size })
        assertEquals(listOf(0, 0, 1, 0, 1, 0), cuts.map { it.restExtensions.size })
        assertEquals(listOf(null, null, null, null, null, SessionStatus.COMPLETED), cuts.map { it.terminalStatus })
        assertEquals(37, finish.state.activeElapsedSec)
        assertEquals(null, finish.state.currentStep)

        val expectedStepIds = listOf(
            "composition-prefix:r1:g1:group-p:t1:target-p",
            "composition-bridge:warmup:t1",
            "composition-bridge:r1:g1:group-a:t1:a",
            "composition-bridge:r1:g1:group-a:t2:c",
            "composition-bridge:r1:g1:group-a:t3:r",
            "composition-bridge:r1:g2:group-b:t1:b",
            "composition-bridge:r1:between-round-rest:t1",
            "composition-bridge:r2:g1:group-a:t1:a",
            "composition-bridge:r2:g1:group-a:t2:c",
            "composition-bridge:r2:g1:group-a:t3:r",
            "composition-bridge:r2:g2:group-b:t1:b",
            "composition-bridge:cooldown:t1"
        )
        val expectedKinds = listOf(
            "timed_work", "timed_work", "timed_work", "timed_work", "timed_rest", "timed_work",
            "timed_rest", "timed_work", "timed_work", "timed_rest", "timed_work", "timed_work"
        )
        val plannedMs = listOf(1000L, 2000L, 4000L, 3000L, 2000L, 1000L, 3000L, 4000L, 3000L, 2000L, 1000L, 2000L)
        // F101.11 literal matrix: block-local indexes are independent of engine indexes.
        val expectedPayloads = listOf(
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-prefix","timelineStageId":"composition-prefix:r1:g1:group-p","timelineStageKind":"stage_group","stageGroupId":"group-p","targetId":"target-p","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}""",
            """{"compositionVersion":2,"variant":"warmup","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:warmup","timelineStageKind":"warmup","stageGroupId":"composition-bridge:warmup","targetId":"composition-bridge:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}""",
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"a","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}""",
            """{"compositionVersion":2,"variant":"stage_group_custom","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"c","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}""",
            """{"compositionVersion":2,"variant":"stage_group_rest","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"r","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":1,"targetInstanceIndex0":3,"stepIndex0":3}""",
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g2:group-b","timelineStageKind":"stage_group","stageGroupId":"group-b","targetId":"b","targetKind":"action","roundIndex0":0,"stageGroupIndex0":1,"targetIndex0":0,"stageInstanceIndex0":2,"targetInstanceIndex0":4,"stepIndex0":4}""",
            """{"compositionVersion":2,"variant":"between_round_rest","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:between-round-rest","timelineStageKind":"between_round_rest","stageGroupId":"composition-bridge:r1:between-round-rest","targetId":"composition-bridge:r1:between-round-rest:target","targetKind":"between_round_rest","roundIndex0":0,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":3,"targetInstanceIndex0":5,"stepIndex0":5}""",
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r2:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"a","targetKind":"action","roundIndex0":1,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":4,"targetInstanceIndex0":6,"stepIndex0":6}""",
            """{"compositionVersion":2,"variant":"stage_group_custom","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r2:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"c","targetKind":"custom","roundIndex0":1,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":4,"targetInstanceIndex0":7,"stepIndex0":7}""",
            """{"compositionVersion":2,"variant":"stage_group_rest","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r2:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"r","targetKind":"rest","roundIndex0":1,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":4,"targetInstanceIndex0":8,"stepIndex0":8}""",
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r2:g2:group-b","timelineStageKind":"stage_group","stageGroupId":"group-b","targetId":"b","targetKind":"action","roundIndex0":1,"stageGroupIndex0":1,"targetIndex0":0,"stageInstanceIndex0":5,"targetInstanceIndex0":9,"stepIndex0":9}""",
            """{"compositionVersion":2,"variant":"cooldown","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:cooldown","timelineStageKind":"cooldown","stageGroupId":"composition-bridge:cooldown","targetId":"composition-bridge:cooldown:target","targetKind":"cooldown","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":6,"targetInstanceIndex0":10,"stepIndex0":10}"""
        )
        val digest = prepared.orderedStructureDigestHexLowercase()
        val phases = cuts.flatMap { it.phaseStarts }
        phases.forEachIndexed { index, phase ->
            val phaseKind = expectedKinds[index]
            val payload = expectedPayloads[index]
            val expectedIdentity = """
                {"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,
                 "mode":"timed","phaseKind":"$phaseKind",
                 "orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},
                 "payload":$payload}
            """.trimIndent()
            assertEquals(expectedStepIds[index], phase.sourceStep!!.id)
            assertEquals(finish.state.steps[index], phase.sourceStep)
            assertEquals(phaseKind, phase.phaseKind)
            assertEquals(plannedMs[index], phase.plannedDurationMs)
            assertEquals(parseCanonicalJson(expectedIdentity), parseCanonicalJson(phase.phaseIdentityJson))
        }
        val completed = cuts.flatMap { it.completedSteps }
        assertEquals(finish.state.toTimedSessionStepRecords(startedAt), completed.map { it.record })
        assertEquals(expectedStepIds, completed.map { it.record.stepId })
        assertEquals(listOf(1, 2, 4, 3, 7, 1, 7, 4, 3, 2, 1, 2), completed.map { it.record.actualDurationSec })
        assertEquals(List(12) { false }, completed.map { it.record.skipped })
        assertEquals(expectedKinds, completed.map { it.record.kind.contractValue })
        val startSeconds = listOf(0L, 1L, 3L, 7L, 10L, 17L, 18L, 25L, 29L, 32L, 34L, 35L)
        val endSeconds = listOf(1L, 3L, 7L, 10L, 17L, 18L, 25L, 29L, 32L, 34L, 35L, 37L)
        assertEquals(startSeconds.map { startedAt.plusSeconds(it).toString() }, completed.map { it.record.startedAt })
        assertEquals(endSeconds.map { startedAt.plusSeconds(it).toString() }, completed.map { it.record.endedAt })
        completed.forEachIndexed { index, facts ->
            assertEquals(TimedCanonicalStepFactsV1(
                expectedStepIds[index], expectedKinds[index], plannedMs[index], phases[index].phaseIdentityJson
            ), facts.stepFacts)
        }

        val extensions = cuts.flatMap { it.restExtensions }
        assertEquals(finish.state.toTimedRestExtensionRecords(), extensions.map { it.record })
        assertEquals(listOf("timed-rest-extension-1", "timed-rest-extension-2"), extensions.map { it.record.id })
        assertEquals(listOf(
            "composition-bridge:r1:g1:group-a:t3:r", "composition-bridge:r1:between-round-rest:t1"
        ), extensions.map { it.record.stepId })
        assertEquals(listOf(4, 6), extensions.map { it.record.stepIndex })
        assertEquals(listOf(1, 1), extensions.map { it.record.roundIndex })
        assertEquals(listOf("r", "composition-bridge:r1:between-round-rest:target"), extensions.map { it.record.restStageId })
        assertEquals(listOf("c", "b"), extensions.map { it.record.previousStageId })
        assertEquals(listOf(5, 4), extensions.map { it.record.addedSec })
        assertEquals(listOf(2, 3), extensions.map { it.record.plannedRestSec })
        assertEquals(listOf(0, 0), extensions.map { it.record.restElapsedBeforeExtensionSec })
        assertEquals(listOf(2, 3), extensions.map { it.record.extensionAtRemainingSec })
        assertEquals(listOf(5, 4), extensions.map { it.record.cumulativeExtraRestSec })
        assertEquals(listOf(10, 18), extensions.map { it.record.eventElapsedSec })
        assertEquals(listOf(completed[4].stepFacts, completed[6].stepFacts), extensions.map { it.restStepFacts })
    }

    @Test
    fun compositionCanonicalFactsPreservePauseResumeSkipAndEarlyEnd() {
        val block = bridgedCompositionBlock(
            warmupSec = 2, cooldownSec = 2, rounds = 1, restBetweenRoundsSec = 0,
            stageGroups = listOf(
                stageGroup(id = "group-a", targets = listOf(
                    actionTarget(id = "a", durationSec = 4),
                    customTarget(id = "c", order = 2, durationSec = 3),
                    restTarget(id = "r", order = 3, durationSec = 2)
                )),
                stageGroup(id = "group-b", order = 2, targets = listOf(
                    actionTarget(id = "b", durationSec = 1)
                ))
            )
        )
        val plan = workoutPlan(block)
        val frozen = WorkoutPlanSnapshot(
            planId = plan.id, title = plan.title, mode = plan.mode, blocks = plan.blocks,
            preferences = plan.preferences, followAlong = plan.followAlong
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            frozen.toStorageJson(), frozen.mode
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val timelines = frozen.blocks.filterIsInstance<TimedCompositionBlock>()
            .map(TimedCompositionTimelineAdapter::expand)
        val startedAt = Instant.parse("2026-09-11T00:00:00Z")
        val initial = TimedWorkoutEngine.create(frozen, sessionId = "a2-controls")
        val start = TimedWorkoutEngine.dispatch(initial, WorkoutCommand.StartSession)
        val startFacts = compositionTimedTransitionFactsV1(prepared, timelines, initial, start, startedAt)
        val warmupTick = TimedWorkoutEngine.tick(start.state, 1)
        val warmupTickFacts = compositionTimedTransitionFactsV1(prepared, timelines, start.state, warmupTick, startedAt)
        val pause = TimedWorkoutEngine.dispatch(warmupTick.state, WorkoutCommand.PauseSession)
        val pauseFacts = compositionTimedTransitionFactsV1(prepared, timelines, warmupTick.state, pause, startedAt)
        val pausedTick = TimedWorkoutEngine.tick(pause.state, 5)
        val pausedTickFacts = compositionTimedTransitionFactsV1(prepared, timelines, pause.state, pausedTick, startedAt)
        val resume = TimedWorkoutEngine.dispatch(pausedTick.state, WorkoutCommand.ResumeSession)
        val resumeFacts = compositionTimedTransitionFactsV1(prepared, timelines, pausedTick.state, resume, startedAt)
        val skipWarmup = TimedWorkoutEngine.dispatch(resume.state, WorkoutCommand.SkipStep)
        val skipWarmupFacts = compositionTimedTransitionFactsV1(prepared, timelines, resume.state, skipWarmup, startedAt)
        val actionTick = TimedWorkoutEngine.tick(skipWarmup.state, 1)
        val actionTickFacts = compositionTimedTransitionFactsV1(prepared, timelines, skipWarmup.state, actionTick, startedAt)
        val skipAction = TimedWorkoutEngine.dispatch(actionTick.state, WorkoutCommand.SkipStep)
        val skipActionFacts = compositionTimedTransitionFactsV1(prepared, timelines, actionTick.state, skipAction, startedAt)
        val skipCustom = TimedWorkoutEngine.dispatch(skipAction.state, WorkoutCommand.SkipStep)
        val skipCustomFacts = compositionTimedTransitionFactsV1(prepared, timelines, skipAction.state, skipCustom, startedAt)
        val restTick = TimedWorkoutEngine.tick(skipCustom.state, 1)
        val restTickFacts = compositionTimedTransitionFactsV1(prepared, timelines, skipCustom.state, restTick, startedAt)
        val end = TimedWorkoutEngine.dispatch(restTick.state, WorkoutCommand.EndSession(reason = "a2-test"))
        val endFacts = compositionTimedTransitionFactsV1(prepared, timelines, restTick.state, end, startedAt)
        val cuts = listOf(
            startFacts, warmupTickFacts, pauseFacts, pausedTickFacts, resumeFacts, skipWarmupFacts,
            actionTickFacts, skipActionFacts, skipCustomFacts, restTickFacts, endFacts
        )
        val warmupId = "composition-bridge:warmup:t1"
        val actionId = "composition-bridge:r1:g1:group-a:t1:a"
        val customId = "composition-bridge:r1:g1:group-a:t2:c"
        val restId = "composition-bridge:r1:g1:group-a:t3:r"
        assertEquals(listOf(
            listOf(warmupId), emptyList(), listOf(null), emptyList(), listOf(warmupId),
            listOf(actionId), emptyList(), listOf(customId), listOf(restId), emptyList(), emptyList()
        ), cuts.map { cut -> cut.phaseStarts.map { it.sourceStep?.id } })
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1), cuts.map { it.completedSteps.size })
        assertEquals(List(11) { 0 }, cuts.map { it.restExtensions.size })
        assertEquals(List(10) { null } + SessionStatus.ABANDONED, cuts.map { it.terminalStatus })
        assertEquals(3, end.state.activeElapsedSec)
        assertEquals(5, end.state.pausedElapsedSec)

        val expectedPayloads = listOf(
            """{"compositionVersion":2,"variant":"warmup","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:warmup","timelineStageKind":"warmup","stageGroupId":"composition-bridge:warmup","targetId":"composition-bridge:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}""",
            """{"variant":"paused","compositionVersion":2,"compositionBlockId":null,"timelineStageId":null,"timelineStageKind":null,"stageGroupId":null,"targetId":null,"targetKind":null,"roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":null,"stageInstanceIndex0":null,"targetInstanceIndex0":null,"stepIndex0":null}""",
            """{"compositionVersion":2,"variant":"warmup","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:warmup","timelineStageKind":"warmup","stageGroupId":"composition-bridge:warmup","targetId":"composition-bridge:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}""",
            """{"compositionVersion":2,"variant":"stage_group_action","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"a","targetKind":"action","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":0,"stageInstanceIndex0":1,"targetInstanceIndex0":1,"stepIndex0":1}""",
            """{"compositionVersion":2,"variant":"stage_group_custom","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"c","targetKind":"custom","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":1,"stageInstanceIndex0":1,"targetInstanceIndex0":2,"stepIndex0":2}""",
            """{"compositionVersion":2,"variant":"stage_group_rest","compositionBlockId":"composition-bridge","timelineStageId":"composition-bridge:r1:g1:group-a","timelineStageKind":"stage_group","stageGroupId":"group-a","targetId":"r","targetKind":"rest","roundIndex0":0,"stageGroupIndex0":0,"targetIndex0":2,"stageInstanceIndex0":1,"targetInstanceIndex0":3,"stepIndex0":3}"""
        )
        val expectedKinds = listOf("timed_work", "paused", "timed_work", "timed_work", "timed_work", "timed_rest")
        val plannedMs = listOf(2000L, null, 2000L, 4000L, 3000L, 2000L)
        val stepIndexes = listOf(0, null, 0, 1, 2, 3)
        val digest = prepared.orderedStructureDigestHexLowercase()
        val phases = cuts.flatMap { it.phaseStarts }
        phases.forEachIndexed { index, phase ->
            val phaseKind = expectedKinds[index]
            val payload = expectedPayloads[index]
            val expectedIdentity = """
                {"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,
                 "mode":"timed","phaseKind":"$phaseKind",
                 "orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},
                 "payload":$payload}
            """.trimIndent()
            assertEquals(stepIndexes[index]?.let { end.state.steps[it] }, phase.sourceStep)
            assertEquals(phaseKind, phase.phaseKind)
            assertEquals(plannedMs[index], phase.plannedDurationMs)
            assertEquals(parseCanonicalJson(expectedIdentity), parseCanonicalJson(phase.phaseIdentityJson))
        }
        assertEquals(phases[0].phaseIdentityJson, phases[2].phaseIdentityJson)
        val completed = cuts.flatMap { it.completedSteps }
        assertEquals(end.state.toTimedSessionStepRecords(startedAt), completed.map { it.record })
        assertEquals(listOf(warmupId, actionId, customId, restId), completed.map { it.record.stepId })
        assertEquals(listOf(1, 1, 0, 1), completed.map { it.record.actualDurationSec })
        assertEquals(listOf(true, true, true, false), completed.map { it.record.skipped })
        assertEquals(listOf("timed_work", "timed_work", "timed_work", "timed_rest"), completed.map { it.record.kind.contractValue })
        assertEquals(listOf(0L, 1L, 2L, 2L).map { startedAt.plusSeconds(it).toString() }, completed.map { it.record.startedAt })
        assertEquals(listOf(1L, 2L, 2L, 3L).map { startedAt.plusSeconds(it).toString() }, completed.map { it.record.endedAt })
        val completedPhaseIndexes = listOf(0, 3, 4, 5)
        completed.forEachIndexed { index, facts ->
            val phaseIndex = completedPhaseIndexes[index]
            assertEquals(TimedCanonicalStepFactsV1(
                listOf(warmupId, actionId, customId, restId)[index],
                expectedKinds[phaseIndex], plannedMs[phaseIndex]!!, phases[phaseIndex].phaseIdentityJson
            ), facts.stepFacts)
        }
    }

    @Test
    fun compositionCanonicalFactsRejectMismatchedFrozenIdentity() {
        val block = bridgedCompositionBlock(
            warmupSec = 0, cooldownSec = 0, rounds = 1, restBetweenRoundsSec = 0,
            stageGroups = listOf(stageGroup(
                id = "group-a", targets = listOf(actionTarget(id = "a", durationSec = 4))
            ))
        )
        val plan = workoutPlan(block)
        val frozen = WorkoutPlanSnapshot(
            planId = plan.id, title = plan.title, mode = plan.mode, blocks = plan.blocks,
            preferences = plan.preferences, followAlong = plan.followAlong
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            frozen.toStorageJson(), frozen.mode
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val foreignBlock = block.copy(stageGroups = listOf(stageGroup(
            id = "group-a", targets = listOf(actionTarget(id = "foreign-a", durationSec = 4))
        )))
        val foreignSnapshot = frozen.copy(blocks = listOf(foreignBlock))
        val timelines = foreignSnapshot.blocks.filterIsInstance<TimedCompositionBlock>()
            .map(TimedCompositionTimelineAdapter::expand)
        val before = TimedWorkoutEngine.create(foreignSnapshot, sessionId = "a2-mismatch")
        val result = TimedWorkoutEngine.dispatch(before, WorkoutCommand.StartSession)
        val error = assertThrows(RecorderValidationException::class.java) {
            compositionTimedTransitionFactsV1(
                prepared, timelines, before, result, Instant.parse("2026-09-11T00:00:00Z")
            )
        }
        assertEquals("invalid_phase_identity", error.code)
    }

    private fun activeStateAt(
        plan: WorkoutPlan,
        stepId: String
    ): TimedWorkoutEngineState {
        var state = TimedWorkoutEngine.dispatch(
            TimedWorkoutEngine.create(plan),
            WorkoutCommand.StartSession
        ).state

        while (state.currentStep?.id != stepId && !state.isTerminal) {
            state = TimedWorkoutEngine.tick(
                state,
                seconds = state.remainingSec.coerceAtLeast(1)
            ).state
        }

        assertEquals(stepId, state.currentStep?.id)
        return state
    }

    private fun legacyTimedPlan(): WorkoutPlan {
        return workoutPlan(
            WarmupBlock(
                id = "legacy-warmup",
                order = 1,
                title = "Legacy warmup",
                durationSec = 10
            ),
            TimedCircuitBlock(
                id = "legacy-circuit",
                order = 2,
                rounds = 2,
                items = listOf(
                    TimedExerciseItem(
                        id = "main",
                        labelOverride = "Main work",
                        stageType = TimedStageType.WORK,
                        workDurationSec = 30,
                        restAfterSec = 15
                    )
                ),
                title = "Legacy circuit",
                restBetweenRoundsSec = 20
            ),
            CooldownBlock(
                id = "legacy-cooldown",
                order = 3,
                title = "Legacy cooldown",
                durationSec = 12
            )
        )
    }

    private fun workoutPlan(vararg blocks: PlanBlock): WorkoutPlan {
        return WorkoutPlan(
            id = "timed-composition-bridge-plan",
            mode = WorkoutMode.TIMED,
            title = "Timed composition bridge plan",
            blocks = blocks.toList(),
            createdAt = "2026-06-27T01:00:00Z",
            updatedAt = "2026-06-27T01:00:00Z"
        )
    }

    private fun bridgedCompositionBlock(
        compositionVersion: Int = TIMED_COMPOSITION_CURRENT_VERSION,
        warmupSec: Int = 10,
        cooldownSec: Int = 12,
        rounds: Int = 2,
        restBetweenRoundsSec: Int = 8,
        stageGroups: List<TimedCompositionStageGroup> = listOf(
            stageGroup(
                id = "group-main",
                targets = listOf(
                    actionTarget(id = "target-action", order = 1),
                    customTarget(id = "target-custom", order = 2),
                    restTarget(id = "target-rest", order = 3)
                )
            )
        )
    ): TimedCompositionBlock {
        return TimedCompositionBlock(
            id = "composition-bridge",
            order = 1,
            title = "Composition bridge",
            compositionVersion = compositionVersion,
            warmupSec = warmupSec,
            cooldownSec = cooldownSec,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            stageGroups = stageGroups
        )
    }

    private fun stageGroup(
        id: String,
        order: Int = 1,
        targets: List<TimedCompositionTarget>
    ): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = "Main group",
            colorHex = TimedStageType.WORK.defaultColorHex,
            targets = targets
        )
    }

    private fun actionTarget(
        id: String,
        order: Int = 1,
        durationSec: Int = 40
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Jumping jacks",
            kind = TimedCompositionTargetKind.ACTION,
            durationSec = durationSec,
            colorHex = TimedStageType.WORK.defaultColorHex
        )
    }

    private fun customTarget(
        id: String,
        order: Int,
        durationSec: Int = 25
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Shadow boxing",
            kind = TimedCompositionTargetKind.CUSTOM,
            durationSec = durationSec,
            colorHex = TimedStageType.CUSTOM.defaultColorHex
        )
    }

    private fun restTarget(
        id: String,
        order: Int,
        durationSec: Int = 20
    ): TimedCompositionTarget {
        return target(
            id = id,
            order = order,
            name = "Breathe",
            kind = TimedCompositionTargetKind.REST,
            durationSec = durationSec,
            colorHex = TimedStageType.REST.defaultColorHex
        )
    }

    private fun target(
        id: String,
        order: Int,
        name: String,
        kind: TimedCompositionTargetKind,
        durationSec: Int,
        colorHex: String
    ): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = id,
            order = order,
            name = name,
            kind = kind,
            durationSec = durationSec,
            colorHex = colorHex
        )
    }

    private fun TimedCompositionTimelineStep.toExpectedEngineStep(
        roundCount: Int
    ): EngineStepExpectation {
        val isRestStep = stepKind == TimedCompositionTimelineStepKind.REST
        return EngineStepExpectation(
            id = id,
            kind = if (isRestStep) TimedSessionStepKind.REST else TimedSessionStepKind.WORK,
            sessionStepKind = if (isRestStep) SessionStepKind.TIMED_REST else SessionStepKind.TIMED_WORK,
            blockId = compositionBlockId,
            itemId = targetId,
            title = displayName,
            durationSec = plannedDurationSec,
            round = roundIndex,
            roundCount = roundIndex?.let { roundCount },
            stageType = targetKind.toExpectedStageType(),
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    private fun TimedSessionStep.toBridgeExpectation(): EngineStepExpectation {
        return EngineStepExpectation(
            id = id,
            kind = kind,
            sessionStepKind = sessionStepKind,
            blockId = blockId,
            itemId = itemId,
            title = title,
            durationSec = durationSec,
            round = round,
            roundCount = roundCount,
            stageType = stageType,
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    private fun TimedCompositionTimelineTargetKind.toExpectedStageType(): TimedStageType {
        return when (this) {
            TimedCompositionTimelineTargetKind.ACTION -> TimedStageType.WORK
            TimedCompositionTimelineTargetKind.REST -> TimedStageType.REST
            TimedCompositionTimelineTargetKind.CUSTOM -> TimedStageType.CUSTOM
            TimedCompositionTimelineTargetKind.WARMUP -> TimedStageType.WARMUP
            TimedCompositionTimelineTargetKind.COOLDOWN -> TimedStageType.COOLDOWN
            TimedCompositionTimelineTargetKind.BETWEEN_ROUND_REST -> TimedStageType.REST
        }
    }

    private data class EngineStepExpectation(
        val id: String,
        val kind: TimedSessionStepKind,
        val sessionStepKind: SessionStepKind,
        val blockId: String,
        val itemId: String?,
        val title: String,
        val durationSec: Int,
        val round: Int?,
        val roundCount: Int?,
        val stageType: TimedStageType?,
        val iconKey: String?,
        val colorHex: String?
    )
}
