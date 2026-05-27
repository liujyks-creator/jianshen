package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface WorkoutPlanDao {
    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun count(): Int
}
