package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.CountdownCue
import com.liujyks.trainflow.core.model.CueSettings
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.FollowAlongPlanMeta
import com.liujyks.trainflow.core.model.HeartRateDisplayPreference
import com.liujyks.trainflow.core.model.PlanBlock
import com.liujyks.trainflow.core.model.PlanBlockKind
import com.liujyks.trainflow.core.model.PlanReminder
import com.liujyks.trainflow.core.model.PlanPreferences
import com.liujyks.trainflow.core.model.RepTarget
import com.liujyks.trainflow.core.model.RestBlock
import com.liujyks.trainflow.core.model.StrengthExerciseBlock
import com.liujyks.trainflow.core.model.StrengthExerciseTarget
import com.liujyks.trainflow.core.model.StrengthSetKind
import com.liujyks.trainflow.core.model.StrengthSetPlan
import com.liujyks.trainflow.core.model.StrengthSetTimerMode
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TIMED_COMPOSITION_CURRENT_VERSION
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilityMeta
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilitySourceVersion
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WeightUnit
import com.liujyks.trainflow.core.model.WeightValue
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.WorkoutPlanSnapshot
import com.liujyks.trainflow.core.model.normalized
import com.liujyks.trainflow.core.model.normalizeStageColorHex

internal fun WorkoutPlan.toPlanSnapshot(): WorkoutPlanSnapshot {
    return WorkoutPlanSnapshot(
        planId = id,
        title = title,
        mode = mode,
        blocks = blocks,
        preferences = preferences,
        followAlong = followAlong
    )
}

internal fun List<PlanBlock>.toPlanBlocksStorageJson(): String {
    return map { block -> block.toJson() }.jsonArray().render()
}

internal fun String.toPlanBlocksStorage(): List<PlanBlock> {
    val root = runCatching { JsonParser(this).parse() }.getOrNull() as? JsonValue.Arr
        ?: return emptyList()
    return root.values.mapNotNull { value -> value.toPlanBlock() }
}

internal fun PlanReminder.toStorageJson(): String {
    return jsonObject(
        "enabled" to enabled.jsonBool(),
        "scheduleAt" to scheduleAt?.jsonString(),
        "repeatRule" to repeatRule?.jsonString()
    ).render()
}

internal fun String.toPlanReminderStorage(): PlanReminder? {
    val root = runCatching { JsonParser(this).parse() }.getOrNull() as? JsonValue.Obj
        ?: return null
    return PlanReminder(
        enabled = root.boolean("enabled") ?: false,
        scheduleAt = root.string("scheduleAt"),
        repeatRule = root.string("repeatRule")
    )
}

internal fun PlanPreferences.toStorageJson(): String {
    return toJson().render()
}

internal fun String.toPlanPreferencesStorage(): PlanPreferences? {
    val root = runCatching { JsonParser(this).parse() }.getOrNull() as? JsonValue.Obj
        ?: return null
    return root.toPlanPreferences()
}

internal fun FollowAlongPlanMeta.toStorageJson(): String {
    return toJson().render()
}

internal fun String.toFollowAlongMetaStorage(): FollowAlongPlanMeta? {
    val root = runCatching { JsonParser(this).parse() }.getOrNull() as? JsonValue.Obj
        ?: return null
    return root.toFollowAlongMeta()
}

internal fun WorkoutPlanSnapshot.toStorageJson(): String {
    return jsonObject(
        "planId" to planId?.jsonString(),
        "title" to title.jsonString(),
        "mode" to mode.contractValue.jsonString(),
        "blocks" to blocks.map { block -> block.toJson() }.jsonArray(),
        "preferences" to preferences?.toJson(),
        "followAlong" to followAlong?.toJson()
    ).render()
}

internal fun String.toPlanSnapshot(fallbackMode: WorkoutMode): WorkoutPlanSnapshot {
    val root = runCatching { JsonParser(this).parse() }.getOrNull() as? JsonValue.Obj
        ?: return WorkoutPlanSnapshot(
            title = "未命名训练",
            mode = fallbackMode,
            blocks = emptyList()
        )
    val title = root.string("title")?.ifBlank { null } ?: "未命名训练"
    val mode = root.string("mode")?.let(::workoutModeFrom) ?: fallbackMode
    return WorkoutPlanSnapshot(
        planId = root.string("planId"),
        title = title,
        mode = mode,
        blocks = root.array("blocks")?.mapNotNull { value -> value.toPlanBlock() } ?: emptyList(),
        preferences = root.obj("preferences")?.toPlanPreferences(),
        followAlong = root.obj("followAlong")?.toFollowAlongMeta()
    )
}

private fun PlanBlock.toJson(): JsonValue.Obj {
    return when (this) {
        is WarmupBlock -> jsonObject(
            commonBlockFields(),
            "durationSec" to durationSec?.jsonNumber(),
            "items" to items.map { item -> item.toJson() }.jsonArray()
        )

        is StretchBlock -> jsonObject(
            commonBlockFields(),
            "durationSec" to durationSec?.jsonNumber(),
            "items" to items.map { item -> item.toJson() }.jsonArray()
        )

        is CooldownBlock -> jsonObject(
            commonBlockFields(),
            "durationSec" to durationSec?.jsonNumber(),
            "items" to items.map { item -> item.toJson() }.jsonArray()
        )

        is RestBlock -> jsonObject(
            commonBlockFields(),
            "durationSec" to durationSec.jsonNumber(),
            "label" to label?.jsonString()
        )

        is TimedCircuitBlock -> jsonObject(
            commonBlockFields(),
            "rounds" to rounds.jsonNumber(),
            "restBetweenRoundsSec" to restBetweenRoundsSec?.jsonNumber(),
            "items" to items.map { item -> item.toJson() }.jsonArray()
        )

        is TimedCompositionBlock -> normalized().let { block ->
            jsonObject(
                block.commonBlockFields(),
                "compositionVersion" to block.compositionVersion.jsonNumber(),
                "warmupSec" to block.warmupSec.jsonNumber(),
                "warmupStyle" to block.warmupStyle?.toJson(),
                "cooldownSec" to block.cooldownSec.jsonNumber(),
                "cooldownStyle" to block.cooldownStyle?.toJson(),
                "rounds" to block.rounds.jsonNumber(),
                "restBetweenRoundsSec" to block.restBetweenRoundsSec.jsonNumber(),
                "restBetweenRoundsStyle" to block.restBetweenRoundsStyle?.toJson(),
                "stageGroups" to block.stageGroups.map { group -> group.toJson() }.jsonArray(),
                "compatibility" to block.compatibility?.toJson()
            )
        }

        is StrengthExerciseBlock -> jsonObject(
            commonBlockFields(),
            "exerciseId" to exerciseId.jsonString(),
            "target" to target?.toJson(),
            "sets" to sets.map { set -> set.toJson() }.jsonArray(),
            "substitutions" to substitutions.map { it.jsonString() }.jsonArray(),
            "setTimerMode" to setTimerMode.contractValue.jsonString()
        )
    }
}

private fun PlanBlock.commonBlockFields(): Map<String, JsonValue?> {
    return mapOf(
        "id" to id.jsonString(),
        "kind" to kind.contractValue.jsonString(),
        "title" to title?.jsonString(),
        "order" to order.jsonNumber()
    )
}

private fun TimedCompositionStageGroup.toJson(): JsonValue.Obj {
    return jsonObject(
        "id" to id.jsonString(),
        "order" to order.jsonNumber(),
        "name" to name.jsonString(),
        "colorHex" to colorHex.jsonString(),
        "iconKey" to iconKey?.jsonString(),
        "targets" to targets.map { target -> target.toJson() }.jsonArray(),
        "cueSettings" to cueSettings?.toJson(),
        "compatibility" to compatibility?.toJson()
    )
}

private fun TimedStageStyle.toJson(): JsonValue.Obj {
    return jsonObject(
        "colorHex" to colorHex?.jsonString(),
        "iconKey" to iconKey?.jsonString()
    )
}

private fun TimedCompositionTarget.toJson(): JsonValue.Obj {
    return jsonObject(
        "id" to id.jsonString(),
        "order" to order.jsonNumber(),
        "name" to name.jsonString(),
        "kind" to kind.contractValue.jsonString(),
        "durationSec" to durationSec.jsonNumber(),
        "colorHex" to colorHex.jsonString(),
        "iconKey" to iconKey?.jsonString(),
        "cueSettings" to cueSettings?.toJson(),
        "autoAdvance" to autoAdvance.jsonBool(),
        "compatibility" to compatibility?.toJson()
    )
}

private fun TimedCompositionCompatibilityMeta.toJson(): JsonValue.Obj {
    return jsonObject(
        "sourceVersion" to sourceVersion?.contractValue?.jsonString(),
        "legacyBlockId" to legacyBlockId?.jsonString(),
        "legacyItemId" to legacyItemId?.jsonString(),
        "legacyStageType" to legacyStageType?.contractValue?.jsonString(),
        "convertedAt" to convertedAt?.jsonString()
    )
}

private fun TimedExerciseItem.toJson(): JsonValue.Obj {
    return jsonObject(
        "id" to id.jsonString(),
        "exerciseId" to exerciseId?.jsonString(),
        "labelOverride" to labelOverride?.jsonString(),
        "side" to side?.contractValue?.jsonString(),
        "stageType" to stageType.contractValue.jsonString(),
        "iconKey" to iconKey.jsonString(),
        "colorHex" to colorHex.jsonString(),
        "workDurationSec" to workDurationSec.jsonNumber(),
        "restAfterSec" to restAfterSec?.jsonNumber(),
        "cueSettings" to cueSettings?.toJson(),
        "autoAdvance" to autoAdvance.jsonBool()
    )
}

private fun StrengthExerciseTarget.toJson(): JsonValue.Obj {
    return jsonObject(
        "weight" to weight?.toJson(),
        "repTarget" to repTarget?.toJson(),
        "restAfterSetSec" to restAfterSetSec?.jsonNumber()
    )
}

private fun StrengthSetPlan.toJson(): JsonValue.Obj {
    return jsonObject(
        "id" to id.jsonString(),
        "order" to order.jsonNumber(),
        "kind" to kind.contractValue.jsonString(),
        "side" to side?.contractValue?.jsonString(),
        "targetWeight" to targetWeight?.toJson(),
        "repTarget" to repTarget?.toJson(),
        "restAfterSec" to restAfterSec?.jsonNumber()
    )
}

private fun WeightValue.toJson(): JsonValue.Obj {
    return jsonObject(
        "value" to value.jsonNumber(),
        "unit" to unit.contractValue.jsonString()
    )
}

private fun RepTarget.toJson(): JsonValue.Obj {
    return when (this) {
        is RepTarget.Fixed -> jsonObject(
            "kind" to "fixed".jsonString(),
            "reps" to reps.jsonNumber()
        )

        is RepTarget.Range -> jsonObject(
            "kind" to "range".jsonString(),
            "minReps" to minReps.jsonNumber(),
            "maxReps" to maxReps.jsonNumber()
        )
    }
}

private fun PlanPreferences.toJson(): JsonValue.Obj {
    return jsonObject(
        "cueSettings" to cueSettings?.toJson(),
        "heartRateDisplay" to heartRateDisplay?.toJson()
    )
}

private fun CueSettings.toJson(): JsonValue.Obj {
    return jsonObject(
        "actionEnding" to actionEnding?.toJson(),
        "restEnding" to restEnding?.toJson()
    )
}

private fun CountdownCue.toJson(): JsonValue.Obj {
    return jsonObject(
        "enabled" to enabled.jsonBool(),
        "thresholdSec" to thresholdSec.jsonNumber(),
        "soundEnabled" to soundEnabled.jsonBool(),
        "vibrationEnabled" to vibrationEnabled.jsonBool(),
        "emphasisAnimationEnabled" to emphasisAnimationEnabled.jsonBool(),
        "voiceCueEnabled" to voiceCueEnabled.jsonBool()
    )
}

private fun HeartRateDisplayPreference.toJson(): JsonValue.Obj {
    return jsonObject(
        "enabled" to enabled.jsonBool(),
        "showDisconnectedPlaceholder" to showDisconnectedPlaceholder.jsonBool()
    )
}

private fun FollowAlongPlanMeta.toJson(): JsonValue.Obj {
    return jsonObject(
        "preset" to preset.jsonBool(),
        "coverMediaId" to coverMediaId?.jsonString(),
        "coachMediaIds" to coachMediaIds.map { it.jsonString() }.jsonArray(),
        "chapterIds" to chapterIds.map { it.jsonString() }.jsonArray(),
        "timelineCueIds" to timelineCueIds.map { it.jsonString() }.jsonArray(),
        "musicTrackIds" to musicTrackIds.map { it.jsonString() }.jsonArray(),
        "aiAnalysisProfileId" to aiAnalysisProfileId?.jsonString()
    )
}

private fun JsonValue.toPlanBlock(): PlanBlock? {
    val obj = this as? JsonValue.Obj ?: return null
    val id = obj.string("id") ?: return null
    val kind = obj.string("kind")?.let(::planBlockKindFrom) ?: return null
    val order = obj.int("order") ?: 0
    val title = obj.string("title")
    return when (kind) {
        PlanBlockKind.WARMUP -> WarmupBlock(
            id = id,
            order = order,
            title = title,
            durationSec = obj.int("durationSec"),
            items = obj.timedItems()
        )

        PlanBlockKind.STRETCH -> StretchBlock(
            id = id,
            order = order,
            title = title,
            durationSec = obj.int("durationSec"),
            items = obj.timedItems()
        )

        PlanBlockKind.COOLDOWN -> CooldownBlock(
            id = id,
            order = order,
            title = title,
            durationSec = obj.int("durationSec"),
            items = obj.timedItems()
        )

        PlanBlockKind.REST -> RestBlock(
            id = id,
            order = order,
            title = title,
            durationSec = obj.int("durationSec") ?: 0,
            label = obj.string("label")
        )

        PlanBlockKind.TIMED_CIRCUIT -> TimedCircuitBlock(
            id = id,
            order = order,
            title = title,
            rounds = obj.int("rounds") ?: 1,
            restBetweenRoundsSec = obj.int("restBetweenRoundsSec"),
            items = obj.timedItems()
        )

        PlanBlockKind.TIMED_COMPOSITION -> {
            if (obj.int("compositionVersion") != TIMED_COMPOSITION_CURRENT_VERSION) {
                return null
            }
            TimedCompositionBlock(
                id = id,
                order = order,
                title = title,
                compositionVersion = obj.int("compositionVersion") ?: TIMED_COMPOSITION_CURRENT_VERSION,
                warmupSec = obj.int("warmupSec") ?: 0,
                warmupStyle = obj.obj("warmupStyle")?.toTimedStageStyle(),
                cooldownSec = obj.int("cooldownSec") ?: 0,
                cooldownStyle = obj.obj("cooldownStyle")?.toTimedStageStyle(),
                rounds = obj.int("rounds") ?: 1,
                restBetweenRoundsSec = obj.int("restBetweenRoundsSec") ?: 0,
                restBetweenRoundsStyle = obj.obj("restBetweenRoundsStyle")?.toTimedStageStyle(),
                stageGroups = obj.array("stageGroups")
                    ?.mapNotNull { value -> value.toTimedCompositionStageGroup() }
                    ?: emptyList(),
                compatibility = obj.obj("compatibility")?.toTimedCompositionCompatibilityMeta()
            ).normalized()
        }

        PlanBlockKind.STRENGTH_EXERCISE -> StrengthExerciseBlock(
            id = id,
            order = order,
            title = title,
            exerciseId = obj.string("exerciseId") ?: return null,
            target = obj.obj("target")?.toStrengthTarget(),
            sets = obj.array("sets")?.mapNotNull { value -> value.toStrengthSetPlan() } ?: emptyList(),
            substitutions = obj.stringArray("substitutions"),
            setTimerMode = obj.string("setTimerMode")?.let(::strengthSetTimerModeFrom)
                ?: StrengthSetTimerMode.MANUAL_START
        )
    }
}

private fun JsonValue.Obj.toTimedStageStyle(): TimedStageStyle? {
    return TimedStageStyle(
        colorHex = string("colorHex"),
        iconKey = string("iconKey")
    ).normalized()
}

private fun JsonValue.toTimedCompositionStageGroup(): TimedCompositionStageGroup? {
    val obj = this as? JsonValue.Obj ?: return null
    return TimedCompositionStageGroup(
        id = obj.string("id") ?: return null,
        order = obj.int("order") ?: 0,
        name = obj.string("name").orEmpty(),
        colorHex = obj.string("colorHex").orEmpty(),
        iconKey = obj.string("iconKey"),
        targets = obj.array("targets")?.mapNotNull { value -> value.toTimedCompositionTarget() } ?: emptyList(),
        cueSettings = obj.obj("cueSettings")?.toCueSettings(),
        compatibility = obj.obj("compatibility")?.toTimedCompositionCompatibilityMeta()
    ).normalized()
}

private fun JsonValue.toTimedCompositionTarget(): TimedCompositionTarget? {
    val obj = this as? JsonValue.Obj ?: return null
    val kind = obj.string("kind")?.let(::timedCompositionTargetKindFrom) ?: return null
    return TimedCompositionTarget(
        id = obj.string("id") ?: return null,
        order = obj.int("order") ?: 0,
        name = obj.string("name").orEmpty(),
        kind = kind,
        durationSec = obj.int("durationSec") ?: 0,
        colorHex = obj.string("colorHex").orEmpty(),
        iconKey = obj.string("iconKey"),
        cueSettings = obj.obj("cueSettings")?.toCueSettings(),
        autoAdvance = obj.boolean("autoAdvance") ?: true,
        compatibility = obj.obj("compatibility")?.toTimedCompositionCompatibilityMeta()
    ).normalized()
}

private fun JsonValue.Obj.toTimedCompositionCompatibilityMeta(): TimedCompositionCompatibilityMeta {
    return TimedCompositionCompatibilityMeta(
        sourceVersion = string("sourceVersion")?.let(::timedCompositionCompatibilitySourceVersionFrom),
        legacyBlockId = string("legacyBlockId"),
        legacyItemId = string("legacyItemId"),
        legacyStageType = string("legacyStageType")?.let(::timedStageTypeFrom),
        convertedAt = string("convertedAt")
    )
}

private fun JsonValue.toTimedExerciseItem(): TimedExerciseItem? {
    val obj = this as? JsonValue.Obj ?: return null
    val stageType = obj.string("stageType")?.let(::timedStageTypeFrom) ?: TimedStageType.WORK
    return TimedExerciseItem(
        id = obj.string("id") ?: return null,
        exerciseId = obj.string("exerciseId"),
        labelOverride = obj.string("labelOverride"),
        side = obj.string("side")?.let(::exerciseSideFrom),
        stageType = stageType,
        iconKey = obj.string("iconKey") ?: stageType.defaultIconKey,
        colorHex = normalizeStageColorHex(obj.string("colorHex"), stageType),
        workDurationSec = obj.int("workDurationSec") ?: return null,
        restAfterSec = obj.int("restAfterSec"),
        cueSettings = obj.obj("cueSettings")?.toCueSettings(),
        autoAdvance = obj.boolean("autoAdvance") ?: false
    )
}

private fun JsonValue.toStrengthSetPlan(): StrengthSetPlan? {
    val obj = this as? JsonValue.Obj ?: return null
    return StrengthSetPlan(
        id = obj.string("id") ?: return null,
        order = obj.int("order") ?: 0,
        kind = obj.string("kind")?.let(::strengthSetKindFrom) ?: StrengthSetKind.WORKING,
        side = obj.string("side")?.let(::exerciseSideFrom),
        targetWeight = obj.obj("targetWeight")?.toWeightValue(),
        repTarget = obj.obj("repTarget")?.toRepTarget(),
        restAfterSec = obj.int("restAfterSec")
    )
}

private fun JsonValue.Obj.timedItems(): List<TimedExerciseItem> {
    return array("items")?.mapNotNull { value -> value.toTimedExerciseItem() } ?: emptyList()
}

private fun JsonValue.Obj.toStrengthTarget(): StrengthExerciseTarget {
    return StrengthExerciseTarget(
        weight = obj("weight")?.toWeightValue(),
        repTarget = obj("repTarget")?.toRepTarget(),
        restAfterSetSec = int("restAfterSetSec")
    )
}

private fun JsonValue.Obj.toWeightValue(): WeightValue? {
    val value = number("value") ?: return null
    val unit = string("unit")?.let(::weightUnitFrom) ?: return null
    return WeightValue(value = value, unit = unit)
}

private fun JsonValue.Obj.toRepTarget(): RepTarget? {
    return when (string("kind")) {
        "fixed" -> int("reps")?.let { reps -> RepTarget.Fixed(reps) }
        "range" -> {
            val min = int("minReps")
            val max = int("maxReps")
            if (min != null && max != null) RepTarget.Range(min, max) else null
        }

        else -> null
    }
}

private fun JsonValue.Obj.toPlanPreferences(): PlanPreferences {
    return PlanPreferences(
        cueSettings = obj("cueSettings")?.toCueSettings(),
        heartRateDisplay = obj("heartRateDisplay")?.toHeartRateDisplay()
    )
}

private fun JsonValue.Obj.toCueSettings(): CueSettings {
    return CueSettings(
        actionEnding = obj("actionEnding")?.toCountdownCue(),
        restEnding = obj("restEnding")?.toCountdownCue()
    )
}

private fun JsonValue.Obj.toCountdownCue(): CountdownCue {
    return CountdownCue(
        enabled = boolean("enabled") ?: true,
        thresholdSec = int("thresholdSec") ?: CountdownCue.DEFAULT_THRESHOLD_SEC,
        soundEnabled = boolean("soundEnabled") ?: true,
        vibrationEnabled = boolean("vibrationEnabled") ?: true,
        emphasisAnimationEnabled = boolean("emphasisAnimationEnabled") ?: true,
        voiceCueEnabled = boolean("voiceCueEnabled") ?: false
    )
}

private fun JsonValue.Obj.toHeartRateDisplay(): HeartRateDisplayPreference {
    return HeartRateDisplayPreference(
        enabled = boolean("enabled") ?: false,
        showDisconnectedPlaceholder = boolean("showDisconnectedPlaceholder") ?: false
    )
}

private fun JsonValue.Obj.toFollowAlongMeta(): FollowAlongPlanMeta {
    return FollowAlongPlanMeta(
        preset = boolean("preset") ?: false,
        coverMediaId = string("coverMediaId"),
        coachMediaIds = stringArray("coachMediaIds"),
        chapterIds = stringArray("chapterIds"),
        timelineCueIds = stringArray("timelineCueIds"),
        musicTrackIds = stringArray("musicTrackIds"),
        aiAnalysisProfileId = string("aiAnalysisProfileId")
    )
}

private fun workoutModeFrom(value: String): WorkoutMode {
    return WorkoutMode.entries.firstOrNull { mode -> mode.contractValue == value } ?: WorkoutMode.TIMED
}

private fun planBlockKindFrom(value: String): PlanBlockKind? {
    return PlanBlockKind.entries.firstOrNull { kind -> kind.contractValue == value }
}

private fun timedStageTypeFrom(value: String): TimedStageType {
    return TimedStageType.entries.firstOrNull { type -> type.contractValue == value } ?: TimedStageType.WORK
}

private fun timedCompositionTargetKindFrom(value: String): TimedCompositionTargetKind? {
    return TimedCompositionTargetKind.entries.firstOrNull { kind -> kind.contractValue == value }
}

private fun timedCompositionCompatibilitySourceVersionFrom(
    value: String
): TimedCompositionCompatibilitySourceVersion? {
    return TimedCompositionCompatibilitySourceVersion.entries.firstOrNull { source ->
        source.contractValue == value
    }
}

private fun exerciseSideFrom(value: String): ExerciseSide {
    return ExerciseSide.entries.firstOrNull { side -> side.contractValue == value } ?: ExerciseSide.BOTH
}

private fun strengthSetKindFrom(value: String): StrengthSetKind {
    return StrengthSetKind.entries.firstOrNull { kind -> kind.contractValue == value } ?: StrengthSetKind.WORKING
}

private fun strengthSetTimerModeFrom(value: String): StrengthSetTimerMode {
    return StrengthSetTimerMode.entries.firstOrNull { mode -> mode.contractValue == value }
        ?: StrengthSetTimerMode.MANUAL_START
}

private fun weightUnitFrom(value: String): WeightUnit? {
    return WeightUnit.entries.firstOrNull { unit -> unit.contractValue == value }
}

private sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val values: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Double) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

private fun jsonObject(vararg fields: Pair<String, JsonValue?>): JsonValue.Obj {
    return JsonValue.Obj(fields.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())
}

private fun jsonObject(
    commonFields: Map<String, JsonValue?>,
    vararg fields: Pair<String, JsonValue?>
): JsonValue.Obj {
    return JsonValue.Obj(
        (commonFields.toList() + fields.toList())
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap()
    )
}

private fun String.jsonString(): JsonValue.Str = JsonValue.Str(this)

private fun Int.jsonNumber(): JsonValue.Num = JsonValue.Num(toDouble())

private fun Double.jsonNumber(): JsonValue.Num = JsonValue.Num(this)

private fun Boolean.jsonBool(): JsonValue.Bool = JsonValue.Bool(this)

private fun List<JsonValue>.jsonArray(): JsonValue.Arr = JsonValue.Arr(this)

private fun JsonValue.Obj.string(name: String): String? = fields[name].let { value ->
    (value as? JsonValue.Str)?.value
}

private fun JsonValue.Obj.number(name: String): Double? = fields[name].let { value ->
    (value as? JsonValue.Num)?.value
}

private fun JsonValue.Obj.int(name: String): Int? = number(name)?.toInt()

private fun JsonValue.Obj.boolean(name: String): Boolean? = fields[name].let { value ->
    (value as? JsonValue.Bool)?.value
}

private fun JsonValue.Obj.obj(name: String): JsonValue.Obj? = fields[name] as? JsonValue.Obj

private fun JsonValue.Obj.array(name: String): List<JsonValue>? = (fields[name] as? JsonValue.Arr)?.values

private fun JsonValue.Obj.stringArray(name: String): List<String> {
    return array(name)?.mapNotNull { value -> (value as? JsonValue.Str)?.value } ?: emptyList()
}

private fun JsonValue.render(): String {
    return when (this) {
        is JsonValue.Obj -> fields.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { (key, value) -> "${key.escapeJson().quote()}:${value.render()}" }

        is JsonValue.Arr -> values.joinToString(prefix = "[", postfix = "]") { value -> value.render() }
        is JsonValue.Str -> value.escapeJson().quote()
        is JsonValue.Num -> {
            val long = value.toLong()
            if (value == long.toDouble()) long.toString() else value.toString()
        }

        is JsonValue.Bool -> value.toString()
        JsonValue.Null -> "null"
    }
}

private fun String.quote(): String = "\"$this\""

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

private class JsonParser(private val source: String) {
    private var index = 0

    fun parse(): JsonValue {
        val value = parseValue()
        skipWhitespace()
        require(index == source.length)
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> {
                consumeLiteral("true")
                JsonValue.Bool(true)
            }

            'f' -> {
                consumeLiteral("false")
                JsonValue.Bool(false)
            }

            'n' -> {
                consumeLiteral("null")
                JsonValue.Null
            }

            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonValue.Obj {
        consume('{')
        skipWhitespace()
        val fields = linkedMapOf<String, JsonValue>()
        if (peek() == '}') {
            consume('}')
            return JsonValue.Obj(fields)
        }
        while (true) {
            val key = parseString()
            skipWhitespace()
            consume(':')
            fields[key] = parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> {
                    consume(',')
                    skipWhitespace()
                }

                '}' -> {
                    consume('}')
                    return JsonValue.Obj(fields)
                }

                else -> error("Expected object delimiter.")
            }
        }
    }

    private fun parseArray(): JsonValue.Arr {
        consume('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (peek() == ']') {
            consume(']')
            return JsonValue.Arr(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> consume(',')
                ']' -> {
                    consume(']')
                    return JsonValue.Arr(values)
                }

                else -> error("Expected array delimiter.")
            }
        }
    }

    private fun parseString(): String {
        consume('"')
        return buildString {
            while (index < source.length) {
                when (val char = source[index++]) {
                    '"' -> return@buildString
                    '\\' -> {
                        val escaped = source[index++]
                        append(
                            when (escaped) {
                                '"' -> '"'
                                '\\' -> '\\'
                                '/' -> '/'
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'u' -> parseUnicode()
                                else -> escaped
                            }
                        )
                    }

                    else -> append(char)
                }
            }
        }
    }

    private fun parseUnicode(): Char {
        val hex = source.substring(index, index + 4)
        index += 4
        return hex.toInt(16).toChar()
    }

    private fun parseNumber(): JsonValue.Num {
        val start = index
        while (index < source.length && source[index] in "-+0123456789.eE") {
            index++
        }
        return JsonValue.Num(source.substring(start, index).toDouble())
    }

    private fun consumeLiteral(literal: String) {
        require(source.startsWith(literal, index))
        index += literal.length
    }

    private fun consume(expected: Char) {
        skipWhitespace()
        require(peek() == expected) { "Expected $expected at $index." }
        index++
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
    }
}
