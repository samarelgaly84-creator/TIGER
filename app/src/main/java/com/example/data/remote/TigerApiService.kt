package com.example.data.remote

import com.example.core.E2eeCryptoManager
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = false)
data class AuthLoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = false)
data class AuthRegisterRequest(val displayName: String, val email: String, val password: String)

@JsonClass(generateAdapter = false)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@JsonClass(generateAdapter = false)
data class AuthSessionResponse(
    val token: String? = null,
    val userId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val planTier: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = false)
data class AiChatRequest(
    val prompt: String,
    val language: String = "ar",
    val contextNotes: String? = null
)

@JsonClass(generateAdapter = false)
data class CelebrationSeenRequest(val cycleNumber: Int)

@JsonClass(generateAdapter = false)
data class GameScorePayload(
    val gameId: String,
    val score: Int,
    val xpEarned: Int,
    val coinsEarned: Int
)

@JsonClass(generateAdapter = false)
data class CallSignalRequest(
    val callId: String,
    val type: String, // offer | answer | ice | invite_participant | hangup
    val targetUserId: String,
    val sdpOrCandidate: String
)

@JsonClass(generateAdapter = false)
data class StoreUpsertRequest(
    val storeName: String,
    val description: String,
    val bankName: String,
    val accountHolder: String,
    val iban: String,
    val accountNumber: String,
    val bankNotes: String
)

@JsonClass(generateAdapter = false)
data class GenericApiResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val url: String? = null
)

interface TigerApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: AuthLoginRequest): Response<AuthSessionResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: AuthRegisterRequest): Response<AuthSessionResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<GenericApiResponse>

    @POST("/api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<GenericApiResponse>

    @GET("/api/auth/me")
    suspend fun getMe(): Response<AuthSessionResponse>

    @Multipart
    @POST("/api/auth/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part,
        @Part("configJson") configJson: RequestBody
    ): Response<GenericApiResponse>

    @Streaming
    @POST("/api/ai/chat/stream")
    suspend fun streamAiChat(@Body request: AiChatRequest): Response<ResponseBody>

    @Multipart
    @POST("/api/ai/chat/upload")
    suspend fun uploadAiMedia(
        @Part file: MultipartBody.Part,
        @Part("prompt") prompt: RequestBody
    ): Response<GenericApiResponse>

    @POST("/api/ai/drive")
    suspend fun analyzeDrive(@Body request: AiChatRequest): Response<GenericApiResponse>

    @POST("/api/ai/celebration-seen")
    suspend fun markCelebrationSeen(@Body request: CelebrationSeenRequest): Response<GenericApiResponse>

    @POST("/api/store/my-store")
    suspend fun upsertStore(@Body request: StoreUpsertRequest): Response<GenericApiResponse>

    @Multipart
    @POST("/api/store/receipts/upload")
    suspend fun uploadTransferReceipt(
        @Part receipt: MultipartBody.Part,
        @Part("orderId") orderId: RequestBody
    ): Response<GenericApiResponse>

    @POST("/api/engagement/games/score")
    suspend fun submitGameScore(@Body payload: GameScorePayload): Response<GenericApiResponse>

    @POST("/api/chat/call/signal")
    suspend fun sendCallSignal(@Body signal: CallSignalRequest): Response<GenericApiResponse>
}

object TigerNetworkFactory {
    fun buildOkHttpClient(cryptoManager: E2eeCryptoManager): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = cryptoManager.getBearerToken()
            val reqBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(reqBuilder.build())
        }
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun buildApiService(baseUrl: String, client: OkHttpClient): TigerApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TigerApiService::class.java)
    }
}
