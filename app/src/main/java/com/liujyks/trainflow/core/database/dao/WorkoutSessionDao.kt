package com.liujyks.trainflow.core.database.dao

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("""
        SELECT id, plan_id, mode, status, started_at, ended_at, total_elapsed_sec,
            effective_elapsed_sec, paused_elapsed_sec, timeline_version, trusted_end_offset_ms,
            terminal_reason, start_local_date, start_zone_id, start_utc_offset_seconds,
            time_metadata_source_contract_version, last_durable_offset_ms
        FROM workout_sessions ORDER BY COALESCE(ended_at, started_at, '') DESC, id ASC
    """)
    suspend fun sessionHeaders(): List<WorkoutSessionHeaderRow>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun sessionCount(): Int

    @Query("SELECT COUNT(*) FROM session_step_records")
    suspend fun stepRecordCount(): Int

    @Query("SELECT COUNT(*) FROM strength_set_records")
    suspend fun strengthSetRecordCount(): Int

    @Query("SELECT COUNT(*) FROM timed_rest_extension_records")
    suspend fun timedRestExtensionRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY id")
    suspend fun sessionsForRecorderGate(): List<WorkoutSessionEntity>

    @Query(
        """
        UPDATE workout_sessions
        SET status = 'abandoned',
            trusted_end_offset_ms = :expectedOffsetMs,
            terminal_reason = 'process_interrupted',
            last_mutation_sequence = :reconciledMutationSequence
        WHERE id = :sessionId
          AND status = :expectedStatus
          AND timeline_version = :reconciliationContractVersion
          AND last_durable_offset_ms = :expectedOffsetMs
          AND last_mutation_sequence = :expectedMutationSequence
          AND trusted_end_offset_ms IS NULL
          AND terminal_reason IS NULL
          AND display_metadata_contract_version = 1
          AND session_display_metadata_json IS NOT NULL
        """
    )
    suspend fun reconcileProcessInterrupted(
        sessionId: String,
        expectedStatus: String,
        expectedOffsetMs: Long,
        expectedMutationSequence: Long,
        reconciledMutationSequence: Long,
        reconciliationContractVersion: Int
    ): Int

    @Query(
        """
        UPDATE workout_sessions
        SET last_durable_offset_ms = :nextOffsetMs,
            last_mutation_sequence = :nextMutationSequence,
            status = :nextStatus,
            session_display_metadata_json = CASE WHEN :nextDisplayMetadataJson IS NULL
                THEN session_display_metadata_json ELSE :nextDisplayMetadataJson END
        WHERE id = :sessionId
          AND status = :expectedStatus
          AND timeline_version = 1
          AND last_durable_offset_ms = :expectedOffsetMs
          AND last_mutation_sequence = :expectedMutationSequence
          AND trusted_end_offset_ms IS NULL
          AND terminal_reason IS NULL
          AND EXISTS (
              SELECT 1 FROM workout_phase_intervals
              WHERE id = :expectedOpenPhaseId
                AND session_id = :sessionId
                AND end_offset_ms IS NULL
                AND end_mutation_sequence IS NULL
                AND open_marker = 1
          )
          AND (
              (:expectedRecordingId IS NULL AND :expectedOpenAcquisitionId IS NULL
               AND NOT EXISTS (SELECT 1 FROM heart_rate_recordings WHERE session_id = :sessionId))
              OR EXISTS (
                  SELECT 1
                  FROM heart_rate_recordings AS recording
                  JOIN heart_rate_acquisition_intervals AS acquisition
                    ON acquisition.recording_id = recording.recording_id
                  WHERE recording.recording_id = :expectedRecordingId
                    AND recording.session_id = :sessionId
                    AND recording.status = 'active'
                    AND acquisition.id = :expectedOpenAcquisitionId
                    AND acquisition.end_offset_ms IS NULL
                    AND acquisition.end_mutation_sequence IS NULL
                    AND acquisition.open_marker = 1
              )
          )
        """
    )
    suspend fun advanceCanonicalHeader(
        sessionId: String,
        expectedStatus: String,
        expectedOffsetMs: Long,
        expectedMutationSequence: Long,
        expectedOpenPhaseId: String,
        expectedRecordingId: String?,
        expectedOpenAcquisitionId: String?,
        nextOffsetMs: Long,
        nextMutationSequence: Long,
        nextStatus: String = expectedStatus,
        nextDisplayMetadataJson: String? = null
    ): Int

    @Query(
        """
        UPDATE workout_sessions
        SET last_durable_offset_ms = :nextOffsetMs,
            last_mutation_sequence = :nextMutationSequence,
            session_display_metadata_json = :nextDisplayMetadataJson
        WHERE id = :sessionId
          AND status = :expectedStatus
          AND timeline_version = 1
          AND last_durable_offset_ms = :expectedOffsetMs
          AND last_mutation_sequence = :expectedMutationSequence
          AND trusted_end_offset_ms IS NULL
          AND terminal_reason IS NULL
          AND display_metadata_contract_version = 1
          AND EXISTS (
              SELECT 1 FROM workout_phase_intervals
              WHERE id = :expectedOpenPhaseId
                AND session_id = :sessionId
                AND end_offset_ms IS NULL
                AND end_mutation_sequence IS NULL
                AND open_marker = 1
          )
          AND (
              (:expectedRecordingId IS NULL AND :expectedOpenAcquisitionId IS NULL)
              OR EXISTS (
                  SELECT 1
                  FROM heart_rate_recordings AS recording
                  JOIN heart_rate_acquisition_intervals AS acquisition
                    ON acquisition.recording_id = recording.recording_id
                  WHERE recording.recording_id = :expectedRecordingId
                    AND recording.session_id = :sessionId
                    AND recording.status = 'active'
                    AND acquisition.id = :expectedOpenAcquisitionId
                    AND acquisition.end_offset_ms IS NULL
                    AND acquisition.end_mutation_sequence IS NULL
                    AND acquisition.open_marker = 1
              )
          )
        """
    )
    suspend fun appendCanonicalDisplayMetadata(
        sessionId: String,
        expectedStatus: String,
        expectedOffsetMs: Long,
        expectedMutationSequence: Long,
        expectedOpenPhaseId: String,
        expectedRecordingId: String?,
        expectedOpenAcquisitionId: String?,
        nextOffsetMs: Long,
        nextMutationSequence: Long,
        nextDisplayMetadataJson: String
    ): Int

    @Query(
        """
        UPDATE workout_sessions
        SET status = :terminalStatus,
            last_durable_offset_ms = :finalOffsetMs,
            last_mutation_sequence = :finalMutationSequence,
            trusted_end_offset_ms = :finalOffsetMs,
            terminal_reason = :terminalReason
        WHERE id = :sessionId
          AND status = :expectedStatus
          AND timeline_version = 1
          AND last_durable_offset_ms = :expectedOffsetMs
          AND last_mutation_sequence = :expectedMutationSequence
          AND trusted_end_offset_ms IS NULL
          AND terminal_reason IS NULL
          AND display_metadata_contract_version = 1
          AND session_display_metadata_json IS NOT NULL
          AND EXISTS (
              SELECT 1
              FROM workout_phase_intervals AS phase
              JOIN heart_rate_recordings AS recording
                ON recording.session_id = :sessionId
              JOIN heart_rate_acquisition_intervals AS acquisition
                ON acquisition.recording_id = recording.recording_id
              WHERE phase.id = :expectedClosedPhaseId
                AND phase.session_id = :sessionId
                AND phase.end_offset_ms = :finalOffsetMs
                AND phase.end_mutation_sequence = :finalMutationSequence
                AND phase.open_marker IS NULL
                AND recording.recording_id = :recordingId
                AND recording.status = 'terminal'
                AND recording.ended_offset_ms = :finalOffsetMs
                AND recording.ended_mutation_sequence = :finalMutationSequence
                AND recording.original_analysis_version IS NULL
                AND acquisition.id = :expectedClosedAcquisitionId
                AND acquisition.end_offset_ms = :finalOffsetMs
                AND acquisition.end_mutation_sequence = :finalMutationSequence
                AND acquisition.open_marker IS NULL
          )
        """
    )
    suspend fun finalizeTerminalizeSession(
        sessionId: String,
        recordingId: String,
        expectedStatus: String,
        expectedOffsetMs: Long,
        expectedMutationSequence: Long,
        expectedClosedPhaseId: String,
        expectedClosedAcquisitionId: String,
        finalOffsetMs: Long,
        finalMutationSequence: Long,
        terminalStatus: String,
        terminalReason: String
    ): Int

    @Query(
        """
        UPDATE workout_sessions
        SET ended_at = :endedAt,
            total_elapsed_sec = :totalElapsedSec,
            effective_elapsed_sec = :effectiveElapsedSec,
            paused_elapsed_sec = :pausedElapsedSec,
            session_display_metadata_json = :displayMetadataJson
        WHERE id = :sessionId AND timeline_version = 1
          AND status = :expectedStatus
          AND last_durable_offset_ms = :expectedOffsetMs
          AND last_mutation_sequence = :expectedMutationSequence
          AND trusted_end_offset_ms IS NULL AND terminal_reason IS NULL
          AND EXISTS (SELECT 1 FROM workout_phase_intervals
              WHERE id = :expectedOpenPhaseId AND session_id = :sessionId
                AND end_offset_ms IS NULL AND end_mutation_sequence IS NULL AND open_marker = 1)
          AND ((:recordingId IS NULL AND :expectedOpenAcquisitionId IS NULL
                AND NOT EXISTS (SELECT 1 FROM heart_rate_recordings WHERE session_id = :sessionId))
            OR EXISTS (SELECT 1 FROM heart_rate_recordings AS recording
                JOIN heart_rate_acquisition_intervals AS acquisition
                  ON acquisition.recording_id = recording.recording_id
                WHERE recording.session_id = :sessionId AND recording.recording_id = :recordingId
                  AND recording.status = 'active' AND recording.original_analysis_version IS NULL
                  AND acquisition.id = :expectedOpenAcquisitionId
                  AND acquisition.end_offset_ms IS NULL
                  AND acquisition.end_mutation_sequence IS NULL AND acquisition.open_marker = 1))
        """
    )
    suspend fun writeCanonicalExecutionHeader(
        sessionId: String,
        expectedStatus: String,
        expectedOffsetMs: Long,
        expectedMutationSequence: Long,
        expectedOpenPhaseId: String,
        recordingId: String?,
        expectedOpenAcquisitionId: String?,
        endedAt: String?,
        totalElapsedSec: Int?,
        effectiveElapsedSec: Int?,
        pausedElapsedSec: Int?,
        displayMetadataJson: String
    ): Int

    @Query(
        """
        UPDATE workout_sessions
        SET status = :terminalStatus, terminal_reason = :terminalReason,
            trusted_end_offset_ms = :finalOffsetMs
        WHERE id = :sessionId AND timeline_version = 1 AND status = :expectedStatus
          AND last_durable_offset_ms = :finalOffsetMs
          AND last_mutation_sequence = :finalMutationSequence
          AND trusted_end_offset_ms IS NULL AND terminal_reason IS NULL
          AND NOT EXISTS (SELECT 1 FROM heart_rate_recordings WHERE session_id = :sessionId)
          AND EXISTS (SELECT 1 FROM workout_phase_intervals
              WHERE id = :closedPhaseId AND session_id = :sessionId AND open_marker IS NULL
                AND end_offset_ms = :finalOffsetMs AND end_mutation_sequence = :finalMutationSequence)
        """
    )
    suspend fun finalizeSessionWithoutRecording(
        sessionId: String,
        expectedStatus: String,
        closedPhaseId: String,
        finalOffsetMs: Long,
        finalMutationSequence: Long,
        terminalStatus: String,
        terminalReason: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCanonicalStepRecords(records: List<SessionStepRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCanonicalStrengthSetRecords(records: List<StrengthSetRecordEntity>)

    @Query(
        """
        UPDATE workout_sessions
        SET plan_id = :planId,
            mode = :mode,
            status = :status,
            plan_snapshot_json = :planSnapshotJson,
            started_at = :startedAt,
            ended_at = :endedAt,
            total_elapsed_sec = :totalElapsedSec,
            effective_elapsed_sec = :effectiveElapsedSec,
            paused_elapsed_sec = :pausedElapsedSec
        WHERE id = :id
          AND timeline_version IS NULL
          AND last_durable_offset_ms IS NULL
          AND last_mutation_sequence IS NULL
          AND trusted_end_offset_ms IS NULL
          AND terminal_reason IS NULL
          AND display_metadata_contract_version IS NULL
          AND session_display_metadata_json IS NULL
        """
    )
    suspend fun updateLegacySession(
        id: String,
        planId: String?,
        mode: String,
        status: String,
        planSnapshotJson: String,
        startedAt: String?,
        endedAt: String?,
        totalElapsedSec: Int?,
        effectiveElapsedSec: Int?,
        pausedElapsedSec: Int?
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepRecords(records: List<SessionStepRecordEntity>)

    @Query("SELECT * FROM session_step_records WHERE session_id = :sessionId ORDER BY id")
    suspend fun stepRecordsForSession(sessionId: String): List<SessionStepRecordEntity>

    @Query("SELECT * FROM timed_rest_extension_records WHERE session_id = :sessionId ORDER BY id")
    suspend fun restExtensionRecordsForSession(sessionId: String): List<TimedRestExtensionRecordEntity>

    @Query("SELECT * FROM strength_set_records WHERE session_id = :sessionId ORDER BY id")
    suspend fun strengthSetRecordsForSession(sessionId: String): List<StrengthSetRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStrengthSetRecords(records: List<StrengthSetRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimedRestExtensionRecords(records: List<TimedRestExtensionRecordEntity>)

    @Query("DELETE FROM session_step_records WHERE session_id = :sessionId")
    suspend fun deleteStepRecordsForSession(sessionId: String)

    @Query("DELETE FROM timed_rest_extension_records WHERE session_id = :sessionId")
    suspend fun deleteTimedRestExtensionRecordsForSession(sessionId: String)

    @Query("DELETE FROM strength_set_records WHERE session_id = :sessionId")
    suspend fun deleteStrengthSetRecordsForSession(sessionId: String)

    @Query("DELETE FROM session_step_records")
    suspend fun deleteAllStepRecords()

    @Query("DELETE FROM timed_rest_extension_records")
    suspend fun deleteAllTimedRestExtensionRecords()

    @Query("DELETE FROM strength_set_records")
    suspend fun deleteAllStrengthSetRecords()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM session_step_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE plan_id = :planId)")
    suspend fun deleteStepRecordsForPlan(planId: String)

    @Query("DELETE FROM timed_rest_extension_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE plan_id = :planId)")
    suspend fun deleteTimedRestExtensionRecordsForPlan(planId: String)

    @Query("DELETE FROM strength_set_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE plan_id = :planId)")
    suspend fun deleteStrengthSetRecordsForPlan(planId: String)

    @Query("DELETE FROM workout_sessions WHERE plan_id = :planId")
    suspend fun deleteSessionsForPlan(planId: String)

    @Query("DELETE FROM session_step_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE started_at IS NOT NULL AND substr(started_at, 1, 10) = :date)")
    suspend fun deleteStepRecordsStartedOnDate(date: String)

    @Query("DELETE FROM timed_rest_extension_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE started_at IS NOT NULL AND substr(started_at, 1, 10) = :date)")
    suspend fun deleteTimedRestExtensionRecordsStartedOnDate(date: String)

    @Query("DELETE FROM strength_set_records WHERE session_id IN (SELECT id FROM workout_sessions WHERE started_at IS NOT NULL AND substr(started_at, 1, 10) = :date)")
    suspend fun deleteStrengthSetRecordsStartedOnDate(date: String)

    @Query("DELETE FROM workout_sessions WHERE started_at IS NOT NULL AND substr(started_at, 1, 10) = :date")
    suspend fun deleteSessionsStartedOnDate(date: String)

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY COALESCE(ended_at, started_at, '') DESC")
    fun observeSessionsWithRecords(): Flow<List<WorkoutSessionWithRecords>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY COALESCE(ended_at, started_at, '') DESC")
    suspend fun getSessionsWithRecords(): List<WorkoutSessionWithRecords>
}

data class WorkoutSessionHeaderRow(
    val id: String,
    @ColumnInfo(name = "plan_id") val planId: String?,
    val mode: String,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: String?,
    @ColumnInfo(name = "ended_at") val endedAt: String?,
    @ColumnInfo(name = "total_elapsed_sec") val totalElapsedSec: Int?,
    @ColumnInfo(name = "effective_elapsed_sec") val effectiveElapsedSec: Int?,
    @ColumnInfo(name = "paused_elapsed_sec") val pausedElapsedSec: Int?,
    @ColumnInfo(name = "timeline_version") val timelineVersion: Int?,
    @ColumnInfo(name = "trusted_end_offset_ms") val trustedEndOffsetMs: Long?,
    @ColumnInfo(name = "terminal_reason") val terminalReason: String?,
    @ColumnInfo(name = "start_local_date") val startLocalDate: String?,
    @ColumnInfo(name = "start_zone_id") val startZoneId: String?,
    @ColumnInfo(name = "start_utc_offset_seconds") val startUtcOffsetSeconds: Long?,
    @ColumnInfo(name = "time_metadata_source_contract_version") val timeMetadataSourceContractVersion: Long?,
    @ColumnInfo(name = "last_durable_offset_ms") val lastDurableOffsetMs: Long?
)

data class WorkoutSessionWithRecords(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val stepRecords: List<SessionStepRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val timedRestExtensionRecords: List<TimedRestExtensionRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val strengthSetRecords: List<StrengthSetRecordEntity>
)
