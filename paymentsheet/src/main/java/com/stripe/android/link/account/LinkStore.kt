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

    fun hasPassedAttestationCheck(): Boolean {
        return sharedPrefs.getBoolean(PassedAttestationCheck, false)
    }

    fun markAttestationCheckAsPassed() {
        sharedPrefs.edit { putBoolean(PassedAttestationCheck, true) }
    }

    fun clear() {
        sharedPrefs.edit { clear() }
    }

    internal companion object {
        const val FileName = "PaymentSheet_LinkStore"
        const val HasUsedLink = "has_used_link"
        const val PassedAttestationCheck = "passed_attestation_check"
    }
}
