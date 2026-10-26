package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.GeminiRepository
import com.example.data.repository.PlanRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WorkoutRepository
import com.example.domain.usecase.CompleteWorkoutSessionUseCase
import com.example.domain.usecase.GetStreakUseCase
import com.example.domain.usecase.GetTodayWorkoutUseCase
import com.example.domain.usecase.PlanGeneratorUseCase
import com.example.domain.usecase.ScheduleRemindersUseCase
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FitApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    // Repositories
    lateinit var exerciseRepository: ExerciseRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var planRepository: PlanRepository
        private set

    lateinit var workoutRepository: WorkoutRepository
        private set

    lateinit var progressRepository: ProgressRepository
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    // Use cases
    lateinit var planGeneratorUseCase: PlanGeneratorUseCase
        private set

    lateinit var getStreakUseCase: GetStreakUseCase
        private set

    lateinit var getTodayWorkoutUseCase: GetTodayWorkoutUseCase
        private set

    lateinit var completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase
        private set

    lateinit var scheduleRemindersUseCase: ScheduleRemindersUseCase
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        dataStoreManager = DataStoreManager(this)
        notificationHelper = NotificationHelper(this)

        exerciseRepository = ExerciseRepository(database.exerciseDao())
        userRepository = UserRepository(database.userDao())
        planRepository = PlanRepository(database.planDao(), database.workoutDao())
        workoutRepository = WorkoutRepository(database.workoutDao(), database.exerciseDao())
        progressRepository = ProgressRepository(database.progressDao())
        geminiRepository = GeminiRepository()

        planGeneratorUseCase = PlanGeneratorUseCase()
        getStreakUseCase = GetStreakUseCase(progressRepository)
        getTodayWorkoutUseCase = GetTodayWorkoutUseCase(
            planRepository,
            workoutRepository,
            database.workoutDao(),
            getStreakUseCase
        )
        completeWorkoutSessionUseCase = CompleteWorkoutSessionUseCase(
            workoutRepository,
            progressRepository,
            getStreakUseCase
        )
        scheduleRemindersUseCase = ScheduleRemindersUseCase(this)

        // Seed exercises and setup default reminders in background
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.populateInitialData(this@FitApp, database)

            // If user already completed onboarding, verify the plan has exercises
            try {
                val profile = userRepository.getUserProfileDirect()
                if (profile != null && profile.isOnboarded) {
                    val activePlan = planRepository.getActivePlanDirect()
                    val exercises = exerciseRepository.allExercises.firstOrNull() ?: emptyList()
                    if (exercises.isNotEmpty()) {
                        val firstDay = activePlan?.let { workoutRepository.getFirstIncompleteDay(it.id) }
                        val dayExercises = firstDay?.let { workoutRepository.getDayExercisesDirect(it.id) } ?: emptyList()
                        if (activePlan == null || dayExercises.isEmpty()) {
                            val generatedPlan = planGeneratorUseCase.generatePlan(
                                userProfile = profile,
                                availableExercises = exercises,
                                totalWeeks = 8
                            )
                            planRepository.saveGeneratedPlan(
                                plan = generatedPlan.plan,
                                weeks = generatedPlan.weeks,
                                days = generatedPlan.days,
                                dayExercises = generatedPlan.dayExercises
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val reminderEnabled = dataStoreManager.dailyReminderEnabled.firstOrNull() ?: true
            val hour = dataStoreManager.dailyReminderHour.firstOrNull() ?: 18
            val minute = dataStoreManager.dailyReminderMinute.firstOrNull() ?: 0
            val hydrationEnabled = dataStoreManager.hydrationReminderEnabled.firstOrNull() ?: true

            scheduleRemindersUseCase.scheduleDailyWorkoutReminder(hour, minute, reminderEnabled)
            scheduleRemindersUseCase.scheduleHydrationReminder(hydrationEnabled)
        }
    }
}
