package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.model.WorkoutMode

internal sealed interface LegacyUnversionedPlanSnapshotReadResult {
    data class Valid(
        val persistedJson: String,
        val mode: WorkoutMode,
        val root: CanonicalJsonValue.Obj
    ) : LegacyUnversionedPlanSnapshotReadResult

    data class Invalid(
        val code: String = "invalid_legacy_unversioned_plan_snapshot"
    ) : LegacyUnversionedPlanSnapshotReadResult
}

internal object LegacyUnversionedPlanSnapshotReader {
    fun read(
        persistedJson: String,
        sessionMode: WorkoutMode
    ): LegacyUnversionedPlanSnapshotReadResult {
        val root = parseCanonicalJson(persistedJson) as? CanonicalJsonValue.Obj
            ?: return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        val fields = root.fields
        if (!fields.keys.containsAll(setOf("title", "mode", "blocks")) ||
            fields.keys.any { it !in setOf("title", "mode", "blocks", "planId", "preferences", "followAlong") } ||
            fields["title"] !is CanonicalJsonValue.Str ||
            (fields["mode"] as? CanonicalJsonValue.Str)?.value != sessionMode.contractValue
        ) {
            return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        }
        when (fields["planId"]) {
            null, CanonicalJsonValue.Null, is CanonicalJsonValue.Str -> Unit
            else -> return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        }
        val blocks = fields["blocks"] as? CanonicalJsonValue.Arr
            ?: return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        for (block in blocks.values) {
            if (PlanSnapshotStorageV1Validator.canonicalBlock(block) == null) {
                return LegacyUnversionedPlanSnapshotReadResult.Invalid()
            }
        }
        when (val preferences = fields["preferences"]) {
            null, CanonicalJsonValue.Null -> Unit
            is CanonicalJsonValue.Obj -> {
                if (PlanSnapshotStorageV1Validator.canonicalPreferences(preferences) == null) {
                    return LegacyUnversionedPlanSnapshotReadResult.Invalid()
                }
            }
            else -> return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        }
        when (val followAlong = fields["followAlong"]) {
            null, CanonicalJsonValue.Null -> Unit
            is CanonicalJsonValue.Obj -> {
                if (PlanSnapshotStorageV1Validator.canonicalFollowAlong(followAlong) == null) {
                    return LegacyUnversionedPlanSnapshotReadResult.Invalid()
                }
            }
            else -> return LegacyUnversionedPlanSnapshotReadResult.Invalid()
        }
        return LegacyUnversionedPlanSnapshotReadResult.Valid(persistedJson, sessionMode, root)
    }
}
