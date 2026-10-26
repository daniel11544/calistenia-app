package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int,
    val gender: String,
    val weightKg: Float,
    val heightCm: Float,
    val level: String,
    val goal: String,
    val daysPerWeek: Int,
    val minutesPerSession: Int,
    val isOnboarded: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val totalWeeks: Int,
    val targetGoal: String,
    val targetLevel: String,
    val daysPerWeek: Int,
    val minutesPerSession: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(
    tableName = "workout_weeks",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutWeekEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val weekNumber: Int,
    val focus: String
)

@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutWeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weekId")]
)
data class WorkoutDayEntity(
    @PrimaryKey val id: String,
    val weekId: String,
    val dayNumber: Int,
    val name: String,
    val isRestDay: Boolean,
    val durationMinutes: Int,
    val estimatedCalories: Int,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val difficulty: String,
    val equipment: String,
    val description: String,
    val instructions: List<String>,
    val commonMistakes: List<String>,
    val defaultSets: Int,
    val defaultReps: String,
    val defaultRestSeconds: Int,
    val easierVariation: String,
    val harderVariation: String
)

@Entity(
    tableName = "workout_day_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId"), Index("exerciseId")]
)
data class WorkoutDayExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val dayId: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val totalExercises: Int,
    val completedExercises: Int,
    val caloriesBurned: Int,
    val notes: String = ""
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconType: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val currentProgress: Int = 0,
    val maxProgress: Int = 1
)
