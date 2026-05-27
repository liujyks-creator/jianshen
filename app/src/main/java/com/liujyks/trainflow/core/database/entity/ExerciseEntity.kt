package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    @ColumnInfo(name = "equipment_json") val equipmentJson: String,
    val difficulty: String,
    @ColumnInfo(name = "capabilities_json") val capabilitiesJson: String,
    @ColumnInfo(name = "content_status") val contentStatus: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null
)
