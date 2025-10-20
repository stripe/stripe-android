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
    return runCatching {
        val linkAccount = getOrThrow()
        return if (linkAccount != null) {
            LinkAuthResult.Success(linkAccount)
        } else {
            LinkAuthResult.NoLinkAccountFound
        }
    }.getOrElse { error ->
        error.toLinkAuthResult()
    }
}

private fun Throwable.toLinkAuthResult(): LinkAuthResult {
    return when {
        isAttestationError -> {
            LinkAuthResult.AttestationFailed(this)
        }
        isAccountError -> {
            LinkAuthResult.AccountError(this)
        }
        else -> {
            LinkAuthResult.Error(this)
        }
    }
}

private val Throwable.isAttestationError: Boolean
    get() = isIntegrityManagerError || isBackendAttestationError

// Interaction with Integrity API to generate tokens resulted in a failure
internal val Throwable.isIntegrityManagerError: Boolean
    get() = this is com.stripe.attestation.AttestationError

// Stripe backend could not verify the integrity of the request
internal val Throwable.isBackendAttestationError: Boolean
    get() = this is com.stripe.android.core.exception.APIException &&
        stripeError?.code == "link_failed_to_attest_request"

private val Throwable.isAccountError: Boolean
    get() = when (this) {
        // This happens when account is suspended or banned
        is com.stripe.android.core.exception.APIException -> stripeError?.code == "link_consumer_details_not_available"
        else -> false
    }
