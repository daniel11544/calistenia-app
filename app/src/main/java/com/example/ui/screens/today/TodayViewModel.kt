package com.example.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.TodayWorkoutState
import com.example.data.repository.WorkoutRepository
import com.example.domain.usecase.CompleteWorkoutSessionUseCase
import com.example.domain.usecase.GetTodayWorkoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodayViewModel(
    private val getTodayWorkoutUseCase: GetTodayWorkoutUseCase,
    private val workoutRepository: WorkoutRepository,
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayWorkoutState())
    val uiState: StateFlow<TodayWorkoutState> = _uiState.asStateFlow()

    init {
        loadTodayWorkout()
    }

    fun loadTodayWorkout() {
        viewModelScope.launch {
            getTodayWorkoutUseCase().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleExercise(dayExerciseId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            workoutRepository.setExerciseCompleted(dayExerciseId, isCompleted)
        }
    }

    fun finishWorkoutSession() {
        val current = _uiState.value
        val day = current.currentDay ?: return
        val plan = current.plan ?: return

        viewModelScope.launch {
            val totalEx = current.exercises.size
            val completedEx = current.exercises.count { it.isCompleted }
            completeWorkoutSessionUseCase(
                dayId = day.id,
                planId = plan.id,
                durationSeconds = day.durationMinutes * 60,
                totalExercises = totalEx,
                completedExercises = completedEx,
                caloriesBurned = day.estimatedCalories
            )
            loadTodayWorkout()
        }
    }

    class Factory(
        private val getTodayWorkoutUseCase: GetTodayWorkoutUseCase,
        private val workoutRepository: WorkoutRepository,
        private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(
                getTodayWorkoutUseCase,
                workoutRepository,
                completeWorkoutSessionUseCase
            ) as T
        }
    }
}
