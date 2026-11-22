package com.example.data.firestore

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Tiger User",
    val username: String = "tiger_user",
    val email: String? = null,
    val role: String = "user",
    val planTier: String = "FREE",
    val xp: Long = 0L,
    val coins: Long = 100L,
    val monthlyAiUsed: Long = 0L,
    val monthlyAiLimit: Long = 10_000L,
    val cycleNumber: Long = 1L,
    val celebrationSeenCycle: Long = 0L,
    val avatarConfigJson: String? = null,
    val e2eePublicJwk: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val category: String = "General",
    val content: String = "",
    val mediaLabel: String? = null,
    val mediaType: String = "none",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserStore(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val storeName: String = "",
    val description: String? = null,
    val bankName: String = "",
    val accountHolder: String = "",
    val iban: String = "",
    val accountNumber: String = "",
    val bankNotes: String? = null,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreProduct(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerId: String = "",
    val storeName: String = "",
    val title: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val stock: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreOrder(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val quantity: Long = 1L,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "COD",
    val receiptFileName: String? = null,
    val status: String = "pending",
    val refundedPartialAmount: Double = 0.0,
    val refundNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val title: String = "",
    val groupChat: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val encryptedPayload: String? = null,
    val unreadCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val cipherText: String = "",
    val ivBase64: String = "",
    val plainPreview: String = "",
    val messageType: String = "text",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VaultItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val folder: String = "Documents",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val vaultLocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiMessageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderRole: String = "user",
    val text: String = "",
    val attachmentLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameScoreRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val gameId: String = "",
    val score: Long = 0L,
    val xpEarned: Long = 0L,
    val coinsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
