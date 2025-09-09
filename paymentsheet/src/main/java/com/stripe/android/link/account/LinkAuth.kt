package com.stripe.android.link.account

import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource

/**
 * Interface for low-level Link authentication operations including attestation logic.
 */
internal interface LinkAuth {
    suspend fun lookup(
        email: String? = null,
        emailSource: EmailSource? = null,
        linkAuthIntentId: String? = null,
        customerId: String? = null
    ): Result<ConsumerSessionLookup>

    suspend fun signup(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<ConsumerSessionSignup>
}
