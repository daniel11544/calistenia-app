package com.example.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExerciseEntity
import com.example.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ExercisesFilterState(
    val searchQuery: String = "",
    val selectedMuscleGroup: String = "Todos",
    val selectedDifficulty: String = "Todos",
    val selectedExercise: ExerciseEntity? = null
)

class ExercisesViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ExercisesFilterState())
    val filterState: StateFlow<ExercisesFilterState> = _filterState

    val filteredExercises: StateFlow<List<ExerciseEntity>> = combine(
        exerciseRepository.allExercises,
        _filterState
    ) { allExercises, filters ->
        allExercises.filter { ex ->
            val matchesQuery = filters.searchQuery.isBlank() ||
                    ex.name.contains(filters.searchQuery, ignoreCase = true) ||
                    ex.muscleGroup.contains(filters.searchQuery, ignoreCase = true) ||
                    ex.equipment.contains(filters.searchQuery, ignoreCase = true)

            val matchesMuscle = filters.selectedMuscleGroup == "Todos" ||
                    ex.muscleGroup.equals(filters.selectedMuscleGroup, ignoreCase = true)

            val matchesDifficulty = filters.selectedDifficulty == "Todos" ||
                    ex.difficulty.equals(filters.selectedDifficulty, ignoreCase = true)

            matchesQuery && matchesMuscle && matchesDifficulty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun onSelectMuscleGroup(group: String) {
        _filterState.value = _filterState.value.copy(selectedMuscleGroup = group)
    }

    fun onSelectDifficulty(difficulty: String) {
        _filterState.value = _filterState.value.copy(selectedDifficulty = difficulty)
    }

    fun selectExercise(exercise: ExerciseEntity?) {
        _filterState.value = _filterState.value.copy(selectedExercise = exercise)
    }

    class Factory(private val exerciseRepository: ExerciseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExercisesViewModel(exerciseRepository) as T
        }
    }
}
