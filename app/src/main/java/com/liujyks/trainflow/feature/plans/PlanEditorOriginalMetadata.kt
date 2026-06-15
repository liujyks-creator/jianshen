package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.PlanReminder
import com.liujyks.trainflow.core.model.WorkoutPlan

internal data class OriginalPlanMetadata(
    val id: String,
    val createdAt: String,
    val reminder: PlanReminder? = null,
    val preferences: PlanPreferences? = null
)

internal fun WorkoutPlan.toOriginalPlanMetadata(): OriginalPlanMetadata {
    return OriginalPlanMetadata(
        id = id,
        createdAt = createdAt,
        reminder = reminder,
        preferences = preferences
    )
}
