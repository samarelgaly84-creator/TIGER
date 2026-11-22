package com.example.call

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * High-priority Firebase Cloud Messaging service handling incoming WebRTC call pushes
 * even when the app is terminated or the phone screen is locked.
 */
class TigerMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TigerFCM", "FCM device token refreshed (${token.take(10)}...)")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        when (data["type"]) {
            "incoming_call" -> {
                val caller = data["callerName"] ?: "Tiger Caller"
                val isVideo = data["isVideo"] == "true"
                val callId = data["callId"] ?: "fcm_${System.currentTimeMillis()}"
                CallNotificationHelper.launchIncomingCall(
                    context = applicationContext,
                    callerName = caller,
                    isVideo = isVideo,
                    callId = callId
                )
            }
            else -> {
                Log.d("TigerFCM", "Received push message: ${data["type"] ?: "notification"}")
            }
        }
    }
}
