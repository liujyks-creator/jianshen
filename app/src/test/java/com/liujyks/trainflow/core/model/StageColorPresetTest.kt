package com.liujyks.trainflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StageColorPresetTest {
    @Test
    fun presetIdsAreUniqueAndHexValuesAreValid() {
        assertEquals(StageColorPresets.size, StageColorPresets.map { preset -> preset.id }.toSet().size)
        StageColorPresets.forEach { preset ->
            assertTrue("${preset.id} hex must be valid", isValidStageColorHex(preset.hex))
            assertTrue("${preset.id} text color must be valid", isValidStageColorHex(preset.textColor))
        }
    }

    @Test
    fun recommendedPresetCountStaysFastChoiceSized() {
        assertTrue(RecommendedStageColorPresets.size in 5..8)
        assertTrue(MoreStageColorPresets.size >= 20)
    }

    @Test
    fun everyPresetProvidesPickerAndTalkBackMetadata() {
        StageColorPresets.forEach { preset ->
            assertFalse(preset.name.isBlank())
            assertFalse(preset.tone.isBlank())
            assertFalse(preset.recommendedUse.isBlank())
            assertTrue(preset.accessibilityLabel.contains(preset.name))
            assertTrue(preset.accessibilityLabel.contains(preset.recommendedUse))
        }
    }

    @Test
    fun highAttentionColorsAreExplicitlyMarked() {
        val highAttentionHexes = StageColorPresets
            .filter { preset -> preset.isHighAttention }
            .map { preset -> preset.hex }
            .toSet()

        assertTrue("#F26B4F" in highAttentionHexes)
        assertTrue("#F44336" in highAttentionHexes)
        assertTrue("#FF5722" in highAttentionHexes)
        assertTrue("#FF9800" in highAttentionHexes)
        assertTrue("#FFC107" in highAttentionHexes)
    }

    @Test
    fun lookupAndFallbackNormalizeColorsSafely() {
        assertNotNull(stageColorPresetFor("#ffc107"))
        assertEquals("#FFC107", normalizeStageColorHex("#ffc107", TimedStageType.WORK))
        assertEquals(TimedStageType.REST.defaultColorHex, normalizeStageColorHex("bad", TimedStageType.REST))
        assertEquals("#111820", stageTextColorHexFor("#FFC107", TimedStageType.WORK))
        assertEquals("#FFFFFF", stageTextColorHexFor("#123456", TimedStageType.CUSTOM))
    }
}
