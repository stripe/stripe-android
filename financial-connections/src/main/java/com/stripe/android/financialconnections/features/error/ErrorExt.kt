package com.stripe.android.financialconnections.features.error

import com.stripe.android.core.exception.APIException
import com.stripe.attestation.AttestationError

internal fun Throwable.isAttestationError(): Boolean = when (this) {
    // Stripe backend could not verify the intregrity of the request
    is APIException -> stripeError?.code == "link_failed_to_attest_request"
    // Interaction with Integrity API to generate tokens resulted in a failure
    is AttestationError -> true
    else -> false
}
