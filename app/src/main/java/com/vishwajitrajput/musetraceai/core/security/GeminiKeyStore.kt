package com.vishwajitrajput.musetraceai.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveGeminiKey(key: String) {
        preferences.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun getGeminiKey(): String? = preferences.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }

    fun hasGeminiKey(): Boolean = getGeminiKey() != null

    fun clearGeminiKey() {
        preferences.edit().remove(KEY_GEMINI).apply()
    }

    private companion object {
        const val SECURE_PREFS = "secure_musetrace"
        const val KEY_GEMINI = "gemini_api_key"
    }
}
