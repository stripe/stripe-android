package com.stripe.android.link.account

import com.stripe.android.link.ui.inline.SignUpConsentAction

internal interface LinkAuth {
    suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult

    suspend fun lookUp(
        email: String
    ): LinkAuthResult
}
