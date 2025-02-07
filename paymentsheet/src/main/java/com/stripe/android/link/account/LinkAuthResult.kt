package com.stripe.android.link.account

import com.stripe.android.link.model.LinkAccount

internal sealed interface LinkAuthResult {
    data class Success(val account: LinkAccount) : LinkAuthResult
    data object NoLinkAccountFound : LinkAuthResult
    data class AttestationFailed(val error: Throwable) : LinkAuthResult
    data class AccountError(val error: Throwable) : LinkAuthResult
    data class Error(val error: Throwable) : LinkAuthResult
}
