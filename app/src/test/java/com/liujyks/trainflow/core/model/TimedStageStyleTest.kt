package com.liujyks.trainflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedStageStyleTest {
    @Test
    fun styleNormalizationPreservesValidColorAndKnownIconKey() {
        val style = TimedStageStyle(
            colorHex = "#f2b84b",
            iconKey = "recover_breathe"
        ).normalized()

        assertEquals(TimedStageStyle(colorHex = "#F2B84B", iconKey = "recover_breathe"), style)
    }

    @Test
    fun styleNormalizationDropsInvalidColorAndUnknownIconKey() {
        val invalidStyle = TimedStageStyle(
            colorHex = "orange",
            iconKey = "moon"
        ).normalized()

        assertNull(invalidStyle)
        assertNull(TimedStageStyle(colorHex = "#12345", iconKey = "workout.png").normalized())
    }

    @Test
    fun builtInIconKeyContractContainsStageStyleKeys() {
        val keys = TimedStageIconKey.entries.map { key -> key.contractValue }.toSet()

        assertEquals(
            setOf(
                "warmup",
                "work",
                "speed_up",
                "sprint",
                "rest",
                "recover_breathe",
                "cooldown",
                "strength",
                "mobility",
                "custom"
            ),
            keys
        )
        assertTrue(isKnownTimedStageIconKey("warmup"))
        assertTrue(isKnownTimedStageIconKey("custom"))
        assertFalse(isKnownTimedStageIconKey("unknown"))
    }

    @Test
    fun assetLikeIconKeysAreRejected() {
        val rejectedKeys = listOf(
            "https://example.com/icon.png",
            "file:///tmp/icon.svg",
            "C:/Users/me/icon.png",
            "res/drawable/ic_stage.svg",
            "@drawable/ic_stage",
            "icons/warmup.svg",
            "warmup.png",
            "data:image/png;base64,abc",
            "uploaded_asset_123"
        )

        rejectedKeys.forEach { key ->
            assertNull("Expected asset-like icon key to be rejected: $key", normalizeTimedStageIconKey(key))
        }
    }
}
