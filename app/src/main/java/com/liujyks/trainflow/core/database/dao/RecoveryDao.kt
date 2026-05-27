package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface RecoveryDao {
    @Query("SELECT COUNT(*) FROM recovery_areas")
    suspend fun areaCount(): Int

    @Query("SELECT COUNT(*) FROM recovery_recommendations")
    suspend fun recommendationCount(): Int
}
