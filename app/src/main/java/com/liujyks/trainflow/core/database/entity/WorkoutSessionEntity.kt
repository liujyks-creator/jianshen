package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["plan_id"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String? = null,
    val mode: String,
    val status: String,
    @ColumnInfo(name = "plan_snapshot_json") val planSnapshotJson: String,
    @ColumnInfo(name = "started_at") val startedAt: String? = null,
    @ColumnInfo(name = "ended_at") val endedAt: String? = null,
    @ColumnInfo(name = "total_elapsed_sec") val totalElapsedSec: Int? = null,
    @ColumnInfo(name = "effective_elapsed_sec") val effectiveElapsedSec: Int? = null,
    @ColumnInfo(name = "paused_elapsed_sec") val pausedElapsedSec: Int? = null
)

@Entity(
    tableName = "session_step_records",
    indices = [Index(value = ["session_id"])]
)
data class SessionStepRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "step_id") val stepId: String,
    val kind: String,
    @ColumnInfo(name = "block_id") val blockId: String? = null,
    @ColumnInfo(name = "item_id") val itemId: String? = null,
    @ColumnInfo(name = "set_plan_id") val setPlanId: String? = null,
    @ColumnInfo(name = "exercise_id") val exerciseId: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: String,
    @ColumnInfo(name = "ended_at") val endedAt: String? = null,
    val skipped: Boolean = false,
    @ColumnInfo(name = "actual_duration_sec") val actualDurationSec: Int? = null,
    @ColumnInfo(name = "planned_duration_sec") val plannedDurationSec: Int? = null
)

@Entity(
    tableName = "strength_set_records",
    indices = [Index(value = ["session_id"]), Index(value = ["exercise_id"])]
)
data class StrengthSetRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "source_set_plan_id") val sourceSetPlanId: String? = null,
    @ColumnInfo(name = "set_order") val setOrder: Int,
    @ColumnInfo(name = "set_kind") val setKind: String,
    val side: String? = null,
    @ColumnInfo(name = "planned_json") val plannedJson: String? = null,
    @ColumnInfo(name = "actual_json") val actualJson: String? = null,
    @ColumnInfo(name = "active_duration_sec") val activeDurationSec: Int? = null,
    @ColumnInfo(name = "actual_rest_after_sec") val actualRestAfterSec: Int? = null,
    val effort: String? = null,
    @ColumnInfo(name = "substituted_from_exercise_id") val substitutedFromExerciseId: String? = null,
    val notes: String? = null
)
