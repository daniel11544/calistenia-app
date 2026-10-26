package com.example.data.model

import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutWeekEntity

data class WorkoutExerciseItem(
    val id: Long,
    val dayId: String,
    val exercise: ExerciseEntity,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val isCompleted: Boolean
)

data class TodayWorkoutState(
    val plan: PlanEntity? = null,
    val currentWeek: WorkoutWeekEntity? = null,
    val currentDay: WorkoutDayEntity? = null,
    val exercises: List<WorkoutExerciseItem> = emptyList(),
    val isRestDay: Boolean = false,
    val isCompleted: Boolean = false,
    val streakDays: Int = 0,
    val isLoading: Boolean = true
)

data class UserProgressSummary(
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val totalCalories: Int = 0,
    val currentStreak: Int = 0,
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList()
)
