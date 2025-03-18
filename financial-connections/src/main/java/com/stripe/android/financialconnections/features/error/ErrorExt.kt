package com.stripe.android.financialconnections.features.error

import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ElementsSessionContext.PrefillDetails
import com.stripe.attestation.AttestationError

/**
 * Maps attestation related exceptions from the data layer (Integrity SDK, Stripe API) to a Financial Connections
 * specific domain exception type, when applicable.
 *
 * @param sdkPrefillDetails the prefill details to be used when the attestation error triggers a switch from
 * native to web flow. If the user entered email / phone on the native side before the exception occurred,
 * we'll pass this information to the web flow so the user doesn't have to re-enter it.
 */
internal fun Throwable.toAttestationErrorIfApplicable(sdkPrefillDetails: PrefillDetails?): Throwable = when {
    this is APIException && stripeError?.code == "link_failed_to_attest_request" -> {
        FinancialConnectionsAttestationError(
            errorType = AttestationError.ErrorType.BACKEND_VERDICT_FAILED,
            message = stripeError?.message ?: "An unknown error occurred",
            prefillDetails = sdkPrefillDetails,
            cause = this
        )
    }
    this is AttestationError -> {
        FinancialConnectionsAttestationError(
            errorType = this.errorType,
            message = this.message ?: "An unknown error occurred",
            prefillDetails = sdkPrefillDetails,
            cause = this
        )
    }
    else -> this
}

internal class FinancialConnectionsAttestationError(
    val errorType: AttestationError.ErrorType,
    val prefillDetails: PrefillDetails?,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
