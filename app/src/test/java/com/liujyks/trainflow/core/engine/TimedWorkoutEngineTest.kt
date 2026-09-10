package com.liujyks.trainflow.core.engine

import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.TimedRestExtensionRecord
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.WorkoutCommand
import com.liujyks.trainflow.core.model.WorkoutEvent
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.feature.workoutsession.legacyBoundaryBlockStepFactsV1
import com.liujyks.trainflow.feature.workoutsession.legacyBoundaryItemStepFactsV1
import com.liujyks.trainflow.feature.workoutsession.legacyCircuitStepFactsV1
import com.liujyks.trainflow.feature.workoutsession.legacyTimedTransitionFactsV1
import com.liujyks.trainflow.feature.workoutsession.TimedCanonicalPhaseFactsV1
import com.liujyks.trainflow.feature.workoutsession.TimedCanonicalStepFactsV1
import com.liujyks.trainflow.feature.workoutsession.toWorkoutSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedWorkoutEngineTest {
    @Test
    fun legacyTimedClosureFactsFollowRealEngineTransitions() {
        val workoutPlan = plan(
            blocks = listOf(
                circuit(
                    rounds = 1,
                    items = listOf(
                        item("first", "jumping-jacks", workSec = 1),
                        item("middle", "bodyweight-squat", workSec = 1),
                        item("last", "jumping-jacks", workSec = 2)
                    )
                ),
                RestBlock(id = "standalone", order = 2, durationSec = 3)
            )
        )
        val snapshot = workoutPlan.toSnapshot()
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            snapshot.toStorageJson(), WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val startedAt = Instant.parse("2026-09-11T00:00:00Z")

        val beforeStart = TimedWorkoutEngine.create(snapshot, sessionId = "session-legacy-final")
        val start = TimedWorkoutEngine.dispatch(beforeStart, WorkoutCommand.StartSession)
        val startFacts = legacyTimedTransitionFactsV1(prepared, beforeStart, start, startedAt)

        val beforeAdvance = start.state
        val advance = TimedWorkoutEngine.tick(beforeAdvance, seconds = 2)
        val advanceFacts = legacyTimedTransitionFactsV1(prepared, beforeAdvance, advance, startedAt)

        val beforePause = advance.state
        val pause = TimedWorkoutEngine.dispatch(beforePause, WorkoutCommand.PauseSession)
        val pauseFacts = legacyTimedTransitionFactsV1(prepared, beforePause, pause, startedAt)

        val beforePausedTick = pause.state
        val pausedTick = TimedWorkoutEngine.tick(beforePausedTick, seconds = 1)
        val pausedTickFacts = legacyTimedTransitionFactsV1(prepared, beforePausedTick, pausedTick, startedAt)

        val beforeResume = pausedTick.state
        val resume = TimedWorkoutEngine.dispatch(beforeResume, WorkoutCommand.ResumeSession)
        val resumeFacts = legacyTimedTransitionFactsV1(prepared, beforeResume, resume, startedAt)

        val beforeSkip = resume.state
        val skip = TimedWorkoutEngine.dispatch(beforeSkip, WorkoutCommand.SkipStep)
        val skipFacts = legacyTimedTransitionFactsV1(prepared, beforeSkip, skip, startedAt)

        val beforeExtension = skip.state
        val extension = TimedWorkoutEngine.dispatch(beforeExtension, WorkoutCommand.ExtendRest(seconds = 15))
        val extensionFacts = legacyTimedTransitionFactsV1(prepared, beforeExtension, extension, startedAt)

        val beforeRestTick = extension.state
        val restTick = TimedWorkoutEngine.tick(beforeRestTick, seconds = 1)
        val restTickFacts = legacyTimedTransitionFactsV1(prepared, beforeRestTick, restTick, startedAt)

        val beforeEnd = restTick.state
        val end = TimedWorkoutEngine.dispatch(beforeEnd, WorkoutCommand.EndSession(reason = "user_requested"))
        val endFacts = legacyTimedTransitionFactsV1(prepared, beforeEnd, end, startedAt)

        val transitions = listOf(
            startFacts, advanceFacts, pauseFacts, pausedTickFacts, resumeFacts,
            skipFacts, extensionFacts, restTickFacts, endFacts
        )
        val digest = prepared.orderedStructureDigestHexLowercase()
        val expectedIdentities = listOf(
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"circuit_item_work","blockId":"circuit","stepIndex0":0,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"first","exerciseId":"jumping-jacks","roundIndex0":0}}""",
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"circuit_item_work","blockId":"circuit","stepIndex0":1,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"middle","exerciseId":"bodyweight-squat","roundIndex0":0}}""",
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"circuit_item_work","blockId":"circuit","stepIndex0":2,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"last","exerciseId":"jumping-jacks","roundIndex0":0}}""",
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"paused","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"paused","blockId":null,"stepIndex0":null,"legacyBlockKind":null,"legacyStageType":null,"itemId":null,"exerciseId":null,"roundIndex0":null}}""",
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"circuit_item_work","blockId":"circuit","stepIndex0":2,"legacyBlockKind":"timed_circuit","legacyStageType":"work","itemId":"last","exerciseId":"jumping-jacks","roundIndex0":0}}""",
            """{"phaseIdentityContractVersion":1,"family":"legacy_timed_v1","payloadVersion":1,"mode":"timed","phaseKind":"timed_rest","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"$digest"},"payload":{"variant":"standalone_rest","blockId":"standalone","stepIndex0":0,"legacyBlockKind":"rest","legacyStageType":"rest","itemId":null,"exerciseId":null,"roundIndex0":null}}"""
        )
        val phases = transitions.flatMap { it.phaseStarts }
        assertEquals(
            listOf("circuit-r1-first-work", "circuit-r1-middle-work", "circuit-r1-last-work",
                null, "circuit-r1-last-work", "standalone-rest"),
            phases.map { it.sourceStep?.id }
        )
        assertEquals(
            listOf(
                TimedCanonicalPhaseFactsV1(beforeStart.steps[0], "timed_work", 1000L, expectedIdentities[0]),
                TimedCanonicalPhaseFactsV1(beforeStart.steps[1], "timed_work", 1000L, expectedIdentities[1]),
                TimedCanonicalPhaseFactsV1(beforeStart.steps[2], "timed_work", 2000L, expectedIdentities[2]),
                TimedCanonicalPhaseFactsV1(null, "paused", null, expectedIdentities[3]),
                TimedCanonicalPhaseFactsV1(beforeStart.steps[2], "timed_work", 2000L, expectedIdentities[4]),
                TimedCanonicalPhaseFactsV1(beforeStart.steps[3], "timed_rest", 3000L, expectedIdentities[5])
            ),
            phases
        )
        assertEquals(listOf(1, 2, 1, 0, 1, 1, 0, 0, 0), transitions.map { it.phaseStarts.size })

        val expectedRecords = listOf(
            SessionStepRecord(
                stepId = "circuit-r1-first-work", kind = SessionStepKind.TIMED_WORK,
                startedAt = "2026-09-11T00:00:00Z", endedAt = "2026-09-11T00:00:01Z",
                skipped = false, actualDurationSec = 1
            ),
            SessionStepRecord(
                stepId = "circuit-r1-middle-work", kind = SessionStepKind.TIMED_WORK,
                startedAt = "2026-09-11T00:00:01Z", endedAt = "2026-09-11T00:00:02Z",
                skipped = false, actualDurationSec = 1
            ),
            SessionStepRecord(
                stepId = "circuit-r1-last-work", kind = SessionStepKind.TIMED_WORK,
                startedAt = "2026-09-11T00:00:02Z", endedAt = "2026-09-11T00:00:02Z",
                skipped = true, actualDurationSec = 0
            ),
            SessionStepRecord(
                stepId = "standalone-rest", kind = SessionStepKind.TIMED_REST,
                startedAt = "2026-09-11T00:00:02Z", endedAt = "2026-09-11T00:00:03Z",
                skipped = false, actualDurationSec = 1
            )
        )
        val completedSteps = transitions.flatMap { it.completedSteps }
        assertEquals(expectedRecords, completedSteps.map { it.record })
        assertEquals(
            listOf(
                TimedCanonicalStepFactsV1("circuit-r1-first-work", "timed_work", 1000L, expectedIdentities[0]),
                TimedCanonicalStepFactsV1("circuit-r1-middle-work", "timed_work", 1000L, expectedIdentities[1]),
                TimedCanonicalStepFactsV1("circuit-r1-last-work", "timed_work", 2000L, expectedIdentities[2]),
                TimedCanonicalStepFactsV1("standalone-rest", "timed_rest", 3000L, expectedIdentities[5])
            ),
            completedSteps.map { it.stepFacts }
        )
        val expectedExtension = TimedRestExtensionRecord(
            id = "timed-rest-extension-1",
            stepId = "standalone-rest",
            stepIndex = 3,
            roundIndex = null,
            restStageId = "standalone",
            restStageTitle = "Rest",
            previousStageId = "last",
            previousStageTitle = "jumping-jacks",
            addedSec = 15,
            plannedRestSec = 3,
            restElapsedBeforeExtensionSec = 0,
            extensionAtRemainingSec = 3,
            cumulativeExtraRestSec = 15,
            eventElapsedSec = 2
        )
        val extensions = transitions.flatMap { it.restExtensions }
        assertEquals(listOf(expectedExtension), extensions.map { it.record })
        assertEquals(
            listOf(TimedCanonicalStepFactsV1("standalone-rest", "timed_rest", 3000L, expectedIdentities[5])),
            extensions.map { it.restStepFacts }
        )
        assertEquals(
            listOf(null, null, null, null, null, null, null, null, SessionStatus.ABANDONED),
            transitions.map { it.terminalStatus }
        )
        val legacyRecord = end.state.toWorkoutSessionRecord(
            workoutPlan, startedAt, Instant.parse("2026-09-11T00:00:04Z")
        )
        assertEquals(legacyRecord.stepHistory, completedSteps.map { it.record })
        assertEquals(legacyRecord.timedRestExtensionRecords, extensions.map { it.record })
    }

    @Test
    fun boundaryItemsProduceCanonicalStepFacts() {
        val snapshot = plan(
            blocks = listOf(
                WarmupBlock(
                    id = "warm-items",
                    order = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "warm-action",
                            exerciseId = "jumping-jacks",
                            stageType = TimedStageType.WARMUP,
                            workDurationSec = 3,
                            restAfterSec = 2
                        ),
                        TimedExerciseItem(
                            id = "warm-rest",
                            stageType = TimedStageType.REST,
                            workDurationSec = 2
                        )
                    )
                ),
                StretchBlock(
                    id = "stretch-items",
                    order = 2,
                    items = listOf(
                        TimedExerciseItem(
                            id = "stretch-action",
                            stageType = TimedStageType.CUSTOM,
                            workDurationSec = 4
                        )
                    )
                ),
                CooldownBlock(
                    id = "cool-items",
                    order = 3,
                    items = listOf(
                        TimedExerciseItem(
                            id = "cool-action",
                            stageType = TimedStageType.COOLDOWN,
                            workDurationSec = 3
                        )
                    )
                )
            )
        ).toSnapshot()
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            snapshot.toStorageJson(), WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val steps = TimedWorkoutEngine.create(snapshot, sessionId = "session-boundary-items").steps

        val warmWork = legacyBoundaryItemStepFactsV1(prepared, steps[0], 0)
        assertEquals("warm-items-warm-action-work", warmWork.sourceStepId)
        assertEquals("timed_work", warmWork.phaseKind)
        assertEquals(3000L, warmWork.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_item_work"),
                    "blockId" to CanonicalJsonValue.Str("warm-items"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("warmup"),
                    "legacyStageType" to CanonicalJsonValue.Str("warmup"),
                    "itemId" to CanonicalJsonValue.Str("warm-action"),
                    "exerciseId" to CanonicalJsonValue.Str("jumping-jacks"),
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(warmWork.phaseIdentityJson)
        )

        val warmRestAfter = legacyBoundaryItemStepFactsV1(prepared, steps[1], 1)
        assertEquals("warm-items-warm-action-rest", warmRestAfter.sourceStepId)
        assertEquals("timed_rest", warmRestAfter.phaseKind)
        assertEquals(2000L, warmRestAfter.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_rest"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_rest_after_item"),
                    "blockId" to CanonicalJsonValue.Str("warm-items"),
                    "stepIndex0" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("warmup"),
                    "legacyStageType" to CanonicalJsonValue.Str("rest"),
                    "itemId" to CanonicalJsonValue.Str("warm-action"),
                    "exerciseId" to CanonicalJsonValue.Str("jumping-jacks"),
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(warmRestAfter.phaseIdentityJson)
        )

        val warmRest = legacyBoundaryItemStepFactsV1(prepared, steps[2], 2)
        assertEquals("warm-items-warm-rest-rest", warmRest.sourceStepId)
        assertEquals("timed_rest", warmRest.phaseKind)
        assertEquals(2000L, warmRest.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_rest"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_item_rest"),
                    "blockId" to CanonicalJsonValue.Str("warm-items"),
                    "stepIndex0" to CanonicalJsonValue.Num(2.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("warmup"),
                    "legacyStageType" to CanonicalJsonValue.Str("rest"),
                    "itemId" to CanonicalJsonValue.Str("warm-rest"),
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(warmRest.phaseIdentityJson)
        )

        val stretchWork = legacyBoundaryItemStepFactsV1(prepared, steps[3], 0)
        assertEquals("stretch-items-stretch-action-work", stretchWork.sourceStepId)
        assertEquals("timed_work", stretchWork.phaseKind)
        assertEquals(4000L, stretchWork.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_item_work"),
                    "blockId" to CanonicalJsonValue.Str("stretch-items"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("stretch"),
                    "legacyStageType" to CanonicalJsonValue.Str("custom"),
                    "itemId" to CanonicalJsonValue.Str("stretch-action"),
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(stretchWork.phaseIdentityJson)
        )

        val coolWork = legacyBoundaryItemStepFactsV1(prepared, steps[4], 0)
        assertEquals("cool-items-cool-action-work", coolWork.sourceStepId)
        assertEquals("timed_work", coolWork.phaseKind)
        assertEquals(3000L, coolWork.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_item_work"),
                    "blockId" to CanonicalJsonValue.Str("cool-items"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("cooldown"),
                    "legacyStageType" to CanonicalJsonValue.Str("cooldown"),
                    "itemId" to CanonicalJsonValue.Str("cool-action"),
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(coolWork.phaseIdentityJson)
        )
    }

    @Test
    fun plainStretchBlockProducesCanonicalBoundaryFacts() {
        val plan = plan(
            blocks = listOf(
                StretchBlock(id = "stretch", order = 1, title = "拉伸", durationSec = 3, items = emptyList())
            )
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            plan.toSnapshot().toStorageJson(), WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        val result = TimedWorkoutEngine.dispatch(TimedWorkoutEngine.create(plan), WorkoutCommand.StartSession)
        val step = requireNotNull(result.state.currentStep)
        val facts = legacyBoundaryBlockStepFactsV1(prepared, step, 0)

        assertEquals("stretch-work", facts.sourceStepId)
        assertEquals("timed_work", facts.phaseKind)
        assertEquals(3000L, facts.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_block_work"),
                    "blockId" to CanonicalJsonValue.Str("stretch"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("stretch"),
                    "legacyStageType" to CanonicalJsonValue.Str("cooldown"),
                    "itemId" to CanonicalJsonValue.Null,
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(facts.phaseIdentityJson)
        )
    }

    @Test
    fun pureIntervalStagesAdvanceWithoutExerciseLibraryActions() {
        val plan = plan(
            blocks = listOf(
                WarmupBlock(id = "warmup", order = 1, title = "热身", durationSec = 3),
                circuit(
                    rounds = 1,
                    items = listOf(
                        stage(id = "work", name = "训练", type = TimedStageType.WORK, sec = 4),
                        stage(id = "rest", name = "休息", type = TimedStageType.REST, sec = 2),
                        stage(id = "custom", name = "核心保持", type = TimedStageType.CUSTOM, sec = 3)
                    )
                ),
                CooldownBlock(id = "cooldown", order = 3, title = "放松", durationSec = 2)
            )
        )
        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            plan.toSnapshot().toStorageJson(), WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        var result = TimedWorkoutEngine.dispatch(TimedWorkoutEngine.create(plan), WorkoutCommand.StartSession)

        assertEquals("warmup-work", result.state.currentStep?.id)
        assertEquals("热身", result.state.currentStep?.title)
        assertEquals(null, result.state.currentStep?.exerciseId)
        assertEquals(TimedStageType.WARMUP, result.state.currentStep?.stageType)
        val warmupFacts = legacyBoundaryBlockStepFactsV1(prepared, requireNotNull(result.state.currentStep), 0)
        assertEquals("warmup-work", warmupFacts.sourceStepId)
        assertEquals("timed_work", warmupFacts.phaseKind)
        assertEquals(3000L, warmupFacts.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_block_work"),
                    "blockId" to CanonicalJsonValue.Str("warmup"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("warmup"),
                    "legacyStageType" to CanonicalJsonValue.Str("warmup"),
                    "itemId" to CanonicalJsonValue.Null,
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(warmupFacts.phaseIdentityJson)
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals("circuit-r1-work-work", result.state.currentStep?.id)
        assertEquals("训练", result.state.currentStep?.title)
        assertEquals(null, result.state.currentStep?.exerciseId)
        assertEquals(TimedStageType.WORK, result.state.currentStep?.stageType)

        result = TimedWorkoutEngine.tick(result.state, seconds = 4)
        assertEquals("circuit-r1-rest-rest", result.state.currentStep?.id)
        assertEquals(TimedSessionStepKind.REST, result.state.currentStep?.kind)
        assertEquals("休息", result.state.currentStep?.title)
        val facts = legacyCircuitStepFactsV1(prepared, requireNotNull(result.state.currentStep), 1)
        assertEquals("circuit-r1-rest-rest", facts.sourceStepId)
        assertEquals("timed_rest", facts.phaseKind)
        assertEquals(2000L, facts.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_rest"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("circuit_item_rest"),
                    "blockId" to CanonicalJsonValue.Str("circuit"),
                    "stepIndex0" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("timed_circuit"),
                    "legacyStageType" to CanonicalJsonValue.Str("rest"),
                    "itemId" to CanonicalJsonValue.Str("rest"),
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Num(0.toBigDecimal())
                ))
            )),
            parseCanonicalJson(facts.phaseIdentityJson)
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 5)
        assertEquals("cooldown-work", result.state.currentStep?.id)
        assertEquals(TimedStageType.COOLDOWN, result.state.currentStep?.stageType)
        val cooldownFacts = legacyBoundaryBlockStepFactsV1(prepared, requireNotNull(result.state.currentStep), 0)
        assertEquals("cooldown-work", cooldownFacts.sourceStepId)
        assertEquals("timed_work", cooldownFacts.phaseKind)
        assertEquals(2000L, cooldownFacts.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_work"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("boundary_block_work"),
                    "blockId" to CanonicalJsonValue.Str("cooldown"),
                    "stepIndex0" to CanonicalJsonValue.Num(0.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("cooldown"),
                    "legacyStageType" to CanonicalJsonValue.Str("cooldown"),
                    "itemId" to CanonicalJsonValue.Null,
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Null
                ))
            )),
            parseCanonicalJson(cooldownFacts.phaseIdentityJson)
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertEquals(14, result.state.activeElapsedSec)
    }

    @Test
    fun validTimedSnapshotAdvancesThroughActionsRestsRoundsAndCompletes() {
        val snapshot = plan(
            blocks = listOf(
                circuit(
                    rounds = 2,
                    restBetweenRoundsSec = 3,
                    items = listOf(
                        item(id = "jump", exerciseId = "jumping-jacks", workSec = 4, restSec = 2),
                        item(id = "squat", exerciseId = "bodyweight-squat", workSec = 3)
                    )
                )
            )
        ).toSnapshot()

        val prepared = (PlanSnapshotStorageV1Validator.prepare(
            snapshot.toStorageJson(), WorkoutMode.TIMED
        ) as PreparedPlanSnapshotStorageV1Result.Valid).prepared
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(snapshot, sessionId = "session-timed"),
            command = WorkoutCommand.StartSession
        )

        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertEquals("circuit-r1-jump-work", result.state.currentStep?.id)
        assertEquals(4, result.state.remainingSec)
        assertTrue(result.events[0] is WorkoutEvent.SessionStarted)
        assertTrue(result.events[1] is WorkoutEvent.TimedWorkStarted)
        assertEquals(7, result.state.steps.size)

        result = TimedWorkoutEngine.tick(result.state, seconds = 4)
        assertEquals("circuit-r1-jump-rest", result.state.currentStep?.id)
        assertEquals(2, result.state.remainingSec)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state, seconds = 5)
        assertEquals("circuit-r1-round-rest", result.state.currentStep?.id)
        assertEquals(3, result.state.remainingSec)
        val facts = legacyCircuitStepFactsV1(prepared, requireNotNull(result.state.currentStep), 3)
        assertEquals("circuit-r1-round-rest", facts.sourceStepId)
        assertEquals("timed_rest", facts.phaseKind)
        assertEquals(3000L, facts.plannedDurationMs)
        assertEquals(
            CanonicalJsonValue.Obj(linkedMapOf(
                "phaseIdentityContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "family" to CanonicalJsonValue.Str("legacy_timed_v1"),
                "payloadVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                "mode" to CanonicalJsonValue.Str("timed"),
                "phaseKind" to CanonicalJsonValue.Str("timed_rest"),
                "orderedStructureSignature" to CanonicalJsonValue.Obj(linkedMapOf(
                    "signatureContractVersion" to CanonicalJsonValue.Num(1.toBigDecimal()),
                    "algorithm" to CanonicalJsonValue.Str("sha256"),
                    "digestHexLowercase" to CanonicalJsonValue.Str(prepared.orderedStructureDigestHexLowercase())
                )),
                "payload" to CanonicalJsonValue.Obj(linkedMapOf(
                    "variant" to CanonicalJsonValue.Str("between_round_rest"),
                    "blockId" to CanonicalJsonValue.Str("circuit"),
                    "stepIndex0" to CanonicalJsonValue.Num(3.toBigDecimal()),
                    "legacyBlockKind" to CanonicalJsonValue.Str("timed_circuit"),
                    "legacyStageType" to CanonicalJsonValue.Str("rest"),
                    "itemId" to CanonicalJsonValue.Null,
                    "exerciseId" to CanonicalJsonValue.Null,
                    "roundIndex0" to CanonicalJsonValue.Num(0.toBigDecimal())
                ))
            )),
            parseCanonicalJson(facts.phaseIdentityJson)
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 12)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertEquals(7, result.state.completedStepCount)
        assertEquals(0, result.state.remainingSec)
        assertTrue(result.events.last() is WorkoutEvent.SessionCompleted)
    }

    @Test
    fun pauseFreezesRemainingTimeAndResumeContinuesOriginalStep() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(3, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        assertEquals(SessionStatus.PAUSED, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionPaused)

        result = TimedWorkoutEngine.tick(result.state, seconds = 10)
        assertEquals(SessionStatus.PAUSED, result.state.status)
        assertEquals(3, result.state.remainingSec)
        assertEquals(2, result.state.activeElapsedSec)
        assertEquals(10, result.state.pausedElapsedSec)
        assertTrue(result.events.isEmpty())

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionResumed)

        result = TimedWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertEquals(
            listOf(
                TimedWorkoutControlHistoryType.START_SESSION,
                TimedWorkoutControlHistoryType.PAUSE_SESSION,
                TimedWorkoutControlHistoryType.RESUME_SESSION
            ),
            result.state.controlHistory.map { event -> event.type }
        )
        assertEquals(5, result.state.stepHistory.single().actualDurationSec)
        assertEquals(10, result.state.pausedElapsedSec)
        assertEquals(TimedSessionStepHistoryStatus.COMPLETED, result.state.stepHistory.single().status)
    }

    @Test
    fun endingCueEventsFireOncePerStepAndRemainingSecond() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    restSec = 2,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 2),
                        restEnding = CountdownCue(thresholdSec = 1)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        assertFalse(result.events.any { event -> event is WorkoutEvent.TimedWorkEnding })

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(2), result.events.workEndingRemainingSeconds())

        val noTick = TimedWorkoutEngine.tick(result.state, seconds = 0)
        assertTrue(noTick.events.isEmpty())
        assertEquals(2, noTick.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        assertFalse(result.events.any { event -> event is WorkoutEvent.TimedWorkEnding })

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.workEndingRemainingSeconds())

        result = TimedWorkoutEngine.tick(result.state)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.restEndingRemainingSeconds())
    }

    @Test
    fun endingCueEventsCoverConfiguredFinalSecondsThroughOne() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 8,
                    cueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 6)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )
        val remainingSeconds = mutableListOf<Int>()

        repeat(8) {
            result = TimedWorkoutEngine.tick(result.state)
            remainingSeconds += result.events.workEndingRemainingSeconds()
        }

        assertEquals(listOf(6, 5, 4, 3, 2, 1), remainingSeconds)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
    }

    @Test
    fun endingCueThresholdClampsToStageDurationAcrossTimedStages() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                plan(
                    preferences = PlanPreferences(
                        cueSettings = CueSettings(
                            actionEnding = CountdownCue(thresholdSec = 5),
                            restEnding = CountdownCue(thresholdSec = 5)
                        )
                    ),
                    blocks = listOf(
                        WarmupBlock(id = "warm", order = 1, title = "Warm", durationSec = 2),
                        circuit(
                            items = listOf(
                                stage(id = "work", name = "Work", type = TimedStageType.WORK, sec = 2),
                                stage(id = "rest", name = "Rest", type = TimedStageType.REST, sec = 2)
                            )
                        ),
                        CooldownBlock(id = "cool", order = 3, title = "Cool", durationSec = 2)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )
        val workRemainingSeconds = result.events.workEndingRemainingSeconds().toMutableList()
        val restRemainingSeconds = result.events.restEndingRemainingSeconds().toMutableList()

        repeat(8) {
            result = TimedWorkoutEngine.tick(result.state)
            workRemainingSeconds += result.events.workEndingRemainingSeconds()
            restRemainingSeconds += result.events.restEndingRemainingSeconds()
        }

        assertEquals(listOf(2, 1, 2, 1, 2, 1), workRemainingSeconds)
        assertEquals(listOf(2, 1), restRemainingSeconds)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
    }

    @Test
    fun itemCueOverridesGlobalCueAndTooLargeThresholdsClampToStageDuration() {
        val globalCueTooLarge = CueSettings(
            actionEnding = CountdownCue(thresholdSec = 5),
            restEnding = CountdownCue(thresholdSec = 5)
        )
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    restSec = 2,
                    cueSettings = globalCueTooLarge,
                    itemCueSettings = CueSettings(
                        actionEnding = CountdownCue(thresholdSec = 1),
                        restEnding = CountdownCue(thresholdSec = 1)
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )
        assertEquals(emptyList<Int>(), result.events.workEndingRemainingSeconds())

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(listOf(1), result.events.workEndingRemainingSeconds())

        result = TimedWorkoutEngine.tick(result.state)
        assertTrue(result.events.first() is WorkoutEvent.RestStarted)

        result = TimedWorkoutEngine.tick(result.state)
        assertEquals(listOf(1), result.events.restEndingRemainingSeconds())

        result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                singleActionPlan(
                    workSec = 3,
                    cueSettings = globalCueTooLarge
                )
            ),
            command = WorkoutCommand.StartSession
        )
        val clampedRemainingSeconds = result.events.workEndingRemainingSeconds().toMutableList()

        repeat(3) {
            result = TimedWorkoutEngine.tick(result.state)
            clampedRemainingSeconds += result.events.workEndingRemainingSeconds()
        }

        assertEquals(listOf(3, 2, 1), clampedRemainingSeconds)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
    }

    @Test
    fun skipStepMovesToNextExecutableStepAndLastSkipCompletes() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                plan(
                    blocks = listOf(
                        circuit(
                            items = listOf(
                                item(id = "first", exerciseId = "jumping-jacks", workSec = 10),
                                item(id = "second", exerciseId = "bodyweight-squat", workSec = 10)
                            )
                        )
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        assertEquals("circuit-r1-second-work", result.state.currentStep?.id)
        assertEquals(listOf("circuit-r1-first-work"), result.state.skippedStepIds)
        assertEquals(TimedSessionStepHistoryStatus.SKIPPED, result.state.skippedStepHistory.single().status)
        assertEquals("circuit-r1-first-work", result.state.skippedStepHistory.single().stepId)
        assertEquals("jumping-jacks", result.state.skippedStepHistory.single().title)
        assertEquals(10, result.state.skippedStepHistory.single().remainingSec)
        assertEquals(0, result.state.skippedStepHistory.single().actualDurationSec)
        assertEquals(TimedWorkoutControlHistoryType.SKIP_STEP, result.state.controlHistory.last().type)
        assertTrue(result.events.single() is WorkoutEvent.TimedWorkStarted)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        assertEquals(SessionStatus.COMPLETED, result.state.status)
        assertTrue(result.events.single() is WorkoutEvent.SessionCompleted)
    }

    @Test
    fun extendRestOnlyChangesCurrentRestStep() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 2, restSec = 2)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        assertEquals(2, result.state.remainingSec)
        assertEquals(0, result.state.extendedRestSec)

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(SessionStepKind.TIMED_REST, result.state.currentSessionStep?.kind)
        assertEquals(2, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        assertEquals(17, result.state.remainingSec)
        assertEquals(15, result.state.extendedRestSec)
        assertEquals(2, result.state.steps.size)
        assertEquals(1, result.state.restExtensionHistory.size)
        assertEquals(15, result.state.restExtensionHistory.single().addedSec)
        assertEquals(15, result.state.restExtensionHistory.single().cumulativeAddedSec)
        assertEquals(1, result.state.restExtensionHistory.single().stepIndex)
        assertEquals(2, result.state.restExtensionHistory.single().plannedRestSec)
        assertEquals(0, result.state.restExtensionHistory.single().restElapsedBeforeExtensionSec)
        assertEquals(2, result.state.restExtensionHistory.single().extensionAtRemainingSec)
        assertEquals("jump", result.state.restExtensionHistory.single().previousStageId)
        assertEquals("jumping-jacks", result.state.restExtensionHistory.single().previousStageTitle)
        assertEquals(15, result.state.stepHistory.last().extendedRestSec)
        assertEquals(TimedWorkoutControlHistoryType.EXTEND_REST, result.state.controlHistory.last().type)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = -5))
        assertEquals(17, result.state.remainingSec)
        assertEquals(15, result.state.extendedRestSec)
        assertEquals(1, result.state.restExtensionHistory.size)
    }

    @Test
    fun multipleRestExtensionsAccumulateOnSameRestWithoutInsertingSteps() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(
                plan(
                    blocks = listOf(
                        circuit(
                            rounds = 2,
                            items = listOf(
                                stage(id = "work", name = "工作", type = TimedStageType.WORK, sec = 2),
                                stage(id = "rest", name = "恢复", type = TimedStageType.REST, sec = 5)
                            )
                        )
                    )
                )
            ),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals("circuit-r1-rest-rest", result.state.currentStep?.id)
        assertEquals(4, result.state.steps.size)

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))

        assertEquals(33, result.state.remainingSec)
        assertEquals(30, result.state.extendedRestSec)
        assertEquals(4, result.state.steps.size)
        assertEquals(listOf(15, 30), result.state.restExtensionHistory.map { record -> record.cumulativeAddedSec })
        assertEquals(listOf(2, 2), result.state.restExtensionHistory.map { record -> record.restElapsedBeforeExtensionSec })
        assertEquals(listOf(3, 18), result.state.restExtensionHistory.map { record -> record.extensionAtRemainingSec })
        assertEquals(listOf(1, 1), result.state.restExtensionHistory.map { record -> record.roundIndex })
        assertEquals("work", result.state.restExtensionHistory.first().previousStageId)
        assertEquals("工作", result.state.restExtensionHistory.first().previousStageTitle)
        assertEquals(30, result.state.stepHistory.last().extendedRestSec)
    }

    @Test
    fun abandonedRestIgnoresLateExtendRestCommand() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 2, restSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 2)
        assertEquals(SessionStepKind.TIMED_REST, result.state.currentSessionStep?.kind)
        assertEquals(5, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(
            state = result.state,
            command = WorkoutCommand.EndSession(reason = "user_exit")
        )
        val abandonedState = result.state

        result = TimedWorkoutEngine.dispatch(
            state = abandonedState,
            command = WorkoutCommand.ExtendRest(seconds = 15)
        )

        assertEquals(SessionStatus.ABANDONED, result.state.status)
        assertTrue(result.state.isTerminal)
        assertEquals(abandonedState.currentStep?.id, result.state.currentStep?.id)
        assertEquals(5, result.state.remainingSec)
        assertEquals(0, result.state.extendedRestSec)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun endSessionMarksAbandonedWithoutCompletedEvent() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "user_exit"))

        assertEquals(SessionStatus.ABANDONED, result.state.status)
        assertTrue(result.state.isTerminal)
        assertFalse(result.events.any { event -> event is WorkoutEvent.SessionCompleted })
        assertEquals("user_exit", result.state.earlyEnd?.reason)
        assertEquals(SessionStatus.ABANDONED, result.state.earlyEnd?.status)
        assertEquals("circuit-r1-jump-work", result.state.earlyEnd?.currentStepId)
        assertEquals(5, result.state.earlyEnd?.currentStepRemainingSec)
        assertEquals(0, result.state.earlyEnd?.currentStepActualDurationSec)
        assertEquals(TimedSessionStepHistoryStatus.ABANDONED, result.state.stepHistory.single().status)
        assertEquals(TimedWorkoutControlHistoryType.END_SESSION, result.state.controlHistory.last().type)
    }

    @Test
    fun terminalStateIgnoresLateTrainingControlsWithoutHistoryPollution() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 2, restSec = 5)),
            command = WorkoutCommand.StartSession
        )
        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "done"))
        val terminalState = result.state

        val lateCommands = listOf(
            WorkoutCommand.PauseSession,
            WorkoutCommand.ResumeSession,
            WorkoutCommand.SkipStep,
            WorkoutCommand.ExtendRest(seconds = 15),
            WorkoutCommand.EndSession(reason = "late")
        )

        lateCommands.forEach { command ->
            result = TimedWorkoutEngine.dispatch(result.state, command)
        }

        assertEquals(terminalState, result.state)
        assertEquals(listOf("done"), result.state.controlHistory.mapNotNull { event -> event.reason })
        assertEquals(1, result.state.controlHistory.count { event ->
            event.type == TimedWorkoutControlHistoryType.END_SESSION
        })
    }

    @Test
    fun e91RecoveryRegressionKeepsPausedBackgroundTickResumeRestExtensionAndTerminalBoundaryStable() {
        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(singleActionPlan(workSec = 4, restSec = 5)),
            command = WorkoutCommand.StartSession
        )

        result = TimedWorkoutEngine.tick(result.state, seconds = 1)
        assertEquals(3, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        val pausedState = result.state

        result = TimedWorkoutEngine.tick(result.state, seconds = 99)
        assertEquals(pausedState.copy(pausedElapsedSec = 99), result.state)
        assertTrue(result.events.isEmpty())

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        result = TimedWorkoutEngine.tick(result.state, seconds = 3)
        assertEquals(SessionStepKind.TIMED_REST, result.state.currentSessionStep?.kind)
        assertEquals(5, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ExtendRest(seconds = 15))
        assertEquals(20, result.state.remainingSec)
        assertEquals(15, result.state.extendedRestSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "user_requested"))
        val terminalState = result.state

        result = TimedWorkoutEngine.tick(result.state, seconds = 99)
        assertEquals(terminalState, result.state)

        listOf(
            WorkoutCommand.ResumeSession,
            WorkoutCommand.SkipStep,
            WorkoutCommand.ExtendRest(seconds = 15),
            WorkoutCommand.EndSession(reason = "late")
        ).forEach { command ->
            result = TimedWorkoutEngine.dispatch(result.state, command)
        }

        assertEquals(terminalState, result.state)
        assertEquals(
            listOf(
                TimedWorkoutControlHistoryType.START_SESSION,
                TimedWorkoutControlHistoryType.PAUSE_SESSION,
                TimedWorkoutControlHistoryType.RESUME_SESSION,
                TimedWorkoutControlHistoryType.EXTEND_REST,
                TimedWorkoutControlHistoryType.END_SESSION
            ),
            result.state.controlHistory.map { event -> event.type }
        )
        assertEquals(1, result.state.restExtensionHistory.size)
        assertEquals(SessionStatus.ABANDONED, result.state.status)
    }

    @Test
    fun followAlongPlanUsesTimedEngineForStartTickPauseResumeSkipAndEnd() {
        val followAlongPlan = plan(
            blocks = listOf(
                circuit(
                    items = listOf(
                        item(id = "jump", exerciseId = "jumping-jacks", workSec = 3, restSec = 2),
                        item(id = "squat", exerciseId = "bodyweight-squat", workSec = 4)
                    )
                )
            )
        ).copy(
            mode = WorkoutMode.FOLLOW_ALONG,
            followAlong = FollowAlongPlanMeta(preset = true)
        )

        var result = TimedWorkoutEngine.dispatch(
            state = TimedWorkoutEngine.create(followAlongPlan),
            command = WorkoutCommand.StartSession
        )
        assertEquals(SessionStatus.ACTIVE, result.state.status)
        assertEquals("circuit-r1-jump-work", result.state.currentStep?.id)
        assertTrue(result.events.any { event -> event is WorkoutEvent.TimedWorkStarted })

        result = TimedWorkoutEngine.tick(result.state, seconds = 1)
        assertEquals(2, result.state.remainingSec)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.PauseSession)
        assertEquals(SessionStatus.PAUSED, result.state.status)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.ResumeSession)
        assertEquals(SessionStatus.ACTIVE, result.state.status)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.SkipStep)
        assertEquals("circuit-r1-jump-rest", result.state.currentStep?.id)
        assertEquals(TimedSessionStepHistoryStatus.SKIPPED, result.state.skippedStepHistory.single().status)

        result = TimedWorkoutEngine.dispatch(result.state, WorkoutCommand.EndSession(reason = "user_requested"))
        assertEquals(SessionStatus.ABANDONED, result.state.status)
        assertEquals("user_requested", result.state.earlyEnd?.reason)
        assertEquals(TimedWorkoutControlHistoryType.END_SESSION, result.state.controlHistory.last().type)
    }

    private fun singleActionPlan(
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings? = null,
        itemCueSettings: CueSettings? = null
    ): WorkoutPlan {
        return plan(
            preferences = cueSettings?.let { PlanPreferences(cueSettings = it) },
            blocks = listOf(
                circuit(
                    items = listOf(
                        item(
                            id = "jump",
                            exerciseId = "jumping-jacks",
                            workSec = workSec,
                            restSec = restSec,
                            cueSettings = itemCueSettings
                        )
                    )
                )
            )
        )
    }

    private fun plan(
        blocks: List<PlanBlock>,
        preferences: PlanPreferences? = null
    ): WorkoutPlan {
        return WorkoutPlan(
            id = "plan-timed",
            mode = WorkoutMode.TIMED,
            title = "Timed plan",
            blocks = blocks,
            preferences = preferences,
            createdAt = "2026-05-30T00:00:00Z",
            updatedAt = "2026-05-30T00:00:00Z"
        )
    }

    private fun circuit(
        rounds: Int = 1,
        restBetweenRoundsSec: Int? = null,
        items: List<TimedExerciseItem>
    ): TimedCircuitBlock {
        return TimedCircuitBlock(
            id = "circuit",
            order = 1,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            items = items
        )
    }

    private fun item(
        id: String,
        exerciseId: String,
        workSec: Int,
        restSec: Int? = null,
        cueSettings: CueSettings? = null
    ): TimedExerciseItem {
        return TimedExerciseItem(
            id = id,
            exerciseId = exerciseId,
            workDurationSec = workSec,
            restAfterSec = restSec,
            cueSettings = cueSettings
        )
    }

    private fun stage(
        id: String,
        name: String,
        type: TimedStageType,
        sec: Int
    ): TimedExerciseItem {
        return TimedExerciseItem(
            id = id,
            exerciseId = null,
            labelOverride = name,
            stageType = type,
            workDurationSec = sec
        )
    }

    private fun WorkoutPlan.toSnapshot(): WorkoutPlanSnapshot {
        return WorkoutPlanSnapshot(
            planId = id,
            title = title,
            mode = mode,
            blocks = blocks,
            preferences = preferences,
            followAlong = followAlong
        )
    }

    private fun List<WorkoutEvent>.workEndingRemainingSeconds(): List<Int> {
        return filterIsInstance<WorkoutEvent.TimedWorkEnding>().map { event -> event.remainingSec }
    }

    private fun List<WorkoutEvent>.restEndingRemainingSeconds(): List<Int> {
        return filterIsInstance<WorkoutEvent.RestEnding>().map { event -> event.remainingSec }
    }
}
