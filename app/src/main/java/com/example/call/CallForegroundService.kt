package com.example.call

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service keeping active WebRTC voice/video calls alive while app is backgrounded or screen locked.
 */
class CallForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val peerName = intent?.getStringExtra(CallNotificationHelper.EXTRA_CALLER_NAME) ?: "Tiger Call"
        val isVideo = intent?.getBooleanExtra(CallNotificationHelper.EXTRA_IS_VIDEO, false) ?: false

        CallNotificationHelper.createNotificationChannels(this)

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangupIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CallForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CallNotificationHelper.CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(peerName)
            .setContentText(if (isVideo) "Active Encrypted Video Call • E2EE" else "Active Encrypted Voice Call • E2EE")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Call", hangupIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    CallNotificationHelper.NOTIF_ID_ACTIVE_CALL,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(CallNotificationHelper.NOTIF_ID_ACTIVE_CALL, notification)
            }
        } catch (_: Exception) {
            // Gracefully handle missing foreground permission on restricted emulators
            stopSelf()
        }

        return START_STICKY
    }

    companion object {
        const val ACTION_STOP = "com.example.call.ACTION_STOP_CALL"

        fun start(context: Context, peerName: String, isVideo: Boolean) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(CallNotificationHelper.EXTRA_CALLER_NAME, peerName)
                putExtra(CallNotificationHelper.EXTRA_IS_VIDEO, isVideo)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
