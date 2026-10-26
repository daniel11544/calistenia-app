package com.example.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.domain.usecase.GetStreakUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ProgressUiState(
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val totalCalories: Int = 0,
    val streakDays: Int = 0,
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val isGeminiLoading: Boolean = false,
    val geminiAdvice: String? = null,
    val isLoading: Boolean = true
)

class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository,
    private val geminiRepository: GeminiRepository,
    private val getStreakUseCase: GetStreakUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    fun loadProgressData() {
        viewModelScope.launch {
            val streak = getStreakUseCase()
            val sessions = progressRepository.allSessions.firstOrNull() ?: emptyList()
            val achievements = progressRepository.allAchievements.firstOrNull() ?: emptyList()
            val minutes = progressRepository.totalMinutesTrained.firstOrNull() ?: 0
            val calories = progressRepository.totalCaloriesBurned.firstOrNull() ?: 0

            _uiState.value = _uiState.value.copy(
                totalSessions = sessions.size,
                totalMinutes = minutes,
                totalCalories = calories,
                streakDays = streak,
                recentSessions = sessions,
                achievements = achievements,
                isLoading = false
            )
        }
    }

    fun requestGeminiAdvice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeminiLoading = true)

            val profile = userRepository.getUserProfileDirect()
            val goal = profile?.goal ?: "Ganar masa muscular"
            val level = profile?.level ?: "Principiante"
            val state = _uiState.value

            val result = geminiRepository.getCoachingAdvice(
                goal = goal,
                level = level,
                totalSessions = state.totalSessions,
                totalMinutes = state.totalMinutes,
                streakDays = state.streakDays
            )

            _uiState.value = _uiState.value.copy(
                isGeminiLoading = false,
                geminiAdvice = result.getOrNull()
            )
        }
    }

    class Factory(
        private val progressRepository: ProgressRepository,
        private val userRepository: UserRepository,
        private val geminiRepository: GeminiRepository,
        private val getStreakUseCase: GetStreakUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(
                progressRepository,
                userRepository,
                geminiRepository,
                getStreakUseCase
            ) as T
        }
    }
}
