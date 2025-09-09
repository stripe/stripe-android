package com.stripe.android.link.account

import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource

/**
 * Interface for low-level Link authentication operations including attestation logic.
 * 
 * WARNING: Do not use this interface directly in your code. Use LinkAccountManager instead.
 * LinkAccountManager is the single source of truth for Link account operations and
 * provides proper session management and LinkAccount creation.
 * 
 * This interface should only be used internally by LinkAccountManager for raw API calls.
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