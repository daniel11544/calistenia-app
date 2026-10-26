package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.local.entity.WorkoutDayExerciseEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutWeekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun getExerciseById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseByIdDirect(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE muscleGroup = :group ORDER BY name ASC")
    fun getExercisesByMuscleGroup(group: String): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles")
    suspend fun clearUserProfile()
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<PlanEntity?>

    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlanDirect(): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Query("UPDATE plans SET isActive = 0")
    suspend fun deactivateAllPlans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeks(weeks: List<WorkoutWeekEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<WorkoutDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayExercises(dayExercises: List<WorkoutDayExerciseEntity>)

    @Query("DELETE FROM plans")
    suspend fun clearAllPlans()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_weeks WHERE planId = :planId ORDER BY weekNumber ASC")
    fun getWeeksForPlan(planId: String): Flow<List<WorkoutWeekEntity>>

    @Query("SELECT * FROM workout_days WHERE weekId = :weekId ORDER BY dayNumber ASC")
    fun getDaysForWeek(weekId: String): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE id = :dayId LIMIT 1")
    fun getDayById(dayId: String): Flow<WorkoutDayEntity?>

    @Query("SELECT * FROM workout_days WHERE id = :dayId LIMIT 1")
    suspend fun getDayByIdDirect(dayId: String): WorkoutDayEntity?

    @Query("SELECT d.* FROM workout_days d INNER JOIN workout_weeks w ON d.weekId = w.id WHERE w.planId = :planId AND d.isCompleted = 0 ORDER BY w.weekNumber ASC, d.dayNumber ASC LIMIT 1")
    suspend fun getFirstIncompleteDay(planId: String): WorkoutDayEntity?

    @Query("SELECT d.* FROM workout_days d INNER JOIN workout_weeks w ON d.weekId = w.id WHERE w.planId = :planId ORDER BY w.weekNumber ASC, d.dayNumber ASC LIMIT 1")
    suspend fun getFirstDayOfPlan(planId: String): WorkoutDayEntity?

    @Query("SELECT * FROM workout_day_exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun getDayExercises(dayId: String): Flow<List<WorkoutDayExerciseEntity>>

    @Query("SELECT * FROM workout_day_exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getDayExercisesDirect(dayId: String): List<WorkoutDayExerciseEntity>

    @Query("UPDATE workout_day_exercises SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setDayExerciseCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE workout_day_exercises SET isCompleted = :isCompleted WHERE dayId = :dayId")
    suspend fun setAllDayExercisesCompleted(dayId: String, isCompleted: Boolean)

    @Query("UPDATE workout_days SET isCompleted = :completed, completedAt = :timestamp WHERE id = :dayId")
    suspend fun setDayCompleted(dayId: String, completed: Boolean, timestamp: Long?)
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getSessionsCount(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) / 60 FROM workout_sessions")
    fun getTotalMinutesTrained(): Flow<Int?>

    @Query("SELECT SUM(caloriesBurned) FROM workout_sessions")
    fun getTotalCaloriesBurned(): Flow<Int?>

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("UPDATE achievements SET isUnlocked = 0, unlockedAt = NULL, currentProgress = 0")
    suspend fun resetAchievements()
}
