package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "blocks_json") val blocksJson: String,
    @ColumnInfo(name = "reminder_json") val reminderJson: String? = null,
    @ColumnInfo(name = "preferences_json") val preferencesJson: String? = null,
    @ColumnInfo(name = "follow_along_json") val followAlongJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
