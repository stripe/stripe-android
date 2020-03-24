package com.stripe.android

import android.content.Context
import java.util.UUID

internal interface ClientFingerprintDataStore {
    fun getMuid(): String

    class Default(context: Context) : ClientFingerprintDataStore {
        private val prefs = context.getSharedPreferences(
            PREF_NAME, Context.MODE_PRIVATE
        )

        override fun getMuid(): String {
            val currentMuid = prefs.getString(KEY_MUID, null)
            return currentMuid ?: createMuid()
        }

        private fun createMuid(): String {
            return UUID.randomUUID().toString().also {
                prefs
                    .edit()
                    .putString(KEY_MUID, it)
                    .apply()
            }
        }

        private companion object {
            private const val PREF_NAME = "client_fingerprint_data"
            private const val KEY_MUID = "muid"
        }
    }
}
