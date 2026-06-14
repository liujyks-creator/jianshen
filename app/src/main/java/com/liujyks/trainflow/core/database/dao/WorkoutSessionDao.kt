package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun sessionCount(): Int

    @Query("SELECT COUNT(*) FROM session_step_records")
    suspend fun stepRecordCount(): Int

    @Query("SELECT COUNT(*) FROM strength_set_records")
    suspend fun strengthSetRecordCount(): Int

    @Query("SELECT COUNT(*) FROM timed_rest_extension_records")
    suspend fun timedRestExtensionRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepRecords(records: List<SessionStepRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStrengthSetRecords(records: List<StrengthSetRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimedRestExtensionRecords(records: List<TimedRestExtensionRecordEntity>)

    @Query("DELETE FROM session_step_records WHERE session_id = :sessionId")
    suspend fun deleteStepRecordsForSession(sessionId: String)

    @Query("DELETE FROM timed_rest_extension_records WHERE session_id = :sessionId")
    suspend fun deleteTimedRestExtensionRecordsForSession(sessionId: String)

    @Query("DELETE FROM strength_set_records WHERE session_id = :sessionId")
    suspend fun deleteStrengthSetRecordsForSession(sessionId: String)

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY COALESCE(ended_at, started_at, '') DESC")
    fun observeSessionsWithRecords(): Flow<List<WorkoutSessionWithRecords>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY COALESCE(ended_at, started_at, '') DESC")
    suspend fun getSessionsWithRecords(): List<WorkoutSessionWithRecords>
}

data class WorkoutSessionWithRecords(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val stepRecords: List<SessionStepRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val timedRestExtensionRecords: List<TimedRestExtensionRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val strengthSetRecords: List<StrengthSetRecordEntity>
)
