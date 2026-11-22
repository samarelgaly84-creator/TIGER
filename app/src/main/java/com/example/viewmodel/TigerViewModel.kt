package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppSettingsState
import com.example.core.LayoutPreviewMode
import com.example.data.firestore.AiMessageRecord
import com.example.data.firestore.ChatMessage
import com.example.data.firestore.ChatThread
import com.example.data.firestore.GameScoreRecord
import com.example.data.firestore.ModerationReport
import com.example.data.firestore.SocialPost
import com.example.data.firestore.StoreOrder
import com.example.data.firestore.StoreProduct
import com.example.data.firestore.UserProfile
import com.example.data.firestore.UserStore
import com.example.data.firestore.VaultItem
import com.example.data.local.CachedPostEntity
import com.example.data.remote.CelebrationSeenRequest
import com.example.data.remote.GameScorePayload
import com.example.data.remote.StoreUpsertRequest
import com.example.di.AppContainer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val TAG = "TigerViewModel"

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

data class StoryItem(
    val id: String,
    val author: String,
    val caption: String,
    val gradientIndex: Int,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class LiveComment(
    val id: String,
    val author: String,
    val text: String
)

class TigerViewModel(
    private val container: AppContainer,
    private val currentUserId: String
) : ViewModel() {

    private val repo = container.firestoreRepo
    private val aiRepo = container.aiRepository
    val crypto = container.cryptoManager
    val webSocket = container.webSocketClient
    val settingsStore = container.settingsStore

    val settings: StateFlow<AppSettingsState> = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppSettingsState()
    )

    private fun <T> Flow<T>.toSafeStateFlow(initial: DataState<T> = DataState.Loading): StateFlow<DataState<T>> =
        this.map<T, DataState<T>> { DataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(DataState.Error(rootError.message ?: "Failed to sync cloud data"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = initial
            )

    val profileState: StateFlow<DataState<UserProfile?>> = repo.observeMyProfile().toSafeStateFlow()

    val postsState: StateFlow<DataState<List<SocialPost>>> = repo.observePublicPosts()
        .onEach { posts ->
            viewModelScope.launch {
                runCatching {
                    container.roomDatabase.cacheDao().upsertPosts(
                        posts.take(30).map {
                            CachedPostEntity(
                                id = it.id,
                                authorName = it.authorName,
                                category = it.category,
                                content = it.content,
                                mediaType = it.mediaType,
                                likesCount = it.likesCount.toInt()
                            )
                        }
                    )
                }
            }
        }
        .toSafeStateFlow()

    val storesState: StateFlow<DataState<List<UserStore>>> = repo.observePublicStores().toSafeStateFlow()
    val productsState: StateFlow<DataState<List<StoreProduct>>> = repo.observePublicProducts().toSafeStateFlow()
    val buyerOrdersState: StateFlow<DataState<List<StoreOrder>>> = repo.observeMyBuyerOrders().toSafeStateFlow()
    val sellerOrdersState: StateFlow<DataState<List<StoreOrder>>> = repo.observeMySellerOrders().toSafeStateFlow()
    val chatsState: StateFlow<DataState<List<ChatThread>>> = repo.observeMyChats().toSafeStateFlow()
    val vaultItemsState: StateFlow<DataState<List<VaultItem>>> = repo.observeVaultItems().toSafeStateFlow()
    val aiMessagesState: StateFlow<DataState<List<AiMessageRecord>>> = repo.observeAiMessages().toSafeStateFlow()
    val gameScoresState: StateFlow<DataState<List<GameScoreRecord>>> = repo.observeMyGameScores().toSafeStateFlow()

    val isAdminUser: Boolean
        get() {
            val email = Firebase.auth.currentUser?.email
            val role = (profileState.value as? DataState.Success)?.data?.role
            return role == "admin" || email.equals("samarelgaly84@gmail.com", ignoreCase = true)
        }

    val reportsState: StateFlow<DataState<List<ModerationReport>>> =
        repo.observeModerationReports(isAdminUser).toSafeStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedChatMessagesState: StateFlow<DataState<List<ChatMessage>>> = _selectedChatId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else repo.observeChatMessages(id)
        }
        .toSafeStateFlow(DataState.Success(emptyList()))

    private val _aiStreamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val aiStreamState: StateFlow<AiStreamState> = _aiStreamState.asStateFlow()

    private val _pendingAiPrompt = MutableStateFlow<String?>(null)
    val pendingAiPrompt: StateFlow<String?> = _pendingAiPrompt.asStateFlow()

    private val _showCelebrationDialog = MutableStateFlow(false)
    val showCelebrationDialog: StateFlow<Boolean> = _showCelebrationDialog.asStateFlow()

    private val _celebrationSummary = MutableStateFlow(0L to 1L) // settledCount to newCycle
    val celebrationSummary: StateFlow<Pair<Long, Long>> = _celebrationSummary.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Ephemeral 24h Stories + Live Stream comments (augmented by user actions)
    private val _stories = MutableStateFlow(
        listOf(
            StoryItem("s1", "Tiger Studio", "Welcome to TIGER CONNECT 2026! Swipe & Explore.", 0),
            StoryItem("s2", "Tech Pulse", "ECDH P-256 + Gemini 3.8 Flash Live Streaming", 1),
            StoryItem("s3", "Marketplace", "Zero-fee Bank Transfer QR + Receipt Verification", 2)
        )
    )
    val stories: StateFlow<List<StoryItem>> = _stories.asStateFlow()

    private val _liveComments = MutableStateFlow(
        listOf(
            LiveComment("l1", "Nadia", "The new Arabic RTL layout looks fire!"),
            LiveComment("l2", "Kareem", "Testing WebRTC HD Audio from Riyadh")
        )
    )
    val liveComments: StateFlow<List<LiveComment>> = _liveComments.asStateFlow()

    private val _bookmarkedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedPostIds: StateFlow<Set<String>> = _bookmarkedPostIds.asStateFlow()

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null && user.uid == currentUserId) {
                repo.ensureUserProfile(
                    displayName = user.displayName ?: "Tiger Member",
                    email = user.email,
                    e2eePublicJwk = crypto.publicJwkString
                )
                // Sync backend session check & WebSocket signaling non-blockingly
                webSocket.connect(settings.value.backendBaseUrl, crypto.getBearerToken())
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun currentDisplayName(): String =
        (profileState.value as? DataState.Success)?.data?.displayName
            ?: Firebase.auth.currentUser?.displayName
            ?: "Tiger Member"

    // --- SETTINGS ---

    fun toggleLanguage() = viewModelScope.launch { settingsStore.setArabic(!settings.value.isArabic) }
    fun toggleTheme() = viewModelScope.launch { settingsStore.setDarkTheme(!settings.value.darkTheme) }
    fun setAccent(accentId: String) = viewModelScope.launch { settingsStore.setAccent(accentId) }
    fun setLayoutMode(mode: LayoutPreviewMode) = viewModelScope.launch { settingsStore.setLayoutPreviewMode(mode) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { settingsStore.setBiometricEnabled(enabled) }
    fun saveVaultPin(pin: String) = viewModelScope.launch {
        settingsStore.saveVaultPin(pin)
        showMessage(if (settings.value.isArabic) "تم حفظ رمز الخزينة بنجاح" else "Vault PIN saved securely")
    }

    // --- SOCIAL ---

    fun createPost(category: String, content: String, mediaType: String = "none", mediaLabel: String? = null) {
        viewModelScope.launch {
            val res = repo.createPost(currentDisplayName(), category, content, mediaType, mediaLabel)
            res.onSuccess {
                showMessage(if (settings.value.isArabic) "تم نشر المنشور بنجاح" else "Post published to Feed")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to publish post")
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch { repo.incrementPostLike(postId) }
    }

    fun toggleBookmark(postId: String) {
        _bookmarkedPostIds.value = if (postId in _bookmarkedPostIds.value) {
            _bookmarkedPostIds.value - postId
        } else {
            _bookmarkedPostIds.value + postId
        }
    }

    fun addStory(caption: String, gradientIndex: Int) {
        if (caption.isBlank()) return
        val story = StoryItem(
            id = "story_${System.currentTimeMillis()}",
            author = currentDisplayName(),
            caption = caption.trim(),
            gradientIndex = gradientIndex
        )
        _stories.value = listOf(story) + _stories.value
        showMessage(if (settings.value.isArabic) "تمت إضافة القصة لمدة 24 ساعة" else "24h Story published")
    }

    fun addLiveComment(text: String) {
        if (text.isBlank()) return
        _liveComments.value = _liveComments.value + LiveComment(
            id = "lc_${System.currentTimeMillis()}",
            author = currentDisplayName(),
            text = text.trim()
        )
    }

    fun sendToAiFromShare(text: String) {
        _pendingAiPrompt.value = text
    }

    fun consumePendingAiPrompt(): String? {
        val value = _pendingAiPrompt.value
        _pendingAiPrompt.value = null
        return value
    }

    // --- MESSENGER & E2EE ---

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
    }

    fun createChat(title: String, isGroup: Boolean) {
        viewModelScope.launch {
            repo.createChatThread(title, isGroup).onSuccess { id ->
                _selectedChatId.value = id
                showMessage(if (settings.value.isArabic) "تم إنشاء محادثة مشفّرة E2EE" else "Encrypted chat created")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to create chat")
            }
        }
    }

    fun sendEncryptedChatMessage(chat: ChatThread, plainText: String, messageType: String = "text") {
        if (plainText.isBlank()) return
        viewModelScope.launch {
            val enc = crypto.encryptMessage(plainText.trim())
            repo.sendEncryptedMessage(
                chat = chat,
                senderName = currentDisplayName(),
                cipherText = enc.cipherTextBase64,
                ivBase64 = enc.ivBase64,
                plainPreview = plainText.trim(),
                messageType = messageType
            ).onFailure {
                showMessage(it.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun shareTextToFirstOrNewChat(text: String, messageType: String = "shared_post") {
        viewModelScope.launch {
            val existing = (chatsState.value as? DataState.Success)?.data?.firstOrNull()
            if (existing != null) {
                sendEncryptedChatMessage(existing, text, messageType)
                showMessage(if (settings.value.isArabic) "تم الإرسال في المحادثة المشفّرة" else "Shared to encrypted chat")
            } else {
                repo.createChatThread("Tiger Saved Notes", false).onSuccess { newId ->
                    val created = ChatThread(id = newId, title = "Tiger Saved Notes", memberUids = listOf(currentUserId))
                    sendEncryptedChatMessage(created, text, messageType)
                    showMessage(if (settings.value.isArabic) "تم الإرسال في المحادثة المشفّرة" else "Shared to encrypted chat")
                }
            }
        }
    }

    // --- TIGER AI STUDIO & 30-DAY QUOTA CYCLE ---

    fun sendAiPrompt(prompt: String, attachedBitmap: Bitmap? = null, includeDriveContext: Boolean = false) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        val profile = (profileState.value as? DataState.Success)?.data
        val used = profile?.monthlyAiUsed ?: 0L
        val limit = profile?.monthlyAiLimit ?: 10_000L
        if (used >= limit) {
            _aiStreamState.value = AiStreamState.Error(
                if (settings.value.isArabic) "لقد استهلكت حصتك الشهرية من الرسائل. قم بالترقية إلى PLUS أو PRO."
                else "Monthly AI quota reached ($limit msgs). Upgrade to PLUS or PRO."
            )
            return
        }

        viewModelScope.launch {
            val attachLabel = when {
                attachedBitmap != null -> "Photo Attachment (${attachedBitmap.width}x${attachedBitmap.height})"
                includeDriveContext -> "Cloud Drive AI Context"
                else -> null
            }
            repo.appendAiMessage("user", cleanPrompt, attachLabel)

            val driveSummary = if (includeDriveContext) {
                val files = (vaultItemsState.value as? DataState.Success)?.data.orEmpty()
                if (files.isEmpty()) "Drive is currently empty."
                else files.joinToString("\n") { "- ${it.folder}/${it.name} (${it.mimeType}, ${it.sizeBytes / 1024} KB, locked=${it.vaultLocked})" }
            } else null

            _aiStreamState.value = AiStreamState.Loading
            var finalReply = ""
            try {
                aiRepo.streamAssistantResponse(
                    prompt = cleanPrompt,
                    isArabic = settings.value.isArabic,
                    attachedBitmap = attachedBitmap,
                    driveContext = driveSummary
                ).collect { partial ->
                    finalReply = partial
                    _aiStreamState.value = AiStreamState.Streaming(partial)
                }

                if (finalReply.isNotBlank()) {
                    repo.appendAiMessage("assistant", finalReply)
                    repo.updateProfileFields(mapOf("monthlyAiUsed" to (used + 1L)))
                    _aiStreamState.value = AiStreamState.Idle
                } else {
                    _aiStreamState.value = AiStreamState.Error("Empty AI response received.")
                }
            } catch (e: TimeoutCancellationException) {
                _aiStreamState.value = AiStreamState.Error("TIGER AI timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation error", e)
                _aiStreamState.value = AiStreamState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }

    fun settleMonthlyAiCycle() {
        viewModelScope.launch {
            val profile = (profileState.value as? DataState.Success)?.data
            val settled = (profile?.monthlyAiUsed ?: 0L).coerceAtLeast(1L)
            val nextCycle = (profile?.cycleNumber ?: 1L) + 1L
            repo.updateProfileFields(
                mapOf(
                    "monthlyAiUsed" to 0L,
                    "cycleNumber" to nextCycle
                )
            ).onSuccess {
                _celebrationSummary.value = settled to nextCycle
                _showCelebrationDialog.value = true
            }
        }
    }

    fun markCelebrationSeen() {
        val (_, cycle) = _celebrationSummary.value
        _showCelebrationDialog.value = false
        viewModelScope.launch {
            repo.updateProfileFields(mapOf("celebrationSeenCycle" to cycle))
            runCatching {
                container.apiService(settings.value.backendBaseUrl)
                    .markCelebrationSeen(CelebrationSeenRequest(cycle.toInt()))
            }
        }
    }

    // --- TIGER STORE ---

    fun createStore(
        storeName: String,
        description: String,
        bankName: String,
        accountHolder: String,
        iban: String,
        accountNumber: String,
        bankNotes: String
    ) {
        viewModelScope.launch {
            repo.createStore(
                ownerName = currentDisplayName(),
                storeName = storeName,
                description = description,
                bankName = bankName,
                accountHolder = accountHolder,
                iban = iban,
                accountNumber = accountNumber,
                bankNotes = bankNotes
            ).onSuccess {
                showMessage(if (settings.value.isArabic) "تم إنشاء متجرك ببيانات التحويل البنكي" else "Store created with Bank Transfer details")
                runCatching {
                    container.apiService(settings.value.backendBaseUrl).upsertStore(
                        StoreUpsertRequest(storeName, description, bankName, accountHolder, iban, accountNumber, bankNotes)
                    )
                }
            }.onFailure {
                showMessage(it.localizedMessage ?: "Could not create store")
            }
        }
    }

    fun addProduct(storeId: String, storeName: String, title: String, description: String, price: Double, deliveryFee: Double, stock: Long) {
        viewModelScope.launch {
            repo.createProduct(storeId, storeName, title, description, price, deliveryFee, stock)
                .onSuccess {
                    showMessage(if (settings.value.isArabic) "تمت إضافة المنتج للمتجر" else "Product listed in store")
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Failed to list product")
                }
        }
    }

    fun placeOrder(
        product: StoreProduct,
        quantity: Long,
        paymentMethod: String,
        receiptFileName: String?,
        receiptBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            repo.createOrder(currentDisplayName(), product, quantity, paymentMethod, receiptFileName)
                .onSuccess { orderId ->
                    showMessage(if (settings.value.isArabic) "تم تسجيل الطلب بنجاح" else "Order placed (#${orderId.take(6)})")
                    if (receiptBytes != null && !receiptFileName.isNullOrBlank()) {
                        runCatching {
                            val body = receiptBytes.toRequestBody("image/jpeg".toMediaType())
                            val part = MultipartBody.Part.createFormData("receipt", receiptFileName, body)
                            val orderPart = orderId.toRequestBody("text/plain".toMediaType())
                            container.apiService(settings.value.backendBaseUrl).uploadTransferReceipt(part, orderPart)
                        }
                    }
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Order failed")
                }
        }
    }

    fun sellerReviewOrder(orderId: String, approve: Boolean, refundPartialAmount: Double = 0.0, refundNote: String? = null) {
        viewModelScope.launch {
            repo.sellerReviewOrder(orderId, approve, refundPartialAmount, refundNote)
                .onSuccess {
                    val msg = if (approve) {
                        if (settings.value.isArabic) "تم اعتماد الطلب بنجاح" else "Order approved"
                    } else {
                        if (settings.value.isArabic) "تم إلغاء الطلب وتوثيق استرداد المبلغ الجزئي" else "Order cancelled & partial refund recorded"
                    }
                    showMessage(msg)
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Review update failed")
                }
        }
    }

    // --- AVATAR STUDIO, MINI-GAMES, DRIVE & VAULT, PLANS, ADMIN ---

    fun saveAvatar(configJson: String, bitmap: Bitmap?) {
        viewModelScope.launch {
            repo.updateProfileFields(mapOf("avatarConfigJson" to configJson.take(3900)))
                .onSuccess {
                    showMessage(if (settings.value.isArabic) "تم حفظ الأفاتار وتصدير PNG" else "Avatar saved & PNG exported")
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Failed to save avatar")
                }

            if (bitmap != null) {
                runCatching {
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    val part = MultipartBody.Part.createFormData(
                        "avatar",
                        "tiger_avatar.png",
                        out.toByteArray().toRequestBody("image/png".toMediaType())
                    )
                    container.apiService(settings.value.backendBaseUrl)
                        .uploadAvatar(part, configJson.toRequestBody("application/json".toMediaType()))
                }
            }
        }
    }

    fun submitMiniGameScore(gameId: String, score: Int) {
        val xp = (score * 2 + 25).coerceIn(25, 2500)
        val coins = (score / 2 + 10).coerceIn(10, 1000)
        viewModelScope.launch {
            repo.saveGameScore(currentDisplayName(), gameId, score.toLong(), xp.toLong(), coins.toLong())
                .onSuccess {
                    showMessage(
                        if (settings.value.isArabic) "تم تسجيل النتيجة! +$xp XP • +$coins عملة"
                        else "Score saved! +$xp XP • +$coins Coins"
                    )
                }
            runCatching {
                container.apiService(settings.value.backendBaseUrl)
                    .submitGameScore(GameScorePayload(gameId, score, xp, coins))
            }
        }
    }

    fun addVaultFile(name: String, folder: String, mimeType: String, sizeBytes: Long, vaultLocked: Boolean) {
        viewModelScope.launch {
            repo.addVaultItem(name, folder, mimeType, sizeBytes, vaultLocked)
                .onSuccess {
                    showMessage(if (settings.value.isArabic) "تم رفع الملف إلى تايجر درايف" else "Uploaded to TIGER Drive")
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Upload failed")
                }
        }
    }

    fun deleteVaultFile(itemId: String) {
        viewModelScope.launch { repo.deleteVaultItem(itemId) }
    }

    fun upgradePlan(planTier: String) {
        val aiLimit = when (planTier) {
            "PRO" -> 10_000_000L
            "PLUS" -> 100_000L
            else -> 10_000L
        }
        viewModelScope.launch {
            repo.updateProfileFields(
                mapOf(
                    "planTier" to planTier,
                    "monthlyAiLimit" to aiLimit
                )
            ).onSuccess {
                showMessage(if (settings.value.isArabic) "تم تفعيل باقة $planTier بنجاح!" else "$planTier plan activated!")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Plan upgrade failed")
            }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            repo.submitModerationReport(targetType, targetId, reason).onSuccess {
                showMessage(if (settings.value.isArabic) "تم إرسال البلاغ لفريق الإدارة" else "Moderation report submitted")
            }
        }
    }

    fun resolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            repo.resolveModerationReport(reportId, status).onSuccess {
                showMessage(if (settings.value.isArabic) "تم تحديث حالة البلاغ" else "Report updated ($status)")
            }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TigerViewModel(container, userId) as T
        }
    }
}
