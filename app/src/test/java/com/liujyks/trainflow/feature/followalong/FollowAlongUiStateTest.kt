package com.liujyks.trainflow.feature.followalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongUiStateTest {
    @Test
    fun freeFollowAlongEntryOffersSingleStartWithoutPreset() {
        val state = buildDefaultFollowAlongScreenState()

        assertEquals("跟练", state.title)
        assertEquals("开始跟练", state.startLabel)
        assertTrue(state.canStartFollowAlong)
        assertFalse(state.summary.contains("动作清单"))
        assertFalse(state.summary.contains("预计"))
        assertFalse(state.summary.contains("计划"))
    }

    @Test
    fun entryCopyUsesExternalPlaybackWithoutMediaControls() {
        val state = buildDefaultFollowAlongScreenState()

        assertTrue(state.summary.contains("其他设备播放视频或直播"))
        assertTrue(state.summary.contains("记录本次训练"))
        listOf("播放器", "课程选择", "AI", "媒体占位").forEach { word ->
            assertFalse(state.summary.contains(word))
        }
    }
}
