package com.liujyks.trainflow.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liujyks.trainflow.core.database.dao.ExerciseDao
import com.liujyks.trainflow.core.database.dao.RecoveryDao
import com.liujyks.trainflow.core.database.dao.WorkoutPlanDao
import com.liujyks.trainflow.core.database.dao.WorkoutSessionDao
import com.liujyks.trainflow.core.database.entity.ExerciseEntity
import com.liujyks.trainflow.core.database.entity.RecoveryAreaEntity
import com.liujyks.trainflow.core.database.entity.RecoveryRecommendationEntity
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPlanEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutSessionEntity::class,
        SessionStepRecordEntity::class,
        StrengthSetRecordEntity::class,
        RecoveryAreaEntity::class,
        RecoveryRecommendationEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TrainFlowDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun recoveryDao(): RecoveryDao

    companion object {
        const val DATABASE_NAME = "trainflow.db"

        fun create(context: Context): TrainFlowDatabase {
            return Room.databaseBuilder(
                context = context.applicationContext,
                klass = TrainFlowDatabase::class.java,
                name = DATABASE_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
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
    }
}
