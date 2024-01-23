package com.stripe.android.link.account

import androidx.annotation.RestrictTo
import com.stripe.android.link.model.AccountStatus

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AlreadyLoggedInLinkException(
    val email: String?,
    val accountStatus: AccountStatus
) : Exception()
