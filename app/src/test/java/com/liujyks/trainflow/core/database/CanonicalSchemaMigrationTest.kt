package com.liujyks.trainflow.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.database.entity.HeartRateAcquisitionIntervalEntity
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.HeartRateSampleEntity
import com.liujyks.trainflow.core.database.entity.WorkoutPhaseIntervalEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.runBlocking
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
    fun versionFiveMigrationPreservesLegacyRowsAndValidCanonicalGraphsWithoutBackfill() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath(TrainFlowDatabase.DATABASE_NAME).absolutePath
        val old = helper.createDatabase(path, 5)
        listOf("timed", "strength", "follow_along").forEachIndexed { index, mode ->
            old.insertLegacySession("legacy-$index", "completed")
            old.execSQL(
                "UPDATE workout_sessions SET mode=?, plan_snapshot_json=?, started_at=?, ended_at=? WHERE id=?",
                arrayOf(mode, "{ \"title\":\"旧记录 $mode\", \"mode\":\"$mode\", \"blocks\":[] }",
                    "2026-01-01T20:30:00Z", "2026-01-01T20:31:00Z", "legacy-$index")
            )
        }
        old.execSQL("INSERT INTO session_step_records(id, session_id, step_id, kind, started_at, skipped) VALUES('step', 'legacy-0', 'work', 'timed_work', '2026-01-01T20:30:00Z', 0)")
        old.execSQL("INSERT INTO strength_set_records(id, session_id, exercise_id, set_order, set_kind, planned_json, actual_json) VALUES('set', 'legacy-1', 'squat', 1, 'working', 'weight=60.0,kg|rep=range,8,12', 'weight=60.0,kg|reps=8')")
        old.execSQL("INSERT INTO timed_rest_extension_records(id, session_id, step_id, step_index, rest_stage_title, added_sec, planned_rest_sec, rest_elapsed_before_extension_sec, extension_at_remaining_sec, cumulative_extra_rest_sec, event_elapsed_sec) VALUES('rest', 'legacy-0', 'rest-step', 1, '休息', 15, 30, 5, 25, 15, 35)")

        val planJson = """{"planSnapshotStorageContractVersion":1,"planId":null,"title":"Timed","mode":"timed","blocks":[{"id":"block","kind":"timed_composition","order":0,"compositionVersion":2,"warmupSec":10,"cooldownSec":0,"rounds":1,"restBetweenRoundsSec":0,"stageGroups":[]}],"preferences":null,"followAlong":null}"""
        val displayJson = """{"displayMetadataContractVersion":1,"entries":[]}"""
        val phaseJson = """{"phaseIdentityContractVersion":1,"family":"timed_composition_v2","payloadVersion":2,"mode":"timed","phaseKind":"timed_work","orderedStructureSignature":{"signatureContractVersion":1,"algorithm":"sha256","digestHexLowercase":"38376293776bcfc20b092f80441fbde7344ef1b837e0f5ba2c7fc28f6b6a5855"},"payload":{"variant":"warmup","compositionVersion":2,"compositionBlockId":"block","timelineStageId":"block:warmup","timelineStageKind":"warmup","stageGroupId":"block:warmup","targetId":"block:warmup:target","targetKind":"warmup","roundIndex0":null,"stageGroupIndex0":null,"targetIndex0":0,"stageInstanceIndex0":0,"targetInstanceIndex0":0,"stepIndex0":0}}"""
        for (terminal in listOf(false, true)) {
            val id = if (terminal) "terminal" else "active"
            val endOffset = if (terminal) 100L else null
            val endSequence = if (terminal) 4L else null
            val open = if (terminal) null else 1
            val session = WorkoutSessionEntity(
                id = id, mode = "timed", status = if (terminal) "completed" else "active",
                planSnapshotJson = planJson, startedAt = "2026-01-01T20:30:00Z",
                endedAt = if (terminal) "2026-01-01T20:30:00.100Z" else null,
                timelineVersion = 1, lastDurableOffsetMs = 100, lastMutationSequence = 4,
                trustedEndOffsetMs = endOffset, terminalReason = if (terminal) "completed" else null,
                displayMetadataContractVersion = 1, sessionDisplayMetadataJson = displayJson
            )
            val phase = WorkoutPhaseIntervalEntity(
                id = "phase-$id", sessionId = id, sequence = 0,
                startOffsetMs = 0, endOffsetMs = endOffset, startMutationSequence = 0,
                endMutationSequence = endSequence, openMarker = open,
                phaseKind = "timed_work", phaseIdentityJson = phaseJson
            )
            val recording = HeartRateRecordingEntity(
                recordingId = "recording-$id", sessionId = id,
                status = if (terminal) "terminal" else "active",
                startedOffsetMs = 0, startedMutationSequence = 0,
                endedOffsetMs = endOffset, endedMutationSequence = endSequence,
                sourceContractVersion = 1, sourceKind = "ble_hrs", acquisitionContractVersion = 1,
                parameterSnapshotVersion = 1, originalAnalysisVersion = if (terminal) 1 else null
            )
            val acquisition = HeartRateAcquisitionIntervalEntity(
                id = "acquisition-$id", recordingId = recording.recordingId, sequence = 0,
                startOffsetMs = 0, endOffsetMs = endOffset, startMutationSequence = 0,
                endMutationSequence = endSequence, openMarker = open,
                recordingIntent = "expected_recording", intentReason = null,
                deviceState = "live", deviceReason = null
            )
            val sample = HeartRateSampleEntity(recording.recordingId, 0, 0, 0, 120)
            val input = CanonicalSessionGraphV1(session, listOf(phase), recording, listOf(acquisition), listOf(sample), emptyList())
            // Existing analysis builds the historical fixture; preservation is judged against its pre-migration bytes.
            val snapshots = if (terminal) listOf(CanonicalAnalysisV1.derive(input, "2026-01-01T20:30:01Z")) else emptyList()
            assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(input.copy(snapshots = snapshots)))
            old.execSQL(
                "INSERT INTO workout_sessions(id, mode, status, plan_snapshot_json, started_at, ended_at, timeline_version, last_durable_offset_ms, last_mutation_sequence, trusted_end_offset_ms, terminal_reason, display_metadata_contract_version, session_display_metadata_json) VALUES(?, ?, ?, ?, ?, ?, 1, 100, 4, ?, ?, 1, ?)",
                arrayOf<Any?>(id, session.mode, session.status, planJson, session.startedAt, session.endedAt, endOffset, session.terminalReason, displayJson)
            )
            old.execSQL("INSERT INTO workout_phase_intervals VALUES(?, ?, 0, 0, ?, 0, ?, ?, 'timed_work', ?)",
                arrayOf<Any?>(phase.id, id, endOffset, endSequence, open, phaseJson))
            old.execSQL("INSERT INTO heart_rate_recordings VALUES(?, ?, ?, 0, 0, ?, ?, 1, 'ble_hrs', 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, ?)",
                arrayOf<Any?>(recording.recordingId, id, recording.status, endOffset, endSequence, recording.originalAnalysisVersion))
            old.execSQL("INSERT INTO heart_rate_acquisition_intervals VALUES(?, ?, 0, 0, ?, 0, ?, ?, 'expected_recording', NULL, 'live', NULL)",
                arrayOf<Any?>(acquisition.id, recording.recordingId, endOffset, endSequence, open))
            old.execSQL("INSERT INTO heart_rate_samples VALUES(?, 0, 0, 0, 120)", arrayOf(recording.recordingId))
            snapshots.forEach { s ->
                old.execSQL("INSERT INTO heart_rate_analysis_snapshots VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(s.recordingId, s.analysisVersion, s.createdAt, s.inputLastMutationSequence,
                        s.sampleStatus, s.coverageStatus, s.zoneStatus, s.canonicalSampleCount, s.primaryPointSampleCount,
                        s.eligibleDurationMs, s.coveredDurationMs, s.coverageBasisPoints, s.weightedBpmMs,
                        s.observedAvgBpm, s.observedMaxBpm, s.highestOffsetMs, s.highestMutationSequence, s.highestSampleSequence,
                        s.analysisConfigJson, s.zoneDurationsJson, s.phaseAggregatesJson, s.durationBreakdownJson, s.qualityReasonsJson))
            }
        }
        val tables = old.tableNames() - setOf("android_metadata", "room_master_table", "sqlite_sequence")
        assertEquals(13, tables.size)
        val originalColumns = tables.associateWith { old.columnNames(it).toList() }
        val before = originalColumns.mapValues { (table, columns) ->
            old.query("SELECT ${columns.joinToString(",")} FROM $table ORDER BY 1, 2").use { cursor ->
                buildList { while (cursor.moveToNext()) add(columns.indices.map { cursor.getType(it) to cursor.getString(it) }) }
            }
        }
        assertEquals(5, before.getValue("workout_sessions").size)
        assertEquals(2, before.getValue("heart_rate_samples").size)
        assertEquals(1, before.getValue("heart_rate_analysis_snapshots").size)
        old.close()
        helper.runMigrationsAndValidate(path, 6, true, TrainFlowDatabase.MIGRATION_5_6).close()
        val upgraded = TrainFlowDatabase.create(context)
        try {
            val sql = upgraded.openHelper.writableDatabase
            assertEquals(6, sql.version)
            originalColumns.forEach { (table, columns) ->
                val after = sql.query("SELECT ${columns.joinToString(",")} FROM $table ORDER BY 1, 2").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(columns.indices.map { cursor.getType(it) to cursor.getString(it) }) }
                }
                assertEquals("all original logical rows, raw JSON and bindings: $table", before.getValue(table), after)
            }
            sql.query("SELECT start_local_date, start_zone_id, start_utc_offset_seconds, time_metadata_source_contract_version FROM workout_sessions").use { cursor ->
                assertEquals(5, cursor.count)
                while (cursor.moveToNext()) for (column in 0..3) assertTrue(cursor.isNull(column))
            }
            for (id in listOf("active", "terminal")) {
                val rows = requireNotNull(upgraded.canonicalTimelineHeartRateDao().canonicalGraphRows(id))
                val recording = rows.recordings.single()
                assertEquals(CanonicalValidationResult.Valid, CanonicalSessionGraphV1Validator.validate(
                    CanonicalSessionGraphV1(rows.session, rows.phases, recording.recording,
                        recording.acquisitions, recording.samples, recording.snapshots)
                ))
            }
            val sessions = WorkoutSessionRepository(upgraded).getSessions()
            assertEquals(5, sessions.size)
            sessions.forEach { session ->
                assertEquals(null, session.startLocalDate)
                assertEquals(null, session.startZoneId)
                assertEquals(null, session.startUtcOffsetSeconds)
                assertEquals(null, session.timeMetadataSourceContractVersion)
            }
        } finally {
            upgraded.close()
        }
    }

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

        EXPECTED_COLUMNS.forEach { (table, columns) ->
            assertEquals("columns/nullability/PK for $table", columns, migrated.columnInventory(table))
        }
        EXPECTED_INDEXES.forEach { (table, indexes) ->
            assertEquals("explicit indexes for $table", indexes, migrated.explicitIndexInventory(table))
        }

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
    fun freshVersionSixAndRealVersionFourUpgradeUseIdenticalCanonicalSchema() {
        migrateEmptyVersionFour("schema-equivalence").close()
        val migrated = helper.runMigrationsAndValidate(
            testDatabasePath("schema-equivalence"), 6, true, TrainFlowDatabase.MIGRATION_5_6
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fresh = Room.inMemoryDatabaseBuilder(context, TrainFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val freshSchema = fresh.openHelper.writableDatabase.canonicalSchemaSql()
            // SQLite preserves ADD COLUMN identifier quoting; all other DDL remains exact.
            assertEquals(
                migrated.canonicalSchemaSql().mapValues { (_, sql) -> sql?.replace("`", "") },
                freshSchema.mapValues { (_, sql) -> sql?.replace("`", "") }
            )
        } finally {
            fresh.close()
        }
    }

    @Test
    fun roomExportedPhysicalSchemaContainsNoUnrepresentableChecks() {
        val migrated = migrateEmptyVersionFour("physical-authority")

        migrated.canonicalSchemaSql().forEach { (identity, sql) ->
            assertFalse("$identity unexpectedly contains CHECK: $sql", sql?.contains("CHECK", true) == true)
        }
    }

    @Test
    fun physicalUniqueOpenMarkersAndPureValidatorsRejectSemanticRows() {
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
        db.execSQL(
            """
            INSERT INTO workout_phase_intervals(
                id, session_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                phase_kind, phase_identity_json
            ) VALUES('phase-bad-kind', 'session', 2, 20, 21, 2, 3, NULL, 'unknown', '{}')
            """.trimIndent()
        )
        assertTrue(PhaseIdentityV1Validator.validateStructure("{}") is CanonicalValidationResult.Invalid)
        db.execSQL(
            """
            INSERT INTO workout_phase_intervals(
                id, session_id, sequence, start_offset_ms, end_offset_ms,
                start_mutation_sequence, end_mutation_sequence, open_marker,
                phase_kind, phase_identity_json
            ) VALUES('phase-bad-tuple', 'session', 3, 20, 20, 4, 4, NULL, 'timed_rest', '{}')
            """.trimIndent()
        )
        assertFalse(validCanonicalIntervalEnd(20, 4, 20, 4, null))
    }

    @Test
    fun roomPhysicalConstraintsAndPureValidatorsCoverHeaderSampleAndAnalysisRules() {
        val db = migrateEmptyVersionFour("constraint-matrix")

        db.execSQL(
            """
            INSERT INTO workout_sessions(
                id, mode, status, plan_snapshot_json, timeline_version
            ) VALUES('invalid-header', 'timed', 'active', '{}', 2)
            """.trimIndent()
        )
        assertTrue(
            CanonicalSessionHeaderV1Validator.validate(
                WorkoutSessionEntity(
                    id = "invalid-header",
                    mode = "timed",
                    status = "active",
                    planSnapshotJson = "{}",
                    timelineVersion = 2
                )
            ) is CanonicalSessionHeaderV1Result.Invalid
        )

        db.insertLegacySession("session", "active")
        db.insertActiveRecording("recording", "session")
        assertThrows(SQLiteConstraintException::class.java) {
            db.insertActiveRecording("second-recording", "session")
        }
        db.execSQL(
            """
            INSERT INTO heart_rate_samples(
                recording_id, sample_sequence, offset_ms, mutation_sequence, bpm
            ) VALUES('recording', 0, 0, 0, 0)
            """.trimIndent()
        )
        assertFalse(
            CanonicalSessionGraphV1Validator.validateCanonicalSamples(
                samples = listOf(HeartRateSampleEntity("recording", 0, 0, 0, 0)),
                recordingId = "recording",
                recordingStart = CanonicalTuple(0, 0),
                inputCut = CanonicalTuple(1, 1)
            )
        )
        db.execSQL(
            validAnalysisSnapshotInsert("recording").replace(
                "'no_canonical_samples', 'no_eligible_duration'",
                "'primary_points_available', 'no_eligible_duration'"
            )
        )
    }

    @Test
    fun failedCanonicalBatchRollsBackAndSessionDeleteCascadesAllFiveTables() {
        val db = migrateEmptyVersionFour("rollback-cascade")
        db.insertLegacySession("rollback-session", "active")

        db.beginTransaction()
        try {
            db.insertActiveRecording("rollback-recording", "rollback-session")
            db.execSQL(
                """
                INSERT INTO heart_rate_acquisition_intervals(
                    id, recording_id, sequence, start_offset_ms, end_offset_ms,
                    start_mutation_sequence, end_mutation_sequence, open_marker,
                    recording_intent, intent_reason, device_state, device_reason
                ) VALUES(
                    'first-acquisition', 'rollback-recording', 0, 0, NULL,
                    0, NULL, 1, 'expected_recording', NULL, 'live', NULL
                )
                """.trimIndent()
            )
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL(
                    """
                    INSERT INTO heart_rate_acquisition_intervals(
                        id, recording_id, sequence, start_offset_ms, end_offset_ms,
                        start_mutation_sequence, end_mutation_sequence, open_marker,
                        recording_intent, intent_reason, device_state, device_reason
                    ) VALUES(
                        'bad-acquisition', 'rollback-recording', 0, 0, NULL,
                        0, NULL, 1, 'expected_recording', NULL, 'live', NULL
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

    private fun SupportSQLiteDatabase.columnInventory(table: String): Map<String, Pair<Boolean, Int>> {
        val result = linkedMapOf<String, Pair<Boolean, Int>>()
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                result[cursor.getString(nameIndex)] =
                    (cursor.getInt(notNullIndex) == 1) to cursor.getInt(primaryKeyIndex)
            }
        }
        return result
    }

    private fun SupportSQLiteDatabase.explicitIndexInventory(
        table: String
    ): Map<String, Pair<Boolean, List<String>>> {
        val indexes = linkedMapOf<String, Pair<Boolean, List<String>>>()
        query("PRAGMA index_list($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
            val originIndex = cursor.getColumnIndexOrThrow("origin")
            while (cursor.moveToNext()) {
                if (cursor.getString(originIndex) != "c") continue
                val name = cursor.getString(nameIndex)
                val columns = mutableListOf<String>()
                query("PRAGMA index_info(`$name`)").use { columnCursor ->
                    val columnNameIndex = columnCursor.getColumnIndexOrThrow("name")
                    while (columnCursor.moveToNext()) columns += columnCursor.getString(columnNameIndex)
                }
                indexes[name] = (cursor.getInt(uniqueIndex) == 1) to columns
            }
        }
        return indexes
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
        val EXPECTED_COLUMNS = linkedMapOf(
            "workout_sessions" to linkedMapOf(
                "id" to (true to 1), "plan_id" to (false to 0), "mode" to (true to 0),
                "status" to (true to 0), "plan_snapshot_json" to (true to 0),
                "started_at" to (false to 0), "ended_at" to (false to 0),
                "total_elapsed_sec" to (false to 0), "effective_elapsed_sec" to (false to 0),
                "paused_elapsed_sec" to (false to 0), "timeline_version" to (false to 0),
                "last_durable_offset_ms" to (false to 0), "last_mutation_sequence" to (false to 0),
                "trusted_end_offset_ms" to (false to 0), "terminal_reason" to (false to 0),
                "display_metadata_contract_version" to (false to 0),
                "session_display_metadata_json" to (false to 0)
            ),
            "workout_phase_intervals" to linkedMapOf(
                "id" to (true to 1), "session_id" to (true to 0), "sequence" to (true to 0),
                "start_offset_ms" to (true to 0), "end_offset_ms" to (false to 0),
                "start_mutation_sequence" to (true to 0), "end_mutation_sequence" to (false to 0),
                "open_marker" to (false to 0), "phase_kind" to (true to 0),
                "phase_identity_json" to (true to 0)
            ),
            "heart_rate_recordings" to linkedMapOf(
                "recording_id" to (true to 1), "session_id" to (true to 0),
                "status" to (true to 0), "started_offset_ms" to (true to 0),
                "started_mutation_sequence" to (true to 0), "ended_offset_ms" to (false to 0),
                "ended_mutation_sequence" to (false to 0), "source_contract_version" to (true to 0),
                "source_kind" to (true to 0), "acquisition_contract_version" to (true to 0),
                "parameter_snapshot_version" to (true to 0), "age" to (false to 0),
                "personal_max_bpm" to (false to 0), "effective_max_bpm" to (false to 0),
                "effective_max_source" to (false to 0), "alert_threshold_bpm" to (false to 0),
                "zone_snapshot_json" to (false to 0), "original_analysis_version" to (false to 0)
            ),
            "heart_rate_acquisition_intervals" to linkedMapOf(
                "id" to (true to 1), "recording_id" to (true to 0), "sequence" to (true to 0),
                "start_offset_ms" to (true to 0), "end_offset_ms" to (false to 0),
                "start_mutation_sequence" to (true to 0), "end_mutation_sequence" to (false to 0),
                "open_marker" to (false to 0), "recording_intent" to (true to 0),
                "intent_reason" to (false to 0), "device_state" to (true to 0),
                "device_reason" to (false to 0)
            ),
            "heart_rate_samples" to linkedMapOf(
                "recording_id" to (true to 1), "sample_sequence" to (true to 2),
                "offset_ms" to (true to 0), "mutation_sequence" to (true to 0),
                "bpm" to (true to 0)
            ),
            "heart_rate_analysis_snapshots" to linkedMapOf(
                "recording_id" to (true to 1), "analysis_version" to (true to 2),
                "created_at" to (true to 0), "input_last_mutation_sequence" to (true to 0),
                "sample_status" to (true to 0), "coverage_status" to (true to 0),
                "zone_status" to (true to 0), "canonical_sample_count" to (true to 0),
                "primary_point_sample_count" to (true to 0), "eligible_duration_ms" to (false to 0),
                "covered_duration_ms" to (false to 0), "coverage_basis_points" to (false to 0),
                "weighted_bpm_ms" to (false to 0), "observed_avg_bpm" to (false to 0),
                "observed_max_bpm" to (false to 0), "highest_offset_ms" to (false to 0),
                "highest_mutation_sequence" to (false to 0), "highest_sample_sequence" to (false to 0),
                "analysis_config_json" to (true to 0), "zone_durations_json" to (false to 0),
                "phase_aggregates_json" to (true to 0), "duration_breakdown_json" to (true to 0),
                "quality_reasons_json" to (true to 0)
            )
        )
        val EXPECTED_INDEXES = linkedMapOf(
            "workout_sessions" to linkedMapOf(
                "index_workout_sessions_plan_id" to (false to listOf("plan_id"))
            ),
            "workout_phase_intervals" to linkedMapOf(
                "index_workout_phase_intervals_session_start" to
                    (false to listOf("session_id", "start_offset_ms")),
                "index_workout_phase_intervals_session_open_marker" to
                    (true to listOf("session_id", "open_marker")),
                "index_workout_phase_intervals_session_sequence" to
                    (true to listOf("session_id", "sequence"))
            ),
            "heart_rate_recordings" to linkedMapOf(
                "index_heart_rate_recordings_session_id" to (true to listOf("session_id"))
            ),
            "heart_rate_acquisition_intervals" to linkedMapOf(
                "index_hr_acquisition_recording_start" to
                    (false to listOf("recording_id", "start_offset_ms")),
                "index_hr_acquisition_recording_open_marker" to
                    (true to listOf("recording_id", "open_marker")),
                "index_hr_acquisition_recording_sequence" to
                    (true to listOf("recording_id", "sequence"))
            ),
            "heart_rate_samples" to linkedMapOf(
                "index_hr_samples_canonical_order" to
                    (false to listOf("recording_id", "offset_ms", "mutation_sequence", "sample_sequence"))
            ),
            "heart_rate_analysis_snapshots" to emptyMap()
        )
    }
}
