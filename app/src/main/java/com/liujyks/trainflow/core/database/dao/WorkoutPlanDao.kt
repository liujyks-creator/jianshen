package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY updated_at DESC, created_at DESC")
    fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans ORDER BY updated_at DESC, created_at DESC")
    suspend fun getPlans(): List<WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    suspend fun getPlan(planId: String): WorkoutPlanEntity?

    @Upsert
    suspend fun upsertPlan(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String)

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun count(): Int
}
