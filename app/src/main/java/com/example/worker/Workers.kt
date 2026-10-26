package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.service.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dataStoreManager = DataStoreManager(context)
        val enabled = dataStoreManager.dailyReminderEnabled.firstOrNull() ?: true
        if (!enabled) return Result.success()

        // Check if user already completed a workout session today
        val db = AppDatabase.getDatabase(context)
        val sessions = db.progressDao().getAllSessions().firstOrNull() ?: emptyList()

        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        val completedToday = sessions.any {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay
        }

        if (!completedToday) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showWorkoutReminderNotification()
        }

        return Result.success()
    }
}

class HydrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dataStoreManager = DataStoreManager(context)
        val enabled = dataStoreManager.hydrationReminderEnabled.firstOrNull() ?: true
        if (!enabled) return Result.success()

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showHydrationNotification()

        return Result.success()
    }
}
