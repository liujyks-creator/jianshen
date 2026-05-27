package com.liujyks.trainflow.feature.exerciselibrary

import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseLibraryFilters
import com.liujyks.trainflow.core.domain.exerciselibrary.ExerciseTrainingModeFilter
import com.liujyks.trainflow.core.model.EquipmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun mapsExerciseDetailGuidanceFromFixtureInstructions() {
        val detail = requireNotNull(findExerciseDetailUiState("barbell-bench-press"))

        assertEquals("杠铃卧推", detail.name)
        assertEquals("Bench Press", detail.aliasSummary)
        assertEquals("力量", detail.categoryLabel)
        assertEquals("进阶", detail.difficultyLabel)
        assertEquals(listOf("胸部", "肱三头肌"), detail.primaryMuscleLabels)
        assertEquals(listOf("肩部"), detail.secondaryMuscleLabels)
        assertEquals(listOf("杠铃"), detail.equipmentLabels)
        assertTrue("重量" in detail.capabilityLabels)
        assertEquals("脚踩稳，肩胛稳，控制下放再推起。", detail.shortCue)
        assertEquals("力量 3组 8-12次 / 休息120秒", detail.defaultSummary)
        assertEquals(3, detail.steps.size)
        assertTrue("手腕保持稳定。" in detail.keyPoints)
        assertTrue("弹胸借力。" in detail.commonMistakes)
        assertEquals(listOf("下放吸气，推起呼气。"), detail.breathingCues)
        assertTrue(detail.cautions.single().contains("安全架"))
        assertEquals(listOf("上斜俯卧撑 · 器械替代、无器械、较低负荷"), detail.substitutionLabels)
        assertEquals(listOf("胸肩前侧放松"), detail.recoveryAreaLabels)
    }

    @Test
    fun resolvesDetailFromListItemIdAndReturnsNullForMissingExercise() {
        val listState = buildExerciseLibraryUiState(
            ExerciseLibraryFilters(trainingMode = ExerciseTrainingModeFilter.TIMED)
        )
        val firstTimedExerciseId = listState.items.first().id

        assertEquals(firstTimedExerciseId, findExerciseDetailUiState(firstTimedExerciseId)?.id)
        assertNull(findExerciseDetailUiState("missing-exercise"))
    }
}
