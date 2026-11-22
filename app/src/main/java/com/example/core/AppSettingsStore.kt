package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.tigerSettingsDataStore by preferencesDataStore(name = "tiger_connect_settings")

enum class LayoutPreviewMode(val id: String) {
    AUTO("auto"),
    PHONE("phone"),
    TABLET("tablet");

    companion object {
        fun fromId(id: String): LayoutPreviewMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class AppSettingsState(
    val isArabic: Boolean = true,
    val darkTheme: Boolean = true,
    val accentId: String = "orange",
    val layoutPreviewMode: LayoutPreviewMode = LayoutPreviewMode.AUTO,
    val biometricLockEnabled: Boolean = false,
    val vaultPinHash: String = "",
    val backendBaseUrl: String = BuildConfig.TIGER_API_BASE_URL
)

class AppSettingsStore(private val context: Context) {
    private object Keys {
        val IS_ARABIC = booleanPreferencesKey("is_arabic")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_ID = stringPreferencesKey("accent_id")
        val LAYOUT_MODE = stringPreferencesKey("layout_preview_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
    }

    val settingsFlow: Flow<AppSettingsState> = context.tigerSettingsDataStore.data.map { prefs ->
        AppSettingsState(
            isArabic = prefs[Keys.IS_ARABIC] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            accentId = prefs[Keys.ACCENT_ID] ?: "orange",
            layoutPreviewMode = LayoutPreviewMode.fromId(prefs[Keys.LAYOUT_MODE] ?: "auto"),
            biometricLockEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: "",
            backendBaseUrl = prefs[Keys.BACKEND_BASE_URL] ?: BuildConfig.TIGER_API_BASE_URL
        )
    }

    suspend fun setArabic(enabled: Boolean) {
        context.tigerSettingsDataStore.edit { it[Keys.IS_ARABIC] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.tigerSettingsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setAccent(accentId: String) {
        context.tigerSettingsDataStore.edit { it[Keys.ACCENT_ID] = accentId }
    }

    suspend fun setLayoutPreviewMode(mode: LayoutPreviewMode) {
        context.tigerSettingsDataStore.edit { it[Keys.LAYOUT_MODE] = mode.id }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.tigerSettingsDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun saveVaultPin(pin: String) {
        val hash = hashPin(pin)
        context.tigerSettingsDataStore.edit { it[Keys.VAULT_PIN_HASH] = hash }
    }

    suspend fun setBackendBaseUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        if (cleaned.startsWith("https://") || cleaned.startsWith("http://")) {
            context.tigerSettingsDataStore.edit { it[Keys.BACKEND_BASE_URL] = cleaned }
        }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return pin == "1234"
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("tiger_vault_salt_v1:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
