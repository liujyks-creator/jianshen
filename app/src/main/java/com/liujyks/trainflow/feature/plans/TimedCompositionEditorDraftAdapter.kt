package com.liujyks.trainflow.feature.plans

import com.liujyks.trainflow.core.model.CooldownBlock
import com.liujyks.trainflow.core.model.StretchBlock
import com.liujyks.trainflow.core.model.TimedCircuitBlock
import com.liujyks.trainflow.core.model.TimedCompositionBlock
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilityMeta
import com.liujyks.trainflow.core.model.TimedCompositionCompatibilitySourceVersion
import com.liujyks.trainflow.core.model.TimedCompositionStageGroup
import com.liujyks.trainflow.core.model.TimedCompositionTarget
import com.liujyks.trainflow.core.model.TimedCompositionTargetKind
import com.liujyks.trainflow.core.model.TimedExerciseItem
import com.liujyks.trainflow.core.model.TimedStageStyle
import com.liujyks.trainflow.core.model.TimedStageType
import com.liujyks.trainflow.core.model.WarmupBlock
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.model.WorkoutPlan
import com.liujyks.trainflow.core.model.normalized
import com.liujyks.trainflow.core.model.normalizeStageColorHex

internal enum class TimedCompositionEditorDraftSource {
    LEGACY_TIMED,
    V2_PAYLOAD,
    UNSUPPORTED
}

internal enum class TimedCompositionEditorDraftExportMode {
    PRESERVE_SOURCE,
    EXPORT_V2_PAYLOAD
}

internal data class TimedCompositionEditorDraft(
    val planId: String,
    val title: String,
    val description: String,
    val warmupSec: Int,
    val cooldownSec: Int,
    val rounds: Int,
    val restBetweenRoundsSec: Int,
    val warmupStyle: TimedStageStyle? = null,
    val cooldownStyle: TimedStageStyle? = null,
    val restBetweenRoundsStyle: TimedStageStyle? = null,
    val stageGroups: List<TimedCompositionStageGroupDraft>,
    val source: TimedCompositionEditorDraftSource,
    val sourcePlan: WorkoutPlan,
    val requiresExplicitConversionForV2: Boolean
) {
    val repeatedDurationSec: Int
        get() = stageGroups.sumOf { group -> group.durationSec }

    val estimatedDurationSec: Int
        get() = warmupSec + cooldownSec + repeatedDurationSec * rounds +
            restBetweenRoundsSec * (rounds - 1).coerceAtLeast(0)

    fun toWorkoutPlan(
        exportMode: TimedCompositionEditorDraftExportMode = TimedCompositionEditorDraftExportMode.PRESERVE_SOURCE,
        timestamp: String = DefaultTimedPlanTimestamp
    ): WorkoutPlan {
        return when (exportMode) {
            TimedCompositionEditorDraftExportMode.PRESERVE_SOURCE -> sourcePlan
            TimedCompositionEditorDraftExportMode.EXPORT_V2_PAYLOAD -> sourcePlan.copy(
                mode = WorkoutMode.TIMED,
                title = title.trim().ifBlank { sourcePlan.title },
                description = description.trim().ifBlank { sourcePlan.description.orEmpty() }
                    .takeIf { value -> value.isNotBlank() },
                blocks = listOf(toTimedCompositionBlock(timestamp)),
                preferences = sourcePlan.preferences,
                updatedAt = timestamp
            )
        }
    }

    fun toTimedCompositionBlock(timestamp: String = DefaultTimedPlanTimestamp): TimedCompositionBlock {
        val sourceVersion = when (source) {
            TimedCompositionEditorDraftSource.LEGACY_TIMED ->
                TimedCompositionCompatibilitySourceVersion.LEGACY_TIMED_CIRCUIT

            TimedCompositionEditorDraftSource.V2_PAYLOAD,
            TimedCompositionEditorDraftSource.UNSUPPORTED ->
                TimedCompositionCompatibilitySourceVersion.V2
        }
        return TimedCompositionBlock(
            id = "${planId}-timed-composition",
            order = 1,
            title = title.trim().ifBlank { sourcePlan.title },
            warmupSec = warmupSec,
            warmupStyle = warmupStyle,
            cooldownSec = cooldownSec,
            cooldownStyle = cooldownStyle,
            rounds = rounds,
            restBetweenRoundsSec = restBetweenRoundsSec,
            restBetweenRoundsStyle = restBetweenRoundsStyle,
            stageGroups = stageGroups.map { group -> group.toModel() },
            compatibility = TimedCompositionCompatibilityMeta(
                sourceVersion = sourceVersion,
                convertedAt = timestamp
            )
        ).normalized()
    }
}

internal data class TimedCompositionStageGroupDraft(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconKey: String?,
    val order: Int,
    val targets: List<TimedCompositionTargetDraft>,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) {
    val durationSec: Int
        get() = targets.sumOf { target -> target.durationSec }

    fun toModel(): TimedCompositionStageGroup {
        return TimedCompositionStageGroup(
            id = id,
            order = order,
            name = name,
            colorHex = colorHex,
            iconKey = iconKey,
            targets = targets.map { target -> target.toModel() },
            compatibility = compatibility
        )
    }
}

internal data class TimedCompositionTargetDraft(
    val id: String,
    val name: String,
    val kind: TimedCompositionTargetKind,
    val colorHex: String,
    val iconKey: String?,
    val durationSec: Int,
    val order: Int,
    val compatibility: TimedCompositionCompatibilityMeta? = null
) {
    fun toModel(): TimedCompositionTarget {
        return TimedCompositionTarget(
            id = id,
            order = order,
            name = name,
            kind = kind,
            durationSec = durationSec,
            colorHex = colorHex,
            iconKey = iconKey,
            compatibility = compatibility
        )
    }
}

internal fun WorkoutPlan.toTimedCompositionEditorDraft(): TimedCompositionEditorDraft {
    if (mode != WorkoutMode.TIMED) {
        return TimedCompositionEditorDraft(
            planId = id,
            title = title,
            description = description.orEmpty(),
            warmupSec = 0,
            cooldownSec = 0,
            rounds = 1,
            restBetweenRoundsSec = 0,
            stageGroups = emptyList(),
            source = TimedCompositionEditorDraftSource.UNSUPPORTED,
            sourcePlan = this,
            requiresExplicitConversionForV2 = true
        )
    }

    val compositionBlock = blocks.filterIsInstance<TimedCompositionBlock>()
        .sortedBy { block -> block.order }
        .firstOrNull()
    if (compositionBlock != null) {
        return compositionBlock.normalized().toDraft(this)
    }

    return toLegacyTimedCompositionDraft()
}

private fun TimedCompositionBlock.toDraft(plan: WorkoutPlan): TimedCompositionEditorDraft {
    return TimedCompositionEditorDraft(
        planId = plan.id,
        title = plan.title,
        description = plan.description.orEmpty(),
        warmupSec = warmupSec,
        cooldownSec = cooldownSec,
        rounds = rounds,
        restBetweenRoundsSec = restBetweenRoundsSec,
        warmupStyle = warmupStyle,
        cooldownStyle = cooldownStyle,
        restBetweenRoundsStyle = restBetweenRoundsStyle,
        stageGroups = stageGroups.map { group -> group.toDraft() },
        source = TimedCompositionEditorDraftSource.V2_PAYLOAD,
        sourcePlan = plan,
        requiresExplicitConversionForV2 = false
    )
}

private fun TimedCompositionStageGroup.toDraft(): TimedCompositionStageGroupDraft {
    return TimedCompositionStageGroupDraft(
        id = id,
        name = name,
        colorHex = colorHex,
        iconKey = iconKey,
        order = order,
        targets = targets.map { target -> target.toDraft() },
        compatibility = compatibility
    )
}

private fun TimedCompositionTarget.toDraft(): TimedCompositionTargetDraft {
    return TimedCompositionTargetDraft(
        id = id,
        name = name,
        kind = kind,
        colorHex = colorHex,
        iconKey = iconKey,
        durationSec = durationSec,
        order = order,
        compatibility = compatibility
    )
}

private fun WorkoutPlan.toLegacyTimedCompositionDraft(): TimedCompositionEditorDraft {
    val orderedBlocks = blocks.sortedBy { block -> block.order }
    val circuits = orderedBlocks.filterIsInstance<TimedCircuitBlock>()
    val firstCircuit = circuits.firstOrNull()
    return TimedCompositionEditorDraft(
        planId = id,
        title = title,
        description = description.orEmpty(),
        warmupSec = orderedBlocks.filterIsInstance<WarmupBlock>().sumOf { block -> block.boundaryDurationSec() },
        cooldownSec = orderedBlocks.filterIsInstance<CooldownBlock>().sumOf { block -> block.boundaryDurationSec() } +
            orderedBlocks.filterIsInstance<StretchBlock>().sumOf { block -> block.boundaryDurationSec() },
        rounds = firstCircuit?.rounds?.coerceAtLeast(1) ?: 1,
        restBetweenRoundsSec = firstCircuit?.restBetweenRoundsSec?.coerceAtLeast(0) ?: 0,
        stageGroups = circuits.flatMap { block -> block.toDraftStageGroups() }
            .mapIndexed { index, group -> group.copy(order = index + 1) },
        source = TimedCompositionEditorDraftSource.LEGACY_TIMED,
        sourcePlan = this,
        requiresExplicitConversionForV2 = true
    )
}

private fun WarmupBlock.boundaryDurationSec(): Int {
    return durationSec?.coerceAtLeast(0) ?: items.sumOf { item -> item.workDurationSec.coerceAtLeast(0) }
}

private fun CooldownBlock.boundaryDurationSec(): Int {
    return durationSec?.coerceAtLeast(0) ?: items.sumOf { item -> item.workDurationSec.coerceAtLeast(0) }
}

private fun StretchBlock.boundaryDurationSec(): Int {
    return durationSec?.coerceAtLeast(0) ?: items.sumOf { item -> item.workDurationSec.coerceAtLeast(0) }
}

private fun TimedCircuitBlock.toDraftStageGroups(): List<TimedCompositionStageGroupDraft> {
    return items.mapIndexedNotNull { index, item ->
        when (item.stageType) {
            TimedStageType.WARMUP,
            TimedStageType.COOLDOWN -> null

            TimedStageType.WORK,
            TimedStageType.REST,
            TimedStageType.CUSTOM -> item.toDraftStageGroup(block = this, order = index + 1)
        }
    }
}

private fun TimedExerciseItem.toDraftStageGroup(
    block: TimedCircuitBlock,
    order: Int
): TimedCompositionStageGroupDraft {
    val itemKind = stageType.toTimedCompositionTargetKind()
    val itemColor = normalizeStageColorHex(colorHex, stageType)
    val actionTarget = TimedCompositionTargetDraft(
        id = if (itemKind == TimedCompositionTargetKind.REST) "$id-rest" else "$id-action",
        name = labelOverride?.takeIf { label -> label.isNotBlank() } ?: stageType.displayName,
        kind = itemKind,
        colorHex = itemColor,
        iconKey = iconKey.ifBlank { stageType.defaultIconKey },
        durationSec = workDurationSec.coerceAtLeast(0),
        order = 1,
        compatibility = TimedCompositionCompatibilityMeta(
            sourceVersion = TimedCompositionCompatibilitySourceVersion.LEGACY_TIMED_CIRCUIT,
            legacyBlockId = block.id,
            legacyItemId = id,
            legacyStageType = stageType
        )
    )
    val restTarget = restAfterSec
        ?.coerceAtLeast(0)
        ?.takeIf { seconds -> seconds > 0 && itemKind != TimedCompositionTargetKind.REST }
        ?.let { seconds ->
            TimedCompositionTargetDraft(
                id = "$id-rest",
                name = TimedStageType.REST.displayName,
                kind = TimedCompositionTargetKind.REST,
                colorHex = normalizeStageColorHex(TimedStageType.REST.defaultColorHex, TimedStageType.REST),
                iconKey = TimedStageType.REST.defaultIconKey,
                durationSec = seconds,
                order = 2,
                compatibility = TimedCompositionCompatibilityMeta(
                    sourceVersion = TimedCompositionCompatibilitySourceVersion.LEGACY_TIMED_CIRCUIT,
                    legacyBlockId = block.id,
                    legacyItemId = id,
                    legacyStageType = TimedStageType.REST
                )
            )
        }

    return TimedCompositionStageGroupDraft(
        id = "${block.id}-$id",
        name = labelOverride?.takeIf { label -> label.isNotBlank() } ?: stageType.displayName,
        colorHex = itemColor,
        iconKey = iconKey.ifBlank { stageType.defaultIconKey },
        order = order,
        targets = listOfNotNull(actionTarget, restTarget),
        compatibility = TimedCompositionCompatibilityMeta(
            sourceVersion = TimedCompositionCompatibilitySourceVersion.LEGACY_TIMED_CIRCUIT,
            legacyBlockId = block.id,
            legacyItemId = id,
            legacyStageType = stageType
        )
    )
}

private fun TimedStageType.toTimedCompositionTargetKind(): TimedCompositionTargetKind {
    return when (this) {
        TimedStageType.REST -> TimedCompositionTargetKind.REST
        TimedStageType.CUSTOM -> TimedCompositionTargetKind.CUSTOM
        TimedStageType.WARMUP,
        TimedStageType.WORK,
        TimedStageType.COOLDOWN -> TimedCompositionTargetKind.ACTION
    }
}
