package com.stripe.android.link.account

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.model.AccountStatus

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AlreadyLoggedInLinkException(
    val email: String?,
    val accountStatus: AccountStatus
) : StripeException() {
    override fun analyticsValue(): String = "alreadyLoggedIntoLinkError"
}
