package com.example.ui.screens.timer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.model.WorkoutExerciseItem
import com.example.data.repository.WorkoutRepository
import com.example.domain.usecase.CompleteWorkoutSessionUseCase
import com.example.service.ServiceTimerState
import com.example.service.TimerForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    WORK, // User is performing the set
    REST, // Rest countdown between sets
    FINISHED // All exercises and sets finished!
}

data class WorkoutTimerUiState(
    val day: WorkoutDayEntity? = null,
    val exercises: List<WorkoutExerciseItem> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSet: Int = 1,
    val currentPhase: WorkoutPhase = WorkoutPhase.WORK,
    val secondsRemaining: Int = 0,
    val totalSeconds: Int = 60,
    val isPaused: Boolean = false,
    val elapsedWorkoutSeconds: Int = 0,
    val isLoading: Boolean = true
)

class WorkoutTimerViewModel(
    private val dayId: String,
    private val workoutRepository: WorkoutRepository,
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutTimerUiState())
    val uiState: StateFlow<WorkoutTimerUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutData()
        observeForegroundServiceTimer()
    }

    private fun loadWorkoutData() {
        viewModelScope.launch {
            val day = workoutRepository.getDayByIdDirect(dayId)
            workoutRepository.getWorkoutExerciseItems(dayId).collect { items ->
                val firstIncompleteIndex = items.indexOfFirst { !it.isCompleted }.coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(
                    day = day,
                    exercises = items,
                    currentExerciseIndex = firstIncompleteIndex,
                    currentSet = 1,
                    isLoading = false
                )
            }
        }
    }

    private fun observeForegroundServiceTimer() {
        viewModelScope.launch {
            TimerForegroundService.timerState.collect { sState ->
                if (_uiState.value.currentPhase == WorkoutPhase.REST) {
                    _uiState.value = _uiState.value.copy(
                        secondsRemaining = sState.secondsRemaining,
                        totalSeconds = sState.totalSeconds,
                        isPaused = sState.isPaused
                    )

                    if (sState.isFinished) {
                        onRestCompleted()
                    }
                }
            }
        }
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return

        if (state.currentSet < currentExercise.sets) {
            // Move to rest phase
            val restSeconds = currentExercise.restSeconds.coerceAtLeast(30)
            _uiState.value = state.copy(
                currentPhase = WorkoutPhase.REST,
                secondsRemaining = restSeconds,
                totalSeconds = restSeconds,
                isPaused = false
            )
            startServiceTimer(restSeconds, currentExercise.exercise.name, isRest = true)
        } else {
            // Exercise finished! Mark completed in DB
            viewModelScope.launch {
                workoutRepository.setExerciseCompleted(currentExercise.id, true)
            }

            if (state.currentExerciseIndex < state.exercises.size - 1) {
                // Next exercise with rest transition
                val restSeconds = (currentExercise.restSeconds + 15).coerceAtLeast(45)
                _uiState.value = state.copy(
                    currentExerciseIndex = state.currentExerciseIndex + 1,
                    currentSet = 1,
                    currentPhase = WorkoutPhase.REST,
                    secondsRemaining = restSeconds,
                    totalSeconds = restSeconds,
                    isPaused = false
                )
                startServiceTimer(restSeconds, "Siguiente: ${state.exercises[state.currentExerciseIndex + 1].exercise.name}", isRest = true)
            } else {
                // Whole workout finished!
                finishWorkout()
            }
        }
    }

    fun skipRest() {
        stopServiceTimer()
        onRestCompleted()
    }

    private fun onRestCompleted() {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return

        val nextSet = if (state.currentSet < currentExercise.sets) state.currentSet + 1 else 1

        _uiState.value = state.copy(
            currentSet = nextSet,
            currentPhase = WorkoutPhase.WORK,
            secondsRemaining = 0,
            isPaused = false
        )
    }

    fun togglePauseResume() {
        val state = _uiState.value
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = if (state.isPaused) TimerForegroundService.ACTION_RESUME else TimerForegroundService.ACTION_PAUSE
        }
        context.startService(intent)
        _uiState.value = state.copy(isPaused = !state.isPaused)
    }

    fun finishWorkout() {
        stopServiceTimer()
        val state = _uiState.value
        val day = state.day ?: return

        _uiState.value = state.copy(currentPhase = WorkoutPhase.FINISHED)

        viewModelScope.launch {
            completeWorkoutSessionUseCase(
                dayId = day.id,
                planId = "",
                durationSeconds = day.durationMinutes * 60,
                totalExercises = state.exercises.size,
                completedExercises = state.exercises.size,
                caloriesBurned = day.estimatedCalories
            )
        }
    }

    private fun startServiceTimer(seconds: Int, name: String, isRest: Boolean) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_SECONDS, seconds)
            putExtra(TimerForegroundService.EXTRA_NAME, name)
            putExtra(TimerForegroundService.EXTRA_IS_REST, isRest)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopServiceTimer() {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun onCleared() {
        stopServiceTimer()
        super.onCleared()
    }

    class Factory(
        private val dayId: String,
        private val workoutRepository: WorkoutRepository,
        private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutTimerViewModel(
                dayId,
                workoutRepository,
                completeWorkoutSessionUseCase,
                context
            ) as T
        }
    }
}
