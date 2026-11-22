package com.example.call

import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * Self-managed Android Telecom ConnectionService for native Tiger Connect VoIP Audio & Video calls.
 */
class TigerConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val callerName = request?.extras?.getString(CallNotificationHelper.EXTRA_CALLER_NAME)
            ?: "Tiger Caller"
        val isVideo = request?.extras?.getBoolean(CallNotificationHelper.EXTRA_IS_VIDEO, false) ?: false

        val connection = TigerVoipConnection(callerName).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectionProperties = Connection.PROPERTY_SELF_MANAGED
            }
            setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)
            setInitializing()
            setRinging()
        }

        CallNotificationHelper.launchIncomingCall(
            context = applicationContext,
            callerName = callerName,
            isVideo = isVideo
        )
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val target = request?.extras?.getString(CallNotificationHelper.EXTRA_CALLER_NAME) ?: "Tiger Contact"
        return TigerVoipConnection(target).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectionProperties = Connection.PROPERTY_SELF_MANAGED
            }
            setCallerDisplayName(target, TelecomManager.PRESENTATION_ALLOWED)
            setDialing()
            setActive()
        }
    }

    private class TigerVoipConnection(private val peerName: String) : Connection() {
        override fun onAnswer() {
            Log.d("TigerVoipConn", "Answered call with $peerName")
            setActive()
        }

        override fun onReject() {
            Log.d("TigerVoipConn", "Rejected call with $peerName")
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()
        }

        override fun onDisconnect() {
            Log.d("TigerVoipConn", "Disconnected call with $peerName")
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }
    }
}
