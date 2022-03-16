package com.stripe.android.link.account

import android.content.Context
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
