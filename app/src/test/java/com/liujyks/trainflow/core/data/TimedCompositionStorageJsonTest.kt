package com.liujyks.trainflow.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimedCompositionStorageJsonTest {
    private lateinit var database: TrainFlowDatabase
    private lateinit var planRepository: WorkoutPlanRepository
    private lateinit var sessionRepository: WorkoutSessionRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrainFlowDatabase::class.java
        ).allowMainThreadQueries().build()
        planRepository = WorkoutPlanRepository(database)
        sessionRepository = WorkoutSessionRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun v2PlanBlocksRoundTripThroughExistingBlocksJson() = runBlocking {
        planRepository.upsertPlan(compositionPlan())

        val restored = requireNotNull(planRepository.getPlan("composition-plan"))
        val block = restored.blocks.single() as TimedCompositionBlock
        val group = block.stageGroups.single()

        assertEquals(WorkoutMode.TIMED, restored.mode)
        assertEquals(2, block.compositionVersion)
        assertEquals(90, block.warmupSec)
        assertEquals(60, block.cooldownSec)
        assertEquals(3, block.rounds)
        assertEquals(30, block.restBetweenRoundsSec)
        assertEquals("group-main", group.id)
        assertEquals(60, group.durationSec)
        assertEquals(listOf(TimedCompositionTargetKind.ACTION, TimedCompositionTargetKind.REST), group.targets.map { it.kind })
        assertEquals(listOf("work-target", "rest-target"), group.targets.map { it.id })
    }

    @Test
    fun v2SessionSnapshotRoundTripsWithoutRewritingSnapshotShape() = runBlocking {
        sessionRepository.upsertSession(
            WorkoutSession(
                id = "composition-session",
                planId = "composition-plan",
                mode = WorkoutMode.TIMED,
                planSnapshot = WorkoutPlanSnapshot(
                    planId = "composition-plan",
                    title = "Composition Snapshot",
                    mode = WorkoutMode.TIMED,
                    blocks = compositionPlan().blocks
                ),
                status = SessionStatus.COMPLETED,
                startedAt = "2026-06-26T10:00:00Z",
                endedAt = "2026-06-26T10:12:00Z",
                totalElapsedSec = 720,
                effectiveElapsedSec = 650,
                pausedElapsedSec = 70
            )
        )

        val snapshot = sessionRepository.getSessions().single().planSnapshot
        val block = snapshot.blocks.single() as TimedCompositionBlock

        assertEquals("composition-plan", snapshot.planId)
        assertEquals("Composition Snapshot", snapshot.title)
        assertEquals(2, block.compositionVersion)
        assertEquals("Main", block.stageGroups.single().name)
        assertEquals(60, block.stageGroups.single().durationSec)
    }

    @Test
    fun legacyTimedPlanBlocksKeepLegacyJsonShapeOnRoundTrip() = runBlocking {
        planRepository.upsertPlan(legacyPlan())

        val restored = requireNotNull(planRepository.getPlan("legacy-plan"))

        assertTrue(restored.blocks.first() is WarmupBlock)
        assertTrue(restored.blocks[1] is TimedCircuitBlock)
        assertTrue(restored.blocks.last() is CooldownBlock)
        assertFalse(restored.blocks.any { block -> block is TimedCompositionBlock })
        assertEquals("Legacy Plan", restored.title)
    }

    private fun compositionPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "composition-plan",
            mode = WorkoutMode.TIMED,
            title = "Composition Plan",
            description = "JSON payload foundation",
            blocks = listOf(
                TimedCompositionBlock(
                    id = "composition-block",
                    order = 1,
                    title = "V2",
                    warmupSec = 90,
                    cooldownSec = 60,
                    rounds = 3,
                    restBetweenRoundsSec = 30,
                    stageGroups = listOf(
                        TimedCompositionStageGroup(
                            id = "group-main",
                            order = 1,
                            name = "Main",
                            colorHex = TimedStageType.WORK.defaultColorHex,
                            targets = listOf(
                                TimedCompositionTarget(
                                    id = "work-target",
                                    order = 1,
                                    name = "Work",
                                    kind = TimedCompositionTargetKind.ACTION,
                                    durationSec = 40,
                                    colorHex = TimedStageType.WORK.defaultColorHex
                                ),
                                TimedCompositionTarget(
                                    id = "rest-target",
                                    order = 2,
                                    name = "Rest",
                                    kind = TimedCompositionTargetKind.REST,
                                    durationSec = 20,
                                    colorHex = TimedStageType.REST.defaultColorHex
                                )
                            )
                        )
                    )
                )
            ),
            createdAt = "2026-06-26T09:00:00Z",
            updatedAt = "2026-06-26T09:00:00Z"
        )
    }

    private fun legacyPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "legacy-plan",
            mode = WorkoutMode.TIMED,
            title = "Legacy Plan",
            blocks = listOf(
                WarmupBlock(
                    id = "legacy-warmup",
                    order = 1,
                    durationSec = 60
                ),
                TimedCircuitBlock(
                    id = "legacy-circuit",
                    order = 2,
                    rounds = 2,
                    restBetweenRoundsSec = 15,
                    items = listOf(
                        TimedExerciseItem(
                            id = "legacy-work",
                            labelOverride = "Work",
                            stageType = TimedStageType.WORK,
                            workDurationSec = 40,
                            restAfterSec = 20
                        )
                    )
                ),
                CooldownBlock(
                    id = "legacy-cooldown",
                    order = 3,
                    durationSec = 60
                )
            ),
            createdAt = "2026-06-26T09:00:00Z",
            updatedAt = "2026-06-26T09:00:00Z"
        )
    }
}
