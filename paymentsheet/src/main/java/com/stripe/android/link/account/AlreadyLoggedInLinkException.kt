package com.stripe.android.link.account

import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.model.AccountStatus

internal data class AlreadyLoggedInLinkException(
    val email: String?,
    val accountStatus: AccountStatus
) : StripeException() {
    override fun analyticsValue(): String = "alreadyLoggedIntoLinkError"
}
