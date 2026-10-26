package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fit_calisthenics_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val DARK_MODE_OPTION = stringPreferencesKey("dark_mode_option") // "dark", "light", "system"
        private val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        private val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        private val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        private val HYDRATION_REMINDER_ENABLED = booleanPreferencesKey("hydration_reminder_enabled")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    val darkModeOption: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_OPTION] ?: "dark" // Default to dark aesthetic
    }

    suspend fun setDarkModeOption(option: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_OPTION] = option
        }
    }

    val dailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DAILY_REMINDER_ENABLED] ?: true
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REMINDER_ENABLED] = enabled
        }
    }

    val dailyReminderHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_REMINDER_HOUR] ?: 18 // Default 6 PM
    }

    val dailyReminderMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_REMINDER_MINUTE] ?: 0
    }

    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REMINDER_HOUR] = hour
            preferences[DAILY_REMINDER_MINUTE] = minute
        }
    }

    val hydrationReminderEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HYDRATION_REMINDER_ENABLED] ?: true
    }

    suspend fun setHydrationReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HYDRATION_REMINDER_ENABLED] = enabled
        }
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED] ?: true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED] ?: true
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
