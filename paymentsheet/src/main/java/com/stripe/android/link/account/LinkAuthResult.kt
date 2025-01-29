package com.stripe.android.link.account

import com.stripe.android.link.model.LinkAccount

internal sealed interface LinkAuthResult {
    data class Success(val account: LinkAccount) : LinkAuthResult
    data object NoLinkAccountFound : LinkAuthResult
    data class AttestationFailed(val throwable: Throwable) : LinkAuthResult
    data class Error(val throwable: Throwable) : LinkAuthResult
}
