package com.example.ui.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.TigerStrings
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.AiStreamState
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TigerAiScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val messagesState by viewModel.aiMessagesState.collectAsStateWithLifecycle()
    val streamState by viewModel.aiStreamState.collectAsStateWithLifecycle()
    val showCelebration by viewModel.showCelebrationDialog.collectAsStateWithLifecycle()
    val celebrationSummary by viewModel.celebrationSummary.collectAsStateWithLifecycle()
    val pendingPrompt by viewModel.pendingAiPrompt.collectAsStateWithLifecycle()

    var promptText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var includeDriveAi by remember { mutableStateOf(false) }

    LaunchedEffect(pendingPrompt) {
        if (!pendingPrompt.isNullOrBlank()) {
            val shared = viewModel.consumePendingAiPrompt()
            if (!shared.isNullOrBlank()) {
                promptText = shared
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        val maxDim = 1024
                        val scale = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
                        attachedBitmap = if (scale < 1f) {
                            Bitmap.createScaledBitmap(
                                decoded,
                                (decoded.width * scale).toInt().coerceAtLeast(1),
                                (decoded.height * scale).toInt().coerceAtLeast(1),
                                true
                            )
                        } else decoded
                    }
                }
            }
        }
    }

    val profile = (profileState as? DataState.Success)?.data
    val used = profile?.monthlyAiUsed ?: 0L
    val limit = (profile?.monthlyAiLimit ?: 10_000L).coerceAtLeast(1L)
    val planTier = profile?.planTier ?: "FREE"
    val cycleNum = profile?.cycleNumber ?: 1L
    val quotaProgress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Quota & 30-Day Settlement Header
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_tiger_ai_1795365410971),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xE60B0F19), Color(0xB31E130A))
                            )
                        )
                )
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TigerGold)
                            Text(
                                text = strings.aiStudioTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(color = TigerOrange, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "$planTier • Cycle #$cycleNum",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.aiQuotaTitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${strings.formatNumber(used)} / ${strings.formatNumber(limit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TigerGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { quotaProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = TigerOrange,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = includeDriveAi,
                            onClick = { includeDriveAi = !includeDriveAi },
                            label = { Text(strings.aiDriveSummary, color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = TigerGold, modifier = Modifier.size(16.dp)) }
                        )
                        OutlinedButton(
                            onClick = { viewModel.settleMonthlyAiCycle() },
                            modifier = Modifier.testTag("settle_ai_cycle_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = TigerGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.aiSettleCycle, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Conversation Stream List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val msgs = (messagesState as? DataState.Success)?.data.orEmpty()
            if (msgs.isEmpty() && streamState is AiStreamState.Idle) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.pick("مرحباً بك في استوديو تايجر AI", "Welcome to TIGER AI Studio"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.pick(
                                    "اسأل أي شيء، ارفع صورة لتحليلها فورياً، أو فعّل ذكاء درايف السحابي لتلخيص ملفاتك.",
                                    "Ask anything with real-time Gemini streaming, attach photos for multimodal vision analysis, or toggle Cloud Drive AI."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(msgs, key = { it.id }) { msg ->
                val isUser = msg.senderRole == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(0.88f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!msg.attachmentLabel.isNullOrBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "📎 ${msg.attachmentLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else TigerGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = msg.text,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!isUser) {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    IconButton(
                                        onClick = { viewModel.shareTextToFirstOrNewChat("TIGER AI:\n${msg.text}", "ai_reply") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share AI Reply", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (val st = streamState) {
                is AiStreamState.Loading -> item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(strings.pick("تايجر AI يكتب الآن...", "TIGER AI is streaming..."))
                    }
                }
                is AiStreamState.Streaming -> item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Text(st.text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is AiStreamState.Error -> item {
                    Surface(
                        color = CrimsonDanger.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(st.message, color = CrimsonDanger, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                AiStreamState.Idle -> Unit
            }
        }

        // Attached Photo Preview Pill
        attachedBitmap?.let { bmp ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Attached Image",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = strings.pick("صورة مرفقة للتحليل المتعدد الوسائط", "Image ready for multimodal vision analysis"),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { attachedBitmap = null }) {
                    Text(strings.pick("إزالة", "Remove"))
                }
            }
        }

        // Input Composer
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("ai_attach_image_btn")
                ) {
                    Icon(Icons.Default.Image, contentDescription = strings.aiAnalyzeImage, tint = MaterialTheme.colorScheme.primary)
                }
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text(strings.pick("اسأل تايجر AI...", "Ask TIGER AI...")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("ai_prompt_input")
                )
                IconButton(
                    onClick = {
                        if (promptText.isNotBlank() && streamState !is AiStreamState.Loading && streamState !is AiStreamState.Streaming) {
                            viewModel.sendAiPrompt(promptText, attachedBitmap, includeDriveAi)
                            promptText = ""
                            attachedBitmap = null
                        }
                    },
                    modifier = Modifier.testTag("ai_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to AI", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showCelebration) {
        MonthlyCelebrationConfettiDialog(
            settledMessages = celebrationSummary.first,
            newCycle = celebrationSummary.second,
            strings = strings,
            onDismiss = { viewModel.markCelebrationSeen() }
        )
    }
}

@Composable
private fun MonthlyCelebrationConfettiDialog(
    settledMessages: Long,
    newCycle: Long,
    strings: TigerStrings,
    onDismiss: () -> Unit
) {
    val anim = rememberInfiniteTransition(label = "confetti")
    val phase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "confetti_phase"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = TigerGold)
                Text(strings.pick("احتفال دورة تايجر AI الشهرية!", "30-Day AI Milestone Celebration!"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val colors = listOf(TigerOrange, TigerGold, EmeraldSuccess, Color(0xFF3B82F6), Color(0xFFEC4899))
                        for (i in 0 until 28) {
                            val seed = (i * 37) % 100
                            val x = size.width * ((seed / 100f) + 0.08f * sin((phase * 6.28f + i).toDouble()).toFloat())
                            val y = ((phase * size.height * 1.4f + i * 29f) % (size.height + 20f)) - 10f
                            val w = 8f + (i % 5) * 2.5f
                            val h = 12f + (i % 4) * 3f
                            drawRect(
                                color = colors[i % colors.size],
                                topLeft = Offset(x.coerceIn(0f, size.width), y),
                                size = Size(w * (0.6f + 0.4f * cos((phase * 12f + i).toDouble()).toFloat()), h)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.formatNumber(settledMessages),
                            style = MaterialTheme.typography.displaySmall,
                            color = TigerGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.pick("رسالة ذكاء اصطناعي تمت تسويتها", "AI Messages Settled"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = strings.pick(
                        "تمت تصفية عداد الشهر بنجاح وبدأت الدورة رقم $newCycle بحصتك الكاملة المتجددة!",
                        "Your completed 30-day cycle has been settled and Cycle #$newCycle is live with your full refreshed quota!"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("celebration_seen_btn")
            ) {
                Text(strings.pick("رائع! لننطلق", "Awesome! Continue"))
            }
        }
    )
}
