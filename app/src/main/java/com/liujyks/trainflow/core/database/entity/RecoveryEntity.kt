package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recovery_areas")
data class RecoveryAreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "body_region") val bodyRegion: String,
    val summary: String,
    @ColumnInfo(name = "media_json") val mediaJson: String? = null,
    @ColumnInfo(name = "caution_text") val cautionText: String? = null
)

@Entity(
    tableName = "recovery_recommendations",
    indices = [Index(value = ["session_id"])]
)
data class RecoveryRecommendationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "trained_muscle_ids_json") val trainedMuscleIdsJson: String,
    @ColumnInfo(name = "area_ids_json") val areaIdsJson: String,
    @ColumnInfo(name = "content_ids_json") val contentIdsJson: String? = null
)
