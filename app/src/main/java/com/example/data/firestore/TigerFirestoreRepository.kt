package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TigerFirestoreRepository(private val db: FirebaseFirestore) {

    // Always initialize with R.string.firestore_database_id (never parameterless default)
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.withFirestoreErrorHandling(
        uid: String,
        op: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- USER PROFILE ---

    fun observeMyProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .withFirestoreErrorHandling(uid, OperationType.GET, path)
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?,
        e2eePublicJwk: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val cleanName = displayName.trim().ifBlank { "Tiger Member" }.take(80)
                val baseHandle = cleanName.lowercase().replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "tiger" }.take(20) + "_" + uid.take(4).lowercase()
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "username" to baseHandle,
                    "email" to email?.take(160),
                    "role" to "user",
                    "planTier" to "FREE",
                    "xp" to 120L,
                    "coins" to 250L,
                    "monthlyAiUsed" to 0L,
                    "monthlyAiLimit" to 10_000L,
                    "cycleNumber" to 1L,
                    "celebrationSeenCycle" to 0L,
                    "e2eePublicJwk" to e2eePublicJwk.take(1900),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "e2eePublicJwk" to e2eePublicJwk.take(1900),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val mutable = fields.toMutableMap()
            mutable["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(mutable).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- SOCIAL POSTS ---

    fun observePublicPosts(): Flow<List<SocialPost>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun createPost(
        authorName: String,
        category: String,
        content: String,
        mediaType: String = "none",
        mediaLabel: String? = null
    ): Result<Unit> {
        val path = "posts"
        return try {
            val uid = requireUserId()
            val trimmed = content.trim()
            require(trimmed.isNotEmpty() && trimmed.length <= 5000) { "Post content must be 1..5000 chars" }
            val docRef = db.collection(path).document()
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "Tiger User" }.take(80),
                "category" to category,
                "content" to trimmed,
                "mediaLabel" to mediaLabel?.take(300),
                "mediaType" to mediaType,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementPostLike(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank())
            db.collection("posts").document(postId).update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- TIGER STORE, PRODUCTS & ORDERS ---

    fun observePublicStores(): Flow<List<UserStore>> = flow {
        val uid = requireUserId()
        val path = "stores"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { it.toObjects(UserStore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    fun observePublicProducts(): Flow<List<StoreProduct>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { it.toObjects(StoreProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    fun observeMyBuyerOrders(): Flow<List<StoreOrder>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("buyerId", uid)
                .snapshots()
                .map { it.toObjects(StoreOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    fun observeMySellerOrders(): Flow<List<StoreOrder>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("sellerId", uid)
                .snapshots()
                .map { it.toObjects(StoreOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun createStore(
        ownerName: String,
        storeName: String,
        description: String,
        bankName: String,
        accountHolder: String,
        iban: String,
        accountNumber: String,
        bankNotes: String
    ): Result<String> {
        val path = "stores"
        return try {
            val uid = requireUserId()
            require(storeName.trim().length >= 2) { "Store name required" }
            require(iban.trim().length >= 8) { "Valid IBAN required (8+ chars)" }
            val docRef = db.collection(path).document()
            val payload = mapOf<String, Any?>(
                "ownerId" to uid,
                "ownerName" to ownerName.trim().ifBlank { "Seller" }.take(80),
                "storeName" to storeName.trim().take(100),
                "description" to description.trim().take(600).ifBlank { null },
                "bankName" to bankName.trim().take(100),
                "accountHolder" to accountHolder.trim().take(120),
                "iban" to iban.trim().uppercase().take(40),
                "accountNumber" to accountNumber.trim().take(40),
                "bankNotes" to bankNotes.trim().take(400).ifBlank { null },
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createProduct(
        storeId: String,
        storeName: String,
        title: String,
        description: String,
        price: Double,
        deliveryFee: Double,
        stock: Long
    ): Result<Unit> {
        val path = "products"
        return try {
            val uid = requireUserId()
            require(title.trim().length >= 2)
            val docRef = db.collection(path).document()
            val payload = mapOf<String, Any?>(
                "storeId" to storeId,
                "ownerId" to uid,
                "storeName" to storeName.take(100),
                "title" to title.trim().take(120),
                "description" to description.trim().take(800).ifBlank { null },
                "price" to price.coerceIn(0.0, 10_000_000.0),
                "deliveryFee" to deliveryFee.coerceIn(0.0, 100_000.0),
                "stock" to stock.coerceIn(0L, 1_000_000L),
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        buyerName: String,
        product: StoreProduct,
        quantity: Long,
        paymentMethod: String,
        receiptFileName: String?
    ): Result<String> {
        val path = "orders"
        return try {
            val uid = requireUserId()
            val qty = quantity.coerceIn(1L, 1000L)
            val subtotal = product.price * qty
            val delivery = product.deliveryFee * qty
            val total = subtotal + delivery
            val status = if (!receiptFileName.isNullOrBlank()) "receipt_uploaded" else "pending"
            val docRef = db.collection(path).document()
            val payload = mapOf<String, Any?>(
                "buyerId" to uid,
                "buyerName" to buyerName.trim().ifBlank { "Buyer" }.take(80),
                "sellerId" to product.ownerId,
                "storeId" to product.storeId,
                "storeName" to product.storeName,
                "productId" to product.id,
                "productTitle" to product.title,
                "quantity" to qty,
                "subtotal" to subtotal,
                "deliveryFee" to delivery,
                "totalAmount" to total,
                "paymentMethod" to paymentMethod,
                "receiptFileName" to receiptFileName?.take(240),
                "status" to status,
                "refundedPartialAmount" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun attachReceiptToOrder(orderId: String, receiptFileName: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "receiptFileName" to receiptFileName.take(240),
                    "status" to "receipt_uploaded",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sellerReviewOrder(
        orderId: String,
        approve: Boolean,
        refundPartialAmount: Double = 0.0,
        refundNote: String? = null
    ): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to if (approve) "approved" else "cancelled_refunded",
                "refundedPartialAmount" to if (approve) 0.0 else refundPartialAmount.coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!approve && !refundNote.isNullOrBlank()) {
                updates["refundNote"] = refundNote.trim().take(400)
            }
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ENCRYPTED CHATS ---

    fun observeMyChats(): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun createChatThread(title: String, groupChat: Boolean, extraMembers: List<String> = emptyList()): Result<String> {
        val path = "chats"
        return try {
            val uid = requireUserId()
            val members = (listOf(uid) + extraMembers).distinct().take(25)
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "title" to title.trim().ifBlank { "Tiger Chat" }.take(100),
                "groupChat" to groupChat,
                "memberUids" to members,
                "lastMessagePreview" to "E2EE session established",
                "unreadCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendEncryptedMessage(
        chat: ChatThread,
        senderName: String,
        cipherText: String,
        ivBase64: String,
        plainPreview: String,
        messageType: String = "text"
    ): Result<Unit> {
        val path = "chats/${chat.id}/messages"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
            val msgPayload = mapOf(
                "chatId" to chat.id,
                "senderId" to uid,
                "senderName" to senderName.trim().ifBlank { "Me" }.take(80),
                "memberUids" to chat.memberUids.ifEmpty { listOf(uid) },
                "cipherText" to cipherText.take(8000),
                "ivBase64" to ivBase64.take(128),
                "plainPreview" to plainPreview.take(2000),
                "messageType" to messageType,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(msgPayload).await()
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessagePreview" to plainPreview.take(480),
                    "encryptedPayload" to cipherText.take(3900),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- CLOUD DRIVE & PRIVATE VAULT ---

    fun observeVaultItems(): Flow<List<VaultItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/vaultItems"
        emitAll(
            db.collection("users").document(uid).collection("vaultItems")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VaultItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun addVaultItem(
        name: String,
        folder: String,
        mimeType: String,
        sizeBytes: Long,
        vaultLocked: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/vaultItems"
        return try {
            val docRef = db.collection("users").document(uid).collection("vaultItems").document()
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim().ifBlank { "Untitled" }.take(160),
                "folder" to folder.trim().ifBlank { "Documents" }.take(64),
                "mimeType" to mimeType.take(100),
                "sizeBytes" to sizeBytes.coerceAtLeast(0L),
                "vaultLocked" to vaultLocked,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(itemId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/vaultItems/$itemId"
        return try {
            db.collection("users").document(uid).collection("vaultItems").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- AI MESSAGES & GAME SCORES ---

    fun observeAiMessages(): Flow<List<AiMessageRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/aiMessages"
        emitAll(
            db.collection("users").document(uid).collection("aiMessages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AiMessageRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun appendAiMessage(senderRole: String, text: String, attachmentLabel: String? = null): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/aiMessages"
        return try {
            val docRef = db.collection("users").document(uid).collection("aiMessages").document()
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "senderRole" to senderRole,
                "text" to text.trim().ifBlank { "..." }.take(19500),
                "attachmentLabel" to attachmentLabel?.take(200),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMyGameScores(): Flow<List<GameScoreRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/gameScores"
        emitAll(
            db.collection("users").document(uid).collection("gameScores")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameScoreRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.score }
                }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun saveGameScore(
        playerName: String,
        gameId: String,
        score: Long,
        xpEarned: Long,
        coinsEarned: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/gameScores"
        return try {
            val docRef = db.collection("users").document(uid).collection("gameScores").document()
            val payload = mapOf(
                "userId" to uid,
                "playerName" to playerName.trim().ifBlank { "Player" }.take(80),
                "gameId" to gameId.take(64),
                "score" to score.coerceAtLeast(0L),
                "xpEarned" to xpEarned.coerceIn(0L, 100_000L),
                "coinsEarned" to coinsEarned.coerceIn(0L, 100_000L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "xp" to FieldValue.increment(xpEarned.coerceIn(0L, 100_000L)),
                    "coins" to FieldValue.increment(coinsEarned.coerceIn(0L, 100_000L)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- MODERATION REPORTS ---

    fun observeModerationReports(isAdminUser: Boolean): Flow<List<ModerationReport>> = flow {
        val uid = requireUserId()
        val path = "moderationReports"
        val query = if (isAdminUser) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("reporterId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorHandling(uid, OperationType.LIST, path)
        )
    }

    suspend fun submitModerationReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        val path = "moderationReports"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "reporterId" to uid,
                "targetType" to targetType,
                "targetId" to targetId.take(128),
                "reason" to reason.trim().take(500),
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveModerationReport(reportId: String, newStatus: String): Result<Unit> {
        val path = "moderationReports/$reportId"
        return try {
            requireUserId()
            db.collection("moderationReports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
