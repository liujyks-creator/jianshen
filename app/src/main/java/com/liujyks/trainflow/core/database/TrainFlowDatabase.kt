package com.liujyks.trainflow.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liujyks.trainflow.core.database.dao.CanonicalTimelineHeartRateDao
import com.liujyks.trainflow.core.database.dao.ExerciseDao
import com.liujyks.trainflow.core.database.dao.RecoveryDao
import com.liujyks.trainflow.core.database.dao.WorkoutPlanDao
import com.liujyks.trainflow.core.database.dao.WorkoutSessionDao
import com.liujyks.trainflow.core.database.entity.ExerciseEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateAnalysisSnapshotEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.RecoveryAreaEntity
import com.liujyks.trainflow.core.database.entity.RecoveryRecommendationEntity
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.TimedRestExtensionRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutSessionEntity::class,
        SessionStepRecordEntity::class,
        TimedRestExtensionRecordEntity::class,
        StrengthSetRecordEntity::class,
        RecoveryAreaEntity::class,
        RecoveryRecommendationEntity::class,
        WorkoutPhaseIntervalEntity::class,
        HeartRateRecordingEntity::class,
        HeartRateAcquisitionIntervalEntity::class,
        HeartRateSampleEntity::class,
        HeartRateAnalysisSnapshotEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class TrainFlowDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun recoveryDao(): RecoveryDao
    abstract fun canonicalTimelineHeartRateDao(): CanonicalTimelineHeartRateDao

    companion object {
        const val DATABASE_NAME = "trainflow.db"

        fun create(context: Context): TrainFlowDatabase {
            return Room.databaseBuilder(
                context = context.applicationContext,
                klass = TrainFlowDatabase::class.java,
                name = DATABASE_NAME
            ).addCallback(CANONICAL_SCHEMA_V5_ON_CREATE)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }

        val CANONICAL_SCHEMA_V5_ON_CREATE = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE heart_rate_analysis_snapshots")
                db.execSQL("DROP TABLE heart_rate_samples")
                db.execSQL("DROP TABLE heart_rate_acquisition_intervals")
                db.execSQL("DROP TABLE heart_rate_recordings")
                db.execSQL("DROP TABLE workout_phase_intervals")
                db.execSQL("DROP TABLE workout_sessions")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `plan_id` TEXT, `mode` TEXT NOT NULL, `status` TEXT NOT NULL, `plan_snapshot_json` TEXT NOT NULL, `started_at` TEXT, `ended_at` TEXT, `total_elapsed_sec` INTEGER, `effective_elapsed_sec` INTEGER, `paused_elapsed_sec` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_plan_id` ON `workout_sessions` (`plan_id`)"
                )
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN total_elapsed_sec INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN effective_elapsed_sec INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN paused_elapsed_sec INTEGER")
                db.execSQL("ALTER TABLE session_step_records ADD COLUMN step_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE session_step_records ADD COLUMN block_id TEXT")
                db.execSQL("ALTER TABLE session_step_records ADD COLUMN item_id TEXT")
                db.execSQL("ALTER TABLE session_step_records ADD COLUMN set_plan_id TEXT")
                db.execSQL("ALTER TABLE session_step_records ADD COLUMN planned_duration_sec INTEGER")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN source_set_plan_id TEXT")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN side TEXT")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN active_duration_sec INTEGER")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN actual_rest_after_sec INTEGER")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN substituted_from_exercise_id TEXT")
                db.execSQL("ALTER TABLE strength_set_records ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN description TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS timed_rest_extension_records (
                        id TEXT NOT NULL,
                        session_id TEXT NOT NULL,
                        step_id TEXT NOT NULL,
                        step_index INTEGER NOT NULL,
                        round_index INTEGER,
                        rest_stage_id TEXT,
                        rest_stage_title TEXT NOT NULL,
                        previous_stage_id TEXT,
                        previous_stage_title TEXT,
                        added_sec INTEGER NOT NULL,
                        planned_rest_sec INTEGER NOT NULL,
                        rest_elapsed_before_extension_sec INTEGER NOT NULL,
                        extension_at_remaining_sec INTEGER NOT NULL,
                        cumulative_extra_rest_sec INTEGER NOT NULL,
                        event_elapsed_sec INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_timed_rest_extension_records_session_id ON timed_rest_extension_records(session_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_timed_rest_extension_records_step_id ON timed_rest_extension_records(step_id)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN timeline_version INTEGER DEFAULT NULL
                    CHECK (timeline_version IS NULL OR timeline_version = 1)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN last_durable_offset_ms INTEGER DEFAULT NULL
                    CHECK (last_durable_offset_ms IS NULL OR last_durable_offset_ms >= 0)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN last_mutation_sequence INTEGER DEFAULT NULL
                    CHECK (last_mutation_sequence IS NULL OR last_mutation_sequence >= 0)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN trusted_end_offset_ms INTEGER DEFAULT NULL
                    CHECK (trusted_end_offset_ms IS NULL OR trusted_end_offset_ms >= 0)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN terminal_reason TEXT DEFAULT NULL
                    CHECK (
                        terminal_reason IS NULL OR terminal_reason IN (
                            'completed',
                            'user_abandoned',
                            'owner_cleared',
                            'process_interrupted'
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN display_metadata_contract_version INTEGER DEFAULT NULL
                    CHECK (
                        display_metadata_contract_version IS NULL
                        OR display_metadata_contract_version = 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE workout_sessions
                    ADD COLUMN session_display_metadata_json TEXT DEFAULT NULL
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE workout_phase_intervals (
                        id TEXT NOT NULL PRIMARY KEY,
                        session_id TEXT NOT NULL,
                        sequence INTEGER NOT NULL CHECK (sequence >= 0),
                        start_offset_ms INTEGER NOT NULL CHECK (start_offset_ms >= 0),
                        end_offset_ms INTEGER CHECK (end_offset_ms IS NULL OR end_offset_ms >= 0),
                        start_mutation_sequence INTEGER NOT NULL
                            CHECK (start_mutation_sequence >= 0),
                        end_mutation_sequence INTEGER
                            CHECK (end_mutation_sequence IS NULL OR end_mutation_sequence >= 0),
                        open_marker INTEGER CHECK (open_marker IS NULL OR open_marker = 1),
                        phase_kind TEXT NOT NULL CHECK (
                            phase_kind IN (
                                'timed_work',
                                'timed_rest',
                                'strength_prepare_set',
                                'strength_active_set',
                                'strength_confirm_set',
                                'strength_rest',
                                'follow_along_action',
                                'follow_along_rest',
                                'paused'
                            )
                        ),
                        phase_identity_json TEXT NOT NULL,
                        FOREIGN KEY (session_id)
                            REFERENCES workout_sessions(id) ON DELETE CASCADE,
                        UNIQUE (session_id, sequence),
                        UNIQUE (session_id, open_marker),
                        CHECK (
                            (
                                open_marker = 1
                                AND end_offset_ms IS NULL
                                AND end_mutation_sequence IS NULL
                            )
                            OR
                            (
                                open_marker IS NULL
                                AND end_offset_ms IS NOT NULL
                                AND end_mutation_sequence IS NOT NULL
                            )
                        ),
                        CHECK (
                            end_offset_ms IS NULL
                            OR end_offset_ms > start_offset_ms
                            OR (
                                end_offset_ms = start_offset_ms
                                AND end_mutation_sequence > start_mutation_sequence
                            )
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_workout_phase_intervals_session_sequence
                    ON workout_phase_intervals(session_id, sequence)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_workout_phase_intervals_session_open_marker
                    ON workout_phase_intervals(session_id, open_marker)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX index_workout_phase_intervals_session_start
                    ON workout_phase_intervals(session_id, start_offset_ms)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE heart_rate_recordings (
                        recording_id TEXT NOT NULL PRIMARY KEY,
                        session_id TEXT NOT NULL UNIQUE,
                        status TEXT NOT NULL CHECK (status IN ('active', 'terminal')),
                        started_offset_ms INTEGER NOT NULL CHECK (started_offset_ms >= 0),
                        started_mutation_sequence INTEGER NOT NULL
                            CHECK (started_mutation_sequence >= 0),
                        ended_offset_ms INTEGER
                            CHECK (ended_offset_ms IS NULL OR ended_offset_ms >= 0),
                        ended_mutation_sequence INTEGER
                            CHECK (ended_mutation_sequence IS NULL OR ended_mutation_sequence >= 0),
                        source_contract_version INTEGER NOT NULL
                            CHECK (source_contract_version = 1),
                        source_kind TEXT NOT NULL CHECK (source_kind = 'ble_hrs'),
                        acquisition_contract_version INTEGER NOT NULL
                            CHECK (acquisition_contract_version = 1),
                        parameter_snapshot_version INTEGER NOT NULL
                            CHECK (parameter_snapshot_version = 1),
                        age INTEGER CHECK (age IS NULL OR age BETWEEN 1 AND 130),
                        personal_max_bpm INTEGER
                            CHECK (personal_max_bpm IS NULL OR personal_max_bpm BETWEEN 30 AND 260),
                        effective_max_bpm INTEGER
                            CHECK (effective_max_bpm IS NULL OR effective_max_bpm BETWEEN 30 AND 260),
                        effective_max_source TEXT CHECK (
                            effective_max_source IS NULL
                            OR effective_max_source IN ('personal_max', 'age_220_minus_age')
                        ),
                        alert_threshold_bpm INTEGER CHECK (
                            alert_threshold_bpm IS NULL
                            OR alert_threshold_bpm BETWEEN 30 AND 260
                        ),
                        zone_snapshot_json TEXT,
                        original_analysis_version INTEGER CHECK (
                            original_analysis_version IS NULL
                            OR original_analysis_version = 1
                        ),
                        FOREIGN KEY (session_id)
                            REFERENCES workout_sessions(id) ON DELETE CASCADE,
                        CHECK (
                            (
                                status = 'active'
                                AND ended_offset_ms IS NULL
                                AND ended_mutation_sequence IS NULL
                                AND original_analysis_version IS NULL
                            )
                            OR
                            (
                                status = 'terminal'
                                AND ended_offset_ms IS NOT NULL
                                AND ended_mutation_sequence IS NOT NULL
                                AND original_analysis_version = 1
                            )
                        ),
                        CHECK (
                            ended_offset_ms IS NULL
                            OR ended_offset_ms > started_offset_ms
                            OR (
                                ended_offset_ms = started_offset_ms
                                AND ended_mutation_sequence > started_mutation_sequence
                            )
                        ),
                        CHECK (
                            (
                                effective_max_bpm IS NULL
                                AND effective_max_source IS NULL
                                AND zone_snapshot_json IS NULL
                                AND age IS NULL
                                AND personal_max_bpm IS NULL
                            )
                            OR
                            (
                                effective_max_source = 'personal_max'
                                AND personal_max_bpm IS NOT NULL
                                AND effective_max_bpm = personal_max_bpm
                                AND zone_snapshot_json IS NOT NULL
                            )
                            OR
                            (
                                effective_max_source = 'age_220_minus_age'
                                AND personal_max_bpm IS NULL
                                AND age IS NOT NULL
                                AND effective_max_bpm = 220 - age
                                AND zone_snapshot_json IS NOT NULL
                            )
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_heart_rate_recordings_session_id
                    ON heart_rate_recordings(session_id)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE heart_rate_acquisition_intervals (
                        id TEXT NOT NULL PRIMARY KEY,
                        recording_id TEXT NOT NULL,
                        sequence INTEGER NOT NULL CHECK (sequence >= 0),
                        start_offset_ms INTEGER NOT NULL CHECK (start_offset_ms >= 0),
                        end_offset_ms INTEGER CHECK (end_offset_ms IS NULL OR end_offset_ms >= 0),
                        start_mutation_sequence INTEGER NOT NULL
                            CHECK (start_mutation_sequence >= 0),
                        end_mutation_sequence INTEGER
                            CHECK (end_mutation_sequence IS NULL OR end_mutation_sequence >= 0),
                        open_marker INTEGER CHECK (open_marker IS NULL OR open_marker = 1),
                        recording_intent TEXT NOT NULL CHECK (
                            recording_intent IN ('expected_recording', 'user_excluded')
                        ),
                        intent_reason TEXT CHECK (
                            intent_reason IS NULL
                            OR intent_reason IN (
                                'user_turned_off',
                                'user_opted_out',
                                'user_disconnected_suppress_recovery'
                            )
                        ),
                        device_state TEXT NOT NULL CHECK (
                            device_state IN (
                                'not_observing',
                                'no_source_selected',
                                'permission_required',
                                'bluetooth_unavailable',
                                'searching',
                                'connecting',
                                'waiting_first_sample',
                                'live',
                                'stale',
                                'reconnecting',
                                'disconnected',
                                'technical_failure'
                            )
                        ),
                        device_reason TEXT CHECK (
                            device_reason IS NULL
                            OR device_reason IN (
                                'initial_acquisition',
                                'automatic_recovery',
                                'source_not_selected',
                                'source_unavailable',
                                'permission_missing',
                                'permission_revoked',
                                'bluetooth_off',
                                'platform_unavailable',
                                'first_sample_timeout',
                                'sample_stale_timeout',
                                'unexpected_disconnect',
                                'connection_timeout',
                                'measurement_stream_unavailable',
                                'platform_failure'
                            )
                        ),
                        FOREIGN KEY (recording_id)
                            REFERENCES heart_rate_recordings(recording_id) ON DELETE CASCADE,
                        UNIQUE (recording_id, sequence),
                        UNIQUE (recording_id, open_marker),
                        CHECK (
                            (
                                recording_intent = 'expected_recording'
                                AND intent_reason IS NULL
                            )
                            OR
                            (
                                recording_intent = 'user_excluded'
                                AND intent_reason IN (
                                    'user_turned_off',
                                    'user_opted_out',
                                    'user_disconnected_suppress_recovery'
                                )
                            )
                        ),
                        CHECK (
                            (
                                open_marker = 1
                                AND end_offset_ms IS NULL
                                AND end_mutation_sequence IS NULL
                            )
                            OR
                            (
                                open_marker IS NULL
                                AND end_offset_ms IS NOT NULL
                                AND end_mutation_sequence IS NOT NULL
                            )
                        ),
                        CHECK (
                            end_offset_ms IS NULL
                            OR end_offset_ms > start_offset_ms
                            OR (
                                end_offset_ms = start_offset_ms
                                AND end_mutation_sequence > start_mutation_sequence
                            )
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_hr_acquisition_recording_sequence
                    ON heart_rate_acquisition_intervals(recording_id, sequence)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_hr_acquisition_recording_open_marker
                    ON heart_rate_acquisition_intervals(recording_id, open_marker)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX index_hr_acquisition_recording_start
                    ON heart_rate_acquisition_intervals(recording_id, start_offset_ms)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE heart_rate_samples (
                        recording_id TEXT NOT NULL,
                        sample_sequence INTEGER NOT NULL CHECK (sample_sequence >= 0),
                        offset_ms INTEGER NOT NULL CHECK (offset_ms >= 0),
                        mutation_sequence INTEGER NOT NULL CHECK (mutation_sequence >= 0),
                        bpm INTEGER NOT NULL CHECK (bpm BETWEEN 1 AND 65535),
                        PRIMARY KEY (recording_id, sample_sequence),
                        FOREIGN KEY (recording_id)
                            REFERENCES heart_rate_recordings(recording_id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX index_hr_samples_canonical_order
                    ON heart_rate_samples(
                        recording_id,
                        offset_ms,
                        mutation_sequence,
                        sample_sequence
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE heart_rate_analysis_snapshots (
                        recording_id TEXT NOT NULL,
                        analysis_version INTEGER NOT NULL CHECK (analysis_version = 1),
                        created_at TEXT NOT NULL,
                        input_last_mutation_sequence INTEGER NOT NULL
                            CHECK (input_last_mutation_sequence >= 0),
                        sample_status TEXT NOT NULL CHECK (
                            sample_status IN (
                                'no_canonical_samples',
                                'canonical_only_excluded',
                                'primary_points_available'
                            )
                        ),
                        coverage_status TEXT NOT NULL CHECK (
                            coverage_status IN (
                                'no_eligible_duration',
                                'insufficient',
                                'partial',
                                'normal'
                            )
                        ),
                        zone_status TEXT NOT NULL CHECK (
                            zone_status IN ('available', 'unavailable_no_effective_max')
                        ),
                        canonical_sample_count INTEGER NOT NULL
                            CHECK (canonical_sample_count >= 0),
                        primary_point_sample_count INTEGER NOT NULL CHECK (
                            primary_point_sample_count >= 0
                            AND primary_point_sample_count <= canonical_sample_count
                        ),
                        eligible_duration_ms INTEGER
                            CHECK (eligible_duration_ms IS NULL OR eligible_duration_ms >= 0),
                        covered_duration_ms INTEGER
                            CHECK (covered_duration_ms IS NULL OR covered_duration_ms >= 0),
                        coverage_basis_points INTEGER CHECK (
                            coverage_basis_points IS NULL
                            OR coverage_basis_points BETWEEN 0 AND 10000
                        ),
                        weighted_bpm_ms INTEGER
                            CHECK (weighted_bpm_ms IS NULL OR weighted_bpm_ms >= 0),
                        observed_avg_bpm INTEGER CHECK (
                            observed_avg_bpm IS NULL OR observed_avg_bpm BETWEEN 1 AND 65535
                        ),
                        observed_max_bpm INTEGER CHECK (
                            observed_max_bpm IS NULL OR observed_max_bpm BETWEEN 1 AND 65535
                        ),
                        highest_offset_ms INTEGER
                            CHECK (highest_offset_ms IS NULL OR highest_offset_ms >= 0),
                        highest_mutation_sequence INTEGER CHECK (
                            highest_mutation_sequence IS NULL
                            OR highest_mutation_sequence >= 0
                        ),
                        highest_sample_sequence INTEGER CHECK (
                            highest_sample_sequence IS NULL
                            OR highest_sample_sequence >= 0
                        ),
                        analysis_config_json TEXT NOT NULL,
                        zone_durations_json TEXT,
                        phase_aggregates_json TEXT NOT NULL,
                        duration_breakdown_json TEXT NOT NULL,
                        quality_reasons_json TEXT NOT NULL,
                        PRIMARY KEY (recording_id, analysis_version),
                        FOREIGN KEY (recording_id)
                            REFERENCES heart_rate_recordings(recording_id) ON DELETE CASCADE,
                        CHECK (
                            eligible_duration_ms IS NOT NULL
                            AND covered_duration_ms IS NOT NULL
                            AND covered_duration_ms <= eligible_duration_ms
                        ),
                        CHECK (
                            (
                                eligible_duration_ms = 0
                                AND covered_duration_ms = 0
                                AND coverage_basis_points IS NULL
                                AND coverage_status = 'no_eligible_duration'
                            )
                            OR
                            (
                                eligible_duration_ms > 0
                                AND coverage_basis_points IS NOT NULL
                                AND coverage_status IN ('insufficient', 'partial', 'normal')
                            )
                        ),
                        CHECK (
                            (
                                covered_duration_ms = 0
                                AND weighted_bpm_ms IS NULL
                                AND observed_avg_bpm IS NULL
                            )
                            OR
                            (
                                covered_duration_ms > 0
                                AND weighted_bpm_ms IS NOT NULL
                                AND observed_avg_bpm IS NOT NULL
                            )
                        ),
                        CHECK (
                            (
                                observed_max_bpm IS NULL
                                AND highest_offset_ms IS NULL
                                AND highest_mutation_sequence IS NULL
                                AND highest_sample_sequence IS NULL
                            )
                            OR
                            (
                                observed_max_bpm IS NOT NULL
                                AND highest_offset_ms IS NOT NULL
                                AND highest_mutation_sequence IS NOT NULL
                                AND highest_sample_sequence IS NOT NULL
                            )
                        ),
                        CHECK (
                            (
                                primary_point_sample_count = 0
                                AND observed_max_bpm IS NULL
                            )
                            OR
                            (
                                primary_point_sample_count > 0
                                AND observed_max_bpm IS NOT NULL
                            )
                        ),
                        CHECK (
                            (
                                sample_status = 'no_canonical_samples'
                                AND canonical_sample_count = 0
                                AND primary_point_sample_count = 0
                            )
                            OR
                            (
                                sample_status = 'canonical_only_excluded'
                                AND canonical_sample_count > 0
                                AND primary_point_sample_count = 0
                            )
                            OR
                            (
                                sample_status = 'primary_points_available'
                                AND primary_point_sample_count > 0
                            )
                        ),
                        CHECK (
                            (
                                zone_status = 'unavailable_no_effective_max'
                                AND zone_durations_json IS NULL
                            )
                            OR
                            (
                                zone_status = 'available'
                                AND (
                                    eligible_duration_ms = 0
                                    OR zone_durations_json IS NOT NULL
                                )
                            )
                        )
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
