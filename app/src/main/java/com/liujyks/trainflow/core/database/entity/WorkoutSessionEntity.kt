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
    @ColumnInfo(name = "ended_at") val endedAt: String? = null
)

@Entity(
    tableName = "session_step_records",
    indices = [Index(value = ["session_id"])]
)
data class SessionStepRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val kind: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: String,
    @ColumnInfo(name = "ended_at") val endedAt: String? = null,
    val skipped: Boolean = false,
    @ColumnInfo(name = "actual_duration_sec") val actualDurationSec: Int? = null
)

@Entity(
    tableName = "strength_set_records",
    indices = [Index(value = ["session_id"]), Index(value = ["exercise_id"])]
)
data class StrengthSetRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "set_order") val setOrder: Int,
    @ColumnInfo(name = "set_kind") val setKind: String,
    @ColumnInfo(name = "planned_json") val plannedJson: String? = null,
    @ColumnInfo(name = "actual_json") val actualJson: String? = null,
    val effort: String? = null
)
