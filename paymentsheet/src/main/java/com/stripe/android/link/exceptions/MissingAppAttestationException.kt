package com.stripe.android.link.exceptions

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

/**
 * Link is available for the merchant, but app attestation is missing or the device can't use native Link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MissingAppAttestationException : StripeException(
    message = "App attestation is missing or device cannot use native Link.",
) {
    override fun analyticsValue(): String = "missingAppAttestation"
}
