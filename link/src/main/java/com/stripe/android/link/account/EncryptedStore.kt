package com.stripe.android.link.account

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent key-value storage encrypted at rest, backed by [EncryptedSharedPreferences].
 */
@Singleton
internal class EncryptedStore @Inject constructor(
    private val context: Context
) {
    private val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    private val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        FILE_NAME,
        mainKeyAlias,
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Write a key-value pair to the encrypted SharedPreferences
     */
    fun write(key: String, value: String) = with(sharedPreferences.edit()) {
        putString(key, value)
        apply()
    }

    /**
     * Read a String value from the encrypted SharedPreferences.
     * Returns null if value doesn't exist.
     */
    fun read(key: String) = sharedPreferences.getString(key, null)

    /**
     * Delete a value from the encrypted SharedPreferences.
     */
    fun delete(key: String) = with(sharedPreferences.edit()) {
        remove(key)
        apply()
    }

    private companion object {
        const val FILE_NAME = "LinkStore"
    }
}
