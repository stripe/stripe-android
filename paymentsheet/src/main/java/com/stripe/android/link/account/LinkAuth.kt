package com.stripe.android.link.account

import androidx.annotation.RestrictTo
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkAuth {
    suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult

    suspend fun lookUp(
        email: String,
        emailSource: EmailSource,
        startSession: Boolean
    ): LinkAuthResult
}
