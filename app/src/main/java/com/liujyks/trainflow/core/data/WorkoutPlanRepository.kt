package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WorkoutPlanRepository(
    database: TrainFlowDatabase
) {
    private val dao = database.workoutPlanDao()

    val plans: Flow<List<WorkoutPlan>> = dao.observePlans()
        .map { rows -> rows.map { row -> row.toDomain() } }

    suspend fun upsertPlan(plan: WorkoutPlan) {
        dao.upsertPlan(plan.toEntity())
    }

    suspend fun deletePlan(planId: String) {
        dao.deletePlan(planId)
    }

    suspend fun getPlans(): List<WorkoutPlan> {
        return dao.getPlans().map { row -> row.toDomain() }
    }

    suspend fun getPlan(planId: String): WorkoutPlan? {
        return dao.getPlan(planId)?.toDomain()
    }
}

private fun WorkoutPlan.toEntity(): WorkoutPlanEntity {
    return WorkoutPlanEntity(
        id = id,
        mode = mode.contractValue,
        title = title,
        description = description,
        blocksJson = blocks.toPlanBlocksStorageJson(),
        reminderJson = reminder?.toStorageJson(),
        preferencesJson = preferences?.toStorageJson(),
        followAlongJson = followAlong?.toStorageJson(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun WorkoutPlanEntity.toDomain(): WorkoutPlan {
    return WorkoutPlan(
        id = id,
        mode = workoutModeFrom(mode),
        title = title,
        description = description,
        blocks = blocksJson.toPlanBlocksStorage(),
        reminder = reminderJson?.toPlanReminderStorage(),
        preferences = preferencesJson?.toPlanPreferencesStorage(),
        followAlong = followAlongJson?.toFollowAlongMetaStorage(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun workoutModeFrom(value: String): WorkoutMode {
    return WorkoutMode.entries.firstOrNull { mode -> mode.contractValue == value } ?: WorkoutMode.TIMED
}
