package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServiceTimerState(
    val secondsRemaining: Int = 0,
    val totalSeconds: Int = 0,
    val isPaused: Boolean = false,
    val isRest: Boolean = false,
    val exerciseName: String = "",
    val isFinished: Boolean = false
)

class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_TIMER"
        const val ACTION_PAUSE = "ACTION_PAUSE_TIMER"
        const val ACTION_RESUME = "ACTION_RESUME_TIMER"
        const val ACTION_SKIP = "ACTION_SKIP_TIMER"
        const val ACTION_STOP = "ACTION_STOP_TIMER"

        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val EXTRA_NAME = "EXTRA_NAME"
        const val EXTRA_IS_REST = "EXTRA_IS_REST"

        private val _timerState = MutableStateFlow(ServiceTimerState())
        val timerState: StateFlow<ServiceTimerState> = _timerState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 60)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Ejercicio"
                val isRest = intent.getBooleanExtra(EXTRA_IS_REST, false)
                startTimer(seconds, name, isRest)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_SKIP -> skipTimer()
            ACTION_STOP -> stopTimerService()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Int, name: String, isRest: Boolean) {
        timerJob?.cancel()
        _timerState.value = ServiceTimerState(
            secondsRemaining = totalSeconds,
            totalSeconds = totalSeconds,
            isPaused = false,
            isRest = isRest,
            exerciseName = name,
            isFinished = false
        )

        val notification = notificationHelper.buildTimerNotification(
            exerciseName = name,
            statusText = if (isRest) "Tiempo de descanso" else "Serie activa",
            timeFormatted = formatTime(totalSeconds),
            isPaused = false
        ).build()

        startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notification)

        timerJob = serviceScope.launch {
            while (_timerState.value.secondsRemaining > 0) {
                delay(1000)
                if (!_timerState.value.isPaused) {
                    val remaining = _timerState.value.secondsRemaining - 1
                    _timerState.value = _timerState.value.copy(secondsRemaining = remaining)
                    updateNotification()

                    if (remaining == 0) {
                        vibrateAlert()
                        _timerState.value = _timerState.value.copy(isFinished = true)
                        break
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        _timerState.value = _timerState.value.copy(isPaused = true)
        updateNotification()
    }

    private fun resumeTimer() {
        _timerState.value = _timerState.value.copy(isPaused = false)
        updateNotification()
    }

    private fun skipTimer() {
        timerJob?.cancel()
        vibrateAlert()
        _timerState.value = _timerState.value.copy(secondsRemaining = 0, isFinished = true)
        updateNotification()
    }

    private fun stopTimerService() {
        timerJob?.cancel()
        _timerState.value = ServiceTimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val current = _timerState.value
        val notification = notificationHelper.buildTimerNotification(
            exerciseName = current.exerciseName,
            statusText = if (current.isRest) "Descanso" else "En ejecución",
            timeFormatted = formatTime(current.secondsRemaining),
            isPaused = current.isPaused
        ).build()
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
    }

    private fun vibrateAlert() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 200, 400), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
