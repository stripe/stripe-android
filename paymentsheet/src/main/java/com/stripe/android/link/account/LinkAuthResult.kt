package com.stripe.android.link.account

import com.stripe.android.link.model.LinkAccount

internal sealed interface LinkAuthResult {
    data class Success(val account: LinkAccount) : LinkAuthResult
    data object NoLinkAccountFound : LinkAuthResult
    data class AttestationFailed(val error: Throwable) : LinkAuthResult
    data class AccountError(val error: Throwable) : LinkAuthResult
    data class Error(val error: Throwable) : LinkAuthResult
}

internal fun Result<LinkAccount?>.toLinkAuthResult(): LinkAuthResult {
    return fold(
        onSuccess = { account ->
            if (account != null) {
                LinkAuthResult.Success(account)
            } else {
                LinkAuthResult.NoLinkAccountFound
            }
        },
        onFailure = { error ->
            when {
                isAttestationError(error) -> LinkAuthResult.AttestationFailed(error)
                isAccountError(error) -> LinkAuthResult.AccountError(error)
                else -> LinkAuthResult.Error(error)
            }
        }
    )
}

private fun isAttestationError(error: Throwable): Boolean {
    return error is com.stripe.attestation.AttestationError ||
        (
            error is com.stripe.android.core.exception.APIException &&
                error.stripeError?.code == "link_failed_to_attest_request"
            )
}

private fun isAccountError(error: Throwable): Boolean {
    return error is com.stripe.android.core.exception.APIException &&
        error.stripeError?.code == "link_consumer_details_not_available"
}
