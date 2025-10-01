package com.stripe.android.link.account

import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource

/**
 * Interface for low-level Link authentication operations including attestation logic.
 */
internal interface LinkAuth {
    suspend fun lookup(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        customerId: String?,
        sessionId: String,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionLookup>

    suspend fun signup(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<ConsumerSessionSignup>

    suspend fun refreshConsumer(
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
    ): Result<ConsumerSessionRefresh>
}
