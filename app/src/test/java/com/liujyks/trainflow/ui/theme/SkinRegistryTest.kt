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
    }

    @Test
    fun skinMetadataStatesAudienceAndCapabilityBoundary() {
        SkinRegistry.skins.forEach { skin ->
            assertTrue(skin.displayName.isNotBlank())
            assertTrue(skin.description.isNotBlank())
            assertTrue(skin.targetUser.isNotBlank())
            assertTrue(skin.capabilityBoundary.contains("不") || skin.capabilityBoundary.contains("E8.1"))
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
    fun themeColorSchemeConsumesCurrentSkinTokens() {
        val bigType = SkinRegistry.resolve("big_type")
        val colorScheme = trainFlowLightColorSchemeForSkin(bigType)

        assertEquals(bigType.tokens.primary, colorScheme.primary)
        assertEquals(bigType.tokens.action, colorScheme.tertiary)
        assertEquals(bigType.tokens.surfaceMuted, colorScheme.background)
        assertEquals(Color(0xFF0E1418), colorScheme.onBackground)
        assertFalse(bigType.isDefault)
    }
}
