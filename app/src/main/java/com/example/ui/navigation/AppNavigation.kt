package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.FitApp
import com.example.R
import com.example.ui.screens.exercises.ExercisesScreen
import com.example.ui.screens.exercises.ExercisesViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.progress.ProgressViewModel
import com.example.ui.screens.today.TodayScreen
import com.example.ui.screens.today.TodayViewModel
import com.example.ui.screens.timer.WorkoutTimerScreen
import com.example.ui.screens.timer.WorkoutTimerViewModel

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object WorkoutTimer : Screen("workout_timer/{dayId}") {
        fun createRoute(dayId: String) = "workout_timer/$dayId"
    }
}

enum class MainTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    TODAY(R.string.nav_today, Icons.Default.FitnessCenter, Icons.Outlined.FitnessCenter, "tab_today"),
    EXERCISES(R.string.nav_exercises, Icons.Default.List, Icons.Outlined.List, "tab_exercises"),
    PROGRESS(R.string.nav_progress, Icons.Default.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "tab_progress"),
    PROFILE(R.string.nav_profile, Icons.Default.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as FitApp
    val navController = rememberNavController()

    val isOnboarded by app.dataStoreManager.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = null)

    // Request POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied handled gracefully */ }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (isOnboarded == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (isOnboarded == true) Screen.Main.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(
                    app.userRepository,
                    app.exerciseRepository,
                    app.planRepository,
                    app.planGeneratorUseCase,
                    app.dataStoreManager
                )
            )

            OnboardingScreen(
                viewModel = onboardingViewModel,
                onOnboardingFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen(
                app = app,
                onStartWorkout = { dayId ->
                    navController.navigate(Screen.WorkoutTimer.createRoute(dayId))
                },
                onResetToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.WorkoutTimer.route,
            arguments = listOf(navArgument("dayId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getString("dayId") ?: ""
            val timerViewModel: WorkoutTimerViewModel = viewModel(
                factory = WorkoutTimerViewModel.Factory(
                    dayId,
                    app.workoutRepository,
                    app.completeWorkoutSessionUseCase,
                    context
                )
            )

            WorkoutTimerScreen(
                viewModel = timerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    app: FitApp,
    onStartWorkout: (dayId: String) -> Unit,
    onResetToOnboarding: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.TODAY) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main_tab_anim"
            ) { tab ->
                when (tab) {
                    MainTab.TODAY -> {
                        val todayViewModel: TodayViewModel = viewModel(
                            factory = TodayViewModel.Factory(
                                app.getTodayWorkoutUseCase,
                                app.workoutRepository,
                                app.completeWorkoutSessionUseCase
                            )
                        )
                        TodayScreen(
                            viewModel = todayViewModel,
                            onStartWorkout = onStartWorkout,
                            onExerciseClick = { /* Could open detail dialog if desired */ }
                        )
                    }
                    MainTab.EXERCISES -> {
                        val exercisesViewModel: ExercisesViewModel = viewModel(
                            factory = ExercisesViewModel.Factory(app.exerciseRepository)
                        )
                        ExercisesScreen(viewModel = exercisesViewModel)
                    }
                    MainTab.PROGRESS -> {
                        val progressViewModel: ProgressViewModel = viewModel(
                            factory = ProgressViewModel.Factory(
                                app.progressRepository,
                                app.userRepository,
                                app.geminiRepository,
                                app.getStreakUseCase
                            )
                        )
                        ProgressScreen(viewModel = progressViewModel)
                    }
                    MainTab.PROFILE -> {
                        val profileViewModel: ProfileViewModel = viewModel(
                            factory = ProfileViewModel.Factory(
                                app.userRepository,
                                app.planRepository,
                                app.exerciseRepository,
                                app.progressRepository,
                                app.planGeneratorUseCase,
                                app.scheduleRemindersUseCase,
                                app.dataStoreManager
                            )
                        )
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onResetToOnboarding = onResetToOnboarding
                        )
                    }
                }
            }
        }
    }
}
