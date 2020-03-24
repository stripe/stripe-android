package com.stripe.android

import android.content.Context
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

internal interface ClientFingerprintDataStore {
    fun getMuid(): String
    fun getSid(): String

    class Default(
        context: Context,
        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }
    ) : ClientFingerprintDataStore {
        private val prefs = context.getSharedPreferences(
            PREF_NAME, Context.MODE_PRIVATE
        )

        override fun getMuid(): String {
            val currentMuid = prefs.getString(KEY_MUID, null)
            return currentMuid ?: createMuid()
        }

        override fun getSid(): String {
            val sid = prefs.getString(KEY_SID, null).takeUnless { isSidExpired() }
            return sid ?: createSid()
        }

        private fun isSidExpired(): Boolean {
            return (timestampSupplier() - prefs.getLong(KEY_SID_TIMESTAMP, 0L)) > SID_TTL
        }

        private fun createSid(): String {
            return UUID.randomUUID().toString().also {
                prefs
                    .edit()
                    .putString(KEY_SID, it)
                    .putLong(KEY_SID_TIMESTAMP, timestampSupplier())
                    .apply()
            }
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
            private const val KEY_SID = "sid"
            private const val KEY_SID_TIMESTAMP = "sid_timestamp"

            private val SID_TTL = TimeUnit.MINUTES.toMillis(30L)
        }
    }
}
