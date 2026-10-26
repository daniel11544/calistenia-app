package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataStoreManager
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.PlanRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.domain.usecase.PlanGeneratorUseCase
import com.example.domain.usecase.ScheduleRemindersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfileEntity? = null,
    val activePlan: PlanEntity? = null,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 18,
    val dailyReminderMinute: Int = 0,
    val hydrationReminderEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val isLoading: Boolean = true
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val exerciseRepository: ExerciseRepository,
    private val progressRepository: ProgressRepository,
    private val planGeneratorUseCase: PlanGeneratorUseCase,
    private val scheduleRemindersUseCase: ScheduleRemindersUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            val profile = userRepository.getUserProfileDirect()
            val plan = planRepository.getActivePlanDirect()
            val dailyEnabled = dataStoreManager.dailyReminderEnabled.firstOrNull() ?: true
            val hour = dataStoreManager.dailyReminderHour.firstOrNull() ?: 18
            val minute = dataStoreManager.dailyReminderMinute.firstOrNull() ?: 0
            val hydrationEnabled = dataStoreManager.hydrationReminderEnabled.firstOrNull() ?: true
            val darkMode = dataStoreManager.darkModeOption.firstOrNull() != "light"

            _uiState.value = ProfileUiState(
                userProfile = profile,
                activePlan = plan,
                dailyReminderEnabled = dailyEnabled,
                dailyReminderHour = hour,
                dailyReminderMinute = minute,
                hydrationReminderEnabled = hydrationEnabled,
                isDarkMode = darkMode,
                isLoading = false
            )
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDailyReminderEnabled(enabled)
            val state = _uiState.value
            scheduleRemindersUseCase.scheduleDailyWorkoutReminder(
                state.dailyReminderHour,
                state.dailyReminderMinute,
                enabled
            )
            _uiState.value = state.copy(dailyReminderEnabled = enabled)
        }
    }

    fun toggleHydrationReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setHydrationReminderEnabled(enabled)
            scheduleRemindersUseCase.scheduleHydrationReminder(enabled)
            _uiState.value = _uiState.value.copy(hydrationReminderEnabled = enabled)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkModeOption(if (enabled) "dark" else "light")
            _uiState.value = _uiState.value.copy(isDarkMode = enabled)
        }
    }

    fun regeneratePlan(daysPerWeek: Int, durationMinutes: Int, goal: String, level: String) {
        viewModelScope.launch {
            val current = _uiState.value.userProfile ?: return@launch
            val updatedProfile = current.copy(
                daysPerWeek = daysPerWeek,
                minutesPerSession = durationMinutes,
                goal = goal,
                level = level
            )
            userRepository.saveProfile(updatedProfile)

            val exercises = exerciseRepository.allExercises.firstOrNull() ?: emptyList()
            val generated = planGeneratorUseCase.generatePlan(
                userProfile = updatedProfile,
                availableExercises = exercises,
                totalWeeks = 8
            )

            planRepository.saveGeneratedPlan(
                plan = generated.plan,
                weeks = generated.weeks,
                days = generated.days,
                dayExercises = generated.dayExercises
            )

            loadProfileData()
        }
    }

    fun resetAllData(onResetDone: () -> Unit) {
        viewModelScope.launch {
            progressRepository.resetAll()
            planRepository.clearAll()
            userRepository.clearProfile()
            dataStoreManager.setOnboardingCompleted(false)
            onResetDone()
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val planRepository: PlanRepository,
        private val exerciseRepository: ExerciseRepository,
        private val progressRepository: ProgressRepository,
        private val planGeneratorUseCase: PlanGeneratorUseCase,
        private val scheduleRemindersUseCase: ScheduleRemindersUseCase,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(
                userRepository,
                planRepository,
                exerciseRepository,
                progressRepository,
                planGeneratorUseCase,
                scheduleRemindersUseCase,
                dataStoreManager
            ) as T
        }
    }
}
