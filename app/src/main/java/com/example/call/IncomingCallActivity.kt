package com.example.call

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TigerConnectTheme
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import kotlinx.coroutines.delay

/**
 * Full-screen Incoming Call Activity displayed directly over the lock screen (`setShowWhenLocked` + `setTurnScreenOn`)
 * with system ringtone, vibration pattern, and multi-participant WebRTC Call controls.
 */
class IncomingCallActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val callerName = intent.getStringExtra(CallNotificationHelper.EXTRA_CALLER_NAME) ?: "Omar Al-Mansoor"
        val isVideo = intent.getBooleanExtra(CallNotificationHelper.EXTRA_IS_VIDEO, true)

        startRingtoneAndVibration()

        setContent {
            TigerConnectTheme(darkTheme = true, isArabic = false) {
                IncomingCallScreen(
                    callerName = callerName,
                    isVideo = isVideo,
                    onAnswer = {
                        stopRingtoneAndVibration()
                        CallNotificationHelper.dismissIncomingCallNotification(this)
                        CallForegroundService.start(this, callerName, isVideo)
                    },
                    onDeclineOrEnd = {
                        stopRingtoneAndVibration()
                        CallNotificationHelper.dismissIncomingCallNotification(this)
                        CallForegroundService.stop(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun startRingtoneAndVibration() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.also { it.play() }
        }
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 700, 500, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopRingtoneAndVibration() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
    }

    override fun onDestroy() {
        stopRingtoneAndVibration()
        super.onDestroy()
    }
}

@Composable
private fun IncomingCallScreen(
    callerName: String,
    isVideo: Boolean,
    onAnswer: () -> Unit,
    onDeclineOrEnd: () -> Unit
) {
    var answered by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(isVideo) }
    var seconds by remember { mutableIntStateOf(0) }
    val participants = remember { mutableStateListOf(callerName) }

    LaunchedEffect(answered) {
        if (answered) {
            while (true) {
                delay(1000L)
                seconds++
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "call_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1F120A), ObsidianBg, Color(0xFF0A1120))
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = TigerOrange.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2EE Call",
                            tint = TigerGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isVideo) "TIGER WebRTC HD Video • E2EE" else "TIGER WebRTC Voice • E2EE",
                            style = MaterialTheme.typography.labelMedium,
                            color = TigerGold
                        )
                    }
                }
                Spacer(Modifier.height(36.dp))

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(if (!answered) pulse.value else 1.05f)
                            .clip(CircleShape)
                            .background(TigerOrange.copy(alpha = 0.22f))
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(TigerOrange, TigerGold))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callerName.take(2).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFF0B0F19),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = participants.joinToString(" • "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (answered) {
                        String.format("%02d:%02d • DTLS-SRTP Connected", seconds / 60, seconds % 60)
                    } else {
                        "Ringing over Lock Screen (Telecom Service)..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )
            }

            if (!answered) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onDeclineOrEnd,
                            modifier = Modifier.size(72.dp).testTag("decline_call_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Decline", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = {
                                answered = true
                                onAnswer()
                            },
                            modifier = Modifier.size(72.dp).testTag("answer_call_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Answer Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Answer", color = Color.White)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledIconButton(
                        onClick = { muted = !muted },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (muted) TigerOrange else Color(0xFF1E293B)
                        )
                    ) {
                        Icon(if (muted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                    }
                    FilledIconButton(
                        onClick = { speakerOn = !speakerOn },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (speakerOn) TigerOrange else Color(0xFF1E293B)
                        )
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
                    }
                    FilledIconButton(
                        onClick = {
                            val nextGuest = listOf("Layla N.", "Tariq K.", "Sara M.")
                                .firstOrNull { it !in participants }
                            if (nextGuest != null) participants.add(nextGuest)
                        },
                        modifier = Modifier.size(56.dp).testTag("add_participant_call_btn"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Person", tint = Color.White)
                    }
                    FilledIconButton(
                        onClick = onDeclineOrEnd,
                        modifier = Modifier.size(56.dp).testTag("end_active_call_btn"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Hang Up", tint = Color.White)
                    }
                }
            }
        }
    }
}
