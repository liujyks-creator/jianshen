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
    suspend fun insertPhaseInterval(interval: WorkoutPhaseIntervalEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecording(recording: HeartRateRecordingEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAcquisitionInterval(interval: HeartRateAcquisitionIntervalEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSample(sample: HeartRateSampleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnalysisSnapshot(snapshot: HeartRateAnalysisSnapshotEntity)

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
