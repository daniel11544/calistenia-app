package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.DataStoreManager
import com.example.domain.usecase.ScheduleRemindersUseCase
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dataStore = DataStoreManager(context)
                    val reminderEnabled = dataStore.dailyReminderEnabled.firstOrNull() ?: true
                    val reminderHour = dataStore.dailyReminderHour.firstOrNull() ?: 18
                    val reminderMinute = dataStore.dailyReminderMinute.firstOrNull() ?: 0
                    val hydrationEnabled = dataStore.hydrationReminderEnabled.firstOrNull() ?: true

                    val scheduleUseCase = ScheduleRemindersUseCase(context)
                    scheduleUseCase.scheduleDailyWorkoutReminder(reminderHour, reminderMinute, reminderEnabled)
                    scheduleUseCase.scheduleHydrationReminder(hydrationEnabled)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val helper = NotificationHelper(context)
        helper.showWorkoutReminderNotification()
    }
}
