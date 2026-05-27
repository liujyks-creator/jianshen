package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface WorkoutSessionDao {
    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun sessionCount(): Int

    @Query("SELECT COUNT(*) FROM session_step_records")
    suspend fun stepRecordCount(): Int

    @Query("SELECT COUNT(*) FROM strength_set_records")
    suspend fun strengthSetRecordCount(): Int
}
