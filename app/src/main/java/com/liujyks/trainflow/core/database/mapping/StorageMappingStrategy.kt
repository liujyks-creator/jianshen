package com.liujyks.trainflow.core.database.mapping

/**
 * Documents the E0.4 storage mapping boundary.
 *
 * Room entities intentionally stay separate from core model classes. Early
 * polymorphic plan/session structures are stored as JSON columns until later
 * stories need richer query paths or repository mapping.
 */
object StorageMappingStrategy {
    val jsonBackedColumns = setOf(
        "exercises.equipment_json",
        "exercises.capabilities_json",
        "workout_plans.blocks_json",
        "workout_plans.reminder_json",
        "workout_plans.preferences_json",
        "workout_sessions.plan_snapshot_json",
        "strength_set_records.planned_json",
        "strength_set_records.actual_json",
        "recovery_recommendations.trained_muscle_ids_json",
        "recovery_recommendations.area_ids_json"
    )
}
