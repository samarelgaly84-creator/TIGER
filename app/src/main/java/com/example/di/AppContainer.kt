package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.call.CallNotificationHelper
import com.example.core.AppSettingsStore
import com.example.core.E2eeCryptoManager
import com.example.data.ai.TigerAiRepository
import com.example.data.firestore.TigerFirestoreRepository
import com.example.data.local.TigerDatabase
import com.example.data.remote.TigerApiService
import com.example.data.remote.TigerNetworkFactory
import com.example.data.remote.TigerWebSocketClient

/**
 * Clean application-wide dependency injection container providing singletons for
 * Firestore, Room cache, DataStore, Keystore E2EE Crypto, Retrofit/OkHttp, WebSocket, and Firebase AI Logic.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsStore: AppSettingsStore by lazy { AppSettingsStore(appContext) }
    val cryptoManager: E2eeCryptoManager by lazy { E2eeCryptoManager(appContext) }
    val roomDatabase: TigerDatabase by lazy { TigerDatabase.getInstance(appContext) }
    val okHttpClient by lazy { TigerNetworkFactory.buildOkHttpClient(cryptoManager) }
    val webSocketClient: TigerWebSocketClient by lazy { TigerWebSocketClient(okHttpClient) }
    val firestoreRepo: TigerFirestoreRepository by lazy { TigerFirestoreRepository(appContext) }
    val aiRepository: TigerAiRepository by lazy { TigerAiRepository() }

    fun apiService(baseUrl: String = BuildConfig.TIGER_API_BASE_URL): TigerApiService {
        val safeUrl = if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            baseUrl
        } else {
            BuildConfig.TIGER_API_BASE_URL
        }
        return TigerNetworkFactory.buildApiService(safeUrl, okHttpClient)
    }

    init {
        CallNotificationHelper.createNotificationChannels(appContext)
        CallNotificationHelper.registerTelecomAccount(appContext)
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
