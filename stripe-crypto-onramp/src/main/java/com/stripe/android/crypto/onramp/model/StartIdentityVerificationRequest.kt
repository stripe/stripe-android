package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal API request model for starting identity verification.
 * This represents the exact structure expected by the Stripe API.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class StartIdentityVerificationRequest(

    val credentials: CryptoCustomerRequestParams.Credentials,

    @SerialName("is_mobile")
    val isMobile: Boolean = true
)
