package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_WORKOUT = "channel_workout_reminders"
        const val CHANNEL_TIMER = "channel_timer_foreground"
        const val CHANNEL_HYDRATION = "channel_hydration_reminders"

        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_WORKOUT = 1002
        const val NOTIFICATION_ID_HYDRATION = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT,
                context.getString(R.string.notif_channel_workout_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_workout_desc)
                enableVibration(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.notif_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_timer_desc)
                setShowBadge(false)
            }

            val hydrationChannel = NotificationChannel(
                CHANNEL_HYDRATION,
                context.getString(R.string.notif_channel_hydration_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_hydration_desc)
            }

            manager.createNotificationChannel(workoutChannel)
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(hydrationChannel)
        }
    }

    fun showWorkoutReminderNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_workout_title))
            .setContentText(context.getString(R.string.notif_workout_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_WORKOUT, notification)
    }

    fun showHydrationNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HYDRATION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_hydration_title))
            .setContentText(context.getString(R.string.notif_hydration_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_HYDRATION, notification)
    }

    fun buildTimerNotification(
        exerciseName: String,
        statusText: String,
        timeFormatted: String,
        isPaused: Boolean
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = if (isPaused) TimerForegroundService.ACTION_RESUME else TimerForegroundService.ACTION_PAUSE
        }
        val pauseResumePending = PendingIntent.getService(
            context,
            3,
            pauseResumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val skipIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_SKIP
        }
        val skipPending = PendingIntent.getService(
            context,
            4,
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$exerciseName • $timeFormatted")
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                if (isPaused) context.getString(R.string.timer_resume) else context.getString(R.string.timer_pause),
                pauseResumePending
            )
            .addAction(
                0,
                context.getString(R.string.timer_skip_rest),
                skipPending
            )
    }
}
