package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.FitCalisthenicsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FitApp

        setContent {
            val darkModeOption by app.dataStoreManager.darkModeOption.collectAsStateWithLifecycle(initialValue = "dark")
            val isDark = when (darkModeOption) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            FitCalisthenicsTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}
