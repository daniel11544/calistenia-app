package com.example.domain.usecase

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.model.TodayWorkoutState
import com.example.data.repository.PlanRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.WorkoutRepository
import com.example.worker.DailyReminderWorker
import com.example.worker.HydrationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class GetTodayWorkoutUseCase(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val workoutDao: WorkoutDao,
    private val getStreakUseCase: GetStreakUseCase
) {
    operator fun invoke(): Flow<TodayWorkoutState> = flow {
        val plan = planRepository.getActivePlanDirect()
        if (plan == null) {
            emit(TodayWorkoutState(isLoading = false))
            return@flow
        }

        // Find the current incomplete day or fallback to first day
        val day = workoutRepository.getFirstIncompleteDay(plan.id)
        if (day == null) {
            emit(TodayWorkoutState(plan = plan, isLoading = false, isCompleted = true))
            return@flow
        }

        val week = workoutDao.getWeeksForPlan(plan.id).firstOrNull()
            ?.firstOrNull { it.id == day.weekId }

        val streak = getStreakUseCase()

        if (day.isRestDay) {
            emit(
                TodayWorkoutState(
                    plan = plan,
                    currentWeek = week,
                    currentDay = day,
                    isRestDay = true,
                    isCompleted = day.isCompleted,
                    streakDays = streak,
                    isLoading = false
                )
            )
            return@flow
        }

        workoutRepository.getWorkoutExerciseItems(day.id).collect { items ->
            val allCompleted = items.isNotEmpty() && items.all { it.isCompleted }
            emit(
                TodayWorkoutState(
                    plan = plan,
                    currentWeek = week,
                    currentDay = day,
                    exercises = items,
                    isRestDay = false,
                    isCompleted = day.isCompleted || allCompleted,
                    streakDays = streak,
                    isLoading = false
                )
            )
        }
    }
}

class CompleteWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
    private val progressRepository: ProgressRepository,
    private val getStreakUseCase: GetStreakUseCase
) {
    suspend operator fun invoke(
        dayId: String,
        planId: String,
        durationSeconds: Int,
        totalExercises: Int,
        completedExercises: Int,
        caloriesBurned: Int,
        notes: String = ""
    ) {
        // Mark day complete in Room
        workoutRepository.markDayCompleted(dayId, true)

        // Save session record
        val session = WorkoutSessionEntity(
            id = UUID.randomUUID().toString(),
            planId = planId,
            dayId = dayId,
            dateMillis = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            totalExercises = totalExercises,
            completedExercises = completedExercises,
            caloriesBurned = caloriesBurned,
            notes = notes
        )
        progressRepository.recordSession(session)

        // Update streak achievements
        val newStreak = getStreakUseCase()
        progressRepository.updateStreakAchievements(newStreak)
    }
}

class GetStreakUseCase(private val progressRepository: ProgressRepository) {
    suspend operator fun invoke(): Int {
        val sessions = progressRepository.allSessions.firstOrNull() ?: return 0
        if (sessions.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Group session timestamps by calendar day
        val sessionDays = sessions.map { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.dateMillis }
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
        }.distinct().toSet()

        var streak = 0
        var checkYear = todayYear
        var checkDayOfYear = todayDayOfYear

        // Check if user completed a session today or yesterday
        val doneToday = sessionDays.contains(Pair(todayYear, todayDayOfYear))
        if (!doneToday) {
            // Check yesterday
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            checkYear = calendar.get(Calendar.YEAR)
            checkDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            if (!sessionDays.contains(Pair(checkYear, checkDayOfYear))) {
                return 0
            }
        }

        // Count backwards
        val countCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, checkYear)
            set(Calendar.DAY_OF_YEAR, checkDayOfYear)
        }

        while (true) {
            val key = Pair(countCalendar.get(Calendar.YEAR), countCalendar.get(Calendar.DAY_OF_YEAR))
            if (sessionDays.contains(key)) {
                streak++
                countCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}

class ScheduleRemindersUseCase(private val context: Context) {

    fun scheduleDailyWorkoutReminder(hour: Int, minute: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "daily_workout_reminder_work"

        if (!enabled) {
            workManager.cancelUniqueWork(workTag)
            return
        }

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelayMillis = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workTag,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun scheduleHydrationReminder(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "hydration_reminder_work"

        if (!enabled) {
            workManager.cancelUniqueWork(workTag)
            return
        }

        val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(2, TimeUnit.HOURS)
            .addTag(workTag)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workTag,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
