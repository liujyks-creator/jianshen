package com.liujyks.trainflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "workout_phase_intervals",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["session_id", "sequence"],
            unique = true,
            name = "index_workout_phase_intervals_session_sequence"
        ),
        Index(
            value = ["session_id", "open_marker"],
            unique = true,
            name = "index_workout_phase_intervals_session_open_marker"
        ),
        Index(
            value = ["session_id", "start_offset_ms"],
            name = "index_workout_phase_intervals_session_start"
        )
    ]
)
data class WorkoutPhaseIntervalEntity(
    val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val sequence: Int,
    @ColumnInfo(name = "start_offset_ms") val startOffsetMs: Long,
    @ColumnInfo(name = "end_offset_ms") val endOffsetMs: Long?,
    @ColumnInfo(name = "start_mutation_sequence") val startMutationSequence: Long,
    @ColumnInfo(name = "end_mutation_sequence") val endMutationSequence: Long?,
    @ColumnInfo(name = "open_marker") val openMarker: Int?,
    @ColumnInfo(name = "phase_kind") val phaseKind: String,
    @ColumnInfo(name = "phase_identity_json") val phaseIdentityJson: String
)

@Entity(
    tableName = "heart_rate_recordings",
    primaryKeys = ["recording_id"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["session_id"],
            unique = true,
            name = "index_heart_rate_recordings_session_id"
        )
    ]
)
data class HeartRateRecordingEntity(
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val status: String,
    @ColumnInfo(name = "started_offset_ms") val startedOffsetMs: Long,
    @ColumnInfo(name = "started_mutation_sequence") val startedMutationSequence: Long,
    @ColumnInfo(name = "ended_offset_ms") val endedOffsetMs: Long?,
    @ColumnInfo(name = "ended_mutation_sequence") val endedMutationSequence: Long?,
    @ColumnInfo(name = "source_contract_version") val sourceContractVersion: Int,
    @ColumnInfo(name = "source_kind") val sourceKind: String,
    @ColumnInfo(name = "acquisition_contract_version") val acquisitionContractVersion: Int,
    @ColumnInfo(name = "parameter_snapshot_version") val parameterSnapshotVersion: Int,
    val age: Int? = null,
    @ColumnInfo(name = "personal_max_bpm") val personalMaxBpm: Int? = null,
    @ColumnInfo(name = "effective_max_bpm") val effectiveMaxBpm: Int? = null,
    @ColumnInfo(name = "effective_max_source") val effectiveMaxSource: String? = null,
    @ColumnInfo(name = "alert_threshold_bpm") val alertThresholdBpm: Int? = null,
    @ColumnInfo(name = "zone_snapshot_json") val zoneSnapshotJson: String? = null,
    @ColumnInfo(name = "original_analysis_version") val originalAnalysisVersion: Int? = null
)

@Entity(
    tableName = "heart_rate_acquisition_intervals",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = HeartRateRecordingEntity::class,
            parentColumns = ["recording_id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["recording_id", "sequence"],
            unique = true,
            name = "index_hr_acquisition_recording_sequence"
        ),
        Index(
            value = ["recording_id", "open_marker"],
            unique = true,
            name = "index_hr_acquisition_recording_open_marker"
        ),
        Index(
            value = ["recording_id", "start_offset_ms"],
            name = "index_hr_acquisition_recording_start"
        )
    ]
)
data class HeartRateAcquisitionIntervalEntity(
    val id: String,
    @ColumnInfo(name = "recording_id") val recordingId: String,
    val sequence: Int,
    @ColumnInfo(name = "start_offset_ms") val startOffsetMs: Long,
    @ColumnInfo(name = "end_offset_ms") val endOffsetMs: Long?,
    @ColumnInfo(name = "start_mutation_sequence") val startMutationSequence: Long,
    @ColumnInfo(name = "end_mutation_sequence") val endMutationSequence: Long?,
    @ColumnInfo(name = "open_marker") val openMarker: Int?,
    @ColumnInfo(name = "recording_intent") val recordingIntent: String,
    @ColumnInfo(name = "intent_reason") val intentReason: String?,
    @ColumnInfo(name = "device_state") val deviceState: String,
    @ColumnInfo(name = "device_reason") val deviceReason: String?
)

@Entity(
    tableName = "heart_rate_samples",
    primaryKeys = ["recording_id", "sample_sequence"],
    foreignKeys = [
        ForeignKey(
            entity = HeartRateRecordingEntity::class,
            parentColumns = ["recording_id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["recording_id", "offset_ms", "mutation_sequence", "sample_sequence"],
            name = "index_hr_samples_canonical_order"
        )
    ]
)
data class HeartRateSampleEntity(
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "sample_sequence") val sampleSequence: Long,
    @ColumnInfo(name = "offset_ms") val offsetMs: Long,
    @ColumnInfo(name = "mutation_sequence") val mutationSequence: Long,
    val bpm: Int
)

@Entity(
    tableName = "heart_rate_analysis_snapshots",
    primaryKeys = ["recording_id", "analysis_version"],
    foreignKeys = [
        ForeignKey(
            entity = HeartRateRecordingEntity::class,
            parentColumns = ["recording_id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HeartRateAnalysisSnapshotEntity(
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "analysis_version") val analysisVersion: Int,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "input_last_mutation_sequence") val inputLastMutationSequence: Long,
    @ColumnInfo(name = "sample_status") val sampleStatus: String,
    @ColumnInfo(name = "coverage_status") val coverageStatus: String,
    @ColumnInfo(name = "zone_status") val zoneStatus: String,
    @ColumnInfo(name = "canonical_sample_count") val canonicalSampleCount: Long,
    @ColumnInfo(name = "primary_point_sample_count") val primaryPointSampleCount: Long,
    @ColumnInfo(name = "eligible_duration_ms") val eligibleDurationMs: Long?,
    @ColumnInfo(name = "covered_duration_ms") val coveredDurationMs: Long?,
    @ColumnInfo(name = "coverage_basis_points") val coverageBasisPoints: Int?,
    @ColumnInfo(name = "weighted_bpm_ms") val weightedBpmMs: Long?,
    @ColumnInfo(name = "observed_avg_bpm") val observedAvgBpm: Int?,
    @ColumnInfo(name = "observed_max_bpm") val observedMaxBpm: Int?,
    @ColumnInfo(name = "highest_offset_ms") val highestOffsetMs: Long?,
    @ColumnInfo(name = "highest_mutation_sequence") val highestMutationSequence: Long?,
    @ColumnInfo(name = "highest_sample_sequence") val highestSampleSequence: Long?,
    @ColumnInfo(name = "analysis_config_json") val analysisConfigJson: String,
    @ColumnInfo(name = "zone_durations_json") val zoneDurationsJson: String?,
    @ColumnInfo(name = "phase_aggregates_json") val phaseAggregatesJson: String,
    @ColumnInfo(name = "duration_breakdown_json") val durationBreakdownJson: String,
    @ColumnInfo(name = "quality_reasons_json") val qualityReasonsJson: String
)
