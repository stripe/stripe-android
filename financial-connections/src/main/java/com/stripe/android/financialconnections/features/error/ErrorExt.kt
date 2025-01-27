package com.stripe.android.financialconnections.features.error

import com.stripe.android.core.exception.APIException
import com.stripe.attestation.AttestationError

internal fun Throwable.toAttestationErrorIfApplicable(prefilledEmail: String?): Throwable {
    return when {
        this is APIException && stripeError?.code == "link_failed_to_attest_request" -> FinancialConnectionsAttestationError(
            errorType = AttestationError.ErrorType.BACKEND_VERDICT_FAILED,
            message = stripeError?.message ?: "An unknown error occurred",
            prefilledEmail = prefilledEmail,
            cause = this
        )
        this is AttestationError -> FinancialConnectionsAttestationError(
            errorType = this.errorType,
            message = this.message ?: "An unknown error occurred",
            prefilledEmail = prefilledEmail,
            cause = this
        )
        else -> this
    }
}

internal class FinancialConnectionsAttestationError(
    val errorType: AttestationError.ErrorType,
    val prefilledEmail: String?,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

