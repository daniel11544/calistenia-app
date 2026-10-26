package com.example

import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.usecase.PlanGeneratorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGeneratorUseCaseTest {

    private val useCase = PlanGeneratorUseCase()

    private val sampleExercises = listOf(
        ExerciseEntity(
            id = "pushup_regular",
            name = "Flexiones Clásicas",
            muscleGroup = "Pecho",
            difficulty = "Principiante",
            equipment = "Ninguno",
            description = "Desc",
            instructions = listOf("Step 1", "Step 2"),
            commonMistakes = listOf("Mistake 1"),
            defaultSets = 3,
            defaultReps = "10-12 reps",
            defaultRestSeconds = 60,
            easierVariation = "Incline pushup",
            harderVariation = "Diamond pushup"
        ),
        ExerciseEntity(
            id = "squat_air",
            name = "Sentadillas al Aire",
            muscleGroup = "Piernas",
            difficulty = "Principiante",
            equipment = "Ninguno",
            description = "Desc",
            instructions = listOf("Step 1"),
            commonMistakes = emptyList(),
            defaultSets = 3,
            defaultReps = "15-20 reps",
            defaultRestSeconds = 60,
            easierVariation = "",
            harderVariation = ""
        ),
        ExerciseEntity(
            id = "plank_isometric",
            name = "Plancha Abdominal",
            muscleGroup = "Core",
            difficulty = "Principiante",
            equipment = "Ninguno",
            description = "Desc",
            instructions = listOf("Step 1"),
            commonMistakes = emptyList(),
            defaultSets = 3,
            defaultReps = "45 seg",
            defaultRestSeconds = 45,
            easierVariation = "",
            harderVariation = ""
        )
    )

    @Test
    fun generatePlan_createsCorrectStructure() {
        val profile = UserProfileEntity(
            id = 1,
            age = 26,
            gender = "Hombre",
            weightKg = 72f,
            heightCm = 178f,
            level = "Principiante",
            goal = "Ganar masa muscular",
            daysPerWeek = 3,
            minutesPerSession = 30
        )

        val result = useCase.generatePlan(
            userProfile = profile,
            availableExercises = sampleExercises,
            totalWeeks = 8
        )

        assertNotNull(result.plan)
        assertEquals(8, result.weeks.size)
        // 8 weeks * 7 days = 56 days
        assertEquals(56, result.days.size)

        // For 3 days/week, each week should have 3 training days and 4 rest days
        val week1Days = result.days.take(7)
        val activeDaysCount = week1Days.count { !it.isRestDay }
        val restDaysCount = week1Days.count { it.isRestDay }

        assertEquals(3, activeDaysCount)
        assertEquals(4, restDaysCount)
    }

    @Test
    fun generatePlan_respectsRestDaysAndProgressiveOverload() {
        val profile = UserProfileEntity(
            id = 1,
            age = 30,
            gender = "Mujer",
            weightKg = 60f,
            heightCm = 165f,
            level = "Principiante",
            goal = "Perder grasa y definir",
            daysPerWeek = 4,
            minutesPerSession = 45
        )

        val result = useCase.generatePlan(
            userProfile = profile,
            availableExercises = sampleExercises,
            totalWeeks = 4
        )

        val week1Days = result.days.filter { it.weekId == result.weeks[0].id }
        assertEquals(4, week1Days.count { !it.isRestDay })
        assertEquals(3, week1Days.count { it.isRestDay })

        // Check deload week 4
        val week4 = result.weeks[3]
        assertTrue(week4.focus.contains("Descarga", ignoreCase = true))
    }
}
