package com.liujyks.trainflow.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liujyks.trainflow.core.database.mapping.StorageMappingStrategy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainFlowDatabaseSmokeTest {
    private lateinit var database: TrainFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrainFlowDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createsEmptyRoomDatabaseSkeleton() = runBlocking {
        assertEquals(0, database.exerciseDao().count())
        assertEquals(0, database.workoutPlanDao().count())
        assertEquals(0, database.workoutSessionDao().sessionCount())
        assertEquals(0, database.workoutSessionDao().stepRecordCount())
        assertEquals(0, database.workoutSessionDao().strengthSetRecordCount())
        assertEquals(0, database.recoveryDao().areaCount())
        assertEquals(0, database.recoveryDao().recommendationCount())
    }

    @Test
    fun documentsJsonBackedPlanSnapshotBoundary() {
        assertTrue(
            StorageMappingStrategy.jsonBackedColumns.contains(
                "workout_sessions.plan_snapshot_json"
            )
        )
    }
}
