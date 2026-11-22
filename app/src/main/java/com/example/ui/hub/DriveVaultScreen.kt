package com.example.ui.hub

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.BiometricLockHelper
import com.example.core.TigerStrings
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DRIVE_FOLDERS = listOf("All", "Documents", "Media", "Projects", "Private Vault")

@Composable
fun DriveVaultScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val itemsState by viewModel.vaultItemsState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedFolder by remember { mutableStateOf("All") }
    var vaultUnlocked by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }

    val profile = (profileState as? DataState.Success)?.data
    val items = (itemsState as? DataState.Success)?.data.orEmpty()
    val planTier = profile?.planTier ?: "FREE"
    val quotaBytes = when (planTier) {
        "PRO" -> 50L * 1024L * 1024L * 1024L
        "PLUS" -> 5L * 1024L * 1024L * 1024L
        else -> 100L * 1024L * 1024L
    }
    val usedBytes = items.sumOf { it.sizeBytes }
    val usedMb = usedBytes / (1024.0 * 1024.0)
    val quotaLabel = when (planTier) {
        "PRO" -> "50 GB (PRO)"
        "PLUS" -> "5 GB (PLUS)"
        else -> "100 MB (FREE)"
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "tiger_doc.pdf"
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val size = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull()?.takeIf { it > 0 } ?: 184_320L
            val targetFolder = if (selectedFolder == "All") "Documents" else selectedFolder
            val lock = targetFolder == "Private Vault"

            scope.launch {
                isUploading = true
                for (step in 1..5) {
                    uploadProgress = step / 5f
                    delay(120L)
                }
                viewModel.addVaultFile(fileName, targetFolder, mime, size, lock)
                isUploading = false
                uploadProgress = 0f
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubDriveVault, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Storage Quota Meter Card
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.pick("مساحة تايجر درايف السحابية", "TIGER Cloud Storage Quota"), fontWeight = FontWeight.Bold)
                    Text("${String.format("%.2f", usedMb)} MB / $quotaLabel", color = TigerGold, style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(
                    progress = { (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0.02f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = TigerOrange
                )
                if (isUploading) {
                    LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth(), color = EmeraldSuccess)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f).testTag("drive_upload_file_btn")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pick("رفع ملف", "Upload File"))
                    }
                    OutlinedButton(
                        onClick = {
                            if (vaultUnlocked) {
                                vaultUnlocked = false
                            } else {
                                val activity = context as? FragmentActivity
                                if (activity != null && BiometricLockHelper.canAuthenticate(activity)) {
                                    BiometricLockHelper.authenticate(
                                        activity = activity,
                                        title = strings.pick("فتح الخزينة الخاصة", "Unlock Private Vault"),
                                        subtitle = strings.pick("تحقق بالبصمة أو رمز الجهاز", "Authenticate with Biometrics or Device PIN"),
                                        onSuccess = { vaultUnlocked = true },
                                        onFallbackPin = { showPinDialog = true }
                                    )
                                } else {
                                    showPinDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("toggle_vault_lock_btn")
                    ) {
                        Icon(if (vaultUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (vaultUnlocked) strings.pick("قفل الخزينة", "Lock Vault") else strings.pick("فتح الخزينة", "Unlock Vault"))
                    }
                }
            }
        }

        // Folder Filter Rail
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DRIVE_FOLDERS) { folder ->
                FilterChip(
                    selected = selectedFolder == folder,
                    onClick = { selectedFolder = folder },
                    leadingIcon = {
                        Icon(
                            if (folder == "Private Vault") Icons.Default.Lock else Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(folder) }
                )
            }
        }

        // Files List
        val filtered = items.filter { item ->
            val folderMatch = selectedFolder == "All" || item.folder == selectedFolder
            val lockVisible = !item.vaultLocked || vaultUnlocked
            folderMatch && lockVisible
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (selectedFolder == "Private Vault" && !vaultUnlocked) {
                item {
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TigerOrange, modifier = Modifier.size(38.dp))
                            Text(strings.pick("الخزينة الخاصة مقفلة بالبصمة / PIN", "Private Vault Locked (PIN default: 1234)"), fontWeight = FontWeight.Bold)
                            Button(onClick = { showPinDialog = true }) {
                                Text(strings.pick("إدخال رمز الخزينة", "Enter Vault PIN"))
                            }
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(strings.pick("لا توجد ملفات في هذا المجلد بعد. اضغط «رفع ملف».", "No files in this folder yet. Tap Upload File."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(filtered, key = { it.id }) { file ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                if (file.vaultLocked) Icons.Default.Lock else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (file.vaultLocked) TigerGold else MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, fontWeight = FontWeight.Bold)
                                Text("${file.folder} • ${file.sizeBytes / 1024} KB • ${file.mimeType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteVaultFile(file.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = CrimsonDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinErr by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.pick("رمز الخزينة الخاصة (PIN)", "Private Vault PIN")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.pick("أدخل الرمز (الافتراضي 1234 إن لم تقم بتعيينه في الإعدادات)", "Enter your Vault PIN (default 1234 if unset)"))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vault_pin_input")
                    )
                    if (pinErr != null) Text(pinErr!!, color = CrimsonDanger, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.settingsStore.verifyPin(pinInput, settings.vaultPinHash)) {
                        vaultUnlocked = true
                        showPinDialog = false
                    } else {
                        pinErr = strings.pick("الرمز غير صحيح", "Incorrect PIN")
                    }
                }) { Text(strings.pick("فتح", "Unlock")) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text(strings.pick("إلغاء", "Cancel")) }
            }
        )
    }
}
