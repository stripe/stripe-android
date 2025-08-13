package com.stripe.android.link.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class LinkStore @Inject constructor(
    context: Context,
) {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
    }

    fun hasUsedLink(): Boolean {
        return sharedPrefs.getBoolean(HasUsedLink, false)
    }

    fun markLinkAsUsed() {
        sharedPrefs.edit { putBoolean(HasUsedLink, true) }
    }

    fun hasPassedAttestationChecksRecently(): Boolean {
        val timestamp = sharedPrefs.getLong(AttestationCheckTimestamp, 0L)
        if (timestamp == 0L) return false

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - timestamp

        return elapsedTime < ATTESTATION_EXPIRY_DURATION_MS
    }

    fun markAttestationCheckAsPassed() {
        sharedPrefs.edit {
            putLong(AttestationCheckTimestamp, System.currentTimeMillis())
        }
    }

    fun clear() {
        sharedPrefs.edit { clear() }
    }

    internal companion object {
        const val FileName = "PaymentSheet_LinkStore"
        const val HasUsedLink = "has_used_link"
        const val AttestationCheckTimestamp = "attestation_check_timestamp"

        // Weekly expiration: 7 days in milliseconds
        private val ATTESTATION_EXPIRY_DURATION_MS = TimeUnit.DAYS.toMillis(7)
    }
}
