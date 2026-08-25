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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
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
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `timeline_version` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `last_durable_offset_ms` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `last_mutation_sequence` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `trusted_end_offset_ms` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `terminal_reason` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `display_metadata_contract_version` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN `session_display_metadata_json` TEXT DEFAULT NULL")

                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_phase_intervals` (`id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `start_offset_ms` INTEGER NOT NULL, `end_offset_ms` INTEGER, `start_mutation_sequence` INTEGER NOT NULL, `end_mutation_sequence` INTEGER, `open_marker` INTEGER, `phase_kind` TEXT NOT NULL, `phase_identity_json` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_phase_intervals_session_sequence` ON `workout_phase_intervals` (`session_id`, `sequence`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_phase_intervals_session_open_marker` ON `workout_phase_intervals` (`session_id`, `open_marker`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_phase_intervals_session_start` ON `workout_phase_intervals` (`session_id`, `start_offset_ms`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `heart_rate_recordings` (`recording_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `status` TEXT NOT NULL, `started_offset_ms` INTEGER NOT NULL, `started_mutation_sequence` INTEGER NOT NULL, `ended_offset_ms` INTEGER, `ended_mutation_sequence` INTEGER, `source_contract_version` INTEGER NOT NULL, `source_kind` TEXT NOT NULL, `acquisition_contract_version` INTEGER NOT NULL, `parameter_snapshot_version` INTEGER NOT NULL, `age` INTEGER, `personal_max_bpm` INTEGER, `effective_max_bpm` INTEGER, `effective_max_source` TEXT, `alert_threshold_bpm` INTEGER, `zone_snapshot_json` TEXT, `original_analysis_version` INTEGER, PRIMARY KEY(`recording_id`), FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_heart_rate_recordings_session_id` ON `heart_rate_recordings` (`session_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `heart_rate_acquisition_intervals` (`id` TEXT NOT NULL, `recording_id` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `start_offset_ms` INTEGER NOT NULL, `end_offset_ms` INTEGER, `start_mutation_sequence` INTEGER NOT NULL, `end_mutation_sequence` INTEGER, `open_marker` INTEGER, `recording_intent` TEXT NOT NULL, `intent_reason` TEXT, `device_state` TEXT NOT NULL, `device_reason` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`recording_id`) REFERENCES `heart_rate_recordings`(`recording_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hr_acquisition_recording_sequence` ON `heart_rate_acquisition_intervals` (`recording_id`, `sequence`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hr_acquisition_recording_open_marker` ON `heart_rate_acquisition_intervals` (`recording_id`, `open_marker`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hr_acquisition_recording_start` ON `heart_rate_acquisition_intervals` (`recording_id`, `start_offset_ms`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `heart_rate_samples` (`recording_id` TEXT NOT NULL, `sample_sequence` INTEGER NOT NULL, `offset_ms` INTEGER NOT NULL, `mutation_sequence` INTEGER NOT NULL, `bpm` INTEGER NOT NULL, PRIMARY KEY(`recording_id`, `sample_sequence`), FOREIGN KEY(`recording_id`) REFERENCES `heart_rate_recordings`(`recording_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hr_samples_canonical_order` ON `heart_rate_samples` (`recording_id`, `offset_ms`, `mutation_sequence`, `sample_sequence`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `heart_rate_analysis_snapshots` (`recording_id` TEXT NOT NULL, `analysis_version` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `input_last_mutation_sequence` INTEGER NOT NULL, `sample_status` TEXT NOT NULL, `coverage_status` TEXT NOT NULL, `zone_status` TEXT NOT NULL, `canonical_sample_count` INTEGER NOT NULL, `primary_point_sample_count` INTEGER NOT NULL, `eligible_duration_ms` INTEGER, `covered_duration_ms` INTEGER, `coverage_basis_points` INTEGER, `weighted_bpm_ms` INTEGER, `observed_avg_bpm` INTEGER, `observed_max_bpm` INTEGER, `highest_offset_ms` INTEGER, `highest_mutation_sequence` INTEGER, `highest_sample_sequence` INTEGER, `analysis_config_json` TEXT NOT NULL, `zone_durations_json` TEXT, `phase_aggregates_json` TEXT NOT NULL, `duration_breakdown_json` TEXT NOT NULL, `quality_reasons_json` TEXT NOT NULL, PRIMARY KEY(`recording_id`, `analysis_version`), FOREIGN KEY(`recording_id`) REFERENCES `heart_rate_recordings`(`recording_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }
    }
}
