package com.example.ui.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.LayoutPreviewMode
import com.example.core.TigerStrings
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel

private data class PlanSpec(
    val tier: String,
    val priceLabel: String,
    val aiQuota: String,
    val driveQuota: String,
    val highlightColor: Color
)

@Composable
fun PlansScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activeTier = (profileState as? DataState.Success)?.data?.planTier ?: "FREE"

    val plans = listOf(
        PlanSpec("FREE", strings.pick("مجاني", "$0 / mo"), "10,000 AI msgs / mo", "100 MB TIGER Drive", Color(0xFF64748B)),
        PlanSpec("PLUS", strings.pick("29 ر.س / شهرياً", "$9.99 / mo"), "100,000 AI msgs / mo", "5 GB TIGER Drive", TigerOrange),
        PlanSpec("PRO", strings.pick("99 ر.س / شهرياً", "$29.99 / mo"), "10,000,000 AI msgs / mo", "50 GB TIGER Drive + Priority WebRTC", TigerGold)
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubPlans, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        plans.forEach { plan ->
            val isCurrent = activeTier == plan.tier
            Card(
                modifier = Modifier.fillMaxWidth().border(if (isCurrent) 2.dp else 0.dp, plan.highlightColor, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = plan.highlightColor)
                            Text(plan.tier, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = plan.highlightColor)
                        }
                        Text(plan.priceLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("• ${plan.aiQuota}", style = MaterialTheme.typography.bodyMedium)
                    Text("• ${plan.driveQuota}", style = MaterialTheme.typography.bodyMedium)
                    Text("• ${strings.pick("تشفير طرف لطرف ومكالمات HD", "E2EE Messenger + Lock-Screen VoIP Calls")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.upgradePlan(plan.tier) },
                        enabled = !isCurrent,
                        modifier = Modifier.fillMaxWidth().testTag("select_plan_${plan.tier}"),
                        colors = ButtonDefaults.buttonColors(containerColor = plan.highlightColor)
                    ) {
                        Text(
                            text = if (isCurrent) strings.pick("باقتك الحالية", "Current Active Plan")
                            else strings.pick("الترقية إلى ${plan.tier}", "Activate ${plan.tier}"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val reports = (reportsState as? DataState.Success)?.data.orEmpty()
    val isAdmin = viewModel.isAdminUser

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubAdmin, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = if (isAdmin) EmeraldSuccess else TigerOrange)
                    Text(
                        text = if (isAdmin) strings.pick("صلاحية المشرف مفعّلة (Admin)", "Verified Admin Access Granted")
                        else strings.pick("عرض محدود — للمشرفين فقط (role='admin')", "Restricted View — Admin Role Required"),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = strings.pick("إدارة البلاغات، خطط المستخدمين، وسجلات التدقيق الأمني.", "Manage moderation reports, subscription tiers, and security audit logs."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(strings.pick("بلاغات الإشراف (${reports.size})", "Moderation Queue (${reports.size})"), fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (reports.isEmpty()) {
                item {
                    Text(strings.pick("لا توجد بلاغات مفتوحة حالياً.", "No moderation reports in queue."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(reports, key = { it.id }) { report ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${report.targetType.uppercase()} #${report.targetId.take(8)}", fontWeight = FontWeight.Bold)
                                Text(report.status, color = if (report.status == "resolved") EmeraldSuccess else TigerOrange, fontWeight = FontWeight.Bold)
                            }
                            Text(report.reason, style = MaterialTheme.typography.bodySmall)
                            if (isAdmin && report.status == "open") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.resolveReport(report.id, "resolved") }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Resolve")
                                    }
                                    OutlinedButton(onClick = { viewModel.resolveReport(report.id, "dismissed") }, modifier = Modifier.weight(1f)) {
                                        Text("Dismiss")
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

@Composable
fun ThemeAndSecuritySettingsScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler(onBack = onBack)

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var newPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubSettings, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Language & Dark Mode
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.pick("اللغة العربية (RTL)", "Arabic Interface (RTL)"), fontWeight = FontWeight.Bold)
                    Switch(checked = settings.isArabic, onCheckedChange = { viewModel.toggleLanguage() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.pick("الوضع الداكن الفاخر", "Obsidian Dark Theme"), fontWeight = FontWeight.Bold)
                    Switch(checked = settings.darkTheme, onCheckedChange = { viewModel.toggleTheme() })
                }
                Text(strings.pick("اللون المميز (Accent Color)", "Accent Color"), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPreset.entries.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preset.primary)
                                .border(if (settings.accentId == preset.id) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { viewModel.setAccent(preset.id) }
                        )
                    }
                }
            }
        }

        // Global Device-Preview / Adaptive Layout Switcher
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.pick("تبديل تخطيط الجهاز (هاتف / تابلت)", "Global Device-Preview / Layout Switcher"), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayoutPreviewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.layoutPreviewMode == mode,
                            onClick = { viewModel.setLayoutMode(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
            }
        }

        // Biometric & Vault PIN Security
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess)
                    Text(strings.pick("قفل البصمة ورمز الخزينة", "Biometric & Vault PIN Security"), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.pick("تفعيل قفل البصمة", "Enable Biometric Lock"))
                    Switch(checked = settings.biometricLockEnabled, onCheckedChange = { viewModel.setBiometricLock(it) })
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text(strings.pick("تعيين رمز PIN جديد للخزينة", "Set Custom Vault PIN (4-6 digits)")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newPin.length >= 4) {
                            viewModel.saveVaultPin(newPin)
                            newPin = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.pick("حفظ رمز الخزينة", "Save Vault PIN"))
                }
            }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_sign_out_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.signOut, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
