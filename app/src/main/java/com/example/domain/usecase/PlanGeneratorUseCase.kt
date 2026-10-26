package com.example.domain.usecase

import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.local.entity.WorkoutDayExerciseEntity
import com.example.data.local.entity.WorkoutWeekEntity
import java.util.UUID

data class GeneratedPlanResult(
    val plan: PlanEntity,
    val weeks: List<WorkoutWeekEntity>,
    val days: List<WorkoutDayEntity>,
    val dayExercises: List<WorkoutDayExerciseEntity>
)

class PlanGeneratorUseCase {

    fun generatePlan(
        userProfile: UserProfileEntity,
        availableExercises: List<ExerciseEntity>,
        totalWeeks: Int = 8
    ): GeneratedPlanResult {
        val planId = UUID.randomUUID().toString()
        val daysPerWeek = userProfile.daysPerWeek.coerceIn(2, 6)
        val minutesPerSession = userProfile.minutesPerSession.coerceIn(15, 60)
        val level = userProfile.level
        val goal = userProfile.goal

        val planTitle = "Plan Calistenia: $goal ($level)"
        val planDescription = "Programa de $totalWeeks semanas con sobrecarga progresiva adaptado para $daysPerWeek días a la semana y sesiones de $minutesPerSession min."

        val plan = PlanEntity(
            id = planId,
            title = planTitle,
            description = planDescription,
            totalWeeks = totalWeeks,
            targetGoal = goal,
            targetLevel = level,
            daysPerWeek = daysPerWeek,
            minutesPerSession = minutesPerSession,
            isActive = true
        )

        val weeks = mutableListOf<WorkoutWeekEntity>()
        val days = mutableListOf<WorkoutDayEntity>()
        val dayExercises = mutableListOf<WorkoutDayExerciseEntity>()

        // Split days across 7-day schedule
        val activeDaysOfWeek = getActiveDaysDistribution(daysPerWeek)

        // Exercise count based on session duration
        val exerciseCount = when (minutesPerSession) {
            15 -> 3
            30 -> 5
            45 -> 6
            else -> 7
        }

        // Filter exercises compatible with user level
        val matchedExercises = filterExercisesByLevel(availableExercises, level)

        for (weekNum in 1..totalWeeks) {
            val weekId = UUID.randomUUID().toString()
            val isDeloadWeek = weekNum % 4 == 0

            val weekFocus = when {
                isDeloadWeek -> "Semana $weekNum: Descarga Activa y Recuperación Articular"
                weekNum == 1 -> "Semana $weekNum: Adaptación Anatómica y Técnica"
                weekNum % 2 == 0 -> "Semana $weekNum: Sobrecarga Progresiva (+2 Reps por Serie)"
                else -> "Semana $weekNum: Incremento de Volumen (+1 Serie)"
            }

            weeks.add(
                WorkoutWeekEntity(
                    id = weekId,
                    planId = planId,
                    weekNumber = weekNum,
                    focus = weekFocus
                )
            )

            // Overload increments
            val setsMultiplier = if (isDeloadWeek) -1 else if (weekNum >= 5) 1 else 0
            val repsBonus = if (isDeloadWeek) 0 else ((weekNum - 1) * 2)

            for (dayNum in 1..7) {
                val dayId = UUID.randomUUID().toString()
                val isTrainingDay = activeDaysOfWeek.contains(dayNum)

                if (!isTrainingDay) {
                    days.add(
                        WorkoutDayEntity(
                            id = dayId,
                            weekId = weekId,
                            dayNumber = dayNum,
                            name = "Día $dayNum: Descanso Activo",
                            isRestDay = true,
                            durationMinutes = 0,
                            estimatedCalories = 0
                        )
                    )
                } else {
                    val daySplitName = getDaySplitName(dayNum, daysPerWeek)
                    val estimatedCalories = (minutesPerSession * 7.5).toInt()

                    days.add(
                        WorkoutDayEntity(
                            id = dayId,
                            weekId = weekId,
                            dayNumber = dayNum,
                            name = "Día $dayNum: $daySplitName",
                            isRestDay = false,
                            durationMinutes = minutesPerSession,
                            estimatedCalories = estimatedCalories
                        )
                    )

                    // Pick exercises for this day's split
                    val selectedExercises = pickExercisesForDay(
                        daySplitName = daySplitName,
                        exercises = matchedExercises,
                        count = exerciseCount
                    )

                    selectedExercises.forEachIndexed { index, exercise ->
                        val baseSets = (exercise.defaultSets + setsMultiplier).coerceIn(2, 5)
                        val adjustedReps = formatAdjustedReps(exercise.defaultReps, repsBonus)

                        dayExercises.add(
                            WorkoutDayExerciseEntity(
                                dayId = dayId,
                                exerciseId = exercise.id,
                                orderIndex = index,
                                sets = baseSets,
                                reps = adjustedReps,
                                restSeconds = exercise.defaultRestSeconds
                            )
                        )
                    }
                }
            }
        }

        return GeneratedPlanResult(
            plan = plan,
            weeks = weeks,
            days = days,
            dayExercises = dayExercises
        )
    }

    private fun getActiveDaysDistribution(daysPerWeek: Int): Set<Int> {
        return when (daysPerWeek) {
            2 -> setOf(2, 5) // Tue, Fri
            3 -> setOf(1, 3, 5) // Mon, Wed, Fri
            4 -> setOf(1, 2, 4, 5) // Mon, Tue, Thu, Fri
            5 -> setOf(1, 2, 3, 5, 6) // Mon, Tue, Wed, Fri, Sat
            6 -> setOf(1, 2, 3, 4, 5, 6) // Mon - Sat
            else -> setOf(1, 3, 5)
        }
    }

    private fun getDaySplitName(dayNum: Int, daysPerWeek: Int): String {
        return when (daysPerWeek) {
            2 -> if (dayNum <= 3) "Cuerpo Completo A (Fuerza)" else "Cuerpo Completo B (Volumen)"
            3 -> when (dayNum) {
                1 -> "Full Body: Empuje y Piernas"
                3 -> "Full Body: Tracción y Core"
                else -> "Full Body: Metabólico y Potencia"
            }
            4 -> when (dayNum) {
                1 -> "Torso: Pecho, Hombros y Tríceps"
                2 -> "Piernas y Core"
                4 -> "Torso: Espalda y Bíceps"
                else -> "Piernas y Abdomen"
            }
            5 -> when (dayNum) {
                1 -> "Empuje (Pecho, Hombros)"
                2 -> "Tracción (Espalda, Bíceps)"
                3 -> "Piernas (Fuerza y Saltos)"
                5 -> "Torso Completo"
                else -> "Core y Acondicionamiento"
            }
            else -> when (dayNum) {
                1 -> "Empuje Pesado"
                2 -> "Tracción Pesada"
                3 -> "Piernas y Pantorrillas"
                4 -> "Empuje Volumen"
                5 -> "Tracción Volumen"
                else -> "Piernas y Core"
            }
        }
    }

    private fun filterExercisesByLevel(
        exercises: List<ExerciseEntity>,
        level: String
    ): List<ExerciseEntity> {
        if (exercises.isEmpty()) return emptyList()

        return when {
            level.contains("Principiante", ignoreCase = true) ->
                exercises.filter { it.difficulty == "Principiante" || it.difficulty == "Intermedio" }
            level.contains("Avanzado", ignoreCase = true) ->
                exercises.filter { it.difficulty == "Avanzado" || it.difficulty == "Intermedio" }
            else ->
                exercises // Intermedio uses full spectrum
        }.ifEmpty { exercises }
    }

    private fun pickExercisesForDay(
        daySplitName: String,
        exercises: List<ExerciseEntity>,
        count: Int
    ): List<ExerciseEntity> {
        val targetGroups = when {
            daySplitName.contains("Empuje", ignoreCase = true) -> listOf("Pecho", "Hombros", "Brazos", "Core")
            daySplitName.contains("Tracción", ignoreCase = true) -> listOf("Espalda", "Brazos", "Core")
            daySplitName.contains("Pierna", ignoreCase = true) -> listOf("Piernas", "Core")
            daySplitName.contains("Torso", ignoreCase = true) -> listOf("Pecho", "Espalda", "Hombros", "Brazos")
            daySplitName.contains("Core", ignoreCase = true) -> listOf("Core", "Hombros", "Piernas")
            else -> listOf("Pecho", "Espalda", "Piernas", "Core", "Hombros")
        }

        val result = mutableListOf<ExerciseEntity>()
        for (group in targetGroups) {
            val groupMatches = exercises.filter { it.muscleGroup.equals(group, ignoreCase = true) && !result.contains(it) }
            if (groupMatches.isNotEmpty() && result.size < count) {
                result.add(groupMatches.shuffled().first())
            }
        }

        // Fill remaining if needed
        val remaining = exercises.filter { !result.contains(it) }.shuffled()
        for (ex in remaining) {
            if (result.size >= count) break
            result.add(ex)
        }

        return result
    }

    private fun formatAdjustedReps(baseReps: String, repsBonus: Int): String {
        if (repsBonus <= 0) return baseReps
        val regex = Regex("(\\d+)-?(\\d*)")
        val match = regex.find(baseReps)
        return if (match != null) {
            val firstNum = match.groupValues[1].toIntOrNull() ?: return baseReps
            val secondNum = match.groupValues[2].toIntOrNull()
            if (secondNum != null) {
                "${firstNum + repsBonus}-${secondNum + repsBonus} reps"
            } else {
                "${firstNum + repsBonus} reps"
            }
        } else {
            baseReps
        }
    }
}
