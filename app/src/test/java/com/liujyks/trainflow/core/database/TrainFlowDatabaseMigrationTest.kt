package com.liujyks.trainflow.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainFlowDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        requireNotNull(TrainFlowDatabase::class.java.canonicalName),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrationFromVersion1To2KeepsSessionRecordsAndAddsDefaults() {
        val testDbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath(TEST_DB)
            .absolutePath

        helper.createDatabase(testDbPath, 1).apply {
            insertVersion1WorkoutSession()
            insertVersion1StepRecord()
            insertVersion1StrengthSetRecord()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            testDbPath,
            2,
            true,
            TrainFlowDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT id, total_elapsed_sec, effective_elapsed_sec, paused_elapsed_sec FROM workout_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("session-v1", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
        migrated.query("SELECT id, step_id, block_id, item_id, set_plan_id, planned_duration_sec FROM session_step_records").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("step-v1", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertTrue(cursor.isNull(5))
        }
        migrated.query(
            "SELECT id, source_set_plan_id, side, active_duration_sec, actual_rest_after_sec, substituted_from_exercise_id, notes FROM strength_set_records"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("set-v1", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertTrue(cursor.isNull(5))
            assertTrue(cursor.isNull(6))
        }
        migrated.query("PRAGMA table_info(session_step_records)").use { cursor ->
            val stepIdColumn = generateSequence {
                if (cursor.moveToNext()) cursor else null
            }.first { row -> row.getString(row.getColumnIndexOrThrow("name")) == "step_id" }

            assertEquals(1, stepIdColumn.getInt(stepIdColumn.getColumnIndexOrThrow("notnull")))
            assertEquals("''", stepIdColumn.getString(stepIdColumn.getColumnIndexOrThrow("dflt_value")))
        }
    }

    @Test
    fun migrationFromVersion2To3AddsNullableWorkoutPlanDescription() {
        val testDbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath(TEST_DB)
            .absolutePath

        helper.createDatabase(testDbPath, 2).apply {
            insertVersion2WorkoutPlan()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            testDbPath,
            3,
            true,
            TrainFlowDatabase.MIGRATION_2_3
        )

        migrated.query("SELECT id, description FROM workout_plans").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("plan-v2", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
    }

    @Test
    fun migrationFromVersion3To4CreatesTimedRestExtensionRecordsTable() {
        val testDbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath(TEST_DB)
            .absolutePath

        helper.createDatabase(testDbPath, 3).apply {
            insertVersion3WorkoutSession()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            testDbPath,
            4,
            true,
            TrainFlowDatabase.MIGRATION_3_4
        )

        migrated.query("SELECT id FROM workout_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("session-v3", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM timed_rest_extension_records").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("PRAGMA table_info(timed_rest_extension_records)").use { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                names += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
            assertTrue("step_index" in names)
            assertTrue("round_index" in names)
            assertTrue("previous_stage_title" in names)
            assertTrue("cumulative_extra_rest_sec" in names)
        }
    }

    @Test
    fun versionFiveUpgradeAndFreshInstallHaveIdenticalVersionSixSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath(TrainFlowDatabase.DATABASE_NAME).absolutePath
        helper.createDatabase(path, 5).close()

        val upgraded = TrainFlowDatabase.create(context)
        val fresh = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val sql = upgraded.openHelper.writableDatabase
            assertEquals(6, sql.version)
            val freshSql = fresh.openHelper.writableDatabase
            assertEquals(6, freshSql.version)
            sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name").use { tables ->
                var count = 0
                while (tables.moveToNext()) {
                    val name = tables.getString(0)
                    val schemaQuery = "SELECT type, name, sql FROM sqlite_master WHERE tbl_name='$name' ORDER BY type, name"
                    val expected = freshSql.query(schemaQuery).use { cursor ->
                        buildList { while (cursor.moveToNext()) add(listOf(cursor.getString(0), cursor.getString(1), cursor.getString(2)?.replace("`", ""))) }
                    }
                    val actual = sql.query(schemaQuery).use { cursor ->
                        buildList { while (cursor.moveToNext()) add(listOf(cursor.getString(0), cursor.getString(1), cursor.getString(2)?.replace("`", ""))) }
                    }
                    assertEquals("table, constraints and indexes: $name", expected, actual)
                    count++
                }
                assertEquals(13, count)
            }
            val columns = sql.query("PRAGMA table_info(workout_sessions)").use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) put(cursor.getString(1), listOf(cursor.getString(2), cursor.getString(3), cursor.getString(4)))
                }
            }
            listOf(
                "start_local_date" to "TEXT",
                "start_zone_id" to "TEXT",
                "start_utc_offset_seconds" to "INTEGER",
                "time_metadata_source_contract_version" to "INTEGER"
            ).forEach { (name, type) ->
                val column = requireNotNull(columns[name])
                assertEquals(listOf(type, "0", "NULL"), column)
            }
        } finally {
            upgraded.close()
            fresh.close()
        }
    }

    @Test
    fun versionOneFullProductionUpgradeReachesSixAndKeepsLegacyValuesUnknown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath(TrainFlowDatabase.DATABASE_NAME).absolutePath
        helper.createDatabase(path, 1).apply {
            insertVersion1WorkoutSession()
            insertVersion1StepRecord()
            insertVersion1StrengthSetRecord()
            close()
        }
        val upgraded = TrainFlowDatabase.create(context)
        try {
            val sql = upgraded.openHelper.writableDatabase
            assertEquals(6, sql.version)
            sql.query("SELECT id, plan_snapshot_json, started_at, ended_at, start_local_date, start_zone_id, start_utc_offset_seconds, time_metadata_source_contract_version FROM workout_sessions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("session-v1", cursor.getString(0))
                assertEquals("{\"title\":\"Legacy Strength\",\"mode\":\"strength\"}", cursor.getString(1))
                assertEquals("2026-06-07T10:00:00Z", cursor.getString(2))
                assertEquals("2026-06-07T10:10:00Z", cursor.getString(3))
                for (column in 4..7) assertTrue(cursor.isNull(column))
                assertEquals(1, cursor.count)
            }
            sql.query("SELECT id, step_id, actual_duration_sec FROM session_step_records").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("step-v1", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertEquals(30, cursor.getInt(2))
                assertEquals(1, cursor.count)
            }
            sql.query("SELECT id, planned_json, actual_json FROM strength_set_records").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("set-v1", cursor.getString(0))
                assertEquals("weight=60.0,kg|rep=range,8,12", cursor.getString(1))
                assertEquals("weight=60.0,kg|reps=8", cursor.getString(2))
                assertEquals(1, cursor.count)
            }
        } finally {
            upgraded.close()
        }
    }

    private fun SupportSQLiteDatabase.insertVersion1WorkoutSession() {
        execSQL(
            """
            INSERT INTO workout_sessions(
                id, plan_id, mode, status, plan_snapshot_json, started_at, ended_at
            ) VALUES(
                'session-v1',
                'plan-v1',
                'strength',
                'completed',
                '{"title":"Legacy Strength","mode":"strength"}',
                '2026-06-07T10:00:00Z',
                '2026-06-07T10:10:00Z'
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertVersion1StepRecord() {
        execSQL(
            """
            INSERT INTO session_step_records(
                id, session_id, kind, exercise_id, started_at, ended_at, skipped, actual_duration_sec
            ) VALUES(
                'step-v1',
                'session-v1',
                'strength_active_set',
                'barbell-bench-press',
                '2026-06-07T10:00:00Z',
                '2026-06-07T10:00:30Z',
                0,
                30
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertVersion1StrengthSetRecord() {
        execSQL(
            """
            INSERT INTO strength_set_records(
                id, session_id, exercise_id, set_order, set_kind, planned_json, actual_json, effort
            ) VALUES(
                'set-v1',
                'session-v1',
                'barbell-bench-press',
                1,
                'working',
                'weight=60.0,kg|rep=range,8,12',
                'weight=60.0,kg|reps=8',
                'good'
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertVersion2WorkoutPlan() {
        execSQL(
            """
            INSERT INTO workout_plans(
                id, mode, title, blocks_json, reminder_json, preferences_json, follow_along_json, created_at, updated_at
            ) VALUES(
                'plan-v2',
                'timed',
                'Legacy Timed',
                '[]',
                NULL,
                NULL,
                NULL,
                '2026-06-13T08:00:00Z',
                '2026-06-13T08:01:00Z'
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertVersion3WorkoutSession() {
        execSQL(
            """
            INSERT INTO workout_sessions(
                id, plan_id, mode, status, plan_snapshot_json, started_at, ended_at,
                total_elapsed_sec, effective_elapsed_sec, paused_elapsed_sec
            ) VALUES(
                'session-v3',
                'plan-v3',
                'timed',
                'completed',
                '{"title":"Legacy Timed","mode":"timed","blocks":[]}',
                '2026-06-14T10:00:00Z',
                '2026-06-14T10:10:00Z',
                600,
                580,
                20
            )
            """.trimIndent()
        )
    }

    private companion object {
        const val TEST_DB = "trainflow-migration-test"
    }
}
