package com.example.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

object CallNotificationHelper {
    const val CHANNEL_CALLS = "tiger_calls_high_priority"
    const val CHANNEL_MESSAGES = "tiger_messages_e2ee"
    const val CHANNEL_ALERTS = "tiger_system_alerts"

    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_IS_VIDEO = "extra_is_video"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val NOTIF_ID_INCOMING_CALL = 9001
    const val NOTIF_ID_ACTIVE_CALL = 9002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val callAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val callsChannel = NotificationChannel(
            CHANNEL_CALLS,
            "Tiger Connect Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen voice & video calls with ringtone"
            setSound(ringtoneUri, callAudio)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        val msgTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val msgAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val messagesChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Encrypted Messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "E2EE 1-on-1 and Group Chat notifications"
            setSound(msgTone, msgAudio)
            enableVibration(true)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Store & AI Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Store receipts, refunds, and monthly AI cycle milestones"
        }

        nm.createNotificationChannel(callsChannel)
        nm.createNotificationChannel(messagesChannel)
        nm.createNotificationChannel(alertsChannel)
    }

    fun registerTelecomAccount(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val handle = PhoneAccountHandle(
                ComponentName(context, TigerConnectionService::class.java),
                "tiger_connect_voip"
            )
            val account = PhoneAccount.builder(handle, "Tiger Connect VoIP")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .build()
            telecomManager.registerPhoneAccount(account)
        } catch (e: Exception) {
            Log.w("Telecom", "Self-managed PhoneAccount registration skipped: ${e.message}")
        }
    }

    fun launchIncomingCall(
        context: Context,
        callerName: String,
        isVideo: Boolean,
        callId: String = "call_${System.currentTimeMillis()}"
    ) {
        createNotificationChannels(context)
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_IS_VIDEO, isVideo)
            putExtra(EXTRA_CALL_ID, callId)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_INCOMING_CALL,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText(if (isVideo) "Incoming Encrypted Video Call" else "Incoming Encrypted Voice Call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        try {
            nm.notify(NOTIF_ID_INCOMING_CALL, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not yet granted on API 33+; launch activity directly
        }
        context.startActivity(fullScreenIntent)
    }

    fun dismissIncomingCallNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_INCOMING_CALL)
    }
}
