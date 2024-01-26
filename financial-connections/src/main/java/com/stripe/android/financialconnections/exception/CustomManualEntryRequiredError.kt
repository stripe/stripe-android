package com.stripe.android.financialconnections.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

/**
 * The AuthFlow was prematurely cancelled due to user requesting manual entry.
 *
 */
class CustomManualEntryRequiredError : StripeException() {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "fcCustomManualEntryRequiredError"
}
