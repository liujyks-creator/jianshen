package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.database.parseCanonicalJson
import com.liujyks.trainflow.core.database.renderCanonicalJson
import com.liujyks.trainflow.core.model.WorkoutMode
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Collections

internal sealed interface PreparedPlanSnapshotStorageV1Result {
    data class Valid(
        val prepared: PreparedPlanSnapshotStorageV1
    ) : PreparedPlanSnapshotStorageV1Result

    data class Invalid(
        val result: PlanSnapshotStorageV1ValidationResult
    ) : PreparedPlanSnapshotStorageV1Result
}

private object PreparedPlanSnapshotStorageV1FactoryProof

internal class PreparedPlanSnapshotStorageV1 private constructor(
    private val storage: WorkoutPlanSnapshotStorageV1,
    private val orderedStructureSignatureInput: String,
    private val orderedStructureDigestHexLowercase: String,
    phaseBindingBlocks: List<PlanSnapshotPhaseBlockFactsV1>
) {
    private val phaseBindingBlocks = Collections.unmodifiableList(ArrayList(phaseBindingBlocks))

    internal fun storage(): WorkoutPlanSnapshotStorageV1 = storage

    internal fun orderedStructureSignatureInputBytes(): ByteArray =
        orderedStructureSignatureInput.toByteArray(Charsets.UTF_8)

    internal fun orderedStructureDigestHexLowercase(): String = orderedStructureDigestHexLowercase

    internal fun phaseBindingBlocks(): List<PlanSnapshotPhaseBlockFactsV1> = phaseBindingBlocks

    internal companion object {
        internal fun fromValidated(
            factoryProof: Any,
            storage: WorkoutPlanSnapshotStorageV1,
            orderedStructureSignatureInput: String,
            orderedStructureDigestHexLowercase: String,
            phaseBindingBlocks: List<PlanSnapshotPhaseBlockFactsV1>
        ): PreparedPlanSnapshotStorageV1? = if (factoryProof === PreparedPlanSnapshotStorageV1FactoryProof) {
            PreparedPlanSnapshotStorageV1(
                storage = storage,
                orderedStructureSignatureInput = orderedStructureSignatureInput,
                orderedStructureDigestHexLowercase = orderedStructureDigestHexLowercase,
                phaseBindingBlocks = phaseBindingBlocks
            )
        } else {
            null
        }
    }
}

internal class PlanSnapshotPhaseBlockFactsV1 internal constructor(
    val id: String,
    val kind: String,
    val rounds: Long?,
    val restBetweenRoundsSec: Long?,
    val durationSec: Long?,
    val warmupSec: Long?,
    val cooldownSec: Long?,
    val exerciseId: String?,
    items: List<Item>,
    sets: List<SetPlan>,
    substitutions: List<String>,
    compositionGroups: List<CompositionGroup>
) {
    val items: List<Item> = Collections.unmodifiableList(ArrayList(items))
    val sets: List<SetPlan> = Collections.unmodifiableList(ArrayList(sets))
    val substitutions: List<String> = Collections.unmodifiableList(ArrayList(substitutions))
    val compositionGroups: List<CompositionGroup> = Collections.unmodifiableList(ArrayList(compositionGroups))

    internal data class Item(
        val id: String,
        val exerciseId: String?,
        val stageType: String,
        val restAfterSec: Long?
    )

    internal data class SetPlan(
        val id: String,
        val kind: String
    )

    internal class CompositionGroup internal constructor(
        val id: String,
        val order: Long,
        targets: List<CompositionTarget>
    ) {
        val targets: List<CompositionTarget> = Collections.unmodifiableList(ArrayList(targets))
    }

    internal data class CompositionTarget(
        val id: String,
        val order: Long,
        val kind: String
    )
}

object PlanSnapshotStorageV1Validator {
    fun validate(
        persistedJson: String,
        sessionMode: WorkoutMode
    ): PlanSnapshotStorageV1ValidationResult = when (val result = prepare(persistedJson, sessionMode)) {
        is PreparedPlanSnapshotStorageV1Result.Valid ->
            PlanSnapshotStorageV1ValidationResult.Valid(result.prepared.storage())
        is PreparedPlanSnapshotStorageV1Result.Invalid -> result.result
    }

    internal fun prepare(
        persistedJson: String,
        sessionMode: WorkoutMode
    ): PreparedPlanSnapshotStorageV1Result {
        val root = parseCanonicalJson(persistedJson) as? CanonicalJsonValue.Obj
            ?: return invalidPrepared()
        val version = root.int("planSnapshotStorageContractVersion") ?: return invalidPrepared()
        if (version != 1L) {
            return PreparedPlanSnapshotStorageV1Result.Invalid(
                PlanSnapshotStorageV1ValidationResult.UnsupportedVersion(version.toString())
            )
        }
        val canonical = canonicalRoot(root, sessionMode) ?: return invalidPrepared()
        if (canonical.renderCanonicalJson() != persistedJson) return invalidPrepared()
        val signatureInput = OrderedStructureSignatureInputV1.renderValidated(root, sessionMode)
            ?: return invalidPrepared()
        val signatureBytes = signatureInput.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(signatureBytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return PreparedPlanSnapshotStorageV1Result.Valid(
            requireNotNull(
                PreparedPlanSnapshotStorageV1.fromValidated(
                    factoryProof = PreparedPlanSnapshotStorageV1FactoryProof,
                    storage = WorkoutPlanSnapshotStorageV1(
                        mode = sessionMode,
                        persistedJson = persistedJson
                    ),
                    orderedStructureSignatureInput = signatureInput,
                    orderedStructureDigestHexLowercase = digest,
                    phaseBindingBlocks = phaseBindingBlockFacts(root)
                )
            )
        )
    }

    private fun phaseBindingBlockFacts(
        root: CanonicalJsonValue.Obj
    ): List<PlanSnapshotPhaseBlockFactsV1> = requireNotNull(root.array("blocks")).map { value ->
        val block = value as CanonicalJsonValue.Obj
        val items = block.array("items").orEmpty().map { itemValue ->
            val item = itemValue as CanonicalJsonValue.Obj
            PlanSnapshotPhaseBlockFactsV1.Item(
                id = requireNotNull(item.string("id")),
                exerciseId = item.string("exerciseId"),
                stageType = requireNotNull(item.string("stageType")),
                restAfterSec = item.int("restAfterSec")
            )
        }
        val sets = block.array("sets").orEmpty().map { setValue ->
            val set = setValue as CanonicalJsonValue.Obj
            PlanSnapshotPhaseBlockFactsV1.SetPlan(
                id = requireNotNull(set.string("id")),
                kind = requireNotNull(set.string("kind"))
            )
        }
        val substitutions = block.array("substitutions").orEmpty().map { substitution ->
            (substitution as CanonicalJsonValue.Str).value
        }
        val compositionGroups = block.array("stageGroups").orEmpty().map { groupValue ->
            val group = groupValue as CanonicalJsonValue.Obj
            PlanSnapshotPhaseBlockFactsV1.CompositionGroup(
                id = requireNotNull(group.string("id")),
                order = requireNotNull(group.int("order")),
                targets = requireNotNull(group.array("targets")).map { targetValue ->
                    val target = targetValue as CanonicalJsonValue.Obj
                    PlanSnapshotPhaseBlockFactsV1.CompositionTarget(
                        id = requireNotNull(target.string("id")),
                        order = requireNotNull(target.int("order")),
                        kind = requireNotNull(target.string("kind"))
                    )
                }
            )
        }
        PlanSnapshotPhaseBlockFactsV1(
            id = requireNotNull(block.string("id")),
            kind = requireNotNull(block.string("kind")),
            rounds = block.int("rounds"),
            restBetweenRoundsSec = block.int("restBetweenRoundsSec"),
            durationSec = block.int("durationSec"),
            warmupSec = block.int("warmupSec"),
            cooldownSec = block.int("cooldownSec"),
            exerciseId = block.string("exerciseId"),
            items = items,
            sets = sets,
            substitutions = substitutions,
            compositionGroups = compositionGroups
        )
    }

    private fun invalidPrepared() = PreparedPlanSnapshotStorageV1Result.Invalid(
        PlanSnapshotStorageV1ValidationResult.Invalid()
    )

    private fun canonicalRoot(
        root: CanonicalJsonValue.Obj,
        sessionMode: WorkoutMode
    ): CanonicalJsonValue.Obj? {
        if (!root.hasExactKeys(ROOT_KEYS)) return null
        if (!root.isStringOrNull("planId") || root.string("title") == null) return null
        if (root.string("mode") != sessionMode.contractValue) return null
        val blocks = root.array("blocks") ?: return null
        val canonicalBlocks = blocks.map { value -> canonicalBlock(value) ?: return null }
        val preferences = when (val value = root.fields["preferences"]) {
            CanonicalJsonValue.Null -> CanonicalJsonValue.Null
            is CanonicalJsonValue.Obj -> canonicalPreferences(value) ?: return null
            else -> return null
        }
        val followAlong = when (val value = root.fields["followAlong"]) {
            CanonicalJsonValue.Null -> CanonicalJsonValue.Null
            is CanonicalJsonValue.Obj -> canonicalFollowAlong(value) ?: return null
            else -> return null
        }
        return orderedObject(
            root,
            ROOT_ORDER,
            mapOf(
                "blocks" to CanonicalJsonValue.Arr(canonicalBlocks),
                "preferences" to preferences,
                "followAlong" to followAlong
            )
        )
    }

    private fun canonicalBlock(value: CanonicalJsonValue): CanonicalJsonValue.Obj? {
        val block = value as? CanonicalJsonValue.Obj ?: return null
        if (!block.validCommonBlock()) return null
        return when (block.string("kind")) {
            "warmup", "stretch", "cooldown" -> canonicalBoundaryBlock(block)
            "rest" -> canonicalRestBlock(block)
            "timed_circuit" -> canonicalTimedCircuit(block)
            "timed_composition" -> canonicalTimedComposition(block)
            "strength_exercise" -> canonicalStrengthBlock(block)
            else -> null
        }
    }

    private fun canonicalBoundaryBlock(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!block.hasRequiredAndOptionalKeys(COMMON_BLOCK_REQUIRED + setOf("items"), setOf("title", "durationSec"))) {
            return null
        }
        if (block.has("durationSec") && !block.isNonNegativeInteger("durationSec")) return null
        val items = canonicalArray(block, "items", ::canonicalTimedItem) ?: return null
        return orderedObject(block, COMMON_BLOCK_ORDER + listOf("durationSec", "items"), mapOf("items" to items))
    }

    private fun canonicalRestBlock(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!block.hasRequiredAndOptionalKeys(COMMON_BLOCK_REQUIRED + setOf("durationSec"), setOf("title", "label"))) {
            return null
        }
        if (!block.isNonNegativeInteger("durationSec") ||
            (block.has("label") && block.string("label") == null)
        ) {
            return null
        }
        return orderedObject(block, COMMON_BLOCK_ORDER + listOf("durationSec", "label"))
    }

    private fun canonicalTimedCircuit(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!block.hasRequiredAndOptionalKeys(
                COMMON_BLOCK_REQUIRED + setOf("rounds", "items"),
                setOf("title", "restBetweenRoundsSec")
            ) || !block.isPositiveInteger("rounds") ||
            (block.has("restBetweenRoundsSec") && !block.isNonNegativeInteger("restBetweenRoundsSec"))
        ) {
            return null
        }
        val items = canonicalArray(block, "items", ::canonicalTimedItem) ?: return null
        return orderedObject(
            block,
            COMMON_BLOCK_ORDER + listOf("rounds", "restBetweenRoundsSec", "items"),
            mapOf("items" to items)
        )
    }

    private fun canonicalTimedComposition(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!block.hasRequiredAndOptionalKeys(
                COMMON_BLOCK_REQUIRED + COMPOSITION_REQUIRED,
                setOf(
                    "title",
                    "warmupStyle",
                    "cooldownStyle",
                    "restBetweenRoundsStyle",
                    "compatibility"
                )
            ) || block.int("compositionVersion") != 2L ||
            !block.isNonNegativeInteger("warmupSec") ||
            !block.isNonNegativeInteger("cooldownSec") ||
            !block.isPositiveInteger("rounds") ||
            !block.isNonNegativeInteger("restBetweenRoundsSec")
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        listOf("warmupStyle", "cooldownStyle", "restBetweenRoundsStyle").forEach { key ->
            if (block.has(key)) {
                val style = canonicalStageStyle(block.obj(key) ?: return null) ?: return null
                replacements[key] = style
            }
        }
        val groups = canonicalArray(block, "stageGroups", ::canonicalStageGroup) ?: return null
        replacements["stageGroups"] = groups
        if (block.has("compatibility")) {
            replacements["compatibility"] = canonicalCompatibility(
                block.obj("compatibility") ?: return null
            ) ?: return null
        }
        return orderedObject(block, COMMON_BLOCK_ORDER + COMPOSITION_ORDER, replacements)
    }

    private fun canonicalStrengthBlock(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!block.hasRequiredAndOptionalKeys(
                COMMON_BLOCK_REQUIRED + setOf("exerciseId", "sets", "substitutions", "setTimerMode"),
                setOf("title", "target")
            ) || block.nonEmptyString("exerciseId") == null ||
            block.string("setTimerMode") !in STRENGTH_TIMER_MODES
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        replacements["sets"] = canonicalArray(block, "sets", ::canonicalStrengthSet) ?: return null
        val substitutions = block.array("substitutions") ?: return null
        if (substitutions.any { value -> (value as? CanonicalJsonValue.Str)?.value.isNullOrEmpty() }) return null
        replacements["substitutions"] = CanonicalJsonValue.Arr(substitutions)
        if (block.has("target")) {
            replacements["target"] = canonicalStrengthTarget(block.obj("target") ?: return null) ?: return null
        }
        return orderedObject(block, COMMON_BLOCK_ORDER + STRENGTH_BLOCK_ORDER, replacements)
    }

    private fun canonicalTimedItem(value: CanonicalJsonValue): CanonicalJsonValue.Obj? {
        val item = value as? CanonicalJsonValue.Obj ?: return null
        if (!item.hasRequiredAndOptionalKeys(
                TIMED_ITEM_REQUIRED,
                setOf("exerciseId", "labelOverride", "side", "restAfterSec", "cueSettings")
            ) || item.nonEmptyString("id") == null || item.string("stageType") !in TIMED_STAGE_TYPES ||
            item.nonEmptyString("iconKey") == null || item.nonEmptyString("colorHex") == null ||
            !item.isNonNegativeInteger("workDurationSec") || item.boolean("autoAdvance") == null ||
            (item.has("exerciseId") && item.nonEmptyString("exerciseId") == null) ||
            (item.has("labelOverride") && item.string("labelOverride") == null) ||
            (item.has("side") && item.string("side") !in EXERCISE_SIDES) ||
            (item.has("restAfterSec") && !item.isNonNegativeInteger("restAfterSec"))
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        if (item.has("cueSettings")) {
            replacements["cueSettings"] = canonicalCueSettings(item.obj("cueSettings") ?: return null)
                ?: return null
        }
        return orderedObject(item, TIMED_ITEM_ORDER, replacements)
    }

    private fun canonicalStageStyle(style: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!style.hasRequiredAndOptionalKeys(emptySet(), setOf("colorHex", "iconKey"))) return null
        if (style.has("colorHex") && style.nonEmptyString("colorHex") == null) return null
        if (style.has("iconKey") && style.nonEmptyString("iconKey") == null) return null
        return orderedObject(style, listOf("colorHex", "iconKey"))
    }

    private fun canonicalStageGroup(value: CanonicalJsonValue): CanonicalJsonValue.Obj? {
        val group = value as? CanonicalJsonValue.Obj ?: return null
        if (!group.hasRequiredAndOptionalKeys(
                STAGE_GROUP_REQUIRED,
                setOf("iconKey", "cueSettings", "compatibility")
            ) || group.nonEmptyString("id") == null || !group.isNonNegativeInteger("order") ||
            group.string("name") == null || group.nonEmptyString("colorHex") == null ||
            (group.has("iconKey") && group.nonEmptyString("iconKey") == null)
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        replacements["targets"] = canonicalArray(group, "targets", ::canonicalCompositionTarget)
            ?: return null
        if (group.has("cueSettings")) {
            replacements["cueSettings"] = canonicalCueSettings(group.obj("cueSettings") ?: return null)
                ?: return null
        }
        if (group.has("compatibility")) {
            replacements["compatibility"] = canonicalCompatibility(
                group.obj("compatibility") ?: return null
            ) ?: return null
        }
        return orderedObject(group, STAGE_GROUP_ORDER, replacements)
    }

    private fun canonicalCompositionTarget(value: CanonicalJsonValue): CanonicalJsonValue.Obj? {
        val target = value as? CanonicalJsonValue.Obj ?: return null
        if (!target.hasRequiredAndOptionalKeys(
                COMPOSITION_TARGET_REQUIRED,
                setOf("iconKey", "cueSettings", "compatibility")
            ) || target.nonEmptyString("id") == null || !target.isNonNegativeInteger("order") ||
            target.string("name") == null || target.string("kind") !in COMPOSITION_TARGET_KINDS ||
            !target.isNonNegativeInteger("durationSec") || target.nonEmptyString("colorHex") == null ||
            (target.has("iconKey") && target.nonEmptyString("iconKey") == null) ||
            target.boolean("autoAdvance") == null
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        if (target.has("cueSettings")) {
            replacements["cueSettings"] = canonicalCueSettings(target.obj("cueSettings") ?: return null)
                ?: return null
        }
        if (target.has("compatibility")) {
            replacements["compatibility"] = canonicalCompatibility(
                target.obj("compatibility") ?: return null
            ) ?: return null
        }
        return orderedObject(target, COMPOSITION_TARGET_ORDER, replacements)
    }

    private fun canonicalCompatibility(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasRequiredAndOptionalKeys(emptySet(), COMPATIBILITY_KEYS)) return null
        if (value.has("sourceVersion") && value.string("sourceVersion") !in COMPATIBILITY_VERSIONS) return null
        listOf("legacyBlockId", "legacyItemId", "convertedAt").forEach { key ->
            if (value.has(key) && value.string(key) == null) return null
        }
        if (value.has("legacyStageType") && value.string("legacyStageType") !in TIMED_STAGE_TYPES) return null
        return orderedObject(value, COMPATIBILITY_ORDER)
    }

    private fun canonicalStrengthTarget(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasRequiredAndOptionalKeys(emptySet(), STRENGTH_TARGET_KEYS)) return null
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        if (value.has("weight")) {
            replacements["weight"] = canonicalWeight(value.obj("weight") ?: return null) ?: return null
        }
        if (value.has("repTarget")) {
            replacements["repTarget"] = canonicalRepTarget(value.obj("repTarget") ?: return null)
                ?: return null
        }
        if (value.has("restAfterSetSec") && !value.isNonNegativeInteger("restAfterSetSec")) return null
        return orderedObject(value, STRENGTH_TARGET_ORDER, replacements)
    }

    private fun canonicalStrengthSet(value: CanonicalJsonValue): CanonicalJsonValue.Obj? {
        val set = value as? CanonicalJsonValue.Obj ?: return null
        if (!set.hasRequiredAndOptionalKeys(
                setOf("id", "order", "kind"),
                setOf("side", "targetWeight", "repTarget", "restAfterSec")
            ) || set.nonEmptyString("id") == null || !set.isNonNegativeInteger("order") ||
            set.string("kind") !in STRENGTH_SET_KINDS ||
            (set.has("side") && set.string("side") !in EXERCISE_SIDES) ||
            (set.has("restAfterSec") && !set.isNonNegativeInteger("restAfterSec"))
        ) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        if (set.has("targetWeight")) {
            replacements["targetWeight"] = canonicalWeight(set.obj("targetWeight") ?: return null)
                ?: return null
        }
        if (set.has("repTarget")) {
            replacements["repTarget"] = canonicalRepTarget(set.obj("repTarget") ?: return null)
                ?: return null
        }
        return orderedObject(set, STRENGTH_SET_ORDER, replacements)
    }

    private fun canonicalWeight(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasExactKeys(setOf("value", "unit"))) return null
        val number = value.fields["value"] as? CanonicalJsonValue.Num ?: return null
        if (number.value < BigDecimal.ZERO || value.string("unit") !in WEIGHT_UNITS) return null
        return orderedObject(value, listOf("value", "unit"))
    }

    private fun canonicalRepTarget(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        return when (value.string("kind")) {
            "fixed" -> {
                val reps = value.int("reps") ?: return null
                if (value.hasExactKeys(setOf("kind", "reps")) && reps in 1L..200L) {
                    orderedObject(value, listOf("kind", "reps"))
                } else {
                    null
                }
            }

            "range" -> {
                val minReps = value.int("minReps") ?: return null
                val maxReps = value.int("maxReps") ?: return null
                if (
                    value.hasExactKeys(setOf("kind", "minReps", "maxReps")) &&
                    minReps in 1L..200L && maxReps in 1L..200L && minReps <= maxReps
                ) {
                    orderedObject(value, listOf("kind", "minReps", "maxReps"))
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun canonicalPreferences(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasRequiredAndOptionalKeys(emptySet(), setOf("cueSettings", "heartRateDisplay"))) {
            return null
        }
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        if (value.has("cueSettings")) {
            replacements["cueSettings"] = canonicalCueSettings(value.obj("cueSettings") ?: return null)
                ?: return null
        }
        if (value.has("heartRateDisplay")) {
            val display = value.obj("heartRateDisplay") ?: return null
            if (!display.hasExactKeys(setOf("enabled", "showDisconnectedPlaceholder")) ||
                display.boolean("enabled") == null ||
                display.boolean("showDisconnectedPlaceholder") == null
            ) {
                return null
            }
            replacements["heartRateDisplay"] = orderedObject(
                display,
                listOf("enabled", "showDisconnectedPlaceholder")
            )
        }
        return orderedObject(value, listOf("cueSettings", "heartRateDisplay"), replacements)
    }

    private fun canonicalCueSettings(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasRequiredAndOptionalKeys(emptySet(), setOf("actionEnding", "restEnding"))) return null
        val replacements = mutableMapOf<String, CanonicalJsonValue>()
        listOf("actionEnding", "restEnding").forEach { key ->
            if (value.has(key)) {
                replacements[key] = canonicalCountdownCue(value.obj(key) ?: return null) ?: return null
            }
        }
        return orderedObject(value, listOf("actionEnding", "restEnding"), replacements)
    }

    private fun canonicalCountdownCue(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasExactKeys(COUNTDOWN_CUE_KEYS) || value.boolean("enabled") == null ||
            !value.isNonNegativeInteger("thresholdSec") || value.boolean("soundEnabled") == null ||
            value.boolean("vibrationEnabled") == null ||
            value.boolean("emphasisAnimationEnabled") == null ||
            value.boolean("voiceCueEnabled") == null
        ) {
            return null
        }
        return orderedObject(value, COUNTDOWN_CUE_ORDER)
    }

    private fun canonicalFollowAlong(value: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? {
        if (!value.hasRequiredAndOptionalKeys(FOLLOW_ALONG_REQUIRED, FOLLOW_ALONG_OPTIONAL) ||
            value.boolean("preset") == null
        ) {
            return null
        }
        listOf("coverMediaId", "aiAnalysisProfileId").forEach { key ->
            if (value.has(key) && value.string(key) == null) return null
        }
        listOf("coachMediaIds", "chapterIds", "timelineCueIds", "musicTrackIds").forEach { key ->
            val array = value.array(key) ?: return null
            if (array.any { entry -> entry !is CanonicalJsonValue.Str }) return null
        }
        return orderedObject(value, FOLLOW_ALONG_ORDER)
    }
}

object OrderedStructureSignatureInputV1 {
    fun encode(storage: WorkoutPlanSnapshotStorageV1): ByteArray? {
        val prepared = PlanSnapshotStorageV1Validator.prepare(storage.persistedJson, storage.mode)
        return (prepared as? PreparedPlanSnapshotStorageV1Result.Valid)
            ?.prepared
            ?.orderedStructureSignatureInputBytes()
    }

    fun digestHexLowercase(storage: WorkoutPlanSnapshotStorageV1): String? {
        val prepared = PlanSnapshotStorageV1Validator.prepare(storage.persistedJson, storage.mode)
        return (prepared as? PreparedPlanSnapshotStorageV1Result.Valid)
            ?.prepared
            ?.orderedStructureDigestHexLowercase()
    }

    internal fun renderValidated(
        root: CanonicalJsonValue.Obj,
        mode: WorkoutMode
    ): String? {
        val blocks = root.array("blocks")?.map { value ->
            signatureBlock(value as? CanonicalJsonValue.Obj ?: return null) ?: return null
        } ?: return null
        val projection = signatureObject(
            "signatureInputContractVersion" to CanonicalJsonValue.Num(BigDecimal.ONE),
            "mode" to CanonicalJsonValue.Str(mode.contractValue),
            "blocks" to CanonicalJsonValue.Arr(blocks)
        )
        return projection.renderCanonicalJson()
    }

    private fun signatureBlock(block: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj? =
        when (val kind = block.string("kind")) {
            "warmup", "stretch", "cooldown" -> signatureObject(
                "blockId" to block.required("id"),
                "blockKind" to CanonicalJsonValue.Str(kind),
                "order" to block.required("order"),
                "durationSec" to block.nullable("durationSec"),
                "items" to signatureArray(block, "items", ::signatureTimedItem)
            )

            "rest" -> signatureObject(
                "blockId" to block.required("id"),
                "blockKind" to CanonicalJsonValue.Str(kind),
                "order" to block.required("order"),
                "durationSec" to block.required("durationSec")
            )

            "timed_circuit" -> signatureObject(
                "blockId" to block.required("id"),
                "blockKind" to CanonicalJsonValue.Str(kind),
                "order" to block.required("order"),
                "rounds" to block.required("rounds"),
                "restBetweenRoundsSec" to block.nullable("restBetweenRoundsSec"),
                "items" to signatureArray(block, "items", ::signatureTimedItem)
            )

            "timed_composition" -> signatureObject(
                "blockId" to block.required("id"),
                "blockKind" to CanonicalJsonValue.Str(kind),
                "order" to block.required("order"),
                "compositionVersion" to block.required("compositionVersion"),
                "warmupSec" to block.required("warmupSec"),
                "cooldownSec" to block.required("cooldownSec"),
                "rounds" to block.required("rounds"),
                "restBetweenRoundsSec" to block.required("restBetweenRoundsSec"),
                "stageGroups" to signatureArray(block, "stageGroups", ::signatureStageGroup)
            )

            "strength_exercise" -> signatureObject(
                "blockId" to block.required("id"),
                "blockKind" to CanonicalJsonValue.Str(kind),
                "order" to block.required("order"),
                "exerciseId" to block.required("exerciseId"),
                "target" to (block.obj("target")?.let(::signatureStrengthTarget)
                    ?: CanonicalJsonValue.Null),
                "sets" to signatureArray(block, "sets", ::signatureStrengthSet),
                "substitutions" to block.required("substitutions"),
                "setTimerMode" to block.required("setTimerMode")
            )

            else -> null
        }

    private fun signatureTimedItem(item: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "itemId" to item.required("id"),
            "exerciseId" to item.nullable("exerciseId"),
            "side" to item.nullable("side"),
            "stageType" to item.required("stageType"),
            "workDurationSec" to item.required("workDurationSec"),
            "restAfterSec" to item.nullable("restAfterSec"),
            "autoAdvance" to item.required("autoAdvance")
        )

    private fun signatureStageGroup(group: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "stageGroupId" to group.required("id"),
            "order" to group.required("order"),
            "targets" to signatureArray(group, "targets", ::signatureCompositionTarget)
        )

    private fun signatureCompositionTarget(target: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "targetId" to target.required("id"),
            "order" to target.required("order"),
            "targetKind" to target.required("kind"),
            "durationSec" to target.required("durationSec"),
            "autoAdvance" to target.required("autoAdvance")
        )

    private fun signatureStrengthTarget(target: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "weight" to (target.obj("weight")?.let(::signatureWeight) ?: CanonicalJsonValue.Null),
            "repTarget" to (target.obj("repTarget")?.let(::signatureRepTarget)
                ?: CanonicalJsonValue.Null),
            "restAfterSetSec" to target.nullable("restAfterSetSec")
        )

    private fun signatureStrengthSet(set: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "setPlanId" to set.required("id"),
            "order" to set.required("order"),
            "setKind" to set.required("kind"),
            "side" to set.nullable("side"),
            "targetWeight" to (set.obj("targetWeight")?.let(::signatureWeight)
                ?: CanonicalJsonValue.Null),
            "repTarget" to (set.obj("repTarget")?.let(::signatureRepTarget)
                ?: CanonicalJsonValue.Null),
            "restAfterSec" to set.nullable("restAfterSec")
        )

    private fun signatureWeight(weight: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        signatureObject(
            "value" to weight.required("value"),
            "unit" to weight.required("unit")
        )

    private fun signatureRepTarget(target: CanonicalJsonValue.Obj): CanonicalJsonValue.Obj =
        when (target.string("kind")) {
            "fixed" -> signatureObject(
                "kind" to CanonicalJsonValue.Str("fixed"),
                "fixedReps" to target.required("reps"),
                "minReps" to CanonicalJsonValue.Null,
                "maxReps" to CanonicalJsonValue.Null
            )

            "range" -> signatureObject(
                "kind" to CanonicalJsonValue.Str("range"),
                "fixedReps" to CanonicalJsonValue.Null,
                "minReps" to target.required("minReps"),
                "maxReps" to target.required("maxReps")
            )

            else -> error("Validated storage contains an unsupported rep target")
        }
}

private fun signatureArray(
    owner: CanonicalJsonValue.Obj,
    key: String,
    transform: (CanonicalJsonValue.Obj) -> CanonicalJsonValue.Obj
): CanonicalJsonValue.Arr = CanonicalJsonValue.Arr(
    requireNotNull(owner.array(key)).map { value -> transform(value as CanonicalJsonValue.Obj) }
)

private fun signatureObject(
    vararg fields: Pair<String, CanonicalJsonValue>
): CanonicalJsonValue.Obj = CanonicalJsonValue.Obj(linkedMapOf(*fields))

private fun CanonicalJsonValue.Obj.required(key: String): CanonicalJsonValue =
    requireNotNull(fields[key])

private fun CanonicalJsonValue.Obj.nullable(key: String): CanonicalJsonValue =
    fields[key] ?: CanonicalJsonValue.Null

private fun canonicalArray(
    owner: CanonicalJsonValue.Obj,
    key: String,
    canonicalize: (CanonicalJsonValue) -> CanonicalJsonValue?
): CanonicalJsonValue.Arr? {
    val values = owner.array(key) ?: return null
    return CanonicalJsonValue.Arr(values.map { value -> canonicalize(value) ?: return null })
}

private fun orderedObject(
    source: CanonicalJsonValue.Obj,
    order: List<String>,
    replacements: Map<String, CanonicalJsonValue> = emptyMap()
): CanonicalJsonValue.Obj {
    val fields = linkedMapOf<String, CanonicalJsonValue>()
    order.forEach { key ->
        if (source.has(key)) fields[key] = replacements[key] ?: requireNotNull(source.fields[key])
    }
    return CanonicalJsonValue.Obj(fields)
}

private fun CanonicalJsonValue.Obj.validCommonBlock(): Boolean =
    nonEmptyString("id") != null && string("kind") != null && isNonNegativeInteger("order") &&
        (!has("title") || string("title") != null)

private fun CanonicalJsonValue.Obj.has(key: String): Boolean = fields.containsKey(key)

private fun CanonicalJsonValue.Obj.hasExactKeys(keys: Set<String>): Boolean = fields.keys == keys

private fun CanonicalJsonValue.Obj.hasRequiredAndOptionalKeys(
    required: Set<String>,
    optional: Set<String>
): Boolean = fields.keys.containsAll(required) && (required + optional).containsAll(fields.keys)

private fun CanonicalJsonValue.Obj.string(key: String): String? =
    (fields[key] as? CanonicalJsonValue.Str)?.value

private fun CanonicalJsonValue.Obj.nonEmptyString(key: String): String? =
    string(key)?.takeIf { value -> value.isNotEmpty() }

private fun CanonicalJsonValue.Obj.int(key: String): Long? =
    (fields[key] as? CanonicalJsonValue.Num)?.value?.exactLongOrNull()

private fun BigDecimal.exactLongOrNull(): Long? = try {
    longValueExact()
} catch (_: ArithmeticException) {
    null
}

private fun CanonicalJsonValue.Obj.boolean(key: String): Boolean? =
    (fields[key] as? CanonicalJsonValue.Bool)?.value

private fun CanonicalJsonValue.Obj.obj(key: String): CanonicalJsonValue.Obj? =
    fields[key] as? CanonicalJsonValue.Obj

private fun CanonicalJsonValue.Obj.array(key: String): List<CanonicalJsonValue>? =
    (fields[key] as? CanonicalJsonValue.Arr)?.values

private fun CanonicalJsonValue.Obj.isStringOrNull(key: String): Boolean =
    fields[key] is CanonicalJsonValue.Str || fields[key] === CanonicalJsonValue.Null

private fun CanonicalJsonValue.Obj.isNonNegativeInteger(key: String): Boolean =
    int(key)?.let { value -> value >= 0 } == true

private fun CanonicalJsonValue.Obj.isPositiveInteger(key: String): Boolean =
    int(key)?.let { value -> value > 0 } == true

private val ROOT_KEYS = setOf(
    "planSnapshotStorageContractVersion",
    "planId",
    "title",
    "mode",
    "blocks",
    "preferences",
    "followAlong"
)
private val ROOT_ORDER = ROOT_KEYS.toList()
private val COMMON_BLOCK_REQUIRED = setOf("id", "kind", "order")
private val COMMON_BLOCK_ORDER = listOf("id", "kind", "title", "order")
private val COMPOSITION_REQUIRED = setOf(
    "compositionVersion",
    "warmupSec",
    "cooldownSec",
    "rounds",
    "restBetweenRoundsSec",
    "stageGroups"
)
private val COMPOSITION_ORDER = listOf(
    "compositionVersion",
    "warmupSec",
    "warmupStyle",
    "cooldownSec",
    "cooldownStyle",
    "rounds",
    "restBetweenRoundsSec",
    "restBetweenRoundsStyle",
    "stageGroups",
    "compatibility"
)
private val STRENGTH_BLOCK_ORDER = listOf(
    "exerciseId",
    "target",
    "sets",
    "substitutions",
    "setTimerMode"
)
private val TIMED_ITEM_REQUIRED = setOf(
    "id",
    "stageType",
    "iconKey",
    "colorHex",
    "workDurationSec",
    "autoAdvance"
)
private val TIMED_ITEM_ORDER = listOf(
    "id",
    "exerciseId",
    "labelOverride",
    "side",
    "stageType",
    "iconKey",
    "colorHex",
    "workDurationSec",
    "restAfterSec",
    "cueSettings",
    "autoAdvance"
)
private val STAGE_GROUP_REQUIRED = setOf("id", "order", "name", "colorHex", "targets")
private val STAGE_GROUP_ORDER = listOf(
    "id",
    "order",
    "name",
    "colorHex",
    "iconKey",
    "targets",
    "cueSettings",
    "compatibility"
)
private val COMPOSITION_TARGET_REQUIRED = setOf(
    "id",
    "order",
    "name",
    "kind",
    "durationSec",
    "colorHex",
    "autoAdvance"
)
private val COMPOSITION_TARGET_ORDER = listOf(
    "id",
    "order",
    "name",
    "kind",
    "durationSec",
    "colorHex",
    "iconKey",
    "cueSettings",
    "autoAdvance",
    "compatibility"
)
private val COMPATIBILITY_KEYS = setOf(
    "sourceVersion",
    "legacyBlockId",
    "legacyItemId",
    "legacyStageType",
    "convertedAt"
)
private val COMPATIBILITY_ORDER = listOf(
    "sourceVersion",
    "legacyBlockId",
    "legacyItemId",
    "legacyStageType",
    "convertedAt"
)
private val STRENGTH_TARGET_KEYS = setOf("weight", "repTarget", "restAfterSetSec")
private val STRENGTH_TARGET_ORDER = listOf("weight", "repTarget", "restAfterSetSec")
private val STRENGTH_SET_ORDER = listOf(
    "id",
    "order",
    "kind",
    "side",
    "targetWeight",
    "repTarget",
    "restAfterSec"
)
private val COUNTDOWN_CUE_KEYS = setOf(
    "enabled",
    "thresholdSec",
    "soundEnabled",
    "vibrationEnabled",
    "emphasisAnimationEnabled",
    "voiceCueEnabled"
)
private val COUNTDOWN_CUE_ORDER = COUNTDOWN_CUE_KEYS.toList()
private val FOLLOW_ALONG_REQUIRED = setOf(
    "preset",
    "coachMediaIds",
    "chapterIds",
    "timelineCueIds",
    "musicTrackIds"
)
private val FOLLOW_ALONG_OPTIONAL = setOf("coverMediaId", "aiAnalysisProfileId")
private val FOLLOW_ALONG_ORDER = listOf(
    "preset",
    "coverMediaId",
    "coachMediaIds",
    "chapterIds",
    "timelineCueIds",
    "musicTrackIds",
    "aiAnalysisProfileId"
)
private val TIMED_STAGE_TYPES = setOf("warmup", "work", "rest", "cooldown", "custom")
private val EXERCISE_SIDES = setOf("both", "left", "right", "alternating")
private val STRENGTH_TIMER_MODES = setOf("manual_start", "auto_after_rest")
private val COMPOSITION_TARGET_KINDS = setOf("action", "rest", "custom")
private val COMPATIBILITY_VERSIONS = setOf("legacy_timed_circuit", "composition_v2")
private val STRENGTH_SET_KINDS = setOf("warmup", "working", "drop", "backoff")
private val WEIGHT_UNITS = setOf("kg", "lb")
