package com.example.data.repository

import com.example.data.local.dao.ExerciseDao
import com.example.data.local.dao.PlanDao
import com.example.data.local.dao.ProgressDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.local.entity.WorkoutDayExerciseEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutWeekEntity
import com.example.data.model.WorkoutExerciseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ExerciseRepository(private val exerciseDao: ExerciseDao) {
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun getExerciseById(id: String): Flow<ExerciseEntity?> = exerciseDao.getExerciseById(id)

    suspend fun getExerciseByIdDirect(id: String): ExerciseEntity? = exerciseDao.getExerciseByIdDirect(id)

    fun getExercisesByMuscleGroup(group: String): Flow<List<ExerciseEntity>> =
        if (group == "Todos") exerciseDao.getAllExercises()
        else exerciseDao.getExercisesByMuscleGroup(group)
}

class UserRepository(private val userDao: UserDao) {
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun getUserProfileDirect(): UserProfileEntity? = userDao.getUserProfileDirect()

    suspend fun saveProfile(profile: UserProfileEntity) {
        userDao.insertOrUpdateProfile(profile)
    }

    suspend fun clearProfile() {
        userDao.clearUserProfile()
    }
}

class PlanRepository(
    private val planDao: PlanDao,
    private val workoutDao: WorkoutDao
) {
    val activePlan: Flow<PlanEntity?> = planDao.getActivePlan()

    suspend fun getActivePlanDirect(): PlanEntity? = planDao.getActivePlanDirect()

    suspend fun saveGeneratedPlan(
        plan: PlanEntity,
        weeks: List<WorkoutWeekEntity>,
        days: List<WorkoutDayEntity>,
        dayExercises: List<WorkoutDayExerciseEntity>
    ) {
        planDao.deactivateAllPlans()
        planDao.insertPlan(plan)
        planDao.insertWeeks(weeks)
        planDao.insertDays(days)
        planDao.insertDayExercises(dayExercises)
    }

    suspend fun clearAll() {
        planDao.clearAllPlans()
    }
}

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) {
    fun getWeeksForPlan(planId: String): Flow<List<WorkoutWeekEntity>> =
        workoutDao.getWeeksForPlan(planId)

    fun getDaysForWeek(weekId: String): Flow<List<WorkoutDayEntity>> =
        workoutDao.getDaysForWeek(weekId)

    fun getDayById(dayId: String): Flow<WorkoutDayEntity?> =
        workoutDao.getDayById(dayId)

    suspend fun getDayByIdDirect(dayId: String): WorkoutDayEntity? =
        workoutDao.getDayByIdDirect(dayId)

    suspend fun getFirstIncompleteDay(planId: String): WorkoutDayEntity? =
        workoutDao.getFirstIncompleteDay(planId) ?: workoutDao.getFirstDayOfPlan(planId)

    fun getWorkoutExerciseItems(dayId: String): Flow<List<WorkoutExerciseItem>> {
        return workoutDao.getDayExercises(dayId).map { dayExercises ->
            dayExercises.mapNotNull { de ->
                val exercise = exerciseDao.getExerciseByIdDirect(de.exerciseId)
                exercise?.let {
                    WorkoutExerciseItem(
                        id = de.id,
                        dayId = de.dayId,
                        exercise = it,
                        orderIndex = de.orderIndex,
                        sets = de.sets,
                        reps = de.reps,
                        restSeconds = de.restSeconds,
                        isCompleted = de.isCompleted
                    )
                }
            }
        }
    }

    suspend fun getDayExercisesDirect(dayId: String): List<WorkoutDayExerciseEntity> =
        workoutDao.getDayExercisesDirect(dayId)

    suspend fun setExerciseCompleted(dayExerciseId: Long, isCompleted: Boolean) {
        workoutDao.setDayExerciseCompleted(dayExerciseId, isCompleted)
    }

    suspend fun markDayCompleted(dayId: String, completed: Boolean) {
        val timestamp = if (completed) System.currentTimeMillis() else null
        workoutDao.setDayCompleted(dayId, completed, timestamp)
        if (completed) {
            workoutDao.setAllDayExercisesCompleted(dayId, true)
        }
    }
}

class ProgressRepository(private val progressDao: ProgressDao) {
    val allSessions: Flow<List<WorkoutSessionEntity>> = progressDao.getAllSessions()
    val totalSessionsCount: Flow<Int> = progressDao.getSessionsCount()
    val totalMinutesTrained: Flow<Int> = progressDao.getTotalMinutesTrained().map { it ?: 0 }
    val totalCaloriesBurned: Flow<Int> = progressDao.getTotalCaloriesBurned().map { it ?: 0 }
    val allAchievements: Flow<List<AchievementEntity>> = progressDao.getAllAchievements()

    suspend fun recordSession(session: WorkoutSessionEntity) {
        progressDao.insertWorkoutSession(session)
        checkAndUnlockAchievements(session)
    }

    private suspend fun checkAndUnlockAchievements(newSession: WorkoutSessionEntity) {
        // Unlock first session achievement
        val firstAch = progressDao.getAchievementById("first_session")
        if (firstAch != null && !firstAch.isUnlocked) {
            progressDao.updateAchievement(
                firstAch.copy(
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis(),
                    currentProgress = 1
                )
            )
        }

        // Check sessions count
        val totalSessions = progressDao.getAllSessions().firstOrNull()?.size ?: 1

        val ach5 = progressDao.getAchievementById("workouts_5")
        if (ach5 != null) {
            val progress = totalSessions.coerceAtMost(ach5.maxProgress)
            progressDao.updateAchievement(
                ach5.copy(
                    currentProgress = progress,
                    isUnlocked = progress >= ach5.maxProgress,
                    unlockedAt = if (progress >= ach5.maxProgress && ach5.unlockedAt == null) System.currentTimeMillis() else ach5.unlockedAt
                )
            )
        }

        val ach15 = progressDao.getAchievementById("workouts_15")
        if (ach15 != null) {
            val progress = totalSessions.coerceAtMost(ach15.maxProgress)
            progressDao.updateAchievement(
                ach15.copy(
                    currentProgress = progress,
                    isUnlocked = progress >= ach15.maxProgress,
                    unlockedAt = if (progress >= ach15.maxProgress && ach15.unlockedAt == null) System.currentTimeMillis() else ach15.unlockedAt
                )
            )
        }
    }

    suspend fun updateStreakAchievements(currentStreak: Int) {
        val streak3 = progressDao.getAchievementById("streak_3")
        if (streak3 != null) {
            val progress = currentStreak.coerceAtMost(streak3.maxProgress)
            progressDao.updateAchievement(
                streak3.copy(
                    currentProgress = progress,
                    isUnlocked = progress >= streak3.maxProgress,
                    unlockedAt = if (progress >= streak3.maxProgress && streak3.unlockedAt == null) System.currentTimeMillis() else streak3.unlockedAt
                )
            )
        }

        val streak7 = progressDao.getAchievementById("streak_7")
        if (streak7 != null) {
            val progress = currentStreak.coerceAtMost(streak7.maxProgress)
            progressDao.updateAchievement(
                streak7.copy(
                    currentProgress = progress,
                    isUnlocked = progress >= streak7.maxProgress,
                    unlockedAt = if (progress >= streak7.maxProgress && streak7.unlockedAt == null) System.currentTimeMillis() else streak7.unlockedAt
                )
            )
        }
    }

    suspend fun resetAll() {
        progressDao.clearSessions()
        progressDao.resetAchievements()
    }
}
