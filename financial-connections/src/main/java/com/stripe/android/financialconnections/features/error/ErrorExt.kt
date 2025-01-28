package com.stripe.android.financialconnections.features.error

import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails
import com.stripe.attestation.AttestationError

internal fun Throwable.toAttestationErrorIfApplicable(sdkPrefillDetails: PrefillDetails?): Throwable {
    return when {
        this is APIException && stripeError?.code == "link_failed_to_attest_request" -> FinancialConnectionsAttestationError(
            errorType = AttestationError.ErrorType.BACKEND_VERDICT_FAILED,
            message = stripeError?.message ?: "An unknown error occurred",
            prefillDetails = sdkPrefillDetails,
            cause = this
        )
        this is AttestationError -> FinancialConnectionsAttestationError(
            errorType = this.errorType,
            message = this.message ?: "An unknown error occurred",
            prefillDetails = sdkPrefillDetails,
            cause = this
        )
        else -> this
    }
}

internal class FinancialConnectionsAttestationError(
    val errorType: AttestationError.ErrorType,
    val prefillDetails: PrefillDetails?,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
