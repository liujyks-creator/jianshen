package com.liujyks.trainflow.feature.exerciselibrary

import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseLibraryFilters
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseTrainingModeFilter
import com.liujyks.trainflow.core.model.EquipmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibraryUiMapperTest {
    @Test
    fun mapsFirstActionFixturesIntoReadableLibraryCards() {
        val state = buildExerciseLibraryUiState(ExerciseLibraryFilters())
        val squat = state.items.first { it.id == "bodyweight-squat" }

        assertEquals(11, state.totalCount)
        assertEquals(11, state.visibleCount)
        assertFalse(state.hasActiveFilters)
        assertEquals("徒手深蹲", squat.name)
        assertEquals("徒手", squat.categoryLabel)
        assertEquals("新手", squat.difficultyLabel)
        assertEquals(listOf("股四头肌", "臀部"), squat.muscleLabels)
        assertEquals(listOf("徒手"), squat.equipmentLabels)
        assertTrue("计时" in squat.capabilityLabels)
        assertTrue("次数" in squat.capabilityLabels)
        assertEquals("膝盖跟脚尖，臀部向后坐。", squat.shortCue)
        assertEquals("计时 40秒 / 休息20秒 · 力量 3组 8-12次 / 休息60秒", squat.defaultSummary)
    }

    @Test
    fun exposesSelectedFiltersAndEmptyStateForUi() {
        val state = buildExerciseLibraryUiState(
            ExerciseLibraryFilters(
                trainingMode = ExerciseTrainingModeFilter.FOLLOW_ALONG,
                equipment = EquipmentKind.BARBELL
            )
        )

        assertTrue(state.hasActiveFilters)
        assertTrue(state.isEmpty)
        assertEquals(0, state.visibleCount)
        assertTrue(state.trainingModeOptions.first { it.value == ExerciseTrainingModeFilter.FOLLOW_ALONG }.selected)
        assertTrue(state.equipmentOptions.first { it.value == EquipmentKind.BARBELL }.selected)
    }
}
