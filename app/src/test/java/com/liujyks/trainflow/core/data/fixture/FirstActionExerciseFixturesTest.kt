package com.liujyks.trainflow.core.data.fixture

import com.liujyks.trainflow.core.model.ContentStatus
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.RepTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstActionExerciseFixturesTest {
    @Test
    fun firstActionSliceImportsTheAcceptedElevenExerciseIds() {
        val ids = FirstActionExerciseFixtures.exercises.map { it.id }

        assertEquals(
            listOf(
                "jumping-jacks",
                "bodyweight-squat",
                "incline-push-up",
                "forearm-plank",
                "alternating-reverse-lunge",
                "glute-bridge",
                "dumbbell-goblet-squat",
                "one-arm-dumbbell-row",
                "dumbbell-romanian-deadlift",
                "barbell-bench-press",
                "standing-quad-stretch"
            ),
            ids
        )
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun requiredExerciseContractFieldsAreFilled() {
        FirstActionExerciseFixtures.exercises.forEach { exercise ->
            assertTrue("${exercise.id} name", exercise.name.isNotBlank())
            assertTrue("${exercise.id} category", exercise.category.isNotBlank())
            assertTrue("${exercise.id} primary muscles", exercise.primaryMuscleIds.isNotEmpty())
            assertTrue("${exercise.id} equipment", exercise.equipment.isNotEmpty())
            assertTrue("${exercise.id} roles", exercise.roles.isNotEmpty())
            assertTrue("${exercise.id} short cue", exercise.instructions.shortCue.isNotBlank())
            assertTrue("${exercise.id} steps", exercise.instructions.steps.size in 2..5)
            assertTrue("${exercise.id} key points", exercise.instructions.keyPoints.size in 2..5)
            assertTrue("${exercise.id} common mistakes", exercise.instructions.commonMistakes.size >= 2)
            assertTrue("${exercise.id} cautions", exercise.instructions.cautions.isNotEmpty())
            assertNotNull("${exercise.id} recovery", exercise.recovery)
            assertEquals("${exercise.id} content status", ContentStatus.REVIEWED, exercise.contentStatus)
            assertEquals(
                "${exercise.id} source",
                listOf("docs/planning/action-content-slice.md"),
                exercise.sourceMeta?.sourceRefs
            )
            assertTrue("${exercise.id} extensions stay empty", exercise.extensions.isEmpty())
        }
    }

    @Test
    fun trainingTypeMetadataMatchesExerciseCapabilitiesAndDefaults() {
        FirstActionExerciseFixtures.entries.forEach { entry ->
            val capabilities = entry.exercise.capabilities

            when (entry.trainingTypeSupport) {
                TrainingTypeSupport.TIMED -> {
                    assertTrue(entry.exercise.id, capabilities.supportsTimedTraining)
                    assertFalse(entry.exercise.id, capabilities.supportsWeight)
                    assertNotNull(entry.exercise.id, entry.timedDefault)
                    assertEquals(entry.exercise.id, null, entry.strengthDefault)
                }

                TrainingTypeSupport.STRENGTH -> {
                    assertFalse(entry.exercise.id, capabilities.supportsTimedTraining)
                    assertTrue(entry.exercise.id, capabilities.supportsReps)
                    assertNotNull(entry.exercise.id, entry.strengthDefault)
                    assertEquals(entry.exercise.id, null, entry.timedDefault)
                }

                TrainingTypeSupport.BOTH -> {
                    assertTrue(entry.exercise.id, capabilities.supportsTimedTraining)
                    assertTrue(entry.exercise.id, capabilities.supportsReps)
                    assertNotNull(entry.exercise.id, entry.timedDefault)
                    assertNotNull(entry.exercise.id, entry.strengthDefault)
                }
            }
        }
    }

    @Test
    fun defaultSuggestionsStayWithinFirstVersionBoundaries() {
        FirstActionExerciseFixtures.entries.forEach { entry ->
            entry.timedDefault?.let { timed ->
                assertTrue("${entry.exercise.id} work duration", timed.workDurationSec > 0)
                assertTrue("${entry.exercise.id} rest duration", timed.restAfterSec >= 0)
                assertTrue("${entry.exercise.id} rounds", timed.minRounds in 1..timed.maxRounds)
            }

            entry.strengthDefault?.let { strength ->
                assertTrue("${entry.exercise.id} set count", strength.sets >= 1)
                assertTrue("${entry.exercise.id} rest", strength.restAfterSetSec > 0)
                when (val target = strength.repTarget) {
                    is RepTarget.Fixed -> assertTrue("${entry.exercise.id} fixed reps", target.reps > 0)
                    is RepTarget.Range -> {
                        assertTrue("${entry.exercise.id} min reps", target.minReps > 0)
                        assertTrue("${entry.exercise.id} rep range", target.minReps <= target.maxReps)
                    }
                }
            }
        }
    }

    @Test
    fun standingQuadStretchTimedDefaultIsPerSideInsteadOfLeftOnly() {
        val entry = FirstActionExerciseFixtures.entries.single {
            it.exercise.id == "standing-quad-stretch"
        }
        val timedDefault = requireNotNull(entry.timedDefault)

        assertTrue(entry.exercise.capabilities.isUnilateral)
        assertEquals(30, timedDefault.workDurationSec)
        assertEquals(5, timedDefault.restAfterSec)
        assertEquals(ExerciseSide.ALTERNATING, timedDefault.side)
    }

    @Test
    fun bodyAreasEquipmentGuidanceAndSubstitutionsAreStableForMvpUse() {
        val exerciseIds = FirstActionExerciseFixtures.exercises.map { it.id }.toSet()
        val recoveryAreaIds = FirstActionExerciseFixtures.exercises.flatMap { exercise ->
            exercise.recovery?.recommendedRecoveryAreaIds.orEmpty()
        }.toSet()

        assertTrue("lower-body-release" in recoveryAreaIds)
        assertTrue("posterior-chain-release" in recoveryAreaIds)
        assertTrue("chest-shoulder-release" in recoveryAreaIds)
        assertTrue("upper-back-release" in recoveryAreaIds)
        assertTrue("core-breathing-reset" in recoveryAreaIds)

        FirstActionExerciseFixtures.exercises.forEach { exercise ->
            exercise.substitutions.forEach { substitution ->
                assertTrue(
                    "${exercise.id} substitution ${substitution.exerciseId} should stay inside the first fixture slice",
                    substitution.exerciseId in exerciseIds
                )
            }
        }
    }
}
