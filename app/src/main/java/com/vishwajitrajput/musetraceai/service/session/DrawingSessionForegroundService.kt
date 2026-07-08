package com.vishwajitrajput.musetraceai.service.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vishwajitrajput.musetraceai.MainActivity
import com.vishwajitrajput.musetraceai.R
import com.vishwajitrajput.musetraceai.service.accessibility.AccessibilityBridge

class DrawingSessionForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        DrawingSessionStore.initialize(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                AccessibilityBridge.pauseDrawing()
                DrawingSessionStore.pause()
            }
            ACTION_STOP -> {
                AccessibilityBridge.cancelDrawing()
                DrawingSessionStore.cancelSession()
                stopSelf()
            }
        }
        startForeground(NOTIFICATION_ID, notification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(DrawingSessionStore.state.value.status)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                10,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .addAction(0, "Pause", actionIntent(ACTION_PAUSE, 12))
        .addAction(0, "Stop", actionIntent(ACTION_STOP, 13))
        .build()

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, DrawingSessionForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MuseTrace drawing session",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "musetrace_session"
        private const val NOTIFICATION_ID = 2101
        const val ACTION_PAUSE = "com.vishwajitrajput.musetraceai.session.PAUSE"
        const val ACTION_STOP = "com.vishwajitrajput.musetraceai.session.STOP"

        fun startIntent(context: Context): Intent =
            Intent(context, DrawingSessionForegroundService::class.java)
    }
}
