package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity

@Dao
interface CanonicalTimelineHeartRateDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhaseInterval(interval: WorkoutPhaseIntervalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecording(recording: HeartRateRecordingEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAcquisitionInterval(interval: HeartRateAcquisitionIntervalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSample(sample: HeartRateSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnalysisSnapshot(snapshot: HeartRateAnalysisSnapshotEntity): Long

    @Query(
        """
        UPDATE workout_phase_intervals
        SET end_offset_ms = :endOffsetMs,
            end_mutation_sequence = :endMutationSequence,
            open_marker = NULL
        WHERE id = :expectedOpenRowId
          AND session_id = :sessionId
          AND end_offset_ms IS NULL
          AND end_mutation_sequence IS NULL
          AND open_marker = 1
          AND EXISTS (
              SELECT 1 FROM workout_sessions
              WHERE id = :sessionId
                AND status = 'abandoned'
                AND timeline_version = :reconciliationContractVersion
                AND last_durable_offset_ms = :endOffsetMs
                AND last_mutation_sequence = :endMutationSequence
                AND trusted_end_offset_ms = :endOffsetMs
                AND terminal_reason = 'process_interrupted'
          )
        """
    )
    suspend fun closeOpenPhaseForProcessInterruption(
        sessionId: String,
        expectedOpenRowId: String,
        endOffsetMs: Long,
        endMutationSequence: Long,
        reconciliationContractVersion: Int
    ): Int

    @Query(
        """
        UPDATE workout_phase_intervals
        SET end_offset_ms = :endOffsetMs,
            end_mutation_sequence = :endMutationSequence,
            open_marker = NULL
        WHERE id = :expectedOpenRowId
          AND session_id = :sessionId
          AND end_offset_ms IS NULL
          AND end_mutation_sequence IS NULL
          AND open_marker = 1
          AND EXISTS (
              SELECT 1 FROM workout_sessions
              WHERE id = :sessionId
                AND status = :expectedStatus
                AND timeline_version = 1
                AND last_durable_offset_ms = :endOffsetMs
                AND last_mutation_sequence = :endMutationSequence
                AND trusted_end_offset_ms IS NULL
                AND terminal_reason IS NULL
          )
        """
    )
    suspend fun closeOpenPhase(
        sessionId: String,
        expectedStatus: String,
        expectedOpenRowId: String,
        endOffsetMs: Long,
        endMutationSequence: Long
    ): Int

    @Query(
        """
        UPDATE heart_rate_acquisition_intervals
        SET end_offset_ms = :endOffsetMs,
            end_mutation_sequence = :endMutationSequence,
            open_marker = NULL
        WHERE id = :expectedOpenRowId
          AND recording_id = :recordingId
          AND end_offset_ms IS NULL
          AND end_mutation_sequence IS NULL
          AND open_marker = 1
          AND EXISTS (
              SELECT 1
              FROM heart_rate_recordings AS recording
              JOIN workout_sessions AS session ON session.id = recording.session_id
              WHERE recording.recording_id = :recordingId
                AND recording.status = 'active'
                AND session.status = :expectedSessionStatus
                AND session.timeline_version = 1
                AND session.last_durable_offset_ms = :endOffsetMs
                AND session.last_mutation_sequence = :endMutationSequence
                AND session.trusted_end_offset_ms IS NULL
                AND session.terminal_reason IS NULL
          )
        """
    )
    suspend fun closeOpenAcquisition(
        recordingId: String,
        expectedSessionStatus: String,
        expectedOpenRowId: String,
        endOffsetMs: Long,
        endMutationSequence: Long
    ): Int

    @Query(
        """
        SELECT * FROM workout_phase_intervals
        WHERE session_id = :sessionId
        ORDER BY sequence
        """
    )
    suspend fun phaseIntervals(sessionId: String): List<WorkoutPhaseIntervalEntity>

    @Query(
        """
        SELECT * FROM heart_rate_samples
        WHERE recording_id = :recordingId
        ORDER BY offset_ms, mutation_sequence, sample_sequence
        """
    )
    suspend fun samplesInCanonicalOrder(recordingId: String): List<HeartRateSampleEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun sessionById(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM heart_rate_recordings WHERE session_id = :sessionId ORDER BY recording_id")
    suspend fun recordingsForSession(sessionId: String): List<HeartRateRecordingEntity>

    @Query(
        """
        SELECT * FROM heart_rate_acquisition_intervals
        WHERE recording_id = :recordingId
        ORDER BY sequence
        """
    )
    suspend fun acquisitionsInSequence(recordingId: String): List<HeartRateAcquisitionIntervalEntity>

    @Query(
        """
        SELECT * FROM heart_rate_analysis_snapshots
        WHERE recording_id = :recordingId
        ORDER BY analysis_version
        """
    )
    suspend fun snapshotsInVersionOrder(recordingId: String): List<HeartRateAnalysisSnapshotEntity>

    @Transaction
    suspend fun canonicalGraphRows(sessionId: String): CanonicalSessionGraphRows? {
        val session = sessionById(sessionId) ?: return null
        val recordings = recordingsForSession(sessionId).map { recording ->
            HeartRateRecordingWithRows(
                recording = recording,
                acquisitions = acquisitionsInSequence(recording.recordingId),
                samples = samplesInCanonicalOrder(recording.recordingId),
                snapshots = snapshotsInVersionOrder(recording.recordingId)
            )
        }
        return CanonicalSessionGraphRows(
            session = session,
            phases = phaseIntervals(sessionId),
            recordings = recordings
        )
    }

    @Query("SELECT COUNT(*) FROM workout_phase_intervals")
    suspend fun phaseIntervalCount(): Int

    @Query("SELECT COUNT(*) FROM heart_rate_recordings")
    suspend fun recordingCount(): Int

    @Query("SELECT COUNT(*) FROM heart_rate_acquisition_intervals")
    suspend fun acquisitionIntervalCount(): Int

    @Query("SELECT COUNT(*) FROM heart_rate_samples")
    suspend fun sampleCount(): Int

    @Query("SELECT COUNT(*) FROM heart_rate_analysis_snapshots")
    suspend fun analysisSnapshotCount(): Int
}

data class CanonicalSessionGraphRows(
    val session: WorkoutSessionEntity,
    val phases: List<WorkoutPhaseIntervalEntity>,
    val recordings: List<HeartRateRecordingWithRows>
)

data class HeartRateRecordingWithRows(
    val recording: HeartRateRecordingEntity,
    val acquisitions: List<HeartRateAcquisitionIntervalEntity>,
    val samples: List<HeartRateSampleEntity>,
    val snapshots: List<HeartRateAnalysisSnapshotEntity>
)
