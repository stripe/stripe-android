package com.stripe.android.link.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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
        return AttestationCache.hasPassedAttestation()
    }

    fun markAttestationCheckAsPassed() {
        AttestationCache.markAttestationAsPassed()
    }

    fun clear() {
        sharedPrefs.edit { clear() }
        AttestationCache.clear()
    }

    internal companion object {
        const val FileName = "PaymentSheet_LinkStore"
        const val HasUsedLink = "has_used_link"
    }
}
