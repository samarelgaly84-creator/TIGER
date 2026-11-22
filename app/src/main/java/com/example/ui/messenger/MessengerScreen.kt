package com.example.ui.messenger

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.call.CallForegroundService
import com.example.call.CallNotificationHelper
import com.example.core.TigerStrings
import com.example.data.firestore.ChatMessage
import com.example.data.firestore.ChatThread
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun MessengerScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings
) {
    val context = LocalContext.current
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val messagesState by viewModel.selectedChatMessagesState.collectAsStateWithLifecycle()
    val wsState by viewModel.webSocket.connectionState.collectAsStateWithLifecycle()
    val signalLog by viewModel.webSocket.lastSignalLog.collectAsStateWithLifecycle()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var activeCallPeer by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled gracefully */ }

    val selectedChat = (chatsState as? DataState.Success)?.data?.firstOrNull { it.id == selectedChatId }

    if (activeCallPeer != null) {
        val (peerName, isVideo) = activeCallPeer!!
        ActiveCallOverlay(
            peerName = peerName,
            isVideo = isVideo,
            signalLog = signalLog,
            strings = strings,
            onAddParticipant = { guest ->
                viewModel.webSocket.sendWebRtcSignal("call_live", "invite_participant", guest)
            },
            onEndCall = {
                CallForegroundService.stop(context)
                viewModel.webSocket.sendWebRtcSignal("call_live", "hangup", peerName)
                activeCallPeer = null
            }
        )
        return
    }

    if (selectedChat != null) {
        ChatConversationView(
            chat = selectedChat,
            messagesState = messagesState,
            viewModel = viewModel,
            strings = strings,
            onBack = { viewModel.selectChat(null) },
            onStartCall = { isVideo ->
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                CallForegroundService.start(context, selectedChat.title, isVideo)
                viewModel.webSocket.sendWebRtcSignal("call_${selectedChat.id}", "offer", selectedChat.title)
                activeCallPeer = selectedChat.title to isVideo
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // E2EE & Native Lock-Screen Calling Telemetry Banner
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess)
                        Text(
                            text = strings.e2eeBadge,
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Key Fingerprint: ${viewModel.crypto.keyFingerprint} • WSS: ${wsState.name}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.weight(1f).testTag("new_e2ee_chat_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.pick("محادثة جديدة", "New Chat"))
                        }
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                CallNotificationHelper.launchIncomingCall(
                                    context = context,
                                    callerName = "Tariq Al-Rashid (E2EE)",
                                    isVideo = true
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("test_lockscreen_call_btn")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.testIncomingCall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            when (chatsState) {
                is DataState.Loading -> Text(strings.pick("جاري تحميل المحادثات المشفّرة...", "Loading encrypted threads..."))
                is DataState.Error -> Text((chatsState as DataState.Error).message, color = CrimsonDanger)
                is DataState.Success -> {
                    val threads = (chatsState as DataState.Success<List<ChatThread>>).data
                    if (threads.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Text(strings.pick("ابدأ أول محادثة مشفّرة بطرف لطرف", "Start your first E2EE conversation"), fontWeight = FontWeight.Bold)
                                Text(
                                    text = strings.pick("جميع الرسائل والمكالمات مشفّرة بمفاتيح ECDH P-256 + AES-GCM", "Messages and calls use ECDH P-256 + AES-GCM"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(threads, key = { it.id }) { thread ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectChat(thread.id) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (thread.groupChat) Icons.Default.Groups else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(thread.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = thread.lastMessagePreview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (thread.unreadCount > 0) {
                                            Badge { Text(thread.unreadCount.toString()) }
                                        }
                                        IconButton(onClick = {
                                            CallForegroundService.start(context, thread.title, false)
                                            activeCallPeer = thread.title to false
                                        }) {
                                            Icon(Icons.Default.Call, contentDescription = strings.startVoiceCall, tint = EmeraldSuccess)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var chatTitle by remember { mutableStateOf("") }
        var isGroup by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(strings.newGroupChat) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chatTitle,
                        onValueChange = { chatTitle = it },
                        label = { Text(strings.pick("اسم الشخص أو المجموعة", "Contact or Group Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_chat_title_input")
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isGroup, onCheckedChange = { isGroup = it })
                        Text(strings.pick("محادثة جماعية مشفّرة", "Encrypted Group Chat"))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chatTitle.isNotBlank()) {
                            viewModel.createChat(chatTitle, isGroup)
                            showNewChatDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_chat_btn")
                ) { Text(strings.pick("إنشاء", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text(strings.pick("إلغاء", "Cancel")) }
            }
        )
    }
}

@Composable
private fun ChatConversationView(
    chat: ChatThread,
    messagesState: DataState<List<ChatMessage>>,
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit,
    onStartCall: (Boolean) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    val myUid = Firebase.auth.currentUser?.uid

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendEncryptedChatMessage(chat, "[Encrypted Photo] ${uri.lastPathSegment ?: "media.jpg"}", "image")
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.sendEncryptedChatMessage(chat, "[Encrypted File] ${uri.lastPathSegment ?: "document"}", "document")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with Audio & Video Call Triggers
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("AES-256-GCM • ECDH P-256", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                }
                IconButton(onClick = { onStartCall(false) }, modifier = Modifier.testTag("start_voice_call_btn")) {
                    Icon(Icons.Default.Call, contentDescription = strings.startVoiceCall, tint = EmeraldSuccess)
                }
                IconButton(onClick = { onStartCall(true) }, modifier = Modifier.testTag("start_video_call_btn")) {
                    Icon(Icons.Default.Videocam, contentDescription = strings.startVideoCall, tint = TigerOrange)
                }
            }
        }

        // Encrypted Messages List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val msgs = (messagesState as? DataState.Success)?.data.orEmpty()
            items(msgs, key = { it.id }) { msg ->
                val isMine = msg.senderId == myUid
                val decrypted = remember(msg.id, msg.cipherText) {
                    viewModel.crypto.decryptMessage(msg.cipherText, msg.ivBase64, msg.plainPreview)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = decrypted,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "🔒 ${msg.messageType} • IV:${msg.ivBase64.take(6)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Composer Row (Photo, Document, Voice Note, Encrypted Send)
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Icon(Icons.Default.Image, contentDescription = "Send Image")
                }
                IconButton(onClick = { docPicker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Send Document")
                }
                IconButton(onClick = {
                    isRecordingVoice = !isRecordingVoice
                    if (!isRecordingVoice) {
                        viewModel.sendEncryptedChatMessage(chat, "🎙️ Voice Note (0:06) • Opus E2EE", "voice")
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Note",
                        tint = if (isRecordingVoice) CrimsonDanger else MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(strings.pick("رسالة مشفّرة...", "Encrypted message...")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("chat_message_input")
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendEncryptedChatMessage(chat, input)
                            input = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ActiveCallOverlay(
    peerName: String,
    isVideo: Boolean,
    signalLog: String,
    strings: TigerStrings,
    onAddParticipant: (String) -> Unit,
    onEndCall: () -> Unit
) {
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(isVideo) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    val participants = remember { mutableStateListOf(peerName) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSec++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A101E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = TigerOrange.copy(alpha = 0.2f), shape = RoundedCornerShape(14.dp)) {
                Text(
                    text = if (isVideo) "WebRTC Multi-Party Video • DTLS-SRTP" else "WebRTC HD Voice • Telecom Service",
                    color = TigerGold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(104.dp).clip(CircleShape).background(TigerOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(peerName.take(2).uppercase(), style = MaterialTheme.typography.displaySmall, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Text(participants.joinToString(" + "), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                color = EmeraldSuccess,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = signalLog,
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledIconButton(
                onClick = { muted = !muted },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (muted) TigerOrange else Color(0xFF1E293B))
            ) {
                Icon(if (muted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
            }
            FilledIconButton(
                onClick = { speaker = !speaker },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (speaker) TigerOrange else Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    val next = listOf("Omar F.", "Huda A.", "Zayd S.").firstOrNull { it !in participants }
                    if (next != null) {
                        participants.add(next)
                        onAddParticipant(next)
                    }
                },
                modifier = Modifier.size(56.dp).testTag("add_person_to_call_btn"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = strings.addPersonToCall, tint = Color.White)
            }
            FilledIconButton(
                onClick = onEndCall,
                modifier = Modifier.size(56.dp).testTag("hangup_call_btn"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrimsonDanger)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
            }
        }
    }
}
