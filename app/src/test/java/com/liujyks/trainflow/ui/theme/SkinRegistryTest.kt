package com.liujyks.trainflow.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinRegistryTest {
    @Test
    fun registryExposesThreeBuiltInSkinsWithOfficialDefault() {
        val skins = SkinRegistry.skins

        assertEquals(3, skins.size)
        assertEquals("official_flow", SkinRegistry.defaultSkin.id)
        assertTrue(SkinRegistry.defaultSkin.isDefault)
        assertEquals(
            listOf("official_flow", "tile_flow", "big_type"),
            skins.map { skin -> skin.id }
        )
        assertEquals(
            BuiltInUiSkin.entries.map { skin -> skin.id },
            skins.map { skin -> skin.id }
        )
        assertEquals(
            listOf("official_flow"),
            skins.filter { skin -> skin.isDefault }.map { skin -> skin.id }
        )
    }

    @Test
    fun skinMetadataStatesAudienceAndCapabilityBoundary() {
        SkinRegistry.skins.forEach { skin ->
            assertTrue(skin.displayName.isNotBlank())
            assertTrue(skin.description.isNotBlank())
            assertTrue(skin.targetUser.isNotBlank())
            assertTrue(skin.description.length >= 16)
            assertTrue(skin.targetUser.length >= 12)
            assertTrue(skin.capabilityBoundary.contains("不改变"))
            assertTrue(skin.capabilityBoundary.contains("训练"))
        }
    }

    @Test
    fun unknownSkinIdFallsBackToOfficialFlow() {
        assertEquals(SkinRegistry.defaultSkin, SkinRegistry.resolve("remote_market_skin"))
        assertEquals(SkinRegistry.defaultSkin, SkinRegistry.resolve(null))
    }

    @Test
    fun builtInSkinsHaveObservableTokenDifferences() {
        val official = SkinRegistry.resolve("official_flow")
        val tile = SkinRegistry.resolve("tile_flow")
        val bigType = SkinRegistry.resolve("big_type")

        assertNotEquals(official.tokens.primary, tile.tokens.primary)
        assertNotEquals(official.tokens.cardCornerDp, tile.tokens.cardCornerDp)
        assertNotEquals(official.tokens.timerScale, bigType.tokens.timerScale)
    }

    @Test
    fun tileFlowAndBigTypeDefineDistinctCompleteLayoutPolicies() {
        val official = SkinRegistry.resolve("official_flow")
        val tile = SkinRegistry.resolve("tile_flow")
        val bigType = SkinRegistry.resolve("big_type")

        assertTrue(tile.isTileFlow)
        assertEquals(16, tile.tokens.pageHorizontalPaddingDp)
        assertEquals(12, tile.tokens.sectionSpacingDp)
        assertEquals(18, tile.tokens.cardCornerDp)
        assertEquals(22, tile.tokens.prominentCardCornerDp)
        assertTrue(tile.description.contains("磁贴"))
        assertTrue(tile.capabilityBoundary.contains("不改变训练流程"))

        assertFalse(official.isTileFlow)
        assertFalse(bigType.isTileFlow)
        assertTrue(bigType.isBigType)
        assertTrue(bigType.description.contains("固定底部控制"))
        assertTrue(bigType.capabilityBoundary.contains("信息密集页沿用现有组合"))
        assertEquals(16, bigType.tokens.pageHorizontalPaddingDp)
        assertEquals(12, bigType.tokens.sectionSpacingDp)
        assertEquals(64, bigType.tokens.trainingButtonHeightDp)
        assertEquals(52, bigType.tokens.secondaryButtonHeightDp)
        assertEquals(188, bigType.tokens.executionControlReserveDp)
        assertTrue(bigType.tokens.fontScale > official.tokens.fontScale)
        assertTrue(bigType.tokens.timerScale > official.tokens.timerScale)
        assertEquals(48, official.tokens.trainingButtonHeightDp)
        assertEquals(48, tile.tokens.trainingButtonHeightDp)
        assertEquals(160, official.tokens.executionControlReserveDp)
        assertEquals(160, tile.tokens.executionControlReserveDp)
    }

    @Test
    fun themeColorSchemeConsumesCurrentSkinTokens() {
        val bigType = SkinRegistry.resolve("big_type")
        val colorScheme = trainFlowLightColorSchemeForSkin(bigType)

        assertEquals(bigType.tokens.primary, colorScheme.primary)
        assertEquals(bigType.tokens.action, colorScheme.tertiary)
        assertEquals(bigType.tokens.surfaceMuted, colorScheme.background)
        assertEquals(Color(0xFF080D10), colorScheme.onBackground)
        assertFalse(bigType.isDefault)
    }
}
