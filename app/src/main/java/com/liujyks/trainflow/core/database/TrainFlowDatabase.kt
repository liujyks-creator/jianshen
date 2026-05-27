package com.liujyks.trainflow.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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
            ).build()
        }
    }
}
