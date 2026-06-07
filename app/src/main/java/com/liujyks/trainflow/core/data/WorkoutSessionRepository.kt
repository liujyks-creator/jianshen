package com.liujyks.trainflow.core.data

import androidx.room.withTransaction
import com.liujyks.trainflow.core.database.TrainFlowDatabase
import com.liujyks.trainflow.core.database.dao.WorkoutSessionWithRecords
import com.liujyks.trainflow.core.database.entity.SessionStepRecordEntity
import com.liujyks.trainflow.core.database.entity.StrengthSetRecordEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.SessionStepKind
import com.liujyks.trainflow.core.model.SessionStepRecord
import com.liujyks.trainflow.core.model.SetEffort
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetRecord
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WorkoutSessionRepository(
    private val database: TrainFlowDatabase
) {
    private val dao = database.workoutSessionDao()

    val sessions: Flow<List<WorkoutSession>> = dao.observeSessionsWithRecords()
        .map { rows -> rows.map { row -> row.toDomain() } }

    suspend fun upsertSession(session: WorkoutSession) {
        database.withTransaction {
            dao.upsertSession(session.toEntity())
            dao.deleteStepRecordsForSession(session.id)
            dao.deleteStrengthSetRecordsForSession(session.id)
            dao.upsertStepRecords(session.stepHistory.map { record -> record.toEntity(session.id) })
            dao.upsertStrengthSetRecords(session.strengthSetRecords.map { record -> record.toEntity(session.id) })
        }
    }

    suspend fun getSessions(): List<WorkoutSession> {
        return dao.getSessionsWithRecords().map { row -> row.toDomain() }
    }
}

private fun WorkoutSession.toEntity(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = id,
        planId = planId,
        mode = mode.contractValue,
        status = status.contractValue,
        planSnapshotJson = planSnapshot.toStorageJson(),
        startedAt = startedAt,
        endedAt = endedAt,
        totalElapsedSec = totalElapsedSec,
        effectiveElapsedSec = effectiveElapsedSec,
        pausedElapsedSec = pausedElapsedSec
    )
}

private fun SessionStepRecord.toEntity(sessionId: String): SessionStepRecordEntity {
    return SessionStepRecordEntity(
        id = "$sessionId:$stepId",
        sessionId = sessionId,
        stepId = stepId,
        kind = kind.contractValue,
        startedAt = startedAt,
        endedAt = endedAt,
        skipped = skipped,
        actualDurationSec = actualDurationSec
    )
}

private fun StrengthSetRecord.toEntity(sessionId: String): StrengthSetRecordEntity {
    return StrengthSetRecordEntity(
        id = "$sessionId:$id",
        sessionId = sessionId,
        exerciseId = exerciseId,
        sourceSetPlanId = sourceSetPlanId,
        setOrder = setOrder,
        setKind = setKind.contractValue,
        side = side?.contractValue,
        plannedJson = encodePlanned(plannedWeight, plannedRepTarget),
        actualJson = encodeActual(actualWeight, actualReps),
        activeDurationSec = activeDurationSec,
        actualRestAfterSec = actualRestAfterSec,
        effort = effort?.contractValue,
        substitutedFromExerciseId = substitutedFromExerciseId,
        notes = notes
    )
}

private fun WorkoutSessionWithRecords.toDomain(): WorkoutSession {
    val mode = workoutModeFrom(session.mode)
    return WorkoutSession(
        id = session.id,
        planId = session.planId,
        mode = mode,
        planSnapshot = session.planSnapshotJson.toPlanSnapshot(fallbackMode = mode),
        status = sessionStatusFrom(session.status),
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        totalElapsedSec = session.totalElapsedSec,
        effectiveElapsedSec = session.effectiveElapsedSec,
        pausedElapsedSec = session.pausedElapsedSec,
        stepHistory = stepRecords.sortedBy { record -> record.startedAt }.map { record -> record.toDomain() },
        strengthSetRecords = strengthSetRecords.sortedBy { record -> record.setOrder }.map { record -> record.toDomain() }
    )
}

private fun SessionStepRecordEntity.toDomain(): SessionStepRecord {
    return SessionStepRecord(
        stepId = stepId.ifBlank { id.substringAfter(':', id) },
        kind = sessionStepKindFrom(kind),
        startedAt = startedAt,
        endedAt = endedAt,
        skipped = skipped,
        actualDurationSec = actualDurationSec
    )
}

private fun StrengthSetRecordEntity.toDomain(): StrengthSetRecord {
    val planned = plannedJson.decodePlanned()
    val actual = actualJson.decodeActual()
    return StrengthSetRecord(
        id = id.substringAfter(':', id),
        exerciseId = exerciseId,
        sourceSetPlanId = sourceSetPlanId,
        setOrder = setOrder,
        setKind = strengthSetKindFrom(setKind),
        side = side?.let(::exerciseSideFrom),
        plannedWeight = planned.weight,
        plannedRepTarget = planned.repTarget,
        actualWeight = actual.weight,
        actualReps = actual.reps,
        activeDurationSec = activeDurationSec,
        actualRestAfterSec = actualRestAfterSec,
        effort = effort?.let(::setEffortFrom),
        substitutedFromExerciseId = substitutedFromExerciseId,
        notes = notes
    )
}

private fun WorkoutPlanSnapshot.toStorageJson(): String {
    return """{"title":"${title.escapeJson()}","mode":"${mode.contractValue.escapeJson()}"}"""
}

private fun String.toPlanSnapshot(fallbackMode: WorkoutMode): WorkoutPlanSnapshot {
    val title = jsonValue("title")?.unescapeJson()?.ifBlank { null } ?: "未命名训练"
    val mode = jsonValue("mode")?.let(::workoutModeFrom) ?: fallbackMode
    return WorkoutPlanSnapshot(
        title = title,
        mode = mode,
        blocks = emptyList()
    )
}

private fun String.jsonValue(name: String): String? {
    val pattern = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"])*)"""")
    return pattern.find(this)?.groupValues?.getOrNull(1)
}

private data class PlannedSetStorage(
    val weight: WeightValue?,
    val repTarget: RepTarget?
)

private data class ActualSetStorage(
    val weight: WeightValue?,
    val reps: Int?
)

private fun encodePlanned(
    weight: WeightValue?,
    repTarget: RepTarget?
): String? {
    val fields = buildList {
        weight?.let { add("weight=${it.value},${it.unit.contractValue}") }
        when (repTarget) {
            is RepTarget.Fixed -> add("rep=fixed,${repTarget.reps}")
            is RepTarget.Range -> add("rep=range,${repTarget.minReps},${repTarget.maxReps}")
            null -> Unit
        }
    }
    return fields.takeIf { it.isNotEmpty() }?.joinToString("|")
}

private fun encodeActual(
    weight: WeightValue?,
    reps: Int?
): String? {
    val fields = buildList {
        weight?.let { add("weight=${it.value},${it.unit.contractValue}") }
        reps?.let { add("reps=$it") }
    }
    return fields.takeIf { it.isNotEmpty() }?.joinToString("|")
}

private fun String?.decodePlanned(): PlannedSetStorage {
    val fields = toFields()
    return PlannedSetStorage(
        weight = fields["weight"]?.toWeightValue(),
        repTarget = fields["rep"]?.toRepTarget()
    )
}

private fun String?.decodeActual(): ActualSetStorage {
    val fields = toFields()
    return ActualSetStorage(
        weight = fields["weight"]?.toWeightValue(),
        reps = fields["reps"]?.toIntOrNull()
    )
}

private fun String?.toFields(): Map<String, String> {
    return this
        ?.split("|")
        ?.mapNotNull { field ->
            val key = field.substringBefore("=", missingDelimiterValue = "")
            val value = field.substringAfter("=", missingDelimiterValue = "")
            if (key.isBlank()) null else key to value
        }
        ?.toMap()
        ?: emptyMap()
}

private fun String.toWeightValue(): WeightValue? {
    val parts = split(",")
    val value = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
    val unit = parts.getOrNull(1)?.let(::weightUnitFrom) ?: return null
    return WeightValue(value = value, unit = unit)
}

private fun String.toRepTarget(): RepTarget? {
    val parts = split(",")
    return when (parts.getOrNull(0)) {
        "fixed" -> parts.getOrNull(1)?.toIntOrNull()?.let { reps -> RepTarget.Fixed(reps) }
        "range" -> {
            val min = parts.getOrNull(1)?.toIntOrNull()
            val max = parts.getOrNull(2)?.toIntOrNull()
            if (min != null && max != null) RepTarget.Range(min, max) else null
        }
        else -> null
    }
}

private fun workoutModeFrom(value: String): WorkoutMode {
    return WorkoutMode.entries.firstOrNull { mode -> mode.contractValue == value } ?: WorkoutMode.TIMED
}

private fun sessionStatusFrom(value: String): SessionStatus {
    return SessionStatus.entries.firstOrNull { status -> status.contractValue == value } ?: SessionStatus.COMPLETED
}

private fun sessionStepKindFrom(value: String): SessionStepKind {
    return SessionStepKind.entries.firstOrNull { kind -> kind.contractValue == value } ?: SessionStepKind.TIMED_WORK
}

private fun strengthSetKindFrom(value: String): StrengthSetKind {
    return StrengthSetKind.entries.firstOrNull { kind -> kind.contractValue == value } ?: StrengthSetKind.WORKING
}

private fun exerciseSideFrom(value: String): ExerciseSide {
    return ExerciseSide.entries.firstOrNull { side -> side.contractValue == value } ?: ExerciseSide.BOTH
}

private fun setEffortFrom(value: String): SetEffort {
    return SetEffort.entries.firstOrNull { effort -> effort.contractValue == value } ?: SetEffort.GOOD
}

private fun weightUnitFrom(value: String): WeightUnit? {
    return WeightUnit.entries.firstOrNull { unit -> unit.contractValue == value }
}

private fun String.escapeJson(): String {
    return buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

private fun String.unescapeJson(): String {
    return replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}
