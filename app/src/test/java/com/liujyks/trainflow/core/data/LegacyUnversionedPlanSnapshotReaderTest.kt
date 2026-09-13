package com.liujyks.trainflow.core.data

import com.liujyks.trainflow.core.database.CanonicalJsonValue
import com.liujyks.trainflow.core.model.WorkoutMode
import java.math.BigDecimal
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyUnversionedPlanSnapshotReaderTest {
    @Test
    fun acceptsLegacyFamiliesWithoutRewriting() {
        assertValid("V1/T", timed(), WorkoutMode.TIMED)
        assertValid("V1/S", strength(), WorkoutMode.STRENGTH)
        assertValid("V1/F", followAlong(), WorkoutMode.FOLLOW_ALONG)
        val whitespaceJson = " \n" + """{"blocks":[],"mode":"timed","title":"\u8bad\n\""}""" + " \t"
        val expected = linkedMapOf<String, Any?>(
            "blocks" to emptyList<Any?>(),
            "mode" to "timed",
            "title" to "训\n\""
        )
        assertValid("V1/W/whitespace-and-escapes", expected, WorkoutMode.TIMED, whitespaceJson)
    }

    @Test
    fun preservesRootOmissionNullAndMode() {
        modes.forEach { (text, mode) ->
            val base = bare(text)
            val values = linkedMapOf<String, Any?>(
                "planId" to "",
                "preferences" to linkedMapOf<String, Any?>(),
                "followAlong" to linkedMapOf<String, Any?>(
                    "preset" to false,
                    "coachMediaIds" to emptyList<Any?>(),
                    "chapterIds" to emptyList<Any?>(),
                    "timelineCueIds" to emptyList<Any?>(),
                    "musicTrackIds" to emptyList<Any?>()
                )
            )
            assertValid("V2/$text/all-absent", base, mode)
            assertValid("V2/$text/all-null", base + values.mapValues { null }, mode)
            values.keys.forEach { key ->
                assertValid("V2/$text/$key-null", base + (key to null), mode)
            }
            values.forEach { (key, value) ->
                assertValid("V2/$text/$key-value", base + (key to value), mode)
            }
            assertValid("V2/$text/all-values", base + values, mode)
            values.keys.forEach { key ->
                assertInvalid("V2/$text/$key-false", render(base + (key to false)), mode)
            }
        }
    }

    @Test
    fun rejectsMalformedVersionedAndMismatchedRoots() {
        val malformed = linkedMapOf(
            "empty" to "",
            "null" to "null",
            "array" to "[]",
            "boolean" to "true",
            "number" to "1",
            "string" to "\"text\"",
            "open-object" to "{",
            "trailing-value" to """{"title":"","mode":"timed","blocks":[]} false""",
            "duplicate-root-key" to """{"title":"a","title":"b","mode":"timed","blocks":[]}""",
            "duplicate-block-key" to """{"title":"","mode":"timed","blocks":[{"id":"r","id":"r","kind":"rest","order":0,"durationSec":0}]}""",
            "trailing-comma" to """{"title":"","mode":"timed","blocks":[],}""",
            "unquoted-key" to """{title:"","mode":"timed","blocks":[]}"""
        )
        malformed.forEach { (name, json) -> assertInvalid("V3/$name", json, WorkoutMode.TIMED) }
        listOf<Any?>(BigDecimal("1"), BigDecimal("2"), null, "1", true, linkedMapOf<String, Any?>())
            .forEachIndexed { index, version ->
                assertInvalid(
                    "V3/B/version-$index",
                    render(bare("timed") + ("planSnapshotStorageContractVersion" to version)),
                    WorkoutMode.TIMED
                )
            }
        modes.forEach { (text, _) ->
            modes.filter { it.first != text }.forEach { (_, wrongMode) ->
                assertInvalid("V3/$text/mismatch-$wrongMode", render(bare(text)), wrongMode)
            }
        }
        assertInvalid("V3/B/unknown-mode", render(bare("unknown")), WorkoutMode.TIMED)
    }

    @Test
    fun rejectsMissingNullWrongTypedRequiredAndUnknownMembers() {
        val base = bare("timed")
        listOf("title", "mode", "blocks").forEach { key ->
            assertInvalid("V4/B/$key-missing", render(base - key), WorkoutMode.TIMED)
            assertInvalid("V4/B/$key-null", render(base + (key to null)), WorkoutMode.TIMED)
            val wrongType = if (key == "blocks") linkedMapOf<String, Any?>() else false
            assertInvalid("V4/B/$key-wrong-type", render(base + (key to wrongType)), WorkoutMode.TIMED)
        }
        assertInvalid("V4/B/unknown-member", render(base + ("unexpected" to true)), WorkoutMode.TIMED)
    }

    @Test
    fun rejectsInvalidBlocksAndRootNestedShapes() {
        (0..5).forEach { index ->
            assertInvalid(
                "V5/T/blocks[$index].kind-unknown",
                render(change(timed(), listOf("blocks", index, "kind")) { "unknown" }),
                WorkoutMode.TIMED
            )
        }
        assertInvalid(
            "V5/S/blocks[0].kind-unknown",
            render(change(strength(), listOf("blocks", 0, "kind")) { "unknown" }),
            WorkoutMode.STRENGTH
        )
        assertInvalid(
            "V5/T/preferences-unknown-member",
            render(change(timed(), listOf("preferences")) { (it as Map<*, *>) + ("unexpected" to true) }),
            WorkoutMode.TIMED
        )
        assertInvalid(
            "V5/F/followAlong-unknown-member",
            render(change(followAlong(), listOf("followAlong")) { (it as Map<*, *>) + ("unexpected" to true) }),
            WorkoutMode.FOLLOW_ALONG
        )
    }

    @Test
    fun usesExistingNestedValidationWithoutFallback() {
        assertInvalid(
            "V6/T/workDurationSec-string",
            render(change(timed(), listOf("blocks", 2, "items", 0, "workDurationSec")) { "30" }),
            WorkoutMode.TIMED
        )
        assertInvalid(
            "V6/T/target-kind-unknown",
            render(change(timed(), listOf("blocks", 4, "stageGroups", 0, "targets", 0, "kind")) { "unknown" }),
            WorkoutMode.TIMED
        )
        assertInvalid(
            "V6/S/maxReps-zero",
            render(change(strength(), listOf("blocks", 0, "sets", 0, "repTarget", "maxReps")) { BigDecimal("0") }),
            WorkoutMode.STRENGTH
        )
        assertInvalid(
            "V6/T/enabled-string",
            render(change(timed(), listOf("preferences", "cueSettings", "actionEnding", "enabled")) { "true" }),
            WorkoutMode.TIMED
        )
        assertInvalid(
            "V6/F/coachMediaId-number",
            render(change(followAlong(), listOf("followAlong", "coachMediaIds", 0)) { BigDecimal("0") }),
            WorkoutMode.FOLLOW_ALONG
        )
    }

    @Test
    fun preservesArraysAndRejectsInvalidElements() {
        val selectors = listOf(
            Triple(timed(), WorkoutMode.TIMED, listOf<Any>("blocks")),
            Triple(timed(), WorkoutMode.TIMED, listOf<Any>("blocks", 2, "items")),
            Triple(timed(), WorkoutMode.TIMED, listOf<Any>("blocks", 4, "stageGroups", 0, "targets")),
            Triple(strength(), WorkoutMode.STRENGTH, listOf<Any>("blocks", 0, "sets")),
            Triple(followAlong(), WorkoutMode.FOLLOW_ALONG, listOf<Any>("followAlong", "coachMediaIds"))
        )
        selectors.forEach { (base, mode, path) ->
            val id = "V7/$mode/" + path.joinToString(".")
            assertValid("$id/reverse", change(base, path) { (it as List<*>).reversed() }, mode)
            assertValid("$id/duplicate-first", change(base, path) {
                val array = it as List<*>
                array + listOf(array.first())
            }, mode)
            assertInvalid("$id/insert-null", render(change(base, path) {
                val array = it as List<*>
                array.take(1) + listOf(null) + array.drop(1)
            }), mode)
        }
    }

    private fun assertValid(
        id: String,
        expected: Any?,
        mode: WorkoutMode,
        json: String = render(expected)
    ) {
        val result = LegacyUnversionedPlanSnapshotReader.read(json, mode)
        assertTrue("$id: $result", result is LegacyUnversionedPlanSnapshotReadResult.Valid)
        result as LegacyUnversionedPlanSnapshotReadResult.Valid
        assertEquals("$id/mode", mode, result.mode)
        assertArrayEquals(
            "$id/persisted-UTF8",
            json.toByteArray(Charsets.UTF_8),
            result.persistedJson.toByteArray(Charsets.UTF_8)
        )
        assertEquals("$id/complete-root", expectedAst(expected), result.root)
    }

    private fun assertInvalid(id: String, json: String, mode: WorkoutMode) {
        val result = LegacyUnversionedPlanSnapshotReader.read(json, mode)
        assertTrue("$id: $result", result is LegacyUnversionedPlanSnapshotReadResult.Invalid)
        assertEquals(
            "$id/code",
            "invalid_legacy_unversioned_plan_snapshot",
            (result as LegacyUnversionedPlanSnapshotReadResult.Invalid).code
        )
    }

    // Independent literal trees supply fixed JSON inputs and complete expected AST values.
    // Each change copies the selected path and leaves the original golden untouched.
    private fun change(value: Any?, path: List<Any>, replace: (Any?) -> Any?): Any? {
        if (path.isEmpty()) return replace(value)
        val key = path.first()
        val rest = path.drop(1)
        return when (value) {
            is Map<*, *> -> value.entries.associateTo(linkedMapOf()) { (name, child) ->
                name to if (name == key) change(child, rest, replace) else child
            }
            is List<*> -> value.mapIndexed { index, child ->
                if (index == key) change(child, rest, replace) else child
            }
            else -> error("Unsupported fixture path: $path")
        }
    }

    private fun render(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        is Boolean -> value.toString()
        is BigDecimal -> value.toPlainString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (key, child) ->
            render(key as String) + ":" + render(child)
        }
        is List<*> -> value.joinToString(",", "[", "]") { render(it) }
        else -> error("Unsupported fixture value: $value")
    }

    private fun expectedAst(value: Any?): CanonicalJsonValue = when (value) {
        null -> CanonicalJsonValue.Null
        is String -> CanonicalJsonValue.Str(value)
        is Boolean -> CanonicalJsonValue.Bool(value)
        is BigDecimal -> CanonicalJsonValue.Num(value)
        is Map<*, *> -> CanonicalJsonValue.Obj(value.entries.associateTo(linkedMapOf()) { (key, child) ->
            (key as String) to expectedAst(child)
        })
        is List<*> -> CanonicalJsonValue.Arr(value.map { expectedAst(it) })
        else -> error("Unsupported expected literal: $value")
    }

    private val modes = listOf(
        "timed" to WorkoutMode.TIMED,
        "strength" to WorkoutMode.STRENGTH,
        "follow_along" to WorkoutMode.FOLLOW_ALONG
    )

    private fun bare(mode: String): Map<String, Any?> = linkedMapOf(
        "title" to "",
        "mode" to mode,
        "blocks" to emptyList<Any?>()
    )

    private fun timed(): Map<String, Any?> = linkedMapOf<String, Any?>(
        "title" to "",
        "mode" to "timed",
        "planId" to "plan-A",
        "preferences" to linkedMapOf<String, Any?>(
            "cueSettings" to linkedMapOf<String, Any?>(
                "actionEnding" to linkedMapOf<String, Any?>(
                    "enabled" to true,
                    "thresholdSec" to BigDecimal("0"),
                    "soundEnabled" to false,
                    "vibrationEnabled" to true,
                    "emphasisAnimationEnabled" to false,
                    "voiceCueEnabled" to false
                ),
                "restEnding" to linkedMapOf<String, Any?>(
                    "enabled" to false,
                    "thresholdSec" to BigDecimal("0"),
                    "soundEnabled" to false,
                    "vibrationEnabled" to true,
                    "emphasisAnimationEnabled" to false,
                    "voiceCueEnabled" to false
                )
            ),
            "heartRateDisplay" to linkedMapOf<String, Any?>(
                "enabled" to false,
                "showDisconnectedPlaceholder" to true
            )
        ),
        "followAlong" to null,
        "blocks" to listOf(
            linkedMapOf<String, Any?>(
                "id" to "warm",
                "kind" to "warmup",
                "order" to BigDecimal("5"),
                "title" to "",
                "durationSec" to BigDecimal("0"),
                "items" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "item-A",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "both",
                        "stageType" to "warmup",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("30"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false,
                        "cueSettings" to linkedMapOf<String, Any?>(
                            "actionEnding" to linkedMapOf<String, Any?>(
                                "enabled" to true,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            ),
                            "restEnding" to linkedMapOf<String, Any?>(
                                "enabled" to false,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            )
                        )
                    )
                )
            ),
            linkedMapOf<String, Any?>(
                "id" to "stretch",
                "kind" to "stretch",
                "order" to BigDecimal("2"),
                "title" to "",
                "durationSec" to BigDecimal("1"),
                "items" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "item-S",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "both",
                        "stageType" to "custom",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("30"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false
                    )
                )
            ),
            linkedMapOf<String, Any?>(
                "id" to "circuit",
                "kind" to "timed_circuit",
                "order" to BigDecimal("2"),
                "title" to "",
                "rounds" to BigDecimal("2"),
                "restBetweenRoundsSec" to BigDecimal("0"),
                "items" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "item-A",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "both",
                        "stageType" to "work",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("30"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false,
                        "cueSettings" to linkedMapOf<String, Any?>(
                            "actionEnding" to linkedMapOf<String, Any?>(
                                "enabled" to true,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            ),
                            "restEnding" to linkedMapOf<String, Any?>(
                                "enabled" to false,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            )
                        )
                    ),
                    linkedMapOf<String, Any?>(
                        "id" to "item-B",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "left",
                        "stageType" to "work",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("0"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false
                    )
                )
            ),
            linkedMapOf<String, Any?>(
                "id" to "rest",
                "kind" to "rest",
                "order" to BigDecimal("1"),
                "title" to "",
                "durationSec" to BigDecimal("0"),
                "label" to ""
            ),
            linkedMapOf<String, Any?>(
                "id" to "composition",
                "kind" to "timed_composition",
                "order" to BigDecimal("8"),
                "title" to "",
                "compositionVersion" to BigDecimal("2"),
                "warmupSec" to BigDecimal("0"),
                "warmupStyle" to linkedMapOf<String, Any?>(
                    "colorHex" to "#123456",
                    "iconKey" to "timer"
                ),
                "cooldownSec" to BigDecimal("0"),
                "cooldownStyle" to linkedMapOf<String, Any?>(),
                "rounds" to BigDecimal("1"),
                "restBetweenRoundsSec" to BigDecimal("0"),
                "restBetweenRoundsStyle" to linkedMapOf<String, Any?>(),
                "stageGroups" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "group-A",
                        "order" to BigDecimal("3"),
                        "name" to "",
                        "colorHex" to "#654321",
                        "iconKey" to "timer",
                        "cueSettings" to linkedMapOf<String, Any?>(
                            "actionEnding" to linkedMapOf<String, Any?>(
                                "enabled" to true,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            ),
                            "restEnding" to linkedMapOf<String, Any?>(
                                "enabled" to false,
                                "thresholdSec" to BigDecimal("0"),
                                "soundEnabled" to false,
                                "vibrationEnabled" to true,
                                "emphasisAnimationEnabled" to false,
                                "voiceCueEnabled" to false
                            )
                        ),
                        "compatibility" to linkedMapOf<String, Any?>(
                            "sourceVersion" to "legacy_timed_circuit",
                            "legacyBlockId" to "",
                            "legacyItemId" to "item-A",
                            "legacyStageType" to "work",
                            "convertedAt" to "historical-text"
                        ),
                        "targets" to listOf(
                            linkedMapOf<String, Any?>(
                                "id" to "target-A",
                                "order" to BigDecimal("2"),
                                "name" to "",
                                "kind" to "action",
                                "durationSec" to BigDecimal("30"),
                                "colorHex" to "#123456",
                                "iconKey" to "timer",
                                "cueSettings" to linkedMapOf<String, Any?>(
                                    "actionEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to true,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    ),
                                    "restEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to false,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    )
                                ),
                                "compatibility" to linkedMapOf<String, Any?>(
                                    "sourceVersion" to "legacy_timed_circuit",
                                    "legacyBlockId" to "",
                                    "legacyItemId" to "item-A",
                                    "legacyStageType" to "work",
                                    "convertedAt" to "historical-text"
                                ),
                                "autoAdvance" to false
                            ),
                            linkedMapOf<String, Any?>(
                                "id" to "target-B",
                                "order" to BigDecimal("1"),
                                "name" to "",
                                "kind" to "rest",
                                "durationSec" to BigDecimal("0"),
                                "colorHex" to "#123456",
                                "iconKey" to "timer",
                                "cueSettings" to linkedMapOf<String, Any?>(
                                    "actionEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to true,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    ),
                                    "restEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to false,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    )
                                ),
                                "compatibility" to linkedMapOf<String, Any?>(
                                    "sourceVersion" to "legacy_timed_circuit",
                                    "legacyBlockId" to "",
                                    "legacyItemId" to "item-A",
                                    "legacyStageType" to "work",
                                    "convertedAt" to "historical-text"
                                ),
                                "autoAdvance" to false
                            ),
                            linkedMapOf<String, Any?>(
                                "id" to "target-C",
                                "order" to BigDecimal("1"),
                                "name" to "",
                                "kind" to "custom",
                                "durationSec" to BigDecimal("30"),
                                "colorHex" to "#123456",
                                "iconKey" to "timer",
                                "cueSettings" to linkedMapOf<String, Any?>(
                                    "actionEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to true,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    ),
                                    "restEnding" to linkedMapOf<String, Any?>(
                                        "enabled" to false,
                                        "thresholdSec" to BigDecimal("0"),
                                        "soundEnabled" to false,
                                        "vibrationEnabled" to true,
                                        "emphasisAnimationEnabled" to false,
                                        "voiceCueEnabled" to false
                                    )
                                ),
                                "compatibility" to linkedMapOf<String, Any?>(
                                    "sourceVersion" to "legacy_timed_circuit",
                                    "legacyBlockId" to "",
                                    "legacyItemId" to "item-A",
                                    "legacyStageType" to "work",
                                    "convertedAt" to "historical-text"
                                ),
                                "autoAdvance" to false
                            )
                        )
                    )
                ),
                "compatibility" to linkedMapOf<String, Any?>(
                    "sourceVersion" to "legacy_timed_circuit",
                    "legacyBlockId" to "",
                    "legacyItemId" to "item-A",
                    "legacyStageType" to "work",
                    "convertedAt" to "historical-text"
                )
            ),
            linkedMapOf<String, Any?>(
                "id" to "cool",
                "kind" to "cooldown",
                "order" to BigDecimal("0"),
                "title" to "",
                "durationSec" to BigDecimal("0"),
                "items" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "item-C",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "both",
                        "stageType" to "cooldown",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("30"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false
                    )
                )
            )
        )
    )

    private fun strength(): Map<String, Any?> = linkedMapOf<String, Any?>(
        "title" to "力量",
        "mode" to "strength",
        "planId" to null,
        "preferences" to linkedMapOf<String, Any?>(),
        "followAlong" to null,
        "blocks" to listOf(
            linkedMapOf<String, Any?>(
                "id" to "strength-A",
                "kind" to "strength_exercise",
                "order" to BigDecimal("4"),
                "title" to "",
                "exerciseId" to "squat",
                "setTimerMode" to "manual_start",
                "substitutions" to listOf(
                    "alt-B",
                    "alt-A",
                    "alt-B"
                ),
                "target" to linkedMapOf<String, Any?>(
                    "weight" to linkedMapOf<String, Any?>(
                        "value" to BigDecimal("0"),
                        "unit" to "kg"
                    ),
                    "repTarget" to linkedMapOf<String, Any?>(
                        "kind" to "fixed",
                        "reps" to BigDecimal("1")
                    ),
                    "restAfterSetSec" to BigDecimal("0")
                ),
                "sets" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "set-B",
                        "order" to BigDecimal("2"),
                        "kind" to "working",
                        "side" to "right",
                        "targetWeight" to linkedMapOf<String, Any?>(
                            "value" to BigDecimal("2.5"),
                            "unit" to "lb"
                        ),
                        "repTarget" to linkedMapOf<String, Any?>(
                            "kind" to "range",
                            "minReps" to BigDecimal("1"),
                            "maxReps" to BigDecimal("200")
                        ),
                        "restAfterSec" to BigDecimal("0")
                    ),
                    linkedMapOf<String, Any?>(
                        "id" to "set-A",
                        "order" to BigDecimal("0"),
                        "kind" to "warmup"
                    )
                )
            )
        )
    )

    private fun followAlong(): Map<String, Any?> = linkedMapOf<String, Any?>(
        "title" to "跟练",
        "mode" to "follow_along",
        "blocks" to listOf(
            linkedMapOf<String, Any?>(
                "id" to "follow",
                "kind" to "timed_circuit",
                "order" to BigDecimal("0"),
                "rounds" to BigDecimal("1"),
                "items" to listOf(
                    linkedMapOf<String, Any?>(
                        "id" to "item-A",
                        "exerciseId" to "exercise-A",
                        "labelOverride" to "",
                        "side" to "both",
                        "stageType" to "work",
                        "iconKey" to "timer",
                        "colorHex" to "#ABCDEF",
                        "workDurationSec" to BigDecimal("30"),
                        "restAfterSec" to BigDecimal("0"),
                        "autoAdvance" to false
                    )
                )
            )
        ),
        "followAlong" to linkedMapOf<String, Any?>(
            "preset" to false,
            "coverMediaId" to "",
            "coachMediaIds" to listOf(
                "c2",
                "",
                "c2"
            ),
            "chapterIds" to listOf(
                "ch2",
                "ch1"
            ),
            "timelineCueIds" to listOf(
                "t2",
                "t1"
            ),
            "musicTrackIds" to listOf(
                "m2",
                "m1"
            ),
            "aiAnalysisProfileId" to ""
        )
    )
}
