package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session_merge_groups")
data class WorkoutSessionMergeGroupEntity(
    @PrimaryKey @ColumnInfo(name = "group_id") val groupId: String
)

@Entity(
    tableName = "workout_session_merge_members",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"],
            childColumns = ["session_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = WorkoutSessionMergeGroupEntity::class, parentColumns = ["group_id"],
            childColumns = ["group_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["group_id"])]
)
data class WorkoutSessionMergeMemberEntity(
    @PrimaryKey @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "group_id") val groupId: String
)
