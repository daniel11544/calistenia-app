package com.example.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataStoreManager
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.PlanRepository
import com.example.data.repository.UserRepository
import com.example.domain.usecase.PlanGeneratorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0, // 0: Welcome, 1: Physical, 2: Goal, 3: Availability
    val age: Int = 25,
    val gender: String = "Hombre",
    val weightKg: Float = 70f,
    val heightCm: Float = 175f,
    val level: String = "Principiante",
    val goal: String = "Ganar masa muscular",
    val daysPerWeek: Int = 3,
    val minutesPerSession: Int = 30,
    val isGenerating: Boolean = false,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planRepository: PlanRepository,
    private val planGeneratorUseCase: PlanGeneratorUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        if (_uiState.value.currentStep < 3) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
        } else {
            finishOnboardingAndGeneratePlan()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun updateAge(age: Int) {
        _uiState.value = _uiState.value.copy(age = age.coerceIn(14, 99))
    }

    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun updateWeight(weight: Float) {
        _uiState.value = _uiState.value.copy(weightKg = weight.coerceIn(35f, 250f))
    }

    fun updateHeight(height: Float) {
        _uiState.value = _uiState.value.copy(heightCm = height.coerceIn(120f, 230f))
    }

    fun updateLevel(level: String) {
        _uiState.value = _uiState.value.copy(level = level)
    }

    fun updateGoal(goal: String) {
        _uiState.value = _uiState.value.copy(goal = goal)
    }

    fun updateDaysPerWeek(days: Int) {
        _uiState.value = _uiState.value.copy(daysPerWeek = days.coerceIn(2, 6))
    }

    fun updateMinutesPerSession(minutes: Int) {
        _uiState.value = _uiState.value.copy(minutesPerSession = minutes)
    }

    fun finishOnboardingAndGeneratePlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)

            val currentState = _uiState.value
            val profile = UserProfileEntity(
                id = 1,
                age = currentState.age,
                gender = currentState.gender,
                weightKg = currentState.weightKg,
                heightCm = currentState.heightCm,
                level = currentState.level,
                goal = currentState.goal,
                daysPerWeek = currentState.daysPerWeek,
                minutesPerSession = currentState.minutesPerSession,
                isOnboarded = true
            )

            // 1. Save user profile to Room
            userRepository.saveProfile(profile)

            // 2. Fetch available exercises
            val exercises = exerciseRepository.allExercises.firstOrNull() ?: emptyList()

            // 3. Generate 8-week progressive plan
            val generatedPlan = planGeneratorUseCase.generatePlan(
                userProfile = profile,
                availableExercises = exercises,
                totalWeeks = 8
            )

            // 4. Save plan and schedule
            planRepository.saveGeneratedPlan(
                plan = generatedPlan.plan,
                weeks = generatedPlan.weeks,
                days = generatedPlan.days,
                dayExercises = generatedPlan.dayExercises
            )

            // 5. Mark onboarding completed in DataStore
            dataStoreManager.setOnboardingCompleted(true)

            _uiState.value = _uiState.value.copy(isGenerating = false, isCompleted = true)
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val exerciseRepository: ExerciseRepository,
        private val planRepository: PlanRepository,
        private val planGeneratorUseCase: PlanGeneratorUseCase,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(
                userRepository,
                exerciseRepository,
                planRepository,
                planGeneratorUseCase,
                dataStoreManager
            ) as T
        }
    }
}
