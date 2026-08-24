package com.liujyks.trainflow.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CanonicalSchemaMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrainFlowDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun versionFourRowsAcrossAllStatusesKeepCanonicalHeaderNull() {
        val path = testDatabasePath("legacy-statuses")
        helper.createDatabase(path, 4).apply {
            LEGACY_STATUSES.forEachIndexed { index, status ->
                insertLegacySession("legacy-$index", status)
            }
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            path,
            5,
            true,
            TrainFlowDatabase.MIGRATION_4_5
        )

        migrated.query(
            """
            SELECT status, timeline_version, last_durable_offset_ms,
                   last_mutation_sequence, trusted_end_offset_ms, terminal_reason,
                   display_metadata_contract_version, session_display_metadata_json
            FROM workout_sessions
            ORDER BY id
            """.trimIndent()
        ).use { cursor ->
            var rows = 0
            while (cursor.moveToNext()) {
                assertTrue(LEGACY_STATUSES.contains(cursor.getString(0)))
                for (column in 1..7) assertTrue(cursor.isNull(column))
                rows++
            }
            assertEquals(LEGACY_STATUSES.size, rows)
        }
    }

    @Test
    fun migrationCreatesExactCanonicalColumnsForeignKeysAndIndexes() {
        val migrated = migrateEmptyVersionFour("schema-shape")

        assertEquals(
            setOf(
                "workout_phase_intervals",
                "heart_rate_recordings",
                "heart_rate_acquisition_intervals",
                "heart_rate_samples",
                "heart_rate_analysis_snapshots"
            ),
            migrated.tableNames().intersect(CANONICAL_TABLES)
        )
        assertEquals(
            setOf(
                "timeline_version",
                "last_durable_offset_ms",
                "last_mutation_sequence",
                "trusted_end_offset_ms",
                "terminal_reason",
                "display_metadata_contract_version",
                "session_display_metadata_json"
            ),
            migrated.columnNames("workout_sessions").intersect(CANONICAL_SESSION_COLUMNS)
        )

        assertForeignKey(migrated, "workout_phase_intervals", "workout_sessions", "session_id", "id")
        assertForeignKey(migrated, "heart_rate_recordings", "workout_sessions", "session_id", "id")
        assertForeignKey(
            migrated,
            "heart_rate_acquisition_intervals",
            "heart_rate_recordings",
            "recording_id",
            "recording_id"
        )
        assertForeignKey(migrated, "heart_rate_samples", "heart_rate_recordings", "recording_id", "recording_id")
        assertForeignKey(
            migrated,
            "heart_rate_analysis_snapshots",
            "heart_rate_recordings",
            "recording_id",
            "recording_id"
        )

        assertTrue("index_workout_phase_intervals_session_start" in migrated.indexNames("workout_phase_intervals"))
        assertTrue("index_hr_acquisition_recording_start" in migrated.indexNames("heart_rate_acquisition_intervals"))
        assertTrue("index_hr_samples_canonical_order" in migrated.indexNames("heart_rate_samples"))
    }

    @Test
    fun freshVersionFiveAndRealVersionFourMigrationUseIdenticalCanonicalDdl() {
        val migrated = migrateEmptyVersionFour("schema-equivalence")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fresh = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .addCallback(TrainFlowDatabase.CANONICAL_SCHEMA_V5_ON_CREATE)
            .allowMainThreadQueries()
            .build()

        try {
            val freshSchema = fresh.openHelper.writableDatabase.canonicalSchemaSql()
            assertEquals(migrated.canonicalSchemaSql(), freshSchema)
        } finally {
            fresh.close()
        }
    }

    @Test
    fun ddlRejectsInvalidOpenRowsLiteralsAndDuplicateOpenMarkers() {
        val db = migrateEmptyVersionFour("constraints")
        db.insertLegacySession("session", "active")

        db.execSQL(
            """
            INSERT INTO workout_phase_intervals(
                id, session_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                phase_kind, phase_identity_json
            ) VALUES('phase-open', 'session', 0, 0, NULL, 0, NULL, 1, 'timed_work', '{}')
            """.trimIndent()
        )

        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO workout_phase_intervals(
                    id, session_id, sequence, start_offset_ms, end_offset_ms,
                    start_mutation_sequence, end_mutation_sequence, open_marker,
                    phase_kind, phase_identity_json
                ) VALUES('phase-second-open', 'session', 1, 10, NULL, 1, NULL, 1, 'timed_rest', '{}')
                """.trimIndent()
            )
        }
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO workout_phase_intervals(
                    id, session_id, sequence, start_offset_ms, end_offset_ms,
                    start_mutation_sequence, end_mutation_sequence, open_marker,
                    phase_kind, phase_identity_json
                ) VALUES('phase-bad-kind', 'session', 2, 20, 21, 2, 3, NULL, 'unknown', '{}')
                """.trimIndent()
            )
        }
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO workout_phase_intervals(
                    id, session_id, sequence, start_offset_ms, end_offset_ms,
                    start_mutation_sequence, end_mutation_sequence, open_marker,
                    phase_kind, phase_identity_json
                ) VALUES('phase-bad-tuple', 'session', 3, 20, 20, 4, 4, NULL, 'timed_rest', '{}')
                """.trimIndent()
            )
        }
    }

    @Test
    fun ddlEnforcesSessionRecordingSampleAndAnalysisConstraints() {
        val db = migrateEmptyVersionFour("constraint-matrix")

        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO workout_sessions(
                    id, mode, status, plan_snapshot_json, timeline_version
                ) VALUES('invalid-header', 'timed', 'active', '{}', 2)
                """.trimIndent()
            )
        }

        db.insertLegacySession("session", "active")
        db.insertActiveRecording("recording", "session")
        assertThrows(SQLiteConstraintException::class.java) {
            db.insertActiveRecording("second-recording", "session")
        }
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                """
                INSERT INTO heart_rate_samples(
                    recording_id, sample_sequence, offset_ms, mutation_sequence, bpm
                ) VALUES('recording', 0, 0, 0, 0)
                """.trimIndent()
            )
        }
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(
                validAnalysisSnapshotInsert("recording").replace(
                    "'no_canonical_samples', 'no_eligible_duration'",
                    "'primary_points_available', 'no_eligible_duration'"
                )
            )
        }
    }

    @Test
    fun failedCanonicalBatchRollsBackAndSessionDeleteCascadesAllFiveTables() {
        val db = migrateEmptyVersionFour("rollback-cascade")
        db.insertLegacySession("rollback-session", "active")

        db.beginTransaction()
        try {
            db.insertActiveRecording("rollback-recording", "rollback-session")
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL(
                    """
                    INSERT INTO heart_rate_acquisition_intervals(
                        id, recording_id, sequence, start_offset_ms, end_offset_ms,
                        start_mutation_sequence, end_mutation_sequence, open_marker,
                        recording_intent, intent_reason, device_state, device_reason
                    ) VALUES(
                        'bad-acquisition', 'rollback-recording', 0, 0, NULL,
                        0, NULL, 1, 'expected_recording', 'user_turned_off', 'live', NULL
                    )
                    """.trimIndent()
                )
            }
        } finally {
            db.endTransaction()
        }
        assertEquals(0, db.count("heart_rate_recordings"))

        db.insertLegacySession("cascade-session", "completed")
        db.execSQL(
            """
            INSERT INTO workout_phase_intervals(
                id, session_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                phase_kind, phase_identity_json
            ) VALUES('phase', 'cascade-session', 0, 0, 100, 0, 4, NULL, 'timed_work', '{}')
            """.trimIndent()
        )
        db.insertActiveRecording("recording", "cascade-session")
        db.execSQL(
            """
            INSERT INTO heart_rate_acquisition_intervals(
                id, recording_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                recording_intent, intent_reason, device_state, device_reason
            ) VALUES('acquisition', 'recording', 0, 0, NULL, 0, NULL, 1,
                     'expected_recording', NULL, 'live', NULL)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO heart_rate_samples(
                recording_id, sample_sequence, offset_ms, mutation_sequence, bpm
            ) VALUES('recording', 0, 10, 1, 120)
            """.trimIndent()
        )
        db.execSQL(validAnalysisSnapshotInsert("recording"))

        db.execSQL("DELETE FROM workout_sessions WHERE id='cascade-session'")

        CANONICAL_TABLES.forEach { table -> assertEquals(table, 0, db.count(table)) }
        assertEquals(1, db.count("workout_sessions"))
    }

    private fun migrateEmptyVersionFour(name: String): SupportSQLiteDatabase {
        val path = testDatabasePath(name)
        helper.createDatabase(path, 4).close()
        return helper.runMigrationsAndValidate(
            path,
            5,
            true,
            TrainFlowDatabase.MIGRATION_4_5
        ).also { database -> database.setForeignKeyConstraintsEnabled(true) }
    }

    private fun testDatabasePath(name: String): String =
        ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath("$TEST_DB_PREFIX-$name")
            .absolutePath

    private fun SupportSQLiteDatabase.insertLegacySession(id: String, status: String) {
        execSQL(
            """
            INSERT INTO workout_sessions(
                id, plan_id, mode, status, plan_snapshot_json, started_at, ended_at,
                total_elapsed_sec, effective_elapsed_sec, paused_elapsed_sec
            ) VALUES(
                '$id', NULL, 'timed', '$status',
                '{"title":"Legacy","mode":"timed","blocks":[]}',
                NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.insertActiveRecording(recordingId: String, sessionId: String) {
        execSQL(
            """
            INSERT INTO heart_rate_recordings(
                recording_id, session_id, status, started_offset_ms,
                started_mutation_sequence, ended_offset_ms, ended_mutation_sequence,
                source_contract_version, source_kind, acquisition_contract_version,
                parameter_snapshot_version, age, personal_max_bpm, effective_max_bpm,
                effective_max_source, alert_threshold_bpm, zone_snapshot_json,
                original_analysis_version
            ) VALUES(
                '$recordingId', '$sessionId', 'active', 0, 0, NULL, NULL,
                1, 'ble_hrs', 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )
    }

    private fun validAnalysisSnapshotInsert(recordingId: String): String =
        """
        INSERT INTO heart_rate_analysis_snapshots(
            recording_id, analysis_version, created_at, input_last_mutation_sequence,
            sample_status, coverage_status, zone_status,
            canonical_sample_count, primary_point_sample_count,
            eligible_duration_ms, covered_duration_ms, coverage_basis_points,
            weighted_bpm_ms, observed_avg_bpm, observed_max_bpm,
            highest_offset_ms, highest_mutation_sequence, highest_sample_sequence,
            analysis_config_json, zone_durations_json, phase_aggregates_json,
            duration_breakdown_json, quality_reasons_json
        ) VALUES(
            '$recordingId', 1, '2026-08-25T00:00:00Z', 1,
            'no_canonical_samples', 'no_eligible_duration', 'available',
            0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
            '{}', NULL, '{}', '{}', '{}'
        )
        """.trimIndent()

    private fun SupportSQLiteDatabase.tableNames(): Set<String> {
        val result = mutableSetOf<String>()
        query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) result += cursor.getString(0)
        }
        return result
    }

    private fun SupportSQLiteDatabase.columnNames(table: String): Set<String> {
        val result = mutableSetOf<String>()
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) result += cursor.getString(nameIndex)
        }
        return result
    }

    private fun SupportSQLiteDatabase.indexNames(table: String): Set<String> {
        val result = mutableSetOf<String>()
        query("PRAGMA index_list($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) result += cursor.getString(nameIndex)
        }
        return result
    }

    private fun SupportSQLiteDatabase.canonicalSchemaSql(): Map<String, String?> {
        val result = linkedMapOf<String, String?>()
        val tables = SCHEMA_IDENTITY_TABLES.joinToString(",") { table -> "'$table'" }
        query(
            """
            SELECT type, name, sql
            FROM sqlite_master
            WHERE (type = 'table' AND name IN ($tables))
               OR (type = 'index' AND tbl_name IN ($tables))
            ORDER BY type, name
            """.trimIndent()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val key = "${cursor.getString(0)}:${cursor.getString(1)}"
                result[key] = if (cursor.isNull(2)) null else cursor.getString(2)
            }
        }
        return result
    }

    private fun assertForeignKey(
        db: SupportSQLiteDatabase,
        table: String,
        expectedParent: String,
        expectedFrom: String,
        expectedTo: String
    ) {
        var found = false
        db.query("PRAGMA foreign_key_list($table)").use { cursor ->
            val tableIndex = cursor.getColumnIndexOrThrow("table")
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val toIndex = cursor.getColumnIndexOrThrow("to")
            val deleteIndex = cursor.getColumnIndexOrThrow("on_delete")
            while (cursor.moveToNext()) {
                if (
                    cursor.getString(tableIndex) == expectedParent &&
                    cursor.getString(fromIndex) == expectedFrom &&
                    cursor.getString(toIndex) == expectedTo
                ) {
                    assertEquals("CASCADE", cursor.getString(deleteIndex))
                    found = true
                }
            }
        }
        assertTrue("Missing FK $table.$expectedFrom -> $expectedParent.$expectedTo", found)
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB_PREFIX = "canonical-schema-migration"
        val LEGACY_STATUSES = listOf("ready", "active", "paused", "completed", "abandoned")
        val CANONICAL_TABLES = setOf(
            "workout_phase_intervals",
            "heart_rate_recordings",
            "heart_rate_acquisition_intervals",
            "heart_rate_samples",
            "heart_rate_analysis_snapshots"
        )
        val SCHEMA_IDENTITY_TABLES = CANONICAL_TABLES + "workout_sessions"
        val CANONICAL_SESSION_COLUMNS = setOf(
            "timeline_version",
            "last_durable_offset_ms",
            "last_mutation_sequence",
            "trusted_end_offset_ms",
            "terminal_reason",
            "display_metadata_contract_version",
            "session_display_metadata_json"
        )
    }
}
