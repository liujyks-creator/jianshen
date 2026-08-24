package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.model.WorkoutMode

data class WorkoutPlanSnapshotStorageV1(
    val mode: WorkoutMode,
    val persistedJson: String
)

sealed interface PlanSnapshotStorageV1ValidationResult {
    data class Valid(
        val storage: WorkoutPlanSnapshotStorageV1
    ) : PlanSnapshotStorageV1ValidationResult

    data class Invalid(
        val code: String = "invalid_plan_snapshot_storage_v1"
    ) : PlanSnapshotStorageV1ValidationResult

    data class UnsupportedVersion(
        val actualVersion: String
    ) : PlanSnapshotStorageV1ValidationResult
}
