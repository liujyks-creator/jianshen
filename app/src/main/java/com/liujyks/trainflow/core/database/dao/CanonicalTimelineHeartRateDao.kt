package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
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

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun canonicalGraphRows(sessionId: String): CanonicalSessionGraphRows?

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
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val phases: List<WorkoutPhaseIntervalEntity>,
    @Relation(
        entity = HeartRateRecordingEntity::class,
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val recordings: List<HeartRateRecordingWithRows>
)

data class HeartRateRecordingWithRows(
    @Embedded val recording: HeartRateRecordingEntity,
    @Relation(
        parentColumn = "recording_id",
        entityColumn = "recording_id"
    )
    val acquisitions: List<HeartRateAcquisitionIntervalEntity>,
    @Relation(
        parentColumn = "recording_id",
        entityColumn = "recording_id"
    )
    val samples: List<HeartRateSampleEntity>,
    @Relation(
        parentColumn = "recording_id",
        entityColumn = "recording_id"
    )
    val snapshots: List<HeartRateAnalysisSnapshotEntity>
)
