package com.stripe.android.link.account

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent key-value storage encrypted at rest, backed by [EncryptedSharedPreferences].
 */
@Singleton
internal class EncryptedStore @Inject constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun write(key: String, value: String) = sharedPreferences.edit {
        putString(key, value)
    }

    fun write(key: String, value: Boolean) = sharedPreferences.edit {
        putBoolean(key, value)
    }

    fun read(key: String): String? = sharedPreferences.getString(key, null)

    fun read(
        key: String,
        defaultValue: Boolean,
    ): Boolean = sharedPreferences.getBoolean(key, defaultValue)

    fun delete(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    private companion object {
        const val FILE_NAME = "LinkStore"
    }
}
