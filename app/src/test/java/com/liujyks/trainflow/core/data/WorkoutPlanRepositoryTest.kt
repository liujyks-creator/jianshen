package com.liujyks.trainflow.core.data

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.notifications.clearLegacyPlanReminders
import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutPlanRepositoryTest {
    private lateinit var database: TrainFlowDatabase
    private lateinit var repository: WorkoutPlanRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrainFlowDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = WorkoutPlanRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun timedPlanPersistsCustomStagesRoundsColorsIconsNamesAndOrder() = runBlocking {
        repository.upsertPlan(customTimedPlan())

        val restored = requireNotNull(repository.getPlan("custom-timed"))
        val warmup = restored.blocks[0] as WarmupBlock
        val circuit = restored.blocks[1] as TimedCircuitBlock
        val cooldown = restored.blocks[2] as CooldownBlock
        val items = circuit.items

        assertEquals(WorkoutMode.TIMED, restored.mode)
        assertEquals("用户自定义计时", restored.title)
        assertEquals("本地保存的纯间歇计时器计划", restored.description)
        assertEquals("激活热身", warmup.title)
        assertEquals(90, warmup.durationSec)
        assertEquals("warmup-flow", warmup.items.single().iconKey)
        assertEquals("#00BCD4", warmup.items.single().colorHex)
        assertEquals(4, circuit.rounds)
        assertEquals(35, circuit.restBetweenRoundsSec)
        assertEquals(
            listOf("stage-rest", "stage-custom", "stage-work"),
            items.map { item -> item.id }
        )
        assertEquals(listOf("主动恢复", "核心保持", "冲刺"), items.map { item -> item.labelOverride })
        assertEquals(listOf(20, 50, 40), items.map { item -> item.workDurationSec })
        assertEquals(listOf(TimedStageType.REST, TimedStageType.CUSTOM, TimedStageType.WORK), items.map { it.stageType })
        assertEquals(listOf("rest", "plank", "bolt"), items.map { it.iconKey })
        assertEquals(listOf("#2FBF8F", "#8B6CFF", "#F26B4F"), items.map { it.colorHex })
        assertEquals("静态放松", cooldown.title)
        assertEquals(75, cooldown.durationSec)
        assertEquals("cooldown-moon", cooldown.items.single().iconKey)
        assertEquals("#FFC107", cooldown.items.single().colorHex)
        assertEquals(4, restored.preferences?.cueSettings?.actionEnding?.thresholdSec)
        assertEquals(3, restored.preferences?.cueSettings?.restEnding?.thresholdSec)
    }

    @Test
    fun timedPlanInvalidStageColorFallsBackDuringRoundTrip() = runBlocking {
        val plan = customTimedPlan().copy(
            blocks = listOf(
                TimedCircuitBlock(
                    id = "invalid-color-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "bad-color-work",
                            labelOverride = "坏色值",
                            stageType = TimedStageType.WORK,
                            iconKey = "work",
                            colorHex = "not-a-color",
                            workDurationSec = 30,
                            autoAdvance = true
                        )
                    )
                )
            )
        )

        repository.upsertPlan(plan)

        val restored = requireNotNull(repository.getPlan("custom-timed"))
        val item = (restored.blocks.single() as TimedCircuitBlock).items.single()

        assertEquals(TimedStageType.WORK.defaultColorHex, item.colorHex)
    }

    @Test
    fun strengthPlanPersistsTargetsWithoutCreatingSessionRecords() = runBlocking {
        repository.upsertPlan(strengthPlan())

        val restored = repository.getPlans().single()
        val block = restored.blocks.single() as StrengthExerciseBlock

        assertEquals(WorkoutMode.STRENGTH, restored.mode)
        assertEquals("本地保存的力量计划", restored.description)
        assertEquals("barbell-bench-press", block.exerciseId)
        assertEquals(WeightValue(60.0, WeightUnit.KG), block.target?.weight)
        assertEquals(RepTarget.Range(8, 12), block.target?.repTarget)
        assertEquals(2, block.sets.size)
        assertEquals("bench-warmup", block.sets.first().id)
        assertEquals(StrengthSetKind.WARMUP, block.sets.first().kind)
        assertEquals(0, database.workoutSessionDao().strengthSetRecordCount())
    }

    @Test
    fun followAlongPresetMetadataCanPersistButHasNoEditorSaveEntry() = runBlocking {
        repository.upsertPlan(followAlongPlan())

        val restored = repository.plans.first().single()

        assertEquals(WorkoutMode.FOLLOW_ALONG, restored.mode)
        val followAlong = requireNotNull(restored.followAlong)
        assertTrue(followAlong.preset)
        assertEquals("cover-basic", followAlong.coverMediaId)
    }

    @Test
    fun deletePlanRemovesOnlyPlanRows() = runBlocking {
        repository.upsertPlan(customTimedPlan())
        repository.deletePlan("custom-timed")

        assertTrue(repository.getPlans().isEmpty())
        assertEquals(0, database.workoutPlanDao().count())
        assertEquals(0, database.workoutSessionDao().sessionCount())
    }

    @Test
    @Config(sdk = [35], application = Application::class)
    fun clearingLegacyRemindersPreservesPlanAndSessionData() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val plan = customTimedPlan().copy(id = "p")
        repository.upsertPlan(plan)
        val planDao = database.workoutPlanDao()
        val oldJson = """{"enabled":true,"scheduleAt":"2026-06-16T11:30:00Z","repeatRule":"weekly"}"""
        planDao.upsertPlan(requireNotNull(planDao.getPlan("p")).copy(reminderJson = oldJson))
        val session = WorkoutSessionEntity(
            id = "history-p", planId = "p", mode = "timed", status = "completed",
            planSnapshotJson = plan.toPlanSnapshot().toStorageJson(),
            startedAt = "2026-06-16T11:30:00Z", endedAt = "2026-06-16T11:32:00Z"
        )
        val sessionDao = database.workoutSessionDao()
        sessionDao.insertSession(session)
        val beforePlan = requireNotNull(planDao.getPlan("p"))
        val beforeSession = sessionDao.sessionsForRecorderGate().single()

        val alarms = application.getSystemService(AlarmManager::class.java)
        val oldIntent = Intent("com.liujyks.trainflow.PLAN_REMINDER").setClassName(
            application, "com.liujyks.trainflow.core.notifications.PlanReminderNotificationReceiver"
        )
        val pending = PendingIntent.getBroadcast(
            application, 112, oldIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarms.set(AlarmManager.RTC_WAKEUP, 1_900_000_000_000L, pending)
        assertTrue(shadowOf(alarms).scheduledAlarms.any { it.operation == pending })

        val notifications = application.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("trainflow_plan_reminders", "训练提醒", NotificationManager.IMPORTANCE_DEFAULT))
        notifications.createNotificationChannel(NotificationChannel("trainflow_active_workout", "训练进行中", NotificationManager.IMPORTANCE_LOW))
        notifications.notify(112, Notification.Builder(application, "trainflow_plan_reminders")
            .setSmallIcon(com.liujyks.trainflow.R.drawable.ic_launcher_foreground)
            .setContentText("旧预约").build())
        notifications.notify(7200, Notification.Builder(application, "trainflow_active_workout")
            .setSmallIcon(com.liujyks.trainflow.R.drawable.ic_launcher_foreground)
            .setContentText("当前训练").build())

        clearLegacyPlanReminders(application, repository)

        assertFalse(shadowOf(alarms).scheduledAlarms.any { it.operation == pending })
        assertNull(shadowOf(notifications).getNotification(112))
        assertEquals("当前训练", shadowOf(notifications).getNotification(7200)
            .extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(beforePlan.copy(reminderJson = null), planDao.getPlan("p"))
        assertEquals(beforeSession, sessionDao.sessionsForRecorderGate().single())
        val restored = requireNotNull(repository.getPlan("p"))
        repository.upsertPlan(restored)
        assertNull(planDao.getPlan("p")?.reminderJson)
    }

    private fun customTimedPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "custom-timed",
            mode = WorkoutMode.TIMED,
            title = "用户自定义计时",
            description = "本地保存的纯间歇计时器计划",
            blocks = listOf(
                WarmupBlock(
                    id = "stage-warmup",
                    order = 1,
                    title = "激活热身",
                    durationSec = 90,
                    items = listOf(
                        TimedExerciseItem(
                            id = "stage-warmup",
                            labelOverride = "激活热身",
                            stageType = TimedStageType.WARMUP,
                            iconKey = "warmup-flow",
                            colorHex = "#00BCD4",
                            workDurationSec = 90,
                            autoAdvance = true
                        )
                    )
                ),
                TimedCircuitBlock(
                    id = "custom-circuit",
                    order = 2,
                    title = "自定义阶段",
                    rounds = 4,
                    restBetweenRoundsSec = 35,
                    items = listOf(
                        TimedExerciseItem(
                            id = "stage-rest",
                            labelOverride = "主动恢复",
                            stageType = TimedStageType.REST,
                            iconKey = "rest",
                            colorHex = "#2FBF8F",
                            workDurationSec = 20,
                            autoAdvance = true
                        ),
                        TimedExerciseItem(
                            id = "stage-custom",
                            labelOverride = "核心保持",
                            stageType = TimedStageType.CUSTOM,
                            iconKey = "plank",
                            colorHex = "#8B6CFF",
                            workDurationSec = 50,
                            autoAdvance = true
                        ),
                        TimedExerciseItem(
                            id = "stage-work",
                            labelOverride = "冲刺",
                            stageType = TimedStageType.WORK,
                            iconKey = "bolt",
                            colorHex = "#F26B4F",
                            workDurationSec = 40,
                            autoAdvance = true
                        )
                    )
                ),
                CooldownBlock(
                    id = "stage-cooldown",
                    order = 3,
                    title = "静态放松",
                    durationSec = 75,
                    items = listOf(
                        TimedExerciseItem(
                            id = "stage-cooldown",
                            labelOverride = "静态放松",
                            stageType = TimedStageType.COOLDOWN,
                            iconKey = "cooldown-moon",
                            colorHex = "#FFC107",
                            workDurationSec = 75,
                            autoAdvance = true
                        )
                    )
                )
            ),
            preferences = PlanPreferences(
                cueSettings = CueSettings(
                    actionEnding = CountdownCue(thresholdSec = 4),
                    restEnding = CountdownCue(thresholdSec = 3)
                )
            ),
            createdAt = "2026-06-13T08:00:00Z",
            updatedAt = "2026-06-13T08:01:00Z"
        )
    }

    private fun strengthPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "strength-plan",
            mode = WorkoutMode.STRENGTH,
            title = "力量持久化",
            description = "本地保存的力量计划",
            blocks = listOf(
                StrengthExerciseBlock(
                    id = "bench",
                    order = 1,
                    exerciseId = "barbell-bench-press",
                    target = StrengthExerciseTarget(
                        weight = WeightValue(60.0, WeightUnit.KG),
                        repTarget = RepTarget.Range(8, 12),
                        restAfterSetSec = 90
                    ),
                    sets = listOf(
                        StrengthSetPlan(
                            id = "bench-warmup",
                            order = 1,
                            kind = StrengthSetKind.WARMUP,
                            targetWeight = WeightValue(30.0, WeightUnit.KG),
                            repTarget = RepTarget.Fixed(10),
                            restAfterSec = 60
                        ),
                        StrengthSetPlan(
                            id = "bench-working",
                            order = 2,
                            kind = StrengthSetKind.WORKING,
                            targetWeight = WeightValue(60.0, WeightUnit.KG),
                            repTarget = RepTarget.Range(8, 12),
                            restAfterSec = 90
                        )
                    )
                )
            ),
            createdAt = "2026-06-13T08:00:00Z",
            updatedAt = "2026-06-13T08:02:00Z"
        )
    }

    private fun followAlongPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = "follow-plan",
            mode = WorkoutMode.FOLLOW_ALONG,
            title = "跟练 preset",
            blocks = listOf(
                TimedCircuitBlock(
                    id = "follow-circuit",
                    order = 1,
                    rounds = 1,
                    items = listOf(
                        TimedExerciseItem(
                            id = "follow-item",
                            exerciseId = "jumping-jacks",
                            workDurationSec = 30,
                            autoAdvance = true
                        )
                    )
                )
            ),
            followAlong = FollowAlongPlanMeta(
                preset = true,
                coverMediaId = "cover-basic"
            ),
            createdAt = "2026-06-13T08:00:00Z",
            updatedAt = "2026-06-13T08:03:00Z"
        )
    }
}
