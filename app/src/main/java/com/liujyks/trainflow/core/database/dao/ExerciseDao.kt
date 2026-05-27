package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}
