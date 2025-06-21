package com.stripe.android.link.account

import androidx.annotation.RestrictTo
import com.stripe.android.link.model.LinkAccount

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LinkAuthResult {
    data class Success(val account: LinkAccount) : LinkAuthResult
    data object NoLinkAccountFound : LinkAuthResult
    data class AttestationFailed(val error: Throwable) : LinkAuthResult
    data class AccountError(val error: Throwable) : LinkAuthResult
    data class Error(val error: Throwable) : LinkAuthResult
}
